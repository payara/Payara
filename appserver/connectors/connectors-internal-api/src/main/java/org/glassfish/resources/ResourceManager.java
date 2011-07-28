/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.resources;

import com.sun.appserv.connectors.internal.api.ConnectorDescriptorProxy;
import com.sun.appserv.connectors.internal.api.ResourcesBinder;
import org.glassfish.resource.common.ResourceInfo;
import org.glassfish.api.admin.ServerEnvironment;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.component.*;
import org.jvnet.hk2.config.*;
import org.jvnet.hk2.config.types.Property;
import org.glassfish.api.naming.GlassfishNamingManager;
import org.glassfish.internal.api.*;

import java.beans.PropertyChangeEvent;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.*;

import com.sun.enterprise.config.serverbeans.*;
import com.sun.appserv.connectors.internal.api.ConnectorsUtil;
import com.sun.appserv.connectors.internal.api.ConnectorRuntime;
import com.sun.appserv.connectors.internal.api.ConnectorConstants;
import com.sun.appserv.connectors.internal.spi.ResourceDeployer;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.logging.LogDomains;
import org.jvnet.hk2.config.ObservableBean;

import javax.naming.NamingException;

/**
 * Resource manager to bind various resources during start-up, create/update/delete of resource/pool
 * @author Jagadish Ramu
 */
@Scoped(Singleton.class)
@Service(name="ResourceManager") // this name is used in ApplicationLoaderService
public class ResourceManager implements PostStartup, PostConstruct, PreDestroy, ConfigListener {

    private static final Logger logger =
            LogDomains.getLogger(ResourceManager.class,LogDomains.RESOURCE_BUNDLE);

    private static LocalStringManagerImpl localStrings =
        new LocalStringManagerImpl(ResourceManager.class);


    @Inject
    private ResourcesBinder resourcesBinder;

    @Inject
    private Habitat connectorRuntimeHabitat;

    @Inject
    private Habitat deployerHabitat;

    @Inject
    private Habitat habitat;

    private ConnectorRuntime runtime;

    @Inject
    private GlassfishNamingManager namingMgr;

    @Inject
    private ServerEnvironment environment;

    @Inject
    private Domain domain;

    //Listen to config changes of resource refs.
    //@Inject
    //private ResourceRef[] resourceRefs;
    //private Server server;


    public void postConstruct() {
        bindConnectorDescriptors();
        deployResources(domain.getResources().getResources());
        addListenerToResources();
        addListenerToResourceRefs();
        addListenerToServer();
    }

    private void addListenerToResources(){
        Resources resources = domain.getResources();
        ObservableBean bean = (ObservableBean) ConfigSupport.getImpl((ConfigBeanProxy)resources);
        bean.addListener(this);
    }

    private void addListenerToServer() {
        Server server = domain.getServerNamed(environment.getInstanceName());
        ObservableBean bean = (ObservableBean) ConfigSupport.getImpl((ConfigBeanProxy)server);
        bean.addListener(this);
    }

    private void bindConnectorDescriptors() {
        for(String rarName : ConnectorConstants.systemRarNames){
            bindConnectorDescriptorProxies(rarName);
        }
    }

    private void bindConnectorDescriptorProxies(String rarName) {
        //these proxies are needed as appclient container may lookup descriptors
        String jndiName = ConnectorsUtil.getReservePrefixedJNDINameForDescriptor(rarName);
        ConnectorDescriptorProxy proxy = habitat.getComponent(ConnectorDescriptorProxy.class);
        proxy.setJndiName(jndiName);
        proxy.setRarName(rarName);
        try {
            namingMgr.publishObject(jndiName, proxy, true);
        } catch (NamingException e) {
            Object[] params = new Object[]{rarName, e};
            logger.log(Level.WARNING, "resources.resource-manager.connector-descriptor.bind.failure", params);
        }
    }

