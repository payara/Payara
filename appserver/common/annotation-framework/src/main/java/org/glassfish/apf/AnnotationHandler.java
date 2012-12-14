/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.apf;

import org.jvnet.hk2.annotations.Contract;

import java.lang.annotation.Annotation;

/**
 * This interface defines the contract for annotation handlers 
 * and the annotation processing engine. Each annotation handler
 * is registered for a particular annotation type and will be 
 * called by the engine when such annotation type is encountered.
 *
 * The AnnotationHandler is a stateless object, no state should 
 * be stored, instead users should use the ProcessingContext.
 *
 * Annotation can be defined or processed in random orders on a 
 * particular type, however, a particular annotation may need 
 * other annotation to be processed before itself in order to be 
 * processed successfully. An annotation type can indicate through
 * the @see getAnnotations() method which annotation types should 
 * be processed before itself.
 *
 * Each implementation of this interface must specify the annotation that it can handle using
 * {@link AnnotationHandlerFor} annotation.
 *
 * @author Jerome Dochez
 */
@Contract
public interface AnnotationHandler {
    public final static String ANNOTATION_HANDLER_METADATA = "AnnotationHandlerFor";
    
    /**
     * @return the annotation type this annotation handler is handling
     */
    public Class<? extends Annotation> getAnnotationType();
    
    /**
     * Process a particular annotation which type is the same as the 
     * one returned by @see getAnnotationType(). All information 
     * pertinent to the annotation and its context is encapsulated 
     * in the passed AnnotationInfo instance.
     * 
     * @param element the annotation information
     */
    public HandlerProcessingResult processAnnotation(AnnotationInfo element)
        throws AnnotationProcessorException;
    
    /**
     * @return an array of annotation types this annotation handler would 
     * require to be processed (if present) before it processes it's own 
     * annotation type.
     */
    public Class<? extends Annotation>[] getTypeDependencies();
        
}
