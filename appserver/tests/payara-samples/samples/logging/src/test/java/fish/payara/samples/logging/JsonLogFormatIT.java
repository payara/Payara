/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2020-2021 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.samples.logging;

import com.gargoylesoftware.htmlunit.TextPage;
import com.gargoylesoftware.htmlunit.WebClient;

import fish.payara.jul.formatter.JSONLogFormatter;
import fish.payara.jul.handler.PayaraLogHandlerConfiguration.PayaraLogHandlerProperty;
import fish.payara.jul.handler.SimpleLogHandler.SimpleLogHandlerProperty;
import fish.payara.samples.CliCommands;
import fish.payara.samples.NotMicroCompatible;
import fish.payara.samples.PayaraArquillianTestRunner;
import fish.payara.samples.PayaraTestShrinkWrap;
import fish.payara.samples.ServerOperations;
import fish.payara.samples.SincePayara;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.function.Predicate;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static fish.payara.samples.logging.LogRecordMapParameterServlet.correlationIdKey;
import static fish.payara.samples.logging.LogRecordMapParameterServlet.correlationIdValue;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.stringContainsInOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * The {@link LogRecordMapParameterServlet} creates log records with parameters which should be
 * propagated into JSON formatted server.log
 *
 * @author Rudy De Busscher
 * @author David Matejcek
 */
@RunWith(PayaraArquillianTestRunner.class)
@SincePayara("5.2020.5")
@NotMicroCompatible
public class JsonLogFormatIT {

    private static final String P_TIME = "\\d\\d:\\d\\d:\\d\\d.\\d\\d\\d";
    private static final String P_TIMEZONE = "[0-9:.+-]{6}";
    private static final String P_TIMESTAMP = "[0-9]{4}\\-[0-9]{2}\\-[0-9]{2}T" + P_TIME + P_TIMEZONE;
    private static final String P_RECORD = "\\{"
        + "\\\"Timestamp\\\"\\:\\\"" + P_TIMESTAMP + "\\\","
        + "\\\"Level\\\"\\:\\\"INFO\\\","
        + "\\\"Version\\\"\\:\\\"Payara 5[A-Z0-9\\.\\-]+\\\","
        + "\\\"LoggerName\\\"\\:\\\"[a-zA-Z0-9\\.]*\\\","
        + "\\\"ThreadID\\\"\\:\\\"[0-9]+\\\","
        + "\\\"ThreadName\\\"\\:\\\"[a-zA-Z0-9\\:\\(\\)\\-]+\\\","
        + "\\\"TimeMillis\\\"\\:\\\"[0-9]+\\\","
        + "\\\"LevelValue\\\"\\:\\\"800\\\","
        + "(\\\".*\\\"\\:\\\".*\\\",)*"
        + "\\\"LogMessage\\\"\\:\\\".+\\\""
        + "\\}";
    /** Filters just lines containing the servlet class name, we are not interested in other */
    private static final Predicate<String> FILTER = line -> line
        .contains("LoggerName\":\"" + LogRecordMapParameterServlet.class.getName());

    @ArquillianResource
    private URL baseUrl;

    private String originalFileHandlerFormatter;
    private String originalConsoleHandlerFormatter;

    @Deployment(testable = true)
    public static WebArchive createDeployment() {
        return PayaraTestShrinkWrap.getWebArchive()
            .addPackages(true, "com.gargoylesoftware")
            .addPackages(true, "org.apache.http")
            .addPackages(true, "org.apache.commons")
            .addPackages(true, "net.sourceforge.htmlunit.corejs")
            .addPackages(true, "org.apache.xml.utils")
            .addPackages(true, "org.eclipse.jetty")
            .addClass(LogRecordMapParameterServlet.class);
    }


