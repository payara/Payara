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
package fish.payara.appserver.micro.services.data;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
    private List<Integer> httpPorts;
    private List<Integer> httpsPorts;
    private InetAddress hostName;
    private Map<String, ApplicationDescriptor> deployedApplications;
    private boolean liteMember;
    private String instanceType;
    private int hazelcastPort;
    private int adminPort;

    public InstanceDescriptor(String UUID) throws UnknownHostException {
        hostName = InetAddress.getLocalHost();
        memberUUID = UUID;
        httpPorts = new ArrayList<>();
        httpsPorts = new ArrayList<>();
    }

    public void addApplication(ApplicationInfo info) {
        if (deployedApplications == null) {
            deployedApplications = new HashMap<>(3);
        }
        
        ApplicationDescriptor ad = new ApplicationDescriptor(info);
        deployedApplications.put(ad.getName(), ad);
    }
    
    public void addApplication(ApplicationDescriptor descriptor) {
        if (deployedApplications == null) {
            deployedApplications = new HashMap<>(3);
        }
        
        deployedApplications.put(descriptor.getName(), descriptor);
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
     * @return the httpPorts
     */
    public List<Integer> getHttpPorts() {
        return httpPorts;
    }

    /**
     * @param httpPort the httpPort to add
     */
    public void addHttpPort(int httpPort) {
        httpPorts.add(httpPort);
    }

    @Override
    public String toString() {
        return "InstanceDescriptor{" + "memberUUID=" + memberUUID + ", httpPorts=" + httpPorts + ", httpsPorts=" + httpsPorts + ", hostName=" + hostName + ", deployedApplications=" + deployedApplications + '}';
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
     * @return the httpsPorts
     */
    public List<Integer> getHttpsPorts() {
        return httpsPorts;
    }

    /**
     * @param httpsPort the httpsPort to add
     */
    public void addHttpsPort(int httpsPort) {
        httpsPorts.add(httpsPort);
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
    
    public boolean isLiteMember() {
        return liteMember;
    }
    
    public void setLiteMember(boolean isLiteMember) {
        liteMember = isLiteMember;
    }
    
    public boolean isMicroInstance() {
        return instanceType.equals("MICRO");
    }
    
    public boolean isPayaraInstance() {
        return (instanceType.equals("DAS") || instanceType.equals("INSTANCE"));
    }
    
    public void setInstanceType(String instanceType) {
        this.instanceType = instanceType;
    }
    
    public String getInstanceType() {
        return instanceType;
    }
    
    public void setHazelcastPort(int hazelcastPort) {
        this.hazelcastPort = hazelcastPort;
    }
    
    public int getHazelcastPort() {
        return hazelcastPort;
    }
    
    public void setAdminPort(int adminPort) {
        this.adminPort = adminPort;
    }
    
    public int getAdminPort() {
        return adminPort;
    }
}
