/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2020-2024] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.nucleus.notification;

import static fish.payara.internal.notification.NotifierUtils.getNotifierName;
import static java.lang.Boolean.valueOf;
import static java.lang.String.format;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.hk2.api.ServiceHandle;

import fish.payara.internal.notification.PayaraConfiguredNotifier;
import fish.payara.internal.notification.PayaraNotification;
import fish.payara.internal.notification.PayaraNotifier;
import fish.payara.internal.notification.PayaraNotifierConfiguration;

public class NotifierHandler implements Consumer<PayaraNotification> {

    private static final Logger LOGGER = Logger.getLogger(NotifierHandler.class.getName());

    private final PayaraNotifier notifier;
    private final PayaraNotifierConfiguration config;
    private final String notifierName;

    private final BlockingQueue<PayaraNotification> notificationQueue;
    
    private final ScheduledExecutorService executor;
    private final AtomicInteger threadNumber = new AtomicInteger(1);
    private ScheduledFuture<?> taskResult;

    public NotifierHandler(final ServiceHandle<PayaraNotifier> notifierHandle) {
        this(notifierHandle, null);
    }

    public NotifierHandler(final ServiceHandle<PayaraNotifier> notifierHandle, final PayaraNotifierConfiguration config) {
        this.notifier = notifierHandle.getService();
        this.notifierName = getNotifierName(notifierHandle.getActiveDescriptor());
        this.config = config;

        this.notificationQueue = new LinkedBlockingDeque<>();
        this.executor = Executors.newScheduledThreadPool(1,
                r -> new Thread(r, notifierName + "-" + threadNumber.getAndIncrement()));
    }

    protected PayaraNotifierConfiguration getConfig() {
        return config;
    }

    protected String getName() {
        return notifierName;
    }

    protected void reconfigure() {
        // Get the current configuration
        PayaraNotifierConfiguration currentConfig = null;
        if (config != null) {
            currentConfig = PayaraConfiguredNotifier.class.cast(notifier).getConfiguration();
            if (currentConfig == null) {
                currentConfig = config;
            }
        }

        final boolean enabled = config != null && valueOf(config.getEnabled());
        final boolean wasEnabled = config != null && valueOf(currentConfig.getEnabled());

        if (!enabled) {
            if (wasEnabled) {
                // If the notifier isn't enabled but was before
                destroy();
            }
        } else {
            if (wasEnabled) {
                // If the notifier is enabled and was before
                destroy();
                bootstrap();
            } else {
                // If the notifier is enabled and wasn't before
                bootstrap();
            }
        }
    }

    protected synchronized void destroy() {
        // Should only destroy a notifier if it's enabled before any configuration change
        final boolean wasEnabled = isEnabled();

        // Set the configuration before destroying the notifier
        if (config != null) {
            PayaraConfiguredNotifier.class.cast(notifier).setConfiguration(config);
        }
        if (taskResult != null) {
            taskResult.cancel(true);
        }
        if (wasEnabled) {
            notifier.destroy();
        }
    }

    @SuppressWarnings("unchecked")
    protected synchronized void bootstrap() {
        // Set the configuration before bootstrapping the notifier
        if (config != null) {
            PayaraConfiguredNotifier.class.cast(notifier).setConfiguration(config);
        }
        if (isEnabled()) {
            notifier.bootstrap();
            taskResult = executor.scheduleWithFixedDelay(this::run, 0, 5, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public void accept(final PayaraNotification notification) {
        if (isEnabled() && !this.notificationQueue.offer(notification)) {
            LOGGER.warning(format("Notifier %s failed to accept the notification \"%s\".", notifierName, notification));
        }
    }

    private void run() {
        assert isEnabled();
        try {
            final PayaraNotification notification = notificationQueue.take();
            try {
                if (notification != null) {
                    notifier.tryHandleNotification(notification);
                }
            } catch (final Exception ex) {
                LOGGER.log(Level.WARNING,
                        format("Notifier %s failed to handle notification \"%s\".", notifierName, notification), ex);
            }
        } catch (InterruptedException ex) {
            LOGGER.log(Level.FINE, format("Cancelled waiting on queue for notifier \"%s\".", notifierName), ex);
        }
    }

    /**
     * @return true if the current notifier is enabled, or false otherwise
     */
    private boolean isEnabled() {
        if (config != null) {
            return valueOf(PayaraConfiguredNotifier.class.cast(notifier).getConfiguration().getEnabled());
        }
        return true;
    }

}
