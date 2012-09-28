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

package org.glassfish.ejb.config;

import org.jvnet.hk2.config.Attribute;
import org.jvnet.hk2.config.Configured;
import org.jvnet.hk2.config.Element;
import org.jvnet.hk2.config.ConfigBeanProxy;

import java.beans.PropertyVetoException;
import java.util.List;

import org.glassfish.api.admin.config.PropertiesDesc;
import org.jvnet.hk2.config.types.Property;
import org.jvnet.hk2.config.types.PropertyBag;
import org.glassfish.quality.ToDo;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import org.glassfish.api.admin.config.PropertyDesc;
import org.glassfish.api.admin.config.ConfigExtension;

/**
 * Configuration of EJB Container
 */

/* @XmlType(name = "", propOrder = {
    "ejbTimerService",
    "property"
}) */

@Configured
public interface EjbContainer extends ConfigBeanProxy, PropertyBag, ConfigExtension {
    public static final int DEFAULT_THREAD_CORE_POOL_SIZE = 16;
    public static final int DEFAULT_THREAD_MAX_POOL_SIZE = 32;
    public static final long DEFAULT_THREAD_KEEP_ALIVE_SECONDS = 60;
    public static final int DEFAULT_THREAD_QUEUE_CAPACITY = Integer.MAX_VALUE;
    public static final boolean DEFAULT_ALLOW_CORE_THREAD_TIMEOUT = false;
    public static final boolean DEFAULT_PRESTART_ALL_CORE_THREADS = false;
 
