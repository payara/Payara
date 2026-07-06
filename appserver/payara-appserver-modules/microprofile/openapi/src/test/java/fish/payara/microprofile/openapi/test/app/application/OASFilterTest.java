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
package fish.payara.microprofile.openapi.test.app.application;

import com.fasterxml.jackson.databind.node.ObjectNode;
import fish.payara.microprofile.openapi.test.app.OpenApiApplicationTest;
import fish.payara.microprofile.openapi.test.util.JsonUtils;
import static org.eclipse.microprofile.openapi.OASFactory.createObject;
import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.media.Schema.SchemaType;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 * In response to {@link https://github.com/payara/Payara/issues/3724} this tests checks that using a {@link OASFilter}
 * to add a {@link Schema} with additional properties is successful.
 */
@OpenAPIDefinition(
        info = @Info(title = "title", version = "version"))
public class OASFilterTest extends OpenApiApplicationTest {

    @Override
    protected Class<? extends OASFilter> filterClass() {
        return AdditionalPropertiesOASFilter.class;
    }

    public static class AdditionalPropertiesOASFilter implements OASFilter {

        @Override
        public void filterOpenAPI(final OpenAPI openAPI) {
            openAPI.getComponents().addSchema("SimpleMap", createMapKey());
        }

        private static Schema createMapKey() {
            return createObject(Schema.class)
                    .type(SchemaType.OBJECT)
                    .additionalPropertiesSchema(createObject(Schema.class).type(SchemaType.STRING));
        }
    }

    @Test
    public void additionalPropertiesAreAddedByFilter() {
        ObjectNode openAPI = getOpenAPIJson();
        assertEquals("string", JsonUtils.path(openAPI, "components.schemas.SimpleMap.additionalProperties.type").textValue());
    }
}
