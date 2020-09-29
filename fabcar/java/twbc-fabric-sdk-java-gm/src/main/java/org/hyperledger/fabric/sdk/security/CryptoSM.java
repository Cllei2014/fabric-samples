package org.hyperledger.fabric.sdk.security;

import java.io.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertPathValidatorException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXParameters;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.security.auth.x500.X500Principal;
import javax.xml.bind.DatatypeConverter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.asn1.gm.GMNamedCurves;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.digests.SM3Digest;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.params.ParametersWithID;
import org.bouncycastle.crypto.params.ParametersWithRandom;
import org.bouncycastle.crypto.signers.SM2Signer;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.crypto.util.PrivateKeyInfoFactory;
import org.bouncycastle.crypto.util.SubjectPublicKeyInfoFactory;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.math.ec.ECCurve;
import org.bouncycastle.math.ec.ECCurve.Fp;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.bouncycastle.util.io.pem.PemHeader;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.hyperledger.common.SafeText;
import org.hyperledger.fabric.sdk.exception.CryptoException;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.helper.Config;
import org.hyperledger.fabric.sdk.helper.DiagnosticFileDumper;
import org.hyperledger.gm.helper.SM2Util;
import org.hyperledger.gm.helper.SM3Util;
import org.hyperledger.gm.helper.cert.CommonUtil;
import org.hyperledger.gm.helper.cert.SM2PublicKey;
import org.hyperledger.gm.helper.cert.SM2X509CertMaker;

/**
 * @author Bryan
 * @date 2020-01-16
 */
public class CryptoSM implements CryptoSuite {

    private static final Log logger = LogFactory.getLog(CryptoSM.class);
    private static final Config config = Config.getConfig();
    private static final boolean IS_TRACE_LEVEL = logger.isTraceEnabled();
    private static final DiagnosticFileDumper diagnosticFileDumper =
            IS_TRACE_LEVEL ? config.getDiagnosticFileDumper() : null;

    private static final BigInteger SM2_ECC_P;
    private static final BigInteger SM2_ECC_A;
    private static final BigInteger SM2_ECC_B;
    private static final BigInteger SM2_ECC_N;
    private static final BigInteger SM2_ECC_GX;
    private static final BigInteger SM2_ECC_GY;
    private static final ECCurve CURVE;
    private static final ECDomainParameters DOMAIN_PARAMS;
    private static final String SIGNATURE_ALGORITHM = "SM2";
    private static final String CURVE_ALGORITHM = "SM3withSM2";
    private static final String PROVIDER_SHORTNAME = "BC";
    private static X9ECParameters x9ECParameters = GMNamedCurves.getByName("sm2p256v1");
    private static ECParameterSpec ecParameterSpec = new ECParameterSpec(x9ECParameters.getCurve(), x9ECParameters.getG(), x9ECParameters.getN());

    private CertificateFactory cf;
    private Provider SECURITY_PROVIDER;
    private String hashAlgorithm = config.getHashAlgorithm();
    private int securityLevel = config.getSecurityLevel();
    private String CERTIFICATE_FORMAT = config.getCertificateFormat();
    private KeyStore trustStore = null;
    private final AtomicBoolean inited;

    static {
        SM2_ECC_P = new BigInteger("FFFFFFFEFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF00000000FFFFFFFFFFFFFFFF", 16);
        SM2_ECC_A = new BigInteger("FFFFFFFEFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF00000000FFFFFFFFFFFFFFFC", 16);
        SM2_ECC_B = new BigInteger("28E9FA9E9D9F5E344D5A9E4BCF6509A7F39789F515AB8F92DDBCBD414D940E93", 16);
        SM2_ECC_N = new BigInteger("FFFFFFFEFFFFFFFFFFFFFFFFFFFFFFFF7203DF6B21C6052B53BBF40939D54123", 16);
        SM2_ECC_GX = new BigInteger("32C4AE2C1F1981195F9904466A39C9948FE30BBFF2660BE1715A4589334C74C7", 16);
        SM2_ECC_GY = new BigInteger("BC3736A2F4F6779C59BDCEE36B692153D0A9877CC62A474002DF32E52139F0A0", 16);
        CURVE = new Fp(SM2_ECC_P, SM2_ECC_A, SM2_ECC_B);
        DOMAIN_PARAMS = new ECDomainParameters(CURVE,
                CURVE.createPoint(SM2_ECC_GX, SM2_ECC_GY), SM2_ECC_N, BigInteger.ONE);
    }

