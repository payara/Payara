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

package com.sun.enterprise.connectors.module;

import com.sun.appserv.connectors.internal.api.ConnectorConstants;
import com.sun.appserv.connectors.internal.api.ConnectorsUtil;
import com.sun.enterprise.config.serverbeans.Resource;
import com.sun.enterprise.config.serverbeans.Resources;
import com.sun.enterprise.connectors.ConnectorRuntime;
import com.sun.enterprise.connectors.util.ResourcesUtil;
import com.sun.enterprise.deployment.ConnectorDescriptor;
import com.sun.enterprise.util.i18n.StringManager;
import com.sun.logging.LogDomains;
import org.glassfish.api.ActionReport;
import org.glassfish.api.deployment.*;
import org.glassfish.api.event.EventListener;
import org.glassfish.api.event.Events;
import org.glassfish.connectors.config.AdminObjectResource;
import org.glassfish.connectors.config.ConnectorConnectionPool;
import org.glassfish.internal.deployment.Deployment;
import org.glassfish.resources.listener.ApplicationScopedResourcesManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a connector application, one per resource-adapter.
 * GlassFish kernel will call start/stop of connector application during start/stop of server and
 * deploy/undeploy of the resource-adapter.
 *
 * @author Jagadish Ramu
 */
public class ConnectorApplication implements ApplicationContainer, EventListener {
    private static Logger _logger = LogDomains.getLogger(ConnectorApplication.class, LogDomains.RSR_LOGGER);
    private String moduleName = "";
    //indicates the "application" (ear) name if its embedded rar
    private String applicationName = null;
    private org.glassfish.resourcebase.resources.listener.ResourceManager resourceManager;
    private ApplicationScopedResourcesManager asrManager;
    private ClassLoader loader;
    private ConnectorRuntime runtime;
    private Events event;
    private ConnectorDescriptor descriptor;
    private static StringManager localStrings = StringManager.getManager(ConnectorRuntime.class);
    private ResourcesUtil resourcesUtil;

    public ConnectorApplication(String moduleName, String appName, org.glassfish.resourcebase.resources.listener.ResourceManager resourceManager,
                                ApplicationScopedResourcesManager asrManager, ClassLoader loader,
                                ConnectorRuntime runtime, Events event, ConnectorDescriptor descriptor) {
        this.setModuleName(moduleName);
        this.resourceManager = resourceManager;
        this.asrManager = asrManager;
        this.loader = loader;
        this.runtime = runtime;
        this.applicationName = appName;
        this.event = event;
        this.descriptor = descriptor;
        this.resourcesUtil = ResourcesUtil.createInstance();
    }

    /**
     * Returns the deployment descriptor associated with this application
     *
     * @return deployment descriptor if they exist or null if not
     */
    public Object getDescriptor() {
        return descriptor;
    }

    /**
     * Starts an application container.
     * ContractProvider starting should not throw an exception but rather should
     * use their prefered Logger instance to log any issue they encounter while
     * starting. Returning false from a start mean that the container failed
     * to start
     *
     * @param startupContext the start up context
     * @return true if the container startup was successful.
     */
    public boolean start(ApplicationContext startupContext) {
        boolean started = false;

        deployResources();
        runtime.registerConnectorApplication(this);

        started = true;

        event.register(this);

        logFine("Resource Adapter [ " + getModuleName() + " ] started");
        return started;
    }

    /**
     * deploy all resources/pools pertaining to this resource adapter
     */
    public void deployResources() {
        deployGlobalResources();
        //deployApplicationScopedResources();
    }

    private void deployGlobalResources() {
        Resources allResources = resourceManager.getAllResources();
        Collection<Resource> resources = resourcesUtil.filterConnectorResources(allResources, moduleName, false);
        resourceManager.deployResources(resources);
    }

/*
    private void deployApplicationScopedResources() {
        Resources resources = asrManager.getResources(applicationName);
        if(resources != null){
            Collection<Resource> connectorResources = filterConnectorResources(resources);
            asrManager.deployResources(connectorResources);
        }
    }
*/
    /**
     * undeploy all resources/pools pertaining to this resource adapter
     */
    public void undeployResources() {
        undeployGlobalResources(false);
        //undeployApplicationScopedResources();
    }

/*
    public void undeployApplicationScopedResources() {
        Collection<Resource> resources = filterConnectorResources(asrManager.getResources(applicationName));
        asrManager.undeployResources(resources);    
    }
*/

