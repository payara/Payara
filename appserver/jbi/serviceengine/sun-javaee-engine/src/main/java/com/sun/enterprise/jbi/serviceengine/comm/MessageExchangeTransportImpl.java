/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

/*
 * MessageExchangeTransportImpl.java
 *
 * Created on November 16, 2006, 7:29 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.sun.enterprise.jbi.serviceengine.comm;

import com.sun.enterprise.jbi.serviceengine.ServiceEngineException;
import com.sun.enterprise.jbi.serviceengine.handlers.JBIHandler;
import com.sun.enterprise.jbi.serviceengine.handlers.JBIHandlerFactory;
import com.sun.logging.LogDomains;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;

/**
 *
 * @author bhavanishankar@dev.java.net
 */
public abstract class MessageExchangeTransportImpl
        implements MessageExchangeTransport {

    MessageExchange me;
    protected NormalizedMessage msg = null;
    protected static final Logger logger =
            LogDomains.getLogger(MessageExchangeTransportImpl.class, LogDomains.SERVER_LOGGER);

    protected MessageExchangeTransportImpl(MessageExchange me) {
        this.me = me;
    }

    public void sendStatus(ExchangeStatus status) {
        try {
            me.setStatus(status);
            MessageSender messageSender = new MessageSender();
            messageSender.setMessageExchange(me);
            messageSender.send();
        } catch(Exception ex) {
            // Can't do much here.
            logger.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    public void sendError(Exception exception) {
        try {
            logger.log(Level.SEVERE, 
                    "Sending error to the consumer - " + exception);
            if(exception != null) {
                me.setError(exception);
            }
            me.setStatus(ExchangeStatus.ERROR);
            send();
        } catch(Exception ex) {
            // Can't do much here.
            logger.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    public MessageExchange getMessageExchange() {
        return me;
    }

    public NormalizedMessage getMessage() {
        return msg;
    }

    /**
     * @return Properties of Normalized Message msg.
     */
    public Map<String, Object> getMessageProperties() {
        Map<String, Object> props = new HashMap<String, Object>();
        if (msg != null) {
            Set propNames = msg.getPropertyNames();
            if (propNames != null)
                for (Object o : propNames) {
                    if (o instanceof String) {
                        String propName = (String) o;
                        props.put(propName, msg.getProperty(propName));
                    }
                }
        }
        return props;
    }

    /**
     * @param props Properties to be set in Normalized Message msg.
     */
    public void setMessageProperties(Map<String, Object> props) {
        if ((props != null) && (msg != null)) {
            Set<String> propNames = props.keySet();
            if (propNames != null) {
                for (String propName : propNames) {
                    msg.setProperty(propName, props.get(propName));
                }
            }
        }
    }

    protected void send() throws Exception {
        preSend();
        MessageSender messageSender = new MessageSender();
        messageSender.setMessageExchange(me);
        messageSender.send();
        Exception exception = messageSender.getException();
        if(exception != null)
            throw exception;
    }

    public Exception receiveError() {
        preReceive();
        return me.getError();
    }

    protected String extractFaultCode(String completeFaultCode) {
        StringTokenizer tokenizer = new StringTokenizer(completeFaultCode, ":");
        if(tokenizer.countTokens() > 1) tokenizer.nextToken();
        return tokenizer.nextToken();
    }

    protected void preReceive() {
        invokeHandlersForOutbound();
    }

    protected void preSend() throws Exception {
        invokeHandlersForInbound();
    }

    // Before calling the handlers ensure that the msg is set in this 
    // MessageExchangeTransport
    protected void invokeHandlersForInbound() throws ServiceEngineException {
        for (JBIHandler handler : JBIHandlerFactory.getInstance().getHandlers())
            try {
            handler.handleInbound(this);
        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            throw new ServiceEngineException(e);
        }
    }

    protected void invokeHandlersForOutbound() {
        for (JBIHandler handler : JBIHandlerFactory.getInstance().getHandlers())
            try {
            handler.handleOutbound(this);
        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            // throw new ServiceEngineException(e);
        }
    }
}