    /**
     * deploy resources
     * @param resources list
     */
    public void deployResources(Collection<Resource> resources){
        for(Resource resource : resources){
            if(resource instanceof BindableResource){
                BindableResource bindableResource = (BindableResource)resource;
                if(isBindableResourceEnabled(bindableResource.getJndiName())){
                    ResourceInfo resourceInfo = new ResourceInfo(bindableResource.getJndiName());
                    resourcesBinder.deployResource(resourceInfo, resource);
                }
            } else if (resource instanceof ResourcePool) {
                // ignore, as they are loaded lazily
            } else{
                // only other resource types left are RAC, CWSM
                try{
                    getResourceDeployer(resource).deployResource(resource);
                }catch(Exception e){
                    Object[] params = {ConnectorsUtil.getGenericResourceInfo(resource), e};
                    logger.log(Level.WARNING, "resources.resource-manager.deploy-resource-failed", params);
                }
            }
        }
        /* TODO V3 
            will there be a chance of double listener registration for a resource ?
            eg: allresources added during startup, resources of a particular
            connector during connector startup / redeploy ?
        */
        addListenerToResources(resources);
    }

    private boolean isBindableResourceEnabled(String jndiName) {
        boolean resourceEnabled = false;
        BindableResource res = (BindableResource)
                ConnectorsUtil.getResourceByName(domain.getResources(), BindableResource.class, jndiName);
        if(res != null){
            resourceEnabled = Boolean.valueOf(res.getEnabled());
        }

        boolean refEnabled = false;
        if(resourceEnabled){
            ResourceRef ref = domain.getServerNamed(environment.getInstanceName()).getResourceRef(jndiName);
            if(ref != null){
                refEnabled = Boolean.valueOf(ref.getEnabled());
            }
        }
        return resourceEnabled && refEnabled;
    }

    public Resources getAllResources(){
        return domain.getResources();
    }


