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

package org.glassfish.appclient.client.acc;

import com.sun.enterprise.container.common.spi.util.InjectionException;
import com.sun.enterprise.module.bootstrap.BootException;
import com.sun.enterprise.util.LocalStringManager;
import com.sun.enterprise.util.LocalStringManagerImpl;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.security.auth.callback.CallbackHandler;

import org.glassfish.api.naming.ClientNamingConfigurator;
import org.glassfish.appclient.client.acc.config.AuthRealm;
import org.glassfish.appclient.client.acc.config.ClientCredential;
import org.glassfish.appclient.client.acc.config.MessageSecurityConfig;
import org.glassfish.appclient.client.acc.config.Property;
import org.glassfish.appclient.client.acc.config.TargetServer;
import org.glassfish.appclient.client.acc.config.util.XML;
import org.glassfish.enterprise.iiop.api.GlassFishORBHelper;
import org.xml.sax.SAXParseException;

/**
 * Implements a builder for accumulating configuration information for the
 * app client container and then starting the ACC.
 * <p>
 * The interface for the  ACC builder is defined as AppClientContainer.Builder so the
 * relevant JavaDoc is concentrated in that one class.
 *<p>
 * The AppClientContainerBuilder class records the
 * information the container itself needs in order to operate.
 *
 * @author tjquinn
 */
public class AppClientContainerBuilder implements AppClientContainer.Builder {

    private final static String ENDPOINTS_PROPERTY_NAME = "com.sun.appserv.iiop.endpoints";

    private static final LocalStringManager localStrings = new LocalStringManagerImpl(AppClientContainerBuilder.class);
    /** caller-specified target servers */
    private TargetServer[] targetServers;

    /** caller-optional logger  - initialized to logger name from the class; caller can override with logger method */
    private Logger logger = Logger.getLogger(getClass().getName());

    private AuthRealm authRealm = null;

    private URLClassLoader classLoader = (URLClassLoader) Thread.currentThread().getContextClassLoader();

    /**
     * The caller can pre-set the client credentials using the
     * <code>clientCredentials</code> method.  The ACC will use the
     * username and realm values in intializing a callback handler if one is
     * needed.
     */
    private ClientCredential clientCredential = null;

    private boolean sendPassword = true;

    private GlassFishORBHelper orbHelper;

    /** caller-provided message security configurations */
    private final List<MessageSecurityConfig> messageSecurityConfigs = new ArrayList<MessageSecurityConfig>();

//    private Class<? extends CallbackHandler> callbackHandlerClass = null;

    /**
     * optional caller-specified properties governing the ACC's behavior.
     * Correspond to the property elements available in the client-container
     * element from sun-application-client-containerxxx.dtd.
     */
    private Properties containerProperties = null;

    AppClientContainerBuilder() {

    }
    
    /**
     * Creates a new builder with the specified target servers and client URI.
     *
     * @param targetServers the <code>TargetServer</code>s to use
     * @param clientURI the URI of the client archive to launch
     */
    AppClientContainerBuilder(final TargetServer[] targetServers) {
        this.targetServers = targetServers;
    }

    public AppClientContainer newContainer(final Class mainClass,
            final CallbackHandler callerSpecifiedCallbackHandler) throws Exception {
        prepareHabitat();
        Launchable client = Launchable.LaunchableUtil.newLaunchable(
                ACCModulesManager.getHabitat(), mainClass);
        AppClientContainer container = createContainer(client, 
                callerSpecifiedCallbackHandler, false /* istextAuth */);
        return container;
    }
    
    public AppClientContainer newContainer(final Class mainClass) throws Exception {
        return newContainer(mainClass, null);

    }

    public AppClientContainer newContainer(final URI clientURI,
            final CallbackHandler callerSpecifiedCallbackHandler,
            final String callerSpecifiedMainClassName,
            final String callerSpecifiedAppClientName) throws Exception, UserError {
        return newContainer(clientURI, callerSpecifiedCallbackHandler,
                callerSpecifiedMainClassName,
                callerSpecifiedAppClientName,
                false /* isTextAuth */);
    }

    public AppClientContainer newContainer(final URI clientURI,
            final CallbackHandler callerSpecifiedCallbackHandler,
            final String callerSpecifiedMainClassName,
            final String callerSpecifiedAppClientName,
            final boolean isTextAuth) throws Exception, UserError {
        prepareHabitat();
        prepareIIOP(targetServers, containerProperties);
        Launchable client = Launchable.LaunchableUtil.newLaunchable(
                clientURI,
                callerSpecifiedMainClassName,
                callerSpecifiedAppClientName,
                ACCModulesManager.getHabitat());

        AppClientContainer container = createContainer(client, 
                callerSpecifiedCallbackHandler, isTextAuth);
        return container;
    }

