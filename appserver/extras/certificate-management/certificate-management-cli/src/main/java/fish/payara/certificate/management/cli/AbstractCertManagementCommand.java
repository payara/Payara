/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2020 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
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
package fish.payara.certificate.management.cli;

import com.sun.enterprise.admin.cli.Environment;
import com.sun.enterprise.admin.cli.ProgramOptions;
import com.sun.enterprise.admin.cli.cluster.SynchronizeInstanceCommand;
import com.sun.enterprise.admin.servermgmt.KeystoreManager;
import com.sun.enterprise.admin.servermgmt.RepositoryException;
import com.sun.enterprise.admin.servermgmt.cli.LocalDomainCommand;
import com.sun.enterprise.universal.xml.MiniXmlParser;
import com.sun.enterprise.universal.xml.MiniXmlParserException;
import com.sun.enterprise.util.SystemPropertyConstants;
import fish.payara.certificate.management.CertificateManagementDomainConfigUtils;
import fish.payara.certificate.management.CertificateManagementKeytoolCommands;
import org.glassfish.api.Param;
import org.glassfish.api.admin.CommandException;
import org.glassfish.config.support.TranslatedConfigView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

/**
 * Parent class from which other certificate management commands can extend. Contains the common methods and params.
 */
public abstract class AbstractCertManagementCommand extends LocalDomainCommand {

    @Param(name = "domain_name", optional = true, alias = "domainname")
    protected String domainName0;

    @Param(name = "listener", optional = true)
    protected String listener;

    // For passing to AbstractCertManagementLocalInstanceCommand - see LocalInstanceCommand
    @Param(name = "nodedir", optional = true, alias = "agentdir")
    protected String nodeDir0;

    // For passing to AbstractCertManagementLocalInstanceCommand - see LocalInstanceCommand
    @Param(name = "node", optional = true, alias = "nodeagent")
    protected String node0;

    @Param(name = "target", optional = true, defaultValue = SystemPropertyConstants.DAS_SERVER_NAME)
    protected String target;

    protected String userArgAlias;

    protected File keystore;
    protected File truststore;
    protected char[] keystorePassword;
    protected char[] truststorePassword;
    protected char[] masterPassword;

    @Override
    protected void validate() throws CommandException {
        setDomainName(domainName0);
        super.validate();
    }

    /**
     * Gets the passwords for the key and trust store.
     *
     * @param parser    The {@link MiniXmlParser} for extracting info from the domain.xml
     * @param listener  The name of the HTTP or IIOP listener to get the key or trust store passwords from. Can be null.
     * @param serverDir The directory of the target instance, used for accessing the domain-passwords store
     * @throws MiniXmlParserException If there's an issue reading the domain.xml
     * @throws CommandException       If there's an issue getting the master password
     */
    protected void getKeyAndTrustStorePasswords(MiniXmlParser parser, String listener, File serverDir)
            throws MiniXmlParserException, CommandException {
        getKeyStorePassword(parser, listener, serverDir);
        getTrustStorePassword(parser, listener, serverDir);
    }

    /**
     * Gets the passwords for the key store.
     *
     * @param parser    The {@link MiniXmlParser} for extracting info from the domain.xml
     * @param listener  The name of the HTTP or IIOP listener to get the key or trust store passwords from. Can be null.
     * @param serverDir The directory of the target instance, used for accessing the domain-passwords store
     * @throws MiniXmlParserException If there's an issue reading the domain.xml
     * @throws CommandException       If there's an issue getting the master password
     */
    protected void getKeyStorePassword(MiniXmlParser parser, String listener, File serverDir)
            throws MiniXmlParserException, CommandException {
        if (listener != null) {
            // Check if listener has a password set
            keystorePassword = CertificateManagementDomainConfigUtils.getPasswordFromListener(parser, listener, "key-store-password");
        }

        if (keystorePassword != null && keystorePassword.length > 0) {
            // Expand alias if required
            if (new String(keystorePassword).startsWith("${ALIAS=")) {
                JCEKSDomainPasswordAliasStore passwordAliasStore = new JCEKSDomainPasswordAliasStore(
                        serverDir.getPath() + File.separator + "config" + File.separator +
                                "domain-passwords", masterPassword());
                keystorePassword = passwordAliasStore.get(
                        TranslatedConfigView.getAlias(new String(keystorePassword), "ALIAS"));
            }
        } else {
            // Default to master
            keystorePassword = masterPassword();
        }
    }

