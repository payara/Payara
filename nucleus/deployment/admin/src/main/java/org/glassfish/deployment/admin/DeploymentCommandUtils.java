/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.deployment.admin;

import com.sun.enterprise.admin.util.ClusterOperationUtil;
import com.sun.enterprise.config.serverbeans.Application;
import com.sun.enterprise.config.serverbeans.ApplicationRef;
import com.sun.enterprise.config.serverbeans.Applications;
import com.sun.enterprise.util.io.FileUtils;
import com.sun.enterprise.config.serverbeans.Cluster;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.Server;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Logger;
import java.util.logging.Level;
import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.FailurePolicy;
import org.glassfish.api.admin.ParameterMap;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.deployment.OpsParams;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.deployment.Deployment;
import org.glassfish.common.util.admin.ParameterMapExtractor;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.glassfish.api.admin.AccessRequired;
import org.glassfish.deployment.common.DeploymentUtils;
import org.jvnet.hk2.config.ConfigBeanProxy;

/**
 * Utility methods useful from deployment-related commands.
 *
 * @author Tim Quinn
 */
public class DeploymentCommandUtils {

    public final static String APPLICATION_RESOURCE_NAME = "domain/applications/application";
    
    final static String LIBRARY_SECURITY_RESOURCE_PREFIX = "domain/libraries/";
    final static String CLUSTERS_RESOURCE_NAME = "domain/clusters/cluster";
    final static String SERVERS_RESOURCE_NAME = "domain/servers/server";
    
    final private static String COPY_IN_PLACE_ARCHIVE_PROP_NAME = "copy.inplace.archive";
    
    private static final List<String> LIST_CONTAINING_DOMAIN = new ArrayList<String>(Arrays.asList(DeploymentUtils.DOMAIN_TARGET_NAME));

    /**
     * Replicates an enable or disable command to all instances in the cluster
     * of which the target is a member.  If the target is not cluster member
     * this method is a no-op.
     * @param commandName name of the command to replicate to cluster instances
     * @param domain domain containing the relevant configuration
     * @param target name of the target being enabled or disabled
     * @param appName name of the application being enabled or disabled
     * @param habitat hk2 habitat
     * @param context command context passed to the running enable or disable command 
     * @param command command object
     * @return
     */
    public static ActionReport.ExitCode replicateEnableDisableToContainingCluster(
            final String commandName,
            final Domain domain,
            final String target,
            final String appName,
            final ServiceLocator habitat,
            final AdminCommandContext context,
            final AdminCommand command) throws IllegalArgumentException, IllegalAccessException {
        /*
         * If the target is a cluster instance, the DAS will broadcast the command
         * to all instances in the cluster so they can all update their configs.
         */
        final Cluster containingCluster = domain.getClusterForInstance(target);
        if (containingCluster != null) {
            final ParameterMapExtractor extractor = new ParameterMapExtractor(command);
            final ParameterMap pMap = extractor.extract(Collections.EMPTY_LIST);
            pMap.set("DEFAULT", appName);

            return ClusterOperationUtil.replicateCommand(
                    commandName,
                    FailurePolicy.Error,
                    FailurePolicy.Warn,
                    FailurePolicy.Ignore,
                    containingCluster.getInstances(),
                    context,
                    pMap,
                    habitat);
        }
        return ActionReport.ExitCode.SUCCESS;
    }

    public static String getLocalHostName() {
        String defaultHostName = "localhost";
        try {
            InetAddress host = InetAddress.getLocalHost();
            defaultHostName = host.getCanonicalHostName();
        } catch(UnknownHostException uhe) {
           // ignore
        }
        return defaultHostName;
    }

    public static String getTarget(ParameterMap parameters, OpsParams.Origin origin, Deployment deployment) {
        String appName = parameters.getOne("DEFAULT");
        Boolean isClassicStyle = Boolean.valueOf(parameters.getOne("_classicstyle"));
        String targetName = deployment.getDefaultTarget(appName, origin, isClassicStyle);
        parameters.set("target", targetName);
        return targetName;
    }

