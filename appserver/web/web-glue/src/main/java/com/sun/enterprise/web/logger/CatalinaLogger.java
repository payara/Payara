/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.web.logger;

import com.sun.enterprise.util.logging.IASLevel;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is an adapter of java.util.logging.Logger to org.apache.catalina.Logger.
 *
 * @author Shing Wai Chan
 *
 */
public final class CatalinaLogger extends LoggerBase {
    private Logger logger = null;

    /**
     * Construct a new instance of this class, that uses the specified
     * logger instance.
     *
     * @param logger The logger to send log messages to
     */
    public CatalinaLogger(Logger logger) {
        this.logger = logger;
    }

    @Override
    protected void write(String msg, int verbosity) {
        
        if (logger == null) {
            return;
        }

        Level level = Level.INFO;

        if (verbosity == FATAL) {
            level = (Level)IASLevel.FATAL;
        } else if (verbosity == ERROR) {
            level = Level.SEVERE;
        } else if (verbosity == WARNING) {
            level = Level.WARNING;
        } else if (verbosity == INFORMATION) {
            level = Level.INFO;
        } else if (verbosity == DEBUG) {
            level = Level.FINER;
        }

        logger.log(level, msg);
    }

    /**
     * Set the verbosity level of this logger.  Messages logged with a
     * higher verbosity than this level will be silently ignored.
     * 
     * @param logLevel The new verbosity level, as a string
     */
    public void setLevel(String logLevel) {
        if ("SEVERE".equalsIgnoreCase(logLevel)) {
            logger.setLevel(Level.SEVERE);
        } else if ("WARNING".equalsIgnoreCase(logLevel)) {
            logger.setLevel(Level.WARNING);
        } else if ("INFO".equalsIgnoreCase(logLevel)) {
            logger.setLevel(Level.INFO);
        } else if ("CONFIG".equalsIgnoreCase(logLevel)) {
            logger.setLevel(Level.CONFIG);
        } else if ("FINE".equalsIgnoreCase(logLevel)) {
            logger.setLevel(Level.FINE);
        } else if ("FINER".equalsIgnoreCase(logLevel)) {
            logger.setLevel(Level.FINER);
        } else if ("FINEST".equalsIgnoreCase(logLevel)) {
            logger.setLevel(Level.FINEST);
        } else {
            logger.setLevel(Level.INFO);
        }
    }
}
