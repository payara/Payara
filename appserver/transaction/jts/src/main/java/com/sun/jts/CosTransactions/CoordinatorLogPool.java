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

package com.sun.jts.CosTransactions;

// Import required classes.

import java.util.*;


/**
 * The CoordinatorLogPool is used as a cache for CoordinatorLog objects.
 * This pool allows the re-use of these objects which are very expensive
 * to instantiate.
 *
 * The pool is used by replacing calls to 'new CoordinatorLog()' in the
 * TopCoordinator with calls to CoordinatorLogPool.getCoordinatorLog().
 * The getCoordinatorLog() method attempts to return a CoordinatorLog
 * from the pool. If the pool is empty it instantiates a new
 * CoordinatorLog. 
 *
 * Objects are re-used by calling CoordinatorLogPool.putCoordinatorLog()
 * to return a CoordinatorLog object back to the pool. At this time a
 * check is made to ensure that the internal pool size doesn't exceed a
 * pre set limit. If it does, then the object is discarded and not put
 * back into the pool.
 *
 * The pool was added to improve performance of transaction logging
 *
 * @version 1.00
 *
 * @author Arun Krishnan
 *
 * @see
*/



class CoordinatorLogPool {

    private Stack pool;
    private static final int MAXSTACKSIZE = 3;

    public static CoordinatorLogPool CLPool = new CoordinatorLogPool();
    public static Hashtable CLPooltable = new Hashtable();
    

    /** 
     * constructor
     *
     */
    public CoordinatorLogPool() {
	pool = new Stack();
    }

    /**
     * get a CoordinatorLog object from the cache. Instantiate a
     * new CoordinatorLog object if the cache is empty.
     *
     */
    public static synchronized CoordinatorLog getCoordinatorLog() {
        if (Configuration.isDBLoggingEnabled() || 
            Configuration.isFileLoggingDisabled())
            return null;
	if (CLPool.pool.empty()) {
	    return new CoordinatorLog();
	}
	else {
	    CoordinatorLog cl = (CoordinatorLog) CLPool.pool.pop();
	    return cl;
	}
    }

    /**
     * return a CoordinatorLog object to the cache. To limit the size of
     * the cache a check is made to ensure that the cache doesn't
     * already have more that MAXSTACKSIZE elements. If so the object
     * being returned is discarded.
     *
     */
    public static void putCoordinatorLog(CoordinatorLog cl) {
	if (CLPool.pool.size() <= MAXSTACKSIZE) {
	    CLPool.pool.push(cl);
	} 
    }

    // Added to support delegated recovery: multiple logs should coexist
    public static synchronized CoordinatorLog getCoordinatorLog(String logPath) {
        CoordinatorLogPool clpool = (CoordinatorLogPool)CLPooltable.get(logPath);
        if (clpool == null) {
            clpool = new CoordinatorLogPool();
            CLPooltable.put(logPath,clpool);
        }
        if (clpool.pool.empty()) {
            return new CoordinatorLog(logPath);
        }
        else {
            return (CoordinatorLog)clpool.pool.pop();
        }
    }

    // Added to support delegated recovery: multiple logs should coexist
    public static void putCoordinatorLog(CoordinatorLog cl, String logPath) {
        CoordinatorLogPool clpool = (CoordinatorLogPool)CLPooltable.get(logPath);
        if (clpool == null) {
            clpool = new CoordinatorLogPool();
            CLPooltable.put(logPath,clpool);
        }
        if (clpool.pool.size() <= MAXSTACKSIZE) {
            clpool.pool.push(cl);
        }
    }

}

