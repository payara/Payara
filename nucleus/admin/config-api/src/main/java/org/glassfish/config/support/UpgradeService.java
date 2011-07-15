/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2010 Oracle and/or its affiliates. All rights reserved.
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
import java.util.List;
import java.util.ArrayList;
import java.io.File;

import com.sun.enterprise.config.serverbeans.*;
import org.glassfish.api.admin.config.ConfigurationUpgrade;
import org.jvnet.hk2.config.types.Property;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.PostConstruct;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.TransactionFailure;
import org.jvnet.hk2.config.Transaction;
import org.jvnet.hk2.config.RetryableException;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.types.PropertyBag;

/**
 * Startup service to update existing domain.xml to the latest expected format
 *
 * @author Jerome Dochez
 */
@Service
public class UpgradeService implements ConfigurationUpgrade, PostConstruct {

    @Inject
    Domain domain;

    /*
     * Required to make gms changes before any changes to a cluster
     * or config can be saved. This is because GMS changed attribute
     * names from v2 to 3.1. (Issue 15195.)
     */
    @Inject(name="gmsupgrade", optional=true)
    ConfigurationUpgrade precondition = null;

    private final static Logger logger = Logger.getAnonymousLogger();

    private static final String MODULE_TYPE = "moduleType";
    private static final String APPCLIENT_SNIFFER_NAME = "appclient";
    private static final String V3_0_1_JAVA_WEB_START_ENABLED_PROPERTY_NAME = "javaWebStartEnabled";
    private static final String GF3_1_JAVA_WEB_START_ENABLED_PROPERTY_NAME = "java-web-start-enabled";
    
    public void postConstruct() {
        upgradeApplicationElements();
    }

    private void upgradeApplicationElements() {
        upgradeV2ApplicationElements();
        upgradeV3PreludeApplicationElements();
        upgradeV3_0_1_AppClientElements();
    }

    private void upgradeV3PreludeApplicationElements() {
        // in v3-prelude, engines were created under application directly,
        // in v3 final, engines are placed under individual modules composing the application
        // so if we have engines under application and not modules deployed, we need to upgrade
        List<Application> allApps = new ArrayList<Application>();
        allApps.addAll(domain.getApplications().getApplications());
        allApps.addAll(domain.getSystemApplications().getApplications());
        for (Application app : allApps) {
            if (app.getEngine()!=null && app.getEngine().size()>0 &&
                    (app.getModule()==null || app.getModule().size()==0)) {
                // we need to update the application declaration from v3 prelude,
                // we can safely assume this was a single module application
                try {
                    ConfigSupport.apply(new SingleConfigCode<Application>() {
                        public Object run(Application application) throws PropertyVetoException, TransactionFailure {
                            Module module = application.createChild(Module.class);
                            module.setName(application.getName());
                            for (Engine engine : application.getEngine()) {
                                module.getEngines().add(engine);
                            }
                            application.getModule().add(module);
                            application.getEngine().clear();
                            return null;
                        }
                    }, app);
                } catch(TransactionFailure tf) {
                    Logger.getAnonymousLogger().log(Level.SEVERE, "Failure while upgrading application "
                            + app.getName() + " please redeploy", tf);
                    throw new RuntimeException(tf);
                }
            }
        }
    }

    /**
     * Adds a property with the specified name and value to a writable config
     * object.
     * @param <T> the type of the config object
     * @param propName name of the property to add
     * @param propValue value of the property to add
     * @param owner_w the owning config object
     * @return the added Property object
     * @throws TransactionFailure
     * @throws PropertyVetoException
     */
    private <T extends PropertyBag & ConfigBeanProxy> Property addProperty(
            final String propName,
            final String propValue,
            final T owner_w) throws TransactionFailure, PropertyVetoException {
        final Property p = owner_w.createChild(Property.class);
        p.setName(propName);
        p.setValue(propValue);
        owner_w.getProperty().add(p);
        return p;
    }

