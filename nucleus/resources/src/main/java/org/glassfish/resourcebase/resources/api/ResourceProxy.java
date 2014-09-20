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

package org.glassfish.resourcebase.resources.api;

import com.sun.enterprise.config.serverbeans.Resource;
import org.glassfish.api.naming.NamingObjectProxy;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.naming.Context;
import javax.naming.NamingException;



/**
 * Represents the proxy object for a resource. Proxy will be bound in jndi
 * during startup and the actual <i>resource</i> will be deployed during
 * first lookup
 *
 * @author Jagadish Ramu
 */
@Service
@PerLookup
public class ResourceProxy implements NamingObjectProxy.InitializationNamingObjectProxy {

    @Inject
    private Provider<org.glassfish.resourcebase.resources.util.ResourceManagerFactory> resourceManagerFactoryProvider;

    @Inject
    private org.glassfish.resourcebase.resources.naming.ResourceNamingService namingService;

    private Resource resource = null;
    private Object result = null;
    private org.glassfish.resourcebase.resources.api.ResourceInfo resourceInfo = null;

    public Object create(Context ic) throws NamingException {
        //this is a per-lookup object and once we have the resource,
        //we remove the proxy and bind the resource (ref) with same jndi-name
        //hence block synchronization is fine as it blocks only callers
        //of this particular resource and also only for first time (initialization)
        synchronized(this){
            try{
                if(result == null){
                    getResourceDeployer(resource).deployResource(resource, resourceInfo.getApplicationName(),
                            resourceInfo.getModuleName());
                }
                result = namingService.lookup(resourceInfo, resourceInfo.getName(), ic.getEnvironment());
            }catch(Exception e){
                throwResourceNotFoundException(e, resourceInfo);
            }
        }
        return result;
    }

    /**
     * Set the resource config bean instance
     * @param resource config bean
     */
    public void setResource(Resource resource){
        this.resource = resource;
    }


    /**
     * Name by which the proxy (or the resource) will be bound in JNDI
     * @param resourceInfo resource-info
     */
    public void setResourceInfo(org.glassfish.resourcebase.resources.api.ResourceInfo resourceInfo) {
        this.resourceInfo = resourceInfo;
    }

    protected Object throwResourceNotFoundException(Exception e, org.glassfish.resourcebase.resources.api.ResourceInfo resourceInfo) throws NamingException {
        NamingException ne = new NamingException("Unable to lookup resource : " + resourceInfo);
        ne.initCause(e);
        throw ne;
    }

    /**
     * Given a <i>resource</i> instance, appropriate deployer will be provided
     *
     * @param resource resource instance
     * @return ResourceDeployer
     */
    protected org.glassfish.resourcebase.resources.api.ResourceDeployer getResourceDeployer(Object resource){
        return resourceManagerFactoryProvider.get().getResourceDeployer(resource);
    }
}
