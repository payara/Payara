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

package com.sun.enterprise.security.deployment.annotation.handlers;

import com.sun.enterprise.deployment.EjbDescriptor;
import com.sun.enterprise.deployment.MethodDescriptor;
import com.sun.enterprise.deployment.annotation.context.*;
import com.sun.enterprise.deployment.annotation.handlers.PostProcessor;
import com.sun.enterprise.deployment.annotation.handlers.AbstractCommonAttributeHandler;
import com.sun.enterprise.deployment.util.TypeUtil;
import org.glassfish.apf.AnnotatedElementHandler;
import org.glassfish.apf.AnnotationInfo;
import org.glassfish.apf.AnnotationProcessorException;
import org.glassfish.apf.HandlerProcessingResult;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;

/**
 * This is an abstract class encapsulate generic behaviour of auth annotations,
 * such as @DenyAll, @PermitAll and @RolesAllowed.
 *
 * Concrete subclass handlers need to implement the following:
 *     public Class&lt;? extends Annotation&gt; getAnnotationType();
 *     protected void processEjbMethodSecurity(Annotaton authAnnotation,
 *          MethodDescriptor md, EjbDescriptor ejbDesc);
 *     protected Classlt;? extends Annotaion&gt;[] relatedAnnotationClasses();
 *      
 * @author Shing Wai Chan
 */
