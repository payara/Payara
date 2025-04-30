package fish.payara.samples.programatic;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

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
