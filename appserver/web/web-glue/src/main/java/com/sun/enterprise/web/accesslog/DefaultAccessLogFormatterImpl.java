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

package com.sun.enterprise.web.accesslog;

import com.sun.enterprise.config.serverbeans.ConfigBeansUtilities;
import com.sun.enterprise.web.Constants;
import org.apache.catalina.Container;
import org.apache.catalina.HttpResponse;
import org.apache.catalina.Request;
import org.apache.catalina.Response;
import org.glassfish.logging.annotation.LogMessageInfo;

import javax.servlet.ServletRequest;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.nio.CharBuffer;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Access log formatter using the SJSAS format.
 */
public class DefaultAccessLogFormatterImpl extends AccessLogFormatter {

    private static final Logger _logger = com.sun.enterprise.web.WebContainer.logger;

    @LogMessageInfo(
            message = "Illegal access log pattern [{0}], is not a valid nickname and does not contain any ''%''",
            level = "SEVERE",
            cause = "The pattern is either null or does not contain '%'",
            action = "Check the pattern for validity")
    public static final String ACCESS_LOG_VALVE_INVALID_ACCESS_LOG_PATTERN = "AS-WEB-GLUE-00047";

    @LogMessageInfo(
            message = "Missing end delimiter in access log pattern: {0}",
            level = "SEVERE",
            cause = "An end delimiter ismissing in the access log pattern",
            action = "Check the pattern for validity")
    public static final String MISSING_ACCESS_LOG_PATTERN_END_DELIMITER = "AS-WEB-GLUE-00048";

    @LogMessageInfo(
            message = "Invalid component: {0} in access log pattern: {1}",
            level = "SEVERE",
            cause = "Access log pattern containds invalid component",
            action = "Check the pattern for validity")
    public static final String INVALID_ACCESS_LOG_PATTERN_COMPONENT = "AS-WEB-GLUE-00049";

    private static final String QUOTE = "\"";

    /*
     * HTTP header names
     */
    private static final String HTTP_HEADER_ACCEPT = "Accept";
    private static final String HTTP_HEADER_AUTHORIZATION = "Authorization";
    private static final String HTTP_HEADER_DATE = "Date";
    private static final String HTTP_HEADER_IF_MODIFIED_SINCE = "If-Modified-Since";

    /*
     * Supported access log entry tokens
     */
    private static final String ATTRIBUTE_BY_NAME_PREFIX = "attribute.";
    private static final int ATTRIBUTE_BY_NAME_PREFIX_LEN =
        ATTRIBUTE_BY_NAME_PREFIX.length();
    private static final String SESSION_ATTRIBUTE_BY_NAME_PREFIX = "session.";
    private static final int SESSION_ATTRIBUTE_BY_NAME_PREFIX_LEN =
        SESSION_ATTRIBUTE_BY_NAME_PREFIX.length();
    private static final String AUTH_USER_NAME = "auth-user-name";
    private static final String CLIENT_DNS = "client.dns";
    private static final String CLIENT_NAME = "client.name";
    private static final String COOKIE = "cookie";
    private static final String COOKIES = "cookies";
    private static final String COOKIE_VALUE = "cookie.value";
    private static final String COOKIE_BY_NAME_PREFIX = "cookie.";
    private static final int COOKIE_BY_NAME_PREFIX_LEN
        = COOKIE_BY_NAME_PREFIX.length();
    private static final String COOKIES_BY_NAME_PREFIX = "cookies.";
    private static final int COOKIES_BY_NAME_PREFIX_LEN
        = COOKIES_BY_NAME_PREFIX.length();
    private static final String DATE_TIME = "datetime";
    private static final String HEADER_ACCEPT = "header.accept";
    private static final String HEADER_BY_NAME_PREFIX = "header.";
    private static final int HEADER_BY_NAME_PREFIX_LEN
        = HEADER_BY_NAME_PREFIX.length();
    private static final String HEADERS_BY_NAME_PREFIX = "headers.";
    private static final int HEADERS_BY_NAME_PREFIX_LEN =
        HEADERS_BY_NAME_PREFIX.length();
    private static final String RESPONSE_HEADER_BY_NAME_PREFIX =
        "response.header.";
    private static final int RESPONSE_HEADER_BY_NAME_PREFIX_LEN =
        RESPONSE_HEADER_BY_NAME_PREFIX.length();
    private static final String RESPONSE_HEADERS_BY_NAME_PREFIX =
        "response.headers.";
    private static final int RESPONSE_HEADERS_BY_NAME_PREFIX_LEN =
        RESPONSE_HEADERS_BY_NAME_PREFIX.length();
    private static final String HEADER_AUTH = "header.auth";
    private static final String HEADER_DATE = "header.date";
    private static final String HEADER_IF_MOD_SINCE = "header.if-mod-since";
    private static final String HEADER_USER_AGENT = "header.user-agent";
    private static final String HEADER_REFERER = "header.referer";
    private static final String HTTP_METHOD = "http-method";
    private static final String HTTP_URI = "http-uri";
    private static final String HTTP_VERSION = "http-version";
    private static final String QUERY_STR = "query-str";
    private static final String REFERER = "referer";
    private static final String REQUEST = "request";
    private static final String RESPONSE_LENGTH = "response.length";
    private static final String RESPONSE_CONTENT_TYPE = "response.content-type";
    private static final String STATUS = "status";
    private static final String TIME_TAKEN = "time-taken";
    private static final String USER_AGENT = "user.agent";
    private static final String VS_ID = "vs.id";

