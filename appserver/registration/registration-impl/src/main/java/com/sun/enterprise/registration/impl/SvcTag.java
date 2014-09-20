/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2011 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.registration.impl;

import com.sun.enterprise.registration.impl.environment.XMLUtil;


import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.xml.sax.InputSource;

import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.Formatter;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;


/**
 *
 */
class SvcTag {
    public static final String REGISTERED = "REGISTERED";
    public static final String UNREGISTERED = "UNREGISTERED";
    public static final String UNINSTALLED = "UNINSTALLED";
    public static final String UNKNOWN = "UNKNOWN";
    public static final String MATCHED_DOMAINS = "MATCHED_DOMAINS";
    public static final String MISMATCHED_DOMAINS = "MISMATCHED_DOMAINS";
    private String productName;
    private String version;
    private String vendor;
    private String instanceURN;
    private String receivedIPAddress;
    private Date receivedTimestamp;
    private String userID;
    private String agentURN;
    private String productURN;
    private String status;
    private String subStatus;
    private String customerAssetTag;
    private Date timestamp;
    private String source;
    private String container;
    private int domainID;
    private String domainName;
    private int[] channelInstanceIDs;
    private String productDefinedInstID;
    private String productParentURN;
    private String agentVersion;
    private String helperVersion;
    private String registrationClientURN;

    /**
     * Constructor (TODO: make private?).
     *
     * @see #getNew
     * @see #getExisting
     */
    public SvcTag(String instanceURN) {
        this.instanceURN = instanceURN;
    }

    public static SvcTag getNew(String instanceURN) {
        return new SvcTag(instanceURN);
    }

    public static SvcTag getExisting(String instanceURN) {
        throw new RuntimeException("TODO: Implement");
    }

    public void setDomainId(int domainID) {
        this.domainID = domainID;
    }

