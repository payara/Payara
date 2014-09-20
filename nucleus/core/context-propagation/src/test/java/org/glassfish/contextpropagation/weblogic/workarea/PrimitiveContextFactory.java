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
import java.io.Serializable;


/**
 * <code>PrimitiveContextFactory</code> provides internal users and
 * layered products convenience mechanisms for creating
 * {@link WorkContext}s instances containing primitive data.
 *
 * @see org.glassfish.contextpropagation.weblogic.workarea.WorkContextMap
 *
 * @author Copyright (c) 2003 by BEA Systems Inc. All Rights Reserved.
 */
public class PrimitiveContextFactory 
{
  /**
   * Creates a short {@link WorkContext} key based on
   * <code>key</code>. Short keys are more efficiently serialized. The
   * returned key will always be the same for the same values of
   * <code>key</code>.
   *
   * @see org.glassfish.contextpropagation.weblogic.workarea.WorkContextMap#put
   */
  public static String createEncodedKey(String key) {
    int hash = key.hashCode();
    StringBuffer code = new StringBuffer();
    while (hash != 0) {
      code.append((char)(59 + (hash & 0x3F)));
      hash >>>= 6;
    }
    return code.toString();
  }

  /**
   * Creates a new {@link WorkContext} containing Unicode String
   * context data.
   *
   * @see org.glassfish.contextpropagation.weblogic.workarea.WorkContextMap#put
   */
  public static WorkContext create(String ctx) {
    return new StringWorkContext(ctx);
  }

  /**
   * Creates a new {@link WorkContext} containing 64-bit long
   * context data.
   *
   * @see org.glassfish.contextpropagation.weblogic.workarea.WorkContextMap#put
   */
  public static WorkContext create(long ctx) {
    return new LongWorkContext(ctx);
  }

  /**
   * Creates a new {@link WorkContext} containing 8-bit ASCII
   * context data.
   *
   * @see org.glassfish.contextpropagation.weblogic.workarea.WorkContextMap#put
   */
  public static WorkContext createASCII(String ctx) {
    return new AsciiWorkContext(ctx);
  }

  /**
   * Creates a new {@link WorkContext} containing opaque
   * Serializable context data. <b>CAUTION: use with care</b>. Data
   * propagated in this way will be opaque to underlying protocol
   * implementations and will generally be less efficient.
   *
   * @see org.glassfish.contextpropagation.weblogic.workarea.WorkContextMap#put
   */
  public static WorkContext create(Serializable ctx) throws IOException {
    return new SerializableWorkContext(ctx);
  }
  
  /**
   * Creates a new {@link WorkContext} containing opaque
   * Serializable context data. The context data is not serialized at the time 
   * of creation of WorkContext but only when the WorkContextMap needs to 
   * propagate the WorkContext entries. This allows the Serializable context 
   * data to be updated even after it is put in the WorkContextMap. 
   * <b>CAUTION: use with care</b>. Data
   * propagated in this way will be opaque to underlying protocol
   * implementations and will generally be less efficient.
   *
   * @see org.glassfish.contextpropagation.weblogic.workarea.WorkContextMap#put
   */
  public static WorkContext createMutable(Serializable ctx) 
    throws IOException {
    return new SerializableWorkContext(ctx, true /*enableUpdate*/);
  }
}

