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

import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.MessageInfo;
import javax.security.auth.message.config.AuthConfig;
import javax.security.auth.message.config.AuthConfigFactory;
import javax.security.auth.message.config.AuthConfigProvider;
import javax.security.auth.message.config.ClientAuthConfig;
import javax.security.auth.message.config.ClientAuthContext;
import javax.security.auth.message.config.RegistrationListener;
import javax.security.auth.message.config.ServerAuthConfig;
import javax.security.auth.message.config.ServerAuthContext;
import javax.security.auth.message.config.AuthConfigFactory.RegistrationContext;

import com.sun.enterprise.security.jmac.AuthMessagePolicy;
import com.sun.enterprise.security.jmac.WebServicesDelegate;
import org.glassfish.internal.api.Globals;


/**
 * This is based Helper class for 196 Configuration.
 * This class implements RegistrationListener. 
 */
public abstract class ConfigHelper /*implements RegistrationListener*/ {
    private static final String DEFAULT_HANDLER_CLASS =
        "com.sun.enterprise.security.jmac.callback.ContainerCallbackHandler"; 

//    private static String handlerClassName = null;
    protected static final AuthConfigFactory factory = AuthConfigFactory.getFactory();

    private ReadWriteLock rwLock;
    private Lock rLock;
    private Lock wLock;

   
	
    protected String layer;
    protected String appCtxt;
    protected Map map;
    protected CallbackHandler cbh;
    protected AuthConfigRegistrationWrapper listenerWrapper = null;

    protected void init(String layer, String appContext,
            Map map, CallbackHandler cbh) {

	this.layer = layer;
	this.appCtxt = appContext;
	this.map = map;
	this.cbh = cbh;
        if (this.cbh == null) {
            this.cbh = getCallbackHandler();
        }

	this.rwLock = new ReentrantReadWriteLock(true);
        this.rLock = rwLock.readLock();
        this.wLock = rwLock.writeLock();
        
        listenerWrapper =  new  AuthConfigRegistrationWrapper(this.layer, this.appCtxt);
        
    }

    public void setJmacProviderRegisID(String jmacProviderRegisID) {
        this.listenerWrapper.setJmacProviderRegisID(jmacProviderRegisID);
    }
    
    public AuthConfigRegistrationWrapper getRegistrationWrapper() {
        return this.listenerWrapper;
    }
    
    public void setRegistrationWrapper(AuthConfigRegistrationWrapper wrapper) {
        this.listenerWrapper = wrapper;
    }
    
    public AuthConfigRegistrationWrapper.AuthConfigRegistrationListener getRegistrationListener() {
        return this.listenerWrapper.getListener();
    }

    public void disable() {
	listenerWrapper.disable();
    }

    public Object getProperty(String key) {
	return map == null ? null : map.get(key);
    }

    public String getAppContextID() {
	return appCtxt;
    }

    public ClientAuthConfig getClientAuthConfig() throws AuthException {
        return (ClientAuthConfig)getAuthConfig(false);
    }

    public ServerAuthConfig getServerAuthConfig() throws AuthException {
        return (ServerAuthConfig)getAuthConfig(true);
    }

    public ClientAuthContext getClientAuthContext(MessageInfo info, Subject s) 
    throws AuthException {
	ClientAuthConfig c = (ClientAuthConfig)getAuthConfig(false);
	if (c != null) {
	    return c.getAuthContext(c.getAuthContextID(info),s,map);
	}
	return null;
    }

    public ServerAuthContext getServerAuthContext(MessageInfo info, Subject s) 
    throws AuthException {
	ServerAuthConfig c = (ServerAuthConfig)getAuthConfig(true);
	if (c != null) {
	    return c.getAuthContext(c.getAuthContextID(info),s,map);
	}
	return null;
    }

    protected AuthConfig getAuthConfig(AuthConfigProvider p, boolean isServer) 
    throws AuthException {
	AuthConfig c = null; 
	if (p != null) {
	    if (isServer) { 
		c = p.getServerAuthConfig(layer, appCtxt, cbh);
	    } else {
		c = p.getClientAuthConfig(layer, appCtxt, cbh);
	    }
	}
	return c;
    }

    protected AuthConfig getAuthConfig(boolean isServer) throws AuthException {

	ConfigData d = null;
	AuthConfig c = null;
	boolean disabled = false;
	AuthConfigProvider lastP = null;

	try {
	    rLock.lock();
	    disabled = (!listenerWrapper.isEnabled());
	    if (!disabled) {
                d = listenerWrapper.getConfigData();
		if (d != null) {
		    c = (isServer ? d.sConfig : d.cConfig);
		    lastP = d.provider;
		}
	    }
	    
	} finally {
	    rLock.unlock();
	    if (disabled || c != null || (d != null && lastP == null)) {
		return c;
	    }
	} 


	// d == null || (d != null && lastP != null && c == null)
	if (d == null) {
	    try {
		wLock.lock();
                if (listenerWrapper.getConfigData()== null) {
                    AuthConfigProvider nextP =
                        factory.getConfigProvider(layer,appCtxt,this.getRegistrationListener());
                    if (nextP != null) {
                        listenerWrapper.setConfigData(new ConfigData(nextP,getAuthConfig(nextP,isServer)));
                    } else {
                        listenerWrapper.setConfigData(new ConfigData());
                    }
                }
                d = listenerWrapper.getConfigData();
	    } finally {
		wLock.unlock();
	    }
	} 

        return ((isServer)? d.sConfig : d.cConfig);
    }

