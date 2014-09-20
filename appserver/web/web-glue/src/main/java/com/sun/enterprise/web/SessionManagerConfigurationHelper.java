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

package com.sun.enterprise.web;

import com.sun.enterprise.deployment.WebBundleDescriptor;
import com.sun.enterprise.web.session.PersistenceType;
import org.glassfish.logging.annotation.LogMessageInfo;
import org.glassfish.web.deployment.runtime.ManagerProperties;
import org.glassfish.web.deployment.runtime.SessionManager;
import org.glassfish.web.deployment.runtime.StoreProperties;
import org.glassfish.web.deployment.runtime.WebProperty;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author  lwhite
 * @author Rajiv Mordani
 */
public class SessionManagerConfigurationHelper {

    private static final Logger _logger = com.sun.enterprise.web.WebContainer.logger;

    @LogMessageInfo(
            message = "Web App Distributable {0}: {1}",
            level = "FINEST")
    public static final String WEB_APP_DISTRIBUTABLE = "AS-WEB-GLUE-00123";

    @LogMessageInfo(
            message = "AvailabilityGloballyEnabled = {0}",
            level = "FINEST")
    public static final String AVAILABILITY_GLOBALLY_ENABLED = "AS-WEB-GLUE-00124";

    @LogMessageInfo(
            message = "instance-level persistence-type = {0} instance-level persistenceFrequency = {1} instance-level persistenceScope = {2}",
            level = "FINEST")
    public static final String INSTANCE_LEVEL_INFO = "AS-WEB-GLUE-00125";

    @LogMessageInfo(
            message = "webAppLevelPersistenceType = {0} webAppLevelPersistenceFrequency = {1} webAppLevelPersistenceScope = {2}",
            level = "FINEST")
    public static final String WEB_APP_LEVEL_INFO = "AS-WEB-GLUE-00126";

    @LogMessageInfo(
            message = "IN WebContainer>>ConfigureSessionManager after web level check AFTER_WEB_PERSISTENCE-TYPE IS = {0} AFTER_WEB_PERSISTENCE_FREQUENCY IS = {1} AFTER_WEB_PERSISTENCE_SCOPE IS = {2}",
            level = "FINEST")
    public static final String AFTER_WEB_LEVEL_CHECK_INFO = "AS-WEB-GLUE-00127";

    @LogMessageInfo(
            message = "Is {0} a system app: {1}",
            level = "FINEST")
    public static final String IS_SYSTEM_APP = "AS-WEB-GLUE-00128";

    @LogMessageInfo(
            message = "SessionConfigurationHelper: Is AppDistributable {0}",
            level = "FINEST")
    public static final String IS_APP_DISTRIBUTABLE = "AS-WEB-GLUE-00129";

    @LogMessageInfo(
            message = "Invalid Session Management Configuration for non-distributable app [{0}] - defaulting to memory: persistence-type = [{1}] / persistenceFrequency = [{2}] / persistenceScope = [{3}]",
            level = "INFO")
    public static final String INVALID_SESSION_MANAGER_CONFIG = "AS-WEB-GLUE-00130";

    @LogMessageInfo(
            message = "IN WebContainer>>ConfigureSessionManager before builder factory FINAL_PERSISTENCE-TYPE IS = {0} FINAL_PERSISTENCE_FREQUENCY IS = {1} FINAL_PERSISTENCE_SCOPE IS = {2}",
            level = "FINEST")
    public static final String CONFIGURE_SESSION_MANAGER_FINAL = "AS-WEB-GLUE-00131";


protected WebModule _ctx = null;
    protected SessionManager _smBean = null; 
    protected WebBundleDescriptor _wbd = null;
    protected WebModuleConfig _wmInfo = null;
    protected PersistenceType _persistence = PersistenceType.MEMORY;
    protected String _persistenceFrequency = null;
    protected String _persistenceScope = null;
    private boolean _initialized = false;
    private ArrayList<String> _systemApps = new ArrayList<String>();
    protected ServerConfigLookup serverConfigLookup;

