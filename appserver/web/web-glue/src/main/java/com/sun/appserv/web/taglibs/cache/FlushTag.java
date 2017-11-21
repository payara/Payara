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

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.TagSupport;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.web.LogFacade;


/**
 * FlushTag is a JSP tag that is used with the CacheTag. The FlushTag
 * allows you to invalidate a complete cache or a particular cache element
 * identified by the key.
 *
 * Usage Example:
 * <%@ taglib prefix="ias" uri="Sun ONE Application Server Tags" %> 
 * <ias:flush key="<%= cacheKey %>" />
 */
public class FlushTag extends TagSupport {
    /**
     * The key for the cache entry that needs to be flushed.
     */
    private String _key;

    /**
     * This specifies the scope of the cache that needs to be flushed.
     */
    private int _scope = PageContext.APPLICATION_SCOPE;

    /**
     * The logger to use for logging ALL web container related messages.
     */
    private static final Logger _logger = LogFacade.getLogger();

    // ---------------------------------------------------------------------
    // Tag logic

    /**
     * doStartTag is called when the flush tag is encountered. By
     * the time this is called, the tag attributes are already set.
     *
     * @throws JspException the standard exception thrown
     * @return SKIP_BODY since the tag should be empty
     */
    public int doStartTag()
        throws JspException
    {
        // get the cache from the specified scope
        Cache cache = CacheUtil.getCache(pageContext, _scope);

        // generate the cache key using the user specified key.
   
        if (_key != null) {
            String key = CacheUtil.generateKey(_key, pageContext);

            // remove the entry for the key
            cache.remove(key);

            if (_logger.isLoggable(Level.FINE))
                _logger.log(Level.FINE, LogFacade.FLUSH_TAG_CLEAR_KEY, key);
        } else {
            // clear the entire cache
            cache.clear();

            if (_logger.isLoggable(Level.FINE))
                _logger.log(Level.FINE, LogFacade.FLUSH_TAG_CLEAR_CACHE);
        }

        return SKIP_BODY;
    }

    /**
     * doEndTag just resets all the valiables in case the tag is reused
     *
     * @throws JspException the standard exception thrown
     * @return always returns EVAL_PAGE since we want the entire jsp evaluated
     */
    public int doEndTag()
        throws JspException
    {
        _key = null;
        _scope = PageContext.APPLICATION_SCOPE;

        return EVAL_PAGE;
    }

    // ---------------------------------------------------------------------
    // Attribute setters

    /**
     * This is set a key for the cache element that needs to be cleared
     */
    public void setKey(String key) {
        if (key != null && key.length() > 0)
            _key = key;
    }

    /**
     * Sets the scope of the cache.
     *
     * @param scope the scope of the cache
     *
     * @throws IllegalArgumentException if the specified scope is different
     * from request, session, and application
     */
    public void setScope(String scope) {
        _scope = CacheUtil.convertScope(scope);
    }
}
