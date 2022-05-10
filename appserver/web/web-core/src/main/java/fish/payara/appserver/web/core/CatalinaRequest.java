/*
 *
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright (c) 2022 Payara Foundation and/or its affiliates. All rights reserved.
 *
 *  The contents of this file are subject to the terms of either the GNU
 *  General Public License Version 2 only ("GPL") or the Common Development
 *  and Distribution License("CDDL") (collectively, the "License").  You
 *  may not use this file except in compliance with the License.  You can
 *  obtain a copy of the License at
 *  https://github.com/payara/Payara/blob/master/LICENSE.txt
 *  See the License for the specific
 *  language governing permissions and limitations under the License.
 *
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at glassfish/legal/LICENSE.txt.
 *
 *  GPL Classpath Exception:
 *  The Payara Foundation designates this particular file as subject to the "Classpath"
 *  exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *  file that accompanied this code.
 *
 *  Modifications:
 *  If applicable, add the following below the License Header, with the fields
 *  enclosed by brackets [] replaced by your own identifying information:
 *  "Portions Copyright [year] [name of copyright owner]"
 *
 *  Contributor(s):
 *  If you wish your version of this file to be governed by only the CDDL or
 *  only the GPL Version 2, indicate your decision by adding "[Contributor]
 *  elects to include this software in this distribution under the [CDDL or GPL
 *  Version 2] license."  If you don't indicate a single choice of license, a
 *  recipient has the option to distribute your version of this file under
 *  either the CDDL, the GPL Version 2 or to extend the choice of license to
 *  its licensees as provided above.  However, if you add GPL Version 2 code
 *  and therefore, elected the GPL Version 2 license, then the option applies
 *  only if the new code is made subject to such option by the copyright
 *  holder.
 *
 */

package fish.payara.appserver.web.core;

import java.io.BufferedReader;
import java.io.CharConversionException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletMapping;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpUpgradeHandler;
import jakarta.servlet.http.Part;
import jakarta.servlet.http.PushBuilder;
import org.apache.catalina.Context;
import org.apache.catalina.Host;
import org.apache.catalina.Session;
import org.apache.catalina.Wrapper;
import org.apache.catalina.connector.Response;
import org.apache.catalina.core.AsyncContextImpl;
import org.apache.catalina.mapper.MappingData;
import org.apache.tomcat.util.buf.B2CConverter;
import org.apache.tomcat.util.buf.MessageBytes;
import org.apache.tomcat.util.http.ServerCookies;
import org.glassfish.grizzly.http.server.Request;

public class CatalinaRequest extends org.apache.catalina.connector.Request {

    private Request grizzlyRequest;

    /**
     * Create a new Request object associated with the given Connector.
     *
     * @param connector The Connector with which this Request object will always
     *                  be associated. In normal usage this must be non-null. In
     *                  some test scenarios, it may be possible to use a null
     *                  Connector without triggering an NPE.
     */
    public CatalinaRequest(GrizzlyConnector connector) {
        super(connector);
    }

    public Request getGrizzlyRequest() {
        return grizzlyRequest;
    }

    @Override
    public void setCoyoteRequest(org.apache.coyote.Request coyoteRequest) {
        throw new UnsupportedOperationException("We don't like coyote requests around here");
    }
    @Override
    public org.apache.coyote.Request getCoyoteRequest() {
        // TODO: carefully copy all relevant data
        throw new UnsupportedOperationException("We don't like coyote requests around here");
        //return super.getCoyoteRequest();
    }

    @Override
    protected void addPathParameter(String name, String value) {
        throw new UnsupportedOperationException("Path parameters not supported in Grizzly");
    }

    @Override
    protected String getPathParameter(String name) {
        throw new UnsupportedOperationException("Path parameters not supported in Grizzly");
    }

    @Override
    public void setAsyncSupported(boolean asyncSupported) {
        super.setAsyncSupported(asyncSupported);
    }

