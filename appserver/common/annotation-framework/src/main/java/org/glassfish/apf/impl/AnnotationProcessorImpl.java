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
// Portions Copyright 2016-2026 Payara Foundation and/or its affiliates

package org.glassfish.apf.impl;

import fish.payara.nucleus.hotdeploy.AnnotationProcessorState;
import fish.payara.nucleus.hotdeploy.ApplicationState;
import org.glassfish.apf.AnnotatedElementHandler;
import org.glassfish.apf.AnnotationHandler;
import org.glassfish.apf.AnnotationInfo;
import org.glassfish.apf.AnnotationProcessor;
import org.glassfish.apf.AnnotationProcessorException;
import org.glassfish.apf.ComponentInfo;
import org.glassfish.apf.HandlerProcessingResult;
import org.glassfish.apf.ProcessingContext;
import org.glassfish.apf.ProcessingResult;
import org.glassfish.apf.ResultType;
import org.glassfish.apf.Scanner;
import org.glassfish.internal.deployment.ClassHierarchy;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.MethodParameterInfo;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Processes the annotations of the classes a {@link Scanner} selected, reading them from the Jandex
 * index instead of loading the classes. For each class the annotations of the class itself (declared
 * and inherited), of its fields, public constructors and methods including their parameters, and the
 * class-level annotations of its superclasses are dispatched to the registered
 * {@link AnnotationHandler}s, honoring the dependencies a handler declares on other annotations.
 *
 * @author Jerome Dochez
 */
public class AnnotationProcessorImpl implements AnnotationProcessor {

    private static final String PACKAGE_INFO = "package-info";

    private AnnotationProcessorImpl delegate;
    private final Map<String, List<AnnotationHandler>> handlers = new HashMap<>();
    private int errorCount;
    private final Logger logger;
    private final Deque<StackElement> annotatedElements = new LinkedList<>();
    private final Set<String> visitedPackages = new HashSet<>();

    /** Creates a new instance of AnnotationProcessorImpl */
    public AnnotationProcessorImpl() {
        logger = AnnotationUtils.getLogger();
    }

    public void setDelegate(AnnotationProcessorImpl delegate) {
        this.delegate = delegate;
    }

    @Override
    public ProcessingContext createContext() {
        ProcessingContext ctx = new ProcessingContextImpl(this);
        ctx.setErrorHandler(new DefaultErrorHandler());
        return ctx;
    }

    /**
     * Log a message on the default logger
     */
    @Override
    public void log(Level level, AnnotationInfo locator, String localizedMessage) {
        if (logger != null && logger.isLoggable(level)) {
            if (locator != null) {
                logger.log(level, AnnotationUtils.getLocalString("enterprise.deployment.annotation.error",
                        "{2}\n symbol: {0}\n location: {1}",
                        locator.getAnnotationTypeName(), locator.getAnnotatedElement(), localizedMessage));
            } else {
                logger.log(level, localizedMessage);
            }
        }
    }

    /**
     * Starts the annotation processing tool passing the processing context which
     * encapsulate all information necessary for the configuration of the tool.
     * @param ctx is the initialised processing context
     * @return the result of the annotations processing
     */
    @Override
    public ProcessingResult process(ProcessingContext ctx) throws AnnotationProcessorException {
        ApplicationState state = ctx.getArchive().getExtraData(ApplicationState.class);
        Optional<AnnotationProcessorState> processorState = Optional.empty();
        if (state != null) {
            processorState = state.getProcessingState(ctx);
        }

        Scanner<Object> scanner = ctx.getProcessingInput();
        ProcessingResultImpl result;
        errorCount = 0;

        if (state == null) {
            result = new ProcessingResultImpl();
            for (ClassInfo c : scanner.getElements()) {
                result.add(process(ctx, c));
            }
        } else if (state.isInactive()) {
            result = new ProcessingResultImpl();
            for (ClassInfo c : scanner.getElements()) {
                result.add(process(ctx, c));
            }
            processorState.ifPresent(s -> s.setProcessingResult(result));
        } else {
            result = processorState.get().getProcessingResult(ProcessingResultImpl.class);
            for (ClassInfo modifiedClass : scanner.getElements(state.getClassesChanged().keySet())) {
                result.add(process(ctx, modifiedClass));
            }
        }

        return result;
    }

