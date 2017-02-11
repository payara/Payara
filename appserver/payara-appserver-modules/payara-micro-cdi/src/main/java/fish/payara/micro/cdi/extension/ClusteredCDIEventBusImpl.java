/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2016 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.micro.cdi.extension;

import fish.payara.micro.cdi.Outbound;
import fish.payara.micro.cdi.Inbound;
import fish.payara.micro.cdi.ClusteredCDIEventBus;
import fish.payara.appserver.micro.services.PayaraClusteredCDIEventImpl;
import fish.payara.micro.event.CDIEventListener;
import fish.payara.micro.event.PayaraClusteredCDIEvent;
import fish.payara.appserver.micro.services.PayaraInstance;
import java.io.IOException;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.EventMetadata;
import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletContext;
import org.glassfish.api.invocation.ComponentInvocation;
import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.internal.api.Globals;

/**
 *
 * @author steve
 */
@ApplicationScoped
public class ClusteredCDIEventBusImpl implements CDIEventListener, ClusteredCDIEventBus {

    @Inject
    private PayaraInstance runtime;

    @Inject
    private BeanManager bm;

    @Resource
    private ManagedExecutorService managedExecutorService;

    private ComponentInvocation capturedInvocation;

    private InvocationManager im;

    private ClassLoader capturedClassLoader;
    
    private final static String INSTANCE_PROPERTY = "InstanceName";
    
    private final static String EVENT_PROPERTY = "EventName";
    
    @PostConstruct
    void postConstruct() {
        runtime.addCDIListener(this);
        capturedClassLoader = Thread.currentThread().getContextClassLoader();
        im = Globals.getDefaultHabitat().getService(InvocationManager.class);
        capturedInvocation = im.getCurrentInvocation();
        if (managedExecutorService == null) {
            try {
                InitialContext ctx = new InitialContext();
                managedExecutorService = (ManagedExecutorService) ctx.lookup("java:comp/DefaultManagedExecutorService");
            } catch (NamingException ex) {
                Logger.getLogger(ClusteredCDIEventBusImpl.class.getName()).log(Level.SEVERE, null, ex);
            }

        }
    }

    @PreDestroy
    void preDestroy() {
        runtime.removeCDIListener(this);
        capturedClassLoader = null;
        capturedInvocation = null;
    }
    
    public void onStart(@Observes @Initialized(ApplicationScoped.class) ServletContext init) {
       initialize();
       if (runtime.isClustered()) {
            Logger.getLogger(ClusteredCDIEventBusImpl.class.getName()).log(Level.INFO, "Clustered CDI Event bus initialized for " + init.getContextPath());
        }
    }
    
    @Override
    public void initialize() {
        
        // try again

        if (im == null) {
            im = Globals.getDefaultHabitat().getService(InvocationManager.class);
        }
        
        if (capturedInvocation == null) {
            capturedInvocation = im.getCurrentInvocation(); 
        }
        
        if (managedExecutorService == null) {
            try {
                InitialContext ctx = new InitialContext();
                managedExecutorService = (ManagedExecutorService) ctx.lookup("java:comp/DefaultManagedExecutorService");
            } catch (NamingException ex) {
                Logger.getLogger(ClusteredCDIEventBusImpl.class.getName()).log(Level.SEVERE, null, ex);
            }

        }
    }

    @Override
    public void eventReceived(final PayaraClusteredCDIEvent event) {
        
        // first check if the event is targetted at a specific instance
        String instanceName = event.getProperty(INSTANCE_PROPERTY);
        if (!(instanceName == null) && !(instanceName.length() == 0) ) {
            // there is an instance name filter
            String names[] = instanceName.split(",");
            boolean forUs = false;
            String thisInstance = runtime.getInstanceName();
            for (String name : names) {
                if (name.equals(thisInstance)) {
                    forUs = true;
                    break;
                }
            }
            if (!forUs)
                return;
        }

        // as we are on the hazelcast thread we need to establish the invocation manager
        ComponentInvocation newInvocation = new ComponentInvocation(capturedInvocation.getComponentId(),
                capturedInvocation.getInvocationType(),
                capturedInvocation.getContainer(),
                capturedInvocation.getAppName(),
                capturedInvocation.getModuleName());
        final ClassLoader oldTCCL = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(capturedClassLoader);
            im.preInvoke(newInvocation);

            managedExecutorService.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        Serializable eventPayload = event.getPayload();
                        Inbound inbound = new Inbound() {
                            @Override
                            public String eventName() {
                                return event.getProperty(EVENT_PROPERTY);
                            }

                            @Override
                            public Class<? extends Annotation> annotationType() {
                                return Inbound.class;
                            }
                        };
                        bm.fireEvent(eventPayload,inbound);
                    } catch (IOException | ClassNotFoundException ex) {
                        Logger.getLogger(ClusteredCDIEventBusImpl.class.getName()).log(Level.FINE, "Received event which could not be deserialized", ex);
                    }
                }
            });
        } finally {
            Thread.currentThread().setContextClassLoader(oldTCCL);
            im.postInvoke(newInvocation);
        }
    }

    void onOutboundEvent(@Observes @Outbound Serializable event, EventMetadata meta) {
        PayaraClusteredCDIEvent clusteredEvent;
        
        // read the metadata on the Outbound Annotation to set data into the event
        try {
            boolean loopBack = false;
            String eventName = "";
            String instanceName = "";
            for (Annotation annotation : meta.getQualifiers()) {
                if (annotation instanceof Outbound) {
                    Outbound outboundattn = (Outbound)annotation;
                    eventName = outboundattn.eventName();
                    loopBack = outboundattn.loopBack();
                    instanceName = outboundattn.instanceName();
                }
            }
            clusteredEvent = new PayaraClusteredCDIEventImpl(runtime.getLocalDescriptor(), event);
            clusteredEvent.setLoopBack(loopBack);
            clusteredEvent.setProperty(EVENT_PROPERTY, eventName);
            clusteredEvent.setProperty(INSTANCE_PROPERTY, instanceName);
            runtime.publishCDIEvent(clusteredEvent);
        } catch (IOException ex) {
        }
    }

}
