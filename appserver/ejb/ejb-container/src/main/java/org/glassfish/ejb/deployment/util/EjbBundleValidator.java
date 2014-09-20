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

package org.glassfish.ejb.deployment.util;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.ejb.containers.EJBTimerSchedule;
import com.sun.enterprise.deployment.Application;
import com.sun.enterprise.deployment.BundleDescriptor;
import com.sun.enterprise.deployment.EjbInterceptor;
import com.sun.enterprise.deployment.InjectionCapable;
import com.sun.enterprise.deployment.LifecycleCallbackDescriptor;
import com.sun.enterprise.deployment.MethodDescriptor;
import com.sun.enterprise.deployment.ResourceEnvReferenceDescriptor;
import com.sun.enterprise.deployment.ResourceReferenceDescriptor;
import com.sun.enterprise.deployment.ServiceReferenceDescriptor;
import com.sun.enterprise.deployment.WebService;
import com.sun.enterprise.deployment.types.EjbReference;
import com.sun.enterprise.deployment.types.MessageDestinationReferencer;
import com.sun.enterprise.deployment.util.ComponentValidator;
import com.sun.enterprise.deployment.util.DOLUtils;
import com.sun.enterprise.deployment.util.EjbBundleVisitor;
import com.sun.enterprise.util.LocalStringManagerImpl;
import org.glassfish.ejb.LogFacade;
import org.glassfish.ejb.deployment.descriptor.DummyEjbDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbBundleDescriptorImpl;
import org.glassfish.ejb.deployment.descriptor.EjbCMPEntityDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbEntityDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbMessageBeanDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbSessionDescriptor;
import org.glassfish.ejb.deployment.descriptor.FieldDescriptor;
import org.glassfish.ejb.deployment.descriptor.InterceptorBindingDescriptor;
import org.glassfish.ejb.deployment.descriptor.PersistenceDescriptor;
import org.glassfish.ejb.deployment.descriptor.RelationshipDescriptor;
import org.glassfish.ejb.deployment.descriptor.ScheduledTimerDescriptor;
import org.glassfish.internal.api.Globals;
import org.glassfish.internal.deployment.AnnotationTypesProvider;
import org.glassfish.logging.annotation.LogMessageInfo;

/**
 * This class validates a EJB Bundle descriptor once loaded from an .jar file
 *
 * @author Jerome Dochez
 */
public class EjbBundleValidator extends ComponentValidator implements EjbBundleVisitor, EjbVisitor {
    
    protected EjbBundleDescriptorImpl ejbBundleDescriptor=null;
    protected EjbDescriptor ejb = null;
    private static LocalStringManagerImpl localStrings =
            new LocalStringManagerImpl(EjbBundleValidator.class);
    private static final Logger _logger  = LogFacade.getLogger();

    @LogMessageInfo(
        message = "Passivation-capable value of stateful session bean [{0}] is false, " +
                "it should not have any PrePassivate nor PostActivate configuration, " +
                "but you have configuration at [{1}].",
        level = "WARNING")
    private static final String REDUNDANT_PASSIVATION_CALLBACK_METADATA = "AS-EJB-00048";
    
    @Override
    public void accept (BundleDescriptor descriptor) {
        this.bundleDescriptor = descriptor;
        this.application = descriptor.getApplication();
        if (descriptor instanceof EjbBundleDescriptorImpl) {
            EjbBundleDescriptorImpl ejbBundle = (EjbBundleDescriptorImpl)descriptor;
            accept(ejbBundle);

            for (EjbDescriptor anEjb : ejbBundle.getEjbs()) {
                anEjb.visit(getSubDescriptorVisitor(anEjb));
            }
            if (ejbBundle.hasRelationships()) {
                for (Iterator itr = ejbBundle.getRelationships().iterator();itr.hasNext();) {
                    RelationshipDescriptor rd = (RelationshipDescriptor) itr.next();
                    accept(rd);
                }
            }
            for (WebService aWebService : ejbBundle.getWebServices().getWebServices()) {
                accept(aWebService);
            }

            // Ejb-jar level dependencies

            // Visit all injectables first.  In some cases, basic type
            // information has to be derived from target inject method or 
            // inject field.
            for(InjectionCapable injectable : ejbBundle.getInjectableResources(ejbBundle)) {
                accept(injectable);
            }

            super.accept(descriptor);
        } else {
            super.accept(descriptor);
        }
    }

