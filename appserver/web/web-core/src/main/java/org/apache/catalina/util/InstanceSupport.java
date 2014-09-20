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
 *
 *
 * This file incorporates work covered by the following copyright and
 * permission notice:
 *
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.catalina.util;


import org.apache.catalina.InstanceEvent;
import org.apache.catalina.InstanceListener;
import org.apache.catalina.Wrapper;

import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
// END SJSAS 6374619

/**
 * Support class to assist in firing InstanceEvent notifications to
 * registered InstanceListeners.
 *
 * @author Craig R. McClanahan
 * @version $Id: InstanceSupport.java,v 1.3 2006/03/09 20:38:05 jfarcand Exp $
 */

public final class InstanceSupport {


    // ----------------------------------------------------------- Constructors


    /**
     * Construct a new InstanceSupport object associated with the specified
     * Instance component.
     *
     * @param lifecycle The Instance component that will be the source
     *  of events that we fire
     */
    public InstanceSupport(Wrapper wrapper) {

        super();
        this.wrapper = wrapper;

    }


    // ----------------------------------------------------- Instance Variables
    // START SJSAS 6374619
    private ReadWriteLock listenersLock = new ReentrantReadWriteLock();
    private Lock listenersReadLock = listenersLock.readLock();
    private Lock listenersWriteLock = listenersLock.writeLock();
    // END SJSAS 6374619

    /**
     * The set of registered InstanceListeners for event notifications.
     */
    private InstanceListener listeners[] = new InstanceListener[0];


    /**
     * The source component for instance events that we will fire.
     */
    private Wrapper wrapper = null;


    // ------------------------------------------------------------- Properties


    /**
     * Return the Wrapper with which we are associated.
     */
    public Wrapper getWrapper() {

        return (this.wrapper);

    }


    // --------------------------------------------------------- Public Methods


    /**
     * Add a lifecycle event listener to this component.
     *
     * @param listener The listener to add
     */
    public void addInstanceListener(InstanceListener listener) {
        /* SJSAS 6374619
        synchronized (listeners) {
          InstanceListener results[] =
            new InstanceListener[listeners.length + 1];
          for (int i = 0; i < listeners.length; i++)
              results[i] = listeners[i];
          results[listeners.length] = listener;
          listeners = results;
        }
        */
        // START SJSAS 6374619
        listenersWriteLock.lock();
        try {
            InstanceListener results[] =
                new InstanceListener[listeners.length + 1];
            for (int i = 0; i < listeners.length; i++)
              results[i] = listeners[i];
            results[listeners.length] = listener;
            listeners = results;
        } finally {
            listenersWriteLock.unlock();
        }
        // END SJSAS 6374619
    }


    /**
     * Notify all lifecycle event listeners that a particular event has
     * occurred for this Container.  The default implementation performs
     * this notification synchronously using the calling thread.
     *
     * @param type Event type
     * @param filter The relevant Filter for this event
     */
    public void fireInstanceEvent(InstanceEvent.EventType type, Filter filter) {

        if (listeners.length == 0)
            return;

        InstanceEvent event = new InstanceEvent(wrapper, filter, type);
        InstanceListener interested[] = null;
        /* SJSAS XXX
        synchronized (listeners) {
            interested = (InstanceListener[]) listeners.clone();
        }
        for (int i = 0; i < interested.length; i++)
            interested[i].instanceEvent(event);
        */
        // START SJSAS XXX
        listenersReadLock.lock();
        try {
            for (int i = 0; i < listeners.length; i++)
                listeners[i].instanceEvent(event);
        } finally {
            listenersReadLock.unlock();
        }
    }


    /**
     * Notify all lifecycle event listeners that a particular event has
     * occurred for this Container.  The default implementation performs
     * this notification synchronously using the calling thread.
     *
     * @param type Event type
     * @param filter The relevant Filter for this event
     * @param exception Exception that occurred
     */
    public void fireInstanceEvent(InstanceEvent.EventType type, Filter filter,
                                  Throwable exception) {

        if (listeners.length == 0)
            return;

        InstanceEvent event = new InstanceEvent(wrapper, filter, type,
                                                exception);
        /* SJSAS 6374619
        InstanceListener interested[] = null;
        synchronized (listeners) {
            interested = (InstanceListener[]) listeners.clone();
        }
        for (int i = 0; i < interested.length; i++)
            interested[i].instanceEvent(event);
        */
        // START SJSAS 6374619
        listenersReadLock.lock();
        try {
            for (int i = 0; i < listeners.length; i++)
                listeners[i].instanceEvent(event);
        } finally {
            listenersReadLock.unlock();
        }
        // END SJSAS 6374619

    }


