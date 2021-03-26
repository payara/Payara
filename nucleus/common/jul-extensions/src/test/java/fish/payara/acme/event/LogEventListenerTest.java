/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright (c) 2012-2013 Oracle and/or its affiliates. All rights reserved.
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
// Portions Copyright [2018-2021] [Payara Foundation and/or its affiliates]

package fish.payara.acme.event;

import fish.payara.logging.jul.cfg.PayaraLogHandlerConfiguration;
import fish.payara.logging.jul.event.LogEvent;
import fish.payara.logging.jul.event.LogEventListener;
import fish.payara.logging.jul.formatter.UniformLogFormatter;
import fish.payara.logging.jul.handler.PayaraLogHandler;

import java.io.File;
import java.nio.file.Files;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


/**
 * @author sanshriv
 * @author Sven Diedrichsen - executor service, 2018
 * @author David Matejcek - update to JUnit5, 2021
 */
public class LogEventListenerTest {

    private static final String LOGGER_NAME = "javax.enterprise.test.logging.events";

    private static Logger logger;
    private static PayaraLogHandler handler;
    private static TestLogEventListener logEventListener;

    @BeforeAll
    public static void initializeLoggingAnnotationsTest() throws Exception {
        logger = Logger.getLogger(LOGGER_NAME);
        final File tempDir = Files.createTempDirectory("LogEventListenerTest_").toFile();
        final File logFile = new File(tempDir, "test-events.log");
        System.out.println("Using temporary file: " + logFile);

        // Add a file handler with UniformLogFormatter
        final PayaraLogHandlerConfiguration cfg = new PayaraLogHandlerConfiguration();
        final UniformLogFormatter formatter = new UniformLogFormatter();
        formatter.setLogEventBroadcaster(handler);

        cfg.setFormatterConfiguration(formatter);
        cfg.setLogFile(logFile);
        cfg.setLogToFile(true);
        handler = new PayaraLogHandler(cfg);

        logEventListener = new TestLogEventListener();
        handler.addLogEventListener(logEventListener);

        logger.addHandler(handler);
        logger.setLevel(Level.ALL);
        logger.setUseParentHandlers(false);
    }


    @Test
    public void testLogEventListenerNotifications() throws Exception {
        String msg = "Test message for testLogEventListenerNotifications";
        logger.info(msg);
        LogEvent event = logEventListener.logEvents.poll(1L, TimeUnit.SECONDS);
        assertNotNull(event, "Waiting on the event timed out");
        assertEquals(msg, event.getMessage());
    }

    @AfterAll
    public static void cleanupLoggingAnnotationsTest() throws Exception {
        logEventListener.logEvents.clear();
        logger.removeHandler(handler);
        handler.flush();
        handler.close();
    }

    private static class TestLogEventListener implements LogEventListener {

        private final BlockingQueue<LogEvent> logEvents = new ArrayBlockingQueue<>(100);

        @Override
        public void messageLogged(LogEvent event) {
            logEvents.add(event);
        }

    }
}
