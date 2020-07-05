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

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.DataSerializable;
import com.hazelcast.spi.tenantcontrol.DestroyEventContext;
import com.hazelcast.spi.tenantcontrol.TenantControl;
import java.io.Closeable;
import java.io.IOException;
import org.glassfish.api.event.EventListener;
import org.glassfish.api.event.Events;
import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.internal.api.Globals;
import org.glassfish.internal.api.JavaEEContextUtil;
import org.glassfish.internal.data.ModuleInfo;
import org.glassfish.internal.deployment.Deployment;

/**
 * Java EE Context and class loading support for Hazelcast objects and thread-callbacks
 * 
 * @author lprimak
 */
public class PayaraHazelcastTenant implements TenantControl, DataSerializable {
    private JavaEEContextUtil ctxUtil;
    private EventListenerImpl destroyEventListener;
    private Events events;


    public PayaraHazelcastTenant(final DestroyEventContext event) {
        init(event);
    }

    public PayaraHazelcastTenant() { } // de-serialization requirement

    private void init() {
        ctxUtil = Globals.getDefaultHabitat().getService(JavaEEContextUtil.class);
        events = Globals.getDefaultHabitat().getService(Events.class);
    }

    private void init(DestroyEventContext event) {
        init();
        destroyEventListener = new EventListenerImpl(event,
                Globals.getDefaultHabitat().getService(InvocationManager.class)
                        .getCurrentInvocation().getModuleName());
    }

    private void init(String componentId, DestroyEventContext destroyEvent, String moduleName) {
        init();
        ctxUtil.setInstanceComponentId(componentId);
        destroyEventListener = new EventListenerImpl(destroyEvent, moduleName);
    }

    @Override
    public void register() {
        events.register(destroyEventListener);
    }

    @Override
    public void unregister() {
        // Hazelcast object has been destroyed
        events.unregister(destroyEventListener);
    }

    @Override
    public Closeable setTenant(boolean createRequestScope) {
        return (createRequestScope? ctxUtil.pushRequestContext() : ctxUtil.pushContext())::close;
    }

    @Override
    public void writeData(ObjectDataOutput out) throws IOException {
        out.writeUTF(ctxUtil.getInstanceComponentId());
        out.writeObject(destroyEventListener.destroyEvent);
        out.writeUTF(destroyEventListener.moduleName);
    }

    @Override
    public void readData(ObjectDataInput in) throws IOException {
        String componentId = in.readUTF();
        DestroyEventContext destroyEvent = in.readObject();
        String moduleName = in.readUTF();
        init(componentId, destroyEvent, moduleName);
    }

    @Override
    public void tenantUnavailable() {
        ctxUtil.clearInstanceInvocation();
    }

    @Override
    public boolean isAvailable() {
        return ctxUtil.isLoaded();
    }

    private static class EventListenerImpl implements EventListener {
        private final DestroyEventContext destroyEvent;
        private final String moduleName;


        private EventListenerImpl(DestroyEventContext event, String moduleName) {
            this.destroyEvent = event;
            this.moduleName = moduleName;
        }

        @Override
        public void event(EventListener.Event payaraEvent) {
            if(payaraEvent.is(Deployment.MODULE_STOPPED)) {
                if(((ModuleInfo)payaraEvent.hook()).getName().equals(moduleName)) {
                    // decouple the tenant classes from the event
                    destroyEvent.tenantUnavailable(Globals.getDefaultHabitat().getService(HazelcastCore.class).getInstance());
                }
            }
        }
    }
}
