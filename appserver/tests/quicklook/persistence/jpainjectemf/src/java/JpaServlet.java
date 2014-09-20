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

package myapp;

import javax.persistence.*;
import javax.transaction.*;
import javax.annotation.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;


public class JpaServlet extends HttpServlet {

    @PersistenceUnit(unitName="pu1")
    private EntityManagerFactory emf;
    private EntityManager em;
    private @Resource UserTransaction utx;
    
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/plain");
        PrintWriter out = response.getWriter();
        boolean status = false;

        out.println("@PersistenceUnit EntityManagerFactory=" + emf);
        out.println("@Resource UserTransaction=" + utx);

        em = emf.createEntityManager();
        out.println("createEM  EntityManager=" + em);

        String testcase = request.getParameter("testcase");
        System.out.println("testcase="+testcase);
        if (testcase != null) {
          JpaTest jt = new JpaTest(emf, em, utx, out);

	  try {

	    if ("llinit".equals(testcase)) {
                 status = jt.lazyLoadingInit();
	    } else if ("llfind".equals(testcase)) {
                 status = jt.lazyLoadingByFind(1);
	    } else if ("llquery".equals(testcase)) {
                 status = jt.lazyLoadingByQuery("Carla");
	    } 
            if (status) {
	      out.println(testcase+":pass");
	    } else {
	      out.println(testcase+":fail");
	    }
	  } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println("servlet test failed");
            throw new ServletException(ex);
	  } 
	}
    }
}
