package org.example;

import org.hyperledger.fabric.sdk.security.CryptoSM;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.hyperledger.fabric_ca.sdk.HFCAClient;

import java.net.MalformedURLException;
import java.util.Properties;

public class HfcaClientFactory {
    public static HFCAClient getHfcaClient() throws MalformedURLException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        String caUrl = "http://127.0.0.1:7054";
        Properties props = new Properties();
        props.put("providerName", "GM");

        HFCAClient caClient = HFCAClient.createNewInstance(caUrl, props);
        CryptoSuite cryptoSuite = new CryptoSM();
        caClient.setCryptoSuite(cryptoSuite);
        return caClient;
    }
}
