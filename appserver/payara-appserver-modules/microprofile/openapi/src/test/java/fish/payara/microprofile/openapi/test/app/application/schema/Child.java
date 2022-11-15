package fish.payara.microprofile.openapi.test.app.application.schema;

import javax.ws.rs.GET;
import javax.ws.rs.core.Response;

public class Child {

    @GET
    public Response childMethod() {
        return Response.ok().entity("child").build();
    }
}
