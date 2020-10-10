/*
SPDX-License-Identifier: Apache-2.0
*/

package org.example;

import org.hyperledger.fabric.gateway.Wallet;
import org.hyperledger.fabric.gateway.Wallet.Identity;
import org.hyperledger.fabric.sdk.Enrollment;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.hyperledger.fabric.sdk.security.CryptoSuiteFactory;
import org.hyperledger.fabric_ca.sdk.HFCAClient;

import java.nio.file.Paths;
import java.util.Properties;

public class EnrollAdmin {


    static {
        System.setProperty("org.hyperledger.fabric.sdk.service_discovery.as_localhost", "true");
    }

    public static Enrollment notls() {
        Enrollment enrollment = null;

        try {
            HFCAClient caClient = HfcaClientFactory.getHfcaClient();
            enrollment = caClient.enroll("admin", "adminpw");
            System.out.println(caClient.getCAName());
            System.out.println(enrollment.getKey());
            System.out.println(enrollment.getCert());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return enrollment;
    }



    public static Enrollment tls() {
        Enrollment enrollment = null;

        try {

            // Create a CA client for interacting with the CA.
            Properties props = new Properties();
            props.put("pemFile", "../../first-network/crypto-config/peerOrganizations/org1.example.com/ca-config/ca-cert.pem");
            props.put("allowAllHostNames", "true");
            HFCAClient caClient = HFCAClient.createNewInstance("http://localhost:7054", props);
            CryptoSuite cryptoSuite = CryptoSuiteFactory.getDefault().getCryptoSuite();
//            CryptoSuite cryptoSuite = new CryptoSM();

            caClient.setCryptoSuite(cryptoSuite);


            // Enroll the admin user, and import the new identity into the wallet.
//        final EnrollmentRequest enrollmentRequestTLS = new EnrollmentRequest();
//        enrollmentRequestTLS.addHost("localhost");
//        enrollmentRequestTLS.setProfile("tls");
//		Enrollment enrollment = caClient.enroll("admin", "adminpw", enrollmentRequestTLS);

            enrollment = caClient.enroll("admin", "adminpw");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return enrollment;

    }


    public static void main(String[] args) throws Exception {

        Enrollment enrollment = notls();
//        Enrollment enrollment = tls();

        // Create a wallet for managing identities
        Wallet wallet = Wallet.createFileSystemWallet(Paths.get("wallet"));

        // Check to see if we've already enrolled the admin user.
        boolean adminExists = wallet.exists("admin");
        if (adminExists) {
            System.out.println("An identity for the admin user \"admin\" already exists in the wallet");
            return;
        }

        Identity user = Identity.createIdentity("Org1MSP", enrollment.getCert(), enrollment.getKey());
        wallet.put("admin", user);
        System.out.println("Successfully enrolled user \"admin\" and imported it into the wallet");
    }
}
