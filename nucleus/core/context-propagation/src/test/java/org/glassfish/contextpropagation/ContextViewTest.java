/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2014 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.contextpropagation;

import static org.junit.Assert.assertEquals;

import java.util.EnumSet;

//import org.glassfish.contextpropagation.adaptors.BootstrapUtils;
import org.glassfish.contextpropagation.spi.ContextMapHelper;
import org.glassfish.contextpropagation.wireadapters.glassfish.DefaultWireAdapter;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class ContextViewTest {
  

//  @BeforeClass
//  public static void setUpBeforeClass() throws Exception {
//    BootstrapUtils.bootstrap(new DefaultWireAdapter());
//  }
//
//  @Before
//  public void setUp() throws Exception {
//  }
//
//  private static class MyViewBasedContext implements ViewCapable {
//    private View view;
//    EnumSet<PropagationMode> propModes = PropagationMode.defaultSet();
//
//    private MyViewBasedContext(View aView) {
//      view = aView;
//    }
//
//    public void setFoo(String foo) {
//      view.put("foo", foo, propModes);
//    }
//
//    public String getFoo() { 
//      return view.get("foo"); 
//    };
//
//    public void setLongValue(long value) {
//      view.put("long value", value, propModes);
//    }
//
//    public long getLongValue() { 
//      return (Long) view.get("long value"); 
//    }
//
//  }
//
//  @Test
//  public void testContextViewExample() throws InsufficientCredentialException {
//    ContextViewFactory factory = new ContextViewFactory() {
//
//      @Override
//      public ViewCapable createInstance(final View view) {
//        return new MyViewBasedContext(view) ;
//      }
//
//      @Override
//      public EnumSet<PropagationMode> getPropagationModes() {
//        return PropagationMode.defaultSet();
//      }
//    };
//
//    // Define prefix and register factory -- done only once during server startup phase
//    String prefix = "my.prefix";
//    ContextMapHelper.registerContextFactoryForPrefixNamed(prefix, factory);
//
//    // Get a ContextMap
//    ContextMap wcMap = ContextMapHelper.getScopeAwareContextMap();
//
//    // Since this is a new ContextMap, get will create the vbContext with the registered factory
//    MyViewBasedContext mvbContext = wcMap.createViewCapable(prefix);
//    mvbContext.setFoo("foo value");
//    assertEquals("foo value", mvbContext.getFoo());
//    mvbContext.setLongValue(1);
//    assertEquals(1L, mvbContext.getLongValue());
//  }

}
