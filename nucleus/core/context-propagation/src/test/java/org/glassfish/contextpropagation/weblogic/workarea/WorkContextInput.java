package org.glassfish.contextpropagation.weblogic.workarea;

import java.io.DataInput;
import java.io.IOException;



/**
 * <code>WorkContextInput</code> is a primitive stream used for
 * unmarshaling {@link WorkContext} implementations.
 *
 * @see weblogic.workarea.WorkContextOuput
 * @author Copyright (c) 2003 by BEA Systems Inc. All Rights Reserved.
 */
public interface WorkContextInput extends DataInput {
  /**
   * Reads an 8-bit, variable-length, ASCII string from the underlying
   * data stream.
   */
  public String readASCII() throws IOException;

  /**
   * Reads a {@link WorkContext} from the underlying
   * stream. The class is encoded as part of the marshaled form in a
   * protocol-dependent fashion to allow remote java implementations
   * to decode it.
   */
  public WorkContext readContext() throws IOException, ClassNotFoundException;
}
