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
package fish.payara.microprofile.openapi.impl.model.parameters;

import fish.payara.microprofile.openapi.api.visitor.ApiContext;
import fish.payara.microprofile.openapi.impl.model.ExtensibleImpl;
import fish.payara.microprofile.openapi.impl.model.examples.ExampleImpl;
import fish.payara.microprofile.openapi.impl.model.media.ContentImpl;
import fish.payara.microprofile.openapi.impl.model.media.SchemaImpl;

import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.applyReference;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.createList;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.createMap;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.extractAnnotations;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.mergeProperty;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.readOnlyView;

import java.util.List;
import java.util.Map;
import org.eclipse.microprofile.openapi.models.examples.Example;
import org.eclipse.microprofile.openapi.models.media.Content;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.parameters.Parameter;
import org.glassfish.hk2.classmodel.reflect.AnnotationModel;
import org.glassfish.hk2.classmodel.reflect.EnumModel;

public class ParameterImpl extends ExtensibleImpl<Parameter> implements Parameter {

    private String name;
    private In in;
    private String description;
    private Boolean required;
    private Boolean deprecated;
    private Boolean allowEmptyValue;
    private String ref;

    private Style style;
    private Boolean explode;
    private Boolean allowReserved;
    private Schema schema;
    private Map<String, Example> examples = createMap();
    private Object example;
    private Content content = new ContentImpl();
    private List<ContentImpl> contents = createList();

    public static Parameter createInstance(AnnotationModel annotation, ApiContext context) {
        ParameterImpl from = new ParameterImpl();
        from.setName(annotation.getValue("name", String.class));
        EnumModel inEnum = annotation.getValue("in", EnumModel.class);
        if (inEnum != null) {
            from.setIn(In.valueOf(inEnum.getValue()));
        }
        from.setDescription(annotation.getValue("description", String.class));
        from.setExtensions(parseExtensions(annotation));
        from.setRequired(annotation.getValue("required", Boolean.class));
        from.setDeprecated(annotation.getValue("deprecated", Boolean.class));
        from.setAllowEmptyValue(annotation.getValue("allowEmptyValue", Boolean.class));
        String ref = annotation.getValue("ref", String.class);
        if (ref != null && !ref.isEmpty()) {
            from.setRef(ref);
        }
        EnumModel styleEnum = annotation.getValue("style", EnumModel.class);
        if (styleEnum != null) {
            from.setStyle(Style.valueOf(styleEnum.getValue()));
        }
        EnumModel explodeEnum = annotation.getValue("explode", EnumModel.class);
        if (explodeEnum != null) {
            from.setExplode("TRUE".equals(explodeEnum.getValue()));
        }
        from.setAllowReserved(annotation.getValue("allowReserved", Boolean.class));
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
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public In getIn() {
        return in;
    }

    @Override
    public void setIn(In in) {
        this.in = in;
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
    public Boolean getAllowReserved() {
        return allowReserved;
    }

    @Override
    public void setAllowReserved(Boolean allowReserved) {
        this.allowReserved = allowReserved;
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
        if (examples == null) {
            this.examples = null;
        } else {
            this.examples = createMap(examples);
        }
    }

    @Override
    public Parameter addExample(String key, Example example) {
        if (example != null) {
            if (this.examples == null) {
                this.examples = createMap();
            }
            this.examples.put(key, example);
        }
        return this;
    }

    @Override
    public void removeExample(String key) {
        if (this.examples != null) {
            this.examples.remove(key);
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

    public List<ContentImpl> getContents() {
        return readOnlyView(contents);
    }

    public void setContents(List<ContentImpl> contents) {
        this.contents = createList(contents);
    }

    @Override
    public String getRef() {
        return ref;
    }

    @Override
    public void setRef(String ref) {
        if (ref != null && !ref.contains(".") && !ref.contains("/")) {
            ref = "#/components/parameters/" + ref;
        }
        this.ref = ref;
    }

    public static void merge(Parameter from, Parameter to,
            boolean override, ApiContext context) {
        if (from == null) {
            return;
        }
        if (from.getRef() != null && !from.getRef().isEmpty()) {
            applyReference(to, from.getRef());
            return;
        }
        to.setName(mergeProperty(to.getName(), from.getName(), override));
        to.setDescription(mergeProperty(to.getDescription(), from.getDescription(), override));
        to.setExtensions(mergeProperty(to.getExtensions(), from.getExtensions(), override));
        if (from.getIn()!= null) {
            to.setIn(mergeProperty(to.getIn(),from.getIn(), override));
        }
        to.setRequired(mergeProperty(to.getRequired(), from.getRequired(), override));
        to.setDeprecated(mergeProperty(to.getDeprecated(), from.getDeprecated(), override));
        to.setAllowEmptyValue(mergeProperty(to.getAllowEmptyValue(), from.getAllowEmptyValue(), override));
        if (from.getStyle() != null){
            to.setStyle(mergeProperty(to.getStyle(), from.getStyle(), override));
        }
        if (from.getExplode() != null) {
            to.setExplode(mergeProperty(to.getExplode(), false, override));
        }
        to.setAllowReserved(mergeProperty(to.getAllowReserved(), from.getAllowReserved(), override));
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
        if (from instanceof ParameterImpl) {
            ParameterImpl fromImpl = (ParameterImpl)from;
            if (fromImpl.getContents() != null) {
                if (to.getContent() == null) {
                    to.setContent(new ContentImpl());
                }
                for (ContentImpl content : fromImpl.getContents()) {
                    ContentImpl.merge(content, to.getContent(), override, context);
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

}