    /**
     * Gets the passwords for the trust store.
     *
     * @param parser    The {@link MiniXmlParser} for extracting info from the domain.xml
     * @param listener  The name of the HTTP or IIOP listener to get the key or trust store passwords from. Can be null.
     * @param serverDir The directory of the target instance, used for accessing the domain-passwords store
     * @throws MiniXmlParserException If there's an issue reading the domain.xml
     * @throws CommandException       If there's an issue getting the master password
     */
    protected void getTrustStorePassword(MiniXmlParser parser, String listener, File serverDir)
            throws MiniXmlParserException, CommandException {
        if (listener != null) {
            // Check if listener has a password set
            truststorePassword = CertificateManagementDomainConfigUtils.getPasswordFromListener(parser, listener, "trust-store-password");
        }

        if (truststorePassword != null && truststorePassword.length > 0) {
            // Expand alias if required
            if (new String(truststorePassword).startsWith("${ALIAS=")) {
                JCEKSDomainPasswordAliasStore passwordAliasStore = new JCEKSDomainPasswordAliasStore(
                        serverDir.getPath() + File.separator + "config" + File.separator +
                                "domain-passwords", masterPassword());
                truststorePassword = passwordAliasStore.get(
                        TranslatedConfigView.getAlias(new String(truststorePassword), "ALIAS"));
            }
        } else {
            // Default to master
            truststorePassword = masterPassword();
        }
    }

    /**
     * Gets the master password
     *
     * @return The master password in a char array
     * @throws CommandException If there's an issue getting the master password
     */
    protected char[] masterPassword() throws CommandException {
        if (masterPassword == null || masterPassword.length == 0) {
            masterPassword = getMasterPassword().toCharArray();
        }

        return masterPassword;
    }

    /**
     * Parses the key & trust store locations and their passwords from the domain.xml
     *
     * @throws CommandException if there's an issue parsing the domain.xml
     */
    protected void parseKeyAndTrustStores() throws CommandException {
        try {
            MiniXmlParser parser = new MiniXmlParser(getDomainXml(), target);
            keystore = CertificateManagementDomainConfigUtils.resolveKeyStore(parser, listener, getDomainRootDir());
            truststore = CertificateManagementDomainConfigUtils.resolveTrustStore(parser, listener, getDomainRootDir());
            getKeyAndTrustStorePasswords(parser, listener, getDomainRootDir());
        } catch (MiniXmlParserException miniXmlParserException) {
            throw new CommandException("Error parsing domain.xml", miniXmlParserException);
        }
    }

    /**
     * Parses the key store location and its password from the domain.xml
     *
     * @throws CommandException if there's an issue parsing the domain.xml
     */
    protected void parseKeyStore() throws CommandException {
        try {
            MiniXmlParser parser = new MiniXmlParser(getDomainXml(), target);
            keystore = CertificateManagementDomainConfigUtils.resolveKeyStore(parser, listener, getDomainRootDir());
            getKeyStorePassword(parser, listener, getDomainRootDir());
        } catch (MiniXmlParserException miniXmlParserException) {
            throw new CommandException("Error parsing domain.xml", miniXmlParserException);
        }
    }

    /**
     * Parses the trust store location and its password from the domain.xml
     *
     * @throws CommandException if there's an issue parsing the domain.xml
     */
    protected void parseTrustStore() throws CommandException {
        try {
            MiniXmlParser parser = new MiniXmlParser(getDomainXml(), target);
            truststore = CertificateManagementDomainConfigUtils.resolveTrustStore(parser, listener, getDomainRootDir());
            getTrustStorePassword(parser, listener, getDomainRootDir());
        } catch (MiniXmlParserException miniXmlParserException) {
            throw new CommandException("Error parsing domain.xml", miniXmlParserException);
        }
    }

    /**
     * Adds a certificate or key to the key store.
     *
     * @param file The key or certificate to add
     * @throws CommandException If there's an issue accessing the key store
     */
    protected void addToKeyStore(File file) throws CommandException {
        try {
            addToStore(file, keystore, keystorePassword);
        } catch (RepositoryException ex) {
            throw new CommandException(ex);
        }
    }

