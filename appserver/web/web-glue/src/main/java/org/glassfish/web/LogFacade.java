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
 */

package org.glassfish.web;

import org.glassfish.logging.annotation.LogMessageInfo;
import org.glassfish.logging.annotation.LoggerInfo;
import org.glassfish.logging.annotation.LogMessagesResourceBundle;
import java.util.logging.Logger;

/**
/**
 *
 * Provides the logging facilities.
 *
 * @author Shing Wai Chan
 */
public class LogFacade {
    @LogMessagesResourceBundle
    public static final String SHARED_LOGMESSAGE_RESOURCE =
            "com.sun.enterprise.web.LogMessages";

    @LoggerInfo(subsystem="WEB", description="Main WEB Logger", publish=true)
    public static final String WEB_MAIN_LOGGER = "javax.enterprise.web";

    public static final Logger LOGGER =
            Logger.getLogger(WEB_MAIN_LOGGER, SHARED_LOGMESSAGE_RESOURCE);

    private LogFacade() {}

    public static Logger getLogger() {
        return LOGGER;
    }

    private static final String prefix = "AS-WEB-GLUE-";

    @LogMessageInfo(
            message = "Exception in creating cache",
            level = "WARNING")
    public static final String CACHE_MANAGER_EXCEPTION_CREATING_CACHE = prefix + "00001";

    @LogMessageInfo(
            message = "Exception initializing cache-helper {0}; please check your helper class implementation",
            level = "INFO")
    public static final String CACHE_MANAGER_EXCEPTION_INITIALIZING_CACHE_HELPER = prefix + "00002";

    @LogMessageInfo(
            message = "Illegal CacheKeyGenerator",
            level = "WARNING")
    public static final String CACHE_DEFAULT_HELP_ILLEGAL_KET_GENERATOR = prefix + "00003";

    @LogMessageInfo(
            message = "DefaultCacheHelper: cannot find all the required key fields in the request {0}",
            level = "FINE")
    public static final String REQUIRED_KEY_FIELDS_NOT_FOUND = prefix + "00004";

    @LogMessageInfo(
            message = "CachingFilter {0} ready; isEnabled = {1} manager = {2}",
            level = "FINE")
    public static final String CACHING_FILTER_READY = prefix + "00005";

    @LogMessageInfo(
            message = "CachingFilter {0} request is cacheable; key {1} index = {2}",
            level = "FINE")
    public static final String CACHING_FILTER_CACHEABLE = prefix + "00006";

    @LogMessageInfo(
            message = "CachingFilter {0} request needs a refresh; key {1}",
            level = "FINE")
    public static final String CACHING_FILTER_NEEDS_REFRESH = prefix + "00007";

    @LogMessageInfo(
            message = "CachingFilter {0} serving response from the cache  {1}",
            level = "FINE")
    public static final String CACHING_FILTER_SERVING_RESPONSE = prefix + "00008";

    @LogMessageInfo(
            message = "CachingFilter {0} pass thru; isEnabled = {1}",
            level = "FINE")
    public static final String CACHING_FILTER_PASS_THRU = prefix + "00009";

    @LogMessageInfo(
            message = "CachingFilter {0} received cacheManager enabled event",
            level = "FINE")
    public static final String CACHING_FILTER_ENABLED_EVENT = prefix + "00010";

    @LogMessageInfo(
            message = "CachingFilter {0} received cacheManager disabled event",
            level = "FINE")
    public static final String CACHING_FILTER_DISABLED_EVENT = prefix + "00011";

    @LogMessageInfo(
            message = "The constraint field {0} is not found in the scope {1}; returning cache-on-match-failure: {2}",
            level = "FINE")
    public static final String CONSTRAINT_FIELD_NOT_FOUND = prefix + "00012";

    @LogMessageInfo(
            message = "The constraint field {0} value = {1} is found in scope {2}; returning cache-on-match: {3}",
            level = "FINE")
    public static final String CONSTRAINT_FIELD_FOUND = prefix + "00013";

    @LogMessageInfo(
            message = "The constraint field {0} value = {1} is found in scope {2}; and matches with a value {3}; returning cache-on-match: {4}",
            level = "FINE")
    public static final String CONSTRAINT_FIELD_MATCH = prefix + "00014";

    @LogMessageInfo(
            message = "The constraint field {0} value = {1} is found in scope {2}; but didn't match any of the value constraints; returning cache-on-match-failure = {3}",
            level = "FINE")
    public static final String CONSTRAINT_FIELD_NOT_MATCH = prefix + "00015";

    @LogMessageInfo(
            message = "Incorrect scope value [{0}] for web application cache field name [{1}]",
            level = "WARNING")
    public static final String CACHE_MAPPING_INCORRECT_SCOPE = prefix + "00016";

    @LogMessageInfo(
            message = "''greater'' expression requires a numeric value; please check your value {0}",
            level = "WARNING")
    public static final String GREATER_EXP_REQ_NUMERIC = prefix + "00017";

    @LogMessageInfo(
            message = "''lesser'' expression requires a numeric value; please check your value [{0}]",
            level = "WARNING")
    public static final String LESSER_EXP_REQ_NUMERIC = prefix + "00018";

    @LogMessageInfo(
            message = "illegal value [{0}] expr [{1}]",
            level = "WARNING")
    public static final String ILLEGAL_VALUE_EXP = prefix + "00019";

    @LogMessageInfo(
            message = "illegal in-range constraint; specify a valid range (xxx-yyy) value [{0}]",
            level = "WARNING")
    public static final String ILLEGAL_VALUE_RANGE = prefix + "00020";

    @LogMessageInfo(
            message = "missing separator in the ''in-range'' constraint; [{0}]",
            level = "WARNING")
    public static final String MISSING_RANGE_SEP = prefix + "00021";

    @LogMessageInfo(
            message = "''in-range'' constraint requires numeric values for the lower bound [{0}]",
            level = "WARNING")
    public static final String LOWER_RANGE_REQ_NUMBER = prefix + "00022";

    @LogMessageInfo(
            message = "''in-range'' constraint requires a value for the upper bound of the range; check your value [{0}]",
            level = "WARNING")
    public static final String RANGE_REQ_UPPER_BOUND= "AS-WEB-GLUE-00023";

    @LogMessageInfo(
            message = "''in-range'' constraint requires numeric values for the upper bound [{0}]",
            level = "WARNING")
    public static final String UPPER_RANGE_REQ_NUMBER = prefix + "00024";

    @LogMessageInfo(
            message = "CacheTag[{0}]: Timeout = {1}",
            level = "FINE")
    public static final String CACHETAG_TIMEOUT = prefix + "00025";

    @LogMessageInfo(
            message = "Cache not found in the specified scope",
            level = "INFO")
    public static final String TAGLIBS_CACHE_NO_CACHE = prefix + "00026";

    @LogMessageInfo(
            message = "Illegal value ([{0}]) for scope attribute of cache tag",
            level = "WARNING")
    public static final String ILLEGAL_SCOPE = prefix + "00027";

    @LogMessageInfo(
            message = "FlushTag: clear [{0}]",
            level = "FINE")
    public static final String FLUSH_TAG_CLEAR_KEY = prefix + "00028";

    @LogMessageInfo(
            message = "FlushTag: clear cache",
            level = "FINE")
    public static final String FLUSH_TAG_CLEAR_CACHE = prefix + "00029";

    @LogMessageInfo(
            message = "Process session destroyed on {0}",
            level = "FINE")
    public static final String SESSION_DESTROYED = prefix + "00030";

    @LogMessageInfo(
            message = "Process request for '{0}'",
            level = "FINE")
    public static final String REQUEST_PROCESSED = prefix + "00031";

    @LogMessageInfo(
            message = "Principal '{0}' has already been authenticated",
            level = "FINE")
    public static final String PRINCIPAL_ALREADY_AUTHENTICATED = prefix + "00032";

