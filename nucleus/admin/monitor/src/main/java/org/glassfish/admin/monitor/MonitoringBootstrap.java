/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2013 Oracle and/or its affiliates. All rights reserved.
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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.glassfish.admin.monitor;

import java.beans.PropertyChangeEvent;
import java.net.URISyntaxException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.external.probe.provider.StatsProviderInfo;
import org.glassfish.hk2.api.PostConstruct;
import org.glassfish.hk2.api.PreDestroy;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.runlevel.RunLevel;
import org.glassfish.external.probe.provider.StatsProviderManager;
import com.sun.enterprise.config.serverbeans.*;
import org.glassfish.flashlight.MonitoringRuntimeDataRegistry;

import org.jvnet.hk2.annotations.Optional;

import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.ConfigListener;
import org.jvnet.hk2.config.UnprocessedChangeEvents;

import com.sun.enterprise.module.Module;
import com.sun.enterprise.module.ModuleState;
import com.sun.enterprise.module.ModuleDefinition;
import com.sun.enterprise.module.ModulesRegistry;
import com.sun.enterprise.module.ModuleLifecycleListener;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.URI;
import java.util.jar.Manifest;
import java.util.jar.Attributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.StringTokenizer;

import org.glassfish.external.amx.AMXGlassfish;

import org.glassfish.api.event.Events;
import org.glassfish.api.event.EventListener;
import org.glassfish.api.event.EventTypes;
import org.glassfish.api.monitoring.ContainerMonitoring;
import org.glassfish.flashlight.client.ProbeClientMediator;
import org.glassfish.flashlight.impl.client.AgentAttacher;
import org.glassfish.flashlight.provider.ProbeProviderFactory;
import org.glassfish.flashlight.provider.ProbeProviderEventListener;
import org.glassfish.flashlight.provider.ProbeRegistry;
import org.glassfish.internal.api.InitRunLevel;
import org.glassfish.internal.api.LogManager;
import org.jvnet.hk2.config.Transactions;

import javax.inject.Inject;
import javax.inject.Named;
import static org.glassfish.admin.monitor.MLogger.*;


/**
 *
 * @author abbagani
 */
@Service
@RunLevel(InitRunLevel.VAL)
public class MonitoringBootstrap implements PostConstruct, PreDestroy, EventListener, ModuleLifecycleListener, ConfigListener {
    @SuppressWarnings("unused")
    @Inject @Optional
    private LogManager dependency0;  // The LogManager must come up prior to this service
    @Inject
    private MonitoringRuntimeDataRegistry mrdr;
    @Inject
    private ModulesRegistry registry;
    @Inject
    protected ProbeProviderFactory probeProviderFactory;
    @Inject
    protected ProbeClientMediator pcm;
    @Inject
    Events events;

    @Inject
    ServerEnvironment serverEnv;

    @Inject
    @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    @Optional
    MonitoringService monitoringService = null;
    @Inject
    private org.glassfish.flashlight.provider.ProbeRegistry probeRegistry;
    @Inject
    ServiceLocator habitat;
    @Inject
    Transactions transactions;

    //Don't inject ConfigBeans to avoid getting every event on them
    private Domain domain;


    Map<String,Module> map = Collections.synchronizedMap(new WeakHashMap<String,Module>());
    List<String> appList = Collections.synchronizedList(new ArrayList<String>());

    private static final String INSTALL_ROOT_URI_PROPERTY_NAME = "com.sun.aas.installRootURI";
    private static final Logger logger = getLogger();
    private static final String PROBE_PROVIDER_CLASS_NAMES = "probe-provider-class-names";
    private static final String PROBE_PROVIDER_XML_FILE_NAMES = "probe-provider-xml-file-names";
    private static final String DELIMITER = ",";
    private StatsProviderManagerDelegateImpl spmd;
    private boolean monitoringEnabled = false;
    private boolean hasDiscoveredXMLProviders = false;

    @Override
    public void postConstruct() {
        domain = habitat.getService(Domain.class);
        transactions.addListenerForType(ContainerMonitoring.class, this);
        transactions.addListenerForType(MonitoringService.class, this);
        transactions.addListenerForType(ModuleMonitoringLevels.class, this);

        // wbn: This value sticks for the life of the bootstrapping.  If the user changes it
        // somehow during bootstrapping we would have some problems so we just get the value
        // and run with it...

        boolean enableMonitoring = (monitoringService != null) ?
                Boolean.parseBoolean(monitoringService.getMonitoringEnabled())
                    && monitoringService.isAnyModuleOn()  :
                false;

        //Don't listen for any events and dont process any probeProviders or statsProviders (dont set delegate)
        if (enableMonitoring) {
            enableMonitoring(false);
        }
    }

