/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2017 Oracle and/or its affiliates. All rights reserved.
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
// Portions Copyright [2018-2022] [Payara Foundation and/or its affiliates]

package com.sun.enterprise.web.connector.coyote;

import com.sun.enterprise.config.serverbeans.ConfigBeansUtilities;
import com.sun.enterprise.config.serverbeans.HttpService;
import com.sun.enterprise.web.WebContainer;
import com.sun.enterprise.web.connector.extension.GrizzlyConfig;
import com.sun.enterprise.web.connector.grizzly.DummyConnectorLauncher;
import com.sun.enterprise.web.pwc.connector.coyote.PwcCoyoteRequest;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.tomcat.util.net.SSLHostConfig;
import org.apache.tomcat.util.net.SSLHostConfigCertificate;
import org.glassfish.grizzly.config.dom.*;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.util.Mapper;
import org.glassfish.web.util.IntrospectionUtils;
import org.apache.catalina.*;
import org.apache.catalina.connector.Connector;
import org.glassfish.security.common.CipherInfo;
import org.glassfish.web.LogFacade;

import java.io.*;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import static org.glassfish.grizzly.config.dom.Ssl.SSL2;
import static org.glassfish.grizzly.config.dom.Ssl.SSL2_HELLO;
import static org.glassfish.grizzly.config.dom.Ssl.SSL3;
import static org.glassfish.grizzly.config.dom.Ssl.TLS1;
import static org.glassfish.grizzly.config.dom.Ssl.TLS11;
import static org.glassfish.grizzly.config.dom.Ssl.TLS12;
import static org.glassfish.grizzly.config.dom.Ssl.TLS13;

@Deprecated(forRemoval = true)
public class PECoyoteConnector extends Connector {

    private static final String DEFAULT_KEYSTORE_TYPE = "JKS";
    private static final String DEFAULT_TRUSTSTORE_TYPE = "JKS";

    private static final String DUMMY_CONNECTOR_LAUNCHER = DummyConnectorLauncher.class.getName();

    protected static final Logger _logger = LogFacade.getLogger();

    protected static final ResourceBundle _rb = _logger.getResourceBundle();

    /**
     * Are we recycling objects
     */
    protected boolean recycleObjects;


     /**
     * The number of acceptor threads.
     */
    protected int maxAcceptWorkerThreads;


    /**
     * The number of reader threads.
     */
    protected int maxReadWorkerThreads;


    /**
     * The request timeout value used by the processor threads.
     */
    protected int processorWorkerThreadsTimeout;


    /**
     * The increment number used by the processor threads.
     *
    protected int minProcessorWorkerThreadsIncrement;
     */


    /**
     * The size of the accept queue.
     */
    protected int minAcceptQueueLength;


    /**
     * The size of the read queue
     */
    protected int minReadQueueLength;


    /**
     * The size of the processor queue.
     */
    protected int minProcessorQueueLength;


    /**
     * Use direct or non direct byte buffer.
     */
    protected boolean useDirectByteBuffer;


    // Are we using the NIO Connector or the CoyoteConnector
    //private boolean coyoteOn = false;

    /*
     * Number of seconds before idle keep-alive connections expire
     */
    private int keepAliveTimeoutInSeconds;

    /*
     * Number of keep-alive threads
     */
    private int keepAliveThreadCount;

    /**
     * Maximum pending connection before refusing requests.
     */
    private int queueSizeInBytes = 4096;

    /**
     * Server socket backlog.
     */
    protected int ssBackLog = 4096;

    /**
     * Set the number of <code>Selector</code> used by Grizzly.
     */
    private int selectorReadThreadsCount = 0;

    /**
     * The monitoring classes used to gather stats.
     */
    protected GrizzlyConfig grizzlyMonitor;

    /**
     * The root folder where application are deployed
     */
    private String rootFolder = "";

    /**
     * Mapper listener.
     */
    protected MapperListener mapperListener;

    protected Mapper mapper;

    // --------------------------------------------- FileCache support --//

    /**
     * Timeout before remove the static resource from the cache.
     */
    private int secondsMaxAge = -1;

    /**
     * The maximum entries in the <code>fileCache</code>
     */
    private int maxCacheEntries = 1024;

    /**
     * The maximum size of a cached resources.
     */
    private long minEntrySize = 2048;

    /**
     * The maximum size of a cached resources.
     */
    private long maxEntrySize = 537600;

    /**
     * The maximum cached bytes
     */
    private long maxLargeFileCacheSize = 10485760;

    /**
     * The maximum cached bytes
     */
    private long maxSmallFileCacheSize = 1048576;

    /**
     * Is the FileCache enabled.
     */
    private boolean fileCacheEnabled = true;

    /**
     * Is the large FileCache enabled.
     */
    private boolean isLargeFileCacheEnabled = true;

