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

import fish.payara.jul.PayaraLogManager;
import fish.payara.jul.PayaraLogManager.Action;
import fish.payara.jul.PayaraLoggingStatus;
import fish.payara.jul.cfg.PayaraLogManagerConfiguration;
import fish.payara.jul.cfg.SortedProperties;
import fish.payara.jul.handler.BlockingExternallyManagedLogHandler;
import fish.payara.jul.handler.LogCollectorHandler;
import fish.payara.jul.record.EnhancedLogRecord;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;

import static fish.payara.jul.cfg.PayaraLogManagerProperty.KEY_ROOT_HANDLERS;
import static fish.payara.jul.env.LoggingSystemEnvironment.isResolveLevelWithIncompleteConfiguration;
import static fish.payara.jul.env.LoggingSystemEnvironment.setResolveLevelWithIncompleteConfiguration;
import static java.util.concurrent.CompletableFuture.runAsync;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Timeout;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This test is executed as a sequence going through some lifecycle usage.
 *
 * @author David Matejcek
 */
@TestMethodOrder(OrderAnnotation.class)
public class PayaraLogManagerLifeCycleTest {

    private static final PayaraLogManager MANAGER = PayaraLogManager.getLogManager();
    private static final Logger LOG = Logger.getLogger(PayaraLogManagerLifeCycleTest.class.getName());

    private static final LogCollectorHandler COLLECTOR = new LogCollectorHandler(LOG);
    private static final BlockingAction ACTION_RECONFIG = new BlockingAction();
    private static final BlockingAction ACTION_FLUSH = new BlockingAction();

    private static PayaraLogManagerConfiguration originalCfg;
    private static boolean originalLevelResolution;
    private static CompletableFuture<Void> process;

    @BeforeAll
    public static void backupConfiguration() {
        assertNotNull(MANAGER, () -> "Unsupported log manager used: " + LogManager.getLogManager());
        originalCfg = MANAGER.getConfiguration();
        originalLevelResolution = isResolveLevelWithIncompleteConfiguration();

        COLLECTOR.setLevel(Level.ALL);
        LOG.setUseParentHandlers(false);
    }


    @AfterAll
    public static void resetConfiguration() throws Exception {
        ACTION_FLUSH.unblock();
        ACTION_RECONFIG.unblock();
        MANAGER.reconfigure(originalCfg, null, null);
        setResolveLevelWithIncompleteConfiguration(originalLevelResolution);
        COLLECTOR.close();
    }


    /**
     * This uses Payara Server - because it starts without a configuration, uses
     * {@link BlockingExternallyManagedLogHandler} to block logging system in
     * {@link PayaraLoggingStatus#CONFIGURING} state, so it is just collecting
     * log records to the StartupQueue.
     * <p>
     * Payara Micro doesn't need it.
     */
    @Test
    @Order(1)
    public void startReconfigurationAndBlock() {
        reconfigureToBlockingHandler();
        assertEquals(PayaraLoggingStatus.CONFIGURING, MANAGER.getLoggingStatus());
        LOG.log(Level.INFO, "Hello StartupQueue, my old friend");
    }


    /**
     * Then Payara Server finds domain directory and logging properties and initiates
     * reconfiguration.
     * <p>
     * Here we simulate additional actions by two fake blocking implementations, controlled
     * by the test.
     * <p>
     * The logging should stay in {@link PayaraLoggingStatus#CONFIGURING}.
     */
    @Test
    @Order(2)
    @Timeout(5)
    public void startReconfiguration() throws Exception {
        setResolveLevelWithIncompleteConfiguration(false);
        assertEquals(PayaraLoggingStatus.CONFIGURING, MANAGER.getLoggingStatus(),
            "status after startReconfigurationAndBlock test");

        doLog(Level.FINE, "Log before reconfiguration");
        doLog(Level.FINEST, "This log should be dropped, logger's level is FINE");
        process = runAsync(() -> MANAGER.reconfigure(originalCfg, ACTION_RECONFIG, ACTION_FLUSH));
        Thread.sleep(10L);
        assertAll(
            () -> assertEquals(PayaraLoggingStatus.CONFIGURING, MANAGER.getLoggingStatus()),
            () -> assertNull(COLLECTOR.pop(), "COLLECTOR should be empty after reconfiguration started"));
        doLog(Level.INFO, "Log after reconfiguration started");
        assertNull(COLLECTOR.pop(), "COLLECTOR should be empty after reconfiguration started even if we log again");
    }


