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

// Portions Copyright [2019] [Payara Foundation and/or its affiliates]

package com.sun.enterprise.v3.server;

import com.sun.enterprise.module.bootstrap.ModuleStartup;
import com.sun.enterprise.module.bootstrap.StartupContext;
import com.sun.enterprise.admin.report.DoNothingActionReporter;
import com.sun.enterprise.config.serverbeans.Applications;
import com.sun.enterprise.config.serverbeans.Application;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.config.serverbeans.ServerTags;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.util.io.FileUtils;
import com.sun.enterprise.util.StringUtils;
import com.sun.enterprise.deploy.shared.ArchiveFactory;
import org.glassfish.hk2.api.IterableProvider;
import org.jvnet.hk2.annotations.Optional;
import org.jvnet.hk2.annotations.Service;
import javax.inject.Inject;
import javax.inject.Named;

import org.jvnet.hk2.config.*;
import org.glassfish.api.admin.CommandRunner;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.deployment.DeployCommandParameters;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.api.deployment.archive.WritableArchive;
import org.glassfish.api.ActionReport;
import org.glassfish.deployment.common.DeploymentProperties;
import org.glassfish.internal.api.*;

import java.util.*;
import java.util.jar.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.net.URISyntaxException;
import java.net.URI;
import java.io.*;
import java.beans.PropertyVetoException;
import org.glassfish.kernel.KernelLoggerInfo;

/**
 * Very simple ModuleStartup that basically force an immediate shutdown.
 * When start() is invoked, the upgrade of the domain.xml has already been
 * performed.
 * 
 * @author Jerome Dochez
 */
@Service(name="upgrade")
public class UpgradeStartup implements ModuleStartup {

    @Inject
    CommandRunner runner;

    @Inject
    AppServerStartup appservStartup;

    @Inject
    Applications applications;

    @Inject
    ArchiveFactory archiveFactory;

    @Inject 
    ServerEnvironment env;

    @Inject @Named( ServerEnvironment.DEFAULT_INSTANCE_NAME)
    Server server;

    @Inject 
    Domain domain;

    @Inject
    CommandRunner commandRunner;

    @Inject @Optional
    IterableProvider<DomainUpgrade> upgrades;

    // we need to refine, a better logger should be used.
    @Inject
    Logger logger;
    
    @Inject
    private InternalSystemAdministrator kernelIdentity;

    private final static String MODULE_TYPE = "moduleType";

    private final static String J2EE_APPS = "j2ee-apps";
    private final static String J2EE_MODULES = "j2ee-modules";

    private final static String DOMAIN_TARGET = "domain";

    private final static String SIGNATURE_TYPES_PARAM = "-signatureTypes";

    private List<String> sigTypeList = new ArrayList<String>(); 

    public void setStartupContext(StartupContext startupContext) {
        appservStartup.setStartupContext(startupContext);
    }

