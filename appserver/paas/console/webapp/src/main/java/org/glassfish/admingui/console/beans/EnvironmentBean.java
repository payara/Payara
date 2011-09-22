/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.glassfish.admingui.console.beans;

import java.io.Serializable;
import javax.faces.bean.*;
import javax.faces.context.FacesContext;
import java.util.*;
import org.glassfish.admingui.console.rest.RestUtil;
import org.glassfish.admingui.console.util.DeployUtil;


@ManagedBean(name="environmentBean")
@ViewScoped
public class EnvironmentBean implements Serializable {

    private String envName;
    //public List<String> URLs;
    private List<Map> applications = null;
    private List<Map> instances = null;
    private List<String> instanceNames = null;
    private String minScale = "1";
    private String maxScale = "4";
    private String dummy = "";


    public EnvironmentBean() {
        Map requestMap = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
        envName = (String)requestMap.get("envName");
    }

    public String getEnvName(){
        return envName;
    }

    public EnvironmentBean(String envName) {
        this.envName = envName;
    }

    public List<Map> getApplications() {
        if (applications == null){
            applications = new ArrayList();
            try {
                List<String> appsNameList = RestUtil.getChildNameList(REST_URL + "/clusters/cluster/" + envName + "/application-ref");
                for (String oneApp : appsNameList) {
                    String contextRoot = (String) RestUtil.getAttributesMap(REST_URL + "/applications/application/" + oneApp).get("contextRoot");
                    Map aMap = new HashMap();
                    aMap.put("appName", oneApp);
                    aMap.put("contextRoot", contextRoot);
                    List<String> urls = getLaunchUrls(oneApp);
                    if (urls.size()>0){
                        aMap.put("url", urls.get(0));
                    }
                    applications.add(aMap);
                }
            } catch (Exception ex) {
            }
        }
        return applications;
    }


    public String getApplicationName(){
        return (String) getApplications().get(0).get("appName");
    }


    //For instances, we want this to get call everytime the page is loaded.  Since this is in a tab set, so, even when declared @viewScope
    //this bean will still be cached.  However, since this is for generating table data,  this method will get called multipe times when the
    //page is loaded.  We want to avoid that too.
    
    public List<Map> getInstances() {
        if (instances == null){
            try {
                instances = new ArrayList();
                Map attrs = new HashMap();
                attrs.put("whichtarget", envName);
                List<Map> iList =  RestUtil.getListFromREST(REST_URL+"/list-instances", attrs, "instanceList");
                int num=0;
                for(Map oneI : iList){
                    if("running".equals(((String)oneI.get("status")).toLowerCase())){
                        oneI.put("statusImage", "/images/running_small.gif");
                        oneI.put("actionImage", "/images/stop-instance.png");
                        oneI.put("action", "stop-instance");
                        oneI.put("confirmMsg", "Instance will be stopped, continue ? ");
                        oneI.put("shortDesc", "Stop Instance");
                        oneI.put("num", ""+num);
                        num++;
                    }else{
                        oneI.put("statusImage", "/images/not-running_small.png");
                        oneI.put("actionImage", "/images/start-instance.png");
                        oneI.put("action", "start-instance");
                        oneI.put("confirmMsg", "Instance will be started, continue ? ");
                        oneI.put("shortDesc",  "Start Instance");
                        oneI.put("num", ""+num);
                        num++;
                    }
                    instances.add(oneI);
                }
            } catch (Exception ex) {
            }
        }
        return instances;
    }

    public List<String> getInstanceNames(){
        if (instanceNames == null){
            instanceNames = new ArrayList();
            List<Map> imap = this.getInstances();
            for(Map oneInstance : imap){
                instanceNames.add((String)oneInstance.get("name"));
            }
        }
        return instanceNames;
    }

    public void instanceAction(String action, String instanceName){
        System.out.println("------------- action = " + REST_URL+"/servers/server/"+instanceName+"/"+action);
        try{
            RestUtil.restRequest(REST_URL+"/servers/server/"+instanceName+"/"+action, null , "POST", null, null, false);
        }catch(Exception ex){
        }
        instances = null;
    }


    public String getMinScale(){

        String elasticEndpoint = REST_URL + "/elastic-services/elasticservice/" + envName;
        if (RestUtil.doesProxyExist(elasticEndpoint)){
            minScale = (String) RestUtil.getAttributesMap(elasticEndpoint).get("min");
        }
        return minScale;
    }

    public String getMaxScale(){
        String elasticEndpoint = REST_URL + "/elastic-services/elasticservice/" + envName;
        if (RestUtil.doesProxyExist(elasticEndpoint)){
            maxScale = (String) RestUtil.getAttributesMap(elasticEndpoint).get("max");
        }
        return maxScale;
    }

    public List<String> getLaunchUrls(String appName) {
        return DeployUtil.getApplicationURLs(appName);
    }

    public String getDummy(){
        instances = null;
        instanceNames = null;
        return dummy;
    }

    private final static String REST_URL = "http://localhost:4848/management/domain";

}
