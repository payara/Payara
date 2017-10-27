/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.api.invocation;


import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PerLookup;

import java.util.HashMap;
import java.util.Map;

@PerLookup
@Service
public class ComponentInvocation
    implements Cloneable {

    public enum ComponentInvocationType {
        SERVLET_INVOCATION, EJB_INVOCATION,
        APP_CLIENT_INVOCATION, UN_INITIALIZED,
        SERVICE_STARTUP
    }

    private ComponentInvocationType invocationType
            = ComponentInvocationType.UN_INITIALIZED;

    private boolean preInvokeDoneStatus;

    private Boolean auth;

    // the component instance, type Servlet, Filter or EnterpriseBean
    public Object instance;
    
    // the name of this instance
    private String instanceName;

    // ServletContext for servlet, Container for EJB
    public Object container;

    public Object jndiEnvironment;
    public void setJNDIEnvironment(Object val) {
      jndiEnvironment = val;
    }
    public Object getJNDIEnvironment() {
      return jndiEnvironment;
    }

    public String componentId;

    public Object transaction;

    // true if transaction commit or rollback is
    // happening for this invocation context
    private boolean transactionCompleting = false;

    //  security context coming in a call
    // security context changes on a runas call - on a run as call
    // the old logged in security context is stored in here.
    public Object oldSecurityContext;
    
    private Object resourceTableKey;

    private ResourceHandler resourceHandler;

    /**
     * Registry to be carried with this invocation
     */
    private Map<Class, Object> registry;
    
    protected String appName;
    
    protected String moduleName;

    public ComponentInvocation() {
        
    }
    
    public ComponentInvocation(String componentId,
            ComponentInvocationType invocationType,
            Object container,
            String appName,
            String moduleName) {
        this.componentId = componentId;
        this.invocationType = invocationType;
        this.container = container;
        this.appName = appName;
        this.moduleName = moduleName;
    }


    public ComponentInvocation(String componentId,
            ComponentInvocationType invocationType,
            Object instance, Object container,
            Object transaction) {
        this.componentId = componentId;
        this.invocationType = invocationType;
        this.instance = instance;
        this.container = container;
        this.transaction = transaction;
    }

    public ComponentInvocationType getInvocationType() {
        return invocationType;
    }

    public void setComponentInvocationType(ComponentInvocationType t) {
        this.invocationType = t;
    }

    public Object getInstance() {
        return instance;
    }
    
    public String getInstanceName() {
      return instanceName;
    }
    
    public void setInstanceName(String instanceName) {
      this.instanceName = instanceName;
    }

    public String getComponentId() {
        return this.componentId;
    }

    public Object getContainer() {
        return container;
    }

    public Object getContainerContext() {
        return container;
    }

    public Object getTransaction() {
        return transaction;
    }

    public void setTransaction(Object t) {
        this.transaction = t;
    }

    private Object transactionOperationsManager;
    public void setTransactionOperationsManager(Object transactionOperationsManager) {
        this.transactionOperationsManager = transactionOperationsManager;
    }

    public Object getTransactionOperationsManager() {
        return transactionOperationsManager;
    }

    /** 
     * Sets the security context of the call coming in
     */
    public void setOldSecurityContext (Object sc){
	this.oldSecurityContext = sc;
    }
    /**
     * gets the security context of the call that came in
     * before a new context for runas is made
     */
    public Object getOldSecurityContext (){
	return oldSecurityContext;
    }

    public boolean isTransactionCompleting() {
        return transactionCompleting;
    }

    public void setTransactionCompeting(boolean value) {
        transactionCompleting = value;
    }

    public void setResourceTableKey(Object key) {
        this.resourceTableKey = key;
    }

    public Object getResourceTableKey() {
        return resourceTableKey;
    }

    public void setResourceHandler(ResourceHandler h) {
        resourceHandler = h;
    }

    public ResourceHandler getResourceHandler() {
        return resourceHandler;
    }

    /**
     * @return Registry associated with this invocation for the given <code>key</code>
     */
    public Object getRegistryFor(Class key) {
        if(registry == null) {
            return null;
        } else {
            return registry.get(key);
        }
    }

    /**
     * Associate given <code></code>registry</code> with given <code>key</code> for this invocation
     */
    public void setRegistryFor(Class key, Object payLoad) {
        if(registry == null) {
            registry = new HashMap<Class, Object>();
        }
        registry.put(key, payLoad);
    }

    //In most of the cases we don't want registry entries from being reused in the cloned
    //  invocation, in which case, this method must be called. I am not sure if async
    //  ejb invocation must call this (It never did and someone in ejb team must investigate
    //  if clearRegistry() must be called from EjbAsyncInvocationManager)
    public void clearRegistry() {
        if (registry != null) {
            registry.clear();
        }
    }

    public boolean isPreInvokeDone() {
        return preInvokeDoneStatus;
    }

    public void setPreInvokeDone(boolean value) {
        preInvokeDoneStatus = value;
    }

    public Boolean getAuth() {
        return auth;
    }

    public void setAuth(boolean value) {
        auth = value;
    }
    
    /**
     * Returns the appName for the current invocation, equivalent to the value
     * bound to java:app/AppName, without the cost of lookup.  For standalone
     * modules, returns the same value as getModuleName().  For invocations that
     * are not on Java EE components, returns null.
     */
    public String getAppName() {
        return appName;
    }
    
    /**
     * Returns the moduleName for the current invocation, equivalent to the value 
     * bound to java:module/ModuleName, without the cost of lookup.  For invocations
     * that are not on Java EE components, returns null.
     */
    public String getModuleName() {
        return moduleName;
    }

    public ComponentInvocation clone() {
        ComponentInvocation newInv = null;
        try {
            newInv = (ComponentInvocation) super.clone();
        } catch (CloneNotSupportedException cnsEx) {
            //Shouldn't happen as we implement Cloneable
            throw new Error(cnsEx);
        }

        newInv.auth = null;
        newInv.preInvokeDoneStatus = false;
        newInv.instance = null;
        newInv.transaction = null;
        newInv.transactionCompleting = false;

        return newInv;
    }
}
