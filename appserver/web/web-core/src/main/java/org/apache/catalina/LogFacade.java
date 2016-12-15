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

package org.apache.catalina;

import org.glassfish.logging.annotation.LogMessageInfo;
import org.glassfish.logging.annotation.LoggerInfo;
import org.glassfish.logging.annotation.LogMessagesResourceBundle;
import java.util.logging.Logger;

/**
 *
 * Provides the logging facilities.
 *
 * @author Shing Wai Chan
 */
public class LogFacade {
    @LogMessagesResourceBundle
    private static final String SHARED_LOG_MESSAGE_RESOURCE =
             "org.apache.catalina.core.LogMessages";

    @LoggerInfo(subsystem = "WEB", description = "WEB Core Logger", publish = true)
    private static final String WEB_CORE_LOGGER = "javax.enterprise.web.core";

    private static final Logger LOGGER =
             Logger.getLogger(WEB_CORE_LOGGER, SHARED_LOG_MESSAGE_RESOURCE);

    private LogFacade() {}

    public static Logger getLogger() {
        return LOGGER;
    }

    private static final String prefix = "AS-WEB-CORE-";

    @LogMessageInfo(
            message = "Configuration error:  Must be attached to a Context",
            level = "WARNING"
    )
    public static final String CONFIG_ERROR_MUST_ATTACH_TO_CONTEXT = prefix + "00001";

    @LogMessageInfo(
            message = "Authenticator[{0}]: {1}",
            level = "INFO"
    )
    public static final String AUTHENTICATOR_INFO = prefix + "00002";

    @LogMessageInfo(
            message = "Exception getting debug value",
            level = "SEVERE",
            cause = "Could not get the method or invoke underlying method",
            action = "Verify the existence of such method and access permission"
    )
    public static final String GETTING_DEBUG_VALUE_EXCEPTION = prefix + "00003";

    @LogMessageInfo(
            message = "Unexpected error forwarding or redirecting to login page",
            level = "WARNING"
    )
    public static final String UNEXPECTED_ERROR_FORWARDING_TO_LOGIN_PAGE = prefix + "00004";

    @LogMessageInfo(
            message = "Started",
            level = "INFO"
    )
    public static final String START_COMPONENT_INFO = prefix + "00005";

    @LogMessageInfo(
            message = "Stopped",
            level = "INFO"
    )
    public static final String STOP_COMPONENT_INFO = prefix + "00006";

    @LogMessageInfo(
            message = "Process session destroyed on {0}",
            level = "INFO"
    )
    public static final String PROCESS_SESSION_DESTROYED_INFO = prefix + "00007";

    @LogMessageInfo(
            message = "Process request for '{0}'",
            level = "INFO"
    )
    public static final String PROCESS_REQUEST_INFO = prefix + "00008";

    @LogMessageInfo(
            message = "Principal {0} has already been authenticated",
            level = "INFO"
    )
    public static final String PRINCIPAL_BEEN_AUTHENTICATED_INFO = prefix + "00009";

    @LogMessageInfo(
            message = "Checking for SSO cookie",
            level = "INFO"
    )
    public static final String CHECK_SSO_COOKIE_INFO = prefix + "00010";

    @LogMessageInfo(
            message = "SSO cookie is not present",
            level = "INFO"
    )
    public static final String SSO_COOKIE_NOT_PRESENT_INFO = prefix + "00011";

    @LogMessageInfo(
            message = "Checking for cached principal for {0}",
            level = "INFO"
    )
    public static final String CHECK_CACHED_PRINCIPAL_INFO = prefix + "00012";

    @LogMessageInfo(
            message = "Found cached principal {0} with auth type {1}",
            level = "INFO"
    )
    public static final String FOUND_CACHED_PRINCIPAL_AUTH_TYPE_INFO = prefix + "00013";

    @LogMessageInfo(
            message = "No cached principal found, erasing SSO cookie",
            level = "INFO"
    )
    public static final String NO_CACHED_PRINCIPAL_FOUND_INFO = prefix + "00014";

    @LogMessageInfo(
            message = "Associate sso id {0} with session {1}",
            level = "INFO"
    )
    public static final String ASSOCIATE_SSO_WITH_SESSION_INFO = prefix + "00015";

    @LogMessageInfo(
            message = "Registering sso id {0} for user {1} with auth type {2}",
            level = "INFO"
    )
    public static final String REGISTERING_SSO_INFO = prefix + "00016";

    @LogMessageInfo(
            message = "Looking up certificates",
            level = "INFO"
    )
    public static final String LOOK_UP_CERTIFICATE_INFO = prefix + "00017";

    @LogMessageInfo(
            message = "No certificates included with this request",
            level = "INFO"
    )
    public static final String NO_CERTIFICATE_INCLUDED_INFO = prefix + "00018";

    @LogMessageInfo(
            message = "No client certificate chain in this request",
            level = "WARNING"
    )
    public static final String NO_CLIENT_CERTIFICATE_CHAIN = prefix + "00019";

    @LogMessageInfo(
            message = "Cannot authenticate with the provided credentials",
            level = "WARNING"
    )
    public static final String CANNOT_AUTHENTICATE_WITH_CREDENTIALS = prefix + "00020";

    @LogMessageInfo(
            message = "Unable to determine target of zero-arg dispatcher",
            level = "WARNING"
    )
    public static final String UNABLE_DETERMINE_TARGET_OF_DISPATCHER = prefix + "00021";

    @LogMessageInfo(
            message = "Unable to acquire RequestDispatcher for {0}",
            level = "WARNING"
    )
    public static final String UNABLE_ACQUIRE_REQUEST_DISPATCHER = prefix + "00022";

    @LogMessageInfo(
            message = "Unable to acquire RequestDispatcher for {0} in servlet context {1}",
            level = "WARNING"
    )
    public static final String UNABLE_ACQUIRE_REQUEST_DISPATCHER_IN_SERVLET_CONTEXT = prefix + "00023";

    @LogMessageInfo(
            message = "Error invoking AsyncListener",
            level = "WARNING"
    )
    public static final String ERROR_INVOKE_ASYNCLISTENER = prefix + "00024";

    @LogMessageInfo(
            message = "Asynchronous dispatch already in progress, must call ServletRequest.startAsync first",
            level = "WARNING"
    )
    public static final String ASYNC_DISPATCH_ALREADY_IN_PROGRESS_EXCEPTION = prefix + "00025";

    @LogMessageInfo(
            message = "Must not call AsyncContext.addListener after the container-initiated dispatch during which ServletRequest.startAsync was called has returned to the container",
            level = "WARNING"
    )
    public static final String ASYNC_CONTEXT_ADD_LISTENER_EXCEPTION = prefix + "00026";

    @LogMessageInfo(
            message = "Must not call AsyncContext.setTimeout after the container-initiated dispatch during which ServletRequest.startAsync was called has returned to the container",
            level = "WARNING"
    )
    public static final String ASYNC_CONTEXT_SET_TIMEOUT_EXCEPTION = prefix + "00027";

    @LogMessageInfo(
            message = "The connector has already been initialized",
            level = "INFO"
    )
    public static final String CONNECTOR_BEEN_INIT = prefix + "00028";

    @LogMessageInfo(
            message = "Error registering connector ",
            level = "SEVERE",
            cause = "Could not register connector",
            action = "Verify domain name and type"
    )
    public static final String ERROR_REGISTER_CONNECTOR_EXCEPTION = prefix + "00029";

    @LogMessageInfo(
            message = "Failed to instanciate HttpHandler ",
            level = "WARNING"
    )
    public static final String FAILED_INSTANCIATE_HTTP_HANDLER_EXCEPTION = prefix + "00030";

    @LogMessageInfo(
            message = "mod_jk invalid Adapter implementation: {0} ",
            level = "WARNING"
    )
    public static final String INVALID_ADAPTER_IMPLEMENTATION_EXCEPTION = prefix + "00031";

    @LogMessageInfo(
            message = "Protocol handler instantiation failed: {0}",
            level = "WARNING"
    )
    public static final String PROTOCOL_HANDLER_INIT_FAILED_EXCEPTION = prefix + "00032";

    @LogMessageInfo(
            message = "The connector has already been started",
            level = "INFO"
    )
    public static final String CONNECTOR_BEEN_STARTED = prefix + "00033";

    @LogMessageInfo(
            message = "Protocol handler start failed: {0}",
            level = "WARNING"
    )
    public static final String PROTOCOL_HANDLER_START_FAILED_EXCEPTION = prefix + "00034";

    @LogMessageInfo(
            message = "Coyote connector has not been started",
            level = "SEVERE",
            cause = "Could not stop processing requests via this Connector",
            action = "Verify if the connector has not been started"
    )
    public static final String CONNECTOR_NOT_BEEN_STARTED = prefix + "00035";

    @LogMessageInfo(
            message = "Protocol handler destroy failed: {0}",
            level = "WARNING"
    )
    public static final String PROTOCOL_HANDLER_DESTROY_FAILED_EXCEPTION = prefix + "00036";

    @LogMessageInfo(
            message = "An exception or error occurred in the container during the request processing",
            level = "SEVERE",
            cause = "Could not process the request in the container",
            action = "Verify certificate chain retrieved from the request header and the correctness of request"
    )
    public static final String REQUEST_PROCESSING_EXCEPTION = prefix + "00037";

    @LogMessageInfo(
            message = "HTTP listener on port {0} has been disabled",
            level = "FINE"
    )
    public static final String HTTP_LISTENER_DISABLED = prefix + "00038";

    @LogMessageInfo(
            message = "Error parsing client cert chain into array of java.security.cert.X509Certificate instances",
            level = "SEVERE",
            cause = "Could not get the SSL client certificate chain",
            action = "Verify certificate chain and the request"
    )
    public static final String PARSING_CLIENT_CERT_EXCEPTION = prefix + "00039";

    @LogMessageInfo(
            message = "No Host matches server name {0}",
            level = "INFO"
    )
    public static final String NO_HOST_MATCHES_SERVER_NAME_INFO = prefix + "00040";

    @LogMessageInfo(
            message = "Cannot use this object outside a servlet's service method or outside a filter's doFilter method",
            level = "WARNING"
    )
    public static final String OBJECT_INVALID_SCOPE_EXCEPTION = prefix + "00041";

    @LogMessageInfo(
            message = "Cannot set a null ReadListener object",
            level = "WARNING"
    )
    public static final String NULL_READ_LISTENER_EXCEPTION = prefix + "00042";

    @LogMessageInfo(
            message = "Cannot set a null WriteListener object",
            level = "WARNING"
    )
    public static final String NULL_WRITE_LISTENER_EXCEPTION = prefix + "00043";

    @LogMessageInfo(
            message = "Failed to skip {0} characters in the underlying buffer of CoyoteReader on readLine().",
            level = "WARNING"
    )
    public static final String FAILED_SKIP_CHARS_IN_BUFFER = prefix + "00044";

    @LogMessageInfo(
            message = "Stream closed",
            level = "WARNING"
    )
    public static final String STREAM_CLOSED = prefix + "00045";

    @LogMessageInfo(
            message = "Already set read listener",
            level = "WARNING"
    )
    public static final String ALREADY_SET_READ_LISTENER = prefix + "00046";

    @LogMessageInfo(
            message = "Cannot set ReaderListener for non-async or non-upgrade request",
            level = "WARNING"
    )
    public static final String NON_ASYNC_UPGRADE_READER_EXCEPTION = prefix + "00047";

    @LogMessageInfo(
            message = "Error in invoking ReadListener.onDataAvailable",
            level = "WARNING"
     )
    public static final String READ_LISTENER_ON_DATA_AVAILABLE_ERROR = prefix + "00048";

    @LogMessageInfo(
            message = "The WriteListener has already been set.",
            level = "WARNING"
    )
    public static final String WRITE_LISTENER_BEEN_SET = prefix + "00049";

    @LogMessageInfo(
            message = "Cannot set WriteListener for non-async or non-upgrade request",
            level = "WARNING"
    )
    public static final String NON_ASYNC_UPGRADE_WRITER_EXCEPTION = prefix + "00050";

    @LogMessageInfo(
            message = "Error in invoking WriteListener.onWritePossible",
            level = "WARNING"
    )
    public static final String WRITE_LISTENER_ON_WRITE_POSSIBLE_ERROR = prefix + "00051";

    @LogMessageInfo(
            message = "getReader() has already been called for this request",
            level = "WARNING"
    )
    public static final String GETREADER_BEEN_CALLED_EXCEPTION = prefix + "00052";

    @LogMessageInfo(
            message = "getInputStream() has already been called for this request",
            level = "WARNING"
    )
    public static final String GETINPUTSTREAM_BEEN_CALLED_EXCEPTION = prefix + "00053";

    @LogMessageInfo(
            message = "Unable to determine client remote address from proxy (returns null)",
            level = "WARNING"
    )
    public static final String UNABLE_DETERMINE_CLIENT_ADDRESS = prefix + "00054";

    @LogMessageInfo(
            message = "Unable to resolve IP address {0} into host name",
            level = "WARNING"
    )
    public static final String UNABLE_RESOLVE_IP_EXCEPTION = prefix + "00055";

    @LogMessageInfo(
            message = "Exception thrown by attributes event listener",
            level = "WARNING"
    )
    public static final String ATTRIBUTE_EVENT_LISTENER_EXCEPTION = prefix + "00056";

    @LogMessageInfo(
            message = "Cannot call setAttribute with a null name",
            level ="WARNING"
    )
    public static final String NULL_ATTRIBUTE_NAME_EXCEPTION = prefix + "00057";

    @LogMessageInfo(
            message = "Unable to determine canonical name of file [{0}] specified for use with sendfile",
            level = "WARNING"
    )
    public static final String UNABLE_DETERMINE_CANONICAL_NAME = prefix + "00058";

    @LogMessageInfo(
            message = "Unable to set request character encoding to {0} from context {1}, because request parameters have already been read, or ServletRequest.getReader() has already been called",
            level = "WARNING"
    )
    public static final String UNABLE_SET_REQUEST_CHARS = prefix + "00059";

    @LogMessageInfo(
            message = "Attempt to re-login while the user identity already exists",
            level = "SEVERE",
            cause = "Could not re-login",
            action = "Verify if user has already login"
    )
    public static final String ATTEMPT_RELOGIN_EXCEPTION = prefix + "00060";

    @LogMessageInfo(
            message = "changeSessionId has been called without a session",
            level = "WARNING"
    )
    public static final String CHANGE_SESSION_ID_BEEN_CALLED_EXCEPTION = prefix + "00061";

    @LogMessageInfo(
            message = "Cannot create a session after the response has been committed",
            level = "WARNING"
    )
    public static final String CANNOT_CREATE_SESSION_EXCEPTION = prefix + "00062";

    @LogMessageInfo(
            message = "Invalid URI encoding; using HTTP default",
            level = "SEVERE",
            cause = "Could not set URI converter",
            action = "Verify URI encoding, using HTTP default"
    )
    public static final String INVALID_URI_ENCODING = prefix + "00063";

    @LogMessageInfo(
            message = "Invalid URI character encoding; trying ascii",
            level = "SEVERE",
            cause = "Could not encode URI character",
            action = "Verify URI encoding, trying ascii"
    )
    public static final String INVALID_URI_CHAR_ENCODING = prefix + "00064";

    @LogMessageInfo(
            message = "Request is within the scope of a filter or servlet that does not support asynchronous operations",
            level = "WARNING"
    )
    public static final String REQUEST_WITHIN_SCOPE_OF_FILTER_OR_SERVLET_EXCEPTION = prefix + "00065";

