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

import org.jvnet.hk2.config.Attribute;
import org.jvnet.hk2.config.Configured;
import org.jvnet.hk2.config.Element;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.types.Property;

import java.beans.PropertyVetoException;
import java.util.List;


import org.glassfish.api.admin.config.PropertiesDesc;
import org.jvnet.hk2.config.types.PropertyBag;

import org.glassfish.quality.ToDo;


/**
 * The security service element defines parameters and configuration information
 * needed by the core J2EE security service. Some container-specific security
 * configuration elements are in the various container configuration elements
 * and not here. SSL configuration is also elsewhere. At this time the security
 * service configuration consists of a set of authentication realms. A number of
 * top-level attributes are defined as well
 * 
 */
/* @XmlType(name = "", propOrder = {
    "authRealm",
    "jaccProvider",
    "auditModule",
    "messageSecurityConfig",
    "property"
}) */

@Configured
public interface SecurityService extends ConfigBeanProxy, PropertyBag {

    /**
     * Gets the value of the defaultRealm property.
     *
     * Specifies which realm (by name) is used by default when no realm is
     * specifically requested. The file realm is the common default
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="file")
    public String getDefaultRealm();

    /**
     * Sets the value of the defaultRealm property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setDefaultRealm(String value) throws PropertyVetoException;

    /**
     * Gets the value of the defaultPrincipal property.
     *
     * Used as the identity of default security contexts when necessary and
     * no principal is provided
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute
    public String getDefaultPrincipal();

    /**
     * Sets the value of the defaultPrincipal property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setDefaultPrincipal(String value) throws PropertyVetoException;

    /**
     * Gets the value of the defaultPrincipalPassword property.
     *
     * Password of default principal
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute
    public String getDefaultPrincipalPassword();

    /**
     * Sets the value of the defaultPrincipalPassword property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setDefaultPrincipalPassword(String value) throws PropertyVetoException;

    /**
     * Gets the value of the anonymousRole property.
     *
     * This attribute is deprecated.
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="AttributeDeprecated")
    public String getAnonymousRole();

    /**
     * Sets the value of the anonymousRole property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setAnonymousRole(String value) throws PropertyVetoException;

    /**
     * Gets the value of the auditEnabled property.
     *
     * If true, additional access logging is performed to provide
     * audit information
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="false",dataType=Boolean.class)
    public String getAuditEnabled();

    /**
     * Sets the value of the auditEnabled property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setAuditEnabled(String value) throws PropertyVetoException;

    /**
     * Gets the value of the jacc property.
     * Specifies the name of the jacc-provider element to use for setting up the
     * JACC infrastructure. The default value "default" does not need to be
     * changed unless adding a custom JACC provider.
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="default")
    public String getJacc();

    /**
     * Sets the value of the jacc property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setJacc(String value) throws PropertyVetoException;

    /**
     * Gets the value of the auditModules property.
     *
     * Optional list of audit provider modules which will be used by the audit
     * subsystem. Default value refers to the internal log-based audit module
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="default")
    public String getAuditModules();

    /**
     * Sets the value of the auditModules property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setAuditModules(String value) throws PropertyVetoException;

    /**
     * Gets the value of the activateDefaultPrincipalToRoleMapping property.
     *
     * Causes the appserver to apply a default principal to role mapping, to any
     * application that does not have an application specific mapping defined.
     * Every role is mapped to a same-named (as the role) instance of a
     * java.security.Principal implementation class (see mapped-principal-class)
     * This behavior is similar to that of Tomcat servlet container.
     * It is off by default.
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="false",dataType=Boolean.class)
    public String getActivateDefaultPrincipalToRoleMapping();

    /**
     * Sets the value of the activateDefaultPrincipalToRoleMapping property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setActivateDefaultPrincipalToRoleMapping(String value) throws PropertyVetoException;

    /**
     * Customizes the java.security.Principal implementation class used 
     * when activate-default-principal-to-role-mapping is set to true.
     * Should the default be set to com.sun.enterprise.deployment.Group?
     *
     * This attribute is used to customize the java.security.Principal
     * implementation class used in the default principal to role mapping.
     * This attribute is optional. When it is not specified,
     * com.sun.enterprise.deployment.Group implementation of
     * java.security.Principal is used. The value of this attribute is only
     * relevant when the activate-default principal-to-role-mapping attribute
     * is set to true
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute
    public String getMappedPrincipalClass();

    /**
     * Sets the value of the mappedPrincipalClass property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setMappedPrincipalClass(String value) throws PropertyVetoException;

    /**
     * Gets the value of the authRealm property.
     * <p/>
     * <p/>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the authRealm property.
     * <p/>
     * <p/>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getAuthRealm().add(newItem);
     * </pre>
     * <p/>
     * <p/>
     * <p/>
     * Objects of the following type(s) are allowed in the list
     * {@link AuthRealm }
     */
    @Element(required=true)
    public List<AuthRealm> getAuthRealm();

    /**
     * Gets the value of the jaccProvider property.
     * <p/>
     * <p/>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the jaccProvider property.
     * <p/>
     * <p/>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getJaccProvider().add(newItem);
     * </pre>
     * <p/>
     * <p/>
     * <p/>
     * Objects of the following type(s) are allowed in the list
     * {@link JaccProvider }
     */
    @Element(required=true)
    public List<JaccProvider> getJaccProvider();

    /**
     * Gets the value of the auditModule property.
     * <p/>
     * <p/>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the auditModule property.
     * <p/>
     * <p/>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getAuditModule().add(newItem);
     * </pre>
     * <p/>
     * <p/>
     * <p/>
     * Objects of the following type(s) are allowed in the list
     * {@link AuditModule }
     */
    @Element
    public List<AuditModule> getAuditModule();

    /**
     * Gets the value of the messageSecurityConfig property.
     *
     * Optional list of layer specific lists of configured
     * message security providers.
     * 
     * <p/>
     * <p/>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the messageSecurityConfig property.
     * <p/>
     * <p/>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getMessageSecurityConfig().add(newItem);
     * </pre>
     * <p/>
     * <p/>
     * <p/>
     * Objects of the following type(s) are allowed in the list
     * {@link MessageSecurityConfig }
     */
    @Element
    public List<MessageSecurityConfig> getMessageSecurityConfig();
    
    /**
    	Properties as per {@link PropertyBag}
     */
    @ToDo(priority=ToDo.Priority.IMPORTANT, details="Provide PropertyDesc for legal props" )
    @PropertiesDesc(props={})
    @Element
    List<Property> getProperty();
}
