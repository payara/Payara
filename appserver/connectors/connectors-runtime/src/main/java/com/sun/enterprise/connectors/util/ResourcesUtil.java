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

package com.sun.enterprise.connectors.util;

import com.sun.appserv.connectors.internal.api.ConnectorConstants;
import com.sun.appserv.connectors.internal.api.ConnectorRuntimeException;
import com.sun.appserv.connectors.internal.api.ConnectorsUtil;
import com.sun.enterprise.config.serverbeans.*;
import com.sun.enterprise.connectors.ConnectorRuntime;
import com.sun.enterprise.connectors.ConnectorRuntimeExtension;
import com.sun.enterprise.connectors.DeferredResourceConfig;
import com.sun.enterprise.deploy.shared.FileArchive;
import com.sun.enterprise.deployment.ConnectorDescriptor;
import com.sun.enterprise.deployment.archivist.ApplicationArchivist;
import com.sun.enterprise.util.i18n.StringManager;
import com.sun.logging.LogDomains;
import org.glassfish.connectors.config.*;
import org.glassfish.internal.api.Globals;
import org.glassfish.internal.api.RelativePathResolver;
import org.glassfish.internal.api.ServerContext;
import org.glassfish.resources.api.ResourcesRegistry;
import org.glassfish.resourcebase.resources.api.PoolInfo;
import org.glassfish.resourcebase.resources.api.ResourceInfo;
import org.jvnet.hk2.config.ConfigBeanProxy;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


public class ResourcesUtil {

/*
    //The thread local ResourcesUtil is used in two cases
    //1. An event config context is to be used as in case of resource
    //   deploy/undeploy and enable/disable events.
    //2. An admin config context to be used for ConnectorRuntime.getConnection(...)
    //   request
    static ThreadLocal<ResourcesUtil> localResourcesUtil =
            new ThreadLocal<ResourcesUtil>();

*/
    static Logger _logger = LogDomains.getLogger(ResourcesUtil.class,LogDomains.RSR_LOGGER);

    static StringManager localStrings =
            StringManager.getManager(ResourcesUtil.class);

    static ServerContext sc_ = null;

    protected Domain domain = null;

    protected Resources resources = null;

    private ConnectorRuntime runtime;

    private Server server;

    private volatile static ResourcesUtil resourcesUtil;

    private ResourcesUtil(){
    }

    public Resources getGlobalResources(){
        return getRuntime().getResources();
    }
    public Resources getResources(ResourceInfo resourceInfo) {
        return getRuntime().getResources(resourceInfo);
    }

    public Resources getResources(PoolInfo poolInfo) {
        return getRuntime().getResources(poolInfo);
    }

    private Domain getDomain(){
        if(domain == null){
            domain = getRuntime().getDomain();
        }
        return domain;
    }

    private ConnectorRuntime getRuntime(){
        if(runtime == null){
            runtime = ConnectorRuntime.getRuntime();
        }
        return runtime;
    }

    private Server getServer(){
        if(server == null){
            server = getDomain().getServerNamed(getRuntime().getServerEnvironment().getInstanceName());
        }
        return server;
    }

    private Applications getApplications(){
        return getRuntime().getApplications();
    }

    private Application getApplicationByName(String name){
        Application application = null;
        List<Application> apps = getApplications().getApplications();
        for(Application app : apps){
            if(app.getName().equals(name)){
                application = app;
                break;
            }
        }
        return application;
    }
    /**
     * Gets the deployment location for a J2EE application.
     * @param appName application name
     * @return application deploy location
     */
    public String getApplicationDeployLocation(String appName) {
        String location = null;
        Application app = getApplicationByName(appName);
        if(app != null){
            //TODO V3 with annotations, is this right location ?
            location = RelativePathResolver.resolvePath(app.getLocation());
        }
        return location;
    }


    public boolean belongToStandAloneRar(String resourceAdapterName) {
        return false;
    }

    public static ResourcesUtil createInstance() {
        //stateless, no synchronization needed
        if(resourcesUtil == null){
            synchronized(ResourcesUtil.class) {
                if(resourcesUtil == null) {
                    resourcesUtil = new ResourcesUtil();
                }
            }
        }
        return resourcesUtil;
    }

/*    public DeferredResourceConfig getDeferredResourceConfig(ResourceInfo resourceInfo) {
        DeferredResourceConfig resConfig = getDeferredConnectorResourceConfigs(resourceInfo);
        if(resConfig != null) {
            return resConfig;
        }

        resConfig = getDeferredJdbcResourceConfigs(resourceInfo);

        if(resConfig != null) {
            return resConfig;
        }

        resConfig = getDeferredAdminObjectConfigs(resourceInfo);

        return resConfig;
    }
*/


/*    public DeferredResourceConfig getDeferredPoolConfig(PoolInfo poolInfo) {

        DeferredResourceConfig resConfig = getDeferredConnectorPoolConfigs(poolInfo);
        if(resConfig != null) {
            return resConfig;
        }

        if(poolInfo == null){
            return null;
        }

        resConfig = getDeferredJdbcPoolConfigs(poolInfo);

        return resConfig;
    }
*/

