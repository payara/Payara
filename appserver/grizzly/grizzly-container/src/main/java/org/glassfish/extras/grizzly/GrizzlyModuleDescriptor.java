/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.extras.grizzly;

import org.glassfish.api.deployment.archive.ReadableArchive;
import org.xml.sax.SAXException;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.util.Map;
import java.util.HashMap;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Descriptor for a grizzly application.
 *
 * @author Jerome Dochez
 */
public class GrizzlyModuleDescriptor {

    private final static String[] HANDLER_ELEMENTS = {"adapter", "http-handler"};
    final static String DescriptorPath = "META-INF/grizzly-glassfish.xml";
    final Map<String, String> tuples = new HashMap<String, String>();
    final Map<String, ArrayList<GrizzlyProperty>> adapterProperties = new HashMap<String,  ArrayList<GrizzlyProperty>>();

    GrizzlyModuleDescriptor(ReadableArchive source, Logger logger) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            parse(factory.newDocumentBuilder().parse(source.getEntry(DescriptorPath)));
        } catch (SAXException e) {
            logger.log(Level.SEVERE, e.getMessage(),e);
            throw new RuntimeException(e);
        } catch (IOException e) {
            logger.log(Level.SEVERE, e.getMessage(),e);
            throw new RuntimeException(e);
        } catch (ParserConfigurationException e) {
            logger.log(Level.SEVERE, e.getMessage(),e);
            throw new RuntimeException(e);
        }
    }

    private void parse(Document document) {
        Element element = document.getDocumentElement();
        for (String handlerElement : HANDLER_ELEMENTS) {
            NodeList adapters = element.getElementsByTagName(handlerElement);
            for (int i=0;i<adapters.getLength();i++) {
                Node adapter = adapters.item(i);
                NamedNodeMap attrs = adapter.getAttributes();
                NodeList properties = adapter.getChildNodes();
                ArrayList<GrizzlyProperty> list = new ArrayList<GrizzlyProperty>();

                // Read the properties to be set on a GrizzlyAdapter
                for (int j=0; j < properties.getLength(); j++){
                    Node property = properties.item(j);
                    NamedNodeMap values = property.getAttributes();
                   if (values != null){
                        list.add(new GrizzlyProperty(values.getNamedItem("name").getNodeValue(),
                                              values.getNamedItem("value").getNodeValue()));
                    }
                }

                adapterProperties.put(attrs.getNamedItem("class-name").getNodeValue(), list);
                addAdapter(attrs.getNamedItem("context-root").getNodeValue(),
                        attrs.getNamedItem("class-name").getNodeValue());
            }
        }
    }

    public void addAdapter(String contextRoot, String className) {
        if (tuples.containsKey(contextRoot)) {
            throw new RuntimeException("duplicate context root in configuration :" + contextRoot);
        }
        tuples.put(contextRoot, className);
    }
        
    public Map<String, String> getAdapters() {
        return tuples;
    }

    static class GrizzlyProperty{

        String name ="";
        String value = "";

        public GrizzlyProperty(String name, String value) {
            this.name = name;
            this.value = value;
        }

    }

    /**
     * Return the properties to be set on {@link Adapter}
     */
    Map<String,ArrayList<GrizzlyProperty>> getProperties(){
        return adapterProperties;
    }
}
