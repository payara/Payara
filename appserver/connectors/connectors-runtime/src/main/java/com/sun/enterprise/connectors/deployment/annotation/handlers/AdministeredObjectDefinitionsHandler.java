/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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
package com.sun.enterprise.connectors.deployment.annotation.handlers;


import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;

import javax.resource.AdministeredObjectDefinition;
import javax.resource.AdministeredObjectDefinitions;

import org.glassfish.apf.AnnotationHandlerFor;
import org.glassfish.apf.AnnotationInfo;
import org.glassfish.apf.AnnotationProcessorException;
import org.glassfish.apf.HandlerProcessingResult;
import org.jvnet.hk2.annotations.Service;

import com.sun.enterprise.deployment.AdministeredObjectDefinitionDescriptor;
import com.sun.enterprise.deployment.annotation.context.ResourceContainerContext;
import com.sun.enterprise.deployment.annotation.handlers.AbstractResourceHandler;
import com.sun.enterprise.util.LocalStringManagerImpl;

/**
 * @author Dapeng Hu
 */
@Service
@AnnotationHandlerFor(AdministeredObjectDefinitions.class)
public class AdministeredObjectDefinitionsHandler extends AbstractResourceHandler {
    
    protected final static LocalStringManagerImpl localStrings =
            new LocalStringManagerImpl(AdministeredObjectDefinitionsHandler.class);

    public AdministeredObjectDefinitionsHandler() {
    }

    @Override
    protected HandlerProcessingResult processAnnotation(AnnotationInfo ainfo,  ResourceContainerContext[] rcContexts)
            throws AnnotationProcessorException {
        AdministeredObjectDefinitions defns = (AdministeredObjectDefinitions) ainfo.getAnnotation();
        AdministeredObjectDefinition values[] = defns.value();
        Set<String> duplicates = new HashSet<String>();
        if(values != null && values.length >0){
            for(AdministeredObjectDefinition defn : values){
                String defnName = AdministeredObjectDefinitionDescriptor.getJavaName(defn.name());

                if(duplicates.contains(defnName)){
                    // where is the local-string file?
                    String localString = localStrings.getLocalString(
                            "enterprise.deployment.annotation.handlers.administeredobjectdefinitionsduplicates",
                            "@AdministeredObjectDefinitions cannot have multiple definitions with same name : '{0}'",
                            defnName);
                    throw new IllegalStateException(localString);
                }else{
                    duplicates.add(defnName);
                }
                AdministeredObjectDefinitionHandler handler = new AdministeredObjectDefinitionHandler();
                handler.processAnnotation(defn, ainfo, rcContexts);
            }
            duplicates.clear();
        }
        return getDefaultProcessedResult();
    }

    
    public Class<? extends Annotation>[] getTypeDependencies() {
        return getEjbAndWebAnnotationTypes();
    }

}
