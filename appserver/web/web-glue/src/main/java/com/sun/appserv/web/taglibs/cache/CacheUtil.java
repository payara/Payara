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

package com.sun.appserv.web.taglibs.cache;

import com.sun.appserv.util.cache.Cache;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.PageContext;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import org.glassfish.web.LogFacade;

/**
 * CacheUtil has utility methods used by the cache tag library.
 */
public class CacheUtil {

    private static final Logger _logger = LogFacade.getLogger();

    /**
     * The resource bundle containing the localized message strings.
     */
    private static final ResourceBundle _rb = _logger.getResourceBundle();

    private static final String PAGE_SCOPE = "page";   
    private static final String REQUEST_SCOPE = "request";   
    private static final String SESSION_SCOPE = "session";   
    private static final String APPLICATION_SCOPE = "application";

    /**
     * This is used to get the cache itself. The cache is stored as an
     * attribute in the specified scope.
     * @return the cache object
     */
    public static Cache getCache(PageContext pc, int scope)
    {
        return (Cache)pc.getAttribute(Constants.JSPTAG_CACHE_KEY, scope);
    }

    /**
     * This function generates the key to the cache. It creates the key
     * by suffixing the servlet path with either the user-specified key or 
     * by keeping a counter in the request attribute which it will
     * increment each time so that multiple cache tags in a page each get
     * a unique key.
     * @return the generated key
     */
    public static String generateKey(String key, PageContext pc)
    {
        HttpServletRequest req = (HttpServletRequest)pc.getRequest();

        // use the key as the suffix by default
        String suffix = key;
        if (suffix == null) {
            String saved = (String)req.getAttribute(Constants.JSPTAG_COUNTER_KEY);

            if (saved == null)
                suffix = "1";
            else
                suffix = Integer.toString(Integer.parseInt(saved) + 1);

            req.setAttribute(Constants.JSPTAG_COUNTER_KEY, suffix);
        }
        
        // concatenate the servlet path and the suffix to generate key
        return req.getServletPath() + '_' + suffix;
    }


    /*
     * Converts the string representation of the given scope into an int.
     *
     * @param scope The string representation of the scope
     *
     * @return The corresponding int constant
     *
     * @throws IllegalArgumentException if the specified scope is different
     * from request, session, and application
     */
    public static int convertScope(String scope) {

        int ret;

        if (REQUEST_SCOPE.equalsIgnoreCase(scope)) {
            ret = PageContext.REQUEST_SCOPE;
	} else if (SESSION_SCOPE.equalsIgnoreCase(scope)) {
            ret = PageContext.SESSION_SCOPE;
        } else if (APPLICATION_SCOPE.equalsIgnoreCase(scope)) {
            ret = PageContext.APPLICATION_SCOPE;
        } else {
            String msg = _rb.getString(LogFacade.ILLEGAL_SCOPE);
            msg = MessageFormat.format(msg, new Object[] { scope });
            throw new IllegalArgumentException(msg);
        }

        return ret;
    }
}
