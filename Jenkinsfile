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
                IMAGE_PEER = "${DOCKER_REGISTRY}/${params.IMAGE_PEER}
                IMAGE_ORDERER = "${DOCKER_REGISTRY}/${params.IMAGE_ORDERER}
                IMAGE_CA = "${DOCKER_REGISTRY}/${params.IMAGE_CA}
                IMAGE_TOOLS = "${DOCKER_REGISTRY}/${params.IMAGE_TOOLS}
            }

            steps {
                dir('fabcar') {
                    sh './stopFabric.sh'
                    sh './startFabric.sh'
                    sh './stopFabric.sh'
                }
            }
        }
    }
}
