/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.v3.server;

import org.glassfish.server.ServerEnvironmentImpl;
import com.sun.enterprise.module.bootstrap.StartupContext;
import com.sun.enterprise.glassfish.bootstrap.StartupContextUtil;
import org.glassfish.internal.api.ClassLoaderHierarchy;
import org.glassfish.internal.api.ServerContext;
import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.api.naming.GlassfishNamingManager;
import javax.inject.Inject;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PostConstruct;
import org.glassfish.hk2.api.ServiceLocator;

import javax.inject.Singleton;

import javax.naming.InitialContext;
import java.io.File;
import java.util.Map;

/**
 * This is the Server Context object.
 *
 * @author Jerome Dochez
 */
@Service
@Singleton
public class ServerContextImpl implements ServerContext, PostConstruct {

    @Inject
    ServerEnvironmentImpl env;

    @Inject
    StartupContext startupContext;

    @Inject
    ServiceLocator services;

    File instanceRoot;
    String[] args;

    /** Creates a new instance of ServerContextImpl */
    public void postConstruct() {
        this.instanceRoot = env.getDomainRoot();
        this.args = new String[startupContext.getArguments().size()*2];
        int i=0;
        for (Map.Entry<Object, Object> entry : startupContext.getArguments().entrySet()) {
            args[i++] = entry.getKey().toString();
            args[i++] = entry.getValue().toString();
        }
    }
    
    public File getInstanceRoot() {
        return instanceRoot;
    }

    public String[] getCmdLineArgs() {
        return args;
    }

    public File getInstallRoot() {
        return StartupContextUtil.getInstallRoot(startupContext);
    }

    public String getInstanceName() {
        return env.getInstanceName();
    }

    public String getServerConfigURL() {
        File domainXML = new File(instanceRoot, ServerEnvironmentImpl.kConfigDirName);
        domainXML = new File(domainXML, ServerEnvironmentImpl.kConfigXMLFileName);
        return domainXML.toURI().toString();
    }

    public com.sun.enterprise.config.serverbeans.Server getConfigBean() {
        return services.getService(com.sun.enterprise.config.serverbeans.Server.class);
    }

    public InitialContext getInitialContext() {
        GlassfishNamingManager gfNamingManager = 
            services.getService(GlassfishNamingManager.class);
        return (InitialContext)gfNamingManager.getInitialContext();
    }

    public ClassLoader getCommonClassLoader() {
        return services.<CommonClassLoaderServiceImpl>getService(CommonClassLoaderServiceImpl.class).getCommonClassLoader();
    }

    public ClassLoader getSharedClassLoader() {
        return services.<ClassLoaderHierarchy>getService(ClassLoaderHierarchy.class).getConnectorClassLoader(null);
    }

    public ClassLoader getLifecycleParentClassLoader() {
        return services.<ClassLoaderHierarchy>getService(ClassLoaderHierarchy.class).getConnectorClassLoader(null);
    }

    public InvocationManager getInvocationManager() {
        return services.getService(InvocationManager.class);
    }

    public String getDefaultDomainName() {
        return "glassfish-web";
    }
    /**
     * Returns the default services for this instance
     * @return default services
     */
    public ServiceLocator getDefaultServices() {
        return services;
        
    }
}
