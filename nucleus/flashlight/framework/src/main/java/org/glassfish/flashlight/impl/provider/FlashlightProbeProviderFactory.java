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
// Portions Copyright [2019] Payara Foundation and/or affiliates

package org.glassfish.flashlight.impl.provider;

import org.glassfish.external.probe.provider.annotations.*;
import org.glassfish.flashlight.xml.ProbeProviderStaxParser;
import com.sun.enterprise.config.serverbeans.MonitoringService;
import com.sun.enterprise.util.ObjectAnalyzer;

import fish.payara.monitoring.collect.MonitoringDataCollector;
import fish.payara.monitoring.collect.MonitoringDataSource;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;
import org.glassfish.api.monitoring.DTraceContract;
import org.glassfish.flashlight.FlashlightLoggerInfo;
import static org.glassfish.flashlight.FlashlightLoggerInfo.*;
import org.glassfish.flashlight.FlashlightUtils;
import org.glassfish.flashlight.impl.client.FlashlightProbeClientMediator;
import org.glassfish.flashlight.provider.*;
import org.glassfish.flashlight.impl.core.*;
import org.glassfish.flashlight.provider.ProbeProviderFactory;
import org.jvnet.hk2.annotations.Service;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.hk2.api.PostConstruct;
import org.glassfish.hk2.api.ServiceLocator;

import javax.inject.Inject;

/**
 * @author Mahesh Kannan
 * @author Byron Nevins
 * @author Prashanth Abbagani
 */