    @LogMessageInfo(
            message = "Checking for SSO cookie",
            level = "FINE")
    public static final String CHECKING_SSO_COOKIE = prefix + "00033";

    @LogMessageInfo(
            message = "SSO cookie is not present",
            level = "FINE")
    public static final String SSO_COOKIE_NOT_PRESENT = prefix + "00034";

    @LogMessageInfo(
            message = "No realm configured for this application, SSO does not apply",
            level = "FINE")
    public static final String NO_REALM_CONFIGURED = prefix + "00035";

    @LogMessageInfo(
            message = "This application uses realm '{0}'",
            level = "FINE")
    public static final String APP_REALM = prefix + "00036";

    @LogMessageInfo(
            message = "Checking for cached principal for {0}",
            level = "FINE")
    public static final String CHECKING_CACHED_PRINCIPAL = prefix + "00037";

    @LogMessageInfo(
            message = "Found cached principal '{0}' with auth type '{1}' in realm '{2}'",
            level = "FINE")
    public static final String FOUND_CACHED_PRINCIPAL = prefix + "00038";

    @LogMessageInfo(
            message = "Ignoring SSO entry which does not match application realm '{0}'",
            level = "FINE")
    public static final String IGNORING_SSO = prefix + "00039";

    @LogMessageInfo(
            message = "No cached principal found, erasing SSO cookie",
            level = "FINE")
    public static final String NO_CACHED_PRINCIPAL_FOUND = prefix + "00040";

    @LogMessageInfo(
            message = "Deregistering sso id '{0}'",
            level = "FINE")
    public static final String DEREGISTER_SSO = prefix + "00041";

    @LogMessageInfo(
            message = "SSO expiration started. Current entries: {0}",
            level = "FINE")
    public static final String SSO_EXPIRATION_STARTED = prefix + "00042";

    @LogMessageInfo(
            message = "SSO cache will expire {0} entries",
            level = "FINE")
    public static final String SSO_CACHE_EXPIRE = prefix + "00043";

    @LogMessageInfo(
            message = "SSO expiration removing entry: {0}",
            level = "FINE")
    public static final String SSO_EXPRIRATION_REMOVING_ENTRY = prefix + "00044";

    @LogMessageInfo(
            message = "Caught exception during SingleSignOn expiration",
            level = "WARNING")
    public static final String EXCEPTION_DURING_SSO_EXPIRATION = prefix + "00045";

    @LogMessageInfo(
            message = "Removing session {0} from sso id {1}",
            level = "FINE")
    public static final String REMOVE_SESSION_FROM_SSO = prefix + "00046";

    @LogMessageInfo(
            message = "Illegal access log pattern [{0}], is not a valid nickname and does not contain any ''%''",
            level = "SEVERE",
            cause = "The pattern is either null or does not contain '%'",
            action = "Check the pattern for validity")
    public static final String ACCESS_LOG_VALVE_INVALID_ACCESS_LOG_PATTERN = prefix + "00047";

    @LogMessageInfo(
            message = "Missing end delimiter in access log pattern: {0}",
            level = "SEVERE",
            cause = "An end delimiter ismissing in the access log pattern",
            action = "Check the pattern for validity")
    public static final String MISSING_ACCESS_LOG_PATTERN_END_DELIMITER = prefix + "00048";

    @LogMessageInfo(
            message = "Invalid component: {0} in access log pattern: {1}",
            level = "SEVERE",
            cause = "Access log pattern containds invalid component",
            action = "Check the pattern for validity")
    public static final String INVALID_ACCESS_LOG_PATTERN_COMPONENT = prefix + "00049";

    @LogMessageInfo(
            message = "Error processing request received on ad-hoc path {0}",
            level = "WARNING")
    public static final String ADHOC_SERVLET_SERVICE_ERROR = prefix + "00050";

    @LogMessageInfo(
            message = "No ad-hoc servlet configured to process ad-hoc path {0}",
            level = "WARNING")
    public static final String NO_ADHOC_SERVLET = prefix + "00051";

    @LogMessageInfo(
            message = "mgr reapInterval set = {0}",
            level = "FINEST")
    public static final String MANAGER_REAP_INTERVAL_SET = prefix + "00052";

    @LogMessageInfo(
            message = "no instance level value set for mgr reapInterval",
            level = "FINEST")
    public static final String NO_INSTANCE_LEVEL_VALUE_SET_MGR_REAP_INTERVAL = prefix + "00053";

    @LogMessageInfo(
            message = "maxSessions set = {0}",
            level = "FINEST")
    public static final String MAX_SESSIONS_SET = prefix + "00054";

    @LogMessageInfo(
            message = "no instance level value set for maxSessions",
            level = "FINEST")
    public static final String NO_INSTANCE_LEVEL_VALUE_SET_MAX_SESSIONS = prefix + "00055";

    @LogMessageInfo(
            message = "sessionFilename set = {0}",
            level = "FINEST")
    public static final String SESSION_FILENAME_SET = prefix + "00056";

    @LogMessageInfo(
            message = "sessionIdGeneratorClassname set = {0}",
            level = "FINEST")
    public static final String SESSION_ID_GENERATOR_CLASSNAME_SET = prefix + "00057";

    @LogMessageInfo(
            message = "storeReapInterval set = {0}",
            level = "FINEST")
    public static final String STORE_REAP_INTERVAL_SET = prefix + "00058";

    @LogMessageInfo(
            message = "directory set = {0}",
            level = "FINEST")
    public static final String DIRECTORY_SET = prefix + "00059";

    @LogMessageInfo(
            message = "sessionMaxInactiveInterval set = {0}",
            level = "FINEST")
    public static final String SESSION_MAX_INACTIVE_INTERVAL_SET = prefix + "00060";

    @LogMessageInfo(
            message = "no instance level value set for sessionMaxInactiveInterval",
            level = "FINEST")
    public static final String NO_INSTANCE_LEVEL_VALUE_SET_SESSION_MAX_INACTIVE_INTERVAL = prefix + "00061";

    @LogMessageInfo(
            message = "Configuring cache for web application",
            level = "FINE")
    public static final String CONFIGURE_CACHE = prefix + "00062";

    @LogMessageInfo(
            message = "Added a caching filter for servlet-name = {0} url-pattern = {1}",
            level = "FINE")
    public static final String CACHING_FILTER_ADDED = prefix + "00063";

    @LogMessageInfo(
            message = "Added a key-field : name = {0} scope = {1}",
            level = "FINE")
    public static final String KEY_FIELD_ADDED = prefix + "00064";

    @LogMessageInfo(
            message = "Added a constraint: {0}",
            level = "FINE")
    public static final String CONSTRAINT_ADDED = prefix + "00065";

    @LogMessageInfo(
            message = "Added a constraint-field name = {0} scope = {1} cache-on-match = {2} cache-on-match-failure = {3}",
            level = "FINE")
    public static final String CONSTRAINT_FIELD_ADDED = prefix + "00066";

    @LogMessageInfo(
            message = "Invalid max-pending-count attribute value [{0}], using default [{1}]",
            level = "WARNING")
    public static final String INVALID_MAX_PENDING_COUNT = prefix + "00067";

    @LogMessageInfo(
            message = "Unable to parse proxy port component ({0}) of server-name attribute of network-listener {1}",
            level = "SEVERE",
            cause = "The String does not contain a parsable integer",
            action = "Check the proxy port string")
    public static final String INVALID_PROXY_PORT = prefix + "00068";

    @LogMessageInfo(
            message = "Unable to parse redirect-port ({0}) attribute of network-listener {1}, using default: {2}",
            level = "WARNING")
    public static final String INVALID_REDIRECT_PORT = prefix + "00069";

    @LogMessageInfo(
            message = "Unable to parse acceptor-threads attribute ({0}) of network-listener {1}, using default: {2}",
            level = "WARNING")
    public static final String INVALID_ACCEPTOR_THREADS = prefix + "00070";

