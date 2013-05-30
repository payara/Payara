package org.glassfish.weld;

import org.apache.catalina.core.StandardContext;
import org.apache.jasper.runtime.JspApplicationContextImpl;
import org.easymock.EasyMockSupport;

import static junit.framework.Assert.*;
import static org.easymock.EasyMock.*;

import org.jboss.weld.el.WeldELContextListener;
import org.junit.Test;

import javax.el.ELResolver;
import javax.el.ExpressionFactory;
import javax.enterprise.inject.spi.BeanManager;
import javax.servlet.ServletContextEvent;
import javax.servlet.jsp.JspApplicationContext;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * @author <a href="mailto:j.j.snyder@oracle.com">JJ Snyder</a>
 */
public class WeldContextListenerTest {
    @Test
    public void testcontextInitialized() throws Exception {
        EasyMockSupport mockSupport = new EasyMockSupport();

        ELResolver elResolver = mockSupport.createMock(ELResolver.class);

        ExpressionFactory expressionFactory = mockSupport.createMock(ExpressionFactory.class);
        StandardContext servletContext = new StandardContext();
        servletContext.getServletContext();

        ServletContextEvent servletContextEvent = mockSupport.createMock( ServletContextEvent.class );
        BeanManager beanManager = mockSupport.createMock( BeanManager.class );
        JspApplicationContextImpl jspApplicationContext = new JspApplicationContextImpl(servletContext);

        expect(beanManager.getELResolver()).andReturn(elResolver);
        expect( beanManager.wrapExpressionFactory(isA(ExpressionFactory.class))).andReturn(expressionFactory);

        mockSupport.replayAll();

        WeldContextListener weldContextListener = getWeldContextListener(beanManager, jspApplicationContext);
        weldContextListener.contextInitialized( servletContextEvent );

        assertSame( expressionFactory, jspApplicationContext.getExpressionFactory() );
        validateJspApplicationContext( jspApplicationContext, elResolver );

        mockSupport.verifyAll();
        mockSupport.resetAll();

    }

    @Test
    public void testcontextDestroyed() throws Exception {
        EasyMockSupport mockSupport = new EasyMockSupport();

        BeanManager beanManager = mockSupport.createMock( BeanManager.class );
        mockSupport.replayAll();

        WeldContextListener weldContextListener = getWeldContextListener(beanManager, null);

        Class<?> clazz = LocalWeldContextListener.class.getSuperclass();
        Field beanManagerField = clazz.getDeclaredField("beanManager");
        beanManagerField.setAccessible(true);
        assertNotNull( beanManagerField.get(weldContextListener) );

        weldContextListener.contextDestroyed( null );
        assertNull( beanManagerField.get(weldContextListener) );

        mockSupport.verifyAll();
        mockSupport.resetAll();

    }

    private void validateJspApplicationContext( JspApplicationContextImpl jspApplicationContext,
                                                ELResolver elResolver ) throws Exception {
        Method getELResolversMethod = JspApplicationContextImpl.class.getDeclaredMethod("getELResolvers");
        getELResolversMethod.setAccessible( true );
        Iterator iterator = (Iterator) getELResolversMethod.invoke( jspApplicationContext );
        Object elResover = iterator.next();
        assertSame( elResover, elResolver );
        assertFalse(iterator.hasNext());

        Field listenersField = JspApplicationContextImpl.class.getDeclaredField("listeners");
        listenersField.setAccessible(true);

        ArrayList listeners = ( ArrayList ) listenersField.get(jspApplicationContext);
        assertEquals( 1, listeners.size() );
        assertTrue( listeners.get(0) instanceof WeldELContextListener );
    }

    private WeldContextListener getWeldContextListener(BeanManager beanManager,
                                                       JspApplicationContext jspApplicationContext) throws Exception {
        LocalWeldContextListener localWeldContextListener = new LocalWeldContextListener(jspApplicationContext );
        Class<?> clazz = LocalWeldContextListener.class.getSuperclass();
        Field beanManagerField = clazz.getDeclaredField("beanManager");
        beanManagerField.setAccessible(true);
        beanManagerField.set(localWeldContextListener, beanManager);
        return localWeldContextListener;
    }

    private class LocalWeldContextListener extends WeldContextListener {
        private JspApplicationContext jspApplicationContext;
        public LocalWeldContextListener( JspApplicationContext jspApplicationContext  ) {
            super();
            this.jspApplicationContext = jspApplicationContext;
        }

        protected JspApplicationContext getJspApplicationContext(ServletContextEvent servletContextEvent) {
            return jspApplicationContext;
        }
    }

}
