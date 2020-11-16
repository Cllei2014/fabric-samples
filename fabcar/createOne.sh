#!/bin/bash
set -e

docker exec cli peer chaincode invoke -C mychannel -n fabcar -c '{"Args":["createCar", "CAR13", "Honda", "Accord", "black", "Tom"]}' --cafile /opt/gopath/src/github.com/hyperledger/fabric/peer/crypto/ordererOrganizations/example.com/orderers/orderer.example.com/msp/tlscacerts/tlsca.example.com-cert.pem --tls true --tlsRootCertFiles /opt/gopath/src/github.com/hyperledger/fabric/peer/crypto/peerOrganizations/org1.example.com/peers/peer0.org1.example.com/tls/ca.crt --tlsRootCertFiles /opt/gopath/src/github.com/hyperledger/fabric/peer/crypto/peerOrganizations/org2.example.com/peers/peer0.org2.example.com/tls/ca.crt --peerAddresses peer0.org1.example.com:7051 --peerAddresses peer0.org2.example.com:9051
