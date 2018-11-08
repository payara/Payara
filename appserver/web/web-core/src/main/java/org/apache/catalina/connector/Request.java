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
//Portions Copyright [2016-2018] [Payara Foundation]

package org.apache.catalina.connector;

import com.sun.appserv.ProxyHandler;
import org.apache.catalina.*;
import org.apache.catalina.authenticator.AuthenticatorBase;
import org.apache.catalina.authenticator.SingleSignOn;
import org.apache.catalina.core.ApplicationPushBuilder;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.core.StandardWrapper;
import org.apache.catalina.fileupload.Multipart;
import org.apache.catalina.security.SecurityUtil;
import org.apache.catalina.session.PersistentManagerBase;
import org.apache.catalina.session.StandardSession;
import org.apache.catalina.util.Enumerator;
import org.apache.catalina.util.ParameterMap;
import org.apache.catalina.util.RequestUtil;
import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.CompletionHandler;
import org.glassfish.grizzly.EmptyCompletionHandler;
import org.glassfish.grizzly.http.server.Response.SuspendedContextImpl;
import org.glassfish.grizzly.http.server.TimeoutHandler;
import org.glassfish.grizzly.http.server.util.MappingData;
import org.glassfish.grizzly.http.server.util.RequestUtils;
import org.glassfish.grizzly.http.util.*;
import org.glassfish.grizzly.http2.Http2Stream;
import org.glassfish.grizzly.memory.Buffers;
import org.glassfish.grizzly.utils.Charsets;
import org.glassfish.web.valve.GlassFishValve;

import javax.security.auth.Subject;
import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.nio.charset.UnsupportedCharsetException;
import java.security.*;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.Integer.parseInt;

/**
 * Wrapper object for the Coyote request.
 *
 * @author Remy Maucherat
 * @author Craig R. McClanahan
 * @author Rajiv Mordani
 * @version $Revision: 1.67.2.9 $ $Date: 2008/04/17 18:37:34 $
 */
