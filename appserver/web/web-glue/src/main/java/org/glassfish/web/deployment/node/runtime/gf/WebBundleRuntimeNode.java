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
// Portions Copyright [2016-2019] [Payara Foundation and/or its affiliates]
package org.glassfish.web.deployment.node.runtime.gf;

import static com.sun.enterprise.deployment.xml.RuntimeTagNames.PAYARA_JAXRS_ROLES_ALLOWED_ENABLED;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.glassfish.deployment.common.SecurityRoleMapper;
import org.glassfish.security.common.Group;
import org.glassfish.security.common.Role;
import org.glassfish.web.deployment.descriptor.WebBundleDescriptorImpl;
import org.glassfish.web.deployment.node.WebBundleNode;
import org.glassfish.web.deployment.runtime.Cache;
import org.glassfish.web.deployment.runtime.ClassLoader;
import org.glassfish.web.deployment.runtime.JspConfig;
import org.glassfish.web.deployment.runtime.LocaleCharsetInfo;
import org.glassfish.web.deployment.runtime.Servlet;
import org.glassfish.web.deployment.runtime.SessionConfig;
import org.glassfish.web.deployment.runtime.SunWebAppImpl;
import org.glassfish.web.deployment.runtime.Valve;
import org.glassfish.web.deployment.runtime.WebProperty;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;

import com.sun.enterprise.deployment.Application;
import com.sun.enterprise.deployment.ResourceEnvReferenceDescriptor;
import com.sun.enterprise.deployment.ResourceReferenceDescriptor;
import com.sun.enterprise.deployment.ServiceReferenceDescriptor;
import com.sun.enterprise.deployment.WebComponentDescriptor;
import com.sun.enterprise.deployment.node.XMLElement;
import com.sun.enterprise.deployment.node.runtime.EjbRefNode;
import com.sun.enterprise.deployment.node.runtime.MessageDestinationRefNode;
import com.sun.enterprise.deployment.node.runtime.MessageDestinationRuntimeNode;
import com.sun.enterprise.deployment.node.runtime.ResourceEnvRefNode;
import com.sun.enterprise.deployment.node.runtime.ResourceRefNode;
import com.sun.enterprise.deployment.node.runtime.RuntimeBundleNode;
import com.sun.enterprise.deployment.node.runtime.RuntimeDescriptorNode;
import com.sun.enterprise.deployment.node.runtime.ServiceRefNode;
import com.sun.enterprise.deployment.node.runtime.WebServiceRuntimeNode;
import com.sun.enterprise.deployment.node.runtime.common.SecurityRoleMappingNode;
import com.sun.enterprise.deployment.runtime.common.PrincipalNameDescriptor;
import com.sun.enterprise.deployment.runtime.common.SecurityRoleMapping;
import com.sun.enterprise.deployment.runtime.web.IdempotentUrlPattern;
import com.sun.enterprise.deployment.runtime.web.SunWebApp;
import com.sun.enterprise.deployment.types.EjbReference;
import com.sun.enterprise.deployment.xml.DTDRegistry;
import com.sun.enterprise.deployment.xml.RuntimeTagNames;
import com.sun.enterprise.deployment.xml.WebServicesTagNames;
import java.util.Collections;

/**
 * This node is responsible for handling all runtime information for web bundle.
 *
 * @author Jerome Dochez
 * @version
 */
public class WebBundleRuntimeNode extends RuntimeBundleNode<WebBundleDescriptorImpl> {

    /**
     * Creates new WebBundleRuntimeNode
     * 
     * @param descriptor
     */
    public WebBundleRuntimeNode(WebBundleDescriptorImpl descriptor) {
        super(descriptor);
        // trigger registration in standard node, if it hasn't happened
        serviceLocator.getService(WebBundleNode.class);
        if (descriptor != null) {
            getSunDescriptor();
        }
    }

    /** Creates new WebBundleRuntimeNode */
    public WebBundleRuntimeNode() {
        this(null);
    }

