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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.glassfish.api.logging.LogHelper;
import org.glassfish.logging.annotation.LogMessageInfo;
import org.glassfish.logging.annotation.LogMessagesResourceBundle;
import org.glassfish.logging.annotation.LoggerInfo;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author sanshriv
 *
 */
public class LoggingAnnotationsTest {

    private static final String CANNOT_READ_TEST_CONFIGURATION_FILE_MSG = "Cannot read test configuration file ";

    private static final String TEST_EXCEPTION_MESSAGE = "Test exception message";

    private static final String TEST_CONF_FILE = "test.conf";

    private static final String FILE_SEP = System.getProperty("file.separator");
    
    private static final String USER_DIR = System.getProperty("user.dir");
    
    private static final String BASE_PATH = USER_DIR + FILE_SEP + "target";
    
    private static final String ODL_LOG =  BASE_PATH + FILE_SEP + "odl.log";

    private static final String ULF_LOG = BASE_PATH + FILE_SEP + "ulf.log";

    @LoggerInfo(subsystem = "Logging", description="Main logger for testing logging annotations.")
    public static final String LOGGER_NAME = "javax.enterprise.test.logging.annotations";
    
    @LogMessagesResourceBundle()
    public static final String RB_NAME = "com.sun.enterprise.server.logging.test.LogMessages";

    @LogMessageInfo(message = "Cannot read test configuration file {0}", level="SEVERE",
            cause="An exception has occurred while reading the logging configuration file.",
            action="Take appropriate action based on the exception message.")
    public static final String ERROR_READING_TEST_CONF_FILE_ID = "TEST-LOGGING-00001";
    
    private static final Logger LOGGER = Logger.getLogger(LOGGER_NAME, RB_NAME);

    private static final String LINE_SEP = System.getProperty("line.separator");

    @LogMessageInfo(message = "FINE Level test message", level="FINE")
    private static final String FINE_TEST_MESSAGE_ID = "TEST-LOGGING-00002";

    private static FileHandler uniformFormatHandler;

    private static FileHandler odlFormatHandler;
    
    private static ConsoleHandler consoleHandler;
    
    @BeforeClass
    public static void initializeLoggingAnnotationsTest() throws Exception {
        File basePath = new File(BASE_PATH);
        basePath.mkdirs();
        
        // Add a file handler with UniformLogFormatter
        uniformFormatHandler = new FileHandler(ULF_LOG);
        uniformFormatHandler.setLevel(Level.FINE);
        uniformFormatHandler.setFormatter(new UniformLogFormatter());

        // Add a file handler with ODLLogFormatter
        odlFormatHandler = new FileHandler(ODL_LOG);
        odlFormatHandler.setLevel(Level.FINE);
        odlFormatHandler.setFormatter(new ODLLogFormatter());
        
        consoleHandler = new ConsoleHandler();
        consoleHandler.setFormatter(new UniformLogFormatter());

        LOGGER.addHandler(uniformFormatHandler);
        LOGGER.addHandler(odlFormatHandler);
        Boolean enableConsoleHandler = Boolean.getBoolean(LoggingAnnotationsTest.class.getName() + ".enableConsoleHandler");
        if (enableConsoleHandler) {
            LOGGER.addHandler(consoleHandler);        
        }
        LOGGER.setUseParentHandlers(false);
        LOGGER.setLevel(Level.FINE);
    }
    
    @Test
    public void testLogMessageWithExceptionArgument() throws IOException {
        LogHelper.log(LOGGER, Level.SEVERE, ERROR_READING_TEST_CONF_FILE_ID, 
                new Exception(TEST_EXCEPTION_MESSAGE), TEST_CONF_FILE);
        String[] expectedContents = new String[] {
                CANNOT_READ_TEST_CONFIGURATION_FILE_MSG + TEST_CONF_FILE,
                TEST_EXCEPTION_MESSAGE
        };
        validateLogContents(ULF_LOG, expectedContents);
        validateLogContents(ODL_LOG, expectedContents);
        System.out.println("Test passed successfully.");
    }

    @Test
    public void testFineLevelMessageWithSourceInfo() throws IOException {
        LOGGER.fine(FINE_TEST_MESSAGE_ID);
        String testMessage = "FINE Level test message";
        String[] ulfContents = new String[] {testMessage, 
                "ClassName=com.sun.enterprise.server.logging.LoggingAnnotationsTest;",
                "MethodName=testFineLevelMessageWithSourceInfo;"};
        validateLogContents(ULF_LOG, ulfContents);
        String[] odlContents = new String[] {testMessage,
                "[CLASSNAME: com.sun.enterprise.server.logging.LoggingAnnotationsTest]",
                "[METHODNAME: testFineLevelMessageWithSourceInfo]"};
        validateLogContents(ODL_LOG, odlContents);
        System.out.println("Test passed successfully.");
    }

    @Test
    public void testFineLevelMessageWithoutSourceInfo() throws IOException {
        String msg = "Hello FINE World";
        LogRecord rec = new LogRecord(Level.FINE, msg);
        rec.setLoggerName(LOGGER_NAME);
        rec.setSourceClassName(null);
        rec.setSourceMethodName(null);
        LOGGER.log(rec);
        String[] ulfContents = new String[] {msg};
        String contents = validateLogContents(ULF_LOG, ulfContents);
        assertEquals(false,contents.contains("MethodName=testFineLevelMessageWithoutSourceInfo;"));
        String[] odlContents = new String[] {msg};
        contents = validateLogContents(ODL_LOG, odlContents);
        assertEquals(false,contents.contains("[METHODNAME: testFineLevelMessageWithoutSourceInfo]"));
        System.out.println("Test passed successfully.");
    }

    @Test
    public void testGetFormattedMessage() throws IOException {
        String formattedMsg = LogHelper.getFormattedMessage(
                LOGGER, ERROR_READING_TEST_CONF_FILE_ID, TEST_CONF_FILE);
        assertEquals(CANNOT_READ_TEST_CONFIGURATION_FILE_MSG + TEST_CONF_FILE,
                formattedMsg);
        System.out.println("Test passed successfully.");
    }

    private String validateLogContents(String file, String[] messages) throws IOException {
        StringBuffer buf = new StringBuffer();
        BufferedReader reader=null;
        try {
            reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line=reader.readLine()) != null) {
                buf.append(line);
                buf.append(LINE_SEP);
            }
            String contents = buf.toString();
            for (String msg : messages) {
                assertEquals("File " + file + " does not contain expected log message:" + msg, 
                        true, contents.contains(msg));                
            }
            return contents;
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }

    @AfterClass
    public static void cleanupLoggingAnnotationsTest() throws Exception {
        LOGGER.removeHandler(consoleHandler);
        LOGGER.removeHandler(uniformFormatHandler);
        LOGGER.removeHandler(odlFormatHandler);
        
        // Flush and Close the handlers
        consoleHandler.flush();
        uniformFormatHandler.flush();
        uniformFormatHandler.close();
        odlFormatHandler.flush();
        odlFormatHandler.close();
    }
    
}
