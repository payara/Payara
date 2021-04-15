/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright (c) 2020 Payara Foundation and/or its affiliates. All rights reserved.
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

import fish.payara.jul.PayaraLogManager;
import fish.payara.jul.PayaraLogger;
import fish.payara.jul.PayaraLoggingStatus;
import fish.payara.jul.PayaraLogManager.Action;
import fish.payara.jul.cfg.PayaraLogManagerConfiguration;
import fish.payara.jul.cfg.SortedProperties;
import fish.payara.jul.env.LoggingSystemEnvironment;
import fish.payara.jul.formatter.OneLineFormatter;
import fish.payara.jul.handler.ExternallyManagedLogHandler;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static fish.payara.jul.cfg.PayaraLoggingJvmOptions.CLASS_LOG_MANAGER_PAYARA;
import static fish.payara.jul.cfg.PayaraLoggingJvmOptions.JVM_OPT_LOGGING_MANAGER;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author David Matejcek
 */
@EnabledIfSystemProperty(named = JVM_OPT_LOGGING_MANAGER, matches = CLASS_LOG_MANAGER_PAYARA)
public class PayaraLogManagerTest {

    private static PayaraLogManager logManager;
    private static PayaraLogManagerConfiguration originalCfg;


    @BeforeAll
    public static void backup() {
        logManager = PayaraLogManager.getLogManager();
        originalCfg = logManager.getConfiguration();
        System.out.println("Original configuration: " + originalCfg);
    }


    @AfterEach
    public void reset() {
        logManager.reconfigure(originalCfg);
    }


    @Test
    public void getRootLogger() {
        final Logger rootLogger = logManager.getLogger(PayaraLogManager.ROOT_LOGGER_NAME);
        assertNotNull(rootLogger, "root logger");
        assertAll(
            () -> assertThat("root logger", rootLogger.getClass().getName(), equalTo(PayaraLogger.class.getName())),
            () -> assertThat("logManager.getRootLogger()", logManager.getRootLogger(), sameInstance(rootLogger))
        );
    }


    @Test
    public void getGlobalLogger() {
        final Logger globalLogger = logManager.getLogger(Logger.GLOBAL_LOGGER_NAME);
        assertAll(
            () -> assertNotNull(globalLogger, "global logger"),
            () -> assertNotSame(globalLogger, Logger.getGlobal(), "global logger")
        );
        assertAll(
            () -> assertThat("global logger via PayaraLogManager", globalLogger.getClass().getName(),
                equalTo("fish.payara.jul.PayaraLoggerWrapper")),
            () -> assertThat("global logger name", Logger.getGlobal().getName(), equalTo(globalLogger.getName()))
        );
    }


    @Test
    public void addLoggers() {
        final String loggerName = "com.acme.MyCustomLogger";
        final CustomLogger customLogger = new CustomLogger(loggerName);
        final boolean result = logManager.addLogger(customLogger);
        assertAll(
            () -> assertTrue(result, "addLogger result"),
            () -> assertSame(customLogger, logManager.getLogger(loggerName), "LogManager.getLogger"),
            () -> assertSame(logManager.getLogger(loggerName), Logger.getLogger(loggerName), "getLogger results"),
            () -> assertFalse(logManager.addLogger(new CustomLogger(Logger.GLOBAL_LOGGER_NAME)), "add as global"),
            () -> assertFalse(logManager.addLogger(new PayaraLogger(logManager.getRootLogger())), "add as root"),
            () -> assertFalse(logManager.addLogger(customLogger), "added for the second time"),
            () -> assertThrows(NullPointerException.class,
                () -> logManager.addLogger(Logger.getAnonymousLogger()), "add anonymous")
        );
    }


