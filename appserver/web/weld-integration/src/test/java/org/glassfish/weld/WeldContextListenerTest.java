/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

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
