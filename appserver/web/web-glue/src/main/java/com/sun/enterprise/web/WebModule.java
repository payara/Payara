/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2017 Oracle and/or its affiliates. All rights reserved.
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
// Portions Copyright [2016-2017] [Payara Foundation and/or its affiliates]

package com.sun.enterprise.web;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.text.MessageFormat.format;
import static java.util.logging.Level.WARNING;
import static org.glassfish.web.LogFacade.UNABLE_TO_RESTORE_SESSIONS_DURING_REDEPLOY;
import static org.glassfish.web.LogFacade.UNABLE_TO_SAVE_SESSIONS_DURING_REDEPLOY;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RunAs;
import javax.servlet.Filter;
import javax.servlet.HttpMethodConstraintElement;
import javax.servlet.Servlet;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletSecurityElement;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpUpgradeHandler;

import org.apache.catalina.Container;
import org.apache.catalina.ContainerListener;
import org.apache.catalina.InstanceListener;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Loader;
import org.apache.catalina.Pipeline;
import org.apache.catalina.Realm;
import org.apache.catalina.Valve;
import org.apache.catalina.Wrapper;
import org.apache.catalina.core.DynamicServletRegistrationImpl;
import org.apache.catalina.core.ServletRegistrationImpl;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardPipeline;
import org.apache.catalina.core.StandardWrapper;
import org.apache.catalina.deploy.FilterMaps;
import org.apache.catalina.loader.WebappLoader;
import org.apache.catalina.servlets.DefaultServlet;
import org.apache.catalina.session.StandardManager;
import org.apache.jasper.servlet.JspServlet;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.embeddable.web.Context;
import org.glassfish.embeddable.web.config.FormLoginConfig;
import org.glassfish.embeddable.web.config.LoginConfig;
import org.glassfish.embeddable.web.config.SecurityConfig;
import org.glassfish.embeddable.web.config.TransportGuarantee;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.classmodel.reflect.Types;
import org.glassfish.internal.api.ServerContext;
import org.glassfish.security.common.Role;
import org.glassfish.web.LogFacade;
import org.glassfish.web.admin.monitor.ServletProbeProvider;
import org.glassfish.web.admin.monitor.SessionProbeProvider;
import org.glassfish.web.admin.monitor.WebModuleProbeProvider;
import org.glassfish.web.deployment.annotation.handlers.ServletSecurityHandler;
import org.glassfish.web.deployment.descriptor.AbsoluteOrderingDescriptor;
import org.glassfish.web.deployment.descriptor.AuthorizationConstraintImpl;
import org.glassfish.web.deployment.descriptor.LoginConfigurationImpl;
import org.glassfish.web.deployment.descriptor.SecurityConstraintImpl;
import org.glassfish.web.deployment.descriptor.UserDataConstraintImpl;
import org.glassfish.web.deployment.descriptor.WebBundleDescriptorImpl;
import org.glassfish.web.deployment.descriptor.WebComponentDescriptorImpl;
import org.glassfish.web.deployment.descriptor.WebResourceCollectionImpl;
import org.glassfish.web.deployment.runtime.CookieProperties;
import org.glassfish.web.deployment.runtime.LocaleCharsetInfo;
import org.glassfish.web.deployment.runtime.LocaleCharsetMap;
import org.glassfish.web.deployment.runtime.SessionConfig;
import org.glassfish.web.deployment.runtime.SessionManager;
import org.glassfish.web.deployment.runtime.SessionProperties;
import org.glassfish.web.deployment.runtime.SunWebAppImpl;
import org.glassfish.web.deployment.runtime.WebProperty;
import org.glassfish.web.loader.ServletContainerInitializerUtil;
import org.glassfish.web.valve.GlassFishValve;
import org.jvnet.hk2.config.types.Property;

import com.sun.enterprise.config.serverbeans.Application;
import com.sun.enterprise.config.serverbeans.ConfigBeansUtilities;
import com.sun.enterprise.config.serverbeans.ServerTags;
import com.sun.enterprise.container.common.spi.util.JavaEEIOUtils;
import com.sun.enterprise.deployment.RunAsIdentityDescriptor;
import com.sun.enterprise.deployment.WebBundleDescriptor;
import com.sun.enterprise.deployment.WebComponentDescriptor;
import com.sun.enterprise.deployment.WebServiceEndpoint;
import com.sun.enterprise.deployment.WebServicesDescriptor;
import com.sun.enterprise.deployment.runtime.web.SunWebApp;
import com.sun.enterprise.deployment.web.LoginConfiguration;
import com.sun.enterprise.deployment.web.SecurityConstraint;
import com.sun.enterprise.deployment.web.ServletFilterMapping;
import com.sun.enterprise.deployment.web.UserDataConstraint;
import com.sun.enterprise.deployment.web.WebResourceCollection;
import com.sun.enterprise.security.integration.RealmInitializer;
import com.sun.enterprise.util.StringUtils;
import com.sun.enterprise.web.deploy.LoginConfigDecorator;
import com.sun.enterprise.web.pwc.PwcWebModule;
import com.sun.enterprise.web.session.PersistenceType;
import com.sun.enterprise.web.session.SessionCookieConfig;
import com.sun.web.security.RealmAdapter;

/**
 * Class representing a web module for use by the Application Server.
 */

public class WebModule extends PwcWebModule implements Context {

    // ----------------------------------------------------- Class Variables

    private static final Logger logger = LogFacade.getLogger();

    protected static final ResourceBundle rb = logger.getResourceBundle();

    private static final String ALTERNATE_FROM = "from=";
    private static final String ALTERNATE_DOCBASE = "dir=";

    private static final Base64.Encoder encoder = Base64.getMimeEncoder();
    private static final Base64.Decoder decoder = Base64.getMimeDecoder();

    private static final String WS_SERVLET_CONTEXT_LISTENER =
        "com.sun.xml.ws.transport.http.servlet.WSServletContextListener";

    // ----------------------------------------------------- Instance Variables

    // Object containing sun-web.xml information
    private SunWebAppImpl iasBean = null;

    //locale-charset-info tag from sun-web.xml
    private LocaleCharsetMap[] _lcMap = null;

    /**
     * Is the default-web.xml parsed?
     */
    private boolean hasBeenXmlConfigured = false;

    private WebContainer webContainer;

    private final Map<String,AdHocServletInfo> adHocPaths;
    private boolean hasAdHocPaths;

    private final Map<String,AdHocServletInfo> adHocSubtrees;
    private boolean hasAdHocSubtrees;

    private StandardPipeline adHocPipeline;

    // File encoding of static resources
    private String fileEncoding;

    /**
     * Cached findXXX results
     */
    protected Object[] cachedFinds;

    private Application bean;

    private WebBundleDescriptor webBundleDescriptor;

    private boolean hasStarted = false;
    private String compEnvId = null;
    private ServerContext serverContext = null;

    private ServletProbeProvider servletProbeProvider = null;
    private SessionProbeProvider sessionProbeProvider = null;
    private WebModuleProbeProvider webModuleProbeProvider = null;

    private JavaEEIOUtils javaEEIOUtils;

    // The id of the parent container (i.e., virtual server) on which this
    // web module was deployed
    private String vsId;

    private String monitoringNodeName;

    private WebModuleConfig wmInfo;

    // true if standalone WAR, false if embedded in EAR file
    private boolean isStandalone = true;

    private ServiceLocator services;

    /**
     * Constructor.
     */
    public WebModule() {
        this(null);
    }

    public WebModule(ServiceLocator services) {
        super();
        this.services = services;
        this.adHocPaths = new HashMap<String,AdHocServletInfo>();
        this.adHocSubtrees = new HashMap<String,AdHocServletInfo>();

        this.adHocPipeline = new StandardPipeline(this);
        this.adHocPipeline.setBasic(new AdHocContextValve(this));

        notifyContainerListeners = false;
    }


    /**
     * set the sun-web.xml config bean
     */
    public void setIasWebAppConfigBean(SunWebAppImpl iasBean) {
       this.iasBean = iasBean;
    }

    /**
     * gets the sun-web.xml config bean
     */
    public SunWebAppImpl getIasWebAppConfigBean() {
       return iasBean;
    }

    /**
     * Gets the web container in which this web module was loaded.
     *
     * @return the web container in which this web module was loaded
     */
    public WebContainer getWebContainer() {
        return webContainer;
    }

    /**
     * Sets the web container in which this web module was loaded.
     *
     */
    public void setWebContainer(WebContainer webContainer) {
        this.webContainer = webContainer;
        this.servletProbeProvider = webContainer.getServletProbeProvider();
        this.sessionProbeProvider = webContainer.getSessionProbeProvider();
        this.webModuleProbeProvider =
            webContainer.getWebModuleProbeProvider();
        this.javaEEIOUtils =
            webContainer.getJavaEEIOUtils();
    }

    public void setWebModuleConfig(WebModuleConfig wmInfo) {
        this.wmInfo = wmInfo;
    }

    public WebModuleConfig getWebModuleConfig() {
        return wmInfo;
    }

    void setMonitoringNodeName(String monitoringNodeName) {
        this.monitoringNodeName = monitoringNodeName;
    }

    public String getMonitoringNodeName() {
        return monitoringNodeName;
    }

    /**
     * Sets the parameter encoding (i18n) info from web.xml and sun-web.xml and glassfish-web.xml.
     */
    public void setI18nInfo() {
    	
		if (webBundleDescriptor != null) {
			String reqEncoding = webBundleDescriptor.getRequestCharacterEncoding();
			if (reqEncoding != null) {
				setRequestCharacterEncoding(reqEncoding);
			}
			String resEncoding = webBundleDescriptor.getResponseCharacterEncoding();
			if (resEncoding != null) {
				setResponseCharacterEncoding(resEncoding);
			}
		}

        if (iasBean == null) {
            return;
        }

        if (iasBean.isParameterEncoding()) {
            formHintField = iasBean.getAttributeValue(
                                                SunWebApp.PARAMETER_ENCODING,
                                                SunWebApp.FORM_HINT_FIELD);
            defaultCharset = iasBean.getAttributeValue(
                                                SunWebApp.PARAMETER_ENCODING,
                                                SunWebApp.DEFAULT_CHARSET);
        }

        LocaleCharsetInfo lcinfo = iasBean.getLocaleCharsetInfo();
        if (lcinfo != null) {
            if (lcinfo.getAttributeValue(
                            LocaleCharsetInfo.DEFAULT_LOCALE) != null) {
               logger.warning(LogFacade.DEFAULT_LOCALE_DEPRECATED);
            }
            /*
             * <parameter-encoding> subelem of <sun-web-app> takes precedence
             * over that of <locale-charset-info>
             */
            if (lcinfo.isParameterEncoding()
                    && !iasBean.isParameterEncoding()) {
                formHintField = lcinfo.getAttributeValue(
                                        LocaleCharsetInfo.PARAMETER_ENCODING,
                                        LocaleCharsetInfo.FORM_HINT_FIELD);
                defaultCharset = lcinfo.getAttributeValue(
                                        LocaleCharsetInfo.PARAMETER_ENCODING,
                                        LocaleCharsetInfo.DEFAULT_CHARSET);
            }
            _lcMap = lcinfo.getLocaleCharsetMap();
        }
        
		if (defaultCharset != null) {
			setRequestCharacterEncoding(defaultCharset);
			setResponseCharacterEncoding(defaultCharset);
		}
    }

