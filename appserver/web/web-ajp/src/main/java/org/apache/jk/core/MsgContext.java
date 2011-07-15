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
import java.io.ByteArrayInputStream;
import java.net.InetAddress;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.logging.*;

import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;

import org.apache.jk.common.JkInputStream;
import org.glassfish.grizzly.http.HttpRequestPacket;
import org.glassfish.grizzly.http.HttpResponsePacket;
import org.glassfish.grizzly.http.util.ByteChunk;
import org.glassfish.grizzly.http.util.C2BConverter;
import org.glassfish.grizzly.http.util.MessageBytes;
import org.glassfish.grizzly.ssl.SSLSupport;


/**
 *
 * @author Henri Gomez [hgomez@apache.org]
 * @author Dan Milstein [danmil@shore.net]
 * @author Keith Wannamaker [Keith@Wannamaker.org]
 * @author Kevin Seguin
 * @author Costin Manolache
 */
public class MsgContext implements ActionHook {

    private static final Logger log = Logger.getLogger(MsgContext.class.getName());
    private static final Logger logTime = Logger.getLogger( "org.apache.jk.REQ_TIME" );

    private int type;
    private Object notes[]=new Object[32];
    private JkHandler next;
    private JkChannel source;
    private JkInputStream jkIS;
    private C2BConverter c2b;
    private HttpRequestPacket req;
    private WorkerEnv wEnv;
    private Msg msgs[]=new Msg[10];
    private int status=0;
    // Control object
    private Object control;

    // Application managed, like notes
    private long timers[]=new long[20];
    
    // The context can be used by JNI components as well
    private long jkEndpointP;
    private long xEnvP;

    // Temp: use notes and dynamic strings
    public static final int TIMER_RECEIVED=0;
    public static final int TIMER_PRE_REQUEST=1;
    public static final int TIMER_POST_REQUEST=2;

    // Status codes
    public static final int JK_STATUS_NEW=0;
    public static final int JK_STATUS_HEAD=1;
    public static final int JK_STATUS_CLOSED=2;
    public static final int JK_STATUS_ERROR=3;

    public MsgContext(int bsize) {
        try {
            c2b = new C2BConverter("iso-8859-1");
        } catch(IOException iex) {
            log.log(Level.WARNING, "Can't happen", iex);
        }
       jkIS = new JkInputStream(this, bsize);
    }

    /**
     * @deprecated
     */
    public MsgContext() {
        this(8*1024);
    }
    
    public final Object getNote( int id ) {
        return notes[id];
    }

    public final void setNote( int id, Object o ) {
        notes[id]=o;
    }

    /** The id of the chain */
    public final int getType() {
        return type;
    }

    public final void setType(int i) {
        type=i;
    }

    public final void setLong( int i, long l) {
        timers[i]=l;
    }
    
    public final long getLong( int i) {
        return timers[i];
    }
    
    // Common attributes ( XXX should be notes for flexibility ? )

    public final WorkerEnv getWorkerEnv() {
        return wEnv;
    }

    public final void setWorkerEnv( WorkerEnv we ) {
        this.wEnv=we;
    }
    
    public final JkChannel getSource() {
        return source;
    }
    
    public final void setSource(JkChannel ch) {
        this.source=ch;
    }

    public final int getStatus() {
        return status;
    }

    public final void setStatus( int s ) {
        status=s;
    }
    
    public final JkHandler getNext() {
        return next;
    }
    
    public final void setNext(JkHandler ch) {
        this.next=ch;
    }

    /** The high level request object associated with this context
     */
    public final void setRequest( HttpRequestPacket req ) {
//        this.req=req;
//        req.setInputBuffer(jkIS);
//        HttpResponsePacket res = req.getResponse();
//        res.setOutputBuffer(jkIS);
//        res.setHook(this);
    }

    public final HttpRequestPacket getRequest() {
        return req;
    }

    /** The context may store a number of messages ( buffers + marshalling )
     */
    public final Msg getMsg(int i) {
        return msgs[i];
    }

