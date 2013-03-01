/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.appserv.connectors.internal;

import java.beans.PropertyChangeEvent;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.naming.NamingException;

import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.CommandRunner;
import org.glassfish.api.admin.ParameterMap;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.naming.GlassfishNamingManager;
import org.glassfish.hk2.api.ServiceHandle;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.ClassLoaderHierarchy;
import org.glassfish.resourcebase.resources.api.PoolInfo;
import org.glassfish.resourcebase.resources.util.ResourceUtil;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.Changed;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.ConfigListener;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.NotProcessed;
import org.jvnet.hk2.config.UnprocessedChangeEvents;

import com.sun.appserv.connectors.internal.api.ConnectorConstants;
import com.sun.appserv.connectors.internal.api.ConnectorDescriptorProxy;
import com.sun.appserv.connectors.internal.api.ConnectorRuntime;
import com.sun.appserv.connectors.internal.api.ConnectorsUtil;
import com.sun.enterprise.config.serverbeans.Application;
import com.sun.enterprise.config.serverbeans.Applications;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.Module;
import com.sun.enterprise.config.serverbeans.Resource;
import com.sun.enterprise.config.serverbeans.ResourcePool;
import com.sun.enterprise.config.serverbeans.Resources;
import com.sun.logging.LogDomains;
import org.glassfish.internal.api.InternalSystemAdministrator;


/**
 * ResourceManager lifecycle listener that listens to resource-manager startup and shutdown
 * and does connector related work. eg: binding connector proxies.</br>
 * Also, does ping-connection-pool for application and module scoped resources (if ping=true)
 * @author Jagadish Ramu
 */
@Service
@Singleton
public class ConnectorResourceManagerLifecycleListener implements org.glassfish.resourcebase.resources.listener.ResourceManagerLifecycleListener, ConfigListener {

    @Inject
    private GlassfishNamingManager namingMgr;

    @Inject
    private Provider<ConnectorDescriptorProxy> connectorDescriptorProxyProvider;

    @Inject
    private Provider<CommandRunner> commandRunnerProvider;

    @Inject
    private Provider<ActionReport> actionReportProvider;

    @Inject
    private Provider<ConnectorRuntime> connectorRuntimeProvider;

    @Inject
    private Domain domain;

    @Inject
    private Applications applications;

    @Inject
    private ServiceLocator connectorRuntimeHabitat;

    private ConnectorRuntime runtime;

    @Inject
    private ClassLoaderHierarchy clh;

    @Inject
    private ServerEnvironment serverEnvironment;
    
    @Inject 
    private InternalSystemAdministrator internalSystemAdministrator;

    private static final Logger logger =
            LogDomains.getLogger(ConnectorRuntime.class, LogDomains.RESOURCE_BUNDLE);

    private void bindConnectorDescriptors() {
        for(String rarName : ConnectorConstants.systemRarNames){
            bindConnectorDescriptorProxies(rarName);
        }
    }

    private void bindConnectorDescriptorProxies(String rarName) {
        //these proxies are needed as appclient container may lookup descriptors
        String jndiName = ConnectorsUtil.getReservePrefixedJNDINameForDescriptor(rarName);
        ConnectorDescriptorProxy proxy = connectorDescriptorProxyProvider.get();
        proxy.setJndiName(jndiName);
        proxy.setRarName(rarName);
        try {
            namingMgr.publishObject(jndiName, proxy, true);
        } catch (NamingException e) {
            Object[] params = new Object[]{rarName, e};
            logger.log(Level.WARNING, "resources.resource-manager.connector-descriptor.bind.failure", params);
        }
    }

    private ConnectorRuntime getConnectorRuntime() {
        if(runtime == null){
            runtime = connectorRuntimeProvider.get();
        }
        return runtime;
    }

    /**
     * Check whether connector-runtime is initialized.
     * @return boolean representing connector-runtime initialization status.
     */
    public boolean isConnectorRuntimeInitialized() {
        List<ServiceHandle<ConnectorRuntime>> serviceHandles =
                connectorRuntimeHabitat.getAllServiceHandles(ConnectorRuntime.class);
        for(ServiceHandle<ConnectorRuntime> inhabitant : serviceHandles){
            // there will be only one implementation of connector-runtime
            return inhabitant.isActive();
        }
        return true; // to be safe
    }

