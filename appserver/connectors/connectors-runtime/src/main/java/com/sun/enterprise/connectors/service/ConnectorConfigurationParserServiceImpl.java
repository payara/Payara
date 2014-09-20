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
 */

package com.sun.enterprise.connectors.service;

import com.sun.enterprise.deployment.ConnectorDescriptor;
import com.sun.enterprise.deployment.SecurityPermission;
import com.sun.enterprise.connectors.util.*;
import com.sun.appserv.connectors.internal.api.ConnectorRuntimeException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Set;
import java.util.Iterator;
import java.util.Properties;
import java.util.logging.Level;


/**
 * This is configuration parser service. Retrieves various configuration
 * information from ra.xml
 * @author    Srikanth P
 */
public class ConnectorConfigurationParserServiceImpl extends ConnectorService {

    /**
     * Default constructor
     */
     public ConnectorConfigurationParserServiceImpl() {
         super();
     }

    /**
     * Obtains the Permission string that needs to be added to the
     * to the security policy files. These are the security permissions needed
     * by the resource adapter implementation classes.
     * These strings are obtained by parsing the ra.xml
     *
     * @param moduleName rar module Name
     * @return Required policy permissions in server.policy file
     * @throws ConnectorRuntimeException If rar.xml parsing fails.
     */
    public String getSecurityPermissionSpec(String moduleName)
            throws ConnectorRuntimeException {

        if (moduleName == null) {
            return null;
        }
        String policyString = null;

        //check whether the policy file already has required permissions.
        String fileName = System.getProperty("java.security.policy");
        if (fileName != null) {
            File policyFile = new File(fileName);
            String policyContent = getFileContent(policyFile);

            ConnectorDescriptor connectorDescriptor = getConnectorDescriptor(moduleName);
            Set securityPermissions = connectorDescriptor.getSecurityPermissions();
            Iterator it = securityPermissions.iterator();
            SecurityPermission secPerm = null;
            String permissionString = null;

            while (it.hasNext()) {
                secPerm = (SecurityPermission) it.next();
                permissionString = secPerm.getPermission();
                int intIndex = policyContent.indexOf(permissionString);
                if (intIndex == -1) {
                    if (permissionString != null) {
                        if (policyString != null) {
                            policyString = policyString + "\n \n" + permissionString;
                        } else {
                            policyString = "\n\n" + permissionString;
                        }
                    }
                }
            }

            //print the missing permissions
            if (policyString != null) {
                policyString = CAUTION_MESSAGE + policyString;
            }
        }
        return policyString;
    }

    /**
     * Obtain the content of server.policy file
     *
     * @param file File server.policy file
     * @return String content of server.policy file
     */
    public String getFileContent(File file) {
        StringBuilder contents = new StringBuilder();
        BufferedReader input = null;
        try {
            input = new BufferedReader(new FileReader(file));
            try {
                String line = null;
                while ((line = input.readLine()) != null) {
                    contents.append(line);
                    contents.append(System.getProperty("line.separator"));
                }
            } finally {
                input.close();
            }
        }
        catch (Exception ex) {
            _logger.log(Level.WARNING, "Exception while performing resource-adapter's " +
                    "security permission check : ", ex);
        }
        return contents.toString();
    }

    /** Obtains all the Connection definition names of a rar
     *  @param rarName rar moduleName
     *  @return Array of connection definition names.
     */
    public String[] getConnectionDefinitionNames(String rarName)
               throws ConnectorRuntimeException
    {

        String[] result = new String[0];
        ConnectorDescriptor desc = getConnectorDescriptor(rarName);
        if(desc != null) {
            MCFConfigParser mcfConfigParser = (MCFConfigParser)
              ConnectorConfigParserFactory.getParser(ConnectorConfigParser.MCF);
            return mcfConfigParser.getConnectionDefinitionNames(desc);
        } else {
            return result;
        }
    }

