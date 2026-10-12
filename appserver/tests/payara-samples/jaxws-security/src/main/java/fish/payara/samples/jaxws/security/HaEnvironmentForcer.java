/*
 *    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) 2026 Payara Foundation and/or its affiliates. All rights reserved.
 *
 *    The contents of this file are subject to the terms of either the GNU
 *    General Public License Version 2 only ("GPL") or the Common Development
 *    and Distribution License("CDDL") (collectively, the "License").  You
 *    may not use this file except in compliance with the License.  You can
 *    obtain a copy of the License at
 *    https://github.com/payara/Payara/blob/main/LICENSE.txt
 *    See the License for the specific
 *    language governing permissions and limitations under the License.
 *
 *    When distributing the software, include this License Header Notice in each
 *    file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 *    GPL Classpath Exception:
 *    The Payara Foundation designates this particular file as subject to the "Classpath"
 *    exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *    file that accompanied this code.
 *
 *    Modifications:
 *    If applicable, add the following below the License Header, with the fields
 *    enclosed by brackets [] replaced by your own identifying information:
 *    "Portions Copyright [year] [name of copyright owner]"
 *
 *    Contributor(s):
 *    If you wish your version of this file to be governed by only the CDDL or
 *    only the GPL Version 2, indicate your decision by adding "[Contributor]
 *    elects to include this software in this distribution under the [CDDL or GPL
 *    Version 2] license."  If you don't indicate a single choice of license, a
 *    recipient has the option to distribute your version of this file under
 *    either the CDDL, the GPL Version 2 or to extend the choice of license to
 *    its licensees as provided above.  However, if you add GPL Version 2 code
 *    and therefore, elected the GPL Version 2 license, then the option applies
 *    only if the new code is made subject to such option by the copyright
 *    holder.
 */
package fish.payara.samples.jaxws.security;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Forces Metro's HighAvailabilityProvider into "HA configured" mode for the
 * lifetime of this deployment only, then restores the original state on
 * undeployment.
 *
 * HighAvailabilityProvider is a JVM-wide enum singleton whose entire HA state
 * is held in one volatile field (haEnvironment). We save that field's value
 * before calling initHaEnvironment() and write it back in contextDestroyed(),
 * so no state leaks to other deployments or tests running on the same server.
 *
 * Without this listener, isHaEnvironmentConfigured() returns false on a
 * single-node server, Metro selects NonHANonceManager (local cache), and the
 * BackingStoreFactoryRegistry "replicated" lookup is never reached. With it,
 * HANonceManager is selected and the lookup is exercised, validating the fix
 * in ReplicatedBackingStoreFactoryProxy.
 */
@WebListener
public class HaEnvironmentForcer implements ServletContextListener {

    private Field haEnvironmentField;
    private Object instance;
    private Object savedHaEnvironment;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        try {
            Class<?> providerClass = Class.forName(
                    "com.sun.xml.ws.api.ha.HighAvailabilityProvider",
                    true,
                    Thread.currentThread().getContextClassLoader());

            instance = providerClass.getField("INSTANCE").get(null);

            haEnvironmentField = providerClass.getDeclaredField("haEnvironment");
            haEnvironmentField.setAccessible(true);
            savedHaEnvironment = haEnvironmentField.get(instance);

            Method initMethod = providerClass.getMethod("initHaEnvironment", String.class, String.class);
            initMethod.invoke(instance, "test-cluster", "instance1");

            Method checkMethod = providerClass.getMethod("isHaEnvironmentConfigured");
            sce.getServletContext().log(
                    "[HaEnvironmentForcer] isHaEnvironmentConfigured = " + checkMethod.invoke(instance));
        } catch (Exception e) {
            throw new RuntimeException("[HaEnvironmentForcer] Failed to force HA environment", e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        try {
            if (haEnvironmentField != null) {
                haEnvironmentField.set(instance, savedHaEnvironment);
                sce.getServletContext().log("[HaEnvironmentForcer] HA environment state restored");
            }
        } catch (Exception e) {
            sce.getServletContext().log(
                    "[HaEnvironmentForcer] Failed to restore HA environment: " + e.getMessage());
        }
    }
}
