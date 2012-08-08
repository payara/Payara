/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.glassfish.admin.rest.composite.metadata;

import java.lang.annotation.Annotation;
import javax.ws.rs.QueryParam;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.glassfish.admin.rest.composite.CompositeUtil;

/**
 *
 * @author jdlee
 */
public class ParamMetadata {

    private String name;
    private String type;
    private String help;

    public ParamMetadata(Class<?> paramType, QueryParam qp, Annotation[] others) {
        name = qp.value();
        type = paramType.getSimpleName();

        help = CompositeUtil.instance().getHelpText(others);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getHelp() {
        return help;
    }

    public void setHelp(String help) {
        this.help = help;
    }

    @Override
    public String toString() {
        return "ParamMetadata{" + "name=" + name + ", type=" + type + ", help=" + help + '}';
    }

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("name", name);
        o.put("type", type);
        o.put("help", help);

        return o;
    }
}
