/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.deployment.annotation.introspection;

import com.sun.enterprise.deployment.annotation.factory.SJSASFactory;
import javax.inject.Inject;
import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PostConstruct;
import javax.inject.Singleton;
import java.util.Set;


/**
 * This class contains the list of all annotations types name 
 * which can be present at the class level (Type.TYPE).  
 *
 * @author Jerome Dochez
 */
@Service(name="default")
@Singleton
public class DefaultAnnotationScanner implements AnnotationScanner, 
    PostConstruct {

    @Inject 
    SJSASFactory factory;
    
    private Set<String> annotations=null;
    private Set<String> annotationsMetaDataComplete=null;
    
    /**
     * Test if the passed constant pool string is a reference to 
     * a Type.TYPE annotation of a J2EE component
     *
     * @String the constant pool info string 
     * @return true if it is a J2EE annotation reference
     */
    public boolean isAnnotation(String value) {
        return annotations.contains(value);
    }
    
    public void postConstruct() {
        annotations = factory.getAnnotations(false);
        annotationsMetaDataComplete = factory.getAnnotations(true);
    }

    public Set<String> getAnnotations(boolean isMetaDataComplete) {
        if (!isMetaDataComplete) {
            return AbstractAnnotationScanner.constantPoolToFQCN(annotations);
        } else {
            return AbstractAnnotationScanner.constantPoolToFQCN(annotationsMetaDataComplete);
        }
    }

    @Override
    public Set<String> getAnnotations() {
        return getAnnotations(false);
    }
}
