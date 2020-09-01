/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2018-2020] Payara Foundation and/or its affiliates. All rights reserved.
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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import fish.payara.microprofile.openapi.api.visitor.ApiContext;
import fish.payara.microprofile.openapi.impl.model.ExtensibleImpl;
import fish.payara.microprofile.openapi.impl.model.ExternalDocumentationImpl;
import fish.payara.microprofile.openapi.impl.visitor.AnnotationInfo;
import fish.payara.microprofile.openapi.impl.model.util.ModelUtils;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.applyReference;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.mergeProperty;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import static java.util.logging.Level.WARNING;
import java.util.logging.Logger;
import org.eclipse.microprofile.openapi.models.ExternalDocumentation;
import org.eclipse.microprofile.openapi.models.media.Discriminator;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.media.XML;
import org.glassfish.hk2.classmodel.reflect.AnnotationModel;
import org.glassfish.hk2.classmodel.reflect.ClassModel;
import org.glassfish.hk2.classmodel.reflect.EnumModel;
import org.glassfish.hk2.classmodel.reflect.ExtensibleType;
import org.glassfish.hk2.classmodel.reflect.Type;

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

    private Schema not;
    private List<Schema> anyOf = new ArrayList<>();
    private List<Schema> allOf = new ArrayList<>();
    private List<Schema> oneOf = new ArrayList<>();

    private Object additionalProperties;
    private Schema items;
    private String implementation;

    public static Schema createInstance(AnnotationModel annotation, ApiContext context) {
        SchemaImpl from = new SchemaImpl();
        from.setDefaultValue(annotation.getValue("defaultValue", Object.class));
        from.setTitle(annotation.getValue("title", String.class));
        Double multipleOf = annotation.getValue("multipleOf", Double.class);
        if (multipleOf != null) {
            from.setMultipleOf(BigDecimal.valueOf(multipleOf));
        }
        String maximum = annotation.getValue("maximum", String.class);
        if (maximum != null && !maximum.isEmpty()) {
            from.setMaximum(new BigDecimal(maximum));
        }
        from.setExclusiveMaximum(annotation.getValue("exclusiveMaximum", Boolean.class));
        String minimum = annotation.getValue("minimum", String.class);
        if (minimum != null && !minimum.isEmpty()) {
            from.setMinimum(new BigDecimal(minimum));
        }
        from.setExclusiveMinimum(annotation.getValue("exclusiveMinimum", Boolean.class));
        from.setMaxLength(annotation.getValue("maxLength", Integer.class));
        from.setMinLength(annotation.getValue("minLength", Integer.class));
        from.setPattern(annotation.getValue("pattern", String.class));
        from.setMaxItems(annotation.getValue("maxItems", Integer.class));
        from.setMinItems(annotation.getValue("minItems", Integer.class));
        from.setUniqueItems(annotation.getValue("uniqueItems", Boolean.class));
        from.setMaxProperties(annotation.getValue("maxProperties", Integer.class));
        from.setMinProperties(annotation.getValue("minProperties", Integer.class));
        from.setRequired(annotation.getValue("requiredProperties", List.class));
        EnumModel typeEnum = annotation.getValue("type", EnumModel.class);
        if (typeEnum != null) {
            from.setType(SchemaType.valueOf(typeEnum.getValue()));
        }
        from.setDescription(annotation.getValue("description", String.class));
        from.setFormat(annotation.getValue("format", String.class));
        String ref = annotation.getValue("ref", String.class);
        if (ref != null && !ref.isEmpty()) {
            from.setRef(ref);
        }
        from.setNullable(annotation.getValue("nullable", Boolean.class));
        from.setReadOnly(annotation.getValue("readOnly", Boolean.class));
        from.setWriteOnly(annotation.getValue("writeOnly", Boolean.class));
        from.setExample(annotation.getValue("example", Object.class));
        AnnotationModel externalDocs = annotation.getValue("externalDocs", AnnotationModel.class);
        if (externalDocs != null) {
            from.setExternalDocs(ExternalDocumentationImpl.createInstance(externalDocs));
        }
        from.setDeprecated(annotation.getValue("deprecated", Boolean.class));
        from.setEnumeration(annotation.getValue("enumeration", List.class));
        String discriminatorProperty = annotation.getValue("discriminatorProperty", String.class);
        List<AnnotationModel> discriminatorMapping = annotation.getValue("discriminatorMapping", List.class);
        if (discriminatorMapping != null && !discriminatorProperty.isEmpty()) {
            DiscriminatorImpl discriminator = new DiscriminatorImpl();
            discriminator.setPropertyName(discriminatorProperty);
            for (AnnotationModel mapping : discriminatorMapping) {
                String value = mapping.getValue("value", String.class);
                String schema = mapping.getValue("schema", String.class);
                discriminator.addMapping(value, ModelUtils.getSimpleName(schema));
            }
            from.setDiscriminator(discriminator);
        }

        String not = annotation.getValue("not", String.class);
        if (not != null) {
            Schema schema = from.getSchemaInstance(not, context);
            if (schema != null) {
                from.setNot(schema);
            }
        }
        List<String> anyOf = annotation.getValue("anyOf", List.class);
        if (anyOf != null) {
            if (from.getAnyOf() == null) {
                from.setAnyOf(new ArrayList<>());
            }
            from.getAnyOf().addAll(from.getSchemaInstances(anyOf, context));
        }
        List<String> allOf = annotation.getValue("allOf", List.class);
        if (allOf != null) {
            if (from.getAllOf() == null) {
                from.setAllOf(new ArrayList<>());
            }
            from.getAllOf().addAll(from.getSchemaInstances(allOf, context));
        }
        List<String> oneOf = annotation.getValue("oneOf", List.class);
        if (oneOf != null) {
            if (from.getOneOf() == null) {
                from.setOneOf(new ArrayList<>());
            }
            from.getOneOf().addAll(from.getSchemaInstances(oneOf, context));
        }

        from.setImplementation(annotation.getValue("implementation", String.class));
        return from;
    }

    private List<Schema> getSchemaInstances(List<String> fromList, ApiContext context) {
        List<Schema> to = new ArrayList<>();
        if (fromList != null) {
            for (String from : fromList) {
                Schema schema = getSchemaInstance(from, context);
                if (schema != null) {
                    to.add(schema);
                }
            }
        }
        return to;
    }

    private Schema getSchemaInstance(String schemaClassName, ApiContext context) {
        if (schemaClassName != null
                && !schemaClassName.equals("java.lang.Void")) {
            Type schemaType = context.getType(schemaClassName);
            if (schemaType instanceof ClassModel) {
                ClassModel schemaClassModel = (ClassModel) schemaType;
                if (schemaClassModel.isInstanceOf(Schema.class.getName())) {
                    try {
                        Class<?> oneOfClass = context.getApplicationClassLoader().loadClass(schemaClassName);
                        return (Schema) oneOfClass.newInstance();
                    } catch (ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
                        LOGGER.log(WARNING, "Unable to create Schema class instance.", ex);
                    }
                }
            }
        }
        return null;
    }

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

    @JsonProperty
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
        return additionalProperties instanceof Boolean ? (Boolean) additionalProperties : null;
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

    public String getImplementation() {
        return implementation;
    }

    public void setImplementation(String implementation) {
        this.implementation = implementation;
    }

    public static void merge(Schema from, Schema to,
            boolean override, ApiContext context) {

        if (from == null) {
            return;
        }
        if (from.getRef() != null && !from.getRef().isEmpty()) {
            applyReference(to, from.getRef());
            return;
        }
        to.setDefaultValue(mergeProperty(to.getDefaultValue(), from.getDefaultValue(), override));
        to.setTitle(mergeProperty(to.getTitle(), from.getTitle(), override));
        if (from.getMultipleOf() != null && from.getMultipleOf().compareTo(BigDecimal.ZERO) > 0) {
            to.setMultipleOf(mergeProperty(to.getMultipleOf(),
                    from.getMultipleOf().stripTrailingZeros(), override));
        }
        if (from.getMaximum() != null) {
            to.setMaximum(mergeProperty(to.getMaximum(), from.getMaximum(), override));
        }
        to.setExclusiveMaximum(mergeProperty(to.getExclusiveMaximum(), from.getExclusiveMaximum(), override));
        if (from.getMinimum() != null) {
            to.setMinimum(mergeProperty(to.getMinimum(), from.getMinimum(), override));
        }
        to.setExclusiveMinimum(mergeProperty(to.getExclusiveMinimum(), from.getExclusiveMinimum(), override));
        to.setMaxLength(mergeProperty(to.getMaxLength(), from.getMaxLength(), override));
        to.setMinLength(mergeProperty(to.getMinLength(), from.getMinLength(), override));
        to.setPattern(mergeProperty(to.getPattern(), from.getPattern(), override));
        to.setMaxItems(mergeProperty(to.getMaxItems(), from.getMaxItems(), override));
        to.setMinItems(mergeProperty(to.getMinItems(), from.getMinItems(), override));
        to.setUniqueItems(mergeProperty(to.getUniqueItems(), from.getUniqueItems(), override));
        to.setMaxProperties(mergeProperty(to.getMaxProperties(), from.getMaxProperties(), override));
        to.setMinProperties(mergeProperty(to.getMinProperties(), from.getMinProperties(), override));
        if (from.getRequired() != null && !from.getRequired().isEmpty()) {
            if (to.getRequired() == null) {
                to.setRequired(new ArrayList<>());
            }
            for (String value : from.getRequired()) {
                if (!to.getRequired().contains(value)) {
                    to.addRequired(value);
                }
            }
        }
        if (from.getType() != null) {
            to.setType(mergeProperty(to.getType(), from.getType(), override));
        }
        if (from.getProperties() != null && !from.getProperties().isEmpty()) {
            if (to.getProperties() == null) {
                to.setProperties(new HashMap<>());
            }
            for (String key : from.getProperties().keySet()) {
                if (!to.getProperties().containsKey(key)) {
                    to.addProperty(key, from.getProperties().get(key));
                }
            }
        }
        to.setDescription(mergeProperty(to.getDescription(), from.getDescription(), override));
        to.setFormat(mergeProperty(to.getFormat(), from.getFormat(), override));
        to.setNullable(mergeProperty(to.getNullable(), from.getNullable(), override));
        to.setReadOnly(mergeProperty(to.getReadOnly(), from.getReadOnly(), override));
        to.setWriteOnly(mergeProperty(to.getWriteOnly(), from.getWriteOnly(), override));
        to.setExample(mergeProperty(to.getExample(), from.getExample(), override));
        if (from.getExternalDocs() != null) {
            if (to.getExternalDocs() == null) {
                to.setExternalDocs(new ExternalDocumentationImpl());
            }
            ExternalDocumentationImpl.merge(from.getExternalDocs(), to.getExternalDocs(), override);
        }
        to.setDeprecated(mergeProperty(to.getDeprecated(), from.getDeprecated(), override));
        if (from.getEnumeration() != null && from.getEnumeration().size() > 0) {
            if (to.getEnumeration() == null) {
                to.setEnumeration(new ArrayList<>());
            }
            for (Object value : from.getEnumeration()) {
                if (!to.getEnumeration().contains(value)) {
                    to.addEnumeration(value);
                }
            }
        }
        if (from.getDiscriminator() != null) {
            if (to.getDiscriminator() == null) {
                to.setDiscriminator(new DiscriminatorImpl());
            }
            Discriminator discriminator = to.getDiscriminator();
            discriminator.setPropertyName(
                    mergeProperty(discriminator.getPropertyName(), from.getDiscriminator().getPropertyName(), override)
            );
            for (Entry<String, String> mapping : from.getDiscriminator().getMapping().entrySet()) {
                discriminator.addMapping(mapping.getKey(), mapping.getValue());
            }
        }
        if (from.getNot() != null) {
            to.setNot(from.getNot());
        }
        if (from.getAllOf() != null) {
            if (to.getAllOf() == null) {
                to.setAllOf(new ArrayList<>());
            }
            to.getAllOf().addAll(from.getAllOf());
        }
        if (from.getAnyOf() != null) {
            if (to.getAnyOf() == null) {
                to.setAnyOf(new ArrayList<>());
            }
            to.getAnyOf().addAll(from.getAnyOf());
        }
        if (from.getOneOf() != null) {
            if (to.getOneOf() == null) {
                to.setOneOf(new ArrayList<>());
            }
            to.getOneOf().addAll(from.getOneOf());
        }
        if (from instanceof SchemaImpl
                && ((SchemaImpl) from).getImplementation() != null
                && context.getApi().getComponents().getSchemas() != null) {
            String implementationClass = ((SchemaImpl) from).getImplementation();
            if (!implementationClass.equals("java.lang.Void")) {
                Type type = context.getType(implementationClass);
                String schemaName = null;
                if (type instanceof ExtensibleType) {
                    ExtensibleType implementationType = (ExtensibleType) type;
                    AnnotationInfo annotationInfo = context.getAnnotationInfo(implementationType);
                    AnnotationModel annotation = annotationInfo.getAnnotation(org.eclipse.microprofile.openapi.annotations.media.Schema.class);
                    // Get the schema name
                    if (annotation != null) {
                        schemaName = annotation.getValue("name", String.class);
                    }
                }
                if (schemaName == null || schemaName.isEmpty()) {
                    schemaName = ModelUtils.getSimpleName(implementationClass);
                }
                // Get the schema reference, and copy it's values over to the new schema model
                Schema copyFrom = context.getApi().getComponents().getSchemas().get(schemaName);
                if (copyFrom == null) {
                    SchemaType schemaType = ModelUtils.getSchemaType(implementationClass, context);
                    copyFrom = new SchemaImpl().type(schemaType);
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

    }

}