    // do nothing, just return, at the time the upgrade service has
    // run correctly.
    public void start() {

        // we need to disable all the applications before starting server 
        // so the applications will not get loaded before redeployment
        // store the list of previous enabled applications
        // so we can reset these applications back to enabled after
        // redeployment
        List<Application> enabledApps = new ArrayList<Application>();
        List<String> enabledAppNames = new ArrayList<String>();

        for (Application app : domain.getApplications().getApplications()) {
            logger.log(Level.INFO, "app " + app.getName() + " is " + app.getEnabled() + " resulting in " + Boolean.parseBoolean(app.getEnabled()));
            if (Boolean.parseBoolean(app.getEnabled())) {
                logger.log(Level.INFO, "Disabling application " + app.getName());
                enabledApps.add(app);
                enabledAppNames.add(app.getName());
            }
        }

        if (enabledApps.size()>0) {
            try  {
                ConfigSupport.apply(new ConfigCode() {
                    public Object run(ConfigBeanProxy... configBeanProxies) throws PropertyVetoException, TransactionFailure {
                        for (ConfigBeanProxy proxy : configBeanProxies) {
                            Application app = (Application) proxy;
                            app.setEnabled(Boolean.FALSE.toString());
                        }
                        return null;
                    }
                }, enabledApps.toArray(new Application[enabledApps.size()]));
            } catch(TransactionFailure tf) {
                logger.log(Level.SEVERE, "Exception while disabling applications", tf);
                return;
            }
        }

        // start the application server
        appservStartup.start();

        initializeSigTypeList();

        // redeploy all existing applications
        for (Application app : applications.getApplications()) {
            // we don't need to redeploy lifecycle modules
            if (Boolean.valueOf(app.getDeployProperties().getProperty
                (ServerTags.IS_LIFECYCLE))) {
                continue;
            }
            logger.log(Level.INFO, "Redeploy application " + app.getName() + " located at " + app.getLocation());    
            // we let upgrade proceed even if one application 
            // failed to redeploy
            redeployApp(app);
        }

        // re-enables all applications. 
        // we need to use the names in the enabledAppNames to find all
        // the application refs that need to be re-enabled
        // as the previous application collected not longer exist
        // after redeployment
        if (enabledAppNames.size()>0) {
            for (Application app : domain.getApplications().getApplications()) {
                if (enabledAppNames.contains(app.getName())) {
                    logger.log(Level.INFO, "Enabling application " + app.getName());
                    try {
                        ConfigSupport.apply(new SingleConfigCode<Application>() {
                            public Object run(Application param) throws PropertyVetoException, TransactionFailure {
                                if (!Boolean.parseBoolean(param.getEnabled())) {
                                    param.setEnabled(Boolean.TRUE.toString());
                                }
                                return null;
                            }
                        }, app);
                    } catch(TransactionFailure tf) {
                        logger.log(Level.SEVERE, "Exception while disabling applications", tf);
                        return;
                    }
                }
            }
        }

        // clean up leftover directories
        cleanupLeftOverDirectories();

        // stop-the server.
        KernelLoggerInfo.getLogger().info(KernelLoggerInfo.exitUpgrade);
        try {
            Thread.sleep(3000);
            if (runner!=null) {
                runner.getCommandInvocation("stop-domain", new DoNothingActionReporter(), kernelIdentity.getSubject()).execute();
            }

        } catch (InterruptedException e) {
            KernelLoggerInfo.getLogger().log(Level.SEVERE, KernelLoggerInfo.exceptionUpgrade, e);
        }

    }

    public void stop() {
        appservStartup.stop();
    }

    private void cleanupLeftOverDirectories() {
        // 1. remove applications/j2ee-apps(modules) directory
        File oldJ2eeAppsRepository = new File(
            env.getApplicationRepositoryPath(), J2EE_APPS); 
        FileUtils.whack(oldJ2eeAppsRepository);
        File oldJ2eeModulesRepository = new File(
            env.getApplicationRepositoryPath(), J2EE_MODULES);
        FileUtils.whack(oldJ2eeModulesRepository);

        // 2. remove generated/xml/j2ee-apps(modules) directory
        File oldJ2eeAppsGeneratedXMLDir = new File( 
           env.getApplicationGeneratedXMLPath(), J2EE_APPS);
        FileUtils.whack(oldJ2eeAppsGeneratedXMLDir);
        File oldJ2eeModulesGeneratedXMLDir = new File( 
           env.getApplicationGeneratedXMLPath(), J2EE_MODULES);
        FileUtils.whack(oldJ2eeModulesGeneratedXMLDir);

        // 3. remove generated/ejb/j2ee-apps(modules) directory
        File oldJ2eeAppsEJBStubDir = new File( 
           env.getApplicationEJBStubPath(), J2EE_APPS);
        FileUtils.whack(oldJ2eeAppsEJBStubDir);
        File oldJ2eeModulesEJBStubDir = new File( 
           env.getApplicationEJBStubPath(), J2EE_MODULES);
        FileUtils.whack(oldJ2eeModulesEJBStubDir);

        // 4. clean up generated/jsp/j2ee-apps(modules) directory
        File oldJ2eeAppsJSPCompileDir = new File(
           env.getApplicationCompileJspPath(), J2EE_APPS);
        FileUtils.whack(oldJ2eeAppsJSPCompileDir);
        File oldJ2eeModulesJSPCompileDir = new File(
           env.getApplicationCompileJspPath(), J2EE_MODULES);
        FileUtils.whack(oldJ2eeModulesJSPCompileDir);

        // 5. clean up old system apps policy files
        File policyRootDir = env.getApplicationPolicyFilePath();
        File adminapp = new File(policyRootDir, "adminapp");
        FileUtils.whack(adminapp);
        File admingui = new File(policyRootDir, "admingui");
        FileUtils.whack(admingui);
        File ejbtimer = new File(policyRootDir, "__ejb_container_timer_app");
        FileUtils.whack(ejbtimer);
        File mejbapp = new File(policyRootDir, "MEjbApp");
        FileUtils.whack(mejbapp);
        File wstx = new File(policyRootDir, "WSTXServices");
        FileUtils.whack(wstx);
        File jwsappclient = new File(policyRootDir, "__JWSappclients");
        FileUtils.whack(jwsappclient);
    }

