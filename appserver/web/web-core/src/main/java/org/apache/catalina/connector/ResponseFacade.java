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
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
// Portions Copyright 2019-2024 Payara Foundation and/or affiliates

package org.apache.catalina.connector;

import org.apache.catalina.LogFacade;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

import static java.security.AccessController.doPrivileged;
import static org.apache.catalina.LogFacade.NULL_RESPONSE_OBJECT;
import static org.apache.catalina.security.SecurityUtil.isPackageProtectionEnabled;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.*;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Supplier;


/**
 * Facade class that wraps a Coyote response object. 
 * All methods are delegated to the wrapped response.
 *
 * @author Remy Maucherat
 * @author Jean-Francois Arcand
 * @version $Revision: 1.9 $ $Date: 2007/05/05 05:32:43 $
 */


public class ResponseFacade 
    implements HttpServletResponse {

    private static final ResourceBundle rb = LogFacade.getLogger().getResourceBundle();



    // ----------------------------------------------------------- DoPrivileged
    
    private final class SetContentTypePrivilegedAction
            implements PrivilegedAction<Void> {

        private final String contentType;

        public SetContentTypePrivilegedAction(String contentType){
            this.contentType = contentType;
        }
        
        @Override
        public Void run() {
            response.setContentType(contentType);
            return null;
        }            
    }
     
    
    // ----------------------------------------------------------- Constructors


    /**
     * Construct a wrapper for the specified response.
     *
     * @param response The response to be wrapped
     */
    public ResponseFacade(Response response) {
        this.response = response;
    }


    // ----------------------------------------------- Class/Instance Variables


    /**
     * The wrapped response.
     */
    protected Response response = null;


    // --------------------------------------------------------- Public Methods

    
    /**
     * Prevent cloning the facade.
     * @return never
     * @throws CloneNotSupportedException always
     */
    @Override
    protected Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }
      
    
    /**
     * Clear facade.
     */
    public void clear() {
        response = null;
    }


    public void finish() {

        // Disallow operation if the object has gone out of scope
        if (response == null) {
            throw new IllegalStateException(rb.getString(LogFacade.NULL_RESPONSE_OBJECT));
        }

        response.setSuspended(true);

    }


    public boolean isFinished() {

        // Disallow operation if the object has gone out of scope
        if (response == null) {
            throw new IllegalStateException(rb.getString(LogFacade.NULL_RESPONSE_OBJECT));
        }

        return response.isSuspended();
    }


    // ------------------------------------------------ ServletResponse Methods


    @Override
    public String getCharacterEncoding() {

        // Disallow operation if the object has gone out of scope
        if (response == null) {
            throw new IllegalStateException(rb.getString(NULL_RESPONSE_OBJECT));
        }

        return response.getCharacterEncoding();
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {

        // Disallow operation if the object has gone out of scope
        if (response == null) {
            throw new IllegalStateException(rb.getString(NULL_RESPONSE_OBJECT));
        }

        ServletOutputStream sos = response.getOutputStream();
        if (isFinished()) {
            response.setSuspended(true);
        }

        return sos;
    }

    @Override
    public PrintWriter getWriter() throws IOException {

        // Disallow operation if the object has gone out of scope
        if (response == null) {
            throw new IllegalStateException(rb.getString(NULL_RESPONSE_OBJECT));
        }

        PrintWriter writer = response.getWriter();
        if (isFinished()) {
            response.setSuspended(true);
        }
        
        return writer;
    }

    @Override
    public void setContentLength(int len) {

        // Disallow operation if the object has gone out of scope
        if (response == null) {
            throw new IllegalStateException(rb.getString(NULL_RESPONSE_OBJECT));
        }

        if (isCommitted()) {
            return;
        }

        response.setContentLength(len);
    }

    @Override
    public void setContentLengthLong(long len) {

        // Disallow operation if the object has gone out of scope
        if (response == null) {
            throw new IllegalStateException(rb.getString(NULL_RESPONSE_OBJECT));
        }

        if (isCommitted()) {
            return;
        }

        response.setContentLengthLong(len);
    }

    @Override
    public void setContentType(String type) {

        // Disallow operation if the object has gone out of scope
        if (response == null) {
            throw new IllegalStateException(rb.getString(NULL_RESPONSE_OBJECT));
        }

        if (isCommitted()) {
            return;
        }
        
        if (isPackageProtectionEnabled()){
            doPrivileged(new SetContentTypePrivilegedAction(type));
        } else {
            response.setContentType(type);            
        }
    }

    @Override
    public void setBufferSize(int size) {

        // Disallow operation if the object has gone out of scope
        if (response == null) {
            throw new IllegalStateException(rb.getString(NULL_RESPONSE_OBJECT));
        }

        if (isCommitted()) {
            throw new IllegalStateException
                (/*sm.getString("responseBase.reset.ise")*/);
        }

        response.setBufferSize(size);
    }

    @Override
    public int getBufferSize() {

        // Disallow operation if the object has gone out of scope
        if (response == null) {
            throw new IllegalStateException(rb.getString(NULL_RESPONSE_OBJECT));
        }

        return response.getBufferSize();
    }

    @Override
    public void flushBuffer() throws IOException {

        // Disallow operation if the object has gone out of scope
        if (response == null) {
            throw new IllegalStateException(rb.getString(NULL_RESPONSE_OBJECT));
        }

        if (isFinished()) {
            return;
        }
        
        if (isPackageProtectionEnabled()) {
            try {
                doPrivileged(new PrivilegedExceptionAction<Void>() {
                    @Override
                    public Void run() throws IOException {
                        response.setAppCommitted(true);

                        response.flushBuffer();
                        return null;
                    }
                });
            } catch (PrivilegedActionException e) {
                Exception ex = e.getException();
                if (ex instanceof IOException) {
                    throw (IOException) ex;
                }
            }
        } else {
            response.setAppCommitted(true);

            response.flushBuffer();
        }
    }

    @Override
    public void resetBuffer() {

        // Disallow operation if the object has gone out of scope
        if (response == null) {
            throw new IllegalStateException(rb.getString(NULL_RESPONSE_OBJECT));
        }

        if (isCommitted()) {
            throw new IllegalStateException();
        }

        response.resetBuffer();
    }

    @Override
    public boolean isCommitted() {

        // Disallow operation if the object has gone out of scope
        if (response == null) {
            throw new IllegalStateException(rb.getString(NULL_RESPONSE_OBJECT));
        }

        return response.isAppCommitted();
    }

    @Override
    public void reset() {

        // Disallow operation if the object has gone out of scope
        if (response == null) {
            throw new IllegalStateException(rb.getString(NULL_RESPONSE_OBJECT));
        }

        if (isCommitted()) {
            throw new IllegalStateException();
        }

        response.reset();
    }

    @Override
    public void setLocale(Locale loc) {

        // Disallow operation if the object has gone out of scope
        if (response == null) {
            throw new IllegalStateException(rb.getString(NULL_RESPONSE_OBJECT));
        }

        if (isCommitted()) {
            return;
        }

        response.setLocale(loc);
    }

    @Override
    public Locale getLocale() {

        // Disallow operation if the object has gone out of scope
        if (response == null) {
            throw new IllegalStateException(rb.getString(NULL_RESPONSE_OBJECT));
        }

        return response.getLocale();
    }

    @Override
    public void addCookie(Cookie cookie) {

        // Disallow operation if the object has gone out of scope
        if (response == null) {
            throw new IllegalStateException(rb.getString(NULL_RESPONSE_OBJECT));
        }

        if (isCommitted()) {
            return;
        }

        response.addCookie(cookie);
    }

    @Override
    public boolean containsHeader(String name) {

        // Disallow operation if the object has gone out of scope
        if (response == null) {
            throw new IllegalStateException(rb.getString(NULL_RESPONSE_OBJECT));
        }

        return response.containsHeader(name);
    }

    @Override
    public String encodeURL(String url) {

        // Disallow operation if the object has gone out of scope
        if (response == null) {
            throw new IllegalStateException(rb.getString(NULL_RESPONSE_OBJECT));
        }

        return response.encodeURL(url);
    }

    @Override
    public String encodeRedirectURL(String url) {

        // Disallow operation if the object has gone out of scope
        if (response == null) {
            throw new IllegalStateException(rb.getString(NULL_RESPONSE_OBJECT));
        }

        return response.encodeRedirectURL(url);
    }

    @Override
    public void sendError(int sc, String msg) throws IOException {

        // Disallow operation if the object has gone out of scope
        if (response == null) {
            throw new IllegalStateException(rb.getString(NULL_RESPONSE_OBJECT));
        }

        if (isCommitted()) {
            throw new IllegalStateException();
        }

        response.setAppCommitted(true);

        response.sendError(sc, msg);
    }

    @Override
    public void sendError(int sc) throws IOException {

        // Disallow operation if the object has gone out of scope
        if (response == null) {
            throw new IllegalStateException(rb.getString(NULL_RESPONSE_OBJECT));
        }

        if (isCommitted()) {
            throw new IllegalStateException();
        }

        response.setAppCommitted(true);

        response.sendError(sc);
    }

    @Override
    public void sendRedirect(String location) throws IOException {
        response.sendRedirect(location, SC_MOVED_TEMPORARILY, true);
    }

    @Override
    public void sendRedirect(String location, int sc, boolean clearBuffer) throws IOException {
        // Disallow operation if the object has gone out of scope
        if (response == null) {
            throw new IllegalStateException(rb.getString(NULL_RESPONSE_OBJECT));
        }

        if (isCommitted()) {
            throw new IllegalStateException();
        }

        response.setAppCommitted(true);

        response.sendRedirect(location, sc, clearBuffer);
    }

    @Override
    public void setDateHeader(String name, long date) {

        // Disallow operation if the object has gone out of scope
        if (response == null) {
            throw new IllegalStateException(rb.getString(NULL_RESPONSE_OBJECT));
        }

        if (isCommitted()) {
            return;
        }

        response.setDateHeader(name, date);
    }

    @Override
    public void addDateHeader(String name, long date) {

        // Disallow operation if the object has gone out of scope
        if (response == null) {
            throw new IllegalStateException(rb.getString(NULL_RESPONSE_OBJECT));
        }

        if (isCommitted()) {
            return;
        }

        response.addDateHeader(name, date);
    }

    @Override
    public void setHeader(String name, String value) {

        // Disallow operation if the object has gone out of scope
        if (response == null) {
            throw new IllegalStateException(rb.getString(NULL_RESPONSE_OBJECT));
        }

        if (isCommitted()) {
            return;
        }

        response.setHeader(name, value);
    }

    @Override
    public void addHeader(String name, String value) {

        // Disallow operation if the object has gone out of scope
        if (response == null) {
            throw new IllegalStateException(rb.getString(NULL_RESPONSE_OBJECT));
        }

        if (isCommitted()) {
            return;
        }

        response.addHeader(name, value);
    }

    @Override
    public void setIntHeader(String name, int value) {

        // Disallow operation if the object has gone out of scope
        if (response == null) {
            throw new IllegalStateException(rb.getString(NULL_RESPONSE_OBJECT));
        }

        if (isCommitted()) {
            return;
        }

        response.setIntHeader(name, value);
    }

    @Override
    public void addIntHeader(String name, int value) {

        // Disallow operation if the object has gone out of scope
        if (response == null) {
            throw new IllegalStateException(rb.getString(LogFacade.NULL_RESPONSE_OBJECT));
        }

        if (isCommitted())
            return;

        response.addIntHeader(name, value);
    }

    @Override
    public void setStatus(int sc) {

        // Disallow operation if the object has gone out of scope
        if (response == null) {
            throw new IllegalStateException(rb.getString(LogFacade.NULL_RESPONSE_OBJECT));
        }

        if (isCommitted())
            return;

        response.setStatus(sc);
    }

    @Override
    public String getContentType() {

        // Disallow operation if the object has gone out of scope
        if (response == null) {
            throw new IllegalStateException(rb.getString(LogFacade.NULL_RESPONSE_OBJECT));
        }

        return response.getContentType();
    }

    @Override
    public void setCharacterEncoding(String charSet) {

        // Disallow operation if the object has gone out of scope
        if (response == null) {
            throw new IllegalStateException(rb.getString(LogFacade.NULL_RESPONSE_OBJECT));
        }

        response.setCharacterEncoding(charSet);
    }


    // START SJSAS 6374990
    @Override
    public int getStatus() {

        // Disallow operation if the object has gone out of scope
        if (response == null) {
            throw new IllegalStateException(rb.getString(LogFacade.NULL_RESPONSE_OBJECT));
        }

        return response.getStatus();
    }

    // END SJSAS 6374990

    @Override
    public String getHeader(String name) {
        if (response == null) {
            throw new IllegalStateException(rb.getString(LogFacade.NULL_RESPONSE_OBJECT));
        }
        return response.getHeader(name);
    }

    @Override
    public Collection<String> getHeaders(String name) {
        if (response == null) {
            throw new IllegalStateException(rb.getString(LogFacade.NULL_RESPONSE_OBJECT));
        }
        return response.getHeaders(name);
    }
    
    @Override
    public Collection<String> getHeaderNames() {
        if (response == null) {
            throw new IllegalStateException(rb.getString(LogFacade.NULL_RESPONSE_OBJECT));
        }
        return response.getHeaderNames();
    }
    
    @Override
    public Supplier<Map<String, String>> getTrailerFields() {
        if (response == null) {
            throw new IllegalStateException(rb.getString(LogFacade.NULL_RESPONSE_OBJECT));
        }
        
        return response.getTrailerFields();
    }
    
    @Override
    public void setTrailerFields(Supplier<Map<String, String>> supplier) {
        if (response == null) {
            throw new IllegalStateException(rb.getString(NULL_RESPONSE_OBJECT));
        }
        
        response.setTrailerFields(supplier);
    }

}