    @Override
    public void recycle() {
        super.recycle();
        coyoteRequest.recycle();
        // grizzly is recycled in its own lifecycle
    }

    @Override
    protected void recycleSessionInfo() {
        super.recycleSessionInfo();
    }

    @Override
    protected void recycleCookieInfo(boolean recycleCoyote) {
        super.recycleCookieInfo(recycleCoyote);
    }

    @Override
    public GrizzlyConnector getConnector() {
        return (GrizzlyConnector) super.getConnector();
    }

    @Override
    public Context getContext() {
        return super.getContext();
    }

    @Override
    public boolean getDiscardFacades() {
        return super.getDiscardFacades();
    }

    @Override
    public FilterChain getFilterChain() {
        return super.getFilterChain();
    }

    @Override
    public void setFilterChain(FilterChain filterChain) {
        super.setFilterChain(filterChain);
    }

    @Override
    public Host getHost() {
        return super.getHost();
    }

    @Override
    public MappingData getMappingData() {
        return super.getMappingData();
    }

    @Override
    public HttpServletRequest getRequest() {
        return super.getRequest();
    }

    @Override
    public void setRequest(HttpServletRequest applicationRequest) {
        super.setRequest(applicationRequest);
    }

    @Override
    public CatalinaResponse getResponse() {
        return (CatalinaResponse) super.getResponse();
    }

    @Override
    public void setResponse(Response response) {
        if (!(response instanceof CatalinaResponse)) {
            throw new IllegalArgumentException("Only GrizzlyResponse is supported");
        }
        super.setResponse(response);
    }

    @Override
    public InputStream getStream() {
        // this should be wrapper around inputbuffer, but grizzly already has one
        return grizzlyRequest.getInputStream();
    }

    @Override
    protected B2CConverter getURIConverter() {
        return super.getURIConverter();
    }

    @Override
    protected void setURIConverter(B2CConverter URIConverter) {
        super.setURIConverter(URIConverter);
    }

    @Override
    public Wrapper getWrapper() {
        return super.getWrapper();
    }

    @Override
    public ServletInputStream createInputStream() throws IOException {
        // TODO: here we need to bridge something
        return super.createInputStream();
    }

    @Override
    public void finishRequest() throws IOException {
        super.finishRequest();
    }

    @Override
    public Object getNote(String name) {
        // notes are owned by this object, though network stack has their own as well.
        return super.getNote(name);
    }

    @Override
    public void removeNote(String name) {
        super.removeNote(name);
    }

    @Override
    public void setLocalPort(int port) {
        super.setLocalPort(port);
    }

    @Override
    public void setNote(String name, Object value) {
        super.setNote(name, value);
    }

    @Override
    public void setRemoteAddr(String remoteAddr) {
        super.setRemoteAddr(remoteAddr);
    }

    @Override
    public void setRemoteHost(String remoteHost) {
        super.setRemoteHost(remoteHost);
    }

    @Override
    public void setSecure(boolean secure) {
        super.setSecure(secure);
    }

    @Override
    public void setServerPort(int port) {
        super.setServerPort(port);
    }

    @Override
    public Object getAttribute(String name) {
        return grizzlyRequest.getAttribute(name);
    }

    @Override
    public long getContentLengthLong() {
        return grizzlyRequest.getContentLengthLong();
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return Collections.enumeration(grizzlyRequest.getAttributeNames());
    }

    @Override
    public String getCharacterEncoding() {
        return grizzlyRequest.getCharacterEncoding();
    }

    @Override
    public int getContentLength() {
        return grizzlyRequest.getContentLength();
    }

    @Override
    public String getContentType() {
        return grizzlyRequest.getContentType();
    }

    @Override
    public void setContentType(String contentType) {
        grizzlyRequest.getRequest().setContentType(contentType);
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        // TODO: add input bufferring
        return super.getInputStream();
    }

    @Override
    public Locale getLocale() {
        return grizzlyRequest.getLocale();
    }

