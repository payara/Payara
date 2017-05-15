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

package org.glassfish.web.ha;

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
    /**
     * The logger to use for logging ALL web container related messages.
     */
    @LogMessagesResourceBundle
    private static final String SHARED_LOGMESSAGE_RESOURCE =
            "org.glassfish.web.ha.session.management.LogMessages";

    @LoggerInfo(subsystem="WEB", description="WEB HA Logger", publish=true)
    private static final String WEB_HA_LOGGER = "javax.enterprise.web.ha";

    private static final Logger LOGGER =
            Logger.getLogger(WEB_HA_LOGGER, SHARED_LOGMESSAGE_RESOURCE);

    private LogFacade() {}

    public static Logger getLogger() {
        return LOGGER;
    }

    private static final String prefix = "AS-WEB-HA-";

    @LogMessageInfo(
            message = "Exception during removing synchronized from backing store",
            level = "WARNING")
    public static final String EXCEPTION_REMOVING_SYNCHRONIZED = prefix + "00001";

    @LogMessageInfo(
            message = "Exception during removing expired session from backing store",
            level = "WARNING")
    public static final String EXCEPTION_REMOVING_EXPIRED_SESSION = prefix + "00002";

    @LogMessageInfo(
            message = "Error creating inputstream",
            level = "WARNING")
    public static final String ERROR_CREATING_INPUT_STREAM = prefix + "00003";

    @LogMessageInfo(
            message = "Exception during deserializing the session",
            level = "WARNING")
    public static final String EXCEPTION_DESERIALIZING_SESSION = prefix + "00004";

    @LogMessageInfo(
            message = "Exception occurred in getSession",
            level = "WARNING")
    public static final String EXCEPTION_GET_SESSION = prefix + "00005";

    @LogMessageInfo(
            message = "Failed to remove session from backing store",
            level = "WARNING")
    public static final String FAILED_TO_REMOVE_SESSION = prefix + "00006";

    @LogMessageInfo(
            message = "Required version NumberFormatException",
            level = "INFO")
    public static final String REQUIRED_VERSION_NFE = prefix + "00007";

    @LogMessageInfo(
            message = "Could not create backing store",
            level = "WARNING")
    public static final String COULD_NOT_CREATE_BACKING_STORE = prefix + "00008";

}
