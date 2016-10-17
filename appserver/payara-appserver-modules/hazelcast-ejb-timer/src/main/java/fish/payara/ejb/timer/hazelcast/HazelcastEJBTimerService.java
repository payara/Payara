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
package fish.payara.ejb.timer.hazelcast;

import com.sun.ejb.PersistentTimerService;
import fish.payara.nucleus.hazelcast.HazelcastCore;
import javax.inject.Inject;
import org.jvnet.hk2.annotations.Service;

/**
 *
 * @author steve
 */
@Service
public class HazelcastEJBTimerService implements PersistentTimerService {
    
    
    @Inject
    HazelcastCore hazelcast;
    
    

    @Override
    public void initPersistentTimerService(String target) {
        HazelcastTimerStore.init(hazelcast);
    }
    
}
