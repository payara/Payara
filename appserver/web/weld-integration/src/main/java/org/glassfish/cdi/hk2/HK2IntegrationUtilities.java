/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.cdi.hk2;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Named;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.glassfish.hk2.api.Injectee;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.InjecteeImpl;
import org.glassfish.hk2.utilities.NamedImpl;

/**
 * Integration utilities
 * 
 * @author jwells
 *
 */
public class HK2IntegrationUtilities {
    private final static String APP_SL_NAME = "java:app/hk2/ServiceLocator";
    
    /**
     * This method returns the proper ApplicationServiceLocator
     * to use for CDI integration
     * 
     * @return The application service loctor (will not return null)
     * @throws AssertionError if no ServiceLocator can be found
     */
    public static ServiceLocator getApplicationServiceLocator() {
        try {
            Context ic = new InitialContext();
            
            return (ServiceLocator) ic.lookup(APP_SL_NAME);
        }
        catch (NamingException ne) {
            return null;
        }
    }
    
    private static Set<Annotation> getHK2Qualifiers(InjectionPoint injectionPoint) {
        Set<Annotation> setQualifiers = injectionPoint.getQualifiers();
        
        Set<Annotation> retVal = new HashSet<Annotation>();
        
        for (Annotation anno : setQualifiers) {
            if (anno.annotationType().equals(Default.class)) continue;
            
            if (anno.annotationType().equals(Named.class)) {
                Named named = (Named) anno;
                if ("".equals(named.value())) {
                    Annotated annotated = injectionPoint.getAnnotated();
                    if (annotated instanceof AnnotatedField) {
                        AnnotatedField<?> annotatedField = (AnnotatedField<?>) annotated;
                        
                        Field field = annotatedField.getJavaMember();
                        anno = new NamedImpl(field.getName());
                    }
                    
                }
                
            }
            
            retVal.add(anno);
        }
        
        return retVal;
    }
    
    public static Injectee convertInjectionPointToInjectee(InjectionPoint injectionPoint) {
        InjecteeImpl retVal = new InjecteeImpl(injectionPoint.getType());
        
        retVal.setRequiredQualifiers(getHK2Qualifiers(injectionPoint));
        retVal.setParent((AnnotatedElement) injectionPoint.getMember());  // Also sets InjecteeClass
        
        Annotated annotated = injectionPoint.getAnnotated();
        if (annotated instanceof AnnotatedField) {
            retVal.setPosition(-1);
        }
        else {
            AnnotatedParameter<?> annotatedParameter = (AnnotatedParameter<?>) annotated;
            retVal.setPosition(annotatedParameter.getPosition());
        }
        
        return retVal;
    }

}
