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

package com.sun.enterprise.deployment.util;

import com.sun.enterprise.deployment.*;
import com.sun.enterprise.deployment.types.*;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.logging.LogDomains;
import org.glassfish.api.deployment.archive.ArchiveType;
import org.glassfish.internal.api.Globals;

import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class validates a EJB Bundle descriptor once loaded from an .jar file
 *
 * @author Jerome Dochez
 */
public class EjbBundleValidator extends ComponentValidator implements EjbBundleVisitor, EjbVisitor {
    
    protected EjbBundleDescriptor ejbBundleDescriptor=null;
    protected EjbDescriptor ejb = null;
    private static LocalStringManagerImpl localStrings =
            new LocalStringManagerImpl(EjbBundleValidator.class);
    private static final Logger _logger = LogDomains.getLogger(DOLUtils.class, LogDomains.DPL_LOGGER);

    public void accept (BundleDescriptor descriptor) {
        if (descriptor instanceof EjbBundleDescriptor) {
            EjbBundleDescriptor ejbBundle = (EjbBundleDescriptor)descriptor;
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

    /** visits an ejb bundle descriptor
     * @param bundleDescriptor ejb bundle descriptor
     */
    public void accept(EjbBundleDescriptor bundleDescriptor) {
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

    private void handleOverloadedInterceptorMethodBindings(EjbBundleDescriptor
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

                for(Method ejbClassMethod : ejbClass.getDeclaredMethods()) {

                    if( ejbClassMethod.getName().equals(methodName) ) {

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

        try {

            ClassLoader cl = ejb.getEjbBundleDescriptor().getClassLoader();
            Class ejbClass = cl.loadClass(ejb.getEjbClassName());

            if (Globals.getDefaultHabitat() == null) {
                return;
            }

            // Perform 2.x style TimedObject processing if the class 
            // hasn't already been identified as a timed object.  
            AnnotationTypesProvider provider = Globals.getDefaultHabitat().getComponent(AnnotationTypesProvider.class, "EJB");
            if( !ejb.isTimedObject() ) {

                if (provider!=null) {
                    if( provider.getType("javax.ejb.TimedObject").isAssignableFrom(ejbClass) ) {
                        MethodDescriptor timedObjectMethod =
                            new MethodDescriptor("ejbTimeout",
                                                 "TimedObject timeout method",
                                                 new String[] {"javax.ejb.Timer"},
                                                 MethodDescriptor.EJB_BEAN);
                        ejb.setEjbTimeoutMethod(timedObjectMethod);
                    }
                } else {
                    throw new RuntimeException("Cannot find AnnotationTypesProvider named 'EJB'");
                }

            } else {
                // If timeout-method was only processed from the descriptor,
                // we need to create a MethodDescriptor using the actual
                // Method object corresponding to the timeout method.  The
                // timeout method can have any access type and be anywhere
                // in the bean class hierarchy.
                if (ejb.getEjbTimeoutMethod() != null) {
                    MethodDescriptor timeoutMethodDescOrig = ejb.getEjbTimeoutMethod();
                    MethodDescriptor timeoutMethodDesc = 
                            processTimeoutMethod(ejb, timeoutMethodDescOrig, provider, ejbClass);
                    ejb.setEjbTimeoutMethod(timeoutMethodDesc);
                }

                ScheduledTimerValidator validator = Globals.getDefaultHabitat().
                        getComponent(ScheduledTimerValidator.class);
                for (ScheduledTimerDescriptor sd : ejb.getScheduledTimerDescriptors()) {
                    if (validator != null) {
                        try {
                            validator.validateScheduledTimerDescriptor(sd);
                        } catch (Exception e) {
                            throw new RuntimeException(ejb.getName() + ": Invalid schedule " + 
                                "defined on method " + sd.getTimeoutMethod().getFormattedString() + 
                                ": " + e.getMessage());
                        }
                    }

                    MethodDescriptor timeoutMethodDescOrig = sd.getTimeoutMethod();
                    MethodDescriptor timeoutMethodDesc = 
                            processTimeoutMethod(ejb, timeoutMethodDescOrig, provider, ejbClass);
                    sd.setTimeoutMethod(timeoutMethodDesc);
                }

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

        for (Iterator it = ejb.getJmsDestinationReferenceDescriptors().iterator(); it.hasNext();) {
            JmsDestinationReferenceDescriptor next =
                    (JmsDestinationReferenceDescriptor) it.next();
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

                        EjbBundleDescriptor ejbBundle = (bundle.getModuleType() != null && bundle.getModuleType().equals(org.glassfish.deployment.common.DeploymentUtils.warType())) ?
                                bundle.getExtensionsDescriptors(EjbBundleDescriptor.class).iterator().next()
                                :  (EjbBundleDescriptor) bundle;

                        if( !ejbBundle.hasEjbByName(ejbName) ) {
                            throw new RuntimeException("Invalid DependsOn dependency '" +
                               next + "' for EJB " + ejb.getName());
                        }

                    } else {

                        EjbBundleDescriptor bundle = ejb.getEjbBundleDescriptor();
                        if( !bundle.hasEjbByName(next) ) {
                            throw new RuntimeException("Invalid DependsOn dependency '" +
                               next + "' for EJB " + ejb.getName());
                        }
                    }
                }
            }
        }

    }

    /**
     * visits an ejb reference for the last J2EE component visited
     * @param ejbRef ejb reference
     */
    public void accept(EjbReference ejbRef) {
        DOLUtils.getDefaultLogger().fine("Visiting Ref" + ejbRef);
	if (ejbRef.getEjbDescriptor()!=null) 
            return;

        // let's try to derive the ejb-ref-type first it is not defined
        if (ejbRef.getType() == null) {
            // if it's EJB30 (no home/local home), it must be session
            if (ejbRef.isEJB30ClientView()) {
                ejbRef.setType("Session");
            } else {
                // if home interface has findByPrimaryKey method, 
                // it's entity, otherwise it's session
                String homeIntf = ejbRef.getEjbHomeInterface();
                BundleDescriptor referringJar = ejbRef.getReferringBundleDescriptor();
                if (referringJar == null) {
                    referringJar = getBundleDescriptor();
                }           
                ClassLoader classLoader = referringJar.getClassLoader();

                Class clazz = null;
                try {
                    clazz = classLoader.loadClass(homeIntf);
                    for (Method method : clazz.getDeclaredMethods()) {
                        if (method.getName().equals("findByPrimaryKey")) {
                            ejbRef.setType("Entity");
                            break;
                        }
                    }
                    if (ejbRef.getType() == null) {
                        ejbRef.setType("Session");
                    }
                } catch(Exception e) {
                    _logger.log(Level.FINE, "Could not load " + homeIntf, e);
                }
            }
        }
  
        //
        // NOTE : In the 3.0 local/remote business view, the local vs.
        // remote designation is not always detectable from the interface 
        // itself.
        //
        // That means 
        // 
        // 1) we need to figure it out during this stage of the processing
        // 2) the EjbReferenceDescriptor.isLocal() operations shouldn't be
        //    be used before the post-application validation stage since its
        //    value would be unreliable.
        // 3) We can't write out the standard deployment descriptors to XML
        //    until the full application has been processed, including this
        //    validation stage.
        //
        // During @EJB processing, setLocal() is set to false if 
        // local vs. remote is ambiguous.  setLocal() is set to true within this 
        // method upon successfuly resolution to a local business interface.
        //

        if (ejbRef.getJndiName()!=null && ejbRef.getJndiName().length()!=0) {


            // ok this is getting a little complicated here
            // the jndi name is not null, if this is a remote ref, proceed with resolution
            // if this is a local ref, proceed with resolution only if ejb-link is null            
            if (!ejbRef.isLocal() || (ejbRef.isLocal() && ejbRef.getLinkName()==null)) {
                DOLUtils.getDefaultLogger().fine("Ref " + ejbRef.getName() + " is bound to Ejb with JNDI Name " + ejbRef.getJndiName());
                if (getEjbDescriptors() != null) {
                    for (Iterator iter = getEjbDescriptors().iterator(); iter.hasNext();) {
                        EjbDescriptor ejb = (EjbDescriptor)iter.next();

                        if (ejbRef.getJndiName().equals(ejb.getJndiName())) {
                            ejbRef.setEjbDescriptor(ejb);
                            return;
                        } 
                    }
                }
            }
        }

        // If the reference does not have an ejb-link or jndi-name or lookup string associated
        // with it, attempt to resolve it by checking against all the ejbs
        // within the application.  If no match is found, just fall through
        // and let the existing error-checking logic kick in.
        if (( (ejbRef.getJndiName() == null) || 
              (ejbRef.getJndiName().length() == 0) )
            &&
            ( (ejbRef.getLinkName() == null) ||
              (ejbRef.getLinkName().length() == 0) )
            && !ejbRef.hasLookupName() ) {

            Map<String, EjbIntfInfo> ejbIntfInfoMap = getEjbIntfMap();
            if ( ejbIntfInfoMap.size() > 0 ) {

                String interfaceToMatch = ejbRef.isEJB30ClientView() ?
                    ejbRef.getEjbInterface() : ejbRef.getEjbHomeInterface();

                EjbIntfInfo intfInfo = ejbIntfInfoMap.get(interfaceToMatch);

                // make sure exactly one match
                if ( intfInfo != null ) {
                    int numMatches = intfInfo.ejbs.size();
                    if( numMatches == 1 ) {
                        Iterator iter = intfInfo.ejbs.iterator();

                        EjbDescriptor target = (EjbDescriptor)iter.next();

                        BundleDescriptor targetModule = 
                            target.getEjbBundleDescriptor();
                        BundleDescriptor sourceModule =
                            ejbRef.getReferringBundleDescriptor();

                        Application app = targetModule.getApplication();

                        //
                        // It's much cleaner to derive the ejb-link value
                        // and set that instead of the descriptor.  This way,
                        // if there are multiple ejb-jars within the .ear that
                        // each have an ejb with the target bean's ejb-name,
                        // there won't be any ambiguity about which one is
                        // the correct target.  It's not so much a problem
                        // during this phase of the processing, but if the
                        // fully-qualified ejb-link name is required and is not
                        // written out, there could be non-deterministic
                        // behavior when the application is re-loaded.
                        // Let the ejb-link processing logic handle the 
                        // conversion to ejb descriptor.
                        //

                        // If the ejb reference and the target ejb are defined
                        // within the same ejb-jar, the ejb-link will only
                        // be set to ejb-name.  This is done regardless of
                        // whether the ejb-jar is within an .ear or is
                        // stand-alone.  The ejb-link processing
                        // logic will always check the current ejb-jar
                        // first so there won't be any ambiguity.  
                        String ejbLinkName = target.getName();
                        if (!sourceModule.isPackagedAsSingleModule(targetModule)) {
                            String relativeUri = null;
                            if( sourceModule == app ) {
                                // Now that dependencies can be defined within application.xml
                                // it's possible for source module to be the Application object.
                                // In this case, just use the target module uri as the relative
                                // uri.
                                relativeUri = targetModule.getModuleDescriptor().getArchiveUri();
                            } else {
                                // Since there are at least two modules, we
                                // must be within an application.
                                relativeUri = getApplication().getRelativeUri(sourceModule, targetModule);
                            }
                            ejbLinkName = relativeUri + "#" + ejbLinkName;
                        }

                        ejbRef.setLinkName(ejbLinkName);

                    } else {
                        String msg = localStrings.getLocalString("enterprise.deployment.util.multiple_ejbs_with_interface", "Cannot resolve reference {0} because there are {1} ejbs in the application with interface {2}.", new Object[] {ejbRef, numMatches, interfaceToMatch});
                        throw new IllegalArgumentException(msg);
                    }
                }                          
            } 
        }

        // now all cases fall back here, we need to resolve through the link-name    
        if (ejbRef.getLinkName()==null) {


            // if no link name if present, and this is a local ref, this is an
            // error (unless there is a lookup string) because we must resolve all
            // local refs within the app and we cannot resolve it 
            if (ejbRef.isLocal()) {
                if( ejbRef.hasLookupName() ) {
                    return;
                }
                DOLUtils.getDefaultLogger().severe("Cannot resolve reference " + ejbRef);
                throw new RuntimeException("Cannot resolve reference " + ejbRef);
            } else {
                // this is a remote interface, jndi will eventually contain the referenced 
                // ejb ref, apply default jndi name if there is none
                if (!ejbRef.hasJndiName() && !ejbRef.hasLookupName()) {
                    String jndiName = getDefaultEjbJndiName(
                        ejbRef.isEJB30ClientView() ?
                        ejbRef.getEjbInterface() : ejbRef.getEjbHomeInterface());
                    ejbRef.setJndiName(jndiName);
                    DOLUtils.getDefaultLogger().fine("Applying default to ejb reference: " + ejbRef);
                }

                return;
            }                          
        }        
        
        // Beginning of ejb-link resolution
        
        // save anticipated types for checking if interfaces are compatible
        String homeClassName = ejbRef.getEjbHomeInterface();
        String intfClassName = ejbRef.getEjbInterface();

        // save anticipated type for checking if bean type is compatible
        String type = ejbRef.getType();
        
        EjbDescriptor ejbReferee=null;
            
        String linkName = ejbRef.getLinkName();
        int ind = linkName.lastIndexOf('#');
        if ( ind != -1 ) {
            // link has a relative path from referring EJB JAR,
            // of form "../products/product.jar#ProductEJB"
            String ejbName = linkName.substring(ind+1);
            String jarPath = linkName.substring(0, ind);            
            BundleDescriptor referringJar = ejbRef.getReferringBundleDescriptor();
            if (referringJar==null) {
                ejbRef.setReferringBundleDescriptor(getBundleDescriptor());
                referringJar = getBundleDescriptor();
            }           
            
            if (getApplication()!=null) {
                BundleDescriptor refereeJar = null;
                if( referringJar instanceof Application ) {
                    refereeJar = ((Application)referringJar).getModuleByUri(jarPath);
                } else {
                 refereeJar = 
                    getApplication().getRelativeBundle(referringJar, jarPath);
                }
                if( (refereeJar != null) &&
                    refereeJar instanceof EjbBundleDescriptor ) {
                    // this will throw an exception if ejb is not found
                    ejbReferee = 
                       ((EjbBundleDescriptor)refereeJar).getEjbByName(ejbName);
                }
            }
        }
        else {

            // Handle an unqualified ejb-link, which is just an ejb-name.

            // If we're in an application and currently processing an
            // ejb-reference defined within an ejb-jar, first check
            // the current ejb-jar for an ejb-name match.  From a spec
            // perspective, the deployer can't depend on this behavior,
            // but it's still better to have deterministic results.  In
            // addition, in the case of automatic-linking, the fully-qualified
            // "#" ejb-link syntax is not used when the ejb reference and
            // target ejb are within the same ejb-jar.  Checking the
            // ejb-jar first will ensure the correct linking behavior for
            // that case.
            if ( (getApplication() != null) && (ejbBundleDescriptor != null)
                 && ejbBundleDescriptor.hasEjbByName(linkName) ) {

                ejbReferee = ejbBundleDescriptor.getEjbByName(linkName);

            } else if ( (getApplication() != null)  && 
                        getApplication().hasEjbByName(linkName)) {
                
                ejbReferee = 
                    getApplication().getEjbByName(ejbRef.getLinkName());
                    
            } else if (ejb!=null) {
                try {
                    ejbReferee = ejb.getEjbBundleDescriptor().getEjbByName(ejbRef.getLinkName());
                } catch (IllegalArgumentException e) {
                    // this may happen when we have no application and the ejb ref
                    // cannot be resolved to a ejb in the bundle. The ref will
                    // probably be resolved when the application is assembled.
                    DOLUtils.getDefaultLogger().warning("Unresolved <ejb-link>: "+linkName);
                    return;
                }
                    
            }
        } 

        if (ejbReferee==null)
        {  

            // we could not resolve through the ejb-link. if this is a local ref, this 
            // is an error, if this is a remote ref, this should be also an error at 
            // runtime but maybe the jndi name will be specified by deployer so 
            // a warning should suffice
            if (ejbRef.isLocal()) {
                DOLUtils.getDefaultLogger().severe("Unresolved <ejb-link>: "+linkName);
                throw new RuntimeException("Error: Unresolved <ejb-link>: "+linkName);
            } else {
                final ArchiveType moduleType = ejbRef.getReferringBundleDescriptor().getModuleType();
                if(moduleType != null && moduleType.equals(org.glassfish.deployment.common.DeploymentUtils.carType())) {
                    // Because no annotation processing is done within ACC runtime, this case typically
                    // arises for remote @EJB annotations, so don't log it as warning.
                    DOLUtils.getDefaultLogger().fine("Unresolved <ejb-link>: "+linkName);
                } else {
                    DOLUtils.getDefaultLogger().warning("Unresolved <ejb-link>: "+linkName);
                }
                return;
            }
        } else {

            if( ejbRef.isEJB30ClientView() ) {

                BundleDescriptor referringBundle = 
                    ejbRef.getReferringBundleDescriptor();

                // If we can verify that the current ejb 3.0 reference is defined
                // in any Application Client module or in a stand-alone web module
                // it must be remote business.
                if( ( (referringBundle == null) && (ejbBundleDescriptor == null) )
                    ||
                    (referringBundle.getModuleType() == org.glassfish.deployment.common.DeploymentUtils.carType())
                    ||
                    ( (getApplication() == null) &&
                      (referringBundle.getModuleType() != null && referringBundle.getModuleType().equals(org.glassfish.deployment.common.DeploymentUtils.warType())) ) ) {

                    ejbRef.setLocal(false);

                    // Double-check that target has a remote business interface of this
                    // type.  This will handle the common error case that the target 
                    // EJB has intended to support a remote business interface but
                    // has not used @Remote to specify it, in which case
                    // the interface was assigned the default of local business.

                    if( !ejbReferee.getRemoteBusinessClassNames().contains
                        (intfClassName) ) {
                        String msg = "Target ejb " + ejbReferee.getName() + " for " +
                            " remote ejb 3.0 reference " + ejbRef.getName() + 
                            " does not expose a remote business interface of type " +
                            intfClassName;
                        throw new RuntimeException(msg);
                    }

                } else if(ejbReferee.getLocalBusinessClassNames().
                         contains(intfClassName)) {

                    ejbRef.setLocal(true);

                } else if(ejbReferee.getRemoteBusinessClassNames().
                          contains(intfClassName)) {

                    ejbRef.setLocal(false);

                } else {
                    if (ejbReferee.isLocalBean()) {
                        ejbRef.setLocal(true);    
                    } else {
                        String msg = "Warning : Unable to determine local " +
                            " business vs. remote business designation for " +
                            " EJB 3.0 ref " + ejbRef;
                        throw new RuntimeException(msg);
                    }
                }
            }

            ejbRef.setEjbDescriptor(ejbReferee);
        }
        
        // if we are here, we must have resolved the reference
        
	if(DOLUtils.getDefaultLogger().isLoggable(Level.FINE)) {
            DOLUtils.getDefaultLogger().fine("Done Visiting " + ejb.getName() + " reference " + ejbRef);
	}

        // check that declared types are compatible with expected values
        // if there is a target ejb descriptor available
        if( ejbReferee != null ) {

            if( ejbRef.isEJB30ClientView() ) {

                Set<String> targetBusinessIntfs = ejbRef.isLocal() ?
                    ejbReferee.getLocalBusinessClassNames() :
                    ejbReferee.getRemoteBusinessClassNames();

                EjbDescriptor ejbDesc = ejbRef.getEjbDescriptor();

                // If it's neither a business interface nor a no-interface view
                if( !targetBusinessIntfs.contains(intfClassName) &&
                    (   ejbDesc.isLocalBean() &&
                        !(intfClassName.equals(ejbReferee.getEjbClassName()))) ) {


                    DOLUtils.getDefaultLogger().log(Level.WARNING,
                           "enterprise.deployment.backend.ejbRefTypeMismatch",
                           new Object[] {ejbRef.getName() , intfClassName, 
                           ejbReferee.getName(), ( ejbRef.isLocal() ?
                           "Local Business" : "Remote Business"),
                                             targetBusinessIntfs.toString()});


                    // We can only figure out what the correct type should be
                    // if there is only 1 target remote/local business intf.
                    if( targetBusinessIntfs.size() == 1 ) {
                        Iterator iter = targetBusinessIntfs.iterator();
                        ejbRef.setEjbInterface((String)iter.next());
                    }
                }

            } else {

                String targetHome = ejbRef.isLocal() ? 
                    ejbReferee.getLocalHomeClassName() : 
                    ejbReferee.getHomeClassName();

                if( !homeClassName.equals(targetHome) ) {

                    DOLUtils.getDefaultLogger().log(Level.WARNING, 
                       "enterprise.deployment.backend.ejbRefTypeMismatch",
                       new Object[] {ejbRef.getName() , homeClassName, 
                       ejbReferee.getName(), ( ejbRef.isLocal() ? 
                       "Local Home" : "Remote Home"), targetHome});

                    if( targetHome != null ) {
                        ejbRef.setEjbHomeInterface(targetHome);
                    }
                }

                String targetComponentIntf = ejbRef.isLocal() ?
                    ejbReferee.getLocalClassName() : 
                    ejbReferee.getRemoteClassName();

                // In some cases for 2.x style @EJBs that point to Entity beans
                // the interface class cannot be derived, so only do the
                // check if the intf is known.
                if( (intfClassName != null) &&
                    !intfClassName.equals(targetComponentIntf) ) {

                    DOLUtils.getDefaultLogger().log(Level.WARNING, 
                       "enterprise.deployment.backend.ejbRefTypeMismatch",
                       new Object[] {ejbRef.getName() , intfClassName, 
                       ejbReferee.getName(), ( ejbRef.isLocal() ? 
                       "Local" : "Remote"), targetComponentIntf});

                    if( targetComponentIntf != null ) {
                        ejbRef.setEjbInterface(targetComponentIntf);
                    }
                }
            }

            // set jndi name in ejb ref 
            ejbRef.setJndiName(ejbReferee.getJndiName());
        }

        if (!type.equals(ejbRef.getType())) {
            // if they don't match 
            // print a warning and reset the type in ejb ref
            DOLUtils.getDefaultLogger().log(Level.WARNING, "enterprise.deployment.backend.invalidDescriptorMappingFailure",
            new Object[] {ejbRef.getName() , type});

            ejbRef.setType(ejbRef.getType());

        }          
    }        

    /**
     * Returns a map of interface name -> EjbIntfInfo based on all the ejbs
     * within the application or stand-alone module.  Only RemoteHome, 
     * RemoteBusiness, LocalHome, and LocalBusiness are eligible for map. 
     */
    private Map<String, EjbIntfInfo> getEjbIntfMap() {

        Collection ejbs = getEjbDescriptors();

        Map<String, EjbIntfInfo> intfInfoMap=new HashMap<String, EjbIntfInfo>();

        for(Iterator iter = ejbs.iterator(); iter.hasNext();) {
            EjbDescriptor next = (EjbDescriptor) iter.next();

            if( next.isRemoteInterfacesSupported() ) {
                addIntfInfo(intfInfoMap, next.getHomeClassName(), 
                            EjbIntfType.REMOTE_HOME, next);
            }

            if( next.isRemoteBusinessInterfacesSupported() ) {
                for(String nextIntf : next.getRemoteBusinessClassNames()) {
                    addIntfInfo(intfInfoMap, nextIntf, 
                                EjbIntfType.REMOTE_BUSINESS, next);
                }
            }

            if( next.isLocalInterfacesSupported() ) {
                addIntfInfo(intfInfoMap, next.getLocalHomeClassName(), 
                            EjbIntfType.LOCAL_HOME, next);
            }

            if( next.isLocalBusinessInterfacesSupported() ) {
                for(String nextIntf : next.getLocalBusinessClassNames()) {
                    addIntfInfo(intfInfoMap, nextIntf, 
                                EjbIntfType.LOCAL_BUSINESS, next);
                }
            }

            if (next.isLocalBean()) {
                addIntfInfo(intfInfoMap, next.getEjbClassName(),
                                EjbIntfType.NO_INTF_LOCAL_BUSINESS, next);
            }

        }

        return intfInfoMap;
    }

    private void addIntfInfo(Map<String, EjbIntfInfo> intfInfoMap,
                             String intf, EjbIntfType intfType,
                             EjbDescriptor ejbDesc) {

        EjbIntfInfo intfInfo = intfInfoMap.get(intf);
        if( intfInfo == null ) {
            EjbIntfInfo newInfo = new EjbIntfInfo();
            newInfo.ejbs = new HashSet<EjbDescriptor>();
            newInfo.ejbs.add(ejbDesc);
            newInfo.intfType = intfType;
            intfInfoMap.put(intf, newInfo);
        } else {
            intfInfo.ejbs.add(ejbDesc);
            // Since there's more than one match, reset intf type.
            intfInfo.intfType = EjbIntfType.NONE;
        }

    }

    /**
     * @return a vector of EjbDescriptor for this bundle
     */
    protected Collection getEjbDescriptors() {
        if (getApplication() != null) {
            return getApplication().getEjbDescriptors();
        } else if (ejbBundleDescriptor!=null) {
            return ejbBundleDescriptor.getEjbs();
        } else {
            return new HashSet();
        }
    } 
    
    /**
     * @return the Application object if any
     */
    protected Application getApplication() {
        return null;
    }
     
    /**
     * @return the bundleDescriptor we are validating
     */
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
                     intfName = (String)classNames.iterator().next();
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

    /**
     * @param intfName
     * @return default jndi name for a given interface name
     */
    //XXX this is first implementation. It does not handle two ejb with same
    //    interface in different jar
    private String getDefaultEjbJndiName(String intfName) {
        return intfName;
    }

    private enum EjbIntfType {
        NONE,
        REMOTE_HOME,
        REMOTE_BUSINESS,
        LOCAL_HOME,
        LOCAL_BUSINESS,
        NO_INTF_LOCAL_BUSINESS
    }

    private static class EjbIntfInfo {

        Set<EjbDescriptor> ejbs;

        // Only set when there is one ejb in the set. 
        // Otherwise, value = NONE
        EjbIntfType intfType;
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
        return new MethodDescriptor(m, MethodDescriptor.EJB_BEAN);
    }
}
