/*
SPDX-License-Identifier: Apache-2.0
*/

package org.example;

import org.hyperledger.fabric.gateway.Contract;
import org.hyperledger.fabric.gateway.Gateway;
import org.hyperledger.fabric.gateway.Network;
import org.hyperledger.fabric.gateway.Wallet;
import org.hyperledger.fabric.sdk.helper.Config;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Security;

public class ClientApp {
    static String userName = RegisterUser.userName;

    static {
        System.setProperty("org.hyperledger.fabric.sdk.service_discovery.as_localhost", "true");
        Path currentPath = Paths.get("fabcar-java", "config.properties");
        System.setProperty(Config.ORG_HYPERLEDGER_FABRIC_SDK_CONFIGURATION, currentPath.toAbsolutePath().toString());

        // 设置hash就能切换整个密码库了
        System.setProperty(Config.HASH_ALGORITHM, "SM3");

        //java发送https请求出现Unknown named curve: 1.2.156.10197.1.301
        Security.removeProvider("SunEC");
    }

    public static void main(String[] args) throws Exception {
        // Load a file system based wallet for managing identities.
        Path walletPath = Paths.get("wallet");
        Wallet wallet = Wallet.createFileSystemWallet(walletPath);

        // load a CCP
        Path networkConfigPath = Paths.get("..", "..", "first-network-ica", "connection-org1.yaml");

        Gateway.Builder builder = Gateway.createBuilder();
        builder.identity(wallet, userName).networkConfig(networkConfigPath).discovery(true);

        // create a gateway connection
        try (Gateway gateway = builder.connect()) {

            // get the network and contract
            Network network = gateway.getNetwork("mychannel");
            Contract contract = network.getContract("fabcar");

            byte[] result;

            result = contract.evaluateTransaction("queryAllCars");
            System.out.println(new String(result));

            contract.submitTransaction("createCar", "CAR10", "VW", "Polo", "Grey", "Mary");

            result = contract.evaluateTransaction("queryCar", "CAR10");
            System.out.println(new String(result));

            contract.submitTransaction("changeCarOwner", "CAR10", "Archie");

            result = contract.evaluateTransaction("queryCar", "CAR10");
            System.out.println(new String(result));
        }
    }

}