    /**
     * return locale-charset-map
     */
    public LocaleCharsetMap[] getLocaleCharsetMap() {
        return _lcMap;
    }

    /**
     * Returns true if this web module specifies a locale-charset-map in its
     * sun-web.xml, false otherwise.
     *
     * @return true if this web module specifies a locale-charset-map in its
     * sun-web.xml, false otherwise
     */
    @Override
    public boolean hasLocaleToCharsetMapping() {
        LocaleCharsetMap[] locCharsetMap = getLocaleCharsetMap();
        return (locCharsetMap != null && locCharsetMap.length > 0);
    }

    /**
     * Matches the given request locales against the charsets specified in
     * the locale-charset-map of this web module's sun-web.xml, and returns
     * the first matching charset.
     *
     * @param locales Request locales
     *
     * @return First matching charset, or null if this web module does not
     * specify any locale-charset-map in its sun-web.xml, or no match was
     * found
     */
    @Override
    public String mapLocalesToCharset(Enumeration locales) {

        String encoding = null;

        LocaleCharsetMap[] locCharsetMap = getLocaleCharsetMap();
        if (locCharsetMap != null && locCharsetMap.length > 0) {
            /*
             * Check to see if there is a match between the request
             * locales (in preference order) and the locales in the
             * locale-charset-map.
             */
            boolean matchFound = false;
            while (locales.hasMoreElements() && !matchFound) {
                Locale reqLoc = (Locale) locales.nextElement();
                for (int i=0; i<locCharsetMap.length && !matchFound; i++) {
                    String language = locCharsetMap[i].getAttributeValue(
                                                LocaleCharsetMap.LOCALE);
                    if (language == null || "".equals(language)) {
                        continue;
                    }
                    String country = null;
                    int index = language.indexOf('_');
                    if (index != -1) {
                        country = language.substring(index+1);
                        language = language.substring(0, index);
                    }
                    Locale mapLoc = null;
                    if (country != null) {
                        mapLoc = new Locale(language, country);
                    } else {
                        mapLoc = new Locale(language);
                    }
                    if (mapLoc.equals(reqLoc)) {
                        /*
                         * Match found. Get the charset to which the
                         * matched locale maps.
                         */
                        encoding = locCharsetMap[i].getAttributeValue(
                                                    LocaleCharsetMap.CHARSET);
                        matchFound = true;
                    }
                }
            }
        }

        return encoding;
    }

    /**
     * Creates an ObjectInputStream that provides special deserialization
     * logic for classes that are normally not serializable (such as
     * javax.naming.Context).
     */
    @Override
    public ObjectInputStream createObjectInputStream(InputStream is)
            throws IOException {

        ObjectInputStream ois = null;

        Loader loader = getLoader();
        if (loader != null) {
            ClassLoader classLoader = loader.getClassLoader();
            if (classLoader != null) {
                try {
                    ois = javaEEIOUtils.createObjectInputStream(
                        is, true, classLoader, getUniqueId());
                } catch (Exception e) {
                    logger.log(Level.SEVERE,
                            LogFacade.CREATE_CUSTOM_OBJECT_INTPUT_STREAM_ERROR, e);
                }
            }
        }

        if (ois == null) {
            ois = new ObjectInputStream(is);
        }

        return ois;
    }

    /**
     * Creates an ObjectOutputStream that provides special serialization
     * logic for classes that are normally not serializable (such as
     * javax.naming.Context).
     */
    @Override
    public ObjectOutputStream createObjectOutputStream(OutputStream os)
            throws IOException {

        ObjectOutputStream oos = null;

        try {
            oos = javaEEIOUtils.createObjectOutputStream(os, true);
        } catch (IOException ioe) {
            logger.log(Level.SEVERE,
                    LogFacade.CREATE_CUSTOM_BOJECT_OUTPUT_STREAM_ERROR, ioe);
            oos = new ObjectOutputStream(os);
        }

        return oos;
    }

    /**
     * Set to <code>true</code> when the default-web.xml has been read for
     * this module.
     */
    public void setXmlConfigured(boolean hasBeenXmlConfigured){
        this.hasBeenXmlConfigured = hasBeenXmlConfigured;
    }

    /**
     * Return <code>true</code> if the default=web.xml has been read for
     * this module.
     */
    public boolean hasBeenXmlConfigured(){
        return hasBeenXmlConfigured;
    }

    /**
     * Cache the result of doing findXX on this object
     * NOTE: this method MUST be used only when loading/using
     * the content of default-web.xml
     */
    public void setCachedFindOperation(Object[] cachedFinds){
        this.cachedFinds = cachedFinds;
    }

    /**
     * Return the cached result of doing findXX on this object
     * NOTE: this method MUST be used only when loading/using
     * the content of default-web.xml
     */
    public Object[] getCachedFindOperation(){
        return cachedFinds;
    }

    @Override
    public void setRealm(Realm realm) {
        if ((realm != null) && !(realm instanceof RealmAdapter)) {
            logger.log(Level.SEVERE, LogFacade.IGNORE_INVALID_REALM,
                    new Object[] { realm.getClass().getName(),
                        RealmAdapter.class.getName() });
        } else {
            super.setRealm(realm);
        }
    }

    /**
     * Starts this web module.
     */
    @Override
    public synchronized void start() throws LifecycleException {
        // Get interestList of ServletContainerInitializers present, if any.
        List<Object> orderingList = null;
        boolean hasOthers = false;
        Map<String, String> webFragmentMap = Collections.emptyMap();
        if (webBundleDescriptor != null) {
            AbsoluteOrderingDescriptor aod =
                    ((WebBundleDescriptorImpl)webBundleDescriptor).getAbsoluteOrderingDescriptor();
            if (aod != null) {
                orderingList = aod.getOrdering();
                hasOthers = aod.hasOthers();
            }
            webFragmentMap = webBundleDescriptor.getJarNameToWebFragmentNameMap();
        }

        boolean servletInitializersEnabled = true;
        if (webBundleDescriptor != null) {
            servletInitializersEnabled = webBundleDescriptor.getServletInitializersEnabled();
        }
        Iterable<ServletContainerInitializer> allInitializers =
            ServletContainerInitializerUtil.getServletContainerInitializers(
                webFragmentMap, orderingList, hasOthers,
                wmInfo.getAppClassLoader(), servletInitializersEnabled);
        setServletContainerInitializerInterestList(allInitializers);

        DeploymentContext dc = getWebModuleConfig().getDeploymentContext();
        if (dc != null) {
            directoryDeployed = 
                    Boolean.valueOf(dc.getAppProps().getProperty(ServerTags.DIRECTORY_DEPLOYED));
        }
        if (webBundleDescriptor != null) {
            showArchivedRealPathEnabled = webBundleDescriptor.isShowArchivedRealPathEnabled();
            servletReloadCheckSecs = webBundleDescriptor.getServletReloadCheckSecs();
            	String reqEncoding = webBundleDescriptor.getRequestCharacterEncoding();
			if (reqEncoding != null) {
				setRequestCharacterEncoding(reqEncoding);
			}
			String resEncoding = webBundleDescriptor.getResponseCharacterEncoding();
			if (resEncoding != null) {
				setResponseCharacterEncoding(resEncoding);
			}
        }

        // Start and register Tomcat mbeans
        super.start();

        // Configure catalina listeners and valves. This can only happen
        // after this web module has been started, in order to be able to
        // load the specified listener and valve classes.
        configureValves();
        configureCatalinaProperties();
        webModuleStartedEvent();
        if (directoryListing) {
            setDirectoryListing(directoryListing);
        }

        hasStarted = true;
    }

    /**
     * Stops this web module.
     */
    @Override
    public void stop() throws LifecycleException {
        // Unregister monitoring mbeans only if this web module was
        // successfully started, because if stop() is called during an
        // aborted start(), no monitoring mbeans will have been registered
        if (hasStarted) {
            webModuleStoppedEvent();
            hasStarted = false;
        }

        // Stop and unregister Tomcat mbeans
        super.stop(getWebContainer().isShutdown());
    }

    @Override
    protected void contextListenerStart() {
        ServletContext servletContext = getServletContext();
        WebBundleDescriptor wbd = getWebBundleDescriptor();
        try {
            // for jsf injection
            servletContext.setAttribute(
                    Constants.DEPLOYMENT_CONTEXT_ATTRIBUTE,
                    getWebModuleConfig().getDeploymentContext());
            // null check for OSGi/HTTP
            if (wbd != null) {
                servletContext.setAttribute(
                        Constants.IS_DISTRIBUTABLE_ATTRIBUTE,
                        wbd.isDistributable());
            }
            servletContext.setAttribute(
                    Constants.ENABLE_HA_ATTRIBUTE,
                    Boolean.valueOf(
                        webContainer.getServerConfigLookup().calculateWebAvailabilityEnabledFromConfig(this)));

            super.contextListenerStart();
        } finally {
            servletContext.removeAttribute(
                    Constants.DEPLOYMENT_CONTEXT_ATTRIBUTE);
            servletContext.removeAttribute(
                    Constants.IS_DISTRIBUTABLE_ATTRIBUTE);
            servletContext.removeAttribute(
                    Constants.ENABLE_HA_ATTRIBUTE);
        }
        for (ServletRegistrationImpl srImpl : servletRegisMap.values()) {
            if (srImpl instanceof DynamicWebServletRegistrationImpl) {
                DynamicWebServletRegistrationImpl dwsrImpl =
                    (DynamicWebServletRegistrationImpl)srImpl;
                dwsrImpl.postProcessAnnotations();
            }
        }
        webContainer.afterServletContextInitializedEvent(wbd);
    }

