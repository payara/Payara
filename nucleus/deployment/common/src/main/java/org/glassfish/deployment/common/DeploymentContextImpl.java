/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2011 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.deployment.common;

import org.glassfish.deployment.versioning.VersioningUtils;
import java.lang.instrument.ClassFileTransformer;
import org.glassfish.api.ActionReport;
import org.glassfish.api.deployment.InstrumentableClassLoader;
import org.glassfish.api.deployment.OpsParams;
import org.glassfish.api.deployment.DeployCommandParameters;

import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.api.deployment.archive.ArchiveHandler;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.internal.api.ClassLoaderHierarchy;
import org.glassfish.internal.deployment.*;
import org.glassfish.loader.util.ASClassLoaderUtil;

import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.MalformedURLException;

import org.jvnet.hk2.component.PreDestroy;
import com.sun.enterprise.util.io.FileUtils;
import org.glassfish.hk2.classmodel.reflect.Parser;
import org.glassfish.hk2.classmodel.reflect.Types;

/**
 *
 * @author dochez
 */
public class DeploymentContextImpl implements ExtendedDeploymentContext, PreDestroy {

    private static final String INTERNAL_DIR_NAME = "__internal";

    ReadableArchive source;
    ReadableArchive originalSource;
    final OpsParams parameters;
    final Logger logger;
    ActionReport actionReport;
    final ServerEnvironment env;
    ClassLoader cloader;
    ArchiveHandler archiveHandler;
    Properties props;
    Map<String, Object> modulesMetaData = new HashMap<String, Object>();
    List<ClassFileTransformer> transformers = new ArrayList<ClassFileTransformer>();
    Phase phase = Phase.UNKNOWN;
    ClassLoader sharableTemp = null;
    Map<String, Properties> modulePropsMap = new HashMap<String, Properties>();
    Map<String, Object> transientAppMetaData = new HashMap<String, Object>();
    Map<String, ArchiveHandler> moduleArchiveHandlers = new HashMap<String, ArchiveHandler>();
    Map<String, ExtendedDeploymentContext> moduleDeploymentContexts = new HashMap<String, ExtendedDeploymentContext>();
    ExtendedDeploymentContext parentContext = null;
    String moduleUri = null;

    /** Creates a new instance of DeploymentContext */
    public DeploymentContextImpl(Deployment.DeploymentContextBuilder builder, ServerEnvironment env) {
        this(builder.report(), builder.logger(),  builder.sourceAsArchive(), builder.params(), env);
    }
    public DeploymentContextImpl(ActionReport actionReport, Logger logger, 
        ReadableArchive source, OpsParams params, ServerEnvironment env) {
        this.originalSource = source;
        this.source = source;
        this.logger = logger;
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
        return logger;
    }

