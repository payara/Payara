/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.ejb.startup;

import com.sun.enterprise.util.LocalStringManagerImpl;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.ejb.Container;
import com.sun.ejb.ContainerFactory;
import com.sun.ejb.containers.AbstractSingletonContainer;
import com.sun.enterprise.deployment.Application;
import com.sun.enterprise.security.PolicyLoader;
import com.sun.logging.LogDomains;
import org.glassfish.api.deployment.ApplicationContainer;
import org.glassfish.api.deployment.ApplicationContext;
import org.glassfish.api.deployment.DeployCommandParameters;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.api.deployment.OpsParams;
import org.glassfish.api.deployment.UndeployCommandParameters;
import org.glassfish.ejb.deployment.descriptor.EjbBundleDescriptorImpl;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbSessionDescriptor;
import org.glassfish.internal.data.ApplicationInfo;
import org.glassfish.internal.data.ApplicationRegistry;
import org.glassfish.internal.deployment.ExtendedDeploymentContext;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceLocator;

/**
 * This class represents a logical collection of EJB components contained in one ejb-jar
 * or one .war.
 *
 * @author Mahesh Kannan
 */
@Service(name = "ejb")
@PerLookup
public class EjbApplication
        implements ApplicationContainer<Collection<EjbDescriptor>> {

    private static final Logger _logger =
                LogDomains.getLogger(EjbApplication.class, LogDomains.EJB_LOGGER);

    private EjbBundleDescriptorImpl ejbBundle;
    private Collection<EjbDescriptor> ejbs;
    private Collection<Container> containers = new ArrayList<Container>();
    private ClassLoader ejbAppClassLoader;
    private DeploymentContext dc;
    
    private ServiceLocator services;

    private SingletonLifeCycleManager singletonLCM;

    private PolicyLoader policyLoader;

    private boolean initializeInOrder;

    private volatile boolean started;

    private static final String CONTAINER_LIST_KEY = "org.glassfish.ejb.startup.EjbContainerList";

    private static final String EJB_APP_MARKED_AS_STARTED_STATUS = "org.glassfish.ejb.startup.EjbApplicationMarkedAsStarted";

    static final String KEEP_STATE = "org.glassfish.ejb.startup.keepstate";
    private static final LocalStringManagerImpl localStrings =
            new LocalStringManagerImpl(EjbApplication.class);

  public EjbApplication(
            EjbBundleDescriptorImpl bundle, DeploymentContext dc,
            ClassLoader cl, ServiceLocator services) {
        this.ejbBundle = bundle;
        this.ejbs = bundle.getEjbs();
        this.ejbAppClassLoader = cl;
        this.dc = dc;
        this.services = services;
        this.policyLoader = services.getService(PolicyLoader.class);
        Application app = ejbBundle.getApplication();
        initializeInOrder = (app != null) && (app.isInitializeInOrder());
    }
    
    public Collection<EjbDescriptor> getDescriptor() {
        return ejbs;
    }

    public EjbBundleDescriptorImpl getEjbBundleDescriptor() {
        return ejbBundle;
    }

    public boolean isStarted() {
        return started;
    }       // TODO handle singleton startup dependencies that refer to singletons in a different
            // module within the application

    void markAllContainersAsStarted() {
        for (Container container : containers) {
                container.setStartedState();
        }
    }

    public boolean start(ApplicationContext startupContext) throws Exception {
        started = true;

        if (! initializeInOrder) {
            Boolean alreadyMarked = dc.getTransientAppMetaData(EJB_APP_MARKED_AS_STARTED_STATUS, Boolean.class);
            if (! alreadyMarked.booleanValue()) {
                List<EjbApplication> ejbAppList = dc.getTransientAppMetaData(CONTAINER_LIST_KEY, List.class);
                for (EjbApplication app : ejbAppList) {
                    app.markAllContainersAsStarted();
                }
                dc.addTransientAppMetaData(EJB_APP_MARKED_AS_STARTED_STATUS, Boolean.TRUE);
            }
        }
        
        try {
            DeployCommandParameters params = ((DeploymentContext)startupContext).
                    getCommandParameters(DeployCommandParameters.class);

            for (Container container : containers) {
                container.startApplication(params.origin.isDeploy());
            }

            singletonLCM.doStartup(this);

        } catch(Exception e) {
            abortInitializationAfterException();
            throw e;
        }

        return true;
    }

    /**
     * Initial phase of continer initialization.  This creates the concrete container
     * instance for each EJB component, registers JNDI entries, etc.  However, no
     * EJB bean instances or invocations occur during this phase.  Those must be
     * delayed until start() is called.
     * @param startupContext
     * @return
     */
    boolean loadContainers(ApplicationContext startupContext) {

        DeploymentContext dc = (DeploymentContext) startupContext;

        String dcMapToken = "org.glassfish.ejb.startup.SingletonLCM";
        singletonLCM = dc.getTransientAppMetaData(dcMapToken, SingletonLifeCycleManager.class);
        if (singletonLCM == null) {
            singletonLCM = new SingletonLifeCycleManager(initializeInOrder);
            dc.addTransientAppMetaData(dcMapToken, singletonLCM);
        }

        if (! initializeInOrder) {
            dc.addTransientAppMetaData(EJB_APP_MARKED_AS_STARTED_STATUS, Boolean.FALSE);
            List<EjbApplication> ejbAppList = dc.getTransientAppMetaData(CONTAINER_LIST_KEY, List.class);
            if (ejbAppList == null) {
                ejbAppList = new ArrayList<EjbApplication>();
                dc.addTransientAppMetaData(CONTAINER_LIST_KEY, ejbAppList);
            }
            ejbAppList.add(this);
        }

        try {
            policyLoader.loadPolicy();

            for (EjbDescriptor desc : ejbs) {

                // Initialize each ejb container (setup component environment, register JNDI objects, etc.)
                // Any instance instantiation , timer creation/restoration, message inflow is delayed until
                // start phase.
                ContainerFactory ejbContainerFactory = services.getService
                      (ContainerFactory.class, desc.getContainerFactoryQualifier());
                if (ejbContainerFactory == null) {
                  String errMsg = localStrings.getLocalString("invalid.container.module",
                          "Container module is not available", desc.getEjbTypeForDisplay());
                  throw new RuntimeException(errMsg);
                }

                Container container = ejbContainerFactory.createContainer
                        (desc, ejbAppClassLoader, dc);
                containers.add(container);

                if (desc instanceof EjbSessionDescriptor &&
                      ((EjbSessionDescriptor) desc).isSingleton()) {
                  singletonLCM.addSingletonContainer(this,
                         (AbstractSingletonContainer) container);
                }
            }

        } catch(Throwable t) {
            abortInitializationAfterException();
            throw new RuntimeException("EJB Container initialization error", t);
        }
        
        return true;
    }

    public boolean stop(ApplicationContext stopContext) {
        DeploymentContext depc = (DeploymentContext) stopContext;
        OpsParams params = depc.getCommandParameters(OpsParams.class);
        boolean keepState = false;

        //calculate keepstate value for undeploy only.  For failed deploy,
        //keepstate remains the default value (false).
        if(params.origin.isUndeploy()) {
            keepState = resolveKeepStateOptions(depc, false, ejbBundle);
            if (keepState) {
                Properties appProps = depc.getAppProps();
                Object appId = appProps.get(EjbDeployer.APP_UNIQUE_ID_PROP);
                Properties actionReportProps = null;

                if (ejbBundle.getApplication().isVirtual()) {
                    actionReportProps = depc.getActionReport().getExtraProperties();
                } else { // the application is EAR
                    ExtendedDeploymentContext exdc = (ExtendedDeploymentContext) depc;
                    actionReportProps = exdc.getParentContext().getActionReport().getExtraProperties();
                }

                actionReportProps.put(EjbDeployer.APP_UNIQUE_ID_PROP, appId);
                actionReportProps.put(EjbApplication.KEEP_STATE, String.valueOf(true));
                _logger.log(Level.INFO, "keepstate options resolved to true, saving appId {0} for application {1}.",
                        new Object[]{appId, params.name()});
            }
        }

        // If true we're shutting down b/c of an undeploy or a fatal error during
        // deployment.  If false, it's a shutdown where the application will remain
        // deployed.
        boolean undeploy = (params.origin.isUndeploy() || params.origin.isDeploy());

        // for undeploy and failed deploy, store the keepstate in ApplicationInfo
        // and Application.  For failed deploy, keepstate is the default value (false).
        if(undeploy) {
            // store keepstate in ApplicationInfo to make it available to
            // EjbDeployer.clean().  A different instance of DeploymentContext
            // is passed to EjbDeployer.clean so we cannot use anything in DC (e.g.
            // appProps, transientData) to store keepstate.
            ApplicationRegistry appRegistry = services.getService(ApplicationRegistry.class);
            ApplicationInfo appInfo = appRegistry.get(params.name());
            appInfo.addTransientAppMetaData(KEEP_STATE, keepState);

            // store keepState in Application to make it available to subsequent
            // undeploy-related methods.
            ejbBundle.getApplication().setKeepStateResolved(String.valueOf(keepState));
        }

        // First, shutdown any singletons that were initialized based
        // on a particular ordering dependency.
        // TODO Make sure this covers both eagerly and lazily initialized
        // Singletons.
        singletonLCM.doShutdown();

        for (Container container : containers) {
            if( undeploy ) {
                container.undeploy();      
            } else {
                container.onShutdown();
            }
            if(container.getSecurityManager() != null) {
                container.getSecurityManager().destroy();
            }
        }
        
        containers.clear();
        
        return true;
    }

    /**
     * Suspends this application container.
     *
     * @return true if suspending was successful, false otherwise.
     */
    public boolean suspend() {
        // Not (yet) supported
        return false;
    }

    /**
     * Resumes this application container.
     *
     * @return true if resumption was successful, false otherwise.
     */
    public boolean resume() {
        // Not (yet) supported
        return false;
    }

    /**
     * Returns the class loader associated with this application
     *
     * @return ClassLoader for this app
     */
    public ClassLoader getClassLoader() {
        return ejbAppClassLoader;
    }

    /**
     * Returns true if at least one of the containers represents a timed object
     */
    boolean containsTimedObject() {
        for (Container container : containers) {
            if (container.isTimedObject()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Called when an exception is thrown from either the load phase or the start phase.
     * In this case we can't guarantee that the deployment framework will give us an
     * opportunity to clean up, especially if the EjbApplication object itself is never
     * registered due to the exception.  The most important thing is to make sure
     * global resources like JNDI names are cleaned up. Otherwise, subsequent deployment
     * attempts might fail without a server restart.   The container instances
     * themselves must be prepared to gracefully handle any case where undeploy/shutdown
     * is called multiple times.
     */
    private void abortInitializationAfterException() {

        for (Container container : containers) {
            container.undeploy();
        }

    }

    /**
     * Returns a consolidated keepstate value.  keepstate only takes effect for
     * redeploy operations where the app is already registered.  If the app is
     * not already registered, keepstate always resolves to false even if
     * keepstate is true in CLI or descriptors.  For redeploy operations, CLI
     * --keepstate option has precedence over descriptor keep-state element.
     * @param deployContext
     * @param isDeploy
     * @param bundleDesc
     * @return true if keepstate is true after consolidating --keepstate CLI option
     * and keep-state element in descriptors; false otherwise.
     */
    private boolean resolveKeepStateOptions(DeploymentContext deployContext, boolean isDeploy,
            EjbBundleDescriptorImpl bundleDesc) {
        Boolean isredeploy = Boolean.FALSE;
        Boolean keepState = null;
        if (isDeploy) {
            DeployCommandParameters dcp = deployContext.getCommandParameters(DeployCommandParameters.class);
            if (dcp != null) {
                isredeploy = dcp.isredeploy;
                keepState = dcp.keepstate;
            }
        } else {
            UndeployCommandParameters ucp = deployContext.getCommandParameters(UndeployCommandParameters.class);
            if (ucp != null) {
                isredeploy = ucp.isredeploy;
                keepState = ucp.keepstate;
            }
        }
        if(!isredeploy) {
            return false;
        } 

        if (keepState == null) {
            Application app = bundleDesc.getApplication();
            keepState = app.getKeepState();
        }

        return keepState;
    }

}
