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
 */

package com.sun.enterprise.admin.remote;

import com.sun.enterprise.admin.util.AdminLoggerInfo;
import com.sun.enterprise.config.serverbeans.SecureAdmin;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.SSLException;


import org.glassfish.api.admin.*;
import org.glassfish.api.admin.CommandModel.ParamModel;

import com.sun.enterprise.universal.i18n.LocalStringsImpl;
import com.sun.enterprise.universal.io.SmartFile;
import com.sun.enterprise.universal.GFBase64Encoder;
import com.sun.enterprise.admin.util.CommandModelData.ParamModelData;
import com.sun.enterprise.admin.util.AuthenticationInfo;
import com.sun.enterprise.admin.util.CachedCommandModel;
import com.sun.enterprise.admin.util.HttpConnectorAddress;
import com.sun.enterprise.admin.util.cache.AdminCacheUtils;
import com.sun.enterprise.util.io.FileUtils;
import com.sun.enterprise.util.net.NetUtils;
import org.glassfish.admin.payload.PayloadFilesManager;
import org.glassfish.admin.payload.PayloadImpl;
import org.glassfish.api.admin.Payload;
import javax.xml.parsers.*;
import org.glassfish.common.util.admin.AuthTokenManager;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

/**
 * Utility class for executing remote admin commands.
 * Each instance of RemoteAdminCommand represents a particular
 * remote command on a particular remote server accessed using
 * particular credentials.  The instance can be reused to execute
 * the same command multiple times with different arguments.
 * <p>
 * Arguments to the command are supplied using a ParameterMap
 * passed to the executeCommand method.
 * ParameterMap is a MultiMap where each key can have multiple
 * values, although this class only supports a single value for
 * each option.  Operands for the command are stored as the option
 * named "DEFAULT" and can have multiple values.
 * <p>
 * Before a command can be executed, the metadata for the command
 * (in the form of a CommandModel) is required.  The getCommandModel
 * method will fetch the metadata from the server, save it, and
 * return it.  If the CommandModel for a command is known
 * independently (e.g., stored in a local cache, or known a priori),
 * it can be set using the setCommandModel method.  If the
 * metadata isn't known when the exectureCommand method is
 * called, it will fetch the metadata from the server before executing
 * the command.
 * <p>
 * Any files returned by the command will be stored in the current
 * directory.  The setFileOutputDirectory method can be used to control
 * where returned files are saved.
 * 
 * <p>
 * <b>This implementation is now in retention period. All content was migrated
 * to RemoteRestAdminCommand. This implementation will be removed just after
 * all necessary changes and tests will be done.</b>
 */
public class RemoteAdminCommand {

    private static final LocalStringsImpl strings =
            new LocalStringsImpl(RemoteAdminCommand.class);

    private static final String QUERY_STRING_INTRODUCER = "?";
    private static final String QUERY_STRING_SEPARATOR = "&";
    private static final String ADMIN_URI_PATH = "/__asadmin/";
    private static final String COMMAND_NAME_REGEXP =
                                    "^[a-zA-Z_][-a-zA-Z0-9_]*$";
    private static final String READ_TIMEOUT = "AS_ADMIN_READTIMEOUT";
    public static final String COMMAND_MODEL_MATCH_HEADER = "X-If-Command-Model-Match";
    private static final int defaultReadTimeout; // read timeout for URL conns

    private String              responseFormatType = "hk2-agent";
    private OutputStream        userOut;
    // return output string rather than printing it
    protected String              output;
    private Map<String, String> attrs;
    private boolean             doUpload = false;
    private boolean             addedUploadOption = false;
    private Payload.Outbound    outboundPayload;
    private String              usage;
    private File                fileOutputDir;
    private StringBuilder       passwordOptions;

    // constructor parameters
    protected String            name;
    protected String            host;
    private String              canonicalHostCache; //Used by getCanonicalHost() to cache resolved value
    protected int               port;
    protected boolean           secure;
    protected String            user;
    protected String            password;
    protected Logger            logger;
    protected String            scope;
    protected String            authToken = null;
    protected boolean           prohibitDirectoryUploads = false;

    // executeCommand parameters
    protected ParameterMap      options;
    protected List<String>      operands;

    private CommandModel        commandModel;
    private boolean             commandModelFromCache = false;
    private StringBuilder       metadataErrors; // XXX
    private int                 readTimeout = defaultReadTimeout;
    private int                 connectTimeout = -1;
    private boolean             interactive = true;
    private boolean             omitCache = true;

    private List<Header>        requestHeaders = new ArrayList<Header>();

    /*
     * Set a default read timeout for URL connections.
     */
    static {
        String rt = System.getProperty(READ_TIMEOUT);
        if (rt == null) {
            rt = System.getenv(READ_TIMEOUT);
        }
        if (rt != null) {
            defaultReadTimeout = Integer.parseInt(rt);
        } else {
            defaultReadTimeout = 10 * 60 * 1000;       // 10 minutes
        }
    }

    /**
     * content-type used for each file-transfer part of a payload to or from
     * the server
     */
    private static final String FILE_PAYLOAD_MIME_TYPE =
            "application/octet-stream";

    /**
     * Interface to enable factoring out common HTTP connection management code.
     * <p>
     * The implementation of this interface must implement
     * <ul>
     * <li>{@link #prepareConnection} - to perform all pre-connection configuration - set headers, chunking, etc.
     * as well as writing any payload to the outbound connection.  In short
     * anything needed prior to the URLConnection#connect invocation.
     * <p>
     * The caller will invoke this method after it has invoked {@link URL#openConnection}
     * but before it invokes {@link URL#connect}.
     * <li>{@link #useConnection} - to read from the
     * input stream, etc.  The caller will invoke this method after it has
     * successfully invoked {@link URL#connect}. 
     * </ul>
     * Because the caller might have to work with multiple URLConnection objects
     * (as it follows redirection, for example) this contract allows the caller
     * to delegate to the HttpCommand implementation multiple times to configure
     * each of the URLConnections objects, then to invoke useConnection only
     * once after it has the "final" URLConnection object.  For this reason
     * be sure to implement prepareConnection so that it can be invoked
     * multiple times.
     * 
     */
    interface HttpCommand {

