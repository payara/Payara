package fish.payara.server.internal.tests.api;

import fish.payara.server.internal.tests.facade.*;
import fish.payara.server.internal.tests.model.*;
import jakarta.inject.*;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;

@Path("person")
@Produces(value = MediaType.APPLICATION_JSON)
@Consumes(value = MediaType.APPLICATION_JSON)
public class PersonResource
{
  @Inject
  private PersonFacade personFacade;

  @GET
  public Response findAll()
  {
    return Response.ok().entity(personFacade.findAll()).build();
  }

  @POST
  public Response create (Person person)
  {
    return Response.accepted().entity(personFacade.createPerson(person)).build();
  }
}
