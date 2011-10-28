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

package com.sun.enterprise.web.accesslog;

import org.apache.catalina.HttpResponse;
import org.apache.catalina.Request;
import org.apache.catalina.Response;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import java.nio.CharBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Access log formatter using the <i>common</i> access log format from
 * Apache.
 */
public class CommonAccessLogFormatterImpl extends AccessLogFormatter {

    protected static final String NULL_VALUE = "-";


    /**
     * Constructor.
     */
    public CommonAccessLogFormatterImpl() {

        super();

        final TimeZone timeZone = tz;
        dayFormatter = new ThreadLocal<SimpleDateFormat>() {
            @Override
            protected SimpleDateFormat initialValue() {
                SimpleDateFormat f = new SimpleDateFormat("dd");
                f.setTimeZone(timeZone);
                return f;
            }
        };
        monthFormatter = new ThreadLocal<SimpleDateFormat>() {
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

        ServletRequest req = request.getRequest();
        HttpServletRequest hreq = (HttpServletRequest) req;

        appendClientName(charBuffer, req);
        charBuffer.put(SPACE);

        appendClientId(charBuffer, req);
        charBuffer.put(SPACE);

        appendAuthUserName(charBuffer, hreq);
        charBuffer.put(SPACE);

        appendCurrentDate(charBuffer);
        charBuffer.put(SPACE);

        appendRequestInfo(charBuffer, hreq);
        charBuffer.put(SPACE);

        appendResponseStatus(charBuffer, response);
        charBuffer.put(SPACE);

        appendResponseLength(charBuffer, response);
        charBuffer.put(SPACE);
    }


    /*
     * Appends the client host name of the given request to the given char
     * buffer.
     */
    private void appendClientName(CharBuffer cb, ServletRequest req) {
        String value = req.getRemoteHost();
        if (value == null) {
            value = NULL_VALUE;
        }
        cb.put(value);
    }


    /*
     * Appends the client's RFC 1413 identity to the given char buffer..
     */
    private void appendClientId(CharBuffer cb, ServletRequest req) {
        cb.put(NULL_VALUE); // unsupported
    }


    /*
     * Appends the authenticated user (if any) to the given char buffer.
     */
    private void appendAuthUserName(CharBuffer cb, HttpServletRequest hreq) {
        String user = hreq.getRemoteUser();
        if (user == null) {
            user = NULL_VALUE;
        }
        cb.put(user);
    }


    /*
     * Appends the current date to the given char buffer.
     */
    private void appendCurrentDate(CharBuffer cb) {
        Date date = getDate();
        cb.put("[");
        cb.put(dayFormatter.get().format(date));           // Day
        cb.put('/');
        cb.put(lookup(monthFormatter.get().format(date))); // Month
        cb.put('/');
        cb.put(yearFormatter.get().format(date));          // Year
        cb.put(':');
        cb.put(timeFormatter.get().format(date));          // Time
        cb.put(SPACE);
        cb.put(timeZone);                                  // Time Zone
        cb.put("]");
    }


    /*
     * Appends info about the given request to the given char buffer.
     */
    private void appendRequestInfo(CharBuffer cb, HttpServletRequest hreq) {
        cb.put("\"");
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
        cb.put("\"");
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
}
