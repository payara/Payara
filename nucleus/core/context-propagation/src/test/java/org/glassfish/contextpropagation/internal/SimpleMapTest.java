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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

import org.glassfish.contextpropagation.ContextLifecycle;
import org.glassfish.contextpropagation.PropagationMode;
import org.glassfish.contextpropagation.ViewCapable;
//import org.glassfish.contextpropagation.adaptors.BootstrapUtils;
//import org.glassfish.contextpropagation.adaptors.MockContextAccessController;
//import org.glassfish.contextpropagation.adaptors.MockThreadLocalAccessor;
//import org.glassfish.contextpropagation.adaptors.RecordingLoggerAdapter;
import org.glassfish.contextpropagation.bootstrap.ContextBootstrap;
import org.glassfish.contextpropagation.bootstrap.LoggerAdapter.Level;
import org.glassfish.contextpropagation.bootstrap.LoggerAdapter.MessageID;
import org.glassfish.contextpropagation.internal.Entry.ContextType;
import org.glassfish.contextpropagation.internal.SimpleMap.Filter;
import org.glassfish.contextpropagation.wireadapters.glassfish.DefaultWireAdapter;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class SimpleMapTest {
//  SimpleMap sm;
//  static RecordingLoggerAdapter logger;  
//  
//  public static class LifeCycleEventRecorder implements ContextLifecycle {
//    StackTraceElement lastElement;
//    Object lastArg;
//    
//    @Override
//    public void contextChanged(Object replacementContext) {
//      set(Thread.currentThread().getStackTrace(), replacementContext);
//    }
//
//    @Override
//    public void contextAdded() {
//      set(Thread.currentThread().getStackTrace(), null);      
//    }
//
//    @Override
//    public void contextRemoved() {
//      set(Thread.currentThread().getStackTrace(), null);
//    }
//
//    @Override
//    public ViewCapable contextToPropagate() {
//      set(Thread.currentThread().getStackTrace(), null);
//      return this;
//    }
//    
//    void set(StackTraceElement[] trace, Object arg) {
//      lastElement = trace[1];
//      lastArg = arg;
//    }
//    
//    void verify(String methodName, Object arg) {
//      assertEquals(methodName, lastElement.getMethodName());
//      assertEquals(arg, lastArg);
//      lastElement = null;
//      lastArg = null;
//    }
//    
//  }
//  
//  private static final LifeCycleEventRecorder LIFE_CYCLE_CONTEXT = new LifeCycleEventRecorder();
//  static final Entry DUMMY_ENTRY = createEntry(LIFE_CYCLE_CONTEXT, PropagationMode.defaultSet(), ContextType.OPAQUE);
//
//  private static Entry createEntry(Object context, EnumSet<PropagationMode> propModes, ContextType ct) {
//    return new Entry(context, propModes, ct) {
//      void validate() {}
//    };
//  }
//
//  @BeforeClass
//  public static void setupClass() {
//    logger = new RecordingLoggerAdapter();
//    BootstrapUtils.reset();
//    ContextBootstrap.configure(logger, 
//        new DefaultWireAdapter(), new MockThreadLocalAccessor(), 
//        new MockContextAccessController(), "guid");
//  }
//
//  @Before
//  public void setup() {
//    sm = new SimpleMap();
//    sm.put("foo", createEntry("fooString", PropagationMode.defaultSet(), ContextType.STRING));
//  }
//
//  @Test
//  public void testGetEntry() {
//    Entry entry = sm.getEntry("foo");
//    assertEquals("fooString", entry.getValue());
//    logger.verify(Level.DEBUG, null, MessageID.OPERATION, new Object[] {"getEntry", "foo", entry} );
//  }
//
//  @Test(expected=java.lang.IllegalArgumentException.class)
//  public void testGetEntryWithNullKey() {
//    sm.getEntry(null); // Does not log if fails validation
//  }
//
//  @Test
//  public void testGet() {
//    assertEquals("fooString", sm.get("foo"));
//    logger.verify(Level.DEBUG, null, MessageID.OPERATION, new Object[] {"get", "foo", "fooString"} );
//  }
//
//  @Test(expected=java.lang.IllegalArgumentException.class)
//  public void testGetWithNullKey() {
//    sm.get(null); // Does not log if validate fails
//  }
//
//  @Test
//  public void testPutWhereNoBefore() {
//    Entry e = sm.put("new key", DUMMY_ENTRY);
//    assertNull(e);
//    logger.verify(Level.DEBUG, null, MessageID.PUT, new Object[] {"new key", DUMMY_ENTRY.value, null} );
//    LIFE_CYCLE_CONTEXT.verify("contextAdded", null);
//  }
//
//  @Test
//  public void testPutReplace() {
//    LifeCycleEventRecorder oldRecorder = new LifeCycleEventRecorder();
//    String fooString = sm.put("foo", createEntry(oldRecorder, PropagationMode.defaultSet(), ContextType.OPAQUE));
//    assertEquals("fooString", fooString);
//    logger.verify(Level.DEBUG, null, MessageID.PUT, new Object[] {"foo", oldRecorder, "fooString"} );
//    LifeCycleEventRecorder oldValue = sm.put("foo", DUMMY_ENTRY);
//    assertEquals(oldRecorder, oldValue);
//    oldRecorder.verify("contextChanged", LIFE_CYCLE_CONTEXT); // oldRecoder finds out about the new value
//    LIFE_CYCLE_CONTEXT.verify("contextAdded", null);
//  }
//
//  @Test(expected=java.lang.IllegalArgumentException.class)
//  public void testPutWithNullKey() {
//    sm.put(null, DUMMY_ENTRY);
//  }
//
//  @Test(expected=java.lang.IllegalArgumentException.class)
//  public void testPutWithNullEntry() {
//    sm.put("dummy key", null);
//  }
//
//  @Test(expected=java.lang.IllegalArgumentException.class)
//  public void testPutWithNullValue() {
//    sm.put("dummy key", createEntry(null, PropagationMode.defaultSet(), ContextType.ATOMICINTEGER));
//  }
//
//  @Test(expected=java.lang.IllegalArgumentException.class)
//  public void testPutWithInvalidEntry() {
//    sm.put("dummy key", new Entry(null, PropagationMode.defaultSet(), ContextType.ATOMICINTEGER) {
//      void validate() { throw new IllegalStateException(); }
//    });
//  }
//
//  @Test
//  public void testRemove() {
//    sm.put("removeMe", createEntry(LIFE_CYCLE_CONTEXT, PropagationMode.defaultSet(), ContextType.STRING));
//    Object removeMe = sm.remove("removeMe");
//    assertEquals(LIFE_CYCLE_CONTEXT, removeMe);
//    logger.verify(Level.DEBUG, null, MessageID.OPERATION, new Object[] {"remove", "removeMe", LIFE_CYCLE_CONTEXT} );
//    LIFE_CYCLE_CONTEXT.verify("contextRemoved", null);
//  }
//
//  @Test
//  public void testRemoveNoneExistent() {
//    String removeMe = sm.remove("removeMe");
//    assertEquals(null, removeMe);
//  }
//
//  @Test
//  public void testEmptyIterator() {
//    SimpleMap emptyMap = new SimpleMap();
//    Iterator<?> iter = emptyMap.iterator(null, null);
//    assertFalse(iter.hasNext());
//  }
//
//  @Test
//  public void testIteratorFiltersAll() {
//    sm.put("dummy", DUMMY_ENTRY);
//    Iterator<Map.Entry<String, Entry>> iter = sm.iterator(new Filter() {
//      @Override
//      public boolean keep(java.util.Map.Entry<String, Entry> mapEntry,
//          PropagationMode mode) {
//        return false;
//      }      
//    }, PropagationMode.JMS_QUEUE);
//    assertFalse(iter.hasNext());
//  }
//
//  @SuppressWarnings("serial")
//  @Test
//  public void testIteratorFilterNone() {
//    sm.put("dummy", DUMMY_ENTRY);
//    Iterator<Map.Entry<String, Entry>> iter = sm.iterator(new Filter() {
//      @Override
//      public boolean keep(java.util.Map.Entry<String, Entry> mapEntry,
//          PropagationMode mode) {
//        return true;
//      }      
//    }, PropagationMode.JMS_QUEUE);
//    int count = 0;
//    HashSet<String> keys = new HashSet<String>();
//    while (iter.hasNext()) {
//      keys.add(iter.next().getKey());
//      count++;
//    }
//    assertEquals(2, count);
//    assertEquals(new HashSet<String>() {{add("foo"); add("dummy");}}, keys);
//  }
//
//  @Test
//  public void testIteratorRemove() {
//    sm.put("dummy", DUMMY_ENTRY);
//    Iterator<Map.Entry<String, Entry>> iter = sm.iterator(new Filter() {
//      @Override
//      public boolean keep(java.util.Map.Entry<String, Entry> mapEntry,
//          PropagationMode mode) {
//        return true;
//      }      
//    }, PropagationMode.JMS_QUEUE);
//    assertEquals(2, sm.map.size());
//    assertNotNull(iter.next());
//    iter.remove();
//    assertEquals(1, sm.map.size());
//    assertNotNull(iter.next());
//    iter.remove();
//    assertEquals(0, sm.map.size());
//    int exceptionCount = 0;
//    try {
//      iter.next();
//    } catch (NoSuchElementException nsee) {exceptionCount++;}
//    assertEquals("Expected NoSuchElementException after the last element was retrieved", 1, exceptionCount);
//    try {
//      iter.remove();
//    } catch (IllegalStateException ise) {exceptionCount++;}
//    assertEquals("Expected IllegalStateException on last remove call", 2, exceptionCount);
//  }

}
