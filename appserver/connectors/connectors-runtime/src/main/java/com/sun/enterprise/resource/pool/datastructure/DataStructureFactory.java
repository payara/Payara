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

package com.sun.enterprise.resource.pool.datastructure;

import com.sun.appserv.connectors.internal.api.PoolingException;
import com.sun.enterprise.resource.pool.ResourceHandler;
import com.sun.logging.LogDomains;

import java.lang.reflect.Constructor;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Factory to create appropriate datastructure type for the pool<br>
 *
 * @author Jagadish Ramu
 */
public class DataStructureFactory {
    //TODO synchronize datastructure creation ?
    protected final static Logger _logger = LogDomains.getLogger(DataStructureFactory.class,LogDomains.RSR_LOGGER);

    public static DataStructure getDataStructure(String className, String parameters, int maxPoolSize,
                                                 ResourceHandler handler, String strategyClass) throws PoolingException {
        DataStructure ds;

        if (className != null) {
            if(className.equals(ListDataStructure.class.getName())){
                ds = new ListDataStructure(parameters, maxPoolSize, handler, strategyClass);
            }else if(className.equals(RWLockDataStructure.class.getName())){
                ds = new RWLockDataStructure(parameters, maxPoolSize, handler, strategyClass);
            }else{
                ds = initializeCustomDataStructureInPrivilegedMode(className, parameters, maxPoolSize, handler, strategyClass);
            }
        } else {
            debug("Initializing RWLock DataStructure");
            ds = new RWLockDataStructure(parameters, maxPoolSize, handler, strategyClass);
        }
        return ds;
    }

    private static DataStructure initializeCustomDataStructureInPrivilegedMode(final String className,
                                                                               final String parameters,
                                                                               final int maxPoolSize,
                                                                               final ResourceHandler handler,
                                                                               final String strategyClass)
            throws PoolingException {
        Object result = AccessController.doPrivileged(new PrivilegedAction() {
            public Object run() {

                Object result = null;
                try {
                    result = initializeDataStructure(className, parameters, maxPoolSize, handler, strategyClass);
                } catch (Exception e) {
                    _logger.log(Level.WARNING, "pool.datastructure.init.failure", className);
                    _logger.log(Level.WARNING, "pool.datastructure.init.failure.exception", e);
                }
                return result;
            }
        });
        if (result != null) {
            return (DataStructure) result;
        } else {
            throw new PoolingException("Unable to initalize custom DataStructure : " + className);
        }
    }

    private static DataStructure initializeDataStructure(String className, String parameters, int maxPoolSize,
                                                         ResourceHandler handler, String strategyClass) throws Exception {
        DataStructure ds;
        Object[] constructorParameters = new Object[]{parameters, maxPoolSize, handler, strategyClass};

        Class class1 = Thread.currentThread().getContextClassLoader().loadClass(className);
        Constructor constructor = class1.getConstructor(String.class, int.class, ResourceHandler.class, String.class);
        ds = (DataStructure) constructor.newInstance(constructorParameters);
        if(_logger.isLoggable(Level.FINEST)) {
            _logger.log(Level.FINEST, "Using Pool Data Structure : ", className);
        }
        return ds;
    }

    private static void debug(String debugStatement) {
        if(_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE, debugStatement);
        }
    }
}
