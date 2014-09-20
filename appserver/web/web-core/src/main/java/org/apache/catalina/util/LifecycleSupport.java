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

import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 * Support class to assist in firing LifecycleEvent notifications to
 * registered LifecycleListeners.
 *
 * @author Craig R. McClanahan
 * @version $Id: LifecycleSupport.java,v 1.2 2005/12/08 01:28:17 kchung Exp $
 */

public final class LifecycleSupport {


    // ----------------------------------------------------------- Constructors


    /**
     * Construct a new LifecycleSupport object associated with the specified
     * Lifecycle component.
     *
     * @param lifecycle The Lifecycle component that will be the source
     *  of events that we fire
     */
    public LifecycleSupport(Lifecycle lifecycle) {
        super();
        this.lifecycle = lifecycle;
    }


    // ----------------------------------------------------- Instance Variables


    /**
     * The source component for lifecycle events that we will fire.
     */
    private Lifecycle lifecycle = null;


    /**
     * The list of registered LifecycleListeners for event notifications.
     */
    private List<LifecycleListener> listeners =
        new ArrayList<LifecycleListener>();


    // --------------------------------------------------------- Public Methods


    /**
     * Add a lifecycle event listener to this component.
     *
     * @param listener The listener to add
     */
    public void addLifecycleListener(LifecycleListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }


    /**
     * Gets the (possibly empty) list of lifecycle listeners associated
     * with this LifecycleSupport instance.
     */
    public List<LifecycleListener> findLifecycleListeners() {
        return listeners;
    }


    /**
     * Notify all lifecycle event listeners that a particular event has
     * occurred for this Container.  The default implementation performs
     * this notification synchronously using the calling thread.
     *
     * @param type Event type
     * @param data Event data
     */
    public void fireLifecycleEvent(String type, Object data)
            throws LifecycleException {


        LifecycleListener[] listenersArray = null;

        synchronized (listeners) {
            if (listeners.isEmpty()) {
                return;
            }
            listenersArray = listeners.toArray(
                    new LifecycleListener[listeners.size()]);
        }

        LifecycleEvent event = new LifecycleEvent(lifecycle, type, data);

        for (int i = 0; i < listenersArray.length; i++) {
            listenersArray[i].lifecycleEvent(event);
        }

    }


    /**
     * Remove a lifecycle event listener from this component.
     *
     * @param listener The listener to remove
     */
    public void removeLifecycleListener(LifecycleListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }


    /**
     * Removes any lifecycle event listeners from this LifecycleSupport
     * instance.
     */
    public void removeLifecycleListeners() {
        listeners.clear();
    }

}
