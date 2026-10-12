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
import fish.payara.nucleus.healthcheck.stuck.StuckThreadsStore;
import fish.payara.opentracing.OpenTelemetryService;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Scope;
import java.lang.reflect.Method;
import java.util.logging.Logger;
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

    private static final Logger logger = Logger.getLogger(MessageBeanListenerImpl.class.getName());

    private final MessageBeanContainer container_;
    private ResourceHandle resourceHandle_;
    private final OpenTelemetryService openTelemetryService;
    private final StuckThreadsStore stuckThreadsStore;

    MessageBeanListenerImpl(MessageBeanContainer container,
                            ResourceHandle handle) {
        container_ = container;

        // can be null
        resourceHandle_ = handle;

        // get the OpenTelemetry service
        openTelemetryService = Globals.getDefaultHabitat().getService(OpenTelemetryService.class);
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

        Span span = null;
        Scope scope = null;
        OpenTelemetryService otel = openTelemetryService;
        if (otel != null && otel.isEnabled()) {
            try {
                SpanBuilder sb = otel.getCurrentTracer().spanBuilder("deliverMdb")
                        .setSpanKind(SpanKind.CONSUMER)
                        .setAttribute("MDB Class", container_.getEjbDescriptor().getEjbClassName())
                        .setAttribute("Message Count", container_.getMessageCount())
                        .setAttribute("JNDI", container_.getEjbDescriptor().getJndiName());
                try {
                    if (params != null && params.length > 0) {
                        jakarta.jms.Message msg = (jakarta.jms.Message) params[0];
                        putIfNotNull(sb, "JMS Type", msg.getJMSType());
                        putIfNotNull(sb, "JMS CorrelationID", msg.getJMSCorrelationID());
                        putIfNotNull(sb, "JMS MessageID", msg.getJMSMessageID());
                        putIfNotNull(sb, "JMS Destination", getDestinationName(msg.getJMSDestination()));
                        putIfNotNull(sb, "JMS ReplyTo", getDestinationName(msg.getJMSReplyTo()));
                    }
                } catch (ClassCastException | JMSException ignored) { }
                span = sb.startSpan();
                scope = span.makeCurrent();
            } catch (RuntimeException e) {
                logger.fine("Failed to start OTel span for MDB delivery: " + e.getMessage());
            }
        }
        try {
            return container_.deliverMessage(params);
        }finally {
            if (scope != null) {
                scope.close();
                span.end();
            }
            if (stuckThreadsStore != null){
                stuckThreadsStore.deregisterThread(Thread.currentThread().getId());
            }
        }
    }

    private static void putIfNotNull(SpanBuilder b, String k, String v) {
        if (v != null) b.setAttribute(k, v);
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
