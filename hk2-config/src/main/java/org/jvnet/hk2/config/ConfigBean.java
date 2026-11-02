/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007-2017 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
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

package org.jvnet.hk2.config;

import javax.xml.stream.XMLStreamReader;
import java.beans.*;
import java.lang.reflect.Type;
import java.lang.reflect.Proxy;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import javax.management.ObjectName;

import org.glassfish.hk2.api.ServiceLocator;


/**
 * ConfigBean is the core implementation of the config beans. It has features like locking
 * view creation and optional features attachement.
 *
 * @author Jerome Dochez
 */
public class ConfigBean extends Dom implements ConfigView {
    private static final int WAIT_ITERATIONS = Integer.getInteger("org.glassfish.hk2.config.locktimeout.iterations", 100);

    private WriteableView writeableView;
    private volatile boolean writeLock = false;
    private final Map<Class , ConfigBeanInterceptor> optionalFeatures =
            new HashMap<Class, ConfigBeanInterceptor>();
    
    /**
        ObjectName will be null until when/if an MBean is registered.
     */
    private volatile ObjectName objectName = null;
    
    public ObjectName getObjectName() { return objectName; }
    
    public void setObjectName( final ObjectName objectNameIn )
    {
        if ( objectName != null ) throw new IllegalStateException();
        objectName = objectNameIn;
    }

    public ConfigBean(ServiceLocator habitat, DomDocument document, ConfigBean parent, ConfigModel model, XMLStreamReader in) {

        super(habitat, document, parent, model, in);
        // by default all ConfigBean support the ConstrainedBeanListener interface
        // allowing clients to register interest in attributes changing.
        addInterceptor(ConstrainedBeanListener.class ,new ConfigBeanInterceptor<ConstrainedBeanListener>() {

            List<VetoableChangeListener> listeners = new ArrayList<VetoableChangeListener>();

            public ConstrainedBeanListener getConfiguration() {
                return new ConstrainedBeanListener() {

                    public void removeVetoableChangeListener(VetoableChangeListener listener) {
                        listeners.remove(listener);
                    }

                    public void addVetoableChangeListener(VetoableChangeListener listener) {
                        listeners.add(listener);
                    }
                };
            }

            public void beforeChange(PropertyChangeEvent evt) throws PropertyVetoException {
                for (VetoableChangeListener listener : listeners) {
                    listener.vetoableChange(evt);
                }
            }
            public void afterChange(PropertyChangeEvent evt, long timestamp) {
            }

            public void readValue(ConfigBean source, String xmlName, Object Value) {
            }
        });
    }

    /**
     * Copy constructor, used to get a deep copy of the passed instance.
     *
     * @param source  the instance to copy 
     */
    public ConfigBean(Dom source, Dom parent) {
        super(source, parent);
    }

    /**
     * Returns a copy of itself providing the parent for the new copy.
     *
     * @param parent the parent instance for the cloned copy
     * @return the cloned copy
     */
    @Override
    protected <T extends Dom> T copy(T parent) {
        return (T) new ConfigBean(this, parent);
    }

    public boolean equals(Object o) {
        if (super.equals(o)) {
            if(((ConfigBean)o).objectName == this.objectName) {
                return true;
            }
        }
        return false;
    }

    public int hashCode() {
        return System.identityHashCode(this);
    }

    /**
     * Returns an optional feature of the ConfigBean. Optional features are implemented
     * by other objects and attached to this instance. Attached features can be queried
     * using the getOptionalFeature method giving the type of the requestion optional
     * feature.
     * 
     * @param featureType type of the optional feature requested.
     * @return optional feature implementation is one is attached to this instance
     */
    @SuppressWarnings("unchecked")    
    public <T> T getOptionalFeature(Class<T> featureType) {
        if (optionalFeatures.containsKey(featureType)) {
            return (T) optionalFeatures.get(featureType).getConfiguration();
        }
        return null;
    }

    Collection<ConfigBeanInterceptor> getOptionalFeatures() {
        return optionalFeatures.values();
    }

    protected void setter(ConfigModel.Property target, Object value) throws Exception  {
        if (!writeLock) {
            throw new PropertyVetoException("Instance of " + getImplementation() + " named '" + getKey() +
                    "' is not locked for writing when changing attribute " + target.xmlName()
                    + ", you must use transaction semantics to access it.", null);
        }
        _setter(target, value);
    }

    void _setter(ConfigModel.Property target, Object value) throws Exception  {    
        Object oldValue = super.getter(target, value.getClass());
        PropertyChangeEvent evt = new PropertyChangeEvent(this, target.xmlName(), oldValue, value);
        for (ConfigBeanInterceptor interceptor : optionalFeatures.values()) {
            interceptor.beforeChange(evt);
        }
        super.setter(target, value);
        for (ConfigBeanInterceptor interceptor : optionalFeatures.values()) {
            interceptor.afterChange(evt, System.currentTimeMillis());
        }
    }

