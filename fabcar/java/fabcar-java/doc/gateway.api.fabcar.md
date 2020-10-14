# gatway 接口说明

本文档的目的是说明gateway的作用，吃透代码，为写更好的测试做准备。

本文档是根据集成测试写出来的。

## 接口列表

### 1.fabcar合约

#### Feature: Configure Fabric using SDK and submit/evaluate using a network Gateway
	Background:
#####		Given I have deployed a tls Fabric network

```
	    if (fabricNetworkType.equals("tls")) {
                            tlsOptions = Arrays.asList("--tls", "true", "--cafile", "/etc/hyperledger/configtx/crypto-config/ordererOrganizations/example.com/tlsca/tlsca.example.com-cert.pem");
                        } else {
                            tlsOptions = Collections.emptyList();
                        }
    
                        String ccPath = Paths.get(FileSystems.getDefault().getSeparator(),
                                "opt", "gopath", "src", "github.com", "chaincode", ccType, ccName).toString();
    
                        exec("docker", "exec", "org1_cli", "peer", "chaincode", "install",
                                "-l", ccType,
                                "-n", ccName,
                                "-v", version,
                                "-p", ccPath
                        );
    
                        exec("docker", "exec", "org2_cli", "peer", "chaincode", "install",
                                "-l", ccType,
                                "-n", ccName,
                                "-v", version,
                                "-p", ccPath
                        );
```
		
#####		And I have created and joined all channels from the tls connection profile
```
 Given("I have created and joined all channels from the {word} connection profile", (String tlsType) -> {
            // TODO this only does mychannel
            if (!channelsJoined) {
                final List<String> tlsOptions;
                if (tlsType.equals("tls")) {
                    tlsOptions = Arrays.asList("--tls", "true", "--cafile", "/etc/hyperledger/configtx/crypto-config/ordererOrganizations/example.com/tlsca/tlsca.example.com-cert.pem");
                } else {
                    tlsOptions = Collections.emptyList();
                }

                List<String> createChannelCommand = new ArrayList<>();
                Collections.addAll(createChannelCommand,
                        "docker", "exec", "org1_cli", "peer", "channel", "create",
                        "-o", "orderer.example.com:7050",
                        "-c", "mychannel",
                        "-f", "/etc/hyperledger/configtx/channel.tx",
                        "--outputBlock", "/etc/hyperledger/configtx/mychannel.block");
                createChannelCommand.addAll(tlsOptions);
                exec(createChannelCommand);

                List<String> org1JoinChannelCommand = new ArrayList<>();
                Collections.addAll(org1JoinChannelCommand,
                        "docker", "exec", "org1_cli", "peer", "channel", "join",
                        "-b", "/etc/hyperledger/configtx/mychannel.block"
                );
                org1JoinChannelCommand.addAll(tlsOptions);
                exec(org1JoinChannelCommand);

                List<String> org2JoinChannelCommand = new ArrayList<>();
                Collections.addAll(org2JoinChannelCommand,
                        "docker", "exec", "org2_cli", "peer", "channel", "join",
                        "-b", "/etc/hyperledger/configtx/mychannel.block"
                );
                org2JoinChannelCommand.addAll(tlsOptions);
                exec(org2JoinChannelCommand);

                List<String> org1AnchorPeersCommand = new ArrayList<>();
                Collections.addAll(org1AnchorPeersCommand,
                        "docker", "exec", "org1_cli", "peer", "channel", "update",
                        "-o", "orderer.example.com:7050",
                        "-c", "mychannel",
                        "-f", "/etc/hyperledger/configtx/Org1MSPanchors.tx"
                );
                org1AnchorPeersCommand.addAll(tlsOptions);
                exec(org1AnchorPeersCommand);

                List<String> org2AnchorPeersCommand = new ArrayList<>();
                Collections.addAll(org2AnchorPeersCommand,
                        "docker", "exec", "org2_cli", "peer", "channel", "update",
                        "-o", "orderer.example.com:7050",
                        "-c", "mychannel",
                        "-f", "/etc/hyperledger/configtx/Org2MSPanchors.tx"
                );
                org2AnchorPeersCommand.addAll(tlsOptions);
                exec(org2AnchorPeersCommand);

                channelsJoined = true;
            }
        });
```

