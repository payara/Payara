/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright (c) 2021 Payara Foundation and/or its affiliates. All rights reserved.
 *
 *  The contents of this file are subject to the terms of either the GNU
 *  General Public License Version 2 only ("GPL") or the Common Development
 *  and Distribution License("CDDL") (collectively, the "License").  You
 *  may not use this file except in compliance with the License.  You can
 *  obtain a copy of the License at
 *  https://github.com/payara/Payara/blob/master/LICENSE.txt
 *  See the License for the specific
 *  language governing permissions and limitations under the License.
 *
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at glassfish/legal/LICENSE.txt.
 *
 *  GPL Classpath Exception:
 *  The Payara Foundation designates this particular file as subject to the "Classpath"
 *  exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *  file that accompanied this code.
 *
 *  Modifications:
 *  If applicable, add the following below the License Header, with the fields
 *  enclosed by brackets [] replaced by your own identifying information:
 *  "Portions Copyright [year] [name of copyright owner]"
 *
 *  Contributor(s):
 *  If you wish your version of this file to be governed by only the CDDL or
 *  only the GPL Version 2, indicate your decision by adding "[Contributor]
 *  elects to include this software in this distribution under the [CDDL or GPL
 *  Version 2] license."  If you don't indicate a single choice of license, a
 *  recipient has the option to distribute your version of this file under
 *  either the CDDL, the GPL Version 2 or to extend the choice of license to
 *  its licensees as provided above.  However, if you add GPL Version 2 code
 *  and therefore, elected the GPL Version 2 license, then the option applies
 *  only if the new code is made subject to such option by the copyright
 *  holder.
 */

package fish.payara.acme.formatter;

import fish.payara.logging.jul.formatter.AnsiColor;
import fish.payara.logging.jul.formatter.ExcludeFieldsSupport.SupplementalAttribute;
import fish.payara.logging.jul.formatter.ODLLogFormatter;
import fish.payara.logging.jul.record.EnhancedLogRecord;

import java.util.Arrays;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.collection.IsArrayWithSize.arrayWithSize;
import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static org.hamcrest.text.StringContainsInOrder.stringContainsInOrder;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author David Matejcek
 */
public class ODLLogFormatterTest {

    private static final String P_TIME = "\\d\\d:\\d\\d:\\d\\d.\\d\\d\\d";
    private static final String P_TIMEZONE = "[0-9:.+-]{6}";
    private static final String P_LEVEL_NAME = "[A-Z]+";
    private static final String P_LEVEL_VALUE = "[0-9]{3,4}";
    private static final String P_LOGGER_NAME = "[a-z.]*";
    private static final String P_MESSAGE_KEY = "[a-zA-Z0-9.]*";
    private static final String P_PRODUCT_ID = ".*";
    private static final String P_TIMESTAMP = "[0-9]{4}\\-[0-9]{2}\\-[0-9]{2}T" + P_TIME + P_TIMEZONE;

    private static final Pattern PATTERN_MULTILINE = Pattern.compile(
        "\\[" + P_TIMESTAMP + "\\]"
        + " \\[" + P_PRODUCT_ID + "\\]"
        + " \\[" + P_LEVEL_NAME + "\\]"
        + " \\[" + P_MESSAGE_KEY + "\\]"
        + " \\[" + P_LOGGER_NAME + "\\]"
        + " \\[tid: _ThreadID=1 _ThreadName=main\\]"
        + " \\[timeMillis: [0-9]+\\]"
        + " \\[levelValue: " + P_LEVEL_VALUE + "\\]"
        + " \\[\\["
    );
    private static final Pattern PATTERN_SINGLELINE = Pattern.compile(
        "\\[" + P_TIMESTAMP + "\\]"
        + " \\[" + P_PRODUCT_ID + "\\]"
        + " \\[" + P_LEVEL_NAME + "\\]"
        + " \\[" + P_MESSAGE_KEY + "\\]"
        + " \\[" + P_LOGGER_NAME + "\\]"
        + " \\[tid: _ThreadID=1 _ThreadName=main\\]"
        + " \\[timeMillis: [0-9]+\\]"
        + " \\[levelValue: " + P_LEVEL_VALUE + "\\]"
        + " .+"
    );

    @Test
    public void nullRecord() {
        assertEquals("", new ODLLogFormatter().format(null));
    }


    @Test
    public void nullMessage() {
        final LogRecord record = new LogRecord(Level.INFO, null);
        final String log = new ODLLogFormatter().format(record);
        assertEquals("", log);
    }


    @Test
    public void simpleLogRecordMultiLineEnabled() {
        final String message = "Ok, this works!";
        final LogRecord record = new LogRecord(Level.INFO, message);
        final ODLLogFormatter formatter = new ODLLogFormatter();
        final String log = formatter.format(record);
        assertNotNull(log, "log");
        final String[] lines = log.split("\n");
        assertAll(
            () -> assertThat(lines, arrayWithSize(2)),
            () -> assertThat(lines[0], matchesPattern(PATTERN_MULTILINE)),
            () -> assertThat(lines[0],
                stringContainsInOrder("0] [] [INFO] [] [] [tid", "main] [timeMillis: ", "] [levelValue: 800] [[")),
            () -> assertThat(lines[1], stringContainsInOrder(message, "]]"))
        );
    }

