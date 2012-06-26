package org.glassfish.contextpropagation.weblogic.workarea.spi;

import java.io.IOException;

import org.glassfish.contextpropagation.weblogic.workarea.WorkContextInput;
import org.glassfish.contextpropagation.weblogic.workarea.WorkContextOutput;



/**
 * Interceptor SPI that {@link org.glassfish.contextpropagation.weblogic.workarea.WorkContext} provider
 * implementations should call on to marshal and unmarshal the current
 * {@link org.glassfish.contextpropagation.weblogic.workarea.WorkContextMap}.
 *
 * An instance of the workarea runtime implementing
 * <code>WorkContextMapInterceptor</code> can be obtained as follows:
 * <p> <pre>
 * WorkContextMapInterceptor interceptor 
 *   = WorkContextHelper.getWorkContextHelper().getInterceptor();
 *</pre>
 *
 * It is also possible to check the current thread for the presence of
 * a {@link org.glassfish.contextpropagation.weblogic.workarea.WorkContextMap}, and obtain the interceptor, in one
 * call:
 * <p> <pre>
 * WorkContextMapInterceptor interceptor 
 *   = WorkContextHelper.getWorkContextHelper().getLocalInterceptor();
 *</pre>
 *
 * Note that the latter form is not suitable for service providers
 * that need to infect the current thread with
 * {@link org.glassfish.contextpropagation.weblogic.workarea.WorkContextMap} data since they will always need to
 * obtain an interceptor instance.
 *
 * There are three use cases for interceptor usage, one for remote
 * invocations and two for local invocations. In the remote case the
 * send/receive functions are used to marshal and unmarshal
 * WorkContextMap data from an appropriate stream. The stream is
 * provided by the service provider and may encode the data as binary,
 * XML or whatever.
 *
 * In the local case service providers generally need to copy context
 * data from one thread to another. This is done via copy/restore and
 * the interceptor will ensure that only data that should be copied
 * is. The other local case is where the service provider needs to
 * propagate context data remotely in a non-standard way. In this case
 * the the send/receive calls are used first to marshal the data and
 * then suspend/resume used to prevent the data being marshaled by
 * other remote mechanisms.
 *
 * @see org.glassfish.contextpropagation.weblogic.workarea.WorkContextMap
 * @see org.glassfish.contextpropagation.weblogic.workarea.PropagationMode
 *
 * @author Copyright (c) 2003 by BEA Systems Inc. All Rights Reserved.
 * @exclude
 */
public interface WorkContextMapInterceptor {
  /**
   * Transfer {@link org.glassfish.contextpropagation.weblogic.workarea.WorkContext}s from thread-local storage to
   * the protocol-specific {@link org.glassfish.contextpropagation.weblogic.workarea.WorkContextOutput} before
   * making an outbound request. This must be called at the
   * appropriate time before any thread context-switches.  This call
   * does not affect the contents of thread-local storage in any way.
   *
   * @param out The{@link org.glassfish.contextpropagation.weblogic.workarea.WorkContextOutput} implementation to
   * marshal the {@link org.glassfish.contextpropagation.weblogic.workarea.WorkContextMap} data to.
   * @param propagationMode The {@link org.glassfish.contextpropagation.weblogic.workarea.PropagationMode} being
   * utilized in this call. This is a hint to the runtime system.
   * @exception IOException if the data cannot be serialized.
   */
  public void sendRequest(WorkContextOutput out, int propagationMode) throws IOException;
  
  /**
   * Transfer {@link org.glassfish.contextpropagation.weblogic.workarea.WorkContext}s from thread-local storage to
   * the protocol-specific {@link org.glassfish.contextpropagation.weblogic.workarea.WorkContextOutput} before
   * sending a response. This must be called at the appropriate time
   * before any thread context-switches.  This call does not affect
   * the contents of thread-local storage in any way.
   *
   * @param out The{@link org.glassfish.contextpropagation.weblogic.workarea.WorkContextOutput} implementation to
   * marshal the {@link org.glassfish.contextpropagation.weblogic.workarea.WorkContextMap} data to.
   * @param propagationMode The {@link org.glassfish.contextpropagation.weblogic.workarea.PropagationMode} being
   * utilized in this call. This is a hint to the runtime system.
   * @exception IOException if the data cannot be serialized.
   */
  public void sendResponse(WorkContextOutput out, int propagationMode) throws IOException;
  
