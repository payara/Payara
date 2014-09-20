/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.config.dom;

import org.jvnet.hk2.config.Attribute;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.Configured;
import org.jvnet.hk2.config.Dom;
import org.jvnet.hk2.config.DuckTyped;
import org.jvnet.hk2.config.types.PropertyBag;

import java.beans.PropertyVetoException;
import java.util.ArrayList;
import java.util.List;

@Configured
public interface ThreadPool extends ConfigBeanProxy, PropertyBag {
    String DEFAULT_THREAD_POOL_CLASS_NAME =
            "org.glassfish.grizzly.threadpool.GrizzlyExecutorService";
    
    int IDLE_THREAD_TIMEOUT = 900;
    int MAX_QUEUE_SIZE = 4096;

    // min and max are set to the same value to force the use
    // of the fixed thread pool in default configuration cases.
    // This thread pool offers better performance characteristics
    // over the sync thread pool.
    int MAX_THREADPOOL_SIZE = 5;
    int MIN_THREADPOOL_SIZE = 5;

    /**
     * The classname of a thread pool implementation
     */
    @Attribute(defaultValue = DEFAULT_THREAD_POOL_CLASS_NAME)
    String getClassname();

    void setClassname(String value);

    /**
     * Idle threads are removed from pool, after this time (in seconds)
     */
    @Attribute(defaultValue = "" + IDLE_THREAD_TIMEOUT, dataType = Integer.class)
    String getIdleThreadTimeoutSeconds();

    void setIdleThreadTimeoutSeconds(String value);

    /**
     * The maxim number of tasks, which could be queued on the thread pool.  -1 disables any maximum checks.
     */
    @Attribute(defaultValue = "" + MAX_QUEUE_SIZE, dataType = Integer.class)
    String getMaxQueueSize();

    void setMaxQueueSize(String value);

    /**
     * Maximum number of threads in the thread pool servicing
     * requests in this queue. This is the upper bound on the no. of
     * threads that exist in the thread pool.
     */
    @Attribute(defaultValue = "" + MAX_THREADPOOL_SIZE, dataType = Integer.class)
    String getMaxThreadPoolSize();

    void setMaxThreadPoolSize(String value) throws PropertyVetoException;

    /**
     * Minimum number of threads in the thread pool servicing
     * requests in this queue. These are created up front when this
     * thread pool is instantiated
     */
    @Attribute(defaultValue = "" + MIN_THREADPOOL_SIZE, dataType = Integer.class)
    String getMinThreadPoolSize();

    void setMinThreadPoolSize(String value);

    /**
     * This is an id for the work-queue e.g. "thread-pool-1", "thread-pool-2" etc
     */
    @Attribute(required = true, key = true)
    String getName();

    void setName(String value);

    /**
     * This is an id for the work-queue e.g. "thread-pool-1", "thread-pool-2" etc
     */
    @Attribute
    @Deprecated
    String getThreadPoolId();

    void setThreadPoolId(String value);

    @DuckTyped
    List<NetworkListener> findNetworkListeners();

    class Duck {

        static public List<NetworkListener> findNetworkListeners(ThreadPool threadpool) {
            NetworkConfig config = threadpool.getParent().getParent(NetworkConfig.class);
            if (!Dom.unwrap(config).getProxyType().equals(NetworkConfig.class)) {
                config = Dom.unwrap(config).element("network-config").createProxy();
            }
            List<NetworkListener> listeners = config.getNetworkListeners().getNetworkListener();
            List<NetworkListener> refs = new ArrayList<NetworkListener>();
            for (NetworkListener listener : listeners) {
                if (listener.getThreadPool().equals(threadpool.getName())) {
                    refs.add(listener);
                }
            }
            return refs;
        }

    }
}
