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
// Portions Copyright [2018] [Payara Foundation and/or its affiliates]

package com.sun.ejb.timers;

import java.io.*;
import java.util.Set;

import javax.servlet.*;
import javax.ejb.*;
import javax.servlet.http.*;

import org.glassfish.ejb.persistent.timer.TimerLocal;
import com.sun.ejb.containers.EJBTimerService;
import com.sun.ejb.containers.TimerPrimaryKey;

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
            Set persistentTimers = timer.findActiveTimersOwnedByThisServer();
            // Non-persistent timers get directly from the service

            EJBTimerService ejbTimerService = null;
            if (EJBTimerService.isPersistentTimerServiceLoaded()) {
                ejbTimerService = EJBTimerService.getPersistentTimerService();
            } else if (EJBTimerService.isNonPersistentTimerServiceLoaded()) {
                ejbTimerService = EJBTimerService.getNonPersistentTimerService();
            }
            if(ejbTimerService == null) {
                throw new IllegalStateException("EJB Timer Service is not available");
            }
            Set<TimerPrimaryKey> nonPersistentTimers = ejbTimerService.getNonPersistentActiveTimerIdsByThisServer();
            int persistentTimerSize = persistentTimers.size();
            int nonPersistentTimerSize = nonPersistentTimers.size();

            out.println("There " + ((persistentTimerSize == 1)? "is " : "are  ")
                    + persistentTimerSize
                    + " active persistent timer" + ((persistentTimerSize == 1)? "" : "s")
                    + " on this container");
            out.println("<br>");
            out.println("There " + ((nonPersistentTimerSize == 1)? "is " : "are  ")
                    + nonPersistentTimerSize
                    + " active non-persistent timer" + ((nonPersistentTimerSize == 1)? "" : "s")
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
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        processRequest(request, response);
    } 

    /** 
    * Handles the HTTP <code>POST</code> method.
    * @param request servlet request
    * @param response servlet response
    */
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        processRequest(request, response);
    }

    /** 
    * Returns a short description of the servlet.
    */
    public String getServletInfo() {
        return "Timer Application Servlet";
    }
    // </editor-fold>
}