    /**
     * Adds a certificate or key to the trust store.
     *
     * @param file The key or certificate to add
     * @throws CommandException If there's an issue accessing the trust store
     */
    protected void addToTrustStore(File file) throws CommandException {
        try {
            addToStore(file, truststore, truststorePassword);
        } catch (RepositoryException ex) {
            throw new CommandException(ex);
        }
    }

    /**
     * Helper method to add a certificate or bundle to the key or trust store.
     *
     * @param file The key or certificate to add
     * @throws CommandException If there's an issue accessing the key store
     */
    private void addToStore(File file, File keyOrTrustStore, char[] password) throws RepositoryException {
        try {
            KeystoreManager.KeytoolExecutor keytoolExecutor = new KeystoreManager.KeytoolExecutor(
                    CertificateManagementKeytoolCommands.constructImportCertKeytoolCommand(
                            file, keyOrTrustStore, password, userArgAlias),
                    60);

            keytoolExecutor.execute("certNotAdded", keyOrTrustStore);
        } catch (RepositoryException re) {
            if (re.getMessage().contains("No certificate data found")
                    || re.getMessage().contains("signed fields invalid")
                    || re.getMessage().contains("Input not an X.509 certificate")) {
                logger.fine("Couldn't add file as a certificate, attempting to add as a store");
                KeystoreManager.KeytoolExecutor keytoolExecutor = new KeystoreManager.KeytoolExecutor(
                        CertificateManagementKeytoolCommands.constructImportKeystoreKeytoolCommand(
                                file, keyOrTrustStore, password, password, userArgAlias),
                        60);
                keytoolExecutor.execute("certNotAdded", keyOrTrustStore);
            } else {
                throw re;
            }
        }
    }

