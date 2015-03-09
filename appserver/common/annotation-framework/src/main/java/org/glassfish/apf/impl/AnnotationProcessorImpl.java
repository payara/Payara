/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2015 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.apf.impl;

import java.util.EmptyStackException;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Stack;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.util.logging.Logger;

import org.glassfish.apf.ProcessingContext;
import org.glassfish.apf.AnnotationProcessor;
import org.glassfish.apf.AnnotationInfo;
import org.glassfish.apf.AnnotationProcessorException;
import org.glassfish.apf.AnnotationHandler;
import org.glassfish.apf.AnnotatedElementHandler;
import org.glassfish.apf.ComponentInfo;
import org.glassfish.apf.ResultType;
import org.glassfish.apf.HandlerProcessingResult;
import org.glassfish.apf.ProcessingResult;
import org.glassfish.apf.Scanner;
import java.util.logging.Level;


/**
 *
 * @author dochez
 */
public class AnnotationProcessorImpl implements AnnotationProcessor {
    
    AnnotationProcessorImpl delegate;
    Map<String, List<AnnotationHandler>> handlers =
            new HashMap<String, List<AnnotationHandler>>();
    
    int errorCount;
    Logger logger;
    Stack<StackElement> annotatedElements = new Stack<StackElement>();
    Set<Package> visitedPackages = new HashSet<Package>();
    
    /** Creates a new instance of AnnotationProcessorImpl */
    public AnnotationProcessorImpl() {
        logger = AnnotationUtils.getLogger();
    }
    
    public void setDelegate(AnnotationProcessorImpl delegate) {
        this.delegate = delegate;
    }    
    public ProcessingContext createContext() {
        ProcessingContext ctx = new ProcessingContextImpl(this);
        ctx.setErrorHandler(new DefaultErrorHandler());
        return ctx;
    }
    
    /**
     * Log a message on the default logger
     */
    public void log(Level level, AnnotationInfo locator, String localizedMessage){
        if (logger!=null && logger.isLoggable(level)){
            if (locator!=null){
                logger.log(level, AnnotationUtils.getLocalString(
                    "enterprise.deployment.annotation.error",
                    "{2}\n symbol: {0}\n location: {1}",
                    new Object[] { locator.getAnnotation().annotationType().getName(), locator.getAnnotatedElement(), localizedMessage}));
            } else{
                logger.log(level, localizedMessage);
            }
        }
    }
    
    /**
     * Starts the annotation processing tool passing the processing context which 
     * encapuslate all information necessary for the configuration of the tool. 
     * @param ctx is the initialized processing context
     * @return the result of the annoations processing
     */    
    public ProcessingResult process(ProcessingContext ctx)
        throws AnnotationProcessorException
    {
        
        Scanner<Object> scanner = ctx.getProcessingInput();
        ProcessingResultImpl result = new ProcessingResultImpl();
        errorCount=0;
        
        for (Class c : scanner.getElements()) {
            
            result.add(process(ctx, c));          
        }
        return result;
    }
    
    /**
     * Process a set of classes from the parameter list rather than from the 
     * processing context. This allow the annotation handlers to call be the
     * annotation processing tool when classes need to be processed in a 
     * particular context rather than when they are picked up by the scanner.
     * 
     * @param ctx the processing context
     * @param classes the list of classes to process
     * @return the processing result for such classes
     * @throws AnnotationProcessorException if handlers fail to process 
     * an annotation
     */    
    public ProcessingResult process(ProcessingContext ctx, Class[] classes)
        throws AnnotationProcessorException {
        
        ProcessingResultImpl result = new ProcessingResultImpl();
        for (Class c : classes) {
            result.add(process(ctx, c));
        }
        return result;
    }
    
