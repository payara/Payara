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
// Portions Copyright [2016-2019] [Payara Foundation and/or its affiliates]

package org.glassfish.internal.data;

import com.sun.enterprise.config.serverbeans.Engine;
import com.sun.enterprise.config.serverbeans.Module;
import com.sun.enterprise.config.serverbeans.ServerTags;
import com.sun.enterprise.util.ExceptionUtil;
import java.beans.PropertyVetoException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.api.container.Container;
import org.glassfish.api.container.Sniffer;
import org.glassfish.api.deployment.ApplicationContainer;
import org.glassfish.api.deployment.DeployCommandParameters;
import org.glassfish.api.deployment.Deployer;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.api.event.EventListener.Event;
import org.glassfish.api.event.Events;
import org.glassfish.internal.deployment.Deployment;
import org.glassfish.internal.deployment.DeploymentTracing;
import org.glassfish.internal.deployment.ExtendedDeploymentContext;
import org.glassfish.internal.deployment.analysis.DeploymentSpan;
import org.glassfish.internal.deployment.analysis.StructuredDeploymentTracing;
import org.glassfish.internal.deployment.analysis.TraceContext;
import org.jvnet.hk2.config.TransactionFailure;
import org.jvnet.hk2.config.types.Property;

/**
 * Each module of an application has an associated module info instance keeping
 * the list of engines in which that module is loaded.
 *
 * @author Jerome Dochez
 */
public class ModuleInfo {

    protected Set<EngineRef> engines = new LinkedHashSet<EngineRef>();

    // The reversed engines contain the same elements as engines but just in
    // reversed order, they are used when stopping/unloading the module.
    // The engines should be stopped/unloaded in the reverse order of what
    // they were originally loaded/started.
    protected LinkedList<EngineRef> reversedEngines = new LinkedList<EngineRef>();

    final protected Map<Class<? extends Object>, Object> metaData = new HashMap<Class<? extends Object>, Object>();

    protected final String name;
    protected final Events events;
    private Properties moduleProps;
    private boolean started=false;
    private ClassLoader moduleClassLoader;
    private Set<ClassLoader> classLoaders = new HashSet<ClassLoader>();
    
  
    public ModuleInfo(final Events events, String name, Collection<EngineRef> refs, 
        Properties moduleProps) {
        this.name = name;
        this.events = events;
        for (EngineRef ref : refs) {
            engines.add(ref);
        }
        for (EngineRef ref : refs) {
            reversedEngines.addFirst(ref);
        }
        this.moduleProps = moduleProps;
    }

    public Set<EngineRef> getEngineRefs() {
        Set<EngineRef> copy = new LinkedHashSet<EngineRef>();
        copy.addAll(_getEngineRefs());
        return copy; 
    }

    protected Set<EngineRef> _getEngineRefs() {
        return engines;
    }

    public Set<ClassLoader> getClassLoaders() {
        return classLoaders;
    }
    
    public ClassLoader getModuleClassLoader() {
        return moduleClassLoader;
    }

    public void cleanClassLoaders() {
        classLoaders = null; 
        moduleClassLoader = null;
    }

    public void addMetaData(Object o) {
        metaData.put(o.getClass(), o);
    }

    public <T> T getMetaData(Class<T> c) {
        return c.cast(metaData.get(c));
    }

    public Object getMetaData(String className) {
        for (Entry<Class<? extends Object>, Object> entry : metaData.entrySet()) {
            if (entry.getKey().getName().equals(className)) {
                return entry.getValue();
            }
        }
        return null;
    }

    public String getName() {
        return name;
    }

    public Properties getModuleProps() {
        Properties props =  new Properties();
        if (moduleProps != null) {
            props.putAll(moduleProps);
        }
        return props;
    }


    /**
     * Returns the list of sniffers that participated in loaded this
     * application
     *
     * @return array of sniffer that loaded the application's module
     */
    public Collection<Sniffer> getSniffers() {
        List<Sniffer> sniffers = new ArrayList<Sniffer>();
        for (EngineRef engine : _getEngineRefs()) {
            sniffers.add(engine.getContainerInfo().getSniffer());
        }
        return sniffers;
    }

