/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017 Oracle and/or its affiliates. All rights reserved.
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
 *
 * This file incorporates work covered by the following copyright and
 * permission notice:
 *
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.catalina.core;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletRequestWrapper;
import jakarta.servlet.SessionTrackingMode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.PushBuilder;

import org.apache.catalina.Context;
import org.apache.catalina.LogFacade;
import org.apache.catalina.connector.Request;
import org.glassfish.grizzly.http.Cookie;
import org.glassfish.grizzly.http.server.http2.PushEvent;
import org.glassfish.grizzly.http.util.Header;
import org.glassfish.grizzly.http.util.MimeHeaders;
import org.glassfish.grizzly.http2.Http2Stream;

/**
 * Implementation of jakarta.servlet.http.PushBuilder.
 *
 * @author Shing Wai Chan
 */
public class ApplicationPushBuilder implements PushBuilder {

    private static final ResourceBundle rb = LogFacade.getLogger().getResourceBundle();

    private static final Header[] REMOVE_HEADERS = {
            Header.ETag,
            Header.IfModifiedSince,
            Header.IfNoneMatch,
            Header.IfRange,
            Header.IfUnmodifiedSince,
            Header.IfMatch,
            Header.LastModified,
            Header.Referer,
            Header.AcceptRanges,
            Header.Range,
            Header.AcceptRanges,
            Header.ContentRange,
            Header.Authorization,
            Header.ProxyAuthenticate,
            Header.ProxyAuthorization,
    };

    // RFC 7232
    private static final Header[] CONDITIONAL_HEADERS = {
            Header.IfModifiedSince,
            Header.IfNoneMatch,
            Header.IfRange,
            Header.IfUnmodifiedSince,
            Header.IfMatch,
    };

    private final HttpServletRequest baseRequest;
    private final Request catalinaRequest;
    private final org.glassfish.grizzly.http.server.Request coyoteRequest;
    private final String sessionCookieName;
    private final boolean addSessionCookie;
    private final boolean addSessionPathParameter;

    private final MimeHeaders headers = new MimeHeaders();
    private final List<Cookie> cookies = new ArrayList<>();
    private String method = "GET";
    private String path;
    private String queryString;
    private String sessionId;

    public ApplicationPushBuilder(HttpServletRequest request) {
        baseRequest = request;

        // Need a reference to the CoyoteRequest in order to process the push
        ServletRequest current = request;
        while (current instanceof ServletRequestWrapper) {
            current = ((ServletRequestWrapper) current).getRequest();
        }
        if (current instanceof Request) {
            catalinaRequest = ((Request) current);
            coyoteRequest = catalinaRequest.getCoyoteRequest();
        } else {
            String msg = MessageFormat.format(
                    rb.getString(LogFacade.NO_PUSH_COYOTE_REQUEST_EXCEPTION), current.getClass().getName());
            throw new UnsupportedOperationException(msg);
        }

        headers.copyFrom(coyoteRequest.getRequest().getHeaders());
        String authorization = headers.getHeader(Header.Authorization);

        for (Header removeHeader : REMOVE_HEADERS) {
            headers.removeHeader(removeHeader);
        }

        if (request.getRemoteUser() != null) {
            headers.addValue(Header.Authorization).setString(authorization);
        }

        // set the referer header
        StringBuffer referer = request.getRequestURL();
        if (request.getQueryString() != null) {
            referer.append('?');
            referer.append(request.getQueryString());

        }
        headers.addValue(Header.Referer).setString(referer.toString());

        // Session
        Context context = catalinaRequest.getContext();
        sessionCookieName = context.getSessionCookieName();

        HttpSession session = request.getSession(false);
        if (session != null) {
            sessionId = session.getId();
        }
        if (sessionId == null) {
            sessionId = request.getRequestedSessionId();
        }
        if (!request.isRequestedSessionIdFromCookie() && !request.isRequestedSessionIdFromURL() &&
                sessionId != null) {
            Set<SessionTrackingMode> sessionTrackingModes =
                    request.getServletContext().getEffectiveSessionTrackingModes();
            addSessionCookie = sessionTrackingModes.contains(SessionTrackingMode.COOKIE);
            addSessionPathParameter = sessionTrackingModes.contains(SessionTrackingMode.URL);
        } else {
            addSessionCookie = request.isRequestedSessionIdFromCookie();
            addSessionPathParameter = request.isRequestedSessionIdFromURL();
        }

        // Cookies
        if (request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie c : request.getCookies()) {
                cookies.add(new Cookie(c.getName(), c.getValue()));
            }
        }

