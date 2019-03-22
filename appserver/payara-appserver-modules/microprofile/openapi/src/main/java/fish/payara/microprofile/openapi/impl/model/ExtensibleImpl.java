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
package fish.payara.microprofile.openapi.impl.model;

import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.isAnnotationNull;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.mergeProperty;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.eclipse.microprofile.openapi.annotations.extensions.Extension;
import org.eclipse.microprofile.openapi.models.Extensible;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public abstract class ExtensibleImpl<T extends Extensible<T>> implements Extensible<T> {

    private static final Logger LOGGER = Logger.getLogger(ExtensibleImpl.class.getName());

    @JsonIgnore
    protected Map<String, Object> extensions = new LinkedHashMap<>();

    @Override
    public Map<String, Object> getExtensions() {
        return extensions;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T addExtension(String name, Object value) {
        if (value != null) {
            extensions.put(extensionName(name), value);
        }
        return (T) this;
    }

    @Override
    public void removeExtension(String name) {
        extensions.remove(extensionName(name));
    }

    @Override
    public void setExtensions(Map<String, Object> extensions) {
        this.extensions.clear();
        for (Entry<String, Object> entry : extensions.entrySet()) {
            this.extensions.put(extensionName(entry.getKey()), entry.getValue());
        }
    }

    public static String extensionName(String name) {
        return name.startsWith("x-") ? name : "x-" + name;
    }

    public static void merge(Extension from, Extensible<?> to, boolean override) {
        if (isAnnotationNull(from)) {
            return;
        }
        if (to.getExtensions() == null) {
            to.setExtensions(new LinkedHashMap<>());
        }
        if (from.name() != null && !from.name().isEmpty()) {
            Object value = mergeProperty(to.getExtensions().get(from.name()), 
                    convertExtensionValue(from.value(), from.parseValue()), override);
            to.getExtensions().put(from.name(), value);
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
                LOGGER.warning("Failed to parse extension value: " + value);
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
        str.append(getClass().getSimpleName());
        Class<?> type = getClass();
        while (type != Object.class) {
            for (Field field : type.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers()) && !field.isSynthetic()) {
                    str.append("\n").append(indent).append(field.getName()).append(": ");
                    try {
                        field.setAccessible(true);
                        Object value = field.get(this);
                        if (value instanceof ExtensibleImpl) {
                            ((ExtensibleImpl<?>)value).toString(str, indent + "\t");
                        } else {
                            str.append(value);
                        }
                    } catch (IllegalArgumentException | IllegalAccessException e) {
                        str.append("<failure:").append(e.getMessage()).append(">");
                    }
                }
            }
            type = type.getSuperclass();
        }
    }
}