    public synchronized void preDestroy() {
        try {
            PreDestroy.class.cast(sharableTemp).preDestroy();
        } catch (Exception e) {
          // ignore, the classloader does not need to be destroyed
        }
        try {
            PreDestroy.class.cast(cloader).preDestroy();
        } catch (Exception e) {
          // ignore, the classloader does not need to be destroyed
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
    public ClassLoader getClassLoader() {
        return getClassLoader(true);
    }

    public void setClassLoader(ClassLoader cloader) {
        this.cloader = cloader;
    }


    // this classloader will be used for sniffer retrieval, metadata parsing 
    // and the prepare
    public synchronized void createDeploymentClassLoader(ClassLoaderHierarchy clh, ArchiveHandler handler)
            throws URISyntaxException, MalformedURLException {
        this.addTransientAppMetaData(ExtendedDeploymentContext.IS_TEMP_CLASSLOADER, Boolean.TRUE);
        this.sharableTemp = createClassLoader(clh, handler, null); 
    }

    // this classloader will used to load and start the application
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
        if (phase==Phase.PREPARE) {
            if (sharable) {
                return sharableTemp;
            } else {
                InstrumentableClassLoader cl = InstrumentableClassLoader.class.cast(sharableTemp);
                return cl.copy();
            }
        } else {
            // we are out of the prepare phase, destroy the shareableTemp and 
            // return the final classloader
            if (sharableTemp!=null) { 
                try {
                    PreDestroy.class.cast(sharableTemp).preDestroy();
                } catch (Exception e) {
                    // ignore, the classloader does not need to be destroyed
                }
                sharableTemp=null;
            }
            return cloader;
        }
    }

    /**
     * Returns a scratch directory that can be used to store things in.
     * The scratch directory will be persisted accross server restart but
     * not accross redeployment of the same application
     *
     * @param subDirName the sub directory name of the scratch dir
     * @return the scratch directory for this application based on
     *         passed in subDirName. Returns the root scratch dir if the
     *         passed in value is null.
     */
    public File getScratchDir(String subDirName) {
        File rootScratchDir = env.getApplicationStubPath();
        if (subDirName != null )
            rootScratchDir = new File(rootScratchDir, subDirName);
        String appDirName = VersioningUtils.getRepositoryName(parameters.name());
        return new File(rootScratchDir, appDirName);
    }

    /**
     * {@inheritDoc}
     */
    public File getSourceDir() {

        return new File(getSource().getURI());
    }

    public void addModuleMetaData(Object metaData) {
        if (metaData!=null) {
            modulesMetaData.put(metaData.getClass().getName(), metaData);
        }
    }

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

    public Collection<Object> getModuleMetadata() {
        List<Object> copy = new ArrayList<Object>();
        copy.addAll(modulesMetaData.values());
        return copy;
    }

    public Map<String, Object> getTransientAppMetadata() {
        HashMap<String, Object> copy = new HashMap<String, Object>();
        copy.putAll(transientAppMetaData);
        return copy;
    }

    public void addTransientAppMetaData(String metaDataKey, Object metaData) {
        if (metaData!=null) {
            transientAppMetaData.put(metaDataKey, metaData);
        }
    }

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
     * Application level propertries are shared by all the modules.
     *
     * @return the application's properties.
     */
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
     * Add a new ClassFileTransformer to the context. Once all the deployers potentially
     * invalidating the application class loader (as indicated by the
     * @link {MetaData.invalidatesClassLoader()})
     * the deployment backend will recreate the application's class loader registering
     * all the ClassTransformers added by the deployers to this context.
     *
     * @param transformer the new class file transformer to register to the new application
     * class loader
     * @throws UnsupportedOperationException if the class loader we use does not support the
     * registration of a ClassFileTransformer. In such case, the deployer should either fail
     * deployment or revert to a mode without the byteocode enhancement feature.
     */
    public void addTransformer(ClassFileTransformer transformer) {

        transformers.add(transformer);
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
                libURIs.add(url.toURI());
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
            if(logger.isLoggable(Level.FINEST)){
                logger.log(Level.FINEST, "Detected [EXTENSION_LIST]" +
                        " installed-library [ " + url + " ] for archive [ "+source.getName()+ "]");
            }
        }

        return libURIs;
    }

    public void clean() {
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
    public ActionReport getActionReport() {
        return actionReport;
    }

    public File getAppInternalDir() {
        final File internalDir = new File(env.getApplicationRepositoryPath(), INTERNAL_DIR_NAME);
        return new File(internalDir, VersioningUtils.getRepositoryName(parameters.name()));
    }

    @Override
    public void postDeployClean(boolean isFinalClean) {
        if (transientAppMetaData != null) {
            if (isFinalClean) {
                transientAppMetaData.clear();
            } else {
                final String [] classNamesToClean = {Types.class.getName(), Parser.class.getName()};
        
                for (String className : classNamesToClean) {
                    transientAppMetaData.remove(className);
                }
            }
        }
        actionReport = null;
    }
}
