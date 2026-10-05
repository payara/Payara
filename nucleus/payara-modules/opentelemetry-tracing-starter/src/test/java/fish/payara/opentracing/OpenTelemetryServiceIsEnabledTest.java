/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
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
 */
package fish.payara.opentracing;

import io.opentelemetry.sdk.OpenTelemetrySdk;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Verifies that {@link OpenTelemetryService.OpenTelemetrySdkHandle#isEnabled()} correctly
 * reflects whether a real or Noop SDK was stored, so that
 * {@link OpenTelemetryService#isEnabled()} never returns {@code true} solely because a
 * map entry exists (the pre-fix bug: any deploy caused isEnabled()=true forever).
 */
public class OpenTelemetryServiceIsEnabledTest {

    @Test
    public void handleWithEnabledTrueReportsEnabled() {
        OpenTelemetrySdkHandle handle = new OpenTelemetrySdkHandle(noopSdk(), true);
        assertTrue("Handle created with enabled=true must report isEnabled()=true",
                handle.isEnabled());
    }

    @Test
    public void handleWithEnabledFalseReportsDisabled() {
        OpenTelemetrySdkHandle handle = new OpenTelemetrySdkHandle(noopSdk(), false);
        assertFalse("Handle created with enabled=false must report isEnabled()=false",
                handle.isEnabled());
    }

    @Test
    public void handleCreatedWithLegacySingleArgConstructorDefaultsToEnabled() {
        // The single-arg constructor is used for runtimeHandle (always a real SDK).
        OpenTelemetrySdkHandle handle = new OpenTelemetrySdkHandle(noopSdk());
        assertTrue("Single-arg constructor must default to enabled=true for backwards compatibility",
                handle.isEnabled());
    }

    @Test
    public void disabledHandleDoesNotBecomeEnabledAfterConstruction() {
        OpenTelemetrySdkHandle handle = new OpenTelemetrySdkHandle(noopSdk(), false);
        // Call a non-mutating method to ensure no lazy side-effect flips the flag.
        handle.sdk();
        assertFalse("enabled flag must be immutable after construction",
                handle.isEnabled());
    }

    private static OpenTelemetrySdk noopSdk() {
        return OpenTelemetrySdk.builder().build();
    }

    // Use the package-private inner class directly via a local subclass so the test
    // does not need to spin up the full HK2 server context.
    static class OpenTelemetrySdkHandle extends OpenTelemetryService.OpenTelemetrySdkHandle {
        OpenTelemetrySdkHandle(OpenTelemetrySdk sdk) {
            super(sdk);
        }
        OpenTelemetrySdkHandle(OpenTelemetrySdk sdk, boolean enabled) {
            super(sdk, enabled);
        }
    }
}