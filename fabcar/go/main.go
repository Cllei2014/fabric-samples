package main

import (
	"fmt"
	"os"

	"github.com/tw-bc-group/fabric-samples/fabcar/go/lib"
)

const (
	configFile = "connection-org1.tls.yaml"
)

func main() {
	// Initiate the sdk using the config file
	client := lib.ClientFixture{}
	//create the CA instance
	sdk := client.Setup(configFile)

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

	lib.QueryAllCars(contract)
	carName := "Car10"
	lib.CreateCarAndSelectIt(contract, carName)
	owner := "Archie"
	lib.ChangeOwnAndSelectIt(contract, carName, owner)
}
