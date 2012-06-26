package org.glassfish.contextpropagation.weblogic.workarea;


/**
 * <code>PropagationMode</code> defines the propagation properties of
 * {@link WorkContext}s. {@link WorkContext}s can be
 * propagated locally, across threads, across RMI invocations, across
 * JMS queues and topics, and across SOAP messages. 
 *
 * In general <code>PropagationMode</code>s are provided as a
 * bitwise-OR of desired values. If no <code>PropagationMode</code> is
 * specified when a {@link WorkContext} is created then the
 * default value <code>DEFAULT</code> is used which propagates data
 * across remote and local calls in the same thread only.
 *
 * @see org.glassfish.contextpropagation.weblogic.workarea.WorkContextMap
 *
 * @author Copyright (c) 2003 by BEA Systems Inc. All Rights Reserved.
 */
public interface PropagationMode 
{
  /**
   * Propagate a WorkContext only for the scope of the current thread.
   */
  public static final int LOCAL = 1;

  /**
   * Propagate a WorkContext across Work instances.
   */
  public static final int WORK = 2;

  /**
   * Propagate a WorkContext across RMI invocations.
   */
  public static final int RMI = 4;

  /**
   * Propagate a WorkContext across global transactions.
   */
  public static final int TRANSACTION = 8;

  /**
   * Propagate a WorkContext to JMS queues.
   */
  public static final int JMS_QUEUE = 16;

  /**
   * Propagate a WorkContext to JMS topics.
   */
  public static final int JMS_TOPIC = 32;

  /**
   * Propagate a WorkContext across SOAP messages.
   */
  public static final int SOAP = 64;

  /**
   * Propagate a WorkContext from a MIME header or HTTP cookie.
   */
  public static final int MIME_HEADER = 128;

  /**
   * Propagate a WorkContext downstream from the caller only.
   */
  public static final int ONEWAY = 256;

  /**
   * Propagate a WorkContext across remote invocations and local
   * invocations in the same thread.
   */
  public static final int GLOBAL = RMI | JMS_QUEUE | SOAP | MIME_HEADER;

  /**
   * The default <code>PropagationMode</code> which is equivalent to
   * <code>GLOBAL</code>.
   */
  public static final int DEFAULT = GLOBAL;
}