  /**
   * Transfer the protocol-specific {@link org.glassfish.contextpropagation.weblogic.workarea.WorkContextInput} data
   * to thread-local storage after receiving a request. This must be
   * called at the appropriate time after any thread context-switches.
   * All existing thread-local contexts are overwritten, athough in
   * general the thread execution model should ensure that there are
   * no existing thread-local contexts.
   *
   * @param in The {@link org.glassfish.contextpropagation.weblogic.workarea.WorkContextInput} implementation to
   * read {@link org.glassfish.contextpropagation.weblogic.workarea.WorkContextMap} data from.
   * @exception IOException if the data cannot be read.
   */
  public void receiveRequest(WorkContextInput in)
    throws IOException;
  
  /**
   * Transfer the protocol-specific {@link org.glassfish.contextpropagation.weblogic.workarea.WorkContextInput} data
   * to thread-local storage after receiving a response. This must be
   * called at the appropriate time after any thread context-switches.
   * The data is merged with any existing thread-local contexts.
   * <code>in</code> may be null which means that the callee removed
   * all of the {@link org.glassfish.contextpropagation.weblogic.workarea.WorkContext}s.
   *
   * @param in The {@link org.glassfish.contextpropagation.weblogic.workarea.WorkContextInput} implementation to
   * read {@link org.glassfish.contextpropagation.weblogic.workarea.WorkContextMap} data from.
   * @exception IOException if the data cannot be read.
   */
  public void receiveResponse(WorkContextInput in) 
    throws IOException;
  
  /**
   * Return the thread-local contexts that should be propagated to new
   * threads. Callers should not attempt to modify or use the returned
   * object in any way other than to call
   * {@link #restoreThreadContexts} or one of the interceptor functions.
   * The existing thread-local contexts are not modified in any way. While
   * the returned {@link org.glassfish.contextpropagation.weblogic.workarea.WorkContextMap} is a new instance,
   * the contained values are not, so developers should not attempt to update the
   * contents of the values.
   *
   * @return a {@link WorkContextMapInterceptor} that contains the
   * {@link org.glassfish.contextpropagation.weblogic.workarea.WorkContextMap} data to be propagated.
   * @param mode only contexts with this {@link org.glassfish.contextpropagation.weblogic.workarea.PropagationMode}
   * will be copied. mode can
   * be a bit-wise OR of {@link org.glassfish.contextpropagation.weblogic.workarea.PropagationMode}s
   */
  public WorkContextMapInterceptor copyThreadContexts(int mode);
  
  /**
   * Restore thread-local contexts from <code>contexts</code> that
   * should be propagated to new threads. <code>contexts</code> must
   * have been obtained as a result of calling {@link
   * #copyThreadContexts}. Existing thread-local contexts are
   * overwritten, although in general there should be none.
   *
   * @param contexts an {@link WorkContextMapInterceptor} obtained via
   * {@link #copyThreadContexts}.
   */
  public void restoreThreadContexts(WorkContextMapInterceptor contexts);
  
  /**
   * Remove and return all the thread-local contexts from the current
   * thread.  thread. Callers should not attempt to modify or use the
   * returned object in any way other than to call
   * {@link #resumeThreadContexts} or another interceptor function.
   * The intention of this call is that a service provider will have already called
   * {@link #sendRequest} or {@link #sendResponse} to
   * transfer thread-local contexts to a remote message and this call
   * is used to prevent other RMI mechnisms being involved.
   *
   * @return a {@link WorkContextMapInterceptor} that contains the
   * {@link org.glassfish.contextpropagation.weblogic.workarea.WorkContextMap} data to be saved.
  */
  public WorkContextMapInterceptor suspendThreadContexts();
  
  /**
   * Resume thread-local contexts from
   * <code>contexts</code>. <code>contexts</code> must have been
   * obtained as a result of calling
   * {@link #suspendThreadContexts}. All existing thread-local
   * contexts are overwritten, although in general there should be
   * none.
   *
   * @param contexts a {@link WorkContextMapInterceptor} obtained via
   * {@link #suspendThreadContexts}.
   */
  public void resumeThreadContexts(WorkContextMapInterceptor contexts);

  /**
   * Return a version indicator for the current {@link org.glassfish.contextpropagation.weblogic.workarea.WorkContextMap}. 
   * The version will change when
   * properties in the map are added, removed or changed.
   *
   * @return version indicator.
   */
  public int version();
}