    private Container container;

    /**
     * List of access log pattern components
     */
    private LinkedList<String> patternComponents;

    /**
     * Constructor.
     *
     * @param pattern The access log pattern
     * @param container The container associated with the access log valve
     */
    public DefaultAccessLogFormatterImpl(String pattern, Container container) {

        super();

        this.patternComponents = parsePattern(pattern);
        if (patternComponents == null) {
            // Use default format if error in pattern
            patternComponents = parsePattern(ConfigBeansUtilities.getDefaultFormat());
        }
        this.container = container;

        final TimeZone timeZone = tz;
        dayFormatter = new ThreadLocal<SimpleDateFormat>() {
            @Override
            protected SimpleDateFormat initialValue() {
                SimpleDateFormat f = new SimpleDateFormat("dd");
                f.setTimeZone(timeZone);
                return f;
            }
        };
        monthFormatter =  new ThreadLocal<SimpleDateFormat>() {
            @Override
            protected SimpleDateFormat initialValue() {
                SimpleDateFormat f = new SimpleDateFormat("MM");
                f.setTimeZone(timeZone);
                return f;
            }
        };
        yearFormatter = new ThreadLocal<SimpleDateFormat>() {
            @Override
            protected SimpleDateFormat initialValue() {
                SimpleDateFormat f = new SimpleDateFormat("yyyy");
                f.setTimeZone(timeZone);
                return f;
            }
        };
        timeFormatter = new ThreadLocal<SimpleDateFormat>() {
            @Override
            protected SimpleDateFormat initialValue() {
                SimpleDateFormat f = new SimpleDateFormat("HH:mm:ss");
                f.setTimeZone(timeZone);
                return f;
            }
        };
    }

