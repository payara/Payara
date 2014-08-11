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

package org.glassfish.contextpropagation.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.EnumSet;
import java.util.Iterator;

//import mockit.Deencapsulation;

import org.glassfish.contextpropagation.ContextMap;
import org.glassfish.contextpropagation.ContextViewFactory;
import org.glassfish.contextpropagation.InsufficientCredentialException;
import org.glassfish.contextpropagation.Location;
import org.glassfish.contextpropagation.PropagationMode;
import org.glassfish.contextpropagation.View;
import org.glassfish.contextpropagation.ViewCapable;
//import org.glassfish.contextpropagation.adaptors.BootstrapUtils;
//import org.glassfish.contextpropagation.adaptors.MockLoggerAdapter;
import org.glassfish.contextpropagation.internal.Entry.ContextType;
import org.glassfish.contextpropagation.internal.Utils.AccessControlledMapFinder;
import org.glassfish.contextpropagation.spi.ContextMapHelper;
import org.glassfish.contextpropagation.wireadapters.glassfish.DefaultWireAdapter;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class ContextMapImplTest {
//  private static Entry DUMMY_ENTRY;
//  private static EnumSet<PropagationMode> PROP_MODES = PropagationMode.defaultSet();
//  static ContextMap cm;
//  static AccessControlledMap acMap, savedMap; 
//  static AccessControlledMapFinder mapFinder = new AccessControlledMapFinder() {
//    @Override
//    protected AccessControlledMap getMapIfItExists() {
//      AccessControlledMap map = super.getMapIfItExists();
//      return map == null ? acMap : map;
//    }    
//  };
//  
//  @BeforeClass
//  public static void setupClass() {
//    BootstrapUtils.bootstrap(new DefaultWireAdapter());
//    DUMMY_ENTRY = 
//        new Entry("dummy", PropagationMode.defaultSet(), ContextType.STRING).init(true, true);
//    savedMap = new AccessControlledMap();
//    Deencapsulation.setField(Utils.class, "mapFinder", mapFinder);
//    cm = Utils.getScopeAwareContextMap();
//    savedMap.simpleMap.put("key", DUMMY_ENTRY);
//    savedMap.simpleMap.put("removeMe", DUMMY_ENTRY);
//    savedMap.simpleMap.put(Location.KEY, 
//        new Entry(new Location(new ViewImpl(Location.KEY)){}, 
//            PropagationMode.defaultSet(), 
//            ContextType.VIEW_CAPABLE).init(true, true));
//  }
//  
//  @Before
//  public void setup() {
//    BootstrapUtils.bootstrap(new DefaultWireAdapter());
//  }
//
//  @Test
//  public void testGet() throws InsufficientCredentialException {
//    acMap = null;   
//    assertNull(cm.get("key"));
//    acMap = savedMap;
//    assertEquals("dummy", cm.get("key"));
//  }
//
//  @Test
//  public void testPutString() throws InsufficientCredentialException {
//    checkPut("a String", "string", "new string", "put");
//  }
//
//  protected void checkPut(String key, Object origContext, Object newContext, String methodName) throws InsufficientCredentialException {
//    acMap = null;
//    Deencapsulation.invoke(cm, "put", key, origContext, PROP_MODES);
//    assertNotNull(mapFinder.getMapIfItExists());
//    assertEquals(origContext, mapFinder.getMapIfItExists().get(key));
//    assertTrue(PROP_MODES == mapFinder.getMapIfItExists().getPropagationModes(key));
//    assertEquals(origContext, Deencapsulation.invoke(cm, methodName, key, newContext, PROP_MODES));
//  }
//
//  @Test
//  public void testPutAscii() throws InsufficientCredentialException {
//    checkPut("an ascii String", "ascii string", "new string", "putAscii");
//  }
//
//  @Test
//  public void testPutNumber() throws InsufficientCredentialException {
//    checkPut("a long", 1L, 2L, "put");
//  }
//
//  @Test
//  public void testPutBoolean() throws InsufficientCredentialException {
//    checkPut("a boolean", true, false, "put");
//  }
//
//  @Test
//  public void testCreateViewCapable() throws InsufficientCredentialException {
//    acMap = savedMap;
//    String prefix = "a view capable";
//    ContextMapHelper.registerContextFactoryForPrefixNamed(prefix, 
//        new ContextViewFactory() {
//          @Override
//          public EnumSet<PropagationMode> getPropagationModes() {
//             return PropagationMode.defaultSet();
//          }
//          @Override
//          public ViewCapable createInstance(View view) {
//            return new ViewCapable() { /* dummy instance */};
//          }
//        });
//    assertNotNull(cm.createViewCapable(prefix));
//  }
//
//  @Test
//  public void testPutSerializable() throws InsufficientCredentialException {
//    checkPut("a Serializable", "serializable", "new serializable", "putSerializable");
//  }
//
//  @Test
//  public void testGetPropagationModes() throws InsufficientCredentialException {
//    acMap = null;   
//    assertNull(cm.getPropagationModes("key"));
//    acMap = savedMap;
//    assertEquals(PropagationMode.defaultSet(), cm.getPropagationModes("key"));
//  }
//
//  @Test
//  public void testRemove() throws InsufficientCredentialException {
//    acMap = savedMap;
//    assertNull(cm.remove("nonexistent"));
//    assertNotNull(cm.remove("removeMe"));
//  }
//
//  @Test
//  public void testPutCharacter() throws InsufficientCredentialException {
//    checkPut("a Character", 'c', 'd', "put");
//  }
//
//  @Ignore@Test(expected=AssertionError.class) // TODO re-evaluate this test
//  public void testGetLocationBeforeItIsSet1() {
//    acMap = null;
//    cm.getLocation();
//  }
//  
//  @Ignore@Test(expected=AssertionError.class) // TODO re-evaluate this test
//  public void testGetLocationBeforeItIsSet2() {
//    acMap = new AccessControlledMap();
//    cm.getLocation();
//  }
//  
//  @Test
//  public void testGetLocationNormalCase() {
//    acMap = savedMap;
//    Location location = cm.getLocation();
//    assertNotNull(location);
//  }
//
//  @Test
//  public void testIsEmpty() {
//    acMap = null;
//    assertTrue(cm.isEmpty());
//    acMap = new AccessControlledMap();
//    assertTrue(cm.isEmpty());
//    acMap = savedMap;
//    assertFalse(cm.isEmpty());    
//  }
//
//  @Test
//  public void testNames() {
//    acMap = null;
//    Iterator<?> iter = cm.names();
//    assertNull(iter);
//    acMap = savedMap;
//    iter = cm.names();
//    while (iter.hasNext()) {
//      MockLoggerAdapter.debug((String) iter.next());
//    }
//  }

}
