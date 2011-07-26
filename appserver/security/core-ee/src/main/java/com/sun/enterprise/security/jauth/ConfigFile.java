/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.security.jauth;

import com.sun.enterprise.security.jmac.config.ConfigParser;
import com.sun.enterprise.security.jmac.config.GFServerConfigProvider;
import java.io.*;
import java.util.*;

import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.AppConfigurationEntry;

import com.sun.logging.LogDomains;

/**
 * This is a default file-based AuthConfig implementation.
 *
 * @version %I%, %G%
 */
class ConfigFile extends AuthConfig {

    // indicates the age of the configuration approximately in
    // terms of the number of times refresh has been called
    private int epoch;

    // parser class name
    private String parserClassName;

    // parser
    private ConfigParser parser;

    // package private for ConfigFileParser
    static final String CLIENT = "client";
    static final String SERVER = "server";

    private static final String DEFAULT_HANDLER_CLASS =
        "com.sun.enterprise.security.jmac.callback.ContainerCallbackHandler";

    private static final String DEFAULT_PARSER_CLASS =
	"com.sun.enterprise.security.jmac.config.ConfigDomainParser";

    private static final Logger logger = LogDomains.getLogger(ConfigFile.class, LogDomains.SECURITY_LOGGER);
    ConfigFile() throws IOException {
	String propertyValue = System.getProperty("config.parser");
	if (propertyValue == null) {
	    parserClassName = DEFAULT_PARSER_CLASS;
	} else {
	    parserClassName = propertyValue;
	}
	this.epoch = 1;
	parser = ConfigFile.loadParser(parserClassName);
        parser.initialize(null);
    }

    /**
     * Get a default ClientAuthContext.
     *
     * @return an instance of ConfigClient.
     */
    public ClientAuthContext getClientAuthContext(String intercept,
						String id,
						AuthPolicy requestPolicy,
						AuthPolicy responsePolicy,
						CallbackHandler handler)
		throws AuthException {

	ConfigFile.Entry[] entries = getEntries(intercept,
						id,
						requestPolicy,
						responsePolicy,
						CLIENT);
	if (entries == null || entries.length == 0) {
	    return null;
	}

	// instantiate and initialize modules up front as well

	if (handler == null) {
	    handler = ConfigFile.loadDefaultCallbackHandler();
	} else if (handler instanceof DependentCallbackHandler) { 
	    handler = new DelegatingHandler(handler);
        }

	for (int i = 0; i < entries.length; i++) {
	    entries[i].module = ConfigFile.createModule(entries[i], handler);
	}

	return new ConfigClient(entries);
    }

    /**
     * Get a default ServerAuthContext.
     *
     * @return an instance of ConfigServer.
     */
    public ServerAuthContext getServerAuthContext(String intercept,
						String id,
						AuthPolicy requestPolicy,
						AuthPolicy responsePolicy,
						CallbackHandler handler)
		throws AuthException {

	ConfigFile.Entry[] entries = getEntries(intercept,
						id,
						requestPolicy,
						responsePolicy,
						SERVER);
	if (entries == null || entries.length == 0) {
	    return null;
	}

	// instantiate and initialize modules up front as well

	if (handler == null) {
	    handler = ConfigFile.loadDefaultCallbackHandler();
	} else if (handler instanceof DependentCallbackHandler) { 
	    handler = new DelegatingHandler(handler);
        }

	for (int i = 0; i < entries.length; i++) {
	    entries[i].module = ConfigFile.createModule(entries[i], handler);
	}

	return new ConfigServer(entries);
    }

    public void refresh() throws AuthException {
	synchronized (this) {
	    ConfigParser nextParser;
	    int next = this.epoch + 1;
	    try {
		nextParser = ConfigFile.loadParser(parserClassName);
	    } catch (IOException ioe) {
		throw new AuthException(ioe.toString());
	    }
	    this.epoch = (next == 0 ? 1 : next);
	    parser = nextParser;
	}
    }

