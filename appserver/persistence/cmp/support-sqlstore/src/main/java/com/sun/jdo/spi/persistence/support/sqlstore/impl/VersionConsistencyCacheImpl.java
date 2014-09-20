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

package com.sun.jdo.spi.persistence.support.sqlstore.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;

import com.sun.appserv.util.cache.Cache;
import com.sun.appserv.util.cache.CacheListener;
import com.sun.appserv.util.cache.LruCache;

import com.sun.jdo.spi.persistence.support.sqlstore.LogHelperPersistenceManager;
import com.sun.jdo.spi.persistence.support.sqlstore.StateManager;
import com.sun.jdo.spi.persistence.support.sqlstore.VersionConsistencyCache;

import com.sun.jdo.spi.persistence.utility.BucketizedHashtable;
import com.sun.jdo.spi.persistence.utility.logging.Logger;
import org.glassfish.persistence.common.I18NHelper;


/**
 * A 2-level cache of StateManager instances (i.e., a map of maps).  The inner
 * map is a BucketizedHashtable or a LRU cache, depending on parameter given at
 * construction.
 *
 * @author Dave Bristor
 */
public class VersionConsistencyCacheImpl implements VersionConsistencyCache {
    /** The outermost map of the two-level cache. */
    private final Map pcTypeMap = new HashMap();

    /** Used to create different kinds of caches. */
    // Not final, so that we can create different kinds of caches for testing.
    private static CacheFactory cacheFactory;

    private final static ResourceBundle messages =
        I18NHelper.loadBundle(VersionConsistencyCacheImpl.class); 

    /** Use the PersistenceManager's logger. */
    private static Logger logger = LogHelperPersistenceManager.getLogger();

    /** Name of implementation class of LRU cache. */
    private static final String LRU_CACHE_CLASSNAME =
      "com.sun.appserv.util.cache.LruCache"; // NOI18aN


    //
    // Cache configuration controls
    //

    /** Prefix of each property name of configuration item. */
    private static final String PROPERTY_PREFIX =
        "com.sun.jdo.spi.persistence.support.sqlstore.impl.VersionConsistency."; // NOI18N


    /** Determines whether to use LruCache or the default. */
    private static boolean lruCache = false;

    /** Name of property to choose LRU or basic cache. */
    private static final String LRU_CACHE_PROPERTY = PROPERTY_PREFIX + "LruCache"; // NOI18N


    /** For both LruCache and BucketizedHashtable. */
    private static float loadFactor = 0.75F;

    /** Name of property for specifying loadFactor. */
    private static final String LOAD_FACTOR_PROPERTY =
        PROPERTY_PREFIX + "loadFactor"; // NOI18N


    /** For BucketizedHashtable only. */
    private static int bucketSize = 13;

    /** Name of property for specifying bucketSize. */
    private static final String BUCKET_SIZE_PROPERTY =
        PROPERTY_PREFIX + "bucketSize"; // NOI18N


    /** For BucketizedHashtable only. */
    private static int initialCapacity = 131;

    /** Name of property for specifying initialCapacity. */
    private static final String INITIAL_CAPACITY_PROPERTY =
        PROPERTY_PREFIX + "initialCapacity"; // NOI18N


    /** For LruCache only. */
    private static int maxEntries = 131;

    /** Name of property for specifying maxEntries. */
    private static final String MAX_ENTRIES_PROPERTY =
        PROPERTY_PREFIX + "maxEntries"; // NOI18N


     /** LruCache only, 10 minute timeout */
    private static long timeout = 1000L * 60 * 10;
    
    /** Name of property for specifying timeout. */
    private static final String TIMEOUT_PROPERTY =
        PROPERTY_PREFIX + "timeout"; // NOI18N

    // Create the cache factory
    static {
        cacheFactory = createCacheFactory();
    }
    

    /** Empty default constructor. */
    private VersionConsistencyCacheImpl() {
    }

    /** Creates a cache with desired performance.  This constructor is
     * expected to be used for unit testing ONLY.
     * @param highPerf If true, use LruCache, else use BucketizedHashtable.
     */
    protected VersionConsistencyCacheImpl(boolean highPerf) {
        if (highPerf) {
            cacheFactory = new LruCacheFactory();
        } else {
            cacheFactory = new BasicCacheFactory();
        }
    }

    /**
     * Create a cache.  The performance characteristics of the cache depends
     * on the setting of the runtime properties.  If the flag
     * <code>com.sun.jdo.spi.persistence.support.sqlstore.impl.VersionConsistency.LruCache</code>
     * is true, then the LruCache cache is used.  If it has some other value,
     * the BucketizedHashtable cache is used.  If not set, but we can load 
     * the LruCache class, the LruCache cache is used.  Otherwise, we use
     * the BucketizedHashtable cache.  Other properties control particulars
     * of those two caches.
     */
    static VersionConsistencyCache create() {
        return new VersionConsistencyCacheImpl();
    }

