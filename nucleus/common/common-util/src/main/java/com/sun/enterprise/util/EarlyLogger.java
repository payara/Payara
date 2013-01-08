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

package com.sun.enterprise.util;

import com.sun.enterprise.universal.i18n.LocalStringsImpl;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;

/**
 *
 * @author bnevins
 */
public class EarlyLogger {
    private EarlyLogger() {
        // no instances allowed...
    }

    public static void add(Level level, String message) {
        messages.add(new LevelAndMessage(level, prepend + message));
        // also log to the console...
        logger.log(level, message);
    }

    public static List<LevelAndMessage> getEarlyMessages() {
        return messages;
    }

    public final static class LevelAndMessage {

        private final String msg;
        private final Level level;

        LevelAndMessage(Level l, String m) {
            msg = m;
            level = l;
        }

        public final String getMessage() {
            return msg;
        }

        public final Level getLevel() {
            return level;
        }
    }
    private final static List<LevelAndMessage> messages =
            new CopyOnWriteArrayList<LevelAndMessage>();
    private final static LocalStringsImpl strings =
            new LocalStringsImpl(EarlyLogger.class);
    private final static String prepend = strings.get("EarlyLogger.prepend");
    private final static Logger logger = Logger.getAnonymousLogger();
}
