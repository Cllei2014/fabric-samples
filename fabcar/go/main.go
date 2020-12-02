package main

import (
	"fabcar/lib"
	"fmt"
	"github.com/tw-bc-group/fabric-sdk-go-gm/pkg/client/ledger"
	"github.com/tw-bc-group/fabric-sdk-go-gm/pkg/fabsdk"
	"os"
)

const (
	configFile = "connection-org1.tls.yaml"
)

func main() {
	// Initiate the sdk using the config file
	client := lib.ClientFixture{}
	//create the CA instance
	sdk := client.Setup(configFile)
	defer sdk.Close()

	fmt.Printf("------- EnrollUser %s------\n", "admin")
	_, err := lib.EnrollUser(sdk, "admin", "adminpw")
	if err != nil {
		fmt.Printf("Failed to EnrollUser: %s\n", err)
		os.Exit(1)
	}

	fmt.Printf("------- RegisterlUser %s------\n", "yin")
	lib.RegisterlUser(sdk, "yin", "yin", "")

	err, contract := lib.GetContract(configFile, "yin-wallet")

	if err != nil {
		fmt.Printf("Failed to GetContract: %s\n", err)
		os.Exit(1)
	}

	fmt.Printf("------- QueryAllCars ------\n")
	lib.QueryAllCars(contract)

	carName := "Car10"
	fmt.Printf("------- CreateCarAndSelectIt %s------\n", carName)
	lib.CreateCarAndSelectIt(contract, carName)

	owner := "Archie"
	fmt.Printf("------- ChangeOwnAndSelectIt owner：%s------\n", owner)
	lib.ChangeOwnAndSelectIt(contract, carName, owner)

	fmt.Printf("------- QueryBlock ------\n")
	org1AdminChannelContext := sdk.ChannelContext("mychannel", fabsdk.WithUser("admin"), fabsdk.WithOrg("Org1"))
	cli, err := ledger.New(org1AdminChannelContext)
	if err != nil {
		fmt.Printf("ledger.New Failed to : %s\n", err)
		os.Exit(1)
	}
	block, err := cli.QueryBlock(1)
	fmt.Printf("block metadata : %s\n", block.Metadata.String())

	if err != nil {
		fmt.Printf("QueryBlock Failed to : %s\n", err)
		os.Exit(1)
	}
}
