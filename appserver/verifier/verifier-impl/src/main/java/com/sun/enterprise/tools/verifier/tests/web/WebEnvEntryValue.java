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

package com.sun.enterprise.tools.verifier.tests.web;

import com.sun.enterprise.tools.verifier.tests.web.WebTest;
import java.util.*;
import com.sun.enterprise.deployment.*;
import com.sun.enterprise.tools.verifier.*;
import com.sun.enterprise.tools.verifier.tests.*;

/** 
 * If the Bean Provider provides a value for an environment entry using the 
 * env-entry-value element, the value can be changed later by the Application 
 * Assembler or Deployer. The value must be a string that is valid for the 
 * constructor of the specified type that takes a single String parameter.
 */
public class WebEnvEntryValue extends WebTest implements WebCheck { 


    /** 
     * If the Bean Provider provides a value for an environment entry using the 
     * env-entry-value element, the value can be changed later by the Application 
     * Assembler or Deployer. The value must be a string that is valid for the 
     * constructor of the specified type that takes a single String parameter.
     *
     * @param descriptor the Web deployment descriptor
     *   
     * @return <code>Result</code> the results for this assertion
     */
    public Result check(WebBundleDescriptor descriptor) {

	Result result = getInitializedResult();
	ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();

	boolean oneFailed = false;
        boolean globalFailed = false;
	if (!descriptor.getEnvironmentProperties().isEmpty()) {
            int oneEnvValue = 0;
            int oneNA = 0;
	    // The value must be a string that is valid for the
	    // constructor of the specified type that takes a single String parameter
	    for (Iterator itr2 = descriptor.getEnvironmentProperties().iterator(); 
		 itr2.hasNext();) {
                oneEnvValue++;
		EnvironmentProperty nextEnvironmentProperty = 
		    (EnvironmentProperty) itr2.next();
	        if ((nextEnvironmentProperty.getValue() != null) && (nextEnvironmentProperty.getValue().length() > 0)) {
		    if (nextEnvironmentProperty.getType().equals("java.lang.String"))  {
			// don't need to do anything in this case, since any string results
			// in a valid object creation
			try {
			    new String(nextEnvironmentProperty.getValue());
			} catch (Exception e) {
			    if (debug) {
				e.printStackTrace();
			    }
			    oneFailed = true;
			}
		    } else  if (nextEnvironmentProperty.getType().equals("java.lang.Character"))  {
			try {
			    if (nextEnvironmentProperty.getValue().length() == 1) {
				char c = (nextEnvironmentProperty.getValue()).charAt(0);
				new Character(c);
			    }
			    else oneFailed = true;
			} catch (Exception e) {
			    if (debug) {
				e.printStackTrace();
			    }
			    oneFailed = true;
			}
		    }else if (nextEnvironmentProperty.getType().equals("java.lang.Integer")) {
			try {
			    new Integer(nextEnvironmentProperty.getValue());
			} catch (NumberFormatException e) {
			    if (debug) {
				e.printStackTrace();
			    }
			    oneFailed = true;
			}
		    } else if  (nextEnvironmentProperty.getType().equals("java.lang.Boolean")) {
			// don't need to do anything in this case, since any string results
			// in a valid object creation
			try {
			    new Boolean(nextEnvironmentProperty.getValue());
			} catch (Exception e) {
			    if (debug) {
				e.printStackTrace();
			    }
			    oneFailed = true;
			}
		    } else if  (nextEnvironmentProperty.getType().equals("java.lang.Double")) {
			try {
			    new Double(nextEnvironmentProperty.getValue());
			} catch (NumberFormatException e) {
			    if (debug) {
				e.printStackTrace();
			    }
			    oneFailed = true;
			}
		    } else if  (nextEnvironmentProperty.getType().equals("java.lang.Byte")) {
			try {
			    new Byte(nextEnvironmentProperty.getValue());
			} catch (NumberFormatException e) {
			    if (debug) {
				e.printStackTrace();
			    }
			    oneFailed = true;
			}
		    } else if  (nextEnvironmentProperty.getType().equals("java.lang.Short")) {
			try {
			    new Short(nextEnvironmentProperty.getValue());
			} catch (NumberFormatException e) {
			    if (debug) {
				e.printStackTrace();
			    }
			    oneFailed = true;
			}
		    } else if  (nextEnvironmentProperty.getType().equals("java.lang.Long")) {
			try {
			    new Long(nextEnvironmentProperty.getValue());
			} catch (NumberFormatException e) {
			    if (debug) {
				e.printStackTrace();
			    }
			    oneFailed = true;
			}
		    } else if  (nextEnvironmentProperty.getType().equals("java.lang.Float")) {
			try {
			    new Float(nextEnvironmentProperty.getValue());
			} catch (NumberFormatException e) {
			    if (debug) {
				e.printStackTrace();
			    }
			    oneFailed = true;
			}
		    } else {
			oneFailed = true;
		    }
		    if (oneFailed) {
			result.addErrorDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
			result.addErrorDetails
			    (smh.getLocalString
			     (getClass().getName() + ".failed",
			      "Error: Environment entry value [ {0} ] does not have valid value [ {1} ] for constructor of the specified type [ {2} ] that takes a single String parameter within web archive [ {3} ]",
			      new Object[] {nextEnvironmentProperty.getName(),nextEnvironmentProperty.getValue(),nextEnvironmentProperty.getType(),descriptor.getName()}));
                        globalFailed = true;
			oneFailed = false;
		    } else {
			result.addGoodDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
			result.addGoodDetails
			    (smh.getLocalString
			     (getClass().getName() + ".passed",
			      "Environment entry value [ {0} ] has valid value [ {1} ] for constructor of the specified type [ {2} ] that takes a single String parameter within web archive [ {3} ]",
			      new Object[] {nextEnvironmentProperty.getName(),nextEnvironmentProperty.getValue(),nextEnvironmentProperty.getType(),descriptor.getName()}));
		    } 
		} else {
		    // maybe nextEnvironmentProperty.getValue is null 'cause we 
		    // are not using nextEnvironmentProperty.getValue
		    // if that is the case, then test is N/A,
		    result.addNaDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
		    result.addNaDetails(smh.getLocalString
					(getClass().getName() + ".notApplicable1",
					 "Environment entry [ {0} ] initial value is not defined within web application [ {1} ]",
					 new Object[] {nextEnvironmentProperty.getName(), descriptor.getName()}));
		    oneNA++;
		}
	    }
	    if (globalFailed){
		result.setStatus(Result.FAILED);
            } else if (oneNA == oneEnvValue) {
                result.setStatus(Result.NOT_APPLICABLE);
	    } else {
		result.setStatus(Result.PASSED);
	    }
	} else {
	    result.addNaDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
	    result.notApplicable(smh.getLocalString
				 (getClass().getName() + ".notApplicable",
				  "There are no environment entry elements defined within this web archive [ {0} ]",
				  new Object[] {descriptor.getName()}));
	}

	return result;
    }
}
