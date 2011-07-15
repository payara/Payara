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

package com.sun.enterprise.web.logger;

import com.sun.enterprise.util.logging.IASLevel;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An implementation of <b>Logger</b> that writes log messages
 * using JDK 1.4's logging API.
 *
 * @author Arvind Srinivasan
 * @author Neelam Vaidya
 * @version $Revision: 1.4 $
 */

public final class IASLogger extends LoggerBase {

    // ----------------------------------------------------- Instance Variables

    /**
     * The server wide log message handler.
     */
    Logger _logger = null;

    /**
     * Classname of the object invoking the log method.
     */
    private String _classname;

    /**
     * Name of the method invoking the log method.
     */
    private String _methodname;
    
    /**
     * The descriptive information about this implementation.
     */
    private static final String info =
        "com.sun.enterprise.web.logger.IASLogger/1.0";


    // ----------------------------------------------------------- Constructors

    /**
     * Deny void construction.
     */
    private IASLogger() {
        super();
    }

    /**
     * Construct a new instance of this class, that uses the specified
     * logger instance.
     *
     * @param logger The logger to send log messages to
     */
    public IASLogger(Logger logger) {
        _logger = logger;
    }

    // ------------------------------------------------------ Protected Methods

    /**
     * Logs the message to the JDK 1.4 logger that handles all log
     * messages for the iPlanet Application Server.
     */
    protected void write(String msg, int verbosity) {
        
        if (_logger == null)
            return;

        Level level = Level.INFO;

        if (verbosity == FATAL)
            level = (Level)IASLevel.FATAL;
        else if (verbosity == ERROR)
            level = Level.SEVERE;
        else if (verbosity == WARNING)
            level = Level.WARNING;
        else if (verbosity == INFORMATION)
            level = Level.INFO;
        else if (verbosity == DEBUG)
            level = Level.FINER;

        inferCaller();
        _logger.logp(level, _classname, _methodname, msg);
    }

    // ------------------------------------------------------ Private Methods

    /**
     * Examine the call stack and determine the name of the method and the
     * name of the class logging the message.
     */
    private void inferCaller() {
        // Get the stack trace.
        StackTraceElement stack[] = (new Throwable()).getStackTrace();
        _classname = "";
        _methodname = "";
        for (int ix=0; ix < stack.length; ix++) {
	    StackTraceElement frame = stack[ix];
	    _classname = frame.getClassName();
	    if (!_classname.startsWith("com.sun.enterprise.web.logger")) {
		// We've found the relevant frame. Get Method Name.
		_methodname = frame.getMethodName();
		return;
	    }
        }
    }
}