    public final void setMsg(int i, Msg msg) {
        this.msgs[i]=msg;
    }

    public final C2BConverter getConverter() {
        return c2b;
    }

    public final void setConverter(C2BConverter c2b) {
        this.c2b = c2b;
    }
    
    public final boolean isLogTimeEnabled() {
        return logTime.isLoggable(Level.FINEST);
    }

    public JkInputStream getInputStream() {
        return jkIS;
    }

    /** Each context contains a number of byte[] buffers used for communication.
     *  The C side will contain a char * equivalent - both buffers are long-lived
     *  and recycled.
     *
     *  This will be called at init time. A long-lived global reference to the byte[]
     *  will be stored in the C context.
     */
    public byte[] getBuffer( int id ) {
        // We use a single buffer right now. 
        if( msgs[id]==null ) {
            return null;
        }
        return msgs[id].getBuffer();
    }

    /** Invoke a java hook. The xEnv is the representation of the current execution
     *  environment ( the jni_env_t * )
     */
    public int execute() throws IOException {
        int status=next.invoke(msgs[0], this);
        return status;
    }

    // -------------------- Jni support --------------------
    
    /** Store native execution context data when this handler is called
     *  from JNI. This will change on each call, represent temproary
     *  call data.
     */
    public void setJniEnv( long xEnvP ) {
            this.xEnvP=xEnvP;
    }

    public long getJniEnv() {
        return xEnvP;
    }
    
    /** The long-lived JNI context associated with this java context.
     *  The 2 share pointers to buffers and cache data to avoid expensive
     *  jni calls.
     */
    public void setJniContext( long cContext ) {
        this.jkEndpointP=cContext;
    }

    public long getJniContext() {
        return jkEndpointP;
    }

    public Object getControl() {
        return control;
    }

    public void setControl(Object control) {
        this.control = control;
    }

    // -------------------- Coyote Action implementation --------------------
    
