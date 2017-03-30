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

package org.apache.catalina.connector;


import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import com.sun.appserv.ProxyHandler;
import org.apache.catalina.Connector;
import org.apache.catalina.Context;
import org.apache.catalina.LogFacade;
import org.apache.catalina.Globals;
import org.apache.catalina.HttpResponse;
import org.apache.catalina.Session;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.security.SecurityUtil;
import org.apache.catalina.util.CharsetMapper;
import org.apache.catalina.util.RequestUtil;
import org.glassfish.grizzly.http.util.CharChunk;
import org.glassfish.grizzly.http.util.CookieSerializerUtils;
import org.glassfish.grizzly.http.util.CookieUtils;
import org.glassfish.grizzly.http.util.FastHttpDateFormat;
import org.glassfish.grizzly.http.util.MimeHeaders;
import org.glassfish.grizzly.http.util.UEncoder;
import org.glassfish.web.util.HtmlEntityEncoder;
// START S1AS 6170450

// END S1AS 6170450

/**
 * Wrapper object for the Coyote response.
 *
 * @author Remy Maucherat
 * @author Craig R. McClanahan
 * @version $Revision: 1.22 $ $Date: 2007/05/05 05:32:43 $
 */

public class Response
    implements HttpResponse, HttpServletResponse {

    // ------------------------------------------------------ Static variables

    private static final Logger log = LogFacade.getLogger();
    private static final ResourceBundle rb = log.getResourceBundle();


    /**
     * Whether or not to enforce scope checking of this object.
     */
    private static boolean enforceScope = false;

    public static final String HTTP_RESPONSE_DATE_HEADER =
        "EEE, dd MMM yyyy HH:mm:ss zzz";

    /**
     * Descriptive information about this Response implementation.
     */
    protected static final String info =
        "org.apache.catalina.connector.Response/1.0";


    // ----------------------------------------------------------- Constructors

    public Response() {
        // START OF SJSAS 6231069
        outputBuffer = new OutputBuffer();
        outputStream = new CoyoteOutputStream(outputBuffer);
        writer = createWriter(outputBuffer);
        // END OF SJSAS 6231069
        urlEncoder.addSafeCharacter('/');
    }
    
    // START OF SJSAS 6231069
    public Response(boolean chunkingDisabled) {
        outputBuffer = new OutputBuffer();
        outputStream = new CoyoteOutputStream(outputBuffer);
        writer = createWriter(outputBuffer);
        urlEncoder.addSafeCharacter('/');
    }
    // END OF SJSAS 6231069


    // ----------------------------------------------------- Instance Variables

    // BEGIN S1AS 4878272
    private String detailErrorMsg;
    // END S1AS 4878272

    /**
     * The date format we will use for creating date headers.
     */
    protected SimpleDateFormat format = null;

    /**
     * Associated context.
     */
    protected Context context = null;

    protected boolean upgrade = false;

    // ------------------------------------------------------------- Properties

    /**
     * Set whether or not to enforce scope checking of this object.
     */
    public static void setEnforceScope(boolean enforce) {

        enforceScope = enforce;

    }


    /**
     * Associated Catalina connector.
     */
    protected Connector connector;

    /**
     * Return the Connector through which this Request was received.
     */
    public Connector getConnector() {
        return this.connector;
    }

    /**
     * Set the Connector through which this Request was received.
     *
     * @param connector The new connector
     */
    public void setConnector(Connector connector) {
        this.connector = connector;
    }


    /**
     * Coyote response.
     */
    protected org.glassfish.grizzly.http.server.Response coyoteResponse;

    /**
     * Set the Coyote response.
     * 
     * @param coyoteResponse The Coyote response
     */
    public void setCoyoteResponse(org.glassfish.grizzly.http.server.Response coyoteResponse) {
        this.coyoteResponse = coyoteResponse;
        outputBuffer.setCoyoteResponse(this);
    }

    /**
     * Get the Coyote response.
     */
    public org.glassfish.grizzly.http.server.Response getCoyoteResponse() {
        return coyoteResponse;
    }


    /**
     * Return the Context within which this Request is being processed.
     */
    public Context getContext() {
        /*
         * Ideally, we would call CoyoteResponse.setContext() from
         * CoyoteAdapter (the same way we call it for CoyoteRequest), and
         * have getContext() return this context. However, for backwards
         * compatibility with WS 7.0's NSAPIProcessor, which does not call
         * CoyoteResponse.setContext(), we must delegate to the getContext()
         * method of the linked request object.
         */
        return request.getContext();
    }

    /**
     * Set the Context within which this Request is being processed.  This
     * must be called as soon as the appropriate Context is identified, because
     * it identifies the value to be returned by <code>getContextPath()</code>,
     * and thus enables parsing of the request URI.
     *
     * @param context The newly associated Context
     */
    public void setContext(Context context) {
        this.context = context;
    }


    /**
     * The associated output buffer.
     */
    // START OF SJSAS 6231069    
    //protected OutputBuffer outputBuffer = new OutputBuffer();
    protected OutputBuffer outputBuffer;
    // END OF SJSAS 6231069    

    /**
     * The associated output stream.
     */    
    // START OF SJSAS 6231069 
    /*protected CoyoteOutputStream outputStream =
        new CoyoteOutputStream(outputBuffer);*/
    protected CoyoteOutputStream outputStream;    
    // END OF SJSAS 6231069    

    /**
     * The associated writer.
     */
    // START OF SJSAS 6231069 
    // protected CoyoteWriter writer = new CoyoteWriter(outputBuffer);
    protected CoyoteWriter writer;
    // END OF SJSAS 6231069    
    

    /**
     * The application commit flag.
     */
    protected boolean appCommitted = false;


    /**
     * The included flag.
     */
    protected boolean included = false;

    
    /**
     * The characterEncoding flag
     */
    private boolean isCharacterEncodingSet = false;
    
    /**
     * The contextType flag
     */    
    private boolean isContentTypeSet = false;

    
    /**
     * The error flag.
     */
    protected boolean error = false;


    /**
     * Using output stream flag.
     */
    protected boolean usingOutputStream = false;


    /**
     * Using writer flag.
     */
    protected boolean usingWriter = false;


    /**
     * URL encoder.
     */
    protected UEncoder urlEncoder = new UEncoder();


    /**
     * Recyclable buffer to hold the redirect URL.
     */
    protected CharChunk redirectURLCC = new CharChunk();


    // --------------------------------------------------------- Public Methods


    /**
     * Release all object references, and initialize instance variables, in
     * preparation for reuse of this object.
     */
    public void recycle() {

        if (request != null && request.isAsyncStarted()) {
            return;
        }

        context = null;
        outputBuffer.recycle();
        usingOutputStream = false;
        usingWriter = false;
        appCommitted = false;
        included = false;
        error = false;
        isContentTypeSet = false;
        isCharacterEncodingSet = false;
        detailErrorMsg = null;

        if (enforceScope) {
            if (facade != null) {
                facade.clear();
                facade = null;
            }
            if (outputStream != null) {
                outputStream.clear();
                outputStream = null;
            }
            if (writer != null) {
                writer.clear();
                writer = null;
            }
        } else {
            writer.recycle();
        }

    }


    // ------------------------------------------------------- Response Methods


    /**
     * Return the number of bytes actually written to the output stream.
     */
    public int getContentCount() {
        return outputBuffer.getContentWritten();
    }


    /**
     * Set the application commit flag.
     * 
     * @param appCommitted The new application committed flag value
     */
    public void setAppCommitted(boolean appCommitted) {
        this.appCommitted = appCommitted;
    }


    /**
     * Application commit flag accessor.
     */
    public boolean isAppCommitted() {
        return this.appCommitted || isCommitted() || isSuspended()
                || getContentLength() > 0
                    && getContentCount() >= getContentLength();
    }


    /**
     * Return the "processing inside an include" flag.
     */
    public boolean getIncluded() {
        return included;
    }


    /**
     * Set the "processing inside an include" flag.
     *
     * @param included <code>true</code> if we are currently inside a
     *  RequestDispatcher.include(), else <code>false</code>
     */
    public void setIncluded(boolean included) {
        this.included = included;
    }


    /**
     * Return descriptive information about this Response implementation and
     * the corresponding version number, in the format
     * <code>&lt;description&gt;/&lt;version&gt;</code>.
     */
    public String getInfo() {
        return info;
    }


    /**
     * The request with which this response is associated.
     */
    protected Request request = null;

    /**
     * Return the Request with which this Response is associated.
     */
    public org.apache.catalina.Request getRequest() {
        return this.request;
    }

    /**
     * Set the Request with which this Response is associated.
     *
     * @param request The new associated request
     */
    public void setRequest(org.apache.catalina.Request request) {
        if (request instanceof Request) {
            this.request = (Request) request;
        }
    }


    /**
     * The facade associated with this response.
     */
    protected ResponseFacade facade = null;

    /**
     * Return the <code>ServletResponse</code> for which this object
     * is the facade.
     */
    public HttpServletResponse getResponse() {
        if (facade == null) {
            facade = new ResponseFacade(this);
        }
        return facade;
    }


    /**
     * Return the output stream associated with this Response.
     */
    public OutputStream getStream() {
        if (outputStream == null) {
            outputStream = new CoyoteOutputStream(outputBuffer);
        }
        return outputStream;
    }


    /**
     * Set the output stream associated with this Response.
     *
     * @param stream The new output stream
     */
    public void setStream(OutputStream stream) {
        // This method is evil
    }


    /**
     * Set the suspended flag.
     * 
     * @param suspended The new suspended flag value
     */
    public void setSuspended(boolean suspended) {
        outputBuffer.setSuspended(suspended);
    }


    /**
     * Suspended flag accessor.
     */
    public boolean isSuspended() {
        return outputBuffer.isSuspended();
    }


    /**
     * Set the error flag.
     */
    public void setError() {
        error = true;
    }


    /**
     * Error flag accessor.
     */
    public boolean isError() {
        return error;
    }


    // BEGIN S1AS 4878272
    /**
     * Sets detail error message.
     *
     * @param message detail error message
     */
    public void setDetailMessage(String message) {
        this.detailErrorMsg = message;
    }


    /**
     * Gets detail error message.
     *
     * @return the detail error message
     */
    public String getDetailMessage() {
        return this.detailErrorMsg;
    }
    // END S1AS 4878272


    /**
     * Create and return a ServletOutputStream to write the content
     * associated with this Response.
     *
     * @exception IOException if an input/output error occurs
     */
    public ServletOutputStream createOutputStream() 
        throws IOException {
        // Probably useless
        if (outputStream == null) {
            outputStream = new CoyoteOutputStream(outputBuffer);
        }
        return outputStream;
    }


    /**
     * Perform whatever actions are required to flush and close the output
     * stream or writer, in a single operation.
     *
     * @exception IOException if an input/output error occurs
     */
    public void finishResponse() 
        throws IOException {

        // Writing leftover bytes
        try {
            outputBuffer.close();
        } catch(IOException e) {
	    ;
        } catch(Throwable t) {
	    log(rb.getString(LogFacade.ERROR_DURING_FINISH_RESPONSE), t);
        }
    }


    /**
     * Return the content length that was set or calculated for this Response.
     */
    public int getContentLength() {
        return coyoteResponse.getContentLength();
    }


    /**
     * Return the content type that was set or calculated for this response,
     * or <code>null</code> if no content type was set.
     */
    public String getContentType() {
        return coyoteResponse.getContentType();
    }


    /**
     * Return a PrintWriter that can be used to render error messages,
     * regardless of whether a stream or writer has already been acquired.
     *
     * @return Writer which can be used for error reports. If the response is
     * not an error report returned using sendError or triggered by an
     * unexpected exception thrown during the servlet processing
     * (and only in that case), null will be returned if the response stream
     * has already been used.
     *
     * @exception IOException if an input/output error occurs
     */
    public PrintWriter getReporter() throws IOException {
        if (outputBuffer.isNew()) {
            outputBuffer.checkConverter();
            if (writer == null) {
                writer = createWriter(outputBuffer);
            }
            return writer;
        } else {
            return null;
        }
    }


    // ------------------------------------------------ ServletResponse Methods


    /**
     * Flush the buffer and commit this response.
     *
     * @exception IOException if an input/output error occurs
     */
    public void flushBuffer() 
        throws IOException {
        outputBuffer.flush();
    }


    /**
     * Return the actual buffer size used for this Response.
     */
    public int getBufferSize() {
        return outputBuffer.getBufferSize();
    }


    /**
     * Return the character encoding used for this Response.
     */
    public String getCharacterEncoding() {
        return coyoteResponse.getCharacterEncoding();
    }

    
    /*
     * Overrides the name of the character encoding used in the body
     * of the request. This method must be called prior to reading
     * request parameters or reading input using getReader().
     *
     * @param charset String containing the name of the character encoding.
     */
    public void setCharacterEncoding(String charset) {

        if (isCommitted())
            return;

        // Ignore any call from an included servlet
        if (included)
            return;

        // Ignore any call made after the getWriter has been invoked
        // The default should be used
        if (usingWriter)
            return;

        coyoteResponse.setCharacterEncoding(charset);
        isCharacterEncodingSet = true;
    }

    
    /**
     * Return the servlet output stream associated with this Response.
     *
     * @exception IllegalStateException if <code>getWriter</code> has
     *  already been called for this response
     * @exception IOException if an input/output error occurs
     */
    public ServletOutputStream getOutputStream() 
        throws IOException {

        if (usingWriter)
            throw new IllegalStateException
                (rb.getString(LogFacade.GET_WRITER_BEEN_CALLED_EXCEPTION));

        usingOutputStream = true;
        if (outputStream == null) {
            outputStream = new CoyoteOutputStream(outputBuffer);
        }
        return outputStream;

    }


    /**
     * Return the Locale assigned to this response.
     */
    public Locale getLocale() {
        return coyoteResponse.getLocale();
    }


    /**
     * Return the writer associated with this Response.
     *
     * @exception IllegalStateException if <code>getOutputStream</code> has
     *  already been called for this response
     * @exception IOException if an input/output error occurs
     */
    public PrintWriter getWriter() 
        throws IOException {

        if (usingOutputStream)
            throw new IllegalStateException(rb.getString(LogFacade.GET_OUTPUT_STREAM_BEEN_CALLED_EXCEPTION));

        /*
         * If the response's character encoding has not been specified as
         * described in <code>getCharacterEncoding</code> (i.e., the method
         * just returns the default value <code>ISO-8859-1</code>),
         * <code>getWriter</code> updates it to <code>ISO-8859-1</code>
         * (with the effect that a subsequent call to getContentType() will
         * include a charset=ISO-8859-1 component which will also be
         * reflected in the Content-Type response header, thereby satisfying
         * the Servlet spec requirement that containers must communicate the
         * character encoding used for the servlet response's writer to the
         * client).
         */
        setCharacterEncoding(getCharacterEncoding());

        usingWriter = true;
        outputBuffer.checkConverter();
        if (writer == null) {
            writer = createWriter(outputBuffer);
        }
        return writer;

    }


    /**
     * Has the output of this response already been committed?
     */
    public boolean isCommitted() {
        return coyoteResponse.isCommitted();
    }


    /**
     * Clear any content written to the buffer.
     *
     * @exception IllegalStateException if this response has already
     *  been committed
     */
    public void reset() {

        if (included)
            return;     // Ignore any call from an included servlet

        coyoteResponse.reset();
        outputBuffer.reset();
        // reset Grizzly duplicated internal attributes
        coyoteResponse.resetBuffer(true);
        usingOutputStream = false;
        usingWriter = false;
        isCharacterEncodingSet = false;
    }


    /**
     * Reset the data buffer but not any status or header information.
     *
     * @exception IllegalStateException if the response has already
     *  been committed
     */
    public void resetBuffer() {
        resetBuffer(false);
    }

    
    /**
     * Reset the data buffer and the using Writer/Stream flags but not any
     * status or header information.
     *
     * @param resetWriterStreamFlags <code>true</code> if the internal
     *        <code>usingWriter</code>, <code>usingOutputStream</code>,
     *        <code>isCharacterEncodingSet</code> flags should also be reset
     * 
     * @exception IllegalStateException if the response has already
     *  been committed
     */
    public void resetBuffer(boolean resetWriterStreamFlags) {

        if (isCommitted())
            throw new IllegalStateException(rb.getString(LogFacade.CANNOT_RESET_BUFFER_EXCEPTION));

        outputBuffer.reset();
                
        if(resetWriterStreamFlags) {
            usingOutputStream = false;
            usingWriter = false;
            isCharacterEncodingSet = false;
        }

    }


    /**
     * Set the buffer size to be used for this Response.
     *
     * @param size The new buffer size
     *
     * @exception IllegalStateException if this method is called after
     *  output has been committed for this response
     */
    public void setBufferSize(int size) {

        if (isCommitted() || !outputBuffer.isNew())
            throw new IllegalStateException(rb.getString(LogFacade.CANNOT_CHANGE_BUFFER_SIZE_EXCEPTION));

        outputBuffer.setBufferSize(size);

    }


    /**
     * Set the content length (in bytes) for this Response.
     *
     * @param length The new content length
     */
    public void setContentLength(int length) {

        setContentLengthLong(length);

    }


    /**
     * Sets the length of the content body in the response In HTTP servlets,
     * this method sets the HTTP Content-Length header.
     *
     * @param length The new content length
     */
    public void setContentLengthLong(long length) {

        if (isCommitted())
            return;

        // Ignore any call from an included servlet
        if (included)
            return;

        if (usingWriter)
            return;

        coyoteResponse.setContentLengthLong(length);

    }


    /**
     * Set the content type for this Response.
     *
     * @param type The new content type
     */
    public void setContentType(String type) {

        if (isCommitted())
            return;

        // Ignore any call from an included servlet
        if (included)
            return;

        // Ignore charset if getWriter() has already been called
        if (usingWriter) {
            if (type != null) {
                int index = type.indexOf(";");
                if (index != -1) {
                    type = type.substring(0, index);
                }
            }
        }

        coyoteResponse.setContentType(type);

        // Check to see if content type contains charset
        if (type != null) {
            int index = type.indexOf(";");
            if (index != -1) {
                int len = type.length();
                index++;
                while (index < len && Character.isWhitespace(type.charAt(index))) {
                    index++;
                }
                if (index+7 < len
                        && type.charAt(index) == 'c'
                        && type.charAt(index+1) == 'h'
                        && type.charAt(index+2) == 'a'
                        && type.charAt(index+3) == 'r'
                        && type.charAt(index+4) == 's'
                        && type.charAt(index+5) == 'e'
                        && type.charAt(index+6) == 't'
                        && type.charAt(index+7) == '=') {
                    isCharacterEncodingSet = true;
                }
            }
        }

        isContentTypeSet = true;    
    }


    /**
     * Set the Locale that is appropriate for this response, including
     * setting the appropriate character encoding.
     *
     * @param locale The new locale
     */
    public void setLocale(Locale locale) {

        if (isCommitted())
            return;

        // Ignore any call from an included servlet
        if (included)
            return;

        coyoteResponse.setLocale(locale);

        // Ignore any call made after the getWriter has been invoked.
        // The default should be used
        if (usingWriter)
            return;

        if (isCharacterEncodingSet) {
            return;
        }

        CharsetMapper cm = getContext().getCharsetMapper();
        String charset = cm.getCharset( locale );
        if ( charset != null ){
            coyoteResponse.setCharacterEncoding(charset);
        }

    }


    // --------------------------------------------------- HttpResponse Methods


    /**
     * Return the value for the specified header, or <code>null</code> if this
     * header has not been set.  If more than one value was added for this
     * name, only the first is returned; use {@link #getHeaders(String)} to
     * retrieve all of them.
     *
     * @param name Header name to look up
     */
    public String getHeader(String name) {
        return coyoteResponse.getHeader(name);
    }


    /**
     * @return a (possibly empty) <code>Collection</code> of the names
     * of the headers of this response
     */
    public Collection<String> getHeaderNames() {
        final Collection<String> result = new ArrayList<String>();
        for (final String headerName : coyoteResponse.getResponse().getHeaders().names()) {
            result.add(headerName);
        }

        return result;
    }


    /**
     * @param name the name of the response header whose values to return
     *
     * @return a (possibly empty) <code>Collection</code> of the values
     * of the response header with the given name
     */
    public Collection<String> getHeaders(String name) {
        final Collection<String> result = new ArrayList<String>();
        for (final String headerValue : coyoteResponse.getResponse().getHeaders().values(name)) {
            result.add(headerValue);
        }

        return result;
    }


    /**
     * Return the error message that was set with <code>sendError()</code>
     * for this Response.
     */
    public String getMessage() {
        return coyoteResponse.getMessage();
    }


    /**
     * Return the HTTP status code associated with this Response.
     */
    public int getStatus() {
        return coyoteResponse.getStatus();
    }


    /**
     * Reset this response, and specify the values for the HTTP status code
     * and corresponding message.
     *
     * @exception IllegalStateException if this response has already been
     *  committed
     */
    public void reset(int status, String message) {
        reset();
        setStatus(status, message);
    }


    // -------------------------------------------- HttpServletResponse Methods


    /**
     * Add the specified Cookie to those that will be included with
     * this Response.
     *
     * @param cookie Cookie to be added
     */
    public void addCookie(final Cookie cookie) {

        if (isCommitted())
            return;

        // Ignore any call from an included servlet
        if (included)
            return;

        /* GlassFish 898
        final StringBuilder sb = new StringBuilder();
        if (SecurityUtil.isPackageProtectionEnabled()) {
            AccessController.doPrivileged(new PrivilegedAction<Void>() {
                public Void run(){
                    ServerCookie.appendCookieValue
                        (sb, cookie.getVersion(), cookie.getName(), 
                         cookie.getValue(), cookie.getPath(), 
                         cookie.getDomain(), cookie.getComment(), 
                         cookie.getMaxAge(), cookie.getSecure());
                    return null;
                }
            });
        } else {
            ServerCookie.appendCookieValue
                (sb, cookie.getVersion(), cookie.getName(), cookie.getValue(),
                     cookie.getPath(), cookie.getDomain(), cookie.getComment(), 
                     cookie.getMaxAge(), cookie.getSecure());
        }
        */
        // START GlassFish 898
        String cookieValue = getCookieString(cookie);
        // END GlassFish 898

        // the header name is Set-Cookie for both "old" and v.1 ( RFC2109 )
        // RFC2965 is not supported by browsers and the Servlet spec
        // asks for 2109.
        /* GlassFish 898
        addHeader("Set-Cookie", sb.toString());
        */
        // START GlassFish 898
        addHeader("Set-Cookie", cookieValue);
        // END GlassFish 898
    }

    /**
     * Special method for adding a session cookie as we should be overriding 
     * any previous 
     * @param cookie
     */
    public void addSessionCookieInternal(final Cookie cookie) {
        if (isCommitted())
            return;

        String name = cookie.getName();
        final String headername = "Set-Cookie";
        final String startsWith = name + "=";
        final String cookieString = getCookieString(cookie);
        boolean set = false;
        MimeHeaders headers = coyoteResponse.getResponse().getHeaders();
        int n = headers.size();
        for (int i = 0; i < n; i++) {
            if (headers.getName(i).toString().equals(headername)) {
                if (headers.getValue(i).toString().startsWith(startsWith)) {
                    headers.getValue(i).setString(cookieString);
                    set = true;
                }
            }
        }
        if (!set) {
            addHeader(headername, cookieString);
        }


    }

    /**
     * Add the specified date header to the specified value.
     *
     * @param name Name of the header to set
     * @param value Date value to be set
     */
    public void addDateHeader(String name, long value) {

        if (name == null || name.length() == 0) {
            return;
        }

        if (isCommitted())
            return;

        // Ignore any call from an included servlet
        if (included) {
            return;
        }

        if (format == null) {
            format = new SimpleDateFormat(HTTP_RESPONSE_DATE_HEADER,
                                          Locale.US);
            format.setTimeZone(TimeZone.getTimeZone("GMT"));
        }

        addHeader(name, FastHttpDateFormat.formatDate(value, format));

    }


    /**
     * Add the specified header to the specified value.
     *
     * @param name Name of the header to set
     * @param value Value to be set
     */
    public void addHeader(String name, String value) {

        if (name == null || name.length() == 0 || value == null) {
            return;
        }

        if (isCommitted())
            return;

        // Ignore any call from an included servlet
        if (included)
            return;

        coyoteResponse.addHeader(name, value);

    }


    /**
     * Add the specified integer header to the specified value.
     *
     * @param name Name of the header to set
     * @param value Integer value to be set
     */
    public void addIntHeader(String name, int value) {

        if (name == null || name.length() == 0) {
            return;
        }

        if (isCommitted())
            return;

        // Ignore any call from an included servlet
        if (included)
            return;

        addHeader(name, "" + value);

    }


    /**
     * Has the specified header been set already in this response?
     *
     * @param name Name of the header to check
     */
    public boolean containsHeader(String name) {
        return coyoteResponse.containsHeader(name);
    }


    /**
     * Encode the session identifier associated with this response
     * into the specified redirect URL, if necessary.
     *
     * @param url URL to be encoded
     */
    public String encodeRedirectURL(String url) {
        if (isEncodeable(toAbsolute(url))) {
            String sessionVersion = null;
            Map<String, String> sessionVersions = 
                request.getSessionVersionsRequestAttribute();
            if (sessionVersions != null) {
                sessionVersion = RequestUtil.createSessionVersionString(
                    sessionVersions);
            }
            return toEncoded(url,
                              request.getSessionInternal().getIdInternal(),
                              sessionVersion);
        } else {
            return url;
        }
    }


    /**
     * Encode the session identifier associated with this response
     * into the specified redirect URL, if necessary.
     *
     * @param url URL to be encoded
     *
     * @deprecated As of Version 2.1 of the Java Servlet API, use
     *  <code>encodeRedirectURL()</code> instead.
     */
    public String encodeRedirectUrl(String url) {
        return encodeRedirectURL(url);
    }


    /**
     * Encode the session identifier associated with this response
     * into the specified URL, if necessary.
     *
     * @param url URL to be encoded
     */
    public String encodeURL(String url) {
        String absolute = toAbsolute(url);
        if (isEncodeable(absolute)) {
            // W3c spec clearly said 
            if (url.equalsIgnoreCase("")){
                url = absolute;
            } else if (url.equals(absolute) && !hasPath(url)) {
                url += '/';
            }
            String sessionVersion = null;
            Map<String, String> sessionVersions = 
                request.getSessionVersionsRequestAttribute();
            if (sessionVersions != null) {
                sessionVersion = RequestUtil.createSessionVersionString(
                    sessionVersions);
            }
            return toEncoded(url,
                              request.getSessionInternal().getIdInternal(),
                              sessionVersion);
        } else {
            return url;
        }
    }


    /**
     * Encode the session identifier associated with this response
     * into the specified URL, if necessary.
     *
     * @param url URL to be encoded
     *
     * @deprecated As of Version 2.1 of the Java Servlet API, use
     *  <code>encodeURL()</code> instead.
     */
    public String encodeUrl(String url) {
        return encodeURL(url);
    }


    /**
     * Apply URL Encoding to the given URL without adding session identifier
     * et al associated to this response.
     *
     * @param url URL to be encoded
     */
    public String encode(String url) {
        return urlEncoder.encodeURL(url);
    }


    /**
     * Send an acknowledgment of a request.
     * 
     * @exception IOException if an input/output error occurs
     */
    public void sendAcknowledgement()
        throws IOException {

        if (isCommitted())
            return;

        // Ignore any call from an included servlet
        if (included)
            return; 

        coyoteResponse.sendAcknowledgement();

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
    public void sendError(int status) 
        throws IOException {
        sendError(status, null);
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
    public void sendError(int status, String message) 
        throws IOException {

        if (isCommitted())
            throw new IllegalStateException(rb.getString(LogFacade.CANNOT_CALL_SEND_ERROR_EXCEPTION));

        // Ignore any call from an included servlet
        if (included) {
            return; 
        }

        setError();

        coyoteResponse.setStatus(status);
        // use encoding in GlassFish
        coyoteResponse.getResponse().setHtmlEncodingCustomReasonPhrase(false);
        coyoteResponse.setDetailMessage(HtmlEntityEncoder.encodeXSS(message));

        // Clear any data content that has been buffered
        resetBuffer();

        // Cause the response to be finished (from the application perspective)
        setSuspended(true);

    }


    /**
     * Sends a temporary redirect to the specified redirect location URL.
     *
     * @param location Location URL to redirect to
     *
     * @throws IllegalStateException if this response has
     *  already been committed
     * @throws IOException if an input/output error occurs
     */
    public void sendRedirect(String location) throws IOException {
        sendRedirect(location, true);
    }


    /**
     * Sends a temporary or permanent redirect to the specified redirect
     * location URL.
     *
     * @param location Location URL to redirect to
     * @param isTemporary true if the redirect is supposed to be temporary,
     * false if permanent
     *
     * @throws IllegalStateException if this response has
     *  already been committed
     * @throws IOException if an input/output error occurs
     */    
    public void sendRedirect(String location, boolean isTemporary)
            throws IOException {

        if (isCommitted())
            throw new IllegalStateException(rb.getString(LogFacade.CANNOT_CALL_SEND_REDIRECT_EXCEPTION));

        // Ignore any call from an included servlet
        if (included)
            return; 

        // Clear any data content that has been buffered
        resetBuffer();

        // Generate a temporary redirect to the specified location
        try {
            /* RIMOD 4642650
            String absolute = toAbsolute(location);
            */
            // START RIMOD 4642650
            String absolute;
            if (getContext().getAllowRelativeRedirect())
                absolute = location;
            else
                absolute = toAbsolute(location);
            // END RIMOD 4642650
            if (isTemporary) {
                setStatus(SC_MOVED_TEMPORARILY);
            } else {
                setStatus(SC_MOVED_PERMANENTLY);
            }
            setHeader("Location", absolute);

            // According to RFC2616 section 10.3.3 302 Found,
            // the response SHOULD contain a short hypertext note with
            // a hyperlink to the new URI.
            setContentType("text/html");
            setLocale(Locale.getDefault());

            String href = HtmlEntityEncoder.encodeXSS(absolute);
            StringBuilder sb = new StringBuilder(150 + href.length());

            sb.append("<html>\r\n");
            sb.append("<head><title>Document moved</title></head>\r\n");
            sb.append("<body><h1>Document moved</h1>\r\n");
            sb.append("This document has moved <a href=\"");
            sb.append(href);
            sb.append("\">here</a>.<p>\r\n");
            sb.append("</body>\r\n");
            sb.append("</html>\r\n");

            try {
                getWriter().write(sb.toString());
            } catch (IllegalStateException ise1) {
                try {
                    getOutputStream().print(sb.toString());
                } catch (IllegalStateException ise2) {
                    // ignore; the RFC says "SHOULD" so it is acceptable
                    // to omit the body in case of an error
                }
            }
        } catch (IllegalArgumentException e) {
            setStatus(SC_NOT_FOUND);
        }

        // Cause the response to be finished (from the application perspective)
        setSuspended(true);

    }


    /**
     * Set the specified date header to the specified value.
     *
     * @param name Name of the header to set
     * @param value Date value to be set
     */
    public void setDateHeader(String name, long value) {

        if (name == null || name.length() == 0) {
            return;
        }

        if (isCommitted())
            return;

        // Ignore any call from an included servlet
        if (included) {
            return;
        }

        if (format == null) {
            format = new SimpleDateFormat(HTTP_RESPONSE_DATE_HEADER,
                                          Locale.US);
            format.setTimeZone(TimeZone.getTimeZone("GMT"));
        }

        setHeader(name, FastHttpDateFormat.formatDate(value, format));

    }


    /**
     * Set the specified header to the specified value.
     *
     * @param name Name of the header to set
     * @param value Value to be set
     */
    public void setHeader(String name, String value) {

        if (name == null || name.length() == 0 || value == null) {
            return;
        }

        if (isCommitted())
            return;

        // Ignore any call from an included servlet
        if (included)
            return;

        coyoteResponse.setHeader(name, value);

    }


    /**
     * Set the specified integer header to the specified value.
     *
     * @param name Name of the header to set
     * @param value Integer value to be set
     */
    public void setIntHeader(String name, int value) {

        if (name == null || name.length() == 0) {
            return;
        }

        if (isCommitted())
            return;

        // Ignore any call from an included servlet
        if (included)
            return;

        setHeader(name, "" + value);

    }


    /**
     * Set the HTTP status to be returned with this response.
     *
     * @param status The new HTTP status
     */
    public void setStatus(int status) {
        setStatus(status, null);
    }


    /**
     * Set the HTTP status and message to be returned with this response.
     *
     * @param status The new HTTP status
     * @param message The associated text message
     *
     * @deprecated As of Version 2.1 of the Java Servlet API, this method
     *  has been deprecated due to the ambiguous meaning of the message
     *  parameter.
     */
    public void setStatus(int status, String message) {

        if (isCommitted())
            return;

        // Ignore any call from an included servlet
        if (included)
            return;

        coyoteResponse.setStatus(status);
        // use encoding in GlassFish
        coyoteResponse.getResponse().setHtmlEncodingCustomReasonPhrase(false);
        coyoteResponse.setDetailMessage(HtmlEntityEncoder.encodeXSS(message));
    }


    // ------------------------------------------------------ Protected Methods


    /**
     * Return <code>true</code> if the specified URL should be encoded with
     * a session identifier.  This will be true if all of the following
     * conditions are met:
     * <ul>
     * <li>The request we are responding to asked for a valid session
     * <li>The requested session ID was not received via a cookie
     * <li>The specified URL points back to somewhere within the web
     *     application that is responding to this request
     * </ul>
     *
     * @param location Absolute URL to be validated
     */
    protected boolean isEncodeable(final String location) {

        if (location == null)
            return false;

        // Is this an intra-document reference?
        if (location.startsWith("#"))
            return false;

        // Are we in a valid session that is not using cookies?
        final Request hreq = request;
        final Session session = hreq.getSessionInternal(false);
        if (session == null) {
            return false;
        }
        if (hreq.isRequestedSessionIdFromCookie() ||
            getContext() != null && !getContext().isEnableURLRewriting()) {
            return false;
        }

        if (SecurityUtil.isPackageProtectionEnabled()) {
            return (
                AccessController.doPrivileged(new PrivilegedAction<Boolean>() {

                @Override
                public Boolean run(){
                    return Boolean.valueOf(doIsEncodeable(hreq, session, location));
                }
            })).booleanValue();
        } else {
            return doIsEncodeable(hreq, session, location);
        }
    }

    private boolean doIsEncodeable(Request hreq, Session session,
                                   String location){
        // Is this a valid absolute URL?
        URL url = null;
        try {
            url = new URL(location);
        } catch (MalformedURLException e) {
            return false;
        }

        // Does this URL match down to (and including) the context path?
        if (!hreq.getScheme().equalsIgnoreCase(url.getProtocol()))
            return false;
        if (!hreq.getServerName().equalsIgnoreCase(url.getHost()))
            return false;
        int serverPort = hreq.getServerPort();
        if (serverPort == -1) {
            if ("https".equals(hreq.getScheme()))
                serverPort = 443;
            else
                serverPort = 80;
        }
        int urlPort = url.getPort();
        if (urlPort == -1) {
            if ("https".equals(url.getProtocol()))
                urlPort = 443;
            else
                urlPort = 80;
        }
        if (serverPort != urlPort)
            return false;

        Context ctx = getContext();
        if (ctx != null) {
            String contextPath = ctx.getPath();
            if (contextPath != null) {
                String file = url.getFile();
                if (file == null || !file.startsWith(contextPath)) {
                    return false;
                }
                String sessionParamName = ctx.getSessionParameterName();
                if (file.contains(";" + sessionParamName + "=" + session.getIdInternal())) {
                    return false;
                }
            }
        }

        // This URL belongs to our web application, so it is encodeable
        return true;

    }


    /**
     * Convert (if necessary) and return the absolute URL that represents the
     * resource referenced by this possibly relative URL.  If this URL is
     * already absolute, return it unchanged.
     *
     * @param location URL to be (possibly) converted and then returned
     *
     * @exception IllegalArgumentException if a MalformedURLException is
     *  thrown when converting the relative URL to an absolute one
     */
    protected String toAbsolute(String location) {

        if (location == null)
            return location;

        boolean leadingSlash = location.startsWith("/");

        if (location.startsWith("//")) {
            // Scheme relative, network-path reference in RFC 3986
            redirectURLCC.recycle();
            // Add the scheme
            String scheme = getRedirectScheme();
            try {
                redirectURLCC.append(scheme, 0, scheme.length());
                redirectURLCC.append(':');
                redirectURLCC.append(location, 0, location.length());
                return redirectURLCC.toString();
            } catch (IOException e) {
                IllegalArgumentException iae =
                    new IllegalArgumentException(location);
                iae.initCause(e);
                throw iae;
            }

        } else if (leadingSlash || (location.indexOf("://") == -1)) {

            redirectURLCC.recycle();

            String scheme = getRedirectScheme();

            String name = request.getServerName();
            int port = request.getServerPort();

            try {
                redirectURLCC.append(scheme, 0, scheme.length());
                redirectURLCC.append("://", 0, 3);
                redirectURLCC.append(name, 0, name.length());
                if (scheme.equals("http") && port != 80
                    || scheme.equals("https") && port != 443) {
                    redirectURLCC.append(':');
                    String portS = port + "";
                    redirectURLCC.append(portS, 0, portS.length());
                }
                if (!leadingSlash) {
                    String relativePath = request.getDecodedRequestURI();
                    int pos = relativePath.lastIndexOf('/');
                    relativePath = relativePath.substring(0, pos);
                    
                    String encodedURI = null;
                    final String frelativePath = relativePath;
                    
                     if (SecurityUtil.isPackageProtectionEnabled() ){
                        try{
                            encodedURI = AccessController.doPrivileged( 
                                new PrivilegedExceptionAction<String>(){                                
                                    public String run() throws IOException{
                                        return urlEncoder.encodeURL(frelativePath);
                                    }
                           });   
                        } catch (PrivilegedActionException pae){
                            IllegalArgumentException iae =
                                new IllegalArgumentException(location);
                            iae.initCause(pae.getCause());
                            throw iae;
                        }
                    } else {
                        encodedURI = urlEncoder.encodeURL(relativePath);
                    }
                          
                    redirectURLCC.append(encodedURI, 0, encodedURI.length());
                    redirectURLCC.append('/');
                }
                redirectURLCC.append(location, 0, location.length());
                normalize(redirectURLCC);
            } catch (IOException e) {
                IllegalArgumentException iae =
                    new IllegalArgumentException(location);
                iae.initCause(e);
                throw iae;
            }

            return redirectURLCC.toString();

        } else {

            return location;

        }

    }


    /**
     * Returns the scheme for a redirect if it is not specified.
     */
    private String getRedirectScheme() {
        String scheme = request.getScheme();

        // START S1AS 6170450
        if (getConnector() != null
                && getConnector().getAuthPassthroughEnabled()) {
            ProxyHandler proxyHandler = getConnector().getProxyHandler();
            if (proxyHandler != null
                    && proxyHandler.getSSLKeysize(request) > 0) {
                scheme = "https";
            }
        }
        // END S1AS 6170450

        return scheme;
    }


    /**
     * Return the specified URL with the specified session identifier
     * suitably encoded.
     *
     * @param url URL to be encoded with the session id
     * @param sessionId Session id to be included in the encoded URL
     */
    protected String toEncoded(String url, String sessionId) {
        return toEncoded(url, sessionId, null);
    }


    /**
     * Return the specified URL with the specified session identifier
     * suitably encoded.
     *
     * @param url URL to be encoded with the session id
     * @param sessionId Session id to be included in the encoded URL
     * @param sessionVersion Session version to be included in the encoded URL
     */
    private String toEncoded(String url, String sessionId,
                             String sessionVersion) {
        if (url == null || sessionId == null)
            return url;

        String path = url;
        String query = "";
        String anchor = "";
        int question = url.indexOf('?');
        if (question >= 0) {
            path = url.substring(0, question);
            query = url.substring(question);
        }
        int pound = path.indexOf('#');
        if (pound >= 0) {
            anchor = path.substring(pound);
            path = path.substring(0, pound);
        }

        StringBuilder sb = new StringBuilder(path);
        if( sb.length() > 0 ) { // jsessionid can't be first.
            StandardContext ctx = (StandardContext) getContext();
            String sessionParamName =
                ctx != null ? ctx.getSessionParameterName() :
                    Globals.SESSION_PARAMETER_NAME;
            sb.append(";" + sessionParamName + "=");
            sb.append(sessionId);
            if (ctx != null && ctx.getJvmRoute() != null) {
                sb.append('.').append(ctx.getJvmRoute());
            }                    

            // START SJSAS 6337561
            String jrouteId = request.getHeader(Constants.PROXY_JROUTE);
            if (jrouteId != null) {
                sb.append(":");
                sb.append(jrouteId);
            }
            // END SJSAS 6337561

            final Session session = request.getSessionInternal(false);
            if (session != null) {
                String replicaLocation =
                    (String) session.getNote(Globals.JREPLICA_SESSION_NOTE);
                if (replicaLocation != null) {
                    sb.append(Globals.JREPLICA_PARAMETER);
                    sb.append(replicaLocation);
                }
            }

            if (sessionVersion != null) {
                sb.append(Globals.SESSION_VERSION_PARAMETER);
                sb.append(sessionVersion);
            }
        }

        sb.append(anchor);
        sb.append(query);
        return sb.toString();

    }

    /**
     * Create an instance of CoyoteWriter
     */
    protected CoyoteWriter createWriter(OutputBuffer outbuf) {
        return new CoyoteWriter(outbuf);
    }

    // START GlassFish 898
    /**
     * Gets the string representation of the given cookie.
     *
     * @param cookie The cookie whose string representation to get
     *
     * @return The cookie's string representation
     */
    protected String getCookieString(final Cookie cookie) {
        String cookieValue = null;
        final StringBuilder sb = new StringBuilder();

        // TODO:  default these values for now.  update later.
        final boolean versionOneStrictCompliance = CookieUtils.COOKIE_VERSION_ONE_STRICT_COMPLIANCE;
        final boolean alwaysAddExpires = CookieUtils.ALWAYS_ADD_EXPIRES;
        final boolean rfc6265Support = CookieUtils.RFC_6265_SUPPORT_ENABLED;
        if (SecurityUtil.isPackageProtectionEnabled()) {
            cookieValue = AccessController.doPrivileged(
                new PrivilegedAction<String>() {
                    public String run(){
                        CookieSerializerUtils.serializeServerCookie(
                            sb, versionOneStrictCompliance, rfc6265Support,
                            alwaysAddExpires, cookie.getName(),
                            cookie.getValue(), cookie.getVersion(), cookie.getPath(),
                            cookie.getDomain(), cookie.getComment(),
                            cookie.getMaxAge(), cookie.getSecure(),
                            cookie.isHttpOnly());
                        return sb.toString();
                    }
                });
        } else {
            CookieSerializerUtils.serializeServerCookie(
                sb, versionOneStrictCompliance, rfc6265Support,
                alwaysAddExpires, cookie.getName(),
                cookie.getValue(), cookie.getVersion(), cookie.getPath(),
                cookie.getDomain(), cookie.getComment(),
                cookie.getMaxAge(), cookie.getSecure(),
                cookie.isHttpOnly());
            cookieValue = sb.toString();
        }

        return cookieValue;
    }
    // END GlassFish 898


    // START GlassFish 896
    /**
     * Removes any Set-Cookie response headers whose value contains the
     * string JSESSIONID
     */
    public void removeSessionCookies() {
        String matchExpression = "^" + getContext().getSessionCookieName() + "=.*";
        coyoteResponse.getResponse().getHeaders().removeHeaderMatches("Set-Cookie", matchExpression);
        matchExpression = "^" +
            org.apache.catalina.authenticator.Constants.SINGLE_SIGN_ON_COOKIE + "=.*";
        coyoteResponse.getResponse().getHeaders().removeHeaderMatches("Set-Cookie", matchExpression);
    }
    // END GlassFish 896

    /*
     * Removes /./ and /../ sequences from absolute URLs.
     * Code borrowed heavily from CoyoteAdapter.normalize()
     */
    private void normalize(CharChunk cc) {
        // Strip query string and/or fragment first as doing it this way makes
        // the normalization logic a lot simpler
        int truncate = cc.indexOf('?');
        if (truncate == -1) {
            truncate = cc.indexOf('#');
        }
        char[] truncateCC = null;
        if (truncate > -1) {
            truncateCC = Arrays.copyOfRange(cc.getBuffer(),
                    cc.getStart() + truncate, cc.getEnd());
            cc.setEnd(cc.getStart() + truncate);
        }

        if (cc.endsWith("/.") || cc.endsWith("/..")) {
            try {
                cc.append('/');
            } catch (IOException e) {
                throw new IllegalArgumentException(cc.toString(), e);
            }
        }

        char[] c = cc.getChars();
        int start = cc.getStart();
        int end = cc.getEnd();
        int index = 0;
        int startIndex = 0;

        // Advance past the first three / characters (should place index just
        // scheme://host[:port]

        for (int i = 0; i < 3; i++) {
            startIndex = cc.indexOf('/', startIndex + 1);
        }

        // Remove /./
        index = startIndex;
        while (true) {
            index = cc.indexOf("/./", 0, 3, index);
            if (index < 0) {
                break;
            }
            copyChars(c, start + index, start + index + 2,
                      end - start - index - 2);
            end = end - 2;
            cc.setEnd(end);
        }

        // Remove /../
        index = startIndex;
        int pos;
        while (true) {
            index = cc.indexOf("/../", 0, 4, index);
            if (index < 0) {
                break;
            }
            // Can't go above the server root
            if (index == startIndex) {
                throw new IllegalArgumentException();
            }
            int index2 = -1;
            for (pos = start + index - 1; (pos >= 0) && (index2 < 0); pos --) {
                if (c[pos] == (byte) '/') {
                    index2 = pos;
                }
            }
            copyChars(c, start + index2, start + index + 3,
                      end - start - index - 3);
            end = end + index2 - index - 3;
            cc.setEnd(end);
            index = index2;
        }

        // Add the query string and/or fragment (if present) back in
        if (truncateCC != null) {
            try {
                cc.append(truncateCC, 0, truncateCC.length);
            } catch (IOException ioe) {
                throw new IllegalArgumentException(ioe);
            }
        }
    }

    private void copyChars(char[] c, int dest, int src, int len) {
        for (int pos = 0; pos < len; pos++) {
            c[pos + dest] = c[pos + src];
        }
    }

    /**
     * Determine if an absolute URL has a path component
     */
    private boolean hasPath(String uri) {
        int pos = uri.indexOf("://");
        if (pos < 0) {
            return false;
        }
        pos = uri.indexOf('/', pos + 3);
        if (pos < 0) {
            return false;
        }
        return true;
    }

    private void log(String message, Throwable t) {
        org.apache.catalina.Logger logger = null;
        if (connector != null && connector.getContainer() != null) {
            logger = connector.getContainer().getLogger();
        }
        String localName = "Response";
        if (logger != null) {
            logger.log(localName + " " + message, t,
                org.apache.catalina.Logger.WARNING);
        } else {
            log.log(Level.WARNING, localName + " " + message, t);
        }
    }


    public void setUpgrade(boolean upgrade) {
        this.upgrade = upgrade;
    }

    void disableWriteHandler() {
        outputBuffer.disableWriteHandler();
    }
}

