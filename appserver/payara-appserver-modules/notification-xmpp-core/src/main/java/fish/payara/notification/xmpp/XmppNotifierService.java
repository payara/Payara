/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2016-2019 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.notification.xmpp;

import fish.payara.nucleus.notification.configuration.NotifierType;
import fish.payara.nucleus.notification.configuration.XmppNotifier;
import fish.payara.nucleus.notification.domain.NotificationEvent;
import fish.payara.nucleus.notification.service.QueueBasedNotifierService;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jvnet.hk2.annotations.Service;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.hk2.api.messaging.MessageReceiver;
import org.glassfish.hk2.api.messaging.SubscribeTo;

/**
 * @author mertcaliskan
 */
@Service(name = "service-xmpp")
@RunLevel(StartupRunLevel.VAL)
@MessageReceiver
public class XmppNotifierService extends QueueBasedNotifierService<XmppNotificationEvent,
        XmppNotifier,
        XmppNotifierConfiguration,
        XmppMessageQueue> {

    private static Logger logger = Logger.getLogger(XmppNotifierService.class.getCanonicalName());
    private XMPPTCPConnection connection;
    private XmppNotifierConfigurationExecutionOptions executionOptions;

    XmppNotifierService() {
        super("xmpp-message-consumer-");
    }

    @Override
    public void bootstrap() {
        register(NotifierType.XMPP, XmppNotifier.class, XmppNotifierConfiguration.class);

        try {
            executionOptions = (XmppNotifierConfigurationExecutionOptions) getNotifierConfigurationExecutionOptions();

            if (executionOptions != null && executionOptions.isEnabled()) {
                initializeExecutor();

                XMPPTCPConnectionConfiguration configuration = XMPPTCPConnectionConfiguration.builder()
                        .setSecurityMode(executionOptions.getSecurityDisabled() ?
                                ConnectionConfiguration.SecurityMode.disabled :
                                ConnectionConfiguration.SecurityMode.required)
                        .setServiceName(executionOptions.getServiceName())
                        .setHost(executionOptions.getHost())
                        .setPort(executionOptions.getPort())
                        .build();
                connection = new XMPPTCPConnection(configuration);
                connection.connect();
                if (executionOptions.getUsername() != null && executionOptions.getPassword() != null) {
                    connection.login(executionOptions.getUsername(), executionOptions.getPassword());
                } else {
                    connection.login();
                }
                scheduleExecutor(new XmppNotificationRunnable(queue, executionOptions, connection));
            }
        }
        catch (XMPPException e) {
            logger.log(Level.SEVERE, "Error occurred on XMPP protocol level while connecting host", e);
        }
        catch (SmackException e) {
            logger.log(Level.SEVERE, "Error occurred on Smack protocol level while connecting host", e);
        }
        catch (IOException e) {
            logger.log(Level.SEVERE, "IO Error occurred while connecting host", e);
        }
    }

    @Override
    public void shutdown() {
        super.shutdown();
        if (connection != null) {
            connection.disconnect();
        }
    }

    @Override
    public void handleNotification(@SubscribeTo NotificationEvent event) {
        if (event instanceof XmppNotificationEvent && executionOptions != null && executionOptions.isEnabled()) {
            XmppMessage message = new XmppMessage((XmppNotificationEvent) event, event.getSubject(), event.getMessage());
            queue.addMessage(message);
        }
    }
}