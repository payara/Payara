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

package org.glassfish.enterprise.iiop.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.corba.ee.spi.threadpool.NoSuchThreadPoolException;
import com.sun.corba.ee.spi.threadpool.ThreadPool;
import com.sun.corba.ee.spi.threadpool.ThreadPoolChooser;
import com.sun.corba.ee.spi.threadpool.ThreadPoolFactory;
import com.sun.corba.ee.spi.threadpool.ThreadPoolManager;
import com.sun.logging.LogDomains;
import org.glassfish.internal.api.Globals;

public class S1ASThreadPoolManager implements ThreadPoolManager {

    static Logger _logger = LogDomains.getLogger(S1ASThreadPoolManager.class,
            LogDomains.UTIL_LOGGER);

    private static final int DEFAULT_NUMBER_OF_QUEUES = 0;
    private static final int DEFAULT_MIN_THREAD_COUNT = 10;
    private static final int DEFAULT_MAX_THREAD_COUNT = 200;

    private static HashMap idToIndexTable = new HashMap();
    private static HashMap indexToIdTable = new HashMap();
    private static ArrayList threadpoolList = new ArrayList();
    private static String defaultID;

    private static ThreadPoolManager s1asThreadPoolMgr = new S1ASThreadPoolManager();
    private static IIOPUtils _iiopUtils;

    public static ThreadPoolManager getThreadPoolManager() {
        return s1asThreadPoolMgr;
    }

    
    static {
        try {
            _iiopUtils = Globals.getDefaultHabitat().getService(IIOPUtils.class);
            Collection<org.glassfish.grizzly.config.dom.ThreadPool> tpCol = _iiopUtils.getAllThreadPools();
            org.glassfish.grizzly.config.dom.ThreadPool[] allThreadPools = tpCol.toArray(new org.glassfish.grizzly.config.dom.ThreadPool[tpCol.size()]);
            for (int i = 0; i < allThreadPools.length; i++) {
                createThreadPools(allThreadPools[i], i);
            }
            defaultID = (String) indexToIdTable.get(Integer.valueOf(0));
        } catch (NullPointerException npe) {
            _logger.log(Level.FINE, "Server Context is NULL. Ignoring and proceeding.");
        }


    }
    
    S1ASThreadPoolManager() {
    }