    public AppClientContainer newContainer(final URI clientURI) throws Exception, UserError {
        return newContainer(clientURI, null, null, null);
    }

    private AppClientContainer createContainer(final Launchable client,
            final CallbackHandler callerSuppliedCallbackHandler,
            final boolean isTextAuth) throws BootException, BootException, URISyntaxException, ClassNotFoundException, InstantiationException, IllegalAccessException, InjectionException, IOException, SAXParseException {
        AppClientContainer container = ACCModulesManager.getService(AppClientContainer.class);
        //process the packaged permissions.xml
        container.processPermissions();
        container.setClient(client);
        container.setBuilder(this);
        CallbackHandler callbackHandler = 
                (callerSuppliedCallbackHandler != null ? 
                    callerSuppliedCallbackHandler : getCallbackHandlerFromDescriptor(client.getDescriptor(classLoader).getCallbackHandler()));
        container.prepareSecurity(targetServers, messageSecurityConfigs, containerProperties,
                clientCredential, callbackHandler, classLoader, isTextAuth);
        return container;
    }

    private CallbackHandler getCallbackHandlerFromDescriptor(final String callbackHandlerName) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        if (callbackHandlerName != null && ! callbackHandlerName.equals("")) {
            Class<CallbackHandler> callbackHandlerClass =
                    (Class<CallbackHandler>) Class.forName(callbackHandlerName, true, classLoader);
            return callbackHandlerClass.newInstance();
        }
        return null;
    }

    private void prepareHabitat() throws URISyntaxException {
        ACCModulesManager.initialize(Thread.currentThread().getContextClassLoader());
        orbHelper = ACCModulesManager.getService(GlassFishORBHelper.class);
    }

    /**
     * Prepares the client ORB to bootstrap into the server ORB(s) specified
     * by the TargetServer objects.
     * @param targetServers the TargetServer endpoints to which the client ORB can try to connect
     * @param containerProperties Properties, if specified, which might indicate that SSL is to be used
     * @return ORB-related properties to define host and port for bootstrapping
     */
    private void prepareIIOP(final TargetServer[] targetServers, Properties containerProperties) {
        if (targetServers.length == 0) {
            throw new IllegalArgumentException();
        }

        final StringBuilder sb = new StringBuilder();
        for (TargetServer ts : targetServers) {
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append(ts.getAddress()).append(":").append(ts.getPort());
        }

        /*
         * If the user has explicitly defined the ORB-related properties, do
         * not override those settings.
         */
        if (targetServers.length == 1) {
            defineIfNotDefined(GlassFishORBHelper.OMG_ORB_INIT_HOST_PROPERTY,
                    targetServers[0].getAddress());
            defineIfNotDefined(GlassFishORBHelper.OMG_ORB_INIT_PORT_PROPERTY,
                    Integer.toString(targetServers[0].getPort()));
        } else {
            /*
             * Currently, set a system property to specify multiple endpoints.
             */
            defineIfNotDefined(ENDPOINTS_PROPERTY_NAME, sb.toString());
        }

        if (isSSLRequired(targetServers, containerProperties)) {
            orbHelper.setCSIv2Prop(GlassFishORBHelper.ORB_SSL_CLIENT_REQUIRED, "true");
        }
        logger.log(Level.CONFIG, "Using endpoint address(es): {0}", sb.toString());

    }

    /**
     * Define the specified system property using the new value unless the
     * property is already set.
     * @param propName name of the property to check and, possibly, set
     * @param newPropValue value to set if the property is not already set
     */
    private void defineIfNotDefined(final String propName, final String newPropValue) {
        if (System.getProperty(propName) == null) {
            if (newPropValue == null) {
                throw new RuntimeException(localStrings.getLocalString(
                        AppClientContainerBuilder.class,
                        "appclient.missingValue", 
                        "Value for {0} expected but was not configured or assigned",
                        new Object[] {propName}
                        ));
                        
            }
            System.setProperty(propName, newPropValue);
        }
    }

    /**
     * Reports whether the ORB should be requested to use SSL.
     * <p>
     * If any TargetServer specifies SSL or the container-level properties
     * specify SSL then report "true."
     * @param targetServers configured TargetServer(s)
     * @param containerProperties configured container-level properties
     * @return whether the target servers or the properties implies the use of SSL
     */
    private boolean isSSLRequired(final TargetServer[] targetServers, final Properties containerProperties) {
        if (containerProperties != null) {
            String sslPropertyValue = containerProperties.getProperty("ssl");
            if ("required".equals(sslPropertyValue)) {
                return true;
            }
        }
        for (TargetServer ts : targetServers) {
            /*
             * If this target server has the optional security sub-item then
             * the security sub-item must have an ssl sub-item.  So we can just
             * look for the security sub-item.
             */
            if (ts.getSecurity() != null) {
                return true;
            }
        }
        return false;
    }


