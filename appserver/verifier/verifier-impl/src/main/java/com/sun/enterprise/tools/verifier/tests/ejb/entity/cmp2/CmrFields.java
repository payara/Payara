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

import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.tests.ComponentNameConstructor;
import org.glassfish.deployment.common.Descriptor;
import org.glassfish.ejb.deployment.descriptor.CMRFieldInfo;
import org.glassfish.ejb.deployment.descriptor.EjbBundleDescriptorImpl;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.glassfish.ejb.deployment.descriptor.RelationRoleDescriptor;

import java.util.Iterator;
import java.util.Set;

/**
 * Container managed relationship type field must be :
 *      a reference to a local interface of a entity bean
 *      a collection interface for oneToMany or manyToMany relationships
 *
 * @author  Jerome Dochez
 * @author Sheetal Vartak
 * @version 
 */
public class CmrFields extends CmrFieldTest {

    /**
     * run an individual verifier test of a declated cmr field of the class
     *
     * @param entity the descriptor for the entity bean containing the cmp-field    
     * @param info the descriptor for the declared cmr field
     * @param c the class owning the cmp field
     * @parma r the result object to use to put the test results in
     * 
     * @return true if the test passed
     */            
    protected boolean runIndividualCmrTest(Descriptor descriptor, RelationRoleDescriptor role, Class c, Result result) {
     
	boolean foundIt = false;
	CMRFieldInfo info = null;
	ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();

	try { 
	    info  = role.getCMRFieldInfo();
        }catch (Exception e) {
        addErrorDetails(result, compName);
	    result.addErrorDetails(smh.getLocalString
		   (getClass().getName() + ".failed1",
		    "Error: No Local interfaces defined for EJB [ {0} ]",
	            new Object[] {descriptor.getName()}));
	    return false;
	    
	}   
        if (role.getPartner().getIsMany()) {
            // must be one the collection interface 
            if (info.type.getName().equals("java.util.Collection") ||
                info.type.getName().equals("java.util.Set")) {
                foundIt = true;
            } 
        } else {
	    EjbBundleDescriptorImpl bundle = ((EjbDescriptor) descriptor).getEjbBundleDescriptor();
	    if(((EjbDescriptor) descriptor).getLocalClassName() != null && 
	       !"".equals(((EjbDescriptor) descriptor).getLocalClassName())) {
		if (isValidInterface(info.type, bundle.getEjbs())) {
		    foundIt = true;
		}
	    }
	    else {
		if ((role.getRelationshipDescriptor()).getIsBidirectional()) {
		    result.addErrorDetails(smh.getLocalString
			   (getClass().getName() + ".failed",
			    "Error: Invalid type assigned for container managed relationship [ {0} ] in bean [ {1} ]",
			    new Object[] {info.name , descriptor.getName()}));
		    return false;
		}
		else foundIt = true;
	    }
        }
        if (foundIt) {
	     result.addGoodDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
            result.addGoodDetails(smh.getLocalString
	           (getClass().getName() + ".passed",
		    "Valid type assigned for container managed relationship [ {0} ] in bean [ {1} ]",
	                    new Object[] {info.name , descriptor.getName()}));
        } else {
            result.addErrorDetails(smh.getLocalString
		   (getClass().getName() + ".failed",
		    "Error: Invalid type assigned for container managed relationship [ {0} ] in bean [ {1} ]",
	            new Object[] {info.name , descriptor.getName()}));
        }
        return foundIt;
  
    }
    
    private boolean isValidInterface(Class fieldType, Set<EjbDescriptor> entities) {
        String component = "";
        if (entities==null)
            return false;
	// only local interface can be a valid interface
        Iterator<EjbDescriptor> iterator = entities.iterator();
        while (iterator.hasNext()) {
            EjbDescriptor entity = iterator.next();
	    if (fieldType.getName().equals(entity.getLocalClassName()))
		return true;
	}
        return false;
    }
   
}
