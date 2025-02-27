/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2013 Oracle and/or its affiliates. All rights reserved.
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
// Portions Copyright [2019-2025] Payara Foundation and/or affiliates

package org.glassfish.deployment.common;

import com.sun.enterprise.config.serverbeans.ServerTags;
import org.glassfish.deployment.versioning.VersioningUtils;
import java.lang.instrument.ClassFileTransformer;
import org.glassfish.api.ActionReport;
import org.glassfish.api.deployment.InstrumentableClassLoader;
import org.glassfish.api.deployment.OpsParams;

import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.api.deployment.archive.ArchiveHandler;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.hk2.classmodel.reflect.Parser;
import org.glassfish.hk2.classmodel.reflect.Types;
import org.glassfish.internal.api.ClassLoaderHierarchy;
import org.glassfish.internal.deployment.*;
import org.glassfish.loader.util.ASClassLoaderUtil;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.logging.Logger;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.MalformedURLException;
import java.net.URLClassLoader;

import org.glassfish.hk2.api.PreDestroy;

import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.util.io.FileUtils;
import fish.payara.nucleus.hotdeploy.ApplicationState;
import fish.payara.nucleus.hotdeploy.HotDeployService;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.FINEST;
import org.glassfish.api.deployment.DeployCommandParameters;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.Globals;

import org.glassfish.logging.annotation.LoggerInfo;
import org.glassfish.logging.annotation.LogMessagesResourceBundle;

/**
 *
 * @author dochez
 */
public class DeploymentContextImpl implements ExtendedDeploymentContext, PreDestroy {

    @LogMessagesResourceBundle
    private static final String SHARED_LOGMESSAGE_RESOURCE = "org.glassfish.deployment.LogMessages";

    // Reserve this range [NCLS-DEPLOYMENT-00001, NCLS-DEPLOYMENT-02000]
    // for message ids used in this deployment common module
    @LoggerInfo(subsystem = "DEPLOYMENT", description="Deployment logger for common module", publish=true)
    private static final String DEPLOYMENT_LOGGER = "javax.enterprise.system.tools.deployment.common";

    public static final Logger deplLogger =
        Logger.getLogger(DEPLOYMENT_LOGGER, SHARED_LOGMESSAGE_RESOURCE);

    final private static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(DeploymentContextImpl.class);

    private static final String INTERNAL_DIR_NAME = "__internal";
    private static final String APP_TENANTS_SUBDIR_NAME = "__app-tenants";

    ReadableArchive source;
    ReadableArchive originalSource;
    final OpsParams parameters;
    ActionReport actionReport;
    final ServerEnvironment env;
    ClassLoader cloader;
    ArchiveHandler archiveHandler;
    Properties props;
    Map<String, Object> modulesMetaData = new HashMap<String, Object>();
    List<ClassFileTransformer> transformers = new ArrayList<ClassFileTransformer>();
    Phase phase = Phase.UNKNOWN;
    WeakReference<ClassLoader> sharableTemp = null;
    Map<String, Properties> modulePropsMap = new HashMap<String, Properties>();
    Map<String, Object> transientAppMetaData = new HashMap<>() {
        @Override
        public Object get(Object key) {
            check(key);
            return super.get(key);
        }

        @Override
        public Object put(String key, Object value) {
            check(key);
            return super.put(key, value);
        }

        @Override
        public Object putIfAbsent(String key, Object value) {
            check(key);
            return super.putIfAbsent(key, value);
        }

        @Override
        public Object compute(String key, BiFunction<? super String, ? super Object, ?> remappingFunction) {
            check(key);
            return super.compute(key, remappingFunction);
        }

        @Override
        public Object computeIfAbsent(String key, Function<? super String, ?> mappingFunction) {
            check(key);
            return super.computeIfAbsent(key, mappingFunction);
        }

        @Override
        public Object computeIfPresent(String key, BiFunction<? super String, ? super Object, ?> remappingFunction) {
            check(key);
            return super.computeIfPresent(key, remappingFunction);
        }

        private void check(Object key) {
            if (key.equals(Types.class.getName()) || key.equals(Parser.class.getName())) {
                throw new IllegalArgumentException("Cannot access " + key + " in transient metadata");
            }
        }
    };
    Map<String, ArchiveHandler> moduleArchiveHandlers = new HashMap<String, ArchiveHandler>();
    Map<String, ExtendedDeploymentContext> moduleDeploymentContexts = new HashMap<String, ExtendedDeploymentContext>();
    ExtendedDeploymentContext parentContext = null;
    String moduleUri = null;
    private String tenant = null;
    private String originalAppName = null;
    private File tenantDir = null;

