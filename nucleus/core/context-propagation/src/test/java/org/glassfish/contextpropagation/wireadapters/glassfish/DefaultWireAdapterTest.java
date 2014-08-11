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

package org.glassfish.contextpropagation.wireadapters.glassfish;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;

//import mockit.Deencapsulation;

import org.glassfish.contextpropagation.ContextMap;
import org.glassfish.contextpropagation.InsufficientCredentialException;
import org.glassfish.contextpropagation.PropagationMode;
import org.glassfish.contextpropagation.SerializableContextFactory;
//import org.glassfish.contextpropagation.adaptors.BootstrapUtils;
//import org.glassfish.contextpropagation.adaptors.MockLoggerAdapter;
//import org.glassfish.contextpropagation.adaptors.TestableThread;
import org.glassfish.contextpropagation.internal.Utils;
import org.glassfish.contextpropagation.spi.ContextMapHelper;
import org.glassfish.contextpropagation.spi.ContextMapPropagator;
import org.glassfish.contextpropagation.weblogic.workarea.PropagationTest;
import org.glassfish.contextpropagation.weblogic.workarea.PropertyReadOnlyException;
import org.glassfish.contextpropagation.wireadapters.AbstractWireAdapter;
import org.glassfish.contextpropagation.wireadapters.Catalog;
import org.glassfish.contextpropagation.wireadapters.WireAdapter;
import org.glassfish.contextpropagation.wireadapters.glassfish.DefaultWireAdapter;
import org.glassfish.contextpropagation.wireadapters.wls.MyWLSContext;
import org.glassfish.contextpropagation.wireadapters.wls.WLSWireAdapter;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class DefaultWireAdapterTest {
//  ContextMap wcMap;
//
//  @Before
//  public void setup() throws InsufficientCredentialException {
//    BootstrapUtils.reset();
//    BootstrapUtils.bootstrap(new DefaultWireAdapter());
//    BootstrapUtils.populateMap();
//    wcMap = ContextMapHelper.getScopeAwareContextMap();
//  }
//
//  @Test
//  public void testWrite() throws IOException, InterruptedException {
//    ByteArrayOutputStream out = new ByteArrayOutputStream();
//    ContextMapHelper.getScopeAwarePropagator().sendRequest(out, PropagationMode.RMI);
//    byte[] bytes = out.toByteArray();
//    MockLoggerAdapter.debug("length: " + bytes.length + ", "  + Utils.toString(bytes));
//
//    // Move to its own test
//    final ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
//    new TestableThread() {
//      @Override public void runTest() throws Exception {
//          ContextMap map = ContextMapHelper.getScopeAwareContextMap();
//          assertNull(map.get("one"));
//          ContextMapHelper.getScopeAwarePropagator().receiveRequest(bais);
//          assertNotNull(map.get("one"));
//      }
//    }.startJoinAndCheckForFailures();
//  }
//  
//  @Test
//  public void testPropagateOpaque() throws PropertyReadOnlyException, IOException, InsufficientCredentialException {
//    BootstrapUtils.reset();
//    WireAdapter adapter = new WLSWireAdapter();
//    BootstrapUtils.bootstrap(adapter);
//    // Receive data using WLS adaptor but lacking the necessary factory to instantiate the opaque. Make sure the object is opaque
//    PropagationTest.setup();
//    byte[] bytes = PropagationTest.serialize();
//    ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
//    registerWorkContextFactory();
//    ContextMapPropagator propagator = Utils.getScopeAwarePropagator();
//    propagator.useWireAdapter(adapter);
//    propagator.receiveRequest(bais);
//    ContextMap cm = Utils.getScopeAwareContextMap();
//    assertNotNull(cm.get("workcontext"));
//    
//    // Propagate and receive using Default adaptors, but this time the factory is available. Make sure the object is properly instantiated.
//    // Propagate to a process where there is no factory for workcontext
//    ByteArrayOutputStream baos = new ByteArrayOutputStream();
//    propagator = Utils.getScopeAwarePropagator();
//    adapter = new DefaultWireAdapter();
//    propagator.useWireAdapter(adapter);
//    propagator.sendRequest(baos, PropagationMode.RMI);
//    
//    // Receive on a process where the factory is not registered
//    Map<String, SerializableContextFactory> contextFactoriesByContextName = 
//        Deencapsulation.getField(WireAdapter.HELPER, "contextFactoriesByContextName");
//    contextFactoriesByContextName.remove("workcontext");
//    BootstrapUtils.reset();
//    adapter = new DefaultWireAdapter();
//    BootstrapUtils.bootstrap(adapter);
//    bytes = baos.toByteArray();
//    bais = new ByteArrayInputStream(bytes);
//    propagator.useWireAdapter(adapter);
//    propagator.receiveRequest(bais);
//    assertNotNull(cm.get("workcontext"));
//    // Then propagate again to a process where the library is registered
//    baos = new ByteArrayOutputStream();
//    propagator = Utils.getScopeAwarePropagator();
//    adapter = new DefaultWireAdapter();
//    propagator.useWireAdapter(adapter);
//    propagator.sendRequest(baos, PropagationMode.RMI);
//    BootstrapUtils.reset();
//    adapter = new DefaultWireAdapter();
//    BootstrapUtils.bootstrap(adapter);
//    bytes = baos.toByteArray();
//    bais = new ByteArrayInputStream(bytes);
//    propagator.useWireAdapter(adapter);
//    registerWorkContextFactory();
//    propagator.receiveRequest(bais);
//    MyWLSContext mwc = cm.get("workcontext");
//    assertNotNull(mwc);
//    assertEquals((long) 200, (long) mwc.l);
//  }
//
//  private void registerWorkContextFactory() {
//    WireAdapter.HELPER.registerContextFactoryForContextNamed("workcontext", null, 
//        new SerializableContextFactory() {
//          @Override
//          public WLSContext createInstance() {
//            return new MyWLSContext();
//          }
//    });
//  }
//  
//  @Test public void testWithCatalog() throws PropertyReadOnlyException, IOException {
//    ByteArrayOutputStream baos = new ByteArrayOutputStream();
//    ContextMapPropagator propagator = Utils.getScopeAwarePropagator();
//    WireAdapter adapter = new DefaultWireAdapter();
//    propagator.useWireAdapter(adapter);
//    propagator.sendRequest(baos, PropagationMode.RMI);
//    Catalog catalog = Deencapsulation.getField((AbstractWireAdapter) adapter, "catalog");
//    BootstrapUtils.reset();
//    adapter = new DefaultWireAdapter();
//    BootstrapUtils.bootstrap(adapter);
//    byte[] bytes = baos.toByteArray();
//    MockLoggerAdapter.debug(Utils.toString(bytes));
//    ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
//    propagator.useWireAdapter(adapter);
//    propagator.receiveRequest(bais);
//    Catalog newCatalog = Deencapsulation.getField(adapter, "catalog");
//    assertEquals(catalog, newCatalog);
//  }
//  
//  @Test public void testObjectInputStream() throws IOException {
//    ByteArrayOutputStream baos = new ByteArrayOutputStream();
//    ObjectOutputStream oos = new ObjectOutputStream(baos);
//    oos.write("Some data".getBytes());
//    oos.flush();
//    ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
//    ObjectInputStream ois = new ObjectInputStream(bais);
//    assertFalse(ois.markSupported());
//  }

}