    /**
     * Initialize the child handlers
     */
    @Override
    protected void init() {
        // we do not care about our standard DDS handles
        handlers = null;

        registerElementHandler(new XMLElement(RuntimeTagNames.SECURITY_ROLE_MAPPING), SecurityRoleMappingNode.class);
        registerElementHandler(new XMLElement(RuntimeTagNames.SERVLET), org.glassfish.web.deployment.node.runtime.gf.ServletNode.class);
        registerElementHandler(new XMLElement(RuntimeTagNames.IDEMPOTENT_URL_PATTERN), IdempotentUrlPatternNode.class);
        registerElementHandler(new XMLElement(RuntimeTagNames.SESSION_CONFIG), SessionConfigNode.class);
        registerElementHandler(new XMLElement(RuntimeTagNames.RESOURCE_ENV_REFERENCE), ResourceEnvRefNode.class);
        registerElementHandler(new XMLElement(RuntimeTagNames.MESSAGE_DESTINATION_REFERENCE), MessageDestinationRefNode.class);

        registerElementHandler(new XMLElement(RuntimeTagNames.RESOURCE_REFERENCE), ResourceRefNode.class);
        registerElementHandler(new XMLElement(RuntimeTagNames.EJB_REFERENCE), EjbRefNode.class);

        registerElementHandler(new XMLElement(RuntimeTagNames.CACHE), CacheNode.class);

        registerElementHandler(new XMLElement(RuntimeTagNames.CLASS_LOADER), ClassLoaderNode.class);

        registerElementHandler(new XMLElement(RuntimeTagNames.JSP_CONFIG), JspConfigRuntimeNode.class);

        registerElementHandler(new XMLElement(RuntimeTagNames.LOCALE_CHARSET_INFO), LocaleCharsetInfoNode.class);

        registerElementHandler(new XMLElement(RuntimeTagNames.PROPERTY), WebPropertyNode.class);

        registerElementHandler(new XMLElement(WebServicesTagNames.SERVICE_REF), ServiceRefNode.class);
        registerElementHandler(new XMLElement(RuntimeTagNames.MESSAGE_DESTINATION), MessageDestinationRuntimeNode.class);
        registerElementHandler(new XMLElement(WebServicesTagNames.WEB_SERVICE), WebServiceRuntimeNode.class);
        registerElementHandler(new XMLElement(RuntimeTagNames.VALVE), ValveNode.class);

    }

    /**
     * @return the XML tag associated with this XMLNode
     */
    @Override
    protected XMLElement getXMLRootTag() {
        return new XMLElement(RuntimeTagNames.S1AS_WEB_RUNTIME_TAG);
    }

    /**
     * @return the DOCTYPE that should be written to the XML file
     */
    @Override
    public String getDocType() {
        return DTDRegistry.SUN_WEBAPP_300_DTD_PUBLIC_ID;
    }

    /**
     * @return the SystemID of the XML file
     */
    @Override
    public String getSystemID() {
        return DTDRegistry.SUN_WEBAPP_300_DTD_SYSTEM_ID;
    }

    /**
     * @return NULL for all runtime nodes.
     */
    @Override
    public List<String> getSystemIDs() {
        return null;
    }

