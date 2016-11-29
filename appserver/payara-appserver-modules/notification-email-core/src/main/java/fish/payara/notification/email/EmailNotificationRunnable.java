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
package fish.payara.notification.email;

import com.sun.mail.util.MailConnectException;
import fish.payara.nucleus.notification.service.NotificationRunnable;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author mertcaliskan
 */
public class EmailNotificationRunnable extends NotificationRunnable<EmailMessageQueue, EmailNotifierConfigurationExecutionOptions> {

    private static Logger logger = Logger.getLogger(EmailNotificationRunnable.class.getCanonicalName());

    private Session session;

    EmailNotificationRunnable(EmailMessageQueue queue, Session session, EmailNotifierConfigurationExecutionOptions executionOptions) {
        this.queue = queue;
        this.session = session;
        this.executionOptions = executionOptions;
    }

    @Override
    public void run() {
        while (queue.size() > 0) {
            try {
                EmailMessage emailMessage = queue.getMessage();
                Message message = new MimeMessage(session);
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(executionOptions.getTo()));
                message.setSubject(emailMessage.getSubject());
                message.setText(emailMessage.getMessage());

                Transport.send(message);
            }
            catch (IllegalArgumentException e) {
                logger.log(Level.SEVERE, "mail configuration properties are not provided carefully.", e);
            }
            catch (AuthenticationFailedException e) {
                logger.log(Level.SEVERE, "Username and Password not accepted.", e);
            }
            catch (MailConnectException e) {
                if (e.getCause() instanceof UnknownHostException) {
                    logger.log(Level.SEVERE, "Cannot connect to mail server with given host address", e);
                }
                else {
                    logger.log(Level.SEVERE, "Cannot connect to mail server", e);
                }
            } catch (MessagingException e) {
                logger.log(Level.SEVERE, "Error occurred while trying to send mail.", e);
            }
        }
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        logger.log(Level.SEVERE, "Error occurred consuming email messages from queue", e);
    }
}