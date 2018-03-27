package com.hyperledger.fabric.business;
import com.hyperledger.fabric.commonutils.ConfigHelper;
import com.hyperledger.fabric.commonutils.FabricCommonConfig;
import com.hyperledger.fabric.consortium.ConsortiumOrg;
import com.hyperledger.fabric.consortium.ConsortiumStore;
import com.hyperledger.fabric.consortium.ConsortiumUser;
import com.hyperledger.fabric.commonutils.Util;
import org.apache.commons.codec.binary.Hex;
import org.bouncycastle.openssl.PEMWriter;
import org.hyperledger.fabric.protos.ledger.rwset.kvrwset.KvRwset;
import org.hyperledger.fabric.sdk.*;
import org.hyperledger.fabric.sdk.Peer.PeerRole;
import org.hyperledger.fabric.sdk.TransactionRequest.Type;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.InvalidProtocolBufferRuntimeException;
import org.hyperledger.fabric.sdk.exception.ProposalException;
import org.hyperledger.fabric.sdk.exception.TransactionEventException;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.hyperledger.fabric_ca.sdk.EnrollmentRequest;
import org.hyperledger.fabric_ca.sdk.HFCAClient;
import org.hyperledger.fabric_ca.sdk.HFCAInfo;
import org.hyperledger.fabric_ca.sdk.RegistrationRequest;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hyperledger.fabric.sdk.BlockInfo.EnvelopeType.TRANSACTION_ENVELOPE;
import static org.hyperledger.fabric.sdk.Channel.NOfEvents.createNofEvents;
import static org.hyperledger.fabric.sdk.Channel.PeerOptions.createPeerOptions;
import static org.hyperledger.fabric.sdk.Channel.TransactionOptions.createTransactionOptions;
public class FabricBusiness {
    private FabricCommonConfig testConfig = new FabricCommonConfig();
    private String fabricAdminName = "admin";
    private String fabricUser1Name = "user1";
    private String testFixturesPath = "src/test/fixture";
    private String fooChannelName = "foo";
    private String barChannelName = "bar";
    private byte[] expectedEventData = "!".getBytes(UTF_8);
    private String expectedEventName = "event";
    private String chainCodeFilepath = "sdkintegration/gocc/sample1";
    private String chainCodeName = "example_cc_go";
    private String chainCodePath = "github.com/example_cc";
    private String chainCodeVersion = "1";
    private Type chainCodeLang = Type.GO_LANG;
    private final ConfigHelper configHelper = new ConfigHelper();
    private String testTxID = null;  // save the CC invoke TxID and use in queries
    private ConsortiumStore sampleStore = null;
    private Collection<ConsortiumOrg> consortiumOrgCollection;

    public void checkConfig() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException, MalformedURLException, org.hyperledger.fabric_ca.sdk.exception.InvalidArgumentException {
        configHelper.customizeConfig();
        consortiumOrgCollection = testConfig.getIntegrationTestsSampleOrgs();
        for (ConsortiumOrg sampleOrg : consortiumOrgCollection) {
            String caName = sampleOrg.getCaName(); //Try one of each name and no name.
            if (caName != null && !caName.isEmpty()) {
                sampleOrg.setCaClient(HFCAClient.createNewInstance(caName, sampleOrg.getCaLocation(), sampleOrg.getCaProperties()));
            } else {
                sampleOrg.setCaClient(HFCAClient.createNewInstance(sampleOrg.getCaLocation(), sampleOrg.getCaProperties()));
            }
        }
    }
    Map<String, Properties> clientTLSProperties = new HashMap<>();
    File sampleStoreFile = new File(System.getProperty("java.io.tmpdir") + "/HFCSampletest.properties");
    public void setup() throws Exception {
        if (sampleStoreFile.exists()) { //For testing start fresh
            sampleStoreFile.delete();
        }
        sampleStore = new ConsortiumStore(sampleStoreFile);
        enrollUsersSetup(sampleStore); //This enrolls users with fabric ca and setups sample store to get users later.
        runFabricTest(sampleStore); //Runs Fabric tests with constructing channels, joining peers, exercising chaincode
    }

