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
// Portions Copyright [2026] Payara Foundation and/or its affiliates
package org.glassfish.ejb.deployment.annotation.handlers;

import com.sun.enterprise.deployment.EjbReferenceDescriptor;
import com.sun.enterprise.deployment.InjectionTarget;
import com.sun.enterprise.deployment.MetadataSource;
import com.sun.enterprise.deployment.annotation.context.ResourceContainerContext;
import com.sun.enterprise.deployment.annotation.handlers.AbstractResourceHandler;
import com.sun.enterprise.util.LocalStringManagerImpl;
import jakarta.ejb.EJB;
import jakarta.ejb.EJBHome;
import jakarta.ejb.EJBLocalHome;
import jakarta.ejb.Local;
import org.glassfish.apf.AnnotationHandlerFor;
import org.glassfish.apf.AnnotationInfo;
import org.glassfish.apf.AnnotationProcessorException;
import org.glassfish.apf.AnnotationValues;
import org.glassfish.apf.HandlerProcessingResult;
import org.glassfish.ejb.deployment.descriptor.EjbSessionDescriptor;
import org.glassfish.internal.deployment.ClassHierarchy;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jvnet.hk2.annotations.Service;

import java.lang.annotation.ElementType;

import static com.sun.enterprise.util.StringUtils.ok;

/**
 * This handler is responsible for handling the jakarta.ejb.EJB
 *
 * @author Shing Wai Chan
 */
@Service
@AnnotationHandlerFor(EJB.class)
public class EJBHandler extends AbstractResourceHandler {

    protected final static LocalStringManagerImpl localStrings =
            new LocalStringManagerImpl(EJBHandler.class);

    public EJBHandler() {
    }

    /**
     * Process a particular annotation which type is the same as the
     * one returned by @see getAnnotationType(). All information
     * pertinent to the annotation and its context is encapsulated
     * in the passed AnnotationInfo instance.
     * This is a method in interface AnnotationHandler.
     *
     * @param ainfo the annotation information
     * @param rcContexts an array of ResourceContainerContext
     * @return HandlerProcessingResult
     */
    @Override
    protected HandlerProcessingResult processAnnotation(AnnotationInfo ainfo, ResourceContainerContext[] rcContexts)
            throws AnnotationProcessorException {
        return processEJB(ainfo, rcContexts, ainfo.getAnnotation());
    }

    /**
     * Process a particular annotation whose type is the same as the
     * one returned by @see getAnnotationType(). All information
     * pertinent to the annotation and its context is encapsulated
     * in the passed AnnotationInfo instance.
     *
     * @param ainfo the annotation information
     * @param rcContexts an array of ResourceContainerContext
     * @param ejbAn
     * @return HandlerProcessingResult
     */
    protected HandlerProcessingResult processEJB(AnnotationInfo ainfo,
            ResourceContainerContext[] rcContexts, AnnotationInstance ejbAn)
            throws AnnotationProcessorException {
        IndexView index = ainfo.getIndex();
        String name = AnnotationValues.string(index, ejbAn, "name");
        String beanInterfaceAttribute = AnnotationValues.className(index, ejbAn, "beanInterface");
        boolean defaultBeanInterfaceAttribute = beanInterfaceAttribute.equals(Object.class.getName());

        String defaultLogicalName = null;
        String defaultBeanInterface = null;
        InjectionTarget target = null;

        if (ElementType.FIELD.equals(ainfo.getElementType())) {
            FieldInfo f = ainfo.getAnnotatedElement().asField();
            String targetClassName = f.declaringClass().name().toString();

            defaultLogicalName = targetClassName + "/" + f.name();

            defaultBeanInterface = ClassHierarchy.javaName(f.type());

            target = new InjectionTarget();
            target.setClassName(targetClassName);
            target.setFieldName(f.name());
            target.setMetadataSource(MetadataSource.ANNOTATION);

        } else if (ElementType.METHOD.equals(ainfo.getElementType())) {

            MethodInfo m = ainfo.getAnnotatedElement().asMethod();
            String targetClassName = m.declaringClass().name().toString();

            validateInjectionMethod(m, ainfo);

            // Derive javabean property name.
            String propertyName = getInjectionMethodPropertyName(m, ainfo);

            // prefixing with fully qualified type name
            defaultLogicalName = targetClassName + "/" + propertyName;

            defaultBeanInterface = ClassHierarchy.javaName(m.parameterTypes().get(0));

            target = new InjectionTarget();
            target.setClassName(targetClassName);
            target.setMethodName(m.name());
            target.setMetadataSource(MetadataSource.ANNOTATION);

        } else if( ElementType.TYPE.equals(ainfo.getElementType()) ) {
            // name() and beanInterface() are required for TYPE-level @EJB
            if (name.equals("") || defaultBeanInterfaceAttribute ) {
                ClassInfo c = ainfo.getAnnotatedElement().asClass();
                AnnotationProcessorException fatalException =
                    new AnnotationProcessorException(localStrings.getLocalString(
                    "enterprise.deployment.annotation.handlers.invalidtypelevelejb",
                    "Invalid TYPE-level @EJB with name() = [{0}] and " +
                    "beanInterface = [{1}] in {2}.  Each TYPE-level @EJB " +
                    "must specify both name() and beanInterface().",
                    new Object[] { name, beanInterfaceAttribute, c }),
                    ainfo);
                fatalException.setFatal(true);
                throw fatalException;
            }
        } else {
            // can't happen
            return getDefaultFailedResult();
        }

        // NOTE that default value is Object.class, not null
        String beanInterface = defaultBeanInterfaceAttribute ? defaultBeanInterface : beanInterfaceAttribute;
        String logicalName = name.equals("") ? defaultLogicalName : name;

        String description = AnnotationValues.string(index, ejbAn, "description");
        String lookup = AnnotationValues.string(index, ejbAn, "lookup");
        String mappedName = AnnotationValues.string(index, ejbAn, "mappedName");
        String beanName = AnnotationValues.string(index, ejbAn, "beanName");

        EjbReferenceDescriptor[] ejbRefs = getEjbReferenceDescriptors(logicalName, rcContexts);
        for (EjbReferenceDescriptor ejbRef : ejbRefs) {
            if (target != null)
                ejbRef.addInjectionTarget(target);

            if (!ok(ejbRef.getName()))  // a new one
                ejbRef.setName(logicalName);

            setEjbType(index, ejbRef, DotName.createSimple(beanInterface));

            if (!ok(ejbRef.getDescription()) && ok(description))
                ejbRef.setDescription(description);

            // merge lookup-name and mapped-name
            if (!ejbRef.hasLookupName() && ok(lookup))
                ejbRef.setLookupName(lookup);
            if (!ok(ejbRef.getMappedName()) && ok(mappedName))
                ejbRef.setMappedName(mappedName);

            if (!ok(ejbRef.getLinkName()) && ok(beanName))
                ejbRef.setLinkName(beanName);
        }

        return getDefaultProcessedResult();
    }

