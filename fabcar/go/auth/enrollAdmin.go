package auth

import (
	"fmt"
	clientmsp "github.com/tw-bc-group/fabric-sdk-go-gm/pkg/client/msp"
	"github.com/tw-bc-group/fabric-sdk-go-gm/pkg/common/providers/core"
	mspid "github.com/tw-bc-group/fabric-sdk-go-gm/pkg/common/providers/msp"
	"github.com/tw-bc-group/fabric-sdk-go-gm/pkg/core/config"
	"github.com/tw-bc-group/fabric-sdk-go-gm/pkg/core/cryptosuite"
	"github.com/tw-bc-group/fabric-sdk-go-gm/pkg/fabsdk"
	"github.com/tw-bc-group/fabric-sdk-go-gm/pkg/msp"
	"github.com/tw-bc-group/fabric-sdk-go-gm/pkg/msp/test/mockmsp"
	"net"
	"os"
	"path/filepath"
	"strings"
)

var (
	DefaultHome = os.ExpandEnv("$PWD/CONFIG")
)
var caServerURL string
var caServer = &mockmsp.MockFabricCAServer{}

const (
	caServerURLListen = "http://localhost:7054"
	configFile        = "fabric-ca-server-config.yaml"
)

type nwConfig struct {
	CertificateAuthorities map[string]msp.CAConfig
}

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
	//	*fabsdk.FabricSDK.Context(),
	//	mspclient.WithOrg(OrgName),
	//)
	//if err != nil {
	//	return clientmsp, errors.WithMessage(err, "failed to create MSP client")
	//}

	var c, err = clientmsp.New(sdk.Context())
	if err != nil {
		fmt.Println("failed to create msp client", err)
		return
	}
	fmt.Println("New client instance created", c)

	err = c.Enroll("Admin@org1", clientmsp.WithSecret("Admin@org1"), clientmsp.WithProfile("tls"))
	if err != nil {
		fmt.Println("failed to register identity", err)
	}
}

func (f *clientFixture) setup() *fabsdk.FabricSDK {
	var lis net.Listener
	var err error
	if !caServer.Running() {
		lis, err = net.Listen("tcp", strings.TrimPrefix(caServerURLListen,
			"http://"))
		if err != nil {
			panic(fmt.Sprintf("Error starting CA Server %s", err))
		}

		caServerURL = "http://" + lis.Addr().String()
	}

	configPath := filepath.Join(DefaultHome, configFile)
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
	ctxProvider := sdk.Context()
	ctx, err := ctxProvider()
	if err != nil {
		fmt.Println(err)
	}

	// Start Http Server if it's not running
	if !caServer.Running() {
		caServer.Start(lis, ctx.CryptoSuite())
	}
	return sdk
}
