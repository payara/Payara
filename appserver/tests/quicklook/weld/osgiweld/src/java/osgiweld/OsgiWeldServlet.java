/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010 Oracle and/or its affiliates. All rights reserved.
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

package osgiweld;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Test integrity of OSGi Weld Module.
 * 
 * @author Santiago.PericasGeertsen@sun.com
 */
public class OsgiWeldServlet extends HttpServlet {

    private static List<Attributes.Name> ATTRS =
            Arrays.asList(new Attributes.Name("Export-Package"),
                          new Attributes.Name("Import-Package"));
                          //new Attributes.Name("Private-Package")); 
            //From Weld 1.1, Private-Package is not part of the OSGi headers

    protected void processRequest(HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException
    {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();

        String result = "OK";
        try {
            String gfhome = System.getProperty("com.sun.aas.instanceRoot");
            // Soft test, ignore if can't find module
            if (gfhome != null) {
                String jarFile = gfhome + File.separator + ".."
                        + File.separator + ".." + File.separator
                        + "modules" + File.separator + "weld-osgi-bundle.jar";
                //System.out.println("Weld Osgi module = " + jarFile);
                JarFile jar = new JarFile(jarFile);
                Manifest manifest = jar.getManifest();

                String command = request.getParameter("command");
                //System.out.println("Command: " + command);
                if (command.equals("manifest")) {
                    // Make sure all manifest attrs are there
                    Set<Object> keys = manifest.getMainAttributes().keySet();
                    //System.out.println("Keys: " + keys);
                    if (!keys.containsAll(ATTRS) || !checkBundleSymbolicName(manifest.getMainAttributes())) {
                        result = "ERROR";
                    }
                } else if (command.equals("exports")) {
                    // Make sure package exports are present and return them
                    String exportedValues = manifest.getMainAttributes().getValue(new Attributes.Name("Export-Package"));
                    //System.out.println("Exported Values: " + exportedValues);
                    if (null != exportedValues) {
                        result = exportedValues;
                    } else {
                        result = "ERROR";
                    }
                } else if (command.equals("imports")) {
                    //Make sure package imports are present and return them
                    String importedValues = manifest.getMainAttributes().getValue(new Attributes.Name("Import-Package"));
                    //System.out.println("Imported Values: " + importedValues);
                    if (null != importedValues) {
                        result = importedValues;
                    } else {
                        result = "ERROR";
                    }
                }
            } else {
                System.out.println("Unable to find Weld module");
            }
        } catch (Exception e) {
            result = "ERROR";
        }

        out.println(result);
        out.close();
    }

    private boolean checkBundleSymbolicName(Attributes attrs){
        String name = attrs.getValue("Bundle-SymbolicName");
        System.out.println("Bundle-SymbolicName:"+ name);
        return name.equals("org.jboss.weld.osgi-bundle");
    }

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
