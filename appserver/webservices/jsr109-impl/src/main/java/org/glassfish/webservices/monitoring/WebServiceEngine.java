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

package org.glassfish.webservices.monitoring;

import java.util.Iterator;

/**
 * This interface holds all behaviour associated with the
 * web service engine. For instance, it gives a list of all
 * registered endpoints, provide hooks to register listener
 * interfaces for endpoints creation/deletion and so on...
 *
 * @author Jerome Dochez
 * @author Bhakti Mehta
 */
public interface WebServiceEngine {

    /**
     * @return an iterator of all the registered active endpoints in
     * the engine.
     */
    public Iterator<Endpoint> getEndpoints();

    /**
     * @return an Endpoint instance if the supplied selector is the endpoint's
     * invocation selector. In case of HTTP based web services, the selector is
     * the endpoint URL
     * @param endpointSelector the endpoint selector
     */
    public Endpoint getEndpoint(String endpointSelector);

    /**
     * Register a new listener interface to receive notification of
     * web service endpoint creation and deletion
     * @param listener instance to register
     */
    public void addLifecycleListener(EndpointLifecycleListener listener);

    /**
     * Unregister a listener interface
     * @param listener to unregister.
     */
    public void removeLifecycleListener(EndpointLifecycleListener listener);

    /**
     * Register a new listener interface to receive authentication
     * notification.
     * @param listener to add
     */
    public void addAuthListener(AuthenticationListener listener);

    /**
     * Unregister a listener interface
     * @param listener to remove
     */
    public void removeAuthListener(AuthenticationListener listener);

    /**
     * Set the unique global listener interface to trace all web service requests
     * or responses. Set to null if no tracing is needed
     * @param listener to register
     */
    public void setGlobalMessageListener(GlobalMessageListener listener);

    /**
     * get the global listener interface or null if none is set.
     * @return the global message listener
     */
    public GlobalMessageListener getGlobalMessageListener();

}
