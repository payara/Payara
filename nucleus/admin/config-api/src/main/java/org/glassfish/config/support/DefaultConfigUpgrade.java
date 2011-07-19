/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.config.support;

import java.beans.PropertyVetoException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLInputFactory;
import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;

import com.sun.enterprise.config.serverbeans.*;
import com.sun.enterprise.util.EarlyLogger;
import com.sun.enterprise.util.LocalStringManagerImpl;
import org.glassfish.grizzly.config.dom.*;
import org.glassfish.api.admin.config.ConfigurationUpgrade;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.PostConstruct;
import org.jvnet.hk2.config.*;
import org.jvnet.hk2.config.types.Property;

/**
 * Upgrade service to add the default-config if it doesn't exist.
 * 3.0.1 and v2.x developer profile do not have default-config.
 * The data to populate the default-config is taken from
 * glassfish3\glassfish\lib\templates\domain.xml.  This class uses the StAX
 * parser and depends on the exact order of the elements in the template, and
 * the original contents of the template when glassfish was installed.
 * The DefaultConfigUpgrade may not work if the template has been changed from
 * its original.
 *
 * TODO: Replace using a simpler, more maintainable approach such as HK2 DOM objects, or having
 * a default for the Config object that creates default-config
 *
 * @author Jennifer Chou
 */
@Service(name = "defaultconfigupgrade")
public class DefaultConfigUpgrade implements ConfigurationUpgrade, PostConstruct {

    @Inject
    Configs configs;
    private static final String DEFAULT_CONFIG = "default-config";
    private static final String INSTALL_ROOT = "com.sun.aas.installRoot";
    private static final LocalStringManagerImpl localStrings =
            new LocalStringManagerImpl(DefaultConfigUpgrade.class);

    @Override
    public void postConstruct() {

        Config defaultConfig = configs.getConfigByName(DEFAULT_CONFIG);
        if (defaultConfig != null) {
            Logger.getLogger(DefaultConfigUpgrade.class.getName()).log(
                    Level.INFO, localStrings.getLocalString(
                    "DefaultConfigUpgrade.existingDefaultConfig",
                    "Existing default-config detected during upgrade. No need to create default-config."));
            return;
        }

        String installRoot = System.getProperty(INSTALL_ROOT);
        if (installRoot == null) {
            Logger.getLogger(DefaultConfigUpgrade.class.getName()).log(
                    Level.INFO, localStrings.getLocalString(
                    "DefaultConfigUpgrade.installRootIsNull",
                    "System Property com.sun.aas.installRoot is null. We could be running in unit tests."
                    + "Exiting DefaultConfigUpgrade"));
            return;
        }

        Logger.getLogger(DefaultConfigUpgrade.class.getName()).log(
                Level.INFO, localStrings.getLocalString(
                "DefaultConfigUpgrade.runningDefaultConfigUpgrade",
                "default-config not detected during upgrade. Running DefaultConfigUpgrade to create default-config."));

        String template = getDomainXmlTemplate();

        try {
            ConfigSupport.apply(new MinDefaultConfigCode(), configs);
            defaultConfig = configs.getConfigByName(DEFAULT_CONFIG);

            createParser(template);

            createDefaultConfigAttr(defaultConfig);

            createHttpServiceConfig(defaultConfig);

            createIiopServiceConfig(defaultConfig);

            createAdminServiceConfig(defaultConfig);

            createWebContainerConfig(defaultConfig);

            createEjbContainerConfig(defaultConfig);

            createJmsServiceConfig(defaultConfig);

            createLogServiceConfig(defaultConfig);

            createSecurityServiceConfig(defaultConfig);

            createTransactionServiceConfig(defaultConfig);

            createDiagnosticServiceConfig(defaultConfig);

            createJavaConfig(defaultConfig);

            createAvailabilityService(defaultConfig);

            createNetworkConfig(defaultConfig);

            createThreadPools(defaultConfig);

            createManagementRules(defaultConfig);

            createSystemProperties(defaultConfig);

        } catch (TransactionFailure ex) {
            Logger.getLogger(DefaultConfigUpgrade.class.getName()).log(
                    Level.SEVERE, localStrings.getLocalString(
                    "DefaultConfigUpgrade.failure",
                    "Failure during upgrade - could not create default-config"), ex);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(DefaultConfigUpgrade.class.getName()).log(
                    Level.SEVERE, localStrings.getLocalString(
                    "DefaultConfigUpgrade.failure",
                    "Failure during upgrade - could not create default-config"), ex);
        } catch (XMLStreamException ex) {
            Logger.getLogger(DefaultConfigUpgrade.class.getName()).log(
                    Level.SEVERE, localStrings.getLocalString(
                    "DefaultConfigUpgrade.failure",
                    "Failure during upgrade - could not create default-config"), ex);
        } finally {
            try {
                if (parser != null) {
                    parser.close();
                }
            } catch (Exception e) {
                // ignore
            }
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (Exception e) {
                // ignore
            }
        }
    }

    private String getDomainXmlTemplate() {
        String installRoot = System.getProperty(INSTALL_ROOT);
        String template = installRoot + File.separator + "lib" + File.separator + "templates" + File.separator + "domain.xml";
        File f = new File(template);
        if (!f.exists()) {
            throw new RuntimeException(localStrings.getLocalString(
                    "DefaultConfigUpgrade.missingDomainXmlTemplate",
                    "DefaultConfigUpgrade failed. Missing domain.xml template here " + template, template));
        } else {
            Logger.getLogger(DefaultConfigUpgrade.class.getName()).log(
                    Level.INFO, localStrings.getLocalString(
                    "DefaultConfigUpgrade.foundDomainXmlTemaplate",
                    "Found domain.xml template to create default-config: " + template, template));
        }
        return template;
    }

    private void createDefaultConfigAttr(Config defaultConfig) throws TransactionFailure, XMLStreamException {
        while (true) {
            if (parser.next() == START_ELEMENT) {
                if (parser.getLocalName().equals("config")
                        && parser.getAttributeValue(null, "name").equals(DEFAULT_CONFIG)) {
                    ConfigSupport.apply(new DefaultConfigCode(), defaultConfig);
                    break;
                }
            }
        }
    }

    private void createHttpServiceConfig(Config defaultConfig) throws TransactionFailure, XMLStreamException {
        while (true) {
            if (parser.next() == START_ELEMENT) {
                if (parser.getLocalName().equals("http-service")) {
                    ConfigSupport.apply(new HttpServiceConfigCode(), defaultConfig);
                    break;
                }
            }
        }
    }

    private void createIiopServiceConfig(Config defaultConfig) throws TransactionFailure, XMLStreamException {
        while (true) {
            if (parser.next() == START_ELEMENT) {
                if (parser.getLocalName().equals("iiop-service")) {
                    ConfigSupport.apply(new IiopServiceConfigCode(), defaultConfig);
                    break;
                }
            }
        }
    }

    private void createAdminServiceConfig(Config defaultConfig) throws TransactionFailure, XMLStreamException {
        while (true) {
            if (parser.next() == START_ELEMENT) {
                if (parser.getLocalName().equals("admin-service")) {
                    ConfigSupport.apply(new AdminServiceConfigCode(), defaultConfig);
                    break;
                }
            }
        }
    }

    private void createWebContainerConfig(Config defaultConfig) throws TransactionFailure, XMLStreamException {
        while (true) {
            if (parser.next() == START_ELEMENT) {
                if (parser.getLocalName().equals("web-container")) {
                    SessionManager sm = defaultConfig.getWebContainer().getSessionConfig().getSessionManager();
                    ConfigSupport.apply(new WebContainerConfigCode(), sm);
                    break;
                }
            }
        }
    }

    private void createEjbContainerConfig(Config defaultConfig) throws TransactionFailure, XMLStreamException {
        while (true) {
            if (parser.next() == START_ELEMENT) {
                if (parser.getLocalName().equals("ejb-container")) {
                    ConfigSupport.apply(new EjbContainerConfigCode(), defaultConfig);
                    break;
                }
            }
        }
    }

    private void createJmsServiceConfig(Config defaultConfig) throws TransactionFailure, XMLStreamException {
        while (true) {
            if (parser.next() == START_ELEMENT) {
                if (parser.getLocalName().equals("jms-service")) {
                    ConfigSupport.apply(new JmsServiceConfigCode(), defaultConfig);
                    break;
                }
            }
        }
    }

    private void createLogServiceConfig(Config defaultConfig) throws TransactionFailure, XMLStreamException {
        while (true) {
            if (parser.next() == START_ELEMENT) {
                if (parser.getLocalName().equals("log-service")) {
                    ConfigSupport.apply(new LogServiceConfigCode(), defaultConfig);
                    break;
                }
            }
        }
    }

    private void createSecurityServiceConfig(Config defaultConfig) {
        while (true) {
            try {
                if (parser.next() == START_ELEMENT) {
                    if (parser.getLocalName().equals("security-service")) {
                        ConfigSupport.apply(new SecurityServiceConfigCode(), defaultConfig);
                        break;
                    }
                }
            } catch (TransactionFailure ex) {
                Logger.getLogger(DefaultConfigUpgrade.class.getName()).log(
                        Level.SEVERE, "Failure creating SecurityService Config", ex);
            } catch (XMLStreamException ex) {
                Logger.getLogger(DefaultConfigUpgrade.class.getName()).log(
                        Level.SEVERE, "Problem parsing security-service", ex);
            }
        }
    }

    private void createTransactionServiceConfig(Config defaultConfig) throws TransactionFailure, XMLStreamException {
        while (true) {
            if (parser.next() == START_ELEMENT) {
                if (parser.getLocalName().equals("transaction-service")) {
                    ConfigSupport.apply(new TransactionServiceConfigCode(), defaultConfig);
                    break;
                }
            }
        }
    }

    private void createDiagnosticServiceConfig(Config defaultConfig) throws TransactionFailure, XMLStreamException {
        while (true) {
            if (parser.next() == START_ELEMENT) {
                if (parser.getLocalName().equals("diagnostic-service")) {
                    ConfigSupport.apply(new DiagnosticServiceConfigCode(), defaultConfig);
                    break;
                }
            }
        }
    }

