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

import java.io.DataOutput;
import java.io.IOException;


/**
 * <code>WorkConectOutput</code> is a primitive stream used for
 * marshaling {@link WorkContext} implementations. It is
 * necessary to limit the types that can be marshaled as part of a
 * <code>WorkArea</code> so that efficient representations can be
 * implemented in a variety of protocols. This representation can also
 * be transparent, enabling runtime filtering in SOAP and other
 * protocols.
 *
 * @see org.glassfish.contextpropagation.weblogic.workarea.WorkContextInput
 * @author Copyright (c) 2003 by BEA Systems Inc. All Rights Reserved.
 */
public interface WorkContextOutput extends DataOutput {
  /**
   * Writes an 8-bit, variable-length, string to the underlying data
   * stream. This is analgous to {@link DataOutput#writeBytes} but the
   * length of the string is also encoded.
   */
  public void writeASCII(String s) throws IOException;

  /**
   * Writes the implementation of {@link WorkContext} to the
   * underlying data stream. The actual class is encoded in the stream
   * so that remote java implementations can decode it.
   */
  public void writeContext(WorkContext ctx) throws IOException;
}
