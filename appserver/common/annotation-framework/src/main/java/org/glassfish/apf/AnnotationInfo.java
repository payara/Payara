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

package org.glassfish.apf;

import java.lang.reflect.AnnotatedElement;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;

import com.sun.enterprise.util.LocalStringManagerImpl;

/**
 * Instances encapsulate all information necessary for an AnnotationHandler 
 * to process an annotation. In particular, instances of this class provide 
 * access to :
 *
 * <p>
 * <li> the Annotation instance 
 * <li> the ProcessingContext of the tool
 * <li> the AnnotatedElement which is a reference to the annotation element 
 * (Type, Method...).
 * </p>
 *
 * @see java.lang.annotation.Annotation, java.lang.reflect.AnnotatedElement
 *
 * @author Jerome Dochez
 *
 */
public class AnnotationInfo {

    final private static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(AnnotationInfo.class);

    // the annotated element 
    final private AnnotatedElement annotatedElement;

    // the annotation   
    final private Annotation annotation;
    
    // the processing context
    final private ProcessingContext context;
    
    // the element type
    final private ElementType type;

    /**
     * Creates a new instance of AnnotationInfo with all the information 
     * necessary to process an annotation.
     *
     * @param context the annotation processor processing context
     * @param element the annotated element
     * @param annotation the annotation 
     */
    public AnnotationInfo(ProcessingContext context, AnnotatedElement element, 
            Annotation annotation, ElementType type) {
        
        this.context = context;
        this.annotatedElement = element;
        this.annotation = annotation;
        this.type = type;
    }

    /**
     * @return the annotated element instance
     */
    public AnnotatedElement getAnnotatedElement() {
        return annotatedElement;
    }

    /**
     * @return the annotation instance
     */
    public Annotation getAnnotation() {

        return annotation;
    }

    /**
     * @return the processing context
     */
    public ProcessingContext getProcessingContext() {
        return context;
    }
    
    /**
     * @return the annotated element ElementType
     */
    public ElementType getElementType() {
        return type;
    }

    public String toString() {
        return localStrings.getLocalString("annotatedinfo.string", "annotation [{0}] on annotated element [{1}] of type [{2}]", annotation, annotatedElement, type); 
    }
}