    private void enableMonitoring(boolean isDiscoverXMLProbeProviders) {
        // Register as ModuleLifecycleListener
        events.register(this);

        enableMonitoringForProbeProviders(isDiscoverXMLProbeProviders);
        AgentAttacher.attachAgent();
        //Lets do the catch up for all the statsProviders (we might have ignored the module level changes earlier)
        if (spmd != null) {
            spmd.updateAllStatsProviders();
        }
        monitoringEnabled = true;
    }

    private void discoverProbeProviders() {
        // Iterate thru existing modules
        if (logger.isLoggable(Level.FINE))
            logger.log(Level.FINE, "Discovering the ProbeProviders");
        for (Module m : registry.getModules()) {
            if ((m.getState() == ModuleState.READY) || (m.getState() == ModuleState.RESOLVED)) {
                if (logger.isLoggable(Level.FINE))
                    logger.fine(" In (discoverProbeProviders) ModuleState - " + m.getState() + " : " + m.getName());
                verifyModule(m);
            }
        }

    }

    public void preDestroy() {
        //We need to do the cleanup for preventing errors from server starting in Embedded mode
        ProbeRegistry.cleanup();
        if (spmd != null) {
            spmd = new StatsProviderManagerDelegateImpl(pcm, probeRegistry, mrdr, domain, serverEnv.getInstanceName(),
                    monitoringService);
            StatsProviderManager.setStatsProviderManagerDelegate(spmd);
        }
    }

    public void event(Event event) {
        if (event.is(EventTypes.SERVER_READY)) {
            // Process the XMLProviders in lib/monitor dir. Should be the last thing to do in server startup.
            if (logger.isLoggable(Level.FINE))
                logger.log(Level.FINE, "Discovering the XML ProbeProviders from lib/monitor");
            discoverXMLProviders();
        }
    }

    public void setStatsProviderManagerDelegate() {
        // only run the code one time!
        if(spmd != null)
            return;

        //Set the StatsProviderManagerDelegate, so we can start processing the StatsProviders
        spmd = new StatsProviderManagerDelegateImpl(pcm, probeRegistry, mrdr, domain, serverEnv.getInstanceName(),
                monitoringService);
        StatsProviderManager.setStatsProviderManagerDelegate(spmd);
        StatsProviderUtil.setStatsProviderManagerDelegate(spmd);
        if (logger.isLoggable(Level.FINE))
            logger.fine(" StatsProviderManagerDelegate is assigned");

        // Register listener for AMX DomainRoot loaded
        final AMXGlassfish amxg = AMXGlassfish.DEFAULT;
        amxg.listenForDomainRoot(ManagementFactory.getPlatformMBeanServer(), spmd);
    }

    public void moduleResolved(Module module) {
        if (module == null) return;
        verifyModule(module);
    }

    public synchronized void moduleStarted(Module module) {
        if (module == null) return;
        verifyModule(module);
    }

    private synchronized void verifyModule(Module module) {
        if (module == null) return;
        String str = module.getName();
        if (!map.containsKey(str)) {
            map.put(str, module);
            addProvider(module);
        }
    }

    /**
     * An application that has probes can be registered.
     * @param appName application-name
     * @param appDir directory where application bits are present.
     * @param cl classloader that is used to load application files.
     */
    public synchronized void registerProbes(String appName, File appDir, ClassLoader cl) {
        if (appName == null) return;
        if (cl == null) {
            if (logger.isLoggable(Level.FINE)){
                logger.log(Level.FINE, "Null classloader passed for application : {0}", appName);
            }
            return;
        }
        if (!appList.contains(appName)) {
            appList.add(appName);
            addProvider(appDir, cl);
        }
    }

    // noop to satisfy interface
    @Override
    public synchronized void moduleStopped(Module module) {
    }

    // noop to satisfy interface
    @Override
    public void moduleInstalled(Module module) {
    }

    // noop to satisfy interface
    @Override
    public void moduleUpdated(Module module) {
    }

    private void addProvider(Module module) {
        if (logger.isLoggable(Level.FINE))
            logger.fine(" Adding the Provider - verified the module");
        ClassLoader mcl = module.getClassLoader();
        //get manifest entries and process
        ModuleDefinition md = module.getModuleDefinition();
        Manifest mf = null;
        if (md != null) {
            mf = md.getManifest();
        }
        if (mf != null) {
            processManifest(mf, mcl);
        }
        handleFutureStatsProviders();
    }

