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

import com.sun.enterprise.registration.RegistrationService.RegistrationStatus;
import com.sun.enterprise.registration.RegistrationService.RegistrationReminder;
import com.sun.enterprise.registration.RegistrationException;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Logger;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;

import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;

/**
 *
 * @author msiraj
 * @author tjquinn
 */

/** Manages a local repository for service tags and registration status.
 * <p>
 * The client code specifies (in the constructor) the file path or the File 
 * which is to hold the persisted registration repository.  Note that any public 
 * method that modifies any part of the DOM for the repository also 
 * flushes the repository to the file before returning.
 * <p>
 * If the specified file exists, the manager loads its contents as the initial
 * value of the document.  If the file does not exist, the manager creates a
 * new in-memory document containing the top-level registry element and the
 * default registrationstatus element but does NOT write out this initial data.
 * Writes to the file occur only in response to the client code invoking one of
 * the methods that modifies the DOM.
 */

public class RepositoryManager {
    
    private Logger logger = Logger.getLogger(getClass().getPackage().getName());

    /**
     * Creates a new manager which persists the registration data in the specified File.
     * @throws RegistrationException for any errors creating the XML parser or
     * transformer used for reading and writing the registration data or during
     * the initial load of the repository from the local file
     */
    public RepositoryManager(File registrationFile) throws RegistrationException {
        this.registrationFile = registrationFile;
        try {
            logger.fine("RepositoryManager created for file " + registrationFile.getCanonicalPath());
            documentBuilder = documentBuilderFactory.newDocumentBuilder();
            transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount","4");
            loadOrCreateDocument();
        } catch (Exception e) {
            throw new RegistrationException(e);
        }
    }

    /**
     * Returns a list of all service tag objects represented in the repository.
     * @return List<ServiceTag>
     * the in-memory cache (if it is not already there)
     */
    public List<ServiceTag> getServiceTags() {
        NodeList nodes = document.getElementsByTagName(ServiceTag.SERVICE_TAG);
        List<ServiceTag> serviceTags = new ArrayList<ServiceTag>();
        
        /* nodes is guaranteed to be non-null */
        for(int i = 0 ; i < nodes.getLength();i++) {
            Element elem = (Element)nodes.item(i);
            serviceTags.add(new ServiceTag(elem));
        }
        return serviceTags;
    }
    
    /**
     * Returns the current registration status value.
     * Returns RegistrationService.REGISTERED if any one service tag is registered.
     * @return RegistrationService.RegistrationStatus enum for the current registration status
     */
    public RegistrationStatus getRegistrationStatus() {
        NodeList nodes = document.getElementsByTagName(ServiceTag.SERVICE_TAG);
        for(int i = 0 ; i < nodes.getLength();i++) {
            Element elem = (Element)nodes.item(i);
            RegistrationStatus rs = 
                    RegistrationStatus.valueOf(getSubElementValue(elem, ServiceTag.REGISTRATION_STATUS));
            if (rs.equals(RegistrationStatus.REGISTERED))
                return rs;
        }
        return RegistrationStatus.NOT_REGISTERED;
    }
    

    /**
     * Sets the registration status value for all service tags and persists the change 
     * to the on-disk repository.
     * @param status the new RegistrationStatus value
     * @throws RegistrationException for errors writing the registration data
     * to the local repository
     */
    public void setRegistrationStatus(RegistrationStatus status) throws RegistrationException {
        NodeList nodes = document.getElementsByTagName(ServiceTag.SERVICE_TAG);
        for(int i = 0 ; i < nodes.getLength();i++) {
            Element elem = (Element)nodes.item(i);
            setSubElementValue(elem, ServiceTag.REGISTRATION_STATUS, status.toString());
        }
        writeToFile();
    }

    /**
     * Returns the current registration status value.
     * @return RegistrationService.RegistrationReminder enum for the current registration status
     */
    public RegistrationReminder getRegistrationReminder() {
        Element regElement = findRegistrationReminderElement();
        return RegistrationReminder.valueOf(regElement.getTextContent());
    }

    /**
     * Sets the registration status value and persists the change to the on-disk
     * repository.
     * @param reminder the new RegistrationReminder value
     * @throws RegistrationException for errors writing the registration data
     * to the local repository
     */
    public void setRegistrationReminder(RegistrationReminder reminder) throws RegistrationException {
        findRegistrationReminderElement().setTextContent(reminder.toString());
        writeToFile();
    }


