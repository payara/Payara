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

package com.sun.enterprise.tools.verifier.tests.ejb.entity.ejbql;

import com.sun.enterprise.deployment.MethodDescriptor;
import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.StringManagerHelper;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.jdo.spi.persistence.support.ejb.ejbqlc.EJBQLC;
import com.sun.jdo.spi.persistence.support.ejb.ejbqlc.EJBQLException;
import org.glassfish.ejb.deployment.descriptor.EjbCMPEntityDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.glassfish.ejb.deployment.descriptor.PersistenceDescriptor;
import org.glassfish.ejb.deployment.descriptor.QueryDescriptor;

import java.lang.reflect.Method;
import java.util.Iterator;


/**
 * This class contains tests for EJB QLs that are shared 
 * by tests for entity beans and for depenent objects.
 *
 * @author	Qingqing Ouyang
 * @version
 */
public class EjbQLChecker {
    
    /**
     * <p>
     * helper property to get to the localized strings
     * </p>
     */
    protected static final LocalStringManagerImpl smh = 
        StringManagerHelper.getLocalStringsManager();
    
    /**
     * Check the syntax and semantics of the targetted
     * queries.
     *
     * @param desc An PersistenceDescriptor object.
     * @param ejbqlDriver An EjbQlDriver created using the
     *        targetted ejb bundle.
     * @param result The test results.
     * @param ownerClassName Name of the class initiated the test.
     * @return whether any error has occurred.
     */
    public static boolean checkSyntax (EjbDescriptor ejbDesc,
            EJBQLC ejbqlDriver, Result result, String ownerClassName) {
        
        boolean hasError = false;
        String query = null;
        PersistenceDescriptor desc = ((EjbCMPEntityDescriptor)ejbDesc).getPersistenceDescriptor();
        
        for (Iterator it = desc.getQueriedMethods().iterator(); it.hasNext();) {
            MethodDescriptor method = (MethodDescriptor) it.next();
            try {
                QueryDescriptor qDesc = desc.getQueryFor(method);
                query = qDesc.getQuery();
                
                if (qDesc.getIsEjbQl()) {
                    Method m = method.getMethod(ejbDesc);

                    int retypeMapping = mapRetType(qDesc.getReturnTypeMapping());
        
                    boolean finder = false;

                    if ((method.getName()).startsWith("find")) {
                       finder = true;
                       retypeMapping = 2; /*QueryDescriptor.NO_RETURN_TYPE_MAPPING;*/
                    }

                    ejbqlDriver.compile(query, m, retypeMapping, finder, ejbDesc.getName());
                }
            } catch (EJBQLException ex) {
                ex.printStackTrace();
                if (!hasError) {
                    hasError = true;
                }
	
                result.addErrorDetails
                    (smh.getLocalString(ownerClassName + ".parseError",
                            "Error: [ {0} ] has parsing error(s)",
                            new Object[] {query}));
		result.addErrorDetails
		    (smh.getLocalString(ownerClassName + ".SAXParseException",
                            "Exception occured : [{0}]",
                            new Object[] {ex.toString()}));
            }

        }
	if (hasError == false) {
	    result.addGoodDetails
		    (smh.getLocalString(ownerClassName + ".passed",
                            " Syntax and Semantics of the Queries are correct",
			    new Object[] {}));
	}
        return hasError;
    }

 private static int mapRetType(int rettype) {

    switch(rettype) {

    case 0 : return 2;
    case 1 : return 0;
    case 2 : return 1;
    default: return 2;
         
    }

 }

}
