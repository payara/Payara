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

package com.sun.appserv.server;

import java.util.Properties;

/**
 * lifecycle modules implement <code>com.sun.appserv.server.LifecycleListener</code> interface. 
 * There is just one method in this interface: <code>handleEvent()</code> which posts server
 * lifecycle events to the lifecycle modules.
 * <p>
 * Upon start up, before initializing its subsystems application server posts lifcycle modules the 
 * <code>INIT_EVENT</code>. This is followed by server posting the <code>STARTUP_EVENT</code> to the 
 * lifecycle modules upon which server starts loading and initializaing the applications. Once this 
 * phase is completed, the <code>READY_EVENT</code> is posted to the lifecycle modules.
 * <p>
 * When the server is shutdown, server posts the <code>SHUTDOWN_EVENT</code> to the lifecycle modules and
 * then shuts down the applications and subsystems. Once this phase is completed the 
 * <code>TERMINATION_EVENT</code> is posted.
 * <p>
 * Note that lifecycle modules may obtain the event specific data by calling <code>getData()</code> 
 * on the event parameter in the <code>handleEvent()</code>. For the INIT_EVENT event,
 * <code>getData()</code> returns the lifecycle module's properties configured in server.xml.
 * <p>
 *  When <code>is-failure-fatal</code> in server.xml is set to <code>true</code>, all exceptions from the
 *  lifecycle modules are treated as fatal conditions.
 */
public interface LifecycleListener {

    /** receive a server lifecycle event 
     *  @param event associated event
     *  @throws <code> ServerLifecycleException </code> for exception condition.
     *
     */
    public void handleEvent(LifecycleEvent event) throws ServerLifecycleException; 
}
