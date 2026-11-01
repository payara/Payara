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
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
// Portions Copyright [2016-2026] Payara Foundation and/or its affiliates

package org.glassfish.ejb.deployment.annotation.handlers;

import com.sun.enterprise.deployment.MethodDescriptor;
import com.sun.enterprise.deployment.annotation.context.EjbBundleContext;
import com.sun.enterprise.deployment.annotation.context.EjbContext;
import com.sun.enterprise.deployment.annotation.context.EjbsContext;
import com.sun.enterprise.deployment.annotation.handlers.AbstractHandler;
import com.sun.enterprise.util.LocalStringManagerImpl;
import fish.payara.nucleus.hotdeploy.ApplicationState;
import jakarta.ejb.EJBHome;
import jakarta.ejb.EJBLocalHome;
import jakarta.ejb.Local;
import jakarta.ejb.LocalBean;
import jakarta.ejb.LocalHome;
import jakarta.ejb.Remote;
import jakarta.ejb.RemoteHome;
import jakarta.ejb.TimedObject;
import jakarta.ejb.Timeout;
import org.glassfish.apf.AnnotatedElementHandler;
import org.glassfish.apf.AnnotationInfo;
import org.glassfish.apf.AnnotationProcessorException;
import org.glassfish.apf.AnnotationValues;
import org.glassfish.apf.HandlerProcessingResult;
import org.glassfish.apf.context.AnnotationContext;
import org.glassfish.config.support.TranslatedConfigView;
import org.glassfish.ejb.deployment.descriptor.DummyEjbDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbBundleDescriptorImpl;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.glassfish.internal.deployment.ClassHierarchy;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

import java.io.Externalizable;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

/**
 * This is an abstract class for EJB annotation handler.
 * Concrete subclass handlers need to implements the following methods:
 *     public Class&lt;? extends Annotation&gt; getAnnotationType();
 *     protected String getAnnotatedName(AnnotationInfo ainfo);
 *     protected boolean isValidEjbDescriptor(EjbDescriptor ejbDesc, AnnotationInfo ainfo);
 *     protected EjbDescriptor createEjbDescriptor(String elementName,
 *         AnnotationInfo ainfo) throws AnnotationProcessorException;
 *     protected HandlerProcessingResult setEjbDescriptorInfo(
 *         EjbDescriptor ejbDesc, AnnotationInfo ainfo)
 *         throws AnnotationProcessorException;
 *
 * @author Shing Wai Chan
 */
public abstract class AbstractEjbHandler extends AbstractHandler {

    private static final DotName WEB_SERVICE = DotName.createSimple("jakarta.jws.WebService");
    private static final DotName TIMER = DotName.createSimple("jakarta.ejb.Timer");
    private static final DotName SERIALIZABLE = DotName.createSimple(Serializable.class);
    private static final DotName EXTERNALIZABLE = DotName.createSimple(Externalizable.class);

    protected final static LocalStringManagerImpl localStrings =
            new LocalStringManagerImpl(AbstractEjbHandler.class);

    public AbstractEjbHandler() {
    }

    /**
     * Return the name attribute of given annotation.
     * @param ainfo the annotation
     * @return name
     */
    protected abstract String getAnnotatedName(AnnotationInfo ainfo);

    /**
     * check if the given EjbDescriptor matches the given Annotation.
     * @param ejbDesc
     * @param ainfo
     * @return boolean check for validity of EjbDescriptor
     */
    protected abstract boolean isValidEjbDescriptor(EjbDescriptor ejbDesc, AnnotationInfo ainfo);

    /**
     * Create a new EjbDescriptor for a given elementName and AnnotationInfo.
     * @param elementName
     * @param ainfo
     * @return a new EjbDescriptor
     */
    protected abstract EjbDescriptor createEjbDescriptor(String elementName,
            AnnotationInfo ainfo) throws AnnotationProcessorException;

    /**
     * Set Annotation information to Descriptor.
     * This method will also be invoked for an existing descriptor with
     * annotation as user may not specific a complete xml.
     * @param ejbDesc
     * @param ainfo
     * @return HandlerProcessingResult
     */
    protected abstract HandlerProcessingResult setEjbDescriptorInfo(
            EjbDescriptor ejbDesc, AnnotationInfo ainfo)
            throws AnnotationProcessorException;

