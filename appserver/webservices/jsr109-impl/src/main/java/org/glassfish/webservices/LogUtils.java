/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.webservices;

import java.util.logging.Logger;
import org.glassfish.logging.annotation.LogMessageInfo;
import org.glassfish.logging.annotation.LogMessagesResourceBundle;
import org.glassfish.logging.annotation.LoggerInfo;

/**
 *
 * @author Lukas Jungmann
 */
public final class LogUtils {

    private static final String LOGMSG_PREFIX = "AS-WSJSR109IMPL";

    @LogMessagesResourceBundle
    public static final String LOG_MESSAGES = "org.glassfish.webservices.LogMessages";

    @LoggerInfo(subsystem = "WEBSERVICES", description = "JSR-109 Implementation Logger", publish = true)
    public static final String LOG_DOMAIN = "javax.enterprise.webservices";

    private static final Logger LOGGER = Logger.getLogger(LOG_DOMAIN, LOG_MESSAGES);

    public static Logger getLogger() {
        return LOGGER;
    }
    
    @LogMessageInfo(
            message = "Failed to load deployment descriptor, aborting.",
            level = "SEVERE",
            cause = "unknown",
            action = "unknown")
    public static final String FAILED_LOADING_DD = LOGMSG_PREFIX + "-00001";

    @LogMessageInfo(
            message = "WebService wsdl file {0} not found in archive {1}.",
            comment = "{0} - file URI, {1} - archive name",
            level = "SEVERE",
            cause = "unknown",
            action = "unknown")
    public static final String WSDL_NOT_FOUND = LOGMSG_PREFIX + "-00002";

    @LogMessageInfo(
            message = "Exception while processing catalog {0} Reason : {1}.",
            comment = "{0} - absolute path to the catalog, {1} - message from exception",
            level = "INFO")
    public static final String CATALOG_ERROR = LOGMSG_PREFIX + "-00003";

    @LogMessageInfo(
            message = "Unable to create new File {0}.",
            comment = "{0} - file name",
            level = "INFO")
    public static final String FILECREATION_ERROR = LOGMSG_PREFIX + "-00004";

    @LogMessageInfo(
            message = "WebService {0} has a JAXWS and a JAXRPC endpoint; this is not supported now.",
            comment = "{0} - web service FQN class name",
            level = "SEVERE",
            cause = "unknown",
            action = "unknown")
    public static final String JAXWS_JAXRPC_ERROR = LOGMSG_PREFIX + "-00006";

    @LogMessageInfo(
            message = "Runtime settings error. Cannot find servlet-impl-class for endpoint {0}.",
            comment = "{0} - web service endpoint name",
            level = "INFO")
    public static final String DEPLOYMENT_BACKEND_CANNOT_FIND_SERVLET = LOGMSG_PREFIX + "-00011";

    @LogMessageInfo(
            message = "Cannot proceed with JaxrpcCodegen.",
            level = "INFO")
    public static final String JAXRPC_CODEGEN_FAIL = LOGMSG_PREFIX + "-00012";

    @LogMessageInfo(
            message = "Parsing error line {0}, uri {1}.",
            comment = "{0} - number, {1} - URI location",
            level = "SEVERE",
            cause = "unknown",
            action = "unknown")
    public static final String PARSING_ERROR = LOGMSG_PREFIX + "-00013";

    @LogMessageInfo(
            message = "Error parsing WSDL {0}.",
            comment = "{0} - message from exception",
            level = "SEVERE",
            cause = "unknown",
            action = "unknown")
    public static final String WSDL_PARSING_ERROR = LOGMSG_PREFIX + "-00014";

    @LogMessageInfo(
            message = "Webservice Endpoint deployed {0}\\n listening at address at {1}.",
            comment = "{0} -web service endpoint name, {1} - web service endpoint URL",
            level = "INFO")
    public static final String ENDPOINT_REGISTRATION = LOGMSG_PREFIX + "-00018";

    @LogMessageInfo(
            message = "EJB Endpoint deployed {0}\\n  listening at address at {1}",
            comment = "{0} -web service endpoint name, {1} - web service endpoint URL",
            level = "INFO")
    public static final String EJB_ENDPOINT_REGISTRATION = LOGMSG_PREFIX + "-00019";

    @LogMessageInfo(
            message = "File {0} not found.",
            comment = "{0} - file name",
            level = "INFO")
    public static final String CATALOG_RESOLVER_ERROR = LOGMSG_PREFIX + "-00020";

