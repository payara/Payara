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

package org.apache.catalina;


import javax.servlet.FilterChain;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.Iterator;

/**
 * A <b>Request</b> is the Catalina-internal facade for a
 * <code>ServletRequest</code> that is to be processed, in order to
 * produce the corresponding <code>Response</code>.
 *
 * @author Craig R. McClanahan
 * @version $Revision: 1.4 $ $Date: 2006/04/24 16:36:12 $
 */

public interface Request {


    // ------------------------------------------------------------- Properties


    /**
     * Return the authorization credentials sent with this request.
     */
    public String getAuthorization();


    /**
     * Return the Connector through which this Request was received.
     */
    public Connector getConnector();


    /**
     * Set the Connector through which this Request was received.
     *
     * @param connector The new connector
     */
    public void setConnector(Connector connector);


    /**
     * Return the Context within which this Request is being processed.
     */
    public Context getContext();


    /**
     * Set the Context within which this Request is being processed.  This
     * must be called as soon as the appropriate Context is identified, because
     * it identifies the value to be returned by <code>getContextPath()</code>,
     * and thus enables parsing of the request URI.
     *
     * @param context The newly associated Context
     */
    public void setContext(Context context);


    /**
     * Get filter chain associated with the request.
     */
    public FilterChain getFilterChain();


    /**
     * Set filter chain associated with the request.
     * 
     * @param filterChain new filter chain
     */
    public void setFilterChain(FilterChain filterChain);


    /**
     * Return the Host within which this Request is being processed.
     */
    public Host getHost();


    /**
     * Set the Host within which this Request is being processed.  This
     * must be called as soon as the appropriate Host is identified, and
     * before the Request is passed to a context.
     *
     * @param host The newly associated Host
     */
    public void setHost(Host host);


    /**
     * Return descriptive information about this Request implementation and
     * the corresponding version number, in the format
     * <code>&lt;description&gt;/&lt;version&gt;</code>.
     */
    public String getInfo();


    /**
     * Return the <code>ServletRequest</code> for which this object
     * is the facade.
     */
    public ServletRequest getRequest();


    /**
     * Return the <code>ServletRequest</code> for which this object
     * is the facade.
     *
     * @param maskDefaultContextMapping true if the fact that a request
     * received at the root context was mapped to a default-web-module will
     * be masked, false otherwise
     */
    public ServletRequest getRequest(boolean maskDefaultContextMapping);


    /**
     * Return the Response with which this Request is associated.
     */
    public Response getResponse();


    /**
     * Set the Response with which this Request is associated.
     *
     * @param response The new associated response
     */
    public void setResponse(Response response);


    /**
     * Return the Socket (if any) through which this Request was received.
     * This should <strong>only</strong> be used to access underlying state
     * information about this Socket, such as the SSLSession associated with
     * an SSLSocket.
     */
    public Socket getSocket();


    /**
     * Set the Socket (if any) through which this Request was received.
     *
     * @param socket The socket through which this request was received
     */
    public void setSocket(Socket socket);


    /**
     * Return the input stream associated with this Request.
     */
    public InputStream getStream();


    /**
     * Set the input stream associated with this Request.
     *
     * @param stream The new input stream
     */
    public void setStream(InputStream stream);


    /**
     * Return the Wrapper within which this Request is being processed.
     */
    public Wrapper getWrapper();


    /**
     * Set the Wrapper within which this Request is being processed.  This
     * must be called as soon as the appropriate Wrapper is identified, and
     * before the Request is ultimately passed to an application servlet.
     *
     * @param wrapper The newly associated Wrapper
     */
    public void setWrapper(Wrapper wrapper);


    // --------------------------------------------------------- Public Methods


    /**
     * Create and return a ServletInputStream to read the content
     * associated with this Request.
     *
     * @exception IOException if an input/output error occurs
     */
    public ServletInputStream createInputStream() throws IOException;


    /**
     * Perform whatever actions are required to flush and close the input
     * stream or reader, in a single operation.
     *
     * @exception IOException if an input/output error occurs
     */
    public void finishRequest() throws IOException;


    /**
     * Return the object bound with the specified name to the internal notes
     * for this request, or <code>null</code> if no such binding exists.
     *
     * @param name Name of the note to be returned
     */
    public Object getNote(String name);


    /**
     * Return an Iterator containing the String names of all notes bindings
     * that exist for this request.
     */
    public Iterator getNoteNames();


    /**
     * Release all object references, and initialize instance variables, in
     * preparation for reuse of this object.
     */
    public void recycle();


    /**
     * Remove any object bound to the specified name in the internal notes
     * for this request.
     *
     * @param name Name of the note to be removed
     */
    public void removeNote(String name);


    /**
     * Set the content length associated with this Request.
     *
     * @param length The new content length
     */
    public void setContentLength(int length);


    /**
     * Set the content type (and optionally the character encoding)
     * associated with this Request.  For example,
     * <code>text/html; charset=ISO-8859-4</code>.
     *
     * @param type The new content type
     */
    public void setContentType(String type);


    /**
     * Bind an object to a specified name in the internal notes associated
     * with this request, replacing any existing binding for this name.
     *
     * @param name Name to which the object should be bound
     * @param value Object to be bound to the specified name
     */
    public void setNote(String name, Object value);


    /**
     * Set the protocol name and version associated with this Request.
     *
     * @param protocol Protocol name and version
     */
    public void setProtocol(String protocol);


    /**
     * Set the remote IP address associated with this Request.  NOTE:  This
     * value will be used to resolve the value for <code>getRemoteHost()</code>
     * if that method is called.
     *
     * @param remote The remote IP address
     */
    public void setRemoteAddr(String remote);


    /**
     * Set the value to be returned by <code>isSecure()</code>
     * for this Request.
     *
     * @param secure The new isSecure value
     */
    public void setSecure(boolean secure);


    /**
     * Set the name of the server (virtual host) to process this request.
     *
     * @param name The server name
     */
    public void setServerName(String name);


    /**
     * Set the port number of the server to process this request.
     *
     * @param port The server port
     */
    public void setServerPort(int port);


    // START CR 6415120
    /**
     * Set whether or not access to resources under WEB-INF or META-INF
     * needs to be checked.
     */
    public void setCheckRestrictedResources(boolean check);


    /**
     * Return whether or not access to resources under WEB-INF or META-INF
     * needs to be checked.
     */
    public boolean getCheckRestrictedResources();
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
    public String getJrouteId();
    // END SJSAS 6346226


    /**
     * Generate and return a new session ID.
     *
     * This hook allows connectors to provide their own scalable session
     * ID generators.
     */
    public String generateSessionId();


    /**
     * Disables async support on this request.
     */
    public void disableAsyncSupport();


    /**
     * Sets the requested session cookie path, see IT 7426
     */
    public void setRequestedSessionCookiePath(String cookiePath);


    /**
     * Gets the session associated with this Request, creating one
     * if necessary and requested.
     *
     * @param create true if a new session is to be created if one does not
     * already exist, false otherwise
     */
    public Session getSessionInternal(boolean create);


    /**
     * Change the ID of the session that this request is associated with. There
     * are several things that may trigger an ID change. These include moving
     * between nodes in a cluster and session fixation prevention during the
     * authentication process.
     */
    public String changeSessionId();

    public Session lockSession();

    public void unlockSession();
}
