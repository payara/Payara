/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.enterprise.registration.RegistrationDescriptor;
import java.util.Date;
import java.util.UUID;
import java.util.Map;
import java.util.Date;
import java.util.Properties;
import java.util.List;
import java.util.ArrayList;

import org.w3c.dom.Element;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import java.io.File;


public class ServiceTag implements RegistrationDescriptor {
    
    
    public ServiceTag(Element tagData) {
        String instanceURN = getValue(tagData, INSTANCE_URN);
        
        if (instanceURN == null) {
            instanceURN = getNewInstanceURN();
        }
        svcTag = new SvcTag(instanceURN);
        svcTag.setTimestamp(new Date());
        
        setProductName(getValue(tagData, PRODUCT_NAME));
        setProductVersion(getValue(tagData, PRODUCT_VERSION));
        setProductURN(getValue(tagData, PRODUCT_URN));
        setProductParentURN(getValue(tagData, PRODUCT_PARENT_URN));
        setProductParent(getValue(tagData, PRODUCT_PARENT));
        setProductDefinedInstID(getValue(tagData, PRODUCT_DEFINED_INST_ID));
        setPlatformArch(getValue(tagData, PLATFORM_ARCH));
        setProductVendor(getValue(tagData, PRODUCT_VENDOR));
        setContainer(getValue(tagData, CONTAINER));
        setSource(getValue(tagData, SOURCE));
        setStatus(getValue(tagData, STATUS));
    }
    
    
    public ServiceTag(Properties tagData) {
        
        String instanceURN = tagData.getProperty(INSTANCE_URN);
        
        if (instanceURN == null) {
            instanceURN = getNewInstanceURN();
        }
        svcTag = new SvcTag(instanceURN);
        svcTag.setTimestamp(new Date());

        setProductName(tagData.getProperty(PRODUCT_NAME));
        setProductVersion(tagData.getProperty(PRODUCT_VERSION));
        setProductURN(tagData.getProperty(PRODUCT_URN));
        setProductParentURN(tagData.getProperty(PRODUCT_PARENT_URN ));
        setProductParent(tagData.getProperty(PRODUCT_PARENT));
        setProductDefinedInstID(tagData.getProperty(PRODUCT_DEFINED_INST_ID));
        setPlatformArch(tagData.getProperty(PLATFORM_ARCH));
        setProductVendor(tagData.getProperty(PRODUCT_VENDOR));
        setContainer(tagData.getProperty(CONTAINER));
        setSource(tagData.getProperty(SOURCE));
        setStatus(tagData.getProperty(STATUS));
    }
    
    public void setProductName(String str) {
        svcTag.setProductName(str);
    }
    
    public void setProductURN(String str) {
        svcTag.setProductURN(str);
    }
    
    public void setProductVersion(String str) {
        svcTag.setProductVersion(str);
    }
    
    public void setProductParentURN(String str) {
        svcTag.setProductParentURN(str);
    }
    
    public void setProductParent(String str) {
        // this is not in SvcTag, but is needed by stclient
        productParent = str;
    }
    
    public void setProductDefinedInstID(String str) {
        svcTag.setProductDefinedInstID(str);
    }
    
    public void setPlatformArch(String str) {
        // this is not in SvcTag, but is needed by stclient
        platformArch = str;            
    }

    public void setProductVendor(String str) {
        svcTag.setProductVendor(str);
    }
    
    public void setContainer(String str) {
        svcTag.setContainer(str);
    }
    
    public void setSource(String str) {
        svcTag.setSource(str);
    }
    
    public void setStatus(String status) {
        svcTag.setStatus(status);
    }

    // return the Sysnet SvcTag object...
    public SvcTag getSvcTag() {
        return svcTag;
    }

    private String getValue(Element rootElement, String subElementName ) {
        NodeList nodes = rootElement.getElementsByTagName(subElementName);
        if (nodes.getLength() > 0) {
            return ((Element)nodes.item(0)).getTextContent();
        }
        return null;        
    }

    public static List<SvcTag> getSvcTags(List<ServiceTag> serviceTags) {
        ArrayList<SvcTag> list = new ArrayList<SvcTag>();
        for (ServiceTag s : serviceTags) {
            list.add(s.getSvcTag());
        }
        return list;
    }
    
    public static String getNewInstanceURN() {
        return "urn:st:" + UUID.randomUUID().toString();        
    }

    public static String getDefaultProductDefinedInstID() {
        StringBuilder definedId = new StringBuilder();
        definedId.append(getPropertyString("os.name"));
        definedId.append(getPropertyString("os.version"));
        definedId.append(getPropertyString("java.version"));
        definedId.append(getPropertyString("java.home"));        
        return definedId.toString();
    }
    
