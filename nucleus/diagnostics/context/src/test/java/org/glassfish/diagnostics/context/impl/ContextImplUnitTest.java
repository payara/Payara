/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.diagnostics.context.impl;

import mockit.Mock;
import mockit.MockUp;
import mockit.Mocked;
import mockit.Verifications;
import mockit.Expectations;
import mockit.NonStrictExpectations;
import mockit.integration.junit4.JMockit;

import org.glassfish.contextpropagation.Location;
import org.glassfish.contextpropagation.PropagationMode;
import org.glassfish.contextpropagation.View;

import org.junit.Test;
import org.junit.Assert;
import org.junit.runner.RunWith;

import java.util.EnumSet;

@RunWith(JMockit.class)
public class ContextImplUnitTest {

 /**
  * Test that the Location field of ContextImpl uses the Location
  * object used at construction and that the Location returned from the
  * ContextImpl does not then change over the lifetime of the ContextImpl.
  */
  @Test
  public void testConstructorsLocation(
    @Mocked final Location mockedLocation,
    @Mocked final View mockedView)
  {

    final String mockedLocationIdReturnValue = "mockedLocationIdReturnValue";
    final String mockedOriginReturnValue = "mockedOriginReturnValue";

    new MockUp<Location>()
    {
      @Mock
      public String getLocationId() { return mockedLocationIdReturnValue; }

      @Mock
      public String getOrigin() { return mockedOriginReturnValue; }
    };

    ContextImpl contextImpl = new ContextImpl(mockedView, mockedLocation);

    Location location1 = contextImpl.getLocation();
    Assert.assertSame("Location from contextImpl.getLocation() should be the instance passed in on construction.", mockedLocation, location1);

    // On the face of is these next two assertions seem perfectly reasonable
    // but in reality they prove nothing regarding the behaviour of the
    // org.glassfish.diagnostics.context.impl code, but rather
    // verify that the  mocking framework is doing it's job: the getLocationId
    // and getOrigin methods are overridden by the mock framework to
    // return the values above, they are not returning state from the
    // mockedLocation object itself.
    /*
       Assert.assertEquals("LocationId from contextImpl.getLocation() should be the locationId value from the location used when constructing the ContextImpl.", location1.getLocationId(), mockedLocationIdReturnValue);
       Assert.assertEquals("Origin from contextImpl.getOrigin() should be the origin value from the location used when constructing the ContextImpl.", location1.getOrigin(), mockedOriginReturnValue);
    */

    Location location2 = contextImpl.getLocation();
    Assert.assertSame("Location from contextImpl.getLocation() should still be the instance passed in on construction.", mockedLocation, location2);
  }

 /**
  * Test that the put operations on an instance of ContextImpl delegate
  * as expected to the View object used in construction.
  */
  @Test
  public void testDelegationOfPut(
    @Mocked final Location mockedLocation,
    @Mocked final View mockedView){

    ContextImpl contextImpl = new ContextImpl(mockedView, mockedLocation);

    contextImpl.put("KeyForString-Value1-true", "Value1", true);
    contextImpl.put("KeyForString-Value2-false", "Value2", false);

    contextImpl.put("KeyForNumber-5-true", 5, true);
    contextImpl.put("KeyForNumber-7-false", 7, false);

    new Verifications(){{
      mockedView.put("KeyForString-Value1-true", "Value1", EnumSet.of(
                                                  PropagationMode.THREAD,
                                                  PropagationMode.RMI,
                                                  PropagationMode.JMS_QUEUE,
                                                  PropagationMode.SOAP,
                                                  PropagationMode.MIME_HEADER,
                                                  PropagationMode.ONEWAY));
      mockedView.put("KeyForString-Value2-false", "Value2", EnumSet.of(
                                                  PropagationMode.LOCAL));

      mockedView.put("KeyForNumber-5-true", 5, EnumSet.of(
                                                  PropagationMode.THREAD,
                                                  PropagationMode.RMI,
                                                  PropagationMode.JMS_QUEUE,
                                                  PropagationMode.SOAP,
                                                  PropagationMode.MIME_HEADER,
                                                  PropagationMode.ONEWAY));
      mockedView.put("KeyForNumber-7-false", 7, EnumSet.of(
                                                  PropagationMode.LOCAL));
    }};

  }

 /**
  * Test that the get operation on an instance of ContextImpl delegates
  * as expected to the View object used in construction.
  */
  @Test
  public void testDelegationOfGet(
    @Mocked final Location mockedLocation,
    @Mocked final View mockedView){

    final String key = "testDelegationOfGet-Key1";
    final String expectedValueOfKey1 = "testDelegationOfGet-Value1";
    ContextImpl contextImpl = new ContextImpl(mockedView, mockedLocation);

    new Expectations(){

      // We expect get to be called on the view, and we'll
      // instruct the mocking framework to return expectedValueOfKey1
      // so that we can also verify that contextImpl returns it.
      View expectationsRefViewVariable = mockedView;
      {
        expectationsRefViewVariable.get(key); returns(expectedValueOfKey1);
      }
    };

    String value = contextImpl.get(key);

    Assert.assertEquals("Value returned from contextImpl.get(\""+key+"\") is not the value expected.", expectedValueOfKey1, value);
  }

}
