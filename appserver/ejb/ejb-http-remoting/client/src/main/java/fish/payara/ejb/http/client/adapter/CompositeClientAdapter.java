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

package fish.payara.ejb.http.client.adapter;

import javax.naming.Context;
import javax.naming.NamingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Adapter that allows composing multiple adapter implementations. {@Ccode CompositeClientAdapter}
 * invokes all registered adapters in order in which they were registered, and returns first non-empty match for a name.
 * <p>Downstream adapters are registered by constructing a new {@link Builder}, or method {@link #newBuilder()}</p>
 * <p>Downstream adapters can also be customized by decorators provided by {@link ClientAdapterCustomizer}, which
 * allows separating name matching from proxy construction in adapters' implementations.</p>
 * @see ClientAdapterCustomizer
 */
public final class CompositeClientAdapter implements ClientAdapter {
    private final List<ClientAdapter> adapters;

    private CompositeClientAdapter(Builder builder) {
        this.adapters = new ArrayList<>(builder.adapters);
    }

    /**
     * Find a client adapter for handling specific {@code jndiName}. Composite client adapter will attempt invoking
     * {@link ClientAdapter#makeLocalProxy(String, Context)} for all registered adapters in order they were
     * registered in. The iteration ends when
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
    public Optional<Object> makeLocalProxy(String jndiName, Context remoteContext) throws NamingException {
        for (ClientAdapter adapter : adapters) {
            Optional<Object> result = adapter.makeLocalProxy(jndiName, remoteContext);
            if (result.isPresent()) {
                return result;
            }
        }
        return Optional.empty();
    }

    /**
     * Convenience method for constructing a {@link Builder}
     * @return new builder instance
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * Builder for composite Client Adapter.
     * <p>Adapters can be registered in three ways:</p>
     * <ul>
     *     <li>{@link #register(ClientAdapter...)} : providing an instance</li>
     *     <li>{@link #register(Class[])} : providing a class name. Class will be immediately instantiated via default constructor. </li>
     * </ul>
     * Further customization of client adapter is possible by means of {@link ClientAdapterCustomizer}
     */
    public static final class Builder {
        private List<ClientAdapter> adapters = new ArrayList<>();

        /**
         * Build resulting adapter.
         * @return new instance of CompositeClientAdapter
         */
        public CompositeClientAdapter build() {
            return new CompositeClientAdapter(this);
        }

        /**
         * Register new adapter by means of class name(s). Each class will be instantiated using default constructor.
         * @param adapterClasses adapter classes to register
         * @return this builder
         */
        public Builder register(Class<? extends ClientAdapter>... adapterClasses) {
            for (Class<? extends ClientAdapter> adapterClass : adapterClasses) {
                ClientAdapter instance = instantiate(adapterClass);
                register(instance);
            }
            return this;
        }

        /**
         * Register new adapter by means of instance(s).
         * @param adapterInstances adapter instances to register
         * @return this builder
         */
        public Builder register(ClientAdapter... adapterInstances) {
            Stream.of(adapterInstances).forEach(this::register);
            return this;
        }

        private void register(ClientAdapter adapterSupplier) {
            adapters.add(adapterSupplier);
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
