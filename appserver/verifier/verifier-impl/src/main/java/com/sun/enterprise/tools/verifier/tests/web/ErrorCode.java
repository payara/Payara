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

package com.sun.enterprise.tools.verifier.tests.web;

import java.util.*;
import com.sun.enterprise.deployment.*;
import com.sun.enterprise.tools.verifier.*;
import com.sun.enterprise.tools.verifier.tests.*;
import org.glassfish.web.deployment.descriptor.ErrorPageDescriptor;
import org.glassfish.web.deployment.descriptor.WebBundleDescriptorImpl;


/** Error code element contains an HTTP error code within web application test.
 *  i.e. 404
 */
public class ErrorCode extends WebTest implements WebCheck { 

    
    /**
     * Determine is Error Code is valid.
     *
     * @param errorCodeTypeInteger  error code type
     *
     * @return <code>boolean</code> true if error code is valid, false otherwise
     */
    public static boolean isValidErrorCode(Integer errorCodeTypeInteger) {
	int errorCodeType = errorCodeTypeInteger.intValue();
	if ((errorCodeType == ErrorCodeTypes.CONTINUE) ||
	    (errorCodeType == ErrorCodeTypes.SWITCHING_PROTOCOLS) ||
	    (errorCodeType == ErrorCodeTypes.OK) ||
	    (errorCodeType == ErrorCodeTypes.CREATED) ||
	    (errorCodeType == ErrorCodeTypes.ACCEPTED) ||
	    (errorCodeType == ErrorCodeTypes.NON_AUTHORITATIVE_INFORMATION) ||
	    (errorCodeType == ErrorCodeTypes.NO_CONTENT) ||
	    (errorCodeType == ErrorCodeTypes.RESET_CONTENT) ||
	    (errorCodeType == ErrorCodeTypes.PARTIAL_CONTENT) ||
	    (errorCodeType == ErrorCodeTypes.MULTIPLE_CHOICES) ||
	    (errorCodeType == ErrorCodeTypes.MOVED_PERMANENTLY) ||
	    (errorCodeType == ErrorCodeTypes.FOUND) ||
	    (errorCodeType == ErrorCodeTypes.SEE_OTHER) ||
	    (errorCodeType == ErrorCodeTypes.NOT_MODIFIED) ||
	    (errorCodeType == ErrorCodeTypes.USE_PROXY) ||
	    (errorCodeType == ErrorCodeTypes.UNUSED) ||
	    (errorCodeType == ErrorCodeTypes.TEMPORARY_REDIRECT) ||
	    (errorCodeType == ErrorCodeTypes.BAD_REQUEST) ||
	    (errorCodeType == ErrorCodeTypes.UNAUTHORIZED) ||
	    (errorCodeType == ErrorCodeTypes.PAYMENT_REQUIRED) ||
	    (errorCodeType == ErrorCodeTypes.FORBIDDEN) ||
	    (errorCodeType == ErrorCodeTypes.NOT_FOUND) ||
	    (errorCodeType == ErrorCodeTypes.METHOD_NOT_ALLOWED) ||
	    (errorCodeType == ErrorCodeTypes.NOT_ACCEPTABLE) ||
	    (errorCodeType == ErrorCodeTypes.PROXY_AUTHENTICATION_REQUIRED) ||
	    (errorCodeType == ErrorCodeTypes.REQUEST_TIMEOUT) ||
	    (errorCodeType == ErrorCodeTypes.CONFLICT) ||
	    (errorCodeType == ErrorCodeTypes.GONE) ||
	    (errorCodeType == ErrorCodeTypes.LENGTH_REQUIRED) ||
	    (errorCodeType == ErrorCodeTypes.PRECONDITION_FAILED) ||
	    (errorCodeType == ErrorCodeTypes.REQUEST_ENTITY_TOO_LARGE) ||
	    (errorCodeType == ErrorCodeTypes.REQUEST_URI_TOO_LONG) ||
	    (errorCodeType == ErrorCodeTypes.UNSUPPORTED_MEDIA_TYPE) ||
	    (errorCodeType == ErrorCodeTypes.REQUESTED_RANGE_NOT_SATISFIABLE) ||
	    (errorCodeType == ErrorCodeTypes.EXPECTATION_FAILED) ||
	    (errorCodeType == ErrorCodeTypes.INTERNAL_SERVER_ERROR) ||
	    (errorCodeType == ErrorCodeTypes.NOT_IMPLEMENTED) ||
	    (errorCodeType == ErrorCodeTypes.BAD_GATEWAY) ||
	    (errorCodeType == ErrorCodeTypes.SERVICE_UNAVAILABLE) ||
	    (errorCodeType == ErrorCodeTypes.GATEWAY_TIMEOUT) ||
	    (errorCodeType == ErrorCodeTypes.HTTP_VERSION_NOT_SUPPORTED)) {
	    return true;
	} else {
	    return false;
	}
 
    }

