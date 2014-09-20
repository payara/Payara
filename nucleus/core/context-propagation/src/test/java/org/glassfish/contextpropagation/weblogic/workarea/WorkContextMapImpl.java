/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.contextpropagation.weblogic.workarea;

import java.io.IOException;
import java.util.Iterator;

import org.glassfish.contextpropagation.weblogic.workarea.spi.WorkContextMapInterceptor;


/**
 * Implementation of <code>WorkContextMap</code>. This instance holds
 * a thread-local reference to the actual <code>WorkContextMap</code>
 * implementation that does most of the work.
 *
 * @see org.glassfish.contextpropagation.weblogic.workarea.WorkContextMap
 * @author Copyright (c) 2003 by BEA Systems Inc. All Rights Reserved.
 */
/* package */ final class WorkContextMapImpl
                implements WorkContextMap, WorkContextMapInterceptor
{
  @SuppressWarnings("rawtypes")
  private final static ThreadLocal localContextMap = new ThreadLocal();
    /*= AuditableThreadLocalFactory.createThreadLocal(
      new ThreadLocalInitialValue() {
        protected Object childValue(Object parentValue) {
          if (parentValue == null) return null;
          return ((WorkContextMapInterceptor)parentValue).
            copyThreadContexts(PropagationMode.WORK);
        }
      }
    );*/

  /* package */ WorkContextMapImpl() {
  }
  
  // Implementation of weblogic.workarea.WorkContextMap
  public WorkContext put(String key, WorkContext workContext,
                  int propagationMode) throws PropertyReadOnlyException {
    try {
      return getMap().put(key, workContext, propagationMode);
    }
    finally {
      if (getMapMaybe().isEmpty()) {
        reset();
      }
    }
  }

  public WorkContext put(String key, WorkContext workContext)
    throws PropertyReadOnlyException {
    try {
      return getMap().put(key, workContext);
    }
    finally {
      if (getMapMaybe().isEmpty()) {
        reset();
      }
    }
  }

  public WorkContext get(String key) {
    WorkContextMap map = getMapMaybe();
    if (map == null) {
      return null;
    }
    return map.get(key);
  }

  public WorkContext remove(String key) throws NoWorkContextException,
                                        PropertyReadOnlyException {
    WorkContextMap map = getMapMaybe();
    if (map == null) {
      throw new NoWorkContextException();
    }
    WorkContext prev = map.remove(key);
    // If that was the last element then remove the context from the
    // thread.
    if (map.isEmpty()) {
      reset();
    }

    return prev;
  }

  public int getPropagationMode(String key) {
    if(isEmpty())
      return PropagationMode.LOCAL;
    return getMapMaybe().getPropagationMode(key);
  }

  public boolean isPropagationModePresent(int propMode) {
    return getMapMaybe().isPropagationModePresent(propMode);
  }

  public boolean isEmpty() {
    return (getMapMaybe() == null);
  }

  @SuppressWarnings("unchecked")
  private void reset() {
    localContextMap.set(null);
  }

  @SuppressWarnings("rawtypes")
  public Iterator iterator() {
    WorkContextMap map = getMapMaybe();
    // REVIEW vmehra@bea.com 2004-Apr-23 -- instead of returning null
    // shouldn't we return empty iterator?
    return map == null ? null : map.iterator();
  }

  @SuppressWarnings("rawtypes")
  public Iterator keys() {
    WorkContextMap map = getMapMaybe();
    // REVIEW vmehra@bea.com 2004-Apr-23 -- instead of returning null
    // shouldn't we return empty iterator?
    return map == null ? null : map.keys();
  }

  public int version() {
    WorkContextMapInterceptor map
      = (WorkContextMapInterceptor)localContextMap.get();
    return (map != null ? map.version() : 0);
  }

  private final WorkContextMap getMapMaybe() {
    return (WorkContextMap)localContextMap.get();
  }

  @SuppressWarnings("unchecked")
  private final WorkContextMap getMap() {
    WorkContextMap map = (WorkContextMap)localContextMap.get();
    if (map == null) {
      map = new WorkContextLocalMap();
      localContextMap.set(map);
    }
    return map;
  }

  public WorkContextMapInterceptor getInterceptor() {
    return (WorkContextMapInterceptor)getMapMaybe();
  }

  @SuppressWarnings("unchecked")
  public void setInterceptor(WorkContextMapInterceptor interceptor) {
    localContextMap.set(interceptor);
  }

  // Implementation of weblogic.workarea.spi.WorkContextMapInterceptor

  public void sendRequest(WorkContextOutput out, int propagationMode) throws IOException {
    WorkContextMapInterceptor inter = getInterceptor();
    if (inter != null) {
      inter.sendRequest(out, propagationMode);
    }
  }

  public void sendResponse(WorkContextOutput out, int propagationMode) throws IOException {
    WorkContextMapInterceptor inter = getInterceptor();
    if (inter != null) {
      inter.sendResponse(out, PropagationMode.RMI);
    }
  }

  public void receiveRequest(WorkContextInput in)
    throws IOException
  {
    ((WorkContextMapInterceptor)getMap()).receiveRequest(in);
  }

  @SuppressWarnings("unchecked")
  public void receiveResponse(WorkContextInput in)
    throws IOException
  {
    // If we receive a null context back again but didn't have
    // anything then this is a no-op
    WorkContextMap map = getMapMaybe();
    if (in == null && map == null) {
      return;
    }
    else {
      // We need a map now if in is non-null
      if (map == null) {
        map = new WorkContextLocalMap();
        localContextMap.set(map);
      }
      // Merge all of the contexts into our thread-local map.
      ((WorkContextMapInterceptor)map).receiveResponse(in);
      // Importing actuall deleted everything so remove the thread-local
      if (map.isEmpty()) {
        reset();
      }
    }
  }

  public WorkContextMapInterceptor copyThreadContexts(int mode) {
    WorkContextMapInterceptor inter = getInterceptor();
    if (inter != null) {
      return inter.copyThreadContexts(mode);
    }
    return null;
  }

  public void restoreThreadContexts(WorkContextMapInterceptor contexts) {
    if (contexts != null) {
      ((WorkContextMapInterceptor)getMap()).restoreThreadContexts(contexts);
    }
  }

  public WorkContextMapInterceptor suspendThreadContexts() {
    WorkContextMapInterceptor map = getInterceptor();
    if (map != null) {
      reset();
    }
    return map;
  }

  @SuppressWarnings("unchecked")
  public void resumeThreadContexts(WorkContextMapInterceptor contexts) {
    localContextMap.set(contexts);
  }
}