public class Request
        implements HttpRequest, HttpServletRequest {

    private static final Logger log = LogFacade.getLogger();
    private static final ResourceBundle rb = log.getResourceBundle();

    // ----------------------------------------------------------- Statics
    /**
     * Descriptive information about this Request implementation.
     */
    protected static final String info =
            "org.apache.catalina.connector.Request/1.0";

    /**
     * Whether or not to enforce scope checking of this object.
     */
    private static boolean enforceScope = false;

    // END CR 6309511
    // START OF SJSAS 6231069
    /**
     * The set of SimpleDateFormat formats to use in getDateHeader().
     *
     * Notice that because SimpleDateFormat is not thread-safe, we can't
     * declare formats[] as a static variable.
     */
    private static ThreadLocal staticDateFormats = new ThreadLocal() {

        @Override
        protected Object initialValue() {
            SimpleDateFormat[] f = new SimpleDateFormat[3];
            f[0] = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz",
                    Locale.US);
            f[1] = new SimpleDateFormat("EEEEEE, dd-MMM-yy HH:mm:ss zzz",
                    Locale.US);
            f[2] = new SimpleDateFormat("EEE MMMM d HH:mm:ss yyyy", Locale.US);
            return f;
        }
    };

    protected SimpleDateFormat formats[];
    // END OF SJSAS 6231069

    /**
     * ThreadLocal object to keep track of the reentrancy status of each thread.
     * It contains a byte[] object whose single element is either 0 (initial
     * value or no reentrancy), or 1 (current thread is reentrant). When a
     * thread exits the implies method, byte[0] is alwasy reset to 0.
     */
    private static ThreadLocal reentrancyStatus;

    static {
        reentrancyStatus = new ThreadLocal() {

            @Override
            protected synchronized Object initialValue() {
                return new byte[]{0};
            }
        };
    }
    // ----------------------------------------------------- Instance Variables


    /**
     * The set of cookies associated with this Request.
     */
    protected ArrayList<Cookie> cookies = new ArrayList<Cookie>();
    /**
     * The default Locale if none are specified.
     */
    protected static final Locale defaultLocale = Locale.getDefault();
    /**
     * The attributes associated with this Request, keyed by attribute name.
     */
    protected Map<String, Object> attributes = new HashMap<String, Object>();
    /**
     * The preferred Locales associated with this Request.
     */
    protected ArrayList<Locale> locales = new ArrayList<Locale>();
    /**
     * Internal notes associated with this request by Catalina components
     * and event listeners.
     */
    private Map<String, Object> notes = new HashMap<String, Object>();
    /**
     * Authentication type.
     */
    protected String authType = null;
    /**
     * The current dispatcher type.
     */
    protected Object dispatcherTypeAttr = null;
    /**
     * The associated input buffer.
     */
    protected InputBuffer inputBuffer = new InputBuffer();
    /**
     * ServletInputStream.
     */
    protected CoyoteInputStream inputStream =
            new CoyoteInputStream(inputBuffer);
    /**
     * Reader.
     */
    protected CoyoteReader reader = new CoyoteReader(inputBuffer);
    /**
     * Using stream flag.
     */
    protected boolean usingInputStream = false;
    /**
     * Using writer flag.
     */
    protected boolean usingReader = false;
    /**
     * User principal.
     */
    protected Principal userPrincipal = null;
    /**
     * Session parsed flag.
     */
    protected boolean sessionParsed = false;

    protected boolean parametersProcessed = false;
    /**
     * Cookies parsed flag.
     */
    protected boolean cookiesParsed = false;
    /**
     * Secure flag.
     */
    protected boolean secure = false;
    /**
     * The Subject associated with the current AccessControllerContext
     */
    protected Subject subject = null;
    /**
     * Post data buffer.
     */
    protected static final int CACHED_POST_LEN = 8192;
    protected byte[] postData = null;
    /**
     * Hash map used in the getParametersMap method.
     */
    protected ParameterMap<String, String[]> parameterMap = new ParameterMap<String, String[]>();
    /**
     * The currently active session for this request.
     */
    protected Session session = null;
    /**
     * The current request dispatcher path.
     */
    protected Object requestDispatcherPath = null;
    /**
     * Was the requested session ID received in a cookie?
     */
    protected boolean requestedSessionCookie = false;
    /**
     * The requested session ID (if any) for this request.
     */
    protected String requestedSessionId = null;

    /**
     * The requested session version (if any) for this request.
     */
    protected String requestedSessionVersion = null;

    private boolean isRequestedSessionIdFromSecureCookie;

    // The requested session cookie path, see IT 7426
    protected String requestedSessionCookiePath;

    // Temporary holder for URI params from which session id is parsed
    protected CharChunk uriParamsCC = new CharChunk();

    /**
     * Was the requested session ID received in a URL?
     */
    protected boolean requestedSessionURL = false;
    /**
     * The socket through which this Request was received.
     */
    protected Socket socket = null;
    /**
     * Parse locales.
     */
    protected boolean localesParsed = false;

    /**
     * Local port
     */
    protected int localPort = -1;
    /**
     * Remote address.
     */
    protected String remoteAddr = null;
    /**
     * Remote host.
     */
    protected String remoteHost = null;
    /**
     * Remote port
     */
    protected int remotePort = -1;
    /**
     * Local address
     */
    protected String localName = null;
    /**
     * Local address
     */
    protected String localAddr = null;
    /** After the request is mapped to a ServletContext, we can also
     * map it to a logger.
     */
    /* CR 6309511
    protected Log log=null;
     */
    // START CR 6415120
    /**
     * Whether or not access to resources in WEB-INF or META-INF needs to be
     * checked.
     */
    protected boolean checkRestrictedResources = true;
    // END CR 6415120
    /**
     * has findSession been called and returned null already
     */
    private boolean unsuccessfulSessionFind = false;

    /*
     * Are we supposed to honor the unsuccessfulSessionFind flag?
     * WS overrides this to false.
     */
    protected boolean checkUnsuccessfulSessionFind = true;
    // START S1AS 4703023
    /**
     * The current application dispatch depth.
     */
    private int dispatchDepth = 0;
    /**
     * The maximum allowed application dispatch depth.
     */
    private static int maxDispatchDepth = Constants.DEFAULT_MAX_DISPATCH_DEPTH;
    // END S1AS 4703023
    // START SJSAS 6346226
    private String jrouteId;
    // END SJSAS 6346226
    // START GlassFish 896
    private SessionTracker sessionTracker = new SessionTracker();
    // END GlassFish 896
    // START GlassFish 1024
    private boolean isDefaultContext = false;
    // END GlassFish 1024
    private String requestURI = null;


    // FIX GLASSFISH-21007
    private boolean handlerInitialised = false;

    /**
     * Coyote request.
     */
    protected org.glassfish.grizzly.http.server.Request coyoteRequest;
    /**
     * The facade associated with this request.
     */
    protected RequestFacade facade = null;
    /**
     * Request facade that masks the fact that a request received
     * at the root context was mapped to a default-web-module (if such a
     * mapping exists).
     * For example, its getContextPath() will return "/" rather than the
     * context root of the default-web-module.
     */
    protected RequestFacade defaultContextMaskingFacade = null;
    /**
     * The response with which this request is associated.
     */
    protected org.apache.catalina.Response response = null;
    /**
     * Associated Catalina connector.
     */
    protected org.apache.catalina.Connector connector;
    /**
     * Mapping data.
     */
    protected MappingData mappingData = new MappingData();
    /**
     * Associated wrapper.
     */
    protected Wrapper wrapper = null;
    /**
     * Filter chain associated with the request.
     */
    protected FilterChain filterChain = null;
    /**
     * Async operation
     */
    // Async mode is supported by default for a request, unless the request
    // has passed a filter or servlet that does not support async
    // operation, in which case async operation will be disabled
    private boolean isAsyncSupported = true;
    private AtomicBoolean asyncStarted = new AtomicBoolean();
    private AsyncContextImpl asyncContext;
    private Thread asyncStartedThread;

    /**
     * Multi-Part support
     */
    private Multipart multipart;
    /**
     * Associated context.
     */
    protected Context context = null;
    protected ServletContext servletContext = null;
    // Associated StandardHost valve for error dispatches
    protected GlassFishValve hostValve;

    /*
     * The components of the request path
     */
    private String contextPath;
    private String servletPath;
    private String pathInfo;

    private boolean initRequestFacadeHelper = false;

    // Allow Grizzly to auto detect a remote close connection.
    public final static boolean discardDisconnectEvent =
            Boolean.getBoolean("org.glassfish.grizzly.discardDisconnect");

    /*
     * An upgrade request is received
     */
    private boolean upgrade = false;

    private boolean afterService = false;
    private boolean resume = false;

    /*
     * The HttpUpgradeHandler to be used for upgrade request
     */
    private HttpUpgradeHandler httpUpgradeHandler;

    /*
     * The WebConnection associated with upgraded request
     */
    private WebConnection webConnection;


    // ----------------------------------------------------------- Constructor
    public Request() {
        // START OF SJSAS 6231069
        formats = (SimpleDateFormat[]) staticDateFormats.get();
        TimeZone gmtTZ = TimeZone.getTimeZone("GMT");
        formats[0].setTimeZone(gmtTZ);
        formats[1].setTimeZone(gmtTZ);
        formats[2].setTimeZone(gmtTZ);
        // END OF SJSAS 6231069
    }

    // --------------------------------------------------------- Public Methods
    /**
     * Set the Coyote request.
     *
     * @param grizzlyRequest The Coyote request
     */
    public void setCoyoteRequest(org.glassfish.grizzly.http.server.Request grizzlyRequest) {
        this.coyoteRequest = grizzlyRequest;
        inputBuffer.setRequest(grizzlyRequest);
        inputBuffer.setRequest(this);
    }

    /**
     * Get the Coyote request.
     */
    public org.glassfish.grizzly.http.server.Request getCoyoteRequest() {
        return this.coyoteRequest;
    }

    /**
     * Set whether or not to enforce scope checking of this object.
     */
    public static void setEnforceScope(boolean enforce) {
        enforceScope = enforce;
    }

    /**
     * Release all object references, and initialize instance variables, in
     * preparation for reuse of this object.
     */
    @Override
    public void recycle() {

        if (isAsyncStarted()) {
            return;
        }

        handlerInitialised = false;
        context = null;
        servletContext = null;
        contextPath = null;
        servletPath = null;
        pathInfo = null;
        wrapper = null;

        dispatcherTypeAttr = null;
        requestDispatcherPath = null;

        authType = null;
        requestURI = null;
        inputBuffer.recycle();
        usingInputStream = false;
        usingReader = false;
        userPrincipal = null;
        subject = null;
        sessionParsed = false;
        parametersProcessed = false;
        cookiesParsed = false;
        locales.clear();
        localesParsed = false;
        secure = false;
        remoteAddr = null;
        remoteHost = null;
        remotePort = -1;
        localPort = -1;
        localAddr = null;
        localName = null;
        multipart = null;
        jrouteId = null;
        upgrade = false;
        afterService = false;
        resume = false;

        attributes.clear();
        notes.clear();
        cookies.clear();

        unsuccessfulSessionFind = false;

        if (session != null) {
            session.endAccess();
        }
        session = null;
        requestedSessionCookie = false;
        requestedSessionId = null;
        requestedSessionCookiePath = null;
        requestedSessionURL = false;
        uriParamsCC.recycle();

        // START GlassFish 896
        sessionTracker.reset();
        // END GlassFish 896

        /* CR 6309511
        log = null;
         */
        dispatchDepth = 0; // S1AS 4703023

        parameterMap.setLocked(false);
        parameterMap.clear();

        mappingData.recycle();

        initRequestFacadeHelper = false;
        if (enforceScope) {
            if (facade != null) {
                facade.clear();
                facade = null;
            }
            if (defaultContextMaskingFacade != null) {
                defaultContextMaskingFacade.clear();
                defaultContextMaskingFacade = null;
            }
            if (inputStream != null) {
                inputStream.clear();
                inputStream = null;
            }
            if (reader != null) {
                reader.clear();
                reader = null;
            }
        }

        /*
         * Clear and reinitialize all async related instance vars
         */
        if (asyncContext != null) {
            asyncContext.clear();
            asyncContext = null;
        }
        isAsyncSupported = true;
        asyncStarted.set(false);
        asyncStartedThread = null;
    }

    /**
     * Set the unsuccessfulSessionFind flag.
     *
     * @param unsuccessfulSessionFind
     */
    public void setUnsuccessfulSessionFind(boolean unsuccessfulSessionFind) {
        this.unsuccessfulSessionFind = unsuccessfulSessionFind;
    }

    /**
     * Get the unsuccessfulSessionFind flag.
     */
    public boolean getUnsuccessfulSessionFind() {
        return this.unsuccessfulSessionFind;
    }

    public void setUpgrade(boolean upgrade) {
        this.upgrade = upgrade;
    }

    public boolean isUpgrade() {
        return upgrade;
    }

    public HttpUpgradeHandler getHttpUpgradeHandler() {
        return httpUpgradeHandler;
    }

    // -------------------------------------------------------- Request Methods
    /**
     * Return the authorization credentials sent with this request.
     */
    @Override
    public String getAuthorization() {
        return coyoteRequest.getHeader(Constants.AUTHORIZATION_HEADER);
    }

    /**
     * Return the Connector through which this Request was received.
     */
    @Override
    public org.apache.catalina.Connector getConnector() {
        return connector;
    }

    /**
     * Set the Connector through which this Request was received.
     *
     * @param connector The new connector
     */
    @Override
    public void setConnector(org.apache.catalina.Connector connector) {
        this.connector = connector;
    }

    /**
     * Return the Context within which this Request is being processed.
     */
    @Override
    public Context getContext() {
        return context;
    }

    /**
     * Set the Context within which this Request is being processed.  This
     * must be called as soon as the appropriate Context is identified, because
     * it identifies the value to be returned by <code>getContextPath()</code>,
     * and thus enables parsing of the request URI.
     *
     * @param context The newly associated Context
     */
    @Override
    public void setContext(Context context) {
        this.context = context;
        if (context != null) {
            this.servletContext = context.getServletContext();
            Pipeline p = context.getParent().getPipeline();
            if (p != null) {
                hostValve = p.getBasic();
            }

			try {
				String reqEncoding = this.servletContext.getRequestCharacterEncoding();
				if (reqEncoding != null) {
					setCharacterEncoding(reqEncoding);
				}
				String resEncoding = this.servletContext.getResponseCharacterEncoding();
				if (resEncoding != null) {
					getResponse().getResponse().setCharacterEncoding(resEncoding);
				}
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException(e);
			}

        }
        // START GlassFish 896
        initSessionTracker();
        // END GlassFish 896
    }

    // START GlassFish 1024
    /**
     * @param isDefaultContext true if this request was mapped to a context
     * with an empty context root that is backed by the vitual server's
     * default-web-module
     */
    public void setDefaultContext(boolean isDefaultContext) {
        this.isDefaultContext = isDefaultContext;
    }
    // END GlassFish 1024

    /**
     * Get filter chain associated with the request.
     */
    @Override
    public FilterChain getFilterChain() {
        return filterChain;
    }

    /**
     * Set filter chain associated with the request.
     *
     * @param filterChain new filter chain
     */
    @Override
    public void setFilterChain(FilterChain filterChain) {
        this.filterChain = filterChain;
    }

    /**
     * Return the Host within which this Request is being processed.
     */
    @Override
    public Host getHost() {
        return (Host) mappingData.host;
    }

    /**
     * Set the Host within which this Request is being processed.  This
     * must be called as soon as the appropriate Host is identified, and
     * before the Request is passed to a context.
     *
     * @param host The newly associated Host
     */
    @Override
    public void setHost(Host host) {
        mappingData.host = host;
    }

	@Override
	public HttpServletMapping getHttpServletMapping() {
		return new MappingImpl(mappingData);
	}

    /**
     * Return descriptive information about this Request implementation and
     * the corresponding version number, in the format
     * <code>&lt;description&gt;/&lt;version&gt;</code>.
     */
    @Override
    public String getInfo() {
        return info;
    }

    /**
     * Return mapping data.
     */
    public MappingData getMappingData() {
        return mappingData;
    }

    /**
     * Set the mapping data for this Request.
     */
    public void setMappingData(MappingData mappingData) {
        this.mappingData = mappingData;
    }

    /**
     * Update this instance with the content of the {@link MappingData}
     * {@link MappingData}
     */
    public void updatePaths(MappingData md) {
        /*
         * Save the path components of this request, in order for them to
         * survive when the mapping data get recycled as the request
         * returns to the container after it has been put into async mode.
         * This is required to satisfy the requirements of subsequent async
         * dispatches (or error dispatches, if the async operation times out,
         * and no async listeners have been registered that could be notified
         * at their onTimeout method)
         */
        pathInfo = md.pathInfo.toString();
        servletPath = md.wrapperPath.toString();
        contextPath = md.contextPath.toString();
    }

    /**
     * Gets the <code>ServletRequest</code> for which this object
     * is the facade. This method must be implemented by a subclass.
     */
    @Override
    public HttpServletRequest getRequest() {
        return getRequest(false);
    }

    /**
     * Gets the <code>ServletRequest</code> for which this object
     * is the facade. This method must be implemented by a subclass.
     *
     * @param maskDefaultContextMapping true if the fact that a request
     * received at the root context was mapped to a default-web-module will
     * be masked, false otherwise
     */
    @Override
    public HttpServletRequest getRequest(boolean maskDefaultContextMapping) {
        if (!maskDefaultContextMapping || !isDefaultContext) {
            if (facade == null) {
                facade = new RequestFacade(this);
            }

            if (!initRequestFacadeHelper) {
                attributes.put(Globals.REQUEST_FACADE_HELPER,
                        facade.getRequestFacadeHelper());
                initRequestFacadeHelper = true;
            }
            return facade;
        } else {
            if (defaultContextMaskingFacade == null) {
                defaultContextMaskingFacade = new RequestFacade(this, true);
            }

            if (!initRequestFacadeHelper) {
                attributes.put(Globals.REQUEST_FACADE_HELPER,
                        defaultContextMaskingFacade.getRequestFacadeHelper());
                initRequestFacadeHelper = true;
            }

            return defaultContextMaskingFacade;
        }
    }

    /**
     * Return the Response with which this Request is associated.
     */
    @Override
    public org.apache.catalina.Response getResponse() {
        return this.response;
    }

    /**
     * Set the Response with which this Request is associated.
     *
     * @param response The new associated response
     */
    @Override
    public void setResponse(org.apache.catalina.Response response) {
        this.response = response;
        if (response instanceof Response) {
            sessionTracker.setResponse((Response) response);
        }
    }

    /**
     * Return the Socket (if any) through which this Request was received.
     * This should <strong>only</strong> be used to access underlying state
     * information about this Socket, such as the SSLSession associated with
     * an SSLSocket.
     */
    @Override
    public Socket getSocket() {
        return socket;
    }

    /**
     * Set the Socket (if any) through which this Request was received.
     *
     * @param socket The socket through which this request was received
     */
    @Override
    public void setSocket(Socket socket) {
        this.socket = socket;
        remoteHost = null;
        remoteAddr = null;
        remotePort = -1;
        localPort = -1;
        localAddr = null;
        localName = null;
    }

    /**
     * Return the input stream associated with this Request.
     */
    @Override
    public InputStream getStream() {
        if (inputStream == null) {
            inputStream = new CoyoteInputStream(inputBuffer);
        }
        return inputStream;
    }

    /**
     * Set the input stream associated with this Request.
     *
     * @param stream The new input stream
     */
    @Override
    public void setStream(InputStream stream) {
        // Ignore
    }
    /**
     * URI byte to char converter (not recycled).
     */
    protected B2CConverter URIConverter = null;

    /**
     * Return the URI converter.
     */
    protected B2CConverter getURIConverter() {
        return URIConverter;
    }

    /**
     * Set the URI converter.
     *
     * @param URIConverter the new URI converter
     */
    protected void setURIConverter(B2CConverter URIConverter) {
        this.URIConverter = URIConverter;
    }

    /**
     * Return the Wrapper within which this Request is being processed.
     */
    @Override
    public Wrapper getWrapper() {
        return wrapper;
    }

    /**
     * Set the Wrapper within which this Request is being processed.  This
     * must be called as soon as the appropriate Wrapper is identified, and
     * before the Request is ultimately passed to an application servlet.
     * @param wrapper The newly associated Wrapper
     */
    @Override
    public void setWrapper(Wrapper wrapper) {
        this.wrapper = wrapper;
    }

    // ------------------------------------------------- Request Public Methods
    /**
     * Create and return a ServletInputStream to read the content
     * associated with this Request.
     *
     * @exception IOException if an input/output error occurs
     */
    @Override
    public ServletInputStream createInputStream()
            throws IOException {
        if (inputStream == null) {
            inputStream = new CoyoteInputStream(inputBuffer);
        }
        return inputStream;
    }

    /**
     * Perform whatever actions are required to flush and close the input
     * stream or reader, in a single operation.
     *
     * @exception IOException if an input/output error occurs
     */
    @Override
    public void finishRequest() throws IOException {
        // The reader and input stream don't need to be closed
    }

    /**
     * Return the object bound with the specified name to the internal notes
     * for this request, or <code>null</code> if no such binding exists.
     *
     * @param name Name of the note to be returned
     */
    @Override
    public Object getNote(String name) {
        return notes.get(name);
    }

    /**
     * Return an Iterator containing the String names of all notes bindings
     * that exist for this request.
     */
    public Iterator<String> getNoteNames() {
        return notes.keySet().iterator();
    }

    /**
     * Remove any object bound to the specified name in the internal notes
     * for this request.
     *
     * @param name Name of the note to be removed
     */
    @Override
    public void removeNote(String name) {
        notes.remove(name);
    }

    /**
     * Bind an object to a specified name in the internal notes associated
     * with this request, replacing any existing binding for this name.
     *
     * @param name Name to which the object should be bound
     * @param value Object to be bound to the specified name
     */
    @Override
    public void setNote(String name, Object value) {
        notes.put(name, value);
    }

    /**
     * Set the content length associated with this Request.
     *
     * @param length The new content length
     */
    @Override
    public void setContentLength(int length) {
        coyoteRequest.getRequest().setContentLength(length);
    }

    /**
     * Set the content type (and optionally the character encoding)
     * associated with this Request.  For example,
     * <code>text/html; charset=ISO-8859-4</code>.
     *
     * @param type The new content type
     */
    @Override
    public void setContentType(String type) {
        // Not used
    }

    /**
     * Set the protocol name and version associated with this Request.
     *
     * @param protocol Protocol name and version
     */
    @Override
    public void setProtocol(String protocol) {
        // Not used
    }

    /**
     * Set the IP address of the remote client associated with this Request.
     *
     * @param remoteAddr The remote IP address
     */
    @Override
    public void setRemoteAddr(String remoteAddr) {
        // Not used
    }

    /**
     * Set the fully qualified name of the remote client associated with this
     * Request.
     *
     * @param remoteHost The remote host name
     */
    public void setRemoteHost(String remoteHost) {
        // Not used
    }

    /**
     * Set the value to be returned by <code>isSecure()</code>
     * for this Request.
     *
     * @param secure The new isSecure value
     */
    @Override
    public void setSecure(boolean secure) {
        this.secure = secure;
    }

    /**
     * Set the name of the server (virtual host) to process this request.
     *
     * @param name The server name
     */
    @Override
    public void setServerName(String name) {
        coyoteRequest.setServerName(name);
    }

    /**
     * Set the port number of the server to process this request.
     *
     * @param port The server port
     */
    @Override
    public void setServerPort(int port) {
        coyoteRequest.setServerPort(port);
    }

    // START CR 6415120
    /**
     * Set whether or not access to resources under WEB-INF or META-INF
     * needs to be checked.
     */
    @Override
    public void setCheckRestrictedResources(boolean check) {
        this.checkRestrictedResources = check;
    }

    /**
     * Return whether or not access to resources under WEB-INF or META-INF
     * needs to be checked.
     */
    @Override
    public boolean getCheckRestrictedResources() {
        return this.checkRestrictedResources;
    }
    // END CR 6415120

    // ------------------------------------------------- ServletRequest Methods
    /**
     * Return the specified request attribute if it exists; otherwise, return
     * <code>null</code>.
     *
     * @param name Name of the request attribute to return
     */
    @Override
    public Object getAttribute(String name) {

        switch (name) {
            case Globals.DISPATCHER_TYPE_ATTR:
                return dispatcherTypeAttr == null
                        ? DispatcherType.REQUEST
                        : dispatcherTypeAttr;
            case Globals.DISPATCHER_REQUEST_PATH_ATTR:
                return requestDispatcherPath == null
                        ? getRequestPathMB().toString()
                        : requestDispatcherPath.toString();
            case Globals.CONSTRAINT_URI:
                return getRequestPathMB() != null
                        ? getRequestPathMB().toString() : null;
        }

        Object attr = attributes.get(name);

        if (attr != null) {
            return attr;
        }

        attr = coyoteRequest.getAttribute(name);
        if (attr != null) {
            return attr;
        }
        if (Globals.SSL_CERTIFICATE_ATTR.equals(name)) {
            // @TODO Implement SSL rehandshake
            RequestUtils.populateCertificateAttribute(coyoteRequest);
            attr = getAttribute(Globals.CERTIFICATES_ATTR);
            if (attr != null) {
                attributes.put(name, attr);
            }
        } else if (isSSLAttribute(name)) {
            // START SJSAS 6419950
            RequestUtils.populateSSLAttributes(coyoteRequest);
            // END SJSAS 6419950
            attr = attributes.get(name);
        }

        return attr;
    }

    /**
     * Test if a given name is one of the special Servlet-spec SSL attributes.
     */
    static boolean isSSLAttribute(String name) {
        return Globals.CERTIFICATES_ATTR.equals(name) ||
                Globals.CIPHER_SUITE_ATTR.equals(name) ||
                Globals.KEY_SIZE_ATTR.equals(name) ||
                Globals.SSL_SESSION_ID_ATTR.equals(name);
    }

    /**
     * Return the names of all request attributes for this Request, or an
     * empty <code>Enumeration</code> if there are none.
     */
    @Override
    public Enumeration<String> getAttributeNames() {
        if (isSecure()) {
            populateSSLAttributes();
        }
        return new Enumerator<>(attributes.keySet(), true);
    }

    /**
     * Return the character encoding for this Request.
     */
    @Override
    public String getCharacterEncoding() {
        return coyoteRequest.getCharacterEncoding();
    }

    /**
     * Return the content length for this Request.
     */
    @Override
    public int getContentLength() {
        return coyoteRequest.getContentLength();
    }

    /**
     * Return the content length for this Request.
     */
    @Override
    public long getContentLengthLong() {
        return coyoteRequest.getContentLengthLong();
    }

    /**
     * Return the content type for this Request.
     */
    @Override
    public String getContentType() {
        return coyoteRequest.getContentType();
    }

    /**
     * Return the servlet input stream for this Request.  The default
     * implementation returns a servlet input stream created by
     * <code>createInputStream()</code>.
     *
     * @exception IllegalStateException if <code>getReader()</code> has
     *  already been called for this request
     * @exception IOException if an input/output error occurs
     */
    @Override
    public ServletInputStream getInputStream() throws IOException {

        if (usingReader) {
            throw new IllegalStateException(rb.getString(LogFacade.GETREADER_BEEN_CALLED_EXCEPTION));
        }

        usingInputStream = true;
        if (inputStream == null) {
            inputStream = new CoyoteInputStream(inputBuffer);
        }

        return inputStream;
    }

    /**
     * Return the preferred Locale that the client will accept content in,
     * based on the value for the first <code>Accept-Language</code> header
     * that was encountered.  If the request did not specify a preferred
     * language, the server's default Locale is returned.
     */
    @Override
    public Locale getLocale() {
        return coyoteRequest.getLocale();
    }

    /**
     * Return the set of preferred Locales that the client will accept
     * content in, based on the values for any <code>Accept-Language</code>
     * headers that were encountered.  If the request did not specify a
     * preferred language, the server's default Locale is returned.
     */
    @Override
    public Enumeration<Locale> getLocales() {
        return new Enumerator<Locale>(coyoteRequest.getLocales());
    }

    private void processParameters() {
        if (parametersProcessed) {
            return;
        }
        getCharacterEncoding();
        if (isMultipartConfigured() && getMethod().equalsIgnoreCase("POST")) {
            String contentType = getContentType();
            if (contentType != null &&
                        contentType.startsWith("multipart/form-data")) {
                getMultipart().init();
            }
        }
        parametersProcessed = true;
    }

    /**
     * Return the value of the specified request parameter, if any; otherwise,
     * return <code>null</code>.  If there is more than one value defined,
     * return only the first one.
     *
     * @param name Name of the desired request parameter
     */
    @Override
    public String getParameter(String name) {
        processParameters();

        return coyoteRequest.getParameter(name);
    }

    /**
     * Returns a <code>Map</code> of the parameters of this request.
     * Request parameters are extra information sent with the request.
     * For HTTP servlets, parameters are contained in the query string
     * or posted form data.
     *
     * @return A <code>Map</code> containing parameter names as keys
     *  and parameter values as map values.
     */
    @Override
    public Map<String, String[]> getParameterMap() {

        if (parameterMap.isLocked()) {
            return parameterMap;
        }

        Enumeration<String> e = getParameterNames();
        while (e.hasMoreElements()) {
            String name = e.nextElement();
            String[] values = getParameterValues(name);
            parameterMap.put(name, values);
        }

        parameterMap.setLocked(true);

        return parameterMap;

    }

    /**
     * Return the names of all defined request parameters for this request.
     */
    @Override
    public Enumeration<String> getParameterNames() {
        processParameters();

        return new Enumerator<>(coyoteRequest.getParameterNames());
    }

    /**
     * Return the defined values for the specified request parameter, if any;
     * otherwise, return <code>null</code>.
     *
     * @param name Name of the desired request parameter
     */
    @Override
    public String[] getParameterValues(String name) {
        processParameters();

        return coyoteRequest.getParameterValues(name);
    }

    /**
     * Return the protocol and version used to make this Request.
     */
    @Override
    public String getProtocol() {
        return coyoteRequest.getProtocol().getProtocolString();
    }

    /**
     * Read the Reader wrapping the input stream for this Request.  The
     * default implementation wraps a <code>BufferedReader</code> around the
     * servlet input stream returned by <code>createInputStream()</code>.
     *
     * @exception IllegalStateException if <code>getInputStream()</code>
     *  has already been called for this request
     * @exception IOException if an input/output error occurs
     */
    @Override
    public BufferedReader getReader() throws IOException {

        if (usingInputStream) {
            throw new IllegalStateException(rb.getString(LogFacade.GETINPUTSTREAM_BEEN_CALLED_EXCEPTION));
        }

        usingReader = true;
        try {
            inputBuffer.checkConverter();
        } catch (UnsupportedCharsetException uce) {
            UnsupportedEncodingException uee =
                    new UnsupportedEncodingException(uce.getMessage());
            uee.initCause(uce);
            throw uee;
        }

        if (reader == null) {
            reader = new CoyoteReader(inputBuffer);
        }
        return reader;
    }

    /**
     * Return the real path of the specified virtual path.
     *
     * @param path Path to be translated
     *
     * @deprecated As of version 2.1 of the Java Servlet API, use
     *  <code>ServletContext.getRealPath()</code>.
     */
    @Override
    public String getRealPath(String path) {
        if (servletContext == null) {
            return null;
        } else {
            try {
                return servletContext.getRealPath(path);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    /**
     * Return the remote IP address making this Request.
     */
    @Override
    public String getRemoteAddr() {
        if (remoteAddr == null) {

            // START SJSAS 6347215
            if (connector.getAuthPassthroughEnabled() && connector.getProxyHandler() != null) {
                remoteAddr = connector.getProxyHandler().getRemoteAddress(
                        getRequest());
                if (remoteAddr == null && log.isLoggable(Level.FINEST)) {
                    log.log(Level.FINEST, LogFacade.UNABLE_DETERMINE_CLIENT_ADDRESS);
                }
                return remoteAddr;
            }
            // END SJSAS 6347215

            if (socket != null) {
                InetAddress inet = socket.getInetAddress();
                remoteAddr = inet.getHostAddress();
            } else {
//                coyoteRequest.action(ActionCode.ACTION_REQ_HOST_ADDR_ATTRIBUTE, coyoteRequest);
                remoteAddr = coyoteRequest.getRemoteAddr();
            }
        }
        return remoteAddr;
    }

    /**
     * Return the remote host name making this Request.
     */
    @Override
    public String getRemoteHost() {
        if (remoteHost == null) {
            if (!connector.getEnableLookups()) {
                remoteHost = getRemoteAddr();
                // START SJSAS 6347215
            } else if (connector.getAuthPassthroughEnabled() && connector.getProxyHandler() != null) {
                String addr =
                        connector.getProxyHandler().getRemoteAddress(getRequest());
                if (addr != null) {
                    try {
                        remoteHost = InetAddress.getByName(addr).getHostName();
                    } catch (UnknownHostException e) {
                        String msg = MessageFormat.format(rb.getString(LogFacade.UNABLE_RESOLVE_IP_EXCEPTION), addr);
                        log.log(Level.WARNING, msg, e);
                    }
                } else if (log.isLoggable(Level.FINEST)) {
                    log.log(Level.FINEST, LogFacade.UNABLE_DETERMINE_CLIENT_ADDRESS);
                }
                // END SJSAS 6347215
            } else if (socket != null) {
                InetAddress inet = socket.getInetAddress();
                remoteHost = inet.getHostName();
            } else {
//                coyoteRequest.action(ActionCode.ACTION_REQ_HOST_ATTRIBUTE, coyoteRequest);
                remoteHost = coyoteRequest.getRemoteHost();
            }
        }
        return remoteHost;
    }

    /**
     * Returns the Internet Protocol (IP) source port of the client
     * or last proxy that sent the request.
     */
    @Override
    public int getRemotePort() {
        if (remotePort == -1) {
            if (socket != null) {
                remotePort = socket.getPort();
            } else {
//                coyoteRequest.action(ActionCode.ACTION_REQ_REMOTEPORT_ATTRIBUTE, coyoteRequest);
                remotePort = coyoteRequest.getRemotePort();
            }
        }
        return remotePort;
    }

    /**
     * Returns the host name of the Internet Protocol (IP) interface on
     * which the request was received.
     */
    @Override
    public String getLocalName() {
        if (localName == null) {
            if (socket != null) {
                InetAddress inet = socket.getLocalAddress();
                localName = inet.getHostName();
            } else {
//                coyoteRequest.action(ActionCode.ACTION_REQ_LOCAL_NAME_ATTRIBUTE, coyoteRequest);
                localName = coyoteRequest.getLocalName();
            }
        }
        return localName;
    }

    /**
     * Returns the Internet Protocol (IP) address of the interface on
     * which the request  was received.
     */
    @Override
    public String getLocalAddr() {
        if (localAddr == null) {
            if (socket != null) {
                InetAddress inet = socket.getLocalAddress();
                localAddr = inet.getHostAddress();
            } else {
//                coyoteRequest.action(ActionCode.ACTION_REQ_LOCAL_ADDR_ATTRIBUTE, coyoteRequest);
                localAddr = coyoteRequest.getLocalAddr();
            }
        }
        return localAddr;
    }

    /**
     * Returns the Internet Protocol (IP) port number of the interface
     * on which the request was received.
     */
    @Override
    public int getLocalPort() {
        if (localPort == -1) {
            if (socket != null) {
                localPort = socket.getLocalPort();
            } else {
//                coyoteRequest.action(ActionCode.ACTION_REQ_LOCALPORT_ATTRIBUTE, coyoteRequest);
                localPort = coyoteRequest.getLocalPort();
            }
        }
        return localPort;
    }

    /**
     * Return a RequestDispatcher that wraps the resource at the specified
     * path, which may be interpreted as relative to the current request path.
     *
     * @param path Path of the resource to be wrapped
     */
    @Override
    public RequestDispatcher getRequestDispatcher(String path) {

        if (servletContext == null) {
            return null;
        }

        // If the path is already context-relative, just pass it through
        if (path == null) {
            return null;
        } else if (path.startsWith("/")) {
            return servletContext.getRequestDispatcher(path);
        }

        // Convert a request-relative path to a context-relative one
        String servPath = (String) getAttribute(
                RequestDispatcher.INCLUDE_SERVLET_PATH);
        if (servPath == null) {
            servPath = getServletPath();
        }

        // Add the path info, if there is any
        String pInfo = getPathInfo();
        String requestPath = pInfo == null
            ? servPath
            : servPath + pInfo;

        int pos = requestPath.lastIndexOf('/');
        String relative = pos >= 0
            ? requestPath.substring(0, pos + 1) + path
            : requestPath + path;

        return servletContext.getRequestDispatcher(relative);

    }

    /**
     * Return the scheme used to make this Request.
     */
    @Override
    public String getScheme() {
        // START S1AS 6170450
        if (getConnector() != null && getConnector().getAuthPassthroughEnabled()) {
            ProxyHandler proxyHandler = getConnector().getProxyHandler();
            if (proxyHandler != null && proxyHandler.getSSLKeysize(getRequest()) > 0) {
                return "https";
            }
        }
        // END S1AS 6170450

        return coyoteRequest.getScheme();
    }

    /**
     * Return the server name responding to this Request.
     */
    @Override
    public String getServerName() {
        return coyoteRequest.getServerName();
    }

    /**
     * Return the server port responding to this Request.
     */
    @Override
    public int getServerPort() {
        /* SJSAS 6586658
        return (coyoteRequest.getServerPort());
         */
        // START SJSAS 6586658
        if (isSecure()) {
            String host = getHeader("host");
            if (host != null && host.indexOf(':') == -1) {
                // No port number provided with Host header, use default
                return 443;
            } else {
                return coyoteRequest.getServerPort();
            }
        } else {
            return coyoteRequest.getServerPort();
        }
        // END SJSAS 6586658
    }

    /**
     * Was this request received on a secure connection?
     */
    @Override
    public boolean isSecure() {
        return secure;
    }

    /**
     * Remove the specified request attribute if it exists.
     *
     * @param name Name of the request attribute to remove
     */
    @Override
    public void removeAttribute(String name) {
        Object value = null;
        boolean found = attributes.containsKey(name);
        if (found) {
            value = attributes.get(name);
            attributes.remove(name);
        } else {
            return;
        }

        // Notify interested application event listeners
        List<EventListener> listeners = context.getApplicationEventListeners();
        if (listeners.isEmpty()) {
            return;
        }
        ServletRequestAttributeEvent event =
                new ServletRequestAttributeEvent(servletContext, getRequest(),
                name, value);
        for (EventListener eventListener : listeners) {
            if (!(eventListener instanceof ServletRequestAttributeListener)) {
                continue;
            }
            ServletRequestAttributeListener listener =
                    (ServletRequestAttributeListener) eventListener;
            try {
                listener.attributeRemoved(event);
            } catch (Throwable t) {
                log(rb.getString(LogFacade.ATTRIBUTE_EVENT_LISTENER_EXCEPTION), t);
                // Error valve will pick this exception up and display it to user
                attributes.put(RequestDispatcher.ERROR_EXCEPTION, t);
            }
        }
    }

    /**
     * Set the specified request attribute to the specified value.
     *
     * @param name Name of the request attribute to set
     * @param value The associated value
     */
    @Override
    public void setAttribute(String name, Object value) {

        // Name cannot be null
        if (name == null) {
            throw new IllegalArgumentException(rb.getString(LogFacade.NULL_ATTRIBUTE_NAME_EXCEPTION));
        }

        // Null value is the same as removeAttribute()
        if (value == null) {
            removeAttribute(name);
            return;
        }

        if (name.equals(Globals.DISPATCHER_TYPE_ATTR)) {
            dispatcherTypeAttr = value;
            return;
        } else if (name.equals(Globals.DISPATCHER_REQUEST_PATH_ATTR)) {
            requestDispatcherPath = value;
            return;
        }

        boolean replaced = false;

        // Do the security check before any updates are made
        if (Globals.IS_SECURITY_ENABLED &&
                name.equals("org.apache.tomcat.sendfile.filename")) {
            // Use the canonical file name to avoid any possible symlink and
            // relative path issues
            String canonicalPath;
            try {
                canonicalPath = new File(value.toString()).getCanonicalPath();
            } catch (IOException e) {
                String msg = MessageFormat.format(rb.getString(LogFacade.UNABLE_DETERMINE_CANONICAL_NAME), value);
                throw new SecurityException(msg, e);
            }
            // Sendfile is performed in Tomcat's security context so need to
            // check if the web app is permitted to access the file while still
            // in the web app's security context
            System.getSecurityManager().checkRead(canonicalPath);
            // Update the value so the canonical path is used
            value = canonicalPath;
        }

        Object oldValue = attributes.put(name, value);
        if (oldValue != null) {
            replaced = true;
        }

        // START SJSAS 6231069
        // Pass special attributes to the ngrizzly layer
        if (name.startsWith("grizzly.")) {
            coyoteRequest.setAttribute(name, value);
        }
        // END SJSAS 6231069

        // Notify interested application event listeners
        List<EventListener> listeners = context.getApplicationEventListeners();
        if (listeners.isEmpty()) {
            return;
        }
        ServletRequestAttributeEvent event = replaced
            ? new ServletRequestAttributeEvent(servletContext,
                    getRequest(), name,
                    oldValue)
            : new ServletRequestAttributeEvent(servletContext,
                    getRequest(), name,
                    value);

        for (EventListener eventListener : listeners) {
            if (!(eventListener instanceof ServletRequestAttributeListener)) {
                continue;
            }
            ServletRequestAttributeListener listener =
                    (ServletRequestAttributeListener) eventListener;
            try {
                if (replaced) {
                    listener.attributeReplaced(event);
                } else {
                    listener.attributeAdded(event);
                }
            } catch (Throwable t) {
                log(rb.getString(LogFacade.ATTRIBUTE_EVENT_LISTENER_EXCEPTION), t);
                // Error valve will pick this exception up and display it to user
                attributes.put(RequestDispatcher.ERROR_EXCEPTION, t);
            }
        }
    }

    /**
     * Overrides the name of the character encoding used in the body of this
     * request.
     *
     * This method must be called prior to reading request parameters or
     * reading input using <code>getReader()</code>. Otherwise, it has no
     * effect.
     *
     * @param enc      <code>String</code> containing the name of
     *                 the character encoding.
     * @throws         UnsupportedEncodingException if this
     *                 ServletRequest is still in a state where a
     *                 character encoding may be set, but the specified
     *                 encoding is invalid
     *
     * @since Servlet 2.3
     */
    @Override
    public void setCharacterEncoding(String enc)
            throws UnsupportedEncodingException {

        // START SJSAS 4936855
        if (parametersProcessed || usingReader) {
            String contextName =
                getContext() != null ? getContext().getName() : "UNKNOWN";
            log.log(Level.WARNING, LogFacade.UNABLE_SET_REQUEST_CHARS, new Object[] {enc, contextName});
            return;
        }
        // END SJSAS 4936855

        // Ensure that the specified encoding is valid
        byte buffer[] = new byte[1];
        buffer[0] = (byte) 'a';

        // START S1AS 6179607: Workaround for 6181598. Workaround should be
        // removed once the underlying issue in J2SE has been fixed.
        /*
         * String dummy = new String(buffer, enc);
         */
        // END S1AS 6179607
        // START S1AS 6179607
        final byte[] finalBuffer = buffer;
        final String finalEnc = enc;
        if (Globals.IS_SECURITY_ENABLED) {
            try {
                AccessController.doPrivileged(
                    (PrivilegedExceptionAction<String>) () ->
                            new String(finalBuffer, RequestUtil.lookupCharset(finalEnc))
                );
            } catch (PrivilegedActionException pae) {
                throw (UnsupportedEncodingException) pae.getCause();
            }
        } else {
            new String(buffer, RequestUtil.lookupCharset(enc));
        }
        // END S1AS 6179607

        // Save the validated encoding
        coyoteRequest.setCharacterEncoding(enc);

    }

    // START S1AS 4703023
    /**
     * Static setter method for the maximum dispatch depth
     */
    public static void setMaxDispatchDepth(int depth) {
        maxDispatchDepth = depth;
    }

    public static int getMaxDispatchDepth() {
        return maxDispatchDepth;
    }

    /**
     * Increment the depth of application dispatch
     */
    public int incrementDispatchDepth() {
        return ++dispatchDepth;
    }

    /**
     * Decrement the depth of application dispatch
     */
    public int decrementDispatchDepth() {
        return --dispatchDepth;
    }

    /**
     * Check if the application dispatching has reached the maximum
     */
    public boolean isMaxDispatchDepthReached() {
        return dispatchDepth > maxDispatchDepth;
    }
    // END S1AS 4703023

    // ---------------------------------------------------- HttpRequest Methods
    @Override
    public boolean authenticate(HttpServletResponse response)
            throws IOException, ServletException {

        //Issue 9650 - COmmenting this as required
      /*  if (getUserPrincipal() != null) {
        throw new ServletException("Attempt to re-login while the " +
        "user identity already exists");
        }*/

        if (context == null) {//TODO: throw an exception
            throw new ServletException("Internal error: Context null");
        }

        final AuthenticatorBase authBase = (AuthenticatorBase) context.getAuthenticator();

        if (authBase == null) {
            throw new ServletException("Internal error: Authenticator null");
        }

        byte[] alreadyCalled = (byte[]) reentrancyStatus.get();
        if (alreadyCalled[0] == 1) {
            //Re-entrancy from a JSR 196  module, so call the authenticate directly
            try {
                return authBase.authenticate(this, (HttpResponse) getResponse(),
                        context.getLoginConfig());
            } catch (Exception ex) {
                throw new ServletException("Exception thrown while attempting to authenticate", ex);
            }

        } else {
            //No re-entrancy, so call invokeAuthenticateDelegate to check if
            //JSR196 module is present
            alreadyCalled[0] = 1;
            try {
                final Realm realm = context.getRealm();
                final Request req = this;
                if (realm == null) {
                    throw new ServletException("Internal error: realm null");
                }
                try {
                    if (Globals.IS_SECURITY_ENABLED) {
                        return AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
                            @Override
                            public Boolean run() {
                                try {
                                    return realm.invokeAuthenticateDelegate(req, (HttpResponse) getResponse(), context, (AuthenticatorBase) authBase, true);
                                } catch (IOException ex) {
                                    throw new RuntimeException("Exception thrown while attempting to authenticate", ex);
                                }
                            }
                        });
                    } else {
                        return realm.invokeAuthenticateDelegate(req, (HttpResponse) getResponse(), context, (AuthenticatorBase) authBase, true);
                    }

                } catch (Exception ex) {
                    throw new ServletException("Exception thrown while attempting to authenticate", ex);
                }

            } finally {
                //Reset the threadlocal re-entrancy check variable
                alreadyCalled[0] = 0;
            }
        }
    }

    @Override
    public void login(final String username, final String password)
            throws ServletException {
        login(username, password != null ? password.toCharArray() : null);
    }

    public void login(final String username, final char[] password)
            throws ServletException {
        final Realm realm = context.getRealm();
        if (realm != null && realm.isSecurityExtensionEnabled(getServletContext())) {
            throw new ServletException
               (rb.getString(LogFacade.LOGIN_WITH_AUTH_CONFIG));
 	}

        if (getAuthType() != null || getRemoteUser() != null ||
                getUserPrincipal() != null) {
            throw new ServletException(
                    rb.getString(LogFacade.ALREADY_AUTHENTICATED));
        }

        if (context.getAuthenticator() == null) {
            throw new ServletException(rb.getString(LogFacade.NO_AUTHENTICATOR));
        }

        context.getAuthenticator().login(username, password, this);
    }

    @Override
    public void logout() throws ServletException {

        Realm realm = (context == null ? null : context.getRealm());
        if (realm == null) {
            if (getUserPrincipal() != null || getAuthType() != null) {
                throw new ServletException(
                        rb.getString(LogFacade.INTERNAL_LOGOUT_ERROR));
            }
            return;
        }
        /*
         * Pass the request (this).
         */
        realm.logout(this);
    }

    /**
     * Add a Cookie to the set of Cookies associated with this Request.
     *
     * @param cookie The new cookie
     */
    @Override
    public void addCookie(Cookie cookie) {

        // For compatibility only
        if (!cookiesParsed) {
            parseCookies();
        }

        cookies.add(cookie);
    }

    /**
     * Add a Header to the set of Headers associated with this Request.
     *
     * @param name The new header name
     * @param value The new header value
     */
    @Override
    public void addHeader(String name, String value) {
        coyoteRequest.getRequest().getHeaders().addValue(name).setString(value);
    }

    /**
     * Add a Locale to the set of preferred Locales for this Request.  The
     * first added Locale will be the first one returned by getLocales().
     *
     * @param locale The new preferred Locale
     */
    @Override
    public void addLocale(Locale locale) {
        locales.add(locale);
    }

    /**
     * Add a parameter name and corresponding set of values to this Request.
     * (This is used when restoring the original request on a form based
     * login).
     *
     * @param name Name of this request parameter
     * @param values Corresponding values for this request parameter
     */
    @Override
    public void addParameter(String name, String values[]) {
        coyoteRequest.addParameter(name, values);
    }

    /**
     * Clear the collection of Cookies associated with this Request.
     */
    @Override
    public void clearCookies() {
        cookiesParsed = true;
        cookies.clear();
    }

    /**
     * Clear the collection of Headers associated with this Request.
     */
    @Override
    public void clearHeaders() {
        coyoteRequest.getRequest().getHeaders().recycle();
    }

    /**
     * Clear the collection of Locales associated with this Request.
     */
    @Override
    public void clearLocales() {
        locales.clear();
    }

    /**
     * Clear the collection of parameters associated with this Request
     * and reset the query string encoding charset.
     */
    @Override
    public void clearParameters() {
        coyoteRequest.getParameters().recycle();
        coyoteRequest.getParameters().setQueryStringEncoding(
                Charsets.lookupCharset(getConnector().getURIEncoding()));
    }

    @Override
    public void replayPayload(byte[] payloadByteArray) {
        if (payloadByteArray == null) {
            return;
        }

        coyoteRequest.replayPayload(Buffers.wrap(
                coyoteRequest.getContext().getMemoryManager(), payloadByteArray));
    }

    /**
     * Set the authentication type used for this request, if any; otherwise
     * set the type to <code>null</code>.  Typical values are "BASIC",
     * "DIGEST", or "SSL".
     *
     * @param type The authentication type used
     */
    @Override
    public void setAuthType(String type) {
        this.authType = type;
    }

    /**
     * Set the HTTP request method used for this Request.
     *
     * <p>Used by FBL when the original request is restored after
     * successful authentication.
     *
     * @param method The request method
     */
    @Override
    public void setMethod(String method) {
        coyoteRequest.setMethod(method);
    }

    /**
     * Sets the query string for this Request.
     *
     * <p>Used by FBL when the original request is restored after
     * successful authentication.
     *
     * @param query The query string
     */
    @Override
    public void setQueryString(String query) {
        coyoteRequest.setQueryString(query);
    }

    /**
     * Set the path information for this Request.
     *
     * @param pathInfo The path information
     */
    @Override
    public void setPathInfo(String pathInfo) {
        mappingData.pathInfo.setString(pathInfo);
        this.pathInfo = pathInfo;
    }

    /**
     * Set a flag indicating whether or not the requested session ID for this
     * request came in through a cookie.  This is normally called by the
     * HTTP Connector, when it parses the request headers.
     *
     * @param flag The new flag
     */
    @Override
    public void setRequestedSessionCookie(boolean flag) {
        this.requestedSessionCookie = flag;
    }

    /**
     * Sets the requested session cookie path, see IT 7426
     */
    @Override
    public void setRequestedSessionCookiePath(String cookiePath) {
        requestedSessionCookiePath = cookiePath;
    }

    /**
     * Set the requested session ID for this request.  This is normally called
     * by the HTTP Connector, when it parses the request headers.
     *
     * This method, which is called when the session id is sent as a cookie,
     * or when it is encoded in the request URL, removes a jvmRoute
     * (if present) from the given id.
     *
     * @param id The new session id
     */
    @Override
    public void setRequestedSessionId(String id) {
        requestedSessionId = id;
        if (id != null && connector.getJvmRoute() != null) {
            // Remove jvmRoute. The assumption is that the first dot in the
            // passed in id is the separator between the session id and the
            // jvmRoute. Therefore, the session id, however generated, must
            // never have any dots in it if the jvmRoute mechanism has been
            // enabled. There is no choice to use a separator other than dot
            // because this is the semantics mandated by the mod_jk LB for it
            // to function properly.
            // We can't use StandardContext.getJvmRoute() to determine whether
            // jvmRoute has been enabled, because this CoyoteRequest may not
            // have been associated with any context yet.
            int index = id.indexOf(".");
            if (index > 0) {
                requestedSessionId = id.substring(0, index);
            }
        }
    }

    /**
     * Set a flag indicating whether or not the requested session ID for this
     * request came in through a URL.  This is normally called by the
     * HTTP Connector, when it parses the request headers.
     *
     * @param flag The new flag
     */
    @Override
    public void setRequestedSessionURL(boolean flag) {
        this.requestedSessionURL = flag;
    }

    /**
     * Set the unparsed request URI for this Request.  This will normally be
     * called by the HTTP Connector, when it parses the request headers.
     *
     * Used by FBL when restoring original request after successful
     * authentication.
     *
     * @param uri The request URI
     */
    @Override
    public void setRequestURI(String uri) {
        coyoteRequest.setRequestURI(uri);
    }

    /**
     * Get the decoded request URI.
     *
     * @return the URL decoded request URI
     */
    @Override
    public String getDecodedRequestURI() {
        return getDecodedRequestURI(false);
    }

    /**
     * Gets the decoded request URI.
     *
     * @param maskDefaultContextMapping true if the fact that a request
     * received at the root context was mapped to a default-web-module will
     * be masked, false otherwise
     */
    public String getDecodedRequestURI(boolean maskDefaultContextMapping) {
        try {
            if (maskDefaultContextMapping || !isDefaultContext) {
                return coyoteRequest.getDecodedRequestURI();
            } else {
                return getContextPath() + coyoteRequest.getDecodedRequestURI();
            }
        } catch (CharConversionException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Sets the servlet path for this Request.
     *
     * @param servletPath The servlet path
     */
    @Override
    public void setServletPath(String servletPath) {
        mappingData.wrapperPath.setString(servletPath);
        this.servletPath = servletPath;
    }

    /**
     * Set the Principal who has been authenticated for this Request.  This
     * value is also used to calculate the value to be returned by the
     * <code>getRemoteUser()</code> method.
     *
     * @param principal The user Principal
     */
    @Override
    public void setUserPrincipal(Principal principal) {

        if (SecurityUtil.isPackageProtectionEnabled()) {
            HttpSession session = getSession(false);
            if (subject != null &&
                !subject.getPrincipals().contains(principal)) {
                subject.getPrincipals().add(principal);
            } else if (session != null &&
                    session.getAttribute(Globals.SUBJECT_ATTR) == null) {
                subject = new Subject();
                subject.getPrincipals().add(principal);
            }
            if (session != null) {
                session.setAttribute(Globals.SUBJECT_ATTR, subject);
            }
        }

        this.userPrincipal = principal;
    }

    // --------------------------------------------- HttpServletRequest Methods
    /**
     * Return the authentication type used for this Request.
     */
    @Override
    public String getAuthType() {
        return authType;
    }

    /**
     * Return the portion of the request URI used to select the Context
     * of the Request.
     */
    @Override
    public String getContextPath() {
        return getContextPath(false);
    }

    /**
     * Gets the portion of the request URI used to select the Context
     * of the Request.
     *
     * @param maskDefaultContextMapping true if the fact that a request
     * received at the root context was mapped to a default-web-module will
     * be masked, false otherwise
     */
    public String getContextPath(boolean maskDefaultContextMapping) {
        if (isDefaultContext && maskDefaultContextMapping) {
            return "";
        } else {
            return contextPath;
        }
    }

    /**
     * Return the set of Cookies received with this Request.
     */
    @Override
    public Cookie[] getCookies() {

        if (!cookiesParsed) {
            parseCookies();
        }

        if (cookies.size() == 0) {
            return null;
        }

        return cookies.toArray(new Cookie[0]);
    }

    /**
     * Set the set of cookies received with this Request.
     */
    public void setCookies(Cookie[] cookies) {

        this.cookies.clear();
        if (cookies != null) {
            Collections.addAll(this.cookies, cookies);
        }
    }

    /**
     * Return the value of the specified date header, if any; otherwise
     * return -1.
     *
     * @param name Name of the requested date header
     *
     * @exception IllegalArgumentException if the specified header value
     *  cannot be converted to a date
     */
    @Override
    public long getDateHeader(String name) {

        String value = getHeader(name);
        if (value == null) {
            return -1L;
        }

        // Attempt to convert the date header in a variety of formats
        long result = FastHttpDateFormat.parseDate(value, formats);
        if (result != -1L) {
            return result;
        }
        throw new IllegalArgumentException(value);

    }

    /**
     * Return the first value of the specified header, if any; otherwise,
     * return <code>null</code>
     *
     * @param name Name of the requested header
     */
    @Override
    public String getHeader(String name) {
        return coyoteRequest.getHeader(name);
    }

    /**
     * Return all of the values of the specified header, if any; otherwise,
     * return an empty enumeration.
     *
     * @param name Name of the requested header
     */
    @Override
    public Enumeration<String> getHeaders(String name) {
        return new Enumerator<>(coyoteRequest.getHeaders(name).iterator());
    }

    /**
     * Return the names of all headers received with this request.
     */
    @Override
    public Enumeration<String> getHeaderNames() {
        return new Enumerator<>(coyoteRequest.getHeaderNames().iterator());
    }

    /**
     * Return the value of the specified header as an integer, or -1 if there
     * is no such header for this request.
     *
     * @param headerName Name of the requested header
     *
     * @exception IllegalArgumentException if the specified header value
     *  cannot be converted to an integer
     */
    @Override
    public int getIntHeader(String headerName) {

        String header = getHeader(headerName);
        if (header == null) {
            return -1;
        }

        return parseInt(header);
    }

    @Override
    public Map<String, String> getTrailerFields() {
        return coyoteRequest.getTrailers();
    }

    @Override
    public boolean isTrailerFieldsReady() {
        return coyoteRequest.areTrailersAvailable();
    }

    /**
     * Return the HTTP request method used in this Request.
     */
    @Override
    public String getMethod() {
        return coyoteRequest.getMethod().getMethodString();
    }

    /**
     * Return the path information associated with this Request.
     */
    @Override
    public String getPathInfo() {
        return pathInfo;
    }

    /**
     * Return the extra path information for this request, translated
     * to a real path.
     */
    @Override
    public String getPathTranslated() {

        if (servletContext == null) {
            return null;
        }

        if (getPathInfo() == null) {
            return null;
        } else {
            return servletContext.getRealPath(getPathInfo());
        }

    }

    @Override
    public PushBuilder newPushBuilder() {
        Http2Stream http2Stream = null;
        if (coyoteRequest != null) {
            http2Stream = (Http2Stream) coyoteRequest.getAttribute(Http2Stream.HTTP2_STREAM_ATTRIBUTE);
        }
        if (http2Stream != null && http2Stream.isPushEnabled()) {
            return new ApplicationPushBuilder(this);
        } else {
            return null;
        }
    }

    /**
     * Return the query string associated with this request.
     */
    @Override
    public String getQueryString() {
        String queryString = coyoteRequest.getQueryString();

        if (queryString == null || "".equals(queryString)) {
            return null;
        } else {
            return queryString;
        }
    }

    /**
     * Return the name of the remote user that has been authenticated
     * for this Request.
     */
    @Override
    public String getRemoteUser() {
        if (userPrincipal != null) {
            return userPrincipal.getName();
        } else {
            return null;
        }
    }

    /**
     * Get the request path.
     *
     * @return the request path
     */
    @Override
    public DataChunk getRequestPathMB() {
        return mappingData.requestPath;
    }

    /**
     * Return the session identifier included in this request, if any.
     */
    @Override
    public String getRequestedSessionId() {
        return requestedSessionId;
    }

    /**
     * Return the request URI for this request.
     */
    @Override
    public String getRequestURI() {
        return getRequestURI(false);
    }

    /**
     * Gets the request URI for this request.
     *
     * @param maskDefaultContextMapping true if the fact that a request
     * received at the root context was mapped to a default-web-module will
     * be masked, false otherwise
     */
    public String getRequestURI(boolean maskDefaultContextMapping) {
        if (maskDefaultContextMapping) {
            return coyoteRequest.getRequestURI();
        } else {
            if (requestURI == null) {
                // START GlassFish 1024
                if (isDefaultContext) {
                    requestURI = getContextPath() +
                        coyoteRequest.getRequestURI();
                } else {
                    // END GlassFish 1024
                    requestURI = coyoteRequest.getRequestURI();
                    // START GlassFish 1024
                }
                // END GlassFish 1024
            }
            return requestURI;
        }
    }

    /**
     * Reconstructs the URL the client used to make the request.
     * The returned URL contains a protocol, server name, port
     * number, and server path, but it does not include query
     * string parameters.
     * <p>
     * Because this method returns a <code>StringBuffer</code>,
     * not a <code>String</code>, you can modify the URL easily,
     * for example, to append query parameters.
     * <p>
     * This method is useful for creating redirect messages and
     * for reporting errors.
     *
     * @return A <code>StringBuffer</code> object containing the
     *  reconstructed URL
     */
    @Override
    public StringBuffer getRequestURL() {
        return getRequestURL(false);
    }

    public StringBuffer getRequestURL(boolean maskDefaultContextMapping) {
        StringBuffer url = new StringBuffer();
        String scheme = getScheme();
        int port = getServerPort();
        if (port < 0) {
            port = 80; // Work around java.net.URL bug
        }
        url.append(scheme);
        url.append("://");
        url.append(getServerName());
        if (scheme.equals("http") && port != 80 || scheme.equals("https") && port != 443) {
            url.append(':');
            url.append(port);
        }
        url.append(getRequestURI(maskDefaultContextMapping));

        return url;

    }

    /**
     * Return the portion of the request URI used to select the servlet
     * that will process this request.
     */
    @Override
    public String getServletPath() {
        return servletPath;
    }

    /**
     * Return the session associated with this Request, creating one
     * if necessary.
     */
    @Override
    public HttpSession getSession() {
        Session session = doGetSession(true);
        if (session != null) {
            return session.getSession();
        } else {
            return null;
        }
    }

    /**
     * Return the session associated with this Request, creating one
     * if necessary and requested.
     *
     * @param create Create a new session if one does not exist
     */
    @Override
    public HttpSession getSession(boolean create) {
        Session session = doGetSession(create);
        if (session != null) {
            return session.getSession();
        } else {
            return null;
        }
    }

    /**
     * set the session - this method is not for general use
     *
     * @param newSess the new session
     */
    public void setSession(Session newSess) {
        session = newSess;
    }

    /**
     * Return <code>true</code> if the session identifier included in this
     * request came from a cookie.
     */
    @Override
    public boolean isRequestedSessionIdFromCookie() {

        if (requestedSessionId != null) {
            return requestedSessionCookie;
        } else {
            return false;
        }

    }

    /**
     * Return <code>true</code> if the session identifier included in this
     * request came from the request URI.
     */
    @Override
    public boolean isRequestedSessionIdFromURL() {

        if (requestedSessionId != null) {
            return requestedSessionURL;
        } else {
            return false;
        }

    }

    /**
     * Return <code>true</code> if the session identifier included in this
     * request came from the request URI.
     *
     * @deprecated As of Version 2.1 of the Java Servlet API, use
     *  <code>isRequestedSessionIdFromURL()</code> instead.
     */
    @Override
    public boolean isRequestedSessionIdFromUrl() {
        return isRequestedSessionIdFromURL();
    }

    /**
     * Marks (or unmarks) this request as having a JSESSIONID cookie
     * that is marked as secure
     *
     * @param secure true if this request has a JSESSIONID cookie that is
     * marked as secure, false otherwise
     */
    public void setRequestedSessionIdFromSecureCookie(boolean secure) {
        isRequestedSessionIdFromSecureCookie = secure;
    }


    /**
     * @return true if this request contains a JSESSIONID cookie that is
     * marked as secure, false otherwise
     */
    public boolean isRequestedSessionIdFromSecureCookie() {
        return isRequestedSessionIdFromSecureCookie;
    }

    /**
     * Return <code>true</code> if the session identifier included in this
     * request identifies a valid session.
     */
    @Override
    public boolean isRequestedSessionIdValid() {
        if (requestedSessionId == null) {
            return false;
        }
        if (context == null) {
            return false;
        }

        if (session != null &&
                requestedSessionId.equals(session.getIdInternal())) {
            return session.isValid();
        }

        Manager manager = context.getManager();
        if (manager == null) {
            return false;
        }
        Session localSession = null;
        try {
            if (manager.isSessionVersioningSupported()) {
                localSession = manager.findSession(requestedSessionId,
                                                   requestedSessionVersion);
            } else {
                localSession = manager.findSession(requestedSessionId, this);
            }
        } catch (IOException e) {
            localSession = null;
        }
        return localSession != null && localSession.isValid();

    }

    /**
     * Return <code>true</code> if the authenticated user principal
     * possesses the specified role name.
     *
     * @param role Role name to be validated
     */
    @Override
    public boolean isUserInRole(String role) {

        // BEGIN RIMOD 4949842
        /*
         * Must get userPrincipal through getUserPrincipal(), can't assume
         * it has already been set since it may be coming from core.
         */
        Principal userPrincipal = this.getUserPrincipal();
        // END RIMOD 4949842

        // Have we got an authenticated principal at all?
        if (userPrincipal == null) {
            return false;
        }

        // Identify the Realm we will use for checking role assignments
        if (context == null) {
            return false;
        }
        Realm realm = context.getRealm();
        if (realm == null) {
            return false;
        }

        // Check for a role alias defined in a <security-role-ref> element
        if (wrapper != null) {
            String realRole = wrapper.findSecurityReference(role);

            //START SJSAS 6232464
            if (realRole != null &&
                    realm.hasRole(this, (HttpResponse) response,
                    userPrincipal, realRole)) {
                return true;
            }
        }

        // Check for a role defined directly as a <security-role>

        return realm.hasRole(this, (HttpResponse) response,
                userPrincipal, role);
        //END SJSAS 6232464
    }

    /**
     * Return the principal that has been authenticated for this Request.
     */
    @Override
    public Principal getUserPrincipal() {
        return userPrincipal;
    }

    /**
     * Return the session associated with this Request, creating one
     * if necessary.
     */
    public Session getSessionInternal() {
        return doGetSession(true);
    }

    /**
     * Gets the session associated with this Request, creating one
     * if necessary and requested.
     *
     * @param create true if a new session is to be created if one does not
     * already exist, false otherwise
     */
    @Override
    public Session getSessionInternal(boolean create) {
        return doGetSession(create);
    }

    /**
     * Change the session id of the current session associated with this
     * request and return the new session id.
     *
     * @return the new session id
     *
     * @throws IllegalStateException if there is no session associated
     * with the request
     *
     * @since Servlet 3.1
     */
    @Override
    public String changeSessionId() {
        Manager manager = context.getManager();
        if (manager == null) {
            throw new IllegalStateException(rb.getString(LogFacade.CHANGE_SESSION_ID_BEEN_CALLED_EXCEPTION));
        }
        Session session = getSessionInternal(false);
        if (session == null) {
            throw new IllegalStateException(rb.getString(LogFacade.CHANGE_SESSION_ID_BEEN_CALLED_EXCEPTION));
        }

        manager.changeSessionId(session);
        String newSessionId = session.getId();
        // This should only ever be called if there was an old session ID but
        // double check to be sure
        if (requestedSessionId != null && requestedSessionId.length() > 0) {
            requestedSessionId = newSessionId;
        }

        addSessionCookie();

        return newSessionId;
    }

    /**
     * This object does not implement a session ID generator. Provide
     * a dummy implementation so that the default one will be used.
     */
    @Override
    public String generateSessionId() {
        return null;
    }

    /**
     * Gets the servlet context to which this servlet request was last
     * dispatched.
     *
     * @return the servlet context to which this servlet request was last
     * dispatched
     */
    @Override
    public ServletContext getServletContext() {
        return servletContext;
    }

    /**
     * Create an instance of <code>HttpUpgradeHandler</code> for an given
     * class and uses it for the http protocol upgrade processing.
     *
     * @param handlerClass The <code>HttpUpgradeHandler</code> class used for the upgrade.
     *
     * @return an instance of the <code>HttpUpgradeHandler</code>
     *
     * @exception IOException if an I/O error occurred during the upgrade
     * @exception ServletException if the given <tt>clazz</tt> fails to be
     * instantiated
     *
     * @see javax.servlet.http.HttpUpgradeHandler
     * @see javax.servlet.http.WebConnection
     *
     * @since Servlet 3.1
     */
    @Override
    public <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass)
            throws IOException, ServletException {
        upgrade = true;
        T handler = null;
        try {
            handler = ((StandardContext) getContext()).createHttpUpgradeHandlerInstance(handlerClass);
        } catch(IOException | ServletException ise) {
            throw ise;
        } catch(Throwable t) {
            throw new ServletException(t);
        }
        httpUpgradeHandler = handler;
        coyoteRequest.getResponse().suspend();
        return handler;
    }

    public void initialiseHttpUpgradeHandler(WebConnection wc) {
        // ensure the handler is only initialised once
        if (!handlerInitialised && httpUpgradeHandler != null) {
            httpUpgradeHandler.init(wc);
            handlerInitialised = true;
        }
    }

    public WebConnection getWebConnection() {
        return webConnection;
    }

    public void setWebConnection(WebConnection wc) {
        webConnection = wc;
    }

    // ------------------------------------------------------ Protected Methods

    protected Session doGetSession(boolean create) {

        // There cannot be a session if no context has been assigned yet
        if (context == null) {
            return null;
        }

        // Return the current session if it exists and is valid
        if (session != null && !session.isValid()) {
            session = null;
        }
        if (session != null) {
            return session;
        }

        // Return the requested session if it exists and is valid
        Manager manager = context.getManager();
        if (manager == null) {
            return null;      // Sessions are not supported
        }
        if (requestedSessionId != null) {
            if (!checkUnsuccessfulSessionFind || !unsuccessfulSessionFind) {
                try {
                    if (manager.isSessionVersioningSupported()) {
                        session = manager.findSession(requestedSessionId,
                                                      requestedSessionVersion);
                        //XXX need to revisit
                        if (session instanceof StandardSession) {
                            incrementSessionVersion((StandardSession) session,
                                                    context);
                        }
                    } else {
                        session = manager.findSession(requestedSessionId, this);
                    }
                    if (session == null) {
                        unsuccessfulSessionFind = true;
                    }
                } catch (IOException e) {
                    session = null;
                }
            }
            if (session != null && !session.isValid()) {
                session = null;
            }
            if (session != null) {
                session.access();
                return session;
            }
        }

        // Create a new session if requested and the response is not committed
        if (!create) {
            return null;
        }
        if (context != null && response != null &&
                context.getCookies() &&
                response.getResponse().isCommitted()) {
            throw new IllegalStateException(rb.getString(LogFacade.CANNOT_CREATE_SESSION_EXCEPTION));
        }

        // START S1AS8PE 4817642
        if (requestedSessionId != null && context != null && context.getReuseSessionID()) {
            session = manager.createSession(requestedSessionId);
            if (manager instanceof PersistentManagerBase) {
                ((PersistentManagerBase) manager).removeFromInvalidatedSessions(requestedSessionId);
            }
            // END S1AS8PE 4817642
            // START GlassFish 896
        } else if (sessionTracker.getActiveSessions() > 0) {
            synchronized (sessionTracker) {
                if (sessionTracker.getActiveSessions() > 0) {
                    String id = sessionTracker.getSessionId();
                    session = manager.createSession(id);
                    if (manager instanceof PersistentManagerBase) {
                        ((PersistentManagerBase) manager).removeFromInvalidatedSessions(id);
                    }
                }
            }
            // END GlassFish 896
            // START S1AS8PE 4817642
        } else {
            // END S1AS8PE 4817642
            // Use the connector's random number generator (if any) to generate
            // a session ID. Fallback to the default session ID generator if
            // the connector does not implement one.
            String id = generateSessionId();
            session = id != null
                ? manager.createSession(id)
                : manager.createSession();
            // START S1AS8PE 4817642
        }
        // END S1AS8PE 4817642

        StandardHost reqHost = (StandardHost) getHost();
        if (reqHost != null) {
            SingleSignOn sso = reqHost.getSingleSignOn();
            if (sso != null) {
                String ssoId = (String) getNote(
                        org.apache.catalina.authenticator.Constants.REQ_SSOID_NOTE);
                if (ssoId != null) {
                    long ssoVersion = 0L;
                    Long ssoVersionObj = (Long)getNote(
                            org.apache.catalina.authenticator.Constants.REQ_SSO_VERSION_NOTE);
                    if (ssoVersionObj != null) {
                        ssoVersion = ssoVersionObj;
                    }
                    sso.associate(ssoId, ssoVersion, session);
                    removeNote(
                            org.apache.catalina.authenticator.Constants.REQ_SSOID_NOTE);
                }
            }
        }

        // START GlassFish 896
        sessionTracker.track(session);
        // END GlassFish 896

        // Creating a new session cookie based on the newly created session
        if (session != null && getContext() != null) {
            if (manager.isSessionVersioningSupported()) {
                incrementSessionVersion((StandardSession) session, context);
            }

            addSessionCookie();
        }

        if (session != null) {
            session.access();
            return session;
        } else {
            return null;
        }

    }

    /**
     * Configures the given JSESSIONID cookie.
     *
     * @param cookie The JSESSIONID cookie to be configured
     */
    protected void configureSessionCookie(Cookie cookie) {
        cookie.setHttpOnly(true);
        cookie.setMaxAge(-1);
        String contextPath = null;
        // START GlassFish 1024
        if (isDefaultContext) {
            cookie.setPath("/");
        } else {
            // END GlassFish 1024
            if (context != null) {
                // START OF SJSAS 6231069
                contextPath = context.getPath();
                // END OF SJSAS 6231069
            }
            if (contextPath != null && contextPath.length() > 0) {
                cookie.setPath(contextPath);
            } else {
                cookie.setPath("/");
            }
            // START GlassFish 1024
        }
        // END GlassFish 1024
        if (isSecure()) {
            cookie.setSecure(true);
        }

        // Override the default config with servlet context
        // sessionCookieConfig
        if (context != null) {
            SessionCookieConfig sessionCookieConfig =
                    context.getSessionCookieConfig();
            if (sessionCookieConfig.getDomain() != null) {
                cookie.setDomain(sessionCookieConfig.getDomain());
            }
            if (sessionCookieConfig.getPath() != null) {
                cookie.setPath(sessionCookieConfig.getPath());
            }
            if (sessionCookieConfig.getComment() != null) {
                cookie.setVersion(1);
                cookie.setComment(sessionCookieConfig.getComment());
            }
            // do nothing if it is already secure
            if (!cookie.getSecure()) {
                cookie.setSecure(sessionCookieConfig.isSecure());
            }
            cookie.setHttpOnly(sessionCookieConfig.isHttpOnly());
            cookie.setMaxAge(sessionCookieConfig.getMaxAge());
        }

        if (requestedSessionCookiePath != null) {
            cookie.setPath(requestedSessionCookiePath);
        }
    }

    /**
     * Parse cookies.
     */
    protected void parseCookies() {

        cookiesParsed = true;

        org.glassfish.grizzly.http.Cookie[] serverCookies = coyoteRequest.getCookies();
        int count = serverCookies.length;
        if (count <= 0) {
            return;
        }

        cookies.clear();

        for (org.glassfish.grizzly.http.Cookie scookie : serverCookies) {
            try {
                // START GlassFish 898
                Cookie cookie = makeCookie(scookie);
                // END GlassFish 898
                cookie.setPath(scookie.getPath());
                cookie.setVersion(scookie.getVersion());
                String domain = scookie.getDomain();
                if (domain != null) {
                    cookie.setDomain(scookie.getDomain());
                }
                cookies.add(cookie);
            } catch (IllegalArgumentException e) {
                ; // Ignore bad cookie.
            }
        }
    }

    // START GlassFish 898
    protected Cookie makeCookie(org.glassfish.grizzly.http.Cookie scookie) {
        return makeCookie(scookie, false);
    }

    protected Cookie makeCookie(org.glassfish.grizzly.http.Cookie scookie, boolean decode) {

        String name = scookie.getName();
        String value = scookie.getValue();

        if (decode) {
            try {
                name = URLDecoder.decode(name, "UTF-8");
                value = URLDecoder.decode(value, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                name = URLDecoder.decode(name);
                value = URLDecoder.decode(value);
            }
        }

        return new Cookie(name, value);
    }
    // END GlassFish 898


    // START SJSAS 6346738
    /**
     * Gets the POST body of this request.
     *
     * @return The POST body of this request
     */
    protected byte[] getPostBody() throws IOException {

        int len = getContentLength();
        byte[] formData = null;

        if (len < CACHED_POST_LEN) {
            if (postData == null) {
                postData = new byte[CACHED_POST_LEN];
            }
            formData = postData;
        } else {
            formData = new byte[len];
        }
        int actualLen = readPostBody(formData, len);
        if (actualLen == len) {
            return formData;
        }

        return null;
    }
    // END SJSAS 6346738

    /**
     * Read post body in an array.
     */
    protected int readPostBody(byte body[], int len)
            throws IOException {

        Buffer b = coyoteRequest.getPostBody(len).duplicate();
        final int length = b.limit() - b.position();
        b.get(body, b.position(), length);
        return length;

    }

    /*
     * Returns true if the given string is composed of upper- or lowercase
     * letters only, false otherwise.
     *
     * @return true if the given string is composed of upper- or lowercase
     * letters only, false otherwise.
     */
    protected static boolean isAlpha(String value) {

        if (value == null) {
            return false;
        }

        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (!(c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z')) {
                return false;
            }
        }

        return true;
    }

    /**
     * Take the session id from Grizzly Request
     */
    protected void obtainSessionId() {
        setRequestedSessionURL(true);
        setJrouteId(coyoteRequest.getJrouteId());
        setRequestedSessionId(coyoteRequest.getRequestedSessionId());
    }

    // START CR 6309511
    /**
     * Parse session id in URL.
     */
    protected void parseSessionId(String sessionParameterName, CharChunk uriBB) {

        // Parse session ID, and extract it from the decoded request URI
        String sessionParam = ";" + sessionParameterName + "=";
        String sessionId =
            parseParameterFromRequestURI(uriBB, sessionParam);

        if (sessionId != null) {
            // START SJSAS 6346226
            int jrouteIndex = sessionId.lastIndexOf(':');
            if (jrouteIndex > 0) {
                setRequestedSessionId(sessionId.substring(0, jrouteIndex));
                if (jrouteIndex < sessionId.length() - 1) {
                    setJrouteId(sessionId.substring(jrouteIndex + 1));
                }
            } else {
                setRequestedSessionId(sessionId);
            }
            // END SJSAS 6346226

            setRequestedSessionURL(true);

            // START SJSWS 6376484
            /*
             * Parse the session id from the encoded URI only if the encoded
             * URI is not null, to allow for lazy evaluation
             */
            if (coyoteRequest.getRequestURI() != null) {
                removeParameterFromRequestURI(sessionParam);
            }
            // END SJSWS 6376484

        } else {
            setRequestedSessionId(null);
            setRequestedSessionURL(false);
        }
    }
    // END CR 6309511

    /**
     * Parses and removes any session version (if present) from the request
     * URI.
     *
     */
    protected void parseSessionVersion(CharChunk uriCC) {
        String sessionVersionString =
            parseParameterFromRequestURI(uriCC, Globals.SESSION_VERSION_PARAMETER);

        if (sessionVersionString != null) {
            parseSessionVersionString(sessionVersionString);

            removeParameterFromRequestURI(Globals.SESSION_VERSION_PARAMETER);
        }
    }

    /**
     * Parses and removes jreplica (if present) from the request URI.
     */
    protected void parseJReplica(CharChunk uriCC) {
        String jreplica =
            parseParameterFromRequestURI(uriCC, Globals.JREPLICA_PARAMETER);

        if (jreplica != null) {
            Session session = getSessionInternal(false);
            if (session != null) {
                session.setNote(Globals.JREPLICA_SESSION_NOTE, jreplica);
            }

            removeParameterFromRequestURI(Globals.JREPLICA_PARAMETER);
        }

    }

    private void addSessionCookie() {
        if (context != null && context.getCookies() && response != null) {
            String jvmRoute = ((StandardContext) getContext()).getJvmRoute();
            /*
             * Check if context has been configured with jvmRoute for
             * Apache LB. If it has, do not add the JSESSIONID cookie
             * here, but rely on OutputBuffer#addSessionCookieWithJvmRoute
             * to add the jvmRoute enhanced JSESSIONID as a cookie right
             * before the response is flushed.
             */
            if (jvmRoute == null) {
                Cookie newCookie = new Cookie(
                        getContext().getSessionCookieName(), session.getId());
                configureSessionCookie(newCookie);
                ((HttpResponse)response).addSessionCookieInternal(newCookie);
            }
        }
    }

    /**
     * @param parameter  of the form ";" + parameterName + "="
     * @return parameterValue
     */
    private String parseParameterFromRequestURI(CharChunk uriCC, String parameter) {

        String parameterValue = null;

        int semicolon = uriCC.indexOf(parameter, 0, parameter.length(), 0);
        if (semicolon >= 0) {

            int start = uriCC.getStart();
            int end = uriCC.getEnd();

            int parameterStart = start + semicolon + parameter.length();
            int semicolon2 = uriCC.indexOf(';', semicolon + parameter.length());
            if (semicolon2 >= 0) {
                parameterValue = new String(
                    uriCC.getBuffer(),
                    parameterStart,
                    semicolon2 - semicolon - parameter.length());
            } else {
                parameterValue = new String(
                    uriCC.getBuffer(),
                    parameterStart,
                    end - parameterStart);
            }

        }

        return parameterValue;
    }

    // START SJSWS 6376484
    /**
     * Removes the session version from the request URI.
     * @param parameter   of the form ";" + parameterName + "="
     */
    private void removeParameterFromRequestURI(String parameter) {

        int semicolon, semicolon2;

        final DataChunk uriBC =
                coyoteRequest.getRequest().getRequestURIRef().getRequestURIBC();

        semicolon = uriBC.indexOf(parameter, 0);

        if (semicolon > 0) {
            semicolon2 = uriBC.indexOf(';', semicolon + parameter.length());

            final int end;
            if (semicolon2 >= 0) {
                end = semicolon2;
                uriBC.notifyDirectUpdate();
            } else {
                end = uriBC.getLength();
            }

            uriBC.delete(semicolon, end);
        }
    }
    // END SJSWS 6376484

    /*
     * Parses the given session version string into its components. Each
     * component is stored as an entry in a HashMap, which maps a context
     * path to its session version number. The HashMap is stored as a
     * request attribute, to make it available to any target contexts to which
     * this request may be dispatched.
     *
     * This method also sets the session version number for the context with
     * which this request has been associated.
     */
    void parseSessionVersionString(String sessionVersionString) {
        if (sessionVersionString == null || !isSessionVersioningSupported()) {
            return;
        }

        HashMap<String, String> sessionVersions =
            RequestUtil.parseSessionVersionString(sessionVersionString);
        if (sessionVersions != null) {
            attributes.put(Globals.SESSION_VERSIONS_REQUEST_ATTRIBUTE,
                           sessionVersions);
            if (context != null) {
                String path = context.getPath();
                if ("".equals(path)) {
                    path = "/";
                }
                this.requestedSessionVersion = sessionVersions.get(path);
            }
        }
    }

    /**
     * Parses the value of the JROUTE cookie, if present.
     */
    void parseJrouteCookie() {
        org.glassfish.grizzly.http.Cookie[] serverCookies = coyoteRequest.getCookies();
        int count = serverCookies.length;
        if (count <= 0) {
            return;
        }

        for (org.glassfish.grizzly.http.Cookie scookie : serverCookies) {
            if (scookie.getName().equals(Constants.JROUTE_COOKIE)) {
                setJrouteId(scookie.getValue());
                break;
            }
        }
    }

    /**
     * Sets the jroute id of this request.
     *
     * @param jrouteId The jroute id
     */
    void setJrouteId(String jrouteId) {
        this.jrouteId = jrouteId;
    }

    /**
     * Gets the jroute id of this request, which may have been
     * sent as a separate <code>JROUTE</code> cookie or appended to the
     * session identifier encoded in the URI (if cookies have been disabled).
     *
     * @return The jroute id of this request, or null if this request does not
     * carry any jroute id
     */
    @Override
    public String getJrouteId() {
        return jrouteId;
    }
    // END SJSAS 6346226

    // START CR 6309511
    /**
     * Parse session id in URL.
     */
    protected void parseSessionCookiesId() {

        // If session tracking via cookies has been disabled for the current
        // context, don't go looking for a session ID in a cookie as a cookie
        // from a parent context with a session ID may be present which would
        // overwrite the valid session ID encoded in the URL
        Context context = (Context) getMappingData().context;
        if (context != null && !context.getCookies()) {
            return;
        }

        // Parse session id from cookies
        org.glassfish.grizzly.http.Cookie[] serverCookies = coyoteRequest.getCookies();
        int count = serverCookies.length;
        if (count <= 0) {
            return;
        }

        String sessionCookieName = Globals.SESSION_COOKIE_NAME;
        if (context != null) {
            sessionCookieName = context.getSessionCookieName();
        }
        for (org.glassfish.grizzly.http.Cookie scookie : serverCookies) {
            if (scookie.getName().equals(sessionCookieName)) {
                // Override anything requested in the URL
                if (!isRequestedSessionIdFromCookie()) {
                    // Accept only the first session id cookie
                    setRequestedSessionId(scookie.getValue());
                    // TODO: Pass cookie path into
                    // getSessionVersionFromCookie()
                    String sessionVersionString = getSessionVersionFromCookie();
                    parseSessionVersionString(sessionVersionString);
                    setRequestedSessionCookie(true);
                    // TBD: ServerCookie#getSecure currently always returns
                    // false.
                    setRequestedSessionIdFromSecureCookie(scookie.isSecure());
                    setRequestedSessionURL(false);
                } else {
                    if (!isRequestedSessionIdValid()) {
                        // Replace the session id until one is valid
                        setRequestedSessionId(scookie.getValue());
                        // TODO: Pass cookie path into
                        // getSessionVersionFromCookie()
                        String sessionVersionString =
                                getSessionVersionFromCookie();
                        parseSessionVersionString(sessionVersionString);
                    }
                }
            }
        }
    }
    // END CR 6309511

    /*
     * Returns the value of the first JSESSIONIDVERSION cookie, or null
     * if no such cookie present in the request.
     *
     * TODO: Add cookie path argument, and return value of JSESSIONIDVERSION
     * cookie with the specified path.
     */
    private String getSessionVersionFromCookie() {
        if (!isSessionVersioningSupported()) {
            return null;
        }

        org.glassfish.grizzly.http.Cookie[] serverCookies = coyoteRequest.getCookies();
        int count = serverCookies.length;
        if (count <= 0) {
            return null;
        }

        for (org.glassfish.grizzly.http.Cookie scookie : serverCookies) {
            if (scookie.getName().equals(
                    Globals.SESSION_VERSION_COOKIE_NAME)) {
                return scookie.getValue();
            }
        }

        return null;
    }

    /*
     * @return temporary holder for URI params from which session id is parsed
     */
    CharChunk getURIParams() {
        return uriParamsCC;
    }

    // START CR 6309511
    /**
     * Character conversion of the URI.
     */
    protected void convertURI(MessageBytes uri)
            throws Exception {

        ByteChunk bc = uri.getByteChunk();
        CharChunk cc = uri.getCharChunk();
        int length = bc.getLength();
        cc.allocate(length, -1);

        String enc = connector.getURIEncoding();
        if (enc != null && !enc.isEmpty() &&
                !Globals.ISO_8859_1_ENCODING.equalsIgnoreCase(enc)) {
            B2CConverter conv = getURIConverter();
            try {
                if (conv == null) {
                    conv = new B2CConverter(enc);
                    setURIConverter(conv);
                }
            } catch (IOException e) {
                // Ignore
                log.log(Level.SEVERE, LogFacade.INVALID_URI_ENCODING);
                connector.setURIEncoding(null);
            }
            if (conv != null) {
                try {
                    conv.convert(bc, cc, cc.getBuffer().length - cc.getEnd());
                    uri.setChars(cc.getBuffer(), cc.getStart(),
                            cc.getLength());
                    return;
                } catch (IOException e) {
                    log.log(Level.SEVERE, LogFacade.INVALID_URI_CHAR_ENCODING);
                    cc.recycle();
                }
            }
        }

        // Default encoding: fast conversion
        byte[] bbuf = bc.getBuffer();
        char[] cbuf = cc.getBuffer();
        int start = bc.getStart();
        for (int i = 0; i < length; i++) {
            cbuf[i] = (char) (bbuf[i + start] & 0xff);
        }
        uri.setChars(cbuf, 0, length);

    }
    // END CR 6309511

    @Override
    public DispatcherType getDispatcherType() {
        DispatcherType dispatcher = (DispatcherType) getAttribute(
                Globals.DISPATCHER_TYPE_ATTR);
        if (dispatcher == null) {
            dispatcher = DispatcherType.REQUEST;
        }
        return dispatcher;
    }

    /**
     * Starts async processing on this request.
     */
    @Override
    public AsyncContext startAsync() throws IllegalStateException {
        return startAsync(getRequest(), getResponse().getResponse(), true);
    }

    /**
     * Starts async processing on this request.
     *
     * @param servletRequest the ServletRequest with which to initialize
     * the AsyncContext
     * @param servletResponse the ServletResponse with which to initialize
     * the AsyncContext
     */
    @Override
    public AsyncContext startAsync(ServletRequest servletRequest,
                                   ServletResponse servletResponse)
            throws IllegalStateException {
        return startAsync(servletRequest, servletResponse, false);
    }

    /**
     * Starts async processing on this request.
     *
     * @param servletRequest the ServletRequest with which to initialize
     * the AsyncContext
     * @param servletResponse the ServletResponse with which to initialize
     * the AsyncContext
     * @param isStartAsyncWithZeroArg true if the zero-arg version of
     * startAsync was called, false otherwise
     */
    private AsyncContext startAsync(ServletRequest servletRequest,
                ServletResponse servletResponse,
                boolean isStartAsyncWithZeroArg)
            throws IllegalStateException {

        if (servletRequest == null || servletResponse == null) {
            throw new IllegalArgumentException("Null request or response");
        }

        if (!isAsyncSupported()) {
            throw new IllegalStateException(rb.getString(LogFacade.REQUEST_WITHIN_SCOPE_OF_FILTER_OR_SERVLET_EXCEPTION));
        }

        final AsyncContextImpl asyncContextLocal = asyncContext;

        if (asyncContextLocal != null) {
            if (isAsyncStarted()) {
                throw new IllegalStateException(rb.getString(LogFacade.START_ASYNC_CALLED_AGAIN_EXCEPTION));
            }
            if (asyncContextLocal.isAsyncComplete()) {
                throw new IllegalStateException(rb.getString(LogFacade.ASYNC_ALREADY_COMPLETE_EXCEPTION));
            }
            if (!asyncContextLocal.isStartAsyncInScope()) {
                throw new IllegalStateException(rb.getString(LogFacade.START_ASYNC_CALLED_OUTSIDE_SCOPE_EXCEPTION));
            }

            // Reinitialize existing AsyncContext
            asyncContextLocal.reinitialize(servletRequest, servletResponse,
                    isStartAsyncWithZeroArg);
        } else {
            final AsyncContextImpl asyncContextFinal =
                    new AsyncContextImpl(this,
                                         servletRequest,
                                         (Response) getResponse(),
                                         servletResponse,
                                         isStartAsyncWithZeroArg);
            asyncContext = asyncContextFinal;

            final CompletionHandler<org.glassfish.grizzly.http.server.Response> requestCompletionHandler =
                    new EmptyCompletionHandler<org.glassfish.grizzly.http.server.Response>() {

                        @Override
                        public void completed(org.glassfish.grizzly.http.server.Response response) {
                            asyncContextFinal.notifyAsyncListeners(
                                    AsyncContextImpl.AsyncEventType.COMPLETE,
                                    null);
                        }
                    };

            final TimeoutHandler timeoutHandler = response -> processTimeout();

            coyoteRequest.getResponse().suspend(-1, TimeUnit.MILLISECONDS,
                    requestCompletionHandler, timeoutHandler);
            asyncStartedThread = Thread.currentThread();
        }

        asyncStarted.set(true);

        return asyncContext;
    }

    /**
     * Checks whether async processing has started on this request.
     */
    @Override
    public boolean isAsyncStarted() {
        return asyncStarted.get();
    }

    void setAsyncStarted(boolean asyncStarted) {
        this.asyncStarted.set(asyncStarted);
    }

    /**
     * Disables async support for this request.
     *
     * Async support is disabled as soon as this request has passed a filter
     * or servlet that does not support async (either via the designated
     * annotation or declaratively).
     */
    @Override
    public void disableAsyncSupport() {
        isAsyncSupported = false;
    }

    void setAsyncTimeout(long timeout) {
        coyoteRequest.getResponse().getSuspendContext().setTimeout(
                timeout, TimeUnit.MILLISECONDS);;
    }

    /**
     * Checks whether this request supports async.
     */
    @Override
    public boolean isAsyncSupported() {
        return isAsyncSupported;
    }

    /**
     * Gets the AsyncContext of this request.
     */
    @Override
    public AsyncContext getAsyncContext() {
        if (!isAsyncStarted()) {
            throw new IllegalStateException(rb.getString(LogFacade.REQUEST_NOT_PUT_INTO_ASYNC_MODE_EXCEPTION));
        }

        return asyncContext;
    }

    /*
     * Invokes any registered AsyncListener instances at their
     * <tt>onComplete</tt> method
     */
    void asyncComplete() {
        asyncStarted.set(false);

        if (asyncStartedThread != Thread.currentThread() ||
                !asyncContext.isOkToConfigure()) {
            // it's not safe to just mark response as resumed
            coyoteRequest.getResponse().resume();
        } else {
            // This code is called if we startAsync and complete in the service() thread.
            // So instead of resuming the suspendedContext (which will finish the response processing),
            // we just have to mark the context as resumed like it has never been suspended.
            final SuspendedContextImpl suspendContext =
                    (SuspendedContextImpl) coyoteRequest.getResponse().getSuspendContext();

            suspendContext.markResumed();
            suspendContext.getSuspendStatus().reset();
        }
    }

    /*
     * Invokes all registered AsyncListener instances at their
     * <tt>onTimeout</tt> method.
     *
     * This method also performs an error dispatch and completes the response
     * if none of the listeners have done so.
     */
    void asyncTimeout() {
        if (asyncContext != null) {
            asyncContext.notifyAsyncListeners(
                    AsyncContextImpl.AsyncEventType.TIMEOUT, null);
        }
        inputBuffer.disableReadHandler();
        if (response instanceof Response) {
            ((Response)response).disableWriteHandler();
        }
        errorDispatchAndComplete(null);
    }

    /**
     * Notifies this Request that the container-initiated dispatch
     * during which ServletRequest#startAsync was called is about to
     * return to the container
     */
    void onExitService() {
        final AsyncContextImpl ac = asyncContext;

        if (ac != null) {
            ac.setOkToConfigure(false);

            if (asyncStarted.get()) {
                coyoteRequest.getResponse().getSuspendContext().setTimeout(
                        ac.getTimeout(), TimeUnit.MILLISECONDS);
            }

            ac.onExitService();
        }
        afterService = true;
        if (resume) {
            coyoteRequest.getResponse().resume();
        }

    }

    void resumeAfterService() {
        if (afterService) {
            coyoteRequest.getResponse().resume();
        } else {
            resume = true;
        }
    }

    private boolean processTimeout() {
        boolean result = true;
        final AsyncContextImpl asyncContextLocal = this.asyncContext;
        try {
            asyncTimeout();
        } finally {
            result = asyncContextLocal != null && !asyncContextLocal.getAndResetDispatchInScope();
        }

        return result;
    }

    void errorDispatchAndComplete(Throwable t) {
        /*
         * If no listeners, or none of the listeners called
         * AsyncContext#complete or any of the AsyncContext#dispatch
         * methods (in which case asyncStarted would have been set to false),
         * perform an error dispatch with a status code equal to 500.
         */
        final AsyncContextImpl ac = asyncContext;
        if (ac != null
                && !ac.isDispatchInScope()
                && !ac.isAsyncComplete()) {
            ((HttpServletResponse) response).setStatus(
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setError();
            if (t != null) {
                setAttribute(RequestDispatcher.ERROR_EXCEPTION, t);
            }
            try {
                if (hostValve != null) {
                    hostValve.postInvoke(this, response);
                }
            } catch (Exception e) {
                log.log(Level.SEVERE, LogFacade.UNABLE_PERFORM_ERROR_DISPATCH, e);
            } finally {
                /*
                 * If no matching error page was found, or the error page
                 * did not call AsyncContext#complete or any of the
                 * AsyncContext#dispatch methods, call AsyncContext#complete
                 */
                if (!ac.isAsyncComplete()) {
                    ac.complete();
                }
            }
        }
    }

    private Multipart getMultipart() {
        if (multipart == null) {
            multipart = new Multipart(this,
                    wrapper.getMultipartLocation(),
                    wrapper.getMultipartMaxFileSize(),
                    wrapper.getMultipartMaxRequestSize(),
                    wrapper.getMultipartFileSizeThreshold());
        }
        return multipart;
    }

    private boolean isMultipartConfigured() {
        if (wrapper instanceof StandardWrapper) {
            return ((StandardWrapper)wrapper).isMultipartConfigured();
        }
        return false;
    }

    private void checkMultipartConfiguration(String name) {
        if (! isMultipartConfigured()) {
            String msg = MessageFormat.format(rb.getString(LogFacade.REQUEST_CALLED_WITHOUT_MULTIPART_CONFIG_EXCEPTION), name);
            throw new IllegalStateException(msg);
        }
    }

    @Override
    public Collection<Part> getParts() throws IOException, ServletException {
        checkMultipartConfiguration("getParts");
        return getMultipart().getParts();
    }

    @Override
    public Part getPart(String name) throws IOException, ServletException {
        checkMultipartConfiguration("getPart");
        return getMultipart().getPart(name);
    }

    /**
     * Log a message on the Logger associated with our Container (if any).
     *
     * @param message Message to be logged
     * @param t Associated exception
     */
    private void log(String message, Throwable t) {
        org.apache.catalina.Logger logger = null;
        if (connector != null && connector.getContainer() != null) {
            logger = connector.getContainer().getLogger();
        }
        String localName = "Request";
        if (logger != null) {
            logger.log(localName + " " + message, t,
                    org.apache.catalina.Logger.WARNING);
        } else {
            log.log(Level.WARNING, localName + " " + message, t);
        }
    }

    // START SJSAS 6419950
    private void populateSSLAttributes() {
        RequestUtils.populateSSLAttributes(coyoteRequest);
        Object attr = coyoteRequest.getAttribute(Globals.CERTIFICATES_ATTR);
        if (attr != null) {
            attributes.put(Globals.CERTIFICATES_ATTR, attr);
        }
        attr = coyoteRequest.getAttribute(Globals.CIPHER_SUITE_ATTR);
        if (attr != null) {
            attributes.put(Globals.CIPHER_SUITE_ATTR, attr);
        }
        attr = coyoteRequest.getAttribute(Globals.KEY_SIZE_ATTR);
        if (attr != null) {
            attributes.put(Globals.KEY_SIZE_ATTR, attr);
        }
        attr = coyoteRequest.getAttribute(Globals.SSL_SESSION_ID_ATTR);
        if (attr != null) {
            attributes.put(Globals.SSL_SESSION_ID_ATTR, attr);
        }
    }
    // END SJSAS 6419950

    // START GlassFish 896
    private void initSessionTracker() {
        notes.put(Globals.SESSION_TRACKER, sessionTracker);
    }
    // END GlassFish 896

    /**
     * lock the session associated with this request
     * this will be a foreground lock
     * checks for background lock to clear
     * and does a decay poll loop to wait until
     * it is clear; after 5 times it takes control for
     * the foreground
     *
     * @return the session that's been locked
     */
    @Override
    public Session lockSession() {
        Session sess = getSessionInternal(false);
        // Now lock the session
        if (sess != null) {
            long pollTime = 200L;
            int maxNumberOfRetries = 7;
            int tryNumber = 0;
            // Try to lock up to maxNumberOfRetries times.
            // Poll and wait starting with 200 ms.
            while (!sess.lockForeground()) {
                tryNumber++;
                if (tryNumber < maxNumberOfRetries) {
                    pollTime = pollTime * 2L;
                    threadSleep(pollTime);
                } else {
                    // Tried to wait and lock maxNumberOfRetries times.
                    // Unlock the background so we can take over.
                    log.log(Level.WARNING, LogFacade.BREAKING_BACKGROUND_LOCK_EXCEPTION, sess);
                    if (sess instanceof StandardSession) {
                        ((StandardSession) sess).unlockBackground();
                    }
                }
            }
        }

        return sess;
    }

    private void threadSleep(long sleepTime) {
        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            ;
        }
    }

    /**
     * unlock the session associated with this request
     */
    @Override
    public void unlockSession() {
        Session sess = getSessionInternal(false);
        // Now unlock the session
        if (sess != null) {
            sess.unlockForeground();
        }
    }

    /**
     * Increments the version of the given session, and stores it as a
     * request attribute, so it can later be included in a response cookie.
     */
    private void incrementSessionVersion(StandardSession ss,
                                         Context context) {
        if (ss == null || context == null) {
            return;
        }

        String versionString = Long.toString(ss.incrementVersion());

        Map<String, String> sessionVersions = getSessionVersionsRequestAttribute();
        if (sessionVersions == null) {
            sessionVersions = new HashMap<String, String>();
            setAttribute(Globals.SESSION_VERSIONS_REQUEST_ATTRIBUTE,
                         sessionVersions);
        }
        String path = context.getPath();
        if ("".equals(path)) {
            path = "/";
        }
        sessionVersions.put(path, versionString);
    }

    @SuppressWarnings("unchecked")
    Map<String, String> getSessionVersionsRequestAttribute() {
        return (Map<String, String>) getAttribute(
                Globals.SESSION_VERSIONS_REQUEST_ATTRIBUTE);
    }

    private boolean isSessionVersioningSupported() {
        return context != null &&
            context.getManager() != null &&
            context.getManager().isSessionVersioningSupported();
    }
}
