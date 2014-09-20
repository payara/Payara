/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.registration.glassfish;

import java.util.logging.Logger;


import com.sun.enterprise.util.SystemPropertyConstants;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.io.InputStream;

import com.sun.enterprise.registration.impl.SysnetRegistrationService;
import com.sun.enterprise.registration.impl.RepositoryManager;
import com.sun.enterprise.registration.impl.ServiceTag;
import com.sun.enterprise.registration.RegistrationException;
import com.sun.enterprise.registration.impl.RegistrationLogger;

import com.sun.pkg.client.Image;

public class RegistrationUtil {


    private static final Logger logger = RegistrationLogger.getLogger();
    private static final String REGISTRATION = "registration";
    private static final String LIB = "lib";
    private static final String SERVICE_TAG_REGISTRY_BASE = "servicetag-registry";
    private static final String SERVICE_TAG_REGISTRY_NAME = SERVICE_TAG_REGISTRY_BASE + ".xml";
    private static final String SERVICE_TAG_REGISTRY_LINK_NAME = SERVICE_TAG_REGISTRY_BASE + ".lnk";
    private static final String GLASSFISH_REGISTRY_PROPERTIES = "Registration.properties";

    /**
     * @return home for registration
     */
    public static File getRegistrationHome() {
        String installRoot = System.getProperty(SystemPropertyConstants.INSTALL_ROOT_PROPERTY);
        return getRegistrationHome(installRoot);
    }

    /**
     * @return home for registration relative to an installRoot
     */
    public static File getRegistrationHome(String installRoot) {
        File lib = new File(installRoot, LIB);
        File registration = new File(lib, REGISTRATION);
        return registration;
    }

    /**
     * @return Service tag file. Please note that it is possible that the file may not actually exist
     */
    public static File getServiceTagRegistry() {
        File serviceTagRegistry = new File(getRegistrationHome(), SERVICE_TAG_REGISTRY_NAME);
        return getServiceTagRegistry(serviceTagRegistry);
    }

    /**
     * @return Service tag file relative to an installRoot. Please note that it is possible that the file may not actually exist
     */
    public static File getServiceTagRegistry(String installRoot) {
        File serviceTagRegistry = new File(getRegistrationHome(installRoot), SERVICE_TAG_REGISTRY_NAME);
        return getServiceTagRegistry(serviceTagRegistry);
    }

    private static File getServiceTagRegistry(File serviceTagRegistry) {
        if (!serviceTagRegistry.exists()) {
            // It is possible that we are embedded inside other product check for a link to registration file
            File serviceTagLink = new File(getRegistrationHome(), SERVICE_TAG_REGISTRY_LINK_NAME);
            if (serviceTagLink.exists()) {
                BufferedReader in = null;
                try {
                    in = new BufferedReader(new FileReader(serviceTagLink));
                    //The first line in the link file is expected to contain fully qualified path to actual service tag repository
                    String indirectedServiceTagRegistryName = in.readLine();
                    if (indirectedServiceTagRegistryName != null) {
                        File indirectedServiceTagRegisitryFile = new File(indirectedServiceTagRegistryName);
                        if (indirectedServiceTagRegisitryFile.exists()) {
                            //Return indirectedServiceTagRegisitryFile as the serviceTagRegistry only if it exists
                            serviceTagRegistry = indirectedServiceTagRegisitryFile;
                        }
                    }
                } catch (IOException e) {
                    //I/O error occured. There is not much we can do to recover. Assumer that service tags are not present
                    //TODO: Check with Kedar, if a logger can be used here to log a debug message
                }
                finally {
                    if (in != null)
                        try {
                            in.close();
                        } catch (IOException ex) {
                        }

                }
            } else {
                //the link also does not exist. Fall through and return serviceTagRegistry as the
            }
          }
        return serviceTagRegistry;
    }

    public static String getGFProductURN() throws RegistrationException {
        try {
            InputStream is = RegistrationUtil.class.getClassLoader().getResourceAsStream(
                    GLASSFISH_REGISTRY_PROPERTIES);
            Properties props = new Properties();
            props.load(is);
            return props.getProperty("product_urn");
        } catch (Exception ex) {
            throw new RegistrationException(ex);
        }
    }

    public static String getGFInstanceURN() throws RegistrationException {
        SysnetRegistrationService srs = new SysnetRegistrationService(getServiceTagRegistry());

        List<ServiceTag> st = srs.getRegistrationDescriptors(getGFProductURN());
        if (st.isEmpty()) {
            throw new RegistrationException("Instance URN for " +
                    getGFProductURN() + " not found"); // i18n

        }
        return st.get(0).getInstanceURN();
    }
    
    public static void synchUUID() throws RegistrationException {
        RepositoryManager rm = new RepositoryManager(getServiceTagRegistry());
        String gfProductURN = getGFProductURN();
        String gfInstanceURN = rm.getInstanceURN(gfProductURN);
        if (gfInstanceURN == null || gfInstanceURN.length() == 0) {
            gfInstanceURN = ServiceTag.getNewInstanceURN();
            boolean updated = rm.setInstanceURN(gfProductURN, 
                    gfInstanceURN);
            if (!updated) {
                // couldn't set instance urn in servicetag file. This shouldn't
                // happen, but if it does, ignore it and do not update the UC
                // file
                logger.info("GlassFish instance URN not found");
                return;
            }
        }
        
        setUpdateCenterUUID(gfInstanceURN);
    }

    public static Image getUpdateCenterImage() throws Exception {
        File installRoot = 
            new File(System.getProperty(SystemPropertyConstants.INSTALL_ROOT_PROPERTY));
        return new Image (installRoot.getParent());
    }

    public static void setUpdateCenterUUID(String instanceURN)
            throws RegistrationException {
        final String prefix = "urn:st:";
        try {
            Image image = getUpdateCenterImage();
            String[] authorities = image.getAuthorityNames();
            if (instanceURN.startsWith(prefix))
                instanceURN = instanceURN.substring(prefix.length());
            for (String authority : authorities) {
                image.setAuthority(authority, null, instanceURN);
            }
            image.saveConfig();
        } catch(Exception ex) {
            throw new RegistrationException(ex);
        }
    }
}
