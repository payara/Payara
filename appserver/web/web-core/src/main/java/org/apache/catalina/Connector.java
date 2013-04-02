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

package org.apache.catalina;

import java.security.cert.X509Certificate;

import com.sun.appserv.ProxyHandler;
import org.apache.catalina.net.ServerSocketFactory;
import org.glassfish.grizzly.http.server.HttpHandler;

// END SJSAS 6363251

/**
 * A <b>Connector</b> is a component responsible receiving requests from,
 * and returning responses to, a client application.  A Connector performs
 * the following general logic:
 * <ul>
 * <li>Receive a request from the client application.
 * <li>Create (or allocate from a pool) appropriate Request and Response
 *     instances, and populate their properties based on the contents of
 *     the received request, as described below.
 *     <ul>
 *     <li>For all Requests, the <code>connector</code>,
 *         <code>protocol</code>, <code>remoteAddr</code>,
 *         <code>response</code>, <code>scheme</code>,
 *         <code>secure</code>, <code>serverName</code>,
 *         <code>serverPort</code> and <code>stream</code>
 *         properties <b>MUST</b> be set. The <code>contentLength</code>
 *         and <code>contentType</code> properties are also generally set.
 *     <li>For HttpRequests, the <code>method</code>, <code>queryString</code>,
 *         <code>requestedSessionCookie</code>,
 *         <code>requestedSessionId</code>, <code>requestedSessionURL</code>,
 *         <code>requestURI</code>, and <code>secure</code> properties
 *         <b>MUST</b> be set.  In addition, the various <code>addXxx</code>
 *         methods must be called to record the presence of cookies, headers,
 *         and locales in the original request.
 *     <li>For all Responses, the <code>connector</code>, <code>request</code>,
 *         and <code>stream</code> properties <b>MUST</b> be set.
 *     <li>No additional headers must be set by the Connector for
 *         HttpResponses.
 *     </ul>
 * <li>Identify an appropriate Container to use for processing this request.
 *     For a stand alone Catalina installation, this will probably be a
 *     (singleton) Engine implementation.  For a Connector attaching Catalina
 *     to a web server such as Apache, this step could take advantage of
 *     parsing already performed within the web server to identify the
 *     Context, and perhaps even the Wrapper, to utilize in satisfying this
 *     Request.
 * <li>Call the <code>invoke()</code> method of the selected Container,
 *     passing the initialized Request and Response instances as arguments.
 * <li>Return any response created by the Container to the client, or
 *     return an appropriate error message if an exception of any type
 *     was thrown.
 * <li>If utilizing a pool of Request and Response objects, recycle the pair
 *     of instances that was just used.
 * </ul>
 * It is expected that the implementation details of various Connectors will
 * vary widely, so the logic above should considered typical rather than
 * normative.
 *
 * @author Craig R. McClanahan
 * @version $Revision: 1.6 $ $Date: 2007/05/05 05:31:50 $
 */

public interface Connector {


    // ------------------------------------------------------------- Properties


    /**
     * Return the Container used for processing requests received by this
     * Connector.
     */
    Container getContainer();


    /**
     * Set the Container used for processing requests received by this
     * Connector.
     *
     * @param container The new Container to use
     */
    void setContainer(Container container);


    /**
     * Return the "enable DNS lookups" flag.
     */
    boolean getEnableLookups();


    /**
     * Set the "enable DNS lookups" flag.
     *
     * @param enableLookups The new "enable DNS lookups" flag value
     */
    void setEnableLookups(boolean enableLookups);


    /**
     * Return the server socket factory used by this Container.
     */
    ServerSocketFactory getFactory();


    /**
     * Set the server socket factory used by this Container.
     *
     * @param factory The new server socket factory
     */
    void setFactory(ServerSocketFactory factory);


    /**
     * Return descriptive information about this Connector implementation.
     */
    String getInfo();


    /**
     * Return the port number to which a request should be redirected if
     * it comes in on a non-SSL port and is subject to a security constraint
     * with a transport guarantee that requires SSL.
     */
    int getRedirectPort();


    /**
     * Set the redirect port number.
     *
     * @param redirectPort The redirect port number (non-SSL to SSL)
     */
    void setRedirectPort(int redirectPort);


