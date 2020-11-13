package main

import (
	"fabcar/lib"
	"fmt"
)

var caServerURL string

const (
	configFile = "../../first-network/connection-org1.yaml"
)

func main() {

	// Initiate the sdk using the config file
	client := lib.ClientFixture{}
	//create the CA instance
	sdk := client.Setup(configFile)

	fmt.Printf("------- RegisterlUser %s------\n", "yin")
	lib.RegisterlUser(sdk, "yin1", "yin1", "")
}
