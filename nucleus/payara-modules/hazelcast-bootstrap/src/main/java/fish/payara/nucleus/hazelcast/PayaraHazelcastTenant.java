/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2016-2017] Payara Foundation and/or its affiliates. All rights reserved.
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

import com.hazelcast.config.TenantControl;
import java.io.IOException;
import java.io.Serializable;
import lombok.RequiredArgsConstructor;
import org.glassfish.api.event.EventListener;
import org.glassfish.api.event.Events;
import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.internal.api.Globals;
import org.glassfish.internal.api.JavaEEContextUtil;
import org.glassfish.internal.api.JavaEEContextUtil.Context;
import org.glassfish.internal.data.ModuleInfo;
import org.glassfish.internal.deployment.Deployment;

/**
 * Java EE Context support for JCache with Hazelcast
 * 
 * @author lprimak
 */
public class PayaraHazelcastTenant implements TenantControl {
    public PayaraHazelcastTenant() {
        this(null);
    }

    public PayaraHazelcastTenant(final DestroyEvent event) {
        ctxUtil = Globals.getDefaultHabitat().getService(JavaEEContextUtil.class);
        if(event != null) {
            destroyEventListener = new SerializableEventListenerImpl(event,
                    Globals.getDefaultHabitat().getService(InvocationManager.class)
                            .getCurrentInvocation().getModuleName());
        } else {
            destroyEventListener = null;
        }
        init();
    }

    @Override
    public TenantControl saveCurrentTenant(DestroyEvent event) {
        return new PayaraHazelcastTenant(event);
    }

    @Override
    public void unregister() {
        if(destroyEventListener != null) {
            events.unregister(destroyEventListener);
        }
    }

    @Override
    public TenantControl.Closeable setTenant(boolean createRequestScope) {
        final Context ctx = createRequestScope? ctxUtil.pushRequestContext() : ctxUtil.pushContext();
        return new Closeable() {
            @Override
            public void close() {
                ctx.close();
            }
        };
    }

    private void init() {
        events = Globals.getDefaultHabitat().getService(Events.class);
        if(destroyEventListener != null) {
            events.register(destroyEventListener);
        }
    }

    private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        init();
    }

    private interface SerializableEventListener extends EventListener, Serializable {};

    @RequiredArgsConstructor
    private static class SerializableEventListenerImpl implements SerializableEventListener {
        @Override
        public void event(EventListener.Event payaraEvent) {
            if(payaraEvent.is(Deployment.MODULE_STOPPED)) {
                ModuleInfo moduleInfo = (ModuleInfo)payaraEvent.hook();
                if(destroyEvent != null && moduleInfo.getName().equals(moduleName)) {
                    destroyEvent.destroy();
                }
            }
        }

        private final DestroyEvent destroyEvent;
        private final String moduleName;
        private static final long serialVersionUID = 1L;
    }


    private final JavaEEContextUtil ctxUtil;
    private final EventListener destroyEventListener;
    private transient Events events;
    private static final long serialVersionUID = 1L;
}
