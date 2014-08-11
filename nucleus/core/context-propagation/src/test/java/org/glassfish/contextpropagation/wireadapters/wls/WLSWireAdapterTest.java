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

package org.glassfish.contextpropagation.wireadapters.wls;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.EnumSet;

//import mockit.Deencapsulation;

import org.glassfish.contextpropagation.ContextMap;
import org.glassfish.contextpropagation.InsufficientCredentialException;
import org.glassfish.contextpropagation.PropagationMode;
import org.glassfish.contextpropagation.SerializableContextFactory;
import org.glassfish.contextpropagation.SerializableContextFactory.WLSContext;
//import org.glassfish.contextpropagation.adaptors.BootstrapUtils;
//import org.glassfish.contextpropagation.adaptors.MockLoggerAdapter;
import org.glassfish.contextpropagation.internal.AccessControlledMap;
import org.glassfish.contextpropagation.internal.Entry;
import org.glassfish.contextpropagation.internal.Utils;
import org.glassfish.contextpropagation.internal.Utils.PrivilegedWireAdapterAccessor;
import org.glassfish.contextpropagation.spi.ContextMapHelper;
import org.glassfish.contextpropagation.spi.ContextMapPropagator;
import org.glassfish.contextpropagation.weblogic.workarea.PropagationTest;
import org.glassfish.contextpropagation.weblogic.workarea.PropertyReadOnlyException;
import org.glassfish.contextpropagation.wireadapters.AbstractWireAdapter;
import org.glassfish.contextpropagation.wireadapters.Catalog;
import org.glassfish.contextpropagation.wireadapters.WireAdapter;
import org.glassfish.contextpropagation.wireadapters.wls.WLSWireAdapter.ClassNames;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class WLSWireAdapterTest {
//  WLSWireAdapter adapter = new WLSWireAdapter(); 
//
//  @BeforeClass
//  public static void setupClass() {
//    BootstrapUtils.bootstrap(new WLSWireAdapter());
//    WLSWireAdapter.HELPER.registerContextFactoryForClass(MyWLSContext.class, 
//        "org.glassfish.contextpropagation.weblogic.workarea.MyContext", 
//        new SerializableContextFactory() {
//      @Override
//      public WLSContext createInstance() {
//        return new MyWLSContext();
//      }
//    });
//  }
//  
//  @Before public void before() {
//    BootstrapUtils.reset();
//    BootstrapUtils.bootstrap(new WLSWireAdapter());
//  }
//
//  @Test
//  public void testFromWLS() throws IOException, PropertyReadOnlyException, ClassNotFoundException {
//    PropagationTest.setup();
//    byte[] bytes = PropagationTest.serialize();
//    ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
//    WireAdapter adapter = new WLSWireAdapter();
//    adapter.prepareToReadFrom(bais);
//    for(String key = adapter.readKey(); key != null; key = adapter.readKey()) {
//      adapter.readEntry();
//    }
//  }
//
//  @Test public void testWithCatalog() throws PropertyReadOnlyException, IOException, InsufficientCredentialException {
//    BootstrapUtils.populateMap();
//    ByteArrayOutputStream baos = new ByteArrayOutputStream();
//    ContextMapPropagator propagator = Utils.getScopeAwarePropagator();
//    WLSWireAdapter adapter = new WLSWireAdapter();
//    propagator.useWireAdapter(adapter);
//    propagator.sendRequest(baos, PropagationMode.RMI);
//    Catalog catalog = Deencapsulation.getField((AbstractWireAdapter) adapter, "catalog");
//    BootstrapUtils.reset();
//    adapter = new WLSWireAdapter();
//    BootstrapUtils.bootstrap(adapter);
//    byte[] bytes = baos.toByteArray();
//    MockLoggerAdapter.debug(Utils.toString(bytes));
//    ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
//    propagator.useWireAdapter(adapter);
//    propagator.receiveRequest(bais);
//    Catalog newCatalog = Deencapsulation.getField(adapter, "wlsCatalog");
//    assertEquals(catalog, newCatalog);
//    //MockLoggerAdapter.debug("start: " + Deencapsulation.getField(newCatalog, "start") + ", end: " + Deencapsulation.getField(newCatalog, "end"));
//  }
//
//  @Test
//  public void testResilientWithBadSerializableInsertFirst() throws IOException, InsufficientCredentialException {
//    badSerializable(true); 
//  }
//  
//  @Test
//  public void testResilientWithBadSerializableInsertLast() throws IOException, InsufficientCredentialException {
//    badSerializable(false);
//  }
//
//  private void badSerializable(boolean insertFirst) throws IOException, InsufficientCredentialException {
//    ContextMap wcMap = ContextMapHelper.getScopeAwareContextMap();  
//    String key = "faulty serializable";
//    @SuppressWarnings("serial")
//    Serializable faultySerializable = new Serializable() {
//      @SuppressWarnings("unused")
//      transient String s = "";
//      private void writeObject(ObjectOutputStream out)
//          throws IOException {
//        out.writeLong(1L);
//        out.writeUTF("a string");
//      }
//      private void readObject(ObjectInputStream in)
//          throws IOException, ClassNotFoundException {
//        MockLoggerAdapter.debug("*******");
//        in.readFully(new byte[25]); // expected to fail since we should be reading a long and produce stack traces
//        MockLoggerAdapter.debug(" -- done");
//      }
//    };
//    if (insertFirst) Deencapsulation.invoke(wcMap, "putSerializable", key, faultySerializable, PropagationMode.defaultSet());
//    BootstrapUtils.populateMap();
//    if (!insertFirst) Deencapsulation.invoke(wcMap, "putSerializable", key, faultySerializable, PropagationMode.defaultSet());
//    ByteArrayOutputStream baos = new ByteArrayOutputStream();
//    ContextMapPropagator propagator = Utils.getScopeAwarePropagator();
//    WLSWireAdapter adapter = new WLSWireAdapter();
//    propagator.useWireAdapter(adapter);
//    propagator.sendRequest(baos, PropagationMode.RMI);
//    Catalog catalog = Deencapsulation.getField((AbstractWireAdapter) adapter, "catalog");
//    BootstrapUtils.reset();
//    adapter = new WLSWireAdapter();
//    BootstrapUtils.bootstrap(adapter);
//    byte[] bytes = baos.toByteArray();
//    MockLoggerAdapter.debug(Utils.toString(bytes));
//    ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
//    propagator.useWireAdapter(adapter);
//    propagator.receiveRequest(bais);
//    Catalog newCatalog = Deencapsulation.getField(adapter, "wlsCatalog");
//    assertNull(wcMap.get(key));
//    assertEquals(catalog, newCatalog); // Check that the catalog is read since the faulty context is read before it.
//  }
//
//  @Test(expected = IOException.class) public void markReset() throws IOException {
//    ByteArrayOutputStream baos = new ByteArrayOutputStream();
//    ObjectOutputStream oos = new ObjectOutputStream(baos);
//    oos.write("Some data".getBytes());
//    oos.flush();
//    ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
//    bais.mark(100);
//    bais.read();
//    bais.reset();
//    ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(bais));
//    ois.mark(100);
//    ois.read();
//    ois.skip(5); // It can skip but cannot reset
//    assertEquals(false, ois.markSupported());
//    ois.reset(); // ObjectOutputStream does not support reset even if we give it a BufferedInputStream
//  }
//
//  @Test
//  public void testResilientWithBadWorkContext() throws IOException, InsufficientCredentialException {
//    ContextMap wcMap = ContextMapHelper.getScopeAwareContextMap();   
//    AccessControlledMap acm = ((PrivilegedWireAdapterAccessor) wcMap).getAccessControlledMap(true);
//    String key = "bad work context";
//    WLSContext ctx = new WLSContext() {
//      @Override
//      public void writeContext(ObjectOutput out) throws IOException {
//        out.writeUTF("a string"); 
//      }
//      @Override
//      public void readContext(ObjectInput in) throws IOException {
//        in.readLong(); // Expected to fail
//      }     
//    };
//    acm.put(key, Entry.createOpaqueEntryInstance(ctx, PropagationMode.defaultSet(), ctx.getClass().getName()).init(
//        true, false));
//    BootstrapUtils.populateMap();
//    ByteArrayOutputStream baos = new ByteArrayOutputStream();
//    ContextMapPropagator propagator = Utils.getScopeAwarePropagator();
//    WLSWireAdapter adapter = new WLSWireAdapter();
//    propagator.useWireAdapter(adapter);
//    propagator.sendRequest(baos, PropagationMode.RMI);
//    Catalog catalog = Deencapsulation.getField((AbstractWireAdapter) adapter, "catalog");
//    BootstrapUtils.reset();
//    adapter = new WLSWireAdapter();
//    BootstrapUtils.bootstrap(adapter);
//    byte[] bytes = baos.toByteArray();
//    MockLoggerAdapter.debug(Utils.toString(bytes));
//    ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
//    propagator.useWireAdapter(adapter);
//    propagator.receiveRequest(bais);
//    Catalog newCatalog = Deencapsulation.getField(adapter, "wlsCatalog");
//    assertEquals(catalog, newCatalog);
//    assertNull(wcMap.get(key));
//  }
//
//  public static byte[] toWLSBytes() throws InsufficientCredentialException, IOException {
//    ByteArrayOutputStream baos = new ByteArrayOutputStream();
//    WireAdapter adapter = new WLSWireAdapter();
//    BootstrapUtils.bootstrap(adapter);
//    BootstrapUtils.populateMap();
//    //adapter.prepareToWriteTo(baos);
//    ContextMapPropagator propagator = ContextMapHelper.getScopeAwarePropagator();
//    Deencapsulation.setField(WLSWireAdapter.class, "WLS_CARRIER_CLASS_NAME", 
//        "org.glassfish.contextpropagation.weblogic.workarea.SerializableWorkContext$Carrier");
//    Deencapsulation.setField(ClassNames.class, "ASCII", 
//        "org.glassfish.contextpropagation.weblogic.workarea.AsciiWorkContext".getBytes());
//    Deencapsulation.setField(ClassNames.class, "LONG", 
//        "org.glassfish.contextpropagation.weblogic.workarea.LongWorkContext".getBytes());
//    Deencapsulation.setField(ClassNames.class, "STRING", 
//        "org.glassfish.contextpropagation.weblogic.workarea.StringWorkContext".getBytes());
//    Deencapsulation.setField(ClassNames.class, "SERIALIZABLE", 
//        "org.glassfish.contextpropagation.weblogic.workarea.SerializableWorkContext".getBytes());
//    propagator.sendRequest(baos, PropagationMode.RMI);
//    //MockLoggerAdapter.debug(Utils.toString(baos.toByteArray()));
//    return baos.toByteArray();
//  }
//
//  @Test @Ignore("moved functionality to Carrier, check if duplicate test or needs rework")
//  public void testObjectOutputStreamWriteObject() throws IOException {
//    /*
//     * checks the capability to change the class name on the wire when serializing objects.
//     */
//    ByteArrayOutputStream baos = new ByteArrayOutputStream();
//    SerializableContextFactory contextFactory = new SerializableContextFactory() {
//      @Override
//      public WLSContext createInstance() {
//        //  Auto-generated method stub
//        return null;
//      }};
//      Serializable s = new MySerializable();
//      WLSWireAdapter.HELPER.registerContextFactoryForClass(MySerializable.class, 
//          "workarea.String", contextFactory);
//      WLSWireAdapter.HELPER.registerContextFactoryForContextNamed(
//          "foo", "workarea.Foo", contextFactory);
//      Object oois = Deencapsulation.newInstance(
//          "weblogic.workarea.WLSWireAdapter$ObjectOutputInterceptorStream", baos);
//      Deencapsulation.invoke(oois, "writeObject", s);
//      Deencapsulation.invoke(oois, "setContextName", "foo");
//      Deencapsulation.invoke(oois, "writeObject", new MySerializable2());
//      Deencapsulation.invoke(oois, "flush");
//      String output = new String(baos.toByteArray());
//      assertTrue(output.contains("workarea.String"));
//      assertTrue(output.contains("workarea.Foo"));
//  }
//
//  @Test public void convertPropagationMode() {
//    // LOCAL, WORK, RMI, TRANSACTION, JMS_QUEUE, JMS_TOPIC, SOAP, MIME_HEADER, ONEWAY
//    checkPropModeConversion(EnumSet.of(PropagationMode.LOCAL), 
//        org.glassfish.contextpropagation.weblogic.workarea.PropagationMode.LOCAL);
//    checkPropModeConversion(EnumSet.of(PropagationMode.THREAD), 
//        org.glassfish.contextpropagation.weblogic.workarea.PropagationMode.WORK);
//    checkPropModeConversion(EnumSet.of(PropagationMode.RMI), 
//        org.glassfish.contextpropagation.weblogic.workarea.PropagationMode.RMI);
//    checkPropModeConversion(EnumSet.of(PropagationMode.TRANSACTION), 
//        org.glassfish.contextpropagation.weblogic.workarea.PropagationMode.TRANSACTION);
//    checkPropModeConversion(EnumSet.of(PropagationMode.JMS_QUEUE), 
//        org.glassfish.contextpropagation.weblogic.workarea.PropagationMode.JMS_QUEUE);
//    checkPropModeConversion(EnumSet.of(PropagationMode.JMS_TOPIC), 
//        org.glassfish.contextpropagation.weblogic.workarea.PropagationMode.JMS_TOPIC);
//    checkPropModeConversion(EnumSet.of(PropagationMode.SOAP), 
//        org.glassfish.contextpropagation.weblogic.workarea.PropagationMode.SOAP);
//    checkPropModeConversion(EnumSet.of(PropagationMode.MIME_HEADER), 
//        org.glassfish.contextpropagation.weblogic.workarea.PropagationMode.MIME_HEADER);
//    checkPropModeConversion(EnumSet.of(PropagationMode.ONEWAY), 
//        org.glassfish.contextpropagation.weblogic.workarea.PropagationMode.ONEWAY);
//    /*
//     * Glassfish's default includes all of the WLS default plus THREAD (which 
//     * is equivalent to the WLS propagation mode WORK)
//     */
//    checkPropModeConversion(PropagationMode.defaultSet(),
//        org.glassfish.contextpropagation.weblogic.workarea.PropagationMode.DEFAULT +
//        org.glassfish.contextpropagation.weblogic.workarea.PropagationMode.WORK);
//    checkPropModeConversion(PropagationMode.defaultSet(), 
//        org.glassfish.contextpropagation.weblogic.workarea.PropagationMode.GLOBAL + 
//        org.glassfish.contextpropagation.weblogic.workarea.PropagationMode.WORK);
//  }
//
//  private void checkPropModeConversion(EnumSet<PropagationMode> propagationModes, int expectedWLSPropMode) {
//    assertEquals(expectedWLSPropMode, WLSWireAdapter.toWlsPropagationMode(propagationModes));
//  }
//
//  @Test public void toPropagationMode() {
//    checkReversePropModeConversion(EnumSet.of(PropagationMode.LOCAL), 
//        org.glassfish.contextpropagation.weblogic.workarea.PropagationMode.LOCAL);
//    checkReversePropModeConversion(EnumSet.of(PropagationMode.THREAD), 
//        org.glassfish.contextpropagation.weblogic.workarea.PropagationMode.WORK);
//    checkReversePropModeConversion(EnumSet.of(PropagationMode.RMI), 
//        org.glassfish.contextpropagation.weblogic.workarea.PropagationMode.RMI);
//    checkReversePropModeConversion(EnumSet.of(PropagationMode.TRANSACTION), 
//        org.glassfish.contextpropagation.weblogic.workarea.PropagationMode.TRANSACTION);
//    checkReversePropModeConversion(EnumSet.of(PropagationMode.JMS_QUEUE), 
//        org.glassfish.contextpropagation.weblogic.workarea.PropagationMode.JMS_QUEUE);
//    checkReversePropModeConversion(EnumSet.of(PropagationMode.JMS_TOPIC), 
//        org.glassfish.contextpropagation.weblogic.workarea.PropagationMode.JMS_TOPIC);
//    checkReversePropModeConversion(EnumSet.of(PropagationMode.SOAP), 
//        org.glassfish.contextpropagation.weblogic.workarea.PropagationMode.SOAP);
//    checkReversePropModeConversion(EnumSet.of(PropagationMode.MIME_HEADER), 
//        org.glassfish.contextpropagation.weblogic.workarea.PropagationMode.MIME_HEADER);
//    checkReversePropModeConversion(EnumSet.of(PropagationMode.ONEWAY), 
//        org.glassfish.contextpropagation.weblogic.workarea.PropagationMode.ONEWAY);
//    checkReversePropModeConversion(PropagationMode.defaultSet(), 
//        org.glassfish.contextpropagation.weblogic.workarea.PropagationMode.DEFAULT +
//        org.glassfish.contextpropagation.weblogic.workarea.PropagationMode.WORK);
//    checkReversePropModeConversion(PropagationMode.defaultSet(), 
//        org.glassfish.contextpropagation.weblogic.workarea.PropagationMode.GLOBAL +
//        org.glassfish.contextpropagation.weblogic.workarea.PropagationMode.WORK);   
//  }
//
//  private void checkReversePropModeConversion(
//      EnumSet<PropagationMode> modes, int mode) {
//    assertEquals(modes, WLSWireAdapter.toPropagationMode(mode));
//
//  }
//
//  void foo(Object o) {System.out.println(o);}
//
//  @Test public void testFoo() {
//    foo(1);
//  }
//
//  @Test public void testStreams() throws IOException {
//    ByteArrayOutputStream baos = new ByteArrayOutputStream();
//    ObjectOutputStream oos = new ObjectOutputStream(baos);
//    oos.write("foo".getBytes());
//    oos.write("bar".getBytes());
//    oos.flush();
//    MockLoggerAdapter.debug(Utils.toString(baos.toByteArray()));
//
//    baos = new ByteArrayOutputStream();
//    oos = new ObjectOutputStream(baos);
//    oos.write("foo".getBytes());
//    ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
//    ObjectOutputStream oos2 = new ObjectOutputStream(baos2);
//    oos2.write("bar".getBytes());
//    oos2.flush();
//    oos.write(baos2.toByteArray());
//    oos.flush();
//    MockLoggerAdapter.debug(Utils.toString(baos.toByteArray()));
//
//    baos = new ByteArrayOutputStream();
//    oos = new ObjectOutputStream(baos);
//    oos.write("foo".getBytes());
//    baos2 = new ByteArrayOutputStream();
//    oos2 = new ObjectOutputStream(baos2);
//    oos2.write("bar".getBytes());
//    oos2.flush();
//    oos.flush();
//    baos.write(baos2.toByteArray());
//    MockLoggerAdapter.debug(Utils.toString(baos.toByteArray()));
//
//    baos = new ByteArrayOutputStream();
//    baos.write("foo".getBytes());
//    MockLoggerAdapter.debug(Utils.toString(baos.toByteArray()));
//
//    baos = new ByteArrayOutputStream();
//    oos = new ObjectOutputStream(baos);
//    oos.write("foo".getBytes());
//    baos2 = new ByteArrayOutputStream();
//    oos2 = new ObjectOutputStream(baos2);
//    oos2.write("bar".getBytes());
//    oos2.flush();
//    byte[] bytes = baos2.toByteArray();
//    oos.write(bytes, 6, bytes.length - 6);
//    oos.flush();
//    MockLoggerAdapter.debug(Utils.toString(baos.toByteArray()));
//  }


}
