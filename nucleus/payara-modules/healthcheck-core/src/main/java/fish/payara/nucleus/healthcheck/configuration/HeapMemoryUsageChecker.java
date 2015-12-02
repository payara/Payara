/*
 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 Copyright (c) 2015 C2B2 Consulting Limited. All rights reserved.
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

import fish.payara.nucleus.healthcheck.preliminary.BaseHealthCheck;
import org.glassfish.api.admin.config.PropertiesDesc;
import org.glassfish.api.admin.config.PropertyDesc;
import org.jvnet.hk2.config.Configured;
import org.jvnet.hk2.config.Element;
import org.jvnet.hk2.config.types.Property;
import org.jvnet.hk2.config.types.PropertyBag;

import java.util.List;

/**
 * @author mertcaliskan
 */
@Configured
public interface HeapMemoryUsageChecker extends Checker, PropertyBag {

    @Element
    @PropertiesDesc(props = {
            @PropertyDesc(name = BaseHealthCheck.THRESHOLD_CRITICAL, defaultValue = BaseHealthCheck
                    .THRESHOLD_DEFAULTVAL_CRITICAL),
            @PropertyDesc(name = BaseHealthCheck.THRESHOLD_WARNING, defaultValue = BaseHealthCheck
                    .THRESHOLD_DEFAULTVAL_WARNING),
            @PropertyDesc(name = BaseHealthCheck.THRESHOLD_GOOD, defaultValue = BaseHealthCheck
                    .THRESHOLD_DEFAULTVAL_GOOD)
    })
    List<Property> getProperty();
}
