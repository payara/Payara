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

import java.util.EnumSet;
import java.util.Iterator;

import org.glassfish.contextpropagation.InsufficientCredentialException;
import org.glassfish.contextpropagation.PropagationMode;
//import org.glassfish.contextpropagation.adaptors.BootstrapUtils;
//import org.glassfish.contextpropagation.adaptors.MockLoggerAdapter;
//import org.glassfish.contextpropagation.adaptors.MockThreadLocalAccessor;
import org.glassfish.contextpropagation.bootstrap.ContextAccessController;
import org.glassfish.contextpropagation.bootstrap.ContextBootstrap;
import org.glassfish.contextpropagation.internal.AccessControlledMap.ContextAccessLevel;
import org.glassfish.contextpropagation.internal.Entry.ContextType;
import org.glassfish.contextpropagation.wireadapters.glassfish.DefaultWireAdapter;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class AccessControlledMapTest {
//  AccessControlledMap acm;
//  SimpleMap sm;
//  final Entry DUMMY_ENTRY = createEntry("dummy", PropagationMode.defaultSet(), ContextType.STRING);
//
//  private static Entry createEntry(Object context, EnumSet<PropagationMode> propModes, ContextType ct) {
//    return new Entry(context, propModes, ct).init(false, false);
//  }
//
//  @BeforeClass
//  public static void setupClass() {
//    BootstrapUtils.reset();
//    ContextBootstrap.configure(new MockLoggerAdapter(), 
//        new DefaultWireAdapter(), new MockThreadLocalAccessor(), 
//        new ContextAccessController() {
//      @Override
//      public boolean isAccessAllowed(String key, ContextAccessLevel type) {
//        switch(type) {
//        case READ:
//          return key.contains("read") || key.contains("create") || key.contains("delete") || key.contains("update");
//        case CREATE:
//          return key.contains("create");
//        case DELETE:
//          return key.contains("delete");
//        case UPDATE:
//          return key.contains("update");
//        }
//        return false;
//      }
//      @Override
//      public boolean isEveryoneAllowedToRead(String key) {
//        if ("put".equals(Thread.currentThread().getStackTrace()[2].getMethodName())) {
//          return true;
//        } else {
//          throw new UnsupportedOperationException();
//        }
//      }
//    }, "guid");
//  }
//
//  @Before
//  public void setup()  {
//    acm = new AccessControlledMap();
//    sm = acm.simpleMap;
//    sm.put("noAccess", createEntry("noAccessString", PropagationMode.defaultSet(), ContextType.STRING));
//    sm.put("readOnly", createEntry("readOnlyString", PropagationMode.defaultSet(), ContextType.STRING));
//    sm.put("readableByAll", createEntry("readableByAllString", PropagationMode.defaultSet(), ContextType.STRING).init(true, true));
//  }
//
//  @Test(expected=InsufficientCredentialException.class)
//  public void testGetNoAccess() throws InsufficientCredentialException {
//    acm.get("noAccess");
//  }
//
//  @Test
//  public void testGetAllCanRead() throws InsufficientCredentialException {
//    assertNotNull(acm.get("readableByAll"));
//  }
//
//  @Test
//  public void testGet() throws InsufficientCredentialException {
//    assertNotNull(acm.get("readOnly"));
//  }
//
//  @Test(expected=InsufficientCredentialException.class)
//  public void testGetNoexistent() throws InsufficientCredentialException {
//    acm.get("noexistent");
//  }
//
//  @Test
//  public void testPutReplacePermitted() throws InsufficientCredentialException {
//    sm.put("update", DUMMY_ENTRY);
//    assertNotNull(acm.put("update", DUMMY_ENTRY));
//  }
//
//  @Test(expected=InsufficientCredentialException.class)
//  public void testPutReplaceNotPermitted() throws InsufficientCredentialException {
//    acm.put("readOnly", DUMMY_ENTRY);
//  }
//
//  @Test
//  public void testPutCreatePermitted() throws InsufficientCredentialException {
//    assertNull(acm.put("create", DUMMY_ENTRY));
//  }
//
//  @Test(expected=InsufficientCredentialException.class)
//  public void testPutCreateNoPermitted() throws InsufficientCredentialException {
//    acm.put("readOnlyNew", DUMMY_ENTRY);
//  }
//
//  @Test
//  public void testRemovePermitted() throws InsufficientCredentialException {
//    sm.put("deleteMe", createEntry("deleteMe", PropagationMode.defaultSet(), ContextType.STRING));
//    assertNotNull(acm.remove("deleteMe"));
//  }
//
//  @Test(expected=InsufficientCredentialException.class)
//  public void testRemoveNotPermitted() throws InsufficientCredentialException {
//    acm.remove("readOnly");
//  }
//
//  @Test
//  public void testRemovePermittedNonExistent() throws InsufficientCredentialException {
//    assertNull(acm.remove("deleteNonExistent"));
//  }
//
//  @Test(expected=InsufficientCredentialException.class) // We would rather throw than return null and indicate that there is no entry by that name
//  public void testRemoveNonPermittedNonExistent() throws InsufficientCredentialException {
//    acm.remove("readOnlyNonExistent");
//  }
//
//  @Test(expected=InsufficientCredentialException.class)
//  public void testGetPropagationModesNoAccess() throws InsufficientCredentialException {
//    acm.getPropagationModes("noAccess");
//  }
//
//  @Test
//  public void testGetPropagationModesAllCanRead() throws InsufficientCredentialException {
//    assertNotNull(acm.getPropagationModes("readableByAll"));
//  }
//
//  @Test
//  public void testGetPropagationModes() throws InsufficientCredentialException {
//    assertNotNull(acm.getPropagationModes("readOnly"));
//  }
//
//  @Test(expected=InsufficientCredentialException.class)
//  public void testGetPropagationModesNoexistent() throws InsufficientCredentialException {
//    acm.getPropagationModes("noexistent");
//  }
//
//  @Test
//  public void testNamesEmpty() {
//    sm.map.clear();
//    assertFalse(acm.names().hasNext());
//  }
//  
//  @Test
//  public void testNamesAllUnaccessible() {
//    sm.map.clear();
//    sm.put("hidden1", DUMMY_ENTRY);
//    sm.put("hidden1", DUMMY_ENTRY);
//    assertFalse(acm.names().hasNext());
//  }
//  
//  @Test
//  public void testNamesSomeVisible() {
//    sm.map.clear();
//    sm.put("hidden1", DUMMY_ENTRY);
//    sm.put("hidden1", DUMMY_ENTRY);
//    sm.put("readOnly", DUMMY_ENTRY);
//    sm.put("delete", DUMMY_ENTRY);
//    sm.put("create", DUMMY_ENTRY);
//    sm.put("update", DUMMY_ENTRY);
//    Iterator<?> iter = acm.names();
//    int count = 0;
//    while(iter.hasNext()) { iter.next(); count++; }
//    assertEquals(4, count);
//  }

}