    @Test
    public void externalHandlers() {
        assertEquals(PayaraLoggingStatus.FULL_SERVICE, logManager.getLoggingStatus());
        final AtomicBoolean reconfigActionCalled = new AtomicBoolean();
        final AtomicBoolean flushActionCalled = new AtomicBoolean();
        final SortedProperties properties = new SortedProperties();
        properties.setProperty(PayaraLogManagerConfiguration.KEY_TRACING_ENABLED, "true");
        final PayaraLogManagerConfiguration cfg1 = new PayaraLogManagerConfiguration(properties);
        logManager.reconfigure(cfg1, () -> reconfigActionCalled.set(true), () -> flushActionCalled.set(true));
        assertAll(
            () -> assertTrue(reconfigActionCalled.get(), "reconfig action was executed"),
            () -> assertTrue(flushActionCalled.get(), "flush action was executed"),
            () -> assertEquals(PayaraLoggingStatus.FULL_SERVICE, logManager.getLoggingStatus()),
            () -> assertFalse(LoggingSystemEnvironment.getOriginalStdOut().checkError(),
                "something probably closed STDOUT"),
            () -> assertFalse(LoggingSystemEnvironment.getOriginalStdErr().checkError(),
                "something probably closed STDERR")
        );

        // we will reuse both actions
        reconfigActionCalled.set(false);
        flushActionCalled.set(false);
        final String handlerName = PayaraLogManagerTest.class.getName() + "$TestHandler";
        properties.setProperty("handlers", handlerName);
        final PayaraLogManagerConfiguration cfg2 = new PayaraLogManagerConfiguration(properties);
        logManager.reconfigure(cfg2, () -> reconfigActionCalled.set(true), () -> flushActionCalled.set(true));
        assertAll(
            () -> assertTrue(reconfigActionCalled.get(), "reconfig action was executed"),
            () -> assertFalse(flushActionCalled.get(), "flush action was executed"),
            () -> assertNotNull(logManager.getRootLogger().getHandler(TestHandler.class), "test handler"),
            () -> assertFalse(logManager.getRootLogger().getHandler(TestHandler.class).isReady(), "test handler ready"),
            () -> assertEquals(PayaraLoggingStatus.CONFIGURING, logManager.getLoggingStatus())
        );

        Logger.getAnonymousLogger().info("Tick tock!");
        // why: CONFIGURING already sends records to handlers and it is on the handler how it will manage it.
        assertTrue(logManager.getRootLogger().getHandler(TestHandler.class).published, "publish called");
        final Action reconfigAction3 = () -> logManager.getRootLogger().getHandler(TestHandler.class).ready = true;
        logManager.reconfigure(cfg2, reconfigAction3, () -> flushActionCalled.set(true));
        assertAll(
            () -> assertTrue(flushActionCalled.get(), "flush action was executed"),
            () -> assertEquals(PayaraLoggingStatus.FULL_SERVICE, logManager.getLoggingStatus()),
            () -> assertTrue(logManager.getRootLogger().getHandler(TestHandler.class).published,
                "publish called - maybe another instance!")
        );

        logManager.closeAllExternallyManagedLogHandlers();
        assertNull(logManager.getRootLogger().getHandler(TestHandler.class));
    }


    @Test
    public void reconfigure() {
        final PayaraLogManagerConfiguration configuration0 = logManager.getConfiguration();
        logManager.reconfigure(configuration0);
        final PayaraLogManagerConfiguration configuration1 = logManager.getConfiguration();
        assertAll(
            () -> assertNotSame(configuration0.getPropertyNames(), configuration1.getPropertyNames()),
            () -> assertNotSame(configuration0, configuration1),
            () -> assertThat(configuration1.getPropertyNames(), contains(configuration0.getPropertyNames().toArray())),
            () -> assertThat(configuration1.getPropertyNames(), hasSize(configuration0.getPropertyNames().size()))
        );
        for (String name : configuration0.getPropertyNames()) {
            assertEquals(configuration0.getProperty(name), configuration1.getProperty(name));
        }
    }


    private static class CustomLogger extends PayaraLogger {

        protected CustomLogger(String name) {
            super(name);
        }
    }


    public static class TestHandler extends Handler implements ExternallyManagedLogHandler {
        private boolean ready;
        private boolean published;

        public TestHandler() {
            setFormatter(new OneLineFormatter());
        }

        @Override
        public boolean isReady() {
            return ready;
        }


        @Override
        public void close() {
            ready = false;
        }


        @Override
        public void publish(final LogRecord record) {
            LoggingSystemEnvironment.getOriginalStdOut().println(getFormatter().format(record));
            published = true;
        }


        @Override
        public void flush() {
            // nothing
        }
    }
}
