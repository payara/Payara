package fish.payara.microprofile.openapi.impl.rest.app.provider;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;

import org.eclipse.microprofile.openapi.models.Components;
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

import fish.payara.microprofile.openapi.impl.model.ComponentsImpl;
import fish.payara.microprofile.openapi.impl.model.ExternalDocumentationImpl;
import fish.payara.microprofile.openapi.impl.model.OpenAPIImpl;
import fish.payara.microprofile.openapi.impl.model.OperationImpl;
import fish.payara.microprofile.openapi.impl.model.PathItemImpl;
import fish.payara.microprofile.openapi.impl.model.PathsImpl;
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
import fish.payara.microprofile.openapi.impl.model.util.ExtensionsMixin;

public final class ObjectMapperFactory {

    public static ObjectMapper createJson() {
        return create(new JsonFactory());
    }

    public static ObjectMapper createYaml() {
        YAMLFactory factory = new YAMLFactory();
        factory.disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER);
        factory.enable(YAMLGenerator.Feature.MINIMIZE_QUOTES);
        factory.enable(YAMLGenerator.Feature.SPLIT_LINES);
        factory.enable(YAMLGenerator.Feature.ALWAYS_QUOTE_NUMBERS_AS_STRINGS);
        return create(factory);
    }

    public static ObjectMapper create(JsonFactory factory) {
        ObjectMapper mapper = new ObjectMapper(factory);
        mapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
        mapper.setSerializationInclusion(Include.NON_DEFAULT);
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.configure(SerializationFeature.WRITE_ENUMS_USING_TO_STRING, true);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // Create mapping module
        SimpleModule module = new SimpleModule();

        module.addAbstractTypeMapping(Callback.class, CallbackImpl.class);
        module.addAbstractTypeMapping(Example.class, ExampleImpl.class);
        module.addAbstractTypeMapping(Header.class, HeaderImpl.class);
        module.addAbstractTypeMapping(Info.class, InfoImpl.class);
        module.addAbstractTypeMapping(Contact.class, ContactImpl.class);
        module.addAbstractTypeMapping(License.class, LicenseImpl.class);
        module.addAbstractTypeMapping(Link.class, LinkImpl.class);
        module.addAbstractTypeMapping(Content.class, ContentImpl.class);
        module.addAbstractTypeMapping(Discriminator.class, DiscriminatorImpl.class);
        module.addAbstractTypeMapping(Encoding.class, EncodingImpl.class);
        module.addAbstractTypeMapping(MediaType.class, MediaTypeImpl.class);
        module.addAbstractTypeMapping(Schema.class, SchemaImpl.class);
        module.addAbstractTypeMapping(XML.class, XMLImpl.class);
        module.addAbstractTypeMapping(Parameter.class, ParameterImpl.class);
        module.addAbstractTypeMapping(RequestBody.class, RequestBodyImpl.class);
        module.addAbstractTypeMapping(APIResponse.class, APIResponseImpl.class);
        module.addAbstractTypeMapping(APIResponses.class, APIResponsesImpl.class);
        module.addAbstractTypeMapping(OAuthFlow.class, OAuthFlowImpl.class);
        module.addAbstractTypeMapping(OAuthFlows.class, OAuthFlowsImpl.class);
        module.addAbstractTypeMapping(Scopes.class, ScopesImpl.class);
        module.addAbstractTypeMapping(SecurityRequirement.class, SecurityRequirementImpl.class);
        module.addAbstractTypeMapping(SecurityScheme.class, SecuritySchemeImpl.class);
        module.addAbstractTypeMapping(Server.class, ServerImpl.class);
        module.addAbstractTypeMapping(ServerVariable.class, ServerVariableImpl.class);
        module.addAbstractTypeMapping(ServerVariables.class, ServerVariablesImpl.class);
        module.addAbstractTypeMapping(Tag.class, TagImpl.class);
        module.addAbstractTypeMapping(Components.class, ComponentsImpl.class);
        module.addAbstractTypeMapping(ExternalDocumentation.class, ExternalDocumentationImpl.class);
        module.addAbstractTypeMapping(OpenAPI.class, OpenAPIImpl.class);
        module.addAbstractTypeMapping(Operation.class, OperationImpl.class);
        module.addAbstractTypeMapping(PathItem.class, PathItemImpl.class);
        module.addAbstractTypeMapping(Paths.class, PathsImpl.class);

        List<Class<?>> mixinTargets = Arrays.asList(APIResponse.class, Callback.class, Components.class, Contact.class,
                Encoding.class, Example.class, ExternalDocumentation.class, Header.class, Info.class, License.class,
                Link.class, MediaType.class, OAuthFlow.class, OAuthFlows.class, OpenAPI.class, Operation.class,
                Parameter.class, PathItem.class, Paths.class, RequestBody.class, Scopes.class, SecurityScheme.class,
                Server.class, ServerVariable.class, ServerVariables.class, Tag.class, XML.class, Schema.class);
        mapper.setMixIns(
                mixinTargets.stream().collect(Collectors.toMap(Function.identity(), c -> ExtensionsMixin.class)));

        mapper.registerModule(module);

        return mapper;
    }

}