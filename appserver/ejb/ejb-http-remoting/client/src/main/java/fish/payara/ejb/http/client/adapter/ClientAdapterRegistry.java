/*
 *    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) [2019] Payara Foundation and/or its affiliates. All rights reserved.
 *
 *    The contents of this file are subject to the terms of either the GNU
 *    General Public License Version 2 only ("GPL") or the Common Development
 *    and Distribution License("CDDL") (collectively, the "License").  You
 *    may not use this file except in compliance with the License.  You can
 *    obtain a copy of the License at
 *    https://github.com/payara/Payara/blob/master/LICENSE.txt
 *    See the License for the specific
 *    language governing permissions and limitations under the License.
 *
 *    When distributing the software, include this License Header Notice in each
 *    file and include the License file at glassfish/legal/LICENSE.txt.
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

package fish.payara.ejb.http.client.adapter;

import javax.naming.Context;
import javax.naming.NamingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Registry of Client Adapters to use for specific remote context.
 * <p>The class offers a {@link Builder} to register static or dynamic client adapters, and implements logic to call
 * them in sequence in order to find the one matching specific jndi name</p>
 */
public final class ClientAdapterRegistry implements ClientAdapter {
    private final List<Supplier<? extends ClientAdapter>> adapterSuppliers;

    private ClientAdapterRegistry(Builder builder) {
        this.adapterSuppliers = new ArrayList<>(builder.adapterSuppliers);
    }

    /**
     * Find a client adapter for handling specific {@code jndiName}. The registry will attempt invoking
     * {@link ClientAdapter#makeClientAdapter(String, Context)} for all registered adapters in order they were
     * registered. The iteration ends when
     * <ol>
     *     <li>A registered adapter returns non-empty value; or</li>
     *     <li>A registered adapter throws {@code NamingException}</li>
     *     <li>All adapters are tried and return empty value</li>
     * </ol>
     *
     * @param jndiName JNDI name that is being looked up
     * @param remoteContext EJB HTTP Client naming context for delegating the calls
     * @return adapter object if any of registered client adapters returns one, {@code Optional.empty()} otherwise
     * @throws NamingException if any of attempted client adapters throws one
     */
    @Override
    public Optional<Object> makeClientAdapter(String jndiName, Context remoteContext) throws NamingException {
        Optional<ResolutionResult> resolutionResult = adapterSuppliers.stream()
                .map(Supplier::get)
                .map(adapter -> resolve(adapter, jndiName, remoteContext))
                .filter(Optional::isPresent)
                .findFirst()
                .map(Optional::get);
        if (resolutionResult.isPresent()) {
            ResolutionResult result = resolutionResult.get();
            if (result.exception != null) {
                throw result.exception;
            } else {
                return Optional.of(result.instance);
            }
        } else {
            return Optional.empty();
        }
    }

    /**
     * Convenience method for constructing a {@link Builder}
     * @return new builder instance
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    private Optional<ResolutionResult> resolve(ClientAdapter adapter, String jndiName, Context remoteContext) {
        try {
            return adapter.makeClientAdapter(jndiName, remoteContext).map(ResolutionResult::result);
        } catch (NamingException e) {
            return Optional.of(ResolutionResult.exception(e));
        }
    }

    private static class ResolutionResult {
        Object instance;
        NamingException exception;

        static ResolutionResult result(Object instance) {
            ResolutionResult result = new ResolutionResult();
            result.instance = instance;
            return result;
        }

        static ResolutionResult exception(NamingException exception) {
            ResolutionResult result = new ResolutionResult();
            result.exception = exception;
            return result;
        }
    }


    /**
     * Builder for Client Adapter Registry.
     * <p>Adapters can be registered in three ways:</p>
     * <ul>
     *     <li>{@link #register(ClientAdapter...)} : providing an instance</li>
     *     <li>{@link #register(Class[])} : providing a class name. Class will be immediately instantiated via default constructor. </li>
     *     <li>{@link #register(Supplier[])} : providing instance supplier. The supplier will be invoked for each lookup. This can enable more contextual
     *          use cases, for example thread-local adapters.</li>
     * </ul>
     * Further customization of client adapter is possible by means of {@link ClientAdapterCustomizer}
     */
    public static final class Builder {
        private List<Supplier<? extends ClientAdapter>> adapterSuppliers = new ArrayList<>();

        /**
         * Build resulting registry.
         * @return new instance of ClientAdapterRegistry
         */
        public ClientAdapterRegistry build() {
            return new ClientAdapterRegistry(this);
        }

        /**
         * Register new adapter by means of class name(s). Each class will be instantiated using default constructor.
         * @param adapterClasses adapter classes to register
         * @return this builder
         */
        public Builder register(Class<? extends ClientAdapter>... adapterClasses) {
            for (Class<? extends ClientAdapter> adapterClass : adapterClasses) {
                ClientAdapter instance = instantiate(adapterClass);
                register(() -> instance);
            }
            return this;
        }

        /**
         * Register new adapter by means of instance(s).
         * @param adapterInstances adapter instances to register
         * @return this builder
         */
        public Builder register(ClientAdapter... adapterInstances) {
            for (ClientAdapter adapterInstance : adapterInstances) {
                register(() -> adapterInstance);
            }
            return this;
        }

        /**
         * Register new adapter by means of instance suppliers. The adapter classes are fetched from supplier at every
         * lookup.
         * @param adapterSuppliers adapter suppliers to register
         * @return this builder
         */
        public Builder register(Supplier<? extends ClientAdapter>... adapterSuppliers) {
            for (Supplier<? extends ClientAdapter> adapterSupplier : adapterSuppliers) {
                register(adapterSupplier);
            }
            return this;
        }

        private void register(Supplier<? extends ClientAdapter> adapterSupplier) {
            adapterSuppliers.add(adapterSupplier);
        }

        static ClientAdapter instantiate(Class<? extends ClientAdapter> adapterClass) {
            try {
                return adapterClass.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new IllegalArgumentException("Cannot instantiate " + adapterClass.getName(), e);
            }
        }
    }

}
