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

package com.sun.enterprise.deployment.annotation.context;

import com.sun.enterprise.deployment.ServiceReferenceDescriptor;
import com.sun.enterprise.deployment.WebBundleDescriptor;
import com.sun.enterprise.deployment.WebComponentDescriptor;
import com.sun.enterprise.deployment.WebServiceEndpoint;
import com.sun.enterprise.deployment.types.HandlerChainContainer;
import org.glassfish.apf.AnnotatedElementHandler;

import java.lang.annotation.ElementType;
import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.List;

/**
 * This ClientContext implementation holds a top level reference
 * to the DOL Web BundleDescriptor which will be used to populate
 * any information processed from the annotations.
 *
 * @author Shing Wai Chan
 */
public class WebBundleContext extends ResourceContainerContextImpl {

    public WebBundleContext(WebBundleDescriptor webBundleDescriptor) {
        super(webBundleDescriptor);
    }

    public WebBundleDescriptor getDescriptor() {
        return (WebBundleDescriptor)descriptor;
    }

    /**
     * This method create a context for web component(s) by using
     * descriptor(s) associated to given webComponet impl class. 
     * Return null if corresponding descriptor is not found.
     */
    public AnnotatedElementHandler createContextForWeb() {
        AnnotatedElement anTypeElement =
                this.getProcessingContext().getProcessor(
                ).getLastAnnotatedElement(ElementType.TYPE);
        WebComponentDescriptor[] webComps = null;
        if (anTypeElement != null) {
            String implClassName = ((Class)anTypeElement).getName();
            webComps = getDescriptor().getWebComponentByImplName(implClassName);
        }

        AnnotatedElementHandler aeHandler = null;
        if (webComps != null && webComps.length > 1) {
            aeHandler = new WebComponentsContext(webComps);
        } else if (webComps != null && webComps.length == 1) {
            aeHandler = new WebComponentContext(webComps[0]);
        }

        if (aeHandler != null) {
            // push a WebComponent(s)Context to stack
            this.getProcessingContext().pushHandler(aeHandler);
        }
        return aeHandler;
    }
    
    public HandlerChainContainer[] 
            getHandlerChainContainers(boolean serviceSideHandlerChain, Class declaringClass) {
        if(serviceSideHandlerChain) {
            List<WebServiceEndpoint> result = new ArrayList<WebServiceEndpoint>();            
            for (WebServiceEndpoint endpoint : getDescriptor().getWebServices().getEndpoints()) {
                if (endpoint.getWebComponentImpl().getWebComponentImplementation().equals(declaringClass.getName())) {
                    result.add(endpoint);
                }
            }            
            return(result.toArray(new HandlerChainContainer[result.size()]));
        } else {
            List<ServiceReferenceDescriptor> result = new ArrayList<ServiceReferenceDescriptor>();
            result.addAll(getDescriptor().getServiceReferenceDescriptors());
            return(result.toArray(new HandlerChainContainer[result.size()]));
        }
    }
}
