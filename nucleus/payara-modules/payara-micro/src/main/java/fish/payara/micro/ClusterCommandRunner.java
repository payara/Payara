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
package fish.payara.micro;

import com.hazelcast.core.Member;
import fish.payara.micro.services.PayaraMicroInstance;
import fish.payara.micro.services.data.InstanceDescriptor;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import org.glassfish.embeddable.CommandResult;
import org.glassfish.embeddable.CommandRunner;

/**
 *
 * @author steve
 */
public class ClusterCommandRunner implements CommandRunner {
    
    private boolean terse = false;
    private final CommandRunner localRunner;
    private final PayaraMicroInstance instanceService;

    ClusterCommandRunner(CommandRunner commandRunner, PayaraMicroInstance instanceService) {
        this.localRunner = commandRunner;
        this.instanceService = instanceService;
    }
    
    /**
     *  Submits a Callable to the specified Payara Micro instances and returns the results.
     * @param <T> The type of the Callable Result
     * @param members The set of Payara Micro instances to run the callable on
     * @param callable The Callable to execute
     * @return A Map of result futures for the callables
     */
    public <T extends Serializable> Map<InstanceDescriptor, Future<T>> run (Collection<InstanceDescriptor> members, Callable<T> callable) {
        Map<Member, Future<T>> callResults = instanceService.getClusterCommandRunner(members).run(callable);
        Map<InstanceDescriptor,Future<T>> result = new HashMap<>(callResults.size());
        for (Member member : callResults.keySet()) {
            for (InstanceDescriptor id : members) {
                if (id.getMemberUUID().equals(member.getUuid())) {
                        result.put(id, callResults.get(member));
                }
            }
        }
        return result;
        
    }
    
    
    
    /**
     * Runs an asadmin command on the specified members of a Payara Micro Cluster
     * @param members A list of the UUIDs of the members to run the command on
     * @param command The asadmin command to run
     * @param args The asadmin command arguments
     * @return A Map of the member InstanceDescriptor to the Command Result
     */
    public Map<InstanceDescriptor, Future<CommandResult>> run (Collection<InstanceDescriptor> members, String command, String... args ) {
        Map<Member, Future<CommandResult>> run = instanceService.getClusterCommandRunner(members).run(command, args);
        
        Map<InstanceDescriptor,Future<CommandResult>> result = new HashMap<>(run.size());
        for (Member member : run.keySet()) {
            for (InstanceDescriptor id : members) {
                if (id.getMemberUUID().equals(member.getUuid())) {
                        result.put(id, run.get(member));
                }
            }
        }
        return result;
    }

    /**
     * Runs an asadmin command on the local Payara Micro
     * @param command The asadmin command to run
     * @param args The arguments for the command
     * @return The result
     */
    @Override
    public CommandResult run(String command, String... args) {
        return localRunner.run(command, args);
    }

    @Override
    public void setTerse(boolean terse) {
        this.terse = terse;
        localRunner.setTerse(terse);
    }
    
}
