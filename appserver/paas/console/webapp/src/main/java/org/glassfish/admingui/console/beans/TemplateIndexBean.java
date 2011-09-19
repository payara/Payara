/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.glassfish.admingui.console.beans;

import java.io.Serializable;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import java.util.*;

@ManagedBean(name="templateIndexBean")
@SessionScoped
public class TemplateIndexBean {

    public TemplateIndexBean() {
    }

    public List<TemplateIndexes> getIndexes() {
        List<TemplateIndexes> indexes = new ArrayList<TemplateIndexes>();

        indexes.add(new TemplateIndexes("ServiceType", "JavaEE"));
        indexes.add(new TemplateIndexes("VirtualizationType", "OVM"));

        return indexes;
    }

    public static class TemplateIndexes {
        private String type;
        private String value;

        public TemplateIndexes(String type, String value) {
            this.type = type;
            this.value = value;
        }

        public String getType() {
            return type;
        }

        public String getValue() {
            return value;
        }

    }
}
