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
package fish.payara.microprofile.openapi.impl.model.headers;

import fish.payara.microprofile.openapi.api.visitor.ApiContext;
import fish.payara.microprofile.openapi.impl.model.ExtensibleImpl;
import fish.payara.microprofile.openapi.impl.model.examples.ExampleImpl;
import fish.payara.microprofile.openapi.impl.model.media.ContentImpl;
import fish.payara.microprofile.openapi.impl.model.media.SchemaImpl;

import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.UNKNOWN_ELEMENT_NAME;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.applyReference;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.createList;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.createMap;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.extractAnnotations;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.mergeProperty;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.readOnlyView;
import java.util.List;
import java.util.Map;
import org.eclipse.microprofile.openapi.models.examples.Example;
import org.eclipse.microprofile.openapi.models.headers.Header;
import org.eclipse.microprofile.openapi.models.media.Content;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.glassfish.hk2.classmodel.reflect.AnnotationModel;
import org.glassfish.hk2.classmodel.reflect.EnumModel;

public class HeaderImpl extends ExtensibleImpl<Header> implements Header {

    private String ref;
    private String description;
    private Boolean required;
    private Boolean deprecated;
    private Boolean allowEmptyValue;
    private Style style;
    private Boolean explode;
    private Schema schema;
    private Map<String, Example> examples = createMap();
    private Object example;
    private Content content = new ContentImpl();

    public static Map<String, Header> createInstances(AnnotationModel annotation, ApiContext context) {
        Map<String, Header> map = createMap();
        List<AnnotationModel> headers = annotation.getValue("headers", List.class);
        if (headers != null) {
            for (AnnotationModel header : headers) {
                String headerName = header.getValue("name", String.class);
                if(headerName == null) {
                    headerName = header.getValue("ref", String.class);
                }
                map.put(
                        headerName,
                        createInstance(header, context)
                );
            }
        }
        return map;
    }

    public static Header createInstance(AnnotationModel annotation, ApiContext context) {
        HeaderImpl from = new HeaderImpl();
        String ref = annotation.getValue("ref", String.class);
        if (ref != null && !ref.isEmpty()) {
            from.setRef(ref);
        }
        from.setExtensions(parseExtensions(annotation));
        from.setDescription(annotation.getValue("description", String.class));
        from.setRequired(annotation.getValue("required", Boolean.class));
        from.setDeprecated(annotation.getValue("deprecated", Boolean.class));
        from.setAllowEmptyValue(annotation.getValue("allowEmptyValue", Boolean.class));
        EnumModel styleEnum = annotation.getValue("style", EnumModel.class);
        if (styleEnum != null) {
            from.setStyle(Header.Style.valueOf(styleEnum.getValue()));
        }
        from.setExplode(annotation.getValue("explode", Boolean.class));
        AnnotationModel schemaAnnotation = annotation.getValue("schema", AnnotationModel.class);
        if (schemaAnnotation != null) {
            Boolean hidden = schemaAnnotation.getValue("hidden", Boolean.class);
            if (hidden == null || !hidden) {
                from.setSchema(SchemaImpl.createInstance(schemaAnnotation, context));
            }
        }
        extractAnnotations(annotation, context, "examples", "name", ExampleImpl::createInstance, from::addExample);
        from.setExample(annotation.getValue("example", Object.class));

        final List<ContentImpl> contents = createList();
        extractAnnotations(annotation, context, "content", ContentImpl::createInstance, contents::add);
        for (ContentImpl content : contents) {
            content.getMediaTypes().forEach(from.content::addMediaType);
        }

        return from;
    }

    @Override
    public String getRef() {
        return ref;
    }

