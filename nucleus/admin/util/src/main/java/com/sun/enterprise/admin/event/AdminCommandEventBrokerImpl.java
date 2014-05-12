/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2014 Oracle and/or its affiliates. All rights reserved.
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
package com.sun.enterprise.admin.event;

import com.sun.enterprise.admin.util.AdminLoggerInfo;
import com.sun.enterprise.util.StringUtils;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.glassfish.api.admin.AdminCommandEventBroker;

/**
 *
 * @author mmares
 */
public class AdminCommandEventBrokerImpl<T> implements AdminCommandEventBroker<T> {
    
    private static class ListenerGroup {
        
        private final Pattern pattern;
        private final List<AdminCommandListener> listeners = new ArrayList<AdminCommandListener>(1);

        private ListenerGroup(String pattern) {
            this.pattern = Pattern.compile(pattern);
        }
        
        public String getOriginalPattern() {
            return pattern.pattern();
        }
        
        public boolean matches(CharSequence name) {
            return pattern.matcher(name).matches();
        }
        
        public Iterator<AdminCommandListener> listeners() {
            return listeners.iterator();
        }
        
        public boolean add(AdminCommandListener listener) {
            return listeners.add(listener);
        }
        
        public boolean remove(AdminCommandListener listener) {
            return listeners.remove(listener);
        }
    }
    
    private final List<ListenerGroup> listenerGroups = new ArrayList<ListenerGroup>();
    private static final Logger logger = AdminLoggerInfo.getLogger();

    public AdminCommandEventBrokerImpl() {
    }
    
    @Override
    public void fireEvent(String name, Object event) {
        if (event == null) {
            return;
        }
        if (name == null) {
            throw new IllegalArgumentException("Argument name must be defined");
        }
        IdentityHashMap<AdminCommandListener, AdminCommandListener> deduplicated 
                = new IdentityHashMap<AdminCommandListener, AdminCommandListener>();
        synchronized (this) {
            for (ListenerGroup listenerGroup : listenerGroups) {
                if (listenerGroup.matches(name)) {
                    for (AdminCommandListener listener : listenerGroup.listeners) {
                        deduplicated.put(listener, listener);
                    }
                }
            }
        }//Call all listeners
        for (AdminCommandListener listener : deduplicated.keySet()) {
            try {
                listener.onAdminCommandEvent(name, event);
            } catch (Exception ex) {
                logger.log(Level.WARNING, AdminLoggerInfo.mExceptionFromEventListener, ex);
            }
        }
    }

    @Override
    public synchronized void fireEvent(Object event) {
        if (event == null) {
            return;
        }
        fireEvent(event.getClass().getName(), event);
    }

    @Override
    public synchronized void registerListener(String regexpForName, AdminCommandListener<T> listener) {
        if (regexpForName == null) {
            throw new IllegalArgumentException("Argument regexpForName must be defined");
        }
        if (listener == null) {
            return;
        }
        ListenerGroup lgrp = null;
        for (ListenerGroup listenerGroup : listenerGroups) {
            if (regexpForName.equals(listenerGroup.getOriginalPattern())) {
                lgrp = listenerGroup;
                break;
            }
        }
        if (lgrp == null) {
            lgrp = new ListenerGroup(regexpForName);
            listenerGroups.add(lgrp);
        }
        lgrp.add(listener);
        fireEvent(BrokerListenerRegEvent.EVENT_NAME_LISTENER_REG, 
                new BrokerListenerRegEvent(this, listener));
    }
    
    @Override
    public synchronized boolean listening(String eventName) {
        if (eventName == null) {
            for (ListenerGroup listenerGroup : listenerGroups) {
                if (!listenerGroup.listeners.isEmpty()) {
                    return true;
                }
            }
        } else {
            for (ListenerGroup listenerGroup : listenerGroups) {
                if (listenerGroup.matches(eventName) && !listenerGroup.listeners.isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public synchronized void unregisterListener(AdminCommandListener listener) {
        boolean removed = false;
        for (ListenerGroup listenerGroup : listenerGroups) {
            if (listenerGroup.remove(listener)) {
                removed = true;
            }
            //No break. Can be registered for more names
        }
        if (removed) {
            fireEvent(BrokerListenerRegEvent.EVENT_NAME_LISTENER_UNREG, 
                new BrokerListenerRegEvent(this, listener));
        }
    }
    
    @Override
    public EventBrokerUtils getUtils() {
        return new EventBrokerUtils() {

            @Override
            public void sendMessage(String message) {
                if (StringUtils.ok(message)) {
                    fireEvent(USER_MESSAGE_NAME, message);
                }
            }
            
        };
    }
    
}
