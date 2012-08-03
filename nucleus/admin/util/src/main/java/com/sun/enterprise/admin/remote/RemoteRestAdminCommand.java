/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.enterprise.admin.remote.RestPayloadImpl.Inbound;
import com.sun.enterprise.admin.remote.reader.ActionReportJsonReader;
import com.sun.enterprise.admin.remote.reader.CliActionReport;
import com.sun.enterprise.admin.remote.writer.ParameterMapFormWriter;
import com.sun.enterprise.admin.remote.writer.PayloadPartProvider;
import com.sun.enterprise.admin.util.AsadminTrustManager;
import com.sun.enterprise.admin.util.AuthenticationInfo;
import com.sun.enterprise.admin.util.CachedCommandModel;
import com.sun.enterprise.admin.util.CommandModelData.ParamModelData;
import com.sun.enterprise.admin.util.HttpConnectorAddress.BasicHostnameVerifier;
import com.sun.enterprise.admin.util.cache.AdminCacheUtils;
import com.sun.enterprise.config.serverbeans.SecureAdmin;
import com.sun.enterprise.universal.i18n.LocalStringsImpl;
import com.sun.enterprise.universal.io.SmartFile;
import com.sun.enterprise.util.StringUtils;
import com.sun.enterprise.util.net.NetUtils;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManager;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.glassfish.admin.payload.PayloadFilesManager;
import org.glassfish.api.ActionReport;
import org.glassfish.api.ActionReport.MessagePart;
import org.glassfish.api.admin.CommandModel.ParamModel;
import org.glassfish.api.admin.*;
import org.glassfish.api.admin.Payload.Part;
import org.glassfish.common.util.admin.AuthTokenManager;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.JerseyClientFactory;
import org.glassfish.jersey.client.filter.CsrfProtectionFilter;
import org.glassfish.jersey.client.filter.HttpBasicAuthFilter;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.glassfish.jersey.media.multipart.MultiPartClientBinder;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

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
 */
public class RemoteRestAdminCommand {

    private static final LocalStringsImpl strings =
            new LocalStringsImpl(RemoteAdminCommand.class);

    private static final String EOL = StringUtils.EOL;

    private static final String QUERY_STRING_INTRODUCER = "?";
    private static final String QUERY_STRING_SEPARATOR = "&";
    private static final String ADMIN_URI_PATH = "/__asadmin/";
    private static final String COMMAND_NAME_REGEXP =
                                    "^[a-zA-Z_][-a-zA-Z0-9_]*$";
    private static final String READ_TIMEOUT = "AS_ADMIN_READTIMEOUT";
    public static final String COMMAND_MODEL_MATCH_HEADER = "X-If-Command-Model-Match";
    private static final MediaType MEDIATYPE_ACTIONREPORT = new MediaType("actionreport", "json",
            new HashMap<String, String>(1) {{ put("q", "0.8"); }});
    private static final MediaType MEDIATYPE_MULTIPART = new MediaType("multipart", null,
            new HashMap<String, String>(1) {{ put("q", "0.9"); }});
    private static final int defaultReadTimeout; // read timeout for URL conns

    //JAX-RS Client related attributes
    private static final Client client;
    static {
        client = JerseyClientFactory.newClient(new ClientConfig().binders(new MultiPartClientBinder()));
        // client = ClientFactory.newClient(); - Move to this standard initialisation when people from Jersey will produce Multipart as a Feature not just module.
        client.configuration()
                .register(CsrfProtectionFilter.class)
                .register(ActionReportJsonReader.class)
                .register(ParameterMapFormWriter.class)
                .register(PayloadPartProvider.class);
//                .register(MultiPartReaderClientSide.class)
//                .register(MultiPartWriter.class);
    }
//    private Target target;

    private String              responseFormatType = "hk2-agent";
    private OutputStream        userOut;
    // return output string rather than printing it
    protected String              output;
    private ActionReport        actionReport;
//    private Map<String, String> attrs;
    private boolean             doUpload = false;
    private boolean             addedUploadOption = false;
    private RestPayloadImpl.Outbound    outboundPayload;
    private String              usage;
    private File                fileOutputDir;
    //private StringBuilder       passwordOptions;

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

//    /**
//     * Interface to enable factoring out common HTTP connection management code.
//     * <p>
//     * The implementation of this interface must implement
//     * <ul>
//     * <li>{@link #prepareConnection} - to perform all pre-connection configuration - set headers, chunking, etc.
//     * as well as writing any payload to the outbound connection.  In short
//     * anything needed prior to the URLConnection#connect invocation.
//     * <p>
//     * The caller will invoke this method after it has invoked {@link URL#openConnection}
//     * but before it invokes {@link URL#connect}.
//     * <li>{@link #useConnection} - to read from the
//     * input stream, etc.  The caller will invoke this method after it has
//     * successfully invoked {@link URL#connect}.
//     * </ul>
//     * Because the caller might have to work with multiple URLConnection objects
//     * (as it follows redirection, for example) this contract allows the caller
//     * to delegate to the HttpCommand implementation multiple times to configure
//     * each of the URLConnections objects, then to invoke useConnection only
//     * once after it has the "final" URLConnection object.  For this reason
//     * be sure to implement prepareConnection so that it can be invoked
//     * multiple times.
//     *
//     */
//    interface HttpCommand {
//
//        /**
//         * Configures the HttpURLConnection (headers, chuncking, etc.) according
//         * to the needs of this use of the connection and then writes any
//         * required outbound payload to the connection.
//         * <p>
//         * This method might be invoked multiple times before the connection is
//         * actually connected, so it should be serially reentrant.  Note that the
//         * caller will
//         * @param urlConnection the connection to be configured
//         */
//        public void prepareConnection(HttpURLConnection urlConnection) throws IOException;
//
//        /**
//         * Uses the configured and connected connection to read
//         * data, process it, etc.
//         *
//         * @param urlConnection the connection to be used
//         * @throws CommandException
//         * @throws IOException
//         */
//        public void useConnection(HttpURLConnection urlConnection)
//                throws CommandException, IOException;
//    }

