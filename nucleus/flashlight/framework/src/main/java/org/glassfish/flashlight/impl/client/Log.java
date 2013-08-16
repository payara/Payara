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
package org.glassfish.flashlight.impl.client;

import com.sun.enterprise.util.Utility;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.flashlight.FlashlightLoggerInfo;

/**
 * Nice easy simple fast (and therefore more likely to use!) class for logging
 * messages. Has built-in support for local strings. If the string is not found
 * then the infrastructure logger code will look for 'log strings'
 *
 * @author Byron Nevins
 */
final class Log {

    private static final Logger logger;

    static {
        logger = FlashlightLoggerInfo.getLogger();

        if (Boolean.parseBoolean(Utility.getEnvOrProp("AS_DEBUG")))
            logger.setLevel(Level.ALL);
    }

    static Logger getLogger() {
        return logger;
    }

    static void finer(String msg, Object... objs) {
        logger.finer(Strings.get(msg, objs));
    }

    static void fine(String msg, Object... objs) {
        logger.fine(Strings.get(msg, objs));
    }

    static void info(String msg, Object... objs) {
        logger.info(Strings.get(msg, objs));
    }

    static void warning(String msg, Object... objs) {
        logger.warning(Strings.get(msg, objs));
    }

    static void severe(String msg, Object... objs) {
        logger.severe(Strings.get(msg, objs));
    }
}