    public DeferredResourceConfig getDeferredResourceConfig(Object resource, Object pool, String resType, String raName)
            throws ConnectorRuntimeException {
        String resourceAdapterName ;
        DeferredResourceConfig resConfig = null;
        //TODO V3 there should not be res-type related check, refactor deferred-ra-config
        //TODO V3 (not to hold specific resource types)
        if (resource instanceof ConnectorResource || pool instanceof ConnectorConnectionPool) {
            ConnectorConnectionPool connPool = (ConnectorConnectionPool) pool;
            ConnectorResource connResource = (ConnectorResource) resource;
            resourceAdapterName = connPool.getResourceAdapterName();

            //TODO V3 need to get AOR & RA-Config later
            resConfig = new DeferredResourceConfig(resourceAdapterName, null, connPool, connResource, null);

            Resource[] resourcesToload = new Resource[]{connPool, connResource};
            resConfig.setResourcesToLoad(resourcesToload);

        } else {
            Collection<ConnectorRuntimeExtension> extensions =
                    Globals.getDefaultHabitat().getAllServices(ConnectorRuntimeExtension.class);
            for (ConnectorRuntimeExtension extension : extensions) {
                return extension.getDeferredResourceConfig(resource, pool, resType, raName);
            }
        }
        return resConfig;
    }

/*    public DeferredResourceConfig getDeferredJdbcResourceConfig(JdbcResource resource, JdbcConnectionPool pool) {
        DeferredResourceConfig resConfig = null;

        if (parseBoolean(resource.getEnabled())){
            String rarName = getRANameofJdbcConnectionPool(pool);
            resConfig = new DeferredResourceConfig(rarName, null, null, null, pool, resource, null);
            Resource[] resourcesToload = new Resource[]{pool, resource};
            resConfig.setResourcesToLoad(resourcesToload);
        }
        return resConfig;
    }
*/

