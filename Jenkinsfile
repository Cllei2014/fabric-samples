pipeline {
    agent any
    parameters {
        string(name: 'IMAGE_PEER', defaultValue: "${env.DOCKER_REGISTRY}/twbc/fabric-peer-gm:latest")
        string(name: 'IMAGE_ORDERER', defaultValue: "${env.DOCKER_REGISTRY}/twbc/fabric-orderer-gm:latest")
        string(name: 'IMAGE_CA', defaultValue: "${env.DOCKER_REGISTRY}/twbc/fabric-ca-gm:latest")
        string(name: 'IMAGE_TOOLS', defaultValue: "${env.DOCKER_REGISTRY}/twbc/fabric-tools-gm:latest")
    }
    stages {
        stage('Test Fabcar') {
            environment {
                IMAGE_PEER = "${params.IMAGE_PEER}"
                IMAGE_ORDERER = "${params.IMAGE_ORDERER}"
                IMAGE_CA = "${params.IMAGE_CA}"
                IMAGE_TOOLS = "${params.IMAGE_TOOLS}"
                BYFN_CA = "no"
            }

            steps {
                sh 'aws ecr get-login-password | docker login --username AWS --password-stdin ${DOCKER_REGISTRY}'

                echo "Clean fabcar"
                sh '''
                ./scripts/ci_scripts/test_fabcar.sh ./stopFabric.sh
                '''

                echo "Start fabcar"
                sh '''
                ./scripts/ci_scripts/test_fabcar.sh ./startFabric.sh
                '''

                echo "Clean fabcar"
                sh '''
                ./scripts/ci_scripts/test_fabcar.sh ./stopFabric.sh
                '''
            }
        }
    }
}

