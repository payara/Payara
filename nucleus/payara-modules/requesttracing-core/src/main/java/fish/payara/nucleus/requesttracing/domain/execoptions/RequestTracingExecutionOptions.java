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
 * Configuration class that holds enable/disable value of request tracing, the threshold value with its timeunit that will
 * trigger request tracing mechainsm, and a list of notifier configurations.
 *
 * @author mertcaliskan
 */
public class RequestTracingExecutionOptions {

    private boolean enabled;
    private Long thresholdValue;
    private TimeUnit thresholdUnit;
    private boolean historicalTraceEnabled;
    private Integer historicalTraceStoreSize;
    private Long historicalTraceTimeout;
    private Map<NotifierType, NotifierExecutionOptions> notifierExecutionOptionsList = new HashMap<NotifierType, NotifierExecutionOptions>();

    /**
     * Adds a notifier to be used with request tracing
     * @param notifierExecutionOptions 
     */
    public void addNotifierExecutionOption(NotifierExecutionOptions notifierExecutionOptions) {
        getNotifierExecutionOptionsList().put(notifierExecutionOptions.getNotifierType(), notifierExecutionOptions);
    }
    
    /**
     * Removes a notifier from the list of configured ones
     * @param notifierExecutionOptions A notifier with the same type of this will be removed
     */
    public void removeNotifierExecutionOption(NotifierExecutionOptions notifierExecutionOptions) {
        getNotifierExecutionOptionsList().remove(notifierExecutionOptions.getNotifierType());
    }
    
    /**
     * Removes all notifiers from the request tracing service
     */
    public void resetNotifierExecutionOptions() {
        getNotifierExecutionOptionsList().clear();
    }

    /**
     * Whether request tracing is set to use the notification service at all
     * @return 
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Set whether request tracing is set to use the notification service at all
     * @param enabled 
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
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
     * Returns true if a store of historic traces is enabled
     * @return 
     */
    public boolean isHistoricalTraceEnabled() {
        return historicalTraceEnabled;
    }

    /**
     * Set whether a store of historic traces is enabled
     * @param historicalTraceEnabled 
     */
    public void setHistoricalTraceEnabled(boolean historicalTraceEnabled) {
        this.historicalTraceEnabled = historicalTraceEnabled;
    }

    /**
     * Gets maximum the number of traces stored
     * @return 
     */
    public Integer getHistoricalTraceStoreSize() {
        return historicalTraceStoreSize;
    }

    /**
     * Sets the maximum number of traces to store
     * @param historicalTraceStoreSize 
     */
    public void setHistoricalTraceStoreSize(Integer historicalTraceStoreSize) {
        this.historicalTraceStoreSize = historicalTraceStoreSize;
    }

    public Long getHistoricalTraceTimeout() {
        return historicalTraceTimeout;
    }

    public void setHistoricalTraceTimeout(Long historicalTraceTimeout) {
        this.historicalTraceTimeout = historicalTraceTimeout;
    }

    /**
     * Gets the notifier options configured with request tracing
     * @return 
     */
    public Map<NotifierType, NotifierExecutionOptions> getNotifierExecutionOptionsList() {
        return notifierExecutionOptionsList;
    }

    @Override
    public String toString() {
        return "RequestTracingExecutionOptions{" +
                "enabled=" + enabled +
                ", thresholdValue=" + thresholdValue +
                ", thresholdUnit=" + thresholdUnit +
                ", historicalTraceEnabled=" + historicalTraceEnabled +
                ", historicalTraceStoreSize=" + historicalTraceStoreSize +
                ", historicalTraceTimeout=" + historicalTraceTimeout +
                ", notifierExecutionOptionsList=" + notifierExecutionOptionsList +
                '}';
    }
}