    /**
     * Returns the deferred connector resource config. This can be resource of JMS RA which is lazily
     * loaded. Or for other connector RA which is not loaded at start-up. The connector RA which does
     * not have any resource or admin object associated with it are not loaded at start-up. They are
     * all lazily loaded.
     */
/*    protected DeferredResourceConfig getDeferredConnectorResourceConfigs(ResourceInfo resourceInfo) {

        if (resourceInfo == null) {
            return null;
        }
        Resource[] resourcesToload = new Resource[2];

        try {
            if (!isReferenced(resourceInfo)) {
                return null;
            }
        } catch (Exception e) {
            String message = localStrings.getString(
                    "error.finding.resources.references",
                    resourceInfo);
            _logger.log(Level.WARNING, message + e.getMessage());
            if(_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE, message + e.getMessage(), e);
            }
        }


        ConnectorResource connectorResource = (ConnectorResource)
                ConnectorsUtil.getResourceByName(getResources(resourceInfo), ConnectorResource.class, resourceInfo.getName());
        if (connectorResource == null || !ConnectorsUtil.parseBoolean(connectorResource.getEnabled())) {
            return null;
        }
        String poolName = connectorResource.getPoolName();
        ConnectorConnectionPool ccPool = (ConnectorConnectionPool)
                ConnectorsUtil.getResourceByName(getResources(resourceInfo), ConnectorConnectionPool.class, poolName);
        if (ccPool == null) {
            return null;
        }
        String rarName = ccPool.getResourceAdapterName();
        if (rarName != null) {
            resourcesToload[0] = ccPool;
            resourcesToload[1] = connectorResource;

            //TODO ASR :need to get RACs from application/modules resources also ?
            ResourceAdapterConfig[] resourceAdapterConfig = new ResourceAdapterConfig[1];
            resourceAdapterConfig[0] = (ResourceAdapterConfig)
                    ConnectorsUtil.getResourceByName(getGlobalResources(), ResourceAdapterConfig.class, rarName);

            DeferredResourceConfig resourceConfig =
                    new DeferredResourceConfig(rarName, null, ccPool, connectorResource, null, null,
                            resourceAdapterConfig);
            resourceConfig.setResourcesToLoad(resourcesToload);
            return resourceConfig;
        }
        return null;
    }
*/

/*    protected DeferredResourceConfig getDeferredJdbcResourceConfigs(ResourceInfo resourceInfo) {

        Resource[] resourcesToload = new Resource[2];
        if(resourceInfo == null) {
            return null;
        }

        try {
            //__pm does not have a domain.xml entry and hence will not
            //be referenced
            if(!(resourceInfo.getName().endsWith("__pm"))){
                if(!isReferenced(resourceInfo)){
                    return null;
                }
            }
        } catch (Exception e) {
            String message = localStrings.getString("error.finding.resources.references", resourceInfo);
            _logger.log(Level.WARNING, message + e.getMessage());
            if(_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE,message + e.getMessage(), e);
            }
        }

        JdbcResource jdbcResource = (JdbcResource) ConnectorsUtil.getResourceByName(getResources(resourceInfo), JdbcResource.class,
                resourceInfo.getName());
        if(jdbcResource == null || !ConnectorsUtil.parseBoolean(jdbcResource.getEnabled())) {
            String cmpResourceName = getCorrespondingCmpResourceName(resourceInfo);
            jdbcResource = (JdbcResource) ConnectorsUtil.getResourceByName(getResources(resourceInfo), JdbcResource.class,
                    cmpResourceName);
            if(jdbcResource == null) {
                return null;
            }
        }
        JdbcConnectionPool jdbcPool = (JdbcConnectionPool)
                ConnectorsUtil.getResourceByName(getResources(resourceInfo), JdbcConnectionPool.class, jdbcResource.getPoolName());
        if(jdbcPool == null) {
            return null;
        }
        String rarName = getRANameofJdbcConnectionPool(jdbcPool);
        if(rarName != null && ConnectorsUtil.belongsToSystemRA(rarName)) {
            resourcesToload[0] = jdbcPool;
            resourcesToload[1] = jdbcResource;
            DeferredResourceConfig resourceConfig =
                    new DeferredResourceConfig(rarName,null,null,
                    null,jdbcPool,jdbcResource,null);
            resourceConfig.setResourcesToLoad(resourcesToload);
            return resourceConfig;
        }
        return null;
    }
*/


/**
     * Returns the deferred admin object config. This can be admin object of JMS RA which is lazily
     * loaded. Or for other connector RA which is not loaded at start-up. The connector RA which does
     * not have any resource or admin object associated with it are not loaded at start-up. They are
     * all lazily loaded.
     */

/*    protected DeferredResourceConfig getDeferredAdminObjectConfigs(ResourceInfo resourceInfo) {

        if(resourceInfo == null) {
            return null;
        }
        Resource[] resourcesToload = new Resource[1];

        try {
            if(!isReferenced(resourceInfo)){
                return null;
            }
        } catch (Exception e) {
            String message = localStrings.getString(
                    "error.finding.resources.references",
                    resourceInfo);
            _logger.log(Level.WARNING, message + e.getMessage());
            if(_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE,message + e.getMessage(), e);
            }
        }

        AdminObjectResource adminObjectResource = (AdminObjectResource)
                ConnectorsUtil.getResourceByName(getResources(resourceInfo), AdminObjectResource.class, resourceInfo.getName());
        if(adminObjectResource == null || !ConnectorsUtil.parseBoolean(adminObjectResource.getEnabled())) {
            return null;
        }
        String rarName = adminObjectResource.getResAdapter();
        if(rarName != null){
            //TODO ASR : look for RACs in application/module's resources also 
            resourcesToload[0] = adminObjectResource;
            ResourceAdapterConfig[] resourceAdapterConfig =
                    new ResourceAdapterConfig[1];
            resourceAdapterConfig[0] = (ResourceAdapterConfig)
                    ConnectorsUtil.getResourceByName(getGlobalResources(), ResourceAdapterConfig.class, rarName);
            DeferredResourceConfig resourceConfig =
                    new DeferredResourceConfig(rarName,adminObjectResource,
                    null,null,null,null,resourceAdapterConfig);
            resourceConfig.setResourcesToLoad(resourcesToload);
            return resourceConfig;
        }
        return null;
    }
*/
    protected String getCorrespondingCmpResourceName(ResourceInfo resourceInfo) {

        int index = resourceInfo.getName().lastIndexOf("__pm");
        if(index != -1) {
            return resourceInfo.getName().substring(0,index);
        }
        return null;
    }


/**
     * Returns the deferred connector connection pool config. This can be pool of JMS RA which is lazily
     * loaded. Or for other connector RA which is not loaded at startup. The connector RA which does
     * not have any resource or admin object associated with it are not loaded at startup. They are
     * all lazily loaded.
     */

/*    protected DeferredResourceConfig getDeferredConnectorPoolConfigs(PoolInfo poolInfo) {

        Resource[] resourcesToload = new Resource[1];
        if(poolInfo == null) {
            return null;
        }


        ConnectorConnectionPool ccPool = (ConnectorConnectionPool)
                ConnectorsUtil.getResourceByName(getResources(poolInfo), ConnectorConnectionPool.class, poolInfo.getName());
        if(ccPool == null) {
            return null;
        }

        String rarName = ccPool.getResourceAdapterName();

        if(rarName != null){
            //TODO ASR : look for RAC in application/module's namespaces
            resourcesToload[0] = ccPool;
            ResourceAdapterConfig[] resourceAdapterConfig =
                    new ResourceAdapterConfig[1];
            resourceAdapterConfig[0] = (ResourceAdapterConfig)
                    ConnectorsUtil.getResourceByName(getGlobalResources(), ResourceAdapterConfig.class, rarName);
            DeferredResourceConfig resourceConfig =
                    new DeferredResourceConfig(rarName,null,ccPool,
                    null,null,null,resourceAdapterConfig);
            resourceConfig.setResourcesToLoad(resourcesToload);
            return resourceConfig;
        }
        return null;
    }
  */
/*    protected DeferredResourceConfig getDeferredJdbcPoolConfigs(PoolInfo poolInfo) {

        Resource[] resourcesToload = new Resource[1];
        if(poolInfo == null) {
            return null;
        }

        JdbcConnectionPool jdbcPool = (JdbcConnectionPool)
                ConnectorsUtil.getResourceByName(getResources(poolInfo), JdbcConnectionPool.class, poolInfo.getName());
        if(jdbcPool == null) {
            return null;
        }
        String rarName = getRANameofJdbcConnectionPool(jdbcPool);

        if(rarName != null && ConnectorsUtil.belongsToSystemRA(rarName)) {
            resourcesToload[0] = jdbcPool;
            DeferredResourceConfig resourceConfig =
                    new DeferredResourceConfig(rarName,null,null,
                    null,jdbcPool,null,null);
            resourceConfig.setResourcesToLoad(resourcesToload);
            return resourceConfig;
        }
        return null;
    }
*/

/*
    public boolean poolBelongsToSystemRar(String poolName) {
        ConnectorConnectionPool ccPool = (ConnectorConnectionPool)
                getResources().getResourceByName(ConnectorConnectionPool.class, poolName);
        if(ccPool != null){
            return ConnectorsUtil.belongsToSystemRA(ccPool.getResourceAdapterName());
        } else {
            JdbcConnectionPool jdbcPool = (JdbcConnectionPool)
                    getResources().getResourceByName(JdbcConnectionPool.class, poolName);
            if(jdbcPool != null) {
                return true;
            }
        }
        return false;
    }

    public boolean adminObjectBelongsToSystemRar(String adminObject) {
        AdminObjectResource aor = (AdminObjectResource)
                getResources().getResourceByName(AdminObjectResource.class, adminObject);
        if(aor != null) {
            return ConnectorsUtil.belongsToSystemRA(aor.getResAdapter());
        }
        return false;
    }
*/

/*
    public boolean resourceBelongsToSystemRar(String resourceName) {
        ConnectorResource connectorResource = (ConnectorResource)
                getResources().getResourceByName(ConnectorResource.class, resourceName);
        if(connectorResource != null){
            return poolBelongsToSystemRar(connectorResource.getPoolName());
        } else {
            JdbcResource jdbcResource = (JdbcResource)
                    getResources().getResourceByName(JdbcResource.class, resourceName);
            if(jdbcResource != null) {
                return true;
            }
        }
        return false;
    }
*/

