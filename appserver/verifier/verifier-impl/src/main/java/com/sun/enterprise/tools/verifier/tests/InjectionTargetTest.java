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

package com.sun.enterprise.tools.verifier.tests;

import com.sun.enterprise.tools.verifier.Result;
import org.glassfish.deployment.common.Descriptor;
import com.sun.enterprise.deployment.InjectionCapable;
import com.sun.enterprise.deployment.InjectionTarget;

import java.util.List;
import java.util.Set;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Method;

/**
 * The field or method where injection annotation is used may have any access 
 * qualifier (public , private , etc.) but must not be static or final.
 * This is the base class of all the InjectionAnnotation tests. 
 * 
 * @author Vikas Awasthi
 */
public abstract class InjectionTargetTest extends VerifierTest implements VerifierCheck {
// Currently only ejbs are checked for injection annotations. Other modules can 
// also use this class to test the assertion.
    protected abstract List<InjectionCapable> getInjectables(String className);
    protected abstract String getClassName();
    private Descriptor descriptor;
    Result result; 
    ComponentNameConstructor compName;
    
    public Result check(Descriptor descriptor) {
        this.descriptor = descriptor;
        result = getInitializedResult();
        compName = getVerifierContext().getComponentNameConstructor();
        ClassLoader cl = getVerifierContext().getClassLoader();
        List<InjectionCapable> injectables = getInjectables(getClassName());
        for (InjectionCapable injectionCapable : injectables) {
            Set<InjectionTarget> iTargets =  injectionCapable.getInjectionTargets();
            for (InjectionTarget target : iTargets) {
                try {
                    if(target.isFieldInjectable()) {
                        Class classObj = Class.forName(getClassName(), false, cl);
                        Field field = classObj.getDeclaredField(target.getFieldName());
                        testMethodModifiers(field.getModifiers(), "field", field);
                    }
                    if(target.isMethodInjectable()) {
                        Class classObj = Class.forName(getClassName(), false, cl);
                        Method method = getInjectedMethod(classObj, target.getMethodName());
                        if(method == null) continue;
                        testMethodModifiers(method.getModifiers(), "method", method);
                    }
                } catch (Exception e) {} //ignore as it will be caught in other tests
            }  
        }
        
        if(result.getStatus() != Result.FAILED) {
            addGoodDetails(result, compName);
            result.passed(smh.getLocalString
                    (getClass().getName()+".passed",
                    "Valid injection method(s)."));
        }
        return result;
    }

    protected Descriptor getDescriptor() {
        return descriptor;
    }
    
    private void testMethodModifiers(int modifier, String targetType, Object fieldOrMethod) {
        if(Modifier.isStatic(modifier) ||
                Modifier.isFinal(modifier)) {
            addErrorDetails(result, compName);
            result.failed(smh.getLocalString
                    ("com.sun.enterprise.tools.verifier.tests.InjectionTargetTest.failed",
                    "Invalid annotation in {0} [ {1} ].",
                    new Object[] {targetType, fieldOrMethod}));
        }
    }
    
    private Method getInjectedMethod(Class classObj, String methodName) {
        for (Method method : classObj.getDeclaredMethods()) {
            if(method.getName().equals(methodName)) 
                return method;
        }
        return null;
    }
}
