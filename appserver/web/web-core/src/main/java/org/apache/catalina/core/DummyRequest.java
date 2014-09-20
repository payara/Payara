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


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.security.Principal;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import javax.servlet.AsyncContext;
import javax.servlet.AsyncListener;
import javax.servlet.DispatcherType;
import javax.servlet.FilterChain;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;
import javax.servlet.http.HttpUpgradeHandler;

import org.apache.catalina.Connector;
import org.apache.catalina.Context;
import org.apache.catalina.Host;
import org.apache.catalina.HttpRequest;
import org.apache.catalina.Response;
import org.apache.catalina.Session;
import org.apache.catalina.Wrapper;
import org.glassfish.grizzly.http.util.DataChunk;

/**
 * Dummy request object, used for request dispatcher mapping, as well as
 * JSP precompilation.
 *
 * @author Remy Maucherat
 * @version $Revision: 1.5.6.2 $ $Date: 2008/04/17 18:37:07 $
 */

public class DummyRequest implements HttpRequest, HttpServletRequest {

    protected String queryString;
    protected String pathInfo;
    protected String servletPath;
    protected Wrapper wrapper;

    protected FilterChain filterChain;

    // START CR 6415120
    /**
     * Whether or not access to resources in WEB-INF or META-INF needs to be
     * checked.
     */
    protected boolean checkRestrictedResources = true;
    // END CR 6415120

    // START PWC 4707989
    private String method;
    // END PWC 4707989

    private static Enumeration<String> dummyEnum = new Enumeration<String>(){
        public boolean hasMoreElements(){
            return false;
        }
        public String nextElement(){
            return null;
        }
    };

    public String getContextPath() {
        return null;
    }

    public ServletRequest getRequest() {
        return this;
    }

    public ServletRequest getRequest(boolean maskDefaultContextMapping) {
        return getRequest();
    }

    public String getDecodedRequestURI() {
        return null;
    }

    public FilterChain getFilterChain() {
        return filterChain;
    }

    public void setFilterChain(FilterChain filterChain) {
        this.filterChain = filterChain;
    }

    public String getQueryString() {
        return queryString;
    }

    public void setQueryString(String query) {
        queryString = query;
    }

    public String getPathInfo() {
        return pathInfo;
    }

    public void setPathInfo(String path) {
        pathInfo = path;
    }

    public DataChunk getRequestPathMB() {
        return null;
    }

    public String getServletPath() {
        return servletPath;
    }

    public void setServletPath(String path) {
        servletPath = path;
    }

    public Wrapper getWrapper() {
        return wrapper;
    }

    public void setWrapper(Wrapper wrapper) {
        this.wrapper = wrapper;
    }

    // START PWC 4707989
    public void setMethod(String method) {
        this.method = method;
    }

    public String getMethod() {
        return method;
    }
    // END PWC 4707989

