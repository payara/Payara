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

package org.glassfish.api.container;

import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.api.deployment.archive.ArchiveType;
import org.glassfish.api.deployment.DeploymentContext;
import org.jvnet.hk2.annotations.Contract;

import java.io.IOException;
import java.util.logging.Logger;
import java.lang.annotation.Annotation;

import com.sun.enterprise.module.Module;
import java.util.Map;

/**
 * A sniffer implementation is responsible for identifying a particular
 * application type and/or a particular file type.
 *
 * <p>
 * For clients who want to work with Sniffers, see <tt>SnifferManager</tt> in the kernel.
 *
 * @author Jerome Dochez
 */
@Contract
public interface Sniffer {

    /**
     * Returns true if the passed file or directory is recognized by this
     * sniffer.
     * @param context deployment context
     * @return true if the location is recognized by this sniffer
     */
    public boolean handles(DeploymentContext context);

    /**
     * Returns true if the passed file or directory is recognized by this
     * sniffer.
     * @param source the file or directory abstracted as an archive
     * resources from the source archive.
     * @return true if the location is recognized by this sniffer
     */
    public boolean handles(ReadableArchive source);

    /**
     * Returns the array of patterns to apply against the request URL
     * If the pattern matches the URL, the service method of the associated
     * container will be invoked
     * @return array of patterns
     */
    public String[] getURLPatterns();

    /**
     * Returns the list of annotations types that this sniffer is interested in.
     * If an application bundle contains at least one class annotated with
     * one of the returned annotations, the deployment process will not
     * call the handles method but will invoke the containers deployers as if
     * the handles method had been called and returned true.
     *
     * @return list of annotations this sniffer is interested in or an empty array
     */
    public Class<? extends Annotation>[] getAnnotationTypes();
    
    /**
     * Returns the list of annotation names that this sniffer is interested in.
     * If an application bundle contains at least one class annotated with
     * one of the returned annotations, the deployment process will not
     * call the handles method but will invoke the containers deployers as if
     * the handles method had been called and returned true.
     *
     * @param context deployment context
     * @return list of annotation names this sniffer is interested in or an empty array
     */
    public String[] getAnnotationNames(DeploymentContext context);

    /**
     * Returns the container type associated with this sniffer
     * @return the container type
     */
    public String getModuleType(); // This method should be renamed to getContainerType

   /**                                          
     * Sets up the container libraries so that any imported bundle from the
     * connector jar file will now be known to the module subsystem
     *
     * This method returns a {@link Module}s for the module containing
     * the core implementation of the container. That means that this module
     * will be locked as long as there is at least one module loaded in the
     * associated container.
     *
     * @param containerHome is where the container implementation resides
     * @param logger the logger to use
     * @return the module definition of the core container implementation.
     *
     * @throws java.io.IOException exception if something goes sour
     */
    public Module[] setup(String containerHome, Logger logger) throws IOException;

   /**
     * Tears down a container, remove all imported libraries from the module
     * subsystem.
     *
     */
    public void tearDown();

    /**
     * Returns the list of Containers that this Sniffer enables.
     *
     * The runtime will look up each container implementing
     * using the names provided in the habitat.
     *
     * @return list of container names known to the habitat for this sniffer
     */
    public String[] getContainersNames();

    /** 
     * @return whether this sniffer should be visible to user
     * 
     */
    public boolean isUserVisible();
    
    /**
     * @return whether this sniffer represents a Java EE container type
     *
     */
    public boolean isJavaEE();

    /**
     * Returns a map of deployment configurations for this Sniffer from the
     * specific archive source.  
     * <p>
     * Many sniffers (esp. Java EE sniffers) will choose to set the key of each 
     * map entry to the relative path within the ReadableArchive of the 
     * deployment descriptor and the value of that map entry to the
     * descriptor's contents.
     * 
     * @param source the contents of the application's archive
     * @return map of configuration names to configurations for the application
     * @throws java.io.IOException in case of errors searching or reading the
     * archive for the deployment configuration(s)
     */
    public Map<String,String> getDeploymentConfigurations(final ReadableArchive source) throws IOException;

    /** 
     * @return the set of the sniffers that should not co-exist for the 
     * same module. For example, ejb and appclient sniffers should not 
     * be returned in the sniffer list for a certain module.
     * This method will be used to validate and filter the retrieved sniffer
     * lists for a certain module
     * 
     */
    public String[] getIncompatibleSnifferTypes();

    /**
     *
     * This API is used to help determine if the sniffer should recognize 
     * the current archive.
     * If the sniffer does not support the archive type associated with 
     * the current deployment, the sniffer should not recognize the archive.
     *
     * @param archiveType the archive type to check
     * @return whether the sniffer supports the archive type
     *
     */
    public boolean supportsArchiveType(ArchiveType archiveType);
}