    @Override
    public void setRef(String ref) {
        if (ref != null && !ref.contains(".") && !ref.contains("/")) {
            ref = "#/components/headers/" + ref;
        }
        this.ref = ref;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public Boolean getRequired() {
        return required;
    }

    @Override
    public void setRequired(Boolean required) {
        this.required = required;
    }

    @Override
    public Boolean getDeprecated() {
        return deprecated;
    }

    @Override
    public void setDeprecated(Boolean deprecated) {
        this.deprecated = deprecated;
    }

    @Override
    public Boolean getAllowEmptyValue() {
        return allowEmptyValue;
    }

    @Override
    public void setAllowEmptyValue(Boolean allowEmptyValue) {
        this.allowEmptyValue = allowEmptyValue;
    }

    @Override
    public Style getStyle() {
        return style;
    }

    @Override
    public void setStyle(Style style) {
        this.style = style;
    }

    @Override
    public Boolean getExplode() {
        return explode;
    }

    @Override
    public void setExplode(Boolean explode) {
        this.explode = explode;
    }

    @Override
    public Schema getSchema() {
        return schema;
    }

    @Override
    public void setSchema(Schema schema) {
        this.schema = schema;
    }

    @Override
    public Map<String, Example> getExamples() {
        return readOnlyView(examples);
    }

    @Override
    public void setExamples(Map<String, Example> examples) {
        this.examples = createMap(examples);
    }

    @Override
    public Header addExample(String key, Example examplesItem) {
        if (examplesItem != null) {
            if (this.examples == null) {
                this.examples = createMap();
            }
            this.examples.put(key, examplesItem);
        }
        return this;
    }

    @Override
    public void removeExample(String key) {
        if (examples != null) {
            examples.remove(key);
        }
    }

    @Override
    public Object getExample() {
        return example;
    }

    @Override
    public void setExample(Object example) {
        this.example = example;
    }

    @Override
    public Content getContent() {
        return content;
    }

    @Override
    public void setContent(Content content) {
        this.content = content;
    }

    public static void merge(Header from, Header to,
            boolean override, ApiContext context) {
        if (from == null) {
            return;
        }
        if (from.getRef() != null && !from.getRef().isEmpty()) {
            applyReference(to, from.getRef());
            return;
        }
        to.setExtensions(mergeProperty(to.getExtensions(), from.getExtensions(), override));
        to.setDescription(mergeProperty(to.getDescription(), from.getDescription(), override));
        to.setRequired(mergeProperty(to.getRequired(), from.getRequired(), override));
        to.setDeprecated(mergeProperty(to.getDeprecated(), from.getDeprecated(), override));
        to.setAllowEmptyValue(mergeProperty(to.getAllowEmptyValue(), from.getAllowEmptyValue(), override));
        to.setStyle(Style.SIMPLE);
        to.setExplode(mergeProperty(to.getExplode(), from.getExplode(), override));
        if (from.getSchema() != null) {
            if (to.getSchema() == null) {
                to.setSchema(new SchemaImpl());
            }
            SchemaImpl.merge(from.getSchema(), to.getSchema(), override, context);
        }
        to.setExample(mergeProperty(to.getExample(), from.getExample(), override));
        if (from.getExamples() != null) {
            for (String exampleName : from.getExamples().keySet()) {
                if (exampleName != null) {
                    Example example = new ExampleImpl();
                    ExampleImpl.merge(from.getExamples().get(exampleName), example, override);
                    to.addExample(exampleName, example);
                }
            }
        }
        if (from.getContent() != null) {
            if (to.getContent() == null) {
                to.setContent(new ContentImpl());
            }
            ContentImpl.merge((ContentImpl)from.getContent(), to.getContent(), override, context);
        }
    }

    public static void merge(
            String headerName,
            Header header,
            Map<String, Header> headers,
            boolean override,
            ApiContext context) {

        if (header == null) {
            return;
        }

        // Get the header name
        if (headerName == null || headerName.isEmpty()) {
            headerName = UNKNOWN_ELEMENT_NAME;
        }

        // Get or create the header
        Header model = headers.getOrDefault(headerName, new HeaderImpl());
        headers.put(headerName, model);

        // Merge the annotation
        merge(header, model, override, context);

        // If the merged annotation has a reference, set the name to the reference
        if (model.getRef() != null) {
            headers.remove(headerName);
            headers.put(model.getRef().split("/")[3], model);
        }
    }

}