    @LogMessageInfo(
            message = "ServletRequest.startAsync called again without any asynchronous dispatch, or called outside the scope of any such dispatch, or called again within the scope of the same dispatch",
            level = "WARNING"
    )
    public static final String START_ASYNC_CALLED_AGAIN_EXCEPTION = prefix + "00066";

    @LogMessageInfo(
            message = "Response already closed",
            level = "WARNING"
    )
    public static final String ASYNC_ALREADY_COMPLETE_EXCEPTION = prefix + "00067";

    @LogMessageInfo(
            message = "ServletRequest.startAsync called outside the scope of an async dispatch",
            level = "WARNING"
    )
    public static final String START_ASYNC_CALLED_OUTSIDE_SCOPE_EXCEPTION = prefix + "00068";

    @LogMessageInfo(
            message = "The request has not been put into asynchronous mode, must call ServletRequest.startAsync first",
            level = "WARNING"
    )
    public static final String REQUEST_NOT_PUT_INTO_ASYNC_MODE_EXCEPTION = prefix + "00069";

    @LogMessageInfo(
            message = "Request already released from asynchronous mode",
            level = "WARNING"
    )
    public static final String REQUEST_ALREADY_RELEASED_EXCEPTION = prefix + "00070";

    @LogMessageInfo(
            message = "Unable to perform error dispatch",
            level = "SEVERE",
            cause = "Could not perform post-request processing as required by this Valve",
            action = "Verify if I/O exception or servlet exception occur"
    )
    public static final String UNABLE_PERFORM_ERROR_DISPATCH = prefix + "00071";

    @LogMessageInfo(
            message = "Request.{0} is called without multipart configuration. Either add a @MultipartConfig to the servlet, or a multipart-config element to web.xml",
            level = "WARNING"
    )
    public static final String REQUEST_CALLED_WITHOUT_MULTIPART_CONFIG_EXCEPTION = prefix + "00072";

    @LogMessageInfo(
            message = "This should not happen-breaking background lock: sess = {0}",
            level = "WARNING"
    )
    public static final String BREAKING_BACKGROUND_LOCK_EXCEPTION = prefix + "00073";

    @LogMessageInfo(
            message = " Must not use request object outside the scope of a servlet's service or a filter's doFilter method",
            level = "WARNING"
    )
    public static final String CANNOT_USE_REQUEST_OBJECT_OUTSIDE_SCOPE_EXCEPTION = prefix + "00074";

    @LogMessageInfo(
            message = "Error during finishResponse",
            level = "WARNING"
    )
    public static final String ERROR_DURING_FINISH_RESPONSE = prefix + "00075";

    @LogMessageInfo(
            message = "getWriter() has already been called for this response",
            level = "WARNING"
    )
    public static final String GET_WRITER_BEEN_CALLED_EXCEPTION = prefix + "00076";

    @LogMessageInfo(
            message = "getOutputStream() has already been called for this response",
            level = "WARNING"
    )
    public static final String GET_OUTPUT_STREAM_BEEN_CALLED_EXCEPTION = prefix + "00077";

    @LogMessageInfo(
            message = "Cannot reset buffer after response has been committed",
            level = "WARNING"
    )
    public static final String CANNOT_RESET_BUFFER_EXCEPTION = prefix + "00078";

    @LogMessageInfo(
            message = "Cannot change buffer size after data has been written",
            level = "WARNING"
    )
    public static final String CANNOT_CHANGE_BUFFER_SIZE_EXCEPTION = prefix + "00079";

    @LogMessageInfo(
            message = "Cannot call sendError() after the response has been committed",
            level = "WARNING"
    )
    public static final String CANNOT_CALL_SEND_ERROR_EXCEPTION = prefix + "00080";

    @LogMessageInfo(
            message = "Cannot call sendRedirect() after the response has been committed",
            level = "WARNING"
    )
    public static final String CANNOT_CALL_SEND_REDIRECT_EXCEPTION = prefix + "00081";

    @LogMessageInfo(
            message = "Null response object",
            level = "WARNING"
    )
    public static final String NULL_RESPONSE_OBJECT = prefix + "00082";

    @LogMessageInfo(
        message = "Not allowed to call this javax.servlet.ServletContext method from a ServletContextListener that was neither declared in the application's deployment descriptor nor annotated with WebListener",
        level = "INFO"
    )
    public static final String UNSUPPORTED_OPERATION_EXCEPTION = prefix + "00083";

    @LogMessageInfo(
        message = "Exception thrown by attributes event listener",
        level = "WARNING",
        cause = "Could not modify attribute",
        action = "Verify name and value from Servlet Context"
    )
    public static final String ATTRIBUTES_EVENT_LISTENER_EXCEPTION = prefix + "00084";

    @LogMessageInfo(
        message = "Name cannot be null",
        level = "INFO"
    )
    public static final String ILLEGAL_ARGUMENT_EXCEPTION = prefix + "00085";

    @LogMessageInfo(
        message = "Cannot forward after response has been committed",
        level = "INFO"
    )
    public static final String ILLEGAL_STATE_EXCEPTION = prefix + "00086";

    @LogMessageInfo(
        message = "Servlet {0} is currently unavailable",
        level = "WARNING"
    )
    public static final String UNAVAILABLE_SERVLET = prefix + "00087";

    @LogMessageInfo(
        message = "Allocate exception for servlet {0}",
        level = "SEVERE",
        cause = "Could not allocate servlet instance",
        action = "Verify the configuration of wrapper"
    )
    public static final String ALLOCATE_SERVLET_EXCEPTION = prefix + "00088";

    @LogMessageInfo(
        message = "Exceeded maximum depth for nested request dispatches: {0}",
        level = "INFO"
    )
    public static final String MAX_DISPATCH_DEPTH_REACHED = prefix + "00089";

    @LogMessageInfo(
        message = "Servlet.service() for servlet {0} threw exception",
        level = "WARNING"
    )
    public static final String SERVLET_SERVICE_EXCEPTION = prefix + "00090";

    @LogMessageInfo(
        message = "Release filters exception for servlet {0}",
        level = "SEVERE",
        cause = "Could not release filter chain",
        action = "Verify the availability of current filter chain"
    )
    public static final String RELEASE_FILTERS_EXCEPTION_SEVERE = "AS-WEB-CORE-00091";

    @LogMessageInfo(
        message = "Deallocate exception for servlet {0}",
        level = "SEVERE",
        cause = "Could not deallocate the allocated servlet instance",
        action = "Verify the availability of servlet instance"
    )
    public static final String DEALLOCATE_SERVLET_EXCEPTION = prefix + "00092";

    @LogMessageInfo(
        message = "ApplicationDispatcher[{0}]: {1}",
        level = "INFO"
    )
    public static final String APPLICATION_DISPATCHER_INFO = prefix + "00093";

    @LogMessageInfo(
        message = "ApplicationDispatcher[{0}]: {1}",
        level = "WARNING",
        cause = "Could not get logger from parent context",
        action = "Verify if logger is null"
    )
    public static final String APPLICATION_DISPATCHER_WARNING = prefix + "00094";

    @LogMessageInfo(
            message = "Exception processing {0}",
            level = "WARNING")
    public static final String EXCEPTION_PROCESSING = prefix + "00095";

    @LogMessageInfo(
            message = "Exception sending default error page",
            level = "WARNING")
    public static final String EXCEPTION_SENDING_DEFAULT_ERROR_PAGE = prefix + "00096";

    @LogMessageInfo(
        message = "Filter execution threw an exception",
        level = "WARNING"
    )
    public static final String FILTER_EXECUTION_EXCEPTION = prefix + "00097";

    @LogMessageInfo(
        message = "ApplicationFilterConfig.doAsPrivilege",
        level = "SEVERE",
        cause = "Could not release allocated filter instance",
        action = "Verify the privilege"
    )
    public static final String DO_AS_PRIVILEGE = prefix + "00098";

    @LogMessageInfo(
        message = "ContainerBase.setLoader: stop: ",
        level = "SEVERE",
        cause = "Could not stop previous loader",
        action = "Verify previous loader"
    )
    public static final String CONTAINER_BASE_SET_LOADER_STOP = prefix + "00099";

    @LogMessageInfo(
        message = "ContainerBase.setLoader: start:",
        level = "SEVERE",
        cause = "Could not start new loader",
        action = "Verify the configuration of container"
    )
    public static final String CONTAINER_BASE_SET_LOADER_START = prefix + "00100";

    @LogMessageInfo(
        message = "ContainerBase.setLogger: stop: ",
        level = "SEVERE",
        cause = "Could not stop previous logger",
        action = "Verify previous logger"
    )
    public static final String CONTAINER_BASE_SET_LOGGER_STOP = prefix + "00101";

    @LogMessageInfo(
        message = "ContainerBase.setLogger: start: ",
        level = "SEVERE",
        cause = "Could not start new logger",
        action = "Verify the configuration of container"
    )
    public static final String CONTAINER_BASE_SET_LOGGER_START = prefix + "00102";

    @LogMessageInfo(
        message = "ContainerBase.setManager: stop: ",
        level = "SEVERE",
        cause = "Could not stop previous manager",
        action = "Verify previous manager"
    )
    public static final String CONTAINER_BASE_SET_MANAGER_STOP = prefix + "00103";

    @LogMessageInfo(
        message = "ContainerBase.setManager: start: ",
        level = "SEVERE",
        cause = "Could not start new manager",
        action = "Verify the configuration of container"
    )
    public static final String CONTAINER_BASE_SET_MANAGER_START = prefix + "00104";

    @LogMessageInfo(
        message = "ContainerBase.setRealm: stop: ",
        level = "SEVERE",
        cause = "Could not stop previous realm",
        action = "Verify previous realm"
    )
    public static final String CONTAINER_BASE_SET_REALM_STOP = prefix + "00105";

    @LogMessageInfo(
        message = "ContainerBase.setRealm: start: ",
        level = "SEVERE",
        cause = "Could not start new realm",
        action = "Verify the configuration of container"
    )
    public static final String CONTAINER_BASE_SET_REALM_START = prefix + "00106";

    @LogMessageInfo(
        message = "addChild: Child name {0} is not unique",
        level = "WARNING"
    )
    public static final String DUPLICATE_CHILD_NAME_EXCEPTION = prefix + "00107";

    @LogMessageInfo(
        message = "ContainerBase.addChild: start: ",
        level = "SEVERE",
        cause = "Could not start new child container",
        action = "Verify the configuration of parent container"
    )
    public static final String CONTAINER_BASE_ADD_CHILD_START = prefix + "00108";

    @LogMessageInfo(
        message = "ContainerBase.removeChild: stop: ",
        level = "SEVERE",
        cause = "Could not stop existing child container",
        action = "Verify existing child container"
    )
    public static final String CONTAINER_BASE_REMOVE_CHILD_STOP = prefix + "00109";

    @LogMessageInfo(
        message = "Container {0} has already been started",
        level = "INFO"
    )
    public static final String CONTAINER_STARTED = prefix + "00110";

    @LogMessageInfo(
        message = "Container {0} has not been started",
        level = "SEVERE",
        cause = "Current container has not been started",
        action = "Verify the current container"
    )
    public static final String CONTAINER_NOT_STARTED_EXCEPTION = prefix + "00111";

    @LogMessageInfo(
        message = "Error stopping container {0}",
        level = "SEVERE",
        cause = "Could not stop child container",
        action = "Verify the existence of current child container"
    )
    public static final String ERROR_STOPPING_CONTAINER = prefix + "00112";

    @LogMessageInfo(
        message = "Error unregistering ",
        level = "SEVERE",
        cause = "Could not unregister current container",
        action = "Verify if the container has been registered"
    )
    public static final String ERROR_UNREGISTERING = prefix + "00113";

    @LogMessageInfo(
        message = "Exception invoking periodic operation: ",
        level = "SEVERE",
        cause = "Could not set the context ClassLoader",
        action = "Verify the security permission"
    )
    public static final String EXCEPTION_INVOKES_PERIODIC_OP = prefix + "00114";

    @LogMessageInfo(
        message = "Unable to configure {0} for filter {1} of servlet context {2}, because this servlet context has already been initialized",
        level = "WARNING"
    )
    public static final String DYNAMIC_FILTER_REGISTRATION_ALREADY_INIT = prefix + "00115";

    @LogMessageInfo(
        message = "Unable to configure {0} for servlet {1} of servlet context {2}, because this servlet context has already been initialized",
        level = "WARNING"
    )
    public static final String DYNAMIC_SERVLET_REGISTRATION_ALREADY_INIT = prefix + "00116";

    @LogMessageInfo(
        message = "Unable to configure {0} for filter {1} of servlet context {2}, because this servlet context has already been initialized",
        level = "WARNING"
    )
    public static final String FILTER_REGISTRATION_ALREADY_INIT = prefix + "00117";

    @LogMessageInfo(
        message = "Unable to configure mapping for filter {0} of servlet context {1}, because servlet names are null or empty",
        level = "WARNING"
    )
    public static final String FILTER_REGISTRATION_MAPPING_SERVLET_NAME_EXCEPTION = prefix + "00118";

    @LogMessageInfo(
        message = "Unable to configure mapping for filter {0} of servlet context {1}, because URL patterns are null or empty",
        level = "WARNING"
    )
    public static final String FILTER_REGISTRATION_MAPPING_URL_PATTERNS_EXCEPTION = prefix + "00119";

    @LogMessageInfo(
        message = "Creation of the naming context failed: {0}",
        level = "WARNING"
    )
    public static final String CREATION_NAMING_CONTEXT_FAILED = prefix + "00120";

    @LogMessageInfo(
        message = "Failed to bind object: {0}",
        level = "WARNING"
    )
    public static final String BIND_OBJECT_FAILED = prefix + "00121";

    @LogMessageInfo(
        message = "Environment entry {0} has an invalid type",
        level = "WARNING"
    )
    public static final String ENV_ENTRY_INVALID_TYPE = prefix + "00122";

    @LogMessageInfo(
        message = "Environment entry {0} has an invalid value",
        level = "WARNING"
    )
    public static final String ENV_ENTRY_INVALID_VALUE = prefix + "00123";

    @LogMessageInfo(
        message = "Failed to unbind object: {0}",
        level = "WARNING"
    )
    public static final String UNBIND_OBJECT_FAILED = prefix + "00124";

    @LogMessageInfo(
        message = "Must not use request object outside the scope of a servlet's service or a filter's doFilter method",
        level = "WARNING"
    )
    public static final String VALIDATE_REQUEST_EXCEPTION = prefix + "00125";

    @LogMessageInfo(
        message = "Null response object",
        level = "WARNING"
    )
    public static final String VALIDATE_RESPONSE_EXCEPTION = prefix + "00126";

    @LogMessageInfo(
        message = "Unable to configure {0} for servlet {1} of servlet context {2}, because this servlet context has already been initialized",
        level = "WARNING"
    )
    public static final String SERVLET_REGISTRATION_ALREADY_INIT = prefix + "00127";

    @LogMessageInfo(
        message = "Unable to configure mapping for servlet {0} of servlet context {1}, because URL patterns are null or empty",
        level = "WARNING"
    )
    public static final String SERVLET_REGISTRATION_MAPPING_URL_PATTERNS_EXCEPTION = prefix + "00128";

    @LogMessageInfo(
        message = "Unable to configure {0} session tracking cookie property for servlet context {1}, because this servlet context has already been initialized",
        level = "WARNING"
    )
    public static final String SESSION_COOKIE_CONFIG_ALREADY_INIT = prefix + "00129";

