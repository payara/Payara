package org.glassfish.contextpropagation.weblogic.workarea.spi;

import java.io.IOException;

import org.glassfish.contextpropagation.weblogic.workarea.WorkContext;
import org.glassfish.contextpropagation.weblogic.workarea.WorkContextOutput;



/**
 * <code>WorkContextEntry</code> encapsulates the runtime state of a
 * WorkArea property so that it can be marshaled transparently by an
 * underlying protocol.
 * @exclude
 */
public interface WorkContextEntry 
{
  public static final WorkContextEntry
    NULL_CONTEXT = new WorkContextEntryImpl(null, null, 1);
  
  public WorkContext getWorkContext();
  public String getName();
  public int getPropagationMode();
  public boolean isOriginator();

  /**
   * Writes the implementation of {@link org.glassfish.contextpropagation.weblogic.workarea.WorkContext} to the
   * {@link org.glassfish.contextpropagation.weblogic.workarea.WorkContextOutput} data stream.
   */
  public void write(WorkContextOutput out) throws IOException;
}
