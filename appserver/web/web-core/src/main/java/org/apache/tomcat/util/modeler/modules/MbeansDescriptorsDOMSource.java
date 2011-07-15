/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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
 *
 *
 * This file incorporates work covered by the following copyright and
 * permission notice:
 *
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.tomcat.util.modeler.modules;

import org.apache.tomcat.util.modeler.*;
import org.apache.tomcat.util.modeler.util.DomUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.management.ObjectName;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


public class MbeansDescriptorsDOMSource extends ModelerSource
{
    private static Logger log = Logger.getLogger(MbeansDescriptorsDOMSource.class.getName());

    Registry registry;
    String type;
    List<ObjectName> mbeans=new ArrayList<ObjectName>();

    public void setRegistry(Registry reg) {
        this.registry=reg;
    }

    public void setLocation( String loc ) {
        this.location=loc;
    }

    /** Used if a single component is loaded
     *
     * @param type
     */
    public void setType( String type ) {
       this.type=type;
    }

    public void setSource( Object source ) {
        this.source=source;
    }

    public List<ObjectName> loadDescriptors( Registry registry, String location,
                                 String type, Object source)
            throws Exception
    {
        setRegistry(registry);
        setLocation(location);
        setType(type);
        setSource(source);
        execute();
        return mbeans;
    }

