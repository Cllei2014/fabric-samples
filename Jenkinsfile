pipeline {
    agent any
    parameters {
        string(name: 'IMAGE_PEER', defaultValue: 'twbc/fabric-peer-gm:latest')
        string(name: 'IMAGE_ORDERER', defaultValue: 'twbc/fabric-orderer-gm:latest')
        string(name: 'IMAGE_CA', defaultValue: 'twbc/fabric-ca-gm:latest')
        string(name: 'IMAGE_TOOLS', defaultValue: 'twbc/fabric-tools-gm:latest')
    }
    stages {
        stage('Test Fabcar') {
            environment {
                IMAGE_PEER = "${DOCKER_REGISTRY}/${params.IMAGE_PEER}"
                IMAGE_ORDERER = "${DOCKER_REGISTRY}/${params.IMAGE_ORDERER}"
                IMAGE_CA = "${DOCKER_REGISTRY}/${params.IMAGE_CA}"
                IMAGE_TOOLS = "${DOCKER_REGISTRY}/${params.IMAGE_TOOLS}"
                BYFN_CA = "no"
            }

            steps {
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