    /**
     * Returns true if the given resource is referenced by this server.
     *
     * @param resourceInfo the name of the resource
     * @return true if the named resource is used/referred by this server
     */
    public boolean isReferenced(ResourceInfo resourceInfo) {
        boolean refExists = false;
        if (ConnectorsUtil.isModuleScopedResource(resourceInfo) ||
                ConnectorsUtil.isApplicationScopedResource(resourceInfo)) {
            refExists = getServer().getApplicationRef(resourceInfo.getApplicationName()) != null;
        } else {
            String resourceName = resourceInfo.getName();
            refExists = getServer().isResourceRefExists(resourceName);
        }
        if (_logger.isLoggable(Level.FINE)) {
            _logger.fine("isReferenced :: " + resourceInfo + " - " + refExists);
        }
        return refExists;
    }

    public boolean isEnabled(Application application){
        if(application == null){
            return false;
        }
        boolean appEnabled = Boolean.valueOf(application.getEnabled());
        ApplicationRef appRef = getServer().getApplicationRef(application.getName());
        boolean appRefEnabled = false;
        if(appRef != null ){
            appRefEnabled = Boolean.valueOf(appRef.getEnabled());
        }
        return appEnabled && appRefEnabled;
    }

    /**
     * Checks if a Resource is enabled.
     * <p/>
     * Since 8.1 PE/SE/EE, A resource [except resource adapter configs, connector and
     * JDBC connection pools which are global and hence enabled always] is enabled
     * only when the resource is enabled and there exists a resource ref to this
     * resource in this server instance and that resource ref is enabled.
     * <p/>
     * Before a resource is loaded or deployed, it is checked to see if it is
     * enabled.
     *
     * @since 8.1 PE/SE/EE
     */
    public boolean isEnabled(Resource resource) {
        if(_logger.isLoggable(Level.FINE)) {
            _logger.fine("ResourcesUtil :: isEnabled");
        }
        if (resource == null){
            return false;
        }else if (resource instanceof BindableResource) {
            BindableResource bindableResource = (BindableResource)resource;
            if(bindableResource.getJndiName().contains(ConnectorConstants.DATASOURCE_DEFINITION_JNDINAME_PREFIX)){
                return Boolean.valueOf(bindableResource.getEnabled());
            }
            ResourceRef resRef = getServer().getResourceRef(
                    ((BindableResource) resource).getJndiName());
            return isEnabled((BindableResource) resource) &&
                    ((resRef != null) && parseBoolean(resRef.getEnabled()));
        } else if(resource instanceof ResourcePool) {
            return isEnabled((ResourcePool) resource);
        }else if(resource instanceof WorkSecurityMap || resource instanceof ResourceAdapterConfig){
            return true;
        }else{
            return false;
        }
    }