    /** Creates a new instance of SessionManagerConfigurationHelper */
    public SessionManagerConfigurationHelper(
        WebModule ctx, SessionManager smBean, WebBundleDescriptor wbd,
        WebModuleConfig wmInfo, ServerConfigLookup serverConfigLookup) {
        _ctx = ctx;
        _smBean = smBean;
        _wbd = wbd;
        _wmInfo = wmInfo;
        this.serverConfigLookup = serverConfigLookup;
        _systemApps.add("com_sun_web_ui");
        _systemApps.add(Constants.DEFAULT_WEB_MODULE_PREFIX + "admingui");
        _systemApps.add("adminapp");
        _systemApps.add("admingui");        
    }
    
    protected boolean isSystemApp(String appName) {
        return _systemApps.contains(appName);
    }
    
    protected void initializeConfiguration() {

        //XXX Need to look at whether the app is distributable.
        
        boolean isAppDistributable = false;
        if (_wbd != null) {
            isAppDistributable = _wbd.isDistributable();
        }
        if (_logger.isLoggable(Level.FINEST)) {
            _logger.log(Level.FINEST,
                    WEB_APP_DISTRIBUTABLE,
                    new Object[] {getApplicationId(_ctx), isAppDistributable});
        }

        PersistenceType persistence = PersistenceType.MEMORY;
        String persistenceFrequency = null;
        String persistenceScope = null;
        
        boolean isAvailabilityEnabled = 
            serverConfigLookup.calculateWebAvailabilityEnabledFromConfig(_ctx);
        if (_logger.isLoggable(Level.FINEST)) {
            _logger.log(Level.FINEST, AVAILABILITY_GLOBALLY_ENABLED, isAvailabilityEnabled);
        }
        if (isAvailabilityEnabled) {
            // These are the global defaults if nothing is
            // set at domain.xml or sun-web.xml
            persistence = PersistenceType.REPLICATED;
            persistenceFrequency = "web-method";
            persistenceScope = "session";
        }
        
        PersistenceType serverDefaultPersistenceType =
            serverConfigLookup.getPersistenceTypeFromConfig();


        if (serverDefaultPersistenceType != null) {
            persistence = serverDefaultPersistenceType;        
            persistenceFrequency = serverConfigLookup.getPersistenceFrequencyFromConfig();
            persistenceScope = serverConfigLookup.getPersistenceScopeFromConfig();
        }
        String insLevelPersistenceTypeString = null;
        if (persistence != null) {
            insLevelPersistenceTypeString = persistence.getType();
        }
        if (_logger.isLoggable(Level.FINEST)) {
            _logger.log(Level.FINEST,
                    INSTANCE_LEVEL_INFO,
                    new Object[] {insLevelPersistenceTypeString, persistenceFrequency, persistenceScope});
        }
        
        String webAppLevelPersistenceFrequency = null;
        String webAppLevelPersistenceScope = null;

        if (_smBean != null) {
            // The persistence-type controls what properties of the 
            // session manager can be configured
            String pType = _smBean.getAttributeValue(SessionManager.PERSISTENCE_TYPE);
            persistence = PersistenceType.parseType(pType, persistence);

            webAppLevelPersistenceFrequency = getPersistenceFrequency(_smBean);           
            webAppLevelPersistenceScope = getPersistenceScope(_smBean);
            if (_logger.isLoggable(Level.FINEST)) {
                _logger.log(Level.FINEST,
                        WEB_APP_LEVEL_INFO,
                        new Object[] {pType, webAppLevelPersistenceFrequency, webAppLevelPersistenceScope});
            }
        }
        
        // Use web app level values if they exist (i.e. not null)
        if (webAppLevelPersistenceFrequency != null) {
            persistenceFrequency = webAppLevelPersistenceFrequency;
        }
        if (webAppLevelPersistenceScope != null) {
            persistenceScope = webAppLevelPersistenceScope;
        }
        if (_logger.isLoggable(Level.FINEST)) {
            _logger.log(Level.FINEST, AFTER_WEB_LEVEL_CHECK_INFO,
                    new Object[] {persistence.getType(), persistenceFrequency, persistenceScope});
        }
        
        // Delegate remaining initialization to builder
        String frequency = null;
        String scope = null;
        if ( persistence == PersistenceType.MEMORY 
            || persistence == PersistenceType.FILE 
            || persistence == PersistenceType.CUSTOM) {
            // Deliberately leaving frequency & scope null
        } else {
            frequency = persistenceFrequency;
            scope = persistenceScope;
        }

        // If app is not distributable and non-memory option
        // is attempted, log error and set back to "memory"
        if (!isAppDistributable && persistence != PersistenceType.MEMORY) {
            String wmName = getApplicationId(_ctx);
            if (_logger.isLoggable(Level.FINEST)) {
                _logger.log(Level.FINEST, IS_SYSTEM_APP, new Object[] {wmName, isSystemApp(wmName)});
            }

            if (_logger.isLoggable(Level.FINEST)) {
                _logger.log(Level.FINEST, IS_APP_DISTRIBUTABLE, isAppDistributable);
            }
            // Suppress log error msg for default-web-module
            // log message only if availabilityenabled = true is attempted
            if (isAvailabilityEnabled &&
                    !wmName.equals(Constants.DEFAULT_WEB_MODULE_NAME) &&
                    !this.isSystemApp(wmName)) { 
                //log error
                if (_logger.isLoggable(Level.INFO)) {
                    Object[] params = { getApplicationId(_ctx), persistence.getType(), frequency, scope };
                    _logger.log(Level.INFO,
                                INVALID_SESSION_MANAGER_CONFIG,
                                params); 
                }
            }    
            // Set back to memory option
            persistence = PersistenceType.MEMORY;
            frequency = null;
            scope = null;            
        }
        
        // If availability-enabled is false, reset to "memory"
        if (!isAvailabilityEnabled && (persistence != PersistenceType.FILE &&
                persistence != PersistenceType.COOKIE &&
                persistence != PersistenceType.COHERENCE_WEB)) {
            // Set back to memory option
            persistence = PersistenceType.MEMORY;
            frequency = null;
            scope = null;             
        }
        
        if (_logger.isLoggable(Level.FINEST)) {
            _logger.log(Level.FINEST,
                    CONFIGURE_SESSION_MANAGER_FINAL,
                    new Object[] {persistence.getType(), frequency, scope});
        }
        
        _persistence = persistence;
        _persistenceFrequency = frequency;
        _persistenceScope = scope;
    }
    
