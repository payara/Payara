/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2022] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.sample.concurrency.annotations.managedexecutor;

import jakarta.annotation.Resource;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutionException;

import jakarta.enterprise.concurrent.ManagedExecutorDefinition;
import jakarta.enterprise.concurrent.ManagedExecutorService;

@ManagedExecutorDefinition(name = "java:app/managedexecutor/CustomManagedExecutor",
        hungTaskThreshold = 120000, maxAsync = 5)
@ManagedExecutorDefinition(name = "java:comp/managedexecutor/CustomManagedExecutorA",
        hungTaskThreshold = 120000, maxAsync = 5)
@ManagedExecutorDefinition(name = "java:global/managedexecutor/CustomManagedExecutorB",
        hungTaskThreshold = 120000, maxAsync = 5)
@ManagedExecutorDefinition(name = "java:module/managedexecutor/CustomManagedExecutorC",
        hungTaskThreshold = 120000, maxAsync = 5)
@Path("annotation")
public class ManagedExecutorDefinitionRest {

    private static final Logger logger = Logger.getLogger(ManagedExecutorDefinitionRest.class.getName());

    @Resource(lookup = "java:app/managedexecutor/CustomManagedExecutor")
    ManagedExecutorService managedExecutorService;

    @Resource(lookup = "java:comp/managedexecutor/CustomManagedExecutorA")
    ManagedExecutorService managedExecutorServiceA;

    @Resource(lookup = "java:global/managedexecutor/CustomManagedExecutorB")
    ManagedExecutorService managedExecutorServiceB;

    @Resource(lookup = "java:module/managedexecutor/CustomManagedExecutorC")
    ManagedExecutorService managedExecutorServiceC;

    @Resource(lookup = "java:app/managedexecutor/XMLManagedExector")
    ManagedExecutorService xmlManagedExecutorService;

    @GET
    @Path("managedexecutor")
    @Produces(MediaType.TEXT_PLAIN)
    public String processManagedExecutor() throws InterruptedException, ExecutionException {
        logger.log(Level.INFO, String.format("ManagedExecutor:%s", managedExecutorService));
        AtomicInteger numberExecution = new AtomicInteger(0);
        Future future = managedExecutorService.submit(() -> {
            numberExecution.incrementAndGet();
            System.out.println("Job running");
        });
        future.get();
        return "Executor submitted:" + numberExecution.get();
    }

    @GET
    @Path("multiplemanagedexecutor")
    @Produces(MediaType.TEXT_PLAIN)
    public String processMultipleManagedExecutor() throws InterruptedException, ExecutionException {
        logger.log(Level.INFO, String.format("ManagedExecutorA:%s", managedExecutorServiceA));
        logger.log(Level.INFO, String.format("ManagedExecutorB:%s", managedExecutorServiceB));
        logger.log(Level.INFO, String.format("ManagedExecutorC:%s", managedExecutorServiceC));

        AtomicInteger numberExecutionA = new AtomicInteger(0);
        AtomicInteger numberExecutionB = new AtomicInteger(0);
        AtomicInteger numberExecutionC = new AtomicInteger(0);
        Future future1 = managedExecutorServiceA.submit(() -> {
            numberExecutionA.incrementAndGet();
            System.out.println("Job running from A");
        });
        Future future2 = managedExecutorServiceB.submit(() -> {
            numberExecutionB.incrementAndGet();
            System.out.println("Job running from B");
        });
        Future future3 = managedExecutorServiceC.submit(() -> {
            numberExecutionC.incrementAndGet();
            System.out.println("Job running from C");
        });

        future1.get();
        future2.get();
        future3.get();
        return "Executor submitted:" + (numberExecutionA.get() + numberExecutionB.get() + numberExecutionC.get());
    }

    @GET
    @Path("xmlmanagedexecutor")
    @Produces(MediaType.TEXT_PLAIN)
    public String xmlManagedExecutor() throws InterruptedException, ExecutionException {
        logger.log(Level.INFO, String.format("XML ManagedExecutor:%s", xmlManagedExecutorService));
        AtomicInteger numberExecution = new AtomicInteger(0);
        Future future = xmlManagedExecutorService.submit(() -> {
            numberExecution.incrementAndGet();
            System.out.println("Job running");
        });
        future.get();
        return "Executor submitted:" + numberExecution.get();
    }
}