    public void runFabricTest(final ConsortiumStore sampleStore) throws Exception {
        HFClient client = HFClient.createNewInstance();
        client.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
        ConsortiumOrg sampleOrg = testConfig.getIntegrationTestsSampleOrg("peerOrg1");
        Channel fooChannel = constructChannel(fooChannelName, client, sampleOrg);
        sampleStore.saveChannel(fooChannel);
        runChannel(client, fooChannel, true, sampleOrg, 0);
        fooChannel.shutdown(true); // Force foo channel to shutdown clean up resources.
        sampleOrg = testConfig.getIntegrationTestsSampleOrg("peerOrg2");
        Channel barChannel = constructChannel(barChannelName, client, sampleOrg);
        sampleStore.saveChannel(barChannel);
        runChannel(client, barChannel, true, sampleOrg, 100); //run a newly constructed bar channel with different b value!
        blockWalker(client, barChannel);
    }

    /**
     * Will register and enroll users persisting them to samplestore.
     * @param sampleStore
     * @throws Exception
     */
    public void enrollUsersSetup(ConsortiumStore sampleStore) throws Exception {
        for (ConsortiumOrg sampleOrg : consortiumOrgCollection) {
            HFCAClient ca = sampleOrg.getCaClient();
            final String orgName = sampleOrg.getName();
            final String mspid = sampleOrg.getMspid();
            ca.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
            if (testConfig.isRunningFabricTLS()) {
                final EnrollmentRequest enrollmentRequestTLS = new EnrollmentRequest();
                enrollmentRequestTLS.addHost("localhost");
                enrollmentRequestTLS.setProfile("tls");
                final Enrollment enroll = ca.enroll("admin", "adminpw", enrollmentRequestTLS);
                final String tlsCertPEM = enroll.getCert();
                final String tlsKeyPEM = getPEMStringFromPrivateKey(enroll.getKey());
                final Properties tlsProperties = new Properties();
                tlsProperties.put("clientKeyBytes", tlsKeyPEM.getBytes(UTF_8));
                tlsProperties.put("clientCertBytes", tlsCertPEM.getBytes(UTF_8));
                clientTLSProperties.put(sampleOrg.getName(), tlsProperties);
                sampleStore.storeClientPEMTLCertificate(sampleOrg, tlsCertPEM);
                sampleStore.storeClientPEMTLSKey(sampleOrg, tlsKeyPEM);
            }
            HFCAInfo info = ca.info(); //just check if we connect at all.
            String infoName = info.getCAName();
            if (infoName != null && !infoName.isEmpty()) {
            }

            ConsortiumUser admin = sampleStore.getMember(fabricAdminName, orgName);
            if (!admin.isEnrolled()) {  //Preregistered admin only needs to be enrolled with Fabric caClient.
                admin.setEnrollment(ca.enroll(admin.getName(), "adminpw"));
                admin.setMspId(mspid);
            }
            sampleOrg.setAdmin(admin); // The admin of this org --
            ConsortiumUser user = sampleStore.getMember(fabricUser1Name, sampleOrg.getName());
            if (!user.isRegistered()) {  // users need to be registered AND enrolled
                RegistrationRequest rr = new RegistrationRequest(user.getName(), "org1.department1");
                user.setEnrollmentSecret(ca.register(rr, admin));
            }
            if (!user.isEnrolled()) {
                user.setEnrollment(ca.enroll(user.getName(), user.getEnrollmentSecret()));
                user.setMspId(mspid);
            }
            sampleOrg.addUser(user); //Remember user belongs to this Org
            final String sampleOrgName = sampleOrg.getName();
            final String sampleOrgDomainName = sampleOrg.getDomainName();
            ConsortiumUser peerOrgAdmin = sampleStore.getMember(sampleOrgName + "Admin", sampleOrgName, sampleOrg.getMspid(),
                    Util.findFileSk(Paths.get(testConfig.getTestChannelPath(), "crypto-config/peerOrganizations/",
                            sampleOrgDomainName, format("/users/Admin@%s/msp/keystore", sampleOrgDomainName)).toFile()),
                    Paths.get(testConfig.getTestChannelPath(), "crypto-config/peerOrganizations/", sampleOrgDomainName,
                            format("/users/Admin@%s/msp/signcerts/Admin@%s-cert.pem", sampleOrgDomainName, sampleOrgDomainName)).toFile());
            sampleOrg.setPeerAdmin(peerOrgAdmin); //A special user that can create channels, join peers and install chaincode
        }
    }

