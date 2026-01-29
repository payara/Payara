/*
 *    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) [2019-2021] Payara Foundation and/or its affiliates. All rights reserved.
 *
 *    The contents of this file are subject to the terms of either the GNU
 *    General Public License Version 2 only ("GPL") or the Common Development
 *    and Distribution License("CDDL") (collectively, the "License").  You
 *    may not use this file except in compliance with the License.  You can
 *    obtain a copy of the License at
 *    https://github.com/payara/Payara/blob/main/LICENSE.txt
 *    See the License for the specific
 *    language governing permissions and limitations under the License.
 *
 *    When distributing the software, include this License Header Notice in each
 *    file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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

package fish.payara.ejb.http.client.adapter.example.client;

import fish.payara.ejb.http.client.RemoteEJBContextFactory;
import fish.payara.ejb.http.client.adapter.CompositeClientAdapter;
import fish.payara.ejb.http.client.adapter.example.stub.ConnectionFactoryAdapter;
import fish.payara.ejb.http.client.adapter.example.stub.QueueAdapter;
import fish.payara.ejb.http.client.adapter.example.stub.RemoteCallAdapter;
import org.junit.Test;

import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageProducer;
import jakarta.jms.Queue;
import jakarta.jms.Session;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.Properties;

import static fish.payara.ejb.http.client.adapter.ClientAdapterCustomizer.customize;
import static org.junit.Assert.assertEquals;

public class ClientUsageTest {
    String sentPayload;
    String sentToQueue;
    String sentViaFactory;
    @Test
    public void remoteJmsEmulation() throws NamingException, JMSException {

        RemoteCallAdapter remoteCallMock = (payload, queueName, factoryName) ->  {
            this.sentPayload = payload;
            this.sentToQueue = queueName;
            this.sentViaFactory = factoryName;
            return true;
        };

        // Usual remote JMS interaction
        Properties props = new Properties();
        props.put(Context.INITIAL_CONTEXT_FACTORY, RemoteEJBContextFactory.FACTORY_CLASS);
        props.put(Context.PROVIDER_URL, "http://not.relevant/");
        props.put(RemoteEJBContextFactory.FISH_PAYARA_CLIENT_ADAPTER,
                CompositeClientAdapter.newBuilder()
                    .register(customize(new ConnectionFactoryAdapter((ctx) -> remoteCallMock)).matchPrefix("jms/"),
                            customize(QueueAdapter.class).matchPrefix("queue/"))
                    .build()
                );
        Context context = new InitialContext(props);

        ConnectionFactory connFactory = (ConnectionFactory) context.lookup("jms/RemoteConnectionFactory");
        Connection conn = connFactory.createConnection();
        Session session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);

        Queue queue = (Queue) context.lookup("queue/testQueue");
        MessageProducer producer = session.createProducer(queue);
        Message messageToSend = session.createTextMessage("hello world");
        producer.send(messageToSend);


        assertEquals("hello world", sentPayload);
        assertEquals("testQueue", sentToQueue);
        assertEquals("RemoteConnectionFactory", sentViaFactory);
    }
}
