/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.glassfish.admin.rest.resources;

import java.util.Map;
import java.util.Properties;
import org.glassfish.admin.rest.composite.RestModel;

/**
 *
 * @author jdlee
 */
public interface CommandResult extends RestModel {
    String getMessage();
    void setMessage(String message);
    Properties getProperties();
    void setProperties(Properties props);
    Map<String, Object> getExtraProperties();
    void setExtraProperties(Map<String, Object> props);
}
