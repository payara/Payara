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

/*
 * CustomerDetails.java
 *
 * Created on March 17, 2008, 1:00 AM
 */

package web;

import java.io.*;
import java.net.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Resource;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.transaction.UserTransaction;
import persistence.*;

/**
 *
 * @author adminuser
 * @version
 */
@PersistenceContext(name = "persistence/LogicalName", unitName = "webappPU")
public class CustomerDetails extends HttpServlet {

    @Resource
    private UserTransaction utx;
    
    /** Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
     * @param request servlet request
     * @param response servlet response
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        // TODO output your page here
        out.println("<html>");
        out.println("<head>");
        out.println("<title>Servlet CustomerDetails</title>");
        out.println("</head>");
        out.println("<body>");
        out.println("<h1>Servlet CustomerDetails at " + request.getContextPath () + "</h1>");
        
        out.println("<h2>Search Customer Information</h2>");
        out.println("<p>Pl. select from 1,2,3,4,5 as a customer number</p>");
        String customerNr = request.getParameter("customer_nr");
        if((customerNr != null) && !(customerNr.equals(""))) {

            WebCustomer customer = findByID(new Integer(customerNr));
            if(customer != null){
                out.println("Customer's info for nr. " + customerNr + ": " + customer.getCustname());
            }else{
                out.println("Customer not found.");
            }
        }
        out.println("<form>");
        out.println("Customer number: <input type='text' name='customer_nr' />");
        out.println("<input type=submit value=Select />");
        out.println("</form>");
        
        
        out.println("</body>");
        out.println("</html>");
        
        out.close();
    }
    
    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /** Handles the HTTP <code>GET</code> method.
     * @param request servlet request
     * @param response servlet response
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        processRequest(request, response);
    }
    
    /** Handles the HTTP <code>POST</code> method.
     * @param request servlet request
     * @param response servlet response
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        processRequest(request, response);
    }
    
    /** Returns a short description of the servlet.
     */
    public String getServletInfo() {
        return "Short description";
    }
    // </editor-fold>

    protected WebCustomer findByID(Integer customerNr) {
        WebCustomer customer=null;
        try {
            Context ctx = (Context) new InitialContext().lookup("java:comp/env");
            utx.begin();
            EntityManager em =  (EntityManager) ctx.lookup("persistence/LogicalName");
            customer = em.find(WebCustomer.class, customerNr);
            utx.commit();
            
            
            // TODO:
            // em.persist(object);    utx.commit();
        } catch(Exception e) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE,"exception caught", e);
            throw new RuntimeException(e);
        }
        
        return customer;
    }
    
    
}
