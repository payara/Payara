/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.tools.upgrade.logging;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;

/**
 * Sets up the common logger used by all packages. Currently sending
 * output to log file. This can be turned off by calling
 * removeDefaultLogHandler -- the GUI may want to do this for example.
 */
public class LogService {

    // This is the logger that all upgrade classes use.
    private static final Logger logger =
        Logger.getLogger("com.sun.enterprise.tools.upgrade");

    // Where the logs will go. For more options see:
    // http://java.sun.com/javase/6/docs/api/java/util/logging/FileHandler.html
    private static final String LOG_FILE_PATTERN = "upgrade.log";

    // Need a handler so we can use our formatter
    private static final Handler defaultHandler = createLogHandler();

    static {
        defaultHandler.setFormatter(new UpgradeFormatter());
        logger.addHandler(defaultHandler);
        logger.setUseParentHandlers(false);
    }

    /**
     * Get the logger used by all classes in the upgrade tool.
     *
     * @return The logger used by the upgrade tool.
     */
    public static Logger getLogger() {
        return logger;
    }

    // CLI could use this for sending some messages to console
    public static Formatter createFormatter() {
        return new UpgradeFormatter();
    }
    
    // Called when class is initialized
    private static Handler createLogHandler() {
        Handler handler = null;
        try {
            // 2nd param will create new file rather than appending
            handler = new FileHandler(LOG_FILE_PATTERN, false);
        } catch (IOException ioe) {
            // very odd, but lets tell the user and continue
            System.err.println(String.format(
                "Could not create log '%s' due to error '%s'",
                LOG_FILE_PATTERN, ioe.getLocalizedMessage()));
            System.err.println("Will send logs to standard.err instead.");
            handler = new StreamHandler(System.err, new UpgradeFormatter());
        }
        return handler;
    }

    // Simply outputting the log record's message
    private static class UpgradeFormatter extends Formatter {

        @Override
        public String format(LogRecord record) {
            return record.getMessage() + "\n";
        }

    }
}

