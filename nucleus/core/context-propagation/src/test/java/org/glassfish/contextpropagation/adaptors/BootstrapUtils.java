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

package org.glassfish.contextpropagation.adaptors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;

//import mockit.Deencapsulation;

import org.glassfish.contextpropagation.ContextMap;
import org.glassfish.contextpropagation.ContextViewFactory;
import org.glassfish.contextpropagation.InsufficientCredentialException;
import org.glassfish.contextpropagation.PropagationMode;
import org.glassfish.contextpropagation.View;
import org.glassfish.contextpropagation.ViewCapable;
import org.glassfish.contextpropagation.bootstrap.ContextBootstrap;
import org.glassfish.contextpropagation.internal.Utils.ContextMapAdditionalAccessors;
import org.glassfish.contextpropagation.spi.ContextMapHelper;
import org.glassfish.contextpropagation.wireadapters.WireAdapter;

public class BootstrapUtils {
  
  private static class MyViewCapable implements ViewCapable {
    View view;   
    public MyViewCapable(View aView) {
      view = aView;
      view.put(".value", "a value", PropagationMode.defaultSet());
    }
    public String getValue() {
      return view.get(".value");
    }
  }

  public static void populateMap() throws InsufficientCredentialException {
    ContextMap wcMap = ContextMapHelper.getScopeAwareContextMap();
    wcMap.put("true", true, PropagationMode.defaultSet()); 
    wcMap.put("string", "string", PropagationMode.defaultSet());
    wcMap.put("one", 1L, PropagationMode.defaultSet());
    ((ContextMapAdditionalAccessors) wcMap).putAscii("ascii", "ascii", PropagationMode.defaultSet());
    ((ContextMapAdditionalAccessors) wcMap).putSerializable("serializable", new HashSet<String>(Arrays.asList("foo")), PropagationMode.defaultSet());
    wcMap.put("byte", (byte) 'b', PropagationMode.defaultSet());
    
    // View Capable Stuff
    // 1 - Create the factory (assumes that you have already created a ViewCapable class
    ContextViewFactory viewCapableFactory = new ContextViewFactory() {
      @Override
      public ViewCapable createInstance(View view) {
        return new MyViewCapable(view);
      }
      @Override
      public EnumSet<PropagationMode> getPropagationModes() {
        return PropagationMode.defaultSet();
      }    
    };
    // 2 - Register the factory
    ContextMapHelper.registerContextFactoryForPrefixNamed(
        "view capable", viewCapableFactory);
    // 3 - Create the ViewCapable instance
    wcMap.createViewCapable("view capable");
    assertEquals("a value", ((MyViewCapable) wcMap.get("view capable")).getValue());
    
    wcMap.get("ascii");
  }

//  public static void bootstrap(WireAdapter wireAdapter) {
//    reset();
//    /*ThreadLocalAccessor tla = Deencapsulation.getField(ContextBootstrap.class, "threadLocalAccessor");
//    tla.set(null);*/
//    ContextBootstrap.configure(new MockLoggerAdapter(), 
//        wireAdapter, new MockThreadLocalAccessor(), 
//        new MockContextAccessController(), "guid");   
//  }

//  public static void reset() {
//    Deencapsulation.setField(ContextBootstrap.class, "isConfigured", false);
//    try {
//      ContextMapHelper.getScopeAwareContextMap().get("true");
//      fail("Should get IllegalStateException");
//    } catch (InsufficientCredentialException e) {
//      fail(e.toString());
//    } catch (IllegalStateException ignoreThisIsExpected) {}
//  }




}
