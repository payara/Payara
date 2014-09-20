/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved.
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
package com.sun.web.security;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import javax.servlet.*;
import javax.servlet.http.*;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.RequestFacade;


class RequestFacadeWrapper extends RequestFacade implements HttpServletRequest {

    private final HttpServletRequest servletRequest;

    RequestFacadeWrapper(Request request,
            HttpServletRequest servletRequest,boolean mask) {
        super(request,mask);
        this.servletRequest = servletRequest;
    }
    
    /**
     * methods defined by HttpServletRequest
     */
    @Override
    public String getAuthType() {
        return servletRequest.getAuthType();
    }

    @Override
    public Cookie[] getCookies() {
        return servletRequest.getCookies();
    }

    @Override
    public long getDateHeader(String name) {
        return servletRequest.getDateHeader(name);
    }

    @Override
    public String getHeader(String name) {
         return servletRequest.getHeader(name);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        return servletRequest.getHeaders(name);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        return servletRequest.getHeaderNames();
    }

    @Override
    public int getIntHeader(String name) {
        return servletRequest.getIntHeader(name);
    }

    @Override
    public String getMethod() {
        return servletRequest.getMethod();
    }

    @Override
    public String getPathInfo() {
        return servletRequest.getPathInfo();
    }

    @Override
    public String getPathTranslated() {
        return servletRequest.getPathTranslated();
    }

    @Override
    public String getContextPath() {
        return servletRequest.getContextPath();
    }

    @Override
    public String getQueryString() {
        return servletRequest.getQueryString();
    }

    @Override
    public String getRemoteUser() {
        return servletRequest.getRemoteUser();
    }

    @Override
    public boolean isUserInRole(String role) {
        return servletRequest.isUserInRole(role);
    }

    @Override
    public Principal getUserPrincipal() {
        return servletRequest.getUserPrincipal();
    }

    @Override
    public String getRequestedSessionId() {
        return servletRequest.getRequestedSessionId();
    }

    @Override
    public String getRequestURI() {
        return servletRequest.getRequestURI();
    }

    @Override
    public StringBuffer getRequestURL() {
        return servletRequest.getRequestURL();
    }

    @Override
    public String getServletPath() {
        return servletRequest.getServletPath();
    }

    @Override
    public HttpSession getSession(boolean create) {
        return servletRequest.getSession(create);
    }

    @Override
    public HttpSession getSession() {
        return servletRequest.getSession();
    }

    @Override
    public boolean isRequestedSessionIdValid() {
        return servletRequest.isRequestedSessionIdValid();
    }

    @Override
    public boolean isRequestedSessionIdFromCookie() {
        return servletRequest.isRequestedSessionIdFromCookie();
    }

    @Override
    public boolean isRequestedSessionIdFromURL() {
        return servletRequest.isRequestedSessionIdFromURL();
    }

    @Override
    public boolean isRequestedSessionIdFromUrl() {
        return servletRequest.isRequestedSessionIdFromUrl();
    }

    @Override
    public boolean authenticate(HttpServletResponse response) throws IOException, ServletException {
        return servletRequest.authenticate(response);
    }

    @Override
    public void login(String username, String password) throws ServletException {
        servletRequest.login(username, password);
    }

    @Override
    public void logout() throws ServletException {
        servletRequest.logout();
    }

    @Override
    public Collection<Part> getParts() throws IOException, ServletException {
        return servletRequest.getParts();
    }

    @Override
    public Part getPart(String name) throws IOException, ServletException {
        return servletRequest.getPart(name);
    }

    /**
     * Methods inherited from ServletRequest
     */
    
    @Override
    public Object getAttribute(String name) {
        return servletRequest.getAttribute(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return servletRequest.getAttributeNames();
    }

    @Override
    public String getCharacterEncoding() {
        return servletRequest.getCharacterEncoding();
    }

    @Override
    public void setCharacterEncoding(String env) throws UnsupportedEncodingException {
        servletRequest.setCharacterEncoding(env);
    }

    @Override
    public int getContentLength() {
        return servletRequest.getContentLength();
    }

    @Override
    public String getContentType() {
        return servletRequest.getContentType();
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        return servletRequest.getInputStream();
    }

    @Override
    public String getParameter(String name) {
        return servletRequest.getParameter(name);
    }

    @Override
    public Enumeration<String> getParameterNames() {
        return servletRequest.getParameterNames();
    }

    @Override
    public String[] getParameterValues(String name) {
        return servletRequest.getParameterValues(name);
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        return servletRequest.getParameterMap();
    }

    @Override
    public String getProtocol() {
        return servletRequest.getProtocol();
    }

    @Override
    public String getScheme() {
        return servletRequest.getScheme();
    }

    @Override
    public String getServerName() {
        return servletRequest.getServerName();
    }

    @Override
    public int getServerPort() {
        return servletRequest.getServerPort();
    }

    @Override
    public BufferedReader getReader() throws IOException {
        return servletRequest.getReader();
    }

    @Override
    public String getRemoteAddr() {
        return servletRequest.getRemoteAddr();
    }

    @Override
    public String getRemoteHost() {
        return servletRequest.getRemoteHost();
    }

    @Override
    public void setAttribute(String name, Object o) {
        servletRequest.setAttribute(name, o);
    }

    @Override
    public void removeAttribute(String name) {
        servletRequest.removeAttribute(name);
    }

    @Override
    public Locale getLocale() {
        return servletRequest.getLocale();
    }

    @Override
    public Enumeration<Locale> getLocales() {
        return servletRequest.getLocales();
    }

    @Override
    public boolean isSecure() {
        return servletRequest.isSecure();
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String path) {
        return servletRequest.getRequestDispatcher(path);
    }

    @Override
    public String getRealPath(String path) {
        return servletRequest.getRealPath(path);
    }

    @Override
    public int getRemotePort() {
        return servletRequest.getRemotePort();
    }

    @Override
    public String getLocalName() {
        return servletRequest.getLocalName();
    }

    @Override
    public String getLocalAddr() {
        return servletRequest.getLocalAddr();
    }

    @Override
    public int getLocalPort() {
        return servletRequest.getLocalPort();
    }

    @Override
    public ServletContext getServletContext() {
        return servletRequest.getServletContext();
    }

    @Override
    public AsyncContext startAsync() throws IllegalStateException {
        return servletRequest.startAsync();
    }

    @Override
    public AsyncContext startAsync(ServletRequest sRequest, 
        ServletResponse sResponse) throws IllegalStateException {
        return servletRequest.startAsync(sRequest, sResponse);
    }

    @Override
    public boolean isAsyncStarted() {
        return servletRequest.isAsyncStarted();

    }

    @Override
    public boolean isAsyncSupported() {
        return servletRequest.isAsyncSupported();
    }

    @Override
    public AsyncContext getAsyncContext() {
        return servletRequest.getAsyncContext();
    }

    @Override
    public DispatcherType getDispatcherType() {
        return servletRequest.getDispatcherType();
    }

}