    /**
     * Appends an access log entry line, with info obtained from the given
     * request and response objects, to the given CharBuffer.
     *
     * @param request The request object from which to obtain access log info
     * @param response The response object from which to obtain access log info
     * @param charBuffer The CharBuffer to which to append access log info
     */
    public void appendLogEntry(Request request,
                               Response response,
                               CharBuffer charBuffer) {

        HttpServletRequest hreq = (HttpServletRequest)
            request.getRequest();
        HttpServletResponse hres = (HttpServletResponse)
            response.getResponse();

        for (int i=0; i<patternComponents.size(); i++) {
            String pc = patternComponents.get(i);
            if (pc.startsWith(ATTRIBUTE_BY_NAME_PREFIX)) {
                appendAttributeByName(
                    charBuffer,
                    pc.substring(ATTRIBUTE_BY_NAME_PREFIX_LEN),
                    hreq);
            } else if (pc.startsWith(SESSION_ATTRIBUTE_BY_NAME_PREFIX)) {
                appendSessionAttributeByName(
                    charBuffer,
                    pc.substring(SESSION_ATTRIBUTE_BY_NAME_PREFIX_LEN),
                    hreq);
            } else if (AUTH_USER_NAME.equals(pc)) {
                appendAuthUserName(charBuffer, hreq);
            } else if (CLIENT_DNS.equals(pc)) {
                appendClientDNS(charBuffer, hreq);
            } else if (CLIENT_NAME.equals(pc)) {
                appendClientName(charBuffer, hreq);
            } else if (COOKIE.equals(pc)) {
                appendCookie(charBuffer, hreq);
            } else if (COOKIES.equals(pc)) {
                appendCookies(charBuffer, hreq);
            } else if (COOKIE_VALUE.equals(pc)) {
                appendCookieValue(charBuffer, hreq);
            } else if (pc.startsWith(COOKIE_BY_NAME_PREFIX)) {
                appendCookieByName(charBuffer,
                                   pc.substring(COOKIE_BY_NAME_PREFIX_LEN),
                                   hreq);
            } else if (pc.startsWith(COOKIES_BY_NAME_PREFIX)) {
                appendCookiesByName(charBuffer,
                                   pc.substring(COOKIES_BY_NAME_PREFIX_LEN),
                                   hreq);
            } else if (DATE_TIME.equals(pc)) {
                appendCurrentDate(charBuffer);       
            } else if (HEADER_ACCEPT.equals(pc)) {
                appendHeaderAccept(charBuffer, hreq);
            } else if (HEADER_AUTH.equals(pc)) {
                appendHeaderAuth(charBuffer, hreq);
            } else if (HEADER_DATE.equals(pc)) {
                appendHeaderDate(charBuffer, hreq);
            } else if (HEADER_IF_MOD_SINCE.equals(pc)) {
                appendHeaderIfModSince(charBuffer, hreq);
            } else if (HEADER_USER_AGENT.equals(pc)) {
                appendUserAgent(charBuffer, hreq);
            } else if (HEADER_REFERER.equals(pc)) {
                appendReferer(charBuffer, hreq);
            } else if (HTTP_METHOD.equals(pc)) {
                appendHTTPMethod(charBuffer, hreq);
            } else if (HTTP_URI.equals(pc)) {
                appendHTTPUri(charBuffer, hreq);
            } else if (HTTP_VERSION.equals(pc)) {
                appendHTTPVersion(charBuffer, hreq);
            } else if (QUERY_STR.equals(pc)) {
                appendQueryString(charBuffer, hreq);
            } else if (REFERER.equals(pc)) {
                appendReferer(charBuffer, hreq);
            } else if (REQUEST.equals(pc)) {
                appendRequestInfo(charBuffer, hreq);
            } else if (RESPONSE_LENGTH.equals(pc)) {
                appendResponseLength(charBuffer, response);
            } else if (RESPONSE_CONTENT_TYPE.equals(pc)) {
                appendResponseContentType(charBuffer, response);
            } else if (STATUS.equals(pc)) {
                appendResponseStatus(charBuffer, response);
            } else if (TIME_TAKEN.equals(pc)) {
                appendTimeTaken(charBuffer, request);
            } else if (USER_AGENT.equals(pc)) {
                appendUserAgent(charBuffer, hreq);
            } else if (VS_ID.equals(pc)) {
                appendVirtualServerId(charBuffer);
            } else if (pc.startsWith(HEADER_BY_NAME_PREFIX)) {
                appendHeaderByName(charBuffer,
                                   pc.substring(HEADER_BY_NAME_PREFIX_LEN),
                                   hreq);
            } else if (pc.startsWith(HEADERS_BY_NAME_PREFIX)) {
                appendHeadersByName(charBuffer,
                                    pc.substring(HEADERS_BY_NAME_PREFIX_LEN),
                                    hreq);
            } else if (pc.startsWith(RESPONSE_HEADER_BY_NAME_PREFIX)) {
                appendResponseHeaderByName(charBuffer,
                    pc.substring(RESPONSE_HEADER_BY_NAME_PREFIX_LEN), hres, response);
            } else if (pc.startsWith(RESPONSE_HEADERS_BY_NAME_PREFIX)) {
                appendResponseHeadersByName(charBuffer,
                    pc.substring(RESPONSE_HEADERS_BY_NAME_PREFIX_LEN), hres, response);
            }

            charBuffer.put(SPACE);
        }
    }

