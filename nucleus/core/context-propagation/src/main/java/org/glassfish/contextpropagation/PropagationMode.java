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
package org.glassfish.contextpropagation;

import java.util.EnumSet;

/**
 * The propagation mode determines if a work context is propagated. The creator
 * of a work context declares a set of propagation modes that is allowed by
 * the context. When protocols send or receive messages, they will propagate 
 * any work context that includes the protocol's specific propagation mode.
 *  - LOCAL Context lives only in the current thread
 *  - THREAD Context will be propagated from thread to thread
 *  - RMI propagation along RMI messages
 *  - JMS_QUEUE propagation to JMS queues
 *  - JMS_TOPIC propagation to JMS topics
 *  - SOAP propagation along a SOAP message
 *  - MIME_HEADER propagation from a mime header or http cookie
 *  - ONEWAY propagation in requests only, not in responses.
 */
 /*  For future implementation
 *  - SECRET indicates that the context should not be exposed via the ContextMap APIs, it could be used to hide the individual constituents of a ViewCapable
 *  - IIOP propagation with IIOP messages
 */
public enum PropagationMode {
  LOCAL, THREAD, RMI, TRANSACTION, JMS_QUEUE, JMS_TOPIC, SOAP, MIME_HEADER, 
  ONEWAY; /*SECRET, IIOP, CUSTOM; think about extension */
  private static PropagationMode[] byOrdinal = createByOrdinal();

  private static PropagationMode[] createByOrdinal() {
    PropagationMode[] values = values();
    PropagationMode[] byOrdinal = new PropagationMode[values.length];
    for (PropagationMode value : values) {
      byOrdinal[value.ordinal()] = value;
    }
    return byOrdinal;
  }

  /**
   * A utility method for getting a PropagationMode given that we know its
   * ordinal value. Developers are not expected to use this function. It is
   * mostly used by WireAdapters.
   * @param ordinal
   * @return The propagation mode that has the specified ordinal value.
   */
  public static PropagationMode fromOrdinal(int ordinal) {
    return byOrdinal[ordinal];
  }
  
  /**
   * @return The default set of propagation modes: THREAD, RMI, JMS_QUEUE, SOAP and MIME_HEADER
   */
  public static EnumSet<PropagationMode> defaultSet() {
    return EnumSet.of(THREAD, RMI, JMS_QUEUE, SOAP, MIME_HEADER);
  }

  public static EnumSet<PropagationMode> defaultSetOneway() {
    return EnumSet.of(THREAD, RMI, JMS_QUEUE, SOAP, MIME_HEADER, ONEWAY);
  }
  

}
