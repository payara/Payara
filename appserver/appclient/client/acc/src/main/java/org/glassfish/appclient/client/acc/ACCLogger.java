/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.appclient.client.acc;

import java.io.File;
import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import org.glassfish.appclient.client.acc.config.LogService;

/**
 * Logger that conforms to the glassfish-acc.xml config file settings for logging
 * while, in some cases, also adjusting other loggers.
 * <p>
 * Historically the logging level specified in the glassfish-acc.xml is used to set the level
 * for all loggers.  Beginning with v3, which supports conventional log settings
 * via logging.properties as well, we make sure that each logger's level is
 * at least as detailed as the setting in the config file.
 * <p>
 * Also, in prior versions if the user specified a logging file in the
 * glassfish-acc.xml config file then all pre-existing handlers would be removed,
 * essentially replaced with a single handler to send output to the user-specified
 * file.  Beginning with v3 the ACC augments - rather than replaces - existing
 * handlers if the settings in glassfish-acc.xml specify a file.
 *
 * @author tjquinn
 */
public class ACCLogger extends Logger {

    private static final String ACC_LOGGER_NAME = "GlassFish.ACC";
    
    private static final Level DEFAULT_ACC_LOG_LEVEL = Level.INFO;

    public ACCLogger(final LogService logService) throws IOException {
        super(ACC_LOGGER_NAME, null);
        init(logService);
    }

    private void init(final LogService logService) throws IOException {
        final Level level = chooseLevel(logService);
        final Handler configuredFileHandler = createHandler(logService, level);
        final ResourceBundle rb = ResourceBundle.getBundle(
                ACCLogger.class.getPackage().getName() + ".LogStrings");

        /*
         * Set existing loggers to at least the configured level.
         */
        for (Enumeration<String> names =
                LogManager.getLogManager().getLoggerNames();
               names.hasMoreElements(); ) {
            final String loggerName = names.nextElement();
            final Logger logger = LogManager.getLogManager().getLogger(loggerName);
            if (logger == null) {
                final String msg = MessageFormat.format(
                        rb.getString("appclient.nullLogger"),
                        loggerName);
                if (level.intValue() <= Level.CONFIG.intValue()) {
                    System.err.println(msg);
                }
            } else {
                reviseLogger(
                        logger,
                        level,
                        configuredFileHandler);
            }
        }
    }

    /**
     * Returns the logging level to use, checking the configured log level and
     * using the default if the configured value is absent or invalid.
     * @param configLevelText configured level name
     * @return log level to use for all logging in the ACC
     */
    private static Level chooseLevel(final LogService logService) {
        Level level = DEFAULT_ACC_LOG_LEVEL;
        if (logService != null ) {
            String configLevelText = logService.getLevel();
            if (configLevelText!= null &&  ( ! configLevelText.equals(""))) {
                try {
                    level = Level.parse(configLevelText);
                } catch (IllegalArgumentException e) {
                    //ignore - use the previously-assigned default - and log it !
                    Logger.getLogger(ACCLogger.class.getName()).warning("Logger.Level = " + configLevelText + "??");
                }
            }
        }
        return level;
    }

    /**
     * Creates a logging handler to send logging to the specified file, using
     * the indicated level.
     * @param filePath path to the log file to which to log
     * @param level level at which to log
     * @return logging Handler if filePath is specified and valid; null otherwise
     */
    private static Handler createHandler(final LogService logService, final Level level) throws IOException {
        Handler handler = null;
        final String filePath = (logService == null) ? null : logService.getFile();
        if (filePath == null || filePath.equals("")) {
            return null;
        }
        handler = new FileHandler(filePath, true /* append */);
        handler.setFormatter(new SimpleFormatter());
        handler.setLevel(level);
        File lockFile = new File(filePath + ".lck");
        lockFile.deleteOnExit();
        return handler;
    }

    private static void reviseLogger(final Logger logger,
            final Level level,
            final Handler handler) {
            AccessController.doPrivileged(
                    new PrivilegedAction() {
                        public Object run() {
                            if ( ! logger.isLoggable(level)) {
                                logger.setLevel(level);
                            }
                            if (handler != null) {
                                logger.addHandler(handler);
                            }
                            return null;
                        }
                    });
        }
    }