    /**
     * undeploy all resources/pools pertaining to this resource adapter
     */
    public boolean undeployGlobalResources(boolean failIfResourcesExist) {
        boolean status;
        //TODO ASR : should we undeploy app-scoped connector resources also ?
        //TODO ASR : should we stop deployment by checking app-scoped connector resources also ?
        Collection<Resource> resources =
                resourcesUtil.filterConnectorResources(resourceManager.getAllResources(), moduleName, true);
        if (failIfResourcesExist && resources.size() > 0) {
            String message = "one or more resources of resource-adapter [ " + moduleName + " ] exist, " +
                    "use '--cascade=true' to delete them during undeploy";
            _logger.log(Level.WARNING, "resources.of.rar.exist", moduleName);
            status = false;
            throw new RuntimeException(message);
        } else {
            resourceManager.undeployResources(resources);
            status = true;
        }
        return status;
    }

    /**
     * Stop the application container
     *
     * @param stopContext
     * @return true if stopping was successful.
     */
    public boolean stop(ApplicationContext stopContext) {
        boolean stopped = false;

        DeploymentContext dc = (DeploymentContext) stopContext;
        UndeployCommandParameters dcp = dc.getCommandParameters(UndeployCommandParameters.class);
        boolean failIfResourcesExist = false;

        //"stop" may be called even during deployment/load failure.
        //Look for the undeploy flags only when it is undeploy-command
        if(dcp != null){
            if (dcp.origin == OpsParams.Origin.undeploy) {
                if(!(dcp._ignoreCascade || dcp.cascade)){
                    failIfResourcesExist = true;
                }
            }
        }

        if (!undeployGlobalResources(failIfResourcesExist)) {
            stopped = false;
        } else {
            runtime.unregisterConnectorApplication(getModuleName());
            stopped = true;
            logFine("Resource Adapter [ " + getModuleName() + " ] stopped");
            event.unregister(this);
        }
        return stopped;
    }

    /**
     * Suspends this application container.
     *
     * @return true if suspending was successful, false otherwise.
     */
    public boolean suspend() {
        // Not (yet) supported
        return false;
    }

    /**
     * Resumes this application container.
     *
     * @return true if resumption was successful, false otherwise.
     */
    public boolean resume() {
        // Not (yet) supported
        return false;
    }

    /**
     * Returns the class loader associated with this application
     *
     * @return ClassLoader for this app
     */
    public ClassLoader getClassLoader() {
        return loader;
    }

    public void logFine(String message) {
        if(_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE, message);
        }
    }

    /**
     * returns the module name
     *
     * @return module-name
     */
    public String getModuleName() {
        return moduleName;
    }

    /**
     * set the module name of the application
     *
     * @param moduleName module-name
     */
    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public String getApplicationName() {
        return applicationName;
    }

    /**
     * event listener to listen to </code>resource-adapter undeploy validation</code> and
     * to validate the undeployment. Undeployment will fail, if resources are found
     * and --cascade is not set.
     * @param event Event 
     */
    public void event(Event event) {
        if (Deployment.UNDEPLOYMENT_VALIDATION.equals(event.type())) {
            //this is an application undeploy event
            DeploymentContext dc = (DeploymentContext) event.hook();
            UndeployCommandParameters dcp = dc.getCommandParameters(UndeployCommandParameters.class);
            if (dcp.name.equals(moduleName) ||
                    //Consider the application with embedded RAR being undeployed
                    (dcp.name.equals(applicationName) &&
                    moduleName.contains(ConnectorConstants.EMBEDDEDRAR_NAME_DELIMITER) &&
                    moduleName.startsWith(dcp.name))) {

                if (dcp.origin != OpsParams.Origin.deploy) {
                    if (dcp.origin == OpsParams.Origin.undeploy) {
                        if (!(dcp._ignoreCascade || dcp.cascade)) {
                            if (resourcesUtil.filterConnectorResources(resourceManager.getAllResources(), moduleName, true).size() > 0) {
                                String message = localStrings.getString("con.deployer.resources.exist", moduleName);
                                _logger.log(Level.WARNING, "resources.of.rar.exist", moduleName);

                                ActionReport report = dc.getActionReport();
                                report.setMessage(message);
                                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                            }
                        }
                    }
                }
            }
        }
    }
}
