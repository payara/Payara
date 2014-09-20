/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.connectors.util;

import com.sun.enterprise.deployment.*;
import com.sun.enterprise.connectors.*;
import com.sun.logging.LogDomains;

import java.util.logging.*;
import java.util.*;
import java.lang.*;

import com.sun.enterprise.util.i18n.StringManager;
import com.sun.appserv.connectors.internal.api.ConnectorRuntimeException;

/**
 * This is AdminObject configuration parser. It parses the ra.xml file
 * for the admin object specific configurations like AdminObject javabeans
 * properties .
 *
 * @author Srikanth P
 */

public class AdminObjectConfigParserImpl implements AdminObjectConfigParser {

    private final static Logger _logger = LogDomains.getLogger(AdminObjectConfigParserImpl.class, LogDomains.RSR_LOGGER);

    /**
     * Default constructor.
     */
    public AdminObjectConfigParserImpl() {
    }

    /**
     * Parses the ra.xml for the admin object javabean properties.
     * The admin object to be parsed is identified by the moduleDir
     * where ra.xml is present and the adminObject interface.
     * Admin object interface will be unique in a given ra.xml.
     * <p/>
     * It throws ConnectorRuntimeException if either or both the
     * parameters are null, if corresponding rar is not deployed,
     * if no adminObjectInterce is found in ra.xml. If rar is deployed
     * and admin Object interface is present but no properties are
     * present for the corresponding adminobjectInterface, null is returned.
     *
     * @param desc                 ConnectorDescriptor pertaining to rar.
     * @param adminObjectInterface AdminObject interface
     * @return Javabean properties with the propety names and values
     *         of propeties. The property values will be the values
     *         mentioned in ra.xml if present. Otherwise it will be the
     *         default values obtained by introspecting the javabean.
     *         In both the case if no value is present, empty String is
     *         returned as the value.
     * @throws ConnectorRuntimeException if either of the parameters are null.
     *                                   If corresponding rar is not deployed i.e moduleDir is invalid.
     *                                   If no admin object intercface is found in ra.xml
     */

    public Properties getJavaBeanProps(ConnectorDescriptor desc,
                                       String adminObjectInterface, String rarName)
            throws ConnectorRuntimeException {
        return getJavaBeanProps(desc, adminObjectInterface, null, rarName);
    }

    /**
     * Parses the ra.xml for the admin object javabean properties.
     * The admin object to be parsed is identified by the moduleDir
     * where ra.xml is present and the adminObject interface.
     * Admin object interface will be unique in a given ra.xml.
     * <p/>
     * It throws ConnectorRuntimeException if either or both the
     * parameters are null, if corresponding rar is not deployed,
     * if no adminObjectInterce is found in ra.xml. If rar is deployed
     * and admin Object interface is present but no properties are
     * present for the corresponding adminobjectInterface, null is returned.
     *
     * @param desc                 ConnectorDescriptor pertaining to rar.
     * @param adminObjectInterface AdminObject interface 
     * @param adminObjectClass AdminObject class
     * @return Javabean properties with the propety names and values
     *         of propeties. The property values will be the values
     *         mentioned in ra.xml if present. Otherwise it will be the
     *         default values obtained by introspecting the javabean.
     *         In both the case if no value is present, empty String is
     *         returned as the value.
     * @throws ConnectorRuntimeException if either of the parameters are null.
     *                                   If corresponding rar is not deployed i.e moduleDir is invalid.
     *                                   If no admin object intercface is found in ra.xml
     */

    public Properties getJavaBeanProps(ConnectorDescriptor desc,
                                       String adminObjectInterface, String adminObjectClass, String rarName)
            throws ConnectorRuntimeException {

        AdminObject adminObject = getAdminObject(desc, adminObjectInterface, adminObjectClass);
        Properties mergedVals;
        if (adminObject == null) return null;
        mergedVals = getMergedValues(adminObject, rarName);


        return mergedVals;
    }

