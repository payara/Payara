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

package org.glassfish.api.admin;


import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ArrayBlockingQueue;

import org.jvnet.hk2.annotations.Service;

import javax.inject.Singleton;

import javax.inject.Inject;

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
    private static ReentrantReadWriteLock rwlock = new ReentrantReadWriteLock();

    /**
     * A thread which can hold a Read/Write lock across command invocations.
     * Once the lock is released the thread will exit.
     */
    private SuspendCommandsLockThread suspendCommandsLockThread = null;

    private String lockOwner = null;
    private String lockMessage = null;
    private Date   lockTimeOfAcquisition = null;

    /**
     * The status of a suspend command attempt.
     */
    public enum SuspendStatus { SUCCESS,       // Suspend succeeded
                                TIMEOUT,       // Failed - suspend timed out
                                ILLEGALSTATE,  // Failed - already suspended
                                ERROR          // Failed - other error
                              };

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

        /*
         * If the suspendCommandsLockThread is alive then we were
         * suspended manually (via suspendCommands()) otherwise we
         * may have been locked by a command requiring the EXCLUSIVE
         * lock.
         * If we were suspended via suspendCommands() we don't block
         * waiting for the lock (but we try to acquire the lock just to be
         * safe) - otherwise we set the timeout and try to get the lock.
         */
        if (suspendCommandsLockThread != null &&
            suspendCommandsLockThread.isAlive()) {
            timeout = -1;
        } else {
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
        }
        
        boolean lockAcquired = false;
        while (!lockAcquired) {
            try {
                if (lock.tryLock(timeout, TimeUnit.SECONDS)) {
                    lockAcquired = true;
                } else {
                    /*
                     * A timeout < 0 indicates the domain was likely already
                     * locked manually but we tried to acquire the lock 
                     * anyway - just in case.
                     */
                    if (timeout >= 0)
                        throw new AdminCommandLockTimeoutException(
                            "timeout acquiring lock",
                            getLockTimeOfAcquisition(),
                            getLockOwner());
                    else
                        throw new AdminCommandLockException(
                            getLockMessage(),
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
     * @param user the admin user who acquired the lock.
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
     * Sets a message to be returned if the lock could not be acquired.
     * This message can be displayed to the user to indicate why the
     * domain is locked.
     *
     * @param message The message to return.
     */
    private void setLockMessage(String message) {
        lockMessage = message;
    }

    /**
     * Get the message to be returned if the lock could not be acquired.
     *
     * @return  the message indicating why the domain is locked.
     */
    public synchronized String getLockMessage() {
        return lockMessage;
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

    /**
     * Indicates if commands are currently suspended.
     */
    public synchronized boolean isSuspended() {
        /*
         * If the suspendCommandsLockThread is alive then we are
         * already suspended or really close to it.
         */
        if (suspendCommandsLockThread != null &&
            suspendCommandsLockThread.isAlive()) {
            return true;
        }

        return false;
    }

    /**
     * Lock the DAS from accepting any commands annotated with a SHARED
     * or EXCLUSIVE CommandLock.  This method will result in the acquisition
     * of an EXCLUSIVE lock.  This method will not return until the lock
     * is acquired, it times out or an error occurs. 
     * 
     * @param   timeout         lock timeout in seconds
     * @param   lockOwner       the user who acquired the lock
     * @return                  status regarding acquisition of the lock
     */
    public synchronized SuspendStatus suspendCommands(
                  long timeout,
                  String lockOwner) {
        return suspendCommands(timeout, lockOwner, "");
    }

    /**
     * Lock the DAS from accepting any commands annotated with a SHARED
     * or EXCLUSIVE CommandLock.  This method will result in the acquisition
     * of an EXCLUSIVE lock.  This method will not return until the lock
     * is acquired, it times out or an error occurs. 
     * 
     * @param   timeout         lock timeout in seconds
     * @param   lockOwner       the user who acquired the lock
     * @param   message         message to return when a command is blocked
     * @return                  status regarding acquisition of the lock
     */
    public synchronized SuspendStatus suspendCommands(
                  long timeout,
                  String lockOwner,
                  String message) {

        BlockingQueue<AdminCommandLock.SuspendStatus> suspendStatusQ =
                    new ArrayBlockingQueue<AdminCommandLock.SuspendStatus>(1);

        /*
         * If the suspendCommandsLockThread is alive then we are
         * already suspended or really close to it.
         */
        if (suspendCommandsLockThread != null &&
            suspendCommandsLockThread.isAlive()) {
            return SuspendStatus.ILLEGALSTATE;
        }

        /*
         * Start a thread to manage the RWLock.
         */
        suspendCommandsLockThread =
            new SuspendCommandsLockThread(timeout, suspendStatusQ, lockOwner,
                                          message);
        try {
            suspendCommandsLockThread.setName(
                "DAS Suspended Command Lock Thread");
            suspendCommandsLockThread.setDaemon(true);
        } catch (IllegalThreadStateException e) {
            return SuspendStatus.ERROR;
        } catch (SecurityException e) {
            return SuspendStatus.ERROR;
        }
        suspendCommandsLockThread.start();

        /*
         * Block until the commandLockThread has acquired the
         * EXCLUSIVE lock or times out trying.
         * We don't want the suspend command to return until we
         * know the domain is suspended.
         * The commandLockThread puts the timeout status on the suspendStatusQ
         * once it has acquired the lock or timed out trying.
         */
        SuspendStatus suspendStatus = queueTake(suspendStatusQ);

        return suspendStatus;
    }

    /**
     * Release the lock allowing the DAS to accept commands.  This method
     * may return before the lock is released.  When the thread exits the
     * lock will have been released.  
     *
     * @return  the thread maintaining the lock, null if the DAS is not
     * in a suspended state.  The caller may join() the thread to determine
     * when the lock is released.
     */
    public synchronized Thread resumeCommands() {

        /*
         * We can't resume if commands are not already locked. 
         */
        if (suspendCommandsLockThread == null ||
            suspendCommandsLockThread.isAlive() == false ||
            suspendCommandsLockThread.resumeCommandsSemaphore == null) {
 
            return null;
        }
 
        /*
         * This allows the suspendCommandsLockThread to continue.  This
         * will release the RWLock and allow commands to be processed.
         */
        suspendCommandsLockThread.resumeCommandsSemaphore.release();

        return suspendCommandsLockThread;
    }

    /**
     * Convenience method that puts an object on a BlockingQueue
     * as well as deals with InterruptedExceptions.
     *
     * @param status Object to be put on the queue
     * @param itmQ   The BlockingQueue
     */
    private void queuePut(BlockingQueue<SuspendStatus> itmQ,
                          SuspendStatus status) { 

        boolean itemPut = false;

        while (!itemPut) {
            try {
                itmQ.put(status);
                itemPut = true;
            } catch (java.lang.InterruptedException e) {
                logger.log(Level.FINE, 
                           "Interrupted putting lock status on queue", e);
            }
        } 
    }

    /**
     * Convenience method that takes an object from a BlockingQueue
     * as well as deals with InterruptedExceptions.
     *
     * @param itmQ The BlockingQueue
     */
    private SuspendStatus queueTake(BlockingQueue<SuspendStatus> itmQ) {
        SuspendStatus status = SuspendStatus.SUCCESS;
        boolean       itemTake = false;
 
        while (!itemTake) {
            try {
                status = itmQ.take();
                itemTake = true;
            } catch (java.lang.InterruptedException e) {
                logger.log(Level.FINE,
                           "Interrupted getting status from a suspend queue", e);
            }
        }
        return status;
    }

    /**
     * The SuspendCommandsLockThread represents a thread which will
     * hold a Read/Write lock across command invocations.  Once the
     * lock is released the thread will exit.
     */
    private class SuspendCommandsLockThread extends Thread {
 
        private Semaphore resumeCommandsSemaphore;
        private BlockingQueue<SuspendStatus> suspendStatusQ;
        private boolean suspendCommandsTimedOut;
        private long timeout;
        private String lockOwner;
        private String message;
 
        public SuspendCommandsLockThread(long timeout,
                                   BlockingQueue<SuspendStatus> suspendStatusQ,
                                   String lockOwner,
                                   String message) {

            this.suspendStatusQ = suspendStatusQ;
            this.timeout = timeout;
            this.lockOwner = lockOwner;
            this.message = message;
            resumeCommandsSemaphore = null;
            suspendCommandsTimedOut = false;
        }

        public void run() {

            /*
             * The EXCLUSIVE lock/unlock must occur in the same thread.
             * The lock may block if someone else currently has the
             * EXCLUSIVE lock. 
             * This deals with both the timeout as well as the 
             * potential for an InterruptedException.
             */
            Lock lock = getLock(CommandLock.LockType.EXCLUSIVE);
            boolean lockAcquired = false;
            while (!lockAcquired && !suspendCommandsTimedOut) {
                try {
                    if (lock.tryLock(timeout, TimeUnit.SECONDS))
                        lockAcquired = true;
                    else
                        suspendCommandsTimedOut = true;
                } catch (java.lang.InterruptedException e) {
                    logger.log(Level.FINE, 
                               "Interrupted acquiring command lock. ",
                               e); 
               }
            }

            if (lockAcquired) {
                setLockOwner(lockOwner);
                setLockMessage(message);
                setLockTimeOfAcquisition(new Date());

                /*
                 * A semaphore that is triggered to signal to the thread 
                 * to release the lock.  This should only be created after
                 * the lock has been acquired.
                 */
                resumeCommandsSemaphore = new Semaphore(0, true);
            }

            /*
             * The suspendStatusQ is used to signal that we acquired 
             * the lock.   A blocking queue is used to indicate whether we 
             * timed out or ran into an error acquiring the lock.
             */
            if (suspendStatusQ != null) { 
                if (suspendCommandsTimedOut == true) { 
                    queuePut(suspendStatusQ, SuspendStatus.TIMEOUT);
                } else {
                    queuePut(suspendStatusQ, SuspendStatus.SUCCESS);
                }
            }

            /*
             * If we timed out trying to get the lock then this thread 
             * is finished.
             */
            if (suspendCommandsTimedOut)
                return;
               
            /*
             * We block here waiting to be told to resume.
             */
            semaphoreWait(resumeCommandsSemaphore, 
                          "Interrupted waiting on resume semaphore");

            /*
             * Resume the domain by unlocking the EXCLUSIVE lock.
             */
            lock.unlock();
        }

        /**
         * Convenience method that waits on a semaphore to be
         * released as well as deals with InterruptedExceptions.
         *
         * @param s semaphore to wait on
         * @param logMsg a message to log if InterruptedException caught
         */
        private void semaphoreWait(Semaphore s, String logMsg) {

            boolean semaphoreReleased = false;

            while (!semaphoreReleased) {
                try {
                    s.acquire();
                    semaphoreReleased = true;
                } catch (java.lang.InterruptedException e) {
                    logger.log(Level.FINE, logMsg, e);
                }
            } 
        }
    }

        /**
         * Use this method to temporarily suspend the command lock during
         * which other operations may be performed.  When the method returns
         * the lock will be reestablished.
         * This method must be invoked from the same thread which acquired 
         * the original lock.
         * 
         * @param r A Runnable which will be invoked by the method after the lock is suspended
         */
        public static void runWithSuspendedLock(Runnable r) {
           
            Lock lock = null;

            try {
                // We need to determine the type of lock this thread holds.
                if (rwlock.isWriteLockedByCurrentThread()) {
                    lock = rwlock.writeLock();
                } else if (rwlock.getReadHoldCount() > 0) {
                    lock = rwlock.readLock();
                }

                if (lock != null)
                    lock.unlock();

                // Run the caller's commands without a lock in place.
                r.run();
            } finally {
                // Relock before returning.  This may block if someone else
                // already grabbed the lock.
                if (lock != null)
                    lock.lock();
            }
        }
}
