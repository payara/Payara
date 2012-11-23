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

package org.glassfish.ejb.deployment.descriptor;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

import com.sun.enterprise.deployment.Application;
import com.sun.enterprise.deployment.LifecycleCallbackDescriptor;
import com.sun.enterprise.deployment.MethodDescriptor;
import com.sun.enterprise.deployment.util.TypeUtil;
import com.sun.enterprise.util.LocalStringManagerImpl;
import org.glassfish.internal.api.Globals;
import org.glassfish.internal.deployment.AnnotationTypesProvider;

/**
    * Objects of this kind represent the deployment information describing a single 
    * Session Ejb : { stateful , stateless, singleton }
    *@author Danny Coward
    */

public class EjbSessionDescriptor extends EjbDescriptor
        implements com.sun.enterprise.deployment.EjbSessionDescriptor {

    private Set<LifecycleCallbackDescriptor> postActivateDescs =
        new HashSet<LifecycleCallbackDescriptor>();
    private Set<LifecycleCallbackDescriptor> prePassivateDescs =
        new HashSet<LifecycleCallbackDescriptor>();

    // For EJB 3.0 stateful session beans, information about the assocation
    // between a business method and bean removal.
    private Map<MethodDescriptor, EjbRemovalInfo> removeMethods
        = new HashMap<MethodDescriptor, EjbRemovalInfo>();

    // For EJB 3.0 stateful session beans with adapted homes, list of
    // business methods corresponding to Home/LocalHome create methods.
    private Set<EjbInitInfo> initMethods=new HashSet<EjbInitInfo>();

    private MethodDescriptor afterBeginMethod = null;
    private MethodDescriptor beforeCompletionMethod = null;
    private MethodDescriptor afterCompletionMethod = null;

    // Holds @StatefulTimeout or stateful-timeout from
    // ejb-jar.xml.  Only applies to stateful session beans.
    // Initialize to "not set"(null) state so annotation processing
    // can apply the correct overriding behavior.
    private Long statefulTimeoutValue = null;
    private TimeUnit statefulTimeoutUnit;

    private boolean sessionTypeIsSet = false;
    private boolean isStateless = false;
    private boolean isStateful  = false;
    private boolean isSingleton = false;
    // ejb3.2 spec 4.6.5 Disabling Passivation of Stateful Session Beans
    private boolean isPassivationCapable = true;
    private boolean passivationCapableIsSet = false;

    private List<MethodDescriptor> readLockMethods = new ArrayList<MethodDescriptor>();
    private List<MethodDescriptor> writeLockMethods = new ArrayList<MethodDescriptor>();
    private List<AccessTimeoutHolder> accessTimeoutMethods =
            new ArrayList<AccessTimeoutHolder>();
    private List<MethodDescriptor> asyncMethods = new ArrayList<MethodDescriptor>();

    // Controls eager vs. lazy Singleton initialization
    private Boolean initOnStartup = null;

    private static final String[] _emptyDepends = new String[] {};

    private String[] dependsOn = _emptyDepends;


    private ConcurrencyManagementType concurrencyManagementType;
    
    private static LocalStringManagerImpl localStrings =
	    new LocalStringManagerImpl(EjbSessionDescriptor.class); 

    /**
	*  Default constructor.
	*/
    public EjbSessionDescriptor() {
    }

    @Override
    public String getEjbTypeForDisplay() {
        if (isStateful()) {
            return "StatefulSessionBean";
        } else if (isStateless()) {
            return "StatelessSessionBean";
        } else {
            return "SingletonSessionBean";
        }
    }

    public boolean isPassivationCapable() {
        return isPassivationCapable;
    }

    public void setPassivationCapable(boolean passivationCapable) {
        isPassivationCapable = passivationCapable;
        passivationCapableIsSet = true;
    }

    public boolean isPassivationCapableSet() {
        return passivationCapableIsSet;
    }

    /**
	* Returns the type of this bean - always "Session".
	*/
    public String getType() {
	    return TYPE;
    }
    
    /**
    * Returns the string STATELESS or STATEFUL according as to whether
    * the bean is stateless or stateful.
    **/
    
    public String getSessionType() {
	    if (this.isStateless()) {
	        return STATELESS;
	    } else if( isStateful() ){
	        return STATEFUL;
	    } else {
            return SINGLETON;
        }
    }
    
	/** 
	* Accepts the Strings STATELESS / STATEFUL / SINGLETON
	*/
    public void setSessionType(String sessionType) {
	    if (STATELESS.equals(sessionType)) {
	       isStateless = true;
	    } else if(STATEFUL.equals(sessionType)) {
	       isStateful = true;
        } else if(SINGLETON.equals(sessionType)){
            isSingleton = true;
        } else {
            if (this.isBoundsChecking()) {
	        throw new IllegalArgumentException(localStrings.getLocalString(
		        "enterprise.deployment.exceptionsessiontypenotlegaltype",
		        "{0} is not a legal session type for session ejbs. The type must be {1} or {2}",
                new Object[] {sessionType, STATEFUL, STATELESS}));
	        }

	    }
        sessionTypeIsSet = true;
        return;
    }

    /**
     * Useful for certain annotation / .xml processing.  ejb-jar.xml might
     * not set <session-type> if it's only being used for sparse overriding.
     * @return
     */
    public boolean isSessionTypeSet() {
        return sessionTypeIsSet;
    }
    
	/**
	* Sets my type
	*/
    public void setType(String type) {
	    throw new IllegalArgumentException(localStrings.getLocalString(
								   "enterprise.deployment.exceptioncannotsettypeofsessionbean",
								   "Cannot set the type of a session bean"));
    }
    

    
	/**
	*  Sets the transaction type for this bean. Must be either BEAN_TRANSACTION_TYPE or CONTAINER_TRANSACTION_TYPE.
	*/
    public void setTransactionType(String transactionType) {
	    boolean isValidType = (BEAN_TRANSACTION_TYPE.equals(transactionType) ||
				CONTAINER_TRANSACTION_TYPE.equals(transactionType));
				
	    if (!isValidType && this.isBoundsChecking()) {
	        throw new IllegalArgumentException(localStrings.getLocalString(
									   "enterprise.deployment..exceptointxtypenotlegaltype",
									   "{0} is not a legal transaction type for session beans", new Object[] {transactionType}));
	    } else {
	        super.transactionType = transactionType;
	        super.setMethodContainerTransactions(new Hashtable());

	    }
    }
    
	/**
	* Returns true if I am describing a stateless session bean.
	*/
    public boolean isStateless() {
	    return isStateless;
    }
    
    public boolean isStateful() {
        return isStateful;
    }

    public boolean isSingleton() {
        return isSingleton;
    }

    public boolean hasAsynchronousMethods() {
        return (asyncMethods.size() > 0);    
    }

    public void addAsynchronousMethod(MethodDescriptor m) {
        asyncMethods.add(m);
    }

    public List<MethodDescriptor> getAsynchronousMethods() {
        return new ArrayList<MethodDescriptor>(asyncMethods);
    }

    public boolean isAsynchronousMethod(Method m) {

        boolean async = false;
        for(MethodDescriptor next : asyncMethods) {
            Method nextMethod = next.getMethod(this);
            if( (nextMethod != null)  &&
                TypeUtil.sameMethodSignature(m, nextMethod)) {
                async = true;
                break;
            }
        }
        return async;
    }

    public void addStatefulTimeoutDescriptor(TimeoutValueDescriptor timeout) {
        statefulTimeoutValue = timeout.getValue();
        statefulTimeoutUnit  = timeout.getUnit();
    }

    public void setStatefulTimeout(Long value, TimeUnit unit) {
        statefulTimeoutValue = value;
        statefulTimeoutUnit = unit;
    }

    public boolean hasStatefulTimeout() {
        return (statefulTimeoutValue != null);
    }

    public Long getStatefulTimeoutValue() {
        return statefulTimeoutValue;
    }

    public TimeUnit getStatefulTimeoutUnit() {
        return statefulTimeoutUnit;
    }

    public boolean hasRemoveMethods() {
        return (!removeMethods.isEmpty());
    }

    /**
     * @return remove method info for the given method or null if the
     * given method is not a remove method for this stateful session bean.
     */
    public EjbRemovalInfo getRemovalInfo(MethodDescriptor method) {
        // first try to find the exact match
        for (MethodDescriptor methodDesc : removeMethods.keySet()) {
            if (methodDesc.equals(method)) {
                return removeMethods.get(methodDesc);
            }
        }

        // if nothing is found, try to find the loose match
        for (MethodDescriptor methodDesc : removeMethods.keySet()) {
            if (methodDesc.implies(method)) {
                return removeMethods.get(methodDesc);
            }
        }

        return null;
    }

    public Set<EjbRemovalInfo> getAllRemovalInfo() {
        return new HashSet<EjbRemovalInfo>(removeMethods.values());
    }

    // FIXME by srini - validate changing CDI code to use this is fine
    @Override
    public Set<MethodDescriptor> getRemoveMethodDescriptors() {
        return new HashSet<MethodDescriptor>(removeMethods.keySet());
    }

    public void addRemoveMethod(EjbRemovalInfo removalInfo) {
        removeMethods.put(removalInfo.getRemoveMethod(), removalInfo);
    }

    public boolean hasInitMethods() {
        return (!initMethods.isEmpty());
    }

    public Set<EjbInitInfo> getInitMethods() {
        return new HashSet<EjbInitInfo>(initMethods);
    }

    public void addInitMethod(EjbInitInfo initInfo) {
        initMethods.add(initInfo);
    }
    
    public Set<LifecycleCallbackDescriptor> getPostActivateDescriptors() {
        if (postActivateDescs == null) {
            postActivateDescs = 
                new HashSet<LifecycleCallbackDescriptor>(); 
        }
        return postActivateDescs;
    }   
            
    public void addPostActivateDescriptor(LifecycleCallbackDescriptor
        postActivateDesc) {
        String className = postActivateDesc.getLifecycleCallbackClass();
        boolean found = false;
        for (LifecycleCallbackDescriptor next :
             getPostActivateDescriptors()) {
            if (next.getLifecycleCallbackClass().equals(className)) {
                found = true;
                break;
            }
        }
        if (!found) {
            getPostActivateDescriptors().add(postActivateDesc);
        }
    }

    public LifecycleCallbackDescriptor 
        getPostActivateDescriptorByClass(String className) {

        for (LifecycleCallbackDescriptor next :
                 getPostActivateDescriptors()) {
            if (next.getLifecycleCallbackClass().equals(className)) {
                return next;
            }
        }
        return null;
    }

    public boolean hasPostActivateMethod() {
        return (getPostActivateDescriptors().size() > 0);
    }

    public Set<LifecycleCallbackDescriptor> getPrePassivateDescriptors() {
        if (prePassivateDescs == null) {
            prePassivateDescs = 
                new HashSet<LifecycleCallbackDescriptor>(); 
        }
        return prePassivateDescs;
    }   
            
    public void addPrePassivateDescriptor(LifecycleCallbackDescriptor
        prePassivateDesc) {
        String className = prePassivateDesc.getLifecycleCallbackClass();
        boolean found = false;
        for (LifecycleCallbackDescriptor next :
             getPrePassivateDescriptors()) {
            if (next.getLifecycleCallbackClass().equals(className)) {
                found = true;
                break;
            }
        }
        if (!found) {
            getPrePassivateDescriptors().add(prePassivateDesc);
        }
    }

    public LifecycleCallbackDescriptor 
        getPrePassivateDescriptorByClass(String className) {

        for (LifecycleCallbackDescriptor next :
                 getPrePassivateDescriptors()) {
            if (next.getLifecycleCallbackClass().equals(className)) {
                return next;
            }
        }
        return null;
    }

    public boolean hasPrePassivateMethod() {
        return (getPrePassivateDescriptors().size() > 0);
    }

    public Vector getPossibleTransactionAttributes() {
        Vector txAttributes = super.getPossibleTransactionAttributes();

        // Session beans that implement SessionSynchronization interface
        // have a limited set of possible transaction attributes.
        if( isStateful() ) {
            try {
                EjbBundleDescriptorImpl ejbBundle = getEjbBundleDescriptor();

                ClassLoader classLoader = ejbBundle.getClassLoader();
                Class ejbClass = classLoader.loadClass(getEjbClassName());

                AnnotationTypesProvider provider = Globals.getDefaultHabitat().getService(AnnotationTypesProvider.class, "EJB");
                if (provider!=null) {
                    Class sessionSynchClass = provider.getType("javax.ejb.SessionSynchronization");
                    if( sessionSynchClass.isAssignableFrom(ejbClass) ) {
                        txAttributes = new Vector();
                        txAttributes.add(new ContainerTransaction
                            (ContainerTransaction.REQUIRED, ""));
                        txAttributes.add(new ContainerTransaction
                            (ContainerTransaction.REQUIRES_NEW, ""));
                        txAttributes.add(new ContainerTransaction
                            (ContainerTransaction.MANDATORY, ""));
                    }
                }
            } catch(Exception e) {
                // Don't treat this as a fatal error.  Just return full
                // set of possible transaction attributes.
            }
        }
        return txAttributes;
    }

  @Override
  public String getContainerFactoryQualifier() {
    if(isStateful)
      return "StatefulContainerFactory";
    if(isStateless)
      return "StatelessContainerFactory";
    return "SingletonContainerFactory";
  }

  public void addAfterBeginDescriptor(MethodDescriptor m) {
        afterBeginMethod = m;
    }

    public void addBeforeCompletionDescriptor(MethodDescriptor m) {
        beforeCompletionMethod = m;
    }

    public void addAfterCompletionDescriptor(MethodDescriptor m) {
        afterCompletionMethod = m;
    }

    /**
     * Set the Method annotated @AfterBegin.
     */
    public void setAfterBeginMethodIfNotSet(MethodDescriptor m) {
        if( afterBeginMethod == null) {
            afterBeginMethod = m;
        }
    }
    
    /**
     * Returns the Method annotated @AfterBegin.
     */
    public MethodDescriptor getAfterBeginMethod() {
        return afterBeginMethod;
    }
    
    /**
     * Set the Method annotated @BeforeCompletion.
     */
    public void setBeforeCompletionMethodIfNotSet(MethodDescriptor m) {
        if( beforeCompletionMethod == null ) {
            beforeCompletionMethod = m;
        }
    }
    
    /**
     * Returns the Method annotated @AfterBegin.
     */
    public MethodDescriptor getBeforeCompletionMethod() {
        return beforeCompletionMethod;
    }
    
    /**
     * Set the Method annotated @AfterCompletion.
     */
    public void setAfterCompletionMethodIfNotSet(MethodDescriptor m) {
        if( afterCompletionMethod == null ) {
            afterCompletionMethod = m;
        }
    }
    
    /**
     * Returns the Method annotated @AfterCompletion.
     */
    public MethodDescriptor getAfterCompletionMethod() {
        return afterCompletionMethod;
    }


    public boolean getInitOnStartup() {
        return ( (initOnStartup != null) && initOnStartup );
    }

    public void setInitOnStartup(boolean flag) {
        initOnStartup = flag;
    }

    public void setInitOnStartupIfNotAlreadySet(boolean flag) {
        if( initOnStartup == null ) {
            setInitOnStartup(flag);
        }
    }

    public String[] getDependsOn() {
        return dependsOn;
    }

    public boolean hasDependsOn() {
        return (dependsOn.length > 0);
    }

    public void setDependsOn(String[] dep) {
        dependsOn = (dep == null) ? _emptyDepends : dep;
    }

    public void setDependsOnIfNotSet(String[] dep) {
        if( !hasDependsOn() ) {
            setDependsOn(dep);
        }
    }

    public ConcurrencyManagementType getConcurrencyManagementType() {
        return (concurrencyManagementType != null) ? concurrencyManagementType :
                ConcurrencyManagementType.Container;
    }

    public boolean hasContainerManagedConcurrency() {
        return (getConcurrencyManagementType() == ConcurrencyManagementType.Container);
    }

    public boolean hasBeanManagedConcurrency() {
        return (getConcurrencyManagementType() == ConcurrencyManagementType.Bean);
    }


    public void setConcurrencyManagementType(ConcurrencyManagementType type) {
        concurrencyManagementType = type;
    }

    public void setConcurrencyManagementTypeIfNotSet(ConcurrencyManagementType type) {
        if( concurrencyManagementType == null) {
            setConcurrencyManagementType(type);
        }
    }

    public void addConcurrentMethodFromXml(ConcurrentMethodDescriptor concMethod) {

        // .xml must contain a method.  However, both READ/WRITE lock metadata
        // and access timeout are optional.


        MethodDescriptor methodDesc = concMethod.getConcurrentMethod();

        if( concMethod.hasLockMetadata()) {

            if( concMethod.isWriteLocked()) {
                addWriteLockMethod(methodDesc);
            } else {
                addReadLockMethod(methodDesc);
            }
        }

        if( concMethod.hasAccessTimeout() ) {

            this.addAccessTimeoutMethod(methodDesc, concMethod.getAccessTimeoutValue(),
                    concMethod.getAccessTimeoutUnit());    
        }

    }

    public void addReadLockMethod(MethodDescriptor methodDescriptor) {
        readLockMethods.add(methodDescriptor);
    }

    public void addWriteLockMethod(MethodDescriptor methodDescriptor) {
        writeLockMethods.add(methodDescriptor);
    }

    public List<MethodDescriptor> getReadLockMethods() {
        return new ArrayList<MethodDescriptor>(readLockMethods);
    }

    public List<MethodDescriptor> getWriteLockMethods() {
        return new ArrayList<MethodDescriptor>(writeLockMethods);
    }

    public List<MethodDescriptor> getReadAndWriteLockMethods() {
        List<MethodDescriptor> readAndWriteLockMethods = new ArrayList<MethodDescriptor>();
        readAndWriteLockMethods.addAll(readLockMethods);
        readAndWriteLockMethods.addAll(writeLockMethods);
        return readAndWriteLockMethods;
    }

    public void addAccessTimeoutMethod(MethodDescriptor methodDescriptor, long value,
                                       TimeUnit unit) {
        accessTimeoutMethods.add(new AccessTimeoutHolder(value, unit, methodDescriptor));
    }

    public List<MethodDescriptor> getAccessTimeoutMethods() {
        List<MethodDescriptor> methods = new ArrayList<MethodDescriptor>();
        for(AccessTimeoutHolder holder : accessTimeoutMethods){
            methods.add(holder.method);
        }
        return methods;
    }

    public List<AccessTimeoutHolder> getAccessTimeoutInfo() {
        List<AccessTimeoutHolder> all = new ArrayList<AccessTimeoutHolder>();
        for(AccessTimeoutHolder holder : accessTimeoutMethods){
            all.add(holder);
        }
        return all;
    }

	/**
	* Returns a formatted String of the attributes of this object.
	*/
    public void print(StringBuffer toStringBuffer) {
	    toStringBuffer.append("Session descriptor");
	    toStringBuffer.append("\n sessionType ").append(getSessionType());
	    super.print(toStringBuffer);
    }

    /**
     * Return the fully-qualified portable JNDI name for a given
     * client view (Remote, Local, or no-interface).  
     */
    public String getPortableJndiName(String clientViewType) {
        String appName = null;

        Application app = getEjbBundleDescriptor().getApplication();
        if ( ! app.isVirtual() ) {
            appName = app.getAppName();
        }

        String modName = getEjbBundleDescriptor().getModuleDescriptor().getModuleName();

        StringBuffer javaGlobalPrefix = new StringBuffer("java:global/");

        if (appName != null) {
            javaGlobalPrefix.append(appName);
            javaGlobalPrefix.append("/");
        }

        javaGlobalPrefix.append(modName);
        javaGlobalPrefix.append("/");

        javaGlobalPrefix.append(getName());

        javaGlobalPrefix.append("!");
        javaGlobalPrefix.append(clientViewType);

        return javaGlobalPrefix.toString();
    }

    public static class AccessTimeoutHolder {
        public AccessTimeoutHolder(long v, TimeUnit u, MethodDescriptor m) {
            value = v;
            unit = u;
            method = m;
        }
        public long value;
        public TimeUnit unit;
        public MethodDescriptor method;
    }

    public enum ConcurrencyManagementType {
        Bean,
        Container,
    }

}
