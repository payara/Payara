/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.concurrent.runtime.deployer;

import org.glassfish.api.naming.DefaultResourceProxy;
import org.glassfish.api.naming.NamedNamingObjectProxy;
import org.glassfish.api.naming.NamespacePrefixes;
import org.jvnet.hk2.annotations.Service;

import javax.enterprise.concurrent.ContextService;
import javax.inject.Inject;
import javax.naming.NamingException;

/**
 * Naming Object Proxy to handle the Default ContextService.
 * Maps to a pre-configured context service, when binding for
 * a context service reference is absent in the @Resource annotation.
 */
@Service
@NamespacePrefixes({DefaultContextService.DEFAULT_CONTEXT_SERVICE})
public class DefaultContextService implements NamedNamingObjectProxy, DefaultResourceProxy {

    static final String DEFAULT_CONTEXT_SERVICE = "java:comp/DefaultContextService";
    static final String DEFAULT_CONTEXT_SERVICE_PHYS = "concurrent/__defaultContextService";
    private ContextService contextService;
    
    // Ensure that config for this object has been created
    @Inject org.glassfish.concurrent.config.ContextService.ContextServiceConfigActivator config;

    @Override
    public Object handle(String name) throws NamingException {
        if(contextService == null) {
            javax.naming.Context ctx = new javax.naming.InitialContext();
            // cache the managed executor service to avoid JNDI lookup overheads
            contextService = (ContextService)ctx.lookup(DEFAULT_CONTEXT_SERVICE_PHYS);
        }
        return contextService;
    }

    @Override
    public String getPhysicalName() {
        return DEFAULT_CONTEXT_SERVICE_PHYS;
    }

    @Override
    public String getLogicalName() {
        return DEFAULT_CONTEXT_SERVICE;
    }
}
