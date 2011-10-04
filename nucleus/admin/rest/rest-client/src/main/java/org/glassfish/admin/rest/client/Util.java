/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.glassfish.admin.rest.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/**
 *
 * @author jdlee
 */
public class Util {
    
    public static Map<String, Object> processJsonMap(String json) {
        Map<String, Object> map;
        try {
            map = processJsonObject(new JSONObject(json));
        } catch (JSONException e) {
            map = new HashMap();
        }
        return map;
    }

    public static Map processJsonObject(JSONObject jo) {
        Map<String, Object> map = new HashMap<String, Object>();
        try {
            Iterator i = jo.keys();
            while (i.hasNext()) {
                String key = (String) i.next();
                Object value = jo.get(key);
                if (value instanceof JSONArray) {
                    map.put(key, processJsonArray((JSONArray) value));
                } else if (value instanceof JSONObject) {
                    map.put(key, processJsonObject((JSONObject) value));
                } else {
                    map.put(key, value);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        return map;
    }

    public static List processJsonArray(JSONArray ja) {
        List results = new ArrayList();

        try {
            for (int i = 0; i < ja.length(); i++) {
                Object entry = ja.get(i);
                if (entry instanceof JSONArray) {
                    results.add(processJsonArray((JSONArray) entry));
                } else if (entry instanceof JSONObject) {
                    results.add(processJsonObject((JSONObject) entry));
                } else {
                    results.add(entry);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        return results;
    }

}
