package org.glassfish.contextpropagation;

import static org.junit.Assert.assertEquals;

import java.util.EnumSet;

import org.junit.Test;

public class PropagationModeTest {

  @Test
  public void testFromOrdinal() {
    PropagationMode[] modes = PropagationMode.values();
    for (int i = 0; i < modes.length; i++) {
      assertEquals(modes[i], PropagationMode.fromOrdinal(modes[i].ordinal()));
    }
  }

  @Test
  public void testDefaultSet() {
    assertEquals(EnumSet.of(PropagationMode.THREAD, PropagationMode.RMI, 
        PropagationMode.SOAP, PropagationMode.JMS_QUEUE, 
        PropagationMode.MIME_HEADER), PropagationMode.defaultSet());
  }

}
