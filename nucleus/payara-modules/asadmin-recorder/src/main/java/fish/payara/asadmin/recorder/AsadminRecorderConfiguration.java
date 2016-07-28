/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright (c) 2016 Payara Foundation and/or its affiliates.
 * All rights reserved.
 *
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 */
package fish.payara.asadmin.recorder;

import org.glassfish.api.admin.config.ConfigExtension;
import org.jvnet.hk2.config.Attribute;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.Configured;

/**
 *
 * @author Andrew Pielage
 */
@Configured
public interface AsadminRecorderConfiguration extends ConfigBeanProxy, ConfigExtension
{
    @Attribute(defaultValue = "false", dataType = Boolean.class)
    public String isEnabled();
    public void setEnabled(Boolean enabled);
    
    @Attribute(defaultValue = "true", dataType = Boolean.class)
    public String filterCommands();
    public void setFilterCommands(Boolean filterCommands);
    
    @Attribute(defaultValue = 
            "${com.sun.aas.instanceRoot}/asadmin-commands.txt")
    public String getOutputLocation();
    public void setOutputLocation(String outputLocation);
    
    @Attribute(defaultValue = "version,_(.*),list(.*),get(.*),uptime,"
            + "enable-asadmin-recorder,disable-asadmin-recorder,"
            + "set-asadmin-recorder-configuration,asadmin-recorder-enabled")
    public String getFilteredCommands();
    public void setFilteredCommands(String filteredCommands);
}
