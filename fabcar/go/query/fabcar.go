/*
Copyright 2020 IBM All Rights Reserved.

SPDX-License-Identifier: Apache-2.0
*/

package main

import (
	"fabcar/lib"
	"fmt"
	"os"
)

func main() {
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
