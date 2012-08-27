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

import javax.naming.Context;
import javax.naming.NamingException;

import org.glassfish.contextpropagation.weblogic.workarea.spi.WorkContextAccessController;
import org.glassfish.contextpropagation.weblogic.workarea.spi.WorkContextMapInterceptor;



/**
 * <code>WorkContextHelper</code> allows internal users to obtain and
 * modify {@link WorkContext}s. The class APIs allow for
 * replacement of the implementation - although there are currently no
 * use-cases for this. Typical usages follow. To obtain the current
 * {@link WorkContextMap} for update:
 * <p> <pre>
 * WorkContextMap interceptor 
 *   = WorkContextHelper.getWorkContextHelper().getWorkContextMap();
 *</pre>
 *
 * To obtain a {@link org.glassfish.contextpropagation.weblogic.workarea.spi.WorkContextMapInterceptor}:
 * <p> <pre>
 * WorkContextMapInterceptor interceptor 
 *   = WorkContextHelper.getWorkContextHelper().getInterceptor();
 *</pre>
 *
 * @exclude
 * @see org.glassfish.contextpropagation.weblogic.workarea.WorkContextMap
 * @see org.glassfish.contextpropagation.weblogic.workarea.spi.WorkContextMapInterceptor
 *
 * @author Copyright (c) 2003 by BEA Systems Inc. All Rights Reserved.
 */
public class WorkContextHelper 
{
  private static final String WORK_CONTEXT_BINDING = "WorkContextMap";
  private static final WorkContextMapImpl map = new WorkContextMapImpl();
  
  private static WorkContextHelper singleton = new WorkContextHelper();

  // Prevent outsiders creating one.
  protected WorkContextHelper() { }
  
  /**
   * Get the WorkContextHelper singleton.
   * 
   * @return A suitable WorkContextHelper implementation for client or
   * server.
   */
  public static WorkContextHelper getWorkContextHelper() {
    return singleton;
  }
  
  /**
   * Set the WorkContextHelper singleton. This should be set at startup
   * and not synchronized.
   * 
   * @param wam - a suitable WorkContextHelper implementation for client
   * or server.
   */
  public static void setWorkContextHelper(WorkContextHelper wam) {
    throw new IllegalArgumentException
      ("WorkContextHelper does not currently support replacement");
  }
  
  public WorkContextMap getWorkContextMap() {
    return map;
  }

  //This is F&F API introduced for Oracle DMS for faster WorkContext reads
  //PriviledgedWorkContextMap does a read as KernelId for the given two 
  //DMS specific WorkContext keys
  public WorkContextMap getPriviledgedWorkContextMap() {
    return WorkContextAccessController.getPriviledgedWorkContextMap(map);
  }
    
  /**
   * Get the singleton instance of the current
   * {@link org.glassfish.contextpropagation.weblogic.workarea.spi.WorkContextMapInterceptor}..
   * 
   * @return A suitable WorkContextMapInterceptor implementation for
   * client or server.
   */
  public WorkContextMapInterceptor getInterceptor() {
    return map;
  }
  
  /**
   * Get the singleton thread-local instance of the current
   * {@link org.glassfish.contextpropagation.weblogic.workarea.spi.WorkContextMapInterceptor}, or null if there is no
   * {@link WorkContextMap} for the current thread.
   * 
   * @return A suitable WorkContextMapInterceptor implementation for
   * client or server.
   */
  public WorkContextMapInterceptor getLocalInterceptor() {
    return map.getInterceptor();
  }

  /**
   * Create an instance of {@link
   * org.glassfish.contextpropagation.weblogic.workarea.spi.WorkContextMapInterceptor} for the purposes
   * of serialization. Thread infection can be achieved via {@link
   * #setLocalInterceptor}.
   * 
   * @return A suitable WorkContextMapInterceptor implementation for
   * client or server.
   */
  public WorkContextMapInterceptor createInterceptor() {
    return new WorkContextLocalMap();
  }

  /**
   * Take an {@link org.glassfish.contextpropagation.weblogic.workarea.spi.WorkContextMapInterceptor}
   * object and make it the current map. This bypasses serialization
   * schemes for the {@link WorkContextMap}. This allows callers to
   * separate serialization from thread infection and vice versa.
   */
  public void setLocalInterceptor(WorkContextMapInterceptor interceptor) {
    map.setInterceptor(interceptor);
  }

  public static void bind(Context ctx) throws NamingException {
    ctx.bind(WORK_CONTEXT_BINDING, getWorkContextHelper().getWorkContextMap());
  }

  public static void unbind(Context ctx) throws NamingException {
    ctx.unbind(WORK_CONTEXT_BINDING);
  }
}
