/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2014 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.appclient.server.core;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Applications;
import com.sun.enterprise.module.Module;
import java.io.IOException;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.deployment.Application;
import com.sun.enterprise.deployment.ApplicationClientDescriptor;
import com.sun.enterprise.deployment.archivist.AppClientArchivist;
import com.sun.enterprise.module.ModulesRegistry;
import com.sun.logging.LogDomains;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.inject.Named;

import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.api.deployment.MetaData;
import org.glassfish.api.deployment.UndeployCommandParameters;
import org.glassfish.appclient.server.core.jws.servedcontent.ASJarSigner;
import org.glassfish.deployment.common.DeploymentException;
import org.glassfish.javaee.core.deployment.JavaEEDeployer;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PostConstruct;
import javax.inject.Singleton;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.appclient.server.core.jws.JWSAdapterManager;
import org.glassfish.appclient.server.core.jws.JavaWebStartInfo;
import org.glassfish.deployment.common.Artifacts;
import org.glassfish.deployment.common.DeploymentUtils;
/**
 * AppClient module deployer.
 * <p>
 * Prepares JARs for download to the admin client and tracks which JARs should
 * be downloaded for each application.  (Downloads occur during
 * <code>deploy --retrieve</code> or <code>get-client-stubs</code> command
 * processing, or during Java Web Start launches of app clients.  Also creates
 * AppClientServerApplication instances for each client to provide Java Web Start
 * support.
 * <p>
 * Main responsibilities:
 * <ul>
 * <li>create a new facade JAR for each of the developer's original app client
 * JARs, and
 * <li>create a new facade JAR for the EAR (if the app client is part of an EAR), and
 * <li>manage internal data structures that map each deployed app to the app
 * client-related JARs that should be downloaded for that app.
 * </ul>
 * Each app client facade JAR contains:
 * <ul>
 * <li>a manifest which:
 *      <ul>
 *      <li>lists the GlassFish app client facade class as the Main-Class
 *      <li>contains a Class-Path entry referring to the developer's original JAR
 * and any JARs in the EAR's library directory,
 *      <li>contains a GlassFish-specific item that is a relative URI pointing to the
 * corresponding original JAR
 *      <li>contains a GlassFish-specific item identifying the main class in the
 * developer's original JAR
 *      <li>contains a copy of the SplashScreen-Image item from the developer's
 * original JAR, if there is one
 *      </ul>
 * <li>the app client facade main class that prepares the ACC runtime environment before
 * transferring control to the developer's main class
 * <li>a copy of the splash screen image from the developer's original JAR, if
 * there is one
 * </ul>
 *
 * If the app client being deployed is part of an EAR, then the EAR facade
 * represents an "app client group" and contains:
 * <ul>
 * <li>a manifest which:
 *      <ul>
 *      <li>declares the GlassFish EAR facade class as the Main-Class
 *      <li>lists the URIs to the individual app client facade JARs in a
 * GlassFish-specific item
 *      </ul>
 * <li>the GlassFish app client group facade main class
 * </ul>
 *<p>
 * For backward compatibility, the generated facade JAR is named
 * ${appName}Client.jar and is downloaded into the local directory the user
 * specifies on the <code>deploy --retrieve</code> or <code>get-client-stubs</code>
 * command.  Other JARs - the developer's original app client JAR(s)
 * and any required library JARs - are downloaded into a subdirectory within
 * that local directory named ${appName}Client.  This segregates the files for
 * different clients into different subdirectories to avoid naming collisions if
 * the user downloads multiple clients into the same local directory.
 *
 * @author tjquinn
 * 
 */
