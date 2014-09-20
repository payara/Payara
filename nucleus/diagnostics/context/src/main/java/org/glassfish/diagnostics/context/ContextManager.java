/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.diagnostics.context;

import org.glassfish.logging.annotation.LogMessagesResourceBundle;
import org.jvnet.hk2.annotations.Contract;

import org.glassfish.logging.annotation.LoggerInfo;

import java.util.logging.Logger;

/**
 * The ContextManager provides access to diagnostics Contexts.
 */
@Contract
public interface ContextManager
{
  /**
   * The key under which instances of Context will be created
   * and found in the ContextMap.
   *
   * This key will be used by the ContextMap implementation
   * to label the data belonging to this Context when that
   * data is being propagated. Remote systems attempting to
   * use that data (e.g. to construct a propagated Context)
   * will use the value of this key to find the propagated
   * data. Therefore the value of this key should not be
   * changed...ever!
   */
  public static final String WORK_CONTEXT_KEY =
      "org.glassfish.diagnostics.context.Context";

  //
  // Logging metadata for this module.
  //
  // Message ids must be of the form NCLS-DIAG-##### where ##### is an
  // integer in the range reserved for this module: [03000, 03999].
  //
  // Message ids are to be explicit in code (rather than generated) to
  // facilitate fast locating by developers.
  //
  // Message ids are to be annotated with @LogMessageInfo

  @LogMessagesResourceBundle
  public static final String LOG_MESSAGE_RESOURCE = "org.glassfish.diagnostics.context.LogMessages";

  @LoggerInfo(subsystem="DIAG", description="Diagnostcis Context Logger", publish=true)
  public static final String LOGGER_NAME =
      "javax.enterprise.diagnostics.context.base";

  public static final Logger LOGGER = Logger.getLogger(LOGGER_NAME, LOG_MESSAGE_RESOURCE);

  /**
   * Get the Context associated with the currently executing task,
   * creating a new Context if required.
   */
  public Context getContext();
}