    @LogMessageInfo(
            message = "The jk properties configuration file is not defined",
            level = "FINEST")
    public static final String JK_PROPERTIES_NOT_DEFINED = prefix + "00071";

    @LogMessageInfo(
            message = "JK properties file {0} does not exist",
            level = "WARNING")
    public static final String MISSING_JK_PROPERTIES = prefix + "00072";

    @LogMessageInfo(
            message = "Loading glassfish-jk.properties from {0}",
            level = "FINEST")
    public static final String LOADING_JK_PROPERTIED = prefix + "00073";

    @LogMessageInfo(
            message = "Unable to configure JK properties {0} for connector listening to {1}",
            level = "SEVERE",
            cause = "Failed to load JK properties file",
            action = "Check if the properties file exists and is readable")
    public static final String UNABLE_TO_CONFIGURE_JK = prefix + "00074";

    @LogMessageInfo(
            message = "Invalid attribute [{0}] in thread-pool configuration",
            level = "WARNING")
    public static final String INVALID_THREAD_POOL_ATTRIBUTE = prefix + "00075";

    @LogMessageInfo(
            message = "Unable to load ProxyHandler implementation class {0}",
            level = "SEVERE",
            cause = "An exception occurred during creating a new instance ",
            action = "Check the exception for the error")
    public static final String PROXY_HANDLER_CLASS_LOAD_ERROR = prefix + "00076";

    @LogMessageInfo(
            message = "{0} not an instance of com.sun.appserv.ProxyHandler",
            level = "SEVERE",
            cause = "Invalid proxy handler",
            action = "Check to see if the proxy handler is an instance of com.sun.appserv.ProxyHandler")
    public static final String PROXY_HANDLER_CLASS_INVALID = prefix + "00077";

    @LogMessageInfo(
            message = "All SSL protocol variants disabled for network-listener {0}, using SSL implementation specific defaults",
            level = "WARNING")
    public static final String ALL_SSL_PROTOCOLS_DISABLED = prefix + "00078";

    @LogMessageInfo(
            message = "All SSL cipher suites disabled for network-listener(s) {0}. Using SSL implementation specific defaults",
            level = "FINE")
    public static final String ALL_CIPHERS_DISABLED  = prefix + "00079";

    @LogMessageInfo(
            message = "Unrecognized cipher: {0}",
            level = "WARNING")
    public static final String UNRECOGNIZED_CIPHER = prefix + "00080";

    @LogMessageInfo(
            message = "Exception when initializing monitoring for network-listener [{0}]",
            level = "WARNING")
    public static final String INIT_MONITORING_EXCEPTION = prefix + "00081";

    @LogMessageInfo(
            message = "InvokeGrizzly method={0} objectName={1}",
            level = "FINE")
    public static final String INVOKE_GRIZZLY = prefix + "00082";

    @LogMessageInfo(
            message = "Exception while invoking mebean server operation [{0}]",
            level = "WARNING")
    public static final String INVOKE_MBEAN_EXCEPTION = prefix + "00083";

    @LogMessageInfo(
            message = "Cannot find WebContainer implementation",
            level = "SEVERE",
            cause = "Web container is null",
            action = "Check if the mapper listener is initialized correctly")
    public static final String CANNOT_FIND_WEB_CONTAINER = prefix + "00084";

    @LogMessageInfo(
            message = "Cannot find Engine implementation",
            level = "SEVERE",
            cause = "Engine is null",
            action = "Check if the mapper listener is initialized correctly")
    public static final String CANNOT_FIND_ENGINE = prefix + "00085";

    @LogMessageInfo(
            message = "Error registering contexts",
            level = "WARNING")
    public static final String ERROR_REGISTERING_CONTEXTS = prefix + "00086";

    @LogMessageInfo(
            message = "HTTP listener with network listener name {0} ignoring registration of host with object name {1}, because none of the host's associated HTTP listeners matches this network listener name",
            level = "FINE")
    public static final String IGNORE_HOST_REGISTRATIONS = prefix + "00087";

    @LogMessageInfo(
            message = "Register Context {0}",
            level = "FINE")
    public static final String REGISTER_CONTEXT = prefix + "00088";

    @LogMessageInfo(
            message = "Unregister Context {0}",
            level = "FINE")
    public static final String UNREGISTER_CONTEXT = prefix + "00089";

    @LogMessageInfo(
            message = "Register Wrapper {0} in Context {1}",
            level = "FINE")
    public static final String REGISTER_WRAPPER = prefix + "00090";

    @LogMessageInfo(
            message = "Unable to instantiate ContainerListener of type {0}",
            level = "SEVERE",
            cause = "An exception occurred during instantiation of ContainerListener of type {0}",
            action = "Check the Exception for error")
    public static final String UNABLE_TO_INSTANTIATE_CONTAINER_LISTENER = prefix + "00091";

    @LogMessageInfo(
            message = "Creating connector for address='{0}' port='{1}' protocol='{2}'",
            level = "FINE")
    public static final String CREATE_CONNECTOR = prefix + "00092";

    @LogMessageInfo(
            message = "Enabling file-based persistence for web module [{0}]''s sessions",
            level = "INFO")
    public static final String FILE_PERSISTENCE = prefix + "00093";

    @LogMessageInfo(
            message = "Exception during invocation of PreDestroy-annotated method on JSP tag handler [{0}]",
            level = "WARNING")
    public static final String EXCEPTION_DURING_JSP_TAG_HANDLER_PREDESTROY = prefix + "00094";

    @LogMessageInfo(
            message = "ServerContext is null for ResourceInjector",
            level = "INFO")
    public static final String NO_SERVERT_CONTEXT = prefix + "00095";

    @LogMessageInfo(
            message = "Enabling no persistence for web module [{0}]''s sessions: persistence-type = [{1}]",
            level = "FINE")
    public static final String NO_PERSISTENCE = prefix + "00096";

    @LogMessageInfo(
            message = "Unable to load session uuid generator [{0}]",
            level = "SEVERE",
            cause = "An exception occurred during loading session uuid generator",
            action = "Check the Exception for the error")
    public static final String UNABLE_TO_LOAD_SESSION_UUID_GENERATOR = prefix + "00097";

    @LogMessageInfo(
            message = "Unable to write access log file {0}",
            level = "SEVERE",
            cause = "An exception occurred writing to access log file",
            action = "Check the exception for the error")
    public static final String ACCESS_LOG_UNABLE_TO_WRITE = prefix + "00098";

    @LogMessageInfo(
            message = "Setting accesslog directory for virtual server '{0}' to {1}",
            level = "FINE")
    public static final String ACCESS_LOG_DIRECTORY_SET = prefix + "00099";

    @LogMessageInfo(
            message = "Invalid accessLogWriterInterval value [{0}]",
            level = "WARNING")
    public static final String INVALID_ACCESS_LOG_WRITER_INTERVAL = prefix + "00100";

    @LogMessageInfo(
            message = "Invalid accessLogBufferSize value [{0}]",
            level = "WARNING")
    public static final String INVALID_ACCESS_LOG_BUFFER_SIZE = prefix + "00101";

    @LogMessageInfo(
            message = "Unable to parse max-history-files access log configuration [{0}]",
            level = "WARNING")
    public static final String INVALID_MAX_HISTORY_FILES = prefix + "00102";

    @LogMessageInfo(
            message = "Unable to create {0}",
            level = "WARNING")
    public static final String UNABLE_TO_CREATE = prefix + "00103";

    @LogMessageInfo(
            message = "Unable to rename access log file {0} to {1}",
            level = "WARNING")
    public static final String UNABLE_TO_RENAME_LOG_FILE = prefix + "00104";

    @LogMessageInfo(
            message = "Unable to remove access log file {0}",
            level = "WARNING")
    public static final String UNABLE_TO_REMOVE_LOG_FILE = prefix + "00105";

    @LogMessageInfo(
            message = "Access logger has already been started",
            level = "WARNING")
    public static final String ACCESS_LOG_ALREADY_STARTED = prefix + "00106";

