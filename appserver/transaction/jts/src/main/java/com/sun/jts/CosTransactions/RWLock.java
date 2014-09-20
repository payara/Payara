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
import java.lang.InterruptedException;
import java.util.LinkedList;
import java.util.logging.Logger;
import java.util.logging.Level;
import com.sun.logging.LogDomains;


/**
 * A <i>RWLock</i> provides concurrency control for multiple readers single writer
 * access patterns. This lock can provide access to multiple reader threads simultaneously
 * as long as there are no writer threads. Once a writer thread gains access to the
 * instance locked by a RWLock, all the reader threads wait till the writer completes
 * accessing the instance in question.
 * <p>
 * A RWLock is extremely useful in scenarios where there are lots more readers and
 * very few writers to a data structure. Also if the read operation by the reader
 * thread could take significant amount of time (binary search etc.)
 * <p>
 * The usage of Lock can be see as under:
 *  <p><hr><blockquote><pre>
 *    public class MyBTree {
 *      private RWLock lock = new Lock();
 *      .....
 *      .....
 *      public Object find(Object o) {
 *        try {
 *          lock.acquireReadLock();
 *          ....perform complex search to get the Object ...
 *          return result;
 *        } finally {
 *          lock.releaseReadLock();
 *        }
 *      }
 *
 *      public void insert(Object o) {
 *        try {
 *          lock.acquireWriteLock();
 *          ....perform complex operation to insert object ...
 *        } finally {
 *          lock.releaseWriteLock();
 *        }
 *      }
 *    }
 * </pre></blockquote><hr>
 * <p>
 * @author Dhiru Pandey 8/7/2000 
 */
 
 
public class RWLock {

  int currentReaders;
  int pendingReaders;
  int currentWriters;
  /*
 	 Logger to log transaction messages
  */ 
  static Logger _logger = LogDomains.getLogger(RWLock.class, LogDomains.TRANSACTION_LOGGER);
 
  Queue writerQueue = new Queue();
  /**
   * This method is used to acquire a read lock. If there is already a writer thread
   * accessing the object using the RWLock then the reader thread will wait until
   * the writer completes its operation
   */
  public synchronized void acquireReadLock() {
    if (currentWriters == 0 && writerQueue.size() == 0) {
      ++currentReaders;
    } else {
      ++pendingReaders;
      try {
        wait();
      } catch(InterruptedException ie) {
	  	_logger.log(Level.FINE,"Error in acquireReadLock",ie);
      }
    }
  }

  /**
   * This method is used to acquire a write lock. If there are already reader threads
   * accessing the object using the RWLock, then the writer thread will wait till all
   * the reader threads are finished with their operations.
   */
  public void acquireWriteLock() {
    Object lock = new Object();

    synchronized(lock) {
      synchronized(this) {
        if (writerQueue.size() == 0 && currentReaders == 0 && currentWriters == 0) {
          ++currentWriters;
          // Use logging facility if you need to log this
			//_logger.log(Level.FINE," RW: incremented WriterLock count");
          return;
        }
        writerQueue.enQueue(lock);
        // Use logging facility if you need to log this
			//_logger.log(Level.FINE," RW: Added WriterLock to queue");
      }
      try {
        lock.wait();
      } catch(InterruptedException ie) {
	  	_logger.log(Level.FINE,"Error in acquireWriteLock",ie);
      }
    }
  }

  /**
   * isWriteLocked
   * 
   * returns true if the RWLock is in a write locked state.
   *
   */
  public boolean isWriteLocked()
  {
	  return currentWriters > 0 ;
  }
 
  /**
   * This method is used to release a read lock. 
   * It also notifies any waiting writer thread
   * that it could now acquire a write lock.
   */
  public synchronized void releaseReadLock() {
    if (--currentReaders == 0) 
      notifyWriters();
  }
 
  /**
   * This method is used to release a write lock. It also notifies any pending
   * readers that they could now acquire the read lock. If there are no reader
   * threads then it will try to notify any waiting writer thread that it could now
   * acquire a write lock.
   */
  public synchronized void releaseWriteLock() {
    --currentWriters;
    if (pendingReaders > 0) 
      notifyReaders();
    else 
      notifyWriters();
  }
  private void notifyReaders() {
    currentReaders += pendingReaders;
    pendingReaders = 0;
    notifyAll();
  }
  
  private void notifyWriters() {
    if (writerQueue.size() > 0) {
      Object lock = writerQueue.deQueueFirst();
      ++currentWriters;
      synchronized(lock) { 
        lock.notify();
      }
    }
  }

  class Queue extends LinkedList {

    public Queue() {
      super();
    }

    public void enQueue(Object o) {
      super.addLast(o);
    }

    public Object deQueueFirst() {
      return super.removeFirst();
    }

  }

}
 


