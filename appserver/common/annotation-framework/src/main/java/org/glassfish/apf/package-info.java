/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

/**
 * Provides the classes necessary to process J2SE 1.5 annotations in the context 
 * of the J2EE application server. 
 * 
 * <p>
 * Annotations are defined by their annotation type. This tool assumes that 
 * annotation handlers will be registered to it to process a particular annotation 
 * type. These annotation handlers have no particular knowledge outside of the 
 * annotation they process and the annoted element on which the annotation was
 * defined. 
 * </p>
 * <p>
 * The AnnotationProcessor tool implementation is responsible for maintening a 
 * list of annotations handlers per annotation type. AnnotationHandler are added
 * to the tool through the pushAnnotationHandler and can be removed through the 
 * popAnnotationHandler. Alternatively, the Factory singleton can be used to get
 * an initialized AnnotationProcessor with all the default AnnotationHandler. 
 * </p>
 * <p>
 * The tool uses the ProcessingContext to have access to Class instances. Each 
 * class instance will be processed in order, and if annotations are present, the 
 * tool will also process Field, Constructor and Methods elements. Each time the 
 * annotation processor switches for one particular AnnotatedElement to another,
 * it will send start and stop events to any AnnotatedElementHandler interface 
 * implementation registered within the ProcessingContext. This allow client 
 * code to keep context information about the AnnotatedElements being 
 * processed since AnnotationHandler only know about the AnnotatedElement the 
 * annotation was defined on.
 * </p>
 * @since 9.0
 * @auther Jerome Dochez
 */
package org.glassfish.apf;