    /**
     * Another step. We finish the custom reconfiguration action.
     * <p>
     * The logging should step into {@link PayaraLoggingStatus#FLUSHING_BUFFERS}.
     */
    @Test
    @Order(3)
    @Timeout(5)
    public void finishReconfigurationAndStartFlushing() throws Exception {
        ACTION_RECONFIG.unblock();
        assertAll(
            () -> assertEquals(PayaraLoggingStatus.FLUSHING_BUFFERS, MANAGER.getLoggingStatus(),
                "status after reconfiguration finished"),
            () -> assertNull(COLLECTOR.pop(), "COLLECTOR should be empty after reconfiguration finished")
        );

        doLog(Level.SEVERE, "Log while flushing is still executed");
        assertNull(COLLECTOR.pop(), "COLLECTOR should be empty after reconfiguration finished even if we log again");
    }


    /**
     * Last step, we flush waiting logs from startup queue.
     * <p>
     * The logging shall step into {@link PayaraLoggingStatus#FULL_SERVICE} and all records shall
     * end in our collector handler.
     */
    @Test
    @Order(4)
    @Timeout(10)
    public void finishFlushing() throws Exception {
        ACTION_FLUSH.unblock();
        doLog(Level.INFO, "Log after flushing finished");
        final List<EnhancedLogRecord> logRecords = COLLECTOR.getAll();
        assertAll(
            () -> assertEquals(PayaraLoggingStatus.FULL_SERVICE, MANAGER.getLoggingStatus(),
                "status after all reconfiguration and flushing finished"),
            () -> assertThat(logRecords, hasSize(5)),
            () -> assertThat("record 0", logRecords.get(0).getLevel(), equalTo(Level.INFO)),
            () -> assertThat("record 0", logRecords.get(0).getMessage(), equalTo("Hello StartupQueue, my old friend")),
            () -> assertThat("record 1", logRecords.get(1).getLevel(), equalTo(Level.FINE)),
            () -> assertThat("record 1", logRecords.get(1).getMessage(), equalTo("Log before reconfiguration")),
            () -> assertThat("record 2", logRecords.get(2).getLevel(), equalTo(Level.INFO)),
            () -> assertThat("record 2", logRecords.get(2).getMessage(), equalTo("Log after reconfiguration started")),
            () -> assertThat("record 3", logRecords.get(3).getLevel(), equalTo(Level.SEVERE)),
            () -> assertThat("record 3", logRecords.get(3).getMessage(), equalTo("Log while flushing is still executed")),
            () -> assertThat("record 4", logRecords.get(4).getLevel(), equalTo(Level.INFO)),
            () -> assertThat("record 4", logRecords.get(4).getMessage(), equalTo("Log after flushing finished"))
        );

        process.get(5, TimeUnit.SECONDS);
    }


