/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2016-2021 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.nucleus.requesttracing.configuration;

import java.beans.PropertyVetoException;
import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

import org.glassfish.api.admin.config.ConfigExtension;
import org.jvnet.hk2.config.Attribute;
import org.jvnet.hk2.config.Configured;
import org.jvnet.hk2.config.Element;

/**
 * Configuration class that holds the configuration of the Request
 * Tracing service.
 *
 * @author mertcaliskan
 */
@Configured
public interface RequestTracingServiceConfiguration extends ConfigExtension {

    @Attribute(defaultValue = "false", dataType = Boolean.class)
    String getEnabled();
    void enabled(String value) throws PropertyVetoException;
    
    @Attribute(defaultValue = "1.0")
    @Pattern(regexp = "0(\\.\\d+)?|1(\\.0)?", message = "Must be a valid double between 0 and 1")
    String getSampleRate();
    void setSampleRate(String value) throws PropertyVetoException;

    @Attribute(defaultValue = "false", dataType = Boolean.class)
    String getAdaptiveSamplingEnabled();
    void setAdaptiveSamplingEnabled(String value) throws PropertyVetoException;

    @Attribute(defaultValue = "6", dataType = Integer.class)
    @Min(value = 1, message = "Adaptive sampling target count must be greater than 0")
    @Max(value = Integer.MAX_VALUE, message = "Adaptive sampling target count must be less than " + Integer.MAX_VALUE)
    String getAdaptiveSamplingTargetCount();
    void setAdaptiveSamplingTargetCount(String value) throws PropertyVetoException;

    @Attribute(defaultValue = "1", dataType = Integer.class)
    @Min(value = 1, message = "Adaptive sampling time value must be greater than 0")
    @Max(value = Integer.MAX_VALUE, message = "Adaptive sampling time value must be less than " + Integer.MAX_VALUE)
    String getAdaptiveSamplingTimeValue();
    void setAdaptiveSamplingTimeValue(String value) throws PropertyVetoException;

    @Attribute(defaultValue = "MINUTES")
    @Pattern(regexp = "SECONDS|MINUTES|HOURS|DAYS", message = "Invalid time unit. Value must be one of: SECONDS, MINUTES, HOURS, DAYS.")
    String getAdaptiveSamplingTimeUnit();
    void setAdaptiveSamplingTimeUnit(String value) throws PropertyVetoException;

    @Attribute(defaultValue = "true", dataType = Boolean.class)
    String getApplicationsOnlyEnabled();
    void setApplicationsOnlyEnabled(String value) throws PropertyVetoException;

    @Attribute(defaultValue = "30", dataType = Integer.class)
    @Min(value = 0, message = "Threshold value must be at least 0")
    @Max(value = Integer.MAX_VALUE, message = "Threshold value must be less than " + Integer.MAX_VALUE)
    String getThresholdValue();
    void setThresholdValue(String value) throws PropertyVetoException;

    @Attribute(defaultValue = "SECONDS")
    @Pattern(regexp = "NANOSECONDS|MICROSECONDS|MILLISECONDS|SECONDS|MINUTES|HOURS|DAYS", message = "Invalid time unit. Value must be one of: NANOSECONDS, MICROSECONDS, MILLISECONDS, SECONDS, MINUTES, HOURS, DAYS.")
    String getThresholdUnit();
    void setThresholdUnit(String value) throws PropertyVetoException;

    @Attribute(defaultValue = "true", dataType = Boolean.class)
    String getSampleRateFirstEnabled();
    void setSampleRateFirstEnabled(String value) throws PropertyVetoException;

    @Attribute(defaultValue = "20", dataType = Integer.class)
    @Min(value = 0, message = "Trace store size must be greater than or equal to 0")
    String getTraceStoreSize();
    void setTraceStoreSize(String value) throws PropertyVetoException;

    @Attribute
    String getTraceStoreTimeout();
    void setTraceStoreTimeout(String value) throws PropertyVetoException;

    @Attribute(defaultValue = "false", dataType = Boolean.class)
    String getReservoirSamplingEnabled();
    void setReservoirSamplingEnabled(String value) throws PropertyVetoException;

    @Attribute(defaultValue = "false", dataType = Boolean.class)
    String getHistoricTraceStoreEnabled();
    void setHistoricTraceStoreEnabled(String value) throws PropertyVetoException;

    @Attribute(defaultValue = "20", dataType = Integer.class)
    @Min(value = 0, message = "Historic trace store size must be greater than or equal to 0")
    String getHistoricTraceStoreSize();
    void setHistoricTraceStoreSize(String value) throws PropertyVetoException;

    @Attribute
    String getHistoricTraceStoreTimeout();
    void setHistoricTraceStoreTimeout(String value) throws PropertyVetoException;

    @Element("notifier")
    List<String> getNotifierList();

}
