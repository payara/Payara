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

import org.apache.catalina.deploy.FilterDef;
import org.apache.catalina.deploy.FilterMap;
import org.glassfish.logging.annotation.LogMessageInfo;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Logger;

public class FilterRegistrationImpl implements FilterRegistration {

    protected FilterDef filterDef;
    protected StandardContext ctx;

    private static final ResourceBundle rb = StandardServer.log.getResourceBundle();

    @LogMessageInfo(
        message = "Unable to configure {0} for filter {1} of servlet context {2}, because this servlet context has already been initialized",
        level = "WARNING"
    )
    public static final String FILTER_REGISTRATION_ALREADY_INIT = "AS-WEB-CORE-00117";

    @LogMessageInfo(
        message = "Unable to configure mapping for filter {0} of servlet context {1}, because servlet names are null or empty",
        level = "WARNING"
    )
    public static final String FILTER_REGISTRATION_MAPPING_SERVLET_NAME_EXCEPTION = "AS-WEB-CORE-00118";

    @LogMessageInfo(
        message = "Unable to configure mapping for filter {0} of servlet context {1}, because URL patterns are null or empty",
        level = "WARNING"
    )
    public static final String FILTER_REGISTRATION_MAPPING_URL_PATTERNS_EXCEPTION = "AS-WEB-CORE-00119";

    /**
     * Constructor
     */
    FilterRegistrationImpl(FilterDef filterDef, StandardContext ctx) {
        this.filterDef = filterDef;
        this.ctx = ctx;
    }


    public String getName() {
        return filterDef.getFilterName();
    }


    public FilterDef getFilterDefinition() {
        return filterDef;
    }


    public String getClassName() {
        return filterDef.getFilterClassName();
    }


    public boolean setInitParameter(String name, String value) {
        if (ctx.isContextInitializedCalled()) {
            String msg = MessageFormat.format(rb.getString(FILTER_REGISTRATION_ALREADY_INIT),
                                              new Object[] {"init parameter", filterDef.getFilterName(),
                                                            ctx.getName()});
            throw new IllegalStateException(msg);
        }

        return filterDef.setInitParameter(name, value, false);
    }


    public String getInitParameter(String name) {
        return filterDef.getInitParameter(name);
    }


    public Set<String> setInitParameters(Map<String, String> initParameters) {
        return filterDef.setInitParameters(initParameters);
    }


    public Map<String, String> getInitParameters() {
        return filterDef.getInitParameters();
    }


    public void addMappingForServletNames(
            EnumSet<DispatcherType> dispatcherTypes, boolean isMatchAfter,
            String... servletNames) {

        if (ctx.isContextInitializedCalled()) {
            String msg = MessageFormat.format(rb.getString(FILTER_REGISTRATION_ALREADY_INIT),
                                              new Object[] {"servlet-name mapping", filterDef.getFilterName(),
                                                            ctx.getName()});
            throw new IllegalStateException(msg);
        }

        if ((servletNames==null) || (servletNames.length==0)) {
            String msg = MessageFormat.format(rb.getString(FILTER_REGISTRATION_MAPPING_SERVLET_NAME_EXCEPTION),
                                              new Object[]  {filterDef.getFilterName(), ctx.getName()});
            throw new IllegalArgumentException(msg);
        }

        for (String servletName : servletNames) {
            FilterMap fmap = new FilterMap();
            fmap.setFilterName(filterDef.getFilterName());
            fmap.setServletName(servletName);
            fmap.setDispatcherTypes(dispatcherTypes);

            ctx.addFilterMap(fmap, isMatchAfter);
        }
    }


    public Collection<String> getServletNameMappings() {
        return ctx.getServletNameFilterMappings(getName());
    }


    public void addMappingForUrlPatterns(
            EnumSet<DispatcherType> dispatcherTypes, boolean isMatchAfter,
            String... urlPatterns) {

        if (ctx.isContextInitializedCalled()) {
            String msg = MessageFormat.format(rb.getString(FILTER_REGISTRATION_ALREADY_INIT),
                                              new Object[] {"url-pattern mapping", filterDef.getFilterName(),
                                                            ctx.getName()});
            throw new IllegalStateException(msg);
        }

        if ((urlPatterns==null) || (urlPatterns.length==0)) {
            String msg = MessageFormat.format(rb.getString(FILTER_REGISTRATION_MAPPING_URL_PATTERNS_EXCEPTION),
                                              new Object[] {filterDef.getFilterName(), ctx.getName()});
            throw new IllegalArgumentException(msg);
        }

        for (String urlPattern : urlPatterns) {
            FilterMap fmap = new FilterMap();
            fmap.setFilterName(filterDef.getFilterName());
            fmap.setURLPattern(urlPattern);
            fmap.setDispatcherTypes(dispatcherTypes);

            ctx.addFilterMap(fmap, isMatchAfter);
        }
    }


    public Collection<String> getUrlPatternMappings() {
        return ctx.getUrlPatternFilterMappings(getName());
    }
}

