package main

import (
	"fmt"
	ClientMsp "github.com/tw-bc-group/fabric-sdk-go-gm/pkg/client/msp"
	"github.com/tw-bc-group/fabric-sdk-go-gm/pkg/common/providers/core"
	mspid "github.com/tw-bc-group/fabric-sdk-go-gm/pkg/common/providers/msp"
	"github.com/tw-bc-group/fabric-sdk-go-gm/pkg/core/config"
	"github.com/tw-bc-group/fabric-sdk-go-gm/pkg/core/cryptosuite"
	"github.com/tw-bc-group/fabric-sdk-go-gm/pkg/fabsdk"
	"github.com/tw-bc-group/fabric-sdk-go-gm/pkg/msp"
	"path/filepath"
)

var caServerURL string

const (
	configFile = "/Users/yin/projects/fabric/fabric-samples/first-network/connection-org1.yaml"
)

type clientFixture struct {
	cryptoSuiteConfig core.CryptoSuiteConfig
	identityConfig    mspid.IdentityConfig
}

func main() {

	// Initiate the sdk using the config file
	client := clientFixture{}
	//create the CA instance
	sdk := client.setup()

	fmt.Printf("------- EnrollUser %s------\n", "admin")

	EnrollUser(sdk, "admin", "adminpw")

	//fmt.Printf("------- EnrollUser %s------\n", "yin")

	//EnrollUser(sdk, "yin","yin")

	//fmt.Printf("------- RegisterlUser %s------\n", "yin")

	RegisterlUser(sdk, "yin", "yin", "department1")
}

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

func (f *clientFixture) setup() *fabsdk.FabricSDK {
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
	defer sdk.Close()

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
