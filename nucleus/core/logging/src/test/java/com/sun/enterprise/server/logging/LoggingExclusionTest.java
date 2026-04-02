/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2021-2026 Payara Foundation and/or its affiliates. All rights reserved.
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
package com.sun.enterprise.server.logging;

import fish.payara.enterprise.server.logging.JSONLogFormatter;
import org.glassfish.api.VersionInfo;
import org.glassfish.api.logging.LogHelper;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.Globals;
import org.junit.After;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Testing the ExcludeField support.
 */
public class LoggingExclusionTest {

    private static final String TEST_EXCEPTION_MESSAGE = "Test exception message";
    private static final String TEST_MESSAGE_ID = "TEST_ID_001";

    private static final String TEST_CONF_FILE = "test.conf";

    private static final String FILE_SEP = System.getProperty("file.separator");

    private static final String USER_DIR = System.getProperty("user.dir");

    private static final String BASE_PATH = USER_DIR + FILE_SEP + "target";

    private static final String ODL_LOG = BASE_PATH + FILE_SEP + "odl.log";

    private static final String ULF_LOG = BASE_PATH + FILE_SEP + "ulf.log";

    private static final String JSON_LOG = BASE_PATH + FILE_SEP + "json.log";

    public static final String LOGGER_NAME = "javax.enterprise.test.logging.exclude";

    private static final Logger LOGGER = Logger.getLogger(LOGGER_NAME);

    private static final String LINE_SEP = System.getProperty("line.separator");

    private static final String CURRENT_YEAR = LocalDate.now().getYear() + "-";
    private static final String LEVEL = Level.SEVERE.getLocalizedName();
    private static final String LEVEL_VALUE = String.valueOf(Level.SEVERE.intValue());

    private static final String ABBREVIATED_PRODUCT = "Payara Test";
    private static final String VERSION_PREFIX = "prefix";
    private static final String VERSION_MAJOR = "5";
    private static final String VERSION_MINOR = "test";
    private static final String VERSION_UPDATE = "xx";
    private static final String VERSION_ID = ABBREVIATED_PRODUCT + " " + VERSION_PREFIX + VERSION_MAJOR + "." + VERSION_MINOR + "." + VERSION_UPDATE;
    private static final String TIME_MILLIS_1 = "TimeMillis";
    private static final String TIME_MILLIS_2 = "timeMillis";

    private static FileHandler uniformFormatHandler;
    private static FileHandler odlFormatHandler;
    private static FileHandler jsonFormatHandler;

    private static ConsoleHandler consoleHandler;

    private void initializeLoggingTest(String excludeFields) throws Exception {
        ServiceLocator serviceLocatorMock = Mockito.mock(ServiceLocator.class);

        Globals.setDefaultHabitat(serviceLocatorMock);

        VersionInfo versionInfo = new VersionInfo() {
            @Override
            public String getAbbreviatedProductName() {
                return ABBREVIATED_PRODUCT;
            }

            @Override
            public String getVersionPrefix() {
                return VERSION_PREFIX;
            }

            @Override
            public String getMajorVersion() {
                return VERSION_MAJOR;
            }

            @Override
            public String getMinorVersion() {
                return VERSION_MINOR;
            }

            @Override
            public String getUpdateVersion() {
                return VERSION_UPDATE;
            }
        };
        Mockito.when(serviceLocatorMock.getService(VersionInfo.class))
                .thenReturn(versionInfo);

        File basePath = new File(BASE_PATH);
        basePath.mkdirs();

        // Add a file handler with UniformLogFormatter
        uniformFormatHandler = new FileHandler(ULF_LOG);
        uniformFormatHandler.setLevel(Level.FINE);
        uniformFormatHandler.setFormatter(new UniformLogFormatter(excludeFields));

        // Add a file handler with ODLLogFormatter
        odlFormatHandler = new FileHandler(ODL_LOG);
        odlFormatHandler.setLevel(Level.FINE);
        odlFormatHandler.setFormatter(new ODLLogFormatter(excludeFields));

        // Add a file handler with JSONLogFormatter
        jsonFormatHandler = new FileHandler(JSON_LOG);
        jsonFormatHandler.setLevel(Level.FINE);
        jsonFormatHandler.setFormatter(new JSONLogFormatter(excludeFields));

        consoleHandler = new ConsoleHandler();
        consoleHandler.setFormatter(new UniformLogFormatter(excludeFields));

        LOGGER.addHandler(uniformFormatHandler);
        LOGGER.addHandler(odlFormatHandler);
        LOGGER.addHandler(jsonFormatHandler);

        boolean enableConsoleHandler = Boolean.getBoolean(LoggingExclusionTest.class.getName() + ".enableConsoleHandler");
        if (enableConsoleHandler) {
            LOGGER.addHandler(consoleHandler);
        }
        LOGGER.setUseParentHandlers(false);
        LOGGER.setLevel(Level.FINE);
    }

