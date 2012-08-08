/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.glassfish.admin.rest.composite.metadata;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/**
 *
 * @author jdlee
 */
public class RestResourceMetadata {
    List<RestMethodMetadata> resourceMethods = new ArrayList<RestMethodMetadata>();
    public RestResourceMetadata(Class<?> resource) {
        processClass(resource);
    }

    private void processClass(Class<?> resource) {
        for (Method m : resource.getDeclaredMethods()) {
            Annotation designator = getMethodDesignator(m);

            if (designator != null) {
                RestMethodMetadata rmm = new RestMethodMetadata(m, designator);
                resourceMethods.add(rmm);
            }
        }
    }

    private Annotation getMethodDesignator(Method method) {
        Annotation a = method.getAnnotation(GET.class);
        if (a == null) {
            a = method.getAnnotation(POST.class);
            if (a == null) {
                a = method.getAnnotation(DELETE.class);
                if (a == null) {
                    a = method.getAnnotation(OPTIONS.class);
                }
            }
        }

        return a;
    }

    @Override
    public String toString() {
        return "RestResourceMetadata{" + "resources=" + resourceMethods + '}';
    }

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        final JSONArray array = new JSONArray();
        for (RestMethodMetadata rmd : resourceMethods) {
            array.put(rmd.toJson());
        }

        o.put("resourceMethods", array);

        return o;
    }
}
