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

package org.glassfish.resources.module;

import com.sun.enterprise.config.serverbeans.*;
import com.sun.logging.LogDomains;
import java.util.logging.Level;
import org.glassfish.api.deployment.*;
import org.glassfish.internal.data.ApplicationRegistry;
import org.glassfish.resources.listener.ApplicationScopedResourcesManager;
import org.jvnet.hk2.annotations.Service;

import org.glassfish.hk2.api.PerLookup;

import java.util.logging.Logger;

import javax.inject.Inject;

@Service
@PerLookup
public class ResourcesApplication implements ApplicationContainer{

    private static final Logger _logger = LogDomains.getLogger(ResourcesApplication.class, LogDomains.RSR_LOGGER);
    private String applicationName;

    @Inject
    private ApplicationRegistry appRegistry;

    @Inject
    private Applications applications;

    private Resources resources ;

    @Inject
    private ApplicationScopedResourcesManager asrManager;
    
    @Inject
    private ResourcesDeployer resourcesDeployer;

    public ResourcesApplication(){
    }

    public void setApplicationName(String applicationName){
        this.applicationName = applicationName;
    }

    public String getApplicationName(){
        return applicationName;
    }

    public Object getDescriptor() {
        //TODO return all resources-xml ?
        return null;  
    }

    public boolean start(ApplicationContext startupContext) throws Exception {
        DeploymentContext dc = (DeploymentContext)startupContext;
        final DeployCommandParameters deployParams = dc.getCommandParameters(DeployCommandParameters.class);
        //during app. deployment, create resources config and load resources
        if(deployParams.origin == OpsParams.Origin.deploy || deployParams.origin == OpsParams.Origin.deploy_instance){
            resourcesDeployer.deployResources(applicationName, true);
        }else if (deployParams.origin == OpsParams.Origin.load ||
                deployParams.origin == OpsParams.Origin.create_application_ref) {
            //<application> and its <resources>, <modules> are already available.
            //Deploy them.
            
            //during app. load (eg: server start or application/application-ref enable(), load resources
            asrManager.deployResources(applicationName);
        }
        return true;
    }

    public boolean stop(ApplicationContext stopContext) {
        asrManager.undeployResources(applicationName);
        return true;
    }

    public boolean suspend() {
        return true;
    }

    public boolean resume() throws Exception {
        return true;
    }

    public ClassLoader getClassLoader() {
        //TODO return loader
        return null;
    }
    private void  debug(String message){
        if(_logger.isLoggable(Level.FINEST)) {
            _logger.finest("[ResourcesApplication] " + message);
        }
    }

}
