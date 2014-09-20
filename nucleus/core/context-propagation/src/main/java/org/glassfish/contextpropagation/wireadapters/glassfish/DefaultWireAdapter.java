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
package org.glassfish.contextpropagation.wireadapters.glassfish;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.EnumSet;

import org.glassfish.contextpropagation.InsufficientCredentialException;
import org.glassfish.contextpropagation.PropagationMode;
import org.glassfish.contextpropagation.SerializableContextFactory;
import org.glassfish.contextpropagation.SerializableContextFactory.WLSContext;
import org.glassfish.contextpropagation.bootstrap.ContextBootstrap;
import org.glassfish.contextpropagation.bootstrap.LoggerAdapter.MessageID;
import org.glassfish.contextpropagation.internal.Entry;
import org.glassfish.contextpropagation.internal.Entry.ContextType;
import org.glassfish.contextpropagation.internal.Utils.PrivilegedWireAdapterAccessor;
import org.glassfish.contextpropagation.spi.ContextMapHelper;
import org.glassfish.contextpropagation.wireadapters.AbstractWireAdapter;
import org.glassfish.contextpropagation.wireadapters.Catalog;
import org.glassfish.contextpropagation.wireadapters.WireAdapter;

/* 
 *  We considered the following but chose against that:
 *  -  changing the api so that data can be read lazily. In that case we would store all the metadata in the catalog (key, type and position), and only the value, propagationmode in contents. 
 *  -  using a custom or all-together-different stream to provide faster more compact serialization and to avoid the use of a buffered stream
 */

/**
 * This wire adapter implementation produces a compact wire format that
 * starts with a catalog. It minimizes the amount of metadata sent over the wire.
 * The metadata necessary to rehydrate the objects is either hardwired or 
 * available in registries.
 */
public class DefaultWireAdapter extends AbstractWireAdapter {
  private static final byte[] DWA_MARKER = "DWA".getBytes();
  private static final String NULL_KEY = "";
  @Override
  public void writeHeader(ObjectOutputStream oos) throws IOException {
    oos.write(DWA_MARKER);
    oos.writeLong((long) 0x78787878); // 'xxxx' place holder for the catalog position
  }

  @Override
  public void write(ObjectOutputStream oos, String key, Object value, ContextType contextType,
      EnumSet<PropagationMode> propagationModes, String className) throws IOException {
    writeAscii(oos, key);
    oos.writeByte(contextType.ordinal());
    switch (contextType) {
    case BOOLEAN:
      oos.writeBoolean((Boolean) value);
      break;
    case BYTE:
      oos.writeByte((Byte) value);
      break;
    case SHORT:
      oos.writeShort((Short) value);
      break;
    case INT:
      oos.writeInt((Integer) value);
      break;
    case LONG:
      oos.writeLong((Long) value);
      break;
    case STRING:
      oos.writeUTF((String) value);
      break;
    case ASCII_STRING:
      writeAscii(oos, (String) value);
      break;
    case VIEW_CAPABLE:
      // OPTIMIZE If we change the way we store the data from
      // using the ContextMap to using a view-specific map the payload
      // used by keys will be smaller      
      // Just putting the context type should be sufficient for now. 
      break;
    case SERIALIZABLE:
      writeBytes(oos, WLSContext.HELPER.toBytes((Serializable) value));
      break;
    case ATOMICINTEGER: case ATOMICLONG: case BIGDECIMAL: case BIGINTEGER:
      oos.writeObject(value);
      break;
    case CHAR:
      oos.writeChar((Integer) value);
      break;
    case DOUBLE:
      oos.writeDouble((Double) value);
      break;
    case FLOAT:
      oos.writeFloat((Float) value);
      break;
    case OPAQUE:
      byte[] bytes = value instanceof WLSContext ? WLSContext.HELPER.toBytes((WLSContext) value) : (byte[]) value;
      oos.writeBoolean(className != null);
      if (className != null) writeAscii(oos, className);
      writeBytes(oos, bytes); 
      break;
    default:
      // TODO log unexpected Type
      break;
    }
    writePropagationModes(oos, propagationModes);
  }

  private void writeBytes(ObjectOutputStream oos, byte[] bytes) throws IOException {
    oos.writeShort(bytes.length);
    oos.write(bytes);
  }

  private void writePropagationModes(ObjectOutputStream oos, 
      EnumSet<PropagationMode> propagationModes) throws IOException {
    int count = propagationModes.size();
    oos.writeByte(count);
    for (PropagationMode propMode : propagationModes) {
      oos.writeByte(propMode.ordinal());
    }    
  }

  private void writeAscii(ObjectOutputStream oos, String str) throws IOException {
    oos.writeShort((short) str.length());
    oos.writeBytes(str);
  }

