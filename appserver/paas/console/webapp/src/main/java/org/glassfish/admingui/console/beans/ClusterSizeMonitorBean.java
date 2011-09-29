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

import java.text.DateFormat;
import javax.faces.bean.ManagedBean;
import java.util.*;
import javax.el.ELContext;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import org.apache.myfaces.trinidad.event.PollEvent;
import org.apache.myfaces.trinidad.model.ChartModel;
import org.glassfish.admingui.console.rest.RestUtil;

@ManagedBean(name = "clusterSizeMonitorBean")
@SessionScoped
public class ClusterSizeMonitorBean {

    private final int MAX_SIZE = 15;
    private MyChartModel value = new MyChartModel();
    private String envName = null;

    public String getEnvName() {
        if (envName == null) {
            ELContext elContext = FacesContext.getCurrentInstance().getELContext();
            EnvironmentBean eBean = (EnvironmentBean) FacesContext.getCurrentInstance().getApplication().getELResolver().getValue(elContext, null, "environmentBean");
            envName = eBean.getEnvName();
        }
        return envName;
    }

    public ChartModel getValue() {
        return value;
    }

    public void onRefresh(ActionEvent e) {
        if (value != null) {
            value.getClusterSizeMonitoringStats();
        }
    }

    public void onPoll(PollEvent e) {
        if (value != null) {
            value.getClusterSizeMonitoringStats();
        }
    }

    private class ClusterSizeStat {

        Long instanceCount;
        Long time;

        public ClusterSizeStat(Long instanceCount, Long time) {
            this.instanceCount = instanceCount;
            this.time = time;
        }

        public Long getInstanceCount() {
            return instanceCount;
        }

        public void setInstanceCount(Long instanceCount) {
            this.instanceCount = instanceCount;
        }

        public Long getTime() {
            return time;
        }

        public void setTime(Long time) {
            this.time = time;
        }
    }

    private class MyChartModel extends ChartModel {

        private List<String> _groupLabels = new ArrayList<String>();
        private List<String> _seriesLabels = Arrays.asList(new String[]{getEnvName()});
        private List<List<Double>> _chartYValues;
        private Double _maxYValue = 5.0;

        public MyChartModel() {
            _chartYValues = new ArrayList<List<Double>>();
            getClusterSizeMonitoringStats();
        }

        @Override
        public List<String> getSeriesLabels() {
            return _seriesLabels;
        }

        @Override
        public List<String> getGroupLabels() {
            return _groupLabels;
        }

        @Override
        public List<List<Double>> getXValues() {
            return null;
        }

        @Override
        public List<List<Double>> getYValues() {
            return _chartYValues;
        }

        @Override
        public Double getMaxYValue() {
            return _maxYValue;
        }

        @Override
        public Double getMinYValue() {
            return 0.0;
        }

        @Override
        public String getTitle() {
            return "Cluster Instance Count Statistics";
        }

        @Override
        public String getSubTitle() {
            return "";
        }

        @Override
        public String getFootNote() {
            return "";
        }

        private void getClusterSizeMonitoringStats() {
            List<ClusterSizeStat> heapData = new ArrayList<ClusterSizeStat>();
            SortedMap<Long, Long> sortedMap = new TreeMap<Long, Long>();
            Map<String, Object> result = null;
            String clusterName = getEnvName();
            //To get the instance Name of a cluster that is running
//            String instanceName = getClusterInstanceName(clusterName);
//            if (instanceName.equals("")) {
//                return;
//            }
            //Get the Monitoring statistics
            result = null;
            String endPoint = "http://localhost:4848/monitoring/elasticity/domain/server/cluster_instance_size/" + clusterName + ".json";
            result = (Map<String, Object>) RestUtil.restRequest(endPoint, null, "GET", null, null, false, true).get("data");
            if (result != null) {
                Map<String, Object> heapResultExtraProps = (Map<String, Object>) result.get("extraProperties");
                if (heapResultExtraProps != null) {
                    Map<String, Object> heapResultEntity = (Map<String, Object>) heapResultExtraProps.get("entity");
                    if (heapResultEntity != null && !heapResultEntity.isEmpty()) {
                        Map<String, Map<String, Long>> heapResultProps = (Map<String, Map<String, Long>>) (heapResultEntity.get("count"));
                        for (String heapProp : heapResultProps.keySet()) {
                            sortedMap.put(Long.valueOf(heapProp), heapResultProps.get(heapProp).get("instanceCount"));
                        }
                    }
                }
            }
            setClusterSizeMonitoringChartInfo(sortedMap);
            System.out.println("Cluster Size Monitoring data for chart= "+sortedMap.toString());
        }

        private void setClusterSizeMonitoringChartInfo(SortedMap<Long, Long> data) {
            _groupLabels.clear();
            _chartYValues.clear();
            _maxYValue = 5.0;
            for (Long time : data.keySet()) {
                Double instanceCount = data.get(time).doubleValue();
                setGroupLabel(time);
                _chartYValues.add(Arrays.asList(new Double[]{instanceCount}));
                if (instanceCount > _maxYValue) {
                    _maxYValue = instanceCount + 5;
                }
            }
            if (_chartYValues.size() > MAX_SIZE) {
                _chartYValues = _chartYValues.subList(_chartYValues.size() - MAX_SIZE, _chartYValues.size());
                _groupLabels = _groupLabels.subList(_groupLabels.size() - MAX_SIZE, _groupLabels.size());
            }
        }

        private void setGroupLabel(Long time) {
            if (_groupLabels.isEmpty() || _groupLabels.size() % 3 == 0) {
                _groupLabels.add(DateFormat.getInstance().format(new Date(time)));
            } else {
                _groupLabels.add("");
            }
        }

        private String getClusterInstanceName(String cluster) {
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
                                return (String) instanceInfo.get("name");
                            }
                        }
                    }
                }
            }
            return "";
        }
    }
}
