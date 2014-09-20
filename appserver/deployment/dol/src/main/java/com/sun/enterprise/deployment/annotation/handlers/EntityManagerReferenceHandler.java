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

package com.sun.enterprise.deployment.annotation.handlers;

import com.sun.enterprise.deployment.EntityManagerReferenceDescriptor;
import com.sun.enterprise.deployment.InjectionTarget;
import com.sun.enterprise.deployment.MetadataSource;
import com.sun.enterprise.deployment.annotation.context.AppClientContext;
import com.sun.enterprise.deployment.annotation.context.ResourceContainerContext;
import org.glassfish.apf.*;
import org.jvnet.hk2.annotations.Service;

import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceProperty;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.logging.Level;

/**
 * This handler is responsible for handling the 
 * javax.persistence.PersistenceUnit annotation.
 *
 */
@Service
@AnnotationHandlerFor(PersistenceContext.class)
public class EntityManagerReferenceHandler 
    extends AbstractResourceHandler {
    
    public EntityManagerReferenceHandler() {
    }

    /**
     * Process a particular annotation which type is the same as the
     * one returned by @see getAnnotationType(). All information
     * pertinent to the annotation and its context is encapsulated
     * in the passed AnnotationInfo instance.
     *
     * @param ainfo the annotation information
     * @param rcContexts an array of ResourceContainerContext
     * @return HandlerProcessingResult
     */
    protected HandlerProcessingResult processAnnotation(AnnotationInfo ainfo,
            ResourceContainerContext[] rcContexts)
            throws AnnotationProcessorException {

        AnnotatedElementHandler aeHandler = 
            ainfo.getProcessingContext().getHandler();
        if (aeHandler instanceof AppClientContext) {
            // application client does not support @PersistenceContext
            String msg = localStrings.getLocalString(
                "enterprise.deployment.annotation.handlers.invalidaehandler",
                "Invalid annotation symbol found for this type of class.");
            log(Level.WARNING, ainfo, msg);
            return getDefaultProcessedResult();
        }
        PersistenceContext emRefAn = (PersistenceContext)ainfo.getAnnotation();
        return processEmRef(ainfo, rcContexts, emRefAn);
    }


    /**
     * Process a particular annotation which type is the same as the
     * one returned by @see getAnnotationType(). All information
     * pertinent to the annotation and its context is encapsulated
     * in the passed AnnotationInfo instance.
     *
     */
    protected HandlerProcessingResult processEmRef(AnnotationInfo ainfo,
            ResourceContainerContext[] rcContexts, PersistenceContext emRefAn)
            throws AnnotationProcessorException {
        EntityManagerReferenceDescriptor emRefs[] = null;

        if (ElementType.FIELD.equals(ainfo.getElementType())) {
            Field f = (Field)ainfo.getAnnotatedElement();
            String targetClassName = f.getDeclaringClass().getName();

            String logicalName = emRefAn.name();

            // applying with default
            if (logicalName.equals("")) {
                logicalName = targetClassName + "/" + f.getName();
            }

            emRefs = getEmReferenceDescriptors(logicalName, rcContexts);
            
            InjectionTarget target = new InjectionTarget();
            target.setFieldName(f.getName());
            target.setClassName(targetClassName);            
            target.setMetadataSource(MetadataSource.ANNOTATION);

            for (EntityManagerReferenceDescriptor emRef : emRefs) {
                
                emRef.addInjectionTarget(target);
            
                if (emRef.getName().length() == 0) { // a new one
                    processNewEmRefAnnotation(emRef, logicalName, emRefAn);
                }
            }
        } else if (ElementType.METHOD.equals(ainfo.getElementType())) {

            Method m = (Method)ainfo.getAnnotatedElement();
            String targetClassName = m.getDeclaringClass().getName();

            String logicalName = emRefAn.name();
            if( logicalName.equals("") ) {
                // Derive javabean property name.
                String propertyName = 
                    getInjectionMethodPropertyName(m, ainfo);

                // prefixing with fully qualified type name
                logicalName = targetClassName + "/" + propertyName;
            }

            validateInjectionMethod(m, ainfo);

            emRefs = getEmReferenceDescriptors(logicalName, rcContexts);
            
            InjectionTarget target = new InjectionTarget();
            target.setMethodName(m.getName());
            target.setClassName(targetClassName);                   
            target.setMetadataSource(MetadataSource.ANNOTATION);

            for (EntityManagerReferenceDescriptor emRef : emRefs) {
                
                emRef.addInjectionTarget(target);

                if (emRef.getName().length() == 0) { // a new one

                    processNewEmRefAnnotation(emRef, logicalName, emRefAn);

                }
            }
        } else if( ElementType.TYPE.equals(ainfo.getElementType()) ) {
            // name() is required for TYPE-level usage
            String logicalName = emRefAn.name();

            if( "".equals(logicalName) ) {
                log(Level.SEVERE, ainfo,
                    localStrings.getLocalString(
                    "enterprise.deployment.annotation.handlers.nonametypelevel",
                    "TYPE-Level annotation symbol on class must specify name."));
                return getDefaultFailedResult();
            }
                               
            emRefs = getEmReferenceDescriptors(logicalName, rcContexts);
            for (EntityManagerReferenceDescriptor emRef : emRefs) {
                if (emRef.getName().length() == 0) { // a new one

                    processNewEmRefAnnotation(emRef, logicalName, emRefAn);
                                            
                }
            }
        } 

        return getDefaultProcessedResult();
    }

    /**
     * Return EntityManagerReferenceDescriptors with given name 
     * if exists or a new one without name being set.
     */
    private EntityManagerReferenceDescriptor[] 
        getEmReferenceDescriptors(String logicalName, 
                                   ResourceContainerContext[] rcContexts) {
            
        EntityManagerReferenceDescriptor emRefs[] =
                new EntityManagerReferenceDescriptor[rcContexts.length];
        for (int i = 0; i < rcContexts.length; i++) {
            EntityManagerReferenceDescriptor emRef =
                (EntityManagerReferenceDescriptor)rcContexts[i].
                    getEntityManagerReference(logicalName);
            if (emRef == null) {
                emRef = new EntityManagerReferenceDescriptor();
                rcContexts[i].addEntityManagerReferenceDescriptor
                    (emRef);
            }
            emRefs[i] = emRef;
        }

        return emRefs;
    }

    private void processNewEmRefAnnotation
        (EntityManagerReferenceDescriptor emRef,
         String logicalName, PersistenceContext annotation) {
        
        emRef.setName(logicalName);
        
        if( !(annotation.unitName().equals("")) ) {
            emRef.setUnitName(annotation.unitName());
        }

        emRef.setPersistenceContextType(annotation.type());
        emRef.setSynchronizationType(annotation.synchronization());

        // Add each property from annotation to descriptor, unless
        // it has been overridden within the .xml.
        Map existingProperties = emRef.getProperties();

        for(PersistenceProperty next : annotation.properties()) {
            if( !existingProperties.containsKey(next.name()) ) {
                emRef.addProperty(next.name(), next.value());
            }
        }

    }

}
