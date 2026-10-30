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
package fish.payara.microprofile.openapi.impl.model;

import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.createMap;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.mergeProperty;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.readOnlyView;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.microprofile.openapi.models.Extensible;
import org.glassfish.hk2.classmodel.reflect.AnnotationModel;

public abstract class ExtensibleImpl<T extends Extensible<T>> implements Extensible<T> {

    private static final Logger LOGGER = Logger.getLogger(ExtensibleImpl.class.getName());

    @JsonIgnore
    protected Map<String, Object> extensions = null; // null means not specified, empty map means specified empty extensions

    @Override
    public Map<String, Object> getExtensions() {
        return readOnlyView(extensions);
    }

    @SuppressWarnings("unchecked")
    @Override
    public T addExtension(String name, Object value) {
        if (value != null) {
            if (extensions == null) {
                extensions = createMap();
            }
            extensions.put(extensionName(name), value);
        }
        return (T) this;
    }

    @Override
    public void removeExtension(String name) {
        if (extensions != null) {
            extensions.remove(extensionName(name));
        }
    }

    @Override
    public void setExtensions(Map<String, Object> extensions) {
        this.extensions = createMap(extensions);
    }

    public static String extensionName(String name) {
        if (name != null && !name.startsWith("x-")) {
            //NB. MP group decided that extension names should not be corrected
            LOGGER.log(Level.WARNING, "extension name not starting with `x-` cause invalid Open API documents: {0}", name);
        }
        return name;
    }

    public static Map<String, Object> parseExtensions(AnnotationModel annotation) {
        List<AnnotationModel> extensions = annotation.getValue("extensions", List.class);
        Map<String, Object> parsedExtensions = null;
        if (extensions != null) {
            parsedExtensions = new HashMap<>();
            for (AnnotationModel extension : extensions) {
                String name = extension.getValue("name", String.class);
                String value = extension.getValue("value", String.class);
                parsedExtensions.put(name, value);
            }
        }
        return parsedExtensions;
    }

    public static void merge(Extensible<?> from, Extensible<?> to, boolean override) {
        if (from == null) {
            return;
        }
        if (from.getExtensions() == null) {
            return;
        }
        if (from.getExtensions().isEmpty() && to.getExtensions() == null) {
            // from has empty extensions, but existing(!), copy this information to to
            to.setExtensions(createMap());
            return;
        }
        if (to.getExtensions() == null) {
            // we will copy values, prepare extensions, if they don't exist
            to.setExtensions(createMap());
        }
        for (String extensionName : from.getExtensions().keySet()) {
            Object value = mergeProperty(
                    to.getExtensions().get(extensionName),
                    from.getExtensions().get(extensionName),
                    override
            );
            to.addExtension(extensionName, value);
        }
    }

    public static Object convertExtensionValue(String value, boolean parseValue) {
        if (value == null) {
            return null;
        }
        if (parseValue) {
            try {
                JsonNode node = new ObjectMapper().readTree(value);
                if (node.isBoolean()) {
                    return node.booleanValue();
                }
                if (node.isNumber()) {
                    return node.numberValue();
                }
                return node;
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to parse extension value: {0}", value);
                return value;
            }
        }
        // Could be an array
        if (value.contains(",")) {
            // Remove leading and trailing brackets, then parse to an array
            String[] possibleArray = value.replaceAll("^[\\[\\{\\(]", "").replaceAll("[\\]\\}\\)]$", "").split(",");

            if (possibleArray.length > 1) {
                return possibleArray;
            }
        }
        return value;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        toString(str, "\t");
        return str.toString();
    }

    void toString(StringBuilder str, String indent) {
        str.append('[').append(getClass().getSimpleName()).append("] {");
        Class<?> type = getClass();
        while (type != Object.class) {
            for (Field field : type.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers()) && !field.isSynthetic()) {
                    try {
                        field.setAccessible(true);
                        Object value = field.get(this);
                        toString(str, indent, field.getName(), value);
                    } catch (IllegalArgumentException | IllegalAccessException e) {
                        str.append("<failure:").append(e.getMessage()).append(">");
                    }
                }
            }
            type = type.getSuperclass();
        }
        str.append('\n').append(indent.length() > 0 ? indent.substring(1) : indent).append('}');
    }

    private static void toString(StringBuilder str, String indent, Object key, Object value) {
        if (isNonEmpty(value)) {
            str.append("\n").append(indent);
            if (key != null) {
                str.append('"').append(key).append('"').append(": ");
            }
            if (value instanceof ExtensibleImpl) {
                ((ExtensibleImpl<?>)value).toString(str, indent + "\t");
            } else if (value instanceof Map) {
                str.append('{');
                for (Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                    toString(str, indent + '\t', entry.getKey(), entry.getValue());
                }
                str.append('\n').append(indent).append('}');
            } else if (value instanceof Collection) { 
                str.append('[');
                for (Object element : (Collection<?>)value) {
                    toString(str, indent+ '\t', null, element);
                    str.append(',');
                }
                str.append('\n').append(indent).append(']');
            } else if (value instanceof String) {
                str.append('"').append(value).append('"');
            } else {
                str.append(value);
            }
        }
    }

    private static boolean isNonEmpty(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Object[]) {
            return ((Object[])value).length > 0;
        }
        if (value instanceof Collection) {
            return !((Collection<?>) value).isEmpty();
        }
        if (value instanceof Map) {
            return !((Map<?, ?>) value).isEmpty();
        }
        return true;
    }
}