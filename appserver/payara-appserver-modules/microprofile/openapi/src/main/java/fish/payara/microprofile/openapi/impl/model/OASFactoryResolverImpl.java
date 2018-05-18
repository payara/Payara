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
package fish.payara.microprofile.openapi.impl.model;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.microprofile.openapi.models.Components;
import org.eclipse.microprofile.openapi.models.Constructible;
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
import org.eclipse.microprofile.openapi.models.security.Scopes;
import org.eclipse.microprofile.openapi.models.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.models.security.SecurityScheme;
import org.eclipse.microprofile.openapi.models.servers.Server;
import org.eclipse.microprofile.openapi.models.servers.ServerVariable;
import org.eclipse.microprofile.openapi.models.servers.ServerVariables;
import org.eclipse.microprofile.openapi.models.tags.Tag;
import org.eclipse.microprofile.openapi.spi.OASFactoryResolver;

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
import fish.payara.microprofile.openapi.impl.model.security.ScopesImpl;
import fish.payara.microprofile.openapi.impl.model.security.SecurityRequirementImpl;
import fish.payara.microprofile.openapi.impl.model.security.SecuritySchemeImpl;
import fish.payara.microprofile.openapi.impl.model.servers.ServerImpl;
import fish.payara.microprofile.openapi.impl.model.servers.ServerVariableImpl;
import fish.payara.microprofile.openapi.impl.model.servers.ServerVariablesImpl;
import fish.payara.microprofile.openapi.impl.model.tags.TagImpl;

public class OASFactoryResolverImpl extends OASFactoryResolver {

    public static final Map<Class<? extends Constructible>, Class<? extends Constructible>> MODEL_MAP = new HashMap<>();
    static {
        MODEL_MAP.put(Components.class, ComponentsImpl.class);
        MODEL_MAP.put(ExternalDocumentation.class, ExternalDocumentationImpl.class);
        MODEL_MAP.put(OpenAPI.class, OpenAPIImpl.class);
        MODEL_MAP.put(Operation.class, OperationImpl.class);
        MODEL_MAP.put(PathItem.class, PathItemImpl.class);
        MODEL_MAP.put(Paths.class, PathsImpl.class);
        MODEL_MAP.put(Callback.class, CallbackImpl.class);
        MODEL_MAP.put(Example.class, ExampleImpl.class);
        MODEL_MAP.put(Header.class, HeaderImpl.class);
        MODEL_MAP.put(Contact.class, ContactImpl.class);
        MODEL_MAP.put(Info.class, InfoImpl.class);
        MODEL_MAP.put(License.class, LicenseImpl.class);
        MODEL_MAP.put(Link.class, LinkImpl.class);
        MODEL_MAP.put(Content.class, ContentImpl.class);
        MODEL_MAP.put(Discriminator.class, DiscriminatorImpl.class);
        MODEL_MAP.put(Encoding.class, EncodingImpl.class);
        MODEL_MAP.put(MediaType.class, MediaTypeImpl.class);
        MODEL_MAP.put(Schema.class, SchemaImpl.class);
        MODEL_MAP.put(XML.class, XMLImpl.class);
        MODEL_MAP.put(Parameter.class, ParameterImpl.class);
        MODEL_MAP.put(RequestBody.class, RequestBodyImpl.class);
        MODEL_MAP.put(APIResponse.class, APIResponseImpl.class);
        MODEL_MAP.put(APIResponses.class, APIResponsesImpl.class);
        MODEL_MAP.put(OAuthFlow.class, OAuthFlowImpl.class);
        MODEL_MAP.put(OAuthFlows.class, OAuthFlowsImpl.class);
        MODEL_MAP.put(Scopes.class, ScopesImpl.class);
        MODEL_MAP.put(SecurityRequirement.class, SecurityRequirementImpl.class);
        MODEL_MAP.put(SecurityScheme.class, SecuritySchemeImpl.class);
        MODEL_MAP.put(Server.class, ServerImpl.class);
        MODEL_MAP.put(ServerVariable.class, ServerVariableImpl.class);
        MODEL_MAP.put(ServerVariables.class, ServerVariablesImpl.class);
        MODEL_MAP.put(Tag.class, TagImpl.class);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Constructible> T createObject(Class<T> clazz) {
        // Throw a null pointer exception if the class is null
        if (clazz == null) {
            throw new NullPointerException("Cannot create an object from a null class.");
        }

        // Get the implementation class
        Class<? extends Constructible> implClass = MODEL_MAP.get(clazz);

        // If there is no implementation, throw an exception
        if (implClass == null) {
            throw new IllegalArgumentException(clazz.getName());
        }

        // Return a new instance. Shouldn't throw an exception.
        try {
            return (T) implClass.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