  @Override
  public void readHeader(ObjectInputStream objectInputStream, Catalog catalog) throws IOException {
    for (int i = 0; i < DWA_MARKER.length; i++) {
      if (objectInputStream.read() != DWA_MARKER[i]) {
        throw new IOException("Input stream does not appear to contain context propagation data in the default wire format.");
      }
    }
    catalog.setMeta(objectInputStream.readLong());
  }

  @Override
  public String nextKey() throws IOException {
    try {
      key = readAscii();  
      if (key.equals(NULL_KEY)) key = null;
    } catch (EOFException e) {
      key = null;
    } 
    return key;
  }

  @Override
  public Entry nextEntry() throws IOException, ClassNotFoundException {
    String className = null;
    Entry.ContextType contextType = Entry.ContextType.fromOrdinal(ois.readByte());
    ContextBootstrap.debug(MessageID.READ_CONTEXT_TYPE, contextType);
    Object value = null; 
    switch (contextType) {
    case BOOLEAN:
      value = ois.readBoolean();
      break;
    case BYTE:
      value = ois.readByte(); 
      break;
    case SHORT:
      value = ois.readShort();
      break;
    case INT:
      value = ois.readInt();
      break;
    case LONG:
      value = ois.readLong();
      break;
    case STRING:
      value = ois.readUTF();
      break;
    case ASCII_STRING:
      value = readAscii();
      break;
    case VIEW_CAPABLE:
      try {
        PrivilegedWireAdapterAccessor priviledgedCM = (PrivilegedWireAdapterAccessor) ContextMapHelper.getScopeAwareContextMap();
        priviledgedCM.createViewCapable(key, false);
        Entry entry = priviledgedCM.getAccessControlledMap(false).getEntry(key);
        ContextBootstrap.debug(MessageID.READ_VALUE, "<a ViewCapable>");
        EnumSet<PropagationMode> propModes = readPropModes();
        ContextBootstrap.debug(MessageID.READ_PROP_MODES, propModes);
        return entry;
      } catch (InsufficientCredentialException e) {
        throw new AssertionError("Wire adapter should have sufficient privileges to create a ViewCapable.");
      }
    case SERIALIZABLE:
      value = WLSContext.HELPER.readFromBytes(readBytes(ois));
      break;
    case BIGDECIMAL: case BIGINTEGER: case ATOMICINTEGER: case ATOMICLONG:
      value = ois.readObject();
      break;
    case CHAR:
      value = ois.readChar();
      break;
    case DOUBLE:
      value = ois.readDouble();
      break;
    case FLOAT:
      value = ois.readFloat();
      break;
    case OPAQUE:
      boolean hasClassName = ois.readBoolean();
      className = hasClassName ? readAscii() : null;
      byte[] bytes = readBytes(ois);
      SerializableContextFactory factory = WireAdapter.HELPER.findContextFactory(key, className);
      value = factory == null ? 
          bytes : WLSContext.HELPER.readFromBytes(factory.createInstance(), bytes);
      break;
    default:
      // TODO log unexpected case
      break;
    }
    ContextBootstrap.debug(MessageID.READ_VALUE, value);
    EnumSet<PropagationMode> propModes = readPropModes();
    ContextBootstrap.debug(MessageID.READ_PROP_MODES, propModes);
    return className == null ? new Entry(value, propModes, contextType) :
      Entry.createOpaqueEntryInstance(value, propModes, className);
  }

  private byte[] readBytes(ObjectInputStream ois) throws IOException {
    byte[] bytes = new byte[ois.readShort()];
    ois.readFully(bytes);
    return bytes;
  }

  private EnumSet<PropagationMode> readPropModes() throws IOException {
    int count = ois.readByte();
    EnumSet<PropagationMode> enumSet = EnumSet.noneOf(PropagationMode.class);
    for(int i = 0; i < count; i++) {
      byte ordinal = ois.readByte();
      enumSet.add(PropagationMode.fromOrdinal(ordinal));
    }
    return enumSet;
  }

  private String readAscii() throws IOException {
    byte[] bytes = readBytes(ois);
    return new String(bytes);
  }

  @Override
  protected void writeFooter(ObjectOutputStream objectOutputStream)
      throws IOException {
    // NO-OP
  }

  @Override
  protected void write(ObjectOutputStream objectOutputStream, Catalog catalog) throws IOException {
    writeAscii(objectOutputStream, NULL_KEY); // so that we know there are no more keys and are ready to read the catalog
    catalog.write(objectOutputStream);   
  }

  protected void read(boolean mandatory, ObjectInputStream ois, Catalog catalog) throws IOException {
    if (mandatory) {
      ois.reset();
      int skipAmount = catalog.getStart();
      for (int skipped = 0; 
          skipped < skipAmount;
          skipped += ois.skip(skipAmount - skipped) );
      readAscii(); // Read the NULL_KEY
    }
    catalog.read(ois);
  }

}
