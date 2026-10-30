/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2020-2021] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.samples.classloaderdata;

import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;
import java.util.stream.Collectors;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import org.glassfish.common.util.InstanceCounter;
import org.glassfish.javaee.full.deployment.EarClassLoader;
import org.glassfish.javaee.full.deployment.EarLibClassLoader;
import org.glassfish.web.loader.WebappClassLoader;
/**
 * A simple REST endpoint to return the count of
 * WebappClassLoader instances in the JVM
 *
 * @author - Cuba Stanley
 */
@Path("instance-count")
@ApplicationScoped
public class InstanceResource {
    private final AtomicInteger previousWebappInstanceCount = new AtomicInteger(-1);
    private final AtomicInteger previousEarInstanceCount = new AtomicInteger(-1);
    private final AtomicInteger previousEarLibInstanceCount = new AtomicInteger(-1);

    /**
     * Method handling HTTP GET request for Instance Count
     * Example: curl http://localhost:8080/ClassloaderDataAPI/api/instance-count/webapp/
     * Example: curl http://localhost:8080/ClassloaderDataAPI/api/instance-count/ear/
     * Example: curl http://localhost:8080/ClassloaderDataAPI/api/instance-count/earlib/500
     */
    @GET
    @Path("/webapp/{timeout:.*}")
    public String getWebappClassLoaderCount(@PathParam("timeout") Long timeout) {
        return instanceGetter(WebappClassLoader.class, previousWebappInstanceCount, timeout);
    }

    @GET
    @Path("/ear/{timeout:.*}")
    public String getEarClassLoaderCount(@PathParam("timeout") Long timeout) {
        return instanceGetter(EarClassLoader.class, previousEarInstanceCount, timeout);
    }

    @GET
    @Path("/earlib/{timeout:.*}")
    public String getEarLibClassLoaderCount(@PathParam("timeout") Long timeout) {
        return instanceGetter(EarLibClassLoader.class, previousEarLibInstanceCount, timeout);
    }

    private static String instanceGetter(Class<?> clazz, AtomicInteger previousValue, Long _timeout) {
        long timeout = Optional.ofNullable(_timeout).orElse(4L) / 2L;
        int previous = previousValue.updateAndGet(prev -> prev < 0 ? InstanceCounter.getInstanceCount(clazz, timeout) : prev);
        memoryPressure();
        System.gc();
        LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(timeout));
        previousValue.set(InstanceCounter.getInstanceCount(clazz, timeout));
        return String.format("Instances Before GC: %d\nInstances Remaining: %d\nInstances: \n%s\n",
                previous, previousValue.get(), InstanceCounter.getInstances(clazz, timeout).stream()
                        .map(WeakReference::get).filter(Objects::nonNull).map(Object::toString)
                        .collect(Collectors.joining("\n\n")));
    }

    private static void memoryPressure() {
        try {
            Object[] ignored = new Object[(int) Runtime.getRuntime().maxMemory()];
        } catch (OutOfMemoryError e) {
            // Ignore
        }
    }
}