    private ConfigFile.Entry[] getEntries(String intercept,
				String id,
				AuthPolicy requestPolicy,
				AuthPolicy responsePolicy,
				String type) {

	// get the parsed module config and DD information

	Map configMap;

	synchronized (parser) {
	    configMap = parser.getConfigMap();
	}

	if (configMap == null) {
	    return null;
	}
	
	// get the module config info for this intercept

	GFServerConfigProvider.InterceptEntry intEntry = (GFServerConfigProvider.InterceptEntry)configMap.get(intercept);
	if (intEntry == null || intEntry.getIdMap() == null) {
	    if (logger != null && logger.isLoggable(Level.FINE)) {
 		logger.fine("module config has no IDs configured for [" +
				intercept +
				"]");
	    }
	    return null;
	}

	// look up the DD's provider ID in the module config

	GFServerConfigProvider.IDEntry idEntry = null;
	if (id == null || (idEntry = (GFServerConfigProvider.IDEntry)intEntry.getIdMap().get(id)) == null) {

	    // either the DD did not specify a provider ID,
	    // or the DD-specified provider ID was not found
	    // in the module config.
	    //
	    // in either case, look for a default ID in the module config

	    if (logger != null && logger.isLoggable(Level.FINE)) {
 		logger.fine("DD did not specify ID, " +
				"or DD-specified ID for [" +
				intercept +
				"] not found in config -- " +
				"attempting to look for default ID");
	    }

	    String defaultID;
	    if (CLIENT.equals(type)) {
		defaultID = intEntry.getDefaultClientID();
	    } else {
		defaultID = intEntry.getDefaultServerID();
	    }

	    idEntry = (GFServerConfigProvider.IDEntry)intEntry.getIdMap().get(defaultID);
	    if (idEntry == null) {

		// did not find a default provider ID

		if (logger != null && logger.isLoggable(Level.FINE)) {
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
	if (idEntry.getType().indexOf(type) < 0) {
	    if (logger != null && logger.isLoggable(Level.FINE)) {
 		logger.fine("request type [" +
				type +
				"] does not match config type [" +
				idEntry.getType() +
				"]");
	    }
	    return null;
	}

	// check whether a policy is set
        AuthPolicy reqP, respP;
        if(requestPolicy != null || responsePolicy != null) {
            reqP = requestPolicy;
            respP = responsePolicy;
        } else if(idEntry.getRequestPolicy() != null || idEntry.getResponsePolicy() != null) {
            //default
            reqP = new AuthPolicy(idEntry.getRequestPolicy());
            respP = new AuthPolicy(idEntry.getResponsePolicy());
        } else {
            // optimization: if policy was not set, return null
	    if (logger != null && logger.isLoggable(Level.FINE)) {
 		logger.fine("no policy applies");
	    }
	    return null;
	}

	// return the configured modules with the correct policies

//	ConfigFile.Entry[] entries = new Entry[idEntry.modules.size()];
        ConfigFile.Entry[] entries = new Entry[1];
	for (int i = 0; i < entries.length; i++) {
// Login Bridge profile?
//	    AppConfigurationEntry aEntry =
//				(AppConfigurationEntry)idEntry.modules.get(i);
	    entries[i] = new ConfigFile.Entry(reqP,
					respP,
					idEntry.getModuleClassName(),
					AppConfigurationEntry.LoginModuleControlFlag.REQUIRED,
					idEntry.getOptions());
	}
	    
	if (logger != null && logger.isLoggable(Level.FINE)) {
 		logger.fine("getEntries found " +
			entries.length +
			" entries for: " +
			intercept +
			" -- "
			+ id);


            for (int i = 0; i < entries.length; i++) {
                    logger.fine("Entry " + (i+1) + ":" +
                        "\n    module class: " + entries[i].getLoginModuleName() +
                        "\n    flag: " + entries[i].getControlFlag() +
                        "\n    options: " + entries[i].getOptions() +
                        "\n    request policy: " + entries[i].requestPolicy +
                        "\n    response policy: " + entries[i].responsePolicy);
            }

	}

	return entries;
    }

    /**
     * get a custom config file parser
     *
     * XXX custom file that can be used in place of [domain|sun-acc].xml
     */
    private static ConfigParser loadParser(String className)
		throws IOException {
	try {

	    final String finalClassName = className;
	    final ClassLoader finalLoader = AuthConfig.getClassLoader();

	    return (ConfigParser)java.security.AccessController.doPrivileged
		(new java.security.PrivilegedExceptionAction() {
		public Object run() throws Exception {
		    Class c = Class.forName(finalClassName,true,finalLoader);
		    return c.newInstance();
		}
	    });
	} catch (java.security.PrivilegedActionException pae) {
            IOException iex = new IOException(pae.getException().toString());
            iex.initCause(pae.getException());
            throw iex;
	}
    }

    /**
     * get the default callback handler
     */
    private static CallbackHandler loadDefaultCallbackHandler()
		throws AuthException {

        // get the default handler class
        try {

            final ClassLoader finalLoader = AuthConfig.getClassLoader();

            return (CallbackHandler)
                java.security.AccessController.doPrivileged
                (new java.security.PrivilegedExceptionAction() {
                public Object run() throws Exception {
 
		    String className = DEFAULT_HANDLER_CLASS;
		    Class c = Class.forName(className,
					    true,
					    finalLoader);
                    return c.newInstance();
                }
            });
        } catch (java.security.PrivilegedActionException pae) {
            AuthException aex = new AuthException(pae.getException().toString());
            aex.initCause(pae.getException());
            throw aex;
        }
    }

    /**
     * Instantiate+initialize module class
     */
    private static Object createModule(ConfigFile.Entry entry,
					CallbackHandler handler)
		throws AuthException {
	try {

	    // instantiate module using no-arg constructor

	    Object newModule = entry.newInstance();

	    // initialize module

	    Object[] initArgs = { entry.getRequestPolicy(),
				entry.getResponsePolicy(),
				handler,
				entry.getOptions() };

            try {
                Method initMethod = newModule.getClass().getMethod(AuthContext.INIT,
                        AuthPolicy.class, AuthPolicy.class, CallbackHandler.class,
                        Map.class);
                initMethod.invoke(newModule, initArgs);
		// return the new module
		return newModule;
            } catch(Exception ex) {
                throw new SecurityException("could not invoke " +
                        AuthContext.INIT +
                        " method in module: " + 
                        newModule.getClass().getName() + " " + ex, ex);
            }

	} catch (Exception e) {
	    if (e instanceof AuthException) {
		throw (AuthException)e;
	    }
	    AuthException ae = new AuthException();
	    ae.initCause(e);
	    throw ae;
	}
    }

    /**
     * Class representing a single AuthModule entry configured
     * for an ID, interception point, and stack.
     *
     * <p> An instance of this class contains the same
     * information as its superclass, AppConfigurationEntry.
     * It additionally stores the request and response policy assigned
     * to this module.
     *
     * <p> This class also provides a way for a caller to obtain
     * an instance of the module listed in the entry by invoking
     * the <code>newInstance</code> method.
     */
    static class Entry extends javax.security.auth.login.AppConfigurationEntry {

	// for loading modules
	private static final Class[] PARAMS = { };
	private static final Object[] ARGS = { };

	private AuthPolicy requestPolicy;
	private AuthPolicy responsePolicy;
	Object module = null;	// convenience location to store instance -
			// package private for AuthContext

	/**
	 * Construct a ConfigFile entry.
	 *
	 * <p> An entry encapsulates a single module and its related
	 * information.
	 *
	 * @param requestPolicy the request policy assigned to the module
	 *		listed in this entry, which may be null.
	 *
	 * @param responsePolicy the response policy assigned to the module
	 *		listed in this entry, which may be null.
	 *
	 * @param moduleClass the fully qualified class name of the module.
	 *
	 * @param flag the module control flag.  This value must either
	 *		be REQUIRED, REQUISITE, SUFFICIENT, or OPTIONAL.
	 *
	 * @param options the options configured for this module.
	 */
	Entry(AuthPolicy requestPolicy,
			AuthPolicy responsePolicy,
			String moduleClass,
			AppConfigurationEntry.LoginModuleControlFlag flag,
			Map options) {
	    super(moduleClass, flag, options);
	    this.requestPolicy = requestPolicy;
	    this.responsePolicy = responsePolicy;
	}

	/**
	 * Return the request policy assigned to this module.
	 *
	 * @return the policy, which may be null.
	 */
	AuthPolicy getRequestPolicy() {
	    return requestPolicy;
	}

	/**
	 * Return the response policy assigned to this module.
	 *
	 * @return the policy, which may be null.
	 */
	AuthPolicy getResponsePolicy() {
	    return responsePolicy;
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
		final ClassLoader finalLoader= AuthConfig.getClassLoader();
		String clazz = getLoginModuleName();
		Class c = Class.forName(clazz,
					true,
					finalLoader);
		java.lang.reflect.Constructor constructor =
					c.getConstructor(PARAMS);
		return constructor.newInstance(ARGS);
	    } catch (Exception e) {
		AuthException ae = new AuthException();
		ae.initCause(e);
		throw ae;
	    }
	}
    }

    /**
     * parsed Intercept entry
     */
   /* static class InterceptEntry {

	String defaultClientID;
	String defaultServerID;
	HashMap idMap;

	InterceptEntry(String defaultClientID,
		String defaultServerID,
		HashMap idMap) {
	    this.defaultClientID = defaultClientID;
	    this.defaultServerID = defaultServerID;
	    this.idMap = idMap;
	}
    }*/

    /**
     * parsed ID entry
     */
    /*static class IDEntry {

	private String type;  // provider type (client, server, client-server)
	private AuthPolicy requestPolicy;
	private AuthPolicy responsePolicy;
	private ArrayList modules;

	IDEntry(String type,
		AuthPolicy requestPolicy,
		AuthPolicy responsePolicy,
		ArrayList modules) {

	    this.type = type;
	    this.modules = modules;
	    this.requestPolicy = requestPolicy;
	    this.responsePolicy = responsePolicy;
	}

	// XXX delete this later
	IDEntry(String type,
		String requestPolicy,
		String responsePolicy,
		ArrayList modules) {

	    this.type = type;

	    if (requestPolicy != null) {
		this.requestPolicy =
			new AuthPolicy(AuthPolicy.SOURCE_AUTH_SENDER,
					true,	// recipient-auth
					true);	// beforeContent
	    }
	    if (responsePolicy != null) {
		this.responsePolicy =
			new AuthPolicy(AuthPolicy.SOURCE_AUTH_CONTENT,
					true,	// recipient-auth
					false);	// beforeContent
	    }

	    this.modules = modules;
	}
    } */

    /**
     * Default implementation of ClientAuthContext.
     */
    private static class ConfigClient implements ClientAuthContext {

	// class that does all the work
	private AuthContext context;


	ConfigClient(Entry[] entries) throws AuthException {
	    context = new AuthContext(entries, logger);
	}

	public void secureRequest(AuthParam param,
				Subject subject,
				Map sharedState)
		throws AuthException {

	    // invoke modules
	    Object[] args = { param, subject, sharedState };
	    context.invoke(AuthContext.SECURE_REQUEST, args);
	}

	public void validateResponse(AuthParam param,
				Subject subject,
				Map sharedState)
		throws AuthException {
	    // invoke modules
	    Object[] args = { param, subject, sharedState };
	    context.invoke(AuthContext.VALIDATE_RESPONSE, args);
	}

	public void disposeSubject(Subject subject,
				Map sharedState)
		throws AuthException {
	    // invoke modules
	    Object[] args = { subject, sharedState };
	    context.invoke(AuthContext.DISPOSE_SUBJECT, args);
	}
    }

    /**
     * Default implementation of ServerAuthContext.
     */
    private static class ConfigServer implements ServerAuthContext {

	// class that does all the work
	private AuthContext context;


	ConfigServer(Entry[] entries) throws AuthException {

	    context = new AuthContext(entries, logger);
	}

	public void validateRequest(AuthParam param,
				Subject subject,
				Map sharedState)
		throws AuthException {
	    // invoke modules
	    Object[] args = { param, subject, sharedState };
	    context.invoke(AuthContext.VALIDATE_REQUEST, args);
	}

	public void secureResponse(AuthParam param,
				Subject subject,
				Map sharedState)
		throws AuthException {
	    // invoke modules
	    Object[] args = { param, subject, sharedState };
	    context.invoke(AuthContext.SECURE_RESPONSE, args);
	}

	public void disposeSubject(Subject subject,
				Map sharedState)
		throws AuthException {
	    // invoke modules
	    Object[] args = { subject, sharedState };
	    context.invoke(AuthContext.DISPOSE_SUBJECT, args);
	}

	public boolean managesSessions(Map sharedState)
		throws AuthException {
		    
	    // invoke modules
	    Object[] args = { sharedState };
	    Object[] rValues = null;

	    try {
		rValues = context.invoke(AuthContext.MANAGES_SESSIONS, args);
	    } catch (AuthException ae) {
		// this new method may not be implemeneted
		// by old modules
		if (!(ae.getCause() instanceof NoSuchMethodException)) {
		    throw ae;
		}
	    }

	    boolean rvalue = false;

	    for (int i=0; rValues != null && i<rValues.length; i++) {
		if (rValues[i] != null) {
		    boolean thisValue = ((Boolean) rValues[i]).booleanValue();
		    rvalue = rvalue | thisValue;
		}
	    }

	    return rvalue;
	}
    }

    private static class DelegatingHandler implements CallbackHandler {

	CallbackHandler handler;

	CallbackHandler defaultHandler;

	private DelegatingHandler(CallbackHandler cbh) {
	    handler = cbh;
	    try {
		defaultHandler = ConfigFile.loadDefaultCallbackHandler();
	    } catch (Exception e) {
		defaultHandler = null;
	    }
	}

	public void handle (Callback[] callbacks) 
	throws IOException, UnsupportedCallbackException {
	    if (defaultHandler == null) {
		handler.handle(callbacks);
	    }
	    else {
		Callback[] oneCallback = new Callback[1];
		for (int i=0; i<callbacks.length; i++) {
		    
		    boolean tryDefault = false;
		    
		    oneCallback[0] = callbacks[i];
		    try {
			handler.handle(oneCallback);
		    } catch (UnsupportedCallbackException uce) {
			tryDefault = true;
		    }
		    if (tryDefault) {
			defaultHandler.handle(oneCallback);
		    }
		}
	    }
	}
    }

}