    public void action(ActionCode actionCode, Object param) {
        if( actionCode==ActionCode.ACTION_COMMIT ) {
            if( log.isLoggable(Level.FINEST) ) {
                log.finest("COMMIT " );
            }
            Response res=(Response)param;

            if(  res.isCommitted() ) {
                if( log.isLoggable(Level.FINEST) ) {
                    log.finest("Response already committed " );
                }
            } else {
                try {
                    jkIS.appendHead( res );
                } catch(IOException iex) {
                    log.log(Level.WARNING, "Unable to send headers",iex);
                    setStatus(JK_STATUS_ERROR);
                }
            }
        } else if( actionCode==ActionCode.ACTION_RESET ) {
            if( log.isLoggable(Level.FINEST) ) {
                log.finest("RESET " );
            }
        } else if( actionCode==ActionCode.ACTION_CLIENT_FLUSH ) {
            if( log.isLoggable(Level.FINEST) ) {
                log.finest("CLIENT_FLUSH " );
            }
            Response res = (Response)param;
            if(!res.isCommitted()) {
		action(ActionCode.ACTION_COMMIT, res);
	    }
            try {
                source.flush( null, this );
            } catch(IOException iex) {
                // This is logged elsewhere, so debug only here
                log.log(Level.FINEST, "Error during flush",iex);
//                res.setErrorException(iex);
                res.setError();
                setStatus(JK_STATUS_ERROR);
            }
            
        } else if( actionCode==ActionCode.ACTION_CLOSE ) {
            if( log.isLoggable(Level.FINEST) ) {
                log.finest("CLOSE " );
            }
            Response res=(Response)param;
            if( getStatus()== JK_STATUS_CLOSED || getStatus() == JK_STATUS_ERROR) {
                // Double close - it may happen with forward 
                if( log.isLoggable(Level.FINEST) ) {
                    log.log(Level.FINEST, "Double CLOSE - forward ? {0}",
                            res.getRequest().getRequestURI());
                }
                return;
            }
                 
            if( !res.isCommitted() )
                this.action( ActionCode.ACTION_COMMIT, param );
            try {            
                jkIS.endMessage();
            } catch(IOException iex) {
                log.log(Level.FINEST, "Error sending end packet",iex);
                setStatus(JK_STATUS_ERROR);
            }
            if(getStatus() != JK_STATUS_ERROR) {
                setStatus(JK_STATUS_CLOSED );
            }

            if( logTime.isLoggable(Level.FINEST) ) 
                logTime(res.getRequest(), res);
        } else if( actionCode==ActionCode.ACTION_REQ_SSL_ATTRIBUTE ) {
            Request req=(Request)param;

            // Extract SSL certificate information (if requested)
            MessageBytes certString = (MessageBytes)req.getNote(WorkerEnv.SSL_CERT_NOTE);
            if( certString != null && !certString.isNull() ) {
                ByteChunk certData = certString.getByteChunk();
                ByteArrayInputStream bais = 
                    new ByteArrayInputStream(certData.getBytes(),
                                             certData.getStart(),
                                             certData.getLength());
 
                // Fill the first element.
                X509Certificate jsseCerts[] = null;
                try {
                    CertificateFactory cf =
                        CertificateFactory.getInstance("X.509");
                    while(bais.available() > 0) {
                        X509Certificate cert = (X509Certificate)
                            cf.generateCertificate(bais);
                        if(jsseCerts == null) {
                            jsseCerts =  new X509Certificate[1];
                            jsseCerts[0] = cert;
                        } else {
                            X509Certificate [] temp = new X509Certificate[jsseCerts.length+1];
                            System.arraycopy(jsseCerts,0,temp,0,jsseCerts.length);
                            temp[jsseCerts.length] = cert;
                            jsseCerts = temp;
                        }
                    }
                } catch(java.security.cert.CertificateException e) {
                    log.log(Level.SEVERE, "Certificate convertion failed" , e );
                    return;
                }
 
                req.setAttribute(SSLSupport.CERTIFICATE_KEY, 
                                 jsseCerts);
            }
                
        } else if( actionCode==ActionCode.ACTION_REQ_HOST_ATTRIBUTE ) {
            HttpRequestPacket req = (HttpRequestPacket) param;

            // If remoteHost not set by JK, get it's name from it's remoteAddr
            if( req.remoteHost().isNull()) {
                try {
                    req.remoteHost().setString(InetAddress.getByName(
                                               req.remoteAddr().toString()).
                                               getHostName());
                } catch(IOException iex) {
                    if(log.isLoggable(Level.FINEST)) {
                        log.finest("Unable to resolve "+req.remoteAddr());
                    }
                }
            }
        } else if( actionCode==ActionCode.ACTION_ACK ) {
            if( log.isLoggable(Level.FINEST) ) {
                log.finest("ACK " );
            }
        } else if ( actionCode == ActionCode.ACTION_REQ_SET_BODY_REPLAY ) {
            if( log.isLoggable(Level.FINEST) ) {
                log.finest("Replay ");
            }
            ByteChunk bc = (ByteChunk)param;
            jkIS.setReplay(bc);
            req.setContentLength(bc.getLength()); // reset so re-read
        }
    }
    

    private void logTime(Request req, Response res ) {
        // called after the request
        //            org.glassfish.grizzly.http.server.Request req=(org.glassfish.grizzly.http.server.Request)param;
        //            Response res=req.getResponse();
        String uri=req.getRequestURI();
        if( uri.indexOf( ".gif" ) >0 ) return;
        
        setLong( MsgContext.TIMER_POST_REQUEST, System.currentTimeMillis());
        long t1= getLong( MsgContext.TIMER_PRE_REQUEST ) -
            getLong( MsgContext.TIMER_RECEIVED );
        long t2= getLong( MsgContext.TIMER_POST_REQUEST ) -
            getLong( MsgContext.TIMER_PRE_REQUEST );
        
        logTime.log(Level.FINEST, "Time pre={0}/ service={1} {2} {3}",
                new Object[]{t1, t2, res.getContentLength(), uri});
    }

    public void recycle() {
        jkIS.recycle();
    }
}
