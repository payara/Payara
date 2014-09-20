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

package com.sun.appserv.web.cache.filter;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
// import org.apache.catalina.Context;

/**
 * a wrapper to HttpServletResponse to cache the outbound headers and content
 * @see javax.servlet.http.HttpServletResponseWrapper and 
 * @see javax.servlet.http.HttpServletResponse
 */
public class CachingResponseWrapper extends HttpServletResponseWrapper {
    private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

    int statusCode = HttpCacheEntry.VALUE_NOT_SET;

    /**
     * The HTTP headers explicitly added via addHeader(), but not including
     * those to be added with setContentLength(), setContentType(), and so on.
     * This collection is keyed by the header name, and the elements are
     * ArrayLists containing the associated values that have been set.
     *
     */
    HashMap<String, ArrayList<String>> headers =
        new HashMap<String, ArrayList<String>>();
    /**
     * cache all the set/addDateHeader calls
     */
    HashMap<String, ArrayList<Long>> dateHeaders =
        new HashMap<String, ArrayList<Long>>();
    /**
     * The set of Cookies associated with this Response.
     */
    ArrayList<Cookie> cookies = new ArrayList<Cookie>();

    int contentLength = HttpCacheEntry.VALUE_NOT_SET;
    String contentType;
    Locale locale;

    /**
     * Error flag. True if the response runs into an error.
     * Should not treat the response to be in the error state if the servlet
     * doesn't get the OutpuStream or the Writer.
     */
    // IT 12891
    boolean error = false;

    /**
     * OutputStream and PrintWriter objects for this response.
     */
    CachingOutputStreamWrapper cosw;
    PrintWriter writer;

    /** 
     * Constructs a response adaptor wrapping the given response.
     * @throws java.lang.IllegalArgumentException if the response is null
     */
    public CachingResponseWrapper(HttpServletResponse response) {
	    super(response);
    }
    
    /**
     * Create and return a ServletOutputStream to write the content
     * associated with this Response.
     *
     * @exception IOException if an input/output error occurs
     */
    private CachingOutputStreamWrapper createCachingOutputStreamWrapper()
                                throws IOException {
        return new CachingOutputStreamWrapper();
    }

    /**
     * Return the servlet output stream associated with this Response.
     *
     * @exception IllegalStateException if <code>getWriter</code> has
     *  already been called for this response
     * @exception IOException if an input/output error occurs
     */
    public ServletOutputStream getOutputStream() throws IOException {
        if (writer != null)
            throw new IllegalStateException ("getOutputStream<>getWriter");

        if (cosw == null) {
            cosw = createCachingOutputStreamWrapper();
        }

        return (ServletOutputStream)cosw;
    }  
      
    /**
     * Return the writer associated with this Response.
     *
     * @exception IllegalStateException if <code>getOutputStream</code> has
     *  already been called for this response
     * @exception IOException if an input/output error occurs
     */
    public PrintWriter getWriter() throws IOException {

        if (writer != null)
            return (writer);

        if (cosw != null)
            throw new IllegalStateException ("getWriter<>getOutputStream");

        cosw = createCachingOutputStreamWrapper();

        OutputStreamWriter osw =
            new OutputStreamWriter(cosw, getCharacterEncoding());

        writer = new PrintWriter(osw);

        return (writer);
	}
    
    /**
     * Set the content length (in bytes) for this Response.
     *
     * @param len The new content length
     */
    public void setContentLength(int len) {
        super.setContentLength(len);

        this.contentLength = len;
    }
    
    /**
     * Set the content type for this Response.
     *
     * @param type The new content type
     */
    public void setContentType(String type) {
        super.setContentType(type);

        this.contentType = type;
    }
    
    /**
     * Set the Locale that is appropriate for this response, including
     * setting the appropriate character encoding.
     *
     * @param locale The new locale
     */
    public void setLocale(Locale locale) {
        super.setLocale(locale);

        this.locale = locale;
    }

    /**
     * The default behavior of this method is to call addCookie(Cookie cookie)
     * on the wrapped response object.
     */
    public void addCookie(Cookie cookie) {
        super.addCookie(cookie);

        synchronized (cookies) {
            cookies.add(cookie);
        }
    }
    
