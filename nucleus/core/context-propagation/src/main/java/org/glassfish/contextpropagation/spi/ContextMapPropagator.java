/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2012 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package org.glassfish.contextpropagation.spi;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.glassfish.contextpropagation.PropagationMode;
import org.glassfish.contextpropagation.internal.AccessControlledMap;
import org.glassfish.contextpropagation.wireadapters.WireAdapter;

/**
 * Used by messaging protocols to integrate with the context-propagation framework.
 */
public interface ContextMapPropagator { 
  /**
   * Transfers the entries with the specified {@link org.glassfish.contextpropagation.PropagationMode}
   * from {@link org.glassfish.contextpropagation.ContextMap}s,
   * in thread-local storage, to
   * the protocol {@link java.io.OutputStream} while it is
   * sending an out-bound request. This must be called at the
   * appropriate time before any thread context-switches.  This call
   * does not affect the contents of thread-local storage in any way.
   *
   * @param out The{@link java.io.OutputStream} that the protocol wants the data written to
   * @param propagationMode The {@link org.glassfish.contextpropagation.PropagationMode} being
   * utilized in this call. Only entries that support that propagation mode are propagated.
   * @exception IOException if the data cannot be serialized.
   */
  public void sendRequest(OutputStream out, PropagationMode propagationMode) throws IOException;
  
  /**
   * Transfers the entries with the specified {@link org.glassfish.contextpropagation.PropagationMode}
   * from {@link org.glassfish.contextpropagation.ContextMap}s,
   * in thread-local storage, to
   * the protocol {@link java.io.OutputStream} while it is
   * sending an out-bound response. This must be called at the
   * appropriate time before any thread context-switches.  This call
   * does not affect the contents of thread-local storage in any way.
   *
   * @param out The{@link java.io.OutputStream} that the protocol wants the data written to
   * @param propagationMode The {@link org.glassfish.contextpropagation.PropagationMode} being
   * utilized in this call. Only entries that support that propagation mode are propagated.
   * @exception IOException if the data cannot be serialized.
   */
  public void sendResponse(OutputStream out, PropagationMode propagationMode) throws IOException;
  
  /**
   * Deserializes context from an {@link java.io.InputStream} provided by
   * a protocol that is receiving a request. This must be
   * called at the appropriate time after any thread context-switches.
   * All existing thread-local contexts are overwritten, although in
   * general the thread execution model should ensure that there are
   * no existing thread-local contexts.
   * 
   * While the receiver will attempt to read all and only the context propagation
   * data, it may not do so under unusual circumstances such as when there is a
   * bug in a third-party context implementation. For that reason, if IOException
   * is thrown, the sender is responsible for positioning the stream to the point
   * immediately after the context-propagation data.
   *
   * @param in A {@link java.io.InputStream} provided by the protocol and containing the serialized contexts
   * serialized context propagation bytes and no more. 
   * @exception IOException if the data cannot be read.
   */
  public void receiveRequest(InputStream in) throws IOException;
  
  /**
   * Deserializes context from an {@link java.io.InputStream} provided by
   * a protocol that is receiving a request. This must be
   * called at the appropriate time after any thread context-switches.
   * All existing thread-local contexts with the specified propagation mode are
   * removed before the context entries are read from the specified input stream
   * <code>in</code> may be null which means that the remote server removed
   * all of the contexts with propagation modes that include the specified
   * propagation mode.
   *
   * @param in A {@link java.io.InputStream} provided by the protocol and containing the serialized contexts
   * serialized context propagation bytes and no more. Its read methods
   * must return -1 when the end of the context data has been reached
   * @param mode The {@link org.glassfish.contextpropagation.PropagationMode}
   * associated to the protocol invoking this method.
   * @exception IOException if the data cannot be read.
   */
  public void receiveResponse(InputStream in, PropagationMode mode) throws IOException;
  
  /**
   * Copies the entries that have the propagation mode THREAD to this thread's
   * ContextMap.
   *
   * @param contexts an {@link ContextMapInterceptor} obtained via
   * {@link #copyThreadContexts}.
   */
  public void restoreThreadContexts(AccessControlledMap contexts);
  
  /**
   * A protocol that propagates context data can choose an alternate
   * WireAdapter, and thus a different encoding format on the wire. 
   * @param wireAdapter
   */
  public void useWireAdapter(WireAdapter wireAdapter);
}
