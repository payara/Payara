/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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
 *
 *
 * This file incorporates work covered by the following copyright and
 * permission notice:
 *
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.catalina.startup;


import org.apache.catalina.*;
import org.apache.catalina.Logger;
import org.apache.catalina.core.StandardEngine;
import org.apache.catalina.core.StandardServer;
import org.glassfish.logging.annotation.LogMessageInfo;

import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.logging.Level;


/**
 * Startup event listener for a <b>Engine</b> that configures the properties
 * of that Engine, and the associated defined contexts.
 *
 * @author Craig R. McClanahan
 * @version $Revision: 1.2 $ $Date: 2005/12/08 01:28:07 $
 */

public final class EngineConfig
    implements LifecycleListener {

    // ----------------------------------------------------- Static Variables

    private static final java.util.logging.Logger log = StandardServer.log;
    private static final ResourceBundle rb = log.getResourceBundle();

    @LogMessageInfo(
            message = "Lifecycle event data object {0} is not an Engine",
            level = "WARNING"
    )
    public static final String LIFECYCLE_EVENT_DATA_IS_NOT_ENGINE_EXCEPTION = "AS-WEB-CORE-00440";

    @LogMessageInfo(
            message = "EngineConfig: {0}",
            level = "WARNING"
    )
    public static final String ENGINE_CONFIG = "AS-WEB-CORE-00441";

    @LogMessageInfo(
            message = "EngineConfig: Processing START",
            level = "INFO"
    )
    public static final String ENGINE_CONFIG_PROCESSING_START_INFO = "AS-WEB-CORE-00442";

    @LogMessageInfo(
            message = "EngineConfig: Processing STOP",
            level = "INFO"
    )
    public static final String ENGINE_CONFIG_PROCESSING_STOP_INFO = "AS-WEB-CORE-00443";


    // ----------------------------------------------------- Instance Variables

    /**
     * The debugging detail level for this component.
     */
    private int debug = 0;


    /**
     * The Engine we are associated with.
     */
    private Engine engine = null;


    // ------------------------------------------------------------- Properties


    /**
     * Return the debugging detail level for this component.
     */
    public int getDebug() {
        return (this.debug);
    }


    /**
     * Set the debugging detail level for this component.
     *
     * @param debug The new debugging detail level
     */
    public void setDebug(int debug) {
        this.debug = debug;
    }


    // --------------------------------------------------------- Public Methods


    /**
     * Process the START event for an associated Engine.
     *
     * @param event The lifecycle event that has occurred
     */
    public void lifecycleEvent(LifecycleEvent event) {

        // Identify the engine we are associated with
        try {
            engine = (Engine) event.getLifecycle();
            if (engine instanceof StandardEngine) {
                int engineDebug = ((StandardEngine) engine).getDebug();
                if (engineDebug > this.debug)
                    this.debug = engineDebug;
            }
        } catch (ClassCastException e) {
            String msg = MessageFormat.format(rb.getString(LIFECYCLE_EVENT_DATA_IS_NOT_ENGINE_EXCEPTION),
                                              event.getLifecycle());
            log(msg, e);
            return;
        }

        // Process the event that has occurred
        if (event.getType().equals(Lifecycle.START_EVENT))
            start();
        else if (event.getType().equals(Lifecycle.STOP_EVENT))
            stop();

    }


    // -------------------------------------------------------- Private Methods


    /**
     * Log a message on the Logger associated with our Engine (if any)
     *
     * @param message Message to be logged
     */
    private void log(String message) {
        Logger logger = null;
        if (engine != null) {
            logger = engine.getLogger();
        }
        if (logger != null) {
            logger.log("EngineConfig: " + message);
        } else {
            if (log.isLoggable(Level.INFO)) {
                log.log(Level.INFO, ENGINE_CONFIG, message);
            }
        }
    }


    /**
     * Log a message on the Logger associated with our Engine (if any)
     *
     * @param message Message to be logged
     * @param t Associated exception
     */
    private void log(String message, Throwable t) {
        Logger logger = null;
        if (engine != null) {
            logger = engine.getLogger();
        }
        if (logger != null) {
            logger.log("EngineConfig: " + message, t, Logger.WARNING);
        } else {
            String msg = MessageFormat.format(rb.getString(ENGINE_CONFIG),
                                              message);
            log.log(Level.WARNING, msg, t);
        }
    }


    /**
     * Process a "start" event for this Engine.
     */
    private void start() {

        if (debug > 0)
            log(rb.getString(ENGINE_CONFIG_PROCESSING_START_INFO));

    }


    /**
     * Process a "stop" event for this Engine.
     */
    private void stop() {

        if (debug > 0)
            log(rb.getString(ENGINE_CONFIG_PROCESSING_STOP_INFO));

    }


}
