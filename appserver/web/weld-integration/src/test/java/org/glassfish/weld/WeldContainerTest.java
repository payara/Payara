package org.glassfish.weld;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;

/**
 * @author <a href="mailto:j.j.snyder@oracle.com">JJ Snyder</a>
 */
public class WeldContainerTest {
    @Test
    public void testgetDeployer() throws Exception {
        WeldContainer weldContainer = new WeldContainer();
        assertEquals( WeldDeployer.class, weldContainer.getDeployer() );
    }

    @Test
    public void testgetName() throws Exception {
        WeldContainer weldContainer = new WeldContainer();
        assertEquals( "Weld", weldContainer.getName() );
    }
}