    @Override
    public void accept(com.sun.enterprise.deployment.EjbBundleDescriptor bundleDesc) {
        this.application = bundleDesc.getApplication();
        EjbBundleDescriptorImpl bundleDescriptor = (EjbBundleDescriptorImpl) bundleDesc;
        if (bundleDescriptor.getEjbs().size() == 0) {
            throw new IllegalArgumentException(localStrings.getLocalString(
                "enterprise.deployment.util.no_ejb_in_ejb_jar",
                "Invalid ejb jar {0}: it contains zero ejb. A valid ejb jar requires at least one session/entity/message driven bean.", 
                new Object[] {bundleDescriptor.getModuleDescriptor().getArchiveUri()})); 
        }

	    if (!bundleDescriptor.areResourceReferencesValid()) {
            throw new RuntimeException("Incorrectly resolved role references");
        }         

        this.ejbBundleDescriptor = bundleDescriptor;

        // Now that we have a classloader, we have to check for any
        // interceptor bindings that were specified in .xml to use
        // the syntax that refers to all overloaded methods with a
        // given name.  
        handleOverloadedInterceptorMethodBindings(bundleDescriptor);

        InterceptorBindingTranslator bindingTranslator = 
            new InterceptorBindingTranslator(bundleDescriptor);

        for(Iterator<EjbDescriptor> iter = bundleDescriptor.getEjbs().iterator(); iter.hasNext();) {
            EjbDescriptor ejb0 = iter.next();
            if(ejb0.isRemoteInterfacesSupported() && 
                (ejb0.getRemoteClassName() == null || ejb0.getRemoteClassName().trim().isEmpty())) {
                throw new IllegalArgumentException(localStrings.getLocalString(
                        "enterprise.deployment.util.componentInterfaceMissing", 
                        "{0} Component interface is missing in EJB [{1}]", "Remote", ejb0.getName()));
            }
            if(ejb0.isLocalInterfacesSupported() && 
                (ejb0.getLocalClassName() == null || ejb0.getLocalClassName().trim().isEmpty())) {
                throw new IllegalArgumentException(localStrings.getLocalString(
                        "enterprise.deployment.util.componentInterfaceMissing", 
                        "{0} Component interface is missing in EJB [{1}]", "Local", ejb0.getName()));
            }
            
            if(!EjbEntityDescriptor.TYPE.equals(ejb0.getType())) {
                ejb0.applyInterceptors(bindingTranslator);
            }
        }
    }

    private void handleOverloadedInterceptorMethodBindings(EjbBundleDescriptorImpl
                                                           bundleDesc) {

        List<InterceptorBindingDescriptor> origBindings =
            bundleDesc.getInterceptorBindings();

        if( origBindings.isEmpty() ) {
            return;
        }

        ClassLoader cl = bundleDesc.getClassLoader();

        List<InterceptorBindingDescriptor> newBindings = 
            new LinkedList<InterceptorBindingDescriptor>();

        for(InterceptorBindingDescriptor next : origBindings) {

            if( next.getNeedsOverloadResolution() ) {

                MethodDescriptor overloadedMethodDesc = 
                    next.getBusinessMethod();
                String methodName = overloadedMethodDesc.getName();
                // For method-specific interceptors, there must be an ejb-name.
                String ejbName = next.getEjbName();

                EjbDescriptor ejbDesc = bundleDesc.getEjbByName(ejbName);
                Class ejbClass = null;
           
                try {
                    ejbClass = cl.loadClass(ejbDesc.getEjbClassName());
                } catch(Exception e) {
                    RuntimeException re = new RuntimeException
                        ("Error loading ejb class "+ejbDesc.getEjbClassName());
                    re.initCause(e);
                    throw re;
                }

                boolean isMethod = false;

                for(Method ejbClassMethod : ejbClass.getDeclaredMethods()) {

                    if( ejbClassMethod.getName().equals(methodName) ) {

                        isMethod = true;

                        InterceptorBindingDescriptor newInterceptorBinding =
                            new InterceptorBindingDescriptor();

                        MethodDescriptor newMethodDesc = new MethodDescriptor
                            (ejbClassMethod, MethodDescriptor.EJB_BEAN);
                        
                        newInterceptorBinding.setEjbName(ejbName);
                        newInterceptorBinding.setBusinessMethod
                            (newMethodDesc);
                        for(String interceptorClass : 
                                next.getInterceptorClasses()) {
                            newInterceptorBinding.appendInterceptorClass
                                (interceptorClass);
                        }
                        newInterceptorBinding.setIsTotalOrdering
                            (next.getIsTotalOrdering());
                        newInterceptorBinding.setExcludeDefaultInterceptors
                            (next.getExcludeDefaultInterceptors());
                        newInterceptorBinding.setExcludeClassInterceptors
                            (next.getExcludeClassInterceptors());
                        
                        newBindings.add(newInterceptorBinding);

                    }

                }
                
                // We didn't find a method with this name in class methods,
                // check if it's a constructor
                if (!isMethod && methodName.equals(ejbClass.getSimpleName())) {
                    // Constructor - will resolve via implicit comparison
                    newBindings.add(next);
                    continue;                   
                }

            } else {

                newBindings.add(next);

            }

        }

        bundleDesc.setInterceptorBindings(newBindings);
    }