    @Override
    protected Types getTypes() {
        if (wmInfo.getDeploymentContext()!=null) {
            return wmInfo.getDeploymentContext().getTransientAppMetaData(Types.class.getName(), Types.class);
        } else {
            return null;
        }
    }

    @Override
    protected void callServletContainerInitializers()
            throws LifecycleException {
        WebComponentInvocation inv = new WebComponentInvocation(this);
        try {
            webContainer.getInvocationManager().preInvoke(inv);
            super.callServletContainerInitializers();
        }
        finally {
            webContainer.getInvocationManager().postInvoke(inv);
        }
        if (!isJsfApplication() && !contextListeners.isEmpty()) {
            /* 
             * Remove any JSF related ServletContextListeners from
             * non-JSF apps.
             * This can be done reliably only after all
             * ServletContainerInitializers have been invoked, because
             * system-wide ServletContainerInitializers may be invoked in
             * any order, and it is only after JSF's FacesInitializer has
             * been invoked that isJsfApplication(), which checks for the
             * existence of a mapping to the FacesServlet in the app, may
             * be used reliably because such mapping would have been added
             * by JSF's FacesInitializer. See also IT 10223
             */
            ArrayList<ServletContextListener> listeners =
                new ArrayList<ServletContextListener>(contextListeners);
            String listenerClassName = null;
            for (ServletContextListener listener : listeners) {
                if (listener instanceof
                        StandardContext.RestrictedServletContextListener) {
                    listenerClassName = ((StandardContext.RestrictedServletContextListener) listener).getNestedListener().getClass().getName();
                } else {
                    listenerClassName = listener.getClass().getName();
                }
                /*
                 * TBD: Retrieve listener class name from JSF's TldProvider
                 */
                if ("com.sun.faces.config.ConfigureListener".equals(
                        listenerClassName)) {
                    contextListeners.remove(listener);
                }
            }
        }
    }

    /**
     * Sets the virtual server parent of this web module, and passes it on to
     * this web module's realm adapter..
     *
     * @param container The virtual server parent
     */
    @Override
    public void setParent(Container container) {
        super.setParent(container);

        if (container instanceof VirtualServer) {
            vsId = ((VirtualServer) container).getID();
        }

        // The following assumes that the realm has been set on this WebModule
        // before the WebModule is added as a child to the virtual server on
        // which it is being deployed.
        /*RealmAdapter ra = (RealmAdapter) getRealm();
        if (ra != null) {
          1  ra.setVirtualServer(container);
        }*/
        Realm ra = getRealm();
        if (ra != null && ra instanceof RealmInitializer) {
            ((RealmInitializer) ra).setVirtualServer(container);
        }
    }

    /**
     * Indicates whether this web module contains any ad-hoc paths.
     *
     * An ad-hoc path is a servlet path that is mapped to a servlet
     * not declared in the web module's deployment descriptor.
     *
     * A web module all of whose mappings are for ad-hoc paths is called an
     * ad-hoc web module.
     *
     * @return true if this web module contains any ad-hoc paths, false
     * otherwise
     */
    @Override
    public boolean hasAdHocPaths() {
        return this.hasAdHocPaths;
    }

    /**
     * Indicates whether this web module contains any ad-hoc subtrees.
     *
     * @return true if this web module contains any ad-hoc subtrees, false
     * otherwise
     */
    public boolean hasAdHocSubtrees() {
        return this.hasAdHocSubtrees;
    }

    /*
     * Adds the given ad-hoc path and subtree, along with information about
     * the servlet that will be responsible for servicing it, to this web
     * module.
     *
     * @param path The ad-hoc path to add
     * @param subtree The ad-hoc subtree path to add
     * @param servletInfo Information about the servlet that is responsible
     * for servicing the given ad-hoc path
     */
    void addAdHocPathAndSubtree(String path,
                                String subtree,
                                AdHocServletInfo servletInfo) {

        if (path == null && subtree == null) {
            return;
        }

        Wrapper adHocWrapper = (Wrapper)
            findChild(servletInfo.getServletName());
        if (adHocWrapper == null) {
            adHocWrapper = createAdHocWrapper(servletInfo);
            addChild(adHocWrapper);
        }

        if (path != null) {
            adHocPaths.put(path, servletInfo);
            hasAdHocPaths = true;
        }

        if (subtree != null) {
            adHocSubtrees.put(subtree, servletInfo);
            hasAdHocSubtrees = true;
        }
    }

    /*
     * Adds the given ad-hoc path to servlet mappings to this web module.
     *
     * @param newPaths Mappings of ad-hoc paths to the servlets responsible
     * for servicing them
     */
    void addAdHocPaths(Map<String, AdHocServletInfo> newPaths) {

        if (newPaths == null || newPaths.isEmpty()) {
            return;
        }
        for (Map.Entry<String, AdHocServletInfo> entry : newPaths.entrySet()) {
            AdHocServletInfo servletInfo = entry.getValue();
            Wrapper adHocWrapper = (Wrapper)
                findChild(servletInfo.getServletName());
            if(adHocWrapper == null) {
                adHocWrapper = createAdHocWrapper(servletInfo);
                addChild(adHocWrapper);
            }
            adHocPaths.put(entry.getKey(), servletInfo);
        }

        hasAdHocPaths = true;
    }

    /*
     * Adds the given ad-hoc subtree path to servlet mappings to this web
     * module.
     *
     * @param newSubtrees Mappings of ad-hoc subtree paths to the servlets
     * responsible for servicing them
     */
    void addAdHocSubtrees(Map<String, AdHocServletInfo> newSubtrees) {

        if (newSubtrees == null || newSubtrees.isEmpty()) {
            return;
        }
        for (Map.Entry<String, AdHocServletInfo> entry : newSubtrees.entrySet()) {
            AdHocServletInfo servletInfo = entry.getValue();
            Wrapper adHocWrapper = (Wrapper)findChild(servletInfo.getServletName());
            if(adHocWrapper == null) {
                adHocWrapper = createAdHocWrapper(servletInfo);
                addChild(adHocWrapper);
            }
            adHocSubtrees.put(entry.getKey(), servletInfo);
        }

        hasAdHocSubtrees = true;
    }

    /*
     * Gets the ad-hoc path to servlet mappings managed by this web module.
     *
     * @return The ad-hoc path to servlet mappings managed by this web
     * module.
     */
    Map<String, AdHocServletInfo> getAdHocPaths() {
        return adHocPaths;
    }

    /*
     * Gets the ad-hoc subtree path to servlet mappings managed by this
     * web module.
     *
     * @return The ad-hoc subtree path to servlet mappings managed by
     * this web module.
     */
    Map<String, AdHocServletInfo> getAdHocSubtrees() {
        return adHocSubtrees;
    }

    /**
     * Returns the name of the ad-hoc servlet responsible for servicing the
     * given path.
     *
     * @param path The path whose associated ad-hoc servlet is needed
     *
     * @return The name of the ad-hoc servlet responsible for servicing the
     * given path, or null if the given path does not represent an ad-hoc
     * path
     */
    @Override
    public String getAdHocServletName(String path) {

        if (!hasAdHocPaths() && !hasAdHocSubtrees()) {
            return null;
        }

        AdHocServletInfo servletInfo = null;

        // Check if given path matches any of the ad-hoc paths (exact match)
        if (path == null) {
            servletInfo = adHocPaths.get("");
        } else {
            servletInfo = adHocPaths.get(path);
        }

        // Check if given path starts with any of the ad-hoc subtree paths
        if (servletInfo == null && path != null && hasAdHocSubtrees()) {
            for(String adHocSubtree : adHocSubtrees.keySet()) {
                if(path.startsWith(adHocSubtree)) {
                    servletInfo = adHocSubtrees.get(adHocSubtree);
                    break;
                }
            }
        }

        if (servletInfo != null) {
            return servletInfo.getServletName();
        } else {
            return null;
        }
    }

    /*
     * Removes the given ad-hoc path from this web module.
     *
     * @param path The ad-hoc path to remove
     */
    void removeAdHocPath(String path) {
        if (path == null) {
            return;
        }
        adHocPaths.remove(path);
        if (adHocPaths.isEmpty()) {
            this.hasAdHocPaths = false;
        }
    }

    /*
     * Removes the given ad-hoc path from this web module.
     *
     * @param subtree The ad-hoc subtree to remove
     */
    void removeAdHocSubtree(String subtree) {
        if (subtree == null) {
            return;
        }
        adHocSubtrees.remove(subtree);
        if (adHocSubtrees.isEmpty()) {
            this.hasAdHocSubtrees = false;
        }
    }

    /**
     * Adds the given valve to this web module's ad-hoc pipeline.
     *
     * @param valve The valve to add
     */
    public void addAdHocValve(GlassFishValve valve) {
        adHocPipeline.addValve(valve);
    }

    /**
     * Removes the given valve from this web module's ad-hoc pipeline.
     *
     * @param valve The valve to remove
     */
    public void removeAdHocValve(GlassFishValve valve) {
        adHocPipeline.removeValve(valve);
    }

    /**
     * Gets this web module's ad-hoc pipeline.
     *
     * @return This web module's ad-hoc pipeline
     */
    public Pipeline getAdHocPipeline() {
        return adHocPipeline;
    }

    /**
     * Sets the file encoding of all static resources of this web module.
     *
     * @param enc The file encoding of static resources of this web module
     */
    public void setFileEncoding(String enc) {
        this.fileEncoding = enc;
    }

    /**
     * Gets the file encoding of all static resources of this web module.
     *
     * @return The file encoding of static resources of this web module
     */
    public String getFileEncoding() {
        return fileEncoding;
    }

    /**
     * Configures this web module with the filter mappings specified in the
     * deployment descriptor.
     *
     * @param sfm The filter mappings of this web module as specified in the
     * deployment descriptor
     */
    @SuppressWarnings({"unchecked"})
    void addFilterMap(ServletFilterMapping sfm) {

        FilterMaps filterMaps = new FilterMaps();
        filterMaps.setFilterName(sfm.getName());
        filterMaps.setDispatcherTypes(sfm.getDispatchers());

        List<String> servletNames = sfm.getServletNames();
        if (servletNames != null) {
            for(String servletName : servletNames) {
                filterMaps.addServletName(servletName);
            }
        }

        List<String> urlPatterns = sfm.getUrlPatterns();
        if (urlPatterns != null) {
            for(String urlPattern : urlPatterns) {
                filterMaps.addURLPattern(urlPattern);
            }
        }

        addFilterMaps(filterMaps);
    }

