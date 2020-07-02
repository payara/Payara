/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2019] Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
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
package fish.payara.microprofile.openapi.test.app.application;

import com.fasterxml.jackson.databind.JsonNode;
import fish.payara.microprofile.openapi.impl.model.media.SchemaImpl;
import fish.payara.microprofile.openapi.impl.visitor.OpenApiWalker;
import fish.payara.microprofile.openapi.test.app.OpenApiApplicationTest;
import fish.payara.microprofile.openapi.test.util.JsonUtils;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;

/**
 * This is a minimal example extracted from a failing TCK test where the {@link Schema} details of a bean field were
 * missing from the output.
 * 
 * These should occur as {@link SchemaImpl#getProperties()}.
 */
@Path("/users")
public class SchemaExampleTest extends OpenApiApplicationTest {

    @Schema(maxProperties = 1024, minProperties = 1, requiredProperties = { "password" })
    public static class User {
        @Schema(example = "bobSm37")
        String password;
    }

    /**
     * Interestingly adding this method that uses the {@link User} class could cause the properties of the
     * {@link User#password} schema to disappear from the model and output. This is worked around by the
     * {@link OpenApiWalker} processing {@link Schema} annotation twice.
     */
    @GET
    public User getUser(@SuppressWarnings("unused") @PathParam("id") String id) {
        return new User();
    }

    @Test
    public void fieldSchemaExampleIsRendered() {
        JsonNode passwordProperties = JsonUtils.path(getOpenAPIJson(), "components.schemas.User.properties.password");
        assertNotNull(passwordProperties);
        assertEquals("string", passwordProperties.get("type").textValue());
        assertEquals("bobSm37", passwordProperties.get("example").textValue());
    }
}
