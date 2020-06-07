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
// Portions Copyright [2019] [Payara Foundation and/or its affiliates]

package org.glassfish.kernel.embedded;

import com.sun.enterprise.config.serverbeans.*;
import com.sun.enterprise.deploy.shared.ArchiveFactory;
import com.sun.enterprise.util.io.FileUtils;
import com.sun.enterprise.admin.report.PlainTextActionReporter;
import java.beans.PropertyVetoException;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.CommandRunner;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.deployment.DeployCommandParameters;
import org.glassfish.api.deployment.UndeployCommandParameters;
import org.glassfish.api.deployment.archive.*;
import org.glassfish.deployment.common.*;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.data.*;
import org.glassfish.internal.deployment.Deployment;
import org.glassfish.internal.deployment.ExtendedDeploymentContext;
import org.glassfish.internal.deployment.SnifferManager;
import org.glassfish.internal.embedded.*;
import org.glassfish.internal.embedded.Server;
import org.glassfish.kernel.KernelLoggerInfo;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

/**
 * @author Jerome Dochez
 */
@Service
public class EmbeddedDeployerImpl implements EmbeddedDeployer {

    @Inject
    Deployment deployment;

    @Inject
    Server server;

    @Inject
    CommandRunner commandRunner;

    @Inject
    ServiceLocator habitat;

    @Inject
    ArchiveFactory factory;

    @Inject
    SnifferManager snifferMgr;

    @Inject
    ServerEnvironment env;

    @Inject
    DasConfig config;

    Map<String, EmbeddedDeployedInfo> deployedApps = new HashMap<String, EmbeddedDeployedInfo>();

    final static Logger logger = KernelLoggerInfo.getLogger();

    @Override
    public File getApplicationsDir() {
        return env.getApplicationRepositoryPath();
    }

    @Override
    public File getAutoDeployDir() {
        return new File(env.getInstanceRoot(), config.getAutodeployDir());
    }

    @Override
    public void setAutoDeploy(final boolean flag) {

        String value = config.getAutodeployEnabled();
        boolean active = value!=null && Boolean.parseBoolean(
                config.getAutodeployEnabled());
        if (active!=flag) {
            try {
                ConfigSupport.apply(new SingleConfigCode<DasConfig>() {
                    @Override
                    public Object run(DasConfig dasConfig) throws PropertyVetoException, TransactionFailure {
                        dasConfig.setAutodeployEnabled(Boolean.valueOf(flag).toString());
                        return null;
                    }
                }, config);
            } catch(TransactionFailure e) {
                logger.log(Level.SEVERE, KernelLoggerInfo.exceptionAutodeployment, e);
            }
        }
    }

    @Override
    public String deploy(File archive, DeployCommandParameters params) {
        try {
            ReadableArchive r = factory.openArchive(archive);
            return deploy(r, params);
        } catch (IOException e) {
            logger.log(Level.SEVERE, KernelLoggerInfo.deployException, e);
        }

        return null;
    }

    @Override
    public String deploy(ReadableArchive archive, DeployCommandParameters params) {

        // ensure server is started. start it if not started.
        try {
            server.start();
        } catch (LifecycleException e) {
            throw new RuntimeException(e);
        }

        ActionReport report = new PlainTextActionReporter();
        if (params==null) {
            params = new DeployCommandParameters();
        }
        ExtendedDeploymentContext initialContext = new DeploymentContextImpl(report, archive, params, env);
        ArchiveHandler archiveHandler = null;
        try {
            archiveHandler = deployment.getArchiveHandler(archive);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (archiveHandler==null) {
                throw new RuntimeException("Cannot find archive handler for source archive");
        }
        if (params.name==null) {
                params.name = archiveHandler.getDefaultApplicationName(archive, initialContext);
            }
        ExtendedDeploymentContext context = null;
        try {
            context = deployment.getBuilder(logger, params, report).source(archive).archiveHandler(archiveHandler).build(initialContext);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if(params.property != null){
            context.getAppProps().putAll(params.property);
        }

        if(params.properties != null){
            context.getAppProps().putAll(params.properties);        
        }

        ApplicationInfo appInfo = null;
        try {
            appInfo = deployment.deploy(context);
        } catch(Exception e) {
            logger.log(Level.SEVERE, KernelLoggerInfo.deployException, e);
        }
        if (appInfo!=null) {
            boolean isDirectory = new File(archive.getURI().getPath()).isDirectory();
            EmbeddedDeployedInfo info = new EmbeddedDeployedInfo(appInfo, context.getModulePropsMap(), context.getAppProps(),
                    isDirectory);
            deployedApps.put(appInfo.getName(), info);
            return appInfo.getName();
        }
        return null;
    }

    @Override
    public void undeploy(String name, UndeployCommandParameters params) {

        ActionReport report = habitat.getService(ActionReport.class, "plain");
        EmbeddedDeployedInfo info = deployedApps.get(name);
        ApplicationInfo appInfo  = info!=null?info.appInfo:null;
        if (appInfo==null) {
            appInfo = deployment.get(name);
        }
        if (appInfo == null) {
            report.setMessage(
                "Cannot find deployed application of name " + name);
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;            
        }

        ReadableArchive source = appInfo.getSource();
        if (source == null) {
            report.setMessage(
                "Cannot get source archive for undeployment");
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }

        if (params==null) {
            params = new UndeployCommandParameters(name);
        }
        params.origin = UndeployCommandParameters.Origin.undeploy;
        
        ExtendedDeploymentContext deploymentContext;
        try {
            deploymentContext = deployment.getBuilder(logger, params, report).source(source).build();

            if (info!=null) {
                for (ModuleInfo module : appInfo.getModuleInfos()) {
                    info.map.put(module.getName(), module.getModuleProps());
                    deploymentContext.getModuleProps().putAll(module.getModuleProps());
                }
                deploymentContext.setModulePropsMap(info.map);
                deploymentContext.getAppProps().putAll(info.appProps);
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Cannot create context for undeployment ", e);
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }


        deployment.undeploy(name, deploymentContext);


        if (report.getActionExitCode().equals(ActionReport.ExitCode.SUCCESS)) {
            if (params.keepreposdir == null) {
                params.keepreposdir = false;
            }
            if ( !params.keepreposdir && info != null && !info.isDirectory && source.exists()) {
                FileUtils.whack(new File(source.getURI()));
            }
            //remove context from generated
            deploymentContext.clean();

        }
        
    }

    @Override
    public void undeployAll() {
        for (String appName : deployedApps.keySet()) {
            undeploy(appName, null);
        }

    }

    private final static class EmbeddedDeployedInfo {
        final ApplicationInfo appInfo;
        final Map<String, Properties> map;
        final boolean isDirectory;
        Properties appProps;


        public EmbeddedDeployedInfo(ApplicationInfo appInfo, Map<String, Properties> map, Properties appProps,
                boolean isDirectory) {
            this.appInfo = appInfo;
            this.map = map;
            this.appProps = appProps;
            this.isDirectory = isDirectory;
        }
    }
}
