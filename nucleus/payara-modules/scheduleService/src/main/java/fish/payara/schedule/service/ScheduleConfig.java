/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright (c) 2016 C2B2 Consulting Limited and/or its affiliates.
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
package fish.payara.schedule.service;

import java.util.List;
import org.glassfish.api.admin.config.ConfigExtension;
import org.glassfish.api.admin.config.Container;
import org.jvnet.hk2.config.Attribute;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.Configured;
import org.jvnet.hk2.config.Element;



/**
 *
 * @author Daniel
 */
@Configured
public interface ScheduleConfig extends ConfigBeanProxy, ConfigExtension, Container{
    @Attribute(defaultValue="false")
    public Boolean getEnabled();
    public void setEnabled(String enabled);
    
    @Attribute(defaultValue="1")
    public int getCoreSize();
    public void setCoreSize(String size);
    
    @Attribute(defaultValue="false")
    public Boolean getFixedSize();
    public void setFixedSize(String sizeEnabled);
    
    @Element
    public void setJobs(List<String> args);
    public List<String> getJobs();
    

}
