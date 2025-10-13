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
package fish.payara.ejb.http.client;

import static java.lang.reflect.Proxy.newProxyInstance;
import static java.security.AccessController.doPrivileged;
import static java.util.Collections.emptyList;

import java.security.PrivilegedAction;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;

/**
 * This class generates a type-safe proxy for a given remote EJB interface.
 * 
 * <p>
 * Calls on this proxy are handled by {@link EjbHttpProxyHandler}.
 * 
 * 
 * @author Arjan Tijms
 * @since Payara 5.191
 *
 */
final class EjbHttpProxyFactory {

    private static final MultivaluedMap<String, Object> EMPTY_MULTI_MAP = new MultivaluedHashMap<>();

    public static <C> C newProxy(Class<C> remoteBusinessInterface, WebTarget target, String lookup, Map<String, Object> jndiOptions) {
        return newProxy(remoteBusinessInterface, target, EMPTY_MULTI_MAP, emptyList(), lookup, jndiOptions);
    }

    @SuppressWarnings("unchecked")
    public static <C> C newProxy(Class<C> remoteBusinessInterface, WebTarget target, MultivaluedMap<String, Object> headers, List<Cookie> cookies, String lookup, Map<String, Object> jndiOptions) {
        return (C)
            newProxyInstance(doPrivileged((PrivilegedAction<ClassLoader>)() -> remoteBusinessInterface.getClassLoader()),
                new Class[] { remoteBusinessInterface },
                new EjbHttpProxyHandler(addPathFromClass(remoteBusinessInterface, target), headers, cookies, lookup, jndiOptions));
    }
    
    private static WebTarget addPathFromClass(Class<?> clazz, WebTarget target) {
        return target.path(clazz.getSimpleName());
    }

}