    @LogMessageInfo(
            message = "Access logger has not yet been started",
            level = "WARNING")
    public static final String ACCESS_LOG_NOT_STARTED = prefix + "00107";

    @LogMessageInfo(
            message = "PersistenceStrategyBuilderFactory>>createPersistenceStrategyBuilder: resolvedPersistenceType = {0}, resolvedPersistenceFrequency = {1} resolvedPersistenceScope = {2}",
            level = "FINEST")
    public static final String CREATE_PERSISTENCE_STRATEGY_BUILDER_INFO = prefix + "00108";

    @LogMessageInfo(
            message = "Could not find PersistentStrategyBuilder for persistenceType {0}",
            level = "FINEST")
    public static final String PERSISTENT_STRATEGY_BUILDER_NOT_FOUND = prefix + "00109";

    @LogMessageInfo(
            message = "PersistenceStrategyBuilderFactory>>createPersistenceStrategyBuilder: CandidateBuilderClassName = {0}",
            level = "FINEST")
    public static final String CREATE_PERSISTENCE_STRATEGY_BUILDER_CLASS_NAME = prefix + "00110";

    @LogMessageInfo(
            message = "Unable to set request encoding [{0}] determined from sun-web.xml deployment descriptor of web application [{1}]",
            level = "WARNING")
    public static final String UNABLE_TO_SET_ENCODING = prefix + "00112";

    @LogMessageInfo(
            message = "POST data too large",
            level = "WARNING")
    public static final String POST_TOO_LARGE = prefix + "00113";

    @LogMessageInfo(
            message = "Web container config changed {0} {1} {2}",
            level = "FINE")
    public static final String CHANGE_INVOKED = prefix + "00114";

    @LogMessageInfo(
            message = "Exception processing HttpService configuration change",
            level = "SEVERE",
            cause = "An exception occurred during configuration change ",
            action = "Check the exception for error")
    public static final String EXCEPTION_WEB_CONFIG = prefix + "00115";

    @LogMessageInfo(
            message = "AvailabilityService was not defined - check domain.xml",
            level = "FINEST")
    public static final String AVAILABILITY_SERVICE_NOT_DEFINED = prefix + "00116";

    @LogMessageInfo(
            message = "WebContainerAvailability not defined - check domain.xml",
            level = "FINEST")
    public static final String WEB_CONTAINER_AVAILABILITY_NOT_DEFINED = prefix + "00117";

    @LogMessageInfo(
            message = "globalAvailability = {0}",
            level = "FINEST")
    public static final String GLOBAL_AVAILABILITY= "AS-WEB-GLUE-00118";

    @LogMessageInfo(
            message = "webContainerAvailability = {0}",
            level = "FINEST")
    public static final String WEB_CONTAINER_AVAILABILITY = prefix + "00119";

    @LogMessageInfo(
            message = "webModuleAvailability = {0}",
            level = "FINEST")
    public static final String WEB_MODULE_AVAILABILITY = prefix + "00120";

    @LogMessageInfo(
            message = "SERVER.XML persistenceType= {0}",
            level = "FINEST")
    public static final String PERSISTENCE_TYPE = prefix + "00121";

    @LogMessageInfo(
            message = "SERVER.XML persistenceType missing",
            level = "FINEST")
    public static final String PERSISTENCE_TYPE_MISSING = prefix + "00122";

    @LogMessageInfo(
            message = "Web App Distributable {0}: {1}",
            level = "FINEST")
    public static final String WEB_APP_DISTRIBUTABLE = prefix + "00123";

    @LogMessageInfo(
            message = "AvailabilityGloballyEnabled = {0}",
            level = "FINEST")
    public static final String AVAILABILITY_GLOBALLY_ENABLED = prefix + "00124";

    @LogMessageInfo(
            message = "instance-level persistence-type = {0} instance-level persistenceFrequency = {1} instance-level persistenceScope = {2}",
            level = "FINEST")
    public static final String INSTANCE_LEVEL_INFO = prefix + "00125";

    @LogMessageInfo(
            message = "webAppLevelPersistenceType = {0} webAppLevelPersistenceFrequency = {1} webAppLevelPersistenceScope = {2}",
            level = "FINEST")
    public static final String WEB_APP_LEVEL_INFO = prefix + "00126";

    @LogMessageInfo(
            message = "IN WebContainer>>ConfigureSessionManager after web level check AFTER_WEB_PERSISTENCE-TYPE IS = {0} AFTER_WEB_PERSISTENCE_FREQUENCY IS = {1} AFTER_WEB_PERSISTENCE_SCOPE IS = {2}",
            level = "FINEST")
    public static final String AFTER_WEB_LEVEL_CHECK_INFO = prefix + "00127";

    @LogMessageInfo(
            message = "Is {0} a system app: {1}",
            level = "FINEST")
    public static final String IS_SYSTEM_APP = prefix + "00128";

    @LogMessageInfo(
            message = "SessionConfigurationHelper: Is AppDistributable {0}",
            level = "FINEST")
    public static final String IS_APP_DISTRIBUTABLE = prefix + "00129";

    @LogMessageInfo(
            message = "Invalid Session Management Configuration for non-distributable app [{0}] - defaulting to memory: persistence-type = [{1}] / persistenceFrequency = [{2}] / persistenceScope = [{3}]",
            level = "INFO")
    public static final String INVALID_SESSION_MANAGER_CONFIG = prefix + "00130";

    @LogMessageInfo(
            message = "IN WebContainer>>ConfigureSessionManager before builder factory FINAL_PERSISTENCE-TYPE IS = {0} FINAL_PERSISTENCE_FREQUENCY IS = {1} FINAL_PERSISTENCE_SCOPE IS = {2}",
            level = "FINEST")
    public static final String CONFIGURE_SESSION_MANAGER_FINAL = prefix + "00131";

    @LogMessageInfo(
            message = "Security role name {0} used in an <auth-constraint> without being defined in a <security-role>",
            level = "WARNING")
    public static final String ROLE_AUTH = prefix + "00132";

    @LogMessageInfo(
            message = "Security role name {0} used in a <run-as> without being defined in a <security-role>",
            level = "WARNING")
    public static final String ROLE_RUNAS = prefix + "00133";

    @LogMessageInfo(
            message = "Security role name {0} used in a <role-link> without being defined in a <security-role>",
            level = "WARNING")
    public static final String ROLE_LINK = prefix + "00134";

    @LogMessageInfo(
            message = "The web module {0} has been designated as the default-web-module for virtual server {1}",
            level = "FINE")
    public static final String VS_DEFAULT_WEB_MODULE = prefix + "00135";

    @LogMessageInfo(
            message = "Error looking up the configuration information of the default-web-module {0} for virtual server {1}",
            level = "SEVERE",
            cause = "The web module specified is either not found or disabled or does not specify this virtual server, " +
                    "or there was an error loading its deployment descriptors",
            action = "Verify if the virtual server's default web module is valid")
    public static final String VS_DEFAULT_WEB_MODULE_NOT_FOUND = prefix + "00136";

    @LogMessageInfo(
            message = "The default-web-module {0} is either disabled or does not specify virtual server {1}",
            level = "SEVERE",
            cause = "The default web module is disabled or does not specify virtual server",
            action = "Verify if the default web module is enabled and specify virtual server")
    public static final String VS_DEFAULT_WEB_MODULE_DISABLED = prefix + "00137";

    @LogMessageInfo(
            message = "Virtual server {0} has invalid authentication realm {1}",
            level = "SEVERE",
            cause = "The realm {1} could not be found",
            action = "Verify if the realm {1} exits for virtual server {0}")
    public static final String INVALID_AUTH_REALM = prefix + "00138";

    @LogMessageInfo(
            message = "Invalid sso-cookie-secure configuration {0} for virtual server {1}",
            level = "INFO")
    public static final String INVALID_SSO_COOKIE_SECURE = prefix + "00139";

