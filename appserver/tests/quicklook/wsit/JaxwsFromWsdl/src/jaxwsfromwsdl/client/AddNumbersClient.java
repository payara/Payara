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

package jaxwsfromwsdl.client;

public class AddNumbersClient {

    public AddNumbersClient() {
    }

    public boolean testAddNumbers() {
        
        boolean status=false;
        AddNumbersPortType port = null;

        try {
            port = new AddNumbersService().getAddNumbersPort ();
            int number1 = 10;
            int number2 = 20;
            int number3 = 30;
            
            // System.out.printf ("Invoking addNumbers(%d, %d)\n", number1, number2);
            int result = port.addNumbers (number1, number2);
            // System.out.printf ("The result of adding %d and %d is %d.\n\n", number1, number2, result);
            if (result == number3)
	      status = true;
        } catch(Exception ex){
            System.out.print("Got unexpected exception");
            // ex.printStackTrace();
	} 
        return status;
        
    }

    public boolean testAddNumbersException() {
        
        boolean status=false;
        AddNumbersPortType port = null;

        try {
            port = new AddNumbersService().getAddNumbersPort ();
            int number1 = -10;
            int number2 = 20;
            
            // System.out.printf ("Invoking addNumbers(%d, %d)\n", number1, number2);
            int result = port.addNumbers (number1, number2);
            // System.out.printf ("The result of adding %d and %d is %d.\n\n", number1, number2, result);
        } catch (AddNumbersFault_Exception ex) {
	    // System.out.print("Got expected exception");
  	    // System.out.printf ("Caught AddNumbersFault_Exception: %s\n", ex.getFaultInfo().getFaultInfo ());
            String info1 = ex.getFaultInfo().getFaultInfo();
	    // System.out.print("info1="+info1+"---");     
            String info2 = ex.getFaultInfo().getMessage();
	    // System.out.print("info2="+info2+"---");     
            if (info2.contains("Negative number cant be added!")) 
		status = true;
	} 

        return status;
        
    }

    public static void main (String[] args) {
        System.out.println("AddNumbersClient:main");
        AddNumbersClient client = new AddNumbersClient();
        boolean result = false;
        result = client.testAddNumbers();
        System.out.println("result1="+result);
        result = client.testAddNumbersException();
        System.out.println("result2="+result); 
    }
}

