/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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
package com.sun.enterprise.v3.admin;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.glassfish.api.admin.AdminCommandInstance;
import org.glassfish.api.admin.AdminCommandInstanceRegistry;
import org.glassfish.api.admin.AdminCommandState;
import org.jvnet.hk2.annotations.Service;

/**
 *  This is the implementation for the JobManagerService
 *  The JobManager is responsible
 *  1. generating unique ids for jobs
 *  2. serving as a registry for jobs
 *  3. creating threadpools for jobs
 *
 * @author Martin Mares
 * @author Bhakti Mehta
 */

@Service
public class JobManagerService implements AdminCommandInstanceRegistry {
    
    private static final int MAX_SIZE = 65535;
    
    private HashMap<String, AdminCommandInstance> map = new HashMap<String, AdminCommandInstance>();
    private HashSet<String> ids = new HashSet<String>();
    private AtomicInteger lastId = new AtomicInteger(0);

    
    protected synchronized String getNewId() {
        int nextId = lastId.incrementAndGet();
        if (nextId > MAX_SIZE) {
            reset();
        }
        String nextIdToUse = String.valueOf(nextId);
        return !idInUse(nextIdToUse) ? String.valueOf(nextId): getNewId();
    }


    private void reset() {
        lastId.set(0);
    }

    private boolean idInUse(String id) {
        return map.containsKey(id) ;
    }

    @Override
    public AdminCommandInstance createCommandInstance(String name) {
        return new AdminCommandInstanceImpl(getNewId(),name);
    }

    @Override
    public synchronized void register(AdminCommandInstance instance) throws IllegalArgumentException {
        if (instance == null) {
            throw new IllegalArgumentException("Argument instance can not be null");
        }
        if (map.containsKey(instance.getId())) {
            throw new IllegalArgumentException("Instance id is already in use.");
        }     
       
        map.put(instance.getId(), instance);
        ids.add(instance.getId());
        if (instance instanceof AdminCommandInstanceImpl) {
            ((AdminCommandInstanceImpl) instance).setState(AdminCommandState.State.RUNNING);
        }
    }

    @Override
    public Iterator<AdminCommandInstance> iterator() {
        return map.values().iterator();
    }

    @Override
    public AdminCommandInstance get(String id) {
        return map.get(id);
    }
    
}
