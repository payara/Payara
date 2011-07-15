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

package com.sun.enterprise.tools.verifier.connector;

import java.io.File;
import java.io.IOException;

import com.sun.enterprise.deployment.ConnectorDescriptor;
import org.glassfish.deployment.common.Descriptor;
import com.sun.enterprise.tools.verifier.BaseVerifier;
import com.sun.enterprise.tools.verifier.VerifierFrameworkContext;
import com.sun.enterprise.tools.verifier.SpecVersionMapper;
import com.sun.enterprise.tools.verifier.apiscan.classfile.ClassFileLoaderFactory;
import com.sun.enterprise.tools.verifier.apiscan.packaging.ClassPathBuilder;
import com.sun.enterprise.tools.verifier.apiscan.stdapis.ConnectorClosureCompiler;
import com.sun.enterprise.util.io.FileUtils;

/**
 * @author Vikas Awasthi
 */
public class ConnectorVerifier extends BaseVerifier {

    private ConnectorDescriptor cond = null;
    private String classPath;//this is lazily populated in getClassPath()
    private boolean isASMode = false;

    public ConnectorVerifier(VerifierFrameworkContext verifierFrameworkContext,
                             ConnectorDescriptor cond) {
        this.verifierFrameworkContext = verifierFrameworkContext;
        this.cond = cond;
        this.isASMode = !verifierFrameworkContext.isPortabilityMode();
    }

    public void verify() throws Exception {
        if (areTestsNotRequired(verifierFrameworkContext.isConnector()))
            return;

        preVerification();
        createClosureCompiler();//this can be moved up to base verifier in future.
        verify(cond, new ConnectorCheckMgrImpl(verifierFrameworkContext));
    }

    public Descriptor getDescriptor() {
        return cond;
    }

    protected ClassLoader createClassLoader()
            throws IOException {
        return cond.getClassLoader();
    }

    protected String getArchiveUri() {
        return FileUtils.makeFriendlyFilename(cond.getModuleDescriptor().getArchiveUri());
    }

    protected String[] getDDString() {
        String dd[] = {"META-INF/sun-ra.xml", "META-INF/ra.xml"}; // NOI18N
        return dd;
    }

    /**
     * Creates and returns the class path associated with the rar.
     * Uses the exploded location of the archive for generating the classpath.
     *
     * @return entire classpath string
     * @throws IOException
     */
    protected String getClassPath() throws IOException {
        if (classPath != null) return classPath;

        if(isASMode)
            return (classPath = getClassPath(verifierFrameworkContext.getClassPath()));

        String cp;
        if (!cond.getModuleDescriptor().isStandalone()) {
            //take the cp from the enclosing ear file
            String ear_uri = verifierFrameworkContext.getExplodedArchivePath();
            File ear = new File(ear_uri);
            assert(ear.isDirectory());
            cp = ClassPathBuilder.buildClassPathForEar(ear);
            String libdir = cond.getApplication().getLibraryDirectory();
            if (libdir!=null) {
                cp = getLibdirClasspath(ear_uri, libdir) + cp;
            }
            /** buildClasspathForEar takes care of embedded rars.*/
/*
            //this is a relative path
            String module_uri = cond.getModuleDescriptor().getArchiveUri();
            File module = new File(module_uri);
            assert(module.isFile() && !module.isAbsolute());
            // exploder creates the directory replacing all dots by '_'
            File explodedModuleDir = new File(ear_uri,
                    FileUtils.makeFriendlyFilename(module_uri));
            String moduleCP = ClassPathBuilder.buildClassPathForRar(
                    explodedModuleDir);
            cp = moduleCP + File.pathSeparator + earCP;
*/
        } else {
            //this is an absolute path
            String module_uri = verifierFrameworkContext.getExplodedArchivePath();
            File module = new File(module_uri);
            assert(module.isDirectory() && module.isAbsolute());
            cp = ClassPathBuilder.buildClassPathForRar(module);
        }
        return (classPath = cp);
    }

    /**
     * creates the ClosureCompiler for the rar module and sets it to the
     * verifier context. This is used to compute the closure on the classes used
     * in the rar.
     *
     * @throws IOException
     */
    protected void createClosureCompiler() throws IOException {
        String specVer = SpecVersionMapper.getJCAVersion(
                verifierFrameworkContext.getJavaEEVersion());
        Object arg = (isASMode)?cond.getClassLoader():(Object)getClassPath();
        ConnectorClosureCompiler cc = new ConnectorClosureCompiler(specVer,
                ClassFileLoaderFactory.newInstance(new Object[]{arg}));
        context.setClosureCompiler(cc);
    }
}
