#!/bin/bash
set -e
echo "$(go version)"

echo "cd go/"
cd go
echo "please make sure pointing domain name to 127.0.0.1 in /etc/hosts"
echo "127.0.0.1       orderer.example.com"
echo "127.0.0.1       peer0.org1.example.com"
echo "127.0.0.1       peer1.org1.example.com"
echo "127.0.0.1       peer0.org2.example.com"
echo "127.0.0.1       peer1.org2.example.com"
echo "127.0.0.1       ca.org1.example.com"
echo "127.0.0.1       ca.org2.example.com"

echo "go run main.go"
go run main.go