    private void createJavaConfig(Config defaultConfig) throws TransactionFailure, XMLStreamException {
        while (true) {
            if (parser.next() == START_ELEMENT) {
                if (parser.getLocalName().equals("java-config")) {
                    ConfigSupport.apply(new JavaConfigCode(), defaultConfig);
                    break;
                }
            }
        }
    }

    private void createAvailabilityService(Config defaultConfig) throws TransactionFailure, XMLStreamException {
        while (true) {
            if (parser.next() == START_ELEMENT) {
                if (parser.getLocalName().equals("availability-service")) {
                    ConfigSupport.apply(new AvailabilityServiceConfigCode(), defaultConfig);
                    break;
                }
            }
        }
    }

    private void createNetworkConfig(Config defaultConfig) throws TransactionFailure, XMLStreamException {
        while (true) {
            if (parser.next() == START_ELEMENT) {
                if (parser.getLocalName().equals("network-config")) {
                    ConfigSupport.apply(new NetworkConfigCode(), defaultConfig);
                    break;
                }
            }
        }
    }

    private void createThreadPools(Config defaultConfig) throws TransactionFailure, XMLStreamException {
        while (true) {
            if (parser.next() == START_ELEMENT) {
                if (parser.getLocalName().equals("thread-pools")) {
                    ConfigSupport.apply(new ThreadPoolsConfigCode(), defaultConfig);
                    break;
                }
            }
        }
    }

    private void createManagementRules(Config defaultConfig) throws TransactionFailure, XMLStreamException {
        ConfigSupport.apply(new ManagementRulesConfigCode(), defaultConfig);
    }

    private void createSystemProperties(Config defaultConfig) throws TransactionFailure, XMLStreamException {
        ConfigSupport.apply(new SystemPropertyConfigCode(), defaultConfig);
    }

    /*
     * Creates the default-config object with the required elements (marked with @NotNull)
     */
    private static class MinDefaultConfigCode implements SingleConfigCode<Configs> {

        public Object run(Configs configs) throws PropertyVetoException, TransactionFailure {
            Config defaultConfig = configs.createChild(Config.class);
            defaultConfig.setName(DEFAULT_CONFIG);

            Dom.unwrap(defaultConfig).addDefaultChildren();
            configs.getConfig().add(defaultConfig);

            return defaultConfig;
        }
    }

    private class DefaultConfigCode implements SingleConfigCode<Config> {

        public Object run(Config config) throws PropertyVetoException, TransactionFailure {

            config.setDynamicReconfigurationEnabled(
                    parser.getAttributeValue(null, "dynamic-reconfiguration-enabled"));

            return null;
        }
    }

    /*
     * Creates the http-service object using data from glassfish3\glassfish\lib\templates\domain.xml
     * <http-service>
     *  <access-log/>
     *  <virtual-server id="server" network-listeners="http-listener-1, http-listener-2">
     *      <property name="default-web-xml" value="${com.sun.aas.instanceRoot}/config/default-web.xml"/>
     *  </virtual-server>
     *  <virtual-server id="__asadmin" network-listeners="admin-listener"/>
     * </http-service>
     */
    private class HttpServiceConfigCode implements SingleConfigCode<Config> {

        public Object run(Config config) throws PropertyVetoException, TransactionFailure {

            HttpService httpService = config.createChild(HttpService.class);
            config.setHttpService(httpService);
            AccessLog al = httpService.createChild(AccessLog.class);
            httpService.setAccessLog(al);

            createVirtualServer(httpService);

            return config;
        }
    }

    private void createVirtualServer(HttpService hs) throws PropertyVetoException {
        try {
            while (!(parser.getEventType() == END_ELEMENT && parser.getLocalName().equals("http-service"))) {
                if (parser.next() == START_ELEMENT) {
                    if (parser.getLocalName().equals("virtual-server")) {
                        VirtualServer vs = hs.createChild(VirtualServer.class);
                        hs.getVirtualServer().add(vs);
                        for (int i = 0; i < parser.getAttributeCount(); i++) {
                            String attr = parser.getAttributeLocalName(i);
                            if (attr.equals("id")) {
                                vs.setId(parser.getAttributeValue(i));
                            }
                            if (attr.equals("network-listeners")) {
                                vs.setNetworkListeners(parser.getAttributeValue(i));
                            }
                        }
                        createVirtualServerProperty(vs);
                    }
                }
            }
        } catch (TransactionFailure ex) {
            Logger.getLogger(DefaultConfigUpgrade.class.getName()).log(
                    Level.SEVERE, "Failed to create HttpService VirtualService config object", ex);
        } catch (XMLStreamException ex) {
            Logger.getLogger(DefaultConfigUpgrade.class.getName()).log(
                    Level.SEVERE, "Problem parsing http-service virtual-server in domain.xml template", ex);
        }
    }

    private void createVirtualServerProperty(VirtualServer vs) throws PropertyVetoException {
        while (!(parser.getEventType() == END_ELEMENT && parser.getLocalName().equals("virtual-server"))) {
            try {
                if (parser.next() == START_ELEMENT) {
                    if (parser.getLocalName().equals("property")) {
                        Property p = vs.createChild(Property.class);
                        vs.getProperty().add(p);
                        createProperty(p);
                    }
                }
            } catch (TransactionFailure ex) {
                Logger.getLogger(DefaultConfigUpgrade.class.getName()).log(
                        Level.SEVERE, "Failed creating VirtualServer Property config object", ex);
            } catch (XMLStreamException ex) {
                Logger.getLogger(DefaultConfigUpgrade.class.getName()).log(
                        Level.SEVERE, "Problem parsing virtual-server property element in domain.xml template", ex);
            }
        }
    }

    /*
     * Creates the iiop-service object using data from glassfish3\glassfish\lib\templates\domain.xml
     * <iiop-service>
     *  <orb use-thread-pool-ids="thread-pool-1"/>
     *  <iiop-listener port="${IIOP_LISTENER_PORT}" id="orb-listener-1" address="0.0.0.0"/>
     *  <iiop-listener port="${IIOP_SSL_LISTENER_PORT}" id="SSL" address="0.0.0.0" security-enabled="true">
     *      <ssl classname="com.sun.enterprise.security.ssl.GlassfishSSLImpl" cert-nickname="s1as"/>
     *  </iiop-listener>
     *  <iiop-listener port="${IIOP_SSL_MUTUALAUTH_PORT}" id="SSL_MUTUALAUTH" address="0.0.0.0" security-enabled="true">
     *      <ssl classname="com.sun.enterprise.security.ssl.GlassfishSSLImpl" cert-nickname="s1as" client-auth-enabled="true"/>
     *  </iiop-listener>
     *</iiop-service>
     */
    private class IiopServiceConfigCode implements SingleConfigCode<Config> {

        public Object run(Config config) throws PropertyVetoException, TransactionFailure {

            IiopService iiopService = config.createChild(IiopService.class);
            config.setIiopService(iiopService);

            createOrb(iiopService);
            createIiopListener(iiopService);

            return null;
        }
    }

    /* <orb use-thread-pool-ids="thread-pool-1"/> */
    private void createOrb(IiopService is) throws PropertyVetoException {
        while (true) {
            try {
                if (parser.next() == START_ELEMENT) {
                    if (parser.getLocalName().equals("orb")) {
                        Orb orb = is.createChild(Orb.class);
                        is.setOrb(orb);
                        for (int i = 0; i < parser.getAttributeCount(); i++) {
                            String attr = parser.getAttributeLocalName(i);
                            if (attr.equals("use-thread-pool-ids")) {
                                orb.setUseThreadPoolIds(parser.getAttributeValue(i));
                            }
                        }
                        break;
                    }
                }
            } catch (TransactionFailure ex) {
                Logger.getLogger(DefaultConfigUpgrade.class.getName()).log(
                        Level.SEVERE, "Failure creating IiopService Orb config object", ex);
            } catch (XMLStreamException ex) {
                Logger.getLogger(DefaultConfigUpgrade.class.getName()).log(
                        Level.SEVERE, "Problem parsing iiop-service orb element in domain.xml template", ex);
            }
        }
    }

    /* Loop through all iiop-listener elements in template and create IiopListener config objects.
     * <iiop-listener port="${IIOP_SSL_MUTUALAUTH_PORT}" id="SSL_MUTUALAUTH" address="0.0.0.0" security-enabled="true">
     *      <ssl classname="com.sun.enterprise.security.ssl.GlassfishSSLImpl" cert-nickname="s1as" client-auth-enabled="true"/>
     *  </iiop-listener>  (1 example)
     */
    private void createIiopListener(IiopService is) throws PropertyVetoException {
        while (!(parser.getEventType() == END_ELEMENT && parser.getLocalName().equals("iiop-service"))) {
            try {
                if (parser.next() == START_ELEMENT) {
                    if (parser.getLocalName().equals("iiop-listener")) {
                        IiopListener il = is.createChild(IiopListener.class);
                        is.getIiopListener().add(il);

                        for (int i = 0; i < parser.getAttributeCount(); i++) {
                            String attr = parser.getAttributeLocalName(i);
                            if (attr.equals("port")) {
                                il.setPort(parser.getAttributeValue(i));
                            }
                            if (attr.equals("id")) {
                                il.setId(parser.getAttributeValue(i));
                            }
                            if (attr.equals("address")) {
                                il.setAddress(parser.getAttributeValue(i));
                            }
                            if (attr.equals("security-enabled")) {
                                il.setSecurityEnabled(parser.getAttributeValue(i));
                            }
                        }
                        createIiopListenerSsl(il);
                    }
                }
            } catch (TransactionFailure ex) {
                Logger.getLogger(DefaultConfigUpgrade.class.getName()).log(
                        Level.SEVERE, "Failure creating IiopService IiopListener config object", ex);
            } catch (XMLStreamException ex) {
                Logger.getLogger(DefaultConfigUpgrade.class.getName()).log(
                        Level.SEVERE, "Problem parsing iiop-service iiop-listener element in domain.xml template", ex);
            }
        }
    }

