/*
 *    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) 2026 Payara Foundation and/or its affiliates. All rights reserved.
 *
 *    The contents of this file are subject to the terms of either the GNU
 *    General Public License Version 2 only ("GPL") or the Common Development
 *    and Distribution License("CDDL") (collectively, the "License").  You
 *    may not use this file except in compliance with the License.  You can
 *    obtain a copy of the License at
 *    https://github.com/payara/Payara/blob/main/LICENSE.txt
 *    See the License for the specific
 *    language governing permissions and limitations under the License.
 *
 *    When distributing the software, include this License Header Notice in each
 *    file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 *    GPL Classpath Exception:
 *    The Payara Foundation designates this particular file as subject to the "Classpath"
 *    exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *    file that accompanied this code.
 *
 *    Modifications:
 *    If applicable, add the following below the License Header, with the fields
 *    enclosed by brackets [] replaced by your own identifying information:
 *    "Portions Copyright [year] [name of copyright owner]"
 *
 *    Contributor(s):
 *    If you wish your version of this file to be governed by only the CDDL or
 *    only the GPL Version 2, indicate your decision by adding "[Contributor]
 *    elects to include this software in this distribution under the [CDDL or GPL
 *    Version 2] license."  If you don't indicate a single choice of license, a
 *    recipient has the option to distribute your version of this file under
 *    either the CDDL, the GPL Version 2 or to extend the choice of license to
 *    its licensees as provided above.  However, if you add GPL Version 2 code
 *    and therefore, elected the GPL Version 2 license, then the option applies
 *    only if the new code is made subject to such option by the copyright
 *    holder.
 */

package com.sun.enterprise.web.accesslog;

import com.sun.enterprise.web.Constants;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.apache.catalina.Container;
import org.apache.catalina.HttpResponse;
import org.apache.catalina.Request;
import org.apache.catalina.Response;

