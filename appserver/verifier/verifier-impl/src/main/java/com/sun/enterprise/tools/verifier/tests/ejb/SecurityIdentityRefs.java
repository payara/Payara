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

package com.sun.enterprise.tools.verifier.tests.ejb;

import com.sun.enterprise.deployment.RunAsIdentityDescriptor;
import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.tests.ComponentNameConstructor;
import org.glassfish.ejb.deployment.descriptor.EjbBundleDescriptorImpl;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.glassfish.security.common.Role;

import java.util.Iterator;
import java.util.Set;

/** 
 * Security role references test.
 * The Bean provider must declare all of the enterprise's bean references 
 * to security roles as specified in section 15.2.1.3 of the Moscone spec.
 * Role names must be mapped to names within the jar.
 */
public class SecurityIdentityRefs extends EjbTest { 


  /** 
   * Security role references test.
   * The Bean provider must declare all of the enterprise's bean references
   * to security roles as specified in section 15.2.1.3 of the Moscone spec.
   * Role names must be mapped to names within the jar.
   *
   * @param descriptor the Enterprise Java Bean deployment descriptor
   *
   * @return <code>Result</code> the results for this assertion
   */
  public Result check(EjbDescriptor descriptor) {

    Result result = getInitializedResult();
    ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();
if (descriptor.getUsesCallerIdentity()){
        result.addNaDetails(smh.getLocalString
            ("tests.componentNameConstructor",
             "For [ {0} ]",
             new Object[] {compName.toString()}));
        result.notApplicable(smh.getLocalString(
              "com.sun.enterprise.tools.verifier.tests.ejb.SecurityIdentityRefs.notApplicable3",
              "Bean [ {0} ] does not specify a run-as identity",
              new Object[] {descriptor.getName()}));
        return result;
    }
    RunAsIdentityDescriptor identity = descriptor.getRunAsIdentity();
    if (identity == null) {
      result.addNaDetails(smh.getLocalString
          ("tests.componentNameConstructor",
           "For [ {0} ]",
           new Object[] {compName.toString()}));
      result.notApplicable(smh.getLocalString(
            "com.sun.enterprise.tools.verifier.tests.ejb.SecurityIdentityRefs.notApplicable2",
            "Bean [ {0} ] does not specify a security identity",
            new Object[] {descriptor.getName()}));                    
      return result;
    }

    EjbBundleDescriptorImpl bundleDescriptor = descriptor.getEjbBundleDescriptor();
    Set roles = bundleDescriptor.getRoles();
    Iterator roleIterator = roles.iterator();
    while (roleIterator.hasNext()) {
      Role role = (Role) roleIterator.next();
      if (role.getName().equals(identity.getRoleName())) {
        result.addGoodDetails(smh.getLocalString
            ("tests.componentNameConstructor",
             "For [ {0} ]",
             new Object[] {compName.toString()}));
        result.passed(smh.getLocalString(
              "com.sun.enterprise.tools.verifier.tests.ejb.SecurityIdentityRefs.passed",
              "Security identity run-as specified identity [ {0} ] role is found in the list of roles",
              new Object[] {role.getName()}));        
        return result;                
      }
    }
    result.addErrorDetails(smh.getLocalString
        ("tests.componentNameConstructor",
         "For [ {0} ]",
         new Object[] {compName.toString()}));
    result.failed(smh.getLocalString(
          "com.sun.enterprise.tools.verifier.tests.ejb.SecurityIdentityRefs.failed",
          "Security identity run-as specified identity [ {0} ] role is not valid",
          new Object[] {identity.getRoleName()}));        
    return result;                
  }

}
