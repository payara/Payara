package org.glassfish.contextpropagation;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import mockit.Deencapsulation;

import org.glassfish.contextpropagation.adaptors.BootstrapUtils;
import org.glassfish.contextpropagation.adaptors.MockLoggerAdapter;
import org.glassfish.contextpropagation.adaptors.TestableThread;
import org.glassfish.contextpropagation.internal.ViewImpl;
import org.glassfish.contextpropagation.spi.ContextMapHelper;
import org.glassfish.contextpropagation.wireadapters.glassfish.DefaultWireAdapter;
import org.junit.BeforeClass;
import org.junit.Test;

public class LocationTest {

  @BeforeClass
  public static void setupClass() {
    BootstrapUtils.bootstrap(new DefaultWireAdapter());
  }

  @Test
  public void testGetOrigin() {
    Location location = new Location(new ViewImpl("prefix") {});
    assertEquals("guid", location.getOrigin());
    Deencapsulation.setField(location, "origin", "non-null origin");
    assertEquals("non-null origin", location.getOrigin());
  }

  @Test
  public void testGetLocationId() {
    Location location = new Location(new ViewImpl("prefix") {});
    assertEquals("[0]", location.getLocationId());
  }

  @Test
  public void testContextToPropagateAndContextAdded() {
    Location location = new Location(new ViewImpl("prefix") {});
    Location locationToPropagate = (Location) location.contextToPropagate();
    assertEquals(location, locationToPropagate);
    Location propagatedLocation = new Location(new ViewImpl("prefix") {});
    View view = Deencapsulation.getField(location, "view");
    Deencapsulation.setField(propagatedLocation, "view", view);
    propagatedLocation.contextAdded();
    assertEquals("[0, 1]", propagatedLocation.getLocationId());
  }

  @Test
  public void testMultiplePropagations() throws Exception {
    ContextMap wcMap = ContextMapHelper.getScopeAwareContextMap();    
    Location location = wcMap.getLocation();
    assertEquals("guid", location.getOrigin());
    assertEquals("[0]", location.getLocationId());
    // TODO NOW make sure the location is created if this is the origin of the request.
    for (int i = 1; i <= 3; i++) {
      mimicPropagation("[0, " + i + "]");
    }
  }

  private static void mimicPropagation(final String expectedLocationId)
      throws Exception {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    ContextMapHelper.getScopeAwarePropagator().sendRequest(bos, PropagationMode.SOAP);

    final ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
    new TestableThread() {     
      @Override
      protected void runTest() throws Exception {
        ContextMapHelper.getScopeAwarePropagator().receiveRequest(bis);

        ContextMap wcMap = ContextMapHelper.getScopeAwareContextMap();
        Location location = wcMap.getLocation();
        MockLoggerAdapter.debug(location.getLocationId());       
        assertEquals(expectedLocationId, location.getLocationId());
      }
    }.startJoinAndCheckForFailures();
  }  
}
