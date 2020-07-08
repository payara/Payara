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
import fish.payara.certificate.management.CertificateManagementCommon;
import org.glassfish.api.Param;
import org.glassfish.api.admin.CommandException;

import java.io.File;
import java.util.Map;

/**
 * Parent class for the shared logic between the list-x-entries commands.
 * @author Andrew Pielage <andrew.pielage@payara.fish>
 */
public abstract class AbstractListStoreEntriesCommand extends AbstractCertManagementCommand {

    @Param(name = "verbose", optional = true, shortName = "v")
    private boolean verbose;

    @Param(name = "alias", optional = true, primary = true)
    private String alias;

    @Override
    protected void validate() throws CommandException {
        userArgAlias = alias;
        super.validate();
    }

    /**
     * Lists the entries from the key or trust store, or a specific entry matching the alias, logging them out.
     * @param keyOrTrustStore The key or trust store to get the entries from
     * @param password The password for the key or trust store
     * @throws CommandException If there's an issue reading from the key or trust store
     */
    protected void listEntries(File keyOrTrustStore, char[] password) throws CommandException {
        Map<String, String> entries = CertificateManagementCommon.getEntries(
                keyOrTrustStore, password, userArgAlias, verbose);
        for (Map.Entry<String, String> entry : entries.entrySet()) {
            logger.info("Alias: " + entry.getKey());
            logger.info(entry.getValue());
            logger.info("\n" + "--------------------" + "\n");
        }
    }

    /**
     * LocalInstance version of AbstractListStoreEntriesCommand.
     */
    protected abstract class AbstractLocalInstanceListStoreEntriesCommand extends AbstractLocalInstanceCertManagementCommand {

        public AbstractLocalInstanceListStoreEntriesCommand(ProgramOptions programOpts, Environment env) {
            super(programOpts, env);
        }
    }
}