    /*
     * Parses the access log pattern (that was specified via setPattern) into
     * its individual components, and returns them as a list.
     *
     * @param pattern The pattern to parse
     *
     * @return List containing the access log pattern components
     */
    private LinkedList<String> parsePattern(String pattern) {

        LinkedList<String> list = new LinkedList<String>();

        int from = 0;
        int end = -1;
        int index = -1;
        boolean errorInPattern = false;

        if (pattern == null || pattern.indexOf('%') < 0) {
            _logger.log(Level.SEVERE,
                        ACCESS_LOG_VALVE_INVALID_ACCESS_LOG_PATTERN,
                        pattern);
            errorInPattern = true;
        }

        while ((index = pattern.indexOf('%', from)) >= 0) {
            end = pattern.indexOf('%', index+1);
            if (end < 0) {
                _logger.log(
                    Level.SEVERE,
                    MISSING_ACCESS_LOG_PATTERN_END_DELIMITER,
                    pattern);
                errorInPattern = true;
                break;

            }
            String component = pattern.substring(index+1, end);

            if (!component.startsWith(ATTRIBUTE_BY_NAME_PREFIX)
                    && !AUTH_USER_NAME.equals(component) 
                    && !CLIENT_DNS.equals(component) 
                    && !CLIENT_NAME.equals(component) 
                    && !COOKIE.equals(component) 
                    && !COOKIES.equals(component) 
                    && !COOKIE_VALUE.equals(component)
                    && !component.startsWith(COOKIE_BY_NAME_PREFIX)
                    && !component.startsWith(COOKIES_BY_NAME_PREFIX)
                    && !DATE_TIME.equals(component) 
                    && !HEADER_ACCEPT.equals(component) 
                    && !HEADER_AUTH.equals(component) 
                    && !HEADER_DATE.equals(component) 
                    && !HEADER_IF_MOD_SINCE.equals(component) 
                    && !HEADER_USER_AGENT.equals(component) 
                    && !HEADER_REFERER.equals(component) 
                    && !HTTP_METHOD.equals(component) 
                    && !HTTP_URI.equals(component) 
                    && !HTTP_VERSION.equals(component) 
                    && !QUERY_STR.equals(component) 
                    && !REFERER.equals(component) 
                    && !REQUEST.equals(component) 
                    && !RESPONSE_LENGTH.equals(component)
                    && !RESPONSE_CONTENT_TYPE.equals(component)
                    && !STATUS.equals(component) 
                    && !TIME_TAKEN.equals(component) 
                    && !USER_AGENT.equals(component) 
                    && !VS_ID.equals(component)
                    && !component.startsWith(HEADER_BY_NAME_PREFIX)
                    && !component.startsWith(HEADERS_BY_NAME_PREFIX)
                    && !component.startsWith(RESPONSE_HEADER_BY_NAME_PREFIX)
                    && !component.startsWith(RESPONSE_HEADERS_BY_NAME_PREFIX)) {
                _logger.log(
                    Level.SEVERE,
                    INVALID_ACCESS_LOG_PATTERN_COMPONENT,
                    new Object[] { component, pattern });
                errorInPattern = true;
            }

            if (TIME_TAKEN.equals(component)) {
                needTimeTaken = true;
            }

            list.add(component);
            from = end + 1;    
        }

        if (errorInPattern) {
            return null;
        } else {
            return list;
        }
    }

    /*
     * Appends the string representation of the value of the request
     * attribute with the given name to the given char buffer, or
     * NULL-ATTRIBUTE-<attributeName> if no attribute with the given name
     * is present in the request.
     */
    private void appendAttributeByName(CharBuffer cb,
                                       String attributeName,
                                       HttpServletRequest hreq) {
        if (attributeName == null) {
            throw new IllegalArgumentException("Null request attribute name");
        }

        cb.put(QUOTE);
        Object attrValue = hreq.getAttribute(attributeName);
        if (attrValue != null) {
            cb.put(attrValue.toString());
        } else {
            cb.put("NULL-ATTRIBUTE-" + attributeName.toUpperCase(Locale.ENGLISH));
        }
        cb.put(QUOTE);
    }

