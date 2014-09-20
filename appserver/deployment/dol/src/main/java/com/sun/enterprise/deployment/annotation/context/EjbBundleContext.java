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

/*
 * EjbBundleContext.java
 *
 * Created on January 12, 2005, 10:20 AM
 */

package com.sun.enterprise.deployment.annotation.context;

import com.sun.enterprise.deployment.*;
import com.sun.enterprise.deployment.types.HandlerChainContainer;
import com.sun.enterprise.deployment.types.ServiceReferenceContainer;
import org.glassfish.apf.AnnotatedElementHandler;

import java.lang.annotation.ElementType;
import java.util.ArrayList;
import java.util.List;

/**
 * This ClientContext implementation holds a top level reference
 * to the DOL EJB BundleDescriptor which will be used to populate
 * any information processed from the annotations.
 *
 * @author Jerome Dochez
 */
public class EjbBundleContext extends ResourceContainerContextImpl {
    
    /** Creates a new instance of EjbBundleContext */
    public EjbBundleContext(EjbBundleDescriptor descriptor) {
        super(descriptor);
    }
    
    public EjbBundleDescriptor getDescriptor() {
        return (EjbBundleDescriptor)descriptor;
    }

    /**
     * This methods create a context for Ejb(s) by using descriptor(s)
     * associated to given ejbClassName.
     * Return null if corresponding descriptor is not found.
     */
    public AnnotatedElementHandler createContextForEjb() {
        Class ejbClass = (Class)this.getProcessingContext().getProcessor(
                ).getLastAnnotatedElement(ElementType.TYPE);
        EjbDescriptor[] ejbDescs = null;
        String ejbClassName = null;
        if (ejbClass != null) {
            ejbClassName = ejbClass.getName();
            ejbDescs = this.getDescriptor().getEjbByClassName(ejbClassName);
        }

        AnnotatedElementHandler aeHandler = null;
        if (ejbDescs != null && ejbDescs.length > 1) {
            aeHandler = new EjbsContext(ejbDescs, ejbClass);
        } else if (ejbDescs != null && ejbDescs.length == 1) {
            aeHandler = new EjbContext(ejbDescs[0], ejbClass);
        }

        if (aeHandler != null) {
            // push a EjbContext to stack
            this.getProcessingContext().pushHandler(aeHandler);
        }
        return aeHandler;
    }
            
    public HandlerChainContainer[] 
            getHandlerChainContainers(boolean serviceSideHandlerChain, Class declaringClass) {
        if(serviceSideHandlerChain) {
            EjbDescriptor[] ejbs;
            if(declaringClass.isInterface()) {
                ejbs = getDescriptor().getEjbBySEIName(declaringClass.getName());
            } else {
                ejbs = getDescriptor().getEjbByClassName(declaringClass.getName());
            }
            List<WebServiceEndpoint> result = new ArrayList<WebServiceEndpoint>();
            for (EjbDescriptor ejb : ejbs) {
                result.addAll(getDescriptor().getWebServices().getEndpointsImplementedBy(ejb));
            }
            return(result.toArray(new HandlerChainContainer[result.size()]));
        } else {
            List<ServiceReferenceDescriptor> result = new ArrayList<ServiceReferenceDescriptor>();
            result.addAll(getDescriptor().getEjbServiceReferenceDescriptors());
            return(result.toArray(new HandlerChainContainer[result.size()]));
        }
    }
    
    public ServiceReferenceContainer[] getServiceRefContainers() {
        ServiceReferenceContainer[] container = 
                new ServiceReferenceContainer[getDescriptor().getEjbs().size()];
        ServiceReferenceContainer[] ret = 
                (ServiceReferenceContainer[])getDescriptor().getEjbs().toArray(container);
        return ret;
    }    
    
    /**
     * This methods create a context for EjbInterceptor associated to
     * given className.
     * Return null if corresponding descriptor is not found.
     */
    public AnnotatedElementHandler createContextForEjbInterceptor() {
        Class interceptorClass =
                (Class)this.getProcessingContext().getProcessor(
                ).getLastAnnotatedElement(ElementType.TYPE);
        EjbInterceptor ejbInterceptor =
                this.getDescriptor().getInterceptorByClassName(
                interceptorClass.getName());
        
        AnnotatedElementHandler aeHandler = null;
        if (ejbInterceptor != null) {
            aeHandler = new EjbInterceptorContext(ejbInterceptor);
            // push a EjbInterceptorContext to stack
            this.getProcessingContext().pushHandler(aeHandler);
        }
        return aeHandler;
    }
}
