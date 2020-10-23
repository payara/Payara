/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.microprofile.config.extensions.aws.client;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;

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