    /*
     * Appends the string representation of the value of the session
     * attribute with the given name to the given char buffer, or
     * NULL-SESSION-ATTRIBUTE-<attributeName> if no attribute with the
     * given name is present in the session, or NULL-SESSION if
     * no session exists.
     */
    private void appendSessionAttributeByName(CharBuffer cb,
                                              String attributeName,
                                              HttpServletRequest hreq) {
        if (attributeName == null) {
            throw new IllegalArgumentException("Null session attribute name");
        }

        cb.put(QUOTE);
        HttpSession session = hreq.getSession(false);
        if (session != null) {
            Object attrValue = session.getAttribute(attributeName);
            if (attrValue != null) {
                cb.put(attrValue.toString());
            } else {
                cb.put("NULL-SESSION-ATTRIBUTE-" +
                       attributeName.toUpperCase(Locale.ENGLISH));
            }
        } else {
            cb.put("NULL-SESSION");
        }
        cb.put(QUOTE);
    }

    /*
     * Appends the client host name of the given request to the given char
     * buffer.
     */
    private void appendClientName(CharBuffer cb, ServletRequest req) {
        cb.put(QUOTE);
        String value = req.getRemoteHost();
        if (value == null) {
            value = "NULL-CLIENT-NAME";
        }
        cb.put(value);
        cb.put(QUOTE);
    }

    /*
     * Appends the client DNS of the given request to the given char
     * buffer.
     */
    private void appendClientDNS(CharBuffer cb, ServletRequest req) {
        cb.put(QUOTE);
        String value = req.getRemoteAddr();
        if (value == null) {
            value = "NULL-CLIENT-DNS";
        }
        cb.put(value);
        cb.put(QUOTE);
    }

    /*
     * Appends the authenticated user (if any) to the given char buffer.
     */
    private void appendAuthUserName(CharBuffer cb, HttpServletRequest hreq) {
        cb.put(QUOTE);
        String user = hreq.getRemoteUser();
        if (user == null) {
            user = "NULL-AUTH-USER";
        }
        cb.put(user);
        cb.put(QUOTE);
    }

    /*
     * Appends the current date to the given char buffer.
     */
    private void appendCurrentDate(CharBuffer cb) {
        cb.put(QUOTE);
        Date date = getDate();
        cb.put(dayFormatter.get().format(date));           // Day
        cb.put('/');
        cb.put(lookup(monthFormatter.get().format(date))); // Month
        cb.put('/');
        cb.put(yearFormatter.get().format(date));          // Year
        cb.put(':');
        cb.put(timeFormatter.get().format(date));          // Time
        cb.put(SPACE);
        cb.put(timeZone);                                  // Time Zone
        cb.put(QUOTE);
    }

    /*
     * Appends info about the given request to the given char buffer.
     */
    private void appendRequestInfo(CharBuffer cb, HttpServletRequest hreq) {
        cb.put(QUOTE);
        cb.put(hreq.getMethod());
        cb.put(SPACE);
        String uri = hreq.getRequestURI();
        if (uri == null) {
            uri = "NULL-HTTP-URI";
        }
        cb.put(uri);
        if (hreq.getQueryString() != null) {
            cb.put('?');
            cb.put(hreq.getQueryString());
        }
        cb.put(SPACE);
        cb.put(hreq.getProtocol());
        cb.put(QUOTE);
    }

    /*
     * Appends the response status to the given char buffer.
     */
    private void appendResponseStatus(CharBuffer cb, Response response) {
        cb.put(String.valueOf(((HttpResponse) response).getStatus()));
    }

    /*
     * Appends the content length of the given response to the given char
     * buffer.
     */
    private void appendResponseLength(CharBuffer cb, Response response) {
        cb.put("" + response.getContentCount());
    }

    /*
     * Appends the content type of the given response to the given char
     * buffer.
     */
    private void appendResponseContentType(CharBuffer cb, Response response) {
        cb.put(response.getContentType());
    }

    /*
     * Appends the value of the 'user-agent' header of the given request to
     * the given char buffer.
     */
    private void appendUserAgent(CharBuffer cb, HttpServletRequest hreq) {
        cb.put(QUOTE);
        String ua = hreq.getHeader("user-agent");
        if (ua == null) {
            ua = "NULL-USER-AGENT";
        }
        cb.put(ua);
        cb.put(QUOTE);
    }