    /**
     * visits an ejb descriptor
     * @param ejb descriptor
     */
    @Override
    public void accept(EjbDescriptor ejb) {
        // all the DummyEjbDescriptor which stored partial information from
        // xml should already be resolved to actual ejb descriptors.
        // if not, this means there is a referencing error in the user 
        // application
        if (ejb instanceof DummyEjbDescriptor) {
            throw new IllegalArgumentException(localStrings.getLocalString(
            "enterprise.deployment.exceptionbeanbundle",
            "Referencing error: this bundle has no bean of name: {0}",
            new Object[] {ejb.getName()}));
        }

        this.ejb =ejb;
        setDOLDefault(ejb);
        computeRuntimeDefault(ejb);
        checkDependsOn(ejb);
        
        validateConcurrencyMetadata(ejb);
        validateStatefulTimeout(ejb);
        validatePassivationConfiguration(ejb);

        try {

            ClassLoader cl = ejb.getEjbBundleDescriptor().getClassLoader();
            Class ejbClass = cl.loadClass(ejb.getEjbClassName());

            if (Globals.getDefaultHabitat() == null) {
                return;
            }

            // Perform 2.x style TimedObject processing if the class 
            // hasn't already been identified as a timed object.  
            AnnotationTypesProvider provider = Globals.getDefaultHabitat().getService(AnnotationTypesProvider.class, "EJB");
            if (provider == null) {
                throw new RuntimeException("Cannot find AnnotationTypesProvider named 'EJB'");
            }

            if( ejb.getEjbTimeoutMethod() == null && 
                    provider.getType("javax.ejb.TimedObject").isAssignableFrom(ejbClass) ) {
                MethodDescriptor timedObjectMethod =
                        new MethodDescriptor("ejbTimeout",
                                                 "TimedObject timeout method",
                                                 new String[] {"javax.ejb.Timer"},
                                                 MethodDescriptor.TIMER_METHOD);
                ejb.setEjbTimeoutMethod(timedObjectMethod);

            } else if (ejb.getEjbTimeoutMethod() != null) {
                // If timeout-method was only processed from the descriptor,
                // we need to create a MethodDescriptor using the actual
                // Method object corresponding to the timeout method.  The
                // timeout method can have any access type and be anywhere
                // in the bean class hierarchy.
                MethodDescriptor timeoutMethodDescOrig = ejb.getEjbTimeoutMethod();
                MethodDescriptor timeoutMethodDesc = 
                        processTimeoutMethod(ejb, timeoutMethodDescOrig, provider, ejbClass);
                ejb.setEjbTimeoutMethod(timeoutMethodDesc);
            }

            for (ScheduledTimerDescriptor sd : ejb.getScheduledTimerDescriptors()) {
                try {
                    // This method creates new schedule and attempts to calculate next timeout.
                    // The second part ensures that all values that are not verified up-front
                    // are also validated.
                    // It does not check that such timeout date is a valid date.
                    EJBTimerSchedule.isValid(sd);
                } catch (Exception e) {
                    throw new RuntimeException(ejb.getName() + ": Invalid schedule " +
                            "defined on method " + sd.getTimeoutMethod().getFormattedString() +
                            ": " + e.getMessage());
                }

                MethodDescriptor timeoutMethodDescOrig = sd.getTimeoutMethod();
                MethodDescriptor timeoutMethodDesc =
                        processTimeoutMethod(ejb, timeoutMethodDescOrig, provider, ejbClass);
                sd.setTimeoutMethod(timeoutMethodDesc);
            }


        } catch(Exception e) {
            RuntimeException re = new RuntimeException
                ("Error processing EjbDescriptor");
            re.initCause(e);
            throw re;
        }
        /* It is possible to have an MDB class not implementing message-listener-type. 
        
        if(ejb instanceof EjbMessageBeanDescriptor){
            EjbMessageBeanDescriptor msgBeanDescriptor = (EjbMessageBeanDescriptor)ejb;
            String messageListenerType = msgBeanDescriptor.getMessageListenerType();
            String className = ejb.getEjbClassName();
            boolean matchFound = false;
            try {
                ClassLoader cl = ejb.getEjbBundleDescriptor().getClassLoader();
                Class ejbClass  = cl.loadClass(className);
                Class messageListenerIntf = cl.loadClass(messageListenerType);
                Class[] interfaces = ejbClass.getInterfaces();
                for(Class intf : interfaces){
                    if(messageListenerIntf.isAssignableFrom(intf)){
                        matchFound = true;
                    }
                }
            } catch (ClassNotFoundException e) {
                String msg = localStrings.getLocalString("enterprise.deployment.mdb_validation_failure",
                        "Exception during MDB validation");
                _logger.log(Level.WARNING,msg, e);
            }
            if(!matchFound){
                Object args[] = new Object[]{className, messageListenerType};
                String msg = localStrings.getLocalString("enterprise.deployment.mdb_validation_invalid_msg_listener",
                        "Class " + className + " does not implement messageListener type [ "+messageListenerType+" ] ",
                        args);
                throw new RuntimeException(msg);
            }
        }*/

        // Visit all injectables first.  In some cases, basic type information
        // has to be derived from target inject method or inject field.
        for (InjectionCapable injectable :
                ejb.getEjbBundleDescriptor().getInjectableResources(ejb)) {
            accept(injectable);
        }

        for (Iterator itr = ejb.getEjbReferenceDescriptors().iterator(); itr.hasNext();) {
            EjbReference aRef = (EjbReference) itr.next();
            accept(aRef);
        }

        for (Iterator it = ejb.getResourceReferenceDescriptors().iterator();
             it.hasNext();) {
            ResourceReferenceDescriptor next =
                    (ResourceReferenceDescriptor) it.next();
            accept(next);
        }

        for (Iterator it = ejb.getResourceEnvReferenceDescriptors().iterator(); it.hasNext();) {
            ResourceEnvReferenceDescriptor next =
                    (ResourceEnvReferenceDescriptor) it.next();
            accept(next);
        }

        for (Iterator it = ejb.getMessageDestinationReferenceDescriptors().iterator(); it.hasNext();) {
            MessageDestinationReferencer next =
                    (MessageDestinationReferencer) it.next();
            accept(next);
        }

        // If this is a message bean, it can be a message destination
        // referencer as well.
        if (ejb.getType().equals(EjbMessageBeanDescriptor.TYPE)) {
            if (ejb instanceof MessageDestinationReferencer) {
                MessageDestinationReferencer msgDestReferencer =
                    (MessageDestinationReferencer) ejb;
                if (msgDestReferencer.getMessageDestinationLinkName() != null) {
                    accept(msgDestReferencer);
                }
            }
        }

        Set serviceRefs = ejb.getServiceReferenceDescriptors();
        for (Iterator itr = serviceRefs.iterator(); itr.hasNext();) {
            accept((ServiceReferenceDescriptor) itr.next());
        }

        if (ejb instanceof EjbCMPEntityDescriptor) {
            EjbCMPEntityDescriptor cmp = (EjbCMPEntityDescriptor)ejb;
            PersistenceDescriptor persistenceDesc = cmp.getPersistenceDescriptor();
            for (Iterator e=persistenceDesc.getCMPFields().iterator();e.hasNext();) {
                FieldDescriptor fd = (FieldDescriptor) e.next();
                accept(fd);
            }
        }
    }    
        
