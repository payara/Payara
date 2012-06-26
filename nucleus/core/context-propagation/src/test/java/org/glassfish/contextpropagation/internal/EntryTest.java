package org.glassfish.contextpropagation.internal;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import mockit.Deencapsulation;

import org.glassfish.contextpropagation.PropagationMode;
import org.glassfish.contextpropagation.internal.Entry.ContextType;
import org.junit.Test;

public class EntryTest {
  
  private static final boolean IS_ORIGIN = true;
  private static final boolean ALLOW_ALL_TO_READ = true;

  @Test
  public void validatel() {
    createContext("value", PropagationMode.defaultSet(), ContextType.ASCII_STRING, IS_ORIGIN, ALLOW_ALL_TO_READ).validate();
  }
  
  @Test(expected=IllegalStateException.class)
  public void validateNullValue() {
    createContext(null, PropagationMode.defaultSet(), ContextType.ASCII_STRING, IS_ORIGIN, ALLOW_ALL_TO_READ).validate();
  }

  @Test(expected=IllegalStateException.class)
  public void validateNullPropMode() {
    createContext("value", null, ContextType.ASCII_STRING, IS_ORIGIN, ALLOW_ALL_TO_READ).validate();
  }

  @Test(expected=IllegalStateException.class)
  public void validateNullContextType() {
    createContext("value", PropagationMode.defaultSet(), null, IS_ORIGIN, ALLOW_ALL_TO_READ).validate();
  }

  @Test(expected=IllegalStateException.class)
  public void validateNullISOriginator() {
    createContext("value", PropagationMode.defaultSet(), ContextType.ASCII_STRING, (Boolean) null, ALLOW_ALL_TO_READ).validate();
  }

  @Test(expected=IllegalStateException.class)
  public void validateNullAllowAllToRead() {
    createContext("value", PropagationMode.defaultSet(), ContextType.ASCII_STRING, IS_ORIGIN, null).validate();
  }


  private Entry createContext(String value,
      EnumSet<PropagationMode> propModes, ContextType type,
      Boolean isOrigin, Boolean allowAllToRead) {
    Entry entry = new Entry(value, propModes, type);
    return entry.init(isOrigin, allowAllToRead);
  }

  @Test
  public void testToContextTypeFromNumberClass() {
    assertEquals(ContextType.ATOMICINTEGER, ContextType.fromNumberClass(AtomicInteger.class));
    assertEquals(ContextType.ATOMICLONG, ContextType.fromNumberClass(AtomicLong.class));
    assertEquals(ContextType.BIGDECIMAL, ContextType.fromNumberClass(BigDecimal.class));
    assertEquals(ContextType.BIGINTEGER, ContextType.fromNumberClass(BigInteger.class));
    assertEquals(ContextType.BYTE, ContextType.fromNumberClass(Byte.class));
    assertEquals(ContextType.DOUBLE, ContextType.fromNumberClass(Double.class));
    assertEquals(ContextType.FLOAT, ContextType.fromNumberClass(Float.class));
    assertEquals(ContextType.INT, ContextType.fromNumberClass(Integer.class));
    assertEquals(ContextType.LONG, ContextType.fromNumberClass(Long.class));
    assertEquals(ContextType.SHORT, ContextType.fromNumberClass(Short.class));
  }
  
  @Test
  public void testToContextTypeOrdinal() {
    ContextType[] byOrdinal = Deencapsulation.getField(ContextType.class, "byOrdinal");
    for (int i = 0; i < byOrdinal.length; i++) {
      assertEquals(i, ContextType.fromOrdinal(i).ordinal());
    }
    assertEquals(ContextType.values().length, byOrdinal.length);
  }

}
