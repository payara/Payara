/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2012 Oracle and/or its affiliates. All rights reserved.
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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.ejb.timers;

import java.io.*;
import java.net.*;
import java.util.Set;

import javax.servlet.*;
import javax.ejb.*;
import javax.servlet.http.*;

import org.glassfish.ejb.persistent.timer.TimerLocal;
import com.sun.ejb.containers.EJBTimerService;

/**
 *
 * @author mvatkina
 */

public class TimerWelcomeServlet extends HttpServlet {

    @EJB
    private transient TimerLocal timer;

    /** 
    * Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
    * @param request servlet request
    * @param response servlet response
    */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        try {
            out.println("<html>");
            out.println("<head>");
            out.println("<title>Timer Application</title>");  
            out.println("</head>");
            out.println("<body>");
            out.println("<h3>Welcome to Timer Application</h3>");
            out.println("<br>");

            // Persistent timers
            Set persistenttimers = timer.findActiveTimersOwnedByThisServer();
            // Non-persistent timers get directly from the service

            EJBTimerService ejbTimerService = EJBTimerService.getEJBTimerService();
            Set nonpersistenttimers = ejbTimerService.getNonPersistentActiveTimerIdsByThisServer();
            int persistentsize = persistenttimers.size();
            int nonpersistentsize = nonpersistenttimers.size();

            out.println("There " + ((persistentsize == 1)? "is " : "are  ") 
                    + persistentsize
                    + " active persistent timer" + ((persistentsize == 1)? "" : "s")
                    + " on this container");
            out.println("<br>");
            out.println("There " + ((nonpersistentsize == 1)? "is " : "are  ") 
                    + nonpersistentsize
                    + " active non-persistent timer" + ((nonpersistentsize == 1)? "" : "s")
                    + " on this container");
            out.println("<br>");

        }catch(Throwable e){
            out.println("Problem accessing timers... ");
            out.println(e);
            e.printStackTrace();
        }
        finally {
            out.println("</body>");
            out.println("</html>");
            
            out.close();
            out.flush();

        }
    }


    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /** 
    * Handles the HTTP <code>GET</code> method.
    * @param request servlet request
    * @param response servlet response
    */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        processRequest(request, response);
    } 

    /** 
    * Handles the HTTP <code>POST</code> method.
    * @param request servlet request
    * @param response servlet response
    */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        processRequest(request, response);
    }

    /** 
    * Returns a short description of the servlet.
    */
    @Override
    public String getServletInfo() {
        return "Timer Application Servlet";
    }
    // </editor-fold>
}
