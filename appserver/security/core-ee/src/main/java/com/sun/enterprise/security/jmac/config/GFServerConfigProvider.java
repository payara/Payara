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

package com.sun.enterprise.security.jmac.config;

import java.io.IOException;

import java.lang.reflect.Constructor;

import java.security.AccessController; 
import java.security.Principal; 
import java.security.PrivilegedAction; 
import java.security.PrivilegedActionException; 
import java.security.PrivilegedExceptionAction; 

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;

// jsr 196 interface types
import javax.security.auth.message.AuthException;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessageInfo;
import javax.security.auth.message.MessagePolicy;
import javax.security.auth.message.callback.CallerPrincipalCallback;
import javax.security.auth.message.config.AuthConfig;
import javax.security.auth.message.config.AuthConfigFactory;
import javax.security.auth.message.config.AuthConfigProvider;
import javax.security.auth.message.config.ClientAuthConfig;
import javax.security.auth.message.config.ClientAuthContext;
import javax.security.auth.message.config.ServerAuthConfig;
import javax.security.auth.message.config.ServerAuthContext;
import javax.security.auth.message.module.ClientAuthModule;
import javax.security.auth.message.module.ServerAuthModule;

import com.sun.enterprise.deployment.runtime.common.MessageSecurityBindingDescriptor;
import com.sun.enterprise.deployment.runtime.web.SunWebApp;

// types to support backward compatability of pre-standard 196 auth modules

import com.sun.enterprise.security.jauth.AuthParam;
import com.sun.enterprise.security.jauth.AuthPolicy;
import com.sun.enterprise.security.jauth.FailureException;
import com.sun.enterprise.security.jauth.HttpServletAuthParam;
import com.sun.enterprise.security.jauth.PendingException;
import com.sun.enterprise.security.jmac.AuthMessagePolicy;

import com.sun.enterprise.security.jmac.WebServicesDelegate;
import com.sun.logging.LogDomains;
import org.glassfish.internal.api.Globals;

/**
 * This class implements the interface AuthConfigProvider.
 * @author  Shing Wai Chan
 * @author  Ronald Monzillo
 */
public class GFServerConfigProvider implements AuthConfigProvider {

    public static final String SOAP = "SOAP";
    public static final String HTTPSERVLET = "HttpServlet";

    protected static final String CLIENT = "client";
    protected static final String SERVER = "server";
    protected static final String MANAGES_SESSIONS_OPTION = "managessessions";

    private static Logger logger = 
        LogDomains.getLogger(GFServerConfigProvider.class, LogDomains.SECURITY_LOGGER);
    private static final String DEFAULT_HANDLER_CLASS =
        "com.sun.enterprise.security.jmac.callback.ContainerCallbackHandler";
    private static final String DEFAULT_PARSER_CLASS =
        "com.sun.enterprise.security.jmac.config.ConfigDomainParser";

    // since old api does not have subject in PasswordValdiationCallback,
    // this is for old modules to pass group info back to subject
    private static final ThreadLocal<Subject> subjectLocal = 
        new ThreadLocal<Subject>();

    protected static final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    protected static final Map<String, String> layerDefaultRegisIDMap = 
        new HashMap<String, String>();
    
    // mutable statics should be kept package private to eliminate
    // the ability for subclasses to access them
    static int epoch; 
    static String parserClassName = null;
    static ConfigParser parser;
    static boolean parserInitialized = false;
    static AuthConfigFactory slaveFactory = null;

    // keep the slave from being visible outside
    static AuthConfigProvider slaveProvider = null;

    protected AuthConfigFactory factory = null;
    private WebServicesDelegate wsdelegate = null;

