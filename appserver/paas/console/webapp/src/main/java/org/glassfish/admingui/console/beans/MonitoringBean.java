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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;
import javax.faces.event.ActionEvent;

import org.apache.myfaces.trinidad.event.PollEvent;
import org.glassfish.admingui.console.rest.RestUtil;

@ManagedBean(name = "monitorBean")
@SessionScoped
public class MonitoringBean {

    int refreshEle = 0;

    @ManagedProperty(value = "#{clusterSizeMonitorBean}")
    private ClusterSizeMonitorBean clusterSizeBean;

    @ManagedProperty(value = "#{memoryMonitorBean}")
    private JVMMemoryMonitorBean memoryBean;

    @ManagedProperty(value = "#{sessionsMonitorBean}")
    private ActiveSessionsMonitorBean sessionBean;
    
    @ManagedProperty(value = "#{processingTimeMonitorBean}")
    private ProcessingTimeMonitorBean processTimeBean;

    private Integer pollInterval = 5;

    public void setClusterSizeBean(ClusterSizeMonitorBean clusterSizeBean) {
        this.clusterSizeBean = clusterSizeBean;
    }

    public void setMemoryBean(JVMMemoryMonitorBean memoryBean) {
        this.memoryBean = memoryBean;
    }

    public void setProcessTimeBean(ProcessingTimeMonitorBean processTimeBean) {
        this.processTimeBean = processTimeBean;
    }

    public void setSessionBean(ActiveSessionsMonitorBean sessionBean) {
        this.sessionBean = sessionBean;
    }    

    public void onRefresh(ActionEvent e) {
        clusterSizeBean.onRefresh(e);
        memoryBean.onRefresh(e);
        sessionBean.onRefresh(e);
        processTimeBean.onRefresh(e);
    }

    public void onPoll(PollEvent e) {
        clusterSizeBean.onPoll(e);
        memoryBean.onPoll(e);
        sessionBean.onPoll(e);
        processTimeBean.onPoll(e);
    }

    public Integer getPollInterval() {
        return pollInterval;
    }

    public void setPollInterval(Integer pollInterval) {
        this.pollInterval = pollInterval;
    }

    public static List<String> getClusterInstances(String cluster) {
        List<String> instanceNames = new ArrayList<String>();
        String endPoint = "http://localhost:4848/management/domain/clusters/cluster/" + cluster + "/list-instances.json";
        Map<String, Object> result = (Map<String, Object>) RestUtil.restRequest(endPoint, null, "GET", null, null, false, true).get("data");
        if (result != null) {
            Map<String, Object> heapResultExtraProps = (Map<String, Object>) result.get("extraProperties");
            if (heapResultExtraProps != null) {
                List<Map<String, Object>> instancesDeatils = (List<Map<String, Object>>) heapResultExtraProps.get("instanceList");
                if (instancesDeatils != null && !instancesDeatils.isEmpty()) {
                    for (Map<String, Object> instanceInfo : instancesDeatils) {
                        String status = (String) instanceInfo.get("status");
                        if (status.equals("RUNNING")) {
                            instanceNames.add((String) instanceInfo.get("name"));
                        }
                    }
                }
            }
        }
        return instanceNames;
    }
}
