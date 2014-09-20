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
package org.glassfish.contextpropagation.wireadapters;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.OutputStream;

/**
 * A stream that keeps track of the position
 */
public class PositionAwareObjectOutputStream extends ObjectOutputStream {
  short pos; // initialized by the super constructor
  ObjectOutputStream underlying;

  public PositionAwareObjectOutputStream(OutputStream os) throws IOException {
    super();
    underlying = new ObjectOutputStream(os);
  }

  public short position() throws IOException {
    return pos;
  }

  @Override
  protected void writeObjectOverride(Object obj) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(baos);
    oos.writeObject(obj);
    oos.flush();
    byte[] bytes = baos.toByteArray();
    pos += bytes.length - 1;
    underlying.writeObject(obj);
  }

  @Override
  public void writeUnshared(Object obj) throws IOException {
    throw new UnsupportedOperationException("We do not need this for the WireAdapter");
  }

  @Override
  public void writeFields() throws IOException {
    throw new UnsupportedOperationException("We do not need this for the WireAdapter");
  }

  @Override
  protected void writeStreamHeader() throws IOException {
    pos += 4;
    throw new UnsupportedOperationException("We do not need this for the WireAdapter");
  }

  @Override
  public void useProtocolVersion(int version) throws IOException {
    underlying.useProtocolVersion(version);
  }

  @Override
  public void defaultWriteObject() throws IOException {
    underlying.defaultWriteObject();
  }

  @Override
  public PutField putFields() throws IOException {
    return underlying.putFields();
  }

  @Override
  public void reset() throws IOException {
    underlying.reset();
  }

  @Override
  public void flush() throws IOException {
    underlying.flush();
  }

  @Override
  public void close() throws IOException {
    underlying.close();
  }

  @Override
  protected void writeClassDescriptor(ObjectStreamClass desc)
      throws IOException {
    throw new UnsupportedOperationException("We should never use this directly");
  }

  @Override
  public void write(int val) throws IOException {
    pos += Byte.SIZE / Byte.SIZE;
    underlying.write(val);
  }

  @Override
  public void write(byte[] buf) throws IOException {
    pos += buf.length;
    underlying.write(buf);
  }

  @Override
  public void write(byte[] buf, int off, int len) throws IOException {
    pos += len - off;
    underlying.write(buf, off, len);
  }

  @Override
  public void writeBoolean(boolean val) throws IOException {
    pos += Byte.SIZE / Byte.SIZE;
    underlying.writeBoolean(val);
  }

  @Override
  public void writeByte(int val) throws IOException {
    pos += Byte.SIZE / Byte.SIZE;
    underlying.writeByte(val);
  }

  @Override
  public void writeShort(int val) throws IOException {
    pos += Short.SIZE / Byte.SIZE;
    underlying.writeShort(val);
  }

  @Override
  public void writeChar(int val) throws IOException {
    pos += Character.SIZE / Byte.SIZE;
    underlying.writeChar(val);
  }

  @Override
  public void writeInt(int val) throws IOException {
    pos += Integer.SIZE / Byte.SIZE;
    underlying.writeInt(val);
  }

  @Override
  public void writeLong(long val) throws IOException {
    pos += Long.SIZE / Byte.SIZE;
    underlying.writeLong(val);
  }

  @Override
  public void writeFloat(float val) throws IOException {
    pos += Float.SIZE / Byte.SIZE;
    underlying.writeFloat(val);
  }

  @Override
  public void writeDouble(double val) throws IOException {
    pos += Double.SIZE / Byte.SIZE;
    underlying.writeDouble(val);
  }

  @Override
  public void writeBytes(String str) throws IOException {
    pos += str.length(); // may not be correct
    underlying.writeBytes(str);
  }

  @Override
  public void writeChars(String str) throws IOException {
    pos += str.length() * Character.SIZE / Byte.SIZE;
    underlying.writeChars(str);
  }

  @Override
  public void writeUTF(String str) throws IOException {
    pos += Short.SIZE / Byte.SIZE + getUTFLength(str);
    underlying.writeUTF(str);
  }

  /* From java.io.ObjectOutputStream */
  private static final int CHAR_BUF_SIZE = 2048;
  private char[] cbuf = new char[CHAR_BUF_SIZE];
  synchronized long getUTFLength(String s) {
    int len = s.length();
    long utflen = 0;
    for (int off = 0; off < len; ) {
      int csize = Math.min(len - off, CHAR_BUF_SIZE);
      s.getChars(off, off + csize, cbuf, 0);
      for (int cpos = 0; cpos < csize; cpos++) {
        char c = cbuf[cpos];
        if (c >= 0x0001 && c <= 0x007F) {
          utflen++;
        } else if (c > 0x07FF) {
          utflen += 3;
        } else {
          utflen += 2;
        }
      }
      off += csize;
    }
    return utflen;
  }

}
