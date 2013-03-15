/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.admin.amx.util;

import java.util.logging.Logger;
import org.glassfish.logging.annotation.LogMessageInfo;
import org.glassfish.logging.annotation.LogMessagesResourceBundle;
import org.glassfish.logging.annotation.LoggerInfo;

/**
 * Logger information for the amx-core module.
 * @author Tom Mueller
 */
/* Module private */
public class AMXLoggerInfo {
    public static final String LOGMSG_PREFIX = "NCLS-COM";
    
    @LogMessagesResourceBundle
    public static final String SHARED_LOGMESSAGE_RESOURCE = "org.glassfish.admin.amx.util.LogMessages";
    
    @LoggerInfo(subsystem = "COMMON", description = "AMX Services", publish = true)
    public static final String AMX_LOGGER = "javax.enterprise.system.tools.amx";
    private static final Logger amxLogger = Logger.getLogger(
                AMX_LOGGER, SHARED_LOGMESSAGE_RESOURCE);

    public static Logger getLogger() {
        return amxLogger;
    }
    
    @LogMessageInfo(
            message = "AMX Startup Service Shutdown. MBeans have not been unregistered: {0}",
            level = "WARNING")
    public static final String shutdownNotUnregistered = LOGMSG_PREFIX + "-00002";

    @LogMessageInfo(
            message = "Fatal error loading AMX {0}",
            level = "INFO")
    public static final String fatalError = LOGMSG_PREFIX + "-00004";

    @LogMessageInfo(
            message = "AMX Startup Service: AMXLoader failed to load {0}",
            level = "INFO")
    public static final String failToLoad = LOGMSG_PREFIX + "-00008";

    @LogMessageInfo(
            message = "AMX Startup Service: AMX ready for use, DomainRoot {0}",
            level = "INFO")
    public static final String startupServiceDomainRoot = LOGMSG_PREFIX + "-00009";

    @LogMessageInfo(
            message = "AMXLoader failed to unload: {0}",
            level = "INFO")
    public static final String failToUnLoad = LOGMSG_PREFIX + "-00010";

    @LogMessageInfo(
            message = "ConfigBean not processed, something wrong with it {0}",
            level = "INFO")
    public static final String configBeanNotProcessed = LOGMSG_PREFIX + "-00011";

    @LogMessageInfo(
            message = "In AMXConfigLoader : Loading {0}",
            level = "INFO")
    public static final String inAMXConfigLoader = LOGMSG_PREFIX + "-00012";

    @LogMessageInfo(
            message = "Can't unregister MBean: {0}",
            level = "INFO")
    public static final String unregisterMbean = LOGMSG_PREFIX + "-00019";

    @LogMessageInfo(
            message = "Non-singleton ConfigBean {0} has empty key value (name), supplying {1}",
            level = "WARNING")
    public static final String nonsingletonConfigbean = LOGMSG_PREFIX + "-00020";

    @LogMessageInfo(
            message = "AMX ComplianceMonitor: ValidationLevel = {0}, UnregisterNonCompliant = {1}, LogInaccessibleAttributes = {2}",
            level = "INFO")
    public static final String aMXComplianceMonitorLevel = LOGMSG_PREFIX + "-00021";

    @LogMessageInfo(
            message = "AMX ComplianceMonitor thread has unexpectedly quit {0}",
            cause = "A JMX validation thread has unexpectedly terminated due to an exception.",
            action = "Check the server logs and contact Oracle support.",
            level = "SEVERE")
    public static final String aMXComplianceMonitorThreadquit = LOGMSG_PREFIX + "-00022";

    @LogMessageInfo(
            message = "Validating MBean {0}",
            level = "INFO")
    public static final String validatingMbean = LOGMSG_PREFIX + "-00023";

    @LogMessageInfo(
            message = "Exception validating MBean {0}",
            level = "WARNING")
    public static final String exceptionValidatingMbean = LOGMSG_PREFIX + "-00024";

    @LogMessageInfo(
            message = "Register children for instance name {0}",
            level = "INFO")
    public static final String registerChild = LOGMSG_PREFIX + "-00025";

    @LogMessageInfo(
            message = "AMX Attribute Change Notification for {0}",
            level = "INFO")
    public static final String attributeChangeNotification = LOGMSG_PREFIX + "-00027";

    @LogMessageInfo(
            message = "Attribute {0} not found for object {1}",
            cause = "An attempt to resolve an attribute failed.",
            action = "Check the server logs and contact Oracle support.",
            level = "SEVERE")
    public static final String attributeNotfound = LOGMSG_PREFIX + "-00030";

