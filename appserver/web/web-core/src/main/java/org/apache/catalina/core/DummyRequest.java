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
        @Override
        public boolean hasMoreElements(){
            return false;
        }
        @Override
        public String nextElement(){
            return null;
        }
    };

    @Override
    public String getContextPath() {
        return null;
    }

    @Override
    public ServletRequest getRequest() {
        return this;
    }

    @Override
    public ServletRequest getRequest(boolean maskDefaultContextMapping) {
        return getRequest();
    }

    @Override
    public String getDecodedRequestURI() {
        return null;
    }

    @Override
    public FilterChain getFilterChain() {
        return filterChain;
    }

    @Override
    public void setFilterChain(FilterChain filterChain) {
        this.filterChain = filterChain;
    }

    @Override
    public String getQueryString() {
        return queryString;
    }

    @Override
    public void setQueryString(String query) {
        queryString = query;
    }

    @Override
    public String getPathInfo() {
        return pathInfo;
    }

    @Override
    public void setPathInfo(String path) {
        pathInfo = path;
    }

    @Override
    public DataChunk getRequestPathMB() {
        return null;
    }

    @Override
    public String getServletPath() {
        return servletPath;
    }

    @Override
    public void setServletPath(String path) {
        servletPath = path;
    }

    @Override
    public Wrapper getWrapper() {
        return wrapper;
    }

    @Override
    public void setWrapper(Wrapper wrapper) {
        this.wrapper = wrapper;
    }

    // START PWC 4707989
    @Override
    public void setMethod(String method) {
        this.method = method;
    }

    @Override
    public String getMethod() {
        return method;
    }
    // END PWC 4707989

    @Override
    public String getAuthorization() { return null; }
    @Override
    public Connector getConnector() { return null; }
    @Override
    public void setConnector(Connector connector) {}
    @Override
    public Context getContext() { return null; }
    @Override
    public void setContext(Context context) {}
    @Override
    public Host getHost() { return null; }
    @Override
    public void setHost(Host host) {}
    @Override
    public String getInfo() { return null; }
    @Override
    public Response getResponse() { return null; }
    @Override
    public void setResponse(Response response) {}
    @Override
    public Socket getSocket() { return null; }
    @Override
    public void setSocket(Socket socket) {}
    @Override
    public InputStream getStream() { return null; }
    @Override
    public void setStream(InputStream input) {}
    @Override
    public void addLocale(Locale locale) {}
    @Override
    public ServletInputStream createInputStream() throws IOException {
        return null;
    }
    @Override
    public void finishRequest() throws IOException {}
    @Override
    public Object getNote(String name) { return null; }
    @Override
    public Iterator<String> getNoteNames() { return null; }
    @Override
    public void removeNote(String name) {}
    @Override
    public void setContentType(String type) {}
    @Override
    public void setNote(String name, Object value) {}
    @Override
    public void setProtocol(String protocol) {}
    @Override
    public void setRemoteAddr(String remoteAddr) {}
    public void setRemoteHost(String remoteHost) {}
    @Override
    public void setServerName(String name) {}
    @Override
    public void setServerPort(int port) {}
    @Override
    public Object getAttribute(String name) { return null; }
    @Override
    public Enumeration<String> getAttributeNames() { return null; }
    @Override
    public String getCharacterEncoding() { return null; }
    @Override
    public int getContentLength() { return -1; }
    @Override
    public long getContentLengthLong() { return -1L; }
    @Override
    public void setContentLength(int length) {}
    @Override
    public String getContentType() { return null; }
    @Override
    public ServletInputStream getInputStream() throws IOException {
        return null;
    }
    @Override
    public Locale getLocale() { return null; }
    @Override
    public Enumeration<Locale> getLocales() { return null; }
    @Override
    public String getProtocol() { return null; }
    @Override
    public BufferedReader getReader() throws IOException { return null; }
    @Override
    public String getRealPath(String path) { return null; }
    @Override
    public String getRemoteAddr() { return null; }
    @Override
    public String getRemoteHost() { return null; }
    @Override
    public String getScheme() { return null; }
    @Override
    public String getServerName() { return null; }
    @Override
    public int getServerPort() { return -1; }
    @Override
    public boolean isSecure() { return false; }
    @Override
    public void removeAttribute(String name) {}
    @Override
    public void setAttribute(String name, Object value) {}
    @Override
    public void setCharacterEncoding(String enc)
        throws UnsupportedEncodingException {}
    @Override
    public void addCookie(Cookie cookie) {}
    @Override
    public void addHeader(String name, String value) {}
    @Override
    public void addParameter(String name, String values[]) {}
    @Override
    public void clearCookies() {}
    @Override
    public void clearHeaders() {}
    @Override
    public void clearLocales() {}
    @Override
    public void clearParameters() {}
    @Override
    public void replayPayload(byte[] payloadByteArray) {}
    @Override
    public void recycle() {}
    @Override
    public void setAuthType(String authType) {}
    /* START PWC 4707989
    public void setMethod(String method) {}
    */
    @Override
    public void setRequestedSessionCookie(boolean flag) {}
    @Override
    public void setRequestedSessionId(String id) {}
    @Override
    public void setRequestedSessionURL(boolean flag) {}
    @Override
    public void setRequestURI(String uri) {}
    @Override
    public void setSecure(boolean secure) {}
    @Override
    public void setUserPrincipal(Principal principal) {}
    @Override
    public String getParameter(String name) { return null; }
    @Override
    public Map<String, String[]> getParameterMap() { return null; }
    @Override
    public Enumeration<String> getParameterNames() { return dummyEnum; }
    @Override
    public String[] getParameterValues(String name) { return null; }
    @Override
    public RequestDispatcher getRequestDispatcher(String path) {
        return null;
    }
    @Override
    public String getAuthType() { return null; }
    @Override
    public Cookie[] getCookies() { return null; }
    @Override
    public long getDateHeader(String name) { return -1; }
    @Override
    public String getHeader(String name) { return null; }
    @Override
    public Enumeration<String> getHeaders(String name) { return null; }
    @Override
    public Enumeration<String> getHeaderNames() { return null; }
    @Override
    public int getIntHeader(String name) { return -1; }
    /* START PWC 4707989
    public String getMethod() { return null; }
    */
    @Override
    public String getPathTranslated() { return null; }
    @Override
    public String getRemoteUser() { return null; }
    @Override
    public String getRequestedSessionId() { return null; }
    @Override
    public String getRequestURI() { return null; }
    @Override
    public StringBuffer getRequestURL() { return null; }
    @Override
    public HttpSession getSession() { return null; }
    @Override
    public HttpSession getSession(boolean create) { return null; }
    @Override
    public Session getSessionInternal(boolean create) { return null; }
    @Override
    public String changeSessionId() { return null; }
    @Override
    public boolean isRequestedSessionIdFromCookie() { return false; }
    @Override
    public boolean isRequestedSessionIdFromURL() { return false; }
    @Override
    public boolean isRequestedSessionIdFromUrl() { return false; }
    @Override
    public boolean isRequestedSessionIdValid() { return false; }
    @Override
    public void setRequestedSessionCookiePath(String cookiePath) {}
    @Override
    public boolean isUserInRole(String role) { return false; }
    @Override
    public Principal getUserPrincipal() { return null; }
    @Override
    public String getLocalAddr() { return null; }    
    @Override
    public String getLocalName() { return null; }
    @Override
    public int getLocalPort() { return -1; }
    @Override
    public int getRemotePort() { return -1; }
    @Override
    public DispatcherType getDispatcherType() { return null; }
    @Override
    public AsyncContext startAsync() throws IllegalStateException { return null; }
    @Override
    public AsyncContext startAsync(ServletRequest servletRequest,
                                   ServletResponse servletResponse)
            throws IllegalStateException { return null; }
    @Override
    public boolean isAsyncStarted() { return false; }
    @Override
    public boolean isAsyncSupported() { return false; }
    public void setAsyncTimeout(long timeout) {}
    public long getAsyncTimeout() { return -1; }
    @Override
    public AsyncContext getAsyncContext() { return null; }
    public void addAsyncListener(AsyncListener listener) {};
    public void addAsyncListener(AsyncListener listener,
                                 ServletRequest servletRequest,
                                 ServletResponse servletResponse) {}
    public boolean isSetAsyncTimeoutCalled() { return false; }
    @Override
    public void disableAsyncSupport() {}
    @Override
    public Collection<Part> getParts() {return null;}
    @Override
    public Part getPart(String name) {return null;}
    @Override
    public boolean authenticate(HttpServletResponse response)
        throws IOException, ServletException { return false; }
    @Override
    public void login(String username, String password)
        throws ServletException {}
    @Override
    public void logout() throws ServletException {}
    @Override
    public <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass) { return null; }

    // START CR 6415120
    /**
     * Set whether or not access to resources under WEB-INF or META-INF
     * needs to be checked.
     */
    @Override
    public void setCheckRestrictedResources(boolean check) {
        checkRestrictedResources = check;
    }

    /**
     * Return whether or not access to resources under WEB-INF or META-INF
     * needs to be checked.
     */
    @Override
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
    @Override
    public String getJrouteId() {
        return null;
    }
    // END SJSAS 6346226
    
    /**
     * This object does not implement a session ID generator. Provide
     * a dummy implementation so that the default one will be used.
     */
    @Override
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
    @Override
    public ServletContext getServletContext() {
        return null;
    }

    @Override
    public Session lockSession() {
        return null;
    }

    @Override
    public void unlockSession() {}

}