    public boolean isEnabled(ResourcePool pool) {
        boolean enabled = true;
        if(pool == null) {
            return false;
        }
        if(pool instanceof ConnectorConnectionPool) {
            ConnectorConnectionPool ccpool = (ConnectorConnectionPool) pool;
            String raName = ccpool.getResourceAdapterName();
            enabled = isRarEnabled(raName);
        }
        //JDBC RA is system RA and is always enabled
        return enabled;
    }

    public boolean isEnabled(BindableResource br, ResourceInfo resourceInfo){
        boolean enabled = false;
        //this cannot happen? need to remove later?
        if (br == null) {
            return false;
        }
        boolean resourceEnabled = ConnectorsUtil.parseBoolean(br.getEnabled());

        //TODO can we also check whether the application in which it is defined is enabled (app and app-ref) ?
        if(resourceInfo.getName().contains(ConnectorConstants.DATASOURCE_DEFINITION_JNDINAME_PREFIX)){
            return resourceEnabled;
        }

        boolean refEnabled = isResourceReferenceEnabled(resourceInfo);

        if(br instanceof ConnectorResource) {
            ConnectorResource cr = (ConnectorResource) br;
            String poolName = cr.getPoolName();
            ConnectorConnectionPool ccp = (ConnectorConnectionPool)
                    ConnectorsUtil.getResourceByName(getResources(resourceInfo), ConnectorConnectionPool.class, poolName);
            if (ccp == null) {
                return false;
            }
            boolean poolEnabled = isEnabled(ccp);
            enabled  = poolEnabled && resourceEnabled && refEnabled ;
        } else if(br instanceof AdminObjectResource) {
            //AdminObjectResource aor = (AdminObjectResource) br;
           // String raName = aor.getResAdapter();
            if(/* TODO isRarEnabled &&*/ resourceEnabled && refEnabled){
                enabled = true;
            }
        } else if(refEnabled && resourceEnabled){
            //other bindable resources need to be checked for "resource.enabled" and "resource-ref.enabled"
            enabled = true;
        }
        return enabled;
    }

    public boolean isEnabled(BindableResource br) {
        ResourceInfo resourceInfo = ConnectorsUtil.getResourceInfo(br);
        return isEnabled(br, resourceInfo);
    }