    public void load(ExtendedDeploymentContext context, ProgressTracker tracker) throws Exception {
        Logger logger = context.getLogger();
        context.setPhase(ExtendedDeploymentContext.Phase.LOAD);
        StructuredDeploymentTracing tracing = StructuredDeploymentTracing.load(context);

        moduleClassLoader = context.getClassLoader();

        Set<EngineRef> filteredEngines = new LinkedHashSet<EngineRef>();
        LinkedList<EngineRef> filteredReversedEngines = new LinkedList<EngineRef>();

        ClassLoader currentClassLoader  = Thread.currentThread().getContextClassLoader();
        try (DeploymentSpan span = tracing.startSpan(TraceContext.Level.MODULE, name, DeploymentTracing.AppStage.LOAD)){
            Thread.currentThread().setContextClassLoader(context.getClassLoader());
            for (EngineRef engine : _getEngineRefs()) {
    
                final EngineInfo engineInfo = engine.getContainerInfo();

                // get the container.
                Deployer deployer = engineInfo.getDeployer();

                try (DeploymentSpan containerSpan = tracing.startSpan(TraceContext.Level.CONTAINER, engineInfo.getSniffer().getModuleType(), DeploymentTracing.AppStage.LOAD)) {
                   ApplicationContainer appCtr = deployer.load(engineInfo.getContainer(), context);
                   if (appCtr==null) {
                       String msg = "Cannot load application in " + engineInfo.getContainer().getName() + " container";
                       logger.fine(msg);
                       continue;
                   }
                   engine.load(context, tracker);
                   engine.setApplicationContainer(appCtr);
                   filteredEngines.add(engine);
                   filteredReversedEngines.addFirst(engine);
                } catch(Exception e) {
                    logger.log(Level.SEVERE, "Exception while invoking " + deployer.getClass() + " load method", e);
                    throw e;
                }
            }
            engines = filteredEngines;
            reversedEngines = filteredReversedEngines;

            if (events!=null) {
                events.send(new Event<ModuleInfo>(Deployment.MODULE_LOADED, this), false);
            }
        } finally {
            Thread.currentThread().setContextClassLoader(currentClassLoader);
        }

    }    

    /*
     * Returns the EngineRef for a particular container type
     * @param type the container type
     * @return the module info is this application as a module implemented with
     * the passed container type
     */
    public <T extends Container> EngineRef getEngineRefForContainer(Class<T> type) {
        for (EngineRef engine : _getEngineRefs()) {
            T container = null;
            try {
                container = type.cast(engine.getContainerInfo().getContainer());
            } catch (Exception e) {
                // ignore, wrong container
            }
            if (container!=null) {
                return engine;
            }
        }
        return null;
    }


