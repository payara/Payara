/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

/*
 * Class.java
 *
 * Created on November 11, 2003, 1:45 PM
 */

package org.glassfish.config.support;

import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.config.serverbeans.Cluster;
import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.SystemProperty;

import java.util.List;

/**
 * Utility for getting the value of a system-property of an instance, particularly 
 * for an instance that is not the current running instance.  The current running 
 * instance automatically has tokens in the config resolved.  The value returned is
 * the value of the system property which has the highest precedence.  
 * The system-property defined at higher precedence levels
 * overrides system-property defined at lower precedence levels.  
 * The order of precedence from highest to lowest is
 * 1. server
 * 2. cluster
 * 3. config
 * 4. domain
 *
 * @author  kebbs
 * @author  Jennifer Chou
 */
public class PropertyResolver {

    private Domain _domain = null;
    private Cluster _cluster = null;
    private Server _server = null;
    private Config _config = null;
    
    public PropertyResolver(Domain domain, String instanceName) {
        _domain = domain;
        _server = _domain.getServerNamed(instanceName);
        if (_server != null) {
            _config = _domain.getConfigNamed(_server.getConfigRef());
        } else {
            _config = _domain.getConfigNamed(instanceName);
        }
        _cluster = _domain.getClusterForInstance(instanceName);
    }
    
    /**
     * Given a propery name, return its corresponding value in the specified 
     * SystemProperty array. Return null if the property is not found.
     */
    private String getPropertyValue(String propName, List<SystemProperty> props) {
        String propVal = null;
        for (SystemProperty prop : props) {
            if (prop.getName().equals(propName)) {
                return prop.getValue();
            }
        }
        return propVal;
    }
    
    /**
     * Given a property name, return its corresponding value as defined in
     * the domain, configuration, cluster, or server element. Return null if the property
     * is not found. Property values at the server override those at the configuration
     * which override those at the domain level.
     * Does not check if the property is available in java.lang.System.
     * This restriction is to prevent incorrect values being returned when trying
     * to retrieve properties for instances other than the currently running server (such as DAS).
     * In this case, we don't want to incorrectly return the DAS java.lang.System property.
     */
    public String getPropertyValue(String propName) {
        if (propName.startsWith("${") && propName.endsWith("}")) {
            propName = propName.substring(2, propName.lastIndexOf("}"));
        }
        String propVal = null;
        //First look for a server instance property matching the propName
        if (_server != null) {
            propVal = getPropertyValue(propName, _server.getSystemProperty());
        }
        if (propVal == null) {
            if (_cluster != null) {
                //If not found in the server instance, look for the propName in the 
                //cluster
                propVal = getPropertyValue(propName, _cluster.getSystemProperty());
            }            
            if (propVal == null) {
                if (_config != null) {             
                    //If not found in the server instance or cluster, look for the 
                    //propName in the config
                    propVal = getPropertyValue(propName, _config.getSystemProperty());
                    if (propVal == null) {
                        if (_domain != null) {
                            //Finally if the property is not found in the server, cluster,
                            //or configuration, look for the propName in the domain
                            propVal = getPropertyValue(propName, _domain.getSystemProperty());
                        }
                    }
                }
            }
        }
        
        return propVal;
    }
}