    private boolean isRarEnabled(String raName) {
        if(raName == null || raName.length() == 0)
            return false;
        Application application = getDomain().getApplications().getApplication(raName);
        if(application != null) {
            return isApplicationReferenceEnabled(raName);
        } else if(ConnectorsUtil.belongsToSystemRA(raName)) {
            return true;
        } else {
            return belongToEmbeddedRarAndEnabled(raName);
        }
    }

    /**
     * Checks if the application reference is enabled
     * @param appName application-name
     * @since SJSAS 9.1 PE/SE/EE
     * @return boolean indicating the status
     */
    private boolean isApplicationReferenceEnabled(String appName) {
        ApplicationRef appRef = getServer().getApplicationRef(appName);
        if (appRef == null) {
            if (_logger.isLoggable(Level.FINE)) {
                _logger.fine("ResourcesUtil :: isApplicationReferenceEnabled null ref");
            }
            return false;
        }
        if(_logger.isLoggable(Level.FINE)) {
            _logger.fine("ResourcesUtil :: isApplicationReferenceEnabled appRef enabled ?" + appRef.getEnabled());
        }
        return ConnectorsUtil.parseBoolean(appRef.getEnabled());
    }

    public Collection<AdminObjectResource> getEnabledAdminObjectResources(String raName)  {
        Collection<AdminObjectResource> allResources = new ArrayList<AdminObjectResource>();

        allResources.addAll(getEnabledAdminObjectResources(raName, getGlobalResources()));

/*        Collection<Application> apps = getApplications().getApplications();
        for(Application app : apps){
            //TODO ASR check for enabled app and app-ref
            Resources appScopedResources = app.getResources();
            if(appScopedResources != null){
                allResources.addAll(getEnabledAdminObjectResources(raName, appScopedResources));
            }
            List<Module> modules = app.getModule();
            for(Module module : modules){
                Resources moduleScopedResources = module.getResources();
                if(moduleScopedResources != null){
                    allResources.addAll(getEnabledAdminObjectResources(raName, moduleScopedResources));
                }
            }
        }*/
        return allResources;
    }

    //TODO can be made generic
    //TODO probably, DuckTyped for resources
    public Collection<AdminObjectResource> getEnabledAdminObjectResources(String raName, Resources resources)  {
        List<AdminObjectResource> adminObjectResources = new ArrayList<AdminObjectResource>();
        for(Resource resource : resources.getResources(AdminObjectResource.class)) {

            AdminObjectResource adminObjectResource = (AdminObjectResource)resource;
            String resourceAdapterName = adminObjectResource.getResAdapter();

            if(resourceAdapterName == null)
                continue;
            if(raName!= null && !raName.equals(resourceAdapterName))
                continue;


            // skips the admin resource if it is not referenced by the server
            if(!isEnabled(adminObjectResource))
                continue;
            adminObjectResources.add(adminObjectResource);
        }
        //AdminObjectResource[] allAdminObjectResources =
        //            new AdminObjectResource[adminObjectResources.size()];
        //return adminObjectResources.toArray(allAdminObjectResources);
        return adminObjectResources;
    }

    private boolean belongToEmbeddedRarAndEnabled(String resourceAdapterName)  {
        String appName = getAppNameToken(resourceAdapterName);
        if(appName==null)
            return false;
        Applications apps = getDomain().getApplications();
        Application app = apps.getApplication(appName);
        if(app == null || !ConnectorsUtil.parseBoolean(app.getEnabled()))
            return false;
        return isApplicationReferenceEnabled(appName);
    }

    private String getAppNameToken(String rarName) {
        if(rarName == null) {
            return null;
        }
        int index = rarName.indexOf(
                ConnectorConstants.EMBEDDEDRAR_NAME_DELIMITER);
        if(index != -1) {
            return rarName.substring(0,index);
        } else {
            return null;
        }
    }

