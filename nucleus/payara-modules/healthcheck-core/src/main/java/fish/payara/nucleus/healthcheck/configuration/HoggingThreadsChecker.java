/*
 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 Copyright (c) 2016 Payara Foundation. All rights reserved.
 The contents of this file are subject to the terms of the Common Development
 and Distribution License("CDDL") (collectively, the "License").  You
 may not use this file except in compliance with the License.  You can
 obtain a copy of the License at
 https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 or packager/legal/LICENSE.txt.  See the License for the specific
 language governing permissions and limitations under the License.
 When distributing the software, include this License Header Notice in each
 file and include the License file at packager/legal/LICENSE.txt.
 */
package fish.payara.nucleus.healthcheck.configuration;

import org.jvnet.hk2.config.Attribute;
import org.jvnet.hk2.config.Configured;

import java.beans.PropertyVetoException;

/**
 * @author mertcaliskan
 */
@Configured
@CheckerConfigurationType(type = CheckerType.HOGGING_THREADS)
public interface HoggingThreadsChecker extends Checker {

    @Attribute(defaultValue = "HOGT")
    String getName();
    void setName(String value) throws PropertyVetoException;

    @Attribute(defaultValue = "95", dataType = Long.class)
    String getThresholdPercentage();
    void setThresholdPercentage(String value) throws PropertyVetoException;

    @Attribute(defaultValue = "3", dataType = Integer.class)
    String getRetryCount();
    void setRetryCount(String value) throws PropertyVetoException;

}
