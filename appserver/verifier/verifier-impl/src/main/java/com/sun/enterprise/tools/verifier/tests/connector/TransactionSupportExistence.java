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
 * TransactionSupportExistence.java
 *
 * Created on September 28, 2000, 2:09 PM
 */

package com.sun.enterprise.tools.verifier.tests.connector;

import java.io.File;
import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.Verifier;
import com.sun.enterprise.deployment.ConnectorDescriptor;
import com.sun.enterprise.tools.verifier.tests.*;
import com.sun.enterprise.deployment.xml.ConnectorTagNames;

/**
 * Test the implementation of the proprer transaction support depending on the 
 * level of transaction declared in the deployment descriptor
 *
 * @author  Jerome Dochez
 * @version 
 */
public class TransactionSupportExistence
    extends ConnectorTest 
    implements ConnectorCheck 
{

    /** <p>
     * Test the implementation of the proprer transaction support depending on 
     * the level of transaction declared in the deployment descriptor :
     *  - NoTransaction    neither XAResource or LocalTransaction should be
     *                      implemented, warning if it does
     *  - LocalTransaction LocalTransaction has to be implemented
     *  - XATransaction    XAResource has to be implemented         
     * </p>
     *
     * @param descriptor deployment descriptor for the rar file
     * @return result object containing the result of the individual test
     * performed
     */
    public Result check(ConnectorDescriptor descriptor) {
                
        Result result = getInitializedResult();
	ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();
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
        String connectorTransactionSupport =
        descriptor.getOutboundResourceAdapter().getTransSupport();
        
        // No transaction support specified, this is an error
        if (connectorTransactionSupport==null) {
	    result.addErrorDetails(smh.getLocalString
				   ("tests.componentNameConstructor",
				    "For [ {0} ]",
				    new Object[] {compName.toString()}));
	    result.failed(smh.getLocalString
			  ("com.sun.enterprise.tools.verifier.tests.connector.TransactionSupport.nonexist",
			   "Error: No Transaction support specified for ressource adapter",
			   new Object[] {connectorTransactionSupport}));        
            return result;
        }        
        
        // get the rar file handle
       // File jarFile = Verifier.getJarFile(descriptor.getModuleDescriptor().getArchiveUri());
        
//        File f=Verifier.getArchiveFile(descriptor.getModuleDescriptor().getArchiveUri());
        if (connectorTransactionSupport.equals(ConnectorTagNames.DD_NO_TRANSACTION)) {
            boolean oneFailed=false;
            if (findImplementorOf(descriptor, "javax.resource.spi.LocalTransaction")!=null) {
                oneFailed = true;
		result.addWarningDetails(smh.getLocalString
					  ("tests.componentNameConstructor",
					   "For [ {0} ]",
					   new Object[] {compName.toString()}));
                result.warning(smh.getLocalString(getClass().getName() + ".warning",
                "Warning: Transaction support {0} is specified for ressource adapter but [ {1} ] is implemented",
		new Object[] {"NoTransaction", "javax.resource.spi.LocalTransaction"}));     
            }
            if (findImplementorOf(descriptor, "javax.transaction.xa.XAResource")!=null) {
                oneFailed = true;
		result.addWarningDetails(smh.getLocalString
					  ("tests.componentNameConstructor",
					   "For [ {0} ]",
					   new Object[] {compName.toString()}));
                result.warning(smh.getLocalString(getClass().getName() + ".warning",
                "Warning: Transaction support {0} is specified for ressource adapter but [ {1} ] is implemented",
		new Object[] {"NoTransaction", "javax.transaction.xa.XAResource"}));     
            }
            if (!oneFailed) {
                result.addGoodDetails(smh.getLocalString
					  ("tests.componentNameConstructor",
					   "For [ {0} ]",
					   new Object[] {compName.toString()}));	
		result.passed(smh.getLocalString(getClass().getName() + ".passed1",
                    "Transaction support NoTransaction is specified for ressource adapter and [ {0} ] are not implemented",
                    new Object[] {"javax.transaction.xa.XAResource, javax.resource.spi.LocalTransaction"}));                          
            }
        }
        else {
            if (connectorTransactionSupport.equals(ConnectorTagNames.DD_LOCAL_TRANSACTION)) {
                if (findImplementorOf(descriptor, "javax.resource.spi.LocalTransaction")==null) {
                    result.addErrorDetails(smh.getLocalString
					  ("tests.componentNameConstructor",
					   "For [ {0} ]",
					   new Object[] {compName.toString()}));
		result.failed(smh.getLocalString(getClass().getName() + ".nonexist",
                    "Error: Transaction support {0} is specified for ressource adapter but [ {1} ] is not implemented",
		    new Object[] {"LocalTransaction", "javax.resource.spi.LocalTransaction"}));     
                } else {                
                    if (findImplementorOf(descriptor, "javax.transaction.xa.XAResource")!=null) {
			result.addWarningDetails(smh.getLocalString
					  ("tests.componentNameConstructor",
					   "For [ {0} ]",
					   new Object[] {compName.toString()}));
                        result.addWarningDetails(smh.getLocalString(getClass().getName() + ".warning",
                        "Warning: Transaction support {0} is specified for ressource adapter but [ {1} ] is implemented",
                		new Object[] {"LocalTransaction", "javax.transaction.xa.XAResource"}));     
                    } else {
                        result.addGoodDetails(smh.getLocalString
					  ("tests.componentNameConstructor",
					   "For [ {0} ]",
					   new Object[] {compName.toString()}));
			result.passed(smh.getLocalString(getClass().getName() + ".passed2",
                            "Transaction support {0} is specified for ressource adapter and [ {1} ] is(are) implemented",
                		new Object[] {"LocalTransaction", "javax.resource.spi.LocalTransaction"}));                             
                    }
                }                            
            } else {
                if (connectorTransactionSupport.equals(ConnectorTagNames.DD_XA_TRANSACTION)) {
                    boolean oneFailed = false;
                    if (findImplementorOf(descriptor, "javax.resource.spi.LocalTransaction")==null) {
                        oneFailed = true;
                        result.addErrorDetails(smh.getLocalString
					  ("tests.componentNameConstructor",
					   "For [ {0} ]",
					   new Object[] {compName.toString()}));
			result.failed(smh.getLocalString(getClass().getName() + ".nonexist",
                        "Error: Transaction support {0} is specified for ressource adapter but [ {1} ] is not implemented",
		        new Object[] {"XATransaction", "javax.resource.spi.LocalTransaction"}));                         
                    }
                    if (findImplementorOf(descriptor, "javax.transaction.xa.XAResource")==null) {
                        oneFailed = true;
                        result.addErrorDetails(smh.getLocalString
					  ("tests.componentNameConstructor",
					   "For [ {0} ]",
					   new Object[] {compName.toString()}));
			result.failed(smh.getLocalString(getClass().getName() + ".nonexist",
                        "Error: Transaction support {0} is specified for ressource adapter but [ {1} ] is not implemented",
		        new Object[] {"XATransaction", "javax.transaction.xa.XAResource"}));                         
                    }
                    if (!oneFailed) {
                        result.addGoodDetails(smh.getLocalString
					  ("tests.componentNameConstructor",
					   "For [ {0} ]",
					   new Object[] {compName.toString()}));
			result.passed(smh.getLocalString(getClass().getName() + ".passed2",
                            "Transaction support {0} is specified for ressource adapter and [ {1} ] is(are) implemented",
                            new Object[] {"XATransaction", "javax.transaction.xa.Transaction, javax.resource.spi.LocalTransaction"}));                               
                    }
                } else {
                    // unknow transaction support
	            result.addErrorDetails(smh.getLocalString
					  ("tests.componentNameConstructor",
					   "For [ {0} ]",
					   new Object[] {compName.toString()}));
		    result.failed(smh.getLocalString
	                ("com.sun.enterprise.tools.verifier.tests.connector.TransactionSupport.failed",
                        "Error: Deployment descriptor transaction-support [ {0} ] for ressource adapter is not valid",
		        new Object[] {connectorTransactionSupport}));                        
                }
            }
        } 
        return result;
    }
}
