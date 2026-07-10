/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2021-2025] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.microprofile.openapi.impl.model.media;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import fish.payara.microprofile.openapi.api.visitor.ApiContext;
import fish.payara.microprofile.openapi.impl.model.ExtensibleImpl;
import fish.payara.microprofile.openapi.impl.model.ExternalDocumentationImpl;
import fish.payara.microprofile.openapi.impl.model.util.ModelUtils;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.applyReference;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.createList;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.createMap;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.extractAnnotations;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.mergeImmutableList;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.mergeProperty;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.readOnlyView;
import fish.payara.microprofile.openapi.impl.rest.app.provider.ObjectMapperFactory;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.util.Collections;
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
import org.glassfish.hk2.classmodel.reflect.Type;

// Never serialise the 'name' property, but allow deserialization
@JsonIgnoreProperties(value = "name", allowSetters = true)
public class SchemaImpl extends ExtensibleImpl<Schema> implements Schema {

    private static final Logger LOGGER = Logger.getLogger(SchemaImpl.class.getName());

    private Object defaultValue;

    private String name;
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
    private List<String> required = createList();
    private SchemaType type;
    private Map<String, Schema> properties = createMap();
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
    private List<Object> enumeration = createList();
    private Discriminator discriminator;

    private Schema not;
    private List<Schema> anyOf = createList();
    private List<Schema> allOf = createList();
    private List<Schema> oneOf = createList();

    private Object additionalProperties;
    private Schema items;
    @JsonIgnore
    private String implementation;
    @JsonIgnore
    private boolean isRequired;

    public static SchemaImpl valueOf(String content) throws JsonMappingException, JsonProcessingException {
        return ObjectMapperFactory
                .createJson()
                .readValue(content, SchemaImpl.class);
    }

    public static SchemaImpl createInstance(AnnotationModel annotation, ApiContext context) {
        SchemaImpl from = new SchemaImpl();

        if (annotation == null) {
            return from;
        }

        // Solve the required attribute before "ref" as it is the only one which doesn't conflict with it.
        final Boolean isRequired = annotation.getValue("required", Boolean.class);
        if (isRequired != null) {
            from.isRequired = isRequired;
        }

        String ref = annotation.getValue("ref", String.class);
        if (ref != null && !ref.isEmpty()) {
            from.setRef(ref);
            return from;
        }

        EnumModel typeEnum = annotation.getValue("type", EnumModel.class);
        if (typeEnum != null) {
            from.setType(SchemaType.valueOf(typeEnum.getValue()));
        }

        final String implementationClass = annotation.getValue("implementation", String.class);
        if (implementationClass != null) {
            setImplementation(from, implementationClass, true, context);
        }

        from.setDefaultValue(annotation.getValue("defaultValue", Object.class));
        from.setName(annotation.getValue("name", String.class));
        from.setTitle(annotation.getValue("title", String.class));
        from.setExtensions(parseExtensions(annotation));
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
        String additionalPropertiesAttr = annotation.getValue("additionalProperties", String.class);
        if (additionalPropertiesAttr == null || Void.class.getName().equals(additionalPropertiesAttr)) {
            // Void is used as default, e.g. not specified value
            from.setAdditionalPropertiesSchema(null);
        } else if (org.eclipse.microprofile.openapi.annotations.media.Schema.True.class.getName().equals(additionalPropertiesAttr)
                || org.eclipse.microprofile.openapi.annotations.media.Schema.False.class.getName().equals(additionalPropertiesAttr)) {
            from.setAdditionalPropertiesBoolean(org.eclipse.microprofile.openapi.annotations.media.Schema.True.class.getName().equals(additionalPropertiesAttr));
        } else {
            from.setAdditionalPropertiesSchema(fromImplementation(additionalPropertiesAttr, context));
        }

        extractAnnotations(annotation, context, "properties", "name", SchemaImpl::createInstance, from::addProperty);
        for (Entry<String, Schema> property : from.getProperties().entrySet()) {
            final SchemaImpl propertySchema = (SchemaImpl) property.getValue();
            if (propertySchema.isRequired) {
                from.addRequired(property.getKey());
            }
        }

        from.setDescription(annotation.getValue("description", String.class));
        from.setFormat(annotation.getValue("format", String.class));
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
            mergeImmutableList(from.getAnyOf(), from.getSchemaInstances(anyOf, context), from::setAnyOf);
        }
        List<String> allOf = annotation.getValue("allOf", List.class);
        if (allOf != null) {
            mergeImmutableList(from.getAllOf(), from.getSchemaInstances(allOf, context), from::setAllOf);
        }
        List<String> oneOf = annotation.getValue("oneOf", List.class);
        if (oneOf != null) {
            mergeImmutableList(from.getOneOf(), from.getSchemaInstances(oneOf, context), from::setOneOf);
        }

