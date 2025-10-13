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
 */
// Portions Copyright [2018-2021] [Payara Foundation and/or its affiliates]
package com.sun.appserv.web.cache;

import com.sun.appserv.web.cache.mapping.CacheMapping;
import com.sun.appserv.web.cache.mapping.ConstraintField;
import com.sun.appserv.web.cache.mapping.Field;
import org.glassfish.web.LogFacade;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/** DefaultCacheHelper interface is the built-in implementation of the
 *  <code>CacheHelper</code> interface to aide in:
 *  a) the key generation b) whether to cache the response.
 *  There is one CacheHelper instance per web application.
 */
public class DefaultCacheHelper implements CacheHelper {

    private static final String[] KEY_PREFIXES = {
        "", "ca.", "rh.", "rp.", "rc.", "ra.", "sa.", "si." };

    public static final String ATTR_CACHING_FILTER_NAME =
                                "com.sun.ias.web.cachingFilterName";
    public static final String PROP_KEY_GENERATOR_ATTR_NAME =
                                "cacheKeyGeneratorAttrName";

    private static final Logger LOGGER = LogFacade.getLogger();

    private ServletContext context;

    // cache manager
    private CacheManager manager;
    private String attrKeyGenerator = null;
    private boolean isKeyGeneratorChecked = false;
    private CacheKeyGenerator keyGenerator;

    /**
     * set the CacheManager for this application
     * @param manager associated with this application
     */
    public void setCacheManager(CacheManager manager) {
        this.manager = manager;
    }

    /***         CacheHelper methods          **/

    /**
     * initialize this helper
     * @param context the web application context this helper belongs to
     * @param props helper properties
     */
    public void init(ServletContext context, Map<String, String> props) {
        this.context = context;
        this.attrKeyGenerator = props.get(PROP_KEY_GENERATOR_ATTR_NAME);
    }

    /**
     * cache-mapping for this servlet-name or the URLpattern
     * @param request incoming request
     * @return cache-mapping object; uses servlet name or the URL pattern
     * to lookup in the CacheManager for the mapping.
     */
    private Optional<CacheMapping> lookupCacheMapping(HttpServletRequest request) {
        String name = (String)request.getAttribute(ATTR_CACHING_FILTER_NAME);
        return manager != null
            ? Optional.ofNullable(manager.getCacheMapping(name))
            : Optional.empty();
    }

    /** getCacheKey: generate the key to be used to cache this request
     *  @param request incoming <code>HttpServletRequest</code>
     *  @return key string used to access the cache entry.
     *  Key is composed of: servletPath + a concatenation of the field values in
     *  the request; all key field names must be found in the appropriate scope.
     */
    public String getCacheKey(HttpServletRequest request) {

        if (!isKeyGeneratorChecked && attrKeyGenerator != null) {
            try {
                keyGenerator = (CacheKeyGenerator)
                                context.getAttribute(attrKeyGenerator);
            } catch (ClassCastException cce){
                LOGGER.log(Level.WARNING, LogFacade.CACHE_DEFAULT_HELP_ILLEGAL_KET_GENERATOR, cce);
            }
            isKeyGeneratorChecked = true;
        }

        if (keyGenerator != null) {
            String key = keyGenerator.getCacheKey(context, request);
            if (key != null)
                return key;
        }

        StringBuilder sb = new StringBuilder(128);
        sb.append(request.getServletPath());

        // cache mapping associated with the request
        Optional<CacheMapping> mapping = lookupCacheMapping(request);

        if (mapping.isPresent()) {
            // append the key fields
            Field[] keys = mapping.get().getKeyFields();
            for (Field key : keys) {
                Object value = key.getValue(context, request);

                // all defined key field must be present
                if (value == null) {
                    if (LOGGER.isLoggable(Level.FINE)) {
                        LOGGER.log(Level.FINE, LogFacade.REQUIRED_KEY_FIELDS_NOT_FOUND, request.getServletPath());
                    }
                    return null;
                }

                sb.append(";");
                sb.append(KEY_PREFIXES[key.getScope()]);
                sb.append(key.getName());
                sb.append("=");
                sb.append(value);
            }
        }

        return sb.toString();
    }

    /** isCacheable: is the response to given request cachebale?
     *  @param request incoming <code>HttpServletRequest</code> object
     *  @return <code>true</code> if the response could be cached.
     *  or return <code>false</code> if the results of this request
     *  must not be cached.
     *
     *  Applies pre-configured cacheability constraints in the cache-mapping;
     *  all constraints must pass for this to be cacheable.
     */
    public boolean isCacheable(HttpServletRequest request) {
        boolean result = false;

        // cache mapping associated with the request
        Optional<CacheMapping> mapping = lookupCacheMapping(request);

        if (mapping.isPresent()) {
            // check if the method is in the allowed methods list
            if (mapping.get().findMethod(request.getMethod())) {
                result = true;

                ConstraintField fields[] = mapping.get().getConstraintFields();
                // apply all the constraints
                for (ConstraintField field : fields) {
                    if (!field.applyConstraints(context, request)) {
                        result = false;
                        break;
                    }
                }
            }
        }
        return result;
    }

    /** isRefreshNeeded: is the response to given request be refreshed?
     *  @param request incoming <code>HttpServletRequest</code> object
     *  @return <code>true</code> if the response needs to be refreshed.
     *  or return <code>false</code> if the results of this request
     *  don't need to be refreshed.
     *
     *  XXX: 04/16/02 right now there is no configurability for this in
     *  ias-web.xml; should add a refresh-field element there:
     *  <refresh-field name="refresh" scope="request.parameter" />
     */
    public boolean isRefreshNeeded(HttpServletRequest request) {
        boolean result = false;

        // cache mapping associated with the request
        Optional<CacheMapping> mapping = lookupCacheMapping(request);
        if (mapping.isPresent()) {
            Field field = mapping.get().getRefreshField();
            if (field != null) {
                Object value = field.getValue(context, request);
                // the field's string representation must be "true" or "false"
                if (value != null && "true".equals(value.toString())) {
                    result = true;
                }
            }
        }
        return result;
    }

    /** get timeout for the cacheable data in this request
     *  @param request incoming <code>HttpServletRequest</code> object
     *  @return either the statically specified value or from the request
     *  fields. If not specified, get the timeout defined for the
     *  cache element.
     */
    public int getTimeout(HttpServletRequest request) {
        int result = CacheHelper.TIMEOUT_VALUE_NOT_SET;
        // cache mapping associated with the request
        Optional<CacheMapping> mapping = lookupCacheMapping(request);
        if (mapping.isPresent()) {
            // get the statically configured value, if any
            result = mapping.get().getTimeout();
            // if the field is not defined, return the configured value
            Field field = mapping.get().getTimeoutField();
            if (field != null) {
                Object value = field.getValue(context, request);
                if (value != null) {
                    try {
                        // Integer type timeout object
                        result = Integer.valueOf(value.toString());
                    } catch (NumberFormatException cce) { }
                }
            }
        }
        // Note: this could be CacheHelper.TIMEOUT_NOT_SET
        return result;
    }

    /**
     * Stop this Context component.
     * @exception Exception if a shutdown error occurs
     */
    public void destroy() throws Exception {
    }
}
