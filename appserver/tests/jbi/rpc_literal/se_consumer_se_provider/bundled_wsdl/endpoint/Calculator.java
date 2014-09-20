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

package endpoint;

import javax.jws.WebService;
import javax.jws.WebMethod;
import javax.jws.*;
import javax.jws.soap.SOAPBinding;
import common.IncomeTaxDetails;

@WebService(
        name="Calculator",
        serviceName="CalculatorService",
        targetNamespace="http://example.web.service/Calculator",
		wsdlLocation = "WEB-INF/wsdl/CalculatorService.wsdl"
        )
@SOAPBinding(style=SOAPBinding.Style.RPC, use=SOAPBinding.Use.LITERAL)

public class Calculator {

	public static final String testName = "\nTest :: rpc-literal-bundled-wsdl : ";
    public Calculator() {}
    
    @WebMethod(operationName="add", action="urn:Add")
    public int add(
            @WebParam(name = "int_1", partName = "int_1") int i,
            @WebParam(name = "int_2", partName = "int_2") int j
            ) throws Exception {
        int k = i +j ;
        System.out.println(testName + i + "+" + j +" = " + k);
        return k;
    }

	@WebMethod(operationName="calculateIncomeTax", action="urn:CalculateIncomeTax")
	public long calculateIncomeTax(IncomeTaxDetails details
			 , IncomeTaxDetails details2
			 , IncomeTaxDetails details3
			 , IncomeTaxDetails details4
			 , IncomeTaxDetails details5
			 , IncomeTaxDetails details6
			 , IncomeTaxDetails details7
			 , IncomeTaxDetails details8
			 , IncomeTaxDetails details9
			 , IncomeTaxDetails details10
			) {
		long income = details.annualIncome;
		System.out.println(testName + "Annual income = " + income);
		long taxRate = 30; // 30%
		long taxToBePaid = income / taxRate;
		System.out.println(testName +"Tax to be paid = " + taxToBePaid);
		return taxToBePaid;
	}

	@WebMethod(operationName="sayHi", action="urn:SayHi")
	public String sayHi() {
		return testName + "Hi from sayHi()";
	}

	@WebMethod(operationName="printHi", action="urn:PrintHi")
	public void printHi() {
		System.out.println(testName +"Hi from printHi()");
	}

	@WebMethod(operationName="printHiToMe", action="urn:PrintHiToMe")
	public void printHiToMe(String name) {
		System.out.println(testName +"Hi to " + name + " from printHiToMe()");
	}
}
