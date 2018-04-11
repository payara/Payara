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
import java.io.NotSerializableException;
import java.io.ObjectInput;

import org.glassfish.contextpropagation.weblogic.workarea.WorkContext;
import org.glassfish.contextpropagation.weblogic.workarea.WorkContextInput;



/**
 * @exclude
 */
public final class WorkContextInputAdapter implements WorkContextInput
{
  private final ObjectInput oi;

  public WorkContextInputAdapter(ObjectInput oi) {
    this.oi = oi;
  }

  @SuppressWarnings("deprecation")
  @Override
  public String readASCII() throws IOException {
    int len = readInt();
    byte[] buf = new byte[len];
    readFully(buf);
    return new String(buf, 0);
  }

  // WorkContextInput
  @Override
  public WorkContext readContext()
    throws IOException, ClassNotFoundException
  {

    Class<?> rcClass = null; 
    // Fix for bug 7391692 - Use the correct context classloader but guard its usage for NPEs
    if (Thread.currentThread().getContextClassLoader() !=null) {
        rcClass = Class.forName(readASCII(),false,Thread.currentThread().getContextClassLoader());
    } else {
        rcClass = Class.forName(readASCII());
    }

    try {
      // FIX ME andyp 19-Aug-08 -- we should consider encapsulating
      // this so that we can skip the data.
      WorkContext runtimeContext = (WorkContext)rcClass.newInstance();
      runtimeContext.readContext(this);
      return runtimeContext;
    }
    catch (InstantiationException ie) {
      throw (IOException)new NotSerializableException
        ("WorkContext must have a public no-arg constructor").initCause(ie);
    }
    catch (IllegalAccessException iae) {
      throw (IOException)new NotSerializableException
        ("WorkContext must have a public no-arg constructor").initCause(iae);
    }
  }

  @Override
  public void readFully(byte[] bytes) throws IOException {
    oi.readFully(bytes);
  }

  @Override
  public void readFully(byte[] bytes, int i, int i1) throws IOException {
    oi.readFully(bytes, i, i1);
  }

  @Override
  public int skipBytes(int i) throws IOException {
    return oi.skipBytes(i);
  }

  @Override
  public boolean readBoolean() throws IOException {
    return oi.readBoolean();
  }

  @Override
  public byte readByte() throws IOException {
    return oi.readByte();
  }

  @Override
  public int readUnsignedByte() throws IOException {
    return oi.readUnsignedByte();
  }

  @Override
  public short readShort() throws IOException {
    return oi.readShort();
  }

  @Override
  public int readUnsignedShort() throws IOException {
    return oi.readUnsignedShort();
  }

  @Override
  public char readChar() throws IOException {
    return oi.readChar();
  }

  @Override
  public int readInt() throws IOException {
    return oi.readInt();
  }

  @Override
  public long readLong() throws IOException {
    return oi.readLong();
  }

  @Override
  public float readFloat() throws IOException {
    return oi.readFloat();
  }

  @Override
  public double readDouble() throws IOException {
    return oi.readDouble();
  }

  @Override
  public String readLine() throws IOException {
    return oi.readLine();
  }

  @Override
  public String readUTF() throws IOException {
    return oi.readUTF();
  }

}
