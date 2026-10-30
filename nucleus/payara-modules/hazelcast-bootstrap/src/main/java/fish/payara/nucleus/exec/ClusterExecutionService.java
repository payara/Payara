/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2016-2023] Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
package fish.payara.nucleus.exec;

import com.hazelcast.cluster.Member;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.scheduledexecutor.IScheduledExecutorService;
import com.hazelcast.scheduledexecutor.IScheduledFuture;
import fish.payara.nucleus.events.HazelcastEvents;
import fish.payara.nucleus.hazelcast.HazelcastCore;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.api.event.EventListener;
import org.glassfish.api.event.Events;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

/**
 * Execution Service for running Callables across the Payara Cluster also enables
 * Scheduling of Callables to run on the cluster.
 * @author steve
 */
@Service(name = "payara-cluster-executor")
@RunLevel(StartupRunLevel.VAL)
public class ClusterExecutionService implements EventListener {
    
    private static final Logger logger = Logger.getLogger(ClusterExecutionService.class.getCanonicalName());
   
    @Inject
    private HazelcastCore hzCore;
    
    @Inject
    private Events events;
    
    @PostConstruct
    public void postConstruct() {
        events.register(this);
    }
    
    /**
     * Runs a Callable object 
     * @param <T> Type of the Callable Result
     * @param callable The Callable object
     * @return Future for the result
     */
    public <T extends Serializable> Future<T> runCallable(Callable<T> callable) {
        Future<T> result = null;
        if (hzCore.isEnabled()) {
            result = hzCore.getInstance().getExecutorService(HazelcastCore.CLUSTER_EXECUTOR_SERVICE_NAME).submit(callable);
        }
        return result;
    }
    
    /**
     * Runs a Callable object on the specified Member
     * @param <T> Type of the Callable Result
     * @param memberUUID The member to run the Callable on
     * @param callable The Callable object
     * @return Future for the result
     */
    public <T extends Serializable> Future<T> runCallable(UUID memberUUID, Callable<T> callable) {
        Future<T> result = null;
        if (hzCore.isEnabled()) {
            Member toSubmitTo = selectMember(memberUUID);
            result = hzCore.getInstance().getExecutorService(HazelcastCore.CLUSTER_EXECUTOR_SERVICE_NAME).submitToMember(callable, toSubmitTo);
        }
        return result;
    }
    
    /**
     * Runs a Callable object on a set of members
     * @param <T> The type of the Callable result
     * @param memberUUIDS A set of members to run the Callables on
     * @param callable The Callable to run
     * @return A map of Futures keyed by Member UUID
     */
    public <T extends Serializable> Map<UUID, Future<T>> runCallable(Collection<UUID> memberUUIDS, Callable<T> callable) {
        HashMap<UUID, Future<T>> result = new HashMap<>(2);
        if (hzCore.isEnabled()) {

            Set<Member> membersToSubmit = selectMembers(memberUUIDS);
            Map<Member, Future<T>> results = hzCore.getInstance().getExecutorService(HazelcastCore.CLUSTER_EXECUTOR_SERVICE_NAME).submitToMembers(callable, membersToSubmit);
            for (Entry<Member, Future<T>> entry : results.entrySet()) {
                Member member = entry.getKey();
                result.put(member.getUuid(), entry.getValue());
            }
        }
        return result;
    }
    
    /**
     * Runs a Callable on All members of the cluster
     * @param <T> The result of the Callable
     * @param callable The Callable to run
     * @return A Map of Future results keyed by member UUID
     */
    public <T extends Serializable> Map<UUID, Future<T>> runCallableAllMembers(Callable<T> callable) {
        HashMap<UUID, Future<T>> result = new HashMap<>(2);
        if (hzCore.isEnabled()) {
            // work out which members correspond to cluster UUIDS.
            HazelcastInstance instance = hzCore.getInstance();
            Map<Member, Future<T>> results = hzCore.getInstance().getExecutorService(HazelcastCore.CLUSTER_EXECUTOR_SERVICE_NAME).submitToAllMembers(callable);
            for (Entry<Member, Future<T>> entry : results.entrySet()) {
                Member member = entry.getKey();
                result.put(member.getUuid(), entry.getValue());
            }
        }
        return result;
    }
    