    /*
     * Appends the time (in milliseconds) it has taken to service the given
     * request to the given char buffer.
     */
    private void appendTimeTaken(CharBuffer cb, Request req) {

        String timeTaken = "NULL-TIME-TAKEN";

        cb.put(QUOTE);
        Long startTimeObj = (Long) req.getNote(
            Constants.REQUEST_START_TIME_NOTE);
        if (startTimeObj != null) {
            long startTime = startTimeObj.longValue();
            long endTime = System.currentTimeMillis();
            timeTaken = String.valueOf(endTime - startTime);
        } 
        cb.put(timeTaken);
        cb.put(QUOTE);
    }

    /*
     * Appends the value of the 'referer' header of the given request to
     * the given char buffer.
     */
    private void appendReferer(CharBuffer cb, HttpServletRequest hreq) {
        cb.put(QUOTE);
        String referer = hreq.getHeader("referer");
        if (referer == null) {
            referer = "NULL-REFERER";
        }
        cb.put(referer);
        cb.put(QUOTE);
    }

    /*
     * Appends the Accept header of the given request to the given char
     * buffer.
     */
    private void appendHeaderAccept(CharBuffer cb, HttpServletRequest hreq) {
        cb.put(QUOTE);
        String accept = hreq.getHeader(HTTP_HEADER_ACCEPT);
        if (accept == null) {
            accept = "NULL-HEADER-ACCEPT";
        }
        cb.put(accept);
        cb.put(QUOTE);
    }

    /*
     * Appends the Authorization header of the given request to the given char
     * buffer.
     */
    private void appendHeaderAuth(CharBuffer cb, HttpServletRequest hreq) {
        cb.put(QUOTE);
        String auth = hreq.getHeader(HTTP_HEADER_AUTHORIZATION);
        if (auth == null) {
            auth = "NULL-HEADER-AUTHORIZATION";
        }
        cb.put(auth);
        cb.put(QUOTE);
    }

    /*
     * Appends the Date header of the given request to the given char buffer.
     */
    private void appendHeaderDate(CharBuffer cb, HttpServletRequest hreq) {
        cb.put(QUOTE);
        String date = hreq.getHeader(HTTP_HEADER_DATE);
        if (date == null) {
            date = "NULL-HEADER-DATE";
        }
        cb.put(date);
        cb.put(QUOTE);
    }

    /*
     * Appends the If-Modified-Since header of the given request to the
     * given char buffer.
     */
    private void appendHeaderIfModSince(CharBuffer cb,
                                        HttpServletRequest hreq) {
        cb.put(QUOTE);
        String ifModSince = hreq.getHeader(HTTP_HEADER_IF_MODIFIED_SINCE);
        if (ifModSince == null) {
            ifModSince = "NULL-HEADER-IF-MODIFIED-SINCE";
        }
        cb.put(ifModSince);
        cb.put(QUOTE);
    }

    /*
     * Appends the value of the header with the specified name in the given
     * request to the given char buffer, or NULL-HEADER-<headerName> if no
     * header with the given name is present in the request..
     */
    private void appendHeaderByName(CharBuffer cb,
                                    String headerName,
                                    HttpServletRequest hreq) {
        if (headerName == null) {
            throw new IllegalArgumentException("Null request header name");
        }

        cb.put(QUOTE);
        String value = hreq.getHeader(headerName);
        if (value == null) {
            value = "NULL-HEADER-" + headerName.toUpperCase(Locale.ENGLISH);
        }
        cb.put(value);
        cb.put(QUOTE);
    }

    /*
     * Appends the value of the header with the specified name in the given
     * response to the given char buffer, or
     * NULL-RESPONSE-HEADER-<headerName> if no header with the given name
     * is present in the response.
     */
    private void appendResponseHeaderByName(CharBuffer cb,
                                            String headerName,
                                            HttpServletResponse hres, Response response) {
        if (headerName == null) {
            throw new IllegalArgumentException("Null response header name");
        }

        cb.put(QUOTE);
        String value = hres.getHeader(headerName);
        if (value == null) {
            if (headerName.equalsIgnoreCase("Content-Type")) {
                value = hres.getContentType();
            } else if (headerName.equalsIgnoreCase("Content-Length")) {
                value = ""+response.getContentLength();
            } else {
                value = "NULL-RESPONSE-HEADER-" + headerName.toUpperCase(Locale.ENGLISH);
            }
        }
        cb.put(value);
        cb.put(QUOTE);
    }