    private void upgradeV3_0_1_AppClientElements() {
        /*
         * If an app client has a property setting for javaWebStartEnabled we
         * convert it to java-web-start-enabled which is the documented name.
         * App clients can be either applications or modules within an EAR.
         */
        final Transaction t = new Transaction();
        try {
            for (Application app : domain.getApplications().getApplications()) {
                System.out.println("Checking app " + app.getName());
                Application app_w = null;
                Property oldSetting = app.getProperty(V3_0_1_JAVA_WEB_START_ENABLED_PROPERTY_NAME);
                if (oldSetting != null) {
                    logger.log(Level.INFO, "For application {0} converting property {1} to {2}",
                    new Object[] {
                        app.getName(),
                        V3_0_1_JAVA_WEB_START_ENABLED_PROPERTY_NAME,
                        GF3_1_JAVA_WEB_START_ENABLED_PROPERTY_NAME});
                    app_w = t.enroll(app);
                    addProperty(GF3_1_JAVA_WEB_START_ENABLED_PROPERTY_NAME,
                            oldSetting.getValue(), app_w);
                    app_w.getProperty().remove(oldSetting);
                }
                for (Module mod : app.getModule()) {
                    if (mod.getEngine(APPCLIENT_SNIFFER_NAME) != null) {
                        /*
                         * This is an app client.  See if the client has
                         * a property setting using the old name.
                         */
                        oldSetting = mod.getProperty(V3_0_1_JAVA_WEB_START_ENABLED_PROPERTY_NAME);
                        if (oldSetting != null) {
                            logger.log(Level.INFO, "For application {0}/module {1} converting property {2} to {3}",
                                new Object[] {
                                    app.getName(),
                                    mod.getName(),
                                    V3_0_1_JAVA_WEB_START_ENABLED_PROPERTY_NAME,
                                    GF3_1_JAVA_WEB_START_ENABLED_PROPERTY_NAME});
                            final Module mod_w = t.enroll(mod);
                            addProperty(GF3_1_JAVA_WEB_START_ENABLED_PROPERTY_NAME,
                                    oldSetting.getValue(),
                                    mod_w);
                            mod_w.getProperty().remove(oldSetting);
                        }
                    }
                }

            }
            t.commit();
        } catch (Exception ex) {
            t.rollback();
            throw new RuntimeException("Error upgrading application", ex);
        }
    }

