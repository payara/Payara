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

package com.sun.enterprise.tools.verifier.tests.ejb.entity;

import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.tests.ComponentNameConstructor;
import com.sun.enterprise.tools.verifier.tests.ejb.EjbCheck;
import com.sun.enterprise.tools.verifier.tests.ejb.EjbTest;
import org.glassfish.ejb.deployment.descriptor.EjbBundleDescriptorImpl;
import org.glassfish.ejb.deployment.descriptor.EjbCMPEntityDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbEntityDescriptor;

import java.util.Iterator;
import java.util.Vector;

/** 
 * The abstract schema name for every CMP bean within a jar file should be unique.
 *
 * @author Sheetal Vartak
 *
 */
public class UniqueAbstractSchemaName extends EjbTest implements EjbCheck { 


    /** 
     * The abstract schema name for every CMP bean within a jar file should be unique.
     *
     *
     * @param descriptor the Enterprise Java Bean deployment descriptor
     *   
     * @return <code>Result</code> the results for this assertion
     */
    public Result check(EjbDescriptor descriptor) {

	Result result = getInitializedResult();
	ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();
	boolean oneFailed = false;
	String abstractSchema = null;

	if (descriptor instanceof EjbEntityDescriptor) {
	    if (((EjbEntityDescriptor)descriptor).getPersistenceType().equals(EjbEntityDescriptor.CONTAINER_PERSISTENCE)) {                
                
                if (((EjbCMPEntityDescriptor) descriptor).getCMPVersion()==EjbCMPEntityDescriptor.CMP_2_x) {
                    abstractSchema = ((EjbCMPEntityDescriptor)descriptor).getAbstractSchemaName();
                    if (abstractSchema==null) {
                        result.addErrorDetails(smh.getLocalString
					   ("tests.componentNameConstructor",
					    "For [ {0} ]",
					    new Object[] {compName.toString()}));
                        result.failed(smh.getLocalString
                                        (getClass().getName() + ".failed2",
                                        "No Abstract Schema Name specified for a CMP 2.0 Entity Bean {0} ",
                                        new Object[] {descriptor.getName()}));                          
                        return result;
                    }
                }
            }
            if (abstractSchema ==null) {
                result.addNaDetails(smh.getLocalString
                    ("tests.componentNameConstructor",
                    "For [ {0} ]",
                    new Object[] {compName.toString()}));
                result.notApplicable(smh.getLocalString
                    (getClass().getName() + ".notApplicable",
                    "This test is only for CMP 2.0 beans. Abstract Schema Names should be unique within an ejb JAR file."));
                    return result;
	    }

	    EjbBundleDescriptorImpl bundle = descriptor.getEjbBundleDescriptor();
	    Iterator iterator = (bundle.getEjbs()).iterator();
	    Vector<String> schemaNames = new Vector<String>();
	    while(iterator.hasNext()) {
		EjbDescriptor entity = (EjbDescriptor) iterator.next();
		if (entity instanceof EjbEntityDescriptor) { 
		    if (!entity.equals(descriptor)) {
			if (((EjbEntityDescriptor)entity).getPersistenceType().equals(EjbEntityDescriptor.CONTAINER_PERSISTENCE)) {
			    schemaNames.addElement(((EjbCMPEntityDescriptor)entity).getAbstractSchemaName());
			} 
		    }
		}
	    }

	    for (int i = 0; i < schemaNames.size(); i++) {
		if (abstractSchema.equals(schemaNames.elementAt(i))) {
		    result.addErrorDetails(smh.getLocalString
					   ("tests.componentNameConstructor",
					    "For [ {0} ]",
					    new Object[] {compName.toString()}));
		    result.addErrorDetails
			(smh.getLocalString
			 (getClass().getName() + ".failed",
			  "Abstract Schema Names should be unique within an ejb JAR file. Abstract Schema Name [ {0} ] is not unique.",
			  new Object[] {abstractSchema}));
		    oneFailed = true;
		}
	    }
	    if (oneFailed == false) {
		result.addGoodDetails(smh.getLocalString
				      ("tests.componentNameConstructor",
				       "For [ {0} ]",
				       new Object[] {compName.toString()}));
		result.passed
		(smh.getLocalString
		 (getClass().getName() + ".passed",
		  "PASSED : Abstract Schema Names for all beans within the ejb JAR file are unique."));
	    }
	    else result.setStatus(Result.FAILED);
	    
	} else {
        addNaDetails(result, compName);        
        result.notApplicable(smh.getLocalString
            (getClass().getName() + ".notApplicable",
            "This test is only for CMP 2.0 beans. Abstract Schema Names should be unique within an ejb JAR file."));
    }
    return result;
    }
}