    static String getPEMStringFromPrivateKey(PrivateKey privateKey) throws IOException {
        StringWriter pemStrWriter = new StringWriter();
        PEMWriter pemWriter = new PEMWriter(pemStrWriter);
        pemWriter.writeObject(privateKey);
        pemWriter.close();
        return pemStrWriter.toString();
    }
    //CHECKSTYLE.OFF: Method length is 320 lines (max allowed is 150).
    void runChannel(HFClient client, Channel channel, boolean installChaincode, ConsortiumOrg sampleOrg, int delta) {

        class ChaincodeEventCapture { //A test class to capture chaincode events
            final String handle;
            final BlockEvent blockEvent;
            final ChaincodeEvent chaincodeEvent;
            ChaincodeEventCapture(String handle, BlockEvent blockEvent, ChaincodeEvent chaincodeEvent) {
                this.handle = handle;
                this.blockEvent = blockEvent;
                this.chaincodeEvent = chaincodeEvent;
            }
        }
        Vector<ChaincodeEventCapture> chaincodeEvents = new Vector<>(); // Test list to capture chaincode events.
        try {

            final String channelName = channel.getName();
            boolean isFooChain = fooChannelName.equals(channelName);
            Collection<Orderer> orderers = channel.getOrderers();
            final ChaincodeID chaincodeID;
            Collection<ProposalResponse> responses;
            Collection<ProposalResponse> successful = new LinkedList<>();
            Collection<ProposalResponse> failed = new LinkedList<>();
            String chaincodeEventListenerHandle = channel.registerChaincodeEventListener(Pattern.compile(".*"),
                    Pattern.compile(Pattern.quote(expectedEventName)),
                    (handle, blockEvent, chaincodeEvent) -> {
                        chaincodeEvents.add(new ChaincodeEventCapture(handle, blockEvent, chaincodeEvent));
                        String es = blockEvent.getPeer() != null ? blockEvent.getPeer().getName() : blockEvent.getEventHub().getName();
                    });
            //For non foo channel unregister event listener to test events are not called.
            if (!isFooChain) {
                channel.unregisterChaincodeEventListener(chaincodeEventListenerHandle);
                chaincodeEventListenerHandle = null;
            }
            ChaincodeID.Builder chaincodeIDBuilder = ChaincodeID.newBuilder().setName(chainCodeName)
                    .setVersion(chainCodeVersion);
            if (null != chainCodePath) {
                chaincodeIDBuilder.setPath(chainCodePath);
            }
            chaincodeID = chaincodeIDBuilder.build();

            if (installChaincode) {
                // Install Proposal Request
                client.setUserContext(sampleOrg.getPeerAdmin());
                InstallProposalRequest installProposalRequest = client.newInstallProposalRequest();
                installProposalRequest.setChaincodeID(chaincodeID);
                if (isFooChain) {
                    // on foo chain install from directory.
                    ////For GO language and serving just a single user, chaincodeSource is mostly likely the users GOPATH
                    installProposalRequest.setChaincodeSourceLocation(Paths.get(testFixturesPath, chainCodeFilepath).toFile());
                } else {
                    // On bar chain install from an input stream.
                    if (chainCodeLang.equals(Type.GO_LANG)) {

                        installProposalRequest.setChaincodeInputStream(Util.generateTarGzInputStream(
                                (Paths.get(testFixturesPath, chainCodeFilepath, "src", chainCodePath).toFile()),
                                Paths.get("src", chainCodePath).toString()));
                    } else {
                        installProposalRequest.setChaincodeInputStream(Util.generateTarGzInputStream(
                                (Paths.get(testFixturesPath, chainCodeFilepath).toFile()),
                                "src"));
                    }
                }
                installProposalRequest.setChaincodeVersion(chainCodeVersion);
                installProposalRequest.setChaincodeLanguage(chainCodeLang);
                int numInstallProposal = 0;
                Collection<Peer> peers = channel.getPeers();
                numInstallProposal = numInstallProposal + peers.size();
                responses = client.sendInstallProposal(installProposalRequest, peers);

                for (ProposalResponse response : responses) {
                    if (response.getStatus() == ProposalResponse.Status.SUCCESS) {
                        successful.add(response);
                    } else {
                        failed.add(response);
                    }
                }
                if (failed.size() > 0) {
                    ProposalResponse first = failed.iterator().next();
                }
            }
            //// Instantiate chaincode.
            InstantiateProposalRequest instantiateProposalRequest = client.newInstantiationProposalRequest();
            instantiateProposalRequest.setProposalWaitTime(testConfig.getProposalWaitTime());
            instantiateProposalRequest.setChaincodeID(chaincodeID);
            instantiateProposalRequest.setChaincodeLanguage(chainCodeLang);
            instantiateProposalRequest.setFcn("init");
            instantiateProposalRequest.setArgs(new String[] {"a", "500", "b", "" + (200 + delta)});
            Map<String, byte[]> tm = new HashMap<>();
            tm.put("HyperLedgerFabric", "InstantiateProposalRequest:JavaSDK".getBytes(UTF_8));
            tm.put("method", "InstantiateProposalRequest".getBytes(UTF_8));
            instantiateProposalRequest.setTransientMap(tm);

            /*
              policy OR(Org1MSP.member, Org2MSP.member) meaning 1 signature from someone in either Org1 or Org2
              See README.md Chaincode endorsement policies section for more details.
            */
            ChaincodeEndorsementPolicy chaincodeEndorsementPolicy = new ChaincodeEndorsementPolicy();
            chaincodeEndorsementPolicy.fromYamlFile(new File(testFixturesPath + "/sdkintegration/chaincodeendorsementpolicy.yaml"));
            instantiateProposalRequest.setChaincodeEndorsementPolicy(chaincodeEndorsementPolicy);
            successful.clear();
            failed.clear();

            if (isFooChain) {  //Send responses both ways with specifying peers and by using those on the channel.
                responses = channel.sendInstantiationProposal(instantiateProposalRequest, channel.getPeers());
            } else {
                responses = channel.sendInstantiationProposal(instantiateProposalRequest);
            }
            for (ProposalResponse response : responses) {
                if (response.isVerified() && response.getStatus() == ProposalResponse.Status.SUCCESS) {
                    successful.add(response);
                } else {
                    failed.add(response);
                }
            }
            if (failed.size() > 0) {
                for (ProposalResponse fail : failed) {
                }
                ProposalResponse first = failed.iterator().next();
            }
            Channel.NOfEvents nOfEvents = createNofEvents();
            if (!channel.getPeers(EnumSet.of(PeerRole.EVENT_SOURCE)).isEmpty()) {
                nOfEvents.addPeers(channel.getPeers(EnumSet.of(PeerRole.EVENT_SOURCE)));
            }
            if (!channel.getEventHubs().isEmpty()) {
                nOfEvents.addEventHubs(channel.getEventHubs());
            }
            channel.sendTransaction(successful, createTransactionOptions() //Basically the default options but shows it's usage.
                    .userContext(client.getUserContext()) //could be a different user context. this is the default.
                    .shuffleOrders(false) // don't shuffle any orderers the default is true.
                    .orderers(channel.getOrderers()) // specify the orderers we want to try this transaction. Fails once all Orderers are tried.
                    .nOfEvents(nOfEvents) // The events to signal the completion of the interest in the transaction
            ).thenApply(transactionEvent -> {

                waitOnFabric(0);
                BlockEvent blockEvent = transactionEvent.getBlockEvent(); // This is the blockevent that has this transaction.
                try {
                    successful.clear();
                    failed.clear();
                    client.setUserContext(sampleOrg.getUserMap().get(fabricUser1Name));
                    TransactionProposalRequest transactionProposalRequest = client.newTransactionProposalRequest();
                    transactionProposalRequest.setChaincodeID(chaincodeID);
                    transactionProposalRequest.setChaincodeLanguage(chainCodeLang);
                    //transactionProposalRequest.setFcn("invoke");
                    transactionProposalRequest.setFcn("move");
                    transactionProposalRequest.setProposalWaitTime(testConfig.getProposalWaitTime());
                    transactionProposalRequest.setArgs("a", "b", "100");
                    Map<String, byte[]> tm2 = new HashMap<>();
                    tm2.put("HyperLedgerFabric", "TransactionProposalRequest:JavaSDK".getBytes(UTF_8)); //Just some extra junk in transient map
                    tm2.put("method", "TransactionProposalRequest".getBytes(UTF_8)); // ditto
                    tm2.put("result", ":)".getBytes(UTF_8));  // This should be returned see chaincode why.
                    tm2.put(expectedEventName, expectedEventData);  //This should trigger an event see chaincode why.
                    transactionProposalRequest.setTransientMap(tm2);
                    Collection<ProposalResponse> transactionPropResp = channel.sendTransactionProposal(transactionProposalRequest, channel.getPeers());
                    for (ProposalResponse response : transactionPropResp) {
                        if (response.getStatus() == ProposalResponse.Status.SUCCESS) {
                            successful.add(response);
                        } else {
                            failed.add(response);
                        }
                    }
                    Collection<Set<ProposalResponse>> proposalConsistencySets = SDKUtils.getProposalConsistencySets(transactionPropResp);
                    if (proposalConsistencySets.size() != 1) {
                    }
                    if (failed.size() > 0) {
                        ProposalResponse firstTransactionProposalResponse = failed.iterator().next();
                    }
                    ProposalResponse resp = successful.iterator().next();
                    byte[] x = resp.getChaincodeActionResponsePayload(); // This is the data returned by the chaincode.
                    String resultAsString = null;
                    if (x != null) {
                        resultAsString = new String(x, "UTF-8");
                    }
                    TxReadWriteSetInfo readWriteSetInfo = resp.getChaincodeActionResponseReadWriteSetInfo();
                    ChaincodeID cid = resp.getChaincodeID();
                    final String path = cid.getPath();
                    if (null == chainCodePath) {
                    } else {
                    }
                    return channel.sendTransaction(successful).get(testConfig.getTransactionWaitTime(), TimeUnit.SECONDS);

                } catch (Exception e) {
                    e.printStackTrace();
                }

                return null;

            }).thenApply(transactionEvent -> {
                try {
                    waitOnFabric(0);
                    testTxID = transactionEvent.getTransactionID(); // used in the channel queries later
                    String expect = "" + (300 + delta);
                    QueryByChaincodeRequest queryByChaincodeRequest = client.newQueryProposalRequest();
                    queryByChaincodeRequest.setArgs(new String[] {"b"});
                    queryByChaincodeRequest.setFcn("query");
                    queryByChaincodeRequest.setChaincodeID(chaincodeID);

                    Map<String, byte[]> tm2 = new HashMap<>();
                    tm2.put("HyperLedgerFabric", "QueryByChaincodeRequest:JavaSDK".getBytes(UTF_8));
                    tm2.put("method", "QueryByChaincodeRequest".getBytes(UTF_8));
                    queryByChaincodeRequest.setTransientMap(tm2);

                    Collection<ProposalResponse> queryProposals = channel.queryByChaincode(queryByChaincodeRequest, channel.getPeers());
                    for (ProposalResponse proposalResponse : queryProposals) {
                        if (!proposalResponse.isVerified() || proposalResponse.getStatus() != ProposalResponse.Status.SUCCESS) {
                        } else {
                            String payload = proposalResponse.getProposalResponse().getResponse().getPayload().toStringUtf8();
                        }
                    }
                    return null;
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }).exceptionally(e -> {
                if (e instanceof TransactionEventException) {
                    BlockEvent.TransactionEvent te = ((TransactionEventException) e).getTransactionEvent();
                    if (te != null) {
                        throw new AssertionError(format("Transaction with txid %s failed. %s", te.getTransactionID(), e.getMessage()), e);
                    }
                }
                throw new AssertionError(format("Test failed with %s exception %s", e.getClass().getName(), e.getMessage()), e);
            }).get(testConfig.getTransactionWaitTime(), TimeUnit.SECONDS);
            BlockchainInfo channelInfo = channel.queryBlockchainInfo();
            String chainCurrentHash = Hex.encodeHexString(channelInfo.getCurrentBlockHash());
            String chainPreviousHash = Hex.encodeHexString(channelInfo.getPreviousBlockHash());
            BlockInfo returnedBlock = channel.queryBlockByNumber(channelInfo.getHeight() - 1);
            String previousHash = Hex.encodeHexString(returnedBlock.getPreviousHash());
            byte[] hashQuery = returnedBlock.getPreviousHash();
            returnedBlock = channel.queryBlockByHash(hashQuery);
            returnedBlock = channel.queryBlockByTransactionID(testTxID);
            TransactionInfo txInfo = channel.queryTransactionByID(testTxID);
            if (chaincodeEventListenerHandle != null) {
                channel.unregisterChaincodeEventListener(chaincodeEventListenerHandle);
                final int numberEventsExpected = channel.getEventHubs().size() +
                        channel.getPeers(EnumSet.of(PeerRole.EVENT_SOURCE)).size();
                for (int i = 15; i > 0; --i) {
                    if (chaincodeEvents.size() == numberEventsExpected) {
                        break;
                    } else {
                        Thread.sleep(90); // wait for the events.
                    }
                }
                for (ChaincodeEventCapture chaincodeEventCapture : chaincodeEvents) {
                    BlockEvent blockEvent = chaincodeEventCapture.blockEvent;
                }
            } else {
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    Channel constructChannel(String name, HFClient client, ConsortiumOrg sampleOrg) throws Exception {
        boolean doPeerEventing = !testConfig.isRunningAgainstFabric10() && barChannelName.equals(name);
        client.setUserContext(sampleOrg.getPeerAdmin());
        Collection<Orderer> orderers = new LinkedList<>();
        for (String orderName : sampleOrg.getOrdererNames()) {
            Properties ordererProperties = testConfig.getOrdererProperties(orderName);
            ordererProperties.put("grpc.NettyChannelBuilderOption.keepAliveTime", new Object[] {5L, TimeUnit.MINUTES});
            ordererProperties.put("grpc.NettyChannelBuilderOption.keepAliveTimeout", new Object[] {8L, TimeUnit.SECONDS});
            ordererProperties.put("grpc.NettyChannelBuilderOption.keepAliveWithoutCalls", new Object[] {true});

            if (!clientTLSProperties.isEmpty()) {
                ordererProperties.putAll(clientTLSProperties.get(sampleOrg.getName()));
            }
            orderers.add(client.newOrderer(orderName, sampleOrg.getOrdererLocation(orderName),
                    ordererProperties));
        }
        Orderer anOrderer = orderers.iterator().next();
        orderers.remove(anOrderer);
        ChannelConfiguration channelConfiguration = new ChannelConfiguration(new File(testFixturesPath + "/sdkintegration/e2e-2Orgs/XXX/" + name + ".tx"));
        Channel newChannel = client.newChannel(name, anOrderer, channelConfiguration, client.getChannelConfigurationSignature(channelConfiguration, sampleOrg.getPeerAdmin()));
        boolean everyother = true; //test with both cases when doing peer eventing.
        for (String peerName : sampleOrg.getPeerNames()) {
            String peerLocation = sampleOrg.getPeerLocation(peerName);
            Properties peerProperties = testConfig.getPeerProperties(peerName); //test properties for peer.. if any.
            if (peerProperties == null) {
                peerProperties = new Properties();
            }
            if (!clientTLSProperties.isEmpty()) {
                peerProperties.putAll(clientTLSProperties.get(sampleOrg.getName()));
            }
            peerProperties.put("grpc.NettyChannelBuilderOption.maxInboundMessageSize", 9000000);
            Peer peer = client.newPeer(peerName, peerLocation, peerProperties);
            if (doPeerEventing && everyother) {
                newChannel.joinPeer(peer, createPeerOptions()); //Default is all roles.
            } else {
                newChannel.joinPeer(peer, createPeerOptions().setPeerRoles(PeerRole.NO_EVENT_SOURCE));
            }
            everyother = !everyother;
        }
        if (doPeerEventing) {
        }
        for (Orderer orderer : orderers) { //add remaining orderers if any.
            newChannel.addOrderer(orderer);
        }
        for (String eventHubName : sampleOrg.getEventHubNames()) {
            final Properties eventHubProperties = testConfig.getEventHubProperties(eventHubName);
            eventHubProperties.put("grpc.NettyChannelBuilderOption.keepAliveTime", new Object[] {5L, TimeUnit.MINUTES});
            eventHubProperties.put("grpc.NettyChannelBuilderOption.keepAliveTimeout", new Object[] {8L, TimeUnit.SECONDS});
            if (!clientTLSProperties.isEmpty()) {
                eventHubProperties.putAll(clientTLSProperties.get(sampleOrg.getName()));
            }
            EventHub eventHub = client.newEventHub(eventHubName, sampleOrg.getEventHubLocation(eventHubName),
                    eventHubProperties);
            newChannel.addEventHub(eventHub);
        }
        byte[] serializedChannelBytes = newChannel.serializeChannel();
        newChannel.shutdown(true);
        return client.deSerializeChannel(serializedChannelBytes).initialize();
    }

    private void waitOnFabric(int additional) {
        //NOOP today
    }
    void blockWalker(HFClient client, Channel channel) throws InvalidArgumentException, ProposalException, IOException {
        try {
            BlockchainInfo channelInfo = channel.queryBlockchainInfo();
            for (long current = channelInfo.getHeight() - 1; current > -1; --current) {
                BlockInfo returnedBlock = channel.queryBlockByNumber(current);
                final long blockNumber = returnedBlock.getBlockNumber();
                final int envelopeCount = returnedBlock.getEnvelopeCount();
                int i = 0;
                for (BlockInfo.EnvelopeInfo envelopeInfo : returnedBlock.getEnvelopeInfos()) {
                    ++i;
                    final String channelId = envelopeInfo.getChannelId();
                    if (envelopeInfo.getType() == TRANSACTION_ENVELOPE) {
                        BlockInfo.TransactionEnvelopeInfo transactionEnvelopeInfo = (BlockInfo.TransactionEnvelopeInfo) envelopeInfo;
                        int j = 0;
                        for (BlockInfo.TransactionEnvelopeInfo.TransactionActionInfo transactionActionInfo : transactionEnvelopeInfo.getTransactionActionInfos()) {
                            ++j;
                            for (int n = 0; n < transactionActionInfo.getEndorsementsCount(); ++n) {
                                BlockInfo.EndorserInfo endorserInfo = transactionActionInfo.getEndorsementInfo(n);
                            }
                            for (int z = 0; z < transactionActionInfo.getChaincodeInputArgsCount(); ++z) {
                            }
                            if (blockNumber == 2) {
                                ChaincodeEvent chaincodeEvent = transactionActionInfo.getEvent();
                            }
                            TxReadWriteSetInfo rwsetInfo = transactionActionInfo.getTxReadWriteSet();
                            if (null != rwsetInfo) {
                                for (TxReadWriteSetInfo.NsRwsetInfo nsRwsetInfo : rwsetInfo.getNsRwsetInfos()) {
                                    final String namespace = nsRwsetInfo.getNamespace();
                                    KvRwset.KVRWSet rws = nsRwsetInfo.getRwset();
                                    int rs = -1;
                                    for (KvRwset.KVRead readList : rws.getReadsList()) {
                                        rs++;
                                        if ("bar".equals(channelId) && blockNumber == 2) {
                                            if ("example_cc_go".equals(namespace)) {
                                                if (rs == 0) {
                                                } else if (rs == 1) {
                                                } else {
                                                }
                                            }
                                        }
                                    }
                                    rs = -1;
                                    for (KvRwset.KVWrite writeList : rws.getWritesList()) {
                                        rs++;
                                        if ("bar".equals(channelId) && blockNumber == 2) {
                                            if (rs == 0) {
                                            } else if (rs == 1) {
                                            } else {
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (InvalidProtocolBufferRuntimeException e) {
            throw e.getCause();
        }
    }

}
