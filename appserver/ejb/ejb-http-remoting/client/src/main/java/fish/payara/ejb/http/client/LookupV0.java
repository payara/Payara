/*
 *    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) [2019-2021] Payara Foundation and/or its affiliates. All rights reserved.
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

package fish.payara.ejb.http.client;

import javax.naming.NamingException;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import java.util.HashMap;
import java.util.Map;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

class LookupV0 extends Lookup {

    private final WebTarget invokerRoot;
    private final WebTarget lookup;

    LookupV0(Map<String, Object> environment, WebTarget invokerRoot, WebTarget v0lookup) {
        super(environment);
        this.invokerRoot = invokerRoot;
        this.lookup = v0lookup;
    }

    @Override
    public Object lookup(String jndiName) throws NamingException {
        // For the lookup do a call to the remote server first to obtain
        // the remote business interface class name given a JNDI lookup name.
        // The JNDI lookup name normally does not give us this interface name.
        // This also allows us to check the JNDI name indeed resolves before
        // we create a proxy and return it here.

        Map<String, Object> payload = new HashMap<>();
        payload.put("lookup", jndiName);
        String className =
                lookup
                        .request()
                        .post(Entity.entity(payload, APPLICATION_JSON))
                        .readEntity(String.class);

        // After we have obtained the class name of the remote interface, generate
        // a proxy based on it.

        try {
            return EjbHttpProxyFactory.newProxy(
                    Class.forName(className),
                    invokerRoot
                            .path("ejb")
                            .path("invoke"),
                    jndiName,
                    new HashMap<>(environment)
            );
        } catch (ClassNotFoundException e) {
            throw wrap("Local class does not exist for JNDI name "+jndiName, e);
        }
    }
}
