package fish.payara.datagrid;

import com.hazelcast.core.*;
import com.hazelcast.map.*;

import javax.ws.rs.*;
import javax.ws.rs.core.*;

@Path("/api")
@Produces("text/plain")
public class DataGridResource
{
  private static final String RESPONSE = "Have got distributed cache with %d entries";

  @GET
  @Path("produce")
  public Response produceDistributedCache()
  {
    HazelcastInstance h1 = Hazelcast.newHazelcastInstance(null);
    IMap<Integer, String> map1 = h1.getMap("testmap");
    for (int i = 0; i < 10; i++)
      map1.put(i, "value" + i);
    return Response.ok().entity(String.format(RESPONSE, map1.size())).build();
  }

  @GET
  @Path("consume")
  public Response consumeDistributedCache()
  {
    HazelcastInstance h1 = Hazelcast.newHazelcastInstance(null);
    IMap<Integer, String> map1 = h1.getMap("testmap");
    return Response.ok().entity(String.format(RESPONSE, map1.size())).build();
  }
}
