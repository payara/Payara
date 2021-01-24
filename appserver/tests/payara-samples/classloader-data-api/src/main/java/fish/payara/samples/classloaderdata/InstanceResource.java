/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2020] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.samples.classloaderdata;

import com.sun.enterprise.loader.ASURLClassLoader;
import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import org.glassfish.common.util.InstanceCounter;
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
    private Integer previousWebappInstanceCount;
    private Integer previousASURLInstanceCount;

    /**
     * Method handling HTTP GET request for Instance Count
     * Example: curl http://localhost:8080/ClassloaderDataAPI/api/instance-count/webapp/
     * Example: curl http://localhost:8080/ClassloaderDataAPI/api/instance-count/asurl/500
     */
    @GET
    @Path("/webapp/{timeout:.*}")
    public String getWebappClassLoaderCount(@PathParam("timeout") Long timeout) {
        return instanceGetter(WebappClassLoader.class, () -> previousWebappInstanceCount,
                newValue -> previousWebappInstanceCount = newValue, timeout);
    }

    @GET
    @Path("/asurl/{timeout:.*}")
    public String getPreviousInstanceCount(@PathParam("timeout") Long timeout) {
        return instanceGetter(ASURLClassLoader.class, () -> previousASURLInstanceCount,
                newValue -> previousASURLInstanceCount = newValue, timeout);
    }

    private static String instanceGetter(Class<?> clazz,
            Supplier<Integer> previousCountSupplier, Consumer<Integer> previousCountConsumer,
            Long _timeout) {
        long timeout = Optional.ofNullable(_timeout).orElse(1L);
        int previous = Optional.ofNullable(previousCountSupplier.get()).orElse(InstanceCounter.getInstanceCount(clazz, timeout));
        System.gc();
        previousCountConsumer.accept(InstanceCounter.getInstanceCount(clazz, timeout));
        return String.format("Instances Before GC: %d\nInstances Remaining: %d\nInstances: \n%s\n",
                previous, previousCountSupplier.get(), InstanceCounter.getInstances(clazz, timeout).stream()
                        .map(WeakReference::get).filter(Objects::nonNull).map(Object::toString)
                        .collect(Collectors.joining("\n\n")));
    }
}
