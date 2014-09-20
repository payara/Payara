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

package com.sun.jdo.spi.persistence.support.ejb.codegen;

import java.io.File;
import java.util.Collection;

import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.ejb.deployment.descriptor.EjbBundleDescriptorImpl;
import org.glassfish.ejb.deployment.descriptor.IASEjbCMPEntityDescriptor;

/**
 * This interface must be implemented by all CMP code generators.
 */

public interface CMPGenerator {

    /**
     * This method is called once for each ejb module in the application
     * that contains CMP beans.
     * Only one #init() method can be called.
     * @deprecated
     * This method is not used by the deployment back end, and should be removed
     * as soon as the TestFramework is fixed.
     * @param ejbBundleDescriptor the EjbBundleDescriptor associated with this
     * ejb module.
     * @param cl the ClassLoader that loaded user defined classes.
     * @param bundlePathName full path to the directory where this bundle's
     * files are located.
     * @throws GeneratorException if there is a problem initializing bean 
     * processing.
     */
    void init(EjbBundleDescriptorImpl ejbBundleDescriptor, ClassLoader cl,
        String bundlePathName) throws GeneratorException;

    /**
     * This method is called once for each ejb module in the application
     * that contains CMP beans.
     * Only one #init() method can be called.
     * @param ejbBundleDescriptor the EjbBundleDescriptor associated with this
     * ejb module.
     * @param ctx the DeploymentContext associated with the deployment request.
     * @param bundlePathName full path to the directory where this bundle's
     * files are located.
     * @param generatedXmlsPathName full path to the directory where the 
     * generated files are located.
     * @throws GeneratorException if there is a problem initializing bean 
     * processing.
     */
    void init(EjbBundleDescriptorImpl ejbBundleDescriptor, DeploymentContext ctx,
        String bundlePathName, String generatedXmlsPathName) 
            throws GeneratorException;

    /**
     * This method is called once for each CMP bean of the corresponding ejb module.
     * @param descr the IASEjbCMPEntityDescriptor associated with this CMP bean.
     * @param srcout the location of the source files to be generated.
     * @param classout the location of the class files to be generated.
     * @throws GeneratorException if there is a problem processing the bean.
     */
    void generate(IASEjbCMPEntityDescriptor descr, File srcout, File classout)
        throws GeneratorException;

    /**
     * This method is called once for each ejb module in the application
     * that contains CMP beans. It is called at the end of the module processing.
     * @return a Collection of files to be compiled by the deployment process.
     * @throws GeneratorException if there is any problem.
     */
    Collection<File> cleanup() throws GeneratorException;

    /**
     * This method may be called once for each CMP bean of the corresponding 
     * ejb module to perform the validation.
     * @param descr the IASEjbCMPEntityDescriptor associated with this CMP bean.
     * @return a Collection of Exceptions if there are any problems processing the bean.
     * Returns an empty Collection if validation succeeds.
     */
    Collection validate(IASEjbCMPEntityDescriptor descr);

} 