    @LogMessageInfo(
            message = "Realm {0} is not an instance of {1}, and will be ignored",
            level = "SEVERE",
            cause = "The realm {0} is either NULL or is not an instance of {1}",
            action = "Verify if the realm {0} is an instance of {1}")
    public static final String IGNORE_INVALID_REALM = prefix + "00140";

    @LogMessageInfo(
            message = "Virtual server {0} has a property with missing name or value",
            level = "WARNING")
    public static final String NULL_VIRTUAL_SERVER_PROPERTY = prefix + "00141";

    @LogMessageInfo(
            message = "Invalid redirect property value {0} for virtual server {1}: More than one {2} component",
            level = "WARNING")
    public static final String REDIRECT_MULTIPLE_ELEMENT = prefix + "00142";

    @LogMessageInfo(
            message = "Invalid redirect property value {0} for virtual server {1}: Missing url or url-prefix component",
            level = "WARNING")
    public static final String REDIRECT_MISSING_URL_OR_URL_PREFIX = prefix + "00143";

    @LogMessageInfo(
            message = "Invalid redirect property value {0} for virtual server {1}: Both url and url-prefix specified",
            level = "WARNING")

    public static final String REDIRECT_BOTH_URL_AND_URL_PREFIX = prefix + "00144";

    @LogMessageInfo(
            message = "Invalid redirect property value {0} for virtual server {1}: escape must be equal to yes or no",
            level = "WARNING")
    public static final String REDIRECT_INVALID_ESCAPE = prefix + "00145";

    @LogMessageInfo(
            message = "Invalid send-error property value {0} for virtual server {1}: More than one {2} component",
            level = "WARNING")
    public static final String SEND_ERROR_MULTIPLE_ELEMENT = prefix + "00146";

    @LogMessageInfo(
            message = "Invalid send-error property value {0} for virtual server {1}: Missing path component",
            level = "WARNING")
    public static final String SEND_ERROR_MISSING_PATH = prefix + "00147";

    @LogMessageInfo(
            message = "Unable to add listener of type {0} to virtual server {1}",
            level = "SEVERE",
            cause = "The listener is not an instance of ContainerListener or LifecycleListener",
            action = "Verify if the listener type is supported")
    public static final String INVALID_LISTENER_VIRTUAL_SERVER = prefix + "00148";

    @LogMessageInfo(
            message = " Unable to load extension class {0} from web module {1}",
            level = "SEVERE",
            cause = "An exception occurred loading extension class",
            action = "Check the exception for the error")
    public static final String UNABLE_TO_LOAD_EXTENSION_SEVERE = prefix + "00149";

    @LogMessageInfo(
            message = "Object of type classname {0} not an instance of Valve or GlassFishValve",
            level = "WARNING")
    public static final String NOT_A_VALVE = prefix + "00150";

    @LogMessageInfo(
            message = "Error adding HttpProbes. NetworkListener {0}'s HttpCodecFilter is {1}",
            level = "SEVERE",
            cause = "HttpCodecFilter is either NULL or empty",
            action = "Verify the NetworkListener is valid")
    public static final String CODE_FILTERS_NULL = prefix + "00151";

    @LogMessageInfo(
            message = "Error adding HttpProbes",
            level = "SEVERE",
            cause = "An exception occurred adding HttpProbes",
            action = "Check the exception for the error")
    public static final String ADD_HTTP_PROBES_ERROR = prefix + "00152";

    @LogMessageInfo(
            message = "Disabling Single Sign On (SSO) for virtual server {0} as configured",
            level = "FINE")
    public static final String DISABLE_SSO= "AS-WEB-GLUE-00153";

    @LogMessageInfo(
            message = "Enabling Single Sign On (SSO) for virtual server {0} as configured",
            level = "FINE")
    public static final String ENABLE_SSO = prefix + "00154";

    @LogMessageInfo(
            message = "SSO entry max idle time set to {0} for virtual server {1}",
            level = "FINE")
    public static final String SSO_MAX_INACTIVE_SET= "AS-WEB-GLUE-00155";

    @LogMessageInfo(
            message = "SSO expire thread interval set to {0} for virtual server {1}",
            level = "FINE")
    public static final String SSO_REAP_INTERVAL_SET = prefix + "00156";

    @LogMessageInfo(
            message = "Allowing access to {0} from {1}",
            level = "FINE")
    public static final String ALLOW_ACCESS = prefix + "00157";

    @LogMessageInfo(
            message = "Denying access to {0} from {1}",
            level = "FINE")
    public static final String DENY_ACCESS = prefix + "00158";

    @LogMessageInfo(
            message = "Virtual server {0} enabled context {1}",
            level = "FINE")
    public static final String VS_ENABLED_CONTEXT = prefix + "00159";

    @LogMessageInfo(
            message = "Unable to delete {0}",
            level = "WARNING")
    public static final String UNABLE_TO_DELETE = prefix + "00160";

    @LogMessageInfo(
            message = "Unable to reconfigure access log valve",
            level = "SEVERE",
            cause = "An exception occurred during access log valve reconfiguration",
            action = "Check the exception for error")
    public static final String UNABLE_RECONFIGURE_ACCESS_LOG = prefix + "00161";

    @LogMessageInfo(
            message = "Virtual server {0} added context {1}",
            level = "FINE")
    public static final String VS_ADDED_CONTEXT = prefix + "00162";

    @LogMessageInfo(
            message = "Application {0} is not found",
            level = "SEVERE",
            cause = "The deployed application is not found",
            action = "Check if the application is valid")
    public static final String APP_NOT_FOUND = prefix + "00163";

    @LogMessageInfo(
            message = "Cannot create context for undeployment",
            level = "SEVERE",
            cause = "An IOException occurred during undeployment",
            action = "Check the exception for error")
    public static final String REMOVE_CONTEXT_ERROR = prefix + "00164";

    @LogMessageInfo(
            message = "Successfully removed context {0}",
            level = "FINE")
    public static final String REMOVED_CONTEXT = prefix + "00165";

    @LogMessageInfo(
            message = "Modifying web.xml {0}",
            level = "FINE")
    public static final String MODIFYING_WEB_XML = prefix + "00166";

    @LogMessageInfo(
            message = "Error adding HttpProbes. NetworkListener {0}'s GrizzlyProxy is NULL",
            level = "SEVERE",
            cause = "GrizzlyProxy is NULL",
            action = "Verify the NetworkListener is valid")
    public static final String PROXY_NULL = prefix + "00167";

    @LogMessageInfo(
            message = "Virtual server {0} has been turned off",
            level = "FINE")
    public static final String VS_VALVE_OFF = prefix + "00168";

    @LogMessageInfo(
            message = "Virtual server {0} has been disabled",
            level = "FINE")
    public static final String VS_VALVE_DISABLED = prefix + "00169";

    @LogMessageInfo(
            message = "Invalid redirect URL [{0}]: Impossible to URL encode",
            level = "WARNING")
    public static final String INVALID_REDIRECTION_LOCATION = prefix + "00170";

    @LogMessageInfo(
            message = "Unknown error, loadWebModule returned null, file a bug",
            level = "SEVERE",
            cause = "An exception occurred writing to access log file",
            action = "Check the exception for the error")
    public static final String WEBAPP_UNKNOWN_ERROR = prefix + "00171";

    @LogMessageInfo(
            message = "Loading application [{0}] at [{1}]",
            level = "INFO")
    public static final String LOADING_APP = prefix + "00172";

    @LogMessageInfo(
            message = "App config customization specified to ignore descriptor's {0} {1} so it will not be present for the application",
            level = "FINER")
    public static final String IGNORE_DESCRIPTOR = prefix + "00173";

    @LogMessageInfo(
            message = "Overriding descriptor {0}",
            level = "FINER")
    public static final String OVERIDE_DESCRIPTOR = prefix + "00174";

