/*
 *    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) [2019] Payara Foundation and/or its affiliates. All rights reserved.
 *
 *    The contents of this file are subject to the terms of either the GNU
 *    General Public License Version 2 only ("GPL") or the Common Development
 *    and Distribution License("CDDL") (collectively, the "License").  You
 *    may not use this file except in compliance with the License.  You can
 *    obtain a copy of the License at
 *    https://github.com/payara/Payara/blob/main/LICENSE.txt
 *    See the License for the specific
 *    language governing permissions and limitations under the License.
 *
 *    When distributing the software, include this License Header Notice in each
 *    file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 *    GPL Classpath Exception:
 *    The Payara Foundation designates this particular file as subject to the "Classpath"
 *    exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *    file that accompanied this code.
 *
 *    Modifications:
 *    If applicable, add the following below the License Header, with the fields
 *    enclosed by brackets [] replaced by your own identifying information:
 *    "Portions Copyright [year] [name of copyright owner]"
 *
 *    Contributor(s):
 *    If you wish your version of this file to be governed by only the CDDL or
 *    only the GPL Version 2, indicate your decision by adding "[Contributor]
 *    elects to include this software in this distribution under the [CDDL or GPL
 *    Version 2] license."  If you don't indicate a single choice of license, a
 *    recipient has the option to distribute your version of this file under
 *    either the CDDL, the GPL Version 2 or to extend the choice of license to
 *    its licensees as provided above.  However, if you add GPL Version 2 code
 *    and therefore, elected the GPL Version 2 license, then the option applies
 *    only if the new code is made subject to such option by the copyright
 *    holder.
 */
package fish.payara.samples.ejb.invoker.security;

import fish.payara.samples.CliCommands;
import fish.payara.samples.NotMicroCompatible;
import fish.payara.samples.PayaraArquillianTestRunner;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import static fish.payara.samples.ServerOperations.addUsersToContainerIdentityStore;
import static java.util.Arrays.asList;

/**
 * Calls an EJB bean from a remote server via ejb-invoker endpoint secured with
 * custom realm.
 *
 */
@RunWith(PayaraArquillianTestRunner.class)
@NotMicroCompatible
public class RemoteBeanCustomRealmTest extends AbstractRemoteBeanSecurityTest {

    private static final String USERNAME = "myuser_customrealm";
    private static final String PASSWORD = "mypassword";
    private static final String ROLE = "invoker";
    private static final String REALM = "file_customrealm";

    @BeforeClass
    public static void enableSecurity() {
        if (!Boolean.parseBoolean(System.getProperty("skipConfig", "false"))) {
            // create the new auth realm
            CliCommands.payaraGlassFish(asList("create-auth-realm",
                    "--classname", "com.sun.enterprise.security.auth.realm.file.FileRealm",
                    "--login-module", "com.sun.enterprise.security.auth.login.FileLoginModule",
                    "--property", "jaas-context=fileRealm:file=" + REALM, REALM));

            // undeploy the ejb-invoker app
            CliCommands.payaraGlassFish(asList("set-ejb-invoker-configuration",
                    "--enabled", "false"));
            // enable the security and deploy the ejb-invoker app
            CliCommands.payaraGlassFish(asList("set-ejb-invoker-configuration",
                    "--enabled", "true",
                    "--securityenabled", "true",
                    "--realmName", REALM));

            // Add user with password and group to the container's native identity store
            addUsersToContainerIdentityStore(USERNAME, ROLE, REALM);
        }
    }

    @AfterClass
    public static void resetSecurity() {
        if (!Boolean.parseBoolean(System.getProperty("skipTestConfigCleanup", "false"))) {
            // delete the auth realm
            CliCommands.payaraGlassFish(asList("delete-auth-realm", REALM));

            // undeploy the ejb-invoker app
            CliCommands.payaraGlassFish(asList("set-ejb-invoker-configuration",
                    "--enabled", "false"));
            // disable the security and deploy the ejb-invoker app
            CliCommands.payaraGlassFish(asList("set-ejb-invoker-configuration",
                    "--enabled", "true",
                    "--securityenabled", "false",
                    "--realmName", "file"));
        }
    }

    @Override
    protected String getUserName() {
        return USERNAME;
    }

    @Override
    protected String getPassword() {
        return PASSWORD;
    }

}
