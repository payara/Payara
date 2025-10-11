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
// Portions Copyright [2018-2020] Payara Foundation and/or affiliates

package org.glassfish.apf.impl;

import org.glassfish.apf.Scanner;
import java.util.Stack;
import java.util.logging.Level;
import org.glassfish.apf.*;
import org.glassfish.apf.context.AnnotationContext;
import org.glassfish.api.deployment.archive.ReadableArchive;

/**
 * Minimal implementation of the ProcessingContext interface
 *
 * @author Jerome ochez
 */
@Deprecated(forRemoval = true)
class ProcessingContextImpl implements ProcessingContext {
    
    protected AnnotationProcessor processor;
    protected Stack<AnnotatedElementHandler> handlers = new Stack<AnnotatedElementHandler>();
    protected Scanner scanner;
    protected ReadableArchive archive;

    /** Creates a new instance of ProcessingContextHelper */
    ProcessingContextImpl(AnnotationProcessor processor) {
        this.processor = processor;
    }
    
    @Override
    public AnnotationProcessor getProcessor() {
        return processor;
    }
        
    @Override
    public ReadableArchive getArchive() {
        return archive;    
    }

    @Override
    public void setArchive(ReadableArchive archive) {
        this.archive = archive;
    }

    @Override
    public void pushHandler(AnnotatedElementHandler handler) {
        if (handler instanceof AnnotationContext) {
            ((AnnotationContext) handler).setProcessingContext(this);
        }
        handlers.push(handler);
    }
    
    @Override
    public AnnotatedElementHandler getHandler() {
        if (handlers.isEmpty()) 
            return null;
        
        return handlers.peek();
    }
    
    @Override
    public AnnotatedElementHandler popHandler() {
        if (handlers.isEmpty()) 
            return null;
        
        return handlers.pop();
    }
        
    /** 
     * @return the previously set ClientContext casted to the requested
     * type if possible or throw an exception otherwise.
     */
    @Override
    public <U extends AnnotatedElementHandler> U getHandler(Class<U> contextType)
        throws ClassCastException {
        
        if (handlers.isEmpty()) 
            return null;
        if (AnnotationUtils.shouldLog("handler")) {
            AnnotationUtils.getLogger().log(Level.FINER, "Top handler is {0}", handlers.peek());
        }
        return contextType.cast(handlers.peek());
    }
    
    @Override
    public Scanner getProcessingInput() {
        return scanner;
    }
    
    @Override
    public void setProcessingInput(Scanner scanner) {
        this.scanner = scanner;
    }

    private ErrorHandler errorHandler = null;
    
    /** 
     * Sets the error handler for this processing context.
     */
    @Override
    public void setErrorHandler(ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }
    
    /**
     * @return the error handler for this processing context.
     */
    @Override
    public ErrorHandler getErrorHandler() {
        return errorHandler;
    }      
}
