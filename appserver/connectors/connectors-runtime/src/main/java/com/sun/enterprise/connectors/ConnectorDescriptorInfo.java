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

package com.sun.enterprise.connectors;

import com.sun.enterprise.connectors.util.ConnectionPoolReconfigHelper.ReconfigAction;
import com.sun.enterprise.deployment.ConnectorConfigProperty;

import java.io.Serializable;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * This class abstract the ra.xml values pertaining to the connection
 * management. It contains various config properties of MCF, Resource adapter,
 * Connection and also their respective classes and interfaces.
 *
 * @author Srikanth P
 */

public final class ConnectorDescriptorInfo implements Serializable {

    private String rarName_;
    private String resourceAdapterClass_;
    private String connectionDefinitionName_;
    private String managedConnectionFactoryClass_;
    private String connectionFactoryClass_;
    private String connectionFactoryInterface_;
    private String connectionClass_;
    private String connectionInterface_;
    private Set<ConnectorConfigProperty> mcfConfigProperties_;
    private Set<ConnectorConfigProperty > resourceAdapterConfigProperties_;

    /**
     * Default constructor
     */
    public ConnectorDescriptorInfo() {

        mcfConfigProperties_ = new LinkedHashSet<ConnectorConfigProperty >();
        resourceAdapterConfigProperties_ = new LinkedHashSet<ConnectorConfigProperty >();
    }

    /**
     * Clone method
     *
     * @return ConnectorDescriptorInfo instance
     */

    public ConnectorDescriptorInfo doClone() {

        ConnectorDescriptorInfo cdi = new ConnectorDescriptorInfo();
        cdi.setMCFConfigProperties(mcfConfigProperties_);
        cdi.setResourceAdapterConfigProperties(
                resourceAdapterConfigProperties_);
        cdi.setRarName(rarName_);
        cdi.setResourceAdapterClassName(resourceAdapterClass_);
        cdi.setConnectionDefinitionName(connectionDefinitionName_);
        cdi.setManagedConnectionFactoryClass(managedConnectionFactoryClass_);
        cdi.setConnectionFactoryClass(connectionFactoryClass_);
        cdi.setConnectionFactoryInterface(connectionFactoryInterface_);
        cdi.setConnectionClass(connectionClass_);
        cdi.setConnectionInterface(connectionInterface_);
        return cdi;
    }

    /**
     * Constructor
     *
     * @param mcfConfigProperties Array of MCF config properties
     * @param resourceAdapterConfigProperties
     *                            Array of  Resource adapter config props
     */

    public ConnectorDescriptorInfo(
            ConnectorConfigProperty [] mcfConfigProperties,
            ConnectorConfigProperty [] resourceAdapterConfigProperties) {
        this();
        if (mcfConfigProperties != null) {
            for (ConnectorConfigProperty  mcfConfigProperty : mcfConfigProperties) {
                mcfConfigProperties_.add(mcfConfigProperty);
            }

            if (resourceAdapterConfigProperties != null) {
                for (ConnectorConfigProperty  mcfConfigProperty : mcfConfigProperties) {
                    resourceAdapterConfigProperties_.add(mcfConfigProperty);
                }
            }
        }
    }

    /**
     * Adds an MCF config property to the existing array/Set of MCF config
     * properties.
     *
     * @param configProperty Config property to be added.
     */
    public void addMCFConfigProperty(ConnectorConfigProperty  configProperty) {
        if (configProperty != null) {
            mcfConfigProperties_.add(configProperty);
        }
    }

    /**
     * Removes an config  property from the existing array/Set of MCF config
     * properties
     *
     * @param configProperty Config property to be removed.
     */
    public void removeMCFConfigProperty(ConnectorConfigProperty  configProperty) {
        if (configProperty != null) {
            mcfConfigProperties_.remove(configProperty);
        }
    }

    /**
     * Setter method for MCFConfigProperties property.
     *
     * @param configProperties Set MCF config properties
     */
    public void setMCFConfigProperties(Set configProperties) {
        mcfConfigProperties_ = configProperties;
    }

    /**
     * Setter method for MCFConfigProperties property.
     *
     * @param configProperties Array of MCF config properties
     */

