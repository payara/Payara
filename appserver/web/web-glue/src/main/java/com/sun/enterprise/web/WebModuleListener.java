/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2016 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.appserv.web.cache.CacheManager;
import com.sun.enterprise.container.common.spi.JCDIService;
import com.sun.enterprise.deployment.WebBundleDescriptor;
import com.sun.enterprise.deployment.WebComponentDescriptor;
import com.sun.enterprise.deployment.runtime.web.SunWebApp;
import com.sun.enterprise.deployment.web.InitializationParameter;
import com.sun.enterprise.util.net.JarURIPattern;
import com.sun.enterprise.web.jsp.JspProbeEmitterImpl;
import com.sun.enterprise.web.jsp.ResourceInjectorImpl;
import org.apache.catalina.*;
import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.api.web.TldProvider;
import org.glassfish.web.LogFacade;
import org.glassfish.web.deployment.runtime.WebProperty;
import org.glassfish.web.deployment.runtime.SunWebAppImpl;
import org.glassfish.web.deployment.util.WebValidatorWithCL;
import org.glassfish.loader.util.ASClassLoaderUtil;
import org.glassfish.hk2.api.ServiceLocator;

import javax.naming.NamingException;
import javax.servlet.ServletContext;
import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.StringTokenizer;
import java.util.regex.Pattern;
//import com.sun.enterprise.server.PersistenceUnitLoaderImpl;
//import com.sun.enterprise.server.PersistenceUnitLoader;
//import com.sun.enterprise.config.ConfigException;

/**
 * Startup event listener for a <b>Context</b> that configures the properties
 * of that Jsp Servlet from sun-web.xml
 */

