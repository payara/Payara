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

package fish.payara.acme;

import fish.payara.acme.handler.LogCollectorHandlerTest;
import fish.payara.logging.jul.PayaraLogger;
import fish.payara.logging.jul.handler.LogCollectorHandler;
import fish.payara.logging.jul.record.EnhancedLogRecord;
import fish.payara.logging.jul.tracing.PayaraLoggingTracer;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.hamcrest.collection.IsEmptyCollection;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author David Matejcek
 */
public class PayaraLoggerTest {

    private static PayaraLogger logger;
    private static LogCollectorHandler handler;


    @BeforeAll
    public static void initEnv() throws Exception {
        PayaraLoggingTracer.setTracingEnabled(true);
        LogManager.getLogManager().reset();
        final Logger originalLogger = Logger.getLogger(LogCollectorHandlerTest.class.getName());
        originalLogger.setResourceBundle(new TestResourceBundle());
        handler = new LogCollectorHandler(originalLogger);
        logger = new PayaraLogger(originalLogger);
        logger.setLevel(Level.FINEST);
    }

    @BeforeEach
    public void reinit() {
        logger.setLevel(Level.FINEST);
        handler.setLevel(Level.ALL);
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
    public void filteredLevelAndSupplier() {
        logger.setLevel(Level.INFO);
        logger.finest(() -> "supplied text");
        assertAll(
            () -> assertNull(handler.pop(), "more records"),
            () -> assertThat(handler.getAll(), IsEmptyCollection.empty())
        );
    }


    @Test
    public void messageWithParameter() {
        logger.log(Level.FINE, "bundle.with.parameter", 42L);
        final EnhancedLogRecord record = handler.pop();
        assertAll(
            () -> assertEquals("bundle.with.parameter", record.getMessageKey(), "messageKey"),
            () -> assertEquals("resourceBundleValue with this parameter: 42", record.getMessage(), "message"),
            () -> assertThat(handler.getAll(), IsEmptyCollection.empty())
        );
    }

    @Test
    public void messageWithSupplier() {
        logger.log(Level.SEVERE, () -> "It is not broken!");
        final EnhancedLogRecord record = handler.pop();
        assertAll(
            () -> assertNull(record.getMessageKey(), "messageKey"),
            () -> assertEquals("It is not broken!", record.getMessage(), "message"),
            () -> assertThat(handler.getAll(), IsEmptyCollection.empty())
        );
    }

    @Test
    public void messageWithSupplierAndException() {
        logger.log(Level.SEVERE, new RuntimeException("Kaboom!"), () -> "It is not broken!");
        final EnhancedLogRecord record = handler.pop();
        assertAll(
            () -> assertNull(record.getMessageKey(), "messageKey"),
            () -> assertEquals("It is not broken!", record.getMessage(), "message"),
            () -> assertThat(handler.getAll(), IsEmptyCollection.empty())
        );
    }

    @Test
    public void messageWithException() {
        logger.log(Level.SEVERE, "It is not broken!", new RuntimeException("Kaboom!"));
        final EnhancedLogRecord record = handler.pop();
        assertAll(
            () -> assertNull(record.getMessageKey(), "messageKey"),
            () -> assertEquals("It is not broken!", record.getMessage(), "message"),
            () -> assertThat(handler.getAll(), IsEmptyCollection.empty())
        );
    }

    private static final class TestResourceBundle extends ResourceBundle {
        private final Map<String, String> bundle = new HashMap<>();

        public TestResourceBundle() {
            bundle.put("bundle.with.parameter", "resourceBundleValue with this parameter: {0}");
            bundle.put("bundle.without.parameter", "resourceBundleValue without parameter");
        }

        @Override
        public String getBaseBundleName() {
            return getClass().getSimpleName();
        }

        @Override
        protected Object handleGetObject(String key) {
            return bundle.get(key);
        }


        @Override
        public Enumeration<String> getKeys() {
            return Collections.enumeration(bundle.keySet());
        }
    }
}
