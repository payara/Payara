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
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
// Portions Copyright [2018-2025] [Payara Foundation and/or its affiliates]
package com.sun.enterprise.security.ee.authorization.cache;

import com.sun.enterprise.security.ee.authorization.PolicyProvider;
import com.sun.logging.LogDomains;
import jakarta.security.jacc.Policy;
import jakarta.security.jacc.PolicyContext;
import jakarta.security.jacc.PolicyFactory;
import org.glassfish.exousia.modules.def.DefaultPolicy;
import org.glassfish.exousia.modules.def.DefaultPolicyFactory;

import java.security.AllPermission;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.PrivilegedActionException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;

import static java.util.Collections.list;
import static java.util.logging.Level.SEVERE;

/**
 * This class is
 * 
 * @author Ron Monzillo
 */
public class PermissionCache extends Object {

    private static Logger _logger = LogDomains.getLogger(PermissionCache.class, LogDomains.SECURITY_LOGGER);
    private static Policy policy = PolicyProvider.getInstance();
    private static AllPermission allPermission = new AllPermission();

    private Permissions cache;
    private Permission[] protoPerms;
    private Class<? extends Permission>[] classes;
    private String name;
    private String pcID;
    private final Integer factoryKey;
    private volatile int epoch;
    private volatile boolean loading;
    private ReadWriteLock rwLock;
    private Lock rLock;
    private Lock wLock;

    /*
     * @param key - Integer that uniquely identifies the cache at the factory
     * 
     * @param pcID - a string identifying the policy context and which must be set when getPermissions is called
     * (internally). this value may be null, in which case the permisions of the default policy context will be cached.
     * 
     * @param codesource - the codesource argument to be used in the call to getPermissions. this value may be null.
     * 
     * @param perms - an array of permission objects identifying the permission types that will be managed by the cache.
     * This value may be null. When this argument is not null, only permissions of the types passed in the array or that
     * resolve to the types identified in the will be managed within the cache. When null is passed to this argument,
     * permission type will not be a factor in determining the cached permissions.
     * 
     * @param name - a string corresponding to a value returned by Permission.getName(). Only permissions whose getName()
     * value matches the name parameter will be included in the cache. This value may be null, in which case permission name
     * does not factor into the permission caching.
     */
    public PermissionCache(Integer key, String pcID, Permission[] perms, String name) {
        this.factoryKey = key;
        this.cache = null;
        this.pcID = pcID;
        this.protoPerms = perms;
        if (perms != null && perms.length > 0) {
            this.classes = new Class[perms.length];
            for (int i = 0; i < perms.length; i++) {
                this.classes[i] = perms[i].getClass();
            }
        } else {
            this.classes = null;
        }
        this.name = name;
        this.epoch = 1;
        this.loading = false;
        this.rwLock = new ReentrantReadWriteLock(true);
        this.rLock = rwLock.readLock();
        this.wLock = rwLock.writeLock();
    }
    
    /*
     * USE OF THIS CONSTRUCTOR WITH IS DISCOURAGED PLEASE USE THE Permission (object) based CONSTRUCTOR.
     * 
     * @param key - Integer that uniquely identifies the cache at the factory
     * 
     * @param pcID - a string identifying the policy context and which must be set when getPermissions is called
     * (internally). this value may be null, in which case the permisions of the default policy context will be cached.
     * 
     * @param codesource - the codesource argument to be used in the call to getPermissions. this value may be null.
     * 
     * @param class - a single Class object that identifies the permission type that will be managed by the cache. This
     * value may be null. When this argument is not null, only permissions of the identified type or that resolve to the
     * identified type, will be managed within the cache. When null is passed to this argument, permission type will not be
     * a factor in determining the cached permissions.
     * 
     * @param name - a string corresponding to a value returned by Permission.getName(). Only permissions whose getName()
     * value matches the name parameter will be included in the cache. This value may be null, in which case permission name
     * does not factor into the permission caching.
     */
    public PermissionCache(Integer key, String pcID, Class<?> clazz, String name) {
        this.factoryKey = key;
        this.cache = null;
        this.pcID = pcID;
        this.protoPerms = null;
        if (clazz != null) {
            this.classes = new Class[] { clazz };
        } else {
            this.classes = null;
        }
        this.name = name;
        this.epoch = 1;
        this.loading = false;
        this.rwLock = new ReentrantReadWriteLock(true);
        this.rLock = rwLock.readLock();
        this.wLock = rwLock.writeLock();
    }

    public Integer getFactoryKey() {
        return factoryKey;
    }

    private boolean checkLoadedCache(Permission permission, CachedPermissionImpl.Epoch e) {
        if (e == null) {
            return cache.implies(permission);
        }
        
        if (e.epoch != epoch) {
            e.granted = cache.implies(permission);
            e.epoch = epoch;
        }
        
        return e.granted;
    }

