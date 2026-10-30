/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2022-2024] Payara Foundation and/or its affiliates. All rights reserved.
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

import com.sun.enterprise.deployment.ManagedExecutorDefinitionDescriptor;
import com.sun.enterprise.deployment.MetadataSource;
import com.sun.enterprise.deployment.ResourceDescriptor;
import com.sun.enterprise.deployment.annotation.context.ResourceContainerContext;
import jakarta.enterprise.concurrent.ManagedExecutorDefinition;
import java.util.Arrays;
import org.glassfish.apf.AnnotationHandlerFor;
import org.glassfish.apf.AnnotationInfo;
import org.glassfish.apf.AnnotationProcessorException;
import org.glassfish.apf.HandlerProcessingResult;
import org.glassfish.config.support.TranslatedConfigView;
import org.glassfish.deployment.common.JavaEEResourceType;
import org.jvnet.hk2.annotations.Service;

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
@AnnotationHandlerFor(ManagedExecutorDefinition.class)
public class ManagedExecutorDefinitionHandler extends AbstractConcurrencyHandler {

    private static final Logger logger = Logger.getLogger(ManagedExecutorDefinitionHandler.class.getName());

    @Override
    protected HandlerProcessingResult processAnnotation(AnnotationInfo annotationInfo,
                                                        ResourceContainerContext[] resourceContainerContexts)
            throws AnnotationProcessorException {
        logger.log(Level.INFO, "Entering ManagedExecutorDefinitionHandler.processAnnotation");
        ManagedExecutorDefinition managedExecutorDefinition = (ManagedExecutorDefinition) annotationInfo.getAnnotation();
        return processAnnotation(managedExecutorDefinition, resourceContainerContexts);
    }

    protected HandlerProcessingResult processAnnotation(ManagedExecutorDefinition managedExecutorDefinition,
            ResourceContainerContext[] resourceContainerContexts) {
        logger.log(Level.INFO, "Registering ManagedExecutorService from annotation config");
        ManagedExecutorDefinitionDescriptor medes = createDescriptor(managedExecutorDefinition);

        // add to resource contexts
        for (ResourceContainerContext context : resourceContainerContexts) {
            Set<ResourceDescriptor> resourceDescriptors = context.getResourceDescriptors(JavaEEResourceType.MEDD);
            if (descriptorAlreadyPresent(resourceDescriptors, medes)) {
                merge(resourceDescriptors, managedExecutorDefinition);
            } else {
                resourceDescriptors.add(medes);
            }
        }
        return getDefaultProcessedResult();
    }

    public ManagedExecutorDefinitionDescriptor createDescriptor(ManagedExecutorDefinition managedExecutorDefinition) {
        ManagedExecutorDefinitionDescriptor medd = new ManagedExecutorDefinitionDescriptor();
        medd.setName(TranslatedConfigView.expandValue(managedExecutorDefinition.name()));
        medd.setContext(TranslatedConfigView.expandValue(managedExecutorDefinition.context()));

        if(managedExecutorDefinition.hungTaskThreshold() < 0) {
            medd.setHungAfterSeconds(0);
        } else {
            medd.setHungAfterSeconds(managedExecutorDefinition.hungTaskThreshold());
        }

        if(managedExecutorDefinition.maxAsync() < 0) {
            medd.setMaximumPoolSize(Integer.MAX_VALUE);
        } else {
            medd.setMaximumPoolSize(managedExecutorDefinition.maxAsync());
        }

        medd.setVirtual(managedExecutorDefinition.virtual());
        medd.setMetadataSource(MetadataSource.ANNOTATION);
        medd.setQualifiers(Arrays.asList(managedExecutorDefinition.qualifiers()).stream().map(c -> c.getName()).collect(Collectors.toSet()));
        return medd;
    }

    private void merge(Set<ResourceDescriptor> resourceDescriptors, ManagedExecutorDefinition med) {
        for (ResourceDescriptor resource : resourceDescriptors) {
            ManagedExecutorDefinitionDescriptor descriptor = (ManagedExecutorDefinitionDescriptor) resource;
            if (descriptor.getName().equals(med.name())) {

                if (descriptor.getHungAfterSeconds() == -1 && med.hungTaskThreshold() != -1) {
                    descriptor.setHungAfterSeconds(med.hungTaskThreshold());
                }

                if (descriptor.getMaximumPoolSize() == -1 && med.maxAsync() != -1) {
                    descriptor.setMaximumPoolSize(med.maxAsync());
                }

                if (descriptor.getContext() == null && med.context() != null && !med.context().isBlank()) {
                    descriptor.setContext(TranslatedConfigView.expandValue(med.context()));
                }

                if (descriptor.getQualifiers() == null && med.qualifiers() != null) {
                    descriptor.setQualifiers(mapToSetOfStrings(med.qualifiers()));
                }
            }
        }
    }
}
