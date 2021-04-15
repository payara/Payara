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

import fish.payara.jul.handler.LogCollectorHandler;
import fish.payara.jul.record.EnhancedLogRecord;
import fish.payara.jul.tracing.PayaraLoggingTracer;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author David Matejcek
 */
public class LogCollectorHandlerTest {

    private static Logger logger;
    private static Logger subLogger;
    private static LogCollectorHandler handler;


    @BeforeAll
    public static void initEnv() throws Exception {
        PayaraLoggingTracer.setTracingEnabled(true);
        LogManager.getLogManager().reset();
        logger = Logger.getLogger(LogCollectorHandlerTest.class.getName());
        subLogger = Logger.getLogger(LogCollectorHandlerTest.class.getName() + ".sublog");
        handler = new LogCollectorHandler(logger);
    }


    @BeforeEach
    public void reinit() {
        logger.setLevel(Level.FINEST);
        handler.setLevel(Level.ALL);
        handler.reset();
    }


    @AfterAll
    public static void resetEverything() {
        if (handler != null) {
            handler.close();
        }
        assertThat("Nothing should remain after close", handler.getAll(), hasSize(0));
        LogManager.getLogManager().reset();
        PayaraLoggingTracer.setTracingEnabled(false);
    }


    @Test
    public void mainLogger() {
        logger.entering(LogCollectorHandlerTest.class.getCanonicalName(), "mainLogger");
        final EnhancedLogRecord record = handler.pop();
        assertAll(
            () -> assertNotNull(record, "record"),
            () -> assertNull(handler.pop(), "more records")
        );
        assertAll(
            () -> assertEquals(Level.FINER, record.getLevel())
        );
    }


    @Test
    public void subLogger() {
        subLogger.exiting(LogCollectorHandlerTest.class.getCanonicalName(), "subLogger");
        final List<EnhancedLogRecord> records = handler.getAll();
        assertAll(
            () -> assertThat(records, hasSize(1)),
            () -> assertNull(handler.pop(), "more records")
        );
        final EnhancedLogRecord record = records.get(0);
        assertAll(
            () -> assertEquals(Level.FINER, record.getLevel())
        );
    }


    @Test
    public void handlerLevel() {
        handler.setLevel(Level.INFO);
        logger.log(Level.CONFIG, "Nothing important");
        assertNull(handler.pop(), "more records");
        logger.log(Level.SEVERE, "Important message: {0}", 42);
        assertNotNull(handler.pop(), "more records");
        logger.log(Level.SEVERE, "Some garbage for close() method");
    }
}