    /**
     * Yet another case, here we get into blocked state, and then we DISABLE resolving if the record
     * is loggable or not when we simply don't know. All records then go to the StartupQueue, which
     * means also some memory requirements.
     */
    @Test
    @Order(100)
    @Timeout(5)
    public void reconfigureWithoutResolvingLevelsWithincompleteconfiguration() throws Exception {
        reconfigureToBlockingHandler();
        setResolveLevelWithIncompleteConfiguration(false);
        assertEquals(PayaraLoggingStatus.CONFIGURING, MANAGER.getLoggingStatus(), "after reconfigureToBlockingHandler");

        doLog(Level.FINEST, "message0");
        doLog(Level.INFO, "message1");
        MANAGER.reconfigure(originalCfg, () -> LOG.setLevel(Level.FINEST), null);
        assertFalse(isResolveLevelWithIncompleteConfiguration(), "isResolveLevelWithIncompleteConfiguration");

        final List<EnhancedLogRecord> logRecords = COLLECTOR.getAll();
        assertAll("Both records must pass via startup queue",
            () -> assertEquals(PayaraLoggingStatus.FULL_SERVICE, MANAGER.getLoggingStatus(),
                "status after all reconfiguration and flushing finished"),
            () -> assertThat(logRecords, hasSize(2)),
            () -> assertThat("record 0", logRecords.get(0).getLevel(), equalTo(Level.FINEST)),
            () -> assertThat("record 0", logRecords.get(0).getMessage(), equalTo("message0")),
            () -> assertThat("record 1", logRecords.get(1).getLevel(), equalTo(Level.INFO)),
            () -> assertThat("record 1", logRecords.get(1).getMessage(), equalTo("message1")));
    }


    /**
     * ... and the opposite, here we get into blocked state, and then we ENABLE resolving if the record
     * is loggable or not even when we simply don't know. Only records accepted by loggers with default
     * configuration then go to the StartupQueue, usually it means records with level {@link Level#INFO}
     * and higher.
     * <p>
     * Which variant is better depend on what we need to see in logs.
     */
    @Test
    @Order(101)
    @Timeout(5)
    public void reconfigureWithResolvingLevelsWithincompleteconfiguration() throws Exception {
        reconfigureToBlockingHandler();
        setResolveLevelWithIncompleteConfiguration(true);
        assertEquals(PayaraLoggingStatus.CONFIGURING, MANAGER.getLoggingStatus(), "after reconfigureToBlockingHandler");

        doLog(Level.FINEST, "message0");
        doLog(Level.INFO, "message1");
        MANAGER.reconfigure(originalCfg, () -> LOG.setLevel(Level.FINEST), null);
        assertTrue(isResolveLevelWithIncompleteConfiguration(), "isResolveLevelWithIncompleteConfiguration");

        final List<EnhancedLogRecord> logRecords = COLLECTOR.getAll();
        assertAll("Both records must pass via startup queue",
            () -> assertEquals(PayaraLoggingStatus.FULL_SERVICE, MANAGER.getLoggingStatus(),
                "status after all reconfiguration and flushing finished"),
            () -> assertThat(logRecords, hasSize(1)),
            () -> assertThat("record 0", logRecords.get(0).getLevel(), equalTo(Level.INFO)),
            () -> assertThat("record 0", logRecords.get(0).getMessage(), equalTo("message1"))
        );
    }


    private void reconfigureToBlockingHandler() {
        final SortedProperties properties = new SortedProperties();
        properties.setProperty(KEY_ROOT_HANDLERS.getPropertyName(), BlockingExternallyManagedLogHandler.class.getName());
        final PayaraLogManagerConfiguration cfg = new PayaraLogManagerConfiguration(properties);
        MANAGER.reconfigure(cfg);
    }


    /**
     * Because in this test is targetting locking in PJULE, we just execute log in separate thread
     * and wait 10 millis. It cannot block us.
     *
     * @throws Exception
     */
    private void doLog(final Level level, final String message) throws Exception {
        new Thread(() -> LOG.log(level, message)).start();
        Thread.sleep(10L);
    }

    private static final class BlockingAction implements Action {

        private final AtomicBoolean blocker = new AtomicBoolean(true);

        public void reset() {
            blocker.set(true);
        }


        public void unblock() throws InterruptedException {
            blocker.set(false);
            // in this time the log manager thread finishes the action and changes state
            // without this we would continue wit the test without being sure all sides are
            // in expected state.
            // If they are not even after this, test probably detcted some error.
            Thread.sleep(10L);
        }


        @Override
        public void run() {
            while (blocker.get()) {
                Thread.yield();
            }
        }
    }
}
