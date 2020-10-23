#!/bin/bash
set -e

docker exec cli peer chaincode invoke -C mychannel -n fabcar -c '{"Args":["createCar", "CAR13", "Honda", "Accord", "black", "Tom"]}' --peerAddresses peer0.org1.example.com:7051 --peerAddresses peer0.org2.example.com:9051
