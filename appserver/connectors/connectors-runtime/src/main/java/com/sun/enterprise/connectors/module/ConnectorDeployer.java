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

import com.sun.appserv.connectors.internal.api.ConnectorClassFinder;
import com.sun.appserv.connectors.internal.api.ConnectorConstants;
import com.sun.appserv.connectors.internal.api.ConnectorRuntimeException;
import com.sun.appserv.connectors.internal.api.ConnectorsUtil;
import com.sun.enterprise.config.serverbeans.*;
import com.sun.enterprise.connectors.ConnectorRegistry;
import com.sun.enterprise.connectors.ConnectorRuntime;
import com.sun.enterprise.connectors.util.ResourcesUtil;
import com.sun.enterprise.deployment.Application;
import com.sun.enterprise.deployment.ConnectorDescriptor;
import com.sun.enterprise.util.i18n.StringManager;
import com.sun.logging.LogDomains;
import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.api.deployment.MetaData;
import org.glassfish.api.deployment.OpsParams;
import org.glassfish.api.deployment.UndeployCommandParameters;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.api.event.EventListener;
import org.glassfish.api.event.Events;
import org.glassfish.connectors.config.*;
import org.glassfish.hk2.api.PostConstruct;
import org.glassfish.hk2.api.PreDestroy;
import org.glassfish.internal.api.ClassLoaderHierarchy;
import org.glassfish.internal.api.DelegatingClassLoader;
import org.glassfish.internal.api.Target;
import org.glassfish.internal.deployment.Deployment;
import org.glassfish.javaee.core.deployment.JavaEEDeployer;
import org.glassfish.resources.listener.ApplicationScopedResourcesManager;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

import javax.inject.Inject;
import javax.validation.*;
import javax.validation.bootstrap.GenericBootstrap;
import java.beans.PropertyVetoException;
import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Deployer for a resource-adapter.
 *
 * @author Jagadish Ramu
 */
