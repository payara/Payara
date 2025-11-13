/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2020-2021 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package fish.payara.samples;

import static java.util.logging.Level.FINEST;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.gargoylesoftware.htmlunit.WebClient;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.impl.Jdk14Logger;

import static fish.payara.samples.SecurityUtils.getCertificateChainFromServer;
import static fish.payara.samples.SecurityUtils.getHostFromCertificate;
import static fish.payara.samples.ServerOperations.RuntimeType.CLIENT;
import static fish.payara.samples.ServerOperations.ServerType.MICRO;
import java.io.FileWriter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URISyntaxException;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.Globals;


/**
 * Various high level Java EE 7 samples specific operations to execute against
 * the various servers used for running the samples
 *
 * @author arjan
 */
public class ServerOperations {
    private static final Logger logger = Logger.getLogger(ServerOperations.class.getName());

    private static final String ERR_MESSAGE_UNSUPPORTED
        = "Check 'payara.containerType' system property configuration";
    private static final String CLIENT_CERTIFICATE_FILE = "payara-test-client.crt";
    private static final String CLIENT_CERTIFICATE_PATH = "certificates";
    private static final String DEFAULT_ALIAS = "arquillianClientTestCert";

    public enum ServerType {
        SERVER, MICRO
    }

    public enum RuntimeType {
        SERVER, CLIENT
    }

    private static final ServerEnvironment serverEnvironment;
    private static final ServerType serverType;
    private static final RuntimeType runtimeType;

    static {
        String packageName = ServerOperations.class.getClassLoader().getClass()
                .getPackage().getName();
        if (packageName.startsWith("org.glassfish") ||
                packageName.startsWith("com.sun.enterprise")) {
            runtimeType = RuntimeType.SERVER;
        } else {
            runtimeType = RuntimeType.CLIENT;
        }

        String serverTypeStr = System.getProperty("payara.containerType");
        if (serverTypeStr != null) {
            serverType = ServerType.valueOf(serverTypeStr);
            serverEnvironment = null;
        } else {
            ServiceLocator svcLocator = Globals.getDefaultHabitat();
            if (svcLocator != null) {
                serverEnvironment = svcLocator.getService(ServerEnvironment.class);
                if (serverEnvironment != null) {
                    serverType = serverEnvironment.isMicro() ? MICRO : ServerType.SERVER;
                } else {
                    throw new IllegalStateException(ERR_MESSAGE_UNSUPPORTED);

                }
            } else {
                throw new IllegalStateException(ERR_MESSAGE_UNSUPPORTED);
            }
        }
    }

    public static boolean isMicro() {
        return serverType == MICRO;
    }

    public static boolean isServer() {
        return serverType == ServerType.SERVER;
    }

    public static void addUserToContainerIdentityStore(String username, String groups) {
        addUserToContainerIdentityStore("file", username, groups);
    }

    /**
     * Add the default test user and credentials to the identity store of
     * supported containers
     */
    public static void addUserToContainerIdentityStore(String authRealm, String username, String groups) {
        if (runtimeType == RuntimeType.SERVER) {
            throw new IllegalStateException(ERR_MESSAGE_UNSUPPORTED);
        }
        List<String> cmd = new ArrayList<>();
        cmd.add("create-file-user");
        cmd.add("--groups");
        cmd.add(groups);
        cmd.add("--authrealmname");
        cmd.add(authRealm);
        cmd.add("--passwordfile");
        cmd.add(Paths.get("").toAbsolutePath() + "/src/test/resources/password.txt");

        cmd.add(username);

        CliCommands.payaraGlassFish(cmd);
    }

    /**
     * Add the default test user and credentials to the identity store of
     * supported containers
     */
    public static void addUsersToContainerIdentityStore() {
        addUsersToContainerIdentityStore("u1", "g1", "file");
    }

    public static void addUsersToContainerIdentityStore(String username, String group, String fileAuthRealmName) {
        if (runtimeType == RuntimeType.SERVER) {
            throw new IllegalStateException(ERR_MESSAGE_UNSUPPORTED);
        }
        List<String> cmd = new ArrayList<>();

        cmd.add("create-file-user");
        cmd.add("--groups");
        cmd.add(group);
        cmd.add("--passwordfile");
        cmd.add(Paths.get("").toAbsolutePath() + "/src/test/resources/password.txt");
        cmd.add("--authrealmname");
        cmd.add(fileAuthRealmName);

        cmd.add(username);

        CliCommands.payaraGlassFish(cmd);
    }

