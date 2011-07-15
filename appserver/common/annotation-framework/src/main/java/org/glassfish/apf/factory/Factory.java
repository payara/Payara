/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.apf.factory;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.IOException;

import org.glassfish.apf.Scanner;
import org.glassfish.apf.AnnotationProcessor;
import org.glassfish.apf.AnnotationHandler;
import org.glassfish.apf.impl.AnnotationProcessorImpl;
import org.glassfish.apf.impl.AnnotationUtils;
/**
 * The Factory is responsible for initializing a ready to use AnnotationProcessor. 
 * 
 * @author Jerome Dochez
 */
public abstract class Factory {
    
    private static Set<String> skipAnnotationClassList = null;
    private static final String SKIP_ANNOTATION_CLASS_LIST_URL =
        "skip-annotation-class-list";

    /** we do no Create new instances of Factory */
    protected Factory() {
    }

    /**
     * Return a empty AnnotationProcessor with no annotation handlers registered
     * @return initialized AnnotationProcessor instance
     */ 
    public static AnnotationProcessorImpl getDefaultAnnotationProcessor() {
        return new AnnotationProcessorImpl();        
    }    

    // initialize the list of class files we should skip annotation processing
    private synchronized static void initSkipAnnotationClassList() {
        if (skipAnnotationClassList == null) {
            skipAnnotationClassList = new HashSet<String>();
            InputStream is = null;
            BufferedReader bf = null;
            try {
                is = AnnotationProcessorImpl.class.getClassLoader().getResourceAsStream(SKIP_ANNOTATION_CLASS_LIST_URL);
                if (is==null) {
                    AnnotationUtils.getLogger().log(Level.FINE, "no annotation skipping class list found");
                    return;
                }
                bf = new BufferedReader(new InputStreamReader(is));
                String className;
                while ( (className = bf.readLine()) != null ) {
                    skipAnnotationClassList.add(className.trim());
                }
            } catch (IOException ioe) {
                AnnotationUtils.getLogger().log(Level.WARNING, 
                    ioe.getMessage(), ioe);
            } finally {
                if (is != null) {
                    try {
                        is.close(); 
                    } catch (IOException ioe2) {
                        // ignore
                    }
                }
                if (bf != null) {
                    try {
                        bf.close();
                    } catch (IOException ioe2) {
                        // ignore
                    }
                }
            }
        }
    }

    // check whether a certain class can skip annotation processing
    public static boolean isSkipAnnotationProcessing(String cName) {
        if (skipAnnotationClassList == null) {
            initSkipAnnotationClassList();
        }
        return skipAnnotationClassList.contains(cName); 
    }

}