    private boolean redeployApp(Application app) {
        // we don't need to redeploy any v3 type application
        if (app.getModule().size() > 0 ) {
            logger.log(Level.INFO, "Skip redeploying v3 type application " + 
                app.getName());
            return true;
        }

        // populate the params and properties from application element first
        DeployCommandParameters deployParams = app.getDeployParameters(null);

        // for archive deployment, let's repackage the archive and redeploy
        // that way
        // we cannot just directory redeploy the archive deployed apps in
        // v2->v3 upgrade as the repository layout was different in v2 
        // we should not have to repackage for any upgrade from v3 
        if (! Boolean.valueOf(app.getDirectoryDeployed())) {
            File repackagedFile = null;
            try {
                repackagedFile = repackageArchive(app);
            } catch (IOException ioe) {
                logger.log(Level.SEVERE, "Repackaging of application " + app.getName() + " failed: " + ioe.getMessage(), ioe);
                return false;
            }
            if (repackagedFile == null) {
                logger.log(Level.SEVERE, "Repackaging of application " + app.getName() + " failed.");
                return false;
            }
            logger.log(Level.INFO, "Repackaged application " + app.getName()
                + " at " + repackagedFile.getPath()); 
            deployParams.path = repackagedFile;
        }

        deployParams.properties = app.getDeployProperties();
        // remove the marker properties so they don't get carried over 
        // through redeployment
        deployParams.properties.remove(MODULE_TYPE);
        // add the compatibility property so the applications are 
        // upgraded/redeployed in a backward compatible way
        deployParams.properties.setProperty(
            DeploymentProperties.COMPATIBILITY, "v2");
      
        // now override the ones needed for the upgrade
        deployParams.enabled = null;
        deployParams.force = true;
        deployParams.dropandcreatetables = false;
        deployParams.createtables = false;
        deployParams.target = DOMAIN_TARGET;

        ActionReport report = new DoNothingActionReporter();
        commandRunner.getCommandInvocation("deploy", report, kernelIdentity.getSubject()).parameters(deployParams).execute();

        // should we delete the temp file after we are done
        // it seems it might be useful to keep it around for debugging purpose

        if (report.getActionExitCode().equals(ActionReport.ExitCode.FAILURE)) {
            logger.log(Level.SEVERE, "Redeployment of application " + app.getName() + " failed: " + report.getMessage() + "\nPlease redeploy " + app.getName() + " manually.", report.getFailureCause());
            return false;
        }
        return true;
    }

    private File repackageArchive(Application app) throws IOException {
        URI uri = null;
        try {
            uri = new URI(app.getLocation());
        } catch (URISyntaxException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }

        if (uri == null) {
            return null;
        }
        
        Properties appProperties = app.getDeployProperties();
        String moduleType = appProperties.getProperty(MODULE_TYPE);
        String suffix = getSuffixFromType(moduleType);
        if (suffix == null) {
            suffix = ".jar";
        }
        File repositoryDir = new File(uri);

        // get temporary file directory of the system and set targetDir to it
        File tmp = File.createTempFile("upgrade", null);
        String targetParentDir = tmp.getParent();
        boolean isDeleted = tmp.delete();
        if (!isDeleted) {
            logger.log(Level.WARNING, "Error in deleting file " + tmp.getAbsolutePath());
        }

        if (moduleType.equals(ServerTags.J2EE_APPLICATION)) {
            return repackageApplication(repositoryDir, targetParentDir, suffix);
        } else {
            return repackageStandaloneModule(repositoryDir, targetParentDir, suffix);
        }
    }

