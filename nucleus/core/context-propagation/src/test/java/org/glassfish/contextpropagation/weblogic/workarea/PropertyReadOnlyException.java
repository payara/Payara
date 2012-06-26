package org.glassfish.contextpropagation.weblogic.workarea;

/**
 * <code>PropertyReadOnlyException</code> is thrown when a user tries
 * to modify {@link WorkContextMap} properties that are read-only.
 */
@SuppressWarnings("serial")
public class PropertyReadOnlyException extends Exception {

  public PropertyReadOnlyException() {
    super();
  }

  public PropertyReadOnlyException(String msg) {
    super(msg);
  }
}