    /**
     * Checks if a resource reference is enabled
     * For application-scoped-resource, checks whether application-ref is enabled
     *
     * @param resourceInfo resourceInfo ResourceInfo
     * @return boolean indicating whether the resource-ref/application-ref is enabled.
     */
    private boolean isResourceReferenceEnabled(ResourceInfo resourceInfo) {
        String enabled = "false";
        if (ConnectorsUtil.isModuleScopedResource(resourceInfo) ||
                ConnectorsUtil.isApplicationScopedResource(resourceInfo)) {
            ApplicationRef appRef = getServer().getApplicationRef(resourceInfo.getApplicationName());
            if (appRef != null) {
                enabled = appRef.getEnabled();
            } else {
                // for an application-scoped-resource, if the application is being deployed,
                // <application> element and <application-ref> will be null until deployment
                // is complete. Hence this workaround.
                enabled = "true";
                if(_logger.isLoggable(Level.FINE)) {
                    _logger.fine("ResourcesUtil :: isResourceReferenceEnabled null app-ref");
                }
            }
        } else {
            ResourceRef ref = getServer().getResourceRef(resourceInfo.getName());
            if (ref != null) {
                enabled = ref.getEnabled();
            } else {
                if(_logger.isLoggable(Level.FINE)) {
                    _logger.fine("ResourcesUtil :: isResourceReferenceEnabled null ref");
                }
            }
        }
        if(_logger.isLoggable(Level.FINE)) {
            _logger.fine("ResourcesUtil :: isResourceReferenceEnabled ref enabled ?" + enabled);
        }

        return ConnectorsUtil.parseBoolean(enabled);
    }

    /**
     * Gets a JDBC resource on the basis of its jndi name
     * @param jndiName the jndi name of the JDBC resource to lookup
     * @param checkReference if true, returns this JDBC resource only if it is referenced in
     *                       this server. If false, returns the JDBC resource irrespective of
     *                       whether it is referenced or not.
     * @return JdbcResource resource
     */
/*
    public JdbcResource getJdbcResourceByJndiName( String jndiName, boolean checkReference) {

        if (_logger.isLoggable(Level.FINE)) {
            _logger.fine("ResourcesUtil :: looking up jdbc resource, jndiName is : " + jndiName );
        }

        JdbcResource jdbcResource = (JdbcResource) getResources().getResourceByName(JdbcResource.class, jndiName);

        if (_logger.isLoggable(Level.FINE)) {
            _logger.fine("ResourcesUtil :: looked up jdbc resource:" + jdbcResource );
        }

        //does the isReferenced method throw NPE for null value? Better be safe
        if (jdbcResource == null) {
            return null;
        }

        if (_logger.isLoggable(Level.FINE)) {
            _logger.fine("ResourcesUtil :: looked up jdbc resource name:" + jdbcResource.getJndiName() );
        }

        if(checkReference){
            return isReferenced( jndiName ) ? jdbcResource : null;
        }else{
            return jdbcResource;
        }
    }
*/

    public String getResourceType(ConfigBeanProxy cb) {
        if (cb instanceof ConnectorConnectionPool) {
            return ConnectorConstants.RES_TYPE_CCP;
        } else if (cb instanceof ConnectorResource) {
            return ConnectorConstants.RES_TYPE_CR;
        }
        Collection<ConnectorRuntimeExtension> extensions =
                Globals.getDefaultHabitat().getAllServices(ConnectorRuntimeExtension.class);
        for (ConnectorRuntimeExtension extension : extensions) {
            return extension.getResourceType(cb);
        }
        return null;
    }

    private boolean parseBoolean(String enabled) {
        return Boolean.parseBoolean(enabled);
    }

    public ConnectorDescriptor getConnectorDescriptorFromUri(String rarName, String raLoc) {
        try {
            String appName = rarName.substring(0, rarName.indexOf(ConnectorConstants.EMBEDDEDRAR_NAME_DELIMITER));
            //String actualRarName = rarName.substring(rarName.indexOf(ConnectorConstants.EMBEDDEDRAR_NAME_DELIMITER) + 1);
            String appDeployLocation = ResourcesUtil.createInstance().getApplicationDeployLocation(appName);

            FileArchive in = ConnectorRuntime.getRuntime().getFileArchive();
            in.open(new URI(appDeployLocation));
            ApplicationArchivist archivist = ConnectorRuntime.getRuntime().getApplicationArchivist();
            com.sun.enterprise.deployment.Application application = archivist.open(in);
            return application.getModuleByTypeAndUri(ConnectorDescriptor.class, raLoc);
        } catch (Exception e) {
            Object params[] = new Object[]{rarName, e};
            _logger.log(Level.WARNING, "error.getting.connector.descriptor", params);
        }
        return null;
    }

