/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2025] Payara Foundation and/or its affiliates. All rights reserved.
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
package org.glassfish.weld;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.ws.rs.ConstrainedTo;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.FeatureContext;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.ContextResolver;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.internal.spi.ForcedAutoDiscoverable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import static jakarta.ws.rs.RuntimeType.SERVER;
import static org.glassfish.jersey.internal.spi.AutoDiscoverable.DEFAULT_PRIORITY;

/**
 * Works in conjunction with {@link org.glassfish.weld.GlassFishWeldProvider} to provide the
 * correct {@link Jsonb} instance based on the type of Jax-RS resource class being processed.
 *
 * This includes creating {@link Jsonb} instances that contains correct {@link jakarta.enterprise.inject.spi.BeanManager}
 */
@ConstrainedTo(SERVER)
@Priority(DEFAULT_PRIORITY)
@Produces(MediaType.APPLICATION_JSON)
public class JaxRSJsonContextResolver implements ContextResolver<Jsonb>, ForcedAutoDiscoverable {
    private final Map<Class<?>, Jsonb> jsonbMap = new ConcurrentHashMap<>();
    private final List<ContextResolver<?>> existingResolvers;
    private final List<Class<ContextResolver<?>>> existingResolverClasses;

    @Inject
    InjectionManager injectionManager;

    public JaxRSJsonContextResolver() {
        this.existingResolvers = Collections.emptyList();
        this.existingResolverClasses = Collections.emptyList();
    }

    private JaxRSJsonContextResolver(List<ContextResolver<?>> existingResolvers,
                                     List<Class<ContextResolver<?>>> existingResolverClasses) {
        this.existingResolvers = existingResolvers;
        this.existingResolverClasses = existingResolverClasses;
    }

    @Override
    public void configure(FeatureContext context) {
        List<ContextResolver<?>> resolvers = context.getConfiguration().getInstances().stream()
                .filter(ContextResolver.class::isInstance)
                .map(resolver -> (ContextResolver<?>) resolver)
                .collect(Collectors.toList());
        @SuppressWarnings("unchecked")
        var resolverClasses = context.getConfiguration().getClasses().stream()
                .filter(ContextResolver.class::isAssignableFrom)
                .map(cls -> (Class<ContextResolver<?>>) cls)
                .collect(Collectors.toList());
        context.register(new JaxRSJsonContextResolver(resolvers, resolverClasses));
    }

    @Override
    public Jsonb getContext(Class<?> type) {
        return jsonbMap.computeIfAbsent(type, unused -> {
            instantiateResolverClasses();
            for (ContextResolver<?> resolver : existingResolvers) {
                Object result = resolver.getContext(type);
                if (result instanceof Jsonb) {
                    return (Jsonb) result;
                }
            }
            return JsonbBuilder.create();
        });
    }

    private void instantiateResolverClasses() {
        if (!existingResolverClasses.isEmpty()) {
            existingResolverClasses.stream().map(injectionManager::getInstance)
                    .filter(Objects::nonNull)
                    .forEach(existingResolvers::add);
            existingResolverClasses.clear();
        }
    }
}
