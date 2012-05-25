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

package com.sun.enterprise.tools.verifier.tests.ejb.runtime.resource;

import java.util.Iterator;
import java.util.Set;

import com.sun.enterprise.deployment.ResourceReferenceDescriptor;
import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.tests.ComponentNameConstructor;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;

/** ejb [0,n]
 *   resource-ref [0,n]
 *       res-ref-name [String]
 *       jndi-name [String]
 *       default-resource-principal ?
 *           name [String]
 *           password [String]
 *
 * The jndi-name specifies the JNDI name to which this resource is binded
 * The jndi-name should not be null.
 * The jndi-name should map to the correct subcontext and hence start with the
 * valid subcontext
 *    URL url/
 *    Mail mail/
 *    JDBC jdbc/
 *    JMS jms/
 *
 * @author Irfan Ahmed
 */

public class ASEjbRRefJndiName extends ASEjbResRef { 

    public Result check(EjbDescriptor descriptor)
    {
	Result result = getInitializedResult();
	ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();
        boolean oneFailed = false;
        //boolean oneWarning = false;
   
        try{
        Set resRef = descriptor.getResourceReferenceDescriptors();
        if(!(resRef.isEmpty()))
        {
            Iterator it = resRef.iterator();
            while (it.hasNext())
            {
                ResourceReferenceDescriptor resDesc = ((ResourceReferenceDescriptor)it.next());
                String refName = resDesc.getName();
                String refJndiName = resDesc.getJndiName();
                String type = resDesc.getType();
                
                if(refJndiName == null || refJndiName.trim().equals(""))
                {
                    oneFailed = true;
                    result.failed(smh.getLocalString(getClass().getName()+".failed",
                            "FAILED [AS-EJB resource-ref]: jndi-name is not a non empty string"));
                }
                    /* else  //Fix for bug id 5018617
                    {
                        if(type.indexOf("javax.jms")>-1) //jms resource
                        {
                            if(refJndiName.startsWith("jms/")) {
                                addGoodDetails(result, compName);
                                result.passed(smh.getLocalString(getClass().getName()+".passed1",
                                    "PASSED [AS-EJB resource-ref] : jndi-name {0} is valid", new Object[]{refJndiName}));
                            }
                            else
                            {
                                oneWarning = true;
                                addWarningDetails(result, compName);
                                result.warning(smh.getLocalString(getClass().getName()+".warning1",
                                    "WARNING [AS-EJB resource-ref] : jndi-name is \"{0}\" for resource type \"{1}\"." + 
                                    "The preferred jndi-name for JMS resources should start with jms/",
                                    new Object[]{refJndiName,type}));
                            }
                        }
                        else if(type.indexOf("javax.sql")>-1) //jdbc resource
                        {
                            if(refJndiName.startsWith("jdbc/")) {
                                addGoodDetails(result, compName);
                                result.passed(smh.getLocalString(getClass().getName()+".passed1",
                                    "PASSED [AS-EJB resource-ref] : jndi-name {0} is valid", new Object[]{refJndiName}));
                            }
                            else
                            {
                                oneWarning = true;
                                addWarningDetails(result, compName);
                                result.warning(smh.getLocalString(getClass().getName()+".warning2",
                                    "WARNING [AS-EJB resource-ref] : jndi-name is \"{0}\" for resource type \"{1}\"." + 
                                    "The preferred jndi-name for JDBC resources should start with jdbc/",
                                    new Object[]{refJndiName,type}));
                            }
                        }
                        else if(type.indexOf("java.net")>-1) //url resource
                        {
                            if(refJndiName.startsWith("http://"))//FIX should it start with http:// or url/http://
                            {
                                addGoodDetails(result, compName);
                                result.passed(smh.getLocalString(getClass().getName()+".passed1",
                                    "PASSED [AS-EJB resource-ref] : jndi-name {0} is valid", new Object[]{refJndiName}));
                            }
                            else
                            {
                                oneWarning = true;
                                addWarningDetails(result, compName);
                                result.warning(smh.getLocalString(getClass().getName()+".warning3",
                                    "WARNING [AS-EJB resource-ref] : jndi-name is \"{0}\" for resource type \"{1}\". " + 
                                    "The preferred jndi-name for URL resources should start with a url",
                                    new Object[]{refJndiName,type}));
                            }
                        }
                        else if(type.indexOf("javax.mail")>-1) //jms resource
                        {
                            if(refJndiName.startsWith("mail/")) {
                                addGoodDetails(result, compName);
                                result.passed(smh.getLocalString(getClass().getName()+".passed1",
                                    "PASSED [AS-EJB resource-ref] : jndi-name {0} is valid", new Object[]{refJndiName}));
                            }
                            else
                            {
                                oneWarning = true;
                                addWarningDetails(result, compName);
                                result.warning(smh.getLocalString(getClass().getName()+".warning4",
                                    "WARNING [AS-EJB resource-ref] : jndi-name is \"{0}\" for resource type \"{1}\"." + 
                                    "The preferred jndi-name for MAIL resources should start with mail/",
                                    new Object[]{refJndiName,type}));
                            }
                        }
                        else
                        {
                            addGoodDetails(result, compName);
                            result.passed(smh.getLocalString(getClass().getName()+".passed1","PASSED [AS-EJB resource-ref]: jndi-name {0} is valid",new Object[]{refJndiName}));
                        }
                    }*/
            }
        }
        else
        {
            addNaDetails(result, compName);
            result.notApplicable(smh.getLocalString
                (getClass().getName() + ".notApplicable",
                "NOT APPLICABLE [AS-EJB] : {0} Does not define any resource-ref Elements",
                new Object[] {descriptor.getName()}));
            return result;
        }
        }catch(Exception ex)
        {
            oneFailed = true;
            addErrorDetails(result, compName);
                result.addErrorDetails(smh.getLocalString
                 (getClass().getName() + ".notRun",
                  "NOT RUN [AS-EJB] : Could not create the descriptor object"));
            return result;
        }
        /*if(oneWarning)
            result.setStatus(Result.WARNING);*/
		if(oneFailed)
            result.setStatus(Result.FAILED);
        else {
            addGoodDetails(result, compName);
            result.passed(smh.getLocalString(getClass().getName() + ".passed",
                    "PASSED [AS-EJB resource-ref]: jndi name is specified correctly for the resource-references with in the application",
                    new Object[]{}));
        }
        return result;
    }
}
