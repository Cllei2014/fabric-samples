/*
Copyright 2020 IBM All Rights Reserved.

SPDX-License-Identifier: Apache-2.0
*/

package main

import (
	"errors"
	"fmt"
	"github.com/tw-bc-group/fabric-sdk-go-gm/pkg/core/config"
	"github.com/tw-bc-group/fabric-sdk-go-gm/pkg/gateway"
	"io/ioutil"
	"os"
	"path/filepath"
)

func main() {
	os.Setenv("DISCOVERY_AS_LOCALHOST", "true")
	wallet, err := gateway.NewFileSystemWallet("wallet")
	if err != nil {
		fmt.Printf("Failed to create wallet: %s\n", err)
		os.Exit(1)
	}

	if !wallet.Exists("appUser") {
		err = populateWallet(wallet)
		if err != nil {
			fmt.Printf("Failed to populate wallet contents: %s\n", err)
			os.Exit(1)
		}
	}

	//configFile = "/Users/yin/projects/fabric/fabric-samples/first-network/connection-org1.yaml"

	ccpPath := filepath.Join(
		"..",
		"..",
		"first-network",
		"connection-org1.yaml",
	)

	gw, err := gateway.Connect(
		gateway.WithConfig(config.FromFile(filepath.Clean(ccpPath))),
		gateway.WithIdentity(wallet, "appUser"),
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

	queryAllCars(err, contract)
	carName := "Car10"
	createCar(err, contract, carName)
	owner := "Archie"
	changeOwn(err, contract, carName, owner)
}

func changeOwn(err error, contract *gateway.Contract, carName string, owner string) {
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

func createCar(err error, contract *gateway.Contract, carName string) {
	fmt.Printf("------- createCar %s------\n", carName)

	result, err := contract.SubmitTransaction("createCar", carName, "VW", "Polo", "Grey", "Mary")
	if err != nil {
		fmt.Printf("Failed to submit transaction: %s\n", err)
		os.Exit(1)
	}
	fmt.Println(string(result))

	fmt.Printf("------- queryCar %s ------\n", carName)

	result, err = contract.EvaluateTransaction("queryCar", carName)
	if err != nil {
		fmt.Printf("Failed to evaluate transaction: %s\n", err)
		os.Exit(1)
	}
	fmt.Println(string(result))
}

func queryAllCars(err error, contract *gateway.Contract) {
	fmt.Printf("------- queryAllCars ------\n")

	result, err := contract.EvaluateTransaction("queryAllCars")
	if err != nil {
		fmt.Printf("Failed to evaluate transaction: %s\n", err)
		os.Exit(1)
	}
	fmt.Println(string(result))
}

func populateWallet(wallet *gateway.Wallet) error {
	///Users/yin/projects/fabric/fabric-samples/first-network/crypto-config/peerOrganizations/org1.example.com/users/User1@org1.example.com/msp/signcerts/cert.pem
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

	err = wallet.Put("appUser", identity)
	if err != nil {
		return err
	}
	return nil
}
