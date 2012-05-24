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

package com.sun.enterprise.tools.verifier.tests.ejb.entity.cmp2;

import com.sun.enterprise.deployment.MethodDescriptor;
import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.tests.ComponentNameConstructor;
import org.glassfish.ejb.deployment.descriptor.EjbCMPEntityDescriptor;
import org.glassfish.ejb.deployment.descriptor.QueryDescriptor;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Set;

/**
 * Select methods must be associated with an EJB QL query which includes a 
 * SELECT clause
 *
 * @author  Jerome Dochez
 * @version 
 */
public class SelectMethodQL extends SelectMethodTest {

    /**
     * <p>
     * run an individual test against a declared ejbSelect method
     * </p>
     * 
     * @param m is the ejbSelect method
     * @param descriptor is the entity declaring the ejbSelect
     * @param result is where to put the result
     * 
     * @return true if the test passes
     */
    protected boolean runIndividualSelectTest(Method m, EjbCMPEntityDescriptor descriptor, Result result) {
        ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();
        // We don't use getQueryFor to free ourselfves from classloader issues.
        Set set = descriptor.getPersistenceDescriptor().getQueriedMethods();
        Iterator iterator = set.iterator();
        while(iterator.hasNext()) {
            MethodDescriptor queryMethod = (MethodDescriptor) iterator.next();
            if (queryMethod.getName().equals(m.getName())) {
                Class mParms[] = m.getParameterTypes();
	
                String queryParms[] = queryMethod.getParameterClassNames();
	
		if (queryParms != null) {
	
                if (queryParms.length == mParms.length) {
                    boolean same = true;
                    for (int i=0;i<mParms.length;i++) {
                        if (!mParms[i].getName().equals(queryParms[i]))
                            same=false;                    
                    }
                    if (same) {
                        QueryDescriptor qd = descriptor.getPersistenceDescriptor().getQueryFor(queryMethod);
                        String query = qd.getQuery();
                        if (query == null && qd.getSQL()==null) {
			    result.addErrorDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
                            result.addErrorDetails(smh.getLocalString
                                ("com.sun.enterprise.tools.verifier.tests.ejb.entity.cmp2.SelectMethodQL.failed2",
                                "Error : [ {0} ] EJB-QL query and description are null",
		                new Object[] {m.getName()}));                                                    
                            return false;
                        } else {
                            if (query==null) {
				result.addGoodDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
                                result.addGoodDetails(smh.getLocalString
            		            ("com.sun.enterprise.tools.verifier.tests.ejb.entity.cmp2.SelectMethodQL.passed1",
                                    "Description for [ {0} ] is provided",
        		            new Object[] {m.getName()}));                                                       
                                return true;
                            }                                
                            if (query.toUpperCase().indexOf("SELECT")==-1) {
				result.addErrorDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
                                result.addErrorDetails(smh.getLocalString
                                    ("com.sun.enterprise.tools.verifier.tests.ejb.entity.cmp2.SelectMethodQL.failed2",
                                    "Error : EJB-QL query for method [ {0}  is null",
		                    new Object[] {m.getName()}));                                                    
                                return false;
                            } else {
				result.addGoodDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
                                result.addGoodDetails(smh.getLocalString
            		            ("com.sun.enterprise.tools.verifier.tests.ejb.entity.cmp2.SelectMethodQL.passed2",
                                    "EJB-QL query for [ {0} ] is correct",
        		            new Object[] {m.getName()}));                                                       
                                return true;
                            }
                        }                        
                    }
                }
		}
		else if (mParms.length == 0) {
	
		    result.addGoodDetails(smh.getLocalString
					  ("tests.componentNameConstructor",
					   "For [ {0} ]",
					   new Object[] {compName.toString()}));
		    result.addGoodDetails(smh.getLocalString
					  ("com.sun.enterprise.tools.verifier.tests.ejb.entity.cmp2.SelectMethodQL.passed3",
					   "No EJB-QL query found",
					   new Object[] {}));                                                       
		    return true;
		} else {
		    result.addErrorDetails(smh.getLocalString
					   ("tests.componentNameConstructor",
					    "For [ {0} ]",
					    new Object[] {compName.toString()}));
		    result.addErrorDetails(smh.getLocalString
					   ("com.sun.enterprise.tools.verifier.tests.ejb.entity.cmp2.SelectMethodQL.failed2",
					    "Error : EJB-QL query for method [ {0}  is null",
					    new Object[] {m.getName()}));                                                    
		    return false;
		}
            }	    	    
        }
	
	result.addErrorDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
        result.addErrorDetails(smh.getLocalString
	    ("com.sun.enterprise.tools.verifier.tests.ejb.entity.cmp2.SelectMethodQL.failed1",
            "Error : [ {0} ] does not have a XML query element associated",
	    new Object[] {m.getName()}));                                                    
        return false;            
    }
}