    private void createIiopListenerSsl(IiopListener il) throws PropertyVetoException {
        while (!(parser.getEventType() == END_ELEMENT && parser.getLocalName().equals("iiop-listener"))) {
            try {
                if (parser.next() == START_ELEMENT) {
                    if (parser.getLocalName().equals("ssl") && il != null) {
                        Ssl ssl = il.createChild(Ssl.class);
                        il.setSsl(ssl);
                        for (int i = 0; i < parser.getAttributeCount(); i++) {
                            String attr = parser.getAttributeLocalName(i);
                            if (attr.equals("classname")) {
                                ssl.setClassname(parser.getAttributeValue(i));
                            }
                            if (attr.equals("cert-nickname")) {
                                ssl.setCertNickname(parser.getAttributeValue(i));
                            }
                            if (attr.equals("client-auth-enabled")) {
                                ssl.setClientAuthEnabled(parser.getAttributeValue(i));
                            }
                        }
                    }
                }
            } catch (TransactionFailure ex) {
                Logger.getLogger(DefaultConfigUpgrade.class.getName()).log(
                        Level.SEVERE, "Failed to create IiopListener Ssl config object", ex);
            } catch (XMLStreamException ex) {
                Logger.getLogger(DefaultConfigUpgrade.class.getName()).log(
                        Level.SEVERE, "Problem parsing iiop-listner ssl element in domain.xml template", ex);
            }
        }
    }

    /*
     * Creates the admin-service object using data from glassfish3\glassfish\lib\templates\domain.xml
     * <admin-service system-jmx-connector-name="system" type="server">
     *  <!-- JSR 160  "system-jmx-connector" -->
     *  <jmx-connector address="0.0.0.0" auth-realm-name="admin-realm" name="system" port="${JMX_SYSTEM_CONNECTOR_PORT}" protocol="rmi_jrmp" security-enabled="false"/>
     *  <!-- JSR 160  "system-jmx-connector" -->
     *  <property value="${com.sun.aas.installRoot}/lib/install/applications/admingui.war" name="adminConsoleDownloadLocation"/>
     *</admin-service>
     */
    private class AdminServiceConfigCode implements SingleConfigCode<Config> {

        public Object run(Config config) throws PropertyVetoException, TransactionFailure {

            AdminService adminService = config.createChild(AdminService.class);
            config.setAdminService(adminService);

            //dasConfig cannot be null.  Add a dummy.
            DasConfig dc = adminService.createChild(DasConfig.class);
            adminService.setDasConfig(dc);

            for (int i = 0; i < parser.getAttributeCount(); i++) {
                String attr = parser.getAttributeLocalName(i);
                String val = parser.getAttributeValue(i);
                if (attr.equals("system-jmx-connector-name")) {
                    adminService.setSystemJmxConnectorName(val);
                }
                if (attr.equals("type")) {
                    adminService.setType(val);
                }
            }

            createJmxConnector(adminService);
            createAdminServiceProperty(adminService);

            return null;
        }
    }

    /* <jmx-connector address="0.0.0.0" auth-realm-name="admin-realm" name="system"
     * port="${JMX_SYSTEM_CONNECTOR_PORT}" protocol="rmi_jrmp" security-enabled="false"/>
     */
    private void createJmxConnector(AdminService as) throws PropertyVetoException {
        while (true) {
            try {
                if (parser.next() == START_ELEMENT) {
                    if (parser.getLocalName().equals("jmx-connector")) {
                        JmxConnector jc = as.createChild(JmxConnector.class);
                        as.getJmxConnector().add(jc);
                        for (int i = 0; i < parser.getAttributeCount(); i++) {
                            String attr = parser.getAttributeLocalName(i);
                            String val = parser.getAttributeValue(i);
                            if (attr.equals("address")) {
                                jc.setAddress(val);
                            }
                            if (attr.equals("auth-realm-name")) {
                                jc.setAuthRealmName(val);
                            }
                            if (attr.equals("name")) {
                                jc.setName(val);
                            }
                            if (attr.equals("port")) {
                                jc.setPort(val);
                            }
                            if (attr.equals("protocol")) {
                                jc.setProtocol(val);
                            }
                            if (attr.equals("security-enabled")) {
                                jc.setSecurityEnabled(val);
                            }
                        }
                        break;
                    }
                }
            } catch (TransactionFailure ex) {
                Logger.getLogger(DefaultConfigUpgrade.class.getName()).log(
                        Level.SEVERE, "Failed to create AdminService JmxConnector config object.", ex);
            } catch (XMLStreamException ex) {
                Logger.getLogger(DefaultConfigUpgrade.class.getName()).log(Level.SEVERE,
                        "Problem parsing admin-service jmx-connector", ex);
            }
        }
    }

    /*  <property value="${com.sun.aas.installRoot}/lib/install/applications/admingui.war"
     * name="adminConsoleDownloadLocation"/>
     */
    private void createAdminServiceProperty(AdminService as) throws PropertyVetoException {
        while (true) {
            try {
                if (parser.next() == START_ELEMENT) {
                    if (parser.getLocalName().equals("property")) {
                        Property p = as.createChild(Property.class);
                        as.getProperty().add(p);
                        createProperty(p);
                        break;
                    }
                }
            } catch (TransactionFailure ex) {
                Logger.getLogger(DefaultConfigUpgrade.class.getName()).log(
                        Level.SEVERE, "Failed to create AdminService Property config object", ex);
            } catch (XMLStreamException ex) {
                Logger.getLogger(DefaultConfigUpgrade.class.getName()).log(
                        Level.SEVERE, "Problem parsing asadmin-service property element in domain.xml template", ex);
            }
        }
    }

    /*
     * Creates the web-container object using data from glassfish3\glassfish\lib\templates\domain.xml
     * The required elements elements were already created in MinDefaultConfigCode, so only
     * need to add store-properties.
     * <web-container>
     *  <session-config>
     *      <session-manager>
     *          <manager-properties/>
     *          <store-properties/>
     *      </session-manager>
     *      <session-properties/>
     *  </session-config>
     * </web-container>
     */
    private class WebContainerConfigCode implements SingleConfigCode<SessionManager> {

        public Object run(SessionManager sm) throws PropertyVetoException {
            try {
                /* <store-properties/> */
                while (true) {
                    if (parser.next() == START_ELEMENT) {
                        if (parser.getLocalName().equals("session-manager") && sm != null) {
                            StoreProperties sp = sm.createChild(StoreProperties.class);
                            sm.setStoreProperties(sp);
                            break;
                        }
                    }
                }

            } catch (TransactionFailure ex) {
                Logger.getLogger(DefaultConfigUpgrade.class.getName()).log(
                        Level.SEVERE, "Failure creating WebContainer SessionManager StoreProperties config object", ex);
            } catch (XMLStreamException ex) {
                Logger.getLogger(DefaultConfigUpgrade.class.getName()).log(
                        Level.SEVERE, "Problem parsing web-container session-manager element in domain.xml template", ex);
            }

            return null;
        }
    }

    /*
     * Creates the ejb-container object using data from glassfish3\glassfish\lib\templates\domain.xml
     * <ejb-container session-store="${com.sun.aas.instanceRoot}/session-store">
     *      <ejb-timer-service/>
     * </ejb-container>
     */
    private class EjbContainerConfigCode implements SingleConfigCode<Config> {

        public Object run(Config config) throws PropertyVetoException {
            try {
                EjbContainer ec = config.createChild(EjbContainer.class);
                config.setEjbContainer(ec);
                EjbTimerService ets = ec.createChild(EjbTimerService.class);
                ec.setEjbTimerService(ets);
                /* <ejb-container session-store="${com.sun.aas.instanceRoot}/session-store"> */
                for (int i = 0; i < parser.getAttributeCount(); i++) {
                    String attr = parser.getAttributeLocalName(i);
                    String val = parser.getAttributeValue(i);
                    if (attr.equals("session-store")) {
                        ec.setSessionStore(val);
                    }
                }

            } catch (TransactionFailure ex) {
                Logger.getLogger(DefaultConfigUpgrade.class.getName()).log(
                        Level.SEVERE, "Failure creating EjbContainer EjbTimerService config objects", ex);
            }
            return null;
        }
    }

    /*
     * Creates the jms-service object using data from glassfish3\glassfish\lib\templates\domain.xml
     * <jms-service type="EMBEDDED" default-jms-host="default_JMS_host" addresslist-behavior="priority">
     *  <jms-host name="default_JMS_host" host="localhost" port="${JMS_PROVIDER_PORT}" admin-user-name="admin" admin-password="admin" lazy-init="true"/>
     * </jms-service>
     */
    private class JmsServiceConfigCode implements SingleConfigCode<Config> {

        public Object run(Config config) throws PropertyVetoException, TransactionFailure {
            JmsService js = config.createChild(JmsService.class);
            config.setJmsService(js);

            /* <jms-service type="EMBEDDED" default-jms-host="default_JMS_host"
            addresslist-behavior="priority"> */
            for (int i = 0; i < parser.getAttributeCount(); i++) {
                String attr = parser.getAttributeLocalName(i);
                String val = parser.getAttributeValue(i);
                if (attr.equals("type")) {
                    js.setType(val);
                }
                if (attr.equals("default-jms-host")) {
                    js.setDefaultJmsHost(val);
                }
                if (attr.equals("addresslist-behavior")) {
                    js.setAddresslistBehavior(val);
                }
            }

            createJmsHost(js);

            return null;
        }
    }

