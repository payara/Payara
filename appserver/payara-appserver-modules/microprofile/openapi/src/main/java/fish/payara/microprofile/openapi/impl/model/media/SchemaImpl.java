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
package fish.payara.microprofile.openapi.impl.model.media;

import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.applyReference;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.isAnnotationNull;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.mergeProperty;
import static java.util.logging.Level.WARNING;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.eclipse.microprofile.openapi.annotations.media.DiscriminatorMapping;
import org.eclipse.microprofile.openapi.models.ExternalDocumentation;
import org.eclipse.microprofile.openapi.models.media.Discriminator;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.media.XML;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import fish.payara.microprofile.openapi.impl.model.ExtensibleImpl;
import fish.payara.microprofile.openapi.impl.model.ExternalDocumentationImpl;
import fish.payara.microprofile.openapi.impl.model.util.ModelUtils;

public class SchemaImpl extends ExtensibleImpl<Schema> implements Schema {

    private static final Logger LOGGER = Logger.getLogger(SchemaImpl.class.getName());

    private Object defaultValue;

    private String title;
    private BigDecimal multipleOf;
    private BigDecimal maximum;
    private Boolean exclusiveMaximum;
    private BigDecimal minimum;
    private Boolean exclusiveMinimum;
    private Integer maxLength;
    private Integer minLength;
    private String pattern;
    private Integer maxItems;
    private Integer minItems;
    private Boolean uniqueItems;
    private Integer maxProperties;
    private Integer minProperties;
    private List<String> required = new ArrayList<>();
    private SchemaType type;
    private Schema not;
    private Map<String, Schema> properties = new HashMap<>();
    private String description;
    private String format;
    private String ref;
    private Boolean nullable;
    private Boolean readOnly;
    private Boolean writeOnly;
    private Object example;
    private ExternalDocumentation externalDocs;
    private Boolean deprecated;
    private XML xml;
    private List<Object> enumeration = new ArrayList<>();
    private Discriminator discriminator;

    private List<Schema> anyOf = new ArrayList<>();
    private List<Schema> allOf = new ArrayList<>();
    private List<Schema> oneOf = new ArrayList<>();

    private Object additionalProperties;
    private Schema items;

    @Override
    public Discriminator getDiscriminator() {
        return discriminator;
    }

    @Override
    public void setDiscriminator(Discriminator discriminator) {
        this.discriminator = discriminator;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public Object getDefaultValue() {
        return defaultValue;
    }

    @Override
    public void setDefaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
    }

    @Override
    public List<Object> getEnumeration() {
        return enumeration;
    }

    @Override
    public void setEnumeration(List<Object> enumeration) {
        this.enumeration = enumeration;
    }

    @Override
    public Schema addEnumeration(Object enumerationItem) {
        if (enumerationItem != null) {
            this.enumeration.add(enumerationItem);
        }
        return this;
    }

    @Override
    public void removeEnumeration(Object enumeration) {
        this.enumeration.remove(enumeration);
    }

    @Override
    public BigDecimal getMultipleOf() {
        return multipleOf;
    }

    @Override
    public void setMultipleOf(BigDecimal multipleOf) {
        this.multipleOf = multipleOf;
    }

    @Override
    public BigDecimal getMaximum() {
        return maximum;
    }

    @Override
    public void setMaximum(BigDecimal maximum) {
        this.maximum = maximum;
    }

    @Override
    public Boolean getExclusiveMaximum() {
        return exclusiveMaximum;
    }

    @Override
    public void setExclusiveMaximum(Boolean exclusiveMaximum) {
        this.exclusiveMaximum = exclusiveMaximum;
    }

    @Override
    public BigDecimal getMinimum() {
        return minimum;
    }

    @Override
    public void setMinimum(BigDecimal minimum) {
        this.minimum = minimum;
    }

    @Override
    public Boolean getExclusiveMinimum() {
        return exclusiveMinimum;
    }

    @Override
    public void setExclusiveMinimum(Boolean exclusiveMinimum) {
        this.exclusiveMinimum = exclusiveMinimum;
    }

    @Override
    public Integer getMaxLength() {
        return maxLength;
    }

    @Override
    public void setMaxLength(Integer maxLength) {
        this.maxLength = maxLength;
    }

    @Override
    public Integer getMinLength() {
        return minLength;
    }

    @Override
    public void setMinLength(Integer minLength) {
        this.minLength = minLength;
    }

    @Override
    public String getPattern() {
        return pattern;
    }