    public void accept(WebService webService) {
    }


    private void validateConcurrencyMetadata(EjbDescriptor ejb) {

        if( ejb instanceof EjbSessionDescriptor ) {

            EjbSessionDescriptor sessionDesc = (EjbSessionDescriptor) ejb;

            List<EjbSessionDescriptor.AccessTimeoutHolder> accessTimeoutInfo =
                    sessionDesc.getAccessTimeoutInfo();

            for(EjbSessionDescriptor.AccessTimeoutHolder accessTimeoutHolder : accessTimeoutInfo) {
                MethodDescriptor accessTimeoutMethodDesc = accessTimeoutHolder.method;
                Method accessTimeoutMethod = accessTimeoutMethodDesc.getMethod(ejb);
                if(accessTimeoutMethod == null) {
                    throw new RuntimeException("Invalid AccessTimeout method signature "
                            + accessTimeoutMethodDesc +
                            " . Method could not be resolved to a bean class method for bean " +
                            ejb.getName());
                }
            }

            for(MethodDescriptor lockMethodDesc : sessionDesc.getReadAndWriteLockMethods()) {
                Method readLockMethod = lockMethodDesc.getMethod(sessionDesc);
                if( readLockMethod == null ) {
                    throw new RuntimeException("Invalid Lock method signature "
                            + lockMethodDesc +
                            " . Method could not be resolved to a bean class method for bean " +
                            ejb.getName());
                }

            }

        }

    }

