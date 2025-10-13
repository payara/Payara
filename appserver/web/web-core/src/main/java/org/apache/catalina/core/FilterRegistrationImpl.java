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

import org.apache.catalina.LogFacade;
import org.apache.catalina.deploy.FilterDef;
import org.apache.catalina.deploy.FilterMap;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterRegistration;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.text.MessageFormat;
import java.util.*;

/**
 * Class for a filter that may then be configured
 * 
 * @see DynamicFilterRegistrationImpl
 */
public class FilterRegistrationImpl implements FilterRegistration {

    protected FilterDef filterDef;
    protected StandardContext ctx;

    private static final ResourceBundle rb = LogFacade.getLogger().getResourceBundle();

    /**
     * Constructor
     */
    FilterRegistrationImpl(FilterDef filterDef, StandardContext ctx) {
        this.filterDef = filterDef;
        this.ctx = ctx;
    }


    @Override
    public String getName() {
        return filterDef.getFilterName();
    }


    /**
     * Gets a representation of the filter, as it would be in the  <code>&lt;filter&gt;</code>
     * @return 
     */
    public FilterDef getFilterDefinition() {
        return filterDef;
    }


    @Override
    public String getClassName() {
        return filterDef.getFilterClassName();
    }


    @Override
    public boolean setInitParameter(String name, String value) {
        if (ctx.isContextInitializedCalled()) {
            String msg = MessageFormat.format(rb.getString(LogFacade.FILTER_REGISTRATION_ALREADY_INIT),
                                              new Object[] {"init parameter", filterDef.getFilterName(),
                                                            ctx.getName()});
            throw new IllegalStateException(msg);
        }

        return filterDef.setInitParameter(name, value, false);
    }


    @Override
    public String getInitParameter(String name) {
        return filterDef.getInitParameter(name);
    }


    @Override
    public Set<String> setInitParameters(Map<String, String> initParameters) {
        return filterDef.setInitParameters(initParameters);
    }


    @Override
    public Map<String, String> getInitParameters() {
        return filterDef.getInitParameters();
    }


    @Override
    public void addMappingForServletNames(
            EnumSet<DispatcherType> dispatcherTypes, boolean isMatchAfter,
            String... servletNames) {

        if (ctx.isContextInitializedCalled()) {
            String msg = MessageFormat.format(rb.getString(LogFacade.FILTER_REGISTRATION_ALREADY_INIT),
                                              new Object[] {"servlet-name mapping", filterDef.getFilterName(),
                                                            ctx.getName()});
            throw new IllegalStateException(msg);
        }

        if ((servletNames==null) || (servletNames.length==0)) {
            String msg = MessageFormat.format(rb.getString(LogFacade.FILTER_REGISTRATION_MAPPING_SERVLET_NAME_EXCEPTION),
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


    @Override
    public Collection<String> getServletNameMappings() {
        return ctx.getServletNameFilterMappings(getName());
    }


    @Override
    public void addMappingForUrlPatterns(
            EnumSet<DispatcherType> dispatcherTypes, boolean isMatchAfter,
            String... urlPatterns) {

        if (ctx.isContextInitializedCalled()) {
            String msg = MessageFormat.format(rb.getString(LogFacade.FILTER_REGISTRATION_ALREADY_INIT),
                                              new Object[] {"url-pattern mapping", filterDef.getFilterName(),
                                                            ctx.getName()});
            throw new IllegalStateException(msg);
        }

        if ((urlPatterns==null) || (urlPatterns.length==0)) {
            String msg = MessageFormat.format(rb.getString(LogFacade.FILTER_REGISTRATION_MAPPING_URL_PATTERNS_EXCEPTION),
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


    @Override
    public Collection<String> getUrlPatternMappings() {
        return ctx.getUrlPatternFilterMappings(getName());
    }
}