    private ProcessingResult process(ProcessingContext ctx, Class c) 
        throws AnnotationProcessorException {
        
        Scanner scanner = ctx.getProcessingInput();
        ProcessingResultImpl result = new ProcessingResultImpl();
        
        // let's see first if this package is new to us and annotated.
        Package classPackage = c.getPackage();
        if (classPackage != null && visitedPackages.add(classPackage)) {
            // new package
            result.add(classPackage, 
                    processAnnotations(ctx, ElementType.PACKAGE, classPackage));
        }

        ComponentInfo info = null;
        try {
            info = scanner.getComponentInfo(c);
        } catch (NoClassDefFoundError err) {
            // issue 456: allow verifier to report this issue
            AnnotationProcessorException ape = 
                    new AnnotationProcessorException(
                            AnnotationUtils.getLocalString(
                                    "enterprise.deployment.annotation.classnotfounderror",
                                    "Class [ {0} ] not found. Error while loading [ {1} ]",
                                    new Object[]{err.getMessage(), c}));
            ctx.getErrorHandler().error(ape);
            // let's continue to the next class instead of aborting the whole 
            // annotation processing
            return result;
        }

        // process the class itself.
        AnnotatedElementHandler handler= ctx.getHandler();
        logStart(handler, ElementType.TYPE,c);
        result.add(c, processAnnotations(ctx, c));
        
        // now dive into the fields.
        for (Field field : info.getFields()) {
            result.add(field,processAnnotations(ctx, ElementType.FIELD, field));
        }
        
        // constructors...
        for (Constructor constructor : info.getConstructors()) {
            logStart(ctx.getHandler(), ElementType.CONSTRUCTOR, constructor);
            result.add(constructor, processAnnotations(ctx, constructor));
            
            // parameters
            processParameters(ctx, constructor.getParameterAnnotations());
            
            logEnd(ctx.getHandler(), ElementType.CONSTRUCTOR, constructor);
            
        }
        
        // methods...
        for (Method method : info.getMethods()) {
            logStart(ctx.getHandler(), ElementType.METHOD, method);
            result.add(method, processAnnotations(ctx, method));
            
            // parameters
            processParameters(ctx, method.getParameterAnnotations());
            
            logEnd(ctx.getHandler(), ElementType.METHOD, method);
        }
        
        // Because of annotation inheritance, we need to to travel to
        // the superclasses to ensure that annotations defined at the
        // TYPE level are processed at this component level.
        // Note : so far, I am ignoring the implemented interfaces
        Class currentClass = c.getSuperclass();
        while (currentClass!=null && !currentClass.equals(Object.class)) {
            // the trick is to add the results for this class, not
            // for the ones they are defined in...
            result.add(c, processAnnotations(ctx, currentClass));
            currentClass = currentClass.getSuperclass();
        }
        
        // end of class processing, we need to get the top handler
        // since it may have changed during the annotation processing
        logEnd(ctx.getHandler(), ElementType.TYPE, c);  
        
        return result;
    }
    
    private HandlerProcessingResult processParameters(ProcessingContext ctx, Annotation[][] parametersAnnotations)
    throws AnnotationProcessorException
    {

        HandlerProcessingResultImpl result = new HandlerProcessingResultImpl();
        
        // process the method parameters...
        for (Annotation[] parameterAnnotations : parametersAnnotations) {
            logStart(ctx.getHandler(), ElementType.PARAMETER, null);
            if (parameterAnnotations!=null) {
                for (Annotation annotation : parameterAnnotations) {
                    AnnotationInfo info = new AnnotationInfo(ctx, null, annotation, ElementType.PARAMETER);
                    process(ctx, info, result);
                    dumpProcessingResult(result);
                }
            }
            logEnd(ctx.getHandler(), ElementType.PARAMETER, null);
        }
        return result;        
    }
    
    private HandlerProcessingResult processAnnotations(ProcessingContext ctx, ElementType type, AnnotatedElement element) 
        throws AnnotationProcessorException
    {
        
        AnnotatedElementHandler handler = ctx.getHandler();
        logStart(handler, type, element);
        HandlerProcessingResult result = processAnnotations(ctx, element);
        logEnd(handler, type, element);

        dumpProcessingResult(result);                    
        
        return result;
    }
    
