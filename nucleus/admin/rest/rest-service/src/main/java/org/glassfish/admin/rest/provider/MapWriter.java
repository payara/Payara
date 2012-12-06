/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.glassfish.admin.rest.provider;

import java.util.Map;
import java.util.logging.Level;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.glassfish.admin.rest.RestServiceLoggingInfo;

/**
 *
 * @author jdlee
 */
@Produces({MediaType.APPLICATION_JSON, "application/x-javascript"})
public class MapWriter extends BaseProvider<Map> {
    public MapWriter() {
        super(Map.class, MediaType.APPLICATION_JSON_TYPE);

    }

    @Override
    public String getContent(Map proxy) {
        String json = null;
        try {
            json = new JSONObject(proxy).toString(getFormattingIndentLevel());
        } catch (JSONException ex) {
            RestServiceLoggingInfo.restLogger.log(Level.SEVERE, null, ex);
        }
        return json;
    }

}
