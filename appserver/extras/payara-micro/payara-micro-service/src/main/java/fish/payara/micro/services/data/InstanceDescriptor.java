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
package fish.payara.micro.services.data;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import org.glassfish.internal.data.ApplicationInfo;

/**
 *
 * @author steve
 */
public class InstanceDescriptor implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final String memberUUID;
    private String instanceName;
    private int httpPort;
    private int httpsPort;
    private InetAddress hostName;
    private Map<String, ApplicationDescriptor> deployedApplications;

    public InstanceDescriptor(String UUID) throws UnknownHostException {
        hostName = InetAddress.getLocalHost();
        memberUUID = UUID;
    }

    public void addApplication(ApplicationInfo info) {
        if (deployedApplications == null) {
            deployedApplications = new HashMap<>(3);
        }
        
        ApplicationDescriptor ad = new ApplicationDescriptor(info);
        deployedApplications.put(ad.getName(), ad);
    }

    public String getInstanceName() {
        return instanceName;
    }

    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }
    
    /**
     * @return the memberUUID
     */
    public String getMemberUUID() {
        return memberUUID;
    }

    /**
     * @return the httpPort
     */
    public int getHttpPort() {
        return httpPort;
    }

    /**
     * @param httpPort the httpPort to set
     */
    public void setHttpPort(int httpPort) {
        this.httpPort = httpPort;
    }

    @Override
    public String toString() {
        return "InstanceDescriptor{" + "memberUUID=" + memberUUID + ", httpPort=" + httpPort + ", httpsPort=" + httpsPort + ", hostName=" + hostName + ", deployedApplications=" + deployedApplications + '}';
    }

    /**
     * @return the hostName
     */
    public InetAddress getHostName() {
        return hostName;
    }

    /**
     * @param hostName the hostName to set
     */
    public void setHostName(InetAddress hostName) {
        this.hostName = hostName;
    }

    /**
     * @return the deployedApplications
     */
    public Collection<ApplicationDescriptor> getDeployedApplications() {
        if (deployedApplications == null) {
            return new HashSet<>();
        }
        return deployedApplications.values();
    }

    /**
     * @return the httpsPort
     */
    public int getHttpsPort() {
        return httpsPort;
    }

    /**
     * @param httpsPort the httpsPort to set
     */
    public void setHttpsPort(int httpsPort) {
        this.httpsPort = httpsPort;
    }

    public void removeApplication(ApplicationInfo applicationInfo) {
        if (deployedApplications == null) {
            deployedApplications = new HashMap<>(3);
        }

        deployedApplications.remove(applicationInfo.getName());
    }

    /**
     * Overrides equals purely based on the UUID value
     * @param obj
     * @return 
     */
    @Override
    public boolean equals(Object obj) {
        boolean result = false;
        if (InstanceDescriptor.class.isInstance(obj)) {
            InstanceDescriptor descriptor = (InstanceDescriptor)obj;
            result = this.memberUUID.equals(descriptor.memberUUID);
        }
        return result;
    }

    /**
     * Overrides hashcode based purely on the UUID hashcode
     * @return 
     */
    @Override
    public int hashCode() {
        return memberUUID.hashCode();
    }
    
    
    
}
