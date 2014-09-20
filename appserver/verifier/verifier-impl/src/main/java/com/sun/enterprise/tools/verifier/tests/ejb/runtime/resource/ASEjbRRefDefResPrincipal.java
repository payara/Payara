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

import com.sun.enterprise.deployment.ResourcePrincipal;
import com.sun.enterprise.deployment.ResourceReferenceDescriptor;
import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.tests.ComponentNameConstructor;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;

/** ejb [0,n]
 *    resource-ref [0,n]
 *        res-ref-name [String]
 *        jndi-name [String]
 *        default-resource-principal ?
 *            name [String]
 *            password [String]
 *
 * The default-resource-principal specifies the principal for the
 * resource
 * The name and password should not be null.
 * The principal should be declared if the authorization type for the resource
 * in ejb-jar.xml is "Application"
 * @author Irfan Ahmed
 */

public class ASEjbRRefDefResPrincipal extends ASEjbResRef { 

    public Result check(EjbDescriptor descriptor)
    {
	Result result = getInitializedResult();
	ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();
        boolean oneFailed = false;
        try{
            Set resRef = descriptor.getResourceReferenceDescriptors();
            if(!(resRef.isEmpty())){
                Iterator it = resRef.iterator();
                while (it.hasNext()){
                    ResourceReferenceDescriptor resDesc = ((ResourceReferenceDescriptor)it.next());
                    String refName = resDesc.getName();
                    String refJndiName = resDesc.getJndiName();
                    ResourcePrincipal resPrinci = resDesc.getResourcePrincipal();
                   if(resPrinci == null)
                    {
                        try
                        {
                           resDesc = descriptor.getResourceReferenceByName(refName);
                            String resAuth = resDesc.getAuthorization();
                            if(resAuth.equals(ResourceReferenceDescriptor.APPLICATION_AUTHORIZATION))
                            {
                               addErrorDetails(result, compName);
                               result.failed(smh.getLocalString(getClass().getName()+".failed",
                                    "FAILED [AS-EJB resource-ref] : res-auth for res-ref-name {0} is defined as Application." + 
                                    "Therefore the default-resource-principal should be supplied with valid properties",
                                    new Object[] {refName}));
                            }
                            else
                            {
                               addNaDetails(result, compName);
                               result.notApplicable(smh.getLocalString(getClass().getName()+".notApplicable",
                                    "NOT APPLICABLE [AS-EJB resource-ref] : default-resource-principal Element not defined"));
                            }
                        }
                        catch(IllegalArgumentException iaex)
                        {
                           addErrorDetails(result, compName);
                           result.failed(smh.getLocalString(getClass().getName()+".failed2",
                                "FAILED [AS-EJB resource-ref] : res-ref-name {0} is not defined in the ejb-jar.xml",
                                new Object[]{refName}));
                        }
                    }else
                    {
                        String name = resPrinci.getName();
                        if(name == null || name.length()==0)
                        {
                            oneFailed = true;
                            addErrorDetails(result, compName);
                            result.failed(smh.getLocalString(getClass().getName()+".failed3",
                                "FAILED [AS-EJB default-resource-principal] :  name cannot be an empty string"));
                        }
                        else
                        {
                           addGoodDetails(result, compName);
                           result.passed(smh.getLocalString(getClass().getName()+".passed",
                                "PASSED [AS-EJB default-resource-principal] : name is {0}",new Object[]{name}));
                        }
                        
                        String password = resPrinci.getPassword();
                        if(password == null || password.length()==0)
                        {
                           addWarningDetails(result, compName);
                           result.warning(smh.getLocalString(getClass().getName()+".warning1",
                                "WARNING [AS-EJB default-resource-principal] : password is an empty string"));
                        }
                        else
                        {
                            addGoodDetails(result, compName);
                            result.passed(smh.getLocalString(getClass().getName()+".passed1",
                                "PASSED [AS-EJB default-resource-principal] : password is  {0}",new Object[]{password}));
                        }
                        
                        if(oneFailed)
                            result.setStatus(Result.FAILED);
                    }
                
                }
            }
            else
            {
                addNaDetails(result, compName);
                result.notApplicable(smh.getLocalString
                         (getClass().getName() + ".notApplicable",
                          "{0} Does not define any resource-ref Elements"));
            }
        }catch(Exception ex)
        {
            addErrorDetails(result, compName);
            result.addErrorDetails(smh.getLocalString
                 (getClass().getName() + ".notRun",
                  "NOT RUN [AS-EJB] : Could not create the descriptor object"));        
        }
        return result;
    }
}