    private HandlerProcessingResult processAnnotations(ProcessingContext ctx, AnnotatedElement element)
        throws AnnotationProcessorException
    {
    
        HandlerProcessingResultImpl result= new HandlerProcessingResultImpl();
        try{
        for (Annotation annotation : element.getAnnotations()) {
            // initialize the result...
            AnnotationInfo subElement = new AnnotationInfo(ctx, element, annotation, getTopElementType());
            if (!result.processedAnnotations().containsKey(annotation.annotationType())) {
                process(ctx, subElement, result);
            } else {
                if (AnnotationUtils.shouldLog("annotation")) { 
                    logger.finer("Annotation " + annotation.annotationType() + " already processed");
                }
            }       
        } 
	} catch (ArrayStoreException e) {
	    logger.info("Exception " + e.toString()
		    + " encountered while processing annotaton for element "
		    + element + ". Message is: " + e.getMessage()
		    + ". Ignoring annotations and proceeding.");
	}
        return result;
    }
    
    private void process(ProcessingContext ctx, AnnotationInfo element, HandlerProcessingResultImpl result) 
        throws AnnotationProcessorException 
    {
        
        
        Annotation annotation = element.getAnnotation();
        if (AnnotationUtils.shouldLog("annotation")) {
            logger.finer("Annotation : " + annotation.annotationType().getName() + " delegate = " + delegate);
        }
        result.addResult(annotation.annotationType(), ResultType.UNPROCESSED);
        
        // we ignore all java.* annotations
        Package annPackage = annotation.annotationType().getPackage();
        if (annPackage != null && annPackage.getName().startsWith("java.lang"))
            return;
        
        List<AnnotationHandler> annotationHandlers = handlers.get(annotation.annotationType().getName());
        if (annotationHandlers!=null) {
            for (AnnotationHandler handler : annotationHandlers) {
                
                // here we need to be careful, we are ready to invoke a handler
                // to process a particular annotation type. However, this handler 
                // may have defined a list of annotations that should be processed 
                // (if present on the annotated element) before itself. 
                // do this check and process those annotations first.
                Class<? extends Annotation>[] dependencies = handler.getTypeDependencies();
                if (dependencies!=null) {
                    AnnotatedElement ae = element.getAnnotatedElement();
                    for (Class<? extends Annotation> annotationType : dependencies) {
                        Annotation depAnnotation = ae.getAnnotation(annotationType);
                        if (depAnnotation!=null) {                        
                            ResultType resultType = result.processedAnnotations().get(annotationType);
                            if (resultType==null || resultType==ResultType.UNPROCESSED){
                                // annotation is present, process it.
                                AnnotationInfo info = new AnnotationInfo(ctx, ae, depAnnotation, getTopElementType());
                                process(ctx, info, result);
                            }
                        }
                    }
                }
                
                // at this point, all annotation that I declared depending on
                // are processed
                HandlerProcessingResult processingResult = null;
                try {
                    processingResult = handler.processAnnotation(element);
                } catch(AnnotationProcessorException ape) {
                    // I am logging this exception
                    log(Level.SEVERE, ape.getLocator(), ape.getMessage());
                    
                    // I am not throwing the exception unless it is fatal so annotation
                    // processing can continue and we have a chance to report all 
                    // errors. 
                    if (ape.isFatal()) {
                        throw ape;
                    } 
                    
                    if (++errorCount>100){
                        throw new AnnotationProcessorException(
                                AnnotationUtils.getLocalString(
                                    "enterprise.deployment.annotation.toomanyerror",
                                    "Too many errors, annotation processing abandoned."));
                    }
                    
                    processingResult =
                        HandlerProcessingResultImpl.getDefaultResult(
                        annotation.annotationType(), ResultType.FAILED);
                } catch(Throwable e){
                    AnnotationProcessorException ape = new AnnotationProcessorException(e.getMessage(), element);
                    ape.initCause(e);
                    throw ape;
                }
                result.addAll(processingResult);
            }
        } else {
            if (delegate!=null) {
                delegate.process(ctx, element, result);
            } else {           
                ctx.getErrorHandler().fine(
                        new AnnotationProcessorException("No handler defined for " 
                            + annotation.annotationType()));
            }
        }
    }
    
