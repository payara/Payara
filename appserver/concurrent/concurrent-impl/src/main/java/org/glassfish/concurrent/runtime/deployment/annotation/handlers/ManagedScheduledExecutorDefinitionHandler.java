/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2022-2026] Payara Foundation and/or its affiliates. All rights reserved.
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

import com.sun.enterprise.deployment.ManagedScheduledExecutorDefinitionDescriptor;
import com.sun.enterprise.deployment.MetadataSource;
import com.sun.enterprise.deployment.ResourceDescriptor;
import com.sun.enterprise.deployment.annotation.context.ResourceContainerContext;
import jakarta.enterprise.concurrent.ManagedScheduledExecutorDefinition;
import org.glassfish.apf.AnnotationHandlerFor;
import org.glassfish.apf.AnnotationInfo;
import org.glassfish.apf.AnnotationProcessorException;
import org.glassfish.apf.AnnotationValues;
import org.glassfish.apf.HandlerProcessingResult;
import org.glassfish.config.support.TranslatedConfigView;
import org.glassfish.deployment.common.JavaEEResourceType;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.IndexView;
import org.jvnet.hk2.annotations.Service;

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
@AnnotationHandlerFor(ManagedScheduledExecutorDefinition.class)
public class ManagedScheduledExecutorDefinitionHandler extends AbstractConcurrencyHandler {

    private static final Logger logger = Logger.getLogger(ManagedScheduledExecutorDefinitionHandler.class.getName());

    @Override
    protected HandlerProcessingResult processAnnotation(AnnotationInfo annotationInfo,
                                                        ResourceContainerContext[] resourceContainerContexts)
            throws AnnotationProcessorException {
        logger.log(Level.INFO, "Entering ManagedScheduledExecutorDefinitionHandler.processAnnotation");
        return processAnnotation(annotationInfo.getIndex(), annotationInfo.getAnnotation(), resourceContainerContexts);
    }

    protected HandlerProcessingResult processAnnotation(IndexView index, AnnotationInstance managedScheduledExecutorDefinition,
                                                        ResourceContainerContext[] contexts) {
        logger.log(Level.INFO, "Registering ManagedScheduledExecutorDefinitionHandler from annotation config");
        ManagedScheduledExecutorDefinitionDescriptor msedd = createDescriptor(index, managedScheduledExecutorDefinition);

        // add to resource contexts
        for (ResourceContainerContext context : contexts) {
            Set<ResourceDescriptor> resourceDescriptors = context.getResourceDescriptors(JavaEEResourceType.MSEDD);
            if (descriptorAlreadyPresent(resourceDescriptors, msedd)) {
                merge(resourceDescriptors, index, managedScheduledExecutorDefinition);
            } else {
                resourceDescriptors.add(msedd);
            }
        }
        return getDefaultProcessedResult();
    }

    public ManagedScheduledExecutorDefinitionDescriptor createDescriptor(IndexView index, AnnotationInstance managedScheduledExecutorDefinition) {
        ManagedScheduledExecutorDefinitionDescriptor msedd =
                new ManagedScheduledExecutorDefinitionDescriptor();
        msedd.setName(TranslatedConfigView.expandValue(AnnotationValues.string(index, managedScheduledExecutorDefinition, "name")));
        msedd.setContext(TranslatedConfigView.expandValue(AnnotationValues.string(index, managedScheduledExecutorDefinition, "context")));

        long hungTaskThreshold = AnnotationValues.longValue(index, managedScheduledExecutorDefinition, "hungTaskThreshold");
        if (hungTaskThreshold < 0) {
            msedd.setHungTaskThreshold(0);
        } else {
            msedd.setHungTaskThreshold(hungTaskThreshold);
        }

        int maxAsync = AnnotationValues.integer(index, managedScheduledExecutorDefinition, "maxAsync");
        if (maxAsync < 0) {
            msedd.setMaxAsync(Integer.MAX_VALUE);
        } else {
            msedd.setMaxAsync(maxAsync);
        }
        msedd.setVirtual(AnnotationValues.bool(index, managedScheduledExecutorDefinition, "virtual"));

        msedd.setMetadataSource(MetadataSource.ANNOTATION);
        msedd.setQualifiers(AnnotationValues.classNames(index, managedScheduledExecutorDefinition, "qualifiers"));
        return msedd;
    }

    private void merge(Set<ResourceDescriptor> resourceDescriptors, IndexView index, AnnotationInstance msed) {
        for (ResourceDescriptor resource : resourceDescriptors) {
            ManagedScheduledExecutorDefinitionDescriptor descriptor = (ManagedScheduledExecutorDefinitionDescriptor) resource;
            if (descriptor.getName().equals(AnnotationValues.string(index, msed, "name"))) {

                long hungTaskThreshold = AnnotationValues.longValue(index, msed, "hungTaskThreshold");
                if (descriptor.getHungTaskThreshold() == -1 && hungTaskThreshold != -1) {
                    descriptor.setHungTaskThreshold(hungTaskThreshold);
                }

                if (descriptor.getMaxAsync() == -1) {
                    descriptor.setMaxAsync(AnnotationValues.integer(index, msed, "maxAsync"));
                }

                if (descriptor.getVirtual() == null) {
                    descriptor.setVirtual(AnnotationValues.bool(index, msed, "virtual"));
                }

                String context = AnnotationValues.string(index, msed, "context");
                if (descriptor.getContext() == null && !context.isBlank()) {
                    descriptor.setContext(TranslatedConfigView.expandValue(context));
                }

                if (descriptor.getQualifiers() == null) {
                    descriptor.setQualifiers(AnnotationValues.classNames(index, msed, "qualifiers"));
                }
            }
        }
    }
}