    @Override
    public Enumeration<Locale> getLocales() {
        return Collections.enumeration(grizzlyRequest.getLocales());
    }

    @Override
    public String getParameter(String name) {
        return grizzlyRequest.getParameter(name);
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        return grizzlyRequest.getParameterMap();
    }

    @Override
    public Enumeration<String> getParameterNames() {
        return Collections.enumeration(grizzlyRequest.getParameterNames());
    }

    @Override
    public String[] getParameterValues(String name) {
        return grizzlyRequest.getParameterValues(name);
    }

    @Override
    public String getProtocol() {
        return grizzlyRequest.getProtocol().getProtocolString();
    }

    @Override
    public BufferedReader getReader() throws IOException {
        // TODO: Actual buffering
        return new BufferedReader(grizzlyRequest.getReader());
    }

    @Override
    public String getRealPath(String path) {
        return super.getRealPath(path);
    }

    @Override
    public String getRemoteAddr() {
        return grizzlyRequest.getRemoteAddr();
    }

    @Override
    public String getPeerAddr() {
        return grizzlyRequest.getRemoteAddr();
    }

    @Override
    public String getRemoteHost() {
        return grizzlyRequest.getRemoteHost();
    }

    @Override
    public int getRemotePort() {
        return grizzlyRequest.getRemotePort();
    }

    @Override
    public String getLocalName() {
        return grizzlyRequest.getLocalName();
    }

    @Override
    public String getLocalAddr() {
        return grizzlyRequest.getLocalAddr();
    }

    @Override
    public int getLocalPort() {
        return grizzlyRequest.getLocalPort();
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String path) {
        return super.getRequestDispatcher(path);
    }

    @Override
    public String getScheme() {
        return grizzlyRequest.getScheme();
    }

    @Override
    public String getServerName() {
        return grizzlyRequest.getServerName();
    }

    @Override
    public int getServerPort() {
        return grizzlyRequest.getServerPort();
    }

    @Override
    public boolean isSecure() {
        return grizzlyRequest.isSecure();
    }

    @Override
    public void removeAttribute(String name) {
        grizzlyRequest.removeAttribute(name);
    }

    @Override
    public void setAttribute(String name, Object value) {
        grizzlyRequest.setAttribute(name, value);
    }

    @Override
    public void setCharacterEncoding(String enc) throws UnsupportedEncodingException {
        grizzlyRequest.setCharacterEncoding(enc);
    }

    @Override
    public ServletContext getServletContext() {
        return super.getServletContext();
    }

    @Override
    public AsyncContext startAsync() {
        return super.startAsync();
    }

    @Override
    public AsyncContext startAsync(ServletRequest request, ServletResponse response) {
        // TODO: Heavy dependency on coyote here
        // Reimplementation requires overriding all async methods as asyncContext is private
        return super.startAsync(request, response);
    }

    @Override
    public boolean isAsyncStarted() {
        return super.isAsyncStarted();
    }

    @Override
    public boolean isAsyncDispatching() {
        return super.isAsyncDispatching();
    }

    @Override
    public boolean isAsyncCompleting() {
        return super.isAsyncCompleting();
    }

    @Override
    public boolean isAsync() {
        return super.isAsync();
    }

    @Override
    public boolean isAsyncSupported() {
        return super.isAsyncSupported();
    }

    @Override
    public AsyncContext getAsyncContext() {
        return super.getAsyncContext();
    }

    @Override
    public AsyncContextImpl getAsyncContextInternal() {
        return super.getAsyncContextInternal();
    }

    @Override
    public DispatcherType getDispatcherType() {
        return super.getDispatcherType();
    }

    @Override
    public void addCookie(Cookie cookie) {
        // 
        var grizzlyCookie = new org.glassfish.grizzly.http.Cookie(cookie.getName(), cookie.getValue());
        grizzlyRequest.addCookie(grizzlyCookie);
    }

    @Override
    public void addLocale(Locale locale) {
        grizzlyRequest.addLocale(locale);
    }

