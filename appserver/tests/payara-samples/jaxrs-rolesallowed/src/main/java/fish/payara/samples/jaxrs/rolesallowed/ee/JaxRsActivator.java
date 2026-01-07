/** Copyright Payara Services Limited **/
package fish.payara.samples.jaxrs.rolesallowed.ee;

import jakarta.annotation.security.DeclareRoles;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.security.enterprise.authentication.mechanism.http.BasicAuthenticationMechanismDefinition;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

@ApplicationScoped
@ApplicationPath("/rest")
@DeclareRoles({ "a", "b" })
@BasicAuthenticationMechanismDefinition(realmName = "foo-ee")
public class JaxRsActivator extends Application {

}
