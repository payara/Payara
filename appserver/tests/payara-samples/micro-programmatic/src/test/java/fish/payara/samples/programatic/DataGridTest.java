package fish.payara.samples.programatic;

import com.hazelcast.core.*;
import com.hazelcast.map.*;

import javax.servlet.annotation.*;
import javax.servlet.http.*;
import java.io.*;
import java.util.*;

@WebServlet(urlPatterns = "/cache-provider")
public class DataGridTest extends HttpServlet {
  @Override
  public void doGet(final HttpServletRequest req, final HttpServletResponse resp)
    throws IOException
  {
    HazelcastInstance h1 = Hazelcast.newHazelcastInstance(null);
    IMap<Integer, String> map1 = h1.getMap("testmap");
    for (int i = 0; i < 10; i++)
    {
      map1.put(i, "value" + i);
    }
    resp.setStatus(200);
    resp.setContentType("text/plain");
    resp.getOutputStream().println(
      "A distributed cache containing " + map1.size() +
        " entries has been created");
  }
}
