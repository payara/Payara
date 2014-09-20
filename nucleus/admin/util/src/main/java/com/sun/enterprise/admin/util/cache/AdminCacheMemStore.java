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

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import org.jvnet.hk2.annotations.Service;

/** In memory {@link AdminCache} containing fixed amount of items. Rotation
 * is based on last update first out.<br/>
 * This implementation is backgrounded by {@link AdminCacheWeakReference} and
 * all non locally cached items are searched from that implementation.
 *
 * @author mmares
 */
public class AdminCacheMemStore implements AdminCache {

     private final static class CachedItem implements Comparable<CachedItem> {

        private Object item;
        private long touched;

        private CachedItem(Object item) {
            this.item = item;
            this.touched = System.currentTimeMillis();
        }

        private Object getItem() {
            this.touched = System.currentTimeMillis();
            return this.item;
        }

        @Override
        public int compareTo(CachedItem o) {
            return (int) (this.touched - o.touched);
        }
        @Override
        public boolean equals(Object o) {
            if(o == null || !( o instanceof CachedItem))
                return false;
            return compareTo((CachedItem) o) == 0;
        }

        @Override
        public int hashCode() {
            return (int)touched;
        }
     }

     private static final AdminCacheMemStore instance = new AdminCacheMemStore();

    /** Maximal count of items in cache. Rotation is based on last used first
     * out.
     */
    private static final int MAX_CACHED_ITEMS_COUNT = 16;

    private final Map<String, CachedItem> cache = new HashMap<String, CachedItem>(MAX_CACHED_ITEMS_COUNT + 1);
    private AdminCacheWeakReference underCache = AdminCacheWeakReference.getInstance();

    private AdminCacheMemStore() {
    }

    @Override
    public <A> A get(String key, final Class<A> clazz) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Attribute key must be unempty.");
        }
        if (clazz == null) {
            throw new IllegalArgumentException("Attribute clazz can not be null.");
        }
        CachedItem ci = cache.get(key);
        if (ci == null) {
            A item = underCache.get(key, clazz);
            if (item != null) {
                putToLocalCache(key, new CachedItem(item));
            }
            return item;
        } else {
            return (A) ci.getItem();
        }
    }

    @Override
    public void put(String key, final Object data) {
        underCache.put(key, data);
        putToLocalCache(key, new CachedItem(data));
    }

    private synchronized void putToLocalCache(String key, final CachedItem ci) {
        if (cache.size() >= MAX_CACHED_ITEMS_COUNT) {
            CachedItem oldest = null;
            Collection<CachedItem> values = cache.values();
            for (CachedItem item : values) {
                if (oldest == null) {
                    oldest = item;
                    continue;
                }
                if (item.touched < oldest.touched) {
                    oldest = item;
                }
            }
            values.remove(oldest);
        }
        cache.put(key, ci);
    }

    @Override
    public boolean contains(String key) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Attribute key must be unempty.");
        }
        CachedItem item = cache.get(key);
        if (item != null) {
            item.getItem(); //Just for touch
            return true;
        }
        return underCache.contains(key);
    }

    @Override
    public Date lastUpdated(String key) {
        return underCache.lastUpdated(key);
    }

    public static AdminCacheMemStore getInstance() {
        return instance;
    }

}
