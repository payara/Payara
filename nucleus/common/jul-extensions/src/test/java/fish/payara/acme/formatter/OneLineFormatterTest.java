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

import fish.payara.jul.formatter.OneLineFormatter;
import fish.payara.jul.record.EnhancedLogRecord;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

import static org.apache.commons.lang3.StringUtils.leftPad;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.collection.IsArrayWithSize.arrayWithSize;
import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author David Matejcek
 */
public class OneLineFormatterTest {

    private static final String P_TIME = "\\d\\d:\\d\\d:\\d\\d.\\d\\d\\d";
    private static final String P_LEVEL_NAME = "[A-Z]+";
    private static final String P_CLASS_NAME = "[A-Za-z0-9.]*";
    private static final String P_METHOD_NAME = "[a-z]?[a-zA-Z0-9.]*";

    private static final Pattern PATTERN_SINGLELINE = Pattern.compile(
        P_TIME + "[ ]+" + P_LEVEL_NAME + "[ ]+" + "main" + "[ ]+" + P_CLASS_NAME + "\\." + P_METHOD_NAME + " .+"
    );

    @Test
    public void nullRecord() {
        assertThrows(NullPointerException.class, () -> new OneLineFormatter().format(null));
    }


    @Test
    public void nullMessage() {
        final LogRecord record = new LogRecord(Level.INFO, null);
        final String log = new OneLineFormatter().format(record);
        assertEquals("", log);
    }


    @Test
    public void anonymousLoggerAndNullSource() {
        final String message = "Ok, this works!";
        final LogRecord record = new LogRecord(Level.INFO, message);
        record.setSourceClassName(null);
        record.setSourceMethodName(null);
        record.setLoggerName(null);
        final OneLineFormatter formatter = new OneLineFormatter();
        final String log = formatter.format(record);
        assertNotNull(log, "log");
        final String[] lines = log.split("\n");
        assertAll(
            () -> assertThat(lines, arrayWithSize(1)),
            () -> assertThat(lines[0], matchesPattern(PATTERN_SINGLELINE)),
            () -> assertThat(lines[0], endsWith(leftPad("INFO", 8) + leftPad("main", 21) + leftPad("", 61) + ". " + message))
        );
    }


    @Test
    public void fullLogRecordSingleLine() {
        final String message = "Ok, this works!";
        final LogRecord record = new LogRecord(Level.INFO, message);
        record.setLoggerName("the.test.logger");
        record.setSourceClassName("fish.payara.FakeClass");
        record.setSourceMethodName("fakeMethod");
        final OneLineFormatter formatter = new OneLineFormatter();
        final String log = formatter.format(record);
        assertNotNull(log, "log");
        final String[] lines = log.split("\n");
        assertAll(
            () -> assertThat(lines, arrayWithSize(1)),
            () -> assertThat(lines[0], matchesPattern(PATTERN_SINGLELINE)),
            () -> assertThat(lines[0], endsWith(leftPad("INFO", 8) + leftPad("main", 21)
                    + leftPad("fish.payara.FakeClass", 61) + ".fakeMethod " + message))
        );
    }


    @Test
    public void exception() {
        final EnhancedLogRecord record = new EnhancedLogRecord(Level.SEVERE, "Failure!", false);
        record.setThrown(new RuntimeException("Ooops!"));
        final OneLineFormatter formatter = new OneLineFormatter();
        final String log = formatter.format(record);
        assertNotNull(log, "log");
        final String[] lines = log.split("\n");
        assertAll(
            () -> assertThat(lines, arrayWithSize(greaterThan(20))),
            () -> assertThat(lines[0], endsWith(leftPad("SEVERE", 8) + leftPad("main", 21) + leftPad("", 61) + ". Failure!")),
            () -> assertThat(lines[1], equalTo("java.lang.RuntimeException: Ooops!"))
        );
    }
}