    public void setChannelInstanceIDs(int[] channelInstanceIDs) {
        this.channelInstanceIDs = channelInstanceIDs;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setInstanceURN(String instanceURN) {
        this.instanceURN = instanceURN;
    }

    public void setReceivedIPAddress(String receivedIPAddress) {
        this.receivedIPAddress = receivedIPAddress;
    }

    public void setReceivedTimestamp(Date receivedTimestamp) {
        this.receivedTimestamp = receivedTimestamp;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public void setAgentURN(String agentURN) {
        this.agentURN = agentURN;
    }

    public void setProductURN(String productURN) {
        this.productURN = productURN;
    }

    public void setSubStatus(String subStatus) {
        this.subStatus = subStatus;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setAgentVersion(String agentVersion) {
        this.agentVersion = agentVersion;
    }

    public void setHelperVersion(String helperVersion) {
        this.helperVersion = helperVersion;
    }

    public void setRegistrationClientURN(String registrationClientURN) {
        this.registrationClientURN = registrationClientURN;
        if (this.registrationClientURN != null && this.registrationClientURN.length() > 64) {
            this.registrationClientURN = this.registrationClientURN.substring(0,64);
        }
    }

    public void setCustomerAssetTag(String customerAssetTag) {
        this.customerAssetTag = customerAssetTag;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public String getHelperVersion() {
        return helperVersion;
    }

    public String getDomainName() {
        return domainName;
    }

    public int[] getChannelInstanceIDs() {
        return channelInstanceIDs;
    }

    public int getDomainId() {
        return domainID;
    }

    public void setSource(String s) {
        source = s;
    }

    public String getSource() {
        return source;
    }

    public String getProductDefinedInstID() {
        return productDefinedInstID;
    }

    public void setProductDefinedInstID(String s) {
        productDefinedInstID = s;
    }

    public void setProductVersion(String s) {
        version = s;
    }

    public void setProductVendor(String s) {
        vendor = s;
    }

    public String getProductVendor() {
        return vendor;
    }

    public String getProductVersion() {
        return version;
    }

    public String getAgentVersion() {
        return agentVersion;
    }

    public String getRegistrationClientURN() {
        return registrationClientURN;
    }

    public String getContainer() {
        return container;
    }

    public void setContainer(String s) {
        container = s;
    }

    public String getProductParentURN() {
        return productParentURN;
    }

    public void setProductParentURN(String s) {
        productParentURN = s;
    }

    /**
     * Set the state of this SvcTag using the contents of the supplied
     * DOM Element.
     */
    public void setState(Element e) {
        if (!instanceURN.equals(XMLUtil.getRequiredTextValue(e, "instance_urn"))) {
            String msg = String.format("instance_urns do not match (%s != %s)",
                    instanceURN, XMLUtil.getRequiredTextValue(e, "instance_urn"));
            throw new RuntimeException(msg);
        }

        productName = XMLUtil.getRequiredTextValue(e, "product_name");

        version = XMLUtil.getOptionalTextValue(e, "product_version");
        if (version == null || version.equals("null")) version = "";

        vendor = XMLUtil.getOptionalTextValue(e, "product_vendor");
        if (vendor == null || vendor.equals("null")) vendor = "";

        userID = XMLUtil.getOptionalTextValue(e, "sun_user_id");
        if (userID == null || userID.equals("null")) userID = "";

        agentURN = XMLUtil.getOptionalTextValue(e, "agent_urn");
        if (agentURN == null || agentURN.equals("null")) agentURN = "";

        productURN = XMLUtil.getRequiredTextValue(e, "product_urn");

        source = XMLUtil.getOptionalTextValue(e, "source");
        if (source == null || source.equals("null")) source = "";

        receivedIPAddress = XMLUtil.getOptionalTextValue(e, "received_ip_address");
        if (receivedIPAddress != null &&
                (receivedIPAddress.trim().equals("null")
                || receivedIPAddress.trim().equals(""))) {
            receivedIPAddress = null;
        }

        customerAssetTag = XMLUtil.getOptionalTextValue(e, "customer_asset_tag");
        if (customerAssetTag == null || customerAssetTag.equals("null")) customerAssetTag = "";

        container = XMLUtil.getRequiredTextValue(e, "container");

        status = XMLUtil.getOptionalTextValue(e, "status");
        if ((status == null || status.equals("null")) || status.trim().equals("")) {
            status = SvcTag.UNKNOWN;
        }

        subStatus = XMLUtil.getOptionalTextValue(e, "sub_status");
        if ((subStatus == null || subStatus.equals("null")) || subStatus.trim().equals("")) {
            subStatus = "";
        }

        productDefinedInstID = XMLUtil.getOptionalTextValue(e, "product_defined_inst_id");
        if ((productDefinedInstID == null || productDefinedInstID.equals("null")) || productDefinedInstID.trim().equals("")) {
            productDefinedInstID = "";
        }

        //customerAssetTag = XMLUtil.getOptionalTextValue(e, "customerAssetTag");
        try {
            domainID = Integer.parseInt(XMLUtil.getOptionalTextValue(e,
                        "group_id"));
        } catch (NumberFormatException nfe) {
            domainID = -1;
        }

        Set<Integer> set = new HashSet<Integer>();
        List<String> list = XMLUtil.getOptionalTextValues(e, "channel_instance_id");
        if (list != null) {
            for (String s : list) {
                try {
                    set.add(Integer.parseInt(s));
                } catch (NumberFormatException nfe) {
                }
            }
        }

        channelInstanceIDs = new int[set.size()];
        int index=0;
        for (int id : set) {
            channelInstanceIDs[index++] = id;
        }

        domainName = XMLUtil.getOptionalTextValue(e, "group_name");
        if (domainName == null || domainName.equals("null")) domainName = "";

        agentVersion = XMLUtil.getOptionalTextValue(e, "agent_version");
        if (agentVersion == null || agentVersion.equals("null")) agentVersion = "";

        helperVersion = XMLUtil.getOptionalTextValue(e, "helper_version");
        if (helperVersion == null || helperVersion.equals("null")) helperVersion = "";

        registrationClientURN = XMLUtil.getOptionalTextValue(e, "registration_client_urn");
        if (registrationClientURN == null || registrationClientURN.equals("null")) registrationClientURN = "";
        if (registrationClientURN != null && registrationClientURN.length() > 64) {
            registrationClientURN = registrationClientURN.substring(0,64);
        }

        try {
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
            timestamp = df.parse(XMLUtil.getRequiredTextValue(e, "timestamp"));
        } catch (ParseException ex) {
            // TODO: something better
            throw new RuntimeException(ex);
        }
    }

    public String getProductName() {
        return productName;
    }

    public String getVersion() {
        return version;
    }

    public String getInstanceURN() {
        return instanceURN;
    }

    /**
     * Gets the IP address from which the current SvcTag state was
     * received (such as src IP of the Collector which sent the last
     * update to the Catcher).
     *
     * TODO: move elsewhere, since this may not be truly common?
     */
    public String getReceivedIPAddress() {
        return receivedIPAddress;
    }

    /**
     * Gets the time when the current SvcTag state was received (such as
     * when the Catcher got its last update from a Collector).
     *
     * TODO: move elsewhere, since this may not be truly common?
     */
    public Date getReceivedTimestamp() {
        // Need to return a clone, since Date is mutable
        if (receivedTimestamp == null) {
            receivedTimestamp = new Date();
        }
        return (Date) (receivedTimestamp.clone());
    }

    public String getUserID() {
        return userID;
    }

    public String getProductURN() {
        return productURN;
    }

    public String getAgentURN() {
        return agentURN;
    }

    public String getSubStatus() {
        return subStatus;
    }

    public String getStatus() {
        return status;
    }

    public String getCustomerAssetTag() {
        return customerAssetTag;
    }

    public Date getTimestamp() {
        // We return a clone because Date is mutable
        return (Date) (timestamp.clone());
    }

    public String getTimestampString() {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");

        return df.format(timestamp);
    }
    public String encode(String s) {
        if (s == null) {
            return s;
        }
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException uee) {
            return s;
        }
    }
}
