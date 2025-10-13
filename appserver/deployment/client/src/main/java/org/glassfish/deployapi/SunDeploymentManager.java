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
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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

package org.glassfish.deployapi;

import com.sun.enterprise.util.HostAndPort;
import com.sun.enterprise.util.StringUtils;

import com.sun.enterprise.module.ModulesRegistry;

import com.sun.enterprise.module.single.StaticModulesRegistry;
import com.sun.enterprise.module.bootstrap.StartupContext;
import org.glassfish.api.admin.ProcessEnvironment;

import org.glassfish.deployapi.config.SunDeploymentConfiguration;
import org.glassfish.deployment.client.DeploymentFacility;
import org.glassfish.deployment.client.DeploymentFacilityFactory;
import org.glassfish.deployment.client.ServerConnectionIdentifier;
import org.glassfish.api.deployment.archive.ReadableArchive;
import com.sun.enterprise.deploy.shared.ArchiveFactory;
import com.sun.enterprise.deployment.deploy.shared.MemoryMappedArchive;
import org.glassfish.deployment.client.DFDeploymentProperties;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;

import com.sun.enterprise.util.LocalStringManagerImpl;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;

import java.net.URL;
import java.net.MalformedURLException;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.enterprise.deploy.spi.DeploymentManager;
import javax.enterprise.deploy.model.DeployableObject;
import javax.enterprise.deploy.shared.CommandType;
import javax.enterprise.deploy.shared.DConfigBeanVersionType;
import javax.enterprise.deploy.shared.ModuleType;
import javax.enterprise.deploy.spi.DeploymentConfiguration;
import javax.enterprise.deploy.spi.exceptions.DConfigBeanVersionUnsupportedException;
import javax.enterprise.deploy.spi.exceptions.InvalidModuleException;
import javax.enterprise.deploy.spi.exceptions.TargetException;
import javax.enterprise.deploy.spi.status.ProgressObject;
import javax.enterprise.deploy.spi.status.ProgressEvent;
import javax.enterprise.deploy.spi.status.DeploymentStatus;
import javax.enterprise.deploy.spi.Target;
import javax.enterprise.deploy.spi.TargetModuleID;

/**
 * @author Jerome Dochez
 * @author Tim Quinn
 * @author David Matejcek
 */
public class SunDeploymentManager implements DeploymentManager {

    private static final Logger LOG = Logger.getLogger(SunDeploymentManager.class.getName());

    /** cached reference to a connected DeploymentFacility */
    private DeploymentFacility deploymentFacility;

    private static LocalStringManagerImpl localStrings =
	    new LocalStringManagerImpl(SunDeploymentManager.class);

    private static Locale defaultLocale = Locale.US;
    private Locale currentLocale = defaultLocale;
    private static Locale[] supportedLocales = { Locale.US };
    private final String disconnectedMessage = localStrings.getLocalString(
			"enterprise.deployapi.spi.disconnectedDM", // NOI18N
			"Illegal operation for a disconnected DeploymentManager");// NOI18N

    private ServiceLocator habitat;


    /** Creates a new instance of DeploymentManager */
    public SunDeploymentManager() {
    }

    /** Creates a new instance of DeploymentManager */
    public SunDeploymentManager(ServerConnectionIdentifier sci) {
        deploymentFacility = DeploymentFacilityFactory.getDeploymentFacility();
        deploymentFacility.connect(sci);
        prepareHabitat();
    }

