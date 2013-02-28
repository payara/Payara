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

package com.sun.enterprise.connectors;

import com.sun.appserv.connectors.internal.api.ConnectorConstants;
import com.sun.appserv.connectors.internal.api.ConnectorRuntimeException;
import com.sun.appserv.connectors.internal.api.ConnectorsUtil;
import com.sun.appserv.connectors.internal.api.PoolingException;
import com.sun.appserv.connectors.internal.spi.ConnectionManager;
import com.sun.enterprise.config.serverbeans.BindableResource;
import com.sun.enterprise.connectors.authentication.AuthenticationService;
import com.sun.enterprise.connectors.util.ConnectionPoolObjectsUtils;
import com.sun.enterprise.connectors.util.ResourcesUtil;
import com.sun.enterprise.deployment.ConnectorDescriptor;
import com.sun.enterprise.deployment.ResourcePrincipal;
import com.sun.enterprise.deployment.ResourceReferenceDescriptor;
import com.sun.enterprise.resource.ClientSecurityInfo;
import com.sun.enterprise.resource.ResourceSpec;
import com.sun.enterprise.resource.allocator.ConnectorAllocator;
import com.sun.enterprise.resource.allocator.LocalTxConnectorAllocator;
import com.sun.enterprise.resource.allocator.NoTxConnectorAllocator;
import com.sun.enterprise.resource.allocator.ResourceAllocator;
import com.sun.enterprise.resource.pool.PoolManager;
import com.sun.enterprise.security.SecurityContext;
import com.sun.enterprise.util.i18n.StringManager;
import com.sun.logging.LogDomains;
import org.glassfish.resourcebase.resources.api.PoolInfo;
import org.glassfish.resourcebase.resources.api.ResourceInfo;

import javax.resource.ResourceException;
import javax.resource.spi.*;
import javax.resource.spi.IllegalStateException;
import javax.resource.spi.SecurityException;
import javax.security.auth.Subject;
import java.io.Serializable;
import java.security.Principal;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * @author Tony Ng
 */
public class ConnectionManagerImpl implements ConnectionManager, Serializable {

    protected String jndiName;
    protected String logicalName;
    protected PoolInfo poolInfo;
    protected ResourceInfo resourceInfo;

    private volatile static Logger logger = LogDomains.getLogger(
            ConnectionManagerImpl.class,LogDomains.RSR_LOGGER);
    private volatile static StringManager localStrings = StringManager.getManager(
            ConnectionManagerImpl.class);

    protected String rarName;

    private transient BindableResource resourceConfiguration;

    protected ResourcePrincipal defaultPrin = null;

    public ConnectionManagerImpl(PoolInfo poolInfo, ResourceInfo resourceInfo) {
        this.poolInfo = poolInfo;
        this.resourceInfo = resourceInfo;
    }

    public void setJndiName(String jndiName) {
        this.jndiName = jndiName;
    }

    public String getJndiName() {
        return jndiName;
    }

    public void setLogicalName(String logicalName) {
        this.logicalName = logicalName;
    }

    public String getLogicalName() {
        return logicalName;
    }


/*
    public void setPoolInfo(PoolInfo poolInfo) {
        this.poolInfo = poolInfo;
    }
*/

    /**
     * Allocate a non transactional connection. This connection, even if
     * acquired in the context of an existing transaction, will never
     * be associated with a transaction
     * The typical use case may be to check the original contents of an EIS
     * when a transacted connection is changing the contents, and the tx
     * is yet to be committed.
     * <p/>
     * We create a ResourceSpec for a non tx connection with a name ending
     * in __nontx. This is to maintain uniformity with the scheme of having
     * __pm connections.
     * If one were to create a resource with a jndiName ending with __nontx
     * the same functionality might be achieved.
     */
    public Object allocateNonTxConnection(ManagedConnectionFactory mcf,
                                          ConnectionRequestInfo cxRequestInfo) throws ResourceException {
        String localJndiName = jndiName;

        logFine("Allocating NonTxConnection");

        //If a resource has been created with __nontx, we don't want to
        //add it again.
        //Otherwise we need to add __nontx at the end to ensure that the
        //mechanism to check for the correct resource manager still works
        //We do the addition if and only if we are getting this call
        //from a normal datasource and not a __nontx datasource.
        if (!jndiName.endsWith(ConnectorConstants.NON_TX_JNDI_SUFFIX)) {
            localJndiName = jndiName + ConnectorConstants.NON_TX_JNDI_SUFFIX;
            logFine("Adding __nontx to jndiname");
        } else {
            logFine("lookup happened from a __nontx datasource directly");
        }
        return allocateConnection(mcf, cxRequestInfo, localJndiName);
    }