    @LogMessageInfo(
        message = "Missing alternate docbase URL pattern or directory location",
        level = "WARNING"
    )
    public static final String MISS_PATH_OR_URL_PATTERN_EXCEPTION = prefix + "00130";

    @LogMessageInfo(
        message = "LoginConfig cannot be null",
        level = "WARNING"
    )
    public static final String LOGIN_CONFIG_REQUIRED_EXCEPTION = prefix + "00131";

    @LogMessageInfo(
        message = "Form login page {0} must start with a ''/''",
        level = "WARNING"
    )
    public static final String LOGIN_CONFIG_LOGIN_PAGE_EXCEPTION = prefix + "00132";

    @LogMessageInfo(
        message = "Form error page {0} must start with a ''/''",
        level = "WARNING"
    )
    public static final String LOGIN_CONFIG_ERROR_PAGE_EXCEPTION = prefix + "00133";

    @LogMessageInfo(
        message = "Child of a Context must be a Wrapper",
        level = "WARNING"
    )
    public static final String NO_WRAPPER_EXCEPTION = prefix + "00134";

    @LogMessageInfo(
        message = "JSP file {0} must start with a ''/''",
        level = "WARNING"
    )
    public static final String WRAPPER_ERROR_EXCEPTION = prefix + "00135";

    @LogMessageInfo(
        message = "Invalid <url-pattern> {0} in security constraint",
        level = "WARNING"
    )
    public static final String SECURITY_CONSTRAINT_PATTERN_EXCEPTION = prefix + "00136";

    @LogMessageInfo(
        message = "ErrorPage cannot be null",
        level = "WARNING"
    )
    public static final String ERROR_PAGE_REQUIRED_EXCEPTION = prefix + "00137";

    @LogMessageInfo(
        message = "Error page location {0} must start with a ''/''",
        level = "WARNING"
    )
    public static final String ERROR_PAGE_LOCATION_EXCEPTION = prefix + "00138";

    @LogMessageInfo(
        message = "Invalid status code {0} for error-page mapping. HTTP error codes are defined in the range from 400-600",
        level = "SEVERE",
        cause = "Invalid error page code",
        action = "Verify the error code"
    )
    public static final String INVALID_ERROR_PAGE_CODE_EXCEPTION = prefix + "00139";

    @LogMessageInfo(
        message = "Filter mapping specifies an unknown filter name {0}",
        level = "WARNING"
    )
    public static final String FILTER_MAPPING_NAME_EXCEPTION = prefix + "00140";

    @LogMessageInfo(
        message = "Filter mapping must specify either a <url-pattern> or a <servlet-name>",
        level = "WARNING"
    )
    public static final String FILTER_MAPPING_EITHER_EXCEPTION = prefix + "00141";

    @LogMessageInfo(
        message = "Invalid <url-pattern> {0} in filter mapping",
        level = "WARNING"
    )
    public static final String FILTER_MAPPING_INVALID_URL_EXCEPTION = prefix + "00142";

    @LogMessageInfo(
        message = "Unable to call method {0} on servlet context {1}, because this servlet context has already been initialized",
        level = "WARNING"
    )
    public static final String SERVLET_CONTEXT_ALREADY_INIT_EXCEPTION = prefix + "00143";

    @LogMessageInfo(
        message = "Filter name is null or an empty String",
        level = "WARNING"
    )
    public static final String NULL_EMPTY_FILTER_NAME_EXCEPTION = prefix + "00144";

    @LogMessageInfo(
        message = "Unable to set {0} session tracking mode on servlet context {1}, because it is not supported",
        level = "WARNING"
    )
    public static final String UNSUPPORTED_TRACKING_MODE_EXCEPTION = prefix + "00145";

    @LogMessageInfo(
        message = "Unable to add listener of type: {0}, because it does not implement any of the required ServletContextListener, ServletContextAttributeListener, ServletRequestListener, ServletRequestAttributeListener, HttpSessionListener, or HttpSessionAttributeListener interfaces",
        level = "WARNING"
    )
    public static final String UNABLE_ADD_LISTENER_EXCEPTION = prefix + "00146";

    @LogMessageInfo(
        message = "Both parameter name and parameter value are required, parameter name is {0}",
        level = "WARNING"
    )
    public static final String PARAMETER_REQUIRED_EXCEPTION = prefix + "00147";

    @LogMessageInfo(
        message = "Duplicate context initialization parameter {0}",
        level = "WARNING"
    )
    public static final String DUPLICATE_PARAMETER_EXCEPTION = prefix + "00148";

    @LogMessageInfo(
        message = "Invalid <url-pattern> {0} in servlet mapping",
        level = "WARNING"
    )
    public static final String SERVLET_MAPPING_INVALID_URL_EXCEPTION = prefix + "00149";

    @LogMessageInfo(
        message = "Servlet mapping specifies an unknown servlet name {0}",
        level = "WARNING"
    )
    public static final String SERVLET_MAPPING_UNKNOWN_NAME_EXCEPTION = prefix + "00150";

    @LogMessageInfo(
        message = "Unable to map Servlet [{0}] to URL pattern [{1}], because Servlet [{2}] is already mapped to it",
        level = "WARNING"
    )
    public static final String DUPLICATE_SERVLET_MAPPING_EXCEPTION = prefix + "00151";

    @LogMessageInfo(
        message = "Error creating instance listener {0}",
        level = "SEVERE",
        cause = "Could not create new instance",
        action = "Verify the configuration of Wrapper and InstanceListener"
    )
    public static final String CREATING_INSTANCE_LISTENER_EXCEPTION = prefix + "00152";

    @LogMessageInfo(
        message = "Error creating lifecycle listener {0}",
        level = "SEVERE",
        cause = "Could not create new instance for lifecycle listener",
        action = "Verify the permit of current class to access newInstance()"
    )
    public static final String CREATING_LIFECYCLE_LISTENER_EXCEPTION = prefix + "00153";

    @LogMessageInfo(
        message = "Error creating container listener {0}",
        level = "SEVERE",
        cause = "Could not create new instance for container listener",
        action = "Verify the permit of current class to access newInstance()"
    )
    public static final String CREATING_CONTAINER_LISTENER_EXCEPTION = prefix + "00154";

    @LogMessageInfo(
        message = "Reloading this Context has started",
        level = "INFO"
    )
    public static final String RELOADING_STARTED = prefix + "00155";

    @LogMessageInfo(
        message = "Error stopping context {0}",
        level = "SEVERE",
        cause = "Could not stop context component",
        action = "Verify stop() to guarantee the whole domain is being stopped correctly"
    )
    public static final String STOPPING_CONTEXT_EXCEPTION = prefix + "00156";

    @LogMessageInfo(
        message = "Error starting context {0}",
        level = "SEVERE",
        cause = "Could not start context component",
        action = "Verify start() to guarantee the context component is being started correctly"
    )
    public static final String STARTING_CONTEXT_EXCEPTION = prefix + "00157";

    @LogMessageInfo(
        message = "Error invoking requestInitialized method on ServletRequestListener {0}",
        level = "WARNING"
    )
    public static final String REQUEST_INIT_EXCEPTION = prefix + "00158";

    @LogMessageInfo(
        message = "Error invoking requestDestroyed method on ServletRequestListener {0}",
        level = "WARNING"
    )
    public static final String REQUEST_DESTROY_EXCEPTION = prefix + "00159";

    @LogMessageInfo(
        message = "Exception starting filter {0}",
        level = "WARNING"
    )
    public static final String STARTING_FILTER_EXCEPTION = prefix + "00160";

    @LogMessageInfo(
        message = "Servlet with name {0} does not have any servlet-class or jsp-file configured",
        level = "WARNING"
    )
    public static final String SERVLET_WITHOUT_ANY_CLASS_OR_JSP = prefix + "00161";

    @LogMessageInfo(
        message = "Filter with name {0} does not have any class configured",
        level = "WARNING"
    )
    public static final String FILTER_WITHOUT_ANY_CLASS = prefix + "00162";

    @LogMessageInfo(
        message = "Exception sending context destroyed event to listener instance of class {0}",
        level = "WARNING"
    )
    public static final String LISTENER_STOP_EXCEPTION = prefix + "00163";

    @LogMessageInfo(
        message = "Error starting resources in context {0}",
        level = "SEVERE",
        cause = "Could not get the proxy directory context",
        action = "Verify the existence of the context"
    )
    public static final String STARTING_RESOURCES_EXCEPTION = prefix + "00164";

    @LogMessageInfo(
        message = "Error stopping static resources",
        level = "SEVERE",
        cause = "Could not deallocate resource and destroy proxy",
        action = "Verify if a fatal error that prevents this component from being used"
    )
    public static final String STOPPING_RESOURCES_EXCEPTION = prefix + "00165";

    @LogMessageInfo(
        message = "Current container has already been started with a DirContext object",
        level = "WARNING"
    )
    public static final String RESOURCES_STARTED = prefix + "00166";

    @LogMessageInfo(
        message = "Error starting resources in context {0} with Exception message: {1}",
        level = "SEVERE",
        cause = "Could not get the proxy directory context",
        action = "Verify the existence of the context"
    )
    public static final String STARTING_RESOURCE_EXCEPTION_MESSAGE = prefix + "00167";

    @LogMessageInfo(
        message = "Form login page {0} must start with a ''/'' in Servlet 2.4",
        level = "FINE"
    )
    public static final String FORM_LOGIN_PAGE_FINE = prefix + "00168";

    @LogMessageInfo(
        message = "Form error page {0} must start with a ''/'' in Servlet 2.4",
        level = "FINE"
    )
    public static final String FORM_ERROR_PAGE_FINE = prefix + "00169";

    @LogMessageInfo(
        message = "JSP file {0} must start with a ''/'' in Servlet 2.4",
        level = "FINE"
    )
    public static final String JSP_FILE_FINE = prefix + "00170";

    @LogMessageInfo(
        message = "Container {0} has already been started",
        level = "INFO"
    )
    public static final String CONTAINER_ALREADY_STARTED_EXCEPTION = prefix + "00171";

    @LogMessageInfo(
        message = "Error initialzing resources{0}",
        level = "WARNING"
    )
    public static final String INIT_RESOURCES_EXCEPTION = prefix + "00172";

    @LogMessageInfo(
        message = "Error in dependency check for standard context {0}",
        level = "WARNING"
    )
    public static final String DEPENDENCY_CHECK_EXCEPTION = prefix + "00173";

    @LogMessageInfo(
        message = "Startup of context {0} failed due to previous errors",
        level = "SEVERE",
        cause = "Could not startup servlet",
        action = "Verify the initialization process"
    )
    public static final String STARTUP_CONTEXT_FAILED_EXCEPTION = prefix + "00174";

    @LogMessageInfo(
        message = "Exception during cleanup after start failed",
        level = "SEVERE",
        cause = "Stop staring up failed",
        action = "Verify configurations to stop starting up"
    )
    public static final String CLEANUP_FAILED_EXCEPTION = prefix + "00175";

    @LogMessageInfo(
        message = "Error invoking ServletContainerInitializer {0}",
        level = "SEVERE",
        cause = "Could not instantiate servlet container initializer",
        action = "Verify the access permission of current class loader"
    )
    public static final String INVOKING_SERVLET_CONTAINER_INIT_EXCEPTION = prefix + "00176";

    @LogMessageInfo(
        message = "Error resetting context {0}",
        level = "SEVERE",
        cause = "Could not restore original state",
        action = "Verify if extend 'this' method, and make sure to clean up"
    )
    public static final String RESETTING_CONTEXT_EXCEPTION = prefix + "00177";

    @LogMessageInfo(
        message = "URL pattern {0} must start with a ''/'' in Servlet 2.4",
        level = "FINE"
    )
    public static final String URL_PATTERN_WARNING = prefix + "00178";

    @LogMessageInfo(
        message = "Failed to create work directory {0}",
        level = "SEVERE",
        cause = "Could not create work directory",
        action = "Verify the directory name, and access permission"
    )
    public static final String CREATE_WORK_DIR_EXCEPTION = prefix + "00179";

    @LogMessageInfo(
        message = "The URL pattern {0} contains a CR or LF and so can never be matched",
        level = "WARNING"
    )
    public static final String URL_PATTERN_CANNOT_BE_MATCHED_EXCEPTION = prefix + "00180";

    @LogMessageInfo(
        message = "Missing name attribute in {0}",
        level = "SEVERE",
        cause = "Could not get the attribute",
        action = "Verify the existence of the value associated with the key"
    )
    public static final String MISSING_ATTRIBUTE = prefix + "00181";

    @LogMessageInfo(
        message = "Malformed name {0}, value of name attribute does not start with ''//''",
        level = "SEVERE",
        cause = "Illegal path name",
        action = "Verify path name"
    )
    public static final String MALFORMED_NAME = prefix + "00182";

    @LogMessageInfo(
        message = "Path {0} does not start with ''/''",
        level = "WARNING"
    )
    public static final String INCORRECT_PATH = prefix + "00183";

    @LogMessageInfo(
        message = "Path {0} does not start with ''/'' and is not empty",
        level = "WARNING"
    )
    public static final String INCORRECT_OR_NOT_EMPTY_PATH = prefix + "00184";

    @LogMessageInfo(
        message = "Error during mapping",
        level = "WARNING"
    )
    public static final String MAPPING_ERROR_EXCEPTION = prefix + "00185";

    @LogMessageInfo(
        message = "Unable to create custom ObjectInputStream",
        level = "SEVERE",
        cause = "Could not create custom ObjectInputStream",
        action = "Verify input stream and class loader"
    )
    public static final String CANNOT_CREATE_OBJECT_INPUT_STREAM = prefix + "00186";

    @LogMessageInfo(
        message = "Error during bindThread",
        level = "WARNING"
    )
    public static final String BIND_THREAD_EXCEPTION = prefix + "00187";

    @LogMessageInfo(
        message = "Servlet {0} threw load() exception",
        level = "WARNING"
    )
    public static final String SERVLET_LOAD_EXCEPTION = prefix + "00188";

    @LogMessageInfo(
        message = "Error updating ctx with jmx {0} {1} {2}",
        level = "INFO"
    )
    public static final String ERROR_UPDATING_CTX_INFO = prefix + "00189";

    @LogMessageInfo(
        message = "Error registering wrapper with jmx {0} {1} {2}",
        level = "INFO"
    )
    public static final String ERROR_REGISTERING_WRAPPER_INFO = prefix + "00190";

    @LogMessageInfo(
            message = "Null filter instance",
            level = "WARNING"
    )
    public static final String NULL_FILTER_INSTANCE_EXCEPTION = prefix + "00191";

    @LogMessageInfo(
            message = "Servlet name is null or an empty String",
            level = "WARNING"
    )
    public static final String NULL_EMPTY_SERVLET_NAME_EXCEPTION = prefix + "00192";

    @LogMessageInfo(
            message = "Null servlet instance",
            level = "WARNING"
    )
    public static final String NULL_SERVLET_INSTANCE_EXCEPTION = prefix + "00193";

    @LogMessageInfo(
        message = "Child of an Engine must be a Host",
        level = "WARNING"
    )
    public static final String CHILD_OF_ENGINE_MUST_BE_HOST_EXCEPTION = prefix + "00194";

    @LogMessageInfo(
        message = "Engine cannot have a parent Container",
        level = "WARNING"
    )
    public static final String CANNOT_HAVE_PARENT_CONTAINER_EXCEPTION = prefix + "00195";

    @LogMessageInfo(
        message = "Error registering",
        level = "WARNING"
    )
    public static final String ERROR_REGISTERING_EXCEPTION = prefix + "00196";