    /**
     * Validates @StatefulTimeout or <stateful-timeout> values.  Any value less than -1
     * is invalid.
     */
    private void validateStatefulTimeout(EjbDescriptor ejb) {
        if(ejb instanceof EjbSessionDescriptor) {
            EjbSessionDescriptor sessionDesc = (EjbSessionDescriptor) ejb;
            Long statefulTimeoutValue = sessionDesc.getStatefulTimeoutValue();
            if(statefulTimeoutValue != null && statefulTimeoutValue < -1) {
                throw new IllegalArgumentException(localStrings.getLocalString(
                "enterprise.deployment.invalid_stateful_timeout_value",
                "Invalid value [{0}] for @StatefulTimeout or <stateful-timeout> element in EJB [{1}]. Values less than -1 are not valid.",
                new Object[] {statefulTimeoutValue, sessionDesc.getName()}));
            }
        }
    }

    /**
     * Check when passivation-capable of sfsb is false, PrePassivate and PostActivate configurations
     * are not recommended.
     */
    private void validatePassivationConfiguration(EjbDescriptor ejb) {
        if (ejb instanceof EjbSessionDescriptor) {
            EjbSessionDescriptor sessionDesc = (EjbSessionDescriptor) ejb;
            if (!sessionDesc.isStateful() || sessionDesc.isPassivationCapable()) {
                return;
            }

            String callbackInfo = getAllPrePassivatePostActivateCallbackInfo(sessionDesc);
            if (callbackInfo.length() > 0) {
                _logger.log(Level.WARNING, REDUNDANT_PASSIVATION_CALLBACK_METADATA, new Object[]{ejb.getName(), callbackInfo});
            }
        }
    }

    private String getAllPrePassivatePostActivateCallbackInfo(EjbSessionDescriptor sessionDesc) {
        List<LifecycleCallbackDescriptor> descriptors = new ArrayList<LifecycleCallbackDescriptor>();
        descriptors.addAll(sessionDesc.getPrePassivateDescriptors());
        descriptors.addAll(sessionDesc.getPostActivateDescriptors());
        for (EjbInterceptor interceptor : sessionDesc.getInterceptorClasses()) {
            descriptors.addAll(interceptor.getCallbackDescriptors(LifecycleCallbackDescriptor.CallbackType.PRE_PASSIVATE));
            descriptors.addAll(interceptor.getCallbackDescriptors(LifecycleCallbackDescriptor.CallbackType.POST_ACTIVATE));
        }

        StringBuilder result = new StringBuilder();
        for (LifecycleCallbackDescriptor each : descriptors) {
            result.append(each.getLifecycleCallbackClass());
            result.append(".");
            result.append(each.getLifecycleCallbackMethod());
            result.append(", ");
        }

        if (result.length() > 2) {
            return result.substring(0, result.length() - 2);
        } else {
            return result.toString();
        }
    }

    
    private void checkDependsOn(EjbDescriptor ejb) {

        if( ejb instanceof EjbSessionDescriptor ) {
            EjbSessionDescriptor sessionDesc = (EjbSessionDescriptor) ejb;
            if( sessionDesc.hasDependsOn()) {
                if( !sessionDesc.isSingleton() ) {
                    throw new RuntimeException("Illegal usage of DependsOn for EJB " +
                        ejb.getName() + ". DependsOn is only supported for Singleton beans");
                }
                String[] dependsOn = sessionDesc.getDependsOn();
                for(String next : dependsOn) {

                    // TODO support new EJB 3.1 syntax

                    boolean fullyQualified = next.contains("#");

                    Application app = sessionDesc.getEjbBundleDescriptor().getApplication();

                    if( fullyQualified ) {

                        int indexOfHash = next.indexOf("#");
                        String ejbName = next.substring(indexOfHash+1);
                        String relativeJarPath = next.substring(0, indexOfHash);

                        BundleDescriptor bundle = app.getRelativeBundle(sessionDesc.getEjbBundleDescriptor(),
                            relativeJarPath);

                        if( bundle == null ) {
                            throw new IllegalStateException("Invalid @DependOn value = " + next +
                                    " for Singleton " + sessionDesc.getName());
                        }

                        EjbBundleDescriptorImpl ejbBundle = (bundle.getModuleType() != null && bundle.getModuleType().equals(DOLUtils.warType())) ?
                                bundle.getExtensionsDescriptors(EjbBundleDescriptorImpl.class).iterator().next()
                                :  (EjbBundleDescriptorImpl) bundle;

                        if( !ejbBundle.hasEjbByName(ejbName) ) {
                            throw new RuntimeException("Invalid DependsOn dependency '" +
                               next + "' for EJB " + ejb.getName());
                        }

                    } else {

                        EjbBundleDescriptorImpl bundle = ejb.getEjbBundleDescriptor();
                        if( !bundle.hasEjbByName(next) ) {
                            throw new RuntimeException("Invalid DependsOn dependency '" +
                               next + "' for EJB " + ejb.getName());
                        }
                    }
                }
            }
        }

    }

