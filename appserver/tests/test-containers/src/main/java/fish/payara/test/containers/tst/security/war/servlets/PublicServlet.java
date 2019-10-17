/** Copyright Payara Services Limited **/
package fish.payara.test.containers.tst.security.war.servlets;

import javax.servlet.annotation.WebServlet;

/**
 * @author David Matejcek
 */
@WebServlet(urlPatterns = { "/public" })
public class PublicServlet extends PrincipalInfoPrintingServlet {

    private static final long serialVersionUID = 1L;
}
