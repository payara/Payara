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

import java.util.Properties;

/** Interface class of admin object interface parser methods. 
 *  @author Srikanth P
 */
public interface AdminObjectConfigParser extends ConnectorConfigParser {

    /**
     *  Obtains the admin object intercface names of a given rar.
     *  @param desc ConnectorDescriptor pertaining to rar.
     *  @return Array of admin object interface names as strings
     *  @throws ConnectorRuntimeException If rar is not exploded or 
     *                                    incorrect ra.xml 
     */
    public String[] getAdminObjectInterfaceNames(ConnectorDescriptor desc)
                      throws ConnectorRuntimeException;

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
            throws ConnectorRuntimeException ;

    /**
     *  Checks whether the provided interfacename and classname combination
     *  is present in any of the admin objects for the resource-adapter
     *  @param desc ConnectorDescriptor pertaining to rar.
     *  @param intfName interface-name
     *  @param className class-name
     *  @return boolean indicating the presence of adminobject
     *  @throws ConnectorRuntimeException If rar is not exploded or
     *                                    incorrect ra.xml
     */
    public boolean hasAdminObject(ConnectorDescriptor desc, String intfName, String className)
        throws ConnectorRuntimeException;

    /**
     * Obtains the merged javabean properties (properties present in ra.xml
     * and introspected properties) of a specific configuration.
     *
     * @param desc              ConnectorDescriptor pertaining to rar .
     * @param adminObjectIntfName admin object interface .
     * @param adminObjectClassName admin object classname
     * @param rarName resource-adapter-name
     * @return Merged properties.
     *  @throws ConnectorRuntimeException If rar is not exploded or
     *                                    incorrect ra.xml
     */
    Properties getJavaBeanProps(ConnectorDescriptor desc,
                                String adminObjectIntfName, String adminObjectClassName,
                                String rarName) throws ConnectorRuntimeException;

}

