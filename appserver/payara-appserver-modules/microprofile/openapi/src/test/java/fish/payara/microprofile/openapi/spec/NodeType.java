/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2019-2023] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.microprofile.openapi.spec;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Node types and their structure as described by Open API Specification
 * {@link https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.1.0.md}.
 */
public enum NodeType implements Iterable<Field> {

    // primitive elements
    string,
    bool,
    number,
    // non primitive elements
    OpenAPI,
    Info,
    Contact,
    License,
    Server,
    ServerVariable,
    Components,
    Paths,
    PathItem,
    Operation,
    ExternalDocumentation,
    Parameter,
    RequestBody,
    MediaType,
    Encoding,
    Responses,
    Response,
    Callback,
    Example,
    Link,
    Header,
    Tag,
    Reference,
    Schema,
    Discriminator,
    XML,
    SecurityScheme,
    OAuthFlows,
    OAuthFlow,
    SecurityRequirement
    ;

    private boolean isExtensible = false;
    private final Map<String, Field> fields = new LinkedHashMap<>();
    private Field lastField;

    @Override
    public Iterator<Field> iterator() {
        return fields.values().iterator();
    }

    public boolean isExtensible() {
        return isExtensible;
    }

    public Field getField(String name) {
        Field field = fields.get(name);
        return field == null && fields.containsKey("*") ? fields.get("*") : field;
    }

    public Stream<Field> fields() {
        return fields.values().stream();
    }

    NodeType addField(String name, NodeType... oneOfTypes) {
        lastField = fields.computeIfAbsent(name, Field::new);
        if (oneOfTypes != null && oneOfTypes.length > 0) {
            lastField.oneOfTypes.addAll(Arrays.asList(oneOfTypes));
        }
        return this;
    }

    NodeType array() {
        lastField.isArray = true;
        return this;
    }

    NodeType map() {
        lastField.isMap = true;
        return this;
    }

    NodeType required() {
        lastField.isRequired = true;
        return this;
    }

    NodeType addPatterndFields(NodeType... oneOfTypes) {
        return addField("*", oneOfTypes);
    }

    NodeType extensible() {
        isExtensible = true;
        return this;
    }

