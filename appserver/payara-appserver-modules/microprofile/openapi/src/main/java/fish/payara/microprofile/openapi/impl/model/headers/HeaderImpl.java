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
package fish.payara.microprofile.openapi.impl.model.headers;

import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.UNKNOWN_ELEMENT_NAME;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.applyReference;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.isAnnotationNull;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.mergeProperty;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.microprofile.openapi.models.examples.Example;
import org.eclipse.microprofile.openapi.models.headers.Header;
import org.eclipse.microprofile.openapi.models.media.Content;
import org.eclipse.microprofile.openapi.models.media.Schema;

import fish.payara.microprofile.openapi.impl.model.ExtensibleImpl;
import fish.payara.microprofile.openapi.impl.model.media.SchemaImpl;

public class HeaderImpl extends ExtensibleImpl<Header> implements Header {

    protected String ref;
    protected String description;
    protected Boolean required;
    protected Boolean deprecated;
    protected Boolean allowEmptyValue;
    protected Style style;
    protected Boolean explode;
    protected Schema schema;
    protected Map<String, Example> examples = new HashMap<>();
    protected Object example;
    protected Content content;

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
        return examples;
    }

    @Override
    public void setExamples(Map<String, Example> examples) {
        this.examples = examples;
    }

    @Override
    public Header addExample(String key, Example examplesItem) {
        if (examplesItem != null) {
            this.examples.put(key, examplesItem);
        }
        return this;
    }

    @Override
    public void removeExample(String key) {
        this.examples.remove(key);
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

    public static void merge(org.eclipse.microprofile.openapi.annotations.headers.Header from, Header to,
            boolean override, Map<String, Schema> currentSchemas) {
        if (isAnnotationNull(from)) {
            return;
        }
        if (from.ref() != null && !from.ref().isEmpty()) {
            applyReference(to, from.ref());
            return;
        }
        to.setDescription(mergeProperty(to.getDescription(), from.description(), override));
        to.setRequired(mergeProperty(to.getRequired(), from.required(), override));
        to.setAllowEmptyValue(mergeProperty(to.getAllowEmptyValue(), from.allowEmptyValue(), override));
        to.setDeprecated(mergeProperty(to.getDeprecated(), from.deprecated(), override));
        if (!isAnnotationNull(from.schema())) {
            if (to.getSchema() == null) {
                to.setSchema(new SchemaImpl());
            }
            SchemaImpl.merge(from.schema(), to.getSchema(), override, currentSchemas);
        }
        to.setStyle(Style.SIMPLE);
    }

    public static void merge(org.eclipse.microprofile.openapi.annotations.headers.Header header,
            Map<String, Header> headers, boolean override, Map<String, Schema> currentSchemas) {
        if (isAnnotationNull(header)) {
            return;
        }

        // Get the header name
        String headerName = header.name();
        if (header.name() == null || header.name().isEmpty()) {
            headerName = UNKNOWN_ELEMENT_NAME;
        }

        // Get or create the header
        Header model = headers.getOrDefault(headerName, new HeaderImpl());
        headers.put(headerName, model);

        // Merge the annotation
        merge(header, model, override, currentSchemas);

        // If the merged annotation has a reference, set the name to the reference
        if (model.getRef() != null) {
            headers.remove(headerName);
            headers.put(model.getRef().split("/")[3], model);
        }
    }

}
