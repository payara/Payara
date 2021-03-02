package fish.payara.microprofile.openapi.impl.model;

import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.createMap;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.readOnlyView;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import static fish.payara.microprofile.openapi.impl.model.ExtensibleImpl.extensionName;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import org.eclipse.microprofile.openapi.models.Extensible;
import org.eclipse.microprofile.openapi.models.Reference;

@JsonSerialize(using = ExtensibleTreeMap.ExtensibleTreeMapSerializer.class)
public abstract class ExtensibleTreeMap<V, T extends Extensible<T>> extends TreeMap<String, V>
        implements Extensible<T> {

    @JsonIgnore
    protected Map<String, Object> extensions = createMap();

    protected ExtensibleTreeMap() {
        super();
    }

    protected ExtensibleTreeMap(Map<String, ? extends V> items) {
        super(items);
    }

    @Override
    public final Map<String, Object> getExtensions() {
        return readOnlyView(extensions);
    }

    @Override
    public final void setExtensions(Map<String, Object> extensions) {
        this.extensions.clear();
        for (Entry<String, Object> entry : extensions.entrySet()) {
            this.extensions.put(extensionName(entry.getKey()), entry.getValue());
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public final T addExtension(String name, Object value) {
        if (value != null) {
            this.extensions.put(extensionName(name), value);
        }
        return (T) this;
    }

    @Override
    public final void removeExtension(String name) {
        this.extensions.remove(extensionName(name));
    }

    /**
     * Custom {@link JsonSerializer} that adds both the extended {@link TreeMap} entries as well as the
     * {@link ExtensibleTreeMap#extensions} map to the output object unless the value represents a {@link Reference} in
     * which case only the {@link Reference#getRef()} is added.
     */
    static class ExtensibleTreeMapSerializer extends JsonSerializer<ExtensibleTreeMap<?,?>> {

        @Override
        public void serialize(ExtensibleTreeMap<?,?> value, JsonGenerator gen, SerializerProvider serializers)
                throws IOException {
            if (value instanceof Reference) {
                Reference<?> reference = (Reference<?>) value;
                String ref = reference.getRef();
                if (ref != null) {
                    gen.writeStartObject(value);
                    gen.writeFieldName("$ref");
                    gen.writeString(ref);
                    gen.writeEndObject();
                    return; // if this is a ref no extensions or map entries are relevant
                }
            }
            gen.writeStartObject(value);
            ParameterizedType mapType = (ParameterizedType) value.getClass().getGenericSuperclass();
            Class<?> valueType = (Class<?>) mapType.getActualTypeArguments()[0];
            JsonSerializer<Object> valueSerializer = serializers.findValueSerializer(valueType);
            for (Map.Entry<String,?> entry : value.entrySet()) {
                gen.writeFieldName(entry.getKey());
                valueSerializer.serialize(entry.getValue(), gen, serializers);
            }
            for (Map.Entry<String, Object> extension : value.getExtensions().entrySet()) {
                gen.writeFieldName(extension.getKey());
                Object extensionValue = extension.getValue();
                serializers.findValueSerializer(extensionValue.getClass()).serialize(extensionValue, gen, serializers);
            }
            gen.writeEndObject();
        }
    }
}
