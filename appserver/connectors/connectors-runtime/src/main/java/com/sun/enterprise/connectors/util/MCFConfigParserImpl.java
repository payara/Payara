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
import com.sun.logging.LogDomains;
import com.sun.appserv.connectors.internal.api.ConnectorRuntimeException;

import java.util.logging.*;
import java.util.*;
import java.lang.*;

/**
 *  This is managed connection factory configuration parser. It parses the 
 *  ra.xml file for the managed connection factory specific configurations 
 *  like managed connection factory javabean  properties .
 *
 *  @author      Srikanth P
 *
 */

public class MCFConfigParserImpl implements MCFConfigParser {

    private final static Logger _logger = LogDomains.getLogger(MCFConfigParserImpl.class, LogDomains.RSR_LOGGER);
   
    /**
     *  Default constructor.
     *
     */

    public MCFConfigParserImpl() {

    }

    /* Parses the ra.xml and returns all the connection definition names. 
     * Since there is no specific connection definition attribute in the 
     * <connection-definition element>, connection factory interface is 
     * taken as the connection definition name. 
     *
     * @param desc ConnectorDescriptor pertaining to rar.
     * @return Array of Connection definition names.
     * @throws  ConnectorRuntimeException If moduleDir is null.
     *          If corresponding rar is not deployed. 
     *
     */

    public String[] getConnectionDefinitionNames(ConnectorDescriptor desc)
               throws ConnectorRuntimeException
    {

        if(desc == null) {
            throw new ConnectorRuntimeException("Invalid arguments");
        }

        ConnectionDefDescriptor cdd[] = ddTransformUtil.getConnectionDefs(desc);

        String[] connDefNames = new String[0];
        if(cdd != null) {
            connDefNames = new String[cdd.length];
            for(int i=0;i<cdd.length;++i) {
                connDefNames[i] = cdd[i].getConnectionFactoryIntf();
            }
        }
        return connDefNames;
    }

    /** Parses the ra.xml for the managed connection factory javabean 
     *  properties. The managed connection factory to be parsed is 
     *  identified by the moduleDir where ra.xml is present and the 
     *  connection definition name .
     *
     *  Connection definition name  will be unique in a given ra.xml.
     *
     *  It throws ConnectorRuntimeException if either or both the
     *  parameters are null, if corresponding rar is not deployed,
     *  if no connection definition name is found in ra.xml. If rar is deployed
     *  and connection definition name is present but no properties are
     *  present for the corresponding connection definition name, 
     *  null is returned.
     *
     *  @param  desc ConnectorDescriptor pertaining to rar.
     *  @param  connectionDefName connection definition name which is unique
     *          across all the <connection-definition> elements in a given rar.
     *  @return Javabean properties with the propety names and values
     *          of propeties. The property values will be the values
     *          mentioned in ra.xml if present. Otherwise it will be the
     *          default values obtained by introspecting the javabean.
     *          In both the case if no value is present, empty String is
     *          returned as the value.
     *  @throws  ConnectorRuntimeException if either of the parameters are null.
     *           If corresponding rar is not deployed i.e moduleDir is invalid. 
     *           If no connection definition name is found in ra.xml
     */

    public Properties getJavaBeanProps(ConnectorDescriptor desc, 
               String connectionDefName, String rarName) throws ConnectorRuntimeException 
    {

        ConnectionDefDescriptor cdd = getConnectionDefinition(desc, connectionDefName);
        if (cdd == null) return null;

        Properties mergedVals = null;
        Set ddVals = cdd.getConfigProperties();
        String className = cdd.getManagedConnectionFactoryImpl();
        if(className != null && className.length() != 0) {
            Properties introspectedVals = configParserUtil.introspectJavaBean(
                               className,ddVals, true, rarName);
            mergedVals = configParserUtil.mergeProps(ddVals,introspectedVals);
        }
        return mergedVals;
    }

    private ConnectionDefDescriptor getConnectionDefinition(ConnectorDescriptor desc, String connectionDefName)
            throws ConnectorRuntimeException {
        if(desc == null || connectionDefName == null) {
            throw new ConnectorRuntimeException("Invalid arguments");
        }
        OutboundResourceAdapter ora =
                      desc.getOutboundResourceAdapter();
        if(ora == null || ora.getConnectionDefs().size() == 0) {
            return null;
        }
        Set connectionDefs = ora.getConnectionDefs();
        if(connectionDefs== null || connectionDefs.size() == 0) {
            return null;
        }
        Iterator iter = connectionDefs.iterator();
        ConnectionDefDescriptor cdd = null;
        boolean connectionDefFound=false;
        while(iter.hasNext()) {
            cdd = (ConnectionDefDescriptor)iter.next();
            if(connectionDefName.equals(cdd.getConnectionFactoryIntf())) {
                connectionDefFound=true;
                break;
            }
        }

        if(!connectionDefFound) {
            if (_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE,
                        "No such connectiondefinition found in ra.xml",
                        connectionDefName);
            }
            throw new ConnectorRuntimeException(
                  "No such connectiondefinition found in ra.xml : " +
                  connectionDefName);
        }

        /* ddVals           -> Properties present in ra.xml
        *  introspectedVals -> All properties with values
        *                      obtained by introspection of resource
        *                      adapter javabean
        *  mergedVals       -> merged props of raConfigPros and
        *                      allraConfigPropsWithDefVals
        */
        return cdd;
    }


    public List<String> getConfidentialProperties(ConnectorDescriptor desc, String rarName, String... keyFields)
            throws ConnectorRuntimeException {
        if(keyFields == null || keyFields.length == 0 || keyFields[0] == null){
            throw new ConnectorRuntimeException("ConnectionDefinitionName must be specified");
        }
        ConnectionDefDescriptor cdd = getConnectionDefinition(desc, keyFields[0]);
        List<String> confidentialProperties = new ArrayList<String>();
        if(cdd != null){
            Set configProperties = cdd.getConfigProperties();
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
