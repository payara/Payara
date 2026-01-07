/** Copyright Payara Services Limited **/

package fish.payara.samples.jaccperapp;

import java.io.IOException;
import java.security.Permission;

import jakarta.annotation.security.DeclareRoles;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.HttpConstraint;
import jakarta.servlet.annotation.ServletSecurity;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Protected test Servlet printinhg out authentication details and the permissions
 * logged by our special test JACC Policy; {@link LoggingTestPolicy}.
 *
 * <p>
 * If the Servlet is accessible and the permission are logged, it's proof that the JACC provider
 * we set in {@link JaccInstaller} is used and works.
 *
 * @author Arjan Tijms
 */
@DeclareRoles({ "a", "b", "c" })
@WebServlet("/protected/servlet")
@ServletSecurity(@HttpConstraint(rolesAllowed = "a"))
public class ProtectedServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        response.getWriter().write("This is a protected servlet \n");

        String webName = null;
        if (request.getUserPrincipal() != null) {
            webName = request.getUserPrincipal().getName();
        }

        response.getWriter().write("web username: " + webName + "\n");

        response.getWriter().write("web user has role \"a\": " + request.isUserInRole("a") + "\n");
        response.getWriter().write("web user has role \"b\": " + request.isUserInRole("b") + "\n");
        response.getWriter().write("web user has role \"c\": " + request.isUserInRole("c") + "\n");

        response.getWriter().write("\nLogged permissions: \n");

        for (Permission permission : LoggingTestPolicy.loggedPermissions) {
            response.getWriter().write(permission + "\n");
        }

        LoggingTestPolicy.loggedPermissions.clear();
    }

}