    public String getAuthorization() { return null; }
    public Connector getConnector() { return null; }
    public void setConnector(Connector connector) {}
    public Context getContext() { return null; }
    public void setContext(Context context) {}
    public Host getHost() { return null; }
    public void setHost(Host host) {}
    public String getInfo() { return null; }
    public Response getResponse() { return null; }
    public void setResponse(Response response) {}
    public Socket getSocket() { return null; }
    public void setSocket(Socket socket) {}
    public InputStream getStream() { return null; }
    public void setStream(InputStream input) {}
    public void addLocale(Locale locale) {}
    public ServletInputStream createInputStream() throws IOException {
        return null;
    }
    public void finishRequest() throws IOException {}
    public Object getNote(String name) { return null; }
    public Iterator<String> getNoteNames() { return null; }
    public void removeNote(String name) {}
    public void setContentType(String type) {}
    public void setNote(String name, Object value) {}
    public void setProtocol(String protocol) {}
    public void setRemoteAddr(String remoteAddr) {}
    public void setRemoteHost(String remoteHost) {}
    public void setServerName(String name) {}
    public void setServerPort(int port) {}
    public Object getAttribute(String name) { return null; }
    public Enumeration<String> getAttributeNames() { return null; }
    public String getCharacterEncoding() { return null; }
    public int getContentLength() { return -1; }
    public long getContentLengthLong() { return -1L; }
    public void setContentLength(int length) {}
    public String getContentType() { return null; }
    public ServletInputStream getInputStream() throws IOException {
        return null;
    }
    public Locale getLocale() { return null; }
    public Enumeration<Locale> getLocales() { return null; }
    public String getProtocol() { return null; }
    public BufferedReader getReader() throws IOException { return null; }
    public String getRealPath(String path) { return null; }
    public String getRemoteAddr() { return null; }
    public String getRemoteHost() { return null; }
    public String getScheme() { return null; }
    public String getServerName() { return null; }
    public int getServerPort() { return -1; }
    public boolean isSecure() { return false; }
    public void removeAttribute(String name) {}
    public void setAttribute(String name, Object value) {}
    public void setCharacterEncoding(String enc)
        throws UnsupportedEncodingException {}
    public void addCookie(Cookie cookie) {}
    public void addHeader(String name, String value) {}
    public void addParameter(String name, String values[]) {}
    public void clearCookies() {}
    public void clearHeaders() {}
    public void clearLocales() {}
    public void clearParameters() {}
    public void replayPayload(byte[] payloadByteArray) {}
    public void recycle() {}
    public void setAuthType(String authType) {}
    /* START PWC 4707989
    public void setMethod(String method) {}
    */
    public void setRequestedSessionCookie(boolean flag) {}
    public void setRequestedSessionId(String id) {}
    public void setRequestedSessionURL(boolean flag) {}
    public void setRequestURI(String uri) {}
    public void setSecure(boolean secure) {}
    public void setUserPrincipal(Principal principal) {}
    public String getParameter(String name) { return null; }
    public Map<String, String[]> getParameterMap() { return null; }
    public Enumeration<String> getParameterNames() { return dummyEnum; }
    public String[] getParameterValues(String name) { return null; }
    public RequestDispatcher getRequestDispatcher(String path) {
        return null;
    }
    public String getAuthType() { return null; }
    public Cookie[] getCookies() { return null; }
    public long getDateHeader(String name) { return -1; }
    public String getHeader(String name) { return null; }
    public Enumeration<String> getHeaders(String name) { return null; }
    public Enumeration<String> getHeaderNames() { return null; }
    public int getIntHeader(String name) { return -1; }
    /* START PWC 4707989
    public String getMethod() { return null; }
    */
    public String getPathTranslated() { return null; }
    public String getRemoteUser() { return null; }
    public String getRequestedSessionId() { return null; }
    public String getRequestURI() { return null; }
    public StringBuffer getRequestURL() { return null; }
    public HttpSession getSession() { return null; }
    public HttpSession getSession(boolean create) { return null; }
    public Session getSessionInternal(boolean create) { return null; }
    public String changeSessionId() { return null; }
    public boolean isRequestedSessionIdFromCookie() { return false; }
    public boolean isRequestedSessionIdFromURL() { return false; }
    public boolean isRequestedSessionIdFromUrl() { return false; }
    public boolean isRequestedSessionIdValid() { return false; }
    public void setRequestedSessionCookiePath(String cookiePath) {}
    public boolean isUserInRole(String role) { return false; }
    public Principal getUserPrincipal() { return null; }
    public String getLocalAddr() { return null; }    
    public String getLocalName() { return null; }
    public int getLocalPort() { return -1; }
    public int getRemotePort() { return -1; }
    public DispatcherType getDispatcherType() { return null; }
    public AsyncContext startAsync() throws IllegalStateException { return null; }
    public AsyncContext startAsync(ServletRequest servletRequest,
                                   ServletResponse servletResponse)
            throws IllegalStateException { return null; }
    public boolean isAsyncStarted() { return false; }
    public boolean isAsyncSupported() { return false; }
    public void setAsyncTimeout(long timeout) {}
    public long getAsyncTimeout() { return -1; }
    public AsyncContext getAsyncContext() { return null; }
    public void addAsyncListener(AsyncListener listener) {};
    public void addAsyncListener(AsyncListener listener,
                                 ServletRequest servletRequest,
                                 ServletResponse servletResponse) {}
    public boolean isSetAsyncTimeoutCalled() { return false; }
    public void disableAsyncSupport() {}
    public Collection<Part> getParts() {return null;}
    public Part getPart(String name) {return null;}
    public boolean authenticate(HttpServletResponse response)
        throws IOException, ServletException { return false; }
    public void login(String username, String password)
        throws ServletException {}
    public void logout() throws ServletException {}
    public <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass) { return null; }

    // START CR 6415120
    /**
     * Set whether or not access to resources under WEB-INF or META-INF
     * needs to be checked.
     */
    public void setCheckRestrictedResources(boolean check) {
        checkRestrictedResources = check;
    }

    /**
     * Return whether or not access to resources under WEB-INF or META-INF
     * needs to be checked.
     */
    public boolean getCheckRestrictedResources() {
        return checkRestrictedResources;
    }
    // END CR 6415120

    // START SJSAS 6346226
    /**
     * Gets the jroute id of this request, which may have been 
     * sent as a separate <code>JROUTE</code> cookie or appended to the
     * session identifier encoded in the URI (if cookies have been disabled).
     * 
     * @return The jroute id of this request, or null if this request does not
     * carry any jroute id
     */
    public String getJrouteId() {
        return null;
    }
    // END SJSAS 6346226
    
    /**
     * This object does not implement a session ID generator. Provide
     * a dummy implementation so that the default one will be used.
     */
    public String generateSessionId() {
        return null;
    }

    /**
     * Gets the servlet context to which this servlet request was last
     * dispatched.
     *
     * @return the servlet context to which this servlet request was last
     * dispatched
     */
    public ServletContext getServletContext() {
        return null;
    }

    public Session lockSession() {
        return null;
    }

    public void unlockSession() {}

}