    /**
     * Location of the CRL file
     */
    private String crlFile;

    /**
     * The trust management algorithm
     */
    private String trustAlgorithm;

    /**
     * The maximum number of non-self-issued intermediate
     * certificates that may exist in a certification path
     */
    private String trustMaxCertLength;

    private WebContainer webContainer;

    private HttpHandler handler = null;

    private String name;

    private String defaultHost;

    private String instanceName;


    /**
     * Constructor
     */
    public PECoyoteConnector(WebContainer webContainer) {
        super(DUMMY_CONNECTOR_LAUNCHER);
        this.webContainer = webContainer;
    }


    /**
     * Create (or allocate) and return a Request object suitable for
     * specifying the contents of a Request to the responsible ContractProvider.
     */
    @Override
    public Request createRequest() {
        return new PwcCoyoteRequest(this);
    }


    /**
     * Creates and returns Response object.
     *
     * @return Response object
     */
    @Override
    public Response createResponse() {
        return new PECoyoteResponse();
    }


    /**
     * Gets the number of seconds before a keep-alive connection that has
     * been idle times out and is closed.
     *
     * @return Keep-alive timeout in number of seconds
     */
    public int getKeepAliveTimeoutInSeconds() {
        return keepAliveTimeoutInSeconds;
    }


    /**
     * Sets the number of seconds before a keep-alive connection that has
     * been idle times out and is closed.
     *
     * @param timeout Keep-alive timeout in number of seconds
     */
    public void setKeepAliveTimeoutInSeconds(int timeout) {
        keepAliveTimeoutInSeconds = timeout;
        setProperty("keepAliveTimeoutInSeconds", String.valueOf(timeout));
    }


    /**
     * Gets the number of keep-alive threads.
     *
     * @return Number of keep-alive threads
     */
    public int getKeepAliveThreadCount() {
        return keepAliveThreadCount;
    }

    /**
     * Sets the number of keep-alive threads
     *
     * @param number Number of keep-alive threads
     */
    public void setKeepAliveThreadCount(int number) {
        keepAliveThreadCount = number;
        setProperty("KeepAliveThreadCount", String.valueOf(number));
    }

    /**
     * Set the maximum pending connection this <code>Connector</code>
     * can handle.
     */
    public void setQueueSizeInBytes(int queueSizeInBytes){
        this.queueSizeInBytes = queueSizeInBytes;
        setProperty("queueSizeInBytes", String.valueOf(queueSizeInBytes));
    }


    /**
     * Return the maximum pending connection.
     */
    public int getQueueSizeInBytes(){
        return queueSizeInBytes;
    }


    /**
     * Set the <code>SocketServer</code> backlog.
     */
    public void setSocketServerBacklog(int ssBackLog){
        this.ssBackLog = ssBackLog;
        setProperty("socketServerBacklog", String.valueOf(ssBackLog));
    }


    /**
     * Return the maximum pending connection.
     */
    public int getSocketServerBacklog(){
        return ssBackLog;
    }


    /**
     * Set the <code>recycle-tasks</code> used by this <code>Selector</code>
     */
    public void setRecycleObjects(boolean recycleObjects){
        this.recycleObjects= recycleObjects;
        setProperty("recycleObjects",
                    String.valueOf(recycleObjects));
    }


    /**
     * Return the <code>recycle-tasks</code> used by this
     * <code>Selector</code>
     */
    public boolean getRecycleObjects(){
        return recycleObjects;
    }


    /**
     * Set the <code>reader-thread</code> from domian.xml.
     */
    public void setMaxReadWorkerThreads(int maxReadWorkerThreads){
        this.maxReadWorkerThreads = maxReadWorkerThreads;
        setProperty("maxReadWorkerThreads",
                    String.valueOf(maxReadWorkerThreads));
    }


    /**
     * Return the <code>read-thread</code> used by this <code>Selector</code>
     */
    public int getMaxReadWorkerThreads(){
        return maxReadWorkerThreads;
    }


    /**
     * Set the <code>reader-thread</code> from domian.xml.
     */
    public void setMaxAcceptWorkerThreads(int maxAcceptWorkerThreads){
        this.maxAcceptWorkerThreads = maxAcceptWorkerThreads;
        setProperty("maxAcceptWorkerThreads",
                    String.valueOf(maxAcceptWorkerThreads));
    }


    /**
     * Return the <code>read-thread</code> used by this <code>Selector</code>
     */
    public int getMaxAcceptWorkerThreads(){
        return maxAcceptWorkerThreads;
    }


    /**
     * Set the <code>acceptor-queue-length</code> value
     * on this <code>Selector</code>
     */
    public void setMinAcceptQueueLength(int minAcceptQueueLength){
        this.minAcceptQueueLength = minAcceptQueueLength;
        setProperty("minAcceptQueueLength",
                    String.valueOf(minAcceptQueueLength));
    }


