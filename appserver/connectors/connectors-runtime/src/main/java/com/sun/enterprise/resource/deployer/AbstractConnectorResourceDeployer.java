/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.resource.deployer;

import com.sun.appserv.connectors.internal.api.ConnectorConstants;
import com.sun.appserv.connectors.internal.api.ConnectorsUtil;
import com.sun.enterprise.config.serverbeans.Application;
import com.sun.enterprise.config.serverbeans.Module;
import com.sun.enterprise.config.serverbeans.Resource;
import com.sun.enterprise.config.serverbeans.Resources;
import org.glassfish.resourcebase.resources.api.ResourceConflictException;
import org.glassfish.resourcebase.resources.api.ResourceDeployer;
import org.glassfish.resources.api.GlobalResourceDeployer;
import org.glassfish.resourcebase.resources.api.ResourceConstants;
import org.glassfish.resourcebase.resources.util.ResourceUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class AbstractConnectorResourceDeployer extends GlobalResourceDeployer implements ResourceDeployer {

    /**
     * {@inheritDoc}
     */
    public boolean canDeploy(boolean postApplicationDeployment, Collection<Resource> allResources, Resource resource){
        if(handles(resource)){
            if(postApplicationDeployment &&
                    ConnectorsUtil.isEmbeddedRarResource(resource, allResources) == ResourceConstants.TriState.TRUE){
                    return true;
            }

            if(!postApplicationDeployment&&
                    ConnectorsUtil.isEmbeddedRarResource(resource, allResources) == ResourceConstants.TriState.FALSE){
                    return true;
            }

        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public void validatePreservedResource(Application oldApp, Application newApp, Resource resource,
                                          Resources allResources)
            throws ResourceConflictException {

        //check whether old app has any RAR
        List<Module> oldRARModules = new ArrayList<Module>();
        List<Module> oldModules = oldApp.getModule();
        for (Module oldModule : oldModules) {
            if (oldModule.getEngine(ConnectorConstants.CONNECTOR_MODULE) != null) {
                oldRARModules.add(oldModule);
            }
        }

/*
       //check whether new app has any RAR
       //TODO ASR : <sniffer> info is not available during initial phase of deployment. Hence doing "module-name" check.
       List<Module> newRARModules = new ArrayList<Module>();
        List<Module> newModules = newApp.getModule();
        for (Module newModule : newModules) {
            if (newModule.getEngine(ResourceConstants.CONNECTOR_MODULE) != null) {
                newRARModules.add(newModule);
            }
        }
*/
        List<Module> newRARModules = newApp.getModule();


        //check whether all old RARs are present in new RARs list.
        List<Module> staleRars = new ArrayList<Module>();
        for (Module oldRARModule : oldRARModules) {
            String oldRARModuleName = oldRARModule.getName();
            boolean found = false;
            for (Module newRARModule : newRARModules) {
                String newRARModuleName = newRARModule.getName();
                if (newRARModuleName.equals(oldRARModuleName)) {
                    found = true;
                }
            }
            if(!found){
                staleRars.add(oldRARModule);
            }
        }

        String appName = newApp.getName();
        if (staleRars.size() > 0) {
            validateResourcesForStaleReference(appName, staleRars, allResources);
        }
    }

    /**
     * Validates whether the old application has RARs and those are retained in new application.<br>
     * If the new application does not have any of the old application's RAR, validates whether<br>
     * any module is using the RAR's resources. If used, fail with ResourceConflictException<br>
     * as the RAR's resource is not valid anymore.
     * @param appName application-name
     * @param staleRars List of Stale Resource Adapters (ie., were defined in old app, not in new app)
     * @param resources resources that need to be checked for stale RAR references.
     * @throws org.glassfish.resources.api.ResourceConflictException When any of the resource has reference to old RAR
     */
    public static void validateResourcesForStaleReference(String appName, List<Module> staleRars, Resources resources)
            throws ResourceConflictException {
        boolean found = false;
        for (Resource resource : resources.getResources()) {
            //connector type of resource may be : connector-resource, ccp, aor, wsm, rac
            if (ConnectorsUtil.isRARResource(resource)) {
                String rarNameOfResource = ConnectorsUtil.getRarNameOfResource(resource, resources);
                if (rarNameOfResource.contains(ConnectorConstants.EMBEDDEDRAR_NAME_DELIMITER)) {
                    String embeddedRARName = ConnectorsUtil.getRarNameFromApplication(rarNameOfResource);
                    for (Module module : staleRars) {
                        //check whether these RARs are referenced by app-scoped-resources ?
                        if (ResourceUtil.getActualModuleNameWithExtension(module.getName()).equals(embeddedRARName)) {
                            throw new ResourceConflictException("Existing resources refer RAR " +
                                    "[ " + embeddedRARName + " ] which is" +
                                    "not present in the re-deployed application ["+appName+"] anymore. " +
                                    "re-deploy the application after resolving the conflicts");
                        }
                    }
                }
            }
        }
    }
}
