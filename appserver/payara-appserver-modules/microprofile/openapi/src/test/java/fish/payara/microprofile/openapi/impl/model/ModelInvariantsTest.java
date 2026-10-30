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
package fish.payara.microprofile.openapi.impl.model;

import fish.payara.microprofile.openapi.impl.model.callbacks.CallbackImpl;
import fish.payara.microprofile.openapi.impl.model.examples.ExampleImpl;
import fish.payara.microprofile.openapi.impl.model.headers.HeaderImpl;
import fish.payara.microprofile.openapi.impl.model.info.ContactImpl;
import fish.payara.microprofile.openapi.impl.model.info.InfoImpl;
import fish.payara.microprofile.openapi.impl.model.info.LicenseImpl;
import fish.payara.microprofile.openapi.impl.model.links.LinkImpl;
import fish.payara.microprofile.openapi.impl.model.media.ContentImpl;
import fish.payara.microprofile.openapi.impl.model.media.DiscriminatorImpl;
import fish.payara.microprofile.openapi.impl.model.media.EncodingImpl;
import fish.payara.microprofile.openapi.impl.model.media.MediaTypeImpl;
import fish.payara.microprofile.openapi.impl.model.media.SchemaImpl;
import fish.payara.microprofile.openapi.impl.model.media.XMLImpl;
import fish.payara.microprofile.openapi.impl.model.parameters.ParameterImpl;
import fish.payara.microprofile.openapi.impl.model.parameters.RequestBodyImpl;
import fish.payara.microprofile.openapi.impl.model.responses.APIResponseImpl;
import fish.payara.microprofile.openapi.impl.model.responses.APIResponsesImpl;
import fish.payara.microprofile.openapi.impl.model.security.OAuthFlowImpl;
import fish.payara.microprofile.openapi.impl.model.security.OAuthFlowsImpl;
import fish.payara.microprofile.openapi.impl.model.security.SecurityRequirementImpl;
import fish.payara.microprofile.openapi.impl.model.security.SecuritySchemeImpl;
import fish.payara.microprofile.openapi.impl.model.servers.ServerImpl;
import fish.payara.microprofile.openapi.impl.model.servers.ServerVariableImpl;
import fish.payara.microprofile.openapi.impl.model.tags.TagImpl;
import java.util.List;
import java.util.function.BiPredicate;
import org.eclipse.microprofile.openapi.models.Components;
import org.eclipse.microprofile.openapi.models.Extensible;
import org.eclipse.microprofile.openapi.models.ExternalDocumentation;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.Paths;
import org.eclipse.microprofile.openapi.models.callbacks.Callback;
import org.eclipse.microprofile.openapi.models.examples.Example;
import org.eclipse.microprofile.openapi.models.headers.Header;
import org.eclipse.microprofile.openapi.models.info.Contact;
import org.eclipse.microprofile.openapi.models.info.Info;
import org.eclipse.microprofile.openapi.models.info.License;
import org.eclipse.microprofile.openapi.models.links.Link;
import org.eclipse.microprofile.openapi.models.media.Content;
import org.eclipse.microprofile.openapi.models.media.Discriminator;
import org.eclipse.microprofile.openapi.models.media.Encoding;
import org.eclipse.microprofile.openapi.models.media.MediaType;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.media.XML;
import org.eclipse.microprofile.openapi.models.parameters.Parameter;
import org.eclipse.microprofile.openapi.models.parameters.RequestBody;
import org.eclipse.microprofile.openapi.models.responses.APIResponse;
import org.eclipse.microprofile.openapi.models.responses.APIResponses;
import org.eclipse.microprofile.openapi.models.security.OAuthFlow;
import org.eclipse.microprofile.openapi.models.security.OAuthFlows;
import org.eclipse.microprofile.openapi.models.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.models.security.SecurityScheme;
import org.eclipse.microprofile.openapi.models.servers.Server;
import org.eclipse.microprofile.openapi.models.servers.ServerVariable;
import org.eclipse.microprofile.openapi.models.tags.Tag;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class ModelInvariantsTest {

    interface HasAdd<T, V> {

        T add(T obj, String key, V value);
    }

    @Test
    public void addKeyValueIgnoresNull() {
        BiPredicate<Extensible<?>, String> hasExtension = (obj, key) -> obj.getExtensions() != null && obj.getExtensions().containsKey(key);
        assertAddIgnoresNull(new CallbackImpl(), Callback::addPathItem, Callback::hasPathItem);
        assertAddIgnoresNull(new CallbackImpl(), Callback::addExtension, hasExtension);
        assertAddIgnoresNull(new ExampleImpl(), Example::addExtension,  hasExtension);
        assertAddIgnoresNull(new HeaderImpl(), Header::addExample, (obj, key) -> obj.getExamples().containsKey(key));
        assertAddIgnoresNull(new HeaderImpl(), Header::addExtension, hasExtension);
        assertAddIgnoresNull(new ContactImpl(), Contact::addExtension, hasExtension);
        assertAddIgnoresNull(new InfoImpl(), Info::addExtension, hasExtension);
        assertAddIgnoresNull(new LicenseImpl(), License::addExtension, hasExtension);
        assertAddIgnoresNull(new LinkImpl(), Link::addParameter, (obj, key) -> obj.getParameters().containsKey(key));
        assertAddIgnoresNull(new LinkImpl(), Link::addExtension, hasExtension);
        assertAddIgnoresNull(new ContentImpl(), Content::addMediaType, Content::hasMediaType);
        assertAddIgnoresNull(new DiscriminatorImpl(), Discriminator::addMapping, (obj, key) -> obj.getMapping().containsKey(key));
        assertAddIgnoresNull(new EncodingImpl(), Encoding::addHeader, (obj, key) -> obj.getHeaders().containsKey(key));
        assertAddIgnoresNull(new EncodingImpl(), Encoding::addExtension, hasExtension);
        assertAddIgnoresNull(new MediaTypeImpl(), MediaType::addEncoding, (obj, key) -> obj.getEncoding().containsKey(key));
        assertAddIgnoresNull(new MediaTypeImpl(), MediaType::addExample, (obj, key) -> obj.getExamples().containsKey(key));
        assertAddIgnoresNull(new MediaTypeImpl(), MediaType::addExtension, hasExtension);
        assertAddIgnoresNull(new SchemaImpl(), Schema::addProperty, (obj, key) -> obj.getProperties().containsKey(key));
        assertAddIgnoresNull(new SchemaImpl(), Schema::addExtension, hasExtension);
        assertAddIgnoresNull(new XMLImpl(), XML::addExtension, hasExtension);
        assertAddIgnoresNull(new ParameterImpl(), Parameter::addExample, (obj, key) -> obj.getExamples().containsKey(key));
        assertAddIgnoresNull(new ParameterImpl(), Parameter::addExtension, hasExtension);
        assertAddIgnoresNull(new RequestBodyImpl(), RequestBody::addExtension, hasExtension);
        assertAddIgnoresNull(new APIResponseImpl(), APIResponse::addHeader, (obj, key) -> obj.getHeaders().containsKey(key));
        assertAddIgnoresNull(new APIResponseImpl(), APIResponse::addLink, (obj, key) -> obj.getLinks().containsKey(key));
        assertAddIgnoresNull(new APIResponseImpl(), APIResponse::addExtension, hasExtension);
        assertAddIgnoresNull(new APIResponsesImpl(), APIResponses::addAPIResponse, APIResponses::hasAPIResponse);
        assertAddIgnoresNull(new APIResponsesImpl(), APIResponses::addExtension, hasExtension);
        assertAddIgnoresNull(new OAuthFlowImpl(), OAuthFlow::addExtension, hasExtension);
        assertAddIgnoresNull(new OAuthFlowsImpl(), OAuthFlows::addExtension, hasExtension);
        assertAddIgnoresNull(new SecuritySchemeImpl(), SecurityScheme::addExtension, hasExtension);
        assertAddIgnoresNull(new ServerImpl(), Server::addExtension, hasExtension);
        assertAddIgnoresNull(new ServerVariableImpl(), ServerVariable::addExtension, hasExtension);
        assertAddIgnoresNull(new TagImpl(), Tag::addExtension, hasExtension);
        assertAddIgnoresNull(new ComponentsImpl(), Components::addCallback, (obj, key) -> obj.getCallbacks().containsKey(key));
        assertAddIgnoresNull(new ComponentsImpl(), Components::addExample, (obj, key) -> obj.getExamples().containsKey(key));
        assertAddIgnoresNull(new ComponentsImpl(), Components::addHeader, (obj, key) -> obj.getHeaders().containsKey(key));
        assertAddIgnoresNull(new ComponentsImpl(), Components::addLink, (obj, key) -> obj.getLinks().containsKey(key));
        assertAddIgnoresNull(new ComponentsImpl(), Components::addParameter, (obj, key) -> obj.getParameters().containsKey(key));
        assertAddIgnoresNull(new ComponentsImpl(), Components::addRequestBody, (obj, key) -> obj.getRequestBodies().containsKey(key));
        assertAddIgnoresNull(new ComponentsImpl(), Components::addResponse, (obj, key) -> obj.getResponses().containsKey(key));
        assertAddIgnoresNull(new ComponentsImpl(), Components::addSchema, (obj, key) -> obj.getSchemas().containsKey(key));
        assertAddIgnoresNull(new ComponentsImpl(), Components::addSecurityScheme, (obj, key) -> obj.getSecuritySchemes().containsKey(key));
        assertAddIgnoresNull(new ComponentsImpl(), Components::addExtension, hasExtension);
        assertAddIgnoresNull(new ExternalDocumentationImpl(), ExternalDocumentation::addExtension, hasExtension);
        assertAddIgnoresNull(new OpenAPIImpl(), OpenAPI::addExtension, hasExtension);
        assertAddIgnoresNull(new OperationImpl(), Operation::addCallback, (obj, key) -> obj.getCallbacks().containsKey(key));
        assertAddIgnoresNull(new OperationImpl(), Operation::addExtension, hasExtension);
        assertAddIgnoresNull(new PathItemImpl(), PathItem::addExtension, hasExtension);
        assertAddIgnoresNull(new PathsImpl(), Paths::addPathItem, Paths::hasPathItem);
        assertAddIgnoresNull(new PathsImpl(), Paths::addExtension, hasExtension);
    }

    @Test
    public void ScopesAddScopeDoesAcceptNull() {
        OAuthFlow flow = new OAuthFlowImpl();
        flow.addScope("foo", null);
        assertTrue(flow.getScopes().containsKey("foo"));
    }

    @Test
    public void SecurityRequirementAddSchemePutsEmptyListForNullItem() {
        SecurityRequirement requirement = new SecurityRequirementImpl();
        requirement.addScheme("keyOnly");
        requirement.addScheme("nullItem", (String) null);
        requirement.addScheme("nullList", (List<String>) null);
        List<String> keyOnly = requirement.getScheme("keyOnly");
        assertNotNull(keyOnly);
        assertEquals(0, keyOnly.size());
        List<String> nullItem = requirement.getScheme("nullItem");
        assertNotNull(nullItem);
        assertEquals(0, nullItem.size());
        List<String> nullList = requirement.getScheme("nullList");
        assertNotNull(nullList);
        assertEquals(0, nullList.size());
    }

    private static <T, V> void assertAddIgnoresNull(T modelObject, HasAdd<T, V> addMethod, BiPredicate<? super T, String> containsMethod) {
        String key = "whatever";
        T res = addMethod.add(modelObject, key, null);
        String typeName = modelObject.getClass().getSimpleName();
        assertSame(typeName + " add should return same instance for chaining but doesn't", res, modelObject);
        assertFalse(typeName + " add should not create an entry for key if value is null but does", containsMethod.test(modelObject, key));
    }
}
