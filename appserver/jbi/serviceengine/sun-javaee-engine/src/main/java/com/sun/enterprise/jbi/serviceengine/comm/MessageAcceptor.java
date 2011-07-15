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

package com.sun.enterprise.jbi.serviceengine.comm;

import com.sun.enterprise.jbi.serviceengine.core.JavaEEServiceEngineContext;
import java.util.HashMap;
import java.util.logging.Level;
import javax.jbi.messaging.ExchangeStatus;

import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import com.sun.enterprise.jbi.serviceengine.work.OneWork;

/**
 * Acceptor object that continuously receives messages from
 * NMR. There is one MessageAcceptor object per WorkManager.
 *
 * MessageAcceptor also keeps a house keeping datastructure
 * that holds information about all threads that are waiting
 * for reply from on NMR for their two-way message exchange.
 *
 * @authod Binod PG
 */
public class MessageAcceptor extends OneWork {
    
    private HashMap<String,MessageReceiver> receivers = new HashMap();
    private boolean released = false;
    
    /**
     * Start the acceptor thread. Note that execute() inturn call
     * doWork method, where bulk of logic is present.
     */
    public void startAccepting() {
        execute();
    }
    
    /**
     * Add a MessageReceiver object that waits for a reply from
     * from NMR on their 2-way message exchange.
     *
     * @param receiver MessageReceiver instance.
     */
    public void register(MessageReceiver receiver) {
        String id = receiver.getMessageExchange().getExchangeId();
        logger.log(Level.FINER, "Adding recever for " + id);
        synchronized (receivers) {
            receivers.put(id, receiver);
        }
    }
    
    /**
     * Release the thread from accepting. This method doesnt interrupt
     * the thread. It is just a soft release applicable only from the
     * next iteration of acceptor thread.
     */
    public void release() {
        released = true;
    }
    
    /**
     * Actual work happens in this method. DeliveryChannel available
     * from the super class. If there is any MessageReceiver waiting
     * for this message, then the MessageExchange is made avalable to that
     * MessageReceiver. If no MessageReceiver is interested in this MEP,
     * then a new MessageProcessor will process this message.
     * In the latter case, the message is for a 109 webservice deployed
     * in appserver.
     */
    public void doWork() {
        while (true) {
            try {
                
                MessageExchange me = getDeliveryChannel().accept();
                if (released) {
                    break;
                }
                
                if(me != null) {
                    
                    if(ignoreMessageExchange(me)) {
                        continue;
                    }
                    
                    String id = me.getExchangeId();
                    
                    // The full block is not synchronized since,
                    // 1. Id will always be unique
                    // 2. Service engine will register the receiver
                    //    before sending and hence we wont miss any.
                    if (receivers.containsKey(id)) {
                        synchronized(receivers) {
                            MessageReceiver receiver = receivers.remove(id);
                            receiver.setMessageExchange(me);
                            if (logger.isLoggable(Level.FINE)) {
                                logger.log(Level.FINE,
                                        "Releasing MessageReceiver:" + id + ",MEP :" + me);
                            }
                            receiver.release();
                        }
                    } else {
                        MessageProcessor processor =
                                JavaEEServiceEngineContext.getInstance().
                                getBridge().getMessageProcessor(me);
                        processor.setUseCurrentThread(false);
                        processor.setMessageExchange(me);
                        if (logger.isLoggable(Level.FINE)) {
                            logger.log(Level.FINE,
                                    "Spawning MessageProcessorfor MEP :" + me);
                        }
                        processor.process();
                    }
                }
            } catch (MessagingException ie) {
                //Someone has interrupted the acceptor. Gracefully comeout.
                logger.log(Level.FINE, "Stopping the acceptor thread");
                break;
            }
        }
    }
    
    private boolean ignoreMessageExchange(MessageExchange me) {
        
        if(me.getRole().equals(MessageExchange.Role.PROVIDER)) {
            
            if(me.getStatus().equals(ExchangeStatus.ACTIVE)) {
                return false;
            }
            
            if(me.getStatus().equals(ExchangeStatus.DONE)) {
                if(logger.isLoggable(Level.FINE)) {
                    logger.fine("Message Exchange Completed " + me);
                }
                return true;
            }
            
            if(me.getStatus().equals(ExchangeStatus.ERROR)) {
                logger.warning("JavaEE Service Engine received unsupported Message Exchange " + me);
                return true;
            }
        }
        
        return false;
    }
}