    @Override
    public Target[] getTargets() throws IllegalStateException {
        verifyConnected();
        try {
            return deploymentFacility.listTargets();
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public TargetModuleID[] getRunningModules(ModuleType moduleType,
		Target[] targetList) throws TargetException, IllegalStateException {
        return getModules(moduleType, targetList, DFDeploymentProperties.RUNNING);
    }

    @Override
    public TargetModuleID[] getNonRunningModules(ModuleType moduleType, Target[] targetList)
        throws TargetException, IllegalStateException {
        return getModules(moduleType, targetList, DFDeploymentProperties.NON_RUNNING);
    }

    @Override
    public TargetModuleID[] getAvailableModules(ModuleType moduleType, Target[] targetList)
        throws TargetException, IllegalStateException {
        return getModules(moduleType, targetList, DFDeploymentProperties.ALL);
    }

    /**
     * Single method used by several public methods to make sure the deployment manager is
     * connected and, if not, throw the IllegalStateException.
     *
     * @throws IllegalStateException if the deployment manager is not connected.
     */
    private void verifyConnected() {
        if (isDisconnected()) {
            throw new IllegalStateException(disconnectedMessage);
        }
    }

    /**
     * Report whether the deployment manager is currently disconnected from the DAS.
     *
     * @returns whether the deployment manager is disconnected from the DAS
     */
    private boolean isDisconnected(){
        if (deploymentFacility == null) {
            return true;
        }
        return (!deploymentFacility.isConnected());
    }

    /**
     * Get all modules in the specified state from the targets specified in the argument list.
     *
     * @param moduleType which returned modules must match
     * @param array of Target indicating which targets should be searched for matching modules
     * @param state state of the modules have for the indicated attribute to be matched
     * @throws TargetException if a target was improperly formed
     * @throws IllegalStateException if the method is called after release was called
     */
    private TargetModuleID[] getModules(ModuleType moduleType, Target[] targetList, String state)
        throws TargetException, IllegalStateException {

        verifyConnected();
        if (moduleType == null) {
            return null;
        }

        try {
            Vector<TargetModuleID> resultingTMIDs = new Vector<>();
            for (Target element : targetList) {
                TargetImpl aTarget = (TargetImpl) element;

                TargetModuleID[] tmids = deploymentFacility._listAppRefs(new Target[] {aTarget}, state,
                    getTypeFromModuleType(moduleType));
                addToTargetModuleIDs(tmids, moduleType, aTarget, resultingTMIDs);
            }
            TargetModuleID [] answer = resultingTMIDs.toArray(new TargetModuleIDImpl[resultingTMIDs.size()]);
            return answer;


        } catch(Exception e){
            TargetException tg = new TargetException(localStrings.getLocalString(
                "enterprise.deployapi.spi.errorgetreqmods",
                "Error getting required modules"
                ));
            tg.initCause(e);
            throw tg;
        }
    }

    /**
     * @return web/ejb/appclient/connector/ear
     */
    private String getTypeFromModuleType(ModuleType moduleType) {
       if (moduleType.equals(ModuleType.WAR)) {
           return "web";
       } else if (moduleType.equals(ModuleType.EJB)) {
           return "ejb";
       } else if (moduleType.equals(ModuleType.CAR)) {
           return "appclient";
       } else if (moduleType.equals(ModuleType.RAR)) {
           return "connector";
       } else if (moduleType.equals(ModuleType.EAR)) {
           return "ear";
       }
       return null;
    }

    /**
     * @param type war/ejb/car/rar/ear
     *
     * @return {@link ModuleType} for accepted types or null
     */
    private ModuleType getModuleTypeFromType(String type) {
       if (type.equals("war")) {
           return ModuleType.WAR;
       } else if (type.equals("ejb")) {
           return ModuleType.EJB;
       } else if (type.equals("car")) {
           return ModuleType.CAR;
       } else if (type.equals("rar")) {
           return ModuleType.RAR;
       } else if (type.equals("ear")) {
           return ModuleType.EAR;
       }
       return null;
    }

    /**
     * Augments a Collection of TargetModuleIDs with new entries for target module IDs of a given
     * module type on the specified target.
     *
     * @param tmids array of TargetModuleIDs
     * @param type the ModuleType of interest
     * @param targetImpl the TargetImpl from which to retrieve modules of the selected type
     * @param resultingTMIDs pre-instantiated List to which TargetModuleIDs will be added
     */
    private void addToTargetModuleIDs(TargetModuleID[] tmids, ModuleType type, TargetImpl targetImpl,
        Collection<TargetModuleID> resultingTMIDs) throws IOException {

        for (TargetModuleID tmid : tmids) {

            // Get the host name and port where the application was deployed
            HostAndPort webHost = deploymentFacility.getHostAndPort(targetImpl.getName(), tmid.getModuleID(), false);

            if (tmid instanceof TargetModuleIDImpl) {
                ((TargetModuleIDImpl) tmid).setModuleType(type);
            }
            resultingTMIDs.add(tmid);

            /*
             * Set additional information on the target module ID, depending on what type of
             * module this is. For J2EE apps, this includes constructing sub TargetModuleIDs.
             */
            try {
                if (type.equals(ModuleType.EAR)) {
                    setJ2EEApplicationTargetModuleIDInfo(tmid, webHost);
                } else if (type.equals(ModuleType.WAR)) {
                    setWebApplicationTargetModuleIDInfo(tmid, webHost);
                }
            } catch (Exception exp) {
                LOG.log(Level.WARNING, exp.getLocalizedMessage(), exp);
            }
        }
    }

    /**
     * Attach child target module IDs to a J2EE application target module ID.
     *
     * @param tmid the target module ID for the J2EE application.
     * @param targetImpl the target identifying which installation of this module is of interest
     * @param webHost the host and port for this target
     */
    private void setJ2EEApplicationTargetModuleIDInfo(TargetModuleID tmid, HostAndPort hostAndPort) {
        TargetImpl targetImpl = (TargetImpl) tmid.getTarget();
        try {
            List<String> subModuleInfoList = deploymentFacility.getSubModuleInfoForJ2EEApplication(tmid.getModuleID());
            for (String subModuleInfo : subModuleInfoList) {
                List<String> infoParts = StringUtils.parseStringList(subModuleInfo, ":");
                String subModuleName = infoParts.get(0);
                String subModuleID = tmid.getModuleID() + "#" + subModuleName;
                String subType = infoParts.get(1);
                ModuleType subModuleType = getModuleTypeFromType(subType);
                TargetModuleIDImpl childTmid = new TargetModuleIDImpl(targetImpl, subModuleID);
                childTmid.setModuleType(subModuleType);
                if (subType.equals("war")) {
                    URL webURL = new URL("http", hostAndPort.getHost(), hostAndPort.getPort(), infoParts.get(2));
                    childTmid.setWebURL(webURL.toExternalForm());
                }
                if (tmid instanceof TargetModuleIDImpl) {
                    ((TargetModuleIDImpl) tmid).addChildTargetModuleID(childTmid);
                }
            }
        } catch(Exception exp){
            LOG.log(Level.WARNING, exp.getLocalizedMessage(), exp);
        }
    }

    /**
     * Set additional type-specific information on the target module ID.
     *
     * @param tmid the target module ID for the Web app
     * @param targetImpl the target identifying which installation of this module is of interest
     * @param webHost the host and port for this target
     */
    private void setWebApplicationTargetModuleIDInfo(TargetModuleID tmid, HostAndPort webHost)
        throws MalformedURLException, IOException {

        String path = deploymentFacility.getContextRoot(tmid.getModuleID());
        if (!path.startsWith("/")) { //NOI18N
            path = "/" + path; //NOI18N
        }

        URL webURL = new URL("http", webHost.getHost(), webHost.getPort(), path); //NOI18N
        if (tmid instanceof TargetModuleIDImpl) {
            ((TargetModuleIDImpl)tmid).setWebURL(webURL.toExternalForm());
        }
    }

   /**
    * Retrieve the object that provides server-specific deployment
    * configuration information for the J2EE deployable component.
    *
    * @param dObj An object representing a J2EE deployable component.
    * @throws InvalidModuleException The DeployableObject is an
    *                      unknown or unsupport component for this
    *                      configuration tool.
    */
    @Override
    public DeploymentConfiguration createConfiguration(DeployableObject dObj) throws InvalidModuleException {
        SunDeploymentConfiguration deploymentConfiguration = new SunDeploymentConfiguration(dObj);
        deploymentConfiguration.setDeploymentManager(this);
        return deploymentConfiguration;
    }

    @Override
    public ProgressObject distribute(Target[] targetList, File moduleArchive, File deploymentPlan)
        throws IllegalStateException {
        return deploy(targetList, moduleArchive, deploymentPlan, null /* presetOptions */);
    }

    @Deprecated
    @Override
    public ProgressObject distribute(Target[] targetList, InputStream moduleArchive, InputStream deploymentPlan)
        throws IllegalStateException {
        return deploy(targetList, moduleArchive, deploymentPlan, null /* presetOptions */);
    }

    @Override
    public ProgressObject distribute(Target[] targetList, ModuleType type, InputStream moduleArchive,
        InputStream deploymentPlan) throws IllegalStateException {
        DFDeploymentProperties dProps = new DFDeploymentProperties();
        dProps.setProperty(DFDeploymentProperties.MODULE_EXTENSION, type.getModuleExtension());
        return deploy(targetList, moduleArchive, deploymentPlan, dProps);
    }

    @Override
    public ProgressObject start(TargetModuleID[] moduleIDList) throws IllegalStateException {
        return executeCommandUsingFacility(CommandType.START, moduleIDList);
    }

    @Override
    public ProgressObject stop(TargetModuleID[] moduleIDList) throws IllegalStateException {
        return executeCommandUsingFacility(CommandType.STOP, moduleIDList);
    }

    @Override
    public ProgressObject undeploy(TargetModuleID[] moduleIDList) throws IllegalStateException {
        return executeCommandUsingFacility(CommandType.UNDEPLOY, moduleIDList);
    }

    @Override
    public boolean isRedeploySupported() {
        return true;
    }

    @Override
    public ProgressObject redeploy(TargetModuleID[] moduleIDList, File moduleArchive, File deploymentPlan)
        throws UnsupportedOperationException, IllegalStateException {
        try {
            /*
             *To support multiple different modules in the module ID list, use a TargetModuleIDCollection to
             *organize them and work on each module one at a time.
             */
            TargetModuleIDCollection coll = new TargetModuleIDCollection(moduleIDList);
            for (Iterator<DeploymentFacilityModuleWork> it = coll.iterator(); it.hasNext();) {
                DeploymentFacilityModuleWork work = it.next();
                /*
                 *Set the name in the properties according to the moduleID.  The module is the same for all the
                 *targets represented by this single work object.
                 */
                DFDeploymentProperties options = getRedeployOptions(work.getModuleID());
                ProgressObject progress = deploy(work.targets(), moduleArchive, deploymentPlan, options);

                /*
                 *The work instance needs to know about its own progress object, and the
                 *aggregate progress object needs to also.
                 */
                work.setProgressObject(progress);
                coll.getProgressObjectSink().sinkProgressObject(progress);
            }
            return coll.getProgressObjectSink();
        } catch (Throwable e) {
            return prepareErrorProgressObject(CommandType.REDEPLOY, e);
        }
    }


    @Override
    public ProgressObject redeploy(TargetModuleID[] moduleIDList, InputStream moduleArchive, InputStream deploymentPlan)
        throws UnsupportedOperationException, IllegalStateException {
        try {
            /*
             *To support multiple different modules in the module ID list, use a TargetModuleIDCollection to
             *organize them and work on each module one at a time.
             */
            TargetModuleIDCollection coll = new TargetModuleIDCollection(moduleIDList);
            for (Iterator<DeploymentFacilityModuleWork> it = coll.iterator(); it.hasNext();) {
                /*
                 *The iterator returns one work instance for each module present in the collection.
                 */
                DeploymentFacilityModuleWork work = it.next();
                /*
                 *Set the name in the properties according to the moduleID.  The module is the same for all the
                 *targets represented by this single work object.
                 */
                DFDeploymentProperties options = getRedeployOptions(work.getModuleID());
                ProgressObject po = deploy(work.targets(), moduleArchive, deploymentPlan, options);

                /*
                 *The work instance needs to know about its own progress object, and the
                 *aggregate progress object needs to also.
                 */
                work.setProgressObject(po);
                coll.getProgressObjectSink().sinkProgressObject(po);
            }
            return coll.getProgressObjectSink();
        } catch (Throwable e) {
            return prepareErrorProgressObject(CommandType.REDEPLOY, e);
        }
    }

    @Override
    public void release() {
        // Make sure multiple releases are handled gracefully.
        if (!isDisconnected()) {
            deploymentFacility = null;
        }
    }

    @Override
    public Locale getDefaultLocale() {
        return defaultLocale;
    }

    @Override
    public Locale getCurrentLocale() {
        return currentLocale;
    }

    @Override
    public void setLocale(Locale locale) throws UnsupportedOperationException {
        for (Locale supportedLocale : supportedLocales) {
            if (supportedLocale == locale) {
                currentLocale = locale;
                return;
            }
        }
        throw new UnsupportedOperationException(
            localStrings.getLocalString("enterprise.deployapi.spi.localnotsupported", //NOI18N
                "Locale {0} is not supported", new Object[] {locale})); //NOI18N
    }

    @Override
    public Locale[] getSupportedLocales() {
        return supportedLocales;
    }

    @Override
    public boolean isLocaleSupported(Locale locale) {
        Locale[] locales = getSupportedLocales();
        for (Locale locale2 : locales) {
            if (locale2.equals(locale)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public DConfigBeanVersionType getDConfigBeanVersion() {
        return DConfigBeanVersionType.V5;
    }

   @Override
   public boolean isDConfigBeanVersionSupported(DConfigBeanVersionType version) {
       return version.getValue()==getDConfigBeanVersion().getValue();
   }

   @Override
    public void setDConfigBeanVersion(DConfigBeanVersionType version) throws DConfigBeanVersionUnsupportedException {
       if (!isDConfigBeanVersionSupported(version)) {
           throw new DConfigBeanVersionUnsupportedException(
            localStrings.getLocalString(
                "enterprise.deployapi.spi.dconfigbeanversionnotsupported", //NOI18N
                "DConfigBean version {0} is not supported",  //NOI18N
                new Object[] {version.toString()}));
       }
   }


    /**
     * Return deployment options for the DeploymentFacility preset for the needs of redeployment.
     * These properties will be merged with and will override the options set for normal deployment.
     *
     * @return Properties with the conventional preset properties for redeployment
     */
    private DFDeploymentProperties getRedeployOptions(String moduleID) {
        DFDeploymentProperties deplProps = new DFDeploymentProperties();
        deplProps.setForce(true);
        deplProps.setName(moduleID);
        deplProps.setRedeploy(true);
        return deplProps;
   }


    /**
     * Deploy the specified module to the list of targets.
     * The deployment plan archive can be null.
     *
     * @param targetList the targets to which to deploy the module
     * @param moduleStream the archive stream to be deployed
     * @param deploymentPlanStream the (optional) deployment plan stream
     * @param presetOptions options set by the caller to override and augment any settings made here
     * @return ProgressObject to communicate progress and results of the deployment
     * @throws IllegalStateException if the DeploymentManager has disconnected
     * @throws IOException if there are problems working with the input streams
     */
    private ProgressObject deploy(Target[] targetList, InputStream moduleStream, InputStream deploymentPlanStream,
        Properties presetOptions) throws IllegalStateException {

        /*
         *Create archives for the module's input stream and, if present, the deployment plan's
         *input stream, and then delegate to the variant of deploy that accepts archives as
         *arguments.
         */
        MemoryMappedArchive moduleArchive = null;
        MemoryMappedArchive deploymentPlanArchive = null;

        try {
            moduleArchive = new MemoryMappedArchive(moduleStream);
            if (deploymentPlanStream != null) {
                deploymentPlanArchive = new MemoryMappedArchive(deploymentPlanStream);
            }
            return deploy(targetList, moduleArchive, deploymentPlanArchive, presetOptions);
        } catch (Throwable e) {
            String msg = localStrings.getLocalString(
                "enterprise.deployapi.spi.errpreparearchstream",
                "Could not prepare archives for module and/or deployment plan input streams");
            IllegalArgumentException ex = new IllegalArgumentException(msg);
            ex.initCause(e);
            return prepareErrorProgressObject(CommandType.DISTRIBUTE, ex);
        }
    }


    /**
     * Deploy the specified module to the list of targets.
     * The deployment plan archive can be null.
     *
     * @param targetList the targets to which to deploy the module
     * @param moduleArchive the archive file to be deployed
     * @param deploymentPlan the (optional) deployment plan file
     * @param options set by the caller to override and augment any settings made here
     * @return ProgressObject to communicate progress and results of the deployment
     * @throws IllegalStateException if the DeploymentManager has disconnected
     * @throws IOException if there are problems opening the archive files
     */
    private ProgressObject deploy(Target[] targetList, File moduleArchive, File deploymentPlan, Properties options)
        throws IllegalStateException {

        /*
         *Create archives for the module file and, if present, the deployment plan file, and
         *then delegate to the variant of deploy that accepts archives as arguments.
         */
        ReadableArchive appArchive = null;
        ReadableArchive planArchive = null;
        ArchiveFactory archiveFactory = getArchiveFactory();

        try {
            appArchive = archiveFactory.openArchive(moduleArchive);

            if (deploymentPlan != null && deploymentPlan.length() != 0) {
                planArchive = archiveFactory.openArchive(deploymentPlan);
                if (planArchive == null) {
                    throw new IllegalArgumentException(localStrings.getLocalString(
                        "enterprise.deployapi.spi.noarchivisthandlesplan",
                        "No archivist is able to handle the deployment plan {0}",
                        new Object [] {deploymentPlan.getAbsolutePath()}
                    ));
                }
            }

            ProgressObject po = deploy(targetList, appArchive, planArchive, options);
            return po;
        } catch (Exception se) {
            String msg = localStrings.getLocalString(
                "enterprise.deployapi.spi.errpreparearchfile",
                "Could not prepare archives for module and/or deployment plan files");
            IllegalArgumentException ex = new IllegalArgumentException(msg);
            ex.initCause(se);
            return prepareErrorProgressObject(CommandType.DISTRIBUTE, ex);
        } finally {
            closeArchives(CommandType.DISTRIBUTE, appArchive, moduleArchive.getAbsolutePath(), planArchive,
                (deploymentPlan != null) ? deploymentPlan.getAbsolutePath() : null);
        }
    }


    /**
     * Deploy the specified module to the list of targets.
     * The deployment plan archive can be null.
     *
     * @param targetList the targets to which to deploy the module
     * @param moduleArchive the archive to be deployed
     * @param planArchive the (optional) deployment plan
     * @param options set by the caller to override and augment any settings made here
     * @return ProgressObject to communicate progress and results of the deployment
     * @throws IllegalStateException if the DeploymentManager has disconnected
     */
    private ProgressObject deploy(Target[] targetList, ReadableArchive moduleArchive, ReadableArchive planArchive,
        Properties presetOptions) throws IllegalStateException {

        verifyConnected();

        ProgressObject progressObj = null;

        try {
            DFDeploymentProperties options = getProperties();

            // If any preset options were specified by the caller, use them to
            // override or augment the just-assigned set.
            if (presetOptions != null) {
                options.putAll(presetOptions);
            }
            progressObj = deploymentFacility.deploy(targetList, moduleArchive, planArchive, options);

        } catch (Throwable e) {
            // Prepare a progress object with a deployment status "wrapper" around this exception.
            progressObj = prepareErrorProgressObject(CommandType.DISTRIBUTE, e);
        }
        return progressObj;
    }


    /**
     * Closes the module archive and the plan archive, if any, preparing a ProgressObject if any
     * error occurred.
     *
     * @param commandType the CommandType in progress - used in preparing the progress object
     *            (if needed)
     * @param moduleArchive the main module archive to be closed
     * @param moduleArchiveSpec a String representation of the main module archive
     * @param planArchive the deployment plan archive (if any) to be closed
     * @param planArchiveSpec a String representation of the deployment plan archive (if any)
     *            to be closed
     * @return ProgressObject an error progress object if any error ocurred trying to close
     *         the archive(s)
     */
    private ProgressObject closeArchives(CommandType commandType, ReadableArchive moduleArchive, String moduleArchiveSpec, ReadableArchive planArchive, String planArchiveSpec) {
        ProgressObject errorPO = null;

        IOException moduleIOE = closeArchive(moduleArchive);
        IOException planIOE = closeArchive(planArchive);

        IOException excForProgressObject = null;
        String errorMsg = null;
        /*
         *If the module could not be closed, record the IOException resulting from the attempt for
         *use in the error progress object returned to the caller.
         */
        if (moduleIOE != null) {
            excForProgressObject = moduleIOE;
            /*
             *If there was a problem with both the module archive and the plan archive,
             *compose an appropriate message that says both failed.
             */
            if (planIOE != null) {
                errorMsg = localStrings.getLocalString(
                        "enterprise.deployapi.spi.errclosearchs",
                        "Could not close module archive {0} or deployment plan archive {1}",
                        new Object[] {moduleArchiveSpec, planArchiveSpec}
                );
            } else {
                /*
                 *Either the plan was closed or there was no plan to close.  To build
                 *a message about only the module archive.
                 */
                errorMsg = localStrings.getLocalString(
                        "enterprise.deployapi.spi.errclosemodulearch",
                        "Could not close module archive {0}",
                        new Object[] {moduleArchiveSpec}
                );
            }
        } else if (planIOE != null) {
            /*
             *The module archive was closed fine.  If the plan archive exists and
             *could not be closed, compose an error message to that effect and
             *record the IOException that occurred during the attempt to close the
             *deployment plan archive for use in the error progress object returned
             *to the caller.
             */
            excForProgressObject = planIOE;
            errorMsg = localStrings.getLocalString(
                    "enterprise.deployapi.spi.errcloseplanarch",
                    "Could not close deployment plan archive {0}",
                    new Object[] {planArchiveSpec}
            );
        }

        /*
         *If an error occurred trying to close either archive, build an error progress object
         *for return to the caller.
         */
        if (errorMsg != null) {
            IOException ioe = new IOException(errorMsg);
            ioe.initCause(excForProgressObject); // Only reflects the module exception if both occurred, but the msg describes both.
            errorPO = prepareErrorProgressObject(commandType, ioe);
        }

        return errorPO;
    }


    /**
     * Closes the specified archive, returning any IOException encountered in the process.
     *
     * @param archive the archive to be closed
     * @return IOException describing any error; null if the close succeeded
     */
    private IOException closeArchive(ReadableArchive archive) {
        IOException errorIOE = null;
        if (archive != null) {
            try {
                archive.close();
            } catch (IOException ioe) {
                errorIOE = ioe;
            }
        }
        return errorIOE;
    }


    /**
     * Perform the selected command on the DeploymentFacility using the specified target module IDs.
     * <p>
     * Several of the deployment facility methods have the same signature except for the name.
     * This method collects the pre- and post-processing around those method calls in one place,
     * then chooses which of the deployment facility methods to actually invoke based on
     * the command type provided as an argument.
     *
     * @param commandType selects which method should be invoked
     * @param moduleIDList array of TargetModuleID to be started
     * @throws IllegalArgumentException if the command type is not supported
     * @throws IllegalStateException if the deployment manager had been released previously
     */
     private ProgressObject executeCommandUsingFacility(
        CommandType commandType, TargetModuleID[] targetModuleIDList)
        throws IllegalStateException {

         verifyConnected();
       try {
        /*
         *Create a temporary collection based on the target module IDs to make it easier to deal
         *with the different modules and the set of targets.
         */
        TargetModuleIDCollection coll = new TargetModuleIDCollection(targetModuleIDList);

        /*
         *For each distinct module ID present in the list, ask the deployment facility to
         *operate on that module on all the relevant targets.
         */

        for (Iterator<DeploymentFacilityModuleWork> it = coll.iterator(); it.hasNext();) {
            /*
             *The iterator returns one work instance for each module present in the collection.
             *Each work instance reflects one invocation of a method on the DeploymentFacility.
             */
            DeploymentFacilityModuleWork work = it.next();
            ProgressObject po = null;

            if (commandType.equals(CommandType.START)) {
                po = deploymentFacility.enable(work.targets(), work.getModuleID());

            } else if (commandType.equals(CommandType.STOP)) {
                po = deploymentFacility.disable(work.targets(), work.getModuleID());

            } else if (commandType.equals(CommandType.UNDEPLOY)) {
                po = deploymentFacility.undeploy(work.targets(), work.getModuleID());

            } else {
                throw new IllegalArgumentException(localStrings.getLocalString(
                    "enterprise.deployapi.spi.unexpcommand",
                    "Received unexpected deployment facility command ${0}",
                    new Object [] {commandType.toString()}
                    ));
            }

            /*
             *The new work instance needs to know about its own progress object, and the
             *aggregate progress object needs to also.
             */
            work.setProgressObject(po);
            coll.getProgressObjectSink().sinkProgressObject(po);
        }

        /*
         *Return the single progress object to return to the caller.
         */
        return coll.getProgressObjectSink();

      } catch (Throwable thr) {
        return prepareErrorProgressObject(commandType, thr);
      }
    }

    /**
     * Prepare a ProgressObject that reflects an error, with a related Throwable cause.
     *
     * @param commandType being processed at the time of the error
     * @param throwable that occurred
     * @return ProgressObject set to FAILED with linked cause reporting full error info
     */
    private ProgressObject prepareErrorProgressObject (CommandType commandType, Throwable thr) {
        DeploymentStatus ds = new DeploymentStatusImplWithError(CommandType.DISTRIBUTE, thr);
        SimpleProgressObjectImpl progressObj = new SimpleProgressObjectImpl(ds);
        ProgressEvent event = new ProgressEvent(progressObj, null /*targetModuleID */, ds);
        progressObj.fireProgressEvent(event);
        return progressObj;
    }

    /**
     * Creates new properties; enabled is set to false, nothing else is set.
     *
     * @return {@link DFDeploymentProperties}
     */
    protected DFDeploymentProperties getProperties() {
        // we don't set name from client side and will let server side
        // determine it
        DFDeploymentProperties dProps = new DFDeploymentProperties();
        dProps.setEnabled(false);
        return dProps;
    }

    private void prepareHabitat() {
        ModulesRegistry registry = new StaticModulesRegistry(getClass().getClassLoader());
        ServiceLocator serviceLocator = registry.createServiceLocator("default");
        habitat = serviceLocator.getService(ServiceLocator.class);

        StartupContext startupContext = new StartupContext();
        ServiceLocatorUtilities.addOneConstant(habitat, startupContext);
        ServiceLocatorUtilities.addOneConstant(habitat, new ProcessEnvironment(ProcessEnvironment.ProcessType.Other));
    }

    private ArchiveFactory getArchiveFactory() {
        return habitat.getService(ArchiveFactory.class);
    }

    /**
     * Organizes the target module IDs passed by a JSR88 client for easy processing one module ID
     * at a time.
     * <p>
     * Several methods in the JSR88 DeploymentManager interface accept a list of TargetModuleID
     * values, and these lists can refer to multiple module IDs and multiple targets.
     * Each invocation of a DeploymentFacility method, on the other hand, can work on only a single
     * module although with perhaps multiple targets. This class provides a central way
     * of organizing the target module IDs as passed from the JSR88 client and making
     * the information for a single module ID readily available.
     * <p>
     * Typically, a client will use three methods:
     * <ul>
     * <li>the constructor - pass a TargetModuleID array as supplied by a client
     * <li>the iterator() method, which the client uses to step through
     * the DeploymentFacilityModuleWork instances, each representing a single module and perhaps
     * multiple targets.
     * <li>the getProgressObjectSink which returns the aggregator for the ProgressObjects
     * from each work element
     * </ul>
     */
    protected static class TargetModuleIDCollection {
        /** Maps the module ID to that module's instance of DeploymentFacilityModuleWork. */
        private final HashMap<String, DeploymentFacilityModuleWork> moduleIDToInfoMap = new HashMap<>();

        /** Collects together the individual progress objects into a single aggregate one. */
        private ProgressObjectSink progressObjectSink;


        /**
         * Create a new instance of TargetModuleIDCollection.
         * Accept the array of targetModuleIDs as passed by the JSR88 client and set up
         * the internal data structures.
         *
         * @param targetModuleIDs array of {@link TargetModuleID TargetModuleID} provided from
         *            the calling JSR88 client
         * @throws IllegalArgumentException unsupported target implementation
         */
        public TargetModuleIDCollection(TargetModuleID[] targetModuleIDs) throws IllegalArgumentException {

            for (TargetModuleID targetModuleID : targetModuleIDs) {
                /*
                 *Make sure that this target module ID has a target that is a TargetImpl and was created by this DM.
                 */
                Target candidateTarget = targetModuleID.getTarget();
                if (!(candidateTarget instanceof TargetImpl)) {
                    throw new IllegalArgumentException(
                    localStrings.getLocalString("enterprise.deployapi.spi.nott", //NOI18N
                        "Expected TargetImpl instance but found instance of {0}", new Object[] {candidateTarget.getClass().getName() } )); //NOI18N
                }
                String moduleID = targetModuleID.getModuleID();

                /*
                 *Look for the entry in the hash map for this module.
                 */
                DeploymentFacilityModuleWork work = moduleIDToInfoMap.get(moduleID);
                if (work == null) {
                    /*
                     *This module ID is not yet in the map.  Add a work instance for it with the module ID as the key.
                     */
                    work = new DeploymentFacilityModuleWork(moduleID);
                    moduleIDToInfoMap.put(moduleID, work);
                }
                /*
                 *Either the entry already exists or one has been created.
                 *In either case, add the target to the work to be done with this module.
                 */
                work.addTarget(candidateTarget);
            }
        }

        /**
         * Provides an Iterator over the module work items in the collection.
         * The iterator provides one element for each distinct module that appeared in the original
         * array of TargetModuleIDs.
         *
         * @return Iterator over the DeploymentFacilityModuleWork elements in the collection
         */
        public Iterator<DeploymentFacilityModuleWork> iterator() {
            return moduleIDToInfoMap.values().iterator();
        }

        /**
         * Reports the number of elements in the collection.
         * This is also a measure of the number of distinct module IDs specified
         * in the TargetModuleID array passed to the constructor of the collection.
         *
         * @return the number of DeploymentFacilityModuleWork elements contained in the collection
         */
        public int size() {
            return moduleIDToInfoMap.size();
        }

        /**
         * Returns the aggregate progress object for the collection.
         * Creates a new ProgressObjectSink if needed.
         *
         * @return {@link ProgressObjectSink}
         */
        public ProgressObjectSink getProgressObjectSink() {
            if (progressObjectSink == null) {
                progressObjectSink = new ProgressObjectSink();
            }
            return progressObjectSink;
        }
    }

    /**
     * Encapsulates information used with a single invocation of a DeploymentFacility method--
     * that is, one item of "work" the DeploymentFacility is being asked to perform.
     * This includes the single target ID of interest (because the DF methods operate on a
     * single module), a collection of all the targets to be included in the operation on that
     * module, and the progress object resulting from the DF method invocation.
     */
    protected static class DeploymentFacilityModuleWork {

        /** The module ID this work handles */
        private String moduleID = null;

        /** The targets this work should affect. */
        private final Collection<Target> targets = new Vector<>();

        /** The ProgressObject for this work returned by the DeploymentFacility method invocation. */
        private ProgressObject progressObject = null;

        /**
         * Creates a new instance of DeploymentFacilityModuleWork.
         *
         * @param moduleID the module ID common to all work recorded in this instance
         */
        public DeploymentFacilityModuleWork(String moduleID) {
            this.moduleID = moduleID;
        }


        /**
         * Adds a target to the collection of targets for the work to be done for this distinct
         * module.
         *
         * @param target the {@link Target Target} to be added for this module
         */
        public void addTarget(Target target) {
            if ( ! (target instanceof TargetImpl) ) {
                throw new IllegalArgumentException(localStrings.getLocalString(
                    "enterprise.deployapi.spi.unexptargettyp",
                    "Target must be of type TargetImpl but encountered {0}",
                    new Object [] {target.getClass().getName()}
                ));
            }
            targets.add(target);
        }

        /**
         * Returns an array of {@link Target Target} instances recorded for
         * this module. Note the return of an array of runtime type TargetImpl[].
         *
         * @return array of Target
         */
        public Target [] targets() {
            return targets.toArray(new TargetImpl[targets.size()]);
        }


        /**
         * Returns the {@link ProgressObject ProgressObject} that the DeploymentFacility method
         * returned when it was invoked.
         *
         * @return the ProgressObject
         */
        public ProgressObject getProgressObject() {
            return this.progressObject;
        }


        /**
         * Records the {@link ProgressObject ProgressObject} that the DeploymentFacility returned
         * when its method was invoked.
         *
         * @param progressObject the ProgressObject provided by the DeploymentFacility method
         */
        public void setProgressObject(ProgressObject progressObject) {
            this.progressObject = progressObject;
        }

        /**
         *Reports the module ID for this instance of DeploymentFacilityModuleWork
         *@return the module ID
         */
        public String getModuleID() {
            return this.moduleID;
        }
    }
}
