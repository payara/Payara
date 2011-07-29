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

package com.sun.enterprise.tools.verifier.tests.appclient.elements;

import com.sun.enterprise.tools.verifier.tests.appclient.AppClientTest;
import com.sun.enterprise.tools.verifier.tests.appclient.AppClientCheck;
import java.util.*;
import java.util.logging.Level;

import com.sun.enterprise.deployment.*;
import com.sun.enterprise.tools.verifier.*;
import com.sun.enterprise.tools.verifier.tests.*;

/** 
 * The value of the application client ejb-link element must be the ejb-name 
 * of an enterprise bean in the same ejb .ear file.
 */
public class AppClientEjbLinkElement extends AppClientTest implements AppClientCheck { 

    /**
     * The value of the application client ejb-link element must be the ejb-name 
     * of an enterprise bean in the same ejb-jar file.
     *
     * @param descriptor the Application client deployment descriptor
     *   
     * @return <code>Result</code> the results for this assertion
     */
    public Result check(ApplicationClientDescriptor descriptor) {

	Result result = getInitializedResult();
	ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();

	boolean resolved = false;
	boolean oneFailed = false;
	int na = 0;
/*
        if (Verifier.getEarFile() == null) {
            // this appclient is not part of an .ear file
	    result.addNaDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
            result.notApplicable
                (smh.getLocalString
                    (getClass().getName() + ".no_ear",
                    "This Application Client [ {0} ] is not part of a J2EE Archive.",
		     new Object[] {descriptor.getName()}));
            return result;
        }
*/
//        String applicationName = null;
	// The value of the ejb-link element must be the ejb-name of an enterprise
	// bean in the same ejb-jar file.
	if (!descriptor.getEjbReferenceDescriptors().isEmpty()) {
	    for (Iterator itr = descriptor.getEjbReferenceDescriptors().iterator(); 
		 itr.hasNext();) {                                              
		EjbReferenceDescriptor nextEjbReference = (EjbReferenceDescriptor) itr.next();
		if (nextEjbReference.isLinked()) {
		    String ejb_link = nextEjbReference.getLinkName();
		    ejb_link = ejb_link.substring(ejb_link.indexOf("#") + 1);
		    // get the application descriptor and check all ejb-jars in the application
		    try {
//                        File tmpFile = new File(System.getProperty("java.io.tmpdir"));
//			tmpFile = new File(tmpFile, Verifier.TMPFILENAME + ".tmp");
  
			// iterate through the ejb jars in this J2EE Application
			Set ejbBundles = descriptor.getApplication().getBundleDescriptors(EjbBundleDescriptor.class);
			Iterator ejbBundlesIterator = ejbBundles.iterator();
			EjbBundleDescriptor ejbBundle = null;

			while (ejbBundlesIterator.hasNext()) {
			    ejbBundle = (EjbBundleDescriptor)ejbBundlesIterator.next();
			// Kumar: this code here seems like dead code to me, because tmpFile is
			// not being used. and extractBundleToFile, does not modify ejbBundle	
			//((Application)application).getApplicationArchivist().
			//	extractBundleToFile(ejbBundle, tmpFile);
        
			    for (Iterator itr2 = ejbBundle.getEjbs().iterator(); itr2.hasNext();) {
				EjbDescriptor ejbDescriptor = (EjbDescriptor) itr2.next();
				if (ejbDescriptor.getName().equals(ejb_link)) {
				    resolved = true;
				    result.addGoodDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
				    result.addGoodDetails
					(smh.getLocalString
					 (getClass().getName() + ".passed",
					  "EJB reference [ {0} ] is successfully resolved.",
					  new Object[] {nextEjbReference.getLinkName()}));
				    break;
				}
			    }
			}
		    }catch (Exception e) {
			    logger.log(Level.FINE, "com.sun.enterprise.tools.verifier.testsprint",
                        new Object[] {"[" + getClass() + "] Error: " + e.getMessage()});
		        if (!oneFailed) {
			    oneFailed = true;
                        }
                    }

		    // before you go onto the next ejb-link, tell me whether you
		    // resolved the last ejb-link okay
		    if (!resolved) {
		        if (!oneFailed) {
			    oneFailed = true;
                        }
			result.addErrorDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
			result.addErrorDetails(smh.getLocalString
					       (getClass().getName() + ".failed",
						"Error: Failed to resolve EJB reference [ {0} ].",
						new Object[] {nextEjbReference.getLinkName()}));
		    } else {
			// clear the resolved flag for the next ejb-link 
			resolved =false;
		    }

		} else {
		    // Cannot get the link name of an ejb reference referring 
		    // to an external bean, The value of the ejb-link element 
		    // must be the ejb-name of an enterprise bean in the same 
		    // ejb-ear file.
		    result.addNaDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
		    result.addNaDetails
			(smh.getLocalString
			 (getClass().getName() + ".notApplicable1",
			  "Not Applicable:  Cannot verify the existence of an ejb reference [ {0} ] to external bean within different .ear file.",
			  new Object[] {nextEjbReference.getName()}));
		    na++;
		}
	    }

	    if (oneFailed) {
		result.setStatus(result.FAILED);
	    } else if (na == descriptor.getEjbReferenceDescriptors().size()) {
		result.setStatus(result.NOT_APPLICABLE);
	    } else {
		result.setStatus(result.PASSED);
	    }
//            File tmpFile = new File(System.getProperty("java.io.tmpdir"));
//    	    tmpFile = new File(tmpFile, Verifier.TMPFILENAME + ".tmp");
//            tmpFile.delete();
	    return result;

	} else {
	    result.addNaDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
	    result.notApplicable(smh.getLocalString
				 (getClass().getName() + ".notApplicable",
				  "There are no ejb references to other beans within this application client [ {0} ]",  
				  new Object[] {descriptor.getName()}));
	}

	return result;
    }
/*
    Application getApplication(File file) throws ArchiveException {

      try {
        // There is an ApplicationArchivist lying in appserver-core/com/sun/enterprise/deployment
        com.sun.enterprise.deployment.archivist.Archivist  arch = 
        ArchivistFactory.getArchivistForArchive(file);
        return (Application)(arch.open(file));
      }
      catch(Exception e) {
        throw new ArchiveException(e.getMessage());
      }
    }
 */
    
}
