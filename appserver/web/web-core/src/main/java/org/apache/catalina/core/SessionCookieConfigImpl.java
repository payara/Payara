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
// Portions Copyright [2019-2025] Payara Foundation and/or affiliates
package org.apache.catalina.core;

import org.apache.catalina.Container;
import org.apache.catalina.LogFacade;
import static org.apache.catalina.core.Constants.COOKIE_DOMAIN_ATTR;
import static org.apache.catalina.core.Constants.COOKIE_HTTP_ONLY_ATTR;
import static org.apache.catalina.core.Constants.COOKIE_MAX_AGE_ATTR;
import static org.apache.catalina.core.Constants.COOKIE_PATH_ATTR;
import static org.apache.catalina.core.Constants.COOKIE_SECURE_ATTR;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import jakarta.servlet.SessionCookieConfig;
import static java.lang.String.CASE_INSENSITIVE_ORDER;
import java.util.Collections;
import static java.util.Collections.unmodifiableMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Class that may be used to configure various properties of cookies used for
 * session tracking purposes.
 */
public class SessionCookieConfigImpl implements SessionCookieConfig {

    private String name = DEFAULT_NAME;
    private final StandardContext ctx;
    private Map<String, String> attributes;

    private static final ResourceBundle rb = LogFacade.getLogger().getResourceBundle();

    private final boolean DEFAULT_HTTP_ONLY;
    private static final int DEFAULT_MAX_AGE = -1;
    private static final String DEFAULT_NAME = "JSESSIONID";
    private final String DEFAULT_SECURE;
    private static final String RESERVED_CHAR = ";, ";

    /**
     * Constructor
     */
    public SessionCookieConfigImpl(StandardContext ctx) {
        this.ctx = ctx;
        Container parent = ctx.getParent();
        if (parent instanceof SessionCookieConfigSource) {
            SessionCookieConfigSource source = (SessionCookieConfigSource) parent;
            DEFAULT_HTTP_ONLY = source.isSessionCookieHttpOnly();
            DEFAULT_SECURE = source.getSessionCookieSecure();
        } else {
            DEFAULT_HTTP_ONLY = true;
            DEFAULT_SECURE = "dynamic";
        }
    }

    /**
     * @param name the cookie name to use
     *
     * @throws IllegalStateException if the <code>ServletContext</code> from
     * which this <code>SessionCookieConfig</code> was acquired has already been
     * initialized
     */
    @Override
    public void setName(String name) {
        checkContextInitialized("name");
        this.name = name;
        ctx.setSessionCookieName(name);
    }

    /**
     * @return the cookie name set via {@link #setName}, or
     * <code>JSESSIONID</code> if {@link #setName} was never called
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * @param domain the cookie domain to use
     *
     * @throws IllegalStateException if the <code>ServletContext</code> from
     * which this <code>SessionCookieConfig</code> was acquired has already been
     * initialized
     */
    @Override
    public void setDomain(String domain) {
        checkContextInitialized("domain");
        setAttribute(COOKIE_DOMAIN_ATTR, domain);
    }

    /**
     * @return the cookie domain set via {@link #setDomain}, or
     * <code>null</code> if {@link #setDomain} was never called
     */
    @Override
    public String getDomain() {
        return getAttribute(COOKIE_DOMAIN_ATTR);
    }

    /**
     * @param path the cookie path to use
     *
     * @throws IllegalStateException if the <code>ServletContext</code> from
     * which this <code>SessionCookieConfig</code> was acquired has already been
     * initialized
     */
    @Override
    public void setPath(String path) {
        checkContextInitialized("path");
        setAttribute(COOKIE_PATH_ATTR, path);
    }

    /**
     * @return the cookie path set via {@link #setPath}, or the context path of
     * the <code>ServletContext</code> from which this
     * <code>SessionCookieConfig</code> was acquired if {@link #setPath} was
     * never called
     */
    @Override
    public String getPath() {
        return getAttribute(COOKIE_PATH_ATTR);
    }

    /**
     * @param comment the cookie comment to use
     *
     * @throws IllegalStateException if the <code>ServletContext</code> from
     * which this <code>SessionCookieConfig</code> was acquired has already been
     * initialized
     */
    @Override
    @Deprecated
    public void setComment(String comment) {
        checkContextInitialized("comment");
        setAttribute(Constants.COOKIE_COMMENT_ATTR, comment);
    }

    /**
     * @return the cookie comment set via {@link #setComment}, or
     * <code>null</code> if {@link #setComment} was never called
     */
    @Override
    @Deprecated
    public String getComment() {
        return getAttribute(Constants.COOKIE_COMMENT_ATTR);
    }

    /**
     * @param httpOnly true if the session tracking cookies created on behalf of
     * the <code>ServletContext</code> from which this
     * <code>SessionCookieConfig</code> was acquired shall be marked as
     * <i>HttpOnly</i>, false otherwise
     *
     * @throws IllegalStateException if the <code>ServletContext</code> from
     * which this <code>SessionCookieConfig</code> was acquired has already been
     * initialized
     */
    @Override
    public void setHttpOnly(boolean httpOnly) {
        checkContextInitialized("httpOnly");
        setAttribute(COOKIE_HTTP_ONLY_ATTR, String.valueOf(httpOnly));
    }

