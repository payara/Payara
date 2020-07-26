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

import java.io.File;

@Service(name = "generate-csr")
@PerLookup
public class GenerateCsrCommand extends AbstractCertManagementCommand {

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
            GenerateCsrLocalInstanceCommand localInstanceCommand =
                    new GenerateCsrLocalInstanceCommand(programOpts, env);
            localInstanceCommand.validate();
            return localInstanceCommand.executeCommand();
        }

        // Parse the location of the key store, and the password required to access it
        parseKeyStore();

        // Run keytool command to generate CSR and place in csrLocation
        try {
            generateCsr();
        } catch (CommandException ce) {
            return CLIConstants.ERROR;
        }

        return CLIConstants.SUCCESS;
    }

    /**
     * Generates a CSR
     *
     * @throws CommandException If there's an issue adding the certificate to the key store
     */
    private void generateCsr() throws CommandException {
        // Get CSR install dir and ensure it actually exists
        File csrLocation = new File(getInstallRootPath() + File.separator + "tls");
        if (!csrLocation.exists()) {
            csrLocation.mkdir();
        }

        // Run keytool command to generate self-signed cert
        KeystoreManager.KeytoolExecutor keytoolExecutor = new KeystoreManager.KeytoolExecutor(
                CertificateManagementKeytoolCommands.constructGenerateCertRequestKeytoolCommand(
                        keystore, keystorePassword,
                        new File(csrLocation.getAbsolutePath() + File.separator + userArgAlias + ".csr"),
                        userArgAlias),
                60);

        try {
            keytoolExecutor.execute("csrNotCreated", keystore);
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
    private class GenerateCsrLocalInstanceCommand extends AbstractLocalInstanceCertManagementCommand {

        public GenerateCsrLocalInstanceCommand(ProgramOptions programOpts, Environment env) {
            super(programOpts, env);
        }

        @Override
        protected int executeCommand() throws CommandException {
            parseKeyStore();

            // Run keytool command to generate CSR and place in csrLocation
            try {
                generateCsr();
            } catch (CommandException ce) {
                return CLIConstants.ERROR;
            }

            return CLIConstants.SUCCESS;
        }

    }
}
