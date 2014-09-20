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

package com.sun.enterprise.tools.verifier.tests.ejb;

import java.util.*;
import java.lang.reflect.*;

/** 
 * Utility package for dealing with method descriptors from <method-permission>
 * element and <container-transaction> elements
 */
public class MethodUtils { 

    /** 
     * Add method name to vector
     *
     * @param v Vector to be added to
     * @param methods array of methods to be added to vector 
     *   
     */
    public static void addMethodNamesToVector(Vector<String> v, Method[] methods) {
        for (int i=0; i< methods.length; i++) {
            // add method name to vector
            v.addElement(methods[i].getName());
        }
    }
 
    /** 
     * Determine is method parameters are equal.
     *
     * @param v Vector to be added to
     * @param hMethods array of home interface methods to be added to vector 
     * @param rMethods array of remote interface methods to be added to vector 
     *   
     */
    public static void addMethodNamesToVector(Vector<String> v, Method[] hMethods, Method[] rMethods) {
        // add method names to vector for both home and remote interfaces
        addMethodNamesToVector(v,hMethods);
        addMethodNamesToVector(v,rMethods);
    }
 
   
    /** 
     * Determine is method parameters are equal.
     *
     * @param s1 array of parameters for method 
     * @param s2 array of parameters for method 
     *   
     * @return <code>boolean</code> the results for this parameter equality test
     */
    public static boolean stringArrayEquals(String[] s1, String[] s2) {
        if (s1 == null && s2 == null) {
            return true;
        }
        if (s1 == null && s2 != null) {
            return false;
        }
        if (s2 == null && s1 != null) {
            return false;
        }
        if (s1.length == s2.length) {
            for (int i = 0; i < s1.length; i++) {
                if (!s1[i].equals(s2[i])) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }
    
    /** returns true if method names, return types and parameters match. Otherwise
     * it returns false. */
    public static boolean methodEquals(Method classMethod, Method intfMethod) {
        return classMethod.getName().equals(intfMethod.getName()) &&
                intfMethod.getReturnType().isAssignableFrom(classMethod.getReturnType()) &&
                Arrays.equals(classMethod.getParameterTypes(), intfMethod.getParameterTypes());
    }

}
