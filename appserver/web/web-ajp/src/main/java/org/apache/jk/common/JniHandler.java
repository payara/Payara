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

import javax.management.ObjectName;

import org.apache.jk.apr.AprImpl;
import org.apache.jk.core.JkHandler;
import org.apache.jk.core.Msg;
import org.apache.jk.core.MsgContext;
import org.apache.jk.core.JkChannel;
import org.apache.tomcat.util.modeler.Registry;
import org.glassfish.grizzly.http.util.ByteChunk;
import org.glassfish.grizzly.http.util.C2BConverter;
import org.glassfish.grizzly.http.util.MessageBytes;


/**
 * Base class for components using native code ( libjkjni.so ).
 * It allows to access the jk_env and wrap ( 'box' ? ) a native
 * jk component, and call it's methods.
 *
 * Note that get/setAttribute are expensive ( Strings, etc ),
 * invoke() is were all optimizations are done. We do recycle
 * all memory on both C and java sides ( the only exception is
 * when we attempt pinning but the VM doesn't support it ). The
 * low level optimizations from ByteBuffer, etc are used to
 * reduce the overhead of passing strings.
 *
 * @author Costin Manolache
 */
public class JniHandler extends JkHandler {
    protected AprImpl apr;

    // The native side handler
    protected long nativeJkHandlerP;

    protected String jkHome;

    // Dispatch table codes. Hardcoded for now, will change when we have more handlers.
    public static final int JK_HANDLE_JNI_DISPATCH=0x15;
    public static final int JK_HANDLE_SHM_DISPATCH=0x16;


    public static final int MSG_NOTE=0;
    public static final int MB_NOTE=2;
    private boolean paused = false;


    public JniHandler() {
    }

    /**
     */
    public void setJkHome( String s ) {
        jkHome=s;
    }

    public String getJkHome() {
        return jkHome;
    }

    /** You must call initNative() inside the component init()
     */
    public void init() throws IOException {
        // static field init, temp
    }

    protected void initNative(String nativeComponentName) {
        apr=(AprImpl)wEnv.getHandler("apr");
        if( apr==null ) {
            // In most cases we can just load it automatically.
            // that requires all libs to be installed in standard places
            // ( LD_LIBRARY_PATH, /usr/lib
            try {
                apr=new AprImpl();
                wEnv.addHandler("apr", apr);
                apr.init();
                if( oname != null ) {
                    ObjectName aprname=new ObjectName(oname.getDomain() +
                            ":type=JkHandler, name=apr");
                    Registry.getRegistry(null, null).registerComponent(apr, aprname, null);
                }
            } catch( Throwable t ) {
                log.log(Level.FINEST, "Can't load apr", t);
                apr=null;
            }
        }
        if( apr==null || ! apr.isLoaded() ) {
            if( log.isLoggable(Level.FINEST) )
                log.finest("No apr, disabling jni proxy ");
            apr=null;
            return;
        }

        try {
            long xEnv=apr.getJkEnv();
            nativeJkHandlerP=apr.getJkHandler(xEnv, nativeComponentName );
            
            if( nativeJkHandlerP==0 ) {
                log.finest("Component not found, creating it " + nativeComponentName );
                nativeJkHandlerP=apr.createJkHandler(xEnv, nativeComponentName);
            }
            log.finest("Native proxy " + nativeJkHandlerP );
            apr.releaseJkEnv(xEnv);
        } catch( Throwable t ) {
            apr=null;
            log.log(Level.INFO, "Error calling apr ", t);
        }
   }

    public void appendString( Msg msg, String s, C2BConverter charsetDecoder)
        throws IOException
    {
        ByteChunk bc=charsetDecoder.getByteChunk();
        charsetDecoder.recycle();
        charsetDecoder.convert( s );
        charsetDecoder.flushBuffer();
        msg.appendByteChunk( bc );
    }

    public void pause() throws Exception {
        synchronized(this) {
            paused = true;
        }
    }

    public void resume() throws Exception {
        synchronized(this) {
            paused = false;
            notifyAll();
        }
    }


