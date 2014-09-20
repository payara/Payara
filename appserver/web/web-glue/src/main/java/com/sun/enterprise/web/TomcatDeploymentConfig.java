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

package com.sun.enterprise.web;

import com.sun.enterprise.deployment.*;
import com.sun.enterprise.deployment.types.EjbReference;
import com.sun.enterprise.deployment.web.*;
import com.sun.enterprise.web.deploy.*;
import com.sun.enterprise.web.session.WebSessionCookieConfig;
import org.apache.catalina.Container;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Wrapper;
import org.apache.catalina.core.StandardWrapper;
import org.glassfish.logging.annotation.LogMessageInfo;
import org.glassfish.web.deployment.descriptor.*;

import javax.servlet.SessionCookieConfig;
import javax.servlet.descriptor.JspPropertyGroupDescriptor;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class decorates all <code>com.sun.enterprise.deployment.*</code>
 * objects in order to make them usuable by the Catalina container. 
 * This avoid having duplicate memory representation of the web.xml (as well
 * as parsing the web.xml twice)
 * 
 * @author Jean-Francois Arcand
 */
public class TomcatDeploymentConfig {

    private static final Logger logger = com.sun.enterprise.web.WebContainer.logger;

    @LogMessageInfo(
            message = "Security role name {0} used in an <auth-constraint> without being defined in a <security-role>",
            level = "WARNING")
    public static final String ROLE_AUTH = "AS-WEB-GLUE-00132";

    @LogMessageInfo(
            message = "Security role name {0} used in a <run-as> without being defined in a <security-role>",
            level = "WARNING")
    public static final String ROLE_RUNAS = "AS-WEB-GLUE-00133";

    @LogMessageInfo(
            message = "Security role name {0} used in a <role-link> without being defined in a <security-role>",
            level = "WARNING")
    public static final String ROLE_LINK = "AS-WEB-GLUE-00134";


