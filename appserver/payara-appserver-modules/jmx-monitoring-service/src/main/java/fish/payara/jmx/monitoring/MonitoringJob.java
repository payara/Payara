/*
 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 Copyright (c) 2016 C2B2 Consulting Limited. All rights reserved.
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
 *
 * @author savage
 */
public class MonitoringJob {

    private final ObjectName mBean;
    private final List<String> attributes;

    public MonitoringJob(ObjectName mBean, List<String> attributes) throws MalformedObjectNameException {
        this.mBean = mBean; 
        this.attributes = attributes;
    }

    public String getMonitoringInfo(MBeanServer server) {
        StringBuilder monitoringString = new StringBuilder();

        for (String attribute : attributes) {
                try {
                    Object responseObj = server.getAttribute(mBean, attribute);
                    String valueString = getValueString(attribute, responseObj);
                    monitoringString.append(valueString);
                    monitoringString.append(" ");
                } catch (MBeanException | AttributeNotFoundException | InstanceNotFoundException | ReflectionException ex) {
                    Logger.getLogger(MonitoringJob.class.getName()).log(Level.SEVERE, null, ex);
                }
        }

        return monitoringString.toString();
    }

    public ObjectName getMBean() {
        return mBean;
    }

    public List<String> getAttributes() {
        return attributes;
    }

    private String getValueString(String attributeName, Object attributeObj) {
        StringBuilder attributeString = new StringBuilder();       

        if (attributeObj.getClass() == CompositeDataSupport.class) {
            CompositeDataSupport compositeObj = (CompositeDataSupport) attributeObj;
            for (String entry : compositeObj.getCompositeType().keySet()) {
                attributeString.append(entry);
                attributeString.append(attributeName);
                attributeString.append("=");
                attributeString.append(compositeObj.get(entry).toString());
            }
        } else {
            attributeString.append(attributeName);
            attributeString.append("=");
            attributeString.append(attributeObj.toString());
        }

        return attributeString.toString();
    }

    public void addAttribute(String attribute) {
        if (!attributes.contains(attribute)) {
            attributes.add(attribute);
        }
    }

}
