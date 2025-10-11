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

import jakarta.jms.*;
import java.io.Serializable;
import java.util.Enumeration;

/**
 * A stub that throws UnsupportedOperationException or JMSException for any operation (except for close).
 * Basis for any more useful stub
 */
public class NoOpJmsStub {
    private NoOpJmsStub() {

    }

    static JMSException jmsException() {
        return new JMSException("This operation is not supported by this stub");
    }

    static JMSRuntimeException jmsRuntime() {
        return new JMSRuntimeException("This operation is not supported by this stub");
    }

    static class NoOpConnectionFactory implements ConnectionFactory {

        @Override
        public Connection createConnection() throws JMSException {
            throw jmsException();
        }

        @Override
        public Connection createConnection(String userName, String password) throws JMSException {
            throw jmsException();
        }

        @Override
        public JMSContext createContext() {
            throw jmsRuntime();
        }

        @Override
        public JMSContext createContext(String userName, String password) {
            throw jmsRuntime();
        }

        @Override
        public JMSContext createContext(String userName, String password, int sessionMode) {
            throw jmsRuntime();
        }

        @Override
        public JMSContext createContext(int sessionMode) {
            throw jmsRuntime();
        }
    }



    static class NoOpConnection implements Connection {

        @Override
        public Session createSession(boolean transacted, int acknowledgeMode) throws JMSException {
            throw jmsException();
        }

        @Override
        public Session createSession(int sessionMode) throws JMSException {
            throw jmsException();
        }

        @Override
        public Session createSession() throws JMSException {
            throw jmsException();
        }

        @Override
        public String getClientID() throws JMSException {
            throw jmsException();
        }

        @Override
        public void setClientID(String clientID) throws JMSException {
            throw jmsException();
        }

        @Override
        public ConnectionMetaData getMetaData() throws JMSException {
            throw jmsException();
        }

        @Override
        public ExceptionListener getExceptionListener() throws JMSException {
            throw jmsException();
        }

        @Override
        public void setExceptionListener(ExceptionListener listener) throws JMSException {
            throw jmsException();
        }

        @Override
        public void start() throws JMSException {
            throw jmsException();
        }

        @Override
        public void stop() throws JMSException {
            throw jmsException();
        }

        @Override
        public void close() throws JMSException {

        }

        @Override
        public ConnectionConsumer createConnectionConsumer(Destination destination, String messageSelector, ServerSessionPool sessionPool, int maxMessages) throws JMSException {
            throw jmsException();
        }

        @Override
        public ConnectionConsumer createSharedConnectionConsumer(Topic topic, String subscriptionName, String messageSelector, ServerSessionPool sessionPool, int maxMessages) throws JMSException {
            throw jmsException();
        }

        @Override
        public ConnectionConsumer createDurableConnectionConsumer(Topic topic, String subscriptionName, String messageSelector, ServerSessionPool sessionPool, int maxMessages) throws JMSException {
            throw jmsException();
        }

        @Override
        public ConnectionConsumer createSharedDurableConnectionConsumer(Topic topic, String subscriptionName, String messageSelector, ServerSessionPool sessionPool, int maxMessages) throws JMSException {
            throw jmsException();
        }
    }

    static class NoOpSession implements Session {
        @Override
        public BytesMessage createBytesMessage() throws JMSException {
            throw jmsException();
        }

        @Override
        public MapMessage createMapMessage() throws JMSException {
            throw jmsException();
        }

        @Override
        public Message createMessage() throws JMSException {
            throw jmsException();
        }

        @Override
        public ObjectMessage createObjectMessage() throws JMSException {
            throw jmsException();
        }

        @Override
        public ObjectMessage createObjectMessage(Serializable object) throws JMSException {
            throw jmsException();
        }

        @Override
        public StreamMessage createStreamMessage() throws JMSException {
            throw jmsException();
        }

        @Override
        public TextMessage createTextMessage() throws JMSException {
            throw jmsException();
        }

        @Override
        public TextMessage createTextMessage(String text) throws JMSException {
            throw jmsException();
        }

        @Override
        public boolean getTransacted() throws JMSException {
            throw jmsException();
        }

        @Override
        public int getAcknowledgeMode() throws JMSException {
            throw jmsException();
        }

        @Override
        public void commit() throws JMSException {
            throw jmsException();
        }

        @Override
        public void rollback() throws JMSException {
            throw jmsException();
        }

        @Override
        public void close() throws JMSException {
            // exception to the rule
        }

        @Override
        public void recover() throws JMSException {
            throw jmsException();
        }

        @Override
        public MessageListener getMessageListener() throws JMSException {
            throw jmsException();
        }

        @Override
        public void setMessageListener(MessageListener listener) throws JMSException {
            throw jmsException();
        }

        @Override
        public void run() {

        }

        @Override
        public MessageProducer createProducer(Destination destination) throws JMSException {
            throw jmsException();
        }

        @Override
        public MessageConsumer createConsumer(Destination destination) throws JMSException {
            throw jmsException();
        }

        @Override
        public MessageConsumer createConsumer(Destination destination, String messageSelector) throws JMSException {
            throw jmsException();
        }

