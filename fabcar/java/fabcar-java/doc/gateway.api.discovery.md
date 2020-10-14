
# discovery.feature

https://hyperledger-fabric.readthedocs.io/en/release-1.4/discovery-overview.html#how-service-discovery-works-in-fabric
https://hyperledger-fabric.readthedocs.io/en/release-1.4/discovery-cli.html

Capabilities of the discovery service
The discovery service can respond to the following queries:

1. Configuration query: Returns the MSPConfig of all organizations in the channel along with the orderer endpoints of the channel.
2. Peer membership query: Returns the peers that have joined the channel.
3. Endorsement query: Returns an endorsement descriptor for given chaincode(s) in a channel.
4. Local peer membership query: Returns the local membership information of the peer that responds to the query. By default the client needs to be an administrator for the peer to respond to this query.

## Feature: Configure Fabric using SDK using discovery service and submit/evaluate using a network Gateway

	Background:
###		Given I have deployed a tls Fabric network
```$xslt
  Given("I have deployed a {word} Fabric network", (String tlsType) -> {
            // tlsType is either "tls" or "non-tls"
            fabricNetworkType = tlsType;
        });
```
###		And I have created and joined all channels from the tls connection profile
```$xslt
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
###		And I deploy node chaincode named marbles0 at version 1.0.0 for all organizations on channel mychannel with endorsement policy 1AdminOr2Other and arguments ["init", "a", "1000", "b", "2000"]
```$xslt
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
 	Scenario: Using a Gateway with discovery I can submit and evaluate transactions on instantiated node chaincode
###		Given I have a gateway as user User1 using the discovery connection profile
```$xslt
       Given("I have a gateway as user {word} using the {word} connection profile",
                (String userName, String tlsType) -> {
                    Wallet wallet = createWallet();
                    gatewayBuilder = Gateway.createBuilder();
                    gatewayBuilder.identity(wallet, userName);
                    gatewayBuilder.networkConfig(getNetworkConfigPath(tlsType));
                    gatewayBuilder.commitTimeout(1, TimeUnit.MINUTES);
                    //TODO: discovery设置了在哪里用呢
                    if (tlsType.equals("discovery")) {
                        gatewayBuilder.discovery(true);
                    }
                });
                
                
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
```
###		And I connect the gateway
```$xslt
Given("I connect the gateway", () -> gateway = gatewayBuilder.connect());

@Override
public GatewayImpl connect() {
    return new GatewayImpl(this);
}
        
private GatewayImpl(final Builder builder) {
    this.commitHandlerFactory = builder.commitHandlerFactory;
    this.commitTimeout = builder.commitTimeout;
    this.queryHandlerFactory = builder.queryHandlerFactory;
    this.discovery = builder.discovery;

    if (builder.client != null) {
        // Only for testing!
        this.client = builder.client;
        this.networkConfig = null;

        User user = client.getUserContext();
        Enrollment enrollment = user.getEnrollment();
        this.identity = Identity.createIdentity(user.getMspId(), enrollment.getCert(), enrollment.getKey());
    } else {
        if (null == builder.identity) {
            throw new IllegalStateException("The gateway identity must be set");
        }
        if (null == builder.ccp) {
            throw new IllegalStateException("The network configuration must be specified");
        }
        this.networkConfig = builder.ccp;
        this.identity = builder.identity;

        this.client = createClient();
    }
}

private HFClient createClient() {
        Enrollment enrollment = new X509Enrollment(identity.getPrivateKey(), identity.getCertificate());
        User user = new User() {
            @Override
            public String getName() {
                return "gateway";
            }

            @Override
            public Set<String> getRoles() {
                return Collections.emptySet();
            }

            @Override
            public String getAccount() {
                return "";
            }

            @Override
            public String getAffiliation() {
                return "";
            }

            @Override
            public Enrollment getEnrollment() {
                return enrollment;
            }

            @Override
            public String getMspId() {
                return identity.getMspId();
            }
        };

        HFClient client = HFClient.createNewInstance();

        try {
            CryptoSuite cryptoSuite = CryptoSuiteFactory.getDefault().getCryptoSuite();
            client.setCryptoSuite(cryptoSuite);
            client.setUserContext(user);
        } catch (ClassNotFoundException | CryptoException | IllegalAccessException | NoSuchMethodException
                | InstantiationException | InvalidArgumentException | InvocationTargetException e) {
            throw new GatewayRuntimeException("Failed to configure client", e);
        }

        return client;
    }
    
    
```
###		And I use the mychannel network
```$xslt
 Given("I use the {word} network", (String networkName) -> network = gateway.getNetwork(networkName));
 
  @Override
 public synchronized Network getNetwork(final String networkName) {
     if (networkName == null || networkName.isEmpty()) {
         throw new IllegalArgumentException("Channel name must be a non-empty string");
     }
     NetworkImpl network = networks.get(networkName);
     if (network == null) {
         Channel channel = client.getChannel(networkName);
         if (channel == null && networkConfig != null) {
             try {
                 channel = client.loadChannelFromConfig(networkName, networkConfig);
             } catch (InvalidArgumentException | NetworkConfigurationException ex) {
                 LOG.info("Unable to load channel configuration from connection profile: " + ex.getLocalizedMessage());
             }
         }
         if (channel == null) {
             try {
                 // since this channel is not in the CCP, we'll assume it exists,
                 // and the org's peer(s) has joined it with all roles
                 channel = client.newChannel(networkName);
                 for (Peer peer : getPeersForOrg()) {
                     PeerOptions peerOptions = PeerOptions.createPeerOptions()
                             .setPeerRoles(EnumSet.allOf(PeerRole.class));
                     channel.addPeer(peer, peerOptions);
                 }
             } catch (InvalidArgumentException e) {
                 // we've already checked the channel status
                 throw new GatewayRuntimeException(e);
             }
         }
         network = new NetworkImpl(channel, this);
         networks.put(networkName, network);
     }
     return network;
 }
```
###		And I use the marbles0 contract
```$xslt
 Given("I use the {word} contract", (String contractName) -> contract = network.getContract(contractName));
 
 
 @Override
 public Contract getContract(final String chaincodeId, final String name) {
     if (chaincodeId == null || chaincodeId.isEmpty()) {
         throw new IllegalArgumentException("getContract: chaincodeId must be a non-empty string");
     }
     if (name == null) {
         throw new IllegalArgumentException("getContract: name must not be null");
     }

     String key = chaincodeId + ':' + name;
     return contracts.computeIfAbsent(key, k -> new ContractImpl(this, chaincodeId, name));
 }
 
  ContractImpl(final NetworkImpl network, final String chaincodeId, final String name) {
         this.network = network;
         this.chaincodeId = chaincodeId;
         this.name = name;
     }
```
###	 	When I prepare an initMarble transaction