    @LogMessageInfo(
        message = "No Host matches server name {0}",
        level = "WARNING"
    )
    public static final String NO_HOST_MATCH = prefix + "00197";

     @LogMessageInfo(
         message = "Host name is required",
         level = "WARNING"
     )
     public static final String HOST_NAME_REQUIRED_EXCEPTION = prefix + "00198";

     @LogMessageInfo(
         message = "Child of a Host must be a Context",
         level = "WARNING"
     )
     public static final String CHILD_MUST_BE_CONTEXT_EXCEPTION = prefix + "00199";

     @LogMessageInfo(
         message = "MAPPING configuration error for request URI {0}",
         level = "SEVERE",
         cause = "No context has been selected",
         action = "Verify the uri or default context"
     )
     public static final String MAPPING_CONF_REQUEST_URI_EXCEPTION = prefix + "00200";

     @LogMessageInfo(
         message = "ErrorPage must not be null",
         level = "WARNING"
     )
     public static final String ERROR_PAGE_CANNOT_BE_NULL_EXCEPTION = prefix + "00201";

     @LogMessageInfo(
         message = "XML validation enabled",
         level = "FINE"
     )
     public static final String XML_VALIDATION_ENABLED = prefix + "00202";

     @LogMessageInfo(
         message = "Create Host deployer for direct deployment ( non-jmx )",
         level = "INFO"
     )
     public static final String CREATE_HOST_DEPLOYER_INFO = prefix + "00203";

     @LogMessageInfo(
         message = "Error creating deployer ",
         level = "SEVERE",
         cause = "Could not instantiate deployer",
         action = "Verify access permission"
     )
     public static final String ERROR_CREATING_DEPLOYER_EXCEPTION = prefix + "00204";

     @LogMessageInfo(
         message = "Error registering host {0}",
         level = "SEVERE",
         cause = "Initialization failed",
         action = "Verify domain and host name"
     )
     public static final String ERROR_REGISTERING_HOST_EXCEPTION = prefix + "00205";

     @LogMessageInfo(
         message = "Couldn't load specified error report valve class: {0}",
         level = "SEVERE",
         cause = "Could not load instance of host valve",
         action = "Verify access permission"
     )
     public static final String LOAD_SPEC_ERROR_REPORT_EXCEPTION = prefix + "00206";

    @LogMessageInfo(
        message = "Context path is required",
        level = "WARNING"
    )
    public static final String CONTEXT_PATH_REQUIRED_EXCEPTION = prefix + "00207";

    @LogMessageInfo(
        message = "Invalid context path: {0}",
        level = "WARNING"
    )
    public static final String INVALID_CONTEXT_PATH_EXCEPTION = prefix + "00208";

    @LogMessageInfo(
        message = "Context path {0} is already in use",
        level = "WARNING"
    )
    public static final String CONTEXT_PATH_ALREADY_USED_EXCEPTION = prefix + "00209";

    @LogMessageInfo(
        message = "URL to web application archive is required",
        level = "WARNING"
    )
    public static final String URL_WEB_APP_ARCHIVE_REQUIRED_EXCEPTION = prefix + "00210";

    @LogMessageInfo(
        message = "Installing web application at context path {0} from URL {1}",
        level = "INFO"
    )
    public static final String INSTALLING_WEB_APP_INFO = prefix + "00211";

    @LogMessageInfo(
        message = "Invalid URL for web application archive: {0}",
        level = "WARNING"
    )
    public static final String INVALID_URL_WEB_APP_EXCEPTION = prefix + "00212";

    @LogMessageInfo(
        message = "Only web applications in the Host web application directory can be installed, invalid URL: {0}",
        level = "WARNING"
    )
    public static final String HOST_WEB_APP_DIR_CAN_BE_INSTALLED_EXCEPTION = prefix + "00213";

    @LogMessageInfo(
        message = "Context path {0} must match the directory or WAR file name: {1}",
        level = "WARNING"
    )
    public static final String CONSTEXT_PATH_MATCH_DIR_WAR_NAME_EXCEPTION = prefix + "00214";

    @LogMessageInfo(
        message = "Error installing",
        level = "WARNING"
    )
    public static final String ERROR_INSTALLING_EXCEPTION = prefix + "00215";

    @LogMessageInfo(
        message = "Error deploying application at context path {0}",
        level = "SEVERE",
        cause = "Could not initiate life cycle listener",
        action = "Verify the access permission"
    )
    public static final String ERROR_DEPLOYING_APP_CONTEXT_PATH_EXCEPTION = prefix + "00216";

    @LogMessageInfo(
        message = "URL to configuration file is required",
        level = "WARNING"
    )
    public static final String URL_CONFIG_FILE_REQUIRED_EXCEPTION = prefix + "00217";

    @LogMessageInfo(
        message = "Use of configuration file is not allowed",
        level = "WARNING"
    )
    public static final String USE_CONFIG_FILE_NOT_ALLOWED = prefix + "00218";

    @LogMessageInfo(
        message = "Processing Context configuration file URL {0}",
        level = "INFO"
    )
    public static final String PROCESSING_CONTEXT_CONFIG_INFO = prefix + "00219";

    @LogMessageInfo(
        message = "Installing web application from URL {0}",
        level = "INFO"
    )
    public static final String INSTALLING_WEB_APP_FROM_URL_INFO = prefix + "00220";

    @LogMessageInfo(
        message = "Context path {0} is not currently in use",
        level = "WARNING"
    )
    public static final String CONTEXT_PATH_NOT_IN_USE = prefix + "00221";

    @LogMessageInfo(
        message = "Removing web application at context path {0}",
        level = "INFO"
    )
    public static final String REMOVING_WEB_APP_INFO = prefix + "00222";

    @LogMessageInfo(
        message = "Error removing application at context path {0}",
        level = "SEVERE",
        cause = "Could not remove an existing child Container",
        action = "Verify if there are any I/O errors"
    )
    public static final String ERROR_REMOVING_APP_EXCEPTION = prefix + "00223";

    @LogMessageInfo(
        message = "Starting web application at context path {0}",
        level = "INFO"
    )
    public static final String STARTING_WEB_APP_INFO = prefix + "00224";

    @LogMessageInfo(
        message = "Starting web application at context path {0} failed",
        level = "SEVERE",
        cause = "Could not start web application at current context path",
        action = "Verify if start() is called before any of the public " +
                 "methods of this component are utilized, and it should " +
                 "send START_EVENT to any registered listeners"
    )
    public static final String STARTING_WEB_APP_FAILED_EXCEPTION = prefix + "00225";

    @LogMessageInfo(
        message = "Stopping web application at context path {0}",
        level = "INFO"
    )
    public static final String STOPPING_WEB_APP_INFO = prefix + "00226";

    @LogMessageInfo(
        message = "Stopping web application at context path {0} failed",
        level = "SEVERE",
        cause = "Could not terminate the active use of the public methods of this component",
        action = "Verify if stop() is the last one called on a given instance of this component, " +
                 "and it should send STOP_EVENT to any registered listeners"
    )
    public static final String STOPPING_WEB_APP_FAILED_EXCEPTION = prefix + "00227";

    @LogMessageInfo(
        message = "Failed to remove file {0}",
        level = "WARNING"
    )
    public static final String FAILED_REMOVE_FILE = prefix + "00228";

    @LogMessageInfo(
        message = "Remote Client Aborted Request, IOException: {0}",
        level = "FINE"
    )
    public static final String REMOTE_CLIENT_ABORTED_EXCEPTION = prefix + "00229";

    @LogMessageInfo(
        message = "The error-page {0} or {1} does not exist",
        level = "WARNING"
    )
    public static final String ERROR_PAGE_NOT_EXIST = prefix + "00230";

    @LogMessageInfo(
        message = "No Context configured to process this request",
        level = "WARNING"
    )
    public static final String NO_CONTEXT_TO_PROCESS = prefix + "00231";

    @LogMessageInfo(
        message = "Pipeline has already been started",
        level = "WARNING"
    )
    public static final String PIPLINE_STARTED = prefix + "00232";

    @LogMessageInfo(
        message = "Pipeline has not been started",
        level = "WARNING"
    )
    public static final String PIPLINE_NOT_STARTED = prefix + "00233";

    @LogMessageInfo(
        message = "Exception occurred when stopping GlassFishValve in StandardPipeline.setBasic",
        level = "SEVERE",
        cause = "Could not terminate the active use of the public methods of this component",
        action = "Verify if stop() is the last one called on a given instance of this component, " +
                 "and it should send STOP_EVENT to any registered listeners"
    )
    public static final String SET_BASIC_STOP_EXCEPTION = prefix + "00234";

    @LogMessageInfo(
        message = "Exception occurred when starting GlassFishValve in StandardPipeline.setBasic",
        level = "SEVERE",
        cause = "Could not prepare for the beginning of active use of the public methods of this component",
        action = "Verify if start() is called before any of the public " +
                 "methods of this component are utilized, and it should " +
                 "send START_EVENT to any registered listeners"
    )
    public static final String SET_BASIC_START_EXCEPTION = prefix + "00235";

    @LogMessageInfo(
        message = "Exception occurred when starting GlassFishValve in StandardPipline.addValve",
        level = "SEVERE",
        cause = "Specific valve could not be associated with current container",
        action = "Verify the availability of current valve"
    )
    public static final String ADD_VALVE_EXCEPTION = prefix + "00236";

    @LogMessageInfo(
        message = "Unable to add valve {0}",
        level = "SEVERE",
        cause = "Could not add tomcat-style valve",
        action = "Verify if this is a GlassFish-style valve that was compiled against" +
                 " the old org.apache.catalina.Valve interface"
    )
    public static final String ADD_TOMCAT_STYLE_VALVE_EXCEPTION = prefix + "00237";

    @LogMessageInfo(
        message = "No more Valves in the Pipeline processing this request",
        level = "WARNING"
    )
    public static final String NO_VALVES_IN_PIPELINE_EXCEPTION = prefix + "00238";

    @LogMessageInfo(
        message = "HttpUpgradeHandler handler cannot be null",
        level = "WARNING"
    )
    public static final String PROTOCOL_HANDLER_REQUIRED_EXCEPTION = prefix + "00239";

    @LogMessageInfo(
        message = "Exception occurred when stopping GlassFishValve in StandardPipeline.removeValve",
        level = "SEVERE",
        cause = "Could not terminate the active use of the public methods of this component",
        action = "Verify if stop() is the last one called on a given instance of this component, " +
                 "and it should send STOP_EVENT to any registered listeners"
    )
    public static final String REMOVE_VALVE_EXCEPTION = prefix + "00240";

    @LogMessageInfo(
        message = "StandardPipeline[{0}]: {1}",
        level = "INFO"
    )
    public static final String STANDARD_PIPELINE_INFO = prefix + "00241";

    @LogMessageInfo(
        message = "StandardPipeline[null]: {0}",
        level = "INFO"
    )
    public static final String STANDARD_PIPELINE_NULL_INFO = prefix + "00242";

     @LogMessageInfo(
         message = "LifecycleException occurred during service initialization: {0}",
         level = "SEVERE",
         cause = "This service was already initialized",
         action = "Verify if the service is not already initialized")
     public static final String LIFECYCLE_EXCEPTION_DURING_SERVICE_INIT = prefix + "00243";

     @LogMessageInfo(
         message = "Exception StandardServer.await: create[{0}]",
         level = "SEVERE",
         cause = "An I/O error occurred when opening the socket",
         action = "Verify the port number and try again")
     public static final String STANDARD_SERVER_AWAIT_CREATE_EXCEPTION = prefix + "00244";

     @LogMessageInfo(
         message = "StandardServer.accept security exception: {0}",
         level = "WARNING",
         cause = "Could not get connection",
         action = "Verify the connection settings and try again")
     public static final String STANDARD_SERVER_ACCEPT_SECURITY_EXCEPTION = prefix + "00245";

     @LogMessageInfo(
         message = "StandardServer.await: accept: {0}",
         level = "SEVERE",
         cause = "Could not get input stream",
         action = "Verify the input stream and try again")
     public static final String STANDARD_SERVER_AWAIT_ACCEPT_EXCEPTION = prefix + "00246";

     @LogMessageInfo(
         message = "StandardServer.await: read: {0}",
         level = "WARNING",
         cause = "Could not read from input stream",
         action = "Verify the input stream and try again")
     public static final String STANDARD_SERVER_AWAIT_READ_EXCEPTION = prefix + "00247";

     @LogMessageInfo(
         message = "StandardServer.await: Invalid command {0} received",
         level = "WARNING",
         cause = "Invalid command",
         action = "Verify the command")
     public static final String STANDARD_SERVER_AWAIT_INVALID_COMMAND_RECEIVED_EXCEPTION = prefix + "00248";

     @LogMessageInfo(
         message = "This service has already been initialized",
         level = "INFO")
     public static final String STANDARD_SERVER_INITIALIZE_INITIALIZED = prefix + "00249";

     @LogMessageInfo(
         message = "Error registering: {0}",
         level = "SEVERE",
         cause = "Could not register ObjectName: \"Catalina:type=Server\"",
         action = "Verify the configuration and try again")
     public static final String ERROR_REGISTERING = prefix + "00250";

     @LogMessageInfo(
         message = "This service has already been started",
         level = "INFO"
     )
     public static final String SERVICE_STARTED = prefix + "00251";

     @LogMessageInfo(
         message = "Starting service {0}",
         level = "INFO"
     )
     public static final String STARTING_SERVICE = prefix + "00252";

     @LogMessageInfo(
         message = "Stopping service {0}",
         level = "INFO"
     )
     public static final String STOPPING_SERVICE = prefix + "00253";

     @LogMessageInfo(
         message = "This service has already been initialized",
         level = "INFO"
     )
     public static final String SERVICE_HAS_BEEN_INIT = prefix + "00254";

     @LogMessageInfo(
         message = "Error registering Service at domain {0}",
         level = "SEVERE",
         cause = "Could not register service",
         action = "Verify the domain name and service name"
     )
     public static final String ERROR_REGISTER_SERVICE_EXCEPTION = prefix + "00255";

     @LogMessageInfo(
         message = "Service initializing at {0} failed",
         level = "SEVERE",
         cause = "Could not pre-startup initialization",
         action = "Verify if server was already initialized"
     )
     public static final String FAILED_SERVICE_INIT_EXCEPTION = prefix + "00256";

    @LogMessageInfo(
        message = "Parent container of a Wrapper must be a Context",
        level = "WARNING"
    )
    public static final String PARENT_CONTAINER_MUST_BE_CONTEXT_EXCEPTION = prefix + "00257";

    @LogMessageInfo(
        message = "Wrapper container may not have child containers",
        level = "WARNING"
    )
    public static final String WRAPPER_CONTAINER_NO_CHILD_EXCEPTION = prefix + "00258";

    @LogMessageInfo(
        message = "Cannot allocate servlet {0} because it is being unloaded",
        level = "WARNING"
    )
    public static final String CANNOT_ALLOCATE_SERVLET_EXCEPTION = prefix + "00259";

    @LogMessageInfo(
        message = "Error allocating a servlet instance",
        level = "WARNING"
    )
    public static final String ERROR_ALLOCATE_SERVLET_INSTANCE_EXCEPTION = prefix + "00260";

    @LogMessageInfo(
        message = "Class {0} is not a Servlet",
        level = "WARNING"
    )
    public static final String CLASS_IS_NOT_SERVLET_EXCEPTION = prefix + "00261";

    @LogMessageInfo(
        message = "Error instantiating servlet class {0}",
        level = "WARNING"
    )
    public static final String ERROR_INSTANTIATE_SERVLET_CLASS_EXCEPTION = prefix + "00262";

