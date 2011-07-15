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

package com.sun.gjc.spi.base.datastructure;

import com.sun.gjc.spi.base.CacheObjectKey;

/**
 *
 * @author Shalini M
 */
public interface Cache {

    /**
     * Check if an entry is found for this key object. If found, the entry is
     * put in the result object and back into the list.
     * 
     * @param key whose mapping entry is to be checked.
     * @return result object that contains the key with the entry if not busy
     * or null when
     * (1) object not found in cache
     * (2) object found but is busy
     */
    public Object checkAndUpdateCache(CacheObjectKey key);
    
    /**
     * Add key and entry value into the cache.
     * @param key that contains the sql string and its type (PS/CS)
     * @param entry that is the wrapper of PreparedStatement or 
     * CallableStatement
     * @param force If existing key is to be overwritten
     */
    public void addToCache(CacheObjectKey key, Object entry, boolean force);
    
    /**
     * Clear statement cache
     */
    public void clearCache();

    /**
     * Remove all statements stored in the statement cache after closing
     * the statement objects. Used when the statement cache size exceeds 
     * user defined maximum cache size.
     */
    public void purge();
    
    /**
     * Closing all statements in statement cache and flush the statement cache
     * of all the statements. Used when a physical connection to the underlying 
     * resource manager is destroyed.
     */
    public void flushCache();

    /**
     * Get the size of statement cache
     * @return int statement cache size
     */
    public int getSize();
 
    /**
     * Check if the statement cache is synchronized.
     * @return boolean synchronized flag.
     */   
    public boolean isSynchronized();

    /**
     * Remove the specified entry stored in the statement cache after closing
     * the statement object associated with this entry. Used when statement is
     * being reclaimed and is being used for the subsequent requests from the
     * application.
     *
     * @param entry 
     */
    public void purge(Object entry);
}