    @Override
    public ProcessingResult process(ProcessingContext ctx, Collection<ClassInfo> classes)
            throws AnnotationProcessorException {
        ProcessingResultImpl result = new ProcessingResultImpl();
        for (ClassInfo c : classes) {
            result.add(process(ctx, c));
        }
        return result;
    }

    private ProcessingResult process(ProcessingContext ctx, ClassInfo c) throws AnnotationProcessorException {
        Scanner scanner = ctx.getProcessingInput();
        IndexView index = ctx.getIndex();
        ProcessingResultImpl result = new ProcessingResultImpl();

        // let's see first if this package is new to us and annotated.
        String packageName = c.name().packagePrefix();
        if (packageName != null && visitedPackages.add(packageName)) {
            ClassInfo packageInfo = index.getClassByName(DotName.createSimple(packageName + "." + PACKAGE_INFO));
            if (packageInfo != null) {
                result.add(packageInfo, processAnnotations(ctx, ElementType.PACKAGE, packageInfo,
                        visible(packageInfo.declaredAnnotations())));
            }
        }

        ComponentInfo info = scanner.getComponentInfo(c);

        // process the class itself.
        AnnotatedElementHandler handler = ctx.getHandler();
        logStart(handler, ElementType.TYPE, c);
        result.add(c, processAnnotations(ctx, c, ClassHierarchy.classAnnotations(index, c)));

        // now dive into the fields.
        for (FieldInfo field : info.getFields()) {
            result.add(field, processAnnotations(ctx, ElementType.FIELD, field, visible(field.declaredAnnotations())));
        }

        // constructors...
        for (MethodInfo constructor : info.getConstructors()) {
            logStart(ctx.getHandler(), ElementType.CONSTRUCTOR, constructor);
            result.add(constructor, processAnnotations(ctx, constructor, visible(constructor.declaredAnnotations())));
            processParameters(ctx, constructor);
            logEnd(ctx.getHandler(), ElementType.CONSTRUCTOR, constructor);
        }

        for (MethodInfo method : info.getMethods()) {
            logStart(ctx.getHandler(), ElementType.METHOD, method);
            result.add(method, processAnnotations(ctx, method, visible(method.declaredAnnotations())));
            processParameters(ctx, method);
            logEnd(ctx.getHandler(), ElementType.METHOD, method);
        }

        // class-level annotations of the superclasses, with the superclass as the annotated element
        for (ClassInfo superClass = ClassHierarchy.superClass(index, c); superClass != null;
             superClass = ClassHierarchy.superClass(index, superClass)) {
            result.add(c, processAnnotations(ctx, superClass, ClassHierarchy.classAnnotations(index, superClass)));
        }

        logEnd(ctx.getHandler(), ElementType.TYPE, c);

        return result;
    }

    private HandlerProcessingResult processParameters(ProcessingContext ctx, MethodInfo method)
            throws AnnotationProcessorException {
        HandlerProcessingResultImpl result = new HandlerProcessingResultImpl();
        for (MethodParameterInfo parameter : method.parameters()) {
            logStart(ctx.getHandler(), ElementType.PARAMETER, parameter);
            for (AnnotationInstance annotation : visible(parameter.declaredAnnotations())) {
                AnnotationInfo info = new AnnotationInfo(ctx, parameter, annotation, ElementType.PARAMETER);
                process(ctx, info, result);
                dumpProcessingResult(result);
            }
            logEnd(ctx.getHandler(), ElementType.PARAMETER, parameter);
        }
        return result;
    }