    /**
     * Process a particular annotation which type is the same as the one
     * returned by @see getAnnotationType().All information pertinent to the
     * annotation and its context is encapsulated in the passed AnnotationInfo
     * instance.This is a method in interface AnnotationHandler.
     *
     * @param ainfo the annotation information
     * @return
     * @throws org.glassfish.apf.AnnotationProcessorException
     */
    @Override
    public HandlerProcessingResult processAnnotation(AnnotationInfo ainfo)
            throws AnnotationProcessorException {

        ApplicationState state = ainfo
                .getProcessingContext()
                .getArchive()
                .getExtraData(ApplicationState.class);

        ClassInfo ejbClass = ainfo.getAnnotatedElement().asClass();
        AnnotationInstance annotation = ainfo.getAnnotation();
        if (logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER, "@ process ejb annotation {0} in {1}", new Object[]{annotation, ejbClass});
        }
        AnnotatedElementHandler aeHandler =
                ainfo.getProcessingContext().getHandler();
        if (aeHandler != null && aeHandler instanceof EjbContext) {
            EjbContext context = (EjbContext)aeHandler;
            EjbDescriptor desc = (EjbDescriptor) context.getDescriptor();
            if (isValidEjbDescriptor(desc, ainfo)) {
                return getDefaultProcessedResult();
            } else {
                log(Level.SEVERE, ainfo,
                    localStrings.getLocalString(
                    "enterprise.deployment.annotation.handlers.notcompsuperclass",
                    "The annotation symbol defined in super-class is not compatible with {0} ejb {1}.",
                    new Object[] { desc.getType(), desc.getName() }));
                return getDefaultFailedResult();
            }
        } else if (aeHandler == null || !(aeHandler instanceof EjbBundleContext)) {
            return getInvalidAnnotatedElementHandlerResult(
                ainfo.getProcessingContext().getHandler(), ainfo);
        }

