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

import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.deployment.ContextServiceDefinitionDescriptor;
import com.sun.enterprise.deployment.ResourceDescriptor;
import com.sun.enterprise.deployment.annotation.context.ResourceContainerContext;
import jakarta.enterprise.concurrent.ContextServiceDefinition;
import jakarta.inject.Inject;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.glassfish.apf.AnnotationHandlerFor;
import org.glassfish.apf.AnnotationInfo;
import org.glassfish.apf.AnnotationProcessorException;
import org.glassfish.apf.HandlerProcessingResult;
import org.glassfish.config.support.TranslatedConfigView;
import org.glassfish.deployment.common.JavaEEResourceType;
import org.jvnet.hk2.annotations.Service;

/**
 * Handler for @ContextServiceDefinition.
 *
 * @author Petr Aubrecht <aubrecht@asoftware.cz>
 */
@Service
@AnnotationHandlerFor(ContextServiceDefinition.class)
public class ContextServiceDefinitionHandler extends AbstractConcurrencyHandler {

    private static final Logger logger = Logger.getLogger(ContextServiceDefinitionHandler.class.getName());

    @Inject
    private Domain domain;

    @Override
    protected HandlerProcessingResult processAnnotation(AnnotationInfo annotationInfo,
            ResourceContainerContext[] resourceContainerContexts)
            throws AnnotationProcessorException {
        logger.log(Level.INFO, "Entering ContextServiceDefinitionHandler.processAnnotation");
        ContextServiceDefinition contextServiceDefinition = (ContextServiceDefinition) annotationInfo.getAnnotation();

        processSingleAnnotation(contextServiceDefinition, resourceContainerContexts);

        return getDefaultProcessedResult();
    }

    public void processSingleAnnotation(ContextServiceDefinition contextServiceDefinition,
            ResourceContainerContext[] resourceContainerContexts) {
        logger.log(Level.INFO, "Creating custom context service by annotation");
        ContextServiceDefinitionDescriptor csdd = createDescriptor(contextServiceDefinition);

        // add to resource contexts
        for (ResourceContainerContext context : resourceContainerContexts) {
            Set<ResourceDescriptor> resourceDescriptors = context.getResourceDescriptors(JavaEEResourceType.CSDD);
            if (descriptorAlreadyPresent(resourceDescriptors, csdd)) {
                merge(resourceDescriptors, contextServiceDefinition);
            } else {
                resourceDescriptors.add(csdd);
            }
        }
    }

    public ContextServiceDefinitionDescriptor createDescriptor(ContextServiceDefinition contextServiceDefinition) {
        Set<String> unusedContexts = collectUnusedContexts(contextServiceDefinition);

        ContextServiceDefinitionDescriptor csdd = new ContextServiceDefinitionDescriptor();
        csdd.setDescription("Context Service Definition");
        csdd.setName(TranslatedConfigView.expandValue(contextServiceDefinition.name()));
        csdd.setPropagated(evaluateContexts(contextServiceDefinition.propagated(), unusedContexts));
        csdd.setCleared(evaluateContexts(contextServiceDefinition.cleared(), unusedContexts));
        csdd.setUnchanged(evaluateContexts(contextServiceDefinition.unchanged(), unusedContexts));
        csdd.setQualifiers(Arrays.asList(contextServiceDefinition.qualifiers()).stream().map(c -> c.getName()).collect(Collectors.toSet()));

        return csdd;
    }

    private void merge(Set<ResourceDescriptor> resourceDescriptors, ContextServiceDefinition csd) {
        for (ResourceDescriptor resource : resourceDescriptors) {
            ContextServiceDefinitionDescriptor descriptor = (ContextServiceDefinitionDescriptor) resource;
            if (descriptor.getName().equals(csd.name())) {

                if (descriptor.getCleared() == null && csd.cleared() != null) {
                    descriptor.setCleared(new HashSet<>(Arrays.asList(csd.cleared())));
                }

                if (descriptor.getPropagated() == null && csd.propagated() != null) {
                    descriptor.setPropagated(new HashSet<>(Arrays.asList(csd.propagated())));
                }

                if (descriptor.getUnchanged() == null && csd.unchanged() != null) {
                    descriptor.setUnchanged(new HashSet<>(Arrays.asList(csd.unchanged())));
                }

                if (descriptor.getQualifiers() == null && csd.qualifiers() != null) {
                    descriptor.setQualifiers(mapToSetOfStrings(csd.qualifiers()));
                }
            }
        }
    }

    private Set<String> evaluateContexts(String[] sourceContexts, Set<String> unusedContexts) {
        Set<String> contexts = new HashSet<>();
        for (String context : sourceContexts) {
            if (ContextServiceDefinition.ALL_REMAINING.equals(context)) {
                contexts.addAll(unusedContexts);
                contexts.add(ContextServiceDefinition.ALL_REMAINING); // keep remaining for custom context providers
            } else {
                contexts.add(context);
            }
        }
        return contexts;
    }

    private Set<String> collectUnusedContexts(ContextServiceDefinition csdd) {
        Map<String, String> usedContexts = new HashMap<>();
        for (String context : csdd.propagated()) {
            usedContexts.put(context, "propagated");
        }
        for (String context : csdd.cleared()) {
            String previous = usedContexts.put(context, "cleared");
            if (previous != null) {
                throw new RuntimeException("Duplicate context " + context + " in " + previous + " and cleared context attributes in ContextServiceDefinition annotation!");
            }
        }
        for (String context : csdd.unchanged()) {
            String previous = usedContexts.put(context, "unchanged");
            if (previous != null) {
                throw new RuntimeException("Duplicate context " + context + " in " + previous + " and unchanged context attributes in ContextServiceDefinition annotation!");
            }
        }
        Set<String> allStandardContexts = new HashSet(Set.of(
                ContextServiceDefinition.APPLICATION,
                ContextServiceDefinition.SECURITY,
                ContextServiceDefinition.TRANSACTION));
        allStandardContexts.removeAll(usedContexts.keySet());
        return allStandardContexts;
    }
}
