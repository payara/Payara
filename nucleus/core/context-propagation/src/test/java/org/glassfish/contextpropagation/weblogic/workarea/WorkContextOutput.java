package org.glassfish.contextpropagation.weblogic.workarea;

import java.io.DataOutput;
import java.io.IOException;


/**
 * <code>WorkConectOutput</code> is a primitive stream used for
 * marshaling {@link WorkContext} implementations. It is
 * necessary to limit the types that can be marshaled as part of a
 * <code>WorkArea</code> so that efficient representations can be
 * implemented in a variety of protocols. This representation can also
 * be transparent, enabling runtime filtering in SOAP and other
 * protocols.
 *
 * @see org.glassfish.contextpropagation.weblogic.workarea.WorkContextInput
 * @author Copyright (c) 2003 by BEA Systems Inc. All Rights Reserved.
 */
public interface WorkContextOutput extends DataOutput {
  /**
   * Writes an 8-bit, variable-length, string to the underlying data
   * stream. This is analgous to {@link DataOutput#writeBytes} but the
   * length of the string is also encoded.
   */
  public void writeASCII(String s) throws IOException;

  /**
   * Writes the implementation of {@link WorkContext} to the
   * underlying data stream. The actual class is encoded in the stream
   * so that remote java implementations can decode it.
   */
  public void writeContext(WorkContext ctx) throws IOException;
}