    @LogMessageInfo(
            message = "Creating new {0}",
            level = "FINER")
    public static final String CREATE_DESCRIPTOR = prefix + "00175";

    @LogMessageInfo(
            message = "Exception during Coherence*Web shutdown for application [{0}]",
            level = "WARNING")
    public static final String EXCEPTION_SHUTDOWN_COHERENCE_WEB = prefix + "00176";

    @LogMessageInfo(
            message = "Loading web module {0} in virtual server {1} at {2}",
            level = "INFO")
    public static final String WEB_MODULE_LOADING = prefix + "00177";

    @LogMessageInfo(
            message = "This web container has not yet been started",
            level = "INFO")
    public static final String WEB_CONTAINER_NOT_STARTED = prefix + "00178";

    @LogMessageInfo(
            message = "Property {0} is not yet supported",
            level = "INFO")
    public static final String PROPERTY_NOT_YET_SUPPORTED = prefix + "00179";

    @LogMessageInfo(
            message = "Virtual server {0} already has a web module {1} loaded at {2} therefore web module {3} cannot be loaded at this context path on this virtual server",
            level = "INFO")
    public static final String DUPLICATE_CONTEXT_ROOT = prefix + "00180";

    @LogMessageInfo(
            message = "Unable to stop web container",
            level = "SEVERE",
            cause = "Web container may not have been started",
            action = "Verify if web container is started")
    public static final String UNABLE_TO_STOP_WEB_CONTAINER = prefix + "00181";

    @LogMessageInfo(
            message = "Unable to start web container",
            level = "SEVERE",
            cause = "Web container may have already been started",
            action = "Verify if web container is not already started")
    public static final String UNABLE_TO_START_WEB_CONTAINER = prefix + "00182";

    @LogMessageInfo(
            message = "Property element in sun-web.xml has null 'name' or 'value'",
            level = "INFO")
    public static final String NULL_WEB_PROPERTY = prefix + "00183";

    @LogMessageInfo(
            message = "Web module {0} is not loaded in virtual server {1}",
            level = "SEVERE",
            cause = "Web module has failed to load",
            action = "Verify if web module is valid")
    public static final String WEB_MODULE_NOT_LOADED_TO_VS = prefix + "00184";

    @LogMessageInfo(
            message = "Unable to deploy web module {0} at root context of virtual server {1}, because this virtual server declares a default-web-module",
            level = "INFO")
    public static final String DEFAULT_WEB_MODULE_CONFLICT = prefix + "00185";

    @LogMessageInfo(
            message = "Unable to set default-web-module {0} for virtual server {1}",
            level = "SEVERE",
            cause = "There is no web context deployed on the given" +
                    "virtual server that matches the given default context path",
            action = "Verify if the default context path is deployed on the virtual server")
    public static final String DEFAULT_WEB_MODULE_ERROR= "AS-WEB-GLUE-00186";

    @LogMessageInfo(
            message = "Unable to load web module {0} at context root {1}, because it is not correctly encoded",
            level = "INFO")
    public static final String INVALID_ENCODED_CONTEXT_ROOT = prefix + "00187";

    @LogMessageInfo(
            message = "Unable to destroy web module deployed at context root {0} on virtual server {1} during undeployment",
            level = "WARNING")
    public static final String EXCEPTION_DURING_DESTROY = prefix + "00188";

    @LogMessageInfo(
            message = "Exception setting the schemas/dtds location",
            level = "SEVERE",
            cause = "A malformed URL has occurred. Either no legal protocol could be found in a specification string " +
                    "or the string could not be parsed",
            action = "Verify if the schemas and dtds")
    public static final String EXCEPTION_SET_SCHEMAS_DTDS_LOCATION = prefix + "00189";

    @LogMessageInfo(
            message = "Error loading web module {0}",
            level = "SEVERE",
            cause = "An error occurred during loading web module",
            action = "Check the Exception for the error")
    public static final String LOAD_WEB_MODULE_ERROR = prefix + "00191";

    @LogMessageInfo(
            message = "Undeployment failed for context {0}",
            level = "SEVERE",
            cause = "The context may not have been deployed",
            action = "Verify if the context is deployed on the virtual server")
    public static final String UNDEPLOY_ERROR = prefix + "00192";

    @LogMessageInfo(
            message = "Exception processing HttpService configuration change",
            level = "SEVERE",
            cause = "An error occurred during configurting http service",
            action = "Verify if the configurations are valid")
    public static final String EXCEPTION_CONFIG_HTTP_SERVICE = prefix + "00193";

    @LogMessageInfo(
            message = "Unable to set context root {0}",
            level = "WARNING")
    public static final String UNABLE_TO_SET_CONTEXT_ROOT = prefix + "00194";

    @LogMessageInfo(
            message = "Unable to disable web module at context root {0}",
            level = "WARNING")
    public static final String DISABLE_WEB_MODULE_ERROR = prefix + "00195";

    @LogMessageInfo(
            message = "Error during destruction of virtual server {0}",
            level = "WARNING")
    public static final String DESTROY_VS_ERROR = prefix + "00196";

    @LogMessageInfo(
            message = "Virtual server {0} cannot be updated, because it does not exist",
            level = "WARNING")
    public static final String CANNOT_UPDATE_NON_EXISTENCE_VS= "AS-WEB-GLUE-00197";

    @LogMessageInfo(
            message = "Created HTTP listener {0} on host/port {1}:{2}",
            level = "INFO")
    public static final String HTTP_LISTENER_CREATED = prefix + "00198";

    @LogMessageInfo(
            message = "Created JK listener {0} on host/port {1}:{2}",
            level = "INFO")
    public static final String JK_LISTENER_CREATED = prefix + "00199";

    @LogMessageInfo(
            message = "Created virtual server {0}",
            level = "INFO")
    public static final String VIRTUAL_SERVER_CREATED = prefix + "00200";

    @LogMessageInfo(
            message = "Virtual server {0} loaded default web module {1}",
            level = "INFO")
    public static final String VIRTUAL_SERVER_LOADED_DEFAULT_WEB_MODULE = prefix + "00201";

    @LogMessageInfo(
            message = "Maximum depth for nested request dispatches set to {0}",
            level = "FINE")
    public static final String MAX_DISPATCH_DEPTH_SET = prefix + "00202";

    @LogMessageInfo(
            message = "Unsupported http-service property {0} is being ignored",
            level = "WARNING")
    public static final String INVALID_HTTP_SERVICE_PROPERTY = prefix + "00203";

    @LogMessageInfo(
            message = "The host name {0} is shared by virtual servers {1} and {2}, which are both associated with the same HTTP listener {3}",
            level = "SEVERE",
            cause = "The host name is not unique",
            action = "Verify that the host name is unique")
    public static final String DUPLICATE_HOST_NAME = prefix + "00204";

    @LogMessageInfo(
            message = "Network listener {0} referenced by virtual server {1} does not exist",
            level = "SEVERE",
            cause = "Network listener {0} referenced by virtual server {1} does not exist",
            action = "Verify that the network listener is valid")
    public static final String LISTENER_REFERENCED_BY_HOST_NOT_EXIST = prefix + "00205";

    @LogMessageInfo(
            message = "Web module {0} not loaded to any virtual servers",
            level = "INFO")
    public static final String WEB_MODULE_NOT_LOADED_NO_VIRTUAL_SERVERS = prefix + "00206";

    @LogMessageInfo(
            message = "Loading web module {0} to virtual servers {1}",
            level = "FINE")
    public static final String LOADING_WEB_MODULE = prefix + "00207";

    @LogMessageInfo(
            message = "Unloading web module {0} from virtual servers {1}",
            level = "FINE")
    public static final String UNLOADING_WEB_MODULE = prefix + "00208";

    @LogMessageInfo(
            message = "Context {0} undeployed from virtual server {1}",
            level = "FINE")
    public static final String CONTEXT_UNDEPLOYED = prefix + "00209";

