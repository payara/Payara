package fish.payara.tests;

import javax.net.ssl.*;
import javax.servlet.annotation.*;
import javax.servlet.http.*;
import java.io.*;
import java.util.*;

@WebServlet("/ss")
public class ShowCipherSuitesServlet extends HttpServlet
{
  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
    throws IOException
  {
    SSLServerSocketFactory ssf =
      (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
    Map<String, Boolean> ciphers = new TreeMap<>();
    Arrays.asList(ssf.getSupportedCipherSuites())
      .forEach(cs -> ciphers.put(cs, Boolean.FALSE));
    Arrays.asList(ssf.getDefaultCipherSuites())
      .forEach(cs -> ciphers.put(cs, Boolean.TRUE));
    PrintWriter out = response.getWriter();
    out.println("Default\tCipher");
    ciphers.forEach((key, value) ->
    {
      out.print(Boolean.TRUE.equals(value) ? "*" : " ");
      out.print("\t");
      out.println(key);
    });
    out.close();
  }
}