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
package fish.payara.jmx.monitoring;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.openmbean.CompositeDataSupport;

/**
 * Class which gathers and returns monitoring information for a single MBean and a specified list of its attributes.
 *
 * @author savage
 */
public class MonitoringJob {

    private final ObjectName mBean;
    private final List<String> attributes;

    /**
     * Constructor for the MonitoringJob class.
     * 
     * @param mBean MBean containing the attributes to be monitored.
     * @param attributes Attribute names to be monitored.
     * @throws MalformedObjectNameException 
     */
    public MonitoringJob(ObjectName mBean, List<String> attributes) throws MalformedObjectNameException {
        this.mBean = mBean;
        this.attributes = attributes;
    }

    /**
     * Builds a String from the MonitoringJob's MBean.
     *  Loops through the attributes being monitored.
     *  For each attribute gets the object from the MBeanServer representing it.
     *  Gets the key-value pair of the attribute as a string and appends it.
     * 
     * @param server MBeanServer to get attributes values from.
     * @return Returns a monitoringString which contains key-value metrics.
     */
    public String getMonitoringInfo(MBeanServer server) {
        StringBuilder monitoringString = new StringBuilder();

        for (String attribute : attributes) {
            String[] attributeToks = attribute.split("\\.");
            try {
                Object responseObj = server.getAttribute(mBean, attributeToks[0]);
                String valueString = getValueString(attribute, responseObj);
                monitoringString.append(valueString);
            } catch (MBeanException | AttributeNotFoundException | InstanceNotFoundException | ReflectionException ex) {
                Logger.getLogger(MonitoringJob.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return monitoringString.toString();
    }

    /**
     * Returns the MBean monitored by the job.
     * 
     * @return The MBean monitored by the job.
     */
    public ObjectName getMBean() {
        return mBean;
    }
   
    /**
     * Returns the list of attributes being monitored.
     * 
     * @return The attributes being monitored.
     */
    public List<String> getAttributes() {
        return attributes;
    }

    /**
     * Adds an attribute to be monitored to the job.
     *  Will not add an attribute already being monitored.
     * @param attribute Name of attribute to be monitored.
     */
    public void addAttribute(String attribute) {
        if (!attributes.contains(attribute)) {
            attributes.add(attribute);
        }
    }


    /**
     * Gets the attribute value as a string.
     * 
     * @param attributeName Name of the attribute.
     * @param attributeObj The object representing the attribute.
     * @return Returns a string containing the key-value pair(s) for the attribute. 
     */
    private String getValueString(String attributeName, Object attributeObj) {
        StringBuilder attributeString = new StringBuilder();

        if (attributeObj.getClass() == CompositeDataSupport.class) {
            CompositeDataSupport compositeObj = (CompositeDataSupport) attributeObj;
            String[] attributeToks = attributeName.split("\\.");

            switch (attributeToks.length) {
                case 1:
                    String compositeString = getCompositeString(attributeToks[0], compositeObj); 
                    attributeString.append(compositeString);
                    break;
                case 2:
                    String attributeValue = compositeObj.get(attributeToks[1]).toString();
                    attributeString.append(attributeToks[1]);
                    attributeString.append(attributeToks[0]);
                    attributeString.append("=");
                    attributeString.append(attributeValue);
                    attributeString.append(" ");
                    break;
                default:
                    Logger.getLogger(MonitoringJob.class.getCanonicalName()).log(Level.WARNING, "Could not parse attribute `{0}` it should be of the form `AttributeName` or `AttributeName.property`", attributeName);
            }
        } else {
            attributeString.append(attributeName);
            attributeString.append("=");
            attributeString.append(attributeObj.toString());
            attributeString.append(" ");
        }

        return attributeString.toString();
    }

    /**
     * Gets a composite string for an attribute with multiple keys.
     * 
     * @param attributeName Name of the attribute.
     * @param compositeObj The composite object representing the attribute.
     * @return Returns a string containing the key-value pairs for the attribute.
     */
    private String getCompositeString(String attributeName, CompositeDataSupport compositeObj) {
        StringBuilder compositeString = new StringBuilder();

        for (String entry : compositeObj.getCompositeType().keySet()) {
            compositeString.append(entry);
            compositeString.append(attributeName);
            compositeString.append("=");
            compositeString.append(compositeObj.get(entry).toString());
            compositeString.append(" ");
        }

        return compositeString.toString();
    }

}