    /** Create a msg context to be used with the shm channel
     */
    public MsgContext createMsgContext() {
        if( nativeJkHandlerP==0 || apr==null  )
            return null;

        synchronized(this) {
            try{ 
                while(paused) {
                    wait();
                }
            }catch(InterruptedException ie) {
                // Ignore, since it can't happen
            }
        }

        try {
            MsgContext msgCtx=new MsgContext();
            MsgAjp msg=new MsgAjp();

            msgCtx.setSource( (JkChannel)this );
            msgCtx.setWorkerEnv( wEnv );

            msgCtx.setNext( this );

            msgCtx.setMsg( MSG_NOTE, msg); // XXX Use noteId

            C2BConverter c2b=new C2BConverter(  "iso-8859-1" );
            msgCtx.setConverter( c2b );

            MessageBytes tmpMB= MessageBytes.newInstance();
            msgCtx.setNote( MB_NOTE, tmpMB );
            return msgCtx;
        } catch( Exception ex ) {
            log.log(Level.SEVERE, "Can't create endpoint", ex);
            return null;
        }
    }

    public void setNativeAttribute(String name, String val) throws IOException {
        if( apr==null ) return;

        if( nativeJkHandlerP == 0 ) {
            log.severe( "Unitialized component " + name+ " " + val );
            return;
        }

        long xEnv=apr.getJkEnv();

        apr.jkSetAttribute( xEnv, nativeJkHandlerP, name, val );

        apr.releaseJkEnv( xEnv );
    }

    public void initJkComponent() throws IOException {
        if( apr==null ) return;

        if( nativeJkHandlerP == 0 ) {
            log.severe( "Unitialized component " );
            return;
        }

        long xEnv=apr.getJkEnv();

        apr.jkInit( xEnv, nativeJkHandlerP );

        apr.releaseJkEnv( xEnv );
    }

    public void destroyJkComponent() throws IOException {
        if( apr==null ) return;

        if( nativeJkHandlerP == 0 ) {
            log.severe( "Unitialized component " );
            return;
        }

        long xEnv=apr.getJkEnv();

        apr.jkDestroy( xEnv, nativeJkHandlerP );

        apr.releaseJkEnv( xEnv );
    }



    protected void setNativeEndpoint(MsgContext msgCtx) {
        long xEnv=apr.getJkEnv();
        msgCtx.setJniEnv( xEnv );

        long epP=apr.createJkHandler(xEnv, "endpoint");
        log.finest("create ep " + epP );
        if( epP == 0 ) return;
        apr.jkInit( xEnv, epP );
        msgCtx.setJniContext( epP );

    }

    protected void recycleNative(MsgContext ep) {
        apr.jkRecycle(ep.getJniEnv(), ep.getJniContext());
    }

    /** send and get the response in the same buffer. This calls the
    * method on the wrapped jk_bean object.
     */
    protected int nativeDispatch( Msg msg, MsgContext ep, int code, int raw )
        throws IOException
    {
        if( log.isLoggable(Level.FINEST) )
            log.finest( "Sending packet " + code + " " + raw);

        if( raw == 0 ) {
            msg.end();

            if( log.isLoggable(Level.FINEST) ) msg.dump("OUT:" );
        }

        // Create ( or reuse ) the jk_endpoint ( the native pair of
        // MsgContext )
        long xEnv=ep.getJniEnv();
        long nativeContext=ep.getJniContext();
        if( nativeContext==0 || xEnv==0 ) {
            setNativeEndpoint( ep );
            xEnv=ep.getJniEnv();
            nativeContext=ep.getJniContext();
        }

        if( xEnv==0 || nativeContext==0 || nativeJkHandlerP==0 ) {
            log.severe("invokeNative: Null pointer ");
            return -1;
        }

        // Will process the message in the current thread.
        // No wait needed to receive the response, if any
        int status=AprImpl.jkInvoke( xEnv,
                                 nativeJkHandlerP,
                                 nativeContext,
                                 code, msg.getBuffer(), 0, msg.getLen(), raw );

        if( status != 0 && status != 2 ) {
            log.log(Level.SEVERE, "nativeDispatch: error " + status, new Throwable() );
        }

        if( log.isLoggable(Level.FINEST) ) log.finest( "Sending packet - done " + status);
        return status;
    }

    /** Base implementation for invoke. Dispatch the action to the native
    * code, where invoke() is called on the wrapped jk_bean.
    */
    public  int invoke(Msg msg, MsgContext ep )
        throws IOException
    {
        long xEnv=ep.getJniEnv();
        int type=ep.getType();

        int status=nativeDispatch(msg, ep, type, 0 );

        apr.jkRecycle(xEnv, ep.getJniContext());
        apr.releaseJkEnv( xEnv );
        return status;
    }

    private static Logger log=
        Logger.getLogger( JniHandler.class.getName() );
}
