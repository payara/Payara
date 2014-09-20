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
 * OptPkgDependencySatisfied.java
 *
 * Created on August 30, 2004, 9:39 AM
 */

package com.sun.enterprise.tools.verifier.tests.app;

import com.sun.enterprise.deployment.Application;
import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.apiscan.packaging.ExtensionRef;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

/**
 *
 * @author  ss141213
 * This checks to see that all the installed optional packages that this ear depends on are indeed
 * available in the system or not. 
 */
public class OptPkgDependencySatisfied  extends ApplicationTest implements AppCheck {
    public Result check(Application descriptor){
        Result result = getInitializedResult();
        try{
            String earURI=getAbstractArchiveUri(descriptor);
            com.sun.enterprise.tools.verifier.apiscan.packaging.Archive jar=new com.sun.enterprise.tools.verifier.apiscan.packaging.Archive(new File(earURI));
            //See we do not care about bundled opt packages, as for an ear file
            //there should not be any Class-Path entry.
            com.sun.enterprise.tools.verifier.apiscan.packaging.ExtensionRef[] extRefs=jar.getExtensionRefs();
            com.sun.enterprise.tools.verifier.apiscan.packaging.Archive[] allOptPkgs=com.sun.enterprise.tools.verifier.apiscan.packaging.Archive.getAllOptPkgsInstalledInJRE();
            ArrayList<ExtensionRef> notFounds=new ArrayList<ExtensionRef>();
            for(int i=0;i<extRefs.length;++i){
                ExtensionRef ref=extRefs[i];
                boolean found=false;
                for(int j=0;j<allOptPkgs.length;++j){
                    if(ref.isSatisfiedBy(allOptPkgs[j])) {
                        found=true;
                        break;
                    }
                }
                if(!found) notFounds.add(ref);
            }//for

            if(notFounds.isEmpty()){
                result.passed(smh.getLocalString(getClass().getName() + ".passed",
                                             "All opt package dependency satisfied for this ear file."));
                result.passed("");
            }else{
                result.failed(smh.getLocalString(getClass().getName() + ".failed","Some dependencies could not be satisfied for this ear file. See info below..."));
                for(Iterator i=notFounds.iterator();i.hasNext();){
                    result.addErrorDetails(i.next().toString());
                }
            }
        }catch(IOException e){
            result.failed(e.toString());
        }
        return result;
    }
}