    /**
     * Schedules a Callable object to be run in the future
     * @param <V> The type of the result
     * @param callable The Callable Object
     * @param delay The delay before running the task
     * @param unit The time unit of the delay
     * @return A Future containing the result
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public <V extends Serializable> ScheduledTaskFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
        ScheduledTaskFuture result = null;
        if (hzCore.isEnabled()) {
            IScheduledExecutorService scheduledExecutorService = hzCore.getInstance().getScheduledExecutorService(HazelcastCore.SCHEDULED_CLUSTER_EXECUTOR_SERVICE_NAME);
            IScheduledFuture<V> schedule = scheduledExecutorService.schedule(callable, delay, unit);
            result = new ScheduledTaskFuture(schedule);
        }
        return result;
    }
    
    /**
     * Schedules a Callable object to be run in the future on the specified Member
     * @param <V> The type of the result
     * @param memberUUID The member to schedule the task on
     * @param callable The Callable Object
     * @param delay The delay before running the task
     * @param unit The time unit of the delay
     * @return A Future containing the result
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public <V extends Serializable> ScheduledTaskFuture<V> schedule(UUID memberUUID, Callable<V> callable, long delay, TimeUnit unit) {
        ScheduledTaskFuture result = null;

        if (hzCore.isEnabled()) {
            Member toSubmitTo = selectMember(memberUUID);
            IScheduledExecutorService scheduledExecutorService = hzCore.getInstance().getScheduledExecutorService(HazelcastCore.SCHEDULED_CLUSTER_EXECUTOR_SERVICE_NAME);
            IScheduledFuture<V> schedule = scheduledExecutorService.scheduleOnMember(callable, toSubmitTo, delay, unit);
            result = new ScheduledTaskFuture(schedule);
        }
        return result;
    }
    
    /**
     * Schedules a Callable object to be run in the future on the specified Member
     * @param <V> The type of the result
     * @param memberUUIDs The members to schedule the task on
     * @param callable The Callable Object
     * @param delay The delay before running the task
     * @param unit The time unit of the delay
     * @return A Future containing the result
     */
    public <V extends Serializable> Map<UUID, ScheduledTaskFuture<V>> schedule(Collection<UUID> memberUUIDs, Callable<V> callable, long delay, TimeUnit unit) {
        HashMap<UUID, ScheduledTaskFuture<V>> result = new HashMap<>(2);

        if (hzCore.isEnabled()) {
            Collection<Member> toSubmitTo = selectMembers(memberUUIDs);
            IScheduledExecutorService scheduledExecutorService = hzCore.getInstance().getScheduledExecutorService(HazelcastCore.SCHEDULED_CLUSTER_EXECUTOR_SERVICE_NAME);
            Map<Member, IScheduledFuture<V>> schedule = scheduledExecutorService.scheduleOnMembers(callable, toSubmitTo, delay, unit);
            for (Entry<Member, IScheduledFuture<V>> entry : schedule.entrySet()) {
                Member member = entry.getKey();
                result.put(member.getUuid(), new ScheduledTaskFuture<>(entry.getValue()));
            }
        }
        return result;
    }