    public static File renameUploadedFileOrCopyInPlaceFile(
            final File finalUploadDir,
            final File fileParam,
            final Logger logger,
            ServerEnvironment env) throws IOException {
        if (fileParam == null) {
            return null;
        }
        /*
         * If the fileParam resides within the applications directory then
         * it has been uploaded.  In that case, rename it.
         */
        final File appsDir = env.getApplicationRepositoryPath();

        /*
         * The default answer is the in-place file, to handle the
         * directory-deployment case or the in-place archive case if we ae
         * not copying the in-place archive.
         */
        File result = fileParam;

        if ( ! fileParam.isDirectory() && ! appsDir.toURI().relativize(fileParam.toURI()).isAbsolute()) {
            /*
             * The file lies within the apps directory, so it was
             * uploaded.
             */
            result = new File(finalUploadDir, fileParam.getName());
            final long lastMod = fileParam.lastModified();
            FileUtils.renameFile(fileParam, result);
            if ( ! result.setLastModified(lastMod)) {
                    logger.log(Level.FINE, "In renaming {0} to {1} could not setLastModified; continuing",
                            new Object[] {fileParam.getAbsolutePath(),
                                result.getAbsolutePath()
                            });
            }
        } else {
            final boolean copyInPlaceArchive = Boolean.valueOf(
                    System.getProperty(COPY_IN_PLACE_ARCHIVE_PROP_NAME, "true"));
            if ( ! fileParam.isDirectory() && copyInPlaceArchive) {
                /*
                 * The file was not uploaded and the in-place file is not a directory,
                 * so copy the archive to the permanent location.
                 */
                final long startTime = System.currentTimeMillis();
                result = new File(finalUploadDir, fileParam.getName());
                FileUtils.copy(fileParam, result);
                if ( ! result.setLastModified(fileParam.lastModified())) {
                    logger.log(Level.FINE, "Could not set lastModified for {0}; continuing",
                            result.getAbsolutePath());
                }
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "*** In-place archive copy of {0} took {1} ms",
                            new Object[]{
                                fileParam.getAbsolutePath(),
                                System.currentTimeMillis() - startTime});
                }
            }
        }
        return result;
    }

    private static StringBuilder getTargetResourceName(final Domain d,
                final String target) {
        final StringBuilder sb = new StringBuilder();
        ConfigBeanProxy p = d.getServerNamed(target);
        if (p == null) {
            p = d.getClusterNamed(target);
        }
        if (p == null) {
            sb.append("domain/???/").append(target);
        } else {
            sb.append(AccessRequired.Util.resourceNameFromConfigBeanProxy(p));
        }
        return sb;
    }

    /**
     * Prepares AccessChecks for an application already deployed to one or
     * more targets, returning an access check for the application itself and
     * access checks for each matching version on whatever targets to which
     * it is assigned.
     * @param domain
     * @param applications
     * @param target
     * @param matchedVersions
     * @param appAction
     * @param appRefAction
     * @return 
     */
    public static Collection<? extends AccessRequired.AccessCheck> getAccessChecksForExistingApp(
            final Domain domain, final Applications applications,
            final String target, final Collection<String> matchedVersions,
            final String appAction, final String appRefAction) {
        final List<AccessRequired.AccessCheck> accessChecks = new ArrayList<AccessRequired.AccessCheck>();
        
        final List<String> targets = domain.getTargets(target);
        for (String mv : matchedVersions) {
            final Application app = applications.getApplication(mv);
            if (app == null) {
                continue;
            }
            accessChecks.add(new AccessRequired.AccessCheck(getResourceNameForExistingApp(domain, mv), appAction));
            for (String t : targets) {
                final ApplicationRef ar = domain.getApplicationRefInTarget(mv, t);
                if (ar != null) {
                    accessChecks.add(new AccessRequired.AccessCheck(
                            getTargetResourceNameForExistingAppRef(domain, t, mv), appRefAction));
                }
            }
        }
        return accessChecks;
    }
    
    /**
     * Returns access checks for a new application (not already deployed) and
     * for the corresponding app ref(s) given the specified target.  This method
     * does no target expansion in creating the access checks.
     * @param domain
     * @param applications
     * @param target
     * @param action
     * @return 
     */
    public static Collection<? extends AccessRequired.AccessCheck> getAccessChecksForNewApp(
            final Domain domain, final Applications applications,
            final String target, 
            final String action) {
        final List<AccessRequired.AccessCheck> accessChecks = new ArrayList<AccessRequired.AccessCheck>();
        accessChecks.add(new AccessRequired.AccessCheck(getResourceNameForApps(domain), action));
        accessChecks.add(new AccessRequired.AccessCheck(getTargetResourceNameForNewAppRef(domain, target), action));
        return accessChecks;
    }
    
    public static String getTargetResourceNameForNewApp(
            final Domain d, final String target) {
        final StringBuilder sb = getTargetResourceName(d, target);
        return sb.toString();
    }

    public static String getTargetResourceNameForExistingApp(
            final Domain d, final String target, final String appName) {
        final ApplicationRef appRef = d.getApplicationRefInTarget(appName, target);
        if (appRef != null) {
            return AccessRequired.Util.resourceNameFromConfigBeanProxy(appRef);
        }
        return null;
    }
    
    public static String getTargetResourceNameForNewAppRef(
            final Domain d, final String target) {
        return new StringBuilder(getTargetResourceNameForNewApp(d, target)).append("/application-ref").toString();
    }
    
    public static String getTargetResourceNameForNewAppRef(
            final Domain d, final String target, final String appName) {
        return new StringBuilder(getTargetResourceNameForNewAppRef(d, target)).append('/').append(appName).toString();
    }
    
    public static String getTargetResourceNameForExistingAppRef(
            final Domain d, final String target, final String appName) {
        return AccessRequired.Util.resourceNameFromConfigBeanProxy(d.getApplicationRefInTarget(appName, target));
    }
    
    public static String getResourceNameForApps(
            final Domain d) {
        return APPLICATION_RESOURCE_NAME;
    }
    
    public static String getResourceNameForNewApp(
            final Domain d, final String appName) {
        return new StringBuilder(APPLICATION_RESOURCE_NAME).append('/').append(appName).toString();
    }
    
    public static String getResourceNameForExistingApp(
            final Domain d, final String appName) {
        final Application app = d.getApplications().getApplication(appName);
        if (app != null) {
            return AccessRequired.Util.resourceNameFromConfigBeanProxy(app);
        }
        return null;
    }
}
