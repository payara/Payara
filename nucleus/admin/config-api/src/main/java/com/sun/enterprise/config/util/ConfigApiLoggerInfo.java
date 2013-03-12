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
    public static final String LOGMSG_PREFIX = "NCLS-ADMIN";

    @LogMessagesResourceBundle
    public static final String SHARED_LOGMESSAGE_RESOURCE = "com.sun.enterprise.config.util.LogMessages";

    @LoggerInfo(subsystem = "CONFIG-API", description = "Configuration API", publish = true)
    public static final String ADMIN_LOGGER = "com.sun.enterprise.config";
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
    public static final String noDefaultConfigFound = LOGMSG_PREFIX + "-0001";

    @LogMessageInfo(
            message = "cluster property GMS_DISCOVERY_URI_LIST={0}",
            level = "FINE")
    public static final String clusterGSMBroadCast = LOGMSG_PREFIX + "-0002";

    @LogMessageInfo(
            message = "cluster attribute gms broadcast={0}",
            level = "FINE")
    public final static String clusterGSMDeliveryURI = LOGMSG_PREFIX + "-0003";

    @LogMessageInfo(
            message = "Cluster {0} contains server instances {1} and must not contain any instances",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String clusterMustNotContainInstance = LOGMSG_PREFIX + "-0004";

    @LogMessageInfo(
            message = "Unable to remove config {0}",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String deleteConfigFailed = LOGMSG_PREFIX + "-0005";
    @LogMessageInfo(
            message = "CopyConfig error caused by {0}",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "WARNING")
    public final static String copyConfigError = LOGMSG_PREFIX + "-0006";
    @LogMessageInfo(
            message = "Error when getting clusters on node dues to: {0}",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "WARNING")
    public final static String errorGettingCluster = LOGMSG_PREFIX + "-0007";
    @LogMessageInfo(
            message = "Error when getting servers due to: {0}",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "WARNING")
    public final static String errorGettingServers = LOGMSG_PREFIX + "-0008";
    @LogMessageInfo(
            message = "Unable to create default Http service configuration",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "INFO")
    public final static String unableToCreateHttpServiceConfig = LOGMSG_PREFIX + "-0009";
    @LogMessageInfo(
            message = "Cannot remove Node {0}.",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String cannotRemoveNode = LOGMSG_PREFIX + "-0010";
    @LogMessageInfo(
            message = "Node {0} referenced in server instance(s): {1}.  Remove instances before removing node.",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String referencedByInstance = LOGMSG_PREFIX + "-0011";
    @LogMessageInfo(
            message = "Can''t find the default config (an element named \"default-config\") "
                    + "in domain.xml.  You may specify the name of an existing config element next time.",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String noDefaultConfig = LOGMSG_PREFIX + "-0012";
    @LogMessageInfo(
            message = "Unable to remove server-ref {0} from cluster {1}",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String deleteServerRefFailed = LOGMSG_PREFIX + "-0013";
    @LogMessageInfo(
            message = "The default configuration template (named default-config) "
                    + "cannot be referenced by a server.",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "WARNING")
    public final static String configRefDefaultconfig = LOGMSG_PREFIX + "-0014";
    @LogMessageInfo(
            message = "The configuration of the Domain Administration Server "
                    + "cannot be changed from server-config.",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "WARNING")
    public final static String configRefDASconfig = LOGMSG_PREFIX + "-0015";
    @LogMessageInfo(
            message = "The configuration of the Domain Administration Server "
                    + "(named server-config) cannot be referenced by a server.",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "WARNING")
    public final static String configRefServerconfig = LOGMSG_PREFIX + "-0016";
    @LogMessageInfo(
            message = "The configuration of the Domain Administration Server "
                    + "(named server-config) cannot be referenced by a server.",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "WARNING")
    public final static String configRefClusteredInstance = LOGMSG_PREFIX + "-0017";
    @LogMessageInfo(
            message = "A configuration that doesn't exist cannot be referenced by a server.",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "WARNING")
    public final static String configRefNonexistent = LOGMSG_PREFIX + "-0018";


    @LogMessageInfo(
            message = "Port= {0}",
            publish = true,
            level = "FINER")
    public final static String portBaseHelperPort = LOGMSG_PREFIX + "-0019";

    @LogMessageInfo(
            message = "removing default instance index for {0}",

            publish = true,
            level = "FINE")
    public final static String removingDefaultInstanceIndexFor = LOGMSG_PREFIX + "-0020";

    @LogMessageInfo(
            message = "adding default instance index for {0}",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "FINE")
    public final static String AddingDefaultInstanceIndexFor = LOGMSG_PREFIX + "-0021";


    @LogMessageInfo(
            message = "Existing default-config detected during upgrade. No need to create default-config.",
            publish = true,
            level = "INFO")
    public final static String existingDefaultConfig = LOGMSG_PREFIX + "-0022";

    @LogMessageInfo(
            message = "System Property com.sun.aas.installRoot is null. We could be running in unit tests."
                    + "Exiting DefaultConfigUpgrade",
            publish = true,
            level = "INFO")
    public final static String installRootIsNull = LOGMSG_PREFIX + "-0023";

    @LogMessageInfo(
            message = "default-config not detected during upgrade. Running DefaultConfigUpgrade to create default-config.",
            publish = true,
            level = "INFO")
    public final static String runningDefaultConfigUpgrade = LOGMSG_PREFIX + "-0024";


    @LogMessageInfo(
            message = "Failure during upgrade - could not create default-config",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String defaultConfigUpgradeFailure = LOGMSG_PREFIX + "-0025";

    @LogMessageInfo(
            message = "Failure creating SecurityService Config",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String failureCreatingSecurityServiceConfig = LOGMSG_PREFIX + "-0027";

    @LogMessageInfo(
            message = "Problem parsing security-service",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String problemParsingSecurityServiceConfig = LOGMSG_PREFIX + "-0028";


    @LogMessageInfo(
            message = "Failed to create HttpService VirtualService config object",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String failureCreatingHttpServiceVS = LOGMSG_PREFIX + "-0029";

    @LogMessageInfo(
            message = "Problem parsing http-service virtual-server in domain.xml template",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String problemParsingHttpServiceVs = LOGMSG_PREFIX + "-0030";


    @LogMessageInfo(
            message = "Failed to create AdminService Property config object",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String failedToCreateAdminService = LOGMSG_PREFIX + "-0031";


    @LogMessageInfo(
            message = "Problem parsing asadmin-service property element in domain.xml template",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String problemParsingAdminService = LOGMSG_PREFIX + "-0032";


    @LogMessageInfo(
            message = "Failure creating LogService config object",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "WARNING")
    public final static String failureCreatingLogService = LOGMSG_PREFIX + "-0033";


    @LogMessageInfo(
            message = "Failure creating ModuleLogLevel config object",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String failureCreateModuleLogLevel = LOGMSG_PREFIX + "-0034";

    @LogMessageInfo(
            message = "Problem parsing module-log-levels in domain.xml template",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String problemParsingModuleLogLevel = LOGMSG_PREFIX + "-0035";


    @LogMessageInfo(
            message = "Failure creating SecurityService config object",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String failureCreatingSecurityService = LOGMSG_PREFIX + "-0036";

    @LogMessageInfo(
            message = "Failure creating AuthRealm",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String failureCreatingAuthRealm = LOGMSG_PREFIX + "-0037";

    @LogMessageInfo(
            message = "Problem parsing auth-realm",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String failureParsingAuthRealm = LOGMSG_PREFIX + "-0038";

    @LogMessageInfo(
            message = "Create AuthRealm Property failed. Attr = {0} and Val = {1}",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String failureCreatingAuthRealmProperty = LOGMSG_PREFIX + "-0039";

    @LogMessageInfo(
            message = "Problem parsing auth-realm property",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String failureParsingAuthRealmProperty = LOGMSG_PREFIX + "-0040";


    @LogMessageInfo(
            message = "Failure creating JaccProvider",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String failureCreatingJaccProvider = LOGMSG_PREFIX + "-0041";


    @LogMessageInfo(
            message = "Problem parsing jacc-provider",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String problemParsingJaacProvider = LOGMSG_PREFIX + "-0042";


    @LogMessageInfo(
            message = "Create JaccProvider Property failed. Attr = {0} and Val = {1}",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String failureCreatingJaccProviderAttr = LOGMSG_PREFIX + "-0043";


    @LogMessageInfo(
            message = "Problem parsing jacc-provider property",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String problemParsingJaacProviderAttr = LOGMSG_PREFIX + "-0044";

    @LogMessageInfo(
            message = "Failure creating AuditModule config object",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String failureCreatingAuditModule = LOGMSG_PREFIX + "-0045";


    @LogMessageInfo(
            message = "Create AuditModule Property failed.",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String failureCreatingAuditModuleAttr = LOGMSG_PREFIX + "-0046";


    @LogMessageInfo(
            message = "Problem parsing audit-module property",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String failureParsingAuditModuleProp = LOGMSG_PREFIX + "-0047";

    @LogMessageInfo(
            message = "Failure creating ProviderConfig",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String failureCreatingProviderConfig = LOGMSG_PREFIX + "-0048";


    @LogMessageInfo(
            message = "Problem parsing provider-config",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String ProblemParsingProviderConfig = LOGMSG_PREFIX + "-0049";


    @LogMessageInfo(
            message = "Create ProviderConfig RequestPolicy failed.",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String createProviderConfigRequestPolicyFailed = LOGMSG_PREFIX + "-0050";


    @LogMessageInfo(
            message = "Problem parsing request-policy property",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String problemParsingRequestPolicyProp = LOGMSG_PREFIX + "-0051";

    @LogMessageInfo(
            message = "Create ProviderConfig Property failed",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String createProviderConfigPropertyFailed = LOGMSG_PREFIX + "-0052";

    @LogMessageInfo(
            message = "Problem parsing provider-config property",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String problemParsingProviderConfigProp = LOGMSG_PREFIX + "-0053";


    @LogMessageInfo(
            message = "Failure creating JavaConfig config object",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String failureCreatingJavaConfigObject = LOGMSG_PREFIX + "-0054";


    @LogMessageInfo(
            message = "Problem parsing jvm-options",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String problemParsingJvmOptions = LOGMSG_PREFIX + "-0055";


    @LogMessageInfo(
            message = "Failure creating AvailabilityService config object",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String failureCreatingAvailabilityServiceConfig = LOGMSG_PREFIX + "-0056";


    @LogMessageInfo(
            message = "Failure creating NetworkConfig config object",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String failureCreatingNetworkConfig = LOGMSG_PREFIX + "-0057";


    @LogMessageInfo(
            message = "Failure creating Protocols config object",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String failureCreatingProtocolsConfig = LOGMSG_PREFIX + "-0058";


    @LogMessageInfo(
            message = "Problem parsing protocols element in domain.xml template",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String problemParsingProtocolsConfig = LOGMSG_PREFIX + "-0059";


    @LogMessageInfo(
            message = "Failure creating Protocol config object",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String failureCreatingProtocolConfig = LOGMSG_PREFIX + "-0060";


    @LogMessageInfo(
            message = "Problem parsing protocol element in domain.xml template",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String problemParsingProtocolElement = LOGMSG_PREFIX + "-0091";


    @LogMessageInfo(
            message = "Failure creating Http config object",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String failureCreatingHttpConfig = LOGMSG_PREFIX + "-0061";


    @LogMessageInfo(
            message = "Failure creating FileCache config object",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String failureCreatingFileCacheConfig = LOGMSG_PREFIX + "-0062";


    @LogMessageInfo(
            message = "Problem parsing file-cache element in domain.xml template",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String problemParsingFileCacheElement = LOGMSG_PREFIX + "-0063";


    @LogMessageInfo(
            message = "Failure creating Ssl config object",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String failureCreatingSSLConfig = LOGMSG_PREFIX + "-0064";


    @LogMessageInfo(
            message = "Problem parsing ssl element in domain.xml template",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String problemParsingSSlElement = LOGMSG_PREFIX + "-0065";


    @LogMessageInfo(
            message = "Failure creating HttpRedirect config object",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String failureCreatingHttpRedirect = LOGMSG_PREFIX + "-0066";


    @LogMessageInfo(
            message = "Failure creating PortUnification config object",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String failureCreatingPortUnification = LOGMSG_PREFIX + "-0067";


    @LogMessageInfo(
            message = "Failure creating ProtocolFinder config object",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String failureCreatingProtocolFinder = LOGMSG_PREFIX + "-0068";

    @LogMessageInfo(
            message = "Problem parsing protocol-finder element in domain.xml template",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String problemParsingProtocolFinder = LOGMSG_PREFIX + "-0069";

    @LogMessageInfo(
            message = "Failure creating NetworkListeners config object",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String failureCreatingNetworkListeners = LOGMSG_PREFIX + "-0070";

    @LogMessageInfo(
            message = "Problem parsing network-listeners element in domain.xml template",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String problemParsingNetworkListeners = LOGMSG_PREFIX + "-0071";

    @LogMessageInfo(
            message = "Failure creating NetworkListener config object",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String failureCreatingNetworkListener = LOGMSG_PREFIX + "-0072";

    @LogMessageInfo(
            message = "Problem parsing network-listener element in domain.xml template",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String ProblemParsingNetworkListener = LOGMSG_PREFIX + "-0073";

    @LogMessageInfo(
            message = "Failure creating Transports config object",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String failureCreatingTransportsConfig = LOGMSG_PREFIX + "-0074";

    @LogMessageInfo(
            message = "Problem parsing transports element in domain.xml template",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String failureParsingTransportsConfig = LOGMSG_PREFIX + "-0075";

    @LogMessageInfo(
            message = "Failure creating Transport config object",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String failureCreatingTransportConfig = LOGMSG_PREFIX + "-0076";

    @LogMessageInfo(
            message = "Problem parsing transport element in domain.xml template",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String problemParsingTransportConfig = LOGMSG_PREFIX + "-0077";

    @LogMessageInfo(
            message = "Failure to create ThreadPools config object",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String failureToCreateThreadPoolsObject = LOGMSG_PREFIX + "-0078";

    @LogMessageInfo(
            message = "Failure creating ThreadPool config object",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String failureToCreateThreadpoolObject = LOGMSG_PREFIX + "-0079";

    @LogMessageInfo(
            message = "Problem parsing thread-pool element in domain.xml template",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String problemParsingThreadPoolElement = LOGMSG_PREFIX + "-0080";

    @LogMessageInfo(
            message = "Failure creating SystemProperty config object",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String failureCreatingSystemProperty = LOGMSG_PREFIX + "-0081";

    @LogMessageInfo(
            message = "Problem parsing system-property element in domain.xml template",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String problemParsingSystemProperty = LOGMSG_PREFIX + "-0082";


    @LogMessageInfo(
            message = "Startup class : ",

            level = "FINE")
    public final static String startupClass = LOGMSG_PREFIX + "-0083";


    @LogMessageInfo(
            message = "Successful cleaned domain.xml with ",
            level = "FINE")
    public final static String successfulCleanupWith = LOGMSG_PREFIX + "-0084";


    @LogMessageInfo(
            message = " cleaning domain.xml failed ",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String cleaningDomainXmlFailed = LOGMSG_PREFIX + "-0085";


    @LogMessageInfo(
            message = "Instance {0} from environment not found in domain.xml",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String badEnv = LOGMSG_PREFIX + "-0086";


    @LogMessageInfo(
            message = "Successful Upgrade domain.xml with ",

            publish = true,
            level = "FINE")
    public final static String successfulUpgrade = LOGMSG_PREFIX + "-0087";


    @LogMessageInfo(
            message = " upgrading domain.xml failed ",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String failedUpgrade = LOGMSG_PREFIX + "-0088";


    @LogMessageInfo(
            message = " does not exist or is empty, will use backup",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String noConfigFile = LOGMSG_PREFIX + "-0089";


    @LogMessageInfo(
            message = "Problem parsing system-property element in domain.xml template",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String noBackupFile = LOGMSG_PREFIX + "-0090";


    @LogMessageInfo(
            message = "Total time to parse domain.xml: ",
            level = "FINE")
    public final static String totalTimeToParseDomain = LOGMSG_PREFIX + "-0092";

    @LogMessageInfo(
            message = "Exception while creating the command model for the generic command {0} : {1}",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String commandModelException = LOGMSG_PREFIX + "-0093";

    @LogMessageInfo(
            message = "The CrudResolver {0} could not find the configuration object of type {1} where instances of {2} should be added",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String targetObjectNotFound = LOGMSG_PREFIX + "-0094";

    @LogMessageInfo(
            message = "A {0} instance with a {1} name already exist in the configuration",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String alreadyExistingInstance = LOGMSG_PREFIX + "-0095";

    @LogMessageInfo(
            message = "Exception while invoking {0} method : {1}",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String methodInvocationException = LOGMSG_PREFIX + "-0096";

    @LogMessageInfo(
            message = "The CreationDecorator {0} could not be found in the habitat, is it annotated with @Service?",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String decoratorNotFound = LOGMSG_PREFIX + "-0097";


    @LogMessageInfo(
            message = "Exception while adding the new configuration : {0}",
            cause = "unknown",
            action = "unknown",
            publish = true,
            level = "SEVERE")
    public final static String transactionException = LOGMSG_PREFIX + "-0098";
    //Remaining packages: The GenericCrudCommand and below in support package
    //Entire modularity package
}