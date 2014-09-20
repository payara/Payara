/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.persistence.jpa;

import com.sun.appserv.connectors.internal.api.ConnectorRuntime;
import com.sun.enterprise.deployment.*;
import com.sun.enterprise.deployment.util.DOLUtils;
import com.sun.enterprise.module.bootstrap.StartupContext;
import com.sun.logging.LogDomains;
import org.glassfish.api.deployment.DeployCommandParameters;
import org.glassfish.api.event.EventListener;
import org.glassfish.api.event.Events;
import org.glassfish.deployment.common.RootDeploymentDescriptor;
import org.glassfish.internal.data.ApplicationInfo;
import org.glassfish.internal.data.ApplicationRegistry;
import org.glassfish.internal.deployment.Deployment;
import org.glassfish.internal.deployment.ExtendedDeploymentContext;
import org.glassfish.server.ServerEnvironmentImpl;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.api.deployment.MetaData;
import org.glassfish.api.deployment.OpsParams;
import org.glassfish.deployment.common.SimpleDeployer;
import org.glassfish.deployment.common.DeploymentException;
import org.glassfish.persistence.common.Java2DBProcessorHelper;
import javax.inject.Inject;
import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PostConstruct;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Deployer for JPA applications
 * @author Mitesh Meswani
 */
@Service
public class JPADeployer extends SimpleDeployer<JPAContainer, JPApplicationContainer> implements PostConstruct, EventListener {

    @Inject
    private ConnectorRuntime connectorRuntime;

    @Inject
    private ServerEnvironmentImpl serverEnvironment;

    @Inject
    private volatile StartupContext sc = null;

    @Inject
    private Events events;

    @Inject
    private ApplicationRegistry applicationRegistry;

    private static Logger logger = LogDomains.getLogger(PersistenceUnitLoader.class, LogDomains.PERSISTENCE_LOGGER + ".jpadeployer");

    /** Key used to get/put emflists in transientAppMetadata */
    private static final String EMF_KEY = EntityManagerFactory.class.toString();

    @Override public MetaData getMetaData() {

        return new MetaData(true /*invalidateCL */ ,
                null /* provides */,
                new Class[] {Application.class} /* requires Application from dol */);
    }

    protected void generateArtifacts(DeploymentContext dc) throws DeploymentException {
        // Noting to generate yet!!
    }

    protected void cleanArtifacts(DeploymentContext dc) throws DeploymentException {
        // Drop tables if needed on undeploy.
        OpsParams params = dc.getCommandParameters(OpsParams.class);
        if (params.origin.isUndeploy() && isDas()) {

            boolean hasScopedResource = false;
            String appName = params.name();
            ApplicationInfo appInfo = applicationRegistry.get(appName);
            Application application = appInfo.getMetaData(Application.class);
            Set<BundleDescriptor> bundles = application.getBundleDescriptors();

             // Iterate through all the bundles of the app and collect pu references in referencedPus
             for (BundleDescriptor bundle : bundles) {
                 Collection<? extends PersistenceUnitDescriptor> pusReferencedFromBundle = bundle.findReferencedPUs();
                 for(PersistenceUnitDescriptor pud : pusReferencedFromBundle) {
                     hasScopedResource = hasScopedResource(pud);
                     if(hasScopedResource) {
                         break;
                     }
                 }
             }

            // if there are scoped resources, deploy them so that they are accessible for Java2DB to
            // delete tables.
            if(hasScopedResource){
                connectorRuntime.registerDataSourceDefinitions(application);
            }

             Java2DBProcessorHelper helper = new Java2DBProcessorHelper(dc);
             helper.init();
             helper.createOrDropTablesInDB(false, "JPA"); // NOI18N

            //if there are scoped resources, undeploy them.
            if(hasScopedResource){
                connectorRuntime.unRegisterDataSourceDefinitions(application);
            }
        }
    }

    /**
     * @inheritDoc
     */
    public <V> V loadMetaData(Class<V> type, DeploymentContext context) {
        return null;
    }

