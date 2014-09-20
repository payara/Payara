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

package com.sun.enterprise.deployment.runtime.common;

import com.sun.enterprise.deployment.BundleDescriptor;
import com.sun.enterprise.deployment.MethodDescriptor;
import com.sun.enterprise.deployment.ServiceRefPortInfo;
import com.sun.enterprise.deployment.WebServiceEndpoint;
import com.sun.enterprise.deployment.runtime.RuntimeDescriptor;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MessageDescriptor extends RuntimeDescriptor {
    public static final String JAVA_METHOD = "JavaMethod";
    public static final String OPERATION_NAME = "OperationName";

    private static final String ALL_METHODS = "*";

    private String operationName = null;
    private MethodDescriptor methodDescriptor = null;
    private ArrayList convertedMethodDescs = new ArrayList();

    // when this message is defined from client side
    private ServiceRefPortInfo portInfo = null;

    // when this message is defined from server side
    private WebServiceEndpoint endPoint = null;

    private BundleDescriptor bundleDesc = null; 

    private boolean isConverted = false;

    public MessageDescriptor() {}

    public void setOperationName(String operationName) {
        this.operationName = operationName;
    }
    
    public String getOperationName() {
        return operationName;
    }

    public void setMethodDescriptor(MethodDescriptor methodDescriptor) {
        this.methodDescriptor = methodDescriptor;
    }

    public MethodDescriptor getMethodDescriptor() {
        return methodDescriptor;
    }
    
    public void setServiceRefPortInfo(ServiceRefPortInfo portInfo) {
        this.portInfo = portInfo;
    }

    public ServiceRefPortInfo getServiceRefPortInfo() {
        return portInfo;
    }

    public void setWebServiceEndpoint(WebServiceEndpoint endPoint){
        this.endPoint = endPoint;
    }

    public WebServiceEndpoint getWebServiceEndpoint() {
        return endPoint;
    }

    public void setBundleDescriptor(BundleDescriptor bundleDesc){
        this.bundleDesc = bundleDesc; 
    }
        
    public BundleDescriptor getBundleDescriptor() {
        return bundleDesc;
    }

    /**
     * Return all methods defined in this message.
     *
     * In the case of an empty message, it will return all methods 
     * defined in the SEI.
     *
     * In the case of methods overloading, it will return all methods
     * defined in the SEI that match with the specified method name.
     *
     * In the case of DII, i.e the client doesn't have the SEI info,
     * it will return an empty list for client side defined message.
     *
     **/
    public ArrayList getAllDefinedMethodsInMessage() {
       // only do the conversion if it hasn't done it yet
       if (!isConverted) {
           doStyleConversion();
       }
       return convertedMethodDescs;
    }

    private void doStyleConversion() {
        if (operationName == null && methodDescriptor == null) {
            // this is the empty message case
            // and we need to expand to all methods 
            convertedMethodDescs =  getAllSEIMethodsOf(ALL_METHODS);
        } else if (methodDescriptor != null) {
            if (methodDescriptor.getName() != null  &&
                methodDescriptor.getParameterClassNames() != null) {
                // this is the exact case, so no conversion needed
                convertedMethodDescs.add(methodDescriptor);
            } else if (methodDescriptor.getName() != null  &&
                methodDescriptor.getParameterClassNames() == null) { 
                // we need to check for overloading methods
                convertedMethodDescs = 
                    getAllSEIMethodsOf(methodDescriptor.getName());
            }
        }
        isConverted = true;
    }

    private ArrayList getAllSEIMethodsOf(String methodName) {
        String serviceInterfaceName = null;
        ArrayList allMethodsInSEI = new ArrayList();

        // this is a server side message
        if (endPoint != null) {
            serviceInterfaceName = endPoint.getServiceEndpointInterface();
        // this is a client side message
        } else if (portInfo != null) {
            serviceInterfaceName = portInfo.getServiceEndpointInterface();
        }
        
        // In the case of DII, client doesn't know the SEI name
        // return an empty list
        if (serviceInterfaceName == null) { 
            return allMethodsInSEI;
        }

        ClassLoader classLoader = null; 
        if (bundleDesc != null) {
            classLoader = bundleDesc.getClassLoader();
        }

        // return an empty list if class loader is not set
        if (classLoader == null) {
            return allMethodsInSEI;
        }

        try {
            Class c = classLoader.loadClass(serviceInterfaceName);
            Method[] methods = c.getMethods();
            for (int i = 0; i < methods.length; i++) {
                // empty message or message name is *
                if (methodName.equals(ALL_METHODS)) { 
                    allMethodsInSEI.add(new MethodDescriptor(methods[i]));
                // overloading methods with same method name
                } else if (methodName.equals(methods[i].getName())) {
                    allMethodsInSEI.add(new MethodDescriptor(methods[i]));
                }
            }
        } catch (Exception e) {
            Logger.getAnonymousLogger().log(Level.WARNING, "Error occurred", e); 
            // if there is exception in the class loading
            // then we just return the empty list
        }
        return allMethodsInSEI;
    }
}
