package org.glassfish.weld;

import org.jboss.weld.bootstrap.api.SingletonProvider;
import org.junit.Test;
import static junit.framework.Assert.*;

import java.lang.reflect.Field;

/**
 * @author <a href="mailto:j.j.snyder@oracle.com">JJ Snyder</a>
 */
public class WeldActivatorTest {
    @Test
    public void testAll() throws Exception {
        SingletonProvider aclSingletonProvider = new ACLSingletonProvider();

        Field instanceField = SingletonProvider.class.getDeclaredField("INSTANCE");
        instanceField.setAccessible(true);
        assertNull(instanceField.get(aclSingletonProvider));

        WeldActivator weldActivator = new WeldActivator();
        weldActivator.start( null );

        assertTrue(instanceField.get(aclSingletonProvider) instanceof ACLSingletonProvider);

        weldActivator.stop(null);
        assertNull(instanceField.get(aclSingletonProvider));

    }
}