    @Override
    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    @Override
    public Integer getMaxItems() {
        return maxItems;
    }

    @Override
    public void setMaxItems(Integer maxItems) {
        this.maxItems = maxItems;
    }

    @Override
    public Integer getMinItems() {
        return minItems;
    }

    @Override
    public void setMinItems(Integer minItems) {
        this.minItems = minItems;
    }

    @Override
    public Boolean getUniqueItems() {
        return uniqueItems;
    }

    @Override
    public void setUniqueItems(Boolean uniqueItems) {
        this.uniqueItems = uniqueItems;
    }

    @Override
    public Integer getMaxProperties() {
        return maxProperties;
    }

    @Override
    public void setMaxProperties(Integer maxProperties) {
        this.maxProperties = maxProperties;
    }

    @Override
    public Integer getMinProperties() {
        return minProperties;
    }

    @Override
    public void setMinProperties(Integer minProperties) {
        this.minProperties = minProperties;
    }

    @Override
    public List<String> getRequired() {
        return required;
    }

    @Override
    public void setRequired(List<String> required) {
        this.required = required;
    }

    @Override
    public Schema addRequired(String requiredItem) {
        this.required.add(requiredItem);
        Collections.sort(required);
        return this;
    }

    @Override
    public void removeRequired(String required) {
        this.required.remove(required);
    }

    @Override
    public SchemaType getType() {
        return type;
    }

    @Override
    public void setType(SchemaType type) {
        this.type = type;
    }

    @Override
    public Schema getNot() {
        return not;
    }

    @Override
    public void setNot(Schema not) {
        this.not = not;
    }

    @Override
    public Map<String, Schema> getProperties() {
        return properties;
    }

    @Override
    public void setProperties(Map<String, Schema> properties) {
        this.properties = properties;
    }

    @Override
    public Schema addProperty(String key, Schema propertiesItem) {
        if (propertiesItem != null) {
            this.properties.put(key, propertiesItem);
        }
        return this;
    }

    @Override
    public void removeProperty(String key) {
        this.properties.remove(key);
    }

    @JsonProperty("additionalProperties")
    @Override
    public Object getAdditionalProperties() {
        return additionalProperties;
    }

    @Override
    public void setAdditionalProperties(Schema additionalProperties) {
        this.additionalProperties = additionalProperties;
    }

    @JsonIgnore
    @Override
    public Schema getAdditionalPropertiesSchema() {
        return additionalProperties instanceof Schema ? (Schema) additionalProperties : null;
    }

    @JsonIgnore
    @Override
    public Boolean getAdditionalPropertiesBoolean() {
        return additionalProperties instanceof Boolean ? (Boolean)additionalProperties : null;
    }

    @Override
    public void setAdditionalPropertiesSchema(Schema additionalProperties) {
        this.additionalProperties = additionalProperties;
    }