    /**
     * Determines if a connector connection pool is referred in a
     * server-instance via resource-refs
     * @param poolInfo pool-name
     * @return boolean true if pool is referred in this server instance as well enabled, false
     * otherwise
     */
    public boolean isPoolReferredInServerInstance(PoolInfo poolInfo) {

        Collection<ConnectorResource> connectorResources = getRuntime().getResources(poolInfo).
                getResources(ConnectorResource.class);
        for (ConnectorResource resource : connectorResources) {
            if(_logger.isLoggable(Level.FINE)) {
                _logger.fine("poolname " + resource.getPoolName() + "resource " + resource.getJndiName());
            }
            ResourceInfo resourceInfo = ConnectorsUtil.getResourceInfo(resource);
            if ((resource.getPoolName().equalsIgnoreCase(poolInfo.getName())) && isReferenced(resourceInfo)
                    && isEnabled(resource)){
                if(_logger.isLoggable(Level.FINE)) {
                    _logger.fine("Connector resource "  + resource.getJndiName() + "refers "
                        + poolInfo + "in this server instance and is enabled");
                }
                return true;
            }
        }
        if(_logger.isLoggable(Level.FINE)) {
            _logger.fine("No Connector resource refers [ " + poolInfo + " ] in this server instance");
        }
        return false;
    }

    public ResourcePool getPoolConfig(PoolInfo poolInfo){
        Resources resources = getResources(poolInfo);
        ResourcePool pool = null;
        if(resources != null){
            pool = (ResourcePool) ConnectorsUtil.getResourceByName(resources, ResourcePool.class, poolInfo.getName());
        }
        return pool;
    }

    public ConnectorConnectionPool getConnectorConnectionPoolOfResource(ResourceInfo resourceInfo) {
        ConnectorResource resource = null;
        ConnectorConnectionPool pool = null;
        Resources resources = getResources(resourceInfo);
        if(resources != null){
            resource = (ConnectorResource) ConnectorsUtil.getResourceByName(resources, ConnectorResource.class, resourceInfo.getName());
            if(resource != null){
                pool = (ConnectorConnectionPool)ConnectorsUtil.getResourceByName(resources, ConnectorConnectionPool.class, resource.getPoolName());
            }
        }
        return pool;
    }

    public boolean isRARResource(Resource resource){
        return ConnectorsUtil.isRARResource(resource);
    }

    public String getRarNameOfResource(Resource resource, Resources resources){
        return ConnectorsUtil.getRarNameOfResource(resource, resources);
    }


    public Resource getResource(ResourceInfo resourceInfo, Class resourceType) {
        Resource resource = null;
        String appName = resourceInfo.getApplicationName();
        String jndiName = resourceInfo.getName();
        String moduleName = resourceInfo.getModuleName();
        Resources resources = null;
        if (ConnectorsUtil.isApplicationScopedResource(resourceInfo) ||
                ConnectorsUtil.isModuleScopedResource(resourceInfo)) {
            if (getApplicationByName(appName) != null) {
                resources = getResources(resourceInfo);
            }
        } else {
            resources = getResources(resourceInfo);
        }
        if (resources != null) {
            resource = ConnectorsUtil.getResourceByName( resources, resourceType, jndiName);
        } else {
            //it is possible that "application" is being deployed (eg: during deployment "prepare" or application "start")
            if (ConnectorsUtil.isApplicationScopedResource(resourceInfo) ||
                    ConnectorsUtil.isModuleScopedResource(resourceInfo)) {

                //for app-scoped-resource, resource is stored in "app-name" key
                if (ConnectorsUtil.isApplicationScopedResource(resourceInfo)) {
                    moduleName = appName;
                }

                resources = ResourcesRegistry.getResources(appName, moduleName);
                if (resources != null) {
                    resource = ConnectorsUtil.getResourceByName( resources, resourceType, jndiName);
                }
            }
        }
        return resource;
    }

    public Resource getResource(String jndiName, String appName, String moduleName, Class resourceType) {
        ResourceInfo resourceInfo = new ResourceInfo(jndiName, appName, moduleName);
        return getResource(resourceInfo, resourceType);
    }

    public Collection<Resource> filterConnectorResources(Resources allResources, String moduleName, boolean includePools) {
        //TODO V3 needed for redeploy of module, what happens to the listeners of these resources ?
        Collection<ConnectorConnectionPool> connectionPools =
                ConnectorsUtil.getAllPoolsOfModule(moduleName, allResources);
        Collection<String> poolNames = ConnectorsUtil.getAllPoolNames(connectionPools);
        Collection<Resource> resources = ConnectorsUtil.getAllResources(poolNames, allResources);
        Collection<AdminObjectResource> adminObjectResources =
                ResourcesUtil.createInstance().getEnabledAdminObjectResources(moduleName);
        resources.addAll(adminObjectResources);
        if(includePools){
            Collection<ConnectorConnectionPool> allPoolsOfModule = ConnectorsUtil.getAllPoolsOfModule(moduleName, allResources);
            resources.addAll(allPoolsOfModule);
        }
        return resources;
    }

}