@Service
public class FlashlightProbeProviderFactory
        implements ProbeProviderFactory, PostConstruct, MonitoringDataSource {
    @Inject
    //Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    MonitoringService monitoringServiceConfig;

    @Inject
    ProbeProviderEventManager ppem;

    @Inject
    ServiceLocator habitat;

    private List<ProbeProviderEventListener> listeners = new ArrayList<ProbeProviderEventListener>();

    private final static Set<FlashlightProbeProvider> allProbeProviders = new HashSet<FlashlightProbeProvider>();
    private static final Logger logger = FlashlightLoggerInfo.getLogger();
    private static final ResourceBundle rb = logger.getResourceBundle();

    private static final HashMap<String, Class> primTypes = new HashMap<String, Class>() {
        {
            put("int",Integer.TYPE);
            put("byte",Byte.TYPE);
            put("char",Character.TYPE);
            put("short",Short.TYPE);
            put("long",Long.TYPE);
            put("float",Float.TYPE);
            put("double",Double.TYPE);
            put("boolean",Boolean.TYPE);
            put("void",Void.TYPE);
        }
    };

    @Override
    public void collect(MonitoringDataCollector collector) {
        MonitoringDataCollector flashlightCollector = collector.in("flashlight");
        flashlightCollector.collectNonZero("providers", allProbeProviders.size());
        flashlightCollector.collectNonZero("listeners", listeners.size());
        for (FlashlightProbeProvider provider : allProbeProviders) {
            MonitoringDataCollector providerCollector = flashlightCollector.tag("provider", provider.getProbeProviderName());
            providerCollector.collectNonZero("probes", provider.getProbes().size());
            for (FlashlightProbe probe : provider.getProbes()) {
                providerCollector.entity(probe.getProbeName())
                    .collect("enabled", probe.isEnabled())
                    .collect("invokers", probe.getInvokerCount());
            }
        }
    }

    public void postConstruct() {
        FlashlightUtils.initialize(habitat, monitoringServiceConfig);
    }

    public void dtraceEnabledChanged(boolean newValue) {
        FlashlightUtils.setDTraceEnabled(newValue);

        // if true->false we just need to set the enabled flag
        // DTrace invoker will simply not call the already-built
        // DTrace objects

        if(newValue == false)
            return;

        // if it still is not available -- return.  E.g. they set the flag
        // but they don't have the native libs

        if(!FlashlightUtils.isDtraceAvailable())
            return;

        // if false-> true we might need to create all the DTrace classes.  It
        // depends on whether the server started up with it true, etc.
        // loop through all the Providers -- if any don't have DTrace then
        // instrument with DTrace
        Collection<FlashlightProbeProvider> pps = ProbeProviderRegistry.getInstance().getAllProbeProviders();

        for(FlashlightProbeProvider pp : pps) {
            if(!pp.isDTraceInstrumented()) {
                handleDTrace(pp);
            }
        }
    }

    public void monitoringEnabledChanged(boolean newValue) {
        FlashlightUtils.setMonitoringEnabled(newValue);

        // if monitoring-enabled is going false -> true AND
        // dtrace-enabled is true -- then we need to do something
        // o/w we don't have to do anything

        if(newValue == true && FlashlightUtils.isDtraceEnabled())
            dtraceEnabledChanged(true);
    }

    public <T> T getProbeProvider(Class<T> providerClazz)
            throws InstantiationException, IllegalAccessException {
        return getProbeProvider(providerClazz, null);
    }

    public <T> T getProbeProvider(Class<T> providerClazz, String invokerId)
            throws InstantiationException, IllegalAccessException {

        if (providerClazz == null) {
            throw new NullPointerException("providerClazz cannot be null");
        }

        ProbeProvider provAnn = providerClazz.getAnnotation(ProbeProvider.class);

        String moduleProviderName = null;
        String moduleName = null;
        String probeProviderName = null;

        if (provAnn != null) {
            moduleProviderName = provAnn.moduleProviderName();
            moduleName = provAnn.moduleName();
            probeProviderName = provAnn.probeProviderName();
        }

        if (moduleProviderName == null) {
            moduleProviderName = providerClazz.getName();
        }

        if (moduleName == null) {
            moduleName = providerClazz.getName();
        }

        if (probeProviderName == null) {
            probeProviderName = providerClazz.getSimpleName();
        }

        if (isValidString(moduleProviderName)
                && isValidString(moduleName)
                && isValidString(probeProviderName)) {
            return getProbeProvider(moduleProviderName, moduleName,
                    probeProviderName,
                    invokerId, providerClazz);
        } else {
            logger.log(Level.WARNING,
                    INVALID_PROBE_PROVIDER, new Object[]{providerClazz.getName()});
            return null;
        }
    }

    public <T> T getProbeProvider(String moduleName, String providerName, String appName,
                                  Class<T> clazz)
            throws InstantiationException, IllegalAccessException {

        return getProbeProvider(moduleName, providerName, appName, null, clazz);
    }

    public <T> T getProbeProvider(String moduleProviderName, String moduleName,
    		String probeProviderName, String invokerId,
    		Class<T> providerClazz)
            throws InstantiationException, IllegalAccessException {

        String origProbeProviderName = probeProviderName;
        Class<T> oldProviderClazz = providerClazz;

        ProbeProviderRegistry ppRegistry = ProbeProviderRegistry.getInstance();
        FlashlightProbeProvider genericProvider = null;
        if (invokerId != null) {
            getProbeProvider( moduleProviderName,  moduleName,
    		 probeProviderName, null, providerClazz);
            genericProvider = new FlashlightProbeProvider(
                moduleProviderName, moduleName, probeProviderName, providerClazz);
            genericProvider = ppRegistry.getProbeProvider(genericProvider);

            invokerId = FlashlightUtils.getUniqueInvokerId(invokerId);
            probeProviderName += invokerId;
            try {
                providerClazz = getGeneratedProbeProviderClass(oldProviderClazz, invokerId);

            } catch (Exception ex) {
                //TODO
                providerClazz = oldProviderClazz;
            }
        }
        FlashlightProbeProvider provider = new FlashlightProbeProvider(
                moduleProviderName, moduleName, probeProviderName, providerClazz);
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("ModuleProviderName= " + moduleProviderName + " \tModule= " + moduleName
                + "\tProbeProviderName= " + probeProviderName + "\tProviderClazz= " + providerClazz.toString());
        }

        // IT 10269 -- silently return a fresh instance if it is already registered
        // don't waste time -- return right away...

        FlashlightProbeProvider alreadyExists = ppRegistry.getProbeProvider(provider);

        if(alreadyExists != null) {
            T inst = (T) alreadyExists.getProviderClass().newInstance();
            notifyListenersOnAdd(moduleProviderName, moduleName,
                    probeProviderName, invokerId, providerClazz, inst);
            return inst;
        }

        List<Method> methods = FlashlightUtils.getProbeMethods(providerClazz);

        // If none of the method are annotated with @Probe, then register all
        // public methods as probe providers
        boolean isProbeAnnotationPresent = false;

        for (Method m : methods) {
            Probe pnameAnn = m.getAnnotation(Probe.class);
            boolean self = false;
            boolean hidden = false;
            String probeName = m.getName();
            if (pnameAnn != null) {
                isProbeAnnotationPresent = true;
                self = pnameAnn.self();
                hidden = pnameAnn.hidden();
                if (pnameAnn.name() != null && !pnameAnn.name().isEmpty()) {
                    probeName = pnameAnn.name();
                }

                createProbe(origProbeProviderName, genericProvider, provider,
                        probeName, self, hidden, m, moduleProviderName, moduleName, probeProviderName,
                        invokerId, providerClazz, pnameAnn.stateful(), pnameAnn.statefulReturn(),
                        pnameAnn.statefulException(), pnameAnn.profileNames());

            }
        }

        if (!isProbeAnnotationPresent) {  //Let's do all public methods
            for (Method m : providerClazz.getDeclaredMethods()) {
                String methodName = m.getName();

                createProbe(origProbeProviderName, genericProvider, provider,
                        methodName, false, false, m, moduleProviderName, moduleName, probeProviderName,
                        invokerId, providerClazz,
                        false, false, false, null); // Not stateful and no profile names for these
            }
        }

        handleDTrace(provider);

        Class<T> tClazz = providerClazz;

        int mod = providerClazz.getModifiers();
        if (Modifier.isAbstract(mod)) {

            String generatedClassName = provider.getModuleProviderName() +
                    "_Flashlight_" + provider.getModuleName() + "_" + "Probe_" +
                    ((provider.getProbeProviderName() == null) ? providerClazz.getName() : provider.getProbeProviderName());
            generatedClassName = providerClazz.getName() + "_" + generatedClassName;

            try {
                tClazz = (Class<T>) (providerClazz.getClassLoader()).loadClass(generatedClassName);
                //System.out.println ("Reusing the Generated class");
                T inst = tClazz.newInstance();
                notifyListenersOnAdd(moduleProviderName, moduleName,
                        probeProviderName, invokerId, providerClazz, inst);
                return inst;
            } catch (ClassNotFoundException cnfEx) {
                //Ignore
            }

            ProviderImplGenerator gen = new ProviderImplGenerator();
            generatedClassName = gen.defineClass(provider, providerClazz);

            try {
                tClazz = (Class<T>) providerClazz.getClassLoader().loadClass(generatedClassName);
            } catch (ClassNotFoundException cnfEx) {
                throw new RuntimeException(cnfEx);
            }
        }

        ppRegistry.getInstance().registerProbeProvider(
                provider, tClazz);

        T inst = tClazz.newInstance();
        notifyListenersOnAdd(moduleProviderName, moduleName,
                probeProviderName, invokerId, providerClazz, inst);
        return inst;
    }

    private void createProbe(String origProbeProviderName, FlashlightProbeProvider genericProvider,
            FlashlightProbeProvider provider, String probeName, boolean self, boolean hidden,
            Method m, String moduleProviderName, String moduleName,
    		String probeProviderName, String invokerId,
    		Class providerClazz, boolean stateful, boolean statefulReturn,
    		boolean statefulException, String profileNames) {

            String[] probeParamNames = FlashlightUtils.getParamNames(m);
            FlashlightProbe probe = ProbeFactory.createProbe(
                    providerClazz, moduleProviderName, moduleName, probeProviderName, probeName,
                    probeParamNames, m.getParameterTypes(), self, hidden,
                    stateful, statefulReturn, statefulException, splitProfileNames(profileNames));
            probe.setProviderJavaMethodName(m.getName());
            probe.setProbeMethod(m);
            provider.addProbe(probe);

            if (invokerId != null) {
                if (genericProvider != null) {
                    String probeDescriptor = FlashlightProbe.getProbeDesc(moduleProviderName, moduleName, origProbeProviderName, probeName);
                    if (probeDescriptor != null) {
                        FlashlightProbe fp = genericProvider.getProbe(probeDescriptor);
                        if (fp != null) {
                             probe.setParent(fp);
                        }
                    }

                }
            }



    }

    public void unregisterProbeProvider(Object probeProvider) {
        try {
            ProbeProviderRegistry ppRegistry = ProbeProviderRegistry.getInstance();
            FlashlightProbeProvider fProbeProvider =
                    ppRegistry.getProbeProvider(probeProvider.getClass());
            ProbeRegistry probeRegistry = ProbeRegistry.getInstance();
            for (FlashlightProbe probe : fProbeProvider.getProbes()) {
                probeRegistry.unregisterProbe(probe);
            }
            ppRegistry.unregisterProbeProvider(probeProvider);
        } catch (Throwable t) {
            if (logger.isLoggable(Level.WARNING))
                logger.log(Level.WARNING, UNREGISTER_PROBE_PROVIDER_EXCEPTION, t);
        }
    }

    private <T> Class<T> getGeneratedProbeProviderClass(Class<T> oldProviderClazz, String invokerId) {
        Class<T> genClazz = null;

        try {
            ProviderSubClassImplGenerator gen = new ProviderSubClassImplGenerator(
                    oldProviderClazz, invokerId);

            genClazz = gen.generateAndDefineClass(oldProviderClazz, invokerId);

            //System.out.println("** Loaded generated provider: " + genClazz.getName());
            return genClazz;
        } catch (Throwable cnfEx) {
            throw new RuntimeException(cnfEx);
        }
    }

    public void processXMLProbeProviders(ClassLoader cl, String xml, boolean inBundle) {
        if (logger.isLoggable(Level.FINE))
            logger.fine("processProbeProviderXML for " + xml);
        try (InputStream is = inBundle ? cl.getResourceAsStream(xml) : new FileInputStream(xml)) {
            if (logger.isLoggable(Level.FINE))
                logger.fine("InputStream = " + is);
            ProbeProviderStaxParser providerXMLParser = new ProbeProviderStaxParser(is);
            List<org.glassfish.flashlight.xml.Provider> providers = providerXMLParser.getProviders();
            for (org.glassfish.flashlight.xml.Provider provider : providers) {
                if (logger.isLoggable(Level.FINE))
                    logger.fine(provider.toString());
                registerProvider(cl, provider);
            }
        } catch (Exception e) {
            Object params[] = {xml, e };
            logger.log(Level.SEVERE, CANNOT_PROCESS_XML_PROBE_PROVIDER, params);
        }

    }

    private <T> void notifyListenersOnAdd(String moduleProviderName, String moduleName,
                String probeProviderName, String invokerId,
                Class<T> providerClazz, T provider) {
        for (ProbeProviderEventListener listener : listeners) {
            listener.probeProviderAdded(moduleProviderName, moduleName,
                probeProviderName, invokerId, providerClazz, provider);
        }
    }

    @Override
    public void addProbeProviderEventListener(ProbeProviderEventListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeProbeProviderEventListener(ProbeProviderEventListener listener) {
        listeners.remove(listener);
    }

    @Override
	public String toString() {
		return ObjectAnalyzer.toString(this);
	}


    private void handleDTrace(FlashlightProbeProvider provider) {
        // bnevins:  The way this works above (getProbeProvider()) is that every
        //**method** winds up added to the probe registry with an official ID.
        // I.e. a given provider-class generates possibly many probe objects
        // DTrace has a 1:1 correspondence between provider class and dtrace class imp
        // So we loop through all the probes and add the same DTrace impl object to
        // each probe.
        // We set the DTrace Method object inside the probe just this once to avoid
        // having to discover it anew over and over and over again at runtime...

		if(allHidden(provider))
			return;  // no need to waste time if they are all hidden

        DTraceContract dt = FlashlightUtils.getDtraceEngine();

        // is DTrace available and enabled?
        if(dt == null)
            return;

        // here is a way to do the same thing but you get the intermediate interface class
        //Class dtraceProviderInterface = dt.getInterface(provider);
        //Object dtraceProviderImpl = dt.getProvider(dtraceProviderInterface);

        Object dtraceProviderImpl = dt.getProvider(provider);

        // something is wrong with the provider class
        if(dtraceProviderImpl == null) {
            provider.setDTraceInstrumented(false);
            return;
        }

        provider.setDTraceInstrumented(true);

         Collection<FlashlightProbe> probes = provider.getProbes();
         boolean onlyHidden = true;

         for(FlashlightProbe probe : probes) {
             // if it is hidden -- do not hook-up a DTrace "listener"

             if(!probe.isHidden()) {
                 DTraceMethodFinder mf = new DTraceMethodFinder(probe, dtraceProviderImpl);
                 probe.setDTraceMethod(mf.matchMethod());
                 probe.setDTraceProviderImpl(dtraceProviderImpl);
                 onlyHidden = false;    // we have at least one!
             }
         }

         // if there are only hidden probes -- don't register the class

         if(!onlyHidden)
            FlashlightProbeClientMediator.getInstance().registerDTraceListener(provider);
    }



    private boolean allHidden(FlashlightProbeProvider provider) {
        for(FlashlightProbe probe : provider.getProbes())
            if(!probe.isHidden())
                return false;   // at least one is not hidden

        return true;
    }


    private void registerProvider(ClassLoader cl, org.glassfish.flashlight.xml.Provider provider) {

        String moduleProviderName = provider.getModuleProviderName();
        String moduleName = provider.getModuleName();
        String probeProviderName = provider.getProbeProviderName();
        String providerClass = provider.getProbeProviderClass();
        List<org.glassfish.flashlight.xml.XmlProbe> probes = provider.getProbes();
        Class<?> providerClazz = null;

        try {
            providerClazz = cl.loadClass(providerClass);
            if (logger.isLoggable(Level.FINE))
                logger.fine("providerClazz = " + providerClazz);
        } catch (Exception e) {
            if (logger.isLoggable(Level.FINE))
                logger.fine( " Could not load the class ( " + providerClazz +
                        " ) for the provider " + providerClass);
            e.printStackTrace();
        }
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("moduleProviderName = " + moduleProviderName);
            logger.fine("moduleName = " + moduleName);
            logger.fine("probeProviderName = " + probeProviderName);
            logger.fine("probeProviderClass = " + providerClass);
        }
        FlashlightProbeProvider flProvider = new FlashlightProbeProvider(
            		moduleProviderName, moduleName, probeProviderName, providerClazz);

        for (org.glassfish.flashlight.xml.XmlProbe probe : probes) {
            String probeName = probe.getProbeName();
            String probeMethod = probe.getProbeMethod();
            boolean hasSelf = probe.hasSelf();
            boolean isHidden = probe.isHidden();

            boolean errorParsingProbe = false;
            String[] probeParams = new String[probe.getProbeParams().size()];
            Class<?>[] paramTypes = new Class[probe.getProbeParams().size()];

            int i = 0;
            for (org.glassfish.flashlight.xml.XmlProbeParam param : probe.getProbeParams()) {
                probeParams[i] = param.getName();
                if (logger.isLoggable(Level.FINE))
                    logger.fine("          probeParam[" + i + "] = " + probeParams[i]);
                paramTypes[i] = getParamType(cl, param.getType());

                if (paramTypes[i] == null) {
                    // Lets not create a probe if we see a problem with the
                    // paramType resolution
                    errorParsingProbe = true;
                    logger.log(Level.SEVERE, CANNOT_RESOLVE_PROBE_PARAM_TYPES_FOR_PROBE, new Object[] {probeName});
                    // stop parsing anymore probe params
                    break;
                }

                i++;
            }
            if (errorParsingProbe) {
                //reset
                errorParsingProbe = false;
                // continue for the next probe
                continue;
            }
            FlashlightProbe flProbe = ProbeFactory.createProbe( providerClazz,
                    moduleProviderName, moduleName, probeProviderName, probeName,
                    probeParams, paramTypes, hasSelf, isHidden,
                    probe.getStateful(), probe.getStatefulReturn(), probe.getStatefulException(),
                    splitProfileNames(probe.getProfileNames()));
            flProbe.setProviderJavaMethodName(probeMethod);
            if (logger.isLoggable(Level.FINE))
                logger.fine(" Constructed probe = " + flProbe.toString());
            flProvider.addProbe(flProbe);
        }
        if (flProvider.getProbes().size() == 0)
            return;

        handleDTrace(flProvider);
        allProbeProviders.add(flProvider);
        ProbeProviderRegistry.getInstance().registerProbeProvider(
                flProvider, providerClazz);
        if (logger.isLoggable(Level.FINE))
            logger.fine (" Provider registered successfully - " + probeProviderName);
   }


    private Class<?> getParamType(ClassLoader cl, String paramTypeStr) {
        Class<?> paramType = null;

        try {
            // Lets see if this is a primitive type
            Class primType = primTypes.get(paramTypeStr);
            if (primType != null) {
                return primType;
            }
            //Not a primitive type, lets try to load it as is
            paramType = cl.loadClass(paramTypeStr);

        } catch (ClassNotFoundException ex) {
            try {
                // Not a primitive or the actual type, maybe its one of the java.lang.* types
                // try to prepend java.lang. to the given class
                paramType = cl.loadClass("java.lang." + paramTypeStr);
            } catch (Exception e) {
                Object params[] = {paramTypeStr, e};
                logger.log(Level.SEVERE, CANNOT_RESOLVE_PROBE_PARAM_TYPES, params);
            }
        }

        if (logger.isLoggable(Level.FINE))
            logger.fine("          paramType = " + paramType);

        return paramType;

    }

    private boolean isValidString(String str) {
        if ((str != null) && (str.length() > 0)){
            return true;
        }
        return false;
    }
    
    private String [] splitProfileNames(String profileNamesAll) {
        if (profileNamesAll == null)
            return null;
        String [] profileNames = profileNamesAll.split(",");
        for (int i=0; i<profileNames.length; i++)
            profileNames[i] = (profileNames[i] == null ? null : profileNames[i].trim());
        return profileNames;
    }
}
