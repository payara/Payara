/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2014 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package com.sun.enterprise.registration.impl.environment;

import org.w3c.dom.Element;
import java.util.Formatter;

public class EnvironmentInformation {
    private String hostname = null;
    private String hostId = null;
    private String osName = null;
    private String osVersion = null;
    private String osArchitecture = null;
    private String systemModel = null;
    private String systemManufacturer = null;
    private String cpuManufacturer = null;
    private String serialNumber = null;

    private String physmem = null;
    private String sockets = null;
    private String cores = null;
    private String virtcpus = null;
    private String cpuname = null;
    private String clockrate = null;

    public static void main(String args[]) {
        EnvironmentInformation ei = new EnvironmentInformation();
        System.out.println(ei.toXMLString());
    }

    /**
     * No-arg constructor.  Does a best effort job of capturing the various
     * environment information fields.
     */
    public EnvironmentInformation() {
        init(true, null, null, null, null, null, null, null, null, null,
            null, null, null, null, null, null);
    }

    /**
     * Constructs an EnvironmentInformation object based on the SystemEnvironment object.
     */
    public EnvironmentInformation(SystemEnvironment se) {
        init(false,
            se.getHostname(),
            se.getHostId(),
            se.getOsName(),
            se.getOsVersion(),
            se.getOsArchitecture(),
            se.getSystemModel(),
            se.getSystemManufacturer(),
            se.getCpuManufacturer(),
            se.getSerialNumber(),
            se.getPhysMem(),
            se.getSockets(),
            se.getCores(),
            se.getVirtCpus(),
            se.getCpuName(),
            se.getClockRate());
    }

    /**
     * Constructor which either returns an empty environment or a populated
     * environment from attempting to run system collection methods.  If you
     * don't want any system collection calls run, pass false.
     */
    public EnvironmentInformation(boolean collectEnvData) {
        init(collectEnvData, null, null, null, null, null, null, null, null,
            null, null, null, null, null, null, null);
    }

    /**
     * Constructs an environment information object from information contained
     * within the given Element object.  Does not attempt to collect any information
     * through system calls.
     */
    public EnvironmentInformation(Element e) {
        // pass to init, and let that handle filling out missing/empty fields.
        init(false, XMLUtil.getOptionalTextValue(e, "hostname"),
            XMLUtil.getOptionalTextValue(e, "hostId"),
            XMLUtil.getOptionalTextValue(e, "osName"),
            XMLUtil.getOptionalTextValue(e, "osVersion"),
            XMLUtil.getOptionalTextValue(e, "osArchitecture"),
            XMLUtil.getOptionalTextValue(e, "systemModel"),
            XMLUtil.getOptionalTextValue(e, "systemManufacturer"),
            XMLUtil.getOptionalTextValue(e, "cpuManufacturer"),
            XMLUtil.getOptionalTextValue(e, "serialNumber"),
            XMLUtil.getOptionalTextValue(e, "physmem"),
            XMLUtil.getOptionalTextValue(e, "sockets"),
            XMLUtil.getOptionalTextValue(e, "cores"),
            XMLUtil.getOptionalTextValue(e, "virtcpus"),
            XMLUtil.getOptionalTextValue(e, "name"),
            XMLUtil.getOptionalTextValue(e, "clockrate"));
    }

    /**
     * Constructs an environment information object from information contained
     * within the given Agent Element object.  Does not attempt to collect any information
     * through system calls.
     */
    public EnvironmentInformation(Element e, boolean isAgent) {
        // pass to init, and let that handle filling out missing/empty fields.
        init(false, XMLUtil.getOptionalTextValue(e, "host"),
            XMLUtil.getOptionalTextValue(e, "hostid"),
            XMLUtil.getOptionalTextValue(e, "system"),
            XMLUtil.getOptionalTextValue(e, "release"),
            XMLUtil.getOptionalTextValue(e, "architecture"),
            XMLUtil.getOptionalTextValue(e, "platform"),
            XMLUtil.getOptionalTextValue(e, "manufacturer"),
            XMLUtil.getOptionalTextValue(e, "cpu_manufacturer"),
            XMLUtil.getOptionalTextValue(e, "serial_number"),
            XMLUtil.getOptionalTextValue(e, "physmem"),
            XMLUtil.getOptionalTextValue(e, "sockets"),
            XMLUtil.getOptionalTextValue(e, "cores"),
            XMLUtil.getOptionalTextValue(e, "virtcpus"),
            XMLUtil.getOptionalTextValue(e, "name"),
            XMLUtil.getOptionalTextValue(e, "clockrate"));
    }

    /**
     * Constructs an environment information object with the given fields.  Note
     * that if any of the fields are null or empty, attempts will be made to fill
     * them out with values obtained through system calls.
     */
    public EnvironmentInformation(String hostname, String hostId, String osName,
            String osVersion, String osArchitecture, String systemModel,
            String systemManufacturer, String cpuManufacturer, String serialNumber,
            String physmem, String sockets, String cores, String virtcpus,
            String cpuname, String clockrate) {

        // pass to init, and let that handle filling out missing/empty fields.
        init(true, hostname, hostId, osName, osVersion, osArchitecture,
            systemModel, systemManufacturer, cpuManufacturer, serialNumber,
            physmem, sockets, cores, virtcpus, cpuname, clockrate);
    }

