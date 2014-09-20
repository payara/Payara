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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

//import mockit.Deencapsulation;

//import org.glassfish.contextpropagation.adaptors.BootstrapUtils;
//import org.glassfish.contextpropagation.adaptors.MockLoggerAdapter;
//import org.glassfish.contextpropagation.adaptors.TestableThread;
import org.glassfish.contextpropagation.internal.ViewImpl;
import org.glassfish.contextpropagation.spi.ContextMapHelper;
import org.glassfish.contextpropagation.wireadapters.glassfish.DefaultWireAdapter;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class LocationTest {

//  @BeforeClass
//  public static void setupClass() {
//    BootstrapUtils.bootstrap(new DefaultWireAdapter());
//  }
//
//  @Test
//  public void testGetOrigin() {
//    Location location = new Location(new ViewImpl("prefix") {});
//    assertEquals("guid", location.getOrigin());
//    Deencapsulation.setField(location, "origin", "non-null origin");
//    assertEquals("non-null origin", location.getOrigin());
//  }
//
//  @Test
//  public void testGetLocationId() {
//    Location location = new Location(new ViewImpl("prefix") {});
//    assertEquals("[0]", location.getLocationId());
//  }
//
//  @Test
//  public void testContextToPropagateAndContextAdded() {
//    Location location = new Location(new ViewImpl("prefix") {});
//    Location locationToPropagate = (Location) location.contextToPropagate();
//    assertEquals(location, locationToPropagate);
//    Location propagatedLocation = new Location(new ViewImpl("prefix") {});
//    View view = Deencapsulation.getField(location, "view");
//    Deencapsulation.setField(propagatedLocation, "view", view);
//    propagatedLocation.contextAdded();
//    assertEquals("[0, 1]", propagatedLocation.getLocationId());
//  }
//
//  @Test
//  public void testMultiplePropagations() throws Exception {
//    ContextMap wcMap = ContextMapHelper.getScopeAwareContextMap();    
//    Location location = wcMap.getLocation();
//    assertEquals("guid", location.getOrigin());
//    assertEquals("[0]", location.getLocationId());
//    // TODO NOW make sure the location is created if this is the origin of the request.
//    for (int i = 1; i <= 3; i++) {
//      mimicPropagation("[0, " + i + "]");
//    }
//  }
//
//  private static void mimicPropagation(final String expectedLocationId)
//      throws Exception {
//    ByteArrayOutputStream bos = new ByteArrayOutputStream();
//    ContextMapHelper.getScopeAwarePropagator().sendRequest(bos, PropagationMode.SOAP);
//
//    final ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
//    new TestableThread() {     
//      @Override
//      protected void runTest() throws Exception {
//        ContextMapHelper.getScopeAwarePropagator().receiveRequest(bis);
//
//        ContextMap wcMap = ContextMapHelper.getScopeAwareContextMap();
//        Location location = wcMap.getLocation();
//        MockLoggerAdapter.debug(location.getLocationId());       
//        assertEquals(expectedLocationId, location.getLocationId());
//      }
//    }.startJoinAndCheckForFailures();
//  }  
}
