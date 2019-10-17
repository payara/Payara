/** Copyright Payara Services Limited **/
package fish.payara.test.containers.tst.security.war.servlets;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Arjan Tijms
 * @author David Matejcek
 */
abstract class PrincipalInfoPrintingServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    public static final String[] ROLES = {"payara-role-CN", "payara-role-email", "payara-role-another"};

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        final StringBuilder output = new StringBuilder();
        output.append("principal=").append(request.getUserPrincipal()).append('\n');
        for (String role : ROLES) {
            output.append("request.isUserInRole(" + role + ")=").append(request.isUserInRole(role)).append('\n');
        }
        response.getWriter().println(output.toString());
    }
}
