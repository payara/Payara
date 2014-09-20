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

package com.sun.enterprise.connectors.util;

import com.sun.enterprise.deployment.*;
import com.sun.appserv.connectors.internal.api.ConnectorRuntimeException;

import java.util.*;

/** Interface class of managed connection factory parser methods. 
 *  @author Srikanth P
 */
public interface MessageListenerConfigParser extends ConnectorConfigParser {


    /**
     *  Obtains the Message Listener types of a given rar.
     *  @param desc ConnectorDescriptor pertaining to rar.
     *  @return Array of MessageListener types as strings
     *  @throws ConnectorRuntimeException If rar is not exploded or 
     *                                    incorrect ra.xml 
     */
    public String[] getMessageListenerTypes(ConnectorDescriptor desc)
                      throws ConnectorRuntimeException;

    /**
     *  Returns the ActivationSpecClass name for the given rar and message 
     *  listener type. 
     *  @param desc ConnectorDescriptor pertaining to rar.
     *  @param messageListenerType MessageListener type
     *  @throws ConnectorRuntimeException If rar is not exploded or
     *                                    incorrect ra.xml
     */
    public String getActivationSpecClass(ConnectorDescriptor desc,
          String messageListenerType) throws ConnectorRuntimeException;

    /** 
     * Returns the Properties object consisting of PropertyName as the key 
     * and the datatype as the value
     *  @param desc ConnectorDescriptor pertaining to rar.
     *  @param  messageListenerType message listener type.It is uniqie
     *          across all <messagelistener> sub-elements in <messageadapter>
     *          element in a given rar.
     *  @return properties object with the property names(key) and datatype
     *          of property(as value).
     *  @throws  ConnectorRuntimeException if either of the parameters are null.
     *           If corresponding rar is not deployed i.e moduleDir is invalid.
     *           If messagelistener type is not found in ra.xml
     */
    public Properties getJavaBeanReturnTypes(ConnectorDescriptor desc,
          String messageListenerType, String rarName) throws ConnectorRuntimeException;
}
