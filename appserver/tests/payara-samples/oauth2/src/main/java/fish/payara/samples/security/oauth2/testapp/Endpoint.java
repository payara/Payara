/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *  Copyright (c) [2018-2021] Payara Foundation and/or its affiliates. All rights reserved.
 * 
 *  The contents of this file are subject to the terms of either the GNU
 *  General Public License Version 2 only ("GPL") or the Common Development
 *  and Distribution License("CDDL") (collectively, the "License").  You
 *  may not use this file except in compliance with the License.  You can
 *  obtain a copy of the License at
 *  https://github.com/payara/Payara/blob/main/LICENSE.txt
 *  See the License for the specific
 *  language governing permissions and limitations under the License.
 * 
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 * 
 *  GPL Classpath Exception:
 *  The Payara Foundation designates this particular file as subject to the "Classpath"
 *  exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *  file that accompanied this code.
 * 
 *  Modifications:
 *  If applicable, add the following below the License Header, with the fields
 *  enclosed by brackets [] replaced by your own identifying information:
 *  "Portions Copyright [year] [name of copyright owner]"
 * 
 *  Contributor(s):
 *  If you wish your version of this file to be governed by only the CDDL or
 *  only the GPL Version 2, indicate your decision by adding "[Contributor]
 *  elects to include this software in this distribution under the [CDDL or GPL
 *  Version 2] license."  If you don't indicate a single choice of license, a
 *  recipient has the option to distribute your version of this file under
 *  either the CDDL, the GPL Version 2 or to extend the choice of license to
 *  its licensees as provided above.  However, if you add GPL Version 2 code
 *  and therefore, elected the GPL Version 2 license, then the option applies
 *  only if the new code is made subject to such option by the copyright
 *  holder.
 */
package fish.payara.samples.security.oauth2.testapp;

import java.io.IOException;

import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 *
 * @author jonathan
 */
@WebServlet("/Endpoint")
public class Endpoint extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        StringBuilder returnURL = new StringBuilder(request.getParameter("redirect_uri"));
        returnURL.append("?&state=").append(request.getParameter("state"));
        returnURL.append("&code=plokmijn");

        if (!"code".equals(request.getParameter("response_type"))) {
            response.sendError(401);
        }
        if (!"qwertyuiop".equals(request.getParameter("client_id"))) {
            response.sendError(401);
        }
        response.sendRedirect(returnURL.toString());
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        boolean grantRight = "authorization_code".equals(request.getParameter("grant_type"));
        boolean codeRight = "plokmijn".equals(request.getParameter("code"));
        boolean clientRight = "qwertyuiop".equals(request.getParameter("client_id"));
        boolean secretRight = "asdfghjklzxcvbnm".equals(request.getParameter("client_secret"));

        JsonObjectBuilder jsonresponse = Json.createObjectBuilder();
        if (grantRight && codeRight && clientRight&& secretRight) {

            
            jsonresponse.add("access_token", "qazwsxedc");
            jsonresponse.add("state", request.getParameter("state"));
            jsonresponse.add("token_type", "bearer");
            String built = jsonresponse.build().toString();
            response.getWriter().write(built);
            response.setHeader("Cache-Control", "no-store");
            response.setHeader("Pragma", "no-cache");

        } else {
            jsonresponse.add("error", "somethingwentwrong");
            String errors = Boolean.toString(grantRight) + Boolean.toString(codeRight) + Boolean.toString(clientRight) + Boolean.toString(secretRight);
            jsonresponse.add("error_desc", errors);
            String built = jsonresponse.build().toString();
            response.getWriter().write(built);
            response.sendError(401);
        }
    }

}
