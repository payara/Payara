/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.gjc.spi.base.datastructure;

import com.sun.enterprise.util.i18n.StringManager;
import com.sun.gjc.common.DataSourceObjectBuilder;
import com.sun.logging.LogDomains;
import org.glassfish.resourcebase.resources.api.PoolInfo;

import javax.resource.ResourceException;
import java.lang.reflect.Constructor;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Creates an appropriate statement cache datastructure used in the 
 * Resource Adapter.
 * 
 * @author Shalini M
 */
public class CacheFactory {
    protected final static Logger _logger = 
            LogDomains.getLogger(CacheFactory.class, LogDomains.RSR_LOGGER);

    protected final static StringManager localStrings =
            StringManager.getManager(DataSourceObjectBuilder.class);

    public static Cache getDataStructure(PoolInfo poolInfo, String cacheType,
            int maxSize) throws ResourceException {
        Cache stmtCacheStructure;

        if(cacheType == null || cacheType.trim().equals("")) {
            debug("Initializing LRU Cache Implementation");
            stmtCacheStructure = new LRUCacheImpl(poolInfo, maxSize);
        } else if(cacheType.equals("FIXED")) {
            debug("Initializing FIXED Cache Implementation");
            stmtCacheStructure = new FIXEDCacheImpl(poolInfo, maxSize);
        } else { // consider the value of cacheType as a className
            stmtCacheStructure = initCustomCacheStructurePrivileged(cacheType,
                    maxSize);
        } 
        if(!stmtCacheStructure.isSynchronized()) {
            return new SynchronizedCache(stmtCacheStructure);
        }
        return stmtCacheStructure;
    }

    private static Cache initCustomCacheStructurePrivileged(
            final String className, final int cacheSize) throws ResourceException {
        Object result = AccessController.doPrivileged(new PrivilegedAction() {
            public Object run() {

                Object result = null;
                try {
                    result = initializeCacheStructure(className, cacheSize);
                } catch (Exception e) {
                    _logger.log(Level.WARNING, localStrings.getString(
                            "jdbc.statement-cache.datastructure.init.failure", 
                            className));
                    _logger.log(Level.WARNING, localStrings.getString(
                            "jdbc.statement-cache.datastructure.init.failure.exception", 
                            e));
                }
                return result;
            }
        });
        if (result != null) {
            return (Cache) result;
        } else {
            throw new ResourceException("Unable to initalize custom DataStructure " +
                    "for Statement Cahe : " + className);
        }
    }

    private static Cache initializeCacheStructure(String className, 
            int maxSize) throws Exception {
        Cache ds;
        Object[] constructorParameters = new Object[]{maxSize};

        Class class1 = Class.forName(className);
        Constructor constructor = class1.getConstructor(class1, Integer.class);
        ds = (Cache) constructor.newInstance(constructorParameters);
        return ds;
    }

    private static void debug(String debugStatement) {
        if(_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE, debugStatement);
        }
    }
}