final class WebModuleListener
    implements LifecycleListener {

    /**
     * The logger used to log messages
     */
    private static final Logger _logger = LogFacade.getLogger();

    /**
     * Descriptor object associated with this web application.
     * Used for loading persistence units.
     */
    private WebBundleDescriptor wbd;
    
    private WebContainer webContainer;

    /**
     * Constructor.
     *
     * @param webContainer
     * @param explodedLocation The location where this web module is exploded
     * @param wbd descriptor for this module.
     */
    public WebModuleListener(WebContainer webContainer,
                             WebBundleDescriptor wbd) {
        this.webContainer = webContainer;
        this.wbd = wbd;
    }


    /**
     * Process the START event for an associated WebModule
     * @param event The lifecycle event that has occurred
     */
    public void lifecycleEvent(LifecycleEvent event) {

        // Identify the context we are associated with
        WebModule webModule;
        try {
            webModule = (WebModule) event.getLifecycle();
        } catch (ClassCastException e) {
            _logger.log(Level.WARNING, LogFacade.CLASS_CAST_EXCEPTION, event.getLifecycle());
            return;
        }

        // Process the event that has occurred
        if (event.getType().equals(Lifecycle.START_EVENT)) {
            // post processing DOL object for standalone web module
            if (wbd != null && wbd.getApplication() != null && 
                wbd.getApplication().isVirtual()) {
                wbd.setClassLoader(webModule.getLoader().getClassLoader());
                wbd.visit(new WebValidatorWithCL());
            }
            
            //loadPersistenceUnits(webModule);
            configureDefaultServlet(webModule);
            configureJsp(webModule);
            startCacheManager(webModule);
        } else if (event.getType().equals(Lifecycle.STOP_EVENT)) {
            //unloadPersistenceUnits(webModule);
            stopCacheManager(webModule);
        }
    }


    //------------------------------------------------------- Private Methods

    /**
     * Configure all JSP related aspects of the web module, including
     * any relevant TLDs as well as the jsp config settings of the
     * JspServlet (using the values from sun-web.xml's jsp-config).
     */
    private void configureJsp(WebModule webModule) {

        ServletContext servletContext = webModule.getServletContext();
        servletContext.setAttribute(
            "org.glassfish.jsp.isStandaloneWebapp",
            Boolean.valueOf(webModule.isStandalone()));

        // Find tld URI and set it to ServletContext attribute
        List<URI> appLibUris = webModule.getDeployAppLibs();
        Map<URI, List<String>> appLibTldMap = new HashMap<URI, List<String>>();
        if (appLibUris != null && appLibUris.size() > 0) {
            Pattern pattern = Pattern.compile("META-INF/.*\\.tld");
            for (URI uri : appLibUris) {
                List<String> entries =  JarURIPattern.getJarEntries(uri, pattern);
                if (entries != null && entries.size() > 0) {
                    appLibTldMap.put(uri, entries);
                }
            }
        }

        Collection<TldProvider> tldProviders =
            webContainer.getTldProviders();
        Map<URI, List<String>> tldMap = new HashMap<URI, List<String>>();
        for (TldProvider tldProvider : tldProviders) {
            // Skip any JSF related TLDs for non-JSF apps
            if ("jsfTld".equals(tldProvider.getName()) &&
                    !webModule.isJsfApplication()) {
                continue;
            }
            Map<URI, List<String>> tmap = tldProvider.getTldMap();
            if (tmap != null) {
                tldMap.putAll(tmap);
            }
        }
        tldMap.putAll(appLibTldMap);
        servletContext.setAttribute(
                "com.sun.appserv.tld.map", tldMap);

        /*
         * Discover all TLDs that are known to contain listener
         * declarations, and store the resulting map as a
         * ServletContext attribute
         */
        Map<URI, List<String>> tldListenerMap =
            new HashMap<URI, List<String>>();
        for (TldProvider tldProvider : tldProviders) {
            // Skip any JSF related TLDs for non-JSF apps
            if ("jsfTld".equals(tldProvider.getName()) &&
                    !webModule.isJsfApplication()) {
                continue;
            }
            Map<URI, List<String>> tmap = tldProvider.getTldListenerMap();
            if (tmap != null) {
                tldListenerMap.putAll(tmap);
            }
        }
        tldListenerMap.putAll(appLibTldMap);
        servletContext.setAttribute(
            "com.sun.appserv.tldlistener.map", tldListenerMap);

        ServiceLocator defaultServices =
                webContainer.getServerContext().getDefaultServices();

        // set services for jsf injection
        servletContext.setAttribute(
                Constants.HABITAT_ATTRIBUTE, defaultServices);

        SunWebAppImpl bean = webModule.getIasWebAppConfigBean();

        // Find the default jsp servlet
        Wrapper wrapper = (Wrapper) webModule.findChild(
            org.apache.catalina.core.Constants.JSP_SERVLET_NAME);
        if (wrapper == null) {
            return;
        }

        if (webModule.getTldValidation()) {
            wrapper.addInitParameter("enableTldValidation", "true");
        }
        if (bean != null && bean.getJspConfig()  != null) {
            WebProperty[]  props = bean.getJspConfig().getWebProperty();
            for (int i = 0; i < props.length; i++) {
                String pname = props[i].getAttributeValue("name");
                String pvalue = props[i].getAttributeValue("value");
                if (_logger.isLoggable(Level.FINE)) {
                    _logger.log(Level.FINE,
                            LogFacade.JSP_CONFIG_PROPERTY,
                            "[" + webModule.getID() + "] is [" + pname + "] = [" + pvalue + "]");
                }
                wrapper.addInitParameter(pname, pvalue);
            }
        }
           
        // Override any log setting with the container wide logging level
        wrapper.addInitParameter("logVerbosityLevel",getJasperLogLevel());

        ResourceInjectorImpl resourceInjector = new ResourceInjectorImpl(
            webModule);
        servletContext.setAttribute(
                "com.sun.appserv.jsp.resource.injector",
                resourceInjector);

        // START SJSAS 6311155
        String sysClassPath = ASClassLoaderUtil.getModuleClassPath(
            (ServiceLocator) defaultServices,
            webModule.getID(), null
        );
        // If the configuration flag usMyFaces is set, remove javax.faces.jar
        // from the system class path
        Boolean useMyFaces = (Boolean)
            servletContext.getAttribute("com.sun.faces.useMyFaces");
        if (useMyFaces != null && useMyFaces) {
            sysClassPath =
                sysClassPath.replace("javax.faces.jar", "$disabled$.raj");
            // jsf-connector.jar manifest has a Class-Path to javax.faces.jar
            sysClassPath =
                sysClassPath.replace("jsf-connector.jar", "$disabled$.raj");
        }
        // TODO: combine with classpath from
        // servletContext.getAttribute(("org.apache.catalina.jsp_classpath")
        if (_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE, LogFacade.SYS_CLASSPATH, webModule.getID() + " is " + sysClassPath);
        }
        if (sysClassPath.equals("")) {
            // In embedded mode, services returns SingleModulesRegistry and
            // it has no modules.
            // Try "java.class.path" system property instead.
            sysClassPath = System.getProperty("java.class.path"); 
        }
        sysClassPath = trimSysClassPath(sysClassPath);
        wrapper.addInitParameter("com.sun.appserv.jsp.classpath",
            sysClassPath);
        // END SJSAS 6311155

        // Configure JSP monitoring
        servletContext.setAttribute(
            "org.glassfish.jsp.monitor.probeEmitter",
            new JspProbeEmitterImpl(webModule));

        // Pass BeanManager's ELResolver as ServletContext attribute
        // (see IT 11168)
        InvocationManager invocationMgr =
            webContainer.getInvocationManager();
        WebComponentInvocation inv = new WebComponentInvocation(webModule);
        try {
            invocationMgr.preInvoke(inv);
            JCDIService jcdiService = defaultServices.getService(JCDIService.class);
            // JCDIService can be absent if weld integration is missing in the runtime, so check for null is needed.
            if (jcdiService != null && jcdiService.isCurrentModuleJCDIEnabled()) {
                jcdiService.setELResolver(servletContext);
            }
        } catch (NamingException e) {
            // Ignore
        } finally {
            invocationMgr.postInvoke(inv);
        }

    }

    private boolean includeInitialized;
    private List<String> includeJars;

    private void initIncludeJars() {
        if (includeInitialized) {
            return;
        }

        String includeJarsString = null;;
        for (WebComponentDescriptor wcd: wbd.getWebComponentDescriptors()) {
            if ("jsp".equals(wcd.getCanonicalName())) {
                InitializationParameter initp =
                    wcd.getInitializationParameterByName("system-jar-includes");
                if (initp != null) {
                    includeJarsString = initp.getValue();
                    break;
                }
            }
        }
        includeInitialized = true;
        if (includeJarsString == null) {
            includeJars = null;
            return;
        }
        includeJars = new ArrayList<String>();
        StringTokenizer tokenizer = new StringTokenizer(includeJarsString);
        while (tokenizer.hasMoreElements()) {
            includeJars.add(tokenizer.nextToken());
        }
    }

    private boolean included(String path) {
        for (String item: includeJars) {
            if (path.contains(item)) {
                return true;
            }
        }
        return false;
    }

    /*
     * Remove unnecessary system jars, to improve performance
     */
    private String trimSysClassPath(String sysClassPath) {

        if (sysClassPath == null || sysClassPath.equals("")) {
            return "";
        }
        initIncludeJars();
        if (includeJars == null || includeJars.size() == 0) {
            // revert to previous behavior, i.e. no trimming
            return sysClassPath;
        }
        String sep = System.getProperty("path.separator");
        StringBuilder ret = new StringBuilder();
        StringTokenizer tokenizer = new StringTokenizer(sysClassPath, sep);
        String mySep = "";
        while (tokenizer.hasMoreElements()) {
            String path = tokenizer.nextToken();
            if (included(path)) {
                ret.append(mySep);
                ret.append(path);
                mySep = sep;
            }
        }
        return ret.toString();
    }

    /**
     * Determine the debug setting for JspServlet based on the iAS log
     * level.
     */
    private String getJasperLogLevel() {
        Level level = _logger.getLevel();
        if (level == null )
            return "warning";
        if (level.equals(Level.WARNING))
            return "warning";
        else if (level.equals(Level.FINE))
            return "information";
        else if (level.equals(Level.FINER) || level.equals(Level.FINEST))
            return "debug";
        else 
            return "warning";
    }

    private void startCacheManager(WebModule webModule) {

        SunWebApp bean  = webModule.getIasWebAppConfigBean();

        // Configure the cache, cache-mapping and other settings
        if (bean != null) {
            CacheManager cm = null;
            try {
                cm = CacheModule.configureResponseCache(webModule, bean);
            } catch (Exception ee) {
                _logger.log(Level.WARNING, LogFacade.CACHE_MRG_EXCEPTION, ee);
            }
        
            if (cm != null) {
                try {
                    // first start the CacheManager, if enabled
                    cm.start();
                    if (_logger.isLoggable(Level.FINE)) {
                        _logger.log(Level.FINE, LogFacade.CACHE_MANAGER_STARTED);
                    }
                    // set this manager as a context attribute so that 
                    // caching filters/tags can find it
                    ServletContext ctxt = webModule.getServletContext();
                    ctxt.setAttribute(CacheManager.CACHE_MANAGER_ATTR_NAME, cm);

                } catch (LifecycleException ee) {
                    _logger.log(Level.WARNING, ee.getMessage(),
                                               ee.getCause());
                }
            }
        }
    }

    private void stopCacheManager(WebModule webModule) {
        ServletContext ctxt = webModule.getServletContext();
        CacheManager cm = (CacheManager)ctxt.getAttribute(
                                        CacheManager.CACHE_MANAGER_ATTR_NAME);
        if (cm != null) {
            try {
                cm.stop();
                if (_logger.isLoggable(Level.FINE)) {
                    _logger.log(Level.FINE, LogFacade.CACHE_MANAGER_STOPPED);
                }
                ctxt.removeAttribute(CacheManager.CACHE_MANAGER_ATTR_NAME);
            } catch (LifecycleException ee) {
                _logger.log(Level.WARNING, ee.getMessage(), ee.getCause());
            }
        }
    }


    /**
     * Configures the given web module's DefaultServlet with the 
     * applicable web properties from sun-web.xml.
     */
    private void configureDefaultServlet(WebModule webModule) {

        // Find the DefaultServlet
        Wrapper wrapper = (Wrapper)webModule.findChild("default");
        if (wrapper == null) {
            return;
        }

        String servletClass = wrapper.getServletClassName();
        if (servletClass == null
                || !servletClass.equals(Globals.DEFAULT_SERVLET_CLASS_NAME)) {
            return;
        }

        String fileEncoding = webModule.getFileEncoding();
        if (fileEncoding != null) {
            wrapper.addInitParameter("fileEncoding", fileEncoding);
        }
    }
}
