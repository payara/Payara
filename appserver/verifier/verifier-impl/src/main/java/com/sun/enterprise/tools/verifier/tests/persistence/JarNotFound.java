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

package com.sun.enterprise.tools.verifier.tests.persistence;

import org.glassfish.deployment.common.Descriptor;
import com.sun.enterprise.deployment.PersistenceUnitDescriptor;
import com.sun.enterprise.deployment.BundleDescriptor;
import org.glassfish.deployment.common.ModuleDescriptor;
import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.tests.VerifierCheck;
import com.sun.enterprise.tools.verifier.tests.VerifierTest;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.glassfish.deployment.common.DeploymentUtils;
import org.glassfish.deployment.common.RootDeploymentDescriptor;

/**
 * jar files specified using <jar-file> element in persistence.xml should be
 * present in the archive.
 *
 * @author Sanjeeb.Sahoo@Sun.COM
 */
public class JarNotFound extends VerifierTest implements VerifierCheck {
    public Result check(Descriptor descriptor) {
        Result result = getInitializedResult();
        result.setStatus(Result.PASSED);
        addErrorDetails(result,
                getVerifierContext().getComponentNameConstructor());
        PersistenceUnitDescriptor pu = PersistenceUnitDescriptor.class.cast(
                descriptor);
        File absolutePURootFile = getAbsolutePuRootFile(pu);
        logger.fine("Absolute PU Root: " + absolutePURootFile);
        String absolutePuRoot = absolutePURootFile.getAbsolutePath();
        List<String> jarFileNames = new ArrayList<String>(pu.getJarFiles());
        for (String jarFileName : jarFileNames) {
            // ASSUMPTION:
            // Because of the way deployment changes names of directories etc.
            // it is very difficult to back calculate path names. So,
            // the following code assumes user is specifying valid URIs.

            // in the xml, names always use '/'
            String nativeJarFileName = jarFileName.replace('/',
                    File.separatorChar);
            final File parentFile = new File(absolutePuRoot).getParentFile();
            // only components are exploded, hence first look for original archives.
            File jarFile = new File(parentFile, nativeJarFileName);
            if (!jarFile.exists()) {
                // if the referenced jar is itself a component, then
                // it might have been exploded, hence let's see
                // if that is the case.

                // let's calculate the name component and path component from this URI
                // e.g. if URI is ../../foo_bar/my-ejb.jar,
                // name component is foo_bar/my-ejb.jar and
                // path component is ../../
                // These are my own notions used here.
                String pathComponent = "";
                String nameComponent = jarFileName;
                if(jarFileName.lastIndexOf("../") != -1) {
                    final int separatorIndex = jarFileName.lastIndexOf("../")+3;
                    pathComponent = jarFileName.substring(0,separatorIndex);
                    nameComponent = jarFileName.substring(separatorIndex);
                }
                logger.fine("For jar-file="+ jarFileName+ ", " +
                        "pathComponent=" +pathComponent +
                        ", nameComponent=" + nameComponent);
                File parentPath = new File(parentFile, pathComponent);
                jarFile = new File(parentPath, DeploymentUtils.
                        getRelativeEmbeddedModulePath(parentPath.
                        getAbsolutePath(), nameComponent));

                if (!jarFile.exists()) {
                    result.failed(smh.getLocalString(
                            getClass().getName() + "failed",
                            "[ {0} ] specified in persistence.xml does not exist in the application.",
                            new Object[]{jarFileName}));
                }
            }
        }
        return result;
    }

    private File getAbsolutePuRootFile(
            PersistenceUnitDescriptor persistenceUnitDescriptor) {
        final String applicationLocation =
                getVerifierContext().getAbstractArchive().getURI().getPath();
        File absolutePuRootFile = new File(applicationLocation,
                getAbsolutePuRoot(applicationLocation, 
                persistenceUnitDescriptor).replace('/', File.separatorChar));
        if (!absolutePuRootFile.exists()) {
            throw new RuntimeException(
                    absolutePuRootFile.getAbsolutePath() + " does not exist!");
        }
        return absolutePuRootFile;
    }

    /**
     * This method calculates the absolute path of the root of a PU.
     * Absolute path is not the path with regards to root of file system.
     * It is the path from the root of the Java EE application this
     * persistence unit belongs to.
     * Returned path always uses '/' as path separator.
     * @param applicationLocation absolute path of application root
     * @param persistenceUnitDescriptor
     * @return the absolute path of the root of this persistence unit
     */
    private String getAbsolutePuRoot(String applicationLocation,
            PersistenceUnitDescriptor persistenceUnitDescriptor) {
        RootDeploymentDescriptor rootDD = persistenceUnitDescriptor.getParent().                getParent();
        String puRoot = persistenceUnitDescriptor.getPuRoot();
        if(rootDD.isApplication()){
            return puRoot;
        } else {
            ModuleDescriptor module = BundleDescriptor.class.cast(rootDD).
                    getModuleDescriptor();
            if(module.isStandalone()) {
                return puRoot;
            } else {
                final String moduleLocation =
                        DeploymentUtils.getRelativeEmbeddedModulePath(
                        applicationLocation, module.getArchiveUri());
                return moduleLocation + '/' + puRoot; // see we always '/'
            }
        }
    }

}
