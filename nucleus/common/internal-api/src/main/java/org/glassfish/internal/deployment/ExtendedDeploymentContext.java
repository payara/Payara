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

package org.glassfish.internal.deployment;

import java.io.File;
import java.io.IOException;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.api.deployment.archive.ArchiveHandler;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.internal.api.ClassLoaderHierarchy;

import java.lang.instrument.ClassFileTransformer;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.net.URISyntaxException;
import java.net.MalformedURLException;

/**
 * semi-private interface to the deployment context
 *
 * @author Jerome Dochez
 */
public interface ExtendedDeploymentContext extends DeploymentContext {
    
    public enum Phase { UNKNOWN, PREPARE, PREPARED, LOAD, START, STOP, UNLOAD, CLEAN, REPLICATION }

    public static final String IS_TEMP_CLASSLOADER = "isTempClassLoader";
    public static final String TRACKER = "tracker";


    /**
     * Sets the phase of the deployment activity.
     * 
     * @param newPhase
     */
    public void setPhase(Phase newPhase);

    public Phase getPhase();

    /**
     * Returns the list of transformers registered to this context.
     *
     * @return the transformers list
     */
    public List<ClassFileTransformer> getTransformers();

    /**
     * Create the deployment class loader. It will be used for sniffer 
     * retrieval, metadata parsing and deployer prepare. 
     *
     * @param clh the hierarchy of class loader for the parent
     * @param handler the archive handler for the source archive
     */
    public void createDeploymentClassLoader(ClassLoaderHierarchy clh, ArchiveHandler handler)
            throws URISyntaxException, MalformedURLException;

    /**
     * Create the final class loader. It will be used to load and start
     * application.
     *
     * @param clh the hierarchy of class loader for the parent
     * @param handler the archive handler for the source archive
     */
    public void createApplicationClassLoader(ClassLoaderHierarchy clh, ArchiveHandler handler)
            throws URISyntaxException, MalformedURLException;

    public void clean();

    /**
     * Sets the archive handler that's associated with this context
     *
     * @param archiveHandler
     */
    public void setArchiveHandler(ArchiveHandler archiveHandler);

    /**
     * Sets the source archive
     *
     * @param props
     */
    public void setSource(ReadableArchive source);

    /**
     * Sets the module properties for modules
     *
     * @param modulePropsMap
     */
    public void setModulePropsMap(Map<String, Properties> modulePropsMap);

    /**
     * Gets the deployment context for modules
     *
     * @return a map containing module deployment contexts
     */
    public Map<String, ExtendedDeploymentContext> getModuleDeploymentContexts();

   /**
     * Sets the classloader
     *
     * @param cloader
     */
    public void setClassLoader(ClassLoader cloader);

   /**
     * Sets the parent context
     *
     * @param parentContext 
     */
    public void setParentContext(ExtendedDeploymentContext parentContext);

    /**
     * Gets the module uri for this module context
     *
     * @return the module uri
     */
    public String getModuleUri();

   /**
     * Sets the module uri for this module context
     *
     * @param moduleUri
     */
    public void setModuleUri(String moduleUri);

    /**
     * Gets the parent context for this context
     *
     * @return the parent context
     */
    public ExtendedDeploymentContext getParentContext();


    /**
     * Returns the internal directory for the application (used for holding
     * the uploaded archive, for example).
     *
     * @return location of the internal directory for the application
     */
    public File getAppInternalDir();

    /**
     * Returns the alternate deployment descriptor directory for the 
     * application (used for holding the external alternate deployment 
     * descriptors).
     *
     * @return location of the alternate deployment descriptor directory for 
     *  the application
     */
    public File getAppAltDDDir();


    /**
     * Returns the tenant, if one is valid for this DeploymentContext.
     * @return tenant name if applicable, null if no tenant is set for this DC
     */
    public String getTenant();

    /**
     * Sets the tenant to which this deployment context applies. Also initializes
     * the tenant directory.
     *
     * @param tenantName the name of the tenant
     * @param appName the name of the application
     */
    public void setTenant(String tenant, String appName);

    /**
     * Returns the directory containing the expanded tenant customization archive,
     * if this DC is for a tenant and if a customization archive was specified
     * when the tenant was provisioned.
     * @return directory containing the expanded customization archive; null if none
     */
    public File getTenantDir();

    /**
     * Performs any clean-up of the deployment context after deployment has
     * finished.
     * <p>
     * This method can be invoked either with "true", meaning that this is the
     * final clean-up for the DC, or with "false," meaning that the DC
     * implementation should be selective.  (Some data is used, for instance,
     * in the DeployCommand logic after ApplicationLifeCycle.deploy has
     * completed.)
     * 
     * @param isFinalClean whether this clean is the final clean or a selective one.
     */
    public void postDeployClean(boolean isFinalClean);

    /**
     * Prepare the scratch directories, creating the directories 
     * if they do not exist
     */
    public void prepareScratchDirs() throws IOException;
}
