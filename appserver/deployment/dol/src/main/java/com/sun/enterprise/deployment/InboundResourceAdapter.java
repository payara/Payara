/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.deployment;

import org.glassfish.deployment.common.Descriptor;

import java.util.Set;

/**
 * This class argueably could be rolled up to the
 * ConnectorDescriptor class.  However, it is easier
 * to keep track of the changes in inbound ra element
 * as well as encapsulate the concept of inbound ra.
 *
 * <!ELEMENT inbound-resourceadapter (messageadapter?)>
 * <!ELEMENT messageadapter (messagelistener+)>
 *
 * @author	Qingqing Ouyang
 */
public class InboundResourceAdapter extends Descriptor
{
    private Set messageListeners;

    public InboundResourceAdapter () 
    {
        messageListeners = new OrderedSet();
    }
    
    public Set
    getMessageListeners()
    {
        return messageListeners;
    }
    
    public void
    addMessageListener (MessageListener listener)
    {
        messageListeners.add(listener);
    }

    public void 
    removeMessageListener (MessageListener listener) 
    {
	messageListeners.remove(listener);
    }

    public boolean hasMessageListenerType(String msgListenerType){
        for(Object messageListenerObject : messageListeners){
            MessageListener ml = (MessageListener) messageListenerObject;
            if(ml.getMessageListenerType().equals(msgListenerType)){
                return true;
            }
        }
        return false;
    }

    public MessageListener getMessageListener(String msgListenerType){
        for(Object messageListenerObject : messageListeners){
            MessageListener ml = (MessageListener) messageListenerObject;
            if(ml.getMessageListenerType().equals(msgListenerType)){
                return ml;
            }
        }
        return null;
    }
}