    /**
     * Return the scheme that will be assigned to requests received
     * through this connector.  Default value is "http".
     */
    String getScheme();


    /**
     * Set the scheme that will be assigned to requests received through
     * this connector.
     *
     * @param scheme The new scheme
     */
    void setScheme(String scheme);


    /**
     * Return the secure connection flag that will be assigned to requests
     * received through this connector.  Default value is "false".
     */
    boolean getSecure();


    /**
     * Set the secure connection flag that will be assigned to requests
     * received through this connector.
     *
     * @param secure The new secure connection flag
     */
    void setSecure(boolean secure);


    /**
     * Return the <code>Service</code> with which we are associated (if any).
     */
    Service getService();


    /**
     * Set the <code>Service</code> with which we are associated (if any).
     *
     * @param service The service that owns this Engine
     */
    void setService(Service service);


    // BEGIN S1AS 5000999
    /**
     * Sets the default host for this Connector.
     *
     * @param defaultHost The default host for this Connector
     */
    void setDefaultHost(String defaultHost);

    /**
     * Gets the default host of this Connector.
     *
     * @return The default host of this Connector
     */
    String getDefaultHost();
    // END S1AS 5000999


    // START S1AS 6188932
    /**
     * Returns the value of this connector's authPassthroughEnabled flag.
     *
     * @return true if this connector is receiving its requests from
     * a trusted intermediate server, false otherwise
     */
    boolean getAuthPassthroughEnabled();

    /**
     * Sets the value of this connector's authPassthroughEnabled flag.
     *
     * @param authPassthroughEnabled true if this connector is receiving its
     * requests from a trusted intermediate server, false otherwise
     */
    void setAuthPassthroughEnabled(boolean authPassthroughEnabled);

    /**
     * Gets the ProxyHandler instance associated with this CoyoteConnector.
     * 
     * @return ProxyHandler instance associated with this CoyoteConnector,
     * or null
     */
    ProxyHandler getProxyHandler();

    /**
     * Sets the ProxyHandler implementation for this CoyoteConnector to use.
     * 
     * @param proxyHandler ProxyHandler instance to use
     */
    void setProxyHandler(ProxyHandler proxyHandler);
    // END S1AS 6188932
    
    /**
     * Gets the name of this Connector.
     */
    String getName();

    /**
     * Sets the jvmRoute associated with this connector.
     */
    void setJvmRoute(String jvmRoute);

    /**
     * Get the jvmRoute associated with this connector.
     */
    String getJvmRoute();

    // --------------------------------------------------------- Public Methods


    /**
     * Create (or allocate) and return a Request object suitable for
     * specifying the contents of a Request to the responsible Container.
     */
    Request createRequest();


    /**
     * Create (or allocate) and return a Response object suitable for
     * receiving the contents of a Response from the responsible Container.
     */
    Response createResponse();

    /**
     * Invoke a pre-startup initialization. This is used to allow connectors
     * to bind to restricted ports under Unix operating environments.
     *
     * @exception LifecycleException If this server was already initialized.
     */
    void initialize()
    throws LifecycleException;

    // START OF SJSAS 8.1 PE 6191830
    /**
      * Get the underlying WebContainer certificate for the request     
      */
    X509Certificate[] getCertificates(Request request);
    // END OF SJSAS 8.1 PE 6191830

    // START CR 6309511
    /**
     * Get the encoding to be used for byte<-->char conversion for
     * data sent/received via this Connector
     */
    String getURIEncoding();

    /**
     * Set the encoding to be used for byte<-->char conversion for
     * data sent/received via this Connector
     */
    void setURIEncoding(String encoding);
    // END CR 6309511

    
    // START SJSAS 6363251
    /**
     * Set the <code>Adapter</code> used by this connector.
     */
    void setHandler(HttpHandler adapter);
    
    
    /**
     * Get the <code>HttpHandler</code> used by this connector.
     */
    HttpHandler getHandler();
    // END SJSAS 6363251

    /**
     * Set the maximum size of a POST which will be automatically
     * parsed by the container.
     *
     * @param maxPostSize The new maximum size in bytes of a POST which will
     * be automatically parsed by the container
     */
    void setMaxPostSize(int maxPostSize);


    /**
     * Return the maximum size of a POST which will be saved by the container
     * during authentication.
     */
    int getMaxSavePostSize();
}
