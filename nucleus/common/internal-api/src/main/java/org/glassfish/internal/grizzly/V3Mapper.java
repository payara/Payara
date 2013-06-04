/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.internal.grizzly;


import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.grizzly.http.server.naming.NamingContext;

import org.glassfish.grizzly.http.server.util.Mapper;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;

/**
 * Extended that {@link Mapper} that prevent the WebContainer to unregister
 * the current {@link Mapper} configuration.
 * 
 * @author Jeanfrancois Arcand
 */
@Service
@ContractsProvided({V3Mapper.class, Mapper.class})
public class V3Mapper extends ContextMapper {

    private static final String ADMIN_LISTENER = "admin-listener";
    private static final String ADMIN_VS = "__asadmin";


    public V3Mapper() {
    }   
    
    
    public V3Mapper(Logger logger) {
        super(logger);
    }

    
    /**
     * {@inheritDoc}
     */
    @Override
    public void addWrapper(String hostName, String contextPath, String path,
            Object wrapper, boolean jspWildCard) {
        super.addWrapper(hostName, contextPath, path, wrapper, jspWildCard);
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Wrapper-Host: {0} contextPath {1} wrapper {2} path {3} jspWildcard {4}",
                    new Object[]{hostName, contextPath, wrapper, path, jspWildCard});
        }                          
    }
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void addHost(String name, String[] aliases, Object host) {

        // Prevent any admin related artifacts from being registered on a
        // non-admin listener, and vice versa
        if (ADMIN_LISTENER.equals(getId()) && !ADMIN_VS.equals(name) ||
            !ADMIN_LISTENER.equals(getId()) && ADMIN_VS.equals(name)) {
            return;
        }

        super.addHost(name, aliases, host);
    }

    
    /**
     * {@inheritDoc}
     */
    @Override
    public void addContext(String hostName, String path, Object context,
            String[] welcomeResources, NamingContext resources) {
        
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Context-Host: {0} path {1} context {2} port {3}",
                    new Object[]{hostName, path, context, getPort()});
        }
        
        // Prevent any admin related artifacts from being registered on a
        // non-admin listener, and vice versa
        if (ADMIN_LISTENER.equals(getId()) && !ADMIN_VS.equals(hostName) ||
            !ADMIN_LISTENER.equals(getId()) && ADMIN_VS.equals(hostName)) {
            return;
        }
        
        // The WebContainer is registering new Context. In that case, we must
        // clean all the previously added information, specially the 
        // MappingData.wrapper info as this information cannot apply
        // to this Container.
        if (adapter != null && "org.apache.catalina.connector.CoyoteAdapter".equals(adapter.getClass().getName())) {
            removeContext(hostName, path);
        }
        
        super.addContext(hostName, path, context, welcomeResources, resources);
    }
}
