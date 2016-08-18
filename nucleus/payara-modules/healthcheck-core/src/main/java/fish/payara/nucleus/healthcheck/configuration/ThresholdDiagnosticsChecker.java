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

import fish.payara.nucleus.healthcheck.HealthCheckConstants;
import org.glassfish.api.admin.config.PropertiesDesc;
import org.glassfish.api.admin.config.PropertyDesc;
import org.jvnet.hk2.config.Element;
import org.jvnet.hk2.config.types.Property;
import org.jvnet.hk2.config.types.PropertyBag;

import java.util.List;

/**
 * @author mertcaliskan
 */
public interface ThresholdDiagnosticsChecker extends Checker, PropertyBag {

    @Element
    @PropertiesDesc(props = {
            @PropertyDesc(name = HealthCheckConstants.THRESHOLD_CRITICAL, defaultValue = HealthCheckConstants
                    .THRESHOLD_DEFAULTVAL_CRITICAL),
            @PropertyDesc(name = HealthCheckConstants.THRESHOLD_WARNING, defaultValue = HealthCheckConstants
                    .THRESHOLD_DEFAULTVAL_WARNING),
            @PropertyDesc(name = HealthCheckConstants.THRESHOLD_GOOD, defaultValue = HealthCheckConstants
                    .THRESHOLD_DEFAULTVAL_GOOD)
    })
    List<Property> getProperty();
}