abstract class AbstractAuthAnnotationHandler extends AbstractCommonAttributeHandler
        implements PostProcessor {

    /**
     * This method processes the EJB Security for the given Annotation.
     */
    protected abstract void processEjbMethodSecurity(Annotation authAnnotation,
            MethodDescriptor md, EjbDescriptor ejbDesc);

    /**
     * Process Annotation with given EjbContexts.
     * @param ainfo
     * @param ejbContexts
     * @return HandlerProcessingResult
     */
    @Override
    protected HandlerProcessingResult processAnnotation(
            AnnotationInfo ainfo, EjbContext[] ejbContexts)
            throws AnnotationProcessorException {

        if (!validateAccessControlAnnotations(ainfo)) {
            return getDefaultFailedResult();
        }

        Annotation authAnnotation = ainfo.getAnnotation();
        for (EjbContext ejbContext : ejbContexts) {
            EjbDescriptor ejbDesc = ejbContext.getDescriptor();
                
            if (ElementType.TYPE.equals(ainfo.getElementType())) {
                // postpone the processing at the end
                ejbContext.addPostProcessInfo(ainfo, this);
            } else { // METHOD
                Method annMethod = (Method) ainfo.getAnnotatedElement();
                for (Object next : ejbDesc.getSecurityBusinessMethodDescriptors()) {
                    MethodDescriptor md = (MethodDescriptor)next;
                    // override by xml
                    if (!hasMethodPermissionsFromDD(md, ejbDesc)) {
                        Method m = md.getMethod(ejbDesc);
                        if (TypeUtil.sameMethodSignature(m, annMethod)) {
                            processEjbMethodSecurity(authAnnotation, md, ejbDesc);
                        }
                    }
                }
            }
        }

        return getDefaultProcessedResult();
    }

    /**
     * Process Annotation with given WebCompContexts.
     * @param ainfo
     * @param webCompContexts
     * @return HandlerProcessingResult
     */
    @Override
    protected HandlerProcessingResult processAnnotation(
            AnnotationInfo ainfo, WebComponentContext[] webCompContexts)
            throws AnnotationProcessorException {

        // this is not for web component
        return getInvalidAnnotatedElementHandlerResult(webCompContexts[0], ainfo);
    }

    /**
     * Process Annotation with given WebBundleContext.
     * @param ainfo
     * @param webBundleContext
     * @return HandlerProcessingResult
     */
    @Override
    protected HandlerProcessingResult processAnnotation(
            AnnotationInfo ainfo, WebBundleContext webBundleContext)
            throws AnnotationProcessorException {

        // this is not for web bundle
        return getInvalidAnnotatedElementHandlerResult(webBundleContext, ainfo);
    }

    /**
     * This method is for processing security annotation associated to ejb.
     * Dervied class call this method may like to override
     *
     * protected void processEjbMethodSecurity(Annotation authAnnotation,
     *         MethodDescriptor md, EjbDescriptor ejbDesc)
     */
    @Override
    public void postProcessAnnotation(AnnotationInfo ainfo,
            AnnotatedElementHandler aeHandler)
            throws AnnotationProcessorException {

        EjbContext ejbContext = (EjbContext)aeHandler;
        EjbDescriptor ejbDesc = ejbContext.getDescriptor();
        Annotation authAnnotation = ainfo.getAnnotation();

        if (!ejbContext.isInherited() &&
                (ejbDesc.getMethodPermissionsFromDD() == null ||
                ejbDesc.getMethodPermissionsFromDD().size() == 0)) {
            for (MethodDescriptor md : getMethodAllDescriptors(ejbDesc)) {
                processEjbMethodSecurity(authAnnotation, md, ejbDesc);
            }
        } else {
            Class classAn = (Class)ainfo.getAnnotatedElement();
            for (Object next : ejbDesc.getSecurityBusinessMethodDescriptors()) {
                MethodDescriptor md = (MethodDescriptor)next;
                // override by existing info
                if (classAn.equals(ejbContext.getDeclaringClass(md)) &&
                        !hasMethodPermissionsFromDD(md, ejbDesc)) {
                    processEjbMethodSecurity(authAnnotation, md, ejbDesc);
                }
            }
        }
    }

    @Override
    protected boolean supportTypeInheritance() {
        return true;
    }

    /**
     * This method returns a list of related annotation types.
     * Those annotations should not be used with the given annotaton type.
     */
    protected Class<? extends Annotation>[] relatedAnnotationTypes() {
        return new Class[0];
    }

    //---------- helper methods ---------

    /**
     * Returns MethodDescriptors representing All for a given EjbDescriptor.
     * @param ejbDesc
     * @return resulting MethodDescriptor
     */
    private Set<MethodDescriptor> getMethodAllDescriptors(
            EjbDescriptor ejbDesc) {
        Set methodAlls = new HashSet();
        if (ejbDesc.isRemoteInterfacesSupported() ||
            ejbDesc.isRemoteBusinessInterfacesSupported()) {
            methodAlls.add(
                    new MethodDescriptor(MethodDescriptor.ALL_METHODS,
                    "", MethodDescriptor.EJB_REMOTE));
            if (ejbDesc.isRemoteInterfacesSupported()) {
                methodAlls.add(
                    new MethodDescriptor(MethodDescriptor.ALL_METHODS,
                    "", MethodDescriptor.EJB_HOME));
            }
        }

        if (ejbDesc.isLocalInterfacesSupported() ||
                ejbDesc.isLocalBusinessInterfacesSupported()) {
            methodAlls.add(
                    new MethodDescriptor(MethodDescriptor.ALL_METHODS,
                    "", MethodDescriptor.EJB_LOCAL));
            if (ejbDesc.isLocalInterfacesSupported()) {
                methodAlls.add(
                    new MethodDescriptor(MethodDescriptor.ALL_METHODS,
                    "", MethodDescriptor.EJB_LOCALHOME));
            }
        }

        if (ejbDesc.isLocalBean()) {
            methodAlls.add(
                    new MethodDescriptor(MethodDescriptor.ALL_METHODS,
                    "", MethodDescriptor.EJB_LOCAL));    
        }

        if (ejbDesc.hasWebServiceEndpointInterface()) {
            methodAlls.add(
                    new MethodDescriptor(MethodDescriptor.ALL_METHODS,
                    "", MethodDescriptor.EJB_WEB_SERVICE));
        }

        return methodAlls;
    }

    /**
     * @param methodDesc
     * @param ejbDesc
     * @return whether the given methodDesc has permission defined in ejbDesc
     */
    private boolean hasMethodPermissionsFromDD(MethodDescriptor methodDesc,
            EjbDescriptor ejbDesc) {
        HashMap methodPermissionsFromDD = ejbDesc.getMethodPermissionsFromDD();
        if (methodPermissionsFromDD != null) {
            Set allMethods = ejbDesc.getMethodDescriptors();
            for (Object mdObjsObj : methodPermissionsFromDD.values()) {
                List mdObjs = (List)mdObjsObj;
                for (Object mdObj : mdObjs) {
                    MethodDescriptor md = (MethodDescriptor)mdObj;
                    for (Object style3MdObj :
                            md.doStyleConversion(ejbDesc, allMethods)) {
                        MethodDescriptor style3Md = (MethodDescriptor)style3MdObj;
                        if (methodDesc.equals(style3Md)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * This method checks whether annotations are compatible.
     * One cannot have two or more of the @DenyAll, @PermitAll, @RoleAllowed.
     *
     * @param ainfo
     * @return validity
     */
    private boolean validateAccessControlAnnotations(AnnotationInfo ainfo)
            throws AnnotationProcessorException {

        boolean validity = true;
        AnnotatedElement ae = (AnnotatedElement)ainfo.getAnnotatedElement();

        int count = 0;
        boolean hasDenyAll = false;

        count += (ae.isAnnotationPresent(RolesAllowed.class)? 1 : 0);
        if (ae.isAnnotationPresent(DenyAll.class)) {
            count += 1;
            hasDenyAll = true;
        }

        // continue the checking if not already more than one
        if (count < 2 && ae.isAnnotationPresent(PermitAll.class)) {
            count++;
        }

        if (count > 1) {
            log(Level.SEVERE, ainfo,
                localStrings.getLocalString(
                "enterprise.deployment.annotation.handlers.morethanoneauthannotation",
                "One cannot have more than one of @RolesAllowed, @PermitAll, @DenyAll in the same AnnotatedElement."));
            validity = false;
        }

        return validity;
    }
}
