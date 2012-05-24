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

import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.tests.ejb.EjbCheck;
import com.sun.enterprise.tools.verifier.tests.ejb.EjbTest;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;

/** ias-ejb-jar
 *    security-role-mapping [0,n]
 *        role-name [String]
 *        principal-name [String] | group-name [String]
 *
 * The element defines the security role mappings for the bean.
 * The role-name should not be an empty string
 * The role-name should be desclared in the assembly-descriptor in the ejb-jar.xml
 * file.
 * The principal-name and group-name should not be empty strings.
 *
 * @author Irfan Ahmed
 */
public class ASSecurityRoleMapping extends EjbTest implements EjbCheck { 
//hs NO API for security-role-mapping element from sun-ejb-jar.xml
//hs DOL Issue - information missing

    public Result check(EjbDescriptor descriptor)
    {
        Result result = getInitializedResult();
/*
 *        
	ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();
        boolean oneFailed = false;
        SunEjbJar ejbJar = descriptor.getEjbBundleDescriptor().getIasEjbObject();
        
        if(descriptor.getEjbBundleDescriptor().getTestsDone().contains(getClass().getName()))
        {
            result.setStatus(Result.NOT_RUN);
            result.addGoodDetails(smh.getLocalString("iasEjbJar.allReadyRun",
                "NOT RUN [AS-EJB ias-ejb-jar] security-role-mapping is a JAR Level Test. This test has already been run once"));
            return result;
        }
        descriptor.getEjbBundleDescriptor().setTestsDone(getClass().getName());
        
        if(ejbJar!=null)
        {
            SecurityRoleMapping secRoleMapping[] = ejbJar.getSecurityRoleMapping();
            if(secRoleMapping.length>0)
            {
                for(int i=0;i<secRoleMapping.length;i++)
                {
                    String roleName = secRoleMapping[i].getRoleName();
                    if(roleName.length()==0)
                    {
                        oneFailed = true;
                        result.failed(smh.getLocalString(getClass().getName()+".failed",
                            "FAILED [AS-EJB security-role-mapping] : role-name cannot be an empty string",
                            new Object[]{new Integer(i)}));
                    }
                    else
                    {
                        boolean roleNameFound = false;
                        Set roles = descriptor.getEjbBundleDescriptor().getRoles();
                        Iterator it = roles.iterator();
                        while(it.hasNext())
                        {
                            Role role = (Role)it.next();
                            if(role.getName().equals(roleName))
                            {
                                roleNameFound = true;
                                break;
                            }
                        }
                        if(roleNameFound)
                        {
                            result.passed(smh.getLocalString(getClass().getName()+".passed",
                                "PASSED [AS-EJB security-role-mapping] : role-name {1} verified with ejb-jar.xml",
                                new Object[]{new Integer(i), roleName}));
                        }
                        else
                        {
                            oneFailed = true;
                            //<addition> srini@sun.com Bug: 4721914
                            //result.failed(smh.getLocalString(getClass().getName()+".failed",
                            result.failed(smh.getLocalString(getClass().getName()+".failed1",
                                "FAILED [AS-EJB security-role-mapping] : role-name {1} could not be located in ejb-jar.xml",
                                new Object[]{new Integer(i), roleName}));
                            //<addition>
                        }
                    }

                    String pName[] = secRoleMapping[i].getPrincipalName();
                    for(int j=0;j<pName.length;j++)
                    {
                        if(pName[j].length()==0)
                        {
                            oneFailed = true;
                            //<addition> srini@sun.com Bug: 4721914
                            //result.failed(smh.getLocalString(getClass().getName()+".failed",
                            result.failed(smh.getLocalString(getClass().getName()+".failed2",
                                "FAILED [AS-EJB security-role-mapping] : principal-name cannot be empty string",
                                new Object[]{new Integer(i)}));
                            //<addition>
                        }
                        else
                        {
                            //<addition> srini@sun.com Bug: 4721914
                            //result.passed(smh.getLocalString(getClass().getName()+".passed",
                            result.passed(smh.getLocalString(getClass().getName()+".passed1",
                                "PASSED [AS-EJB security-role-mapping] : principal-name is {1}",
                                new Object[]{new Integer(i),pName[j]}));
                            //<addition>
                        }
                    }

                    pName = secRoleMapping[i].getGroupName();
                    for(int j=0;j<pName.length;j++)
                    {
                        if(pName[j].length()==0)
                        {
                            oneFailed = true;
                            //<addition> srini@sun.com Bug: 4721914
                            //result.failed(smh.getLocalString(getClass().getName()+".failed",
                            result.failed(smh.getLocalString(getClass().getName()+".failed3",
                                "FAILED [AS-EJB security-role-mapping] : group-name cannot be empty string",
                                new Object[]{new Integer(i)}));
                            //<addition>
                        }
                        else
                        {
                            //<addition> srini@sun.com Bug: 4721914
                            //result.passed(smh.getLocalString(getClass().getName()+".passed",
                            result.passed(smh.getLocalString(getClass().getName()+".passed2",
                                "PASSED [AS-EJB security-role-mapping] : group-name is {1}",
                                new Object[]{new Integer(i),pName[j]}));
                            //<addition>
                        }
                    }
                }
            }
            else
            {
                result.notApplicable(smh.getLocalString(getClass().getName()+".notApplicable",
                    "NOT APPLICABLE [AS-EJB] : security-role-mapping element is not defined"));
            }
            if(oneFailed)
                result.setStatus(Result.FAILED);
        }
        else
        {
            result.addErrorDetails(smh.getLocalString
                                   ("tests.componentNameConstructor",
                                    "For [ {0} ]",
                                    new Object[] {compName.toString()}));
            result.addErrorDetails(smh.getLocalString
                 (getClass().getName() + ".notRun",
                  "NOT RUN [AS-EJB] : Could not create an SunEjbJar object"));
        }
 */
        return result;
    }
}
        