    private File repackageApplication(File appDir,
        String targetParentDir, String suffix) throws IOException {
        String appName = appDir.getName();

        ReadableArchive source = archiveFactory.openArchive(appDir);

        File tempEar = new File(targetParentDir, appName + suffix);

        if (tempEar.exists()) {
            boolean isDeleted = tempEar.delete();
            if (!isDeleted) {
                logger.log(Level.WARNING, "Error in deleting file " + tempEar.getAbsolutePath());
            }
        }

        WritableArchive target = archiveFactory.createArchive("jar", tempEar);

        Collection<String> directoryEntries = source.getDirectories();
        List<String> subModuleEntries = new ArrayList<String>();
        List<String> entriesToExclude = new ArrayList<String>();
 
        // first put all the sub module jars to the target archive
        for (String directoryEntry : directoryEntries) {
            if (directoryEntry.endsWith("_jar") || 
                directoryEntry.endsWith("_war") || 
                directoryEntry.endsWith("_rar")) {
                subModuleEntries.add(directoryEntry); 
                File moduleJar = processModule(new File(
                    appDir, directoryEntry), targetParentDir, null);
                OutputStream os = null;
                InputStream is = new BufferedInputStream(
                    new FileInputStream(moduleJar));
                try {
                    os = target.putNextEntry(moduleJar.getName());
                    FileUtils.copy(is, os, moduleJar.length());
                } finally {
                    if (os!=null) {
                        target.closeEntry();
                    }
                    is.close();
                }
            }
        }

        // now find all the entries we should exclude to copy to the target
        // basically all sub module entries should be excluded
        for (String subModuleEntry : subModuleEntries) {
            Enumeration<String> ee = source.entries(subModuleEntry);
            while (ee.hasMoreElements()) {
                String eeEntryName = ee.nextElement();
                entriesToExclude.add(eeEntryName);
            }
        }

        // now copy the rest of the entries
        Enumeration<String> e = source.entries();
        while (e.hasMoreElements()) {
            String entryName = e.nextElement();
            if (! entriesToExclude.contains(entryName)) {
                InputStream sis = source.getEntry(entryName);
                if (isSigFile(entryName)) {
                    logger.log(Level.INFO, "Excluding signature file: " 
                        + entryName + " from repackaged application: " + 
                        appName + "\n");
                    continue;
                }
                if (sis != null) {
                    InputStream is = new BufferedInputStream(sis);
                    OutputStream os = null;
                    try {
                        os = target.putNextEntry(entryName);
                        FileUtils.copy(is, os, source.getEntrySize(entryName));
                    } finally {
                        if (os!=null) {
                            target.closeEntry();
                        }
                        is.close();
                    }
                }
            }
        }

        // last is manifest if existing.
        Manifest m = source.getManifest();
        if (m!=null) {
            processManifest(m, appName);
            OutputStream os  = target.putNextEntry(JarFile.MANIFEST_NAME);
            m.write(os);
            target.closeEntry();
        }

        source.close();
        target.close();
      
        return tempEar;
    }

    private File repackageStandaloneModule(File moduleDirName, 
        String targetParentDir, String suffix) throws IOException {
        return processModule(moduleDirName, targetParentDir, suffix);
    }

