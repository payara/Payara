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

package com.sun.enterprise.security.ee;

import com.sun.enterprise.security.ee.J2EESecurityManager;
import java.security.CodeSource;
import java.security.Permission;
import java.security.Policy;
import java.util.Hashtable;
import java.util.Iterator;

/**
 * This class is the factory for creating and managing PermissionCache.
 * @author Shing Wai Chan
 */

public class PermissionCacheFactory {

    private static final Hashtable cacheMap = new Hashtable();
    private static int factoryKey = 0;
    private static boolean supportsReuse = false;

    private static Permission[] protoPerms = {
	new java.net.SocketPermission("localhost","connect"),
	new java.util.PropertyPermission("x","read")
    };
 
    private static PermissionCache securityManagerCache = 
	createSecurityManagerCache();

    static {
        try {
	    // make a call to policy.refresh() to see if the provider
	    // calls the supportsReuse callback (see resetCaches below).
	    // which will set supportsReuse to true (to enable caching).
	    Policy policy = Policy.getPolicy();
	    if (policy != null) {
		policy.refresh();
	    }
        } catch(Exception pe) {
        }
    }

    /**
     * Reserve the next Cache Key for subsequent registration.
     * @return the key as an Integer object.
     */
    private static Integer getNextKey() {
   
   	Integer key = Integer.valueOf(factoryKey++);
   
   	while (cacheMap.get(key) != null) {
   	    key = Integer.valueOf(factoryKey++);
   	}
   	
  	return key;
    }

    private static synchronized PermissionCache createSecurityManagerCache() {

        Integer key = getNextKey();

	PermissionCache cache = 

	    new PermissionCache(key, null, null, protoPerms, null);

        return registerPermissionCache(cache);
    }

    /**
     * Create a PermissionCache object.
     * If the corresponding object exists, then it will overwrite the
     * previous one.
     * @param pcID - a string identifying the policy context and which must
     *               be set when getPermissions is called (internally).
     *               This value may be null, in which case the permisions of
     *               the default policy context will be cached.
     * @param codesource - the codesource argument to be used in the call
     *                     to getPermissions.  This value may be null.
     * @param perms - an array of Permission objects identifying the
     *                  permission types that will be managed by the cache.
     *                  This value may be null, in which case all permissions
     *                  obtained by the getPermissions call will be cached.
     * @param name - a string corresponding to a value returned by
     *               Permission.getName() only permissions whose getName()
     *               value matches the name parameter will be included
     *               in the cache. This value may be null, in which case
     *               permission name dos not factor into the
     *               permission caching.
     */
    public static synchronized PermissionCache 
    createPermissionCache(String pcID,
	    CodeSource codesource, Permission[] perms, String name) {

        if (!supportsReuse) {
            return null;
        }

        Integer key = getNextKey();

	PermissionCache cache = 
	    new PermissionCache(key, pcID, codesource, perms, name);

        return registerPermissionCache(cache);
    }

    /**
     * Create a PermissionCache object.
     * If the corresponding object exists, then it will overwrite the
     * previous one.
     * @param pcID - a string identifying the policy context and which must
     *               be set when getPermissions is called (internally).
     *               This value may be null, in which case the permisions of
     *               the default policy context will be cached.
     * @param codesource - the codesource argument to be used in the call
     *                     to getPermissions.  This value may be null.
     * @param clazz - a class object identifying the
     *                  permission type that will be managed by the cache.
     *                  This value may be null, in which case all permissions
     *                  obtained by the getPermissions call will be cached.
     * @param name - a string corresponding to a value returned by
     *               Permission.getName() only permissions whose getName()
     *               value matches the name parameter will be included
     *               in the cache. This value may be null, in which case
     *               permission name dos not factor into the
     *               permission caching.
     */
    public static synchronized PermissionCache 
    createPermissionCache(String pcID,
	    CodeSource codesource, Class clazz, String name) {
  
	if (!supportsReuse) {
	    return null;
	}
  
	Integer key = getNextKey();
 
 	PermissionCache cache = 
 	    new PermissionCache(key, pcID, codesource, clazz, name);
	
 	return registerPermissionCache(cache);
    }
 
     /**
      * Register a PermissionCache object with the factory. If an object is 
      * already registered at the key, it will be overidden.
      * @param cache a cache with an internal key value.
      * @return the cache object
      */
    private static PermissionCache 
    registerPermissionCache(PermissionCache cache) {
	cacheMap.put(cache.getFactoryKey(),cache);
    
	return cache;
    }

    public static synchronized PermissionCache
    removePermissionCache(PermissionCache cache) {
  
	PermissionCache rvalue = null;
  
	if (cache != null) {
	    
	    Object value = cacheMap.remove(cache.getFactoryKey());
  
	    if (value != null && value instanceof PermissionCache) {
		rvalue = (PermissionCache) value;
		rvalue.reset();
	    }
	}
	return rvalue;
    }

    /**
     * This resets all caches inside the factory.
     */
    public static synchronized void resetCaches() {

	supportsReuse = true;

	java.lang.SecurityManager sm = System.getSecurityManager();
	if (sm != null && sm instanceof J2EESecurityManager) {
	    if (!((J2EESecurityManager)sm).cacheEnabled()) {
		((J2EESecurityManager)sm).enablePermissionCache
		    (securityManagerCache);
	    }
	}

	Iterator iter = cacheMap.values().iterator();
	while (iter.hasNext()) {
  	    Object cache = iter.next();
  	    if (cache instanceof PermissionCache) {
  		((PermissionCache) cache).reset();
  	    }
	}
    }
}
