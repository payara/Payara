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

package com.sun.enterprise.tools.verifier.tests.web.elements;

import com.sun.enterprise.tools.verifier.tests.web.WebTest;
import com.sun.enterprise.tools.verifier.tests.web.WebCheck;
import java.util.*;
import com.sun.enterprise.deployment.*;
import com.sun.enterprise.tools.verifier.*;
import com.sun.enterprise.tools.verifier.tests.*;
import com.sun.enterprise.tools.verifier.tests.web.*;

/** 
 * The Bean Provider must declare all enterprise bean's references to the
 * homes of other enterprise beans as specified in section 14.3.2 of the 
 * Moscone spec.  Check for one within the same jar file, can't check 
 * outside of jar file.  Load/locate & check other bean's home/remote/bean,
 * ensure they match with what the linking bean says they should be; check
 * for pair of referencing and referenced beans exist.
 */
public class WebReferencesInWEBINF extends WebTest implements WebCheck { 
    Result result = null;
    ComponentNameConstructor compName = null;
    EjbReferenceDescriptor ejbReference;
    Iterator iterator;
  
    /** 
     * The referenced Ejb bean/home/component interfaces should exist inside the WEB-INF/classes dir 
     * or the WEB-INF/lib/*.jar. These classes should be loadable by the war file's class loader.
     *
     * @param descriptor the Web deployment descriptor
     *
     * @return <code>Result</code> the results for this assertion
     */
    public Result check(WebBundleDescriptor descriptor) {

        result = getInitializedResult();
	boolean oneFailed = false;
	compName = getVerifierContext().getComponentNameConstructor();

	if (descriptor instanceof WebBundleDescriptor) {

	    /*  
		The referenced Ejb home/component interfaces should exist inside the WEB-INF/classes dir 
		or the WEB-INF/lib/*.jar. These classes should be loadable by the war file's class loader.
	    */
   	   
	    String f = descriptor.getModuleDescriptor().getArchiveUri();
	    Set references = descriptor.getEjbReferenceDescriptors();
	    iterator = references.iterator();
	    result = loadWarFile(descriptor);

	    try {
		if (iterator.hasNext()) {
		    while (iterator.hasNext()) {
			ejbReference = (EjbReferenceDescriptor) iterator.next();
			if (ejbReference.isLinked()) { // instanceof EjbDescriptor) {
			    oneFailed = findEjbRef(f);
			} else {
			    // (e.g. external references)
			    result.addGoodDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
			    result.addGoodDetails(smh.getLocalString
						  (getClass().getName() + ".notApplicable2",
						   "Not Applicable: [ {0} ] must be external reference to bean outside of [ {1} ].",
						   new Object[] {ejbReference.getName(),f}));
			}
		    }
		}
		else {
		    result.addNaDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
		    result.notApplicable(smh.getLocalString
					 (getClass().getName() + ".notApplicable",
					  "Not Applicable: No EJB references found in [ {0} ]",
					  new Object[] {f}));
		    return result;
		}
	    } catch (Exception e) {
		result.addErrorDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
		result.failed(smh.getLocalString
			      (getClass().getName() + ".IOException", 
			       "I/O error trying to open {0}", new Object[] {f}));
	    } finally {
		try {
		} catch (Exception x) {}
	    }
	    if (oneFailed) {
		result.setStatus(Result.FAILED);
	    } else {
		result.setStatus(Result.PASSED);
	    }
	} else {
	    result.addNaDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
	    result.notApplicable(smh.getLocalString
				 (getClass().getName() + ".notApplicable1",
				  "There are no ejb references to other beans within this web archive [ {0} ]",
				  new Object[] {descriptor.getName()}));
	}
	
	return result;
    }
    
    private boolean findEjbRef(String f) {
	
	boolean oneFailed = false;
	
	try {
	  if(ejbReference.getEjbHomeInterface() != null && 
	       !"".equals(ejbReference.getEjbHomeInterface()) &&
	       ejbReference.getEjbInterface() != null && 
	       !"".equals(ejbReference.getEjbInterface())) {
		oneFailed = common(ejbReference.getEjbHomeInterface(),f);
		    if (oneFailed == true) {
			result.addErrorDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
			result.failed(smh.getLocalString
				      (getClass().getName() + ".failed",
				       "Error: The [ {0} ] interface of the referenced EJB [ {1} ] is not packaged inside the WEB-INF directory of the war file.",
				       new Object[] {ejbReference.getEjbHomeInterface(),ejbReference.getName()}));
			return true;
		    }
		    
		    oneFailed = common(ejbReference.getEjbInterface(),f);
		    if (oneFailed == true) {
			result.addErrorDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
			result.failed(smh.getLocalString
				      (getClass().getName() + ".failed",
				       "Error: The [ {0} ] interface of the referenced EJB [ {1} ] is not packaged inside the WEB-INF directory of the war file",
				       new Object[] {ejbReference.getEjbInterface(),ejbReference.getName()}));
			return true;
		    }
	    } 
	    return oneFailed;
	} catch (NoClassDefFoundError e) {
	    result.addErrorDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
	    result.failed(smh.getLocalString
			  (getClass().getName() + ".failed1",
			   "Error: Bean class [ {0} ] does not exist or is not loadable:NoClassDefFoundError.",
			   new Object[] {ejbReference.getName()}));
	    oneFailed = true;
	    return oneFailed;
	} catch (Exception e) {
	    Verifier.debug(e);
	    result.addErrorDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
	    result.failed(smh.getLocalString
			  (getClass().getName() + ".failed1",
			   "Error: Bean class [ {0} ] does not exist or is not loadable:ClassNotFoundException",
			   new Object[] {ejbReference.getName()}));
	    oneFailed = true;
	    return oneFailed;
	}	
    }
    
    private boolean common(String intf, String f) {
	Class cl = loadClass(result, intf);
	if (cl != null) {
	    result.addGoodDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));	
	    result.passed(smh.getLocalString
			  (getClass().getName() + ".passed",
			   "The referenced beans home/component [ {0} ] interface exists and is loadable within [ {1} ].",
			   new Object[] {intf,f}));
	    return false;
	} 
	else return true;
    }
}    


