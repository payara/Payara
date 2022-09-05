package fish.payara.tests;

import javax.net.ssl.*;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.io.*;
import java.util.*;

@Path("/api")
@Produces("text/plain")
public class ShowCipherSuitesResource
{
  @GET
  public Response showCiphers()
  {
    SSLServerSocketFactory ssf =
      (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
    Map<String, Boolean> ciphers = new TreeMap<>();
    Arrays.asList(ssf.getSupportedCipherSuites())
      .forEach(cs -> ciphers.put(cs, Boolean.FALSE));
    Arrays.asList(ssf.getDefaultCipherSuites())
      .forEach(cs -> ciphers.put(cs, Boolean.TRUE));
    StringBuilder out = new StringBuilder();
    out.append("Default\tCiphers\n---------------\n");
    ciphers.forEach((key, value) ->
    {
      out.append(Boolean.TRUE.equals(value) ? "*" : " ");
      out.append("\t");
      out.append(key);
      out.append("\n");
    });
    return Response.ok().entity(out.toString()).build();
  }
}
