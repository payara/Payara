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
package fish.payara.micro.services.command;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IExecutorService;
import com.hazelcast.core.Member;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import org.glassfish.embeddable.CommandResult;

/**
 *
 * @author steve
 */
public class ClusterCommandRunner {
    
    private final HazelcastInstance instance;
    private final Set<Member> members;
    private boolean terse = false;
    
    public ClusterCommandRunner(HazelcastInstance instance, Set<Member> members) {
       this.instance = instance;
       this.members = members;
    }

    public Map<Member, Future<CommandResult>> run(String command, String... args) {
        IExecutorService executorService = instance.getExecutorService("PayaraMicro");
        AsAdminCallable callable = new AsAdminCallable(command, args);
        return executorService.submitToMembers(callable, members);
    }

    public void setTerse(boolean terse) {
        this.terse = terse;
    }


    
}
