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
package fish.payara.certificate.management.admin;

import com.sun.enterprise.admin.cli.CLICommand;
import com.sun.enterprise.admin.cli.CLIConstants;
import com.sun.enterprise.admin.cli.Environment;
import com.sun.enterprise.admin.cli.ProgramOptions;
import com.sun.enterprise.admin.cli.cluster.SynchronizeInstanceCommand;
import com.sun.enterprise.admin.servermgmt.KeystoreManager;
import com.sun.enterprise.admin.servermgmt.RepositoryException;
import com.sun.enterprise.admin.servermgmt.cli.LocalDomainCommand;
import com.sun.enterprise.universal.xml.MiniXmlParser;
import com.sun.enterprise.universal.xml.MiniXmlParserException;
import com.sun.enterprise.util.SystemPropertyConstants;
import fish.payara.certificate.management.CertificateManagementUtils;
import org.glassfish.api.Param;
import org.glassfish.api.admin.CommandException;
import org.glassfish.config.support.TranslatedConfigView;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;

import java.io.File;
import java.util.logging.Logger;

/**
 * CLI command for generating self-signed certificates and placing them in an instance or listener's key
 * and trust stores.
 *
 * @author Andrew Pielage
 */
@Service(name = "generate-self-signed-certificate")
@PerLookup
public class GenerateSelfSignedCertificateCommand extends LocalDomainCommand {

    private static final Logger logger = Logger.getLogger(CLICommand.class.getPackage().getName());

    @Param(name = "domain_name", optional = true)
    private String domainName0;

    @Param(name = "distinguishedname", alias = "dn")
    private String dn;

    @Param(name = "alternativenames", optional = true, alias = "altnames", separator = ';')
    private String[] altnames;

    @Param(name = "listener", optional = true)
    private String listener;

    @Param(name = "target", optional = true, defaultValue = SystemPropertyConstants.DAS_SERVER_NAME)
    private String target;

    @Param(name = "alias", primary = true)
    private String alias;

    private File keystore;
    private File truststore;
    private char[] keystorePassword;
    private char[] truststorePassword;
    private char[] masterPassword;

    @Override
    protected void validate() throws CommandException {
        setDomainName(domainName0);
        super.validate();
    }

