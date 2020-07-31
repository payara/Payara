/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2018] Payara Foundation and/or its affiliates. All rights reserved.
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

import fish.payara.microprofile.openapi.test.app.OpenApiApplicationTest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_XML;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.models.responses.APIResponses;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import org.junit.Test;

/**
 * A resource to test that various response types are mapped properly.
 */
@Path("/response")
@Produces({ APPLICATION_JSON, APPLICATION_XML })
public class ResponseTest extends OpenApiApplicationTest {

    @GET
    @APIResponse(responseCode = "200", description = "success", 
        content = @Content(schema = @Schema(description = "hello!")))
    @APIResponse(responseCode = "400", description = "error")
    public String getInheritedMediaType() {
        return null;
    }

    @Test
    public void inheritedMediaTypeTest() {
        APIResponses responses = getDocument().getPaths().getPathItem("/test/response").getGET().getResponses();
        // Test the default response doesn't exist
        assertNull("The default response should be removed when not used.", responses.getDefaultValue());

        // Test the 200 response
        assertNotNull("The 200 response should have been created.", responses.getAPIResponse("200"));
        assertNotNull("The 200 response should return application/json.",
                responses.getAPIResponse("200").getContent().getMediaType(APPLICATION_JSON));
        assertEquals("The 200 response application/json should match the specified schema.", "hello!",
                responses.getAPIResponse("200").getContent().getMediaType(APPLICATION_JSON).getSchema().getDescription());
        assertNotNull("The 200 response should return application/xml.",
                responses.getAPIResponse("200").getContent().getMediaType(APPLICATION_XML));
        assertEquals("The 200 response application/xml should match the specified schema.", "hello!",
                responses.getAPIResponse("200").getContent().getMediaType(APPLICATION_XML).getSchema().getDescription());
    }

}