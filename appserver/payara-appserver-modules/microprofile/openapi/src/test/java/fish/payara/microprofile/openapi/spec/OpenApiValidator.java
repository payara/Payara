/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2019] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.microprofile.openapi.spec;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fish.payara.microprofile.openapi.test.util.JsonUtils;
import static java.util.Arrays.asList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import static org.junit.Assert.fail;

/**
 * Uses the structural model described by {@link NodeType} to check a {@link JsonNode} document against the expected
 * structure of a particular {@link NodeType}.
 * 
 * This check is rough as most fields are optional (or optional under certain conditions which is just treated as fully
 * optional) but this does detect malformed structures and illegal fields being used.
 */
public class OpenApiValidator {

    private static final EnumMap<NodeType, Set<String>> REQUIRED_FIELDS = new EnumMap<>(NodeType.class);

    private static Set<String> requiredFields(NodeType type) {
        return new HashSet<>(REQUIRED_FIELDS.computeIfAbsent(type, key -> key.fields()
                .filter(field -> field.isRequired())
                .map(field -> field.getName())
                .collect(Collectors.toSet())));
    }

    public static void validate(JsonNode document) {
        try {
            validate(NodeType.OpenAPI, document);
        } catch (AssertionError ex) {
            throw new AssertionError("Invalid OpenAPI Document: " + describe(document), ex);
        }
    }

    public static void validate(NodeType expected, JsonNode actual) {
        // do we have right type of JSON object?
        if (expected == NodeType.string) {
            assertJsonType(JsonNodeType.STRING, asList(expected), actual);
        } else if (expected == NodeType.number) {
            assertJsonType(JsonNodeType.NUMBER, asList(expected), actual);
        } else if (expected == NodeType.bool) {
            assertJsonType(JsonNodeType.BOOLEAN, asList(expected), actual);
        } else {
            assertJsonType(JsonNodeType.OBJECT, asList(expected), actual);
            // are the fields ok?
            ObjectNode obj = (ObjectNode) actual;
            Iterator<Entry<String, JsonNode>> iter = obj.fields();
            Set<String> requiredFields = requiredFields(expected);
            while (iter.hasNext()) {
                Entry<String, JsonNode> actualField = iter.next();
                String fieldName = actualField.getKey();
                if (fieldName.startsWith("x-")) {
                    if (!expected.isExtensible()) {
                        fail("Disallowed extension  " + describeField(fieldName) + " for " + expected + " node in "
                                + describe(actual));
                    }
                } else {
                    requiredFields.remove(fieldName);
                    Field expectedField = expected.getField(fieldName);
                    if (expectedField == null) {
                        fail("Disallowed " + describeField(fieldName) + " for " + expected + " node in "
                                + describe(actual));
                    }
                    validate(expectedField, actualField.getValue());
                }
            }
            if (!requiredFields.isEmpty()) {
                fail("Missing required fields " + requiredFields + " for " + expected + " node in "
                        + describe(actual));
            }
        }
    }

    public static void validate(Field expected, JsonNode actual) {
        if (expected.isArray()) {
            assertJsonType(JsonNodeType.ARRAY, null, actual);
            validateArray(expected, (ArrayNode) actual);
            return;
        }
        if (expected.isMap()) {
            assertJsonType(JsonNodeType.OBJECT, null, actual);
            validateMap(expected, (ObjectNode) actual);
            return;
        }
        validateTypes(expected, actual);
    }

    private static void validateMap(Field expected, ObjectNode actual) {
        for (JsonNode field : actual) {
            validateTypes(expected, field);
        }
        // Note: the keys can be anything in general
        // while certain maps might have special constraints these are not validated yet
    }

    private static void validateArray(Field expected, ArrayNode actual) throws AssertionError {
        for (JsonNode element : actual) {
            validateTypes(expected, element);
        }
    }

    private static void validateTypes(Field expected, JsonNode actual) throws AssertionError {
        if (expected.isAnyType()) {
            return; // anything is fine, no validation required
        }
        Map<NodeType, AssertionError> errors = new LinkedHashMap<>();
        for (NodeType expectedElementType : expected) {
            try {
                validate(expectedElementType, actual);
                return; // found a valid type
            } catch (AssertionError e) {
                errors.put(expectedElementType, e);
                // try next
            }
        }
        // none worked
        if (errors.size() == 1) {
            throw errors.values().iterator().next();
        }
        StringBuilder message = new StringBuilder();
        message.append("Disallowed value for " + describeField(expected.getName()) + ": " + describe(actual));
        for (Entry<NodeType, AssertionError> error : errors.entrySet()) {
            message.append("\n\nTrying ").append(error.getKey()).append(" gave:\n").append(error.getValue().getMessage());
        }
        throw new AssertionError(message);
    }

    private static void assertJsonType(JsonNodeType expectedJsonType, Iterable<NodeType> expected, JsonNode actual) {
        if (actual.isNull()) {
            if (expectedJsonType != JsonNodeType.OBJECT) {
                fail("Disallowed null value for node of type" + expected + " in " + describe(actual));
            }
        } else if (actual.getNodeType() != expectedJsonType) {
            fail("Disallowed type " + actual.getNodeType() + " for " + expected + " in " + describe(actual));
        }
    }

    private static String describe(JsonNode node) {
        return JsonUtils.prettyPrint(node);
    }

    private static String describeField(String fieldName) {
        return "*".equals(fieldName) ? "patterned field" : "field `"+fieldName + "`";
    }
}
