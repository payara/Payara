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

package org.glassfish.apf;

import org.glassfish.apf.Scanner;
import java.util.Set;
import org.glassfish.api.deployment.archive.ReadableArchive;

/**
 * This interface defines the context for the annotation procesing
 * handler. There is only one context instance per AnnotationProcessor 
 * invocation.
 *
 * @author Jerome Dochez
 */
public interface ProcessingContext {
    
    /**
     * Returns the AnnotationProcessor instance this context is associated
     * with. 
     * @return annotation processor instance
     */
    public AnnotationProcessor getProcessor();    
    
    /**
     * Returns the Scanner implementation which is responsible for providing
     * access to all the .class files the processing tool needs to scan.
     * @return scanner instance 
     */
    public Scanner getProcessingInput();

    /**
     * Returns the module archive that can be used to load files/resources,
     *  that assist in the processing of annotations. Using the ClassLoader is
     * preferred, but not all files can be loaded by  it and this can be handy
     * in those cases.
     *@return module archive
     */
    public ReadableArchive getArchive();

    public void setArchive(ReadableArchive archive);
    
    /**
     * Sets the Scanner implementation which is responsible for accessing 
     * all the .class files the AnnotationProcessor should process.
     */
    public void setProcessingInput(Scanner scanner);
    
    /**
     * Push a new handler on the stack of handlers. This handler will receive 
     * all the AnnotedElementHandler events until it is removed from the stack 
     * with a popHandler() call. 
     * @param handler the new events handler.
     */    
    public void pushHandler(AnnotatedElementHandler handler);
    
    /** 
     * Return the current handler (if any) receving all the annotated elements
     * start and stop events.
     * @return the top handler
     */
    public AnnotatedElementHandler getHandler();    
        
    /** 
     * Removes the top handler
     * @return the removed handler
     */
    public AnnotatedElementHandler popHandler();
    
    /** 
     * Return the top handler casted to the requested handler type
     * @param requested handler type 
     * @return the top handler
     * @throws ClassCastException if the top handler cannot be casted to 
     * the requested handler type.
     */
    public <U extends AnnotatedElementHandler> U getHandler(Class<U> handlerType)
        throws ClassCastException;

    /**
     * Sets the ErrorHandler instance for all errors/warnings that may be raised
     * during the annotation processing.
     * @param handler the annotation handler
     */
    public void setErrorHandler(ErrorHandler errorHandler);
    
    /**
     * Return the error handler for this processing context.
     * @return the error handler
     */
    public ErrorHandler getErrorHandler();
            
}