    public RemoteRestAdminCommand(String name, String host, int port)
            throws CommandException {

        this(name, host, port, false, "admin", null, Logger.getAnonymousLogger());
    }

    public RemoteRestAdminCommand(String name, String host, int port,
            boolean secure, String user, String password, Logger logger)
            throws CommandException {
        this(name, host, port, secure, user, password, logger, null, null, false);
    }

    /**
     * Construct a new remote command object.  The command and arguments
     * are supplied later using the execute method in the superclass.
     */
    public RemoteRestAdminCommand(String name, String host, int port,
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
        if (scope != null && scope.endsWith("/")) {
            this.scope = scope.substring(0, scope.length() - 1);
        } else {
            this.scope = scope;
        }
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

//    private Target getTarget() {
//        if (this.target == null) {
//            StringBuilder path = new StringBuilder();
//            if (secure) {
//                path.append("https://");
//            } else {
//                path.append("http://");
//            }
//            path.append(host);
//            if (port > 0) {
//                path.append(':').append(port);
//            }
//            path.append("/command/").append(name);
//            target = client.target(path.toString());
//        }
//        return target;
//    }

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

    public ActionReport getActionReport() {
        return actionReport;
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
        if (commandModel == null) {
            long startNanos = System.nanoTime();
            try {
                commandModel = getCommandModelFromCahce();
                if (commandModel != null) {
                    this.commandModelFromCache = true;
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

    private CommandModel getCommandModelFromCahce() {
        String cachedModel = AdminCacheUtils.getCache().get(createCommandCacheKey(), String.class);
        if (cachedModel == null) {
            return null;
        }
        cachedModel = cachedModel.trim();
        int ind = cachedModel.indexOf('\n');
        if (ind < 0) {
            return null;
        }
        String eTag = cachedModel.substring(0, ind);
        if (!eTag.startsWith("ETag:")) {
            return null;
        }
        eTag = eTag.substring(5).trim();
        if (logger.isLoggable(Level.FINEST)) {
            logger.log(Level.FINEST, "Cached command model ETag is {0}", eTag);
        }
        String content = cachedModel.substring(ind + 1).trim();
        return parseMetadata(content, eTag);
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

    public String executeCommand(ParameterMap opts) throws CommandException {
        //Just to be sure. Cover get help
        if (opts != null && opts.size() == 1 && opts.containsKey("help")) {
            return getManPage();
        }
        //execute
        ParameterMap preparedParams = processParams(opts);
        Response response = doRestCommand(preparedParams, null, "POST", false,
                MEDIATYPE_MULTIPART, MEDIATYPE_ACTIONREPORT);
        MediaType resultMediaType = response.getMediaType();
        if (logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER, "Result type is {0}", resultMediaType);
        }
        if (MEDIATYPE_ACTIONREPORT.isCompatible(resultMediaType)) {
            setActionReport(response.readEntity(ActionReport.class));
//            if (logger.isLoggable(Level.FINER)) {
//                String data = response.readEntity(String.class);
//                logger.log(Level.FINER, "-------- RAW DATA --------");
//                logger.log(Level.FINER, data);
//                logger.log(Level.FINER, "------ END RAW DATA ------");
//            }
        } else if (MEDIATYPE_MULTIPART.isCompatible(resultMediaType)) {
            MultiPart mp = response.readEntity(MultiPart.class);
            Inbound inbound = new RestPayloadImpl.Inbound();
            setActionReport(RestPayloadImpl.Inbound.fillFromMultipart(mp, inbound, logger));
            if (logger.isLoggable(Level.FINER)) {
                logger.log(Level.FINER, "------ PAYLOAD ------");
                Iterator<Part> parts = inbound.parts();
                while (parts.hasNext()) {
                    Part part = parts.next();
                    logger.log(Level.FINER, " - {0} [{1}]", new Object[]{part.getName(), part.getContentType()});
                }
                logger.log(Level.FINER, "---- END PAYLOAD ----");
            }
            PayloadFilesManager downloadedFilesMgr =
                    new PayloadFilesManager.Perm(fileOutputDir, null, logger, null);
            try {
                downloadedFilesMgr.processParts(inbound);
            } catch (CommandException cex) {
                throw cex;
            } catch (Exception ex) {
                throw new CommandException(ex.getMessage(), ex);
            }
        } else {
            throw new CommandException(strings.get("unknownResponse", resultMediaType));
        }
        if (logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER, "------ ACTION REPORT ------");
            logger.log(Level.FINER, String.valueOf(actionReport));
            logger.log(Level.FINER, "---- END ACTION REPORT ----");
        }
        if (actionReport == null) {
            this.output = null;
            throw new CommandException(strings.get("emptyResponse"));
        }
        return output;
    }

    protected void setActionReport(ActionReport ar) {
        this.actionReport = ar;
        if (ar == null) {
            this.output = null;
        } else {
            StringBuilder sb = new StringBuilder();
            if (ar instanceof CliActionReport) {
                addCombinedMessages((CliActionReport) ar, sb);
            } else if (ar.getMessage() != null) {
                sb.append(ar.getMessage());
            }
            addSubMessages("", ar.getTopMessagePart(), sb);
            this.output = sb.toString();
        }
    }

    private static void addSubMessages(String indentPrefix, ActionReport.MessagePart mp, StringBuilder sb) {
        if (mp == null || sb == null) {
            return;
        }
        if (indentPrefix == null) {
            indentPrefix = "";
        }
        List<MessagePart> children = mp.getChildren();
        if (children != null) {
            for (MessagePart subPart : children) {
                if (sb.length() > 0) {
                    sb.append(EOL);
                }
//                if (ok(subPart.getChildrenType())) {
//                    sb.append(indentPrefix).append(subPart.getChildrenType()).append(": "); //I dont understand why indent only if exist childrentype, but it is as is now implemented
//                }
                if (ok(subPart.getMessage())) {
                    sb.append(subPart.getMessage());
                }
                addSubMessages(indentPrefix + "    ", subPart, sb);
            }
        }
    }

    private static void addCombinedMessages(CliActionReport aReport, StringBuilder sb) {
        if (aReport == null || sb == null) {
            return;
        }
        String mainMsg = ""; //this is the message related to the topMessage
        String failMsg; //this is the message related to failure cause
        // Other code in the server may write something like report.setMessage(exception.getMessage())
        // and also set report.setFailureCause(exception). We need to avoid the duplicate message.
        if (aReport.getMessage() != null && aReport.getMessage().length() != 0) {
            if (sb.length() > 0) sb.append(EOL);
            sb.append(aReport.getMessage());
        }
        if (aReport.getFailureCause() != null && aReport.getFailureCause().getMessage() != null && aReport.getFailureCause().getMessage().length() != 0) {
            failMsg = aReport.getFailureCause().getMessage();
            if (!failMsg.equals(mainMsg))
                if (sb.length() > 0) sb.append(EOL);
                sb.append(failMsg);
        }
        for (CliActionReport sub : aReport.getSubActionsReport()) {
            addCombinedMessages(sub, sb);
        }
    }

    /**
     * Run the command using the specified arguments.
     * Return the output of the command.
     */
    public ParameterMap processParams(ParameterMap opts) throws CommandException {

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
                outboundPayload = new RestPayloadImpl.Outbound(true);
            }

            ParameterMap result = new ParameterMap();
            ParamModel operandParam = null;
            for (ParamModel opt : commandModel.getParameters()) {
                if (opt.getParam().primary()) {
                    operandParam = opt;
                    continue;
                }
                String paramName = opt.getName();
                // XXX - no multi-value support
                String paramValue =
		    options.getOne(paramName.toLowerCase(Locale.ENGLISH));
                if (paramValue == null) {
                    // is it an alias ?
                    if (opt.isParamId(paramName)) {
                        paramValue = options.getOne(
			    opt.getParam().alias().toLowerCase(Locale.ENGLISH));
                    }
                }
                if (paramValue == null) // perhaps it's set in the environment?
                    paramValue = getFromEnvironment(paramName);
                if (paramValue == null) {
                    /*
                     * Option still not set.  Note that we ignore the default
                     * value and don't send it explicitly on the assumption
                     * that the server will supply the default value itself.
                     *
                     * If the missing option is required, that's an error,
                     * which should never happen here because validate()
                     * should check it first.
                     */
                    if (!opt.getParam().optional())
                        throw new CommandException(strings.get("missingOption",
                                paramName));
                    // optional param not set, skip it
                    continue;
                }
                if (opt.getType() == File.class) {
                    addFileOption(result, paramName, paramValue);
                } else if (opt.getParam().password()) {
                    addPasswordOption(result, paramName, paramValue);
                } else {
                    addStringOption(result, paramName, paramValue);
                }
            }

            // add operands
            for (String operand : operands) {
                if (operandParam.getType() == File.class ||
                        operandParam.getType() == File[].class) {
                    addFileOption(result, "DEFAULT", operand);
                } else {
                    addStringOption(result, "DEFAULT", operand);
                }
            }
            return result;
        } catch (IOException ioex) {
            // possibly an error caused while reading or writing a file?
            throw new CommandException("I/O Error", ioex);
        }
    }

//    /**
//     * After a successful command execution, the attributes returned
//     * by the command are saved.  This method returns those saved
//     * attributes.
//     */
//    public Map<String, String> getAttributes() {
//        return attrs;
//    }

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

//    /**
//     * Get the URI for executing the command.
//     */
//    protected StringBuilder getCommandURI() {
//        StringBuilder rv = new StringBuilder(ADMIN_URI_PATH);
//        if (scope != null) rv.append(scope);
//        rv.append(name).append(QUERY_STRING_INTRODUCER);
//        return rv;
//    }

//    /**
//     * Actually execute the remote command.
//     */
//    private void executeRemoteCommand(String uri) throws CommandException {
//        doHttpCommand(uri, chooseRequestMethod(), new HttpCommand() {
//
//            @Override
//            public void prepareConnection(final HttpURLConnection urlConnection) throws IOException {
//                if (doUpload) {
//                    /*
//                     * If we are uploading anything then set the content-type
//                     * and add the uploaded part(s) to the payload.
//                     */
//                    urlConnection.setChunkedStreamingMode(0); // use default value
//                    urlConnection.setRequestProperty("Content-Type",
//                            outboundPayload.getContentType());
//                }
//
//                // add any user-specified headers
//                for (Header h : requestHeaders) {
//                    urlConnection.addRequestProperty(h.getName(), h.getValue());
//                }
//
//                if (doUpload) {
//                    outboundPayload.writeTo(urlConnection.getOutputStream());
//                }
//
//            }
//
//            @Override
//            public void useConnection(final HttpURLConnection urlConnection)
//                    throws CommandException, IOException {
//
//                InputStream in = urlConnection.getInputStream();
//
//                String responseContentType = urlConnection.getContentType();
//
//                Payload.Inbound inboundPayload =
//                    PayloadImpl.Inbound.newInstance(responseContentType, in);
//
//                if (inboundPayload == null)
//                    throw new IOException(
//                        strings.get("NoPayloadSupport", responseContentType));
//                PayloadFilesManager downloadedFilesMgr =
//                    new PayloadFilesManager.Perm(fileOutputDir, null, logger,
//                        new PayloadFilesManager.ActionReportHandler() {
//                            @Override
//                            public void handleReport(InputStream reportStream)
//                                                    throws Exception {
//                                handleResponse(options, reportStream,
//                                    urlConnection.getResponseCode(), userOut);
//                            }
//                        });
//                try {
//                    downloadedFilesMgr.processParts(inboundPayload);
//                } catch (CommandException cex) {
//                    throw cex;
//                } catch (Exception ex) {
//                    throw new CommandException(ex.getMessage(), ex);
//                }
//                }
//            });
//    }

    private URI createURI(boolean secure, String pathSufix) throws CommandException {
        StringBuilder path = new StringBuilder("/command/");
        if (ok(scope)) {
            path.append(scope).append('/');
        }
        path.append(name);
        if (ok(pathSufix)) {
            if (!pathSufix.startsWith("/")) {
                path.append('/');
            }
            path.append(pathSufix);
        }
        try {
            return new URI(secure ? "https" : "http", null, host, port, path.toString(),  null, null);
        } catch (URISyntaxException e) {
            throw new CommandException(
                    strings.get("internal", e.getMessage()), e);
        }
    }

    /**
     * This method will try to execute the command repeatedly, for example,
     * retrying with updated credentials (typically from the interactive user), etc., until the
     * command succeeds or there are no more ways to retry that might succeed.
     */
    private Response doRestCommand(ParameterMap options, String pathSufix,
            String method, boolean isForMetadata,
            MediaType... acceptedResponseTypes) throws CommandException {
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
         * Do not send caller-provided credentials the first time unless
         * we know we will send the first request securely.
         */
        boolean shouldSendCredentials = secure;

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

        //Create JAX-RS target
        URI uri = createURI(shouldUseSecure, pathSufix);

        do {
            WebTarget target = client.target(uri);
            target.configuration()
                    .setProperty(ClientProperties.HOSTNAME_VERIFIER, new BasicHostnameVerifier(host))
                    .setProperty(ClientProperties.SSL_CONTEXT, getSslContext());
            /*
             * Any code that wants to trigger a retry will say so explicitly.
             */
            shouldTryCommandAgain = false;
            try {
                if (logger.isLoggable(Level.FINER)) {
                    logger.log(Level.FINER, "URI: {0}", uri.toString());
                    logger.log(Level.FINER, "Using auth info: User: {0}, Password: {1}",
                            new Object[]{user, ok(password) ? "<non-null>" : "<null>"});
                }
                final AuthenticationInfo authInfo = authenticationInfo();
                if (authInfo != null && shouldSendCredentials) {
                    target.configuration().register(
                            new HttpBasicAuthFilter(authInfo.getUser(), authInfo.getPassword()));
                }
                Builder request = target.request(acceptedResponseTypes);
                if (authToken != null) {
                    /*
                     * If this request is for metadata then we expect to reuse
                     * the auth token.
                     */
                    request = request.header(
                            SecureAdmin.Util.ADMIN_ONE_TIME_AUTH_TOKEN_HEADER_NAME,
                            (isForMetadata ? AuthTokenManager.markTokenForReuse(authToken) : authToken));
                }
                if (commandModel != null && isCommandModelFromCache() && commandModel instanceof CachedCommandModel) {
                    request =  request.header(COMMAND_MODEL_MATCH_HEADER,
                            ((CachedCommandModel) commandModel).getETag());
                    if (logger.isLoggable(Level.FINER)) {
                        logger.log(Level.FINER, "CommandModel ETag: {0}", ((CachedCommandModel) commandModel).getETag());
                    }
                }
                //Headers
                for (Header h : requestHeaders) {
                    request = request.header(h.getName(), h.getValue());
                }
                request = addAdditionalHeaders(request);
                if (logger.isLoggable(Level.FINER)) {
                    request = request.header("X-Indent", "true");
                }
                //Make invocation
                Invocation invoc = null;
                if ("POST".equals(method)) {
                    if (outboundPayload != null && outboundPayload.size() > 0) {
                        FormDataMultiPart mp = new FormDataMultiPart();
                        //Copy params there
                        for (Map.Entry<String, List<String>> entry : options.entrySet()) {
                            String key = entry.getKey();
                            for (String val : entry.getValue()) {
                                mp.field(key, val);
                            }
                        }
                        //Copy outbound there
                        outboundPayload.addToMultipart(mp, logger);
                        Entity<FormDataMultiPart> entity = Entity.<FormDataMultiPart>entity(mp, mp.getMediaType());
                        invoc = request.build(method, entity);
                    } else {
                        Entity<ParameterMap> entity = Entity.<ParameterMap>entity(options, MediaType.APPLICATION_FORM_URLENCODED_TYPE);
                        invoc = request.build(method, entity);
                    }
                } else {
                    invoc = request.build(method);
                }
                //todo: set timeout
//                urlConnection.setReadTimeout(readTimeout);
//                if (connectTimeout >= 0)
//                    urlConnection.setConnectTimeout(connectTimeout);

                //Invoke
                Response response = invoc.invoke();
                /*
                 * We must handle redirection from http to https explicitly
                 * because, even if the HttpURLConnection's followRedirect is
                 * set to true, the Java SE implementation does not do so if the
                 * procotols are different.
                 */
                String redirection = checkConnect(response, uri.getHost(), uri.getPort());
                if (ok(redirection)) {
                    /*
                     * Log at FINER; at FINE it would appear routinely when used from
                     * asadmin.
                     */
                    if (logger.isLoggable(Level.FINER)) {
                        logger.log(Level.FINER, "Following redirection to {0}", redirection);
                    }
                    uri = new URI(redirection);
                    shouldTryCommandAgain = true;

                    /*
                     * Record that, during the retry of this request, we should
                     * use https.
                     */
                    shouldUseSecure = "https".equals(uri.getScheme());

                    /*
                     * Record that, if this is a metadata request, the real
                     * request should use https also.
                     */
                    secure = true;

                    /*
                     * If we have been redirected to https then we can send
                     * the credentials - if we have them - on the next
                     * request we send because the request and therefore the
                     * credentials will be encrypted.
                     */
                    shouldSendCredentials = shouldUseSecure;

                    continue;
                }

                processHeaders(response);
                logger.finer("doHttpCommand succeeds");
                return response;
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
                    shouldSendCredentials = true;
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
                shouldSendCredentials = true;
                shouldTryCommandAgain = true;
                continue;

            } catch (ConnectException ce) {
                if (logger.isLoggable(Level.FINER)) {
                    logger.log(Level.FINER, "doHttpCommand: connect exception {0}", ce);
                }
                // this really means nobody was listening on the remote server
                // note: ConnectException extends IOException and tells us more!
                String msg = strings.get("ConnectException", host, port + "");
                throw new CommandException(msg, ce);
            } catch (UnknownHostException he) {
                if (logger.isLoggable(Level.FINER)) {
                    logger.log(Level.FINER, "doHttpCommand: host exception {0}", he);
                }
                // bad host name
                String msg = strings.get("UnknownHostException", host);
                throw new CommandException(msg, he);
            } catch (SocketException se) {
                if (logger.isLoggable(Level.FINER)) {
                    logger.log(Level.FINER, "doHttpCommand: socket exception {0}", se);
                }
                try {
                    boolean serverAppearsSecure = NetUtils.isSecurePort(host, port);
                    if (serverAppearsSecure && !shouldUseSecure) {
                        if (retryUsingSecureConnection(host, port)) {
                            // retry using secure connection
                            shouldUseSecure = true;
                            shouldSendCredentials = true;
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
                if (logger.isLoggable(Level.FINER)) {
                    logger.log(Level.FINER, "doHttpCommand: SSL exception {0}", se);
                }
                try {
                    boolean serverAppearsSecure = NetUtils.isSecurePort(host, port);
                    if (!serverAppearsSecure && secure) {
                        logger.severe(strings.get("ServerIsNotSecure",
                                                    host, port + ""));
                    }
                    throw new CommandException(se);
                } catch(IOException io) {
                    // XXX - logger.printExceptionStackTrace(io);
                    throw new CommandException(io);
                }
            } catch (SocketTimeoutException e) {
                if (logger.isLoggable(Level.FINER)) {
                    logger.log(Level.FINER, "doHttpCommand: read timeout {0}", e);
                }
                throw new CommandException(
                    strings.get("ReadTimeout", (float)readTimeout / 1000), e);
            } catch (IOException e) {
                if (logger.isLoggable(Level.FINER)) {
                    logger.log(Level.FINER, "doHttpCommand: IO exception {0}", e);
                }
                throw new CommandException(
                    strings.get("IOError", e.getMessage()), e);
            } catch (CommandException e) {
                throw e;
            } catch (Exception e) {
                // logger.log(Level.FINER, "doHttpCommand: exception", e);
                if (logger.isLoggable(Level.FINER)) {
                    logger.log(Level.FINER, "doHttpCommand: exception {0}", e);
                }
                ByteArrayOutputStream buf = new ByteArrayOutputStream();
                e.printStackTrace(new PrintStream(buf));
                logger.finer(buf.toString());
                throw new CommandException(e);
            }
        } while (shouldTryCommandAgain);
        outboundPayload = null; // no longer needed
        return null;
    }

//    /**
//     * Creates a new HttpConnectorAddress corresponding to the location to which
//     * an earlier request was redirected.
//     * <p>
//     * If the new protocol is https then the HttpConnectorAddress secure setting
//     * is turned on.
//     * @param originalAddr the address which has been redirected elsewhere
//     * @param redirection the location to which the attempted connection was redirected
//     * @return connector address for the new location
//     * @throws MalformedURLException
//     */
//    private Target followRedirection(
//            final Target target,
//            final String redirection) throws MalformedURLException {
//        target.
//        final URL url = new URL(redirection);
//        final boolean useSecure = (url.getProtocol().equalsIgnoreCase("https"));
//        HttpConnectorAddress hca = new HttpConnectorAddress(
//                url.getHost(),
//                url.getPort(),
//                useSecure,
//                originalAddr.getPath(),
//                originalAddr.getSSLSocketFactory());
//        hca.setInteractive(interactive);
//        return hca;
//    }

//    /**
//     * Provides an HttpConnectorAddress for use in connecting to the desired
//     * admin listener.
//     * <p>
//     * This implementation works for true admin clients and will not work
//     * correctly for commands submitted to instances from inside the DAS.  (That
//     * is done from the implementation in ServerRemoteAdminCommand which extends
//     * this class.)
//     * <p>
//     * This code constructs the HttpConnectorAddress in a way that uses either
//     * no SSLSocketFactory (if security is off) or uses an SSLSocketFactory
//     * linked to the asadmin truststore.
//     *
//     * @param host the host name to which the connection should be made
//     * @param port the admin port on that host
//     * @param shouldUseSecure whether SSL should be used to connect or not
//     * @return
//     */
//    protected HttpConnectorAddress getHttpConnectorAddress(
//            final String host, final int port, final boolean shouldUseSecure) {
//        HttpConnectorAddress hca = new HttpConnectorAddress(
//                                host, port, shouldUseSecure);
//        hca.setInteractive(interactive);
//        return hca;
//    }

    protected SSLContext getSslContext() {
        try {
            String protocol = "TLSv1";
            SSLContext cntxt = SSLContext.getInstance(protocol);
            /*
             * Pass null for the array of KeyManagers.  That uses the default
             * ones, so if the user has loaded client keys into the standard
             * Java SE keystore they will be found.
             */
            AsadminTrustManager atm = new AsadminTrustManager();
            atm.setInteractive(interactive);
            cntxt.init(null, new TrustManager[] {atm}, null);
            return cntxt;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Adds any headers needed for the current environment to the admin
     * request.
     *
     * @param request
     */
    protected Builder addAdditionalHeaders(final Builder request) {
        /*
         * No additional headers are needed for connections originating from
         * true admin clients.
         */
        return request;
    }

    /**
     * Process any headers needed from the reply to the admin
     * request.   Subclasses can override this method to handle processing
     * headers in the command's reply.
     */
    protected void processHeaders(final Response headers) {
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
    private String checkConnect(Response response, String host, int port)
                                throws IOException, CommandException {
        int code = response.getStatus();
        if (logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER, "Response code: " + code);
        }
        if (code == -1) {
            throw new CommandException(
                strings.get("NotHttpResponse", host, port));
        }
        if (code == HttpURLConnection.HTTP_UNAUTHORIZED) {
            throw new AuthenticationException(reportAuthenticationException());
        }
        if (code == HttpURLConnection.HTTP_PRECON_FAILED) {
            throw new CommandValidationException("Code: " + HttpURLConnection.HTTP_PRECON_FAILED + ": Cached CommandModel is invalid.");
        }
        if (code == HttpURLConnection.HTTP_NOT_FOUND) {
            throw new InvalidCommandException(response.readEntity(String.class));
        }
        /*
         * The DAS might be redirecting to a secure port.  If so, follow
         * the redirection.
         */
        if (isStatusRedirection(code)) {
            return response.getHeader("Location");
        }
        if (code != HttpURLConnection.HTTP_OK) {
            throw new CommandException(strings.get("BadResponse", String.valueOf(code),
                                        response.readEntity(String.class)));
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
     * @param params Add option here
     * @param option the option expression to be added
     * @return the URI so far, including the newly-added option
     */
    private void addStringOption(ParameterMap params, String name, String option) {
        params.add(name, option);
    }

    /**
     * Add a password option. Same as String option. No more password specialities
     */
    private void addPasswordOption(ParameterMap params, String name,
            String option) throws IOException {
        addStringOption(params, name, option);
    }

    /**
     * Adds an option for a file argument, passing the name (for uploads) or the
     * path (for no-upload) operations.
     *
     * @param params params to modify
     * @param optionName the option which takes a path or name
     * @param filename the name of the file
     * @throws java.io.IOException
     */
    private void addFileOption(
            ParameterMap params,
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
            addStringOption(params, optionName, pathToPass);
        }
    }

//    /**
//     * Decide what request method to use in building the HTTP request.
//     * @return the request method appropriate to the current command and options
//     */
//    private String chooseRequestMethod() {
//        // XXX - should be part of command metadata
//        if (doUpload) {
//            return "POST";
//        } else {
//            return "GET";
//        }
//    }

//    //TODO: Remove
//    private void handleResponse(ParameterMap params,
//            InputStream in, int code, OutputStream userOut)
//            throws IOException, CommandException {
//        if (userOut == null) {
//            handleResponse(params, in, code);
//        } else {
//            FileUtils.copy(in, userOut, 0);
//        }
//    }
//
//    //TODO: Remove
//    private void handleResponse(ParameterMap params,
//            InputStream in, int code) throws IOException, CommandException {
//        RemoteResponseManager rrm = null;
//
//        try {
//            rrm = new RemoteResponseManager(in, code, logger);
//            rrm.process();
//        } catch (RemoteSuccessException rse) {
//            // save results
//            output = rse.getMessage();
//	    assert rrm != null;
//	    attrs = rrm.getMainAtts();
//            return;
//        } catch (RemoteException rfe) {
//            // XXX - gross
//            if (rfe.getRemoteCause().indexOf("CommandNotFoundException") >= 0) {
//                // CommandNotFoundException from the server, then display
//                // the closest matching commands
//                throw new InvalidCommandException(rfe.getMessage());
//            }
//            throw new CommandException(
//                        "remote failure: " + rfe.getMessage(), rfe);
//        }
//    }

    public String getManPage() throws CommandException {
        logger.log(Level.FINEST, "getManPage()");
        Response res = doRestCommand(new ParameterMap(), "manpage", "GET", false, MediaType.TEXT_PLAIN_TYPE);
        if (res.getStatus() == HttpURLConnection.HTTP_OK) {
            return res.readEntity(String.class);
        } else {
            if (logger.isLoggable(Level.FINER)) {
                logger.log(Level.FINER, "get ManPege result status: {0}", res.getStatus());
            }
            throw new CommandException(res.readEntity(String.class));
        }
    }

    protected void fetchCommandModel() throws CommandException {
        logger.log(Level.FINEST, "fetchCommandModel()");
        long startNanos = System.nanoTime();
        Response res = doRestCommand(new ParameterMap(), null, "GET", true, MediaType.APPLICATION_JSON_TYPE);
        if (res.getStatus() == HttpURLConnection.HTTP_OK) {
            String str = res.readEntity(String.class);
            if (logger.isLoggable(Level.FINEST)) {
                logger.log(Level.FINEST, "Command model for {0} command fetched from remote server first part. [Duration: {1} nanos]", new Object[] {name, System.nanoTime() - startNanos});
            }
            EntityTag eTag = res.getEntityTag();
            commandModel = parseMetadata(str, eTag == null ? null : eTag.getValue());
            if (commandModel != null) {
                this.commandModelFromCache = false;
                if (logger.isLoggable(Level.FINEST)) {
                    logger.log(Level.FINEST, "Command model for {0} command fetched from remote server. [Duration: {1} nanos]", new Object[] {name, System.nanoTime() - startNanos});
                }
                try {
                    StringBuilder forCache = new StringBuilder(str.length() + 40);
                    forCache.append("ETag: ").append(eTag == null ? "" : eTag.getValue());
                    forCache.append("\n");
                    forCache.append(str);
                    AdminCacheUtils.getCache().put(createCommandCacheKey(), forCache.toString());
                } catch (Exception ex) {
                    if (logger.isLoggable(Level.WARNING)) {
                        logger.log(Level.WARNING, strings.get("CantPutToCache", createCommandCacheKey()), ex);
                    }
                }
            } else {
                throw new InvalidCommandException(strings.get("unknownError"));
            }
        } else {
            if (logger.isLoggable(Level.FINER)) {
                logger.log(Level.FINER, "Fatch command model result is {0}", res.getStatus());
            }
            throw new InvalidCommandException(strings.get("unknownError"));
        }
    }

    //TODO: Remove
//    /**
//     * Fetch the command metadata from the remote server.
//     */
//    protected void fetchCommandModel() throws CommandException {
//        long startNanos = System.nanoTime();
//        commandModel = null; //For sure not be used during request header construction
//
//        // XXX - there should be a "help" command, that returns XML output
//        //StringBuilder uriString = new StringBuilder(ADMIN_URI_PATH).
//                //append("help").append(QUERY_STRING_INTRODUCER);
//        //addStringOption(uriString, "DEFAULT", name);
//        StringBuilder uriString = getCommandURI();
//        addStringOption(uriString, "Xhelp", "true");
//
//        // remove the last character, whether it was "?" or "&"
//        uriString.setLength(uriString.length() - 1);
//
//        doHttpCommand(uriString.toString(), "GET", new HttpCommand() {
//
//            @Override
//            public void prepareConnection(HttpURLConnection urlConnection) {
//                //urlConnection.setRequestProperty("Accept: ", "text/xml");
//                urlConnection.setRequestProperty("User-Agent", "metadata");
//            }
//
//            @Override
//            public void useConnection(HttpURLConnection urlConnection)
//                    throws CommandException, IOException {
//
//                InputStream in = urlConnection.getInputStream();
//
//                String responseContentType = urlConnection.getContentType();
//                logger.finer("Response Content-Type: " + responseContentType);
//                Payload.Inbound inboundPayload =
//                    PayloadImpl.Inbound.newInstance(responseContentType, in);
//
//                if (inboundPayload == null)
//                    throw new IOException(
//                        strings.get("NoPayloadSupport", responseContentType));
//
//                boolean isReportProcessed = false;
//                Iterator<Payload.Part> partIt = inboundPayload.parts();
//                while (partIt.hasNext()) {
//                    /*
//                     * There should be only one part, which should be the
//                     * metadata, but skip any other parts just in case.
//                     */
//                    if (!isReportProcessed) {
//                        metadataErrors = new StringBuilder();
//                        commandModel =
//                                parseMetadata(partIt.next().getInputStream(),
//                                metadataErrors);
//                        logger.finer(
//                            "fetchCommandModel: got command opts: " +
//                            commandModel);
//                        isReportProcessed = true;
//                    } else {
//                        partIt.next();  // just throw it away
//                    }
//                }
//            }
//        });
//        if (commandModel == null) {
//            if (metadataErrors != null) {
//                throw new InvalidCommandException(metadataErrors.toString());
//            } else {
//                throw new InvalidCommandException(strings.get("unknownError"));
//            }
//        } else {
//            this.commandModelFromCache = false;
//            if (logger.isLoggable(Level.FINEST)) {
//                logger.log(Level.FINEST, "Command model for {0} command fetched from remote server. [Duration: {1} nanos]", new Object[] {name, System.nanoTime() - startNanos});
//            }
//            //if (!omitCache) {
//                try {
//                    AdminCacheUtils.getCache().put(createCommandCacheKey(), commandModel);
//                } catch (Exception ex) {
//                    if (logger.isLoggable(Level.FINER)) {
//                        logger.log(Level.FINER, "Can not put data to cache under key {0}", createCommandCacheKey());
//                    }
//                }
//            //}
//        }
//    }

    private String createCommandCacheKey() {
        StringBuilder result = new StringBuilder(getCanonicalHost().length() + name.length() + 12);
        result.append("cache/");
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
                    canonicalHostCache = canonicalHostCache.trim().toLowerCase();
                }
            }
        }
        return canonicalHostCache;
    }

    /**
     * Parse the JSon metadata for the command.
     *
     * @param in the input stream
     * @return the set of ValidOptions
     */
    private CachedCommandModel parseMetadata(String str, String etag) {
        if (logger.isLoggable(Level.FINER)) { // XXX - assume "debug" == "FINER"
            logger.finer("------- RAW METADATA RESPONSE ---------");
            logger.log(Level.FINER, "ETag: {0}", etag);
            logger.finer(str);
            logger.finer("------- RAW METADATA RESPONSE ---------");
        }
        if (str == null) {
            return null;
        }
        try {
            boolean sawFile = false;
            JSONObject obj = new JSONObject(str);
            obj = obj.getJSONObject("command");
            CachedCommandModel cm = new CachedCommandModel(obj.getString("@name"), etag);
            cm.dashOk = obj.optBoolean("@unknown-options-are-operands", false);
            cm.supportsProgress = obj.optBoolean("@supports-progress", false);
            cm.setUsage(obj.optString("usage"));
            Object optns = obj.opt("option");
            if (!JSONObject.NULL.equals(optns)) {
                JSONArray jsonOptions;
                if (optns instanceof JSONArray) {
                    jsonOptions = (JSONArray) optns;
                } else {
                    jsonOptions = new JSONArray();
                    jsonOptions.put(optns);
                }
                for (int i = 0; i < jsonOptions.length(); i++) {
                    JSONObject jsOpt = jsonOptions.getJSONObject(i);
                    String type = jsOpt.getString("@type");
                    ParamModelData opt = new ParamModelData(
                            jsOpt.getString("@name"),
                            typeOf(type),
                            jsOpt.optBoolean("@optional", false),
                            jsOpt.optString("@default"),
                            jsOpt.optString("@short"),
                            jsOpt.optBoolean("@obsolete", false),
                            jsOpt.optString("@alias"));
                    opt.param._acceptableValues = jsOpt.optString("@acceptable-values");
                    if ("PASSWORD".equals(type)) {
                        opt.param._password = true;
                        opt.description = jsOpt.optString("$");
                    } else if ("FILE".equals(type)) {
                        sawFile = true;
                    }
                    if (jsOpt.optBoolean("@primary", false)) {
                        opt.param._primary = true;
                    }
                    if (jsOpt.optBoolean("@multiple", false)) {
                        if (opt.type == File.class) {
                            opt.type = File[].class;
                        } else {
                            opt.type = List.class;
                        }
                        opt.param._multiple = true;
                    }
                    cm.add(opt);
                }
            }
            if (sawFile) {
                cm.add(new ParamModelData("upload", Boolean.class,
                        true, null));
                addedUploadOption = true;
                cm.setAddedUploadOption(true);
            }
            this.usage = cm.getUsage();
            return cm;
        } catch (JSONException ex) {
            logger.log(Level.FINER, "Can not parse command metadata", ex);
            return null;
        }
    }

    // TODO: remove parseMetadata - obsolete
//    /**
//     * Parse the XML metadata for the command on the input stream.
//     *
//     * @param in the input stream
//     * @return the set of ValidOptions
//     */
//    private CommandModel parseMetadata(InputStream in, StringBuilder errors) {
//        if (logger.isLoggable(Level.FINER)) { // XXX - assume "debug" == "FINER"
//            ByteArrayOutputStream baos = new ByteArrayOutputStream();
//            try {
//                FileUtils.copy(in, baos, 0);
//            } catch (IOException ex) { }
//            in = new ByteArrayInputStream(baos.toByteArray());
//            String response = baos.toString();
//            logger.finer("------- RAW METADATA RESPONSE ---------");
//            logger.finer(response);
//            logger.finer("------- RAW METADATA RESPONSE ---------");
//        }
//
//        CachedCommandModel cm = new CachedCommandModel(name);
//        boolean sawFile = false;
//        try {
//            DocumentBuilder d =
//                    DocumentBuilderFactory.newInstance().newDocumentBuilder();
//            Document doc = d.parse(in);
//            NodeList cmd = doc.getElementsByTagName("command");
//            Node cmdnode = cmd.item(0);
//            if (cmdnode == null) {
//                Node report = doc.getElementsByTagName("action-report").item(0);
//                String cause = getAttr(report.getAttributes(), "failure-cause");
//                if (ok(cause))
//                    errors.append(cause);
//                else {
//                    Node mp = report.getFirstChild();   // message-part
//                    if (mp != null)
//                        cause = getAttr(mp.getAttributes(), "message");
//                    if (ok(cause))
//                        errors.append(cause);
//                }
//                // no command info, must be invalid command or something
//                // wrong with command implementation
//                return null;
//            }
//            NamedNodeMap cmdattrs = cmdnode.getAttributes();
//            usage = getAttr(cmdattrs, "usage");
//            cm.setUsage(usage);
//            String dashOk = getAttr(cmdattrs, "unknown-options-are-operands");
//            if (dashOk != null)
//                cm.dashOk = Boolean.parseBoolean(dashOk);
//            NodeList opts = doc.getElementsByTagName("option");
//            for (int i = 0; i < opts.getLength(); i++) {
//                Node n = opts.item(i);
//                NamedNodeMap attributes = n.getAttributes();
//                String sn = getAttr(attributes, "short");
//                String def = getAttr(attributes, "default");
//                String obs = getAttr(attributes, "obsolete");
//                String alias = getAttr(attributes, "alias");
//                ParamModelData opt = new ParamModelData(
//                        getAttr(attributes, "name"),
//                        typeOf(getAttr(attributes, "type")),
//                        Boolean.parseBoolean(getAttr(attributes, "optional")),
//                        def,
//                        ok(sn) ? sn : null,
//			ok(obs) ? Boolean.parseBoolean(obs) : false,
//			alias);
//                if (getAttr(attributes, "type").equals("PASSWORD")) {
//                    opt.param._password = true;
//                    opt.description = getAttr(attributes, "description");
//                }
//                cm.add(opt);
//                if (opt.getType() == File.class)
//                    sawFile = true;
//            }
//            // should be only one operand item
//            opts = doc.getElementsByTagName("operand");
//            for (int i = 0; i < opts.getLength(); i++) {
//                Node n = opts.item(i);
//                NamedNodeMap attributes = n.getAttributes();
//                Class<?> type = typeOf(getAttr(attributes, "type"));
//                if (type == File.class) {
//                    sawFile = true;
//                }
//                int min = Integer.parseInt(getAttr(attributes, "min"));
//                String max = getAttr(attributes, "max");
//                boolean multiple = false;
//                if (max.equals("unlimited")) {
//                    multiple = true;
//                    // XXX - should convert to array of whatever
//                    if (type == File.class) {
//                        type = File[].class;
//                    } else {
//                        type = List.class;
//                    }
//                }
//                ParamModelData pm = new ParamModelData(
//                    getAttr(attributes, "name"), type, min == 0, null);
//                pm.param._primary = true;
//                pm.param._multiple = multiple;
//                cm.add(pm);
//            }
//
//            /*
//             * If one of the options or operands is a FILE,
//             * make sure there's also a --upload option available.
//             * XXX - should only add it if it's not present
//             * XXX - should just define upload parameter on remote command
//             */
//            if (sawFile) {
//                cm.add(new ParamModelData("upload", Boolean.class,
//                        true, null));
//                addedUploadOption = true;
//                cm.setAddedUploadOption(true);
//            }
//        } catch (ParserConfigurationException pex) {
//            // ignore for now
//            return null;
//        } catch (SAXException sex) {
//            // ignore for now
//            return null;
//        } catch (IOException ioex) {
//            // ignore for now
//            return null;
//        }
//        return cm;
//    }

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
            if (opt != null && opt.getType() == File.class) {
                sawFile = true;
                final File optionFile = new File(options.getOne(opt.getName()));
                sawDirectory |= optionFile.isDirectory();
                sawUploadableFile |= optionFile.isFile();
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
