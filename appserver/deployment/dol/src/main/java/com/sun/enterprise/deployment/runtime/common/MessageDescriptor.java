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
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
// Portions Copyright [2019] [Payara Foundation and/or its affiliates]
package com.sun.enterprise.deployment.runtime.common;

import static java.util.logging.Level.WARNING;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.sun.enterprise.deployment.BundleDescriptor;
import com.sun.enterprise.deployment.MethodDescriptor;
import com.sun.enterprise.deployment.ServiceRefPortInfo;
import com.sun.enterprise.deployment.WebServiceEndpoint;
import com.sun.enterprise.deployment.runtime.RuntimeDescriptor;

public class MessageDescriptor extends RuntimeDescriptor {
    
    private static final long serialVersionUID = 6097863237384161130L;
    
    public static final String JAVA_METHOD = "JavaMethod";
    public static final String OPERATION_NAME = "OperationName";
    private static final String ALL_METHODS = "*";

    private String operationName;
    private MethodDescriptor methodDescriptor;
    private List<MethodDescriptor> convertedmethodDescriptors = new ArrayList<>();

    // When this message is defined from client side
    private ServiceRefPortInfo portInfo;

    // When this message is defined from server side
    private WebServiceEndpoint webServiceEndpoint;
    private BundleDescriptor bundleDescriptor;
    private boolean isConverted;

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

    public void setWebServiceEndpoint(WebServiceEndpoint endPoint) {
        this.webServiceEndpoint = endPoint;
    }

    public WebServiceEndpoint getWebServiceEndpoint() {
        return webServiceEndpoint;
    }

    public void setBundleDescriptor(BundleDescriptor bundleDesc) {
        this.bundleDescriptor = bundleDesc;
    }

    public BundleDescriptor getBundleDescriptor() {
        return bundleDescriptor;
    }

    /**
     * Return all methods defined in this message.
     *
     * In the case of an empty message, it will return all methods defined in the SEI.
     *
     * In the case of methods overloading, it will return all methods defined in the SEI that match with the specified
     * method name.
     *
     * In the case of DII, i.e the client doesn't have the SEI info, it will return an empty list for client side defined
     * message.
     *
     **/
    public List<MethodDescriptor> getAllDefinedMethodsInMessage() {
        // Only do the conversion if it hasn't been done yet
        if (!isConverted) {
            doStyleConversion();
        }
        
        return convertedmethodDescriptors;
    }

    private void doStyleConversion() {
        if (operationName == null && methodDescriptor == null) {
            // This is the empty message case and we need to expand to all methods
            
            convertedmethodDescriptors = getAllSEIMethodsOf(ALL_METHODS);
        } else if (methodDescriptor != null) {
            if (methodDescriptor.getName() != null && methodDescriptor.getParameterClassNames() != null) {
                // This is the exact case, so no conversion needed
                
                convertedmethodDescriptors.add(methodDescriptor);
            } else if (methodDescriptor.getName() != null && methodDescriptor.getParameterClassNames() == null) {
                // We need to check for overloading methods
                
                convertedmethodDescriptors = getAllSEIMethodsOf(methodDescriptor.getName());
            }
        }
        
        isConverted = true;
    }

    private List<MethodDescriptor> getAllSEIMethodsOf(String methodName) {
        String serviceInterfaceName = null;
        List<MethodDescriptor> allMethodsInSEI = new ArrayList<>();
        
        if (webServiceEndpoint != null) {
            
            // This is a server side message
            
            serviceInterfaceName = webServiceEndpoint.getServiceEndpointInterface();
            
        } else if (portInfo != null) {
            
            // This is a client side message
            
            serviceInterfaceName = portInfo.getServiceEndpointInterface();
        }

        // In the case of DII, client doesn't know the SEI name
        // return an empty list
        if (serviceInterfaceName == null) {
            return allMethodsInSEI;
        }

        ClassLoader classLoader = null;
        if (bundleDescriptor != null) {
            classLoader = bundleDescriptor.getClassLoader();
        }

        // Return an empty list if class loader is not set
        if (classLoader == null) {
            return allMethodsInSEI;
        }

        try {
            for (Method method : classLoader.loadClass(serviceInterfaceName).getMethods()) {
                if (methodName.equals(ALL_METHODS)) {
                    
                    // Empty message or message name is *
                    
                    allMethodsInSEI.add(new MethodDescriptor(method));
                } else if (methodName.equals(method.getName())) {
                    
                    // Overloading methods with same method name
                    
                    allMethodsInSEI.add(new MethodDescriptor(method));
                }
            }
        } catch (Exception e) {
            // If there is exception in the class loading then we just log and return the empty list
            Logger.getAnonymousLogger().log(WARNING, "Error occurred", e);
        }
        
        return allMethodsInSEI;
    }
}
