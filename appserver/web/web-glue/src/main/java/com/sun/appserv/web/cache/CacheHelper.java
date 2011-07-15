/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.appserv.web.cache;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/** CacheHelper interface is an user-extensible interface to customize: 
 *  a) the key generation b) whether to cache the response.
 */
public interface CacheHelper {

    // name of request attributes
    public static final String ATTR_CACHE_MAPPED_SERVLET_NAME = 
                                    "com.sun.appserv.web.cachedServletName";
    public static final String ATTR_CACHE_MAPPED_URL_PATTERN = 
                                    "com.sun.appserv.web.cachedURLPattern";

    public static final int TIMEOUT_VALUE_NOT_SET = -2;

    /** initialize the helper 
     *  @param context the web application context this helper belongs to
     *  @exception Exception if a startup error occurs
     */
    public void init(ServletContext context, Map<String, String> props) throws Exception;

    /** getCacheKey: generate the key to be used to cache this request 
     *  @param request incoming <code>HttpServletRequest</code> object
     *  @return the generated key for this requested cacheable resource.
     */
    public String getCacheKey(HttpServletRequest request);

    /** isCacheable: is the response to given request cachebale? 
     *  @param request incoming <code>HttpServletRequest</code> object
     *  @return <code>true</code> if the response could be cached. or 
     *  <code>false</code> if the results of this request must not be cached.
     */
    public boolean isCacheable(HttpServletRequest request);

    /** isRefreshNeeded: is the response to given request be refreshed?
     *  @param request incoming <code>HttpServletRequest</code> object
     *  @return <code>true</code> if the response needs to be refreshed.
     *  or return <code>false</code> if the results of this request 
     *  don't need to be refreshed.
     */
    public boolean isRefreshNeeded(HttpServletRequest request);

    /** get timeout for the cached response.
     *  @param request incoming <code>HttpServletRequest</code> object
     *  @return the timeout in seconds for the cached response; a return
     *  value of -1 means the response never expires and a value of -2 indicates
     *  helper cannot determine the timeout (container assigns default timeout)
     */ 
    public int getTimeout(HttpServletRequest request);

    /**
     * Stop the helper from active use
     * @exception Exception if an error occurs
     */
    public void destroy() throws Exception;
}