    @LogMessageInfo(
            message = "Context {0} disabled from virtual server {1}",
            level = "FINE")
    public static final String CONTEXT_DISABLED = prefix + "00210";

    @LogMessageInfo(
            message = "Virtual server {0}'s network listeners are updated from {1} to {2}",
            level = "FINE")
    public static final String VS_UPDATED_NETWORK_LISTENERS = prefix + "00211";

    @LogMessageInfo(
            message = "The class {0} is annotated with an invalid scope",
            level = "INFO")
    public static final String INVALID_ANNOTATION_SCOPE = prefix + "00212";

    @LogMessageInfo(
            message = "-DjvmRoute updated with {0}",
            level = "FINE")
    public static final String JVM_ROUTE_UPDATED= "AS-WEB-GLUE-00213";

    @LogMessageInfo(
            message = "Unable to parse port number {0} of network-listener {1}",
            level = "INFO")
    public static final String HTTP_LISTENER_INVALID_PORT = prefix + "00214";

    @LogMessageInfo(
            message = "Virtual server {0} set listener name {1}",
            level = "FINE")
    public static final String VIRTUAL_SERVER_SET_LISTENER_NAME = prefix + "00215";

    @LogMessageInfo(
            message = "Must not disable network-listener {0}, because it is associated with admin virtual server {1}",
            level = "INFO")
    public static final String MUST_NOT_DISABLE = prefix + "00216";

    @LogMessageInfo(
            message = "Virtual server {0} set jk listener name {1}",
            level = "FINE")
    public static final String VIRTUAL_SERVER_SET_JK_LISTENER_NAME = prefix + "00217";

    @LogMessageInfo(
            message = "virtual server {0} has an invalid docroot {1}",
            level = "INFO")
    public static final String VIRTUAL_SERVER_INVALID_DOCROOT = prefix + "00218";

    @LogMessageInfo(
            message = "{0} network listener is not included in {1} and will be updated ",
            level = "FINE")
    public static final String UPDATE_LISTENER = prefix + "00219";

    @LogMessageInfo(
            message = "Unable to load configuration of web module [{0}]",
            level = "WARNING")
    public static final String UNABLE_TO_LOAD_CONFIG = prefix + "00220";

    @LogMessageInfo(
            message = "Failed to precompile JSP pages of web module [{0}]",
            level = "SEVERE",
            cause = "An exception occurred precompiling JSP pages",
            action = "Check the exception for the error")
    public static final String JSPC_FAILED = prefix + "00221";

    @LogMessageInfo(
            message = "Unable to create custom ObjectInputStream",
            level = "SEVERE",
            cause = "An exception occurred during creating ObjectInputStream",
            action = "Check the Exception for error")
    public static final String CREATE_CUSTOM_OBJECT_INTPUT_STREAM_ERROR = prefix + "00222";

    @LogMessageInfo(
            message = "Unable to create custom ObjectOutputStream",
            level = "SEVERE",
            cause = "An exception occurred during creating ObjectOutputStream",
            action = "Check the Exception for error")
    public static final String CREATE_CUSTOM_BOJECT_OUTPUT_STREAM_ERROR = prefix + "00223";

    @LogMessageInfo(
            message = "The default-locale attribute of locale-charset-info element is being ignored",
            level = "WARNING")
    public static final String DEFAULT_LOCALE_DEPRECATED = prefix + "00224";


    @LogMessageInfo(
            message = "Web module [{0}] has a property with missing name or value",
            level = "WARNING")
    public static final String NULL_WEB_MODULE_PROPERTY = prefix + "00226";

    @LogMessageInfo(
            message = "Object of type {0} is not a valve",
            level = "WARNING")
    public static final String VALVE_CLASS_NAME_NO_VALVE = prefix + "00227";

    @LogMessageInfo(
            message = "Unable to add valve to web module {0}",
            level = "WARNING")
    public static final String VALVE_MISSING_NAME = prefix + "00228";

    @LogMessageInfo(
            message = "Unable to add valve with name {0} to web module {1}",
            level = "WARNING")
    public static final String VALVE_MISSING_CLASS_NAME = prefix + "00229";

    @LogMessageInfo(
            message = "No method {0}(java.lang.String) defined on valve {1} of web module {2}",
            level = "SEVERE",
            cause = "A matching method is not found",
            action = "Check the method name")
    public static final String VALVE_SPECIFIED_METHOD_MISSING = prefix + "00230";

    @LogMessageInfo(
            message = "Exception during execution of method {0} on valve {1} of web module {2}",
            level = "SEVERE",
            cause = "An exception occurred during method execution",
            action = "Check the Exception for error")
    public static final String VALVE_SETTER_CAUSED_EXCEPTION = prefix + "00231";

    @LogMessageInfo(
            message = "Valve {0} of web module {1} has a property without any name",
            level = "SEVERE",
            cause = "The valve is missing property name",
            action = "Check the property name")
    public static final String VALVE_MISSING_PROPERTY_NAME = prefix + "00232";

    @LogMessageInfo(
            message = "Unable to add listener of type {0} to web module {1}",
            level = "WARNING")
    public static final String INVALID_LISTENER = prefix + "00233";

    @LogMessageInfo(
            message = "Unable to load extension class {0} from web module {1}",
            level = "WARNING")
    public static final String UNABLE_TO_LOAD_EXTENSION = prefix + "00234";

    @LogMessageInfo(
            message = "Null property name or value for alternate docbase",
            level = "WARNING")
    public static final String ALTERNATE_DOC_BASE_NULL_PROPERTY_NAME_VALVE = prefix + "00235";

    @LogMessageInfo(
            message = "Alternate docbase property value {0} is missing a URL pattern or docbase",
            level = "WARNING")
    public static final String ALTERNATE_DOC_BASE_MISSING_PATH_OR_URL_PATTERN = prefix + "00236";

    @LogMessageInfo(
            message = "URL pattern {0} for alternate docbase is invalid",
            level = "WARNING")
    public static final String ALTERNATE_DOC_BASE_ILLEGAL_URL_PATTERN = prefix + "00237";

    @LogMessageInfo(
            message = "Failed to parse sun-web.xml singleThreadedServletPoolSize property value ({0}) of web module deployed at {1}, using default ({2})",
            level = "WARNING")
    public static final String INVALID_SERVLET_POOL_SIZE = prefix + "00238";

    @LogMessageInfo(
            message = "Enabled session ID reuse for web module {0} deployed on virtual server {1}",
            level = "WARNING")
    public static final String SESSION_IDS_REUSED = prefix + "00239";

    @LogMessageInfo(
            message = "Using alternate deployment descriptor {0} for web module {1}",
            level = "FINE")
    public static final String ALT_DD_NAME = prefix + "00240";

    @LogMessageInfo(
            message = "Ignoring invalid property {0} = {1}",
            level = "WARNING")
    public static final String INVALID_PROPERTY = prefix + "00241";

    @LogMessageInfo(
            message = "Unable to save sessions for web module {0} during redeployment",
            level = "WARNING")
    public static final String UNABLE_TO_SAVE_SESSIONS_DURING_REDEPLOY = prefix + "00242";

    @LogMessageInfo(
            message = "Unable to restore sessions for web module [{0}] from previous deployment",
            level = "WARNING")
    public static final String UNABLE_TO_RESTORE_SESSIONS_DURING_REDEPLOY = prefix + "00243";

    @LogMessageInfo(
            message = "Webservice based application, requires Metro to be installed. Run updatecenter client located in bin folder to install Metro",
            level = "WARNING")
    public static final String MISSING_METRO = prefix + "00244";

    @LogMessageInfo(
            message = "WebModule[{0}]: Setting delegate to {1}",
            level = "FINE")
    public static final String SETTING_DELEGATE = prefix + "00245";

    @LogMessageInfo(
            message = "WebModule[{0}]: Adding {1} to the classpath",
            level = "FINE")
    public static final String ADDING_CLASSPATH = prefix + "00246";