    @Test
    public void simpleLogRecordMultiLineMessage() {
        final String message = "Ok!\nThis works!";
        final LogRecord record = new LogRecord(Level.INFO, message);
        final ODLLogFormatter formatter = new ODLLogFormatter();
        formatter.setMultiLineMode(false);
        final String log = formatter.format(record);
        assertNotNull(log, "log");
        final String[] lines = log.split("\n");
        assertAll(
            () -> assertThat(lines, arrayWithSize(3)),
            () -> assertThat(lines[0], matchesPattern(PATTERN_MULTILINE)),
            () -> assertThat(lines[0],
                stringContainsInOrder("0] [] [INFO] [] [] [tid", "main] [timeMillis: ", "] [levelValue: 800] [[")),
            () -> assertThat(lines[1], equalTo("  Ok!")),
            () -> assertThat(lines[2], equalTo("This works!]]"))
        );
    }


    @Test
    public void fullLogRecordSingleLine() {
        final String message = "Ok, this works!";
        final LogRecord record = new LogRecord(Level.INFO, message);
        record.setLoggerName("the.test.logger");
        record.setSourceClassName("fish.payara.FakeClass");
        record.setSourceMethodName("fakeMethod");
        final ODLLogFormatter formatter = new ODLLogFormatter();
        formatter.setMultiLineMode(false);
        formatter.setProductId("PAYARA TEST");
        formatter.setPrintRecordNumber(true);
        formatter.setPrintSource(true);

        final String log = formatter.format(record);
        assertNotNull(log, "log");
        final String[] lines = log.split("\n");
        assertAll(
            () -> assertThat(lines, arrayWithSize(1)),
            () -> assertThat(lines[0], matchesPattern(PATTERN_SINGLELINE)),
            () -> assertThat(lines[0], stringContainsInOrder("0] [PAYARA TEST] [INFO] [] [the.test.logger] [tid",
                "main] [timeMillis: ", "] [levelValue: 800] [RECORDNUMBER: ", "]"
                    + " [CLASSNAME: fish.payara.FakeClass] [METHODNAME: fakeMethod] " + message))
        );
    }


    @Test
    public void enhancedLogRecordAndAnsiColoring() {
        final EnhancedLogRecord record = new EnhancedLogRecord(Level.SEVERE, "Ok!", false);
        record.setLoggerName("the.test.logger");
        record.setMessageKey("error.message.key");
        final ODLLogFormatter formatter = new ODLLogFormatter();
        formatter.setAnsiColor(true);
        formatter.setLoggerColor(AnsiColor.BOLD_INTENSE_WHITE);
        formatter.setLevelColors(Collections.singletonMap(Level.SEVERE,AnsiColor.BOLD_INTENSE_RED));

        final String log = formatter.format(record);
        assertNotNull(log, "log");
        final String[] lines = log.split("\n");
        assertAll(
            () -> assertThat(lines, arrayWithSize(2)),
            () -> assertThat(lines[0], stringContainsInOrder(
                "0] []"
                + " [" + AnsiColor.BOLD_INTENSE_RED + "SEVERE" + AnsiColor.RESET + "]"
                + " [error.message.key] [" + AnsiColor.BOLD_INTENSE_WHITE + "the.test.logger" + AnsiColor.RESET + "]"
                + " [tid",
                "main] [timeMillis: ",
                "] [levelValue: 1000] [[")),
            () -> assertThat(lines[1], equalTo("  Ok!]]"))
        );
    }


    @Test
    public void exception() {
        final EnhancedLogRecord record = new EnhancedLogRecord(Level.SEVERE, "Failure!", false);
        record.setThrown(new RuntimeException("Ooops!"));
        final ODLLogFormatter formatter = new ODLLogFormatter();
        final String log = formatter.format(record);
        assertNotNull(log, "log");
        final String[] lines = log.split("\n");
        assertAll(
            () -> assertThat(lines, arrayWithSize(greaterThan(20))),
            () -> assertThat(lines[0], stringContainsInOrder(
                "0] [] [SEVERE] [] [] [tid",
                "main] [timeMillis: ",
                "] [levelValue: 1000] [[")),
            () -> assertThat(lines[1], equalTo("  Failure!")),
            () -> assertThat(lines[2], equalTo("java.lang.RuntimeException: Ooops!"))
        );
    }


    @Test
    public void exclusionsAndCustomTimestampFormat() {
        final LogRecord record = new LogRecord(Level.INFO, "This is a message.");
        final ODLLogFormatter formatter = new ODLLogFormatter();
        formatter.setDateTimeFormatter("HH:mm:ss.SSS");
        formatter.setMultiLineMode(false);
        formatter.setExcludeFields(Arrays.stream(SupplementalAttribute.values()).map(SupplementalAttribute::getId)
            .collect(Collectors.joining(",")));
        final String log = formatter.format(record);
        assertNotNull(log, "log");

        final Pattern pattern = Pattern.compile(
            "\\[" + P_TIME + "\\]"
            + " \\[\\]"
            + " \\[INFO\\]"
            + " \\[\\]"
            + " \\[\\]"
            + " This is a message\\.+\\n\\n"
        );
        assertThat(log, matchesPattern(pattern));
    }
}