    /**
     * EMFs for refered pus are created and stored in JPAApplication instance.
     * The JPAApplication instance is stored in given DeploymentContext to be retrieved by load
     */
    @Override public boolean prepare(DeploymentContext context) {
        boolean prepared = super.prepare(context);
        if(prepared) {
            if(isEMFCreationRequired(context)) {
                createEMFs(context);
            }
        }
        return prepared;
    }

    /**
     * CreateEMFs and save them in persistence
     * @param context
     */
    private void createEMFs(DeploymentContext context) {
        Application application = context.getModuleMetaData(Application.class);
        Set<BundleDescriptor> bundles = application.getBundleDescriptors();

        // Iterate through all the bundles for the app and collect pu references in referencedPus
        boolean hasScopedResource = false;
        final List<PersistenceUnitDescriptor> referencedPus = new ArrayList<PersistenceUnitDescriptor>();
        for (BundleDescriptor bundle : bundles) {
            Collection<? extends PersistenceUnitDescriptor> pusReferencedFromBundle = bundle.findReferencedPUs();
            for(PersistenceUnitDescriptor pud : pusReferencedFromBundle) {
                referencedPus.add(pud);
                if( hasScopedResource(pud) ) {
                    hasScopedResource = true;
                }
            }
        }
        if (hasScopedResource) {
            // Scoped resources are registered by connector runtime after prepare(). That is too late for JPA
            // This is a hack to initialize connectorRuntime for scoped resources
            connectorRuntime.registerDataSourceDefinitions(application);
        }

        //Iterate through all the PUDs for this bundle and if it is referenced, load the corresponding pu
        PersistenceUnitDescriptorIterator pudIterator = new PersistenceUnitDescriptorIterator() {
            @Override void visitPUD(PersistenceUnitDescriptor pud, DeploymentContext context) {
                if(referencedPus.contains(pud)) {
                    boolean isDas = isDas();

                    // While running in embedded mode, it is not possible to guarantee that entity classes are not loaded by the app classloader before transformers are installed
                    // If that happens, weaving will not take place and EclipseLink will throw up. Provide users an option to disable weaving by passing the flag.
                    // Note that we enable weaving if not explicitly disabled by user
                    boolean weavingEnabled = Boolean.valueOf(sc.getArguments().getProperty("org.glassfish.persistence.embedded.weaving.enabled", "true"));

                    ProviderContainerContractInfo providerContainerContractInfo = weavingEnabled ?
                            new ServerProviderContainerContractInfo(context, connectorRuntime, isDas) :
                            new EmbeddedProviderContainerContractInfo(context, connectorRuntime, isDas);

                    try {
                        ((ExtendedDeploymentContext) context).prepareScratchDirs();
                    } catch (IOException e) {
                        // There is no way to recover if we are not able to create the scratch dirs. Just rethrow the exception.
                        throw new RuntimeException(e);
                    }


                    PersistenceUnitLoader puLoader = new PersistenceUnitLoader(pud, providerContainerContractInfo);
                    // Store the puLoader in context. It is retrieved to execute java2db and to
                    // store the loaded emfs in a JPAApplicationContainer object for cleanup
                    context.addTransientAppMetaData(getUniquePuIdentifier(pud), puLoader );
                }
            }
        };
        pudIterator.iteratePUDs(context);
    }

    /**
     * @return true if given <code>pud</code> is using scoped resource
     */
    private boolean hasScopedResource(PersistenceUnitDescriptor pud) {
        boolean hasScopedResource = false;
        String jtaDataSource = pud.getJtaDataSource();
        if(jtaDataSource != null && jtaDataSource.startsWith("java:")){
            hasScopedResource = true;
        }
        return hasScopedResource;
    }

