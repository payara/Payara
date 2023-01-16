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
package fish.payara.sample.concurrency.annotations.managedthreadfactory;

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
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;
import java.util.stream.LongStream;
import java.util.Arrays;

import jakarta.enterprise.concurrent.ManagedThreadFactoryDefinition;
import jakarta.enterprise.concurrent.ManagedThreadFactory;


@ManagedThreadFactoryDefinition(name = "java:app/managedexecutor/CustomManagedThreadFactory", priority = 4)
@ManagedThreadFactoryDefinition(name = "java:app/managedexecutor/CustomManagedThreadFactoryA", priority = 3)
@ManagedThreadFactoryDefinition(name = "java:app/managedexecutor/CustomManagedThreadFactoryB", priority = 2)
@Path("annotation")
public class ManagedThreadFactoryRest {

    private static final Logger logger = Logger.getLogger(ManagedThreadFactoryRest.class.getName());

    @Resource(lookup = "java:app/managedexecutor/CustomManagedThreadFactory")
    ManagedThreadFactory managedThreadFactory;

    @Resource(lookup = "java:app/managedexecutor/CustomManagedThreadFactoryA")
    ManagedThreadFactory managedThreadFactoryA;

    @Resource(lookup = "java:app/managedexecutor/CustomManagedThreadFactoryB")
    ManagedThreadFactory managedThreadFactoryB;

    @Resource(lookup = "java:app/managedexecutor/CustomManagedThreadFactoryC")
    ManagedThreadFactory managedThreadFactoryC;


    @GET
    @Path("managedthreadfactory")
    @Produces(MediaType.TEXT_PLAIN)
    public String processManagedThreadFactory() throws InterruptedException, ExecutionException {
        logger.log(Level.INFO, String.format("ManagedThreadFactory:%s", managedThreadFactory));
        final long[] numbers = LongStream.rangeClosed(1, 500_000).toArray();
        ForkJoinPool pool = new ForkJoinPool(Runtime.getRuntime().availableProcessors(), managedThreadFactory, null, false);
        ForkJoinTask<Long> task = new ForkJoinSum(numbers);
        ForkJoinTask<Long> total = pool.submit(task);
        Long t = total.get();
        String message = String.format("Counting numbers total:%d", t);
        logger.log(Level.INFO, message);
        pool.shutdown();
        return message;
    }

    @GET
    @Path("multiplemanagedthreadfactory")
    @Produces(MediaType.TEXT_PLAIN)
    public String processMultipleManagedThreadFactory() throws InterruptedException, ExecutionException {
        logger.log(Level.INFO, String.format("ManagedThreadFactoryA:%s", managedThreadFactoryA));
        logger.log(Level.INFO, String.format("ManagedThreadFactoryB:%s", managedThreadFactoryB));
        final long[] numbers = LongStream.rangeClosed(1, 500_000).toArray();

        ForkJoinPool pool1 = new ForkJoinPool(Runtime.getRuntime().availableProcessors(), managedThreadFactoryA, null, false);
        ForkJoinTask<Long> task1 = new ForkJoinSum(numbers);
        ForkJoinTask<Long> total1 = pool1.submit(task1);

        ForkJoinPool pool2 = new ForkJoinPool(Runtime.getRuntime().availableProcessors(), managedThreadFactoryB, null, false);
        ForkJoinTask<Long> task2 = new ForkJoinSum(numbers);
        ForkJoinTask<Long> total2 = pool2.submit(task2);

        Long t1 = total1.get();
        Long t2 = total2.get();

        String message = String.format("Counting numbers total:%d", (t1 + t2));
        logger.log(Level.INFO, message);
        pool1.shutdown();
        pool2.shutdown();
        return message;
    }

    @GET
    @Path("webxmlconfig")
    @Produces(MediaType.TEXT_PLAIN)
    public String processXmlConfig() throws InterruptedException, ExecutionException {
        logger.log(Level.INFO, String.format("ManagedThreadFactory:%s", managedThreadFactoryC));
        final long[] numbers = LongStream.rangeClosed(1, 500_000).toArray();
        ForkJoinPool pool = new ForkJoinPool(Runtime.getRuntime().availableProcessors(), managedThreadFactoryC, null, false);
        ForkJoinTask<Long> totals = pool.submit(() -> Arrays.stream(numbers).parallel().reduce(0L, Long::sum));
        Long t = totals.join();
        String message = String.format("Counting numbers total:%d", t);
        logger.log(Level.INFO, message);
        pool.shutdown();
        return message;
    }



    static class ForkJoinSum extends RecursiveTask<Long> {

        public static final long THRESHOLD = 10_000;

        private final long[] numbers;
        private final int start;
        private final int end;

        public ForkJoinSum(long[] numbers) {
            this(numbers, 0, numbers.length);
        }

        private ForkJoinSum(long[] numbers, int start, int end) {
            this.numbers = numbers;
            this.start = start;
            this.end = end;
        }

        @Override
        protected Long compute() {
            System.out.println("thread name:" + Thread.currentThread().getName());
            int length = end - start;
            if (length <= THRESHOLD) {
                return computeSequentially();
            }
            ForkJoinSum leftTask = new ForkJoinSum(numbers, start, start + length / 2);
            leftTask.fork();
            ForkJoinSum rightTask = new ForkJoinSum(numbers, start + length / 2, end);
            Long rightResult = rightTask.compute();
            Long leftResult = leftTask.join();
            return leftResult + rightResult;
        }

        private long computeSequentially() {
            long sum = 0;
            for (int i = start; i < end; i++) {
                sum += numbers[i];
            }
            return sum;
        }
    }

}
