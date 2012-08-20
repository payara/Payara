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
 * EjbContext.java
 *
 * Created on January 16, 2005, 5:53 PM
 */

package com.sun.enterprise.deployment.annotation.context;

import java.lang.annotation.ElementType;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import com.sun.enterprise.deployment.EjbDescriptor;
import com.sun.enterprise.deployment.MethodDescriptor;
import com.sun.enterprise.deployment.ServiceReferenceDescriptor;
import com.sun.enterprise.deployment.WebServiceEndpoint;
import com.sun.enterprise.deployment.annotation.handlers.PostProcessor;
import com.sun.enterprise.deployment.types.HandlerChainContainer;
import com.sun.enterprise.deployment.types.ServiceReferenceContainer;
import com.sun.enterprise.deployment.util.TypeUtil;
import org.glassfish.apf.AnnotationInfo;
import org.glassfish.apf.AnnotationProcessorException;
import org.glassfish.apf.impl.ComponentDefinition;
import org.glassfish.deployment.common.Descriptor;

/**
 *
 * @author dochez
 */
public class EjbContext extends ResourceContainerContextImpl {
    private WebServiceEndpoint endpoint;
    private Method[] methods;
    private boolean inherited;
    private ArrayList<PostProcessInfo> postProcessInfos =
            new ArrayList<PostProcessInfo>();

    public EjbContext(EjbDescriptor currentEjb, Class ejbClass) {
        super((Descriptor) currentEjb); // FIXME by srini - can we extract intf to avoid this
        componentClassName = currentEjb.getEjbClassName();
        ComponentDefinition cdef = new ComponentDefinition(ejbClass);
        methods = cdef.getMethods();
        Class superClass = ejbClass.getSuperclass();
        inherited = (superClass != null && !Object.class.equals(superClass));
    }

    public EjbDescriptor getDescriptor() {
        return (EjbDescriptor)descriptor;
    }

    public void setDescriptor(EjbDescriptor currentEjb) {
        descriptor = (Descriptor) currentEjb;  // FIXME by srini - can we extract intf to avoid this
    }

    public void setEndpoint(WebServiceEndpoint endpoint) {
        this.endpoint = endpoint;
    }
    
    public WebServiceEndpoint getEndpoint() {
        return endpoint;
    }

    public void endElement(ElementType type, AnnotatedElement element) 
            throws AnnotationProcessorException {
        
        if (ElementType.TYPE.equals(type)) {
            for (PostProcessInfo ppInfo : postProcessInfos) {
                 ppInfo.postProcessor.postProcessAnnotation(
                         ppInfo.ainfo, this);
            }

            // done with processing this class, let's pop this context
            getProcessingContext().popHandler();
        }
    }

    public Class getDeclaringClass(MethodDescriptor md) {
        Method method = md.getMethod(getDescriptor());
        Class declaringClass = null;
        for (Method m : methods) {
            if (TypeUtil.sameMethodSignature(m, method)) {
                declaringClass = m.getDeclaringClass();
            }
        }
        return declaringClass;
    }

    public Method[] getComponentDefinitionMethods() {
        return methods;
    }

    public boolean isInherited() {
        return inherited;
    }

    public void addPostProcessInfo(AnnotationInfo ainfo, PostProcessor postProcessor) {
        PostProcessInfo ppInfo = new PostProcessInfo();
        ppInfo.ainfo = ainfo;
        ppInfo.postProcessor = postProcessor;
        postProcessInfos.add(ppInfo);
    }

    private static class PostProcessInfo {
        public AnnotationInfo ainfo;
        public PostProcessor postProcessor;
    }
    
    public ServiceReferenceContainer[] getServiceRefContainers(String implName) {
        return getDescriptor().getEjbBundleDescriptor().getEjbByClassName(implName);
    }    

    public HandlerChainContainer[] 
            getHandlerChainContainers(boolean serviceSideHandlerChain, Class declaringClass) {
        if(serviceSideHandlerChain) {
            EjbDescriptor[] ejbs = getDescriptor().getEjbBundleDescriptor().getEjbByClassName(declaringClass.getName());
            List<WebServiceEndpoint> result = new ArrayList<WebServiceEndpoint>();
            for (EjbDescriptor ejb : ejbs) {
                result.addAll(getDescriptor().getEjbBundleDescriptor().getWebServices().getEndpointsImplementedBy(ejb));
            }
            return(result.toArray(new HandlerChainContainer[result.size()]));
        } else {
            List<ServiceReferenceDescriptor> result = new ArrayList<ServiceReferenceDescriptor>();
            result.addAll(getDescriptor().getEjbBundleDescriptor().getEjbServiceReferenceDescriptors());
            return(result.toArray(new HandlerChainContainer[result.size()]));
        }
    }    
}
