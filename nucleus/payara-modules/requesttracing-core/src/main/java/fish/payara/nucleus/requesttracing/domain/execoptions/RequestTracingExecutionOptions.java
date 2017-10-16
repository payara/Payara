/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2016-2017 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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
package fish.payara.nucleus.requesttracing.domain.execoptions;

import fish.payara.nucleus.notification.configuration.NotifierType;
import fish.payara.nucleus.notification.domain.NotifierExecutionOptions;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Configuration class that holds the dynamic configuration of the Request
 * Tracing service.
 *
 * @author mertcaliskan
 */
public class RequestTracingExecutionOptions {

    private boolean enabled;
    private Integer sampleChance;
    private boolean reservoirSamplingEnabled;
    private Long thresholdValue;
    private TimeUnit thresholdUnit;
    private boolean historicalTraceEnabled;
    private Integer historicalTraceStoreSize;
    private Long historicalTraceTimeout;
    private Map<NotifierType, NotifierExecutionOptions> notifierExecutionOptionsList = new HashMap<NotifierType, NotifierExecutionOptions>();

    public void addNotifierExecutionOption(NotifierExecutionOptions notifierExecutionOptions) {
        getNotifierExecutionOptionsList().put(notifierExecutionOptions.getNotifierType(), notifierExecutionOptions);
    }

    public void removeNotifierExecutionOption(NotifierExecutionOptions notifierExecutionOptions) {
        getNotifierExecutionOptionsList().remove(notifierExecutionOptions.getNotifierType());
    }

    public void resetNotifierExecutionOptions() {
        getNotifierExecutionOptionsList().clear();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Integer getSampleChance() {
        return sampleChance;
    }

    public void setSampleChance(Integer sampleChance) {
        this.sampleChance = sampleChance;
    }

    public Boolean getReservoirSamplingEnabled() {
        return reservoirSamplingEnabled;
    }

    public void setReservoirSamplingEnabled(Boolean reservoirSamplingEnabled) {
        this.reservoirSamplingEnabled = reservoirSamplingEnabled;
    }

    public Long getThresholdValue() {
        return thresholdValue;
    }

    public void setThresholdValue(Long thresholdValue) {
        this.thresholdValue = thresholdValue;
    }

    public TimeUnit getThresholdUnit() {
        return thresholdUnit;
    }

    public void setThresholdUnit(TimeUnit thresholdUnit) {
        this.thresholdUnit = thresholdUnit;
    }

    public boolean isHistoricalTraceEnabled() {
        return historicalTraceEnabled;
    }

    public void setHistoricalTraceEnabled(boolean historicalTraceEnabled) {
        this.historicalTraceEnabled = historicalTraceEnabled;
    }

    public Integer getHistoricalTraceStoreSize() {
        return historicalTraceStoreSize;
    }

    public void setHistoricalTraceStoreSize(Integer historicalTraceStoreSize) {
        this.historicalTraceStoreSize = historicalTraceStoreSize;
    }

    public Long getHistoricalTraceTimeout() {
        return historicalTraceTimeout;
    }

    public void setHistoricalTraceTimeout(Long historicalTraceTimeout) {
        this.historicalTraceTimeout = historicalTraceTimeout;
    }

    public Map<NotifierType, NotifierExecutionOptions> getNotifierExecutionOptionsList() {
        return notifierExecutionOptionsList;
    }

    @Override
    public String toString() {
        return "RequestTracingExecutionOptions{"
                + "enabled=" + enabled
                + ", thresholdValue=" + thresholdValue
                + ", thresholdUnit=" + thresholdUnit
                + ", sampleChance=" + sampleChance
                + ", reservoirSamplingEnabled=" + reservoirSamplingEnabled
                + ", historicalTraceEnabled=" + historicalTraceEnabled
                + ", historicalTraceStoreSize=" + historicalTraceStoreSize
                + ", historicalTraceTimeout=" + historicalTraceTimeout
                + ", notifierExecutionOptionsList=" + notifierExecutionOptionsList
                + '}';
    }
}
