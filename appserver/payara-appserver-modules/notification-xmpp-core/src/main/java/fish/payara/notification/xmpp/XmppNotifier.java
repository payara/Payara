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

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.api.StartupRunLevel;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.MultiUserChatManager;
import org.jvnet.hk2.annotations.Service;

import fish.payara.internal.notification.PayaraConfiguredNotifier;
import fish.payara.internal.notification.PayaraNotification;

/**
 * @author mertcaliskan
 */
@Service(name = "xmpp-notifier")
@RunLevel(StartupRunLevel.VAL)
public class XmppNotifier extends PayaraConfiguredNotifier<XmppNotifierConfiguration> {

    private static Logger LOGGER = Logger.getLogger(XmppNotifier.class.getCanonicalName());

    private XMPPTCPConnection connection;

    @Override
    public void handleNotification(PayaraNotification event) {
        if (connection == null) {
            LOGGER.fine("XMPP notifier received notification, but no connection was available.");
            return;
        }

        try {
            MultiUserChatManager manager = MultiUserChatManager.getInstanceFor(connection);
            MultiUserChat multiUserChat = manager
                    .getMultiUserChat(configuration.getRoomId() + "@" + configuration.getServiceName());

            if (multiUserChat != null) {
                if (!multiUserChat.isJoined()) {
                    multiUserChat.join(configuration.getUsername(), configuration.getPassword());
                }

                final String body = event.getMessage();
                final String subject = getDetailedSubject(event);

                Message message = new Message();
                message.setSubject(subject);
                message.setBody(body);

                multiUserChat.sendMessage(message);
            }
            LOGGER.log(Level.FINE, "Message sent successfully");
        } catch (XMPPException | SmackException e) {
            LOGGER.log(Level.SEVERE, "Error occurred while sending message to room", e);
        }
    }

    @Override
    public void bootstrap() {
        try {
            XMPPTCPConnectionConfiguration xmppConfig = XMPPTCPConnectionConfiguration.builder()
                    .setSecurityMode(Boolean.valueOf(configuration.getSecurityDisabled()) ?
                            ConnectionConfiguration.SecurityMode.disabled :
                            ConnectionConfiguration.SecurityMode.required)
                    .setServiceName(configuration.getServiceName())
                    .setHost(configuration.getHost())
                    .setPort(Integer.valueOf(configuration.getPort()))
                    .build();
            this.connection = new XMPPTCPConnection(xmppConfig);
            connection.connect();

            final String username = configuration.getUsername();
            final String password = configuration.getPassword();
            if (username != null && password != null) {
                connection.login(username, password);
            } else {
                connection.login();
            }
        } catch (XMPPException e) {
            LOGGER.log(Level.SEVERE, "Error occurred on XMPP protocol level while connecting host", e);
        } catch (SmackException e) {
            LOGGER.log(Level.SEVERE, "Error occurred on Smack protocol level while connecting host", e);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "IO Error occurred while connecting host", e);
        }
    }

    @Override
    public void destroy() {
        if (connection != null) {
            connection.disconnect();
        }
    }

    private static String getDetailedSubject(PayaraNotification event) {
        return String.format("%s. (host: %s, server: %s, domain: %s, instance: %s)", 
                event.getSubject(),
                event.getHostName(),
                event.getServerName(),
                event.getDomainName(),
                event.getInstanceName());
    }
}