    /**
     * Adds an element for the specified ServiceTag object (and writes the revised
     * DOM to the file).
     * @param serviceTag the ServiceTag for which to add an XML element
     * @throws RegistrationException for errors writing the registration data
     * to the local repository
     */
    public void add(ServiceTag serviceTag) throws RegistrationException {
        Element st = findServiceTag(serviceTag);
        if (st != null) {
            throw new RegistrationException(StringManager.getString("rpmgr.svrTagExists"));
            //throw new RegistrationException("rpmgr.svrTagExists");
        }
        /*
         * Create a new Element in the active document using the specified 
         * service tag information.
         */
        st = serviceTag.getElement(document);
        Element registrationReminderElement = findRegistrationReminderElement();
        /*
         * Insert the new service tag just before the registration status element, if it
         * is found. Otherwise insert it under registryElement
         */
        if (registrationReminderElement != null) {
            registryElement.insertBefore(st, registrationReminderElement);
        }
        else {
            registryElement.appendChild(st);
        }

        writeToFile();
    }

    /**
     * Removes the XML element corresponding to the specified service tag (and
     * persists the change to the file).
     * @param serviceTag the ServiceTag for which the XML element should be removed
     * @throws RegistrationException for errors writing the registration data
     * to the local repository
     */
    public void remove(ServiceTag serviceTag) throws RegistrationException {
        Element st = findServiceTag(serviceTag);
        if (st == null) {
         throw new RegistrationException(StringManager.getString("rpmgr.noSuchSrvTag"));
         //throw new RegistrationException("rpmgr.noSuchSrvTag");
        }
        registryElement.removeChild(st);
        writeToFile();
    }
    
    /**
     * Sets the status for the specified service tag in the repository (and 
     * persists the change to the file).
     * @param serviceTag the service tag of interest
     * @param the status to be assigned
     * @throws RegistrationException for errors writing the registration data
     * to the local repository
     */
    public void setRegistrationStatus(ServiceTag serviceTag, ServiceTag.RegistrationStatus status) throws RegistrationException {
        Element st = findServiceTag(serviceTag);
        if (st == null) {
            throw new RegistrationException(StringManager.getString("rpmgr.noSuchSrvTag"));
        }
        setSubElementValue(st, ServiceTag.REGISTRATION_STATUS, status.toString());
        writeToFile();
    }

    public ServiceTag.RegistrationStatus getRegistrationStatus(ServiceTag serviceTag) throws RegistrationException {
        Element st = findServiceTag(serviceTag);
        if (st == null) {
            throw new RegistrationException(StringManager.getString("rpmgr.noSuchSrvTag"));
        }
        return ServiceTag.RegistrationStatus.valueOf(getSubElementValue(st, ServiceTag.REGISTRATION_STATUS));
    }

    public void setStatus(ServiceTag serviceTag, ServiceTag.Status status) throws RegistrationException {
        Element st = findServiceTag(serviceTag);
        if (st == null) {
            throw new RegistrationException(StringManager.getString("rpmgr.noSuchSrvTag"));
        }
        setSubElementValue(st, ServiceTag.STATUS, status.toString());
        writeToFile();
    }

    public ServiceTag.Status getStatus(ServiceTag serviceTag) throws RegistrationException {
        Element st = findServiceTag(serviceTag);
        if (st == null) {
            throw new RegistrationException(StringManager.getString("rpmgr.noSuchSrvTag"));
        }
        return ServiceTag.Status.valueOf(getSubElementValue(st, ServiceTag.STATUS));
    }
    
    public String getInstanceURN(String productURN) throws RegistrationException {
        NodeList nodes = document.getElementsByTagName(ServiceTag.SERVICE_TAG);
        for(int i = 0 ; i < nodes.getLength();i++) {
            Element elem = (Element)nodes.item(i);
            String productURN1 = getSubElementValue(elem, ServiceTag.PRODUCT_URN);
            if (productURN.equals(productURN1))
                return getSubElementValue(elem, ServiceTag.INSTANCE_URN);            
        }
        return null;
    }

    public boolean setInstanceURN(String productURN, String instanceURN) 
            throws RegistrationException {
        NodeList nodes = document.getElementsByTagName(ServiceTag.SERVICE_TAG);
        for(int i = 0 ; i < nodes.getLength();i++) {
            Element elem = (Element)nodes.item(i);
            String productURN1 = getSubElementValue(elem, ServiceTag.PRODUCT_URN);
            if (productURN.equals(productURN1)) {
                setSubElementValue(elem, ServiceTag.INSTANCE_URN, instanceURN);
                writeToFile();
                return true;
            }
        }
        //throw new RegistrationException("No such productURN : " + productURN);
        return false;
    }
    