    /**
     * Gets the value of the steadyPoolSize property.
     *
     * (slsb,eb) number of bean instances normally maintained in pool.
     * When a pool is first created, it will be populated with size equal to
     * steady-pool-size. When an instance is removed from the pool, it is
     * replenished asynchronously, so that the pool size is at or above the
     * steady-pool-size. This addition will be in multiples of
     * pool-resize-quantity. When a bean is disassociated from a method
     * invocation, it is put back in the pool, subject to max-pool-size limit.
     * If the max pool size is exceeded the bean id destroyed immediately.
     * A pool cleaning thread, executes at an interval defined by
     * pool-idle-timeout-in-seconds. This thread reduces the pool size to
     * steady-pool-size, in steps defined by pool-resize-quantity. If the pool
     * is empty, the required object will be created and returned immediately.
     * This prevents threads from blocking till the pool is replenished by the
     * background thread. steady-pool-size must be greater than 1 and at most
     * equal to the max-pool-size.
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="0")
    @Min(value=0)        
    String getSteadyPoolSize();

    /**
     * Sets the value of the steadyPoolSize property
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setSteadyPoolSize(String value) throws PropertyVetoException;

    /**
     * Gets the value of the poolResizeQuantity property.
     *
     * (slsb,eb) size of bean pool grows (shrinks) in steps specified by
     * pool-resize-quantity, subject to max-pool-size (steady-pool-size) limit.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="8")
    @Min(value=0)        
    String getPoolResizeQuantity();

    /**
     * Sets the value of the poolResizeQuantity property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setPoolResizeQuantity(String value) throws PropertyVetoException;

    /**
     * Gets the value of the maxPoolSize property.
     *
     * (slsb,eb) maximum size, a pool can grow to. A value of 0 implies an
     * unbounded pool. Unbounded pools eventually shrink to the steady-pool-size,
     * in steps defined by pool-resize-quantity.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="32")
    @Min(value=0)    
    String getMaxPoolSize();

    /**
     * Sets the value of the maxPoolSize property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setMaxPoolSize(String value) throws PropertyVetoException;

    /**
     * Gets the value of the cacheResizeQuantity property.
     * (eb,sfsb) Cache elements have identity, hence growth is in unit steps and
     * created on demand. Shrinking of cache happens when
     * cache-idle-timeout-in-seconds timer expires and a cleaner thread
     * passivates beans which have been idle for longer than
     * cache-idle-timeout-in-seconds. All idle instances are passivated at once.
     * cache-resize-quantity does not apply in this case.
     *
     * When max cache size is reached, an asynchronous task is created to bring
     * the size back under the max-cache-size limit. This task removes
     * cache-resize-quantity elements, consulting the victim-selection-policy.
     *
     * Must be greater than 1 and less than max-cache-size.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="32")
    @Min(value=1)
    String getCacheResizeQuantity();

    /**
     * Sets the value of the cacheResizeQuantity property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setCacheResizeQuantity(String value) throws PropertyVetoException;

    /**
     * Gets the value of the maxCacheSize property.
     *
     * (sfsb,eb) specifies the maximum number of instances that can be cached.
     * For entity beans, internally two caches are maintained for higher
     * concurrency: (i) Ready (R$) (ii) Active in an Incomplete Transaction(TX$)
     *
     * The TX$ is populated with instances from R$ or from the Pool directly.
     * When an instance in TX$ completes the transaction, it is placed back in
     * R$ (or in pool, in case an instance with same identity already is in R$).
     *
     * max-cache-size only specifies the upper limit for R$.
     *
     * The container computes an appropriate size for TX$. For SFSBs, after the
     * max-cache-size is reached, beans(as determined by victim-selection-policy)
     * get passivated.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="512")
    @Min(value=0)
    String getMaxCacheSize();

    /**
     * Sets the value of the maxCacheSize property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setMaxCacheSize(String value) throws PropertyVetoException;

    /**
     * Gets the value of the poolIdleTimeoutInSeconds property.
     *
     * (slsb,eb) defines the rate at which the pool cleaning thread is executed.
     * This thread checks if current size is greater than steady pool size, it
     * removes pool-resize-quantity elements. If the current size is less than
     * steady-pool-size it is increased by pool-resize-quantity, with a ceiling
     * of min (current-pool-size + pool-resize-quantity, max-pool-size)
     *
     * Only objects that have not been accessed for more than
     * pool-idle-timeout-in-seconds are candidates for removal.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="600")
    @Min(value=0)    
    String getPoolIdleTimeoutInSeconds();

    /**
     * Sets the value of the poolIdleTimeoutInSeconds property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setPoolIdleTimeoutInSeconds(String value) throws PropertyVetoException;

    /**
     * Gets the value of the cacheIdleTimeoutInSeconds property.
     *
     * (eb, sfsb) specifies the rate at which the cache cleaner thread is
     * scheduled. All idle instances are passivated at once.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="600")
    @Min(value=0)    
    String getCacheIdleTimeoutInSeconds();

    /**
     * Sets the value of the cacheIdleTimeoutInSeconds property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setCacheIdleTimeoutInSeconds(String value) throws PropertyVetoException;

    /**
     * Gets the value of the removalTimeoutInSeconds property.
     *
     * (sfsb) Instance is removed from cache or passivation store, if it is not
     * accesed within this time. All instances that can be removed, will be removed.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="5400")
    @Min(value=0)    
    String getRemovalTimeoutInSeconds();

    /**
     * Sets the value of the removalTimeoutInSeconds property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setRemovalTimeoutInSeconds(String value) throws PropertyVetoException;

    /**
     * Gets the value of the victimSelectionPolicy property.
     *
     * (sfsb) Victim selection policy when cache needs to shrink. Victims are
     * passivated. Entity Bean Victims are selected always using fifo discipline
     * Does not apply to slsb because it does not matter, which particular
     * instances are removed.
     *
     * fifo
     *     method picks victims, oldest instance first.
     * lru
     *     algorithm picks least recently accessed instances.
     * nru
     *     policy tries to pick 'not recently used' instances and is a
     *     pseudo-random selection process.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="nru")
    @Pattern(regexp="(nru|fifo|lru)")
    String getVictimSelectionPolicy();

    /**
     * Sets the value of the victimSelectionPolicy property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setVictimSelectionPolicy(String value) throws PropertyVetoException;

    /**
     * Gets the value of the commitOption property.
     *
     * (eb) Entity Beans caching is controlled by this setting.
     * Commit Option C implies that no caching is performed in the container.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="B")
    @Pattern(regexp="B|C")
    String getCommitOption();

    /**
     * Sets the value of the commitOption property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setCommitOption(String value) throws PropertyVetoException;

    /**
     * Gets the value of the sessionStore property.
     *
     * specifies the directory where passivated beans & persisted HTTP sessions
     * are stored on the file system. Defaults to $INSTANCE-ROOT/session-store
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute(defaultValue = "${com.sun.aas.instanceRoot}/session-store")
    String getSessionStore();

    /**
     * Sets the value of the sessionStore property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setSessionStore(String value) throws PropertyVetoException;

    /**
     * Gets the value of the ejbTimerService property.
     *
     * Contains the configuration for  the ejb timer service.
     * There is at most one ejb timer service per server instance.
     *
     * @return possible object is
     *         {@link EjbTimerService }
     */
    @Element
    @NotNull
    EjbTimerService getEjbTimerService();

    /**
     * Sets the value of the ejbTimerService property.
     *
     * @param value allowed object is
     *              {@link EjbTimerService }
     */
    void setEjbTimerService(EjbTimerService value) throws PropertyVetoException;
    
    
    /**
    	Properties as per {@link PropertyBag}
     */
    @ToDo(priority=ToDo.Priority.IMPORTANT, details="Provide PropertyDesc for legal props" )
    @PropertiesDesc(props = {
        @PropertyDesc(name = "disable-nonportable-jndi-names",
        defaultValue = "false",
        values = {"true", "false"}),
        
        @PropertyDesc(name = "thread-core-pool-size"),
        @PropertyDesc(name = "thread-max-pool-size"),
        @PropertyDesc(name = "thread-keep-alive-seconds"),
        @PropertyDesc(name = "thread-queue-capacity"),
        @PropertyDesc(name = "allow-core-thread-timeout"),
        @PropertyDesc(name = "prestart-all-core-threads")
    })
    @Element
    List<Property> getProperty();
}
