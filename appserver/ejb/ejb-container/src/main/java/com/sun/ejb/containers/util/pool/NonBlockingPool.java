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
// Portions Copyright [2016] [Payara Foundation and/or its affiliates]

/**
 * <BR> <I>$Source: /cvs/glassfish/appserv-core/src/java/com/sun/ejb/containers/util/pool/NonBlockingPool.java,v $</I>
 * @author     $Author: cf126330 $
 * @version    $Revision: 1.4 $ $Date: 2007/03/30 19:10:26 $
 */
 

package com.sun.ejb.containers.util.pool;

import com.sun.ejb.containers.EJBContextImpl;
import com.sun.ejb.containers.EjbContainerUtilImpl;
import com.sun.enterprise.util.Utility;

import java.util.ArrayList;
import java.util.TimerTask;
import java.util.logging.Level;

/**
 * <p>NonBlockingPool pool provides the basic implementation of an object 
 * pool. The implementation uses a linked list to maintain a list of 
 * (available) objects. If the pool is empty it simply creates one using the
 * ObjectFactory instance. Subclasses can change this behaviour by overriding
 * getObject(...) and returnObject(....) methods. This class provides basic 
 * support for synchronization, event notification, pool shutdown
 * and pool object recycling. It also does some very basic bookkeeping like the
 * number of objects created, number of threads waiting for object.
 * <p> Subclasses can make use of these book-keeping data to provide complex 
 * pooling mechanism like LRU / MRU / Random. Also, note that AbstractPool 
 * does not have a notion of  pool limit. It is upto to the derived classes 
 * to implement these features.
 *	
 */