    /* <jms-host name="default_JMS_host" host="localhost" port="${JMS_PROVIDER_PORT}"
     *       admin-user-name="admin" admin-password="admin" lazy-init="true"/>
     */
    private void createJmsHost(JmsService js) throws PropertyVetoException {
        try {
            while (true) {
                if (parser.next() == START_ELEMENT) {
                    if (parser.getLocalName().equals("jms-host") && js != null) {
                        JmsHost jh = js.createChild(JmsHost.class);
                        js.getJmsHost().add(jh);

                        for (int i = 0; i < parser.getAttributeCount(); i++) {
                            String attr = parser.getAttributeLocalName(i);
                            String val = parser.getAttributeValue(i);
                            if (attr.equals("name")) {
                                jh.setName(val);
                            }
                            if (attr.equals("host")) {
                                jh.setHost(val);
                            }
                            if (attr.equals("port")) {
                                jh.setPort(val);
                            }
                            if (attr.equals("admin-user-name")) {
                                jh.setAdminUserName(val);
                            }
                            if (attr.equals("admin-password")) {
                                jh.setAdminPassword(val);
                            }
                            if (attr.equals("lazy-init")) {
                                jh.setLazyInit(val);
                            }
                        }
                        break;
                    }
                }
            }

        } catch (TransactionFailure ex) {
            Logger.getLogger(DefaultConfigUpgrade.class.getName()).log(
                    Level.SEVERE, "Failure creating JmsHost config object", ex);
        } catch (XMLStreamException ex) {
            Logger.getLogger(DefaultConfigUpgrade.class.getName()).log(
                    Level.SEVERE, "Problem parsing jms-host element in domain.xml template", ex);
        }
    }

    /*
     * Creates the log-service object using data from glassfish3\glassfish\lib\templates\domain.xml
     * <log-service log-rotation-limit-in-bytes="2000000" file="${com.sun.aas.instanceRoot}/logs/server.log">
     *      <module-log-levels/>
     * </log-service>
     */
    private class LogServiceConfigCode implements SingleConfigCode<Config> {

        public Object run(Config config) throws PropertyVetoException {
            LogService ls = null;
            try {
                ls = config.createChild(LogService.class);
            } catch (TransactionFailure ex) {
                Logger.getLogger(DefaultConfigUpgrade.class.getName()).log(
                        Level.SEVERE, "Failure creating LogService config object", ex);
            }
            config.setLogService(ls);

            for (int i = 0; i < parser.getAttributeCount(); i++) {
                String attr = parser.getAttributeLocalName(i);
                String val = parser.getAttributeValue(i);
                if (attr.equals("log-rotation-limit-in-bytes")) {
                    ls.setLogRotationLimitInBytes(val);
                }
                if (attr.equals("file")) {
                    ls.setFile(val);
                }
            }

            createModuleLogLevels(ls);

            return null;
        }
    }

    /* <module-log-levels/> */
    private void createModuleLogLevels(LogService ls) throws PropertyVetoException {
        try {
            while (true) {
                if (parser.next() == START_ELEMENT) {
                    if (parser.getLocalName().equals("module-log-levels") && ls != null) {
                        ModuleLogLevels mll = ls.createChild(ModuleLogLevels.class);
                        ls.setModuleLogLevels(mll);
                        break;
                    }
                }
            }
        } catch (TransactionFailure ex) {
            Logger.getLogger(DefaultConfigUpgrade.class.getName()).log(
                    Level.SEVERE, "Failure creating ModuleLogLevel config object", ex);
        } catch (XMLStreamException ex) {
            Logger.getLogger(DefaultConfigUpgrade.class.getName()).log(
                    Level.SEVERE, "Problem parsing module-log-levels in domain.xml template", ex);
        }
    }

    /*
     * Creates the security-service object using data from glassfish3\glassfish\lib\templates\domain.xml
     * <security-service>
     *  <auth-realm classname="com.sun.enterprise.security.auth.realm.file.FileRealm" name="admin-realm">
     *      <property name="file" value="${com.sun.aas.instanceRoot}/config/admin-keyfile"/>
     *      <property name="jaas-context" value="fileRealm"/>
     *  </auth-realm>
     *  <auth-realm classname="com.sun.enterprise.security.auth.realm.file.FileRealm" name="file">
     *      <property name="file" value="${com.sun.aas.instanceRoot}/config/keyfile"/>
     *      <property name="jaas-context" value="fileRealm"/>
     *  </auth-realm>
     *  <auth-realm classname="com.sun.enterprise.security.auth.realm.certificate.CertificateRealm" name="certificate"/>
     *  <jacc-provider policy-provider="com.sun.enterprise.security.provider.PolicyWrapper" name="default" policy-configuration-factory-provider="com.sun.enterprise.security.provider.PolicyConfigurationFactoryImpl">
     *      <property name="repository" value="${com.sun.aas.instanceRoot}/generated/policy"/>
     *  </jacc-provider>
     *  <jacc-provider policy-provider="com.sun.enterprise.security.jacc.provider.SimplePolicyProvider" name="simple" policy-configuration-factory-provider="com.sun.enterprise.security.jacc.provider.SimplePolicyConfigurationFactory"/>
     *  <audit-module classname="com.sun.enterprise.security.Audit" name="default">
     *      <property name="auditOn" value="false"/>
     *  </audit-module>
     *  <message-security-config auth-layer="SOAP">
     *      <provider-config provider-type="client" provider-id="XWS_ClientProvider" class-name="com.sun.xml.wss.provider.ClientSecurityAuthModule">
     *          <request-policy auth-source="content"/>
     *          <response-policy auth-source="content"/>
     *          <property name="encryption.key.alias" value="s1as"/>
     *          <property name="signature.key.alias" value="s1as"/>
     *          <property name="dynamic.username.password" value="false"/>
     *          <property name="debug" value="false"/>
     *      </provider-config>
     *      <provider-config provider-type="client" provider-id="ClientProvider" class-name="com.sun.xml.wss.provider.ClientSecurityAuthModule">
     *          <request-policy auth-source="content"/>
     *          <response-policy auth-source="content"/>
     *          <property name="encryption.key.alias" value="s1as"/>
     *          <property name="signature.key.alias" value="s1as"/>
     *          <property name="dynamic.username.password" value="false"/>
     *          <property name="debug" value="false"/>
     *          <property name="security.config" value="${com.sun.aas.instanceRoot}/config/wss-server-config-1.0.xml"/>
     *      </provider-config>
     *      <provider-config provider-type="server" provider-id="XWS_ServerProvider" class-name="com.sun.xml.wss.provider.ServerSecurityAuthModule">
     *          <request-policy auth-source="content"/>
     *          <response-policy auth-source="content"/>
     *          <property name="encryption.key.alias" value="s1as"/>
     *          <property name="signature.key.alias" value="s1as"/>
     *          <property name="debug" value="false"/>
     *      </provider-config>
     *      <provider-config provider-type="server" provider-id="ServerProvider" class-name="com.sun.xml.wss.provider.ServerSecurityAuthModule">
     *          <request-policy auth-source="content"/>
     *          <response-policy auth-source="content"/>
     *          <property name="encryption.key.alias" value="s1as"/>
     *          <property name="signature.key.alias" value="s1as"/>
     *          <property name="debug" value="false"/>
     *          <property name="security.config" value="${com.sun.aas.instanceRoot}/config/wss-server-config-1.0.xml"/>
     *      </provider-config>
     *  </message-security-config>
     * </security-service>
     */
    private class SecurityServiceConfigCode implements SingleConfigCode<Config> {

        public Object run(Config config) throws PropertyVetoException {
            try {
                SecurityService ss = config.createChild(SecurityService.class);
                config.setSecurityService(ss);
                createAuthRealm(ss);
                createJaccProvider(ss);
                createAuditModule(ss);
                createMessageSecurityConfig(ss);
                
            } catch (TransactionFailure ex) {
                Logger.getLogger(DefaultConfigUpgrade.class.getName()).log(
                        Level.SEVERE, "Failure creating SecurityService config object", ex);
            }
            return null;
        }
    }

    /* Loop through all auth-realm elements in the template and create AuthRealm config objects.
     * One example from template:
     * <auth-realm classname="com.sun.enterprise.security.auth.realm.file.FileRealm" name="admin-realm">
     *      <property name="file" value="${com.sun.aas.instanceRoot}/config/admin-keyfile"/>
     *      <property name="jaas-context" value="fileRealm"/>
     *  </auth-realm>
     */
    private void createAuthRealm(SecurityService ss) throws PropertyVetoException {
        while (!(parser.getEventType() == START_ELEMENT && parser.getLocalName().equals("jacc-provider"))) {
            try {
                if (parser.next() == START_ELEMENT) {
                    if (parser.getLocalName().equals("auth-realm") && ss != null) {
                        AuthRealm ar = ss.createChild(AuthRealm.class);
                        ss.getAuthRealm().add(ar);
                        for (int i = 0; i < parser.getAttributeCount(); i++) {
                            String attr = parser.getAttributeLocalName(i);
                            String val = parser.getAttributeValue(i);
                            if (attr.equals("classname")) {
                                ar.setClassname(val);
                            }
                            if (attr.equals("name")) {
                                ar.setName(val);
                            }
                        }

                        createAuthRealmProperty(ar);
                    }
                }
            } catch (TransactionFailure ex) {
                Logger.getLogger(DefaultConfigUpgrade.class.getName()).log(
                        Level.SEVERE, "Failure creating AuthRealm", ex);
            } catch (XMLStreamException ex) {
                Logger.getLogger(DefaultConfigUpgrade.class.getName()).log(Level.SEVERE,
                        "Problem parsing auth-realm", ex);
            }
        }
    }

    private void createAuthRealmProperty(AuthRealm ar) throws PropertyVetoException {
        while (!(parser.getEventType() == END_ELEMENT && parser.getLocalName().equals("auth-realm"))) {
            String attr = null;
            String val = null;
            try {
                if (parser.next() == START_ELEMENT) {
                    if (parser.getLocalName().equals("property") && ar != null) {
                        Property p = ar.createChild(Property.class);
                        ar.getProperty().add(p);
                        createProperty(p);
                    }
                }
            } catch (TransactionFailure ex) {
                Logger.getLogger(DefaultConfigUpgrade.class.getName()).log(
                        Level.SEVERE, "Create AuthRealm Property failed. Attr = " + attr + " Val = " + val, ex);
            } catch (XMLStreamException ex) {
                Logger.getLogger(DefaultConfigUpgrade.class.getName()).log(
                        Level.SEVERE, "Problem parsing auth-realm property", ex);
            }
        }
    }

