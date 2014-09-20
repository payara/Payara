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

package com.sun.enterprise.tools.verifier;

import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.util.ArrayList;

/**
 * This class represents the fault location of the failed verifier assertions.
 * It can be used by IDE's to open the faulty source file in the editor.
 * 
 * The location object is initialized as part of Result initialization and so it
 * is available in every assertion. Just like every assertion has to populate
 * the result object, it will have to populate this location. 
 * For now, few assertions in package 'tests.ejb.ejb30' are modified to provide 
 * the location information.
 * 
 * @author Vikas Awasthi
 */
public class FaultLocation {

    private Class faultyClass;
    private Method faultyMethod;
    private Field faultyField;
    private String faultyClassName;
    private String faultyMethodName;
    private String[] faultyMethodParams;

    public Class getFaultyClass() {
        return faultyClass;
    }

    public void setFaultyClass(Class faultyClass) {
        this.faultyClass = faultyClass;
    }

    public Method getFaultyMethod() {
        return faultyMethod;
    }

    public void setFaultyMethod(Method faultyMethod) {
        this.faultyMethod = faultyMethod;
    }

    public String getFaultyClassName() {
        if(faultyClassName==null && faultyClass != null)
            faultyClassName = faultyClass.getName();
        return faultyClassName;
    }

    public void setFaultyClassName(String faultyClassName) {
        this.faultyClassName = faultyClassName;
    }

    public String getFaultyMethodName() {
        if(faultyMethodName==null && faultyMethod != null)
            faultyMethodName = faultyMethod.getName();
        return faultyMethodName;
    }

    public void setFaultyMethodName(String faultyMethodName) {
        this.faultyMethodName = faultyMethodName;
    }

    public Field getFaultyField() {
        return faultyField;
    }

    public void setFaultyField(Field faultyField) {
        this.faultyField = faultyField;
    }

    public String[] getFaultyMethodParams() {
        if(faultyMethodParams==null && faultyMethod != null) {
            ArrayList<String> l = new ArrayList<String>();
            for (Class<?> aClass : faultyMethod.getParameterTypes()) 
                l.add(aClass.getName());
            faultyMethodParams = l.toArray(new String[]{});
        }
        return faultyMethodParams;
    }
    
    public void setFaultyClassAndMethod(Method faultyMethod) {
        this.faultyClass = faultyMethod.getDeclaringClass();
        this.faultyMethod = faultyMethod;
    }

    public void setFaultyClassAndField(Field faultyField) {
        this.faultyField = faultyField;
        this.faultyClass = faultyField.getDeclaringClass();
    }
}
