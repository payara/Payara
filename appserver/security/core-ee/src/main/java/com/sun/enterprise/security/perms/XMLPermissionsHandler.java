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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.PermissionCollection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.xml.stream.XMLStreamException;

import org.xml.sax.SAXParseException;


import com.sun.enterprise.config.serverbeans.DasConfig;
import com.sun.enterprise.deployment.io.PermissionsDeploymentDescriptorFile;
import com.sun.enterprise.deployment.PermissionItemDescriptor;
import com.sun.enterprise.deployment.PermissionsDescriptor;
import com.sun.logging.LogDomains;

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.Globals;

/**
 * 
 * Utility class to get declared permissions  
 *
 */

public class XMLPermissionsHandler {
    
    
    private static ServiceLocator serviceLocator = Globals.getDefaultBaseServiceLocator();
    private DasConfig dasConfig;
    
    private PermissionCollection declaredPermXml = null;
    private PermissionCollection restrictedPC = null;  //per app based restriction, not used for now
    
    private SMGlobalPolicyUtil.CommponentType compType;
    
    private static final Logger logger = Logger.getLogger(LogDomains.SECURITY_LOGGER);

    public XMLPermissionsHandler(File base,  
            SMGlobalPolicyUtil.CommponentType type)
            throws XMLStreamException, FileNotFoundException {
        this.compType = type;
        
        configureAppDeclaredPermissions(base);
        checkServerRestrictedPermissions();
    }

    public XMLPermissionsHandler(InputStream restrictPermInput,
            InputStream allowedPermInput, 
            SMGlobalPolicyUtil.CommponentType type)
            throws XMLStreamException, FileNotFoundException {

        this.compType = type;
        
        configureAppDeclaredPermissions(allowedPermInput);
        checkServerRestrictedPermissions();
    }

    
    public PermissionCollection getAppDeclaredPermissions() {
        return declaredPermXml;
    }

    public PermissionCollection getRestrictedPermissions() {
        return restrictedPC;
    }


    private void configureAppDeclaredPermissions(File base)
            throws XMLStreamException, FileNotFoundException {

        File permissionsXml = new File(base.getAbsolutePath(),
                PermissionXMLParser.PERMISSIONS_XML);

        if (permissionsXml.exists()) {
            FileInputStream fi = null;
            try {
                //this one uses the Node approach
                PermissionsDeploymentDescriptorFile pddf = new PermissionsDeploymentDescriptorFile();
                
                if (serviceLocator != null) {
                    dasConfig = serviceLocator.getService(DasConfig.class);
                    if (dasConfig != null) {
                        String xmlValidationLevel = dasConfig.getDeployXmlValidation();
                        if (xmlValidationLevel.equals("none"))
                            pddf.setXMLValidation(false);
                        else
                            pddf.setXMLValidation(true);                    
                        pddf.setXMLValidationLevel(xmlValidationLevel);
                    }
                }
                    
                fi = new FileInputStream(permissionsXml);
                PermissionsDescriptor pd = (PermissionsDescriptor)pddf.read(fi);
                
                declaredPermXml = pd.getDeclaredPermissions();
                
            } catch (SAXParseException e) {
                throw new SecurityException(e);
            } catch (IOException e) {
                throw new SecurityException(e);
            } finally {
                if (fi != null) {
                    try {
                        fi.close();
                    } catch (IOException e) {
                    }            
                }
            }

            if (logger.isLoggable(Level.FINE)){
                logger.fine("App declared permission = " + declaredPermXml);
            }
        }        
    }

    
    
    private void configureAppDeclaredPermissions(InputStream permInput)
            throws XMLStreamException, FileNotFoundException {

        if (permInput != null) {
            //this one has no shchema check (for client)
            PermissionXMLParser parser = new PermissionXMLParser(
                    permInput, restrictedPC);
            this.declaredPermXml = parser.getPermissions();
            if (logger.isLoggable(Level.FINE)){
                logger.fine("App declared permission = " + declaredPermXml);
            }

        }
    }

    //check the app declared permissions against server restricted policy
    private void checkServerRestrictedPermissions()  {
     
        if (this.declaredPermXml == null)
            return;
        
        if (compType == null)  //don't know the type, no check
            return;
        
        SMGlobalPolicyUtil.checkRestrictionOfComponentType(declaredPermXml, this.compType);            
    }

    
}
