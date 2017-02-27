/*

 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

 Copyright (c) 2014,2016 Payara Foundation. All rights reserved.

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
 * @author Steve Millidge (Payara Foundation)
 */
@Configured
public interface HazelcastRuntimeConfiguration 
    extends ConfigBeanProxy, ConfigExtension {

    @Attribute(defaultValue = "false", dataType = Boolean.class)
    String getEnabled();
    public void setEnabled(String value);
    
    @Attribute(defaultValue = "hazelcast-config.xml")
    String getHazelcastConfigurationFile();
    public void setHazelcastConfigurationFile(String value);
    
    @Attribute(defaultValue = "5900")
    String getStartPort();
    public void setStartPort(String value);
    
    @Attribute(defaultValue = "224.2.2.3")
    String getMulticastGroup();
    public void setMulticastGroup(String value);
    
    
    @Attribute(defaultValue = "false", dataType = Boolean.class)
    String getGenerateNames();
    public void setGenerateNames(String value);

    
    @Attribute(defaultValue = "payara")
    String getMemberName();
    public void setMemberName(String value);

    @Attribute(defaultValue = "MicroShoal")
    String getMemberGroup();
    public void setMemberGroup(String value);

    
    @Attribute(defaultValue = "development")
    String getClusterGroupName();
    public void setClusterGroupName(String value);
    
    @Attribute(defaultValue = "D3v3l0pm3nt")
    String getClusterGroupPassword();
    public void setClusterGroupPassword(String value);
    
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

    @Attribute(defaultValue = "false", dataType = Boolean.class)
    String getHostAwarePartitioning();
    public void setHostAwarePartitioning(String value);

    @Attribute(defaultValue = "4")
    String getExecutorPoolSize();
    public void setExecutorPoolSize(String value);
    
    @Attribute(defaultValue = "20")
    String getExecutorQueueCapacity();
    public void setExecutorQueueCapacity(String value);
    
    @Attribute(defaultValue = "4")
    String getScheduledExecutorPoolSize();
    public void setScheduledExecutorPoolSize(String value);
    
    @Attribute(defaultValue = "20")
    String getScheduledExecutorQueueCapacity();
    public void setScheduledExecutorQueueCapacity(String value);

    @Attribute(defaultValue = "")
    String getLicenseKey();
    public void setLicenseKey(String value);
    
    @Attribute(defaultValue = "false", dataType = Boolean.class)
    String getLite();
    public void setLite(String value);
}
