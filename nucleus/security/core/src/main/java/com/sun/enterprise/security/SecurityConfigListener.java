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

package com.sun.enterprise.security;

import com.sun.enterprise.security.auth.realm.NoSuchRealmException;
import org.jvnet.hk2.config.*;
import org.jvnet.hk2.config.types.Property;

import javax.inject.Singleton;
import org.jvnet.hk2.annotations.Service;
import javax.inject.Inject;

import com.sun.enterprise.config.serverbeans.SecurityService;
import com.sun.enterprise.config.serverbeans.AuthRealm;
import com.sun.enterprise.config.serverbeans.JaccProvider;
import com.sun.enterprise.config.serverbeans.AuditModule;
import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.MessageSecurityConfig;

import com.sun.enterprise.security.audit.BaseAuditManager;
import com.sun.enterprise.security.auth.realm.Realm;
import com.sun.enterprise.security.auth.realm.RealmsManager;
import java.beans.PropertyChangeEvent;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;
import javax.inject.Named;
import javax.security.auth.login.Configuration;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.hk2.api.PostConstruct;

/**
 *
 * @author kumar.jayanti
 */
@Service
@Singleton
public class SecurityConfigListener implements ConfigListener, PostConstruct {
    
    @Inject @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    SecurityService securityService;
    
    @Inject
    private Logger logger;
    
    @Inject
    private RealmsManager realmsManager;

    @Inject
    BaseAuditManager auditManager;
    
    private String auditEnabled = null;
    private String defaultRealm = null;
    private String jacc = null;
    private String activateDefaultP2RMapping = null;
    private String mappedPrincipalClassName = null;

    public SecurityConfigListener() {
        
    }
     /**
     * Notification that @Configured objects that were injected have changed
     *
     * @param events list of changes
     */
public UnprocessedChangeEvents changed(PropertyChangeEvent[] events) {
    // I am not so interested with the list of events, just sort who got added or removed for me.
    ConfigSupport.sortAndDispatch(events, new Changed() {
        /**
         * Notification of a change on a configuration object
         *
         * @param type            type of change : ADD mean the changedInstance was added to the parent
         *                        REMOVE means the changedInstance was removed from the parent, CHANGE means the
         *                        changedInstance has mutated.
         * @param changedType     type of the configuration object
         * @param changedInstance changed instance.
         */
        public <T extends ConfigBeanProxy> NotProcessed changed(TYPE type, Class<T> changedType, T changedInstance) {
            NotProcessed np = null;
            switch(type) {
                case ADD : logger.fine("A new " + changedType.getName() + " was added : " + changedInstance);
                    np = handleAddEvent(changedInstance);
                    break;

                case CHANGE : logger.fine("A " + changedType.getName() + " was changed : " + changedInstance);
                    np = handleChangeEvent(changedInstance);
                    break;

                case REMOVE : logger.fine("A " + changedType.getName() + " was removed : " + changedInstance);
                    np = handleRemoveEvent(changedInstance);
                    break;
            }
            return np;
        }

        private <T extends ConfigBeanProxy> NotProcessed handleAddEvent( T instance) {
            NotProcessed np = null;
            if(instance instanceof AuthRealm){
                authRealmCreated((AuthRealm)instance);
            }else if (instance instanceof JaccProvider){
                np = new NotProcessed( "Cannot change JACC provider once installed, restart required" );
                //inject PolicyLoader and try to call loadPolicy
                //but policyLoader in V2 does not allow reloading of policy provider
                //once installed. The only option is restart the server
            }else if (instance instanceof AuditModule){
                auditModuleCreated((AuditModule)instance);
            }else if (instance instanceof MessageSecurityConfig){
                // do nothing since we have a Message security config listener
            } else if (instance instanceof SecurityService) {
               //since everything exists the only thing that can be added
               // in terms of Attrs is the defaultPrincipal and defaultPrinPassword
               // but they are directly used from securityService in core/security
            }
            else {
                np = new NotProcessed( "unimplemented: unknown instance: " + instance.getClass().getName() );
            }
            return np;
        }

        private <T extends ConfigBeanProxy> NotProcessed handleRemoveEvent(final T instance) {
            NotProcessed np = null;
            if(instance instanceof AuthRealm){
                authRealmDeleted((AuthRealm)instance);
            }else if (instance instanceof JaccProvider){
                np = new NotProcessed( "Cannot change JACC provider once installed, restart required" );
                //inject PolicyLoader and try to call loadPolicy
                //but policyLoader in V2 does not allow reloading of policy provider
                //once installed. The only option is restart the server
            }else if (instance instanceof AuditModule){
                auditModuleDeleted((AuditModule)instance);
            }else if (instance instanceof MessageSecurityConfig){
               //do nothing since we have a message security config listener
            } else if (instance instanceof SecurityService) {
               // The only Attrs on securityService whose removal can affect the
               // security code are those which are stored explicitly
               // they are getAuditEnabled, getDefaultRealm and getAuditModules
               // not sure what the effect of removing getDefaultRealm
            }
            else {
                np = new NotProcessed( "unimplemented: unknown instance: " + instance.getClass().getName() );
            }
            return np;
        }
        
        private <T extends ConfigBeanProxy> NotProcessed handleChangeEvent(final T instance) {
            NotProcessed np = null;
            if(instance instanceof AuthRealm){
                authRealmUpdated((AuthRealm)instance);
            }else if (instance instanceof JaccProvider){
                np = new NotProcessed( "Cannot change JACC provider once installed, restart required" );
                //inject PolicyLoader and try to call loadPolicy
                //but policyLoader in V2 does not allow reloading of policy provider
                //once installed. The only option is restart the server
            }else if (instance instanceof AuditModule){
                auditModuleUpdated((AuditModule)instance);
            }else if (instance instanceof MessageSecurityConfig){
               //do nothing since we have a message security config listener
            } else if (instance instanceof SecurityService) {
               // The only Attrs on securityService whose change in value can affect the
               // security code are those which are stored explicitly
               // they are getAuditEnabled, getDefaultRealm and getAuditModules
               if (defaultRealm != null && 
                       !defaultRealm.equals(((SecurityService)instance).getDefaultRealm())) {
                   defaultRealm = ((SecurityService)instance).getDefaultRealm();
                   Realm.setDefaultRealm(defaultRealm);
               }
               if ((auditEnabled != null) &&
                       !auditEnabled.equals(((SecurityService)instance).getAuditEnabled())) {
                   boolean auditON = Boolean.parseBoolean(((SecurityService)instance).getAuditEnabled());
                   auditManager.setAuditOn(auditON);
               }
               if (!jacc.equals(((SecurityService)instance).getJacc())) {
                   np = new NotProcessed( "Cannot change JACC provider once installed, restart required" );
               }
               if ((mappedPrincipalClassName != null) && !mappedPrincipalClassName.equals(((SecurityService)instance).getMappedPrincipalClass())) {
                   np = new NotProcessed( "MappedPrincipalClassname changes for existing applications requires restart and redeployment" );
               }
               if (!activateDefaultP2RMapping.equals(((SecurityService)instance).getActivateDefaultPrincipalToRoleMapping())) {
                   np = new NotProcessed( "DefaultP2R changes for existng applications requires restart and redeployment" );
               }
            }
            else {
                np = new NotProcessed( "unimplemented: unknown instance: " + instance.getClass().getName() );
            }
            return np;
        }
    }, logger);
     return null;
}

