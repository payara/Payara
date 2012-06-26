package org.glassfish.contextpropagation;

import static org.junit.Assert.assertEquals;

import java.util.EnumSet;

import org.glassfish.contextpropagation.adaptors.BootstrapUtils;
import org.glassfish.contextpropagation.spi.ContextMapHelper;
import org.glassfish.contextpropagation.wireadapters.glassfish.DefaultWireAdapter;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class ContextViewTest {

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    BootstrapUtils.bootstrap(new DefaultWireAdapter());
  }

  @Before
  public void setUp() throws Exception {
  }

  private static class MyViewBasedContext implements ViewCapable {
    private View view;
    EnumSet<PropagationMode> propModes = PropagationMode.defaultSet();

    private MyViewBasedContext(View aView) {
      view = aView;
    }

    public void setFoo(String foo) {
      view.put("foo", foo, propModes);
    }

    public String getFoo() { 
      return view.get("foo"); 
    };

    public void setLongValue(long value) {
      view.put("long value", value, propModes);
    }

    public long getLongValue() { 
      return (Long) view.get("long value"); 
    }

  }

  @Test
  public void testContextViewExample() throws InsufficientCredentialException {
    ContextViewFactory factory = new ContextViewFactory() {

      @Override
      public ViewCapable createInstance(final View view) {
        return new MyViewBasedContext(view) ;
      }

      @Override
      public EnumSet<PropagationMode> getPropagationModes() {
        return PropagationMode.defaultSet();
      }
    };

    // Define prefix and register factory -- done only once during server startup phase
    String prefix = "my.prefix";
    ContextMapHelper.registerContextFactoryForPrefixNamed(prefix, factory);

    // Get a ContextMap
    ContextMap wcMap = ContextMapHelper.getScopeAwareContextMap();

    // Since this is a new ContextMap, get will create the vbContext with the registered factory
    MyViewBasedContext mvbContext = wcMap.createViewCapable(prefix);
    mvbContext.setFoo("foo value");
    assertEquals("foo value", mvbContext.getFoo());
    mvbContext.setLongValue(1);
    assertEquals(1L, mvbContext.getLongValue());
  }

}
