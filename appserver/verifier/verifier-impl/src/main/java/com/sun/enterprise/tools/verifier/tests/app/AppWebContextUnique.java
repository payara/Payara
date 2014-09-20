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

package com.sun.enterprise.tools.verifier.tests.app;

import com.sun.enterprise.tools.verifier.tests.app.ApplicationTest;
import java.util.*;
import com.sun.enterprise.deployment.*;
import com.sun.enterprise.tools.verifier.*;

/** 
 * All web modules in the application must have unique context-root. 
 */

public class AppWebContextUnique extends ApplicationTest implements AppCheck { 


    /** 
     * All web modules in the application must have unique context-root.
     * Applicable for j2ee 1.3 or below. For 1.4 and above xml schema takes care of this.
     * @param descriptor the Application deployment descriptor
     *   
     * @return <code>Result</code> the results for this assertion
     */
    public Result check(Application descriptor) {

	Result result = getInitializedResult();
        Set webs=descriptor.getBundleDescriptors(WebBundleDescriptor.class);
        if(webs.size()<=1){
            result.notApplicable(smh.getLocalString
				 (getClass().getName() + ".notApplicable",
				  "There is one or less web component in application [ {0} ]",
				  new Object[] {descriptor.getName()}));        
            return result;
        }
        Set<String> contexts=new HashSet<String>();
        Iterator itr=webs.iterator();
        boolean oneFailed=false;
	while (itr.hasNext()) {
            WebBundleDescriptor wbd = (WebBundleDescriptor) itr.next();
            String ctx=wbd.getContextRoot();
            if(!contexts.add(ctx)){
                oneFailed=true;
                result.failed(
                    (smh.getLocalString
                     (getClass().getName() + ".failed",
                      "Error: There is already a web module with context-root [ {0} ] within application [ {1} ]",
                      new Object[] {ctx, descriptor.getName()})));
            }
        }
	if(!oneFailed){
            result.passed(
                (smh.getLocalString
                 (getClass().getName() + ".passed",
                  "All the context-root values are unique within application [ {0} ]",
                  new Object[] {descriptor.getName()})));
        }
	return result;
    }
}