    /**
     * Creates an ad-hoc servlet wrapper from the given ad-hoc servlet info.
     *
     * @param servletInfo Ad-hoc servlet info from which to generate
     * ad-hoc servlet wrapper
     *
     * @return The generated ad-hoc servlet wrapper
     */
    private Wrapper createAdHocWrapper(AdHocServletInfo servletInfo) {

        Wrapper adHocWrapper = new StandardWrapper();
        adHocWrapper.setServletClassName(
            servletInfo.getServletClass().getName());
        adHocWrapper.setName(servletInfo.getServletName());
        Map<String,String> initParams = servletInfo.getServletInitParams();
        if (initParams != null && !initParams.isEmpty()) {
            for(Map.Entry<String,String> entry : initParams.entrySet()) {
                adHocWrapper.addInitParameter(entry.getKey(), entry.getValue());
            }
        }

        return adHocWrapper;
    }

    /**
     * Configure the <code>WebModule</code> valves.
     */
    protected void configureValves(){
        if (iasBean != null && iasBean.getValve() != null && iasBean.sizeValve() > 0) {
            org.glassfish.web.deployment.runtime.Valve[] valves = iasBean.getValve();
            for (org.glassfish.web.deployment.runtime.Valve valve: valves) {
                addValve(valve);
            }
        }

    }

    /**
     * Configure the <code>WebModule</code< properties.
     */
    protected void configureCatalinaProperties(){
        String propName = null;
        String propValue = null;
        if (bean != null) {
            List<Property> props = bean.getProperty();
            if (props != null) {
                for (Property prop : props) {
                    propName = prop.getName();
                    propValue = prop.getValue();
                    configureCatalinaProperties(propName,propValue);
                }
            }
        }

        if (iasBean != null && iasBean.sizeWebProperty() > 0) {
            WebProperty[] wprops = iasBean.getWebProperty();
            for(WebProperty wprop : wprops) {
                propName = wprop.getAttributeValue("name");
                propValue = wprop.getAttributeValue("value");
                configureCatalinaProperties(propName, propValue);
            }
        }
    }

    /**
     * Configure the <code>WebModule</code< properties.
     * @param propName the property name
     * @param propValue the property value
     */
    protected void configureCatalinaProperties(String propName,
                                               String propValue){
        if (propName == null || propValue == null) {
            logger.log(Level.WARNING,
                        LogFacade.NULL_WEB_MODULE_PROPERTY,
                        getName());
            return;
        }

        if (propName.startsWith("valve_")) {
            addValve(propValue);
        } else if (propName.startsWith("listener_")) {
            addCatalinaListener(propValue);
        }
    }

    /**
     * Instantiates a <tt>Valve</tt> from the given <tt>className</tt>
     * and adds it to the <tt>Pipeline</tt> of this WebModule.
     *
     * @param className the fully qualified class name of the <tt>Valve</tt>
     */
    protected void addValve(String className) {
        Object valve = loadInstance(className);
        if (valve instanceof Valve) {
            super.addValve((Valve) valve);
        } else if (valve instanceof GlassFishValve) {
            super.addValve((GlassFishValve) valve);
        } else {
            logger.log(Level.WARNING, LogFacade.VALVE_CLASS_NAME_NO_VALVE,
                       className);
        }
    }

    /**
     * Constructs a <tt>Valve</tt> from the given <tt>valveDescriptor</tt>
     * and adds it to the <tt>Pipeline</tt> of this WebModule.
     * @param valveDescriptor the object containing the information to
     * create the valve.
     */
    protected void addValve(org.glassfish.web.deployment.runtime.Valve valveDescriptor) {
        String valveName = valveDescriptor.getAttributeValue(
                org.glassfish.web.deployment.runtime.Valve.NAME);
        String className = valveDescriptor.getAttributeValue(
                org.glassfish.web.deployment.runtime.Valve.CLASS_NAME);
        if (valveName == null) {
            logger.log(Level.WARNING, LogFacade.VALVE_MISSING_NAME,
                       getName());
            return;
        }
        if (className == null) {
            logger.log(Level.WARNING, LogFacade.VALVE_MISSING_CLASS_NAME,
                       new Object[]{valveName, getName()});
            return;
        }
        Object valve = loadInstance(className);
        if (valve == null) {
            return;
        }
        if (!(valve instanceof GlassFishValve) &&
                !(valve instanceof Valve)) {
            logger.log(Level.WARNING, LogFacade.VALVE_CLASS_NAME_NO_VALVE,
                       className);
            return;
        }
        WebProperty[] props = valveDescriptor.getWebProperty();
        if (props != null && props.length > 0) {
            for (WebProperty property: props) {
                String propName = getSetterName(
                    property.getAttributeValue(WebProperty.NAME));
                if (propName != null && propName.length() != 0) {
                    String value = property.getAttributeValue(
                        WebProperty.VALUE);
                    try {
                        Method method = valve.getClass().getMethod(
                            propName, String.class);
                        method.invoke(valve, value);
                    } catch (NoSuchMethodException ex) {
                        String msg = rb.getString(LogFacade.VALVE_SPECIFIED_METHOD_MISSING);
                        msg = MessageFormat.format(msg,
                            new Object[] { propName, valveName, getName()});
                        logger.log(Level.SEVERE, msg, ex);
                    } catch (Throwable t) {
                        String msg = rb.getString(LogFacade.VALVE_SETTER_CAUSED_EXCEPTION);
                        msg = MessageFormat.format(msg,
                            new Object[] { propName, valveName, getName()});
                        logger.log(Level.SEVERE, msg, t);
                    }
                } else {
                    logger.log(Level.WARNING,
                        LogFacade.VALVE_MISSING_PROPERTY_NAME,
                        new Object[]{valveName, getName()});
                    return;
                }
            }
        }
        if (valve instanceof Valve) {
            super.addValve((Valve) valve);
        } else if (valve instanceof GlassFishValve) {
            super.addValve((GlassFishValve) valve);
        }
    }

    /**
     * Adds the Catalina listener with the given class name to this
     * WebModule.
     *
     * @param listenerName The fully qualified class name of the listener
     */
    protected void addCatalinaListener(String listenerName) {
        Object listener = loadInstance(listenerName);

        if ( listener == null ) return;

        if (listener instanceof ContainerListener) {
            addContainerListener((ContainerListener)listener);
        } else if (listener instanceof LifecycleListener ){
            addLifecycleListener((LifecycleListener)listener);
        } else if (listener instanceof InstanceListener){
            addInstanceListener(listenerName);
        } else {
            logger.log(Level.SEVERE, LogFacade.INVALID_LISTENER,
                new Object[] {listenerName, getName()});
        }
    }

    private Object loadInstance(String className){
        try{
            Class clazz = getLoader().getClassLoader().loadClass(className);
            return clazz.newInstance();
        } catch (Throwable ex){
            String msg = rb.getString(LogFacade.UNABLE_TO_LOAD_EXTENSION);
            msg = MessageFormat.format(msg, new Object[] { className, getName() });
            logger.log(Level.SEVERE, msg, ex);
        }
        return null;
    }

    private String getSetterName(String propName) {
        if (propName != null) {
            if (propName.length() > 1) {
                propName = "set" + Character.toUpperCase(propName.charAt(0)) +
                        propName.substring(1);
            }
            else {
                propName = "set" + Character.toUpperCase(propName.charAt(0));
            }
        }
        return propName;
    }

    public Application getBean() {
        return bean;
    }

    public void setBean(Application bean) {
        this.bean = bean;
    }

    void setStandalone(boolean isStandalone) {
        this.isStandalone = isStandalone;
    }

    @Override
    protected boolean isStandalone() {
        return isStandalone;
    }

    /**
     * Sets the WebBundleDescriptor (web.xml) for this WebModule.
     *
     * @param wbd The WebBundleDescriptor
     */
    void setWebBundleDescriptor(WebBundleDescriptor wbd) {
        this.webBundleDescriptor = wbd;
    }

    /**
     * Gets the WebBundleDesciptor (web.xml) for this WebModule.
     */
    public WebBundleDescriptor getWebBundleDescriptor() {
        return this.webBundleDescriptor;
    }

    /**
     * Gets ComponentId for Invocation.
     */
    public String getComponentId() {
        return compEnvId;
    }

    /**
     * Sets ComponentId for Invocation.
     */
    void setComponentId(String compEnvId) {
        this.compEnvId = compEnvId;
    }

    /**
     * Gets ServerContext.
     */
    public ServerContext getServerContext() {
        return serverContext;
    }

    /**
     * Sets ServerContext.
     */
    void setServerContext(ServerContext serverContext) {
        this.serverContext = serverContext;
    }

    /**
     * Sets the alternate docroots of this web module from the given
     * "alternatedocroot_" properties.
     */
    void setAlternateDocBases(List <Property> props) {

        if (props == null) {
            return;
        }

        for (Property prop : props) {
            parseAlternateDocBase(prop.getName(), prop.getValue());
        }
    }

    void parseAlternateDocBase(String propName, String propValue) {

        if (propName == null || propValue == null) {
            logger.log(Level.WARNING, LogFacade.ALTERNATE_DOC_BASE_NULL_PROPERTY_NAME_VALVE);
            return;
        }

        if (!propName.startsWith("alternatedocroot_")) {
            return;
        }

        /*
         * Validate the prop value
         */
        String urlPattern = null;
        String docBase = null;

        int fromIndex = propValue.indexOf(ALTERNATE_FROM);
        int dirIndex = propValue.indexOf(ALTERNATE_DOCBASE);

        if (fromIndex < 0 || dirIndex < 0) {
            logger.log(
                Level.WARNING,
                LogFacade.ALTERNATE_DOC_BASE_MISSING_PATH_OR_URL_PATTERN,
                propValue);
            return;
        }

        if (fromIndex > dirIndex) {
            urlPattern = propValue.substring(
                fromIndex + ALTERNATE_FROM.length());
            docBase = propValue.substring(
                dirIndex + ALTERNATE_DOCBASE.length(),
                fromIndex);
        } else {
            urlPattern = propValue.substring(
                fromIndex + ALTERNATE_FROM.length(),
                dirIndex);
            docBase = propValue.substring(
                dirIndex + ALTERNATE_DOCBASE.length());
        }

        urlPattern = urlPattern.trim();
        if (!validateURLPattern(urlPattern)) {
            logger.log(Level.WARNING,
                       LogFacade.ALTERNATE_DOC_BASE_ILLEGAL_URL_PATTERN,
                       urlPattern);
            return;
        }

        docBase = docBase.trim();

        addAlternateDocBase(urlPattern, docBase);
    }

