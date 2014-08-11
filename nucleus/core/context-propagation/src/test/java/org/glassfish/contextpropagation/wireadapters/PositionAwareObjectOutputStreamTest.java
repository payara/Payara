/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2014 Oracle and/or its affiliates. All rights reserved.
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

import static org.junit.Assert.assertArrayEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.LinkedList;
import java.util.List;

//import mockit.Deencapsulation;

//import org.glassfish.contextpropagation.adaptors.MockLoggerAdapter;
import org.glassfish.contextpropagation.internal.Utils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class PositionAwareObjectOutputStreamTest {
//
//  @Test
//  public void testSimple() throws IOException {
//    Populateable p = new Populateable() {
//      @Override  public void populate(ObjectOutputStream oos, List<Short> positions) throws IOException {
//        oos.writeLong(1);
//        oos.writeObject("foo");
//        oos.writeLong(2);
//        oos.writeObject("bar");        
//      }      
//    };
//    assertArrayEquals(populate(ObjectOutputStream.class, p, null),
//        populate(PositionAwareObjectOutputStream.class, p, null));
//  }
//  
//  @Test public void testPositions() throws IOException {
//    LinkedList<Short> positions = new LinkedList<Short>();
//    Populateable p = new Populateable() {
//      @Override  public void populate(ObjectOutputStream oos, List<Short> positions) throws IOException {
//        byte[] buf = "Some Bytes".getBytes(); 
//        addPosition(positions, oos); oos.writeObject("foo"); 
//        addPosition(positions, oos); oos.writeShort(1);
//        addPosition(positions, oos); oos.writeLong(2);
//        addPosition(positions, oos); oos.writeBoolean(true);
//        addPosition(positions, oos); oos.writeDouble(2.1E5);
//        addPosition(positions, oos); oos.writeFloat(2.1F);
//        addPosition(positions, oos); oos.write(buf);
//        addPosition(positions, oos); oos.writeByte((byte) 'b');
//        addPosition(positions, oos); oos.writeChar('c');
//        addPosition(positions, oos); oos.writeChars("chars");
//        addPosition(positions, oos); oos.writeInt(1);
//        addPosition(positions, oos); oos.writeObject("bar"); 
//        addPosition(positions, oos); 
//      }
//    };
//    byte[] bytes = populate(PositionAwareObjectOutputStream.class, p, positions);
//    MockLoggerAdapter.debug("bytes size: " + bytes.length + ", positions: " + positions);
//    byte [] expectedBytes = populate(ObjectOutputStream.class, p, null);
//    MockLoggerAdapter.debug(Utils.toString(expectedBytes));
//    MockLoggerAdapter.debug(Utils.toString(bytes));
//    assertArrayEquals(expectedBytes, bytes);
//  }
//  
//
//  private void addPosition(List<Short> positions, ObjectOutputStream oos) throws IOException {
//    if (oos instanceof PositionAwareObjectOutputStream) {
//      positions.add(((PositionAwareObjectOutputStream) oos).position());
//    }   
//  } 
//  
//  private interface Populateable {
//    void populate(ObjectOutputStream oos, List<Short> positions) throws IOException;
//  }
//  
//  private <T extends ObjectOutputStream> byte[] populate(Class<T> clz, Populateable p, List<Short> positions) throws IOException {
//    StreamWrapper<T> sw = new StreamWrapper<T>(clz);
//    ObjectOutputStream oos = sw.getObjectOutputStream();
//    p.populate(oos, positions);
//    return sw.getBytes();
//  }
//  
//  private static class StreamWrapper<T extends ObjectOutputStream> {
//    ByteArrayOutputStream baos;
//    ObjectOutputStream oos;
//    
//    private StreamWrapper(Class<T> clz) throws IOException {
//    baos = new ByteArrayOutputStream();
//    //oos = clz == PositionAwareObjectOutputStream.class ? new PositionAwareObjectOutputStream(baos) : new ObjectOutputStream(baos);
//    oos = Deencapsulation.newInstance(clz, baos); // Above approach produces the same results. Something strange with the instantiation of this stream.
//    }
//    
//    private ObjectOutputStream getObjectOutputStream() {
//      return oos;
//    }
//    
//    private byte[] getBytes() throws IOException {
//      oos.flush();
//      return baos.toByteArray();
//    }
//  }

}