    /**
     * Goes through all service tags in the repository, and updates runtime
     * values if necessary. 
     * @throws RegistrationException in case of errors writing the output
     */
    
    
    public void updateRuntimeValues() throws RegistrationException {
        NodeList nodes = document.getElementsByTagName(ServiceTag.SERVICE_TAG);
        
        /* nodes is guaranteed to be non-null */
        for(int i = 0 ; i < nodes.getLength();i++) {
            Element elem = (Element)nodes.item(i);
            /* generate value for instanceURN if necessary */
            String instanceURN = getSubElementValue(elem, ServiceTag.INSTANCE_URN);
            if (instanceURN == null || instanceURN.length() == 0) {
                instanceURN = ServiceTag.getNewInstanceURN();
                setSubElementValue(elem, ServiceTag.INSTANCE_URN, instanceURN, true);
            }

            /* generate value for platformArchif necessary */
            String platformArch = getSubElementValue(elem, ServiceTag.PLATFORM_ARCH);
            if (platformArch == null || platformArch.length() == 0) {
                platformArch = System.getProperty("os.arch");
                setSubElementValue(elem, ServiceTag.PLATFORM_ARCH, platformArch, true);
            }
            
            /* generate value for product_defined_inst_id if necessary */
            String product_defined_inst_id = getSubElementValue(elem, ServiceTag.PRODUCT_DEFINED_INST_ID);
            if (product_defined_inst_id == null || product_defined_inst_id.length() == 0) {
                product_defined_inst_id = ServiceTag.getDefaultProductDefinedInstID();
                setSubElementValue(elem, ServiceTag.PRODUCT_DEFINED_INST_ID, product_defined_inst_id, true);
            }            
        }
        writeToFile();        
    }

    
    /**
     * Writes the contents of the specified Node to the OutputStream provided.
     * <p>
     * Typically used for testing and debugging.  Not intended as part of the
     * normally-used public interface.
     * @param node the root of the subtree to write out
     * @param stream the OutputStream to which to write the XML
     * @throws RegistrationException in case of errors writing the output
     */
    void write(Node node, OutputStream stream) throws RegistrationException {
        try {
            transformer.transform(new DOMSource(node), new StreamResult(stream));
        } catch (Exception e) {
            throw new RegistrationException(e);
        }
    }
    
    /**
     * Writes the cached document to the provided output stream
     * <p>
     * Typically used for testing and debugging.  Not intended as part of the
     * normally-used public interface.
     * @param stream the OutputStream to which to write the document
     * @throws RegistrationException in case of errors writing the output
     */
    void write(OutputStream stream) throws RegistrationException {
        write(document, stream);
    }

    /**
     * Locates the registration status element from the document.
     * <p>
     * If absent, adds the element to the end of the document and initializes it to
     * the default value.
     * @return Element for the registration status 
     */
    private Element findRegistrationReminderElement() {
        Element result = null;
        NodeList nodes = document.getElementsByTagName(REGISTRATION_REMINDER_TAG);
        if (nodes.getLength() > 0) {
            /*
             * There should be at most one, but take the first if 
             * there is at least one.
             */
            result = (Element)nodes.item(0);
        }
        return result;
    }

    /**
     * Locates the servicetag element corresponding to the specified ServiceTag
     * object.
     * @param serviceTag the ServiceTag for which to find the corresponding element
     * @return the Element for that service tag; null if not found
     */
    private Element findServiceTag(ServiceTag serviceTag) {
        Element result = null;
        NodeList nodes = document.getElementsByTagName(ServiceTag.SERVICE_TAG);
        for (int i = 0; i < nodes.getLength(); i++) {
            if (isSameServiceTag(serviceTag, nodes.item(i))) {
                result = (Element) nodes.item(i);
                break;
            }
        }
        return result;
    }
    
    /**
     * Indicates whether the specified ServiceTag object and the specified Node
     * represent the same service tag, based on the unique identifying information.
     * @param serviceTag the ServiceTag of interest
     * @param candidateNode the node that may represent the ServiceTag
     * @return boolean if the node represents the specified service tag
     */
    private boolean isSameServiceTag(ServiceTag serviceTag, Node candidateNode) {
        boolean result = false;
        if (candidateNode instanceof Element) {
            Element candidateElement = (Element) candidateNode;
            String productURN = getSubElementValue(candidateElement, ServiceTag.PRODUCT_URN);
            String instanceURN = getSubElementValue(candidateElement, ServiceTag.INSTANCE_URN);
            if (productURN != null && instanceURN != null ) {
                result = productURN.equals(serviceTag.getSvcTag().getProductURN())
                      && instanceURN.equals(serviceTag.getSvcTag().getInstanceURN());
            }
        }
        return result;
    }
    
    
    private String getSubElementValue(Element rootElement, String subElementName ) {
        NodeList nodes = rootElement.getElementsByTagName(subElementName);
        if (nodes.getLength() > 0) {
            return ((Element)nodes.item(0)).getTextContent();
        }
        return null;        
    }

