/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.glassfish.admingui.console.beans;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.faces.bean.*;
import org.glassfish.admingui.console.rest.RestUtil;
import org.glassfish.admingui.console.util.CommandUtil;

@ManagedBean(name="loadBalancerBean")
@ViewScoped
public class LoadBalancerBean {

    private String httpPort = null;
    private String sslEnabled = null;
    private String httpsPort = null;

    @ManagedProperty(value="#{environmentBean.envName}")
    private String envName;

    @ManagedProperty(value="#{environmentBean.applicationName}")
    private String applicationName;
    
    public LoadBalancerBean() {
    }

    public String getEnvName(){
        return envName;
    }

    public void setEnvName(String envN)
    {
        envName = envN;
    }

    public void setApplicationName(String apn){
        applicationName = apn;
    }

    private void initInfo(){
        Map lbProps = new HashMap();
        List<Map> services = CommandUtil.listServices(applicationName, "load_balancer", "application");
        if (services.size() <= 0){
            return;
        }
        String serviceName = (String) services.get(0).get("SERVICE-NAME");
        Map attrs = new HashMap();
        attrs.put("appname", applicationName);
        attrs.put("servicename", serviceName );
        String endpoint = REST_URL+"/applications/_get-service-description";
        try{
            Map responseMap = RestUtil.restRequest( endpoint , attrs, "GET" , null, null, false, true);
            Map extraPropertiesMap = (Map)((Map)responseMap.get("data")).get("extraProperties");
            if (extraPropertiesMap != null){
                lbProps = (Map)extraPropertiesMap.get("list");
            }

            if (lbProps != null) {
                Map configuration = (Map) lbProps.get("configurations");
                if (configuration != null) {
                    httpPort = (String) configuration.get("http-port");
                    sslEnabled = (String) configuration.get("ssl-enabled");
                    httpsPort = (String) configuration.get("https-port");
                }
            }
        }catch (Exception ex){
            System.out.println("======== error in calling _get-service-description");
        }
    }

    public String getHttpPort() {
        if (httpPort == null){
            initInfo();
        }
        return httpPort;
    }

    public String getSslEnabled() {
        if (sslEnabled == null){
            initInfo();
        }
        return sslEnabled;
    }

    public String getHttpsPort() {
        if (httpsPort == null){
            initInfo();
        }
        return httpsPort;
    }

    private final static String REST_URL = "http://localhost:4848/management/domain";
}