    /**
     *  Retrieves the Resource adapter javabean properties with default values.
     *  The default values will the values present in the ra.xml. If the
     *  value is not present in ra.xxml, javabean is introspected to obtain
     *  the default value present, if any. If intrspection fails or null is the
     *  default value, empty string is returned.
     *  If ra.xml has only the property and no value, empty string is the value
     *  returned.
     *  @param rarName rar module name
     *  @return Resource adapter javabean properties with default values.
     *  @throws ConnectorRuntimeException if property retrieval fails.
     */
    public Properties getResourceAdapterConfigProps(String rarName)
                throws ConnectorRuntimeException
    {
        return getConnectorConfigJavaBeans(
               rarName,null,ConnectorConfigParser.RA);
    }

    /**
     *  Retrieves the MCF javabean properties with default values.
     *  The default values will the values present in the ra.xml. If the
     *  value is not present in ra.xxml, javabean is introspected to obtain
     *  the default value present, if any. If intrspection fails or null is the
     *  default value, empty string is returned.
     *  If ra.xml has only the property and no value, empty string is the value
     *  returned.
     *  @param rarName rar module name
     *  @return managed connection factory javabean properties with
     *          default values.
     *  @throws ConnectorRuntimeException if property retrieval fails.
     */
    public Properties getMCFConfigProps(
     String rarName,String connectionDefName) throws ConnectorRuntimeException
    {
        Properties props = getConnectorConfigJavaBeans(
               rarName,connectionDefName,ConnectorConfigParser.MCF);
/* TODO V3 handle JMS-RA later
	if (rarName.equals(ConnectorConstants.DEFAULT_JMS_ADAPTER)) {
            props.remove(ActiveJmsResourceAdapter.ADDRESSLIST);
	}
*/
        return props;
    }

    /**
     *  Retrieves the admin object javabean properties with default values.
     *  The default values will the values present in the ra.xml. If the
     *  value is not present in ra.xxml, javabean is introspected to obtain
     *  the default value present, if any. If intrspection fails or null is the
     *  default value, empty string is returned.
     *  If ra.xml has only the property and no value, empty string is the value
     *  returned.
     *  @param rarName rar module name
     *  @return admin object javabean properties with
     *          default values.
     *  @throws ConnectorRuntimeException if property retrieval fails.
     */
    public Properties getAdminObjectConfigProps(
      String rarName,String adminObjectIntf) throws ConnectorRuntimeException
    {
        return getConnectorConfigJavaBeans(
               rarName,adminObjectIntf,ConnectorConfigParser.AOR);
    }

    /**
     *  Retrieves the admin object javabean properties with default values.
     *  The default values will the values present in the ra.xml. If the
     *  value is not present in ra.xxml, javabean is introspected to obtain
     *  the default value present, if any. If intrspection fails or null is the
     *  default value, empty string is returned.
     *  If ra.xml has only the property and no value, empty string is the value
     *  returned.
     *  @param rarName rar module name
     *  @param adminObjectIntf admin object interface
     *  @param adminObjectClass admin object class
     *  @return admin object javabean properties with
     *          default values.
     *  @throws ConnectorRuntimeException if property retrieval fails.
     */
    public Properties getAdminObjectConfigProps(
      String rarName,String adminObjectIntf, String adminObjectClass) throws ConnectorRuntimeException
    {
        ConnectorDescriptor desc = getConnectorDescriptor(rarName);
        if(desc != null) {
            AdminObjectConfigParser adminObjectConfigParser =
                 (AdminObjectConfigParser)
                 ConnectorConfigParserFactory.getParser(
                 ConnectorConfigParser.AOR);
            return adminObjectConfigParser.getJavaBeanProps(
                       desc,adminObjectIntf, adminObjectClass, rarName);
        } else {
            return null;
        }
    }

