/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2016 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.notification.jms;

import fish.payara.nucleus.notification.service.NotificationRunnable;

import javax.jms.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author mertcaliskan
 */
public class JmsNotificationRunnable extends NotificationRunnable<JmsMessageQueue, JmsNotifierConfigurationExecutionOptions> {

    private static Logger logger = Logger.getLogger(JmsNotificationRunnable.class.getCanonicalName());

    JmsNotifierConfigurationExecutionOptions executionOptions;
    private Connection connection;

    public JmsNotificationRunnable(JmsMessageQueue queue, JmsNotifierConfigurationExecutionOptions executionOptions, Connection connection) {
        this.queue = queue;
        this.executionOptions = executionOptions;
        this.connection = connection;
    }

    @Override
    public void run() {
        while (queue.size() > 0) {
            try (Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)) {
                Queue jmsQueue = session.createQueue(executionOptions.getQueueName());
                MessageProducer producer = session.createProducer(jmsQueue);
                TextMessage message = session.createTextMessage();
                JmsMessage jmsMessage = queue.getMessage();
                message.setText(jmsMessage.getSubject() + "\n" + jmsMessage.getMessage());
                producer.send(message);
            } catch (JMSException e) {
                logger.log(Level.SEVERE, "Error occurred while creating session", e);
            }
        }
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        logger.log(Level.SEVERE, "Error occurred consuming JMS messages from queue", e);
    }
}