    /**
     * Return the <code>acceptor-queue-length</code> value
     * on this <code>Selector</code>
     */
    public int getMinAcceptQueueLength(){
        return minAcceptQueueLength;
    }


    /**
     * Set the <code>reader-queue-length</code> value
     * on this <code>Selector</code>
     */
    public void setMinReadQueueLength(int minReadQueueLength){
        this.minReadQueueLength = minReadQueueLength;
        setProperty("minReadQueueLength",
                    String.valueOf(minReadQueueLength));
    }


    /**
     * Return the <code>reader-queue-length</code> value
     * on this <code>Selector</code>
     */
    public int getMinReadQueueLength(){
        return minReadQueueLength;
    }


    /**
     * Set the <code>processor-queue-length</code> value
     * on this <code>Selector</code>
     */
    public void setMinProcessorQueueLength(int minProcessorQueueLength){
        this.minProcessorQueueLength = minProcessorQueueLength;
        setProperty("minProcessorQueueLength",
                    String.valueOf(minProcessorQueueLength));
    }


    /**
     * Return the <code>processor-queue-length</code> value
     * on this <code>Selector</code>
     */
    public int getMinProcessorQueueLength(){
        return minProcessorQueueLength;
    }


    /**
     * Set the <code>use-nio-non-blocking</code> by this <code>Selector</code>
     */
    public void setUseDirectByteBuffer(boolean useDirectByteBuffer){
        this.useDirectByteBuffer = useDirectByteBuffer;
        setProperty("useDirectByteBuffer",
                    String.valueOf(useDirectByteBuffer));
    }


    /**
     * Return the <code>use-nio-non-blocking</code> used by this
     * <code>Selector</code>
     */
    public boolean getUseDirectByteBuffer(){
        return useDirectByteBuffer;
    }

    public void setProcessorWorkerThreadsTimeout(int timeout){
        processorWorkerThreadsTimeout = timeout;
        setProperty("processorWorkerThreadsTimeout",
                    String.valueOf(timeout));
    }

    public int getProcessorWorkerThreadsTimeout(){
        return processorWorkerThreadsTimeout;
    }

    /*public int getMinProcessorWorkerThreadsIncrement(){
        return minProcessorWorkerThreadsIncrement;
    }*/

    public void setSelectorReadThreadsCount(int selectorReadThreadsCount) {
        this.selectorReadThreadsCount = selectorReadThreadsCount;
        setProperty("selectorReadThreadsCount",
                    String.valueOf(selectorReadThreadsCount));
    }

    public int getSelectorReadThreadsCount() {
        return selectorReadThreadsCount;
    }

    @Override
    public void startInternal() throws LifecycleException {
        super.startInternal();

        String domain = getDomainInternal();
        if (domain != null) {
            if (!"admin-listener".equals(getName())) {
                // See IT 8255
                mapper.removeContext(getDefaultHost(), "");
                mapper.removeHost(getDefaultHost());
            }
            mapperListener.setDomain(domain);
            // BEGIN S1AS 5000999
            mapperListener.setNetworkListenerName(this.getName());
            mapperListener.setDefaultHost(getDefaultHost());
            // END S1AS 5000999
            mapperListener.setInstanceName(getInstanceName());
            mapperListener.init();
        }
        if ( grizzlyMonitor != null ) {
            grizzlyMonitor.initConfig();
            grizzlyMonitor.registerMonitoringLevelEvents();
        }
    }

    @Override
    public void stopInternal() throws LifecycleException {
        super.stopInternal();
        if ( grizzlyMonitor != null ) {
            grizzlyMonitor.destroy();
            grizzlyMonitor=null;
        }
    }

    //------------------------------------------------- FileCache config -----/

    /**
     * The timeout in seconds before remove a <code>FileCacheEntry</code>
     * from the <code>fileCache</code>
     */
    public void setSecondsMaxAge(int sMaxAges) {
        secondsMaxAge = sMaxAges;
        setProperty("secondsMaxAge", String.valueOf(secondsMaxAge));
    }

    public int getSecondsMaxAge() {
        return secondsMaxAge;
    }

    /**
     * Set the maximum entries this cache can contains.
     */
    public void setMaxCacheEntries(int mEntries){
        maxCacheEntries = mEntries;
        setProperty("maxCacheEntries", String.valueOf(maxCacheEntries));
    }

    /**
     * Return the maximum entries this cache can contains.
     */
    public int getMaxCacheEntries(){
        return maxCacheEntries;
    }


    /**
     * Set the maximum size a <code>FileCacheEntry</code> can have.
     */
    public void setMinEntrySize(long mSize){
        minEntrySize = mSize;
        setProperty("minEntrySize", String.valueOf(minEntrySize));
    }


    /**
     * Get the maximum size a <code>FileCacheEntry</code> can have.
     */
    public long getMinEntrySize(){
        return minEntrySize;
    }


