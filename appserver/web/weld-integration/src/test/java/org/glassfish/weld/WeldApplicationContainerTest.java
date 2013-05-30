package org.glassfish.weld;

import org.glassfish.api.deployment.ApplicationContext;
import org.junit.Test;
import static junit.framework.Assert.*;

/**
 * @author <a href="mailto:j.j.snyder@oracle.com">JJ Snyder</a>
 */
public class WeldApplicationContainerTest {
    @Test
    public void testAll() throws Exception {
        WeldApplicationContainer weldApplicationContainer = new WeldApplicationContainer();
        assertNull( weldApplicationContainer.getDescriptor() );
        assertTrue( weldApplicationContainer.start( null ) );
        assertTrue( weldApplicationContainer.stop( null ) );
        assertFalse( weldApplicationContainer.suspend() );
        assertFalse( weldApplicationContainer.resume() );
        assertNull( weldApplicationContainer.getClassLoader() );
    }
}