    Object _getter(ConfigModel.Property target, Type t) {
        final Object value = super.getter(target,t);
        for (ConfigBeanInterceptor interceptor : optionalFeatures.values()) {
            interceptor.readValue(this, target.xmlName(), value);
        }
        return value;        
    }

    protected Object getter(ConfigModel.Property target, Type t) {
        final Object value = _getter(target, t);
        if (value instanceof List) {
            final List valueList = (List) value;

            // we need to protect this list as it was obtained from a readable view...
            return new AbstractList() {

                public int size() {
                    return valueList.size();
                }

                public Object get(int index) {
                    return valueList.get(index);
                }

                public boolean add(Object o) {
                    throw new IllegalStateException("Not part of a transaction !", null);
                }
                public Object set(int index, Object element) {
                    throw new IllegalStateException("Not part of a transaction !", null);
                }

                public Object remove(int index) {
                    throw new IllegalStateException("Not part of a transaction !", null);
                }
            };

        }
        return value;
    }

    /**
     * Add a new ConfigBeanInterceptor to this ConfigBean instance. The inteceptor will
     * be called each time a attribute of this bean is accessed.
     *
     * @param interceptorType type of the type interceptor.
     * @param interceptor the new interceptor
     */
    public void addInterceptor(Class<?> interceptorType, ConfigBeanInterceptor interceptor) {
        optionalFeatures.put(interceptorType, interceptor);
    }

    /**
     * We are the master view.
     *
     * @return the master view
     */
    public ConfigBean getMasterView() {
        return this;
    }

    public void setMasterView(ConfigView view) {        
    }

    /**
     * Creates a proxy for this view.
     *
     * @param proxyType requested proxy type
     * @return Java SE proxy
     */
    public <T extends ConfigBeanProxy> T getProxy(final Class<T> proxyType) {
        ClassLoader cl;
        if (System.getSecurityManager()!=null) {
            cl = AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
                @Override
                public ClassLoader run() {
                    return proxyType.getClassLoader();
                }
            });
        } else {
            cl = proxyType.getClassLoader();
        }
        return proxyType.cast(Proxy.newProxyInstance(cl, new Class[] { proxyType} , this));
    }

    /**
     * Allocate a new ConfigBean object as part of the Transaction
     * associated with this configuration object. This will eventually
     * be moved to a factory.
     *
     * @param type the request configuration object type
     * @return the properly constructed configuration object
     */

    public ConfigBean allocate(Class<?> type) {
        return (ConfigBean) document.make(getHabitat(), null, this, document.buildModel(type));
   }

    /**
     * Allocate a new ConfigBean object as part of the Transaction
     * associated with this configuration object. This will eventually
     * be moved to a factory.
     *
     * @param type the request configuration object type
     * @return the propertly constructed configuration object
     */

    <T extends ConfigBeanProxy> T allocateProxy(Class<T> type) {
        return allocate(type).createProxy(type);
    }

    /**
     * Returns the lock on this object, only one external view (usually the writeable view) can
     * acquire the lock ensuring that the objects cannot be concurrently modified
     *
     * @return lock instance
     */
    public Lock getLock() {
        return lock;

    }

    @Override
    public ConfigBean parent() {
        return (ConfigBean) super.parent();
    }

    void setWriteableView(WriteableView writeableView) {
        if (writeLock) {
            this.writeableView = writeableView;
        } else {
            throw new IllegalStateException("Config bean is not locked");
        }
    }

    WriteableView getWriteableView() {
        return writeableView;
    }

    /**
     * simplistic non reentrant lock implementation, needs rework
     */
    final private Lock lock = new Lock() {
        
        public void lock() {
            throw new UnsupportedOperationException();
        }

        public void lockInterruptibly() throws InterruptedException {
            throw new UnsupportedOperationException();
        }

        public synchronized boolean tryLock() {
            if (!writeLock) {
                writeLock=true;
                return true;
            }
            return false;
        }

        public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
            long nanosTimeout = TimeUnit.NANOSECONDS.convert(time, unit);
            long increment = nanosTimeout / WAIT_ITERATIONS;
            long lastTime = System.nanoTime();
            for (; ;) {
                if (tryLock()) {
                    return true;
                }
                if (nanosTimeout < 0) {
                    return false;
                }
                LockSupport.parkNanos(increment);
                long now = System.nanoTime();
                nanosTimeout -= now - lastTime;
                lastTime = now;
                if (Thread.interrupted())
                    break;
            }
            throw new InterruptedException();
        }

        public synchronized void unlock() {
            writeLock = false;
            writeableView = null;
        }

        public Condition newCondition() {
            throw new UnsupportedOperationException();
        }
    };
}
