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
package com.oracle.hk2.devtest.isolation.web.iso2;

import java.io.IOException;
import java.io.PrintWriter;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.glassfish.hk2.api.ServiceLocator;

/**
 * Simple returns the name of the HABITAT property
 * 
 * @author jwells
 */
public class Iso2Servlet extends HttpServlet {
    /**
     * For serialization
     */
    private static final long serialVersionUID = -9177540431267005946L;
    
    private static final String HABITAT_ATTRIBUTE = "org.glassfish.servlet.habitat";
    private static final String JNDI_APP_LOOKUP = "java:app/hk2/ServiceLocator";
    
    private static final String SERVLET_CONTEXT_LOCATOR = "ServletContextLocator=";
    private static final String JNDI_APP_LOCATOR = "JndiAppLocator=";
    
    private String getJndiAppLocatorName() {
        
        try {
          Context context = new InitialContext();
          
          ServiceLocator retVal = (ServiceLocator) context.lookup(JNDI_APP_LOOKUP);
          
          return retVal.getName();
        }
        catch (NamingException ne) {
            return null;
        }
    }

    /**
     * Just prints out the value of the ServiceLocator getName
     */
    @Override
    public void doGet(HttpServletRequest request,
            HttpServletResponse response)
        throws IOException, ServletException {
        ServletContext context = getServletContext();
        
        ServiceLocator locator = (ServiceLocator) context.getAttribute(HABITAT_ATTRIBUTE);
        
        String reply1 = SERVLET_CONTEXT_LOCATOR + ((locator == null) ? "null" : locator.getName());
        
        String jndiAppLocatorName = getJndiAppLocatorName();
        String reply2 = JNDI_APP_LOCATOR + ((jndiAppLocatorName == null) ? "null" : jndiAppLocatorName);

        response.setContentType("text/html");
        PrintWriter writer = response.getWriter();
        
        writer.println("<html>");
        writer.println("<head>");
        writer.println("<title>Iso2 WebApp</title>");
        writer.println("</head>");
        writer.println("<body>");

        writer.println(reply1);
        writer.println(reply2);

        writer.println("</body>");
        writer.println("</html>");
    }
}