    /**
     * Return EjbReferenceDescriptors with given name if exists or a new
     * one without name being set.
     * @param logicalName
     * @param rcContexts
     * @return an array of EjbReferenceDescriptor
     */
    private EjbReferenceDescriptor[] getEjbReferenceDescriptors(String logicalName,
            ResourceContainerContext[] rcContexts) {
        EjbReferenceDescriptor[] ejbRefs = new EjbReferenceDescriptor[rcContexts.length];
        for (int i = 0; i < rcContexts.length; i++) {
            EjbReferenceDescriptor ejbRef = (EjbReferenceDescriptor)rcContexts[i].getEjbReference(logicalName);
            if (ejbRef == null) {
                ejbRef = new EjbReferenceDescriptor();
                rcContexts[i].addEjbReferenceDescriptor(ejbRef);
            }
            ejbRefs[i] = ejbRef;
        }

        return ejbRefs;
    }

    private void setEjbType(IndexView index, EjbReferenceDescriptor ejbRef, DotName beanInterface) {
        if (ClassHierarchy.isSubtype(index, beanInterface, EJBHome.class) ||
                ClassHierarchy.isSubtype(index, beanInterface, EJBLocalHome.class)) {
            setEjbHomeType(index, ejbRef, beanInterface);
        } else {
            setEjbIntfType(index, ejbRef, beanInterface);
        }
    }

    private void setEjbIntfType(IndexView index, EjbReferenceDescriptor ejbRef, DotName beanInterface) {
        // the interface was set in xml
        if (ejbRef.getEjbInterface() != null)
            return;

        ejbRef.setEjbInterface(beanInterface.toString());

        // it's a local interface if it carries @Local, a remote one otherwise
        ejbRef.setLocal(AbstractEjbHandler.hasAnnotation(index, beanInterface, Local.class));
        ejbRef.setType(EjbSessionDescriptor.TYPE);
    }

    private void setEjbHomeType(IndexView index, EjbReferenceDescriptor ejbRef, DotName beanInterface) {

        // the home was set in xml
        if (ejbRef.getHomeClassName() != null)
            return;

        String targetBeanType = EjbSessionDescriptor.TYPE;
        ejbRef.setHomeClassName(beanInterface.toString());

        // the component interface is the return type of the home's create method
        ClassInfo home = index.getClassByName(beanInterface);
        if (home != null) {
            for (MethodInfo m : AbstractEjbHandler.publicMethods(index, home)) {
                if (m.name().equals("create")) {
                    ejbRef.setEjbInterface(ClassHierarchy.rawName(m.returnType()));
                    break;
                }
            }
        }

        ejbRef.setLocal(ClassHierarchy.isSubtype(index, beanInterface, EJBLocalHome.class));
        ejbRef.setType(targetBeanType);
    }
}
