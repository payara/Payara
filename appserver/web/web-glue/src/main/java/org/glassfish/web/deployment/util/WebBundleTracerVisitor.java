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
import com.sun.enterprise.deployment.web.ServletFilter;
import com.sun.enterprise.deployment.util.DOLUtils;
import com.sun.enterprise.deployment.util.TracerVisitor;
import org.glassfish.web.deployment.descriptor.ServletFilterDescriptor;

import java.util.Iterator;

public class WebBundleTracerVisitor extends TracerVisitor implements WebBundleVisitor {

    public WebBundleTracerVisitor() {
    }

    public void accept (BundleDescriptor descriptor) {
        if (descriptor instanceof WebBundleDescriptor) {
            WebBundleDescriptor webBundle = (WebBundleDescriptor)descriptor;
            accept(webBundle);

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
                ServletFilter servletFilterDescriptor = itr.next();
                accept(servletFilterDescriptor);
            }
        }
    }

   /**
     * visit a web bundle descriptor
     *
     * @param the web bundle descriptor
     */
    public void accept(WebBundleDescriptor descriptor) {
        DOLUtils.getDefaultLogger().info(descriptor.toString());
    }

   /**
     * visit a web component descriptor
     *
     * @param the web component
     */
    protected void accept(WebComponentDescriptor descriptor) {
        DOLUtils.getDefaultLogger().info("==================");
        DOLUtils.getDefaultLogger().info(descriptor.toString());
    }

   /**
     * visit a servlet filter descriptor
     *
     * @param the servlet filter
     */
    protected void accept(ServletFilter descriptor) {
        DOLUtils.getDefaultLogger().info("==================");
        DOLUtils.getDefaultLogger().info(descriptor.toString());
    }
}

