#!/bin/bash
set -e
echo "$(go version)"

echo "cd go/"
cd go

echo "go run main.go"
go run main.go