    List<URI> getDeployAppLibs() {
        List<URI> uris = null;
        if (wmInfo.getDeploymentContext() != null) {
            try {
                uris = wmInfo.getDeploymentContext().getAppLibs();
            } catch(URISyntaxException e) {
                throw new IllegalArgumentException(e);
            }
        }
        return uris;
    }


    /**
     * Configure miscellaneous settings such as the pool size for
     * single threaded servlets, specifying a temporary directory other
     * than the default etc.
     *
     * Since the work directory is used when configuring the session manager
     * persistence settings, this method must be invoked prior to
     * <code>configureSessionSettings</code>.
     */
    void configureMiscSettings(SunWebAppImpl bean, VirtualServer vs,
                               String contextPath) {

        /*
         * Web app inherits setting of allowLinking property from vs on which
         * it is being deployed, but may override it using allowLinking
         * property in its sun-web.xml
         */
        boolean allowLinking = vs.getAllowLinking();

        if ((bean != null) && (bean.sizeWebProperty() > 0)) {
            WebProperty[] props = bean.getWebProperty();
            for (WebProperty prop : props) {
                String name = prop.getAttributeValue("name");
                String value = prop.getAttributeValue("value");
                if (name == null || value == null) {
                    throw new IllegalArgumentException(
                            rb.getString(LogFacade.NULL_WEB_MODULE_PROPERTY));
                }
                if ("singleThreadedServletPoolSize".equalsIgnoreCase(name)) {
                    int poolSize = getSTMPoolSize();
                    try {
                        poolSize = Integer.parseInt(value);
                    } catch(NumberFormatException e) {
                        String msg = rb.getString(LogFacade.INVALID_SERVLET_POOL_SIZE);
                        msg = MessageFormat.format(msg, value, contextPath, Integer.toString(poolSize));
                        logger.log(Level.WARNING, msg, e);
                    }
                    if (poolSize > 0) {
                        setSTMPoolSize(poolSize);
                    }

                } else if("tempdir".equalsIgnoreCase(name)) {
                    setWorkDir(value);
                } else if("crossContextAllowed".equalsIgnoreCase(name)) {
                    boolean crossContext = Boolean.parseBoolean(value);
                    setCrossContext(crossContext);
                } else if("allowLinking".equalsIgnoreCase(name)) {
                    allowLinking = ConfigBeansUtilities.toBoolean(value);
                    // START S1AS8PE 4817642
                } else if("reuseSessionID".equalsIgnoreCase(name)) {
                    boolean reuse = ConfigBeansUtilities.toBoolean(value);
                    setReuseSessionID(reuse);
                    if (reuse) {
                        String msg = rb.getString(LogFacade.SESSION_IDS_REUSED);
                        msg = MessageFormat.format(msg, contextPath, vs.getID());
                        logger.log(Level.WARNING, msg);
                    }
                    // END S1AS8PE 4817642
                } else if("useResponseCTForHeaders".equalsIgnoreCase(name)) {
                    if("true".equalsIgnoreCase(value)) {
                        setResponseCTForHeaders();
                    }
                } else if("encodeCookies".equalsIgnoreCase(name)) {
                    boolean flag = ConfigBeansUtilities.toBoolean(value);
                    setEncodeCookies(flag);
                    // START RIMOD 4642650
                } else if("relativeRedirectAllowed".equalsIgnoreCase(name)) {
                    boolean relativeRedirect = ConfigBeansUtilities.toBoolean(value);
                    setAllowRelativeRedirect(relativeRedirect);
                    // END RIMOD 4642650
                } else if("fileEncoding".equalsIgnoreCase(name)) {
                    setFileEncoding(value);
                } else if("enableTldValidation".equalsIgnoreCase(name)
                    && ConfigBeansUtilities.toBoolean(value)) {
                    setTldValidation(true);
                } else if("enableTldNamespaceAware".equalsIgnoreCase(name)
                    && ConfigBeansUtilities.toBoolean(value)) {
                    setTldNamespaceAware(true);
                } else if("securePagesWithPragma".equalsIgnoreCase(name)) {
                    boolean securePagesWithPragma = ConfigBeansUtilities.toBoolean(value);
                    setSecurePagesWithPragma(securePagesWithPragma);
                } else if("useMyFaces".equalsIgnoreCase(name)) {
                    setUseMyFaces(ConfigBeansUtilities.toBoolean(value));
                } else if("useBundledJsf".equalsIgnoreCase(name)) {
                    setUseMyFaces(ConfigBeansUtilities.toBoolean(value));
                } else if("default-role-mapping".equalsIgnoreCase(name)) {
                    wmInfo.getDescriptor().setDefaultGroupPrincipalMapping(ConfigBeansUtilities.toBoolean(value));
                } else if(name.startsWith("alternatedocroot_")) {
                    parseAlternateDocBase(name, value);
                } else if(name.startsWith("valve_") ||
                        name.startsWith("listener_")) {
                    // do nothing; these properties are dealt with
                    // in configureCatalinaProperties()
                } else {
                    Object[] params = {name, value};
                    logger.log(Level.WARNING, LogFacade.INVALID_PROPERTY,
                        params);
                }
            }
        }

        setAllowLinking(allowLinking);
    }

    /**
     * Determines and sets the alternate deployment descriptor for
     * this web module.
     */
    void configureAlternateDD(WebBundleDescriptor wbd) {

        String altDDName =
            wbd.getModuleDescriptor().getAlternateDescriptor();
        if (altDDName == null) {
            return;
        }

        com.sun.enterprise.deployment.Application app = wbd.getApplication();
        if (app == null || app.isVirtual()) {
            // Alternate deployment descriptors are only supported for
            // WAR files embedded inside EAR files
            return;
        }

        DeploymentContext dc = getWebModuleConfig().getDeploymentContext();
        if (dc == null) {
            return;
        }

        altDDName = altDDName.trim();
        if (altDDName.startsWith("/")) {
            altDDName = altDDName.substring(1);
        } 
      
        String appLoc = dc.getSource().getParentArchive().getURI().getPath();
        altDDName = appLoc + altDDName;

        if (logger.isLoggable(Level.FINE)) {
            Object[] objs = {altDDName, wmInfo.getName()};
            logger.log(Level.FINE, LogFacade.ALT_DD_NAME, objs);
        }

        setAltDDName(altDDName);
    }

    /*
     * Configures this web module with its web services, based on its
     * "hasWebServices" and "endpointAddresses" properties
     */
    void configureWebServices(WebBundleDescriptor wbd) {

        if (wbd.hasWebServices()) {

            setHasWebServices(true);

            // creates the list of endpoint addresses
            String[] endpointAddresses;
            WebServicesDescriptor webService = wbd.getWebServices();
            Vector<String> endpointList = new Vector<String>();
            for(WebServiceEndpoint wse : webService.getEndpoints()) {
                if(wbd.getContextRoot() != null) {
                    endpointList.add(wbd.getContextRoot() + "/" +
                        wse.getEndpointAddressUri());
                } else {
                    endpointList.add(wse.getEndpointAddressUri());
                }
            }
            endpointAddresses = new String[endpointList.size()];
            endpointList.copyInto(endpointAddresses);

            setEndpointAddresses(endpointAddresses);

        } else {
            setHasWebServices(false);
        }
    }

    /**
     * Configure the class loader for the web module based on the
     * settings in sun-web.xml's class-loader element (if any).
     */
    Loader configureLoader(SunWebApp bean) {

        org.glassfish.web.deployment.runtime.ClassLoader clBean = null;

        WebappLoader loader = new V3WebappLoader(wmInfo.getAppClassLoader());

        loader.setUseMyFaces(isUseMyFaces());

        if (bean != null) {
            clBean = ((SunWebAppImpl)bean).getClassLoader();
        }
        if (clBean != null) {
            configureLoaderAttributes(loader, clBean);
            configureLoaderProperties(loader, clBean);
        } else {
            loader.setDelegate(true);
        }

        // START S1AS 6178005
        String stubPath = wmInfo.getStubPath();
        if (stubPath != null && stubPath.length() > 0) {
            if (stubPath.charAt(0) != '/') {
                stubPath = "/" + stubPath;
            }
            loader.addRepository("file:" + stubPath + File.separator);
        }
        // END S1AS 6178005

        // START PE 4985680
        /**
         * Adds the given package name to the list of packages that may
         * always be overriden, regardless of whether they belong to a
         * protected namespace
         */
        String packagesName =
                System.getProperty("com.sun.enterprise.overrideablejavaxpackages");

        if (packagesName != null) {
            List<String> overridablePackages =
                    StringUtils.parseStringList(packagesName, " ,");
            for(String overridablePackage : overridablePackages) {
                loader.addOverridablePackage(overridablePackage);
            }
        }
        // END PE 4985680

        setLoader(loader);

        return loader;
    }

    /**
     * Saves all active sessions to the given deployment context, so they
     * can be restored following a redeployment.
     *
     * @param props the deployment context properties to which to save the
     * sessions
     */
    void saveSessions(Properties props) {
        if (props == null) {
            return;
        }

        StandardManager manager = (StandardManager) getManager();
        if (manager == null) {
            return;
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            manager.writeSessions(baos, false);
            props.setProperty(getObjectName(), new String(encoder.encode(baos.toByteArray()), UTF_8));
        } catch (Exception ex) {
            logger.log(WARNING, format(rb.getString(UNABLE_TO_SAVE_SESSIONS_DURING_REDEPLOY), getName()), ex);
        }
    }

    /**
     * Loads any sessions that were stored in the given deployment context
     * prior to a redeployment of this web module.
     *
     * @param deploymentProperties the deployment context properties from
     * which to load the sessions
     */
    void loadSessions(Properties deploymentProperties) {
        if (deploymentProperties == null) {
            return;
        }

        StandardManager manager = (StandardManager) getManager();
        if (manager == null) {
            return;
        }

        String sessions = deploymentProperties.getProperty(getObjectName());
        if (sessions != null) {
            try {
                manager.readSessions(new ByteArrayInputStream(decoder.decode(sessions)));
            } catch (Exception ex) {
                logger.log(WARNING, format(rb.getString(UNABLE_TO_RESTORE_SESSIONS_DURING_REDEPLOY), getName()), ex);
            }
            deploymentProperties.remove(getObjectName());
        }
    }

    /**
     * Loads and instantiates the listener with the specified classname.
     *
     * @param loader the classloader to use
     * @param listenerClassName the fully qualified classname to instantiate
     *
     * @return the instantiated listener
     *
     * @throws Exception if the specified classname fails to be loaded or
     * instantiated
     */
    @Override
    protected EventListener loadListener(ClassLoader loader,
                                         String listenerClassName)
            throws Exception {
        try {
            return super.loadListener(loader, listenerClassName);
        } catch (Exception e) {
            if (WS_SERVLET_CONTEXT_LISTENER.equals(listenerClassName)) {
                logger.log(Level.WARNING, LogFacade.MISSING_METRO, e);
            }
            throw e;
        }
    }