    @LogMessageInfo(
            message = "MTOM is valid only for SOAP Bindings; Ignoring Enable-MTOM for port {0}.",
            comment = "{0} - web service port name",
            level = "SEVERE",
            cause = "unknown",
            action = "unknown")
    public static final String INVALID_MTOM = LOGMSG_PREFIX + "-00021";

    @LogMessageInfo(
            message = "Implicit mapping not supported; ignoring for now; Remove *. specified in the url-pattern.",
            level = "SEVERE",
            cause = "unknown",
            action = "unknown")
    public static final String ENTERPRISE_WEBSERVICE_IMPLICIT_MAPPING_NOT_SUPPORTED = LOGMSG_PREFIX + "-00033";

    @LogMessageInfo(
            message = "Two web services are being deployed with the same endpoint URL {0}; The service that gets loaded last will always be the one that is active for this URL.",
            comment = "{0} - URL",
            level = "SEVERE",
            cause = "unknown",
            action = "unknown")
    public static final String ENTERPRISE_WEBSERVICE_DUPLICATE_SERVICE = LOGMSG_PREFIX + "-00034";

    @LogMessageInfo(
            message = "Exception while tracing request: {0}.",
            comment = "{0} - message from exception",
            level = "WARNING")
    public static final String EXCEPTION_TRACING_REQUEST = LOGMSG_PREFIX + "-00043";

    @LogMessageInfo(
            message = "Exception while tracing response: {0}.",
            comment = "{0} - message from exception",
            level = "WARNING")
    public static final String EXCEPTION_TRACING_RESPONSE = LOGMSG_PREFIX + "-00044";

    @LogMessageInfo(
            message = "JAXWS WebServiceDispatcher {0} entering for {1} and query string {2}.",
            level = "FINE")
    public static final String WEBSERVICE_DISPATCHER_INFO = LOGMSG_PREFIX + "-00047";

    @LogMessageInfo(
            message = "Ejb endpoint exception.",
            level = "WARNING")
    public static final String EJB_ENDPOINT_EXCEPTION = LOGMSG_PREFIX + "-00048";

    @LogMessageInfo(
            message = "Unable to find adapter for endpoint {0}.",
            comment = "{0} - endpoint name",
            level = "SEVERE",
            cause = "unknown",
            action = "unknown")
    public static final String UNABLE_FIND_ADAPTER = LOGMSG_PREFIX + "-00049";

    @LogMessageInfo(
            message = "Following exception was thrown:",
            level = "WARNING")
    public static final String EXCEPTION_THROWN = LOGMSG_PREFIX + "-00050";

    @LogMessageInfo(
            message = "Client not authorized for invocation of {0}.",
            comment = "{0} - method name",
            level = "INFO")
    public static final String CLIENT_UNAUTHORIZED = LOGMSG_PREFIX + "-00051";

    @LogMessageInfo(
            message = "The following error was thrown by ServletPreHandler which is the first handler in the handler chain {0}.",
            comment = "{0} - message from exception",
            level = "WARNING")
    public static final String PRE_WEBHANDLER_ERROR = LOGMSG_PREFIX + "-00052";

    @LogMessageInfo(
            message = "The following error was thrown by ServletPostHandler which is the last handler in the handler chain {0}.",
            comment = "{0} - message from exception",
            level = "WARNING")
    public static final String POST_WEBHANDLER_ERROR = LOGMSG_PREFIX + "-00053";

    @LogMessageInfo(
            message = "Error registering endpoint {0}.",
            comment = "{0} - message from exception",
            level = "SEVERE",
            cause = "unknown",
            action = "unknown")
    public static final String ENDPOINT_REGISTRATION_ERROR = LOGMSG_PREFIX + "-00054";

    @LogMessageInfo(
            message = "Error unregistering endpoint {0}.",
            comment = "{0} - message from exception",
            level = "SEVERE",
            cause = "unknown",
            action = "unknown")
    public static final String ENDPOINT_UNREGISTRATION_ERROR = LOGMSG_PREFIX + "-00055";

    @LogMessageInfo(
            message = "Deployment cannot proceed as the ejb has a null endpoint address uri. Potential cause may be webservice endpoints not supported in embedded ejb case.",
            level = "INFO")
    public static final String EJB_ENDPOINTURI_ERROR = LOGMSG_PREFIX + "-00056";

    @LogMessageInfo(
            message = "WebService {0} type is declared as {1} but should be either as a JAX-WS or JAX-RPC.",
            comment = "{0} - web service name, {1} - type",
            level = "SEVERE",
            cause = "unknown",
            action = "unknown")
    public static final String WS_TYPE_ERROR = LOGMSG_PREFIX + "-00057";

