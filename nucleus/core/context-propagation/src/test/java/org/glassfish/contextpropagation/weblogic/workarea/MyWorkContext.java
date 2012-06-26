package org.glassfish.contextpropagation.weblogic.workarea;

import java.io.IOException;


public class MyWorkContext implements WorkContext {
  long l;

  @Override
  public void writeContext(WorkContextOutput out) throws IOException {
    out.writeLong(100L);
  }

  @Override
  public void readContext(WorkContextInput in) throws IOException {
    l = in.readLong();
  }

}
