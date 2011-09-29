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
import java.util.ArrayList;
import javax.el.ELContext;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import org.apache.myfaces.trinidad.event.PollEvent;
import org.apache.myfaces.trinidad.model.ChartModel;
import org.glassfish.admingui.console.rest.RestUtil;

@ManagedBean(name = "memoryMonitorBean")
@SessionScoped
public class JVMMemoryMonitorBean {

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
            value.getJvmMemoryMonitoringStats();
        }
    }

    public void onPoll(PollEvent e) {
        if (value != null) {
            value.getJvmMemoryMonitoringStats();
        }
    }

    private class MyChartModel extends ChartModel {
        private List<String> _groupLabels = new ArrayList<String>();
        private List<String> _seriesLabels = new ArrayList<String>();
        private List<List<Double>> _chartYValues =  new ArrayList<List<Double>>();
        private Double _maxYValue = 5.0;

        public MyChartModel() {
            getJvmMemoryMonitoringStats();
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
            return "JVM Memory Usage Statistics";
        }

        private void getJvmMemoryMonitoringStats() {
            Map<String, Object> instanceMemoryData = new HashMap<String, Object>();
            Map<String, Object> result = null;
            String clusterName = getEnvName();
            List<String> instanceNames = MonitoringBean.getClusterInstances(clusterName);
            //Get the Monitoring statistics
            for (String instanceName : instanceNames) {
                result = null;
                String endPoint = "http://localhost:4848/monitoring/elasticity/domain/" + instanceName + "/jvm_memory.json";
                result = (Map<String, Object>) RestUtil.restRequest(endPoint, null, "GET", null, null, false, true).get("data");
                if (result != null) {
                    Map<String, Object> heapResultExtraProps = (Map<String, Object>) result.get("extraProperties");
                    if (heapResultExtraProps != null) {
                        Map<String, Object> heapResultEntity = (Map<String, Object>) heapResultExtraProps.get("entity");
                        if (heapResultEntity != null && !heapResultEntity.isEmpty()) {
                            Map<String, Map<String, Long>> heapResultProps = (Map<String, Map<String, Long>>) (heapResultEntity.get("heap"));
                            instanceMemoryData.put(instanceName, heapResultProps);
                            //System.out.println("JVM Memory Statistics for instance " + instanceName + " = " + heapResultProps.toString());
                        }
                    }
                }
            }
            normalizeMonitoringData(instanceMemoryData);
        }

        private void normalizeMonitoringData(Map<String, Object> data) {
            //sortedMap to prepare the data for chart.. time as key and list of instances values as value.
            SortedMap<Double, List<Double>> sortedMap = new TreeMap<Double, List<Double>>();
            _maxYValue = 5.0;
            _seriesLabels.clear();

            for (String inst : data.keySet()) {
                _seriesLabels.add(inst);
                Map<String, Map<String, Long>> heapResultProps = (Map<String, Map<String, Long>>) data.get(inst);
                for (String heapProp : heapResultProps.keySet()) {
                    Long time = Long.valueOf(heapProp);
                    Long usedValue = heapResultProps.get(heapProp).get("used");
                    List<Double> val = sortedMap.get(time.doubleValue());
                    if (val == null) {
                        val = new ArrayList<Double>();
                        val.add(usedValue.doubleValue());
                    } else {
                        val.add(usedValue.doubleValue());
                    }
                    sortedMap.put(time.doubleValue(), val);
                    if (usedValue.doubleValue() > _maxYValue) {
                        _maxYValue = usedValue.doubleValue() + 5;
                    }
                }
            }
            setMemoryMonitoringChartInfo(sortedMap);            
            //System.out.println("JVM Memory Monitoring data for chart= "+sortedMap.toString());
        }

        private void setMemoryMonitoringChartInfo(SortedMap<Double, List<Double>> data) {
            _groupLabels.clear();
            _chartYValues.clear();
            int instanceCount = _seriesLabels.size();
            for (Double time : data.keySet()) {
                List<Double> usedValList = data.get(time);
                if (usedValList.size() == instanceCount) {
                    setGroupLabel(time.longValue());
                    _chartYValues.add(usedValList);
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
    }
}