    /**
     * Create a CacheFactory.  Uses system properties to determine what kind of
     * cache will be returned by the factory.
     */
    static CacheFactory createCacheFactory() {
        CacheFactory rc = null;
        
        loadFactor = getFloatValue(LOAD_FACTOR_PROPERTY, loadFactor);

        bucketSize = getIntValue(BUCKET_SIZE_PROPERTY, bucketSize);

        initialCapacity = getIntValue(INITIAL_CAPACITY_PROPERTY, initialCapacity);

        maxEntries = getIntValue(MAX_ENTRIES_PROPERTY, maxEntries);

        timeout = getLongValue(TIMEOUT_PROPERTY, timeout);

        // Determine whether to use LRU cache or not.
        boolean lruCache = false;
        try {
            
            // Don't use Boolean.getBoolean, because we want to know if the
            // flag is given or not.
            String s = System.getProperty(LRU_CACHE_PROPERTY);
            if (s != null) {
                lruCache = Boolean.valueOf(s).booleanValue();
                if (lruCache) {
                    
                    // If user specifies lruCache, but it is not available,
                    // log a WARNING and use the basic cache.
                    try {
                        Class.forName(LRU_CACHE_CLASSNAME);
                    } catch (Exception ex) {
                        logger.warning(
                            I18NHelper.getMessage(
                                messages,
                                "jdo.versionconsistencycacheimpl.lrucachenotfound")); // NOI18N
                        lruCache = false;
                    }
                }
                
            } else {
                // No flag given: Try to load LRU cache
                try {
                    Class.forName(LRU_CACHE_CLASSNAME);
                    lruCache = true;
                } catch (Exception ex) {
                    // LRU cache not found, so use default
                }
            }
        } catch (Exception ex) {
            
            // This probably should not happen, but fallback to the
            // default cache just in case.
            lruCache = false;
            logger.warning(
                I18NHelper.getMessage(
                    messages,
                    "jdo.versionconsistencycacheimpl.unexpectedduringcreate", ex));// NOI18N
        }

        if (lruCache) {
            rc = new LruCacheFactory();
        } else {
            rc = new BasicCacheFactory();
        }

        if (logger.isLoggable(Logger.FINER)) {
            String values =
                "\nloadFactor= " + loadFactor // NOI18N
                + "\nbucketSize= " + bucketSize // NOI18N
                + "\ninitialCapacity=" + initialCapacity // NOI18N
                + "\nmaxEntries=" + maxEntries // NOI18N
                + "\ntimeout=" + timeout // NOI18N
                + "\nlruCache=" + lruCache; // NOI18N
            logger.finer(
                I18NHelper.getMessage(
                    messages,
                    "jdo.versionconsistencycacheimpl.created",
                    values)); // NOI18N
        }

        return rc;
    }

    /**
     * Returns the value for the given property name.  If not available,
     * returns the default value.  If the property's value cannot be parsed as
     * an integer, logs a warning.
     * @param propName Name of property for value
     * @param defaultVal Default value used if property is not set.
     * @return value for the property.
     */
    private static int getIntValue(String propName, int defaultVal) {
        int rc = defaultVal;
        String valString = System.getProperty(propName);
        if (null != valString && valString.length() > 0) {
            try {
                rc = Integer.parseInt(valString);
            } catch (NumberFormatException ex) {
                logBadConfigValue(propName, valString);
            }
        }
        return rc;
    }

    /**
     * Returns the value for the given property name.  If not available,
     * returns the default value.  If the property's value cannot be parsed as
     * a float, logs a warning.
     * @param propName Name of property for value
     * @param defaultVal Default value used if property is not set.
     * @return value for the property.
     */
    private static float getFloatValue(String propName, float defaultVal) {
        float rc = defaultVal;
        String valString = System.getProperty(propName);
        if (null != valString && valString.length() > 0) {
            try {
                rc = Float.parseFloat(valString);
            } catch (NumberFormatException ex) {
                logBadConfigValue(propName, valString);
            }
        }
        return rc;
    }

    /**
     * Returns the value for the given property name.  If not available,
     * returns the default value.  If the property's value cannot be parsed as
     * a long, logs a warning.
     * @param propName Name of property for value
     * @param defaultVal Default value used if property is not set.
     * @return value for the property.
     */
    private static long getLongValue(String propName, long defaultVal) {
        long rc = defaultVal;
        String valString = System.getProperty(propName);
        if (null != valString && valString.length() > 0) {
            try {
                rc = Long.parseLong(valString);
            } catch (NumberFormatException ex) {
                logBadConfigValue(propName, valString);
            }
        }
        return rc;
    }