    /*
     * Appends the values (separated by ";") of all headers with the
     * specified name in the given request to the given char buffer, or
     * NULL-HEADERS-<headerName> if no headers with the given name are 
     * present in the request..
     */
    private void appendHeadersByName(CharBuffer cb,
                                     String headerName,
                                     HttpServletRequest hreq) {
        if (headerName == null) {
            throw new IllegalArgumentException("Null request header name");
        }

        cb.put(QUOTE);
        Enumeration e = hreq.getHeaders(headerName);
        if (e != null) {
            boolean first = true;
            while (e.hasMoreElements()) {
                if (first) {
                    first = false;
                } else {
                    cb.put(";");
                }
                cb.put(e.nextElement().toString());
            }
            if (first) {
                cb.put("NULL-HEADERS-" + headerName.toUpperCase(Locale.ENGLISH));
            }
        } else {
            cb.put("NULL-HEADERS-" + headerName.toUpperCase(Locale.ENGLISH));
        }
        cb.put(QUOTE);
    }

    /*
     * Appends the values (separated by ";") of all headers with the
     * specified name in the given response to the given char buffer, or
     * NULL-RESPONSE-HEADERS-<headerName> if no headers with the given name
     * are present in the response.
     */
    private void appendResponseHeadersByName(CharBuffer cb,
            String headerName, HttpServletResponse hres, Response response) {
        if (headerName == null) {
            throw new IllegalArgumentException("Null response header name");
        }

        cb.put(QUOTE);
        boolean first = true;
        Collection<String> values = hres.getHeaders(headerName);
        if (!values.isEmpty()) {
            for (String value : values) {
                if (first) {
                    first = false;
                } else {
                    cb.put(";");
                }
                cb.put(value);
            }
        } else {
            String value = null;
            if (headerName.equalsIgnoreCase("Content-Type")) {
                value = hres.getContentType();
            } else if (headerName.equalsIgnoreCase("Content-Length")) {
                value = ""+response.getContentLength();
            }
            if (value != null) {
                first = false;
                cb.put(value);
            }
        }
        if (first) {
            cb.put("NULL-RESPONSE-HEADERS-" + headerName.toUpperCase(Locale.ENGLISH));
        }
        cb.put(QUOTE);
    }

    /*
     * Appends the name and value (separated by '=') of the first cookie
     * in the given request to the given char buffer, or NULL-COOKIE if no
     * cookies are present in the request.
     */
    private void appendCookie(CharBuffer cb, HttpServletRequest hreq) {
        cb.put(QUOTE);
        String cookie = "NULL-COOKIE";
        Cookie[] cookies = hreq.getCookies();
        if (cookies != null && cookies.length > 0) {
            cookie = cookies[0].getName() + "=" + cookies[0].getValue();
        }
        cb.put(cookie);
        cb.put(QUOTE);
    }

    /*
     * Appends the name and value (separated by '=') of all cookies
     * (separated by ';') in the given request to the given char buffer,
     * or NULL-COOKIES if no cookies are present in the request.
     */
    private void appendCookies(CharBuffer cb, HttpServletRequest hreq) {
        cb.put(QUOTE);
        Cookie[] cookies = hreq.getCookies();
        if (cookies != null && cookies.length > 0) {
            for (int i=0; i<cookies.length; i++) {
                cb.put(cookies[i].getName() + "=" + cookies[i].getValue());
                if (i<cookies.length-1) {
                    cb.put(";");
                }
            }
        } else {
            cb.put("NULL-COOKIES");
        }
        cb.put(QUOTE);
    }