#####		And I deploy node chaincode named fabcar at version 1.0.0 for all organizations on channel mychannel with endorsement policy 1AdminOr2Other and arguments ["initLedger"]
```
 Given("I deploy {word} chaincode named {word} at version {word} for all organizations on channel {word} with endorsement policy {} and arguments {}",
                (String ccType, String ccName, String version, String channelName,
                 String policyType, String argsJson) -> {
                    String mangledName = ccName + version + channelName;
                    if (runningChaincodes.contains(mangledName)) {
                        return;
                    }

                    JsonArray functionAndArgs = parseJsonArray(argsJson);
                    String transactionName = functionAndArgs.getString(0);
                    JsonArray args = Json.createArrayBuilder(functionAndArgs).remove(0).build();
                    String initArg = Json.createObjectBuilder()
                            .add("function", transactionName)
                            .add("Args", args)
                            .build().toString();

                    final List<String> tlsOptions;
                    if (fabricNetworkType.equals("tls")) {
                        tlsOptions = Arrays.asList("--tls", "true", "--cafile", "/etc/hyperledger/configtx/crypto-config/ordererOrganizations/example.com/tlsca/tlsca.example.com-cert.pem");
                    } else {
                        tlsOptions = Collections.emptyList();
                    }

                    String ccPath = Paths.get(FileSystems.getDefault().getSeparator(),
                            "opt", "gopath", "src", "github.com", "chaincode", ccType, ccName).toString();

                    exec("docker", "exec", "org1_cli", "peer", "chaincode", "install",
                            "-l", ccType,
                            "-n", ccName,
                            "-v", version,
                            "-p", ccPath
                    );

                    exec("docker", "exec", "org2_cli", "peer", "chaincode", "install",
                            "-l", ccType,
                            "-n", ccName,
                            "-v", version,
                            "-p", ccPath
                    );

                    Thread.sleep(3000);

                    List<String> instantiateCommand = new ArrayList<>();
                    Collections.addAll(instantiateCommand,
                            "docker", "exec", "org1_cli", "peer", "chaincode", "instantiate",
                            "-o", "orderer.example.com:7050",
                            "-l", ccType,
                            "-C", channelName,
                            "-n", ccName,
                            "-v", version,
                            "-c", initArg,
                            "-P", "AND(\"Org1MSP.member\",\"Org2MSP.member\")"
                    );
                    instantiateCommand.addAll(tlsOptions);
                    exec(instantiateCommand);

                    runningChaincodes.add(mangledName);
                    Thread.sleep(60000);
                });
```


#### 	Scenario: Using a Gateway I can submit and evaluate transactions on instantiated node chaincode
#####		Given I have a gateway as user User1 using the tls connection profile
```
 Given("I have a gateway as user {word} using the {word} connection profile",
                (String userName, String tlsType) -> {
                    Wallet wallet = createWallet();
                    gatewayBuilder = Gateway.createBuilder();
                    gatewayBuilder.identity(wallet, userName);
                    gatewayBuilder.networkConfig(getNetworkConfigPath(tlsType));
                    gatewayBuilder.commitTimeout(1, TimeUnit.MINUTES);
                    if (tlsType.equals("discovery")) {
                        gatewayBuilder.discovery(true);
                    }
                });
```
#####		And I connect the gateway