    private AdminObject getAdminObject(ConnectorDescriptor desc, String adminObjectInterface, String adminObjectClass)
            throws ConnectorRuntimeException {
        if (desc == null || adminObjectInterface == null ) {
            throw new ConnectorRuntimeException("Invalid arguments");
        }

        Set adminObjectSet = desc.getAdminObjects();
        if (adminObjectSet == null || adminObjectSet.size() == 0) {
            return null;
        }
        AdminObject adminObject = null;
        Iterator iter = adminObjectSet.iterator();
        Properties mergedVals = null;
        boolean adminObjectFound = false;
        while (iter.hasNext()) {
            adminObject = (AdminObject) iter.next();
            if (adminObjectInterface.equals(adminObject.getAdminObjectInterface()) &&
                    (adminObjectClass == null || adminObjectClass.equals(adminObject.getAdminObjectClass()))) {
                adminObjectFound = true;
                break;
            }
        }

        if (!adminObjectFound) {
            StringManager localStrings =
                    StringManager.getManager(AdminObjectConfigParserImpl.class);
            String msg = localStrings.getString(
                    "no_adminobject_interface_found_in_raxml", adminObjectInterface);
            if(_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE, msg);
            }
            throw new ConnectorRuntimeException(msg);
        }
        return adminObject;
    }

    private Properties getMergedValues(AdminObject adminObject, String raName) throws ConnectorRuntimeException {
        /* ddVals           -> Properties present in ra.xml
        *  introspectedVals -> All properties with values
        *                                 obtained by introspection of resource
        *                                  adapter javabean
        *  mergedVals       -> merged props of raConfigPros and
        *                                 allraConfigPropsWithDefVals
        */
        Set ddVals = adminObject.getConfigProperties();
        String className = adminObject.getAdminObjectClass();
        Properties mergedVals = null;
        if (className != null && className.length() != 0) {
            Properties introspectedVals =
                    configParserUtil.introspectJavaBean(className, ddVals, false, raName);
            mergedVals = configParserUtil.mergeProps(ddVals, introspectedVals);
        }
        return mergedVals;
    }

    /**
     * gets the adminObjectInterfaceNames pertaining to a rar.
     *
     * @param desc ConnectorDescriptor pertaining to rar.
     * @return Array of AdminObjectInterface names as Strings
     * @throws ConnectorRuntimeException if parsing fails
     */
    public String[] getAdminObjectInterfaceNames(ConnectorDescriptor desc)
            throws ConnectorRuntimeException {

        if (desc == null) {
            throw new ConnectorRuntimeException("Invalid arguments");
        }

        Set adminObjectSet = desc.getAdminObjects();
        if (adminObjectSet == null || adminObjectSet.size() == 0) {
            return null;
        }
        String[] adminObjectInterfaceNames = new String[adminObjectSet.size()];
        Iterator it = adminObjectSet.iterator();
        AdminObject aor = null;
        for (int i = 0; it.hasNext(); ++i) {
            aor = (AdminObject) it.next();
            adminObjectInterfaceNames[i] = aor.getAdminObjectInterface();
        }
        return adminObjectInterfaceNames;
    }

    /**
     * gets the adminObjectClassNames pertaining to a rar & a specific
     * adminObjectInterfaceName
     *
     * @param desc ConnectorDescriptor pertaining to rar.
     * @param intfName admin-object-interface name
     * @return Array of AdminObjectInterface names as Strings
     * @throws ConnectorRuntimeException if parsing fails
     */
    public String[] getAdminObjectClassNames(ConnectorDescriptor desc, String intfName)
            throws ConnectorRuntimeException {

        if (desc == null) {
            throw new ConnectorRuntimeException("Invalid arguments");
        }

        Set adminObjectSet = desc.getAdminObjects();
        if (adminObjectSet == null || adminObjectSet.size() == 0) {
            return null;
        }

        Iterator it = adminObjectSet.iterator();
        AdminObject aor = null;
        Set<String> adminObjectClasses = new HashSet<String>();
        for (int i = 0; it.hasNext(); ++i) {
            aor = (AdminObject) it.next();
            String adminObjectIntfName = aor.getAdminObjectInterface();
            if(adminObjectIntfName.equals(intfName)){
                adminObjectClasses.add(aor.getAdminObjectClass());
            }
        }
        String[] adminObjectClassNames = new String[adminObjectClasses.size()];
        adminObjectClassNames = adminObjectClasses.toArray(adminObjectClassNames);
        return adminObjectClassNames;
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasAdminObject(ConnectorDescriptor desc, String intfName, String className)
            throws ConnectorRuntimeException {
        if (desc == null || intfName == null || className == null) {
            throw new ConnectorRuntimeException("Invalid arguments");
        }

        Set adminObjectSet = desc.getAdminObjects();
        if (adminObjectSet == null || adminObjectSet.size() == 0) {
            return false;
        }

        Iterator it = adminObjectSet.iterator();
        AdminObject aor = null;
        for (int i = 0; it.hasNext(); ++i) {
            aor = (AdminObject) it.next();
            if(aor.getAdminObjectInterface().equals(intfName) &&
                    aor.getAdminObjectClass().equals(className)){
                return true;
            }
        }
        return false;
    }

    public List<String> getConfidentialProperties(ConnectorDescriptor desc, String rarName, String... keyFields)
            throws ConnectorRuntimeException {
        if(keyFields == null || keyFields.length == 0 || keyFields[0] == null){
            throw new ConnectorRuntimeException("adminObjectInterface must be specified");
        }
        String interfaceName = keyFields[0];
        String className = null;

        if(keyFields.length > 1){
           className = keyFields[1];
        }

        AdminObject adminObject = getAdminObject(desc, interfaceName, className );
        List<String> confidentialProperties = new ArrayList<String>();
        if(adminObject != null){
            Set configProperties = adminObject.getConfigProperties();
            if(configProperties != null){
                Iterator iterator = configProperties.iterator();
                while(iterator.hasNext()){
                    ConnectorConfigProperty ccp = (ConnectorConfigProperty)iterator.next();
                    if(ccp.isConfidential()){
                        confidentialProperties.add(ccp.getName());
                    }
                }
            }
        }
        return confidentialProperties;
    }
}
