/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2019-2020] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.microprofile.openapi.impl.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import static fish.payara.microprofile.openapi.test.util.JsonUtils.path;
import static java.util.Collections.singletonMap;
import static org.eclipse.microprofile.openapi.OASFactory.createServer;
import static org.eclipse.microprofile.openapi.OASFactory.createServerVariable;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.servers.Server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import fish.payara.microprofile.openapi.impl.model.servers.ServerImpl;

/**
 * Checks the JSON rendering of {@link fish.payara.microprofile.openapi.impl.model.servers.*}.
 */
public class ServersBuilderTest extends OpenApiBuilderTest {

    @Override
    protected void setupBaseDocument(OpenAPI document) {
        document.addServer(createServer()
                .url("url")
                .description("description")
                .addExtension("x-ext", "ext-value")
                .variables(singletonMap("var1", createServerVariable()
                        .defaultValue("defaultValue")
                        .description("description")
                        .addEnumeration("enumeration1")
                        .addEnumeration("enumeration2")
                        .addExtension("x-ext", "ext-value"))));
    }

    @Test
    public void serverIsInitialisedCorrectly() {
        Server server = new ServerImpl();
        assertNotNull("server variables are null", server.getVariables());
    }

    @Test
    public void serverHasExpectedFields() {
        JsonNode server = path(getOpenAPIJson(), "servers.[0]");
        assertNotNull(server);
        assertEquals("url", server.get("url").textValue());
        assertEquals("description", server.get("description").textValue());
        assertEquals("ext-value", server.get("x-ext").textValue());
    }

    @Test
    public void serverVariablesHasExpectedFields() {
        JsonNode var1 = path(getOpenAPIJson(), "servers.[0].variables.var1");
        assertNotNull(var1);
        assertEquals("defaultValue", var1.get("default").textValue());
        assertEquals("description", var1.get("description").textValue());
        assertEquals("ext-value", var1.get("x-ext").textValue());
        ArrayNode enumeration = (ArrayNode) var1.get("enum");
        assertNotNull(enumeration);
        assertEquals(2, enumeration.size());
        assertEquals("enumeration1", enumeration.get(0).textValue());
        assertEquals("enumeration2", enumeration.get(1).textValue());
    }
}
