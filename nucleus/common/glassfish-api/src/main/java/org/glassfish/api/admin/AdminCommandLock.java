/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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
 *
 * Portions Copyright [2017-2021] Payara Foundation and/or affiliates
 */

package org.glassfish.api.admin;


import org.jvnet.hk2.annotations.Service;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The implementation of the admin command lock.
 * 
 * @author Bill Shannon
 * @author Chris Kasso
 */
@Service
@Singleton
public class AdminCommandLock {

    @Inject
    Logger logger;

    /**
     * The read/write lock.  We depend on this class being a singleton
     * and thus there being exactly one such lock object, shared by all
     * users of this class.
     */
    private static final ReentrantReadWriteLock rwlock = new ReentrantReadWriteLock();

    private String lockOwner = null;
    private Date   lockTimeOfAcquisition = null;

    /**
     * Return the appropriate Lock object for the specified LockType.
     * The returned lock has not been locked.  If the LockType is
     * not SHARED or EXCLUSIVE null is returned.
     *
     * @param   type    the LockType
     * @return          the Lock object to use, or null
     */
    public Lock getLock(CommandLock.LockType type) {
        if (type == CommandLock.LockType.SHARED)
            return rwlock.readLock();
        if (type == CommandLock.LockType.EXCLUSIVE)
            return rwlock.writeLock();
        return null;    // no lock
    }

    public void dumpState(Logger logger, Level level) {
        if (logger.isLoggable(level)) {
            logger.log(level, "Current locking conditions are " + rwlock.getReadLockCount()
                         + "/"+ rwlock.getReadHoldCount() + " shared locks"
                         + "and " + rwlock.getWriteHoldCount() + " write lock");
        }
    }

    /**
     * Return the appropriate Lock object for the specified command.
     * The returned lock has not been locked.  If this command needs
     * no lock, null is returned.
     *
     * @param   command the AdminCommand object
     * @return          the Lock object to use, or null if no lock needed
     */
    public Lock getLock(AdminCommand command) {
        CommandLock alock = command.getClass().getAnnotation(CommandLock.class);
        if (alock == null || alock.value() == CommandLock.LockType.SHARED)
            return rwlock.readLock();
        if (alock.value() == CommandLock.LockType.EXCLUSIVE)
            return rwlock.writeLock();
        return null;    // no lock
    }

    /**
     * Return the appropriate Lock object for the specified command.
     * The returned lock has been locked.  If this command needs
     * no lock, null is returned.
     *
     * @param   command the AdminCommand object
     * @param   owner   the authority who requested the lock
     * @return          the Lock object to use, or null if no lock needed
     */
    public Lock getLock(AdminCommand command, String owner) throws
            AdminCommandLockTimeoutException,
            AdminCommandLockException {

        Lock lock = null;
        boolean exclusive = false;
        int timeout = 30; 

        CommandLock alock = command.getClass().getAnnotation(CommandLock.class);

        if (alock == null || alock.value() == CommandLock.LockType.SHARED)
            lock = rwlock.readLock();
        else if (alock.value() == CommandLock.LockType.EXCLUSIVE) {
            lock = rwlock.writeLock();
            exclusive = true;
        }

        if (lock == null) 
            return null; // no lock


        boolean badTimeOutValue = false;
        String timeout_s = System.getProperty(
            "com.sun.aas.commandLockTimeOut", "30");

        try {
            timeout = Integer.parseInt(timeout_s);
            if (timeout < 0)
                badTimeOutValue = true;
        } catch (NumberFormatException e) {
            badTimeOutValue = true;
        }
        if (badTimeOutValue) {
            //XXX: Deal with logger injection attack.
            logger.log(Level.INFO,
                       "Bad value com.sun.aas.commandLockTimeOut: "
                       + timeout_s + ". Using 30 seconds.");
            timeout = 30;
        }

        
        boolean lockAcquired = false;
        while (!lockAcquired) {
            try {
                if (lock.tryLock(timeout, TimeUnit.SECONDS)) {
                    lockAcquired = true;
                } else {
                    throw new AdminCommandLockTimeoutException(
                        "timeout acquiring lock",
                        getLockTimeOfAcquisition(),
                        getLockOwner());
                }
            } catch (java.lang.InterruptedException e) {
                logger.log(Level.FINE, "Interrupted acquiring command lock. ",
                           e);
            }
        }

        if (lockAcquired && exclusive) {
            setLockOwner(owner);
            setLockTimeOfAcquisition(new Date());
        }

        return lock;
    }

    /**
     * Sets the admin user id for the user who acquired the exclusive lock.
     *
     * @param owner the admin user who acquired the lock.
     */
    private void setLockOwner(String owner) {
        lockOwner = owner;
    }

    /**
     * Get the admin user id for the user who acquired the exclusive lock.
     * This does not imply the lock is still held.
     *
     * @return  the admin user who acquired the lock
     */
    public synchronized String getLockOwner() {
        return lockOwner;
    }

    /**
     * Sets the time the exclusive lock was acquired.
     *
     * @param time the time the lock was acquired
     */
    private void setLockTimeOfAcquisition(Date time) {
        lockTimeOfAcquisition = time;
    }

    /**
     * Get the time the exclusive lock was acquired.  This does not
     * imply the lock is still held.
     *
     * @return the time the lock was acquired
     */
    public synchronized Date getLockTimeOfAcquisition() {
        return lockTimeOfAcquisition;
    }

}
