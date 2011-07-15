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

package org.glassfish.apf.impl;

import org.glassfish.apf.ErrorHandler;
import org.glassfish.apf.AnnotationInfo;
import org.glassfish.apf.AnnotationProcessorException;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Default implementation of the ErrorHandler
 *
 * @author Jerome Dochez
 */
public class DefaultErrorHandler implements ErrorHandler {
    
    Logger logger;

    /**
     * Creates a new ErrorHandler with the default logger
     */
    public DefaultErrorHandler() {
        logger = AnnotationUtils.getLogger();
    }
    
    /**
     * Creates a new ErrorHandler with the provided looger;
     */
    public DefaultErrorHandler(Logger logger){
        this.logger = logger;
    }

    /**
     * Receive notication of a fine error message
     * @param ape The warning information
     * @throws any exception to stop the annotation processing 
     */ 
    public void fine(AnnotationProcessorException ape) throws
            AnnotationProcessorException {
        
        if (logger.isLoggable(Level.FINE)){
            AnnotationInfo info = ape.getLocator();
            if (info==null){
                logger.fine(ape.getMessage());
            } else{
                logger.fine(AnnotationUtils.getLocalString(
                    "enterprise.deployment.annotation.error",
                    "{2}\n symbol: {0}\n location: {1}\n\n",
                    new Object[] { info.getElementType(), info.getAnnotatedElement(), ape.getMessage()}));            
            }
        }
        
    }
    
    /**
     * Receive notification of a warning
     * @param ape The warning information
     * @throws any exception to stop the annotation processing 
     */
    public void warning(AnnotationProcessorException ape) throws
            AnnotationProcessorException {
        
        if (logger.isLoggable(Level.WARNING)){
            AnnotationInfo info = ape.getLocator();
            if (info==null){
                logger.warning(ape.getMessage());
            } else{
                logger.warning(AnnotationUtils.getLocalString(
                    "enterprise.deployment.annotation.error",
                    "{2}\n symbol: {0}\n location: {1}\n\n",
                    new Object[] { info.getElementType(), info.getAnnotatedElement(), ape.getMessage()}));            
            }
        }
    }
    
    /**
     * Receive notification of an error
     * @param ape The error information
     * @throws amy exception to stop the annotation processing
     */
    public void error(AnnotationProcessorException ape) throws 
            AnnotationProcessorException {
        
        AnnotationInfo info = ape.getLocator();
        if (info==null){
            logger.severe(ape.getMessage());
        } else{
            logger.severe(AnnotationUtils.getLocalString(
                "enterprise.deployment.annotation.error",
                "{2}\n symbol: {0} location: {1}\n\n",
                new Object[] { info.getElementType(), info.getAnnotatedElement(), ape.getMessage()}));            
        }
    }
}