    /*
     * Appends the value of the first cookie in the given request to the
     * given char buffer, or NULL-COOKIE-VALUE if no cookies are present
     * in the request.
     */
    private void appendCookieValue(CharBuffer cb, HttpServletRequest hreq) {
        cb.put(QUOTE);
        String cookieValue = "NULL-COOKIE-VALUE";
        Cookie[] cookies = hreq.getCookies();
        if (cookies != null && cookies.length > 0) {
            cookieValue = cookies[0].getValue();
        }
        cb.put(cookieValue);
        cb.put(QUOTE);
    }

    /*
     * Appends the value of the first cookie with the given cookie name to the
     * given char buffer, or NULL-COOKIE-<cookieName> if no cookies with the
     * given cookie name are present in the request.
     */
    private void appendCookieByName(CharBuffer cb,
                                    String cookieName,
                                    HttpServletRequest hreq) {
        if (cookieName == null) {
            throw new IllegalArgumentException("Null request cookie name");
        }

        cb.put(QUOTE);
        String cookieValue = null;
        Cookie[] cookies = hreq.getCookies();
        if (cookies != null && cookies.length > 0) {
            for (Cookie cookie : cookies) {
                if (cookieName.equals(cookie.getName())) {
                    cookieValue = cookie.getValue();
                    break;
                }
            }
        }
        if (cookieValue == null) {
            cookieValue = "NULL-COOKIE-" + cookieName.toUpperCase(Locale.ENGLISH);
        }

        cb.put(cookieValue);
        cb.put(QUOTE);
    }

    /*
     * Appends the value of all cookies with the given cookie name to the
     * given char buffer, or NULL-COOKIES-<cookieName> if no cookies with the
     * given cookie name are present in the request.
     */
    private void appendCookiesByName(CharBuffer cb,
                                     String cookieName,
                                     HttpServletRequest hreq) {
        if (cookieName == null) {
            throw new IllegalArgumentException("Null request cookie name");
        }

        cb.put(QUOTE);
        Cookie[] cookies = hreq.getCookies();
        if (cookies != null && cookies.length > 0) {
            boolean first = true;
            for (int i=0; i<cookies.length; i++) {
                if (cookieName.equals(cookies[i].getName())) {
                    if (first) {
                        first = false;
                    } else {
                        cb.put(";");
		    }
                    cb.put(cookies[i].getValue());
                }
            }
        } else {
            cb.put("NULL-COOKIES-" + cookieName.toUpperCase(Locale.ENGLISH));
        }
        cb.put(QUOTE);
    }

    /*
     * Appends the HTTP method of the given request to the given char buffer.
     */
    private void appendHTTPMethod(CharBuffer cb, HttpServletRequest hreq) {
        cb.put(QUOTE);
        String method = hreq.getMethod();
        if (method == null) {
            method = "NULL-HTTP-METHOD";
        }
        cb.put(method);
        cb.put(QUOTE);
    }

    /*
     * Appends the URI of the given request to the given char buffer.
     */
    private void appendHTTPUri(CharBuffer cb, HttpServletRequest hreq) {
        cb.put(QUOTE);
        String uri = hreq.getRequestURI();
        if (uri == null) {
            uri = "NULL-HTTP-URI";
        }
        cb.put(uri);
        cb.put(QUOTE);
    }

    /*
     * Appends the HTTP protocol version of the given request to the given
     * char buffer.
     */
    private void appendHTTPVersion(CharBuffer cb, HttpServletRequest hreq) {
        cb.put(QUOTE);
        String protocol = hreq.getProtocol();
        if (protocol == null) {
            protocol = "NULL-HTTP-PROTOCOL";
        }
        cb.put(protocol);
        cb.put(QUOTE);
    }

    /*
     * Appends the query string of the given request to the given char buffer.
     */
    private void appendQueryString(CharBuffer cb, HttpServletRequest hreq) {
        cb.put(QUOTE);
        String query = hreq.getQueryString();
        if (query == null) {
            query = "NULL-QUERY";
        }
        cb.put(query);
        cb.put(QUOTE);
    }

    /*
     * Appends the id of the virtual server with which this access log valve
     * has been associated to the given char buffer.
     */
    private void appendVirtualServerId(CharBuffer cb) {
        String vsId = "NULL-VIRTUAL-SERVER";
        if (container != null) {
            vsId = container.getName();
        }
        cb.put(vsId);
    }

}
