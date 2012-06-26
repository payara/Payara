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