    /* Loop through all jacc-provider elements in the template and create JaccProvider config objects.
     * Cursor should already be at first jacc-provider START_ELEMENT.
     * from template:
     * <jacc-provider policy-provider="com.sun.enterprise.security.provider.PolicyWrapper" name="default" policy-configuration-factory-provider="com.sun.enterprise.security.provider.PolicyConfigurationFactoryImpl">
     *  <property name="repository" value="${com.sun.aas.instanceRoot}/generated/policy"/>
     * </jacc-provider>
     * <jacc-provider policy-provider="com.sun.enterprise.security.jacc.provider.SimplePolicyProvider" name="simple" policy-configuration-factory-provider="com.sun.enterprise.security.jacc.provider.SimplePolicyConfigurationFactory"/>
     */
    private void createJaccProvider(SecurityService ss) throws PropertyVetoException {
        while (!(parser.getEventType() == START_ELEMENT && parser.getLocalName().equals("message-security-config"))) {
            try {
                if (parser.getEventType() == START_ELEMENT || parser.next() == START_ELEMENT) {
                    if (parser.getLocalName().equals("jacc-provider") && ss != null) {
                        JaccProvider jp = ss.createChild(JaccProvider.class);
                        ss.getJaccProvider().add(jp);
                        for (int i = 0; i < parser.getAttributeCount(); i++) {
                            String attr = parser.getAttributeLocalName(i);
                            String val = parser.getAttributeValue(i);
                            if (attr.equals("policy-provider")) {
                                jp.setPolicyProvider(val);
                            }
                            if (attr.equals("name")) {
                                jp.setName(val);
                            }
                            if (attr.equals("policy-configuration-factory-provider")) {
                                jp.setPolicyConfigurationFactoryProvider(val);
                            }
                        }

                        createJaccProviderProperty(jp);
                    }
                }
            } catch (TransactionFailure ex) {
                Logger.getLogger(DefaultConfigUpgrade.class.getName()).log(
                        Level.SEVERE, "Failure creating JaccProvider", ex);
            } catch (XMLStreamException ex) {
                Logger.getLogger(DefaultConfigUpgrade.class.getName()).log(Level.SEVERE,
                        "Problem parsing jacc-provider", ex);
            }
        }
    }

    private void createJaccProviderProperty(JaccProvider jp) throws PropertyVetoException {
        while (!(parser.getEventType() == END_ELEMENT && parser.getLocalName().equals("jacc-provider"))) {
            String attr = null;
            String val = null;
            try {
                if (parser.next() == START_ELEMENT) {
                    if (parser.getLocalName().equals("property") && jp != null) {
                        Property p = jp.createChild(Property.class);
                        jp.getProperty().add(p);
                        for (int i = 0; i < parser.getAttributeCount(); i++) {
                            attr = parser.getAttributeLocalName(i);
                            val = parser.getAttributeValue(i);
                            if (attr.equals("name")) {
                                p.setName(val);
                            }
                            if (attr.equals("value")) {
                                p.setValue(val);
                            }
                        }
                    }
                }
            } catch (TransactionFailure ex) {
                Logger.getLogger(DefaultConfigUpgrade.class.getName()).log(
                        Level.SEVERE, "Create JaccProvider Property failed. Attr = " + attr + " Val = " + val, ex);
            } catch (XMLStreamException ex) {
                Logger.getLogger(DefaultConfigUpgrade.class.getName()).log(
                        Level.SEVERE, "Problem parsing jacc-provider property", ex);
            }
        }
    }

    /* Cursor should be already be at audit-module START_ELEMENT in the template.
     * Create AuditModle config object.
     * from template:
     * <audit-module classname="com.sun.enterprise.security.Audit" name="default">
     *      <property name="auditOn" value="false"/>
     * </audit-module>
     */
    private void createAuditModule(SecurityService ss) throws PropertyVetoException {
        try {
            if (parser.getLocalName().equals("audit-module") && ss != null) {
                AuditModule am = ss.createChild(AuditModule.class);
                ss.getAuditModule().add(am);
                for (int i = 0; i < parser.getAttributeCount(); i++) {
                    String attr = parser.getAttributeLocalName(i);
                    String val = parser.getAttributeValue(i);
                    if (attr.equals("classname")) {
                        am.setClassname(val);
                    }
                    if (attr.equals("name")) {
                        am.setName(val);
                    }
                }

                createAuditModuleProperty(am);
            }

        } catch (TransactionFailure ex) {
            Logger.getLogger(DefaultConfigUpgrade.class.getName()).log(
                    Level.SEVERE, "Failure creating AuditModule config object", ex);
        }

    }

    private void createAuditModuleProperty(AuditModule am) throws PropertyVetoException {
        while (!(parser.getEventType() == END_ELEMENT && parser.getLocalName().equals("audit-module"))) {
            try {
                if (parser.next() == START_ELEMENT) {
                    if (parser.getLocalName().equals("property") && am != null) {
                        Property p = am.createChild(Property.class);
                        am.getProperty().add(p);
                        createProperty(p);
                    }
                }
            } catch (TransactionFailure ex) {
                Logger.getLogger(DefaultConfigUpgrade.class.getName()).log(
                        Level.SEVERE, "Create JaccProvider Property failed.", ex);
            } catch (XMLStreamException ex) {
                Logger.getLogger(DefaultConfigUpgrade.class.getName()).log(
                        Level.SEVERE, "Problem parsing jacc-provider property", ex);
            }
        }
    }

    /* Create MessageSecurityConfig
     * from template:
     * <message-security-config auth-layer="SOAP">
     *  <provider-config provider-type="client" provider-id="XWS_ClientProvider" class-name="com.sun.xml.wss.provider.ClientSecurityAuthModule">
     *      ...................
     *  </provider-config>
     *  <provider-config provider-type="client" provider-id="ClientProvider" class-name="com.sun.xml.wss.provider.ClientSecurityAuthModule">
     *      ..............
     *  </provider-config>
     *  <provider-config provider-type="server" provider-id="XWS_ServerProvider" class-name="com.sun.xml.wss.provider.ServerSecurityAuthModule">
     *      ...........................
     *  </provider-config>
     *  <provider-config provider-type="server" provider-id="ServerProvider" class-name="com.sun.xml.wss.provider.ServerSecurityAuthModule">
     *      .............................
     *  </provider-config>
     * </message-security-config>
     */
    private void createMessageSecurityConfig(SecurityService ss) throws PropertyVetoException {
        while (true) {
            try {
                if (parser.getLocalName().equals("message-security-config") && ss != null) {
                    MessageSecurityConfig msc = ss.createChild(MessageSecurityConfig.class);
                    ss.getMessageSecurityConfig().add(msc);
                    for (int i = 0; i < parser.getAttributeCount(); i++) {
                        String attr = parser.getAttributeLocalName(i);
                        String val = parser.getAttributeValue(i);
                        if (attr.equals("auth-layer")) {
                            msc.setAuthLayer(val);
                        }
                    }

                    createProviderConfig(msc);
                    break;
                }
            } catch (TransactionFailure ex) {
                Logger.getLogger(DefaultConfigUpgrade.class.getName()).log(
                        Level.SEVERE, "Failure creating JaccProvider", ex);
            }
        }
    }

    /* Loop through all provider-config elements in the template and create ProviderConfig config objects.
     * Cursor should already be at first jacc-provider START_ELEMENT.
     * 1 example from template:
     *  <provider-config provider-type="client" provider-id="XWS_ClientProvider" class-name="com.sun.xml.wss.provider.ClientSecurityAuthModule">
     *      <request-policy auth-source="content"/>
     *      <response-policy auth-source="content"/>
     *      <property name="encryption.key.alias" value="s1as"/>
     *      <property name="signature.key.alias" value="s1as"/>
     *      <property name="dynamic.username.password" value="false"/>
     *      <property name="debug" value="false"/>
     *  </provider-config>
     */
    private void createProviderConfig(MessageSecurityConfig msc) throws PropertyVetoException {
        while (!(parser.getEventType() == END_ELEMENT && parser.getLocalName().equals("message-security-config"))) {
            try {
                if (parser.next() == START_ELEMENT) {
                    if (parser.getLocalName().equals("provider-config") && msc != null) {
                        ProviderConfig pc = msc.createChild(ProviderConfig.class);
                        msc.getProviderConfig().add(pc);
                        for (int i = 0; i < parser.getAttributeCount(); i++) {
                            String attr = parser.getAttributeLocalName(i);
                            String val = parser.getAttributeValue(i);
                            if (attr.equals("provider-type")) {
                                pc.setProviderType(val);
                            }
                            if (attr.equals("provider-id")) {
                                pc.setProviderId(val);
                            }
                            if (attr.equals("class-name")) {
                                pc.setClassName(val);
                            }
                        }

                        createRequestPolicy(pc);
                        createResponsePolicy(pc);
                        createProviderConfigProperty(pc);
                    }
                }
            } catch (TransactionFailure ex) {
                Logger.getLogger(DefaultConfigUpgrade.class.getName()).log(
                        Level.SEVERE, "Failure creating ProviderConfig", ex);
            } catch (XMLStreamException ex) {
                Logger.getLogger(DefaultConfigUpgrade.class.getName()).log(Level.SEVERE,
                        "Problem parsing provider-config", ex);
            }
        }
    }

