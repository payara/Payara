/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.glassfish.admin.rest;

import java.lang.*;import java.lang.Class;import java.lang.Exception;import java.lang.Object;import java.lang.String;import java.lang.System;import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.glassfish.admin.rest.composite.CompositeUtil;
import org.glassfish.admin.rest.composite.RestModel;
import org.glassfish.admin.rest.model.BaseModel;
import org.glassfish.admin.rest.readers.JsonPojoProvider;
import org.junit.Test;

/**
 *
 * @author jdlee
 */
public class CompositeUtilTest {
    private static String json =
            "{\"name\":\"testModel\",\"count\":123, \"related\":[{\"id\":\"rel1\", \"description\":\"description 1\"},{\"id\":\"rel2\", \"description\":\"description 2\"}]}";

    @Test
    public void readInJson() throws Exception {
//        JsonPojoProvider p = new JsonPojoProvider();
        JSONObject o = new JSONObject(json);
        BaseModel model = CompositeUtil.hydrateClass(BaseModel.class, o);

        Assert.assertEquals("testModel", model.getName());
        Assert.assertEquals(2, model.getRelated().size());
        Assert.assertTrue(model.getRelated().get(0).getDescription().startsWith("description "));
    }
}
