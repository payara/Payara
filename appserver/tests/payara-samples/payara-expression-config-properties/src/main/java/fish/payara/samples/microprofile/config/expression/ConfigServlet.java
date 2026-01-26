/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2020] Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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

package fish.payara.samples.microprofile.config.expression;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Map;

/**
 * @author Andrew Pielage <andrew.pielage@payara.fish>
 */
@WebServlet("/ConfigServlet")
public class ConfigServlet extends HttpServlet {

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request  servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException      if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            updateEnv("Dibbles", "${Dabbles}");
            updateEnv("Bibbly", "Bibbles");
        } catch (ReflectiveOperationException ex) {
            throw new ServletException(ex);
        }
        Config config = ConfigProvider.getConfig();
        System.setProperty("fish.payara.examples.config.sources", "Tiddles!");
        System.setProperty("Dabbles", "Dobbles");
        System.setProperty("Bobbly", "Bobbles");

        response.getWriter().println("Normal Notation: " + config.getOptionalValue("wibbles", String.class).orElse(null) +
                "\n" + "Substitution Notation: " + config.getOptionalValue("${ALIAS=wibbles}", String.class).orElse(null) +
                "\n" + "Password Alias from File: " + config.getOptionalValue("fish.payara.examples.expression.password", String.class).orElse(null) +
                "\n" + "System Property Alias from File: " + config.getOptionalValue("fish.payara.examples.expression.system", String.class).orElse(null) +
                "\n" + "Environment Variable Alias referencing System Property Alias from File: " + config.getOptionalValue("fish.payara.examples.expression.recurse", String.class).orElse(null) +
                "\n" + "Environment Variable Alias and System Property Alias from File (same property): " + config.getOptionalValue("fish.payara.examples.expression.multiple", String.class).orElse(null));
    }

    @SuppressWarnings({"unchecked"})
    public static void updateEnv(String name, String val) throws ReflectiveOperationException {
        Map<String, String> env = System.getenv();
        Field field = env.getClass().getDeclaredField("m");
        field.setAccessible(true);
        ((Map<String, String>) field.get(env)).put(name, val);
    }
}
