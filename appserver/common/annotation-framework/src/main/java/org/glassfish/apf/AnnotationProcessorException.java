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

import com.sun.enterprise.util.LocalStringManagerImpl;

/**
 * Exception that denotes a warning or error condition in the 
 * annotation procesing tool
 *
 * @author Jerome Dochez
 */
public class AnnotationProcessorException extends Exception {
    
    final private String message;
    
    transient final private AnnotationInfo locator; // TODO if this class is meant for serialization, make sure all its constituents are serializable.
    
    boolean isFatal = false;

    final private static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(AnnotationProcessorException.class);


    /** 
     * Creats a new annotation exception
     * @param message describing the exception cause
     */
    public AnnotationProcessorException(String message) {
        this.message = message;
        this.locator = null;
    }
    
    /**
     * Creates a new annotation exception 
     * @param message describing the exception cause
     * @param locator gives information about the annotation and 
     * the annotated element which caused the exception
     */
    public AnnotationProcessorException(String message, AnnotationInfo locator) {
        this.message = message;
        this.locator = locator;
    }
    
    /**
     * Return a meaningful string explaining the exception cause
     * @return the exception reason
     */
    public String getMessage() {
        return message;
    }
    
    /**
     * Return information about the annotation and annotated element 
     * which caused the exception or null if it is not available.
     * @return the annotation info instance 
     */
    public AnnotationInfo getLocator() {
        return locator;
    }
    
    /**
     * @return a meaningful description
     */
    public String toString() {
        if (locator == null) {
            return message;
        } else {
            return localStrings.getLocalString("annotationprocessorexception.with.locator", "{0}. Related annotation information: {1}", message, locator);
        }
    }
    
    /**
     *
     * @return true if this exception was considered by the sender as being
     * fatal to the annotations processing(i.e. it should stop).
     */
    public boolean isFatal(){
        return isFatal;
    }
   
    /**
     * Sets wether is exception is considered as fatal to the annotation 
     * processing.
     * @param true if the annotation processing should stop
     */
     public void setFatal(boolean fatal){
         this.isFatal = fatal;
     }      
}
