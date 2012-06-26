package org.glassfish.contextpropagation.weblogic.workarea;

import java.io.IOException;
import java.io.Serializable;


/**
 * An implementation for propagating simple 8-bit ASCII string-based
 * {@link WorkContext}s.
 */
@SuppressWarnings("serial")
public class AsciiWorkContext implements PrimitiveWorkContext, Serializable
{
  private String str;
  
  public AsciiWorkContext() {
  }
  
  /* package */ AsciiWorkContext(String str) {
    this.str = str;
  }

  public String toString() { return str; }
  public Object get() { return str; }
  
  public boolean equals(Object obj) {
    if (obj instanceof AsciiWorkContext) {
      return ((AsciiWorkContext)obj).str.equals(str);
    }
    return false;
  }

  public void writeContext(WorkContextOutput out) throws IOException {
    out.writeASCII(str);
  }
  
  public void readContext(WorkContextInput in) throws IOException {
    str = in.readASCII();
  }
}