    private void setSubElementValue(Element rootElement, String subElementName, String value) 
            throws RegistrationException {
        setSubElementValue(rootElement, subElementName, value, false);
/*
        NodeList nodes = rootElement.getElementsByTagName(subElementName);
        if (nodes.getLength() > 0) {
            Element subElement = ((Element)nodes.item(0));
            subElement.setTextContent(value);
        }
        else {
            throw new RegistrationException(StringManager.getString("rpmgr.noSuchElement"));
        }
 */
    }
    
    private void setSubElementValue(Element rootElement, String subElementName, String value, boolean force) 
            throws RegistrationException {
        NodeList nodes = rootElement.getElementsByTagName(subElementName);
        
        if (nodes.getLength() > 0) {
            Element subElement = ((Element)nodes.item(0));
            subElement.setTextContent(value);
        }
        else {
            if (force) {
                Element element = document.createElement(subElementName);
                element.setTextContent(value);
                rootElement.appendChild(element);        
            }
            else {            
                throw new RegistrationException(StringManager.getString("rpmgr.noSuchElement"));
            }
        }
    }

    /**
     * Initializes the cached Document for the repository.
     * <p>
     * If the specified file exists, reads the document from that file.  If the 
     * file does not exist, creates a new document and populates it with the 
     * top-level registry element and the default registrationstatus element.
     * @throws RegistrationException for errors reading the registration into
     * the in-memory cache
     */
    private synchronized void loadOrCreateDocument() throws RegistrationException  {
        if (document == null) {
            if (registrationFile.exists()) {
                try {
                    document = documentBuilder.parse(registrationFile);
                    registryElement = findRegistryElement();
                } catch (Exception e) {
                    throw new RegistrationException(e);
                }
            }
            else {
                document = documentBuilder.newDocument();
                registryElement = document.createElement(REGISTRY_TAG);
                document.appendChild(registryElement);
                
                Element registrationStatusElement = document.createElement(REGISTRATION_REMINDER_TAG);
                registrationStatusElement.setTextContent(REGISTRATION_REMINDER_DEFAULT_VALUE.toString());
                registryElement.appendChild(registrationStatusElement);
                
            }
        }
    }
    
    /**
     * Writes the cached document to the on-disk file.
     * @throws RegistrationException for errors writing the cache into the file
     */
    private void writeToFile() throws RegistrationException {
        OutputStream os = null;
        try {
            os = new FileOutputStream(registrationFile);
            write(os);
        } catch (Exception e) {
            throw new RegistrationException(e);
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException ex) {
                    throw new RegistrationException(StringManager.getString("rpmgr.errClosingRepos"), ex);
                }
            }
        }
    }
    
    /**
     * Locates the top-level element, the registry element.
     * @return the top-level registry element
     */
    private Element findRegistryElement() {
        Element result = null;
        NodeList nodes = document.getElementsByTagName(REGISTRY_TAG);
        if (nodes.getLength() > 0) {
            result = (Element) nodes.item(0);
        }
        return result;
    }
    
    /** element name for the registrationstatus element */
    private static final String REGISTRATION_REMINDER_TAG = "registration_reminder";
    
    /** element name for the top-level registry element */
    private static final String REGISTRY_TAG = "registry";
    
    /** default value for the registration status element */
    private static final RegistrationReminder REGISTRATION_REMINDER_DEFAULT_VALUE = 
            RegistrationReminder.ASK_FOR_REGISTRATION;
    /*
     * Doc builder factory, doc builder, transformer factory, and transformer
     * are all reusable so get them once.
     */
    private final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
    private final DocumentBuilder documentBuilder; 
    
    private final TransformerFactory transformerFactory = TransformerFactory.newInstance();
    private final Transformer transformer;
    
    /** the file to write the registration data to and read it from */
    private File registrationFile = null;
    
    /** the cached in-memory data */
    private Document document = null;
    
    /** the cached top-level registry element */
    private Element registryElement = null;
    
    
}
