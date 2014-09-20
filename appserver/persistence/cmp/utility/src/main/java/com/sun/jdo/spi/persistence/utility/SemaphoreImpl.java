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

package com.sun.jdo.spi.persistence.utility;

import java.util.ResourceBundle;
import com.sun.jdo.spi.persistence.utility.logging.Logger;
import org.glassfish.persistence.common.I18NHelper;


/** Implements a simple semaphore.
 *
 * @author Dave Bristor
 * @author Marina Vatkina
 */
// db13166: I would rather we use Doug Lea's stuff, but don't want to
// introduce that magnitude of change at this point in time.
public class SemaphoreImpl implements Semaphore {
    /** Where to log messages about locking operations
     */
    private static final Logger _logger = LogHelperUtility.getLogger();

    /** For logging, indicates on whose behalf locking is done.
     */
    private final String _owner;

    /** Synchronizes the lock.
     */
    private final Object _lock = new Object();

    /** Thread which holds the lock.
     */
    private Thread _holder = null;

    /** Semaphore counter.
     */
    private int _counter = 0;
    
    /**
     * I18N message handler
     */
    private final static ResourceBundle messages = 
        I18NHelper.loadBundle(SemaphoreImpl.class);


    public SemaphoreImpl(String owner) {
        _owner = owner;
    }

    /** Acquire a lock.
     */
    public void acquire() {
        boolean debug = _logger.isLoggable(Logger.FINEST);

        if (debug) {
            Object[] items = new Object[] {_owner, Thread.currentThread(),new Integer(_counter)};
            _logger.finest("utility.semaphoreimpl.acquire",items); // NOI18N
        }

        synchronized (_lock) {
            //
            // If the current thread already holds this lock, we simply
            // update the count and return.
            //
            if (Thread.currentThread() == _holder) {
                _counter++;
                
            } else {
                while (_counter > 0) {
                    try {
                        // wait for the lock to be released
                        _lock.wait();
                    } catch (InterruptedException e) {
                    }
                }
                _holder = Thread.currentThread();
                _counter++;
                
                if (debug) {
                    Object[] items = new Object[] {_owner, Thread.currentThread(),new Integer(_counter)};
                    _logger.finest("utility.semaphoreimpl.gotlock",items); // NOI18N
                }
            }
        }
    }

    /** Release a lock.
     */
    public void release() {
        boolean debug = _logger.isLoggable(Logger.FINEST);
        
        if (debug) {
            Object[] items = new Object[] {_owner, Thread.currentThread(),new Integer(_counter)};
            _logger.finest("utility.semaphoreimpl.release",items); // NOI18N
        }
        
        synchronized (_lock) {
            //
            // If the current thread already holds this lock, we simply
            // update the count and return.
            //
            if (Thread.currentThread() == _holder) {
                if (--_counter == 0) {
                    _holder = null;
                    _lock.notify();
                }
            } else {
                throw new IllegalMonitorStateException(
                  I18NHelper.getMessage(messages,
                                        "utility.semaphoreimpl.wrongthread", // NOI18N
                                        new Object[] {_owner, Thread.currentThread()}));
            }
        }
    }
}
