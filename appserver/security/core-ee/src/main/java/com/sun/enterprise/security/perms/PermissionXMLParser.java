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


import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
//import java.text.MessageFormat;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLResolver;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import static javax.xml.stream.XMLStreamConstants.END_DOCUMENT;
import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;

//import com.sun.enterprise.deploy.shared.LogMessageInfo;
import com.sun.enterprise.security.integration.PermissionCreator;

/**
 * Paser to parse permissions.xml packaged in a ear or in a standalone module
 */
public class PermissionXMLParser {

    protected static final String PERMISSIONS_XML = "META-INF/permissions.xml"; 
    protected static final String RESTRICTED_PERMISSIONS_XML = "META-INF/restricted-permissions.xml"; 

    protected XMLStreamReader parser = null;
    
    PermissionCollection pc = new Permissions();

    private PermissionCollection permissionCollectionToBeRestricted = null;
    
    private static XMLInputFactory xmlInputFactory;
    
    static {
        xmlInputFactory = XMLInputFactory.newInstance();
        xmlInputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        xmlInputFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, true);

        // set an zero-byte XMLResolver as IBM JDK does not take SUPPORT_DTD=false
        // unless there is a jvm option com.ibm.xml.xlxp.support.dtd.compat.mode=false
        xmlInputFactory.setXMLResolver(new XMLResolver() {
                @Override
                public Object resolveEntity(String publicID,
                        String systemID, String baseURI, String namespace)
                        throws XMLStreamException {

                    return new ByteArrayInputStream(new byte[0]);
                }
            });
    }

    
    //@LogMessageInfo(message = "This is an unexpected end of document", level = "WARNING")
    //public static final String UNEXPECTED_END_IN_XMLDOCUMENT = "NCLS-DEPLOYMENT-00048";

    
    public PermissionXMLParser(File permissionsXmlFile, 
            PermissionCollection permissionCollectionToBeRestricted)
            throws XMLStreamException, FileNotFoundException {
        
        FileInputStream fi = null;
        try {
            this.permissionCollectionToBeRestricted = permissionCollectionToBeRestricted;
            fi = new FileInputStream(permissionsXmlFile); 
            init(fi);
        } finally {
            if (fi != null)
                try {
                    fi.close();
                } catch (IOException e) {
                }            
        }
        
    }

    
    public PermissionXMLParser(InputStream input,
            PermissionCollection permissionCollectionToBeRestricted)
            throws XMLStreamException, FileNotFoundException {

        this.permissionCollectionToBeRestricted = permissionCollectionToBeRestricted;
        init(input);

    }
    
    protected static XMLInputFactory getXMLInputFactory() {
        return xmlInputFactory;
    }

    
    /**
     * This method will parse the input stream and set the XMLStreamReader
     * object for latter use.
     *
     * @param input InputStream
     * @exception XMLStreamException;
     */
    //@Override
    protected void read(InputStream input) throws XMLStreamException {
        parser = getXMLInputFactory().createXMLStreamReader(input);

        int event = 0;
        String classname = null;
        String target = null;
        String actions = null;
        while (parser.hasNext() && (event = parser.next()) != END_DOCUMENT) {
            if (event == START_ELEMENT) {
                String name = parser.getLocalName();
                if ("permission".equals(name)) {
                    classname = null;
                    target = null;
                    actions = null;
                } else if ("class-name".equals(name)) {
                    classname = parser.getElementText();
                } else if ("name".equals(name)) {
                    target = parser.getElementText();
                } else if ("actions".equals(name)) {
                    actions = parser.getElementText();
                } else if ("permissions".equals(name)) {
                    // continue trough subtree
                }  else {
                    skipSubTree(name);
                }
            }
            else if (event == END_ELEMENT) {
                String name = parser.getLocalName();
                if ("permission".equals(name)) {
                    if(classname != null && !classname.isEmpty()) {
                        addPermission(classname, target, actions);
                    }
                }
            }
        }
    }
    
    protected void init(InputStream input) throws XMLStreamException {

        try {
            read(input);
        } finally {
            if (parser != null) {
                try {
                    parser.close();
                } catch (Exception ex) {
                    // ignore
                }
            }
        }
    }

    protected void skipRoot(String name) throws XMLStreamException {
        while (true) {
            int event = parser.next();
            if (event == START_ELEMENT) {
                String localName = parser.getLocalName();
                if (!name.equals(localName)) {
                    //String msg = rb.getString(UNEXPECTED_ELEMENT_IN_XML);
                    //msg = MessageFormat.format(msg, new Object[] { name,
                    //        localName });
                    //throw new XMLStreamException(msg);
                    throw new  XMLStreamException("Unexpected element with name " + name);
                }
                return;
            }
        }
    }

    protected void skipSubTree(String name) throws XMLStreamException {
        while (true) {
            int event = parser.next();
            if (event == END_DOCUMENT) {
                throw new XMLStreamException(
                        //rb.getString(UNEXPECTED_END_IN_XMLDOCUMENT));
                        "Unexpected element with name " + name);
            } else if (event == END_ELEMENT
                    && name.equals(parser.getLocalName())) {
                return;
            }
        }
    }
    
    private void addPermission(String classname, String target, String actions)  {
        try {
            Permission pm = PermissionCreator.getInstance(classname, target, actions);

            if (pm != null) {
                if (permissionCollectionToBeRestricted != null && permissionCollectionToBeRestricted.implies(pm)) {
                    throw new SecurityException("Restricted Permission Declared - fail deployment!");
                }
                else {
                    pc.add(pm);
                }
            }
        } catch (ClassNotFoundException e) {
            throw new SecurityException(e);
        } catch (NoSuchMethodException e) {
            throw new SecurityException(e);
        } catch (InstantiationException e) {
            throw new SecurityException(e);
        } catch (IllegalAccessException e) {
            throw new SecurityException(e);
        } catch (InvocationTargetException e) {
            throw new SecurityException(e);
        }
    }
    
    
    
    protected PermissionCollection getPermissions() {
        return pc;
    }

}