//    /**
//     * Returns an AppClientContainer prepared to execute the app client implied
//     * by the launch info and the app client args.
//     * @param launchInfo info about the launch (type, name)
//     * @param appclientArgs appclient command line arguments
//     * @return
//     */
//    AppClientContainer newContainer(final CommandLaunchInfo launchInfo, final AppclientCommandArguments appclientArgs) {
//        AppClientContainer acc = null;
////        switch (launchInfo.getClientLaunchType()) {
////            case JAR:
////                /*
////                 * The user will have used local file path syntax, so create a
////                 * file first and then get its URI.
////                 */
////                File f = new File(launchInfo.getClientName());
////                acc = newContainer(f.toURI());
////
////                StandAloneAppClientInfo acInfo = new StandAloneAppClientInfo(
////                        false /* isJWS */,
////                        logger,
////                        f, archivist, mainClassFromCommandLine)
////        }
//        return acc;
//    }

    public AppClientContainerBuilder addMessageSecurityConfig(final MessageSecurityConfig msConfig) {
        messageSecurityConfigs.add(msConfig);
        return this;
    }

    public List<MessageSecurityConfig> getMessageSecurityConfig() {
        return this.messageSecurityConfigs;
    }

    public AppClientContainerBuilder logger(final Logger logger) {
        this.logger = logger;
        return this;
    }

    public Logger getLogger() {
        return logger;
    }


    public AppClientContainerBuilder authRealm(final String className) {
        authRealm = new AuthRealm(className);
        return this;
    }

    public AuthRealm getAuthRealm() {
        return authRealm;
    }

    public AppClientContainerBuilder clientCredentials(final String user, final char[] password) {
        return clientCredentials(user, password, null);
    }

    public AppClientContainerBuilder clientCredentials(final String user, final char[] password, final String realm) {
//        this.clientCredential = new ClientCredential()
//        this.user = user;
//        this.password = password;
//        this.realmName = realm;
        ClientCredential cc = new ClientCredential(user, new XML.Password(password), realm);
        return clientCredentials(cc);
    }

    public AppClientContainerBuilder clientCredentials(final ClientCredential cc) {
        clientCredential = cc;
        return this;
    }

    public ClientCredential getClientCredential() {
        return clientCredential;
    }

    public AppClientContainerBuilder containerProperties(final Properties props) {
        this.containerProperties = props;
        return this;
    }

    public AppClientContainerBuilder containerProperties(final List<Property> props) {
        containerProperties = XML.toProperties(props);
        return this;
    }

    public Properties getContainerProperties() {
        return containerProperties;
    }

    public AppClientContainerBuilder sendPassword(final boolean sendPassword){
        this.sendPassword = sendPassword;
        return this;
    }

    public boolean getSendPassword() {
        return sendPassword;
    }

//    public AppClientContainerBuilder callbackHandler(final Class<? extends CallbackHandler> callbackHandlerClass) {
//        this.callbackHandlerClass = callbackHandlerClass;
//        return this;
//    }

//    public Class<? extends CallbackHandler> getCallbackHandler() {
//        return callbackHandlerClass;
//    }

    public TargetServer[] getTargetServers() {
        return targetServers;
    }

//    public AppClientContainerBuilder mainClass(Class mainClass) {
//        this.mainClass = mainClass;
//        return this;
//    }

//    public Class getMainClass() {
//        return mainClass;
//    }
//
//    public AppClientContainerBuilder mainClassName(String mainClassName) {
//        if (isMainClassFromCaller) {
//            throw new IllegalStateException();
//        }
//        this.mainClassName = mainClassName;
//        return this;
//    }
//
//    public String getMainClassName() {
//        return mainClassName;
//    }
//
//    public AppClientContainerBuilder mainClass(final Class mainClass) {
//        this.mainClass = mainClass;
//        mainClassName = mainClass.getName();
//        return this;
//    }
//
//    public Method getMainMethod() {
//        return mainMethod;
//    }
//
//    private void completeConfig() throws NoSuchMethodException {
//        mainMethod = initMainMethod();
//    }
//

}
