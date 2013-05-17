/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.config.util;

import org.glassfish.logging.annotation.LogMessageInfo;
import org.glassfish.logging.annotation.LogMessagesResourceBundle;
import org.glassfish.logging.annotation.LoggerInfo;

import java.text.MessageFormat;
import java.util.logging.Logger;

/**
 * Logger information for the config-api module.
 *
 * @author Masoud Kalali
 */
/* Module private */
public class ConfigApiLoggerInfo {
    public static final String LOGMSG_PREFIX = "CONFIG-API";

    @LogMessagesResourceBundle
    public static final String SHARED_LOGMESSAGE_RESOURCE = "com.sun.enterprise.config.util.LogMessages";

    @LoggerInfo(subsystem = "CONFIG-API", description = "Configuration API", publish = true)
    public static final String ADMIN_LOGGER = "javax.enterprise.config.api";
    private static final Logger adminLogger = Logger.getLogger(
            ADMIN_LOGGER, SHARED_LOGMESSAGE_RESOURCE);

    public static Logger getLogger() {
        return adminLogger;
    }

    public static String getString(String message, Object[] parameters) {
        return MessageFormat.format(message, parameters);
    }

    @LogMessageInfo(
            message = "No default config found, using config {0} as the default config for the cluster {1}",
            cause = "unknown",
            action = "unknown",
            level = "WARNING")
    public static final String noDefaultConfigFound = LOGMSG_PREFIX + "-00001";

    @LogMessageInfo(
            message = "cluster property GMS_DISCOVERY_URI_LIST={0}",
            level = "FINE")
    public static final String clusterGSMBroadCast = LOGMSG_PREFIX + "-00002";

    @LogMessageInfo(
            message = "cluster attribute gms broadcast={0}",
            level = "FINE")
    public final static String clusterGSMDeliveryURI = LOGMSG_PREFIX + "-00003";

