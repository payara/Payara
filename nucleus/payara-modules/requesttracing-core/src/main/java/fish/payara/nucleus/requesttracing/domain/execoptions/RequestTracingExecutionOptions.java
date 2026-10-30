/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2016-2020 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Configuration class that holds the dynamic configuration of the Request
 * Tracing service.
 *
 * @author mertcaliskan
 */
public class RequestTracingExecutionOptions {

    // Default values taken from RequestTracingServiceConfiguration
    private Boolean enabled = false;
    
    private Double sampleRate = 1.0;
    private Boolean adaptiveSamplingEnabled = false;
    private Integer adaptiveSamplingTargetCount = 6;
    private Integer adaptiveSamplingTimeValue = 1;
    private TimeUnit adaptiveSamplingTimeUnit = MINUTES;
    
    private Boolean applicationsOnlyEnabled = true;
    private Long thresholdValue = 30L;
    private TimeUnit thresholdUnit = SECONDS;
    private Boolean sampleRateFirstEnabled = true;
    
    private Integer traceStoreSize = 20;
    // Default timeout value **NOT** taken from RequestTracingServiceConfiguration, but from TimeUtil.setStoreTimeLimit
    private Long traceStoreTimeout = 0L;
    private Boolean reservoirSamplingEnabled = false;
    
    private Boolean historicTraceStoreEnabled = false;
    private Integer historicTraceStoreSize = 20;
    // Default timeout value **NOT** taken from RequestTracingServiceConfiguration, but from TimeUtil.setStoreTimeLimit
    private Long historicTraceStoreTimeout = 0L;

    private final Set<String> enabledNotifiers = new LinkedHashSet<>();

