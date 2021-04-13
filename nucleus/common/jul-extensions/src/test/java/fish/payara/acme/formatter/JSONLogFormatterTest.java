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

import fish.payara.logging.jul.cfg.LoggingSystemEnvironment;
import fish.payara.logging.jul.formatter.ExcludeFieldsSupport.SupplementalAttribute;
import fish.payara.logging.jul.formatter.JSONLogFormatter;
import fish.payara.logging.jul.record.EnhancedLogRecord;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;
import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static org.hamcrest.text.StringContainsInOrder.stringContainsInOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author David Matejcek
 */
public class JSONLogFormatterTest {

    private static final String P_TIME = "\\d\\d:\\d\\d:\\d\\d.\\d\\d\\d";
    private static final String P_TIMEZONE = "[0-9:.+-]{6}";
    private static final String P_TIMESTAMP = "[0-9]{4}\\-[0-9]{2}\\-[0-9]{2}T" + P_TIME + P_TIMEZONE;
    private String backupProductId;

    @BeforeEach
    public void initProductId() {
        this.backupProductId = LoggingSystemEnvironment.getProductId();
    }


    @AfterEach
    public void resetProductId() {
        LoggingSystemEnvironment.setProductId(backupProductId);
    }


    @Test
    public void nullRecord() {
        assertEquals("", new JSONLogFormatter().format(null));
    }


    @Test
    public void nullMessage() {
        final LogRecord record = new LogRecord(Level.INFO, null);
        final String log = new JSONLogFormatter().format(record);
        assertEquals("", log);
    }


    @Test
    public void fullLog() {
        LoggingSystemEnvironment.setProductId("PAYARA TEST");
        final String message = "Ok, this works!";
        final LogRecord record = new LogRecord(Level.INFO, message);
        record.setLoggerName("the.test.logger");
        record.setSourceClassName("fish.payara.FakeClass");
        record.setSourceMethodName("fakeMethod");
        final JSONLogFormatter formatter = new JSONLogFormatter();
        formatter.setPrintRecordNumber(true);
        formatter.setPrintSource(true);

        final String log = formatter.format(record);
        assertNotNull(log, "log");
        assertThat(log, matchesPattern("\\{"
            + "\\\"Timestamp\\\"\\:\\\"" + P_TIMESTAMP + "\\\","
            + "\\\"Level\\\"\\:\\\"INFO\\\","
            + "\\\"Version\\\"\\:\\\"PAYARA TEST\\\","
            + "\\\"LoggerName\\\"\\:\\\"the\\.test\\.logger\\\","
            + "\\\"ThreadID\\\"\\:\\\"1\\\","
            + "\\\"ThreadName\\\"\\:\\\"main\\\","
            + "\\\"TimeMillis\\\"\\:\\\"[0-9]+\\\","
            + "\\\"LevelValue\\\"\\:\\\"800\\\","
            + "\\\"LogMessage\\\"\\:\\\"Ok, this works!\\\""
            + "\\}\n"));
    }


    @Test
    public void enhancedLogRecord() {
        final EnhancedLogRecord record = new EnhancedLogRecord(Level.SEVERE, "\"Ok!\"", false);
        record.setLoggerName("the.test.logger");
        record.setMessageKey("error.message.key");
        final JSONLogFormatter formatter = new JSONLogFormatter();

        final String log = formatter.format(record);
        assertNotNull(log, "log");
        assertThat(log, matchesPattern("\\{"
            + "\\\"Timestamp\\\"\\:\\\"" + P_TIMESTAMP + "\\\","
            + "\\\"Level\\\"\\:\\\"SEVERE\\\","
            + "\\\"LoggerName\\\"\\:\\\"the\\.test\\.logger\\\","
            + "\\\"ThreadID\\\"\\:\\\"1\\\","
            + "\\\"ThreadName\\\"\\:\\\"main\\\","
            + "\\\"TimeMillis\\\"\\:\\\"[0-9]+\\\","
            + "\\\"LevelValue\\\"\\:\\\"1000\\\","
            + "\\\"MessageID\\\"\\:\\\"error\\.message\\.key\\\","
            + "\\\"LogMessage\\\"\\:\\\"\\\\\"Ok!\\\\\"\\\""
            + "\\}\n"));
    }


