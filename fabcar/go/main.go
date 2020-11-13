package main

import (
	"fabcar/lib"
	"fmt"
	"os"
)

const (
	configFile = "../../first-network/connection-org1.yaml"
)

func main() {
	// Initiate the sdk using the config file
	client := lib.ClientFixture{}
	//create the CA instance
	sdk := client.Setup(configFile)

	fmt.Printf("------- EnrollUser %s------\n", "admin")
	lib.EnrollUser(sdk, "admin", "adminpw")

	fmt.Printf("------- RegisterlUser %s------\n", "yin")
	lib.RegisterlUser(sdk, "yin1", "yin1", "")

	err, contract := lib.GetContract()

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