    /**
     * Removes a certificate or key from the key store.
     *
     * @throws CommandException If there's an issue accessing the key store
     */
    protected void removeFromKeyStore() throws CommandException {
        try {
            KeyStore store = KeyStore.getInstance(KeyStore.getDefaultType());
            store.load(new FileInputStream(keystore), keystorePassword);
            removeFromStore(store, keystore, keystorePassword);
        } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException ex) {
            throw new CommandException(ex);
        }
    }

    /**
     * Removes a certificate or key from the trust store.
     *
     * @throws CommandException If there's an issue accessing the key store
     */
    protected void removeFromTrustStore() throws CommandException {
        try {
            KeyStore store = KeyStore.getInstance(KeyStore.getDefaultType());
            store.load(new FileInputStream(truststore), truststorePassword);
            removeFromStore(store, truststore, truststorePassword);
        } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException ex) {
            throw new CommandException(ex);
        }
    }

    /**
     * Helper method to remove a certificate or key from the key or trust store.
     *
     * @throws CommandException If there's an issue accessing the key store
     */
    private void removeFromStore(KeyStore store, File keyOrTrustStore, char[] password)
            throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {
        store.deleteEntry(userArgAlias);
        try (FileOutputStream out = new FileOutputStream(keyOrTrustStore)) {
            store.store(out, password);
            out.flush();
        }
    }

    /**
     * Parent class for the local instance version of certificate management commands to inherit from.
     */
    protected abstract class AbstractLocalInstanceCertManagementCommand extends SynchronizeInstanceCommand {

        protected boolean alreadySynced = false;
        protected boolean defaultKeystore = false;
        protected boolean defaultTruststore = false;

        public AbstractLocalInstanceCertManagementCommand(ProgramOptions programOpts, Environment env) {
            super.programOpts = programOpts;
            super.env = env;
        }

        @Override
        protected void validate() throws CommandException {
            if (ok(target)) {
                instanceName = target;
            }
            if (ok(nodeDir0)) {
                nodeDir = nodeDir0;
            }
            if (ok(node0)) {
                node = node0;
            }

            super.validate();
        }

        /**
         * Parses the key & trust store locations and their passwords from the domain.xml, syncing with the DAS if
         * the files are not yet present
         *
         * @throws CommandException If there's an issue parsing the domain.xml
         */
        protected void parseKeyAndTrustStores() throws CommandException {
            try {
                File domainXml = getDomainXml();
                if (!domainXml.exists()) {
                    logger.info("No domain.xml found, syncing with the DAS...");
                    synchronizeInstance();
                    alreadySynced = true;
                }

                MiniXmlParser parser = new MiniXmlParser(domainXml, target);
                keystore = CertificateManagementDomainConfigUtils.resolveKeyStore(parser, listener, instanceDir);
                truststore = CertificateManagementDomainConfigUtils.resolveTrustStore(parser, listener, instanceDir);
                getKeyAndTrustStorePasswords(parser, listener, instanceDir);
            } catch (MiniXmlParserException miniXmlParserException) {
                throw new CommandException("Error parsing domain.xml", miniXmlParserException);
            }
        }

        /**
         * Parses the key store location and its password from the domain.xml, syncing with the DAS if
         * the files are not yet present
         *
         * @throws CommandException if there's an issue parsing the domain.xml
         */
        protected void parseKeyStore() throws CommandException {
            try {
                File domainXml = getDomainXml();
                if (!domainXml.exists()) {
                    logger.info("No domain.xml found, syncing with the DAS...");
                    synchronizeInstance();
                    alreadySynced = true;
                }

                MiniXmlParser parser = new MiniXmlParser(domainXml, target);
                keystore = CertificateManagementDomainConfigUtils.resolveKeyStore(parser, listener, instanceDir);
                getKeyStorePassword(parser, listener, instanceDir);
            } catch (MiniXmlParserException miniXmlParserException) {
                throw new CommandException("Error parsing domain.xml", miniXmlParserException);
            }
        }

        /**
         * Parses the trust store location and its password from the domain.xml, syncing with the DAS if
         * the files are not yet present
         *
         * @throws CommandException if there's an issue parsing the domain.xml
         */
        protected void parseTrustStore() throws CommandException {
            try {
                File domainXml = getDomainXml();
                if (!domainXml.exists()) {
                    logger.info("No domain.xml found, syncing with the DAS...");
                    synchronizeInstance();
                    alreadySynced = true;
                }

                MiniXmlParser parser = new MiniXmlParser(domainXml, target);
                truststore = CertificateManagementDomainConfigUtils.resolveTrustStore(parser, listener, instanceDir);
                getTrustStorePassword(parser, listener, instanceDir);
            } catch (MiniXmlParserException miniXmlParserException) {
                throw new CommandException("Error parsing domain.xml", miniXmlParserException);
            }
        }

        /**
         * Resolves the key and trust store locations and checks if either are set to the default value
         *
         * @return true if either the key or trust store are set to default
         */
        protected boolean checkDefaultKeyOrTrustStore() {
            defaultKeystore = keystore.getAbsolutePath()
                    .equals(CertificateManagementDomainConfigUtils.DEFAULT_KEYSTORE
                            .replace("${com.sun.aas.instanceRoot}", instanceDir.getAbsolutePath()));
            defaultTruststore = truststore.getAbsolutePath()
                    .equals(CertificateManagementDomainConfigUtils.DEFAULT_TRUSTSTORE
                            .replace("${com.sun.aas.instanceRoot}", instanceDir.getAbsolutePath()));

            return defaultKeystore || defaultTruststore;
        }

        /**
         * Resolves the key store location and checks if it is set to the default value
         *
         * @return true if the key store is set to default
         */
        protected boolean checkDefaultKeyStore() {
            defaultKeystore = keystore.getAbsolutePath()
                    .equals(CertificateManagementDomainConfigUtils.DEFAULT_KEYSTORE
                            .replace("${com.sun.aas.instanceRoot}", instanceDir.getAbsolutePath()));

            return defaultKeystore;
        }

        /**
         * Resolves the trust store location and checks if it is set to the default value
         *
         * @return true if the trust store is set to default
         */
        protected boolean checkDefaultTrustStore() {
            defaultTruststore = truststore.getAbsolutePath()
                    .equals(CertificateManagementDomainConfigUtils.DEFAULT_TRUSTSTORE
                            .replace("${com.sun.aas.instanceRoot}", instanceDir.getAbsolutePath()));

            return defaultTruststore;
        }


    }
}