    /**
     * Logs a warning that the property's value is invalid.
     * @param propName Name of property
     * @param valString Value of property as a String.
     */
    private static void logBadConfigValue(String propName, String valString) {
        logger.warning(
            I18NHelper.getMessage(
                messages,
                "jdo.versionconsistencycacheimpl.badconfigvalue", // NOI18N
                propName, valString));
    }

    /**
     * @see VersionConsistencyCache#put
     */
    public StateManager put(Class pcType, Object oid, StateManager sm) {
        boolean logAtFinest = logger.isLoggable(Logger.FINEST);

        if (logAtFinest) {
            logger.finest(
                    I18NHelper.getMessage(
                            messages,
                            "jdo.versionconsistencycacheimpl.put.entering", // NOI18N
                            new Object[] {pcType, oid, sm}));
        }

        StateManager rc = null;
        VCCache oid2sm = null;
        synchronized (pcTypeMap) {
            oid2sm = (VCCache) pcTypeMap.get(pcType);
    
            if (null == oid2sm) {
                oid2sm = cacheFactory.create();
                pcTypeMap.put(pcType, oid2sm);
            }
        }

        rc = oid2sm.put(oid, sm);

        if (logAtFinest) {
            logger.finest(
                    I18NHelper.getMessage(
                            messages,
                            "jdo.versionconsistencycacheimpl.put.returning", // NOI18N
                            rc));
        }

        return rc;
    }

    /**
     * @see VersionConsistencyCache#get
     */
    public StateManager get(Class pcType, Object oid) {
        boolean logAtFinest = logger.isLoggable(Logger.FINEST);

        if (logAtFinest) {
            logger.finest(
                    I18NHelper.getMessage(
                            messages,
                            "jdo.versionconsistencycacheimpl.get.entering", // NOI18N
                            new Object[] {pcType, oid}));
        }
        StateManager rc = null;

        VCCache oid2sm = null;
        synchronized (pcTypeMap) {
            oid2sm = (VCCache) pcTypeMap.get(pcType);
        }
    
        if (null != oid2sm) {
            rc = oid2sm.get(oid);
        }

        if (logAtFinest) {
            logger.finest(
                    I18NHelper.getMessage(
                            messages,
                            "jdo.versionconsistencycacheimpl.get.returning", // NOI18N
                            rc));
        }
        return rc;
    }


    /**
     * @see VersionConsistencyCache#remove
     */
    public StateManager remove(Class pcType, Object oid) {
        boolean logAtFinest = logger.isLoggable(Logger.FINEST);

        if (logAtFinest) {
            logger.finest(
                    I18NHelper.getMessage(
                            messages,
                            "jdo.versionconsistencycacheimpl.remove.entering", // NOI18N
                            new Object[] {pcType, oid}));
        }

        StateManager rc = null;
        synchronized (pcTypeMap) {
            VCCache oid2sm = (VCCache) pcTypeMap.get(pcType);

            if (null != oid2sm) {
                rc = (StateManager) oid2sm.remove(oid);
                if (oid2sm.isEmpty()) {
                    pcTypeMap.remove(pcType);
                }
            }
        }

        if (logAtFinest) {
            logger.finest(
                    I18NHelper.getMessage(
                            messages,
                            "jdo.versionconsistencycacheimpl.remove.returning", // NOI18N
                            rc));
        }
        return rc;
    }

    /**
     * This implementation does nothing.  Instead, we create buckets for each
     * pcType as-needed; see {@link #put}
     */
    public void addPCType(Class pcType) {
        if (logger.isLoggable(Logger.FINEST)) {
            logger.finest(
                    I18NHelper.getMessage(
                            messages,
                            "jdo.versionconsistencycacheimpl.addpctype", // NOI18N
                            pcType));
        }
        // Intentionally empty
    }

    /**
     * @see VersionConsistencyCache#removePCType
     */
    public void removePCType(Class pcType) {
        if (logger.isLoggable(Logger.FINEST)) {
            logger.finest(
                    I18NHelper.getMessage(
                            messages,
                            "jdo.versionconsistencycacheimpl.removepctype", // NOI18N
                            pcType));
        }

        synchronized (pcTypeMap) {
            VCCache oid2sm = (VCCache) pcTypeMap.get(pcType);

            if (null != oid2sm) {
                oid2sm.clear();
            }
            pcTypeMap.remove(pcType);
        }
    }

    /**
     * @return the number of elements in the cache.
     */
    public int size() {
        int rc = 0;
        synchronized (pcTypeMap) {
            for (Iterator i = pcTypeMap.keySet().iterator(); i.hasNext();) {
                VCCache oid2sm = (VCCache) pcTypeMap.get(i.next());
                rc += oid2sm.size();
            }
        }
        return rc;                
    }

    /**
     * @return true if this cache is based on LRU cache; false otherwise.
     */
    public boolean isHighPerf() {
        return LruCacheFactory.class.equals(cacheFactory.getClass());
    }
    
