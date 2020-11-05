/*
 *
 *  Copyright 2016,2017 DTCC, Fujitsu Australia Software Technology, IBM - All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.hyperledger.fabric.sdk.transaction;

import com.google.common.base.Charsets;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Timestamp;
import org.hyperledger.fabric.protos.common.Common;
import org.hyperledger.fabric.protos.msp.Identities;
import org.hyperledger.fabric.protos.peer.FabricProposal;
import org.hyperledger.fabric.sdk.security.CryptoSM;
import org.junit.Assert;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.PublicKey;
import java.util.Calendar;
import java.util.Date;

import static org.hyperledger.fabric.sdk.transaction.ProtoUtils.getCurrentFabricTimestamp;
import static org.hyperledger.fabric.sdk.transaction.ProtoUtils.getDateFromTimestamp;
import static org.hyperledger.fabric.sdk.transaction.ProtoUtils.getTimestampFromDate;

public class ProtoUtilsTest {

    @Test
    public void timeStampDrill() throws Exception {

        final long millis = System.currentTimeMillis();

        //Test values over 2seconds
        for (long start = millis; start < millis + 2010; ++start) {
            Timestamp ts = Timestamp.newBuilder().setSeconds(start / 1000)
                .setNanos((int) ((start % 1000) * 1000000)).build();

            Date dateFromTimestamp = getDateFromTimestamp(ts);
            //    System.out.println(dateFromTimestamp);
            Date expectedDate = new Date(start);
            //Test various formats to make sure...
            Assert.assertEquals(expectedDate, dateFromTimestamp);
            Assert.assertEquals(expectedDate.getTime(), dateFromTimestamp.getTime());
            Assert.assertEquals(expectedDate.toString(), dateFromTimestamp.toString());
            //Now reverse it
            Timestamp timestampFromDate = getTimestampFromDate(expectedDate);
            Assert.assertEquals(ts, timestampFromDate);
            Assert.assertEquals(ts.getNanos(), timestampFromDate.getNanos());
            Assert.assertEquals(ts.getSeconds(), timestampFromDate.getSeconds());
            Assert.assertEquals(ts.toString(), timestampFromDate.toString());

        }

    }

    @Test
    public void timeStampCurrent() throws Exception {
        final int skew = 200;  // need some skew here as we are not getting the times at same instance.

        Calendar before = Calendar.getInstance(); // current time.

        final Date currentDateTimestamp = getDateFromTimestamp(getCurrentFabricTimestamp());
        Calendar after = (Calendar) before.clone(); // another copy.

        before.add(Calendar.MILLISECOND, -skew);
        after.add(Calendar.MILLISECOND, skew);
        Assert.assertTrue(before.getTime().before(currentDateTimestamp));
        Assert.assertTrue(after.getTime().after(currentDateTimestamp));
    }

    @Test
    public void verifyMarshalSignedProposalIsGood() throws Exception {
        byte[] bytes = Files.readAllBytes(Paths.get(System.getProperty("user.dir") + "/diagnostic/protobuf_2020-10-27T09-35-11_921P57073_1_4.proto"));

        FabricProposal.SignedProposal signedProposal = FabricProposal.SignedProposal.parseFrom(bytes);

        ByteString proposalBytes = signedProposal.getProposalBytes();

        String pubKey = "-----BEGIN PUBLIC KEY-----\n" +
            "MFkwEwYHKoZIzj0CAQYIKoEcz1UBgi0DQgAEAcOGZXl1Rr+hLesCMFpuvt6eUWi8\n" +
            "5XW4qSfDAZ+SVcR5TP7K1rn5e446HbUh7D+ADE5qSc9TnMLaiZf51vgxgQ==\n" +
            "-----END PUBLIC KEY-----\n";
        CryptoSM cryptoSuite = new CryptoSM();
        PublicKey publicKey = cryptoSuite.bytesToPublicKey(pubKey.getBytes(Charsets.UTF_8));
        boolean ok = new CryptoSM().verify(publicKey, "1234567812345678".getBytes(), proposalBytes.toByteArray(), signedProposal.getSignature().toByteArray());

        Assert.assertTrue(ok);
    }

    private void parseSignedProposal(FabricProposal.SignedProposal signedProposal) throws InvalidProtocolBufferException {
        ByteString proposalBytes = signedProposal.getProposalBytes();

        FabricProposal.Proposal proposal = FabricProposal.Proposal.parseFrom(proposalBytes);
        ByteString header = proposal.getHeader();
        Common.SignatureHeader signatureHeader = Common.SignatureHeader.parseFrom(header);
        ByteString creator = signatureHeader.getCreator();
        Identities.SerializedIdentity identity = Identities.SerializedIdentity.parseFrom(creator);

        // TODO: identity is null but should not be null!
    }

}
