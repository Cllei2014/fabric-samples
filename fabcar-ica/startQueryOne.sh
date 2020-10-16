#!/bin/bash
set -e

docker exec cli peer chaincode query -C mychannel -n fabcar -c '{"Args":["queryCar", "CAR13"]}'
