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

package fish.payara.acme.handler;

import fish.payara.jul.env.LoggingSystemEnvironment;
import fish.payara.jul.handler.SimpleLogHandler;
import fish.payara.jul.record.EnhancedLogRecord;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.collection.IsArrayWithSize.arrayWithSize;
import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;


/**
 * @author David Matejcek
 */
public class SimpleLogHandlerTest {

    private static final String P_TIME = "\\d\\d:\\d\\d:\\d\\d.\\d\\d\\d";

    private ByteArrayOutputStream os;
    private PrintStream logCollector;

    @BeforeEach
    public void setOutput() throws Exception {
        os = new ByteArrayOutputStream();
        logCollector = new PrintStream(os, true, StandardCharsets.UTF_8.name());
    }

    @AfterEach
    public void resetOutput() throws Exception {
        assertAll(
            () -> assertFalse(LoggingSystemEnvironment.getOriginalStdErr().checkError(), "stderr closed"),
            () -> assertFalse(LoggingSystemEnvironment.getOriginalStdOut().checkError(), "stdout closed")
        );
        logCollector.close();
    }


    @Test
    void standardMessage() throws Exception {
        final SimpleLogHandler handler = new SimpleLogHandler(logCollector);
        final EnhancedLogRecord record = new EnhancedLogRecord(Level.INFO, "This should log.");
        record.setSourceClassName("FakeClass");
        record.setSourceMethodName("fakeMethod");
        handler.publish(record);
        final String log = os.toString(StandardCharsets.UTF_8.name());
        assertNotNull(log, "log");
        final String[] lines = log.split("\n");
        assertAll(
            () -> assertThat(lines, arrayWithSize(1)),
            () -> assertThat(lines[0],
                matchesPattern(P_TIME + "\\s{4}INFO\\s{17}main\\s{52}FakeClass\\.fakeMethod This should log\\."))
        );
    }


    @Test
    void exception() throws Exception {
        final SimpleLogHandler handler = new SimpleLogHandler(logCollector);
        final EnhancedLogRecord record = new EnhancedLogRecord(Level.SEVERE, "This should log.");
        record.setSourceClassName("FakeClass");
        record.setSourceMethodName("fakeMethod");
        record.setThrown(new IllegalStateException("Something broke."));
        handler.publish(record);
        final String log = os.toString(StandardCharsets.UTF_8.name());
        assertNotNull(log, "log");
        final String[] lines = log.split("\n");
        assertAll(
            () -> assertThat(lines, arrayWithSize(greaterThan(20))),
            () -> assertThat(lines[0],
                matchesPattern(P_TIME + "\\s{2}SEVERE\\s{17}main\\s{52}FakeClass\\.fakeMethod This should log\\.")),
            () -> assertThat(lines[1], equalTo("java.lang.IllegalStateException: Something broke."))
        );
    }

}