    @Override
    protected EjbBundleDescriptorImpl getEjbBundleDescriptor() {
        return ejbBundleDescriptor;
    }

    @Override
    protected EjbDescriptor getEjbDescriptor() {
        return this.ejb;
    }

    /**
     * @return the Application object if any
     */
    @Override
    protected Application getApplication() {
        return application;
    }
     
    /**
     * @return the bundleDescriptor we are validating
     */
    @Override
    protected BundleDescriptor getBundleDescriptor() {
        return ejbBundleDescriptor;
    }

    /**
     * Set default value for EjbDescriptor.
     */
    private void setDOLDefault(EjbDescriptor ejb) {
        if (ejb.getUsesCallerIdentity() == null) {
            if (ejb instanceof EjbMessageBeanDescriptor) {
                ejb.setUsesCallerIdentity(false);
            } else {
                ejb.setUsesCallerIdentity(true);
            }
        }
        // for ejb 3.0
        if (ejb.getTransactionType() == null) {
            ejb.setTransactionType(EjbDescriptor.CONTAINER_TRANSACTION_TYPE);
        }
        ejb.setUsesDefaultTransaction();
    }

    /**
     * Set runtime default value for EjbDescriptor.
     */
    private void computeRuntimeDefault(EjbDescriptor ejb) {
        String intfName = null;

        if ((ejb.getJndiName() == null) || (ejb.getJndiName().length() == 0)) {
            if (ejb.isRemoteInterfacesSupported() && ejb.isRemoteBusinessInterfacesSupported()) {
                 // can't use a default.
            } else if (ejb.isRemoteInterfacesSupported()) {
                 // For 2.x view, use the Home as the basis for the default
                 intfName = ejb.getHomeClassName();
            } else if (ejb.isRemoteBusinessInterfacesSupported()) {
                 Set<String> classNames = ejb.getRemoteBusinessClassNames();
                 if (classNames.size() == 1) {
                     intfName = classNames.iterator().next();
                 }
            }
        }

        if( intfName != null ) {
            String jndiName = getDefaultEjbJndiName(intfName);
            ejb.setJndiName(jndiName);
        } 

        if (!ejb.getUsesCallerIdentity()) {
            computeRunAsPrincipalDefault(
                ejb.getRunAsIdentity(), ejb.getApplication());
        }
    }

    private MethodDescriptor processTimeoutMethod(EjbDescriptor ejb, 
            MethodDescriptor timeoutMethodDescOrig, AnnotationTypesProvider provider, 
            Class ejbClass) throws ClassNotFoundException {
        Method m = timeoutMethodDescOrig.getDeclaredMethod(ejb);
        if (m == null) {
           // In case deployment descriptor didn't specify "javax.ejb.Timer"
           // as the method-params, and we were not relying on it before,
           // check explicitly for a method with "javax.ejb.Timer" param type.
            Class[] params = new Class[1];
            if (provider!=null) {
                params[0] = provider.getType("javax.ejb.Timer");
            } else {
                throw new RuntimeException("Cannot find AnnotationTypesProvider named 'EJB'");
            }

            m = timeoutMethodDescOrig.getDeclaredMethod(ejb, params);
        }

        if (m == null) {
            throw new RuntimeException("Class " + ejbClass.getName() +
                    " does not define timeout method " + 
                    timeoutMethodDescOrig.getFormattedString());        
        }
        return new MethodDescriptor(m, MethodDescriptor.TIMER_METHOD);
    }
}
