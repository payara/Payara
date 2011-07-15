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

import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;

import org.apache.catalina.connector.Constants;
import org.apache.jk.core.Msg;
import org.apache.jk.core.MsgContext;
import org.glassfish.grizzly.http.server.io.InputBuffer;
import org.glassfish.grizzly.http.server.io.OutputBuffer;
import org.glassfish.grizzly.http.util.ByteChunk;
import org.glassfish.grizzly.http.util.C2BConverter;
import org.glassfish.grizzly.http.util.MessageBytes;


/** Generic input stream impl on top of ajp
 */
public class JkInputStream {
//        implements InputBuffer, OutputBuffer {
    
    private static Logger log=
        Logger.getLogger( JkInputStream.class.getName() );

    private Msg bodyMsg ;
    private Msg outputMsg ;
    private MsgContext mc;

    
    // Holds incoming chunks of request body data
    private MessageBytes bodyBuff = MessageBytes.newInstance();
    private MessageBytes tempMB = MessageBytes.newInstance();
    private boolean end_of_stream=false; 
    private boolean isEmpty = true;
    private boolean isFirst = true;
    private boolean isReplay = false;
    private boolean isReadRequired = false;
    private int packetSize = AjpConstants.MAX_PACKET_SIZE;
    
    static {
        // Make certain HttpMessages is loaded for SecurityManager
        try {
            Class.forName("org.glassfish.grizzly.util.http.HttpMessages");
        } catch(Exception ex) {
            // ignore
        }
    }
   
    public JkInputStream(MsgContext context, int bsize) {
        mc = context;
        if (bsize < AjpConstants.MAX_PACKET_SIZE)
            this.packetSize = AjpConstants.MAX_PACKET_SIZE;
        else
            this.packetSize = bsize;
        bodyMsg = new MsgAjp(this.packetSize);
        outputMsg = new MsgAjp(this.packetSize);
    }

    /**
     * @deprecated
     */
    public JkInputStream(MsgContext context) {
        this(context, AjpConstants.MAX_PACKET_SIZE);
    }
   
    // -------------------- Jk specific methods --------------------


    /**
     * Set the flag saying that the server is sending a body
     */
    public void setIsReadRequired(boolean irr) {
        isReadRequired = irr;
    }

    /**
     * Return the flag saying that the server is sending a body
     */
    public boolean isReadRequired() {
        return isReadRequired;
    }

    
    /** Must be called before or after each request
     */
    public void recycle() {
        if(isReadRequired && isFirst) {
            // The Servlet never read the request body, so we need to junk it
            try {
              receive();
            } catch(IOException iex) {
              log.log(Level.FINEST, "Error consuming request body",iex);
            }
        }

        end_of_stream = false;
        isEmpty = true;
        isFirst = true;
        isReplay = false;
        isReadRequired = false;
        bodyBuff.recycle();
        tempMB.recycle();
    }


    public void endMessage() throws IOException {
        outputMsg.reset();
        outputMsg.appendByte(AjpConstants.JK_AJP13_END_RESPONSE);
        outputMsg.appendByte(1);
        mc.getSource().send(outputMsg, mc);
        mc.getSource().flush(outputMsg, mc);
    }


    // -------------------- OutputBuffer implementation --------------------

        
    public int doWrite(ByteChunk chunk, Response res) 
        throws IOException    {
//        if (!res.isCommitted()) {
//            // Send the connector a request for commit. The connector should
//            // then validate the headers, send them (using sendHeader) and
//            // set the filters accordingly.
//            res.sendHeaders();
//        }
//
//        int len=chunk.getLength();
//        byte buf[]=outputMsg.getBuffer();
//        // 4 - hardcoded, byte[] marshalling overhead
//        int chunkSize=buf.length - outputMsg.getHeaderLength() - 4;
//        int off=0;
//        while( len > 0 ) {
//            int thisTime=len;
//            if( thisTime > chunkSize ) {
//                thisTime=chunkSize;
//            }
//            len-=thisTime;
//
//            outputMsg.reset();
//            outputMsg.appendByte( AjpConstants.JK_AJP13_SEND_BODY_CHUNK);
//            if( log.isLoggable(Level.FINEST) )
//                log.log(Level.FINEST, "doWrite {0} {1} {2}", new Object[]{off, thisTime, len});
//            outputMsg.appendBytes( chunk.getBytes(), chunk.getOffset() + off, thisTime );
//            off+=thisTime;
//            mc.getSource().send( outputMsg, mc );
//        }
        return 0;
    }

    public int doRead(ByteChunk responseChunk, Request req) 
        throws IOException {

        if( log.isLoggable(Level.FINEST))
            log.log( Level.FINEST, "doRead {0} {1} {2}",
                    new Object[]{end_of_stream, responseChunk.getOffset(), responseChunk.getLength()});
        if( end_of_stream ) {
            return -1;
        }

        if( isFirst && isReadRequired ) {
            // Handle special first-body-chunk, but only if httpd expects it.
            if( !receive() ) {
                return 0;
            }
        } else if(isEmpty) {
            if ( !refillReadBuffer() ){
                return -1;
            }
        }
        ByteChunk bc = bodyBuff.getByteChunk();
        responseChunk.setBytes( bc.getBuffer(), bc.getStart(), bc.getLength() );
        isEmpty = true;
        return responseChunk.getLength();
    }
    