        /**
     * Schedules a Callable object to be run in the future
     * @param runnable The Runnable Object
     * @param delay The delay before running the task
     * @param period The period for the fixed rate
     * @param unit The time unit of the delay
     * @return A Future containing the result
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public ScheduledTaskFuture<? extends Serializable> scheduleAtFixedRate(Runnable runnable, long delay, long period, TimeUnit unit) {
        ScheduledTaskFuture result = null;
        if (hzCore.isEnabled()) {
            IScheduledExecutorService scheduledExecutorService = hzCore.getInstance().getScheduledExecutorService(HazelcastCore.SCHEDULED_CLUSTER_EXECUTOR_SERVICE_NAME);
            IScheduledFuture<?> schedule = scheduledExecutorService.scheduleAtFixedRate(runnable, delay, period, unit);
            result = new ScheduledTaskFuture(schedule);
        }
        return result;
    }
    
    /**
     * Schedules a Callable object to be run in the future on the specified Member
     * @param memberUUID The member to schedule the task on
     * @param runnable The Runnable Object
     * @param delay The delay before running the task
     * @param period The period for the fixed rate
     * @param unit The time unit of the delay
     * @return A Future containing the result
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public ScheduledTaskFuture<?> scheduleAtFixedRate(UUID memberUUID, Runnable runnable, long delay, long period, TimeUnit unit) {
        ScheduledTaskFuture result = null;

        if (hzCore.isEnabled()) {
            Member toSubmitTo = selectMember(memberUUID);
            IScheduledExecutorService scheduledExecutorService = hzCore.getInstance().getScheduledExecutorService(HazelcastCore.SCHEDULED_CLUSTER_EXECUTOR_SERVICE_NAME);
            IScheduledFuture<?> schedule = scheduledExecutorService.scheduleOnMemberAtFixedRate(runnable, toSubmitTo, delay, period, unit);
            result = new ScheduledTaskFuture(schedule);
        }
        return result;
    }
    
    /**
     * Schedules a Callable object to be run in the future on the specified Member
     * @param memberUUIDs The members to schedule the task on
     * @param runnable The Runnable Object
     * @param delay The delay before running the task
     * @param period The period of the fixed rate
     * @param unit The time unit of the delay
     * @return A Future containing the result
     */
    public Map<UUID, ScheduledTaskFuture<?>> scheduleAtFixedRate(Collection<UUID> memberUUIDs, Runnable runnable, long delay, long period, TimeUnit unit) {
        HashMap<UUID, ScheduledTaskFuture<?>> result = new HashMap<>(2);

        if (hzCore.isEnabled()) {
            Collection<Member> toSubmitTo = selectMembers(memberUUIDs);
            IScheduledExecutorService scheduledExecutorService = hzCore.getInstance().getScheduledExecutorService(HazelcastCore.SCHEDULED_CLUSTER_EXECUTOR_SERVICE_NAME);
            Map<Member, IScheduledFuture<Object>> schedule = scheduledExecutorService.<Object>scheduleOnMembersAtFixedRate(runnable, toSubmitTo, delay, period, unit);
            for (Entry<Member, IScheduledFuture<Object>> entry : schedule.entrySet()) {
                Member member = entry.getKey();
                result.put(member.getUuid(), new ScheduledTaskFuture<>(entry.getValue()));
            }
        }
        return result;
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void event(Event event) {
        if (event.is(HazelcastEvents.HAZELCAST_BOOTSTRAP_COMPLETE)) {
            if (hzCore.isEnabled()) {
                logger.config("Payara Clustered Executor Service Enabled");
            }
        }
    }
    
    private Member selectMember(UUID memberUUID) {
        Set<Member> members = hzCore.getInstance().getCluster().getMembers();
        Member toSubmitTo = null;
        for (Member member : members) {
            if (member.getUuid().equals(memberUUID)) {
                toSubmitTo = member;
                break;
            }
        }
        return toSubmitTo;
    }
    
    private Set<Member> selectMembers(Collection<UUID> memberUUIDS) {
        // work out which members correspond to cluster UUIDS.
        HazelcastInstance instance = hzCore.getInstance();
        Set<Member> members = instance.getCluster().getMembers();
        Set<Member> membersToSubmit = new HashSet<>(members.size());
        for (Member member : members) {
            if (memberUUIDS.contains(member.getUuid())) {
                membersToSubmit.add(member);
            }
        }
        return membersToSubmit;
    }
}