    /**
     * Sets the hostname.
     * @param hostname The hostname to set.
     */
    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    /**
     * Sets the os name.
     * @param osName The osName to set.
     */
    public void setOsName(String osName) {
        this.osName = osName;
    }

    /**
     * Sets the os version.
     * @param osVersion The osVersion to set.
     */
    public void setOsVersion(String osVersion) {
        this.osVersion = osVersion;
    }

    /**
     * Sets the physmem
     * @param physmem The physmem to set.
     */
    public void setPhysMem(String physmem) {
        this.physmem = physmem;
    }

    /**
     * Sets the sockets
     * @param sockets The sockets to set.
     */
    public void setSockets(String sockets) {
        this.sockets = sockets;
    }

    /**
     * Sets the cores
     * @param cores The cores to set.
     */
    public void setCores(String cores) {
        this.cores = cores;
    }

    /**
     * Sets the virtcpus
     * @param virtcpus The virtcpus to set.
     */
    public void setVirtCpus(String virtcpus) {
        this.virtcpus = virtcpus;
    }

    /**
     * Sets the cpuname
     * @param cpuname The cpuname to set.
     */
    public void setCpuName(String cpuname) {
        this.cpuname = cpuname;
    }

    /**
     * Sets the clockrate
     * @param clockrate The clockrate to set.
     */
    public void setClockRate(String clockrate) {
        this.clockrate = clockrate;
    }

    /**
     * Sets the os architecture.
     * @param osArchitecture The osArchitecture to set.
     */
    public void setOsArchitecture(String osArchitecture) {
        this.osArchitecture = osArchitecture;
    }

    /**
     * Sets the system model.
     * @param systemModel The systemModel to set.
     */
    public void setSystemModel(String systemModel) {
        this.systemModel = systemModel;
    }

    /**
     * Sets the system manufacturer.
     * @param systemManufacturer The systemManufacturer to set.
     */
    public void setSystemManufacturer(String systemManufacturer) {
        this.systemManufacturer = systemManufacturer;
    }

    /**
     * Sets the cpu manufacturer.
     * @param cpuManufacturer The cpuManufacturer to set.
     */
    public void setCpuManufacturer(String cpuManufacturer) {
        this.cpuManufacturer = cpuManufacturer;
    }

    /**
     * Sets the serial number.
     * @param serialNumber The serialNumber to set.
     */
    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    /**
     * Sets the hostid.  Truncates to a max length of 16 chars.
     * @param hostId The hostid to set.
     */
    public void setHostId(String hostId) {
        if (hostId == null || hostId.equals("null")) {
            hostId = "";
        }
        hostId = hostId.trim();
        if (hostId.length() > 16) {
            hostId = hostId.substring(0,16);
        }
        this.hostId = hostId;
    }

    /**
     * Returns the hostname.
     * @return The hostname.
     */
    public String getHostname() {
        return hostname;
    }

    /**
     * Returns the osName.
     * @return The osName.
     */
    public String getOsName() {
        return osName;
    }

    /**
     * Returns the osVersion.
     * @return The osVersion.
     */
    public String getOsVersion() {
        return osVersion;
    }

    /**
     * Returns the osArchitecture.
     * @return The osArchitecture.
     */
    public String getOsArchitecture() {
        return osArchitecture;
    }

    /**
     * Returns the systemModel.
     * @return The systemModel.
     */
    public String getSystemModel() {
        return systemModel;
    }

    /**
     * Returns the systemManufacturer.
     * @return The systemManufacturer.
     */
    public String getSystemManufacturer() {
        return systemManufacturer;
    }

    /**
     * Returns the serialNumber.
     * @return The serialNumber.
     */
    public String getSerialNumber() {
        return serialNumber;
    }

    /**
     * Returns the hostId.
     * @return The hostId.
     */
    public String getHostId() {
        return hostId;
    }

    /**
     * Returns the cpuManufacturer.
     * @return The cpuManufacturer.
     */
    public String getCpuManufacturer() {
        return cpuManufacturer;
    }

    public String getPhysMem() {
        return physmem;
    }

    public String getSockets() {
        return sockets;
    }

    public String getCores() {
        return cores;
    }

    public String getVirtCpus() {
        return virtcpus;
    }

    public String getCpuName() {
        return cpuname;
    }

    public String getClockRate() {
        return clockrate;
    }

