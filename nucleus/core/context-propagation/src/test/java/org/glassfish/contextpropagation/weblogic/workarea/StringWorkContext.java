package org.glassfish.contextpropagation.weblogic.workarea;

import java.io.IOException;
import java.io.Serializable;


/**
 * An implementation for propagating simple string-based {@link
 * WorkContext}s.
 */
@SuppressWarnings("serial")
public class StringWorkContext implements PrimitiveWorkContext, Serializable
{
  private String str;
  
  public StringWorkContext() {
  }
  
  /* package */ StringWorkContext(String str) {
    this.str = str;
  }

  public String toString() { return str; }
  public Object get() { return str; }

  public boolean equals(Object obj) {
    if (obj instanceof StringWorkContext) {
      return ((StringWorkContext)obj).str.equals(str);
    }
    return false;
  }
  
  public void writeContext(WorkContextOutput out) throws IOException {
    out.writeUTF(str);
  }
  
  public void readContext(WorkContextInput in) throws IOException {
    str = in.readUTF();
  }
}