    private static String getPropertyString(String name) {
        String prop = System.getProperty(name);
        if (prop != null) {
            return name + "=" + prop + ";";
        }
        return "";            
    }
    
    public String getInstanceURN() {
        return svcTag.getInstanceURN();
    }

    public String getProductName() {
        return svcTag.getProductName();
    }
    
    public String getProductURN() {
        return svcTag.getProductURN();
    }
        
    public String getProductVersion() {
        return svcTag.getProductVersion();
    }
    
    public String getProductParentURN() {
        return svcTag.getProductParentURN();
    }
        
    public String getProductDefinedInstID() {
        return svcTag.getProductDefinedInstID();
    }    
    
    public String getContainer() {
        return svcTag.getContainer();
    }
    
    public String getSource() {
        return svcTag.getSource();
    }
    
    public String getProductVendor() { 
        return svcTag.getProductVendor();
    }
    
    public String getPlatformArch() { 
        if (platformArch == null || platformArch.length() == 0) {
            platformArch = System.getProperty("os.arch");
        }
        return platformArch;
    }

    public String getProductParent() { 
        return productParent;
    }

    /* @returns and Element object that represents this ServiceTag */
    
    public Element getElement(Document doc) {
        Element rootElem = doc.createElement(SERVICE_TAG);

        addChild(doc, rootElem, INSTANCE_URN, getInstanceURN());
        addChild(doc, rootElem, PRODUCT_NAME, getProductName());
        addChild(doc, rootElem, PRODUCT_VERSION, getProductVersion());
        addChild(doc, rootElem, PRODUCT_URN, getProductURN());
        addChild(doc, rootElem, PRODUCT_PARENT_URN, getProductParentURN());
        addChild(doc, rootElem, PRODUCT_PARENT, getProductParent());
        addChild(doc, rootElem, PRODUCT_DEFINED_INST_ID, getProductDefinedInstID());
        addChild(doc, rootElem, PRODUCT_VENDOR, getProductVendor());
        addChild(doc, rootElem, PLATFORM_ARCH, getPlatformArch());
        addChild(doc, rootElem, CONTAINER, getContainer());
        addChild(doc, rootElem, SOURCE, getSource());

        addChild(doc, rootElem, STATUS, 
                Status.NOT_TRANSFERRED.toString());
        addChild(doc, rootElem, REGISTRATION_STATUS, 
                RegistrationStatus.NOT_REGISTERED.toString());
        return rootElem;
    }
    
    private void addChild(Document doc, Element rootElem, String name, String value) {
        Element element = doc.createElement(name);
        element.setTextContent(value);
        rootElem.appendChild(element);        
    }

    /*
    public void setStatus(String status) throws RegistrationException {
        getRepositoryManager().setStatus(this, Status.valueOf(status));
    }
    
    public String getStatus() throws RegistrationException {
        return (getRepositoryManager().getStatus(this)).toString();
    }

    public void setRegistrationStatus(String status) throws RegistrationException {
        getRepositoryManager().setRegistrationStatus(this, RegistrationStatus.valueOf(status));
    }
    
    public String getRegistrationStatus() throws RegistrationException {
        return getRepositoryManager().getRegistrationStatus(this).toString();   
    }
    
    private RepositoryManager getRepositoryManager() throws RegistrationException{
        return new RepositoryManager(new File(SysnetRegistrationService.getRepositoryFile()));
    }
  */    
    
    @Override
    public String toString() {
        return "ServiceTag:\n" + svcTag.toString();
    }

    public enum Status {
        TRANSFERRED,
        NOT_TRANSFERRED
    }
        
    private final SvcTag svcTag;
    
    // these are not in svcTag but are required by stclient.
    private String platformArch, productParent;
    
    public static final String PRODUCT_NAME     = "product_name";
    public static final String  PRODUCT_VERSION = "product_version";
    public static final String  PRODUCT_URN     = "product_urn";
    public static final String  PRODUCT_PARENT_URN = "product_parent_urn";
    public static final String  PRODUCT_PARENT  = "product_parent";
    public static final String  PRODUCT_DEFINED_INST_ID = "product_defined_inst_id";
    public static final String  PLATFORM_ARCH   = "platform_arch";
    public static final String  CONTAINER       = "container";
    public static final String  SOURCE          = "source";
    public static final String  INSTANCE_URN    = "instance_urn";
    public static final String  PRODUCT_VENDOR   = "product_vendor";
    public static final String  STATUS          = "status";
    public static final String  REGISTRATION_STATUS = "registration_status";
    
    public static final String  SERVICE_TAG = "service_tag";
    
    
}

