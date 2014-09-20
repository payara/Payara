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
import com.sun.appserv.connectors.internal.api.ConnectorRuntimeException;

import java.util.logging.*;
import java.util.*;
import java.lang.*;

/**
 *  This is message listener configuration parser. It parses the 
 *  ra.xml file for the message listener specific configurations 
 *  like activationSpec javabean  properties, message listener types .
 *
 *  @author      Srikanth P
 *
 */

public class MessageListenerConfigParserImpl implements 
                               MessageListenerConfigParser {

    private final static Logger _logger = LogDomains.getLogger(MessageListenerConfigParserImpl.class, LogDomains.RSR_LOGGER);
   
    /**
     *  Default constructor.
     *
     */

    public MessageListenerConfigParserImpl() {

    }

    /**
     * Return the ActivationSpecClass name for given rar and messageListenerType
     * @param desc ConnectorDescriptor pertaining to rar.
     * @param messageListenerType MessageListener type
     * @throws  ConnectorRuntimeException If moduleDir is null.
     *          If corresponding rar is not deployed. 
     */

    public String getActivationSpecClass( ConnectorDescriptor desc, 
             String messageListenerType) throws ConnectorRuntimeException
    {
        if(desc == null) {
            throw new ConnectorRuntimeException("Invalid arguments");
        }

        MessageListener messageListeners[] = 
               ddTransformUtil.getMessageListeners(desc);

        if(messageListeners != null) {
            for(int i=0;i<messageListeners.length;++i) {
                if(messageListenerType.equals( 
                           messageListeners[i].getMessageListenerType())){
                    return messageListeners[i].getActivationSpecClass();
                }
            }
        }
        return null; 
    }

    /* Parses the ra.xml and returns all the Message listener types. 
     *
     * @param desc ConnectorDescriptor pertaining to rar.
     * @return Array of message listener types as strings.
     * @throws  ConnectorRuntimeException If moduleDir is null.
     *          If corresponding rar is not deployed. 
     *
     */

    public String[] getMessageListenerTypes(ConnectorDescriptor desc)
               throws ConnectorRuntimeException 
    {

        if(desc == null) {
            throw new ConnectorRuntimeException("Invalid arguments");
        }

        MessageListener messageListeners[] = 
               ddTransformUtil.getMessageListeners(desc);

        String[] messageListenerTypes = null;
        if(messageListeners != null) {
            messageListenerTypes = new String[messageListeners.length];
            for(int i=0;i<messageListeners.length;++i) {
                messageListenerTypes[i] = 
                           messageListeners[i].getMessageListenerType();
            }
        }
        return messageListenerTypes;
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
     *  @param desc ConnectorDescriptor pertaining to rar.
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

    public Properties getJavaBeanProps(ConnectorDescriptor desc, 
               String messageListenerType, String rarName) throws ConnectorRuntimeException 
    {

        MessageListener messageListener = getMessageListener(desc, messageListenerType);

        /* ddVals           -> Properties present in ra.xml
        *  introspectedVals -> All properties with values
        *                      obtained by introspection of resource
        *                      adapter javabean
        *  mergedVals       -> merged props of raConfigPros and
        *                      allraConfigPropsWithDefVals
        */

        Properties mergedVals = null;
        Set ddVals = messageListener.getConfigProperties();
        String className = messageListener.getActivationSpecClass();
        if(className != null && className.length() != 0) {
            Properties introspectedVals = configParserUtil.introspectJavaBean(
                               className,ddVals, false, rarName);
            mergedVals = configParserUtil.mergeProps(ddVals,introspectedVals);
        }
        return mergedVals;
    }

    private MessageListener getMessageListener(ConnectorDescriptor desc, String messageListenerType)
            throws ConnectorRuntimeException {
        if(desc == null || messageListenerType == null) {
            throw new ConnectorRuntimeException("Invalid arguments");
        }

        MessageListener allMessageListeners[] =
               ddTransformUtil.getMessageListeners(desc);

        MessageListener messageListener = null;
        for(int i=0;i<allMessageListeners.length;++i) {
            if(messageListenerType.equals(
                    allMessageListeners[i].getMessageListenerType())) {
                messageListener = allMessageListeners[i];
            }
        }

        if(messageListener == null) {
            if (_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE,
                        "No such MessageListener found in ra.xml",
                        messageListenerType);
            }
            throw new ConnectorRuntimeException(
                  "No such MessageListener found in ra.xml : " +
                  messageListenerType);
        }
        return messageListener;
    }

    public List<String> getConfidentialProperties(ConnectorDescriptor desc, String rarName, String... keyFields)
            throws ConnectorRuntimeException {
        if(keyFields == null || keyFields.length == 0 || keyFields[0] == null){
            throw new ConnectorRuntimeException("MessageListenerType must be specified");
        }
        MessageListener messageListener = getMessageListener(desc, keyFields[0]);
        List<String> confidentialProperties = new ArrayList<String>();
        Set configProperties = messageListener.getConfigProperties();
        if(configProperties != null){
            Iterator iterator = configProperties.iterator();
            while(iterator.hasNext()){
                ConnectorConfigProperty ccp = (ConnectorConfigProperty)iterator.next();
                if(ccp.isConfidential()){
                    confidentialProperties.add(ccp.getName());
                }
            }
        }
        return confidentialProperties;
    }

    /** Returns the Properties object consisting of propertyname as the
     *  key and datatype as the value.
     *  @param  messageListenerType message listener type.It is uniqie
     *          across all <messagelistener> sub-elements in <messageadapter> 
     *          element in a given rar.
     *  @return Properties object with the property names(key) and datatype
     *          of property(as value). 
     *  @throws  ConnectorRuntimeException if either of the parameters are null.
     *           If corresponding rar is not deployed i.e moduleDir is invalid. 
     *           If messagelistener type is not found in ra.xml
     */
    public Properties getJavaBeanReturnTypes(ConnectorDescriptor desc, 
               String messageListenerType, String rarName) throws ConnectorRuntimeException
    {

        MessageListener messageListener = getMessageListener(desc, messageListenerType);

        /* ddVals           -> Properties present in ra.xml
        *  introspectedVals -> All properties with values
        *                      obtained by introspection of resource
        *                      adapter javabean
        *  mergedVals       -> merged props of raConfigPros and
        *                      allraConfigPropsWithDefVals
        */

        Properties mergedVals = null;
        Set ddVals = messageListener.getConfigProperties();
        String className = messageListener.getActivationSpecClass();
        if(className != null && className.length() != 0) {
            Properties introspectedVals =
               configParserUtil.introspectJavaBeanReturnTypes(className,ddVals, rarName);
            mergedVals = configParserUtil.mergePropsReturnTypes(
                                              ddVals,introspectedVals);
        }
        return mergedVals;
    }
}