        @Override
        public MessageConsumer createConsumer(Destination destination, String messageSelector, boolean noLocal) throws JMSException {
            throw jmsException();
        }

        @Override
        public MessageConsumer createSharedConsumer(Topic topic, String sharedSubscriptionName) throws JMSException {
            throw jmsException();
        }

        @Override
        public MessageConsumer createSharedConsumer(Topic topic, String sharedSubscriptionName, String messageSelector) throws JMSException {
            throw jmsException();
        }

        @Override
        public Queue createQueue(String queueName) throws JMSException {
            throw jmsException();
        }

        @Override
        public Topic createTopic(String topicName) throws JMSException {
            throw jmsException();
        }

        @Override
        public TopicSubscriber createDurableSubscriber(Topic topic, String name) throws JMSException {
            throw jmsException();
        }

        @Override
        public TopicSubscriber createDurableSubscriber(Topic topic, String name, String messageSelector, boolean noLocal) throws JMSException {
            throw jmsException();
        }

        @Override
        public MessageConsumer createDurableConsumer(Topic topic, String name) throws JMSException {
            throw jmsException();
        }

        @Override
        public MessageConsumer createDurableConsumer(Topic topic, String name, String messageSelector, boolean noLocal) throws JMSException {
            throw jmsException();
        }

        @Override
        public MessageConsumer createSharedDurableConsumer(Topic topic, String name) throws JMSException {
            throw jmsException();
        }

        @Override
        public MessageConsumer createSharedDurableConsumer(Topic topic, String name, String messageSelector) throws JMSException {
            throw jmsException();
        }

        @Override
        public QueueBrowser createBrowser(Queue queue) throws JMSException {
            throw jmsException();
        }

        @Override
        public QueueBrowser createBrowser(Queue queue, String messageSelector) throws JMSException {
            throw jmsException();
        }

        @Override
        public TemporaryQueue createTemporaryQueue() throws JMSException {
            throw jmsException();
        }

        @Override
        public TemporaryTopic createTemporaryTopic() throws JMSException {
            throw jmsException();
        }

        @Override
        public void unsubscribe(String name) throws JMSException {
            throw jmsException();
        }
    }

    static class NoOpMessage implements Message {

        @Override
        public String getJMSMessageID() throws JMSException {
            throw jmsException();
        }

        @Override
        public void setJMSMessageID(String id) throws JMSException {
            throw jmsException();
        }

        @Override
        public long getJMSTimestamp() throws JMSException {
            throw jmsException();
        }

        @Override
        public void setJMSTimestamp(long timestamp) throws JMSException {
            throw jmsException();
        }

        @Override
        public byte[] getJMSCorrelationIDAsBytes() throws JMSException {
            throw jmsException();
        }

        @Override
        public void setJMSCorrelationIDAsBytes(byte[] correlationID) throws JMSException {
            throw jmsException();
        }

        @Override
        public void setJMSCorrelationID(String correlationID) throws JMSException {
            throw jmsException();
        }

        @Override
        public String getJMSCorrelationID() throws JMSException {
            throw jmsException();
        }

        @Override
        public Destination getJMSReplyTo() throws JMSException {
            throw jmsException();
        }

        @Override
        public void setJMSReplyTo(Destination replyTo) throws JMSException {
            throw jmsException();
        }

        @Override
        public Destination getJMSDestination() throws JMSException {
            throw jmsException();
        }

        @Override
        public void setJMSDestination(Destination destination) throws JMSException {
            throw jmsException();
        }

        @Override
        public int getJMSDeliveryMode() throws JMSException {
            throw jmsException();
        }

        @Override
        public void setJMSDeliveryMode(int deliveryMode) throws JMSException {
            throw jmsException();
        }

        @Override
        public boolean getJMSRedelivered() throws JMSException {
            throw jmsException();
        }

        @Override
        public void setJMSRedelivered(boolean redelivered) throws JMSException {
            throw jmsException();
        }

        @Override
        public String getJMSType() throws JMSException {
            throw jmsException();
        }

        @Override
        public void setJMSType(String type) throws JMSException {
            throw jmsException();
        }

        @Override
        public long getJMSExpiration() throws JMSException {
            throw jmsException();
        }

        @Override
        public void setJMSExpiration(long expiration) throws JMSException {
            throw jmsException();
        }

        @Override
        public long getJMSDeliveryTime() throws JMSException {
            throw jmsException();
        }

        @Override
        public void setJMSDeliveryTime(long deliveryTime) throws JMSException {
            throw jmsException();
        }

        @Override
        public int getJMSPriority() throws JMSException {
            throw jmsException();
        }

        @Override
        public void setJMSPriority(int priority) throws JMSException {
            throw jmsException();
        }

        @Override
        public void clearProperties() throws JMSException {
            throw jmsException();
        }

        @Override
        public boolean propertyExists(String name) throws JMSException {
            throw jmsException();
        }

        @Override
        public boolean getBooleanProperty(String name) throws JMSException {
            throw jmsException();
        }

        @Override
        public byte getByteProperty(String name) throws JMSException {
            throw jmsException();
        }