    private void dumpProcessingResult(HandlerProcessingResult result) {

        if (result==null || !AnnotationUtils.shouldLog("annotation")) {
            return;
        }
   
        Map<Class<? extends Annotation>, ResultType> annotationResults = 
                result.processedAnnotations();
        for (Map.Entry<Class<? extends Annotation>, ResultType> element : annotationResults.entrySet()) {
            logger.finer("Annotation " + element.getKey() + " : " +
                    element.getValue());
        }
    }
    public void pushAnnotationHandler(AnnotationHandler handler) {
        
        String type = handler.getAnnotationType().getName();
        pushAnnotationHandler(type, handler);
    }

    /**
     * This method is similar to {@link #pushAnnotationHandler(AnnotationHandler)} except that
     * it takes an additional String type argument which allows us to avoid extracting the information from the
     * AnnotationHandler. Calling the AnnotationHandler can lead to its instantiation where as the annotation
     * that a handler is responsible for handling is a metadata that can be statically extracted. This allows us to
     * build more lazy systems.
     *
     * @param type
     * @param handler
     */
    public void pushAnnotationHandler(String type, AnnotationHandler handler) {
        List<AnnotationHandler> currentHandlers = handlers.get(type);
        if (currentHandlers==null) {
            currentHandlers = new ArrayList<AnnotationHandler>();
            handlers.put(type, currentHandlers);
        }
        currentHandlers.add(handler);
    }

    public void popAnnotationHandler(Class<? extends Annotation> type) {
        List<AnnotationHandler> currentHandlers = handlers.get(type.getName());
        if (currentHandlers!=null) {
            currentHandlers.remove(currentHandlers.size()-1);
        }        
    }    

    public AnnotationHandler getAnnotationHandler(Class<? extends Annotation> type) {
        List<AnnotationHandler> currentHandlers = handlers.get(type.getName());
        if (currentHandlers!=null && currentHandlers.size()>0) {
            return currentHandlers.get(0);
        }        
        return null;
    }      
    
    /**
     * @return the last element pushed on the stack which ElementType was
     * the one passed or null if no stack element is of the given type.
     */
    public AnnotatedElement getLastAnnotatedElement(ElementType type) {
        for (int i=annotatedElements.size();i!=0;i--) {
            StackElement e = annotatedElements.get(i - 1);
            if (e.getElementType().equals(type)) 
                return e.getAnnotatedElement();
        }
        return null;
    }    
    
    public Stack<StackElement> getStack() {
        return annotatedElements;
    }
    
    private void logStart(AnnotatedElementHandler handler, ElementType type, AnnotatedElement c) throws AnnotationProcessorException {
        
        if (AnnotationUtils.shouldLog("types")) {
            AnnotationUtils.getLogger().finer(type + " START : " + c);
        }
        
        // push it to our annotated element stack
        annotatedElements.push(new StackElement(type, c));
        if(delegate!=null) {
            delegate.getStack().push(new StackElement(type, c));
        }
        
        if (handler!=null) {
            handler.startElement(type, c);
        }   
    }
    
    private void logEnd(AnnotatedElementHandler handler, ElementType type, AnnotatedElement c) throws AnnotationProcessorException {
        
        if (AnnotationUtils.shouldLog("types")) {
            AnnotationUtils.getLogger().finer(type + " END : " + c);
        }
        
        // pop it from our annotated element stack
        annotatedElements.pop();
        if(delegate!=null) {
            delegate.getStack().pop();
        }
        
        if (handler!=null) {
            handler.endElement(type, c);
        }   
    }   
    
    /** 
     * @return the top annotated elements stack element type 
     */
    private ElementType getTopElementType() {
        try {
            StackElement top = annotatedElements.peek();
            return top.getElementType();
        } catch(EmptyStackException ex) {
            return null;
        }
    }
}