    public void setMCFConfigProperties(ConnectorConfigProperty [] configProperties) {
        if (configProperties != null) {
            for (int i = 0; i < configProperties.length; ++i) {
                mcfConfigProperties_.add(configProperties[i]);
            }
        }
    }

    /**
     * Getter method for MCFConfigProperties property
     *
     * @return Set of managed connection factory config properties
     */

    public Set getMCFConfigProperties() {
        return mcfConfigProperties_;
    }

    /**
     * Adds a Resource Adapter config property to the existing array/Set
     * of Resource Adapter config properties.
     *
     * @param configProperty Config property to be added.
     */

    public void addResourceAdapterConfigProperty(ConnectorConfigProperty  configProperty) {
        if (configProperty != null) {
            resourceAdapterConfigProperties_.add(configProperty);
        }
    }

    /**
     * Removes a Resource Adapter config property to the existing array/Set
     * of Resource Adapter config properties.
     *
     * @param configProperty Config property to be removed.
     */
    public void removeResourceAdapterConfigProperty(ConnectorConfigProperty  configProperty) {
        if (configProperty != null) {
            resourceAdapterConfigProperties_.remove(configProperty);
        }
    }

    /**
     * Setter method for ResourceAdapterConfigProperties property.
     *
     * @param configProperties Set ResourceAdapter config properties
     */
    public void setResourceAdapterConfigProperties(Set configProperties) {
        resourceAdapterConfigProperties_ = configProperties;
    }

    /**
     * Setter method for ResourceAdapterConfigProperties property.
     *
     * @param configProperties Array ResourceAdapter config properties
     */

    public void setResourceAdapterConfigProperties(
            ConnectorConfigProperty [] configProperties) {
        if (configProperties != null) {
            for (ConnectorConfigProperty  configProperty : configProperties) {
                resourceAdapterConfigProperties_.add(configProperty);
            }
        }
    }

    /**
     * Getter method for ResourceAdapterConfigProperties property
     *
     * @return Set of resource adapter config properties
     */

    public Set getResourceAdapterConfigProperties() {
        return resourceAdapterConfigProperties_;
    }

    /**
     * Getter method for RarName property
     *
     * @return rarName
     */

    public String getRarName() {
        return rarName_;
    }

    /**
     * Setter method for RarName property
     *
     * @param rarName rar name
     */

    public void setRarName(String rarName) {
        rarName_ = rarName;
    }

    /**
     * Getter method for ResourceAdapterClassName property
     *
     * @return Resource adapter class name
     */

    public String getResourceAdapterClassName() {
        return resourceAdapterClass_;
    }

    /**
     * Setter method for ResourceAdapterClassName property
     *
     * @param resourceAdapterClass Resource adapter class name
     */

    public void setResourceAdapterClassName(String resourceAdapterClass) {
        resourceAdapterClass_ = resourceAdapterClass;
    }

    /**
     * Getter method for ConnectionDefinitionName property
     *
     * @return connection definition name
     */

    public String getConnectionDefinitionName() {
        return connectionDefinitionName_;
    }

    /**
     * Setter method for ConnectionDefinitionName property
     *
     * @param connectionDefinitionName connection definition name
     */

    public void setConnectionDefinitionName(String connectionDefinitionName) {
        connectionDefinitionName_ = connectionDefinitionName;
    }

    /**
     * Getter method for ManagedConnectionFactoryClass property
     *
     * @return managed connection factory class
     */

    public String getManagedConnectionFactoryClass() {
        return managedConnectionFactoryClass_;
    }

    /**
     * Setter method for ManagedConnectionFactoryClass property
     *
     * @param managedConnectionFactoryClass managed connection factory class
     */

    public void setManagedConnectionFactoryClass(
            String managedConnectionFactoryClass) {
        managedConnectionFactoryClass_ = managedConnectionFactoryClass;
    }

    /**
     * Getter method for ConnectionFactoryClass property
     *
     * @return connection factory class
     */

    public String getConnectionFactoryClass() {
        return connectionFactoryClass_;
    }