        /**
         * Configures the HttpURLConnection (headers, chuncking, etc.) according
         * to the needs of this use of the connection and then writes any
         * required outbound payload to the connection.
         * <p>
         * This method might be invoked multiple times before the connection is
         * actually connected, so it should be serially reentrant.  Note that the
         * caller will
         * @param urlConnection the connection to be configured
         */
        public void prepareConnection(HttpURLConnection urlConnection) throws IOException;

        /**
         * Uses the configured and connected connection to read
         * data, process it, etc.
         * 
         * @param urlConnection the connection to be used
         * @throws CommandException
         * @throws IOException
         */
        public void useConnection(HttpURLConnection urlConnection)
                throws CommandException, IOException;
    }

    public RemoteAdminCommand(String name, String host, int port)
            throws CommandException {

        this(name, host, port, false, "admin", null, Logger.getAnonymousLogger());
    }

    public RemoteAdminCommand(String name, String host, int port,
            boolean secure, String user, String password, Logger logger)
            throws CommandException {
        this(name, host, port, secure, user, password, logger, null, null, false);
    }

    /**
     * Construct a new remote command object.  The command and arguments
     * are supplied later using the execute method in the superclass.
     */
    public RemoteAdminCommand(String name, String host, int port,
            boolean secure, String user, String password, Logger logger,
            final String scope,
            final String authToken,
            final boolean prohibitDirectoryUploads)
            throws CommandException {
        this.name = name;
        this.host = host;
        this.port = port;
        this.secure = secure;
        this.user = user;
        this.password = password;
        this.logger = logger;
        this.scope = scope;
        this.authToken = authToken;
        this.prohibitDirectoryUploads = prohibitDirectoryUploads;
        checkName();
    }

    /**
     * Make sure the command name is legitimate and
     * won't allow any URL spoofing attacks.
     */
    private void checkName() throws CommandException {
        if (!name.matches(COMMAND_NAME_REGEXP)) {
            throw new CommandException("Illegal command name: " + name);
            //todo: XXX - I18N
        }
    }

    /**
     * Set the response type used in requests to the server.
     * The response type is sent in the User-Agent HTTP header
     * and tells the server what format of response to produce.
     */
    public void setResponseFormatType(String responseFormatType) {
        this.responseFormatType = responseFormatType;
    }

    /**
     * If set, the raw response from the command is written to the
     * specified stream.
     */
    public void setUserOut(OutputStream userOut) {
        this.userOut = userOut;
    }

    /**
     * Set the CommandModel used by this command.  Normally the
     * CommandModel will be fetched from the server using the
     * getCommandModel method, which will also save the CommandModel
     * for further use.  If the CommandModel is known in advance, it
     * can be set with this method and avoid the call to the server.
     */
    public void setCommandModel(CommandModel commandModel) {
        this.commandModel = commandModel;
        this.commandModelFromCache = false;
    }