    /**
     * Check if there is a provider register for a given layer and appCtxt.
     */
    protected boolean hasExactMatchAuthProvider() {
        boolean exactMatch = false;
        // XXX this may need to be optimized
        AuthConfigProvider p = 
                factory.getConfigProvider(layer, appCtxt, null);
        if (p != null) {
            String[] IDs = factory.getRegistrationIDs(p);
            for (String i : IDs) {
                RegistrationContext c = factory.getRegistrationContext(i);
                if (layer.equals(c.getMessageLayer()) && 
                        appCtxt.equals(c.getAppContext())) {
                    exactMatch = true;
                    break;
                }
            }
        }

        return exactMatch;
    }

    /**
     * Get the callback default handler
     */
    private CallbackHandler getCallbackHandler() {

        CallbackHandler rvalue = AuthMessagePolicy.getDefaultCallbackHandler();
        if (rvalue instanceof CallbackHandlerConfig) {
            ((CallbackHandlerConfig)rvalue).setHandlerContext
                    (getHandlerContext(map));
        }

        return rvalue;
    }

    /**
     * This method is invoked by the constructor and should be
     * overrided by subclass.
     */
    protected HandlerContext getHandlerContext(Map map) {
        return null;
    }

    private static class ConfigData {

	private AuthConfigProvider provider; 
	private AuthConfig sConfig; 
	private AuthConfig cConfig; 

	ConfigData() {
	    provider = null;
	    sConfig = null;
	    cConfig = null;
	}

	ConfigData(AuthConfigProvider p, AuthConfig a) {
	    provider = p;
	    if (a == null) {
		sConfig = null;
		cConfig = null;
	    } else if (a instanceof ServerAuthConfig) {
		sConfig = a;
		cConfig = null;
	    } else if (a instanceof ClientAuthConfig) {
		sConfig = null;
		cConfig = a;
	    } else {
		throw new IllegalArgumentException();
	    }
	}
    }
    
    //Adding extra inner class because specializing the Linstener Impl class would 
    //make the GF 196 implementation Non-Replaceable.
    // This class would hold a RegistrationListener within.
    public static class AuthConfigRegistrationWrapper {
        
        private String layer;
        private String appCtxt;
        private String jmacProviderRegisID = null;
        private boolean enabled;
        private ConfigData data;
        
        private Lock wLock;
        private ReadWriteLock rwLock;
        
        AuthConfigRegistrationListener listener;
        int referenceCount = 1;
        private WebServicesDelegate delegate = null;
        public AuthConfigRegistrationWrapper(String layer, String appCtxt) {
            this.layer = layer;
            this.appCtxt = appCtxt;
            this.rwLock = new ReentrantReadWriteLock(true);
	    this.wLock = rwLock.writeLock();
            enabled = (factory != null);
            listener = new AuthConfigRegistrationListener(layer, appCtxt);
            if (Globals.getDefaultHabitat() != null) {
                delegate = Globals.get(WebServicesDelegate.class);
            } else {
                try {
                    //for non HK2 environments
                    //try to get WebServicesDelegateImpl by reflection.
                    ClassLoader loader = Thread.currentThread().getContextClassLoader();
                    Class delegateClass = loader.loadClass("com.sun.enterprise.security.webservices.WebServicesDelegateImpl");
                    delegate = (WebServicesDelegate) delegateClass.newInstance();
                } catch (InstantiationException ex) {
                } catch (IllegalAccessException ex) {
                } catch (ClassNotFoundException ex) {
                }
            }
        }
        
        public AuthConfigRegistrationListener getListener() {
            return listener;
        }
        
        public void setListener(AuthConfigRegistrationListener listener) {
            this.listener = listener;
        }
        
        public void disable() {
            this.wLock.lock();
            try {
                setEnabled(false);
            } finally {
                this.wLock.unlock();
                data = null;
            }
            if (factory != null) {
                String[] ids = factory.detachListener(this.listener,layer,appCtxt);
//                if (ids != null) {
//                    for (int i=0; i < ids.length; i++) {
//                        factory.removeRegistration(ids[i]);
//                    }
//                }
                if (getJmacProviderRegisID() != null) {
                    factory.removeRegistration(getJmacProviderRegisID());
                }
            }
        }

        //detach the listener, but dont remove-registration
        public void disableWithRefCount() {
            if (referenceCount <= 1) {
               disable();
               if (delegate != null) {
                   delegate.removeListener(this);
               }
            } else {
                try {
                    this.wLock.lock();
                    referenceCount--;
                } finally {
                    this.wLock.unlock();
                }
                
            }
        }
        
        public void incrementReference() {
            try {
                this.wLock.lock();
                referenceCount++;
            } finally {
                this.wLock.unlock();
            }
        }
        
        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getJmacProviderRegisID() {
            return this.jmacProviderRegisID;
        }
        
        public void setJmacProviderRegisID(String jmacProviderRegisID) {
            this.jmacProviderRegisID = jmacProviderRegisID;
        }
        
        private ConfigData getConfigData() {
            return data;
        }
        
        private void setConfigData(ConfigData data) {
            this.data = data;
        }
         
    
        public class AuthConfigRegistrationListener implements RegistrationListener {
            
            private String layer;
            private String appCtxt;
            
            public AuthConfigRegistrationListener(String layer, String appCtxt) {
                this.layer = layer;
                this.appCtxt = appCtxt;
            }
            
            public void notify(String layer, String appContext) {
                if (this.layer.equals(layer) &&
                        ((this.appCtxt == null && appContext == null) ||
                        (appContext != null && appContext.equals(this.appCtxt)))) {
                    try {
                        wLock.lock();
                        data = null;
                    } finally {
                        wLock.unlock();
                    }
                }
            }
            
        }
    }
    
}
