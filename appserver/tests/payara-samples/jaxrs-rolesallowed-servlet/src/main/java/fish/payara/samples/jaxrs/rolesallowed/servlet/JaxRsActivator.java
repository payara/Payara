/** Copyright Payara Services Limited **/
package fish.payara.samples.jaxrs.rolesallowed.servlet;

import jakarta.annotation.security.DeclareRoles;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

@ApplicationScoped
@ApplicationPath("/rest")
@DeclareRoles({ "a", "b" })
public class JaxRsActivator extends Application {

}
