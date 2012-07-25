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
package com.sun.enterprise.v3.admin.instance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import org.glassfish.api.admin.AdminCommandInstance;
import org.glassfish.api.admin.AdminCommandInstanceRegistry;
import org.glassfish.api.admin.AdminCommandState;
import org.jvnet.hk2.annotations.Service;

/** Basic implementation.
 *
 * @author mmares
 */
//TODO: Will be replaced with Bhakti's JobManager. Now only for SSE early implementation
@Service
public class AdminCommandInstanceRegistryImpl implements AdminCommandInstanceRegistry {
    
    private static final int MAX_SIZE = 36;
    private static final int MAX_ID = Character.MAX_RADIX ^ 4;
    
    private HashMap<String, AdminCommandInstance> map = new HashMap<String, AdminCommandInstance>();
    private List<String> ids = new ArrayList<String>(MAX_SIZE);
    private int lastId;

    public AdminCommandInstanceRegistryImpl() {
        lastId = (int) (System.currentTimeMillis() % MAX_ID); 
    }
    
    protected synchronized String getNewId() {
        lastId++;
        if (lastId > MAX_ID) {
            lastId = 0;
        }
        return Integer.toString(lastId, Character.MAX_RADIX);
    }
    
    @Override
    public AdminCommandInstance createCommandInstance() {
        return new AdminCommandInstanceImpl(getNewId());
    }

    @Override
    public synchronized void register(AdminCommandInstance instance) throws IllegalArgumentException {
        if (instance == null) {
            throw new IllegalArgumentException("Argument instance can not be null");
        }
        if (map.containsKey(instance.getId())) {
            throw new IllegalArgumentException("Instance id is already in use.");
        }     
        if (ids.size() >= MAX_SIZE) {
            String rid = ids.remove(0);
            map.remove(rid);
        }
        map.put(instance.getId(), instance);
        ids.add(instance.getId());
        if (instance instanceof AdminCommandInstanceImpl) {
            ((AdminCommandInstanceImpl) instance).setState(AdminCommandState.State.RUNNING);
        }
    }

    @Override
    public Iterator<AdminCommandInstance> itarator() {
        return map.values().iterator();
    }

    @Override
    public AdminCommandInstance get(String id) {
        return map.get(id);
    }
    
}
