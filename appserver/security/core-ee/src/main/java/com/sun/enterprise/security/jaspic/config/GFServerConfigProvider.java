/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
// Portions Copyright [2018-2021] [Payara Foundation and/or its affiliates]
package com.sun.enterprise.security.jaspic.config;

import static com.sun.enterprise.security.jaspic.AuthMessagePolicy.getHttpServletPolicies;
import static com.sun.enterprise.security.jaspic.AuthMessagePolicy.getMessageSecurityBinding;
import static com.sun.enterprise.security.jaspic.AuthMessagePolicy.getProviderID;
import static com.sun.enterprise.security.jaspic.AuthMessagePolicy.getSOAPPolicies;
import static com.sun.enterprise.security.jaspic.AuthMessagePolicy.getSunWebApp;
import static com.sun.enterprise.security.jaspic.AuthMessagePolicy.oneSOAPPolicy;
import static com.sun.enterprise.security.jaspic.config.HttpServletConstants.IS_MANDATORY;
import static java.security.AccessController.doPrivileged;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;

import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
// jsr 196 interface types
import jakarta.security.auth.message.AuthException;
import jakarta.security.auth.message.AuthStatus;
import jakarta.security.auth.message.MessageInfo;
import jakarta.security.auth.message.MessagePolicy;
import jakarta.security.auth.message.config.AuthConfig;
import jakarta.security.auth.message.config.AuthConfigFactory;
import jakarta.security.auth.message.config.AuthConfigProvider;
import jakarta.security.auth.message.config.ClientAuthConfig;
import jakarta.security.auth.message.config.ClientAuthContext;
import jakarta.security.auth.message.config.ServerAuthConfig;
import jakarta.security.auth.message.config.ServerAuthContext;
import jakarta.security.auth.message.module.ClientAuthModule;
import jakarta.security.auth.message.module.ServerAuthModule;

import org.glassfish.internal.api.Globals;

import com.sun.enterprise.deployment.runtime.common.MessageSecurityBindingDescriptor;
import com.sun.enterprise.deployment.runtime.web.SunWebApp;
import com.sun.enterprise.security.jaspic.AuthMessagePolicy;
import com.sun.enterprise.security.jaspic.WebServicesDelegate;
import com.sun.logging.LogDomains;

/**
 * This class implements the interface AuthConfigProvider.
 * 
 * @author Shing Wai Chan
 * @author Ronald Monzillo
 */
public class GFServerConfigProvider implements AuthConfigProvider {
    
    private static final Logger logger = LogDomains.getLogger(GFServerConfigProvider.class, LogDomains.SECURITY_LOGGER);

    public static final String SOAP = "SOAP";
    public static final String HTTPSERVLET = "HttpServlet";

    protected static final String CLIENT = "client";
    protected static final String SERVER = "server";
    protected static final String MANAGES_SESSIONS_OPTION = "managessessions";
    
    private static final String DEFAULT_PARSER_CLASS = "com.sun.enterprise.security.jaspic.config.ConfigDomainParser";

    // since old api does not have subject in PasswordValdiationCallback,
    // this is for old modules to pass group info back to subject
    private static final ThreadLocal<Subject> subjectLocal = new ThreadLocal<Subject>();

    protected static final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    protected static final Map<String, String> layerDefaultRegisIDMap = new HashMap<String, String>();

    // Mutable statics should be kept package private to eliminate
    // the ability for subclasses to access them
    static int epoch;
    static String parserClassName;
    static ConfigParser parser;
    static boolean parserInitialized;
    static AuthConfigFactory slaveFactory;

    // keep the slave from being visible outside
    static AuthConfigProvider slaveProvider;

    protected AuthConfigFactory factory;
    private WebServicesDelegate wsdelegate;

