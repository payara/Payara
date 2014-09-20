/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2013 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.concurrent;

import org.glassfish.logging.annotation.LogMessageInfo;
import org.glassfish.logging.annotation.LoggerInfo;
import org.glassfish.logging.annotation.LogMessagesResourceBundle;
import java.util.logging.Logger;

public class LogFacade {

    @LoggerInfo(subsystem = "GlassFish-Concurrency", description = "GlassFish Concurrency Logger", publish = true)
    private static final String LOGGER_NAME = "javax.enterprise.concurrent";

    @LogMessagesResourceBundle
    private static final String LOGGER_RB = "org.glassfish.concurrent.LogMessages";

    private static final Logger LOGGER = Logger.getLogger(LOGGER_NAME, LOGGER_RB);

    private LogFacade() {}

    public static Logger getLogger() {
        return LOGGER;
    }

    private static final String prefix = "AS-CONCURRENT-";

    @LogMessageInfo(
            message = "Task [{0}] has been running on thread [{1}] for {2} seconds, which is more than the configured " +
                    "hung task threshold of {3} seconds in [{4}].",
            comment = "A task has been running for longer time than the configured hung task threshold setting.",
            level = "WARNING",
            cause = "A task has been running for longer time than the configured hung task threshold setting.",
            action = "Monitor the task to find out why it is running for a long time. " +
                     "If this is normal, consider setting a higher hung task threshold or setting the " +
                     "\"Long-Running Tasks\" configuration attribute to true. "
    )
    public static final String UNRESPONSIVE_TASK = prefix + "00001";

    @LogMessageInfo(
            message = "Unable to setup or reset runtime context for a task because an invalid context handle is being passed.",
            comment = "When trying to setup and runtime context for a task, an invalid context handle is being passed",
            level = "SEVERE",
            cause = "An invalid context handle is being passed.",
            action = "Contact Glassfish support. "
    )
    public static final String UNKNOWN_CONTEXT_HANDLE = prefix + "00002";

    @LogMessageInfo(
            message = "Unable to bind {0} to JNDI location [{1}].",
            comment = "An unexpected exception occurred when trying to bind a managed object to JNDI namespace.",
            level = "SEVERE",
            cause = "An unexpected exception occurred when trying to bind a managed object to JNDI namespace",
            action = "Review the exception message to determine the cause of the failure and take appropriate action. "
    )
    public static final String UNABLE_TO_BIND_OBJECT = prefix + "00003";

    @LogMessageInfo(
            message = "Unable to deploy {0}.",
            comment = "Unable to deploy a managed object because the configuration information is missing",
            level = "WARNING",
            cause = "No configuration information is provided when trying to deploy a managed object.",
            action = "Contact Glassfish support. "
    )
    public static final String DEPLOY_ERROR_NULL_CONFIG = prefix + "00004";

}
