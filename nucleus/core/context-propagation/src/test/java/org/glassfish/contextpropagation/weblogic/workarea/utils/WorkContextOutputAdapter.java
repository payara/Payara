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

package org.glassfish.contextpropagation.weblogic.workarea.utils;

import java.io.IOException;
import java.io.ObjectOutput;

import org.glassfish.contextpropagation.weblogic.workarea.WorkContext;
import org.glassfish.contextpropagation.weblogic.workarea.WorkContextOutput;



/**
 * @exclude
 */
public class WorkContextOutputAdapter implements WorkContextOutput
{
  private final ObjectOutput oo;

  public WorkContextOutputAdapter(ObjectOutput oo) {
    this.oo = oo;
  }

  public void writeASCII(String s) throws IOException {
    writeInt(s.length());
    writeBytes(s);
  }

  public void writeContext(WorkContext ctx) throws IOException {
    writeASCII(ctx.getClass().getName());
    ctx.writeContext(this);
  }

  public void write(int i) throws IOException {
    oo.write(i);
  }

  public void write(byte[] bytes) throws IOException {
    oo.write(bytes);
  }

  public void write(byte[] bytes, int i, int i1) throws IOException {
    oo.write(bytes, i, i1);
  }

  public void writeBoolean(boolean b) throws IOException {
    oo.writeBoolean(b);
  }

  public void writeByte(int i) throws IOException {
    oo.writeByte(i);
  }

  public void writeShort(int i) throws IOException {
    oo.writeShort(i);
  }

  public void writeChar(int i) throws IOException {
    oo.writeChar(i);
  }

  public void writeInt(int i) throws IOException {
    oo.writeInt(i);
  }

  public void writeLong(long l) throws IOException {
    oo.writeLong(l);
  }

  public void writeFloat(float v) throws IOException {
    oo.writeFloat(v);
  }

  public void writeDouble(double v) throws IOException {
    oo.writeDouble(v);
  }

  public void writeBytes(String s) throws IOException {
    oo.writeBytes(s);
  }

  public void writeChars(String s) throws IOException {
    oo.writeChars(s);
  }

  public void writeUTF(String s) throws IOException {
    oo.writeUTF(s);
  }
}
