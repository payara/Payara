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
import java.util.logging.Logger;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;

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
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author David Matejcek
 */
@TestMethodOrder(OrderAnnotation.class)
public class TwoPhaseStartupTest {

    private static final PayaraLogManager MANAGER = PayaraLogManager.getLogManager();
    private static final AtomicBoolean BLOCK_RECONFIG = new AtomicBoolean(true);
    private static final AtomicBoolean BLOCK_FLUSH = new AtomicBoolean(true);
    private static final Logger LOG = Logger.getLogger("mycustomtestlogger");
    private static PayaraLogManagerConfiguration originalCfg;

    @BeforeAll
    public static void backupConfiguration() {
        originalCfg = MANAGER.getConfiguration();
    }


    @AfterAll
    public static void resetConfiguration() {
        BLOCK_FLUSH.set(false);
        BLOCK_RECONFIG.set(false);
        Thread.currentThread().interrupt();
        MANAGER.reconfigure(originalCfg, null, null);
    }


    @Test
    @Order(1)
    public void reconfigureToBlock() {
        final SortedProperties properties = new SortedProperties();
        properties.setProperty(PayaraLogManager.KEY_ROOT_HANDLERS.getPropertyName(),
            BlockingExternallyManagedLogHandler.class.getName());
        final PayaraLogManagerConfiguration cfg = new PayaraLogManagerConfiguration(properties);
        MANAGER.reconfigure(cfg);
        assertEquals(PayaraLoggingStatus.CONFIGURING, MANAGER.getLoggingStatus());
        LOG.log(Level.INFO, "Hello StartupQueue, my old friend");
    }


    @Test
    @Order(2)
    @Timeout(10)
    public void externalReconfiguration() throws Exception {
        final LogCollectorHandler collector = new LogCollectorHandler(LOG);
        collector.setLevel(Level.FINE);
        final Action reconfig = () -> {
            while (BLOCK_RECONFIG.get()) {
                Thread.yield();
            }
        };
        final Action flush = () -> {
            while (BLOCK_FLUSH.get()) {
                Thread.yield();
            }
        };

        assertEquals(PayaraLoggingStatus.CONFIGURING, MANAGER.getLoggingStatus(), "status after reconfigureToBlock test");

        doLog(Level.FINE, "Log before reconfiguration");
        doLog(Level.FINEST, "This log should be ignored");
        final CompletableFuture<Void> future = runAsync(() -> MANAGER.reconfigure(originalCfg, reconfig, flush));
        assertAll(
            () -> assertEquals(PayaraLoggingStatus.CONFIGURING, MANAGER.getLoggingStatus(),
                "status after reconfiguration started"),
            () -> assertNull(collector.pop(), "collector should be empty after reconfigure started")
        );
        doLog(Level.INFO, "Log after reconfiguration started");
        assertNull(collector.pop(), "collector should be empty after reconfiguration started even if we log again");

        BLOCK_RECONFIG.set(false);
        Thread.yield();
        assertAll(
            () -> assertEquals(PayaraLoggingStatus.FLUSHING_BUFFERS, MANAGER.getLoggingStatus(),
                "status after reconfiguration finished"),
            () -> assertNull(collector.pop(), "collector should be empty after reconfiguration finished")
        );

        doLog(Level.SEVERE, "Log while flushing is still executed");
        assertNull(collector.pop(), "collector should be empty after reconfiguration finished even if we log again");

        BLOCK_FLUSH.set(false);
        Thread.yield();
        doLog(Level.INFO, "Log after flushing finished");
        final List<EnhancedLogRecord> logRecords = collector.getAll();
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

        future.get(5, TimeUnit.SECONDS);
    }


    /**
     * Because in this test is targetting locking in PJULE, we just execute log in separate thread
     * and wait 10 millis. It cannot block us.
     * @throws Exception
     */
    private void doLog(final Level level, final String message) throws Exception {
        new Thread(() -> {
            // the order of logs is undefined when parallel threads use loggers.
            // so we keep the order at least here.
            synchronized (LOG) {
                LOG.log(level, message);
            }
        }).start();
        Thread.sleep(10L);
    }

}
