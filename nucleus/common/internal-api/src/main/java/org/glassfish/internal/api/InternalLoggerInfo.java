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
package org.glassfish.internal.api;

import java.util.logging.Logger;
import org.glassfish.logging.annotation.LogMessageInfo;
import org.glassfish.logging.annotation.LogMessagesResourceBundle;
import org.glassfish.logging.annotation.LoggerInfo;

/**
 * Logger information for the internal-api module.
 * @author Tom Mueller
 */
/* Module private */
public class InternalLoggerInfo {
    public static final String LOGMSG_PREFIX = "NCLS-COM";
    
    @LogMessagesResourceBundle
    public static final String SHARED_LOGMESSAGE_RESOURCE = "org.glassfish.internal.api.LogMessages";
    
    @LoggerInfo(subsystem = "COMMON", description = "Internal API", publish = true)
    public static final String INT_LOGGER = "javax.enterprise.system.tools.util";
    private static final Logger intLogger = Logger.getLogger(
                INT_LOGGER, SHARED_LOGMESSAGE_RESOURCE);

    public static Logger getLogger() {
        return intLogger;
    }

    @LogMessageInfo(
            message = "Exception {0} resolving password alias {1} in property {2}.",
            level = "WARNING")
    public static final String exceptionResolvingAlias = LOGMSG_PREFIX + "-01001";

    @LogMessageInfo(
            message = "Unknown property {0} found unresolving {1}.",
            cause = "No value was found for a property. This indicates a software problem.", 
            action = "Check the server logs and contact Oracle support.",
            level = "SEVERE")
    public static final String unknownProperty = LOGMSG_PREFIX + "-01002";

    @LogMessageInfo(
            message = "System property reference missing trailing \"'}'\" at {0} in domain.xml.",
            cause = "A system property reference in domain.xml is invalid.", 
            action = "Check the domain.xml file for an invalid system property reference.",
            level = "SEVERE")
    public static final String referenceMissingTrailingDelim = LOGMSG_PREFIX + "-01003";

    @LogMessageInfo(
            message = "System property reference missing starting \"$'{'\" at {0} in domain.xml.",
            cause = "A system property reference in domain.xml is invalid.", 
            action = "Check the domain.xml file for an invalid system property reference.",
            level = "SEVERE")
    public static final String referenceMissingStartingDelim = LOGMSG_PREFIX + "-01004";

}
