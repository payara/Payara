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

import java.text.SimpleDateFormat;
import javax.faces.bean.ManagedBean;
import java.util.*;
import javax.faces.bean.SessionScoped;
import org.apache.myfaces.trinidad.event.PollEvent;
import org.apache.myfaces.trinidad.model.ChartModel;

@ManagedBean(name = "monitoringBean")
@SessionScoped
public class MonitoringChartBean {
    private final int MAX_SIZE = 10;
    private MyChartModel value = new MyChartModel();

    public ChartModel getValue() {        
        return value;
    }

    public void onPoll(PollEvent e) {
        if (value != null) { // && value.isUpdate()) {
            value.updateLabelsAndValues();
        }
    }
    
    private class MyChartModel extends ChartModel {
        private List<String> _groupLabels = new ArrayList<String>();
        private List<String> _seriesLabels = Arrays.asList(new String[]{"Request Count", "Error Count"});
        private List<List<Double>> _chartYValues;
        private List<List<Double>> _chartXValues;
        Date minDate, maxDate;

        public MyChartModel() {
            _chartYValues = new ArrayList<List<Double>>();
            _chartYValues.add(Arrays.asList(new Double[]{2.0, 1.0}));
            _chartYValues.add(Arrays.asList(new Double[]{5.0, 2.0}));
            _chartYValues.add(Arrays.asList(new Double[]{10.0, 4.0}));
            _chartYValues.add(Arrays.asList(new Double[]{15.0, 6.0}));
            _chartYValues.add(Arrays.asList(new Double[]{20.0, 2.0}));

            _chartXValues = new ArrayList<List<Double>>();
            setLabelsAndValues();
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
            return 40.0;
        }

        @Override
        public Double getMinYValue() {
            return 0.0;
        }

        @Override
        public Double getMaxXValue() {
            return Double.longBitsToDouble(maxDate.getTime());
        }

        @Override
        public Double getMinXValue() {
            return Double.longBitsToDouble(minDate.getTime());
        }

        @Override
        public String getTitle() {
            return "Request Monitoring Data";
        }

        @Override
        public String getSubTitle() {
            return "";
        }

        @Override
        public String getFootNote() {
            return "";
        }
        
        private void setLabelsAndValues() {
            Calendar cal = Calendar.getInstance();
            Date dt = new Date(2000, 7, 23, 10, 10, 10);
            minDate = dt;
            cal.setTime(dt);
            cal.add(Calendar.HOUR, 2);
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm");
            for (int i = 0; i < 5; i++) {
                cal.add(Calendar.HOUR, 2);
                String formattedStr = sdf.format(cal.getTime());
                _groupLabels.add(formattedStr);
                _chartXValues.add(Arrays.asList(new Double[]{Double.longBitsToDouble(cal.getTimeInMillis())}));
            }
            cal.add(Calendar.HOUR, 2);
            maxDate = cal.getTime();
        }
        
        public void updateLabelsAndValues() {
            Random generator = new Random();
            
            Calendar cal = Calendar.getInstance();
            cal.setTime(maxDate);
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm");
            cal.add(Calendar.HOUR, 2);
            String formattedStr = sdf.format(cal.getTime());
            _groupLabels.add(formattedStr);
            _chartXValues.add(Arrays.asList(new Double[]{Double.longBitsToDouble(cal.getTimeInMillis())}));
            _chartYValues.add(Arrays.asList(new Double[]{generator.nextDouble()*20+10, generator.nextDouble()*10}));
            cal.add(Calendar.HOUR, 2);
            maxDate = cal.getTime();
            
            if (_chartYValues.size() > MAX_SIZE) {
                _chartYValues = _chartYValues.subList(_chartYValues.size()-MAX_SIZE, _chartYValues.size());
                _chartXValues = _chartXValues.subList(_chartXValues.size()-MAX_SIZE, _chartXValues.size());
                _groupLabels = _groupLabels.subList(_groupLabels.size()-MAX_SIZE, _groupLabels.size());
            }
        }
    }
}