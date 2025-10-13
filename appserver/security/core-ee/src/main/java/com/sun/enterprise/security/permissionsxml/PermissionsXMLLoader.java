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
// Portions Copyright [2018-2019] [Payara Foundation and/or its affiliates]
package com.sun.enterprise.security.permissionsxml;

import static com.sun.enterprise.security.permissionsxml.PermissionsXMLParser.PERMISSIONS_XML;
import static java.util.logging.Level.FINE;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.PermissionCollection;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.Globals;
import org.xml.sax.SAXParseException;

import com.sun.enterprise.config.serverbeans.DasConfig;
import com.sun.enterprise.deployment.PermissionsDescriptor;
import com.sun.enterprise.deployment.io.PermissionsDeploymentDescriptorFile;
import com.sun.logging.LogDomains;

/**
 * Utility class to load application <code>permissions.xml</code> declared permissions from a <code>File</code> path
 * or <code>InputStream</code>.
 * 
 * <p>
 * This is a thin convenience layer over {@link PermissionsXMLParser}.
 *
 */
public class PermissionsXMLLoader {
    
    private static final Logger logger = Logger.getLogger(LogDomains.SECURITY_LOGGER);

    private static ServiceLocator serviceLocator = Globals.getDefaultBaseServiceLocator();
    private DasConfig dasConfig;

    private PermissionCollection declaredPermissionXml;
    private PermissionCollection restrictedPermissionCollection; // per app based restriction, not used for now

    private CommponentType componentType;
    

    public PermissionsXMLLoader(File base, CommponentType type) throws XMLStreamException, FileNotFoundException {
        this.componentType = type;

        loadAppPermissionsFromPath(base);
        checkServerRestrictedPermissions();
    }

    public PermissionsXMLLoader(InputStream restrictPermInput, InputStream allowedPermInput, CommponentType type) throws XMLStreamException, FileNotFoundException {
        this.componentType = type;

        loadAppPermissionsFromStream(allowedPermInput);
        checkServerRestrictedPermissions();
    }

    public PermissionCollection getAppDeclaredPermissions() {
        return declaredPermissionXml;
    }

    
    
    // ### Private methods   

    private void loadAppPermissionsFromPath(File base) throws XMLStreamException, FileNotFoundException {
        File permissionsXml = new File(base.getAbsolutePath(), PERMISSIONS_XML);

        if (permissionsXml.exists()) {
            try (FileInputStream permissionsXmlStream = new FileInputStream(permissionsXml)) {
                // This one uses the Node approach
                PermissionsDeploymentDescriptorFile pddf = getPermissionsDeploymentDescriptorFile();

                PermissionsDescriptor permissionsDescriptor = (PermissionsDescriptor) pddf.read(permissionsXmlStream);

                declaredPermissionXml = permissionsDescriptor.getDeclaredPermissions();

            } catch (SAXParseException | IOException e) {
                throw new SecurityException(e);
            }

            if (logger.isLoggable(FINE)) {
                logger.fine("App declared permission = " + declaredPermissionXml);
            }
        }
    }
    
    private void loadAppPermissionsFromStream(InputStream permissionsInput) throws XMLStreamException, FileNotFoundException {
        if (permissionsInput == null) {
            return;
        }
        
        // this one has no schema check (for client)
        PermissionsXMLParser parser = new PermissionsXMLParser(permissionsInput, restrictedPermissionCollection);
        declaredPermissionXml = parser.getPermissions();
        
        if (logger.isLoggable(FINE)) {
            logger.fine("App declared permission = " + declaredPermissionXml);
        }
    }
    
    private PermissionsDeploymentDescriptorFile getPermissionsDeploymentDescriptorFile() {
        PermissionsDeploymentDescriptorFile descriptorFile = new PermissionsDeploymentDescriptorFile();
        
        if (serviceLocator == null) {
            return descriptorFile;
        }

        dasConfig = serviceLocator.getService(DasConfig.class);
        if (dasConfig == null) {
            return descriptorFile;
        }
        
        String xmlValidationLevel = dasConfig.getDeployXmlValidation();
        if (xmlValidationLevel.equals("none")) {
            descriptorFile.setXMLValidation(false);
        } else {
            descriptorFile.setXMLValidation(true);
        }
        descriptorFile.setXMLValidationLevel(xmlValidationLevel);
        
        return descriptorFile;
    }

    // Check the app declared permissions against server restricted policy
    private void checkServerRestrictedPermissions() {
        if (declaredPermissionXml == null) {
            return; 
        }

        if (componentType == null) { // don't know the type, no check
            return;
        }

        GlobalPolicyUtil.checkRestrictionOfComponentType(declaredPermissionXml, componentType);
    }

}
