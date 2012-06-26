package org.glassfish.contextpropagation.weblogic.workarea;

import java.io.IOException;


/**
 * <code>WorkContext</code> is a marker interface used for marshaling
 * and unmarshaling user data in a <code>WorkArea</code>. The
 * interfaces {@link WorkContextOutput} and
 * {@link WorkContextInput} will only allow primtive types and
 * objects implementing <code>WorkContext</code> to be marshaled. This
 * limits the type surface area that needs to be dealt with by
 * underlying protocols. <code>WorkContext</code> is analogous to
 * {@link java.io.Externalizable} but with some restrictions on the types
 * that can be marshaled. Advanced {@link java.io.Externalizable}
 * features, such as enveloping, are not supported - implementations
 * should provide their own versioning scheme if
 * necessary. <code>WorkContext</code> implementations must provide a
 * public no-arg constructor.
 *
 * @author Copyright (c) 2003 by BEA Systems Inc. All Rights Reserved.
 */
public interface WorkContext {
  /**
   * Writes the implementation of <code>WorkContext</code> to the
   * {@link WorkContextOutput} data stream.
   */
  public void writeContext(WorkContextOutput out) throws IOException;

  /**
   * Reads the implementation of <code>WorkContext</code> from the
   * {@link WorkContextInput} data stream.
   */
  public void readContext(WorkContextInput in) throws IOException;
}
