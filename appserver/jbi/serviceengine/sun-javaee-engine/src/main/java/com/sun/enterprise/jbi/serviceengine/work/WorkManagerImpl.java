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

package com.sun.enterprise.jbi.serviceengine.work;

import java.util.logging.Logger;
import java.util.logging.Level;
import com.sun.corba.se.spi.orbutil.threadpool.ThreadPool;
import com.sun.corba.se.spi.orbutil.threadpool.ThreadPoolManager;
import com.sun.corba.se.spi.orbutil.threadpool.NoSuchThreadPoolException;
import com.sun.enterprise.jbi.serviceengine.comm.MessageAcceptor;
import com.sun.enterprise.jbi.serviceengine.config.ComponentConfiguration;
import com.sun.enterprise.jbi.serviceengine.ServiceEngineException;
import com.sun.logging.LogDomains;
import com.sun.corba.se.impl.orbutil.threadpool.ThreadPoolManagerImpl;
import java.lang.reflect.Constructor;

/**
 * Work Manager implementation for JBI Service Engine.
 *
 * @author Binod PG
 */ 
public class WorkManagerImpl implements WorkManager, Runnable {

    private ComponentConfiguration config = null;
    private String threadPoolName = null;
    private ThreadPoolManager tpm = null; 
    private ThreadPool tp = null;
    private MessageAcceptor acceptor= null;

    private static Logger logger =
        LogDomains.getLogger(WorkManagerImpl.class, LogDomains.SERVER_LOGGER);

    /**
     * Saves the static service engine instance for use by 
     * work manager. Also obtains configured thread pool name.
     */
    public WorkManagerImpl(ComponentConfiguration config) {
        this.config = config;
        this.threadPoolName = this.config.getCommonThreadPoolName();
    }

    /**
     * Set the name of thread pool. 
     */
    public void setPoolName(String poolName) {
        this.threadPoolName = poolName;
    }

    /**
     * Retrieves the name of the thread pool used by this
     * work manager.
     */
    public String getPoolName() {
        return this.threadPoolName;
    }

    /**
     * Retrieves the MessageAcceptor of the work manager. 
     */
    public MessageAcceptor getMessageAcceptor() {
        return this.acceptor;
    }

    /**
     * Submit a work to the queue of the thread pool.
     * We just submit to a random queue of the thread pool.
     *
     * @param work Work object as specified by the Appserver
     * thread pool interface.
     */
    public void submitWork(OneWork work) {
        tp.getAnyWorkQueue().addWork(work);
    }

    /**
     * Start the message acceptor thread.
     */
    public void startAcceptor() throws ServiceEngineException {
        //TODO :- until appserver threadpool is available, using corba se threadpool.
        tpm = getThreadPoolManager();

        if (getPoolName() == null) { 
            // This will be default orb thread pool
            tp = tpm.getDefaultThreadPool();
        } else {
            try {
                tp = tpm.getThreadPool(getPoolName());
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE,
                    "Got the thread pool for :" + getPoolName());
                }
            } catch (NoSuchThreadPoolException e) {
                logger.log(Level.SEVERE,
                "workmanager.threadpool_not_found", getPoolName());
                throw new ServiceEngineException(e.getMessage());
            }
        }
        this.acceptor = new MessageAcceptor();
        this.acceptor.startAccepting();
    }

    /**
     * Convenience method to release acceptor thread.
     */
    public void stop() {
        this.acceptor.release();
    }
    
    public void run() {
        try {
            startAcceptor();
        } catch(ServiceEngineException se) {
            se.printStackTrace();
        } finally {
            stop();
        }
    }

        /**
     * JDK 1.6.0_14 & JDK 1.6.0_16 has changes in SE thread pool api.
     * Later we will be using appserver's thread pool.
     * Using the workaround to check the constructor availability and act accordingly.
     * @return thread pool manager
     * @throws ServiceEngineException when unable to provide thread pool manager
     */
    private static ThreadPoolManager getThreadPoolManager() throws ServiceEngineException {
        Constructor defaultConstructor;
        Constructor threadGroupParamConstructor;
        try {
            defaultConstructor = ThreadPoolManagerImpl.class.getConstructor();
            defaultConstructor.setAccessible(true);

            return (ThreadPoolManager)defaultConstructor.newInstance();

        } catch(NoSuchMethodException e) {
            //do nothing. Second trial with a ThreadGroup parameter constructor will be done.
        } catch(Exception e){
            //do nothing.  Second trial with a ThreadGroup parameter constructor will be done.
        }

        try {
            threadGroupParamConstructor = ThreadPoolManagerImpl.class.getConstructor(ThreadGroup.class);
            threadGroupParamConstructor.setAccessible(true);

            ThreadGroup tg = null;
            return (ThreadPoolManager)threadGroupParamConstructor.newInstance(tg);

        } catch(Exception e){
            throw new ServiceEngineException("Service Engine unable to provide thread pool manager");
        }
    }
}
