/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.apache.catalina.core;

import javax.servlet.MultipartConfigElement;
import javax.servlet.Servlet;
import javax.servlet.ServletRegistration;
import javax.servlet.ServletSecurityElement;
import java.util.Collections;
import java.util.Set;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import java.text.MessageFormat;

import org.glassfish.logging.annotation.LogMessageInfo;

public class DynamicServletRegistrationImpl
    extends ServletRegistrationImpl
    implements ServletRegistration.Dynamic {

    private static final ResourceBundle rb = StandardServer.log.getResourceBundle();

    @LogMessageInfo(
        message = "Unable to configure {0} for servlet {1} of servlet context {2}, because this servlet context has already been initialized",
        level = "WARNING"
    )
    public static final String DYNAMIC_SERVLET_REGISTRATION_ALREADY_INIT = "AS-WEB-CORE-00116";

    /**
     * Constructor
     */
    public DynamicServletRegistrationImpl(StandardWrapper wrapper,
            StandardContext ctx) {
        super(wrapper, ctx);
    }

    public void setLoadOnStartup(int loadOnStartup) {
        if (ctx.isContextInitializedCalled()) {
            String msg = MessageFormat.format(rb.getString(DYNAMIC_SERVLET_REGISTRATION_ALREADY_INIT),
                                              new Object[] {"load-on-startup", wrapper.getName(), ctx.getName()});
            throw new IllegalStateException(msg);
        }

        wrapper.setLoadOnStartup(loadOnStartup);
    }

    public void setAsyncSupported(boolean isAsyncSupported) {
        if (ctx.isContextInitializedCalled()) {
            String msg = MessageFormat.format(rb.getString(DYNAMIC_SERVLET_REGISTRATION_ALREADY_INIT),
                                              new Object[] {"load-on-startup", wrapper.getName(), ctx.getName()});
            throw new IllegalStateException(msg);
        }

        wrapper.setIsAsyncSupported(isAsyncSupported);
    }

    public Set<String> setServletSecurity(ServletSecurityElement constraint) {
        Set<String> emptySet = Collections.emptySet();
        return Collections.unmodifiableSet(emptySet);
    }

    public void setMultipartConfig(MultipartConfigElement mpConfig) {
        if (ctx.isContextInitializedCalled()) {
            String msg = MessageFormat.format(rb.getString(DYNAMIC_SERVLET_REGISTRATION_ALREADY_INIT),
                    new Object[] {"multipart-config", wrapper.getName(), ctx.getName()});
            throw new IllegalStateException(msg);
        }

        wrapper.setMultipartLocation(mpConfig.getLocation());
        wrapper.setMultipartMaxFileSize(mpConfig.getMaxFileSize());
        wrapper.setMultipartMaxRequestSize(mpConfig.getMaxRequestSize());
        wrapper.setMultipartFileSizeThreshold(
            mpConfig.getFileSizeThreshold());
    }

    public void setRunAsRole(String roleName) {
        if (ctx.isContextInitializedCalled()) {
            String msg = MessageFormat.format(rb.getString(DYNAMIC_SERVLET_REGISTRATION_ALREADY_INIT),
                    new Object[] {"run-as", wrapper.getName(), ctx.getName()});
            throw new IllegalStateException(msg);
        }

        wrapper.setRunAs(roleName);
    }

    protected void setServletClassName(String className) {
        wrapper.setServletClassName(className);
    }

    protected void setServletClass(Class <? extends Servlet> clazz) {
        wrapper.setServletClass(clazz);
    }

}

