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

package org.glassfish.api.deployment.archive;

import org.glassfish.api.deployment.DeploymentContext;
import org.jvnet.hk2.annotations.Contract;

import java.io.IOException;
import java.util.jar.Manifest;
import java.util.List;
import java.net.URI;

/**
 * ArchiveHandlers are handling certain archive type. An archive has a unique type which is usually defines how
 * classes and resources are loaded from the archive. An archive is also known as a module. It represents a unit
 * of deployment.
 *
 * ArchiveHandler should be stateless objects although the implementations of this contract can
 * control that using the scope element of the @Service annotation.
 * 
 * @author Jerome Dochez, Sanjeeb Sahoo
 */
@Contract
public interface ArchiveHandler {

    // TODO(Sahoo): Introduce abstraction like DeploymentUnit that's stateful and maps straight to a war/jar/ear/etc.

    /**
     * This method is semantically equivalent to {@link ArchiveDetector#getArchiveType()} except that
     * this method returns string equivalent of ArchiveType because of backward compatibility reasons.
     *
     * @return the type of the archive or deployment unit handled by this handler
     * @see ArchiveDetector#getArchiveType()
     */
    public String getArchiveType();

    /**
     * Returns the default name by which the specified archive can be 
     * identified.
     * <p>
     * The default name is used, for example, during deployment if no name
     * was specified explicitly as part of the deployment request.  
     * @param archive the archive for which to provide the default name
     * @return the default name for identifying the specified archive
     */
    public String getDefaultApplicationName(ReadableArchive archive);

    public String getDefaultApplicationName(ReadableArchive archive, DeploymentContext context);

    /**
     * Returns the version identifier by which the specified archive can be
     * deployed.<p>
     * The version identifier is used during deployment if no version identifier
     * was specified <code>null</code> must be returned
     * @param archive the archive for which to provide the version identifier
     * @return the version identifier for versioning the deployment archive or <code>null</code>
     */
    public String getVersionIdentifier(ReadableArchive archive);

    /**
     * Returns true if this handler understands the specified archive and
     * can process it.
     *
     * @throws IOException
     *      The implementation of this method is expected to interact with
     *      the given archive, and if methods on {@link ReadableArchive}
     *      throws an {@link IOException}, it can be simply tunneled to the caller.
     */
    public boolean handles(ReadableArchive archive) throws IOException;

    /**
     * Creates a classloader that can load code from inside the archive.
     *
     * @param parent
     *      The newly created classloader to be returned must eventually delegate to this classloader.
     *      (This classloader is capable of resolving APIs and other things that the container
     * @param context
     */
    public ClassLoader getClassLoader(ClassLoader parent, DeploymentContext context);
    
    /**
     * Prepares the jar file to a format the ApplicationContainer is
     * expecting. This could be just a pure unzipping of the jar or
     * nothing at all.
     * @param source of the expanding
     * @param target of the expanding
     * @param context
     */
    public void expand(ReadableArchive source, WritableArchive target, DeploymentContext context) throws IOException;

    /**
     * Returns the manifest file for this archive, this file is usually located at
     * the META-INF/MANIFEST location, however, certain archive type can change this
     * default location or use another mean of expressing manifest information.
     *
     * @param archive file
     * @return manifest instance or null if this archive has no manifest
     */
    public Manifest getManifest(ReadableArchive archive) throws IOException;

    /**
     * Returns the classpath URIs for this archive.
     *
     * @param archive file
     * @return classpath URIs for this archive
     */
    public List<URI> getClassPathURIs(ReadableArchive archive);

    /**
     * Returns whether this archive requires annotation scanning.
     *
     * @param archive file
     * @return whether this archive requires annotation scanning
     */
    public boolean requiresAnnotationScanning(ReadableArchive archive);

}
