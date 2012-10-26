/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

/**
 * <BR> <I>$Source: /cvs/glassfish/appserv-core/src/java/com/sun/ejb/containers/util/pool/AbstractPool.java,v $</I>
 * @author     $Author: cf126330 $
 * @version    $Revision: 1.5 $ $Date: 2007/03/30 19:10:26 $
 */
 

package com.sun.ejb.containers.util.pool;

import com.sun.ejb.containers.EjbContainerUtilImpl;
import com.sun.ejb.monitoring.probes.EjbPoolProbeProvider;
import com.sun.ejb.monitoring.stats.EjbMonitoringUtils;
import com.sun.enterprise.util.Utility;
import org.glassfish.flashlight.provider.ProbeProviderFactory;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <p>Abstract pool provides the basic implementation of an object pool. 
 * The implementation uses a linked list to maintain a list of (available) 
 * objects. If the pool is empty it simply creates one using the ObjectFactory
 * instance. Subclasses can change this behaviour by overriding getObject(...)
 * and returnObject(....) methods. This class provides basic support for 
 * synchronization, event notification, pool shutdown and pool object 
 * recycling. It also does some very basic bookkeeping like the
 * number of objects created, number of threads waiting for object.
 * <p> Subclasses can make use of these book-keeping data to provide complex 
 * pooling mechanism like LRU / MRU / Random. Also, note that AbstractPool 
 * does not have a notion of  pool limit. It is upto to the derived classes 
 * to implement these features.
 */