    /**
     * register this node as a root node capable of loading entire DD files
     *
     * @param publicIDToDTD is a mapping between xml Public-ID to DTD
     * @param versionUpgrades The list of upgrades from older versions to the latest schema
     * @return the doctype tag name
     */
    public static String registerBundle(Map<String, String> publicIDToDTD, Map<String, List<Class<?>>> versionUpgrades) {
        publicIDToDTD.put(DTDRegistry.SUN_WEBAPP_230_DTD_PUBLIC_ID, DTDRegistry.SUN_WEBAPP_230_DTD_SYSTEM_ID);
        publicIDToDTD.put(DTDRegistry.SUN_WEBAPP_231_DTD_PUBLIC_ID, DTDRegistry.SUN_WEBAPP_231_DTD_SYSTEM_ID);
        publicIDToDTD.put(DTDRegistry.SUN_WEBAPP_240_DTD_PUBLIC_ID, DTDRegistry.SUN_WEBAPP_240_DTD_SYSTEM_ID);
        publicIDToDTD.put(DTDRegistry.SUN_WEBAPP_241_DTD_PUBLIC_ID, DTDRegistry.SUN_WEBAPP_241_DTD_SYSTEM_ID);
        publicIDToDTD.put(DTDRegistry.SUN_WEBAPP_250_DTD_PUBLIC_ID, DTDRegistry.SUN_WEBAPP_250_DTD_SYSTEM_ID);
        publicIDToDTD.put(DTDRegistry.SUN_WEBAPP_300_DTD_PUBLIC_ID, DTDRegistry.SUN_WEBAPP_300_DTD_SYSTEM_ID);
        
        if (!restrictDTDDeclarations()) {
            publicIDToDTD.put(DTDRegistry.SUN_WEBAPP_240beta_DTD_PUBLIC_ID, DTDRegistry.SUN_WEBAPP_240beta_DTD_SYSTEM_ID);
        }

        return RuntimeTagNames.S1AS_WEB_RUNTIME_TAG;
    }

    /**
     * @return the descriptor instance to associate with this XMLNode
     */
    public Object getSunDescriptor() {
        return descriptor.getSunDescriptor();
    }

    /**
     * Adds a new DOL descriptor instance to the descriptor instance associated with this XMLNode
     *
     * @param newDescriptor the new descriptor
     */
    @Override
    public void addDescriptor(Object newDescriptor) {

        SunWebAppImpl sunWebApp = (SunWebAppImpl) descriptor.getSunDescriptor();

        if (newDescriptor instanceof WebComponentDescriptor) {
            WebComponentDescriptor servlet = (WebComponentDescriptor) newDescriptor;
            // for backward compatibility with s1as schema2beans generated desc
            Servlet s1descriptor = new Servlet();
            s1descriptor.setServletName(servlet.getCanonicalName());
            if (servlet.getRunAsIdentity() != null) {
                s1descriptor.setPrincipalName(servlet.getRunAsIdentity().getPrincipal());
            }
            sunWebApp.addServlet(s1descriptor);
        } else if (newDescriptor instanceof ServiceReferenceDescriptor) {
            descriptor.addServiceReferenceDescriptor((ServiceReferenceDescriptor) newDescriptor);
        } else if (newDescriptor instanceof SecurityRoleMapping) {
            SecurityRoleMapping srm = (SecurityRoleMapping) newDescriptor;
            sunWebApp.addSecurityRoleMapping(srm);
            // store it in the application using pure DOL descriptors...
            Application app = descriptor.getApplication();
            if (app != null) {
                Role role = new Role(srm.getRoleName());
                SecurityRoleMapper rm = app.getRoleMapper();
                if (rm != null) {
                    List<PrincipalNameDescriptor> principals = srm.getPrincipalNames();
                    for (int i = 0; i < principals.size(); i++) {
                        rm.assignRole(principals.get(i).getPrincipal(), role, descriptor);
                    }
                    List<String> groups = srm.getGroupNames();
                    for (int i = 0; i < groups.size(); i++) {
                        rm.assignRole(new Group(groups.get(i)), role, descriptor);
                    }
                }
            }
        } else if (newDescriptor instanceof IdempotentUrlPattern) {
            sunWebApp.addIdempotentUrlPattern((IdempotentUrlPattern) newDescriptor);
        } else if (newDescriptor instanceof SessionConfig) {
            sunWebApp.setSessionConfig((SessionConfig) newDescriptor);
        } else if (newDescriptor instanceof Cache) {
            sunWebApp.setCache((Cache) newDescriptor);
        } else if (newDescriptor instanceof ClassLoader) {
            sunWebApp.setClassLoader((ClassLoader) newDescriptor);
        } else if (newDescriptor instanceof JspConfig) {
            sunWebApp.setJspConfig((JspConfig) newDescriptor);
        } else if (newDescriptor instanceof LocaleCharsetInfo) {
            sunWebApp.setLocaleCharsetInfo((LocaleCharsetInfo) newDescriptor);
        } else if (newDescriptor instanceof WebProperty) {
            sunWebApp.addWebProperty((WebProperty) newDescriptor);
        } else if (newDescriptor instanceof Valve) {
            sunWebApp.addValve((Valve) newDescriptor);
        } else
            super.addDescriptor(descriptor);
    }

