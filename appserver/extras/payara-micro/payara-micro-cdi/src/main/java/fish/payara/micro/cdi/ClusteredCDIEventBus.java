/*

 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

 Copyright (c) 2016 Payara Foundation. All rights reserved.

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
package fish.payara.micro.cdi;

import fish.payara.micro.PayaraMicroRuntime;
import fish.payara.appserver.micro.services.CDIEventListener;
import fish.payara.appserver.micro.services.PayaraClusteredCDIEvent;
import java.io.IOException;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;

/**
 *
 * @author steve
 */
@ApplicationScoped
public class ClusteredCDIEventBus implements CDIEventListener {

    @Inject
    PayaraMicroRuntime runtime;

    @Inject
    BeanManager bm;

    @Inject
    @Inbound
    Event<Serializable> clusterEvent;

    @Resource
    ManagedExecutorService managedExecutorService;

    ClassLoader capturedClassLoader;

    @Override
    public void eventReceived(final PayaraClusteredCDIEvent event) {
        final ClassLoader oldTCCL = Thread.currentThread().getContextClassLoader();
        managedExecutorService.submit(new Runnable() {
            @Override
            public void run() {
                try {

                    Thread.currentThread().setContextClassLoader(capturedClassLoader);
                    Serializable eventPayload = event.getPayload();
                    clusterEvent.fire(eventPayload);
                } catch (IOException | ClassNotFoundException ex) {
                    Logger.getLogger(ClusteredCDIEventBus.class.getName()).log(Level.FINE, "Received event which could not be deserialized", ex);
                } finally {
                    Thread.currentThread().setContextClassLoader(oldTCCL);
                }
            }
        });
    }

    public void initialize() {
        Logger.getLogger(ClusteredCDIEventBus.class.getName()).log(Level.INFO, "Clustered CDI Event bus initialized");
    }

    void onOutboundEvent(@Observes @Outbound Serializable event) {
        PayaraClusteredCDIEvent clusteredEvent;
        try {
            clusteredEvent = new PayaraClusteredCDIEvent(runtime.getLocalDescriptor(), event);
            runtime.publishCDIEvent(clusteredEvent);
        } catch (IOException ex) {
        }
    }

    @PostConstruct
    void postConstruct() {
        runtime.addCDIEventListener(this);
        capturedClassLoader = Thread.currentThread().getContextClassLoader();
    }

    @PreDestroy
    void preDestroy() {
        runtime.removeCDIEventListener(this);
    }

}
