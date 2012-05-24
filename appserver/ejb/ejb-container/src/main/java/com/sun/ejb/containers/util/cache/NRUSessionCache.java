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

package com.sun.ejb.containers.util.cache;

import java.util.Properties;

import com.sun.ejb.spi.container.SFSBContainerCallback;

public class NRUSessionCache
    extends LruSessionCache
{ 

    protected boolean doOrdering = false;
    protected int orderingThreshold = 0;

    public NRUSessionCache(String cacheName, 
        SFSBContainerCallback container, int cacheIdleTime, int removalTime)
    {
        super("NRU-" + cacheName, container, cacheIdleTime, removalTime);
    }

    public void init(int maxEntries, float loadFactor, Properties props) {
        super.init(maxEntries, loadFactor, props);
        orderingThreshold = (int) (0.75 * threshold);
    }
    
    protected CacheItem itemAdded(CacheItem item) {
        CacheItem addedItem = super.itemAdded(item);
        doOrdering = (entryCount >= orderingThreshold);
        return addedItem;
    }
    
    protected void itemAccessed(CacheItem item) {
        LruCacheItem lc = (LruCacheItem) item;
        synchronized (this) {
            if (lc.isTrimmed()) {
                lc.setTrimmed(false);
                CacheItem overflow = super.itemAdded(item);
                if (overflow != null) {
                    trimItem(overflow);
                }
            } else if (doOrdering) {
                super.itemAccessed(item);
            }
        }
    }

    protected void itemRefreshed(CacheItem item, int oldSize) {
    }
    
    protected void itemRemoved(CacheItem item) {
        super.itemRemoved(item);
        doOrdering = (entryCount >= orderingThreshold);
    }

    public void trimTimedoutItems(int  maxCount) {
        // If we are maintaining an ordered list use 
        // the superclass method for trimming
        if (doOrdering) {
            super.trimTimedoutItems(maxCount);
        } else {
            // we don't have an ordered list, 
            // so go through the whole cache and pick victims
            trimUnSortedTimedoutItems(maxCount);
        }
    }

}
