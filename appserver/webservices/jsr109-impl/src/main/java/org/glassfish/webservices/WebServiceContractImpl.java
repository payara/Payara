/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.webservices;

import com.sun.enterprise.container.common.spi.util.ComponentEnvManager;
import com.sun.enterprise.container.common.spi.util.InjectionManager;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.container.Adapter;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.glassfish.hk2.api.IterableProvider;
import org.jvnet.hk2.annotations.Optional;
import org.jvnet.hk2.annotations.Service;
import org.glassfish.internal.api.Globals;
import org.glassfish.internal.data.ApplicationRegistry;
import org.glassfish.server.ServerEnvironmentImpl;
import org.glassfish.api.invocation.InvocationManager;
import com.sun.enterprise.module.ModulesRegistry;
import com.sun.enterprise.config.serverbeans.Config;

import java.util.Collections;
import java.util.logging.Logger;

/**
 * This is the implementation class which will provide the implementation
 * to access the injected fields like the NamingManager , ComponentEnvManager
 */
@Service
public class WebServiceContractImpl implements WebServicesContract{

    @Inject
    private ComponentEnvManager compEnvManager;

    @Inject
    private ServerEnvironmentImpl env;

    @Inject
    private ModulesRegistry modulesRegistry;

    @Inject
    private InvocationManager invManager;

    @Inject @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME) @Optional
    private Provider<Config> configProvider;

    @Inject @Optional
	private Provider<ApplicationRegistry> applicationRegistryProvider;

    @Inject @Optional
	private IterableProvider<Adapter> adapters;

    @Inject @Optional
	private Provider<InjectionManager> injectionManagerProvider;
    
    private  static WebServiceContractImpl wscImpl;

    private static final Logger logger = LogUtils.getLogger();

    public ComponentEnvManager getComponentEnvManager() {
        return compEnvManager;  
    }

    public Config getConfig() {
        return configProvider.get();
    }
  
    public InvocationManager getInvocationManager() {
            return invManager;
    }

    public ServerEnvironmentImpl getServerEnvironmentImpl (){
        return env;
    }

    public ModulesRegistry getModulesRegistry (){
            return modulesRegistry;
    }

    public static WebServiceContractImpl getInstance() {
        // Create the instance first to access the logger.
        wscImpl = Globals.getDefaultHabitat().getService(
                WebServiceContractImpl.class);
        return wscImpl;
    }

    public Logger getLogger() {
        return logger;
    }

	public ApplicationRegistry getApplicationRegistry() {
		return applicationRegistryProvider.get();
	}

	public ServerEnvironment getServerEnvironment() {
		return env;
	}

	public Iterable<Adapter> getAdapters() {
		return (adapters != null) ? adapters : Collections.EMPTY_LIST;
	}

	public InjectionManager getInjectionManager() {
		return injectionManagerProvider.get();
	}
}
