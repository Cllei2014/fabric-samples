#!/bin/bash

SCRIP_PATH=$(readlink -f "$(dirname "$0")")

export UID=$(id -u)
export GID=$(getent group docker | cut -d ':' -f 3)

docker build --build-arg IMAGE_CA=${IMAGE_CA-hyperledger/fabric-ca-gm:latest} \
             --build-arg IMAGE_TOOLS=${IMAGE_TOOLS-hyperledger/fabric-tools-gm:latest} \
             -t hyperledger/fabric-tools-ca-gm:latest \
             - < $SCRIP_PATH/Dockerfile

docker run --rm \
    -u $UID:$GID \
    -v "$PWD:$PWD" \
    -v "$(which docker):$(which docker)" \
    -v "$(which docker-compose):$(which docker-compose)" \
    -v "/var/run/docker.sock:/var/run/docker.sock" \
    -w "$PWD/fabcar" \
    -e "IMAGE_PEER" \
    -e "IMAGE_ORDERER" \
    -e "IMAGE_CA" \
    -e "IMAGE_TOOLS" \
    -e "IMAGE_CCENV" \
    -e "BYFN_CA" \
    --network host \
    hyperledger/fabric-tools-ca-gm:latest \
    $1