    /**
     * @param context
     * @return true if emf creation is required false otherwise
     */
    private boolean isEMFCreationRequired(DeploymentContext context) {
/*
  Here are various use cases that needs to be handled.
  This method handles EMF creation part, APPLICATION_PREPARED event handle handles java2db and closing of emf

  To summarize,
  -Unconditionally create EMFs on DAS for java2db if it is deploy. We will close this EMF in APPLICATION_PREPARED after java2db if (target!= DAS || enable=false)
  -We will not create EMFs on instance if application is not enabled

        ------------------------------------------------------------------------------------
            Scenario                                       Expected Behavior
        ------------------------------------------------------------------------------------
        deploy --target=server   --enabled=true.   DAS(EMF created, java2db, EMF remains open)
           -restart                                DAS(EMF created, EMF remains open)
           -undeploy                               DAS(EMF closed. Drop tables)
           -create-application-ref instance1       DAS(No action)
                                                   INSTANCE1(EMF created)

        deploy --target=server   --enabled=false.  DAS(EMF created,java2db, EMF closed in APPLICATION_PREPARED)
           -restart                                DAS(No EMF created)
           -undeploy                               DAS(No EMF to close, Drop tables)

           -enable                                 DAS(EMF created)
           -undelpoy                               DAS(EMF closed, Drop tables)

           -create-application-ref instance1       DAS(No action)
                                                   INSTANCE1(EMF created)

        deploy --target=instance1 --enabled=true   DAS(EMF created, java2db, EMF closed in APPLICATION_PREPARED)
                                                   INSTANCE1(EMF created)
            -create-application-ref instance2      INSTANCE2(EMF created)
            -restart                               DAS(No EMF created)
                                                   INSTANCE1(EMF created)
                                                   INSTANCE2(EMF created)
            -undeploy                              DAS(No EMF to close, Drop tables)
                                                   INSTANCE1(EMF closed)

            -create-application-ref server         DAS(EMF created)
            -delete-application-ref server         DAS(EMF closed)
            undeploy                               INSTANCE1(EMF closed)


        deploy --target=instance --enabled=false.  DAS(EMF created, java2db, EMF closed in APPLICATION_PREPARED)
                                                   INSTANCE1(No EMF created)
            -create-application-ref instance2      DAS(No action)
                                                   INSTANCE2(No Action)
            -restart                               DAS(No EMF created)
                                                   INSTANCE1(No EMF created)
                                                   INSTANCE2(No EMF created)
            -undeploy                              DAS(No EMF to close, Drop tables)
                                                   INSTANCE1(No EMF to close)
                                                   INSTANCE2(No EMF to close)

            -enable --target=instance1             DAS(No EMF created)
                                                   INSTANCE1(EMF created)

*/

        boolean createEMFs = false;
        DeployCommandParameters deployCommandParameters = context.getCommandParameters(DeployCommandParameters.class);
        boolean deploy  = deployCommandParameters.origin.isDeploy();
        boolean enabled = deployCommandParameters.enabled;
        boolean isDas = isDas();

        if(logger.isLoggable(Level.FINER)) {
            logger.finer("isEMFCreationRequired(): deploy: " + deploy + " enabled: " + enabled + " isDas: " + isDas);
        }

        if(isDas) {
            if(deploy) {
                createEMFs = true; // Always create emfs on DAS while deploying to take care of java2db and PU validation on deploy
            } else {
                //We reach here for (!deploy && das) => server restart or enabling a disabled app on DAS
                boolean isTargetDas = isTargetDas(deployCommandParameters);
                if(logger.isLoggable(Level.FINER)) {
                    logger.finer("isEMFCreationRequired(): isTargetDas: " + isTargetDas);
                }
                
                if(enabled && isTargetDas) {
                    createEMFs = true;
                }
            }
        } else { //!das => on an instance
            if(enabled) {
                createEMFs = true;
            }
        }

        if(logger.isLoggable(Level.FINER)) {
            logger.finer("isEMFCreationRequired(): returning createEMFs:" + createEMFs);
        }

        return createEMFs;
    }

    private static boolean isTargetDas(DeployCommandParameters deployCommandParameters) {
        return "server".equals(deployCommandParameters.target); // TODO discuss with Hong. This comparison should be encapsulated somewhere
    }

    /**
     * @inheritDoc
     */
    //@Override
    public JPApplicationContainer load(JPAContainer container, DeploymentContext context) {
        return new JPApplicationContainer();
    }

