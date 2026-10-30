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
    protected Map<String, Object> extensions = null; // null means not specified, empty map means specified empty extensions

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
        if (extensions == null) {
            this.extensions = null;
        } else {
            this.extensions = createMap();
            for (Entry<String, Object> entry : extensions.entrySet()) {
                this.extensions.put(extensionName(entry.getKey()), entry.getValue());
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public final T addExtension(String name, Object value) {
        if (value != null) {
            if (extensions == null) {
                extensions = createMap();
            }
            this.extensions.put(extensionName(name), value);
        }
        return (T) this;
    }

    @Override
    public final void removeExtension(String name) {
        if (extensions != null) {
            this.extensions.remove(extensionName(name));
        }
    }

    /**
     * Custom {@link JsonSerializer} that adds both the extended {@link TreeMap} entries as well as the
     * {@link ExtensibleTreeMap#extensions} map to the output object unless the value represents a {@link Reference} in
     * which case only the {@link Reference#getRef()} is added.
     */
    static class ExtensibleTreeMapSerializer extends JsonSerializer<ExtensibleTreeMap<?, ?>> {

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
            if (value.getExtensions() != null) {
                for (Map.Entry<String, Object> extension : value.getExtensions().entrySet()) {
                    gen.writeFieldName(extension.getKey());
                    Object extensionValue = extension.getValue();
                    serializers.findValueSerializer(extensionValue.getClass()).serialize(extensionValue, gen, serializers);
                }
            }
            gen.writeEndObject();
        }
    }
}