    /**
     * Set the maximum size a <code>FileCacheEntry</code> can have.
     */
    public void setMaxEntrySize(long mEntrySize){
        maxEntrySize = mEntrySize;
        setProperty("maxEntrySize", String.valueOf(maxEntrySize));
    }


    /**
     * Get the maximum size a <code>FileCacheEntry</code> can have.
     */
    public long getMaxEntrySize(){
        return maxEntrySize;
    }


    /**
     * Set the maximum cache size
     */
    public void setMaxLargeCacheSize(long mCacheSize){
        maxLargeFileCacheSize = mCacheSize;
        setProperty("maxLargeFileCacheSize",
                String.valueOf(maxLargeFileCacheSize));
    }


    /**
     * Get the maximum cache size
     */
    public long getMaxLargeCacheSize(){
        return maxLargeFileCacheSize;
    }


    /**
     * Set the maximum cache size
     */
    public void setMaxSmallCacheSize(long mCacheSize){
        maxSmallFileCacheSize = mCacheSize;
        setProperty("maxSmallFileCacheSize",
                String.valueOf(maxSmallFileCacheSize));
    }


    /**
     * Get the maximum cache size
     */
    public long getMaxSmallCacheSize(){
        return maxSmallFileCacheSize;
    }


    /**
     * Is the fileCache enabled.
     */
    public boolean isFileCacheEnabled(){
        return fileCacheEnabled;
    }


    /**
     * Is the file caching mechanism enabled.
     */
    public void setFileCacheEnabled(boolean fileCacheEnabled){
        this.fileCacheEnabled = fileCacheEnabled;
        setProperty("fileCacheEnabled",String.valueOf(fileCacheEnabled));
    }


    /**
     * Is the large file cache support enabled.
     */
    public void setLargeFileCacheEnabled(boolean isLargeEnabled){
        isLargeFileCacheEnabled = isLargeEnabled;
        setProperty("largeFileCacheEnabled",
                String.valueOf(isLargeFileCacheEnabled));
    }


    /**
     * Is the large file cache support enabled.
     */
    public boolean getLargeFileCacheEnabled(){
        return isLargeFileCacheEnabled;
    }

    // --------------------------------------------------------------------//


    /**
     * Set the documenr root folder
     */
    public void setWebAppRootPath(String rootFolder){
        this.rootFolder = rootFolder;
        setProperty("webAppRootPath",rootFolder);
    }


    /**
     * Return the folder's root where application are deployed.
     */
    public String getWebAppRootPath(){
        return rootFolder;
    }


    /**
     * Initialize this connector.
     */
    @Override
    public void initInternal() throws LifecycleException {
        super.initInternal();
        mapperListener = new MapperListener(mapper, webContainer);
        // Set the monitoring.
        grizzlyMonitor = new GrizzlyConfig(webContainer, getDomainInternal(), getPort());
    }


    /**
     * Gets the location of the CRL file
     *
     * @return The location of the CRL file
     */
    public String getCrlFile() {
         return crlFile;
    }


    /**
     * Sets the location of the CRL file.
     *
     * @param crlFile The location of the CRL file
     */
    public void setCrlFile(String crlFile) {
        this.crlFile = crlFile;
        setProperty("crlFile", crlFile);
    }


    /**
     * Gets the trust management algorithm
     *
     * @return The trust management algorithm
     */
    public String getTrustAlgorithm() {
         return trustAlgorithm;
    }


    /**
     * Sets the trust management algorithm
     *
     * @param trustAlgorithm The trust management algorithm
     */
    public void setTrustAlgorithm(String trustAlgorithm) {
        this.trustAlgorithm = trustAlgorithm;
        setProperty("truststoreAlgorithm", trustAlgorithm);
    }


    /**
     * Gets the maximum number of non-self-issued intermediate
     * certificates that may exist in a certification path
     *
     * @return The maximum number of non-self-issued intermediate
     * certificates that may exist in a certification path
     */
    public String getTrustMaxCertLength() {
         return trustMaxCertLength;
    }


    /**
     * Sets the maximum number of non-self-issued intermediate
     * certificates that may exist in a certification path
     *
     * @param trustMaxCertLength The maximum number of non-self-issued
     * intermediate certificates that may exist in a certification path
     */
    public void setTrustMaxCertLength(String trustMaxCertLength) {
        this.trustMaxCertLength = trustMaxCertLength;
        setProperty("trustMaxCertLength", trustMaxCertLength);
    }

    /**
     * Gets the MapperListener of this connector.
     *
     * @return The MapperListener of this connector
     */
    public MapperListener getMapperListener() {
        return mapperListener;
    }