    @LogMessageInfo(
        message = "Servlet of class {0} is privileged and cannot be loaded by this web application",
        level = "WARNING"
    )
    public static final String PRIVILEGED_SERVLET_CANNOT_BE_LOADED_EXCEPTION = prefix + "00263";

    @LogMessageInfo(
        message = "No servlet class has been specified for servlet {0}",
        level = "WARNING"
    )
    public static final String NO_SERVLET_BE_SPECIFIED_EXCEPTION = prefix + "00264";

    @LogMessageInfo(
        message = "Wrapper cannot find Loader for servlet {0}",
        level = "WARNING"
    )
    public static final String CANNOT_FIND_LOADER_EXCEPTION = prefix + "00265";

    @LogMessageInfo(
        message = "Wrapper cannot find servlet class {0} or a class it depends on",
        level = "WARNING"
    )
    public static final String CANNOT_FIND_SERVLET_CLASS_EXCEPTION = prefix + "00266";

    @LogMessageInfo(
        message = "Servlet.init() for servlet {0} threw exception",
        level = "WARNING"
    )
    public static final String SERVLET_INIT_EXCEPTION = prefix + "00267";

    @LogMessageInfo(
        message = "Servlet execution threw an exception",
        level = "WARNING"
    )
    public static final String SERVLET_EXECUTION_EXCEPTION = prefix + "00268";

    @LogMessageInfo(
        message = "Marking servlet {0} as unavailable",
        level = "FINE"
    )
    public static final String MARK_SERVLET_UNAVAILABLE = prefix + "00269";

    @LogMessageInfo(
        message = "Waiting for {0} instance(s) of {1} to be deallocated",
        level = "INFO"
    )
    public static final String WAITING_INSTANCE_BE_DEALLOCATED = prefix + "00270";

    @LogMessageInfo(
        message = "Servlet.destroy() for servlet {0} threw exception",
        level = "WARNING"
    )
    public static final String DESTROY_SERVLET_EXCEPTION = prefix + "00271";

    @LogMessageInfo(
        message = "Servlet {0} threw unload() exception",
        level = "WARNING"
    )
    public static final String SERVLET_UNLOAD_EXCEPTION = prefix + "00272";

    @LogMessageInfo(
            message = "Error loading {0} {1}",
            level = "INFO"
    )
    public static final String ERROR_LOADING_INFO = prefix + "00273";

    @LogMessageInfo(
        message = "This application is not currently available",
        level = "WARNING"
    )
    public static final String APP_UNAVAILABLE = prefix + "00274";

    @LogMessageInfo(
        message = "Servlet {0} is currently unavailable",
        level = "WARNING"
    )
    public static final String SERVLET_UNAVAILABLE  = prefix + "00275";

    @LogMessageInfo(
        message = "Servlet {0} is not available",
        level = "WARNING"
    )
    public static final String SERVLET_NOT_FOUND = prefix + "00276";

    @LogMessageInfo(
        message = "Allocate exception for servlet {0}",
        level = "WARNING"
    )
    public static final String SERVLET_ALLOCATE_EXCEPTION = prefix + "00277";

    @LogMessageInfo(
        message = "Exception for sending acknowledgment of a request: {0}",
        level = "WARNING"
    )
    public static final String SEND_ACKNOWLEDGEMENT_EXCEPTION = prefix + "00278";

    @LogMessageInfo(
        message = "Release filters exception for servlet {0}",
        level = "WARNING"
    )
    public static final String RELEASE_FILTERS_EXCEPTION = prefix + "00280";

    @LogMessageInfo(
        message = "Deallocate exception for servlet {0}",
        level = "WARNING"
    )
    public static final String DEALLOCATE_EXCEPTION = prefix + "00281";

    @LogMessageInfo(
        message = "StandardWrapperValve[{0}]: {1}",
        level = "INFO"
    )
    public static final String STANDARD_WRAPPER_VALVE = prefix + "00283";

    @LogMessageInfo(
            message = "Failed to skip {0} bytes in the underlying buffer of MultipartStream on close().",
            level = "WANING"
    )
    public static final String FAILED_SKIP_BYTES_MULTIPART_STREAM_CLOSE_EXCEPTION = prefix + "00284";

    @LogMessageInfo(
            message = "file data is empty.",
            level = "INFO"
    )
    public static final String FILE_DATA_IS_EMPTY_INFO = prefix + "00285";

    @LogMessageInfo(
            message = "Unable to create Random source using class [{0}]",
            level = "WARNING"
    )
    public static final String UNABLE_CREATE_RANDOM_SOURCE_EXCEPTION = prefix + "00286";

    @LogMessageInfo(
            message = "The property \"{0}\" is not defined for filters of type \"{1}\"",
            level = "WARNING"
    )
    public static final String PROPERTY_NOT_DEFINED_EXCEPTION = prefix + "00287";

    @LogMessageInfo(
            message = "Error registering loader",
            level = "SEVERE",
            cause = "Could not register loader",
            action = "Verify Object name"
    )
    public static final String REGISTERING_LOADER_EXCEPTION = prefix + "00288";

    @LogMessageInfo(
            message = "Error registering jndi stream handler",
            level = "SEVERE",
            cause = "Could not register jndi stream handler",
            action = "Verify if the application has already set a factory, " +
                     "if a security manager exists and its" +
                     "checkSetFactory method doesn't allow" +
                     "the operation"
    )
    public static final String REGISTERING_JNDI_STREAM_HANDLER_EXCEPTION = prefix + "00289";

    @LogMessageInfo(
            message = "Loader has already been started",
            level = "WARNING"
    )
    public static final String LOADER_ALREADY_STARTED_EXCEPTION = prefix + "00290";

    @LogMessageInfo(
            message = "No resources for {0}",
            level = "INFO"
    )
    public static final String NO_RESOURCE_INFO = prefix + "00291";

    @LogMessageInfo(
            message = "LifecycleException",
            level = "SEVERE",
            cause = "Could not construct a class loader",
            action = "Verify if there is any lifecycle exception"
    )
    public static final String LIFECYCLE_EXCEPTION = prefix + "00292";

    @LogMessageInfo(
            message = "Loader has not yet been started",
            level = "WARNING"
    )
    public static final String LOADER_NOT_STARTED_EXCEPTION = prefix + "00293";

    @LogMessageInfo(
            message = "Cannot set reloadable property to {0}",
            level = "SEVERE",
            cause = "Could not set reloadable property",
            action = "Verify the value for the property"
    )
    public static final String SET_RELOADABLE_PROPERTY_EXCEPTION = prefix + "00294";

    @LogMessageInfo(
            message = "WebappLoader[{0}]: {1}",
            level = "WARNING"
    )
    public static final String WEB_APP_LOADER_EXCEPTION = prefix + "00295";

    @LogMessageInfo(
            message = "No work dir for {0}",
            level = "INFO"
    )
    public static final String NO_WORK_DIR_INFO = prefix + "00296";

    @LogMessageInfo(
            message = "Failed to create destination directory to copy resources",
            level = "WARNING"
    )
    public static final String FAILED_CREATE_DEST_DIR = prefix + "00297";

    @LogMessageInfo(
            message = "Failed to copy resources",
            level = "WARNING"
    )
    public static final String FAILED_COPY_RESOURCE = prefix + "00298";

    @LogMessageInfo(
            message = "Failed to create work directory to {0}",
            level = "SEVERE",
            cause = "Coud not create work directory",
            action = "Verify the PATH "
    )
    public static final String FAILED_CREATE_WORK_DIR_EXCEPTION = prefix + "00299";

    @LogMessageInfo(
            message = "File Logger has already been started",
            level = "WARNING"
    )
    public static final String FILE_LOGGER_STARTED = prefix + "00300";

    @LogMessageInfo(
            message = "File Logger has not yet been started",
            level = "WARNING"
    )
    public static final String FILE_LOGGER_NOT_STARTED = prefix + "00301";

     @LogMessageInfo(
             message = "Unknown container {0}",
             level = "SEVERE",
             cause = "Unknown container for implementation of StandardEngine interface",
             action = "Verify the current container"
     )
     public static final String UNKNOWN_CONTAINER_EXCEPTION = prefix + "00302";

     @LogMessageInfo(
             message = "Null engine !! {0}",
             level = "SEVERE",
             cause = "Could not get engine",
             action = "Verify current container"
     )
     public static final String NULL_ENGINE_EXCEPTION = prefix + "00303";

     @LogMessageInfo(
             message = "Unable to create javax.management.ObjectName for Logger",
             level = "WARNING"
     )
     public static final String UNABLE_CREATE_OBJECT_NAME_FOR_LOGGER_EXCEPTION = prefix + "00304";

     @LogMessageInfo(
             message = "Can't register logger {0}",
             level = "SEVERE",
             cause = "Could not register logger",
             action = "Verify registration is called after configure()"
     )
     public static final String CANNOT_REGISTER_LOGGER_EXCEPTION = prefix + "00305";

     @LogMessageInfo(
             message = "Setting JAAS app name {0}",
             level = "INFO"
     )
     public static final String SETTING_JAAS_INFO = prefix + "00306";

     @LogMessageInfo(
             message = "Login exception authenticating username {0}",
             level = "FINE"
     )
     public static final String LOGIN_EXCEPTION_AUTHENTICATING_USERNAME = prefix + "00307";

     @LogMessageInfo(
             message = "Username {0} NOT authenticated due to failed login",
             level = "FINE"
     )
     public static final String USERNAME_NOT_AUTHENTICATED_FAILED_LOGIN = prefix + "00308";

     @LogMessageInfo(
             message = "Username {0} NOT authenticated due to expired account",
             level = "FINE"
     )
     public static final String USERNAME_NOT_AUTHENTICATED_EXPIRED_ACCOUNT = prefix + "00309";

     @LogMessageInfo(
             message = "Username {0} NOT authenticated due to expired credential",
             level = "FINE"
     )
     public static final String USERNAME_NOT_AUTHENTICATED_EXPIRED_CREDENTIAL = prefix + "00310";

     @LogMessageInfo(
             message = "error ",
             level = "SEVERE",
             cause = "Could not authenticate by using the current username",
             action = "Verify the username and credential"
     )
     public static final String AUTHENTICATION_ERROR = prefix + "00311";

    @LogMessageInfo(
            message = "Illegal digestEncoding: {0}",
            level = "SEVERE",
            cause = "Could not convert the char array to byte array with respect to given charset",
            action = "Verify the current charset"
    )
    public static final String ILLEGAL_DIGEST_ENCODING_EXCEPTION = prefix + "00312";

    @LogMessageInfo(
            message = "Access to the requested resource has been denied",
            level = "WARNING"
    )
    public static final String ACCESS_RESOURCE_DENIED = prefix + "00313";

    @LogMessageInfo(
            message = "Configuration error: Cannot perform access control without an authenticated principal",
            level = "WARNING"
    )
    public static final String CONFIG_ERROR_NOT_AUTHENTICATED = prefix + "00314";

    @LogMessageInfo(
            message = "Username {0} has role {1}",
            level = "FINE"
    )
    public static final String USERNAME_HAS_ROLE = prefix + "00315";

    @LogMessageInfo(
            message = "Username {0} does NOT have role {1}",
            level = "FINE"
    )
    public static final String USERNAME_NOT_HAVE_ROLE = prefix + "00316";

    @LogMessageInfo(
            message = "This Realm has already been started",
            level = "INFO"
    )
    public static final String REALM_BEEN_STARTED = prefix + "00317";

    @LogMessageInfo(
            message = "Invalid message digest algorithm {0} specified",
            level = "WARNING"
    )
    public static final String INVALID_ALGORITHM_EXCEPTION = prefix + "00318";

    @LogMessageInfo(
            message = "This Realm has not yet been started",
            level = "INFO"
    )
    public static final String REALM_NOT_BEEN_STARTED = prefix + "00319";

    @LogMessageInfo(
            message = "Error digesting user credentials",
            level = "SEVERE",
            cause = "Could not digest user credentials",
            action = "Verify the current credential"
    )
    public static final String ERROR_DIGESTING_USER_CREDENTIAL_EXCEPTION = prefix + "00320";

    @LogMessageInfo(
            message = "Couldn't get MD5 digest",
            level = "SEVERE",
            cause = "Could not get instance of MessageDigest based on MD5",
            action = "Verify if it supports a MessageDigestSpi implementation " +
                     "for the specified algorithm"
    )
    public static final String CANNOT_GET_MD5_DIGEST_EXCEPTION = prefix + "00321";

    @LogMessageInfo(
            message = "An exception occurs when running the PrivilegedExceptionAction block.",
            level = "FINE"
    )
    public static final String PRIVILEGE_ACTION_EXCEPTION = prefix + "00322";

    @LogMessageInfo(
            message = "Only skipped [{0}] bytes when [{1}] were requested",
            level = "WARNING"
    )
    public static final String SKIP_BYTES_EXCEPTION = prefix + "00323";

    @LogMessageInfo(
            message = "Directory Listing For {0}",
            level = "INFO"
    )
    public static final String DIR_TITLE_INFO = prefix + "00324";

    @LogMessageInfo(
            message = "Up To {0}",
            level = "INFO"
    )
    public static final String DIR_PARENT_INFO = prefix + "00325";

    @LogMessageInfo(
            message = "Filename",
            level = "INFO"
    )
    public static final String DIR_FILENAME_INFO = prefix + "00326";

    @LogMessageInfo(
            message = "Size",
            level = "INFO"
    )
    public static final String DIR_SIZE_INFO = prefix + "00327";

    @LogMessageInfo(
            message = "Last Modified",
            level = "INFO"
    )
    public static final String DIR_LAST_MODIFIED_INFO = prefix + "00328";

    @LogMessageInfo(
            message = "Container has not called setWrapper() for this servlet",
            level = "WARNING"
    )
    public static final String SET_WRAPPER_NOT_CALLED_EXCEPTION = prefix + "00329";

    @LogMessageInfo(
            message = "Cannot call invoker servlet with a named dispatcher",
            level = "WARNING"
    )
    public static final String CANNOT_CALL_INVOKER_SERVLET = prefix + "00330";

    @LogMessageInfo(
            message = "No servlet name or class was specified in path {0}",
            level = "WARNING"
    )
    public static final String INVALID_PATH_EXCEPTION = prefix + "00331";

    @LogMessageInfo(
            message = "Cannot create servlet wrapper for path {0}",
            level = "WARNING"
    )
    public static final String CANNOT_CREATE_SERVLET_WRAPPER_EXCEPTION = prefix + "00332";

    @LogMessageInfo(
            message = "Cannot allocate servlet instance for path {0}",
            level = "WARNING"
    )
    public static final String CANNOT_ALLOCATE_SERVLET_INSTANCE_EXCEPTION = prefix + "00333";

    @LogMessageInfo(
            message = "Cannot deallocate servlet instance for path {0}",
            level = "WARNING"
    )
    public static final String CANNOT_DEALLOCATE_SERVLET_INSTANCE_EXCEPTION = prefix + "00334";

    @LogMessageInfo(
            message = "JAXP initialization failed",
            level = "WARNING"
    )
    public static final String JAXP_INTI_FAILED = prefix + "00335";

    @LogMessageInfo(
            message = "Ignored external entity, publicID: {0}, systemID: {1}",
            level = "INFO"
    )
    public static final String IGNORED_EXTERNAL_ENTITY_INFO = prefix + "00336";