    public Object allocateConnection(ManagedConnectionFactory mcf, ConnectionRequestInfo cxRequestInfo)
            throws ResourceException {
        return this.allocateConnection(mcf, cxRequestInfo, jndiName);
    }

    public Object allocateConnection(ManagedConnectionFactory mcf, ConnectionRequestInfo cxRequestInfo,
                                     String jndiNameToUse) throws ResourceException {
        return this.allocateConnection(mcf, cxRequestInfo, jndiNameToUse, null);
    }

    public Object allocateConnection(ManagedConnectionFactory mcf,
                                     ConnectionRequestInfo cxRequestInfo, String jndiNameToUse, Object conn)
            throws ResourceException {
        validateResourceAndPool();
        PoolManager poolmgr = ConnectorRuntime.getRuntime().getPoolManager();
        boolean resourceShareable = true;

        ResourceReferenceDescriptor ref =  poolmgr.getResourceReference(jndiNameToUse, logicalName);

        if (ref != null) {
            String shareableStr = ref.getSharingScope();

            if (shareableStr.equals(ref.RESOURCE_UNSHAREABLE)) {
                resourceShareable = false;
            }
        }

        //TODO V3 refactor all the 3 cases viz, no res-ref, app-auth, cont-auth.
        if (ref == null) {
            if(getLogger().isLoggable(Level.FINE)) {
                getLogger().log(Level.FINE, "poolmgr.no_resource_reference", jndiNameToUse);
            }
            return internalGetConnection(mcf, defaultPrin, cxRequestInfo,
                    resourceShareable, jndiNameToUse, conn, true);
        }
        String auth = ref.getAuthorization();

        if (auth.equals(ResourceReferenceDescriptor.APPLICATION_AUTHORIZATION)) {
            if (cxRequestInfo == null) {

                String msg = getLocalStrings().getString("con_mgr.null_userpass");
                throw new ResourceException(msg);
            }
            ConnectorRuntime.getRuntime().switchOnMatching(rarName, poolInfo);
            return internalGetConnection(mcf, null, cxRequestInfo,
                    resourceShareable, jndiNameToUse, conn, false);
        } else {
            ResourcePrincipal prin = null;
            Set principalSet = null;
            Principal callerPrincipal = null;
            SecurityContext securityContext = null;
            ConnectorRuntime connectorRuntime = ConnectorRuntime.getRuntime();
            //TODO V3 is SecurityContext.getCurrent() the right way ? Does it need to be injected ?
            if (connectorRuntime.isServer() &&
                    (securityContext = SecurityContext.getCurrent()) != null &&
                    (callerPrincipal = securityContext.getCallerPrincipal()) != null &&
                    (principalSet = securityContext.getPrincipalSet()) != null) {
                AuthenticationService authService =
                        connectorRuntime.getAuthenticationService(rarName, poolInfo);
                if (authService != null) {
                    prin = (ResourcePrincipal) authService.mapPrincipal(
                            callerPrincipal, principalSet);
                }
            }

            if (prin == null) {
                prin = ref.getResourcePrincipal();
                if (prin == null) {
                    if (getLogger().isLoggable(Level.FINE)) {
                        getLogger().log(Level.FINE, "default-resource-principal not"
                                + "specified for " + jndiNameToUse + ". Defaulting to"
                                + " user/password specified in the pool");
                    }
                    prin = defaultPrin;
                } else if (!prin.equals(defaultPrin)) {
                    ConnectorRuntime.getRuntime().switchOnMatching(rarName, poolInfo);
                }
            }
            return internalGetConnection(mcf, prin, cxRequestInfo,
                    resourceShareable, jndiNameToUse, conn, false);
        }
    }

