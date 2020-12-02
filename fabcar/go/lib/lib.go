package lib

import (
	"errors"
	"fmt"
	ClientMsp "github.com/tw-bc-group/fabric-sdk-go-gm/pkg/client/msp"
	"github.com/tw-bc-group/fabric-sdk-go-gm/pkg/common/providers/core"
	mspid "github.com/tw-bc-group/fabric-sdk-go-gm/pkg/common/providers/msp"
	"github.com/tw-bc-group/fabric-sdk-go-gm/pkg/core/config"
	"github.com/tw-bc-group/fabric-sdk-go-gm/pkg/core/cryptosuite"
	"github.com/tw-bc-group/fabric-sdk-go-gm/pkg/fabsdk"
	"github.com/tw-bc-group/fabric-sdk-go-gm/pkg/gateway"
	"github.com/tw-bc-group/fabric-sdk-go-gm/pkg/msp"
	"io/ioutil"
	"os"
	"path/filepath"
)

//EnrollUser enroll a user have registerd
func EnrollUser(sdk *fabsdk.FabricSDK, username string, password string) (bool, error) {
	ctx := sdk.Context()
	mspClient, err := ClientMsp.New(ctx)
	if err != nil {
		fmt.Printf("Failed to create msp client: %s\n", err)
		return true, err
	}

	_, err = mspClient.GetSigningIdentity(username)
	if err == ClientMsp.ErrUserNotFound {
		fmt.Println("Going to enroll user")
		err = mspClient.Enroll(username, ClientMsp.WithSecret(password))
		if err != nil {
			fmt.Printf("Failed to enroll user: %s\n", err)
			return true, err
		}
		fmt.Printf("Success enroll user: %s\n", username)
		return true, nil
	} else if err != nil {
		fmt.Printf("Failed to get user: %s\n", err)
		return false, err
	}
	fmt.Printf("User %s already enrolled, skip enrollment.\n", username)
	return true, nil
}

//Register a new user with username , password and department.
func RegisterlUser(sdk *fabsdk.FabricSDK, username, password, department string) error {
	ctx := sdk.Context()
	mspClient, err := ClientMsp.New(ctx)
	if err != nil {
		fmt.Printf("Failed to create msp client: %s\n", err)
	}
	request := &ClientMsp.RegistrationRequest{
		Name:        username,
		Type:        "user",
		Affiliation: department,
		Secret:      password,
	}

	secret, err := mspClient.Register(request)
	if err != nil {
		fmt.Printf("register %s [%s]\n", username, err)
		return err
	}
	fmt.Printf("register %s successfully,with password %s\n", username, secret)
	return nil
}

type ClientFixture struct {
	cryptoSuiteConfig core.CryptoSuiteConfig
	identityConfig    mspid.IdentityConfig
}

func (f *ClientFixture) Setup(configFile string) *fabsdk.FabricSDK {
	var err error

	configPath := filepath.Join(configFile)
	backend, err := config.FromFile(configPath)()
	if err != nil {
		fmt.Println(err)
	}
	configProvider := func() ([]core.ConfigBackend, error) {
		return backend, nil
	}

	// Instantiate the SDK
	sdk, err := fabsdk.New(configProvider)
	if err != nil {
		fmt.Println(err)
	}
	configBackend, err := sdk.Config()
	if err != nil {
		panic(fmt.Sprintf("Failed to get config: %s", err))
	}

	f.cryptoSuiteConfig = cryptosuite.ConfigFromBackend(configBackend)
	f.identityConfig, _ = msp.ConfigFromBackend(configBackend)
	if err != nil {
		fmt.Println(err)
	}
	return sdk
}

func ChangeOwnAndSelectIt(contract *gateway.Contract, carName string, owner string) {
	result, err := contract.SubmitTransaction("changeCarOwner", carName, owner)
	if err != nil {
		fmt.Printf("Failed to submit transaction: %s\n", err)
		os.Exit(1)
	}

	result, err = contract.EvaluateTransaction("queryCar", carName)
	if err != nil {
		fmt.Printf("Failed to evaluate transaction: %s\n", err)
		os.Exit(1)
	}
	fmt.Println(string(result))
}

func CreateCarAndSelectIt(contract *gateway.Contract, carName string) {
	result, err := contract.SubmitTransaction("createCar", carName, "VW", "Polo", "Grey", "Mary")
	if err != nil {
		fmt.Printf("Failed to submit transaction: %s\n", err)
		os.Exit(1)
	}
	result, err = contract.EvaluateTransaction("queryCar", carName)
	if err != nil {
		fmt.Printf("Failed to evaluate transaction: %s\n", err)
		os.Exit(1)
	}
	fmt.Println(string(result))
}

func QueryAllCars(contract *gateway.Contract) {
	result, err := contract.EvaluateTransaction("queryAllCars")
	if err != nil {
		fmt.Printf("Failed to evaluate transaction: %s\n", err)
		os.Exit(1)
	}
	fmt.Println(string(result))
}

func PopulateWallet(wallet *gateway.Wallet, walletName string) error {
	credPath := filepath.Join(
		"..",
		"..",
		"first-network",
		"crypto-config",
		"peerOrganizations",
		"org1.example.com",
		"users",
		"User1@org1.example.com",
		"msp",
	)

	certPath := filepath.Join(credPath, "signcerts", "cert.pem")
	// read the certificate pem
	cert, err := ioutil.ReadFile(filepath.Clean(certPath))
	if err != nil {
		return err
	}

	keyDir := filepath.Join(credPath, "keystore")
	// there's a single file in this dir containing the private key
	files, err := ioutil.ReadDir(keyDir)
	if err != nil {
		return err
	}
	if len(files) != 1 {
		return errors.New("keystore folder should have contain one file")
	}
	keyPath := filepath.Join(keyDir, files[0].Name())
	key, err := ioutil.ReadFile(filepath.Clean(keyPath))
	if err != nil {
		return err
	}

	identity := gateway.NewX509Identity("Org1MSP", string(cert), string(key))

	err = wallet.Put(walletName, identity)
	if err != nil {
		return err
	}
	return nil
}

func GetContract(configPath string, walletName string) (error, *gateway.Contract) {
	//WE use org1.example.com as domain name, so disable localhost discovery here.
	//os.Setenv("DISCOVERY_AS_LOCALHOST", "true")
	wallet, err := gateway.NewFileSystemWallet("wallet")
	if err != nil {
		fmt.Printf("Failed to create wallet: %s\n", err)
		os.Exit(1)
	}

	if !wallet.Exists(walletName) {
		err = PopulateWallet(wallet, walletName)
		if err != nil {
			fmt.Printf("Failed to populate wallet contents: %s\n", err)
			os.Exit(1)
		}
	}

	ccpPath := filepath.Join(configPath)

	gw, err := gateway.Connect(
		gateway.WithConfig(config.FromFile(filepath.Clean(ccpPath))),
		gateway.WithIdentity(wallet, walletName),
	)
	if err != nil {
		fmt.Printf("Failed to connect to gateway: %s\n", err)
		os.Exit(1)
	}
	defer gw.Close()

	network, err := gw.GetNetwork("mychannel")
	if err != nil {
		fmt.Printf("Failed to get network: %s\n", err)
		os.Exit(1)
	}

	contract := network.GetContract("fabcar")
	return err, contract
}