    /**
     * Returns unique identifier for this pu within application
     * @param pud The given pu
     * @return Absolute pu root + pu name
     */
    private static String getUniquePuIdentifier(PersistenceUnitDescriptor pud) {
        return pud.getAbsolutePuRoot() + pud.getName();
     }

    private boolean isDas() {
        return serverEnvironment.isDas() || serverEnvironment.isEmbedded();
    }

    @Override
    public void postConstruct() {
        events.register(this);
    }

    @Override
    public void event(Event event) {
        if(logger.isLoggable(Level.FINEST)) {
            logger.finest("JpaDeployer.event():" + event.name());
        }
        if (event.is(Deployment.APPLICATION_PREPARED) ) {
            ExtendedDeploymentContext context = (ExtendedDeploymentContext)event.hook();
            DeployCommandParameters deployCommandParameters = context.getCommandParameters(DeployCommandParameters.class);
            if(logger.isLoggable(Level.FINE)) {
                logger.fine("JpaDeployer.event(): Handling APPLICATION_PREPARED origin is:" + deployCommandParameters.origin);
            }

            // When create-application-ref is called for an already deployed app, APPLICATION_PREPARED will be sent on DAS
            // Obviously there is no new emf created for this event and we need not do java2db also. Ignore the event
            // However, if target for create-application-ref is DAS => the app was deployed on other instance but now
            // an application-ref is being created on DAS. Process the app
            if(!deployCommandParameters.origin.isCreateAppRef() || isTargetDas(deployCommandParameters)) {
                Map<String, ExtendedDeploymentContext> deploymentContexts = context.getModuleDeploymentContexts();

                for (DeploymentContext deploymentContext : deploymentContexts.values()) {
                    //bundle level pus
                    iterateInitializedPUsAtApplicationPrepare(deploymentContext);
                }
                //app level pus
                iterateInitializedPUsAtApplicationPrepare(context);
            }
        } else if(event.is(Deployment.APPLICATION_DISABLED)) {
            logger.fine("JpaDeployer.event(): APPLICATION_DISABLED");
            // APPLICATION_DISABLED will be generated when an app is disabled/undeployed/appserver goes down.
            //close all the emfs created for this app
            ApplicationInfo appInfo = (ApplicationInfo) event.hook();
            closeEMFs(appInfo);
        }
    }

    private void closeEMFs(ApplicationInfo appInfo) {
        //Suppress warning required as there is no way to pass equivalent of List<EMF>.class to the method
        @SuppressWarnings("unchecked") List<EntityManagerFactory> emfsCreatedForThisApp = appInfo.getTransientAppMetaData(EMF_KEY, List.class);
        if(emfsCreatedForThisApp != null) { // Events are always dispatched to all registered listeners. emfsCreatedForThisApp will be null for an app that does not have PUs.
            for (EntityManagerFactory entityManagerFactory : emfsCreatedForThisApp) {
                entityManagerFactory.close();
            }
            // We no longer have the emfs in open state clear the list.
            // On app enable(after a disable), for a cluster, the deployment framework calls prepare() for instances but not for DAS.
            // So on DAS, at a disable, the emfs will be closed and we will not attempt to close emfs when appserver goes down even if the app is re-enabled.
            emfsCreatedForThisApp.clear();
        }
    }