    /**
     * Set the read timeout for the URLConnection.
     */
    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }
    
    public static int getReadTimeout() {
        return defaultReadTimeout;
    }

    /**
     * Set the connect timeout for the URLConnection.
     */
    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }
    
    /**
     * Set the interactive mode for the command.  By default, the command is
     * interactive.
     */
    public void setInteractive(boolean state) {
        this.interactive = state;
    }
    
    /**
     * Omit local {@code AdminCache} to process command metadata. 
     * If {@code true} it will download the metadata from remote server.<br/>
     * <i>Default value is</i> {@code false}
     */
    public void setOmitCache(boolean omitCache) {
        this.omitCache = omitCache;
    }

    /**
     * Get the CommandModel for the command from the server.
     * If the CommandModel hasn't been set, it's fetched from
     * the server.
     *
     * @return the model for the command
     * @throws CommandException if the server can't be contacted
     */
    public CommandModel getCommandModel() throws CommandException {
        if (commandModel == null && !omitCache) {
            long startNanos = System.nanoTime();
            try {
                commandModel = AdminCacheUtils.getCache().get(createCommandCacheKey(), CommandModel.class);
                if (commandModel != null) {
                    this.commandModelFromCache = true;
                    if (commandModel instanceof CachedCommandModel) {
                        CachedCommandModel ccm = (CachedCommandModel) commandModel;
                        this.usage = ccm.getUsage();
                        addedUploadOption = ccm.isAddedUploadOption();
                    }
                    if (logger.isLoggable(Level.FINEST)) {
                        logger.log(Level.FINEST, "Command model for command {0} was successfully loaded from the cache. [Duration: {1} nanos]", new Object[] {name, System.nanoTime() - startNanos});
                    }
                } else {
                    if (logger.isLoggable(Level.FINEST)) {
                        logger.log(Level.FINEST, "Command model for command {0} is not in cache. It must be fatched from server.", name);
                    }
                }
            } catch (Exception ex) {
                if (logger.isLoggable(Level.FINEST)) {
                    logger.log(Level.FINEST, "Can not get data from cache under key " + createCommandCacheKey(), ex);
                }
            }
        }
        if (commandModel == null) {
            fetchCommandModel();
        }
        return commandModel;
    }
    
    /** If command model was load from local cache.
     */
    public boolean isCommandModelFromCache() {
        return commandModelFromCache;
    }

    /**
     * Set the directory in which any returned files will be stored.
     * The default is the user's home directory.
     */
    public void setFileOutputDirectory(File dir) {
        fileOutputDir = dir;
    }

    /**
     * Return a modifiable list of headers to be added to the request.
     */
    public List<Header> headers() {
        return requestHeaders;
    }

    /**
     * Run the command using the specified arguments.
     * Return the output of the command.
     */
    public String executeCommand(ParameterMap opts) throws CommandException {
        // first, make sure we have the command model
        getCommandModel();

        // XXX : This is to take care of camel case from ReST calls that
	// do not go through usual CLI path
        // XXX : This is not clean; this should be handled the same way
	// it is handled for incoming CLI commands
        options = new ParameterMap();
        for (Map.Entry<String, List<String>> o : opts.entrySet()) {
            String key = o.getKey();
            List<String> value = o.getValue();
            options.set(key.toLowerCase(Locale.ENGLISH), value);
        }
        operands = options.get("default");	// "DEFAULT".toLowerCase()

        try {
            initializeDoUpload();

            // if uploading, we need a payload
            if (doUpload) {
                outboundPayload = PayloadImpl.Outbound.newInstance();
            }

            StringBuilder uriString = getCommandURI();
            ParamModel operandParam = null;
            for (ParamModel opt : commandModel.getParameters()) {
                if (opt.getParam().primary()) {
                    operandParam = opt;
                    continue;
                }
                String paramName = opt.getName();
                
                List<String> paramValues = new ArrayList<String>(options.get(paramName.toLowerCase(Locale.ENGLISH)));
                if (!opt.getParam().alias().isEmpty() && 
                        !paramName.equalsIgnoreCase(opt.getParam().alias())){
                    paramValues.addAll(options.get(opt.getParam().alias().toLowerCase(Locale.ENGLISH)));
                }
                if (!opt.getParam().multiple() && paramValues.size() > 1) {
                    throw new CommandException(strings.get("tooManyOptions", 
                            paramName));
                }
                if (paramValues.isEmpty()) {
                    // perhaps it's set in the environment?
                    String envValue = getFromEnvironment(paramName);
                    if (envValue != null) {
                        paramValues.add(envValue); 
                    }
                }
                if (paramValues.isEmpty()) {
                    /*
                     * Option still not set.  Note that we ignore the default
                     * value and don't send it explicitly on the assumption
                     * that the server will supply the default value itself.
                     *
                     * If the missing option is required, that's an error,
                     * which should never happen here because validate()
                     * should check it first.
                     */
                    if (!opt.getParam().optional()) {
                        throw new CommandException(strings.get("missingOption",
                                paramName));
                    }
                    // optional param not set, skip it
                    continue;
                }
                for (String paramValue : paramValues) {
                    if (opt.getType() == File.class ||
                            opt.getType() == File[].class) {
                        addFileOption(uriString, paramName, paramValue);
                    } else if (opt.getParam().password()) {
                        addPasswordOption(uriString, paramName, paramValue);
                    } else {
                        addStringOption(uriString, paramName, paramValue);
                    }
                }
            }

            // add operands
            for (String operand : operands) {
                if (operandParam.getType() == File.class ||
                        operandParam.getType() == File[].class) {
                    addFileOption(uriString, "DEFAULT", operand);
                } else {
                    addStringOption(uriString, "DEFAULT", operand);
                }
            }

            // remove the last character, whether it was "?" or "&"
            uriString.setLength(uriString.length() - 1);
            executeRemoteCommand(uriString.toString());
        } catch (IOException ioex) {
            // possibly an error caused while reading or writing a file?
            throw new CommandException("I/O Error", ioex);
        }
        return output;
    }

    /**
     * After a successful command execution, the attributes returned
     * by the command are saved.  This method returns those saved
     * attributes.
     */
    public Map<String, String> getAttributes() {
        return attrs;
    }

    /**
     * Return true if we're successful in collecting new information
     * (and thus the caller should try the request again).
     * Subclasses can override to (e.g.) collect updated authentication
     * information by prompting the user.
     * The implementation in this class returns false, indicating that the
     * authentication information was not updated.
     */
    protected boolean updateAuthentication() {
        return false;
    }

    /**
     * Subclasses can override to supply parameter values from environment.
     * The implementation in this class returns null, indicating that the
     * name is not available in the environment.
     */
    protected String getFromEnvironment(String name) {
        return null;
    }

    /**
     * Called when a non-secure connection attempt fails and it appears
     * that the server requires a secure connection.
     * Subclasses can override to indicate that the connection should
     * The implementation in this class returns false, indicating that the
     * connection should not be retried.
     */
    protected boolean retryUsingSecureConnection(String host, int port) {
        return false;
    }

    /**
     * Return the error message to be used in the AuthenticationException.
     * Subclasses can override to provide a more detailed message, for
     * example, indicating the source of the password that failed.
     * The implementation in this class returns a default error message.
     */
    protected String reportAuthenticationException() {
        return strings.get("InvalidCredentials", user);
    }
    
    /**
     * Get the URI for executing the command.
     */
    protected StringBuilder getCommandURI() {
        StringBuilder rv = new StringBuilder(ADMIN_URI_PATH);
        if (scope != null) rv.append(scope);
        rv.append(name).append(QUERY_STRING_INTRODUCER);
        return rv;
    }

    /**
     * Actually execute the remote command.
     */
    private void executeRemoteCommand(String uri) throws CommandException {
        doHttpCommand(uri, chooseRequestMethod(), new HttpCommand() {
            
            @Override
            public void prepareConnection(final HttpURLConnection urlConnection) throws IOException {
                if (doUpload) {
                    /*
                     * If we are uploading anything then set the content-type
                     * and add the uploaded part(s) to the payload.
                     */
                    urlConnection.setChunkedStreamingMode(0); // use default value
                    urlConnection.setRequestProperty("Content-Type",
                            outboundPayload.getContentType());
                }

                // add any user-specified headers
                for (Header h : requestHeaders) {
                    urlConnection.addRequestProperty(h.getName(), h.getValue());
                }

                if (doUpload) {
                    outboundPayload.writeTo(urlConnection.getOutputStream());
                }

            }
            
            @Override
            public void useConnection(final HttpURLConnection urlConnection)
                    throws CommandException, IOException {
                InputStream in = urlConnection.getInputStream();

                String responseContentType = urlConnection.getContentType();

                Payload.Inbound inboundPayload =
                    PayloadImpl.Inbound.newInstance(responseContentType, in);

                if (inboundPayload == null)
                    throw new IOException(
                        strings.get("NoPayloadSupport", responseContentType));
                PayloadFilesManager downloadedFilesMgr =
                    new PayloadFilesManager.Perm(fileOutputDir, null, logger,
                        new PayloadFilesManager.ActionReportHandler() {
                            @Override
                            public void handleReport(InputStream reportStream)
                                                    throws Exception {
                                handleResponse(options, reportStream,
                                    urlConnection.getResponseCode(), userOut);
                            }
                        });
                try {
                    downloadedFilesMgr.processParts(inboundPayload);
                } catch (CommandException cex) {
                    throw cex;
                } catch (Exception ex) {
                    throw new CommandException(ex.getMessage(), ex);
                }
                }
            });
    }

    private void doHttpCommand(String uriString, String httpMethod,
            HttpCommand cmd) throws CommandException {
        doHttpCommand(uriString, httpMethod, cmd, false /* isForMetadata */);
    }

    /**
     * Set up an HTTP connection, call cmd.prepareConnection so the consumer of
     * the connection can further configure it, then open the connection (following
     * redirects if needed), then call cmd.useConnection so the consumer of the
     * connection can use it.
     * <P>
     * This method will try to execute the command repeatedly, for example,
     * retrying with updated credentials (typically from the interactive user), etc., until the
     * command succeeds or there are no more ways to retry that might succeed.
     *
     * @param uriString     the URI to connect to
     * @param httpMethod    the HTTP method to use for the connection
     * @param cmd           the HttpCommand object
     * @throws CommandException if anything goes wrong
     */
    private void doHttpCommand(String uriString, String httpMethod,
            HttpCommand cmd, boolean isForMetadata) throws CommandException {
        HttpURLConnection urlConnection;
        /*
         * There are various reasons we might retry the command - an authentication
         * challenges from the DAS, shifting from an insecure connection to
         * a secure one, etc.  So just keep trying as long as it makes sense.
         *
         * Any exception handling code inside the loop that changes something
         * about the connection or the request and wants to retry must set
         * shoudTryCommandAgain to true.
         */
        boolean shouldTryCommandAgain;
        
        /*
         * If the DAS challenges us for credentials and we've already sent
         * the caller-provided ones, we might ask the user for a new set
         * and use them.  But we want to ask only once.
         */
        boolean askedUserForCredentials = false;
        
        /*
         * On a subsequent retry we might need to use secure, even if the
         * caller did not request it.
         */
        boolean shouldUseSecure = secure;

        /*
         * Send the caller-provided credentials (typically from command line
         * options or the password file) on the first attempt only if we know
         * the connection will
         * be secure.
         */
        boolean usedCallerProvidedCredentials = secure;
        
        /*
         * Note: HttpConnectorAddress will set up SSL/TLS client cert
         * handling if the current configuration calls for it.
         */
        HttpConnectorAddress url = getHttpConnectorAddress(
                                host, port, shouldUseSecure);
        url.setInteractive(interactive);

        do {
            /*
             * Any code that wants to trigger a retry will say so explicitly.
             */
            shouldTryCommandAgain = false;
            try {
                if (logger.isLoggable(Level.FINER)) {
                    logger.log(Level.FINER, "URI: {0}", uriString);
                    logger.log(Level.FINER, "URL: {0}", url.toString());
                    logger.log(Level.FINER, "URL: {0}", url.toURL(uriString).toString());
                    logger.log(Level.FINER, "Password options: {0}", passwordOptions);
                    logger.log(Level.FINER, "Using auth info: User: {0}, Password: {1}", 
                            new Object[]{user, ok(password) ? "<non-null>" : "<null>"});
                }
                final AuthenticationInfo authInfo = authenticationInfo();
                if (authInfo != null) {
                    url.setAuthenticationInfo(authInfo);
                }
                urlConnection = (HttpURLConnection) url.openConnection(uriString);
                urlConnection.setRequestProperty("User-Agent", responseFormatType);
                if (passwordOptions != null) {
                    urlConnection.setRequestProperty("X-passwords", passwordOptions.toString());
                }

                if (authToken != null) {
                    /*
                     * If this request is for metadata then we expect to reuse
                     * the auth token.   
                     */
                    urlConnection.setRequestProperty(
                            SecureAdmin.Util.ADMIN_ONE_TIME_AUTH_TOKEN_HEADER_NAME,
                            (isForMetadata ? AuthTokenManager.markTokenForReuse(authToken) : authToken));
                }
                if (commandModel != null && isCommandModelFromCache() && commandModel instanceof CachedCommandModel) {
                    urlConnection.setRequestProperty(COMMAND_MODEL_MATCH_HEADER, ((CachedCommandModel) commandModel).getETag());
                    if (logger.isLoggable(Level.FINER)) {
                        logger.log(Level.FINER, "CommandModel ETag: {0}", ((CachedCommandModel) commandModel).getETag());
                    }
                }
                urlConnection.setRequestMethod(httpMethod);
                urlConnection.setReadTimeout(readTimeout);
                if (connectTimeout >= 0)
                    urlConnection.setConnectTimeout(connectTimeout);
                addAdditionalHeaders(urlConnection);
                
                cmd.prepareConnection(urlConnection);
                urlConnection.connect();
                /*
                 * We must handle redirection from http to https explicitly
                 * because, even if the HttpURLConnection's followRedirect is
                 * set to true, the Java SE implementation does not do so if the
                 * procotols are different.
                 */
                String redirection = checkConnect(urlConnection);
                if (redirection != null) {
                    /*
                     * Log at FINER; at FINE it would appear routinely when used from
                     * asadmin.
                     */
                    logger.log(Level.FINER, "Following redirection to " + redirection);
                    url = followRedirection(url, redirection);
                    shouldTryCommandAgain = true;
                    /*
                     * Record that, during the retry of this request, we should
                     * use https.
                     */
                    shouldUseSecure = url.isSecure();

                    /*
                     * Record that, if this is a metadata request, the real
                     * request should use https also.
                     */
                    secure = true;
                    
                    urlConnection.disconnect();

                    continue;
                }

                /*
                 * No redirection, so we have established the connection.
                 * Now delegate again to the command processing to use the
                 * now-created connection.
                 */
                cmd.useConnection(urlConnection);
                processHeaders(urlConnection);
                logger.finer("doHttpCommand succeeds");
            } catch (AuthenticationException authEx) {
                
                logger.log(Level.FINER, "DAS has challenged for credentials");

                /*
                 * The DAS has challenged us to provide valid credentials.
                 *
                 * We might have sent the request without credentials previously
                 * (because the connection was not secure, typically). In that case,
                 * retry using the caller provided credentials (if there are any).
                 */
                if ( ! usedCallerProvidedCredentials) {
                    logger.log(Level.FINER, "Have not tried caller-supplied credentials yet; will do that next");
                    usedCallerProvidedCredentials = true;
                    shouldTryCommandAgain = true;
                    continue;
                }
                /*
                 * We already tried the caller-provided credentials.  Try to
                 * update the credentials if we haven't already done so.
                 */
                logger.log(Level.FINER, "Already used caller-supplied credentials");
                if (askedUserForCredentials) {
                    /*
                     * We already updated the credentials once, and the updated
                     * ones did not work.  No recourse.
                     */
                    logger.log(Level.FINER, "Already tried with updated credentials; cannot authenticate");
                    throw authEx;
                }

                /*
                 * Try to update the creds.
                 */
                logger.log(Level.FINER, "Have not yet tried to update credentials, so will try to update them");
                if ( ! updateAuthentication()) {
                    /*
                     * No updated credentials are avaiable, so we
                     * have no more options.
                     */
                    logger.log(Level.FINER, "Could not update credentials; cannot authenticate");
                    throw authEx;
                }
                /*
                 * We have another set of credentials we can try.
                 */
                logger.log(Level.FINER, "Was able to update the credentials so will retry with the updated ones");
                askedUserForCredentials = true;
                shouldTryCommandAgain = true;
                continue;

            } catch (ConnectException ce) {
                logger.finer("doHttpCommand: connect exception " + ce);
                // this really means nobody was listening on the remote server
                // note: ConnectException extends IOException and tells us more!
                String msg = strings.get("ConnectException", host, port + "");
                throw new CommandException(msg, ce);
            } catch (UnknownHostException he) {
                logger.finer("doHttpCommand: host exception " + he);
                // bad host name
                String msg = strings.get("UnknownHostException", host);
                throw new CommandException(msg, he);
            } catch (SocketException se) {
                logger.finer("doHttpCommand: socket exception " + se);
                try {
                    boolean serverAppearsSecure = NetUtils.isSecurePort(host, port);
                    if (serverAppearsSecure && !shouldUseSecure) {
                        if (retryUsingSecureConnection(host, port)) {
                            // retry using secure connection
                            shouldUseSecure = true;
                            usedCallerProvidedCredentials = true;
                            shouldTryCommandAgain = true;
                            continue;
                        }
                    }
                    throw new CommandException(se);
                } catch(IOException io) {
                    // XXX - logger.printExceptionStackTrace(io);
                    throw new CommandException(io);
                }
            } catch (SSLException se) {
                logger.finer("doHttpCommand: SSL exception " + se);
                try {
                    boolean serverAppearsSecure = NetUtils.isSecurePort(host, port);
                    if (!serverAppearsSecure && secure) {
                        logger.log(Level.SEVERE, AdminLoggerInfo.mServerIsNotSecure, 
                                new Object[] { host, port });
                    }
                    throw new CommandException(se);
                } catch(IOException io) {
                    // XXX - logger.printExceptionStackTrace(io);
                    throw new CommandException(io);
                }
            } catch (SocketTimeoutException e) {
                logger.finer("doHttpCommand: read timeout " + e);
                throw new CommandException(
                    strings.get("ReadTimeout", (float)readTimeout / 1000), e);
            } catch (IOException e) {
                logger.finer("doHttpCommand: IO exception " + e);
                throw new CommandException(
                    strings.get("IOError", e.getMessage()), e);
            } catch (CommandException e) {
                throw e;
            } catch (Exception e) {
                // logger.log(Level.FINER, "doHttpCommand: exception", e);
                logger.finer("doHttpCommand: exception " + e);
                ByteArrayOutputStream buf = new ByteArrayOutputStream();
                e.printStackTrace(new PrintStream(buf));
                logger.finer(buf.toString());
                throw new CommandException(e);
            }
        } while (shouldTryCommandAgain);
        outboundPayload = null; // no longer needed
    }

    /**
     * Creates a new HttpConnectorAddress corresponding to the location to which
     * an earlier request was redirected.
     * <p>
     * If the new protocol is https then the HttpConnectorAddress secure setting
     * is turned on.
     * @param originalAddr the address which has been redirected elsewhere
     * @param redirection the location to which the attempted connection was redirected
     * @return connector address for the new location
     * @throws MalformedURLException
     */
    private HttpConnectorAddress followRedirection(
            final HttpConnectorAddress originalAddr,
            final String redirection) throws MalformedURLException {
        final URL url = new URL(redirection);
        final boolean useSecure = (url.getProtocol().equalsIgnoreCase("https"));
        HttpConnectorAddress hca = new HttpConnectorAddress(
                url.getHost(),
                url.getPort(),
                useSecure,
                originalAddr.getPath(),
                originalAddr.getSSLSocketFactory());
        hca.setInteractive(interactive);
        return hca;
    }

    /**
     * Provides an HttpConnectorAddress for use in connecting to the desired
     * admin listener.
     * <p>
     * This implementation works for true admin clients and will not work
     * correctly for commands submitted to instances from inside the DAS.  (That
     * is done from the implementation in ServerRemoteAdminCommand which extends
     * this class.)
     * <p>
     * This code constructs the HttpConnectorAddress in a way that uses either
     * no SSLSocketFactory (if security is off) or uses an SSLSocketFactory
     * linked to the asadmin truststore.
     *
     * @param host the host name to which the connection should be made
     * @param port the admin port on that host
     * @param shouldUseSecure whether SSL should be used to connect or not
     * @return
     */
    protected HttpConnectorAddress getHttpConnectorAddress(
            final String host, final int port, final boolean shouldUseSecure) {
        HttpConnectorAddress hca = new HttpConnectorAddress(
                                host, port, shouldUseSecure);
        hca.setInteractive(interactive);
        return hca;
    }

    /**
     * Adds any headers needed for the current environment to the admin
     * request.
     *
     * @param urlConnection
     */
    protected void addAdditionalHeaders(final URLConnection urlConnection) {
        /*
         * No additional headers are needed for connections originating from
         * true admin clients.
         */
    }

    /**
     * Process any headers needed from the reply to the admin
     * request.   Subclasses can override this method to handle processing
     * headers in the command's reply.
     *
     * @param urlConnection
     */
    protected void processHeaders(final URLConnection urlConnection) {
        /*
         * No headers are processed by RemoteAdminCommand.
         */
    }
    
    
    /*
     * Returns the username/password authenticaiton information to use
     * in building the outbound HTTP connection.
     * 
     * @return the username/password auth. information to send with the request
     */
    protected AuthenticationInfo authenticationInfo() {
        return ((user != null || password != null) ? new AuthenticationInfo(user, password) : null);
    }
    

    /**
     * Check that the connection was successful and handle any error responses,
     * turning them into exceptions.
     */
    private String checkConnect(HttpURLConnection urlConnection)
                                throws IOException, CommandException {
        int code = urlConnection.getResponseCode();
        if (logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER, "Response code: " + code);
        }
        if (code == -1) {
            URL url = urlConnection.getURL();
            throw new CommandException(
                strings.get("NotHttpResponse", url.getHost(), url.getPort()));
        }
        if (code == HttpURLConnection.HTTP_UNAUTHORIZED) {
            throw new AuthenticationException(reportAuthenticationException());
        }
        if (code == HttpURLConnection.HTTP_PRECON_FAILED) {
            throw new CommandValidationException("Code: " + HttpURLConnection.HTTP_PRECON_FAILED + ": Cached CommandModel is invalid.");
        }
        /*
         * The DAS might be redirecting to a secure port.  If so, follow
         * the redirection.
         */
        if (isStatusRedirection(code)) {
            return urlConnection.getHeaderField("Location");
        }
        if (code != HttpURLConnection.HTTP_OK) {
            throw new CommandException(strings.get("BadResponse", "" + code,
                                        urlConnection.getResponseMessage()));
        }
        /*
         * If the connection worked then return null, indicating no
         * redirection is needed.
         */
        return null;
    }
    
    private boolean isStatusRedirection(final int returnCode) {
        /*
         * Currently, Grizzly redirects using 302.  For admin requests the
         * other varieties of redirection do not apply.
         */
        return (returnCode == HttpURLConnection.HTTP_MOVED_TEMP);
    }

    /**
     * Get the usage text.
     * If we got usage information from the server, use it.
     *
     * @return usage text
     */
    public String getUsage() {
        return usage;
    }

    /**
     * Adds a single option expression to the URI.  Appends a '?' in preparation
     * for the next option.
     *
     * @param uriString the URI composed so far
     * @param option the option expression to be added
     * @return the URI so far, including the newly-added option
     */
    private StringBuilder addStringOption(StringBuilder uriString, String name,
            String option) {
        try {
            String encodedOption = URLEncoder.encode(option, "UTF-8");
            uriString.append(name).
                append('=').
                append(encodedOption).
                append(QUERY_STRING_SEPARATOR);
        } catch (UnsupportedEncodingException e) {
            // XXX - should never happen
            throw new RuntimeException("Error encoding value for: " + name 
                    + ", value:" + option, e);
        }
        return uriString;
    }

    /**
     * Add a password option, passing it as a header in the request
     */
    private StringBuilder addPasswordOption(StringBuilder uriString, String name,
            String option) throws IOException {
        if (passwordOptions == null) {
            passwordOptions = new StringBuilder();
        } else {
            passwordOptions.append(QUERY_STRING_SEPARATOR);
        }
        GFBase64Encoder encoder = new GFBase64Encoder();
        passwordOptions.append(name).append('=').append(
                URLEncoder.encode(encoder.encode(option.getBytes()), "UTF-8"));
        return uriString;
    }
    
    /**
     * Adds an option for a file argument, passing the name (for uploads) or the
     * path (for no-upload) operations.
     *
     * @param uriString the URI string so far
     * @param optionName the option which takes a path or name
     * @param filename the name of the file
     * @return the URI string
     * @throws java.io.IOException
     */
    private StringBuilder addFileOption(
            StringBuilder uriString,
            String optionName,
            String filename) throws IOException, CommandException {
        File f = SmartFile.sanitize(new File(filename));
        logger.finer("FILE PARAM: " + optionName + " = " + f);
        final boolean uploadThisFile = doUpload && ! f.isDirectory();
        // attach the file to the payload - include the option name in the
        // relative URI to avoid possible conflicts with same-named files
        // in different directories
        if (uploadThisFile) {
            logger.finer("Uploading file");
            try {
            outboundPayload.attachFile(FILE_PAYLOAD_MIME_TYPE,
                URI.create(optionName + "/" + f.getName() + (f.isDirectory() ? "/" : "")),
                optionName,
                null,
                f,
                true /* isRecursive - in case it's a directory */);
            } catch (FileNotFoundException fnfe) {
                /*
                 * Probably due to an attempt to upload a non-existent file.
                 * Convert this to a CommandException so it's better handled
                 * by the rest of the command running infrastructure.
                 */
                throw new CommandException(strings.get("UploadedFileNotFound", f.getAbsolutePath()));
            }
        }
        if (f != null) {
            // if we are about to upload it -- give just the name
            // o/w give the full path
            String pathToPass = (uploadThisFile ? f.getName() : f.getPath());
            addStringOption(uriString, optionName, pathToPass);
        }
        return uriString;
    }

    /**
     * Decide what request method to use in building the HTTP request.
     * @return the request method appropriate to the current command and options
     */
    private String chooseRequestMethod() {
        // XXX - should be part of command metadata
        if (doUpload) {
            return "POST";
        } else {
            return "GET";
        }
    }

    private void handleResponse(ParameterMap params,
            InputStream in, int code, OutputStream userOut)
            throws IOException, CommandException {
        if (userOut == null) {
            handleResponse(params, in, code);
        } else {
            FileUtils.copy(in, userOut, 0);
        }
    }

    private void handleResponse(ParameterMap params,
            InputStream in, int code) throws IOException, CommandException {
        RemoteResponseManager rrm = null;

        try {
            rrm = new RemoteResponseManager(in, code, logger);
            rrm.process();
        } catch (RemoteSuccessException rse) {
            // save results
            output = rse.getMessage();
	    assert rrm != null;
	    attrs = rrm.getMainAtts();
            return;
        } catch (RemoteException rfe) {
            // XXX - gross
            if (rfe.getRemoteCause().indexOf("CommandNotFoundException") >= 0) {
                // CommandNotFoundException from the server, then display
                // the closest matching commands
                throw new InvalidCommandException(rfe.getMessage());
            }
            throw new CommandException(
                        "remote failure: " + rfe.getMessage(), rfe);
        }
    }

    /**
     * Fetch the command metadata from the remote server.
     */
    protected void fetchCommandModel() throws CommandException {
        long startNanos = System.nanoTime();
        commandModel = null; //For sure not be used during request header construction
        
        // XXX - there should be a "help" command, that returns XML output
        //StringBuilder uriString = new StringBuilder(ADMIN_URI_PATH).
                //append("help").append(QUERY_STRING_INTRODUCER);
        //addStringOption(uriString, "DEFAULT", name);
        StringBuilder uriString = getCommandURI();
        addStringOption(uriString, "Xhelp", "true");

        // remove the last character, whether it was "?" or "&"
        uriString.setLength(uriString.length() - 1);

        doHttpCommand(uriString.toString(), "GET", new HttpCommand() {

            @Override
            public void prepareConnection(HttpURLConnection urlConnection) {
                //urlConnection.setRequestProperty("Accept: ", "text/xml");
                urlConnection.setRequestProperty("User-Agent", "metadata");
            }

            @Override
            public void useConnection(HttpURLConnection urlConnection)
                    throws CommandException, IOException {

                InputStream in = urlConnection.getInputStream();

                String responseContentType = urlConnection.getContentType();
                logger.finer("Response Content-Type: " + responseContentType);
                Payload.Inbound inboundPayload =
                    PayloadImpl.Inbound.newInstance(responseContentType, in);

                if (inboundPayload == null)
                    throw new IOException(
                        strings.get("NoPayloadSupport", responseContentType));

                boolean isReportProcessed = false;
                Iterator<Payload.Part> partIt = inboundPayload.parts();
                while (partIt.hasNext()) {
                    /*
                     * There should be only one part, which should be the
                     * metadata, but skip any other parts just in case.
                     */
                    if (!isReportProcessed) {
                        metadataErrors = new StringBuilder();
                        commandModel =
                                parseMetadata(partIt.next().getInputStream(),
                                metadataErrors);
                        logger.finer(
                            "fetchCommandModel: got command opts: " +
                            commandModel);
                        isReportProcessed = true;
                    } else {
                        partIt.next();  // just throw it away
                    }
                }
            }
        });
        if (commandModel == null) {
            if (metadataErrors != null) {
                throw new InvalidCommandException(metadataErrors.toString());
            } else {
                throw new InvalidCommandException(strings.get("unknownError"));
            }
        } else {
            this.commandModelFromCache = false;
            if (logger.isLoggable(Level.FINEST)) {
                logger.log(Level.FINEST, "Command model for {0} command fetched from remote server. [Duration: {1} nanos]", new Object[] {name, System.nanoTime() - startNanos});
            }
            //if (!omitCache) {
                try {
                    AdminCacheUtils.getCache().put(createCommandCacheKey(), commandModel);
                } catch (Exception ex) {
                    if (logger.isLoggable(Level.WARNING)) {
                        logger.log(Level.WARNING, AdminLoggerInfo.mCantPutToCache, 
                                new Object[] { createCommandCacheKey() });
                    }
                }
            //}
        }
    }
    
    private String createCommandCacheKey() {
        StringBuilder result = new StringBuilder(getCanonicalHost().length() + name.length() + 6);
        result.append(getCanonicalHost()).append('_').append(port);
        result.append('/').append(name);
        return result.toString();
    } 
    
    protected String getCanonicalHost() {
        if (canonicalHostCache == null) {
            try {
                InetAddress address = InetAddress.getByName(host);
                canonicalHostCache = address.getCanonicalHostName();
            } catch (UnknownHostException ex) {
                canonicalHostCache = host;
                if (canonicalHostCache != null) {
                    canonicalHostCache = canonicalHostCache.trim().toLowerCase(Locale.ENGLISH);
                }
            }
        }
        return canonicalHostCache;
    }

    /**
     * Parse the XML metadata for the command on the input stream.
     *
     * @param in the input stream
     * @return the set of ValidOptions
     */
    private CommandModel parseMetadata(InputStream in, StringBuilder errors) {
        if (logger.isLoggable(Level.FINER)) { // XXX - assume "debug" == "FINER"
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                FileUtils.copy(in, baos, 0);
            } catch (IOException ex) { }
            in = new ByteArrayInputStream(baos.toByteArray());
            String response = baos.toString();
            logger.finer("------- RAW METADATA RESPONSE ---------");
            logger.finer(response);
            logger.finer("------- RAW METADATA RESPONSE ---------");
        }

        CachedCommandModel cm = new CachedCommandModel(name);
        boolean sawFile = false;
        try {
            DocumentBuilder d =
                    DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = d.parse(in); 
            NodeList cmd = doc.getElementsByTagName("command");
            Node cmdnode = cmd.item(0);
            if (cmdnode == null) {
                Node report = doc.getElementsByTagName("action-report").item(0);
                String cause = getAttr(report.getAttributes(), "failure-cause");
                if (ok(cause))
                    errors.append(cause);
                else {
                    Node mp = report.getFirstChild();   // message-part
                    if (mp != null)
                        cause = getAttr(mp.getAttributes(), "message");
                    if (ok(cause))
                        errors.append(cause);
                }
                // no command info, must be invalid command or something
                // wrong with command implementation
                return null;
            }
            NamedNodeMap cmdattrs = cmdnode.getAttributes();
            usage = getAttr(cmdattrs, "usage");
            cm.setUsage(usage);
            String dashOk = getAttr(cmdattrs, "unknown-options-are-operands");
            if (dashOk != null)
                cm.dashOk = Boolean.parseBoolean(dashOk);
            NodeList opts = doc.getElementsByTagName("option");
            for (int i = 0; i < opts.getLength(); i++) {
                Node n = opts.item(i);
                NamedNodeMap attributes = n.getAttributes();
                String sn = getAttr(attributes, "short");
                String def = getAttr(attributes, "default");
                String obs = getAttr(attributes, "obsolete");
                String alias = getAttr(attributes, "alias");
                ParamModelData opt = new ParamModelData(
                        getAttr(attributes, "name"),
                        typeOf(getAttr(attributes, "type")),
                        Boolean.parseBoolean(getAttr(attributes, "optional")),
                        def,
                        ok(sn) ? sn : null,
			ok(obs) ? Boolean.parseBoolean(obs) : false,
			alias);
                if (getAttr(attributes, "type").equals("PASSWORD")) {
                    opt.param._password = true;
                    opt.prompt = getAttr(attributes, "prompt");
                    opt.promptAgain = getAttr(attributes, "promptAgain");
                }
                cm.add(opt);
                if (opt.getType() == File.class)
                    sawFile = true;
            }
            // should be only one operand item
            opts = doc.getElementsByTagName("operand");
            for (int i = 0; i < opts.getLength(); i++) {
                Node n = opts.item(i);
                NamedNodeMap attributes = n.getAttributes();
                Class<?> type = typeOf(getAttr(attributes, "type"));
                if (type == File.class) {
                    sawFile = true;
                }
                int min = Integer.parseInt(getAttr(attributes, "min"));
                String max = getAttr(attributes, "max");
                boolean multiple = false;
                if (max.equals("unlimited")) {
                    multiple = true;
                    // XXX - should convert to array of whatever
                    if (type == File.class) {
                        type = File[].class;
                    } else {
                        type = List.class;
                    }
                }
                ParamModelData pm = new ParamModelData(
                    getAttr(attributes, "name"), type, min == 0, null);
                pm.param._primary = true;
                pm.param._multiple = multiple;
                cm.add(pm);
            }

            /*
             * If one of the options or operands is a FILE,
             * make sure there's also a --upload option available.
             * XXX - should only add it if it's not present
             * XXX - should just define upload parameter on remote command
             */
            if (sawFile) {
                cm.add(new ParamModelData("upload", Boolean.class,
                        true, null));
                addedUploadOption = true;
                cm.setAddedUploadOption(true);
            }
        } catch (ParserConfigurationException pex) {
            // ignore for now
            return null;
        } catch (SAXException sex) {
            // ignore for now
            return null;
        } catch (IOException ioex) {
            // ignore for now
            return null;
        }
        return cm;
    }

    private Class<?> typeOf(String type) {
        if (type.equals("STRING"))
            return String.class;
        else if (type.equals("BOOLEAN"))
            return Boolean.class;
        else if (type.equals("FILE"))
            return File.class;
        else if (type.equals("PASSWORD"))
            return String.class;
        else if (type.equals("PROPERTIES"))
            return Properties.class;
        else
            return String.class;
    }

    /**
     * Return the value of a named attribute, or null if not set.
     */
    private static String getAttr(NamedNodeMap attributes, String name) {
        Node n = attributes.getNamedItem(name);
        if (n != null)
            return n.getNodeValue();
        else
            return null;
    }

    /**
     * Search all the parameters that were actually specified to see
     * if any of them are FILE type parameters.  If so, check for the
     * "--upload" option.
     */
    private void initializeDoUpload() throws CommandException {
        boolean sawFile = false;
        boolean sawDirectory = false;
        /*
         * We don't upload directories, even when asked to upload.
         */
        boolean sawUploadableFile = false;

        for (Map.Entry<String, List<String>> param : options.entrySet()) {
            String paramName = param.getKey();
            if (paramName.equals("DEFAULT"))    // operands handled below
                continue;
            ParamModel opt = commandModel.getModelFor(paramName);
            if (opt != null && 
                    (opt.getType() == File.class ||
                     opt.getType() == File[].class)) {
                sawFile = true;
                for (String fname : options.get(opt.getName())) {
                    final File optionFile = new File(fname);
                    sawDirectory |= optionFile.isDirectory();
                    sawUploadableFile |= optionFile.isFile();
                }              
            }
        }

        // now check the operands for files
        ParamModel operandParam = getOperandModel();
        if (operandParam != null &&
                (operandParam.getType() == File.class ||
                 operandParam.getType() == File[].class)) {
            sawFile |= !operands.isEmpty();
            for (String operandValue : operands) {
                final File operandFile = new File(operandValue);
                sawDirectory |= operandFile.isDirectory();
                sawUploadableFile |= operandFile.isFile();
            }
        }

        if (sawFile) {
            logger.finer("Saw a file parameter");
            // found a FILE param, is doUpload set?
            String upString = getOption("upload");
            if (ok(upString))
                doUpload = Boolean.parseBoolean(upString);
            else
                doUpload = !isLocal(host) && sawUploadableFile;
            if (prohibitDirectoryUploads && sawDirectory && doUpload) {
                // oops, can't upload directories
                logger.finer("--upload=" + upString +
                                            ", doUpload=" + doUpload);
                throw new CommandException(strings.get("CantUploadDirectory"));
            }
        }

        if (addedUploadOption) {
            logger.finer("removing --upload option");
            //options.remove("upload");    // remove it
            // XXX - no remove method, have to copy it
            ParameterMap noptions = new ParameterMap();
            for (Map.Entry<String, List<String>> e : options.entrySet()) {
                if (!e.getKey().equals("upload"))
                    noptions.set(e.getKey(), e.getValue());
            }
            options = noptions;
        }

        logger.finer("doUpload set to " + doUpload);
    }

    /**
     * Does the given hostname represent the local host?
     */
    private static boolean isLocal(String hostname) {
        if (hostname.equalsIgnoreCase("localhost"))     // the common case
            return true;
        try {
            // let NetUtils do the hard work
            InetAddress ia = InetAddress.getByName(hostname);
            return NetUtils.isLocal(ia.getHostAddress());
        } catch (UnknownHostException ex) {
            /*
             * Sometimes people misconfigure their name service and they
             * can't even look up the name of their own machine.
             * Too bad.  We just give up and say it's not local.
             */
            return false;
        }
    }

    /**
     * Get the ParamModel that corresponds to the operand
     * (primary parameter).  Return null if none.
     */
    private ParamModel getOperandModel() {
        for (ParamModel pm : commandModel.getParameters()) {
            if (pm.getParam().primary())
                return pm;
        }
        return null;
    }

    /**
     * Get an option value, that might come from the command line
     * or from the environment.  Return the default value for the
     * option if not otherwise specified.
     */
    private String getOption(String name) {
        String val = options.getOne(name);
        if (val == null)
            val = getFromEnvironment(name);
        if (val == null) {
            // no value, find the default
            ParamModel opt = commandModel.getModelFor(name);
            // if no value was specified and there's a default value, return it
            if (opt != null) {
                String def = opt.getParam().defaultValue();
                if (ok(def))
                    val = def;
            }
        }
        return val;
    }

    private static boolean ok(String s) {
        return s != null && s.length() > 0;
    }
}
