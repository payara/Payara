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

import org.glassfish.api.admin.config.ConfigExtension;
import org.jvnet.hk2.config.Attribute;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.Configured;

import java.beans.PropertyVetoException;
import javax.validation.constraints.Min;

/**
 * @author mertcaliskan
 */
@Configured
public interface Checker extends ConfigBeanProxy, ConfigExtension {

    String getName();
    void setName(String value) throws PropertyVetoException;

    @Attribute
    String getEnabled();
    void setEnabled(String value) throws PropertyVetoException;

    @Attribute(defaultValue = "5", dataType = Long.class)
    @Min(value = 0)
    String getTime();
    void setTime(String value) throws PropertyVetoException;

    @Attribute(defaultValue = "MINUTES")
    String getUnit();
    void setUnit(String value) throws PropertyVetoException;
}
