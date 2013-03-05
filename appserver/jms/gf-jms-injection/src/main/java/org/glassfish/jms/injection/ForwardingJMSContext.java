/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
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

package org.glassfish.jms.injection;

import java.io.Serializable;
import java.util.Map;
import javax.jms.*;
import com.sun.enterprise.util.LocalStringManagerImpl;

// Delegate all business methods to JMSContext API
public abstract class ForwardingJMSContext implements JMSContext {
    private final static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(ForwardingJMSContext.class);

    protected abstract JMSContext delegate();

    @Override
    public JMSContext createContext(int sessionMode) {
        return delegate().createContext(sessionMode);
    }

    @Override
    public JMSProducer createProducer() {
        return delegate().createProducer();
    }

    @Override
    public String getClientID() {
        return delegate().getClientID();
    }

    @Override
    public void setClientID(String clientID) {
        throw getUnsupportedException();
    }

    @Override
    public ConnectionMetaData getMetaData() {
        return delegate().getMetaData();
    }

    @Override
    public ExceptionListener getExceptionListener() {
        return null;
    }

    @Override
    public void setExceptionListener(ExceptionListener listener) {
        throw getUnsupportedException();
    }

    @Override
    public void start() {
        throw getUnsupportedException();
    }

    @Override
    public void stop() {
        throw getUnsupportedException();
    }

    @Override
    public void setAutoStart(boolean autoStart) {
        throw getUnsupportedException();
    }

    @Override
    public boolean getAutoStart() {
        return true;
    }

    @Override
    public void close() {
        throw getUnsupportedException();
    }

    @Override
    public BytesMessage createBytesMessage() {
        return delegate().createBytesMessage();
    }

    @Override
    public MapMessage createMapMessage() {
        return delegate().createMapMessage();
    }

    @Override
    public Message createMessage() {
        return delegate().createMessage();
    }

    @Override
    public ObjectMessage createObjectMessage() {
        return delegate().createObjectMessage();
    }

    @Override
    public ObjectMessage createObjectMessage(Serializable object) {
        return delegate().createObjectMessage(object);
    }

    @Override
    public StreamMessage createStreamMessage() {
        return delegate().createStreamMessage();
    }

    @Override
    public TextMessage createTextMessage() {
        return delegate().createTextMessage();
    }

    @Override
    public TextMessage createTextMessage(String text) {
        return delegate().createTextMessage(text);
    }

    @Override
    public boolean getTransacted() {
        return delegate().getTransacted();
    }

    @Override
    public int getSessionMode() {
        return delegate().getSessionMode();
    }

    @Override
    public void commit() {
        throw getUnsupportedException();
    }

    @Override
    public void rollback() {
        throw getUnsupportedException();
    }

    @Override
    public void recover() {
        throw getUnsupportedException();
    }

    @Override
    public JMSConsumer createConsumer(Destination destination) {
        return delegate().createConsumer(destination);
    }

    @Override
    public JMSConsumer createConsumer(Destination destination, String messageSelector) {
        return delegate().createConsumer(destination, messageSelector);
    }

    @Override
    public JMSConsumer createConsumer(Destination destination, String messageSelector, boolean noLocal) {
        return delegate().createConsumer(destination, messageSelector, noLocal);
    }

    @Override
    public Queue createQueue(String queueName) {
        return delegate().createQueue(queueName);
    }

    @Override
    public Topic createTopic(String topicName) {
        return delegate().createTopic(topicName);
    }

    @Override
    public JMSConsumer createDurableConsumer(Topic topic, String name) {
        return delegate().createDurableConsumer(topic, name);
    }

    @Override
    public JMSConsumer createDurableConsumer(Topic topic, String name, String messageSelector, boolean noLocal) {
        return delegate().createDurableConsumer(topic, name, messageSelector, noLocal);
    }

    @Override
    public JMSConsumer createSharedConsumer(Topic topic, String sharedSubscriptionName) {
        return delegate().createSharedConsumer(topic, sharedSubscriptionName);
    }

    @Override
    public JMSConsumer createSharedConsumer(Topic topic, String sharedSubscriptionName, String messageSelector) {
        return delegate().createSharedConsumer(topic, sharedSubscriptionName, messageSelector);
    }

    @Override
    public JMSConsumer createSharedDurableConsumer(Topic topic, String name) {
        return delegate().createSharedDurableConsumer(topic, name);
    }

    @Override
    public JMSConsumer createSharedDurableConsumer(Topic topic, String name, String messageSelector) {
        return delegate().createSharedDurableConsumer(topic, name, messageSelector);
    }

    @Override
    public QueueBrowser createBrowser(Queue queue) {
        return delegate().createBrowser(queue);
    }

    @Override
    public QueueBrowser createBrowser(Queue queue, String messageSelector) {
        return delegate().createBrowser(queue, messageSelector);
    }

    @Override
    public TemporaryQueue createTemporaryQueue() {
        return delegate().createTemporaryQueue();
    }

    @Override
    public TemporaryTopic createTemporaryTopic() {
        return delegate().createTemporaryTopic();
    }

    @Override
    public void unsubscribe(String name) {
        delegate().unsubscribe(name);
    }

    @Override
    public void acknowledge() {
        throw getUnsupportedException();
    }

    private IllegalStateRuntimeException getUnsupportedException() {
        return new IllegalStateRuntimeException(localStrings.getLocalString("JMSContext.injection.not.supported", 
                                      "This method is not permitted on a container-managed (injected) JMSContext."));
    }
}