    @LogMessageInfo(
            message = "setAttribute: Session attribute with name {0} has value that is not of type String (required for cookie-based persistence)",
            level = "WARNING"
    )
    public static final String SET_SESSION_ATTRIBUTE_EXCEPTION = prefix + "00337";

    @LogMessageInfo(
            message = "Loading Session {0} from file {1}",
            level = "FINE"
    )
    public static final String LOADING_SESSION_FROM_FILE = prefix + "00338";

    @LogMessageInfo(
            message = "Removing Session {0} at file {1}",
            level = "FINE"
    )
    public static final String REMOVING_SESSION_FROM_FILE = prefix + "00339";

    @LogMessageInfo(
            message = "Saving Session {0} to file {1}",
            level = "FINE"
    )
    public static final String SAVING_SESSION_TO_FILE = prefix + "00340";

    @LogMessageInfo(
            message = "Unable to delete file [{0}] which is preventing the creation of the session storage location",
            level = "WARNING"
    )
    public static final String UNABLE_DELETE_FILE_EXCEPTION = prefix + "00341";

    @LogMessageInfo(
            message = "Unable to create directory [{0}] for the storage of session data",
            level = "WARNING"
    )
    public static final String UNABLE_CREATE_DIR_EXCEPTION = prefix + "00342";

    @LogMessageInfo(
            message = "SQL Error {0}",
            level = "FINE"
    )
    public static final String SQL_ERROR = prefix + "00343";

    @LogMessageInfo(
            message = "Loading Session {0} from database {1}",
            level = "FINE"
    )
    public static final String LOADING_SESSION_FROM_DATABASE = prefix + "00344";

    @LogMessageInfo(
            message = "Removing Session {0} at database {1}",
            level = "FINE"
    )
    public static final String REMOVING_SESSION_FROM_DATABASE = prefix + "00345";

    @LogMessageInfo(
            message = "Saving Session {0} to database {1}",
            level = "FINE"
    )
    public static final String SAVING_SESSION_TO_DATABASE = prefix + "00346";

    @LogMessageInfo(
            message = "The database connection is null or was found to be closed. Trying to re-open it.",
            level = "FINE"
    )
    public static final String DATABASE_CONNECTION_CLOSED = prefix + "00347";

    @LogMessageInfo(
            message = "The re-open on the database failed. The database could be down.",
            level = "FINE"
    )
    public static final String RE_OPEN_DATABASE_FAILED = prefix + "00348";

    @LogMessageInfo(
            message = "A SQL exception occurred {0}",
            level = "FINE"
    )
    public static final String SQL_EXCEPTION = prefix + "00349";

    @LogMessageInfo(
            message = "JDBC driver class not found {0}",
            level = "FINE"
    )
    public static final String JDBC_DRIVER_CLASS_NOT_FOUND = prefix + "00350";

    @LogMessageInfo(
            message = "Exception initializing random number generator of class {0}",
            level = "SEVERE",
            cause = "Could not construct and seed a new random number generator",
            action = "Verify if the current random number generator class is "
    )
    public static final String INIT_RANDOM_NUMBER_GENERATOR_EXCEPTION = prefix + "00351";

    @LogMessageInfo(
            message = "Seeding random number generator class {0}",
            level = "FINE"
    )
    public static final String SEEDING_RANDOM_NUMBER_GENERATOR_CLASS = prefix + "00352";

    @LogMessageInfo(
            message = "Failed to close randomIS.",
            level = "WARNING"
    )
    public static final String FAILED_CLOSE_RANDOMIS_EXCEPTION = prefix + "00353";

    @LogMessageInfo(
            message = "Error registering ",
            level = "SEVERE",
            cause = "Could not construct an object name",
            action = "Verify the format of domain, path, host. And make sure they are no null"
    )
    public static final String ERROR_REGISTERING_EXCEPTION_SEVERE = prefix + "00354";

    @LogMessageInfo(
            message = "setAttribute: Non-serializable attribute with name {0}",
            level = "WARNING"
    )
    public static final String NON_SERIALIZABLE_ATTRIBUTE_EXCEPTION = prefix + "00355";

    @LogMessageInfo(
            message = "Session not found {0}",
            level = "INFO"
    )
    public static final String SESSION_NOT_FOUND = prefix + "00356";

    @LogMessageInfo(
            message = "Checking isLoaded for id, {0}, {1}",
            level = "SEVERE",
            cause = "Could not find session associated with given ID",
            action = "Verify the session ID"
    )
    public static final String CHECKING_IS_LOADED_EXCEPTION = prefix + "00357";

    @LogMessageInfo(
            message = "Exception clearing the Store",
            level = "SEVERE",
            cause = "Could not instantiate PrivilegedStoreClear()",
            action = "Verify if specified action's run() could remove all sessions from store"
    )
    public static final String CLEARING_STORE_EXCEPTION = prefix + "00358";

    @LogMessageInfo(
            message = "createSession: Too many active sessions",
            level = "WARNING"
    )
    public static final String CREATE_SESSION_EXCEPTION = prefix + "00359";

    @LogMessageInfo(
            message = "Exception in the Store during load",
            level = "SEVERE",
            cause = "Could not instantiate PrivilegedStoreKeys()",
            action = "Verify if specified action's run() does not throw exception"
    )
    public static final String STORE_LOADING_EXCEPTION = prefix + "00360";

    @LogMessageInfo(
            message = "Loading {0} persisted sessions",
            level = "FINE"
    )
    public static final String LOADING_PERSISTED_SESSIONS = prefix + "00361";

    @LogMessageInfo(
            message = "Failed load session from store",
            level = "SEVERE",
            cause = "Could not restore sessions from store to manager's list",
            action = "Verify if the sessions are valid"
    )
    public static final String FAILED_LOAD_SESSION_EXCEPTION = prefix + "00362";

    @LogMessageInfo(
            message = "Can't load sessions from store",
            level = "SEVERE",
            cause = "Could not load sessions from store",
            action = "Verify if there is no exception to get the array containing the session " +
                     "identifiers of all Sessions currently saved in this Store"
    )
    public static final String CANNOT_LOAD_SESSION_EXCEPTION = prefix + "00363";

    @LogMessageInfo(
            message = "Exception in the Store during removeSession",
            level = "SEVERE",
            cause = "Could not instantiate PrivilegedStoreRemove()",
            action = "Verify if the specified action's run() could remove the session with the " +
                     "specified session identifier from this Store"
    )
    public static final String STORE_REMOVE_SESSION_EXCEPTION = prefix + "00364";

    @LogMessageInfo(
            message = "Exception removing session",
            level = "SEVERE",
            cause = "Could not remove specified session identifier from store",
            action = "Verify if there is no I/O error occur"
    )
    public static final String REMOVING_SESSION_EXCEPTION = prefix + "00365";

    @LogMessageInfo(
            message = "Saving {0} persisted sessions",
            level = "FINE"
    )
    public static final String SAVING_PERSISTED_SESSION = prefix + "00366";

    @LogMessageInfo(
            message = "Exception in the Store during swapIn",
            level = "SEVERE",
            cause = "Could not instantiate PrivilegedStoreLoad",
            action = "Verify if action's run() can load and return the Session associated with the specified session " +
                     "identifier from this Store, without removing it"
    )
    public static final String STORE_SWAP_IN_EXCEPTION = prefix + "00367";

    @LogMessageInfo(
            message = "Error deserializing Session {0}: {1}",
            level = "SEVERE",
            cause = "Deserialization error occur, and could not load and return the session " +
                     "associated with the specified session identifier from this Store",
            action = "Verify if ClassNotFoundException occur"
    )
    public static final String DESERILIZING_SESSION_EXCEPTION = prefix + "00368";

    @LogMessageInfo(
            message = "Session swapped in is invalid or expired",
            level = "SEVERE",
            cause = "Session swapped in is invalid or expired",
            action = "Verify if current session is valid"
    )
    public static final String INVALID_EXPIRED_SESSION_EXCEPTION = prefix + "00369";

    @LogMessageInfo(
            message = "Swapping session {0} in from Store",
            level = "FINE"
    )
    public static final String SWAPPING_SESSION_FROM_STORE = prefix + "00370";

    @LogMessageInfo(
            message = "Exception in the Store during writeSession",
            level = "SEVERE",
            cause = "Could not write the provided session to the Store",
            action = "Verify if there are any I/O errors occur"
    )
    public static final String STORE_WRITE_SESSION_EXCEPTION = prefix + "00371";

    @LogMessageInfo(
            message = "Error serializing Session {0}: {1}",
            level = "SEVERE",
            cause = "Could not save the specified Session into this Store",
            action = "Verify if there are any I/O errors occur"
    )
    public static final String SERIALIZING_SESSION_EXCEPTION = prefix + "00372";

    @LogMessageInfo(
            message = "Manager has already been started",
            level = "INFO"
    )
    public static final String MANAGER_STARTED_INFO = prefix + "00373";

    @LogMessageInfo(
            message = "No Store configured, persistence disabled",
            level = "SEVERE",
            cause = "Could not prepare for the beginning of active use of the public methods of this component",
            action = "Verify if Store has been configured"
    )
    public static final String NO_STORE_CONFIG_EXCEPTION = prefix + "00374";

    @LogMessageInfo(
            message = "Manager has not yet been started",
            level = "INFO"
    )
    public static final String  MANAGER_NOT_STARTED_INFO = prefix + "00375";

    @LogMessageInfo(
            message = "Invalid session timeout setting {0}",
            level = "SEVERE",
            cause = "Could not set session timeout from given parameter",
            action = "Verify the number format for session timeout setting"
    )
    public static final String INVALID_SESSION_TIMEOUT_SETTING_EXCEPTION = prefix + "00376";

    @LogMessageInfo(
            message = "Swapping session {0} to Store, idle for {1} seconds",
            level = "FINE"
    )
    public static final String SWAPPING_SESSION_TO_STORE = prefix + "00377";

    @LogMessageInfo(
            message = "Too many active sessions, {0}, looking for idle sessions to swap out",
            level = "FINE"
    )
    public static final String TOO_MANY_ACTIVE_SESSION = prefix + "00378";

    @LogMessageInfo(
            message = "Swapping out session {0}, idle for {1} seconds too many sessions active",
            level = "FINE"
    )
    public static final String SWAP_OUT_SESSION = prefix + "00379";

    @LogMessageInfo(
            message = " Backing up session {0} to Store, idle for {1} seconds",
            level = "FINE"
    )
    public static final String BACKUP_SESSION_TO_STORE = prefix + "00380";

    @LogMessageInfo(
            message = "createSession: Too many active sessions",
            level = "WARNING"
    )
    public static final String TOO_MANY_ACTIVE_SESSION_EXCEPTION = prefix + "00381";

    @LogMessageInfo(
            message = " Loading persisted sessions from {0}",
            level = "FINE"
    )
    public static final String LOADING_PERSISTED_SESSION = prefix + "00382";

    @LogMessageInfo(
            message = "IOException while loading persisted sessions: {0}",
            level = "SEVERE",
            cause = "Could not creates an ObjectInputStream",
            action = "Verify if there are IO exceptions"
    )
    public static final String LOADING_PERSISTED_SESSION_IO_EXCEPTION = prefix + "00383";

    @LogMessageInfo(
            message = "ClassNotFoundException while loading persisted sessions: {0}",
            level = "SEVERE",
            cause = "Could not deserialize and create StandardSession instance ",
            action = "Verify the class for an object being restored can be found"
    )
    public static final String CLASS_NOT_FOUND_EXCEPTION = prefix + "00384";

    @LogMessageInfo(
            message = "Saving persisted sessions to {0}",
            level = "FINE"
    )
    public static final String SAVING_PERSISTED_SESSION_PATH = prefix + "00385";

    @LogMessageInfo(
            message = "IOException while saving persisted sessions: {0}",
            level = "SEVERE",
            cause = "Could not creates an ObjectOutputStream instance",
            action = "Verify if there are any I/O exceptions"
    )
    public static final String SAVING_PERSISTED_SESSION_IO_EXCEPTION = prefix + "00386";

    @LogMessageInfo(
            message = "Exception loading sessions from persistent storage",
            level = "SEVERE",
            cause = "Could not load any currently active sessions",
            action = "Verify if the serialized class is valid and if there are any I/O exceptions"
    )
    public static final String LOADING_SESSIONS_EXCEPTION = prefix + "00387";

    @LogMessageInfo(
            message = "Exception unloading sessions to persistent storage",
            level = "SEVERE",
            cause = "Could not save any currently active sessions",
            action = "Verify if there are any I/O exceptions"
    )
    public static final String UNLOADING_SESSIONS_EXCEPTION = prefix + "00388";

    @LogMessageInfo(
            message = "Session id change event listener threw exception",
            level = "WARNING"
    )
    public static final String SESSION_ID_CHANGE_EVENT_LISTENER_EXCEPTION = prefix + "00389";

    @LogMessageInfo(
            message = "Session event listener threw exception",
            level = "WARNING"
    )
    public static final String SESSION_EVENT_LISTENER_EXCEPTION = prefix + "00390";

    @LogMessageInfo(
            message = "Session already invalidated",
            level = "WARNING"
    )
    public static final String SESSION_INVALIDATED_EXCEPTION = prefix + "00391";

    @LogMessageInfo(
            message = "Session attribute event listener threw exception",
            level = "WARNING"
    )
    public static final String SESSION_ATTRIBUTE_EVENT_LISTENER_EXCEPTION = prefix + "00392";

    @LogMessageInfo(
            message = "setAttribute: name parameter cannot be null",
            level = "WARNING"
    )
    public static final String NAME_PARAMETER_CANNOT_BE_NULL_EXCEPTION = prefix + "00393";

    @LogMessageInfo(
            message = "Session binding event listener threw exception",
            level = "WARNING"
    )
    public static final String SESSION_BINDING_EVENT_LISTENER_EXCEPTION = prefix + "00394";

    @LogMessageInfo(
            message = " Cannot serialize session attribute {0} for session {1}",
            level = "WARNING"
    )
    public static final String CANNOT_SERIALIZE_SESSION_EXCEPTION = prefix + "00395";

    @LogMessageInfo(
            message = "StoreBase has already been started",
            level = "WARNING"
    )
    public static final String STORE_BASE_STARTED_EXCEPTION = prefix + "00396";

    @LogMessageInfo(
            message = "StoreBase has not been started",
            level = "WARNING"
    )
    public static final String STORE_BASE_NOT_STARTED_EXCEPTION = prefix + "00397";

    @LogMessageInfo(
            message = "Class loader creation threw exception",
            level = "SEVERE",
            cause = "Could not create class loader",
            action = "Verify the availability of current repository "
    )
    public static final String CLASS_LOADER_CREATION_EXCEPTION = prefix + "00398";

    @LogMessageInfo(
            message = "Error processing command line arguments",
            level = "WARNING"
    )
    public static final String ERROR_PROCESSING_COMMAND_LINE_EXCEPTION = prefix + "00399";

    @LogMessageInfo(
            message = "Catalina.stop: ",
            level = "SEVERE",
            cause = "Could not stop server",
            action = "Verify if the input file exist or if there are any I/O exceptions, parsing exceptions"
    )
    public static final String CATALINA_STOP_EXCEPTION = prefix + "00400";

    @LogMessageInfo(
            message = "Can't load server.xml from {0}",
            level = "WARNING"
    )
    public static final String CANNOT_LOAD_SERVER_XML_EXCEPTION = prefix + "00401";

    @LogMessageInfo(
            message = "Catalina.start: ",
            level = "WARNING"
    )
    public static final String CATALINA_START_WARNING_EXCEPTION = prefix + "00402";