```
 Given("I connect the gateway", () -> gateway = gatewayBuilder.connect());
```
#####		And I use the mychannel network
```
Given("I use the {word} network", (String networkName) -> network = gateway.getNetwork(networkName));
```
#####		And I use the fabcar contract
```
 Given("I use the {word} contract", (String contractName) -> contract = network.getContract(contractName));
```
#####		When I prepare a createCar transaction
```
  When("I prepare a(n) {word} transaction", (String transactionName) -> {
            Transaction transaction = contract.createTransaction(transactionName);
            transactionInvocation = TransactionInvocation.expectSuccess(transaction);
        });

```
#####	 	And I submit the transaction with arguments ["CAR10", "Trabant", "601 Estate", "brown", "Simon"]
```
 When("^I (submit|evaluate) the transaction with arguments (.+)$",
                (String action, String argsJson) -> {
                    String[] args = newStringArray(parseJsonArray(argsJson));
                    if (action.equals("submit")) {
                        transactionInvocation.submit(args);
                    } else {
                        transactionInvocation.evaluate(args);
                    }
                });
```
#####		And I prepare a queryCar transaction
```
 When("I prepare a(n) {word} transaction", (String transactionName) -> {
            Transaction transaction = contract.createTransaction(transactionName);
            transactionInvocation = TransactionInvocation.expectSuccess(transaction);
        });
```
#####	 	And I evaluate the transaction with arguments ["CAR10"]
```
When("^I (submit|evaluate) the transaction with arguments (.+)$",
            (String action, String argsJson) -> {
                String[] args = newStringArray(parseJsonArray(argsJson));
                if (action.equals("submit")) {
                    transactionInvocation.submit(args);
                } else {
                    transactionInvocation.evaluate(args);
                }
            });
```
#####		Then the response should be JSON matching
            """
		    {
		    	"color": "brown",
		    	"docType": "car",
		    	"make": "Trabant",
		    	"model": "601 Estate",
		    	"owner": "Simon"
		    }
		    """
```$xslt
 Then("the response should be JSON matching", (String expected) -> {
            try (JsonReader expectedReader = createJsonReader(expected);
                 JsonReader actualReader = createJsonReader(transactionInvocation.getResponse())) {
                JsonObject expectedObject = expectedReader.readObject();
                JsonObject actualObject = actualReader.readObject();
                assertThat(actualObject).isEqualTo(expectedObject);
            }
        });
```
		   


####	Scenario: Using a Gateway I can submit transactions with specific endorsing peers
#####		Given I have a gateway as user User1 using the tls connection profile
```
 Given("I have a gateway as user {word} using the {word} connection profile",
                (String userName, String tlsType) -> {
                    Wallet wallet = createWallet();
                    gatewayBuilder = Gateway.createBuilder();
                    gatewayBuilder.identity(wallet, userName);
                    gatewayBuilder.networkConfig(getNetworkConfigPath(tlsType));
                    gatewayBuilder.commitTimeout(1, TimeUnit.MINUTES);
                    if (tlsType.equals("discovery")) {
                        gatewayBuilder.discovery(true);
                    }
                });
```
#####		And I connect the gateway

```
 Given("I connect the gateway", () -> gateway = gatewayBuilder.connect());
```

