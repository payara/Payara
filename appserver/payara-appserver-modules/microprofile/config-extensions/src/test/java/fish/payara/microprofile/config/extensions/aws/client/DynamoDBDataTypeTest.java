/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2020-2021] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.microprofile.config.extensions.aws.client;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;

import org.junit.Test;

import fish.payara.microprofile.config.extensions.dynamodb.DynamoDBConfigSource;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DynamoDBDataTypeTest {

    private final static String KEY_ATTRIBUTE_NAME = "keyAttribute";
    private final static String VALUE_ATTRIBUTE_NAME = "valueAttribute";
    private final static String KEY_ATTRIBUTE_VALUE = "keyData";
    private final static String VALUE_ATTRIBUTE_VALUE = "valueData";

    @Test
    public void testAttributeWithStringType() {
        Map<String, String> stringTypeResult = DynamoDBConfigSource.readDataFromItems(getItems(getAttribute("S", "S")), KEY_ATTRIBUTE_NAME, VALUE_ATTRIBUTE_NAME);
        assertEquals(VALUE_ATTRIBUTE_VALUE, stringTypeResult.get(KEY_ATTRIBUTE_VALUE));
    }

    @Test
    public void testAttributeWithBinaryType() {
        Map<String, String> stringTypeResult = DynamoDBConfigSource.readDataFromItems(getItems(getAttribute("B", "B")), KEY_ATTRIBUTE_NAME, VALUE_ATTRIBUTE_NAME);
        assertEquals(VALUE_ATTRIBUTE_VALUE, stringTypeResult.get(KEY_ATTRIBUTE_VALUE));
    }

    @Test
    public void testAttributeWithBooleanType() {
        Map<String, String> stringTypeResult = DynamoDBConfigSource.readDataFromItems(getItems(getAttribute("BOOL", "BOOL")), KEY_ATTRIBUTE_NAME, VALUE_ATTRIBUTE_NAME);
        assertEquals(VALUE_ATTRIBUTE_VALUE, stringTypeResult.get(KEY_ATTRIBUTE_VALUE));
    }

    @Test
    public void testAttributeWithNumberType() {
        Map<String, String> stringTypeResult = DynamoDBConfigSource.readDataFromItems(getItems(getAttribute("N", "N")), KEY_ATTRIBUTE_NAME, VALUE_ATTRIBUTE_NAME);
        assertEquals(VALUE_ATTRIBUTE_VALUE, stringTypeResult.get(KEY_ATTRIBUTE_VALUE));
    }

    @Test
    public void testAttributeWithListType() {
        assertTrue(DynamoDBConfigSource.readDataFromItems(getItems(getAttribute("L", "L")), KEY_ATTRIBUTE_NAME, VALUE_ATTRIBUTE_NAME).isEmpty());
    }

    @Test
    public void testAttributeStringSetType() {
        assertTrue(DynamoDBConfigSource.readDataFromItems(getItems(getAttribute("SS", "SS")), KEY_ATTRIBUTE_NAME, VALUE_ATTRIBUTE_NAME).isEmpty());
    }

    private static JsonArray getItems(JsonObject jsonObject) {
        return Json.createArrayBuilder()
                .add(jsonObject)
                .build();
    }

    private static JsonObject getAttribute(String keyAttributeDataType, String valueAttributeDataType) {
        return Json.createObjectBuilder()
                .add(KEY_ATTRIBUTE_NAME, Json.createObjectBuilder().add(keyAttributeDataType, KEY_ATTRIBUTE_VALUE))
                .add(VALUE_ATTRIBUTE_NAME, Json.createObjectBuilder().add(valueAttributeDataType, VALUE_ATTRIBUTE_VALUE))
                .build();
    }
}