    @LogMessageInfo(
            message = "Catalina.start: ",
            level = "SEVERE",
            cause = "Could not initialize the server",
            action = "Verify if the server has already been initialized"
    )
    public static final String CATALINA_START_SEVERE_EXCEPTION = prefix + "00403";

    @LogMessageInfo(
            message = "Initialization processed in {0} ms",
            level = "INFO"
    )
    public static final String INIT_PROCESSED_EXCEPTION = prefix + "00404";

    @LogMessageInfo(
            message = "Error loading configuration",
            level = "WARNING"
    )
    public static final String ERROR_LOADING_CONFIGURATION_EXCEPTION = prefix + "00405";

    @LogMessageInfo(
            message = "Server startup in {0} ms",
            level = "WARNING"
    )
    public static final String SERVER_STARTUP_INFO = prefix + "00406";

    @LogMessageInfo(
            message = "Failed to load catalina.properties",
            level = "WARNING"
    )
    public static final String FAILED_LOAD_CATALINA_PROPERTIES_EXCEPTION = prefix + "00407";

    @LogMessageInfo(
            message = "Lifecycle event data object {0} is not a Context",
            level = "WARNING"
    )
    public static final String EVENT_DATA_IS_NOT_CONTEXT_EXCEPTION = prefix + "00408";

    @LogMessageInfo(
            message = "alt-dd file {0} not found",
            level = "WARNING"
    )
    public static final String ALT_DD_FILE_NOT_FOUND_EXCEPTION = prefix + "00409";

    @LogMessageInfo(
            message = "Missing application web.xml, using defaults only {0}",
            level = "FINE"
    )
    public static final String MISSING_APP_WEB_XML_FINE = prefix + "00410";

    @LogMessageInfo(
            message = "Parse error in application web.xml at line {0} and column {1}",
            level = "WARNING"
    )
    public static final String PARSE_ERROR_IN_APP_WEB_XML_EXCEPTION = prefix + "00411";

    @LogMessageInfo(
            message = "Parse error in application web.xml",
            level = "WARNING"
    )
    public static final String PARSE_ERROR_IN_APP_WEB_XML = prefix + "00412";

    @LogMessageInfo(
            message = "Error closing application web.xml",
            level = "SEVERE",
            cause = "Could not close this input stream and releases any system resources " +
                    "associated with the stream.",
            action = "Verify if any I/O errors occur"
    )
    public static final String ERROR_CLOSING_APP_WEB_XML_EXCEPTION = prefix + "00413";

    @LogMessageInfo(
            message = "No Realm has been configured to authenticate against",
            level = "WARNING"
    )
    public static final String NO_REALM_BEEN_CONFIGURED_EXCEPTION = prefix + "00414";

    @LogMessageInfo(
            message = "Cannot configure an authenticator for method {0}",
            level = "WARNING"
    )
    public static final String CANNOT_CONFIG_AUTHENTICATOR_EXCEPTION = prefix + "00415";

    @LogMessageInfo(
            message = "Cannot instantiate an authenticator of class {0}",
            level = "WARNING"
    )
    public static final String CANNOT_INSTANTIATE_AUTHENTICATOR_EXCEPTION = prefix + "00416";

    @LogMessageInfo(
            message = "Configured an authenticator for method {0}",
            level = "FINE"
    )
    public static final String CONFIGURED_AUTHENTICATOR_FINE = prefix + "00417";

    @LogMessageInfo(
            message = "No default web.xml",
            level = "INFO"
    )
    public static final String NO_DEFAULT_WEB_XML_INFO = prefix + "00418";

    @LogMessageInfo(
            message = "Missing default web.xml, using application web.xml only {0} {1}",
            level = "WARNING"
    )
    public static final String MISSING_DEFAULT_WEB_XML_EXCEPTION = prefix + "00419";

    @LogMessageInfo(
            message = "Parse error in default web.xml at line {0} and column {1}",
            level = "SEVERE",
            cause = "Could not parse the content of the specified input source using this Digester",
            action = "Verify the input parameter, if any I/O errors occur"
    )
    public static final String PARSE_ERROR_IN_DEFAULT_WEB_XML_EXCEPTION = prefix + "00420";

    @LogMessageInfo(
            message = "Parse error in default web.xml",
            level = "SEVERE",
            cause = "Could not parse the content of the specified input source using this Digester",
            action = "Verify the input parameter, if any I/O errors occur"
    )
    public static final String PARSE_ERROR_IN_DEFAULT_WEB_XML = prefix + "00421";

    @LogMessageInfo(
            message = "Error closing default web.xml",
            level = "SEVERE",
            cause = "Could not close this input stream and releases any system resources " +
                    "associated with the stream.",
            action = "Verify if any I/O errors occur"
    )
    public static final String ERROR_CLOSING_DEFAULT_WEB_XML_EXCEPTION = prefix + "00422";

    @LogMessageInfo(
            message = "ContextConfig: Initializing",
            level = "FINE"
    )
    public static final String CONTEXT_CONFIG_INIT_FINE = prefix + "00423";

    @LogMessageInfo(
            message = "Exception fixing docBase",
            level = "SEVERE",
            cause = "Could not adjust docBase",
            action = "Verify if any I/O errors occur"
    )
    public static final String FIXING_DOC_BASE_EXCEPTION = prefix + "00424";

    @LogMessageInfo(
            message = "ContextConfig: Processing START",
            level = "FINEST"
    )
    public static final String PROCESSING_START_FINEST = prefix + "00425";

    @LogMessageInfo(
            message = "ContextConfig: Processing STOP",
            level = "FINEST"
    )
    public static final String PROCESSING_STOP_FINEST = prefix + "00426";

    @LogMessageInfo(
            message = "Security role name {0} used in an <auth-constraint> without being defined in a <security-role> in context [{1}]",
            level = "INFO"
    )
    public static final String SECURITY_ROLE_NAME_USED_IN_AUTH_WITHOUT_DEFINITION = prefix + "00427";

    @LogMessageInfo(
            message = "Security role name {0} used in a <run-as> without being defined in a <security-role> in context [{1}]",
            level = "INFO"
    )
    public static final String SECURITY_ROLE_NAME_USED_IN_RUNAS_WITHOUT_DEFINITION = prefix + "00428";

    @LogMessageInfo(
            message = "Security role name {0} used in a <role-link> without being defined in a <security-role> in context [{1}]",
            level = "INFO"
    )
    public static final String SECURITY_ROLE_NAME_USED_IN_LINK_WITHOUT_DEFINITION = prefix + "00429";

    @LogMessageInfo(
            message = "No web.xml, using defaults {0}",
            level = "INFO"
    )
    public static final String NO_WEB_XML_INFO = prefix + "00430";

    @LogMessageInfo(
            message = "No engines have been defined yet",
            level = "WARNING"
    )
    public static final String NO_ENGINES_DEFINED = prefix + "00431";

    @LogMessageInfo(
            message = "Engine.start exception",
            level = "SEVERE",
            cause = "Could not prepare for the beginning of active use of " +
                    "the public methods of this component.",
            action = "Verify if start() be called before any of the public " +
                     "methods of this component are utilized"
    )
    public static final String ENGINE_START_EXCEPTION = prefix + "00432";

    @LogMessageInfo(
            message = "Couldn't load SSL server socket factory.",
            level = "SEVERE",
            cause = "Could not instantiate ServerSocketFactory",
            action = "Verify access permission to this class"
    )
    public static final String COULD_NOT_LOAD_SSL_SERVER_SOCKET_FACTORY_EXCEPTION = prefix + "00433";

    @LogMessageInfo(
            message = "Couldn't create connector.",
            level = "SEVERE",
            cause = "Could not instantiate connector",
            action = "Verify access permission to this class"
    )
    public static final String COULD_NOT_CREATE_CONNECTOR_EXCEPTION = prefix + "00434";

    @LogMessageInfo(
            message = "Connector.stop",
            level = "SEVERE",
            cause = "Could not remove the specified Connector from the set associated from this Service",
            action = "Verify if connector has already been stopped or removed"
    )
    public static final String CONNECTOR_STOP_EXCEPTION = prefix + "00435";

    @LogMessageInfo(
            message = "Engine.stop exception",
            level = "SEVERE",
            cause = "Could not terminate the active use of the public methods of this component",
            action = "Verify if stop() is the last one called on a given instance of this component"
    )
    public static final String  ENGINE_STOP_EXCEPTION = prefix + "00436";

    @LogMessageInfo(
            message = "Specified Authenticator is not a Valve",
            level = "WARNING"
    )
    public static final String AUTH_IS_NOT_VALVE_EXCEPTION = prefix + "00437";

    @LogMessageInfo(
            message = "Embedded service has already been started",
            level = "WARNING"
    )
    public static final String SERVICE_BEEN_STARTED_EXCEPTION = prefix + "00438";

    @LogMessageInfo(
            message = "Embedded service has not yet been started",
            level = "WARNING"
    )
    public static final String SERVICE_NOT_BEEN_STARTED_EXCEPTION = prefix + "00439";

    @LogMessageInfo(
            message = "Lifecycle event data object {0} is not an Engine",
            level = "WARNING"
    )
    public static final String LIFECYCLE_EVENT_DATA_IS_NOT_ENGINE_EXCEPTION = prefix + "00440";

    @LogMessageInfo(
            message = "EngineConfig: {0}",
            level = "WARNING"
    )
    public static final String ENGINE_CONFIG = prefix + "00441";

    @LogMessageInfo(
            message = "EngineConfig: Processing START",
            level = "INFO"
    )
    public static final String ENGINE_CONFIG_PROCESSING_START_INFO = prefix + "00442";

    @LogMessageInfo(
            message = "EngineConfig: Processing STOP",
            level = "INFO"
    )
    public static final String ENGINE_CONFIG_PROCESSING_STOP_INFO = prefix + "00443";

    @LogMessageInfo(
            message = "Application base directory {0} does not exist",
            level = "WARNING"
    )
    public static final String APP_NOT_EXIST_EXCEPTION = prefix + "00444";

    @LogMessageInfo(
            message = "Unable to create the directory [{0}]",
            level = "WARNING"
    )
    public static final String UNABLE_CREATE_DIRECTORY_EXCEPTION = prefix + "00445";

    @LogMessageInfo(
            message = "The archive [{0}] is malformed and will be ignored: an entry contains an illegal path [{1}]",
            level = "WARNING"
    )
    public static final String ARCHIVE_IS_MALFORMED_EXCEPTION = prefix + "00446";

    @LogMessageInfo(
            message = "Failed to set last-modified time of the file {0}",
            level = "WARNING"
    )
    public static final String FAILED_SET_LAST_MODIFIED_TIME_EXCEPTION = prefix + "00447";

    @LogMessageInfo(
            message = "Error copying {0} to {1}",
            level = "SEVERE",
            cause = "Could not copy file",
            action = "Verify if channel is not available for file transfer"
    )
    public static final String ERROR_COPYING_EXCEPTION = prefix + "00448";

    @LogMessageInfo(
            message = "[{0}] could not be completely deleted. The presence of the remaining files may cause problems",
            level = "SEVERE",
            cause = "Could not completely delete specified directory",
            action = "Verify the access permission to specified directory"
    )
    public static final String DELETE_DIR_EXCEPTION = prefix + "00449";

    @LogMessageInfo(
            message = "Lifecycle event data object {0} is not a Host",
            level = "SEVERE",
            cause = "Could not process the START event for an associated Host",
            action = "Verify Lifecycle event data object"
    )
    public static final String LIFECYCLE_OBJECT_NOT_HOST_EXCEPTION = prefix + "00450";

    @LogMessageInfo(
            message = "Deploying configuration descriptor {0}",
            level = "FINE"
    )
    public static final String DEPLOYING_CONFIG_DESCRIPTOR = prefix + "00451";

    @LogMessageInfo(
            message = "Error deploying configuration descriptor {0}",
            level = "SEVERE",
            cause = "Could not deploy configuration descriptor",
            action = "Verify the URL that points to context configuration file and the context path"
    )
    public static final String ERROR_DEPLOYING_CONFIG_DESCRIPTOR_EXCEPTION = prefix + "00452";

    @LogMessageInfo(
            message = "The war name [{0}] is invalid. The archive will be ignored.",
            level = "SEVERE",
            cause = "Could not deploy war file",
            action = "Verify the name war file"
    )
    public static final String INVALID_WAR_NAME_EXCEPTION = prefix + "00453";

    @LogMessageInfo(
            message = "Expanding web application archive {0}",
            level = "FINE"
    )
    public static final String EXPANDING_WEB_APP = prefix + "00454";

    @LogMessageInfo(
            message = "Exception while expanding web application archive {0}",
            level = "WARNING"
    )
    public static final String EXPANDING_WEB_APP_EXCEPTION = prefix + "00455";

    @LogMessageInfo(
            message = "Exception while expanding web application archive {0}",
            level = "SEVERE",
            cause = "Could not expand web application archive",
            action = "Verify the URL, and if any I/O errors orrur"
    )
    public static final String EXPANDING_WEB_APP_ARCHIVE_EXCEPTION = prefix + "00456";

    @LogMessageInfo(
            message = "Deploying web application archive {0}",
            level = "INFO"
    )
    public static final String DEPLOYING_WEB_APP_ARCHIVE = prefix + "00457";

    @LogMessageInfo(
            message = "Error deploying web application archive {0}",
            level = "SEVERE",
            cause = "Could not deploy web application archive",
            action = "Verify the context path and if specified context path " +
                     "is already attached to an existing web application"
    )
    public static final String ERROR_DEPLOYING_WEB_APP_ARCHIVE_EXCEPTION = prefix + "00458";

    @LogMessageInfo(
            message = "Deploying web application directory {0}",
            level = "FINE"
    )
    public static final String DEPLOYING_WEB_APP_DIR = prefix + "00459";

    @LogMessageInfo(
            message = "Error deploying web application directory {0}",
            level = "SEVERE",
            cause = "Could not deploy web application directory",
            action = "Verify the context path and if specified context path " +
                     "is already attached to an existing web application"
    )
    public static final String ERROR_DEPLOYING_WEB_APP_DIR = prefix + "00460";

    @LogMessageInfo(
            message = "Error undeploying Jar file {0}",
            level = "SEVERE",
            cause = "Could not remove an existing web application, attached to the specified context path",
            action = "Verify the context path of the application"
    )
    public static final String ERROR_UNDEPLOYING_JAR_FILE_EXCEPTION = prefix + "00461";

    @LogMessageInfo(
            message = "HostConfig: restartContext [{0}]",
            level = "INFO"
    )
    public static final String RESTART_CONTEXT_INFO = prefix + "00462";

    @LogMessageInfo(
            message = "Error during context [{0}] stop",
            level = "WARNING"
    )
    public static final String ERROR_DURING_CONTEXT_STOP_EXCEPTION = prefix + "00463";

    @LogMessageInfo(
            message = "Error during context [{0}] restart",
            level = "WARNING"
    )
    public static final String ERROR_DURING_CONTEXT_RESTART_EXCEPTION = prefix + "00464";

    @LogMessageInfo(
            message = "HostConfig: Processing START",
            level = "FINE"
    )
    public static final String PROCESSING_START = prefix + "00465";

    @LogMessageInfo(
            message = "HostConfig: Processing STOP",
            level = "FINE"
    )
    public static final String PROCESSING_STOP = prefix + "00466";

    @LogMessageInfo(
            message = "Undeploying deployed web applications",
            level = "FINE"
    )
    public static final String UNDEPLOYING_WEB_APP = prefix + "00467";

    @LogMessageInfo(
            message = "Undeploying context [{0}]",
            level = "FINE"
    )
    public static final String UNDEPLOYING_CONTEXT = prefix + "00468";

