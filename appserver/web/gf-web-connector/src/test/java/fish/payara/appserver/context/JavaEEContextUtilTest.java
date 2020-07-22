/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.appserver.context;

import com.sun.enterprise.util.Utility;
import org.glassfish.internal.api.JavaEEContextUtil;
import org.glassfish.internal.api.JavaEEContextUtil.Context;
import org.glassfish.internal.api.JavaEEContextUtil.Instance;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author lprimak
 */
public class JavaEEContextUtilTest {
    private JavaEEContextUtil ctxUtil;
    @Before
    public void setUp() {
        ctxUtil = new JavaEEContextUtilImpl();
    }

    @Test
    public void empty() {
        Instance empty = ctxUtil.empty();
        assertTrue(empty.isEmpty());
        assertTrue(empty.isLoaded());
        assertNull(empty.getInstanceComponentId());
        ClassLoader cl = Utility.getClassLoader();
        try (Context ctx = empty.setApplicationClassLoader()) {
            assertEquals("inside empty context", Utility.getClassLoader(), cl);
        }
        assertEquals("outside empty context", Utility.getClassLoader(), cl);
    }
}