    /**
     * Configure a <code>WebModule</code> by applying web.xml information
     * contained in <code>WebBundleDescriptor</code>. This astatic void calling
     * Tomcat 5 internal deployment mechanism by re-using the DOL objects.
     */
    public static void configureWebModule(WebModule webModule, 
        WebBundleDescriptorImpl webModuleDescriptor)
            throws LifecycleException { 

        // When context root = "/"
        if ( webModuleDescriptor == null ){
            return;
        }
        
        webModule.setDisplayName(webModuleDescriptor.getDisplayName());
        webModule.setDistributable(webModuleDescriptor.isDistributable());
        webModule.setReplaceWelcomeFiles(true);        
        configureStandardContext(webModule,webModuleDescriptor);
        configureContextParam(webModule,webModuleDescriptor);
        configureApplicationListener(webModule,webModuleDescriptor);
        configureEjbReference(webModule,webModuleDescriptor);
        configureContextEnvironment(webModule,webModuleDescriptor);
        configureErrorPage(webModule,webModuleDescriptor);
        configureFilterDef(webModule,webModuleDescriptor);
        configureFilterMap(webModule,webModuleDescriptor);
        configureLoginConfig(webModule,webModuleDescriptor);
        configureMimeMapping(webModule,webModuleDescriptor);
        configureResourceRef(webModule,webModuleDescriptor);
        configureMessageDestination(webModule,webModuleDescriptor);
        configureContextResource(webModule,webModuleDescriptor);
        configureSecurityConstraint(webModule,webModuleDescriptor);
        configureJspConfig(webModule,webModuleDescriptor);
        configureSecurityRoles(webModule, webModuleDescriptor);
    }

    
    /**
     * Configures EJB resource reference for a web application, as
     * represented in a <code>&lt;ejb-ref&gt;</code> and 
     * <code>&lt;ejb-local-ref&gt;</code>element in the
     * deployment descriptor.
     */
    protected static void configureEjbReference(WebModule webModule,
                                         WebBundleDescriptorImpl wmd) {
        for (EjbReference ejbDescriptor :
                wmd.getEjbReferenceDescriptors()) {
            if (ejbDescriptor.isLocal()) {
                configureContextLocalEjb(webModule,
                    (EjbReferenceDescriptor) ejbDescriptor);
            } else {
                configureContextEjb(webModule,
                    (EjbReferenceDescriptor) ejbDescriptor);
            }           
        }                                                        
    }
    
     
    /**
     * Configures EJB resource reference for a web application, as
     * represented in a <code>&lt;ejb-ref&gt;</code> in the
     * deployment descriptor.
     */    
    protected static void configureContextLocalEjb(WebModule webModule,
                                        EjbReferenceDescriptor ejbDescriptor) {
        ContextLocalEjbDecorator decorator = 
                                new ContextLocalEjbDecorator(ejbDescriptor);
        webModule.addLocalEjb(decorator);
    
    }

    
    /**
     * Configures EJB resource reference for a web application, as
     * represented in a <code>&lt;ejb-local-ref&gt;</code>element in the
     * deployment descriptor.
     */    
    protected static void configureContextEjb(WebModule webModule,
                                       EjbReferenceDescriptor ejbDescriptor) {
        ContextEjbDecorator decorator = new ContextEjbDecorator(ejbDescriptor);
        webModule.addEjb(decorator);        
    }

    
    /**
     * Configure application environment entry, as represented by
     * an <code>&lt;env-entry&gt;</code> element in the deployment descriptor.
     */
    protected static void configureContextEnvironment(WebModule webModule,
                                                 WebBundleDescriptorImpl wmd) {
        for (ContextParameter envRef : wmd.getContextParametersSet()) {
            webModule.addEnvironment(new ContextEnvironmentDecorator(
                (EnvironmentProperty) envRef));
        }
    }

    
    /**
     * Configure error page element for a web application,
     * as represented by a <code>&lt;error-page&gt;</code> element in the
     * deployment descriptor.
     */
    protected static void configureErrorPage(WebModule webModule,
                                      WebBundleDescriptorImpl wmd) {
            
        Enumeration<ErrorPageDescriptor> e =
            wmd.getErrorPageDescriptors();
        while (e.hasMoreElements()){
            webModule.addErrorPage(new ErrorPageDecorator(e.nextElement()));
        }                                     
    }
    
    
    /**
     * Configure filter definition for a web application, as represented
     * by a <code>&lt;filter&gt;</code> element in the deployment descriptor.
     */
    protected static void configureFilterDef(WebModule webModule,
                                             WebBundleDescriptorImpl wmd) {
                                                        
       Vector vector = wmd.getServletFilters();
       
       FilterDefDecorator filterDef;
       ServletFilter servletFilter;
       
       for (int i=0; i < vector.size(); i++)  {
           servletFilter = (ServletFilter)vector.get(i);
           filterDef = new FilterDefDecorator(servletFilter);
          
           webModule.addFilterDef(filterDef);          
       }                                                    
    }
        
    
    /**
     * Configure filter mapping for a web application, as represented
     * by a <code>&lt;filter-mapping&gt;</code> element in the deployment
     * descriptor.  Each filter mapping must contain a filter name plus either
     * a URL pattern or a servlet name.    
     */
    protected static void configureFilterMap(WebModule webModule,
                                             WebBundleDescriptorImpl wmd) {
        Vector vector = wmd.getServletFilterMappingDescriptors();
        for (int i=0; i < vector.size(); i++)  {
            webModule.addFilterMap((ServletFilterMapping)vector.get(i));
        }
    }
    
    
    /**
     * Configure context initialization parameter that is configured
     * in the server configuration file, rather than the application deployment
     * descriptor.  This is convenient for establishing default values (which
     * may be configured to allow application overrides or not) without having
     * to modify the application deployment descriptor itself.  
     */             
    protected static void configureApplicationListener(
            WebModule webModule, WebBundleDescriptorImpl wmd) {
        
        Vector vector = wmd.getAppListenerDescriptors();
        for (int i=0; i < vector.size() ; i++){
            webModule.addApplicationListener( 
                        ((AppListenerDescriptor)vector.get(i)).getListener() );
        }
         
    }
    
    
    /**
     * Configure <code>jsp-config</code> element contained in the deployment
     * descriptor
     */
    protected static void configureJspConfig(WebModule webModule,
                                             WebBundleDescriptorImpl wmd) {
        webModule.setJspConfigDescriptor(wmd.getJspConfigDescriptor());

        JspConfigDescriptorImpl jspConfig = wmd.getJspConfigDescriptor();
        if (jspConfig != null) {
            for (JspPropertyGroupDescriptor jspGroup :
                    jspConfig.getJspPropertyGroups()) {
                for (String urlPattern : jspGroup.getUrlPatterns()) {
                    webModule.addJspMapping(urlPattern);
                }
            }
        }
    }

        
    /**
     * Configure a login configuration element for a web application,
     * as represented by a <code>&lt;login-config&gt;</code> element in the
     * deployment descriptor.
     */ 
    protected static void configureLoginConfig(WebModule webModule,
                                               WebBundleDescriptorImpl wmd) {
        LoginConfiguration loginConf = wmd.getLoginConfiguration();
        if ( loginConf == null ){
            return;
        }

        LoginConfigDecorator decorator = new LoginConfigDecorator(loginConf);
        webModule.setLoginConfig(decorator);         
    }
    
    
    /**
     * Configure mime-mapping defined in the deployment descriptor.
     */
    protected static void configureMimeMapping(WebModule webModule,
                                               WebBundleDescriptorImpl wmd) {
        Enumeration enumeration = wmd.getMimeMappings();
        MimeMapping mimeMapping;
        while (enumeration.hasMoreElements()){
            mimeMapping = (MimeMapping)enumeration.nextElement();
            webModule.addMimeMapping(mimeMapping.getExtension(),
                                     mimeMapping.getMimeType());            
        }
    }
    
    
    /**
     * Configure resource-reference defined in the deployment descriptor.
     */
    protected static void configureResourceRef(WebModule webModule,
                                               WebBundleDescriptorImpl wmd) {
        for (EnvironmentEntry envEntry : wmd.getEnvironmentProperties()) {
            webModule.addResourceEnvRef(envEntry.getName(), 
                                        envEntry.getType());
        }                                                                     
    }
    
    
    /**
     * Configure context parameter defined in the deployment descriptor.
     */
    protected static void configureContextParam(WebModule webModule,
                                                WebBundleDescriptorImpl wmd) {
        for (ContextParameter ctxParam : wmd.getContextParametersSet()) {
            if ("com.sun.faces.injectionProvider".equals(
                            ctxParam.getName()) && 
                    "com.sun.faces.vendor.GlassFishInjectionProvider".equals(
                            ctxParam.getValue())) {
                // Ignore, see IT 9641
                continue;
            }
            webModule.addParameter(ctxParam.getName(), ctxParam.getValue());
        }
    }
    
    
    /**
     * Configure of a message destination for a web application, as
     * represented in a <code>&lt;message-destination&gt;</code> element
     * in the deployment descriptor.
     */    
    protected static void configureMessageDestination(
            WebModule webModule, WebBundleDescriptorImpl wmd) {
        for (MessageDestinationDescriptor msgDrd :
                wmd.getMessageDestinations()) {
            webModule.addMessageDestination(
                new MessageDestinationDecorator(msgDrd));
        }                                              
    }

    
    /**
     * Representation of a message destination reference for a web application,
     * as represented by a <code>&lt;message-destination-ref&gt;</code> element
     * in the deployment descriptor.
     */
    protected static void configureMessageDestinationRef(
            WebModule webModule, WebBundleDescriptorImpl wmd) {
        for (MessageDestinationReferenceDescriptor msgDrd :
                wmd.getMessageDestinationReferenceDescriptors()) {            
            webModule.addMessageDestinationRef(
                new MessageDestinationRefDecorator(msgDrd));
        }                                                             
    }
    
        
    /**
     * Configure a resource reference for a web application, as
     * represented in a <code>&lt;resource-ref&gt;</code> element in the
     * deployment descriptor.
     */    
    protected static void configureContextResource(WebModule webModule,
                                                   WebBundleDescriptorImpl wmd) {
        for (ResourceReferenceDescriptor resRefDesc :
                wmd.getResourceReferenceDescriptors()) {
            webModule.addResource(new ContextResourceDecorator(resRefDesc)); 
        }
    }
   
    
    /**
     * Configure the <code>WebModule</code> instance by creating 
     * <code>StandardWrapper</code> using the information contained
     * in the deployment descriptor (Welcome Files, JSP, Servlets etc.)
     */
    protected static void configureStandardContext(WebModule webModule,
                                                   WebBundleDescriptorImpl wmd) {
        StandardWrapper wrapper;    
        Enumeration enumeration;
        SecurityRoleReference securityRoleReference;
       
        for (WebComponentDescriptor webComponentDesc :
                wmd.getWebComponentDescriptors()) {

            if (!webComponentDesc.isEnabled()) {
                continue;
            }

            wrapper = (StandardWrapper)webModule.createWrapper();
            wrapper.setName(webComponentDesc.getCanonicalName());

            String impl = webComponentDesc.getWebComponentImplementation();
            if (impl != null && !impl.isEmpty()) {
                if (webComponentDesc.isServlet()){
                    wrapper.setServletClassName(impl);
                } else {
                    wrapper.setJspFile(impl);
                }
            }

            /*
             * Add the wrapper only after we have set its 
             * servletClassName, so we know whether we're dealing with
             * a JSF app
             */
            webModule.addChild(wrapper);

            enumeration = webComponentDesc.getInitializationParameters();
            InitializationParameter initP = null;
            while (enumeration.hasMoreElements()) {
                initP = (InitializationParameter)enumeration.nextElement();
                wrapper.addInitParameter(initP.getName(), initP.getValue());
            }

            if (webComponentDesc.getLoadOnStartUp() != null) {
                wrapper.setLoadOnStartup(webComponentDesc.getLoadOnStartUp());
            }
            if (webComponentDesc.isAsyncSupported() != null) {
                wrapper.setIsAsyncSupported(webComponentDesc.isAsyncSupported());
            }

            if (webComponentDesc.getRunAsIdentity() != null) {
                wrapper.setRunAs(webComponentDesc.getRunAsIdentity().getRoleName());
            }

            for (String pattern : webComponentDesc.getUrlPatternsSet()) {
                webModule.addServletMapping(pattern,
                    webComponentDesc.getCanonicalName());
            }

            enumeration = webComponentDesc.getSecurityRoleReferences();
            while (enumeration.hasMoreElements()){
                securityRoleReference = (SecurityRoleReference)
                    enumeration.nextElement();
                wrapper.addSecurityReference(
                    securityRoleReference.getRoleName(),
                    securityRoleReference.getSecurityRoleLink().getName());
            }

            MultipartConfig mpConfig = webComponentDesc.getMultipartConfig();
            if (mpConfig != null) {
                wrapper.setMultipartLocation(mpConfig.getLocation());
                wrapper.setMultipartMaxFileSize(mpConfig.getMaxFileSize());
                wrapper.setMultipartMaxRequestSize(mpConfig.getMaxRequestSize());
                wrapper.setMultipartFileSizeThreshold(mpConfig.getFileSizeThreshold());
            }
        }
       
        SessionConfig sessionConfig = wmd.getSessionConfig();

        // <session-config><session-timeout>
        webModule.setSessionTimeout(sessionConfig.getSessionTimeout());

        // <session-config><cookie-config>
        CookieConfig cookieConfig = sessionConfig.getCookieConfig();
        if (cookieConfig != null) {
            SessionCookieConfig sessionCookieConfig =
                webModule.getSessionCookieConfig();
            /* 
             * Unlike a cookie's domain, path, and comment, its name
             * will be empty (instead of null) if left unspecified
             * inside <session-config><cookie-config>
             */
            if (cookieConfig.getName() != null &&
                    !cookieConfig.getName().isEmpty()) {
                sessionCookieConfig.setName(cookieConfig.getName());
            }
            sessionCookieConfig.setDomain(cookieConfig.getDomain());
            sessionCookieConfig.setPath(cookieConfig.getPath());
            sessionCookieConfig.setComment(cookieConfig.getComment());
            sessionCookieConfig.setHttpOnly(cookieConfig.isHttpOnly());
            sessionCookieConfig.setSecure(cookieConfig.isSecure());
            sessionCookieConfig.setMaxAge(cookieConfig.getMaxAge());
        }

        // <session-config><tracking-mode>
        if (!sessionConfig.getTrackingModes().isEmpty()) {
            webModule.setSessionTrackingModes(
                sessionConfig.getTrackingModes());
        }

        // glassfish-web.xml override the web.xml
        com.sun.enterprise.web.session.SessionCookieConfig gfSessionCookieConfig =
                webModule.getSessionCookieConfigFromSunWebXml();
        if (gfSessionCookieConfig != null) {
            WebSessionCookieConfig sessionCookieConfig =
                (WebSessionCookieConfig)webModule.getSessionCookieConfig();

            if (gfSessionCookieConfig.getName() != null &&
                    !gfSessionCookieConfig.getName().isEmpty()) {
                sessionCookieConfig.setName(gfSessionCookieConfig.getName());
            }

            if (gfSessionCookieConfig.getPath() != null) {
                sessionCookieConfig.setPath(gfSessionCookieConfig.getPath());
            }

            if (gfSessionCookieConfig.getMaxAge() != null) {
                sessionCookieConfig.setMaxAge(gfSessionCookieConfig.getMaxAge());
            }

            if (gfSessionCookieConfig.getDomain() != null) {
                sessionCookieConfig.setDomain(gfSessionCookieConfig.getDomain());
            }

            if (gfSessionCookieConfig.getComment() != null) {
                sessionCookieConfig.setComment(gfSessionCookieConfig.getComment());
            }

            if (gfSessionCookieConfig.getSecure() != null) {
                sessionCookieConfig.setSecure(gfSessionCookieConfig.getSecure());
            }

            if (gfSessionCookieConfig.getHttpOnly() != null) {
                sessionCookieConfig.setHttpOnly(gfSessionCookieConfig.getHttpOnly());
            }
        }

        enumeration = wmd.getWelcomeFiles();
        while (enumeration.hasMoreElements()){
            webModule.addWelcomeFile((String)enumeration.nextElement());
        }
        
        LocaleEncodingMappingListDescriptor lemds = 
                            wmd.getLocaleEncodingMappingListDescriptor();
        if (lemds != null) {
            for (LocaleEncodingMappingDescriptor lemd :
                    lemds.getLocaleEncodingMappingSet()) { 
                webModule.addLocaleEncodingMappingParameter(
                    lemd.getLocale(), lemd.getEncoding());
            }
        }

        webModule.setOrderedLibs(wmd.getOrderedLibs());

        String[] majorMinorVersions = wmd.getSpecVersion().split("\\.");
        if (majorMinorVersions.length != 2) {
            throw new IllegalArgumentException("Illegal Servlet spec version");
        }
        webModule.setEffectiveMajorVersion(
            Integer.parseInt(majorMinorVersions[0]));
        webModule.setEffectiveMinorVersion(
            Integer.parseInt(majorMinorVersions[1]));
    }

    
    /**
     * Configure security constraint element for a web application,
     * as represented by a <code>&lt;security-constraint&gt;</code> element in 
     * the deployment descriptor.    
     *
     * Configure a web resource collection for a web application's security
     * constraint, as represented by a
     * <code>&lt;web-resource-collection&gt;</code>
     * element in the deployment descriptor.
     *
     */
    protected static void configureSecurityConstraint(
            WebModule webModule, WebBundleDescriptor wmd) {
        Enumeration<com.sun.enterprise.deployment.web.SecurityConstraint> enumeration = wmd.getSecurityConstraints(); 
        com.sun.enterprise.deployment.web.SecurityConstraint securityConstraint;
        SecurityConstraintDecorator decorator;
        SecurityCollectionDecorator secCollDecorator;
        while (enumeration.hasMoreElements()){
            securityConstraint = enumeration.nextElement();
            decorator = new SecurityConstraintDecorator(securityConstraint,
                                                        webModule);
            for (WebResourceCollection wrc:
                    securityConstraint.getWebResourceCollections()) {
                secCollDecorator = new SecurityCollectionDecorator(wrc);
                decorator.addCollection(secCollDecorator);           
            }
            webModule.addConstraint(decorator);
        }                                        
    }
    
    
    /**
     * Validate the usage of security role names in the web application
     * deployment descriptor.  If any problems are found, issue warning
     * messages (for backwards compatibility) and add the missing roles.
     * (To make these problems fatal instead, simply set the <code>ok</code>
     * instance variable to <code>false</code> as well).
     */
    protected static void configureSecurityRoles(WebModule webModule,
                                                 WebBundleDescriptorImpl wmd) {

        Enumeration<SecurityRoleDescriptor> e = wmd.getSecurityRoles();
        if (e != null) {
            while (e.hasMoreElements()){
                webModule.addSecurityRole(e.nextElement().getName());
            }
        }

        // Check role names used in <security-constraint> elements
        Iterator<org.apache.catalina.deploy.SecurityConstraint> iter =
            webModule.getConstraints().iterator(); 
        while (iter.hasNext()) {
            String[] roles = iter.next().findAuthRoles();
            for (int j = 0; j < roles.length; j++) {
                if (!"*".equals(roles[j]) &&
                        !webModule.hasSecurityRole(roles[j])) {
                    logger.log(Level.WARNING,
                        ROLE_AUTH, roles[j]);
                    webModule.addSecurityRole(roles[j]);
                }
            }
        }

        // Check role names used in <servlet> elements
        Container wrappers[] = webModule.findChildren();
        for (int i = 0; i < wrappers.length; i++) {
            Wrapper wrapper = (Wrapper) wrappers[i];
            String runAs = wrapper.getRunAs();
            if ((runAs != null) && !webModule.hasSecurityRole(runAs)) {
                logger.log(Level.WARNING,
                    ROLE_RUNAS, runAs);
                webModule.addSecurityRole(runAs);
            }
            String names[] = wrapper.findSecurityReferences();
            for (int j = 0; j < names.length; j++) {
                String link = wrapper.findSecurityReference(names[j]);
                if ((link != null) && !webModule.hasSecurityRole(link)) {
                    logger.log(Level.WARNING,
                        ROLE_LINK, link);
                    webModule.addSecurityRole(link);
                }
            }
        }
    }
}