    /*
     * Configures this connector.
     *
     * @param listener The http-listener that corresponds to the given
     * connector
     * @param isSecure true if the connector is security-enabled, false
     * otherwise
     * @param httpServiceProps The http-service properties
     */
    public void configure(NetworkListener listener, boolean isSecure,
        HttpService httpService) {

        final Transport transport = listener.findTransport();
        try {
            setSocketServerBacklog(
                Integer.parseInt(transport.getMaxConnectionsCount()));
        } catch (NumberFormatException ex) {
            String msg = MessageFormat.format(
                _rb.getString(LogFacade.INVALID_MAX_PENDING_COUNT), transport.getMaxConnectionsCount(),
                Integer.toString(getSocketServerBacklog()));
            _logger.log(Level.WARNING, msg, ex);
        }
        /* TODO
        //WebContainerFeatureFactory wcFeatureFactory = _serverContext.getDefaultHabitat().getService(WebContainerFeatureFactory.class);
        String sslImplementationName =
            webFeatureFactory.getSSLImplementationName();

        if (sslImplementationName != null) {
            connector.setProperty("sSLImplementation",sslImplementationName);
        }*/

        setDomain(webContainer.getServerContext().getDefaultDomainName());

        configureSSL(listener);
        configureThreadPool(listener.findThreadPool());

        final Http http = listener.findHttpProtocol().getHttp();

        configureFileCache(http.getFileCache());
        setProperty("maxHttpHeaderSize", http.getSendBufferSizeBytes());
        setDefaultHost(http.getDefaultVirtualServer());
        setEnableLookups(ConfigBeansUtilities.toBoolean(http.getDnsLookupEnabled()));

        setXpoweredBy(Boolean.valueOf(http.getXpoweredBy()));

        // Application root
        setWebAppRootPath(webContainer.getModulesRoot().getAbsolutePath());

        // server-name (may contain scheme and colon-separated port number)
        String serverName = http.getServerName();
        if (serverName != null && serverName.length() > 0) {
            // Ignore scheme, which was required for webcore issued redirects
            // in 8.x EE
            if (serverName.startsWith("http://")) {
                serverName = serverName.substring("http://".length());
            } else if (serverName.startsWith("https://")) {
                serverName = serverName.substring("https://".length());
            }
            int index = serverName.indexOf(':');
            if (index != -1) {
                setProxyName(serverName.substring(0, index).trim());
                String serverPort = serverName.substring(index+1).trim();
                if (serverPort.length() > 0) {
                    try {
                        setProxyPort(Integer.parseInt(serverPort));
                    } catch (NumberFormatException nfe) {
                        _logger.log(Level.SEVERE,
                            LogFacade.INVALID_PROXY_PORT,
                            new Object[] { serverPort, listener.getName() });
		    }
                }
            } else {
                setProxyName(serverName);
            }
        }

        // redirect-port
        String redirectPort = http.getRedirectPort();
        if (redirectPort != null && redirectPort.length() != 0) {
            try {
                setRedirectPort(Integer.parseInt(redirectPort));
            } catch (NumberFormatException nfe) {
                _logger.log(Level.WARNING,
                    LogFacade.INVALID_REDIRECT_PORT,
                    new Object[] {
                        redirectPort,
                        listener.getName(),
                        Integer.toString(getRedirectPort()) });
            }
        } else {
            setRedirectPort(-1);
        }

        // acceptor-threads
        String acceptorThreads = transport.getAcceptorThreads();
        if (acceptorThreads != null) {
            try {
                setSelectorReadThreadsCount(Integer.parseInt(
                    acceptorThreads));
            } catch (NumberFormatException nfe) {
                _logger.log(Level.WARNING, LogFacade.INVALID_ACCEPTOR_THREADS, new Object[] {acceptorThreads,
                        listener.getName(), getProperty("maxThreads")});
            }
        }

        // Configure Connector with keystore password and location
        if (isSecure) {
            configureKeysAndCerts();
        }

        webContainer.configureHttpServiceProperties(httpService, this);

        // Overrided http-service property if defined.
        configureHttpListenerProperties(listener);
    }


