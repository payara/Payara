package org.glassfish.contextpropagation.bootstrap;

import static org.junit.Assert.assertNotNull;
import mockit.Deencapsulation;

import org.glassfish.contextpropagation.spi.ContextMapHelper;
import org.glassfish.tests.utils.ConfigApiTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.jvnet.hk2.annotations.Contract;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.Habitat;
import org.jvnet.hk2.config.DomDocument;
import org.jvnet.hk2.junit.Hk2Runner;

@RunWith(Hk2Runner.class)
public class BootstrapTest extends ConfigApiTest {

  /*@Inject
  static RandomContract rci;
  @Inject 
  static DependencyProvider dp;*/
  
  @Test
  public void test() {
    //getHabitat().addIndex(new ExistingSingletonInhabitant<DependencyProvider>(new DependencyProviderImpl()), 
    //    DependencyProvider.class.getName(), null);
    //System.out.println("dp: " + getHabitat().getComponent(DependencyProvider.class));
    //assertNotNull(rci); -- fails on command line
    //assertNotNull(dp);
    assertNotNull(ContextMapHelper.getScopeAwareContextMap());
    Object o = Deencapsulation.getField(ContextBootstrap.class, "dependencyProvider"); 
    System.out.println("DependencyProvider: " + o);
    assertNotNull(o);
    assertNotNull(ContextBootstrap.getGuid());
  }
  
  @Service
  public static class RandomService implements RandomContract {
      public int add(int i, int j) {
          return i+j;
      }
  }
  
  @Contract
  public interface RandomContract {

      /**
       * Adds the two paramters and return the result
       * @param i the first element to add
       * @param j the second
       * @return the addition of i and j
       */
      int add(int i, int j);
  }
  
  @Override
  public DomDocument<?> getDocument(Habitat habitat) {
    return null;
  }
  
}
