/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.config.modularity.parser;

import com.sun.enterprise.config.modularity.customization.ConfigBeanDefaultValue;
import com.sun.enterprise.config.modularity.customization.ConfigCustomizationToken;
import com.sun.enterprise.util.LocalStringManager;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author Masoud Kalali
 */
public class ServiceConfigurationParser {
    private static final String CONFIG_BUNDLE = "config-bundle";
    private static final String LOCATION = "location";
    private static final String REPLACE_IF_EXISTS = "replace-if-exist";
    private static final String NAME = "name";
    private static final String DEFAUL_VALUE = "default-value";
    private static final String DESCRIPTION = "description";
    private static final String CONFIGURATION_ELEMENT = "configuration-element";
    private static final String CUSTOMIZATION_TOKEN = "customization-token";
    private static final String TITLE = "title";
    private static final String CONFIG_BEAN_CLASS_NAME = "config-bean-class-name";
    private LocalStringManager localStrings;

    public ServiceConfigurationParser(LocalStringManager localStrings) {
        this.localStrings = localStrings;
    }

    public List<ConfigBeanDefaultValue> parseServiceConfiguration(InputStream xmlDocumentStream ) throws XMLStreamException {

        List<ConfigBeanDefaultValue> configBeans = new ArrayList<ConfigBeanDefaultValue>();
        XMLInputFactory inputFactory = XMLInputFactory.newInstance();
        inputFactory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
        XMLEventReader eventReader = inputFactory.createXMLEventReader(xmlDocumentStream);
        ConfigBeanDefaultValue configValue = null;

        while (eventReader.hasNext()) {
            XMLEvent event = eventReader.nextEvent();
            if (event.isStartElement()) {
                StartElement startElement = event.asStartElement();
                // If we have a item element we create a new item
                if (startElement.getName().getLocalPart().equalsIgnoreCase(CONFIG_BUNDLE)) {

                    configValue = new ConfigBeanDefaultValue();
                    Iterator<Attribute> attributes = startElement.getAttributes();
                    while (attributes.hasNext()) {
                        Attribute attribute = attributes.next();
                        if (attribute.getName().toString().equals(LOCATION)) {
                            configValue.setLocation(attribute.getValue());
                        } else if (attribute.getName().toString().equals(REPLACE_IF_EXISTS)) {
                            configValue.setReplaceCurrentIfExists(Boolean.getBoolean(attribute.getValue()));
                        }
                    }//attributes
                    continue;
                }//config bundle

                if (startElement.getName().getLocalPart().equalsIgnoreCase(CUSTOMIZATION_TOKEN)) {
                    ConfigCustomizationToken token;
                    String value = null;
                    String description = null;
                    String name = null;
                    String title = null;

                    Iterator<Attribute> attributes = startElement.getAttributes();
                    while (attributes.hasNext()) {
                        Attribute attribute = attributes.next();
                        if (attribute.getName().toString().equals(DEFAUL_VALUE)) {
                            value = attribute.getValue();
                        } else if (attribute.getName().toString().equals(DESCRIPTION)) {
                            description = getLocalizedValue(attribute.getValue());
                        } else if (attribute.getName().toString().equals(NAME)) {
                            name = attribute.getValue();
                        } else if (attribute.getName().toString().equals(TITLE)) {
                            title = getLocalizedValue(attribute.getValue());
                        }
                    }//attributes

                    token = new ConfigCustomizationToken(name, title, description, value);
                    //TODO check that ConfigValue is not null
                    configValue.addCustomizationToken(token);
                    continue;
                }
                if (startElement.getName().getLocalPart().equalsIgnoreCase(CONFIGURATION_ELEMENT)) {
                    Iterator<Attribute> attributes = startElement.getAttributes();
                    while (attributes.hasNext()) {
                        Attribute attribute = attributes.next();
                        if (attribute.getName().toString().equals(CONFIG_BEAN_CLASS_NAME)) {
                            configValue.setConfigBeanClassName(attribute.getValue());
                        }
                    }//attributes
                    event = eventReader.nextEvent();
                    if (event.isCharacters()) {
                        String str = event.asCharacters().getData();
                        configValue.setXmlConfiguration(str);
                    }
                    continue;
                }
            }//isStartElement
            if (event.isEndElement()) {
                EndElement endElement = event.asEndElement();
                if (endElement.getName().getLocalPart().equalsIgnoreCase(CONFIG_BUNDLE)) {
                    configBeans.add(configValue);
                }
            }
        }//eventReader
        return configBeans;
    }

    private String getLocalizedValue(String value) {
        if(value.startsWith("$")){
            value = localStrings.getLocalString(value.substring(1,value.length()),value.substring(1,value.length()));
        }
            return value;
    }


}
