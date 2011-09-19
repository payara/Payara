/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.glassfish.admingui.console.beans;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.faces.application.FacesMessage;
import javax.faces.bean.*;
import javax.faces.context.FacesContext;

import org.glassfish.admingui.console.rest.RestUtil;


@ManagedBean(name="scaleBean")
@ViewScoped
public class ScaleBean implements Serializable {

    private String minScale = "1";
    private String maxScale = "2";
    private String elasticEndpoint = "";
    private String alertEndpoint = "";
    private String threshold = "80";
    private String sampleInterval = "5";
    private String enabled = "true";
    private String alertName=null;

    @ManagedProperty(value="#{environmentBean.envName}")
    private String envName;


    public ScaleBean() {
    }

    public String getEnvName(){
        return envName;
    }

    public void setEnvName(String en){
        envName = en;
        elasticEndpoint = REST_URL + "/elastic-services/elasticservice/" + envName;
        alertEndpoint = elasticEndpoint + "/alerts/alert/";
        try{
            Map elasticMap = RestUtil.getAttributesMap(elasticEndpoint);
            if (elasticMap.size()>0){
                minScale = (String) elasticMap.get("min");
                maxScale = (String) elasticMap.get("max");
            }
            List<String> alertList = RestUtil.getChildNameList(alertEndpoint);
            if (alertList.size()>0){
                alertName = alertList.get(0);
                Map payload = new HashMap();
                payload.put("serviceName", envName);
                payload.put("alertName", alertName);
                Map data = (Map) RestUtil.restRequest( REST_URL  + "/elastic-services/describe-memory-alert", payload, "GET", null, null, false, true).get("data");
                Map Props = (Map) data.get("properties");
                threshold =  (String) Props.get("threshold");
                enabled = (String) Props.get("enabled");
                sampleInterval = (String) Props.get("sampleInterval");
            }else{
                alertName=envName + "-memory-alert";
                //take defaults
            }
            
        }catch(Exception ex){
            
        }
    }

    public String getMinScale(){
        return minScale;
    }

    public void setMinScale(String mm) {
        minScale = mm;
    }

    public String getMaxScale(){
        return maxScale;
    }

    public void setMaxScale(String mm) {
        maxScale = mm;
    }

    
    public String getAlertName(){
        return alertName;
    }

    
    public String getSampleInterval(){
        return sampleInterval;
    }

    public void setSampleInterval(String sample) {
        sampleInterval = sample;
    }

    public String getThreshold(){
        return threshold;
    }

    public void setThreshold(String thres) {
        threshold = thres;
    }

    public String getEnabled(){
        return enabled;
    }

    public void setEnabled(String en) {
        enabled = en;
    }

    public String saveScaling(){
        try{
            Map payload = new HashMap();
            payload.put("min", minScale);
            payload.put("max", maxScale);
            RestUtil.restRequest(elasticEndpoint, payload, "POST", null, null, true);

            Map payload2 = new HashMap();
            payload2.put("serviceName", envName);
            payload2.put("alertName", alertName);
            payload2.put("threshold", threshold);
            payload2.put("sample-interval", sampleInterval);
            payload2.put("enabled", enabled);
            RestUtil.restRequest(REST_URL +"/elastic-services/create-memory-alert", payload2, "POST", null, null, true);
            FacesContext.getCurrentInstance().addMessage("saveButton",
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Saved successfully."));

        }catch(Exception ex){
            FacesContext.getCurrentInstance().addMessage("saveButton",
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error",  ex.getMessage()));
        }
        return null;
    }

    private final static String REST_URL = "http://localhost:4848/management/domain";

}
