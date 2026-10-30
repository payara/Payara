/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2019-2021 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.ejb.http.protocol;

import jakarta.json.bind.annotation.JsonbProperty;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

/**
 * Invoke a EJB method.
 * 
 * @author Jan Bernitt
 */
public class InvokeMethodRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonbProperty("java.naming.security.principal")
    public final String principal;
    @JsonbProperty("java.naming.security.credentials")
    public final String credentials;

    @JsonbProperty("lookup")
    public final String jndiName;
    public final String method;
    public final String[] argTypes;
    public final String[] argActualTypes;
    /**
     * JSONB sets the plain {@code Object[]} while java serialisation serialises the array to a {@code byte[]} so it is
     * not attempted to be de-serialised into {@link Object}s by receiving JAX-RS endpoint.
     */
    public final Object argValues;
    /**
     * De-serialises the {@link #argValues} into an {@code Object[]} again.
     */
    public transient ArgumentDeserializer argDeserializer;

    public InvokeMethodRequest(String principal, String credentials, String jndiName, String method, String[] argTypes,
            String[] argActualTypes, Object argValues, ArgumentDeserializer argDeserializer) {
        this.principal = principal;
        this.credentials = credentials;
        this.jndiName = jndiName;
        this.method = method;
        this.argTypes = argTypes;
        this.argActualTypes = argActualTypes;
        this.argValues = argValues;
        this.argDeserializer = argDeserializer;
    }

    @FunctionalInterface
    public interface ArgumentDeserializer {

        /**
         * Converts the method arguments form a {@link Class} and {@link Object} independent format to the actual method
         * argument {@link Object}s. This step is deferred since it has to take place within the right
         * {@link ClassLoader} context in order to not fail looking up application specific classes.
         * 
         * @param args           the argument as send in {@link InvokeMethodRequest}
         * @param argActualTypes the actual {@link Class} types of the arguments as send in {@link InvokeMethodRequest}
         * @param invoked        the {@link Method} that should be invoked with the arguments
         * @param classLoader    the {@link ClassLoader} to use to load further classes if needed.
         * @return The arguments as they should be passed to the {@link Method} invoked
         */
        Object[] deserialise(Object args, Method invoked, Type[] argActualTypes, ClassLoader classLoader);
    }
}
