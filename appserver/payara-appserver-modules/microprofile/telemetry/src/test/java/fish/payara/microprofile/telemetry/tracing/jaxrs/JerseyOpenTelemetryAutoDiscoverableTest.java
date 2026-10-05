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
package fish.payara.microprofile.telemetry.tracing.jaxrs;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigValue;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.Converter;
import org.junit.Test;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Verifies the explicit kill-switch logic in
 * {@link JerseyOpenTelemetryAutoDiscoverable#isExplicitlyDisabled(Config)}.
 */
public class JerseyOpenTelemetryAutoDiscoverableTest {

    @Test
    public void nullConfigIsNotDisabled() {
        assertFalse("null config must not disable tracing",
                JerseyOpenTelemetryAutoDiscoverable.isExplicitlyDisabled(null));
    }

    @Test
    public void propertyAbsentIsNotDisabled() {
        assertFalse("absent kill-switch property must not disable tracing",
                JerseyOpenTelemetryAutoDiscoverable.isExplicitlyDisabled(configWith(null)));
    }

    @Test
    public void propertyTrueIsNotDisabled() {
        assertFalse("kill-switch set to true must NOT disable tracing",
                JerseyOpenTelemetryAutoDiscoverable.isExplicitlyDisabled(configWith(true)));
    }

    @Test
    public void propertyFalseIsDisabled() {
        assertTrue("kill-switch set to false must disable tracing",
                JerseyOpenTelemetryAutoDiscoverable.isExplicitlyDisabled(configWith(false)));
    }

    private static Config configWith(Boolean value) {
        return new Config() {
            @Override
            @SuppressWarnings("unchecked")
            public <T> Optional<T> getOptionalValue(String propertyName, Class<T> propertyType) {
                if (JerseyOpenTelemetryAutoDiscoverable.JAXRS_TRACING_ENABLED_MP_KEY.equals(propertyName)
                        && Boolean.class.equals(propertyType)) {
                    return (Optional<T>) Optional.ofNullable(value);
                }
                return Optional.empty();
            }

            @Override public <T> T getValue(String s, Class<T> c) { throw new NoSuchElementException(); }
            @Override public ConfigValue getConfigValue(String s) { throw new NoSuchElementException(); }
            @Override public Iterable<String> getPropertyNames() { return List.of(); }
            @Override public Iterable<ConfigSource> getConfigSources() { return List.of(); }
            @Override public <T> Optional<Converter<T>> getConverter(Class<T> c) { return Optional.empty(); }
            @Override public <T> T unwrap(Class<T> c) { return null; }
        };
    }
}