public class NonBlockingPool
    extends AbstractPool
{

    private TimerTask	  poolTimerTask;
    protected boolean	  addedResizeTask = false;
    volatile protected boolean	  addedIdleBeanWork = false;
    protected boolean	  inResizing = false;
    private boolean	  maintainSteadySize = false;

    /**
     * If glassfish-ejb-jar.xml <enterprise-beans><property>singleton-bean-pool is
     * true, steadyPoolSize is 1, and maxPoolSize is 1, then this field is set
     * to true, and only 1 bean instance is created.  The pool size at any given
     * time may be 0 or 1.  Both PoolResizeTimerTask and ReSizeWork are skipped.
     */
    protected boolean       singletonBeanPool;

    // Set to true after close().  Prevents race condition
    // of async resize task kicking in after close().
    private boolean poolClosed = false;
    
    private int		  resizeTaskCount;

    protected NonBlockingPool() {
    }

    public NonBlockingPool(long beanId, String poolName, ObjectFactory factory, 
        int steadyPoolSize, int resizeQuantity,
        int maxPoolSize, int idleTimeoutInSeconds, 
        ClassLoader loader)
    {
        this(beanId, poolName, factory,
             steadyPoolSize, resizeQuantity,
             maxPoolSize, idleTimeoutInSeconds,
             loader, false);
    }

    public NonBlockingPool(long beanId, String poolName, ObjectFactory factory,
        int steadyPoolSize, int resizeQuantity,
        int maxPoolSize, int idleTimeoutInSeconds,
        ClassLoader loader, boolean singletonBeanPool)
    {
        this.poolName = poolName;
        this.beanId = beanId;
        this.singletonBeanPool = singletonBeanPool && (steadyPoolSize == 1) &&
                (maxPoolSize == 1);
    	initializePool(factory, steadyPoolSize, resizeQuantity, maxPoolSize,
                       idleTimeoutInSeconds, loader);
    }

    private void initializePool(ObjectFactory factory, int steadyPoolSize,
        int resizeQuantity, int maxPoolSize, int idleTimeoutInSeconds,
        ClassLoader loader)
    {
        this.factory = factory;
        this.steadyPoolSize = (steadyPoolSize <= 0) ? 0 : steadyPoolSize;
        this.resizeQuantity = (resizeQuantity <= 0) ? 0 : resizeQuantity;
        this.maxPoolSize = (maxPoolSize <= 0)
            ? Integer.MAX_VALUE : maxPoolSize;
        this.steadyPoolSize = (this.steadyPoolSize > this.maxPoolSize)
            ? this.maxPoolSize : this.steadyPoolSize;
        this.idleTimeoutInSeconds = 
            (idleTimeoutInSeconds <= 0 || this.singletonBeanPool) ? 0 : idleTimeoutInSeconds;
        
        this.containerClassLoader = loader;
        
        this.maintainSteadySize = this.singletonBeanPool ? false : (this.steadyPoolSize > 0);
        if ((this.idleTimeoutInSeconds > 0) && (this.resizeQuantity > 0)) {
            try {
                this.poolTimerTask =  new PoolResizeTimerTask();
                EjbContainerUtilImpl.getInstance().getTimer().scheduleAtFixedRate
                    (poolTimerTask, idleTimeoutInSeconds*1000L, 
                     idleTimeoutInSeconds*1000L);
                if(_logger.isLoggable(Level.FINE)) {
                    _logger.log(Level.FINE, "[Pool-{0}]: Added PoolResizeTimerTask...", poolName);
                }
            } catch (Throwable th) {
                _logger.log(Level.WARNING,"[Pool-" + 
                            poolName + "]: Could not add"
                            + " PoolTimerTask. Continuing anyway...", th);
            }
        }
    }
    

    @Override
    public Object getObject(Object param)
    {
        boolean toAddResizeTask = false;
        Object obj = null;
        synchronized (list) {
            int size = list.size();
            if (size > steadyPoolSize) {
                poolSuccess++;
                return list.remove(size-1);
            } else if (size > 0) {
                poolSuccess++;
                if ((maintainSteadySize) && (addedResizeTask == false)) {
                    toAddResizeTask = addedResizeTask = true;
                    obj = list.remove(size-1);
                } else {
                    return list.remove(size-1);
                }
            } else if(!singletonBeanPool){
                if ((maintainSteadySize) && (addedResizeTask == false)) {
                    toAddResizeTask = addedResizeTask = true;
                }
                poolProbeNotifier.ejbObjectAddedEvent(beanId, appName, modName, ejbName);
                createdCount++;	//hope that everything will be OK.
            }
        }
        
        if (toAddResizeTask) {
            addResizeTaskForImmediateExecution();
        }
        
        if (obj != null) {
            return obj;
        }

        if (singletonBeanPool) {
            synchronized (list) {
                while (list.isEmpty() && (createdCount - destroyedCount) > 0) {
                    try {
                        list.wait();
                    } catch (InterruptedException ex) {  //ignore
                    }
                }
                if (!list.isEmpty()) {
                    obj = list.remove(0);
                    return obj;
                }
                try {
                    obj = factory.create(param);
                    createdCount++;
                    return obj;
                } catch (RuntimeException th) {
                    poolProbeNotifier.ejbObjectAddFailedEvent(beanId, appName, modName, ejbName);
                    throw th;
                }
            }
        } else {
            try {
                return factory.create(param);
            } catch (RuntimeException th) {
                synchronized (list) {
                    poolProbeNotifier.ejbObjectAddFailedEvent(beanId, appName, modName, ejbName);
                    createdCount--;
                }
                throw th;
            }
        }
    }
    
    private void addResizeTaskForImmediateExecution() {
        try {
            ReSizeWork work = new ReSizeWork();
            EjbContainerUtilImpl.getInstance().addWork(work);
            if(_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE, "[Pool-{0}]: Added PoolResizeTimerTask...", poolName);
            }
            resizeTaskCount++;
        } catch (Exception ex) {
            synchronized (list) {
                addedResizeTask = false;
            }
            if(_logger.isLoggable(Level.WARNING)) {
            	_logger.log(Level.WARNING, 
                            "[Pool-"+poolName+"]: Cannot perform "
                            + " pool resize task", ex);
            }
        }
    }

    /**
     * Return an object back to the pool. An object that is obtained through
     *	getObject() must always be returned back to the pool using either 
     *	returnObject(obj) or through destroyObject(obj).
     * @param object
     */
    @Override
    public void returnObject(Object object) {
    	synchronized (list) {
            if (list.size() < maxPoolSize) {
                list.add(object);
                if(this.singletonBeanPool) {
                    list.notify();
                }
                return;
            } else {
                poolProbeNotifier.ejbObjectDestroyedEvent(beanId, appName, modName, ejbName);
                destroyedCount++;
            }
        }
        
        try {
            factory.destroy(object);
        } catch (Exception ex) {
            _logger.log(Level.FINE, "exception in returnObj", ex);             
        }
    }

    /**
     * Destroys an Object. Note that applications should not ignore 
     * the reference to the object that they got from getObject(). An object 
     * that is obtained through getObject() must always be returned back to 
     * the pool using either returnObject(obj) or through destroyObject(obj). 
     * This method tells that the object should be destroyed and cannot 
     * be reused.
     * @param object
     */
    @Override
    public void destroyObject(Object object) {
        synchronized (list) {
            poolProbeNotifier.ejbObjectDestroyedEvent(beanId, appName, modName, ejbName);
            destroyedCount++;
            if (this.singletonBeanPool) {
                list.notify();
            }
        }

        try {
            factory.destroy(object);
        } catch (Exception ex) {
            _logger.log(Level.FINE, "exception in destroyObject", ex);
        }
    }
    
    /**
    * Preload the pool with objects.
    * @param count the number of objects to be added.
    */
    protected void preload(int count) {
    	
        ArrayList<Object> instances = new ArrayList<>(count);
        try {
            for (int i=0; i<count; i++) {
                instances.add(factory.create(null));
            }
    	} catch (Exception ex) {
            //Need not throw this exception up since we are pre-populating
    	}

        int sz = instances.size();
        if (sz == 0) {
            return;
        }
    	synchronized (list) {
            // check current pool size & adjust add size
            int currsize = list.size();
            int addsz = sz;
            if (currsize + sz > maxPoolSize) {
                addsz = maxPoolSize - currsize;
            }

            for (int i = 0; i < addsz; i++) {
                list.add(instances.remove(0));
            }
            createdCount += sz;
        }

        // destroys unnecessary instances
        for (Object o : instances) {
            destroyObject(o);
        }
    }

    /**
    * Prepopulate the pool with objects.
    * @param count the number of objects to be added.
    */
    public void prepopulate(int count) {
        this.steadyPoolSize = (count <= 0) ? 0 : count;
        this.steadyPoolSize = (this.steadyPoolSize > this.maxPoolSize)
            ? this.maxPoolSize : this.steadyPoolSize;
	
        if (this.steadyPoolSize > 0) {
            preload(this.steadyPoolSize);
        }
            
    }
 
    /**
    * Close the pool
    */
    @Override
    public void close() {
        synchronized (list) {
            if (poolTimerTask != null) {
                try {
                    poolTimerTask.cancel();	
                    if(_logger.isLoggable(Level.FINE)) {
                        _logger.log(Level.FINE,
                            "[Pool-{0}"
                                    + "]: Cancelled pool timer task " + " at: {1}", new Object[]{poolName, new java.util.Date()});
                    }
                } catch (Throwable th) {
                    //Can safely ignore this!!
                }
            }
	
            if(_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE, "[Pool-{0}]: Destroying {1} beans from the pool...", new Object[]{poolName, list.size()});
            }
	
            // since we're calling into ejb code, we need to set context
            // class loader
            ClassLoader origLoader = 
                Utility.setContextClassLoader(containerClassLoader);

            Object[] array = list.toArray();
            for (Object elt : array) {
                try {
                    poolProbeNotifier.ejbObjectDestroyedEvent(beanId, appName, modName, ejbName);
                    destroyedCount++;
                    try {
                        factory.destroy(elt);
                    }catch (Throwable th) {
                        _logger.log(Level.FINE, "exception in close", th);
                    }
                }catch (Throwable th) {
                    _logger.log(Level.WARNING,
                            "[Pool-"+poolName+"]: Error while destroying", th);
                }
            }
            if(_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE, "Pool-{0}]: Pool closed....", poolName);
            }
            list.clear(); 
            unregisterProbeProvider();

            Utility.setContextClassLoader(origLoader);

            poolClosed = true;

            this.list.clear();
            this.factory               = null;
            this.poolTimerTask         = null;
            this.containerClassLoader  = null;
        }
        
    }

    protected void remove(int count) {
        ArrayList<Object> removeList = new ArrayList<>();
        synchronized (list) {
            int size = list.size();
            for (int i=0; (i<count) && (size > 0); i++) {
                removeList.add(list.remove(--size));
                poolProbeNotifier.ejbObjectDestroyedEvent(beanId, appName, modName, ejbName);
                destroyedCount++;
            }
        }
        
        int sz = removeList.size();
        for (int i=0; i<sz; i++) {
            try {
                factory.destroy(removeList.get(i));
            } catch (Throwable th) {
                _logger.log(Level.FINE, "exception in remove", th);
            }
        }
    }

    @Override
    protected void removeIdleObjects() {
    }
    
    protected void doResize() {

        if( poolClosed ) {
            return;
        }
        
        //We need to set the context class loader for this (deamon) thread!!
        final Thread currentThread = Thread.currentThread();
        final ClassLoader previousClassLoader = 
            currentThread.getContextClassLoader();
        final ClassLoader ctxClassLoader = containerClassLoader;
	
        long startTime = 0;
        boolean enteredResizeBlock = false;
        try {
            if(System.getSecurityManager() == null) {
                currentThread.setContextClassLoader(ctxClassLoader);
            } else {
                java.security.AccessController.doPrivileged(
                        new java.security.PrivilegedAction<Object>() {
                    @Override
                    public java.lang.Object run() {
                        currentThread.setContextClassLoader(ctxClassLoader);
                        return null;
                    }
                });
            }

            if(_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE, "[Pool-{0}]: Resize started at: {1} steadyPoolSize ::{2} resizeQuantity ::{3} maxPoolSize ::{4}",
                        new Object[]{poolName, new java.util.Date(), steadyPoolSize, resizeQuantity, maxPoolSize});
            }
            startTime = System.currentTimeMillis();

            ArrayList<Object> removeList = new ArrayList<>();
            long populateCount = 0;
            synchronized (list) {
                if ((inResizing == true) || poolClosed) {
                    return;
                }

                enteredResizeBlock = true;
                inResizing = true;
                
                int curSize = list.size();

                if (curSize > steadyPoolSize) {

                    //possible to reduce pool size....
                    if ((idleTimeoutInSeconds <= 0)  || 
                        (resizeQuantity <= 0)) {
                        return;
                    }
                    int victimCount = 
                        (curSize > (steadyPoolSize + resizeQuantity) )
                        ? resizeQuantity : (curSize - steadyPoolSize);
                    long allowedIdleTime = System.currentTimeMillis() -
                        idleTimeoutInSeconds*1000L;
                    if(_logger.isLoggable(Level.FINE)) {
                        _logger.log(Level.FINE, 
                                    "[Pool-{0}]: Resize:: reducing " + " pool size by: {1}", new Object[]{poolName, victimCount});
                    }
                    for (int i=0; i<victimCount; i++) {
                        //removeList.add(list.remove(--curSize));
               		//destroyedCount++;
                        EJBContextImpl ctx = (EJBContextImpl) list.get(0);
                        if (ctx.getLastTimeUsed() <= allowedIdleTime) {
                            removeList.add(list.remove(0));
                            poolProbeNotifier.ejbObjectDestroyedEvent(beanId, appName, modName, ejbName);
                            destroyedCount++;
                        } else {
                            break;
                        }
                    }
                } else if (curSize < steadyPoolSize) {

                    //Need to populate....
                    if (maintainSteadySize  == false) {
                        return;
                    }

                    if (resizeQuantity <= 0) {
                        populateCount = steadyPoolSize - curSize; 
                    } else {
                        while ((curSize + populateCount) < steadyPoolSize) {
                            populateCount += resizeQuantity;
                        }
                        if ((curSize + populateCount) > maxPoolSize) {
                            populateCount -= (curSize + populateCount) - maxPoolSize;
                        }
                    }
                }
            }
            
            if (removeList.size() > 0) {
                int sz = removeList.size();
                for (int i=0; i<sz; i++) {
                    try {
                        factory.destroy(removeList.get(i));
                    } catch (Throwable th) {
                        _logger.log(Level.FINE, "exception in doResize", th);
                    }
                }
            }

            if (populateCount > 0) {
                //preload adds items inside a sync block....

                if(_logger.isLoggable(Level.FINE)) {
                    _logger.log(Level.FINE, "[Pool-{0}]: Attempting to preload {1} beans. CurSize/MaxPoolSize: {2}/{3}",
                            new Object[]{poolName, populateCount, list.size(), maxPoolSize});
                }

                preload((int)populateCount);

                if(_logger.isLoggable(Level.FINE)) {
                    _logger.log(Level.FINE, 
                            "[Pool-{0}" + "]: After preload " + "CurSize/MaxPoolSize: {1}/{2}",
                            new Object[]{poolName, list.size(), maxPoolSize});
                }
            }
            
            
        } catch (Throwable th) {
            _logger.log(Level.WARNING,
                        "[Pool-"+poolName+"]: Exception during reSize", th);

        } finally {
            
            if (enteredResizeBlock) {
                synchronized (list) {
                    inResizing = false;
                }
            }
            if(System.getSecurityManager() == null) {
                currentThread.setContextClassLoader(previousClassLoader);
            } else {
                java.security.AccessController.doPrivileged(
                        new java.security.PrivilegedAction<Object>() {
                    @Override
                    public java.lang.Object run() {
                        currentThread.setContextClassLoader(previousClassLoader);
                        return null;
                    }
                });
            }
        }

        long endTime = System.currentTimeMillis();
        if(_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE, "[Pool-{0}]: Resize completed at: {1}; after reSize: {2}", 
                    new Object[]{poolName, new java.util.Date(), getAllAttrValues()});
            _logger.log(Level.FINE, "[Pool-{0}]: Resize took: {1} seconds.", 
                    new Object[]{poolName, (endTime-startTime)/1000.0});
        }
    }

    @Override
    public String getAllAttrValues() {
        StringBuilder sbuf = new StringBuilder("[Pool-"+poolName+"] ");
        sbuf.append("CC=").append(createdCount).append("; ")
            .append("DC=").append(destroyedCount).append("; ")
            .append("CS=").append(list.size()).append("; ")
            .append("SS=").append(steadyPoolSize).append("; ")
            .append("MS=").append(maxPoolSize).append(";");
        return sbuf.toString();
    }

    private class ReSizeWork
        implements Runnable
    {
        public void prolog() {
        }
        
    	public void service() {
            run();
        }
        
    	public void epilog() {
        }

        @Override
        public void run() {
            try {
                doResize();
            } catch (Exception ex) {
                _logger.log(Level.WARNING,
                    "[Pool-"+poolName+"]: Exception during reSize", ex);
            } finally {
                synchronized (list) {
                    addedResizeTask = false;
                }
            }
        }
    }

    private class IdleBeanWork
        implements Runnable
    {
        public void prolog() {
        }
        
    	public void service() {
            run();
        }
        
    	public void epilog() {
        }
        
        @Override
        public void run() {
            try {
                doResize();
            } catch (Exception ex) {
            } finally {
                addedIdleBeanWork = false;
            }
        }
    }

    private class PoolResizeTimerTask
        extends java.util.TimerTask
    {
        PoolResizeTimerTask() {}
        
        @Override
        public void run() {
            
            try {
                if (addedIdleBeanWork == true) {
                    return;
                }
                addedIdleBeanWork = true;
                IdleBeanWork work = new IdleBeanWork();
                EjbContainerUtilImpl.getInstance().addWork(work);
            } catch (Exception ex) {
                addedIdleBeanWork = false;
                _logger.log(Level.WARNING, 
                            "[Pool-"+poolName+"]: Cannot perform "
                            + " pool idle bean cleanup", ex);
            }
	
        }
    } // End of class PoolResizeTimerTask

}