    public GFServerConfigProvider(Map properties, AuthConfigFactory factory) {
        this.factory = factory;
        initializeParser();

        if (factory != null) {
            boolean hasSlaveFactory = false;
            try {
                rwLock.readLock().lock();
                hasSlaveFactory = (slaveFactory != null);
            }  finally {
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
            hasSlaveProvider = (slaveProvider != null);
        }  finally {
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
        wsdelegate =Globals.get(WebServicesDelegate.class);
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
                parserClassName = 
                    System.getProperty("config.parser", DEFAULT_PARSER_CLASS);
		loadParser(this, factory, null); 
		parserInitialized = true;
            }
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    /**
     * Instantiate+initialize module class
     */
    static ModuleInfo createModuleInfo(Entry entry,
            CallbackHandler handler, String type, Map properties)
            throws AuthException {
        try {
            // instantiate module using no-arg constructor
            Object newModule = entry.newInstance();

            Map map = properties;
            Map entryOptions = entry.getOptions();

            if (entryOptions != null) {
                if (map == null) {
                    map = new HashMap();
                } else {
                    map = new HashMap(map);
                }
                map.putAll(entryOptions);
            }

            // no doPrivilege at this point, need to revisit
            if (SERVER.equals(type)) {
                if (newModule instanceof ServerAuthModule) {
                    ServerAuthModule sam = (ServerAuthModule)newModule;
                    sam.initialize(entry.getRequestPolicy(),
                        entry.getResponsePolicy(), handler, map);
                } else if (newModule instanceof
                        com.sun.enterprise.security.jauth.ServerAuthModule) {

                    com.sun.enterprise.security.jauth.ServerAuthModule sam0 =
                        (com.sun.enterprise.security.jauth.ServerAuthModule) 
			newModule;

                    AuthPolicy requestPolicy =
                            (entry.getRequestPolicy() != null) ?
                            new AuthPolicy(entry.getRequestPolicy()) : null;

                    AuthPolicy responsePolicy =
                            (entry.getResponsePolicy() != null) ?
                            new AuthPolicy(entry.getResponsePolicy()) : null;

                    sam0.initialize(requestPolicy, responsePolicy,
                        handler, map);
                }
            } else { // CLIENT
                if (newModule instanceof ClientAuthModule) {
                    ClientAuthModule cam = (ClientAuthModule)newModule;
                    cam.initialize(entry.getRequestPolicy(),
                        entry.getResponsePolicy(), handler, map);
                } else if (newModule instanceof
		    com.sun.enterprise.security.jauth.ClientAuthModule) {

                    com.sun.enterprise.security.jauth.ClientAuthModule cam0 =
                        (com.sun.enterprise.security.jauth.ClientAuthModule)
			newModule;

                    AuthPolicy requestPolicy = 
			new AuthPolicy(entry.getRequestPolicy());

		    AuthPolicy responsePolicy =
			new AuthPolicy(entry.getResponsePolicy());

                    cam0.initialize(requestPolicy,responsePolicy,
                        handler, map);
                }
            }

            return new ModuleInfo(newModule, map);
        } catch(Exception e) {
            if (e instanceof AuthException) {
                throw (AuthException)e;
            }
            AuthException ae = new AuthException();
            ae.initCause(e);
            throw ae;
        }
    }

    /**
     * Create an object of a given class.
     * @param className
     *
     */
    private static Object createObject(final String className) {
	final ClassLoader loader = getClassLoader();
	if (System.getSecurityManager() != null) {
	    try {
		return AccessController.doPrivileged
		    (new PrivilegedExceptionAction() {
			public Object run() throws Exception {
			    Class c = Class.forName(className, true, loader);
			    return c.newInstance();
			}
		    });
	    } catch (PrivilegedActionException pae) {
		throw new RuntimeException(pae.getException());
	    }
	}
	try {
	    Class c = Class.forName(className, true, loader);
	    return c.newInstance();
	} catch (Throwable t) {
	    throw new RuntimeException(t);
	} 
    }

    Entry getEntry(String intercept,
            String id, MessagePolicy requestPolicy,
            MessagePolicy responsePolicy, String type) {

        // get the parsed module config and DD information

        Map configMap;

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

        InterceptEntry intEntry = (InterceptEntry)configMap.get(intercept);
        if (intEntry == null || intEntry.idMap == null) {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("module config has no IDs configured for [" +
                                intercept +
                                "]");
            }
            return null;
        }

        // look up the DD's provider ID in the module config

        IDEntry idEntry = null;
        if (id == null || (idEntry = (IDEntry)intEntry.idMap.get(id)) == null){

            // either the DD did not specify a provider ID,
            // or the DD-specified provider ID was not found
            // in the module config.
            //
            // in either case, look for a default ID in the module config

            if (logger.isLoggable(Level.FINE)) {
                logger.fine("DD did not specify ID, " +
                                "or DD-specified ID for [" +
                                intercept +
                                "] not found in config -- " +
                                "attempting to look for default ID");
            }

            String defaultID;
            if (CLIENT.equals(type)) {
                defaultID = intEntry.defaultClientID;
            } else {
                defaultID = intEntry.defaultServerID;
            }

            idEntry = (IDEntry)intEntry.idMap.get(defaultID);
            if (idEntry == null) {

                // did not find a default provider ID

                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("no default config ID for [" +
                                        intercept +
                                        "]");
                }
                return null;
            }
        }

        // we found the DD provider ID in the module config
        // or we found a default module config

        // check provider-type
        if (idEntry.type.indexOf(type) < 0) {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("request type [" +
                                type +
                                "] does not match config type [" +
                                idEntry.type +
                                "]");
            }
            return null;
        }

