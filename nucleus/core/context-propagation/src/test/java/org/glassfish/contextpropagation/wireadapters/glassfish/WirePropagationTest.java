package org.glassfish.contextpropagation.wireadapters.glassfish;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.glassfish.contextpropagation.InsufficientCredentialException;
import org.glassfish.contextpropagation.PropagationMode;
import org.glassfish.contextpropagation.adaptors.BootstrapUtils;
import org.glassfish.contextpropagation.adaptors.MockLoggerAdapter;
import org.glassfish.contextpropagation.internal.Utils;
import org.glassfish.contextpropagation.spi.ContextMapHelper;
import org.glassfish.contextpropagation.spi.ContextMapPropagator;
import org.glassfish.contextpropagation.wireadapters.glassfish.DefaultWireAdapter;
import org.junit.BeforeClass;
import org.junit.Test;

public class WirePropagationTest {
  
  @BeforeClass
  public static void setup() throws InsufficientCredentialException {
    BootstrapUtils.bootstrap(new DefaultWireAdapter());
    BootstrapUtils.populateMap();
  }

  @Test
  public void testPropagateOverWire() throws IOException {
    ContextMapPropagator wcPropagator = ContextMapHelper.getScopeAwarePropagator();
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    wcPropagator.sendRequest(baos, PropagationMode.SOAP);
    MockLoggerAdapter.debug(Utils.toString(baos.toByteArray()));
  }
}