    @Override
    public void clearCookies() {
        grizzlyRequest.clearCookies();
    }

    @Override
    public void clearLocales() {
        grizzlyRequest.clearLocales();
    }

    @Override
    public void setAuthType(String type) {
        grizzlyRequest.getRequest().authType().setString(type);
    }

    @Override
    public void setPathInfo(String path) {
        super.setPathInfo(path);
    }

    @Override
    public void setRequestedSessionCookie(boolean flag) {
        grizzlyRequest.setRequestedSessionCookie(flag);
    }

    @Override
    public void setRequestedSessionId(String id) {
        grizzlyRequest.setRequestedSessionId(id);
    }

    @Override
    public void setRequestedSessionURL(boolean flag) {
        grizzlyRequest.setRequestedSessionURL(flag);
    }

    @Override
    public void setRequestedSessionSSL(boolean flag) {
        grizzlyRequest.setRequestedSessionURL(flag);
    }

    @Override
    public String getDecodedRequestURI() {
        try {
            return grizzlyRequest.getDecodedRequestURI();
        } catch (CharConversionException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public MessageBytes getDecodedRequestURIMB() {
        if (coyoteRequest.decodedURI().isNull()) {
            coyoteRequest.decodedURI().setString(getDecodedRequestURI());
        }
        return coyoteRequest.decodedURI();
    }

    @Override
    public void setUserPrincipal(Principal principal) {
        grizzlyRequest.setUserPrincipal(principal);
    }

    @Override
    public boolean isTrailerFieldsReady() {
        return grizzlyRequest.areTrailersAvailable();
    }

    @Override
    public Map<String, String> getTrailerFields() {
        return grizzlyRequest.getTrailers();
    }

    @Override
    public PushBuilder newPushBuilder() {
        return super.newPushBuilder();
    }

    @Override
    public PushBuilder newPushBuilder(HttpServletRequest request) {
        return super.newPushBuilder(request);
    }

    @Override
    public <T extends HttpUpgradeHandler> T upgrade(Class<T> httpUpgradeHandlerClass) throws IOException, ServletException {
        return super.upgrade(httpUpgradeHandlerClass);
    }

    @Override
    public String getAuthType() {
        return grizzlyRequest.getAuthType();
    }

    @Override
    public String getContextPath() {
        return super.getContextPath();
    }

    @Override
    public Cookie[] getCookies() {
        // TODO
        return super.getCookies();
    }

    @Override
    public ServerCookies getServerCookies() {
        // TODO
        return super.getServerCookies();
    }

    @Override
    public long getDateHeader(String name) {
        return grizzlyRequest.getDateHeader(name);
    }
    

    @Override
    public String getHeader(String name) {
        return grizzlyRequest.getHeader(name);
    }

    static class IteratorWrapper<T> implements Enumeration<T> {
        final Iterator<T> iter;
        IteratorWrapper(Iterator<T> iter) {
            this.iter = iter;
        }
        @Override
        public boolean hasMoreElements() {
            return iter.hasNext();
        }

        @Override
        public T nextElement() {
            return iter.next();
        }

        @Override
        public Iterator<T> asIterator() {
            return iter;
        }
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        return new IteratorWrapper<>(grizzlyRequest.getHeaders(name).iterator());
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        return new IteratorWrapper<>(grizzlyRequest.getHeaderNames().iterator());
    }

    @Override
    public int getIntHeader(String name) {
        return grizzlyRequest.getIntHeader(name);
    }

    @Override
    public HttpServletMapping getHttpServletMapping() {
        return super.getHttpServletMapping();
    }

    @Override
    public String getMethod() {
        return grizzlyRequest.getMethod().getMethodString();
    }

    @Override
    public String getPathInfo() {
        return super.getPathInfo();
    }

    @Override
    public String getPathTranslated() {
        return super.getPathTranslated();
    }

    @Override
    public String getQueryString() {
        return grizzlyRequest.getQueryString();
    }

    @Override
    public String getRemoteUser() {
        return grizzlyRequest.getRemoteUser();
    }

    @Override
    public MessageBytes getRequestPathMB() {
        return super.getRequestPathMB();
    }

    @Override
    public String getRequestedSessionId() {
        return grizzlyRequest.getRequestedSessionId();
    }

    @Override
    public String getRequestURI() {
        return grizzlyRequest.getRequestURI();
    }

    @Override
    public StringBuffer getRequestURL() {
        return new StringBuffer(getRequestURI());
    }

    @Override
    public String getServletPath() {
        return super.getServletPath();
    }

    @Override
    public HttpSession getSession() {
        return super.getSession();
    }

    @Override
    public HttpSession getSession(boolean create) {
        return super.getSession(create);
    }

    @Override
    public boolean isRequestedSessionIdFromCookie() {
        return grizzlyRequest.isRequestedSessionIdFromCookie();
    }

    @Override
    public boolean isRequestedSessionIdFromURL() {
        return grizzlyRequest.isRequestedSessionIdFromURL();
    }

    @Override
    public boolean isRequestedSessionIdFromUrl() {
        return super.isRequestedSessionIdFromUrl();
    }

    @Override
    public boolean isRequestedSessionIdValid() {
        return grizzlyRequest.isRequestedSessionIdValid();
    }

    @Override
    public boolean isUserInRole(String role) {
        return super.isUserInRole(role);
    }

    @Override
    public Principal getPrincipal() {
        return grizzlyRequest.getUserPrincipal();
    }

    @Override
    public Principal getUserPrincipal() {
        return grizzlyRequest.getUserPrincipal();
    }

    @Override
    public Session getSessionInternal() {
        return super.getSessionInternal();
    }

    @Override
    public void changeSessionId(String newSessionId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String changeSessionId() {
        return grizzlyRequest.changeSessionId();
    }

    @Override
    public Session getSessionInternal(boolean create) {
        return super.getSessionInternal(create);
    }

    @Override
    public boolean isParametersParsed() {
        return super.isParametersParsed();
    }

    @Override
    public boolean isFinished() {
        return grizzlyRequest.getInputBuffer().isFinished();
    }

    @Override
    protected void checkSwallowInput() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean authenticate(HttpServletResponse response) throws IOException, ServletException {
        return super.authenticate(response);
    }

    @Override
    public void login(String username, String password) throws ServletException {
        super.login(username, password);
    }

    @Override
    public void logout() throws ServletException {
        super.logout();
    }

    @Override
    public Collection<Part> getParts() throws IOException, IllegalStateException, ServletException {
        return super.getParts();
    }

    @Override
    public Part getPart(String name) throws IOException, IllegalStateException, ServletException {
        return super.getPart(name);
    }

    @Override
    protected Session doGetSession(boolean create) {
        return super.doGetSession(create);
    }

    @Override
    protected String unescape(String s) {
        return super.unescape(s);
    }

    @Override
    protected void parseCookies() {
        super.parseCookies();
    }

    @Override
    protected void convertCookies() {
        super.convertCookies();
    }

    @Override
    protected void parseParameters() {
        super.parseParameters();
    }

    @Override
    protected int readPostBody(byte[] body, int len) throws IOException {
        return super.readPostBody(body, len);
    }

    @Override
    protected byte[] readChunkedPostBody() throws IOException {
        return super.readChunkedPostBody();
    }

    @Override
    protected void parseLocales() {
        super.parseLocales();
    }

    @Override
    protected void parseLocalesHeader(String value, TreeMap<Double, ArrayList<Locale>> locales) {
        super.parseLocalesHeader(value, locales);
    }

    void setRequests(org.apache.coyote.Request coyoteRequest, Request grizzlyRequest) {
        super.setCoyoteRequest(coyoteRequest);
        this.grizzlyRequest = grizzlyRequest;
    }
}
