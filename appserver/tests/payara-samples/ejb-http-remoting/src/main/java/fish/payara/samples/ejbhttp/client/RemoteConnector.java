/*
 *    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) [2019-2020] Payara Foundation and/or its affiliates. All rights reserved.
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

package fish.payara.samples.ejbhttp.client;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import fish.payara.ejb.http.client.RemoteEJBContextFactory;
import fish.payara.ejb.http.protocol.SerializationType;

import java.util.Hashtable;

import static javax.naming.Context.INITIAL_CONTEXT_FACTORY;
import static javax.naming.Context.PROVIDER_URL;

public enum RemoteConnector {
    JSON_V0(SerializationType.JSON, 0),
    JSON_V1(SerializationType.JSON, 1),
    JAVA_V1(SerializationType.JAVA, 1);

    private final InitialContext ejbRemoteContext;
    private final SerializationType type;
    private final int version;

    RemoteConnector(SerializationType type, int version) {
        this.type = type;
        this.version = version;
        Hashtable<String, String> environment = new Hashtable<>();
        environment.put(INITIAL_CONTEXT_FACTORY, "fish.payara.ejb.rest.client.RemoteEJBContextFactory");
        environment.put(PROVIDER_URL, "http://localhost:8080/ejb-invoker");
        environment.put(RemoteEJBContextFactory.JAXRS_CLIENT_SERIALIZATION, type.toString());
        environment.put(RemoteEJBContextFactory.JAXRS_CLIENT_PROTOCOL_VERSION, String.valueOf(version));
        try {
            ejbRemoteContext = new InitialContext(environment);
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T lookup(String jndiName) throws NamingException {
        return (T) ejbRemoteContext.lookup(jndiName);
    }

    public SerializationType getSerializationType() {
        return type;
    }

    public int getVersion() {
        return version;
    }
}
