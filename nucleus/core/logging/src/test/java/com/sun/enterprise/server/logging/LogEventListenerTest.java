/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
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
package com.sun.enterprise.server.logging;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test class to exercise the LogEvent notification mechanism.
 *
 */
public class LogEventListenerTest {

    private static final String FILE_SEP = System.getProperty("file.separator");
    
    private static final String USER_DIR = System.getProperty("user.dir");
    
    private static final String BASE_PATH = USER_DIR + FILE_SEP + "target";

    private static final String TEST_EVENTS_LOG =  BASE_PATH + FILE_SEP + "test-events.log";

    private static GFFileHandler gfFileHandler;

    private static final String LOGGER_NAME = "javax.enterprise.test.logging.events";

    private static final Logger LOGGER = Logger.getLogger(LOGGER_NAME);
    
    @BeforeClass
    public static void initializeLoggingAnnotationsTest() throws Exception {
        File basePath = new File(BASE_PATH);
        basePath.mkdirs();
        File testLog = new File(TEST_EVENTS_LOG);
        
        // Add a file handler with UniformLogFormatter
        gfFileHandler = new GFFileHandler();
        gfFileHandler.changeFileName(testLog);
        UniformLogFormatter formatter = new UniformLogFormatter();
        formatter.setLogEventBroadcaster(gfFileHandler);
        gfFileHandler.setFormatter(formatter );
        gfFileHandler.initializePump();

        logEventListener = new TestLogEventListener();
        gfFileHandler.addLogEventListener(logEventListener);

        LOGGER.addHandler(gfFileHandler);
        LOGGER.setLevel(Level.ALL);
        LOGGER.setUseParentHandlers(false);
    }

    private static TestLogEventListener logEventListener;
    
    @Test
    public void testLogEventListenerNotifications() throws Exception {
        String msg = "Test message for testLogEventListenerNotifications";
        LOGGER.info(msg);
        LogEvent event = logEventListener.logEvents.take();
        assertEquals(msg, event.getMessage());
        System.out.println("Test testLogEventListenerNotifications passed.");
    }
    
    @AfterClass
    public static void cleanupLoggingAnnotationsTest() throws Exception {
        logEventListener.logEvents.clear();
        LOGGER.removeHandler(gfFileHandler);        
        // Flush and Close the handler
        gfFileHandler.flush();
        gfFileHandler.close();
        gfFileHandler.preDestroy();
    }
    
    private static class TestLogEventListener implements LogEventListener {
        
        private BlockingQueue<LogEvent> logEvents = new ArrayBlockingQueue<LogEvent>(100);

        @Override
        public void messageLogged(LogEvent event) {
            logEvents.add(event);
        }
        
    }
    
}