    //
    // Support for the inner map.  It is either HashMap- or Cache- based.
    //

    /** Provides cache operations of put, get, and remove. */
    interface VCCache {
        /** @see Map#put */
        public StateManager put(Object key, StateManager value);

        /** @see Map#get */
        public StateManager get(Object key);

        /** @see Map#remove */
        public StateManager remove(Object key);

        /** @see Map#clear */
        public void clear();

        /** @see Map#isEmpty */
        public boolean isEmpty();

        /** @see Map#size */
        public int size();
    }

    /**
     * VCCache that is HashMap-based.  The methods are not synchronized but
     * the underlying implemention <em>is</em>synchronized.
     */
    static class BasicVCCache implements VCCache {
        private final Map cache;
        
        BasicVCCache() {
            if (logger.isLoggable(Logger.FINER)) {
                logger.finer(
                        I18NHelper.getMessage(
                                messages,
                                "jdo.versionconsistencycacheimpl.usinghashmap", // NOI18N
                                new Object[] {
                                    new Integer(bucketSize),
                                    new Long(initialCapacity),
                                    new Float(loadFactor)}));
            }

            cache = Collections.synchronizedMap(
                    new BucketizedHashtable(
                            bucketSize, initialCapacity, loadFactor));
        }
        
        /** @see Map#put */
        public StateManager put(Object key, StateManager value) {
            return (StateManager) cache.put(key, value);
        }

        /** @see Map#get */
        public StateManager get(Object key) {
            return (StateManager) cache.get(key);
        }

        /** @see Map#remove */
        public StateManager remove(Object key) {
            return (StateManager) cache.remove(key);
        }
        
        /** @see Map#clear */
        public void clear() {
            cache.clear();
        }

        /** @see Map#isEmpty */
        public boolean isEmpty() {
            return cache.isEmpty();
        }

        /** @see Map#size */
        public int size() {
            return cache.size();
        }
    }
        
    /**
     * VCCache that uses LRU cachd.  Methods are not synchronized, but
     * underlying cache implementation <em>is</em>.
     */
    static class LruVCCache implements VCCache {
        /**
         * We can't use the interface type Cache because we need to be able to
         * clear out the cache, which is only supported by the implementation.
         */
        private final Cache cache;

        /**
         * @param maxEntries maximum number of entries expected in the cache
         * @param loadFactor the load factor
         * @param timeout to be used to trim the expired entries
         */
        LruVCCache(int maxEntries, long timeout, float loadFactor) {
            if (logger.isLoggable(Logger.FINER)) {
                logger.finer(
                        I18NHelper.getMessage(
                                messages,
                                "jdo.versionconsistencycacheimpl.usinglrucache", // NOI18N
                                new Object[] {
                                    new Integer(maxEntries),
                                    new Long(timeout),
                                    new Float(loadFactor)}));
            }

            LruCache c = new LruCache();
            c.init(maxEntries, timeout, loadFactor, (Properties) null);
            c.addCacheListener(
                    new CacheListener() {
                        public void trimEvent(Object key, Object value) {
                            cache.remove(key);
                            if (logger.isLoggable(Logger.FINER)) {
                                logger.finer(
                                        I18NHelper.getMessage(
                                                messages,
                                                "jdo.versionconsistencycacheimpl.trimevent")); // NOI18N
                            }
                        }
                    });
            cache = c;
        }
        
        /** @see Map#put */
        public StateManager put(Object key, StateManager value) {
            return (StateManager) cache.put(key, value);
        }

        /** @see Map#get */
        public StateManager get(Object key) {
            return (StateManager) cache.get(key);
        }

        /** @see Map#remove */
        public StateManager remove(Object key) {
            return (StateManager) cache.remove(key);
        }

        /** @see Map#clear */
        public void clear() {
            cache.clear();
        }

        /** @see Map#isEmpty */
        public boolean isEmpty() {
            return cache.isEmpty();
        }

        /** @see Map#size */
        public int size() {
            return cache.getEntryCount();
        }
    }

    //
    // Factory for creating VCCache instances.
    //

    /** Provides for creating an instance of a VCCache. */
    interface CacheFactory {
        /** @return an instance of a VCCache. */
        public VCCache create();
    }

    /** Provides for creating an instance of a BasicVCCache. */
    static class BasicCacheFactory implements CacheFactory {

        /** @return an instance of a BasicVCCache. */
        public VCCache create() {
            return new BasicVCCache();
        }
    }

    /** Provides for creating an instance of a LruVCCache. */
    static class LruCacheFactory implements CacheFactory {

        /** @return an instance of a LruVCCache. */
        public VCCache create() {
            return new LruVCCache(maxEntries, timeout, loadFactor);
        }
    }
}
