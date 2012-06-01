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

package com.sun.enterprise.tools.verifier.tests.ejb.runtime;

import com.sun.enterprise.deployment.EjbReferenceDescriptor;
import com.sun.enterprise.deployment.EjbSessionDescriptor;
import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.tests.ComponentNameConstructor;
import com.sun.enterprise.tools.verifier.tests.ejb.EjbCheck;
import com.sun.enterprise.tools.verifier.tests.ejb.EjbTest;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbEntityDescriptor;

import java.util.Iterator;
import java.util.Set;

/** ejb [0,n]
 *    ejb-ref [0,n]
 *        ejb-ref-name [String]
 *        jndi-name [String]
 *
 * The ejb-ref is root element that binds and ejb reference to a jndi-name.
 * The ejb-ref-name should have an entry in the ejb-jar.xml
 * The jdi-name should not be empty. It shoudl start with ejb/
 * @author
 */

public class ASEjbRef extends EjbTest implements EjbCheck {
    
   /**
     * @param descriptor the Enterprise Java Bean deployment descriptor
     * @return <code>Result</code> the results for this assertion
     */
    public Result check(EjbDescriptor descriptor) {
        Result result = getInitializedResult();
        ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();
        String ejbName = null, jndiName=null;
        boolean oneFailed = false;
        boolean notApplicable = false;
        boolean oneWarning = false;

        try{
            ejbName = descriptor.getName();
            Set ejbRefs = descriptor.getEjbReferenceDescriptors();
            if (ejbRefs.size()>0){
                Iterator it = ejbRefs.iterator();
                while(it.hasNext()){
                    EjbReferenceDescriptor desc = ((EjbReferenceDescriptor)it.next());
                    String refJndiName=getXPathValue("/sun-ejb-jar/enterprise-beans/ejb/ejb-ref[ejb-ref-name=\""+desc.getName()+"\"]/jndi-name");
                    String refName = desc.getName();
                    String type = desc.getType();
                    if(!desc.isLocal()){
                        if (type == null || !( (type.equals(EjbSessionDescriptor.TYPE) || type.equals(EjbEntityDescriptor.TYPE))) ){
                            oneFailed = true;
                            addErrorDetails(result, compName);
                            result.failed(smh.getLocalString(getClass().getName() + ".failed1",
                                    "FAILED [AS-EJB ejb-ref] ejb-ref-name has an invalid type in ejb-jar.xml." +
                                    " Type should be Session or Entity only"));
                        }else{
                            addGoodDetails(result, compName);
                            result.passed(smh.getLocalString(getClass().getName() + ".passed2",
                                    "PASSED [AS-EJB ejb-ref] ejb-ref-name [{0}] is valid",
                                    new Object[]{refName}));
                        }
                    }else{
                        addNaDetails(result, compName);
                        result.notApplicable(smh.getLocalString
                                (getClass().getName() + ".notApplicable",
                                        "{0} Does not define any ejb references",
                                        new Object[] {ejbName}));
                        return result;
                    }
                    
                    if (refJndiName != null){
                        if(refJndiName.length()==0){
                            oneFailed = true;
                            addErrorDetails(result, compName);
                            result.addErrorDetails(smh.getLocalString
                                (getClass().getName() + ".failed2",
                                "FAILED [AS-EJB ejb-ref] : jndi-name cannot be an empty string",
                                new Object[] {refName}));
                        }else{
                            if (!refJndiName.startsWith("ejb/")){
                                oneWarning = true;
                                addWarningDetails(result, compName);
                                result.warning(smh.getLocalString
                                    (getClass().getName() + ".warning",
                                    "WARNING [AS-EJB ejb-ref] JNDI name should start with ejb/ for an ejb reference",
                                    new Object[] {refName}));
                            }
                        }
                    }else {
                        oneFailed = true;
                        addErrorDetails(result, compName);
                        result.addErrorDetails(smh.getLocalString
                            (getClass().getName() + ".failed2",
                            "FAILED [AS-EJB ejb-ref] : jndi-name cannot be an empty string",
                            new Object[] {refName}));
                    }
                    
                    if (!oneFailed){
                        addGoodDetails(result, compName);
                        result.addGoodDetails(smh.getLocalString(
                            getClass().getName() + ".passed1",
                            "PASSED [AS-EJB ejb-ref] : ejb-ref-Name is {0} and jndi-name is {1}",
                            new Object[] {refName,refJndiName}));
                    }
                }
                
            }else{
                addNaDetails(result, compName);
                result.notApplicable(smh.getLocalString
                    (getClass().getName() + ".notApplicable",
                    "{0} Does not define any ejb references",
                    new Object[] {ejbName}));
                return result;
            }
        }catch(Exception ex){
            oneFailed = true;
            addErrorDetails(result, compName);
            result.addErrorDetails(smh.getLocalString
                (getClass().getName() + ".notRun",
                "NOT RUN [AS-EJB] : Could not create descriptor object"));
                return result;
        }
        
	if (oneFailed) 
        {
	    result.setStatus(Result.FAILED);
        }
        else if(oneWarning)
        {
            result.setStatus(Result.WARNING);
        }   
        else
        {
        addErrorDetails(result, compName);
	    result.passed
		(smh.getLocalString
		 (getClass().getName() + ".passed",
		  "PASSED [AS-EJB] :  {0} ejb refernce is verified",
		  new Object[] {ejbName, jndiName}));
	}
        return result;
        
    }
}