public abstract class AbstractPool
    implements Pool
{

    protected static final Logger _logger = EjbContainerUtilImpl.getLogger();

    protected ArrayList	     list;
    protected ObjectFactory  factory = null;
    protected int	     waitCount = 0;
    protected int	     createdCount = 0;

    protected int	     steadyPoolSize;
    protected int	     resizeQuantity = 1;
    protected int	     maxPoolSize = Integer.MAX_VALUE;
    protected long	     maxWaitTimeInMillis;
    protected int	     idleTimeoutInSeconds;


    private AbstractPoolTimerTask  poolTimerTask;

    // class loader used as context class loader for asynchronous operations
    protected ClassLoader	   containerClassLoader;

    protected int		   destroyedCount = 0;
    protected int 		   poolSuccess = 0;
    protected String		   poolName;
    protected int                  poolReturned = 0; 

    protected String		   configData;
    protected EjbPoolProbeProvider poolProbeNotifier;

    protected String appName;
    protected String modName;
    protected String ejbName;

    protected long beanId;

    protected AbstractPool() {
    }

    protected AbstractPool(ObjectFactory factory, long beanId, int steadyPoolSize,
       int resizeQuantity, int maxPoolsize, long maxWaitTimeInMillis, 
                           int idleTimeoutInSeconds, ClassLoader loader) {
    	initializePool(factory, beanId, steadyPoolSize, resizeQuantity, maxPoolsize,
                       maxWaitTimeInMillis, idleTimeoutInSeconds, loader);
    }

    protected void initializePool(ObjectFactory factory, long beanId, int steadyPoolSize,
        int resizeQuantity, int maxPoolsize, long maxWaitTimeInMillis, 
        int idleTimeoutInSeconds, ClassLoader loader) {

        list = new ArrayList();
        
        this.factory = factory;
        this.steadyPoolSize = steadyPoolSize;
        this.resizeQuantity = resizeQuantity;
        this.maxPoolSize = maxPoolsize;
        this.maxWaitTimeInMillis = maxWaitTimeInMillis;
        this.idleTimeoutInSeconds = idleTimeoutInSeconds;

        this.beanId = beanId;

        if (steadyPoolSize > 0) {
            for (int i=0; i<steadyPoolSize; i++) {
                list.add(factory.create(null));
                poolProbeNotifier.ejbObjectAddedEvent(beanId, appName, modName, ejbName);
                createdCount++;
            }
        }
        
        this.containerClassLoader = loader;
        
        if (this.idleTimeoutInSeconds > 0) {
            try {
                this.poolTimerTask =  new AbstractPoolTimerTask();
                EjbContainerUtilImpl.getInstance().getTimer().scheduleAtFixedRate
                    (poolTimerTask, idleTimeoutInSeconds*1000L, 
                     idleTimeoutInSeconds*1000L);
            } catch (Throwable th) {
                _logger.log(Level.WARNING,
                    "[AbstractPool]: Could not add AbstractPoolTimerTask" + 
                    " ... Continuing anyway...");
            }
        }

    }
    

    public void setContainerClassLoader(ClassLoader loader) {
        this.containerClassLoader = loader;
    }

    public void setInfo(String appName, String modName, String ejbName) {
        this.appName = appName;
        this.modName = modName;
        this.ejbName = ejbName;
        try {
            ProbeProviderFactory probeFactory = EjbContainerUtilImpl.getInstance().getProbeProviderFactory();
            String invokerId = EjbMonitoringUtils.getInvokerId(appName, modName, ejbName);
            poolProbeNotifier = probeFactory.getProbeProvider(EjbPoolProbeProvider.class, invokerId);
            if (_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE, "Got poolProbeNotifier: " + poolProbeNotifier.getClass().getName());
            }
        } catch (Exception ex) {
            poolProbeNotifier = new EjbPoolProbeProvider();
            if (_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE, "Error getting the EjbPoolProbeProvider");
            }
        }

    }
    
    /**
     * Get an object. Application can use pool.getObject() to get an object
     *	instead of using new XXX().
     * @param canWait Must be true if the calling thread is willing to 
     * wait for infinite time to get an object, false if the calling thread 
     * does not want to wait at all.
     *	
     */
    public Object getObject(boolean canWait, Object param)
        throws PoolException
    {
        return getObject(param);
    }

    public Object getObject(long maxWaitTime, Object param)
        throws PoolException
    {
        return getObject(param);
    }

    public Object getObject(Object param)
        throws PoolException
    {
        long t1=0, totalWaitTime = 0;
        int size;

        synchronized (list) {
            while (true) {
                if ((size = list.size()) > 0) {
                    poolSuccess++;
                    return list.remove(size-1);
                } else if ((createdCount - destroyedCount) < maxPoolSize) {
                    poolProbeNotifier.ejbObjectAddedEvent(beanId, appName, modName, ejbName);
                    createdCount++;	//hope that everything will be OK.
                    break;
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
                    if ((size = list.size()) > 0) {
                        poolSuccess++;
                        return list.remove(size-1);
                    } else if (maxWaitTimeInMillis == 0) {
                        // nothing special to do in this case
                    } else if (totalWaitTime >= maxWaitTimeInMillis) {
                        throw new PoolException("Pool Instance not obtained" +
                           " within given time interval.");
                    }
                } else {
                    throw new PoolException("Pool Instance not obtained" +
                                            " within given time interval.");
                }
            }
        }
			
        try {
            return factory.create(param);
        } catch (Exception poolEx) {
            synchronized (list) {
                poolProbeNotifier.ejbObjectAddFailedEvent(beanId, appName, modName, ejbName);
                createdCount--;
            }
            throw new RuntimeException("Caught Exception when trying " +
                                       "to create pool Object ", poolEx);
        }
    }
    
    /**
     * Return an object back to the pool. An object that is obtained through
     *	getObject() must always be returned back to the pool using either 
     *	returnObject(obj) or through destroyObject(obj).
     */
    public void returnObject(Object object) {
    	synchronized (list) {
            list.add(object);
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
    public void destroyObject(Object object) {
    	synchronized (list) {
            poolProbeNotifier.ejbObjectDestroyedEvent(beanId, appName, modName, ejbName);
            destroyedCount++;
            if (waitCount > 0) {
                list.notify();
            }
    	}
        try {
            factory.destroy(object);
        } catch (Exception ex) {
            _logger.log(Level.FINE, "Exception in destroyObject()", ex);
        }
    }
    
    /**
    * Preload the pool with objects.
    * @param count the number of objects to be added.
    */
    protected void preload(int count) {
    	
    	synchronized (list) {
            for (int i=0; i<count; i++) {
                try {
                    list.add(factory.create(null));
                    poolProbeNotifier.ejbObjectAddedEvent(beanId, appName, modName, ejbName);
                    createdCount++;
                } catch (PoolException poolEx) {
                    _logger.log(Level.FINE, "Exception in preload()", poolEx);
                }
            }
    	}
    }
    
    /**
    * Close the pool
    */
    public void close() {
        synchronized (list) {
            if (poolTimerTask != null) {
                try {
                    poolTimerTask.cancel();	
                    _logger.log(Level.WARNING,
                                "[AbstractPool]: Cancelled pool timer task "
                                + " at: " + (new java.util.Date()));
                } catch (Throwable th) {
                    //Can safely ignore this!!
                }
            }
            _logger.log(Level.FINE,"[AbstractPool]: Destroying "
                        + list.size() + " beans from the pool...");

            // since we're calling into ejb code, we need to set context
            // class loader
            ClassLoader origLoader = 
                Utility.setContextClassLoader(containerClassLoader);

            Object[] array = list.toArray();
            for (int i=0; i<array.length; i++) {
                try {
                    poolProbeNotifier.ejbObjectDestroyedEvent(beanId, appName, modName, ejbName);
                    destroyedCount++;
                    try {
                        factory.destroy(array[i]);
                    } catch (Throwable th) {
                        _logger.log(Level.FINE, "Exception in destroy()", th);
                    }
                } catch (Throwable th) {
                    _logger.log(Level.WARNING,
                        "[AbstractPool]: Error while destroying: " + th);
                }
            }
            _logger.log(Level.FINE,"[AbstractPool]: Pool closed....");
            unregisterProbeProvider();
            Utility.setContextClassLoader(origLoader);
        }
        

        // helps garbage collection
        this.list                  = null;
        this.factory               = null;
        this.poolTimerTask         = null;
        this.containerClassLoader  = null;
        
    }

    protected void remove(int count) {
        ArrayList removeList = new ArrayList();
        synchronized (list) {
            int size = list.size();
            for (int i=0; (i<count) && (size > 0); i++) {
                removeList.add(list.remove(--size));
                poolProbeNotifier.ejbObjectDestroyedEvent(beanId, appName, modName, ejbName);
                destroyedCount++;
            }
            
            list.notifyAll();
        }
        
        for (int i=removeList.size()-1; i >= 0; i--) {
            factory.destroy(removeList.remove(i));
            try {
                factory.destroy(removeList.remove(i));
            } catch (Throwable th) {
                _logger.log(Level.FINE, "Exception in destroy()", th);
            }
        }
    }

    protected abstract void removeIdleObjects();
    
    private class AbstractPoolTimerTask
        extends java.util.TimerTask
    {
        AbstractPoolTimerTask() {}
        
        public void run() {
            //We need to set the context class loader for this (deamon)thread!!
            final Thread currentThread = Thread.currentThread();
            final ClassLoader previousClassLoader = 
                currentThread.getContextClassLoader();
            final ClassLoader ctxClassLoader = containerClassLoader;
            
            try {
                if(System.getSecurityManager() == null) {
                    currentThread.setContextClassLoader(ctxClassLoader);
                } else {
                    java.security.AccessController.doPrivileged(
                            new java.security.PrivilegedAction() {
                        public java.lang.Object run() {
                            currentThread.setContextClassLoader(ctxClassLoader);
                            return null;
                        }
                    });
                }

                try {
                    if (list.size() > steadyPoolSize) {
                        _logger.log(Level.FINE,"[AbstractPool]: Removing idle "
                            + " objects from pool. Current Size: "
                            + list.size() + "/" + steadyPoolSize
                            + ". Time: " + (new java.util.Date()));
                        removeIdleObjects();
                    }
                } catch (Throwable th) {
                    //removeIdleObjects would have logged the error
                }
                
                if(System.getSecurityManager() == null) {
                    currentThread.setContextClassLoader(previousClassLoader);
                } else {
                    java.security.AccessController.doPrivileged(
                            new java.security.PrivilegedAction() {
                        public java.lang.Object run() {
                            currentThread.setContextClassLoader(previousClassLoader);
                            return null;
                        }
                    });
                }
            } catch (Throwable th) {
                _logger.log(Level.FINE, "Exception in run()", th);
            }
        }
    }

    /**************** For Monitoring ***********************/
    /*******************************************************/
    
    public int getCreatedCount() {
        return createdCount;
    }
    
    public int getDestroyedCount() {
        return destroyedCount;
    }
    
    public int getPoolSuccess() {
        return poolSuccess;
    }
    
    public int getSize() {
        return list.size();
    }
    
    public int getWaitCount() {
        return waitCount;
    }
    
    public int getSteadyPoolSize() {
        return steadyPoolSize;
    }
    
    public int getResizeQuantity() {
        return resizeQuantity;
    }
    
    public int getMaxPoolSize() {
        return maxPoolSize;
    }
    
    public long getMaxWaitTimeInMillis() {
        return maxWaitTimeInMillis;
    }
    
    public int getIdleTimeoutInSeconds() {
        return idleTimeoutInSeconds;
    }

    public void setConfigData(String configData) {
	this.configData = configData;
    }


    //Methods on EJBPoolStatsProvider
    public void appendStats(StringBuffer sbuf) {
	sbuf.append("[Pool: ")
	    .append("SZ=").append(list.size()).append("; ")
	    .append("CC=").append(createdCount).append("; ")
	    .append("DC=").append(destroyedCount).append("; ")
	    .append("WC=").append(waitCount).append("; ")
	    .append("MSG=0");
	if (configData != null) {
	    sbuf.append(configData);
	}
	sbuf.append("]");
    }

    public int getJmsMaxMessagesLoad() {
	return 0;
    }

    public int getNumBeansInPool() {
	return list.size();
    }

    public int getNumThreadsWaiting() {
	return waitCount;
    }

    public int getTotalBeansCreated() {
	return createdCount;
    }

    public int getTotalBeansDestroyed() {
	return destroyedCount;
    }

    public String getAllMonitoredAttrbuteValues() {
        StringBuffer sbuf = new StringBuffer();
        synchronized (list) {
            sbuf.append("createdCount=").append(createdCount).append(";")
                .append("destroyedCount=").append(destroyedCount).append(";")
                .append("waitCount=").append(waitCount).append(";")
                .append("size=").append(list.size()).append(";");
        }
        sbuf.append("maxPoolSize=").append(maxPoolSize).append(";");
        return sbuf.toString();
    }
    
    public String getAllAttrValues() {
        StringBuffer sbuf = new StringBuffer();
        if(null != poolName)
            sbuf.append(":").append(poolName);
        else
            sbuf.append(":POOL");

        sbuf.append("[FP=").append(poolSuccess).append(",")
            .append("TC=").append(createdCount).append(",")
            .append("TD=").append(destroyedCount).append(",")
			.append("PR=").append(poolReturned).append(",")    
            .append("TW=").append(waitCount).append(",")
            .append("CS=").append(list.size()).append(",")
            .append("MS=").append(maxPoolSize);
    
        return sbuf.toString();
    }
    
    protected void unregisterProbeProvider () {
            try {
                ProbeProviderFactory probeFactory = EjbContainerUtilImpl.getInstance().getProbeProviderFactory();
                probeFactory.unregisterProbeProvider(poolProbeNotifier);
            } catch (Exception ex) {
                if (_logger.isLoggable(Level.FINE)) {
                    _logger.log(Level.FINE, "Error getting the EjbPoolProbeProvider");
                }
            }
    }
}