    /**
     * @return true if the session tracking cookies created on behalf of the
     * <code>ServletContext</code> from which this
     * <code>SessionCookieConfig</code> was acquired will be marked as
     * <i>HttpOnly</i>, false otherwise
     */
    @Override
    public boolean isHttpOnly() {
        String value = getAttribute(COOKIE_HTTP_ONLY_ATTR);
        return value == null ? DEFAULT_HTTP_ONLY : Boolean.parseBoolean(value);
    }

    /**
     * @param secure true if the session tracking cookies created on behalf of
     * the <code>ServletContext</code> from which this
     * <code>SessionCookieConfig</code> was acquired shall be marked as
     * <i>secure</i> even if the request that initiated the corresponding
     * session is using plain HTTP instead of HTTPS, and false if they shall be
     * marked as <i>secure</i> only if the request that initiated the
     * corresponding session was also secure
     *
     * @throws IllegalStateException if the <code>ServletContext</code> from
     * which this <code>SessionCookieConfig</code> was acquired has already been
     * initialized
     */
    @Override
    public void setSecure(boolean secure) {
        checkContextInitialized("secure");
        setAttribute(COOKIE_SECURE_ATTR, String.valueOf(secure));
    }

    /**
     * @return true if the session tracking cookies created on behalf of the
     * <code>ServletContext</code> from which this
     * <code>SessionCookieConfig</code> was acquired will be marked as
     * <i>secure</i> even if the request that initiated the corresponding
     * session is using plain HTTP instead of HTTPS, and false if they will be
     * marked as <i>secure</i>
     * only if the request that initiated the corresponding session was also
     * secure
     */
    @Override
    public boolean isSecure() {
        String value = getAttribute(COOKIE_SECURE_ATTR);
        if (value != null) {
            return Boolean.parseBoolean(value);
        }
        if (DEFAULT_SECURE.equals("DYNAMIC")) {
            // Return false as Request.configureSessionCookie() already checks if
            // the request is secure.
            return false;
        }
        return Boolean.parseBoolean(DEFAULT_SECURE);
    }

    @Override
    public void setMaxAge(int maxAge) {
        checkContextInitialized("maxAge");
        setAttribute(COOKIE_MAX_AGE_ATTR, String.valueOf(maxAge));
    }

    @Override
    public int getMaxAge() {
        String value = getAttribute(COOKIE_MAX_AGE_ATTR);
        return value == null ? DEFAULT_MAX_AGE : Integer.parseInt(value);
    }

    /**
     * Sets the value for the given session cookie attribute.
     *
     * @param name Name of attribute to set, case insensitive
     * @param value Value of attribute
     *
     * @throws IllegalStateException if the associated ServletContext has
     * already been initialized
     *
     * @throws IllegalArgumentException If the attribute name is null or
     * contains any characters not permitted for use in Cookie names.
     *
     * @throws NumberFormatException If the attribute is known to be numerical
     * but the provided value cannot be parsed to a number.
     */
    @Override
    public void setAttribute(String name, String value) {
        checkContextInitialized("attribute");
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("attribute name cannot be null");
        }
        if (hasReservedCharacters(name)) {
            throw new IllegalArgumentException("Invalid attribute name " + name);
        }

        if (COOKIE_MAX_AGE_ATTR.equalsIgnoreCase(name) && value != null) {
            Integer.parseInt(value);
        }
        if (this.attributes == null) {
            this.attributes = new TreeMap<>(CASE_INSENSITIVE_ORDER);
        }
        this.attributes.put(name, value);
    }

    /**
     * Get the value for a given session cookie attribute.
     *
     * @param name Name of attribute
     *
     * @return Value of specified attribute
     *
     */
    @Override
    public String getAttribute(String name) {
        if (this.attributes == null) {
            return null;
        }
        return this.attributes.get(name);
    }

    /**
     * Get all the session cookie attributes in case insensitive order
     *
     * @return A read-only Map of attributes.
     *
     */
    @Override
    public Map<String, String> getAttributes() {
        if (this.attributes == null) {
            Collections.emptyMap();
        }
        return unmodifiableMap(this.attributes);
    }

    /**
     * Validate if the associated ServletContext has already been initialized
     */
    private void checkContextInitialized(String param) {
        if (ctx.isContextInitializedCalled()) {
            String msg = MessageFormat.format(
                    rb.getString(LogFacade.SESSION_COOKIE_CONFIG_ALREADY_INIT),
                    new Object[]{param, ctx.getName()}
            );
            throw new IllegalStateException(msg);
        }
    }

    /*
     * validate if the attribute name contains a reserved characters (semi-colon, comma and white space).
     * 
     * @param value the <code>String</code> to be tested
     *
     * @return <code>true</code> if the <code>String</code> contains a reserved character;
     * <code>false</code> otherwise
     */
    private static boolean hasReservedCharacters(String value) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (RESERVED_CHAR.indexOf(c) != -1) {
                return true;
            }
        }
        return false;
    }

    public String getDefaultSecure() {
        return DEFAULT_SECURE;
    }
}
