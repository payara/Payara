package org.glassfish.contextpropagation.wireadapters;

import static org.junit.Assert.*;

import org.junit.Test;

public class CatalogTest {

  @Test
  public void testUpdateMeta() {
    byte dot = '.';
    Catalog cat = new Catalog();
    cat.add((short) 5);
    cat.add((short) 0x02FF);
    byte[] bytes = "..xxxx....".getBytes();
    cat.updateCatalogMetadata(bytes);
    assertEquals(dot, bytes[0]);
    assertEquals(dot, bytes[1]);
    assertEquals((byte) 0, bytes[2]);
    assertEquals((byte) 5, bytes[3]);
    assertEquals((byte) 2, bytes[4]);
    assertEquals((byte) 0xFF, bytes[5]);
  }
  
  @Test public void testSetMeta() {
    Catalog cat = new Catalog();
    cat.setMeta(0xABCD1234);
    assertEquals((short) 0xABCD, cat.start);
    assertEquals((short) 0x1234, cat.end);
  }

}
