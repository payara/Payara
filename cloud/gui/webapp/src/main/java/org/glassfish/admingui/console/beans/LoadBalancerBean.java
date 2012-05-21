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