    public void resourceManagerLifecycleEvent(EVENT event){
        if(EVENT.STARTUP.equals(event)){
            resourceManagerStarted();
        }else if(EVENT.SHUTDOWN.equals(event)){
            resourceManagerShutdown();
        }
    }

    public void resourceManagerStarted() {
        bindConnectorDescriptors();
    }

    public void resourceManagerShutdown() {
        if (isConnectorRuntimeInitialized()) {
            ConnectorRuntime cr = getConnectorRuntime();
            if (cr != null) {
                // clean up will take care of any system RA resources, pools
                // (including pools via datasource-definition)
                cr.cleanUpResourcesAndShutdownAllActiveRAs();
            }
        } else {
            if(logger.isLoggable(Level.FINEST)) {
                logger.finest("ConnectorRuntime not initialized, hence skipping " +
                    "resource-adapters shutdown, resources, pools cleanup");
            }
        }
    }

    public UnprocessedChangeEvents changed(PropertyChangeEvent[] events) {
            return ConfigSupport.sortAndDispatch(events, new ConfigChangeHandler(), logger);
    }

    class ConfigChangeHandler implements Changed {

        private ConfigChangeHandler() {
        }

        /**
         * Notification of a change on a configuration object
         *
         * @param type            CHANGE means the changedInstance has mutated.
         * @param changedType     type of the configuration object
         * @param changedInstance changed instance.
         */
        public <T extends ConfigBeanProxy> NotProcessed changed(Changed.TYPE type, Class<T> changedType,
                                                                T changedInstance) {
            NotProcessed np = null;
            if(!(changedInstance instanceof Application)){
                return np;
            }
            if(serverEnvironment.isDas()){
                ClassLoader contextCL = Thread.currentThread().getContextClassLoader();
                try {
                    //use connector-class-loader so as to get access to classes from resource-adapters
                    ClassLoader ccl = clh.getConnectorClassLoader(null);
                    Thread.currentThread().setContextClassLoader(ccl);
                    switch (type) {
                        case ADD:
                            np = handleAddEvent(changedInstance);
                            break;
                        default:
                            break;
                    }
                } finally {
                    Thread.currentThread().setContextClassLoader(contextCL);
                }
            }
            return np;
        }
        private <T extends ConfigBeanProxy> NotProcessed handleAddEvent(T instance) {
            NotProcessed np = null;
            if(instance instanceof Application){
                Resources resources = ((Application)instance).getResources();
                pingConnectionPool(resources);

                Application app = (Application)instance;
                List<Module> modules = app.getModule();
                if(modules != null){
                    for(Module module : modules){
                        if(module.getResources() !=null && module.getResources().getResources() != null){
                            pingConnectionPool(module.getResources());
                        }
                    }
                }
            }
            return np;
        }

        private void pingConnectionPool(Resources resources) {
            if(resources != null){
                if(resources.getResources() != null){
                    for(Resource resource : resources.getResources()){
                        if(resource instanceof ResourcePool){
                            ResourcePool pool = (ResourcePool)resource;
                            if(Boolean.valueOf(pool.getPing())){
                                PoolInfo poolInfo = ResourceUtil.getPoolInfo(pool);
                                CommandRunner commandRunner = commandRunnerProvider.get();
                                ActionReport report = actionReportProvider.get();
                                CommandRunner.CommandInvocation invocation =
                                        commandRunner.getCommandInvocation("ping-connection-pool", report, internalSystemAdministrator.getSubject());
                                ParameterMap params = new ParameterMap();
                                params.add("appname",poolInfo.getApplicationName());
                                params.add("modulename",poolInfo.getModuleName());
                                params.add("DEFAULT", poolInfo.getName());
                                invocation.parameters(params).execute();
                                if(report.getActionExitCode() == ActionReport.ExitCode.SUCCESS){
                                    logger.log(Level.INFO, "app-scoped.ping.connection.pool.success", poolInfo);
                                }else{
                                    Object args[] = new Object[]{poolInfo, report.getFailureCause()};
                                    logger.log(Level.WARNING, "app-scoped.ping.connection.pool.failed", args);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
