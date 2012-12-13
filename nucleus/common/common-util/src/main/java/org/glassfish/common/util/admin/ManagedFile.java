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

package org.glassfish.common.util.admin;

import com.sun.enterprise.util.CULoggerInfo;
import com.sun.enterprise.util.LocalStringManagerImpl;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Defines the notion of a managed file with a classic Read-Write locking policy.
 * A managed file can be locked for multiple concurrent reads or a single write.
 * <p/>
 * A simple example could follow this :
 * <p/>
 * ManagedFile managedFile = new ManagedFile(new File(...), 1000, -1);
 * Lock writeLock;
 * try {
 * writeLock = managedFile.writeAccess();
 * // write or delete the file
 * } finally {
 * writeLock.unlock();
 * }
 *
 * @author Jerome Dochez
 */
public class ManagedFile {

    final File file;
    final int maxHoldingTime;
    final int timeOut;
    final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
    final ManagedFile.RefCounterLock rl = new ManagedFile.RefCounterLock(rwl.readLock(), true);
    final ManagedFile.RefCounterLock wl = new ManagedFile.RefCounterLock(rwl.writeLock(), false);
    final Queue<Thread> waiters = new ConcurrentLinkedQueue<Thread>();

    final static Logger logger = CULoggerInfo.getLogger();
    final static LocalStringManagerImpl localStrings =
            new LocalStringManagerImpl(ParamTokenizer.class);

    public interface ManagedLock extends java.util.concurrent.locks.Lock {
        public RandomAccessFile getLockedFile();    
    }
    /**
     * Creates a new managed file.
     *
     * @param file           the file to manage
     * @param timeOut        the max time in milliseconds to wait for a read or write lock
     * @param maxHoldingTime the max time in milliseconds to hold the read or write lock
     * @throws IOException when the file cannot be locked
     */
    public ManagedFile(File file, int timeOut, int maxHoldingTime) throws IOException {
        this.file = file;
        this.maxHoldingTime = maxHoldingTime;
        this.timeOut = timeOut;
    }


    /**
     * Blocks for {@link ManagedFile#timeOut} milliseconds for the write access
     * to the managed file.
     *
     * @return the lock instance on the locked file.
     * @throws IOException      if the file cannot be locked
     * @throws TimeoutException if the lock cannot be obtained before the timeOut
     *                          expiration.
     */
    public ManagedLock accessWrite() throws IOException, TimeoutException {
        wl._lock();
        return wl;
    }

    /**
     * Blocks for {@link ManagedFile#timeOut} milliseconds for the read access
     * to the managed file.
     *
     * @return the lock instance on the locked file.
     * @throws IOException      if the file cannot be locked
     * @throws TimeoutException if the lock cannot be obtained before the timeOut
     *                          expiration.
     */
    public ManagedLock accessRead() throws IOException, TimeoutException {
        rl._lock();
        return rl;
    }

    /**
     * Many threads can be requesting the shared read lock, we must keep track
     * through a reference counter when all these users are done. Each thread
     * must call {@link RefCounterLock#unlock()} to release the lock, when the
     * reference counter returns to zero, the file lock is release.
     */
    private class RefCounterLock implements ManagedLock {
        final java.util.concurrent.locks.Lock lock;
        final boolean read;
        final AtomicInteger refs = new AtomicInteger(0);
        FileLock fileLock;
        RandomAccessFile raf;
        FileChannel fc;
        Timer timer;

        public synchronized RandomAccessFile getLockedFile() {
            return raf;
        }

        private FileLock get(FileChannel fc, boolean shared) throws IOException, TimeoutException {

            FileLock fl;

            boolean wasInterrupted = false;
            Thread current = Thread.currentThread();
            waiters.add(current);

            // calculate how much time we want to be blocked for
            long endTime = System.currentTimeMillis() + timeOut;
            // so far, we wait in 1/10th increment of the requested timeOut.
            final int individualWaitTime = timeOut / 10;

            // Block while not first in queue or cannot acquire lock
            while (waiters.peek() != current ||
                    (fl = getLock(fc, shared)) == null) {
                // I cannot just park the thread and signal it since the
                // the lock maybe owned by a different process. I just need
                // to wait...

                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("Waiting..." + individualWaitTime);
                }
                if (System.currentTimeMillis() > endTime) {
                    throw new TimeoutException(localStrings.getLocalString("FileLockTimeOut",
                            "time out expired on locking {0}", file.getPath()));
                }
                try {
                    Thread.sleep(individualWaitTime);
                } catch (InterruptedException e) {
                    wasInterrupted = true;
                }
                if (Thread.interrupted()) // ignore interrupts while waiting
                    wasInterrupted = true;
            }

            waiters.remove();
            if (wasInterrupted)          // reassert interrupt status on exit
                current.interrupt();

            return fl;

        }

        private FileLock getLock(FileChannel fc, boolean shared) throws IOException {
            try {
                return fc.lock(0, Long.MAX_VALUE, shared);
            } catch (OverlappingFileLockException e) {
                return null;
            }
        }

        private synchronized FileLock access(boolean shared, String mode, int timeOut) throws IOException, TimeoutException {
            raf = new RandomAccessFile(file, mode);
            fc = raf.getChannel();
            final FileLock fl = get(fc, shared);
            if (maxHoldingTime != -1) {
                timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        try {
                            if (fl.isValid()) {
                                logger.log(Level.SEVERE, 
                                        CULoggerInfo.fileLockNotReleased, 
                                        file.getPath());
                                release(fl);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }, timeOut);
            }
            return fl;
        }

        private synchronized void release(FileLock lock) throws IOException {
            lock.release();
            if (timer != null) {
                timer.cancel();
                timer = null;
            }
            if (raf != null) {
                raf.close();
                raf = null;
            }
            if (fc != null) {
                fc.close();
                fc = null;
            }
        }

        private RefCounterLock(java.util.concurrent.locks.Lock lock, boolean read) {
            this.lock = lock;
            this.read = read;
        }

        public void _lock() throws IOException, TimeoutException {
            lock.lock();
            if (refs.incrementAndGet() == 1) {
                // create the file lock.
                if (read) {
                    fileLock = access(true, "r", maxHoldingTime);
                } else {
                    fileLock = access(false, "rw", maxHoldingTime);
                }
            }
        }

        @Override
        public void lock() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void lockInterruptibly() throws InterruptedException {
            lock.lockInterruptibly();
            refs.incrementAndGet();
        }

        @Override
        public boolean tryLock() {
            boolean result = lock.tryLock();
            if (result) refs.incrementAndGet();
            return result;
        }

        @Override
        public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
            boolean result = lock.tryLock(time, unit);
            if (result) refs.incrementAndGet();
            return result;
        }

        @Override
        public void unlock() {
            lock.unlock();
            if (refs.decrementAndGet() == 0) {
                try {
                    release(fileLock);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public Condition newCondition() {
            return lock.newCondition();
        }
    }

}

