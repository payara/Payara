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
 *
 *
 * This file incorporates work covered by the following copyright and
 * permission notice:
 *
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.catalina.startup;

import org.apache.catalina.*;
import org.apache.catalina.authenticator.*;
import org.apache.catalina.core.*;
import org.apache.catalina.deploy.FilterDef;
import org.apache.catalina.deploy.LoginConfig;
import org.apache.catalina.deploy.SecurityConstraint;
import org.apache.catalina.session.StandardManager;
import org.apache.tomcat.util.digester.Digester;
import org.apache.tomcat.util.digester.RuleSet;
import org.glassfish.logging.annotation.LogMessageInfo;
import org.glassfish.web.valve.GlassFishValve;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;

import javax.servlet.ServletContext;
import java.io.*;
import java.net.URL;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.*;
import java.util.logging.Logger;

/**
 * Startup event listener for a <b>Context</b> that configures the properties
 * of that Context, and the associated defined servlets.
 *
 * @author Craig R. McClanahan
 * @author Jean-Francois Arcand
 * @version $Revision: 1.14 $ $Date: 2007/02/20 20:16:56 $
 */

// START OF SJAS 8.0 BUG 5046959
// public final class ContextConfig
// NOTE: All the methods were originally private and changed to public.
public class ContextConfig
// END OF SJAS 8.0 BUG 5046959
    implements LifecycleListener {

    private static final Logger log = StandardServer.log;
    private static final ResourceBundle rb = log.getResourceBundle();

    @LogMessageInfo(
            message = "Lifecycle event data object {0} is not a Context",
            level = "WARNING"
    )
    public static final String EVENT_DATA_IS_NOT_CONTEXT_EXCEPTION = "AS-WEB-CORE-00408";

    @LogMessageInfo(
            message = "alt-dd file {0} not found",
            level = "WARNING"
    )
    public static final String ALT_DD_FILE_NOT_FOUND_EXCEPTION = "AS-WEB-CORE-00409";

    @LogMessageInfo(
            message = "Missing application web.xml, using defaults only {0}",
            level = "FINE"
    )
    public static final String MISSING_APP_WEB_XML_FINE = "AS-WEB-CORE-00410";

    @LogMessageInfo(
            message = "Parse error in application web.xml at line {0} and column {1}",
            level = "WARNING"
    )
    public static final String PARSE_ERROR_IN_APP_WEB_XML_EXCEPTION = "AS-WEB-CORE-00411";

    @LogMessageInfo(
            message = "Parse error in application web.xml",
            level = "WARNING"
    )
    public static final String PARSE_ERROR_IN_APP_WEB_XML = "AS-WEB-CORE-00412";

    @LogMessageInfo(
            message = "Error closing application web.xml",
            level = "SEVERE",
            cause = "Could not close this input stream and releases any system resources " +
                    "associated with the stream.",
            action = "Verify if any I/O errors occur"
    )
    public static final String ERROR_CLOSING_APP_WEB_XML_EXCEPTION = "AS-WEB-CORE-00413";

    @LogMessageInfo(
            message = "No Realm has been configured to authenticate against",
            level = "WARNING"
    )
    public static final String NO_REALM_BEEN_CONFIGURED_EXCEPTION = "AS-WEB-CORE-00414";

    @LogMessageInfo(
            message = "Cannot configure an authenticator for method {0}",
            level = "WARNING"
    )
    public static final String CANNOT_CONFIG_AUTHENTICATOR_EXCEPTION = "AS-WEB-CORE-00415";

    @LogMessageInfo(
            message = "Cannot instantiate an authenticator of class {0}",
            level = "WARNING"
    )
    public static final String CANNOT_INSTANTIATE_AUTHENTICATOR_EXCEPTION = "AS-WEB-CORE-00416";

    @LogMessageInfo(
            message = "Configured an authenticator for method {0}",
            level = "FINE"
    )
    public static final String CONFIGURED_AUTHENTICATOR_FINE = "AS-WEB-CORE-00417";

    @LogMessageInfo(
            message = "No default web.xml",
            level = "INFO"
    )
    public static final String NO_DEFAULT_WEB_XML_INFO = "AS-WEB-CORE-00418";

    @LogMessageInfo(
            message = "Missing default web.xml, using application web.xml only {0} {1}",
            level = "WARNING"
    )
    public static final String MISSING_DEFAULT_WEB_XML_EXCEPTION = "AS-WEB-CORE-00419";

    @LogMessageInfo(
            message = "Parse error in default web.xml at line {0} and column {1}",
            level = "SEVERE",
            cause = "Could not parse the content of the specified input source using this Digester",
            action = "Verify the input parameter, if any I/O errors occur"
    )
    public static final String PARSE_ERROR_IN_DEFAULT_WEB_XML_EXCEPTION = "AS-WEB-CORE-00420";

    @LogMessageInfo(
            message = "Parse error in default web.xml",
            level = "SEVERE",
            cause = "Could not parse the content of the specified input source using this Digester",
            action = "Verify the input parameter, if any I/O errors occur"
    )
    public static final String PARSE_ERROR_IN_DEFAULT_WEB_XML = "AS-WEB-CORE-00421";

    @LogMessageInfo(
            message = "Error closing default web.xml",
            level = "SEVERE",
            cause = "Could not close this input stream and releases any system resources " +
                    "associated with the stream.",
            action = "Verify if any I/O errors occur"
    )
    public static final String ERROR_CLOSING_DEFAULT_WEB_XML_EXCEPTION = "AS-WEB-CORE-00422";

    @LogMessageInfo(
            message = "ContextConfig: Initializing",
            level = "FINE"
    )
    public static final String CONTEXT_CONFIG_INIT_FINE = "AS-WEB-CORE-00423";

    @LogMessageInfo(
            message = "Exception fixing docBase",
            level = "SEVERE",
            cause = "Could not adjust docBase",
            action = "Verify if any I/O errors occur"
    )
    public static final String FIXING_DOC_BASE_EXCEPTION = "AS-WEB-CORE-00424";

    @LogMessageInfo(
            message = "ContextConfig: Processing START",
            level = "FINEST"
    )
    public static final String PROCESSING_START_FINEST = "AS-WEB-CORE-00425";

    @LogMessageInfo(
            message = "ContextConfig: Processing STOP",
            level = "FINEST"
    )
    public static final String PROCESSING_STOP_FINEST = "AS-WEB-CORE-00426";

    @LogMessageInfo(
            message = "Security role name {0} used in an <auth-constraint> without being defined in a <security-role> in context [{1}]",
            level = "INFO"
    )
    public static final String SECURITY_ROLE_NAME_USED_IN_AUTH_WITHOUT_DEFINITION = "AS-WEB-CORE-00427";

    @LogMessageInfo(
            message = "Security role name {0} used in a <run-as> without being defined in a <security-role> in context [{1}]",
            level = "INFO"
    )
    public static final String SECURITY_ROLE_NAME_USED_IN_RUNAS_WITHOUT_DEFINITION = "AS-WEB-CORE-00428";

    @LogMessageInfo(
            message = "Security role name {0} used in a <role-link> without being defined in a <security-role> in context [{1}]",
            level = "INFO"
    )
    public static final String SECURITY_ROLE_NAME_USED_IN_LINK_WITHOUT_DEFINITION = "AS-WEB-CORE-00429";

    @LogMessageInfo(
            message = "No web.xml, using defaults {0}",
            level = "INFO"
    )
    public static final String NO_WEB_XML_INFO = "AS-WEB-CORE-00430";
    // --------------------------------------------------- Instance Variables


    /*
     * Custom mappings of login methods to authenticators
     */
    //START SJSAS 6202703
    //private Map customAuthenticators;
    protected Map<String, Authenticator> customAuthenticators;
    //END SJSAS 6202703


    /**
     * The set of Authenticators that we know how to configure.  The key is
     * the name of the implemented authentication method, and the value is
     * the fully qualified Java class name of the corresponding Valve.
     */
    //START SJSAS 6202703
    //private static Properties authenticators = null;
    
    protected static final Properties authenticators = new Properties();
    //END SJSAS 6202703


    private ClassLoader classLoader;


    /**
     * The Context we are associated with.
     */
    protected Context context = null;


    /**
     * The debugging detail level for this component.
     */
    private int debug = 0;


    // START GlassFish 2439
    /**
     * The default web application's context file location.
     */
    protected String defaultContextXml = null;
    // END GlassFish 2439
    

    /**
     * The default web application's deployment descriptor location.
     */
    // BEGIN OF SJSAS 8.1 6172288  
    // private String defaultWebXml = null;
    protected String defaultWebXml = null;
    
    
    /**
     * Track any fatal errors during startup configuration processing.
     */
    // private boolean ok = false;
    protected boolean ok = false;
    // END OF SJSAS 8.1 6172288 


    // START GlassFish 2439
    /**
     * Any parse error which occurred while parsing XML descriptors.
     */
    protected SAXParseException parseException = null;
    // END GlassFish 2439 


    // START GlassFish 2439
    /**
     * The <code>Digester</code> we will use to process web application
     * context files.
     */
    protected static final Digester contextDigester =
        createContextDigester();
    // END GlassFish 2439


    /**
     * The <code>Digester</code> we will use to process web application
     * deployment descriptor files.
     */
    // BEGIN OF SJSAS 8.1 6172288  
    // private static Digester webDigester = null;
    protected static final Digester webDigester =
        createWebDigester();
    // END OF SJSAS 8.1 6172288  
    
    
    /**
     * The <code>Rule</code> used to parse the web.xml
     */
    // BEGIN OF SJSAS 8.1 6172288  
    // private static WebRuleSet webRuleSet = new WebRuleSet();
    protected static final WebRuleSet webRuleSet = new WebRuleSet();
    // END OF SJSAS 8.1 6172288  

    /**
     * Attribute value used to turn on/off XML validation
     */
    private static boolean xmlValidation = false;


    /**
     * Attribute value used to turn on/off XML namespace awarenes.
     */
    private static boolean xmlNamespaceAware = false;

        
    /**
     * Static initializer
     */
    static {
        authenticators.setProperty(
            "BASIC", BasicAuthenticator.class.getName());
        authenticators.setProperty(
            "CLIENT-CERT", SSLAuthenticator.class.getName());
        authenticators.setProperty(
            "FORM", FormAuthenticator.class.getName());
        authenticators.setProperty(
            "NONE", NonLoginAuthenticator.class.getName());
        authenticators.setProperty(
            "DIGEST", DigestAuthenticator.class.getName());
    }


    // ----------------------------------------------------------- Properties


    public void setClassLoader(ClassLoader cl) {
        this.classLoader = cl;
    }


    /**
     * Return the debugging detail level for this component.
     */
    public int getDebug() {

        return (this.debug);

    }


    /**
     * Set the debugging detail level for this component.
     *
     * @param debug The new debugging detail level
     */
    public void setDebug(int debug) {
        this.debug = debug;
    }
    
    
    // START GlassFish 2439
    /**
     * Return the location of the default deployment descriptor
     */
    public String getDefaultWebXml() {
        if( defaultWebXml == null ) defaultWebXml=Constants.DefaultWebXml;
        return (this.defaultWebXml);
    }


    /**
     * Set the location of the default deployment descriptor
     *
     * @param path Absolute/relative path to the default web.xml
     */
    public void setDefaultWebXml(String path) {
        this.defaultWebXml = path;
    }
    // END GlassFish 2439


    /**
     * Return the location of the default context file
     */
    public String getDefaultContextXml() {
        if( defaultContextXml == null ) {
            defaultContextXml=Constants.DEFAULT_CONTEXT_XML;
        }

        return (this.defaultContextXml);
    }


    /**
     * Set the location of the default context file
     *
     * @param path Absolute/relative path to the default context.xml
     */
    public void setDefaultContextXml(String path) {
        this.defaultContextXml = path;
    }


    /**
     * Sets custom mappings of login methods to authenticators.
     *
     * @param customAuthenticators Custom mappings of login methods to
     * authenticators
     */
    public void setCustomAuthenticators(Map<String, Authenticator> customAuthenticators) {
        this.customAuthenticators = customAuthenticators;
    }


    // --------------------------------------------------------- Public Methods


    /**
     * Process the START event for an associated Context.
     *
     * @param event The lifecycle event that has occurred
     */
    public void lifecycleEvent(LifecycleEvent event)
            throws LifecycleException {

        // Identify the context we are associated with
        try {
            context = (Context) event.getLifecycle();
        } catch (ClassCastException e) {
            String msg = MessageFormat.format(rb.getString(EVENT_DATA_IS_NOT_CONTEXT_EXCEPTION),
                                              event.getLifecycle());
            throw new LifecycleException(msg, e);
        }

        // Called from ContainerBase.addChild() -> StandardContext.start()
        // Process the event that has occurred
        if (event.getType().equals(Lifecycle.START_EVENT)) {
            start();
        } else if (event.getType().equals(Lifecycle.STOP_EVENT)) {
            stop();
        // START GlassFish 2439
        } else if (event.getType().equals(Lifecycle.INIT_EVENT)) {
            init();
        // END GlassFish 2439
        }
    }


    // -------------------------------------------------------- Private Methods


    /**
     * Process the application configuration file, if it exists.
     */
    protected void applicationConfig() throws LifecycleException {

        String altDDName = null;

        // Open the application web.xml file, if it exists
        InputStream stream = null;
        ServletContext servletContext = context.getServletContext();
        if (servletContext != null) {
            altDDName = (String)servletContext.getAttribute(
                    Globals.ALT_DD_ATTR);
            if (altDDName != null) {
                try {
                    stream = new FileInputStream(altDDName);
                } catch (FileNotFoundException e) {
                    String msg = MessageFormat.format(rb.getString(ALT_DD_FILE_NOT_FOUND_EXCEPTION),
                                                      altDDName);
                    throw new LifecycleException(msg);
                }
            }
            else {
                stream = servletContext.getResourceAsStream
                    (Constants.ApplicationWebXml);
            }
        }
        if (stream == null) {
            /* PWC 6296257
            log.info(sm.getString("contextConfig.applicationMissing") + " " + context);
            //contextConfig.applicationMissing=PWC3013: Missing application web.xml, using defaults only
            */
            // START PWC 6296257
            if (log.isLoggable(Level.FINE)) {
                log.log(Level.FINE, MISSING_APP_WEB_XML_FINE, context);
            }
            // END PWC 6296257
            return;
        }
        
        long t1=System.currentTimeMillis();

        URL url=null;
        // Process the application web.xml file
        synchronized (webDigester) {
            try {
                if (altDDName != null) {
                    url = new File(altDDName).toURL();
                } else {
                    url = servletContext.getResource(
                                                Constants.ApplicationWebXml);
                }
                if( url!=null ) {
                    InputSource is = new InputSource(url.toExternalForm());
                    is.setByteStream(stream);
                    webDigester.clear();
                    webDigester.setDebug(getDebug());
                    if (context instanceof StandardContext) {
                        ((StandardContext) context).setReplaceWelcomeFiles(true);
                    }
                    webDigester.setUseContextClassLoader(false);
                    webDigester.push(context);
                    webDigester.parse(is);
                } else {
                    if (log.isLoggable(Level.INFO)) {
                        log.log(Level.INFO, NO_WEB_XML_INFO);
                    }
                }
            } catch (SAXParseException e) {
                String msg = MessageFormat.format(rb.getString(PARSE_ERROR_IN_APP_WEB_XML_EXCEPTION),
                                                  new Object[] {e.getLineNumber(), e.getColumnNumber()});
                throw new LifecycleException(msg, e);
            } catch (Exception e) {
                throw new LifecycleException(rb.getString(PARSE_ERROR_IN_APP_WEB_XML), e);
            } finally {
                try {
                    stream.close();
                } catch (IOException e) {
                    log.log(Level.SEVERE, ERROR_CLOSING_APP_WEB_XML_EXCEPTION,
                            e);
                }
                webDigester.push(null);
            }
        }
        webRuleSet.recycle();

        long t2=System.currentTimeMillis();
        if (context instanceof StandardContext) {
            ((StandardContext) context).setStartupTime(t2-t1);
        }
    }


    /**
     * Set up a manager.
     */
    protected synchronized void managerConfig() {
        if (context.getManager() == null) {
            context.setManager(new StandardManager());
        }
    }


    /**
     * Set up an Authenticator automatically if required, and one has not
     * already been configured.
     */
    protected synchronized void authenticatorConfig()
            throws LifecycleException {

        // Does this Context require an Authenticator?
        /* START IASRI 4856062
           // This constraints check is relocated to happen after
           // setRealmName(). This allows apps which have no constraints
           // and no authenticator to still have a realm name set in
           // their RealmAdapater. This is only relevant in the case where
           // the core ACLs are doing all access control AND the servlet
           // wishes to call isUserInRole AND the application does have
           // security-role-mapping elements in sun-web.xml. This is probably
           // not an interesting scenario. But might as well allow it to
           // work, maybe it is of some use.
        SecurityConstraint constraints[] = context.findConstraints();
        if ((constraints == null) || (constraints.length == 0))
            return;
        */
        LoginConfig loginConfig = context.getLoginConfig();
        if (loginConfig == null) {
            loginConfig = new LoginConfig("NONE", null, null, null);
            context.setLoginConfig(loginConfig);
        }

        // Has an authenticator been configured already?
        if (context instanceof Authenticator)
            return;
        if (context instanceof ContainerBase) {
            Pipeline pipeline = ((ContainerBase) context).getPipeline();
            if (pipeline != null) {
                GlassFishValve basic = pipeline.getBasic();
                if ((basic != null) && (basic instanceof Authenticator))
                    return;
                GlassFishValve valves[] = pipeline.getValves();
                for (int i = 0; i < valves.length; i++) {
                    if (valves[i] instanceof Authenticator)
                        return;
                }
            }
        } else {
            return;     // Cannot install a Valve even if it would be needed
        }

        // Has a Realm been configured for us to authenticate against?
        /* START IASRI 4856062
        if (context.getRealm() == null) {
        */
        // BEGIN IASRI 4856062
        Realm rlm = context.getRealm();
        if (rlm == null) {
        // END IASRI 4856062
            throw new LifecycleException(rb.getString(NO_REALM_BEEN_CONFIGURED_EXCEPTION));
        }

        // BEGIN IASRI 4856062
        // If a realm is available set its name in the Realm(Adapter)
        rlm.setRealmName(loginConfig.getRealmName(),
                         loginConfig.getAuthMethod());
        if (!context.hasConstraints()) {
            return;
        }
        // END IASRI 4856062

        /*
         * First check to see if there is a custom mapping for the login
         * method. If so, use it. Otherwise, check if there is a mapping in
         * org/apache/catalina/startup/Authenticators.properties.
         */
        GlassFishValve authenticator = null;
        if (customAuthenticators != null) {
            /* PWC 6392537
            authenticator = (Valve)
                customAuthenticators.get(loginConfig.getAuthMethod());
            */
            // START PWC 6392537
            String loginMethod = loginConfig.getAuthMethod();
            if (loginMethod != null
                    && customAuthenticators.containsKey(loginMethod)) {
                authenticator = getGlassFishValveAuthenticator(loginMethod);
                if (authenticator == null) {
                    String msg = MessageFormat.format(rb.getString(CANNOT_CONFIG_AUTHENTICATOR_EXCEPTION),
                                                      loginMethod);
                    throw new LifecycleException(msg);
                }
            }
            // END PWC 6392537
        }

        if (authenticator == null) {
            // Identify the class name of the Valve we should configure
            String authenticatorName = null;

            // BEGIN RIMOD 4808402
            // If login-config is given but auth-method is null, use NONE
            // so that NonLoginAuthenticator is picked
            String authMethod = loginConfig.getAuthMethod();
            if (authMethod == null) {
                authMethod = "NONE";
            }
            authenticatorName = authenticators.getProperty(authMethod);
            // END RIMOD 4808402
            /* RIMOD 4808402
            authenticatorName =
                    authenticators.getProperty(loginConfig.getAuthMethod());
            */

            if (authenticatorName == null) {
                String msg = MessageFormat.format(rb.getString(CANNOT_CONFIG_AUTHENTICATOR_EXCEPTION),
                                                  loginConfig.getAuthMethod());
                throw new LifecycleException(msg);
            }

            // Instantiate and install an Authenticator of the requested class
            try {
                Class authenticatorClass = Class.forName(authenticatorName);
                authenticator = (GlassFishValve) authenticatorClass.newInstance();
            } catch (Throwable t) {
                String msg = MessageFormat.format(rb.getString(CANNOT_INSTANTIATE_AUTHENTICATOR_EXCEPTION),
                                                  authenticatorName);
                throw new LifecycleException(msg, t);
            }
        }

        if (authenticator != null && context instanceof ContainerBase) {
            Pipeline pipeline = ((ContainerBase) context).getPipeline();
            if (pipeline != null) {
                ((ContainerBase) context).addValve(authenticator);
                if (log.isLoggable(Level.FINE)) {
                    log.log(Level.FINE, CONFIGURED_AUTHENTICATOR_FINE, loginConfig.getAuthMethod());
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private GlassFishValve getGlassFishValveAuthenticator(String loginMethod) {
        return (GlassFishValve) customAuthenticators.get(loginMethod);
    }

    /**
     * Create and return a Digester configured to process the
     * web application deployment descriptor (web.xml).
     */
    // BEGIN OF SJSAS 8.1 6172288  
    // private static Digester createWebDigester() {
    public static Digester createWebDigester() {
    // END OF SJSAS 8.1 6172288  
        return createWebXmlDigester(xmlNamespaceAware, xmlValidation);
    }


    /*
     * Create and return a Digester configured to process the
     * web application deployment descriptor (web.xml).
     */
    public static Digester createWebXmlDigester(boolean namespaceAware,
                                                boolean validation) {
        DigesterFactory df = org.glassfish.internal.api.Globals.get(
            DigesterFactory.class);
        Digester digester = df.newDigester(xmlValidation,
            xmlNamespaceAware, webRuleSet);
        digester.getParser();
        return digester;
    }


    // START GlassFish 2439 
    /**
     * Create and return a Digester configured to process the
     * context configuration descriptor for an application.
     */
    protected static Digester createContextDigester() {
        Digester digester = new Digester();
        digester.setValidating(false);
        RuleSet contextRuleSet = new ContextRuleSet("", false);
        digester.addRuleSet(contextRuleSet);
        RuleSet namingRuleSet = new NamingRuleSet("Context/");
        digester.addRuleSet(namingRuleSet);
        digester.getParser();
        return digester;
    }
    // END GlassFish 2439


    protected String getBaseDir() {
        Container engineC=context.getParent().getParent();
        if( engineC instanceof StandardEngine ) {
            return ((StandardEngine)engineC).getBaseDir();
        }
        return System.getProperty("catalina.base");
    }

    /**
     * Process the default configuration file, if it exists.
     * The default config must be read with the container loader - so
     * container servlets can be loaded
     */
    protected void defaultConfig() throws LifecycleException {
        long t1 = System.currentTimeMillis();

        // Open the default web.xml file, if it exists
        if (defaultWebXml == null && context instanceof StandardContext) {
            defaultWebXml=((StandardContext)context).getDefaultWebXml();
        }
        // set the default if we don't have any overrides
        if (defaultWebXml == null) getDefaultWebXml();

        File file = new File(this.defaultWebXml);
        if (!file.isAbsolute()) {
            file = new File(getBaseDir(), this.defaultWebXml);
        }
        
        InputStream stream = null;
        InputSource source = null;

        try {
            if ( ! file.exists() ) {
                // Use getResource and getResourceAsStream
                stream = getClass().getClassLoader()
                    .getResourceAsStream(defaultWebXml);
                if( stream != null ) {
                    source = new InputSource
                            (getClass().getClassLoader()
                            .getResource(defaultWebXml).toString());
                } 

                if (stream == null) { 
                    // maybe embedded
                    stream = getClass().getClassLoader()
                        .getResourceAsStream("web-embed.xml");
                    if( stream != null ) {
                        source = new InputSource
                        (getClass().getClassLoader()
                                .getResource("web-embed.xml").toString());
                    }                                         
                }

                if( stream== null ) {
                    if (log.isLoggable(Level.INFO)) {
                        log.log(Level.INFO, NO_DEFAULT_WEB_XML_INFO);
                    }
                    // no default web.xml
                    return;
                }
            } else {
                source =
                    new InputSource("file://" + file.getAbsolutePath());
                stream = new FileInputStream(file);
            }
        } catch (Exception e) {
            String msg = MessageFormat.format(rb.getString(MISSING_DEFAULT_WEB_XML_EXCEPTION),
                                              new Object[] {defaultWebXml, file});
            throw new LifecycleException(msg, e);
        }

        // Process the default web.xml file
        synchronized (webDigester) {
            try {
                source.setByteStream(stream);
                webDigester.setDebug(getDebug());
                // JFA
                if (context instanceof StandardContext)
                    ((StandardContext) context).setReplaceWelcomeFiles(true);
                webDigester.clear();
                webDigester.setClassLoader(this.getClass().getClassLoader());
                webDigester.setUseContextClassLoader(false);
                webDigester.push(context);
                webDigester.parse(source);
            } catch (SAXParseException e) {
                String msg = MessageFormat.format(rb.getString(PARSE_ERROR_IN_DEFAULT_WEB_XML_EXCEPTION),
                                                  new Object[] {e.getLineNumber(), e.getColumnNumber()});
                throw new LifecycleException(msg, e);
            } catch (Exception e) {
                throw new LifecycleException(rb.getString(PARSE_ERROR_IN_DEFAULT_WEB_XML), e);
            } finally {
                try {
                    if (stream != null) {
                        stream.close();
                    }
                } catch (IOException e) {
                    log.log(Level.SEVERE, ERROR_CLOSING_DEFAULT_WEB_XML_EXCEPTION, e);
                }
            }
        }
        webRuleSet.recycle();
        
        long t2=System.currentTimeMillis();
        if( (t2-t1) > 200 && log.isLoggable(Level.FINE) )
            log.log(Level.FINE, "Processed default web.xml " + file + " "  + ( t2-t1));
    }


    // START GlassFish 2439
    /**
     * Process the default configuration file, if it exists.
     */
    protected void contextConfig() {

        if( defaultContextXml==null ) getDefaultContextXml();

        if (!context.getOverride()) {
            processContextConfig(new File(getBaseDir()), defaultContextXml);
        }

        if (context.getConfigFile() != null)
            processContextConfig(new File(context.getConfigFile()), null);

    }


    /**
     * Process a context.xml.
     */
    protected void processContextConfig(File baseDir, String resourceName) {
        if (log.isLoggable(Level.FINE))
            log.log(Level.FINE, "Processing context [" + context.getName() +
                    "] configuration file " + baseDir + " " + resourceName);

        InputSource source = null;
        InputStream stream = null;

        File file = baseDir;
        if (resourceName != null) {
            file = new File(baseDir, resourceName);
        }
        try {
            if ( !file.exists() ) {
                if (resourceName != null) {
                    // Use getResource and getResourceAsStream
                    stream = getClass().getClassLoader()
                        .getResourceAsStream(resourceName);
                    if( stream != null ) {
                        source = new InputSource
                            (getClass().getClassLoader()
                            .getResource(resourceName).toString());
                    }
                }
            } else {
                source =
                    new InputSource("file://" + file.getAbsolutePath());
                stream = new FileInputStream(file);
                // Add as watched resource so that cascade reload occurs if a default
                // config file is modified/added/removed
                context.addWatchedResource(file.getAbsolutePath());
            }
        } catch (Exception e) {
            String msg = MessageFormat.format(rb.getString(MISSING_DEFAULT_WEB_XML_EXCEPTION),
                                              new Object[] {resourceName, file});
            log.log(Level.SEVERE, msg, e);
        }

        if (source == null) {
            if (stream != null) {
                try {
                    stream.close();
                } catch(IOException e) {
                    log.log(Level.SEVERE, ERROR_CLOSING_DEFAULT_WEB_XML_EXCEPTION, e);
                }
            }

            return;
        }

        synchronized (contextDigester) {
            try {
                source.setByteStream(stream);
                contextDigester.setClassLoader(classLoader);
                contextDigester.setUseContextClassLoader(false);
                contextDigester.push(context.getParent());
                contextDigester.push(context);
                contextDigester.setErrorHandler(new ContextErrorHandler());
                contextDigester.parse(source);
                if (parseException != null) {
                    ok = false;
                }
                if (log.isLoggable(Level.FINE))
                    log.log(Level.FINE, "Successfully processed context [" +
                            context.getName() + "] configuration file " +
                            baseDir + " " + resourceName);
            } catch (SAXParseException e) {
                String msg = MessageFormat.format(rb.getString(PARSE_ERROR_IN_DEFAULT_WEB_XML_EXCEPTION),
                                                  new Object[] {e.getLineNumber(), e.getColumnNumber()});
                log.log(Level.SEVERE, PARSE_ERROR_IN_DEFAULT_WEB_XML, e);
                log.log(Level.SEVERE, msg);
                ok = false;
            } catch (Exception e) {
                log.log(Level.SEVERE, PARSE_ERROR_IN_DEFAULT_WEB_XML, e);
                ok = false;
            } finally {
                //contextDigester.reset();
                parseException = null;
                try {
                    if (stream != null) {
                        stream.close();
                    }
                } catch (IOException e) {
                    log.log(Level.SEVERE, ERROR_CLOSING_DEFAULT_WEB_XML_EXCEPTION, e);
                }
            }
        }

    }
    // END GlassFish 2439

        
    /**
     * Adjust docBase.
     */
    protected void fixDocBase()
        throws IOException {
        
        Host host = (Host) context.getParent();
        String appBase = host.getAppBase();
        
        boolean unpackWARs = true;
        if (host instanceof StandardHost) {
            unpackWARs = ((StandardHost) host).isUnpackWARs() 
                && ((StandardContext) context).getUnpackWAR();
        }

        File canonicalAppBase = null;
        if (appBase != null) {
            canonicalAppBase = new File(appBase);
            if (canonicalAppBase.isAbsolute()) {
                canonicalAppBase = canonicalAppBase.getCanonicalFile();
            } else {
                canonicalAppBase = 
                    new File(System.getProperty("catalina.base"), appBase)
                    .getCanonicalFile();
            }
        }

        String docBase = context.getDocBase();
        if (docBase == null) {
            // Trying to guess the docBase according to the path
            String path = context.getPath();
            if (path == null) {
                return;
            }
            if (path.equals("")) {
                docBase = "ROOT";
            } else {
                if (path.startsWith("/")) {
                    docBase = path.substring(1).replace('/', '#');
                } else {
                    docBase = path.replace('/', '#');
                }
            }
        }

        File file = new File(docBase);
        if (!file.isAbsolute() && canonicalAppBase != null) {
            docBase = (new File(canonicalAppBase, docBase)).getPath();
        } else {
            docBase = file.getCanonicalPath();
        }
        file = new File(docBase);
        String origDocBase = docBase;
        
        String pathName = context.getPath();
        if (pathName.equals("")) {
            pathName = "ROOT";
        } else {
            // Context path must start with '/'
            pathName = pathName.substring(1).replace('/','#');
        }
        if (docBase.toLowerCase(Locale.ENGLISH).endsWith(".war") && !file.isDirectory() && unpackWARs) {
            URL war = new URL("jar:" + (new File(docBase)).toURI().toURL() + "!/");
            docBase = ExpandWar.expand(host, war, pathName);
            file = new File(docBase);
            docBase = file.getCanonicalPath();
            if (context instanceof StandardContext) {
                ((StandardContext) context).setOriginalDocBase(origDocBase);
            }
        } else if (docBase.toLowerCase(Locale.ENGLISH).endsWith(".war") &&
                !file.isDirectory() && !unpackWARs) {
            URL war =
                new URL("jar:" + (new File (docBase)).toURI().toURL() + "!/");
            ExpandWar.validate(host, war, pathName);
        } else {
            File docDir = new File(docBase);
            if (!docDir.exists()) {
                File warFile = new File(docBase + ".war");
                if (warFile.exists()) {
                    URL war =
                        new URL("jar:" + warFile.toURI().toURL() + "!/");
                    if (unpackWARs) {
                        docBase = ExpandWar.expand(host, war, pathName);
                        file = new File(docBase);
                        docBase = file.getCanonicalPath();
                    } else {
                        docBase = warFile.getCanonicalPath();
                        ExpandWar.validate(host, war, pathName);
                    }
                }
                if (context instanceof StandardContext) {
                    ((StandardContext) context).setOriginalDocBase(origDocBase);
                }
            }
        }

        if (canonicalAppBase != null &&
                docBase.startsWith(canonicalAppBase.getPath())) {
            docBase = docBase.substring(canonicalAppBase.getPath().length());
            docBase = docBase.replace(File.separatorChar, '/');
            if (docBase.startsWith("/")) {
                docBase = docBase.substring(1);
            }
        } else {
            docBase = docBase.replace(File.separatorChar, '/');
        }

        context.setDocBase(docBase);

    }


    /**
     * Log a message on the Logger associated with our Context (if any)
     *
     * @param message Message to be logged
     *
    private void log(String message) {

        org.apache.catalina.Logger logger = null;
        if (context != null)
            logger = context.getLogger();
        if (logger != null)
            logger.log("ContextConfig[" + context.getName() + "]: " + message);
        else
            log.info( message );
    }


    /**
     * Log a message on the Logger associated with our Context (if any)
     *
     * @param message Message to be logged
     * @param throwable Associated exception
     *
    private void log(String message, Throwable throwable) {

        org.apache.catalina.Logger logger = null;
        if (context != null)
            logger = context.getLogger();
        if (logger != null)
            logger.log("ContextConfig[" + context.getName() + "] "
                       + message, throwable);
        else {
            log.log(Level.SEVERE, message, throwable );
        }
    }
    */


    
    // START GlassFish 2439
    /**
     * Process a "init" event for this Context.
     */
    protected void init() {
        // Called from StandardContext.init()

        if (log.isLoggable(Level.FINE))
            log.log(Level.FINE, CONTEXT_CONFIG_INIT_FINE);
        context.setConfigured(false);
        ok = true;

        contextConfig();
        
        try {
            fixDocBase();
        } catch (IOException e) {
            log.log(Level.SEVERE, FIXING_DOC_BASE_EXCEPTION, e);
        }


    }
    // END GlassFish 2439


    /**
     * Process a "start" event for this Context - in background
     */
    protected synchronized void start() throws LifecycleException {
        if (log.isLoggable(Level.FINEST)) {
            log.log(Level.FINEST, PROCESSING_START_FINEST);
        }

        context.setConfigured(false);

        // Set properties based on DefaultContext
        Container container = context.getParent();
        if( !context.getOverride() ) {
            if( container instanceof Host ) {
             
                // Reset the value only if the attribute wasn't
                // set on the context.
                xmlValidation = context.getXmlValidation();
                if (!xmlValidation) {
                    xmlValidation = ((Host)container).getXmlValidation();
                }
                
                xmlNamespaceAware = context.getXmlNamespaceAware();
                if (!xmlNamespaceAware){
                    xmlNamespaceAware 
                                = ((Host)container).getXmlNamespaceAware();
                }

            }
        }

        // Process the default and application web.xml files
        defaultConfig();
        applicationConfig();
        validateSecurityRoles();

        // Configure an authenticator if we need one
        authenticatorConfig();

        // Configure a manager
        managerConfig();

        // Dump the contents of this pipeline if requested
        if ((log.isLoggable(Level.FINEST)) &&
                (context instanceof ContainerBase)) {
            log.log(Level.FINEST, "Pipline Configuration:");
            Pipeline pipeline = ((ContainerBase) context).getPipeline();
            GlassFishValve valves[] = null;
            if (pipeline != null)
                valves = pipeline.getValves();
            if (valves != null) {
                for (int i = 0; i < valves.length; i++) {
                    log.log(Level.FINEST, "  " + valves[i].getInfo());
                }
            }
            log.log(Level.FINEST, "======================");
        }

        // Make our application available because no problems
        // were encountered
        context.setConfigured(true);
    }


    /**
     * Process a "stop" event for this Context.
     */
    protected synchronized void stop() {

        if (log.isLoggable(Level.FINEST))
            log.log(Level.FINEST, PROCESSING_STOP_FINEST);

        int i;

        // Removing children
        Container[] children = context.findChildren();
        for (i = 0; i < children.length; i++) {
            context.removeChild(children[i]);
        }

        // Removing application parameters
/*        ApplicationParameter[] applicationParameters =
            context.findApplicationParameters();
        for (i = 0; i < applicationParameters.length; i++) {
            context.removeApplicationParameter
                (applicationParameters[i].getName());
        }
*/
        // Removing security constraints
        context.removeConstraints();

        // Removing Ejbs
        /*
        ContextEjb[] contextEjbs = context.findEjbs();
        for (i = 0; i < contextEjbs.length; i++) {
            context.removeEjb(contextEjbs[i].getName());
        }
        */

        // Removing environments
        /*
        ContextEnvironment[] contextEnvironments = context.findEnvironments();
        for (i = 0; i < contextEnvironments.length; i++) {
            context.removeEnvironment(contextEnvironments[i].getName());
        }
        */

        // Removing errors pages
        context.removeErrorPages();

        // Removing filter defs
        FilterDef[] filterDefs = context.findFilterDefs();
        for (i = 0; i < filterDefs.length; i++) {
            context.removeFilterDef(filterDefs[i]);
        }

        // Removing filter maps
        context.removeFilterMaps();

        // Removing local ejbs
        /*
        ContextLocalEjb[] contextLocalEjbs = context.findLocalEjbs();
        for (i = 0; i < contextLocalEjbs.length; i++) {
            context.removeLocalEjb(contextLocalEjbs[i].getName());
        }
        */

        // Removing Mime mappings
        String[] mimeMappings = context.findMimeMappings();
        for (i = 0; i < mimeMappings.length; i++) {
            context.removeMimeMapping(mimeMappings[i]);
        }

        // Removing parameters
        String[] parameters = context.findParameters();
        for (i = 0; i < parameters.length; i++) {
            context.removeParameter(parameters[i]);
        }

        // Removing resource env refs
        /*
        String[] resourceEnvRefs = context.findResourceEnvRefs();
        for (i = 0; i < resourceEnvRefs.length; i++) {
            context.removeResourceEnvRef(resourceEnvRefs[i]);
        }
        */

        // Removing resource links
        /*
        ContextResourceLink[] contextResourceLinks =
            context.findResourceLinks();
        for (i = 0; i < contextResourceLinks.length; i++) {
            context.removeResourceLink(contextResourceLinks[i].getName());
        }
        */

        // Removing resources
        /*
        ContextResource[] contextResources = context.findResources();
        for (i = 0; i < contextResources.length; i++) {
            context.removeResource(contextResources[i].getName());
        }
        */

        // Removing sercurity role
        context.removeSecurityRoles();

        // Removing servlet mappings
        String[] servletMappings = context.findServletMappings();
        for (i = 0; i < servletMappings.length; i++) {
            context.removeServletMapping(servletMappings[i]);
        }

        // FIXME : Removing status pages

        // Removing welcome files
        context.removeWelcomeFiles();

        // Removing wrapper lifecycles
        context.removeWrapperLifecycles();

        // Removing wrapper listeners
        context.removeWrapperListeners();

        ok = true;

    }

    /**
     * Validate the usage of security role names in the web application
     * deployment descriptor.  If any problems are found, issue warning
     * messages (for backwards compatibility) and add the missing roles.
     * (To make these problems fatal instead, simply set the <code>ok</code>
     * instance variable to <code>false</code> as well).
     */
    protected void validateSecurityRoles() {

        // Check role names used in <security-constraint> elements
        Iterator<SecurityConstraint> iter =
            context.getConstraints().iterator(); 
        while (iter.hasNext()) {
            for (String role : iter.next().findAuthRoles()) {
                if (!"*".equals(role) &&
                        !context.hasSecurityRole(role)) {
                    if (log.isLoggable(Level.INFO)) {
                        log.log(Level.INFO, SECURITY_ROLE_NAME_USED_IN_AUTH_WITHOUT_DEFINITION,
                                new Object[] {role, context.getName()});
                    }
                    context.addSecurityRole(role);
                }
            }
        }

        // Check role names used in <servlet> elements
        Container wrappers[] = context.findChildren();
        for (int i = 0; i < wrappers.length; i++) {
            Wrapper wrapper = (Wrapper) wrappers[i];
            String runAs = wrapper.getRunAs();
            if ((runAs != null) && !context.hasSecurityRole(runAs)) {
                if (log.isLoggable(Level.INFO)) {
                    log.log(Level.INFO, SECURITY_ROLE_NAME_USED_IN_RUNAS_WITHOUT_DEFINITION,
                            new Object[] {runAs, context.getName()});
                }
                context.addSecurityRole(runAs);
            }
            String names[] = wrapper.findSecurityReferences();
            for (int j = 0; j < names.length; j++) {
                String link = wrapper.findSecurityReference(names[j]);
                if ((link != null) && !context.hasSecurityRole(link)) {
                    if (log.isLoggable(Level.INFO)) {
                        log.log(Level.INFO, SECURITY_ROLE_NAME_USED_IN_LINK_WITHOUT_DEFINITION,
                                new Object[] {link, context.getName()});
                    }
                    context.addSecurityRole(link);
                }
            }
        }

    }


    // START GlassFish 2439
    protected class ContextErrorHandler
        implements ErrorHandler {

        public void error(SAXParseException exception) {
            parseException = exception;
        }

        public void fatalError(SAXParseException exception) {
            parseException = exception;
        }

        public void warning(SAXParseException exception) {
            parseException = exception;
        }

    }
    // END GlassFish 2439


} //end of public class
