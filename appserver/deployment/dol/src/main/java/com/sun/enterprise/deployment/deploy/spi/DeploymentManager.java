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

/*
 * DeploymentManager.java
 *
 * Created on April 21, 2004, 9:44 AM
 */

package com.sun.enterprise.deployment.deploy.spi;

import org.glassfish.api.deployment.archive.Archive;
import org.glassfish.api.deployment.archive.WritableArchive;

import javax.enterprise.deploy.spi.Target;
import javax.enterprise.deploy.spi.status.ProgressObject;
import java.io.IOException;

/**
 *
 * @author  dochez
 */
public interface DeploymentManager 
    extends javax.enterprise.deploy.spi.DeploymentManager {
    
   /**
    * The distribute method performs three tasks; it validates the
    * deployment configuration data, generates all container specific 
    * classes and interfaces, and moves the fully baked archive to 
    * the designated deployment targets.
    *
    * @param targetList   A list of server targets the user is specifying
    *                     this application be deployed to. 
    * @param moduleArchive The abstraction for the application 
    *                      archive to be disrtibuted.
    * @param deploymentPlan The archive containing the deployment
    *                       configuration information associated with
    *                       this application archive.
    * @param deploymentOptions is a JavaBeans compliant component 
    *                   containing all deployment options for this deployable
    *                   unit. This object must be created using the 
    *                   BeanInfo instance returned by 
    *                   DeploymentConfiguration.getDeploymentOptions
    * @throws IllegalStateException is thrown when the method is
    *                    called when running in disconnected mode.
    * @return ProgressObject an object that tracks and reports the 
    *                       status of the distribution process.
    *
    */

    public ProgressObject distribute(Target[] targetList,
           Archive moduleArchive, Archive deploymentPlan,
           Object deploymentOptions)
           throws IllegalStateException;
    
    /**
     * Creates a new instance of WritableArchive which can be used to 
     * store application elements in a layout that can be directly used by 
     * the application server. Implementation of this method should carefully
     * return the appropriate implementation of the interface that suits 
     * the server needs and provide the fastest deployment time.
     * An archive may already exist at the location and elements may be 
     * read but not changed or added depending on the underlying medium.
     * @param path the directory in which to create this archive if local 
     * storage is a possibility. 
     * @param name is the desired name for the archive
     * @return the writable archive instance
     */    
    public WritableArchive getArchive(java.net.URI path, String name)
        throws IOException;

}
