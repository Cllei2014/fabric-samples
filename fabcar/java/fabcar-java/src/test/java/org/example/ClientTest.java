/*
SPDX-License-Identifier: Apache-2.0
*/

package org.example;

import org.hyperledger.fabric.sdk.security.CryptoSM;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.junit.Assert;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class ClientTest {
    CryptoSM cryptoSuite = new CryptoSM();

    public ClientTest() throws IllegalAccessException, InstantiationException, ClassNotFoundException {
    }

    @Test
    public void test_sm2() throws Exception {
        String signString = "yin";
//        byte[] signStringByte = cryptoSuite.hash(signString.getBytes(UTF_8));
        byte[] signStringByte = signString.getBytes(UTF_8);

        Base64.Encoder b64 = Base64.getEncoder();

        byte[] bytes = Files.readAllBytes(Paths.get(System.getProperty("user.dir") + "/src/test/resources/sm2PriKeyPkcs8.pem"));
        PrivateKey pk = cryptoSuite.bytesToPrivateKey(bytes);

        byte[] signature = cryptoSuite.sign(pk, signStringByte);
        String sign = b64.encodeToString(signature);
        String go = "MEUCIA32FIc/5eGl+coquBhnjPQUDyrEXkX4SO4Q3bruD/+XAiEA8rNFsy6i2D91TibSMgu9Z45rKTKk7yKOK4InUfYLC+k=";

        byte[] bytesPub = Files.readAllBytes(Paths.get(System.getProperty("user.dir") + "/src/test/resources/sm2PubKey.pem"));
        PublicKey publicKey = cryptoSuite.bytesToPublicKey(bytesPub);

        Base64.Decoder decoder = Base64.getDecoder();

        boolean verifyJava = cryptoSuite.verify(publicKey, "1234567812345678".getBytes(), signStringByte, decoder.decode(sign));
        boolean verifyGo = cryptoSuite.verify(publicKey, "1234567812345678".getBytes(), signStringByte, decoder.decode(go));
        Assert.assertTrue("java verified", verifyJava);
        Assert.assertTrue("go verified", verifyGo);
    }

    @Test
    public void should_read_private_key_from_file() throws Exception {
        assertDoesNotThrow(() -> {
            byte[] bytes = Files.readAllBytes(Paths.get(System.getProperty("user.dir") + "/src/test/resources/user_1015_1-priv"));
            cryptoSuite.bytesToPrivateKey(bytes);
        });

        assertDoesNotThrow(() -> {
            byte[] bytes = Files.readAllBytes(Paths.get(System.getProperty("user.dir") + "/src/test/resources/admin-priv"));
            cryptoSuite.bytesToPrivateKey(bytes);
        });
    }
}
