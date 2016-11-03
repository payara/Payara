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
 * <BR> <I>$Source: /cvs/glassfish/appserv-core/src/java/com/sun/ejb/containers/util/pool/AbstractPool.java,v $</I>
 * @author     $Author: cf126330 $
 * @version    $Revision: 1.5 $ $Date: 2007/03/30 19:10:26 $
 */
 

package com.sun.ejb.containers.util.pool;

import com.sun.ejb.containers.EjbContainerUtilImpl;
import com.sun.ejb.monitoring.probes.EjbPoolProbeProvider;
import com.sun.ejb.monitoring.stats.EjbMonitoringUtils;
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

    protected final ArrayList<Object> list = new ArrayList<>();
    protected ObjectFactory  factory = null;
    protected int	     waitCount = 0;
    protected int	     createdCount = 0;

    protected int	     steadyPoolSize;
    protected int	     resizeQuantity = 1;
    protected int	     maxPoolSize = Integer.MAX_VALUE;
    protected long	     maxWaitTimeInMillis;
    protected int	     idleTimeoutInSeconds;


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
                _logger.log(Level.FINE, "Got poolProbeNotifier: {0}", poolProbeNotifier.getClass().getName());
            }
        } catch (InstantiationException | IllegalAccessException ex) {
            poolProbeNotifier = new EjbPoolProbeProvider();
            if (_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE, "Error getting the EjbPoolProbeProvider");
            }
        }

    }

    @Override
    public Object getObject(boolean canWait, Object param)
        throws PoolException
    {
        return getObject(param);
    }

    @Override
    public Object getObject(long maxWaitTime, Object param)
        throws PoolException
    {
        return getObject(param);
    }


    
    protected abstract void removeIdleObjects();
    abstract public void close();
   
    /* *************** For Monitoring ***********************/
    /* ******************************************************/
    
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
        StringBuilder sbuf = new StringBuilder();
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
        StringBuilder sbuf = new StringBuilder();
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
