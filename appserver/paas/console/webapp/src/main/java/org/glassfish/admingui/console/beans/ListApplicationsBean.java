/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.glassfish.admingui.console.beans;

import java.io.Serializable;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import java.util.*;
import org.glassfish.admingui.console.rest.RestUtil;
import org.glassfish.admingui.console.util.CommandUtil;
import org.glassfish.admingui.console.util.DeployUtil;
import javax.faces.context.FacesContext;
import org.apache.myfaces.trinidad.event.PollEvent;

@ManagedBean(name="listApplicationsBean")
@ViewScoped
public class ListApplicationsBean {
    private Map appData;
    private List<Map> apps = null;
    private boolean modelUpdated = false;

    private void ensureModel() {
        if (!modelUpdated) {
            updateModel();
            modelUpdated = true;
        }
    }

    public List<Map> getApplications() {
        ensureModel();
        return apps;
    }

    private void updateModel() {
        apps = new ArrayList();
        Map sessionMap = FacesContext.getCurrentInstance().getExternalContext().getSessionMap();
        List<String> deployingApps = (List) sessionMap.get("_deployingApps");
        String endPoint = "http://localhost:4848/management/domain/applications/list-applications";
        Map attrs = new HashMap();
        attrs.put("target", "domain");  //specify domain to get Paas deployed app.
        Map appData = (Map) RestUtil.restRequest(endPoint, attrs, "GET", null, null, false, true).get("data");
        Map<String, String> props = (Map) appData.get("properties");
        if (props != null) {
            for (String appName : props.keySet()) {
                // FIXME - eventually the app would be a system app, so this check would be  not be needed. This
                // is temporary.
                if ("paas-console".equals(appName))
                    continue;
                Map app = new HashMap();
                app.put("appName", appName);
                app.put("notExist", false);
                app.put("environment", getEnvironment(appName));
                apps.add(app);

                if (deployingApps != null && deployingApps.contains(appName)) {
                    deployingApps.remove(appName);
                }
            }
        }
        if (deployingApps != null) {
            for (String one : deployingApps) {
                Map app = new HashMap();
                app.put("appName", one);
                app.put("notExist", true);
                apps.add(app);
            }
        }
    }
    
    public void onPoll(PollEvent e) {
        Map sessionMap = FacesContext.getCurrentInstance().getExternalContext().getSessionMap();
        sessionMap.remove("_deployingApps");
        updateModel();
    }
    


    private String getEnvironment(String appName) {
        List<String> envList = DeployUtil.getApplicationEnvironments(appName);
        if (envList.size() > 0) return envList.get(0);
        return "";
    }
}
