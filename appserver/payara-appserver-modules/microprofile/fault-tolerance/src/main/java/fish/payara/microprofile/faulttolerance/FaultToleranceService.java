/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *    Copyright (c) [2017] Payara Foundation and/or its affiliates. All rights reserved.
 * 
 *     The contents of this file are subject to the terms of either the GNU
 *     General Public License Version 2 only ("GPL") or the Common Development
 *     and Distribution License("CDDL") (collectively, the "License").  You
 *     may not use this file except in compliance with the License.  You can
 *     obtain a copy of the License at
 *     https://github.com/payara/Payara/blob/master/LICENSE.txt
 *     See the License for the specific
 *     language governing permissions and limitations under the License.
 * 
 *     When distributing the software, include this License Header Notice in each
 *     file and include the License file at glassfish/legal/LICENSE.txt.
 * 
 *     GPL Classpath Exception:
 *     The Payara Foundation designates this particular file as subject to the "Classpath"
 *     exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *     file that accompanied this code.
 * 
 *     Modifications:
 *     If applicable, add the following below the License Header, with the fields
 *     enclosed by brackets [] replaced by your own identifying information:
 *     "Portions Copyright [year] [name of copyright owner]"
 * 
 *     Contributor(s):
 *     If you wish your version of this file to be governed by only the CDDL or
 *     only the GPL Version 2, indicate your decision by adding "[Contributor]
 *     elects to include this software in this distribution under the [CDDL or GPL
 *     Version 2] license."  If you don't indicate a single choice of license, a
 *     recipient has the option to distribute your version of this file under
 *     either the CDDL, the GPL Version 2 or to extend the choice of license to
 *     its licensees as provided above.  However, if you add GPL Version 2 code
 *     and therefore, elected the GPL Version 2 license, then the option applies
 *     only if the new code is made subject to such option by the copyright
 *     holder.
 */
package fish.payara.microprofile.faulttolerance;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import javax.annotation.PostConstruct;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.inject.Inject;
import javax.inject.Named;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Optional;
import org.jvnet.hk2.annotations.Service;

/**
 *
 * @author Andrew Pielage
 */
@Service(name = "microprofile-fault-tolerance-service")
@RunLevel(StartupRunLevel.VAL)
public class FaultToleranceService {
    
    @Inject
    @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    @Optional
    FaultToleranceServiceConfiguration faultToleranceServiceConfiguration;
    
    @Inject
    ServiceLocator habitat;
    
    private final Map<Bulkhead, Semaphore> bulkheadExecutionSemaphores;
    private final Map<Bulkhead, Semaphore> bulkheadExecutionQueueSemaphores;
    
    public FaultToleranceService() {
        bulkheadExecutionSemaphores = new ConcurrentHashMap();
        bulkheadExecutionQueueSemaphores = new ConcurrentHashMap();
    }
    
    @PostConstruct
    public void postConstruct() {
        faultToleranceServiceConfiguration = habitat.getService(FaultToleranceServiceConfiguration.class);
    }
    
    public ManagedExecutorService getManagedExecutorService() throws NamingException {
        String managedExecutorServiceName = faultToleranceServiceConfiguration.getManagedExecutorService();
        InitialContext ctx = new InitialContext();
        
        ManagedExecutorService managedExecutorService = null;
        
        if (managedExecutorServiceName == null || managedExecutorServiceName.isEmpty()) {
            managedExecutorService = (ManagedExecutorService) ctx.lookup("java:comp/DefaultManagedExecutorService");
        } else {
            try {
                managedExecutorService = (ManagedExecutorService) ctx.lookup(managedExecutorServiceName);
            } catch (NamingException ex) {
                managedExecutorService = (ManagedExecutorService) ctx.lookup("java:comp/DefaultManagedExecutorService");
            } 
        }
        
        return managedExecutorService;
    }
    
    public Semaphore getBulkheadExecutionSemaphore(Bulkhead bulkhead) {
        Semaphore bulkheadExecutionSemaphore = bulkheadExecutionSemaphores.get(bulkhead);
        
        if (bulkheadExecutionSemaphore == null) {
            bulkheadExecutionSemaphore = createBulkheadExecutionSemaphore(bulkhead);
        }
        
        return bulkheadExecutionSemaphore;
    }
    
    private Semaphore createBulkheadExecutionSemaphore(Bulkhead bulkhead) {
        Semaphore bulkheadExecutionSemaphore = new Semaphore(bulkhead.value());
        bulkheadExecutionSemaphores.put(bulkhead, bulkheadExecutionSemaphore);
        return bulkheadExecutionSemaphore;
    }
    
    public Semaphore getBulkheadExecutionQueueSemaphore(Bulkhead bulkhead) {
        Semaphore bulkheadExecutionQueueSemaphore = bulkheadExecutionQueueSemaphores.get(bulkhead);
        
        if (bulkheadExecutionQueueSemaphore == null) {
            bulkheadExecutionQueueSemaphore = createBulkheadExecutionQueueSemaphore(bulkhead);
        }
        
        return bulkheadExecutionQueueSemaphore;
    }
    
    private Semaphore createBulkheadExecutionQueueSemaphore(Bulkhead bulkhead) {
        Semaphore bulkheadExecutionQueueSemaphore = new Semaphore(bulkhead.waitingTaskQueue());
        bulkheadExecutionQueueSemaphores.put(bulkhead, bulkheadExecutionQueueSemaphore);
        return bulkheadExecutionQueueSemaphore;
    }
    
//    public Semaphore getCircuitBreaker(CircuitBreaker circuitBreaker) {
//        
//    }
//    
//    private Semaphore createCircuitBreaker(CircuitBreaker circuitBreaker) {
//        
//    }
}
