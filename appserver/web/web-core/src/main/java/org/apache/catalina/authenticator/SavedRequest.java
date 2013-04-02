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

package org.apache.catalina.authenticator;


import javax.servlet.http.Cookie;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;

import org.glassfish.grizzly.http.util.ByteChunk;

/**
 * Object that saves the critical information from a request so that
 * form-based authentication can reproduce it once the user has been
 * authenticated.
 * <p>
 * <b>IMPLEMENTATION NOTE</b> - It is assumed that this object is accessed
 * only from the context of a single thread, so no synchronization around
 * internal collection classes is performed.
 * <p>
 * <b>FIXME</b> - Currently, this object has no mechanism to save or
 * restore the data content of the request, although it does save
 * request parameters so that a POST transaction can be faithfully
 * duplicated.
 *
 * @author Craig R. McClanahan
 * @version $Revision: 1.2 $ $Date: 2005/12/08 01:27:28 $
 */

public final class SavedRequest {


    /**
     * The set of Cookies associated with this Request.
     */
    private ArrayList<Cookie> cookies = new ArrayList<Cookie>();

    public void addCookie(Cookie cookie) {
        cookies.add(cookie);
    }

    public Iterator<Cookie> getCookies() {
        return (cookies.iterator());
    }


    /**
     * The set of Headers associated with this Request.  Each key is a header
     * name, while the value is a ArrayList containing one or more actual
     * values for this header.  The values are returned as an Iterator when
     * you ask for them.
     */
    private HashMap<String, ArrayList<String>> headers =
        new HashMap<String, ArrayList<String>>();

    public void addHeader(String name, String value) {
        ArrayList<String> values = (ArrayList<String>) headers.get(name);
        if (values == null) {
            values = new ArrayList<String>();
            headers.put(name, values);
        }
        values.add(value);
    }

    public Iterator<String> getHeaderNames() {
        return (headers.keySet().iterator());
    }

    public Iterator<String> getHeaderValues(String name) {
        ArrayList<String> values = (ArrayList<String>) headers.get(name);
        if (values == null)
            return ((new ArrayList<String>()).iterator());
        else
            return (values.iterator());
    }


    /**
     * The set of Locales associated with this Request.
     */
    private ArrayList<Locale> locales = new ArrayList<Locale>();

    public void addLocale(Locale locale) {
        locales.add(locale);
    }

    public Iterator<Locale> getLocales() {
        return (locales.iterator());
    }


    /**
     * The request method used on this Request.
     */
    private String method = null;

    public String getMethod() {
        return (this.method);
    }

    public void setMethod(String method) {
        this.method = method;
    }



    /**
     * The set of request parameters associated with this Request.  Each
     * entry is keyed by the parameter name, pointing at a String array of
     * the corresponding values.
     */
    private HashMap<String, String[]> parameters =
        new HashMap<String, String[]>();

    public void addParameter(String name, String values[]) {
        parameters.put(name, values);
    }

    public Iterator<String> getParameterNames() {
        return (parameters.keySet().iterator());
    }

    public String[] getParameterValues(String name) {
        return parameters.get(name);
    }


    /**
     * The query string associated with this Request.
     */
    private String queryString = null;

    public String getQueryString() {
        return (this.queryString);
    }

    public void setQueryString(String queryString) {
        this.queryString = queryString;
    }


    /**
     * The request URI associated with this Request.
     */
    private String requestURI = null;

    public String getRequestURI() {
        return (this.requestURI);
    }

    public void setRequestURI(String requestURI) {
        this.requestURI = requestURI;
    }

    /**
     * The body of this request.
     */
    private ByteChunk body = null;

    public ByteChunk getBody() {
        return (this.body);
    }

    public void setBody(ByteChunk body) {
        this.body = body;
    }

    /**
     * The content type of the request, used if this is a POST.
     */
    private String contentType = null;

    public String getContentType() {
        return (this.contentType);
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    private int contentLength = -1;

    public int getContentLenght() {
        return contentLength;
    }

    public void setContentLength(int contentLength) {
        this.contentLength = contentLength;
    }
}
