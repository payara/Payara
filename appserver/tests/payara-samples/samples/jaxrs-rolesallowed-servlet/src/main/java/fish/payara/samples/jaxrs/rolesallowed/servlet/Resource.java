/** Copyright Payara Services Limited **/

package fish.payara.samples.jaxrs.rolesallowed.servlet;

import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

@Path("/resource")
@Produces(TEXT_PLAIN)
public class Resource {

    @GET
    @Path("hi")
    @RolesAllowed("a")
    public String hi() {
       return "hi!";
    }


}
