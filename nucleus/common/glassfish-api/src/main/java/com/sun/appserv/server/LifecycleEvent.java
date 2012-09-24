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
 */

package com.sun.appserv.server;


/**
 * This class defines the types of events that get fired by the application server. 
 * It also contains a LifecycleEventContext that can be used by the 
 * lifecycle modules.
 */
public class LifecycleEvent extends java.util.EventObject {
    
    private int eventType;
    private Object eventData;
    private transient LifecycleEventContext ctx = null;
    
    // Lifecycle event types 

    /** Server is initializing subsystems and setting up the runtime environment.
    */
    public final static int INIT_EVENT = 0; 

    /** Server is starting up applications
     */
    public final static int STARTUP_EVENT = 1; 

    /** Server is ready to service requests
     */
    public final static int READY_EVENT = 2; 

    /** Server is shutting down applications
     */
    public final static int SHUTDOWN_EVENT = 3; 

    /** Server is terminating the subsystems and the runtime environment.
     */
    public final static int TERMINATION_EVENT = 4; 

    
    /**
     * Construct new lifecycle event
     * @param source The object on which the event initially occurred
     * @param eventType type of the event
     * @param ctx the underlying context for the lifecycle event
     */
    public LifecycleEvent(Object source, int eventType, Object eventData, LifecycleEventContext ctx) {
        super(source);

        this.eventType = eventType;
        this.eventData = eventData;
        this.ctx = ctx;
    }
    
    /** Get the type of event associated with this 
     */
    public int getEventType() {
        return eventType;
    }

    /** Get the data associated with the event.
     */
    public Object getData() {
        return eventData;
    }

    /** Get the ServerContext generating this lifecycle event 
     */
    public LifecycleEventContext getLifecycleEventContext() {
        return ctx;
    }
}
