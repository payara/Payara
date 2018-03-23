/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2013 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.appserv.server.util.Version;
import java.beans.PropertyVetoException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.List;

import javax.inject.Inject;
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
import com.sun.enterprise.config.util.ConfigApiLoggerInfo;
import com.sun.enterprise.util.LocalStringManagerImpl;
import org.glassfish.grizzly.config.dom.*;
import org.glassfish.api.admin.config.ConfigurationUpgrade;
import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PostConstruct;
import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.config.*;
import org.jvnet.hk2.config.types.Property;
 import static com.sun.enterprise.config.util.ConfigApiLoggerInfo.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

/**
 * Upgrade service to add the default-config if it doesn't exist.
 * 3.0.1 and v2.x developer profile do not have default-config.
 * The data to populate the default-config is taken from
 * glassfish4\glassfish\lib\templates\domain.xml.  This class uses the StAX
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

    @Inject
    ServiceLocator habitat;
    
    private static final String DEFAULT_CONFIG = "default-config";
    private static final String INSTALL_ROOT = "com.sun.aas.installRoot";
    private static final LocalStringManagerImpl localStrings =
            new LocalStringManagerImpl(DefaultConfigUpgrade.class);
    private final static Logger logger =ConfigApiLoggerInfo.getLogger();

    @Override
    public void postConstruct() {

        Config defaultConfig = configs.getConfigByName(DEFAULT_CONFIG);
        if (defaultConfig != null) {
            logger.log(
                    Level.INFO,existingDefaultConfig);
            return;
        }

        String installRoot = System.getProperty(INSTALL_ROOT);
        if (installRoot == null) {
            logger.log(
                    Level.INFO, installRootIsNull);
            return;
        }

        logger.log(
                Level.INFO, runningDefaultConfigUpgrade);

        InputStream template = null;
        ZipFile templatezip = null;
        String templatefilename = Version.getDefaultDomainTemplate();
        File templatefile = new File(new File(new File(
                new File(installRoot, "common"), "templates"), "gf"), templatefilename);    
        try {
            templatezip = new ZipFile(templatefile);
            ZipEntry domEnt = templatezip.getEntry("config/domain.xml");
            if (domEnt == null) {
                throw new RuntimeException(localStrings.getLocalString(
                    "DefaultConfigUpgrade.cannotGetDomainXmlTemplate", 
                    "DefaultConfigUpgrade failed. Cannot get default domain.xml from {0)",
                    templatefile.getAbsolutePath()));
            }
            template = templatezip.getInputStream(domEnt);
            
            ConfigSupport.apply(new MinDefaultConfigCode(), configs);
            defaultConfig = configs.getConfigByName(DEFAULT_CONFIG);

            createParser(template);

            createDefaultConfigAttr(defaultConfig);

            createHttpServiceConfig(defaultConfig);

            createAdminServiceConfig(defaultConfig);

            createLogServiceConfig(defaultConfig);

            createSecurityServiceConfig(defaultConfig);

            createJavaConfig(defaultConfig);

            createAvailabilityService(defaultConfig);

            createNetworkConfig(defaultConfig);

            createThreadPools(defaultConfig);

            createSystemProperties(defaultConfig);

        } catch (TransactionFailure ex) {
            logger.log(
                    Level.SEVERE, defaultConfigUpgradeFailure, ex);
        } catch (FileNotFoundException ex) {
            logger.log(
                    Level.SEVERE, defaultConfigUpgradeFailure, ex);
        } catch (XMLStreamException ex) {
            logger.log(
                    Level.SEVERE, defaultConfigUpgradeFailure, ex);
        } catch (IOException ex) {
            throw new RuntimeException(localStrings.getLocalString(
                    "DefaultConfigUpgrade.cannotGetDomainXmlTemplate", 
                    "DefaultConfigUpgrade failed. Cannot get default domain.xml from {0)",
                    templatefile.getAbsolutePath()), ex);
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
            try {
                if (template != null) {
                    template.close();
                }
            } catch (Exception e) {
                // ignore
            }
            try {
                if (templatezip != null) {
                    templatezip.close();
                }
            } catch (Exception e) {
                // ignore
            }
        }
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
                logger.log(
                        Level.SEVERE, failureCreatingSecurityServiceConfig, ex);
            } catch (XMLStreamException ex) {
                logger.log(
                        Level.SEVERE, problemParsingSecurityServiceConfig, ex);
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
     * Creates the http-service object using data from glassfish4\glassfish\lib\templates\domain.xml
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
            logger.log(
                    Level.SEVERE, failureCreatingHttpServiceVS, ex);
        } catch (XMLStreamException ex) {
            logger.log(
                    Level.SEVERE, problemParsingHttpServiceVs, ex);
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
                logger.log(
                        Level.SEVERE, failureCreatingHttpServiceVS, ex);
            } catch (XMLStreamException ex) {
                logger.log(
                        Level.SEVERE, problemParsingHttpServiceVs, ex);
            }
        }
    }

    /*
     * Creates the admin-service object using data from glassfish4\glassfish\lib\templates\domain.xml
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
            
            createAdminServiceProperty(adminService);

            return null;
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
                logger.log(
                        Level.SEVERE, failedToCreateAdminService, ex);
            } catch (XMLStreamException ex) {
                logger.log(
                        Level.SEVERE, problemParsingAdminService, ex);
            }
        }
    }

    /*
     * Creates the log-service object using data from glassfish4\glassfish\lib\templates\domain.xml
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
                logger.log(
                        Level.SEVERE, failureCreatingLogService, ex);
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
            logger.log(
                    Level.SEVERE, failureCreateModuleLogLevel, ex);
        } catch (XMLStreamException ex) {
            logger.log(
                    Level.SEVERE, problemParsingModuleLogLevel, ex);
        }
    }

    /*
     * Creates the security-service object using data from glassfish4\glassfish\lib\templates\domain.xml
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
                logger.log(
                        Level.SEVERE, failureCreatingSecurityService, ex);
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
                logger.log(
                        Level.SEVERE, failureCreatingAuthRealm, ex);
            } catch (XMLStreamException ex) {
                logger.log(Level.SEVERE,
                        failureParsingAuthRealm, ex);
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
                logger.log(
                        Level.SEVERE, failureCreatingAuthRealmProperty , new Object[]{attr, val, ex});
            } catch (XMLStreamException ex) {
                logger.log(
                        Level.SEVERE, failureParsingAuthRealmProperty, ex);
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
        while (!(parser.getEventType() == START_ELEMENT && parser.getLocalName().equals("audit-module"))) {
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
                logger.log(
                        Level.SEVERE, failureCreatingJaccProvider, ex);
            } catch (XMLStreamException ex) {
                logger.log(Level.SEVERE,
                        problemParsingJaacProvider, ex);
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
                logger.log(
                        Level.SEVERE, failureCreatingJaccProviderAttr , new Object[]{attr, val, ex});
            } catch (XMLStreamException ex) {
                logger.log(
                        Level.SEVERE, problemParsingJaacProviderAttr, ex);
            }
        }
    }

    /* Cursor should be already be at audit-module START_ELEMENT in the template.
     * Create AuditModle config object.
     * from template:
     * <audit-module classname="com.sun.enterprise.security.ee.Audit" name="default">
     *      <property name="auditOn" value="false"/>
     * </audit-module>
     */
    private void createAuditModule(SecurityService ss) throws PropertyVetoException {
        while (!(parser.getEventType() == START_ELEMENT && parser.getLocalName().equals("message-security-config"))) {
            try {
                if (parser.getEventType() == START_ELEMENT || parser.next() == START_ELEMENT) {
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
                }
            } catch (TransactionFailure ex) {
                logger.log(
                        Level.SEVERE, failureCreatingAuditModule, ex);
            } catch (XMLStreamException ex) {
                logger.log(
                        Level.SEVERE, failureCreatingAuditModule, ex);
            }
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
                logger.log(
                        Level.SEVERE, failureCreatingAuditModuleAttr, ex);
            } catch (XMLStreamException ex) {
                logger.log(
                        Level.SEVERE, failureParsingAuditModuleProp, ex);
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
                logger.log(
                        Level.SEVERE, failureCreatingJaccProvider, ex);
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
                logger.log(
                        Level.SEVERE, failureCreatingProviderConfig, ex);
            } catch (XMLStreamException ex) {
                logger.log(Level.SEVERE,
                        ProblemParsingProviderConfig, ex);
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
                logger.log(
                        Level.SEVERE, createProviderConfigRequestPolicyFailed, ex);
            } catch (XMLStreamException ex) {
                logger.log(
                        Level.SEVERE, problemParsingRequestPolicyProp, ex);
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
                logger.log(
                        Level.SEVERE, createProviderConfigRequestPolicyFailed, ex);
            } catch (XMLStreamException ex) {
                logger.log(
                        Level.SEVERE, problemParsingRequestPolicyProp, ex);
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
                logger.log(
                        Level.SEVERE, createProviderConfigPropertyFailed, ex);
            } catch (XMLStreamException ex) {
                logger.log(
                        Level.SEVERE, problemParsingProviderConfigProp, ex);
            }
        }
    }

    /*
     * Creates the diagnostic-service object using data from glassfish4\glassfish\lib\templates\domain.xml
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
     * Creates the java-config object using data from glassfish4\glassfish\lib\templates\domain.xml
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
                logger.log(
                        Level.SEVERE,failureCreatingJavaConfigObject , ex);
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
                logger.log(
                        Level.SEVERE, problemParsingJvmOptions, ex);
            }
        }
    }

    /*
     * Creates the availability-service object using data from glassfish4\glassfish\lib\templates\domain.xml
     * <availability-service>
     *  <web-container-availability availability-enabled="true" persistence-frequency="web-method" persistence-scope="session" persistence-type="replicated" sso-failover-enabled="false"/>
     *  <ejb-container-availability availability-enabled="true" sfsb-store-pool-name="jdbc/hastore"/>
     *  <jms-availability availability-enabled="false"/>
     * </availability-service>
     */
    static private class AvailabilityServiceConfigCode implements SingleConfigCode<Config> {

        public Object run(Config config) throws PropertyVetoException {
            try {
                AvailabilityService as = config.createChild(AvailabilityService.class);
                config.setAvailabilityService(as);
            } catch (TransactionFailure ex) {
                logger.log(
                        Level.SEVERE, failureCreatingAvailabilityServiceConfig, ex);
            }

            return null;
        }
    }

    /*
     * Creates the network-config object using data from glassfish4\glassfish\lib\templates\domain.xml
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
                logger.log(
                        Level.SEVERE,failureCreatingNetworkConfig , ex);
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
                logger.log(
                        Level.SEVERE,failureCreatingProtocolsConfig, ex);
            } catch (XMLStreamException ex) {
                logger.log(
                        Level.SEVERE, problemParsingProtocolsConfig, ex);
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
                        while (true) {
                            if (parser.next() == START_ELEMENT) {
                                if (parser.getLocalName().equals("http") && p != null) {
                        createHttp(p);
                        createSsl(p);
                                    break;
                                } else if (parser.getLocalName().equals("http-redirect") && p != null) {
                        createHttpRedirect(p);
                                    break;
                                } else if (parser.getLocalName().equals("port-unification") && p != null) {
                        createPortUnification(p);
                                    break;
                                } else {
                                    break;
                    }
                }
                        }

                    }
                }
            } catch (TransactionFailure ex) {
                logger.log(
                        Level.SEVERE, failureCreatingProtocolConfig, ex);
            } catch (XMLStreamException ex) {
                logger.log(
                        Level.SEVERE,problemParsingProtocolElement , ex);
            }
        }
    }

    /* <http default-virtual-server="__asadmin" max-connections="250"> (1 example with most attributes)*/
    private void createHttp(Protocol p) throws PropertyVetoException {
            try {
                    if (parser.getLocalName().equals("http") && p != null) {
                        Http h = p.createChild(Http.class);
                        p.setHttp(h);
                        for (int i = 0; i < parser.getAttributeCount(); i++) {
                            String attr = parser.getAttributeLocalName(i);
                            String val = parser.getAttributeValue(i);
                            if (attr.equals("default-virtual-server")) {
                                h.setDefaultVirtualServer(val);
                            }
                    if (attr.equals("encoded-slash-enabled")) {
                        h.setEncodedSlashEnabled(val);
                    }
                            if (attr.equals("max-connections")) {
                                h.setMaxConnections(val);
                            }
                        }
                        createFileCache(h);
                    }
            } catch (TransactionFailure ex) {
                logger.log(
                        Level.SEVERE, failureCreatingHttpConfig, ex);
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
                logger.log(
                        Level.SEVERE, failureCreatingFileCacheConfig, ex);
            } catch (XMLStreamException ex) {
                logger.log(
                        Level.SEVERE, problemParsingFileCacheElement, ex);
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
                            if (attr.equals("client-auth")) {
                                ssl.setClientAuth(val);
                        }
                        }
                        break;
                    }
                }
            } catch (TransactionFailure ex) {
                logger.log(
                        Level.SEVERE, failureCreatingSSLConfig, ex);
            } catch (XMLStreamException ex) {
                logger.log(
                        Level.SEVERE,problemParsingSSlElement, ex);
            }
        }
    }
    
    /* <http-redirect secure="true"></http-redirect> */
    private void createHttpRedirect(Protocol p) throws PropertyVetoException {
            try {
                        HttpRedirect hr = p.createChild(HttpRedirect.class);
                        p.setHttpRedirect(hr);
                        for (int i = 0; i < parser.getAttributeCount(); i++) {
                            String attr = parser.getAttributeLocalName(i);
                            String val = parser.getAttributeValue(i);
                            if (attr.equals("secure")) {
                                hr.setSecure(val);
                            }
                        }
            } catch (TransactionFailure ex) {
                logger.log(
                        Level.SEVERE, failureCreatingHttpRedirect, ex);
            }
        }
    
    /* <port-unification>
     *       <protocol-finder protocol="sec-admin-listener" name="http-finder" classname="org.glassfish.grizzly.config.portunif.HttpProtocolFinder"></protocol-finder>
     *       <protocol-finder protocol="admin-http-redirect" name="admin-http-redirect" classname="org.glassfish.grizzly.config.portunif.HttpProtocolFinder"></protocol-finder>
     * </port-unification> */
    private void createPortUnification(Protocol p) throws PropertyVetoException {
            try {
                        PortUnification pu = p.createChild(PortUnification.class);
                        p.setPortUnification(pu);
                        
                        createProtocolFinder(pu);
            } catch (TransactionFailure ex) {
                logger.log(
                        Level.SEVERE, failureCreatingPortUnification, ex);
            }
        }
    
    private void createProtocolFinder(PortUnification pu) throws PropertyVetoException {
        while (!(parser.getEventType() == END_ELEMENT && parser.getLocalName().equals("port-unification"))) {
            try {
                if (parser.next() == START_ELEMENT) {
                    if (parser.getLocalName().equals("protocol-finder") && pu != null) {
                        ProtocolFinder pf = pu.createChild(ProtocolFinder.class);
                        List<ProtocolFinder> pfList = pu.getProtocolFinder();
                        pfList.add(pf);
                        for (int i = 0; i < parser.getAttributeCount(); i++) {
                            String attr = parser.getAttributeLocalName(i);
                            String val = parser.getAttributeValue(i);
                            if (attr.equals("protocol")) {
                                pf.setProtocol(val);
                            }
                            if (attr.equals("name")) {
                                pf.setName(val);
                            }
                            if (attr.equals("classname")) {
                                pf.setClassname(val);
                            }
                        }
                    }
                }
            } catch (TransactionFailure ex) {
                logger.log(
                        Level.SEVERE, failureCreatingProtocolFinder, ex);
            } catch (XMLStreamException ex) {
                logger.log(
                        Level.SEVERE, problemParsingProtocolFinder, ex);
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
                logger.log(
                        Level.SEVERE,failureCreatingNetworkListeners , ex);
            } catch (XMLStreamException ex) {
                logger.log(
                        Level.SEVERE,problemParsingNetworkListeners , ex);
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
                logger.log(
                        Level.SEVERE,failureCreatingNetworkListener, ex);
            } catch (XMLStreamException ex) {
                logger.log(
                        Level.SEVERE, ProblemParsingNetworkListener, ex);
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
                logger.log(
                        Level.SEVERE, failureCreatingTransportsConfig, ex);
            } catch (XMLStreamException ex) {
                logger.log(
                        Level.SEVERE,failureParsingTransportsConfig , ex);
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
                logger.log(
                        Level.SEVERE,failureCreatingTransportConfig , ex);
            } catch (XMLStreamException ex) {
                logger.log(
                        Level.SEVERE, problemParsingTransportConfig, ex);
            }
        }
    }

    /*
     * Creates the thread-pools object using data from glassfish4\glassfish\lib\templates\domain.xml
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
                logger.log(
                        Level.SEVERE,failureToCreateThreadPoolsObject , ex);
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
                logger.log(
                        Level.SEVERE, failureToCreateThreadpoolObject, ex);
            } catch (XMLStreamException ex) {
                logger.log(
                        Level.SEVERE,problemParsingThreadPoolElement , ex);
            }
        }
    }

    /*
     * Creates the system-property elements using data from glassfish4\glassfish\lib\templates\domain.xml
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
                logger.log(
                        Level.SEVERE,failureCreatingSystemProperty , ex);
            } catch (XMLStreamException ex) {
                logger.log(
                        Level.SEVERE,problemParsingSystemProperty, ex);
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

    private void createParser(InputStream template) throws FileNotFoundException, XMLStreamException {
        if (template != null) {
            reader = new InputStreamReader(template);
            parser = XMLInputFactory.newInstance().createXMLStreamReader(
                    "domain.xml", reader);
        }
    }
    private XMLStreamReader parser;
    private InputStreamReader reader;
}
