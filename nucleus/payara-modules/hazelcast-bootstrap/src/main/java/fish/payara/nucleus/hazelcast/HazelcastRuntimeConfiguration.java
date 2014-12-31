/*

 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

 Copyright (c) 2014 C2B2 Consulting Limited. All rights reserved.

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
package fish.payara.nucleus.hazelcast;

import org.glassfish.api.admin.config.ConfigExtension;
import org.jvnet.hk2.config.Attribute;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.Configured;

/**
 *
 * @author steve
 */
@Configured
public interface HazelcastRuntimeConfiguration 
    extends ConfigBeanProxy, ConfigExtension {

    @Attribute(defaultValue = "false")
    Boolean getEnabled();
    public void setEnabled(Boolean value);
    
    @Attribute(defaultValue = "hazelcast-config.xml")
    String getHazelcastConfigurationFile();
    public void setHazelcastConfigurationFile(String value);
    
    @Attribute(defaultValue = "5900")
    String getStartPort();
    public void setStartPort(String value);
    
    @Attribute(defaultValue = "224.2.2.3")
    String getMulticastGroup();
    public void setMulticastGroup(String value);
    
    @Attribute(defaultValue = "54327")
    String getMulticastPort();
    public void setMulticastPort(String value);
    
    @Attribute(defaultValue = "payara/Hazelcast")
    String getJNDIName();
    public void setJNDIName(String value);
    
    @Attribute(defaultValue = "payara/CacheManager")
    String getCacheManagerJNDIName();
    public void setCacheManagerJNDIName(String value);
    
    @Attribute(defaultValue = "payara/CachingProvider")
    String getCachingProviderJNDIName();
    public void setCachingProviderJNDIName(String value);
}
