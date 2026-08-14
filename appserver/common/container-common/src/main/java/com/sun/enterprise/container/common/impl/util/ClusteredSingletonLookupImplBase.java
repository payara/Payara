/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2016-2023] Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
import com.hazelcast.core.IFunction;
import com.hazelcast.cp.CPGroupId;
import com.hazelcast.cp.IAtomicLong;
import com.hazelcast.cp.exception.CPSubsystemException;
import com.hazelcast.cp.lock.FencedLock;
import com.hazelcast.map.IMap;
import com.sun.enterprise.container.common.spi.ClusteredSingletonLookup;
import fish.payara.nucleus.hazelcast.HazelcastCore;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.internal.api.Globals;
import org.glassfish.internal.api.JavaEEContextUtil;
import org.glassfish.internal.api.JavaEEContextUtil.Context;

/**
 * Base class for implementing Clustered Singleton Lookups
 *
 * @author lprimak
 */
public abstract class ClusteredSingletonLookupImplBase implements ClusteredSingletonLookup {
    private static final Logger logger = Logger.getLogger(ClusteredSingletonLookupImplBase.class.getName());

    private final HazelcastCore hzCore = Globals.getDefaultHabitat().getService(HazelcastCore.class);
    private final JavaEEContextUtil ctxUtil = Globals.getDefaultHabitat().getService(JavaEEContextUtil.class);
    private final String componentId;
    private final SingletonType singletonType;
    private final String keyPrefix;
    private final String mapKey;
    private final AtomicReference<String> sessionHzKey = new AtomicReference<>();
    private final AtomicReference<FencedLock> lock = new AtomicReference<>();
    private final AtomicReference<IAtomicLong> count = new AtomicReference<>();


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

    public final String getSessionHzKey() {
        return sessionHzKey.updateAndGet(v -> v != null ? v : makeSessionHzKey());
    }

    @Override
    public FencedLock getDistributedLock() {
        FencedLock existing = lock.get();
        if (existing != null) return existing;
        synchronized (this) {
            existing = lock.get();
            if (existing != null) return existing;
            FencedLock newLock;
            try {
                newLock = retryCpOperation(() ->
                        getHazelcastInstance().getCPSubsystem().getLock(makeLockKey()));
            } catch (UnsupportedOperationException e) {
                logger.log(Level.WARNING, "CP subsystem not available (requires Enterprise license); "
                        + "using IMap-based distributed lock for clustered EJB singleton", e);
                newLock = new IMapFencedLock(getHazelcastInstance().getMap(getMapKey()), makeLockKey());
            }
            lock.set(newLock);
            return newLock;
        }
    }

    @Override
    public IMap<String, Object> getClusteredSingletonMap() {
        try (Context ctx = ctxUtil.empty().pushContext()) {
            return getHazelcastInstance().getMap(getMapKey());
        }
    }

