/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.tools.verifier.tests.ejb.elements;

import com.sun.enterprise.tools.verifier.tests.ejb.EjbTest;
import com.sun.enterprise.tools.verifier.tests.ComponentNameConstructor;
import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.Verifier;
import com.sun.enterprise.deployment.MethodDescriptor;
import com.sun.enterprise.deployment.EjbDescriptor;

import java.lang.reflect.Method;
import java.util.*;

/**
 * Base class for all the MethodsExist tests.
 * ContainerTransactionMethodExists, ExcludeListMethodsExist, 
 * MethodPermissionMethodExists and UncheckedMethodsExist.
 * 
 * @author Vikas Awasthi
 */
public abstract class MethodsExist extends EjbTest {
    
    protected Result result = null;
    protected ComponentNameConstructor compName = null;
    private List<Method> methods = null;
    
    /** test for method styles 1, 2 or 3 */ 
    protected void checkMethodStyles(MethodDescriptor methodDescriptor, 
                                     EjbDescriptor descriptor) {

        if (methodDescriptor.getName().equals(MethodDescriptor.ALL_METHODS)) 
            return; //style 1
        
        if (methodDescriptor.getParameterClassNames() == null) //style 2
            checkStyle(methodDescriptor, descriptor, true);
        else  // style 3
            checkStyle(methodDescriptor, descriptor, false);
    }
    
    private void checkStyle(MethodDescriptor methodDescriptor,
                            EjbDescriptor descriptor,
                            boolean isCheckStyle2) {
        
        String methodName = methodDescriptor.getName();
            
        if (methodDescriptor.getEjbClassSymbol() != null) { // method intf present

            if (methodDescriptor.getEjbClassSymbol().equals(MethodDescriptor.EJB_REMOTE)) {
                // if method-intf is Remote then add EJB3.0 remote business interfaces 
                Set<String> interfaces= new HashSet<String>(descriptor.getRemoteBusinessClassNames());
                if(descriptor.getRemoteClassName()!=null)
                    interfaces.add(descriptor.getRemoteClassName());
                
                if(!contains(methodDescriptor, getAllMethods(interfaces), isCheckStyle2)) 
                    logFailure(methodName, "remote");
                
            } else if(methodDescriptor.getEjbClassSymbol().equals(MethodDescriptor.EJB_LOCAL)) {
                // if method-intf is Local then add EJB3.0 local business interfaces 
                Set<String> interfaces= new HashSet<String>(descriptor.getLocalBusinessClassNames());
                if(descriptor.getLocalClassName()!=null)
                    interfaces.add(descriptor.getLocalClassName());
                
                if(!contains(methodDescriptor, getAllMethods(interfaces), isCheckStyle2)) 
                    logFailure(methodName, "local");
                
            } else if (methodDescriptor.getEjbClassSymbol().equals(MethodDescriptor.EJB_HOME)) {
                
                if(!contains(methodDescriptor, getAllMethods(descriptor.getHomeClassName()), isCheckStyle2)) 
                    logFailure(methodName, "home");
                
            } else if(methodDescriptor.getEjbClassSymbol().equals(MethodDescriptor.EJB_LOCALHOME)) {
                
                if(!contains(methodDescriptor, getAllMethods(descriptor.getLocalHomeClassName()), isCheckStyle2)) 
                    logFailure(methodName, "localhome");
                
            } else if(methodDescriptor.getEjbClassSymbol().equals(MethodDescriptor.EJB_WEB_SERVICE)) {

                String endpointIntfName = descriptor.getWebServiceEndpointInterfaceName();
                if(!contains(methodDescriptor, getAllMethods(endpointIntfName), isCheckStyle2)) 
                    logFailure(methodName, "localhome");
                
            }
            // for ejbTimeout method of TimedObject the methodDescriptor.getEjbClassSymbol()
            // is always EJB_BEAN. So no need to check that as this test is not 
            // applicable for this method.
        } else { // method intf not present
            if(!contains(methodDescriptor, getAllMethods(descriptor), true)) 
                logFailure(methodDescriptor.getName(), "any of component or home");
        }
    }
    
    /** 
     * checks if method1 is present in the given list of methods. 
     * For style 2 methods only method names are compared and for style 3 
     * method parameters are also compared. 
     */
    private boolean contains(MethodDescriptor method1, List<Method> methods, boolean isStyle2) {
        for (Method method : methods) {
            if(isStyle2) {// for style 2 do only name comparison
                if(method.getName().equals(method1.getName()))
                    return true;
            } else if (method.getName().equals(method1.getName()) &&
                    Arrays.equals(new MethodDescriptor().getParameterClassNamesFor(method, method.getParameterTypes()),
                            method1.getParameterClassNames()))
                return true;
        }
        return false;
    }
    
    /**
     * It returns a list of all the methods in component and home interfaces 
     * of this ejbDescriptor.
     */ 
    private List<Method> getAllMethods(EjbDescriptor descriptor) {
        if(methods!=null)
            return methods;
        
        methods = new ArrayList<Method>();
        Set<String> interfaces = descriptor.getLocalBusinessClassNames();
        
        interfaces.addAll(descriptor.getRemoteBusinessClassNames());
        if(descriptor.getRemoteClassName()!=null)
            interfaces.add(descriptor.getRemoteClassName());
        if(descriptor.getLocalClassName()!=null)
            interfaces.add(descriptor.getLocalClassName());
        if(descriptor.getHomeClassName()!=null)
            interfaces.add(descriptor.getHomeClassName());
        if(descriptor.getLocalHomeClassName()!=null)
            interfaces.add(descriptor.getLocalHomeClassName());
        if(descriptor.getWebServiceEndpointInterfaceName()!=null)
            interfaces.add(descriptor.getWebServiceEndpointInterfaceName());
        
        for (String intf : interfaces) {
            Class intfClass = loadClass(intf);
            if(intfClass==null)//ignore if null. Error message is already logged
                continue;
            methods.addAll(Arrays.asList(intfClass.getMethods()));
        }
        return methods;
    }
    
    /** It returns a list of all the methods in the given interface intf */ 
    private List<Method> getAllMethods(String intf) {
        Class intfClass = loadClass(intf);
        return (intfClass==null)? new ArrayList<Method>():Arrays.asList(intfClass.getMethods());
    }
    
    /** It returns a list of all the methods in the given interfaces */ 
    private List<Method> getAllMethods(Set<String> interfaces) {
        List<Method> methods = new ArrayList<Method>();
        for (String intf : interfaces) {
            Class intfClass = loadClass(intf);
            if(intfClass==null) 
                continue;
            methods.addAll(Arrays.asList(intfClass.getMethods()));
        }
        return methods;
    }
    
    private Class loadClass(String className) {
        Class intfClass = null;
        try {
            intfClass = Class.forName(className, 
                                      false, 
                                      getVerifierContext().getClassLoader());
        } catch (ClassNotFoundException e) {
            Verifier.debug(e);
            addErrorDetails(result, compName);
            result.failed(smh.getLocalString
                    (getClass().getName() + ".failedException",
                    "Error: Interface class not found. [ {0} ]",
                    new Object[] {className}));
        }
        return intfClass;
    }
    
    private void logFailure(String msg1, String msg2) {
        addErrorDetails(result, compName);
        result.failed(smh.getLocalString
                (getClass().getName() + ".failed",
                "Error: Method name [ {0} ] not defined in {1} interface.",
                new Object[] {msg1, msg2}));
    }
    
}
