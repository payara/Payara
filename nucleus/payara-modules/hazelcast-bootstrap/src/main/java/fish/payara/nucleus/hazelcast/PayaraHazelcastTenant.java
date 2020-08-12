/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2016-2020] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.nucleus.hazelcast;

import com.hazelcast.cache.impl.operation.AddCacheConfigOperation;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.DataSerializable;
import com.hazelcast.spi.impl.operationservice.Operation;
import com.hazelcast.spi.tenantcontrol.DestroyEventContext;
import com.hazelcast.spi.tenantcontrol.TenantControl;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.api.event.EventListener;
import org.glassfish.api.event.Events;
import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.deployment.versioning.VersioningUtils;
import org.glassfish.internal.api.Globals;
import org.glassfish.internal.api.JavaEEContextUtil;
import org.glassfish.internal.api.JavaEEContextUtil.Instance;
import org.glassfish.internal.data.ApplicationInfo;
import org.glassfish.internal.data.ModuleInfo;
import org.glassfish.internal.deployment.Deployment;

/**
 * Java EE Context and class loading support for Hazelcast objects and thread-callbacks
 *
 * @author lprimak
 */
public class PayaraHazelcastTenant implements TenantControl, DataSerializable {
    private final JavaEEContextUtil ctxUtil = Globals.getDefaultHabitat().getService(JavaEEContextUtil.class);
    private final Events events = Globals.getDefaultHabitat().getService(Events.class);
    private final InvocationManager invMgr = Globals.getDefaultHabitat().getService(InvocationManager.class);
    private final Lock lock = new ReentrantLock();
    private final Condition condition = lock.newCondition();
    private static final Logger log = Logger.getLogger(PayaraHazelcastTenant.class.getName());
    private static final Map<String, Integer> blockedCounts = new ConcurrentHashMap<>();
    private static final Set<String> disabledTenants = new ConcurrentSkipListSet<>();
    private static final Set<Class<?>> filteredClasses = new ConcurrentSkipListSet<>();

    // transient fields
    private EventListenerImpl destroyEventListener;

    // serialized fields
    private Instance contextInstance;
    private String moduleName;

    static {
        filteredClasses.add(AddCacheConfigOperation.class);
    }


    PayaraHazelcastTenant() {
        if (invMgr.getCurrentInvocation() != null) {
            contextInstance = ctxUtil.currentInvocation();
            moduleName = VersioningUtils.getUntaggedName(invMgr.getCurrentInvocation().getModuleName());
        } else {
            contextInstance = ctxUtil.empty();
        }
    }

    @Override
    public void distributedObjectCreated(Optional<DestroyEventContext> destroyContext) {
        destroyEventListener = new EventListenerImpl(destroyContext);
        events.register(destroyEventListener);
    }

    @Override
    public void distributedObjectDestroyed() {
        // Hazelcast object has been destroyed
        events.unregister(destroyEventListener);
        destroyEventListener = null;
    }

    @Override
    public Closeable setTenant() {
        try {
            return contextInstance.pushRequestContext()::close;
        } catch (IllegalStateException exc) {
            throw exc;
        }
    }

    @Override
    public void writeData(ObjectDataOutput out) throws IOException {
        out.writeUTF(contextInstance.getInstanceComponentId());
        out.writeUTF(moduleName);
    }

    @Override
    public void readData(ObjectDataInput in) throws IOException {
        String componentId = in.readUTF();
        contextInstance = componentId == null ? ctxUtil.empty() : ctxUtil.fromComponentId(componentId);
        moduleName = in.readUTF();
    }

    @Override
    public boolean isAvailable(Operation op) {
        if (!contextInstance.isLoaded()) {
            if (filter(op) || disabledTenants.contains(moduleName)) {
                return true;
            }
            lock.lock();
            try {
                String componentId = contextInstance.getInstanceComponentId();
                int unavailableCount = blockedCounts.compute(componentId, (k, v) -> v == null ? 0 : ++v);
                log.log(unavailableCount > 100 ? Level.INFO : Level.FINEST,
                        String.format("BLOCKED: tenant not available: %s, module %s, Operation: %s",
                                componentId, moduleName, op.toString()));
                if (unavailableCount > 100) {
                    blockedCounts.remove(componentId);
                }
                condition.await(100, TimeUnit.MILLISECONDS);
            } catch (InterruptedException ex) {
            } finally {
                lock.unlock();
            }
            return false;
        }
        return true;
    }

    private boolean filter(Operation op) {
        return filteredClasses.stream().anyMatch(cls -> cls.isInstance(op));
    }

    @Override
    public void clearThreadContext() {
        if (!invMgr.isInvocationStackEmpty()) {
            log.warning(() -> String.format("clearThreadContext - non-empty invocations: %s", invMgr.getAllInvocations().toString()));
            invMgr.putAllInvocations(null);
        }
    }

    private void tenantUnavailable() {
        contextInstance.clearInstanceInvocation();
    }

    Instance getContextInstance() {
        return contextInstance;
    }

    public static Set<String> getDisabledTenants() {
        return disabledTenants;
    }

    public static Set<Class<?>> getFilteredClasses() {
        return filteredClasses;
    }

    private class EventListenerImpl implements EventListener {
        private final Optional<DestroyEventContext> destroyEvent;


        private EventListenerImpl(Optional<DestroyEventContext> event) {
            this.destroyEvent = event;
        }

        @Override
        public void event(EventListener.Event payaraEvent) {
            if (payaraEvent.is(Deployment.MODULE_STARTED)) {
                ModuleInfo hook = (ModuleInfo) payaraEvent.hook();
                if (!(hook instanceof ApplicationInfo) && VersioningUtils.getUntaggedName(hook.getName()).equals(moduleName)) {
                    lock.lock();
                    try {
                        condition.signalAll();
                    } finally {
                        lock.unlock();
                    }
                }
            } else if (payaraEvent.is(Deployment.MODULE_STOPPED)) {
                ModuleInfo hook = (ModuleInfo) payaraEvent.hook();
                if (!(hook instanceof ApplicationInfo) && VersioningUtils.getUntaggedName(hook.getName()).equals(moduleName)) {
                    // decouple the tenant classes from the event
                    tenantUnavailable();
                    destroyEvent.ifPresent(DestroyEventContext::tenantUnavailable);
                }
            }
        }
    }
}