    @Override
    public void startElement(XMLElement element, Attributes attributes) {
        if (element.getQName().equals(RuntimeTagNames.PARAMETER_ENCODING)) {
            SunWebAppImpl sunWebApp = (SunWebAppImpl) getSunDescriptor();
            sunWebApp.setParameterEncoding(true);
            for (int i = 0; i < attributes.getLength(); i++) {
                if (RuntimeTagNames.DEFAULT_CHARSET.equals(attributes.getQName(i))) {
                    sunWebApp.setAttributeValue(SunWebApp.PARAMETER_ENCODING, SunWebApp.DEFAULT_CHARSET, attributes.getValue(i));
                }
                if (RuntimeTagNames.FORM_HINT_FIELD.equals(attributes.getQName(i))) {
                    sunWebApp.setAttributeValue(SunWebApp.PARAMETER_ENCODING, SunWebApp.FORM_HINT_FIELD, attributes.getValue(i));
                }
            }
        } else
            super.startElement(element, attributes);
    }

    /**
     * parsed an attribute of an element
     *
     * @param elementName the element name
     * @param attributeName the attribute name
     * @param value the attribute value
     * @return true if the attribute was processed
     */
    @Override
    protected boolean setAttributeValue(XMLElement elementName, XMLElement attributeName, String value) {
        SunWebAppImpl sunWebApp = (SunWebAppImpl) getSunDescriptor();
        if (attributeName.getQName().equals(RuntimeTagNames.ERROR_URL)) {
            sunWebApp.setAttributeValue(SunWebApp.ERROR_URL, value);
            return true;
        }
        if (attributeName.getQName().equals(RuntimeTagNames.HTTPSERVLET_SECURITY_PROVIDER)) {
            sunWebApp.setAttributeValue(SunWebApp.HTTPSERVLET_SECURITY_PROVIDER, value);
            return true;
        }

        return false;
    }

    /**
     * receives notification of the value for a particular tag
     *
     * @param element the xml element
     * @param value it's associated value
     */
    @Override
    public void setElementValue(XMLElement element, String value) {
        if (element.getQName().equals(RuntimeTagNames.CONTEXT_ROOT)) {
            // only set the context root for standalone war;
            // for embedded war, the context root will be set
            // using the value in application.xml
            Application app = descriptor.getApplication();
            if (app == null || app.isVirtual()) {
                descriptor.setContextRoot(value);
            }
        } else if (element.getQName().equals(RuntimeTagNames.KEEP_STATE)) {
            descriptor.setKeepState(value);
        } else if (element.getQName().equals(RuntimeTagNames.VERSION_IDENTIFIER)) {
        } else if (element.getQName().equals(RuntimeTagNames.PAYARA_SCANNING_INCLUDE)) {
            if (descriptor.getApplication() != null) {
                descriptor.getApplication().addScanningInclusions(Collections.singletonList(value), "WEB-INF/lib");
            }
        } else if (element.getQName().equals(RuntimeTagNames.PAYARA_SCANNING_EXCLUDE)) {
            if (descriptor.getApplication() != null) {
                descriptor.getApplication().addScanningExclusions(Collections.singletonList(value), "WEB-INF/lib");
            }
        } else if (element.getQName().equals(PAYARA_JAXRS_ROLES_ALLOWED_ENABLED)) {
            descriptor.setJaxrsRolesAllowedEnabled(Boolean.parseBoolean(value));
        } else if (element.getQName().equals("container-initializer-enabled")) {
            descriptor.setServletInitializersEnabled(Boolean.parseBoolean(value));
        } else if (element.getQName().equals(RuntimeTagNames.PAYARA_WHITELIST_PACKAGE)) {
            if (descriptor.getApplication() != null) {
                descriptor.getApplication().addWhitelistPackage(value);
            }
        } else
            super.setElementValue(element, value);
    }

