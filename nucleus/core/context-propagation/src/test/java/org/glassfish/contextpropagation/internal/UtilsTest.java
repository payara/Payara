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

import static org.junit.Assert.assertNotNull;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

//import mockit.Deencapsulation;

import org.glassfish.contextpropagation.ContextViewFactory;
import org.glassfish.contextpropagation.PropagationMode;
import org.glassfish.contextpropagation.View;
import org.glassfish.contextpropagation.ViewCapable;
//import org.glassfish.contextpropagation.adaptors.BootstrapUtils;
//import org.glassfish.contextpropagation.adaptors.RecordingLoggerAdapter;
import org.glassfish.contextpropagation.bootstrap.ContextBootstrap;
import org.glassfish.contextpropagation.bootstrap.LoggerAdapter.Level;
import org.glassfish.contextpropagation.bootstrap.LoggerAdapter.MessageID;
import org.glassfish.contextpropagation.wireadapters.glassfish.DefaultWireAdapter;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class UtilsTest {
//  @BeforeClass
//  public static void setupClass() {
//    BootstrapUtils.bootstrap(new DefaultWireAdapter());
//  }
//
//  @Test
//  public void testGetScopeAwarePropagator() {
//    assertNotNull(Utils.getScopeAwarePropagator());
//  }
//
//  @Test
//  public void testGetScopeAwareContextMap() {
//    assertNotNull(Utils.getScopeAwarePropagator());
//  }
//
//  @Test
//  public void testRegisterContextFactoryForPrefixNamed() {
//    Utils.registerContextFactoryForPrefixNamed("prefix", 
//        new ContextViewFactory() {
//           @Override
//          public EnumSet<PropagationMode> getPropagationModes() {
//            return PropagationMode.defaultSet();
//          }        
//          @Override
//          public ViewCapable createInstance(View view) {
//            return new ViewCapable() {};
//          }
//        });
//    assertNotNull(Utils.getFactory("prefix"));
//  }
//
//  private static final Object CONTEXT_VIEW_FACTORY = new ContextViewFactory() {
//    @Override
//    public ViewCapable createInstance(View view) {
//       return null;
//    }
//    @Override
//    public EnumSet<PropagationMode> getPropagationModes() {
//      return null;
//    }    
//  };
//  
//  private static MessageID msgID = MessageID.WRITING_KEY; // We need a dummy MessageID
//  @Test(expected=IllegalArgumentException.class)
//  public void testValidateFactoryRegistrationArgsNullKey() {
//    Utils.validateFactoryRegistrationArgs(null, msgID, "context class name", 
//        CONTEXT_VIEW_FACTORY, null);
//  }
//  
//  @Test(expected=IllegalArgumentException.class)
//  public void testValidateFactoryRegistrationArgsNullContextClassName() {
//    Utils.validateFactoryRegistrationArgs("key", msgID, null, 
//        CONTEXT_VIEW_FACTORY, null);
//  }
//  
//  @Test(expected=IllegalArgumentException.class)
//  public void testValidateFactoryRegistrationArgsNullFactory() {
//    Utils.validateFactoryRegistrationArgs("key", msgID, "context class name", 
//        null, null);
//  }
//  
//  @Test
//  public void testValidateFactoryRegistration() {
//    Map<String, ?> map = Collections.emptyMap();
//    Utils.validateFactoryRegistrationArgs("key", msgID, "context class name", 
//        CONTEXT_VIEW_FACTORY, map);
//  }
//  @Test(expected=IllegalArgumentException.class)
//  public void testValidateFactoryRegistrationNullKey() {
//    Map<String, ?> map = Collections.emptyMap();
//    Utils.validateFactoryRegistrationArgs(null, msgID, "context class name", 
//        CONTEXT_VIEW_FACTORY, map);
//  }
//  @Test(expected=IllegalArgumentException.class)
//  public void testValidateFactoryRegistrationNullClassName() {
//    Map<String, ?> map = Collections.emptyMap();
//    Utils.validateFactoryRegistrationArgs("key", msgID, null, 
//        CONTEXT_VIEW_FACTORY, map);
//  }
//  @Test(expected=IllegalArgumentException.class)
//  public void testValidateFactoryRegistrationNullFactory() {
//    Map<String, ?> map = Collections.emptyMap();
//    Utils.validateFactoryRegistrationArgs("key", msgID, "context class name", 
//        null, map);
//  }
//  @Test(expected=IllegalArgumentException.class)
//  public void testValidateFactoryRegistrationNullMessageID() {
//    Map<String, ?> map = Collections.emptyMap();
//    Utils.validateFactoryRegistrationArgs("key", null, "context class name", 
//        CONTEXT_VIEW_FACTORY, map);
//  }
//  @Test
//  public void testValidateFactoryRegistrationAlreadyRegistered() {
//    RecordingLoggerAdapter logger = new RecordingLoggerAdapter();
//    Deencapsulation.setField(ContextBootstrap.class, "loggerAdapter", logger);
//    Map<String, Object> map = new HashMap<String, Object>();
//    Utils.validateFactoryRegistrationArgs("key", msgID, "context class name", 
//        CONTEXT_VIEW_FACTORY, map);
//    logger.verify(null, null, null, (Object[]) null);
//    map.put("context class name", "something");
//    Utils.validateFactoryRegistrationArgs("key", msgID, "context class name", 
//        CONTEXT_VIEW_FACTORY, map);
//    logger.verify(Level.WARN, null, msgID, "context class name", 
//        "something", CONTEXT_VIEW_FACTORY);
//  }

}
