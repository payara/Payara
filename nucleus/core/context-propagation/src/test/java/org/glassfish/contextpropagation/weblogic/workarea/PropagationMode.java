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
