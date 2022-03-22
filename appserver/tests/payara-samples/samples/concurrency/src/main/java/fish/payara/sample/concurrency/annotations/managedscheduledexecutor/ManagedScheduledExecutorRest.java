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
package fish.payara.sample.concurrency.annotations.managedscheduledexecutor;

import jakarta.annotation.Resource;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.enterprise.concurrent.CronTrigger;
import jakarta.enterprise.concurrent.ManagedScheduledExecutorService;
import jakarta.enterprise.concurrent.ManagedScheduledExecutorDefinition;
import jakarta.enterprise.concurrent.Trigger;
import java.time.ZoneId;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;

@ManagedScheduledExecutorDefinition(name = "java:app/managedexecutor/CustomManagedScheduledExecutorA",
        hungTaskThreshold = 120000, maxAsync = 5)
@ManagedScheduledExecutorDefinition(name = "java:app/managedexecutor/CustomManagedScheduledExecutorB",
        hungTaskThreshold = 120000, maxAsync = 5)
@Path("annotation")
public class ManagedScheduledExecutorRest {

    private static final Logger logger = Logger.getLogger(ManagedScheduledExecutorRest.class.getName());

    @Resource(lookup = "java:app/managedexecutor/CustomManagedScheduledExecutorA")
    ManagedScheduledExecutorService customManagedScheduleExecutorA;

    @Resource(lookup = "java:app/managedexecutor/CustomManagedScheduledExecutorB")
    ManagedScheduledExecutorService customManagedScheduleExecutorB;

    @Resource(lookup = "java:app/managedexecutor/CustomManagedScheduledExecutorC")
    ManagedScheduledExecutorService customManagedScheduleExecutorC;


    @GET
    @Path("scheduledexecutor")
    @Produces(MediaType.TEXT_PLAIN)
    public String processScheduledExecutor() throws InterruptedException {
        logger.log(Level.INFO, String.format("Processing schedule executor: %s", customManagedScheduleExecutorA));
        AtomicInteger numberExecution = new AtomicInteger();
        ZoneId mexico = ZoneId.of("America/Mexico_City");
        Trigger trigger = new CronTrigger("* * * * * *", mexico);
        ScheduledFuture feature = customManagedScheduleExecutorA.schedule(() -> {
            numberExecution.getAndIncrement();
            System.out.println("Cron Trigger running");
        }, trigger);
        Thread.sleep(1500);
        feature.cancel(true);
        return "CronTrigger Submitted:"+numberExecution.get();
    }

    @GET
    @Path("multiplescheduledexecutor")
    @Produces(MediaType.TEXT_PLAIN)
    public String processMultipleScheduledExecutor() throws InterruptedException {
        logger.log(Level.INFO, String.format("Processing schedule executor A: %s", customManagedScheduleExecutorA));
        logger.log(Level.INFO, String.format("Processing schedule executor B: %s", customManagedScheduleExecutorB));
        AtomicInteger numberExecutionA = new AtomicInteger();
        AtomicInteger numberExecutionB = new AtomicInteger();

        ZoneId mexico = ZoneId.of("America/Mexico_City");
        Trigger trigger = new CronTrigger("* * * * * *", mexico);

        ScheduledFuture feature1 = customManagedScheduleExecutorA.schedule(() -> {
            numberExecutionA.getAndIncrement();
            System.out.println("Cron Trigger running");
        }, trigger);

        ScheduledFuture feature2 = customManagedScheduleExecutorB.schedule(() -> {
            numberExecutionB.getAndIncrement();
            System.out.println("Cron Trigger running");
        }, trigger);


        Thread.sleep(1500);
        feature1.cancel(true);
        feature2.cancel(true);
        return "CronTrigger Submitted:"+(numberExecutionA.get()+numberExecutionB.get());
    }

    @GET
    @Path("xmlmanagedexecutor")
    @Produces(MediaType.TEXT_PLAIN)
    public String processScheduledExecutorFromXML() throws InterruptedException {
        logger.log(Level.INFO, String.format("Processing schedule executor: %s", customManagedScheduleExecutorC));
        AtomicInteger numberExecution = new AtomicInteger();
        ZoneId mexico = ZoneId.of("America/Mexico_City");
        Trigger trigger = new CronTrigger("* * * * * *", mexico);
        ScheduledFuture feature = customManagedScheduleExecutorC.schedule(() -> {
            numberExecution.getAndIncrement();
            System.out.println("Cron Trigger running");
        }, trigger);
        Thread.sleep(1500);
        feature.cancel(true);
        return "CronTrigger Submitted:"+numberExecution.get();
    }

}