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
// Portions Copyright [2025] [Payara Foundation and/or its affiliates]

package com.sun.enterprise.deployment.annotation.factory;

import org.glassfish.apf.AnnotationHandler;
import org.glassfish.apf.AnnotationInfo;
import org.glassfish.apf.AnnotationProcessor;
import org.glassfish.apf.AnnotationProcessorException;
import org.glassfish.apf.HandlerProcessingResult;
import org.glassfish.apf.context.AnnotationContext;
import org.glassfish.apf.factory.Factory;
import org.glassfish.apf.impl.AnnotationUtils;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.deployment.common.RootDeploymentDescriptor;
import org.glassfish.hk2.api.ActiveDescriptor;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.BuilderHelper;
import org.glassfish.internal.deployment.JandexIndexer;
import org.glassfish.internal.deployment.JandexIndexer.Index;
import org.jboss.jandex.AnnotationInstance;
import org.jvnet.hk2.annotations.Service;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.reflect.AnnotatedElement;
import java.net.URI;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This factory is responsible for initializing a ready to use
 * JandexAnnotationProcessor.
 *
 * @author Shing Wai Chan
 */
@Service
@Singleton
public class SJSASFactory extends Factory {
    private static final Logger logger = AnnotationUtils.getLogger();

    @Inject
    private ServiceLocator locator;
    @Inject
    JandexIndexer jandexIndexer;

    private final Map<String, AnnotationHandler> annotationHandlers = new ConcurrentHashMap<>();

    @Deprecated(forRemoval = true)
    public AnnotationProcessor getAnnotationProcessor(boolean isMetaDataComplete) {
        throw new UnsupportedOperationException("This method is deprecated and should not be used");
    }

    @Deprecated(forRemoval = true)
    public Set<String> getAnnotations(boolean isMetaDataComplete) {
        throw new UnsupportedOperationException("This method is deprecated and should not be used");
    }

    public void processAnnotations(DeploymentContext deploymentContext, RootDeploymentDescriptor bundleDesc,
                                   ReadableArchive archive) throws IOException {
        if (checkAlreadyProcessed(deploymentContext, bundleDesc)) {
            return;
        }

        String archiveURI = bundleDesc.getModuleDescriptor().getArchiveUri();
        if (archiveURI == null) {
            archiveURI = bundleDesc.getName();
        }
        Index index = jandexIndexer.getAllIndexes(deploymentContext)
                .get(new File(archiveURI).toURI().toString());
        if (index == null) {
            File file = new File(archiveURI);
            URI uri = file.toURI();
            if (file.isDirectory()) {
                // TODO: assuming there is only one file in the directory
                // exploded WAR support for EAR
                uri = Files.list(file.toPath()).filter(path -> path.toString().endsWith("ar")).findAny().get().toUri();
            }
            index = jandexIndexer.getIndexesByURI(deploymentContext, Collections.singleton(uri)).values().stream().findAny().get();
        }
        AnnotationProcessingContext processingContext = new AnnotationProcessingContext(
                AnnotatedElementHandlerFactory.createAnnotatedElementHandler(bundleDesc), archive);
        Set<AnnotationInstance> annotationInstances = new HashSet<>();
        for (Map.Entry<String, AnnotationHandler> entry : annotationHandlers.entrySet()) {
            Class<? extends Annotation> annotationClass = entry.getValue().getAnnotationType();
            index.getIndex().getAnnotations(entry.getKey()).forEach(annotationInstance -> {
                if (annotationInstances.add(annotationInstance)) {
                    try {
                        Class<?> cls = deploymentContext.getClassLoader().loadClass(mapAnnotationToClassName(annotationInstance));
                        logger.fine("Processing annotation: " + annotationInstance + " on class: " + cls
                        + " bundleDesc: " + bundleDesc.getName() + " of type: " + bundleDesc.getClass()
                        + " with id: " + System.identityHashCode(bundleDesc));
                        processingContext.getProcessor().process(processingContext, new Class<?>[] { cls });
                        ((AnnotationContext) processingContext.getHandler()).setProcessingContext(processingContext);
                        AnnotatedElement element = mapAnnotationToElement(cls, annotationInstance);
                        AnnotationInfo annotationInfo = new AnnotationInfo(processingContext,
                                element, element.getAnnotation(annotationClass), mapAnnotationToElementType(annotationInstance));
                        entry.getValue().processAnnotation(annotationInfo);
                    } catch (AnnotationProcessorException | ReflectiveOperationException e) {
                        throw new IllegalStateException(e);
                    } finally {
                        try {
                            processingContext.getProcessor().process(processingContext, new Class<?>[] { null });
                            ((AnnotationContext) processingContext.getHandler()).setProcessingContext(null);
                        } catch (AnnotationProcessorException e) {
                            logger.log(Level.FINE, "Error processing annotation", e);
                        }
                    }
                }
            });
        }
    }

