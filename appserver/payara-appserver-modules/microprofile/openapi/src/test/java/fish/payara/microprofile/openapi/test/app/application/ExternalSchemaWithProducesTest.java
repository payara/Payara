/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 *  Copyright (c) 2020-2021 Payara Foundation and/or its affiliates. All rights reserved.
 *  
 *  The contents of this file are subject to the terms of either the GNU
 *  General Public License Version 2 only ("GPL") or the Common Development
 *  and Distribution License("CDDL") (collectively, the "License").  You
 *  may not use this file except in compliance with the License.  You can
 *  obtain a copy of the License at
 *  https://github.com/payara/Payara/blob/main/LICENSE.txt
 *  See the License for the specific
 *  language governing permissions and limitations under the License.
 * 
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 * 
 *  GPL Classpath Exception:
 *  The Payara Foundation designates this particular file as subject to the "Classpath"
 *  exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *  file that accompanied this code.
 * 
 *  Modifications:
 *  If applicable, add the following below the License Header, with the fields
 *  enclosed by brackets [] replaced by your own identifying information:
 *  "Portions Copyright [year] [name of copyright owner]"
 * 
 *  Contributor(s):
 *  If you wish your version of this file to be governed by only the CDDL or
 *  only the GPL Version 2, indicate your decision by adding "[Contributor]
 *  elects to include this software in this distribution under the [CDDL or GPL
 *  Version 2] license."  If you don't indicate a single choice of license, a
 *  recipient has the option to distribute your version of this file under
 *  either the CDDL, the GPL Version 2 or to extend the choice of license to
 *  its licensees as provided above.  However, if you add GPL Version 2 code
 *  and therefore, elected the GPL Version 2 license, then the option applies
 *  only if the new code is made subject to such option by the copyright
 *  holder.
 */
package fish.payara.microprofile.openapi.test.app.application;

import com.fasterxml.jackson.databind.JsonNode;
import fish.payara.microprofile.openapi.resource.rule.ApplicationProcessedDocument;
import fish.payara.microprofile.openapi.test.app.OpenApiApplicationTest;
import fish.payara.microprofile.openapi.test.app.TestApplication;
import fish.payara.microprofile.openapi.test.app.application.schema.TeacherDTO;
import fish.payara.microprofile.openapi.test.util.JsonUtils;
import static fish.payara.microprofile.openapi.test.util.JsonUtils.toJson;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * This test uses an {@link Produces} annotation on the REST method
 * and also has the schema in another package.
 * @author jonathan coustick
 */
@Path("/teachers")
public class ExternalSchemaWithProducesTest extends OpenApiApplicationTest {
    
    @Before
    @Override
    public void createDocument() {
        Class<?> filter = filterClass();
        document = ApplicationProcessedDocument.createDocument(getClass(), TeacherDTO.class, TestApplication.class);
        jsonDocument = toJson(document);
    }
    
    @GET
    @APIResponse(
            description = "Get the Teacher information",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = TeacherDTO.class)
            )
    )
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTeacher() {
        return Response.ok().build();
    }
    
    @Test
    public void externalSchemaExampleIsRendere() {
        JsonNode nameProperties = JsonUtils.path(getOpenAPIJson(), "components.schemas.Teacher.properties.name");
        Assert.assertNotNull(nameProperties);
        Assert.assertEquals("string", nameProperties.get("type").textValue());
        Assert.assertEquals("the teacher name", nameProperties.get("description").textValue());
        Assert.assertEquals("Trunchbull", nameProperties.get("example").textValue());
    }
}