    @LogMessageInfo(
            message = "Error undeploying web application at context path {0}",
            level = "SEVERE",
            cause = "Could not remove an existing web application, attached to the specified context path",
            action = "Verify the context path of the application"
    )
    public static final String ERROR_UNDEPLOYING_WEB_APP_EXCEPTION = prefix + "00469";

    @LogMessageInfo(
            message = "Must set 'catalina.home' system property",
            level = "SEVERE",
            cause = "Did not set 'catalina.home'",
            action = "Verify that 'catalina.home' was passed"
    )
    public static final String MUST_SET_SYS_PROPERTY = prefix + "00470";

    @LogMessageInfo(
            message = "Exception creating instance of {0}",
            level = "SEVERE",
            cause = "Could not load application class",
            action = "Verify the class name"
    )
    public static final String CREATING_INSTANCE_EXCEPTION = prefix + "00472";

    @LogMessageInfo(
            message = "Exception locating main() method",
            level = "SEVERE",
            cause = "Could not locate the static main() method of the application class",
            action = "Verify the access permission"
    )
    public static final String LOCATING_MAIN_METHOD_EXCEPTION = prefix + "00473";

    @LogMessageInfo(
            message = "Exception calling main() method",
            level = "SEVERE",
            cause = "Could not invoke main() method",
            action = "Verify the underlying method is inaccessible, and parameter values"
    )
    public static final String CALLING_MAIN_METHOD_EXCEPTION = prefix + "00474";

    @LogMessageInfo(
            message = "Usage:  java org.apache.catalina.startup.Tool [<options>] <class> [<arguments>]",
            level = "INFO"
    )
    public static final String USAGE_INFO = prefix + "00475";

    @LogMessageInfo(
            message = "Deploying user web applications",
            level = "INFO"
    )
    public static final String DEPLOYING_USER_WEB_APP_INFO = prefix + "00476";

    @LogMessageInfo(
            message = "Exception loading user database",
            level = "WARNING"
    )
    public static final String LOADING_USER_DATABASE_EXCEPTION = prefix + "00477";

    @LogMessageInfo(
            message = "Deploying web application for user {0}",
            level = "INFO"
    )
    public static final String DEPLOYING_WEB_APP_FOR_USER_INFO = prefix + "00478";

    @LogMessageInfo(
            message = "Error deploying web application for user {0}",
            level = "WARNING"
    )
    public static final String DEPLOYING_WEB_APP_FOR_USER_EXCEPTION = prefix + "00479";

    @LogMessageInfo(
            message = "UserConfig[{0}]: {1}",
            level = "INFO"
    )
    public static final String USER_CONFIG = prefix + "00480";

    @LogMessageInfo(
            message = "UserConfig[null]: {0}",
            level = "INFO"
    )
    public static final String USER_CONFIG_NULL = prefix + "00481";

    @LogMessageInfo(
            message = "UserConfig: Processing START",
            level = "INFO"
    )
    public static final String PROCESSING_START_INFO = prefix + "00482";

    @LogMessageInfo(
            message = "UserConfig: Processing STOP",
            level = "INFO"
    )
    public static final String PROCESSING_STOP_INFO = prefix + "00483";

    @LogMessageInfo(
            message = "Failed to load manifest resources {0}",
            level = "SEVERE",
            cause = "Could not find MANIFEST from JAR file",
            action = "Verify the JAR file"
    )
    public static final String FAILED_LOAD_MANIFEST_RESOURCES_EXCEPTION = prefix + "00484";

    @LogMessageInfo(
            message = "ExtensionValidator[{0}][{1}]: Required extension \"{2}\" not found.",
            level = "INFO"
    )
    public static final String EXTENSION_NOT_FOUND_INFO = prefix + "00485";

    @LogMessageInfo(
            message = "ExtensionValidator[{0}]: Failure to find {1} required extension(s).",
            level = "INFO"
    )
    public static final String FAILED_FIND_EXTENSION_INFO = prefix + "00486";

    @LogMessageInfo(
            message = "Odd number of hexadecimal digits",
            level = "WARNING"
    )
    public static final String ODD_NUMBER_HEX_DIGITS_EXCEPTION = prefix + "00487";

    @LogMessageInfo(
            message = "Bad hexadecimal digit",
            level = "WARNING"
    )
    public static final String BAD_HEX_DIGIT_EXCEPTION = prefix + "00488";

    @LogMessageInfo(
            message = "Map is currently locked",
            level = "WARNING"
    )
    public static final String MAP_IS_LOCKED_EXCEPTION = prefix + "00489";

    @LogMessageInfo(
            message = "UTF8 not supported",
            level = "WARNING"
    )
    public static final String UTF8_NOT_SUPPORTED_EXCEPTION = prefix + "00490";

    @LogMessageInfo(
            message = "Could not create a new directory: {0}",
            level = "SEVERE",
            cause = "Could not create a new directory",
            action = "Verify if file is directory, and access permission"
    )
    public static final String CREATING_DIR_EXCEPTION = prefix + "00491";

    @LogMessageInfo(
            message = "status.setContentType",
            level = "WARNING"
    )
    public static final String SET_CONTENT_TYPE_EXCEPTION = prefix + "00492";

    @LogMessageInfo(
            message = "Internal Error",
            level = "SEVERE",
            cause = "Error during invoke the servlet application",
            action = "Trying to invoke the servlet application"
    )
    public static final String INTERNAL_ERROR = prefix + "00493";

    @LogMessageInfo(
            message = "Failed to initialize the interceptor",
            level = "SEVERE",
            cause = "Error in initializing the servlet application",
            action = "initialize the servlet interceptor"
    )
    public static final String FAILED_TO_INITIALIZE_THE_INTERCEPTOR = prefix + "00494";

    @LogMessageInfo(
            message = "Failed to rename log file to {0} for rotate logs",
            level = "SEVERE",
            cause = "Could not rename log file",
            action = "Verify access permission and new file name"
    )
    public static final String FAILED_RENAME_LOG_FILE = prefix + "00503";

    @LogMessageInfo(
            message = "at least this wasn't swallowed",
            level = "INFO"
    )
    public static final String NOT_SWALLOWED_INFO = prefix + "00504";

    @LogMessageInfo(
            message = "Failed to create directory {0}",
            level = "SEVERE",
            cause = "Could not create directory",
            action = "Verify access permission"
    )
    public static final String FAILED_CREATE_DIR = prefix + "00505";

    @LogMessageInfo(
            message = "fields was just empty or whitespace",
            level = "INFO"
    )
    public static final String FIELD_EMPTY_INFO = prefix + "00506";

    @LogMessageInfo(
            message = "unable to decode with rest of chars being: {0}",
            level = "SEVERE",
            cause = "Could not decode rest of chars",
            action = "Verify the current pattern"
    )
    public static final String UNABLE_DECODE_REST_CHARS = prefix + "00507";

    @LogMessageInfo(
            message = "No closing ) found for in decode",
            level = "SEVERE",
            cause = "could not find closing bracket",
            action = "Verify if the parameter includes closing bracket"
    )
    public static final String NO_CLOSING_BRACKET_FOUND = prefix + "00508";

    @LogMessageInfo(
            message = "The next characters couldn't be decoded: {0}",
            level = "SEVERE",
            cause = "Could not decode characters",
            action = "Verify the pattern"
    )
    public static final String CHARACTER_CANNOT_DECODED = prefix + "00509";

    @LogMessageInfo(
            message = "End of line reached before decoding x- param",
            level = "SEVERE",
            cause = "Could not decode, since end of line reached",
            action = "Verify the String index"
    )
    public static final String END_LINE_REACHED = prefix + "00510";

    @LogMessageInfo(
            message = "x param in wrong format. Needs to be 'x-#(...)' read the docs!",
            level = "SEVERE",
            cause = "Could not decode, since x param in wrong format",
            action = "Verify the format of parameter"
    )
    public static final String WRONG_X_PARAM_FORMAT = prefix + "00511";

    @LogMessageInfo(
            message = "x param in wrong format. No closing ')'!",
            level = "SEVERE",
            cause = "Could not decode, since x param has no closing bracket",
            action = "Verify the format of parameter"
    )
    public static final String X_PARAM_NO_CLOSING_BRACKET = prefix + "00512";

    @LogMessageInfo(
            message = "x param for servlet request, couldn't decode value: {0}",
            level = "SEVERE",
            cause = "Could not decode value, since no x param type matched",
            action = "Verify the current fieldInfo"
    )
    public static final String X_PARAM_CANNOT_DECODE_VALUE = prefix + "00513";

    @LogMessageInfo(
            message = "No Context configured to process this request",
            level = "WARNING"
    )
    public static final String NO_CONTEXT_CONFIGURED = prefix + "00514";

    @LogMessageInfo(
            message = "Syntax error in request filter pattern {0}",
            level = "WARNING"
    )
    public static final String SYNTAX_ERROR = prefix + "00515";

    @LogMessageInfo(
            message = "Cannot process the error page: {0}",
            level = "INFO"
    )
    public static final String CANNOT_PROCESS_ERROR_PAGE_INFO = prefix + "00516";

    @LogMessageInfo(
            message = "Digester.getParser: ",
            level = "SEVERE",
            cause = "Could not create new SAXParser",
            action = "Verify the parser configuration and if SAXParser is supported"
    )
    public static final String GET_PARRSER_EXCEPTION = prefix + "00517";

    @LogMessageInfo(
            message = "Cannot get XMLReader",
            level = "SEVERE",
            cause = "Could not get XML Reader",
            action = "Verify if there are XML Readers can be instantiated"
    )
    public static final String CANNOT_GET_XML_READER_EXCEPTION = prefix + "00518";

    @LogMessageInfo(
            message = "Finish event threw exception",
            level = "SEVERE",
            cause = "Rules could not remove data",
            action = "Verify if finish() is called after all parsing methods have been called"
    )
    public static final String FINISH_EVENT_EXCEPTION = prefix + "00519";

    @LogMessageInfo(
            message = "Finish event threw error",
            level = "SEVERE",
            cause = "Rules could not remove data",
            action = "Verify if finish() is called after all parsing methods have been called"
    )
    public static final String FINISH_EVENT_ERROR = prefix + "00520";

    @LogMessageInfo(
            message = "Body event threw exception",
            level = "SEVERE",
            cause = "Could not fire body()",
            action = "Verify if the current rule has body"
    )
    public static final String BODY_EVENT_EXCEPTION = prefix + "00521";

    @LogMessageInfo(
            message = "Body event threw error",
            level = "SEVERE",
            cause = "Could not fire body()",
            action = "Verify if the current rule has body"
    )
    public static final String BODY_EVENT_ERROR = prefix + "00522";

    @LogMessageInfo(
            message = "No rules found matching {0}.",
            level = "WARNING"
    )
    public static final String NO_RULES_FOUND_MATCHING_EXCEPTION = prefix + "00523";

    @LogMessageInfo(
            message = "End event threw exception",
            level = "SEVERE",
            cause = "Could not call end()",
            action = "Verify if this method is called when the end of a matching XML element " +
                     "is encountered"
    )
    public static final String END_EVENT_EXCEPTION = prefix + "00524";

    @LogMessageInfo(
            message = "End event threw error",
            level = "SEVERE",
            cause = "Could not call end()",
            action = "Verify if this method is called when the end of a matching XML element " +
                    "is encountered"
    )
    public static final String END_EVENT_ERROR = prefix + "00525";

    @LogMessageInfo(
            message = "Begin event threw exception",
            level = "SEVERE",
            cause = "Could not call begin()",
            action = "Verify if this method is called when the beginning of a matching XML element " +
                    "is encountered"
    )
    public static final String BEGIN_EVENT_EXCEPTION = prefix + "00526";

    @LogMessageInfo(
            message = "Begin event threw error",
            level = "SEVERE",
            cause = "Could not call begin()",
            action = "Verify if this method is called when the beginning of a matching XML element " +
                    "is encountered"
    )
    public static final String BEGIN_EVENT_ERROR = prefix + "00527";

    @LogMessageInfo(
            message = "Parse Error at line {0} column {1}: {2}",
            level = "SEVERE",
            cause = "Parsing error occurs",
            action = "Verify if there are any parsing errors occur"
    )
    public static final String PARSE_ERROR = prefix + "00528";

    @LogMessageInfo(
            message = "Parse Fatal Error at line {0} column {1}: {2}",
            level = "SEVERE",
            cause = "Parsing error occurs",
            action = "Verify if there are any parsing errors occur"
    )
    public static final String PARSE_FATAL_ERROR = prefix + "00529";

    @LogMessageInfo(
            message = "Parse Warning Error at line {0} column {1}: {2}",
            level = "SEVERE",
            cause = "Parsing error occurs",
            action = "Verify if there are any parsing errors occur"
    )
    public static final String PARSE_WARNING_ERROR = prefix + "00530";

    @LogMessageInfo(
            message = "Empty stack (returning null)",
            level = "WARNING"
    )
    public static final String EMPTY_STACK_EXCEPTION = prefix + "00531";

    @LogMessageInfo(
            message = "No Locator!",
            level = "SEVERE",
            cause = "There is no document locator",
            action = "Verify if document locator has been set"
    )
    public static final String NO_LOCATOR_EXCEPTION = prefix + "00532";

    @LogMessageInfo(
            message = "[SetPropertiesRule]{0} Setting property {1} to {2} did not find a matching property.",
            level = "WARNING"
    )
    public static final String PROPERTIES_RULE_NOT_FIND_MATCHING_PROPERTY = prefix + "00533";

    @LogMessageInfo(
            message = "[SetPropertyRule]{0} Setting property {1} to {2} did not find a matching property.",
            level = "WARNING"
    )
    public static final String PROPERTY_RULE_NOT_FIND_MATCHING_PROPERTY = prefix + "00534";

    @LogMessageInfo(
            message = "Login failed",
            level = "WARNING"
    )

    public static final String LOGIN_FAIL = prefix + "00535";

    @LogMessageInfo(
            message = "This is request has already been authenticated",
            level = "WARNING"
    )
    public static final String ALREADY_AUTHENTICATED = prefix + "00536";

    @LogMessageInfo(
            message = "No authenticator",
            level = "WARNING"
    )
    public static final String NO_AUTHENTICATOR = prefix + "00537";

    @LogMessageInfo(
            message = "Invalid call to login while pluggable authentication method is configured",
            level = "WARNING"
    )
    public static final String LOGIN_WITH_AUTH_CONFIG = prefix + "00538";

    @LogMessageInfo(
            message = "Internal logout error",
            level = "WARNING")
    public static final String INTERNAL_LOGOUT_ERROR = prefix + "00539";

    @LogMessageInfo(
            message = "Blocked access to external entity with publicId [{0}] and systemId [{0}]",
            level = "WARNING"
    )
    public static final String BLOCK_EXTERNAL_ENTITY = prefix + "00540";

    @LogMessageInfo(
            message = "Blocked access to external entity with name [{0}], publicId [{1}], baseURI [{2}] and systemId [{3}]",
            level = "WARNING"
    )
    public static final String BLOCK_EXTERNAL_ENTITY2 = prefix + "00541";

    @LogMessageInfo(
            message = "Blocked access to external subset with name [{0}] and baseURI [{1}]",
            level = "WARNING"
    )
    public static final String BLOCK_EXTERNAL_SUBSET = prefix + "00542";

    @LogMessageInfo(
            message = "Fail to read file [{0}]",
            level = "WARNING"
    )
    public static final String READ_FILE_EXCEPTION = prefix + "00543";

}
