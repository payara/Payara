/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.api.deployment;

import org.glassfish.api.ExecutionContext;
import org.glassfish.api.ActionReport;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.api.deployment.archive.ArchiveHandler;
import java.io.File;
import java.lang.instrument.ClassFileTransformer;
import java.util.Properties;
import java.util.Collection;
import java.util.Map;
import java.util.List;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Useful services for Deployer service implementation
 *
 * @author Jerome Dochez
 */
public interface DeploymentContext extends ApplicationContext, ExecutionContext {

    /**
     * Application bits, at the raw level. Deployer's should avoid
     * using such low level access as it binds the deployer to a particular directory
     * layout. Instead Deployers should use the class loader obtained via the getClassLoader() API
     *
     * @return Abstraction to the application's source archive.
     */
    public ReadableArchive getSource();

    /**
     * During the prepare phase, when a deployer need to have access to the class loader
     * that will be used to load the application in the runtime container, it can call
     * this API during the prepare phase. Otherswise, deployers should use the
     * getClassLoader API.
     *
     * If a deployers needs to have access to the classloader during the prepare phase
     *
     *
     * @return the final class loader
     */
    public ClassLoader getFinalClassLoader();

    /**
     * Returns the DeployCommand parameters
     * @param opsParamsType expected deployment operation parameters type.
     * @return the command parameters
     */
    public <U extends OpsParams> U getCommandParameters(Class<U> opsParamsType);

    /**
     * Returns a scratch directory that can be used to store things in.
     * The scratch directory will be persisted accross server restart but not 
     * accross redeployment of the same application
     *
     * @param subDirName the sub directory name of the scratch dir
     * @return the specific scratch subdirectory for this application based on 
     *         passed in subDirName. Returns the root scratch dir if the 
     *         passed in value is null.      
     */
    public File getScratchDir(String subDirName);
    
    /**
     * Returns the directory where the original applications bits should be 
     * stored. This is useful when users deploy an archive file that need to 
     * be unzipped somewhere for the container to work with. 
     * 
     * @return the source directory for this application
     */
    public File getSourceDir();

    /**
     * Stores a descriptor for the module in the context so other deployer's
     * can have access to it. Module meta-data is usual not persistent which
     * mean that any modification to it will not be available at the next
     * server restart and will need to be reset.
     *
     * @param metaData the meta data itself
     */
    public void addModuleMetaData(Object metaData);

    /**
     * Returns the meta data associated with a module type.
     *
     * @param metadataType type of the meta date.
     * @return instance of T or null
     */
    public <T> T getModuleMetaData(Class<T> metadataType);


    /**
     * Gets the archive handlers for modules
     *
     * @return a map containing module archive handlers
     */
    public Map<String, ArchiveHandler> getModuleArchiveHandlers();

    /**
     * Stores application level metadata in the context so other deployer's
     * can have access to it. The transient meta-data is not persistent which
     * mean that any modification to it will not be available at the next
     * server restart and will need to be reset.
     *
     * @param metaDataKey key of the meta date.
     * @param metaData the meta data itself
     */
    public void addTransientAppMetaData(String metaDataKey, Object metaData);

    /**
     * Returns the meta data for the given key
     *
     * @param metaDataKey key of the meta date.
     * @param metadataType type of the meta date.
     * @return instance of T or null
     */
    public  <T> T getTransientAppMetaData(String metaDataKey, Class<T> metadataType);

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
     * deployment or revert to a mode without the bytecode enhancement feature.
     */
    public void addTransformer(ClassFileTransformer transformer);

    /**
     * Returns all the metadata associated with this deployment
     *
     * @return collection of metadata added to the context
     */
    public Collection<Object> getModuleMetadata();  

    /**
     * Returns all the transient app metadata associated with this deployment
     *
     * @return collection of metadata added to the context
     */
    public Map<String, Object> getTransientAppMetadata();  

    /**
     * Returns the archive handler that's associated with this context
     *
     * @return archive handler
     */
    public ArchiveHandler getArchiveHandler();

    /**
     * Gets the original source archive
     * In case of archive deployment, this will return the archive before
     * expanding. In case of directory deployment, this will return the same
     * thing as getSource()
     *
     * @return the original source archive
     */
    public ReadableArchive getOriginalSource();

    /**
     * Gets the module properties for modules
     *
     * @return a map containing module properties
     */
    public Map<String, Properties> getModulePropsMap();

    /**
     * Gets the action report for this context
     *
     * @return an action report
     */
    public ActionReport getActionReport();

    /**
     * gets the app-libs specified for this archive<br>
     * This list includes --libraries as well EXTENSION_LIST specified in the manifest entries
     * @return list of library URIs
     * @throws URISyntaxException  when unable to get the library URIs
     */
    public List<URI> getAppLibs()
            throws URISyntaxException;
}
