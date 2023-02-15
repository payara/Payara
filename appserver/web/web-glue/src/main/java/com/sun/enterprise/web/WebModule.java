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
// Portions Copyright [2016-2022] [Payara Foundation and/or its affiliates]

package com.sun.enterprise.web;

import com.sun.enterprise.config.serverbeans.Application;
import com.sun.enterprise.config.serverbeans.ConfigBeansUtilities;
import com.sun.enterprise.config.serverbeans.ServerTags;
import com.sun.enterprise.container.common.spi.util.JavaEEIOUtils;
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
import com.sun.enterprise.web.deploy.LoginConfigDecorator;
import com.sun.enterprise.web.pwc.PwcWebModule;
import com.sun.enterprise.web.session.PersistenceType;
import com.sun.enterprise.web.session.SessionCookieConfig;
import com.sun.web.security.RealmAdapter;
import fish.payara.jacc.JaccConfigurationFactory;
import fish.payara.web.WebModuleInstanceManager;
import fish.payara.web.WebModuleValve;
import jakarta.security.jacc.PolicyConfigurationFactory;
import jakarta.security.jacc.PolicyContextException;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterRegistration;
import jakarta.servlet.HttpMethodConstraintElement;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRegistration;
import jakarta.servlet.ServletSecurityElement;
import jakarta.servlet.SessionTrackingMode;
import jakarta.servlet.annotation.HandlesTypes;
import org.apache.catalina.Container;
import org.apache.catalina.ContainerListener;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Realm;
import org.apache.catalina.Valve;
import org.apache.catalina.core.DefaultInstanceManager;
import org.apache.catalina.deploy.NamingResourcesImpl;
import org.apache.catalina.servlets.DefaultServlet;
import org.glassfish.wasp.servlet.JspServlet;
import org.apache.tomcat.InstanceManager;
import org.apache.tomcat.util.descriptor.web.FilterMap;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.embeddable.web.Context;
import org.glassfish.embeddable.web.config.FormLoginConfig;
import org.glassfish.embeddable.web.config.LoginConfig;
import org.glassfish.embeddable.web.config.SecurityConfig;
import org.glassfish.embeddable.web.config.TransportGuarantee;
import org.glassfish.hk2.api.ServiceLocator;
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
import org.glassfish.web.deployment.descriptor.WebResourceCollectionImpl;
import org.glassfish.web.deployment.runtime.CookieProperties;
import org.glassfish.web.deployment.runtime.LocaleCharsetInfo;
import org.glassfish.web.deployment.runtime.LocaleCharsetMap;
import org.glassfish.web.deployment.runtime.SessionConfig;
import org.glassfish.web.deployment.runtime.SessionManager;
import org.glassfish.web.deployment.runtime.SessionProperties;
import org.glassfish.web.deployment.runtime.SunWebAppImpl;
import org.glassfish.web.deployment.runtime.WebProperty;
import org.glassfish.web.deployment.runtime.WebPropertyContainer;
import org.glassfish.web.loader.ServletContainerInitializerUtil;
import org.jvnet.hk2.config.types.Property;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Base64;
import java.util.Collections;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.sun.enterprise.security.ee.SecurityUtil.getContextID;
import static com.sun.enterprise.web.Constants.DEPLOYMENT_CONTEXT_ATTRIBUTE;
import static com.sun.enterprise.web.Constants.ENABLE_HA_ATTRIBUTE;
import static com.sun.enterprise.web.Constants.IS_DISTRIBUTABLE_ATTRIBUTE;

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

    private final ServiceLocator services;

    private WebModuleValve webModuleValve;

    // Originally from forked StandardContext
    protected boolean directoryDeployed = false;

    // Originally from forked StandardContext, refers to option in weblogic.xml
    protected boolean showArchivedRealPathEnabled = true;

    private boolean available = true;

    private String engineName;

    private String jvmRoute;

    /**
     * Constructor.
     */
    public WebModule() {
        this(null);
    }

    public WebModule(ServiceLocator services) {
        super();
        this.services = services;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    @Override
    public InstanceManager createInstanceManager() {
        return new WebModuleInstanceManager(this);
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
     * Sets the parameter encoding (i18n) info from web.xml, sun-web.xml, glassfish-web.xml and payara-web.xml.
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

    @Override
    public synchronized void initInternal() throws LifecycleException {
        super.initInternal();

        // Add our valve here instead of during construction, since we may want to perform lookup using the
        // ServiceLocator obtained from the ServerContext, which would be null during construction
        webModuleValve = new WebModuleValve(this);
        getPipeline().addValve(webModuleValve);
    }

    /**
     * Starts this web module.
     */
    @Override
    public synchronized void startInternal() throws LifecycleException {
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

        for (ServletContainerInitializer initializer : allInitializers) {
            // Check if getInterestList is required here - config of servletContainerInitializers interest list
            // is also done in ContextConfig#processServletContainerInitializers which gets called in response to
            // LifeCycle.CONFIGURE_START_EVENT which gets fired during super.startInternal
            addServletContainerInitializer(initializer, getInterestList(initializer));
        }

        DeploymentContext dc = getWebModuleConfig().getDeploymentContext();
        if (dc != null) {
            directoryDeployed =
                    Boolean.valueOf(dc.getAppProps().getProperty(ServerTags.DIRECTORY_DEPLOYED));
        }
        if (webBundleDescriptor != null) {
            showArchivedRealPathEnabled = webBundleDescriptor.isShowArchivedRealPathEnabled();
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
        super.startInternal();

        // Configure catalina listeners and valves. This can only happen
        // after this web module has been started, in order to be able to
        // load the specified listener and valve classes.
        configureValves();
        configureCatalinaProperties();
        webModuleStartedEvent();

        hasStarted = true;
    }

    /**
     * Creates a Set of the classes that a given {@link ServletContainerInitializer} has expressed interest in via
     * {@link HandlesTypes}
     *
     * @param initializer The {@link ServletContainerInitializer} to scan
     *
     * @return A {@link Set} of classes that the given {@link ServletContainerInitializer} has expressed interest in
     * via {@link HandlesTypes}
     */
    public static Set<Class<?>> getInterestList(ServletContainerInitializer initializer) {
        Class<? extends ServletContainerInitializer> sciClass = initializer.getClass();
        HandlesTypes ann = sciClass.getAnnotation(HandlesTypes.class);

        if (ann == null) {
            return null;
        }

        Class[] interestedClasses = ann.value();
        if (interestedClasses == null || interestedClasses.length == 0) {
            return null;
        }

        Set<Class<?>> interestSet = new HashSet<>();
        for (Class interestedClass : interestedClasses) {
            interestSet.add(interestedClass);
        }

        return interestSet;
    }

    @Override
    public String getRealPath(String path) {
        if (!(showArchivedRealPathEnabled || directoryDeployed)) {
            return null;
        }

        return super.getRealPath(path);
    }

    /**
     * Stops this web module.
     */
    @Override
    public void stopInternal() throws LifecycleException {
        // Unregister monitoring mbeans only if this web module was
        // successfully started, because if stop() is called during an
        // aborted start(), no monitoring mbeans will have been registered
        if (hasStarted) {
            webModuleStoppedEvent();
            hasStarted = false;
        }

        // Stop and unregister Tomcat mbeans
        super.stopInternal();
    }

    @Override
    public boolean listenerStart() {
        logger.finest("contextListenerStart()");
        ServletContext servletContext = getServletContext();
        WebBundleDescriptor bundleDescriptor = getWebBundleDescriptor();
        JaccConfigurationFactory jaccConfigurationFactory = getJaccConfigurationFactory();

        boolean ok = false;
        try {
            // For JSF injection
            servletContext.setAttribute(
                    DEPLOYMENT_CONTEXT_ATTRIBUTE,
                    getWebModuleConfig().getDeploymentContext());

            // Null check for OSGi/HTTP
            if (bundleDescriptor != null) {
                servletContext.setAttribute(IS_DISTRIBUTABLE_ATTRIBUTE, bundleDescriptor.isDistributable());
                bundleDescriptor.setAppContextId(getAppContextId(servletContext));
                if (jaccConfigurationFactory != null) {
                    // Add a mapping from the JACC context Id, which is not available to the application yet at this point
                    // to the Servlet based application Id, which the application uses
                    jaccConfigurationFactory.addContextIdMapping(
                            bundleDescriptor.getAppContextId(), // Servlet application context Id
                            getContextID(bundleDescriptor));    // JACC context Id
                }
            }

            servletContext.setAttribute(
                    ENABLE_HA_ATTRIBUTE,
                    webContainer.getServerConfigLookup().calculateWebAvailabilityEnabledFromConfig(this));

            ok = super.listenerStart();
        } finally {
            servletContext.removeAttribute(DEPLOYMENT_CONTEXT_ATTRIBUTE);
            servletContext.removeAttribute(IS_DISTRIBUTABLE_ATTRIBUTE);
            servletContext.removeAttribute(ENABLE_HA_ATTRIBUTE);
        }

        if (!ok) {
            return ok;
        }

        if (jaccConfigurationFactory != null && bundleDescriptor != null) {
            if (jaccConfigurationFactory.getContextProviderByPolicyContextId(getContextID(bundleDescriptor)) != null) {
                bundleDescriptor.setPolicyModified(true);
            }
        }

        webContainer.afterServletContextInitializedEvent(bundleDescriptor);

        return ok;
    }

    private String getAppContextId(ServletContext servletContext) {
        return servletContext.getVirtualServerName() + " " + servletContext.getContextPath();
    }

    private JaccConfigurationFactory getJaccConfigurationFactory() {
        try {
            PolicyConfigurationFactory policyConfigurationFactory = PolicyConfigurationFactory.getPolicyConfigurationFactory();
            if (policyConfigurationFactory instanceof JaccConfigurationFactory) {
                return (JaccConfigurationFactory) policyConfigurationFactory;
            }

        } catch (ClassNotFoundException | PolicyContextException e) {
            // Ignore
        }

        return null;
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
        FilterMap filterMap = new FilterMap();
        filterMap.setFilterName(sfm.getName());

        sfm.getServletNames().forEach(servletName -> filterMap.addServletName(servletName));
        sfm.getUrlPatterns().forEach(urlPattern -> filterMap.addURLPattern(urlPattern));
        sfm.getDispatchers().forEach(dispatcherType -> filterMap.setDispatcher(dispatcherType.name()));
        addFilterMap(filterMap);
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
                WebPropertyContainer.NAME);
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
        if (!(valve instanceof Valve)) {
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
        }
    }

    /**
     * Adds the Catalina listener with the given class name to this
     * WebModule.
     *
     * @param listenerName The fully qualified class name of the listener
     */
    protected void addCatalinaListener(String listenerName) {
        final Object listener = loadInstance(listenerName);
        if (listener == null) {
            return;
        }

        if (listener instanceof ContainerListener) {
            addContainerListener((ContainerListener)listener);
        } else if (listener instanceof LifecycleListener ){
            addLifecycleListener((LifecycleListener)listener);
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
     *
     * Properties are inherited from VirtualServer and may be overridden by configuration from sun-web.xml
     */
    void configureMiscSettings(final VirtualServer vs, final String contextPath) {
        for (final Property prop : vs.getProperties()) {
            final String name = prop.getName();
            final String value = prop.getValue();
            configureProperty(vs, contextPath, name, value);
        }

        // sen-web.xml preserved for backward compatibility
        final SunWebAppImpl configBean = getIasWebAppConfigBean();
        if (configBean != null && configBean.sizeWebProperty() > 0) {
            for (final WebProperty webProperty : configBean.getWebProperty()) {
                final String name = webProperty.getAttributeValue("name");
                final String value = webProperty.getAttributeValue("value");
                configureProperty(vs, contextPath, name, value);
            }
        }
    }

    private void configureProperty(
        final VirtualServer vs,
        final String contextPath,
        final String name,
        final String value
    ) {
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
            final boolean crossContext = Boolean.parseBoolean(value);
            setCrossContext(crossContext);
        } else if("useResponseCTForHeaders".equalsIgnoreCase(name)) {
            if("true".equalsIgnoreCase(value)) {
                setResponseCTForHeaders();
            }
        } else if("encodeCookies".equalsIgnoreCase(name)) {
            final boolean flag = ConfigBeansUtilities.toBoolean(value);
            setEncodeCookies(flag);
        } else if("relativeRedirectAllowed".equalsIgnoreCase(name)) {
            final boolean relativeRedirect = ConfigBeansUtilities.toBoolean(value);
            setUseRelativeRedirects(relativeRedirect);
        } else if("fileEncoding".equalsIgnoreCase(name)) {
            setFileEncoding(value);
        } else if("enableTldValidation".equalsIgnoreCase(name)
            && ConfigBeansUtilities.toBoolean(value)) {
            setTldValidation(true);
        } else if("default-role-mapping".equalsIgnoreCase(name)) {
            wmInfo.getDescriptor().setDefaultGroupPrincipalMapping(ConfigBeansUtilities.toBoolean(value));
        } else if("default-web-xml".equalsIgnoreCase(name)) {
            vs.setDefaultWebXmlLocation(value);
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
            Vector<String> endpointList = new Vector<>();
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
            setSessionTimeout(webXmlTimeoutSeconds);
        } else {
            /*
             * Do not override Tomcat default, unless 'timeoutSeconds' was
             * specified in sun-web.xml
             */
            if (timeoutConfigured) {
                setSessionTimeout(webXmlTimeoutSeconds);
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

    /**
     * Commented out for now - This monitoring was reliant on direct instrumentation of Catalina, which if we're
     *      not patching in needs completely reworking using {@link org.apache.catalina.SessionListener}

//    public void sessionCreatedEvent(HttpSession session) {
//        sessionProbeProvider.sessionCreatedEvent(session.getId(),
//            monitoringNodeName, vsId);
//    }
//
//    public void sessionDestroyedEvent(HttpSession session) {
//        sessionProbeProvider.sessionDestroyedEvent(session.getId(),
//            monitoringNodeName, vsId);
//    }
//
//    public void sessionRejectedEvent(int maxSessions) {
//        sessionProbeProvider.sessionRejectedEvent(maxSessions,
//            monitoringNodeName, vsId);
//    }
//
//    public void sessionExpiredEvent(HttpSession session) {
//        sessionProbeProvider.sessionExpiredEvent(session.getId(),
//            monitoringNodeName, vsId);
//    }
//
//    public void sessionPersistedStartEvent(HttpSession session) {
//        sessionProbeProvider.sessionPersistedStartEvent(session.getId(),
//            monitoringNodeName, vsId);
//    }
//
//    public void sessionPersistedEndEvent(HttpSession session) {
//        sessionProbeProvider.sessionPersistedEndEvent(session.getId(),
//            monitoringNodeName, vsId);
//    }
//
//    public void sessionActivatedStartEvent(HttpSession session) {
//        sessionProbeProvider.sessionActivatedStartEvent(session.getId(),
//            monitoringNodeName, vsId);
//    }
//
//    public void sessionActivatedEndEvent(HttpSession session) {
//        sessionProbeProvider.sessionActivatedEndEvent(session.getId(),
//            monitoringNodeName, vsId);
//    }
//
//    public void sessionPassivatedStartEvent(HttpSession session) {
//        sessionProbeProvider.sessionPassivatedStartEvent(session.getId(),
//            monitoringNodeName, vsId);
//    }
//
//    public void sessionPassivatedEndEvent(HttpSession session) {
//        sessionProbeProvider.sessionPassivatedEndEvent(session.getId(),
//            monitoringNodeName, vsId);
//    }
     *
     */

    @Override
    public String getContextPath() {
        return getServletContext().getContextPath();
    }

    @Override
    public ServletContext getContext(String uriPath) {
        return getServletContext().getContext(uriPath);
    }

    @Override
    public int getMajorVersion() {
        return getServletContext().getMajorVersion();
    }

    @Override
    public int getMinorVersion() {
        return getServletContext().getMinorVersion();
    }

    @Override
    public String getMimeType(String file) {
        return getServletContext().getMimeType(file);
    }

    @Override
    public Set<String> getResourcePaths(String path) {
        return getServletContext().getResourcePaths(path);
    }

    @Override
    public URL getResource(String path) throws MalformedURLException {
        return getServletContext().getResource(path);
    }

    @Override
    public InputStream getResourceAsStream(String path) {
        return getServletContext().getResourceAsStream(path);
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String path) {
        return getServletContext().getRequestDispatcher(path);
    }

    @Override
    public RequestDispatcher getNamedDispatcher(String name) {
        return getServletContext().getNamedDispatcher(name);
    }

    @Override
    public void log(String message) {
        getServletContext().log(message);
    }

    @Override
    public void log(String message, Throwable throwable) {
        getServletContext().log(message, throwable);
    }

    @Override
    public String getServerInfo() {
        return getServletContext().getServerInfo();
    }

    @Override
    public String getInitParameter(String name) {
        return getServletContext().getInitParameter(name);
    }

    @Override
    public Enumeration<String> getInitParameterNames() {
        return getServletContext().getInitParameterNames();
    }

    @Override
    public boolean setInitParameter(String name, String value) {
        return getServletContext().setInitParameter(name, value);
    }

    @Override
    public Object getAttribute(String name) {
        return getServletContext().getAttribute(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return getServletContext().getAttributeNames();
    }

    @Override
    public void setAttribute(String name, Object value) {
        getServletContext().setAttribute(name, value);
    }

    @Override
    public void removeAttribute(String name) {
        getServletContext().removeAttribute(name);
    }

    @Override
    public String getServletContextName() {
        return getServletContext().getServletContextName();
    }

    @Override
    public ServletRegistration.Dynamic addServlet(String servletName, String className) {
        return getServletContext().addServlet(servletName, className);
    }

    @Override
    public ServletRegistration.Dynamic addServlet(String servletName, Servlet servlet) {
        return getServletContext().addServlet(servletName, servlet);
    }

    @Override
    public ServletRegistration.Dynamic addServlet(String servletName, Class<? extends Servlet> servletClass) {
        return getServletContext().addServlet(servletName, servletClass);
    }

    @Override
    public ServletRegistration.Dynamic addJspFile(String jspName, String jspFile) {
        return getServletContext().addJspFile(jspName, jspFile);
    }

    @Override
    public <T extends Servlet> T createServlet(Class<T> servletClass) throws ServletException {
        if (DefaultServlet.class.equals(servletClass) || JspServlet.class.equals(servletClass) ||
                webContainer == null) {
            // Container-provided servlets, skip injection
            return getServletContext().createServlet(servletClass);
        }

        try {
            return (T) getInstanceManager().newInstance(servletClass);
        } catch (Exception exception) {
            // Log and rethrow as ServletException
            logger.log(Level.SEVERE, LogFacade.EXCEPTION_CREATING_SERVLET_INSTANCE);
            throw new ServletException(exception);
        }
    }

    @Override
    public ServletRegistration getServletRegistration(String servletName) {
        return getServletContext().getServletRegistration(servletName);
    }

    @Override
    public Map<String, ? extends ServletRegistration> getServletRegistrations() {
        return getServletContext().getServletRegistrations();
    }

    @Override
    public FilterRegistration.Dynamic addFilter(String filterName, String className) {
        return getServletContext().addFilter(filterName, className);
    }

    @Override
    public FilterRegistration.Dynamic addFilter(String filterName, Filter filter) {
        return getServletContext().addFilter(filterName, filter);
    }

    @Override
    public FilterRegistration.Dynamic addFilter(String filterName, Class<? extends Filter> filterClass) {
        return getServletContext().addFilter(filterName, filterClass);
    }

    @Override
    public <T extends Filter> T createFilter(Class<T> filterClass) throws ServletException {
        if (webContainer == null) {
            return getServletContext().createFilter(filterClass);
        }

        try {
            return (T) getInstanceManager().newInstance(filterClass);
        } catch (Exception exception) {
            // Log and rethrow as ServletException
            logger.log(Level.SEVERE, LogFacade.EXCEPTION_CREATING_FILTER_INSTANCE);
            throw new ServletException(exception);
        }
    }

    @Override
    public FilterRegistration getFilterRegistration(String filterName) {
        return getServletContext().getFilterRegistration(filterName);
    }

    @Override
    public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
        return getServletContext().getFilterRegistrations();
    }

    @Override
    public void setSessionTrackingModes(Set<SessionTrackingMode> sessionTrackingModes) {
        getServletContext().setSessionTrackingModes(sessionTrackingModes);
    }

    @Override
    public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
        return getServletContext().getDefaultSessionTrackingModes();
    }

    @Override
    public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
        return getServletContext().getEffectiveSessionTrackingModes();
    }

    @Override
    public void addListener(String className) {
        getServletContext().addListener(className);
    }

    @Override
    public <T extends EventListener> T createListener(Class<T> listenerClass) throws ServletException {
        if (webContainer == null) {
            return getServletContext().createListener(listenerClass);
        }

        try {
            return (T) getInstanceManager().newInstance(listenerClass);
        } catch (Exception exception) {
            // Log and rethrow as ServletException
            logger.log(Level.SEVERE, LogFacade.EXCEPTION_CREATING_LISTENER_INSTANCE);
            throw new ServletException(exception);
        }
    }

    @Override
    public ClassLoader getClassLoader() {
        return getServletContext().getClassLoader();
    }

    @Override
    public void declareRoles(String... roleNames) {
        getServletContext().declareRoles(roleNames);
        WebBundleDescriptor bundleDescriptor = getWebBundleDescriptor();

        for (String roleName : roleNames) {
            bundleDescriptor.addRole(new Role(roleName));
        }

        bundleDescriptor.setPolicyModified(true);
    }

    @Override
    public String getVirtualServerName() {
        return getServletContext().getVirtualServerName();
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

    @Override
    public SecurityConfig getSecurityConfig() {
        return config;
    }

    @Override
    public <T extends EventListener> void addListener(T t) {
        getServletContext().addListener(t);
    }

    @Override
    public void addListener(Class<? extends EventListener> listenerClass) {
        getServletContext().addListener(listenerClass);
    }

    @Override
    public void setDirectoryListing(boolean directoryListing) {
        throw new UnsupportedOperationException("No longer supported - unused within Payara");
    }

    @Override
    public boolean isDirectoryListing() {
        throw new UnsupportedOperationException("No longer supported - unused within Payara");
    }

    @Override
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
            Valve basic = pipeline.getBasic();
            if ((basic != null) && (basic instanceof java.net.Authenticator)) {
                getPipeline().removeValve((basic));
            }
            Valve valves[] = pipeline.getValves();
            for (Valve valve : valves) {
                if (valve instanceof java.net.Authenticator) {
                    getPipeline().removeValve((valve));
                }
            }
        }

        Realm realm = getRealm();
        if (realm != null && realm instanceof RealmInitializer) {
            ((RealmInitializer) realm).initializeRealm(
                    this.getWebBundleDescriptor(),
                    false,
                    ((VirtualServer)parent).getAuthRealmName());
            ((RealmInitializer) realm).setVirtualServer(getParent());
            ((RealmInitializer) realm).updateWebSecurityManager();
            setRealm(realm);
        }
    }

    public long getUniqueId() {
        com.sun.enterprise.deployment.Application app = wmInfo.getDescriptor().getApplication();
        return app != null? app.getUniqueId() : 0L;
    }



    public void setEngineName(String name) {
        this.engineName = name;
    }

    public void setJvmRoute(String jvmRoute) {
        this.jvmRoute = jvmRoute;
    }

    public String getEngineName() {
        return engineName;
    }

    public String getJvmRoute() {
        return jvmRoute;
    }
}