    @LogMessageInfo(
            message = "Unsupported method request = [{0}] for endpoint {1} at {2}.",
            comment = "{0} - method name, {1} - endpoint name, {2} - URL",
            level = "WARNING")
    public static final String UNSUPPORTED_METHOD_REQUEST = LOGMSG_PREFIX + "-00070";

    @LogMessageInfo(
            message = "invocation error on ejb endpoint {0} at {1} : {2}.",
            comment = "{0} - endpoint name, {1} - URL, {2} - message from exception",
            level = "WARNING")
    public static final String ERROR_ON_EJB = LOGMSG_PREFIX + "-00071";

    @LogMessageInfo(
            message = "Cannot initialize endpoint {0} : error is :",
            comment = "{0} - endpoint name",
            level = "SEVERE",
            cause = "unknown",
            action = "unknown")
    public static final String CANNOT_INITIALIZE = LOGMSG_PREFIX + "-00072";

    @LogMessageInfo(
            message = "Error In EjbRuntimeEndpointInfo",
            level = "FINE")
    public static final String ERROR_EREI = LOGMSG_PREFIX + "-00073";

    @LogMessageInfo(
            message = "Missing internal monitoring info to trace {0}.",
            level = "FINE")
    public static final String MISSING_MONITORING_INFO = LOGMSG_PREFIX + "-00074";

    @LogMessageInfo(
            message = "null message POSTed to ejb endpoint {0} at {1}.",
            level = "FINE")
    public static final String NULL_MESSAGE = LOGMSG_PREFIX + "-00075";

    @LogMessageInfo(
            message = "Invalid request scheme for Endpoint {0}. Expected '{1}', received '{2}'.",
            comment = "{0} - endpoint name, {1} - URL scheme, {2} - URL scheme",
            level = "WARNING")
    public static final String INVALID_REQUEST_SCHEME = LOGMSG_PREFIX + "-00076";

    @LogMessageInfo(
            message = "authentication failed for {0}",
            comment = "{0} - endpoint name",
            level = "WARNING")
    public static final String AUTH_FAILED = LOGMSG_PREFIX + "-00077";

    @LogMessageInfo(
            message = "Servlet web service endpoint '{0}' failure",
            comment = "{0} - endpoint name",
            level = "WARNING")
    public static final String SERVLET_ENDPOINT_FAILURE = LOGMSG_PREFIX + "-00078";

    @LogMessageInfo(
            message = "Error occured",
            level = "SEVERE",
            cause = "unknown",
            action = "unknown")
    public static final String ERROR_OCCURED = LOGMSG_PREFIX + "-00079";

    @LogMessageInfo(
            message = "Error invoking servlet impl",
            level = "INFO")
    public static final String ERROR_INVOKING_SERVLETIMPL = LOGMSG_PREFIX + "-00080";

    @LogMessageInfo(
            message = "Servlet web service endpoint '{0}' HTTP GET error",
            comment = "{0} - endpoint name",
            level = "WARNING")
    public static final String SERVLET_ENDPOINT_GET_ERROR = LOGMSG_PREFIX + "-00081";

    @LogMessageInfo(
            message = "Deployment failed",
            level = "WARNING")
    public static final String DEPLOYMENT_FAILED = LOGMSG_PREFIX + "-00082";

    @LogMessageInfo(
            message = "Cannot load the wsdl from the aplication: {0}",
            comment = "{0} - message from exception",
            level = "SEVERE",
            cause = "unknown",
            action = "unknown")
    public static final String CANNOT_LOAD_WSDL_FROM_APPLICATION = LOGMSG_PREFIX + "-00083";

    @LogMessageInfo(
            message = "Creating endpoint with packaged WSDL {0}",
            comment = "{0} - path to WSDL",
            level = "FINE")
    public static final String CREATING_ENDPOINT_FROM_PACKAGED_WSDL = LOGMSG_PREFIX + "-00084";

    @LogMessageInfo(
            message = "Metadata documents:",
            level = "FINE")
    public static final String METADATA_DOCS = LOGMSG_PREFIX + "-00085";

    @LogMessageInfo(
            message = "For endpoint {0}, Ignoring configuration {1} in weblogic-webservices.xml",
            comment = "{0} - endpoint name, {1} - element name",
            level = "INFO")
    public static final String CONFIGURATION_IGNORE_IN_WLSWS = LOGMSG_PREFIX + "-00086";