    private static void createThreadPools(org.glassfish.grizzly.config.dom.ThreadPool
            threadpoolBean, int index) {
        String threadpoolId = null;
        String minThreadsValue, maxThreadsValue, timeoutValue;//, numberOfQueuesValue;
        int minThreads = DEFAULT_MIN_THREAD_COUNT;
        int maxThreads = DEFAULT_MAX_THREAD_COUNT;
        int idleTimeoutInSeconds = 120000;
//        int numberOfQueues = DEFAULT_NUMBER_OF_QUEUES;

        try {
            threadpoolId = threadpoolBean.getName();
        } catch (NullPointerException npe) {
            if (_logger.isLoggable(Level.WARNING)) {
                _logger.log(Level.WARNING, "ThreadPoolBean may be null ", npe);
            }
        }
        try {
            minThreadsValue = threadpoolBean.getMinThreadPoolSize();
            minThreads = Integer.parseInt(minThreadsValue);
        } catch (NullPointerException npe) {
            if (_logger.isLoggable(Level.WARNING)) {
                _logger.log(Level.WARNING, "ThreadPoolBean may be null ", npe);
                _logger.log(Level.WARNING,
                        "Using default value for steady-threadpool-size = " + minThreads);
            }
        } catch (NumberFormatException nfe) {
            if (_logger.isLoggable(Level.WARNING)) {
                _logger.log(Level.WARNING, "enterprise_util.excep_orbmgr_numfmt", nfe);
                _logger.log(Level.WARNING,
                        "Using default value for min-threadpool-size = " + minThreads);
            }
        }
        try {
            maxThreadsValue = threadpoolBean.getMaxThreadPoolSize();
            maxThreads = Integer.parseInt(maxThreadsValue);
        } catch (NullPointerException npe) {
            if (_logger.isLoggable(Level.WARNING)) {
                _logger.log(Level.WARNING, "ThreadPoolBean may be null ", npe);
                _logger.log(Level.WARNING,
                        "Using default value for max-threadpool-size = " + maxThreads);
            }
        } catch (NumberFormatException nfe) {
            if (_logger.isLoggable(Level.WARNING)) {
                _logger.log(Level.WARNING, "enterprise_util.excep_orbmgr_numfmt", nfe);
                _logger.log(Level.WARNING,
                        "Using default value for max-threadpool-size = " + maxThreads);
            }
        }
        try {
            timeoutValue = threadpoolBean.getIdleThreadTimeoutSeconds();
            idleTimeoutInSeconds = Integer.parseInt(timeoutValue);
        } catch (NullPointerException npe) {
            if (_logger.isLoggable(Level.WARNING)) {
                _logger.log(Level.WARNING, "ThreadPoolBean may be null ", npe);
                _logger.log(Level.WARNING,
                        "Using default value for idle-thread-timeout-in-seconds = " +
                                idleTimeoutInSeconds);
            }
        } catch (NumberFormatException nfe) {
            if (_logger.isLoggable(Level.WARNING)) {
                _logger.log(Level.WARNING, "enterprise_util.excep_orbmgr_numfmt", nfe);
                _logger.log(Level.WARNING,
                        "Using default value for idle-thread-timeout-in-seconds = " +
                                idleTimeoutInSeconds);
            }
        }

        // Currently this value is not used but when multi-queue threadpools are
        // implemented this could be used to decide which one to instantiate and
        // number of queues in the multi-queue threadpool
/*
        try {
            numberOfQueuesValue = threadpoolBean.getNumWorkQueues();
            numberOfQueues = Integer.parseInt(numberOfQueuesValue);
        } catch (NullPointerException npe) {
            if (_logger.isLoggable(Level.WARNING)) {
                _logger.log(Level.WARNING, "ThreadPoolBean may be null ", npe);
                _logger.log(Level.WARNING,
                        "Using default value for num-work-queues = " +
                                numberOfQueues);
            }
        } catch (NumberFormatException nfe) {
            if (_logger.isLoggable(Level.WARNING)) {
                _logger.log(Level.WARNING, "enterprise_util.excep_orbmgr_numfmt", nfe);
                _logger.log(Level.WARNING,
                        "Using default value for num-work-queues = " +
                                numberOfQueues);
            }
        }
*/

        // Mutiplied the idleTimeoutInSeconds by 1000 to convert to milliseconds
        ThreadPoolFactory threadPoolFactory = new ThreadPoolFactory();
        ThreadPool threadpool =
                threadPoolFactory.create(minThreads, maxThreads,
                        idleTimeoutInSeconds * 1000L, threadpoolId,
                        _iiopUtils.getCommonClassLoader());

        // Add the threadpool instance to the threadpoolList
        threadpoolList.add(threadpool);

        // Associate the threadpoolId to the index passed
        idToIndexTable.put(threadpoolId, Integer.valueOf(index));

        // Associate the threadpoolId to the index passed
        indexToIdTable.put(Integer.valueOf(index), threadpoolId);
    }

    /**
     * This method will return an instance of the threadpool given a threadpoolId,
     * that can be used by any component in the app. server.
     *
     * @throws NoSuchThreadPoolException thrown when invalid threadpoolId is passed
     *                                   as a parameter
     */
    public ThreadPool
    getThreadPool(String id)
            throws NoSuchThreadPoolException {

        Integer i = (Integer) idToIndexTable.get(id);
        if (i == null) {
            throw new NoSuchThreadPoolException();
        }
        try {
            ThreadPool threadpool =
                    (ThreadPool)
                            threadpoolList.get(i.intValue());
            return threadpool;
        } catch (IndexOutOfBoundsException iobe) {
            throw new NoSuchThreadPoolException();
        }
    }