    static {
        OpenAPI
            .addField("openapi", string).required()
            .addField("info", Info).required()
            .addField("servers", Server).array()
            .addField("paths", Paths).required()
            .addField("components", Components)
            .addField("security", SecurityRequirement).array()
            .addField("tags", Tag).array()
            .addField("externalDocs", ExternalDocumentation)
            .extensible();

        Info
            .addField("title", string).required()
            .addField("description", string)
            .addField("termsOfService", string)
            .addField("contact", Contact)
            .addField("license", License)
            .addField("version", string).required()
            .extensible();

        Contact
            .addField("name", string)
            .addField("url", string)
            .addField("email", string)
            .extensible();

        License
            .addField("name", string).required()
            .addField("url", string)
            .extensible();

        Server
            .addField("url", string).required()
            .addField("description", string)
            .addField("variables", ServerVariable).map()
            .extensible();

        ServerVariable
            .addField("enum", string).array()
            .addField("default", string).required()
            .addField("description", string)
            .extensible();

        Components
            .addField("schemas", Schema, Reference).map()
            .addField("responses", Response, Reference).map()
            .addField("parameters", Parameter, Reference).map()
            .addField("examples", Example, Reference).map()
            .addField("requestBodies", RequestBody, Reference).map()
            .addField("headers", Header, Reference).map()
            .addField("securitySchemes", SecurityScheme, Reference).map()
            .addField("links", Link, Reference).map()
            .addField("callbacks", Callback, Reference).map()
            .extensible();

        Paths
            .addPatterndFields(PathItem)
            .extensible();

        PathItem
            .addField("$ref", string)
            .addField("summary", string)
            .addField("description", string)
            .addField("get", Operation)
            .addField("put", Operation)
            .addField("post", Operation)
            .addField("delete", Operation)
            .addField("options", Operation)
            .addField("head", Operation)
            .addField("patch", Operation)
            .addField("trace", Operation)
            .addField("servers", Server).array()
            .addField("parameters", Parameter, Reference).array()
            .extensible();

        Operation
            .addField("tags", string).array()
            .addField("summary", string)
            .addField("description", string)
            .addField("externalDocs", ExternalDocumentation)
            .addField("operationId", string)
            .addField("parameters", Parameter, Reference).array()
            .addField("requestBody", RequestBody, Reference)
            .addField("responses", Responses).required()
            .addField("callbacks", Callback, Reference).map()
            .addField("deprecated", bool)
            .addField("security", SecurityRequirement).array()
            .addField("servers", Server).array()
            .extensible();

        ExternalDocumentation
            .addField("description", string)
            .addField("url", string).required()
            .extensible();

        Parameter
            .addField("name", string).required()
            .addField("in", string).required()
            .addField("description", string)
            .addField("required", bool)
            .addField("deprecated", bool)
            .addField("allowEmptyValue", bool)
            .addField("style", string)
            .addField("explode", bool)
            .addField("allowReserved", bool)
            .addField("schema", Schema, Reference)
            .addField("example")
            .addField("examples", Example, Reference).map()
            .addField("content", MediaType).map()
            .extensible();

        RequestBody
            .addField("description", string)
            .addField("content", MediaType).map().required()
            .addField("required", bool)
            .extensible();

        MediaType
            .addField("schema", Schema)
            .addField("schema", Reference)
            .addField("example")
            .addField("examples", Example, Reference).map()
            .addField("encoding", Encoding).map()
            .extensible();

        Encoding
            .addField("contentType", string)
            .addField("headers", Header, Reference).map()
            .addField("style", string)
            .addField("explode", bool)
            .addField("allowReserved", bool)
            .extensible();

        Responses
            .addField("default", Response, Reference)
            .addPatterndFields(Response, Reference)
            .extensible();

        Response
            .addField("description", string).required()
            .addField("headers", Header, Reference).map()
            .addField("content", MediaType).map()
            .addField("links", Link, Reference).map()
            .extensible();

        Callback
            .addPatterndFields(PathItem)
            .extensible();

        Example
            .addField("summary", string)
            .addField("description", string)
            .addField("value")
            .addField("externalValue", string)
            .extensible();

        Link
            .addField("operationRef", string)
            .addField("operationId", string)
            .addField("parameters").map()
            .addField("requestBody")
            .addField("description", string)
            .addField("server", Server)
            .extensible();

        Header
            .addField("description", string)
            .addField("required", bool)
            .addField("deprecated", bool)
            .addField("allowEmptyValue", bool)
            .addField("style", string)
            .addField("explode", bool)
            .addField("allowReserved", bool)
            .addField("schema", Schema)
            .addField("schema", Reference)
            .addField("example")
            .addField("examples", Example, Reference).map()
            .addField("content", MediaType).map()
            .extensible();

        Tag
            .addField("name", string).required()
            .addField("description", string)
            .addField("externalDocs", ExternalDocumentation)
            .extensible();

        Reference
            .addField("$ref", string).required();

        Schema
            .addField("title", string)
            .addField("multipleOf", number)
            .addField("maximum", number)
            .addField("exclusiveMaximum", bool)
            .addField("minimum", number)
            .addField("exclusiveMinimum", bool)
            .addField("maxLength", number)
            .addField("minLength", number)
            .addField("pattern", string)
            .addField("maxItems", number)
            .addField("minItems", number)
            .addField("uniqueItems", bool)
            .addField("maxProperties", number)
            .addField("minProperties", number)
            .addField("required", string).array()
            .addField("enum").array()
            .addField("type", string)
            .addField("allOf", Schema, Reference).array()
            .addField("oneOf", Schema, Reference).array()
            .addField("anyOf", Schema, Reference).array()
            .addField("not", Schema, Reference)
            .addField("items", Schema, Reference)
            .addField("properties", Schema, Reference).map()
            .addField("additionalProperties", bool, Schema, Reference)
            .addField("description", string)
            .addField("format", string)
            .addField("default")
            .addField("nullable",bool)
            .addField("discriminator", Discriminator)
            .addField("readOnly", bool)
            .addField("writeOnly", bool)
            .addField("xml", XML)
            .addField("externalDocs", ExternalDocumentation)
            .addField("example")
            .addField("deprecated", bool)
            .extensible();

        Discriminator
            .addField("propertyName", string).required()
            .addField("mapping", string).map();

        XML
            .addField("name", string)
            .addField("namespace", string)
            .addField("prefix", string)
            .addField("attribute", bool)
            .addField("wrapped", bool)
            .extensible();

        SecurityScheme
            .addField("type", string).required()
            .addField("description", string)
            .addField("name", string)
            .addField("in", string)
            .addField("scheme", string)
            .addField("bearerFormat", string)
            .addField("flows", OAuthFlows)
            .addField("openIdConnectUrl", string)
            .extensible();

        OAuthFlows
            .addField("implicit", OAuthFlow)
            .addField("password", OAuthFlow)
            .addField("clientCredentials", OAuthFlow)
            .addField("authorizationCode", OAuthFlow)
            .extensible();

        OAuthFlow
            .addField("authorizationUrl", string)
            .addField("tokenUrl", string)
            .addField("refreshUrl", string)
            .addField("scopes", string).map()
            .extensible();

        SecurityRequirement
            .addPatterndFields(string).array();
    }
}