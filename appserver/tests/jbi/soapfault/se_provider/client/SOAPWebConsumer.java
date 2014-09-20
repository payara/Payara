/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2010 Oracle and/or its affiliates. All rights reserved.
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

package client;
import javax.xml.ws.WebServiceRef;
import javax.xml.ws.Service;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.soap.SOAPFaultException;
import com.example.calculator.CalculatorService;
import com.example.calculator.Calculator;
import com.sun.ejte.ccl.reporter.SimpleReporterAdapter;

public class SOAPWebConsumer {
    //@WebServiceRef(wsdlLocation="http://localhost:12011/calculatorendpoint/CalculatorService?WSDL")
    //static CalculatorService service;
    CalculatorService service = new CalculatorService();
    private static SimpleReporterAdapter stat =
                new SimpleReporterAdapter("jbi");
	private static String testId = "jbi-serviceengine/soapfault/se_provider";

    public static void main (String[] args) {
        stat.addDescription(testId);
	SOAPWebConsumer client = new SOAPWebConsumer();
	client.addUsingSOAPConsumer();
        stat.printSummary(testId );
    }

    private void addUsingSOAPConsumer() {
	com.example.calculator.Calculator port= null;

                port = service.getCalculatorPort();

		// Get Stub
		BindingProvider stub = (BindingProvider)port;
		String endpointURI ="http://localhost:12011/calculatorendpoint";
		stub.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
		      endpointURI);

		String failedMsg = null;

		try {
	        System.out.println("\nInvoking throwRuntimeException");
		   	port.throwRuntimeException("bhavani");
		} catch(Exception ex) {
			System.out.println(ex);
			if(!(ex instanceof SOAPFaultException) || 
			!(ex.getMessage().equals("java.lang.RuntimeException: Calculator :: Threw Runtime Exception"))) {
				failedMsg = "port.throwRuntimeException() did not receive RuntimeException 'Calculator :: Threw Runtime Exception'";
			}
		}

		try {
	        System.out.println("\nInvoking throwApplicationException");
		   	port.throwApplicationException("bhavani");
		} catch(Exception ex) {
			System.out.println(ex);
			if(!(ex instanceof com.example.calculator.Exception_Exception)) {
				failedMsg = "port.throwApplicationException() did not throw ApplicationException";
			}
		}

		if(failedMsg != null) {
	        stat.addStatus(testId, stat.FAIL);
		} else {
        	stat.addStatus(testId, stat.PASS);
		}
    }
}
