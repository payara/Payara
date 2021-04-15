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
package org.glassfish.flashlight;

import java.util.logging.Logger;
import org.glassfish.logging.annotation.LogMessageInfo;
import org.glassfish.logging.annotation.LogMessagesResourceBundle;
import org.glassfish.logging.annotation.LoggerInfo;

/**
 * Logger information for the flashlight module.
 * @author  Jennifer Galloway
 */
/* Module private */
public class FlashlightLoggerInfo {
    public static final String LOGMSG_PREFIX = "NCLS-MON";
    
    @LogMessagesResourceBundle
    public static final String SHARED_LOGMESSAGE_RESOURCE = "org.glassfish.flashlight.LogMessages";
    
    @LoggerInfo(subsystem = "MON", description = "Flashlight Services", publish = true)
    public static final String MONITORING_LOGGER = "javax.enterprise.system.tools.monitor";
    private static final Logger monitoringLogger = Logger.getLogger(
                MONITORING_LOGGER, SHARED_LOGMESSAGE_RESOURCE);

    public static Logger getLogger() {
        return monitoringLogger;
    }
    
    @LogMessageInfo(
            message = "Cannot process XML ProbeProvider, xml = {0}, \nException: {1}",
            cause = "Possible syntax error in the ProbeProvider XML",
            action = "Check the syntax of ProbeProvider XML",
            level = "SEVERE")
    public static final String CANNOT_PROCESS_XML_PROBE_PROVIDER = LOGMSG_PREFIX + "-00301";
    
    @LogMessageInfo(
            message = "Cannot resolve the paramTypes, unable to create this probe - {0}",
            cause = "Unknown Java type for the param",
            action = "Try giving a fully qualified name for the type",
            level = "SEVERE")
    public static final String CANNOT_RESOLVE_PROBE_PARAM_TYPES_FOR_PROBE = LOGMSG_PREFIX + "-00302";
    
    @LogMessageInfo(
            message = "Cannot resolve the paramTypes of the probe - {0}, ",
            cause = "Unknown Java type for the param",
            action = "Try giving a fully qualified name for the type",
            level = "SEVERE")
    public static final String CANNOT_RESOLVE_PROBE_PARAM_TYPES = LOGMSG_PREFIX + "-00303";
    
    @LogMessageInfo(
            message = "Can not match the Probe method ({0}) with any method in the DTrace object",
            level = "WARNING")
    public static final String DTRACE_CANT_FIND = LOGMSG_PREFIX + "-00304";
    
    @LogMessageInfo(
            message = "Invalid parameters for ProbeProvider, ignoring {0}",
            level = "WARNING")
    public static final String INVALID_PROBE_PROVIDER = LOGMSG_PREFIX + "-00305";
    
    @LogMessageInfo(
            message = "No Probe Provider found in Probe Provider XML",
            cause = "Invalid Probe Provider XML",
            action = "Check Probe Provider XML syntax",
            level = "SEVERE")
    public static final String NO_PROVIDER_IDENTIFIED_FROM_XML = LOGMSG_PREFIX + "-00306";
    
    @LogMessageInfo(
            message = "invalid pid, start flashlight-agent using asadmin enable-monitoring with --pid option, you may get pid using jps command",
            level = "WARNING")
    public static final String INVALID_PID = LOGMSG_PREFIX + "-00501";
    
    @LogMessageInfo(
            message = "flashlight-agent.jar does not exist under {0}",
            level = "WARNING")
    public static final String MISSING_AGENT_JAR = LOGMSG_PREFIX + "-00502";
    
    @LogMessageInfo(
            message = "flashlight-agent.jar directory {0} does not exist",
            level = "WARNING")
    public static final String MISSING_AGENT_JAR_DIR = LOGMSG_PREFIX + "-00503";
    
    @LogMessageInfo(
            message = "Encountered exception during agent attach",
            level = "WARNING")
    public static final String ATTACH_AGENT_EXCEPTION = LOGMSG_PREFIX + "-00504";
    
    @LogMessageInfo(
            message = "Error transforming Probe: {0}",
            cause = "Exception - see message",
            action = "Check probe syntax",
            level = "SEVERE")
    public static final String BAD_TRANSFORM = LOGMSG_PREFIX + "-00505";
    
    @LogMessageInfo(
            message = "Error unregistering ProbeProvider",
            level = "WARNING")
    public static final String UNREGISTER_PROBE_PROVIDER_EXCEPTION = LOGMSG_PREFIX + "-00506";
    
    @LogMessageInfo(
            message = "Error during re-transformation",
            level = "WARNING")
    public static final String RETRANSFORMATION_ERROR = LOGMSG_PREFIX + "-00507";
    
    @LogMessageInfo(
            message = "Error during registration of FlashlightProbe",
            level = "WARNING")
    public static final String REGISTRATION_ERROR = LOGMSG_PREFIX + "-00508";
    
    @LogMessageInfo(
            message = "Error attempting to write the re-transformed class data",
            level = "WARNING")
    public static final String WRITE_ERROR = LOGMSG_PREFIX + "-00509";
    
    @LogMessageInfo(
            message = "Monitoring is disabled because there is no Attach API from the JVM available",
            level = "WARNING")
    public static final String NO_ATTACH_API = LOGMSG_PREFIX + "-00510";
    
    @LogMessageInfo(
            message = "Error while getting Instrumentation object from ProbeAgentMain", 
            level = "WARNING")
    public static final String NO_ATTACH_GET = LOGMSG_PREFIX + "-00511";
    
    @LogMessageInfo(
            message = "DTrace is not available.",
            cause="This is caused if following are missing: \n1. JDK 7 is required to run DTrace\n2. glassfish-dtrace.jar value-add is required for DTrace",
            action="Run with JDK 7 and glassfish-dtrace.jar",
            level = "INFO")
    public static final String DTRACE_NOT_AVAILABLE = LOGMSG_PREFIX + "-00512";

    @LogMessageInfo(
            message = "DTrace is not supported.",
            cause="This is caused if: \n1. Operating System does not support DTrace.  Currently you must have Solaris 10 or higher for dtrace support.",
            action="Run with Solaris 10 or higher",
            level = "INFO")
    public static final String DTRACE_NOT_SUPPORTED = LOGMSG_PREFIX + "-00513";
    
    @LogMessageInfo(
            message = "DTrace is connected and ready.",
            cause="This is caused if: \n1. Operating System does not support DTrace.  Currently you must have Solaris 10 or higher for dtrace support.",
            action="Run with Solaris 10 or higher",
            level = "INFO")
    public static final String DTRACE_READY = LOGMSG_PREFIX + "-00514";

    @LogMessageInfo(
            message = "Unexpected exception invoking DTrace",
            level = "WARNING")
    public static final String DTRACE_UNEXPECTED_EXCEPTION = LOGMSG_PREFIX + "-00515";

}

