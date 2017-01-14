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

import fish.payara.micro.data.ModuleDescriptor;
import fish.payara.micro.data.ApplicationDescriptor;
import fish.payara.micro.data.InstanceDescriptor;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.glassfish.internal.data.ApplicationInfo;

/**
 *
 * @author steve
 */
public class InstanceDescriptorImpl implements InstanceDescriptor {

    private static final long serialVersionUID = 1L;

    private final String memberUUID;
    private String instanceName;
    private final List<Integer> httpPorts;
    private final List<Integer> httpsPorts;
    private InetAddress hostName;
    private Map<String, ApplicationDescriptor> deployedApplications;
    private boolean liteMember;
    private String instanceType;
    private int hazelcastPort;
    private int adminPort;
    private String instanceGroup;

    public InstanceDescriptorImpl(String UUID) throws UnknownHostException {
        hostName = InetAddress.getLocalHost();
        memberUUID = UUID;
        httpPorts = new ArrayList<>();
        httpsPorts = new ArrayList<>();
    }

    public void addApplication(ApplicationInfo info) {
        if (deployedApplications == null) {
            deployedApplications = new HashMap<>(3);
        }

        ApplicationDescriptorImpl ad = new ApplicationDescriptorImpl(info);
        deployedApplications.put(ad.getName(), ad);
    }

    public void addApplication(ApplicationDescriptor descriptor) {
        if (deployedApplications == null) {
            deployedApplications = new HashMap<>(3);
        }

        deployedApplications.put(descriptor.getName(), descriptor);
    }

    @Override
    public String getInstanceName() {
        return instanceName;
    }

    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    /**
     * @return the memberUUID
     */
    @Override
    public String getMemberUUID() {
        return memberUUID;
    }

    /**
     * @return the httpPorts
     */
    @Override
    public List<Integer> getHttpPorts() {
        return httpPorts;
    }

    /**
     * @param httpPort the httpPort to add
     */
    public void addHttpPort(int httpPort) {
        httpPorts.add(httpPort);
    }

    /**
     * @return the hostName
     */
    @Override
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
    @Override
    public Collection<ApplicationDescriptor> getDeployedApplications() {
        if (deployedApplications == null) {
            return new HashSet<>();
        }
        return deployedApplications.values();
    }

    /**
     * @return the httpsPorts
     */
    @Override
    public List<Integer> getHttpsPorts() {
        return httpsPorts;
    }

    /**
     * @param httpsPort the httpsPort to add
     */
    public void addHttpsPort(int httpsPort) {
        httpsPorts.add(httpsPort);
    }

    public void removeApplication(ApplicationDescriptor applicationInfo) {
        if (deployedApplications == null) {
            deployedApplications = new HashMap<>(3);
        }

        deployedApplications.remove(applicationInfo.getName());
    }

    /**
     * Overrides equals purely based on the UUID value
     *
     * @param obj
     * @return
     */
    @Override
    public boolean equals(Object obj) {
        boolean result = false;
        if (InstanceDescriptorImpl.class.isInstance(obj)) {
            InstanceDescriptorImpl descriptor = (InstanceDescriptorImpl) obj;
            result = this.memberUUID.equals(descriptor.memberUUID);
        }
        return result;
    }

    /**
     * Overrides hashcode based purely on the UUID hashcode
     *
     * @return
     */
    @Override
    public int hashCode() {
        return memberUUID.hashCode();
    }

    /**
     * Checks whether or not this instance is described as a Lite Hazelcast
     * member
     *
     * @return true if this instance describes a Hazelcast Lite member
     */
    @Override
    public boolean isLiteMember() {
        return liteMember;
    }

    /**
     * Sets whether or not this descriptor describes a Hazelcast Lite member
     *
     * @param isLiteMember true if this descriptor describes a Hazelcast Lite
     * member
     */
    public void setLiteMember(boolean isLiteMember) {
        liteMember = isLiteMember;
    }

    /**
     * Checks whether or not this descriptor describes a Payara Micro instance
     *
     * @return true if this descriptor describes a Payara Micro instances
     */
    @Override
    public boolean isMicroInstance() {
        return instanceType.equals("MICRO");
    }

