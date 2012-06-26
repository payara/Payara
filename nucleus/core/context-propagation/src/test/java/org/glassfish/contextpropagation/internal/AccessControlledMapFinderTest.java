package org.glassfish.contextpropagation.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.glassfish.contextpropagation.adaptors.BootstrapUtils;
import org.glassfish.contextpropagation.internal.Utils.AccessControlledMapFinder;
import org.glassfish.contextpropagation.wireadapters.glassfish.DefaultWireAdapter;
import org.junit.Before;
import org.junit.Test;

public class AccessControlledMapFinderTest {
  static AccessControlledMapFinder mapFinder = new AccessControlledMapFinder();
  
  @Before
  public void setup() {
    BootstrapUtils.bootstrap(new DefaultWireAdapter());
    mapFinder.getMapAndCreateIfNeeded();
  }
  
  @Test
  public void testGetMapIfItExistsButDoesnt() {
    BootstrapUtils.bootstrap(new DefaultWireAdapter());
    assertNull(mapFinder.getMapIfItExists());
  }
  
  @Test
  public void testGetMapIfItExistsWhenItDoes() {
    assertNotNull(mapFinder.getMapIfItExists());
  }
  
  @Test
  public void testCreateMapIfItExistsButDoesnt() {
    BootstrapUtils.bootstrap(new DefaultWireAdapter());
    assertNull(mapFinder.getMapIfItExists());
    assertNotNull(mapFinder.getMapAndCreateIfNeeded());
  }
  
  @Test
  public void testCreateMapIfItExistsWhenItDoes() {
    assertEquals(mapFinder.getMapIfItExists(), mapFinder.getMapAndCreateIfNeeded());
  }  

}