@Service
public class ConnectorDeployer extends JavaEEDeployer<ConnectorContainer, ConnectorApplication>
        implements PostConstruct, PreDestroy, EventListener {

    @Inject
    private ConnectorRuntime runtime;

    @Inject
    private ClassLoaderHierarchy clh;

    @Inject
    private org.glassfish.resourcebase.resources.listener.ResourceManager resourceManager;

    @Inject
    private ApplicationScopedResourcesManager asrManager;

    @Inject
    private Domain domain;

    @Inject
    private ServerEnvironment env;

    private Resources resources;

    @Inject
    private Events events;
    
    @Inject
    private ConfigBeansUtilities configBeansUtilities;

    private static Logger _logger = LogDomains.getLogger(ConnectorDeployer.class, LogDomains.RSR_LOGGER);
    private static StringManager localStrings = StringManager.getManager(ConnectorRuntime.class);

    private static final String DOMAIN = "domain";
    private static final String EAR = "ear";

    public ConnectorDeployer() {
    }

    /**
     * Returns the meta data assocated with this Deployer
     *
     * @return the meta data for this Deployer
     */
    public MetaData getMetaData() {
        return new MetaData(false, null,
                new Class[]{Application.class});
    }

    /**
     * Loads the meta date associated with the application.
     *
     * @param type type of metadata that this deployer has declared providing.
     */
    public <T> T loadMetaData(Class<T> type, DeploymentContext context) {
        return null;
    }

    /**
     * Loads a previously prepared application in its execution environment and
     * return a ContractProvider instance that will identify this environment in
     * future communications with the application's container runtime.
     *
     * @param container in which the application will reside
     * @param context   of the deployment
     * @return an ApplicationContainer instance identifying the running application
     */
    @Override
    public ConnectorApplication load(ConnectorContainer container, DeploymentContext context) {
        super.load(container, context);
        File sourceDir = context.getSourceDir();
        String sourcePath = sourceDir.getAbsolutePath();
        String moduleName = sourceDir.getName();
        ConnectorDescriptor connDesc = 
            context.getModuleMetaData(ConnectorDescriptor.class);
        if (connDesc != null) {
            connDesc.setClassLoader(context.getClassLoader());
        }
        if(_logger.isLoggable(Level.FINEST)){
            _logger.finest("connector-descriptor during load : " + connDesc);
        }

        boolean isEmbedded = ConnectorsUtil.isEmbedded(context);
        ConnectorClassFinder ccf = null;
        ClassLoader classLoader = null;
        //this check is not needed as system-rars are never deployed, just to be safe.
        if (!ConnectorsUtil.belongsToSystemRA(moduleName)) {
            try {
                //for a connector deployer, classloader will always be ConnectorClassFinder
                classLoader =  context.getClassLoader();
                //for embedded .rar, compute the embedded .rar name
                if (isEmbedded) {
                    moduleName = ConnectorsUtil.getEmbeddedRarModuleName(
                            ConnectorsUtil.getApplicationName(context), moduleName);
                }

                //don't add the class-finder to the chain if its embedded .rar

                if (!(isEmbedded)) {
                    ccf = (ConnectorClassFinder) context.getClassLoader();
                    clh.getConnectorClassLoader(null).addDelegate(ccf);
                }

                registerBeanValidator(moduleName, context.getSource(), classLoader);

                runtime.createActiveResourceAdapter(connDesc, moduleName, sourcePath, classLoader);

            } catch (Exception cre) {
                Object params[] = new Object[]{moduleName, cre};
                _logger.log(Level.WARNING, "unable.to.load.ra", params);

                //since resource-adapter creation has failed, remove the class-loader for the RAR
                if (!(isEmbedded) && ccf != null) {
                    clh.getConnectorClassLoader(null).removeDelegate(ccf);
                }
                //since resource-adapter creation has failed, unregister bean validator of the RAR
                unregisterBeanValidator(moduleName);
                throw new RuntimeException(cre.getMessage(), cre);
            }
        }
        return new ConnectorApplication(moduleName, ConnectorsUtil.getApplicationName(context), resourceManager,
                asrManager, classLoader, runtime, events, connDesc);
    }



    /**
     * Unload or stop a previously running application identified with the
     * ContractProvider instance. The container will be stop upon return from this
     * method.
     *
     * @param appContainer instance to be stopped
     * @param context      of the undeployment
     */
    public void unload(ConnectorApplication appContainer, DeploymentContext context) {

        String moduleName = appContainer.getModuleName();

        try {
            runtime.destroyActiveResourceAdapter(moduleName);
        } catch (ConnectorRuntimeException e) {
            Object params[] = new Object[]{moduleName, e};
            _logger.log(Level.WARNING, "unable.to.unload.ra", params);
        } finally {

            //remove it only if it is not embedded
            if (!ConnectorsUtil.isEmbedded(context)) {
                //remove the class-finder (class-loader) from connector-class-loader chain
                DelegatingClassLoader dcl = clh.getConnectorClassLoader(null);
                for(DelegatingClassLoader.ClassFinder cf : dcl.getDelegates()){
                    ConnectorClassFinder ccf = (ConnectorClassFinder)cf;
                    if(ccf.getResourceAdapterName().equals(moduleName)){
                        dcl.removeDelegate(ccf);
                        break;
                    }
                }
            }

            unregisterBeanValidator(moduleName);
        }
    }

    /**
     * Clean any files and artifacts that were created during the execution
     * of the prepare method.
     *
     * @param dc deployment context
     */
    public void clean(DeploymentContext dc) {
        super.clean(dc);
        //delete resource configuration
        UndeployCommandParameters dcp = dc.getCommandParameters(UndeployCommandParameters.class);
        if (dcp != null && dcp.origin == OpsParams.Origin.undeploy) {
            if (dcp.cascade != null && dcp.cascade) {
                File sourceDir = dc.getSourceDir();
                String moduleName = sourceDir.getName();
                if (ConnectorsUtil.isEmbedded(dc)) {
                    String applicationName = ConnectorsUtil.getApplicationName(dc);
                    moduleName = ConnectorsUtil.getEmbeddedRarModuleName(applicationName, moduleName);
                }
                deleteAllResources(moduleName, dcp.target);
            }
        }
    }

    /**
     * deletes all resources (pool, resource, admin-object-resource, ra-config, work-security-map) of a resource-adapter)
     *
     * @param moduleName   resource-adapter name
     * @param targetServer target instance name
     */
    private void deleteAllResources(String moduleName, String targetServer) {

        Collection<ConnectorConnectionPool> conPools = ConnectorsUtil.getAllPoolsOfModule(moduleName, resources);
        Collection<String> poolNames = ConnectorsUtil.getAllPoolNames(conPools);
        Collection<Resource> connectorResources = ConnectorsUtil.getAllResources(poolNames, resources);
        Collection<AdminObjectResource> adminObjectResources = ResourcesUtil.createInstance().
                getEnabledAdminObjectResources(moduleName);
        Collection<WorkSecurityMap> securityMaps = ConnectorsUtil.getAllWorkSecurityMaps(resources, moduleName);
        ResourceAdapterConfig rac = ConnectorsUtil.getRAConfig(moduleName, resources);


        deleteConnectorResources(connectorResources, targetServer, moduleName);
        deleteConnectionPools(conPools, moduleName);
        deleteAdminObjectResources(adminObjectResources, targetServer, moduleName);
        deleteWorkSecurityMaps(securityMaps, moduleName);
        deleteRAConfig(rac);

    }

    private void deleteRAConfig(final ResourceAdapterConfig rac) {
        if (rac != null) {
            try {
                // delete resource-adapter-config
                if (ConfigSupport.apply(new SingleConfigCode<Resources>() {
                    public Object run(Resources param) throws PropertyVetoException, TransactionFailure {
                        return param.getResources().remove(rac);
                    }
                }, resources) == null) {
                    _logger.log(Level.WARNING, "unable.to.delete.rac", rac.getResourceAdapterName());
                }

            } catch (TransactionFailure tfe) {
                Object params[] = new Object[]{rac.getResourceAdapterName(), tfe};
                _logger.log(Level.WARNING, "unable.to.delete.rac.exception", params);
            }
        }
    }

    private void deleteWorkSecurityMaps(final Collection<WorkSecurityMap> workSecurityMaps, String raName) {
        if (workSecurityMaps.size() > 0) {
            try {
                // delete work-security-maps
                if (ConfigSupport.apply(new SingleConfigCode<Resources>() {

                    public Object run(Resources param) throws PropertyVetoException,
                            TransactionFailure {
                        for (WorkSecurityMap resource : workSecurityMaps) {
                            param.getResources().remove(resource);
                        }
                        return true; // indicating that removal was successful
                    }
                }, resources) == null) {
                    _logger.log(Level.WARNING, "unable.to.delete.work.security.map", raName);
                }

            } catch (TransactionFailure tfe) {
                Object params[] = new Object[]{raName, tfe};
                _logger.log(Level.WARNING, "unable.to.delete.work.security.map.exception", params);
            }

        }
    }

    private void deleteAdminObjectResources(final Collection<AdminObjectResource> adminObjectResources,
                                            final String target, String raName) {
        if (adminObjectResources != null && adminObjectResources.size() > 0) {
            try {

                //delete resource-refs
                for (AdminObjectResource resource : adminObjectResources) {
                    String jndiName = resource.getJndiName();
                    deleteResourceRef(jndiName, target);
                }

                // delete admin-object-resource
                if (ConfigSupport.apply(new SingleConfigCode<Resources>() {
                    public Object run(Resources param) throws PropertyVetoException, TransactionFailure {
                        for (AdminObjectResource resource : adminObjectResources) {
                            param.getResources().remove(resource);
                        }
                        // not found
                        return true;
                    }
                }, resources) == null) {
                    _logger.log(Level.WARNING, "unable.to.delete.admin.object", raName);
                }
            } catch (TransactionFailure tfe) {
                Object params[] = new Object[]{raName, tfe};
                _logger.log(Level.WARNING, "unable.to.delete.admin.object.exception", params);
            }

        }
    }

    private void deleteConnectorResources(final Collection<Resource> connectorResources, final String target,
                                          String raName) {
        if (connectorResources.size() > 0) {
            try {

                //delete resource-refs
                for (Resource resource : connectorResources) {
                    String jndiName = ((ConnectorResource) resource).getJndiName();
                    deleteResourceRef(jndiName, target);
                }

                // delete connector-resource
                if (ConfigSupport.apply(new SingleConfigCode<Resources>() {
                    public Object run(Resources param) throws PropertyVetoException, TransactionFailure {
                        for (Resource resource : connectorResources) {
                            param.getResources().remove(resource);
                        }
                        // not found
                        return true;
                    }
                }, resources) == null) {
                    _logger.log(Level.WARNING, "unable.to.delete.connector.resource", raName);
                }
            } catch (TransactionFailure tfe) {
                Object params[] = new Object[]{raName, tfe};
                _logger.log(Level.WARNING, "unable.to.delete.connector.resource.exception", params);
            }
        }
    }

    private void deleteResourceRef(String jndiName, String target) throws TransactionFailure {

        if (target.equals(DOMAIN)) {
            return ;
        }

        if( domain.getConfigNamed(target) != null){
            return ;
        }

        Server server = configBeansUtilities.getServerNamed(target);
        if (server != null) {
            if (server.isResourceRefExists(jndiName)) {
                // delete ResourceRef for Server
                server.deleteResourceRef(jndiName);
            }
        } else {
            Cluster cluster = domain.getClusterNamed(target);
            if(cluster != null){
                if (cluster.isResourceRefExists(jndiName)) {
                    // delete ResourceRef of Cluster
                    cluster.deleteResourceRef(jndiName);

                    // delete ResourceRef for all instances of Cluster
                    Target tgt = habitat.getService(Target.class);
                    List<Server> instances = tgt.getInstances(target);
                    for (Server svr : instances) {
                        if (svr.isResourceRefExists(jndiName)) {
                            svr.deleteResourceRef(jndiName);
                        }
                    }
                }
            }
        }
    }

    private void deleteConnectionPools(final Collection<ConnectorConnectionPool> conPools, String raName) {
        if (conPools.size() > 0) {
            // delete connector connection pool
            try {
                if (ConfigSupport.apply(new SingleConfigCode<Resources>() {
                    public Object run(Resources param) throws PropertyVetoException, TransactionFailure {
                        for (ConnectorConnectionPool cp : conPools) {
                            return param.getResources().remove(cp);
                        }
                        // not found
                        return null;
                    }
                }, resources) == null) {
                    _logger.log(Level.WARNING, "unable.to.delete.connector.connection.pool", raName);
                }
            } catch (TransactionFailure tfe) {
                Object params[] = new Object[]{raName, tfe};
                _logger.log(Level.WARNING, "unable.to.delete.connector.connection.pool.exception", params);
            }
        }
    }


    /**
     * The component has been injected with any dependency and
     * will be placed into commission by the subsystem.
     */
    public void postConstruct() {
        resources = domain.getResources();
        events.register(this);
    }

    public void logFine(String message) {
        if(_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE, message);
        }
    }

    private void registerBeanValidator(String rarName, ReadableArchive archive, ClassLoader classLoader) {

        ClassLoader contextCL = null;
        try {
            contextCL = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(classLoader);
            Validator beanValidator = null;
            ValidatorFactory validatorFactory = null;

            try {
                List<String> mappingsList = getValidationMappingDescriptors(archive);

                if (mappingsList.size() > 0) {
                    GenericBootstrap bootstrap = Validation.byDefaultProvider();
                    Configuration config = bootstrap.configure();

                    InputStream inputStream = null;
                    try {
                        for (String fileName : mappingsList) {
                            inputStream = archive.getEntry(fileName);
                            config.addMapping(inputStream);
                        }
                        validatorFactory = config.buildValidatorFactory();
                        ValidatorContext validatorContext = validatorFactory.usingContext();
                        beanValidator = validatorContext.getValidator();

                    } catch (IOException e) {
                        if(_logger.isLoggable(Level.FINE)) {
                            _logger.log(Level.FINE, "Exception while processing xml files for detecting " +
                                "bean-validation-mapping", e);
                        }
                    } finally {
                        try {
                            if (inputStream != null) {
                                inputStream.close();
                            }
                        } catch (Exception e) {
                            // ignore ?
                        }
                    }
                }

            } catch (Exception e) {
                Object params[] = new Object[]{rarName, e};
                _logger.log(Level.WARNING, "error.processing.xml.for.bean.validation.mapping", params);
            }
            if (beanValidator == null) {
                validatorFactory = Validation.byDefaultProvider().configure().buildValidatorFactory();
                beanValidator = validatorFactory.getValidator();
            }

            ConnectorRegistry registry = ConnectorRegistry.getInstance();
            registry.addBeanValidator(rarName, beanValidator);
        } finally {
            Thread.currentThread().setContextClassLoader(contextCL);
        }
    }

    private List<String> getValidationMappingDescriptors(ReadableArchive archive) {
        String validationMappingNSName = "jboss.org/xml/ns/javax/validation/mapping";

        Enumeration entries = archive.entries();
        List<String> mappingList = new ArrayList<String>();

        while (entries.hasMoreElements()) {

            String fileName = (String) entries.nextElement();
            if (fileName.toUpperCase(Locale.getDefault()).endsWith(".XML")) {
                BufferedReader reader = null;
                try {
                    InputStream is = archive.getEntry(fileName);
                    reader = new BufferedReader(new InputStreamReader(is));
                    String line;

                    while ((line = reader.readLine()) != null) {

                        if (line.contains(validationMappingNSName)) {
                            mappingList.add(fileName);
                            break;
                        }
                    }
                } catch (IOException e) {
                    if(_logger.isLoggable(Level.FINE)) {
                        _logger.log(Level.FINE, "Exception while processing xml file [ " + fileName + " ] " +
                            "for detecting bean-validation-mapping", e);
                    }
                } finally {
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (Exception e) {
							//ignore ?
                        }
                    }
                }
            }
        }
        return mappingList;
    }

    private void unregisterBeanValidator(String rarName){
        ConnectorRegistry registry = ConnectorRegistry.getInstance();
        registry.removeBeanValidator(rarName);
    }

    public void event(Event event) {

        // added this pre-check so as to validate whether connector-resources referring
        // the application (that has rar or is an standalone rar) are present.
        // Though similar validation is done in 'ConnectorApplication', this is to
        // handle the case where the application is not enabled in DAS (no ConnectorApplication)


        if (/*env.isDas() && */ Deployment.UNDEPLOYMENT_VALIDATION.equals(event.type())) {
            //this is an application undeploy event
            DeploymentContext dc = (DeploymentContext) event.hook();
            UndeployCommandParameters dcp = dc.getCommandParameters(UndeployCommandParameters.class);
            String appName = dcp.name;
            Boolean cascade = dcp.cascade;
            Boolean ignoreCascade = dcp._ignoreCascade;

            if (cascade != null && ignoreCascade != null) {
                if (cascade || ignoreCascade) {
                    return;
                }
            }

            com.sun.enterprise.config.serverbeans.Application app = domain.getApplications().getApplication(appName);
            boolean isRAR = false;
            if (app != null && Boolean.valueOf(app.getEnabled())) {
                isRAR = app.containsSnifferType(ConnectorConstants.CONNECTOR_MODULE);
            }

            if (!isRAR) {
                return;
            }

            boolean isAppRefEnabled = false;
            Server server = domain.getServers().getServer(env.getInstanceName());
            ApplicationRef appRef = server.getApplicationRef(appName);
            if (appRef != null && Boolean.valueOf(appRef.getEnabled())) {
                isAppRefEnabled = true;
            }

            if (isAppRefEnabled) {
                return;
            }
            boolean isEAR = app.containsSnifferType(EAR);
            String moduleName = appName;
            ResourcesUtil resourcesUtil = ResourcesUtil.createInstance();
            if (isEAR) {
                List<Module> modules = app.getModule();
                for (Module module : modules) {
                    moduleName = module.getName();
                    if (module.getEngine(ConnectorConstants.CONNECTOR_MODULE) != null) {
                        moduleName = appName + ConnectorConstants.EMBEDDEDRAR_NAME_DELIMITER + moduleName;
                        if (moduleName.toLowerCase(Locale.getDefault()).endsWith(".rar")) {
                            int index = moduleName.lastIndexOf(".rar");
                            moduleName = moduleName.substring(0, index);
                            if (resourcesUtil.filterConnectorResources
                                    (resourceManager.getAllResources(), moduleName, true).size() > 0) {
                                setFailureStatus(dc, moduleName);
                                return;
                            }
                        }
                    }
                }
            } else {
                if (resourcesUtil.filterConnectorResources
                        (resourceManager.getAllResources(), moduleName, true).size() > 0) {
                    setFailureStatus(dc, moduleName);
                }
            }
        }
    }

    private void setFailureStatus(DeploymentContext dc, String moduleName) {
        String message = localStrings.getString("con.deployer.resources.exist", moduleName);
        _logger.log(Level.WARNING, "resources.of.rar.exist", moduleName);

        ActionReport report = dc.getActionReport();
        report.setActionExitCode(ActionReport.ExitCode.FAILURE);
        report.setMessage(message);
    }

    public void preDestroy() {
        events.unregister(this);
    }
}
