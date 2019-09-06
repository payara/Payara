package fish.payara.samples;

import com.gargoylesoftware.htmlunit.WebClient;
import static java.nio.file.Files.copy;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.impl.Jdk14Logger;
import static java.math.BigInteger.ONE;
import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.DAYS;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;
import static java.util.logging.Level.FINEST;
import java.util.logging.Logger;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import static org.omnifaces.utils.Lang.isEmpty;
import org.omnifaces.utils.security.Certificates;
import static org.omnifaces.utils.security.Certificates.createTempJKSKeyStore;
import static org.omnifaces.utils.security.Certificates.createTempJKSTrustStore;
import static org.omnifaces.utils.security.Certificates.generateRandomRSAKeys;
import static org.omnifaces.utils.security.Certificates.getCertificateChainFromServer;

/**
 * Various high level Java EE 7 samples specific operations to execute against
 * the various servers used for running the samples
 * 
 * @author arjan
 *
 */
public class ServerOperations {
    
    private static final Logger logger = Logger.getLogger(ServerOperations.class.getName());
    
    /**
     * Add the default test user and credentials to the identity store of
     * supported containers
     */
    public static void addUserToContainerIdentityStore(String username, String groups) {

        // TODO: abstract adding container managed users to utility class
        // TODO: consider PR for sending CLI commands to Arquillian

        String javaEEServer = System.getProperty("javaEEServer");

        if ("glassfish-remote".equals(javaEEServer) || "payara-remote".equals(javaEEServer)) {

            System.out.println("Adding user for glassfish-remote");

            List<String> cmd = new ArrayList<>();

            cmd.add("create-file-user");
            cmd.add("--groups");
            cmd.add(groups);
            cmd.add("--passwordfile");
            cmd.add(Paths.get("").toAbsolutePath() + "/src/test/resources/password.txt");

            cmd.add(username);

            CliCommands.payaraGlassFish(cmd);
        } else {
            if (javaEEServer == null) {
                System.out.println("javaEEServer not specified");
            } else {
                System.out.println(javaEEServer + " not supported");
            }
        }

        // TODO: support other servers than Payara and GlassFish

        // WildFly ./bin/add-user.sh -a -u u1 -p p1 -g g1
    }

    /**
     * Add the default test user and credentials to the identity store of 
     * supported containers
     */
    public static void addUsersToContainerIdentityStore() {
        
        // TODO: abstract adding container managed users to utility class
        // TODO: consider PR for sending CLI commands to Arquillian
        
        String javaEEServer = System.getProperty("javaEEServer");
        
        if ("glassfish-remote".equals(javaEEServer) || "payara-remote".equals(javaEEServer)) {
            
            System.out.println("Adding user for glassfish-remote");
            
            List<String> cmd = new ArrayList<>();
            
            cmd.add("create-file-user");
            cmd.add("--groups");
            cmd.add("g1");
            cmd.add("--passwordfile");
            cmd.add(Paths.get("").toAbsolutePath() + "/src/test/resources/password.txt");
            
            cmd.add("u1");
            
            CliCommands.payaraGlassFish(cmd);
        } else {
            if (javaEEServer == null) {
                System.out.println("javaEEServer not specified");
            } else {
                System.out.println(javaEEServer + " not supported");
            }
        }
        
        // TODO: support other servers than Payara and GlassFish
        
        // WildFly ./bin/add-user.sh -a -u u1 -p p1 -g g1
    }
    