    /**
     * Creates a new instance of DeploymentContext
     *
     * @param builder
     * @param env
     */
    public DeploymentContextImpl(Deployment.DeploymentContextBuilder builder, ServerEnvironment env) {
        this(builder.report(),  builder.sourceAsArchive(), builder.params(), env);
    }
    public DeploymentContextImpl(ActionReport actionReport, Logger logger,
        ReadableArchive source, OpsParams params, ServerEnvironment env) {
      this(actionReport, source, params, env);
    }
    public DeploymentContextImpl(ActionReport actionReport,
        ReadableArchive source, OpsParams params, ServerEnvironment env) {
        this.originalSource = source;
        this.source = source;
        this.actionReport = actionReport;
        this.parameters = params;
        this.env = env;
    }

    public Phase getPhase()
    {
        return phase;
    }

    public void setPhase(Phase newPhase) {
        this.phase = newPhase;
    }

    public ReadableArchive getSource() {
        return source;
    }

    public void setSource(ReadableArchive source) {
        this.source = source;
    }

    public <U extends OpsParams> U getCommandParameters(Class<U> commandParametersType) {
        try {
            return commandParametersType.cast(parameters);
        } catch (ClassCastException e) {
            return null;
        }
    }

    public Logger getLogger() {
        return deplLogger;
    }

    @Override
    public synchronized void preDestroy() {
        boolean hotDeploy = getCommandParameters(DeployCommandParameters.class).hotDeploy;
        if (!hotDeploy) {
            try {
                PreDestroy.class.cast(sharableTemp.get()).preDestroy();
            } catch (Exception e) {
                // ignore, the classloader does not need to be destroyed
            }
            try {
                PreDestroy.class.cast(cloader).preDestroy();
            } catch (Exception e) {
                // ignore, the classloader does not need to be destroyed
            }
        }
    }

    /**
     * Returns the class loader associated to this deployment request.
     * ClassLoader instances are usually obtained by the getClassLoader API on
     * the associated ArchiveHandler for the archive type being deployed.
     * <p/>
     * This can return null and the container should allocate a ClassLoader
     * while loading the application.
     *
     * @return a class loader capable of loading classes and resources from the
     *         source
     * @link {org.jvnet.glassfish.apu.deployment.archive.ArchiveHandler.getClassLoader()}
     */
    @Override
    public ClassLoader getFinalClassLoader() {
        return cloader;
    }

    /**
     * Returns the class loader associated to this deployment request.
     * ClassLoader instances are usually obtained by the getClassLoader API on
     * the associated ArchiveHandler for the archive type being deployed.
     * <p/>
     * This can return null and the container should allocate a ClassLoader
     * while loading the application.
     *
     * @return a class loader capable of loading classes and resources from the
     *         source
     * @link {org.jvnet.glassfish.apu.deployment.archive.ArchiveHandler.getClassLoader()}
     */
    @Override
    public ClassLoader getClassLoader() { 
      /* TODO -- Replace this method with another that does not imply it is
       * an accessor and conveys that the result may change depending on the
       * current lifecycle. For instance contemporaryClassLoader()
       * Problem was reported by findbug
       */
      return getClassLoader(true);
    }

    @Override
    public synchronized void setClassLoader(ClassLoader cloader) {
        this.cloader = cloader;
    }


