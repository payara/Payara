/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

/*
 * ManagedConnectionFactoryProperties.java
 *
 * Created on September 27, 2000, 3:01 PM
 */

package com.sun.enterprise.tools.verifier.tests.connector.managed;

import java.util.*;
import java.lang.reflect.Method;
import com.sun.enterprise.deployment.ConnectorDescriptor;
import com.sun.enterprise.tools.verifier.tests.*;
import com.sun.enterprise.deployment.EnvironmentProperty;
import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.tests.connector.ConnectorCheck;

import com.sun.enterprise.deployment.ConnectionDefDescriptor;
import com.sun.enterprise.deployment.OutboundResourceAdapter;

/**
 * Test that the class declared implementing the javax.resource.spi.ManagedConnectionFactory
 * interface implements the properties declared under the config-property
 * xml tag under the followind requirements :
 *      - Provide a getter and setter method ala JavaBeans
 *      - Properties should be either bound or constrained
 *      - PropertyListener registration/unregistration methods are public
 *
 * @author  Jerome Dochez
 * @version 
 */
public class ManagedConnectionFactoryProperties 
    extends ManagedConnectionFactoryTest
    implements ConnectorCheck 
{
    /** <p>
     * Test that the class declared implementing the javax.resource.spi.ManagedConnectionFactory
     * interface implements the properties declared under the config-property
     * xml tag under the followind requirements :
     *      - Provide a getter and setter method ala JavaBeans
     *      - Properties should be either bound or constrained
     *      - PropertyListener registration/unregistration methods are public
     * </p>
     *
     * @paramm descriptor deployment descriptor for the rar file
     * @return result object containing the result of the individual test
     * performed
     */
    public Result check(ConnectorDescriptor descriptor) {

        Result result = getInitializedResult();
	ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();
        // test NA for inboundRA
        if(!descriptor.getOutBoundDefined())
        {
          result.addNaDetails(smh.getLocalString
              ("tests.componentNameConstructor",
               "For [ {0} ]",
               new Object[] {compName.toString()}));
          result.notApplicable(smh.getLocalString
              ("com.sun.enterprise.tools.verifier.tests.connector.managed.notApplicableForInboundRA",
               "Resource Adapter does not provide outbound communication"));
          return result;
        }
        boolean oneFailed=false;
	OutboundResourceAdapter outboundRA =
	    descriptor.getOutboundResourceAdapter();

	Set connDefs = outboundRA.getConnectionDefs();
	Iterator iter = connDefs.iterator();
	while(iter.hasNext()) {
	    
	    ConnectionDefDescriptor connDefDesc = (ConnectionDefDescriptor)
		iter.next();
	    Set configProperties = connDefDesc.getConfigProperties();
	    if (!configProperties.isEmpty()) {
		Iterator propIterator = configProperties.iterator();
		Class mcf = testManagedConnectionFactoryImpl(descriptor, result);
		if (mcf == null) {
		    // not much we can do without the class, the superclass should have
		    // set the error code now, just abandon
		    return result;
		}
		while (propIterator.hasNext()) {
		    EnvironmentProperty ep = (EnvironmentProperty) propIterator.next();
		    
		    // Set method first
		    String propertyName = Character.toUpperCase(ep.getName().charAt(0)) + ep.getName().substring(1);
		    String setMethodName = "set" + propertyName;
		    Class[] parmTypes = new Class[] { ep.getValueType() };
		    Method m = getMethod(mcf, setMethodName, parmTypes);
		    if (m!=null) {
			result.addGoodDetails(smh.getLocalString
					      ("tests.componentNameConstructor",
					       "For [ {0} ]",
					       new Object[] {compName.toString()}));
			result.addGoodDetails(smh.getLocalString(getClass().getName() + ".passed", 
								 "Found a JavaBeans compliant accessor method [ {0} ] for the config-property [ {1} ]",
								 new Object[] {  m, ep.getName()}));               
		    } else {
			oneFailed=true;
			result.addErrorDetails(smh.getLocalString
					       ("tests.componentNameConstructor",
						"For [ {0} ]",
						new Object[] {compName.toString()}));
			result.addErrorDetails(smh.getLocalString
					       (getClass().getName() + ".failed", 
						"Error: There is no JavaBeans compliant accessor method [ {0} ] implemented in [ {1} ] for the config-property [ {2} ]",
						new Object[] {  "public void "+ setMethodName+"("+ep.getValueType().getName()+")", 
								    mcf.getName(), 
								    ep.getName()}));                      
		    }
		    String getMethodName = "get" + propertyName;
		    m = getMethod(mcf, getMethodName, null);
		    if (m!=null) {			
			result.addGoodDetails(smh.getLocalString
					      ("tests.componentNameConstructor",
					       "For [ {0} ]",
					       new Object[] {compName.toString()}));
			result.addGoodDetails(smh.getLocalString(getClass().getName() + ".passed", 
								 "Found a JavaBeans compliant accessor method [ {0} ] for the config-property [ {1} ]",
								 new Object[] {  m, ep.getName()}));   
		    } else {
			oneFailed=true;
			result.addErrorDetails(smh.getLocalString
					       ("tests.componentNameConstructor",
						"For [ {0} ]",
						new Object[] {compName.toString()}));
			result.addErrorDetails(smh.getLocalString
					       (getClass().getName() + ".failed", 
						"Error: There is no JavaBeans compliant accessor method [ {0} ] implemented in [ {1} ] for the config-property [ {2} ]",
						new Object[] {  "public " + ep.getValueType().getName() + " " + getMethodName, 
								    mcf.getName(), 
								    ep.getName()}));                     
		    }                                
		}            
	    }
	}
        if (oneFailed) {
            result.setStatus(Result.FAILED);
        } else {
            result.setStatus(Result.PASSED);
        }
        return result;
    }
}
