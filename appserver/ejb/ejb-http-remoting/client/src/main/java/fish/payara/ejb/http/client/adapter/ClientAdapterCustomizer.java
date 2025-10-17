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
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static fish.payara.ejb.http.client.adapter.CompositeClientAdapter.Builder.instantiate;

/**
 * Customizer for externalizing JNDI name matching and name transformation.
 * The class creates a decorator that takes care of matching looked up JNDI names and transforming the jndi name before
 * passed to downstream adapter.
 * <p>Most common use case is represented by method {@link #matchPrefix(String)}: Given the prefix, the downstream
 * adapter is only called when requested name matches prefix, and it is invoked with the prefix stripped from name.</p>
 * <p>Class is intended to use as static import during construction of {@link CompositeClientAdapter}.</p>
 * <pre>
 * {@code
 * import static fish.payara.ejb.http.client.adapter.ClientAdapterCustomizer.*;
 *
 * ...
 * CompositeClientAdapter.builder()
 *     .register(customize(JmsStubAdapter.class).matchPrefix("java:comp/jms"),
 *               customize(new ConnectionFactoryStub()).matchName(Pattern.compile("jms/.+Factory")::matches))
 *     .build()
 * }
 * </pre>
 */
public final class ClientAdapterCustomizer implements ClientAdapter {
    private final Supplier<ClientAdapter> downstreamAdapter;
    private Predicate<String> namePredicate;
    private Function<String, String> nameTransformation = Function.identity();

    ClientAdapterCustomizer(Supplier<ClientAdapter> downstreamAdapter) {
        this.downstreamAdapter = downstreamAdapter;
    }

    private ClientAdapterCustomizer(ClientAdapterCustomizer previous) {
        this.downstreamAdapter = previous.downstreamAdapter;
        this.namePredicate = previous.namePredicate;
        this.nameTransformation = previous.nameTransformation;
    }

    /**
     * Call downstream adapter when name matches, with jndiName parameter transformed.
     * @param jndiName jndi name requested for lookup
     * @param remoteContext naming context for remote EJB invocation
     * @return looked up adapter if downstream adapter returns one
     * @throws NamingException when downstream adapter throws one
     */
    @Override
    public Optional<Object> makeLocalProxy(String jndiName, Context remoteContext) throws NamingException {
        if (namePredicate == null || namePredicate.test(jndiName)) {
            return downstreamAdapter.get().makeLocalProxy(nameTransformation.apply(jndiName), remoteContext);
        } else {
            return Optional.empty();
        }
    }


    /**
     * Return new customizer with specified predicate for matching a name.
     * The predicates is replaced rather than composed, any current predicate is replaced with the new one in returned instance.
     * @param name predicate for matching name
     * @return new customizer instance
     */
    public ClientAdapterCustomizer matchName(Predicate<String> name) {
        ClientAdapterCustomizer conditional = new ClientAdapterCustomizer(this);
        conditional.namePredicate = name;
        return conditional;
    }

    /**
     * Return new customizer with specified name transformation function.
     * The function is replaced rather than composed, any current transformation is replaced with new one in returned instance.
     * Transformation is applied after predicate specified with {@link #matchName(Predicate)} passes
     * @param nameTransformation JNDI name transformation
     * @return new customizer instance
     */
    public ClientAdapterCustomizer transformName(Function<String, String> nameTransformation) {
        ClientAdapterCustomizer conditional = new ClientAdapterCustomizer(this);
        conditional.nameTransformation = nameTransformation != null ? nameTransformation : Function.identity();
        return conditional;
    }

    /**
     * Return new customizer that matches given prefix, and strips the prefix from the name before invoking downstream
     * adapter.
     * @param prefix JNDI name prefix to match and strip
     * @return new customizer instance
     */
    public ClientAdapterCustomizer matchPrefix(String prefix) {
        return matchName(name -> name.startsWith(prefix)).transformName(name -> name.substring(prefix.length()));
    }

    /**
     * Create customizer of given class. Class is instatiated via default constructor immediately.
     * @param adapterClass
     * @return customizer instance
     */
    public static ClientAdapterCustomizer customize(Class<? extends ClientAdapter> adapterClass) {
        return customize(instantiate(adapterClass));
    }

    /**
     * Create customizer decorating given instance.
     * @param adapterInstance
     * @return customizer instance
     */
    public static ClientAdapterCustomizer customize(ClientAdapter adapterInstance) {
        return customize(() -> adapterInstance);
    }

    /**
     * Create customizer decorating given supplier. Supplier will be invoked at every lookup, but only when JNDI name
     * predicate matches.
     * @param adapterSupplier
     * @return customizer instance
     */
    public static ClientAdapterCustomizer customize(Supplier<ClientAdapter> adapterSupplier) {
        return new ClientAdapterCustomizer(adapterSupplier);
    }
}