@Service
@Singleton
public class AppClientDeployer
        extends JavaEEDeployer<AppClientContainerStarter, AppClientServerApplication>
        implements PostConstruct {

    private Logger logger;

    public static final String APPCLIENT_FACADE_CLASS_FILE = 
            "org/glassfish/appclient/client/AppClientFacade.class";
    public static final String APPCLIENT_AGENT_MAIN_CLASS_FILE =
            "org/glassfish/appclient/client/JWSAppClientContainerMain.class";
    public static final String APPCLIENT_COMMAND_CLASS_NAME = "org.glassfish.appclient.client.AppClientFacade";
    public static final Attributes.Name GLASSFISH_APPCLIENT_MAIN_CLASS =
            new Attributes.Name("GlassFish-AppClient-Main-Class");

    public static final Attributes.Name SPLASH_SCREEN_IMAGE =
            new Attributes.Name("SplashScreen-Image");

    public static final Attributes.Name GLASSFISH_APP_NAME =
            new Attributes.Name("GlassFish-App-Name");

    private static final String GF_CLIENT_MODULE_NAME = "org.glassfish.main.appclient.gf-client-module";

    /** Save the helper across phases in the deployment context's appProps */
    public static final String HELPER_KEY_NAME = "org.glassfish.appclient.server.core.helper";

    @Inject
    protected Domain domain;

    @Inject
    private ModulesRegistry modulesRegistry;

    @Inject
    private Applications applications;

    @Inject
    private ASJarSigner jarSigner;

    @Inject @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    Config config;

//    private DownloadableArtifacts downloadInfo = null;

    /**
     * Maps the app name to the user-friendly context root for that app.
     */
    private final Map<String,String> appAndClientNameToUserFriendlyContextRoot =
            new HashMap<String,String>();



    /** the class loader which knows about the org.glassfish.main.appclient.gf-client-module */
    private ClassLoader gfClientModuleClassLoader;

    /*
     * Each app client server application will listen for config change
     * events - for creation, deletion, or change of java-web-start-enabled
     * property settings.  Because they are not handled as services hk2 will
     * not automatically register them for notification.  This deployer, though,
     * is a service and so by implementing ConfigListener is registered
     * by hk2 automatically for config changes.  The following Set collects
     * all app client server applications so the deployer can forward
     * notifications to each app client server app.
     */
    final private Set<AppClientServerApplication> appClientApps =
            new HashSet<AppClientServerApplication>();

    public AppClientDeployer() {
    }

    @Override
    public void postConstruct() {
        logger = Logger.getLogger(JavaWebStartInfo.APPCLIENT_SERVER_MAIN_LOGGER, 
                JavaWebStartInfo.APPCLIENT_SERVER_LOGMESSAGE_RESOURCE);
        for (Module module : modulesRegistry.getModules(GF_CLIENT_MODULE_NAME)) {
            gfClientModuleClassLoader = module.getClassLoader();
        }
    }

    @Override
    public MetaData getMetaData() {
        return new MetaData(false, null, new Class[]{Application.class});
    }

    @Override
    public AppClientServerApplication load(AppClientContainerStarter containerStarter, DeploymentContext dc) {
        // if the populated DOL object does not container appclient 
        // descriptor, this is an indication that appclient deployer
        // should not handle this module
        ApplicationClientDescriptor appclientDesc = 
            dc.getModuleMetaData(ApplicationClientDescriptor.class);
        if (appclientDesc == null) { 
            return null;
        }
        appclientDesc.setClassLoader(dc.getClassLoader());
        AppClientDeployerHelper helper = null;
        try {
            helper = getSavedHelperOrCreateHelper(dc);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

//        helper.addGroupFacadeToEARDownloads();
        final AppClientServerApplication newACServerApp = newACServerApp(dc, helper);
        appClientApps.add(newACServerApp);
        return newACServerApp;
    }

    public Set<AppClientServerApplication> appClientApps() {
        return appClientApps;
    }
    
    private AppClientServerApplication newACServerApp(
            final DeploymentContext dc, final AppClientDeployerHelper helper) {
        final AppClientServerApplication result = habitat.getService(AppClientServerApplication.class);
        result.init(dc, helper);
        return result;
    }

    @Override
    public void unload(AppClientServerApplication application, DeploymentContext dc) {
        appClientApps.remove(application);
    }

    /**
     * Clean any files and artifacts that were created during the execution
     * of the prepare method.
     *
     * @param dc deployment context
     */
    @Override
    public void clean(DeploymentContext dc) {
        super.clean(dc);
        UndeployCommandParameters params = dc.getCommandParameters(UndeployCommandParameters.class);
        if (params != null) {
            final com.sun.enterprise.config.serverbeans.Application app =
                    applications.getApplication(params.name);
            DeploymentUtils.downloadableArtifacts(app).clearArtifacts();
        }
    }

    @Override
    protected void generateArtifacts(DeploymentContext dc) throws DeploymentException {
        // if the populated DOL object does not container appclient 
        // descriptor, this is an indication that appclient deployer
        // should not handle this module
        if (dc.getModuleMetaData(ApplicationClientDescriptor.class) == null) { 
            return;
        }

        try {
            final AppClientDeployerHelper helper = createAndSaveHelper(
                    dc, gfClientModuleClassLoader);
            helper.prepareJARs();
            addArtifactsToDownloads(helper, dc);
            addArtifactsToGeneratedFiles(helper, dc);
            recordUserFriendlyContextRoot(helper, dc);
        } catch (Exception ex) {
            throw new DeploymentException(ex);
        }
    }

    /**
     * Records the user-friendly path as a property for the app client module.
     * This is primarily for ease-of-lookup from GetRelativeJWSURICommand.
     * 
     * @param helper
     * @param dc
     */
    private void recordUserFriendlyContextRoot(final AppClientDeployerHelper helper,
            final DeploymentContext dc) {
        final String path = JWSAdapterManager.userFriendlyContextRoot(helper.appClientDesc(), dc.getAppProps());
        dc.getModuleProps().put("jws.user.friendly.path", path);
    }

    private void addArtifactsToDownloads(
            final AppClientDeployerHelper helper,
            final DeploymentContext dc) throws IOException {
        final Artifacts downloadInfo = DeploymentUtils.downloadableArtifacts(dc);
        downloadInfo.addArtifacts(helper.earLevelDownloads());
        downloadInfo.addArtifacts(helper.clientLevelDownloads());
    }

    private void addArtifactsToGeneratedFiles(
            final AppClientDeployerHelper helper,
            final DeploymentContext dc) throws IOException {
        final Artifacts generatedFileInfo = DeploymentUtils.generatedArtifacts(dc);
        generatedFileInfo.addArtifacts(helper.earLevelDownloads());
        generatedFileInfo.addArtifacts(helper.clientLevelDownloads());
    }

    private AppClientDeployerHelper createAndSaveHelper(final DeploymentContext dc,
            final ClassLoader clientModuleLoader) throws IOException {
            final AppClientArchivist archivist = habitat.getService(AppClientArchivist.class);
            final AppClientDeployerHelper h =
            AppClientDeployerHelper.newInstance(
                dc,
                archivist,
                clientModuleLoader,
                habitat,
                jarSigner);
        dc.addTransientAppMetaData(HELPER_KEY_NAME + moduleURI(dc), h.proxy());
        return h;
    }

    private AppClientDeployerHelper getSavedHelperOrCreateHelper(final DeploymentContext dc) throws IOException {
        final String key = HELPER_KEY_NAME + moduleURI(dc);
        AppClientDeployerHelper h = null;

        final AppClientDeployerHelper.Proxy p = dc.getTransientAppMetaData(key,
                AppClientDeployerHelper.Proxy.class);
        if (p != null) {
            h = p.helper();
        }

        if (h == null) {
            h = dc.getTransientAppMetaData(key,
                    StandaloneAppClientDeployerHelper.class);
        }
        if (h == null) {
            /*
             * We are probably loading a previously-deployed app client, so
             * the helper has not be created yet.  Create it.
             */
            h = createAndSaveHelper(dc,
                    gfClientModuleClassLoader);
        }
        return h;
    }

    private String moduleURI(final DeploymentContext dc) {
        ApplicationClientDescriptor acd = dc.getModuleMetaData(ApplicationClientDescriptor.class);
        return acd.getModuleDescriptor().getArchiveUri();
    }

    public void recordContextRoot(final String appName,
            final String clientURIWithinEAR,
            final String userFriendlyContextRoot) {
        appAndClientNameToUserFriendlyContextRoot.put(
                keyToAppAndClientNameMap(appName, clientURIWithinEAR),
                userFriendlyContextRoot);
    }

    public void removeContextRoot(final String appName,
            final String clientURIWithinEAR) {
            appAndClientNameToUserFriendlyContextRoot.remove(
                keyToAppAndClientNameMap(appName, clientURIWithinEAR));
    }
    
    /**
     * Returns the user-friendly context root for the specified app client.
     * <p>
     * Primarily used from the admin console for retrieving the context path
     * for launching the specified app client.
     * @param appName
     * @param clientModuleURI
     * @return
     */
    public String userFriendlyContextRoot(final String appName,
            final String clientModuleURI) {
        return appAndClientNameToUserFriendlyContextRoot.get(
                keyToAppAndClientNameMap(appName, clientModuleURI));
    }



    private String keyToAppAndClientNameMap(final String appName,
            final String moduleURIText) {
        return appName + "/" + (moduleURIText == null ? appName : moduleURIText);
    }



}