    @Override
    public void setAdditionalPropertiesBoolean(Boolean additionalProperties) {
        this.additionalProperties = additionalProperties;
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
    public String getFormat() {
        return format;
    }

    @Override
    public void setFormat(String format) {
        this.format = format;
    }

    @Override
    public String getRef() {
        return ref;
    }

    @Override
    public void setRef(String ref) {
        if (ref != null && !ref.contains(".") && !ref.contains("/")) {
            ref = "#/components/schemas/" + ref;
        }
        this.ref = ref;
    }

    @Override
    public Boolean getNullable() {
        return nullable;
    }

    @Override
    public void setNullable(Boolean nullable) {
        this.nullable = nullable;
    }

    @Override
    public Boolean getReadOnly() {
        return readOnly;
    }

    @Override
    public void setReadOnly(Boolean readOnly) {
        this.readOnly = readOnly;
    }

    @Override
    public Boolean getWriteOnly() {
        return writeOnly;
    }

    @Override
    public void setWriteOnly(Boolean writeOnly) {
        this.writeOnly = writeOnly;
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
    public ExternalDocumentation getExternalDocs() {
        return externalDocs;
    }

    @Override
    public void setExternalDocs(ExternalDocumentation externalDocs) {
        this.externalDocs = externalDocs;
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
    public XML getXml() {
        return xml;
    }

    @Override
    public void setXml(XML xml) {
        this.xml = xml;
    }

    @Override
    public Schema enumeration(List<Object> enumeration) {
        setEnumeration(enumeration);
        return this;
    }

    @Override
    public Schema getItems() {
        return this.items;
    }

    @Override
    public void setItems(Schema items) {
        this.items = items;
    }

    @Override
    public List<Schema> getAllOf() {
        return allOf;
    }

    @Override
    public void setAllOf(List<Schema> allOf) {
        this.allOf = allOf;
    }

    @Override
    public Schema addAllOf(Schema allOf) {
        this.allOf.add(allOf);
        return this;
    }

    @Override
    public void removeAllOf(Schema allOf) {
        this.allOf.remove(allOf);
    }

    @Override
    public List<Schema> getAnyOf() {
        return this.anyOf;
    }

    @Override
    public void setAnyOf(List<Schema> anyOf) {
        this.anyOf = anyOf;
    }

    @Override
    public Schema addAnyOf(Schema anyOf) {
        this.anyOf.add(anyOf);
        return this;
    }

    @Override
    public void removeAnyOf(Schema anyOf) {
        this.anyOf.remove(anyOf);
    }

    @Override
    public List<Schema> getOneOf() {
        return this.oneOf;
    }

    @Override
    public void setOneOf(List<Schema> oneOf) {
        this.oneOf = oneOf;
    }

    @Override
    public Schema addOneOf(Schema oneOf) {
        this.oneOf.add(oneOf);
        return this;
    }

    @Override
    public void removeOneOf(Schema oneOf) {
        this.oneOf.remove(oneOf);
    }

    @Override
    public void setAdditionalProperties(Boolean additionalProperties) {
        this.additionalProperties = additionalProperties;
    }

    public static void merge(org.eclipse.microprofile.openapi.annotations.media.Schema from, Schema to,
            boolean override, Map<String, Schema> currentSchemas) {
        if (isAnnotationNull(from)) {
            return;
        }
        if (from.ref() != null && !from.ref().isEmpty()) {
            applyReference(to, from.ref());
            return;
        }
        if (from.type() != null
                && from.type() != org.eclipse.microprofile.openapi.annotations.enums.SchemaType.DEFAULT) {
            to.setType(mergeProperty(to.getType(), SchemaType.valueOf(from.type().name()), override));
        }
        if (from.implementation() != null && currentSchemas != null) {
            Class<?> implementationClass = from.implementation();
            if (!implementationClass.getTypeName().equals("java.lang.Void")) {
                org.eclipse.microprofile.openapi.annotations.media.Schema annotation = implementationClass
                        .getDeclaredAnnotation(org.eclipse.microprofile.openapi.annotations.media.Schema.class);
                // Get the schema name
                String schemaName = null;
                if (annotation == null || annotation.name() == null || annotation.name().isEmpty()) {
                    schemaName = implementationClass.getSimpleName();
                } else {
                    schemaName = annotation.name();
                }

                // Get the schema reference, and copy it's values over to the new schema model
                Schema copyFrom = currentSchemas.get(schemaName);
                if (implementationClass.equals(String.class)) {
                    copyFrom = new SchemaImpl().type(SchemaType.STRING);
                }
                if (to.getType() == SchemaType.ARRAY) {
                    to.setItems(new SchemaImpl());
                    ModelUtils.merge(copyFrom, to.getItems(), true);
                } else {
                    ModelUtils.merge(copyFrom, to, true);
                }
                to.setRef(null);
            }
        }
        if (from.discriminatorMapping() != null && !from.discriminatorProperty().isEmpty()) {
            if (to.getDiscriminator() == null) {
                to.setDiscriminator(new DiscriminatorImpl());
            }
            Discriminator discriminator = to.getDiscriminator();
            discriminator.setPropertyName(
                    mergeProperty(discriminator.getPropertyName(), from.discriminatorProperty(), override));
            for (DiscriminatorMapping mapping : from.discriminatorMapping()) {
                discriminator.addMapping(mapping.value(), mapping.schema().getSimpleName());
            }
        }
        to.setTitle(mergeProperty(to.getTitle(), from.title(), override));
        to.setDefaultValue(mergeProperty(to.getDefaultValue(), from.defaultValue(), override));
        if (from.enumeration() != null && from.enumeration().length > 0) {
            if (to.getEnumeration() == null) {
                to.setEnumeration(new ArrayList<>());
            }
            for (String value : from.enumeration()) {
                if (!to.getEnumeration().contains(value)) {
                    to.addEnumeration(value);
                }
            }
        }
        if (from.multipleOf() > 0) {
            to.setMultipleOf(mergeProperty(to.getMultipleOf(),
                    BigDecimal.valueOf(from.multipleOf()).stripTrailingZeros(), override));
        }
        if (!from.maximum().isEmpty()) {
            to.setMaximum(mergeProperty(to.getMaximum(), new BigDecimal(from.maximum()), override));
        }
        if (!from.minimum().isEmpty()) {
            to.setMinimum(mergeProperty(to.getMinimum(), new BigDecimal(from.minimum()), override));
        }
        to.setExclusiveMaximum(mergeProperty(to.getExclusiveMaximum(), from.exclusiveMaximum(), override));
        to.setExclusiveMinimum(mergeProperty(to.getExclusiveMinimum(), from.exclusiveMinimum(), override));
        to.setMaxLength(mergeProperty(to.getMaxLength(), from.maxLength(), override));
        to.setMinLength(mergeProperty(to.getMinLength(), from.minLength(), override));
        to.setMaxItems(mergeProperty(to.getMaxItems(), from.maxItems(), override));
        to.setMinItems(mergeProperty(to.getMinItems(), from.minItems(), override));
        to.setMaxProperties(mergeProperty(to.getMaxProperties(), from.maxProperties(), override));
        to.setMinProperties(mergeProperty(to.getMinProperties(), from.minProperties(), override));
        to.setUniqueItems(mergeProperty(to.getUniqueItems(), from.uniqueItems(), override));
        to.setPattern(mergeProperty(to.getPattern(), from.pattern(), override));
        if (from.requiredProperties() != null && from.requiredProperties().length > 0) {
            if (to.getRequired() == null) {
                to.setRequired(new ArrayList<>());
            }
            for (String value : from.requiredProperties()) {
                if (!to.getRequired().contains(value)) {
                    to.addRequired(value);
                }
            }
        }
        if (from.not() != null) {
            if (Schema.class.isAssignableFrom(from.not())) {
                try {
                    to.setNot((Schema) from.not().newInstance());
                } catch (InstantiationException | IllegalAccessException ex) {
                    LOGGER.log(WARNING, "Unable to create Schema class instance.", ex);
                }
            }
        }
        to.setDescription(mergeProperty(to.getDescription(), from.description(), override));
        to.setFormat(mergeProperty(to.getFormat(), from.format(), override));
        to.setNullable(mergeProperty(to.getNullable(), from.nullable(), override));
        to.setReadOnly(mergeProperty(to.getReadOnly(), from.readOnly(), override));
        to.setWriteOnly(mergeProperty(to.getWriteOnly(), from.writeOnly(), override));
        to.setExample(mergeProperty(to.getExample(), from.example(), override));
        if (!isAnnotationNull(from.externalDocs())) {
            if (to.getExternalDocs() == null) {
                to.setExternalDocs(new ExternalDocumentationImpl());
            }
            ExternalDocumentationImpl.merge(from.externalDocs(), to.getExternalDocs(), override);
        }
        to.setDeprecated(mergeProperty(to.getDeprecated(), from.deprecated(), override));
        if (from.allOf() != null) {
            for (Class<?> allOfClass : from.allOf()) {
                if (Schema.class.isAssignableFrom(allOfClass)) {
                    if (to.getAllOf() == null) {
                        to.setAllOf(new ArrayList<>());
                    }
                    try {
                        to.addAllOf((Schema) allOfClass.newInstance());
                    } catch (InstantiationException | IllegalAccessException ex) {
                        LOGGER.log(WARNING, "Unable to create Schema class instance.", ex);
                    }
                }
            }
        }
        if (from.anyOf() != null) {
            for (Class<?> anyOfClass : from.anyOf()) {
                if (Schema.class.isAssignableFrom(anyOfClass)) {
                    if (to.getAnyOf() == null) {
                        to.setAnyOf(new ArrayList<>());
                    }
                    try {
                        to.addAnyOf((Schema) anyOfClass.newInstance());
                    } catch (InstantiationException | IllegalAccessException ex) {
                        LOGGER.log(WARNING, "Unable to create Schema class instance.", ex);
                    }
                }
            }
        }
        if (from.oneOf() != null) {
            for (Class<?> oneOfClass : from.oneOf()) {
                if (Schema.class.isAssignableFrom(oneOfClass)) {
                    if (to.getOneOf() == null) {
                        to.setOneOf(new ArrayList<>());
                    }
                    try {
                        to.addOneOf((Schema) oneOfClass.newInstance());
                    } catch (InstantiationException | IllegalAccessException ex) {
                        LOGGER.log(WARNING, "Unable to create Schema class instance.", ex);
                    }
                }
            }
        }
    }

}