#####		And I use the mychannel network
```
Given("I use the {word} network", (String networkName) -> network = gateway.getNetwork(networkName));
```
#####		And I use the fabcar contract
```
 Given("I use the {word} contract", (String contractName) -> contract = network.getContract(contractName));
```
#####		When I prepare a createCar transaction that I expect to fail
```
 When("I prepare a(n) {word} transaction that I expect to fail", (String transactionName) -> {
            Transaction transaction = contract.createTransaction(transactionName);
            transactionInvocation = TransactionInvocation.expectFail(transaction);
        });
```
#####		And I set endorsing peers on the transaction to ["badpeer.org1.example.com"]
```
When("I set endorsing peers on the transaction to {}", (String peersJson) -> {
            Set<String> peerNames = Arrays.stream(newStringArray(parseJsonArray(peersJson)))
                    .collect(Collectors.toSet());
            Collection<Peer> peers = network.getChannel().getPeers().stream()
                    .filter(peer -> peerNames.contains(peer.getName()))
                    .collect(Collectors.toList());
            transactionInvocation.setEndorsingPeers(peers);
        });
        
   public void setEndorsingPeers(Collection<Peer> peers) {
        transaction.setEndorsingPeers(peers);
    }
    
    
    // 尝试看一下 setEndorsingPeers 有什么用
    /Users/yin/projects/fabric/fabric-samples/fabcar/java/twbc-fabric-gateway-java/src/main/java/org/hyperledger/fabric/gateway/impl/TransactionImpl.java

 private Collection<ProposalResponse> sendTransactionProposal(final TransactionProposalRequest request)
            throws InvalidArgumentException, ServiceDiscoveryException, ProposalException {
        if (endorsingPeers != null) {
            return channel.sendTransactionProposal(request, endorsingPeers);
        } else if (network.getGateway().isDiscoveryEnabled()) {
            Channel.DiscoveryOptions discoveryOptions = createDiscoveryOptions()
                    .setEndorsementSelector(ServiceDiscovery.EndorsementSelector.ENDORSEMENT_SELECTION_RANDOM)
                    .setForceDiscovery(true);
            return channel.sendTransactionProposalToEndorsers(request, discoveryOptions);
        } else {
            return channel.sendTransactionProposal(request);
        }
    }
    
    /Users/yin/projects/fabric/fabric-samples/fabcar/java/twbc-fabric-sdk-java-gm/src/main/java/org/hyperledger/fabric/sdk/Channel.java
    
     private Collection<ProposalResponse> sendProposal(TransactionRequest proposalRequest, Collection<Peer> peers) throws
                InvalidArgumentException, ProposalException {
    
            checkChannelState();
            checkPeers(peers);
    
            if (null == proposalRequest) {
                throw new InvalidArgumentException("The proposalRequest is null");
            }
    
            if (isNullOrEmpty(proposalRequest.getFcn())) {
                throw new InvalidArgumentException("The proposalRequest's fcn is null or empty.");
            }
    
            if (proposalRequest.getChaincodeID() == null) {
                throw new InvalidArgumentException("The proposalRequest's chaincode ID is null");
            }
    
            try {
                TransactionContext transactionContext = getTransactionContext(proposalRequest.getUserContext());
                transactionContext.verify(proposalRequest.doVerify());
                transactionContext.setProposalWaitTime(proposalRequest.getProposalWaitTime());
    
                // Protobuf message builder
                ProposalBuilder proposalBuilder = ProposalBuilder.newBuilder();
                proposalBuilder.context(transactionContext);
                proposalBuilder.request(proposalRequest);
    
                SignedProposal invokeProposal = getSignedProposal(transactionContext, proposalBuilder.build());
                return sendProposalToPeers(peers, invokeProposal, transactionContext);
            } catch (ProposalException e) {
                throw e;
    
            } catch (Exception e) {
                ProposalException exp = new ProposalException(e);
                logger.error(exp.getMessage(), exp);
                throw exp;
            }
        }
        
        private Collection<ProposalResponse> sendProposalToPeers(Collection<Peer> peers,
                                                                     SignedProposal signedProposal,
                                                                     TransactionContext transactionContext) throws InvalidArgumentException, ProposalException {
                checkPeers(peers);
        
                if (transactionContext.getVerify()) {
                    try {
                        loadCACertificates(false);
                    } catch (Exception e) {
                        throw new ProposalException(e);
                    }
                }
        
                final String txID = transactionContext.getTxID();
        
                class Pair {
                    private final Peer peer;
        
                    private final Future<FabricProposalResponse.ProposalResponse> future;
        
                    private Pair(Peer peer, Future<FabricProposalResponse.ProposalResponse> future) {
                        this.peer = peer;
                        this.future = future;
                    }
                }
                List<Pair> peerFuturePairs = new ArrayList<>();
                for (Peer peer : peers) {
                    logger.debug(format("Channel %s send proposal to %s, txID: %s",
                            name, peer.toString(), txID));
        
                    if (null != diagnosticFileDumper) {
                        logger.trace(format("Sending to channel %s, peer: %s, proposal: %s, txID: %s", name, peer, txID,
                                diagnosticFileDumper.createDiagnosticProtobufFile(signedProposal.toByteArray())));
        
                    }
        
                    Future<FabricProposalResponse.ProposalResponse> proposalResponseListenableFuture;
                    try {
                        proposalResponseListenableFuture = peer.sendProposalAsync(signedProposal);
                    } catch (Exception e) {
                        proposalResponseListenableFuture = new CompletableFuture<>();
                        ((CompletableFuture) proposalResponseListenableFuture).completeExceptionally(e);
        
                    }
                    peerFuturePairs.add(new Pair(peer, proposalResponseListenableFuture));
        
                }
        
                Collection<ProposalResponse> proposalResponses = new ArrayList<>();
                for (Pair peerFuturePair : peerFuturePairs) {
        
                    FabricProposalResponse.ProposalResponse fabricResponse = null;
                    String message;
                    int status = 500;
                    final String peerName = peerFuturePair.peer.toString();
                    try {
                        fabricResponse = peerFuturePair.future.get(transactionContext.getProposalWaitTime(), TimeUnit.MILLISECONDS);
                        message = fabricResponse.getResponse().getMessage();
                        status = fabricResponse.getResponse().getStatus();
                        peerFuturePair.peer.setHasConnected();
                        logger.debug(format("Channel %s, transaction: %s got back from peer %s status: %d, message: %s",
                                name, txID, peerName, status, message));
                        if (null != diagnosticFileDumper) {
                            logger.trace(format("Got back from channel %s, peer: %s, proposal response: %s", name, peerName,
                                    diagnosticFileDumper.createDiagnosticProtobufFile(fabricResponse.toByteArray())));
        
                        }
                    } catch (InterruptedException e) {
                        message = "Sending proposal with transaction: " + txID + " to " + peerName + " failed because of interruption";
                        logger.error(message, e);
                    } catch (TimeoutException e) {
                        message = format("Channel %s sending proposal with transaction %s to %s failed because of timeout(%d milliseconds) expiration",
                                toString(), txID, peerName, transactionContext.getProposalWaitTime());
                        logger.error(message, e);
                    } catch (ExecutionException e) {
                        Throwable cause = e.getCause();
                        if (cause instanceof Error) {
                            String emsg = "Sending proposal with txID: " + txID + " to " + peerName + " failed because of " + cause.getMessage();
                            logger.error(emsg, new Exception(cause)); //wrapped in exception to get full stack trace.
                            throw (Error) cause;
                        } else {
                            if (cause instanceof StatusRuntimeException) {
                                message = format("Channel %s Sending proposal with transaction: %s to %s failed because of: gRPC failure=%s",
                                        toString(), txID, peerName, ((StatusRuntimeException) cause).getStatus());
                            } else {
                                message = format("Channel %s sending proposal with transaction: %s to %s failed because of: %s",
                                        toString(), txID, peerName, cause.getMessage());
                            }
                            logger.error(message, new Exception(cause)); //wrapped in exception to get full stack trace.
                        }
                    }
        
                    ProposalResponse proposalResponse = new ProposalResponse(transactionContext, status, message);
                    proposalResponse.setProposalResponse(fabricResponse);
                    proposalResponse.setProposal(signedProposal);
                    proposalResponse.setPeer(peerFuturePair.peer);
        
                    if (fabricResponse != null && transactionContext.getVerify()) {
                        proposalResponse.verify(client.getCryptoSuite());
                    }
        
                    proposalResponses.add(proposalResponse);
                }
        
                return proposalResponses;
            }
            
            /Users/yin/projects/fabric/fabric-samples/fabcar/java/twbc-fabric-sdk-java-gm/src/main/java/org/hyperledger/fabric/sdk/Peer.java
            
             ListenableFuture<FabricProposalResponse.ProposalResponse> sendProposalAsync(FabricProposal.SignedProposal proposal)
                    throws PeerException, InvalidArgumentException {
                    checkSendProposal(proposal);
            
                    if (IS_DEBUG_LEVEL) {
                        logger.debug(format("peer.sendProposalAsync %s", toString()));
                    }
            
                    EndorserClient localEndorserClient = getEndorserClient();
            
                    try {
                        return localEndorserClient.sendProposalAsync(proposal);
                    } catch (Throwable t) {
                        removeEndorserClient(true);
                        throw t;
                    }
                }
                
             /Users/yin/projects/fabric/fabric-samples/fabcar/java/twbc-fabric-sdk-java-gm/src/main/java/org/hyperledger/fabric/sdk/EndorserClient.java
                
               public ListenableFuture<FabricProposalResponse.ProposalResponse> sendProposalAsync(FabricProposal.SignedProposal proposal) throws PeerException {
                      if (shutdown) {
                          throw new PeerException("Shutdown " + toString());
                      }
                      return futureStub.processProposal(proposal);
                  }
                  
            /Users/yin/projects/fabric/fabric-samples/fabcar/java/twbc-fabric-sdk-java-gm/target/generated-sources/protobuf/grpc-java/org/hyperledger/fabric/protos/peer/EndorserGrpc.java
```
#####		And I submit the transaction with arguments ["ENDORSING_PEERS", "Trabant", "601 Estate", "brown", "Simon"]

