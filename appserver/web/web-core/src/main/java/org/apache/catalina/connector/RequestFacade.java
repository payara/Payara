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

package org.apache.catalina.connector;

import org.apache.catalina.Globals;
import org.apache.catalina.core.RequestFacadeHelper;
import org.apache.catalina.core.StandardServer;
import org.apache.catalina.security.SecurityUtil;
import org.glassfish.logging.annotation.LogMessageInfo;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.http.HttpUpgradeHandler;
import java.io.BufferedReader;
import java.io.IOException;
import java.security.AccessControlException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.SecurityPermission;
import java.util.*;


/**
 * Facade class that wraps a Coyote request object.  
 * All methods are delegated to the wrapped request.
 *
 * @author Craig R. McClanahan
 * @author Remy Maucherat
 * @author Jean-Francois Arcand
 * @version $Revision: 1.7 $ $Date: 2007/08/01 19:04:28 $
 */
public class RequestFacade 
    implements HttpServletRequest {

    private static final ResourceBundle rb = StandardServer.log.getResourceBundle();

    @LogMessageInfo(
            message = " Must not use request object outside the scope of a servlet's service or a filter's doFilter method",
            level = "WARNING"
    )
    public static final String CANNOT_USE_REQUEST_OBJECT_OUTSIDE_SCOPE_EXCEPTION = "AS-WEB-CORE-00074";
        
        
    // ----------------------------------------------------------- DoPrivileged
    
    private final class GetAttributePrivilegedAction
            implements PrivilegedAction<Enumeration<String>> {
        
        public Enumeration<String> run() {
            return request.getAttributeNames();
        }            
    }
     
    
    private final class GetParameterMapPrivilegedAction
            implements PrivilegedAction<Map<String, String[]>> {
        
        public Map<String, String[]> run() {
            return request.getParameterMap();
        }        
    }    
    
    
    private final class GetRequestDispatcherPrivilegedAction
            implements PrivilegedAction<RequestDispatcher> {

        private String path;

        public GetRequestDispatcherPrivilegedAction(String path){
            this.path = path;
        }
        
        public RequestDispatcher run() {   
            return request.getRequestDispatcher(path);
        }           
    }    
    
    
    private final class GetParameterPrivilegedAction
            implements PrivilegedAction<String> {

        public String name;

        public GetParameterPrivilegedAction(String name){
            this.name = name;
        }

        public String run() {       
            return request.getParameter(name);
        }           
    }    
    
     
    private final class GetParameterNamesPrivilegedAction
            implements PrivilegedAction<Enumeration<String>> {
        
        public Enumeration<String> run() {          
            return request.getParameterNames();
        }           
    } 
    
    
    private final class GetParameterValuePrivilegedAction
            implements PrivilegedAction<String[]> {

        public String name;

        public GetParameterValuePrivilegedAction(String name){
            this.name = name;
        }

        public String[] run() {       
            return request.getParameterValues(name);
        }           
    }    
  
    
    private final class GetCookiesPrivilegedAction
            implements PrivilegedAction<Cookie[]> {
        
        public Cookie[] run() {       
            return request.getCookies();
        }           
    }      
    
    
    private final class GetCharacterEncodingPrivilegedAction
            implements PrivilegedAction<String> {
        
        public String run() {       
            return request.getCharacterEncoding();
        }           
    }   
        
    
    private final class GetHeadersPrivilegedAction
            implements PrivilegedAction<Enumeration<String>> {

        private String name;

        public GetHeadersPrivilegedAction(String name){
            this.name = name;
        }
        
        public Enumeration<String> run() {       
            return request.getHeaders(name);
        }           
    }    
        
    
    private final class GetHeaderNamesPrivilegedAction
            implements PrivilegedAction<Enumeration<String>> {

        public Enumeration<String> run() {       
            return request.getHeaderNames();
        }           
    }  
            
    
    private final class GetLocalePrivilegedAction
            implements PrivilegedAction<Locale> {

        public Locale run() {       
            return request.getLocale();
        }           
    }    
            
    
    private final class GetLocalesPrivilegedAction
            implements PrivilegedAction<Enumeration<Locale>> {

        public Enumeration<Locale> run() {       
            return request.getLocales();
        }           
    }    
    
    private final class GetSessionPrivilegedAction
            implements PrivilegedAction<HttpSession> {

        private boolean create;
        
        public GetSessionPrivilegedAction(boolean create){
            this.create = create;
        }
                
        public HttpSession run() {  
            return request.getSession(create);
        }           
    }

    private final class ChangeSessionIdPrivilegedAction
            implements PrivilegedAction<String> {

        public String run() {
            return request.changeSessionId();
        }
    }


    // ----------------------------------------------------------- Constructors


    /**
     * Construct a wrapper for the specified request.
     *
     * @param request The request to be wrapped
     */
    public RequestFacade(Request request) {
        this(request, false);
    }


    /**
     * Construct a wrapper for the specified request.
     *
     * @param request The request to be wrapped
     * @param maskDefaultContextMapping true if the fact that a request
     * received at the root context was mapped to a default-web-module will
     * be masked, false otherwise
     */
    public RequestFacade(Request request,
                         boolean maskDefaultContextMapping) {
        this.request = request;
        this.maskDefaultContextMapping = maskDefaultContextMapping;
        this.reqFacHelper = new RequestFacadeHelper(request);
    }


    // ----------------------------------------------- Class/Instance Variables


    private static final SecurityPermission
        GET_UNWRAPPED_COYOTE_REQUEST_PERMISSION =
            new SecurityPermission("getUnwrappedCoyoteRequest");




    /**
     * The wrapped request.
     */
    protected Request request = null;


    /*
     * True if the fact that a request received at the root context was
     * mapped to a default-web-module will be masked, false otherwise.
     *
     * For example, if set to true, this request facade's getContextPath()
     * method will return "/", rather than the context root of the
     * default-web-module, for requests received at the root context that
     * were mapped to a default-web-module.
     */
    private boolean maskDefaultContextMapping = false;

    private RequestFacadeHelper reqFacHelper = null;
 

    // --------------------------------------------------------- Public Methods
   
    
    /**
    * Prevent cloning the facade.
    */
    protected Object clone()
        throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }
    
    
    /**
     * Clear facade.
     */
    public void clear() {
        request = null;
        if (reqFacHelper != null) {
            reqFacHelper.clear();
        }
        reqFacHelper = null;
    }


    RequestFacadeHelper getRequestFacadeHelper() {
        return reqFacHelper;
    }
    

    // ------------------------------------------------- ServletRequest Methods


    public Object getAttribute(String name) {

        if (request == null) {
            throw new IllegalStateException(rb.getString(CANNOT_USE_REQUEST_OBJECT_OUTSIDE_SCOPE_EXCEPTION));
        }

        return request.getAttribute(name);
    }


    public Enumeration<String> getAttributeNames() {

        if (request == null) {
            throw new IllegalStateException(rb.getString(CANNOT_USE_REQUEST_OBJECT_OUTSIDE_SCOPE_EXCEPTION));
        }

        if (SecurityUtil.isPackageProtectionEnabled()){
            return AccessController.doPrivileged(
                new GetAttributePrivilegedAction());        
        } else {
            return request.getAttributeNames();
        }
    }


    public String getCharacterEncoding() {

        if (request == null) {
            throw new IllegalStateException(rb.getString(CANNOT_USE_REQUEST_OBJECT_OUTSIDE_SCOPE_EXCEPTION));
        }

        if (SecurityUtil.isPackageProtectionEnabled()){
            return AccessController.doPrivileged(
                new GetCharacterEncodingPrivilegedAction());
        } else {
            return request.getCharacterEncoding();
        }         
    }


    public void setCharacterEncoding(String env)
            throws java.io.UnsupportedEncodingException {

        if (request == null) {
            throw new IllegalStateException(rb.getString(CANNOT_USE_REQUEST_OBJECT_OUTSIDE_SCOPE_EXCEPTION));
        }

        request.setCharacterEncoding(env);
    }


    public int getContentLength() {

        if (request == null) {
            throw new IllegalStateException(rb.getString(CANNOT_USE_REQUEST_OBJECT_OUTSIDE_SCOPE_EXCEPTION));
        }

        return request.getContentLength();
    }


    public long getContentLengthLong() {

        if (request == null) {
            throw new IllegalStateException(rb.getString(CANNOT_USE_REQUEST_OBJECT_OUTSIDE_SCOPE_EXCEPTION));
        }

        return request.getContentLengthLong();
    }


    public String getContentType() {

        if (request == null) {
            throw new IllegalStateException(rb.getString(CANNOT_USE_REQUEST_OBJECT_OUTSIDE_SCOPE_EXCEPTION));
        }

        return request.getContentType();
    }


    public ServletInputStream getInputStream() throws IOException {

        if (request == null) {
            throw new IllegalStateException(rb.getString(CANNOT_USE_REQUEST_OBJECT_OUTSIDE_SCOPE_EXCEPTION));
        }

        return request.getInputStream();
    }


    public String getParameter(String name) {

        if (request == null) {
            throw new IllegalStateException(rb.getString(CANNOT_USE_REQUEST_OBJECT_OUTSIDE_SCOPE_EXCEPTION));
        }

        if (SecurityUtil.isPackageProtectionEnabled()){
            return AccessController.doPrivileged(
                new GetParameterPrivilegedAction(name));
        } else {
            return request.getParameter(name);
        }
    }


    public Enumeration<String> getParameterNames() {

        if (request == null) {
            throw new IllegalStateException(rb.getString(CANNOT_USE_REQUEST_OBJECT_OUTSIDE_SCOPE_EXCEPTION));
        }

        if (SecurityUtil.isPackageProtectionEnabled()){
            return AccessController.doPrivileged(
                new GetParameterNamesPrivilegedAction());
        } else {
            return request.getParameterNames();
        }
    }


    public String[] getParameterValues(String name) {

        if (request == null) {
            throw new IllegalStateException(rb.getString(CANNOT_USE_REQUEST_OBJECT_OUTSIDE_SCOPE_EXCEPTION));
        }

        String[] ret = null;

        /*
         * Clone the returned array only if there is a security manager
         * in place, so that performance won't suffer in the non-secure case
         */
        if (SecurityUtil.isPackageProtectionEnabled()){
            ret = AccessController.doPrivileged(
                new GetParameterValuePrivilegedAction(name));
            if (ret != null) {
                ret = (String[]) ret.clone();
            }
        } else {
            ret = request.getParameterValues(name);
        }

        return ret;
    }


    public Map<String, String[]> getParameterMap() {

        if (request == null) {
            throw new IllegalStateException(rb.getString(CANNOT_USE_REQUEST_OBJECT_OUTSIDE_SCOPE_EXCEPTION));
        }

        if (SecurityUtil.isPackageProtectionEnabled()){
            return AccessController.doPrivileged(
                new GetParameterMapPrivilegedAction());        
        } else {
            return request.getParameterMap();
        }
    }


    public String getProtocol() {

        if (request == null) {
            throw new IllegalStateException(rb.getString(CANNOT_USE_REQUEST_OBJECT_OUTSIDE_SCOPE_EXCEPTION));
        }

        return request.getProtocol();
    }


    public String getScheme() {

        if (request == null) {
            throw new IllegalStateException(rb.getString(CANNOT_USE_REQUEST_OBJECT_OUTSIDE_SCOPE_EXCEPTION));
        }

        return request.getScheme();
    }


    public String getServerName() {

        if (request == null) {
            throw new IllegalStateException(rb.getString(CANNOT_USE_REQUEST_OBJECT_OUTSIDE_SCOPE_EXCEPTION));
        }

        return request.getServerName();
    }


    public int getServerPort() {

        if (request == null) {
            throw new IllegalStateException(rb.getString(CANNOT_USE_REQUEST_OBJECT_OUTSIDE_SCOPE_EXCEPTION));
        }

        return request.getServerPort();
    }


    public BufferedReader getReader() throws IOException {

        if (request == null) {
            throw new IllegalStateException(rb.getString(CANNOT_USE_REQUEST_OBJECT_OUTSIDE_SCOPE_EXCEPTION));
        }

        return request.getReader();
    }


    public String getRemoteAddr() {

        if (request == null) {
            throw new IllegalStateException(rb.getString(CANNOT_USE_REQUEST_OBJECT_OUTSIDE_SCOPE_EXCEPTION));
        }

        return request.getRemoteAddr();
    }


    public String getRemoteHost() {

        if (request == null) {
            throw new IllegalStateException(rb.getString(CANNOT_USE_REQUEST_OBJECT_OUTSIDE_SCOPE_EXCEPTION));
        }

        return request.getRemoteHost();
    }


    public void setAttribute(String name, Object o) {

        if (request == null) {
            throw new IllegalStateException(rb.getString(CANNOT_USE_REQUEST_OBJECT_OUTSIDE_SCOPE_EXCEPTION));
        }

        request.setAttribute(name, o);
    }


    public void removeAttribute(String name) {

        if (request == null) {
            throw new IllegalStateException(rb.getString(CANNOT_USE_REQUEST_OBJECT_OUTSIDE_SCOPE_EXCEPTION));
        }

        request.removeAttribute(name);
    }


    public Locale getLocale() {

        if (request == null) {
            throw new IllegalStateException(rb.getString(CANNOT_USE_REQUEST_OBJECT_OUTSIDE_SCOPE_EXCEPTION));
        }

        if (SecurityUtil.isPackageProtectionEnabled()){
            return AccessController.doPrivileged(
                new GetLocalePrivilegedAction());
        } else {
            return request.getLocale();
        }        
    }


    public Enumeration<Locale> getLocales() {

        if (request == null) {
            throw new IllegalStateException(rb.getString(CANNOT_USE_REQUEST_OBJECT_OUTSIDE_SCOPE_EXCEPTION));
        }

        if (SecurityUtil.isPackageProtectionEnabled()){
            return AccessController.doPrivileged(
                new GetLocalesPrivilegedAction());
        } else {
            return request.getLocales();
        }        
    }


    public boolean isSecure() {

        if (request == null) {
            throw new IllegalStateException(rb.getString(CANNOT_USE_REQUEST_OBJECT_OUTSIDE_SCOPE_EXCEPTION));
        }

        return request.isSecure();
    }


    public RequestDispatcher getRequestDispatcher(String path) {

        if (request == null) {
            throw new IllegalStateException(rb.getString(CANNOT_USE_REQUEST_OBJECT_OUTSIDE_SCOPE_EXCEPTION));
        }

        if (SecurityUtil.isPackageProtectionEnabled()){
            return AccessController.doPrivileged(
                new GetRequestDispatcherPrivilegedAction(path));
        } else {
            return request.getRequestDispatcher(path);
        }
    }


    public String getRealPath(String path) {

        if (request == null) {
            throw new IllegalStateException(rb.getString(CANNOT_USE_REQUEST_OBJECT_OUTSIDE_SCOPE_EXCEPTION));
        }

        return request.getRealPath(path);
    }


    public String getAuthType() {

        if (request == null) {
            throw new IllegalStateException(rb.getString(CANNOT_USE_REQUEST_OBJECT_OUTSIDE_SCOPE_EXCEPTION));
        }

        return request.getAuthType();
    }


    public Cookie[] getCookies() {

        if (request == null) {
            throw new IllegalStateException(rb.getString(CANNOT_USE_REQUEST_OBJECT_OUTSIDE_SCOPE_EXCEPTION));
        }

        Cookie[] ret = null;

        /*
         * Clone the returned array only if there is a security manager
         * in place, so that performance won't suffer in the non-secure case
         */
        if (SecurityUtil.isPackageProtectionEnabled()){
            ret = AccessController.doPrivileged(
                new GetCookiesPrivilegedAction());
            if (ret != null) {
                ret = (Cookie[]) ret.clone();
            }
        } else {
            ret = request.getCookies();
        }

        return ret;
    }


    public long getDateHeader(String name) {

        if (request == null) {
            throw new IllegalStateException(rb.getString(CANNOT_USE_REQUEST_OBJECT_OUTSIDE_SCOPE_EXCEPTION));
        }

        return request.getDateHeader(name);
    }


    public String getHeader(String name) {

        if (request == null) {
            throw new IllegalStateException(rb.getString(CANNOT_USE_REQUEST_OBJECT_OUTSIDE_SCOPE_EXCEPTION));
        }

        return request.getHeader(name);
    }


    public Enumeration<String> getHeaders(String name) {

        if (request == null) {
            throw new IllegalStateException(rb.getString(CANNOT_USE_REQUEST_OBJECT_OUTSIDE_SCOPE_EXCEPTION));
        }

        if (SecurityUtil.isPackageProtectionEnabled()){
            return AccessController.doPrivileged(
                new GetHeadersPrivilegedAction(name));
        } else {
            return request.getHeaders(name);
        }         
    }


    public Enumeration<String> getHeaderNames() {

        if (request == null) {
            throw new IllegalStateException(rb.getString(CANNOT_USE_REQUEST_OBJECT_OUTSIDE_SCOPE_EXCEPTION));
        }

        if (SecurityUtil.isPackageProtectionEnabled()){
            return AccessController.doPrivileged(
                new GetHeaderNamesPrivilegedAction());
        } else {
            return request.getHeaderNames();
        }             
    }


    public int getIntHeader(String name) {

        if (request == null) {
            throw new IllegalStateException(rb.getString(CANNOT_USE_REQUEST_OBJECT_OUTSIDE_SCOPE_EXCEPTION));
        }

        return request.getIntHeader(name);
    }


    public String getMethod() {

        if (request == null) {
            throw new IllegalStateException(rb.getString(CANNOT_USE_REQUEST_OBJECT_OUTSIDE_SCOPE_EXCEPTION));
        }

        return request.getMethod();
    }


    public String getPathInfo() {

        if (request == null) {
            throw new IllegalStateException(rb.getString(CANNOT_USE_REQUEST_OBJECT_OUTSIDE_SCOPE_EXCEPTION));
        }

        return request.getPathInfo();
    }


    public String getPathTranslated() {

        if (request == null) {
            throw new IllegalStateException(rb.getString(CANNOT_USE_REQUEST_OBJECT_OUTSIDE_SCOPE_EXCEPTION));
        }

        return request.getPathTranslated();
    }


    /**
     * Gets the servlet context to which this servlet request was last
     * dispatched.
     *
     * @return the servlet context to which this servlet request was last
     * dispatched
     */
    public ServletContext getServletContext() {
        return request.getServletContext();
    }


    public String getContextPath() {
        if (request == null) {
            throw new IllegalStateException(rb.getString(CANNOT_USE_REQUEST_OBJECT_OUTSIDE_SCOPE_EXCEPTION));
        }
        return request.getContextPath(maskDefaultContextMapping);
    }


    public String getContextPath(boolean maskDefaultContextMapping) {
        if (request == null) {
            throw new IllegalStateException(rb.getString(CANNOT_USE_REQUEST_OBJECT_OUTSIDE_SCOPE_EXCEPTION));
        }
        return request.getContextPath(maskDefaultContextMapping);
    }


    public String getQueryString() {

        if (request == null) {
            throw new IllegalStateException(rb.getString(CANNOT_USE_REQUEST_OBJECT_OUTSIDE_SCOPE_EXCEPTION));
        }

        return request.getQueryString();
    }


    public String getRemoteUser() {

        if (request == null) {
            throw new IllegalStateException(rb.getString(CANNOT_USE_REQUEST_OBJECT_OUTSIDE_SCOPE_EXCEPTION));
        }

        return request.getRemoteUser();
    }


    public boolean isUserInRole(String role) {

        if (request == null) {
            throw new IllegalStateException(rb.getString(CANNOT_USE_REQUEST_OBJECT_OUTSIDE_SCOPE_EXCEPTION));
        }

        return request.isUserInRole(role);
    }


    public java.security.Principal getUserPrincipal() {

        if (request == null) {
            throw new IllegalStateException(rb.getString(CANNOT_USE_REQUEST_OBJECT_OUTSIDE_SCOPE_EXCEPTION));
        }

        return request.getUserPrincipal();
    }


    public String getRequestedSessionId() {

        if (request == null) {
            throw new IllegalStateException(rb.getString(CANNOT_USE_REQUEST_OBJECT_OUTSIDE_SCOPE_EXCEPTION));
        }

        return request.getRequestedSessionId();
    }


    public String getRequestURI() {

        if (request == null) {
            throw new IllegalStateException(rb.getString(CANNOT_USE_REQUEST_OBJECT_OUTSIDE_SCOPE_EXCEPTION));
        }

        return request.getRequestURI(maskDefaultContextMapping);
    }


    public StringBuffer getRequestURL() {

        if (request == null) {
            throw new IllegalStateException(rb.getString(CANNOT_USE_REQUEST_OBJECT_OUTSIDE_SCOPE_EXCEPTION));
        }

        return request.getRequestURL(maskDefaultContextMapping);
    }


    public String getServletPath() {

        if (request == null) {
            throw new IllegalStateException(rb.getString(CANNOT_USE_REQUEST_OBJECT_OUTSIDE_SCOPE_EXCEPTION));
        }

        return request.getServletPath();
    }


    public HttpSession getSession(boolean create) {

        if (request == null) {
            throw new IllegalStateException(rb.getString(CANNOT_USE_REQUEST_OBJECT_OUTSIDE_SCOPE_EXCEPTION));
        }

        if (SecurityUtil.isPackageProtectionEnabled()){
            return AccessController.
                doPrivileged(new GetSessionPrivilegedAction(create));
        } else {
            return request.getSession(create);
        }
    }

    public HttpSession getSession() {

        if (request == null) {
            throw new IllegalStateException(rb.getString(CANNOT_USE_REQUEST_OBJECT_OUTSIDE_SCOPE_EXCEPTION));
        }

        return getSession(true);
    }

    public String changeSessionId() {

        if (request == null) {
            throw new IllegalStateException(rb.getString(CANNOT_USE_REQUEST_OBJECT_OUTSIDE_SCOPE_EXCEPTION));
        }

        if (SecurityUtil.isPackageProtectionEnabled()){
            return AccessController.
                    doPrivileged(new ChangeSessionIdPrivilegedAction());
        } else {
            return request.changeSessionId();
        }
    }


    public boolean isRequestedSessionIdValid() {

        if (request == null) {
            throw new IllegalStateException(rb.getString(CANNOT_USE_REQUEST_OBJECT_OUTSIDE_SCOPE_EXCEPTION));
        }

        return request.isRequestedSessionIdValid();
    }


    public boolean isRequestedSessionIdFromCookie() {

        if (request == null) {
            throw new IllegalStateException(rb.getString(CANNOT_USE_REQUEST_OBJECT_OUTSIDE_SCOPE_EXCEPTION));
        }

        return request.isRequestedSessionIdFromCookie();
    }


    public boolean isRequestedSessionIdFromURL() {

        if (request == null) {
            throw new IllegalStateException(rb.getString(CANNOT_USE_REQUEST_OBJECT_OUTSIDE_SCOPE_EXCEPTION));
        }

        return request.isRequestedSessionIdFromURL();
    }


    public boolean isRequestedSessionIdFromUrl() {

        if (request == null) {
            throw new IllegalStateException(rb.getString(CANNOT_USE_REQUEST_OBJECT_OUTSIDE_SCOPE_EXCEPTION));
        }

        return request.isRequestedSessionIdFromURL();
    }


    public String getLocalAddr() {

        if (request == null) {
            throw new IllegalStateException(rb.getString(CANNOT_USE_REQUEST_OBJECT_OUTSIDE_SCOPE_EXCEPTION));
        }

        return request.getLocalAddr();
    }


    public String getLocalName() {

        if (request == null) {
            throw new IllegalStateException(rb.getString(CANNOT_USE_REQUEST_OBJECT_OUTSIDE_SCOPE_EXCEPTION));
        }

        return request.getLocalName();
    }


    public int getLocalPort() {

        if (request == null) {
            throw new IllegalStateException(rb.getString(CANNOT_USE_REQUEST_OBJECT_OUTSIDE_SCOPE_EXCEPTION));
        }

        return request.getLocalPort();
    }


    public int getRemotePort() {

        if (request == null) {
            throw new IllegalStateException(rb.getString(CANNOT_USE_REQUEST_OBJECT_OUTSIDE_SCOPE_EXCEPTION));
        }

        return request.getRemotePort();
    }


    public DispatcherType getDispatcherType() {
        if (request == null) {
            throw new IllegalStateException(rb.getString(CANNOT_USE_REQUEST_OBJECT_OUTSIDE_SCOPE_EXCEPTION));
        }
        return request.getDispatcherType();
    }


    /**
     * Starts async processing on this request.
     */
    public AsyncContext startAsync() throws IllegalStateException {
        if (request == null) {
            throw new IllegalStateException(rb.getString(CANNOT_USE_REQUEST_OBJECT_OUTSIDE_SCOPE_EXCEPTION));
        }
        return request.startAsync();
    }


    /**
     * Starts async processing on this request.
     */
    public AsyncContext startAsync(ServletRequest sreq,
                                   ServletResponse sresp)
            throws IllegalStateException {
        if (request == null) {
            throw new IllegalStateException(rb.getString(CANNOT_USE_REQUEST_OBJECT_OUTSIDE_SCOPE_EXCEPTION));
        }
        return request.startAsync(sreq, sresp);
    }
        

    /**
     * Checks whether async processing has started on this request.
     */
    public boolean isAsyncStarted() {
        if (request == null) {
            throw new IllegalStateException(rb.getString(CANNOT_USE_REQUEST_OBJECT_OUTSIDE_SCOPE_EXCEPTION));
        }
        return request.isAsyncStarted();
    }


    /**
     * Checks whether this request supports async.
     */
    public boolean isAsyncSupported() {
        if (request == null) {
            throw new IllegalStateException(rb.getString(CANNOT_USE_REQUEST_OBJECT_OUTSIDE_SCOPE_EXCEPTION));
        }
        return request.isAsyncSupported();
    }


    /**
     * Gets the AsyncContext of this request.
     */
    public AsyncContext getAsyncContext() {
        if (request == null) {
            throw new IllegalStateException(rb.getString(CANNOT_USE_REQUEST_OBJECT_OUTSIDE_SCOPE_EXCEPTION));
        }
        return request.getAsyncContext();
    }


    @Override
    public Collection<Part> getParts() throws IOException, ServletException {
        if (request == null) {
            throw new IllegalStateException(rb.getString(CANNOT_USE_REQUEST_OBJECT_OUTSIDE_SCOPE_EXCEPTION));
        }
        return request.getParts();
    }


    @Override
    public Part getPart(String name) throws IOException, ServletException {
        if (request == null) {
            throw new IllegalStateException(rb.getString(CANNOT_USE_REQUEST_OBJECT_OUTSIDE_SCOPE_EXCEPTION));
        }
        return request.getPart(name);
    }


    public boolean authenticate(HttpServletResponse response)
            throws IOException, ServletException {
        if (request == null) {
            throw new IllegalStateException(rb.getString(CANNOT_USE_REQUEST_OBJECT_OUTSIDE_SCOPE_EXCEPTION));
        }
        return request.authenticate(response);
    }


    public void login(String username, String password)
            throws ServletException {
        if (request == null) {
            throw new IllegalStateException(rb.getString(CANNOT_USE_REQUEST_OBJECT_OUTSIDE_SCOPE_EXCEPTION));
        }
        request.login(username, password);
    }


    public void logout() throws ServletException {
        if (request == null) {
            throw new IllegalStateException(rb.getString(CANNOT_USE_REQUEST_OBJECT_OUTSIDE_SCOPE_EXCEPTION));
        }
        request.logout();
    }


    public <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass) throws IOException, ServletException {
        if (request == null) {
            throw new IllegalStateException(rb.getString(CANNOT_USE_REQUEST_OBJECT_OUTSIDE_SCOPE_EXCEPTION));
        }
        return request.upgrade(handlerClass);
    }


    //START S1AS 4703023
    /**
     * Return the original <code>CoyoteRequest</code> object.
     */
    public Request getUnwrappedCoyoteRequest()
        throws AccessControlException {

        // tomcat does not have any Permission types so instead of
        // creating a TomcatPermission for this, use SecurityPermission.
        if (Globals.IS_SECURITY_ENABLED) {
            AccessController.checkPermission(
                GET_UNWRAPPED_COYOTE_REQUEST_PERMISSION);
        }

        return request;
    }
    //END S1AS 4703023
}
