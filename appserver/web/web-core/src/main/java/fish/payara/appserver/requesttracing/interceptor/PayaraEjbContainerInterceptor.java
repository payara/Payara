/*
 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 Copyright (c) 2016 C2B2 Consulting Limited. All rights reserved.
 The contents of this file are subject to the terms of the Common Development
 and Distribution License("CDDL") (collectively, the "License").  You
 may not use this file except in compliance with the License.  You can
 obtain a copy of the License at
 https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 or packager/legal/LICENSE.txt.  See the License for the specific
 language governing permissions and limitations under the License.
 When distributing the software, include this License Header Notice in each
 file and include the License file at packager/legal/LICENSE.txt.
 */
package fish.payara.appserver.requesttracing.interceptor;

import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.deployment.EjbDescriptor;
import fish.payara.nucleus.requesttracing.RequestTracingService;
import fish.payara.nucleus.requesttracing.domain.EjbRequestEvent;
import fish.payara.nucleus.requesttracing.domain.EventType;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.ejb.spi.EjbContainerInterceptor;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import javax.inject.Provider;

/**
 * @author mertcaliskan
 *
 * Interceptor that will be invoked for before and after of all EJB calls.
 */
@Service(name = "rt-interceptor-ejb")
@RunLevel(StartupRunLevel.VAL)
public class PayaraEjbContainerInterceptor implements EjbContainerInterceptor {

    @Inject
    private RequestTracingService service;

    @Inject
    private Domain domain;

    @Inject
    private Server server;

    private ThreadLocal<EjbRequestEvent> requestEventStore;

    public void preInvoke(EjbDescriptor ejbDescriptor) {
        if (!service.isRequestTracingEnabled()) {
            return;
        }
        requestEventStore = new ThreadLocal<EjbRequestEvent>();

        EjbRequestEvent requestEvent = new EjbRequestEvent();
        requestEvent.setServer(server.getName());
        requestEvent.setDomain(domain.getName());
        requestEvent.setHomeClassName(ejbDescriptor.getHomeClassName());
        requestEvent.setEjbClassName(ejbDescriptor.getEjbClassName());
        requestEvent.setRemoteClassName(ejbDescriptor.getRemoteClassName());
        requestEvent.setJndiName(ejbDescriptor.getJndiName());
        requestEvent.setTransactionType(ejbDescriptor.getTransactionType());
        requestEvent.setIsLocalBean(ejbDescriptor.isLocalBean());
        requestEvent.setEventType(EventType.EJB);
        requestEventStore.set(requestEvent);
    }

    public void postInvoke(EjbDescriptor ejbDescriptor) {
        if (!service.isRequestTracingEnabled()) {
            return;
        }

        EjbRequestEvent requestEvent = requestEventStore.get();
        if (requestEvent != null) {
            requestEvent.setElapsedTime(System.currentTimeMillis() - requestEvent.getTimestamp());
            service.traceRequestEvent(requestEvent);
            requestEventStore.remove();
        }
    }
}
