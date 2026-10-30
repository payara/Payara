/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2018-2023] Payara Foundation and/or its affiliates. All rights reserved.
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
import static java.util.Collections.unmodifiableMap;
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
import org.eclipse.microprofile.openapi.models.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.models.security.SecurityScheme;
import org.eclipse.microprofile.openapi.models.servers.Server;
import org.eclipse.microprofile.openapi.models.servers.ServerVariable;
import org.eclipse.microprofile.openapi.models.tags.Tag;
import org.eclipse.microprofile.openapi.spi.OASFactoryResolver;

public class OASFactoryResolverImpl extends OASFactoryResolver {

    /**
     * A map of each OpenAPI model element to the class that models it.
     */
    public static final Map<Class<? extends Constructible>, Class<? extends Constructible>> MODEL_MAP;
    static {
        Map<Class<? extends Constructible>, Class<? extends Constructible>> map = new HashMap<>();
        map.put(Components.class, ComponentsImpl.class);
        map.put(ExternalDocumentation.class, ExternalDocumentationImpl.class);
        map.put(OpenAPI.class, OpenAPIImpl.class);
        map.put(Operation.class, OperationImpl.class);
        map.put(PathItem.class, PathItemImpl.class);
        map.put(Paths.class, PathsImpl.class);
        map.put(Callback.class, CallbackImpl.class);
        map.put(Example.class, ExampleImpl.class);
        map.put(Header.class, HeaderImpl.class);
        map.put(Contact.class, ContactImpl.class);
        map.put(Info.class, InfoImpl.class);
        map.put(License.class, LicenseImpl.class);
        map.put(Link.class, LinkImpl.class);
        map.put(Content.class, ContentImpl.class);
        map.put(Discriminator.class, DiscriminatorImpl.class);
        map.put(Encoding.class, EncodingImpl.class);
        map.put(MediaType.class, MediaTypeImpl.class);
        map.put(Schema.class, SchemaImpl.class);
        map.put(XML.class, XMLImpl.class);
        map.put(Parameter.class, ParameterImpl.class);
        map.put(RequestBody.class, RequestBodyImpl.class);
        map.put(APIResponse.class, APIResponseImpl.class);
        map.put(APIResponses.class, APIResponsesImpl.class);
        map.put(OAuthFlow.class, OAuthFlowImpl.class);
        map.put(OAuthFlows.class, OAuthFlowsImpl.class);
        map.put(SecurityRequirement.class, SecurityRequirementImpl.class);
        map.put(SecurityScheme.class, SecuritySchemeImpl.class);
        map.put(Server.class, ServerImpl.class);
        map.put(ServerVariable.class, ServerVariableImpl.class);
        map.put(Tag.class, TagImpl.class);
        MODEL_MAP = unmodifiableMap(map);
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
            return (T) implClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new OpenAPIClassCreationException(e);
        }
    }

    private class OpenAPIClassCreationException extends RuntimeException {

        private static final long serialVersionUID = 7668110028310822354L;

        OpenAPIClassCreationException(Exception ex) {
            super(ex);
        }
    }

}
