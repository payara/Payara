/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2014 Oracle and/or its affiliates. All rights reserved.
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
// Portions Copyright [2016/2017] [Payara Foundation and/or its affiliates]

package org.glassfish.ejb.startup;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.inject.Provider;

import com.sun.ejb.codegen.StaticRmiStubGenerator;
import com.sun.ejb.containers.EJBTimerService;
import com.sun.ejb.containers.EjbContainerUtilImpl;
import com.sun.enterprise.config.serverbeans.Cluster;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.container.common.spi.util.ComponentEnvManager;
import com.sun.enterprise.deployment.Application;
import com.sun.enterprise.deployment.WebBundleDescriptor;
import com.sun.enterprise.module.bootstrap.StartupContext;
import com.sun.enterprise.security.PolicyLoader;
import com.sun.enterprise.security.ee.SecurityUtil;
import com.sun.enterprise.security.util.IASSecurityException;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.logging.LogDomains;
import java.security.SecureRandom;
import org.glassfish.api.admin.config.ReferenceContainer;
import org.glassfish.api.deployment.DeployCommandParameters;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.api.deployment.MetaData;
import org.glassfish.api.deployment.OpsParams;
import org.glassfish.api.deployment.UndeployCommandParameters;
import org.glassfish.api.event.EventListener;
import org.glassfish.api.event.Events;
import org.glassfish.api.invocation.RegisteredComponentInvocationHandler;
import org.glassfish.deployment.common.DeploymentException;
import org.glassfish.deployment.common.DeploymentProperties;
import org.glassfish.deployment.common.DeploymentUtils;
import org.glassfish.ejb.deployment.descriptor.EjbBundleDescriptorImpl;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.glassfish.ejb.security.application.EJBSecurityManager;
import org.glassfish.ejb.security.application.EjbSecurityProbeProvider;
import org.glassfish.ejb.security.factory.EJBSecurityManagerFactory;
import org.glassfish.ejb.spi.CMPDeployer;
import org.glassfish.ejb.spi.CMPService;
import org.glassfish.internal.api.ServerContext;
import org.glassfish.internal.data.ApplicationInfo;
import org.glassfish.internal.deployment.Deployment;
import org.glassfish.internal.deployment.ExtendedDeploymentContext;
import org.glassfish.javaee.core.deployment.JavaEEDeployer;
import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PostConstruct;

/**
 * Ejb module deployer.
 *
 */
