/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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
 *
 *
 * This file incorporates work covered by the following copyright and
 * permission notice:
 *
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.jk.common;

import java.io.IOException;
import java.util.logging.*;

import org.apache.jk.core.Msg;
import org.glassfish.grizzly.http.util.ByteChunk;
import org.glassfish.grizzly.http.util.MessageBytes;

/**
 * A single packet for communication between the web server and the
 * container.  Designed to be reused many times with no creation of
 * garbage.  Understands the format of data types for these packets.
 * Can be used (somewhat confusingly) for both incoming and outgoing
 * packets.  
 *
 * See Ajp14/Ajp13Packet.java.
 *
 * @author Henri Gomez [hgomez@apache.org]
 * @author Dan Milstein [danmil@shore.net]
 * @author Keith Wannamaker [Keith@Wannamaker.org]
 * @author Kevin Seguin
 * @author Costin Manolache
 */
public class MsgAjp extends Msg {
    private static Logger log=
        Logger.getLogger( MsgAjp.class.getName() );

    // that's the original buffer size in ajp13 - otherwise we'll get interoperability problems.
    private byte buf[];
    // The current read or write position in the buffer
    private int pos;    
    /**
     * This actually means different things depending on whether the
     * packet is read or write.  For read, it's the length of the
     * payload (excluding the header).  For write, it's the length of
     * the packet as a whole (counting the header).  Oh, well.
     */
    private int len; 

    /**
     * The maximum packet size
     */
    private int bufsize;

    /**
     * Constructor that takes a buffer size
     */
    public MsgAjp(int bsize) {
        if(bsize < 8*1024) {
            bsize = 8*1024;
        }
        bufsize = bsize;
        buf = new byte[bsize];
    
    }

    /**
     * No arg constructor.
     * @deprecated Use the buffer size constructor.
     */
    public MsgAjp() {
        this(8*1024);
    }

    /**
     * Prepare this packet for accumulating a message from the container to
     * the web server.  Set the write position to just after the header
     * (but leave the length unwritten, because it is as yet unknown).
     */
    public void reset() {
        len = 4;
        pos = 4;
    }
	
    /**
     * For a packet to be sent to the web server, finish the process of
     * accumulating data and write the length of the data payload into
     * the header.  
     */
    public void end() {
        len=pos;
        int dLen=len-4;

        buf[0] = (byte)0x41;
        buf[1] = (byte)0x42;
        buf[2]=  (byte)((dLen>>>8 ) & 0xFF );
        buf[3] = (byte)(dLen & 0xFF);
    }

    public byte[] getBuffer() {
        return buf;
    }

    public int getLen() {
        return len;
    }
    
    // ============ Data Writing Methods ===================

    /**
     * Add an int.
     *
     * @param val The integer to write.
     */
    public void appendInt( int val ) {
        buf[pos++]   = (byte) ((val >>>  8) & 0xFF);
        buf[pos++] = (byte) (val & 0xFF);
    }

    public void appendByte( int val ) {
        buf[pos++] = (byte)val;
    }
	
    public void appendLongInt( int val ) {
        buf[pos++]   = (byte) ((val >>>  24) & 0xFF);
        buf[pos++] = (byte) ((val >>>  16) & 0xFF);
        buf[pos++] = (byte) ((val >>>   8) & 0xFF);
        buf[pos++] = (byte) (val & 0xFF);
    }

    /**
     * Write a String out at the current write position.  Strings are
     * encoded with the length in two bytes first, then the string, and
     * then a terminating \0 (which is <B>not</B> included in the
     * encoded length).  The terminator is for the convenience of the C
     * code, where it saves a round of copying.  A null string is
     * encoded as a string with length 0.  
     */
    public void appendBytes(MessageBytes mb) throws IOException {
        if(mb==null || mb.isNull() ) {
            appendInt( 0);
            appendByte(0);
            return;
        }

        // XXX Convert !!
        ByteChunk bc= mb.getByteChunk();
        appendByteChunk(bc);
    }

    public void appendByteChunk(ByteChunk bc) throws IOException {
        if(bc==null) {
            log.severe("appendByteChunk() null");
            appendInt( 0);
            appendByte(0);
            return;
        }

        byte[] bytes = bc.getBytes();
        int start=bc.getStart();
        appendInt( bc.getLength() );
        cpBytes(bytes, start, bc.getLength());
        appendByte(0);
    }

    /** 
     * Copy a chunk of bytes into the packet, starting at the current
     * write position.  The chunk of bytes is encoded with the length
     * in two bytes first, then the data itself, and finally a
     * terminating \0 (which is <B>not</B> included in the encoded
     * length).
     *
     * @param b The array from which to copy bytes.
     * @param off The offset into the array at which to start copying
     * @param numBytes The number of bytes to copy.  
     */
    public void appendBytes( byte b[], int off, int numBytes ) {
        appendInt( numBytes );
        cpBytes( b, off, numBytes );
        appendByte(0);
    }
    