```$xslt
   When("I prepare a(n) {word} transaction", (String transactionName) -> {
            Transaction transaction = contract.createTransaction(transactionName);
            transactionInvocation = TransactionInvocation.expectSuccess(transaction);
        });
        
 @Override
    public Transaction createTransaction(final String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Transaction must be a non-empty string");
        }
        String qualifiedName = getQualifiedName(name);
        return new TransactionImpl(this, qualifiedName);
    }
    
    TransactionImpl(final ContractImpl contract, final String name) {
            this.contract = contract;
            this.name = name;
            network = contract.getNetwork();
            channel = network.getChannel();
            gateway = network.getGateway();
            commitHandlerFactory = gateway.getCommitHandlerFactory();
            commitTimeout = gateway.getCommitTimeout();
            queryHandler = network.getQueryHandler();
        }
```
###	 	And I submit the transaction with arguments ["marble1", "blue", "50", "bob"]
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
         // 1 sendTransactionProposal
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
             // 2 sendTransaction
             channel.sendTransaction(validResponses, transactionOptions).get(DEFAULT_ORDERER_TIMEOUT, DEFAULT_ORDERER_TIMEOUT_UNIT);
         } catch (TimeoutException e) {
             commitHandler.cancelListening();
             throw e;
         } catch (Exception e) {
             commitHandler.cancelListening();
             throw new ContractException("Failed to send transaction to the orderer", e);
         }

         // 3 waitForEvents
         commitHandler.waitForEvents(commitTimeout.getTime(), commitTimeout.getTimeUnit());

         return result;
     } catch (InvalidArgumentException | ProposalException | ServiceDiscoveryException e) {
         throw new GatewayRuntimeException(e);
     }
 }
 
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
     
   /**
       * Send a transaction  proposal.
       *
       * @param transactionProposalRequest The transaction proposal to be sent to all the required peers needed for endorsing.
       * @param discoveryOptions
       * @return responses from peers.
       * @throws InvalidArgumentException
       * @throws ProposalException
       */
      public Collection<ProposalResponse> sendTransactionProposalToEndorsers(TransactionProposalRequest transactionProposalRequest, DiscoveryOptions discoveryOptions) throws ProposalException, InvalidArgumentException, ServiceDiscoveryException {
     
     
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
    /**
     * load the peer organizations CA certificates into the channel's trust store so that we
     * can verify signatures from peer messages
     *
     * @throws InvalidArgumentException
     * @throws CryptoException
     */
    protected synchronized void loadCACertificates(boolean force) throws InvalidArgumentException, CryptoException, TransactionException {



    public CompletableFuture<TransactionEvent> sendTransaction(Collection<ProposalResponse> proposalResponses,
                                                                  TransactionOptions transactionOptions) {
   
           return doSendTransaction(proposalResponses, transactionOptions)
                   .whenComplete((result, exception) -> logCompletion("sendTransaction", result, exception));
       }
       
     private CompletableFuture<TransactionEvent> doSendTransaction(Collection<ProposalResponse> proposalResponses,
                                                                   TransactionOptions transactionOptions) {
      非常长的函数
      
      

```
###	 	And I prepare a readMarble transaction

```$xslt
When("I prepare a(n) {word} transaction", (String transactionName) -> {
            Transaction transaction = contract.createTransaction(transactionName);
            transactionInvocation = TransactionInvocation.expectSuccess(transaction);
        });
```
###	 	And I evaluate the transaction with arguments ["marble1"]
	 	Then the response should be JSON matching
		"""
		{
			"color":"blue",
			"docType":"marble",
			"name":"marble1",
			"owner":"bob",
			"size":50
		}
		"""
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
```
