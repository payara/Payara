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

import org.apache.catalina.core.StandardServer;
import org.apache.catalina.security.SecurityUtil;
import org.glassfish.logging.annotation.LogMessageInfo;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.*;
import java.util.Collection;
import java.util.Locale;
import java.util.ResourceBundle;


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

    private static final ResourceBundle rb = StandardServer.log.getResourceBundle();

    @LogMessageInfo(
            message = "Null response object",
            level = "WARNING"
    )
    public static final String NULL_RESPONSE_OBJECT = "AS-WEB-CORE-00082";



    // ----------------------------------------------------------- DoPrivileged
    
    private final class SetContentTypePrivilegedAction
            implements PrivilegedAction<Void> {

        private String contentType;

        public SetContentTypePrivilegedAction(String contentType){
            this.contentType = contentType;
        }
        
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
     */
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
            throw new IllegalStateException(rb.getString(NULL_RESPONSE_OBJECT));
        }

        response.setSuspended(true);

    }


    public boolean isFinished() {

        // Disallow operation if the object has gone out of scope
        if (response == null) {
            throw new IllegalStateException(rb.getString(NULL_RESPONSE_OBJECT));
        }

        return response.isSuspended();
    }


    // ------------------------------------------------ ServletResponse Methods


    public String getCharacterEncoding() {

        // Disallow operation if the object has gone out of scope
        if (response == null) {
            throw new IllegalStateException(rb.getString(NULL_RESPONSE_OBJECT));
        }

        return response.getCharacterEncoding();
    }


    public ServletOutputStream getOutputStream() throws IOException {

        // Disallow operation if the object has gone out of scope
        if (response == null) {
            throw new IllegalStateException(rb.getString(NULL_RESPONSE_OBJECT));
        }

        //        if (isFinished())
        //            throw new IllegalStateException
        //                (/*sm.getString("responseFacade.finished")*/);

        ServletOutputStream sos = response.getOutputStream();
        if (isFinished())
            response.setSuspended(true);
        return (sos);
    }


    public PrintWriter getWriter() throws IOException {

        // Disallow operation if the object has gone out of scope
        if (response == null) {
            throw new IllegalStateException(rb.getString(NULL_RESPONSE_OBJECT));
        }

        //        if (isFinished())
        //            throw new IllegalStateException
        //                (/*sm.getString("responseFacade.finished")*/);

        PrintWriter writer = response.getWriter();
        if (isFinished())
            response.setSuspended(true);
        return (writer);
    }


    public void setContentLength(int len) {

        // Disallow operation if the object has gone out of scope
        if (response == null) {
            throw new IllegalStateException(rb.getString(NULL_RESPONSE_OBJECT));
        }

        if (isCommitted())
            return;

        response.setContentLength(len);
    }


    public void setContentLengthLong(long len) {

        // Disallow operation if the object has gone out of scope
        if (response == null) {
            throw new IllegalStateException(rb.getString(NULL_RESPONSE_OBJECT));
        }

        if (isCommitted())
            return;

        response.setContentLengthLong(len);
    }


    public void setContentType(String type) {

        // Disallow operation if the object has gone out of scope
        if (response == null) {
            throw new IllegalStateException(rb.getString(NULL_RESPONSE_OBJECT));
        }

        if (isCommitted())
            return;
        
        if (SecurityUtil.isPackageProtectionEnabled()){
            AccessController.doPrivileged(new SetContentTypePrivilegedAction(type));
        } else {
            response.setContentType(type);            
        }
    }


    public void setBufferSize(int size) {

        // Disallow operation if the object has gone out of scope
        if (response == null) {
            throw new IllegalStateException(rb.getString(NULL_RESPONSE_OBJECT));
        }

        if (isCommitted())
            throw new IllegalStateException
                (/*sm.getString("responseBase.reset.ise")*/);

        response.setBufferSize(size);
    }


    public int getBufferSize() {

        // Disallow operation if the object has gone out of scope
        if (response == null) {
            throw new IllegalStateException(rb.getString(NULL_RESPONSE_OBJECT));
        }

        return response.getBufferSize();
    }


    public void flushBuffer() throws IOException {

        // Disallow operation if the object has gone out of scope
        if (response == null) {
            throw new IllegalStateException(rb.getString(NULL_RESPONSE_OBJECT));
        }

        if (isFinished())
            //            throw new IllegalStateException
            //                (/*sm.getString("responseFacade.finished")*/);
            return;
        
        if (SecurityUtil.isPackageProtectionEnabled()){
            try{
                AccessController.doPrivileged(
                        new PrivilegedExceptionAction<Void>(){

                    public Void run() throws IOException{
                        response.setAppCommitted(true);

                        response.flushBuffer();
                        return null;
                    }
                });
            } catch(PrivilegedActionException e){
                Exception ex = e.getException();
                if (ex instanceof IOException){
                    throw (IOException)ex;
                }
            }
        } else {
            response.setAppCommitted(true);

            response.flushBuffer();            
        }
    }


    public void resetBuffer() {

        // Disallow operation if the object has gone out of scope
        if (response == null) {
            throw new IllegalStateException(rb.getString(NULL_RESPONSE_OBJECT));
        }

        if (isCommitted())
            throw new IllegalStateException
                (/*sm.getString("responseBase.reset.ise")*/);

        response.resetBuffer();
    }


    public boolean isCommitted() {

        // Disallow operation if the object has gone out of scope
        if (response == null) {
            throw new IllegalStateException(rb.getString(NULL_RESPONSE_OBJECT));
        }

        return (response.isAppCommitted());
    }


    public void reset() {

        // Disallow operation if the object has gone out of scope
        if (response == null) {
            throw new IllegalStateException(rb.getString(NULL_RESPONSE_OBJECT));
        }

        if (isCommitted())
            throw new IllegalStateException
                (/*sm.getString("responseBase.reset.ise")*/);

        response.reset();
    }


    public void setLocale(Locale loc) {

        // Disallow operation if the object has gone out of scope
        if (response == null) {
            throw new IllegalStateException(rb.getString(NULL_RESPONSE_OBJECT));
        }

        if (isCommitted())
            return;

        response.setLocale(loc);
    }


    public Locale getLocale() {

        // Disallow operation if the object has gone out of scope
        if (response == null) {
            throw new IllegalStateException(rb.getString(NULL_RESPONSE_OBJECT));
        }

        return response.getLocale();
    }


    public void addCookie(Cookie cookie) {

        // Disallow operation if the object has gone out of scope
        if (response == null) {
            throw new IllegalStateException(rb.getString(NULL_RESPONSE_OBJECT));
        }

        if (isCommitted())
            return;

        response.addCookie(cookie);
    }


    public boolean containsHeader(String name) {

        // Disallow operation if the object has gone out of scope
        if (response == null) {
            throw new IllegalStateException(rb.getString(NULL_RESPONSE_OBJECT));
        }

        return response.containsHeader(name);
    }


    public String encodeURL(String url) {

        // Disallow operation if the object has gone out of scope
        if (response == null) {
            throw new IllegalStateException(rb.getString(NULL_RESPONSE_OBJECT));
        }

        return response.encodeURL(url);
    }


    public String encodeRedirectURL(String url) {

        // Disallow operation if the object has gone out of scope
        if (response == null) {
            throw new IllegalStateException(rb.getString(NULL_RESPONSE_OBJECT));
        }

        return response.encodeRedirectURL(url);
    }


    public String encodeUrl(String url) {

        // Disallow operation if the object has gone out of scope
        if (response == null) {
            throw new IllegalStateException(rb.getString(NULL_RESPONSE_OBJECT));
        }

        return response.encodeURL(url);
    }


    public String encodeRedirectUrl(String url) {

        // Disallow operation if the object has gone out of scope
        if (response == null) {
            throw new IllegalStateException(rb.getString(NULL_RESPONSE_OBJECT));
        }

        return response.encodeRedirectURL(url);
    }


    public void sendError(int sc, String msg) throws IOException {

        // Disallow operation if the object has gone out of scope
        if (response == null) {
            throw new IllegalStateException(rb.getString(NULL_RESPONSE_OBJECT));
        }

        if (isCommitted())
            throw new IllegalStateException
                (/*sm.getString("responseBase.reset.ise")*/);

        response.setAppCommitted(true);

        response.sendError(sc, msg);
    }


    public void sendError(int sc) throws IOException {

        // Disallow operation if the object has gone out of scope
        if (response == null) {
            throw new IllegalStateException(rb.getString(NULL_RESPONSE_OBJECT));
        }

        if (isCommitted())
            throw new IllegalStateException
                (/*sm.getString("responseBase.reset.ise")*/);

        response.setAppCommitted(true);

        response.sendError(sc);
    }


    public void sendRedirect(String location) throws IOException {

        // Disallow operation if the object has gone out of scope
        if (response == null) {
            throw new IllegalStateException(rb.getString(NULL_RESPONSE_OBJECT));
        }

        if (isCommitted())
            throw new IllegalStateException
                (/*sm.getString("responseBase.reset.ise")*/);

        response.setAppCommitted(true);

        response.sendRedirect(location);
    }


    public void setDateHeader(String name, long date) {

        // Disallow operation if the object has gone out of scope
        if (response == null) {
            throw new IllegalStateException(rb.getString(NULL_RESPONSE_OBJECT));
        }

        if (isCommitted())
            return;

        response.setDateHeader(name, date);
    }


    public void addDateHeader(String name, long date) {

        // Disallow operation if the object has gone out of scope
        if (response == null) {
            throw new IllegalStateException(rb.getString(NULL_RESPONSE_OBJECT));
        }

        if (isCommitted())
            return;

        response.addDateHeader(name, date);
    }


    public void setHeader(String name, String value) {

        // Disallow operation if the object has gone out of scope
        if (response == null) {
            throw new IllegalStateException(rb.getString(NULL_RESPONSE_OBJECT));
        }

        if (isCommitted())
            return;

        response.setHeader(name, value);
    }


    public void addHeader(String name, String value) {

        // Disallow operation if the object has gone out of scope
        if (response == null) {
            throw new IllegalStateException(rb.getString(NULL_RESPONSE_OBJECT));
        }

        if (isCommitted())
            return;

        response.addHeader(name, value);
    }


    public void setIntHeader(String name, int value) {

        // Disallow operation if the object has gone out of scope
        if (response == null) {
            throw new IllegalStateException(rb.getString(NULL_RESPONSE_OBJECT));
        }

        if (isCommitted())
            return;

        response.setIntHeader(name, value);
    }


    public void addIntHeader(String name, int value) {

        // Disallow operation if the object has gone out of scope
        if (response == null) {
            throw new IllegalStateException(rb.getString(NULL_RESPONSE_OBJECT));
        }

        if (isCommitted())
            return;

        response.addIntHeader(name, value);
    }


    public void setStatus(int sc) {

        // Disallow operation if the object has gone out of scope
        if (response == null) {
            throw new IllegalStateException(rb.getString(NULL_RESPONSE_OBJECT));
        }

        if (isCommitted())
            return;

        response.setStatus(sc);
    }


    public void setStatus(int sc, String msg) {

        // Disallow operation if the object has gone out of scope
        if (response == null) {
            throw new IllegalStateException(rb.getString(NULL_RESPONSE_OBJECT));
        }

        if (isCommitted())
            return;

        response.setStatus(sc, msg);
    }


    public String getContentType() {

        // Disallow operation if the object has gone out of scope
        if (response == null) {
            throw new IllegalStateException(rb.getString(NULL_RESPONSE_OBJECT));
        }

        return response.getContentType();
    }


    public void setCharacterEncoding(String arg0) {

        // Disallow operation if the object has gone out of scope
        if (response == null) {
            throw new IllegalStateException(rb.getString(NULL_RESPONSE_OBJECT));
        }

        response.setCharacterEncoding(arg0);
    }


    // START SJSAS 6374990
    public int getStatus() {

        // Disallow operation if the object has gone out of scope
        if (response == null) {
            throw new IllegalStateException(rb.getString(NULL_RESPONSE_OBJECT));
        }

        return response.getStatus();
    }

    // END SJSAS 6374990


    public String getHeader(String name) {
        if (response == null) {
            throw new IllegalStateException(rb.getString(NULL_RESPONSE_OBJECT));
        }
        return response.getHeader(name);
    }


    public Collection<String> getHeaders(String name) {
        if (response == null) {
            throw new IllegalStateException(rb.getString(NULL_RESPONSE_OBJECT));
        }
        return response.getHeaders(name);
    }
    

    public Collection<String> getHeaderNames() {
        if (response == null) {
            throw new IllegalStateException(rb.getString(NULL_RESPONSE_OBJECT));
        }
        return response.getHeaderNames();
    }

}
