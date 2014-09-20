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

package org.glassfish.contextpropagation.weblogic.workarea.spi;

import org.glassfish.contextpropagation.weblogic.workarea.WorkContextMap;

/**
 * SPI helper for checking access control on {@link org.glassfish.contextpropagation.weblogic.workarea.WorkContextMap} keys.
 * On the client this class does nothing, on the server derived implementations will check
 * against applicable security policies.
 *
 * @author Copyright (c) 2004 by BEA Systems, Inc. All Rights Reserved.
 * @exclude
 */
public abstract class WorkContextAccessController
{
  public static final int CREATE = 0;
  public static final int READ = 1;
  public static final int UPDATE = 2;
  public static final int DELETE = 3;

  private static WorkContextAccessController SINGLETON;

  protected WorkContextAccessController() {
    if (SINGLETON != null) {
      throw new IllegalStateException("Cannot register two instances of WorkContextAccessController");
    }
    SINGLETON = this;
  }

  /**
   * Check whethe access of type <code>type</code> is allowed
   * on <code>key</code>
   *
   * @param key the key to check access for
   * @param type the type of access required
   * @return true if access is allowed, false otherwise
   */
  public static boolean isAccessAllowed(String key, int type) {
    return getAccessController().checkAccess(key, type);
  }
  
  public static WorkContextMap getPriviledgedWorkContextMap(WorkContextMap map) {
	return getAccessController().getPriviledgedWrapper(map);
  }

  /**
   * SPI provider implementation of {@link #isAccessAllowed}
   * @param key
   * @param type
   * @return
   */
  protected boolean checkAccess(String key, int type) {
    return true;
  }
  
  /**
   * SPI provider implementation of {@link #getPriviledgedWorkContextMap}
   * @param map   
   * @return
   */
  protected WorkContextMap getPriviledgedWrapper(WorkContextMap map) {
    return map;
  }  

  private static WorkContextAccessController getAccessController() {
    if (SINGLETON == null) return new WorkContextAccessController() {};
    return SINGLETON;
  }
}