```$xslt
When("^I (submit|evaluate) the transaction with arguments (.+)$",
                (String action, String argsJson) -> {
                    String[] args = newStringArray(parseJsonArray(argsJson));
                    if (action.equals("submit")) {
                        transactionInvocation.submit(args);
                    } else {
                        transactionInvocation.evaluate(args);
                    }
                });
                
                 @Override
                    public byte[] submit(final String... args) throws ContractException, TimeoutException, InterruptedException {
                        try {
                            TransactionProposalRequest request = newProposalRequest(args);
                            Collection<ProposalResponse> proposalResponses = sendTransactionProposal(request);
                
                            Collection<ProposalResponse> validResponses = validatePeerResponses(proposalResponses);
                            ProposalResponse proposalResponse = validResponses.iterator().next();
                            byte[] result = proposalResponse.getChaincodeActionResponsePayload();
                            String transactionId = proposalResponse.getTransactionID();
                
                            Channel.TransactionOptions transactionOptions = Channel.TransactionOptions.createTransactionOptions()
                                    .nOfEvents(Channel.NOfEvents.createNoEvents()); // Disable default commit wait behaviour
                
                            CommitHandler commitHandler = commitHandlerFactory.create(transactionId, network);
                            commitHandler.startListening();
                
                            try {
                                channel.sendTransaction(validResponses, transactionOptions).get(DEFAULT_ORDERER_TIMEOUT, DEFAULT_ORDERER_TIMEOUT_UNIT);
                            } catch (TimeoutException e) {
                                commitHandler.cancelListening();
                                throw e;
                            } catch (Exception e) {
                                commitHandler.cancelListening();
                                throw new ContractException("Failed to send transaction to the orderer", e);
                            }
                
                            commitHandler.waitForEvents(commitTimeout.getTime(), commitTimeout.getTimeUnit());
                
                            return result;
                        } catch (InvalidArgumentException | ProposalException | ServiceDiscoveryException e) {
                            throw new GatewayRuntimeException(e);
                        }
                    }
```
#####		Then the error message should contain "No valid proposal responses received"
```$xslt
  Then("the error message should contain {string}",
                (String expected) -> assertThat(transactionInvocation.getError().getMessage()).contains(expected));
```