    /* <request-policy auth-source="content"/> */
    private void createRequestPolicy(ProviderConfig pc) throws PropertyVetoException {
        while (true) {
            try {
                if (parser.next() == START_ELEMENT) {
                    if (parser.getLocalName().equals("request-policy") && pc != null) {
                        RequestPolicy rp = pc.createChild(RequestPolicy.class);
                        pc.setRequestPolicy(rp);
                        for (int i = 0; i < parser.getAttributeCount(); i++) {
                            String attr = parser.getAttributeLocalName(i);
                            String val = parser.getAttributeValue(i);
                            if (attr.equals("auth-source")) {
                                rp.setAuthSource(val);
                            }
                        }
                        break;
                    }
                }
            } catch (TransactionFailure ex) {
                Logger.getLogger(DefaultConfigUpgrade.class.getName()).log(
                        Level.SEVERE, "Create ProviderConfig RequestPolicy failed.", ex);
            } catch (XMLStreamException ex) {
                Logger.getLogger(DefaultConfigUpgrade.class.getName()).log(
                        Level.SEVERE, "Problem parsing request-policy property", ex);
            }
        }
    }

    /* <response-policy auth-source="content"/> */
    private void createResponsePolicy(ProviderConfig pc) throws PropertyVetoException {
        while (true) {
            try {
                if (parser.next() == START_ELEMENT) {
                    if (parser.getLocalName().equals("response-policy") && pc != null) {
                        ResponsePolicy rp = pc.createChild(ResponsePolicy.class);
                        pc.setResponsePolicy(rp);
                        for (int i = 0; i < parser.getAttributeCount(); i++) {
                            String attr = parser.getAttributeLocalName(i);
                            String val = parser.getAttributeValue(i);
                            if (attr.equals("auth-source")) {
                                rp.setAuthSource(val);
                            }
                        }
                        break;
                    }
                }
            } catch (TransactionFailure ex) {
                Logger.getLogger(DefaultConfigUpgrade.class.getName()).log(
                        Level.SEVERE, "Create ProviderConfig RequestPolicy failed.", ex);
            } catch (XMLStreamException ex) {
                Logger.getLogger(DefaultConfigUpgrade.class.getName()).log(
                        Level.SEVERE, "Problem parsing request-policy property", ex);
            }
        }
    }

    private void createProviderConfigProperty(ProviderConfig pc) throws PropertyVetoException {
        while (!(parser.getEventType() == END_ELEMENT && parser.getLocalName().equals("provider-config"))) {
            try {
                if (parser.next() == START_ELEMENT) {
                    if (parser.getLocalName().equals("property") && pc != null) {
                        Property p = pc.createChild(Property.class);
                        pc.getProperty().add(p);
                        createProperty(p);
                    }
                }
            } catch (TransactionFailure ex) {
                Logger.getLogger(DefaultConfigUpgrade.class.getName()).log(
                        Level.SEVERE, "Create ProviderConfig Property failed", ex);
            } catch (XMLStreamException ex) {
                Logger.getLogger(DefaultConfigUpgrade.class.getName()).log(
                        Level.SEVERE, "Problem parsing provider-config property", ex);
            }
        }
    }

    /*
     * Creates the transaction-service object using data from glassfish3\glassfish\lib\templates\domain.xml
     * <transaction-service tx-log-dir="${com.sun.aas.instanceRoot}/logs" automatic-recovery="true"/>
     */
    private class TransactionServiceConfigCode implements SingleConfigCode<Config> {

        public Object run(Config config) throws PropertyVetoException {
            try {
                TransactionService ts = config.createChild(TransactionService.class);
                config.setTransactionService(ts);
                for (int i = 0; i < parser.getAttributeCount(); i++) {
                    String attr = parser.getAttributeLocalName(i);
                    String val = parser.getAttributeValue(i);
                    if (attr.equals("tx-log-dir")) {
                        ts.setTxLogDir(val);
                    }
                    if (attr.equals("automatic-recovery")) {
                        ts.setAutomaticRecovery(val);
                    }
                }
            } catch (TransactionFailure ex) {
                Logger.getLogger(DefaultConfigUpgrade.class.getName()).log(
                        Level.SEVERE, "Failure create TransactionService config object", ex);
            }
            return null;
        }
    }

    /*
     * Creates the diagnostic-service object using data from glassfish3\glassfish\lib\templates\domain.xml
     * <diagnostic-service/>
     */
    private static class DiagnosticServiceConfigCode implements SingleConfigCode<Config> {

        public Object run(Config config) throws PropertyVetoException, TransactionFailure {
            DiagnosticService ds = config.createChild(DiagnosticService.class);
            config.setDiagnosticService(ds);
            return null;
        }
    }

    /*
     * Creates the java-config object using data from glassfish3\glassfish\lib\templates\domain.xml
     * <java-config debug-options="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,
     * address=${JAVA_DEBUGGER_PORT}" system-classpath="" classpath-suffix="">
     */
    private class JavaConfigCode implements SingleConfigCode<Config> {

        public Object run(Config config) throws PropertyVetoException {
            try {
                JavaConfig jc = config.createChild(JavaConfig.class);
                config.setJavaConfig(jc);
                for (int i = 0; i < parser.getAttributeCount(); i++) {
                    String attr = parser.getAttributeLocalName(i);
                    String val = parser.getAttributeValue(i);
                    if (attr.equals("debug-options")) {
                        jc.setDebugOptions(val);
                    }
                    if (attr.equals("system-classpath")) {
                        jc.setSystemClasspath(val);
                    }
                    if (attr.equals("classpath-suffix")) {
                        jc.setClasspathSuffix(val);
                    }
                }

                // All <jvm-options>some jvm option</jvm-options>
                createJvmOptions(jc);

            } catch (TransactionFailure ex) {
                Logger.getLogger(DefaultConfigUpgrade.class.getName()).log(
                        Level.SEVERE, "Failure creating JavaConfig config object", ex);
            }
            return null;
        }
    }

    private void createJvmOptions(JavaConfig jc) {
        while (!(parser.getEventType() == END_ELEMENT && parser.getLocalName().equals("java-config"))) {
            try {
                int eventType = parser.next();
                if (eventType == START_ELEMENT) {
                    if (parser.getLocalName().equals("jvm-options") && jc != null) {
                        jc.getJvmOptions().add(parser.getElementText());
                    }
                }
            } catch (XMLStreamException ex) {
                Logger.getLogger(DefaultConfigUpgrade.class.getName()).log(
                        Level.SEVERE, "Problem parsing jvm-options", ex);
            }
        }
    }

    /*
     * Creates the availability-service object using data from glassfish3\glassfish\lib\templates\domain.xml
     * <availability-service>
     *  <web-container-availability availability-enabled="true" persistence-frequency="web-method" persistence-scope="session" persistence-type="replicated" sso-failover-enabled="false"/>
     *  <ejb-container-availability availability-enabled="true" sfsb-store-pool-name="jdbc/hastore"/>
     *  <jms-availability availability-enabled="false"/>
     * </availability-service>
     */
    private class AvailabilityServiceConfigCode implements SingleConfigCode<Config> {

        public Object run(Config config) throws PropertyVetoException {
            try {
                AvailabilityService as = config.createChild(AvailabilityService.class);
                config.setAvailabilityService(as);

                createWebContainerAvailability(as);
                createEjbContainerAvailability(as);
                createJmsAvailability(as);
            } catch (TransactionFailure ex) {
                Logger.getLogger(DefaultConfigUpgrade.class.getName()).log(
                        Level.SEVERE, "Failure creating AvailabilityService config object", ex);
            }

            return null;
        }
    }

    /* <web-container-availability availability-enabled="true" persistence-frequency="web-method"
     * persistence-scope="session" persistence-type="replicated" sso-failover-enabled="false"/>
     */
    private void createWebContainerAvailability(AvailabilityService as) throws PropertyVetoException {
        while (true) {
            try {
                if (parser.next() == START_ELEMENT) {
                    if (parser.getLocalName().equals("web-container-availability") && as != null) {
                        WebContainerAvailability wca = as.createChild(WebContainerAvailability.class);
                        as.setWebContainerAvailability(wca);
                        for (int i = 0; i < parser.getAttributeCount(); i++) {
                            String attr = parser.getAttributeLocalName(i);
                            String val = parser.getAttributeValue(i);
                            if (attr.equals("availability-enabled")) {
                                wca.setAvailabilityEnabled(val);
                            }
                            if (attr.equals("persistence-frequency")) {
                                wca.setPersistenceFrequency(val);
                            }
                            if (attr.equals("persistence-scope")) {
                                wca.setPersistenceScope(val);
                            }
                        }
                        break;
                    }
                }
            } catch (XMLStreamException ex) {
                Logger.getLogger(DefaultConfigUpgrade.class.getName()).log(
                        Level.SEVERE, "Problem parsing web-container-availability element in domain.xml", ex);
            } catch (TransactionFailure ex) {
                Logger.getLogger(DefaultConfigUpgrade.class.getName()).log(
                        Level.SEVERE, "Failure creating WebContainerAvailability config object", ex);
            }
        }
    }

    /* <ejb-container-availability availability-enabled="true" sfsb-store-pool-name="jdbc/hastore"/> */
    private void createEjbContainerAvailability(AvailabilityService as) throws PropertyVetoException {
        while (true) {
            try {
                if (parser.next() == START_ELEMENT) {
                    if (parser.getLocalName().equals("ejb-container-availability") && as != null) {

                        EjbContainerAvailability eca = as.createChild(EjbContainerAvailability.class);
                        as.setEjbContainerAvailability(eca);
                        for (int i = 0; i < parser.getAttributeCount(); i++) {
                            String attr = parser.getAttributeLocalName(i);
                            String val = parser.getAttributeValue(i);
                            if (attr.equals("availability-enabled")) {
                                eca.setAvailabilityEnabled(val);
                            }
                            if (attr.equals("sfsb-store-pool-name")) {
                                eca.setSfsbStorePoolName(val);
                            }
                        }
                        break;
                    }
                }
            } catch (XMLStreamException ex) {
                Logger.getLogger(DefaultConfigUpgrade.class.getName()).log(
                        Level.SEVERE, "Problem parsing ejb-container-availability element in domain.xml template", ex);
            } catch (TransactionFailure ex) {
                Logger.getLogger(DefaultConfigUpgrade.class.getName()).log(
                        Level.SEVERE, "Failure creating EjbContainerAvailability config object", ex);
            }
        }
    }

