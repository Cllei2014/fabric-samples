#!/bin/bash

docker run --rm \
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
    ${IMAGE_TOOLS:-hyperledger/fabric-tools-gm} \
    $1
