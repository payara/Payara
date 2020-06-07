/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2016-2019] Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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

package com.sun.enterprise.container.common.impl.util;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IAtomicLong;
import com.hazelcast.core.ILock;
import com.hazelcast.core.IMap;
import com.sun.enterprise.container.common.spi.ClusteredSingletonLookup;
import fish.payara.nucleus.hazelcast.HazelcastCore;

import java.util.concurrent.atomic.AtomicReference;

import org.glassfish.internal.api.Globals;

/**
 * Base class for implementing Clustered Singleton Lookups
 *
 * @author lprimak
 */
public abstract class ClusteredSingletonLookupImplBase implements ClusteredSingletonLookup {

    private final HazelcastCore hzCore = Globals.getDefaultHabitat().getService(HazelcastCore.class);
    private final String componentId;
    private final SingletonType singletonType;
    private final String keyPrefix;
    private final String mapKey;
    private final AtomicReference<String> sessionHzKey = new AtomicReference<>();
    private final AtomicReference<String> lockKey = new AtomicReference<>();

    public ClusteredSingletonLookupImplBase(String componentId, SingletonType singletonType) {
        this.componentId = componentId;
        this.singletonType = singletonType;
        this.keyPrefix = makeKeyPrefix();
        this.mapKey = makeMapKey();
    }

    protected final String getKeyPrefix() {
        return keyPrefix;
    }

    protected final String getMapKey() {
        return mapKey;
    }

    protected final String getLockKey() {
        return lockKey.updateAndGet(v -> v != null ? v : makeLockKey());
    }

    public final String getSessionHzKey() {
        return sessionHzKey.updateAndGet(v -> v != null ? v : makeSessionHzKey());
    }

    /**
     * {@link #getSessionHzKey()} and {@link #getLockKey()} are dependent on {@link #getClusteredSessionKey()} so should
     * its value change cache keys need to be invalidated using this method.
     */
    protected final void invalidateKeys() {
        sessionHzKey.set(null);
        lockKey.set(null);
    }

    @Override
    public ILock getDistributedLock() {
        return getHazelcastInstance().getLock(getLockKey());
    }

    @Override
    public IMap<String, Object> getClusteredSingletonMap() {
        return getHazelcastInstance().getMap(getMapKey());
    }

    @Override
    public  IAtomicLong getClusteredUsageCount() {
        return getHazelcastInstance().getAtomicLong(getSessionHzKey()+ "/count");
    }

    private HazelcastInstance getHazelcastInstance() {
        if (!hzCore.isEnabled()) {
            throw new IllegalStateException("ClusteredSingleton.getHazelcastInstance() - Hazelcast is Disabled");
        }
        return hzCore.getInstance();
    }

    @Override
    public boolean isClusteredEnabled() {
        return hzCore.isEnabled();
    }

    @Override
    public boolean isDistributedLockEnabled() {
        return isClusteredEnabled();
    }

    @Override
    public HazelcastCore getHazelcastCore() {
        return hzCore;
    }

    private String makeKeyPrefix() {
        return String.format("Payara/%s/singleton/", singletonType.name().toLowerCase());
    }

    private String makeMapKey() {
        return getKeyPrefix() + componentId;
    }

    private String makeLockKey() {
        return getSessionHzKey() + "/lock";
    }

    private String makeSessionHzKey() {
        return getKeyPrefix() + componentId + "/" + getClusteredSessionKey();
    }
}