    /*
     * Configures this connector for modjk.
     */
    public void configureJKProperties(final NetworkListener listener) {

        File propertiesFile = null;
        if (listener != null) {
            propertiesFile = new File(listener.getJkConfigurationFile());
        }
        String propertyFile = System.getProperty("com.sun.enterprise.web.connector.enableJK.propertyFile");
        if (propertiesFile!=null && !propertiesFile.exists() && propertyFile!=null) {
            propertiesFile   = new File(propertyFile);
        }
        if (propertiesFile==null) {
            if (_logger.isLoggable(Level.FINEST)) {
                _logger.log(Level.FINEST, LogFacade.JK_PROPERTIES_NOT_DEFINED);
            }
            return;
        }

        if (!propertiesFile.exists()) {
            _logger.log(Level.WARNING,
                    MessageFormat.format(_rb.getString("pewebcontainer.missingJKProperties"),
                            propertiesFile.getAbsolutePath()));
            return;
        }

        if (_logger.isLoggable(Level.FINEST)) {
            _logger.log(Level.FINEST,
                    MessageFormat.format(_rb.getString(LogFacade.LOADING_JK_PROPERTIED),
                            propertiesFile.getAbsolutePath()));
        }

        try (InputStream is = new BufferedInputStream(new FileInputStream(propertiesFile))) {
            Properties properties = new Properties();
            properties.load(is);
            Enumeration enumeration = properties.keys();
            while (enumeration.hasMoreElements()) {
                String propName = (String) enumeration.nextElement();
                String value = properties.getProperty(propName);
                if (value != null) {
                    IntrospectionUtils.setProperty(this, propName, value);
                }
            }
        } catch (Exception ex) {
            String msg = MessageFormat.format(_rb.getString(LogFacade.UNABLE_TO_CONFIGURE_JK), propertiesFile, getPort());
            _logger.log(Level.SEVERE, msg, ex);
        }
    }

    /**
     * Configure the Grizzly FileCache mechanism
     * @param fileCache
     */
    public void configureFileCache(FileCache fileCache) {
        if (fileCache != null) {
            setLargeFileCacheEnabled(ConfigBeansUtilities.toBoolean(
                fileCache.getEnabled()));
            if (fileCache.getMaxAgeSeconds() != null) {
                setSecondsMaxAge(Integer.parseInt(fileCache.getMaxAgeSeconds()));
            }
            if (fileCache.getMaxFilesCount() != null) {
                setMaxCacheEntries(Integer.parseInt(
                    fileCache.getMaxFilesCount()));
            }
            if (fileCache.getMaxCacheSizeBytes() != null) {
                setMaxLargeCacheSize(Integer.parseInt(fileCache.getMaxCacheSizeBytes()));
            }
        }
    }


    /**
     * Configures this connector from the given thread-pool
     * configuration bean.
     *
     * @param pool the thread-pool configuration bean
     */
    public void configureThreadPool(ThreadPool pool){
        if (pool != null) {
            try {
                setProperty("maxThreads", pool.getMaxThreadPoolSize());
            } catch (NumberFormatException ex) {
                String msg = MessageFormat.format(_rb.getString(LogFacade.INVALID_THREAD_POOL_ATTRIBUTE), "max-thread-pool-size");
                _logger.log(Level.WARNING, msg, ex);
            }
            try {
                setProperty("minSpareThreads", pool.getMinThreadPoolSize());
            } catch (NumberFormatException ex) {
                String msg = MessageFormat.format(_rb.getString(LogFacade.INVALID_THREAD_POOL_ATTRIBUTE), "min-thread-pool-size");
                _logger.log(Level.WARNING, msg, ex);
            }
            try {
                setQueueSizeInBytes(Integer.parseInt(
                    pool.getMaxQueueSize()));
            } catch (NumberFormatException ex) {
                String msg = MessageFormat.format(_rb.getString(LogFacade.INVALID_THREAD_POOL_ATTRIBUTE), "max-queue-size");
                _logger.log(Level.WARNING, msg, ex);
            }
        }
    }

    public void configHttpProperties(Http http, Transport transport, Ssl ssl) {
        setAllowTrace(ConfigBeansUtilities.toBoolean(http.getTraceEnabled()));
        setProperty("maxKeepAliveRequests", http.getMaxConnections());
        setKeepAliveTimeoutInSeconds(Integer.parseInt(http.getTimeoutSeconds()));
        setMaxPostSize(Integer.parseInt(http.getMaxPostSizeBytes()));
        setMaxSavePostSize(Integer.parseInt(http.getMaxSavePostSizeBytes()));
        setProperty("compression", http.getCompression());
        setProperty("compressableMimeType", http.getCompressableMimeType());
        setProperty("compressionLevel", http.getCompressionLevel());
        setProperty("compressionStrategy", http.getCompressionStrategy());
        if (http.getNoCompressionUserAgents() != null) {
            setProperty("noCompressionUserAgents", http.getNoCompressionUserAgents());
        }
        setProperty("compressionMinSize", http.getCompressionMinSizeBytes());
        if (http.getRestrictedUserAgents() != null) {
            setProperty("restrictedUserAgents", http.getRestrictedUserAgents());
        }
        setProperty("cometSupport",
                Boolean.valueOf(ConfigBeansUtilities.toBoolean(http.getCometSupportEnabled())).toString());
        setProperty("rcmSupport",
                Boolean.valueOf(ConfigBeansUtilities.toBoolean(http.getRcmSupportEnabled())).toString());
        setProperty("connectionUploadTimeout", http.getConnectionUploadTimeoutMillis());
        // Get property as a boolean to convert "" to false, then reverse value due to enabled vs. disabled difference,
        // and finally convert back to String
        setProperty("disableUploadTimeout", Boolean.toString(
                !ConfigBeansUtilities.toBoolean(http.getUploadTimeoutEnabled())));
        setURIEncoding(http.getUriEncoding());
        configSslOptions(ssl);
    }