        org.glassfish.grizzly.http.server.Response coyoteResponse = coyoteRequest.getResponse();
        for (Cookie responseCookie : coyoteResponse.getCookies()) {
            if (responseCookie.getMaxAge() > 0) {
                cookies.add(new Cookie(responseCookie.getName(), responseCookie.getValue()));
            } else {
                // Path information not available so can only remove based on name.
                Iterator<Cookie> cookieIterator = cookies.iterator();
                while (cookieIterator.hasNext()) {
                    Cookie cookie = cookieIterator.next();
                    if (cookie.getName().equals(responseCookie.getName())) {
                        cookieIterator.remove();
                    }
                }
            }
        }

        if (cookies != null && !cookies.isEmpty()) {
            for (Cookie c : cookies) {
                headers.addValue(Header.Cookie).setString(c.asClientCookieString());
            }
        }
    }

    @Override
    public PushBuilder method(String method) {
        if (method == null) {
            throw new NullPointerException(rb.getString(LogFacade.NULL_PUSH_METHOD_EXCEPTION));
        }

        if (method.length() == 0) {
            throw new IllegalArgumentException(rb.getString(LogFacade.EMPTY_PUSH_METHOD_EXCEPTION));
        }

        String upperMethod = method.toUpperCase(Locale.ENGLISH);
        if (upperMethod.equals("POST") || upperMethod.equals("PUT") ||
                upperMethod.equals("DELETE") || upperMethod.equals("CONNECT") ||
                upperMethod.equals("OPTIONS") || upperMethod.equals("TRACE")) {

            String msg = MessageFormat.format(
                    rb.getString(LogFacade.NONCACHEABLE_UNSAFE_PUSH_METHOD_EXCEPTION), method);
            throw new IllegalArgumentException(msg);
        }

        this.method = method;
        return this;
    }

    @Override
    public String getMethod() {
        return method;
    }

    @Override
    public PushBuilder queryString(String queryString) {
        this.queryString = queryString;
        return this;
    }

    @Override
    public String getQueryString() {
        return queryString;
    }

    @Override
    public PushBuilder sessionId(String sessionId) {
        this.sessionId = sessionId;
        return this;
    }

    @Override
    public String getSessionId() {
        return sessionId;
    }

    @Override
    public PushBuilder addHeader(String name, String value) {
        if (isValidNameValue(name, value)) {
            headers.addValue(name).setString(value);
        }
        return this;
    }

    @Override
    public PushBuilder setHeader(String name, String value) {
        if (isValidNameValue(name, value)) {
            headers.setValue(name).setString(value);
        }
        return this;
    }

    @Override
    public PushBuilder removeHeader(String name) {
        if (isValidName(name)) {
            headers.removeHeader(name);
        }
        return this;
    }

    @Override
    public Set<String> getHeaderNames() {
        HashSet<String> names = new HashSet<>();
        Iterator<String> nameIter = headers.names().iterator();
        while (nameIter.hasNext()) {
            names.add(nameIter.next());
        }
        return names;
    }

    @Override
    public String getHeader(String name) {
        return headers.getHeader(name);
    }

    @Override
    public PushBuilder path(String path) {
        if (path != null && !path.startsWith("/")) {
            this.path = baseRequest.getContextPath() + "/" + path;
        } else {
            this.path = path;
        }
        return this;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public void push() {
        if (path == null) {
            throw new IllegalStateException(rb.getString(LogFacade.NO_PUSH_PATH_EXCEPTION));
        }

        Http2Stream http2Stream = (Http2Stream) coyoteRequest.getAttribute(Http2Stream.HTTP2_STREAM_ATTRIBUTE);
        if (http2Stream == null || !http2Stream.isPushEnabled()) {
            return;
        }

        // modify pathLocal rather than path
        String pathLocal = ((path.charAt(0) == '/') ? path : coyoteRequest.getContextPath() + '/' + path);
        if (queryString != null) {
            pathLocal += ((pathLocal.indexOf('?') != -1)
                    ? '&' + queryString
                    : '?' + queryString);
        }

        // Session ID (do this before setting the path since it may change it)
        if (sessionId != null) {
            if (addSessionPathParameter) {
                pathLocal = pathLocal + ";" + sessionCookieName + "=" + sessionId;
            }
            if (addSessionCookie) {
                cookies.add(new Cookie(sessionCookieName, sessionId));
            }
        }

        PushEvent.PushEventBuilder pushEventBuilder = PushEvent.builder();
        pushEventBuilder.method(method);
        pushEventBuilder.headers(headers);
        pushEventBuilder.path(pathLocal);
        pushEventBuilder.httpRequest(coyoteRequest.getRequest());

        coyoteRequest.getContext().notifyDownstream(pushEventBuilder.build());

        // Reset for next call
        path = null;
        for (Header conditionalHeader : CONDITIONAL_HEADERS) {
            headers.removeHeader(conditionalHeader);
        }
    }


    private static boolean isValidNameValue(final String name, final String value) {
        return isValidName(name) && value != null && !value.isEmpty();
    }

    private static boolean isValidName(final String name) {
        return (name != null && !name.isEmpty());
    }
}