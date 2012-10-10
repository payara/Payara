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
package org.glassfish.contextpropagation.wireadapters.wls;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.Serializable;
import java.util.EnumSet;

import org.glassfish.contextpropagation.InsufficientCredentialException;
import org.glassfish.contextpropagation.PropagationMode;
import org.glassfish.contextpropagation.SerializableContextFactory;
import org.glassfish.contextpropagation.SerializableContextFactory.WLSContext;
import org.glassfish.contextpropagation.bootstrap.ContextBootstrap;
import org.glassfish.contextpropagation.bootstrap.LoggerAdapter;
import org.glassfish.contextpropagation.bootstrap.LoggerAdapter.Level;
import org.glassfish.contextpropagation.bootstrap.LoggerAdapter.MessageID;
import org.glassfish.contextpropagation.internal.Entry;
import org.glassfish.contextpropagation.internal.Entry.ContextType;
import org.glassfish.contextpropagation.internal.Utils;
import org.glassfish.contextpropagation.internal.Utils.PrivilegedWireAdapterAccessor;
import org.glassfish.contextpropagation.spi.ContextMapHelper;
import org.glassfish.contextpropagation.wireadapters.AbstractWireAdapter;
import org.glassfish.contextpropagation.wireadapters.Catalog;

/**
 * This WireAdapter implementation is compatible with the wire format
 * from Web Logic Server 12.2 and earlier. It provides limited support
 * for primitive types other than Long, String, and ASCII String. These
 * other primitive types are wrapped inside 
 * weblogic.workarea.SerializableWorkContext$Carrier. 
 * A catalog is also wrapped inside a
 * SerializableContext. This catalog may not be of any use to a WLS process, 
 * however it may be of use to another Glassfish process which may receive it
 * directly or through a WLS process. 
 * The catalog is useful to interpret contexts that may not have been written by WLS
 * or if the catalog is read by glassfish (we can use the WLS wire format between 
 * two glassfish instances as well as between glassfish and WLS instances). 
 */
public class WLSWireAdapter extends AbstractWireAdapter {
  private static /*final breaks the test */ String WLS_CARRIER_CLASS_NAME = "weblogic.workarea.SerializableWorkContext$Carrier";
  private Catalog wlsCatalog;

  @Override
  public void write(ObjectOutputStream oos, String key, Object value, ContextType contextType,
      EnumSet<PropagationMode> propagationModes, String className) throws IOException {
    oos.writeUTF(key);    
    ContextBootstrap.debug(MessageID.WRITING_KEY, key);
    oos.writeInt(toWlsPropagationMode(propagationModes));
    ContextBootstrap.debug(MessageID.WRITE_PROPAGATION_MODES, propagationModes);
    switch (contextType) {
    case LONG:
      writeBytes(oos, ClassNames.LONG);
      oos.writeLong((Long) value);
      break;
    case STRING:
      writeBytes(oos, ClassNames.STRING);
      oos.writeUTF((String) value);
      break;
    case ASCII_STRING:
      writeBytes(oos, ClassNames.ASCII);
      writeAscii(oos, (String) value);
      break;     
    case VIEW_CAPABLE:
      ContextBootstrap.debug(MessageID.WLS_UNSUPPORTED_TYPE, key, contextType.name(), value);
      writeInWrapper(oos, new Carrier((Serializable) new ViewMeta()).toBytes());
      break;
    case ATOMICINTEGER: case ATOMICLONG: case BIGDECIMAL: case BIGINTEGER:
    case BOOLEAN: case BYTE: case CHAR: case DOUBLE: case FLOAT: // Fall through by design
    case INT: case SHORT: // Fall through by designs
      ContextBootstrap.debug(MessageID.WLS_UNSUPPORTED_TYPE, key, contextType.name(), value);
    case SERIALIZABLE:      
      writeInWrapper(oos, new Carrier((Serializable) value).toBytes());
      break;
    case OPAQUE:
      if (value instanceof WLSContext) {
        ((WLSContext) value).writeContext(oos);
      } else {
        writeInWrapper(oos, (byte[]) value);
      }
    default:
      // TODO log unexpected Type
      break;
    }
  }

  private void writeInWrapper(ObjectOutputStream oos, byte[] bytes) throws IOException {
    writeBytes(oos, ClassNames.SERIALIZABLE);
    writeBytes(oos, bytes);
  }

  public static int toWlsPropagationMode(EnumSet<PropagationMode> propagationModes) {
    int result = 0;
    for (PropagationMode pm : propagationModes) {
      result += 1 << pm.ordinal();
    }
    return result;
  }

  public static EnumSet<PropagationMode> toPropagationMode(int mode) {
    EnumSet<PropagationMode> modes = EnumSet.noneOf(PropagationMode.class);
    for (PropagationMode pm : PropagationMode.values()) {
      int pmAsInt = 1 << pm.ordinal();
      if (pmAsInt == (pmAsInt & mode)) {
        modes.add(pm);
      }
    }
    return modes;
  }