    protected Object internalGetConnection(ManagedConnectionFactory mcf,
                                           final ResourcePrincipal prin, ConnectionRequestInfo cxRequestInfo,
                                           boolean shareable, String jndiNameToUse, Object conn, boolean isUnknownAuth)
            throws ResourceException {
        try {
            PoolManager poolmgr = ConnectorRuntime.getRuntime().getPoolManager();
            ConnectorRegistry registry = ConnectorRegistry.getInstance();
            PoolMetaData pmd = registry.getPoolMetaData(poolInfo);

            ResourceSpec spec = new ResourceSpec(jndiNameToUse,
                    ResourceSpec.JNDI_NAME, pmd);
            spec.setPoolInfo(this.poolInfo);
            ManagedConnectionFactory freshMCF = pmd.getMCF();

            if (getLogger().isLoggable(Level.INFO)) {
                if (!freshMCF.equals(mcf)) {
                    getLogger().info("conmgr.mcf_not_equal");
                }
            }
            ConnectorDescriptor desc = registry.getDescriptor(rarName);

            Subject subject = null;
            ClientSecurityInfo info = null;
            boolean subjectDefined = false;
            if (isUnknownAuth && rarName.equals(ConnectorConstants.DEFAULT_JMS_ADAPTER)
                    && !(pmd.isAuthCredentialsDefinedInPool())) {
                //System.out.println("Unkown Auth - pobably nonACC client");
                //Unknown authorization. This is the case for standalone java clients,
                //where the authorization is neither container nor component
                //managed. In this case we associate an non-null Subject with no
                //credentials, so that the RA can either use its own custom logic
                //for figuring out the credentials. Relevant connector spec section
                //is 9.1.8.2.
                //create non-null Subject associated with no credentials
                //System.out.println("RAR name "+ rarName);
                subject = ConnectionPoolObjectsUtils.createSubject(mcf, null);
            } else {
                if (prin == null) {
                    info = new ClientSecurityInfo(cxRequestInfo);
                } else {
                    info = new ClientSecurityInfo(prin);
                    if (prin.equals(defaultPrin)) {
                        subject = pmd.getSubject();
                    } else {
                        subject = ConnectionPoolObjectsUtils.createSubject(mcf, prin);
                    }
                }
            }

            int txLevel = pmd.getTransactionSupport();
            if (getLogger().isLoggable(Level.FINE)) {
                logFine("ConnectionMgr: poolName " + poolInfo +
                        "  txLevel : " + txLevel);
            }

             if ( conn != null ) {
                 spec.setConnectionToAssociate( conn );
             }


            return getResource(txLevel, poolmgr, mcf, spec, subject, cxRequestInfo, info, desc, shareable);

        } catch (PoolingException ex) {
            Object[] params = new Object[]{poolInfo, ex};
            getLogger().log(Level.WARNING, "poolmgr.get_connection_failure", params);
            //GLASSFISH-19609
            //we can't simply look for ResourceException and throw back since
            //Connector Container also throws ResourceException which might
            //hide the SecurityException thrown by RA.
            //So, we try to track SecurityException
            unwrapSecurityException(ex);
            String i18nMsg = getLocalStrings().getString("con_mgr.error_creating_connection", ex.getMessage());
            ResourceAllocationException rae = new ResourceAllocationException(i18nMsg);
            rae.initCause(ex);
            throw rae;
        }
    }

    private void unwrapSecurityException(Throwable ex) throws ResourceException{
        if(ex != null){
            if(ex instanceof SecurityException){
                throw (SecurityException)ex;
            }else{
                unwrapSecurityException(ex.getCause());
            }
        }
    }

    private Object getResource(int txLevel, PoolManager poolmgr, ManagedConnectionFactory mcf, ResourceSpec spec,
                               Subject subject, ConnectionRequestInfo cxRequestInfo, ClientSecurityInfo info,
                               ConnectorDescriptor desc, boolean shareable)
            throws PoolingException, ResourceAllocationException, IllegalStateException, RetryableUnavailableException {
        ResourceAllocator alloc;

        switch (txLevel) {
            case ConnectorConstants.NO_TRANSACTION_INT:
                alloc = new NoTxConnectorAllocator(poolmgr, mcf, spec, subject, cxRequestInfo, info, desc);
                break;
            case ConnectorConstants.LOCAL_TRANSACTION_INT:
                alloc = new LocalTxConnectorAllocator(poolmgr, mcf, spec, subject, cxRequestInfo, info, desc, shareable);
                break;
            case ConnectorConstants.XA_TRANSACTION_INT:
                if (rarName.equals(ConnectorRuntime.DEFAULT_JMS_ADAPTER)) {
                    shareable = false;
                }
                spec.markAsXA();
                alloc = new ConnectorAllocator(poolmgr, mcf, spec, subject, cxRequestInfo, info, desc, shareable);
                return poolmgr.getResource(spec, alloc, info);
            
            default:
                String i18nMsg = getLocalStrings().getString("con_mgr.illegal_tx_level", txLevel + " ");
                throw new IllegalStateException(i18nMsg);
        }
        return poolmgr.getResource(spec, alloc, info);
    }


    public void setRarName(String _rarName) {
        rarName = _rarName;
    }

    public String getRarName() {
        return rarName;
    }

    /*
    * This method is called from the ConnectorObjectFactory lookup
    * With this we move all the housekeeping work in allocateConnection
    * up-front 
    */
    public void initialize() throws ConnectorRuntimeException {

        ConnectorRuntime runtime = ConnectorRuntime.getRuntime();

        if(runtime.isNonACCRuntime()){
            jndiName = ConnectorsUtil.getPMJndiName(jndiName);
        }
        ConnectorRegistry registry = ConnectorRegistry.getInstance();
        PoolMetaData pmd = registry.getPoolMetaData(poolInfo);
        defaultPrin = pmd.getResourcePrincipal();
    }