    private void configSslOptions(final Ssl ssl) {
        if (ssl != null) {
            if (ssl.getCrlFile() != null) {
                setCrlFile(ssl.getCrlFile());
            }
            if (ssl.getTrustAlgorithm() != null) {
                setTrustAlgorithm(ssl.getTrustAlgorithm());
            }
            if (ssl.getTrustMaxCertLength() != null) {
                setTrustMaxCertLength(ssl.getTrustMaxCertLength());
            }
        }
    }

    /*
     * Configures the SSL properties on this PECoyoteConnector from the
     * SSL config of the given HTTP listener.
     *
     * @param listener HTTP listener whose SSL config to use
     */
    private void configureSSL(NetworkListener listener) {

        Ssl sslConfig = listener.findHttpProtocol().getSsl();
        if (sslConfig == null) {
            return;
        }

        SSLHostConfig sslHostConfig = new SSLHostConfig();

        // client-auth
        if (Boolean.valueOf(sslConfig.getClientAuthEnabled())) {
            sslHostConfig.setCertificateVerification("required");
        }

        // ssl protocol variants
        List<String> sslProtocolsBuf = new ArrayList<>();
        if (Boolean.valueOf(sslConfig.getSsl2Enabled())) {
            sslProtocolsBuf.add(SSL2);
        }
        if (Boolean.valueOf(sslConfig.getSsl3Enabled())) {
            sslProtocolsBuf.add(SSL3);
        }
        if (Boolean.valueOf(sslConfig.getTlsEnabled())) {
            sslProtocolsBuf.add(TLS1);
        }
        if (Boolean.valueOf(sslConfig.getTls11Enabled())) {
            sslProtocolsBuf.add(TLS11);
        }
        if (Boolean.valueOf(sslConfig.getTls12Enabled())) {
            sslProtocolsBuf.add(TLS12);
        }
        if (Boolean.valueOf(sslConfig.getTls13Enabled())) {
            sslProtocolsBuf.add(TLS13);
        }
        if (Boolean.valueOf(sslConfig.getSsl3Enabled())
                || Boolean.valueOf(sslConfig.getTlsEnabled())) {
            sslProtocolsBuf.add(SSL2_HELLO);
        }

        if (sslProtocolsBuf.isEmpty()) {
            _logger.log(Level.WARNING, LogFacade.ALL_SSL_PROTOCOLS_DISABLED, listener.getName());
        } else {
            sslHostConfig.setEnabledProtocols(sslProtocolsBuf.toArray(new String[0]));
        }

        // cert-nickname
        String certNickname = sslConfig.getCertNickname();
        if (certNickname != null && certNickname.length() > 0) {
            SSLHostConfigCertificate sslHostConfigCertificate = new SSLHostConfigCertificate(
                    sslHostConfig, SSLHostConfigCertificate.Type.UNDEFINED);
            sslHostConfigCertificate.setCertificateKeyAlias(sslConfig.getCertNickname());
            sslHostConfig.addCertificate(sslHostConfigCertificate);
        }

        // ssl3-tls-ciphers
        String ciphers = sslConfig.getSsl3TlsCiphers();
        if (ciphers != null) {
            String jsseCiphers = getJSSECiphers(ciphers);
            if (jsseCiphers == null) {
                if (_logger.isLoggable(Level.FINE)) {
                    _logger.log(Level.FINE, LogFacade.ALL_CIPHERS_DISABLED, listener.getName());
                }
            } else {
                sslHostConfig.setCiphers(jsseCiphers);
            }
        }

        addSslHostConfig(sslHostConfig);
    }


    /*
     * Configures this connector with its keystore and truststore.
     */
    private void configureKeysAndCerts() {

        // Keystore
        String prop = System.getProperty("javax.net.ssl.keyStore");
        String keyStoreType = System.getProperty("javax.net.ssl.keyStoreType", DEFAULT_KEYSTORE_TYPE);

        if (prop != null) {
            // For each SSLHostConfig, set the Certificate.certificateKeystoreFile and
            // Certificate.certificateKeystoreType properties - these properties are set on the sslHostConfig itself,
            // not on the SSLHostConfigCertificate object.
            // Certificate.certificateKeystoreFile property used instead of the
            // SSLHostConfigCertificate#setCertificateKeyStore method since that's specific to each individual
            // SSLHostConfigCertificate
            for (SSLHostConfig sslHostConfig : findSslHostConfigs()) {
                Set<SSLHostConfigCertificate> sslHostConfigCertificates = sslHostConfig.getCertificates();
                // Since the Certificate.certificateKeystoreFile and Certificate.certificateKeystoreType properties is
                // set on the sslHostConfig itself, we can just grab the first certificate rather than iterating over
                // all of them (setProperty on SSLHostConfig is not a public method)
                if (!sslHostConfigCertificates.isEmpty()) {
                    SSLHostConfigCertificate sslHostConfigCertificate = sslHostConfigCertificates.iterator().next();
                    sslHostConfigCertificate.setCertificateKeystoreFile(prop);
                    sslHostConfigCertificate.setCertificateKeystoreType(keyStoreType);

                }
            }
        }

	    // Truststore
        prop = System.getProperty("javax.net.ssl.trustStore");
        if (prop != null) {
            for (SSLHostConfig sslHostConfig : findSslHostConfigs()) {
                sslHostConfig.setTruststoreFile(prop);
                sslHostConfig.setTruststoreType(DEFAULT_TRUSTSTORE_TYPE);
            }
        }
    }


