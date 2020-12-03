pipeline {
    agent any

    parameters {
        string(name: 'IMAGE_PEER', defaultValue: "${env.DOCKER_REGISTRY}/twbc/fabric-peer-gm:latest")
        string(name: 'IMAGE_ORDERER', defaultValue: "${env.DOCKER_REGISTRY}/twbc/fabric-orderer-gm:latest")
        string(name: 'IMAGE_CA', defaultValue: "${env.DOCKER_REGISTRY}/twbc/fabric-ca-gm:latest")
        string(name: 'IMAGE_TOOLS', defaultValue: "${env.DOCKER_REGISTRY}/twbc/fabric-tools-gm:latest")
        string(name: 'IMAGE_CCENV', defaultValue: "${env.DOCKER_REGISTRY}/twbc/fabric-ccenv-gm:latest")
        choice(name: 'BYFN_CA', choices: ['no', 'yes'])
        string(name: 'START_FABCAR_TIMEOUT_MINS', defaultValue: '5')
    }

    environment {
        IMAGE_PEER = "${params.IMAGE_PEER}"
        IMAGE_ORDERER = "${params.IMAGE_ORDERER}"
        IMAGE_CA = "${params.IMAGE_CA}"
        IMAGE_TOOLS = "${params.IMAGE_TOOLS}"
        IMAGE_CCENV = "${params.IMAGE_CCENV}"
        BYFN_CA = "${params.BYFN_CA}"
        ZHONGHUAN_CE_CONFIG = credentials('ZHONGHUAN_CE_CONFIG')
        ALIBABA_CLOUD_ACCESS_KEY_ID = credentials('ALIBABA_CLOUD_ACCESS_KEY_ID')
        ALIBABA_CLOUD_ACCESS_KEY_SECRET = credentials('ALIBABA_CLOUD_ACCESS_KEY_SECRET')
    }

    stages {
        stage('Prepare') {

            steps {
                sh 'aws ecr get-login-password | docker login --username AWS --password-stdin ${DOCKER_REGISTRY}'
                sh '''
                docker pull $IMAGE_PEER
                docker pull $IMAGE_ORDERER
                docker pull $IMAGE_CA
                docker pull $IMAGE_TOOLS
                docker pull $IMAGE_CCENV
                '''

                echo "Clean fabcar"
                sh '''
                make fabcar-clean
                '''
            }

        }

        stage('Start Fabric') {

            steps {

                echo "Start fabcar"
                timeout(params.START_FABCAR_TIMEOUT_MINS as int) {
                    sh '''
                    make fabcar
                    '''
                }
            }

        }

        stage('Test Chaincode') {

            steps {

                echo "Test Chaincode"
                sh '''
                docker exec cli peer chaincode query -C mychannel -n fabcar -c '{"Args":["queryAllCars"]}'
                '''

            }

        }
	
        stage('Test Go SDK') {

            steps {

                echo "Test Go SDK"
                sh '''
                make sdk-test
                '''

            }

        }
    }

    post {

        always {

            echo "Clean fabcar"
            sh '''
            ./scripts/ci_scripts/test_fabcar.sh ./stopFabric.sh
            '''

        }
    }
}