    private HandlerProcessingResult processAnnotations(ProcessingContext ctx, ElementType type,
                                                       AnnotationTarget element, List<AnnotationInstance> annotations)
            throws AnnotationProcessorException {
        AnnotatedElementHandler handler = ctx.getHandler();
        logStart(handler, type, element);
        HandlerProcessingResult result = processAnnotations(ctx, element, annotations);
        logEnd(handler, type, element);

        dumpProcessingResult(result);

        return result;
    }

    private HandlerProcessingResult processAnnotations(ProcessingContext ctx, AnnotationTarget element,
                                                       List<AnnotationInstance> annotations)
            throws AnnotationProcessorException {
        HandlerProcessingResultImpl result = new HandlerProcessingResultImpl();
        for (AnnotationInstance annotation : annotations) {
            // initialize the result...
            AnnotationInfo subElement = new AnnotationInfo(ctx, element, annotation, getTopElementType());
            if (!result.processedAnnotations().containsKey(annotation.name().toString())) {
                process(ctx, subElement, result);
            } else if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINER, "Annotation {0} already processed", annotation.name());
            }
        }
        return result;
    }

    private void process(ProcessingContext ctx, AnnotationInfo element, HandlerProcessingResultImpl result)
            throws AnnotationProcessorException {
        AnnotationInstance annotation = element.getAnnotation();
        String annotationType = annotation.name().toString();
        if (logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER, "Annotation : {0} delegate = {1}", new Object[]{annotationType, delegate});
        }
        result.addResult(annotationType, ResultType.UNPROCESSED);

        // we ignore all java.* annotations
        if (annotationType.startsWith("java.lang")) {
            return;
        }

        List<AnnotationHandler> annotationHandlers = handlers.get(annotationType);
        if (annotationHandlers != null) {
            for (AnnotationHandler handler : annotationHandlers) {

                // here we need to be careful, we are ready to invoke a handler
                // to process a particular annotation type. However, this handler
                // may have defined a list of annotations that should be processed
                // (if present on the annotated element) before itself.
                // do this check and process those annotations first.
                Class<? extends Annotation>[] dependencies = handler.getTypeDependencies();
                if (dependencies != null) {
                    AnnotationTarget ae = element.getAnnotatedElement();
                    for (Class<? extends Annotation> dependency : dependencies) {
                        AnnotationInstance depAnnotation = ClassHierarchy.annotation(ctx.getIndex(), ae, dependency);
                        if (depAnnotation != null) {
                            ResultType resultType = result.processedAnnotations().get(dependency.getName());
                            if (resultType == null || resultType == ResultType.UNPROCESSED) {
                                // annotation is present, process it.
                                AnnotationInfo info = new AnnotationInfo(ctx, ae, depAnnotation, getTopElementType());
                                process(ctx, info, result);
                            }
                        }
                    }
                }

                // at this point, all annotation that I declared depending on
                // are processed
                HandlerProcessingResult processingResult;
                try {
                    processingResult = handler.processAnnotation(element);
                } catch (AnnotationProcessorException ape) {
                    // I am logging this exception
                    log(Level.SEVERE, ape.getLocator(), ape.getMessage());

                    // I am not throwing the exception unless it is fatal so annotation
                    // processing can continue and we have a chance to report all
                    // errors.
                    if (ape.isFatal()) {
                        throw ape;
                    }

                    if (++errorCount > 100) {
                        throw new AnnotationProcessorException(AnnotationUtils.getLocalString(
                                "enterprise.deployment.annotation.toomanyerror",
                                "Too many errors, annotation processing abandoned."));
                    }

                    processingResult = HandlerProcessingResultImpl.getDefaultResult(annotationType, ResultType.FAILED);
                } catch (Throwable e) {
                    AnnotationProcessorException ape = new AnnotationProcessorException(e.getMessage(), element);
                    ape.initCause(e);
                    throw ape;
                }
                result.addAll(processingResult);
            }
        } else if (delegate != null) {
            delegate.process(ctx, element, result);
        } else {
            ctx.getErrorHandler().fine(new AnnotationProcessorException("No handler defined for " + annotationType));
        }
    }

    private void dumpProcessingResult(HandlerProcessingResult result) {
        if (result == null || !logger.isLoggable(Level.FINER)) {
            return;
        }
        for (Map.Entry<String, ResultType> element : result.processedAnnotations().entrySet()) {
            logger.log(Level.FINER, "Annotation {0} : {1}", new Object[]{element.getKey(), element.getValue()});
        }
    }

    @Override
    public void pushAnnotationHandler(AnnotationHandler handler) {
        pushAnnotationHandler(handler.getAnnotationType().getName(), handler);
    }

    /**
     * This method is similar to {@link #pushAnnotationHandler(AnnotationHandler)} except that
     * it takes an additional String type argument which allows us to avoid extracting the information from the
     * AnnotationHandler. Calling the AnnotationHandler can lead to its instantiation where as the annotation
     * that a handler is responsible for handling is a metadata that can be statically extracted. This allows us to
     * build more lazy systems.
     */
    public void pushAnnotationHandler(String type, AnnotationHandler handler) {
        handlers.computeIfAbsent(type, k -> new ArrayList<>()).add(handler);
    }

    @Override
    public void popAnnotationHandler(Class<? extends Annotation> type) {
        List<AnnotationHandler> currentHandlers = handlers.get(type.getName());
        if (currentHandlers != null) {
            currentHandlers.remove(currentHandlers.size() - 1);
        }
    }

    @Override
    public AnnotationHandler getAnnotationHandler(Class<? extends Annotation> type) {
        List<AnnotationHandler> currentHandlers = handlers.get(type.getName());
        if (currentHandlers != null && !currentHandlers.isEmpty()) {
            return currentHandlers.get(0);
        }
        return null;
    }

    /**
     * @return the last element pushed on the stack which ElementType was
     * the one passed or null if no stack element is of the given type.
     */
    @Override
    public AnnotationTarget getLastAnnotatedElement(ElementType type) {
        for (StackElement e : annotatedElements) {
            if (e.getElementType().equals(type)) {
                return e.getAnnotatedElement();
            }
        }
        return null;
    }

    Deque<StackElement> getStack() {
        return annotatedElements;
    }

    private void logStart(AnnotatedElementHandler handler, ElementType type, AnnotationTarget c)
            throws AnnotationProcessorException {
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(type + " START : " + c);
        }

        // push it to our annotated element stack
        annotatedElements.push(new StackElement(type, c));
        if (delegate != null) {
            delegate.getStack().push(new StackElement(type, c));
        }

        if (handler != null) {
            handler.startElement(type, c);
        }
    }

    private void logEnd(AnnotatedElementHandler handler, ElementType type, AnnotationTarget c)
            throws AnnotationProcessorException {
        if (logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER, "{0} END : {1}", new Object[]{type, c});
        }

        // pop it from our annotated element stack
        annotatedElements.pop();
        if (delegate != null) {
            delegate.getStack().pop();
        }

        if (handler != null) {
            handler.endElement(type, c);
        }
    }

    /**
     * @return the top annotated elements stack element type
     */
    private ElementType getTopElementType() {
        StackElement top = annotatedElements.peek();
        return top == null ? null : top.getElementType();
    }

    /**
     * @return the annotations with runtime retention, the only ones reflection would have returned
     */
    private static List<AnnotationInstance> visible(Collection<AnnotationInstance> annotations) {
        List<AnnotationInstance> result = new ArrayList<>(annotations.size());
        for (AnnotationInstance annotation : annotations) {
            if (annotation.runtimeVisible()) {
                result.add(annotation);
            }
        }
        return result;
    }
}
