/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.hk2.devtest;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import com.oracle.hk2.devtest.cdi.ejb1.BasicEjb;

/**
 * Has a main so it can be used to invoke the EJB from a client
 * 
 * @author jwells
 *
 */
public class Main {
    private final static String BASIC_EJB_JNDI_NAME = "java:global/ejb1/EjbInjectedWithServiceLocator!" +
            BasicEjb.class.getName();
    
    private static int go() throws NamingException {
        Context context = new InitialContext();
        
        BasicEjb basic = (BasicEjb) context.lookup(BASIC_EJB_JNDI_NAME);
        
        boolean ret = basic.cdiManagerInjected();
        
        System.out.println("EJB#cdiManagerInjected invoked with result " + ret);
        
        return 0;
    }
    
    public static void main(String argc[]) {
        try {
            go();
            System.exit(0);
        }
        catch (Throwable th) {
            th.printStackTrace();
            System.exit(1);
        }
        
    }
}