    // repackage a module and return it as a jar file
    private File processModule(File moduleDir, String targetParentDir, 
        String suffix) throws IOException {
 
        String moduleName = moduleDir.getName();

        // sub module in ear case 
        if (moduleName.endsWith("_jar") || moduleName.endsWith("_war") || moduleName.endsWith("_rar")) {
            suffix = "." +  moduleName.substring(moduleName.length() - 3);
            moduleName = moduleName.substring(0, moduleName.lastIndexOf('_'));
        }

        ReadableArchive source = archiveFactory.openArchive(moduleDir);

        File tempJar = new File(targetParentDir, moduleName + suffix);

        if (tempJar.exists()) {
            boolean isDeleted = tempJar.delete();
            if ( !isDeleted) {
                logger.log(Level.WARNING, "Error in deleting file " + tempJar.getAbsolutePath());
            }
        }

        WritableArchive target = archiveFactory.createArchive("jar", tempJar);

        Enumeration<String> e = source.entries();
        while (e.hasMoreElements()) {
            String entryName = e.nextElement();
            if (isSigFile(entryName)) {
                logger.log(Level.INFO, "Excluding signature file: " 
                    + entryName + " from repackaged module: " + moduleName + 
                    "\n");
                continue;
            }
            InputStream sis = source.getEntry(entryName);
            if (sis != null) {
                InputStream is = new BufferedInputStream(sis);
                OutputStream os = null;
                try {
                    os = target.putNextEntry(entryName);
                    FileUtils.copy(is, os, source.getEntrySize(entryName));
                } finally {
                    if (os!=null) {
                        target.closeEntry();
                    }
                    is.close();
                }
            }
        }

        // last is manifest if existing.
        Manifest m = source.getManifest();
        if (m!=null) {
            processManifest(m, moduleName);
            OutputStream os  = target.putNextEntry(JarFile.MANIFEST_NAME);
            m.write(os);
            target.closeEntry();
        }

        source.close();
        target.close();

        return tempJar;
    }

    private String getSuffixFromType(String moduleType) {
        if (moduleType == null) {
            return null;
        }
        if (moduleType.equals(ServerTags.CONNECTOR_MODULE)) {
            return ".rar"; 
        }
        if (moduleType.equals(ServerTags.EJB_MODULE)) {
            return ".jar"; 
        }
        if (moduleType.equals(ServerTags.WEB_MODULE)) {
            return ".war"; 
        }
        if (moduleType.equals(ServerTags.APPCLIENT_MODULE)) {
            return ".jar"; 
        }
        if (moduleType.equals(ServerTags.J2EE_APPLICATION)) {
            return ".ear"; 
        }
        return null;
    }

    private void initializeSigTypeList() {
        String sigTypesParam = env.getStartupContext().getArguments().getProperty(SIGNATURE_TYPES_PARAM);
        if (sigTypesParam != null) {
            sigTypeList = StringUtils.parseStringList(sigTypesParam, ",");
        }
        sigTypeList.add(".SF");
        sigTypeList.add(".sf");
        sigTypeList.add(".RSA");
        sigTypeList.add(".rsa");
        sigTypeList.add(".DSA");
        sigTypeList.add(".dsa");
        sigTypeList.add(".PGP");
        sigTypeList.add(".pgp");
    }

    private boolean isSigFile(String entryName) {
        for (String sigType : sigTypeList) {
            if (entryName.endsWith(sigType)) {
                return true;
            }
        }
        return false;
    }

    private void processManifest(Manifest m, String moduleName) {
        // remove signature related entries from the file
        Map<String, Attributes> entries = m.getEntries(); 
        Iterator<Map.Entry<String, Attributes>> entryItr = entries.entrySet().iterator(); 
        while (entryItr.hasNext()) {
            Attributes attr = entryItr.next().getValue();
            Iterator<Map.Entry<Object, Object>> attrItr  = attr.entrySet().iterator();
            while (attrItr.hasNext()) {
                Object attrKey = attrItr.next().getKey();
                if (attrKey instanceof Attributes.Name) {
                    Attributes.Name attrKey2 = (Attributes.Name) attrKey;
                    if (attrKey2.toString().trim().equals("Digest-Algorithms")
                        || attrKey2.toString().indexOf("-Digest") != -1) {
                        logger.log(Level.INFO, "Removing signature attribute " 
                            + attrKey2 + " from manifest in "  + 
                            moduleName + "\n");
                        attrItr.remove();
                    }
                }
            }
            if (attr.size() == 0) {
                entryItr.remove();
            }
        }
    }
}
