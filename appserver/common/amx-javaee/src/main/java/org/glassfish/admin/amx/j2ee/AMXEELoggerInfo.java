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
package org.glassfish.admin.amx.j2ee;

import java.util.logging.Logger;
import org.glassfish.logging.annotation.LogMessageInfo;
import org.glassfish.logging.annotation.LogMessagesResourceBundle;
import org.glassfish.logging.annotation.LoggerInfo;

/**
 * Logger information for the amx-javaee module.
 * @author Tom Mueller
 */
/* Module private */
public class AMXEELoggerInfo {
    public static final String LOGMSG_PREFIX = "AS-AMXEE";
    
    @LogMessagesResourceBundle
    public static final String SHARED_LOGMESSAGE_RESOURCE = "org.glassfish.admin.amx.j2ee.LogMessages";
    
    @LoggerInfo(subsystem = "AMX-JAVAEE", description = "AMX Services", publish = true)
    public static final String AMXEE_LOGGER = "javax.enterprise.system.tools.amxee";
    private static final Logger amxEELogger = Logger.getLogger(
                AMXEE_LOGGER, SHARED_LOGMESSAGE_RESOURCE);

    public static Logger getLogger() {
        return amxEELogger;
    }
    
    @LogMessageInfo(
            message = "Registering application {0} using AMX having exception {1}",
            level = "INFO")
    public static final String registeringApplicationException = LOGMSG_PREFIX + "-001";

    @LogMessageInfo(
            message = "Null from ApplicationInfo.getMetadata(Application.class) for application {0}",
            level = "WARNING")
    public static final String nullAppinfo = LOGMSG_PREFIX + "-002";

    @LogMessageInfo(
            message = "Unable to get Application config for application {0}",
            level = "WARNING")
    public static final String errorGetappconfig = LOGMSG_PREFIX + "-003";

    @LogMessageInfo(
            message = "Can't register JSR 77 MBean for resourceRef {0} having exception {1}",
            level = "INFO")
    public static final String cantRegisterMbean = LOGMSG_PREFIX + "-004";

    @LogMessageInfo(
            message = "Can't unregister MBean: {0}",
            level = "WARNING")
    public static final String cantUnregisterMbean = LOGMSG_PREFIX + "-005";

    @LogMessageInfo(
            message = "J2EEDomain registered at {0}",
            level = "INFO")
    public static final String domainRegistered = LOGMSG_PREFIX + "-006";



}
