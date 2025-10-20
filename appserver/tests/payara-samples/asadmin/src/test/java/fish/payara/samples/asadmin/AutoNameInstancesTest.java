/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2019 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.samples.asadmin;

import fish.payara.samples.NotMicroCompatible;
import fish.payara.samples.ServerOperations;
import fish.payara.samples.SincePayara;

import org.glassfish.embeddable.CommandResult;
import org.junit.Test;

@SincePayara("5.193")
@NotMicroCompatible("This asadmin command is not supported on Micro")
public class AutoNameInstancesTest extends AsadminTest {
    @Test
    public void testInstanceNameConflict() {
        // Skip test if not running in server context
        if (!ServerOperations.isServer()) {
            return;
        }
        
        String domainName = ServerOperations.getDomainName();
        String conflictInstanceName = "Scrumptious-Swordfish";
        
        try {
            // Create expected conflict if it doesn't already exist.
            CommandResult result = asadmin("list-instances", "-t", "--nostatus");
            if (!result.getOutput().contains(conflictInstanceName)) {
                asadmin("create-instance",
                        "--node", "localhost-" + domainName,
                        conflictInstanceName);
            }

            result = asadmin("create-instance",
                    "--autoname", "true",
                    "--node", "localhost-" + domainName,
                    "-T",
                    conflictInstanceName);
            
            try {
                assertSuccess(result);
            } finally {
                // Cleanup
                String generatedInstanceName = result.getOutput().trim();
                if (!generatedInstanceName.isEmpty()) {
                    asadmin("delete-instance", generatedInstanceName);
                }
            }
        } finally {
            // Cleanup conflict instance
            try {
                asadmin("delete-instance", conflictInstanceName);
            } catch (Exception e) {
                // Ignore if instance doesn't exist
            }
        }
    }

    @Test
    public void testGenerateInstanceName() {
        // Skip test if not running in server context
        if (!ServerOperations.isServer()) {
            return;
        }
        
        String domainName = ServerOperations.getDomainName();
        String generatedInstanceName = null;
        
        try {
            CommandResult result = asadmin("create-instance",
                    "-a",
                    "--node", "localhost-" + domainName,
                    "--extraTerse", "true");

            assertSuccess(result);
            generatedInstanceName = result.getOutput().trim();
        } finally {
            // Cleanup
            if (generatedInstanceName != null && !generatedInstanceName.isEmpty()) {
                try {
                    asadmin("delete-instance", generatedInstanceName);
                } catch (Exception e) {
                    // Ignore cleanup errors
                }
            }
        }
    }

}