    @LogMessageInfo(
            message = "Cluster {0} contains server instances {1} and must not contain any instances",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String clusterMustNotContainInstance = LOGMSG_PREFIX + "-00004";

    @LogMessageInfo(
            message = "Unable to remove config {0}",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String deleteConfigFailed = LOGMSG_PREFIX + "-00005";
    @LogMessageInfo(
            message = "CopyConfig error caused by {0}",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "WARNING")
    public final static String copyConfigError = LOGMSG_PREFIX + "-00006";
    @LogMessageInfo(
            message = "Error when getting clusters on node dues to: {0}",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "WARNING")
    public final static String errorGettingCluster = LOGMSG_PREFIX + "-00007";
    @LogMessageInfo(
            message = "Error when getting servers due to: {0}",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "WARNING")
    public final static String errorGettingServers = LOGMSG_PREFIX + "-00008";
    @LogMessageInfo(
            message = "Unable to create default Http service configuration",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "INFO")
    public final static String unableToCreateHttpServiceConfig = LOGMSG_PREFIX + "-00009";
    @LogMessageInfo(
            message = "Cannot remove Node {0}.",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String cannotRemoveNode = LOGMSG_PREFIX + "-00010";
    @LogMessageInfo(
            message = "Node {0} referenced in server instance(s): {1}.  Remove instances before removing node.",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String referencedByInstance = LOGMSG_PREFIX + "-00011";
    @LogMessageInfo(
            message = "Can''t find the default config (an element named \"default-config\") "
                    + "in domain.xml.  You may specify the name of an existing config element next time.",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String noDefaultConfig = LOGMSG_PREFIX + "-00012";
    @LogMessageInfo(
            message = "Unable to remove server-ref {0} from cluster {1}",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String deleteServerRefFailed = LOGMSG_PREFIX + "-00013";
    @LogMessageInfo(
            message = "The default configuration template (named default-config) "
                    + "cannot be referenced by a server.",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "WARNING")
    public final static String configRefDefaultconfig = LOGMSG_PREFIX + "-00014";
    @LogMessageInfo(
            message = "The configuration of the Domain Administration Server "
                    + "cannot be changed from server-config.",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "WARNING")
    public final static String configRefDASconfig = LOGMSG_PREFIX + "-00015";
    @LogMessageInfo(
            message = "The configuration of the Domain Administration Server "
                    + "(named server-config) cannot be referenced by a server.",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "WARNING")
    public final static String configRefServerconfig = LOGMSG_PREFIX + "-00016";
    @LogMessageInfo(
            message = "The configuration of the Domain Administration Server "
                    + "(named server-config) cannot be referenced by a server.",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "WARNING")
    public final static String configRefClusteredInstance = LOGMSG_PREFIX + "-00017";
    @LogMessageInfo(
            message = "A configuration that doesn't exist cannot be referenced by a server.",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "WARNING")
    public final static String configRefNonexistent = LOGMSG_PREFIX + "-00018";


    @LogMessageInfo(
            message = "Port= {0}",
            publish = true,
            level = "FINER")
    public final static String portBaseHelperPort = LOGMSG_PREFIX + "-00019";

    @LogMessageInfo(
            message = "removing default instance index for {0}",

            publish = true,
            level = "FINE")
    public final static String removingDefaultInstanceIndexFor = LOGMSG_PREFIX + "-00020";

    @LogMessageInfo(
            message = "adding default instance index for {0}",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "FINE")
    public final static String AddingDefaultInstanceIndexFor = LOGMSG_PREFIX + "-00021";


    @LogMessageInfo(
            message = "Existing default-config detected during upgrade. No need to create default-config.",
            publish = true,
            level = "INFO")
    public final static String existingDefaultConfig = LOGMSG_PREFIX + "-00022";

    @LogMessageInfo(
            message = "System Property com.sun.aas.installRoot is null. We could be running in unit tests."
                    + "Exiting DefaultConfigUpgrade",
            publish = true,
            level = "INFO")
    public final static String installRootIsNull = LOGMSG_PREFIX + "-00023";

    @LogMessageInfo(
            message = "default-config not detected during upgrade. Running DefaultConfigUpgrade to create default-config.",
            publish = true,
            level = "INFO")
    public final static String runningDefaultConfigUpgrade = LOGMSG_PREFIX + "-00024";


    @LogMessageInfo(
            message = "Failure during upgrade - could not create default-config",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String defaultConfigUpgradeFailure = LOGMSG_PREFIX + "-00025";

    @LogMessageInfo(
            message = "Failure creating SecurityService Config",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String failureCreatingSecurityServiceConfig = LOGMSG_PREFIX + "-00027";

    @LogMessageInfo(
            message = "Problem parsing security-service",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String problemParsingSecurityServiceConfig = LOGMSG_PREFIX + "-00028";


    @LogMessageInfo(
            message = "Failed to create HttpService VirtualService config object",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String failureCreatingHttpServiceVS = LOGMSG_PREFIX + "-00029";

    @LogMessageInfo(
            message = "Problem parsing http-service virtual-server in domain.xml template",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String problemParsingHttpServiceVs = LOGMSG_PREFIX + "-00030";


    @LogMessageInfo(
            message = "Failed to create AdminService Property config object",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String failedToCreateAdminService = LOGMSG_PREFIX + "-00031";


    @LogMessageInfo(
            message = "Problem parsing asadmin-service property element in domain.xml template",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String problemParsingAdminService = LOGMSG_PREFIX + "-00032";


    @LogMessageInfo(
            message = "Failure creating LogService config object",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "WARNING")
    public final static String failureCreatingLogService = LOGMSG_PREFIX + "-00033";


    @LogMessageInfo(
            message = "Failure creating ModuleLogLevel config object",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String failureCreateModuleLogLevel = LOGMSG_PREFIX + "-00034";

    @LogMessageInfo(
            message = "Problem parsing module-log-levels in domain.xml template",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String problemParsingModuleLogLevel = LOGMSG_PREFIX + "-00035";


    @LogMessageInfo(
            message = "Failure creating SecurityService config object",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String failureCreatingSecurityService = LOGMSG_PREFIX + "-00036";

    @LogMessageInfo(
            message = "Failure creating AuthRealm",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String failureCreatingAuthRealm = LOGMSG_PREFIX + "-00037";

    @LogMessageInfo(
            message = "Problem parsing auth-realm",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String failureParsingAuthRealm = LOGMSG_PREFIX + "-00038";

    @LogMessageInfo(
            message = "Create AuthRealm Property failed. Attr = {0} and Val = {1}",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String failureCreatingAuthRealmProperty = LOGMSG_PREFIX + "-00039";

    @LogMessageInfo(
            message = "Problem parsing auth-realm property",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String failureParsingAuthRealmProperty = LOGMSG_PREFIX + "-00040";


    @LogMessageInfo(
            message = "Failure creating JaccProvider",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String failureCreatingJaccProvider = LOGMSG_PREFIX + "-00041";


    @LogMessageInfo(
            message = "Problem parsing jacc-provider",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String problemParsingJaacProvider = LOGMSG_PREFIX + "-00042";


    @LogMessageInfo(
            message = "Create JaccProvider Property failed. Attr = {0} and Val = {1}",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String failureCreatingJaccProviderAttr = LOGMSG_PREFIX + "-00043";


    @LogMessageInfo(
            message = "Problem parsing jacc-provider property",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String problemParsingJaacProviderAttr = LOGMSG_PREFIX + "-00044";

    @LogMessageInfo(
            message = "Failure creating AuditModule config object",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String failureCreatingAuditModule = LOGMSG_PREFIX + "-00045";


    @LogMessageInfo(
            message = "Create AuditModule Property failed.",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String failureCreatingAuditModuleAttr = LOGMSG_PREFIX + "-00046";


    @LogMessageInfo(
            message = "Problem parsing audit-module property",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String failureParsingAuditModuleProp = LOGMSG_PREFIX + "-00047";

    @LogMessageInfo(
            message = "Failure creating ProviderConfig",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String failureCreatingProviderConfig = LOGMSG_PREFIX + "-00048";


    @LogMessageInfo(
            message = "Problem parsing provider-config",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String ProblemParsingProviderConfig = LOGMSG_PREFIX + "-00049";


    @LogMessageInfo(
            message = "Create ProviderConfig RequestPolicy failed.",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String createProviderConfigRequestPolicyFailed = LOGMSG_PREFIX + "-00050";


    @LogMessageInfo(
            message = "Problem parsing request-policy property",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String problemParsingRequestPolicyProp = LOGMSG_PREFIX + "-00051";

    @LogMessageInfo(
            message = "Create ProviderConfig Property failed",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String createProviderConfigPropertyFailed = LOGMSG_PREFIX + "-00052";

    @LogMessageInfo(
            message = "Problem parsing provider-config property",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String problemParsingProviderConfigProp = LOGMSG_PREFIX + "-00053";


    @LogMessageInfo(
            message = "Failure creating JavaConfig config object",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String failureCreatingJavaConfigObject = LOGMSG_PREFIX + "-00054";


    @LogMessageInfo(
            message = "Problem parsing jvm-options",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String problemParsingJvmOptions = LOGMSG_PREFIX + "-00055";


    @LogMessageInfo(
            message = "Failure creating AvailabilityService config object",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String failureCreatingAvailabilityServiceConfig = LOGMSG_PREFIX + "-00056";


    @LogMessageInfo(
            message = "Failure creating NetworkConfig config object",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String failureCreatingNetworkConfig = LOGMSG_PREFIX + "-00057";


    @LogMessageInfo(
            message = "Failure creating Protocols config object",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String failureCreatingProtocolsConfig = LOGMSG_PREFIX + "-00058";


    @LogMessageInfo(
            message = "Problem parsing protocols element in domain.xml template",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String problemParsingProtocolsConfig = LOGMSG_PREFIX + "-00059";


    @LogMessageInfo(
            message = "Failure creating Protocol config object",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String failureCreatingProtocolConfig = LOGMSG_PREFIX + "-00060";


    @LogMessageInfo(
            message = "Problem parsing protocol element in domain.xml template",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String problemParsingProtocolElement = LOGMSG_PREFIX + "-00091";


    @LogMessageInfo(
            message = "Failure creating Http config object",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String failureCreatingHttpConfig = LOGMSG_PREFIX + "-00061";


    @LogMessageInfo(
            message = "Failure creating FileCache config object",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String failureCreatingFileCacheConfig = LOGMSG_PREFIX + "-00062";


    @LogMessageInfo(
            message = "Problem parsing file-cache element in domain.xml template",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String problemParsingFileCacheElement = LOGMSG_PREFIX + "-00063";


    @LogMessageInfo(
            message = "Failure creating Ssl config object",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String failureCreatingSSLConfig = LOGMSG_PREFIX + "-00064";


    @LogMessageInfo(
            message = "Problem parsing ssl element in domain.xml template",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String problemParsingSSlElement = LOGMSG_PREFIX + "-00065";


    @LogMessageInfo(
            message = "Failure creating HttpRedirect config object",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String failureCreatingHttpRedirect = LOGMSG_PREFIX + "-00066";


    @LogMessageInfo(
            message = "Failure creating PortUnification config object",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String failureCreatingPortUnification = LOGMSG_PREFIX + "-00067";


    @LogMessageInfo(
            message = "Failure creating ProtocolFinder config object",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String failureCreatingProtocolFinder = LOGMSG_PREFIX + "-00068";

    @LogMessageInfo(
            message = "Problem parsing protocol-finder element in domain.xml template",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String problemParsingProtocolFinder = LOGMSG_PREFIX + "-00069";

    @LogMessageInfo(
            message = "Failure creating NetworkListeners config object",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String failureCreatingNetworkListeners = LOGMSG_PREFIX + "-00070";

    @LogMessageInfo(
            message = "Problem parsing network-listeners element in domain.xml template",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String problemParsingNetworkListeners = LOGMSG_PREFIX + "-00071";

    @LogMessageInfo(
            message = "Failure creating NetworkListener config object",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String failureCreatingNetworkListener = LOGMSG_PREFIX + "-00072";

    @LogMessageInfo(
            message = "Problem parsing network-listener element in domain.xml template",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String ProblemParsingNetworkListener = LOGMSG_PREFIX + "-00073";

    @LogMessageInfo(
            message = "Failure creating Transports config object",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String failureCreatingTransportsConfig = LOGMSG_PREFIX + "-00074";

    @LogMessageInfo(
            message = "Problem parsing transports element in domain.xml template",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String failureParsingTransportsConfig = LOGMSG_PREFIX + "-00075";

    @LogMessageInfo(
            message = "Failure creating Transport config object",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String failureCreatingTransportConfig = LOGMSG_PREFIX + "-00076";

    @LogMessageInfo(
            message = "Problem parsing transport element in domain.xml template",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String problemParsingTransportConfig = LOGMSG_PREFIX + "-00077";

    @LogMessageInfo(
            message = "Failure to create ThreadPools config object",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String failureToCreateThreadPoolsObject = LOGMSG_PREFIX + "-00078";

    @LogMessageInfo(
            message = "Failure creating ThreadPool config object",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String failureToCreateThreadpoolObject = LOGMSG_PREFIX + "-00079";

    @LogMessageInfo(
            message = "Problem parsing thread-pool element in domain.xml template",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String problemParsingThreadPoolElement = LOGMSG_PREFIX + "-00080";

    @LogMessageInfo(
            message = "Failure creating SystemProperty config object",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String failureCreatingSystemProperty = LOGMSG_PREFIX + "-00081";

    @LogMessageInfo(
            message = "Problem parsing system-property element in domain.xml template",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String problemParsingSystemProperty = LOGMSG_PREFIX + "-00082";


    @LogMessageInfo(
            message = "Startup class : ",

            level = "FINE")
    public final static String startupClass = LOGMSG_PREFIX + "-00083";


    @LogMessageInfo(
            message = "Successful cleaned domain.xml with ",
            level = "FINE")
    public final static String successfulCleanupWith = LOGMSG_PREFIX + "-00084";


    @LogMessageInfo(
            message = " cleaning domain.xml failed ",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String cleaningDomainXmlFailed = LOGMSG_PREFIX + "-00085";


    @LogMessageInfo(
            message = "Instance {0} from environment not found in domain.xml",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String badEnv = LOGMSG_PREFIX + "-00086";


    @LogMessageInfo(
            message = "Successful Upgrade domain.xml with ",

            publish = true,
            level = "FINE")
    public final static String successfulUpgrade = LOGMSG_PREFIX + "-00087";


    @LogMessageInfo(
            message = " upgrading domain.xml failed ",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String failedUpgrade = LOGMSG_PREFIX + "-00088";


    @LogMessageInfo(
            message = " does not exist or is empty, will use backup",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String noConfigFile = LOGMSG_PREFIX + "-00089";


    @LogMessageInfo(
            message = "Problem parsing system-property element in domain.xml template",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String noBackupFile = LOGMSG_PREFIX + "-00090";


    @LogMessageInfo(
            message = "Total time to parse domain.xml: ",
            level = "FINE")
    public final static String totalTimeToParseDomain = LOGMSG_PREFIX + "-00092";

    @LogMessageInfo(
            message = "Exception while creating the command model for the generic command {0} : {1}",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String commandModelException = LOGMSG_PREFIX + "-00093";

    @LogMessageInfo(
            message = "The CrudResolver {0} could not find the configuration object of type {1} where instances of {2} should be added",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String targetObjectNotFound = LOGMSG_PREFIX + "-00094";

    @LogMessageInfo(
            message = "A {0} instance with a {1} name already exist in the configuration",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String alreadyExistingInstance = LOGMSG_PREFIX + "-00095";

    @LogMessageInfo(
            message = "Exception while invoking {0} method : {1}",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String methodInvocationException = LOGMSG_PREFIX + "-00096";

    @LogMessageInfo(
            message = "The CreationDecorator {0} could not be found in the habitat, is it annotated with @Service?",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String decoratorNotFound = LOGMSG_PREFIX + "-00097";


    @LogMessageInfo(
            message = "Exception while adding the new configuration : {0}",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String transactionException = LOGMSG_PREFIX + "-00098";
    //Remaining packages: The GenericCrudCommand and below in support package
    //Entire modularity package
}