    /* <jms-availability availability-enabled="false"/> */
    private void createJmsAvailability(AvailabilityService as) throws PropertyVetoException {
        while (true) {
            try {
                if (parser.next() == START_ELEMENT) {
                    if (parser.getLocalName().equals("jms-availability") && as != null) {
                        JmsAvailability ja = as.createChild(JmsAvailability.class);
                        as.setJmsAvailability(ja);
                        for (int i = 0; i < parser.getAttributeCount(); i++) {
                            String attr = parser.getAttributeLocalName(i);
                            String val = parser.getAttributeValue(i);
                            if (attr.equals("availability-enabled")) {
                                ja.setAvailabilityEnabled(val);
                            }
                        }
                        break;
                    }
                }
            } catch (TransactionFailure ex) {
                Logger.getLogger(DefaultConfigUpgrade.class.getName()).log(
                        Level.SEVERE, "Failure creating JmsAvailability config object", ex);
            } catch (XMLStreamException ex) {
                Logger.getLogger(DefaultConfigUpgrade.class.getName()).log(
                        Level.SEVERE, "Problem parsing jms-availability element in domain.xml", ex);
            }
        }
    }

    /*
     * Creates the network-config object using data from glassfish3\glassfish\lib\templates\domain.xml
     * <network-config>
     */
    private class NetworkConfigCode implements SingleConfigCode<Config> {

        public Object run(Config config) throws PropertyVetoException {
            try {
                NetworkConfig nc = config.createChild(NetworkConfig.class);
                config.setNetworkConfig(nc);

                createProtocols(nc);
                createNetworkListeners(nc);
                createTransports(nc);
            } catch (TransactionFailure ex) {
                Logger.getLogger(DefaultConfigUpgrade.class.getName()).log(
                        Level.SEVERE, "Failure creating NetworkConfig config object", ex);
            }

            return null;
        }
    }

    /* <protocols>
     *  <protocol name="http-listener-1">
     *      <http default-virtual-server="server">
     *          <file-cache/>
     *      </http>
     *  </protocol>
     *  <protocol security-enabled="true" name="http-listener-2">
     *      <http default-virtual-server="server">
     *      <file-cache/>
     *      </http>
     *      <ssl classname="com.sun.enterprise.security.ssl.GlassfishSSLImpl" ssl3-enabled="false" cert-nickname="s1as"/>
     *  </protocol>
     *  <protocol name="admin-listener">
     *      <http default-virtual-server="__asadmin" max-connections="250">
     *          <file-cache enabled="false"/>
     *      </http>
     *</protocol>
     */
    private void createProtocols(NetworkConfig nc) throws PropertyVetoException {
        while (true) {
            try {
                if (parser.next() == START_ELEMENT) {
                    if (parser.getLocalName().equals("protocols") && nc != null) {
                        Protocols ps = nc.createChild(Protocols.class);
                        nc.setProtocols(ps);
                        createProtocol(ps);
                        break;
                    }
                }
            } catch (TransactionFailure ex) {
                Logger.getLogger(DefaultConfigUpgrade.class.getName()).log(
                        Level.SEVERE, "Failure creating Protocols config object", ex);
            } catch (XMLStreamException ex) {
                Logger.getLogger(DefaultConfigUpgrade.class.getName()).log(
                        Level.SEVERE, "Problem parsing protocols element in domain.xml template", ex);
            }
        }
    }

    /* <protocol security-enabled="true" name="http-listener-2"> (1 example with most attributes) */
    private void createProtocol(Protocols ps) throws PropertyVetoException {
        while (!(parser.getEventType() == END_ELEMENT && parser.getLocalName().equals("protocols"))) {
            try {
                if (parser.next() == START_ELEMENT) {
                    if (parser.getLocalName().equals("protocol") && ps != null) {
                        Protocol p = ps.createChild(Protocol.class);
                        ps.getProtocol().add(p);
                        for (int i = 0; i < parser.getAttributeCount(); i++) {
                            String attr = parser.getAttributeLocalName(i);
                            String val = parser.getAttributeValue(i);
                            if (attr.equals("security-enabled")) {
                                p.setSecurityEnabled(val);
                            }
                            if (attr.equals("name")) {
                                p.setName(val);
                            }
                        }
                        createHttp(p);
                        createSsl(p);
                    }
                }
            } catch (TransactionFailure ex) {
                Logger.getLogger(DefaultConfigUpgrade.class.getName()).log(
                        Level.SEVERE, "Failure creating Protocol config object", ex);
            } catch (XMLStreamException ex) {
                Logger.getLogger(DefaultConfigUpgrade.class.getName()).log(
                        Level.SEVERE, "Problem parsing protocol element in domain.xml template", ex);
            }
        }
    }

    /* <http default-virtual-server="__asadmin" max-connections="250"> (1 example with most attributes)*/
    private void createHttp(Protocol p) throws PropertyVetoException {
        while (true) {
            try {
                if (parser.next() == START_ELEMENT) {
                    if (parser.getLocalName().equals("http") && p != null) {
                        Http h = p.createChild(Http.class);
                        p.setHttp(h);
                        for (int i = 0; i < parser.getAttributeCount(); i++) {
                            String attr = parser.getAttributeLocalName(i);
                            String val = parser.getAttributeValue(i);
                            if (attr.equals("default-virtual-server")) {
                                h.setDefaultVirtualServer(val);
                            }
                            if (attr.equals("max-connections")) {
                                h.setMaxConnections(val);
                            }
                        }
                        createFileCache(h);
                        break;
                    }
                }
            } catch (TransactionFailure ex) {
                Logger.getLogger(DefaultConfigUpgrade.class.getName()).log(
                        Level.SEVERE, "Failure creating Http config object", ex);
            } catch (XMLStreamException ex) {
                Logger.getLogger(DefaultConfigUpgrade.class.getName()).log(
                        Level.SEVERE, "Problem parsing http element in domain.xml template", ex);
            }
        }
    }

    /* <file-cache enabled="false"/> (1 example with most attributes)*/
    private void createFileCache(Http h) throws PropertyVetoException {
        while (true) {
            try {
                if (parser.next() == START_ELEMENT) {
                    if (parser.getLocalName().equals("file-cache") && h != null) {
                        FileCache fc = h.createChild(FileCache.class);
                        h.setFileCache(fc);
                        for (int i = 0; i < parser.getAttributeCount(); i++) {
                            String attr = parser.getAttributeLocalName(i);
                            String val = parser.getAttributeValue(i);
                            if (attr.equals("enabled")) {
                                fc.setEnabled(val);
                            }
                        }
                        break;
                    }
                }
            } catch (TransactionFailure ex) {
                Logger.getLogger(DefaultConfigUpgrade.class.getName()).log(
                        Level.SEVERE, "Failure creating FileCache config object", ex);
            } catch (XMLStreamException ex) {
                Logger.getLogger(DefaultConfigUpgrade.class.getName()).log(
                        Level.SEVERE, "Problem parsing file-cache element in domain.xml template", ex);
            }
        }
    }

    /* <ssl classname="com.sun.enterprise.security.ssl.GlassfishSSLImpl" ssl3-enabled="false" cert-nickname="s1as"/> */
    private void createSsl(Protocol p) throws PropertyVetoException {
        while (!(parser.getEventType() == END_ELEMENT && parser.getLocalName().equals("protocol"))) {
            try {
                if (parser.next() == START_ELEMENT) {
                    if (parser.getLocalName().equals("ssl") && p != null) {
                        Ssl ssl = p.createChild(Ssl.class);
                        p.setSsl(ssl);
                        for (int i = 0; i < parser.getAttributeCount(); i++) {
                            String attr = parser.getAttributeLocalName(i);
                            String val = parser.getAttributeValue(i);
                            if (attr.equals("classname")) {
                                ssl.setClassname(val);
                            }
                            if (attr.equals("ssl3-enabled")) {
                                ssl.setSsl3Enabled(val);
                            }
                            if (attr.equals("cert-nickname")) {
                                ssl.setCertNickname(val);
                            }
                        }
                        break;
                    }
                }
            } catch (TransactionFailure ex) {
                Logger.getLogger(DefaultConfigUpgrade.class.getName()).log(
                        Level.SEVERE, "Failure creating Ssl config object", ex);
            } catch (XMLStreamException ex) {
                Logger.getLogger(DefaultConfigUpgrade.class.getName()).log(
                        Level.SEVERE, "Problem parsing ssl element in domain.xml template", ex);
            }
        }
    }

    /* <network-listeners>
     *   <network-listener address="0.0.0.0" port="${HTTP_LISTENER_PORT}" protocol="http-listener-1" transport="tcp" name="http-listener-1" thread-pool="http-thread-pool"/>
     *   <network-listener address="0.0.0.0" port="${HTTP_SSL_LISTENER_PORT}" protocol="http-listener-2" transport="tcp" name="http-listener-2" thread-pool="http-thread-pool"/>
     *   <network-listener port="${ASADMIN_LISTENER_PORT}" protocol="admin-listener" transport="tcp" name="admin-listener" thread-pool="http-thread-pool"/>
     * </network-listeners>
     */
    private void createNetworkListeners(NetworkConfig nc) throws PropertyVetoException {
        while (true) {
            try {
                if (parser.next() == START_ELEMENT) {
                    if (parser.getLocalName().equals("network-listeners") && nc != null) {
                        NetworkListeners nls = nc.createChild(NetworkListeners.class);
                        nc.setNetworkListeners(nls);
                        createNetworkListener(nls);
                        break;
                    }
                }
            } catch (TransactionFailure ex) {
                Logger.getLogger(DefaultConfigUpgrade.class.getName()).log(
                        Level.SEVERE, "Failure creating NetworkListeners config object", ex);
            } catch (XMLStreamException ex) {
                Logger.getLogger(DefaultConfigUpgrade.class.getName()).log(
                        Level.SEVERE, "Problem parsing network-listeners element in domain.xml template", ex);
            }
        }
    }

