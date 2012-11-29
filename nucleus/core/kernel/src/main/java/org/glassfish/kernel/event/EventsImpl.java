/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.kernel.event;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import org.glassfish.api.event.EventListener;
import org.glassfish.api.event.EventListener.Event;
import org.glassfish.api.event.EventTypes;
import org.glassfish.api.event.Events;
import org.glassfish.api.event.RestrictTo;
import org.glassfish.deployment.common.DeploymentException;
import org.glassfish.kernel.KernelLoggerInfo;
import org.jvnet.hk2.annotations.Service;

/**
 * Simple implementation of the events dispatching facility.
 * 
 * @author Jerome Dochez
 */
@Service
public class EventsImpl implements Events {

    @Inject
    ExecutorService executor;
    
    final static Logger logger = KernelLoggerInfo.getLogger();

    List<EventListener> listeners = Collections.synchronizedList(new ArrayList<EventListener>());

    @Override
    public synchronized void register(EventListener listener) {
        listeners.add(listener);
    }

    @Override
    public void send(final Event event) {
        send(event, true);
    }

    @Override
    public void send(final Event event, boolean asynchronously) {
        
        List<EventListener> l = new ArrayList<EventListener>();
        l.addAll(listeners);
        for (final EventListener listener : l) {
            
            Method m =null;
            try {
                // check if the listener is interested with his event.
                m = listener.getClass().getMethod("event", Event.class);
            } catch (Throwable ex) {
                // We need to catch Throwable, otherwise we can server not to
                // shutdown when the following happens:
                // Assume a bundle which has registered a event listener
                // has been uninstalled without unregistering the listener.
                // listener.getClass() refers to a class of such an uninstalled
                // bundle. If framework has been refreshed, then the
                // classloader can't be used further to load any classes.
                // As a result, an exception like NoClassDefFoundError is thrown
                // from getMethod.
                logger.log(Level.SEVERE, KernelLoggerInfo.exceptionSendEvent, ex);
            }
            if (m!=null) {
                RestrictTo fooBar = m.getParameterTypes()[0].getAnnotation(RestrictTo.class);
                if (fooBar!=null) {
                    EventTypes interested = EventTypes.create(fooBar.value());
                    if (!event.is(interested)) {
                        continue;
                    }
                }
            }

            if (asynchronously) {
                executor.submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            listener.event(event);
                        } catch(Throwable e) {
                            logger.log(Level.WARNING, KernelLoggerInfo.exceptionDispatchEvent, e);
                        }
                    }
                });
            } else {
                try {
                    listener.event(event);
                } catch (DeploymentException de) {
                    // when synchronous listener throws DeploymentException
                    // we re-throw the exception to abort the deployment
                    throw de;
                } catch (Throwable e) {
                    logger.log(Level.WARNING, KernelLoggerInfo.exceptionDispatchEvent, e);
                }
            }
        }
    }

    @Override
    public synchronized boolean unregister(EventListener listener) {
        return listeners.remove(listener);
    }
}