  private void writeBytes(ObjectOutputStream oos, byte[] bytes) throws IOException {
    oos.writeInt(bytes.length);
    oos.write(bytes);
  }

  protected void writeFooter(ObjectOutputStream objectOutputStream) throws IOException {
    objectOutputStream.writeUTF("");
    ContextBootstrap.debug(MessageID.WRITE_FOOTER);
  }

  @Override
  public String nextKey() throws IOException {
    key = ois.readUTF();
    return key == null || key.isEmpty() ? null : key;
  }

  @Override
  public Entry nextEntry() throws IOException {
    EnumSet<PropagationMode> propModes = toPropagationMode(ois.readInt());
    ContextBootstrap.debug(MessageID.READ_PROP_MODES, propModes);
    String className = readAscii();
    Entry.ContextType contextType = toContextType(className);
    ContextBootstrap.debug(MessageID.READ_CONTEXT_TYPE, contextType);
    Object value;
    switch (contextType) {
    case LONG:
      value = ois.readLong();
      if (key.equals(Catalog.CATALOG_META_KEY)) {
        if (wlsCatalog == null) throw new IllegalStateException("wlsCatalog should have been set by readHeader.");
        wlsCatalog.setMeta((Long) value);
      }
      break;
    case STRING:
      value = ois.readUTF();
      break;
    case ASCII_STRING:
      value = readAscii();
      break;
    case SERIALIZABLE:
      byte[] bytes = new byte[ois.readInt()];
      ois.readFully(bytes);
      try {
        Carrier carrier = Carrier.fromBytes(bytes);
        value = carrier.serializable;
        if (value instanceof ViewMeta) {
          try {
            value = ((PrivilegedWireAdapterAccessor) ContextMapHelper.getScopeAwareContextMap()).createViewCapable(key, false);
          } catch (InsufficientCredentialException e) {
            throw new AssertionError("Wire adapter should have sufficient privileges to create a ViewCapable.");
          }
        } else {
          if (key.equals(Catalog.CATALOG_KEY)) {
            wlsCatalog.setPosisionsFrom((Catalog) value);
          }
        }
      } catch (ClassNotFoundException e) {
        /*
         * OPTIMIZE If the object can be extracted we should extract it but it may be better to defer that until it is accessed
         * it the object cannot be created, we need to log a message and store the raw data as of type opaque byte[]
         */
        ContextBootstrap.debug(MessageID.READING_OPAQUE_TYPE, key, bytes.length);
        value = bytes;
        contextType = ContextType.OPAQUE;
      }
      break;
    case OPAQUE:
      /*
       * In general for OPAQUE that originated on WLS, we must have that class
       * on the glassfish server or things will break down. That is also
       * a requirement on WLS so we are not worse off.
       * We also need an implementation of ContextOutput
       */
      SerializableContextFactory factory = HELPER.findContextFactory(key, className); 
      if (factory == null) {
        /* In this case if will not be possible to continue reading from the 
         * stream. LATER We can try to look for the next entry, but that is problematic.
         * A brute force approach would be to read a byte, mark the stream, 
         * and attempt reading the next entry and repeat if we fail to read the entry
         * However this can be tricky because we do no know where the end of data is
         * so we can easily read past the end of context data and thus affect the
         * overall reading of the message if the protocol does not record the size
         * of the context data. We can get around this by modifying legacy
         * wls to either include a catalog or write a long context that contains
         * the length of context data.
         */
        error(MessageID.ERROR_NO_WORK_CONTEXT_FACTORY, key, className);
        return null;
      } else {
        WLSContext ctx = factory.createInstance();
        if (ctx != null) {
          ctx.readContext(ois);
        }
        value = ctx;
      }
      break;
    default:
      throw new AssertionError("Unsupported context type, " + contextType);
    }
    ContextBootstrap.debug(MessageID.READ_VALUE, value);    
    return contextType == ContextType.OPAQUE ? 
        Entry.createOpaqueEntryInstance(value, propModes, className) : new Entry(value, propModes, contextType);  
  }

  private ContextType toContextType(String className) {
    if (className.endsWith("weblogic.workarea.AsciiWorkContext")) {
      return ContextType.ASCII_STRING;
    } else if (className.endsWith("weblogic.workarea.StringWorkContext")) {
      return ContextType.STRING;
    } else if (className.endsWith("weblogic.workarea.LongWorkContext")) {
      return ContextType.LONG;
    } else if (className.endsWith("weblogic.workarea.SerializableWorkContext")) {
      return ContextType.SERIALIZABLE;
    } else {
      return ContextType.OPAQUE;
    }
  }

  public String readAscii() throws IOException {
    byte[] buf = new byte[ois.readInt()];
    ois.readFully(buf);
    return new String(buf);
  }

  private void writeAscii(ObjectOutputStream oos, String s) throws IOException {
    oos.writeInt(s.length());
    oos.writeBytes(s);
  }

