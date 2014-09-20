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
import org.glassfish.ejb.deployment.descriptor.RelationRoleDescriptor;

/**
 * Container managed relationship type field must use of the collection 
 * interface for on-to-many or many-to-many relationships and specify it in 
 * the Deployment Descriptor
 *
 * @author  Jerome Dochez
 * @version 
 */
public class CmrUseCorrectCollectionInterface extends CmrFieldTest {

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
    protected boolean runIndividualCmrTest(Descriptor descriptor, RelationRoleDescriptor rrd, Class c, Result result) {
        ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();
        if (rrd.getPartner().getIsMany()) {
            // must be one the collection interface 
            if (rrd.getCMRFieldType()==null) {
                addErrorDetails(result, compName);
              result.addErrorDetails(smh.getLocalString
		    (getClass().getName() + ".failed2",
                    "Error : CMR field [ {0} ]  cmr-field-type must be defined for one-to-many or many-to-many relationships and the value of the cmr-field-type element must be either: java.util.Collection or java.util.Set",
	            new Object[] {rrd.getCMRField()}));                
                return false;
            } else {
                CMRFieldInfo info = rrd.getCMRFieldInfo();
                if (rrd.getCMRFieldType().equals(info.type.getName())) {
                    result.addGoodDetails(smh.getLocalString
    		    (getClass().getName() + ".passed",
                        "CMR field [ {0} ] is the same type as declared in the deployment descriptors [ {1} ]",
        	            new Object[] {info.name, info.role.getCMRFieldType()}));                
                    return true;                
                } else {
                    addErrorDetails(result, compName);
                    result.addErrorDetails(smh.getLocalString
    		    (getClass().getName() + ".failed",
                        "Error : CMR field [ {0} ] is not the same type as declared in the deployment descriptors [ {1} ]",
    	            new Object[] {info.name, info.role.getCMRFieldType()}));                
                    return false;
                }            
            }
        }
       return true;        
    }
}
