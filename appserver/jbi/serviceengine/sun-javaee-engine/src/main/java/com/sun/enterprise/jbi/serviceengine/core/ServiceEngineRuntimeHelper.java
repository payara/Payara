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

package com.sun.enterprise.jbi.serviceengine.core;

import com.sun.enterprise.config.serverbeans.Applications;
import com.sun.enterprise.transaction.api.JavaEETransactionManager;
import org.glassfish.api.ContractProvider;
import org.glassfish.api.invocation.InvocationManager;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.Singleton;
import com.sun.enterprise.deploy.shared.ArchiveFactory;
import com.sun.enterprise.deployment.archivist.ArchivistFactory;
import com.sun.enterprise.jbi.serviceengine.bridge.EndpointInfoCollector;
import org.glassfish.webservices.WebServiceEjbEndpointRegistry;
import org.jvnet.hk2.component.Habitat;

/**
 * Provides services exposed by GlassFish to ServiceEngine
 * @author Mohit Gupta
 */
@Service(name = "ServiceEngineRuntimeHelper")
@Scoped(Singleton.class)
public class ServiceEngineRuntimeHelper implements ContractProvider {

    private static ServiceEngineRuntimeHelper _runtime;
    @Inject
    private static Habitat habitat;
    @Inject
    private ArchivistFactory archivistFactory;
    @Inject
    private ArchiveFactory archiveFactory;
    @Inject
    private Applications applications;
    @Inject
    private JavaEETransactionManager tm;
    @Inject
    private InvocationManager inv;
    @Inject
    private EndpointInfoCollector endpointInfoCollector;
    @Inject
    private WebServiceEjbEndpointRegistry webServiceEjbEndpointRegistry;

    public static ServiceEngineRuntimeHelper getRuntime() {
        if (_runtime == null) {
            _runtime = habitat.getComponent(ServiceEngineRuntimeHelper.class);
           if (_runtime == null)
                throw new RuntimeException("ServiceEngineRuntimeHelper not initialized");
        }
        return _runtime;
    }

    public ServiceEngineRuntimeHelper() {
    }

    public Habitat getHabitat() {
        return habitat;
    }

    public ArchiveFactory getArchiveFactory() {
        return archiveFactory;
    }

    public ArchivistFactory getArchivistFactory() {
        return archivistFactory;
    }

    public JavaEETransactionManager getTransactionManager() {
        return tm;
    }

    public Applications getApplications() {
        return applications;
    }

    public InvocationManager getInvocationManager() {
        return inv;
    }

    public EndpointInfoCollector getEndpointInfoCollector() {
        return endpointInfoCollector;
    }

    public WebServiceEjbEndpointRegistry getWebServiceEjbEndpointRegistry() {
        return webServiceEjbEndpointRegistry;
    }
}