    /**
     * This method will return an instance of the threadpool given a numeric threadpoolId.
     * This method will be used by the ORB to support the functionality of
     * dedicated threadpool for EJB beans
     *
     * @throws NoSuchThreadPoolException thrown when invalidnumericIdForThreadpool is passed
     *                                   as a parameter
     */
    public ThreadPool getThreadPool(int numericIdForThreadpool)
            throws NoSuchThreadPoolException {

        try {
            ThreadPool threadpool =
                    (ThreadPool)
                            threadpoolList.get(numericIdForThreadpool);
            return threadpool;
        } catch (IndexOutOfBoundsException iobe) {
            throw new NoSuchThreadPoolException();
        }
    }

    /**
     * This method is used to return the numeric id of the threadpool, given a String
     * threadpoolId. This is used by the POA interceptors to add the numeric threadpool
     * Id, as a tagged component in the IOR. This is used to provide the functionality of
     * dedicated threadpool for EJB beans
     */
    public int getThreadPoolNumericId(String id) {
        Integer i = (Integer) idToIndexTable.get(id);
        return ((i == null) ? 0 : i.intValue());
    }

    /**
     * Return a String Id for a numericId of a threadpool managed by the threadpool
     * manager
     */
    public String getThreadPoolStringId(int numericIdForThreadpool) {
        String id = (String) indexToIdTable.get(Integer.valueOf(numericIdForThreadpool));
        return ((id == null) ? defaultID : id);
    }

    /**
     * Returns the first instance of ThreadPool in the ThreadPoolManager
     */
    public ThreadPool
    getDefaultThreadPool() {
        try {
            return getThreadPool(0);
        } catch (NoSuchThreadPoolException nstpe) {
            if (_logger.isLoggable(Level.WARNING)) {
                _logger.log(Level.WARNING, "No default ThreadPool defined ", nstpe);
            }
        }
        return null;
    }

    /**
     * Return an instance of ThreadPoolChooser based on the componentId that was
     * passed as argument
     */
    public ThreadPoolChooser getThreadPoolChooser(String componentId) {
        //FIXME: This method is not used, but should be fixed once
        //ORB's nio select starts working and we start using ThreadPoolChooser
        //This will be mostly used by the ORB
        return null;
    }

    /**
     * Return an instance of ThreadPoolChooser based on the componentIndex that was
     * passed as argument. This is added for improved performance so that the caller
     * does not have to pay the cost of computing hashcode for the componentId
     */
    public ThreadPoolChooser getThreadPoolChooser(int componentIndex) {
        //FIXME: This method is not used, but should be fixed once
        //ORB's nio select starts working and we start using ThreadPoolChooser
        //This will be mostly used by the ORB
        return null;
    }

    /**
     * Sets a ThreadPoolChooser for a particular componentId in the ThreadPoolManager. This
     * would enable any component to add a ThreadPoolChooser for their specific use
     */
    public void setThreadPoolChooser(String componentId, ThreadPoolChooser aThreadPoolChooser) {
        //FIXME: This method is not used, but should be fixed once
        //ORB's nio select starts working and we start using ThreadPoolChooser
        //This will be mostly used by the ORB
    }

    /**
     * Gets the numeric index associated with the componentId specified for a
     * ThreadPoolChooser. This method would help the component call the more
     * efficient implementation i.e. getThreadPoolChooser(int componentIndex)
     */
    public int getThreadPoolChooserNumericId(String componentId) {
        //FIXME: This method is not used, but should be fixed once
        //ORB's nio select starts working and we start using ThreadPoolChooser
        //This will be mostly used by the ORB
        return 0;
    }

    public void close() {
        //TODO
    }
} 


