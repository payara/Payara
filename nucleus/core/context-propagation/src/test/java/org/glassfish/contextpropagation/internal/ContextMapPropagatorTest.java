/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 Oracle and/or its affiliates. All rights reserved.
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.EnumSet;

//import mockit.Deencapsulation;
/*import mockit.Expectations;
import mockit.Mocked;
import mockit.NonStrictExpectations;
import mockit.integration.junit4.JMockit;*/

import org.glassfish.contextpropagation.ContextMap;
import org.glassfish.contextpropagation.InsufficientCredentialException;
import org.glassfish.contextpropagation.Location;
import org.glassfish.contextpropagation.PropagationMode;
//import org.glassfish.contextpropagation.adaptors.BootstrapUtils;
//import org.glassfish.contextpropagation.adaptors.TestableThread;
import org.glassfish.contextpropagation.internal.Entry.ContextType;
import org.glassfish.contextpropagation.spi.ContextMapPropagator;
import org.glassfish.contextpropagation.wireadapters.WireAdapter;
import org.glassfish.contextpropagation.wireadapters.glassfish.DefaultWireAdapter;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/*
 * Behavioral tests to check the ContextMapPropagator is properly driving the WireAdapter
 *
 */
//@RunWith(JMockit.class)
@Ignore
public class ContextMapPropagatorTest {
//  ContextMapPropagator propagator;
//  ContextMap cm;
//  SimpleMap sm;
//  //@Mocked(realClassName="org.glassfish.contextpropagation.wireadapters.glassfish.DefaultWireAdapter")
//  final WireAdapter adapter = new DefaultWireAdapter();   
//  Entry defaultEntry, rmiEntry, soapEntry;
//  final OutputStream out = new ByteArrayOutputStream();
//
//  @Before
//  public void setup() throws InsufficientCredentialException {
//    BootstrapUtils.bootstrap(adapter);
//    propagator = Utils.getScopeAwarePropagator();
//    cm = Utils.getScopeAwareContextMap();
//    EnumSet<PropagationMode> oneWayDefault = PropagationMode.defaultSet();
//    oneWayDefault.add(PropagationMode.ONEWAY);
//    cm.put("default", "default value", oneWayDefault); // Only sent in the request
//    cm.put("rmi", "rmi value", EnumSet.of(PropagationMode.RMI));
//    cm.put("soap", "soap value", EnumSet.of(PropagationMode.SOAP));
//    Utils.AccessControlledMapFinder acmFinder = Deencapsulation.getField(Utils.class, "mapFinder");
//    sm = acmFinder.getMapIfItExists().simpleMap;
//    defaultEntry = sm.getEntry("default");
//    rmiEntry = sm.getEntry("rmi");
//    soapEntry = sm.getEntry("soap");
//  }
//
//  @Test
//  public void testSendRequest() throws IOException, InsufficientCredentialException {
////    new Expectations() {
////      {
////        adapter.prepareToWriteTo(out);
////        adapter.write("default", defaultEntry);
////        adapter.write("rmi", rmiEntry);
////        adapter.flush();
////      }
////    };
//    propagator.sendRequest(out, PropagationMode.RMI);
//  }
//
//  @Ignore("jmockit fails without providing a reason")@Test // TODO re-evaluate this test
//  public void testSendRequestWithLocation() throws IOException, InsufficientCredentialException {
//    final Entry locationEntry = setupLocation();
////    new Expectations() {
////      {
////        adapter.prepareToWriteTo(out);
////        adapter.write(Location.KEY, locationEntry); // the order to location calls may have changed since we no longer write it first.
////        adapter.write("default", defaultEntry);
////        //adapter.write(Location.KEY + ".locationId", (Entry) any);
////        //adapter.write(Location.KEY + ".origin", (Entry) any);
////        adapter.write("rmi", rmiEntry);
////        adapter.flush();
////      }
////    };
//    propagator.sendRequest(out, PropagationMode.RMI);
//  }
//
//  protected Entry setupLocation() {
//    final Entry locationEntry = new Entry(new Location(new ViewImpl(Location.KEY)) {}, 
//        Location.PROP_MODES, ContextType.VIEW_CAPABLE).init(true, false);
//    sm.put(Location.KEY, locationEntry);
//    return locationEntry;
//  }
//
//  @Test
//  public void testSendResponse() throws IOException {
////    new Expectations() {
////      {
////        adapter.prepareToWriteTo(out);
////        // default is not expected because it has propagation mode ONEWAY
////        adapter.write("rmi", rmiEntry);
////        adapter.flush();
////      }
////    };
//    propagator.sendResponse(out, PropagationMode.RMI);
//  }
//
//  @Test
//  public void testSendResponseWithLocation() throws IOException {
//    setupLocation();
////    new Expectations() {
////      {
////        adapter.prepareToWriteTo(out);
////        // Location is not expected for responses
////        // default is not expected because it has propagation mode ONEWAY
////        adapter.write("rmi", rmiEntry);
////        adapter.flush();
////      }
////    };
//    propagator.sendResponse(out, PropagationMode.RMI);
//  }
//
//  final static InputStream NOOPInputStream = new InputStream() {
//    @Override public int read() throws IOException {
//      return 0;
//    }
//  };
//
//  @Test
//  public void testReceiveRequestBehavior() throws IOException, ClassNotFoundException {
//    checkReceiveBehavior("receiveRequest", NOOPInputStream);
//  }
//
//  protected void checkReceiveBehavior(String methodName, Object... args) throws IOException,
//  ClassNotFoundException {
//
////    new Expectations() {
////      {
////        adapter.prepareToReadFrom(NOOPInputStream);
////        adapter.readKey(); result = "default";
////        adapter.readEntry(); result = defaultEntry;
////        adapter.readKey(); result = "rmi";
////        adapter.readEntry(); result = rmiEntry;
////        adapter.readKey(); result = "soap";
////        adapter.readEntry(); result = soapEntry;
////        adapter.readKey(); result = null;
////      }
////    };
//    Deencapsulation.invoke(propagator, methodName, args);
//  }
//
//  @Test
//  public void testReceiveResponse() throws IOException, ClassNotFoundException {
//    checkReceiveBehavior("receiveResponse", NOOPInputStream, PropagationMode.SOAP);
//  }
//
//  @Test
//  public void testRestoreThreadContexts() throws InsufficientCredentialException, InterruptedException {
//    cm.put("local", "local context", EnumSet.of(PropagationMode.LOCAL)); // This one should not propagate since it is LOCAL
//    final AccessControlledMap acm = cm.getAccessControlledMap();
//    new TestableThread() {
//      @Override
//      public void runTest() throws Exception {
//        propagator.restoreThreadContexts(acm);
//        ContextMap newCM = Utils.getScopeAwareContextMap();
//        assertNotSame(cm, newCM);
//        assertNull(newCM.get("local"));
//        assertNotNull(newCM.get("default"));
//        assertNull(newCM.get("soap")); // Does not have the PropagationMode.THREAD
//        assertNull(newCM.get("rmi")); // Does not have the PropagationMode.THREAD
//      }      
//    }.startJoinAndCheckForFailures();
//  }
//
//  public void testUseWireAdapter(final WireAdapter wa) throws IOException {
//    assertTrue(wa != adapter);
//    propagator.useWireAdapter(wa);
////    new NonStrictExpectations() {{
////      wa.prepareToWriteTo((OutputStream) withNotNull()); times = 1;
////    }};
//    propagator.sendRequest(out, PropagationMode.RMI);
//  }
//
//  @Test public void clearPropagatedEntries() throws InsufficientCredentialException {
//    assertEquals(defaultEntry.value, cm.get("default"));
//    assertEquals(rmiEntry.value, cm.get("rmi"));
//    assertEquals(soapEntry.value, cm.get("soap"));
//    Deencapsulation.invoke(propagator, "clearPropagatedEntries", PropagationMode.ONEWAY, sm);
//    assertNull(cm.get("default")); // The only ONEWAY item
//    assertNotNull(cm.get("rmi"));
//    assertNotNull(cm.get("soap"));
//  }

}
