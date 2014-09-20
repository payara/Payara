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

package com.sun.enterprise.resource.pool.waitqueue;

import com.sun.appserv.connectors.internal.api.PoolingException;
import com.sun.logging.LogDomains;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Factory to create appropriate Pool Wait Queue
 *
 * @author Jagadish Ramu
 */
public class PoolWaitQueueFactory {

    private final static Logger _logger = LogDomains.getLogger(PoolWaitQueueFactory.class, LogDomains.RSR_LOGGER);

    public static PoolWaitQueue createPoolWaitQueue(String className) throws PoolingException {
        PoolWaitQueue waitQueue;

        if (className != null) {
            waitQueue = initializeCustomWaitQueueInPrivilegedMode(className);
        } else {
            waitQueue = new DefaultPoolWaitQueue();
            debug("Initializing Default Pool Wait Queue");
        }
        return waitQueue;
    }

    private static PoolWaitQueue initializeCustomWaitQueueInPrivilegedMode(final String className) throws PoolingException {
        Object result = AccessController.doPrivileged(new PrivilegedAction() {
            public Object run() {

                Object result = null;
                try {
                    result = initializeCustomWaitQueue(className);
                } catch (Exception e) {
                    _logger.log(Level.WARNING, "pool.waitqueue.init.failure", className);
                    _logger.log(Level.WARNING, "pool.waitqueue.init.failure.exception", e);
                }
                return result;
            }
        });
        if (result != null) {
            return (PoolWaitQueue) result;
        } else {
            throw new PoolingException("Unable to initalize custom PoolWaitQueue : " + className);
        }
    }

    private static PoolWaitQueue initializeCustomWaitQueue(String className) throws Exception {
        PoolWaitQueue waitQueue;
        Class class1 = Thread.currentThread().getContextClassLoader().loadClass(className);
        waitQueue = (PoolWaitQueue) class1.newInstance();
        if(_logger.isLoggable(Level.FINEST)) {
            _logger.log(Level.FINEST, "Using Pool Wait Queue class : ", className);
        }
        return waitQueue;
    }

    private static void debug(String debugStatement) {
        if(_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE, debugStatement);
        }
    }
}
