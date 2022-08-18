package fish.payara.datagrid;

import fish.payara.cluster.*;

import javax.enterprise.context.*;
import java.io.*;
import java.util.*;

@Clustered
@ApplicationScoped
public class DataGrid implements Serializable
{
  private Map<String, String> map1 = new HashMap<>();

  public void addValue (String key, String value)
  {
    System.out.println (">>>>> DataGrid.addValue(): Adding " + key + "->" + value);
    map1.put(key, value);
    System.out.println (">>>>> DataGrid.addValue(): Have added " + key + "->" + map1.get(key));
  }

  public String getValue (String key)
  {
    System.out.println (">>>>> DataGrid.getValue(): Getting " + key + "->" + map1.get(key));
    return map1.get(key);
  }
}
