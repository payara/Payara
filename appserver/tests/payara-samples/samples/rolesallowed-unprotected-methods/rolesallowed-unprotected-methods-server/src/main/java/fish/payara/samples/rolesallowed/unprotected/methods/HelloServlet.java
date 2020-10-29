package fish.payara.samples.rolesallowed.unprotected.methods;

import java.io.IOException;
import java.io.PrintWriter;

import javax.ejb.EJB;
import javax.ejb.EJBAccessException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/sayhello")
public class HelloServlet extends HttpServlet {

    @EJB
    private HelloServiceLocal helloServiceBean;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        sayHello(response);
    }

    private void sayHello(HttpServletResponse response) throws IOException {
        response.setContentType("text/html;charset=UTF-8");

        try (PrintWriter out = response.getWriter()) {
            out.println("<html>");
            out.println("<head>");
            out.println("<title>Hello Servlet</title>");
            out.println("</head>");
            out.println("<body>");
            out.println("<h1>" + helloServiceBean.sayHello() + "</h1>");

            try {
                // Should fail!
                out.println("<h1>" + helloServiceBean.secureSayHello() + "</h1>");
            } catch (EJBAccessException ejbAccessException) {
                out.println("<h1>Managed to get access!</h1>");
            }

            out.println("</body>");
            out.println("</html>");
            out.println();
        }
    }

}
