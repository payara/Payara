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

package com.sun.enterprise.web.session;

import org.apache.catalina.Globals;

import java.net.URLEncoder;

/**
 * Representation of the session cookie configuration element for a web 
 * application.
 *
 * This configuration is not specified as part of the standard deployment
 * descriptor but as part of the iAS 7.0's "extended" web application
 * deployment descriptor - ias-web.xml.
 */

public final class SessionCookieConfig {

    // ----------------------------------------------------- Manifest Constants

    /**
     * The value that allows the JSESSIONID cookie's secure attribute to
     * be configured based on the connection i.e. secure if HTTPS.
     */
    public static final String DYNAMIC_SECURE = "dynamic";

    // ----------------------------------------------------------- Constructors

    /**
     * Construct a new SessionCookieConfig with default properties.
     */
    public SessionCookieConfig() {
        super();
    }

    // ----------------------------------------------------- Instance Variables

    /**
     * The name of the cookie used for session tracking.
     *
     * Default value is JSESSIONID
     */
    private String _name = null;

    /**
     * The pathname that is set when the cookie is created.
     *
     * The default value is the context path at which the web application
     * is installed.  The browser will send the cookie if the pathname for the
     * request contains this pathname. If set to / (slash), the browser will
     * send the cookie to all URLs.
     */
    private String _path = null;

    /**
     * The expiration time in seconds after which the browser expires
     * the cookie.
     *
     * The default value is -1 (never expire) will be set in
     * org.apache.catalina.core.SessionCookieConfigImpl
     */
    private Integer _maxAge = null;

    /**
     * The domain for which the cookie is valid.
     */
    private String _domain = null;

    /**
     * The comment that identifies the session tracking cookie in the
     * browser's cookie file. Applications may choose to provide a more
     * specific name for this cookie.
     */
    private String _comment = null;

    /**
     * When set to "dynamic", the cookie is marked as secure only if the
     * connection on which the request was received is secure. To override this
     * behaviour, the value of this property can be set to "true" or "false". 
     * If set to "true", user agents will use secure means to contact the
     * origin server when sending back the cookie regardless of whether the
     * connection on which the request was received is secure. If set to 
     * "false", user agents do not have to use secure means to contact the
     * origin server when sending back the cookie regardless of whether the
     * connection on which the request was received is secure.
     */
    private String _secure = DYNAMIC_SECURE;

    /**
     * The Boolean (if set) indicates whether the session coookie will
     * be marked as httpOnly.
     *
     * The default value is true will be set in
     * org.apache.catalina.core.SessionCookieConfigImpl
     */
    private Boolean _httpOnly = null;

    /**
     * Construct a new SessionCookieConfig with the specified properties.
     *
     * @param name    The name of the cookie used for session tracking
     * @param path    The pathname that is set when the cookie is created
     * @param maxAge  The expiration time (in seconds) of the session cookie
     *                (-1 indicates 'never expire')
     * @param domain  The domain for which the cookie is valid
     * @param comment The comment that identifies the session tracking cookie
     *                in the cookie file.
     */
    public SessionCookieConfig(String name, String path, int maxAge,
                               String domain, String comment) {
        super();
        setName(name);
        setPath(path);
        setMaxAge(maxAge);
        setDomain(domain);
        setComment(comment);
    }

    // ------------------------------------------------------------- Properties

    /**
     * Set the name of the session tracking cookie (currently not supported).
     */
    public void setName(String name) {
        _name = name;
    }

    /**
     * Return the name of the session tracking cookie.
     */
    public String getName() {
        return _name;
    }

    /**
     * Set the path to use when creating the session tracking cookie.
     */
    public void setPath(String path) {
        _path = path;
    }

    /**
     * Return the path that is set when the session tracking cookie is
     * created.
     */
    public String getPath() {
        return _path;
    }

    /**
     * Set the expiration time for the session cookie.
     */
    public void setMaxAge(Integer maxAge) {
        _maxAge = maxAge;
    }

    /**
     * Return the expiration time for the session cookie.
     */
    public Integer getMaxAge() {
        return _maxAge;
    }

    /**
     * Set the domain for which the cookie is valid.
     */
    public void setDomain(String domain) {
        _domain = domain;
    }

    /**
     * Return the domain for which the cookie is valid.
     */
    public String getDomain() {
        return _domain;
    }

    /**
     * Set the comment that identifies the session cookie.
     */
    public void setComment(String comment) {
        _comment = comment;
        if (comment != null)
            _comment = URLEncoder.encode(comment);
    }

    /**
     * Return the URLEncoded form of the comment that identifies the session
     * cookie.
     */
    public String getComment() {
        return _comment;
    }

    /**
     * Set whether the cookie is marked Secure or not.
     * @param secure Valid values are "dynamic", "true" or "false"
     */
    public void setSecure(String secure) throws IllegalArgumentException {
        if ((secure == null) || (!secure.equalsIgnoreCase("true") &&
                !secure.equalsIgnoreCase("false") &&
                !secure.equalsIgnoreCase(SessionCookieConfig.DYNAMIC_SECURE))) {
            throw new IllegalArgumentException();
        }
        _secure = secure;
    }

    /**
     * Return whether the cookie is to be marked Secure or not.
     * @return "dynamic", "true" or "false"
     */
    public String getSecure() {
        return _secure;
    }

    public void setHttpOnly(Boolean httpOnly) {
        _httpOnly = httpOnly;
    }

    public Boolean getHttpOnly() {
        return _httpOnly;
    }

    // --------------------------------------------------------- Public Methods

    /**
     * Return a String representation of this object.
     */
    public String toString() {

        StringBuilder sb = new StringBuilder("SessionCookieConfig[");
        if (_name != null) {
            sb.append("name=");
            sb.append(_name);
        }
        if (_path != null) {
            sb.append(", path=");
            sb.append(_path);
        }
        sb.append(", maxAge=");
        sb.append(_maxAge);
        if (_domain != null) {
            sb.append(", domain=");
            sb.append(_domain);
        }
        if (_comment != null) {
            sb.append(", comment=");
            sb.append(_comment);
        }
        sb.append(", secure=");
        sb.append(_secure);
        if (_httpOnly != null) {
            sb.append(", httpOnly=");
            sb.append(_httpOnly);
        }
        sb.append("]");
        return (sb.toString());

    }
}