    @LogMessageInfo(
            message = "Can't find child of type {0}",
            cause = "While removing a child, the child was not found.",
            action = "Check the server logs and contact Oracle support.",
            level = "SEVERE")
    public static final String childNotfound = LOGMSG_PREFIX + "-00031";

    @LogMessageInfo(
            message = "MBeans exist in AMX domain prior to DomainRoot (violates Parent requirement): {0}",
            level = "INFO")
    public static final String mbeanExist = LOGMSG_PREFIX + "-00032";  
    
    @LogMessageInfo(
            message = "Can't register config MBean: type={0}, name={1}, exception={2}",
            level = "WARNING")
    public static final String cantRegister = LOGMSG_PREFIX + "-00033";  
    
    @LogMessageInfo(
            message = "Unexpected thread death of AMXConfigLoaderThread",
            cause = "The AMX configuration loader thread received an unexpected exception.",
            action = "Check the server logs and contact Oracle support.",
            level = "SEVERE")
    public static final String unexpectedDeath = LOGMSG_PREFIX + "-00034";  
    
    @LogMessageInfo(
            message = "Can't create children",
            level = "INFO")
    public static final String cantCreateChildren = LOGMSG_PREFIX + "-00035";  
    
    @LogMessageInfo(
            message = "AMXConfigStartupService.preDestroy(): stopping AMX" ,
            level = "INFO")
    public static final String stoppingAMX = LOGMSG_PREFIX + "-00036";  
    
    @LogMessageInfo(
            message = "Illegal non-string type for {0}.{1}(): {2}",
            level = "INFO")
    public static final String illegalNonstring = LOGMSG_PREFIX + "-00037";  
    
    @LogMessageInfo(
            message = "Can't get field value for {0}: exception: {1}",
            level = "INFO")
    public static final String cantGetField = LOGMSG_PREFIX + "-00038";  
    
    @LogMessageInfo(
            message = "Can't getTypesImplementing for {0}: exception: {1}",
            level = "INFO")
    public static final String cantGetTypesImplementing = LOGMSG_PREFIX + "-00039";  
    
    @LogMessageInfo(
            message = "Can't get childrenSet() from MBean: {0}, exception: {1}",
            level = "WARNING")
    public static final String cantGetChildren = LOGMSG_PREFIX + "-00040";  
    
    @LogMessageInfo(
            message = "Problem with MBean: {0}, exception: {1}" ,
            level = "WARNING")
    public static final String problemWithMbean = LOGMSG_PREFIX + "-00041";  
    
    @LogMessageInfo(
            message = "PathnamesImpl.getAllPathnames(): unexpected Throwable: {1}" ,
            level = "WARNING")
    public static final String unexpectedThrowable = LOGMSG_PREFIX + "-00042";  
    
    @LogMessageInfo(
            message = "Can't get path() for MBean: {0}, exception: {1}" ,
            level = "WARNING")
    public static final String cantGetPath = LOGMSG_PREFIX + "-00043";  
    
    @LogMessageInfo(
            message = "Can't instantiate realm: {0}, exception: {1}" ,
            level = "WARNING")
    public static final String cantInstantiateRealm = LOGMSG_PREFIX + "-00044";  
    
    @LogMessageInfo(
            message = "getRealmNames(): Can't get realm names, exception:" ,
            level = "WARNING")
    public static final String cantGetRealmNames = LOGMSG_PREFIX + "-00045";  
    
    @LogMessageInfo(
            message = "Cannot find primordial com.sun.enterprise.osgi-adapter" ,
            level = "WARNING")
    public static final String cantFindOSGIAdapter = LOGMSG_PREFIX + "-00046";  
    
    @LogMessageInfo(
            message = "Stopping server forcibly",
            level = "WARNING")
    public static final String stoppingServerForcibly = LOGMSG_PREFIX + "-00047";  
    
    @LogMessageInfo(
            message = "Can't get cipher suites",
            level = "INFO")
    public static final String cantGetCipherSuites = LOGMSG_PREFIX + "-00048";  
    
    @LogMessageInfo(
            message = "MBeanInfoSupport: @ManagedAttribute cannot also be @ManagedOperation: {0}.{1}()",
            level = "WARNING")
    public static final String attributeCantBeOperation = LOGMSG_PREFIX + "-00049";  
    
    @LogMessageInfo(
            message = "MBeanInfoSupport: @ManagedAttribute not a getter or setter: {0}.{1}()",
            level = "WARNING")
    public static final String attributeNotGetterSetter = LOGMSG_PREFIX + "-00050";  
    
    
}
