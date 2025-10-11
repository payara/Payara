/*
 * Copyright (c) [2016-2021] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.notification.jms;

import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSException;
import jakarta.jms.MessageProducer;
import jakarta.jms.Queue;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.NoInitialContextException;

import com.sun.enterprise.util.StringUtils;

import org.glassfish.api.StartupRunLevel;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

import fish.payara.internal.notification.PayaraConfiguredNotifier;
import fish.payara.internal.notification.PayaraNotification;

/**
 * @author mertcaliskan
 */
@Service(name = "jms-notifier")
@RunLevel(StartupRunLevel.VAL)
public class JmsNotifierService extends PayaraConfiguredNotifier<JmsNotifierConfiguration> {

    private static final Logger LOGGER = Logger.getLogger(JmsNotifierService.class.getCanonicalName());

    private Connection connection;

    @Override
    public void bootstrap() {
        super.bootstrap();
        try {
            final String contextFactoryClass = configuration.getContextFactoryClass();
            final String connectionFactoryName = configuration.getConnectionFactoryName();
            final String url = configuration.getUrl();
            final String username = configuration.getUsername();
            final String password = configuration.getPassword();

            final Properties env = new Properties();
            if (StringUtils.ok(contextFactoryClass)) {
                env.put(Context.INITIAL_CONTEXT_FACTORY, contextFactoryClass);
            }
            if (StringUtils.ok(url)) {
                env.put(Context.PROVIDER_URL, url);
            }
            if (StringUtils.ok(username)) {
                env.put(Context.SECURITY_PRINCIPAL, username);
            }
            if (StringUtils.ok(password)) {
                env.put(Context.SECURITY_CREDENTIALS, password);
            }
            if (StringUtils.ok(connectionFactoryName)) {
                try {
                    InitialContext ctx = new InitialContext(env);
                    ConnectionFactory connectionFactory =
                            (ConnectionFactory) ctx.lookup(connectionFactoryName);
                    this.connection = connectionFactory.createConnection();
                }
                catch (NoInitialContextException e) {
                    if (e.getRootCause() instanceof ClassNotFoundException) {
                        LOGGER.log(Level.SEVERE, "Context factory class cannot be found on classpath: " + configuration.getContextFactoryClass());
                    }
                }
            }
        } catch (NamingException e) {
            LOGGER.log(Level.SEVERE, "Cannot lookup JMS resources", e);
        } catch (JMSException e) {
            LOGGER.log(Level.SEVERE, "Cannot create JMS connection", e);
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        if (connection != null) {
            try {
                connection.close();
            } catch (JMSException e) {
                LOGGER.log(Level.SEVERE, "Could not close connection", e);
            }
        }
    }

    @Override
    public void handleNotification(PayaraNotification event) {
        if (connection == null) {
            LOGGER.log(Level.SEVERE, "Invalid connection");
            return;
        }

        try (Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)) {
            Queue jmsQueue = session.createQueue(configuration.getQueueName());
            MessageProducer producer = session.createProducer(jmsQueue);
            TextMessage message = session.createTextMessage();
            message.setText(getTextMessage(event));
            producer.send(message);
            LOGGER.log(Level.FINE, "Message successfully sent");
        } catch (JMSException e) {
            LOGGER.log(Level.SEVERE, "Error occurred while creating session", e);
        }
    }

    private static String getTextMessage(PayaraNotification event) {
        final String subject = event.getSubject();
        final String message = event.getMessage();
        String result = "";
        if (subject != null) {
            result += subject;
        }
        if (message != null) {
            result += "\n" + message;
        }
        return result.trim();
    }
}