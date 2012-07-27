/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.web.deployment.util;

import com.sun.enterprise.deployment.*;
import com.sun.enterprise.deployment.web.MultipartConfig;
import com.sun.enterprise.deployment.web.ServletFilter;
import com.sun.enterprise.deployment.util.ApplicationValidator;
import org.glassfish.web.deployment.descriptor.ServletFilterDescriptor;
import org.glassfish.web.deployment.descriptor.SessionConfigDescriptor;

import java.util.Iterator;
import java.util.Set;

/**
 * This class validates the part of the web bundle descriptor 
 *
 */
public class WebBundleValidator extends ApplicationValidator implements WebBundleVisitor {
    
    public void accept (BundleDescriptor descriptor) {
        if (descriptor instanceof WebBundleDescriptor) {
            WebBundleDescriptor webBundle = (WebBundleDescriptor)descriptor;
            accept(webBundle);

            // Visit all injectables first.  In some cases, basic type
            // information has to be derived from target inject method or 
            // inject field.
            for (InjectionCapable injectable : webBundle.getInjectableResources(webBundle)) {
                accept(injectable);
            }

            for (Iterator<WebComponentDescriptor> i = webBundle.getWebComponentDescriptors().iterator(); i.hasNext();) {
                WebComponentDescriptor aWebComp = i.next();
                accept(aWebComp);
            }

            for (Iterator<WebService> itr = webBundle.getWebServices().getWebServices().iterator(); itr.hasNext();) {
                WebService aWebService = itr.next();
                accept(aWebService);
            }

            super.accept(descriptor);

            for (Iterator<ServletFilter> itr = webBundle.getServletFilterDescriptors().iterator(); itr.hasNext();) {
                ServletFilterDescriptor servletFilterDescriptor = (ServletFilterDescriptor) itr.next();
                accept(servletFilterDescriptor);
            }
        }
    }

    /**
     * visit a web bundle descriptor
     *
     * @param descriptor the web bundle descriptor
     */
    public void accept(WebBundleDescriptor descriptor) {
        bundleDescriptor = descriptor;
        application = descriptor.getApplication();

        if (descriptor.getSessionConfig() == null) {
            descriptor.setSessionConfig(new SessionConfigDescriptor());
        }
        if (descriptor.isDistributable() == null) {
            descriptor.setDistributable(Boolean.FALSE);
        }
    }

    /**
     * visit a web component descriptor
     *
     * @param descriptor the web component
     */
    protected void accept(WebComponentDescriptor descriptor) {

        //set default value
        if (descriptor.getLoadOnStartUp() == null) {
            descriptor.setLoadOnStartUp(-1);
        }
        if (descriptor.isAsyncSupported() == null) {
            descriptor.setAsyncSupported(false);
        }

        MultipartConfig multipartConfig = descriptor.getMultipartConfig();
        if (multipartConfig != null) {
            if (multipartConfig.getMaxFileSize() == null) {
                multipartConfig.setMaxFileSize(Long.valueOf(-1));
            }
            if (multipartConfig.getMaxRequestSize() == null) {
                multipartConfig.setMaxRequestSize(Long.valueOf(-1));
            }
            if (multipartConfig.getFileSizeThreshold() == null) {
                multipartConfig.setFileSizeThreshold(Integer.valueOf(0));
            }
        }
        computeRuntimeDefault(descriptor);
    }

    private void computeRuntimeDefault(WebComponentDescriptor webComp) {
        if (!webComp.getUsesCallerIdentity()) {
            computeRunAsPrincipalDefault(
                webComp.getRunAsIdentity(), webComp.getApplication());
        }
    }

    protected void accept(ServletFilterDescriptor descriptor) {
        // set default value
        if (descriptor.isAsyncSupported() == null) {
            descriptor.setAsyncSupported(false);
        }
    }
}
