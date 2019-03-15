package fish.payara.microprofile.openapi.impl.model;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

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
import org.eclipse.microprofile.openapi.models.security.Scopes;
import org.eclipse.microprofile.openapi.models.security.SecurityScheme;
import org.eclipse.microprofile.openapi.models.servers.Server;
import org.eclipse.microprofile.openapi.models.servers.ServerVariable;
import org.eclipse.microprofile.openapi.models.servers.ServerVariables;
import org.eclipse.microprofile.openapi.models.tags.Tag;
import org.junit.Test;

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
import fish.payara.microprofile.openapi.impl.model.security.SecuritySchemeImpl;
import fish.payara.microprofile.openapi.impl.model.servers.ServerImpl;
import fish.payara.microprofile.openapi.impl.model.servers.ServerVariableImpl;
import fish.payara.microprofile.openapi.impl.model.servers.ServerVariablesImpl;
import fish.payara.microprofile.openapi.impl.model.tags.TagImpl;

public class ModelInvariantsTest {

    interface HasAdd<T, V> {

        T add(T obj, String key, V value);
    }

    @Test
    public void addKeyValueIgnoresNull() {
        BiPredicate<Extensible<?>, String> hasExtension = (obj, key) -> obj.getExtensions().containsKey(key);
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
        assertAddIgnoresNull(new APIResponsesImpl(), APIResponses::addApiResponse, APIResponses::hasAPIResponse);
        assertAddIgnoresNull(new APIResponsesImpl(), APIResponses::addAPIResponse, APIResponses::hasAPIResponse);
        assertAddIgnoresNull(new APIResponsesImpl(), APIResponses::addExtension, hasExtension);
        assertAddIgnoresNull(new OAuthFlowImpl(), OAuthFlow::addExtension, hasExtension);
        assertAddIgnoresNull(new OAuthFlowsImpl(), OAuthFlows::addExtension, hasExtension);
        assertAddIgnoresNull(new ScopesImpl(), Scopes::addExtension, hasExtension);
        assertAddIgnoresNull(new SecuritySchemeImpl(), SecurityScheme::addExtension, hasExtension);
        assertAddIgnoresNull(new ServerImpl(), Server::addExtension, hasExtension);
        assertAddIgnoresNull(new ServerVariableImpl(), ServerVariable::addExtension, hasExtension);
        assertAddIgnoresNull(new ServerVariablesImpl(), ServerVariables::addServerVariable, ServerVariables::hasServerVariable);
        assertAddIgnoresNull(new ServerVariablesImpl(), ServerVariables::addExtension, hasExtension);
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
        Scopes scopes = new ScopesImpl().addScope("foo", null);
        assertTrue(scopes.hasScope("foo"));
    }

    private static <T, V> void assertAddIgnoresNull(T modelObject, HasAdd<T, V> addMethod, BiPredicate<? super T, String> containsMethod) {
        String key = "whatever";
        T res = addMethod.add(modelObject, key, null);
        String typeName = modelObject.getClass().getSimpleName();
        assertSame(typeName + " add should return same instance for chaining but doesn't", res, modelObject);
        assertFalse(typeName + " add should not create an entry for key if value is null but does", containsMethod.test(modelObject, key));
    }
}
