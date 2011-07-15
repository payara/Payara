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

package com.sun.enterprise.resource.naming;

import com.sun.enterprise.resource.beans.AdministeredObjectResource;
import com.sun.enterprise.connectors.ConnectorRuntime;
import com.sun.enterprise.connectors.ConnectorRegistry;
import com.sun.enterprise.connectors.service.ConnectorAdminServiceUtils;
import com.sun.enterprise.deployment.ConnectorDescriptor;
import com.sun.logging.LogDomains;
import com.sun.appserv.connectors.internal.api.ConnectorRuntimeException;
import com.sun.appserv.connectors.internal.api.ConnectorsUtil;

import java.util.Hashtable;
import java.util.logging.Logger;
import java.util.logging.Level;
import javax.naming.*;
import javax.naming.spi.ObjectFactory;

/**
 * An object factory to handle creation of administered object
 *
 * @author	Qingqing Ouyang
 *
 */
public class AdministeredObjectFactory implements ObjectFactory {

    private static Logger logger =
    LogDomains.getLogger(AdministeredObjectFactory.class, LogDomains.RSR_LOGGER);

    //required by ObjectFactory
    public AdministeredObjectFactory() {}

    public Object getObjectInstance(Object obj,
				    Name name,
				    Context nameCtx,
				    Hashtable env) throws Exception {

	Reference ref = (Reference) obj;
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("AdministeredObjectFactory: " + ref
                    + " Name:" + name);
        }

        AdministeredObjectResource aor =
            (AdministeredObjectResource) ref.get(0).getContent();
        String moduleName = aor.getResourceAdapter();


        //If call fom application client, start resource adapter lazily.
        //todo: Similar code in ConnectorObjectFactory - to refactor.

        ConnectorRuntime runtime = ConnectorNamingUtils.getRuntime();
        if (runtime.isACCRuntime() || runtime.isNonACCRuntime()) {
            ConnectorDescriptor connectorDescriptor = null;
            try {
                Context ic = new InitialContext();
                String descriptorJNDIName = ConnectorAdminServiceUtils.
                        getReservePrefixedJNDINameForDescriptor(moduleName);
                connectorDescriptor = (ConnectorDescriptor) ic.lookup(descriptorJNDIName);
            } catch (NamingException ne) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "Failed to look up ConnectorDescriptor "
                            + "from JNDI", moduleName);
                }
                throw new ConnectorRuntimeException("Failed to look up " +
                        "ConnectorDescriptor from JNDI");
            }
            runtime.createActiveResourceAdapter(connectorDescriptor, moduleName, null);
        }

        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (runtime.checkAccessibility(moduleName, loader) == false) {
	        throw new NamingException("Only the application that has the embedded resource" +
	                               "adapter can access the resource adapter");

	    }

        if(logger.isLoggable(Level.FINE)) {
	    logger.fine("[AdministeredObjectFactory] ==> Got AdministeredObjectResource = " + aor);
        }

    // all RARs except system RARs should have been available now.
        if(ConnectorsUtil.belongsToSystemRA(moduleName)) {
            //make sure that system rar is started and hence added to connector classloader chain
            if(ConnectorRegistry.getInstance().getActiveResourceAdapter(moduleName) == null){
                String moduleLocation = ConnectorsUtil.getSystemModuleLocation(moduleName);
                runtime.createActiveResourceAdapter(moduleLocation, moduleName, null);
            }
            loader = ConnectorRegistry.getInstance().getActiveResourceAdapter(moduleName).getClassLoader();
        } else if(runtime.isServer()){
            if(ConnectorsUtil.isStandAloneRA(moduleName) ){
                loader = ConnectorRegistry.getInstance().getActiveResourceAdapter(moduleName).getClassLoader();
            }
        }
	return aor.createAdministeredObject(loader);
    }
}