    private void cpBytes( byte b[], int off, int numBytes ) {
        if( pos + numBytes >= buf.length ) {
            log.severe("Buffer overflow: buffer.len=" + buf.length + " pos=" +
                       pos + " data=" + numBytes );
            dump("Overflow/coBytes");
            log.log(Level.SEVERE, "Overflow ", new Throwable());
            return;
        }
        System.arraycopy( b, off, buf, pos, numBytes);
        pos += numBytes;
        // buf[pos + numBytes] = 0; // Terminating \0
    }
    

    
    // ============ Data Reading Methods ===================

    /**
     * Read an integer from packet, and advance the read position past
     * it.  Integers are encoded as two unsigned bytes with the
     * high-order byte first, and, as far as I can tell, in
     * little-endian order within each byte.  
     */
    public int getInt() {
        int b1 = buf[pos++] & 0xFF;  // No swap, Java order
        int b2 = buf[pos++] & 0xFF;

        return  (b1<<8) + b2;
    }

    public int peekInt() {
        int b1 = buf[pos] & 0xFF;  // No swap, Java order
        int b2 = buf[pos+1] & 0xFF;

        return  (b1<<8) + b2;
    }

    public byte getByte() {
        byte res = buf[pos++];
        return res;
    }

    public byte peekByte() {
        byte res = buf[pos];
        return res;
    }

    public void getBytes(MessageBytes mb) {
        int length = getInt();
        if( (length == 0xFFFF) || (length == -1) ) {
            mb.recycle();
            return;
        }
        mb.setBytes( buf, pos, length );
        mb.getCharChunk().recycle();
        pos += length;
        pos++; // Skip the terminating \0
    }
    
    /**
     * Copy a chunk of bytes from the packet into an array and advance
     * the read position past the chunk.  See appendBytes() for details
     * on the encoding.
     *
     * @return The number of bytes copied.
     */
    public int getBytes(byte dest[]) {
        int length = getInt();
        if( length > buf.length ) {
            // XXX Should be if(pos + length > buff.legth)?
            log.severe("getBytes() buffer overflow " + length + " " + buf.length );
        }
	
        if( (length == 0xFFFF) || (length == -1) ) {
            log.info("Null string " + length);
            return 0;
        }

        System.arraycopy( buf, pos,  dest, 0, length );
        pos += length;
        pos++; // Skip terminating \0  XXX I believe this is wrong but harmless
        return length;
    }

    /**
     * Read a 32 bits integer from packet, and advance the read position past
     * it.  Integers are encoded as four unsigned bytes with the
     * high-order byte first, and, as far as I can tell, in
     * little-endian order within each byte.
     */
    public int getLongInt() {
        int b1 = buf[pos++] & 0xFF;  // No swap, Java order
        b1 <<= 8;
        b1 |= (buf[pos++] & 0xFF);
        b1 <<= 8;
        b1 |= (buf[pos++] & 0xFF);
        b1 <<=8;
        b1 |= (buf[pos++] & 0xFF);
        return  b1;
    }

    public int getHeaderLength() {
        return 4;
    }

    public int processHeader() {
        pos = 0;
        int mark = getInt();
        len      = getInt();
	    
        if( mark != 0x1234 && mark != 0x4142 ) {
            // XXX Logging
            log.severe("BAD packet signature " + mark);
            dump( "In: " );
            return -1;
        }

        if( log.isLoggable(Level.FINEST) ) 
            log.finest( "Received " + len + " " + buf[0] );
        return len;
    }
    
    public void dump(String msg) {
        if( log.isLoggable(Level.FINEST) ) 
            log.finest( msg + ": " + " " + pos +"/" + (len + 4));
        int max=pos;
        if( len + 4 > pos )
            max=len+4;
        if( max >1000 ) max=1000;
        if( log.isLoggable(Level.FINEST) ) 
            for( int j=0; j < max; j+=16 ) 
                log.finest( hexLine( buf, j, len ));
	
    }

    /* -------------------- Utilities -------------------- */
    // XXX Move to util package

    public static String hexLine( byte buf[], int start, int len ) {
        StringBuilder sb=new StringBuilder();
        for( int i=start; i< start+16 ; i++ ) {
            if( i < len + 4)
                sb.append( hex( buf[i] ) + " ");
            else 
                sb.append( "   " );
        }
        sb.append(" | ");
        for( int i=start; i < start+16 && i < len + 4; i++ ) {
            if( ! Character.isISOControl( (char)buf[i] ))
                sb.append( new Character((char)buf[i]) );
            else
                sb.append( "." );
        }
        return sb.toString();
    }

    private  static String hex( int x ) {
        //	    if( x < 0) x=256 + x;
        String h=Integer.toHexString( x );
        if( h.length() == 1 ) h = "0" + h;
        return h.substring( h.length() - 2 );
    }

}
