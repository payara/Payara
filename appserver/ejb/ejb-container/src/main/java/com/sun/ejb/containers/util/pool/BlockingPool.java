/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2016 Payara Foundation and/or its affiliates. All rights reserved.
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

package com.sun.ejb.containers.util.pool;

import static com.sun.ejb.containers.util.pool.AbstractPool._logger;
import java.util.logging.Level;

/**
 *
 * @author lprimak
 */
public class BlockingPool extends NonBlockingPool {
    public BlockingPool(long containerId, String name, ObjectFactory sessionCtxFactory, int steadyPoolSize,
            int poolResizeQuantity, int maxPoolSize, int poolIdleTimeoutInSeconds,
            ClassLoader loader, boolean singletonBeanPool, int maxWaitTimeInMillis) {
        super(containerId, name, sessionCtxFactory, steadyPoolSize, poolResizeQuantity, maxPoolSize,
                poolIdleTimeoutInSeconds, loader, singletonBeanPool);
        this.maxWaitTimeInMillis = maxWaitTimeInMillis;
    }


    @Override
    public Object getObject(Object param)
        throws PoolException
    {
        long t1, totalWaitTime = 0;
        synchronized (list) {
            while (singletonBeanPool == false) {
                if (list.size() > 0) {
                    return super.getObject(param);
                } else if ((createdCount - destroyedCount) < maxPoolSize) {
                    return super.getObject(param);
                }

                if (maxWaitTimeInMillis >= 0) {
                    waitCount++;
                    t1 = System.currentTimeMillis();
                    try {
                        _logger.log(Level.FINE, "[AbstractPool]: Waiting on" +
                                    " the pool to get a bean instance...");
                        list.wait(maxWaitTimeInMillis);
                    } catch (InterruptedException inEx) {
                        throw new PoolException("Thread interrupted.", inEx);
                    }
                    waitCount--;
                    totalWaitTime += System.currentTimeMillis() - t1;
                    if (list.size() > 0) {
                        return super.getObject(param);
                    } else if (maxWaitTimeInMillis == 0) {
                        // nothing special to do in this case
                    } else if (totalWaitTime >= maxWaitTimeInMillis) {
                        throw new PoolException("Pool Instance not obtained" +
                           " within given time interval.");
                    }
                } else {
                    throw new IllegalStateException("maxWaitTimeInMillis is negative with BlockingPool");
                }
            }
        }
        throw new IllegalStateException("Should never go here");
    }

    /**
     * Return an object back to the pool. An object that is obtained through
     *	getObject() must always be returned back to the pool using either
     *	returnObject(obj) or through destroyObject(obj).
     */
    @Override
    public void returnObject(Object object) {
        super.returnObject(object);
    	synchronized (list) {
            poolReturned++;
            if (waitCount > 0) {
                list.notify();
            }
    	}
    }

    /**
     * Destroys an Object. Note that applications should not ignore the
     * reference to the object that they got from getObject(). An object
     * that is obtained through getObject() must always be returned back to
     * the pool using either returnObject(obj) or through destroyObject(obj).
     * This method tells that the object should be destroyed and cannot
     * be reused.
     */
    @Override
    public void destroyObject(Object object) {
        super.destroyObject(object);
    	synchronized (list) {
            if (waitCount > 0) {
                list.notify();
            }
    	}
    }

    @Override
    protected void remove(int count) {
        super.remove(count);
        synchronized (list) {
            list.notifyAll();
        }
    }
}
