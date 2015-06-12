/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2015 Oracle and/or its affiliates. All rights reserved.
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

import java.lang.reflect.Constructor;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.security.cert.X509Certificate;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.servlet.http.HttpServletRequest;

import com.sun.appserv.ProxyHandler;
import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.Globals;
import org.apache.catalina.Host;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Service;
import org.apache.catalina.core.StandardEngine;
import org.apache.catalina.core.StandardServer;
import org.apache.catalina.net.ServerSocketFactory;
import org.apache.catalina.util.LifecycleSupport;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.logging.annotation.LogMessageInfo;
import org.glassfish.web.util.IntrospectionUtils;
import org.glassfish.grizzly.http.server.util.Mapper;

/**
 * Implementation of a Coyote connector for Tomcat 5.x.
 *
 * @author Craig R. McClanahan
 * @author Remy Maucherat
 * @version $Revision: 1.23 $ $Date: 2007/07/09 20:46:45 $
 */
public class Connector
    implements org.apache.catalina.Connector, Lifecycle
{
    private static final Logger log = StandardServer.log;
    private static final ResourceBundle rb = log.getResourceBundle();

    @LogMessageInfo(
            message = "The connector has already been initialized",
            level = "INFO"
    )
    public static final String CONNECTOR_BEEN_INIT = "AS-WEB-CORE-00028";

    @LogMessageInfo(
            message = "Error registering connector ",
            level = "SEVERE",
            cause = "Could not register connector",
            action = "Verify domain name and type"
    )
    public static final String ERROR_REGISTER_CONNECTOR_EXCEPTION = "AS-WEB-CORE-00029";

    @LogMessageInfo(
            message = "Failed to instanciate HttpHandler ",
            level = "WARNING"
    )
    public static final String FAILED_INSTANCIATE_HTTP_HANDLER_EXCEPTION = "AS-WEB-CORE-00030";

    @LogMessageInfo(
            message = "mod_jk invalid Adapter implementation: {0} ",
            level = "WARNING"
    )
    public static final String INVALID_ADAPTER_IMPLEMENTATION_EXCEPTION = "AS-WEB-CORE-00031";

    @LogMessageInfo(
            message = "Protocol handler instantiation failed: {0}",
            level = "WARNING"
    )
    public static final String PROTOCOL_HANDLER_INIT_FAILED_EXCEPTION = "AS-WEB-CORE-00032";

    @LogMessageInfo(
            message = "The connector has already been started",
            level = "INFO"
    )
    public static final String CONNECTOR_BEEN_STARTED = "AS-WEB-CORE-00033";

    @LogMessageInfo(
            message = "Protocol handler start failed: {0}",
            level = "WARNING"
    )
    public static final String PROTOCOL_HANDLER_START_FAILED_EXCEPTION = "AS-WEB-CORE-00034";

    @LogMessageInfo(
            message = "Coyote connector has not been started",
            level = "SEVERE",
            cause = "Could not stop processing requests via this Connector",
            action = "Verify if the connector has not been started"
    )
    public static final String CONNECTOR_NOT_BEEN_STARTED = "AS-WEB-CORE-00035";

    @LogMessageInfo(
            message = "Protocol handler destroy failed: {0}",
            level = "WARNING"
    )
    public static final String PROTOCOL_HANDLER_DESTROY_FAILED_EXCEPTION = "AS-WEB-CORE-00036";

    // ---------------------------------------------- Adapter Configuration --//
    
    // START SJSAS 6363251
    /**
     * Coyote Adapter class name.
     * Defaults to the CoyoteAdapter.
     */
    private String defaultClassName =
        "org.apache.catalina.connector.CoyoteAdapter";
    // END SJSAS 6363251

    
    // ----------------------------------------------------- Instance Variables

    /**
     * Holder for our configured properties.
     */
    private Map<String, String> properties = new HashMap<String, String>();

    /**
     * The <code>Service</code> we are associated with (if any).
     */
    private Service service = null;

    /**
     * The accept count for this Connector.
     */
    private int acceptCount = 10;

    /**
     * The IP address on which to bind, if any.  If <code>null</code>, all
     * addresses on the server will be bound.
     */
    private String address = null;
                                                                           
    /**
     * Do we allow TRACE ?
     */
    private boolean allowTrace = true;

    /**
     * The input buffer size we should create on input streams.
     */
    private int bufferSize = 4096;

    /**
     * The Container used for processing requests received by this Connector.
     */
    protected Container container = null;

    /**
     * Compression value.
     */
    private String compression = "off";

    /**
     * The debugging detail level for this component.
     */
    private int debug = 0;

    /**
     * The "enable DNS lookups" flag for this Connector.
     */
    private boolean enableLookups = false;

    /**
     * The server socket factory for this component.
     */
    private ServerSocketFactory factory = null;

    /**
     * Maximum size of a HTTP header. 4KB is the default.
     */
    private int maxHttpHeaderSize = 4 * 1024;

    /*
     * Is generation of X-Powered-By response header enabled/disabled?
     */
    private boolean xpoweredBy;

    /**
     * Descriptive information about this Connector implementation.
     */
    private static final String info =
        "org.apache.catalina.connector.Connector/2.0";

    /**
     * The lifecycle event support for this component.
     */
    protected LifecycleSupport lifecycle = new LifecycleSupport(this);

    /**
     * The minimum number of processors to start at initialization time.
     */
    protected int minProcessors = 5;

    /**
     * The maximum number of processors allowed, or <0 for unlimited.
     */
    private int maxProcessors = 20;

    /**
     * Linger value on the incoming connection.
     * Note : a value inferior to 0 means no linger.
     */
    private int connectionLinger = Constants.DEFAULT_CONNECTION_LINGER;

    /**
     * Timeout value on the incoming connection.
     * Note : a value of 0 means no timeout.
     */
    private int connectionTimeout = Constants.DEFAULT_CONNECTION_TIMEOUT;

    /**
     * Timeout value on the incoming connection during request processing.
     * Note : a value of 0 means no timeout.
     */
    private int connectionUploadTimeout = 
        Constants.DEFAULT_CONNECTION_UPLOAD_TIMEOUT;

    /**
     * Timeout value on the server socket.
     * Note : a value of 0 means no timeout.
     */
    private int serverSocketTimeout = Constants.DEFAULT_SERVER_SOCKET_TIMEOUT;

    /**
     * The port number on which we listen for requests.
     */
    private int port = 8080;

    /**
     * The server name to which we should pretend requests to this Connector
     * were directed.  This is useful when operating Tomcat behind a proxy
     * server, so that redirects get constructed accurately.  If not specified,
     * the server name included in the <code>Host</code> header is used.
     */
    private String proxyName = null;

    /**
     * The server port to which we should pretend requests to this Connector
     * were directed.  This is useful when operating Tomcat behind a proxy
     * server, so that redirects get constructed accurately.  If not specified,
     * the port number specified by the <code>port</code> property is used.
     */
    private int proxyPort = 0;

    /**
     * The redirect port for non-SSL to SSL redirects.
     */
    private int redirectPort = 443;

    // BEGIN S1AS 5000999
    /**
     * The default host.
     */
    private String defaultHost;
    // END S1AS 5000999

    /**
     * The request scheme that will be set on all requests received
     * through this connector.
     */
    private String scheme = "http";

    /**
     * The secure connection flag that will be set on all requests received
     * through this connector.
     */
    private boolean secure = false;
    
    // START SJSAS 6439313     
    /**
     * The blocking connection flag that will be set on all requests received
     * through this connector.
     */
    private boolean blocking = false;
    // END SJSAS 6439313     
    
    /** For jk, do tomcat authentication if true, trust server if false 
     */ 
    private boolean tomcatAuthentication = true;



    /**
     * Flag to disable setting a seperate time-out for uploads.
     * If <code>true</code>, then the <code>timeout</code> parameter is
     * ignored.  If <code>false</code>, then the <code>timeout</code>
     * parameter is used to control uploads.
     */
    private boolean disableUploadTimeout = true;

    /**
     * Maximum number of Keep-Alive requests to honor per connection.
     */
    private int maxKeepAliveRequests = 100;

    /**
     * Maximum size of a POST which will be automatically parsed by the 
     * container. 2MB by default.
     */
    private int maxPostSize = 2 * 1024 * 1024;

    /**
     * Maximum size of a POST which will be saved by the container
     * during authentication. 4kB by default
     */
    protected int maxSavePostSize = 4 * 1024;

    /**
     * Has this component been initialized yet?
     */
    protected boolean initialized = false;

    /**
     * Has this component been started yet?
     */
    private boolean started = false;

    /**
     * The shutdown signal to our background thread
     */
    private boolean stopped = false;

    /**
     * The background thread.
     */
    private Thread thread = null;

    /**
     * Use TCP no delay ?
     */
    private boolean tcpNoDelay = true;

    /**
     * Coyote Protocol handler class name.
     * Defaults to the Coyote HTTP/1.1 protocolHandler.
     */
    private String protocolHandlerClassName =
    	"com.sun.enterprise.web.connector.grizzly.CoyoteConnectorLauncher";

    /**
     * Coyote protocol handler.
     */
    private ProtocolHandler protocolHandler = null;

    private String instanceName;

    /**
     * The name of this Connector
     */
    private String name;

    private HttpHandler handler = null;

    /**
     * Mapper.
     */
    protected Mapper mapper;

    /**
     * URI encoding.
     */
    /* GlassFish Issue 2339
    private String uriEncoding = null;
     */
    // START GlassFish Issue 2339
    private String uriEncoding = "UTF-8";
    // END GlassFish Issue 2339

    // START SJSAS 6331392
    private boolean enabled = true;
    // END SJSAS 6331392

    // START S1AS 6188932
    /**
     * Flag indicating whether this connector is receiving its requests from
     * a trusted intermediate server
     */
    protected boolean authPassthroughEnabled = false;

    protected ProxyHandler proxyHandler = null;
    // END S1AS 6188932

    /**
     * The <code>SelectorThread</code> implementation class.
     */
    private String selectorThreadImpl = null;

    private String jvmRoute;
    
    // ------------------------------------------------------------- Properties

    /**
     * Return a configured property.
     */
    public String getProperty(String name) {
        return properties.get(name);
    }

    /**
     * Set a configured property.
     */
    public void setProperty(String name, String value) {
        properties.put(name, value);
    }

    /** 
     * remove a configured property.
     */
    public void removeProperty(String name) {
        properties.remove(name);
    }

    /**
     * Return the <code>Service</code> with which we are associated (if any).
     */
    @Override
    public Service getService() {
        return service;
    }

    /**
     * Set the <code>Service</code> with which we are associated (if any).
     *
     * @param service The service that owns this Engine
     */
    @Override
    public void setService(Service service) {
        this.service = service;
    }

    /**
     * Get the value of compression.
     */
    public String getCompression() {
        return compression;
    }

    /**
     * Set the value of compression.
     *
     * @param compression The new compression value, which can be "on", "off"
     * or "force"
     */
    public void setCompression(String compression) {
        this.compression = compression;
        setProperty("compression", compression);
    }

    /**
     * Return the connection linger for this Connector.
     */
    public int getConnectionLinger() {
        return connectionLinger;
    }

    /**
     * Set the connection linger for this Connector.
     *
     * @param connectionLinger The new connection linger
     */
    public void setConnectionLinger(int connectionLinger) {
        this.connectionLinger = connectionLinger;
        setProperty("soLinger", String.valueOf(connectionLinger));
    }

    /**
     * Return the connection timeout for this Connector.
     */
    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    /**
     * Set the connection timeout for this Connector.
     *
     * @param connectionTimeout The new connection timeout
     */
    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
        setProperty("soTimeout", String.valueOf(connectionTimeout));
    }

    /**
     * Return the connection upload timeout for this Connector.
     */
    public int getConnectionUploadTimeout() {
        return connectionUploadTimeout;
    }

    /**
     * Set the connection upload timeout for this Connector.
     *
     * @param connectionUploadTimeout The new connection upload timeout
     */
    public void setConnectionUploadTimeout(int connectionUploadTimeout) {
        this.connectionUploadTimeout = connectionUploadTimeout;
        setProperty("timeout", String.valueOf(connectionUploadTimeout));
    }

    /**
     * Return the server socket timeout for this Connector.
     */
    public int getServerSocketTimeout() {
        return serverSocketTimeout;
    }

    /**
     * Set the server socket timeout for this Connector.
     *
     * @param serverSocketTimeout The new server socket timeout
     */
    public void setServerSocketTimeout(int serverSocketTimeout) {
        this.serverSocketTimeout = serverSocketTimeout;
        setProperty("serverSoTimeout", String.valueOf(serverSocketTimeout));
    }

    /**
     * Return the accept count for this Connector.
     */
    public int getAcceptCount() {
        return acceptCount;
    }

    /**
     * Set the accept count for this Connector.
     *
     * @param count The new accept count
     */
    public void setAcceptCount(int count) {
        this.acceptCount = count;
        setProperty("backlog", String.valueOf(count));
    }

    /**
     * Return the bind IP address for this Connector.
     */
    public String getAddress() {
        return address;
    }

    /**
     * Set the bind IP address for this Connector.
     *
     * @param address The bind IP address
     */
    public void setAddress(String address) {
        this.address = address;
        setProperty("address", address);
    }

    /**
     * True if the TRACE method is allowed.  Default value is "false".
     */
    public boolean getAllowTrace() {
        return allowTrace;
    }
                                                                           
    /**
     * Set the allowTrace flag, to disable or enable the TRACE HTTP method.
     *
     * @param allowTrace The new allowTrace flag
     */
    public void setAllowTrace(boolean allowTrace) {
        this.allowTrace = allowTrace;
        setProperty("allowTrace", String.valueOf(allowTrace));
    }

    /**
     * Is this connector available for processing requests?
     */
    public boolean isAvailable() {
        return started;
    }

    /**
     * Return the input buffer size for this Connector.
     */
    public int getBufferSize() {
        return bufferSize;
    }

    /**
     * Set the input buffer size for this Connector.
     *
     * @param bufferSize The new input buffer size.
     */
    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
        setProperty("bufferSize", String.valueOf(bufferSize));
    }

    /**
     * Return the Container used for processing requests received by this
     * Connector.
     */
    @Override
    public Container getContainer() {
        return container;
    }

    /**
     * Set the Container used for processing requests received by this
     * Connector.
     *
     * @param container The new Container to use
     */
    @Override
    public void setContainer(Container container) {
        this.container = container;
    }

    /**
     * Return the debugging detail level for this component.
     */
    public int getDebug() {
        return debug;
    }

    /**
     * Set the debugging detail level for this component.
     *
     * @param debug The new debugging detail level
     */
    public void setDebug(int debug) {
        this.debug = debug;
    }

    /**
     * Return the "enable DNS lookups" flag.
     */
    @Override
    public boolean getEnableLookups() {
        return enableLookups;
    }

    /**
     * Set the "enable DNS lookups" flag.
     *
     * @param enableLookups The new "enable DNS lookups" flag value
     */
    @Override
    public void setEnableLookups(boolean enableLookups) {
        this.enableLookups = enableLookups;
        setProperty("enableLookups", String.valueOf(enableLookups));
    }

    /**
     * Return the server socket factory used by this Container.
     */
    @Override
    public ServerSocketFactory getFactory() {
        return factory;
    }

    /**
     * Set the server socket factory used by this Container.
     *
     * @param factory The new server socket factory
     */
    @Override
    public void setFactory(ServerSocketFactory factory) {
        this.factory = factory;
    }

    /**
     * Return descriptive information about this Connector implementation.
     */
    @Override
    public String getInfo() {
        return info;
    }

    /**
     * Return the mapper.
     */
    public Mapper getMapper() {
        return mapper;
    }
     
    /**
     * Set the {@link Mapper}.
     * @param mapper
     */
    public void setMapper(Mapper mapper){
        this.mapper = mapper;
    }     

    /**
     * Return the minimum number of processors to start at initialization.
     */
    public int getMinProcessors() {
        return minProcessors;
    }

    /**
     * Set the minimum number of processors to start at initialization.
     *
     * @param minProcessors The new minimum processors
     */
    public void setMinProcessors(int minProcessors) {
        this.minProcessors = minProcessors;
        setProperty("minThreads", String.valueOf(minProcessors));
    }

    /**
     * Return the maximum number of processors allowed, or <0 for unlimited.
     */
    public int getMaxProcessors() {
        return maxProcessors;
    }

    /**
     * Set the maximum number of processors allowed, or <0 for unlimited.
     *
     * @param maxProcessors The new maximum processors
     */
    public void setMaxProcessors(int maxProcessors) {
        this.maxProcessors = maxProcessors;
        setProperty("maxThreads", String.valueOf(maxProcessors));
    }

    /**
     * Return the maximum size of a POST which will be automatically
     * parsed by the container.
     */
    public int getMaxPostSize() {
        return maxPostSize;
    }

    /**
     * Set the maximum size of a POST which will be automatically
     * parsed by the container.
     *
     * @param maxPostSize The new maximum size in bytes of a POST which will 
     * be automatically parsed by the container
     */
    public void setMaxPostSize(int maxPostSize) {
        this.maxPostSize = maxPostSize;
        setProperty("maxPostSize", String.valueOf(maxPostSize));
    }

    /**
     * Return the maximum size of a POST which will be saved by the container
     * during authentication.
     */
    public int getMaxSavePostSize() {

        return (maxSavePostSize);

    }

    /**
     * Set the maximum size of a POST which will be saved by the container
     * during authentication.
     *
     * @param maxSavePostSize The new maximum size in bytes of a POST which will
     * be saved by the container during authentication.
     */
    public void setMaxSavePostSize(int maxSavePostSize) {

        this.maxSavePostSize = maxSavePostSize;
        setProperty("maxSavePostSize", String.valueOf(maxSavePostSize));
    }

    /**
     * Return the port number on which we listen for requests.
     */
    public int getPort() {
        return port;
    }

    /**
     * Set the port number on which we listen for requests.
     *
     * @param port The new port number
     */
    public void setPort(int port) {
        this.port = port;
        setProperty("port", String.valueOf(port));
    }

    /**
     * Sets the name of this Connector.
     */
    public void setName(String name){
        this.name = name;
    }
    
    /**
     * Gets the name of this Connector.
     */
    @Override
    public String getName(){
        return name;
    }

    /**
     * Sets the instance name for this Connector.
     * 
     * @param instanceName the instance name
     */
    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    public String getInstanceName() {
        return instanceName;
    }

    /**
     * Return the Coyote protocol handler in use.
     */
    public String getProtocol() {
        if ("org.glassfish.grizzly.tcp.http11.Http11Protocol".equals
            (getProtocolHandlerClassName())) {
            return "HTTP/1.1";
        } else if ("org.apache.jk.server.JkCoyoteHandler".equals
                   (getProtocolHandlerClassName())) {
            return "AJP/1.3";
        }
        return null;
    }

    /**
     * Set the Coyote protocol which will be used by the connector.
     *
     * @param protocol The Coyote protocol name
     */
    public void setProtocol(String protocol) {
        if (protocol.equals("HTTP/1.1")) {
            setProtocolHandlerClassName
                ("org.glassfish.grizzly.tcp.http11.Http11Protocol");
        } else if (protocol.equals("AJP/1.3")) {
            setProtocolHandlerClassName
                ("org.apache.jk.server.JkCoyoteHandler");
        } else {
            setProtocolHandlerClassName(null);
        }
    }

    /**
     * Return the class name of the Coyote protocol handler in use.
     */
    public String getProtocolHandlerClassName() {
        return protocolHandlerClassName;
    }

    /**
     * Set the class name of the Coyote protocol handler which will be used
     * by the connector.
     *
     * @param protocolHandlerClassName The new class name
     */
    public void setProtocolHandlerClassName(String protocolHandlerClassName) {
        this.protocolHandlerClassName = protocolHandlerClassName;
    }

    /**
     * Return the protocol handler associated with the connector.
     */
    public ProtocolHandler getProtocolHandler() {
        return protocolHandler;
    }

    /**
     * Return the proxy server name for this Connector.
     */
    public String getProxyName() {
        return proxyName;
    }

    /**
     * Set the proxy server name for this Connector.
     *
     * @param proxyName The new proxy server name
     */
    public void setProxyName(String proxyName) {
        if(proxyName != null && proxyName.length() > 0) {
            this.proxyName = proxyName;
            setProperty("proxyName", proxyName);
        } else {
            this.proxyName = null;
            removeProperty("proxyName");
        }
    }

    /**
     * Return the proxy server port for this Connector.
     */
    public int getProxyPort() {
        return proxyPort;
    }

    /**
     * Set the proxy server port for this Connector.
     *
     * @param proxyPort The new proxy server port
     */
    public void setProxyPort(int proxyPort) {
        this.proxyPort = proxyPort;
        setProperty("proxyPort", String.valueOf(proxyPort));
    }

    /**
     * Return the port number to which a request should be redirected if
     * it comes in on a non-SSL port and is subject to a security constraint
     * with a transport guarantee that requires SSL.
     */
    @Override
    public int getRedirectPort() {
        return redirectPort;
    }

    /**
     * Set the redirect port number.
     *
     * @param redirectPort The redirect port number (non-SSL to SSL)
     */
    @Override
    public void setRedirectPort(int redirectPort) {
        this.redirectPort = redirectPort;
        setProperty("redirectPort", String.valueOf(redirectPort));
    }

    /**
     * Return the flag that specifies upload time-out behavior.
     */
    public boolean getDisableUploadTimeout() {
        return disableUploadTimeout;
    }

    /**
     * Set the flag to specify upload time-out behavior.
     *
     * @param isDisabled If <code>true</code>, then the <code>timeout</code>
     * parameter is ignored.  If <code>false</code>, then the
     * <code>timeout</code> parameter is used to control uploads.
     */
    public void setDisableUploadTimeout( boolean isDisabled ) {
        disableUploadTimeout = isDisabled;
        setProperty("disableUploadTimeout", String.valueOf(isDisabled));
    }

    /**
      * Return the maximum HTTP header size.
      */
    public int getMaxHttpHeaderSize() {
      return maxHttpHeaderSize;
    }
  
    /**
     * Set the maximum HTTP header size.
     */
    public void setMaxHttpHeaderSize(int size) {
        maxHttpHeaderSize = size;
        setProperty("maxHttpHeaderSize", String.valueOf(size));
    }

    /**
     * Return the Keep-Alive policy for the connection.
     */
    public boolean getKeepAlive() {
        return ((maxKeepAliveRequests != 0) && (maxKeepAliveRequests != 1));
    }

    /**
     * Set the keep-alive policy for this connection.
     */
    public void setKeepAlive(boolean keepAlive) {
        if (!keepAlive) {
            setMaxKeepAliveRequests(1);
        }
    }

    /**
     * Return the maximum number of Keep-Alive requests to honor 
     * per connection.
     */
    public int getMaxKeepAliveRequests() {
        return maxKeepAliveRequests;
    }

    /**
     * Set the maximum number of Keep-Alive requests to honor per connection.
     */
    public void setMaxKeepAliveRequests(int mkar) {
        maxKeepAliveRequests = mkar;
        setProperty("maxKeepAliveRequests", String.valueOf(mkar));
    }

    /**
     * Return the scheme that will be assigned to requests received
     * through this connector.  Default value is "http".
     */
    @Override
    public String getScheme() {
        return scheme;
    }

    /**
     * Set the scheme that will be assigned to requests received through
     * this connector.
     *
     * @param scheme The new scheme
     */
    @Override
    public void setScheme(String scheme) {
        this.scheme = scheme;
        setProperty("scheme", scheme);
    }

    /**
     * Return the secure connection flag that will be assigned to requests
     * received through this connector.  Default value is "false".
     */
    @Override
    public boolean getSecure() {
        return secure;
    }

    /**
     * Set the secure connection flag that will be assigned to requests
     * received through this connector.
     *
     * @param secure The new secure connection flag
     */
    @Override
    public void setSecure(boolean secure) {
        this.secure = secure;
        setProperty("secure", String.valueOf(secure));
    }

    // START SJSAS 6439313     
    /**
     * Return the blocking connection flag that will be assigned to requests
     * received through this connector.  Default value is "false".
     */
    public boolean getBlocking() {
        return blocking;
    }

    /**
     * Set the blocking connection flag that will be assigned to requests
     * received through this connector.
     *
     * @param blocking The new blocking connection flag
     */
    public void setBlocking(boolean blocking) {
        this.blocking = blocking;
        setProperty("blocking", String.valueOf(blocking));
    }
    // END SJSAS 6439313     
    
    public boolean getTomcatAuthentication() {
        return tomcatAuthentication;
    }

    public void setTomcatAuthentication(boolean tomcatAuthentication) {
        this.tomcatAuthentication = tomcatAuthentication;
        setProperty("tomcatAuthentication", String.valueOf(tomcatAuthentication));
    }
    
    /**
     * Return the TCP no delay flag value.
     */
    public boolean getTcpNoDelay() {
        return tcpNoDelay;
    }

    /**
     * Set the TCP no delay flag which will be set on the socket after
     * accepting a connection.
     *
     * @param tcpNoDelay The new TCP no delay flag
     */
    public void setTcpNoDelay(boolean tcpNoDelay) {
        this.tcpNoDelay = tcpNoDelay;
        setProperty("tcpNoDelay", String.valueOf(tcpNoDelay));
    }

    /**
     * Return the character encoding to be used for the URI.
     */
    @Override
    public String getURIEncoding() {
        return uriEncoding;
    }

    /**
     * Set the URI encoding to be used for the URI.
     *
     * @param uriEncoding The new URI character encoding.
     */
    @Override
    public void setURIEncoding(String uriEncoding) {
    	if (Charset.isSupported(uriEncoding)) {
        this.uriEncoding = uriEncoding;
        setProperty("uRIEncoding", uriEncoding);
    	} else {
			if (log.isLoggable(Level.WARNING)) {
				log.log(Level.WARNING, uriEncoding
						+ "is not supported .Setting default URLEncoding as "
						+ this.uriEncoding);
			}
		}
    }

    /**
     * Indicates whether the generation of an X-Powered-By response header for
     * servlet-generated responses is enabled or disabled for this Connector.
     *
     * @return true if generation of X-Powered-By response header is enabled,
     * false otherwise
     */
    public boolean isXpoweredBy() {
        return xpoweredBy;
    }

    /**
     * Enables or disables the generation of an X-Powered-By header (with value
     * Servlet/2.4) for all servlet-generated responses returned by this
     * Connector.
     *
     * @param xpoweredBy true if generation of X-Powered-By response header is
     * to be enabled, false otherwise
     */
    public void setXpoweredBy(boolean xpoweredBy) {
        this.xpoweredBy = xpoweredBy;
        setProperty("xpoweredBy", String.valueOf(xpoweredBy));
    }

    // BEGIN S1AS 5000999
    /**
     * Sets the default host for this Connector.
     *
     * @param defaultHost The default host for this Connector
     */
    @Override
    public void setDefaultHost(String defaultHost) {
        this.defaultHost = defaultHost;
    }

    /**
     * Gets the default host of this Connector.
     *
     * @return The default host of this Connector
     */
    @Override
    public String getDefaultHost() {
        return defaultHost;
    }
    // END S1AS 5000999

    // START S1AS 6188932
    /**
     * Returns the value of this connector's authPassthroughEnabled flag.
     *
     * @return true if this connector is receiving its requests from
     * a trusted intermediate server, false otherwise
     */
    @Override
    public boolean getAuthPassthroughEnabled() {
        return authPassthroughEnabled;
    }

    /**
     * Sets the value of this connector's authPassthroughEnabled flag.
     *
     * @param authPassthroughEnabled true if this connector is receiving its
     * requests from a trusted intermediate server, false otherwise
     */
    @Override
    public void setAuthPassthroughEnabled(boolean authPassthroughEnabled) {
        this.authPassthroughEnabled = authPassthroughEnabled;
    }

    /**
     * Gets the ProxyHandler instance associated with this CoyoteConnector.
     * 
     * @return ProxyHandler instance associated with this CoyoteConnector,
     * or null
     */
    @Override
    public ProxyHandler getProxyHandler() {
        return proxyHandler;
    }

    /**
     * Sets the ProxyHandler implementation for this CoyoteConnector to use.
     * 
     * @param proxyHandler ProxyHandler instance to use
     */
    @Override
    public void setProxyHandler(ProxyHandler proxyHandler) {
        this.proxyHandler = proxyHandler;
    }

    // END S1AS 6188932

    // START SJSAS 6331392
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }
    // END SJSAS 6331392

    public void setJvmRoute(String jvmRoute) {
        this.jvmRoute = jvmRoute;
    }

    public String getJvmRoute() {
        return jvmRoute;
    }

    // --------------------------------------------------------- Public Methods

    /**
     * Create (or allocate) and return a Request object suitable for
     * specifying the contents of a Request to the responsible Container.
     */
    @Override
    public org.apache.catalina.Request createRequest() {
        Request request = new Request();
        request.setConnector(this);
        return request;
    }

    /**
     * Create (or allocate) and return a Response object suitable for
     * receiving the contents of a Response from the responsible Container.
     */
    @Override
    public org.apache.catalina.Response createResponse() {
        Response response = new Response();
        response.setConnector(this);
        return response;
    }


    // -------------------------------------------------- Monitoring Methods

    /**
     * Fires probe event related to the fact that the given request has
     * been entered the web container.
     *
     * @param request the request object
     * @param host the virtual server to which the request was mapped
     * @param context the Context to which the request was mapped
     */
    public void requestStartEvent(HttpServletRequest request, Host host,
            Context context) {
        // Deliberate noop
    };

    /**
     * Fires probe event related to the fact that the given request is about
     * to exit from the web container.
     *
     * @param request the request object
     * @param host the virtual server to which the request was mapped
     * @param context the Context to which the request was mapped
     * @param statusCode the response status code
     */
    public void requestEndEvent(HttpServletRequest request, Host host,
            Context context, int statusCode) {
        // Deliberate noop
    };


    // ------------------------------------------------------ Lifecycle Methods

    /**
     * Add a lifecycle event listener to this component.
     *
     * @param listener The listener to add
     */
    @Override
    public void addLifecycleListener(LifecycleListener listener) {
        lifecycle.addLifecycleListener(listener);
    }

    /**
     * Gets the (possibly empty) list of lifecycle listeners
     * associated with this Connector.
     */
    @Override
    public List<LifecycleListener> findLifecycleListeners() {
        return lifecycle.findLifecycleListeners();
    }

    /**
     * Remove a lifecycle event listener from this component.
     *
     * @param listener The listener to add
     */
    @Override
    public void removeLifecycleListener(LifecycleListener listener) {
        lifecycle.removeLifecycleListener(listener);
    }

    protected ObjectName createObjectName(String domain, String type)
            throws MalformedObjectNameException {
        String encodedAddr = null;
        if (getAddress() != null) {
            encodedAddr = URLEncoder.encode(getProperty("address"));
        }
        String addSuffix = (getAddress() == null) ? "" : ",address="
                + encodedAddr;
        ObjectName _oname = new ObjectName(domain + ":type=" + type + ",port="
                + getPort() + addSuffix);
        return _oname;
    }

    /**
     * Initialize this connector (create ServerSocket here!)
     */
    @Override
    public void initialize()
        throws LifecycleException
    {
        if (initialized) {
            if (log.isLoggable(Level.INFO)) {
                log.log(Level.INFO, CONNECTOR_BEEN_INIT);
            }
            return;
        }

        this.initialized = true;
                
        // If the Mapper is null, do not fail and creates one by default. 
        if (mapper == null){
            mapper = new Mapper();
        }
        
        if( oname == null && (container instanceof StandardEngine)) {
            try {
                // we are loaded directly, via API - and no name was given to us
                StandardEngine cb=(StandardEngine)container;
                oname = createObjectName(domain, "Connector");
                controller=oname;
            } catch (Exception e) {
                log.log(Level.SEVERE, ERROR_REGISTER_CONNECTOR_EXCEPTION, e);
            }
            if (log.isLoggable(Level.FINE)) {
                log.log(Level.FINE, "Creating name for connector " + oname);
            }
        }
        

        //START SJSAS 6363251
        // Initializa handler
        //handler = new CoyoteAdapter(this);
        //END SJSAS 6363251
        // Instantiate Adapter
        //START SJSAS 6363251
        if ( handler == null){
            try {
                Class<?> clazz = Class.forName(defaultClassName);
                Constructor constructor = 
                        clazz.getConstructor(new Class<?>[]{Connector.class});
                handler =
                        (HttpHandler)constructor.newInstance(new Object[]{this});
            } catch (Exception e) {
                throw new LifecycleException
                    (rb.getString(FAILED_INSTANCIATE_HTTP_HANDLER_EXCEPTION), e);
            } 
        }
        //END SJSAS 6363251

        // Instantiate protocol handler
        if ( protocolHandler == null ) {
            try {
                Class<?> clazz = Class.forName(protocolHandlerClassName);

                // use no-arg constructor for JkCoyoteHandler
                if (protocolHandlerClassName.equals("org.apache.jk.server.JkCoyoteHandler")) {
                    protocolHandler = (ProtocolHandler) clazz.newInstance();
                    if (handler instanceof CoyoteAdapter){
                        ((CoyoteAdapter) handler).setCompatWithTomcat(true);
                    } else {
                        String msg = MessageFormat.format(rb.getString(INVALID_ADAPTER_IMPLEMENTATION_EXCEPTION),
                                                          handler);
                        throw new IllegalStateException
                          (msg);

                    }
                // START SJSAS 6439313
                } else {
                    Constructor constructor = 
                            clazz.getConstructor(new Class<?>[]{Boolean.TYPE,
                                                             Boolean.TYPE,
                                                             String.class});

                    protocolHandler = (ProtocolHandler) 
                        constructor.newInstance(secure, blocking,
                                                selectorThreadImpl);
                // END SJSAS 6439313
                }
            } catch (Exception e) {
                String msg = MessageFormat.format(rb.getString(PROTOCOL_HANDLER_INIT_FAILED_EXCEPTION), e);
                throw new LifecycleException
                    (msg);
            }
        }

        protocolHandler.setHandler(handler);

        IntrospectionUtils.setProperty(protocolHandler, "jkHome",
                                       System.getProperty("catalina.base"));

        // Configure secure socket factory
        // XXX For backwards compatibility only.
        if (factory instanceof CoyoteServerSocketFactory) {
            IntrospectionUtils.setProperty(protocolHandler, "secure",
                                           "" + true);
            CoyoteServerSocketFactory ssf =
                (CoyoteServerSocketFactory) factory;
            IntrospectionUtils.setProperty(protocolHandler, "algorithm",
                                           ssf.getAlgorithm());
            if (ssf.getClientAuth()) {
                IntrospectionUtils.setProperty(protocolHandler, "clientauth",
                                               "" + ssf.getClientAuth());
            }
            IntrospectionUtils.setProperty(protocolHandler, "keystore",
                                           ssf.getKeystoreFile());
            IntrospectionUtils.setProperty(protocolHandler, "randomfile",
                                           ssf.getRandomFile());
            IntrospectionUtils.setProperty(protocolHandler, "rootfile",
                                           ssf.getRootFile());

            IntrospectionUtils.setProperty(protocolHandler, "keypass",
                                           ssf.getKeystorePass());
            IntrospectionUtils.setProperty(protocolHandler, "keytype",
                                           ssf.getKeystoreType());
            IntrospectionUtils.setProperty(protocolHandler, "protocol",
                                           ssf.getProtocol());
            IntrospectionUtils.setProperty(protocolHandler, "protocols",
                                           ssf.getProtocols());
            IntrospectionUtils.setProperty(protocolHandler,
                                           "sSLImplementation",
                                           ssf.getSSLImplementation());
            IntrospectionUtils.setProperty(protocolHandler, "ciphers",
                                           ssf.getCiphers());
            IntrospectionUtils.setProperty(protocolHandler, "keyAlias",
                                           ssf.getKeyAlias());
        } else {
            IntrospectionUtils.setProperty(protocolHandler, "secure",
                                           "" + secure);
        }

        /* Set the configured properties.  This only sets the ones that were
         * explicitly configured.  Default values are the responsibility of
         * the protocolHandler.
         */
        Iterator<String> keys = properties.keySet().iterator();
        while( keys.hasNext() ) {
            String name = keys.next();
            String value = properties.get(name);
	    String trnName = translateAttributeName(name);
            IntrospectionUtils.setProperty(protocolHandler, trnName, value);
        }


        try {
            protocolHandler.init();
        } catch (Exception e) {
            String msg = MessageFormat.format(rb.getString(PROTOCOL_HANDLER_INIT_FAILED_EXCEPTION), e);
            throw new LifecycleException
                (msg);
        }
    }

    /*
     * Translate the attribute name from the legacy Factory names to their
     * internal protocol names.
     */
    private String translateAttributeName(String name) {
	if ("clientAuth".equals(name)) {
	    return "clientauth";
	} else if ("keystoreFile".equals(name)) {
	    return "keystore";
	} else if ("randomFile".equals(name)) {
	    return "randomfile";
	} else if ("rootFile".equals(name)) {
	    return "rootfile";
	} else if ("keystorePass".equals(name)) {
	    return "keypass";
	} else if ("keystoreType".equals(name)) {
	    return "keytype";
	} else if ("sslProtocol".equals(name)) {
	    return "protocol";
	} else if ("sslProtocols".equals(name)) {
	    return "protocols";
	}
	return name;
    }

    /**
     * Begin processing requests via this Connector.
     *
     * @exception LifecycleException if a fatal startup error occurs
     */
    @Override
    public void start() throws LifecycleException {
        if( !initialized )
            initialize();

        // Validate and update our current state
        if (started) {
            if (log.isLoggable(Level.INFO)) {
                log.log(Level.INFO, CONNECTOR_BEEN_STARTED);
            }
            return;
        }
        lifecycle.fireLifecycleEvent(START_EVENT, null);
        started = true;

        try {
            protocolHandler.start();
        } catch (Exception e) {
            String msg = MessageFormat.format(rb.getString(PROTOCOL_HANDLER_START_FAILED_EXCEPTION), e);
            throw new LifecycleException
                (msg);
        }

    }

    /**
     * Terminate processing requests via this Connector.
     *
     * @exception LifecycleException if a fatal shutdown error occurs
     */
    @Override
    public void stop() throws LifecycleException {

        // Validate and update our current state
        if (!started) {
            log.log(Level.SEVERE, CONNECTOR_NOT_BEEN_STARTED);
            return;

        }
        lifecycle.fireLifecycleEvent(STOP_EVENT, null);
        started = false;

        try {
            protocolHandler.destroy();
        } catch (Exception e) {
            String msg = MessageFormat.format(rb.getString(PROTOCOL_HANDLER_DESTROY_FAILED_EXCEPTION), e);
            throw new LifecycleException
                (msg);
        }

    }


    // -------------------- Management methods --------------------

    public boolean getClientAuth() {
        boolean ret = false;

        String prop = getProperty("clientauth");
        if (prop != null) {
            ret = Boolean.valueOf(prop).booleanValue();
        } else {	
            ServerSocketFactory factory = this.getFactory();
            if (factory instanceof CoyoteServerSocketFactory) {
                ret = ((CoyoteServerSocketFactory)factory).getClientAuth();
            }
        }

        return ret;
    }

    public void setClientAuth(boolean clientAuth) {
        setProperty("clientauth", String.valueOf(clientAuth));
        ServerSocketFactory factory = this.getFactory();
        if (factory instanceof CoyoteServerSocketFactory) {
            ((CoyoteServerSocketFactory)factory).setClientAuth(clientAuth);
        }
    }

    public String getKeystoreFile() {
        String ret = getProperty("keystore");
        if (ret == null) {
            ServerSocketFactory factory = this.getFactory();
            if (factory instanceof CoyoteServerSocketFactory) {
                ret = ((CoyoteServerSocketFactory)factory).getKeystoreFile();
            }
        }

        return ret;
    }

    public void setKeystoreFile(String keystoreFile) {
        setProperty("keystore", keystoreFile);
        if (factory instanceof CoyoteServerSocketFactory) {
            ((CoyoteServerSocketFactory)factory).setKeystoreFile(keystoreFile);
        }
    }

    /**
     * Return keystorePass
     */
    public String getKeystorePass() {
        String ret = getProperty("keypass");
        if (ret == null) {
            if (factory instanceof CoyoteServerSocketFactory ) {
                return ((CoyoteServerSocketFactory)factory).getKeystorePass();
            }
        }

        return ret;
    }

    /**
     * Set keystorePass
     */
    public void setKeystorePass(String keystorePass) {
        setProperty("keypass", keystorePass);
        ServerSocketFactory factory = getFactory();
        if( factory instanceof CoyoteServerSocketFactory ) {
            ((CoyoteServerSocketFactory)factory).setKeystorePass(keystorePass);
        }
    }

    /**
     * Gets the list of SSL cipher suites that are to be enabled
     *
     * @return Comma-separated list of SSL cipher suites, or null if all
     * cipher suites supported by the underlying SSL implementation are being
     * enabled
     */
    public String getCiphers() {
        String ret = getProperty("ciphers");
        if (ret == null) {
            ServerSocketFactory factory = getFactory();
            if (factory instanceof CoyoteServerSocketFactory) {
                ret = ((CoyoteServerSocketFactory)factory).getCiphers();
            }
        }

        return ret;
    }

    /**
     * Sets the SSL cipher suites that are to be enabled.
     *
     * Only those SSL cipher suites that are actually supported by
     * the underlying SSL implementation will be enabled.
     *
     * @param ciphers Comma-separated list of SSL cipher suites
     */
    public void setCiphers(String ciphers) {
        setProperty("ciphers", ciphers);
        ServerSocketFactory factory = getFactory();
        if (factory instanceof CoyoteServerSocketFactory) {
            ((CoyoteServerSocketFactory)factory).setCiphers(ciphers);
        }
    }

    /**
     * Sets the number of seconds after which SSL sessions expire and are
     * removed from the SSL sessions cache.
     */
    public void setSslSessionTimeout(String timeout) {
        setProperty("sslSessionTimeout", timeout);
    }

    public String getSslSessionTimeout() {
        return getProperty("sslSessionTimeout");
    }

    /**
     * Sets the number of seconds after which SSL3 sessions expire and are
     * removed from the SSL sessions cache.
     */
    public void setSsl3SessionTimeout(String timeout) {
        setProperty("ssl3SessionTimeout", timeout);
    }

    public String getSsl3SessionTimeout() {
        return getProperty("ssl3SessionTimeout");
    }

    /**
     * Sets the number of SSL sessions that may be cached
     */
    public void setSslSessionCacheSize(String cacheSize) {
        setProperty("sslSessionCacheSize", cacheSize);
    }

    public String getSslSessionCacheSize() {
        return getProperty("sslSessionCacheSize");
    }

    /**
     * Gets the alias name of the keypair and supporting certificate chain
     * used by this Connector to authenticate itself to SSL clients.
     *
     * @return The alias name of the keypair and supporting certificate chain
     */
    public String getKeyAlias() {
        String ret = getProperty("keyAlias");
        if (ret == null) {
            ServerSocketFactory factory = getFactory();
            if (factory instanceof CoyoteServerSocketFactory) {
                ret = ((CoyoteServerSocketFactory)factory).getKeyAlias();
            }
        }

        return ret;
    }

    /**
     * Sets the alias name of the keypair and supporting certificate chain
     * used by this Connector to authenticate itself to SSL clients.
     *
     * @param alias The alias name of the keypair and supporting certificate
     * chain
     */
    public void setKeyAlias(String alias) {
        setProperty("keyAlias", alias);
        ServerSocketFactory factory = getFactory();
        if (factory instanceof CoyoteServerSocketFactory) {
            ((CoyoteServerSocketFactory)factory).setKeyAlias(alias);
        }
    }

    /**
     * Gets the SSL protocol variant to be used.
     *
     * @return SSL protocol variant
     */
    public String getSslProtocol() {
        String ret = getProperty("sslProtocol");
        if (ret == null) {
            ServerSocketFactory factory = getFactory();
            if (factory instanceof CoyoteServerSocketFactory) {
                ret = ((CoyoteServerSocketFactory)factory).getProtocol();
            }
        }

        return ret;
    }

    /**
     * Sets the SSL protocol variant to be used.
     *
     * @param sslProtocol SSL protocol variant
     */
    public void setSslProtocol(String sslProtocol) {
        setProperty("sslProtocol", sslProtocol);
        ServerSocketFactory factory = getFactory();
        if (factory instanceof CoyoteServerSocketFactory) {
            ((CoyoteServerSocketFactory)factory).setProtocol(sslProtocol);
        }
    }

    /**
     * Gets the SSL protocol variants to be enabled.
     *
     * @return Comma-separated list of SSL protocol variants
     */
    public String getSslProtocols() {
        String ret = getProperty("sslProtocols");
        if (ret == null) {
            ServerSocketFactory factory = getFactory();
            if (factory instanceof CoyoteServerSocketFactory) {
                ret = ((CoyoteServerSocketFactory)factory).getProtocols();
            }
        }

        return ret;
    }

    /**
     * Sets the SSL protocol variants to be enabled.
     *
     * @param sslProtocols Comma-separated list of SSL protocol variants
     */
    public void setSslProtocols(String sslProtocols) {
        setProperty("sslProtocols", sslProtocols);
        ServerSocketFactory factory = getFactory();
        if (factory instanceof CoyoteServerSocketFactory) {
            ((CoyoteServerSocketFactory)factory).setProtocols(sslProtocols);
        }
    }
    
    // START OF SJSAS 8.1 PE 6191830
    /**
     * Get the underlying WebContainer certificate for the request
     */
    @Override
    public X509Certificate[] getCertificates(org.apache.catalina.Request request) {
        
        Request cRequest = null;
        if (request instanceof Request) {
            cRequest=(Request) request;
        } else {
            return null;
        }
        
        X509Certificate certs[] = (X509Certificate[])
        cRequest.getAttribute(Globals.CERTIFICATES_ATTR);
        if ((certs == null) || (certs.length < 1)) {
            certs = (X509Certificate[])
            cRequest.getAttribute(Globals.SSL_CERTIFICATE_ATTR);
        }
        return certs;
    }    
    // END OF SJSAS 8.1 PE 6191830


    // -------------------- JMX registration  --------------------

    protected String domain;
    protected ObjectName oname;
    ObjectName controller;

    public ObjectName getController() {
        return controller;
    }

    public void setController(ObjectName controller) {
        this.controller = controller;
    }

    public ObjectName getObjectName() {
        return oname;
    }

    public String getDomain() {
        return domain;
    }

    /**
     * Set the domain of this object.
     */
    public void setDomain(String domain){
        this.domain = domain;
    }

    public void init() throws Exception {

        if( this.getService() != null ) {
            if (log.isLoggable(Level.FINE)) {
                log.log(Level.FINE, "Already configured");
            }
            return;
        }
    }

    public void destroy() throws Exception {
        if( oname!=null && controller==oname ) {
            if (log.isLoggable(Level.FINE)) {
                log.log(Level.FINE, "Unregister itself " + oname );
            }
        }
        if( getService() == null)
            return;
        getService().removeConnector(this);
    }

    // START SJSAS 6363251
    /**
     * Set the <code>Adapter</code> used by this connector.
     */
    @Override
    public void setHandler(HttpHandler handler){
        this.handler = handler;
    }
    
    /**
     * Get the <code>Adapter</code> used by this connector.
     */    
    @Override
    public HttpHandler getHandler(){
        return handler;
    }
 
    /**
     * Set the <code>ProtocolHandler</code> used by this connector.
     */
    public void setProtocolHandler(ProtocolHandler protocolHandler){
        this.protocolHandler = protocolHandler;
    }
    // END SJSAS 6363251

    /**
     * Get the underlying <code>SelectorThread</code> implementation, null if 
     * the default is used.
     */
    public String getSelectorThreadImpl() {
        return selectorThreadImpl;
    }

    /**
     * Set the underlying <code>SelectorThread</code> implementation  
     */   
    public void setSelectorThreadImpl(String selectorThreadImpl) {
        this.selectorThreadImpl = selectorThreadImpl;
    } 
}
