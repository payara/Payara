/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2020-2023 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.microprofile.metrics;

import java.util.Set;

import java.util.concurrent.ConcurrentMap;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.jvnet.hk2.annotations.Contract;

import fish.payara.microprofile.metrics.exception.NoSuchRegistryException;

/**
 * The API to use {@link MetricRegistry}s within an multi-application environment.
 *
 * The main purpose of this API is to cleanly decouple the users from the service provider and to avoid API pollution
 * with methods that might be added to test or integrate the implementation but should not be called anywhere else.
 *
 * @author Jan Bernitt
 *
 * @since 5.2020.8
 */
@Contract
public interface MetricsService {

    /**
     * When metrics are disabled no {@link MetricsContext} exists, both {@link #getContext(boolean)} and
     * {@link #getContext(String)} will return null.
     *
     * @return true if the metrics feature is generally enabled, else false
     */
    boolean isEnabled();

    /**
     * Triggers update of internal state usually used after configuration changes that can have the effect that metrics
     * appear or disappear in a way that is detected by the registry.
     */
    void refresh();

    /**
     * @return a set of all {@link MetricsContext} that currently exist. When applications are undeployed their
     *         {@link MetricsContext} equally are removed which means a name from the set of names returned may indeed
     *         no longer exists when using {@link #getContext(boolean)}.
     */
    Set<String> getContextNames();

    /**
     * Access current application metric context. If this is called from a context that is not linked to an application
     * context the server context is returned.
     *
     * @param createIfNotExists true to create the requested context in case it did not exist, false to instead return
     *                          null in that case
     * @return the existing or created context or null if it did not exist and parameter was false or if the servicenot
     *         {@link #isEnabled()}
     */
    MetricsContext getContext(boolean createIfNotExists);

    /**
     * Access the context by name. Implicitly creates the {@link MetricsContext#SERVER_CONTEXT_NAME} if needed but no
     * other context that does not exist yet or no more.
     *
     * @param name the name of the {@link MetricsContext} to provide, this assumes it does exist
     * @return the existing context or null if it does not (or no longer) exist. When the service not
     *         {@link #isEnabled()} no context exists. Otherwise when asking for the
     *         {@link MetricsContext#SERVER_CONTEXT_NAME} the context is created should it not exist yet.
     */
    MetricsContext getContext(String name);

    /**
     * Each deployed application has its own {@link MetricsContext}.
     * In addition there is the server context which has no name as indicated by {@link #isServerContext()}.
     *
     * @author Jan Bernitt
     */
    interface MetricsContext {

        /**
         * The global or server context name cannot have any name but the empty string as all other names potentially
         * are application names used by deployed applications.
         */
        String SERVER_CONTEXT_NAME = "";

        /**
         * @return the name of this context which is either the name of the application this context is linked to or the
         *         empty string in case of the server context
         */
        String getName();

        default boolean isServerContext() {
            return SERVER_CONTEXT_NAME.equals(getName());
        }

        MetricRegistry getOrCreateRegistry(String registryName) throws NoSuchRegistryException;

        default MetricRegistry getBaseRegistry() {
            return getOrCreateRegistry(MetricRegistry.BASE_SCOPE);
        }

        default MetricRegistry getVendorRegistry() {
            return getOrCreateRegistry(MetricRegistry.VENDOR_SCOPE);
        }

        default MetricRegistry getApplicationRegistry() throws NoSuchRegistryException {
            return getOrCreateRegistry(MetricRegistry.APPLICATION_SCOPE);
        }

        ConcurrentMap<String, MetricRegistry> getRegistries();

    }

}