    private boolean checkCache(Permission permissionToCheck, CachedPermissionImpl.Epoch epoch) {

        // Test-and-set to guard critical section
        rLock.lock();
        try {
            if (loading) {
                return false;
            }
            
            if (cache != null) {
                // Cache is loaded and read lock is held.
                // Check permission and return.
                return checkLoadedCache(permissionToCheck, epoch);
            }
        } finally {
            rLock.unlock();
        }

        wLock.lock();
        if (loading) {
            // Another thread started the load
            // release the writelock and return
            wLock.unlock();
            return false;
        }
        
        if (cache != null) {
            // another thread loaded the cache
            // get readlock inside writelock.
            // check permission and return
            rLock.lock();
            wLock.unlock();
            try {
                // cache is loaded and readlock is held
                // check permission and return
                return checkLoadedCache(permissionToCheck, epoch);
            } finally {
                rLock.unlock();
            }
        }
        
        // Set the load indicators so that readers will bypass the cache until it is loaded
        // release the writelock and return
        cache = null;
        loading = true;
        wLock.unlock();
        

        // cache will be null if we proceed past this point
        // NO LOCKS ARE HELD AT THIS POINT

        Permissions nextCache = new Permissions();

        boolean setPc = false;
        String oldpcID = null;
        try {
            oldpcID = PolicyContext.getContextID();
            if (pcID == null || !pcID.equals(oldpcID)) {
                setPc = true;
            }
        } catch (Exception ex) {
            _logger.log(SEVERE, "JACC: Unexpected security exception on access decision", ex);
            return false;
        }

        PermissionCollection pc = null;
        try {
            if (setPc) {
                setPolicyContextID(pcID);
            }
            pc = policy.getPermissionCollection(PolicyContext.get(PolicyContext.SUBJECT));
        } catch (Exception ex) {
            _logger.log(SEVERE, "JACC: Unexpected security exception on access decision", ex);
            return false;
        } finally {
            if (setPc) {
                try {
                    setPolicyContextID(oldpcID);
                } catch (Exception ex) {
                    _logger.log(SEVERE, "JACC: Unexpected security exception on access decision", ex);
                    return false;
                }
            }
        }

        // Force resolution of unresolved permissions so that we can filter out all but the permissions
        // that are supposed to be in the cache.
        resolvePermissions(pc, permissionToCheck);

        for (Permission i : list(pc.elements())) {
            if (i.equals(allPermission)) {
                nextCache.add(i);
            } else {
                boolean classMatch = true;
                if (this.classes != null) {
                    classMatch = false;
                    Class iClazz = i.getClass();
                    for (int j = 0; j < this.classes.length; j++) {
                        if (this.classes[j].equals(iClazz)) {
                            classMatch = true;
                            break;
                        }
                    }
                }
                
                if (classMatch) {
                    if (this.name != null) {
                        String iName = i.getName();
                        if (iName != null && this.name.equals(iName)) {
                            nextCache.add(i);
                        }
                    } else {
                        nextCache.add(i);
                    }
                }
            }
        }

        // Get the writelock to mark cache as loaded
        wLock.lock();
        cache = nextCache;
        loading = false;
        try {
            // Get readlock inside writelock.
            rLock.lock();
            wLock.unlock();
            // cache is loaded and readlock is held
            // check permission and return
            return checkLoadedCache(permissionToCheck, epoch);
        } finally {
            rLock.unlock();
        }
    }

    boolean checkPermission(Permission permission, CachedPermissionImpl.Epoch e) {
        return checkCache(permission, e);
    }

    public boolean checkPermission(Permission permission) {
        return checkCache(permission, null);
    }

    public synchronized void reset() {
        wLock.lock();
        try {
            if (cache != null) {
                // since cache is non-null, we know we are NOT loading
                // setting cache to null will force a (re)load
                cache = null;
                epoch = (epoch + 1 == 0) ? 1 : epoch + 1;
            }
        } finally {
            wLock.unlock();
        }
    }

    private void setPolicyContextID(final String newID) throws PrivilegedActionException {
        PolicyContext.setContextID(newID);
    }

    // Use implies to resolve unresolved permissions
    private void resolvePermissions(PermissionCollection permissionCollection, Permission permission) {
        // Each call to implies will resolve permissions of the
        // argument permission type
        if (protoPerms != null && protoPerms.length > 0) {
            for (int i = 0; i < protoPerms.length; i++) {
                permissionCollection.implies(protoPerms[i]);
            }
        } else {
            permissionCollection.implies(permission);
        }
    }
}
