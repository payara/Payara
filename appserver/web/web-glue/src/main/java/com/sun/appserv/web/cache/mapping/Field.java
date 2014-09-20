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

package com.sun.appserv.web.cache.mapping;

import org.glassfish.logging.annotation.LogMessageInfo;

import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.logging.Logger;

public class Field {

    private static final Logger _logger = com.sun.enterprise.web.WebContainer.logger;

    /**
     * The resource bundle containing the localized message strings.
     */
    private static final ResourceBundle _rb = _logger.getResourceBundle();

    @LogMessageInfo(
            message = "Incorrect scope value [{0}] for web application cache field name [{1}]",
            level = "WARNING")
    public static final String CACHE_MAPPING_INCORRECT_SCOPE = "AS-WEB-GLUE-00016";

    // field name and scope 
    protected String name; 

    // scope defs in Constants
    protected int scope; 

    /**
     * create a new cache field, given a string representation of the scope
     * @param name name of this field
     * @param scope scope of this field
     */
    public Field (String name, String scope) throws IllegalArgumentException {
        this.name = name;
        this.scope = parseScope(scope);
    }

    /**
     * set the associated name
     * @param name name of this field
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * set the associated scope
     * @param scope scope of this field
     */
    public void setScope(int scope) {
        this.scope = scope;
    }

    /**
     * set the associated scope
     * @param scope scope of this field
     */
    private int parseScope(String value) throws IllegalArgumentException {
        int scope;
        if ("context.attribute".equals(value))
            scope = Constants.SCOPE_CONTEXT_ATTRIBUTE;
        else if ("request.header".equals(value))
            scope = Constants.SCOPE_REQUEST_HEADER;
        else if ("request.parameter".equals(value))
            scope = Constants.SCOPE_REQUEST_PARAMETER;
        else if ("request.cookie".equals(value))
            scope = Constants.SCOPE_REQUEST_COOKIE;
        else if ("request.attribute".equals(value))
            scope = Constants.SCOPE_REQUEST_ATTRIBUTE;
        else if ("session.attribute".equals(value))
            scope = Constants.SCOPE_SESSION_ATTRIBUTE;
        else if ("session.id".equals(value))
            scope = Constants.SCOPE_SESSION_ID;
        else  {
            String msg = _rb.getString(CACHE_MAPPING_INCORRECT_SCOPE);
            Object[] params = { value, name };
            msg = MessageFormat.format(msg, params);

            throw new IllegalArgumentException(msg);
        }
        return scope;
    }

    /**
     * get the associated name
     * @return the name of this field
     */
    public String getName() {
        return name;
    }

    /**
     * get the associated scope
     * @return the scope of this field
     */
    public int getScope() {
        return scope;
    }

    /** get the field value by looking up in the given scope
     *  @param context <code>ServletContext</code> underlying web app context
     *  @param request <code>HttpServletRequest</code>
     *  @return field value in the scope
     */
    public Object getValue(ServletContext context, 
                           HttpServletRequest request) {

        Object value = null;
        switch (scope) {
            case Constants.SCOPE_CONTEXT_ATTRIBUTE:
                    value = context.getAttribute(name);
                    break;
            case Constants.SCOPE_REQUEST_HEADER:
                    value = request.getHeader(name);
                    break;
            case Constants.SCOPE_REQUEST_PARAMETER:
                    value = request.getParameter(name);
                    break;
            case Constants.SCOPE_REQUEST_COOKIE:
                    Cookie cookies[] = request.getCookies();
                    for (int i = 0; i < cookies.length; i++) {
                        if (name.equals(cookies[i].getName())) {
                            value = cookies[i].getValue();
                            break;
                        }
                    }
                    break;
            case Constants.SCOPE_REQUEST_ATTRIBUTE:
                    value = request.getAttribute(name);
                    break;
            case Constants.SCOPE_SESSION_ID:
                    {
                        HttpSession session = request.getSession(false);
                        if (session != null) {
                            value = session.getId();
                        }
                    }
                    break;
            case Constants.SCOPE_SESSION_ATTRIBUTE:
                    {
                        HttpSession session = request.getSession(false);
                        if (session != null) {
                            value = session.getAttribute(name);
                        }
                    }
                    break;
            default:
                value = null;
                break;
        }
        return value;
    }
}