    @Test
    public void testLogNoExclusions() throws Exception {
        initializeLoggingTest("");

        LogHelper.log(LOGGER, Level.SEVERE, TEST_MESSAGE_ID,
                new Exception(TEST_EXCEPTION_MESSAGE), TEST_CONF_FILE);
        String[] expectedContentsUDF = new String[]{
                CURRENT_YEAR, LEVEL, LEVEL_VALUE, VERSION_ID, LOGGER_NAME, "_ThreadID=", "_ThreadName=main", TIME_MILLIS_1, TEST_EXCEPTION_MESSAGE
        };
        String[] expectedContentsODL = new String[]{
                CURRENT_YEAR, LEVEL, LEVEL_VALUE, VERSION_ID, LOGGER_NAME, "_ThreadID=", "_ThreadName=main", TIME_MILLIS_2, TEST_EXCEPTION_MESSAGE
        };
        String[] expectedContentsJSON = new String[]{
                CURRENT_YEAR, LEVEL, LEVEL_VALUE, VERSION_ID, LOGGER_NAME, "\"ThreadID\":\"", "\",\"ThreadName\":\"main\"", TIME_MILLIS_1, TEST_EXCEPTION_MESSAGE
        };

        validateLogContents(ULF_LOG, expectedContentsUDF, new String[]{});
        validateLogContents(ODL_LOG, expectedContentsODL, new String[]{});
        validateLogContents(JSON_LOG, expectedContentsJSON, new String[]{});

    }

    @Test
    public void testLogVersionExclusion() throws Exception {
        initializeLoggingTest("version");

        LogHelper.log(LOGGER, Level.SEVERE, TEST_MESSAGE_ID,
                new Exception(TEST_EXCEPTION_MESSAGE), TEST_CONF_FILE);
        String[] expectedContentsULF = new String[]{
                CURRENT_YEAR, LEVEL, LEVEL_VALUE, LOGGER_NAME, "_ThreadID=", "_ThreadName=main", TIME_MILLIS_1, TEST_EXCEPTION_MESSAGE
        };
        String[] expectedContentsODL = new String[]{
                CURRENT_YEAR, LEVEL, LEVEL_VALUE, LOGGER_NAME, "_ThreadID=", "_ThreadName=main", TIME_MILLIS_2, TEST_EXCEPTION_MESSAGE
        };
        String[] expectedContentsJSON = new String[]{
                CURRENT_YEAR, LEVEL, LEVEL_VALUE, LOGGER_NAME, "\"ThreadID\":\"" ,"\",\"ThreadName\":\"main\"", TIME_MILLIS_1, TEST_EXCEPTION_MESSAGE
        };

        String[] notExpectedContents = new String[]{
                VERSION_ID
        };

        validateLogContents(ULF_LOG, expectedContentsULF, notExpectedContents);
        validateLogContents(ODL_LOG, expectedContentsODL, notExpectedContents);
        validateLogContents(JSON_LOG, expectedContentsJSON, notExpectedContents);

    }

    @Test
    public void testLogThreadExclusion() throws Exception {
        initializeLoggingTest("tid");

        LogHelper.log(LOGGER, Level.SEVERE, TEST_MESSAGE_ID,
                new Exception(TEST_EXCEPTION_MESSAGE), TEST_CONF_FILE);
        String[] expectedContents = new String[]{
                CURRENT_YEAR, LEVEL, LEVEL_VALUE, LOGGER_NAME, VERSION_ID, TEST_EXCEPTION_MESSAGE
        };

        String[] notExpectedContents = new String[]{
                "_ThreadID=1", "_ThreadName=main"
        };

        String[] notExpectedContentsJSON = new String[]{
                "\"ThreadID\":\"1\",\"ThreadName\":\"main\""
        };

        validateLogContents(ULF_LOG, expectedContents, notExpectedContents);
        validateLogContents(ODL_LOG, expectedContents, notExpectedContents);
        validateLogContents(JSON_LOG, expectedContents, notExpectedContentsJSON);

    }

