package org.glassfish.contextpropagation.internal;

import static org.junit.Assert.assertNotNull;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import mockit.Deencapsulation;

import org.glassfish.contextpropagation.ContextViewFactory;
import org.glassfish.contextpropagation.PropagationMode;
import org.glassfish.contextpropagation.View;
import org.glassfish.contextpropagation.ViewCapable;
import org.glassfish.contextpropagation.adaptors.BootstrapUtils;
import org.glassfish.contextpropagation.adaptors.RecordingLoggerAdapter;
import org.glassfish.contextpropagation.bootstrap.ContextBootstrap;
import org.glassfish.contextpropagation.bootstrap.LoggerAdapter.Level;
import org.glassfish.contextpropagation.bootstrap.LoggerAdapter.MessageID;
import org.glassfish.contextpropagation.wireadapters.glassfish.DefaultWireAdapter;
import org.junit.BeforeClass;
import org.junit.Test;

public class UtilsTest {
  @BeforeClass
  public static void setupClass() {
    BootstrapUtils.bootstrap(new DefaultWireAdapter());
  }

  @Test
  public void testGetScopeAwarePropagator() {
    assertNotNull(Utils.getScopeAwarePropagator());
  }

  @Test
  public void testGetScopeAwareContextMap() {
    assertNotNull(Utils.getScopeAwarePropagator());
  }

  @Test
  public void testRegisterContextFactoryForPrefixNamed() {
    Utils.registerContextFactoryForPrefixNamed("prefix", 
        new ContextViewFactory() {
           @Override
          public EnumSet<PropagationMode> getPropagationModes() {
            return PropagationMode.defaultSet();
          }        
          @Override
          public ViewCapable createInstance(View view) {
            return new ViewCapable() {};
          }
        });
    assertNotNull(Utils.getFactory("prefix"));
  }

  private static final Object CONTEXT_VIEW_FACTORY = new ContextViewFactory() {
    @Override
    public ViewCapable createInstance(View view) {
       return null;
    }
    @Override
    public EnumSet<PropagationMode> getPropagationModes() {
      return null;
    }    
  };
  
  private static MessageID msgID = MessageID.WRITING_KEY; // We need a dummy MessageID
  @Test(expected=IllegalArgumentException.class)
  public void testValidateFactoryRegistrationArgsNullKey() {
    Utils.validateFactoryRegistrationArgs(null, msgID, "context class name", 
        CONTEXT_VIEW_FACTORY, null);
  }
  
  @Test(expected=IllegalArgumentException.class)
  public void testValidateFactoryRegistrationArgsNullContextClassName() {
    Utils.validateFactoryRegistrationArgs("key", msgID, null, 
        CONTEXT_VIEW_FACTORY, null);
  }
  
  @Test(expected=IllegalArgumentException.class)
  public void testValidateFactoryRegistrationArgsNullFactory() {
    Utils.validateFactoryRegistrationArgs("key", msgID, "context class name", 
        null, null);
  }
  
  @Test
  public void testValidateFactoryRegistration() {
    Map<String, ?> map = Collections.emptyMap();
    Utils.validateFactoryRegistrationArgs("key", msgID, "context class name", 
        CONTEXT_VIEW_FACTORY, map);
  }
  @Test(expected=IllegalArgumentException.class)
  public void testValidateFactoryRegistrationNullKey() {
    Map<String, ?> map = Collections.emptyMap();
    Utils.validateFactoryRegistrationArgs(null, msgID, "context class name", 
        CONTEXT_VIEW_FACTORY, map);
  }
  @Test(expected=IllegalArgumentException.class)
  public void testValidateFactoryRegistrationNullClassName() {
    Map<String, ?> map = Collections.emptyMap();
    Utils.validateFactoryRegistrationArgs("key", msgID, null, 
        CONTEXT_VIEW_FACTORY, map);
  }
  @Test(expected=IllegalArgumentException.class)
  public void testValidateFactoryRegistrationNullFactory() {
    Map<String, ?> map = Collections.emptyMap();
    Utils.validateFactoryRegistrationArgs("key", msgID, "context class name", 
        null, map);
  }
  @Test(expected=IllegalArgumentException.class)
  public void testValidateFactoryRegistrationNullMessageID() {
    Map<String, ?> map = Collections.emptyMap();
    Utils.validateFactoryRegistrationArgs("key", null, "context class name", 
        CONTEXT_VIEW_FACTORY, map);
  }
  @Test
  public void testValidateFactoryRegistrationAlreadyRegistered() {
    RecordingLoggerAdapter logger = new RecordingLoggerAdapter();
    Deencapsulation.setField(ContextBootstrap.class, "loggerAdapter", logger);
    Map<String, Object> map = new HashMap<String, Object>();
    Utils.validateFactoryRegistrationArgs("key", msgID, "context class name", 
        CONTEXT_VIEW_FACTORY, map);
    logger.verify(null, null, null, (Object[]) null);
    map.put("context class name", "something");
    Utils.validateFactoryRegistrationArgs("key", msgID, "context class name", 
        CONTEXT_VIEW_FACTORY, map);
    logger.verify(Level.WARN, null, msgID, "context class name", 
        "something", CONTEXT_VIEW_FACTORY);
  }

}