    @Override
    public IAtomicLong getClusteredUsageCount() {
        IAtomicLong existing = count.get();
        if (existing != null) return existing;
        synchronized (this) {
            existing = count.get();
            if (existing != null) return existing;
            IAtomicLong newCount;
            try {
                newCount = retryCpOperation(() ->
                        getHazelcastInstance().getCPSubsystem().getAtomicLong(makeCountKey()));
            } catch (UnsupportedOperationException e) {
                logger.log(Level.WARNING, "CP subsystem not available (requires Enterprise license); "
                        + "using IMap-based atomic counter for clustered EJB singleton", e);
                newCount = new IMapAtomicLong(getHazelcastInstance().getMap(getMapKey()), makeCountKey());
            }
            count.set(newCount);
            return newCount;
        }
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
    public void destroy() {
        getClusteredSingletonMap().delete(getClusteredSessionKey());

        // CP locks and AtomicLong's can't be destroyed, as per https://github.com/hazelcast/hazelcast/issues/17498
        // so we just release the references to them and reset to zero where we can
        lock.set(null);
        IAtomicLong oldCountValue = count.getAndSet(null);
        if (oldCountValue != null) {
            oldCountValue.set(0);
        }
    }

    @Override
    public HazelcastCore getHazelcastCore() {
        return hzCore;
    }

    private <TT> TT retryCpOperation(Supplier<TT> operation) {
        CPSubsystemException exception = null;
        for (int ii = 0; ii < 3; ++ii) {
            try {
                return operation.get();
            } catch (CPSubsystemException e) {
                exception = e;
            }
        }
        throw exception;
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

    private String makeCountKey() {
        return getSessionHzKey() + "/count";
    }

    private String makeSessionHzKey() {
        String sessionKey = getClusteredSessionKey();
        if (componentId.startsWith(sessionKey)) {
            // shorten session key if componentId is similar
            // workaround for https://github.com/hazelcast/hazelcast/issues/17901
            return getKeyPrefix() + sessionKey;
        } else {
            return getKeyPrefix() + componentId + "/" + sessionKey;
        }
    }

    /**
     * IMap-based distributed lock fallback for when CP subsystem is unavailable (Hazelcast CE).
     * Uses IMap entry locking, which provides AP-mode distributed mutual exclusion equivalent
     * to what UNSAFE-mode CP provided in Hazelcast 5.3.x.
     */
    private static final class IMapFencedLock implements FencedLock {
        private final IMap<String, Object> imap;
        private final String lockKey;
        // Per-instance, per-thread hold count for isLockedByCurrentThread() / getLockCount().
        // Instance-field ThreadLocal means each lock key gets its own counter per thread.
        private final ThreadLocal<Integer> holdCount = ThreadLocal.withInitial(() -> 0);

        IMapFencedLock(IMap<String, Object> imap, String lockKey) {
            this.imap = imap;
            this.lockKey = lockKey;
        }

        @Override
        public void lock() {
            imap.lock(lockKey);
            holdCount.set(holdCount.get() + 1);
        }

        @Override
        public void lockInterruptibly() throws InterruptedException {
            lock();
        }

        @Override
        public long lockAndGetFence() {
            throw new UnsupportedOperationException("Fencing not available with IMap-based distributed lock fallback");
        }

        @Override
        public boolean tryLock() {
            boolean acquired = imap.tryLock(lockKey);
            if (acquired) holdCount.set(holdCount.get() + 1);
            return acquired;
        }

        @Override
        public long tryLockAndGetFence() {
            throw new UnsupportedOperationException("Fencing not available with IMap-based distributed lock fallback");
        }

        @Override
        public boolean tryLock(long timeout, TimeUnit unit) {
            try {
                boolean acquired = imap.tryLock(lockKey, timeout, unit);
                if (acquired) holdCount.set(holdCount.get() + 1);
                return acquired;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        @Override
        public long tryLockAndGetFence(long timeout, TimeUnit unit) {
            throw new UnsupportedOperationException("Fencing not available with IMap-based distributed lock fallback");
        }

        @Override
        public void unlock() {
            imap.unlock(lockKey);
            holdCount.set(Math.max(0, holdCount.get() - 1));
        }

        @Override
        public long getFence() {
            throw new UnsupportedOperationException("Fencing not available with IMap-based distributed lock fallback");
        }

        @Override
        public boolean isLocked() {
            return imap.isLocked(lockKey);
        }

        @Override
        public boolean isLockedByCurrentThread() {
            return holdCount.get() > 0;
        }

        @Override
        public int getLockCount() {
            return holdCount.get();
        }

        @Override
        public CPGroupId getGroupId() {
            throw new UnsupportedOperationException("CP groups not available with IMap-based distributed lock fallback");
        }

        @Override
        public Condition newCondition() {
            throw new UnsupportedOperationException("Conditions not available with IMap-based distributed lock fallback");
        }

        @Override
        public String getPartitionKey() {
            return lockKey;
        }

        @Override
        public String getName() {
            return lockKey;
        }

        @Override
        public String getServiceName() {
            return "hz:impl:imap:lock";
        }

        @Override
        public void destroy() {
            imap.forceUnlock(lockKey);
        }
    }

    /**
     * IMap-based atomic long fallback for when CP subsystem is unavailable (Hazelcast CE).
     * Stores the count value as a Long in the singleton IMap and uses IMap entry locking
     * for atomicity.
     */
    private static final class IMapAtomicLong implements IAtomicLong {
        private final IMap<String, Object> imap;
        private final String countKey;

        IMapAtomicLong(IMap<String, Object> imap, String countKey) {
            this.imap = imap;
            this.countKey = countKey;
        }

        private long getUnlocked() {
            Object val = imap.get(countKey);
            return val instanceof Long ? (Long) val : 0L;
        }

        @Override
        public long get() {
            return getUnlocked();
        }

        @Override
        public void set(long newValue) {
            imap.lock(countKey);
            try {
                imap.put(countKey, newValue);
            } finally {
                imap.unlock(countKey);
            }
        }

        @Override
        public long addAndGet(long delta) {
            imap.lock(countKey);
            try {
                long newVal = getUnlocked() + delta;
                imap.put(countKey, newVal);
                return newVal;
            } finally {
                imap.unlock(countKey);
            }
        }

        @Override
        public long incrementAndGet() {
            return addAndGet(1L);
        }

        @Override
        public long decrementAndGet() {
            return addAndGet(-1L);
        }

        @Override
        public long getAndIncrement() {
            return getAndAdd(1L);
        }

        @Override
        public long getAndDecrement() {
            return getAndAdd(-1L);
        }

        @Override
        public long getAndAdd(long delta) {
            imap.lock(countKey);
            try {
                long current = getUnlocked();
                imap.put(countKey, current + delta);
                return current;
            } finally {
                imap.unlock(countKey);
            }
        }

        @Override
        public long getAndSet(long newValue) {
            imap.lock(countKey);
            try {
                long current = getUnlocked();
                imap.put(countKey, newValue);
                return current;
            } finally {
                imap.unlock(countKey);
            }
        }

        @Override
        public boolean compareAndSet(long expect, long update) {
            imap.lock(countKey);
            try {
                if (getUnlocked() != expect) return false;
                imap.put(countKey, update);
                return true;
            } finally {
                imap.unlock(countKey);
            }
        }

        @Override
        public void alter(IFunction<Long, Long> function) {
            imap.lock(countKey);
            try {
                imap.put(countKey, function.apply(getUnlocked()));
            } finally {
                imap.unlock(countKey);
            }
        }

        @Override
        public long alterAndGet(IFunction<Long, Long> function) {
            imap.lock(countKey);
            try {
                long newVal = function.apply(getUnlocked());
                imap.put(countKey, newVal);
                return newVal;
            } finally {
                imap.unlock(countKey);
            }
        }

        @Override
        public long getAndAlter(IFunction<Long, Long> function) {
            imap.lock(countKey);
            try {
                long current = getUnlocked();
                imap.put(countKey, function.apply(current));
                return current;
            } finally {
                imap.unlock(countKey);
            }
        }

        @Override
        public <R> R apply(IFunction<Long, R> function) {
            return function.apply(get());
        }

        @Override
        public CompletionStage<Long> addAndGetAsync(long delta) {
            return CompletableFuture.completedFuture(addAndGet(delta));
        }

        @Override
        public CompletionStage<Long> incrementAndGetAsync() {
            return CompletableFuture.completedFuture(incrementAndGet());
        }

        @Override
        public CompletionStage<Long> decrementAndGetAsync() {
            return CompletableFuture.completedFuture(decrementAndGet());
        }

        @Override
        public CompletionStage<Long> getAndIncrementAsync() {
            return CompletableFuture.completedFuture(getAndIncrement());
        }

        @Override
        public CompletionStage<Long> getAndDecrementAsync() {
            return CompletableFuture.completedFuture(getAndDecrement());
        }

        @Override
        public CompletionStage<Long> getAndAddAsync(long delta) {
            return CompletableFuture.completedFuture(getAndAdd(delta));
        }

        @Override
        public CompletionStage<Long> getAndSetAsync(long newValue) {
            return CompletableFuture.completedFuture(getAndSet(newValue));
        }

        @Override
        public CompletionStage<Boolean> compareAndSetAsync(long expect, long update) {
            return CompletableFuture.completedFuture(compareAndSet(expect, update));
        }

        @Override
        public CompletionStage<Long> getAsync() {
            return CompletableFuture.completedFuture(get());
        }

        @Override
        public CompletionStage<Void> setAsync(long newValue) {
            set(newValue);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletionStage<Void> alterAsync(IFunction<Long, Long> function) {
            alter(function);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletionStage<Long> alterAndGetAsync(IFunction<Long, Long> function) {
            return CompletableFuture.completedFuture(alterAndGet(function));
        }

        @Override
        public CompletionStage<Long> getAndAlterAsync(IFunction<Long, Long> function) {
            return CompletableFuture.completedFuture(getAndAlter(function));
        }

        @Override
        public <R> CompletionStage<R> applyAsync(IFunction<Long, R> function) {
            return CompletableFuture.completedFuture(apply(function));
        }

        @Override
        public String getPartitionKey() {
            return countKey;
        }

        @Override
        public String getName() {
            return countKey;
        }

        @Override
        public String getServiceName() {
            return "hz:impl:imap:atomicLong";
        }

        @Override
        public void destroy() {
            imap.lock(countKey);
            try {
                imap.delete(countKey);
            } finally {
                imap.unlock(countKey);
            }
        }
    }
}
