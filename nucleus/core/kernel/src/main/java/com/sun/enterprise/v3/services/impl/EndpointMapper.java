/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.v3.services.impl;

import java.util.Collection;
import org.glassfish.api.deployment.ApplicationContainer;
import org.glassfish.api.container.EndpointRegistrationException;

/**
 * registration interface to use with the Mapper classes. 
 * 
 * @author Jeanfrancois Arcand
 */
public interface EndpointMapper<E> {
    
    
    /**
     * Registers a new endpoint (proxy implementation) for a particular
     * context-root. All request coming with the context root will be dispatched
     * to the proxy instance passed in.
     * @param contextRoot for the proxy
     * @param endpointAdapter servicing requests.
     */
    public void registerEndpoint(String contextRoot, Collection<String> vsServers, E adapter,
                                 ApplicationContainer container) throws EndpointRegistrationException;
    
    
    /**
     * Removes the context-root from our list of endpoints.
     */
    public void unregisterEndpoint(String contextRoot, ApplicationContainer app) throws EndpointRegistrationException;

    /**
     * Registers a new endpoint (proxy implementation) defined by the passed
     * {@link Endpoint} object.
     * @param endpoint {@link Endpoint}
     */
    public void registerEndpoint(Endpoint endpoint) throws EndpointRegistrationException;
    
    /**
     * Removes the {@link Endpoint} from our list of endpoints.
     * @param endpoint {@link Endpoint}
     */
    public void unregisterEndpoint(Endpoint endpoint) throws EndpointRegistrationException;
}
