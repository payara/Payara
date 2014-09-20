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

package com.sun.jdo.spi.persistence.support.sqlstore.model;

import com.sun.jdo.spi.persistence.support.sqlstore.ConfigCache;
import com.sun.jdo.spi.persistence.support.sqlstore.LogHelperSQLStore;
import com.sun.jdo.spi.persistence.support.sqlstore.PersistenceConfig;
import com.sun.jdo.spi.persistence.support.sqlstore.VersionConsistencyCache;
import com.sun.jdo.spi.persistence.support.sqlstore.ejb.ApplicationLifeCycleEventListener;
import com.sun.jdo.spi.persistence.support.sqlstore.ejb.EJBHelper;
import com.sun.jdo.spi.persistence.support.sqlstore.query.util.type.TypeTable;
import com.sun.jdo.api.persistence.model.Model;
import com.sun.jdo.spi.persistence.utility.logging.Logger;

import java.util.*;

/**
 * Caches SQLStore config information.
 *
 * @author Mitesh Meswani
 */ 
public class ConfigCacheImpl 
        implements ConfigCache, ApplicationLifeCycleEventListener {

    /**
     * Map of class types to PersistenceConfig.
     */
    private Map classConfigs;

    /**
     * Map of OID classes to PersistenceCapable classes.
     */
    private Map oidClassToClassType;

    /**
     * This cache should be notified every time a VC class
     * is loaded or unloaded.
     */
    private VersionConsistencyCache vcCache;

    /**
     * Map of class loaders to a list of PersistenceCapable
     * classes loaded by the classloader.
     */
    private Map classLoaderToClassType;

    /** The logger. */
    protected final static Logger logger = LogHelperSQLStore.getLogger();

    public ConfigCacheImpl() {
        classConfigs = new HashMap();
        classLoaderToClassType = new HashMap();
        oidClassToClassType = new HashMap();

        // Register for call backs on application loader events.        
        EJBHelper.registerApplicationLifeCycleEventListener(this);        
    }

    /**
     * Get the PersistenceConfig for given pcClass. The config is looked up
     * from a cache. If a config can not be found in cache, a new
     * instance is created and returned.
     *
     * @param pcClass The input pcClass.
     * @return PersistenceConfig for given pcClass.
     */
    public synchronized PersistenceConfig getPersistenceConfig(Class pcClass) {
        ClassDesc sqlConfig =
                (ClassDesc) classConfigs.get(pcClass);
        if (sqlConfig == null) {
            // The order of the below operations is important.
            // Initialize is called after puting sqlConfig into the
            // cache so that ClassDesc#fixupForeignReferences does not
            // cause infinite recursion.
            // After the initialisation, oidClassToClassType.put is
            // called so that sqlConfig.getOidClass() returns correct
            // value.

            sqlConfig = ClassDesc.newInstance(pcClass);
            classConfigs.put(pcClass, sqlConfig);
            sqlConfig.initialize(this);
            oidClassToClassType.put(sqlConfig.getOidClass(), pcClass);
            addToClassLoaderMap(pcClass);
        }
        return sqlConfig;
    }

    /**
     * Gets the Class instance corresponding to given oidType.
     *
     * @param oidType The input oidType.
     * @return The Class instance corresponding to given oidType.
     */
    public Class getClassByOidClass(Class oidType) {
        return (Class) oidClassToClassType.get(oidType);
    }

    /**
     * @inheritDoc
     */
    public void notifyApplicationUnloaded(ClassLoader classLoader) {
        boolean debug = logger.isLoggable(Logger.FINEST);

        // Clean up classConfigs and oidClassToClassType for the given
        // classLoader.
        synchronized (this) {
            if (debug) {
                Object[] items = new Object[] {"classLoaderToClassType", classLoaderToClassType.size()};
                logger.finest("sqlstore.model.configcacheimpl.size_before",items); // NOI18N
            }

            List pcClasses = (List) classLoaderToClassType.get(classLoader);
            if (pcClasses != null) {
                if (debug) {
                    Object[] items = new Object[] {"classConfigs", classConfigs.size()};
                    logger.finest("sqlstore.model.configcacheimpl.size_before",items); // NOI18N
                
                    items = new Object[] {"oidClassToClassType", oidClassToClassType.size()};
                    logger.finest("sqlstore.model.configcacheimpl.size_before",items); // NOI18N
                }

                Iterator it = pcClasses.iterator();
                while (it.hasNext()) {
                    Class classType = (Class) it.next();
                    ClassDesc config =
                            (ClassDesc) classConfigs.remove(classType);
                    Class oidClass = config.getOidClass();
                    oidClassToClassType.remove(oidClass);
                    if (config.hasVersionConsistency() && vcCache != null) {
                        vcCache.removePCType(classType);
                    }
                }
                if (debug) {
                    Object[] items = new Object[] {"classConfigs", classConfigs.size()};
                    logger.finest("sqlstore.model.configcacheimpl.size_after",items); // NOI18N
                
                    items = new Object[] {"oidClassToClassType", oidClassToClassType.size()};
                    logger.finest("sqlstore.model.configcacheimpl.size_after",items); // NOI18N
                }

                // Data about this classLoader is no longer needed. 
                // Remove it from cache.  
                classLoaderToClassType.remove(classLoader);
                if (debug) {
                    Object[] items = new Object[] {"classLoaderToClassType", classLoaderToClassType.size()};
                    logger.finest("sqlstore.model.configcacheimpl.size_after",items); // NOI18N
                }

            }
        }

        // Notify others to cleanup
        TypeTable.removeInstance(classLoader);
        Model model = Model.RUNTIME;
        model.removeResourcesFromCaches(classLoader);

    }

    /**   
     * Sets VersionConsistencyCache field.
     *    
     * @param vcCache the VersionConsistencyCache instance. 
     */  
    public synchronized void setVersionConsistencyCache(
            VersionConsistencyCache vcCache) { 
        this.vcCache = vcCache;
    } 

    /**
     * Add pcClass to a classLoaderToClassType map. The only call to
     * this private method is from getPersistenceConfig(). As that method is
     * synchronized we don't need to synchronize here.
     *
     * @param pcClass The pcClass to be added.
     */
    private void addToClassLoaderMap(Class pcClass) {
        ClassLoader classLoader = pcClass.getClassLoader();
        List classes = (List) classLoaderToClassType.get(classLoader);
        if (classes == null) {
            // First entry for a given ClassLoader, initialize the ArrayList.
            classes = new ArrayList();
            classLoaderToClassType.put(classLoader, classes);
        }
        classes.add(pcClass);
    }
}
