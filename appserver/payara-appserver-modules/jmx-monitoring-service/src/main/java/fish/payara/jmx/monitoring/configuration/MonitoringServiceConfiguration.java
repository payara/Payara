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
package fish.payara.jmx.monitoring.configuration;

import java.beans.PropertyVetoException;
import java.util.List;
import org.glassfish.api.admin.config.ConfigExtension;
import org.jvnet.hk2.config.*;
import org.jvnet.hk2.config.types.Property;
import org.jvnet.hk2.config.types.PropertyBag;

/**
 *
 * @author savage
 */
@Configured
public interface MonitoringServiceConfiguration extends ConfigBeanProxy, ConfigExtension, PropertyBag {

    /**
     * Boolean value determining if the service is enabled or disabled.
     *  Default value is false. 
     * @return 
     */
    @Attribute(defaultValue="false")
    String getEnabled();
    void setEnabled(String value) throws PropertyVetoException;

    /**
     * Boolean value determining if bootAMX is invoked by the service.
     *  Default value is false. 
     * @return 
     */
    @Attribute(defaultValue="false")
    String getAmx();
    void setAmx(String value) throws PropertyVetoException;

    /**
     * Frequency of log messages.
     *  Default value is 15 
     * @return 
     */
    @Attribute(defaultValue="15")
    String getLogFrequency();
    void setLogFrequency(String value) throws PropertyVetoException;

    /**
     * TimeUnit for frequency of log messages.
     *  Default value is TimeUnit.SECONDS 
     * @return 
     */
    @Attribute(defaultValue="SECONDS")
    String getLogFrequencyUnit();
    void setLogFrequencyUnit(String value) throws PropertyVetoException;
  
    /**
     * Properties listed in the domain.xml.
     *  Returns a list of properties which are present in the configuration block
     * @return 
     */
    @Element
    @Override
    List<Property> getProperty();
}
