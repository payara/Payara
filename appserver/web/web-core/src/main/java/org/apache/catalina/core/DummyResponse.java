/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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


import org.apache.catalina.Connector;
import org.apache.catalina.Context;
import org.apache.catalina.HttpResponse;
import org.apache.catalina.Request;

import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Locale;

/**
 * Dummy response object, used for JSP precompilation.
 *
 * @author Remy Maucherat
 * @version $Revision: 1.2 $ $Date: 2005/12/08 01:27:34 $
 */

public class DummyResponse
    implements HttpResponse, HttpServletResponse {

    public DummyResponse() {
    }


    public void setAppCommitted(boolean appCommitted) {}
    public boolean isAppCommitted() { return false; }
    public Connector getConnector() { return null; }
    public void setConnector(Connector connector) {}
    public int getContentCount() { return -1; }
    public Context getContext() { return null; }
    public void setContext(Context context) {}
    public boolean getIncluded() { return false; }
    public void setIncluded(boolean included) {}
    public String getInfo() { return null; }
    public Request getRequest() { return null; }
    public void setRequest(Request request) {}
    public ServletResponse getResponse() { return null; }
    public OutputStream getStream() { return null; }
    public void setStream(OutputStream stream) {}
    public void setSuspended(boolean suspended) {}
    public boolean isSuspended() { return false; }
    public void setError() {}
    public boolean isError() { return false; }
    public ServletOutputStream createOutputStream() throws IOException {
        return null;
    }
    public void finishResponse() throws IOException {}
    public int getContentLength() { return -1; }
    public String getContentType() { return null; }
    public PrintWriter getReporter() { return null; }
    public void recycle() {}
    public void write(int b) throws IOException {}
    public void write(byte b[]) throws IOException {}
    public void write(byte b[], int off, int len) throws IOException {}
    public void flushBuffer() throws IOException {}
    public int getBufferSize() { return -1; }
    public String getCharacterEncoding() { return null; }
    public void setCharacterEncoding(String charEncoding) {}
    public ServletOutputStream getOutputStream() throws IOException {
        return null;
    }
    public Locale getLocale() { return null; }
    public PrintWriter getWriter() throws IOException { return null; }
    public boolean isCommitted() { return false; }
    public void reset() {}
    public void resetBuffer() {}
    
    public void resetBuffer(boolean resetWriterStreamFlags) {}
    public void setBufferSize(int size) {}
    public void setContentLength(int length) {}
    public void setContentLengthLong(long length) {}
    public void setContentType(String type) {}
    public void setLocale(Locale locale) {}

    public String getHeader(String name) { return null; }
    public Collection<String> getHeaderNames() { return null; }
    public Collection<String> getHeaders(String name) { return null; }
    public void addSessionCookieInternal(final Cookie cookie) {}
    public String getMessage() { return null; }
    public int getStatus() { return -1; }
    public void reset(int status, String message) {}
    public void addCookie(Cookie cookie) {}
    public void addDateHeader(String name, long value) {}
    public void addHeader(String name, String value) {}
    public void addIntHeader(String name, int value) {}
    public boolean containsHeader(String name) { return false; }
    public String encodeRedirectURL(String url) { return null; }
    public String encodeRedirectUrl(String url) { return null; }
    public String encodeURL(String url) { return null; }
    public String encodeUrl(String url) { return null; }
    public String encode(String url) { return null; }
    public void sendAcknowledgement() throws IOException {}
    public void sendError(int status) throws IOException {}
    public void sendError(int status, String message) throws IOException {}
    public void sendRedirect(String location) throws IOException {}
    public void setDateHeader(String name, long value) {}
    public void setHeader(String name, String value) {}
    public void setIntHeader(String name, int value) {}
    public void setStatus(int status) {}
    public void setStatus(int status, String message) {}
    public void setDetailMessage(String msg) {}
    public String getDetailMessage() { return null; }

}