  interface ClassNames {
    static final byte[] ASCII = "weblogic.workarea.AsciiWorkContext".getBytes(); 
    static final byte[] STRING = "weblogic.workarea.StringWorkContext".getBytes(); 
    static final byte[] LONG = "weblogic.workarea.LongWorkContext".getBytes(); 
    static /* WARNING Deencapsulation has a problem with final*/ byte[] SERIALIZABLE = "weblogic.workarea.SerializableWorkContext".getBytes(); 
  }

  private static class Carrier implements Serializable {
    //This class carries the Serializable object along with its associated 
    //attributes
    private static final int VERSION = 1; //for interop
    private static final long serialVersionUID = -197593099539117489L;
    private Serializable serializable;
    private boolean mutable = false;

    @SuppressWarnings("unused")
    public Carrier() {} // Fulfills Serializable Contract 

    byte[] toBytes() throws IOException {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ObjectOutputStream oos = new ObjectOutputStream(baos) {
        private boolean isIntercepting = false;
        @Override public void writeUTF(String className) throws IOException {
          String wireClassName = className;
          if (isIntercepting && className.equals(Carrier.class.getName())) {
            wireClassName = WLS_CARRIER_CLASS_NAME;
            ContextBootstrap.debug(MessageID.INTERCEPTING_CLASS, "writing", className, wireClassName);
          }
          super.writeUTF(wireClassName);
        }
        @Override
        public void writeClassDescriptor(ObjectStreamClass osc) throws IOException {
          isIntercepting = true;
          super.writeClassDescriptor(osc);
          isIntercepting = false;
        }
      };
      oos.writeObject(this);
      oos.flush();
      byte[] bytes = baos.toByteArray();
      ContextBootstrap.debug(MessageID.WRITING_SERIALIZED, bytes.length, Utils.toString(bytes));
      return bytes;
    }

    static Carrier fromBytes(byte[] bytes) throws IOException, ClassNotFoundException {
      ContextBootstrap.debug(MessageID.READING_SERIALIZED, bytes.length, Utils.toString(bytes));
      ByteArrayInputStream bin = new ByteArrayInputStream(bytes);
      ObjectInputStream in = new ObjectInputStream(bin) {
        private boolean isIntercepting = false;
        @Override
        protected ObjectStreamClass readClassDescriptor() throws IOException,
        ClassNotFoundException {
          isIntercepting = true;
          ObjectStreamClass osc = super.readClassDescriptor();
          isIntercepting = false;
          return osc;
        }
        @Override
        public String readUTF() throws IOException {
          String result = super.readUTF();
          if (isIntercepting && result.endsWith(WLS_CARRIER_CLASS_NAME)) {
            String wireClassName = result;
            result  = Carrier.class.getName();
            ContextBootstrap.debug(MessageID.INTERCEPTING_CLASS, "reading", wireClassName, result);
          }
          return result;
        }
      };
      Carrier carrier = (Carrier) in.readObject();
      return carrier;
    }

    /*package*/ Carrier(Serializable object) {
      this.serializable = object;
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
      out.writeInt(VERSION);
      out.writeObject(serializable);
      out.writeBoolean(mutable);
    }

    private void readObject(ObjectInputStream in)
        throws IOException, ClassNotFoundException {
      in.readInt(); // WARNING VERSION refers to the version of the Carrier. It is not used and will only be needed if WLS changes the serialization profile of the Carrier.    
      serializable = (Serializable) in.readObject();
      mutable = in.readBoolean();
    }
  }

  private static void error(MessageID messageID, Object... args) {
    LoggerAdapter logger = ContextBootstrap.getLoggerAdapter(); 
    if (logger.isLoggable(Level.ERROR)) {
      logger.log(Level.ERROR, messageID, args);
    }
  }

  @Override
  protected void writeHeader(ObjectOutputStream oos) throws IOException {
    write(oos, Catalog.CATALOG_META_KEY, (long) 0x78787878, ContextType.LONG, PropagationMode.defaultSet(), null);    
  }

  @Override
  protected void readHeader(ObjectInputStream ois, Catalog catalog) throws IOException {
    wlsCatalog = catalog;
  }

  @Override
  protected void write(ObjectOutputStream oos, Catalog catalog)
      throws IOException {
    write(oos, Catalog.CATALOG_KEY, catalog, ContextType.SERIALIZABLE, PropagationMode.defaultSet(), null);    
  }

  @Override
  protected void read(boolean mandatory, ObjectInputStream ois, Catalog catalog)
      throws IOException {
    if (mandatory) {
      ois.reset();
      int amountToSkip = catalog.getStart();
      for (int skipped = 0;
          skipped < amountToSkip;
          skipped += ois.skip(amountToSkip - skipped));
      nextKey();
      Entry catalogEntry = nextEntry();
      catalog.setPosisionsFrom((Catalog) catalogEntry.getValue());
      catalog.upItemNumber(-1);
    }
  }

}