    /**
     * write the descriptor class to a DOM tree and return it
     *
     * @param parent node for the DOM tree
     * @param bundleDescriptor the descriptor to write
     * @return the DOM tree top node
     */
    @Override
    public Node writeDescriptor(Node parent, WebBundleDescriptorImpl bundleDescriptor) {
        Element web = (Element) super.writeDescriptor(parent, bundleDescriptor);
        SunWebAppImpl sunWebApp = (SunWebAppImpl) bundleDescriptor.getSunDescriptor();

        // context-root?
        appendTextChild(web, RuntimeTagNames.CONTEXT_ROOT, bundleDescriptor.getContextRoot());
        // security-role-mapping
        SecurityRoleMapping[] roleMappings = sunWebApp.getSecurityRoleMapping();
        if (roleMappings != null && roleMappings.length > 0) {
            SecurityRoleMappingNode srmn = new SecurityRoleMappingNode();
            for (SecurityRoleMapping roleMapping : roleMappings) {
                srmn.writeDescriptor(web, RuntimeTagNames.SECURITY_ROLE_MAPPING, roleMapping);
            }
        }

        // servlet
        Set servlets = bundleDescriptor.getServletDescriptors();
        org.glassfish.web.deployment.node.runtime.gf.ServletNode servletNode = new org.glassfish.web.deployment.node.runtime.gf.ServletNode();
        for (Iterator itr = servlets.iterator(); itr.hasNext();) {
            WebComponentDescriptor servlet = (WebComponentDescriptor) itr.next();
            servletNode.writeDescriptor(web, RuntimeTagNames.SERVLET, servlet);
        }

        // idempotent-url-pattern
        IdempotentUrlPattern[] patterns = sunWebApp.getIdempotentUrlPatterns();
        if (patterns != null && patterns.length > 0) {
            IdempotentUrlPatternNode node = new IdempotentUrlPatternNode();
            for (IdempotentUrlPattern pattern : patterns) {
                node.writeDescriptor(web, RuntimeTagNames.IDEMPOTENT_URL_PATTERN, pattern);
            }
        }

        // session-config?
        if (sunWebApp.getSessionConfig() != null) {
            SessionConfigNode scn = new SessionConfigNode();
            scn.writeDescriptor(web, RuntimeTagNames.SESSION_CONFIG, sunWebApp.getSessionConfig());
        }

        // ejb-ref*
        Set<EjbReference> ejbRefs = bundleDescriptor.getEjbReferenceDescriptors();
        if (ejbRefs.size() > 0) {
            EjbRefNode node = new EjbRefNode();
            for (EjbReference ejbRef : ejbRefs) {
                node.writeDescriptor(web, RuntimeTagNames.EJB_REF, ejbRef);
            }
        }

        // resource-ref*
        Set<ResourceReferenceDescriptor> resourceRefs = bundleDescriptor.getResourceReferenceDescriptors();
        if (resourceRefs.size() > 0) {
            ResourceRefNode node = new ResourceRefNode();
            for (ResourceReferenceDescriptor resourceRef : resourceRefs) {
                node.writeDescriptor(web, RuntimeTagNames.RESOURCE_REF, resourceRef);
            }
        }

        // resource-env-ref*
        Set<ResourceEnvReferenceDescriptor> resourceEnvRefs = bundleDescriptor.getResourceEnvReferenceDescriptors();
        if (resourceEnvRefs.size() > 0) {
            ResourceEnvRefNode node = new ResourceEnvRefNode();
            for (ResourceEnvReferenceDescriptor resourceEnvRef : resourceEnvRefs) {
                node.writeDescriptor(web, RuntimeTagNames.RESOURCE_ENV_REF, resourceEnvRef);
            }
        }

        // service-ref*
        if (bundleDescriptor.hasServiceReferenceDescriptors()) {
            ServiceRefNode serviceNode = new ServiceRefNode();
            for (ServiceReferenceDescriptor next : bundleDescriptor.getServiceReferenceDescriptors()) {
                serviceNode.writeDescriptor(web, WebServicesTagNames.SERVICE_REF, next);
            }
        }

        // message-destination-ref*
        MessageDestinationRefNode.writeMessageDestinationReferences(web, bundleDescriptor);

        // cache?
        Cache cache = sunWebApp.getCache();
        if (cache != null) {
            CacheNode cn = new CacheNode();
            cn.writeDescriptor(web, RuntimeTagNames.CACHE, cache);
        }

        // class-loader?
        ClassLoader classLoader = sunWebApp.getClassLoader();
        if (classLoader != null) {
            ClassLoaderNode cln = new ClassLoaderNode();
            cln.writeDescriptor(web, RuntimeTagNames.CLASS_LOADER, classLoader);
        }

        // jsp-config?
        if (sunWebApp.getJspConfig() != null) {
            WebPropertyNode propertyNode = new WebPropertyNode();
            Node jspConfig = appendChild(web, RuntimeTagNames.JSP_CONFIG);
            propertyNode.writeDescriptor(jspConfig, RuntimeTagNames.PROPERTY, sunWebApp.getJspConfig().getWebProperty());
        }

        // locale-charset-info?
        if (sunWebApp.getLocaleCharsetInfo() != null) {
            LocaleCharsetInfoNode localeNode = new LocaleCharsetInfoNode();
            localeNode.writeDescriptor(web, RuntimeTagNames.LOCALE_CHARSET_INFO, sunWebApp.getLocaleCharsetInfo());
        }

        // parameter-encoding?
        if (sunWebApp.isParameterEncoding()) {
            Element parameter = appendChild(web, RuntimeTagNames.PARAMETER_ENCODING);

            if (sunWebApp.getAttributeValue(SunWebApp.PARAMETER_ENCODING, SunWebApp.FORM_HINT_FIELD) != null) {
                setAttribute(parameter, RuntimeTagNames.FORM_HINT_FIELD, sunWebApp.getAttributeValue(SunWebApp.PARAMETER_ENCODING, SunWebApp.FORM_HINT_FIELD));
            }

            if (sunWebApp.getAttributeValue(SunWebApp.PARAMETER_ENCODING, SunWebApp.DEFAULT_CHARSET) != null) {
                setAttribute(parameter, RuntimeTagNames.DEFAULT_CHARSET, sunWebApp.getAttributeValue(SunWebApp.PARAMETER_ENCODING, SunWebApp.DEFAULT_CHARSET));
            }
        }

        // property*
        WebPropertyNode props = new WebPropertyNode();
        props.writeDescriptor(web, RuntimeTagNames.PROPERTY, sunWebApp.getWebProperty());

        // valve*
        if (sunWebApp.getValve() != null) {
            ValveNode valve = new ValveNode();
            valve.writeDescriptor(web, RuntimeTagNames.VALVE, sunWebApp.getValve());
        }

        // message-destination*
        RuntimeDescriptorNode.writeMessageDestinationInfo(web, bundleDescriptor);

        // webservice-description*
        WebServiceRuntimeNode webServiceNode = new WebServiceRuntimeNode();
        webServiceNode.writeWebServiceRuntimeInfo(web, bundleDescriptor);

        // error-url
        if (sunWebApp.getAttributeValue(SunWebApp.ERROR_URL) != null) {
            setAttribute(web, RuntimeTagNames.ERROR_URL, sunWebApp.getAttributeValue(SunWebApp.ERROR_URL));
        }

        // httpservlet-security-provider
        if (sunWebApp.getAttributeValue(SunWebApp.HTTPSERVLET_SECURITY_PROVIDER) != null) {
            setAttribute(web, RuntimeTagNames.HTTPSERVLET_SECURITY_PROVIDER, sunWebApp.getAttributeValue(SunWebApp.HTTPSERVLET_SECURITY_PROVIDER));
        }

        // keep-state
        appendTextChild(web, RuntimeTagNames.KEEP_STATE, String.valueOf(bundleDescriptor.getKeepState()));

        return web;
    }
}
