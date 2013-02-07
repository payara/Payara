/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.hk2.devtest.cdi.ear.war1;

import java.io.IOException;
import java.io.PrintWriter;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.oracle.hk2.devtest.cdi.ear.ejb1.Ejb1HK2Service;
import com.oracle.hk2.devtest.cdi.ear.lib1.HK2Service;
import com.oracle.hk2.devtest.cdi.ear.lib1.Lib1HK2Service;

/**
 * 
 * @author jwells
 *
 */
public class War1 extends HttpServlet {
    /**
     * For serialization
     */
    private static final long serialVersionUID = 8705667047049271376L;

    @Inject
    private Lib1HK2Service lib1Hk2Service;
    
    @Inject
    private Ejb1HK2Service ejb1Hk2Service;
    
    @Inject
    private War1HK2Service war1Hk2Service;
    
    /**
     * Just prints out the value of the ServiceLocator getName
     */
    @Override
    public void doGet(HttpServletRequest request,
            HttpServletResponse response)
        throws IOException, ServletException {
        
        if (lib1Hk2Service == null || !lib1Hk2Service.getComponentName().equals(HK2Service.LIB1)) {
            throw new ServletException("lib1HK2Service from lib1 was invalid: " + lib1Hk2Service);
        }
        
        if (ejb1Hk2Service == null || !ejb1Hk2Service.getComponentName().equals(HK2Service.EJB1)) {
            throw new ServletException("ejb1HK2Service from ejb1 was invalid: " + ejb1Hk2Service);
        }
        
        if (war1Hk2Service == null || !war1Hk2Service.getComponentName().equals(HK2Service.WAR1)) {
            throw new ServletException("war1HK2Service from war1 was invalid: " + war1Hk2Service);
        }

        response.setContentType("text/html");
        PrintWriter writer = response.getWriter();
        
        writer.println("<html>");
        writer.println("<head>");
        writer.println("<title>Iso1 WebApp</title>");
        writer.println("</head>");
        writer.println("<body>");
        
        writer.println("success");

        writer.println("</body>");
        writer.println("</html>");
    }
}