        @Override
        public short getShortProperty(String name) throws JMSException {
            throw jmsException();
        }

        @Override
        public int getIntProperty(String name) throws JMSException {
            throw jmsException();
        }

        @Override
        public long getLongProperty(String name) throws JMSException {
            throw jmsException();
        }

        @Override
        public float getFloatProperty(String name) throws JMSException {
            throw jmsException();
        }

        @Override
        public double getDoubleProperty(String name) throws JMSException {
            throw jmsException();
        }

        @Override
        public String getStringProperty(String name) throws JMSException {
            throw jmsException();
        }

        @Override
        public Object getObjectProperty(String name) throws JMSException {
            throw jmsException();
        }

        @Override
        public Enumeration getPropertyNames() throws JMSException {
            throw jmsException();
        }

        @Override
        public void setBooleanProperty(String name, boolean value) throws JMSException {
            throw jmsException();
        }

        @Override
        public void setByteProperty(String name, byte value) throws JMSException {
            throw jmsException();
        }

        @Override
        public void setShortProperty(String name, short value) throws JMSException {
            throw jmsException();
        }

        @Override
        public void setIntProperty(String name, int value) throws JMSException {
            throw jmsException();
        }

        @Override
        public void setLongProperty(String name, long value) throws JMSException {
            throw jmsException();
        }

        @Override
        public void setFloatProperty(String name, float value) throws JMSException {
            throw jmsException();
        }

        @Override
        public void setDoubleProperty(String name, double value) throws JMSException {
            throw jmsException();
        }

        @Override
        public void setStringProperty(String name, String value) throws JMSException {
            throw jmsException();
        }

        @Override
        public void setObjectProperty(String name, Object value) throws JMSException {
            throw jmsException();
        }

        @Override
        public void acknowledge() throws JMSException {
            throw jmsException();
        }

        @Override
        public void clearBody() throws JMSException {
            throw jmsException();
        }

        @Override
        public <T> T getBody(Class<T> c) throws JMSException {
            throw jmsException();
        }

        @Override
        public boolean isBodyAssignableTo(Class c) throws JMSException {
            throw jmsException();
        }
    }

    static class NoOpTextMessage extends NoOpMessage implements TextMessage {

        @Override
        public void setText(String string) throws JMSException {
            throw jmsException();
        }

        @Override
        public String getText() throws JMSException {
            throw jmsException();
        }
    }

    static class NoOpDestination implements Destination {

    }

    static class NoOpQueue extends NoOpDestination implements Queue {


        @Override
        public String getQueueName() throws JMSException {
            throw jmsException();
        }
    }

    static class NoOpMessageProducer implements MessageProducer {


        @Override
        public void setDisableMessageID(boolean value) throws JMSException {
            throw jmsException();
        }

        @Override
        public boolean getDisableMessageID() throws JMSException {
            throw jmsException();
        }

        @Override
        public void setDisableMessageTimestamp(boolean value) throws JMSException {
            throw jmsException();
        }

        @Override
        public boolean getDisableMessageTimestamp() throws JMSException {
            throw jmsException();
        }

        @Override
        public void setDeliveryMode(int deliveryMode) throws JMSException {
            throw jmsException();
        }

        @Override
        public int getDeliveryMode() throws JMSException {
            throw jmsException();
        }

        @Override
        public void setPriority(int defaultPriority) throws JMSException {
            throw jmsException();
        }

        @Override
        public int getPriority() throws JMSException {
            throw jmsException();
        }

        @Override
        public void setTimeToLive(long timeToLive) throws JMSException {
            throw jmsException();
        }

        @Override
        public long getTimeToLive() throws JMSException {
            throw jmsException();
        }

        @Override
        public void setDeliveryDelay(long deliveryDelay) throws JMSException {
            throw jmsException();
        }

        @Override
        public long getDeliveryDelay() throws JMSException {
            throw jmsException();
        }

        @Override
        public Destination getDestination() throws JMSException {
            throw jmsException();
        }

        @Override
        public void close() throws JMSException {

        }

        @Override
        public void send(Message message) throws JMSException {
            throw jmsException();
        }

        @Override
        public void send(Message message, int deliveryMode, int priority, long timeToLive) throws JMSException {
            throw jmsException();
        }

        @Override
        public void send(Destination destination, Message message) throws JMSException {
            throw jmsException();
        }

        @Override
        public void send(Destination destination, Message message, int deliveryMode, int priority, long timeToLive) throws JMSException {
            throw jmsException();
        }

        @Override
        public void send(Message message, CompletionListener completionListener) throws JMSException {
            throw jmsException();
        }

        @Override
        public void send(Message message, int deliveryMode, int priority, long timeToLive, CompletionListener completionListener) throws JMSException {
            throw jmsException();
        }

        @Override
        public void send(Destination destination, Message message, CompletionListener completionListener) throws JMSException {
            throw jmsException();
        }

        @Override
        public void send(Destination destination, Message message, int deliveryMode, int priority, long timeToLive, CompletionListener completionListener) throws JMSException {
            throw jmsException();
        }
    }
}