    /**
     *  Retrieves the XXX javabean properties with default values.
     *  The javabean to introspect/retrieve is specified by the type.
     *  The default values will be the values present in the ra.xml. If the
     *  value is not present in ra.xxml, javabean is introspected to obtain
     *  the default value present, if any. If intrspection fails or null is the
     *  default value, empty string is returned.
     *  If ra.xml has only the property and no value, empty string is the value
     *  returned.
     *  @param rarName rar module name
     *  @return admin object javabean properties with
     *          default values.
     *  @throws ConnectorRuntimeException if property retrieval fails.
     */
    public Properties getConnectorConfigJavaBeans(String rarName,
        String connectionDefName,String type) throws ConnectorRuntimeException
    {

        ConnectorDescriptor desc = getConnectorDescriptor(rarName);
        if(desc != null) {
            ConnectorConfigParser ccParser =
                     ConnectorConfigParserFactory.getParser(type);
            return ccParser.getJavaBeanProps(desc,connectionDefName, rarName);
        } else {
            return null;
        }
    }

    /**
     * Return the ActivationSpecClass name for given rar and messageListenerType
     * @param moduleDir The directory where rar is exploded.
     * @param messageListenerType MessageListener type
     * @throws  ConnectorRuntimeException If moduleDir is null.
     *          If corresponding rar is not deployed.
     */
    public String getActivationSpecClass( String rarName,
             String messageListenerType) throws ConnectorRuntimeException
    {
        ConnectorDescriptor desc = getConnectorDescriptor(rarName);
        if(desc != null) {
            MessageListenerConfigParser messagelistenerConfigParser =
                 (MessageListenerConfigParser)
                 ConnectorConfigParserFactory.getParser(
                 ConnectorConfigParser.MSL);
            return messagelistenerConfigParser.getActivationSpecClass(
                       desc,messageListenerType);
        } else {
            return null;
        }
    }

    /* Parses the ra.xml and returns all the Message listener types.
     *
     * @param  rarName name of the rar module.
     * @return Array of message listener types as strings.
     * @throws  ConnectorRuntimeException If moduleDir is null.
     *          If corresponding rar is not deployed.
     *
     */
    public String[] getMessageListenerTypes(String rarName)
               throws ConnectorRuntimeException
    {
        ConnectorDescriptor desc = getConnectorDescriptor(rarName);
        if(desc != null) {
            MessageListenerConfigParser messagelistenerConfigParser =
                (MessageListenerConfigParser)
                ConnectorConfigParserFactory.getParser(
                ConnectorConfigParser.MSL);
            return messagelistenerConfigParser.getMessageListenerTypes(desc);
        } else {
            return null;
        }
    }

    /** Parses the ra.xml for the ActivationSpec javabean
     *  properties. The ActivationSpec to be parsed is
     *  identified by the moduleDir where ra.xml is present and the
     *  message listener type.
     *
     *  message listener type will be unique in a given ra.xml.
     *
     *  It throws ConnectorRuntimeException if either or both the
     *  parameters are null, if corresponding rar is not deployed,
     *  if message listener type mentioned as parameter is not found in ra.xml.
     *  If rar is deployed and message listener (type mentioned) is present
     *  but no properties are present for the corresponding message listener,
     *  null is returned.
     *
     *  @param  rarName name of the rar module.
     *  @param  messageListenerType message listener type.It is uniqie
     *          across all <messagelistener> sub-elements in <messageadapter>
     *          element in a given rar.
     *  @return Javabean properties with the property names and values
     *          of properties. The property values will be the values
     *          mentioned in ra.xml if present. Otherwise it will be the
     *          default values obtained by introspecting the javabean.
     *          In both the case if no value is present, empty String is
     *          returned as the value.
     *  @throws  ConnectorRuntimeException if either of the parameters are null.
     *           If corresponding rar is not deployed i.e moduleDir is invalid.
     *           If messagelistener type is not found in ra.xml
     */
    public Properties getMessageListenerConfigProps(String rarName,
         String messageListenerType)throws ConnectorRuntimeException
    {
        return getConnectorConfigJavaBeans(
               rarName,messageListenerType,ConnectorConfigParser.MSL);
    }

