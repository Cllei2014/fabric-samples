package main

import (
	"fmt"
	clientmsp "github.com/tw-bc-group/fabric-sdk-go-gm/pkg/client/msp"
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
	//configFile = "/Users/yin/projects/fabric/fabric-samples/first-network/fabric-ca/org1/fabric-ca-server-config.yaml"
	configFile = "/Users/yin/projects/fabric/fabric-samples/first-network/connection-org1.yaml"
)

type clientFixture struct {
	cryptoSuiteConfig core.CryptoSuiteConfig
	identityConfig    mspid.IdentityConfig
}

func main() {

	// Initiate the sdk using the config file
	client := clientFixture{}
	sdk := client.setup()
	//create the CA instance

	//c, err := clientmsp.New(
	var c, err = clientmsp.New(sdk.Context())
	if err != nil {
		fmt.Println("failed to create msp client", err)
		return
	}
	fmt.Println("New client instance created", c)

	err = c.Enroll("admin", clientmsp.WithSecret("adminpw"))
	if err != nil {
		fmt.Println("failed to register identity", err)
	}
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