    private void upgradeV2ApplicationElements() {
        // in v2, we have ejb-module, web-module, j2ee-application etc elements 
        // to represent different type of apps
        // in v3 final, we have one generic application element to represent 
        // all type of apps 
        // we will do three things
        // 1. tranform partially all the old application related elements to 
        //    the new generic application elements (only top level, no module 
        //    sub element).
        // 2. remove all the system apps
        Applications apps = domain.getApplications();

        // workaround to not execute any upgrade if the admin console or
        // any other system app is already installed
        SystemApplications sApps = domain.getSystemApplications();
        if (sApps != null && sApps.getModules().size()>0) {
            return;
        }

        Transaction t = new Transaction();
        try {
            ConfigBeanProxy apps_w = t.enroll(apps);

            // 1. transform all old application elements to new 
            //    application element

            // connector module
            for (ConnectorModule connectorModule : 
                apps.getModules(ConnectorModule.class)) {

                // adding the new application element
                Application app = apps_w.createChild(
                    Application.class);
                app.setName(connectorModule.getName());
                app.setLocation(getLocationAsURIString(
                    connectorModule.getLocation()));
                app.setObjectType(connectorModule.getObjectType());
                app.setDescription(connectorModule.getDescription());
                app.setEnabled(connectorModule.getEnabled());
                app.setDirectoryDeployed(
                    connectorModule.getDirectoryDeployed());
                for (Property property : 
                    connectorModule.getProperty()) {
                    Property prop = 
                        app.createChild(Property.class);
                    prop.setName(property.getName());
                    prop.setValue(property.getValue());
                    app.getProperty().add(prop);
                }

                Property prop =
                    app.createChild(Property.class);
                prop.setName(MODULE_TYPE);
                prop.setValue(ServerTags.CONNECTOR_MODULE);
                app.getProperty().add(prop);

                // removing the old connector module 
                ((Applications)apps_w).getModules().remove(connectorModule);
                // adding the new application element
                ((Applications)apps_w).getModules().add(app);
            }

            // ejb-module
            for (EjbModule ejbModule :
                apps.getModules(EjbModule.class)) {

                // adding the new application element
                Application app = apps_w.createChild(
                    Application.class);
                app.setName(ejbModule.getName());
                app.setLocation(getLocationAsURIString(
                    ejbModule.getLocation()));
                app.setObjectType(ejbModule.getObjectType());
                app.setDescription(ejbModule.getDescription());
                app.setEnabled(ejbModule.getEnabled());
                app.setDirectoryDeployed(
                    ejbModule.getDirectoryDeployed());
                app.setLibraries(ejbModule.getLibraries());
                app.setAvailabilityEnabled(
                            ejbModule.getAvailabilityEnabled());
                for (Property property :
                    ejbModule.getProperty()) {
                    Property prop = 
                        app.createChild(Property.class);
                    prop.setName(property.getName());
                    prop.setValue(property.getValue());
                    app.getProperty().add(prop);
                }

                Property prop =
                    app.createChild(Property.class);
                prop.setName(MODULE_TYPE);
                prop.setValue(ServerTags.EJB_MODULE);
                app.getProperty().add(prop);

                // removing the old ejb module
                ((Applications)apps_w).getModules().remove(ejbModule);
                // adding the new application element
                ((Applications)apps_w).getModules().add(app);
            }

            // web-module
            for (WebModule webModule :
                apps.getModules(WebModule.class)) {

                // adding the new application element
                Application app = apps_w.createChild(
                    Application.class);
                app.setName(webModule.getName());
                app.setLocation(getLocationAsURIString(
                    webModule.getLocation()));
                app.setObjectType(webModule.getObjectType());
                app.setDescription(webModule.getDescription());
                app.setEnabled(webModule.getEnabled());
                app.setDirectoryDeployed(
                    webModule.getDirectoryDeployed());
                app.setLibraries(webModule.getLibraries());
                app.setContextRoot(webModule.getContextRoot());
                app.setAvailabilityEnabled(
                    webModule.getAvailabilityEnabled());
                for (Property property :
                    webModule.getProperty()) {
                    Property prop = 
                        app.createChild(Property.class);
                    prop.setName(property.getName());
                    prop.setValue(property.getValue());
                    app.getProperty().add(prop);
                }

                Property prop =
                    app.createChild(Property.class);
                prop.setName(MODULE_TYPE);
                prop.setValue(ServerTags.WEB_MODULE);
                app.getProperty().add(prop);

                // removing the old web module
                ((Applications)apps_w).getModules().remove(webModule);
                // adding the new application element
                ((Applications)apps_w).getModules().add(app);
            }

            // appclient-module
            for (AppclientModule appclientModule :
                apps.getModules(AppclientModule.class)) {

                // adding the new application element
                Application app = apps_w.createChild(
                    Application.class);
                app.setName(appclientModule.getName());
                app.setLocation(getLocationAsURIString(
                    appclientModule.getLocation()));
                app.setObjectType("user");
                app.setDescription(appclientModule.getDescription());
                app.setEnabled("true");
                app.setDirectoryDeployed(
                    appclientModule.getDirectoryDeployed());
                for (Property property :
                    appclientModule.getProperty()) {
                    Property prop = 
                        app.createChild(Property.class);
                    prop.setName(property.getName());
                    prop.setValue(property.getValue());
                    app.getProperty().add(prop);
                }
                Property prop = 
                    app.createChild(Property.class);
                prop.setName(ServerTags.JAVA_WEB_START_ENABLED);
                prop.setValue(
                    appclientModule.getJavaWebStartEnabled());
                app.getProperty().add(prop);

                Property prop2 =
                    app.createChild(Property.class);
                prop2.setName(MODULE_TYPE);
                prop2.setValue(ServerTags.APPCLIENT_MODULE);
                app.getProperty().add(prop2);

                // removing the old appclient module
                ((Applications)apps_w).getModules().remove(appclientModule);
                // adding the new application element
                ((Applications)apps_w).getModules().add(app);
            }

            // j2ee-application
            for (J2eeApplication j2eeApp :
                apps.getModules(J2eeApplication.class)) {

                // adding the new application element
                Application app = apps_w.createChild(
                    Application.class);
                app.setName(j2eeApp.getName());
                app.setLocation(getLocationAsURIString(
                    j2eeApp.getLocation()));
                app.setObjectType(j2eeApp.getObjectType());
                app.setDescription(j2eeApp.getDescription());
                app.setEnabled(j2eeApp.getEnabled());
                app.setDirectoryDeployed(
                    j2eeApp.getDirectoryDeployed());
                app.setLibraries(j2eeApp.getLibraries());
                app.setAvailabilityEnabled(
                    j2eeApp.getAvailabilityEnabled());
                for (Property property :
                    j2eeApp.getProperty()) {
                    Property prop = 
                        app.createChild(Property.class);
                    prop.setName(property.getName());
                    prop.setValue(property.getValue());
                    app.getProperty().add(prop);
                }
                Property prop = 
                    app.createChild(Property.class);
                prop.setName(ServerTags.JAVA_WEB_START_ENABLED);
                prop.setValue(
                    j2eeApp.getJavaWebStartEnabled());
                app.getProperty().add(prop);

                Property prop2 =
                    app.createChild(Property.class);
                prop2.setName(MODULE_TYPE);
                prop2.setValue(ServerTags.J2EE_APPLICATION);
                app.getProperty().add(prop2);

                // removing the old j2eeapplication module
                ((Applications)apps_w).getModules().remove(j2eeApp);
                // adding the new application element
                ((Applications)apps_w).getModules().add(app);
            }

            // extension-module
            if (apps.getModules(
                ExtensionModule.class).size() > 0) {
                Logger.getAnonymousLogger().log(Level.WARNING, "Ignoring extension-module elements. GlassFish v3 does not support extension modules from GlassFish v2."); 
            }
            for (ExtensionModule extensionModule :
                apps.getModules(ExtensionModule.class)) {
                // removing the extension module
                ((Applications)apps_w).getModules().remove(extensionModule);
            }

            // lifecycle-module
            for (LifecycleModule lifecycleModule :
                apps.getModules(LifecycleModule.class)) {

                // adding the new application element
                Application app = apps_w.createChild(
                    Application.class);
                app.setName(lifecycleModule.getName());
                app.setObjectType(lifecycleModule.getObjectType());
                app.setDescription(lifecycleModule.getDescription());
                app.setEnabled(lifecycleModule.getEnabled());
                for (Property property :
                    lifecycleModule.getProperty()) {
                    Property prop = 
                        app.createChild(Property.class);
                    prop.setName(property.getName());
                    prop.setValue(property.getValue());
                    app.getProperty().add(prop);
                }

                Property prop = 
                    app.createChild(Property.class);
                prop.setName(ServerTags.CLASS_NAME);
                prop.setValue(
                    lifecycleModule.getClassName());
                app.getProperty().add(prop);

                if (lifecycleModule.getClasspath() != null) {
                    Property prop1 =
                        app.createChild(Property.class);
                    prop1.setName(ServerTags.CLASSPATH);
                    prop1.setValue(
                        lifecycleModule.getClasspath());
                    app.getProperty().add(prop1);

                }
                if (lifecycleModule.getLoadOrder() != null) {
                    Property prop2 =
                        app.createChild(Property.class);
                    prop2.setName(ServerTags.LOAD_ORDER);
                    prop2.setValue(
                        lifecycleModule.getLoadOrder());
                    app.getProperty().add(prop2);
                }

                Property prop3 =
                    app.createChild(Property.class);
                prop3.setName(ServerTags.IS_FAILURE_FATAL);
                prop3.setValue(
                    lifecycleModule.getIsFailureFatal());
                        app.getProperty().add(prop3);

                Property prop4 =
                    app.createChild(Property.class);
                prop4.setName(ServerTags.IS_LIFECYCLE);
                prop4.setValue("true");
                app.getProperty().add(prop4);

                // removing the old lifecycle module
                ((Applications)apps_w).getModules().remove(lifecycleModule);
                // adding the new application element
                ((Applications)apps_w).getModules().add(app);
            }

            // custom mbean
            if (apps.getModules(Mbean.class).size() > 0) {
                Logger.getAnonymousLogger().log(Level.WARNING, "Ignoring mbean elements. GlassFish v3 does not support custom MBeans from GlassFish v2."); 
            }
            for (Mbean mbean :
                apps.getModules(Mbean.class)) {
                // removing the custom mbean
                ((Applications)apps_w).getModules().remove(mbean);
            }

            // 2. remove all system apps
            List<String> systemAppNames = new ArrayList<String>();  
            for (Application application : 
                ((Applications)apps_w).getModules(Application.class)) {
                if (application.getObjectType().startsWith(
                    "system-")) {
                    ((Applications)apps_w).getModules().remove(application);
                    systemAppNames.add(application.getName());
                }
            }
 
            List<String> allTargets = domain.getAllTargets();
            for (String target : allTargets) {
                Server servr = domain.getServerNamed(target);
                if (servr != null) {
                    ConfigBeanProxy servr_w = t.enroll(servr);
                    List<ApplicationRef> appRefs = servr.getApplicationRef();
                    for (ApplicationRef appRef : appRefs) {
                        if (systemAppNames.contains(appRef.getRef())) {
                            ((Server)servr_w).getApplicationRef().remove(appRef);
                        }
                    }
                    continue;
                }

                Cluster cluster = domain.getClusterNamed(target);
                if (cluster != null) {
                    ConfigBeanProxy cluster_w = t.enroll(cluster);
                    // remove the application-ref from cluster
                    List<ApplicationRef> appRefs = cluster.getApplicationRef();
                    for (ApplicationRef appRef : appRefs) {
                        if (systemAppNames.contains(appRef.getRef())) {
                            ((Cluster)cluster_w).getApplicationRef().remove(appRef);
                        }
                    }
                    // remove the application-ref from cluster 
                    // instances                
                    for (Server svr : cluster.getInstances() ) {
                        ConfigBeanProxy svr_w = t.enroll(svr);
                        List<ApplicationRef> appRefs2 = svr.getApplicationRef();
                        for (ApplicationRef appRef2 : appRefs2) {
                            if (systemAppNames.contains(appRef2.getRef())) {
                                ((Server)svr_w).getApplicationRef().remove(appRef2);
                            }
                        }
                    }
                }
            }

            // 3. add a new empty system-apps element
            // for v3 system apps
            ConfigBeanProxy domain_w = t.enroll(domain);
            SystemApplications systemApps = domain_w.createChild(
                SystemApplications.class);
            ((Domain)domain_w).setSystemApplications(systemApps);

        } catch(TransactionFailure tf) {
            t.rollback();
            Logger.getAnonymousLogger().log(Level.SEVERE, "Failure while upgrading application", tf);
            throw new RuntimeException(tf);
        } catch (Exception e) {
            t.rollback();
            throw new RuntimeException(e);
        }

        try {
            t.commit();
        } catch (RetryableException e) {
            t.rollback();
        } catch (TransactionFailure e) {
            t.rollback();
            throw new RuntimeException(e);
        }
    }

    private String getLocationAsURIString(String location) {
        File appFile = new File(location);
        return appFile.toURI().toString();
    }
}
