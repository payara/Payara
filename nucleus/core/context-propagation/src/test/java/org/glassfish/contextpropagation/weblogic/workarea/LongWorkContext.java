package org.glassfish.contextpropagation.weblogic.workarea;

import java.io.IOException;
import java.io.Serializable;


/**
 * An implementation for propagating simple long-based
 * {@link WorkContext}s.
 */
@SuppressWarnings("serial")
public class LongWorkContext implements PrimitiveWorkContext, Serializable
{
  private long longValue;
  
  public LongWorkContext() {
  }
  
  /* package */ LongWorkContext(long l) {
    longValue = l;
  }

  public Object get() {
    return new Long(longValue());
  }
  
  public String toString() {
    return "" + longValue;
  }

  public boolean equals(Object obj) {
    if (obj instanceof LongWorkContext) {
      return ((LongWorkContext)obj).longValue == longValue;
    }
    return false;
  }

  public long longValue() {
    return longValue;
  }
  
  public void writeContext(WorkContextOutput out) throws IOException {
    out.writeLong(longValue);
  }
  
  public void readContext(WorkContextInput in) throws IOException {
    longValue = in.readLong();
  }
}
