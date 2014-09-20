/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.appserv.util.cache.Cache;
import com.sun.appserv.util.cache.CacheListener;
import com.sun.appserv.util.cache.Constants;
import com.sun.appserv.util.cache.LruCache;
import com.sun.logging.*;

import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.logging.*;
import org.glassfish.ejb.LogFacade;
import org.glassfish.logging.annotation.LogMessageInfo;

/**
 * LRUCache
 * in-memory bounded cache with an LRU list
 */
public class LruEJBCache extends LruCache {

    protected static final Logger _logger  = LogFacade.getLogger();

    @LogMessageInfo(
        message = "[{0}]: trimLru(), resetting head and tail",
        level = "WARNING")
    private static final String TRIM_LRU_RESETTING_HEAD_AND_TAIL = "AS-EJB-00001";

    protected String cacheName;

    /**
     * default constructor
     */
    public LruEJBCache() { }

    @Override
    protected CacheItem trimLru(long currentTime) {

        LruCacheItem trimItem = tail;

        if (tail != head) {
            tail = trimItem.getLPrev();
            if (tail == null) {
                _logger.log(Level.WARNING, TRIM_LRU_RESETTING_HEAD_AND_TAIL, cacheName);
                // do not let the tail go past the head
                tail = head = null;
            } else {
                tail.setLNext(null);
            }
        } else {
            tail = head = null;
        }
        
        if (trimItem != null) {
            trimItem.setTrimmed(true);
            trimItem.setLPrev(null);
            trimCount++;
            listSize--;
        }

        return trimItem;
    }

    @Override
    protected CacheItem itemAdded(CacheItem item) {
        boolean wasUnbounded = isUnbounded;
        CacheItem overflow = null;

        // force not to check
        isUnbounded = false;
        try {
            overflow = super.itemAdded(item);
        } finally {
            //restore
            isUnbounded = wasUnbounded;
        }

        return overflow;
    }

    public void setCacheName(String name) {
        this.cacheName = name;
    }
}