    /**
     * Set the specified header to the specified value.
     *
     * @param name Name of the header to set
     * @param value Value to be set
     */
    public void setHeader(String name, String value) {
        super.setHeader(name, value);

        ArrayList<String> values = new ArrayList<String>();
        values.add(value);

        synchronized (headers) {
            headers.put(name, values);
        }
    }

    /**
     * Set the specified integer header to the specified value.
     *
     * @param name Name of the header to set
     * @param value Integer value to be set
     */
    public void setIntHeader(String name, int value) {
        setHeader(name, "" + value);
    }

    /**
     * Add the specified header to the specified value.
     *
     * @param name Name of the header to set
     * @param value Value to be set
     */
    public void addHeader(String name, String value) {
        super.addHeader(name, value);

        ArrayList<String> values = headers.get(name);
        if (values == null) {
            values = new ArrayList<String>();

            synchronized (headers) {
                headers.put(name, values);
            }
        }

        values.add(value);
    }

    /**
     * Add the specified integer header to the specified value.
     *
     * @param name Name of the header to set
     * @param value Integer value to be set
     */
    public void addIntHeader(String name, int value) {
        addHeader(name, "" + value);
    }

    /**
     * Set the specified date header to the specified value.
     *
     * @param name Name of the header to set
     * @param value Date value to be set
     */
    public void setDateHeader(String name, long value) {
        super.setDateHeader(name, value);

        ArrayList<Long> values = new ArrayList<Long>();
        values.add(Long.valueOf(value));

        synchronized (dateHeaders) {
            dateHeaders.put(name, values);
        }
    }

    /**
     * Add the specified date header to the specified value.
     *
     * @param name Name of the header to set
     * @param value Date value to be set
     */
    public void addDateHeader(String name, long value) {
        super.addDateHeader(name, value);

        ArrayList<Long> values = dateHeaders.get(name);
        if (values == null) {
            values = new ArrayList<Long>();

            synchronized (dateHeaders) {
                dateHeaders.put(name, values);
            }
        }

        values.add(Long.valueOf(value));
    }

    /**
     * Set the HTTP status to be returned with this response.
     *
     * @param sc The new HTTP status
     */
    public void setStatus(int sc) {
        super.setStatus(sc);

        this.statusCode = sc;
    }
 
    /**
     * Send an error response with the specified status and a
     * default message.
     *
     * @param status HTTP status code to send
     *
     * @exception IllegalStateException if this response has
     *  already been committed
     * @exception IOException if an input/output error occurs
     */
    public void sendError(int status) throws IOException {
        super.sendError(status);

        error = true;
    }

    /**
     * Send an error response with the specified status and message.
     *
     * @param status HTTP status code to send
     * @param message Corresponding message to send
     *
     * @exception IllegalStateException if this response has
     *  already been committed
     * @exception IOException if an input/output error occurs
     */
    public void sendError(int status, String message) throws IOException {
        super.sendError(status, message);

        error = true;
    }

    /**
     * has the response been set to error
     */
    public boolean isError() {
        return error;
    }

    /**
     * return the Expires: date header value
     */
    public Long getExpiresDateHeader() {
        Long expire = null;
        ArrayList<Long> expireList = dateHeaders.get("Expires");
        if (expireList != null && expireList.size() > 0) {
            expire = expireList.get(0);
        }
        return expire;
    }

    /**
     * called by doFilter to cache the response that was just sent out
     * @return the entry with cached response headers and body.
     */
    public HttpCacheEntry cacheResponse() throws IOException {
        // create a new entry
        HttpCacheEntry entry = new HttpCacheEntry();
        entry.responseHeaders = headers;
        entry.dateHeaders = dateHeaders;
        entry.cookies = cookies;

        entry.contentLength = contentLength;
        entry.contentType = contentType;
        entry.locale = locale;

        entry.statusCode = statusCode;

        // flush the writer??
        if (writer != null) {
            writer.flush();
        }


        // IT 12891
        entry.bytes = ((cosw != null)? cosw.getBytes() : EMPTY_BYTE_ARRAY);

        return entry;
    }
    
    /**
     * clear the contents of this wrapper
     */
    public void clear() {
        cosw = null;
        writer = null;
    }
}