    // this classloader will be used for sniffer retrieval, metadata parsing 
    // and the prepare
    @Override
    public synchronized void createDeploymentClassLoader(ClassLoaderHierarchy clh, ArchiveHandler handler)
            throws URISyntaxException, MalformedURLException {
        this.addTransientAppMetaData(ExtendedDeploymentContext.IS_TEMP_CLASSLOADER, Boolean.TRUE);
        boolean hotDeploy = getCommandParameters(DeployCommandParameters.class).hotDeploy;
        if (hotDeploy && this.cloader != null) {
            this.sharableTemp = new WeakReference<>(this.cloader);
        } else {
            this.sharableTemp = new WeakReference<>(createClassLoader(clh, handler, null));
        }
    }

    // this classloader will used to load and start the application
    @Override
    public void createApplicationClassLoader(ClassLoaderHierarchy clh, ArchiveHandler handler)
            throws URISyntaxException, MalformedURLException {
        this.addTransientAppMetaData(ExtendedDeploymentContext.IS_TEMP_CLASSLOADER, Boolean.FALSE);
        if (this.cloader == null) {
            this.cloader = createClassLoader(clh, handler, parameters.name());
        }
    }

    private ClassLoader createClassLoader(ClassLoaderHierarchy clh, ArchiveHandler handler, String appName)
            throws URISyntaxException, MalformedURLException {
        // first we create the appLib class loader, this is non shared libraries class loader
        ClassLoader applibCL = clh.getAppLibClassLoader(appName, getAppLibs());

        ClassLoader parentCL = clh.createApplicationParentCL(applibCL, this);

        return handler.getClassLoader(parentCL, this);
    }

    public synchronized ClassLoader getClassLoader(boolean sharable) {
        // if we are in prepare phase, we need to return our sharable temporary class loader
        // otherwise, we return the final one.
        if (phase == Phase.PREPARE) {
            if (sharable) {
                return sharableTemp.get();
            } else {
                InstrumentableClassLoader cl = InstrumentableClassLoader.class.cast(sharableTemp);
                return cl.copy();
            }
        } else {
            // we are out of the prepare phase, destroy the shareableTemp and 
            // return the final classloader
            if (sharableTemp != null && sharableTemp.get() != cloader) {
                try {
                    PreDestroy.class.cast(sharableTemp.get()).preDestroy();
                } catch (Exception e) {
                    // ignore, the classloader does not need to be destroyed
                }
                sharableTemp = null;
            }
            return cloader;
        }
    }

