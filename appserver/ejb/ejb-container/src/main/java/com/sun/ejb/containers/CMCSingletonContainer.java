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
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
// Portions Copyright [2016-2023] [Payara Foundation and/or its affiliates]

package com.sun.ejb.containers;

import com.hazelcast.cp.exception.CPSubsystemException;
import com.hazelcast.cp.lock.exception.LockOwnershipLostException;
import com.sun.ejb.ComponentContext;
import com.sun.ejb.EjbInvocation;
import com.sun.ejb.InvocationInfo;
import com.sun.ejb.MethodLockInfo;
import com.sun.enterprise.security.SecurityManager;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import jakarta.ejb.ConcurrentAccessException;
import jakarta.ejb.ConcurrentAccessTimeoutException;
import jakarta.ejb.IllegalLoopbackException;
import jakarta.ejb.LockType;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;

/**
 * @author Mahesh Kannan
 */
public class CMCSingletonContainer
        extends AbstractSingletonContainer {

    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock(true);

    private final ReentrantReadWriteLock.ReadLock readLock = rwLock.readLock();

    private final ReentrantReadWriteLock.WriteLock writeLock = rwLock.writeLock();

    private final static long NO_BLOCKING = 0;
    private final static long BLOCK_INDEFINITELY = -1;

    private final MethodLockInfo defaultMethodLockInfo;

    public CMCSingletonContainer(EjbDescriptor desc, ClassLoader cl, SecurityManager sm)
            throws Exception {
        super(desc, cl, sm);

        // In absence of any method lock info default is WRITE lock with no timeout.
        defaultMethodLockInfo = new MethodLockInfo();
        defaultMethodLockInfo.setLockType(LockType.WRITE, clusteredLookup.isDistributedLockEnabled());
    }

    @Override
    protected ComponentContext _getContext(EjbInvocation invocation) {
        super._getContext(invocation);

        MethodLockInfo lockInfo = (invocation.invocationInfo.methodLockInfo == null)
                ? defaultMethodLockInfo : invocation.invocationInfo.methodLockInfo;
        // If the lock is distributed we will have locked it during lookupClusteredSingleton, called from super._getContext
        if (!lockInfo.isDistributed()) {
            lock(invocation);
        }

        //Now that we have the lock return the singletonCtx
        return singletonCtx;
    }

    /**
     * Get the clustered singleton for this container.
     *
     * This overrides the method in {@link AbstractSingletonContainer} to guard concurrent access via {@link Lock Locks}
     *
     * @param invocation The {@link EjbInvocation} that prompted the lookup
     * @return The clustered singleton object
     */
    @Override
    protected Object getClusteredSingleton(EjbInvocation invocation) {
        lock(invocation);
        return super.getClusteredSingleton(invocation);
    }

    /*
     * Findbugs complains that the lock acquired in this method is not
     *  unlocked on all paths in this method.
     *
     * Even though the method doesn't unlock the (possibly) acquired
     * lock, the lock is guaranteed to be unlocked in releaseContext()
     * even in the presence of (both checked and unchecked) exceptions.
     *
     * The general pattern used by various parts of the EJB container code is:
     *
     * try {
     *      container.preInvoke(inv);
     *      returnValue = container.intercept(inv);
     * } catch (Exception1 e1) {
     *      ...
     * } catch (Exception2 e2) {
     *      ...
     * } finally {
     *      container.postInvoke();
     * }
     *
     * Thus, it is clear that, BaseContainer.postInvoke() which in turn
     * calls releaseContext() will be called if container.preInvoke()
     * is called. This ensures that CMCSingletonContainer (this class)
     * releases the lock acquired by _getContext().
     *
     * Also, note that the above works even for loopback methods as
     * container.preInvoke() and container,postInvoke() will be called
     * before every bean method.
     *
     */
    private void lock(EjbInvocation invocation) {
        InvocationInfo invInfo = invocation.invocationInfo;

        MethodLockInfo lockInfo = (invInfo.methodLockInfo == null)
                ? defaultMethodLockInfo : invInfo.methodLockInfo;
        Lock theLock;
        if(lockInfo.isDistributed()) {
            theLock = clusteredSingletonLock;
        } else {
            theLock = lockInfo.isReadLockedMethod() ? readLock : writeLock;
        }

        if ( (rwLock.getReadHoldCount() > 0) &&
                (!rwLock.isWriteLockedByCurrentThread()) ) {
            if( lockInfo.isWriteLockedMethod() ) {
                throw new IllegalLoopbackException("Illegal Reentrant Access : Attempt to make " +
                        "a loopback call on a Write Lock method '" + invInfo.targetMethod1 +
                        "' while a Read lock is already held");
            }
        }

        /*
         * Please see comment at the beginning of the method.
         * Even though the method doesn't unlock the (possibly) acquired
         * lock, the lock is guaranteed to be unlocked in releaseContext()
         * even if exceptions were thrown in _getContext()
         */
        if (!lockInfo.hasTimeout() ||
                ( (lockInfo.hasTimeout() && (lockInfo.getTimeout() == BLOCK_INDEFINITELY) )) ) {
            theLock.lock();
        } else {
            try {
                boolean lockStatus = theLock.tryLock(lockInfo.getTimeout(), lockInfo.getTimeUnit());
                if (! lockStatus) {
                    String msg = "Couldn't acquire a lock within " + lockInfo.getTimeout() +
                            " " + lockInfo.getTimeUnit();
                    if( lockInfo.getTimeout() == NO_BLOCKING ) {
                        throw new ConcurrentAccessException(msg);
                    } else {
                        throw new ConcurrentAccessTimeoutException(msg);
                    }
                }
            } catch (InterruptedException inEx) {
                String msg = "Couldn't acquire a lock within " + lockInfo.getTimeout() +
                        " " + lockInfo.getTimeUnit();
                ConcurrentAccessException cae = (lockInfo.getTimeout() == NO_BLOCKING) ?
                        new ConcurrentAccessException(msg) : new ConcurrentAccessTimeoutException(msg);
                cae.initCause(inEx);
                throw cae;
            }
        }

        //Now that we have acquired the lock, remember it
        invocation.setCMCLock(theLock);
    }

    /**
     * This method must unlock any lock that might have been acquired
     * in _getContext().
     *
     * @see com.sun.ejb.containers.CMCSingletonContainer#_getContext(EjbInvocation inv)
     * @param inv The current EjbInvocation that was passed to _getContext()
     */
    @Override
    public void releaseContext(EjbInvocation inv) {
        super.releaseContext(inv);

        Lock theLock = inv.getCMCLock();
        if (theLock != null) {
            try {
                theLock.unlock();
            } catch (CPSubsystemException | LockOwnershipLostException e) {
                _logger.log(Level.WARNING, "Distributed unlock failed", e);
            }
        }
    }

}
