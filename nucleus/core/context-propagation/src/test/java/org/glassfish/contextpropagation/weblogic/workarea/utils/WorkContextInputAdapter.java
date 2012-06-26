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
  public String readASCII() throws IOException {
    int len = readInt();
    byte[] buf = new byte[len];
    readFully(buf);
    return new String(buf, 0);
  }

  // WorkContextInput
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

  public void readFully(byte[] bytes) throws IOException {
    oi.readFully(bytes);
  }

  public void readFully(byte[] bytes, int i, int i1) throws IOException {
    oi.readFully(bytes, i, i1);
  }

  public int skipBytes(int i) throws IOException {
    return oi.skipBytes(i);
  }

  public boolean readBoolean() throws IOException {
    return oi.readBoolean();
  }

  public byte readByte() throws IOException {
    return oi.readByte();
  }

  public int readUnsignedByte() throws IOException {
    return oi.readUnsignedByte();
  }

  public short readShort() throws IOException {
    return oi.readShort();
  }

  public int readUnsignedShort() throws IOException {
    return oi.readUnsignedShort();
  }

  public char readChar() throws IOException {
    return oi.readChar();
  }

  public int readInt() throws IOException {
    return oi.readInt();
  }

  public long readLong() throws IOException {
    return oi.readLong();
  }

  public float readFloat() throws IOException {
    return oi.readFloat();
  }

  public double readDouble() throws IOException {
    return oi.readDouble();
  }

  public String readLine() throws IOException {
    return oi.readLine();
  }

  public String readUTF() throws IOException {
    return oi.readUTF();
  }

}
