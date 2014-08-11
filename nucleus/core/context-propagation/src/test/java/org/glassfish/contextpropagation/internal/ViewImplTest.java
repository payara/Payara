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

import java.io.Serializable;

//import mockit.Deencapsulation;

import org.glassfish.contextpropagation.PropagationMode;
//import org.glassfish.contextpropagation.adaptors.BootstrapUtils;
import org.glassfish.contextpropagation.internal.Entry.ContextType;
import org.glassfish.contextpropagation.wireadapters.glassfish.DefaultWireAdapter;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class ViewImplTest {
//  static ViewImpl view;
//  static SimpleMap sm;
//  
//  @BeforeClass
//  public static void setupClass() {
//    BootstrapUtils.bootstrap(new DefaultWireAdapter());
//    view = new ViewImpl("prefix");
//    sm = new SimpleMap();
//    Deencapsulation.setField(view, "sMap", sm);
//    sm.put("prefix.removeMe", new Entry("removeMe", PropagationMode.defaultSet(), ContextType.STRING).init(true, true));
//    sm.put("prefix.getMe", new Entry("getMe", PropagationMode.defaultSet(), ContextType.STRING).init(true, true));
//    sm.put("prefix.string", new Entry("string", PropagationMode.defaultSet(), ContextType.STRING).init(true, true));
//    sm.put("prefix.asciiString", new Entry("asciistring", PropagationMode.defaultSet(), ContextType.ASCII_STRING).init(true, true));
//    sm.put("prefix.long", new Entry(1L, PropagationMode.defaultSet(), ContextType.LONG).init(true, true));
//    sm.put("prefix.boolean", new Entry(true, PropagationMode.defaultSet(), ContextType.BOOLEAN).init(true, true));
//    sm.put("prefix.char", new Entry('c', PropagationMode.defaultSet(), ContextType.CHAR).init(true, true));
//    sm.put("prefix.serializable", new Entry("serializable", PropagationMode.defaultSet(), ContextType.SERIALIZABLE).init(true, true));
//  }
//
//  @Test
//  public void testGet() {
//    assertEquals("getMe", view.get("getMe"));
//  }
//
//  @Test
//  public void testPutStringStringEnumSetOfPropagationModeBoolean() {
//    checkPut("string", "string", "new_string", ContextType.STRING);
//  }
//
//  <T> void checkPut(String key, Object origValue, Object newValue, ContextType contextType) {
//    assertEquals(origValue, Deencapsulation.invoke(view, "put", key, newValue, PropagationMode.defaultSet()));
//    assertEquals(newValue, sm.get("prefix." + key));
//    assertEquals(ContextType.STRING, sm.getEntry("prefix.string").getContextType());
//  }
//
//  @Test
//  public void testPutAscii() {
//    checkPut("asciiString", "asciistring", "new_asciistring", ContextType.ASCII_STRING);
//  }
//
//  @Test
//  public void testPutStringUEnumSetOfPropagationModeBoolean() {
//    checkPut("long", 1L, 2L, ContextType.LONG);
//  }
//
//  @Test
//  public void testPutStringBooleanEnumSetOfPropagationModeBoolean() {
//    checkPut("boolean", true, false, ContextType.BOOLEAN);
//  }
//
//  @Test
//  public void testPutStringCharacterEnumSetOfPropagationModeBoolean() {
//    checkPut("char", 'c', 'd', ContextType.CHAR);
//  }
//
//  @Test
//  public void testPutSerializable() {
//    checkPut("serializable", (Serializable) "serializable", (Serializable) "new_serializable", ContextType.SERIALIZABLE);
//  }
//
//  @Test
//  public void testRemove() {
//    assertEquals("removeMe", view.remove("removeMe"));
//  }

}