    /**
     * Do cleanup of system-resource-adapter, resources, pools 
     */
    public void preDestroy() {

        if (isConnectorRuntimeInitialized()) {
            Collection<Resource> resources = ConnectorsUtil.getAllSystemRAResourcesAndPools(domain.getResources());

            undeployResources(resources);
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
        removeListenerForAllResources();
        removeListenerForResources();
        removeListenerForResourceRefs();
        removeListenerForServer();
    }

    private void removeListenerForServer() {
        Server server  = domain.getServerNamed(environment.getInstanceName());
        ObservableBean bean = (ObservableBean) ConfigSupport.getImpl((ConfigBeanProxy)server);
        bean.removeListener(this);
    }

    private ConnectorRuntime getConnectorRuntime() {
        if(runtime == null){
            runtime = connectorRuntimeHabitat.getComponent(ConnectorRuntime.class, null);
        }
        return runtime;
    }

    /**
     * undeploy the given set of resources<br>
     * <b>care has to be taken for the case of dependent resources<br>
     * eg : all resources need to be undeployed <br>
     * before undeploying the pool that they refer to</b>
     * @param resources list of resources
     */
    public void undeployResources(Collection<Resource> resources){
        for(Resource resource : resources){
            try{
                getResourceDeployer(resource).undeployResource(resource);
            }catch(Exception e){
                Object[] params = {ConnectorsUtil.getGenericResourceInfo(resource), e};
                logger.log(Level.WARNING, "resources.resource-manager.undeploy-resource-failed", params);
            }finally{
                removeListenerForResource(resource);
            }
        }
    }

    /**
     * Notification that @Configured objects that were injected have changed
     *
     * @param events list of changes
     */
    public UnprocessedChangeEvents changed(PropertyChangeEvent[] events) {
        return ConfigSupport.sortAndDispatch(events, new PropertyChangeHandler(events), logger);
    }

    /**
     * Check whether connector-runtime is initialized.
     * @return boolean representing connector-runtime initialization status.
     */
    public boolean isConnectorRuntimeInitialized() {
        Collection<Inhabitant<? extends ConnectorRuntime>> inhabitants =
                connectorRuntimeHabitat.getInhabitants(ConnectorRuntime.class);
        for(Inhabitant inhabitant : inhabitants){
            // there will be only one implementation of connector-runtime
            return inhabitant.isActive();
        }
        return true; // to be safe
    }

    class PropertyChangeHandler implements Changed {
        
        PropertyChangeEvent[] events;

        private PropertyChangeHandler(PropertyChangeEvent[] events) {
            this.events = events;
        }

        /**
         * Notification of a change on a configuration object
         *
         * @param type            type of change : ADD mean the changedInstance was added to the parent
         *                        REMOVE means the changedInstance was removed from the parent, CHANGE means the
         *                        changedInstance has mutated.
         * @param changedType     type of the configuration object
         * @param changedInstance changed instance.
         */
        public <T extends ConfigBeanProxy> NotProcessed changed(TYPE type, Class<T> changedType, T changedInstance) {
            NotProcessed np = null;
            ClassLoader contextCL = Thread.currentThread().getContextClassLoader();
            try {
                //use connector-classloader so as to get access to classes from resource-adapters
                ConnectorRuntime cc = getConnectorRuntime();
                if (cc==null) return null;
                ClassLoader ccl = cc.getConnectorClassLoader();
                Thread.currentThread().setContextClassLoader(ccl);
                switch (type) {
                    case ADD:
                        if(logger.isLoggable(Level.FINE)) {
                            logger.fine("A new " + changedType.getName() + " was added : " + changedInstance);
                        }
                        np = handleAddEvent(changedInstance);
                        break;

                    case CHANGE:
                        if(logger.isLoggable(Level.FINE)) {
                            logger.fine("A " + changedType.getName() + " was changed : " + changedInstance);
                        }
                        np = handleChangeEvent(changedInstance);
                        break;

                    case REMOVE:
                        if(logger.isLoggable(Level.FINE)) {
                            logger.fine("A " + changedType.getName() + " was removed : " + changedInstance);
                        }
                        np = handleRemoveEvent(changedInstance);
                        break;

                    default:
                        np = new NotProcessed("Unrecognized type of change: " + type);
                        break;
                }
                return np;
            } finally {
                Thread.currentThread().setContextClassLoader(contextCL);
            }
        }

        private <T extends ConfigBeanProxy> NotProcessed handleChangeEvent(T instance) {
            NotProcessed np = null;
            //TODO V3 handle enabled / disabled / resource-ref / redeploy ?
            try {
                if (ConnectorsUtil.isValidEventType(instance)) {
                    boolean enabledAttributeChange = false;
                    if (instance instanceof BindableResource) {
                        for (PropertyChangeEvent event : events) {
                            String propertyName = event.getPropertyName();
                            //Depending on the type of event (disable/enable, invoke the
                            //method on deployer.
                            if ("enabled".equalsIgnoreCase(propertyName)) {
                                enabledAttributeChange = true;
                                BindableResource bindableResource = (BindableResource) instance;
                                ResourceDeployer deployer = getResourceDeployer(bindableResource);
                                if (deployer != null) {
                                    boolean newValue = ConnectorsUtil.parseBoolean(event.getNewValue().toString());
                                    boolean oldValue = ConnectorsUtil.parseBoolean(event.getOldValue().toString());
                                    if (!(newValue && oldValue)) {
                                        if (newValue) {
                                            deployer.enableResource(bindableResource);
                                        } else {
                                            deployer.disableResource(bindableResource);
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (!enabledAttributeChange) {
                        getResourceDeployer(instance).redeployResource(instance);
                    }
                } else if (ConnectorsUtil.isValidEventType(instance.getParent())) {
                    //Added in case of a property change
                    //check for validity of the property's parent and redeploy
                    getResourceDeployer(instance.getParent()).redeployResource(instance.getParent());
                } else if (instance instanceof ResourceRef) {
                    ResourceRef ref = (ResourceRef) instance;
                    ResourceDeployer deployer = null;
                    String refName = ref.getRef();
                    BindableResource bindableResource = null;

                    for(PropertyChangeEvent event : events) {
                        String propertyName = event.getPropertyName();
                        //Depending on the type of event (disable/enable, invoke the 
                        //method on deployer.
                        if ("enabled".equalsIgnoreCase(propertyName)) {
                            bindableResource = (BindableResource)
                                    ConnectorsUtil.getResourceByName(domain.getResources(), BindableResource.class, refName);
                            deployer = getResourceDeployer(bindableResource);
                            if (deployer != null) {
                                //both cannot be true or false
                                boolean newValue = ConnectorsUtil.parseBoolean(event.getNewValue().toString());
                                boolean oldValue = ConnectorsUtil.parseBoolean(event.getOldValue().toString());
                                if (!(newValue && oldValue)) {
                                    if (newValue) {
                                        deployer.enableResource(bindableResource);
                                    } else {
                                        deployer.disableResource(bindableResource);
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "resources.resource-manager.change-event-failed", ex);
                np = new NotProcessed(
                        localStrings.getLocalString(
                        "resources.resource-manager.change-event-failed",
                        "Change event failed"));
            }
            return np;
        }

        private <T extends ConfigBeanProxy> NotProcessed handleAddEvent(T instance) {
            NotProcessed np = null;
            //Add listener to the changed instance object
            ResourceManager.this.addListenerToResource(instance);

            if (instance instanceof BindableResource) {
                //ignore - as bindable-resources are handled during resource-ref creation
            } else if (instance instanceof ResourcePool) {
                //ignore - as pools are handled lazily
            } else if (instance instanceof Resource) {
                //only resource type left are RAC and CWSM
                try {
                    getResourceDeployer(instance).deployResource(instance);
                } catch (Exception e) {
                    Object params[] = {ConnectorsUtil.getGenericResourceInfo((Resource) instance), e};
                    logger.log(Level.WARNING, "resources.resource-manager.deploy-resource-failed", params);
                }
            } else if (instance instanceof Property) {
                //Property is not handled here. It is handled as part of the
                //Change event of the resource. 
            } else if (instance instanceof ResourceRef) {
                //create-resource-ref
                ResourceRef ref = (ResourceRef)instance;
                BindableResource resource = (BindableResource)
                        ConnectorsUtil.getResourceByName(domain.getResources(), BindableResource.class, ref.getRef());
                if(Boolean.valueOf(ref.getEnabled()) && Boolean.valueOf(resource.getEnabled())){
                    ResourceInfo resourceInfo = new ResourceInfo(resource.getJndiName());
                    resourcesBinder.deployResource(resourceInfo, resource);
                }
            }
            return np;
        }

        private <T extends ConfigBeanProxy> NotProcessed handleRemoveEvent(final T instance) {
            NotProcessed np = null;
            try {
                //this check ensures that a valid resource is handled
                if (instance instanceof BindableResource) {
                    //ignore as bindable-resources will have resource-ref.
                    ResourceManager.this.removeListenerForResource(instance);
                } else if(instance instanceof Resource){
                    //Remove listener from the removed instance
                    ResourceManager.this.removeListenerForResource(instance);
                    //get appropriate deployer and undeploy resource
                    getResourceDeployer(instance).undeployResource(instance);
                } else if (ConnectorsUtil.isValidEventType(instance.getParent())) {
                    //Added in case of a property remove
                    //check for validity of the property's parent and redeploy
                    getResourceDeployer(instance.getParent()).redeployResource(instance.getParent());
                } else if (instance instanceof ResourceRef) {
                    //delete-resource-ref
                    ResourceRef ref = (ResourceRef)instance;
                    BindableResource resource = (BindableResource)
                            ConnectorsUtil.getResourceByName(domain.getResources(), BindableResource.class, ref.getRef());
                    //get appropriate deployer and undeploy resource
                    getResourceDeployer(resource).undeployResource(resource);
                    //Remove listener from the removed instance
                    ResourceManager.this.removeListenerForResource(instance);
                }
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "resources.resource-manager.remove-event-failed");
                np = new NotProcessed(
                        localStrings.getLocalString(
                        "resources.resource-manager.remove-event-failed",
                        "Remove event failed"));
            }
            return np;
        }
    }


    /**
     * Add listener to all resources (JDBC Connection Pool/JDBC Resource/
     * Connector Connection Pool/Connector Resource.
     * Invoked from postConstruct()
     * @param resources list of resources for which listeners will be registered.
     */
    private void addListenerToResources(Collection<Resource> resources) {
        for (Resource configuredResource : resources) {
            addListenerToResource(configuredResource);
        }
    }

    private void addListenerToResourceRefs() {
        for (ResourceRef ref : getResourceRefs()) {
            addListenerToResource(ref);
        }
    }

    private List<ResourceRef> getResourceRefs() {
        //Instead of injecting ResourceRef[] config array (which will inject all resource-refs in domain.xml
        //including the ones in other server instances), get appropriate instance's resource-refs alone.
        return  domain.getServerNamed(environment.getInstanceName()).getResourceRef();
    }

    /**
     * Add listener to a generic resource
     * Used in the case of create asadmin command when listeners have to
     * be added to the specific resource
     * @param instance instance to which listener will be registered
     */
    private void addListenerToResource(Object instance) {
        ObservableBean bean = null;

        //add listener to all types of Resource
        if(instance instanceof Resource){
            bean = (ObservableBean) ConfigSupport.getImpl((ConfigBeanProxy)instance);
            bean.addListener(this);
        } else if(instance instanceof ResourceRef) {
            bean = (ObservableBean) ConfigSupport.getImpl((ConfigBeanProxy)instance);
            bean.addListener(this);
        }
    }


    /**
     * Remove listener from a generic resource (JDBC Connection Pool/Connector 
     * Connection Pool/JDBC resource/Connector resource)
     * Used in the case of delete asadmin command
     * @param instance remove the resource from listening to resource events
     */
    private void removeListenerForResource(Object instance) {
        ObservableBean bean = null;

        if(instance instanceof Resource){
            bean = (ObservableBean) ConfigSupport.getImpl((ConfigBeanProxy)instance);
            bean.removeListener(this);
        } else if(instance instanceof ResourceRef) {
            bean = (ObservableBean) ConfigSupport.getImpl((ConfigBeanProxy) instance);
            bean.removeListener(this);
        }
    }

    /**
     * Remove listener from all resource refs.
     * Invoked from preDestroy().
     */
    private void removeListenerForResourceRefs() {
        for (ResourceRef ref : getResourceRefs()) {
            removeListenerForResource(ref);
        }
    }

    private void removeListenerForResources(){
        ObservableBean bean = (ObservableBean)ConfigSupport.getImpl((ConfigBeanProxy)domain.getResources());
        bean.removeListener(this);
    }
    /**
     * Remove listener from all resources - JDBC Connection Pools/JDBC Resources/
     * Connector Connection Pools/ Connector Resources.
     * Invoked from preDestroy()
     */
    private void removeListenerForAllResources() {
        for (Resource configuredResource : domain.getResources().getResources()) {
            removeListenerForResource(configuredResource);
        }
    }

    /**
     * Given a <i>resource</i> instance, appropriate deployer will be provided
     *
     * @param resource resource instance
     * @return ResourceDeployer
     */
    private ResourceDeployer getResourceDeployer(Object resource){
        Collection<ResourceDeployer> deployers = deployerHabitat.getAllByContract(ResourceDeployer.class);

        for(ResourceDeployer deployer : deployers){
            if(deployer.handles(resource)){
                return deployer;
            }
        }
        return null;
    }
}