    public CryptoSM() throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        this.inited = new AtomicBoolean(false);
        String securityProviderClassName = config.getSecurityProviderClassName();
        this.SECURITY_PROVIDER = setUpExplicitProvider(securityProviderClassName);
        if (this.SECURITY_PROVIDER == null) {
            throw new InstantiationException("SECURITY_PROVIDER is null");
        } else {
            Security.addProvider(this.SECURITY_PROVIDER);
        }
    }

    public void init() throws CryptoException, InvalidArgumentException {
        if (this.inited.getAndSet(true)) {
            throw new InvalidArgumentException("Crypto suite already initialized");
        } else {
            this.resetConfiguration();
        }
    }

    private void resetConfiguration() throws CryptoException {
        try {
            this.cf = CertificateFactory.getInstance(this.CERTIFICATE_FORMAT, PROVIDER_SHORTNAME);
        } catch (NoSuchProviderException | CertificateException e) {
            CryptoException ex = new CryptoException(
                    "Cannot initialize  certificate factory. Error = " + e.getMessage(), e);
            logger.error(ex.getMessage());
            throw ex;
        }
    }

    void setProperties(Properties properties) throws CryptoException, InvalidArgumentException {
        if (properties == null) {
            throw new InvalidArgumentException("properties must not be null");
        } else {
            this.hashAlgorithm = Optional
                    .ofNullable(properties.getProperty("org.hyperledger.fabric.sdk.hash_algorithm"))
                    .orElse(this.hashAlgorithm);
            this.resetConfiguration();
        }
    }

    private static Provider setUpExplicitProvider(String securityProviderClassName)
            throws InstantiationException, ClassNotFoundException, IllegalAccessException {
        if (null == securityProviderClassName) {
            throw new InstantiationException(
                    String.format("Security provider class name property (%s) set to null.",
                            "org.hyperledger.fabric.sdk.security_provider_class_name"));
        } else if ("org.hyperledger.fabric.sdk.security.default_jdk_provider".equals(securityProviderClassName)) {
            return null;
        } else {
            Class aClass = null;
            try {
                securityProviderClassName = SafeText.checkSecurityProviderClassName(securityProviderClassName);
                aClass = Class.forName(securityProviderClassName);
            } catch (Exception e) {
                logger.error(String.format("load securityProviderClassName err: %s", e.getMessage()));
                throw new ClassNotFoundException(
                        String.format("load securityProviderClassName err: %s", e.getMessage()));
            }

            if (null == aClass) {
                throw new InstantiationException("Getting class for security provider returned null");
            } else if (!Provider.class.isAssignableFrom(aClass)) {
                throw new InstantiationException(
                        String.format("Class for security provider %s is not a Java security provider", aClass.getName()));
            } else {
                Provider securityProvider = (Provider) aClass.newInstance();
                return securityProvider;
            }
        }
    }

    @Override
    public CryptoSuiteFactory getCryptoSuiteFactory() {
        return HLSDKJCryptoSuiteFactory.instance();
    }

    @Override
    public Properties getProperties() {
        Properties properties = new Properties();
        properties.setProperty("org.hyperledger.fabric.sdk.hash_algorithm", this.hashAlgorithm);
        properties.setProperty("org.hyperledger.fabric.sdk.security_level", Integer.toString(this.securityLevel));
        properties.setProperty("org.hyperledger.fabric.sdk.crypto.certificate_format", this.CERTIFICATE_FORMAT);
        properties.setProperty("org.hyperledger.fabric.sdk.crypto.default_signature_algorithm", SIGNATURE_ALGORITHM);
        return properties;
    }


    private static KeyPair convertBcToJceKeyPair(AsymmetricCipherKeyPair bcKeyPair) throws Exception {
        byte[] pkcs8Encoded = PrivateKeyInfoFactory.createPrivateKeyInfo(bcKeyPair.getPrivate()).getEncoded();
        PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(pkcs8Encoded);
        byte[] spkiEncoded = SubjectPublicKeyInfoFactory.createSubjectPublicKeyInfo(bcKeyPair.getPublic()).getEncoded();
        X509EncodedKeySpec spkiKeySpec = new X509EncodedKeySpec(spkiEncoded);
        KeyFactory keyFac = KeyFactory.getInstance("EC");
        return new KeyPair(keyFac.generatePublic(spkiKeySpec), keyFac.generatePrivate(pkcs8KeySpec));
    }

    @Override
    public KeyPair keyGen() {
        try {
//            AsymmetricCipherKeyPair asymmetricCipherKeyPair = SM2Util.generateKeyPairParameter();
            return SM2Util.generateKeyPair();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public ECPrivateKeyParameters getPrivateKey(PrivateKey key) {
        ECPrivateKey ecprivateKey = (ECPrivateKey) key;
        BigInteger d = ecprivateKey.getS();
        ECDomainParameters params = DOMAIN_PARAMS;
        ECPrivateKeyParameters prikey = new ECPrivateKeyParameters(d, params);
        return prikey;
    }

    public ECPublicKeyParameters getPublicKey(PublicKey key) {
        BCECPublicKey ecpublicKey = (BCECPublicKey) key;
        ECDomainParameters params = DOMAIN_PARAMS;
        ECPublicKeyParameters pubkey = new ECPublicKeyParameters(
                CURVE.createPoint(ecpublicKey.getW().getAffineX(), ecpublicKey.getW().getAffineY()), params);
        return pubkey;
    }

    @Override
    public byte[] sign(PrivateKey key, byte[] plainText) throws CryptoException {
        logger.debug("SM Signature");
        try {
            byte[] sign = SM2Util.sign((BCECPrivateKey) key, plainText);
            return sign;
        } catch (org.bouncycastle.crypto.CryptoException e) {
            throw new CryptoException("Unable to sign", e);
        }
    }

    @Override
    public boolean verify(byte[] pemCertificate, String signatureAlgorithm, byte[] signature, byte[] plainText)
            throws CryptoException {
        boolean isVerified = false;

        CryptoException ex;
        try {
            X509Certificate certificate = this.getX509Certificate(pemCertificate);
            if (certificate != null) {
                isVerified = this.validateCertificate((Certificate) certificate);
                if (isVerified) {
                    Signature verifier = Signature.getInstance(CURVE_ALGORITHM, PROVIDER_SHORTNAME);
                    verifier.initVerify(certificate);
                    verifier.update(plainText, 0, plainText.length);
                    isVerified = verifier.verify(signature);
                }
            }

            return isVerified;
        } catch (InvalidKeyException ie) {
            ex = new CryptoException(
                    "Cannot verify signature. Error is: " + ie.getMessage() + "\r\nCertificate: " +
                            DatatypeConverter.printHexBinary(pemCertificate), ie);
            logger.error(ex.getMessage(), ex);
            throw ex;
        } catch (NoSuchAlgorithmException | SignatureException | NoSuchProviderException e) {
            ex = new CryptoException("Cannot verify. Signature algorithm is invalid. Error is: " + e.getMessage(),
                    e);
            logger.error(ex.getMessage(), ex);
            throw ex;
        }
    }

    public boolean verify(PublicKey pubKey, byte[] withId, byte[] srcData, byte[] sign) {
        boolean isVerified = SM2Util.verify((BCECPublicKey) pubKey, withId, srcData, sign);
        return isVerified;
    }



    @SuppressWarnings("unchecked")
    private X509Certificate getX509Certificate(byte[] pemCertificate) throws CryptoException {
        X509Certificate ret = null;
        CryptoException rete = null;
        List<Provider> providerList = new LinkedList(Arrays.asList(Security.getProviders()));
        if (this.SECURITY_PROVIDER != null) {
            providerList.add(this.SECURITY_PROVIDER);
        }

        try {
            providerList.add(BouncyCastleProvider.class.newInstance());
        } catch (Exception var22) {
            logger.warn(var22);
        }

        Iterator var5 = providerList.iterator();
        while (var5.hasNext()) {
            Provider provider = (Provider) var5.next();

            try {
                if (null != provider) {
                    CertificateFactory certFactory = CertificateFactory.getInstance(this.CERTIFICATE_FORMAT, provider);
                    if (null != certFactory) {
                        ByteArrayInputStream bis = new ByteArrayInputStream(pemCertificate);
                        Throwable var9 = null;

                        try {
                            Certificate certificate = certFactory.generateCertificate(bis);
                            if (certificate instanceof X509Certificate) {
                                ret = (X509Certificate) certificate;
                                rete = null;
                                break;
                            }
                        } catch (Throwable var23) {
                            var9 = var23;
                            throw var23;
                        } finally {
                            if (bis != null) {
                                if (var9 != null) {
                                    try {
                                        bis.close();
                                    } catch (Throwable var21) {
                                        var9.addSuppressed(var21);
                                    }
                                } else {
                                    bis.close();
                                }
                            }

                        }
                    }
                }
            } catch (Exception var25) {
                rete = new CryptoException(var25.getMessage(), var25);
            }
        }

        if (null != rete) {
            throw rete;
        } else {
            if (ret == null) {
                logger.error("Could not convert pem bytes");
            }

            return ret;
        }
    }

    @Override
    public byte[] hash(byte[] plainText) {
        return SM3Util.hash(plainText);
    }

    @Override
    public void loadCACertificates(Collection<Certificate> certificates) throws CryptoException {
        if (certificates != null && certificates.size() != 0) {
            try {
                Iterator var2 = certificates.iterator();

                while (var2.hasNext()) {
                    Certificate cert = (Certificate) var2.next();
                    this.addCACertificateToTrustStore(cert);
                }

            } catch (InvalidArgumentException var4) {
                throw new CryptoException("Unable to add certificate to trust store. Error: " + var4.getMessage(),
                        var4);
            }
        } else {
            throw new CryptoException("Unable to load CA certificates. List is empty");
        }
    }

    private void addCACertificateToTrustStore(Certificate certificate)
            throws InvalidArgumentException, CryptoException {
        String alias;
        if (certificate instanceof X509Certificate) {
            alias = ((X509Certificate) certificate).getSerialNumber().toString();
        } else {
            alias = Integer.toString(certificate.hashCode());
        }

        this.addCACertificateToTrustStore(certificate, alias);
    }

    private void addCACertificateToTrustStore(Certificate caCert, String alias)
            throws InvalidArgumentException, CryptoException {
        if (alias != null && !alias.isEmpty()) {
            if (caCert == null) {
                throw new InvalidArgumentException("Certificate cannot be null.");
            } else {
                try {
                    if (config.extraLogLevel(10) && null != diagnosticFileDumper) {
                        logger.trace("Adding cert to trust store. certificate");
                    }

                    this.getTrustStore().setCertificateEntry(alias, caCert);
                } catch (KeyStoreException var5) {
                    String emsg = "Unable to add CA certificate to trust store. Error: " + var5.getMessage();
                    logger.error(emsg, var5);
                    throw new CryptoException(emsg, var5);
                }
            }
        } else {
            throw new InvalidArgumentException(
                    "You must assign an alias to a certificate when adding to the trust store.");
        }
    }

    public KeyStore getTrustStore() throws CryptoException {
        if (this.trustStore == null) {
            this.createTrustStore();
        }

        return this.trustStore;
    }

    private void createTrustStore() throws CryptoException {
        try {
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load((InputStream) null, (char[]) null);
            this.setTrustStore(keyStore);
        } catch (NoSuchAlgorithmException | CertificateException | IOException | InvalidArgumentException | KeyStoreException var2) {
            throw new CryptoException("Cannot create trust store. Error: " + var2.getMessage(), var2);
        }
    }

    private void setTrustStore(KeyStore keyStore) throws InvalidArgumentException {
        if (keyStore == null) {
            throw new InvalidArgumentException("Need to specify a java.security.KeyStore input parameter");
        } else {
            this.trustStore = keyStore;
        }
    }

    public boolean validateCertificate(Certificate cert) {
        if (cert == null) {
            return false;
        } else {
            boolean isValidated;
            try {
                KeyStore keyStore = this.getTrustStore();
                PKIXParameters parms = new PKIXParameters(keyStore);
                parms.setRevocationEnabled(false);
                CertPathValidator certValidator = CertPathValidator
                        .getInstance(CertPathValidator.getDefaultType(), PROVIDER_SHORTNAME);
                ArrayList<Certificate> start = new ArrayList();
                start.add(cert);
                CertificateFactory certFactory = CertificateFactory.getInstance(this.CERTIFICATE_FORMAT, PROVIDER_SHORTNAME);
                CertPath certPath = certFactory.generateCertPath(start);
                certValidator.validate(certPath, parms);
                isValidated = true;
            } catch (InvalidAlgorithmParameterException | NoSuchAlgorithmException | CertificateException | CertPathValidatorException | CryptoException | NoSuchProviderException | KeyStoreException var9) {
                logger.error("Cannot validate certificate. Error is: " + var9.getMessage() + "\r\nCertificate" + cert
                        .toString());
                isValidated = false;
            }

            return isValidated;
        }
    }

    public void addCACertificatesToTrustStore(BufferedInputStream bis)
            throws CryptoException, InvalidArgumentException {
        if (bis == null) {
            throw new InvalidArgumentException("The certificate stream bis cannot be null");
        } else {
            try {
                Collection<? extends Certificate> certificates = this.cf.generateCertificates(bis);
                Iterator var3 = certificates.iterator();

                while (var3.hasNext()) {
                    Certificate certificate = (Certificate) var3.next();
                    this.addCACertificateToTrustStore(certificate);
                }

            } catch (CertificateException var5) {
                throw new CryptoException("Unable to add CA certificate to trust store. Error: " + var5.getMessage(),
                        var5);
            }
        }
    }

    boolean validateCertificate(byte[] certPEM) {
        if (certPEM == null) {
            return false;
        } else {
            try {
                X509Certificate certificate = this.getX509Certificate(certPEM);
                if (null == certificate) {
                    throw new Exception("Certificate transformation returned null");
                } else {
                    return this.validateCertificate(certificate);
                }
            } catch (Exception e) {
                logger.error("Cannot validate certificate. Error is: "
                        + e.getMessage() + "\r\nCertificate (PEM, hex): "
                        + DatatypeConverter.printHexBinary(certPEM));
                return false;
            }
        }
    }

    @Override
    public void loadCACertificatesAsBytes(Collection<byte[]> certificatesBytes) throws CryptoException {
        if (certificatesBytes != null && certificatesBytes.size() != 0) {
            StringBuilder sb = new StringBuilder(1000);
            ArrayList<Certificate> certList = new ArrayList();

            byte[] certBytes;
            for (Iterator var4 = certificatesBytes.iterator(); var4.hasNext();
                 certList.add(this.bytesToCertificate(certBytes))) {
                certBytes = (byte[]) var4.next();
                if (null != diagnosticFileDumper) {
                    sb.append("certificate to load:\n").append(new String(certBytes, StandardCharsets.UTF_8));
                }
            }

            this.loadCACertificates(certList);
            if (diagnosticFileDumper != null && sb.length() > 1) {
                logger.trace("loaded certificates");
            }
        } else {
            throw new CryptoException("List of CA certificates is empty. Nothing to load.");
        }
    }


    @Override
    public String generateCertificationRequest(String name, KeyPair keypair) throws InvalidArgumentException {
        try {
            SM2PublicKey rootPub = new SM2PublicKey(keypair.getPublic().getAlgorithm(),
                    (BCECPublicKey) keypair.getPublic());
            X500Principal x500Principal = new X500Principal("CN=" + name);
            X500Name x500Name = X500Name.getInstance(x500Principal.getEncoded());
            PKCS10CertificationRequest rootCSR = CommonUtil.createCSR(x500Name, rootPub, keypair.getPrivate(),
                    SM2X509CertMaker.SIGN_ALGO_SM3WITHSM2);

            return certificationRequestToPEM(rootCSR);
        } catch (Exception var7) {
            logger.error(var7);
            throw new InvalidArgumentException(var7);
        }
    }

    private String certificationRequestToPEM(PKCS10CertificationRequest csr) throws IOException {
        PemObject pemCSR = new PemObject("CERTIFICATE REQUEST", csr.getEncoded());

        StringWriter str = new StringWriter();
        JcaPEMWriter pemWriter = new JcaPEMWriter(str);
        pemWriter.writeObject(pemCSR);
        pemWriter.close();
        str.close();
        return str.toString();
    }

    @Override
    public PrivateKey bytesToPrivateKey(byte[] pemKey) throws Exception {
        Reader pemReader = new StringReader(new String(pemKey, StandardCharsets.UTF_8));
        PEMParser pemParser = new PEMParser(pemReader);
        JcaPEMKeyConverter converter = (new JcaPEMKeyConverter());

        Object object = pemParser.readObject();
        return converter.getPrivateKey((PrivateKeyInfo) object);
    }

    public PublicKey bytesToPublicKey(byte[] pemKey) throws Exception {
        Reader pemReader = new StringReader(new String(pemKey, StandardCharsets.UTF_8));
        PEMParser pemParser = new PEMParser(pemReader);
        JcaPEMKeyConverter converter = (new JcaPEMKeyConverter());

        Object object = pemParser.readObject();
        return converter.getPublicKey((SubjectPublicKeyInfo) object);
    }

    @Override
    public Certificate bytesToCertificate(byte[] certBytes) throws CryptoException {
        if (certBytes != null && certBytes.length != 0) {
            return this.getX509Certificate(certBytes);
        } else {
            throw new CryptoException("bytesToCertificate: input null or zero length");
        }
    }

    public byte[] certificateToDER(String certificatePEM) {
        byte[] content = null;

        try {
            PemReader pemReader = new PemReader(new StringReader(certificatePEM));
            Throwable var4 = null;

            try {
                PemObject pemObject = pemReader.readPemObject();
                if (pemObject != null) {
                    content = pemObject.getContent();
                }
            } catch (Throwable var14) {
                var4 = var14;
                throw var14;
            } finally {
                if (pemReader != null) {
                    if (var4 != null) {
                        try {
                            pemReader.close();
                        } catch (Throwable var13) {
                            var4.addSuppressed(var13);
                        }
                    } else {
                        pemReader.close();
                    }
                }

            }
        } catch (IOException e) {
            logger.error(String.format("readPemObject error: %s", e.getMessage()));
        }

        return content;
    }

}
