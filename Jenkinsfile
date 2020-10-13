pipeline {
    agent any
    parameters {
        string(name: 'IMAGE_PEER', defaultValue: "${DOCKER_REGISTRY}/twbc/fabric-peer-gm:latest")
        string(name: 'IMAGE_ORDERER', defaultValue: "${DOCKER_REGISTRY}/twbc/fabric-orderer-gm:latest")
        string(name: 'IMAGE_CA', defaultValue: "${DOCKER_REGISTRY}/twbc/fabric-ca-gm:latest")
        string(name: 'IMAGE_TOOLS', defaultValue: "${DOCKER_REGISTRY}/twbc/fabric-tools-gm:latest")
    }
    stages {
        stage('Test Fabcar') {
            environment {
                IMAGE_PEER = params.IMAGE_PEER
                IMAGE_ORDERER = params.IMAGE_ORDERER
                IMAGE_CA = params.IMAGE_CA
                IMAGE_TOOLS = params.IMAGE_TOOLS
                BYFN_CA = "no"
            }

            steps {
                sh 'aws ecr get-login-password | docker login --username AWS --password-stdin ${DOCKER_REGISTRY}'

                echo "Clean fabcar"
                sh '''
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
                    -e "BYFN_CA" \
                    $IMAGE_TOOLS \
                    ./stopFabric.sh
                '''

                echo "Start fabcar"
                sh '''
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
                    -e "BYFN_CA" \
                    $IMAGE_TOOLS \
                    ./startFabric.sh
                '''

                echo "Clean fabcar"
                sh '''
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
                    -e "BYFN_CA" \
                    $IMAGE_TOOLS \
                    ./stopFabric.sh
                '''
            }
        }
    }
}