    /**
     * The application id for this web module
     */    
    public String getApplicationId(WebModule ctx) {
        return ctx.getID();
    }
    
    /**
     * Get the persistence frequency for this web module
     * (this is the value from sun-web.xml if defined
     * @param smBean the session manager config bean
     */
    protected String getPersistenceFrequency(SessionManager smBean) {
        String persistenceFrequency = null;        
        ManagerProperties mgrBean = smBean.getManagerProperties();
        if ((mgrBean != null) && (mgrBean.sizeWebProperty() > 0)) {
            WebProperty[] props = mgrBean.getWebProperty();
            for (int i = 0; i < props.length; i++) {
                String name = props[i].getAttributeValue(WebProperty.NAME);
                String value = props[i].getAttributeValue(WebProperty.VALUE);
                if (name.equalsIgnoreCase("persistenceFrequency")) {
                    persistenceFrequency = value;
                }
            }
        }
        return persistenceFrequency;
    }
    
    /**
     * Get the persistence scope for this web module
     * (this is the value from sun-web.xml if defined
     * @param smBean the session manager config bean
     */    
    protected String getPersistenceScope(SessionManager smBean) {
        String persistenceScope = null;
        StoreProperties storeBean = smBean.getStoreProperties();
        if ((storeBean != null) && (storeBean.sizeWebProperty() > 0)) {
            WebProperty[] props = storeBean.getWebProperty();
            for (int i = 0; i < props.length; i++) {
                String name = props[i].getAttributeValue(WebProperty.NAME);
                String value = props[i].getAttributeValue(WebProperty.VALUE);
                if (name.equalsIgnoreCase("persistenceScope")) {
                    persistenceScope = value;
                }
            }
        }
        return persistenceScope;
    } 
    
    protected void checkInitialization() {
        if (!_initialized) {
            initializeConfiguration();
            _initialized = true;
        }
    }    
    
    public PersistenceType getPersistenceType() {
        checkInitialization();
        return _persistence;
    }
    
    public String getPersistenceFrequency() {
        checkInitialization();
        return _persistenceFrequency;
    } 
    
    public String getPersistenceScope() {
        checkInitialization();
        return _persistenceScope;
    }    
}
