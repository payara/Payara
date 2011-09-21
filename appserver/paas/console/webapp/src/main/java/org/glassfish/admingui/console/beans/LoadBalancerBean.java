/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.glassfish.admingui.console.beans;

import javax.faces.bean.*;
import org.glassfish.admingui.console.rest.RestUtil;
import java.util.*;
import org.glassfish.admingui.console.util.GuiUtil;

@ManagedBean(name="loadBalancerBean")
@ViewScoped
public class LoadBalancerBean {

    private String httpPort;
    private String sslEnabled;
    private String httpsPort;
    
    public LoadBalancerBean() {
        httpPort = "";
        sslEnabled = "";
        httpsPort = "";
        String envName = "basic-db";
        String appName = null;
        String svcName = null;
        List<Map> applications = (new EnvironmentBean(envName)).getApplications();
        if (applications.size() > 0) {
            appName = (String)applications.get(0).get("appName");
            svcName = appName + "-lb";
        }
        Map attrs = new HashMap();
        attrs.put("appname", appName);
        attrs.put("id", svcName);
        String endpoint = REST_URL+"/applications/_get-service-description";
        try{
            Map responseMap = RestUtil.restRequest( endpoint , attrs, "GET" , null, null, false, true);
            Map extraPropertiesMap = (Map)((Map)responseMap.get("data")).get("extraProperties");
            Map result;
            if (extraPropertiesMap != null){
                result = (Map)extraPropertiesMap.get("list");
                if (result != null) {
                    Map configuration = (Map) result.get("configurations");
                    if (configuration != null) {
                        httpPort = (String) configuration.get("http-port");
                        sslEnabled = (String) configuration.get("ssl-enabled");
                        httpsPort = (String) configuration.get("https-port");
                    }
                }
            }
        }catch (Exception ex){
            GuiUtil.getLogger().severe("cannot List Services");
        }
    }

    public String getHttpPort() {
        return httpPort;
    }

    public String getSslEnabled() {
        return sslEnabled;
    }

    public String getHttpsPort() {
        return httpsPort;
    }

    private final static String REST_URL = "http://localhost:4848/management/domain";
}