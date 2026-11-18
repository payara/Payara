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
// Portions Copyright [2018-2026] Payara Foundation and/or its affiliates

package org.glassfish.apf;

import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.util.Collection;
import java.util.logging.Level;

/**
 * The annotation processor is the core engine to process annotations.
 * All the processing configuration (input classes, error handlers, etc...)
 * is provided by the ProcessingContext which can be either created from the
 * createContext method or through another mean. Once the ProcessingContext has
 * been initialized, it is passed to the process(ProcessingContext ctx) method which
 * triggers the annotation processing.
 *
 * Each class accessible from the ProcessingContext.getInputScanner instance, will be
 * scanned for annotations. Each annotation will then be passed to the registered
 * AnnotationHandler for processing.
 *
 * Even without a scanner, classes can be processed through the
 * process(ProcessingContext, Collection) method.
 *
 * @author Jerome Dochez
 */
public interface AnnotationProcessor {

    /**
     * Creates a new empty ProcessingContext instance which can be configured
     * before invoking the process() method.
     * @return an empty ProcessingContext
     */
    ProcessingContext createContext();

    /**
     * Starts the annotation processing tool passing the processing context which
     * encapsulate all information necessary for the configuration of the tool.
     * @param ctx is the initialized processing context
     * @return the result of the annotations processing
     */
    ProcessingResult process(ProcessingContext ctx) throws AnnotationProcessorException;

    /**
     * Process a set of classes from the parameter list rather than from the
     * processing context. This allows the annotation handlers to call the
     * annotation processing tool when classes need to be processed in a
     * particular context rather than when they are picked up by the scanner.
     *
     * @param ctx the processing context
     * @param classes the classes to process
     * @return the processing result for such classes
     * @throws AnnotationProcessorException if handlers fail to process an annotation
     */
    ProcessingResult process(ProcessingContext ctx, Collection<ClassInfo> classes) throws AnnotationProcessorException;

    /**
     * Registers a new AnnotationHandler for a particular annotation type. New annotation handler
     * are pushed on a List of annotation handlers for that particular annotation type, the last
     * annotation handler to be registered will be invoked first and so on.
     * The annotation type handled by the AnnotationHandler instance is defined
     * by the getAnnotationType() method of the AnnotationHandler instance.
     *
     * @param handler the new annotation handler
     */
    void pushAnnotationHandler(AnnotationHandler handler);

    /**
     * @return the top annotation handler for a particular annotation type
     * @param type the annotation type
     */
    AnnotationHandler getAnnotationHandler(Class<? extends Annotation> type);

    /**
     * Unregisters the last annotation handler registered for an annotation type.
     * @param type the annotation type
     */
    void popAnnotationHandler(Class<? extends Annotation> type);

    /**
     * @return the most recent annotated element being processed of the given type, or null if
     * no element of that type is being processed
     */
    AnnotationTarget getLastAnnotatedElement(ElementType type);

    /**
     * Log a message on the default logger
     */
    void log(Level level, AnnotationInfo locator, String localizedMessage);
}
