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
import com.sun.enterprise.admin.servermgmt.KeystoreManager;
import com.sun.enterprise.admin.servermgmt.RepositoryException;
import com.sun.enterprise.util.SystemPropertyConstants;
import fish.payara.certificate.management.CertificateManagementKeytoolCommands;
import org.glassfish.api.Param;
import org.glassfish.api.admin.CommandException;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;

import java.util.logging.Logger;

/**
 * CLI command for generating self-signed certificates and placing them in an instance or listener's key
 * and trust stores.
 *
 * @author Andrew Pielage
 */
@Service(name = "generate-self-signed-certificate")
@PerLookup
public class GenerateSelfSignedCertificateCommand extends AbstractCertManagementCommand {

    private static final Logger logger = Logger.getLogger(CLICommand.class.getPackage().getName());

    @Param(name = "distinguishedname", alias = "dn")
    private String dn;

    @Param(name = "alternativenames", optional = true, alias = "altnames", separator = ';')
    private String[] altnames;

    @Param(name = "alias", primary = true)
    private String alias;

    @Override
    protected void validate() throws CommandException {
        userArgAlias = alias;
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
        parseKeyAndTrustStores();

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
     * Generates a self-signed certificate and adds it to the target key store
     *
     * @throws CommandException If there's an issue adding the certificate to the key store
     */
    private void addToKeystore() throws CommandException {
        // Run keytool command to generate self-signed cert
        KeystoreManager.KeytoolExecutor keytoolExecutor = new KeystoreManager.KeytoolExecutor(
                CertificateManagementKeytoolCommands.constructGenerateCertKeytoolCommand(keystore, keystorePassword,
                        userArgAlias, dn, altnames), 60);

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
        KeystoreManager manager = new KeystoreManager();
        try {
            manager.copyCert(keystore, truststore, alias, new String(masterPassword()));
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
    private class GenerateSelfSignedCertificateLocalInstanceCommand extends AbstractLocalInstanceCertManagementCommand {

        public GenerateSelfSignedCertificateLocalInstanceCommand(ProgramOptions programOpts, Environment env) {
            super(programOpts, env);
        }

        @Override
        protected int executeCommand() throws CommandException {
            parseKeyAndTrustStores();

            // If the target is not the DAS and is configured to use the default key or trust store, sync with the
            // DAS instead
            if (checkDefaultKeyOrTrustStore()) {
                logger.warning("The target instance is using the default key or trust store, any new certificates"
                        + " added directly to instance stores would be lost upon next sync.");

                if (!alreadySynced) {
                    logger.warning("Syncing with the DAS instead of generating a new certificate");
                    synchronizeInstance();
                }

                if (defaultKeystore && defaultTruststore) {
                    // Do nothing
                } else if (defaultKeystore) {
                    logger.info("Please add self-signed certificate to truststore manually using the add-to-truststore command");
                } else {
                    logger.info("Please add self-signed certificate to keystore manually using the add-to-keystore command");

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