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
package fish.payara.nucleus.exec;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.Member;
import fish.payara.nucleus.hazelcast.HazelcastCore;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

/**
 * A Hazelcast based Event Bus for Payara
 * @author steve
 */
@Service(name = "payara-cluster-executor")
@RunLevel(StartupRunLevel.VAL)
public class ClusterExecutionService {
    
    private static final Logger logger = Logger.getLogger(ClusterExecutionService.class.getCanonicalName());
    
    @Inject
    private HazelcastCore hzCore;
    
    @PostConstruct
    public void postConstruct() {
        if (hzCore.isEnabled()) {
            logger.info("Payara Clustered Exector Service Enabled");
        }
    }
    
    public <T extends Serializable> Future<T> runCallable(String memberUUID, Callable<T> callable) {
        Future<T> result = null;
        if (hzCore.isEnabled()) {
            HazelcastInstance instance = hzCore.getInstance();
            Set<Member> members = instance.getCluster().getMembers();
            Member toSubmitTo = null;
            for (Member member : members) {
                if (member.getUuid().equals(memberUUID)) {
                    toSubmitTo = member;
                    break;
                }
            }
            result = instance.getExecutorService("payara-cluster-execution").submitToMember(callable, toSubmitTo);
        }
        return result;
    }
    
    public <T extends Serializable> Map<String, Future<T>> runCallable(Collection<String> memberUUIDS, Callable<T> callable) {
        HashMap<String, Future<T>> result = new HashMap<String, Future<T>>(2);
        if (hzCore.isEnabled()) {
            // work out which members correspond to cluster UUIDS.
            HazelcastInstance instance = hzCore.getInstance();
            Set<Member> members = instance.getCluster().getMembers();
            Set<Member> membersToSubmit = new HashSet<Member>(members.size());
            for (Member member : members) {
                if (memberUUIDS.contains(member.getUuid())) {
                    membersToSubmit.add(member);
                }
            }
            Map<Member, Future<T>> results = hzCore.getInstance().getExecutorService("payara-cluster-execution").submitToMembers(callable, membersToSubmit);
            for (Member member : results.keySet()) {
                result.put(member.getUuid(), results.get(member));
            }
        }
        return result;
    }
    
    public <T extends Serializable> Map<String, Future<T>> runCallable(Callable<T> callable) {
        HashMap<String, Future<T>> result = new HashMap<String, Future<T>>(2);
        if (hzCore.isEnabled()) {
            // work out which members correspond to cluster UUIDS.
            HazelcastInstance instance = hzCore.getInstance();
            Map<Member, Future<T>> results = hzCore.getInstance().getExecutorService("payara-cluster-execution").submitToAllMembers(callable);
            for (Member member : results.keySet()) {
                result.put(member.getUuid(), results.get(member));
            }
        }
        return result;
    }
    
    
    
}