    /**
     * Configure http-listener properties
     */
    private void configureHttpListenerProperties(NetworkListener listener) {
        // Configure Connector with <http-service> properties
        configHttpProperties(listener.findHttpProtocol().getHttp(),
            listener.findTransport(), listener.findHttpProtocol().getSsl());
    }


    /*
     * Parses the given comma-separated string of cipher suite names,
     * converts each cipher suite that is enabled (i.e., not preceded by a
     * '-') to the corresponding JSSE cipher suite name, and returns a string
     * of comma-separated JSSE cipher suite names.
     *
     * @param sslCiphers String of SSL ciphers to parse
     *
     * @return String of comma-separated JSSE cipher suite names, or null if
     * none of the cipher suites in the given string are enabled or can be
     * mapped to corresponding JSSE cipher suite names
     */
    private String getJSSECiphers(String ciphers) {
        String cipher = null;
        StringBuilder enabledCiphers = null;
        boolean first = true;
        int index = ciphers.indexOf(',');
        if (index != -1) {
            int fromIndex = 0;
            while (index != -1) {
                cipher = ciphers.substring(fromIndex, index).trim();
                if (cipher.length() > 0 && !cipher.startsWith("-")) {
                    if (cipher.startsWith("+")) {
                        cipher = cipher.substring(1);
                    }
                    String jsseCipher = getJSSECipher(cipher);
                    if (jsseCipher == null) {
                        _logger.log(Level.WARNING,
                            LogFacade.UNRECOGNIZED_CIPHER, cipher);
                    } else {
                        if (enabledCiphers == null) {
                            enabledCiphers = new StringBuilder();
                        }
                        if (!first) {
                            enabledCiphers.append(", ");
                        } else {
                            first = false;
                        }
                        enabledCiphers.append(jsseCipher);
                    }
                }
                fromIndex = index + 1;
                index = ciphers.indexOf(',', fromIndex);
            }
            cipher = ciphers.substring(fromIndex);
        } else {
            cipher = ciphers;
        }
        if (cipher != null) {
            cipher = cipher.trim();
            if (cipher.length() > 0 && !cipher.startsWith("-")) {
                if (cipher.startsWith("+")) {
                    cipher = cipher.substring(1);
                }
                String jsseCipher = getJSSECipher(cipher);
                if (jsseCipher == null) {
                    _logger.log(Level.WARNING,
                        LogFacade.UNRECOGNIZED_CIPHER, cipher);
                } else {
                    if (enabledCiphers == null) {
                        enabledCiphers = new StringBuilder();
                    }
                    if (!first) {
                        enabledCiphers.append(", ");
                    } else {
                        first = false;
                    }
                    enabledCiphers.append(jsseCipher);
                }
            }
        }
        return enabledCiphers == null ? null : enabledCiphers.toString();
    }


    /*
     * Converts the given cipher suite name to the corresponding JSSE cipher.
     *
     * @param cipher The cipher suite name to convert
     *
     * @return The corresponding JSSE cipher suite name, or null if the given
     * cipher suite name can not be mapped
     */
    private String getJSSECipher(String cipher) {

        String jsseCipher = null;

        CipherInfo ci = CipherInfo.getCipherInfo(cipher);
        if( ci != null ) {
            jsseCipher = ci.getCipherName();
        }

        return jsseCipher;
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
     * Set the <code>Adapter</code> used by this connector.
     */
    public void setHandler(HttpHandler handler){
        this.handler = handler;
    }

    /**
     * Get the <code>Adapter</code> used by this connector.
     */
    public HttpHandler getHandler(){
        return handler;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the default host of this Connector.
     *
     * @return The default host of this Connector
     */
    public String getDefaultHost() {
        return defaultHost;
    }

    /**
     * Sets the default host for this Connector.
     *
     * @param defaultHost The default host for this Connector
     */
    public void setDefaultHost(String defaultHost) {
        this.defaultHost = defaultHost;
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
}

