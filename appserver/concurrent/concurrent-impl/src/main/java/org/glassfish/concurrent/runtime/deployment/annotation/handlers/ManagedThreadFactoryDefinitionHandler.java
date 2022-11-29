/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2022] Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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
package org.glassfish.concurrent.runtime.deployment.annotation.handlers;

import com.sun.enterprise.deployment.ManagedThreadFactoryDefinitionDescriptor;
import com.sun.enterprise.deployment.MetadataSource;
import com.sun.enterprise.deployment.ResourceDescriptor;
import com.sun.enterprise.deployment.annotation.context.ResourceContainerContext;
import com.sun.enterprise.deployment.annotation.handlers.AbstractResourceHandler;
import jakarta.enterprise.concurrent.ManagedThreadFactoryDefinition;
import org.glassfish.apf.AnnotationHandlerFor;
import org.glassfish.apf.AnnotationInfo;
import org.glassfish.apf.AnnotationProcessorException;
import org.glassfish.apf.HandlerProcessingResult;
import org.glassfish.config.support.TranslatedConfigView;
import org.glassfish.deployment.common.JavaEEResourceType;
import org.jvnet.hk2.annotations.Service;

import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
@AnnotationHandlerFor(ManagedThreadFactoryDefinition.class)
public class ManagedThreadFactoryDefinitionHandler extends AbstractResourceHandler {

    private static final Logger logger = Logger.getLogger(ManagedThreadFactoryDefinitionHandler.class.getName());

    @Override
    protected HandlerProcessingResult processAnnotation(AnnotationInfo annotationInfo,
                                                        ResourceContainerContext[] resourceContainerContexts)
            throws AnnotationProcessorException {
        logger.log(Level.INFO, "Entering ManagedThreadFactoryDefinitionHandler.processAnnotation");
        ManagedThreadFactoryDefinition managedThreadFactoryDefinition =
                (ManagedThreadFactoryDefinition) annotationInfo.getAnnotation();

        return processAnnotation(managedThreadFactoryDefinition, resourceContainerContexts);
    }

    protected HandlerProcessingResult processAnnotation(ManagedThreadFactoryDefinition managedThreadFactoryDefinition,
                                                        ResourceContainerContext[] contexts) {
        logger.log(Level.INFO, "Registering ManagedThreadFactory from annotation config");
        for (ResourceContainerContext context : contexts) {
            Set<ResourceDescriptor> resourceDescriptors = context.getResourceDescriptors(JavaEEResourceType.MTFDD);
            ManagedThreadFactoryDefinitionDescriptor mtfdd =
                    createDescriptor(managedThreadFactoryDefinition);
            if (descriptorAlreadyPresent(resourceDescriptors, mtfdd)) {
                merge(resourceDescriptors, managedThreadFactoryDefinition);
            } else {
                resourceDescriptors.add(mtfdd);
            }
        }
        return getDefaultProcessedResult();
    }

    public ManagedThreadFactoryDefinitionDescriptor createDescriptor(ManagedThreadFactoryDefinition managedThreadFactoryDefinition) {
        ManagedThreadFactoryDefinitionDescriptor mtfdd = new ManagedThreadFactoryDefinitionDescriptor();
        mtfdd.setMetadataSource(MetadataSource.ANNOTATION);
        mtfdd.setName(TranslatedConfigView.expandValue(managedThreadFactoryDefinition.name()));
        mtfdd.setContext(TranslatedConfigView.expandValue(managedThreadFactoryDefinition.context()));
        if(managedThreadFactoryDefinition.priority() <= 0) {
            mtfdd.setPriority(Thread.NORM_PRIORITY);
        } else {
            mtfdd.setPriority(managedThreadFactoryDefinition.priority());
        }
        return mtfdd;
    }

    private boolean descriptorAlreadyPresent(final Set<ResourceDescriptor> resourceDescriptors,
                                             final ManagedThreadFactoryDefinitionDescriptor mtfdd) {
        Optional<ResourceDescriptor> optResourceDescriptor = resourceDescriptors
                .stream().filter(d -> d.equals(mtfdd)).findAny();
        return optResourceDescriptor.isPresent();
    }

    private void merge(Set<ResourceDescriptor> resourceDescriptors, ManagedThreadFactoryDefinition mtfdd) {
        for (ResourceDescriptor resource : resourceDescriptors) {
            ManagedThreadFactoryDefinitionDescriptor descriptor = (ManagedThreadFactoryDefinitionDescriptor) resource;
            if (descriptor.getName().equals(mtfdd.name())) {

                if (descriptor.getPriority() == -1 && mtfdd.priority() != -1) {
                    descriptor.setPriority(mtfdd.priority());
                }

                if (descriptor.getContext() == null && mtfdd.context() != null && !mtfdd.context().isBlank()) {
                    descriptor.setContext(TranslatedConfigView.expandValue(mtfdd.context()));
                }
            }
        }
    }
}