    public static void addMavenJarsToContainerLibFolder(String pathToPomFile, String mavenCoordinates) {
        String javaEEServer = System.getProperty("javaEEServer");
        
        if ("glassfish-remote".equals(javaEEServer) || "payara-remote".equals(javaEEServer)) {
            
            String gfHome = System.getProperty("glassfishRemote_gfHome");
            if (gfHome == null) {
                logger.info("glassfishRemote_gfHome not specified");
                return;
            }
            
            Path gfHomePath = Paths.get(gfHome);
            if (!gfHomePath.toFile().exists()) {
                logger.severe("glassfishRemote_gfHome at " + gfHome + " does not exists");
                return;
            }
            
            if (!gfHomePath.toFile().isDirectory()) {
                logger.severe("glassfishRemote_gfHome at " + gfHome + " is not a directory");
                return;
            }
                        
            String domain = System.getProperty("payara_domain");
            if (domain == null) {
                domain = getPayaraDomainFromServer();
                logger.info("Using domain \"" + domain + "\" obtained from server. If this is not correct use -Dpayara_domain to override.");
            }
            
            Path libsPath = gfHomePath.resolve("glassfish/lib");
            
            if (!libsPath.toFile().exists()) {
                logger.severe("The container lib folder at " + libsPath.toAbsolutePath() + " does not exists");
                logger.severe("Is the domain \"" + domain + "\" correct?");
                return;
            }
            
            logger.info("*** Adding jars to lib folder " + libsPath.toAbsolutePath());
            
            File[] jars = Libraries.resolveMavenCoordinatesToFiles(pathToPomFile, mavenCoordinates);
            
            for (File jar : jars) {
                logger.info("*** Copying  " + jar.toPath());
                try {
                    copy(jar.toPath(), libsPath.resolve(jar.getName()), REPLACE_EXISTING);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            if (javaEEServer == null) {
                System.out.println("javaEEServer not specified");
            } else {
                System.out.println(javaEEServer + " not supported");
            }
        }
        
        
    }
    
    public static void addCertificateToContainerTrustStore(Certificate clientCertificate) {
        
        String javaEEServer = System.getProperty("javaEEServer");
        
        if ("glassfish-remote".equals(javaEEServer) || "payara-remote".equals(javaEEServer)) {
            
            String gfHome = System.getProperty("glassfishRemote_gfHome");
            if (gfHome == null) {
                logger.info("glassfishRemote_gfHome not specified");
                return;
            }
            
            Path gfHomePath = Paths.get(gfHome);
            if (!gfHomePath.toFile().exists()) {
                logger.severe("glassfishRemote_gfHome at " + gfHome + " does not exists");
                return;
            }
            
            if (!gfHomePath.toFile().isDirectory()) {
                logger.severe("glassfishRemote_gfHome at " + gfHome + " is not a directory");
                return;
            }
                        
            String domain = System.getProperty("payara_domain", "domain1");
            if (domain != null) {
                domain = getPayaraDomainFromServer();
                logger.info("Using domain \"" + domain + "\" obtained from server. If this is not correct use -Dpayara_domain to override.");
            }
            
            Path cacertsPath = gfHomePath.resolve("glassfish/domains/" + domain + "/config/cacerts.jks");
            
            if (!cacertsPath.toFile().exists()) {
                logger.severe("The container trust store at " + cacertsPath.toAbsolutePath() + " does not exists");
                logger.severe("Is the domain \"" + domain + "\" correct?");
                return;
            }
            
            logger.info("*** Adding certificate to container trust store: " + cacertsPath.toAbsolutePath());
        
            KeyStore keyStore = null;
            try (InputStream in = new FileInputStream(cacertsPath.toAbsolutePath().toFile())) {
                keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
                keyStore.load(in, "changeit".toCharArray());
                
                keyStore.setCertificateEntry("arquillianClientTestCert", clientCertificate);
                
                keyStore.store(new FileOutputStream(cacertsPath.toAbsolutePath().toFile()), "changeit".toCharArray());
            } catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException e) {
                throw new IllegalStateException(e);
            }
            
            restartContainer(domain);
        } else {
            if (javaEEServer == null) {
                System.out.println("javaEEServer not specified");
            } else {
                System.out.println(javaEEServer + " not supported");
            }
        }
        
    }
    
    public static URL toContainerHttps(URL url) {
        if ("https".equals(url.getProtocol())) {
            return url;
        }
        
        String javaEEServer = System.getProperty("javaEEServer");
        
        // String protocol, String host, int port, String file
        
        if ("glassfish-remote".equals(javaEEServer) || "payara-remote".equals(javaEEServer)) {
            
            try {
                URL httpsUrl = new URL(
                    "https",
                    url.getHost(),
                    8181,
                    url.getFile()
                );
                
                System.out.println("Changing base URL from " + url + " into " + httpsUrl);
                
                return httpsUrl;
                
            } catch (MalformedURLException e) {
                System.out.println("Failure creating HTTPS URL");
                e.printStackTrace();
                logger.log(Level.SEVERE, "Failure creating HTTPS URL", e);
            }
            
        } else {
            if (javaEEServer == null) {
                System.out.println("javaEEServer not specified");
            } else {
                System.out.println(javaEEServer + " not supported");
            }
        }
        
        return null;
    }
    
    public static String getPayaraDomainFromServer() {
        System.out.println("Getting Payara domain from server");
        
        List<String> output = new ArrayList<>();
        List<String> cmd = new ArrayList<>();
        
        cmd.add("list-domains");
        
        CliCommands.payaraGlassFish(cmd, output);
        
        String domain = null;
        for (String line : output) {
            if (line.contains(" not running")) {
                continue;
            }
            
            if (line.endsWith(" running")) {
                domain = line.substring(0, line.lastIndexOf(" running"));
                break;
            }
        }
        
        return domain;
    }
    
    public static void addContainerSystemProperty(String key, String value) {
        String javaEEServer = System.getProperty("javaEEServer");
        
        if ("glassfish-remote".equals(javaEEServer) || "payara-remote".equals(javaEEServer)) {
            
            System.out.println("Adding system property");
            
            List<String> cmd = new ArrayList<>();
            
            cmd.add("create-jvm-options");
            cmd.add("-D" + key + "=\"" + value + "\"");
            
            CliCommands.payaraGlassFish(cmd);
            
        } else {
            if (javaEEServer == null) {
                System.out.println("javaEEServer not specified");
            } else {
                System.out.println(javaEEServer + " not supported");
            }
        }
    }
    
    public static void restartContainer() {
        restartContainer(null);
    }
    
    public static void restartContainer(String domain) {
        // Arquillian connectors can stop/start already, but not on demand by code
        
        String javaEEServer = System.getProperty("javaEEServer");
        
        if ("glassfish-remote".equals(javaEEServer) || "payara-remote".equals(javaEEServer)) {
            
            System.out.println("Restarting domain");
            
            List<String> cmd = new ArrayList<>();
            
            cmd.add("restart-domain");
            
            String restartDomain = domain;
            if (restartDomain == null) {
                restartDomain = System.getProperty("payara_domain");
            }
            
            if (restartDomain == null) {
                restartDomain = getPayaraDomainFromServer();
            }
            
            cmd.add(restartDomain);
            
            CliCommands.payaraGlassFish(cmd);
            
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            
        } else {
            if (javaEEServer == null) {
                System.out.println("javaEEServer not specified");
            } else {
                System.out.println(javaEEServer + " not supported");
            }
        }
    }
    
    public static void restartContainerDebug() {
        // Arquillian connectors can stop/start already, but not on demand by code
        
        String javaEEServer = System.getProperty("javaEEServer");
        
        if ("glassfish-remote".equals(javaEEServer) || "payara-remote".equals(javaEEServer)) {
            
            System.out.println("Stopping domain");
            
            List<String> cmd = new ArrayList<>();
            
            cmd.add("stop-domain");
            
            CliCommands.payaraGlassFish(cmd);
            
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            
            System.out.println("Starting domain");
            
            cmd = new ArrayList<>();
            
            cmd.add("start-domain");
            
            CliCommands.payaraGlassFish(cmd);
            
            System.out.println("Command returned");
            
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            
            System.out.println("After sleep");
        }
    }
    
    public static void setupContainerJDBCIDigestIdentityStore() {
        
        String javaEEServer = System.getProperty("javaEEServer");
        
        if ("glassfish-remote".equals(javaEEServer) || "payara-remote".equals(javaEEServer)) {
            
            System.out.println("Setting up container JDBC identity store for " + javaEEServer);
            
            List<String> cmd = new ArrayList<>();
            
            cmd.add("create-auth-realm");
            cmd.add("--classname");
            cmd.add("com.sun.enterprise.security.auth.realm.jdbc.JDBCRealm");
            cmd.add("--property");
            cmd.add(
                "jaas-context=jdbcDigestRealm:" + 
                "encoding=HASHED:" + 
                "password-column=password:" + 
                "datasource-jndi=java\\:comp/DefaultDataSource:" + 
                "group-table=grouptable:"+ 
                "charset=UTF-8:" + 
                "user-table=usertable:" + 
                "group-name-column=groupname:" + 
                "digest-algorithm=None:" + 
                "user-name-column=username");
            
            cmd.add("eesamplesdigestrealm");
            
            CliCommands.payaraGlassFish(cmd);
        } else {
            if (javaEEServer == null) {
                System.out.println("javaEEServer not specified");
            } else {
                System.out.println(javaEEServer + " not supported");
            }
        }
        
        // TODO: support other servers than Payara and GlassFish
        
        // WildFly ./bin/add-user.sh -a -u u1 -p p1 -g g1
    }

    public static X509Certificate createSelfSignedCertificate(KeyPair keys) {
        try {
            Provider provider = new BouncyCastleProvider();
            Security.addProvider(provider);
            return new JcaX509CertificateConverter()
                    .setProvider(provider)
                    .getCertificate(
                            new X509v3CertificateBuilder(
                                    new X500Name("CN=lfoo, OU=bar, O=kaz, L=zak, ST=lak, C=UK"),
                                    ONE,
                                    Date.from(now()),
                                    Date.from(now().plus(1, DAYS)),
                                    new X500Name("CN=lfoo, OU=bar, O=kaz, L=zak, ST=lak, C=UK"),
                                    SubjectPublicKeyInfo.getInstance(keys.getPublic().getEncoded()))
                                    .build(
                                            new JcaContentSignerBuilder("SHA256WithRSA")
                                                    .setProvider(provider)
                                                    .build(keys.getPrivate())));
        } catch (CertificateException | OperatorCreationException e) {
            throw new IllegalStateException(e);
        }
    }

    public static URL getHostFromCertificate(X509Certificate[] serverCertificateChain, URL existingURL) {
        try {
            URL httpsUrl = new URL(
                    existingURL.getProtocol(),
                    Certificates.getHostFromCertificate(serverCertificateChain),
                    existingURL.getPort(),
                    existingURL.getFile()
            );

            System.out.println("Changing base URL from " + existingURL + " into " + httpsUrl + "\n");

            return httpsUrl;

        } catch (MalformedURLException e) {
            System.out.println("Failure creating HTTPS URL");
            e.printStackTrace();

            System.out.println("FAILED to get CN. Using existing URL: " + existingURL);

            return existingURL;
        }
    }

    public static void enableSSLDebug() {
        System.setProperty("javax.net.debug", "ssl:handshake");

        System.getProperties().put("org.apache.commons.logging.simplelog.defaultlog", "debug");
        Logger.getLogger("com.gargoylesoftware.htmlunit.httpclient.HtmlUnitSSLConnectionSocketFactory").setLevel(FINEST);
        Logger.getLogger("org.apache.http.conn.ssl.SSLConnectionSocketFactory").setLevel(FINEST);
        Log logger = LogFactory.getLog(org.apache.http.conn.ssl.SSLConnectionSocketFactory.class);
        ((Jdk14Logger) logger).getLogger().setLevel(FINEST);
        logger = LogFactory.getLog(com.gargoylesoftware.htmlunit.httpclient.HtmlUnitSSLConnectionSocketFactory.class);
        ((Jdk14Logger) logger).getLogger().setLevel(FINEST);
        Logger.getGlobal().getParent().getHandlers()[0].setLevel(FINEST);
    }

    public static String createClientKeyStore(){

        Security.addProvider(new BouncyCastleProvider());

        // Enable to get detailed logging about the SSL handshake on the client
        // For an explanation of the TLS handshake see: https://tls.ulfheim.net
        if (System.getProperty("ssl.debug") != null) {
            enableSSLDebug();
        }


        // ### Generate keys for the client, create a certificate, and add those to a new local key store
        // Generate a Private/Public key pair for the client
        KeyPair clientKeyPair = generateRandomRSAKeys();

        // Create a certificate containing the client public key and signed with the private key
        X509Certificate clientCertificate = createSelfSignedCertificate(clientKeyPair);

        // Create a new local key store containing the client private key and the certificate
        String clientKeyStorePath = createTempJKSKeyStore(clientKeyPair.getPrivate(), clientCertificate);
        System.setProperty("javax.net.ssl.keystore", clientKeyStorePath);

        // Enable to get detailed logging about the SSL handshake on the server
        if (System.getProperty("ssl.debug") != null) {
            System.out.println("Setting server SSL debug on");
            addContainerSystemProperty("javax.net.debug", "ssl:handshake");
        }

        // Only test TLS v1.2 for now
        System.setProperty("jdk.tls.client.protocols", "TLSv1.2");

        // Add the client certificate that we just generated to the trust store of the server.
        // That way the server will trust our certificate.
        // Set the actual domain used with -Dpayara_domain=[domain name]
        addCertificateToContainerTrustStore(clientCertificate);

        return clientKeyStorePath;
    }

    public static URL createClientTrustStore(WebClient webClient, URL base, String clientKeyStorePath) throws FileNotFoundException, IOException {

        URL baseHttps = ServerOperations.toContainerHttps(base);
        if (baseHttps == null) {
            throw new IllegalStateException("No https URL could be created from " + base);
        }

        // ### Ask the server for its certificate and add that to a new local trust store
        // Server -> client : the trust store certificates are used to validate the certificate sent
        // by the server
        X509Certificate[] serverCertificateChain = getCertificateChainFromServer(baseHttps.getHost(), baseHttps.getPort());

        if (!isEmpty(serverCertificateChain)) {

            System.out.println("Obtained certificate from server. Storing it in client trust store");

            String trustStorePath = createTempJKSTrustStore(serverCertificateChain);
            System.setProperty("javax.net.ssl.truststore", trustStorePath);

            System.out.println("Reading trust store from: " + trustStorePath);

            webClient.getOptions().setSSLTrustStore(new File(trustStorePath).toURI().toURL(), "changeit", "jks");

            // If the use.cnHost property is we try to extract the host from the server
            // certificate and use exactly that host for our requests.
            // This is needed if a server is listening to multiple host names, for instance
            // localhost and example.com. If the certificate is for example.com, we can't
            // localhost for the request, as that will not be accepted.
            if (System.getProperty("use.cnHost") != null) {
                System.out.println("use.cnHost set. Trying to grab CN from certificate and use as host for requests.");
                baseHttps = getHostFromCertificate(serverCertificateChain, baseHttps);
            }
        } else {
            System.out.println("Could not obtain certificates from server. Continuing without custom truststore");
        }

        System.out.println("Using client key store from: " + clientKeyStorePath);

        // Client -> Server : the key store's private keys and certificates are used to sign
        // and sent a reply to the server
        webClient.getOptions().setSSLClientCertificate(new File(clientKeyStorePath).toURI().toURL(), "changeit", "jks");
        return baseHttps;
    }
}