    /**
     * Does java2db on DAS and saves emfs created during prepare to ApplicationInfo maintained by DOL.
     * ApplicationInfo is not available during prepare() so we can not directly use it there.
     * @param context
     */
    private void iterateInitializedPUsAtApplicationPrepare(final DeploymentContext context) {

        final DeployCommandParameters deployCommandParameters = context.getCommandParameters(DeployCommandParameters.class);
        String appName = deployCommandParameters.name;
        final ApplicationInfo appInfo = applicationRegistry.get(appName);

        //iterate through all the PersistenceUnitDescriptor for this bundle.
        PersistenceUnitDescriptorIterator pudIterator = new PersistenceUnitDescriptorIterator() {
            @Override void visitPUD(PersistenceUnitDescriptor pud, DeploymentContext context) {
                //PersistenceUnitsDescriptor corresponds to  persistence.xml. A bundle can only have one persitence.xml except
                // when the bundle is an application which can have multiple persitence.xml under jars in root of ear and lib.
                PersistenceUnitLoader puLoader = context.getTransientAppMetaData(getUniquePuIdentifier(pud), PersistenceUnitLoader.class);
                if (puLoader != null) { // We have initialized PU
                    boolean saveEMF = true;
                    if(isDas()) { //We do validation and execute Java2DB only on DAS
                        if(deployCommandParameters.origin.isDeploy()) { //APPLICATION_PREPARED will be called for create-application-ref also. We should perform java2db only on first deploy

                            //Create EM to trigger validation on PU
                            EntityManagerFactory emf = puLoader.getEMF();
                            EntityManager em = null;
                            try {
                                // Create EM to trigger any validations that are lazily performed by the provider
                                // EM creation also triggers DDL generation by provider.
                                em = emf.createEntityManager();
                            } catch (PersistenceException e) {
                                // Exception indicates something went wrong while performing validation. Clean up and rethrow to fail deployment
                                emf.close();
                                throw new DeploymentException(e);  // Need to wrap exception in DeploymentException else deployment will not fail !!
                            } finally {
                                if (em != null) {
                                    em.close();
                                }
                            }

                            puLoader.doJava2DB();

                            boolean enabled = deployCommandParameters.enabled;
                            boolean isTargetDas = isTargetDas(deployCommandParameters);
                            if(logger.isLoggable(Level.FINER)) {
                                logger.finer("iterateInitializedPUsAtApplicationPrepare(): enabled: " + enabled + " isTargetDas: " + isTargetDas);
                            }
                            if(!isTargetDas || !enabled) {
                                // we are on DAS but target != das or app is not enabled on das => The EMF was just created for Java2Db. Close it. 
                                puLoader.getEMF().close();
                                saveEMF = false; // Do not save EMF. We have already closed it
                            }
                        }
                    }

                    if(saveEMF) {
                        // Save emf in ApplicationInfo so that it can be retrieved and closed for cleanup
                        @SuppressWarnings("unchecked") //Suppress warning required as there is no way to pass equivalent of List<EMF>.class to the method
                        List<EntityManagerFactory> emfsCreatedForThisApp = appInfo.getTransientAppMetaData(EMF_KEY, List.class );
                        if(emfsCreatedForThisApp == null) {
                            //First EMF for this app, initialize
                            emfsCreatedForThisApp = new ArrayList<EntityManagerFactory>();
                            appInfo.addTransientAppMetaData(EMF_KEY, emfsCreatedForThisApp);
                        }
                        emfsCreatedForThisApp.add(puLoader.getEMF());
                    } // if (saveEMF)
                } // if(puLoader != null)
            }
        };

        pudIterator.iteratePUDs(context);

    }

    /**
     * Helper class to centralize the code for loop that iterates through all the PersistenceUnitDescriptor for a given DeploymentContext (and hence the corresponding bundle)
     */
    private static abstract class PersistenceUnitDescriptorIterator {
        /**
         * Iterate through all the PersistenceUnitDescriptors for the given context (and hence corresponding bundle) and call visitPUD for each of them
         * @param context
         */
        void iteratePUDs(DeploymentContext context) {
            RootDeploymentDescriptor currentBundle = DOLUtils.getCurrentBundleForContext(context);
            if (currentBundle != null) { // it can be null for non-JavaEE type of application deployment. e.g., issue 15869
                Collection<PersistenceUnitsDescriptor> pusDescriptorForThisBundle = currentBundle.getExtensionsDescriptors(PersistenceUnitsDescriptor.class);
                for (PersistenceUnitsDescriptor persistenceUnitsDescriptor : pusDescriptorForThisBundle) {
                        for (PersistenceUnitDescriptor pud : persistenceUnitsDescriptor.getPersistenceUnitDescriptors()) {
                            visitPUD(pud, context);
                        }
                }
            }

        }

        /**
         * Called for each PersistenceUnitDescriptor visited by this iterator.
         */
        abstract void visitPUD(PersistenceUnitDescriptor pud, DeploymentContext context);

    }
}
