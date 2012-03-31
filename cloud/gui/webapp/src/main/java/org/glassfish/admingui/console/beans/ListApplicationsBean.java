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
