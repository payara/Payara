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

package com.sun.enterprise.web;

import com.sun.appserv.web.cache.CacheManager;
import com.sun.appserv.web.cache.mapping.CacheMapping;
import com.sun.appserv.web.cache.mapping.ConstraintField;
import com.sun.appserv.web.cache.mapping.Field;
import com.sun.appserv.web.cache.mapping.ValueConstraint;
import com.sun.enterprise.config.serverbeans.ConfigBeansUtilities;
import com.sun.enterprise.deployment.runtime.web.SunWebApp;
import org.glassfish.web.LogFacade;
import org.glassfish.web.deployment.runtime.*;
import org.apache.catalina.deploy.FilterDef;
import org.apache.catalina.deploy.FilterMap;

import javax.servlet.DispatcherType;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * configures the cache for the application
 */
public final class CacheModule {
    public final static String CACHING_FILTER_CLASSNAME = 
                            "com.sun.appserv.web.cache.filter.CachingFilter";
    public final static String DEFAULT_CACHE_HELPER_CLASSNAME =
                            "com.sun.appserv.web.cache.DefaultCacheHelper";

    public static final Logger logger = LogFacade.getLogger();

    private static String trim(String str) {
        if (str != null)
            return str.trim();
        return str;
    }

    /**
     * configure ias-web response cache
     * @param app WebModule containing the cache
     * @param bean ias-web app config bean
     * @throws Exception
     *
     * read the configuration and setup the runtime support for caching in a
     * application.
     */
    public static CacheManager configureResponseCache(WebModule app, 
                                SunWebApp bean) throws Exception  {

        Cache cacheConfig = ((SunWebAppImpl)bean).getCache();

        // is cache configured?
        if (cacheConfig == null) {
            return null;
        }

        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, LogFacade.CONFIGURE_CACHE, app.getPath());
        }

        // create the CacheManager object for this app
        CacheManager manager = new CacheManager();

        String name, value;
        value = cacheConfig.getAttributeValue(Cache.ENABLED);
        if (value != null) {
            boolean enabled = ConfigBeansUtilities.toBoolean(value);
            manager.setEnabled(enabled);
        }

        // set cache element's attributes and properties
        value = cacheConfig.getAttributeValue(Cache.MAX_ENTRIES);
        if (value != null) {
            try {
                int maxEntries = Integer.parseInt(value.trim());
                manager.setMaxEntries(maxEntries);
            } catch (NumberFormatException e) {
                // XXX need error message
                throw new Exception("invalid max-entries", e);
            }
        }

        value = cacheConfig.getAttributeValue(Cache.TIMEOUT_IN_SECONDS);
        if (value != null) {
            try {
                int defaultTimeout = Integer.parseInt(value.trim());
                manager.setDefaultTimeout(defaultTimeout);
            } catch (NumberFormatException e) {
                // XXX need error message
                throw new Exception("invalid timeout", e);
            }
        }

        WebProperty[] props = cacheConfig.getWebProperty();
        for (int i = 0; i < props.length; i++) {
            name = props[i].getAttributeValue(WebProperty.NAME);
            value = props[i].getAttributeValue(WebProperty.VALUE);

            manager.addProperty(name, value);
        }
        
        // configure the default cache-helper 
        DefaultHelper defHelperConfig = cacheConfig.getDefaultHelper();

        HashMap<String, String> map = new HashMap<String, String>();
        if (defHelperConfig != null) {
            props = defHelperConfig.getWebProperty();
            for (int i = 0; i < props.length; i++) {
                name = props[i].getAttributeValue(WebProperty.NAME);
                value = props[i].getAttributeValue(WebProperty.VALUE);

                map.put(name, value);
            }
        }
        manager.setDefaultHelperProps(map);

        // configure custom cache-helper classes
        for (int i = 0; i < cacheConfig.sizeCacheHelper(); i++) {
            CacheHelper helperConfig = cacheConfig.getCacheHelper(i);

            String helperName = helperConfig.getAttributeValue(
                CacheHelper.NAME); 
            HashMap<String, String> helperProps = new HashMap<String, String>();
            props = helperConfig.getWebProperty();
            for (int j = 0; j < props.length; j++) {
                name = props[i].getAttributeValue(WebProperty.NAME);
                value = props[i].getAttributeValue(WebProperty.VALUE);

                helperProps.put(name, value);
            }
            helperProps.put("class-name", 
                            helperConfig.getAttributeValue(
                            CacheHelper.CLASS_NAME));

            manager.addCacheHelperDef(helperName, helperProps);
        }

        // for each cache-mapping, create CacheMapping, setup the filter
        for (int i = 0; i < cacheConfig.sizeCacheMapping(); i++) {
            org.glassfish.web.deployment.runtime.CacheMapping
                            mapConfig = cacheConfig.getCacheMapping(i);
            
            CacheMapping mapping = new CacheMapping();
            configureCacheMapping(mapConfig, mapping, logger);

            // use filter's name to refer to setup the filter
            String filterName = CACHING_FILTER_CLASSNAME + i;

            /** 
             * all cache-mapings are indexed by the unique filter-name;
             * DefaultCacheHelper uses this name to access the mapping.
             */
            manager.addCacheMapping(filterName, mapping);

            // setup the ias CachingFilter definition with the context
            FilterDef filterDef = new FilterDef();
            filterDef.setFilterName(filterName);
            filterDef.setFilterClassName(CACHING_FILTER_CLASSNAME);

            if (mapping.getServletName() != null) {
                filterDef.addInitParameter("servletName",
                                           mapping.getServletName());
            }
            if (mapping.getURLPattern() != null) {
                filterDef.addInitParameter("URLPattern",
                                           mapping.getURLPattern());
            }

            app.addFilterDef(filterDef);

            // setup the mapping for the specified servlet-name or url-pattern
            FilterMap filterMap = new FilterMap();
            filterMap.setServletName(mapping.getServletName());
            filterMap.setURLPattern(mapping.getURLPattern());
            String[] dispatchers = mapConfig.getDispatcher();
            if (dispatchers != null) {
                EnumSet<DispatcherType> dispatcherTypes = null;
                for (String dispatcher : dispatchers) {
                    // calls to FilterMap.setDispatcher are cumulative
                    if (dispatcherTypes == null) {
                        dispatcherTypes = EnumSet.of(
                            Enum.valueOf(DispatcherType.class, dispatcher));
                    } else {
                        dispatcherTypes.add(
                            Enum.valueOf(DispatcherType.class, dispatcher));
                    }
                }
                filterMap.setDispatcherTypes(dispatcherTypes);
            }
            filterMap.setFilterName(filterName);
            app.addFilterMap(filterMap);

            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE,
                        LogFacade.CACHING_FILTER_ADDED,
                        new Object[] {mapping.getServletName(), mapping.getURLPattern()});
            }
        }
        
        manager.setServletContext(app.getServletContext());
        return manager;
    }

    /**
     * configure ias-web cache-mapping
     * @param Catalina context
     * @param bean ias-web app cache-mapping config bean
     * @throws Exception
     *
     */
    private static void configureCacheMapping(
            org.glassfish.web.deployment.runtime.CacheMapping mapConfig,
            CacheMapping mapping,
            Logger logger) throws Exception {
        String name, scope, value, expr;

        /**
         * <cache-mapping  ((servlet-name|url-pattern)..)
         */
        mapping.setServletName(trim(mapConfig.getServletName()));
        mapping.setURLPattern(trim(mapConfig.getURLPattern()));

        // resolve the helper for this mapping
        String helperRef = mapConfig.getCacheHelperRef();
        if (helperRef == null) {
            helperRef = "default";
        }
        mapping.setHelperNameRef(helperRef);

        /** 
         * <timeout>600</timeout>
         * <timeout name="cacheTimeout" scope="request.attribute" />
         */
        value = mapConfig.getTimeout();
        if (value != null) {
            try {
                mapping.setTimeout(Integer.parseInt(value.trim()));
            } catch (NumberFormatException e) {
                throw new Exception("invalid timeout", e);
            }
        } else {
            // XXX: get the timeout as a field?
            name = mapConfig.getAttributeValue(
                    org.glassfish.web.deployment.runtime.CacheMapping.TIMEOUT,
                    org.glassfish.web.deployment.runtime.CacheMapping.NAME);
            scope = mapConfig.getAttributeValue(
                    org.glassfish.web.deployment.runtime.CacheMapping.TIMEOUT,
                    org.glassfish.web.deployment.runtime.CacheMapping.SCOPE);
            if (name != null && scope != null)
                mapping.setTimeoutField(new Field(name, scope));
        }

        /**
         * <refresh-field name="refreshNow" scope="request.attribute" />
         */

        name = mapConfig.getAttributeValue(
                org.glassfish.web.deployment.runtime.CacheMapping.REFRESH_FIELD,
                org.glassfish.web.deployment.runtime.CacheMapping.NAME);
        scope = mapConfig.getAttributeValue(
                org.glassfish.web.deployment.runtime.CacheMapping.REFRESH_FIELD,
                org.glassfish.web.deployment.runtime.CacheMapping.SCOPE);
        if (name != null && scope != null) {
            Field refreshField = new Field(name, scope);
            mapping.setRefreshField(refreshField);
        }

        /** <http-method> GET </http-method>
         *  <http-method> POST </http-method> 
         */
        if (mapConfig.sizeHttpMethod() > 0) {
            mapping.setMethods(mapConfig.getHttpMethod());
        }

        /**
         * <key-field name="foo" scope="request.parameter"/>
         */
        for (int i = 0; i < mapConfig.sizeKeyField(); i++) {
            name = mapConfig.getAttributeValue(
                    org.glassfish.web.deployment.runtime.CacheMapping.KEY_FIELD,
                i, org.glassfish.web.deployment.runtime.CacheMapping.NAME);
            scope = mapConfig.getAttributeValue(
                    org.glassfish.web.deployment.runtime.CacheMapping.KEY_FIELD,
                i, org.glassfish.web.deployment.runtime.CacheMapping.SCOPE);
            if (name != null && scope != null) {            
                mapping.addKeyField(new Field(name, scope));

                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, LogFacade.KEY_FIELD_ADDED, new Object[]{name, scope});
                }
            }
        }

        /**
         * <constraint-field name="foo" scope="request.parameter">
         *   <value match-expr="equals"> 200 </value>
         */
        for (int i = 0; i < mapConfig.sizeConstraintField(); i++) {
            org.glassfish.web.deployment.runtime.ConstraintField
                                fieldConfig = mapConfig.getConstraintField(i);

            name = fieldConfig.getAttributeValue(
                    org.glassfish.web.deployment.runtime.ConstraintField.NAME);
            scope = fieldConfig.getAttributeValue(
                    org.glassfish.web.deployment.runtime.ConstraintField.SCOPE);
            ConstraintField constraintField = 
                                        new ConstraintField(name, scope);

            value = fieldConfig.getAttributeValue(org.glassfish.web.deployment.runtime.ConstraintField.CACHE_ON_MATCH);
            if (value != null)
                constraintField.setCacheOnMatch(ConfigBeansUtilities.toBoolean(value));

            value = fieldConfig.getAttributeValue(org.glassfish.web.deployment.runtime.ConstraintField.CACHE_ON_MATCH_FAILURE);
            if (value != null)
                constraintField.setCacheOnMatchFailure(
                                    ConfigBeansUtilities.toBoolean(value));


            // now set the value's and the match expressions
            for (int j = 0; j < fieldConfig.sizeValue(); j++) {
                value = fieldConfig.getValue(j).trim();
                expr = fieldConfig.getAttributeValue(
                        org.glassfish.web.deployment.runtime.ConstraintField.VALUE, j, org.glassfish.web.deployment.runtime.ConstraintField.MATCH_EXPR);
                
                ValueConstraint constraint = new ValueConstraint(value, expr);
                value = fieldConfig.getAttributeValue(org.glassfish.web.deployment.runtime.ConstraintField.VALUE, j, org.glassfish.web.deployment.runtime.ConstraintField.CACHE_ON_MATCH);
                if (value != null) {
                    constraint.setCacheOnMatch(ConfigBeansUtilities.toBoolean(value));
                }
                value = fieldConfig.getAttributeValue(org.glassfish.web.deployment.runtime.ConstraintField.VALUE, j, org.glassfish.web.deployment.runtime.ConstraintField.CACHE_ON_MATCH_FAILURE);
                if (value != null) {
                    constraint.setCacheOnMatchFailure(
                                    ConfigBeansUtilities.toBoolean(value));
                }
                constraintField.addConstraint(constraint);

                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, LogFacade.CONSTRAINT_ADDED, constraint.toString());
                }
            }

            mapping.addConstraintField(constraintField);

            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE,
                        LogFacade.CONSTRAINT_FIELD_ADDED,
                        new Object[]{
                                name,
                                scope,
                                constraintField.getCacheOnMatch(),
                                constraintField.getCacheOnMatchFailure()});
            }
        }
    }
}