    private void addProvider(File appDir, ClassLoader classLoader) {
        //get manifest entries and process
        File manifestFile = new File(appDir, "META-INF" + File.separator + "MANIFEST.MF");
        String appDirPath = "";
        Manifest mf;
            try {
                appDirPath = appDir.getCanonicalPath();
                FileInputStream fis = new FileInputStream(manifestFile);
                mf = new Manifest(fis);
            } catch (IOException ex) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE,"Can''t access "+"META-INF{0}" + "MANIFEST.MF" + " for {1}",
                            new Object[]{File.separator, appDirPath});
                    logger.fine(ex.getLocalizedMessage());
                }
                return;
            }
            processManifest(mf, classLoader);

        handleFutureStatsProviders();
    }

    private void processManifest(Manifest mf, ClassLoader mcl) {
        if (mf != null) {
            Attributes attrs = mf.getMainAttributes();
            String cnames = null;
            String xnames = null;
            if (attrs != null) {
                cnames = attrs.getValue(PROBE_PROVIDER_CLASS_NAMES);
                if (cnames != null) {
                    if (logger.isLoggable(Level.FINE))
                        logger.fine("probe providers = " + cnames);
                    StringTokenizer st = new StringTokenizer(cnames, DELIMITER);
                    while (st.hasMoreTokens()) {
                        try {
                            if (mcl != null)
                                processProbeProviderClass(mcl.loadClass(st.nextToken().trim()));
                        } catch (Exception e) {
                            logger.log(Level.SEVERE, unableToLoadProbeProvider, e);
                        }
                    }
                }
                xnames = attrs.getValue(PROBE_PROVIDER_XML_FILE_NAMES);
                if (xnames != null) {
                    if (logger.isLoggable(Level.FINE))
                        logger.fine("xnames = " + xnames);
                    StringTokenizer st = new StringTokenizer(xnames, DELIMITER);
                    while (st.hasMoreTokens()) {
                        processProbeProviderXML(mcl, st.nextToken(), true);
                    }
                }
            }
        }
    }

    public void handleFutureStatsProviders() {
        // we just registered a Probe Provider
        // If there are any future items -- let's try to register them again.

        if(FutureStatsProviders.isEmpty())
            return; // Performance note -- this should be the case almost always

        List<StatsProviderInfo> removeList = new ArrayList<StatsProviderInfo>();
        Iterator<StatsProviderInfo> it = FutureStatsProviders.iterator();

        // the iterator does not allow the remove operation - thus the complexity!
        while(it.hasNext()) {
            StatsProviderInfo spInfo = it.next();
            try {
                spmd.tryToRegister(spInfo);
                removeList.add(spInfo);
            }
            catch(RuntimeException re) {
                // no probe registered yet...
            }
        }

        for(StatsProviderInfo spInfo : removeList) {
            FutureStatsProviders.remove(spInfo);
        }
    }

    private void discoverXMLProviders() {
        // Dont process if already discovered, Ideally we should do this whenever a new XML is dropped in lib/monitor
        if (hasDiscoveredXMLProviders)
            return;

        try {
            URI xmlProviderDirStr = new URI(System.getProperty(INSTALL_ROOT_URI_PROPERTY_NAME) + "/" + "lib" + "/" + "monitor");
            if (logger.isLoggable(Level.FINE))
                logger.fine("ProviderXML's Dir = " + xmlProviderDirStr.getPath());
            File xmlProviderDir = new File(xmlProviderDirStr.getPath());
            //File scriptFile = new File ("/space/GFV3_BLD/glassfish/domains/domain1/applications/scripts/InvokeJavaFromJavascript.js");
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("ProviderXML's Dir exists = " + xmlProviderDir.exists());
                logger.fine("ProviderXML's Dir path - " + xmlProviderDir.getAbsolutePath());
            }
            loadXMLProviders(xmlProviderDir);
            hasDiscoveredXMLProviders = true;
        } catch (URISyntaxException ex) {
            logger.log(Level.SEVERE, unableToProcessXMLProbeProvider, ex);
        }
    }

    private void loadXMLProviders(File xmlProvidersDir) {
        // Creates a filter which will return only xml files
        FilenameFilter filter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".xml");
            }
        };
        // Retrieves all the provider XML's
        File[] files = xmlProvidersDir.listFiles(filter);
        if (files == null)
            return;
        Map<String, File> providerMap = new HashMap();

        for (File file : files) {
            if (logger.isLoggable(Level.FINE))
                logger.fine("Found the provider xml - " + file.getAbsolutePath());
            int index = file.getName().indexOf("-:");
            if (index != -1) {
                String moduleName = file.getName().substring(0,index);
                providerMap.put(moduleName, file);
                if (logger.isLoggable(Level.FINE))
                    logger.fine(" The provider xml belongs to - \"" + moduleName + "\"");
                if (!map.containsKey(moduleName)) {
                    continue;
                }
                if (logger.isLoggable(Level.FINE))
                    logger.fine (" Module found (containsKey)");
                Module module = map.get(moduleName);

                if (module == null) {
                    logger.log(Level.SEVERE,
                                monitoringMissingModuleFromXmlProbeProviders,
                                        new Object[] {moduleName});
                } else {
                    ClassLoader mcl = module.getClassLoader();

                    if (logger.isLoggable(Level.FINE)) {
                        logger.fine("ModuleClassLoader = " + mcl);
                        logger.fine("XML File path = " + file.getAbsolutePath());
                    }
                    processProbeProviderXML(mcl, file.getAbsolutePath(), false);
                }
            }
        }
    }

    private void processProbeProviderClass(Class cls) {
        if (logger.isLoggable(Level.FINE))
            logger.fine("processProbeProviderClass for " + cls);
        try {

            probeProviderFactory.getProbeProvider(cls);

        } catch (InstantiationException ex) {
            logger.log(Level.SEVERE, unableToLoadProbeProvider, ex);
        } catch (IllegalAccessException ex) {
            logger.log(Level.SEVERE, unableToLoadProbeProvider, ex);
        }
    }

    private void processProbeProviderXML(ClassLoader mcl, String xname, boolean inBundle) {
        probeProviderFactory.processXMLProbeProviders(mcl, xname, inBundle);
    }

    /*public void event(Event event) {
        if (event.name().equals(EventTypes.PREPARE_SHUTDOWN_NAME)) {
            spmd.unregisterAll();
        }
    }*/

    public UnprocessedChangeEvents changed(PropertyChangeEvent[] propertyChangeEvents) {
        if (logger.isLoggable(Level.FINE))
            logger.fine(" spmd = " + spmd);
        StatsProviderRegistry spr = (spmd == null) ? null : spmd.getStatsProviderRegistry();
        if (logger.isLoggable(Level.FINE))
            logger.fine("spr = " + spr);
        for (PropertyChangeEvent event : propertyChangeEvents) {
            // let's get out of here ASAP if it is not our stuff!!
            if(event == null)
                continue;

            if (!isCurrentInstanceMatchingTarget(event)) {
                continue;
            }
            
            String propName = event.getPropertyName();
            Object oldVal = event.getOldValue();
            Object newVal = event.getNewValue();

            if(newVal == null || newVal.equals(oldVal))
                continue;   // no change!!

            if(!ok(propName))
                continue;
            String level_change_mesg = "Level change event received, {0} New Level = {1}, Old Level = {2}";
            if (event.getSource() instanceof ModuleMonitoringLevels) {
                String newEnabled = newVal.toString().toUpperCase(Locale.ENGLISH);
                String oldEnabled = (oldVal == null) ? "OFF" : oldVal.toString().toUpperCase(Locale.ENGLISH);
                if (logger.isLoggable(Level.FINE))
                    logger.log(Level.FINE, level_change_mesg,
                                new Object[]{propName, newEnabled, oldEnabled});
                if (!newEnabled.equals(oldEnabled)) {
                    handleLevelChange(propName, newEnabled);
                }
            }
            else if (event.getSource() instanceof ContainerMonitoring) {
                ContainerMonitoring cm = (ContainerMonitoring)event.getSource();

                String newEnabled = newVal.toString().toUpperCase(Locale.ENGLISH);
                String oldEnabled = (oldVal == null) ? "OFF" : oldVal.toString().toUpperCase(Locale.ENGLISH);
                if (logger.isLoggable(Level.FINE))
                    logger.log(Level.FINE, level_change_mesg,
                                new Object[]{propName, newEnabled, oldEnabled});
                if (!newEnabled.equals(oldEnabled)) {
                    handleLevelChange(cm.getName(), newEnabled);
                }
            }
            else if(event.getSource() instanceof MonitoringService) {
                // we don't want to get fooled because config allows ANY string.
                // e.g. "false" --> "foo" --> "fals" are all NOT changes!
                // so we convert to boolean and then compare...
                boolean newEnabled = Boolean.parseBoolean(newVal.toString());
                boolean oldEnabled = (oldVal == null) ? !newEnabled : Boolean.parseBoolean(oldVal.toString());
                if (logger.isLoggable(Level.FINE))
                    logger.log(Level.FINE, level_change_mesg,
                                new Object[]{propName, newEnabled, oldEnabled});

                if(newEnabled != oldEnabled) {
                    handleServiceChange(spr, propName, newEnabled);
                }
            }
        }

       return null;
    }

    private boolean isCurrentInstanceMatchingTarget(PropertyChangeEvent event) {
        // DAS receive all the events, so we need to figure out 
        // whether we should take action on DAS depending on the event.

        if(serverEnv.isInstance()) {
            return true;
        } 

        ConfigBeanProxy proxy = (ConfigBeanProxy)(event.getSource());
        while(proxy != null && !(proxy instanceof Config)) {
            proxy = proxy.getParent();
        }
        if (proxy != null) {
            Config config = (Config)proxy;
            return config.isDas();
        }

        return false;
    }
    
    private void handleLevelChange(String propName, String enabledStr) {
        if (logger.isLoggable(Level.FINE))
            logger.fine("In handleLevelChange(), spmd = " + spmd + "  Enabled="+enabledStr);
        if(!ok(propName))
            return;

        if (!monitoringEnabled && !"OFF".equals(enabledStr)) {
            enableMonitoring(true);
        }

        if(spmd == null)
            return; // nothing to do!

        if (parseLevelsBoolean(enabledStr)) {
            if (logger.isLoggable(Level.FINE))
                logger.log(Level.FINE,
                        "Enabling {0} monitoring to {1}", new Object[] {propName, enabledStr});
            try {
                spmd.enableStatsProviders(propName);
            } catch(RuntimeException rte) {
                logger.log(Level.INFO, UNHANDLED_EXCEPTION_INFO, rte);
            }
        } else {
            if (logger.isLoggable(Level.FINE))
                logger.log(Level.FINE,
                        "Disabling {0} monitoring", propName);
            spmd.disableStatsProviders(propName);
        }
    }

    private void handleServiceChange(StatsProviderRegistry spr, String propName, boolean enabled) {
        if(!ok(propName))
            return;

        if (propName.equals("mbean-enabled")) {
            if(spr == null) // required!
                return;

            if(enabled) {
                logger.log(Level.INFO, mbeanEnabled);
                spmd.registerAllGmbal();
            } else {
                logger.log(Level.INFO, mbeanDisabled);
                spmd.unregisterAllGmbal();
            }
        }
        else if(propName.equals("dtrace-enabled")) {
            logger.log(Level.INFO,dtraceEnabled);
            probeProviderFactory.dtraceEnabledChanged(enabled);
        }
        else if(propName.equals("monitoring-enabled")) {
            //This we do it so we can (un)expose probes as DTrace
            probeProviderFactory.monitoringEnabledChanged(enabled);

            if(enabled) {
                logger.log(Level.INFO,monitoringEnabledLogMsg);
                enableMonitoring(true);
            } else { // if disabled
                logger.log(Level.INFO,monitoringDisabledLogMsg);
                disableMonitoringForProbeProviders();
                if (spmd != null) {
                    spmd.disableAllStatsProviders();
                }
            }
        }
    }

    private void enableMonitoringForProbeProviders(boolean isDiscoverXMLProviders) {
        //Process all ProbeProviders from modules loaded
        discoverProbeProviders();
        //Start listening to any new Modules that are coming in now
        registry.register(this);
        //Don't do this the first time, since we need to wait till the server starts
        // We should try to do this in a seperate thread, as we dont want to get held up in server start
        if (isDiscoverXMLProviders) {
            //Process all XMLProbeProviders from lib/monitor directory
            discoverXMLProviders();
        }
        //Start registering the cached StatsProviders and also those that are coming in new
        setStatsProviderManagerDelegate();
        //register probeprocee listener
        probeProviderFactory.addProbeProviderEventListener(new ProcessProbes());
    }

    private void disableMonitoringForProbeProviders() {
        //Cannot do a whole lot here. The providers that are registered will remain registered.
        //Just disable the StatsProviders, so you remove the listening overhead
        registry.unregister(this);
    }

    private boolean ok(String s) {
        return s != null && s.length() > 0;
    }

    private boolean parseLevelsBoolean(String s) {
        if (ok(s) && s.equals("OFF"))
            return false;

        return true;
    }

    private class ProcessProbes implements ProbeProviderEventListener {
        public <T> void probeProviderAdded(String moduleProviderName, String moduleName,
                String probeProviderName, String invokerId, Class<T> providerClazz, T provider) {
            handleFutureStatsProviders();
        }
    }
}