    @Test
    public void testLogLevelValueExclusion() throws Exception {
        initializeLoggingTest("levelValue");

        LogHelper.log(LOGGER, Level.SEVERE, TEST_MESSAGE_ID,
                new Exception(TEST_EXCEPTION_MESSAGE), TEST_CONF_FILE);

        String[] expectedContentsULF = new String[]{
                CURRENT_YEAR, LOGGER_NAME, LEVEL, "_ThreadID=", "_ThreadName=main", TIME_MILLIS_1, TEST_EXCEPTION_MESSAGE
        };
        String[] expectedContentsODL = new String[]{
                CURRENT_YEAR, LOGGER_NAME, LEVEL, "_ThreadID=", "_ThreadName=main", TIME_MILLIS_2, TEST_EXCEPTION_MESSAGE
        };
        String[] expectedContentsJSON = new String[]{
                CURRENT_YEAR, LOGGER_NAME, LEVEL, "\"ThreadID\":\"", "\",\"ThreadName\":\"main\"", TIME_MILLIS_1, TEST_EXCEPTION_MESSAGE
        };
        String[] notExpectedContents = new String[]{
                LEVEL_VALUE
        };


        validateLogContents(ULF_LOG, expectedContentsULF, notExpectedContents);
        validateLogContents(ODL_LOG, expectedContentsODL, notExpectedContents);
        validateLogContents(JSON_LOG, expectedContentsJSON, notExpectedContents);

    }

    @Test
    public void testLogMillisExclusion() throws Exception {
        initializeLoggingTest("timeMillis");

        LogHelper.log(LOGGER, Level.SEVERE, TEST_MESSAGE_ID,
                new Exception(TEST_EXCEPTION_MESSAGE), TEST_CONF_FILE);
        String[] expectedContents = new String[]{
                CURRENT_YEAR, LEVEL, LEVEL_VALUE, LOGGER_NAME, VERSION_ID, "_ThreadID=", "_ThreadName=main", TEST_EXCEPTION_MESSAGE
        };
        String[] expectedContentsJSON = new String[]{
                CURRENT_YEAR, LEVEL, LEVEL_VALUE, LOGGER_NAME, VERSION_ID, "\"ThreadID\":\"", "\",\"ThreadName\":\"main\"", TEST_EXCEPTION_MESSAGE
        };

        String[] notExpectedContents_UFLJSON = new String[]{
                TIME_MILLIS_1
        };
        String[] notExpectedContents_ODL = new String[]{
                TIME_MILLIS_2
        };

        validateLogContents(ULF_LOG, expectedContents, notExpectedContents_UFLJSON);
        validateLogContents(ODL_LOG, expectedContents, notExpectedContents_ODL);
        validateLogContents(JSON_LOG, expectedContentsJSON, notExpectedContents_UFLJSON);

    }


    private String validateLogContents(String file, String[] messages, String[] missingMessages) throws IOException {
        StringBuilder buf = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                buf.append(line);
                buf.append(LINE_SEP);
            }
            String contents = buf.toString();

            for (String msg : messages) {
                assertTrue("File " + file + " does not contain expected log message:" + msg, contents.contains(msg));
            }
            for (String msg : missingMessages) {
                assertFalse("File " + file + " does contain unexpected log message:" + msg, contents.contains(msg));
            }
            return contents;
        }
    }

    @After
    public void cleanupLoggingAnnotationsTest() {
        LOGGER.removeHandler(consoleHandler);
        LOGGER.removeHandler(uniformFormatHandler);
        LOGGER.removeHandler(odlFormatHandler);

        // Flush and Close the handlers
        consoleHandler.flush();
        uniformFormatHandler.flush();
        uniformFormatHandler.close();
        odlFormatHandler.flush();
        odlFormatHandler.close();
        jsonFormatHandler.flush();
        jsonFormatHandler.close();
    }

}
