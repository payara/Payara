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
package fish.payara.microprofile.telemetry.tracing;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Looks up which {@link WithSpan}-annotated methods on a class have been disabled via
 * MicroProfile Config ({@code [class]/[method]/WithSpan/enabled=false}).
 *
 * <p>Uses a {@link ClassValue} so each class is inspected at most once, the result is
 * thread-safe without explicit locking, and the entry is released automatically when
 * the class is unloaded — preventing classloader leaks across redeployments.
 *
 * <p>Only disabled method names are stored; the common case (nothing disabled) is an
 * empty set.
 */
class WithSpanEnabledLookup extends ClassValue<Set<String>> {

    static final WithSpanEnabledLookup INSTANCE = new WithSpanEnabledLookup();

    @Override
    protected Set<String> computeValue(Class<?> type) {
        Config config;
        try {
            config = ConfigProvider.getConfig();
        } catch (IllegalArgumentException ignored) {
            // No MP Config context available for this classloader (e.g. during early lifecycle).
            // Treat all methods as enabled; the value is not cached by ClassValue on exception,
            // so the next call will retry.
            return Collections.emptySet();
        }
        String prefix = type.getCanonicalName() + "/";
        Set<String> disabled = null;
        for (Method method : type.getDeclaredMethods()) {
            if (!method.isAnnotationPresent(WithSpan.class)) {
                continue;
            }
            // The config key uses the method name only, so overloaded methods sharing the same
            // name are treated as a group: disabling one disables all overloads. This is
            // intentional — the property format is [class]/[method]/WithSpan/enabled.
            String property = prefix + method.getName() + "/WithSpan/enabled";
            boolean enabled = config.getOptionalValue(property, Boolean.class).orElse(Boolean.TRUE);
            if (!enabled) {
                if (disabled == null) {
                    disabled = new HashSet<>();
                }
                disabled.add(method.getName());
            }
        }
        return disabled == null ? Collections.emptySet() : Collections.unmodifiableSet(disabled);
    }

    /**
     * Returns {@code true} if tracing has been disabled for this method via config.
     */
    static boolean isDisabled(Method method) {
        return INSTANCE.get(method.getDeclaringClass()).contains(method.getName());
    }
}
