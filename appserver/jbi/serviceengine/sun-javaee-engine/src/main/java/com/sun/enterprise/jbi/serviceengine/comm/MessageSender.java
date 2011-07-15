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
import com.sun.enterprise.jbi.serviceengine.work.OneWork;
import java.util.logging.Level;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.MessageExchange;


/**
 * An instance of this class is used to send a message to 
 * NMR. Messages can be send either using the caller's thread
 * or in a new thread. 
 *
 * @author Binod PG
 * @see OneWork
 */
public class MessageSender extends OneWork {

    /**
     * Users of this class execute send() method to send
     * the message to NMR. Note that execute() method will 
     * finally call doWork().
     */
    public void send() {
        execute();
    }

    /**
     * Actually send the message to NMR.
     */
    public void doWork() {
        try {
            if (logger.isLoggable(Level.FINEST)) {
                logger.log(Level.FINEST,
                "Sending the message " + getMessageExchange() + "to NMR");
            }
    
            boolean sendSync = shouldSendSync(getMessageExchange());
            if(sendSync) {
                getDeliveryChannel().sendSync(getMessageExchange());
            } else {
                getDeliveryChannel().send(getMessageExchange());
            }
            if (getMessageExchange().getStatus()== ExchangeStatus.ERROR &&
                    getMessageExchange().getRole().equals(MessageExchange.Role.CONSUMER))
                setException(getMessageExchange().getError());
            if (logger.isLoggable(Level.FINEST)) {
                logger.log(Level.FINEST, 
                "Sent message " + getMessageExchange() + "to NMR");
            }
        } catch (MessagingException me) {
            logger.log(Level.WARNING, "Error sending message" + me.getMessage());
            setException(me);
        }
    }
    
    private boolean shouldSendSync(MessageExchange exchange) {

        if(exchange.getRole().equals(MessageExchange.Role.CONSUMER) &&
                exchange.getStatus().equals(ExchangeStatus.ACTIVE)) {
            return true;
        }

        return false;
    }
}