    /**
     * Setter method for ConnectionFactoryClass property
     *
     * @param connectionFactoryClass connection factory class
     */

    public void setConnectionFactoryClass(String connectionFactoryClass) {
        connectionFactoryClass_ = connectionFactoryClass;
    }

    /**
     * Getter method for ConnectionFactoryInterface property
     *
     * @return connection factory interface class
     */

    public String getConnectionFactoryInterface() {
        return connectionFactoryInterface_;
    }

    /**
     * Setter method for ConnectionFactoryInterface property
     *
     * @param connectionFactoryInterface connection factory interface class
     */

    public void setConnectionFactoryInterface(
            String connectionFactoryInterface) {
        connectionFactoryInterface_ = connectionFactoryInterface;
    }

    /**
     * Getter method for ConnectionClass property
     *
     * @return connection class
     */

    public String getConnectionClass() {
        return connectionClass_;
    }

    /**
     * Setter method for ConnectionClass property
     *
     * @param connectionClass connection Class
     */

    public void setConnectionClass(String connectionClass) {
        connectionClass_ = connectionClass;
    }

    /**
     * Getter method for ConnectionInterface property
     *
     * @return connectionInterface class
     */

    public String getConnectionInterface() {
        return connectionInterface_;
    }

    /**
     * Setter method for ConnectionInterface property
     *
     * @param connectionInterface connection interface class
     */

    public void setConnectionInterface(String connectionInterface) {
        connectionInterface_ = connectionInterface;
    }

    /**
     * Compare the MCF Config properties in this object with the
     * passed ones
     *
     * @param cdi - The ConnDescInfo object whose MCF config props are to
     *            to be comapred against our props
     * @return true - if the config properties are the same
     *         false otherwise
     */

    public ReconfigAction compareMCFConfigProperties(ConnectorDescriptorInfo cdi) {
        return compareMCFConfigProperties(cdi, new HashSet());
    }

    /**
     * Compare the MCF Config properties in this object with the
     * passed ones. The properties in the Set of excluded properties
     * are not compared against
     *
     * @param cdi      - The ConnDescInfo object whose MCF config props are to
     *                 to be comapred against our props
     * @param excluded - list of properties to be excluded from comparison
     * @return true - if the config properties are the same
     *         false otherwise
     */

    public ReconfigAction compareMCFConfigProperties(
            ConnectorDescriptorInfo cdi, Set excluded) {
        Set mcfConfigProps = cdi.getMCFConfigProperties();

        if (mcfConfigProps.size() != mcfConfigProperties_.size()) {
            //return false;
            //Cannot determine anything due to size disparity - assume restart
            return ReconfigAction.RECREATE_POOL;
        }

        boolean same = false;

        for (Object mcfConfigProp : mcfConfigProps) {
            ConnectorConfigProperty  prop = (ConnectorConfigProperty ) mcfConfigProp;
            //see if this property is in our list of excludes
            if (excluded.contains(prop.getName())) {
                //_logger.finest("mcfProp ignored : " + prop.getName() );
                continue;
            }

            for (ConnectorConfigProperty  property : mcfConfigProperties_) {
                if (isEnvPropEqual(prop, property)) {
                    //we have a match
                    same = true;
                    //_logger.finest("mcfprop matched : " + prop.getName());
                    break;
                }
            }
            if (!same) {
                //_logger.finest("mcfprop not matched : " + prop.getName() );
                //return false;
                return ReconfigAction.RECREATE_POOL;
            }
            same = false;
        }

        return ReconfigAction.NO_OP;
    }

    /**
     * The ConnectorConfigProperty ::equals method only checks for name equality
     * So we need to write a custom equals
     *
     * @param e1 property
     * @param e2 property
     * @return boolean - equality result
     */
    private boolean isEnvPropEqual(ConnectorConfigProperty  e1,
                                   ConnectorConfigProperty  e2) {
        if (e1 != null && e2 != null &&
                e1.getName() != null && e2.getName() != null &&
                e1.getName().equals(e2.getName())) {
            if (e1.getValue() != null && e2.getValue() != null &&
                    e1.getValue().equals(e2.getValue())) {
                return true;
            }
        }
        return false;
    }
}
