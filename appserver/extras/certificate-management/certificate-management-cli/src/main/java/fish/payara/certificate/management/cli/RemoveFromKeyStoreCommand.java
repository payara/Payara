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

import com.sun.enterprise.admin.cli.CLICommand;
import com.sun.enterprise.admin.cli.CLIConstants;
import com.sun.enterprise.admin.cli.Environment;
import com.sun.enterprise.admin.cli.ProgramOptions;
import com.sun.enterprise.util.SystemPropertyConstants;
import org.glassfish.api.Param;
import org.glassfish.api.admin.CommandException;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;

import java.util.logging.Logger;

/**
 * Command to remove a key or certificate from the target instance or listener's key store.
 *
 * @author Jonathan Coustick
 * @author Andrew Pielage
 */
@Service(name = "remove-from-keystore")
@PerLookup
public class RemoveFromKeyStoreCommand extends AbstractCertManagementCommand {


    private static final Logger logger = Logger.getLogger(CLICommand.class.getPackage().getName());

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
            RemoveFromKeyStoreLocalInstanceCommand localInstanceCommand =
                    new RemoveFromKeyStoreLocalInstanceCommand(programOpts, env);
            localInstanceCommand.validate();
            return localInstanceCommand.executeCommand();
        }

        parseKeyStore();
        removeFromKeyStore();

        return CLIConstants.SUCCESS;
    }

    private class RemoveFromKeyStoreLocalInstanceCommand extends AbstractLocalInstanceCertManagementCommand {

        public RemoveFromKeyStoreLocalInstanceCommand(ProgramOptions programOpts, Environment env) {
            super(programOpts, env);
        }

        protected int executeCommand() throws CommandException {
            parseKeyStore();

            // If the target is not the DAS and is configured to use the default key store, sync with the
            // DAS instead
            if (checkDefaultKeyStore()) {
                logger.warning("The target instance is using the default key store, any new certificates"
                        + " removed directly from instance stores would be lost upon next sync.");

                if (!alreadySynced) {
                    logger.warning("Syncing with the DAS instead of generating a new certificate");
                    synchronizeInstance();
                }

                return CLIConstants.WARNING;
            }

            removeFromKeyStore();

            return CLIConstants.SUCCESS;
        }
    }
}