    /**
     * Create and configure the session manager for this web application
     * according to the persistence type specified.
     *
     * Also configure the other aspects of session management for this
     * web application according to the values specified in the session-config
     * element of sun-web.xml (and whether app is distributable)
     */
    protected void configureSessionSettings(WebBundleDescriptor wbd,
                                            WebModuleConfig wmInfo) {

        SessionConfig cfg = null;
        SessionManager smBean = null;
        SessionProperties sessionPropsBean = null;
        CookieProperties cookieBean = null;

        if (iasBean != null) {
            cfg = iasBean.getSessionConfig();
            if (cfg != null) {
                smBean = cfg.getSessionManager();
                sessionPropsBean = cfg.getSessionProperties();
                cookieBean = cfg.getCookieProperties();
            }
        }

        configureSessionManager(smBean, wbd, wmInfo);
        configureSession(sessionPropsBean, wbd);
        configureCookieProperties(cookieBean);
    }

    /**
     * Configures the given classloader with its attributes specified in
     * sun-web.xml.
     *
     * @param loader The classloader to configure
     * @param clBean The class-loader info from sun-web.xml
     */
    private void configureLoaderAttributes(
            Loader loader,
            org.glassfish.web.deployment.runtime.ClassLoader clBean) {

        String value = clBean.getAttributeValue(
                org.glassfish.web.deployment.runtime.ClassLoader.DELEGATE);

        /*
         * The DOL will *always* return a value: If 'delegate' has not been
         * configured in sun-web.xml, its default value will be returned,
         * which is FALSE in the case of sun-web-app_2_2-0.dtd and
         * sun-web-app_2_3-0.dtd, and TRUE in the case of
         * sun-web-app_2_4-0.dtd.
         */
        boolean delegate = ConfigBeansUtilities.toBoolean(value);
        loader.setDelegate(delegate);
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, LogFacade.SETTING_DELEGATE, new Object[]{getPath(), delegate});
        }

        // Get any extra paths to be added to the class path of this
        // class loader
        value = clBean.getAttributeValue(
                org.glassfish.web.deployment.runtime.ClassLoader.EXTRA_CLASS_PATH);
        if (value != null) {
            // Parse the extra classpath into its ':' and ';' separated
            // components. Ignore ':' as a separator if it is preceded by
            // '\'
            String[] pathElements = value.split(";|((?<!\\\\):)");
            if (pathElements != null) {
                for (String path : pathElements) {
                    path = path.replace("\\:", ":");
                    if (logger.isLoggable(Level.FINE)) {
                        logger.log(Level.FINE, LogFacade.ADDING_CLASSPATH, new Object[]{getPath(), path});
                    }

                    try {
                        URL url = new URL(path);
                        loader.addRepository(path);
                    } catch (MalformedURLException mue1) {
                        // Not a URL, interpret as file
                        File file = new File(path);
                        // START GlassFish 904
                        if (!file.isAbsolute()) {
                            // Resolve relative extra class path to the
                            // context's docroot
                            file = new File(getDocBase(), path);
                        }
                        // END GlassFish 904

                        try {
                            URL url = file.toURI().toURL();
                            loader.addRepository(url.toString());
                        } catch (MalformedURLException mue2) {
                            String msg = rb.getString(LogFacade.CLASSPATH_ERROR);
                            Object[] params = { path };
                            msg = MessageFormat.format(msg, params);
                            logger.log(Level.SEVERE, msg, mue2);
                        }
                    }
                }
            }
        }

        value = clBean.getAttributeValue(
                org.glassfish.web.deployment.runtime.ClassLoader.DYNAMIC_RELOAD_INTERVAL);
        if (value != null) {
            // Log warning if dynamic-reload-interval is specified
            // in sun-web.xml since it is not supported
            logger.log(Level.WARNING, LogFacade.DYNAMIC_RELOAD_INTERVAL);
        }
    }

    /**
     * Configures the given classloader with its properties specified in
     * sun-web.xml.
     *
     * @param loader The classloader to configure
     * @param clBean The class-loader info from sun-web.xml
     */
    private void configureLoaderProperties(
            Loader loader,
            org.glassfish.web.deployment.runtime.ClassLoader clBean) {

        String name = null;
        String value = null;

        WebProperty[] props = clBean.getWebProperty();
        if (props == null || props.length == 0) {
            return;
        }
        for(WebProperty prop : props) {
            name = prop.getAttributeValue(WebProperty.NAME);
            value = prop.getAttributeValue(WebProperty.VALUE);
            if(name == null || value == null) {
                throw new IllegalArgumentException(rb.getString(LogFacade.NULL_WEB_MODULE_PROPERTY));
            }
            if("ignoreHiddenJarFiles".equalsIgnoreCase(name)) {
                loader.setIgnoreHiddenJarFiles(ConfigBeansUtilities.toBoolean(value));
            } else {
                Object[] params = {name, value};
                logger.log(Level.WARNING, LogFacade.INVALID_PROPERTY,
                    params);
            }
        }
    }

    /**
     * Configure the session manager according to the persistence-type
     * specified in the <session-manager> element and the related
     * settings in the <manager-properties> and <store-properties> elements
     * in sun-web.xml.
     */
    private void configureSessionManager(SessionManager smBean,
                                         WebBundleDescriptor wbd,
                                         WebModuleConfig wmInfo) {

        SessionManagerConfigurationHelper configHelper =
            new SessionManagerConfigurationHelper(
                this, smBean, wbd, wmInfo,
                webContainer.getServerConfigLookup());

        PersistenceType persistence = configHelper.getPersistenceType();
        String frequency = configHelper.getPersistenceFrequency();
        String scope = configHelper.getPersistenceScope();

        if (logger.isLoggable(Level.FINEST)) {
            logger.log(Level.FINEST, LogFacade.CONFIGURE_SESSION_MANAGER, new Object[]{persistence.getType(), frequency, scope});
        }

        PersistenceStrategyBuilderFactory factory =
            new PersistenceStrategyBuilderFactory(
                webContainer.getServerConfigLookup(), services);
        PersistenceStrategyBuilder builder =
            factory.createPersistenceStrategyBuilder(persistence.getType(),
                                                     frequency, scope, this);
        if (logger.isLoggable(Level.FINEST)) {
            logger.log(Level.FINEST, LogFacade.PERSISTENCE_STRATEGY_BUILDER, builder.getClass().getName());
        }

        builder.initializePersistenceStrategy(this, smBean,
            webContainer.getServerConfigLookup());
    }

    @Override
    protected ServletRegistrationImpl createServletRegistrationImpl(
            StandardWrapper wrapper) {
        return new WebServletRegistrationImpl(wrapper, this);
    }

    @Override
    protected ServletRegistrationImpl createDynamicServletRegistrationImpl(
            StandardWrapper wrapper) {
        return new DynamicWebServletRegistrationImpl(wrapper, this);
    }

    @Override
    protected void removePatternFromServlet(Wrapper wrapper, String pattern) {
        super.removePatternFromServlet(wrapper, pattern);
        WebBundleDescriptor wbd = getWebBundleDescriptor();
        if (wbd == null) {
            throw new IllegalStateException(
                "Missing WebBundleDescriptor for " + getName());
        }
        WebComponentDescriptor wcd =
            wbd.getWebComponentByCanonicalName(wrapper.getName());
        if (wcd == null) {
            throw new IllegalStateException(
                "Missing WebComponentDescriptor for " + wrapper.getName());
        }
        wcd.removeUrlPattern(pattern);
    }

    /**
     * Configure the properties of the session, such as the timeout,
     * whether to force URL rewriting etc.
     * HERCULES:mod passing in new param wbd
     */
    private void configureSession(SessionProperties spBean,
                                  WebBundleDescriptor wbd) {

        boolean timeoutConfigured = false;
        int timeoutSeconds = 1800; // tomcat default (see StandardContext)

        setCookies(webContainer.instanceEnableCookies);

        if ((spBean != null) && (spBean.sizeWebProperty() > 0)) {
            for(WebProperty prop : spBean.getWebProperty()) {
                String name = prop.getAttributeValue(WebProperty.NAME);
                String value = prop.getAttributeValue(WebProperty.VALUE);
                if(name == null || value == null) {
                    throw new IllegalArgumentException(rb.getString(LogFacade.NULL_WEB_MODULE_PROPERTY));
                }
                if("timeoutSeconds".equalsIgnoreCase(name)) {
                    try {
                        timeoutSeconds = Integer.parseInt(value);
                        timeoutConfigured = true;
                    } catch(NumberFormatException e) {
                        // XXX need error message
                    }
                } else if("enableCookies".equalsIgnoreCase(name)) {
                    setCookies(ConfigBeansUtilities.toBoolean(value));
                } else if("enableURLRewriting".equalsIgnoreCase(name)) {
                    setEnableURLRewriting(ConfigBeansUtilities.toBoolean(value));
                } else {
                    if (logger.isLoggable(Level.INFO)) {
                        logger.log(Level.INFO, LogFacade.PROP_NOT_YET_SUPPORTED, name);
                    }
                }
            }
        }

        int webXmlTimeoutSeconds = -1;
        if (wbd != null) {
            webXmlTimeoutSeconds = wbd.getSessionConfig().getSessionTimeout() * 60;
        }

        //web.xml setting has precedence if it exists
        //ignore if the value is the 30 min default
        if (webXmlTimeoutSeconds != -1 && webXmlTimeoutSeconds != 1800) {
            getManager().setMaxInactiveIntervalSeconds(webXmlTimeoutSeconds);
        } else {
            /*
             * Do not override Tomcat default, unless 'timeoutSeconds' was
             * specified in sun-web.xml
             */
            if (timeoutConfigured) {
                getManager().setMaxInactiveIntervalSeconds(timeoutSeconds);
            }
        }
    }

    /**
     * Configure the settings for the session cookie using the values
     * in sun-web.xml's cookie-property
     */
    private void configureCookieProperties(CookieProperties bean) {
        if (bean != null) {
            WebProperty[] props = bean.getWebProperty();
            if (props != null) {
                SessionCookieConfig cookieConfig = new SessionCookieConfig();
                for(WebProperty prop : props) {
                    String name = prop.getAttributeValue(WebProperty.NAME);
                    String value = prop.getAttributeValue(WebProperty.VALUE);
                    if(name == null || value == null) {
                        throw new IllegalArgumentException(rb.getString(LogFacade.NULL_WEB_MODULE_PROPERTY));
                    }
                    if("cookieName".equalsIgnoreCase(name)) {
                        cookieConfig.setName(value);
                    } else if("cookiePath".equalsIgnoreCase(name)) {
                        cookieConfig.setPath(value);
                    } else if("cookieMaxAgeSeconds".equalsIgnoreCase(name)) {
                        try {
                            cookieConfig.setMaxAge(Integer.parseInt(value));
                        } catch(NumberFormatException e) {
                            // XXX need error message
                        }
                    } else if("cookieDomain".equalsIgnoreCase(name)) {
                        cookieConfig.setDomain(value);
                    } else if("cookieComment".equalsIgnoreCase(name)) {
                        cookieConfig.setComment(value);
                    } else if("cookieSecure".equalsIgnoreCase(name)) {
                        cookieConfig.setSecure(value);
                    } else if("cookieHttpOnly".equalsIgnoreCase(name)) {
                        cookieConfig.setHttpOnly(Boolean.valueOf(value));
                    } else {
                        Object[] params = {name, value};
                        logger.log(Level.WARNING,
                            LogFacade.INVALID_PROPERTY,
                            params);
                    }
                }
                if (props.length > 0) {
                    if (logger.isLoggable(Level.FINE)) {
                        logger.log(Level.FINE, LogFacade.CONFIGURE_COOKIE_PROPERTIES, new Object[]{getPath(), cookieConfig});
                    }
                    setSessionCookieConfigFromSunWebXml(cookieConfig);
                }
            }
        }
    }

    /**
     * Instantiates the given Servlet class.
     *
     * @return the new Servlet instance
     */
    @Override
    protected <T extends Servlet> T createServletInstance(Class<T> clazz)
            throws Exception {
        if (DefaultServlet.class.equals(clazz) ||
                JspServlet.class.equals(clazz) ||
                webContainer == null) {
            // Container-provided servlets, skip injection
            return super.createServletInstance(clazz);
        } else {
            return webContainer.createServletInstance(this, clazz);
        }
    }

    /**
     * Instantiates the given Filter class.
     *
     * @return the new Filter instance
     */
    @Override
    protected <T extends Filter> T createFilterInstance(Class<T> clazz)
            throws Exception {
        if (webContainer != null) {
            return webContainer.createFilterInstance(this, clazz);
        } else {
            return super.createFilterInstance(clazz);
        }
    }

    /**
     * Instantiates the given EventListener class.
     *
     * @return the new EventListener instance
     */
    @Override
    public <T extends EventListener> T createListenerInstance(
                Class<T> clazz) throws Exception {
        if (webContainer != null) {
            return webContainer.createListenerInstance(this, clazz);
        } else {
            return super.createListenerInstance(clazz);
        }
    }

    /**
     * Create an instance of a given class.
     *
     * @param clazz
     *
     * @return an instance of the given class
     * @throws Exception
     */
    @Override
    public <T extends HttpUpgradeHandler> T createHttpUpgradeHandlerInstance(Class<T> clazz) throws Exception {
        if (webContainer != null) {
            return webContainer.createHttpUpgradeHandlerInstance(this, clazz);
        } else {
            return super.createHttpUpgradeHandlerInstance(clazz);
        }
    }


    /*
     * Servlet related probe events
     */

    public void servletInitializedEvent(String servletName) {
        servletProbeProvider.servletInitializedEvent(servletName,
            monitoringNodeName, vsId);
    }

    public void servletDestroyedEvent(String servletName) {
        servletProbeProvider.servletDestroyedEvent(servletName,
            monitoringNodeName, vsId);
    }

    public void beforeServiceEvent(String servletName) {
        servletProbeProvider.beforeServiceEvent(servletName,
            monitoringNodeName, vsId);
    }

    public void afterServiceEvent(String servletName, int status) {
        servletProbeProvider.afterServiceEvent(servletName,
            status, monitoringNodeName, vsId);
    }


    /*
     * HTTP session related probe events
     */

    @Override
    public void sessionCreatedEvent(HttpSession session) {
        sessionProbeProvider.sessionCreatedEvent(session.getId(),
            monitoringNodeName, vsId);
    }

    @Override
    public void sessionDestroyedEvent(HttpSession session) {
        sessionProbeProvider.sessionDestroyedEvent(session.getId(),
            monitoringNodeName, vsId);
    }

    @Override
    public void sessionRejectedEvent(int maxSessions) {
        sessionProbeProvider.sessionRejectedEvent(maxSessions,
            monitoringNodeName, vsId);
    }

    @Override
    public void sessionExpiredEvent(HttpSession session) {
        sessionProbeProvider.sessionExpiredEvent(session.getId(),
            monitoringNodeName, vsId);
    }

    @Override
    public void sessionPersistedStartEvent(HttpSession session) {
        sessionProbeProvider.sessionPersistedStartEvent(session.getId(),
            monitoringNodeName, vsId);
    }

    @Override
    public void sessionPersistedEndEvent(HttpSession session) {
        sessionProbeProvider.sessionPersistedEndEvent(session.getId(),
            monitoringNodeName, vsId);
    }

    @Override
    public void sessionActivatedStartEvent(HttpSession session) {
        sessionProbeProvider.sessionActivatedStartEvent(session.getId(),
            monitoringNodeName, vsId);
    }

    @Override
    public void sessionActivatedEndEvent(HttpSession session) {
        sessionProbeProvider.sessionActivatedEndEvent(session.getId(),
            monitoringNodeName, vsId);
    }

    @Override
    public void sessionPassivatedStartEvent(HttpSession session) {
        sessionProbeProvider.sessionPassivatedStartEvent(session.getId(),
            monitoringNodeName, vsId);
    }

    @Override
    public void sessionPassivatedEndEvent(HttpSession session) {
        sessionProbeProvider.sessionPassivatedEndEvent(session.getId(),
            monitoringNodeName, vsId);
    }


    /*
     * Web module lifecycle related probe events
     */

    public void webModuleStartedEvent() {
        webModuleProbeProvider.webModuleStartedEvent(monitoringNodeName,
            vsId);
    }

    public void webModuleStoppedEvent() {
        webModuleProbeProvider.webModuleStoppedEvent(monitoringNodeName,
            vsId);
    }

    void processServletSecurityElement(ServletSecurityElement servletSecurityElement,
            WebBundleDescriptor wbd, WebComponentDescriptor wcd) {

        Set<String> urlPatterns =
                ServletSecurityHandler.getUrlPatternsWithoutSecurityConstraint(wcd);

        if (urlPatterns.size() > 0) {
            SecurityConstraint securityConstraint =
                ServletSecurityHandler.createSecurityConstraint(wbd,
                    urlPatterns, servletSecurityElement.getRolesAllowed(),
                    servletSecurityElement.getEmptyRoleSemantic(),
                    servletSecurityElement.getTransportGuarantee(),
                    null);

            //we know there is one WebResourceCollection there
            WebResourceCollection webResColl =
                    securityConstraint.getWebResourceCollections().iterator().next();
            for (HttpMethodConstraintElement httpMethodConstraintElement :
                    servletSecurityElement.getHttpMethodConstraints()) {
                String httpMethod = httpMethodConstraintElement.getMethodName();
                ServletSecurityHandler.createSecurityConstraint(wbd,
                        urlPatterns, httpMethodConstraintElement.getRolesAllowed(),
                        httpMethodConstraintElement.getEmptyRoleSemantic(),
                        httpMethodConstraintElement.getTransportGuarantee(),
                        httpMethod);

                //exclude this from the top level constraint
                webResColl.addHttpMethodOmission(httpMethod);
            }
        }
    }

    private SecurityConfig config = null;

    public SecurityConfig getSecurityConfig() {
        return config;
    }

    public void setSecurityConfig(SecurityConfig config) {

        if (config == null) {
            return;
        }
        this.config = config;

        LoginConfig lc = config.getLoginConfig();
        if (lc != null) {
            LoginConfiguration loginConf = new LoginConfigurationImpl();
            loginConf.setAuthenticationMethod(lc.getAuthMethod().name());
            loginConf.setRealmName(lc.getRealmName());

            FormLoginConfig form = lc.getFormLoginConfig();
            if (form != null) {
                loginConf.setFormErrorPage(form.getFormErrorPage());
                loginConf.setFormLoginPage(form.getFormLoginPage());
            }

            LoginConfigDecorator decorator = new LoginConfigDecorator(loginConf);
            setLoginConfig(decorator);
            getWebBundleDescriptor().setLoginConfiguration(loginConf);
        }

        Set<org.glassfish.embeddable.web.config.SecurityConstraint> securityConstraints =
                config.getSecurityConstraints();
        for (org.glassfish.embeddable.web.config.SecurityConstraint sc : securityConstraints) {

            com.sun.enterprise.deployment.web.SecurityConstraint securityConstraint = new SecurityConstraintImpl();

            Set<org.glassfish.embeddable.web.config.WebResourceCollection> wrcs =
                        sc.getWebResourceCollection();
            for (org.glassfish.embeddable.web.config.WebResourceCollection wrc : wrcs) {

                WebResourceCollectionImpl webResourceColl = new WebResourceCollectionImpl();
                webResourceColl.setDisplayName(wrc.getName());
                for (String urlPattern : wrc.getUrlPatterns()) {
                    webResourceColl.addUrlPattern(urlPattern);
                }
                securityConstraint.addWebResourceCollection(webResourceColl);

                AuthorizationConstraintImpl ac = null;
                if (sc.getAuthConstraint() != null && sc.getAuthConstraint().length > 0) {
                    ac = new AuthorizationConstraintImpl();
                    for (String roleName : sc.getAuthConstraint()) {
                        Role role = new Role(roleName);
                        getWebBundleDescriptor().addRole(role);
                        ac.addSecurityRole(roleName);
                    }
                } else { // DENY
                    ac = new AuthorizationConstraintImpl();
                }
                securityConstraint.setAuthorizationConstraint(ac);

                UserDataConstraint udc = new UserDataConstraintImpl();
                udc.setTransportGuarantee(
                        ((sc.getDataConstraint() == TransportGuarantee.CONFIDENTIAL) ?
                                UserDataConstraint.CONFIDENTIAL_TRANSPORT :
                                UserDataConstraint.NONE_TRANSPORT));
                securityConstraint.setUserDataConstraint(udc);

                if (wrc.getHttpMethods() != null) {
                    for (String httpMethod : wrc.getHttpMethods()) {
                        webResourceColl.addHttpMethod(httpMethod);
                    }
                }

                if (wrc.getHttpMethodOmissions() != null) {
                    for (String httpMethod :  wrc.getHttpMethodOmissions()) {
                        webResourceColl.addHttpMethodOmission(httpMethod);
                    }
                }

                getWebBundleDescriptor().addSecurityConstraint(securityConstraint);
                TomcatDeploymentConfig.configureSecurityConstraint(this, getWebBundleDescriptor());
            }
        }

        if (pipeline != null) {
            GlassFishValve basic = pipeline.getBasic();
            if ((basic != null) && (basic instanceof java.net.Authenticator)) {
                removeValve(basic);
            }
            GlassFishValve valves[] = pipeline.getValves();
            for (int i = 0; i < valves.length; i++) {
                if (valves[i] instanceof java.net.Authenticator) {
                    removeValve(valves[i]);
                }
            }
        }

        if (realm != null && realm instanceof RealmInitializer) {
            ((RealmInitializer) realm).initializeRealm(
                    this.getWebBundleDescriptor(),
                    false,
                    ((VirtualServer)parent).getAuthRealmName());
            ((RealmInitializer)realm).setVirtualServer(getParent());
            ((RealmInitializer)realm).updateWebSecurityManager();
            setRealm(realm);
        }
    }
    
    @Override
    public long getUniqueId() {
        com.sun.enterprise.deployment.Application app = wmInfo.getDescriptor().getApplication();
        return app != null? app.getUniqueId() : 0L;
    }
}

