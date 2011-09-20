/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2011 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.naming.impl;

import org.glassfish.api.naming.GlassfishNamingManager;
import org.glassfish.api.naming.JNDIBinding;
import com.sun.enterprise.naming.spi.NamingObjectFactory;
import org.junit.*;
import static org.junit.Assert.*;
import org.glassfish.api.invocation.ComponentInvocation;
import org.glassfish.api.invocation.InvocationException;
import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.api.invocation.InvocationManagerImpl;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.spi.NamingManager;
import java.util.ArrayList;
import java.util.List;

import java.util.Properties;

public class AppTest {
   private static final String INITIAL_CONTEXT_TEST_MODE = "com.sun.enterprise.naming.TestMode";

    @Test public void testCreateNewInitialContext() {
        try {
            newInitialContext();
            assert (true);
        } catch (Exception ex) {
            ex.printStackTrace();
            assert (false);
        }
    }

    @Test public void testBind() {
        GlassfishNamingManager nm = null;
        try {
            InvocationManager im = new InvocationManagerImpl();
            InitialContext ic = newInitialContext();
            nm = new GlassfishNamingManagerImpl(ic);
            nm.publishObject("foo", "Hello: foo", false);
            System.out.println("**lookup() ==> " + ic.lookup("foo"));
            assert (true);
        } catch (Exception ex) {
            ex.printStackTrace();
            assert (false);
        } finally {
            try {
                nm.unpublishObject("foo");
            } catch (Exception ex) {

            }
        }
    }

    @Test(expected = Exception.class) public void testEmptySubContext() throws Exception {
        String name1 = "rmi://a//b/c/d/name1";
        Context ctx = newInitialContext();
        ctx.bind(name1, "Name1");
        String val = (String) ctx.lookup(name1);
    }


    @Test public void testCachingNamingObjectFactory() {
        GlassfishNamingManagerImpl nm = null;
        try {
            InvocationManager im = new InvocationManagerImpl();
            InitialContext ic = newInitialContext();
            nm = new GlassfishNamingManagerImpl(ic);
            nm.publishObject("foo", "Hello: foo", false);
            System.out.println("**lookup() ==> " + ic.lookup("foo"));

            NamingObjectFactory factory = new NamingObjectFactory() {
                private int counter = 1;

                public boolean isCreateResultCacheable() {
                    return true;
                }

                public Object create(Context ic) {
                    return ("FACTORY_Created: " + counter++);
                }

                public String toString() {
                    return "Huh? ";
                }
            };
            nm.publishObject("bar", factory, false);
            System.out.println("**lookup() ==> " + ic.lookup("bar"));
            System.out.println("**lookup() ==> " + ic.lookup("bar"));
            assert (true);
        } catch (Exception ex) {
            ex.printStackTrace();
            assert (false);
        } finally {
            try {
                nm.unpublishObject("foo");
                nm.unpublishObject("bar");
            } catch (Exception ex) {
		//ignore
            }
        }
    }

    private static class Binding
            implements JNDIBinding {
        String logicalName;
        Object value;

        public Binding(String logicalName, Object value) {
            this.logicalName = "java:comp/env/" + logicalName;
            this.value = value;
        }

        public String getName() {
            return logicalName;
        }

        public String getJndiName() {
            return null;
        }

        public Object getValue() {
            return value;
        }
   } 

    @Test public void testEmptyJavaCompEnv() {
        GlassfishNamingManagerImpl nm = null;
        InvocationManager im = new InvocationManagerImpl();
        ComponentInvocation inv = null;
        try {
            InitialContext ic = newInitialContext();
            triggerLoadingNamedProxies(ic);
            nm = new GlassfishNamingManagerImpl(ic);
            nm.setInvocationManager(im);

            inv = new ComponentInvocation("comp1",
                    ComponentInvocation.ComponentInvocationType.EJB_INVOCATION,
                    null, null, null);
            im.preInvoke(inv);

            nm.bindToComponentNamespace("app1", "mod1", "comp1", false, new ArrayList<Binding>());

            Context ctx = (Context) ic.lookup("java:comp/env");
            System.out.println("**lookup(java:comp/env) ==> " + ctx);
        } catch (javax.naming.NamingException nnfEx) {
            nnfEx.printStackTrace();
            assert (false);
        }
    }

