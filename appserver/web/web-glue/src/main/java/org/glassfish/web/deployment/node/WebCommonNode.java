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

package org.glassfish.web.deployment.node;

import com.sun.enterprise.deployment.*;
import com.sun.enterprise.deployment.node.*;
import com.sun.enterprise.deployment.types.EjbReference;
import com.sun.enterprise.deployment.util.DOLUtils;
import com.sun.enterprise.deployment.web.LoginConfiguration;
import com.sun.enterprise.deployment.web.SessionConfig;
import com.sun.enterprise.deployment.xml.TagNames;
import com.sun.enterprise.deployment.xml.WebServicesTagNames;
import org.glassfish.web.deployment.descriptor.*;
import org.glassfish.web.deployment.xml.WebTagNames;
import org.w3c.dom.Node;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This node is responsible for handling the web-common xml tree
 *
 * @author  Shing Wai Chan
 * @version
 */
public abstract class WebCommonNode<T extends WebBundleDescriptorImpl> extends AbstractBundleNode<T> {
    public final static String SPEC_VERSION = "3.1";

    protected T descriptor;
    private Map<String, Vector<String>> servletMappings;

    /** Creates new WebBundleNode */
    protected WebCommonNode()  {
        super();

        registerElementHandler(new XMLElement(TagNames.ENVIRONMENT_PROPERTY), EnvEntryNode.class);                          
        registerElementHandler(new XMLElement(TagNames.EJB_REFERENCE), EjbReferenceNode.class);     
        registerElementHandler(new XMLElement(TagNames.EJB_LOCAL_REFERENCE), EjbLocalReferenceNode.class);     
        JndiEnvRefNode serviceRefNode = habitat.getService(JndiEnvRefNode.class, WebServicesTagNames.SERVICE_REF);
        if (serviceRefNode != null) {
            registerElementHandler(new XMLElement(WebServicesTagNames.SERVICE_REF), serviceRefNode.getClass(),"addServiceReferenceDescriptor");
        }
        registerElementHandler(new XMLElement(TagNames.RESOURCE_REFERENCE), 
                                                            ResourceRefNode.class, "addResourceReferenceDescriptor");   
        registerElementHandler(new XMLElement(TagNames.RESOURCE_ENV_REFERENCE), 
                                                            ResourceEnvRefNode.class, "addResourceEnvReferenceDescriptor");               
        registerElementHandler(new XMLElement(TagNames.MESSAGE_DESTINATION_REFERENCE), MessageDestinationRefNode.class, "addMessageDestinationReferenceDescriptor");
        registerElementHandler(new XMLElement(TagNames.PERSISTENCE_CONTEXT_REF), EntityManagerReferenceNode.class, "addEntityManagerReferenceDescriptor");
        registerElementHandler(new XMLElement(TagNames.PERSISTENCE_UNIT_REF), EntityManagerFactoryReferenceNode.class, "addEntityManagerFactoryReferenceDescriptor");
        registerElementHandler(new XMLElement(TagNames.ROLE), 
                                                            SecurityRoleNode.class, "addRole");            
        registerElementHandler(new XMLElement(WebTagNames.SERVLET), ServletNode.class);       
        registerElementHandler(new XMLElement(WebTagNames.SERVLET_MAPPING), ServletMappingNode.class);               
        registerElementHandler(new XMLElement(WebTagNames.SESSION_CONFIG), SessionConfigNode.class);
        registerElementHandler(new XMLElement(WebTagNames.MIME_MAPPING), 
                                                            MimeMappingNode.class, "addMimeMapping");
        registerElementHandler(new XMLElement(WebTagNames.CONTEXT_PARAM), 
                                                            InitParamNode.class, "addContextParameter");        
        registerElementHandler(new XMLElement(WebTagNames.SECURITY_CONSTRAINT),         
                                                            SecurityConstraintNode.class, "addSecurityConstraint");                
        registerElementHandler(new XMLElement(WebTagNames.FILTER), 
                                                            FilterNode.class, "addServletFilter");
        registerElementHandler(new XMLElement(WebTagNames.FILTER_MAPPING), 
                                                            FilterMappingNode.class, "addServletFilterMapping");            
        registerElementHandler(new XMLElement(WebTagNames.LISTENER), 
                                                            ListenerNode.class, "addAppListenerDescriptor");                    
        registerElementHandler(new XMLElement(WebTagNames.ERROR_PAGE), 
                                                            ErrorPageNode.class, "addErrorPageDescriptor");            
        registerElementHandler(new XMLElement(WebTagNames.LOGIN_CONFIG), 
                                                            LoginConfigNode.class);                    
        // for backward compatibility, from Servlet 2.4 the taglib element is in jsp-config
        registerElementHandler(new XMLElement(WebTagNames.TAGLIB),         
                                                            TagLibNode.class);                      
        registerElementHandler(new XMLElement(WebTagNames.JSPCONFIG),         
                                                            JspConfigNode.class);                      
        registerElementHandler(new XMLElement(WebTagNames.LOCALE_ENCODING_MAPPING),
                                                            LocaleEncodingMappingNode.class, "addLocaleEncodingMappingDescriptor");
        registerElementHandler(new XMLElement(TagNames.MESSAGE_DESTINATION),
                               MessageDestinationNode.class,
                               "addMessageDestination");
        registerElementHandler(new XMLElement(TagNames.POST_CONSTRUCT), LifecycleCallbackNode.class, "addPostConstructDescriptor");
        registerElementHandler(new XMLElement(TagNames.PRE_DESTROY), LifecycleCallbackNode.class, "addPreDestroyDescriptor");
        registerElementHandler(new XMLElement(TagNames.DATA_SOURCE), DataSourceDefinitionNode.class, "addResourceDescriptor");
        registerElementHandler(new XMLElement(TagNames.CONNECTION_FACTORY), ConnectionFactoryDefinitionNode.class, "addResourceDescriptor");
        registerElementHandler(new XMLElement(TagNames.JMS_CONNECTION_FACTORY), JMSConnectionFactoryDefinitionNode.class, "addResourceDescriptor");
        registerElementHandler(new XMLElement(TagNames.JMS_DESTINATION), JMSDestinationDefinitionNode.class, "addResourceDescriptor");
        registerElementHandler(new XMLElement(TagNames.MAIL_SESSION), MailSessionNode.class, "addResourceDescriptor");
        registerElementHandler(new XMLElement(TagNames.ADMINISTERED_OBJECT), AdministeredObjectDefinitionNode.class, "addResourceDescriptor");
    }
    