    public GFServerConfigProvider(Map properties, AuthConfigFactory factory) {
        this.factory = factory;
        initializeParser();

        if (factory != null) {
            boolean hasSlaveFactory = false;
            try {
                rwLock.readLock().lock();
                hasSlaveFactory = (slaveFactory != null);
            } finally {
                rwLock.readLock().unlock();
            }

            if (!hasSlaveFactory) {
                try {
                    rwLock.writeLock().lock();
                    if (slaveFactory == null) {
                        slaveFactory = factory;
                    }
                } finally {
                    rwLock.writeLock().unlock();
                }
            }
        }

        boolean hasSlaveProvider = false;
        try {
            rwLock.readLock().lock();
            hasSlaveProvider = slaveProvider != null;
        } finally {
            rwLock.readLock().unlock();
        }

        if (!hasSlaveProvider) {
            try {
                rwLock.writeLock().lock();
                if (slaveProvider == null) {
                    slaveProvider = this;
                }
            } finally {
                rwLock.writeLock().unlock();
            }
        }
        wsdelegate = Globals.get(WebServicesDelegate.class);
    }

    private void initializeParser() {
        try {
            rwLock.readLock().lock();
            if (parserInitialized) {
                return;
            }
        } finally {
            rwLock.readLock().unlock();
        }

        try {
            rwLock.writeLock().lock();
            if (!parserInitialized) {
                parserClassName = System.getProperty("config.parser", DEFAULT_PARSER_CLASS);
                loadParser(this, factory, null);
                parserInitialized = true;
            }
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    /**
     * Instantiate and initialize module class
     */
    static ModuleInfo createModuleInfo(Entry entry, CallbackHandler handler, String type, Map<String, Object> properties) throws AuthException {
        try {
            // Instantiate module using no-arg constructor
            Object newModule = entry.newInstance();

            Map<String, Object> map = properties;
            Map<String, Object> entryOptions = entry.getOptions();

            if (entryOptions != null) {
                if (map == null) {
                    map = new HashMap<>();
                } else {
                    map = new HashMap<>(map);
                }
                map.putAll(entryOptions);
            }

            // Initialize Module
            if (SERVER.equals(type)) {
                ServerAuthModule sam = (ServerAuthModule) newModule;
                sam.initialize(entry.getRequestPolicy(), entry.getResponsePolicy(), handler, map);
            } else { // CLIENT
                ClientAuthModule cam = (ClientAuthModule) newModule;
                cam.initialize(entry.getRequestPolicy(), entry.getResponsePolicy(), handler, map);
            }

            return new ModuleInfo(newModule, map);
        } catch (Exception e) {
            if (e instanceof AuthException) {
                throw (AuthException) e;
            }
            
            throw (AuthException) new AuthException().initCause(e);
        }
    }

    /**
     * Create an object of a given class.
     * 
     * @param className
     *
     */
    private static Object createObject(String className) {
        ClassLoader loader = getClassLoader();
        
        if (System.getSecurityManager() != null) {
            try {
                return doPrivileged((PrivilegedExceptionAction<Object>)
                    () -> Class.forName(className, true, loader).newInstance());
            } catch (PrivilegedActionException pae) {
                throw new RuntimeException(pae.getException());
            }
        }
        
        try {
            return Class.forName(className, true, loader).newInstance();
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    Entry getEntry(String intercept, String id, MessagePolicy requestPolicy, MessagePolicy responsePolicy, String type) {

        // get the parsed module config and DD information

        Map<String, InterceptEntry> configMap;

        try {
            rwLock.readLock().lock();
            configMap = parser.getConfigMap();
        } finally {
            rwLock.readLock().unlock();
        }

        if (configMap == null) {
            return null;
        }

        // get the module config info for this intercept

        InterceptEntry intEntry = configMap.get(intercept);
        if (intEntry == null || intEntry.idMap == null) {
            if (logger.isLoggable(FINE)) {
                logger.fine("module config has no IDs configured for [" + intercept + "]");
            }
            
            return null;
        }

        // look up the DD's provider ID in the module config

        IDEntry idEntry = null;
        if (id == null || (idEntry = (IDEntry) intEntry.idMap.get(id)) == null) {

            // either the DD did not specify a provider ID,
            // or the DD-specified provider ID was not found
            // in the module config.
            //
            // in either case, look for a default ID in the module config

            if (logger.isLoggable(FINE)) {
                logger.fine("DD did not specify ID, " + "or DD-specified ID for [" + intercept + "] not found in config -- "
                        + "attempting to look for default ID");
            }

            String defaultID;
            if (CLIENT.equals(type)) {
                defaultID = intEntry.defaultClientID;
            } else {
                defaultID = intEntry.defaultServerID;
            }

            idEntry = (IDEntry) intEntry.idMap.get(defaultID);
            if (idEntry == null) {

                // did not find a default provider ID

                if (logger.isLoggable(FINE)) {
                    logger.fine("no default config ID for [" + intercept + "]");
                }
                
                return null;
            }
        }

        // We found the DD provider ID in the module config
        // or we found a default module config

        // check provider-type
        if (idEntry.type.indexOf(type) < 0) {
            if (logger.isLoggable(FINE)) {
                logger.fine("request type [" + type + "] does not match config type [" + idEntry.type + "]");
            }
            
            return null;
        }

        // check whether a policy is set
        MessagePolicy reqP = requestPolicy != null || responsePolicy != null ? requestPolicy : idEntry.requestPolicy; // default;

        MessagePolicy respP = requestPolicy != null || responsePolicy != null ? responsePolicy : idEntry.responsePolicy; // default;

        // optimization: if policy was not set, return null
        if (reqP == null && respP == null) {
            if (logger.isLoggable(FINE)) {
                logger.fine("no policy applies");
            }
            return null;
        }

        // return the configured modules with the correct policies

        Entry entry = new Entry(idEntry.moduleClassName, reqP, respP, idEntry.options);

        if (logger.isLoggable(FINE)) {
            logger.fine("getEntry for: " + intercept + " -- " + id + "\n    module class: " + entry.moduleClassName + "\n    options: "
                    + entry.options + "\n    request policy: " + entry.requestPolicy + "\n    response policy: " + entry.responsePolicy);
        }

        return entry;
    }

    /**
     * Class representing a single AuthModule entry configured for an ID, interception point, and stack.
     *
     * <p>
     * This class also provides a way for a caller to obtain an instance of the module listed in the entry by invoking the
     * <code>newInstance</code> method.
     */
    static class Entry {

        // For loading modules
        private static final Class<?>[] PARAMS = {};
        private static final Object[] ARGS = {};

        private String moduleClassName;
        private MessagePolicy requestPolicy;
        private MessagePolicy responsePolicy;
        private Map<String, Object> options;

        /**
         * Construct a ConfigFile entry.
         *
         * <p>
         * An entry encapsulates a single module and its related information.
         *
         * @param moduleClassName
         *            the module class name
         * @param requestPolicy
         *            the request policy assigned to the module listed in this entry, which may be null.
         *
         * @param responsePolicy
         *            the response policy assigned to the module listed in this entry, which may be null.
         *
         * @param options
         *            the options configured for this module.
         */
        Entry(String moduleClassName, MessagePolicy requestPolicy, MessagePolicy responsePolicy, Map<String, Object> options) {
            this.moduleClassName = moduleClassName;
            this.requestPolicy = requestPolicy;
            this.responsePolicy = responsePolicy;
            this.options = options;
        }

        /**
         * Return the request policy assigned to this module.
         *
         * @return the policy, which may be null.
         */
        MessagePolicy getRequestPolicy() {
            return requestPolicy;
        }

        /**
         * Return the response policy assigned to this module.
         *
         * @return the policy, which may be null.
         */
        MessagePolicy getResponsePolicy() {
            return responsePolicy;
        }

        String getModuleClassName() {
            return moduleClassName;
        }

        Map<String, Object> getOptions() {
            return options;
        }

        /**
         * Return a new instance of the module contained in this entry.
         *
         * <p>
         * The default implementation of this method attempts to invoke the default no-args constructor of the module class.
         * This method may be overridden if a different constructor should be invoked.
         *
         * @return a new instance of the module contained in this entry.
         *
         * @exception AuthException
         *                if the instantiation failed.
         */
        Object newInstance() throws AuthException {
            try {
                return Class.forName(moduleClassName, true, getClassLoader())
                            .getConstructor(PARAMS)
                            .newInstance(ARGS);
            } catch (Exception e) {
                if (logger.isLoggable(WARNING)) {
                    logger.log(WARNING, "jaspic.provider_unable_to_load_authmodule", new String[] { moduleClassName, e.toString() });
                }

                AuthException ae = new AuthException();
                ae.initCause(e);
                throw ae;
            }
        }
    }

    public static class InterceptEntry {
        String defaultClientID;
        String defaultServerID;
        Map<String, GFServerConfigProvider.IDEntry> idMap;

        public InterceptEntry(String defaultClientID, String defaultServerID, Map<String, GFServerConfigProvider.IDEntry> idMap) {
            this.defaultClientID = defaultClientID;
            this.defaultServerID = defaultServerID;
            this.idMap = idMap;
        }

        public Map<String, GFServerConfigProvider.IDEntry> getIdMap() {
            return idMap;
        }

        public void setIdMap(Map<String, GFServerConfigProvider.IDEntry> map) {
            idMap = map;
        }

        public String getDefaultClientID() {
            return defaultClientID;
        }

        public String getDefaultServerID() {
            return defaultServerID;
        }
    }

    /**
     * Get an instance of ClientAuthConfig from this provider.
     *
     * <p>
     * The implementation of this method returns a ClientAuthConfig instance that describes the configuration of
     * ClientAuthModules at a given message layer, and for use in an identified application context.
     *
     * @param layer
     *            a String identifying the message layer for the returned ClientAuthConfig object. This argument must not be
     *            null.
     *
     * @param appContext
     *            a String that identifies the messaging context for the returned ClientAuthConfig object. This argument
     *            must not be null.
     *
     * @param handler
     *            a CallbackHandler to be passed to the ClientAuthModules encapsulated by ClientAuthContext objects derived
     *            from the returned ClientAuthConfig. This argument may be null, in which case the implementation may assign
     *            a default handler to the configuration.
     *
     * @return a ClientAuthConfig Object that describes the configuration of ClientAuthModules at the message layer and
     *         messaging context identified by the layer and appContext arguments. This method does not return null.
     *
     * @exception AuthException
     *                if this provider does not support the assignment of a default CallbackHandler to the returned
     *                ClientAuthConfig.
     *
     * @exception SecurityException
     *                if the caller does not have permission to retrieve the configuration.
     *
     *                The CallbackHandler assigned to the configuration must support the Callback objects required to be
     *                supported by the profile of this specification being followed by the messaging runtime. The
     *                CallbackHandler instance must be initialized with any application context needed to process the
     *                required callbacks on behalf of the corresponding application.
     */
    public ClientAuthConfig getClientAuthConfig(String layer, String appContext, CallbackHandler handler) throws AuthException {
        return new GFClientAuthConfig(this, layer, appContext, handler);
    }

    /**
     * Get an instance of ServerAuthConfig from this provider.
     *
     * <p>
     * The implementation of this method returns a ServerAuthConfig instance that describes the configuration of
     * ServerAuthModules at a given message layer, and for a particular application context.
     *
     * @param layer
     *            a String identifying the message layer for the returned ServerAuthConfig object. This argument must not be
     *            null.
     *
     * @param appContext
     *            a String that identifies the messaging context for the returned ServerAuthConfig object. This argument
     *            must not be null.
     *
     * @param handler
     *            a CallbackHandler to be passed to the ServerAuthModules encapsulated by ServerAuthContext objects derived
     *            from thr returned ServerAuthConfig. This argument may be null, in which case the implementation may assign
     *            a default handler to the configuration.
     *
     * @return a ServerAuthConfig Object that describes the configuration of ServerAuthModules at a given message layer, and
     *         for a particular application context. This method does not return null.
     *
     * @exception AuthException
     *                if this provider does not support the assignment of a default CallbackHandler to the returned
     *                ServerAuthConfig.
     *
     * @exception SecurityException
     *                if the caller does not have permission to retrieve the configuration.
     *                <p>
     *                The CallbackHandler assigned to the configuration must support the Callback objects required to be
     *                supported by the profile of this specification being followed by the messaging runtime. The
     *                CallbackHandler instance must be initialized with any application context needed to process the
     *                required callbacks on behalf of the corresponding application.
     */
    public ServerAuthConfig getServerAuthConfig(String layer, String appContext, CallbackHandler handler) throws AuthException {
        return new GFServerAuthConfig(this, layer, appContext, handler);
    }

    /**
     * Causes a dynamic configuration provider to update its internal state such that any resulting change to its state is
     * reflected in the corresponding authentication context configuration objects previously created by the provider within
     * the current process context.
     *
     * @exception AuthException
     *                if an error occured during the refresh.
     *
     * @exception SecurityException
     *                if the caller does not have permission to refresh the provider.
     */

    public void refresh() {
        loadParser(this, factory, null);
    }

    /**
     * this method is intended to be called by the admin configuration system when the corresponding config object has
     * changed. It relies on the slaves, since it is a static method.
     * 
     * @param config
     *            a config object of type understood by the parser. NOTE: there appears to be a thread saftey problem, and
     *            this method will fail if a slaveProvider has not been established prior to its call.
     */
    public static void loadConfigContext(Object config) {

        boolean hasSlaveFactory = false;
        rwLock.readLock().lock();
        try {
            hasSlaveFactory = (slaveFactory != null);
        } finally {
            rwLock.readLock().unlock();
        }

        if (slaveProvider == null) {
            if (logger.isLoggable(SEVERE)) {
                logger.severe("unableToLoad.noSlaveProvider");
            }
            return;
        }

        if (!hasSlaveFactory) {
            rwLock.writeLock().lock();
            try {
                if (slaveFactory == null) {
                    slaveFactory = AuthConfigFactory.getFactory();
                }
            } finally {
                rwLock.writeLock().unlock();
            }
        }

        loadParser(slaveProvider, slaveFactory, config);
    }

    protected static void loadParser(AuthConfigProvider aProvider, AuthConfigFactory aFactory, Object config) {
        rwLock.writeLock().lock();
        try {
            ConfigParser nextParser;
            int next = epoch + 1;
            nextParser = (ConfigParser) createObject(parserClassName);
            nextParser.initialize(config);

            if (aFactory != null && aProvider != null) {
                Set<String> layerSet = nextParser.getLayersWithDefault();
                for (String layer : layerDefaultRegisIDMap.keySet()) {
                    if (!layerSet.contains(layer)) {
                        String regisID = layerDefaultRegisIDMap.remove(layer);
                        aFactory.removeRegistration(regisID);
                    }
                }

                for (String layer : layerSet) {
                    if (!layerDefaultRegisIDMap.containsKey(layer)) {
                        String regisID = aFactory.registerConfigProvider(aProvider, layer, null,
                                "GFServerConfigProvider: self registration");
                        layerDefaultRegisIDMap.put(layer, regisID);
                    }
                }
            }
            epoch = (next == 0 ? 1 : next);
            parser = nextParser;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    protected static ClassLoader getClassLoader() {
        if (System.getSecurityManager() == null) {
            return Thread.currentThread().getContextClassLoader();
        }

        return (ClassLoader) AccessController.doPrivileged(new PrivilegedAction<Object>() {
            public Object run() {
                return Thread.currentThread().getContextClassLoader();
            }
        });
    }

    // for old API
    public static void setValidateRequestSubject(Subject subject) {
        subjectLocal.set(subject);
    }

    class GFAuthConfig implements AuthConfig {
        
        protected AuthConfigProvider provider;
        protected String layer;
        protected String appContext;
        protected CallbackHandler handler;
        protected String type;
        protected String providerID;
        protected boolean init;
        protected boolean onePolicy;
        protected MessageSecurityBindingDescriptor binding;
        protected SunWebApp sunWebApp;

        protected GFAuthConfig(AuthConfigProvider provider, String layer, String appContext, CallbackHandler handler, String type) {
            this.provider = provider;
            this.layer = layer;
            this.appContext = appContext;
            this.handler = handler != null ? handler : AuthMessagePolicy.getDefaultCallbackHandler();
            this.type = type;
        }

        /**
         * Get the message layer name of this authentication context configuration object.
         *
         * @return the message layer name of this configuration object, or null if the configuration object pertains to an
         *         unspecified message layer.
         */
        public String getMessageLayer() {
            return layer;
        }

        /**
         * Get the application context identifier of this authentication context configuration object.
         *
         * @return the String identifying the application context of this configuration object or null if the configuration
         *         object pertains to an unspecified application context.
         */
        public String getAppContext() {
            return appContext;
        }

        /**
         * Get the authentication context identifier corresponding to the request and response objects encapsulated in
         * messageInfo.
         * 
         * See method AuthMessagePolicy. getHttpServletPolicies() for more details on why this method returns the String's
         * "true" or "false" for AuthContextID.
         *
         * @param messageInfo
         *            a contextual Object that encapsulates the client request and server response objects.
         *
         * @return the authentication context identifier corresponding to the encapsulated request and response objects, or
         *         null.
         * 
         *
         * @throws IllegalArgumentException
         *             if the type of the message objects incorporated in messageInfo are not compatible with the message types
         *             supported by this authentication context configuration object.
         */
        public String getAuthContextID(MessageInfo messageInfo) {
            if (HTTPSERVLET.equals(layer)) {
                return Boolean.valueOf((String) messageInfo.getMap().get(IS_MANDATORY)).toString();
            }
            
            if (SOAP.equals(layer) && wsdelegate != null) {
                return wsdelegate.getAuthContextID(messageInfo);
            }
            
            return null;
        }

        // we should be able to replace the following with a method on packet

        /**
         * Causes a dynamic anthentication context configuration object to update the internal state that it uses to process
         * calls to its <code>getAuthContext</code> method.
         *
         * @exception AuthException
         *                if an error occured during the update.
         *
         * @exception SecurityException
         *                if the caller does not have permission to refresh the configuration object.
         */
        public void refresh() {
            loadParser(provider, factory, null);
        }

        /**
         * Used to determine whether or not the <code>getAuthContext</code> method of the authentication context configuration
         * will return null for all possible values of authentication context identifier.
         *
         * @return false when <code>getAuthContext</code> will return null for all possible values of authentication context
         *         identifier. Otherwise, this method returns true.
         */
        public boolean isProtected() {
            // XXX TBD
            return true;
        }
        

        CallbackHandler getCallbackHandler() {
            return handler;
        }

        protected ModuleInfo getModuleInfo(String authContextID, Map<String, Object> properties) throws AuthException {
            if (!init) {
                initialize(properties);
            }

            MessagePolicy[] policies = null;

            if (HTTPSERVLET.equals(layer)) {
                policies = getHttpServletPolicies(authContextID);
            } else {
                policies = getSOAPPolicies(binding, authContextID, onePolicy);
            }

            MessagePolicy requestPolicy = policies[0];
            MessagePolicy responsePolicy = policies[1];

            Entry entry = getEntry(layer, providerID, requestPolicy, responsePolicy, type);

            return entry != null ? createModuleInfo(entry, handler, type, properties) : null;
        }

        // Lazy initialize this as SunWebApp is not available in RealmAdapter creation
        private void initialize(Map<String, ?> properties) {
            if (!init) {
                if (HTTPSERVLET.equals(layer)) {
                    sunWebApp = getSunWebApp(properties);
                    providerID = getProviderID(sunWebApp);
                    onePolicy = true;
                } else {
                    binding = getMessageSecurityBinding(layer, properties);
                    providerID = getProviderID(binding);
                    onePolicy = oneSOAPPolicy(binding);
                }

                // HandlerContext need to be explicitly set by caller
                init = true;
            }
        }
    }

    class GFServerAuthConfig extends GFAuthConfig implements ServerAuthConfig {

        protected GFServerAuthConfig(AuthConfigProvider provider, String layer, String appContext, CallbackHandler handler) {
            super(provider, layer, appContext, handler, SERVER);
        }

        public ServerAuthContext getAuthContext(String authContextID, Subject serviceSubject, @SuppressWarnings("rawtypes") Map properties) throws AuthException {
            @SuppressWarnings("unchecked")
            ModuleInfo moduleInfo = getModuleInfo(authContextID, (Map<String, Object>) properties);

            if (moduleInfo != null && moduleInfo.getModule() != null) {
                return new GFServerAuthContext(moduleInfo.getModule());
            }

            return null;
        }
    }

    class GFClientAuthConfig extends GFAuthConfig implements ClientAuthConfig {

        protected GFClientAuthConfig(AuthConfigProvider provider, String layer, String appContext, CallbackHandler handler) {
            super(provider, layer, appContext, handler, CLIENT);
        }

        public ClientAuthContext getAuthContext(String authContextID, Subject clientSubject, @SuppressWarnings("rawtypes") Map properties) throws AuthException {
            @SuppressWarnings("unchecked")
            ModuleInfo moduleInfo = getModuleInfo(authContextID, (Map<String, Object>) properties);

            if (moduleInfo != null && moduleInfo.getModule() != null) {
                return new GFClientAuthContext(moduleInfo.getModule());
            }

            return null;
        }
    }

    static protected class GFServerAuthContext implements ServerAuthContext {

        private final ServerAuthModule module;

        GFServerAuthContext(ServerAuthModule module) {
            this.module = module;
        }

        public AuthStatus validateRequest(MessageInfo messageInfo, Subject clientSubject, Subject serviceSubject) throws AuthException {
            if (module == null) {
                throw new AuthException();
            }
            
            return module.validateRequest(messageInfo, clientSubject, serviceSubject);
        }

        public AuthStatus secureResponse(MessageInfo messageInfo, Subject serviceSubject) throws AuthException {
            if (module == null) {
                throw new AuthException();
            }
            
            return module.secureResponse(messageInfo, serviceSubject);
        }

        public void cleanSubject(MessageInfo messageInfo, Subject subject) throws AuthException {
            if (module == null) {
                throw new AuthException();
            }
            
            module.cleanSubject(messageInfo, subject);
        }
    }

    static protected class GFClientAuthContext implements ClientAuthContext {

        private final ClientAuthModule module;

        GFClientAuthContext(ClientAuthModule module) {
            this.module = module;
        }

        public AuthStatus secureRequest(MessageInfo messageInfo, Subject clientSubject) throws AuthException {
            if (module == null) {
                throw new AuthException();
            }
            
            return module.secureRequest(messageInfo, clientSubject);
        }

        public AuthStatus validateResponse(MessageInfo messageInfo, Subject clientSubject, Subject serviceSubject) throws AuthException {
            if (module == null) {
                throw new AuthException();
            }
            
            return module.validateResponse(messageInfo, clientSubject, serviceSubject);
        }

        public void cleanSubject(MessageInfo messageInfo, Subject subject) throws AuthException {
            if (module == null) {
                throw new AuthException();
            }
            
            module.cleanSubject(messageInfo, subject);
        }
    }
    
    /**
     * parsed ID entry
     */
    public static class IDEntry {
        private String type; // provider type (client, server, client-server)
        private String moduleClassName;
        private MessagePolicy requestPolicy;
        private MessagePolicy responsePolicy;
        private Map<String, Object> options;
        
        public IDEntry(String type, String moduleClassName, MessagePolicy requestPolicy, MessagePolicy responsePolicy, Map<String, Object> options) {
            this.type = type;
            this.moduleClassName = moduleClassName;
            this.requestPolicy = requestPolicy;
            this.responsePolicy = responsePolicy;
            this.options = options;
        }

        public String getModuleClassName() {
            return moduleClassName;
        }

        public Map<String, Object> getOptions() {
            return options;
        }

        public MessagePolicy getRequestPolicy() {
            return requestPolicy;
        }

        public MessagePolicy getResponsePolicy() {
            return responsePolicy;
        }

        public String getType() {
            return type;
        }
    }

    /**
     * A data object contains module object and the corresponding map.
     */
    protected static class ModuleInfo {
        
        private final Object module;
        private final Map<String, Object> map;

        ModuleInfo(Object module, Map<String, Object> map) {
            this.module = module;
            this.map = map;
        }

        @SuppressWarnings("unchecked")
        <T> T getModule() {
            return (T) module;
        }

        Map<String, Object> getMap() {
            return map;
        }
    }
}