    @Test public void testNonCachingNamingObjectFactory() {
        GlassfishNamingManagerImpl nm = null;
        InvocationManager im = new InvocationManagerImpl();
        ComponentInvocation inv = null;
        try {
            InitialContext ic = newInitialContext();
            triggerLoadingNamedProxies(ic);
            nm = new GlassfishNamingManagerImpl(ic);
            nm.setInvocationManager(im);

            List<Binding> bindings =
                    new ArrayList<Binding>();

            NamingObjectFactory intFactory = new NamingObjectFactory() {
                private int counter = 1;

                public boolean isCreateResultCacheable() {
                    return false;
                }

                public Object create(Context ic) {
                    return new Integer(++counter);
                }

                public String toString() {
                    return "Huh? ";
                }
            };
            bindings.add(new Binding("conf/area", intFactory));
            bindings.add(new Binding("conf/location", "Santa Clara"));

            nm.bindToComponentNamespace("app1", "mod1", "comp1", false, bindings);

            inv = new ComponentInvocation("comp1",
                    ComponentInvocation.ComponentInvocationType.EJB_INVOCATION,
                    null, null, null);
            im.preInvoke(inv);
           
            System.out.println("**lookup(java:comp/env/conf/area) ==> " + ic.lookup("java:comp/env/conf/area"));
            System.out.println("**lookup(java:comp/env/conf/location) ==> " + ic.lookup("java:comp/env/conf/location"));

            NamingObjectFactory floatFactory = new NamingObjectFactory() {
                private int counter = 1;

                public boolean isCreateResultCacheable() {
                    return false;
                }

                public Object create(Context ic) {
                    return Float.valueOf(("7" + (++counter)) + "." + 2323);
                }

                public String toString() {
                    return "Huh? ";
                }
            };
            List<Binding> bindings2 =
                    new ArrayList<Binding>();
            bindings2.add(new Binding("conf/area", floatFactory));
            bindings2.add(new Binding("conf/location", "Santa Clara[14]"));

            nm.bindToComponentNamespace("app1", "mod1", "comp2", false, bindings2);

            inv = new ComponentInvocation("comp2",
                    ComponentInvocation.ComponentInvocationType.EJB_INVOCATION,
                    null, null, null);
            im.preInvoke(inv);
            System.out.println("**lookup(java:comp/env/conf/area) ==> " + ic.lookup("java:comp/env/conf/area"));
            System.out.println("**lookup(java:comp/env/conf/location) ==> " + ic.lookup("java:comp/env/conf/location"));

            assert (true);
        } catch (InvocationException inEx) {
            inEx.printStackTrace();
            assert(false);
        } catch (Exception ex) {
            ex.printStackTrace();
            assert (false);
        } finally {
            try {
                im.postInvoke(inv);
                nm.unbindComponentObjects("comp1");
            } catch (InvocationException inEx) {
                
            } catch (Exception ex) {

            }
        }
    }

    /**
     * Performs an ignored test lookup to trigger the initial loading of named proxies.
     * See NamedNamingObjectManager.checkAndLoadProxies, which creates a default 
     * GlassFishNamingManagerImpl.  This is not what we want in this test class; we
     * want our own instance of GlassFishNamingManagerImpl that takes our own
     * InvocationManagerImpl. 
     * GlassFishNamingManagerImpl(InitialContext) calls JavaURLContext.setNamingManager(this)
     * to save the GlassFishNamingManagerImpl into JavaURLContext, so the last call wins.
     * We want to make sure our test GlassFishNamingManagerImpl is instantiated after the 
     * default one.
     */
    private void triggerLoadingNamedProxies(InitialContext ic) {
        try {
            ic.lookup("java:comp/env/to-be-ingored");
        } catch (Exception ignore) {
        }
    }

    private InitialContext newInitialContext() throws NamingException {

	// Create a special InitialContext for test purposes
	// Can't just do a new no-arg InitialContext() since
	// this code runs outside a managed environment and the
	// normal behavior would be to try to contact a server.
	// Instead, create an initialcontext with a special
	// property to tell InitialContext to act as if it's
	// running in the server.
	Properties props = new Properties();
	props.setProperty( SerialContext.INITIAL_CONTEXT_TEST_MODE, "true");
	
	return new InitialContext( props );
    }
}