    /**
     * Adds  a new DOL descriptor instance to the descriptor instance associated with 
     * this XMLNode
     *
     * @param newDescriptor the new descriptor
     */    
    public void addDescriptor(Object  newDescriptor) {
        Logger logger = DOLUtils.getDefaultLogger();
        if (newDescriptor instanceof EjbReference) {            
            descriptor.addEjbReferenceDescriptor(
                        (EjbReference) newDescriptor);
        } else  if (newDescriptor instanceof EnvironmentProperty) {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("Adding env entry" + newDescriptor);
            }
            descriptor.addEnvironmentProperty((EnvironmentProperty) newDescriptor);
        } else if (newDescriptor instanceof WebComponentDescriptor) {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("Adding web component" + newDescriptor);
            }
            descriptor.addWebComponentDescriptor((WebComponentDescriptor) newDescriptor);
        } else if (newDescriptor instanceof TagLibConfigurationDescriptor) {
            // for backward compatibility with 2.2 and 2.3 specs, we need to be able 
            // to read tag lib under web-app. Starting with 2.4, the tag moved under jsp-config
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("Adding taglib component " + newDescriptor);
            }
            if (descriptor.getJspConfigDescriptor()==null) {
                descriptor.setJspConfigDescriptor(new JspConfigDescriptorImpl());
            }
            descriptor.getJspConfigDescriptor().addTagLib((TagLibConfigurationDescriptor) newDescriptor);
        } else if (newDescriptor instanceof JspConfigDescriptorImpl) {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("Adding JSP Config Descriptor" + newDescriptor);
            }
            if (descriptor.getJspConfigDescriptor()!=null) {
                throw new RuntimeException(
                    "Has more than one jsp-config element!");
            }
            descriptor.setJspConfigDescriptor(
                (JspConfigDescriptorImpl)newDescriptor);
        } else if (newDescriptor instanceof LoginConfiguration) {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("Adding Login Config Descriptor" + newDescriptor);
            }
            if (descriptor.getLoginConfiguration()!=null) {
                throw new RuntimeException(
                    "Has more than one login-config element!");
            }
            descriptor.setLoginConfiguration(
                (LoginConfiguration)newDescriptor);
        } else if (newDescriptor instanceof SessionConfig) {
            if (descriptor.getSessionConfig() != null) {
                throw new RuntimeException(
                    "Has more than one session-config element!");
            }
            descriptor.setSessionConfig((SessionConfig)newDescriptor);
        } else {
            super.addDescriptor(newDescriptor);
        }
    }       
    

    /**
     * receives notiification of the value for a particular tag
     * 
     * @param element the xml element
     * @param value it's associated value
     */    
    public void setElementValue(XMLElement element, String value) {    
        if (WebTagNames.WELCOME_FILE.equals(element.getQName())) {
            descriptor.addWelcomeFile(value);
        } else {
            super.setElementValue(element, value);
        }
    }       
    
    /**
     * add a servelt mapping for one of the servlet of this bundle
     * 
     * @param servletName the servlet the mapping applies to
     * @param urlPattern the url pattern mapping
     */
    void addServletMapping(String servletName, String urlPattern) {
        if (servletMappings==null) {
            servletMappings = new HashMap<String, Vector<String>>();
        } 
        if (servletMappings.containsKey(servletName)) {
            ((Vector<String>) servletMappings.get(servletName)).add(urlPattern);
        } else {
            Vector<String> mappings = new Vector<String>();
            mappings.add(urlPattern);
            servletMappings.put(servletName, mappings);
        }
    }
    
    /** 
     * receives notification of the end of an XML element by the Parser
     * 
     * @param element the xml tag identification
     * @return true if this node is done processing the XML sub tree
     */
    public boolean endElement(XMLElement element) {
        if (WebTagNames.DISTRIBUTABLE.equals(element.getQName())) {       
            descriptor.setDistributable(true);
            return false;
        } else {
            boolean allDone = super.endElement(element);
            if (allDone && servletMappings!=null) {
                for (Iterator<String> keys = servletMappings.keySet().iterator(); keys.hasNext();) {
                    String servletName = keys.next();
                    Vector<String> mappings = servletMappings.get(servletName);
                    WebComponentDescriptor servlet= descriptor.getWebComponentByCanonicalName(servletName);
                    if (servlet!=null) {
                        for (Iterator<String> mapping = mappings.iterator();mapping.hasNext();) {
                            servlet.addUrlPattern(mapping.next());
                        }
                    } else {
                        throw new RuntimeException("There is no web component by the name of " + servletName + " here.");                    
                    } 
                }
            }
            return allDone;
        }
    }

    /**
     * write the descriptor class to a DOM tree and return it
     *
     * @param parent node for the DOM tree
     * @param webBundleDesc descriptor to write
     * @return the DOM tree top node
     */    
    public Node writeDescriptor(Node parent, 
        T webBundleDesc) {
        Node jarNode = super.writeDescriptor(parent, webBundleDesc);             
        if (webBundleDesc.isDistributable()) {
            appendChild(jarNode, WebTagNames.DISTRIBUTABLE);        
        }
        
        // context-param*
        addInitParam(jarNode, WebTagNames.CONTEXT_PARAM, webBundleDesc.getContextParametersSet());
        
        // filter*
        FilterNode filterNode = new FilterNode();
        for (Enumeration filters = webBundleDesc.getServletFilters().elements();filters.hasMoreElements();) {
            filterNode.writeDescriptor(jarNode, WebTagNames.FILTER, 
                                                     (ServletFilterDescriptor) filters.nextElement());
        }
        
        // filter-mapping*
        FilterMappingNode filterMappingNode = new FilterMappingNode();
        for (Enumeration mappings = webBundleDesc.getServletFilterMappings().elements();
              mappings.hasMoreElements();) {
            filterMappingNode.writeDescriptor(jarNode, WebTagNames.FILTER_MAPPING, 
                                                     (ServletFilterMappingDescriptor) mappings.nextElement());
        }        
        
        // listener*
        Vector appListeners = webBundleDesc.getAppListenerDescriptors();
        if (!appListeners.isEmpty()) {
            ListenerNode listenerNode = new ListenerNode();
            for (Enumeration e = appListeners.elements();e.hasMoreElements();) {
                listenerNode.writeDescriptor(jarNode, WebTagNames.LISTENER,
                        (AppListenerDescriptorImpl) e.nextElement());
            }
        }
        
        Set servlets = webBundleDesc.getWebComponentDescriptors();
        if (!servlets.isEmpty()) {
            // servlet*
            ServletNode servletNode = new ServletNode();
            for (Iterator  e= servlets.iterator();e.hasNext();) {
                WebComponentDescriptor aServlet = (WebComponentDescriptor) e.next();
                servletNode.writeDescriptor(jarNode, aServlet);
            }

            // servlet-mapping*        
            for (Iterator servletsIterator = servlets.iterator(); servletsIterator.hasNext();) {
                WebComponentDescriptor aServlet = (WebComponentDescriptor) servletsIterator.next();                
                for (Iterator patterns = aServlet.getUrlPatternsSet().iterator();patterns.hasNext();) {
                    String pattern = (String) patterns.next();
                    Node mappingNode= appendChild(jarNode, WebTagNames.SERVLET_MAPPING);
                    appendTextChild(mappingNode, WebTagNames.SERVLET_NAME, aServlet.getCanonicalName());
                    
                    // If URL Pattern does not start with "/" then
                    // prepend it (for 1.2 Web apps)                    
                    if (webBundleDesc.getSpecVersion().equals("2.2")) {
                        if (!pattern.startsWith("/") 
                            && !pattern.startsWith("*.")) {
                            pattern = "/" + pattern;
                        }                    
                    }
                    appendTextChild(mappingNode, WebTagNames.URL_PATTERN, pattern);
                }
            }
        }
        
        // mime-mapping*
        MimeMappingNode mimeNode = new MimeMappingNode();
        for (Enumeration e = webBundleDesc.getMimeMappings();e.hasMoreElements();) {
            MimeMappingDescriptor mimeMapping = (MimeMappingDescriptor) e.nextElement();
            mimeNode.writeDescriptor(jarNode, WebTagNames.MIME_MAPPING, mimeMapping);
        }
        
        // welcome-file-list?
        Enumeration welcomeFiles = webBundleDesc.getWelcomeFiles();
        if (welcomeFiles.hasMoreElements()) {
            Node welcomeList = appendChild(jarNode, WebTagNames.WELCOME_FILE_LIST);
            while (welcomeFiles.hasMoreElements()) {
                appendTextChild(welcomeList, WebTagNames.WELCOME_FILE,
                                (String) welcomeFiles.nextElement());
            }
        }
        
        // error-page*
        Enumeration errorPages = webBundleDesc.getErrorPageDescriptors();
        if (errorPages.hasMoreElements()) {
            ErrorPageNode errorPageNode = new ErrorPageNode();
            while (errorPages.hasMoreElements()) {
                errorPageNode.writeDescriptor(jarNode, WebTagNames.ERROR_PAGE, 
                                (ErrorPageDescriptor) errorPages.nextElement());
            }
        }
        
        // jsp-config *
	JspConfigDescriptorImpl jspConf = webBundleDesc.getJspConfigDescriptor();
	if(jspConf != null) {
	    JspConfigNode ln = new JspConfigNode();
	    ln.writeDescriptor(jarNode, 
				WebTagNames.JSPCONFIG,
				jspConf);
	}

        // security-constraint*
        Enumeration securityConstraints = webBundleDesc.getSecurityConstraints();
        if (securityConstraints.hasMoreElements()) {
            SecurityConstraintNode scNode = new SecurityConstraintNode();
            while (securityConstraints.hasMoreElements()) {
                SecurityConstraintImpl sc = (SecurityConstraintImpl) securityConstraints.nextElement();
                scNode.writeDescriptor(jarNode, WebTagNames.SECURITY_CONSTRAINT, sc);
            }
        }

        // login-config ?
        LoginConfigurationImpl lci = (LoginConfigurationImpl) webBundleDesc.getLoginConfiguration();
        if (lci!=null) {
            LoginConfigNode lcn = new LoginConfigNode();
            lcn.writeDescriptor(jarNode, WebTagNames.LOGIN_CONFIG, lci);
        }
        
        // security-role*
        Enumeration roles = webBundleDesc.getSecurityRoles();
        if (roles.hasMoreElements()) {
            SecurityRoleNode srNode = new SecurityRoleNode();
            while (roles.hasMoreElements()) {
                SecurityRoleDescriptor role = (SecurityRoleDescriptor) roles.nextElement();
                srNode.writeDescriptor(jarNode, WebTagNames.ROLE, role);            
            }
        }

        // env-entry*
        writeEnvEntryDescriptors(jarNode, webBundleDesc.getEnvironmentProperties().iterator());

        // ejb-ref * and ejb-local-ref*
        writeEjbReferenceDescriptors(jarNode, webBundleDesc.getEjbReferenceDescriptors().iterator());

        // service-ref*
        writeServiceReferenceDescriptors(jarNode, webBundleDesc.getServiceReferenceDescriptors().iterator());

        // resource-ref*
        writeResourceRefDescriptors(jarNode, webBundleDesc.getResourceReferenceDescriptors().iterator());
                
        // resource-env-ref*
        writeResourceEnvRefDescriptors(jarNode, webBundleDesc.getResourceEnvReferenceDescriptors().iterator());        

        // message-destination-ref*
        writeMessageDestinationRefDescriptors(jarNode, webBundleDesc.getMessageDestinationReferenceDescriptors().iterator());
        
        // persistence-context-ref*
        writeEntityManagerReferenceDescriptors(jarNode, webBundleDesc.getEntityManagerReferenceDescriptors().iterator());
        
        // persistence-unit-ref*
        writeEntityManagerFactoryReferenceDescriptors(jarNode, webBundleDesc.getEntityManagerFactoryReferenceDescriptors().iterator());
        
        // post-construct
        writeLifeCycleCallbackDescriptors(jarNode, TagNames.POST_CONSTRUCT, webBundleDesc.getPostConstructDescriptors());

        // pre-destroy
        writeLifeCycleCallbackDescriptors(jarNode, TagNames.PRE_DESTROY, webBundleDesc.getPreDestroyDescriptors());

        // all descriptors (includes DSD, MSD, JMSCFD, JMSDD,AOD, CFD)*
        writeResourceDescriptors(jarNode, webBundleDesc.getAllResourcesDescriptors().iterator());

        // message-destination*
        writeMessageDestinations
            (jarNode, webBundleDesc.getMessageDestinations().iterator());

        // locale-encoding-mapping-list
        LocaleEncodingMappingListDescriptor lemListDesc = webBundleDesc.getLocaleEncodingMappingListDescriptor();
        if (lemListDesc != null) {
            Node lemList = appendChild(jarNode, WebTagNames.LOCALE_ENCODING_MAPPING_LIST);
            LocaleEncodingMappingNode lemNode = new LocaleEncodingMappingNode();
            for (LocaleEncodingMappingDescriptor lemDesc : lemListDesc.getLocaleEncodingMappingSet()) {
                lemNode.writeDescriptor(lemList, WebTagNames.LOCALE_ENCODING_MAPPING, lemDesc);
            }
        }

        if (webBundleDesc.getSessionConfig() != null) {
            SessionConfigNode scNode = new SessionConfigNode();
            scNode.writeDescriptor(jarNode, WebTagNames.SESSION_CONFIG,
                    (SessionConfigDescriptor)webBundleDesc.getSessionConfig());            
        }

        return jarNode;
    }
   
    static void addInitParam(Node parentNode, String nodeName, Set initParams) {
        if (!initParams.isEmpty()) {
            InitParamNode initParamNode = new InitParamNode();
            for (Iterator e=initParams.iterator();e.hasNext();) {
                EnvironmentProperty ep = (EnvironmentProperty) e.next();
                initParamNode.writeDescriptor(parentNode, nodeName, ep);
            }
        }    
    }
    
    static void addInitParam(Node parentNode, String nodeName, Enumeration initParams) {
        InitParamNode initParamNode = new InitParamNode();
        while (initParams.hasMoreElements()) {
            EnvironmentProperty ep = (EnvironmentProperty) initParams.nextElement();
            initParamNode.writeDescriptor(parentNode, nodeName, ep);
        }
    }
    
    /**
     * @return the default spec version level this node complies to
     */
    public String getSpecVersion() {
        return SPEC_VERSION;
    }
    
}
