/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU 
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You 
 * may not use this file except in compliance with the License.  You can 
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL 
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
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
