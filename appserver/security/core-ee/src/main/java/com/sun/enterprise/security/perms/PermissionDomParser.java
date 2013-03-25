/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.security.perms;

import java.io.File;
import java.io.IOException;
import java.security.PermissionCollection;
import java.security.Permissions;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;



public class PermissionDomParser {
    
    protected static final String PERMISSIONS_XML = "META-INF/permissions.xml"; 

    private static String SF = 
        "/scratch/bg/all/main/appserver/distributions/glassfish/target/stage/glassfish4/glassfish/lib/schemas/permissions_7.xsd"; 
    private static String xmlSF = 
        "/scratch/bg/all/main/appserver/distributions/glassfish/target/stage/glassfish4/glassfish/lib/schemas/xml.xsd"; 

    
    private PermissionCollection pc = new Permissions();
    
    
    public PermissionDomParser (File permissionsXmlFile) {
        
        try {
            System.out.println("Filename = " + permissionsXmlFile);
            Document d = getDocumentBuilder().parse(permissionsXmlFile);
            traverseTree(d.getDocumentElement());
        } catch (IOException e) {
            throw new SecurityException(e);
        } catch (SAXException e) {
            e.printStackTrace();
            throw new SecurityException(e);
        } catch (Exception e) {
            throw new SecurityException(e);
        }         

    }
    
    
    private DocumentBuilder getDocumentBuilder() throws Exception {
        
        System.setProperty("javax.xml.parsers.DocumentBuilderFactory", "com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl");
        DocumentBuilderFactory bf = DocumentBuilderFactory.newInstance();
        System.clearProperty("javax.xml.parsers.DocumentBuilderFactory"); 

        bf.setValidating(true);
        bf.setNamespaceAware(true);
        
        
        bf.setIgnoringComments(false);
        bf.setIgnoringElementContentWhitespace(true);
        bf.setCoalescing(true);
        
        
        
        Schema schema = getSchema();
        //bf.setSchema(schema);
        
        //File schemaFile = new File(SF);
        
        bf.setAttribute(
                "http://java.sun.com/xml/jaxp/properties/schemaLanguage", // NOI18N
                "http://www.w3.org/2001/XMLSchema"); // NOI18N
        //bf.setAttribute("http://java.sun.com/xml/jaxp/properties/schemaSource", schema);
        bf.setAttribute("http://apache.org/xml/properties/schema/external-schemaLocation", SF);
        
        DocumentBuilder builder = bf.newDocumentBuilder();
        builder.setErrorHandler(new DefaultHandler() {
            public void error(SAXParseException e) throws SAXException {
                throw e;
            }
        });
        return builder;
    }

    private void traverseTree(Node node) {
        if (node == null) {
            return;
        }
        if (node.getNodeType() == Node.ELEMENT_NODE) {
            Element e = (Element) node;
            String tagName = e.getTagName();
            System.out.println("tag name  = " + tagName);
            
            
            NodeList childNodes = node.getChildNodes();
            for (int i = 0; i < childNodes.getLength(); i++) {
                traverseTree(childNodes.item(i));
            }
        }
    }
    
    private static Schema permSchema;
    
    protected static Schema getSchema()  {

        if (permSchema != null)
            return permSchema;

        try{
            
            SchemaFactory xsFact = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            File schemaFile = new File(SF);
            StreamSource ss1 = new StreamSource(schemaFile);
            //File xmlSma = new File(xmlSF);
            //StreamSource ss2 = new StreamSource(xmlSma);
            permSchema = xsFact.newSchema(new StreamSource[]{ss1});


        } catch (SAXException e) {
            e.printStackTrace();
            throw new SecurityException(e);
        }
        
        return permSchema;
    }
    
    public final static void main(String[] args) {
        
        File permFile = 
            new File("/scratch/bg/all/main/appserver/tests/quicklook/shaun3/ear1/META-INF/permissions.xml");
        
        PermissionDomParser ps = new PermissionDomParser(permFile);
        
    }

    
    
}
