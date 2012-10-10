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

package com.sun.enterprise.config.serverbeans;

import java.beans.PropertyVetoException;
import java.util.List;

import org.glassfish.api.admin.config.PropertiesDesc;
import org.jvnet.hk2.config.types.Property;
import org.jvnet.hk2.config.types.PropertyBag;
import org.glassfish.quality.ToDo;
import org.jvnet.hk2.config.*;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

/* @XmlType(name = "", propOrder = {
    "jmxConnector",
    "dasConfig",
    "property"
}) */
/**
 * Admin Service exists in every instance. It is the configuration for either
 * a normal server, DAS or PE instance
 */

@Configured
public interface AdminService extends ConfigBeanProxy, PropertyBag {

    /**
     * Gets the value of the type property.
     * An instance can either be of type
     * das
         Domain Administration Server in SE/EE or the PE instance
     * das-and-server
     *   same as das
     * server
     *   Any non-DAS instance in SE/EE. Not valid for PE.
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="server")
    @Pattern(regexp="(das|das-and-server|server)")
    String getType();

    /**
     * Sets the value of the type property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setType(String value) throws PropertyVetoException;

    /**
     * Gets the value of the systemJmxConnectorName property.
     * The name of the internal jmx connector
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute
    String getSystemJmxConnectorName();

    /**
     * Sets the value of the systemJmxConnectorName property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setSystemJmxConnectorName(String value) throws PropertyVetoException;

    /**
     * Gets the value of the jmxConnector property.
     * The jmx-connector element defines the configuration of a JSR 160
     * compliant remote JMX Connector.
     * Objects of the following type(s) are allowed in the list
     * {@link JmxConnector }
     */
    @Element("jmx-connector")
    List<JmxConnector> getJmxConnector();

    /**
     * Gets the value of the dasConfig property.
     *
     * @return possible object is
     *         {@link DasConfig }
     */
    @Element("das-config")
    @NotNull
    DasConfig getDasConfig();

    /**
     * Sets the value of the dasConfig property.
     *
     * @param value allowed object is
     *              {@link DasConfig }
     */
    void setDasConfig(DasConfig value) throws PropertyVetoException;
    
    /**
    	Properties as per {@link org.jvnet.hk2.config.types.PropertyBag}
     */
    @ToDo(priority=ToDo.Priority.IMPORTANT, details="Provide PropertyDesc for legal props" )
    @PropertiesDesc(props={})
    @Element
    List<Property> getProperty();


    /** Gets the name of the auth realm to be used for administration. This obsoletes/deprecates the similarly named
     *  attribute on JmxConnector. Note that this is of essence where admin access is done outside the containers.
     *  Container managed security is still applicable and is handled via security annotations and deployment
     *  descriptors of the admin applications (aka admin GUI application, MEjb application).
     *
     * @return name of the auth realm to be used for admin access
     */
    @Attribute (defaultValue="admin-realm")
    @NotNull
    String getAuthRealmName();

    void setAuthRealmName(String name);

    @DuckTyped
    JmxConnector getSystemJmxConnector();

    @DuckTyped
    AuthRealm getAssociatedAuthRealm();

    @DuckTyped
    boolean usesFileRealm();

    public class Duck {
        public static JmxConnector getSystemJmxConnector(AdminService as) {
            List<JmxConnector> connectors = as.getJmxConnector();
            for (JmxConnector connector : connectors) {
                if (as.getSystemJmxConnectorName().equals(connector.getName())) {
                    return connector;
                }
            }
            return null;
        }

        /** This is the place where the iteration for the {@link AuthRealm} for administration should be carried out
         *  in server. A convenience method for the same. 
         *
         * @param as AdminService implemented by those who implement the interface (outer interface).
         * @return AuthRealm instance for which the name is same as as.getAuthRealmName(), null otherwise.
         */
        public static AuthRealm getAssociatedAuthRealm(AdminService as) {
            String rn                = as.getAuthRealmName();  //this is the name of admin-service@auth-realm-name
            Config cfg               = as.getParent(Config.class); //assumes the structure where <admin-service> resides directly under <config>
            SecurityService ss       = cfg.getSecurityService();
            List<AuthRealm> realms   = ss.getAuthRealm();
            for (AuthRealm realm : realms) {
                if (rn.equals(realm.getName()))
                    return realm;
            }
            return null;
        }

        /** Returns true if the classname of associated authrealm is same as fully qualified FileRealm classname.
         *
         * @param as "This" Admin Service
         * @return  true if associated authrealm is nonnull and its classname equals "com.sun.enterprise.security.auth.realm.file.FileRealm", false otherwise 
         */
        public static boolean usesFileRealm(AdminService as) {
            boolean usesFR = false;
            AuthRealm ar = as.getAssociatedAuthRealm();
            //Note: This is type unsafe.
            if (ar != null && "com.sun.enterprise.security.auth.realm.file.FileRealm".equals(ar.getClassname()))
                usesFR = true;
            return usesFR;
        }
    }
}