    /** Receive a chunk of data. Called to implement the
     *  'special' packet in ajp13 and to receive the data
     *  after we send a GET_BODY packet
     */
    public boolean receive() throws IOException {
        isFirst = false;
        bodyMsg.reset();
        int err = mc.getSource().receive(bodyMsg, mc);
        if( log.isLoggable(Level.FINEST) )
            log.log( Level.INFO, "Receiving: getting request body chunk {0} {1}",
                    new Object[]{err, bodyMsg.getLen()});
        
        if(err < 0) {
            throw new IOException();
        }

        // No data received.
        if( bodyMsg.getLen() == 0 ) { // just the header
            // Don't mark 'end of stream' for the first chunk.
            // end_of_stream = true;
            return false;
        }
        int blen = bodyMsg.peekInt();

        if( blen == 0 ) {
            return false;
        }

        if( log.isLoggable(Level.FINEST) ) {
            bodyMsg.dump("Body buffer");
        }
        
        bodyMsg.getBytes(bodyBuff);
        if( log.isLoggable(Level.FINEST) )
            log.finest( "Data:\n" + bodyBuff);
        isEmpty = false;
        return true;
    }
    
    /**
     * Get more request body data from the web server and store it in the 
     * internal buffer.
     *
     * @return true if there is more data, false if not.    
     */
    private boolean refillReadBuffer() throws IOException 
    {
        // If the server returns an empty packet, assume that that end of
        // the stream has been reached (yuck -- fix protocol??).
        if(isReplay) {
            end_of_stream = true; // we've read everything there is
        }
        if (end_of_stream) {
            if( log.isLoggable(Level.FINEST) ) 
                log.finest("refillReadBuffer: end of stream " );
            return false;
        }

        // Why not use outBuf??
        bodyMsg.reset();
        bodyMsg.appendByte(AjpConstants.JK_AJP13_GET_BODY_CHUNK);
        // bodyMsg.appendInt(AjpConstants.MAX_READ_SIZE);
        bodyMsg.appendInt(packetSize - AjpConstants.H_SIZE - 2);
        
        if( log.isLoggable(Level.FINEST) )
            log.finest("refillReadBuffer " + Thread.currentThread());

        mc.getSource().send(bodyMsg, mc);
        mc.getSource().flush(bodyMsg, mc); // Server needs to get it

        // In JNI mode, response will be in bodyMsg. In TCP mode, response need to be
        // read

        boolean moreData=receive();
        if( !moreData ) {
            end_of_stream=true;
        }
        return moreData;
    }

    public void appendHead(Response res) throws IOException {
//        if( log.isLoggable(Level.FINEST) )
//            log.log(Level.FINEST, "COMMIT sending headers {0} {1}", new Object[]{res, res.getMimeHeaders()});
//
//        C2BConverter c2b=mc.getConverter();
//
//        outputMsg.reset();
//        outputMsg.appendByte(AjpConstants.JK_AJP13_SEND_HEADERS);
//        outputMsg.appendInt( res.getStatus() );
//
//        String message = null;
//        if (res.isAllowCustomReasonPhrase()) {
//            message = res.getMessage();
//        }
//        if( message==null ){
//            message= HttpMessages.getMessage(res.getStatus());
//        } else {
//            message = message.replace('\n', ' ').replace('\r', ' ');
//        }
//        tempMB.setString( message );
//        c2b.convert( tempMB );
//        outputMsg.appendBytes(tempMB);
//
//        // XXX add headers
//
//        MimeHeaders headers=res.getMimeHeaders();
//        String contentType = res.getContentType();
//        if( contentType != null ) {
//            headers.setValue("Content-Type").setString(contentType);
//        }
//        String contentLanguage = res.getContentLanguage();
//        if( contentLanguage != null ) {
//            headers.setValue("Content-Language").setString(contentLanguage);
//        }
//        long contentLength = res.getContentLengthLong();
//        if( contentLength >= 0 ) {
//            headers.setValue("Content-Length").setLong(contentLength);
//        }
//        int numHeaders = headers.size();
//        outputMsg.appendInt(numHeaders);
//        for( int i=0; i<numHeaders; i++ ) {
//            MessageBytes hN=headers.getName(i);
//            // no header to sc conversion - there's little benefit
//            // on this direction
//            c2b.convert ( hN );
//            outputMsg.appendBytes( hN );
//
//            MessageBytes hV=headers.getValue(i);
//            c2b.convert( hV );
//            outputMsg.appendBytes( hV );
//        }
//        mc.getSource().send( outputMsg, mc );
    }

    /**
     * Set the replay buffer for Form auth
     */
    public void setReplay(ByteChunk replay) {
        isFirst = false;
        isEmpty = false;
        isReplay = true;
        bodyBuff.setBytes(replay.getBytes(), replay.getStart(), replay.getLength());
    }


}