    /**
     * Converts the environment information object into an XML format.
     * @return The XML string representing this object.
     */
    public String toXMLString() {
        StringBuilder sb = new StringBuilder();
        Formatter fmt = new Formatter(sb);

        fmt.format("<environment>");
        fmt.format("<hostname>%s</hostname>", hostname);
        fmt.format("<hostId>%s</hostId>", hostId);
        fmt.format("<osName>%s</osName>", osName);
        fmt.format("<osVersion>%s</osVersion>", osVersion);
        fmt.format("<osArchitecture>%s</osArchitecture>", osArchitecture);
        fmt.format("<systemModel>%s</systemModel>", systemModel);
        fmt.format("<systemManufacturer>%s</systemManufacturer>",
            systemManufacturer);
        fmt.format("<cpuManufacturer>%s</cpuManufacturer>", cpuManufacturer);
        fmt.format("<serialNumber>%s</serialNumber>",serialNumber);
        fmt.format("<physmem>%s</physmem>",physmem);
        fmt.format("<cpuinfo>");
        fmt.format("<sockets>%s</sockets>",sockets);
        fmt.format("<cores>%s</cores>",cores);
        fmt.format("<virtcpus>%s</virtcpus>",virtcpus);
        fmt.format("<name>%s</name>",cpuname);
        fmt.format("<clockrate>%s</clockrate>",clockrate);
        fmt.format("</cpuinfo>");
        fmt.format("</environment>");

        return sb.toString();
    }

    // inits the environment information object
    // if passed in data is null or empty, then attempts are made to obtain this
    // information via SystemEnvironment class
    private void init(boolean captureFromLocal, String hostname, String hostId,
            String osName, String osVersion,
            String osArchitecture, String systemModel, String systemManufacturer,
            String cpuManufacturer, String serialNumber, String physmem, String sockets,
            String cores, String virtcpus, String cpuname, String clockrate) {

        if (hostname != null) 
            this.hostname = hostname.trim();

        setHostId(hostId);

        if (osName != null) 
            this.osName = osName.trim();

        if (osVersion != null) 
            this.osVersion = osVersion.trim();

        if (osArchitecture != null) 
            this.osArchitecture = osArchitecture.trim();

        if (systemModel != null) 
            this.systemModel = systemModel.trim();

        if (systemManufacturer != null) 
            this.systemManufacturer = systemManufacturer.trim();

        if (cpuManufacturer != null) 
            this.cpuManufacturer = cpuManufacturer.trim();

        if (serialNumber != null) 
            this.serialNumber = serialNumber.trim();

        if (physmem != null) 
            this.physmem = physmem.trim();

        if (sockets != null)
            this.sockets = sockets.trim();

        if (cores != null) 
            this.cores = cores.trim();

        if (virtcpus != null) 
            this.virtcpus = virtcpus.trim();

        if (cpuname != null) 
            this.cpuname = cpuname.trim();

        if (clockrate != null) 
            this.clockrate = clockrate.trim();

        if (!captureFromLocal) {
            return;
        }

        if (hostname == null || hostname.equals("")
                || hostId == null || hostId.equals("")
                || osName == null || osName.equals("")
                || osVersion == null || osVersion.equals("")
                || osArchitecture == null || osArchitecture.equals("")
                || systemModel == null || systemModel.equals("")
                || systemManufacturer == null || systemManufacturer.equals("")
                || cpuManufacturer == null || cpuManufacturer.equals("")
                || serialNumber == null || serialNumber.equals("")
                || physmem == null || physmem.equals("")
                || sockets == null || sockets.equals("")
                || cores == null || cores.equals("")
                || virtcpus == null || virtcpus.equals("")
                || cpuname == null || cpuname.equals("")
                || clockrate == null || clockrate.equals("")) {
            SystemEnvironment se = SystemEnvironment.getSystemEnvironment();
            if (hostname == null || hostname.equals("")) {
                this.hostname = se.getHostname();
            }
            if (hostId == null || hostId.equals("")) {
                setHostId(se.getHostId());
            }
            if (osName == null || osName.equals("")) {
                this.osName = se.getOsName();
            }
            if (osVersion == null || osVersion.equals("")) {
                this.osVersion = se.getOsVersion();
            }
            if (osArchitecture == null || osArchitecture.equals("")) {
                this.osArchitecture = se.getOsArchitecture();
            }
            if (systemModel == null || systemModel.equals("")) {
                this.systemModel = se.getSystemModel();
            }
            if (systemManufacturer == null || systemManufacturer.equals("")) {
                this.systemManufacturer = se.getSystemManufacturer();
            }
            if (cpuManufacturer == null || cpuManufacturer.equals("")) {
                this.cpuManufacturer = se.getCpuManufacturer();
            }
            if (serialNumber == null || serialNumber.equals("")) {
                this.serialNumber = se.getSerialNumber();
            }
            if (physmem == null || physmem.equals("")) {
                this.physmem = se.getPhysMem();
            }
            if (sockets == null || sockets.equals("")) {
                this.sockets = se.getSockets();
            }
            if (cores == null || cores.equals("")) {
                this.cores = se.getCores();
            }
            if (virtcpus == null || virtcpus.equals("")) {
                this.virtcpus = se.getVirtCpus();
            }
            if (cpuname == null || cpuname.equals("")) {
                this.cpuname = se.getCpuName();
            }
            if (clockrate == null || clockrate.equals("")) {
                this.clockrate = se.getClockRate();
            }
        }
    }
}