    /**
     * New auth realm created.
     * It is called whenever a AuthRealmEvent with action of
     * AuthRealmEvent.ACTION_CREATE is received.
     * @throws AdminEventListenerException when the listener is unable to
     *         process the event.
     */
    public static void authRealmCreated(AuthRealm instance){
        try {
            createRealm(instance);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * New auth realm created.
     * It is called whenever a AuthRealmEvent with action of
     * AuthRealmEvent.ACTION_CREATE is received.
     * @throws AdminEventListenerException when the listener is unable to
     *         process the event.
     */
    public static void authRealmCreated(Config config, AuthRealm instance){
        try {
            createRealm(config, instance);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Auth realm deleted.
     * It is called whenever a AuthRealmEvent with action of
     * AuthRealmEvent.ACTION_DELETE is received.
     * @throws AdminEventListenerException when the listener is unable to
     *         process the event.
     */
    public static void authRealmDeleted(Config config, AuthRealm instance) {
        try {
            //the listener firing has been unpredictable earlier
            //after a CLI delete the listener's were not firing in time
            //so we added explicit calls to this method from CLI
            //now with latest builds it looks like listeners also fire
            //causing a NoSuchRealmException
            if (!Realm.isValidRealm(config.getName(), instance.getName())) {
                return;
            }
            Realm.unloadInstance(config.getName(), instance.getName());
        } catch (NoSuchRealmException ex) {
            throw new RuntimeException(ex);
        }
    }


    /**
     * Auth realm deleted.
     * It is called whenever a AuthRealmEvent with action of
     * AuthRealmEvent.ACTION_DELETE is received.
     * @throws AdminEventListenerException when the listener is unable to
     *         process the event.
     */
    public static void authRealmDeleted(AuthRealm instance) {
        try {
            //the listener firing has been unpredictable earlier
            //after a CLI delete the listener's were not firing in time
            //so we added explicit calls to this method from CLI
            //now with latest builds it looks like listeners also fire
            //causing a NoSuchRealmException
            if (!Realm.isValidRealm(instance.getName())) {
                return;
            }
            Realm.unloadInstance(instance.getName());
        } catch (NoSuchRealmException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Auth realm updated (attributes change).
     * It is called whenever a AuthRealmEvent with action of
     * AuthRealmEvent.ACTION_UPDATE is received.
     * @throws AdminEventListenerException when the listener is unable to
     *         process the event.
     */
    public void authRealmUpdated(AuthRealm instance) {
        try {
            realmsManager.removeFromLoadedRealms(instance.getName());
            createRealm(instance);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Auth realm updated (attributes change).
     * It is called whenever a AuthRealmEvent with action of
     * AuthRealmEvent.ACTION_UPDATE is received.
     * @throws AdminEventListenerException when the listener is unable to
     *         process the event.
     */
    public void authRealmUpdated(Config config, AuthRealm instance) {
        try {
            realmsManager.removeFromLoadedRealms(config.getName(),instance.getName());
            createRealm(config, instance);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * This method will create or replace existing realm with a new one
     * in cache.
     * @param event
     * @exception for instance, BadRealmException, ConfigException,
     *            SynchronizationException
     */
    private static void createRealm(AuthRealm authRealm) throws Exception {
        //authRealm cannot be null here
        String className = authRealm.getClassname();
        List<Property> elementProps = authRealm.getProperty();
        Properties props = new Properties();
        if (elementProps != null) {
            for (Property p : elementProps) {
                props.setProperty(p.getName(), p.getValue());
            }
        }
        Realm.instantiate(authRealm.getName(), className, props);
        Configuration.getConfiguration().refresh();
    }

    /**
     * This method will create or replace existing realm with a new one
     * in cache.
     * @param event
     * @exception for instance, BadRealmException, ConfigException,
     *            SynchronizationException
     */
    private static void createRealm(Config config, AuthRealm authRealm) throws Exception {
        //authRealm cannot be null here
        String className = authRealm.getClassname();
        List<Property> elementProps = authRealm.getProperty();
        Properties props = new Properties();
        if (elementProps != null) {
            for (Property p : elementProps) {
                props.setProperty(p.getName(), p.getValue());
            }
        }
        Realm.instantiate(authRealm.getName(), className, props, config.getName());
        Configuration.getConfiguration().refresh();
    }

    public void postConstruct() {
        if (securityService == null) {
            //should never happen
            return;
        }
        //the first 3 of them below are not stored anywhere and directly
        //used from securityService instance available
        //even defaultPrincipal and defaultPrincipalPassword is directly being
        //read from securityService.
        auditEnabled = securityService.getAuditEnabled();
        defaultRealm = securityService.getDefaultRealm();        
        jacc = securityService.getJacc();      
        if(jacc == null) {
            jacc = "default";
        }
        activateDefaultP2RMapping = securityService.getActivateDefaultPrincipalToRoleMapping();
        mappedPrincipalClassName = securityService.getMappedPrincipalClass();
        
    }
    
    /**
     * New audit module created.
     * It is called whenever a AuditModuleEvent with action of
     * AuditModuleEvent.ACTION_CREATE is received.
     */
    public void auditModuleCreated(AuditModule instance) {
        try {
            String classname = instance.getClassname();
            List<Property> props = instance.getProperty();
            Properties properties = new Properties();
            if (props != null) {
                for (Property p : props) {
                    properties.put(p.getName(), p.getValue());
                }
            }
            auditManager.addAuditModule(instance.getName(), classname, properties);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Audit module deleted.
     * It is called whenever a AuditModuleEvent with action of
     * AuditModuleEvent.ACTION_DELETE is received.
     */
    public void auditModuleDeleted(AuditModule instance) {
       
        auditManager.removeAuditModule(instance.getName());
    }

    /**
     * Audit module updated (attributes change).
     * It is called whenever a AuditModuleEvent with action of
     * AuditModuleEvent.ACTION_UPDATE is received.
     */
    public void auditModuleUpdated(AuditModule instance) {
        try {

            List<Property> props = instance.getProperty();
            Properties properties = new Properties();
            if (props != null) {
                for (Property p : props) {
                    properties.put(p.getName(), p.getValue());
                }
            }
            // we don't have a way to get hold of the Old Module in V3
            // so we would always delete and create new
           
            auditManager.addAuditModule(instance.getName(), instance.getClassname(), properties);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }


    

}
