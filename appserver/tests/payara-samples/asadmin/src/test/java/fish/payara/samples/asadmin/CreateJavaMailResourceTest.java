/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2020-2024 Payara Foundation and/or its affiliates. All rights reserved.
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
import org.glassfish.embeddable.CommandResult;
import org.junit.Test;

@NotMicroCompatible("This asadmin command is not supported on Micro")
public class CreateJavaMailResourceTest extends AsadminTest {

    @Test
    public void testJavaMailDeploymentGroupRef() {
        try {
            deleteOldResources();
            CommandResult result = asadmin("create-deployment-group", "create-javamail-resource-test-dg");
            assertSuccess(result);

            result = asadmin("create-javamail-resource",
                    "--debug=false",
                    "--storeProtocol=imap",
                    "--auth=false",
                    "--transportProtocol=smtp",
                    "--host=localhost",
                    "--storeProtocolClass=org.eclipse.angus.mail.imap.IMAPStore",
                    "--from=ratatosk@payara.fish",
                    "--transportProtocolClass=org.eclipse.angus.mail.smtp.SMTPTransport",
                    "--enabled=true",
                    "--target=domain",
                    "--mailhost=localhost",
                    "--mailuser=ratatosk",
                    "mail/create-javamail-resource-test");
            assertSuccess(result);

            result = asadmin("create-resource-ref",
                    "--enabled=true",
                    "--target=create-javamail-resource-test-dg",
                    "mail/create-javamail-resource-test");
            assertSuccess(result);
        } finally {
            deleteOldResources();
        }
    }

    private void deleteOldResources() {
        asadmin("delete-resource-ref",
                "--target=create-javamail-resource-test-dg",
                "mail/create-javamail-resource-test");
        if (asadmin("list-deployment-groups").getOutput().contains("create-javamail-resource-test-dg")) {
            asadmin("delete-deployment-group", "create-javamail-resource-test-dg");
        }
        asadmin("delete-javamail-resource", "--target", "domain", "mail/create-javamail-resource-test");
    }
}
