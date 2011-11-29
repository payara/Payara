/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.tests.paas.basicjpa;

import java.io.IOException;
import java.io.PrintWriter;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
//import javax.management.*;
import static javax.persistence.CascadeType.ALL;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import static javax.persistence.TemporalType.TIMESTAMP;
import javax.persistence.Transient;
import javax.persistence.EntityManager;

import javax.persistence.EntityManagerFactory;

import javax.persistence.EntityTransaction;

import javax.persistence.Persistence;




import java.util.Iterator;

import java.util.List;
import javax.annotation.Resource;
import javax.persistence.Query;
import javax.transaction.UserTransaction;

/**
 *
 * @author ishan.vishnoi@java.net
 */
public class BasicJPAPaaSServlet extends HttpServlet {

    @PersistenceUnit(unitName = "BasicJPAPU")
    private EntityManagerFactory emf;
    @Resource
    UserTransaction utx;

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        Animal firstAnimal = new Animal();
        firstAnimal.setName("Shera");
        firstAnimal.setCageNumber("A1");
        firstAnimal.setID(1);
        firstAnimal.setSpecies("Lion");
        firstAnimal.setYearOfBirth("2001");
        Boolean x = addAnimal(firstAnimal);
        Animal secondAnimal = new Animal();
        secondAnimal.setName("Bhola");
        secondAnimal.setCageNumber("A2");
        secondAnimal.setID(2);
        secondAnimal.setSpecies("Bear");
        secondAnimal.setYearOfBirth("2004");
        x = addAnimal(secondAnimal);
        Animal thirdAnimal = new Animal();
        thirdAnimal.setName("Ringa");
        thirdAnimal.setCageNumber("A3");
        thirdAnimal.setID(3);
        thirdAnimal.setSpecies("Rhino");
        thirdAnimal.setYearOfBirth("2007");
        x = addAnimal(thirdAnimal);

        try {
            out.println("<html>");
            out.println("<head>");
            out.println("<link rel='stylesheet' type='text/css' href='newcss.css' />");
            out.println("<title>Servlet NewServlet</title>");
            out.println("</head>");
            out.println("<body>");
            out.println("Here is a list of animals in the zoo.");

            List dir = sortByName();
            Iterator dirIterator = dir.iterator();
            out.println("<table border='1'>");

            while (dirIterator.hasNext()) {
                out.println("<tr>");
                Animal animal = (Animal) dirIterator.next();

                out.println("<td> id:" + animal.getID() + "</td>");
                out.println("<td> name:" + animal.getName() + "</td>");
                out.println("<td> species:" + animal.getSpecies() + "</td>");
                out.println("<td> cage_number:" + animal.getCageNumber() + "</td>");
                out.println("<td> year_of_birth:" + animal.getYearOfBirth() + "</td>");
                out.println("</tr>");
            }

            out.println("</table>");

            out.println("<br><a href='index.jsp'>Back</a>");
            out.println("</body>");
            out.println("</html>");
        } finally {
            out.close();
        }
    }

    public List sortByName() {
        EntityManager em = emf.createEntityManager();
        Query q = em.createQuery("SELECT x FROM Animal x order by x.name");
        List results = q.getResultList();
        return (results);
    }

    public boolean addAnimal(Animal animal) {
        EntityManager em = emf.createEntityManager();
        try {
            utx.begin();
            em.persist(animal);
            utx.commit();
        } finally {
            em.close();
            return false;
        }
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
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
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>
}
