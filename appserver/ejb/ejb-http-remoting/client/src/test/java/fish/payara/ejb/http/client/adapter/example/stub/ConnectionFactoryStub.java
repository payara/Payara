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

package fish.payara.ejb.http.client.adapter.example.stub;

import fish.payara.ejb.http.client.adapter.example.stub.NoOpJmsStub.*;

import jakarta.jms.Connection;
import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageProducer;
import jakarta.jms.Queue;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;

class ConnectionFactoryStub extends NoOpConnectionFactory {

    private String factoryName;
    private final RemoteCallAdapter adapter;

    ConnectionFactoryStub(String factoryName, RemoteCallAdapter adapter) {
        this.factoryName = factoryName;
        this.adapter = adapter;
    }

    @Override
    public Connection createConnection() throws JMSException {
        return new ConnectionStub();
    }

    @Override
    public Connection createConnection(String userName, String password) throws JMSException {
        return new ConnectionStub();
    }

    class ConnectionStub extends NoOpConnection {

        @Override
        public Session createSession(boolean transacted, int acknowledgeMode) throws JMSException {
            return new SessionStub();
        }

        @Override
        public Session createSession(int sessionMode) throws JMSException {
            return new SessionStub();
        }

        @Override
        public Session createSession() throws JMSException {
            return new SessionStub();
        }

        @Override
        public String getClientID() throws JMSException {
            return null;
        }

        @Override
        public void setClientID(String clientID) throws JMSException {

        }

        @Override
        public void start() throws JMSException {

        }

        @Override
        public void stop() throws JMSException {

        }
    }

    private class SessionStub extends NoOpSession {


        @Override
        public TextMessage createTextMessage() throws JMSException {
            return new TextMessageStub();
        }

        @Override
        public TextMessage createTextMessage(String text) throws JMSException {
            TextMessage result = new TextMessageStub();
            result.setText(text);
            return result;
        }

        @Override
        public boolean getTransacted() throws JMSException {
            return false;
        }

        @Override
        public int getAcknowledgeMode() throws JMSException {
            return Session.AUTO_ACKNOWLEDGE;
        }

        @Override
        public void commit() throws JMSException {

        }

        @Override
        public void rollback() throws JMSException {

        }

        @Override
        public void recover() throws JMSException {

        }

        @Override
        public MessageProducer createProducer(Destination destination) throws JMSException {
            if (destination instanceof QueueStub) {
                return new QueueMessageProducerStub((QueueStub)destination);
            }
            throw new JMSException("No foreign destinations allowed, only those obtained through the session");
        }

        @Override
        public Queue createQueue(String queueName) throws JMSException {
            return new QueueStub(queueName);
        }

    }

    private class TextMessageStub extends NoOpTextMessage {
        private String text;

        @Override
        public void setText(String string) throws JMSException {
            this.text = string;
        }

        @Override
        public String getText() throws JMSException {
            return this.text;
        }

        @Override
        public <T> T getBody(Class<T> c) throws JMSException {
            if (String.class == c) {
                return (T)getText();
            } else {
                throw new ClassCastException("Only string body is supported");
            }
        }

        @Override
        public boolean isBodyAssignableTo(Class c) throws JMSException {
            return c == String.class;
        }
    }


    static class QueueStub extends NoOpQueue {


        private String queueName;

        public QueueStub(String queueName) {
            this.queueName = queueName;
        }

        @Override
        public String getQueueName() throws JMSException {
            return queueName;
        }
    }

    private class QueueMessageProducerStub extends NoOpMessageProducer {
        private QueueStub destination;

        public QueueMessageProducerStub(QueueStub destination) {
            this.destination = destination;
        }

        @Override
        public void send(Message message) throws JMSException {
            if (message instanceof TextMessage) {
                adapter.sendTextMessageToQueue(((TextMessage) message).getText(), destination.getQueueName(), factoryName);
            } else {
                throw new JMSException("Only TextMessage is supported");
            }
        }

        @Override
        public void send(Message message, int deliveryMode, int priority, long timeToLive) throws JMSException {
            send(message);
        }
    }
}