    /** Error code element contains an HTTP error code within web application test.
     *  i.e. 404
     *
     * @param descriptor the Web deployment descriptor 
     *
     * @return <code>Result</code> the results for this assertion
     */
    public Result check(WebBundleDescriptor descriptor) {

	Result result = getInitializedResult();
	ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();

	if (((WebBundleDescriptorImpl)descriptor).getErrorPageDescriptors().hasMoreElements()) {
	    boolean oneFailed = false;
	    boolean foundIt = false;
            int oneErrorCode = 0;
            int oneNA = 0;
	    // get the errorpage's in this .war
	    for (Enumeration e = ((WebBundleDescriptorImpl)descriptor).getErrorPageDescriptors() ; e.hasMoreElements() ;) {
		foundIt = false;
                oneErrorCode++;
		ErrorPageDescriptor errorpage = (ErrorPageDescriptor) e.nextElement();
                String exceptionType = errorpage.getExceptionType();
                if (!((exceptionType != null) && (exceptionType.length() > 0))) {
		    Integer errorCode = new Integer( errorpage.getErrorCode() );
		    if (isValidErrorCode(errorCode)) {
			foundIt = true;
		    } else {
			foundIt = false;
		    }
   
		    if (foundIt) {
			result.addGoodDetails(smh.getLocalString
					   ("tests.componentNameConstructor",
					    "For [ {0} ]",
					    new Object[] {compName.toString()}));
			result.addGoodDetails(smh.getLocalString
					      (getClass().getName() + ".passed",
					       "Error code [ {0} ] contains valid HTTP error code within web application [ {1} ]",
					       new Object[] {errorCode.toString(), descriptor.getName()}));
		    } else {
			if (!oneFailed) {
			    oneFailed = true;
			}
			result.addErrorDetails(smh.getLocalString
					   ("tests.componentNameConstructor",
					    "For [ {0} ]",
					    new Object[] {compName.toString()}));
			result.addErrorDetails(smh.getLocalString
					       (getClass().getName() + ".failed",
						"Error: error-code [ {0} ] does not contain valid HTTP error code within web application [ {1} ]",
						new Object[] {errorCode.toString(), descriptor.getName()}));
		    }
                } else {
                    // maybe ErrorCode is not used 'cause we are using Exception
                    // if that is the case, then test is N/A,
		    result.addNaDetails(smh.getLocalString
					   ("tests.componentNameConstructor",
					    "For [ {0} ]",
					    new Object[] {compName.toString()}));
                    result.addNaDetails(smh.getLocalString
					(getClass().getName() + ".notApplicable1",
					 "Not Applicable: Error-code is [ {0} ], using [ {1} ] instead within web application [ {2} ]",
					 new Object[] {new Integer(errorpage.getErrorCode()), exceptionType, descriptor.getName()}));
                    oneNA++;
                }
	    }
	    if (oneFailed) {
		result.setStatus(Result.FAILED);
            } else if (oneNA == oneErrorCode) {
                result.setStatus(Result.NOT_APPLICABLE);
	    } else {
		result.setStatus(Result.PASSED);
	    }
	} else {
	    result.addNaDetails(smh.getLocalString
					   ("tests.componentNameConstructor",
					    "For [ {0} ]",
					    new Object[] {compName.toString()}));
	    result.notApplicable(smh.getLocalString
				 (getClass().getName() + ".notApplicable",
				  "There are no error-code elements within the web archive [ {0} ]",
				  new Object[] {descriptor.getName()}));
	}

	return result;
    }
}