    /**
     * Checks whether or not this descriptor describes a Payara Server instance
     * or the DAS
     *
     * @return true if this descriptor describes a Payara Server instance or the
     * DAS
     */
    @Override
    public boolean isPayaraInstance() {
        return (instanceType.equals("DAS") || instanceType.equals("INSTANCE"));
    }

    /**
     * Sets what instance type this descriptor describes
     *
     * @param instanceType the instance type that this descriptor should
     * describe
     */
    public void setInstanceType(String instanceType) {
        this.instanceType = instanceType;
    }

    /**
     * Gets the instance type that this descriptor describes
     *
     * @return the instance type that this descriptor describes
     */
    @Override
    public String getInstanceType() {
        return instanceType;
    }

    /**
     * Sets the Hazelcast port number for this instance descriptor
     *
     * @param hazelcastPort the port number in use by Hazelcast
     */
    public void setHazelcastPort(int hazelcastPort) {
        this.hazelcastPort = hazelcastPort;
    }

    /**
     * Gets the Hazelcast port number of this instance descriptor
     *
     * @return the port number in use by Hazelcast
     */
    @Override
    public int getHazelcastPort() {
        return hazelcastPort;
    }

    /**
     * Sets the admin port number for this instance descriptor
     *
     * @param adminPort the admin port number in use by this instance
     */
    public void setAdminPort(int adminPort) {
        this.adminPort = adminPort;
    }

    /**
     * Gets the admin port number for this instance descriptor
     *
     * @return the admin port number in use by this instance
     */
    @Override
    public int getAdminPort() {
        return adminPort;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\nInstance Configuration\n");
        sb.append("Host: ").append(hostName.getCanonicalHostName()).append('\n');
        sb.append("HTTP Port(s): ");
        for (Integer port : getHttpPorts()) {
            sb.append(port).append(' ');
        }
        sb.append('\n');
        sb.append("HTTPS Port(s): ");
        for (Integer port : getHttpsPorts()) {
            sb.append(port).append(' ');
        }
        sb.append('\n');
        sb.append("Instance Name: ").append(instanceName).append('\n');
        sb.append("Instance Group: ").append(instanceGroup).append('\n');
        if (memberUUID != null) {
            sb.append("Hazelcast Member UUID ").append(this.memberUUID).append('\n');
        }
        for (ApplicationDescriptor ad : getDeployedApplications()) {
            sb.append("Deployed: ");
            sb.append(ad.getName()).append(" ( ");
            for (ModuleDescriptor md : ad.getModuleDescriptors()) {
                sb.append(md.getName()).append(' ').append(md.getType()).append(' ');
                if (md.getContextRoot() != null) {
                    sb.append(md.getContextRoot()).append(' ');
                }
            }
            sb.append(")\n");
            String libraries = ad.getLibraries();
            if (libraries != null) {
                sb.append(' ').append(ad.getLibraries());
            }
        }
        sb.append('\n');
        return sb.toString();
    }

    @Override
    public List<URL> getApplicationURLS() {
        LinkedList<URL> result = new LinkedList<>();
        if (deployedApplications != null) {
            for (Map.Entry<String, ApplicationDescriptor> ai : deployedApplications.entrySet()) {
                for (ModuleDescriptor moduleDescriptor : ai.getValue().getModuleDescriptors()) {
                    String contextRoot = moduleDescriptor.getContextRoot();
                    if (contextRoot != null) {
                        for (Integer httpPort : httpPorts) {
                            try {
                                result.add(new URL("http", hostName.getCanonicalHostName(), httpPort, contextRoot));
                            } catch (MalformedURLException ex) {
                                // ignore
                            }
                        }
                        for (Integer httpsPort : httpsPorts) {
                            try {
                                result.add(new URL("https", hostName.getCanonicalHostName(), httpsPort, contextRoot));
                            } catch (MalformedURLException ex) {
                                // ignore
                            }
                        }
                    }
                }
            }
        }
        return result;
    }
    
    /**
     * Gets the instance group name
     * @return The instance group name
     */
    public String getInstanceGroup() {
        return instanceGroup;
    }
    
    /**
     * Sets the instance group name
     * @param instanceGroup The instance group name
     */
    public void setInstanceGroup(String instanceGroup) {
        this.instanceGroup = instanceGroup;
    }

}
