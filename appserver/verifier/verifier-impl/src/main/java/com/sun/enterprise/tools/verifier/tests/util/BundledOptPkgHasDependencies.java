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
 * JarFileDependsOnOutsidePackage.java
 *
 * Created on August 30, 2004, 10:16 AM
 */

package com.sun.enterprise.tools.verifier.tests.util;

import com.sun.enterprise.tools.verifier.Result;
import java.io.File;
import java.io.FileInputStream;
import java.util.StringTokenizer;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 *
 * @author  ss141213
 * This as per J2EE 1.4 spec section#8.2. Contents from spec is given below...
 * Only Jar format files containing class files or resources to be loaded directly 
 * by a standard ClassLoader should be the target of a Class-Path reference; 
 * such files are always named with a .jar extension. 
 * Top level Jar files that are processed by a deployment tool 
 * should not contain Class-Path entries; 
 * such entries would, by definition, 
 * reference other files external to the deployment unit. 
 * A deployment tool is not required to process such external references.
  */
public class BundledOptPkgHasDependencies {
    public static void test(String explodedJarPath, Result result){
        try{
            boolean failed=false;
            Manifest manifest=new Manifest(new FileInputStream(new File(explodedJarPath+File.separator+JarFile.MANIFEST_NAME)));
            String depClassPath=manifest.getMainAttributes().getValue(Attributes.Name.CLASS_PATH);
            if(depClassPath!=null){
                for(StringTokenizer st=new StringTokenizer(depClassPath);st.hasMoreTokens();){
                    String entry=st.nextToken();
                    String entryPath=new File(explodedJarPath).getParent()+File.separator+entry;
                    File bundledOptPkg=new File(entryPath);
                    if(!bundledOptPkg.isDirectory()){
                        Manifest bundledManifest=new JarFile(bundledOptPkg).getManifest();
                        String bundledCP=bundledManifest.getMainAttributes().getValue(Attributes.Name.CLASS_PATH);
                        if(bundledCP!=null && bundledCP.length()!=0){
                            failed=true;
                            result.failed(entry + " contains Class-Path in it's manifest.");
                        }
                    }
                }
            }//if
            if(!failed){
                result.setStatus(Result.PASSED);
            }
        }catch(Exception e){
            result.failed(e.toString());
        }
    }
}