        // check whether a policy is set
        MessagePolicy reqP =
            (requestPolicy != null || responsePolicy != null) ?
            requestPolicy : 
            idEntry.requestPolicy;  //default;        

        MessagePolicy respP =
            (requestPolicy != null || responsePolicy != null) ?
            responsePolicy : 
            idEntry.responsePolicy;  //default;        

        // optimization: if policy was not set, return null
        if (reqP == null && respP == null) {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("no policy applies");
            }
            return null;
        }

        // return the configured modules with the correct policies

        Entry entry = new Entry(idEntry.moduleClassName,
                reqP, respP, idEntry.options);
            
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("getEntry for: " + intercept + " -- " + id + 
                    "\n    module class: " + entry.moduleClassName +
                    "\n    options: " + entry.options +
                    "\n    request policy: " + entry.requestPolicy +
                    "\n    response policy: " + entry.responsePolicy);
        }

        return entry;
    }

    /**
     * Class representing a single AuthModule entry configured
     * for an ID, interception point, and stack.
     *
     * <p> This class also provides a way for a caller to obtain
     * an instance of the module listed in the entry by invoking
     * the <code>newInstance</code> method.
     */
    static class Entry {

        // for loading modules
        private static final Class[] PARAMS = { };
        private static final Object[] ARGS = { };

        private String moduleClassName;
        private MessagePolicy requestPolicy;
        private MessagePolicy responsePolicy;
        private Map options;

        /**
         * Construct a ConfigFile entry.
         *
         * <p> An entry encapsulates a single module and its related
         * information.
         *
         * @param moduleClassName the module class name
         * @param requestPolicy the request policy assigned to the module
         *                listed in this entry, which may be null.
         *
         * @param responsePolicy the response policy assigned to the module
         *                listed in this entry, which may be null.
         *
         * @param options the options configured for this module.
         */
        Entry(String moduleClassName, MessagePolicy requestPolicy,
                MessagePolicy responsePolicy, Map options) {
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

        Map getOptions() {
            return options;
        }

        /**
         * Return a new instance of the module contained in this entry.
         *
         * <p> The default implementation of this method attempts
         * to invoke the default no-args constructor of the module class.
         * This method may be overridden if a different constructor
         * should be invoked.
         *
         * @return a new instance of the module contained in this entry.
         *
         * @exception AuthException if the instantiation failed.
         */
        Object newInstance() throws AuthException {
            try {
                final ClassLoader finalLoader = getClassLoader();
                Class c = Class.forName(moduleClassName,
                                        true,
                                        finalLoader);
                Constructor constructor = c.getConstructor(PARAMS);
                return constructor.newInstance(ARGS);
            } catch (Exception e) {
                if (logger.isLoggable(Level.WARNING)) {
                    logger.log(Level.WARNING,
                        "jmac.provider_unable_to_load_authmodule",
                        new String [] { moduleClassName, e.toString() });
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
        HashMap idMap;

        public InterceptEntry(String defaultClientID,
                String defaultServerID, HashMap idMap) {
            this.defaultClientID = defaultClientID;
            this.defaultServerID = defaultServerID;
            this.idMap = idMap;
        }
        
        public HashMap getIdMap() {
            return idMap;
        }
        public void setIdMap(HashMap map) {
            idMap = map;
        }
        public String getDefaultClientID(){
            return defaultClientID;
        }
        public String getDefaultServerID(){
            return defaultServerID;
        }
    }

    /**
     * parsed ID entry
     */
    public static class IDEntry {
        private String type;  // provider type (client, server, client-server)
        private String moduleClassName;
        private MessagePolicy requestPolicy;
        private MessagePolicy responsePolicy;
        private Map options;

        public String getModuleClassName() {
            return moduleClassName;
        }

        public Map getOptions() {
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

        public IDEntry(String type, String moduleClassName,
                MessagePolicy requestPolicy,
                MessagePolicy responsePolicy,
                Map options) {
            this.type = type;
            this.moduleClassName = moduleClassName;
            this.requestPolicy = requestPolicy;
            this.responsePolicy = responsePolicy;
            this.options = options;
        }
    }

    /**
     * A data object contains module object and the corresponding map.
     */
    protected static class ModuleInfo {
        private Object module;
        private Map map;

        ModuleInfo(Object module, Map map) {
            this.module = module;
            this.map = map;
        }

        Object getModule() {
            return module;
        }

        Map getMap() {
            return map;
        }   
    }

    /**
     * Get an instance of ClientAuthConfig from this provider.
     *
     * <p> The implementation of this method returns a ClientAuthConfig
     * instance that describes the configuration of ClientAuthModules
     * at a given message layer, and for use in an identified application
     * context.
     *
     * @param layer a String identifying the message layer
     *                for the returned ClientAuthConfig object. 
     *          This argument must not be null.
     *
     * @param appContext a String that identifies the messaging context 
     *          for the returned ClientAuthConfig object.
     *          This argument must not be null.
     *
     * @param handler a CallbackHandler to be passed to the ClientAuthModules
     *                encapsulated by ClientAuthContext objects derived from 
     *                the returned ClientAuthConfig. This argument may be null,
     *                in which case the implementation may assign a default 
     *                handler to the configuration. 
     *
     * @return a ClientAuthConfig Object that describes the configuration
     *                of ClientAuthModules at the message layer and messaging 
     *                context identified by the layer and appContext arguments.
     *                This method does not return null.
     *
     * @exception AuthException if this provider does not support the 
     *          assignment of a default CallbackHandler to the returned 
     *          ClientAuthConfig.
     *
     * @exception SecurityException if the caller does not have permission
     *                to retrieve the configuration.
     *
     * The CallbackHandler assigned to the configuration must support 
     * the Callback objects required to be supported by the profile of this
     * specification being followed by the messaging runtime. 
     * The CallbackHandler instance must be initialized with any application 
     * context needed to process the required callbacks 
     * on behalf of the corresponding application.
     */
    public ClientAuthConfig getClientAuthConfig
            (String layer, String appContext, CallbackHandler handler) 
            throws AuthException {
        return new GFClientAuthConfig(this, layer, appContext, handler);
    }

    /**
     * Get an instance of ServerAuthConfig from this provider.
     *
     * <p> The implementation of this method returns a ServerAuthConfig
     * instance that describes the configuration of ServerAuthModules
     * at a given message layer, and for a particular application context.
     *
     * @param layer a String identifying the message layer
     *                for the returned ServerAuthConfig object.
     *          This argument must not be null.
     *
     * @param appContext a String that identifies the messaging context 
     *          for the returned ServerAuthConfig object.
     *          This argument must not be null.
     *
     * @param handler a CallbackHandler to be passed to the ServerAuthModules
     *                encapsulated by ServerAuthContext objects derived from 
     *                thr returned ServerAuthConfig. This argument may be null,
     *                in which case the implementation may assign a default 
     *                handler to the configuration.
     *
     * @return a ServerAuthConfig Object that describes the configuration
     *                of ServerAuthModules at a given message layer,
     *                and for a particular application context. 
     *                This method does not return null.
     *
     * @exception AuthException if this provider does not support the 
     *          assignment of a default CallbackHandler to the returned
     *          ServerAuthConfig.
     *
     * @exception SecurityException if the caller does not have permission
     *                to retrieve the configuration.
     * <p>
     * The CallbackHandler assigned to the configuration must support 
     * the Callback objects required to be supported by the profile of this
     * specification being followed by the messaging runtime. 
     * The CallbackHandler instance must be initialized with any application 
     * context needed to process the required callbacks 
     * on behalf of the corresponding application.
     */
    public ServerAuthConfig getServerAuthConfig        
            (String layer, String appContext, CallbackHandler handler)
            throws AuthException {
        return new GFServerAuthConfig(this, layer, appContext, handler);
    }


    /**
     * Causes a dynamic configuration provider to update its internal 
     * state such that any resulting change to its state is reflected in
     * the corresponding authentication context configuration objects 
     * previously created by the provider within the current process context. 
     *
     * @exception AuthException if an error occured during the refresh.
     *
     * @exception SecurityException if the caller does not have permission
     *                to refresh the provider.
     */
    
    public void refresh() {
        loadParser(this, factory, null);
    }

    /** 
     * this method is intended to be called by the admin configuration system
     * when the corresponding config object has changed. 
     * It relies on the slaves, since it is a static method.
     * @param config a config object of type understood by the parser.
     * NOTE: there appears to be a thread saftey problem, and this method
     * will fail if a slaveProvider has not been established prior to its call.
     */
    public static void loadConfigContext(Object config) {

	boolean hasSlaveFactory = false;
	boolean hasSlaveProvider = false;
        rwLock.readLock().lock();
	try {    
	    hasSlaveFactory = (slaveFactory != null);
            hasSlaveProvider = (slaveProvider != null);
	}  finally {
	    rwLock.readLock().unlock();
	}

	if (slaveProvider == null) {
	    if (logger.isLoggable(Level.SEVERE)) {
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

    protected static void loadParser(AuthConfigProvider aProvider, 
            AuthConfigFactory aFactory, Object config) {
        rwLock.writeLock().lock();
        try {
            ConfigParser nextParser;
            int next = epoch + 1;
            nextParser = (ConfigParser)createObject(parserClassName);
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
                        String regisID = aFactory.registerConfigProvider
         		       (aProvider, layer, null,
 		           "GFServerConfigProvider: self registration");
                        layerDefaultRegisIDMap.put(layer, regisID);
                    }
                }
            }
            epoch = (next == 0 ? 1 : next);
            parser = nextParser;
        } catch(IOException ex) {
            throw new RuntimeException(ex);
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    protected static ClassLoader getClassLoader() {
	if (System.getSecurityManager() == null) {
	    return Thread.currentThread().getContextClassLoader();
	} 
	    
	return (ClassLoader) AccessController.doPrivileged
	    (new PrivilegedAction() {
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
        protected AuthConfigProvider provider = null;
        protected String layer = null;
        protected String appContext = null;
        protected CallbackHandler handler = null;
        protected String type = null;
        protected String providerID = null;
        protected boolean init = false;
        protected boolean onePolicy = false;
//        protected boolean newHandler = false;
	protected MessageSecurityBindingDescriptor binding = null;
        protected SunWebApp sunWebApp = null;

        protected GFAuthConfig(AuthConfigProvider provider,
                String layer, String appContext, 
                CallbackHandler handler, String type) {
            this.provider = provider;
            this.layer = layer;
            this.appContext = appContext;
            this.type = type;
            if (handler == null) {
                handler = AuthMessagePolicy.getDefaultCallbackHandler();
//		this.newHandler = true;
            }
            this.handler = handler;
        }

        /**
         * Get the message layer name of this authentication context
	 * configuration object.
         *
         * @return the message layer name of this configuration object, or null
         * if the configuration object pertains to an unspecified message 
	 * layer.
         */
        public String getMessageLayer() {
            return layer;
        }

        /**
         * Get the application context identifier of this authentication 
         * context configuration object.
         *
         * @return the String identifying the application context of this
         * configuration object or null if the configuration object pertains
         * to an unspecified application context.
         */
        public String getAppContext() {
            return appContext;
        }

	/**
	 * Get the authentication context identifier corresponding to the
	 * request and response objects encapsulated in messageInfo.
         * 
         * See method AuthMessagePolicy. getHttpServletPolicies() 
         * for more details on why this method returns
         * the String's "true" or "false" for AuthContextID.
	 *
	 * @param messageInfo a contextual Object that encapsulates the
	 *          client request and server response objects.
	 *
	 * @return the authentication context identifier corresponding to the 
	 *          encapsulated request and response objects, or null.
         * 
	 *
	 * @throws IllegalArgumentException if the type of the message
	 * objects incorporated in messageInfo are not compatible with
	 * the message types supported by this 
	 * authentication context configuration object.
	 */
        public String getAuthContextID(MessageInfo messageInfo) {
            if (GFServerConfigProvider.HTTPSERVLET.equals(layer)) {
		String isMandatoryStr =
		    (String)messageInfo.getMap().
		    get(HttpServletConstants.IS_MANDATORY);
		return Boolean.valueOf(isMandatoryStr).toString();
	    } else if (GFServerConfigProvider.SOAP.equals(layer)) {
                if (wsdelegate != null) {
                    return wsdelegate.getAuthContextID(messageInfo);
                }
                return null;
            } else {
                return null;
            }
        }

        // we should be able to replace the following with a method on packet

        /**
         * Causes a dynamic anthentication context configuration object to 
         * update the internal state that it uses to process calls to its
         * <code>getAuthContext</code> method.
         *
         * @exception AuthException if an error occured during the update.
         *
         * @exception SecurityException if the caller does not have permission
         *                to refresh the configuration object.
         */
        public void refresh() {
            loadParser(provider, factory, null); 
        }

	/**
	 * Used to determine whether or not the <code>getAuthContext</code> 
	 * method of the authentication context configuration will return null
	 * for all possible values of authentication context identifier.
	 *
	 * @return false when <code>getAuthContext</code> will return null for
	 *        all possible values of authentication context identifier. 
	 *        Otherwise, this method returns true.
	 */
	public boolean isProtected() {
	    // XXX TBD
	    return true;
	}

        protected AuthParam getAuthParam(MessageInfo info) 
	    throws AuthException{

	    if (GFServerConfigProvider.HTTPSERVLET.equals(layer)) {
		return new HttpServletAuthParam(info);
	    } else if (GFServerConfigProvider.SOAP.equals(layer)) {
                if (wsdelegate != null) {
                    return wsdelegate.newSOAPAuthParam(info);
                }
	    } 
	    throw new AuthException("unsupported AuthParam type");
	}

        CallbackHandler getCallbackHandler() {
            return handler;
        }

        protected ModuleInfo 
	getModuleInfo(String authContextID, Map properties) 
	    throws AuthException {
            if (!init) {
                initialize(properties);
            }

            MessagePolicy[] policies = null;

            if (GFServerConfigProvider.HTTPSERVLET.equals(layer)) {

		policies = AuthMessagePolicy.getHttpServletPolicies
		    (authContextID);

	    } else {

		policies = AuthMessagePolicy.getSOAPPolicies
		    (binding, authContextID, onePolicy);
	    }

            MessagePolicy requestPolicy = policies[0];
            MessagePolicy responsePolicy = policies[1];

            Entry entry = getEntry(layer, providerID,
                    requestPolicy, responsePolicy, type);

            return (entry != null)?
                    createModuleInfo(entry, handler, type, properties) : null;
        }

        // lazy initialize this as SunWebApp is not available in
        // RealmAdapter creation
        private void initialize(Map properties) {
            if (!init) {

		if (GFServerConfigProvider.HTTPSERVLET.equals(layer)) {
                    sunWebApp = AuthMessagePolicy.getSunWebApp(properties);
                    providerID = AuthMessagePolicy.getProviderID(sunWebApp);
		    onePolicy = true;
		}  else {
		    binding = AuthMessagePolicy.getMessageSecurityBinding
			(layer,properties);
		    providerID = AuthMessagePolicy.getProviderID(binding);
		    onePolicy = AuthMessagePolicy.oneSOAPPolicy(binding);
		}

                // handlerContext need to be explictly set by caller
                init = true;
            }
        }
    }

    class GFServerAuthConfig extends GFAuthConfig implements ServerAuthConfig {

        protected GFServerAuthConfig(AuthConfigProvider provider,
                String layer, String appContext,
                CallbackHandler handler) {
            super(provider, layer, appContext, handler, SERVER);
        }

        public ServerAuthContext getAuthContext(
                String authContextID, Subject serviceSubject, Map properties) 
                throws AuthException {
            ServerAuthContext serverAuthContext = null;
            ModuleInfo moduleInfo = getModuleInfo(authContextID,properties);

            if (moduleInfo != null && moduleInfo.getModule() != null) {
                Object moduleObj = moduleInfo.getModule();
                Map map = moduleInfo.getMap();
                if (moduleObj instanceof ServerAuthModule) {
                    serverAuthContext = new GFServerAuthContext(this,
                            (ServerAuthModule)moduleObj, map);
                } else {
                    serverAuthContext = new GFServerAuthContext
			(this,
			 (com.sun.enterprise.security.jauth.ServerAuthModule)
			 moduleObj, map);
                }
            }

            return serverAuthContext;
        }
    }

    class GFClientAuthConfig extends GFAuthConfig implements ClientAuthConfig {

        protected GFClientAuthConfig(AuthConfigProvider provider,
                String layer, String appContext,
                CallbackHandler handler) {
            super(provider, layer, appContext, handler, CLIENT);
        }

        public ClientAuthContext getAuthContext(String authContextID,
                Subject clientSubject, Map properties) 
                throws AuthException {
            ClientAuthContext clientAuthContext = null;
            ModuleInfo moduleInfo = getModuleInfo(authContextID, properties);

            if (moduleInfo != null && moduleInfo.getModule() != null ) {
                Object moduleObj = moduleInfo.getModule();
                Map map = moduleInfo.getMap();
                if (moduleObj instanceof ClientAuthModule) {
                    clientAuthContext = new GFClientAuthContext(this,
                            (ClientAuthModule)moduleObj, map);
                } else {
                    clientAuthContext = new GFClientAuthContext
			(this,
			(com.sun.enterprise.security.jauth.ClientAuthModule)
			 moduleObj, map);
                }
            }

            return clientAuthContext;
        }
    }

    static protected class GFServerAuthContext implements ServerAuthContext {

        private GFServerAuthConfig config;
        private ServerAuthModule module;
        private com.sun.enterprise.security.jauth.ServerAuthModule oldModule;

        private Map map;
        boolean managesSession = false;

        GFServerAuthContext(GFServerAuthConfig config, 
                                      ServerAuthModule module, Map map) {
            this.config = config;
            this.module = module;
            this.oldModule = null;
            this.map = map;
        }
  
        GFServerAuthContext(GFServerAuthConfig config, 
                com.sun.enterprise.security.jauth.ServerAuthModule module,
                Map map) {
            this.config = config;
            this.module = null;
            this.oldModule = module;
            this.map = map;
            if (map != null) {
                String msStr = (String)map.get
		    (GFServerConfigProvider.MANAGES_SESSIONS_OPTION);
                if (msStr != null) {
                    managesSession = Boolean.valueOf(msStr);
                }
            }
        }

	// for old modules
	private static void _setCallerPrincipals(Subject s,
                CallbackHandler handler, Subject pvcSubject) throws 
		AuthException {

            if (handler != null) { // handler should be non-null
		Set<Principal> ps = s.getPrincipals();
		if (ps == null || ps.isEmpty()) {
		    return;
		}
                Iterator<Principal> it = ps.iterator();

		Callback[] callbacks = new Callback[] 
		    { new CallerPrincipalCallback(s, it.next().getName()) };
		if (pvcSubject != null) {
		    s.getPrincipals().addAll(pvcSubject.getPrincipals());
		}

		try {
		    handler.handle(callbacks);
		} catch (Exception e) {
		    AuthException aex = new AuthException();
                    aex.initCause(e);
                    throw aex;
		}
            }
	}

	// for old modules
	private static void setCallerPrincipals(final Subject s,
                final CallbackHandler handler, final Subject pvcSubject)
                throws AuthException {
            if (System.getSecurityManager() == null) { 
		_setCallerPrincipals(s,handler,pvcSubject);
	    } else {
		try {
		    AccessController.doPrivileged
			(new PrivilegedExceptionAction() {
			    public Object run() throws Exception {
				_setCallerPrincipals(s,handler,pvcSubject);
				return null;
                            }
                        });
                } catch(PrivilegedActionException pae) {
                    Throwable cause = pae.getCause();
                    AuthException aex = new AuthException();
                    aex.initCause(cause);
                    throw aex;
                }
            }
	}

        public AuthStatus validateRequest(MessageInfo messageInfo,
                Subject clientSubject, Subject serviceSubject) 
                throws AuthException {
            if (module != null) {
                return module.validateRequest
                    (messageInfo, clientSubject, serviceSubject);
            } 

	    if (oldModule != null) {
                try {
                    subjectLocal.remove();
                    oldModule.validateRequest(config.getAuthParam(messageInfo),
                            clientSubject,
                            messageInfo.getMap());
		    setCallerPrincipals(clientSubject,
                            config.getCallbackHandler(), subjectLocal.get());
                    if (!managesSession &&
                            GFServerConfigProvider.HTTPSERVLET.equals(
                            config.getMessageLayer())) {
                        messageInfo.getMap().put(
                            HttpServletConstants.REGISTER_WITH_AUTHENTICATOR,
                            Boolean.TRUE.toString());
                    }
                    return AuthStatus.SUCCESS;
                } catch(PendingException pe) {
                    return AuthStatus.SEND_CONTINUE;
                } catch(FailureException fe) {
                    return AuthStatus.SEND_FAILURE;
                } finally {
                    subjectLocal.remove();
                }
            } else {
                throw new AuthException();
            }
        }

        public AuthStatus secureResponse(MessageInfo messageInfo,
                Subject serviceSubject) throws AuthException {
            if (module != null) {
                return module.secureResponse(messageInfo, serviceSubject);
            } 

	    if (oldModule != null) {
                oldModule.secureResponse(config.getAuthParam(messageInfo),
                        serviceSubject,
                        messageInfo.getMap());
                return AuthStatus.SEND_SUCCESS;
            } else {
                throw new AuthException();
            }
        }

        public void cleanSubject(MessageInfo messageInfo, Subject subject)
                throws AuthException {
            if (module != null) {
                module.cleanSubject(messageInfo, subject);
            } else if (oldModule != null) {
                oldModule.disposeSubject(subject, messageInfo.getMap());
            } else {
                 throw new AuthException();
            }
        }
    }

    static protected class GFClientAuthContext implements ClientAuthContext {

        private GFClientAuthConfig config;
        private ClientAuthModule module;
        private com.sun.enterprise.security.jauth.ClientAuthModule oldModule;
       // private Map map;

        GFClientAuthContext(GFClientAuthConfig config, 
                                      ClientAuthModule module, Map map) {
            this.config = config;
            this.module = module;
            this.oldModule = null;
           // this.map = map;
        }

        GFClientAuthContext(GFClientAuthConfig config, 
	com.sun.enterprise.security.jauth.ClientAuthModule module, Map map) {
            this.config = config;
            this.module = null;
            this.oldModule = module;
        //    this.map = map;
        }

        public AuthStatus secureRequest(MessageInfo messageInfo,
                Subject clientSubject) throws AuthException {
            if (module != null) {
                return module.secureRequest(messageInfo, clientSubject);
            }
            
	    if (oldModule != null) {
		oldModule.secureRequest(config.getAuthParam(messageInfo),
					clientSubject,
					messageInfo.getMap());
		return AuthStatus.SEND_SUCCESS;
            } else {
                throw new AuthException();
            }
        }

        public AuthStatus validateResponse(MessageInfo messageInfo,
                Subject clientSubject, Subject serviceSubject)
                throws AuthException {
            if (module != null) {
                return module.validateResponse(messageInfo, clientSubject,
                        serviceSubject);
            }

	    if (oldModule != null) {
                oldModule.validateResponse(config.getAuthParam(messageInfo),
					  clientSubject, messageInfo.getMap());
                return AuthStatus.SUCCESS;
            } else {
                throw new AuthException();
            }
        }

        public void cleanSubject(MessageInfo messageInfo, Subject subject)
                throws AuthException {
            if (module != null) {
                module.cleanSubject(messageInfo, subject);
            } else if (oldModule != null) {
                oldModule.disposeSubject(subject, messageInfo.getMap());
            } else {
		throw new AuthException();
            }
        }
    }
}
