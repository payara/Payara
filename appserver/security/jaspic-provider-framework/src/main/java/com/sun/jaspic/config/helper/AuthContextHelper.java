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

package com.sun.jaspic.config.helper;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.AuthStatus;

/**
 *
 * @author Ron Monzillo
 */
public abstract class AuthContextHelper {

    String loggerName;
    private boolean returnNullContexts = false;

    // include this to force subclasses to call constructor with LoggerName
    private AuthContextHelper() {

    }

    protected AuthContextHelper(String loggerName, boolean returnNullContexts) {
        this.loggerName = loggerName;
        this.returnNullContexts = returnNullContexts;
    }

    protected boolean isLoggable(Level level) {
        Logger logger = Logger.getLogger(loggerName);
        return logger.isLoggable(level);
    }

    protected void logIfLevel(Level level, Throwable t, String... msgParts) {
        Logger logger = Logger.getLogger(loggerName);
        if (logger.isLoggable(level)) {
            StringBuffer msgB = new StringBuffer("");
            for (String m : msgParts) {
                msgB.append(m);
            }
            String msg = msgB.toString();
            if ( !msg.isEmpty() && t != null) {
                logger.log(level, msg, t);
            } else if (!msg.isEmpty()) {
                logger.log(level, msg);
            }
        }
    }

    /**
     *
     * @param level
     * @return
     */
    protected Logger getLogger(Level level) {
        Logger rvalue = Logger.getLogger(loggerName);
        if (rvalue.isLoggable(level)) {
            return rvalue;
        }
        return null;
    }

    protected abstract void refresh();

    public boolean returnsNullContexts() {
        return returnNullContexts;
    }

    public <M> boolean isProtected(M[] template, String authContextID) throws AuthException {
        try {
            if (returnNullContexts) {
                return hasModules(template, authContextID);
            } else {
                return true;
            }
        } catch (AuthException ae) {
            throw new RuntimeException(ae);
        }
    }

    /**
     *
     * @param <M>
     * @param template
     * @param authContextID
     * @return
     * @throws AuthException
     */
    public abstract <M> boolean hasModules(M[] template,String authContextID) throws AuthException;

    /**
     *
     * @param <M>
     * @param template
     * @param authContextID
     * @return
     * @throws AuthException
     */
    public abstract <M> M[] getModules(M[] template,String authContextID) throws AuthException;

    /**
     *
     * @param i
     * @param properties
     * @return
     */
    public abstract Map<String, ?> getInitProperties(int i, Map<String, ?> properties);

    /**
     *
     * @param successValue
     * @param i
     * @param moduleStatus
     * @return
     */
    public abstract boolean exitContext(AuthStatus[] successValue,
            int i, AuthStatus moduleStatus);

    /**
     *
     * @param successValue
     * @param defaultFailStatus
     * @param status
     * @param position
     * @return
     */
    public abstract AuthStatus getReturnStatus(AuthStatus[] successValue,
            AuthStatus defaultFailStatus, AuthStatus[] status, int position);
}
