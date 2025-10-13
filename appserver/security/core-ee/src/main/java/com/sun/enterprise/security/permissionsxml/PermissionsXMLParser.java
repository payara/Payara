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
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
// Portions Copyright [2018] [Payara Foundation and/or its affiliates]
package com.sun.enterprise.security.permissionsxml;

import static javax.xml.stream.XMLInputFactory.IS_NAMESPACE_AWARE;
import static javax.xml.stream.XMLInputFactory.SUPPORT_DTD;
import static javax.xml.stream.XMLStreamConstants.END_DOCUMENT;
import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
//import java.text.MessageFormat;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

//import com.sun.enterprise.deploy.shared.LogMessageInfo;
import com.sun.enterprise.security.integration.PermissionCreator;

/**
 * Parser to parse <code>permissions.xml</code> packaged in an EAR or in a standalone module.
 * 
 * <p>
 * This class is primarily used via {@link PermissionsXMLLoader}.
 * 
 */
public class PermissionsXMLParser {

    protected static final String PERMISSIONS_XML = "META-INF/permissions.xml";
    protected static final String RESTRICTED_PERMISSIONS_XML = "META-INF/restricted-permissions.xml";

    protected XMLStreamReader parser;

    private PermissionCollection permissions = new Permissions();
    private PermissionCollection permissionCollectionToBeRestricted;

    private static XMLInputFactory xmlInputFactory;

    static {
        xmlInputFactory = XMLInputFactory.newInstance();
        xmlInputFactory.setProperty(SUPPORT_DTD, false);
        xmlInputFactory.setProperty(IS_NAMESPACE_AWARE, true);

        // Set an zero-byte XMLResolver as IBM JDK does not take SUPPORT_DTD=false
        // unless there is a jvm option com.ibm.xml.xlxp.support.dtd.compat.mode=false
        xmlInputFactory.setXMLResolver((p,s,b,n) -> new ByteArrayInputStream(new byte[0]));
    }

    public PermissionsXMLParser(File permissionsXmlFile, PermissionCollection permissionCollectionToBeRestricted) throws XMLStreamException, FileNotFoundException {
        try (FileInputStream inputStream = new FileInputStream(permissionsXmlFile)) {
            this.permissionCollectionToBeRestricted = permissionCollectionToBeRestricted;
            init(inputStream);
        } catch (IOException e) {
        }
    }

    public PermissionsXMLParser(InputStream input, PermissionCollection permissionCollectionToBeRestricted) throws XMLStreamException, FileNotFoundException {
        this.permissionCollectionToBeRestricted = permissionCollectionToBeRestricted;
        init(input);
    }
    
    /**
     * Returns the permissions that have been loaded and parsed from the <code>File</code> or <code>Stream</code>
     * given in the constructor of this class.
     * 
     * @return Permissions parsed from input
     */
    public PermissionCollection getPermissions() {
        return permissions;
    }

    
    // ### Private methods
    
    private static XMLInputFactory getXMLInputFactory() {
        return xmlInputFactory;
    }

    /**
     * This method will parse the input stream and set the XMLStreamReader object for latter use.
     *
     * @param input InputStream
     * @exception XMLStreamException;
     */
    // @Override
    private void read(InputStream input) throws XMLStreamException {
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
                } else {
                    skipSubTree(name);
                }
            } else if (event == END_ELEMENT) {
                String name = parser.getLocalName();
                if ("permission".equals(name)) {
                    if (classname != null && !classname.isEmpty()) {
                        addPermission(classname, target, actions);
                    }
                }
            }
        }
    }

    private void init(InputStream input) throws XMLStreamException {
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

    private void skipSubTree(String name) throws XMLStreamException {
        while (true) {
            int event = parser.next();
            if (event == END_DOCUMENT) {
                throw new XMLStreamException("Unexpected element with name " + name);
            } else if (event == END_ELEMENT && name.equals(parser.getLocalName())) {
                return;
            }
        }
    }

    private void addPermission(String classname, String target, String actions) {
        try {
            Permission permission = PermissionCreator.getInstance(classname, target, actions);

            if (permission != null) {
                if (permissionCollectionToBeRestricted != null && permissionCollectionToBeRestricted.implies(permission)) {
                    throw new SecurityException("Restricted Permission Declared - fail deployment!");
                }
                
                permissions.add(permission);
            }
        } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new SecurityException(e);
        }
    }

}