    public void execute() throws Exception {
        if( registry==null ) registry=Registry.getRegistry(null, null);

        try {
            InputStream stream=(InputStream)source;
            long t1=System.currentTimeMillis();
            Document doc=DomUtil.readXml(stream);
            // Ignore for now the name of the root element
            Node descriptorsN=doc.getDocumentElement();
            //Node descriptorsN=DomUtil.getChild(doc, "mbeans-descriptors");
            if( descriptorsN == null ) {
                log.log(Level.SEVERE,"No descriptors found");
                return;
            }

            Node firstMbeanN=null;
            if( "mbean".equals( descriptorsN.getNodeName() ) ) {
                firstMbeanN=descriptorsN;
            } else {
                firstMbeanN=DomUtil.getChild(descriptorsN, "mbean");
            }

            if( firstMbeanN==null ) {
                log.log(Level.SEVERE," No mbean tags ");
                return;
            }

            // Process each <mbean> element
            for (Node mbeanN = firstMbeanN; mbeanN != null;
                 mbeanN= DomUtil.getNext(mbeanN))
            {

                // Create a new managed bean info
                ManagedBean managed=new ManagedBean();
                DomUtil.setAttributes(managed, mbeanN);
                Node firstN;

                // Process descriptor subnode
                Node mbeanDescriptorN =
                    DomUtil.getChild(mbeanN, "descriptor");
                if (mbeanDescriptorN != null) {
                    Node firstFieldN =
                        DomUtil.getChild(mbeanDescriptorN, "field");
                    for (Node fieldN = firstFieldN; fieldN != null;
                         fieldN = DomUtil.getNext(fieldN)) {
                        FieldInfo fi = new FieldInfo();
                        DomUtil.setAttributes(fi, fieldN);
                        managed.addField(fi);
                    }
                }

                // process attribute nodes
                firstN=DomUtil.getChild( mbeanN, "attribute");
                for (Node descN = firstN; descN != null;
                     descN = DomUtil.getNext( descN ))
                {

                    // Create new attribute info
                    AttributeInfo ai=new AttributeInfo();
                    DomUtil.setAttributes(ai, descN);

                    // Process descriptor subnode
                    Node descriptorN =
                        DomUtil.getChild(descN, "descriptor");
                    if (descriptorN != null) {
                        Node firstFieldN =
                            DomUtil.getChild(descriptorN, "field");
                        for (Node fieldN = firstFieldN; fieldN != null;
                             fieldN = DomUtil.getNext(fieldN)) {
                            FieldInfo fi = new FieldInfo();
                            DomUtil.setAttributes(fi, fieldN);
                            ai.addField(fi);
                        }
                    }

                    // Add this info to our managed bean info
                    managed.addAttribute( ai );
                    if( log.isLoggable(Level.FINEST)) {
                        log.finest("Create attribute " + ai);
                    }

                }

                // process constructor nodes
                firstN=DomUtil.getChild( mbeanN, "constructor");
                for (Node descN = firstN; descN != null;
                     descN = DomUtil.getNext( descN )) {

                    // Create new constructor info
                    ConstructorInfo ci=new ConstructorInfo();
                    DomUtil.setAttributes(ci, descN);

                    // Process descriptor subnode
                    Node firstDescriptorN =
                        DomUtil.getChild(descN, "descriptor");
                    if (firstDescriptorN != null) {
                        Node firstFieldN =
                            DomUtil.getChild(firstDescriptorN, "field");
                        for (Node fieldN = firstFieldN; fieldN != null;
                             fieldN = DomUtil.getNext(fieldN)) {
                            FieldInfo fi = new FieldInfo();
                            DomUtil.setAttributes(fi, fieldN);
                            ci.addField(fi);
                        }
                    }

                    // Process parameter subnodes
                    Node firstParamN=DomUtil.getChild( descN, "parameter");
                    for (Node paramN = firstParamN;  paramN != null;
                         paramN = DomUtil.getNext(paramN))
                    {
                        ParameterInfo pi=new ParameterInfo();
                        DomUtil.setAttributes(pi, paramN);
                        ci.addParameter( pi );
                    }

                    // Add this info to our managed bean info
                    managed.addConstructor( ci );
                                
                    if( log.isLoggable(Level.FINEST)) {
                        log.finest("Create constructor " + ci);
                    }

                }

                // process notification nodes
                firstN=DomUtil.getChild( mbeanN, "notification");
                for (Node descN = firstN; descN != null;
                     descN = DomUtil.getNext( descN ))
                {

                    // Create new notification info
                    NotificationInfo ni=new NotificationInfo();
                    DomUtil.setAttributes(ni, descN);

                    // Process descriptor subnode
                    Node firstDescriptorN =
                        DomUtil.getChild(descN, "descriptor");
                    if (firstDescriptorN != null) {
                        Node firstFieldN =
                            DomUtil.getChild(firstDescriptorN, "field");
                        for (Node fieldN = firstFieldN; fieldN != null;
                             fieldN = DomUtil.getNext(fieldN)) {
                            FieldInfo fi = new FieldInfo();
                            DomUtil.setAttributes(fi, fieldN);
                            ni.addField(fi);
                        }
                    }

                    // Process notification-type subnodes
                    Node firstParamN=DomUtil.getChild( descN, "notification-type");
                    for (Node paramN = firstParamN;  paramN != null;
                         paramN = DomUtil.getNext(paramN))
                    {
                        ni.addNotifType( DomUtil.getContent(paramN) );
                    }

                    // Add this info to our managed bean info
                    managed.addNotification( ni );
                    if( log.isLoggable(Level.FINEST)) {
                        log.finest("Created notification " + ni);
                    }

                }

                // process operation nodes
                firstN=DomUtil.getChild( mbeanN, "operation");
                for (Node descN = firstN; descN != null;
                     descN = DomUtil.getNext( descN ))

                {

                    // Create new operation info
                    OperationInfo oi=new OperationInfo();
                    DomUtil.setAttributes(oi, descN);

                    // Process descriptor subnode
                    Node firstDescriptorN =
                        DomUtil.getChild(descN, "descriptor");
                    if (firstDescriptorN != null) {
                        Node firstFieldN =
                            DomUtil.getChild(firstDescriptorN, "field");
                        for (Node fieldN = firstFieldN; fieldN != null;
                             fieldN = DomUtil.getNext(fieldN)) {
                            FieldInfo fi = new FieldInfo();
                            DomUtil.setAttributes(fi, fieldN);
                            oi.addField(fi);
                        }
                    }

                    // Process parameter subnodes
                    Node firstParamN=DomUtil.getChild( descN, "parameter");
                    for (Node paramN = firstParamN;  paramN != null;
                         paramN = DomUtil.getNext(paramN))
                    {
                        ParameterInfo pi=new ParameterInfo();
                        DomUtil.setAttributes(pi, paramN);
                        if( log.isLoggable(Level.FINEST)) 
                            log.finest("Add param " + pi.getName());
                        oi.addParameter( pi );
                    }

                    // Add this info to our managed bean info
                    managed.addOperation( oi );
                    if( log.isLoggable(Level.FINEST)) {
                            log.finest("Create operation " + oi);
                    }

                }

                // Add the completed managed bean info to the registry
                registry.addManagedBean(managed);

            }

            if (log.isLoggable(Level.FINE)) {
                long t2=System.currentTimeMillis();
                log.log(Level.FINE, "Reading descriptors ( dom ) " + (t2-t1));
            }
        } catch( Exception ex ) {
            log.log(Level.SEVERE, "Error reading descriptors ", ex);
        }
    }
}