    public synchronized void start(
        DeploymentContext context,
        ProgressTracker tracker) throws Exception {

        Logger logger = context.getLogger();

        if (started)
            return;
        
        ClassLoader currentClassLoader  = 
            Thread.currentThread().getContextClassLoader();
        StructuredDeploymentTracing tracing = StructuredDeploymentTracing.load(context);
        try (DeploymentSpan span = tracing.startSpan(TraceContext.Level.MODULE, getName(), DeploymentTracing.AppStage.START)) {
            Thread.currentThread().setContextClassLoader(context.getClassLoader());
            // registers all deployed items.
            for (EngineRef engine : _getEngineRefs()) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("starting " + engine.getContainerInfo().getSniffer().getModuleType());
                }

                try (DeploymentSpan innerSpan = tracing.startSpan(TraceContext.Level.CONTAINER,  engine.getContainerInfo().getSniffer().getModuleType(), DeploymentTracing.AppStage.START)){
                    if (!engine.start( context, tracker)) {
                        logger.log(Level.SEVERE, "Module not started " +  engine.getApplicationContainer().toString());
                        throw new Exception( "Module not started " +  engine.getApplicationContainer().toString());
                    }
                } catch(Exception e) { 
                    DeployCommandParameters dcp = context.getCommandParameters(DeployCommandParameters.class);
                    if(dcp.isSkipDSFailure() && ExceptionUtil.isDSFailure(e)){
                        logger.log(Level.WARNING, "Resource communication failure exception skipped while invoking " + engine.getApplicationContainer().getClass() + " start method", e);
                    } else {
                        logger.log(Level.SEVERE, "Exception while invoking " + engine.getApplicationContainer().getClass() + " start method", e);
                        throw e;
                    }
                }
            }
            started=true;
            if (events!=null) {
                DeploymentSpan innerSpan = tracing.startSpan(DeploymentTracing.AppStage.START_EVENTS, "Module");
                events.send(new Event<ModuleInfo>(Deployment.MODULE_STARTED, this), false);
                innerSpan.close();
            }
        } finally {
            Thread.currentThread().setContextClassLoader(currentClassLoader);
        }
    }
 
    public synchronized void stop(ExtendedDeploymentContext context, Logger logger) {

        if (!started)
            return;
        
        ClassLoader currentClassLoader  = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(moduleClassLoader);
            for (EngineRef module : reversedEngines) {
                try {
                    context.setClassLoader(moduleClassLoader);
                    module.stop(context);
                } catch(Exception e) {
                    logger.log(Level.SEVERE, "Cannot stop module " +
                        module.getContainerInfo().getSniffer().getModuleType(),e );
                }
            }
            started=false;
            if (events!=null) {
                events.send(new Event<ModuleInfo>(Deployment.MODULE_STOPPED, this), false);
            }
        } finally {
            Thread.currentThread().setContextClassLoader(currentClassLoader);
        }
    }

    public void unload(ExtendedDeploymentContext context) {

        Logger logger = context.getLogger();
        ClassLoader currentClassLoader  = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(moduleClassLoader);
            for (EngineRef engine : reversedEngines) {
                if (engine.getApplicationContainer()!=null && engine.getApplicationContainer().getClassLoader()!=null) {
                    classLoaders.add(engine.getApplicationContainer().getClassLoader());
                    try {
                        context.setClassLoader(moduleClassLoader);
                        engine.unload(context);
                    } catch(Throwable e) {
                        logger.log(Level.SEVERE, "Failed to unload from container type : " +
                            engine.getContainerInfo().getSniffer().getModuleType(), e);
                    }
                }
            }
            // add the module classloader to the predestroy list if it's not
            // already there
            if (classLoaders != null && moduleClassLoader != null) {
                classLoaders.add(moduleClassLoader);
            }
            if (events!=null) {
                events.send(new Event<ModuleInfo>(Deployment.MODULE_UNLOADED, this), false);
            }
        } finally {
            Thread.currentThread().setContextClassLoader(currentClassLoader);
            context.setClassLoader(null);
        }
    }

    public void clean(ExtendedDeploymentContext context) throws Exception {
        for (EngineRef ref : reversedEngines) {
            ref.clean(context);
        }
        if (events!=null) {
            events.send(new Event<DeploymentContext>(Deployment.MODULE_CLEANED,context), false);
        }        
        
    }

    public boolean suspend(Logger logger) {

        boolean isSuccess = true;

        for (EngineRef engine : reversedEngines) {
            try {
                engine.getApplicationContainer().suspend();
            } catch(Exception e) {
                isSuccess = false;
                logger.log(Level.SEVERE, "Error suspending module " +
                           engine.getContainerInfo().getSniffer().getModuleType(),e );
            }
        }

        return isSuccess;
    }

    public boolean resume(Logger logger) {

        boolean isSuccess = true;

        for (EngineRef module : _getEngineRefs()) {
            try {
                module.getApplicationContainer().resume();
            } catch(Exception e) {
                isSuccess = false;
                logger.log(Level.SEVERE, "Error resuming module " +
                           module.getContainerInfo().getSniffer().getModuleType(),e );
            }
        }

        return isSuccess;
    }

    /**
     * Saves its state to the configuration. this method must be called within a transaction
     * to the configured module instance.
     *
     * @param module the module being persisted
     */
    public void save(Module module) throws TransactionFailure, PropertyVetoException {
        // write out the module properties only for composite app
        if (Boolean.valueOf(moduleProps.getProperty(
            ServerTags.IS_COMPOSITE))) {
            moduleProps.remove(ServerTags.IS_COMPOSITE);
            for (Iterator itr = moduleProps.keySet().iterator(); 
                itr.hasNext();) {
                String propName = (String) itr.next();
                Property prop = module.createChild(Property.class);
                module.getProperty().add(prop);
                prop.setName(propName);
                prop.setValue(moduleProps.getProperty(propName));
            }
        }

        for (EngineRef ref : _getEngineRefs()) {
            Engine engine = module.createChild(Engine.class);
            module.getEngines().add(engine);
            ref.save(engine);
        }
    }
}
