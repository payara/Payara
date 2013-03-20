/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 Oracle and/or its affiliates. All rights reserved.
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
package com.sun.enterprise.admin.util.cache;

import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/** {@link AdminCache} based on week references and backgrounded by
 * {@link AdminCacheFileStore} layer. <br/>
 * Max one object representation of cached data are stored by this
 * implementation. If different type is requested, it will be reloaded from
 * file store using {@code AdminCacheFileStore}.<br/>
 *
 * @author mmares
 */
public class AdminCacheWeakReference implements AdminCache {

    private final static class CachedItem {

        private WeakReference item;
        private long updated = -1;
        private Date lastUpdateInStore;

        private CachedItem(final Object item) {
            setItem(item);
        }

        private CachedItem(final Object item, final Date lastUpdateInStore) {
            setItem(item);
            this.lastUpdateInStore = lastUpdateInStore;
        }

        private CachedItem(final Date lastUpdateInStore) {
            this.lastUpdateInStore = lastUpdateInStore;
        }

        public Object getItem() {
            if (item == null) {
                return null;
            } else {
                return item.get();
            }
        }

        public void setItem(Object item) {
            if (item == null) {
                this.item = null;
            } else {
                this.item = new WeakReference(item);
            }
            this.updated = System.currentTimeMillis();
        }

        public Date getLastUpdateInStore() {
            return lastUpdateInStore;
        }

        public void setLastUpdateInStore(Date lastUpdateInStore) {
            this.lastUpdateInStore = lastUpdateInStore;
        }

        public long getUpdated() {
            return updated;
        }

    }

    private static final AdminCacheWeakReference instance = new AdminCacheWeakReference();

    private AdminCacheFileStore fileCache = AdminCacheFileStore.getInstance();
    private final Map<String, CachedItem> cache = new HashMap<String, CachedItem>();

    private AdminCacheWeakReference() {
    }

    @Override
    public <A> A get(final String key, final Class<A> clazz) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Attribute key must be unempty.");
        }
        if (clazz == null) {
            throw new IllegalArgumentException("Attribute clazz can not be null.");
        }
        CachedItem cachedItem = cache.get(key);
        if (cachedItem != null) {
            Object obj = cachedItem.getItem();
            if (obj != null) {
                if (clazz.isAssignableFrom(obj.getClass())) {
                    return (A) obj;
                }
            }
        }
        //Not in local cache => load from underliing
        A item = fileCache.get(key, clazz);
        if (item != null) {
            if (cachedItem == null) {
                cache.put(key, new CachedItem(item));
            } else {
                cachedItem.setItem(item);
            }
        } else {
            cache.remove(key);
        }
        return item;
    }

    @Override
    public void put(final String key, final Object data) {
        fileCache.put(key, data);
        cache.put(key, new CachedItem(data, new Date()));
    }

    @Override
    public boolean contains(final String key) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Attribute key must be unempty.");
        }
        CachedItem item = cache.get(key);
        if (item != null) {
            return true;
        } else {
            boolean result = fileCache.contains(key);
            if (result) {
                cache.put(key, new CachedItem(null));
            }
            return result;
        }
    }

    @Override
    public Date lastUpdated(String key) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Attribute key must be unempty.");
        }
        CachedItem item = cache.get(key);
        if (item != null && item.lastUpdateInStore != null) {
            return item.lastUpdateInStore;
        }
        Date result = fileCache.lastUpdated(key);
        if (result != null) {
            if (item == null) {
                cache.put(key, new CachedItem(result));
            } else {
                if (item.updated != -1 && item.updated < result.getTime()) {
                    item.setItem(null); //Cleare it because it was changed after load
                }
                item.lastUpdateInStore = result;
            }
        }
        return result;
    }

    public static AdminCacheWeakReference getInstance() {
        return instance;
    }

}
