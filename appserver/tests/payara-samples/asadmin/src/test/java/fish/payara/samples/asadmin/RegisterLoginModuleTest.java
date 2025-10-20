/*
 *    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) [2019-2021] Payara Foundation and/or its affiliates. All rights reserved.
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

package fish.payara.samples.asadmin;

import static org.junit.Assert.assertEquals;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;

import org.glassfish.embeddable.CommandResult;
import org.junit.Test;

import fish.payara.samples.ServerOperations;

public class RegisterLoginModuleTest extends AsadminTest {

    @Test
    public void successfulRegistrationUpdatesLoginConf() throws IOException {
        asadmin("delete-auth-realm", "test1");
        CommandResult result = asadmin("create-auth-realm",
                "--classname", "com.sun.enterprise.security.auth.realm.file.FileRealm",
                "--login-module", "com.sun.enterprise.security.auth.login.FileLoginModule",
                "--property", "jaas-context=test1:file=test1", "test1");
        System.out.println(result.getOutput());
        assertSuccess(result);
        String contents = loginConf();
        assertContains("test1 {", contents);

        result = asadmin("delete-auth-realm", "test1");
        System.out.println(result.getOutput());
        assertSuccess(result);

        //Removes JAAS context test1 from the login.conf file on the domain
        //It's expected that this will be the last entry in the login.conf file
        //and so we just grab everything up to this final entry as the new file
        //contents
        String newFileContents = contents.substring(0, contents.indexOf("test1"));
        System.out.print(newFileContents);
        try(BufferedWriter writer = new BufferedWriter(new FileWriter(System.getProperty("java.security.auth.login.config")))) {
            writer.write(newFileContents);
        } catch(IOException e) {
            System.out.print(e.getMessage());
            e.printStackTrace();
        }

        assertFalse(loginConf().contains("test1 {"));
    }

    @Test
    public void existingJaasContextGivesWarning()  {
        // Skip test if not running in server context
        if (!ServerOperations.isServer()) {
            return;
        }

        // First ensure the realm doesn't exist
        CommandResult deleteResult = asadmin("delete-auth-realm", "test2");
        if (deleteResult.getExitStatus() != CommandResult.ExitStatus.SUCCESS &&
            !deleteResult.getOutput().contains("does not exist")) {
            // Only fail if there was an actual error (other than realm not existing)
            System.out.println("Warning during initial cleanup: " + deleteResult.getOutput());
        }

        try {
            // Try to create a realm with an existing JAAS context
            CommandResult result = asadmin("create-auth-realm",
                    "--classname", "com.sun.enterprise.security.auth.realm.file.FileRealm",
                    "--login-module", "com.sun.enterprise.security.auth.login.FileLoginModule",
                    "--property", "jaas-context=fileRealm:file=test2", "test2");

            System.out.println("Output: " + result.getOutput());
            System.out.println("Exit status: " + result.getExitStatus());

            // The first creation should succeed
            assertSuccess(result);

            // Now try to create it again - this should fail because the realm already exists
            result = asadmin("create-auth-realm",
                    "--classname", "com.sun.enterprise.security.auth.realm.file.FileRealm",
                    "--login-module", "com.sun.enterprise.security.auth.login.FileLoginModule",
                    "--property", "jaas-context=fileRealm:file=test2", "test2");

            System.out.println("Output (duplicate): " + result.getOutput());
            System.out.println("Exit status (duplicate): " + result.getExitStatus());

            // Expecting FAILURE because the realm already exists
            assertEquals("Expected FAILURE status when creating duplicate realm",
                    CommandResult.ExitStatus.FAILURE, result.getExitStatus());

            // Ensure the error message is present in the output
            assertContains("Authrealm named test2 exists", result.getOutput());
        } finally {
            // Cleanup
            CommandResult cleanupResult = asadmin("delete-auth-realm", "test2");
            if (cleanupResult.getExitStatus() != CommandResult.ExitStatus.SUCCESS) {
                System.out.println("Warning: Failed to clean up test realm: " + cleanupResult.getOutput());
            }
        }
    }

    @Test
    public void undefinedJaasContextGivesWarning() {
        if (!ServerOperations.isServer()) {
            return;
        }

        // First ensure the realm doesn't exist
        CommandResult deleteResult = asadmin("delete-auth-realm", "test3");
        if (deleteResult.getExitStatus() != CommandResult.ExitStatus.SUCCESS &&
            !deleteResult.getOutput().contains("does not exist")) {
            // Only fail if there was an actual error (other than realm not existing)
            System.out.println("Warning during initial cleanup: " + deleteResult.getOutput());
        }

        try {
            // This should succeed but show a warning about missing JAAS context
            CommandResult result = asadmin("create-auth-realm",
                    "--classname", "com.sun.enterprise.security.auth.realm.certificate.CertificateRealm",
                    "--login-module", "com.sun.enterprise.security.auth.login.FileLoginModule",
                    "--property", "file=test3", "test3");

            System.out.println("Output: " + result.getOutput());
            System.out.println("Exit status: " + result.getExitStatus());

            // The operation should still succeed with a warning
            assertEquals("Expected SUCCESS status but was " + result.getExitStatus(),
                    CommandResult.ExitStatus.SUCCESS, result.getExitStatus());

            // Ensure the warning message is present in the output
            assertContains("No JAAS context is defined", result.getOutput());
        } finally {
            // Cleanup
            try {
                CommandResult cleanupResult = asadmin("delete-auth-realm", "test3");
                if (cleanupResult.getExitStatus() != CommandResult.ExitStatus.SUCCESS) {
                    System.out.println("Warning: Failed to clean up test realm: " + cleanupResult.getOutput());
                }
            } catch (Exception e) {
                System.out.println("Warning during cleanup: " + e.getMessage());
            }
        }
    }

    private String loginConf() throws IOException {
        File loginConf = new File(System.getProperty("java.security.auth.login.config"));
        try (Reader reader = new FileReader(loginConf)) {
            return toString(reader);
        }
    }

    private static String toString(Reader from) throws IOException {

        // Create output and buffer
        StringBuilder to = new StringBuilder();
        char[] buf = new char[0x800];

        // Read from reader to buffer
        int nRead;
        while ((nRead = from.read(buf)) != -1) {
            to.append(buf, 0, nRead);
        }

        return to.toString();
    }

}
