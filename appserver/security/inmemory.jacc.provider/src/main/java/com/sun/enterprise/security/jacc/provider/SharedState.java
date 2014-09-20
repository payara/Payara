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

package com.sun.enterprise.security.jacc.provider;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.security.jacc.PolicyContext;
import javax.security.jacc.PolicyContextException;

/**
 * 
 * @author monzillo
 */
public class SharedState {

    //lock on the shared configTable and linkTable
    private static ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock(true);
    private static Lock rLock = rwLock.readLock();
    private static Lock wLock = rwLock.writeLock();
    private static HashMap<String, SimplePolicyConfiguration> configTable =
            new HashMap<String, SimplePolicyConfiguration>();
    private static HashMap<String, HashSet<String>> linkTable =
            new HashMap<String, HashSet<String>>();
    private static final Logger logger =
            Logger.getLogger(SharedState.class.getPackage().getName());

    private SharedState() {
    }

    static Logger getLogger() {
        return logger;
    }

    static SimplePolicyConfiguration lookupConfig(String pcid) {

        SimplePolicyConfiguration pc = null;
        wLock.lock();
        try {
            pc = configTable.get(pcid);
        } finally {
            wLock.unlock();
        }
        return pc;
    }

    static SimplePolicyConfiguration getConfig(String pcid, boolean remove) {

        SimplePolicyConfiguration pc = null;
        wLock.lock();
        try {
            pc = configTable.get(pcid);
            if (pc == null) {
                pc = new SimplePolicyConfiguration(pcid);
                SharedState.initLinks(pcid);
                configTable.put(pcid, pc);
            } else if (remove) {
                SharedState.removeLinks(pcid);
            }
        } finally {
            wLock.unlock();
        }
        return pc;
    }

    static SimplePolicyConfiguration getActiveConfig()
            throws PolicyContextException {
        String pcid = PolicyContext.getContextID();
        SimplePolicyConfiguration pc = null;
        if (pcid != null) {
            rLock.lock();
            try {
                pc = configTable.get(pcid);
                if (pc == null) {
                    /* unknown policy context set on thread
                     * return null to allow checking to be performed with
                     * default context. Should repair improper setting
                     * of context by encompassing runtime.
                     */
                    SimplePolicyConfiguration.logException(Level.WARNING,
                            "invalid policy context id",
                            new PolicyContextException());
                }

            } finally {
                rLock.unlock();
            }
            if (pc != null) {
                if (!pc.inService()) {
                    /* policy context set on thread is not in service 
                     * return null to allow checking to be performed with
                     * default context. Should repair improper setting
                     * of context by encompassing runtime.
                     */
                    SimplePolicyConfiguration.logException(Level.FINEST,
                            "invalid policy context state",
                            new PolicyContextException());
                    pc = null;
                }
            }
        }

        return pc;
    }

    /**
     * Creates a relationship between this configuration and another
     * such that they share the same principal-to-role mappings.
     * PolicyConfigurations are linked to apply a common principal-to-role
     * mapping to multiple seperately manageable PolicyConfigurations,
     * as is required when an application is composed of multiple
     * modules.
     * <P>
     * Note that the policy statements which comprise a role, or comprise
     * the excluded or unchecked policy collections in a PolicyConfiguration
     * are unaffected by the configuration being linked to another.
     * <P>
     * The relationship formed by this method is symetric, transitive
     * and idempotent. 
     * @param id
     * @param otherId
     * @throws javax.security.jacc.PolicyContextException If otherID 
     * equals receiverID. no relationship is formed.
     */
    static void link(String id, String otherId)
            throws javax.security.jacc.PolicyContextException {

        wLock.lock();
        try {

            if (otherId.equals(id)) {
                String msg = "Operation attempted to link PolicyConfiguration to itself.";
                throw new IllegalArgumentException(msg);
            }

// get the linkSet corresponding to this context
            HashSet<String> linkSet = linkTable.get(id);

            // get the linkSet corresponding to the context being linked to this
            HashSet otherLinkSet = linkTable.get(otherId);

            if (otherLinkSet == null) {
                String msg = "Linked policy configuration (" + otherId + ") does not exist";
                throw new RuntimeException(msg);
            }

            Iterator it = otherLinkSet.iterator();

            // for each context (id) linked to the context being linked to this
            while (it.hasNext()) {
                String nextid = (String) it.next();

                //add the id to this linkSet
                linkSet.add(nextid);

                //replace the linkset mapped to all the contexts being linked
                //to this context, with this linkset.
                linkTable.put(nextid, linkSet);
            }

        } finally {
            wLock.unlock();
        }
    }

    static void initLinks(String id) {
        // create a new linkSet with only this context id, and put in the table.
        HashSet linkSet = new HashSet();
        linkSet.add(id);
        linkTable.put(id, linkSet);
    }

    static void removeLinks(String id) {
        wLock.lock();
        try {        // get the linkSet corresponding to this context.
            HashSet linkSet = linkTable.get(id);
            // remove this context id from the linkSet (which may be shared
            // with other contexts), and unmap the linkSet from this context.
            if (linkSet != null) {
                linkSet.remove(id);
                linkTable.remove(id);
            }

            initLinks(id);
        } finally {
            wLock.unlock();
        }

    }

}