    public static Path getDomainPath(String relativePathInDomain) {
        if (runtimeType != RuntimeType.SERVER) {
            throw new IllegalStateException(ERR_MESSAGE_UNSUPPORTED);
        }

        Path instanceRoot = Paths.get(System.getProperty("com.sun.aas.instanceRoot", "none"));
        if (!instanceRoot.toFile().exists()) {
            throw new IllegalStateException("Cannot determine domain path");
        }
        return instanceRoot.resolve(relativePathInDomain);
    }

    static void addCertificateToContainerTrustStore(Certificate clientCertificate, KeyPair clientKeyPair) throws IOException {
        addCertificateToContainerTrustStore(clientCertificate, clientKeyPair, DEFAULT_ALIAS);
    }

    static void addCertificateToContainerTrustStore(Certificate clientCertificate, KeyPair clientKeyPair, String alias) throws IOException {
        if (runtimeType != RuntimeType.SERVER) {
            throw new IllegalStateException(ERR_MESSAGE_UNSUPPORTED);
        }
        Path cacertsPath = getDomainPath("config/cacerts.p12");
        if (!cacertsPath.toFile().exists()) {
            logger.severe("The container trust store at " + cacertsPath.toAbsolutePath() + " does not exists");
            logger.severe("Is the domain \"" + getDomainName() + "\" correct?");
            return;
        }

        logger.info("*** Adding certificate to container trust store: " + cacertsPath.toAbsolutePath());

        KeyStore keyStore;
        try (InputStream in = new FileInputStream(cacertsPath.toAbsolutePath().toFile())) {
            keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(in, "changeit".toCharArray());

            keyStore.setCertificateEntry(alias, clientCertificate);

            keyStore.store(new FileOutputStream(cacertsPath.toAbsolutePath().toFile()), "changeit".toCharArray());
        } catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException e) {
            throw new IllegalStateException(e);
        }