    @LogMessageInfo(
            message = "extra-class-path component {0} is not a valid pathname",
            level = "SEVERE",
            cause = "A MalformedURLException occurred",
            action = "Check the extra-class-path component")
    public static final String CLASSPATH_ERROR = prefix + "00247";

    @LogMessageInfo(
            message = "class-loader attribute dynamic-reload-interval in sun-web.xml not supported",
            level = "WARNING")
    public static final String DYNAMIC_RELOAD_INTERVAL = prefix + "00248";

    @LogMessageInfo(
            message = "IN WebContainer>>ConfigureSessionManager before builder factory FINAL_PERSISTENCE-TYPE IS = {0} FINAL_PERSISTENCE_FREQUENCY IS = {1} FINAL_PERSISTENCE_SCOPE IS = {2}",
            level = "FINEST")
    public static final String CONFIGURE_SESSION_MANAGER = prefix + "00249";

    @LogMessageInfo(
            message = "PersistenceStrategyBuilder class = {0}",
            level = "FINEST")
    public static final String PERSISTENCE_STRATEGY_BUILDER = prefix + "00250";

    @LogMessageInfo(
            message = "Property [{0}] is not yet supported",
            level = "INFO")
    public static final String PROP_NOT_YET_SUPPORTED = prefix + "00251";

    @LogMessageInfo(
            message = "WebModule[{0}] configure cookie properties {1}",
            level = "FINE")
    public static final String CONFIGURE_COOKIE_PROPERTIES = prefix + "00252";

    @LogMessageInfo(
            message = "Unable to add listener of type: {0}, because it does not implement any of the required ServletContextListener, ServletContextAttributeListener, ServletRequestListener, ServletRequestAttributeListener, HttpSessionListener, or HttpSessionAttributeListener interfaces",
            level = "WARNING")
    public static final String INVALID_LISTENER_TYPE = prefix + "00253";

    @LogMessageInfo(
            message = "Configured an authenticator for method {0}",
            level = "FINEST")
    public static final String AUTHENTICATOR_CONFIGURED = prefix + "00254";

    @LogMessageInfo(
            message = "[{0}] failed to unbind namespace",
            level = "WARNING")
    public static final String UNBIND_NAME_SPACE_ERROR = prefix + "00255";

    @LogMessageInfo(
            message = "No Realm with name [{0}] configured to authenticate against",
            level = "WARNING")
    public static final String MISSING_REALM = prefix + "00256";

    @LogMessageInfo(
            message = "Cannot configure an authenticator for method {0}",
            level = "WARNING")
    public static final String AUTHENTICATOR_MISSING = prefix + "00257";

    @LogMessageInfo(
            message = "Cannot instantiate an authenticator of class {0}",
            level = "WARNING")
    public static final String AUTHENTICATOR_INSTANTIATE_ERROR = prefix + "00258";

    @LogMessageInfo(
            message = "Lifecycle event data object [{0}] is not a WebModule",
            level = "WARNING")
    public static final String CLASS_CAST_EXCEPTION = prefix + "00259";

    @LogMessageInfo(
            message = "jsp-config property for {0} ",
            level = "FINE")
    public static final String JSP_CONFIG_PROPERTY = prefix + "00260";

    @LogMessageInfo(
            message = "sysClasspath for {0} ",
            level = "FINE")
    public static final String SYS_CLASSPATH = prefix + "00261";

    @LogMessageInfo(
            message = "Error creating cache manager and configuring the servlet caching subsystem",
            level = "WARNING")
    public static final String CACHE_MRG_EXCEPTION = prefix + "00262";

    @LogMessageInfo(
            message = "Cache Manager started",
            level = "FINE")
    public static final String CACHE_MANAGER_STARTED = prefix + "00263";

    @LogMessageInfo(
            message = "Cache Manager stopped",
            level = "FINE")
    public static final String CACHE_MANAGER_STOPPED = prefix + "00264";

    @LogMessageInfo(
            message = "*** InstanceEvent: {0}",
            level = "FINEST")
    public static final String INSTANCE_EVENT = prefix + "00265";

    @LogMessageInfo(
            message = "Obtained securityContext implementation class {0}",
            level = "FINE")
    public static final String SECURITY_CONTEXT_OBTAINED = prefix + "00266";

    @LogMessageInfo(
            message = "Failed to obtain securityContext implementation class",
            level = "FINE")
    public static final String SECURITY_CONTEXT_FAILED = prefix + "00267";

    @LogMessageInfo(
            message = "Exception during processing of event of type {0} for web module {1}",
            level = "SEVERE",
            cause = "An exception occurred during processing event type",
            action = "Check the exception for the error")
    public static final String EXCEPTION_DURING_HANDLE_EVENT = prefix + "00268";

    @LogMessageInfo(
            message = "No ServerContext in WebModule [{0}]",
            level = "WARNING")
    public static final String NO_SERVER_CONTEXT = prefix + "00269";

    @LogMessageInfo(
            message = "ContainerEvent: {0}",
            level = "FINEST")
    public static final String CONTAINER_EVENT = prefix + "00270";

    @LogMessageInfo(
            message = "Exception during invocation of InjectionManager.destroyManagedObject on {0} of web module {1}",
            level = "SEVERE",
            cause = "An exception occurred during destroyManagedObject",
            action = "Check the exception for the error")
    public static final String EXCEPTION_DURING_DESTROY_MANAGED_OBJECT = prefix + "00271";

    @LogMessageInfo(
            message = "Network Listener named {0} does not exist.  Creating or using the named protocol element instead.",
            level = "INFO")
    public static final String CREATE_SSL_HTTP_NOT_FOUND = prefix + "00272";

    @LogMessageInfo(
            message = "Network Listener named {0} to which this ssl element is being added already has an ssl element.",
            level = "INFO")
    public static final String CREATE_SSL_HTTP_ALREADY_EXISTS = prefix + "00273";

    @LogMessageInfo(
            message = "HTTP Listener named {0} not found",
            level = "INFO")
    public static final String DELETE_SSL_HTTP_LISTENER_NOT_FOUND = prefix + "00274";

    @LogMessageInfo(
            message = "Ssl element does not exist for Listener named {0}",
            level = "INFO")
    public static final String DELETE_SSL_ELEMENT_DOES_NOT_EXIST = prefix + "00275";

    @LogMessageInfo(
            message = "Error in parsing default-web.xml",
            level = "WARNING")
    public static final String ERROR_PARSING = prefix + "00276";

    @LogMessageInfo(
            message = "An authentication method was not defined in the web.xml descriptor. " +
                    "Using default BASIC for login configuration.",
            level = "WARNING")
    public static final String AUTH_METHOD_NOT_FOUND = prefix + "00277";

    @LogMessageInfo(
            message = "[{0}] is not a valid authentication method",
            level = "WARNING")
    public static final String EXCEPTION_AUTH_METHOD = prefix + "00278";

    @LogMessageInfo(
            message = "Invalid URL Pattern: [{0}]",
            level = "INFO")
    public static final String ENTERPRISE_DEPLOYMENT_INVALID_URL_PATTERN = prefix + "00279";

    @LogMessageInfo(
            message = "Cannot load class {0}",
            level = "FINER")
    public static final String CANNOT_LOAD_CLASS = prefix + "00280";

    @LogMessageInfo(
            message = "Beginning JSP Precompile...",
            level = "INFO")
    public static final String START_MESSAGE = prefix + "00281";

    @LogMessageInfo(
            message = "Finished JSP Precompile...",
            level = "INFO")
    public static final String FINISH_MESSAGE = prefix + "00282";

    @LogMessageInfo(
            message = "Cannot delete file: {0}",
            level = "FINE")
    public static final String CANNOT_DELETE_FILE = prefix + "00283";

    @LogMessageInfo(
        message = "Exception getting Validator Factory from JNDI: {0}",
        level = "WARNING")
    public static final String EXCEPTION_GETTING_VALIDATOR_FACTORY = prefix + "00285";
}