    /**
     * Returns a scratch directory that can be used to store things in.
     * The scratch directory will be persisted across server restart but
     * not across redeployment of the same application
     *
     * @param subDirName the sub directory name of the scratch dir
     * @return the scratch directory for this application based on
     *         passed in subDirName. Returns the root scratch dir if the
     *         passed in value is null.
     */
    public File getScratchDir(String subDirName) {
        File rootScratchDir = env.getApplicationStubPath();
        if (tenant != null && originalAppName != null) {
            // multi-tenant case
            rootScratchDir = getRootScratchTenantDirForApp(originalAppName);
            rootScratchDir = new File(rootScratchDir, tenant);
            if (subDirName != null ) {
                rootScratchDir = new File(rootScratchDir, subDirName);
            }
            return rootScratchDir;
        } else {
            // regular case
            if (subDirName != null ) {
                rootScratchDir = new File(rootScratchDir, subDirName);
            }
            String appDirName = VersioningUtils.getRepositoryName(parameters.name());
            return new File(rootScratchDir, appDirName);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public File getSourceDir() {

        return new File(getSource().getURI());
    }

    @Override
    public void addModuleMetaData(Object metaData) {
        if (metaData!=null) {
            modulesMetaData.put(metaData.getClass().getName(), metaData);
        }
    }

    @Override
    public void resetModuleMetaData() {
        modulesMetaData.clear();
    }

    @Override
    public <T> T getModuleMetaData(Class<T> metadataType) {
        Object moduleMetaData = modulesMetaData.get(metadataType.getName());
        if (moduleMetaData != null) {
            return metadataType.cast(moduleMetaData);
        } else {
            for (Object metadata : modulesMetaData.values()) {
                try {
                    return metadataType.cast(metadata);
                } catch (ClassCastException e) {
                }
            }
            return null;
        }
    }

    @Override
    public Collection<Object> getModuleMetadata() {
        return Collections.unmodifiableCollection(modulesMetaData.values());
    }

    @Override
    public Map<String, Object> getTransientAppMetadata() {
        return Collections.unmodifiableMap(transientAppMetaData);
    }

    @Override
    public void addTransientAppMetaData(String metaDataKey, Object metaData) {
        if (metaData!=null) {
            transientAppMetaData.put(metaDataKey, metaData);
        }
    }

    @Override
    public void removeTransientAppMetaData(String metaDataKey) {
        transientAppMetaData.remove(metaDataKey);
    }

    @Override
    public <T> T getTransientAppMetaData(String key, Class<T> metadataType) {
        Object metaData = transientAppMetaData.get(key);
        if (metaData != null) {
            return metadataType.cast(metaData);
        }
        return null;
    }

    /**
     * Returns the application level properties that will be persisted as a
     * key value pair at then end of deployment. That allows individual
     * Deployers implementation to store some information at the
     * application level that should be available upon server restart.
     * Application level properties are shared by all the modules.
     *
     * @return the application's properties.
     */
    @Override
    public Properties getAppProps() {
        if (props==null) {
            props = new Properties();
        }
        return props;
    }

    /**
     * Returns the module level properties that will be persisted as a
     * key value pair at then end of deployment. That allows individual
     * Deployers implementation to store some information at the module
     * level that should be available upon server restart.
     * Module level properties are only visible to the current module.
     * @return the module's properties.
     */
    @Override
    public Properties getModuleProps() {
        // for standalone case, it would return the same as application level 
        // properties
        // for composite case, the composite deployer will return proper 
        // module level properties
        if (props==null) {
            props = new Properties();
        }
        return props;
    }

    /**
     * Add a new ClassFileTransformer to the context
     *
     * @param transformer the new class file transformer to register to the new application
     * class loader
     * @throws UnsupportedOperationException if the class loader we use does not support the
     * registration of a ClassFileTransformer. In such case, the deployer should either fail
     * deployment or revert to a mode without the bytecode enhancement feature.
     */
    public void addTransformer(ClassFileTransformer transformer) {

        InstrumentableClassLoader icl = InstrumentableClassLoader.class.cast(getFinalClassLoader());
        String isComposite = getAppProps().getProperty(ServerTags.IS_COMPOSITE);

        if (Boolean.valueOf(isComposite) && icl instanceof URLClassLoader) {
            URLClassLoader urlCl = (URLClassLoader)icl;
            boolean isAppLevel = (getParentContext() == null);
            if (isAppLevel) {
                // for ear lib PUs, let's install the
                // tranformers with the EarLibClassLoader
                icl = InstrumentableClassLoader.class.cast(urlCl.getParent().getParent());
            } else {
                // for modules inside the ear, let's install the
                // transformers with the EarLibClassLoader in
                // addition to installing them to module classloader
                ClassLoader libCl = urlCl.getParent().getParent();
                if (!(libCl instanceof URLClassLoader)) {
                    // web module
                    libCl = libCl.getParent();
                }
                if (libCl instanceof URLClassLoader) {
                    InstrumentableClassLoader libIcl = InstrumentableClassLoader.class.cast(libCl);
                    libIcl.addTransformer(transformer);
                }

            }
        }
        icl.addTransformer(transformer);
    }

    /**
     * Returns the list of transformers registered to this context.
     *
     * @return the transformers list
     */ 
    public List<ClassFileTransformer> getTransformers() {
        return transformers;
    }

    public List<URI> getAppLibs()
            throws URISyntaxException {
        List<URI> libURIs = new ArrayList<URI>();
        if (parameters.libraries() != null) {
            URL[] urls = 
                ASClassLoaderUtil.getDeployParamLibrariesAsURLs(
                    parameters.libraries(), env);
            for (URL url : urls) {
                File file = new File(url.getFile());
                deplLogger.log(FINE, "Specified library jar: {0}", file.getAbsolutePath());
                if (file.exists()){
                    libURIs.add(url.toURI());
                } else {
                    throw new IllegalArgumentException(localStrings.getLocalString("enterprise.deployment.nonexist.libraries", "Specified library jar {0} does not exist: {1}", file.getName(), file.getAbsolutePath()));
                }
            }
        }

        Set<String> extensionList = null;
        try{
            extensionList = InstalledLibrariesResolver.getInstalledLibraries(source);
        }catch(IOException ioe){
            throw new RuntimeException(ioe);
        }
        URL[] extensionListLibraries = ASClassLoaderUtil.getLibrariesAsURLs(extensionList, env);
        for (URL url : extensionListLibraries) {
            libURIs.add(url.toURI());
            if (deplLogger.isLoggable(FINEST)) {
                deplLogger.log(FINEST, "Detected [EXTENSION_LIST] installed-library [ {0} ] for archive [ {1}]", new Object[]{url, source.getName()});
            }
        }

        return libURIs;
    }

    @Override
    public void clean() {
        if (parameters.origin == OpsParams.Origin.undeploy ||
            parameters.origin == OpsParams.Origin.deploy ) {
            // for undeploy or deploy failure roll back

            // need to remove the generated directories...
            // need to remove generated/xml, generated/ejb, generated/jsp, 

            // remove generated/xml
            File generatedXmlRoot = getScratchDir("xml");
            FileUtils.whack(generatedXmlRoot);

            // remove generated/ejb
            File generatedEjbRoot = getScratchDir("ejb");
            // recursively delete...
            FileUtils.whack(generatedEjbRoot);

            // remove generated/jsp
            File generatedJspRoot = getScratchDir("jsp");
            // recursively delete...
            FileUtils.whack(generatedJspRoot);

            // remove the internal archive directory which holds the original
            // archive (and possibly deployment plan) that cluster sync can use
            FileUtils.whack(getAppInternalDir());

            FileUtils.whack(getAppAltDDDir());

             // remove the root tenant dir for this application
            FileUtils.whack(getRootTenantDirForApp(parameters.name()));

            // remove the root tenant generated dir root for this application
            FileUtils.whack(getRootScratchTenantDirForApp(parameters.name()));
        } else if (parameters.origin == OpsParams.Origin.mt_unprovision) {
            // for unprovision application, remove the tenant dir
            FileUtils.whack(tenantDir);

            // and remove the generated dir
            File generatedRoot = getScratchDir(null);
            FileUtils.whack(generatedRoot);
        }
    }

    public ArchiveHandler getArchiveHandler() {
        return archiveHandler;
    }

    public void setArchiveHandler(ArchiveHandler archiveHandler) {
        this.archiveHandler = archiveHandler;
    }

    public ReadableArchive getOriginalSource() {
        return originalSource;
    }

    /**
     * Gets the module properties for modules
     *
     * @return a map containing module properties
     */
    public Map<String, Properties> getModulePropsMap() {
        return modulePropsMap;
    }

    /**
     * Sets the module properties for modules
     *
     * @param modulePropsMap
     */
    public void setModulePropsMap(Map<String, Properties> modulePropsMap) {
        this.modulePropsMap = modulePropsMap;
    }

    /**
     * Sets the parent context for the module
     *
     * @param parentContext
     */
    public void setParentContext(ExtendedDeploymentContext parentContext) {
        this.parentContext = parentContext;
    }

    /**
     * Gets the parent context of the module
     *
     *
     * @return the parent context
     */
    public ExtendedDeploymentContext getParentContext() {
        return parentContext;
    }

    /**
     * Gets the module uri for this module context
     *
     * @return the module uri
     */
    public String getModuleUri() {
        return moduleUri;
    }

   /**
     * Sets the module uri for this module context
     *
     * @param moduleUri
     */
    public void setModuleUri(String moduleUri) {
        this.moduleUri = moduleUri;
    }

    /**
     * Gets the archive handlers for modules
     *
     * @return a map containing module archive handlers
     */
    public Map<String, ArchiveHandler> getModuleArchiveHandlers() {
        return moduleArchiveHandlers;
    }

    /**
     * Gets the deployment context for modules
     *
     * @return a map containing module deployment contexts
     */
    public Map<String, ExtendedDeploymentContext> getModuleDeploymentContexts() {
        return moduleDeploymentContexts;
    }

    /**
     * Gets the action report for this context
     *
     * @return an action report
     */
    @Override
    public ActionReport getActionReport() {
        return actionReport;
    }

    public File getAppInternalDir() {
        final File internalDir = new File(env.getApplicationRepositoryPath(), INTERNAL_DIR_NAME);
        return new File(internalDir, VersioningUtils.getRepositoryName(parameters.name()));
    }

    public File getAppAltDDDir() {
        final File altDDDir = env.getApplicationAltDDPath();
        return new File(altDDDir, VersioningUtils.getRepositoryName(parameters.name()));
    }

    public void setTenant(final String tenant, final String appName) {
        this.tenant = tenant;
        this.originalAppName = appName;
        tenantDir = initTenantDir();
    }

    private File initTenantDir() {
        if (tenant == null || originalAppName == null) {
            return null;
        }
        File f = getRootTenantDirForApp(originalAppName);
        f = new File(f, tenant);
        if (!f.exists() && !f.mkdirs()) {
          if (deplLogger.isLoggable(FINEST)) {
              deplLogger.log(FINEST, "Unable to create directory {0}", f.getAbsolutePath());
          }
          
        }
        return f;
    }

    private File getRootTenantDirForApp(String appName) {
        File rootTenantDir = new File(env.getApplicationRepositoryPath(), APP_TENANTS_SUBDIR_NAME);
        File rootTenantDirForApp = new File(rootTenantDir, appName);
        return rootTenantDirForApp;
    }

    private File getRootScratchTenantDirForApp(String appName) {
        File rootScratchTenantDir = new File(env.getApplicationStubPath(), APP_TENANTS_SUBDIR_NAME);
        File rootScratchTenantDirForApp = new File(rootScratchTenantDir, appName);
        return rootScratchTenantDirForApp;
    }

    @Override
    public String getTenant() {
        return tenant;
    }

    @Override
    public File getTenantDir() {
        return tenantDir;
    }

    @Override
    public void postDeployClean(boolean isFinalClean) {
        boolean hotSwap = false;
        ServiceLocator serviceLocator = Globals.getDefaultHabitat();
        if (serviceLocator != null) {
            hotSwap = serviceLocator.getService(HotDeployService.class)
                    .getApplicationState(this)
                    .map(ApplicationState::isHotswap)
                    .orElse(false);
        }
        if (transientAppMetaData != null && !hotSwap) {
            if (isFinalClean) {
                transientAppMetaData.clear();
            } else {
                com.sun.enterprise.deploy.shared.FileArchive.clearCache();
            }
        }
        actionReport = null;
    }

    /**
     * Prepare the scratch directories, creating the directories if they do not
     * exist
     *
     * @throws java.io.IOException
     */
    @Override
    public void prepareScratchDirs() throws IOException {
        prepareScratchDir(getScratchDir("ejb"));
        prepareScratchDir(getScratchDir("xml"));
        prepareScratchDir(getScratchDir("jsp"));
    }

    private void prepareScratchDir(File f) throws IOException {
        if (!f.isDirectory() && !f.mkdirs())
            throw new IOException("Cannot create scratch directory : " + f.getAbsolutePath());
    }
}