    /* Loop through all the network-listener elements inside network-listeners of the template
     * and create the NetworkListener config objects with attribute values from the template.
     * <network-listener address="0.0.0.0" port="${HTTP_LISTENER_PORT}" protocol="http-listener-1"
     *  transport="tcp" name="http-listener-1" thread-pool="http-thread-pool"/> (1 example)
     */
    private void createNetworkListener(NetworkListeners nls) throws PropertyVetoException {
        while (!(parser.getEventType() == END_ELEMENT && parser.getLocalName().equals("network-listeners"))) {
            try {
                if (parser.next() == START_ELEMENT) {
                    if (parser.getLocalName().equals("network-listener") && nls != null) {
                        NetworkListener nl = nls.createChild(NetworkListener.class);
                        nls.getNetworkListener().add(nl);
                        for (int i = 0; i < parser.getAttributeCount(); i++) {
                            String attr = parser.getAttributeLocalName(i);
                            String val = parser.getAttributeValue(i);
                            if (attr.equals("address")) {
                                nl.setAddress(val);
                            }
                            if (attr.equals("port")) {
                                nl.setPort(val);
                            }
                            if (attr.equals("protocol")) {
                                nl.setProtocol(val);
                            }
                            if (attr.equals("transport")) {
                                nl.setTransport(val);
                            }
                            if (attr.equals("name")) {
                                nl.setName(val);
                            }
                            if (attr.equals("thread-pool")) {
                                nl.setThreadPool(val);
                            }
                        }
                    }
                }
            } catch (TransactionFailure ex) {
                Logger.getLogger(DefaultConfigUpgrade.class.getName()).log(
                        Level.SEVERE, "Failure creating NetworkListener config object", ex);
            } catch (XMLStreamException ex) {
                Logger.getLogger(DefaultConfigUpgrade.class.getName()).log(
                        Level.SEVERE, "Problem parsing network-listener element in domain.xml template", ex);
            }
        }
    }

    /* <transports>
     *   <transport name="tcp"/>
     * </transports>
     */
    private void createTransports(NetworkConfig nc) throws PropertyVetoException {
        while (true) {
            try {
                if (parser.next() == START_ELEMENT) {
                    if (parser.getLocalName().equals("transports") && nc != null) {
                        Transports ts = nc.createChild(Transports.class);
                        nc.setTransports(ts);
                        createTransport(ts);
                        break;
                    }
                }
            } catch (TransactionFailure ex) {
                Logger.getLogger(DefaultConfigUpgrade.class.getName()).log(
                        Level.SEVERE, "Failure creating Transports config object", ex);
            } catch (XMLStreamException ex) {
                Logger.getLogger(DefaultConfigUpgrade.class.getName()).log(
                        Level.SEVERE, "Problem parsing transports element in domain.xml template", ex);
            }
        }
    }

    /* <transport name="tcp"/> */
    private void createTransport(Transports ts) throws PropertyVetoException {
        while (true) {
            try {
                if (parser.next() == START_ELEMENT) {
                    if (parser.getLocalName().equals("transport") && ts != null) {
                        Transport t = ts.createChild(Transport.class);
                        ts.getTransport().add(t);
                        for (int i = 0; i < parser.getAttributeCount(); i++) {
                            String attr = parser.getAttributeLocalName(i);
                            String val = parser.getAttributeValue(i);
                            if (attr.equals("name")) {
                                t.setName(val);
                            }
                        }
                        break;
                    }
                }
            } catch (TransactionFailure ex) {
                Logger.getLogger(DefaultConfigUpgrade.class.getName()).log(
                        Level.SEVERE, "Failure creating Transport config object", ex);
            } catch (XMLStreamException ex) {
                Logger.getLogger(DefaultConfigUpgrade.class.getName()).log(
                        Level.SEVERE, "Problem parsing transport element in domain.xml template", ex);
            }
        }
    }

    /*
     * Creates the thread-pools object using data from glassfish3\glassfish\lib\templates\domain.xml
     * <thread-pools>
     *   <thread-pool name="http-thread-pool"/>
     *   <thread-pool max-thread-pool-size="200" idle-thread-timeout-in-seconds="120" name="thread-pool-1"/>
     * </thread-pools>
     */
    private class ThreadPoolsConfigCode implements SingleConfigCode<Config> {

        public Object run(Config config) throws PropertyVetoException {
            try {
                ThreadPools tps = config.createChild(ThreadPools.class);
                config.setThreadPools(tps);
                createThreadPool(tps);
            } catch (TransactionFailure ex) {
                Logger.getLogger(DefaultConfigUpgrade.class.getName()).log(
                        Level.SEVERE, "Failure to create ThreadPools config object", ex);
            }
            return null;
        }
    }

    /*
     * Loop through all the thread-pool elements inside thread-pools of the template
     * and create the ThreadPool config objects with attribute values from the template.
     * One example of thread-pool element:
     * <thread-pool max-thread-pool-size="200" idle-thread-timeout-in-seconds="120" name="thread-pool-1"/>
     */
    private void createThreadPool(ThreadPools ts) throws PropertyVetoException {
        while (!(parser.getEventType() == END_ELEMENT && parser.getLocalName().equals("thread-pools"))) {
            try {
                if (parser.next() == START_ELEMENT) {
                    if (parser.getLocalName().equals("thread-pool") && ts != null) {
                        ThreadPool t = ts.createChild(ThreadPool.class);
                        ts.getThreadPool().add(t);
                        for (int i = 0; i < parser.getAttributeCount(); i++) {
                            String attr = parser.getAttributeLocalName(i);
                            String val = parser.getAttributeValue(i);
                            if (attr.equals("max-thread-pool-size")) {
                                t.setMaxThreadPoolSize(val);
                            }
                            if (attr.equals("idle-thread-timeout-in-seconds")) {
                                t.setIdleThreadTimeoutSeconds(val);
                            }
                            if (attr.equals("name")) {
                                t.setName(val);
                            }
                        }
                    }
                }
            } catch (TransactionFailure ex) {
                Logger.getLogger(DefaultConfigUpgrade.class.getName()).log(
                        Level.SEVERE, "Failure creating ThreadPool config object", ex);
            } catch (XMLStreamException ex) {
                Logger.getLogger(DefaultConfigUpgrade.class.getName()).log(
                        Level.SEVERE, "Problem parsing thread-pool element in domain.xml template", ex);
            }
        }
    }

    /*
     * Creates the management-rules object using data from glassfish3\glassfish\lib\templates\domain.xml
     * <management-rules/>
     */
    private static class ManagementRulesConfigCode implements SingleConfigCode<Config> {

        public Object run(Config config) throws PropertyVetoException {
            try {
                ManagementRules mr = config.createChild(ManagementRules.class);
                config.setManagementRules(mr);
            } catch (TransactionFailure ex) {
                Logger.getLogger(DefaultConfigUpgrade.class.getName()).log(
                        Level.SEVERE, "Failure creating ManagementRules config object", ex);
            }
            return null;
        }
    }

    /*
     * Creates the system-property elements using data from glassfish3\glassfish\lib\templates\domain.xml
     */
    private class SystemPropertyConfigCode implements SingleConfigCode<Config> {

        public Object run(Config config) throws PropertyVetoException {

            createSystemProperty(config);

            return null;
        }
    }

    /* Loop through all the system-property elements inside default-config of the template
     * and create the SystemProperty config objects with attribute values from the template.
     * <system-property name="ASADMIN_LISTENER_PORT" value="24848"/>
     * <system-property name="HTTP_LISTENER_PORT" value="28080"/>
     * <system-property name="HTTP_SSL_LISTENER_PORT" value="28181"/>
     * <system-property name="JMS_PROVIDER_PORT" value="27676"/>
     * <system-property name="IIOP_LISTENER_PORT" value="23700"/>
     * <system-property name="IIOP_SSL_LISTENER_PORT" value="23820"/>
     * <system-property name="IIOP_SSL_MUTUALAUTH_PORT" value="23920"/>
     * <system-property name="JMX_SYSTEM_CONNECTOR_PORT" value="28686"/>
     * <system-property name="OSGI_SHELL_TELNET_PORT" value="26666"/>
     * <system-property name="JAVA_DEBUGGER_PORT" value="29009"/>
     */
    private void createSystemProperty(Config defaultConfig) throws PropertyVetoException {
        while (!(parser.getEventType() == END_ELEMENT && parser.getLocalName().equals("config"))) {
            try {
                if (parser.next() == START_ELEMENT) {
                    if (parser.getLocalName().equals("system-property") && defaultConfig != null) {
                        SystemProperty sp = defaultConfig.createChild(SystemProperty.class);
                        defaultConfig.getSystemProperty().add(sp);
                        for (int i = 0; i < parser.getAttributeCount(); i++) {
                            String attr = parser.getAttributeLocalName(i);
                            String val = parser.getAttributeValue(i);
                            if (attr.equals("name")) {
                                sp.setName(val);
                            }
                            if (attr.equals("value")) {
                                sp.setValue(val);
                            }
                        }
                    }
                }
            } catch (TransactionFailure ex) {
                Logger.getLogger(DefaultConfigUpgrade.class.getName()).log(
                        Level.SEVERE, "Failure creating SystemProperty config object", ex);
            } catch (XMLStreamException ex) {
                Logger.getLogger(DefaultConfigUpgrade.class.getName()).log(
                        Level.SEVERE, "Problem parsing system-property element in domain.xml template", ex);
            }
        }
    }

    private void createProperty(Property p) throws PropertyVetoException {
        for (int i = 0; i < parser.getAttributeCount(); i++) {
            String attr = parser.getAttributeLocalName(i);
            String val = parser.getAttributeValue(i);
            if (attr.equals("name")) {
                p.setName(val);
            }
            if (attr.equals("value")) {
                p.setValue(val);
            }
        }
    }

    private void createParser(String template) throws FileNotFoundException, XMLStreamException {
        if (template != null) {
            reader = new InputStreamReader(new FileInputStream(template));
            parser = XMLInputFactory.newInstance().createXMLStreamReader(
                    template, reader);
        }
    }
    private XMLStreamReader parser;
    private InputStreamReader reader;
}
