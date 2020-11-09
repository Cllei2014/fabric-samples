package main

import (
	"fabcar/lib"
	"fmt"
)

var caServerURL string

const (
	configFile = "/Users/yin/projects/fabric/fabric-samples/first-network/connection-org1.yaml"
)

func main() {

	// Initiate the sdk using the config file
	client := lib.ClientFixture{}
	//create the CA instance
	sdk := client.Setup(configFile)

	fmt.Printf("------- EnrollUser %s------\n", "admin")
	lib.EnrollUser(sdk, "admin", "adminpw")
}