    @Before
    public void initJson() {
        final ArrayList<String> command = new ArrayList<>();
        final ArrayList<String> output = new ArrayList<>();
        command.add("list-log-attributes");
        final int result = CliCommands.payaraGlassFish(command, output);
        assertEquals("list-log-attributes result", 0, result);

        boolean foundFile = false;
        boolean foundConsole = false;
        for (final String keyValue : output) {
            if (keyValue.startsWith(PayaraLogHandlerProperty.FORMATTER.getPropertyFullName())) {
                originalFileHandlerFormatter = keyValue.substring(keyValue.indexOf("\t<") + 2, keyValue.length() - 1);
                foundFile = true;
                continue;
            }
            if (keyValue.startsWith(SimpleLogHandlerProperty.FORMATTER.getPropertyFullName())) {
                originalConsoleHandlerFormatter = keyValue.substring(keyValue.indexOf("\t<") + 2, keyValue.length() - 1);
                foundConsole = true;
                continue;
            }
            if (foundFile && foundConsole) {
                break;
            }
        }
        assertNotNull("originalConsoleHandlerFormatter", originalConsoleHandlerFormatter);
        assertNotNull("originalFileHandlerFormatter", originalFileHandlerFormatter);
    }


    @After
    public void resetLogging() {
        if (originalConsoleHandlerFormatter == null || originalFileHandlerFormatter == null) {
            // nothing to reset, it is broken
            return;
        }
        final ArrayList<String> command = new ArrayList<>();
        final ArrayList<String> output = new ArrayList<>();
        command.add("set-log-attributes");
        command.add(
            SimpleLogHandlerProperty.FORMATTER.getPropertyFullName() + "='" + originalConsoleHandlerFormatter + "'"
            + ":" + PayaraLogHandlerProperty.FORMATTER.getPropertyFullName() + "='" + originalFileHandlerFormatter + "'"
        );
        final int result = CliCommands.payaraGlassFish(command, output);
        assertEquals("set-log-attributes result", 0, result);
        assertEquals("Command set-log-attributes executed successfully.", output.get(output.size() - 1));
    }


    @Test
    public void jsonLogFormattingWithMapParameters() throws Exception {
        final ArrayList<String> command = new ArrayList<>();
        final ArrayList<String> output = new ArrayList<>();
        command.add("set-log-attributes");
        command.add(
            SimpleLogHandlerProperty.FORMATTER.getPropertyFullName() + "='" + JSONLogFormatter.class.getName()
            + "'" + ":" + PayaraLogHandlerProperty.FORMATTER.getPropertyFullName() + "='" + JSONLogFormatter.class.getName() + "'"
        );
        final int result = CliCommands.payaraGlassFish(command, output);
        assertEquals("set-log-attributes result", 0, result);
        assertEquals("Command set-log-attributes executed successfully.", output.get(output.size() - 1));

        Thread.sleep(100L);

        try (WebClient client = new WebClient()) {
            final TextPage page = client.getPage(baseUrl + "LogRecordMapParameter");
            System.out.println(page.getContent());
        }

        final File logFile = getLogFile();
        final List<String> records = new ArrayList<>();
        try (Scanner scanner = new Scanner(new FileInputStream(logFile))) {
            while (scanner.hasNext()) {
                final String line = scanner.nextLine();
                assertThat("line", line, matchesPattern(P_RECORD));
                if (FILTER.test(line)) {
                    records.add(line);
                }
            }
        }
        assertThat("count of lines", records, hasSize(2));
        assertThat(records.get(0), stringContainsInOrder(
            // map used as custom log record fields first
            "\"" + correlationIdKey + "\":\"" + correlationIdValue + "\"",
            // the message is last
            "Message with a map as a parameter: {" + correlationIdKey + "=" + correlationIdValue + "}\"}"
        ));
        assertThat(records.get(1), stringContainsInOrder("fish.payara.samples.logging.LogRecordMapParameterServlet",
            "LogMessage", "Another message with single parameter: Bye!"));

        assertNotNull("logFile", logFile);
        assertThat("logFile.size", logFile.length(), allOf(greaterThan(500L), lessThan(3000L)));
    }


    /** Will return null if file could not be determined. */
    private File getLogFile() {
        final File logFile = ServerOperations.getDomainPath("logs").resolve("server.log").toFile();
        if (logFile.exists() && logFile.canRead()) {
            return logFile;
        }
        return null;
    }
}
