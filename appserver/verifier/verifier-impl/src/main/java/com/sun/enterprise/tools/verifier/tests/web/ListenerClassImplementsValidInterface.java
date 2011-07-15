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

package com.sun.enterprise.tools.verifier.tests.web;

import com.sun.enterprise.tools.verifier.Result;

/** 
 * Listener class must implement a valid interface.
 * Verify that the Listener class implements one of five valid interfaces.
 * 
 * @author Jerome Dochez
 * @version 1.0
 */
public class ListenerClassImplementsValidInterface extends ListenerClass implements WebCheck {
        
    /**
     * <p>
     * Run the verifier test against a declared individual listener class
     * </p>
     *
     * @param result is used to put the test results in
     * @param listenerClass is the individual listener class object to test
     * @return true if the test pass
     */
    protected boolean runIndividualListenerTest(Result result, Class listenerClass) {
        
        boolean validInterface = false;
        Class clazz = listenerClass;
        
        if (listenerClass==null) {
            return false;
        }
        
	validInterface = isImplementorOf(clazz,"javax.servlet.ServletContextAttributeListener");
	if (validInterface != true) {
	    validInterface = isImplementorOf(clazz,"javax.servlet.ServletContextListener");
	}
	if (validInterface != true) {
	    validInterface = isImplementorOf(clazz,"javax.servlet.http.HttpSessionAttributeListener");
	}
	if (validInterface != true) {
	    validInterface = isImplementorOf(clazz,"javax.servlet.http.HttpSessionListener");
	}
    if (validInterface != true) {
	    validInterface = isImplementorOf(clazz,"javax.servlet.ServletRequestAttributeListener");
	}if (validInterface != true) {
	    validInterface = isImplementorOf(clazz,"javax.servlet.ServletRequestListener");
	}if (validInterface != true) {
	    validInterface = isImplementorOf(clazz,"javax.servlet.http.HttpSessionBindingListener");
	}

         if (validInterface) {
             result.addGoodDetails(smh.getLocalString
                (getClass().getName() + ".passed",
                 "Listener class [ {0} ] implements a valid interface.",
                 new Object[] {listenerClass.getName()}));
         } else if (!validInterface){
             result.addErrorDetails(smh.getLocalString
                (getClass().getName() + ".failed",
                 "Error: Listener class [ {0} ] does not implement one or more of the following valid interface.\n javax.servlet.ServletContextAttributeListener, javax.servlet.ServletContextListener, javax.servlet.http.HttpSessionAttributeListener, javax.servlet.http.HttpSessionListener",
                  new Object[] {clazz.getName()}));
	 }
	    	                     
        return validInterface;
    }
}