    @Override
    protected int executeCommand() throws CommandException {
        // If we're targetting an instance that isn't the DAS, use a different command
        if (target != null && !target.equals(SystemPropertyConstants.DAS_SERVER_NAME)) {
            GenerateSelfSignedCertificateLocalInstanceCommand localInstanceCommand =
                    new GenerateSelfSignedCertificateLocalInstanceCommand(programOpts, env);
            localInstanceCommand.validate();
            return localInstanceCommand.executeCommand();
        }

        // Parse the location of the key and trust stores, and the passwords required to access them
        try {
            MiniXmlParser parser = new MiniXmlParser(getDomainXml(), target);
            keystore = CertificateManagementUtils.resolveKeyStore(parser, listener, getDomainRootDir());
            truststore = CertificateManagementUtils.resolveTrustStore(parser, listener, getDomainRootDir());
            getStorePasswords(parser, listener, getDomainRootDir());
        } catch (MiniXmlParserException miniXmlParserException) {
            throw new CommandException("Error parsing domain.xml", miniXmlParserException);
        }

        // Run keytool command to generate self-signed cert and place in keystore
        try {
            addToKeystore();
        } catch (CommandException ce) {
            return CLIConstants.ERROR;
        }

        try {
            addToTruststore();
        } catch (CommandException ce) {
            return CLIConstants.WARNING;
        }

        return CLIConstants.SUCCESS;
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
    private void getStorePasswords(MiniXmlParser parser, String listener, File serverDir)
            throws MiniXmlParserException, CommandException {
        if (listener != null) {
            // Check if listener has a password set
            keystorePassword = CertificateManagementUtils.getPasswordFromListener(parser, listener, "key-store-password");
            truststorePassword = CertificateManagementUtils.getPasswordFromListener(parser, listener, "trust-store-password");
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

        if (truststorePassword != null && truststorePassword.length > 0) {
            // Expand alias if required
            if (new String(truststorePassword).startsWith("${ALIAS=")) {
                JCEKSDomainPasswordAliasStore passwordAliasStore = new JCEKSDomainPasswordAliasStore(
                        serverDir.getPath() + File.separator + "config" + File.separator + "domain-passwords",
                        masterPassword());
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
    private char[] masterPassword() throws CommandException {
        if (masterPassword == null || masterPassword.length == 0) {
            masterPassword = getMasterPassword().toCharArray();
        }

        return masterPassword;
    }

    /**
     * Generates a self-signed certificate and adds it to the target key store
     *
     * @throws CommandException If there's an issue adding the certificate to the key store
     */
    private void addToKeystore() throws CommandException {
        // Run keytool command to generate self-signed cert
        KeystoreManager.KeytoolExecutor keytoolExecutor = new KeystoreManager.KeytoolExecutor(
                CertificateManagementUtils.constructGenerateCertKeytoolCommand(keystore, keystorePassword,
                        alias, dn, altnames), 60);

        try {
            keytoolExecutor.execute("certNotCreated", keystore);
        } catch (RepositoryException re) {
            logger.severe(re.getCause().getMessage()
                    .replace("keytool error: java.lang.Exception: ", "")
                    .replace("keytool error: java.io.IOException: ", ""));
            throw new CommandException(re);
        }
    }

    /**
     * Adds the self-signed certificate to the target trust store
     *
     * @throws CommandException If there's an issue adding the certificate to the trust store
     */
    private void addToTruststore() throws CommandException {
        // Run keytool command to place self-signed cert in truststore
        KeystoreManager.KeytoolExecutor keytoolExecutor = new KeystoreManager.KeytoolExecutor(
                CertificateManagementUtils.constructImportCertKeytoolCommand(keystore, truststore, keystorePassword,
                        truststorePassword, alias), 60);

        try {
            keytoolExecutor.execute("certNotTrusted", keystore);
        } catch (RepositoryException re) {
            logger.severe(re.getCause().getMessage()
                    .replace("keytool error: java.lang.Exception: ", "")
                    .replace("keytool error: java.io.IOException: ", ""));
            throw new CommandException(re);
        }
    }

    /**
     * Local instance (non-DAS) version of the parent command. Not intended for use as a standalone CLI command.
     */
    private class GenerateSelfSignedCertificateLocalInstanceCommand extends SynchronizeInstanceCommand {

        public GenerateSelfSignedCertificateLocalInstanceCommand(ProgramOptions programOpts, Environment env) {
            super.programOpts = programOpts;
            super.env = env;
        }

        @Override
        protected void validate() throws CommandException {
            if (ok(target))
                instanceName = target;
            super.validate();
        }

        @Override
        protected int executeCommand() throws CommandException {
            boolean alreadySynced = false;
            try {
                File domainXml = getDomainXml();
                if (!domainXml.exists()) {
                    logger.info("No domain.xml found, syncing with the DAS...");
                    synchronizeInstance();
                    alreadySynced = true;
                }

                MiniXmlParser parser = new MiniXmlParser(domainXml, target);
                keystore = CertificateManagementUtils.resolveKeyStore(parser, listener, instanceDir);
                truststore = CertificateManagementUtils.resolveTrustStore(parser, listener, instanceDir);
                getStorePasswords(parser, listener, instanceDir);
            } catch (MiniXmlParserException miniXmlParserException) {
                throw new CommandException("Error parsing domain.xml", miniXmlParserException);
            }

            // If the target is not the DAS and is configured to use the default key or trust store, sync with the
            // DAS instead
            boolean defaultKeystore = keystore.getAbsolutePath()
                    .equals(CertificateManagementUtils.DEFAULT_KEYSTORE
                            .replace("${com.sun.aas.instanceRoot}", instanceDir.getAbsolutePath()));
            boolean defaultTruststore = truststore.getAbsolutePath()
                    .equals(CertificateManagementUtils.DEFAULT_TRUSTSTORE
                            .replace("${com.sun.aas.instanceRoot}", instanceDir.getAbsolutePath()));

            if (defaultKeystore || defaultTruststore) {
                logger.warning("The target instance is using the default key or trust store, any new certificates"
                        + " added directly to instance stores would be lost upon next sync.");

                if (!alreadySynced) {
                    logger.warning("Syncing with the DAS instead of generating a new certificate");
                    synchronizeInstance();
                }

                if (defaultKeystore && defaultTruststore) {
                    // Do nothing
                } else if (defaultKeystore) {
                    logger.info("Please add self-signed certificate to truststore manually");
                    // TO-DO
                    // logger.info("Look at using asadmin command 'add-to-truststore'");
                } else {
                    logger.info("Please add self-signed certificate to keystore manually");
                    // TO-DO
                    // logger.info("Look at using asadmin command 'add-to-keystore'");
                }

                return CLIConstants.WARNING;
            }

            // Run keytool command to generate self-signed cert and place in keystore
            try {
                addToKeystore();
            } catch (CommandException ce) {
                return CLIConstants.ERROR;
            }

            try {
                addToTruststore();
            } catch (CommandException ce) {
                return CLIConstants.WARNING;
            }

            return CLIConstants.SUCCESS;
        }
    }
}