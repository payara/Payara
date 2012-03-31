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
                int num = 0;
                for (String oneApp : appsNameList) {
                    String contextRoot = (String) RestUtil.getAttributesMap(REST_URL + "/applications/application/" + oneApp).get("contextRoot");
                    Map aMap = new HashMap();
                    aMap.put("appName", oneApp);
                    aMap.put("contextRoot", contextRoot);
                    aMap.put("appnum", ""+num);
                    num++;
/*                    
                    List<String> urls = getLaunchUrls(oneApp);
                    if (urls.size()>0){
                        aMap.put("url", urls.get(0));
                    }
* 
*/
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


    public String undeploy(String appName){
        try{
            RestUtil.restRequest(REST_URL+"/applications/application/"+appName, null, "DELETE", null, null, true);
            return ("/env/environments");
        }catch(Exception ex){
            return null;
        }
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
/*
    public List<String> getLaunchUrls(String appName) {
        return DeployUtil.getApplicationURLs(appName);
    }
*/
    public String getDummy(){
        instances = null;
        instanceNames = null;
        return dummy;
    }

    public void setDummy(String s){
        instances = null;
        instanceNames = null;
    }

    private final static String REST_URL = "http://localhost:4848/management/domain";

}
