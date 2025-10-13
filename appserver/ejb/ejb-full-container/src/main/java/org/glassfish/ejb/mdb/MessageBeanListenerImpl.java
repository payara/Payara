/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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
 *
 * Portions Copyright [2017-2021] Payara Foundation and/or affiliates
 */

package org.glassfish.ejb.mdb;

import com.sun.appserv.connectors.internal.api.ResourceHandle;
import fish.payara.nucleus.requesttracing.RequestTracingService;
import fish.payara.notification.requesttracing.RequestTraceSpan;
import fish.payara.nucleus.healthcheck.stuck.StuckThreadsStore;
import fish.payara.notification.requesttracing.EventType;
import java.lang.reflect.Method;
import java.util.UUID;
import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import jakarta.jms.Queue;
import jakarta.jms.Topic;
import org.glassfish.ejb.api.MessageBeanListener;
import org.glassfish.ejb.mdb.MessageBeanContainer.MessageDeliveryType;
import org.glassfish.internal.api.Globals;


/**
 *
 *
 * @author Kenneth Saks
 */
public class MessageBeanListenerImpl implements MessageBeanListener {

    private final MessageBeanContainer container_;
    private ResourceHandle resourceHandle_;
    private final RequestTracingService requestTracing;
    private final StuckThreadsStore stuckThreadsStore;

    MessageBeanListenerImpl(MessageBeanContainer container, 
                            ResourceHandle handle) {
        container_ = container;

        // can be null
        resourceHandle_ = handle;
        
        // get the request tracing service
        requestTracing = Globals.getDefaultHabitat().getService(RequestTracingService.class);
        stuckThreadsStore = Globals.getDefaultHabitat().getService(StuckThreadsStore.class);
    }

    @Override
    public void setResourceHandle(ResourceHandle handle) {
        resourceHandle_ = handle;
    }

    @Override
    public ResourceHandle getResourceHandle() {
        return resourceHandle_;
    }

    @Override
    public void beforeMessageDelivery(Method method, boolean txImported) {
        container_.onEnteringContainer();   //Notify Callflow Agent
        container_.beforeMessageDelivery(method, MessageDeliveryType.Message, txImported, resourceHandle_);
    }
    
    @Override
    public Object deliverMessage(Object[] params) throws Throwable {
        if (stuckThreadsStore != null){
            stuckThreadsStore.registerThread(Thread.currentThread().getId());
        }
        
        RequestTraceSpan span = null;
        if (requestTracing!= null && requestTracing.isRequestTracingEnabled()) {            
            span = new RequestTraceSpan(EventType.TRACE_START, "deliverMdb");
            span.addSpanTag("MDB Class", container_.getEjbDescriptor().getEjbClassName());
            span.addSpanTag("Message Count", Long.toString(container_.getMessageCount()));
            span.addSpanTag("JNDI", container_.getEjbDescriptor().getJndiName());
            try {
                jakarta.jms.Message msg = (jakarta.jms.Message) params[0];
                 span.addSpanTag("JMS Type",msg.getJMSType());               
                 span.addSpanTag("JMS CorrelationID",msg.getJMSCorrelationID());               
                 span.addSpanTag("JMS MessageID",msg.getJMSMessageID());               
                 span.addSpanTag("JMS Destination",getDestinationName(msg.getJMSDestination()));
                 span.addSpanTag("JMS ReplyTo", getDestinationName(msg.getJMSReplyTo()));
                 // check RT conversation ID
                 UUID conversationID = (UUID) msg.getObjectProperty("#BAF-CID");
                 if (conversationID !=  null) {
                     // reset the conversation ID to match the received ID to 
                     // propagate the conversation across the message send
                     requestTracing.setTraceId(conversationID);
                 }
            } catch (ClassCastException cce){}
            requestTracing.startTrace(span);
        }
        try {
            return container_.deliverMessage(params);
        }finally {
            if (requestTracing != null && requestTracing.isRequestTracingEnabled()) {
                requestTracing.endTrace();
            }
            if (stuckThreadsStore != null){
                stuckThreadsStore.deregisterThread(Thread.currentThread().getId());
            }
        }
    }

    @Override
    public void afterMessageDelivery() {
        try {
            container_.afterMessageDelivery(resourceHandle_);
        } finally {
            container_.onLeavingContainer();    //Notify Callflow Agent
        }
    }

    private String getDestinationName(Destination jmsDestination) {
        String result = null;
        try {
        if (jmsDestination instanceof Queue) {
            result = ((Queue) jmsDestination).getQueueName();
        } else if (jmsDestination instanceof Topic) {
            result = ((Topic) jmsDestination).getTopicName();
        }
        }catch (JMSException jmse){}
        return result;
    }

}
