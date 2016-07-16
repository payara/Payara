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
package fish.payara.nucleus.hazelcast;

import java.io.File;
import java.net.URI;

/**
 *
 * @author steve
 */
public class MulticastConfiguration {
    private String multicastGroup = "224.2.2.3";
    private int multicastPort = 54327;
    private int startPort = 5900;
    private String memberName;
    private URI alternateConfigFile;
    private boolean lite = false;
    private String clusterGroupName = "development";
    private String clusterGroupPassword = "D3v3l0pm3nt";
    private String licenseKey;

    public MulticastConfiguration() {
    }

    public MulticastConfiguration(String multicastGroup, int multicastPort, int startPort, String memberName, URI alternateConfigurationFile, String licenseKey) {
        this.multicastGroup = multicastGroup;
        this.multicastPort = multicastPort;
        this.startPort = startPort;
        this.memberName = memberName;
        this.alternateConfigFile = alternateConfigurationFile;
        this.licenseKey = licenseKey;
    }

    public void setMulticastGroup(String multicastGroup) {
        this.multicastGroup = multicastGroup;
    }

    public String getMulticastGroup() {
        return multicastGroup;
    }

    public void setMulticastPort(int multicastPort) {
        this.multicastPort = multicastPort;
    }

    public int getMulticastPort() {
        return multicastPort;
    }

    public void setStartPort(int startPort) {
        this.startPort = startPort;
    }

    public int getStartPort() {
        return startPort;
    }

    public String getMemberName() {
        return memberName;
    }

    public void setMemberName(String memberName) {
        this.memberName = memberName;
    }

    public void setAlternateConfiguration(URI alternateHZConfigFile) {
        alternateConfigFile = alternateHZConfigFile;
    }

    public URI getAlternateConfigFile() {
        return alternateConfigFile;
    }

    public void setLicenseKey(String licenseKey) {
        this.licenseKey = licenseKey;
    }

    public String getLicenseKey() {
        return licenseKey;
    }

    public boolean isLite() {
        return lite;
    }

    public void setLite(boolean lite) {
        this.lite = lite;
    }

    public String getClusterGroupName() {
        return clusterGroupName;
    }

    public void setClusterGroupName(String clusterGroupName) {
        this.clusterGroupName = clusterGroupName;
    }

    public String getClusterGroupPassword() {
        return clusterGroupPassword;
    }

    public void setClusterGroupPassword(String clusterGroupPassword) {
        this.clusterGroupPassword = clusterGroupPassword;
    }

    
}
