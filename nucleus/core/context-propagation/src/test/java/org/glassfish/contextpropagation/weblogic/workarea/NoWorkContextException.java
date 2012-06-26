package org.glassfish.contextpropagation.weblogic.workarea;


/**
 * <code>NoWorkContextException</code> is thrown when
 * {@link WorkContextMap} functions that require a
 * {@link WorkContext} are invoked without an existing
 * {@link WorkContext}.
 */
@SuppressWarnings("serial")
public class NoWorkContextException extends Exception 
{
  public NoWorkContextException() 
  {
    super();
  }
  
  public NoWorkContextException(String msg) 
  {
    super(msg);
  }
}