    /** Returns the Properties object consisting of propertyname as the
     *  key and datatype as the value.
     *  @param  rarName name of the rar module.
     *  @param  messageListenerType message listener type.It is uniqie
     *          across all <messagelistener> sub-elements in <messageadapter>
     *          element in a given rar.
     *  @return Properties object with the property names(key) and datatype
     *          of property(as value).
     *  @throws  ConnectorRuntimeException if either of the parameters are null.
     *           If corresponding rar is not deployed i.e moduleDir is invalid.
     *           If messagelistener type is not found in ra.xml
     */
    public Properties getMessageListenerConfigPropTypes(String rarName,
               String messageListenerType) throws ConnectorRuntimeException
    {
        ConnectorDescriptor desc = getConnectorDescriptor(rarName);
        if(desc != null) {
            MessageListenerConfigParser messagelistenerConfigParser =
                (MessageListenerConfigParser)
                ConnectorConfigParserFactory.getParser(
                ConnectorConfigParser.MSL);
            return messagelistenerConfigParser.getJavaBeanReturnTypes(
                    desc, messageListenerType, rarName);
        } else {
            return null;
        }
    }

    /** Obtains all the Connection definition names of a rar
     *  @param rarName rar moduleName
     *  @return Array of connection definition names.
     */
    public String[] getAdminObjectInterfaceNames(String rarName)
               throws ConnectorRuntimeException
    {

        ConnectorDescriptor desc = getConnectorDescriptor(rarName);
        if(desc != null) {
            AdminObjectConfigParser adminObjectConfigParser =
                 (AdminObjectConfigParser)
                 ConnectorConfigParserFactory.getParser(
                 ConnectorConfigParser.AOR);
            return adminObjectConfigParser.getAdminObjectInterfaceNames(desc);
        } else {
            return null;
        }
    }

    /**
     * gets the adminObjectClassNames pertaining to a rar & a specific
     * adminObjectInterfaceName
     *
     * @param rarName resource-adapter name
     * @param intfName admin-object-interface name
     * @return Array of AdminObjectInterface names as Strings
     * @throws ConnectorRuntimeException if parsing fails
     */
    public String[] getAdminObjectClassNames(String rarName, String intfName)
            throws ConnectorRuntimeException {
        ConnectorDescriptor desc = getConnectorDescriptor(rarName);
        if(desc != null) {
            AdminObjectConfigParser adminObjectConfigParser =
                 (AdminObjectConfigParser)
                 ConnectorConfigParserFactory.getParser(
                 ConnectorConfigParser.AOR);
            return adminObjectConfigParser.getAdminObjectClassNames(desc, intfName);
        } else {
            return null;
        }
    }

    /**
     * checks whether the specified intfName, className has presence in
     * admin objects of the RAR
     * @param rarName resource-adapter name
     * @param intfName admin object interface name
     * @param className admin object class name
     * @return boolean indicating the presence of admin object
     * @throws ConnectorRuntimeException when unable to determine the presence
     */
    public boolean hasAdminObject(String rarName, String intfName, String className)
        throws ConnectorRuntimeException{
        ConnectorDescriptor desc = getConnectorDescriptor(rarName);
        if(desc != null) {
            AdminObjectConfigParser adminObjectConfigParser =
                 (AdminObjectConfigParser)
                 ConnectorConfigParserFactory.getParser(
                 ConnectorConfigParser.AOR);
            return adminObjectConfigParser.hasAdminObject(desc, intfName, className);
        } else {
            return false;
        }
    }

    /**
     * Finds the properties of a RA JavaBean bundled in a RAR
     * without exploding the RAR
     *
     * @param pathToDeployableUnit a physical,accessible location of the connector module.
     * [either a RAR for RAR-based deployments or a directory for Directory based deployments]
     * @return A Map that is of <String RAJavaBeanPropertyName, String defaultPropertyValue>
     * An empty map is returned in the case of a 1.0 RAR
     */
/*   TODO V3
    public Map getRABeanProperties(String pathToDeployableUnit) throws ConnectorRuntimeException{
        return RARUtils.getRABeanProperties(pathToDeployableUnit);
    }
*/
}