        EjbBundleContext ctx = (EjbBundleContext)aeHandler;

        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "My context is {0}", ctx);
        }

        String elementName = getAnnotatedName(ainfo);
        if (elementName.length() == 0) {
            elementName = ClassHierarchy.simpleName(ejbClass);
        }
        else {
            elementName = TranslatedConfigView.expandValue(elementName);
        }
        String ejbClassName = ejbClass.name().toString();

        EjbBundleDescriptorImpl currentBundle = (EjbBundleDescriptorImpl) ctx.getDescriptor();
        EjbDescriptor ejbDesc = null;
        try {
            ejbDesc = currentBundle.getEjbByName(elementName);
        } catch(IllegalArgumentException ex) {
            //getEjbByName throws IllegalArgumentException when no ejb is found
        }

        if(state != null && ejbDesc != null) {
            currentBundle.removeEjb(ejbDesc);
            ejbDesc = null;
        }

        if (ejbDesc != null && !(ejbDesc instanceof DummyEjbDescriptor) ) {
            // element has already been defined in the standard DDs,
            // overriding rules applies
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "Overriding rules apply for {0}", ejbClassName);
            }

            // don't allow ejb-jar.xml overwrite ejb type
            if (!isValidEjbDescriptor(ejbDesc, ainfo)) {
                // this is an error
                log(Level.SEVERE, ainfo,
                    localStrings.getLocalString(
                    "enterprise.deployment.annotation.handlers.wrongejbtype",
                    "Wrong annotation symbol for ejb {0}",
                    new Object[] { ejbDesc }));
                return getDefaultFailedResult();
            }

            // <ejb-class> is optional if a component-defining
            // annotation is used.  If present, <ejb-class> element
            // must match the class on which the component defining annotation
            // appears.
            String descriptorEjbClass = ejbDesc.getEjbClassName();
            if( descriptorEjbClass == null ) {
                ejbDesc.setEjbClassName(ejbClassName);
                ejbDesc.applyDefaultClassToLifecycleMethods();
            } else if( !descriptorEjbClass.equals(ejbClassName) ) {
                log(Level.SEVERE, ainfo,
                    localStrings.getLocalString(
                    "enterprise.deployment.annotation.handlers.ejbclsmismatch",
                    "",
                    new Object[] { descriptorEjbClass, elementName, ejbClassName }));
                return getDefaultFailedResult();
            }


        } else {
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "Creating a new descriptor for {0}", ejbClassName);
            }

            EjbDescriptor dummyEjbDesc = ejbDesc;

            ejbDesc = createEjbDescriptor(elementName, ainfo);

            // create the actual ejb descriptor using annotation info and
            // the information from dummy ejb descriptor if applicable
            if (dummyEjbDesc != null) {
                currentBundle.removeEjb(dummyEjbDesc);
                ejbDesc.addEjbDescriptor(dummyEjbDesc);
                // reset ejbClassName on ejbDesc
                ejbDesc.setEjbClassName(ejbClassName);
            }

            // add the actual ejb descriptor to the ejb bundle
            currentBundle.addEjb(ejbDesc);

            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "New {0} bean {1}", new Object[]{getAnnotationType().getName(), elementName});
            }
        }

        // We need to include all ejbs of the same name in the annotation processing context
        // in order to handle the case that a bean class has both a component-defining
        // annotation and there are other ejb-jar.xml-defined beans with the same bean class.

        IndexView index = ainfo.getIndex();
        EjbDescriptor[] ejbDescs = currentBundle.getEjbByClassName(ejbClassName);
        HandlerProcessingResult procResult = null;
        for(EjbDescriptor next : ejbDescs) {
            procResult = setEjbDescriptorInfo(next, ainfo);
            doTimedObjectProcessing(index, ejbClass, next);
        }

        AnnotationContext annContext;
        if( ejbDescs.length == 1 ) {
            annContext = new EjbContext(ejbDesc, ejbClass, index);
        } else {
            annContext = new EjbsContext(ejbDescs, ejbClass, index);
        }

        // we push the new context on the stack...
        ctx.getProcessingContext().pushHandler(annContext);

        return procResult;
    }

    /**
     * Process TimedObject and @Timeout annotation.  It's better to do it
     * when processing the initial bean type since Timeout method is a
     * business method that should be included in any tx processing defaulting
     * that takes place.
     */
    private void doTimedObjectProcessing(IndexView index, ClassInfo ejbClass, EjbDescriptor ejbDesc) {

        // Timeout methods can be declared on the bean class or any
        // super-class and can be public, protected, private, or
        // package access.  There can be at most one timeout method for
        // the entire bean class hierarchy, so we start from the bean
        // class and go up, stopping when we find the first one.

        MethodDescriptor timeoutMethodDesc = null;
        for (ClassInfo nextClass : ClassHierarchy.hierarchy(index, ejbClass)) {
            for (MethodInfo m : nextClass.methods()) {
                if (m.hasDeclaredAnnotation(DotName.createSimple(Timeout.class))) {
                    timeoutMethodDesc = new MethodDescriptor(m, MethodDescriptor.TIMER_METHOD);
                    break;
                }
            }
            if (timeoutMethodDesc != null) {
                break;
            }
        }

        if( (timeoutMethodDesc == null) && ClassHierarchy.isSubtype(index, ejbClass, TimedObject.class) ) {
            // If the class implements the TimedObject interface, it must
            // be ejbTimeout.
            timeoutMethodDesc = new MethodDescriptor
                ("ejbTimeout", "@Timeout method",
                 new String[] { TIMER.toString() },
                 MethodDescriptor.TIMER_METHOD);
        }

        if( timeoutMethodDesc != null ) {
            ejbDesc.setEjbTimeoutMethod(timeoutMethodDesc);
        }

    }

    /**
     * MessageDriven bean does not need to invoke this API.
     * @param ejbDesc
     * @param ainfo  for error handling
     * @return HandlerProcessingResult
     */
    protected HandlerProcessingResult setBusinessAndHomeInterfaces(
            EjbDescriptor ejbDesc, AnnotationInfo ainfo)
            throws AnnotationProcessorException {

        Set<DotName> localBusIntfs = new LinkedHashSet<>();
        Set<DotName> remoteBusIntfs = new LinkedHashSet<>();

        Set<DotName> clientInterfaces = new LinkedHashSet<>();

        IndexView index = ainfo.getIndex();
        ClassInfo ejbClass = ainfo.getAnnotatedElement().asClass();

        // First check for annotations specifying remote/local business intfs.
        // We analyze them here because they are needed during the
        // implements clause processing for beans that specify
        // @Stateless/@Stateful.  In addition, they should *not* be processed
        // if there is no @Stateful/@Stateless annotation.

        AnnotationInstance remoteBusAnn = ClassHierarchy.annotation(index, ejbClass, Remote.class);
        boolean emptyRemoteBusAnn = false;
        if( remoteBusAnn != null ) {
            for(Type next : AnnotationValues.classes(index, remoteBusAnn, "value")) {
                DotName nextName = next.name();
                if (hasAnnotation(index, nextName, Local.class)) {
                    throw invalidBusinessInterface(nextName);
                }
                clientInterfaces.add(nextName);
                remoteBusIntfs.add(nextName);
            }
            emptyRemoteBusAnn = remoteBusIntfs.isEmpty();
        }

        AnnotationInstance localBusAnn = ClassHierarchy.annotation(index, ejbClass, Local.class);
        if( localBusAnn != null ) {
            for(Type next : AnnotationValues.classes(index, localBusAnn, "value")) {
                DotName nextName = next.name();
                if (hasAnnotation(index, nextName, Remote.class)) {
                    throw invalidBusinessInterface(nextName);
                }
                clientInterfaces.add(nextName);
                localBusIntfs.add(nextName);
            }
        }

        List<DotName> imlementingInterfaces = new ArrayList<>();
        List<DotName> implementedDesignatedInterfaces = new ArrayList<>();
        for(DotName next : ejbClass.interfaceNames()) {
            if( !excludedFromImplementsClause(next) ) {
                if( hasAnnotation(index, next, Local.class) || hasAnnotation(index, next, Remote.class) ) {
                    implementedDesignatedInterfaces.add(next);
                }
                imlementingInterfaces.add(next);
            }
        }

        if( ClassHierarchy.annotation(index, ejbClass, LocalBean.class) != null ) {
            ejbDesc.setLocalBean(true);
        }

        // total number of local/remote business interfaces declared
        // outside of the implements clause plus implemented designated interfaces
        int designatedInterfaceCount =
            remoteBusIntfs.size() + localBusIntfs.size() +
            ejbDesc.getRemoteBusinessClassNames().size() +
            ejbDesc.getLocalBusinessClassNames().size() +
            implementedDesignatedInterfaces.size();

        for(DotName next : imlementingInterfaces) {
            String nextIntfName = next.toString();

            if( remoteBusIntfs.contains(next)
                ||
                localBusIntfs.contains(next)
                ||
                ejbDesc.getRemoteBusinessClassNames().contains(nextIntfName)
                ||
                ejbDesc.getLocalBusinessClassNames().contains(nextIntfName)){

                // Interface has already been identified as a Remote/Local
                // business interface, so ignore.

            } else if( hasAnnotation(index, next, Local.class) ) {

                clientInterfaces.add(next);
                localBusIntfs.add(next);

            } else if( hasAnnotation(index, next, Remote.class) ) {

                clientInterfaces.add(next);
                remoteBusIntfs.add(next);

            } else {

                if( (designatedInterfaceCount == 0) &&
                    (!ejbDesc.isLocalBean()) ) {

                    // If there's an empty @Remote annotation on the class,
                    // it's treated as a remote business interface. Otherwise,
                    // it's treated as a local business interface.
                    if( emptyRemoteBusAnn ) {
                        remoteBusIntfs.add(next);
                    } else {
                        localBusIntfs.add(next);
                    }
                    clientInterfaces.add(next);

                } else {

                    // Since the component has at least one other business
                    // interface, each implements clause interface that cannot
                    // be identified as business interface via the deployment
                    // descriptor or a @Remote/@Local annotation is ignored.

                }
            }
        }

        for (DotName next : clientInterfaces) {
            if (remoteBusIntfs.contains(next) && localBusIntfs.contains(next)) {
                throw invalidBusinessInterface(next);
            }
        }

        for(DotName next : localBusIntfs) {
            ejbDesc.addLocalBusinessClassName(next.toString());
        }

        for(DotName next : remoteBusIntfs) {
            ejbDesc.addRemoteBusinessClassName(next.toString());
        }


        // Do Adapted @Home / Adapted @LocalHome processing here too since
        // they are logically part of the structural @Stateless/@Stateful info.
        AnnotationInstance remoteHomeAnn = ClassHierarchy.annotation(index, ejbClass, RemoteHome.class);

        if( remoteHomeAnn != null ) {
            DotName remoteHome = AnnotationValues.value(index, remoteHomeAnn, "value").asClass().name();
            String remoteIntf = getComponentIntfFromHome(index, remoteHome);
            if( ClassHierarchy.isSubtype(index, remoteHome, EJBHome.class) &&
                (remoteIntf != null) ) {

                clientInterfaces.add(remoteHome);
                ejbDesc.setHomeClassName(remoteHome.toString());
                ejbDesc.setRemoteClassName(remoteIntf);

            } else {
                log(Level.SEVERE, ainfo,
                    localStrings.getLocalString(
                    "enterprise.deployment.annotation.handlers.invalidremotehome",
                    "Encountered invalid @RemoteHome interface {0}.",
                    new Object[] { remoteHome }));
                return getDefaultFailedResult();
            }
        }

        AnnotationInstance localHomeAnn = ClassHierarchy.annotation(index, ejbClass, LocalHome.class);

        if( localHomeAnn != null ) {
            DotName localHome = AnnotationValues.value(index, localHomeAnn, "value").asClass().name();
            String localIntf = getComponentIntfFromHome(index, localHome);
            if( ClassHierarchy.isSubtype(index, localHome, EJBLocalHome.class) &&
                (localIntf != null) ) {

                clientInterfaces.add(localHome);
                ejbDesc.setLocalHomeClassName(localHome.toString());
                ejbDesc.setLocalClassName(localIntf);

            } else {
                log(Level.SEVERE, ainfo,
                    localStrings.getLocalString(
                    "enterprise.deployment.annotation.handlers.invalidlocalhome",
                    "Encountered invalid @LocalHome interface {0}.",
                    new Object[] { localHome }));
                return getDefaultFailedResult();
            }
        }

        if( (!ejbDesc.isLocalBean()) &&
            (clientInterfaces.size() == 0) &&
            !ejbDesc.hasWebServiceEndpointInterface() &&
            ClassHierarchy.annotation(index, ejbClass, WEB_SERVICE) == null ) {
            ejbDesc.setLocalBean(true);
        }

        //If this is a no-Interface local EJB, set all classes for this bean
        if (ejbDesc.isLocalBean()) {
            addNoInterfaceLocalBeanClasses(index, ejbDesc, ejbClass);
        }


        return getDefaultProcessedResult();
    }

    private AnnotationProcessorException invalidBusinessInterface(DotName intf) {
        AnnotationProcessorException fatalException =
                new AnnotationProcessorException(localStrings.getLocalString(
                        "enterprise.deployment.annotation.handlers.invalidbusinessinterface",
                        "The interface {0} cannot be both a local and a remote business interface.",
                        new Object[]{intf.toString()}));
        fatalException.setFatal(true);
        return fatalException;
    }

    private void addNoInterfaceLocalBeanClasses(IndexView index, EjbDescriptor ejbDesc, ClassInfo ejbClass) {
        //The session bean's no-interface view can be accessed via the bean class
        //and all its super classes
        for (ClassInfo nextClass : ClassHierarchy.hierarchy(index, ejbClass)) {
            ejbDesc.addNoInterfaceLocalBeanClass(nextClass.name().toString());
        }
    }

    /**
     * @return the name of the component interface the home interface's create methods return, or
     * null if the home interface has none or cannot be resolved
     */
    private String getComponentIntfFromHome(IndexView index, DotName homeIntf) {
        ClassInfo home = index.getClassByName(homeIntf);
        if (home == null) {
            return null;
        }
        for (MethodInfo m : publicMethods(index, home)) {
            if (m.name().startsWith("create")) {
                return ClassHierarchy.rawName(m.returnType());
            }
        }
        return null;
    }

    /**
     * The equivalent of {@code Class.getMethods()} for an interface: its methods and those of its
     * super-interfaces.
     */
    static List<MethodInfo> publicMethods(IndexView index, ClassInfo intf) {
        List<MethodInfo> methods = new ArrayList<>(intf.methods());
        for (DotName superIntf : ClassHierarchy.allInterfaces(index, intf)) {
            ClassInfo superInfo = index.getClassByName(superIntf);
            if (superInfo != null) {
                methods.addAll(superInfo.methods());
            }
        }
        return methods;
    }

    /**
     * @return whether the (interface) type, resolved in the index, declares the annotation
     */
    static boolean hasAnnotation(IndexView index, DotName type, Class<?> annotationType) {
        ClassInfo classInfo = index.getClassByName(type);
        return classInfo != null && ClassHierarchy.annotation(index, classInfo, annotationType) != null;
    }

    protected boolean excludedFromImplementsClause(DotName intf) {
        return intf.equals(SERIALIZABLE) || intf.equals(EXTERNALIZABLE)
                || "jakarta.ejb".equals(intf.packagePrefix());
    }

    protected void doDescriptionProcessing(String description, EjbDescriptor ejbDescriptor) {
        // Since there are multiple descriptions allowed in the deployment
        // descriptor, there are no overriding issues here.  If the
        // component-defining annotation contains a description, it will
        // always be added to the list of descriptions for the bean.
        if( (description != null) && !description.equals("") ) {
            ejbDescriptor.setDescription(description);
        }

    }

    protected void doMappedNameProcessing(String mappedName, EjbDescriptor ejbDesc) {

        // Set mappedName() if a value has been given in the annotation and
        // it hasn't already been set on the descriptor via the .xml.
        if( ejbDesc.getMappedName().equals("") ) {
            if( !mappedName.equals("") ) {
                ejbDesc.setMappedName(mappedName);
            }
        }


    }

}
