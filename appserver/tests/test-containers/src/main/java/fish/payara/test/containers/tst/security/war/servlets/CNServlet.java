/** Copyright Payara Services Limited **/
package fish.payara.test.containers.tst.security.war.servlets;

import javax.servlet.annotation.WebServlet;

/**
 * @author Arjan Tijms
 * @author David Matejcek
 */
@WebServlet(urlPatterns = { "/cn" })
public class CNServlet extends PrincipalInfoPrintingServlet {

    private static final long serialVersionUID = 1L;
}
