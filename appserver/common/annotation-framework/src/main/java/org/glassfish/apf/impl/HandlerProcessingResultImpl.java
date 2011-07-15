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

import java.util.Map;
import java.util.HashMap;
import java.lang.annotation.Annotation;

import org.glassfish.apf.ResultType;
import org.glassfish.apf.HandlerProcessingResult;
import org.glassfish.apf.AnnotationHandler;

/**
 *
 * @author dochez
 */
public class HandlerProcessingResultImpl implements HandlerProcessingResult {
    
    Map<Class<? extends Annotation>,ResultType> results;
    ResultType overallResult = ResultType.UNPROCESSED;
    
    /**
     * Creates a new instance of HandlerProcessingResultImpl 
     */
    public HandlerProcessingResultImpl(Map<Class<? extends Annotation>, ResultType> results) {
        this.results = results;
    }
    
    public HandlerProcessingResultImpl() {        
        results = new HashMap<Class<? extends Annotation>, ResultType>();
    }
    
    public static HandlerProcessingResultImpl getDefaultResult(Class<? extends Annotation> annotationType, ResultType result) {
        
        HandlerProcessingResultImpl impl = new HandlerProcessingResultImpl();
        impl.results.put(annotationType, result);
        impl.overallResult = result;
        return impl;                
    }
    
    public Map<Class<? extends Annotation>,ResultType> processedAnnotations() {
        return results;
    }
    
    public void addResult(Class<? extends Annotation> annotationType, ResultType result) {
        if (result.compareTo(overallResult)>0) {
            overallResult = result;
        }
        results.put(annotationType, result);
    }
    
    public void addAll(HandlerProcessingResult result) {
         if (result == null) {
             return;
         }
         if (result.getOverallResult().compareTo(overallResult)>0) {
            overallResult = result.getOverallResult();
        }
        results.putAll(result.processedAnnotations());
    }
    
    public ResultType getOverallResult(){
        return overallResult;
    }
  
}