    @Test
    public void exception() {
        final EnhancedLogRecord record = new EnhancedLogRecord(Level.SEVERE, "Failure!", false);
        record.setThrown(new RuntimeException("Ooops!"));
        final JSONLogFormatter formatter = new JSONLogFormatter();
        final String log = formatter.format(record);
        assertNotNull(log, "log");
        assertThat(log, matchesPattern(Pattern.compile("\\{"
            + "\\\"Timestamp\\\"\\:\\\"" + P_TIMESTAMP + "\\\","
            + "\\\"Level\\\"\\:\\\"SEVERE\\\","
            + "\\\"ThreadID\\\"\\:\\\"1\\\","
            + "\\\"ThreadName\\\"\\:\\\"main\\\","
            + "\\\"TimeMillis\\\"\\:\\\"[0-9]+\\\","
            + "\\\"LevelValue\\\"\\:\\\"1000\\\","
            + "\\\"LogMessage\\\"\\:\\\"Failure!\\\","
            + "\\\"Throwable\\\"\\:\\{"
                + "\\\"Exception\\\"\\:\\\"Ooops\\!\\\","
                + "\\\"StackTrace\\\"\\:\\\"java\\.lang\\.RuntimeException\\: Ooops\\!\\\\n\\\\tat fish.+\\\""
            + "\\}"
            + "\\}\n"
            , Pattern.DOTALL)));
    }


    @Test
    public void exclusionsAndCustomTimestampFormat() {
        final LogRecord record = new LogRecord(Level.INFO, "This is a message.");
        // name of the root logger is an empty string!
        record.setLoggerName("");
        final JSONLogFormatter formatter = new JSONLogFormatter();
        formatter.setDateTimeFormatter("HH:mm:ss.SSS");
        formatter.setExcludeFields(Arrays.stream(SupplementalAttribute.values()).map(SupplementalAttribute::getId)
            .collect(Collectors.joining(",")));
        final String log = formatter.format(record);
        assertNotNull(log, "log");
        assertThat(log, matchesPattern("\\{"
            + "\\\"Timestamp\\\"\\:\\\"" + P_TIME + "\\\","
            + "\\\"Level\\\"\\:\\\"INFO\\\","
            + "\\\"LoggerName\\\"\\:\\\"\\\","
            + "\\\"LogMessage\\\"\\:\\\"This is a message.\\\""
            + "\\}\n"));
    }


    @Test
    public void mapParameters() {
        final Map<String, Object> map = new HashMap<>();
        map.put("some key", "some value");
        map.put(null, "value for null key");
        map.put("key3", "value not in the message");
        final LogRecord record = new LogRecord(Level.INFO,
            "This is a message with the simple parameter `{0}` and map parameter `{1}`.");
        record.setParameters(new Object[] {"x1", map});
        final JSONLogFormatter formatter = new JSONLogFormatter();
        formatter.setDateTimeFormatter("HH:mm:ss.SSS");
        formatter.setExcludeFields(Arrays.stream(SupplementalAttribute.values()).map(SupplementalAttribute::getId)
            .collect(Collectors.joining(",")));
        final String log = formatter.format(record);
        assertNotNull(log, "log");
        assertThat(log, matchesPattern("\\{"
            + "\\\"Timestamp\\\"\\:\\\"" + P_TIME + "\\\","
            + "\\\"Level\\\"\\:\\\"INFO\\\","
            + "\\\"[\\w ]+\\\"\\:\\\"[\\w ]+\\\","
            + "\\\"[\\w ]+\\\"\\:\\\"[\\w ]+\\\","
            + "\\\"[\\w ]+\\\"\\:\\\"[\\w ]+\\\","
            + "\\\"LogMessage\\\"\\:\\\"This is a message with the simple parameter `x1`"
            + " and map parameter `\\{null=value for null key, key3=value not in the message, some key=some value}`.\\\""
            + "\\}\n"));
        assertThat(log, containsString(",\"null\":\"value for null key\""));
        assertThat(log, containsString(",\"some key\":\"some value\""));
        assertThat(log, containsString(",\"key3\":\"value not in the message\""));
        assertThat(log, stringContainsInOrder("and map parameter `{", "key3=value not in the message"));
        assertThat(log, containsString("null=value for null key"));
        assertThat(log, containsString("some key=some value"));
        assertThat(log, containsString("key3=value not in the message"));
    }
}