    /**
     * Notify all lifecycle event listeners that a particular event has
     * occurred for this Container.  The default implementation performs
     * this notification synchronously using the calling thread.
     *
     * @param type Event type
     * @param filter The relevant Filter for this event
     * @param request The servlet request we are processing
     * @param response The servlet response we are processing
     */
    public void fireInstanceEvent(InstanceEvent.EventType type, Filter filter,
                                  ServletRequest request,
                                  ServletResponse response) {

        if (listeners.length == 0)
            return;

        InstanceEvent event = new InstanceEvent(wrapper, filter, type,
                                                request, response);
        /* SJSAS 6374619
        InstanceListener interested[] = null;
        synchronized (listeners) {
            interested = (InstanceListener[]) listeners.clone();
        }
        for (int i = 0; i < interested.length; i++)
            interested[i].instanceEvent(event);
        */
        // START SJSAS 6374619
        listenersReadLock.lock();
        try {
            for (int i = 0; i < listeners.length; i++)
                listeners[i].instanceEvent(event);
        } finally {
            listenersReadLock.unlock();
        }
        // END SJSAS 6374619
    }


    /**
     * Notify all lifecycle event listeners that a particular event has
     * occurred for this Container.  The default implementation performs
     * this notification synchronously using the calling thread.
     *
     * @param type Event type
     * @param filter The relevant Filter for this event
     * @param request The servlet request we are processing
     * @param response The servlet response we are processing
     * @param exception Exception that occurred
     */
    public void fireInstanceEvent(InstanceEvent.EventType type, Filter filter,
                                  ServletRequest request,
                                  ServletResponse response,
                                  Throwable exception) {

        if (listeners.length == 0)
            return;

        InstanceEvent event = new InstanceEvent(wrapper, filter, type,
                                                request, response, exception);
        /* SJSAS 6374619        
        InstanceListener interested[] = null;
        synchronized (listeners) {
            interested = (InstanceListener[]) listeners.clone();
        }
        for (int i = 0; i < interested.length; i++)
            interested[i].instanceEvent(event);
        */
        // START SJSAS 6374619
        listenersReadLock.lock();
        try {
            for (int i = 0; i < listeners.length; i++)
                listeners[i].instanceEvent(event);
        } finally {
            listenersReadLock.unlock();
        }
        // END SJSAS 6374619
    }


    /**
     * Notify all lifecycle event listeners that a particular event has
     * occurred for this Container.  The default implementation performs
     * this notification synchronously using the calling thread.
     *
     * @param type Event type
     * @param servlet The relevant Servlet for this event
     */
    public void fireInstanceEvent(InstanceEvent.EventType type, Servlet servlet) {

        if (listeners.length == 0)
            return;

        InstanceEvent event = new InstanceEvent(wrapper, servlet, type);
        /* SJSAS 6374619
        InstanceListener interested[] = null;
        synchronized (listeners) {
            interested = (InstanceListener[]) listeners.clone();
        }
        for (int i = 0; i < interested.length; i++)
            interested[i].instanceEvent(event);
        */
        // START SJSAS 6374619
        listenersReadLock.lock();
        try {
            for (int i = 0; i < listeners.length; i++)
                listeners[i].instanceEvent(event);
        } finally {
            listenersReadLock.unlock();
        }
        // END SJSAS 6374619
    }


