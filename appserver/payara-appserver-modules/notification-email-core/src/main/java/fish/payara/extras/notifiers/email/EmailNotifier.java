/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2016-2020] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.extras.notifiers.email;

import static java.lang.String.format;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.mail.AuthenticationFailedException;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.SendFailedException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.glassfish.api.StartupRunLevel;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

import fish.payara.internal.notification.PayaraConfiguredNotifier;
import fish.payara.internal.notification.PayaraNotification;

/**
 * @author mertcaliskan
 */
@Service(name = "email-notifier")
@RunLevel(StartupRunLevel.VAL)
public class EmailNotifier extends PayaraConfiguredNotifier<EmailNotifierConfiguration> {

    private static Logger LOGGER = Logger.getLogger(EmailNotifier.class.getCanonicalName());

    private Session session;

    @Override
    public void handleNotification(PayaraNotification event) {
        if (session == null) {
            LOGGER.fine("Email notifier received notification, but no Java Mail session was available.");
            return;
        }

        final String message = event.getMessage();
        final String subject = String.format("%s. (host: %s, server: %s, domain: %s, instance: %s)", 
                event.getSubject(),
                event.getHostName(),
                event.getServerName(),
                event.getDomainName(),
                event.getInstanceName());

        try {
            Message mime = new MimeMessage(session);
            mime.setRecipients(Message.RecipientType.TO, InternetAddress.parse(configuration.getRecipient()));
            mime.setSubject(subject);
            mime.setText(message);

            Transport.send(mime);
            LOGGER.log(Level.FINE, "Email successfully sent");
        } catch (IllegalArgumentException e) {
            LOGGER.log(Level.SEVERE, "Email configuration properties were not provided correctly.", e);
        } catch (AuthenticationFailedException e) {
            LOGGER.log(Level.SEVERE, "Username and Password not accepted.", e);
        } catch (SendFailedException e) {
            LOGGER.log(Level.SEVERE, "Cannot send to specified recipient", e);
        } catch (MessagingException e) {
            LOGGER.log(Level.SEVERE, "Error occurred while trying to send mail.", e);
        }
    }

    @Override
    public void bootstrap() {
        final String jndiName = configuration.getJndiName();
        try {
            InitialContext context = new InitialContext();
            this.session = (Session) context.lookup(jndiName);
        } catch (NamingException e) {
            LOGGER.log(Level.SEVERE, format("Cannot lookup Java Mail session with given JNDI name: %s.", jndiName), e);
        }
    }

    @Override
    public void destroy() {
        if (session != null) {
            session = null;
        }
    }

}