    private boolean checkAlreadyProcessed(DeploymentContext deploymentContext, RootDeploymentDescriptor bundleDescriptor) {
        @SuppressWarnings("unchecked")
        Set<RootDeploymentDescriptor> alreadyProcessed =
                deploymentContext.getTransientAppMetaData("alreadyProcessedAnnotations", Set.class);
        if (alreadyProcessed == null) {
            alreadyProcessed = Collections.newSetFromMap(new IdentityHashMap<>());
            deploymentContext.addTransientAppMetaData("alreadyProcessedAnnotations", alreadyProcessed);
        }
        return !alreadyProcessed.add(bundleDescriptor);
    }

    /**
     * *** TODO: duplicate from weld-gf-connector
     * @param annotationInstance
     * @return
     */
    private static String mapAnnotationToClassName(AnnotationInstance annotationInstance) {
        String className = null;
        switch (annotationInstance.target().kind()) {
            case CLASS:
                className = annotationInstance.target().asClass().name().toString();
                break;
            case FIELD:
                className = annotationInstance.target().asField().declaringClass().name().toString();
                break;
            case METHOD:
                className = annotationInstance.target().asMethod().receiverType().name().toString();
                break;
            case METHOD_PARAMETER:
                className = annotationInstance.target().asMethodParameter().method().receiverType().name().toString();
                break;
            case TYPE:
                className = annotationInstance.target().asType().asClass().toString();
                break;
            case RECORD_COMPONENT:
                className = annotationInstance.target().asRecordComponent().declaringClass().name().toString();
                break;
        }
        return className;
    }

    private static AnnotatedElement mapAnnotationToElement(Class<?> cls, AnnotationInstance annotationInstance)
            throws ReflectiveOperationException {
        AnnotatedElement element = null;
        switch (annotationInstance.target().kind()) {
            case CLASS:
            case TYPE:
                element = cls;
                break;
            case FIELD:
                element = cls.getDeclaredField(annotationInstance.target().asField().name());
                break;
            case METHOD:
                int parametersCount = annotationInstance.target().asMethod().parametersCount();
                if (annotationInstance.target().asMethod().parametersCount() == 0) {
                    element = cls.getDeclaredMethod(annotationInstance.target().asMethod().name());
                } else {
                    var parameters = new Class[parametersCount];
                    for (int count = 0; count < parametersCount; count++) {
                        parameters[count] = cls.getClassLoader().loadClass(annotationInstance.target()
                                .asMethod().parameterType(count).name().toString());
                    }
                    element = cls.getDeclaredMethod(annotationInstance.target().asMethod().name(), parameters);
                }
                break;
            case RECORD_COMPONENT:
                element = cls.getDeclaredField(annotationInstance.target().asRecordComponent().name());
                break;
            case METHOD_PARAMETER:
                throw new UnsupportedOperationException("Method parameter not supported");
        }
        return element;
    }

    private static ElementType mapAnnotationToElementType(AnnotationInstance annotationInstance) {
        switch (annotationInstance.target().kind()) {
            case CLASS:
            case TYPE:
                return ElementType.TYPE;
            case FIELD:
                return ElementType.FIELD;
            case METHOD:
                return ElementType.METHOD;
            case METHOD_PARAMETER:
                return ElementType.PARAMETER;
            default:
                throw new IllegalArgumentException("Unsupported target kind: " + annotationInstance.target().kind());
        }
    }

    private static String getAnnotationHandlerForStringValue(ActiveDescriptor<AnnotationHandler> onMe) {
        Map<String, List<String>> metadata = onMe.getMetadata();
        List<String> answers = metadata.get(AnnotationHandler.ANNOTATION_HANDLER_METADATA);
        if (answers == null || answers.isEmpty()) return null;
        
        return answers.get(0);
    }

    @SuppressWarnings({ "unused", "unchecked" })
    @PostConstruct
    private void postConstruct() {
        for (ActiveDescriptor<?> i : locator.getDescriptors(BuilderHelper.createContractFilter(
                AnnotationHandler.class.getName()))) {
            ActiveDescriptor<AnnotationHandler> descriptor = (ActiveDescriptor<AnnotationHandler>) i;
            
            String annotationTypeName = getAnnotationHandlerForStringValue(descriptor);
            if (annotationTypeName == null) continue;

            annotationHandlers.put(annotationTypeName, new LazyAnnotationHandler(descriptor));
        }
    }
    
    private class LazyAnnotationHandler implements AnnotationHandler {
        private final ActiveDescriptor<AnnotationHandler> descriptor;
        private AnnotationHandler handler;
        
        private LazyAnnotationHandler(ActiveDescriptor<AnnotationHandler> descriptor) {
            this.descriptor = descriptor;
        }
        
        private AnnotationHandler getHandler() {
            if (handler != null) return handler;
            
            handler = locator.getServiceHandle(descriptor).getService();
            return handler;
        }

        @Override
        public Class<? extends Annotation> getAnnotationType() {
            return getHandler().getAnnotationType();
        }

        @Override
        public HandlerProcessingResult processAnnotation(
                AnnotationInfo element) throws AnnotationProcessorException {
            return getHandler().processAnnotation(element);
        }

        @Override
        public Class<? extends Annotation>[] getTypeDependencies() {
            return getHandler().getTypeDependencies();
        }
    }
}