    private void validateResourceAndPool() throws ResourceException {
        ResourceInfo resourceInfo = this.resourceInfo;
        ResourcesUtil resourcesUtil = ResourcesUtil.createInstance();

        ConnectorRuntime runtime = ConnectorRuntime.getRuntime();
        ConnectorRegistry registry = ConnectorRegistry.getInstance();
        // adding a perf. optimization check so that "config-bean" is not accessed at all for
        // cases where the resource is enabled (deployed). Only for cases where resource
        // is not available, we look further and determine whether resource/resource-ref
        // are disabled.
        if (!registry.isResourceDeployed(resourceInfo)) {
            if(logger.isLoggable(Level.FINEST)){
                logger.log(Level.FINEST,"resourceInfo not found in connector-registry : " + resourceInfo);
            }
            boolean isDefaultResource = false;
            boolean isSunRAResource = false;
            ConnectorDescriptor descriptor = registry.getDescriptor(rarName);
            if (descriptor != null) {
                isDefaultResource = descriptor.getDefaultResourcesNames().contains(resourceInfo.getName());
                if (descriptor.getSunDescriptor() != null) {
                    com.sun.enterprise.deployment.runtime.connector.ResourceAdapter rar =
                            descriptor.getSunDescriptor().getResourceAdapter();
                    if (rar != null) {
                        String sunRAJndiName = (String)
                                rar.getValue(com.sun.enterprise.deployment.runtime.connector.ResourceAdapter.JNDI_NAME);
                        isSunRAResource = resourceInfo.getName().equals(sunRAJndiName);
                    }
                }
            }

            if ((runtime.isServer() || runtime.isEmbedded()) &&
                    (!resourceInfo.getName().contains(ConnectorConstants.DATASOURCE_DEFINITION_JNDINAME_PREFIX) &&
                            (!isDefaultResource) && (!isSunRAResource))) {
                // performance optimization so that resource configuration is not retrieved from
                // resources config bean each time.
                if (resourceConfiguration == null) {
                    resourceConfiguration =
                            (BindableResource) resourcesUtil.getResource(resourceInfo, BindableResource.class);
                    if (resourceConfiguration == null) {
                        String suffix = ConnectorsUtil.getValidSuffix(resourceInfo.getName());
                        // it is possible that the resource is a __PM or __NONTX suffixed resource used by JPA/EJB Container
                        // check for the enabled status and existence using non-prefixed resource-name
                        if (suffix != null) {
                            String nonPrefixedName = resourceInfo.getName().substring(0, resourceInfo.getName().lastIndexOf(suffix));
                            resourceInfo = new ResourceInfo(nonPrefixedName, resourceInfo.getApplicationName(),
                                    resourceInfo.getModuleName());
                            resourceConfiguration = (BindableResource)
                                    resourcesUtil.getResource(resourceInfo, BindableResource.class);
                        }
                    }
                } else {
                    // we cache the resourceConfiguration for performance optimization.
                    // make sure that appropriate (actual) resourceInfo is used for validation.
                    String suffix = ConnectorsUtil.getValidSuffix(resourceInfo.getName());
                    // it is possible that the resource is a __PM or __NONTX suffixed resource used by JPA/EJB Container
                    // check for the enabled status and existence using non-prefixed resource-name
                    if (suffix != null) {
                        String nonPrefixedName = resourceInfo.getName().substring(0, resourceInfo.getName().lastIndexOf(suffix));
                        resourceInfo = new ResourceInfo(nonPrefixedName, resourceInfo.getApplicationName(),
                                resourceInfo.getModuleName());
                    }
                }
                if (resourceConfiguration == null) {
                    throw new ResourceException("No such resource : " + resourceInfo);
                }
                if (!resourcesUtil.isEnabled(resourceConfiguration, resourceInfo)) {
                    throw new ResourceException(resourceInfo + " is not enabled");
                }
            }
        }

        if (registry.getPoolMetaData(poolInfo) == null) {
            String msg = getLocalStrings().getString("con_mgr.no_pool_meta_data", poolInfo);
            throw new ResourceException(poolInfo + ": " + msg);
        }
    }

    public void logFine(String message) {
        if (getLogger().isLoggable(Level.FINE)) {
            getLogger().fine(message);
        }
    }

    private static StringManager getLocalStrings() {
        if (localStrings == null) {
            synchronized (ConnectionManagerImpl.class) {
                if (localStrings == null) {
                    localStrings = StringManager.getManager(ConnectionManagerImpl.class);
                }
            }
        }
        return localStrings;
    }

    protected static Logger getLogger() {
        if (logger == null){
            synchronized(ConnectionManagerImpl.class) {
                if(logger == null) {
                    logger = LogDomains.getLogger(ConnectionManagerImpl.class,LogDomains.RSR_LOGGER);
                }
            }
        }
        return logger;
    }
}