    @LogMessageInfo(
            message = "For endpoint {0}, Unsupported configuration {1} in weblogic-webservices.xml",
            comment = "{0} - endpoint name, {1} - element name",
            level = "WARNING")
    public static final String CONFIGURATION_UNSUPPORTED_IN_WLSWS = LOGMSG_PREFIX + "-00087";

    @LogMessageInfo(
            message = "Unexpected error in EJB WebService endpoint post processing",
            level = "WARNING")
    public static final String EJB_POSTPROCESSING_ERROR = LOGMSG_PREFIX + "-00088";

    @LogMessageInfo(
            message = "Error in resolving the catalog",
            level = "SEVERE",
            cause = "unknown",
            action = "unknown")
    public static final String ERROR_RESOLVING_CATALOG = LOGMSG_PREFIX + "-00089";

    @LogMessageInfo(
            message = "In doWebServicesDeployment: using local web services. There are {0}. The app has total of {1}.",
            level = "FINE")
    public static final String WS_LOCAL = LOGMSG_PREFIX + "-00090";

    @LogMessageInfo(
            message = "In doWebServicesDeployment: using web services via extension {0}",
            level = "FINE")
    public static final String WS_VIA_EXT = LOGMSG_PREFIX + "-00091";

    @LogMessageInfo(
            message = "File already exists {0}",
            level = "FINE")
    public static final String FILE_EXISTS = LOGMSG_PREFIX + "-00092";

    @LogMessageInfo(
            message = "Directory already exists {0}",
            level = "FINE")
    public static final String DIR_EXISTS = LOGMSG_PREFIX + "-00093";

    @LogMessageInfo(
            message = "Received HTTP GET containing text/xml content for endpoint {0} at {1}. HTTP POST should be used instead.",
            comment = "{0} - endpoint name, {1} - URL",
            level = "INFO")
    public static final String GET_RECEIVED = LOGMSG_PREFIX + "-00094";

    @LogMessageInfo(
            message = "Serving up final wsdl {0} for {1}",
            level = "FINE")
    public static final String SERVING_FINAL_WSDL = LOGMSG_PREFIX + "-00095";

    @LogMessageInfo(
            message = "Failure serving WSDL for web service {0}",
            comment = "{0} - endpoint name",
            level = "INFO")
    public static final String FAILURE_SERVING_WSDL = LOGMSG_PREFIX + "-00096";

    @LogMessageInfo(
            message = "Invalid wsdl request for web service {0}",
            comment = "{0} - endpoint name",
            level = "INFO")
    public static final String INVALID_WSDL_REQUEST = LOGMSG_PREFIX + "-00097";

    @LogMessageInfo(
            message = "Unable to load impl class {0}",
            level = "SEVERE",
            cause = "unknown",
            action = "unknown")
    public static final String CANNOT_LOAD_IMPLCLASS = LOGMSG_PREFIX + "-00098";

    @LogMessageInfo(
            message = "Cannot write out a HTTP XML exception : {0}",
            comment = "{0} - message from exception",
            level = "WARNING")
    public static final String CANNOT_WRITE_HTTPXML = LOGMSG_PREFIX + "-00099";

    @LogMessageInfo(
            message = "cannot write SOAPFault to the HTTP response",
            level = "WARNING")
    public static final String CANNOT_WRITE_SOAPFAULT = LOGMSG_PREFIX + "-00100";

    @LogMessageInfo(
            message = "Cannot create soap fault for {0}",
            comment = "{0} - fault message",
            level = "WARNING")
    public static final String CANNOT_CREATE_SOAPFAULT = LOGMSG_PREFIX + "-00101";

    @LogMessageInfo(
            message = "Class {0} not found during PreDestroy processing",
            comment = "{0} - class name",
            level = "SEVERE",
            cause = "unknown",
            action = "unknown")
    public static final String CLASS_NOT_FOUND_IN_PREDESTROY = LOGMSG_PREFIX + "-00102";

    @LogMessageInfo(
            message = "Handler class {0} not found during PreDestroy processing",
            comment = "{0} - class name",
            level = "SEVERE",
            cause = "unknown",
            action = "unknown")
    public static final String HANDLER_NOT_FOUND_IN_PREDESTROY = LOGMSG_PREFIX + "-00103";

    @LogMessageInfo(
            message = "Failure while calling PostConstruct/PreDestroy method",
            level = "SEVERE",
            cause = "unknown",
            action = "unknown")
    public static final String FAILURE_CALLING_POST_PRE = LOGMSG_PREFIX + "-00104";

    @LogMessageInfo(
            message = "Unable to load handler class {0}",
            comment = "{0} - class name, {1} - message from exception",
            level = "SEVERE",
            cause = "unknown",
            action = "unknown")
    public static final String HANDLER_UNABLE_TO_ADD = LOGMSG_PREFIX + "-00105";