class V3WebappLoader extends WebappLoader {

    final ClassLoader cl;

    V3WebappLoader(ClassLoader cl) {
        this.cl = cl;
    }

    @Override
    protected ClassLoader createClassLoader() throws Exception {
        return cl;
    }

    /**
     * Stops the nested classloader
     */
    @Override
    public void stopNestedClassLoader() {
        // Do nothing. The nested (Webapp)ClassLoader is stopped in
        // WebApplication.stop()
    }
}

/**
 * Implementation of javax.servlet.ServletRegistration whose addMapping also
 * updates the WebBundleDescriptor from the deployment backend.
 */
class WebServletRegistrationImpl extends ServletRegistrationImpl {

    public WebServletRegistrationImpl(StandardWrapper wrapper,
                                      StandardContext context) {
        super(wrapper, context);
    }

    @Override
    public Set<String> addMapping(String... urlPatterns) {
        Set<String> conflicts = super.addMapping(urlPatterns);
        if (conflicts.isEmpty() && urlPatterns != null &&
                urlPatterns.length > 0) {
            /*
             * Propagate the new mappings to the underlying 
             * WebBundleDescriptor provided by the deployment backend,
             * so that corresponding security constraints may be calculated
             * by the security subsystem, which uses the
             * WebBundleDescriptor as its input
             */
            WebBundleDescriptor wbd = ((WebModule) getContext()).getWebBundleDescriptor();
            if (wbd == null) {
                throw new IllegalStateException(
                    "Missing WebBundleDescriptor for " + getContext().getName());
            }
            WebComponentDescriptor wcd =
                wbd.getWebComponentByCanonicalName(getName());
            if (wcd == null) {
                throw new IllegalStateException(
                    "Missing WebComponentDescriptor for " + getName());
            }
            for (String urlPattern : urlPatterns) {
                wcd.addUrlPattern(urlPattern);
            }
        }
        return conflicts;
    }
}

