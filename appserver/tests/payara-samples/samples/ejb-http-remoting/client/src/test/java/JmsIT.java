/*
 *    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) [2019] Payara Foundation and/or its affiliates. All rights reserved.
 *
 *    The contents of this file are subject to the terms of either the GNU
 *    General Public License Version 2 only ("GPL") or the Common Development
 *    and Distribution License("CDDL") (collectively, the "License").  You
 *    may not use this file except in compliance with the License.  You can
 *    obtain a copy of the License at
 *    https://github.com/payara/Payara/blob/master/LICENSE.txt
 *    See the License for the specific
 *    language governing permissions and limitations under the License.
 *
 *    When distributing the software, include this License Header Notice in each
 *    file and include the License file at glassfish/legal/LICENSE.txt.
 *
 *    GPL Classpath Exception:
 *    The Payara Foundation designates this particular file as subject to the "Classpath"
 *    exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *    file that accompanied this code.
 *
 *    Modifications:
 *    If applicable, add the following below the License Header, with the fields
 *    enclosed by brackets [] replaced by your own identifying information:
 *    "Portions Copyright [year] [name of copyright owner]"
 *
 *    Contributor(s):
 *    If you wish your version of this file to be governed by only the CDDL or
 *    only the GPL Version 2, indicate your decision by adding "[Contributor]
 *    elects to include this software in this distribution under the [CDDL or GPL
 *    Version 2] license."  If you don't indicate a single choice of license, a
 *    recipient has the option to distribute your version of this file under
 *    either the CDDL, the GPL Version 2 or to extend the choice of license to
 *    its licensees as provided above.  However, if you add GPL Version 2 code
 *    and therefore, elected the GPL Version 2 license, then the option applies
 *    only if the new code is made subject to such option by the copyright
 *    holder.
 */

import fish.payara.samples.ejbhttp.client.JmsClientExample;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import javax.jms.*;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.rmi.PortableRemoteObject;
import java.lang.IllegalStateException;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class JmsIT {

    @Parameters(name = "{0}")
    public static Iterable<JmsClientExample> clients() {
        return Arrays.asList(JmsClientExample.values());
    }

    @Parameter
    public JmsClientExample client;

    @Test
    public void sendAndReceiveMessage() throws NamingException, JMSException {
        final String connectionFactoryName = "jms/ConnectionFactory";
        InitialContext jndi = client.getContext();

        // some real world JMS over remote JNDI code...

        final Object lObject = jndi.lookup(connectionFactoryName);
        QueueConnectionFactory queueConnectionFactory = (QueueConnectionFactory) PortableRemoteObject.narrow(lObject, QueueConnectionFactory.class);


        if (queueConnectionFactory == null) {
            throw new IllegalStateException("The JNDI Queue Conection Factory " + connectionFactoryName + " lookup failed.");
        }

        QueueConnection queueConnection = queueConnectionFactory.createQueueConnection();
        queueConnection.start();
        QueueSession queueSession = queueConnection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);

        // sending
        String pJNDIQueueName = "queue/test.queue";
        Queue queue = (Queue) PortableRemoteObject.narrow(jndi.lookup(pJNDIQueueName), Queue.class);
        QueueSender queueSender = queueSession.createSender(queue);

        queueSender.send(queueSession.createTextMessage("It works"));

        // receiving
        QueueReceiver queueReceiver = queueSession.createReceiver(queue);

        Message m = queueReceiver.receive(1000);

        assertThat(m).isNotNull().isInstanceOf(TextMessage.class);

        TextMessage tm = (TextMessage) m;
        assertThat(tm.getText()).isEqualTo("It works");

        queueConnection.stop();
        queueConnection.close();
    }
}
