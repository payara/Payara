/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2016 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
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
 *
 * Portions Copyright [2017-2021] Payara Foundation and/or affiliates
 */

package org.apache.catalina.core;

import jakarta.servlet.ServletRegistration;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.ResourceBundle;

import org.apache.catalina.LogFacade;

/**
 * Implementation through which a servlet can be configured
 */
public class ServletRegistrationImpl implements ServletRegistration {


    protected StandardWrapper wrapper;
    protected StandardContext ctx;

    private static final ResourceBundle rb = LogFacade.getLogger().getResourceBundle();

    /**
     * Constructor
     * @param wrapper
     * @param ctx
     */
    public ServletRegistrationImpl(StandardWrapper wrapper,
                                      StandardContext ctx) {
        this.wrapper = wrapper;
        this.ctx = ctx;
    }

    @Override
    public String getName() {
        return wrapper.getName();
    }

    public StandardContext getContext() {
        return ctx;
    }

    /**
     * Returns the wrapper containing the servlet definition
     * @return 
     */
    public StandardWrapper getWrapper() {
        return wrapper;
    }

    @Override
    public String getClassName() {
        return wrapper.getServletClassName();
    }

    /**
     * Return the context-relative URI of the JSP file for this servlet.
     * @return null if this is not a JSP Servlet
     */
    public String getJspFile() {
        return wrapper.getJspFile();
    }

    @Override
    public boolean setInitParameter(String name, String value) {
        if (ctx.isContextInitializedCalled()) {
            String msg = MessageFormat.format(rb.getString(LogFacade.SERVLET_REGISTRATION_ALREADY_INIT),
                                              new Object[] {"init parameter", wrapper.getName(),
                                                            ctx.getName()});
            throw new IllegalStateException(msg);
        }
        return wrapper.setInitParameter(name, value, false);
    }

    @Override
    public String getInitParameter(String name) {
        return wrapper.getInitParameter(name);
    }

    @Override
    public Set<String> setInitParameters(Map<String, String> initParameters) {
        return wrapper.setInitParameters(initParameters);
    }

    @Override
    public Map<String, String> getInitParameters() {
        return wrapper.getInitParameters();
    }

    @Override
    public Set<String> addMapping(String... urlPatterns) {
        if (ctx.isContextInitializedCalled()) {
            String msg = MessageFormat.format(rb.getString(LogFacade.SERVLET_REGISTRATION_ALREADY_INIT),
                                              new Object[] {"mapping", wrapper.getName(),
                                                            ctx.getName()});
            throw new IllegalStateException(msg);
        }

        if (urlPatterns == null || urlPatterns.length == 0) {
            String msg = MessageFormat.format(rb.getString(LogFacade.SERVLET_REGISTRATION_MAPPING_URL_PATTERNS_EXCEPTION),
                                              new Object[] {wrapper.getName(), ctx.getName()});
            throw new IllegalArgumentException(msg);
        }

        return ctx.addServletMapping(wrapper.getName(), urlPatterns);
    }

    @Override
    public Collection<String> getMappings() {
        return wrapper.getMappings();
    }

    @Override
    public String getRunAsRole() {
        return wrapper.getRunAs();
    }
}

