/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2020] Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
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
package fish.payara.nucleus.notification.next;

import static fish.payara.internal.notification.NotifierUtils.getNotifierName;
import static java.lang.Boolean.valueOf;
import static java.lang.String.format;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.hk2.api.ServiceHandle;

import fish.payara.internal.notification.PayaraConfiguredNotifier;
import fish.payara.internal.notification.PayaraNotification;
import fish.payara.internal.notification.PayaraNotifier;
import fish.payara.internal.notification.PayaraNotifierConfiguration;

public class NotifierHandler implements Runnable, Consumer<PayaraNotification> {

    private static final Logger LOGGER = Logger.getLogger(NotifierHandler.class.getName());

    private final LazyService lazy;
    private final String notifierName;

    private final Queue<PayaraNotification> notificationQueue;

    public NotifierHandler(ServiceHandle<PayaraNotifier> notifierHandle) {
        this(notifierHandle, null);
    }

    public NotifierHandler(ServiceHandle<PayaraNotifier> notifierHandle, PayaraNotifierConfiguration config) {
        this.lazy = new LazyService(notifierHandle, config);
        this.notifierName = getNotifierName(notifierHandle.getActiveDescriptor());
        this.notificationQueue = new ArrayDeque<>();
    }

    protected String getName() {
        return notifierName;
    }

    protected void destroy() {
        if (lazy.isEnabled()) {
            lazy.getNotifier().destroy();
        }
    }

    protected void bootstrap() {
        if (lazy.isEnabled()) {
            lazy.getNotifier().bootstrap();
        }
    }

    @Override
    public void accept(PayaraNotification notification) {
        if (lazy.isEnabled() && !this.notificationQueue.add(notification)) {
            LOGGER.warning(format("Notifier %s failed to accept the notification \"%s\".", notifierName, notification));
        }
    }

    @Override
    public void run() {
        final PayaraNotification notification = notificationQueue.peek();
        try {
            if (notification != null && lazy.isEnabled()) {
                lazy.getNotifier().handleNotification(notification);
                notificationQueue.remove();
            }
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING,
                    format("Notifier %s failed to handle notification \"%s\".", notifierName, notification), ex);
        }
    }

    private static class LazyService {

        private final ServiceHandle<PayaraNotifier> notifierHandle;
        private final PayaraNotifierConfiguration notifierConfig;
        private PayaraNotifier notifier;

        private LazyService(ServiceHandle<PayaraNotifier> notifierHandle, PayaraNotifierConfiguration notifierConfig) {
            this.notifierHandle = notifierHandle;
            this.notifierConfig = notifierConfig;
        }

        private boolean isEnabled() {
            if (notifierConfig == null) {
                return true;
            }
            if (notifier == null) {
                return valueOf(notifierConfig.getEnabled());
            }
            return valueOf(PayaraConfiguredNotifier.class.cast(notifier).getConfiguration().getEnabled());
        }

        private synchronized PayaraNotifier getNotifier() {
            if (notifier == null) {
                notifier = notifierHandle.getService();
            }
            return notifier;
        }

    }
    
}