@Service
public class EjbDeployer
        extends JavaEEDeployer<EjbContainerStarter, EjbApplication>
        implements PostConstruct, EventListener {

    @Inject
    protected ServerContext sc;

    @Inject
    protected Domain domain;

    @Inject
    protected PolicyLoader policyLoader;

    @Inject
    protected EJBSecurityManagerFactory ejbSecManagerFactory;

    @Inject
    private ComponentEnvManager compEnvManager;

    @Inject
    private Events events;

    @Inject
    StartupContext startupContext;

    private Object lock = new Object();
    private volatile CMPDeployer cmpDeployer = null;

    private static SecureRandom random = new SecureRandom();

    // Property used to persist unique id across server restart.
    static final String APP_UNIQUE_ID_PROP = "org.glassfish.ejb.container.application_unique_id";
    static final String IS_TIMEOUT_APP_PROP = "org.glassfish.ejb.container.is_timeout_application";

    private AtomicLong uniqueIdCounter;

    private static final Logger _logger = LogDomains.getLogger(EjbDeployer.class, LogDomains.EJB_LOGGER, false);

    private static final LocalStringManagerImpl localStrings = new LocalStringManagerImpl(EjbDeployer.class);

    private final EjbSecurityProbeProvider probeProvider = new EjbSecurityProbeProvider();

    @Inject
    Provider<RegisteredComponentInvocationHandler> registeredComponentInvocationHandlerProvider;

    @Inject
    Provider<CMPService> cmpServiceProvider;

    @Inject
    Provider<CMPDeployer> cmpDeployerProvider;

    /**
     * Constructor
     */
    public EjbDeployer() {

        // Seed a counter used for ejb application unique id generation.
        uniqueIdCounter = new AtomicLong(System.currentTimeMillis());

    }

    @Override
    public void postConstruct() {
        Properties arguments = startupContext.getArguments();
        if (arguments != null) {
            boolean isUpgrade = Boolean.valueOf(arguments.getProperty("-upgrade"));
            if (isUpgrade) {
                // we don't want to register this listener for the upgrade
                // start up
                return;
            }
        }

        events.register(this);
    }

    @Override
    public MetaData getMetaData() {
        return new MetaData(false,
                new Class[] {EjbBundleDescriptorImpl.class}, new Class[] {Application.class});
    }

    @Override
    public boolean prepare(DeploymentContext dc) {
        EjbBundleDescriptorImpl ejbBundle = dc.getModuleMetaData(EjbBundleDescriptorImpl.class);

        if( ejbBundle == null ) {
            String errMsg = localStrings.getLocalString("context.contains.no.ejb", "DeploymentContext does not contain any EJB", dc.getSourceDir());
            throw new RuntimeException(errMsg);
        }

        // Get application-level properties (*not* module-level)
        Properties appProps = dc.getAppProps();

        long uniqueAppId;

        if( !appProps.containsKey(APP_UNIQUE_ID_PROP)) {

            // This is the first time load is being called for any ejb module in an
            // application, so generate the unique id.

            uniqueAppId = getNextEjbAppUniqueId();
            appProps.setProperty(APP_UNIQUE_ID_PROP, uniqueAppId + "");
        } else {
            uniqueAppId = Long.parseLong(appProps.getProperty(APP_UNIQUE_ID_PROP));
        }

        OpsParams params = dc.getCommandParameters(OpsParams.class);
        if (params.origin.isDeploy()) {
            // KEEP_STATE is saved to AppProps in EjbApplication.stop
            String keepStateVal = (String) dc.getAppProps().get(EjbApplication.KEEP_STATE);
            if (keepStateVal != null) {
                // save KEEP_STATE to Application so subsequent to make it available
                // to subsequent deploy-related methods.
                ejbBundle.getApplication().setKeepStateResolved(keepStateVal);
                if (_logger.isLoggable(Level.FINE)) {
                    _logger.log(Level.FINE, "EjbDeployer.prepare set keepstate to {0} for application.",
                            ejbBundle.getApplication().getKeepStateResolved());
                }
            }
        }

        Application app = ejbBundle.getApplication();

        if( !app.isUniqueIdSet() ) {
            // This will set the unique id for all EJB components in the application.
            // If there are multiple ejb modules in the app, we'll only call it once
            // for the first ejb module load().  All the old
            // .xml processing for unique-id in the sun-* descriptors is removed so
            // this is the only place where Application.setUniqueId() should be called.
            app.setUniqueId(uniqueAppId);
        }

        return super.prepare(dc);
    }

    @Override
    public EjbApplication load(EjbContainerStarter containerStarter, DeploymentContext dc) {
        super.load(containerStarter, dc);

        if (_logger.isLoggable(Level.FINE)) {
            _logger.log( Level.FINE, "EjbDeployer Loading app from: " + dc.getSourceDir());
        }
        //Register the EjbSecurityComponentInvocationHandler

        RegisteredComponentInvocationHandler handler = habitat.getService(RegisteredComponentInvocationHandler.class,"ejbSecurityCIH");
        handler.register();

        EjbBundleDescriptorImpl ejbBundle = dc.getModuleMetaData(EjbBundleDescriptorImpl.class);

        if( ejbBundle == null ) {
            throw new RuntimeException("Unable to load EJB module.  DeploymentContext does not contain any EJB " +
                    " Check archive to ensure correct packaging for " + dc.getSourceDir());
        }

        ejbBundle.setClassLoader(dc.getClassLoader());
        ejbBundle.setupDataStructuresForRuntime();

        if (ejbBundle.containsCMPEntity()) {
            CMPService cmpService = cmpServiceProvider.get();
            if (cmpService == null) {
                throw new RuntimeException("CMP Module is not available");
            } else if (!cmpService.isReady()) {
                throw new RuntimeException("CMP Module is not initialized");
            }
        }


        EjbApplication ejbApp = new EjbApplication(ejbBundle, dc,
                dc.getClassLoader(), habitat);

        try {
            compEnvManager.bindToComponentNamespace(ejbBundle);

            // If within .war, also bind dependencies declared by web application.  There is
            // a single naming environment for the entire .war module.  Yhis is necessary
            // in order for eagerly initialized ejb components to have visibility to all the
            // dependencies b/c the web container does not bind to the component namespace until
            // its start phase, which comes after the ejb start phase.
            Object rootDesc = ejbBundle.getModuleDescriptor().getDescriptor();
            if( (rootDesc != ejbBundle) && (rootDesc instanceof WebBundleDescriptor ) ) {
                WebBundleDescriptor webBundle = (WebBundleDescriptor) rootDesc;
                compEnvManager.bindToComponentNamespace(webBundle);
            }

        } catch(Exception e) {
            throw new RuntimeException("Exception registering ejb bundle level resources", e);
        }

        ejbApp.loadContainers(dc);

        return ejbApp;
    }

    public void unload(EjbApplication ejbApplication, DeploymentContext dc) {

        EjbBundleDescriptorImpl ejbBundle = ejbApplication.getEjbBundleDescriptor();

        try {
            compEnvManager.unbindFromComponentNamespace(ejbBundle);
        } catch(Exception e) {
             _logger.log( Level.WARNING, "Error unbinding ejb bundle " +
                     ejbBundle.getModuleName() + " dependency namespace", e);
        }

        if (ejbBundle.containsCMPEntity()) {
            initCMPDeployer();
            if (cmpDeployer != null) {
                cmpDeployer.unload(ejbBundle.getClassLoader());
            }
        }

        // All the other work is done in EjbApplication.

    }

    /**
     * Clean any files and artifacts that were created during the execution
     * of the prepare method.
     *
     * @param dc deployment context
     */
    public void clean(DeploymentContext dc) {
        // Both undeploy and shutdown scenarios are
        // handled directly in EjbApplication.shutdown.

        // But CMP drop tables should be handled here.

        OpsParams params = dc.getCommandParameters(OpsParams.class);
        if ( (params.origin.isUndeploy() || params.origin.isDeploy()) && isDas()) {

            // If CMP beans are present, cmpDeployer should've been initialized in unload()
            if (cmpDeployer != null) {
                cmpDeployer.clean(dc);
            }

            Properties appProps = dc.getAppProps();
            String uniqueAppId = appProps.getProperty(APP_UNIQUE_ID_PROP);
            try {
                if (getTimeoutStatusFromApplicationInfo(params.name()) && uniqueAppId != null) {
                    String target = ((params.origin.isDeploy())?
                            dc.getCommandParameters(DeployCommandParameters.class).target :
                            dc.getCommandParameters(UndeployCommandParameters.class).target);

                    if (DeploymentUtils.isDomainTarget(target)) {
                        List<String> targets = (List<String>)dc.getTransientAppMetaData(DeploymentProperties.PREVIOUS_TARGETS, List.class);
                        if (targets == null) {
                            targets = domain.getAllReferencedTargetsForApplication(params.name());
                        }
                        if (targets != null && targets.size() > 0) {
                            target = targets.get(0);
                        }
                    }
                    EJBTimerService persistentTimerService = null;
                    EJBTimerService nonPersistentTimerService = null;
                    if (EJBTimerService.isPersistentTimerServiceLoaded()) {
                        persistentTimerService = EJBTimerService.getPersistentTimerService();
                    }
                    if (EJBTimerService.isNonPersistentTimerServiceLoaded()) {
                        nonPersistentTimerService = EJBTimerService.getNonPersistentTimerService();
                    }
                    if (_logger.isLoggable(Level.FINE)) {
                        _logger.log(Level.FINE, "EjbDeployer APP ID of a Timeout App? {0}", uniqueAppId);
                        _logger.log(Level.FINE, "EjbDeployer Persistent TimerService: {0}", persistentTimerService);
                        _logger.log(Level.FINE, "EjbDeployer Non-Persistent TimerService: {0}", nonPersistentTimerService);
                    }

                    if (persistentTimerService == null) {
                        _logger.log(Level.WARNING, "EJB Persistent Timer Service is not available. Persistent Timers for application with id {0} will not be deleted",
                                uniqueAppId
                        );
                    } else {
                        if (getKeepStateFromApplicationInfo(params.name())) {
                            _logger.log(Level.INFO,
                                    "Persistent Timers will not be destroyed since keepstate is true for application {0}",
                                    params.name()
                            );
                        } else {
                            persistentTimerService.destroyAllTimers(Long.parseLong(uniqueAppId));
                        }
                    }
                    if (nonPersistentTimerService == null) {
                        _logger.log(Level.WARNING,
                                "EJB Non-Persistent Timer Service is not available. Non-Persistent Timers for application with id {0} will not be deleted",
                                uniqueAppId
                        );
                    } else {
                        nonPersistentTimerService.destroyAllTimers(Long.parseLong(uniqueAppId));
                    }
                }
            } catch (Exception e) {
                _logger.log( Level.WARNING, "Failed to delete timers for application with id " + uniqueAppId, e);
            }
        }

        //Security related cleanup is to be done for the undeploy event
        if( params.origin.isUndeploy()|| params.origin.isDeploy() || params.origin.isLoad()) {

            //Removing EjbSecurityManager for undeploy case
            String appName = params.name();
            String[] contextIds =
                    ejbSecManagerFactory.getContextsForApp(appName, false);
            if (contextIds != null) {
                for (String contextId : contextIds) {
                    try {
                        //TODO:appName is not correct, we need the module name
                        //from the descriptor.
                        probeProvider.policyDestructionStartedEvent(contextId);
                        SecurityUtil.removePolicy(contextId);
                        probeProvider.policyDestructionEndedEvent(contextId);
                        probeProvider.policyDestructionEvent(contextId);
                    } catch (IASSecurityException ex) {
                        _logger.log(Level.WARNING, "Error removing the policy file " +
                                "for application " + appName + " " + ex);
                    }

                    ArrayList<EJBSecurityManager> managers =
                            ejbSecManagerFactory.getManagers(contextId, false);
                    if (managers != null) {
                        for (EJBSecurityManager m : managers) {
                            m.destroy();
                        }
                    }
                }
            }
            //Removing the RoleMapper
            SecurityUtil.removeRoleMapper(dc);
        }


    }

    /**
     * Use this method to generate any ejb-related artifacts for the module
     */
    @Override
    protected void generateArtifacts(DeploymentContext dc)
            throws DeploymentException {

        OpsParams params = dc.getCommandParameters(OpsParams.class);
        if (!(params.origin.isDeploy() && isDas())) {
            //Generate artifacts only when being deployed on DAS
            return;
        }

        EjbBundleDescriptorImpl bundle = dc.getModuleMetaData(EjbBundleDescriptorImpl.class);

        DeployCommandParameters dcp =
                dc.getCommandParameters(DeployCommandParameters.class);
        boolean generateRmicStubs = dcp.generatermistubs;
        dc.addTransientAppMetaData(CMPDeployer.MODULE_CLASSPATH, getModuleClassPath(dc));
        if( generateRmicStubs ) {
            StaticRmiStubGenerator staticStubGenerator = new StaticRmiStubGenerator(habitat);
            try {
                staticStubGenerator.ejbc(dc);
            } catch(Exception e) {
                throw new DeploymentException("Static RMI-IIOP Stub Generation exception for " +
                        dc.getSourceDir(), e);
            }
        }

        if (bundle == null || !bundle.containsCMPEntity()) {
            // bundle WAS null in a war file where we do not support CMPs
            return;
        }

        initCMPDeployer();
        if (cmpDeployer == null) {
            throw new DeploymentException("No CMP Deployer is available to deploy this module");
        }
        cmpDeployer.deploy(dc);


    }

    @Override
    public void event(Event event) {
        if (event.is(Deployment.APPLICATION_PREPARED) && isDas()) {
            ExtendedDeploymentContext context = (ExtendedDeploymentContext)event.hook();
            OpsParams opsparams = context.getCommandParameters(OpsParams.class);
            DeployCommandParameters dcp = context.getCommandParameters(DeployCommandParameters.class);

            ApplicationInfo appInfo = appRegistry.get(opsparams.name());
            Application app = appInfo.getMetaData(Application.class);
            if (app == null) {
                // Not a Java EE application
                return;
            }

            if (_logger.isLoggable(Level.FINE)) {
                _logger.log( Level.FINE, "EjbDeployer in APPLICATION_PREPARED for origin: " + opsparams.origin +
                        ", target: " + dcp.target + ", name: " + opsparams.name());
            }

            boolean createTimers = true;
            if (!(opsparams.origin.isDeploy() || opsparams.origin.isCreateAppRef()) || env.getInstanceName().equals(dcp.target)) {
                // Do real work only on deploy for a cluster or create-application-ref (the latter will
                // check if it's the 1st ref being added or a subsequent one (timers with this unique id are present
                // or not)
                // Timers will be created by the BaseContainer if it's a single instance deploy
                if (_logger.isLoggable(Level.FINE)) {
                    _logger.log( Level.FINE, "EjbDeployer ... will only set the timeout application flag if any");
                }
                // But is-timed-app needs to be set in AppInfo in any case
                createTimers = false;
            }

            String target = dcp.target;
            if (createTimers && dcp.isredeploy != null && dcp.isredeploy && DeploymentUtils.isDomainTarget(target)) {
                List<String> targets = (List<String>)context.getTransientAppMetaData(DeploymentProperties.PREVIOUS_TARGETS, List.class);
                for (String ref: targets) {
                    target = ref;
                    if (domain.getClusterNamed(target) != null) {
                        break; // prefer cluster target
                    }
                 }
            }

            if (_logger.isLoggable(Level.FINE)) {
                _logger.log( Level.FINE, "EjbDeployer using target for event as " + target);
            }

            boolean isTimedApp = false;
            for (EjbBundleDescriptorImpl ejbBundle : app.getBundleDescriptors(EjbBundleDescriptorImpl.class)) {
                if (checkEjbBundleForTimers(ejbBundle, createTimers, target)) {
                    isTimedApp = true;
                }
            }

            if (isTimedApp && (opsparams.origin.isDeploy() || opsparams.origin.isLoad())) {
                // Mark application as a timeout application, so that the clean() call removes the timers.
                appInfo.addTransientAppMetaData(IS_TIMEOUT_APP_PROP, Boolean.TRUE);
            }
        }
    }

    private boolean checkEjbBundleForTimers(EjbBundleDescriptorImpl ejbBundle, boolean createTimers, String target) {
        boolean result = false;
        if (ejbBundle != null) {
            if (_logger.isLoggable(Level.FINE)) {
                _logger.log( Level.FINE, "EjbDeployer.checkEjbBundleForTimers in BUNDLE: " + ejbBundle.getName());
            }

            for (EjbDescriptor ejbDescriptor : ejbBundle.getEjbs()) {
                if (_logger.isLoggable(Level.FINE)) {
                    _logger.log( Level.FINE, "EjbDeployer.checkEjbBundleForTimers in EJB: " + ejbDescriptor.getName());
                }

                if (ejbDescriptor.isTimedObject()) {
                    result = true;
                    if (createTimers && !DeploymentUtils.isDomainTarget(target)) {
                        createAutomaticPersistentTimersForEJB(ejbDescriptor, target);
                    }
                }
            }
        }
        return result;
    }


    /** Start EJB Timer Service and create automatic timers for this EJB in this target
     */
    private void createAutomaticPersistentTimersForEJB(EjbDescriptor ejbDescriptor, String target) {
        try {
            //Start EJB Timer Service if it wasn't started yet. On DAS the first start will create the timer table.
            EJBTimerService timerService = EJBTimerService.getEJBTimerService(target);
            if (_logger.isLoggable(Level.FINE)) {
                _logger.log( Level.FINE, "EjbDeployer BEAN ID? " + ejbDescriptor.getUniqueId());
                _logger.log( Level.FINE, "EjbDeployer TimerService: " + timerService);
            }

            if( timerService != null ) {
                if (_logger.isLoggable(Level.FINE)) {
                    _logger.log( Level.FINE, "EjbDeployer - calling timerService.createSchedules for " +
                            ejbDescriptor.getUniqueId());
                }
                timerService.createSchedulesOnServer(ejbDescriptor, getOwnerId(target));

                if (_logger.isLoggable(Level.FINE)) {
                    _logger.log( Level.FINE, "EjbDeployer Done With BEAN ID: " + ejbDescriptor.getUniqueId());
                }
            } else {
                throw new RuntimeException("EJB Timer Service is not available");
            }

        } catch (Exception e) {
            throw new DeploymentException("Failed to create automatic timers for " + ejbDescriptor.getName(), e);
        }
    }

    private String getOwnerId(String target) {
        // If target is a cluster, replace it with the instance
        ReferenceContainer ref = domain.getReferenceContainerNamed(target);

        if(ref != null && ref.isCluster()) {
            Cluster cluster = (Cluster) ref; // guaranteed safe cast!!
            List<Server>  instances = cluster.getInstances();

            // Try a random instance in a cluster
            int useInstance = random.nextInt(instances.size());
            Server s0 = instances.get(useInstance);
            if (s0.isRunning()) {
                return s0.getName();
            } else {
                // Pick the first running instead
                for (Server s : instances) {
                    if (s.isRunning()) {
                        return s.getName();
                    }
                }
            }
            // If none of the instances is running, return a random instance in a
            // cluster
            return s0.getName();
        }


        return target;
    }

    private long getNextEjbAppUniqueId() {
        long next = uniqueIdCounter.incrementAndGet();

        // This number represents the base unique id for each ejb application.
        // It is used to generate an id for each ejb component that is
        // guaranteed to be unique across all the applications deployed to a
        // particular stand-alone server instance or DAS.
        //
        // The unique id is 64 bits, with the low-order 16 bits zeroed out.
        // Component ids are selected from these low-order bits, allowing a
        // maximum of 2^16 EJB components per application.
        //
        // The initial number is seeded from System.currentTimeMillis() the
        // first time this class is instantiated after a server start.
        // Since this epoch value is relative to 1970, even after left-shifting
        // 16 bits, the number of remaining milliseconds won't run out until
        // the year 10889.   This scheme also assumes that for the lifetime
        // of the JVM for a given server, there aren't more individual
        // ejb application deployments than elapsed milliseconds, since the
        // next time the server starts it will simply seed from
        // currentTimeMillis() again rather than remembering the largest unique
        // id that was used the last time the server ran.

        return next << 16;
    }

    private void initCMPDeployer() {
        if (cmpDeployer == null) {
            synchronized(lock) {
                cmpDeployer = cmpDeployerProvider.get();
            }
        }
    }

   /**
    * Embedded is a single-instance like DAS
    */
    private boolean isDas() {
        return EjbContainerUtilImpl.getInstance().isDas();
    }

    private boolean getKeepStateFromApplicationInfo(String appName) {
        return getBooleanStateFromApplicationInfo(EjbApplication.KEEP_STATE, appName);
    }

    private boolean getBooleanStateFromApplicationInfo(String flag, String appName) {
        ApplicationInfo appInfo = appRegistry.get(appName);
        if (appInfo == null) {
            // appInfo can be null when running EjbDeployer.clean after a failed deploy
            return false;
        }
        Boolean rc = appInfo.getTransientAppMetaData(flag, Boolean.class);
        return (rc == null ? false : rc);
    }

    private boolean getTimeoutStatusFromApplicationInfo(String appName) {
        return getBooleanStateFromApplicationInfo(IS_TIMEOUT_APP_PROP, appName);
    }
}