    public Boolean isEnabled() {
        if (enabled == null) {
            return false;
        }
        
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Double getSampleRate() {
        return sampleRate;
    }

    public void setSampleRate(Double sampleRate) {
        this.sampleRate = sampleRate;
    }

    public Boolean getSampleRateFirstEnabled() {
        return sampleRateFirstEnabled;
    }

    public void setSampleRateFirstEnabled(Boolean sampleRateFirstEnabled) {
        this.sampleRateFirstEnabled = sampleRateFirstEnabled;
    }

    public Boolean getAdaptiveSamplingEnabled() {
        return adaptiveSamplingEnabled;
    }

    public void setAdaptiveSamplingEnabled(Boolean adaptiveSamplingEnabled) {
        this.adaptiveSamplingEnabled = adaptiveSamplingEnabled;
    }

    public Integer getAdaptiveSamplingTargetCount() {
        return adaptiveSamplingTargetCount;
    }

    public void setAdaptiveSamplingTargetCount(Integer adaptiveSamplingTargetCount) {
        this.adaptiveSamplingTargetCount = adaptiveSamplingTargetCount;
    }

    public Integer getAdaptiveSamplingTimeValue() {
        return adaptiveSamplingTimeValue;
    }

    public void setAdaptiveSamplingTimeValue(Integer adaptiveSamplingTimeValue) {
        this.adaptiveSamplingTimeValue = adaptiveSamplingTimeValue;
    }

    public TimeUnit getAdaptiveSamplingTimeUnit() {
        return adaptiveSamplingTimeUnit;
    }

    public void setAdaptiveSamplingTimeUnit(TimeUnit adaptiveSamplingTimeUnit) {
        this.adaptiveSamplingTimeUnit = adaptiveSamplingTimeUnit;
    }

    public Boolean getApplicationsOnlyEnabled() {
        return applicationsOnlyEnabled;
    }

    public void setApplicationsOnlyEnabled(Boolean applicationsOnlyEnabled) {
        this.applicationsOnlyEnabled = applicationsOnlyEnabled;
    }

    /**
     * Gets the threshold value above which request traces will be sent to the notification service
     * @return
     * @see #getThresholdUnit() 
     */
    public Long getThresholdValue() {
        return thresholdValue;
    }

    /**
     * Sets the threshold value above which request traces will be sent to the notification service
     * @param thresholdValue
     * @see #setThresholdUnit(TimeUnit) 
     */
    public void setThresholdValue(Long thresholdValue) {
        this.thresholdValue = thresholdValue;
    }

    /**
     * Gets the {@link TimeUnit} which the threshold for request traces is using
     * @return
     * @see #getThresholdValue()
     */
    public TimeUnit getThresholdUnit() {
        return thresholdUnit;
    }

    /**
     * Sets he {@link TimeUnit} which the threshold for request traces is using
     * @param thresholdUnit
     * @see #setThresholdValue(Long)
     */
    public void setThresholdUnit(TimeUnit thresholdUnit) {
        this.thresholdUnit = thresholdUnit;
    }
    
    /**
     * Gets maximum the number of traces stored
     * @return 
     */
    public Integer getTraceStoreSize() {
        return traceStoreSize;
    }

    /**
     * Sets the maximum number of traces to store
     * @param traceStoreSize 
     */
    public void setTraceStoreSize(Integer traceStoreSize) {
        this.traceStoreSize = traceStoreSize;
    }

    public Long getTraceStoreTimeout() {
        return traceStoreTimeout;
    }

    public void setTraceStoreTimeout(Long traceStoreTimeout) {
        this.traceStoreTimeout = traceStoreTimeout;
    }

    public Boolean getReservoirSamplingEnabled() {
        return reservoirSamplingEnabled;
    }

    public void setReservoirSamplingEnabled(Boolean reservoirSamplingEnabled) {
        this.reservoirSamplingEnabled = reservoirSamplingEnabled;
    }
    
    public Boolean isHistoricTraceStoreEnabled() {
        if (historicTraceStoreEnabled == null) {
            return false;
        }
        
        return historicTraceStoreEnabled;
    }
    
    public void setHistoricTraceStoreEnabled(Boolean historicTraceStoreEnabled) {
        this.historicTraceStoreEnabled = historicTraceStoreEnabled;
    }
    
    public Integer getHistoricTraceStoreSize() {
        return historicTraceStoreSize;
    }
    
    public void setHistoricTraceStoreSize(Integer historicTraceStoreSize) {
        this.historicTraceStoreSize = historicTraceStoreSize;
    }
    
    public Long getHistoricTraceStoreTimeout() {
        return historicTraceStoreTimeout;
    }
    
    public void setHistoricTraceStoreTimeout(Long historicTraceStoreTimeout) {
        this.historicTraceStoreTimeout = historicTraceStoreTimeout;
    }
    
    /**
     * Gets the notifier options configured with request tracing
     * @return 
     */
    public Set<String> getEnabledNotifiers() {
        return enabledNotifiers;
    }
    
    public void enableNotifier(String notifier) {
        enabledNotifiers.add(notifier);
    }

    public void disableNotifier(String notifier) {
        enabledNotifiers.remove(notifier);
    }

    public void clearNotifiers() {
        enabledNotifiers.clear();
    }

    @Override
    public String toString() {
        return "RequestTracingExecutionOptions{"
                + "enabled=" + enabled
                + ", sampleRate=" + sampleRate
                + ", adaptiveSamplingEnabled=" + adaptiveSamplingEnabled
                + ", adaptiveSamplingTargetCount=" + adaptiveSamplingTargetCount
                + ", adaptiveSamplingTimeValue=" + adaptiveSamplingTimeValue
                + ", adaptiveSamplingTimeUnit=" + adaptiveSamplingTimeUnit
                + ", applicationsOnlyEnabled=" + applicationsOnlyEnabled
                + ", thresholdValue=" + thresholdValue
                + ", thresholdUnit=" + thresholdUnit
                + ", sampleRateFirstEnabled=" + sampleRateFirstEnabled
                + ", traceStoreSize=" + traceStoreSize
                + ", traceStoreTimeout=" + traceStoreTimeout
                + ", reservoirSamplingEnabled=" + reservoirSamplingEnabled
                + ", historicTraceStoreEnabled=" + historicTraceStoreEnabled
                + " ,historicTraceStoreSize=" + historicTraceStoreSize
                + ", historicTraceStoreTimeout=" + historicTraceStoreTimeout
                + "}";
    }
}
