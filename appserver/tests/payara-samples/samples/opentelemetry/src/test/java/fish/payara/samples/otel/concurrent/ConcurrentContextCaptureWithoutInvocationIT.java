/*
 *
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright (c) 2026 Payara Foundation and/or its affiliates. All rights reserved.
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

package fish.payara.samples.otel.concurrent;

import jakarta.annotation.Resource;
import jakarta.ejb.Stateless;
import jakarta.enterprise.concurrent.ManagedScheduledExecutorService;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Regression test for FISH-14527 / GitHub #8385.
 *
 * <p>Since 7.2026.8, submitting a task to a container-managed executor from a thread
 * that carries no component invocation throws
 * {@code IllegalStateException: No application code is executing}. The failure originates
 * in the new {@code OtelContextProvider} which is called unconditionally during context
 * capture at task-submit time.
 *
 * <p>The EJB boundary is essential for deterministic reproduction:
 * {@code InvocationManagerImpl.computeChildThreadInvocation} copies the parent invocation
 * into a new thread only for {@code SERVLET_INVOCATION}; threads spawned inside an
 * {@code EJB_INVOCATION} receive no invocation frame, making
 * {@code InvocationManager.getCurrentInvocation()} return {@code null} on the new thread.
 *
 * <p>Both scenarios are tested: with OpenTelemetry disabled for the application (the
 * exact reported case) and with OpenTelemetry enabled (verifying that the context
 * provider degrades to a no-op instead of throwing).
 */
@RunWith(Arquillian.class)
public class ConcurrentContextCaptureWithoutInvocationIT {

    // -------------------------------------------------------------------------
    // EJB spawner — the reproduction boundary
    // -------------------------------------------------------------------------

    /**
     * Stateless EJB that deliberately creates an unmanaged thread from within its
     * EJB invocation and submits work to a container-managed scheduled executor from
     * that thread. This mirrors the real-world pattern described in the bug report.
     */
    @Stateless
    public static class SpawnerBean {

        @Resource
        private ManagedScheduledExecutorService scheduler;

        /**
         * Spawns an unmanaged thread, submits a one-second scheduled task from it, and
         * waits for the outcome. Propagates any exception thrown during context capture
         * or scheduling back to the caller.
         */
        public void spawnAndSchedule() throws Exception {
            var done = new CompletableFuture<Void>();
            var worker = new Thread(() -> {
                try {
                    scheduler.schedule(() -> "tick", 1, TimeUnit.SECONDS).get(5, TimeUnit.SECONDS);
                    done.complete(null);
                } catch (Exception e) {
                    done.completeExceptionally(e);
                }
            }, "unmanaged-worker");
            worker.setDaemon(true);
            worker.start();
            // Re-throws the worker's exception (wrapped in ExecutionException) if scheduling failed.
            done.get(10, TimeUnit.SECONDS);
        }
    }

    // -------------------------------------------------------------------------
    // MicroProfile Config sources
    // -------------------------------------------------------------------------

    /** Explicitly disables the OTel SDK — reproduces the exact reported scenario. */
    public static class OtelDisabledConfig implements ConfigSource {
        private static final Map<String, String> PROPS = Map.of("otel.sdk.disabled", "true");

        @Override public Map<String, String> getProperties() { return PROPS; }
        @Override public Set<String> getPropertyNames() { return PROPS.keySet(); }
        @Override public String getValue(String key) { return PROPS.get(key); }
        @Override public String getName() { return "otel-disabled-config"; }
    }

    /**
     * Enables the OTel SDK with no-op exporters — verifies that the context provider
     * degrades gracefully even when OTel is active for the application.
     */
    public static class OtelEnabledConfig implements ConfigSource {
        private static final Map<String, String> PROPS = Map.of(
            "otel.sdk.disabled",      "false",
            "otel.traces.exporter",   "none",
            "otel.metrics.exporter",  "none",
            "otel.logs.exporter",     "none"
        );

        @Override public Map<String, String> getProperties() { return PROPS; }
        @Override public Set<String> getPropertyNames() { return PROPS.keySet(); }
        @Override public String getValue(String key) { return PROPS.get(key); }
        @Override public String getName() { return "otel-enabled-config"; }
    }

    // -------------------------------------------------------------------------
    // Deployments
    // -------------------------------------------------------------------------

    @Deployment(name = "no-otel")
    public static WebArchive deploymentWithoutOtel() {
        return ShrinkWrap.create(WebArchive.class, "no-otel.war")
                .addClass(SpawnerBean.class)
                .addAsServiceProvider(ConfigSource.class, OtelDisabledConfig.class);
    }

    @Deployment(name = "otel-enabled")
    public static WebArchive deploymentWithOtel() {
        return ShrinkWrap.create(WebArchive.class, "otel-enabled.war")
                .addClass(SpawnerBean.class)
                .addAsServiceProvider(ConfigSource.class, OtelEnabledConfig.class);
    }

    // -------------------------------------------------------------------------
    // Test fields
    // -------------------------------------------------------------------------

    @Inject
    SpawnerBean spawner;

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    /**
     * Reproducer for FISH-14527 (GitHub #8385): with OTel disabled, context capture for a
     * {@link ManagedScheduledExecutorService} task submitted from an unmanaged thread inside
     * an EJB invocation must succeed — no {@link IllegalStateException} may be thrown.
     */
    @Test
    @OperateOnDeployment("no-otel")
    public void scheduleFromUnmanagedThreadWithOtelDisabled() throws Exception {
        spawner.spawnAndSchedule();
    }

    /**
     * With OTel enabled for the application, the context provider must fall back to a
     * no-op snapshot (not throw) when the submitting thread carries no component invocation.
     */
    @Test
    @OperateOnDeployment("otel-enabled")
    public void scheduleFromUnmanagedThreadWithOtelEnabled() throws Exception {
        spawner.spawnAndSchedule();
    }
}
