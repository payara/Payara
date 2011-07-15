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

import com.sun.enterprise.module.ModuleDefinition;

/**
 * MetaData associated with a Deployer. This is used by the deployment layers
 * to identify the special requirements of the Deployer.
 *
 * Supported Requirements :
 *      invalidatesClassLoader  Deployer can load classes that need to be reloaded
 *                              for the application to run successfully hence requiring
 *                              the class loader to be flushed and reinitialized between
 *                              the prepare and load phase.
 *      componentAPIs           Components can use APIs that are defined outside of the
 *                              component's bundle. These component's APIs (eg. Java EE
 *                              APIs) must be imported by the application class loader
 *                              before any application code is loaded.
 */
public class MetaData {

    final static Class[] empty = new Class[0];

    private final boolean invalidatesCL;
    private final Class[] requires;
    private final Class[] provides;

    /**
     * Constructor for the Deployer's metadata
     *
     * @param invalidatesClassLoader If true, invalidates the class loader used during
     * the deployment's prepare phase
     *
     */
    public MetaData(boolean invalidatesClassLoader, Class[] provides, Class[] requires) {
        this.invalidatesCL = invalidatesClassLoader;
        this.provides = provides;
        this.requires = requires;
    }

    /**
     * Returns whether or not the class loader is invalidated by the Deployer's propare
     * phase.
     * 
     * @return true if the class loader is invalid after the Deployer's prepare phase
     * call.
     */
    public boolean invalidatesClassLoader() {
        return invalidatesCL;
    }

    /**
     * Returns the list of types of metadata this deployer will provide to the deployement
     * context upon the successful completion of the prepare method.
     *
     * @return list of metadata type;
     */
    public Class[] provides() {
        if (provides==null) {
            return empty;
        }
        return provides;
    };                 

    /**
     * Returns the list of types of metadata this deployer will require to run successfully
     * the prepare method.
     *
     * @return list of metadata required type;
     */
    public Class[] requires() {
        if (requires==null) {
            return empty;
        }
        return requires;
    }
}
