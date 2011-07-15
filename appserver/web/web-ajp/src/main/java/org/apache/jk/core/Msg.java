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

package org.apache.jk.core;

import java.io.IOException;
import org.glassfish.grizzly.http.util.ByteChunk;
import org.glassfish.grizzly.http.util.MessageBytes;



/**
 * A single packet for communication between the web server and the
 * container.
 *
 * In a more generic sense, it's the event that drives the processing chain.
 * XXX Use Event, make Msg a particular case.
 *
 * @author Henri Gomez [hgomez@apache.org]
 * @author Dan Milstein [danmil@shore.net]
 * @author Keith Wannamaker [Keith@Wannamaker.org]
 * @author Kevin Seguin
 * @author Costin Manolache
 */
public abstract class Msg {

    
    
    /**
     * Prepare this packet for accumulating a message from the container to
     * the web server.  Set the write position to just after the header
     * (but leave the length unwritten, because it is as yet unknown).
     */
    public abstract void reset();

    /**
     * For a packet to be sent to the web server, finish the process of
     * accumulating data and write the length of the data payload into
     * the header.  
     */
    public abstract void end();

    public abstract  void appendInt( int val );

    public abstract void appendByte( int val );
	
    public abstract void appendLongInt( int val );

    /**
     */
    public abstract void appendBytes(MessageBytes mb) throws IOException;

    public abstract void appendByteChunk(ByteChunk bc) throws IOException;
    
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
    public abstract void appendBytes( byte b[], int off, int numBytes );

    /**
     * Read an integer from packet, and advance the read position past
     * it.  Integers are encoded as two unsigned bytes with the
     * high-order byte first, and, as far as I can tell, in
     * little-endian order within each byte.  
     */
    public abstract int getInt();

    public abstract int peekInt();

    public abstract byte getByte();

    public abstract byte peekByte();

    public abstract void getBytes(MessageBytes mb);
    
    /**
     * Copy a chunk of bytes from the packet into an array and advance
     * the read position past the chunk.  See appendBytes() for details
     * on the encoding.
     *
     * @return The number of bytes copied.
     */
    public abstract int getBytes(byte dest[]);

    /**
     * Read a 32 bits integer from packet, and advance the read position past
     * it.  Integers are encoded as four unsigned bytes with the
     * high-order byte first, and, as far as I can tell, in
     * little-endian order within each byte.
     */
    public abstract int getLongInt();

    public abstract int getHeaderLength();

    public abstract int processHeader();

    public abstract byte[] getBuffer();

    public abstract int getLen();
    
    public abstract void dump(String msg);

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
