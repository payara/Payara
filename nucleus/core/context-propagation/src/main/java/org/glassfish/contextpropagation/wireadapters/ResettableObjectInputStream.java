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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidObjectException;
import java.io.NotActiveException;
import java.io.ObjectInputStream;
import java.io.ObjectInputValidation;

/**
 * An ObjectInputStream implementation with some support for reset method.
 */
public class ResettableObjectInputStream extends ObjectInputStream {
  private static final int READ_LIMIT = 10000;
  BufferedInputStream bis;
  ObjectInputStream underlying;

  public ResettableObjectInputStream(InputStream in) throws IOException {
    bis = new BufferedInputStream(in);
    bis.mark(READ_LIMIT);
    reset();
  }

  @Override
  protected Object readObjectOverride() throws IOException,
      ClassNotFoundException {
    return underlying.readObject();
  }
  
  @Override
  public Object readUnshared() throws IOException, ClassNotFoundException {
    return underlying.readUnshared();
  }

  @Override
  public void defaultReadObject() throws IOException, ClassNotFoundException {
    underlying.defaultReadObject();
  }

  @Override
  public GetField readFields() throws IOException, ClassNotFoundException {
    return underlying.readFields();
  }

  @Override
  public void registerValidation(ObjectInputValidation obj, int prio)
      throws NotActiveException, InvalidObjectException {
    underlying.registerValidation(obj, prio);
  }

  /*@Override
  protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException,
      ClassNotFoundException {

    return underlying.resolveClass(desc);
  }

  @Override
  protected Class<?> resolveProxyClass(String[] interfaces) throws IOException,
      ClassNotFoundException {

    return underlying.resolveProxyClass(interfaces);
  }

  @Override
  protected Object resolveObject(Object obj) throws IOException {

    return underlying.resolveObject(obj);
  }

  @Override
  protected boolean enableResolveObject(boolean enable)
      throws SecurityException {

    return underlying.enableResolveObject(enable);
  }

  @Override
  protected void readStreamHeader() throws IOException,
      StreamCorruptedException {

    underlying.readStreamHeader();
  }

  @Override
  protected ObjectStreamClass readClassDescriptor() throws IOException,
      ClassNotFoundException {

    return underlying.readClassDescriptor();
  }*/

  @Override
  public int read() throws IOException {
    return underlying.read();
  }

  @Override
  public int read(byte[] buf, int off, int len) throws IOException {
    return underlying.read(buf, off, len);
  }

  @Override
  public int available() throws IOException {
    return underlying.available();
  }

  @Override
  public void close() throws IOException {
    underlying.close();
  }

  @Override
  public boolean readBoolean() throws IOException {
    return underlying.readBoolean();
  }

  @Override
  public byte readByte() throws IOException {
    return underlying.readByte();
  }

  @Override
  public int readUnsignedByte() throws IOException {
    return underlying.readUnsignedByte();
  }

  @Override
  public char readChar() throws IOException {
    return underlying.readChar();
  }

  @Override
  public short readShort() throws IOException {
    return underlying.readShort();
  }

  @Override
  public int readUnsignedShort() throws IOException {
    return underlying.readUnsignedShort();
  }

  @Override
  public int readInt() throws IOException {
    return underlying.readInt();
  }

  @Override
  public long readLong() throws IOException {
    return underlying.readLong();
  }

  @Override
  public float readFloat() throws IOException {
    return underlying.readFloat();
  }

  @Override
  public double readDouble() throws IOException {
    return underlying.readDouble();
  }

  @Override
  public void readFully(byte[] buf) throws IOException {
    underlying.readFully(buf);
  }

  @Override
  public void readFully(byte[] buf, int off, int len) throws IOException {
    underlying.readFully(buf, off, len);
  }

  @Override
  public int skipBytes(int len) throws IOException {
    return underlying.skipBytes(len);
  }

  @SuppressWarnings("deprecation")
  @Override
  public String readLine() throws IOException {
    return underlying.readLine();
  }

  @Override
  public String readUTF() throws IOException {
    return underlying.readUTF();
  }

  @Override
  public int read(byte[] b) throws IOException {
    return underlying.read(b);
  }

  @Override
  public long skip(long n) throws IOException {
    return underlying.skip(n);
  }

  @Override
  public synchronized void mark(int readlimit) {
    throw new UnsupportedOperationException("NO-Op this stream allways reset to the 0 position");
  }

  @Override
  public synchronized void reset() throws IOException {
    bis.reset();
    bis.mark(READ_LIMIT);
    underlying = new ObjectInputStream(bis);
  }

  @Override
  public boolean markSupported() {
    return false; // Does not offer full support for mark, but allow reset to original position
  }

  @Override
  public boolean equals(Object obj) {
    return underlying.equals(obj);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

}
