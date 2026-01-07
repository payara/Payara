/*
 *
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright (c) 2023 Payara Foundation and/or its affiliates. All rights reserved.
 *
 *  The contents of this file are subject to the terms of either the GNU
 *  General Public License Version 2 only ("GPL") or the Common Development
 *  and Distribution License("CDDL") (collectively, the "License").  You
 *  may not use this file except in compliance with the License.  You can
 *  obtain a copy of the License at
 *  https://github.com/payara/Payara/blob/main/LICENSE.txt
 *  See the License for the specific
 *  language governing permissions and limitations under the License.
 *
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 *  GPL Classpath Exception:
 *  The Payara Foundation designates this particular file as subject to the "Classpath"
 *  exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *  file that accompanied this code.
 *
 *  Modifications:
 *  If applicable, add the following below the License Header, with the fields
 *  enclosed by brackets [] replaced by your own identifying information:
 *  "Portions Copyright [year] [name of copyright owner]"
 *
 *  Contributor(s):
 *  If you wish your version of this file to be governed by only the CDDL or
 *  only the GPL Version 2, indicate your decision by adding "[Contributor]
 *  elects to include this software in this distribution under the [CDDL or GPL
 *  Version 2] license."  If you don't indicate a single choice of license, a
 *  recipient has the option to distribute your version of this file under
 *  either the CDDL, the GPL Version 2 or to extend the choice of license to
 *  its licensees as provided above.  However, if you add GPL Version 2 code
 *  and therefore, elected the GPL Version 2 license, then the option applies
 *  only if the new code is made subject to such option by the copyright
 *  holder.
 *
 */

package fish.payara.samples.otel.async;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import jakarta.annotation.Resource;
import jakarta.enterprise.concurrent.ManagedExecutorService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

@Path("/async")
@RequestScoped
public class AsyncResource {
    @Resource
    ManagedExecutorService mes;


    @Inject
    Tracer tracer;

    @Path("/compute")
    @GET
    public String computation(@QueryParam("propagation") @DefaultValue("none") String propagation) throws ExecutionException, InterruptedException {
        List<CompletableFuture<Integer>> tasks = new ArrayList<>();
        for(int i=0; i<10; i++) {
            switch (propagation.toLowerCase()) {
                case "traced":
                    // with manual span
                    tasks.add(mes.supplyAsync(this::tracedTask));
                    break;
                case "wrap":
                    // with manual span and context wrapping
                    tasks.add(mes.supplyAsync(Context.current().wrapSupplier(this::tracedTask)));
                    break;
                default:
                    // without explicit span
                    tasks.add(mes.supplyAsync(this::task));
            }
        }
        CompletableFuture.allOf(tasks.toArray(CompletableFuture[]::new));
        int result = 0;
        for (CompletableFuture<Integer> task : tasks) {
            result += task.get();
        }
        return String.valueOf(result);
    }

    private int task() {

        try {
            Thread.sleep(ThreadLocalRandom.current().nextInt(200));
        } catch (InterruptedException e) {
            // anyway...
        }
        return ThreadLocalRandom.current().nextInt();
    }

    private int tracedTask() {
        var span = tracer.spanBuilder("tracedTask").startSpan();

        try (var scope = span.makeCurrent()){
            var sleep = ThreadLocalRandom.current().nextInt(200);
            Thread.sleep(sleep);
            span.addEvent("sleep", Attributes.of(AttributeKey.longKey("sleeping"), (long)sleep));
        } catch (InterruptedException e) {
            // anyway...
        }
        span.setStatus(StatusCode.OK);
        span.end();
        return ThreadLocalRandom.current().nextInt();
    }
}
