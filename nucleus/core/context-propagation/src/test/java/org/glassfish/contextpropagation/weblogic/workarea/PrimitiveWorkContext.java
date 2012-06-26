package org.glassfish.contextpropagation.weblogic.workarea;



/**
 * <code>PrimitiveWorkContext</code> is a marker interface used for
 * marshaling and unmarshaling primitive user data in a
 * <code>WorkArea</code>. <code>PrimitiveWorkContext</code>s contain
 * only a single data item that can be accessed via
 * {@link #get}. They are provided, not only as a
 * convenience, but because they can be passed between servers without
 * users needing to provide a {@link WorkContext} implementation
 * on the target. <code>PrimitiveWorkContext</code> implementations
 * must provide a public no-arg constructor.
 *
 * @author Copyright (c) 2003 by BEA Systems Inc. All Rights Reserved.
 */
public interface PrimitiveWorkContext extends WorkContext {
  /**
   * Returns the data associated with this <code>PrimitiveWorkContext</code>.
   */
  public Object get();
}