    /**
     * Notify all lifecycle event listeners that a particular event has
     * occurred for this Container.  The default implementation performs
     * this notification synchronously using the calling thread.
     *
     * @param type Event type
     * @param servlet The relevant Servlet for this event
     * @param exception Exception that occurred
     */
    public void fireInstanceEvent(InstanceEvent.EventType type, Servlet servlet,
                                  Throwable exception) {

        if (listeners.length == 0)
            return;

        InstanceEvent event = new InstanceEvent(wrapper, servlet, type,
                                                exception);
        /* SJSAS 6374619
        InstanceListener interested[] = null;
        synchronized (listeners) {
            interested = (InstanceListener[]) listeners.clone();
        }
        for (int i = 0; i < interested.length; i++)
            interested[i].instanceEvent(event);
        */
        // START SJSAS 6374619
        listenersReadLock.lock();
        try {
            for (int i = 0; i < listeners.length; i++)
                listeners[i].instanceEvent(event);
        } finally {
            listenersReadLock.unlock();
        }
        // END SJSAS 6374619
    }


    /**
     * Notify all lifecycle event listeners that a particular event has
     * occurred for this Container.  The default implementation performs
     * this notification synchronously using the calling thread.
     *
     * @param type Event type
     * @param servlet The relevant Servlet for this event
     * @param request The servlet request we are processing
     * @param response The servlet response we are processing
     */
    public void fireInstanceEvent(InstanceEvent.EventType type, Servlet servlet,
                                  ServletRequest request,
                                  ServletResponse response) {

        if (listeners.length == 0)
            return;

        InstanceEvent event = new InstanceEvent(wrapper, servlet, type,
                                                request, response);
        /* SJSAS 6374619
        InstanceListener interested[] = null;
        synchronized (listeners) {
            interested = (InstanceListener[]) listeners.clone();
        }
        for (int i = 0; i < interested.length; i++)
            interested[i].instanceEvent(event);
        */
        // START SJSAS 6374619
        listenersReadLock.lock();
        try {
            for (int i = 0; i < listeners.length; i++)
                listeners[i].instanceEvent(event);
        } finally {
            listenersReadLock.unlock();
        }
        // END SJSAS 6374619
    }


    /**
     * Notify all lifecycle event listeners that a particular event has
     * occurred for this Container.  The default implementation performs
     * this notification synchronously using the calling thread.
     *
     * @param type Event type
     * @param servlet The relevant Servlet for this event
     * @param request The servlet request we are processing
     * @param response The servlet response we are processing
     * @param exception Exception that occurred
     */
    public void fireInstanceEvent(InstanceEvent.EventType type, Servlet servlet,
                                  ServletRequest request,
                                  ServletResponse response,
                                  Throwable exception) {

        if (listeners.length == 0)
            return;

        InstanceEvent event = new InstanceEvent(wrapper, servlet, type,
                                                request, response, exception);
        /* SJSAS 6374619
        InstanceListener interested[] = null;
        synchronized (listeners) {
            interested = (InstanceListener[]) listeners.clone();
        }
        for (int i = 0; i < interested.length; i++)
            interested[i].instanceEvent(event);
        */
        // START SJSAS 6374619
        listenersReadLock.lock();
        try {
            for (int i = 0; i < listeners.length; i++)
                listeners[i].instanceEvent(event);
        } finally {
            listenersReadLock.unlock();
        }
        // END SJSAS 6374619
    }


    /**
     * Remove a lifecycle event listener from this component.
     *
     * @param listener The listener to remove
     */
    public void removeInstanceListener(InstanceListener listener) {

        /* SJSAS 6374619
        synchronized (listeners) {
            int n = -1;
            for (int i = 0; i < listeners.length; i++) {
                if (listeners[i] == listener) {
                    n = i;
                    break;
                }
            }
            if (n < 0)
                return;
            InstanceListener results[] =
              new InstanceListener[listeners.length - 1];
            int j = 0;
            for (int i = 0; i < listeners.length; i++) {
                if (i != n)
                    results[j++] = listeners[i];
            }
            listeners = results;
        }
        */
        // START SJSAS 6374619
        listenersWriteLock.lock();
        try {
            int n = -1;
            for (int i = 0; i < listeners.length; i++) {
                if (listeners[i] == listener) {
                    n = i;
                    break;
                }
            }
            if (n < 0)
                return;
            InstanceListener results[] =
              new InstanceListener[listeners.length - 1];
            int j = 0;
            for (int i = 0; i < listeners.length; i++) {
                if (i != n)
                    results[j++] = listeners[i];
            }
            listeners = results;
        } finally {
            listenersWriteLock.unlock();
        }
        // END SJSAS 6374619
    }


}