    @LogMessageInfo(
            message = "Handler {0} instance injection failed: {1}",
            comment = "{0} - class name, {1} - message from exception",
            level = "SEVERE",
            cause = "unknown",
            action = "unknown")
    public static final String HANDLER_INJECTION_FAILED = LOGMSG_PREFIX + "-00106";
    
    @LogMessageInfo(
            message = "Cannot log SOAP Message {0}",
            comment = "{0} - message from exception",
            level = "WARNING")
    public static final String CANNOT_LOG_SOAPMSG = LOGMSG_PREFIX + "-00107";

    @LogMessageInfo(
            message = "Exception in creating endpoint",
            level = "SEVERE",
            cause = "unknown",
            action = "unknown")
    public static final String EXCEPTION_CREATING_ENDPOINT = LOGMSG_PREFIX + "-00108";

    @LogMessageInfo(
            message = "deleting directory failed : {0}",
            comment = "{0} - file name",
            level = "WARNING")
    public static final String DELETE_DIR_FAILED = LOGMSG_PREFIX + "-00109";

    @LogMessageInfo(
            message = "creating directory failed : {0}",
            comment = "{0} - file name",
            level = "SEVERE",
            cause = "unknown",
            action = "unknown")
    public static final String CREATE_DIR_FAILED = LOGMSG_PREFIX + "-00110";

    @LogMessageInfo(
            message = "Invoking wsimport with {0}",
            comment = "{0} - arguments",
            level = "INFO")
    public static final String WSIMPORT_INVOKE = LOGMSG_PREFIX + "-00111";

    @LogMessageInfo(
            message = "wsimport successful",
            level = "INFO")
    public static final String WSIMPORT_OK = LOGMSG_PREFIX + "-00112";

    @LogMessageInfo(
            message = "wsimport failed",
            level = "SEVERE",
            cause = "unknown",
            action = "unknown")
    public static final String WSIMPORT_FAILED = LOGMSG_PREFIX + "-00113";

    @LogMessageInfo(
            message = "authentication succeeded for endpoint '{0}' in {1} {2}",
            level = "FINER")
    public static final String AUTHENTICATION_SUCCESS = LOGMSG_PREFIX + "-00114";

    @LogMessageInfo(
            message = "wsimport successful",
            level = "FINE")
    public static final String AUTHENTICATION_FAILURE = LOGMSG_PREFIX + "-00115";

    @LogMessageInfo(
            message = "missing implementation class for {0}",
            comment = "{0} - endpoint name",
            level = "SEVERE",
            cause = "No class defined in deployment descriptor",
            action = "add implementation class definition to deployment descriptor")
    public static final String MISSING_IMPLEMENTATION_CLASS = LOGMSG_PREFIX + "-00116";

    @LogMessageInfo(
            message = "Web service endpoint {0} component link {1} is not valid",
            comment = "{0} - endpoint name, {1} - link name",
            level = "WARNING",
            cause = "Component link in webservices.xml is invalid",
            action = "check port-component-name matches the name of service implementation bean and check component link in webservices.xml")
    public static final String UNRESOLVED_LINK = LOGMSG_PREFIX + "-00117";
    
    @LogMessageInfo(
            message = "destroyManagedObject failed for Handler {0} for Service {1} with error {2}",
            comment = "{0} - handler class name, {1} - service endpoint name, {2} - exception message",
            level = "WARNING")
    public static final String DESTORY_ON_HANDLER_FAILED = LOGMSG_PREFIX + "-00120";

    @LogMessageInfo(
            message = "Module type '{0}' is not supported.",
            comment = "{0} - module type name",
            level = "SEVERE",
            cause = "Deployed module is not a web application nor ejb module.",
            action = "Make sure web service is implemented in EJB module or Web Application.")
    public static final String UNSUPPORTED_MODULE_TYPE = LOGMSG_PREFIX + "-00121";

    @LogMessageInfo(
            message = "{0} does not support {1}",
            comment = "{0} - parser class name, {1} - feature ID",
            level = "FINE")
    public static final String PARSER_UNSUPPORTED_FEATURE = LOGMSG_PREFIX + "-00122";

    @LogMessageInfo(
            message = "Going to fetch ServletAdapter holding wsdl content for web service {0} based on url: {1}",
            level = "INFO")
    public static final String SERVLET_ADAPTER_BASED_ON_WSDL_URL = LOGMSG_PREFIX + "-00123";

}
