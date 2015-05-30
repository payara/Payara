/*

 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

 Copyright (c) 2015 C2B2 Consulting Limited. All rights reserved.

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
package fish.payara.micro.services;

import com.hazelcast.core.IMap;
import fish.payara.nucleus.hazelcast.HazelcastCore;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.api.event.EventListener;
import org.glassfish.api.event.EventTypes;
import org.glassfish.deployment.common.DeploymentContextImpl;
import org.glassfish.hk2.runlevel.RunLevel;
import org.glassfish.internal.data.ApplicationInfo;
import org.glassfish.internal.deployment.Deployment;
import org.jboss.logging.Logger;
import org.jvnet.hk2.annotations.Service;

/**
 *
 * @author steve
 */
@Service(name = "payara-micro-instance")
@RunLevel(StartupRunLevel.VAL)
public class PayaraMicroInstance implements EventListener {
    
    private static final Logger logger = Logger.getLogger(PayaraMicroInstance.class);
    
    @Inject
    HazelcastCore hazelcast;
    
    IMap payaraMicroMap;
    
    @PostConstruct
    public void postConstruct() {
        if (hazelcast.isEnabled()) {
            payaraMicroMap = hazelcast.getInstance().getMap("PayaraMicro");
        }
        
    }

    /**
     *
     * @param event
     */
    @Override
    public void event(Event event) {
        if (event.is(EventTypes.SERVER_READY)) {
            
        } else if (event.is(Deployment.APPLICATION_LOADED)) {
            if (event.hook() != null && event.hook() instanceof ApplicationInfo) {
                    ApplicationInfo applicationInfo = (ApplicationInfo) event.hook();
            }            
        } else if (event.is(Deployment.UNDEPLOYMENT_SUCCESS)) {
             if (event.hook() != null && event.hook() instanceof DeploymentContextImpl) {
                    DeploymentContextImpl deploymentContext = (DeploymentContextImpl) event.hook();
            }
        }
    }
    
}
