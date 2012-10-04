/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.naming.impl;

import org.glassfish.api.naming.GlassfishNamingManager;
import org.glassfish.api.naming.NamedNamingObjectProxy;
import org.glassfish.api.naming.NamespacePrefixes;
import org.glassfish.hk2.api.ServiceHandle;
import org.glassfish.hk2.api.ServiceLocator;

import javax.naming.NamingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;

import static com.sun.enterprise.naming.util.LogFacade.logger;

/**
 * @author Mahesh Kannan
 */
public class NamedNamingObjectManager {

    private static final AtomicReference<ServiceLocator> habitatRef
            = new AtomicReference<ServiceLocator>();

    private static final Map<String, NamedNamingObjectProxy> proxies = new HashMap<String, NamedNamingObjectProxy>();

    private static final ReadWriteLock rwLock = new ReentrantReadWriteLock();

    public static void checkAndLoadProxies(ServiceLocator habitat)
            throws NamingException {
        if (NamedNamingObjectManager.habitatRef.get() != habitat) {
            if (habitat != null) {
                rwLock.writeLock().lock();
                try {
                    if (NamedNamingObjectManager.habitatRef.get() != habitat) {
                        NamedNamingObjectManager.habitatRef.set(habitat);
                        proxies.clear();
                    }
                } finally {
                    rwLock.writeLock().unlock();
                }
            }
        }
    }

    public static Object tryNamedProxies(String name)
            throws NamingException {

        NamedNamingObjectProxy proxy = getCachedProxy(name);
        if (proxy != null) {
            if (logger.isLoggable(Level.FINE)) {
                logger.logp(Level.FINE, "NamedNamingObjectManager", "tryNamedProxies",
                        "found cached proxy [{0}] for [{1}]", new Object[]{proxy, name});
            }
            return proxy.handle(name);
        }

//        for (Binding<?> b : getHabitat().getBindings(new NamingDescriptor())) {
//            for (String prefix : b.getDescriptor().getNames()) {
//                if (name.startsWith(prefix)) {
//                    proxy = (NamedNamingObjectProxy) b.getProvider().get();
//                    System.out.println("NamedNamingObjectManager.tryNamedProxies: found a proxy " + proxy + " for " + name);
//                    cacheProxy(prefix, proxy);
//                    return proxy.handle(name);
//                }
//            }
//        }
        
		for (ServiceHandle<?> inhabitant : getHabitat()
				.getAllServiceHandles(NamespacePrefixes.class)) {
		    List<String> prefixes = inhabitant.getActiveDescriptor().getMetadata().get(GlassfishNamingManager.NAMESPACE_METADATA_KEY);
		    if (prefixes == null) continue;
		    
		    String prefix = null;
		    for (String candidate : prefixes) {
		        if (name.startsWith(candidate)) {
		            prefix = candidate;
		            break;
		        }
		    }
		    
			if (prefix != null) {
				proxy = (NamedNamingObjectProxy) inhabitant.getService();
				if (logger.isLoggable(Level.FINE)) {
					logger.logp(Level.FINE, "NamedNamingObjectManager",
							"tryNamedProxies",
							"found a new proxy [{0}] for [{1}]", new Object[] {
									proxy, name });
				}
				cacheProxy(prefix, proxy);
				return proxy.handle(name);
			}
		}

        return null;
    }

    private static ServiceLocator getHabitat() {
        return habitatRef.get();
    }

    private static NamedNamingObjectProxy getCachedProxy(String name) {
        rwLock.readLock().lock();
        try {
            for (String proxyPrefix : proxies.keySet()) {
                if (name.startsWith(proxyPrefix)) {
                    return proxies.get(proxyPrefix);
                }
            }
        } finally {
            rwLock.readLock().unlock();
        }
        return null;
    }

    private static void cacheProxy(String prefix, NamedNamingObjectProxy proxy) {
        rwLock.writeLock().lock();
        try {
            proxies.put(prefix, proxy);
        } finally {
            rwLock.writeLock().unlock();
        }
    }

}