        return from;
    }

    private List<Schema> getSchemaInstances(List<String> fromList, ApiContext context) {
        List<Schema> to = createList();
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
                        return (Schema) oneOfClass.getDeclaredConstructor().newInstance();
                    } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
                            | NoSuchMethodException | SecurityException | InvocationTargetException ex) {
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
        return readOnlyView(enumeration);
    }

    @Override
    public void setEnumeration(List<Object> enumeration) {
        this.enumeration = createList(enumeration);
    }

    @Override
    public Schema addEnumeration(Object enumerationItem) {
        if (enumerationItem != null) {
            if (this.enumeration == null) {
                this.enumeration = createList();
            }
            this.enumeration.add(enumerationItem);
        }
        return this;
    }

    @Override
    public void removeEnumeration(Object enumeration) {
        if (this.enumeration != null) {
            this.enumeration.remove(enumeration);
        }
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
        return readOnlyView(required);
    }

    @Override
    public void setRequired(List<String> required) {
        this.required = createList(required);
    }

    @Override
    public Schema addRequired(String requiredItem) {
        if (requiredItem != null) {
            if (required == null) {
                required = createList();
            }
            if (!required.contains(requiredItem)) {
                required.add(requiredItem);
            }
            Collections.sort(required);
        }
        return this;
    }

    public boolean isRequired() {
        return isRequired;
    }

    public void setRequired(boolean isRequired) {
        this.isRequired = isRequired;
    }

    @Override
    public void removeRequired(String required) {
        if (this.required != null) {
            this.required.remove(required);
        }
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
        return readOnlyView(properties);
    }

    @Override
    public void setProperties(Map<String, Schema> properties) {
        this.properties = createMap(properties);
    }

    @Override
    public Schema addProperty(String key, Schema propertiesItem) {
        if (propertiesItem != null) {
            if (this.properties == null) {
                this.properties = createMap();
            }
            this.properties.put(key, propertiesItem);
        }
        return this;
    }

    @Override
    public void removeProperty(String key) {
        if (this.properties != null) {
            this.properties.remove(key);
        }
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
        return readOnlyView(allOf);
    }

    @Override
    public void setAllOf(List<Schema> allOf) {
        this.allOf = createList(allOf);
    }

    @Override
    public Schema addAllOf(Schema allOf) {
        if (allOf != null) {
            if (this.allOf == null) {
                this.allOf = createList();
            }
            this.allOf.add(allOf);
        }
        return this;
    }

    @Override
    public void removeAllOf(Schema allOf) {
        if (this.allOf != null) {
            this.allOf.remove(allOf);
        }
    }

    @Override
    public List<Schema> getAnyOf() {
        return readOnlyView(anyOf);
    }

    @Override
    public void setAnyOf(List<Schema> anyOf) {
        this.anyOf = createList(anyOf);
    }

    @Override
    public Schema addAnyOf(Schema anyOf) {
        if (anyOf != null) {
            if (this.anyOf == null) {
                this.anyOf = createList();
            }
            this.anyOf.add(anyOf);
        }
        return this;
    }

    @Override
    public void removeAnyOf(Schema anyOf) {
        if (this.anyOf != null) {
            this.anyOf.remove(anyOf);
        }
    }

    @Override
    public List<Schema> getOneOf() {
        return readOnlyView(oneOf);
    }

    @Override
    public void setOneOf(List<Schema> oneOf) {
        this.oneOf = createList(oneOf);
    }

    @Override
    public Schema addOneOf(Schema oneOf) {
        if (oneOf != null) {
            if (this.oneOf == null) {
                this.oneOf = createList();
            }
            this.oneOf.add(oneOf);
        }
        return this;
    }

    @Override
    public void removeOneOf(Schema oneOf) {
        if (this.oneOf != null) {
            this.oneOf.remove(oneOf);
        }
    }

    public String getImplementation() {
        return implementation;
    }

    public void setImplementation(String implementation) {
        this.implementation = implementation;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
        // process extensions attributes
        ExtensibleImpl.merge(from, to, override);
        if (from.getType() != null) {
            to.setType(mergeProperty(to.getType(), from.getType(), override));
        }
        if (from instanceof SchemaImpl && to instanceof SchemaImpl) {
            final String fromImplementation = ((SchemaImpl) from).getImplementation();
            if (fromImplementation != null) {
                setImplementation((SchemaImpl) to, fromImplementation, override, context);
            }
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
                to.setRequired(createList());
            }
            for (String value : from.getRequired()) {
                if (!to.getRequired().contains(value)) {
                    to.addRequired(value);
                }
            }
        }
        if (from.getProperties() != null && !from.getProperties().isEmpty()) {
            if (to.getProperties() == null) {
                to.setProperties(createMap());
            }
            final Map<String, Schema> toProperties = to.getProperties();
            for (Entry<String, Schema> fromEntry : from.getProperties().entrySet()) {
                final String name = fromEntry.getKey();
                final Schema fromSchema = fromEntry.getValue();
                if (!toProperties.containsKey(name)) {
                    to.addProperty(name, fromSchema);
                } else {
                    final Schema toSchema = toProperties.get(name);
                    SchemaImpl.merge(fromSchema, toSchema, override, context);
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
                to.setEnumeration(createList());
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
        if (from.getAdditionalPropertiesBoolean() != null) {
            to.setAdditionalPropertiesBoolean(mergeProperty(to.getAdditionalPropertiesBoolean(), from.getAdditionalPropertiesBoolean(), override));
        }
        if (from.getAdditionalPropertiesSchema() != null) {
            to.setAdditionalPropertiesSchema(mergeProperty(to.getAdditionalPropertiesSchema(), from.getAdditionalPropertiesSchema(), override));
        }
        mergeImmutableList(from.getAnyOf(), to.getAnyOf(), to::setAnyOf);
        mergeImmutableList(from.getAllOf(), to.getAllOf(), to::setAllOf);
        mergeImmutableList(from.getOneOf(), to.getOneOf(), to::setOneOf);
    }

    private static void setImplementation(SchemaImpl schema, String implementationClass, boolean override, ApiContext context) {
        if (context != null && context.getApi().getComponents().getSchemas() != null) {
            if (schema instanceof SchemaImpl) {
                schema.setImplementation(mergeProperty(((SchemaImpl)schema).getImplementation(), implementationClass, override));
            }

            if (implementationClass.endsWith("[]")) {
                implementationClass = implementationClass.substring(0, implementationClass.length() - 2);
                final SchemaImpl itemSchema = new SchemaImpl();
                schema.setItems(itemSchema);
                schema.setType(SchemaType.ARRAY);
                schema = itemSchema;
            }
            
            if (!implementationClass.equals("java.lang.Void")) {
                Type type = context.getType(implementationClass);
                String schemaName;
                if (type != null) {
                    schemaName = ModelUtils.getSchemaName(context, type);
                } else {
                    schemaName = ModelUtils.getSimpleName(implementationClass);
                }
                // Get the schema reference, and copy it's values over to the new schema model if they are missing
                Schema copyFrom = context.getApi().getComponents().getSchemas().get(schemaName);
                if (copyFrom == null) {
                    // If the class hasn't been parsed
                    SchemaType schemaType = ModelUtils.getSchemaType(implementationClass, context);
                    copyFrom = new SchemaImpl().type(schemaType);
                }
                if (schema.getType() == SchemaType.ARRAY) {
                    schema.setItems(new SchemaImpl());
                    ModelUtils.merge(copyFrom, schema.getItems(), false);
                } else {
                    ModelUtils.merge(copyFrom, schema, false);
                }
                schema.setRef(null);
            }
        }
    }

    public static SchemaImpl fromImplementation(String implementationClass, ApiContext context) {
        final SchemaImpl schema = new SchemaImpl();
        setImplementation(schema, implementationClass, true, context);
        return schema;
    }

}