        Path downloadPath = getDomainPath("docroot/" + CLIENT_CERTIFICATE_PATH);
        downloadPath.toFile().mkdirs();
        downloadPath = downloadPath.resolve(CLIENT_CERTIFICATE_FILE);
        downloadPath.toFile().delete();
        try (FileOutputStream fw = new FileOutputStream(downloadPath.toFile())) {
            ObjectOutputStream ostrm = new ObjectOutputStream(fw);
            ostrm.writeObject(clientCertificate);
            ostrm.writeObject(clientKeyPair);
        }
    }

    /**
     * Switch the provided URL to use the admin port if the running server supports
     * it.
     * <p>
     * TODO: add support for embedded. Not necessary while this suite doesn't have
     * an embedded profile
     *
     * @param url the url to transform
     * @return the transformed URL, or null if the running server isn't using an
     *         admin listener.
     */
    public static URL toAdminPort(URL url) {
        try {
            return switchPort(url, 4848, "http");
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Failure creating admin URL", e);
        }
    }

    /**
     * Switch the provided URL to use the secure port if the running server supports
     * it.
     *
     * @param url the url to transform
     * @return the transformed URL, or null if the running server isn't using a
     *         secure listener.
     */
    public static URL toContainerHttps(URL url) {
        if ("https".equals(url.getProtocol())) {
            return url;
        }

        try {
            return switchPort(url, 8181, "https");
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Failure creating HTTPS URL", e);
        }
    }

    /**
     * If the Payara Server being tested supports the provided port, switch the
     * given URL to use that port.
     *
     * @param url      the URL to transform
     * @param port     the target port
     * @param protocol the target protocol to use
     * @return a new URL using the target port, or null if that port isn't supported
     * @throws MalformedURLException if the target URL is invalid
     */
    private static URL switchPort(URL url, int port, String protocol) throws MalformedURLException {
        if (runtimeType != CLIENT) {
            throw new IllegalStateException(ERR_MESSAGE_UNSUPPORTED);
        }
        URL result = new URL(
            protocol,
            url.getHost(),
            port,
            url.getFile()
        );

        logger.info("Changing base URL from " + url + " into " + result);
        return result;
    }

    private static String getPayaraDomainFromServer() {
        if (runtimeType != RuntimeType.SERVER) {
            throw new IllegalStateException(ERR_MESSAGE_UNSUPPORTED);
        }
        return getDomainPath("").getFileName().toString();
    }

    public static void addContainerSystemProperty(String key, String value) {
        if (!isServer()) {
            throw new IllegalStateException(ERR_MESSAGE_UNSUPPORTED);
        }
        logger.info("Adding system property");
        List<String> cmd = new ArrayList<>();

        cmd.add("create-system-properties");
        cmd.add(key + "=\"" + value + "\"");

        CliCommands.payaraGlassFish(cmd);
    }

    /**
     * DO NOT make public or call from outside here.
     * Tests are to be run in parallel and shall not require server restart
     *
     * @param domain
     */
    static void restartDomain() {
        // Arquillian connectors can stop/start already, but not on demand by code

        logger.info("Restarting domain (remote)");
        if (RemoteDomainRestarter.restart()) {
            return;
        }
        logger.info("Restarting domain (local)");
        List<String> cmd = new ArrayList<>();

        cmd.add("restart-domain");

        String restartDomain = getDomainName();

        cmd.add(restartDomain);

        if (CliCommands.payaraGlassFish(cmd) != 0) {
            throw new IllegalStateException("Unable to restart domain");
        }
    }


    public static void setupContainerJDBCIDigestIdentityStore() {
        if (!isServer()) {
            throw new IllegalStateException(ERR_MESSAGE_UNSUPPORTED);
        }
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
    }

    public static void setupContainerFileIdentityStore(String fileRealmName) {
        if (!isServer()) {
            throw new IllegalStateException(ERR_MESSAGE_UNSUPPORTED);
        }
        List<String> cmd = new ArrayList<>();
        cmd.add("create-auth-realm");
        cmd.add("--classname");
        cmd.add("com.sun.enterprise.security.auth.realm.file.FileRealm");
        cmd.add("--property");
        cmd.add("jaas-context=fileRealm:file=" + fileRealmName);
        cmd.add(fileRealmName);

        CliCommands.payaraGlassFish(cmd);
    }


    public static void enableSSLDebug() {
        System.setProperty("javax.net.debug", "ssl:handshake");

        System.getProperties().put("org.apache.commons.logging.simplelog.defaultlog", "debug");
        Logger.getLogger("com.gargoylesoftware.htmlunit.httpclient.HtmlUnitSSLConnectionSocketFactory").setLevel(FINEST);
        Logger.getLogger("org.apache.http.conn.ssl.SSLConnectionSocketFactory").setLevel(FINEST);
        Log sslLogger = LogFactory.getLog(org.apache.http.conn.ssl.SSLConnectionSocketFactory.class);
        ((Jdk14Logger) sslLogger).getLogger().setLevel(FINEST);
        sslLogger = LogFactory.getLog(com.gargoylesoftware.htmlunit.httpclient.HtmlUnitSSLConnectionSocketFactory.class);
        ((Jdk14Logger) sslLogger).getLogger().setLevel(FINEST);
        Logger.getGlobal().getParent().getHandlers()[0].setLevel(FINEST);
    }

    public static String createClientKeyStore() throws IOException {
        // Enable to get detailed logging about the SSL handshake on the client
        // For an explanation of the TLS handshake see: https://tls.ulfheim.net
        if (System.getProperty("ssl.debug") != null) {
            enableSSLDebug();
        }

        String clientKeyStorePath = generateClientKeyStore(false);
        System.setProperty("javax.net.ssl.keystore", clientKeyStorePath);

        setSSLSystemProperties();

        return clientKeyStorePath;
    }

    public static String generateClientKeyStore(boolean addToContainer) throws IOException {
        return generateClientKeyStore(addToContainer, false, DEFAULT_ALIAS);
    }

    /**
     *
     * @param addToContainer Adds the client certificate to the truststore
     * @param expired if true will create an expired SSL certificate
     * @param alias the alias of the keystore, useful to not cause conflict with the cert of another payara-sample
     * @return path to the new keystore created
     * @throws IOException
     */
    public static String generateClientKeyStore(boolean addToContainer, boolean expired, String alias) throws IOException {
        // ### Generate keys for the client, create a certificate, and add those to a new local key store
        // Generate a Private/Public key pair for the client
        KeyPair clientKeyPair = SecurityUtils.generateRandomRSAKeys();

        // Create a certificate containing the client public key and signed with the private key
        X509Certificate clientCertificate;
        if(expired) {
            clientCertificate = SecurityUtils.createExpiredSelfSignedCertificate(clientKeyPair);
        }
        else {
            clientCertificate = SecurityUtils.createSelfSignedCertificate(clientKeyPair);
        }


        if (addToContainer) {
            if (runtimeType != RuntimeType.SERVER) {
                throw new IllegalStateException(ERR_MESSAGE_UNSUPPORTED);
            }

            // Add the client certificate that we just generated to the trust store of the server.
            // That way the server will trust our certificate.
            // Set the actual domain used with -Dpayara.domain.name=[domain name]
            addCertificateToContainerTrustStore(clientCertificate, clientKeyPair, alias);
        }

        // Create a new local key store containing the client private key and the certificate
        return SecurityUtils.createTempJKSKeyStore(clientKeyPair.getPrivate(), clientCertificate);
    }

    private static void setSSLSystemProperties() {
        // Enable to get detailed logging about the SSL handshake on the server
        if (System.getProperty("ssl.debug") != null) {
            logger.info("Setting server SSL debug on");
            addContainerSystemProperty("javax.net.debug", "ssl:handshake");
        }

        // Only test TLS v1.2 for now
        System.setProperty("jdk.tls.client.protocols", "TLSv1.2");
    }

    public static String addClientCertificateFromServer(URL base) throws IOException {
        if (runtimeType != CLIENT) {
           throw new IllegalStateException(ERR_MESSAGE_UNSUPPORTED);
        }

        try (InputStream istr = base.toURI().resolve("..")
                .resolve(String.format("%s/%s", CLIENT_CERTIFICATE_PATH, CLIENT_CERTIFICATE_FILE))
                .toURL().openStream()) {
            ObjectInputStream ois = new ObjectInputStream(istr);
            X509Certificate clientCertificate = (X509Certificate)ois.readObject();
            KeyPair clientKeyPair = (KeyPair)ois.readObject();
            String clientKeyStorePath = SecurityUtils.createTempJKSKeyStore(clientKeyPair.getPrivate(), clientCertificate);
            System.setProperty("javax.net.ssl.keystore", clientKeyStorePath);
            setSSLSystemProperties();
            return clientKeyStorePath;
        } catch (URISyntaxException | ClassNotFoundException ex) {
            throw new IOException(ex);
        }
    }

    public static URL createClientTrustStore(WebClient webClient, URL base, String clientKeyStorePath) throws FileNotFoundException, IOException {
        if (runtimeType != CLIENT) {
            throw new IllegalStateException(ERR_MESSAGE_UNSUPPORTED);
        }

        URL baseHttps = ServerOperations.toContainerHttps(base);
        if (baseHttps == null) {
            throw new IllegalStateException("No https URL could be created from " + base);
        }

        // ### Ask the server for its certificate and add that to a new local trust store
        // Server -> client : the trust store certificates are used to validate the certificate sent
        // by the server
        X509Certificate[] serverCertificateChain = getCertificateChainFromServer(baseHttps.getHost(), baseHttps.getPort());

        if (serverCertificateChain.length == 0) {
            throw new IllegalStateException("Could not obtain certificates from server.");
        }
        logger.info("Obtained certificate from server. Storing it in client trust store");

        String trustStorePath = SecurityUtils.createTempJKSTrustStore(serverCertificateChain);
        System.setProperty("javax.net.ssl.truststore", trustStorePath);

        logger.info("Reading trust store from: " + trustStorePath);

        webClient.getOptions().setSSLTrustStore(new File(trustStorePath).toURI().toURL(), "changeit", "jks");

        logger.info("Using client key store from: " + clientKeyStorePath);

        // Client -> Server : the key store's private keys and certificates are used to sign
        // and sent a reply to the server
        webClient.getOptions().setSSLClientCertificate(new File(clientKeyStorePath).toURI().toURL(), "changeit", "jks");
        return baseURLForServerHost(baseHttps, serverCertificateChain);
    }

    public static URL baseURLForServerHost(URL url) {
        URL httpsUrl = toContainerHttps(url);
        return baseURLForServerHost(url, getCertificateChainFromServer(httpsUrl.getHost(), httpsUrl.getPort()));
    }

    /**
     * transforms URL based on server's SSL host name
     *
     * @param url
     * @param serverCertificateChain
     * @return
     */
    public static URL baseURLForServerHost(URL url, X509Certificate[] serverCertificateChain) {
        // Try to extract the host from the server
        // certificate and use exactly that host for our requests.
        // This is needed if a server is listening to multiple host names, for instance
        // localhost and example.com. If the certificate is for example.com, we can't
        // localhost for the request, as that will not be accepted.
        return getHostFromCertificate(serverCertificateChain, toContainerHttps(url));
    }

    public static URL getClientTrustStoreURL(URL baseHttps) throws MalformedURLException {
        if (runtimeType != CLIENT) {
            throw new IllegalStateException(ERR_MESSAGE_UNSUPPORTED);
        }
        // ### Ask the server for its certificate and add that to a new local trust store
        // Server -> client : the trust store certificates are used to validate the certificate sent
        // by the server
        X509Certificate[] serverCertificateChain = getCertificateChainFromServer(baseHttps.getHost(), baseHttps.getPort());
        if (serverCertificateChain.length == 0) {
            throw new IllegalStateException("Could not obtain certificates from server.");
        }
        logger.info("Obtained certificate from server. Storing it in client trust store");

        String trustStorePath = SecurityUtils.createTempJKSTrustStore(serverCertificateChain);
        System.setProperty("javax.net.ssl.truststore", trustStorePath);

        logger.info("Reading trust store from: " + trustStorePath);
        return new File(trustStorePath).toURI().toURL();
    }

    public static KeyStore getKeyStore(
            final URL keystoreURL,
            final String keystorePassword,
            final String keystoreType) {

        if (keystoreURL == null) {
            return null;
        }

        try {
            final KeyStore keyStore = KeyStore.getInstance(keystoreType);
            final char[] passwordChars = keystorePassword != null ? keystorePassword.toCharArray() : null;
            keyStore.load(keystoreURL.openStream(), passwordChars);
            return keyStore;
        } catch (final IOException | KeyStoreException | NoSuchAlgorithmException | CertificateException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static boolean enableDataGridEncryption() throws IOException {
        if (!isServer() || runtimeType != RuntimeType.SERVER) {
            throw new IllegalStateException(ERR_MESSAGE_UNSUPPORTED);
        }

        if (CliCommands.payaraGlassFish("get-hazelcast-configuration", "--checkencrypted") == 0) {
            logger.info("Data Grid Encryption Already Enabled");
            return false;
        }

        logger.info("Enabling Data Grid Encryption");
        boolean success = true;
        success &= CliCommands.payaraGlassFish("set-hazelcast-configuration", "--encryptdatagrid", "true") == 0;

        logger.info("Generating Encryption Key");
        File passwordfile = new File("datagrid-passwordfile.txt");
        passwordfile.delete();
        try (FileWriter fw = new FileWriter(passwordfile)) {
            fw.append("AS_ADMIN_MASTERPASSWORD=changeit");
        }
        success &= CliCommands.payaraGlassFish("-W", passwordfile.getAbsolutePath(),
                "generate-encryption-key", "--dontcheckifrunning", getDomainName()) == 0;
        if (!success) {
            throw new IllegalStateException("Cannot enable DataGrid Encryption");
        }
        return true;
    }

    public static boolean disableDataGridEncryption() {
        if (!isServer() || runtimeType != RuntimeType.SERVER) {
            throw new IllegalStateException(ERR_MESSAGE_UNSUPPORTED);
        }

        if (CliCommands.payaraGlassFish("get-hazelcast-configuration", "--checkencrypted") != 0) {
            logger.info("Data Grid Encryption Not Enabled");
            return false;
        }

        logger.info("Disabling Data Grid Encryption");
        if (CliCommands.payaraGlassFish("set-hazelcast-configuration", "--encryptdatagrid", "false") != 0) {
            throw new IllegalStateException("Cannot disable DataGrid Encryption");
        }
        return true;
    }

    public static String getDomainName() {
        String domain = System.getProperty("payara.domain.name");
        if (domain == null) {
            domain = getPayaraDomainFromServer();
            if (domain != null) {
                logger.info("Using domain \"" + domain + "\" obtained from server. " +
                        "If this is not correct use -Dpayara.domain.name to override.");
            } else {
                // Default to domain1
                domain = "domain1";
                logger.info("Using default domain \"" + domain + "\".");
            }
        } else {
            logger.info("Using domain \"" + domain + "\" obtained from system property.");
        }

        return domain;
    }
}