/**
 * Implementation of ServletRegistration.Dynamic whose addMapping also
 * updates the WebBundleDescriptor from the deployment backend.
 */
class DynamicWebServletRegistrationImpl
        extends DynamicServletRegistrationImpl {

    private WebBundleDescriptor wbd;
    private WebComponentDescriptor wcd;
    private WebModule webModule;

    private String runAsRoleName = null;
    private ServletSecurityElement servletSecurityElement = null;

    public DynamicWebServletRegistrationImpl(StandardWrapper wrapper, 
                                             WebModule webModule) {
        super(wrapper, webModule);
        this.webModule = webModule;
        wbd = webModule.getWebBundleDescriptor();
        if (wbd == null) {
            throw new IllegalStateException(
                "Missing WebBundleDescriptor for " +
                getContext().getName());
        }
        wbd.setPolicyModified(true);
        wcd = wbd.getWebComponentByCanonicalName(wrapper.getName());
        if (wcd == null) {
            /*
             * Servlet not present in the WebBundleDescriptor provided
             * by the deployment backend, which means we are dealing with
             * the dynamic registration for a programmtically added Servlet,
             * as opposed to the dynamic registration for a Servlet with a
             * preliminary declaration (that is, one without any class name).
             *
             * Propagate the new Servlet to the WebBundleDescriptor, so that
             * corresponding security constraints may be calculated by the
             * security subsystem, which uses the WebBundleDescriptor as its
             * input.
             */
            wcd = new WebComponentDescriptorImpl();
            wcd.setName(wrapper.getName());
            wcd.setCanonicalName(wrapper.getName());
            wbd.addWebComponentDescriptor(wcd);
            String servletClassName = wrapper.getServletClassName();
            if (servletClassName != null) {
                Class<? extends Servlet> clazz = wrapper.getServletClass();
                if (clazz == null) {
                    if (wrapper.getServlet() != null) {
                        clazz = wrapper.getServlet().getClass();
                    } else {                  
                        try {
                            clazz = loadServletClass(servletClassName);
                        } catch(Exception ex) {
                            throw new IllegalArgumentException(ex);
                        }
                    }
                    wrapper.setServletClass(clazz);
                }
                processServletAnnotations(clazz, wbd, wcd, wrapper);
            } else if (wrapper.getJspFile() == null) {
                // Should never happen
                throw new RuntimeException(
                    "Programmatic servlet registration without any " +
                    "supporting servlet class or jsp file");
            }
        }
    }

    @Override
    public Set<String> addMapping(String... urlPatterns) {
        Set<String> conflicts = super.addMapping(urlPatterns);
        if (conflicts.isEmpty() && urlPatterns != null &&
                urlPatterns.length > 0) {
            /*
             * Propagate the new mappings to the underlying 
             * WebBundleDescriptor provided by the deployment backend,
             * so that corresponding security constraints may be calculated
             * by the security subsystem, which uses the
             * WebBundleDescriptor as its input
             */
            for (String urlPattern : urlPatterns) {
                wcd.addUrlPattern(urlPattern);
            }
        }
        return conflicts;
    }

    @Override
    public void setRunAsRole(String roleName) {
        super.setRunAsRole(roleName);
        // postpone processing as we can only setRunAsIdentity in WebComponentDescriptor once
        this.runAsRoleName = roleName;
    }

    @Override
    public Set<String> setServletSecurity(ServletSecurityElement constraint) {
        this.servletSecurityElement = constraint;

        Set<String> conflictUrls = new HashSet<String>(wcd.getUrlPatternsSet());
        conflictUrls.removeAll(ServletSecurityHandler.getUrlPatternsWithoutSecurityConstraint(wcd));
        conflictUrls.addAll(super.setServletSecurity(constraint));
        return conflictUrls;
    }

    /**
     * Completes preliminary servlet registration by loading the class with
     * the given name and scanning it for servlet annotations
     */
    @Override
    protected void setServletClassName(String className) {
        super.setServletClassName(className);
        try {
            Class <? extends Servlet> clazz = loadServletClass(className);
            super.setServletClass(clazz);
            processServletAnnotations(clazz, wbd, wcd, wrapper);
        } catch(Exception ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    /**
     * Completes preliminary servlet registration by scanning the supplied
     * servlet class for servlet annotations
     */
    @Override
    protected void setServletClass(Class <? extends Servlet> clazz) {
        super.setServletClass(clazz);
        processServletAnnotations(clazz, wbd, wcd, wrapper);
    }

    private void processServletAnnotations(
            Class <? extends Servlet> clazz,
            WebBundleDescriptor webBundleDescriptor,
            WebComponentDescriptor wcd, StandardWrapper wrapper) {

        // Process DeclareRoles annotation
        if (clazz.isAnnotationPresent(DeclareRoles.class)) {
            DeclareRoles declareRoles = (DeclareRoles)
                clazz.getAnnotation(DeclareRoles.class);
            for (String roleName : declareRoles.value()) {
                webBundleDescriptor.addRole(new Role(roleName));
                webModule.declareRoles(roleName);
            }
        }
        // Process MultipartConfig annotation
        if (clazz.isAnnotationPresent(MultipartConfig.class)) {
            MultipartConfig mpConfig = (MultipartConfig)
                clazz.getAnnotation(MultipartConfig.class);
            wrapper.setMultipartLocation(mpConfig.location());
            wrapper.setMultipartMaxFileSize(mpConfig.maxFileSize());
            wrapper.setMultipartMaxRequestSize(mpConfig.maxRequestSize());
            wrapper.setMultipartFileSizeThreshold(
                mpConfig.fileSizeThreshold());
        }
    }

    void postProcessAnnotations() {
        Class<? extends Servlet> clazz = wrapper.getServletClass();
		if (clazz == null) {
			return;
		}

        // Process RunAs
        if (wcd.getRunAsIdentity() == null) {
            String roleName = runAsRoleName;
            if (roleName == null && clazz.isAnnotationPresent(RunAs.class)) {
                RunAs runAs = (RunAs)clazz.getAnnotation(RunAs.class);
                roleName = runAs.value();
            }
            if (roleName != null) {
                super.setRunAsRole(roleName);

                wbd.addRole(new Role(roleName));
                RunAsIdentityDescriptor runAsDesc =
                    new RunAsIdentityDescriptor();
                runAsDesc.setRoleName(roleName);
                wcd.setRunAsIdentity(runAsDesc);
            }
        }

        // Process ServletSecurity
        ServletSecurityElement ssElement = servletSecurityElement;
        if (servletSecurityElement == null &&
                clazz.isAnnotationPresent(ServletSecurity.class)) {
            ServletSecurity servletSecurity = (ServletSecurity)
                clazz.getAnnotation(ServletSecurity.class);
            ssElement = new ServletSecurityElement(servletSecurity);
        }
        if (ssElement != null) {
            webModule.processServletSecurityElement(
                    ssElement, wbd, wcd);
        }
    }

    @SuppressWarnings("unchecked")
    private Class<? extends Servlet> loadServletClass(String className)
            throws ClassNotFoundException {
        return (Class <? extends Servlet>)
                ctx.getLoader().getClassLoader().loadClass(className);
    }
}
