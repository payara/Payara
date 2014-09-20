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

package org.glassfish.webservices.metroglue;

import java.util.logging.Logger;
import org.glassfish.logging.annotation.LogMessageInfo;
import org.glassfish.logging.annotation.LogMessagesResourceBundle;
import org.glassfish.logging.annotation.LoggerInfo;

/**
 *
 * @author Lukas Jungmann
 */
public final class LogUtils {

    private static final String LOGMSG_PREFIX = "AS-WSMETROGLUE";

    @LogMessagesResourceBundle
    public static final String LOG_MESSAGES = "org.glassfish.webservices.metroglue.LogMessages";

    @LoggerInfo(subsystem = "WEBSERVICES", description = "Metro Glue Main Logger", publish = true)
    public static final String LOG_DOMAIN = "javax.enterprise.webservices.metroglue";

    private static final Logger LOGGER = Logger.getLogger(LOG_DOMAIN, LOG_MESSAGES);

    public static Logger getLogger() {
        return LOGGER;
    }

    @LogMessageInfo(
            message = "Web service endpoint deployment events listener registered successfully.",
            level = "INFO")
    public static final String ENDPOINT_EVENT_LISTENER_REGISTERED = LOGMSG_PREFIX + "-10010";

    @LogMessageInfo(
            message = "High availability environment configuration injected into Metro high availability provider.",
            level = "INFO")
    public static final String METRO_HA_ENVIRONEMT_INITIALIZED = LOGMSG_PREFIX + "-10020";

    @LogMessageInfo(
            message = "Endpoint deployment even received.",
            level = "FINEST")
    public static final String ENDPOINT_EVENT_DEPLOYED = LOGMSG_PREFIX + "-10011";

    @LogMessageInfo(
            message = "Endpoint undeployment even received.",
            level = "FINEST")
    public static final String ENDPOINT_EVENT_UNDEPLOYED = LOGMSG_PREFIX + "-10012";

    @LogMessageInfo(
            message = "Loading WS-TX Services. Please wait.",
            level = "INFO")
    public static final String WSTX_SERVICE_LOADING = LOGMSG_PREFIX + "-10001";

    @LogMessageInfo(
            message = "WS-TX Services successfully started.",
            level = "INFO")
    public static final String WSTX_SERVICE_STARTED = LOGMSG_PREFIX + "-10002";

    @LogMessageInfo(
            message = "WS-TX Services application was deployed explicitly.",
            level = "WARNING")
    public static final String WSTX_SERVICE_DEPLOYED_EXPLICITLY = LOGMSG_PREFIX + "-10003";

    @LogMessageInfo(
            message = "Cannot deploy or load WS-TX Services: {0}",
            comment = "{0} - cause",
            level = "WARNING")
    public static final String WSTX_SERVICE_CANNOT_DEPLOY = LOGMSG_PREFIX + "-10004";

    @LogMessageInfo(
            message = "Caught unexpected exception.",
            level = "WARNING")
    public static final String WSTX_SERVICE_UNEXPECTED_EXCEPTION = LOGMSG_PREFIX + "-19999";

    @LogMessageInfo(
            message = "Exception occurred retrieving port configuration for WSTX service.",
            level = "FINEST")
    public static final String WSTX_SERVICE_PORT_CONFIGURATION_EXCEPTION = LOGMSG_PREFIX + "-19998";
}
