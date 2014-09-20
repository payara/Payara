/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

import java.util.Properties;

/**
 * Useful services for application loading implementation
 *
 */
public interface ApplicationContext {
    
    /**
     * Returns the class loader associated with the application.
     * ClassLoader instances are usually obtained by the getClassLoader API on
     * the associated ArchiveHandler for the archive type being deployed.
     *
     * This can return null and the container should allocate a ClassLoader
     * while loading the application.
     *
     * @link {org.jvnet.glassfish.api.deployment.archive.ArchiveHandler.getClassLoader()}
     *
     * @return a class loader capable of loading classes and resources from the
     * source
     */
    public ClassLoader getClassLoader();

    /**
     * Returns the application level properties that will be persisted as a
     * key value pair at then end of deployment. That allows individual
     * Deployers implementation to store some information at the
     * application level that should be available upon server restart.
     * Application level propertries are shared by all the modules.
     *
     * @return the application's properties.
     */
    public Properties getAppProps();

    /**
     * Returns the module level properties that will be persisted as a
     * key value pair at then end of deployment. That allows individual
     * Deployers implementation to store some information at the module
     * level that should be available upon server restart.
     * Module level properties are only visible to the current module.
     * @return the module's properties.
     */
    public Properties getModuleProps();
}
