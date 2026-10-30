/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2016-2024] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.jmx.monitoring;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

import javax.management.MBeanServer;

import fish.payara.internal.notification.EventLevel;
import org.glassfish.hk2.api.messaging.Topic;

import fish.payara.internal.notification.PayaraNotification;
import fish.payara.internal.notification.PayaraNotificationBuilder;
import fish.payara.internal.notification.PayaraNotificationFactory;

/**
 * The runnable class which gathers JMX monitoring info from a list of
 * JMXMonitoringJob objects and builds a log string from it.
 *
 * @since 4.1.1.163
 * @author savage
 */
public class JMXMonitoringFormatter implements Runnable {

    private final String LOGMESSAGE_PREFIX = "JMX-MONITORING:";
    private final String NOTIFICATION_SUBJECT = "MBean Attributes: ";

    private final MBeanServer mBeanServer;
    private final List<JMXMonitoringJob> JmxMonitoringJobs;
    private final JMXMonitoringService monitoringService;
    private final Collection<String> enabledNotifiers;
    private Topic<PayaraNotification> notificationEventBus;
    private PayaraNotificationFactory notificationFactory;

    /**
     * Constructor for the JMXMonitoringFormatter class.
     *
     * @param mBeanServer The MBeanServer to monitor.
     * @param jobs List of monitoring jobs to perform.
     * @param monitoringService the monitoring service
     * @param store the store that holds the various event factories that are
     * needed to build notification events
     * @param notificationService the notification service containing all the
     * notifiers
     */
    //This is done this way and not through injection as the result is a hk2 circular dependency error as this class
    //is also used in JMXMonitoringService and each cannot be injected into the other
    public JMXMonitoringFormatter(MBeanServer mBeanServer, List<JMXMonitoringJob> jobs,
            JMXMonitoringService monitoringService, Topic<PayaraNotification> notificationEventBus,
            PayaraNotificationFactory notificationFactory, Collection<String> enabledNotifiers) {
        this.mBeanServer = mBeanServer;
        this.JmxMonitoringJobs = jobs;
        this.monitoringService = monitoringService;
        this.notificationEventBus = notificationEventBus;
        this.notificationFactory = notificationFactory;
        this.enabledNotifiers = enabledNotifiers;
    }

    /**
     * Class runnable method. Calls getJMXMonitoringInfo on all
     * JMXMonitoringJobs passing the MBeanServer. Uses the results to build a
     * String for the log message.
     */
    @Override
    public void run() {
        StringBuilder monitoringString = new StringBuilder();

        monitoringString.append(NOTIFICATION_SUBJECT);

        for (JMXMonitoringJob job : JmxMonitoringJobs) {
            monitoringString.append(job.getMonitoringInfo(mBeanServer));
        }

        sendNotification(Level.INFO, monitoringString.toString(), JmxMonitoringJobs.toArray());
    }

    /**
     * Sends a notification to all notifiers enabled with the JMX monitoring
     * service
     * <p>
     * The subject of the notification will be {@code MBean Attributes:}
     *
     * @since 4.1.2.174
     * @param level Log level to notification at
     * @param message The message to send
     * @param parameters An array of the objects that the notification is about
     * i.e. the Mbeans being monitored
     */
    private void sendNotification(Level level, String message, Object[] parameters) {
        PayaraNotificationBuilder notification = notificationFactory.newBuilder()
            .whitelist(enabledNotifiers.toArray(new String[0]))
            .subject(LOGMESSAGE_PREFIX + message)
            .level(EventLevel.INFO);

        if (parameters != null && parameters.length > 0) {
            notification = notification.message(MessageFormat.format(message, parameters));
        }

        notificationEventBus.publish(notification.build());
    }
}
