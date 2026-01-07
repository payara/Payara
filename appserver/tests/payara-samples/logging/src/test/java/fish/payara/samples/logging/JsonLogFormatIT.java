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
package fish.payara.samples.logging;

import com.gargoylesoftware.htmlunit.TextPage;
import com.gargoylesoftware.htmlunit.WebClient;
import fish.payara.samples.CliCommands;
import fish.payara.samples.NotMicroCompatible;
import fish.payara.samples.PayaraArquillianTestRunner;
import fish.payara.samples.PayaraTestShrinkWrap;
import fish.payara.samples.ServerOperations;
import fish.payara.samples.SincePayara;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Scanner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@RunWith(PayaraArquillianTestRunner.class)
@SincePayara("5.2020.5")
@NotMicroCompatible
public class JsonLogFormatIT {

    @ArquillianResource
    private URL baseUrl;

    @Deployment(testable = true)
    public static WebArchive createDeployment() {
        return PayaraTestShrinkWrap.getWebArchive()
                .addPackages(true, "com.gargoylesoftware")
                .addPackages(true, "org.apache.http")
                .addPackages(true, "org.apache.commons")
                .addPackages(true, "net.sourceforge.htmlunit.corejs")
                .addPackages(true, "org.apache.xml.utils")
                .addPackages(true, "org.eclipse.jetty")
                .addClass(LogRecordMapParameterServlet.class)
                .addClass(JsonLogFormatIT.class);
    }

    @Test
    public void mapParameterLoggedIT() throws Exception {
        ArrayList<String> command = new ArrayList<>();
        ArrayList<String> output = new ArrayList<>();
        command.add("list-log-attributes");
        CliCommands.payaraGlassFish(command, output);

        boolean foundFile = false;
        boolean foundConsole = false;
        String originalFileHandlerFormatter = "";
        String originalConsoleHandlerFormatter = "";
        for (String keyValue : output) {
            if (keyValue.startsWith("com.sun.enterprise.server.logging.GFFileHandler.formatter")) {
                originalFileHandlerFormatter = keyValue.substring(
                        keyValue.indexOf("\t<") + 2, keyValue.length() - 1);
                foundFile = true;
                continue;
            }
            if (keyValue.startsWith("java.util.logging.ConsoleHandler.formatter")) {
                originalConsoleHandlerFormatter = keyValue.substring(
                        keyValue.indexOf("\t<") + 2, keyValue.length() - 1);
                foundConsole = true;
                continue;
            }
            if (foundFile && foundConsole) {
                break;
            }
        }

        // Will return null if file could not be determined.
        File logFile = getLogFile();
        if (logFile == null) {
            fail("Could not determine or read log file.");
        }

        try {
            // Set log settings to JSON
            command.clear();
            output.clear();
            command.add("set-log-attributes");
            command.add("java.util.logging.ConsoleHandler.formatter='fish.payara.enterprise.server.logging.JSONLogFormatter'"
                    + ":com.sun.enterprise.server.logging.GFFileHandler.formatter="
                    + "'fish.payara.enterprise.server.logging.JSONLogFormatter'");
            CliCommands.payaraGlassFish(command, output);
            assertEquals("Command set-log-attributes executed successfully.", output.get(output.size() - 1));

            // Invoke page
            try (WebClient client = new WebClient()) {
                TextPage page = client.getPage(baseUrl + "LogRecordMapParameter");
                System.out.println(page.getContent());
            }

            // Check log
            boolean foundMapContents = false;
            try (Scanner scanner = new Scanner(new FileInputStream(logFile))) {
                while (scanner.hasNext()) {
                    String line = scanner.nextLine();
                    if (line.contains("\"" + LogRecordMapParameterServlet.correlationIdKey
                            + "\":\"" + LogRecordMapParameterServlet.correlationIdValue + "\"")) {
                        foundMapContents = true;
                        break;
                    }
                }
            }
            Assert.assertTrue(foundMapContents);
        } finally {
            command.clear();
            output.clear();
            command.add("set-log-attributes");
            command.add("java.util.logging.ConsoleHandler.formatter=" + originalConsoleHandlerFormatter
                    + ":com.sun.enterprise.server.logging.GFFileHandler.formatter=" + originalFileHandlerFormatter);
            CliCommands.payaraGlassFish(command, output);
        }
    }

    private File getLogFile() {
        File logFile = new File(ServerOperations.getDomainPath("logs")
                + File.separator + "server.log");

        if (logFile.exists() && logFile.canRead()) {
            return logFile;
        }
        return null;
    }
}
