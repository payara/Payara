package fish.payara.datagrid;

import com.hazelcast.core.*;
import com.hazelcast.map.*;

import javax.inject.*;
import javax.ws.rs.*;
import javax.ws.rs.core.*;

@Path("/api")
@Produces("text/plain")
public class DataGridResource
{
  private static final String RESPONSE = "Have got distributed cache with %d entries";
  @Inject
  private DataGrid dataGrid;

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
    if (map1.size() != 10)
      throw new WebApplicationException(Response.Status.NOT_FOUND);
    return Response.ok().entity(String.format(RESPONSE, map1.size())).build();
  }

  @GET
  @Path("get")
  public Response getCachedValue (@QueryParam("key") String key)
  {
    return Response.ok().entity(dataGrid.getValue(key)).build();
  }

  @PUT
  @Path("add")
  public void addCachedValue (@QueryParam("key") String key, @QueryParam("value") String value)
  {
    dataGrid.addValue(key, value);
    System.out.println (">>>>> DataGridRessource.addCachedValue(): Have added " + key + "->" + dataGrid.getValue(key));
  }
}