import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class JsonAccessLogFormatter extends DefaultAccessLogFormatterImpl {
    private static final char QUOTE = '"';
    private static final char COLON = ':';
    private static final char COMMA = ',';
    private static final char SPACE = ' ';
    private static final char OPEN_OBJECT = '{';
    private static final char CLOSE_OBJECT = '}';
    private static final char OPEN_ARRAY = '[';
    private static final char CLOSE_ARRAY = ']';

    public JsonAccessLogFormatter(String pattern, Container container) {
        super(pattern, container);
    }

    @Override
    public void appendLogEntry(Request request, Response response, CharBuffer charBuffer) {
        if (!(request.getRequest() instanceof HttpServletRequest)) return;

        charBuffer.append(OPEN_OBJECT).append(SPACE);
        super.appendLogEntry(request, response, charBuffer);
        charBuffer.append(CLOSE_OBJECT);
    }

    private void appendKeyValue(String key, String value, CharBuffer buffer) {
        char last = buffer.isEmpty() ? OPEN_OBJECT : buffer.get(buffer.position() - 1);
        char beforeLast = buffer.length() < 2 ? OPEN_OBJECT : buffer.get(buffer.position() - 2);
        if (last == QUOTE || last == CLOSE_OBJECT) {
            buffer.append(COMMA).append(SPACE);
        } else if (last == SPACE && beforeLast != OPEN_OBJECT) {
            buffer.put(buffer.position() - 1, COMMA).append(SPACE);
        }

        buffer.append(QUOTE).append(key).append(QUOTE).append(COLON).append(SPACE);
        buffer.append(QUOTE).append(value).append(QUOTE);
    }

    private void appendKeyValue(String key, int value, CharBuffer buffer) {
        appendKeyValue(key, String.valueOf(value), buffer);
    }

    private void appendKeyValueObject(String key, Consumer<CharBuffer> contents, CharBuffer buffer) {
        char last = buffer.isEmpty() ? OPEN_OBJECT : buffer.get(buffer.length() - 1);
        if (last == QUOTE || last == CLOSE_OBJECT) {
            buffer.append(COMMA).append(SPACE);
        }

        buffer.append(QUOTE).append(key).append(QUOTE).append(COLON).append(SPACE);
        buffer.append(OPEN_OBJECT).append(SPACE);
        contents.accept(buffer);
        buffer.append(SPACE).append(CLOSE_OBJECT);
    }

    private void appendKeyValueArray(String key, List<String> contents, CharBuffer buffer) {
        char last = buffer.isEmpty() ? OPEN_OBJECT : buffer.get(buffer.length() - 1);
        if (last == QUOTE || last == CLOSE_OBJECT) {
            buffer.append(COMMA).append(SPACE);
        }

        buffer.append(QUOTE).append(key).append(QUOTE).append(COLON).append(SPACE);
        buffer.append(OPEN_ARRAY).append(SPACE);

        boolean first = true;
        for (String value : contents) {
            if (first) {
                first = false;
            } else {
                buffer.append(COMMA).append(SPACE);
            }

            buffer.append(QUOTE).append(value).append(QUOTE);
        }
        buffer.append(SPACE).append(CLOSE_ARRAY);
    }

    @Override
    protected void appendAttributeByName(CharBuffer buffer, String attributeName, HttpServletRequest hreq) {
        if (attributeName == null) {
            throw new IllegalArgumentException("Null request attribute name");
        }

        Object attributeValue = hreq.getAttribute(attributeName);
        if (attributeValue != null) {
            appendKeyValue(attributeName, attributeValue.toString(), buffer);
        } else {
            appendKeyValue(attributeName, "NULL-ATTRIBUTE-" + attributeName.toUpperCase(Locale.ENGLISH), buffer);
        }
    }

    @Override
    protected void appendSessionAttributeByName(CharBuffer buffer, String attributeName, HttpServletRequest hreq) {
        if (attributeName == null) {
            throw new IllegalArgumentException("Null session attribute name");
        }

        HttpSession session = hreq.getSession(false);
        if (session != null) {
            Object attributeValue = session.getAttribute(attributeName);
            if (attributeValue != null) {
                appendKeyValue(attributeName, attributeValue.toString(), buffer);
            } else {
                appendKeyValue(attributeName, "NULL-SESSION-ATTRIBUTE-" + attributeName.toUpperCase(Locale.ENGLISH), buffer);
            }
        } else {
            appendKeyValue(attributeName, "NULL-SESSION", buffer);
        }
    }

    @Override
    protected void appendClientName(CharBuffer buffer, ServletRequest req) {
        String value = req.getRemoteHost();
        if (value == null) {
            value = "NULL-CLIENT-NAME";
        }
        appendKeyValue(CLIENT_NAME, value, buffer);
    }

    @Override
    protected void appendClientDNS(CharBuffer buffer, ServletRequest req) {
        String value = req.getRemoteAddr();
        if (value == null) {
            value = "NULL-CLIENT-DNS";
        }
        appendKeyValue(CLIENT_DNS, value, buffer);
    }

    @Override
    protected void appendAuthUserName(CharBuffer buffer, HttpServletRequest hreq) {
        String user = hreq.getRemoteUser();
        if (user == null) {
            user = "NULL-AUTH-USER";
        }
        appendKeyValue(AUTH_USER_NAME, user, buffer);
    }

    @Override
    protected void appendCurrentDate(CharBuffer buffer) {
        Date date = getDate();
        String builder = dayFormatter.get().format(date) +  // Day
                '/' +
                lookup(monthFormatter.get().format(date)) + // Month
                '/' +
                yearFormatter.get().format(date) +          // Year
                COLON +
                timeFormatter.get().format(date) +          // Time
                SPACE +
                timeZone;                                   // Time Zone

        appendKeyValue(DATE_TIME, builder, buffer);
    }

    @Override
    protected void appendRequestInfo(CharBuffer buffer, HttpServletRequest hreq) {
        appendKeyValueObject(REQUEST, cb -> {
            String uri = hreq.getRequestURI();
            if (uri == null) {
                uri = "NULL-HTTP-URI";
            }
            if (hreq.getQueryString() != null) {
                uri += '?' + hreq.getQueryString();
            }

            appendKeyValue("uri", uri, cb);
            appendKeyValue("protocol", hreq.getProtocol(), cb);
        }, buffer);
    }

    @Override
    protected void appendResponseStatus(CharBuffer buffer, Response response) {
        appendKeyValue(STATUS, ((HttpResponse)response).getStatus(), buffer);
    }

    @Override
    protected void appendResponseLength(CharBuffer buffer, Response response) {
        appendKeyValue(RESPONSE_LENGTH, response.getContentCount(), buffer);
    }

    @Override
    protected void appendResponseContentType(CharBuffer buffer, Response response) {
        appendKeyValue(RESPONSE_CONTENT_TYPE, response.getContentType(), buffer);
    }

    @Override
    protected void appendUserAgent(CharBuffer buffer, HttpServletRequest hreq) {
        String userAgent = hreq.getHeader("user-agent");
        if (userAgent == null) {
            userAgent = "NULL-USER-AGENT";
        }
        appendKeyValue(USER_AGENT, userAgent, buffer);
    }

    @Override
    protected void appendTimeTaken(CharBuffer buffer, Request request) {
        String timeTaken = "NULL-TIME-TAKEN";
        Long startTimeObj = (Long)request.getNote(Constants.REQUEST_START_TIME_NOTE);
        if (startTimeObj != null) {
            long startTime = startTimeObj;
            long endTime = System.currentTimeMillis();
            timeTaken = String.valueOf(endTime - startTime);
        }
        appendKeyValue(TIME_TAKEN, timeTaken, buffer);
    }

    @Override
    protected void appendReferer(CharBuffer buffer, HttpServletRequest hreq) {
        String referer = hreq.getHeader("referer");
        if (referer == null) {
            referer = "NULL-REFERER";
        }
        appendKeyValue(REFERER, referer, buffer);
    }

    @Override
    protected void appendHeaderAccept(CharBuffer buffer, HttpServletRequest hreq) {
        String accept = hreq.getHeader(HTTP_HEADER_ACCEPT);
        if (accept == null) {
            accept = "NULL-HEADER-ACCEPT";
        }
        appendKeyValue(HEADER_ACCEPT, accept, buffer);
    }

    @Override
    protected void appendHeaderAuth(CharBuffer buffer, HttpServletRequest hreq) {
        String auth = hreq.getHeader(HTTP_HEADER_AUTHORIZATION);
        if (auth == null) {
            auth = "NULL-HEADER-AUTHORIZATION";
        }
        appendKeyValue(HEADER_AUTH, auth, buffer);
    }

    @Override
    protected void appendHeaderDate(CharBuffer buffer, HttpServletRequest hreq) {
        String date = hreq.getHeader(HTTP_HEADER_DATE);
        if (date == null) {
            date = "NULL-HEADER-DATE";
        }
        appendKeyValue(HEADER_DATE, date, buffer);
    }

    @Override
    protected void appendHeaderIfModSince(CharBuffer buffer, HttpServletRequest hreq) {
        String ifModSince = hreq.getHeader(HTTP_HEADER_IF_MODIFIED_SINCE);
        if (ifModSince == null) {
            ifModSince = "NULL-HEADER-IF-MODIFIED-SINCE";
        }
        appendKeyValue(HEADER_IF_MOD_SINCE, ifModSince, buffer);
    }

    @Override
    protected void appendHeaderByName(CharBuffer buffer, String headerName, HttpServletRequest hreq) {
        if (headerName == null) {
            throw new IllegalArgumentException("Null request header name");
        }

        String value = hreq.getHeader(headerName);
        if (value == null) {
            value = "NULL-HEADER-" + headerName.toUpperCase(Locale.ENGLISH);
        }
        appendKeyValue(HEADER_BY_NAME_PREFIX + headerName, value, buffer);
    }

    @Override
    protected void appendHeadersByName(CharBuffer buffer, String headerName, HttpServletRequest hreq) {
        if (headerName == null) {
            throw new IllegalArgumentException("Null request header name");
        }

        Enumeration<String> e = hreq.getHeaders(headerName);
        List<String> values = new ArrayList<>();

        if (e != null) {
            boolean first = true;
            while (e.hasMoreElements()) {
                if (first) {
                    first = false;
                }
                values.add(e.nextElement());
            }
        }

        if (values.isEmpty()) {
            values.add("NULL-HEADERS-" + headerName.toUpperCase(Locale.ENGLISH));
        }

        appendKeyValueArray(HEADERS_BY_NAME_PREFIX, values, buffer);
    }

    @Override
    protected void appendResponseHeaderByName(CharBuffer buffer, String headerName, HttpServletResponse hres, Response response) {
        if (headerName == null) {
            throw new IllegalArgumentException("Null response header name");
        }

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
        appendKeyValue(RESPONSE_HEADER_BY_NAME_PREFIX + headerName, value, buffer);
    }

    @Override
    protected void appendResponseHeadersByName(CharBuffer buffer, String headerName, HttpServletResponse hres, Response response) {
        if (headerName == null) {
            throw new IllegalArgumentException("Null response header name");
        }

        Collection<String> values = hres.getHeaders(headerName);
        if (!values.isEmpty()) {
            appendKeyValueArray(headerName, values.stream().toList(), buffer);
        } else {
            String value = null;
            if (headerName.equalsIgnoreCase("Content-Type")) {
                value = hres.getContentType();
            } else if (headerName.equalsIgnoreCase("Content-Length")) {
                value = "" + response.getContentLength();
            }
            appendKeyValue(headerName, value, buffer);
        }
    }

    @Override
    protected void appendCookie(CharBuffer buffer, HttpServletRequest hreq) {
        String cookie = "NULL-COOKIE";
        Cookie[] cookies = hreq.getCookies();
        if (cookies != null && cookies.length > 0) {
            cookie = cookies[0].getName() + "=" + cookies[0].getValue();
        }
        appendKeyValue(COOKIE, cookie, buffer);
    }

    @Override
    protected void appendCookies(CharBuffer buffer, HttpServletRequest hreq) {
        Cookie[] cookies = hreq.getCookies();
        if (cookies != null && cookies.length > 0) {
            List<String> cookieValues = new ArrayList<>();
            for (Cookie cookie : cookies) {
                cookieValues.add(cookie.getName() + "=" + cookie.getValue());
            }
            appendKeyValueArray(COOKIES, cookieValues, buffer);
        } else {
            appendKeyValue(COOKIES, "NULL-COOKIES", buffer);
        }
    }

    @Override
    protected void appendCookieValue(CharBuffer buffer, HttpServletRequest hreq) {
        String cookieValue = "NULL-COOKIE-VALUE";
        Cookie[] cookies = hreq.getCookies();
        if (cookies != null && cookies.length > 0) {
            cookieValue = cookies[0].getValue();
        }
        appendKeyValue(COOKIE_VALUE, cookieValue, buffer);
    }

    @Override
    protected void appendCookieByName(CharBuffer buffer, String cookieName, HttpServletRequest hreq) {
        if (cookieName == null) {
            throw new IllegalArgumentException("Null request cookie name");
        }

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

        appendKeyValue(COOKIE_BY_NAME_PREFIX + cookieName, cookieValue, buffer);
    }

    @Override
    protected void appendCookiesByName(CharBuffer buffer, String cookieName, HttpServletRequest hreq) {
        if (cookieName == null) {
            throw new IllegalArgumentException("Null request cookie name");
        }

        Cookie[] cookies = hreq.getCookies();
        if (cookies != null && cookies.length > 0) {
            List<String> cookieValues = new ArrayList<>();
            for (Cookie cookie : cookies) {
                if (cookieName.equals(cookie.getName())) {
                    cookieValues.add(cookie.getValue());
                }
            }
            appendKeyValueArray(COOKIES_BY_NAME_PREFIX + cookieName, cookieValues, buffer);
        } else {
            appendKeyValue(COOKIES_BY_NAME_PREFIX + cookieName, "NULL-COOKIES-" + cookieName.toUpperCase(Locale.ENGLISH), buffer);
        }
    }

    @Override
    protected void appendHTTPMethod(CharBuffer buffer, HttpServletRequest hreq) {
        String method = hreq.getMethod();
        if (method == null) {
            method = "NULL-HTTP-METHOD";
        }
        appendKeyValue(HTTP_METHOD, method, buffer);
    }

    @Override
    protected void appendHTTPUri(CharBuffer buffer, HttpServletRequest hreq) {
        String uri = hreq.getRequestURI();
        if (uri == null) {
            uri = "NULL-HTTP-URI";
        }
        appendKeyValue(HTTP_URI, uri, buffer);
    }

    @Override
    protected void appendHTTPVersion(CharBuffer buffer, HttpServletRequest hreq) {
        String protocol = hreq.getProtocol();
        if (protocol == null) {
            protocol = "NULL-HTTP-PROTOCOL";
        }
        appendKeyValue(HTTP_VERSION, protocol, buffer);
    }

    @Override
    protected void appendQueryString(CharBuffer buffer, HttpServletRequest hreq) {
        String query = hreq.getQueryString();
        if (query == null) {
            query = "NULL-QUERY";
        }
        appendKeyValue(QUERY_STR, query, buffer);
    }

    @Override
    protected void appendVirtualServerId(CharBuffer buffer) {
        String vsId = "NULL-VIRTUAL-SERVER";
        if (container != null) {
            vsId = container.getName();
        }
        appendKeyValue(VS_ID, vsId, buffer);
    }
}
