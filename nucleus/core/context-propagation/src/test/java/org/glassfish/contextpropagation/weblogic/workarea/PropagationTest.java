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

package org.glassfish.contextpropagation.weblogic.workarea;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.glassfish.contextpropagation.InsufficientCredentialException;
//import org.glassfish.contextpropagation.adaptors.MockLoggerAdapter;
//import org.glassfish.contextpropagation.adaptors.TestableThread;
import org.glassfish.contextpropagation.internal.Utils;
//import org.glassfish.contextpropagation.weblogic.workarea.spi.WorkContextMapInterceptor;
//import org.glassfish.contextpropagation.weblogic.workarea.utils.WorkContextInputAdapter;
//import org.glassfish.contextpropagation.weblogic.workarea.utils.WorkContextOutputAdapter;
//import org.glassfish.contextpropagation.wireadapters.wls.MySerializable;
import org.glassfish.contextpropagation.wireadapters.wls.WLSWireAdapterTest;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class PropagationTest {  
//  public static WorkContextMap wcMap;
//
//  @BeforeClass
//  public static void setup() throws PropertyReadOnlyException, IOException {
//    wcMap = WorkContextHelper.getWorkContextHelper().getWorkContextMap();
//    wcMap.put("long", PrimitiveContextFactory.create(1L));
//    wcMap.put("string", PrimitiveContextFactory.create("string"));
//    wcMap.put("ascii", PrimitiveContextFactory.createASCII("ascii"));
//    wcMap.put("serializable", PrimitiveContextFactory.createMutable(new MySerializable()));
//    wcMap.put("workcontext", new MyWorkContext());
//  }
//
//  @Test
//  public void testRequestPropagation() throws IOException, InterruptedException {
//    final byte[] bytes = serialize();
//    //MockLoggerAdaper.debug(Utils.toString(bytes));
//    new TestableThread() {
//      @Override public void runTest() throws Exception {
//        final WorkContextMap map = WorkContextHelper.getWorkContextHelper().getWorkContextMap();
//        assertTrue(map.isEmpty());
//        deserialize(bytes);
//        Set<?> expectedSet = map2Set(wcMap);
//        Set<?> resultSet = map2Set(map);
//        assertEquals(expectedSet, resultSet);
//      }
//
//      @SuppressWarnings("serial")
//      private HashSet<?> map2Set(final WorkContextMap map) {
//        return new HashSet<Object>() {{
//          Iterator<?> it = map.iterator();
//          while (it.hasNext()) {
//            add(it.next());
//          }          
//        }};
//      }
//    }.startJoinAndCheckForFailures();
//    MockLoggerAdapter.debug(Utils.toString(bytes));
//  }
//
//  @Ignore("there seems to be a problem with jmockit")
//  @Test public void fromGlassfish() throws InsufficientCredentialException, IOException, InterruptedException {
//    WorkContextMapInterceptor interceptor = WorkContextHelper.getWorkContextHelper().createInterceptor();
//    //byte[] gBytes = getFirstBytes();
//    byte[] bytes = WLSWireAdapterTest.toWLSBytes();
//    //MockLoggerAdaper.debug(">>>" + Utils.toString(gBytes) + "<<<");
//    //MockLoggerAdaper.debug(">>>" + Utils.toString(bytes) + "<<<");
//    ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
//    WorkContextInput wci = new WorkContextInputAdapter(new ObjectInputStream(bais));
//    interceptor.receiveRequest(wci);
//    WorkContextMap map = (WorkContextMap) interceptor;
//    @SuppressWarnings("unchecked")
//    Iterator<String> keys = map.keys();
//    while(keys.hasNext()) {
//      String key = keys.next();
//      MockLoggerAdapter.debug(key + ": " + map.get(key));
//    }
//    assertEquals("ascii", ((PrimitiveWorkContext) map.get("ascii")).get());
//    assertEquals("string", ((PrimitiveWorkContext)map.get("string")).get());
//    assertEquals(1L, ((PrimitiveWorkContext)map.get("one")).get());
//    assertEquals(MyWorkContext.class, map.get("workcontext").getClass());
//    SerializableWorkContext swc = (SerializableWorkContext) map.get("serializable");
//    MockLoggerAdapter.debug("Serializable contents: " + swc.get().getClass() + swc.get());
//    Set<?> set = (Set<?>) swc.get();
//    assertEquals(1,  set.size());
//  }
//
//  public static byte[] serialize() throws IOException {
//    WorkContextMapInterceptor wcInterceptor = WorkContextHelper.getWorkContextHelper().getLocalInterceptor();
//    ByteArrayOutputStream baos = new ByteArrayOutputStream();
//    ObjectOutput oo = new ObjectOutputStream(baos);
//    WorkContextOutput wco = new WorkContextOutputAdapter(oo);
//    wcInterceptor.sendRequest(wco, PropagationMode.RMI);
//    oo.flush();
//    return baos.toByteArray();
//  }
//
//  public static void deserialize(byte[] bytes) {
//    try {
//      //WorkContextHelper.getWorkContextHelper().
//      WorkContextMapInterceptor wcInterceptor = WorkContextHelper.getWorkContextHelper().getInterceptor();
//      ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
//      ObjectInput oi = new ObjectInputStream(bais);
//      WorkContextInput wci = new WorkContextInputAdapter(oi);
//      wcInterceptor.receiveRequest(wci);
//    } catch (IOException ioe) {
//      throw new RuntimeException(ioe);
//    }
//  }


}
