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
import java.util.Vector;
import java.util.logging.*;

import org.apache.jk.apr.AprImpl;
import org.apache.jk.core.Msg;
import org.apache.jk.core.MsgContext;
import org.apache.jk.core.WorkerEnv;
import org.glassfish.grizzly.http.server.util.IntrospectionUtils;
import org.glassfish.grizzly.http.util.C2BConverter;

/* The code is a bit confusing at this moment - the class is used as
   a Bean, or ant Task, or CLI - i.e. you set properties and call execute.

   That's different from the rest of jk handlers wich are stateless ( but
   similar with Coyote-http ).
*/


/** Handle the shared memory objects.
 *
 * @author Costin Manolache
 */
public class Shm extends JniHandler {
    String file="/tmp/shm.file";
    int size;
    String host="localhost";
    int port=8009;
    String unixSocket;

    boolean help=false;
    boolean unregister=false;
    boolean reset=false;
    String dumpFile=null;

    Vector<String> groups = new Vector<String>();
    
    // Will be dynamic ( getMethodId() ) after things are stable 
    static final int SHM_WRITE_SLOT=2;
    static final int SHM_RESET=5;
    static final int SHM_DUMP=6;
    
    public Shm() {
    }

    /** Scoreboard location
     */
    public void setFile( String f ) {
        file=f;
    }

    /** Copy the scoreboard in a file for debugging
     *  Will also log a lot of information about what's in the scoreboard.
     */
    public void setDump( String dumpFile ) {
        this.dumpFile=dumpFile;
    }
    
    /** Size. Used only if the scoreboard is to be created.
     */
    public void setSize( int size ) {
        this.size=size;
    }

    /** Set this to get the scoreboard reset.
     *  The shm segment will be destroyed and a new one created,
     *  with the provided size.
     *
     *  Requires "file" and "size".
     */
    public void setReset(boolean b) {
        reset=true;
    }

    /** Ajp13 host
     */
    public void setHost( String host ) {
        this.host=host;
    }

    /** Mark this instance as belonging to a group
     */
    public void setGroup( String grp ) {
        groups.addElement( grp );
    }

    /** Ajp13 port
     */
    public void setPort( int port ) {
        this.port=port;
    }

    /** Unix socket where tomcat is listening.
     *  Use it only if tomcat is on the same host, of course
     */
    public void setUnixSocket( String unixSocket  ) {
        this.unixSocket=unixSocket;
    }

    /** Set this option to mark the tomcat instance as
        'down', so apache will no longer forward messages to it.
        Note that requests with a session will still try this
        host first.

        This can be used to implement gracefull shutdown.

        Host and port are still required, since they are used
        to identify tomcat.
    */
    public void setUnregister( boolean unregister  ) {
        this.unregister=true;
    }
    
    public void init() throws IOException {
        super.initNative( "shm" );
        if( apr==null ) return;
        if( file==null ) {
            log.severe("No shm file, disabling shared memory");
            apr=null;
            return;
        }

        // Set properties and call init.
        setNativeAttribute( "file", file );
        if( size > 0 )
            setNativeAttribute( "size", Integer.toString( size ) );
        
        initJkComponent();
    }

    public void resetScoreboard() throws IOException {
        if( apr==null ) return;
        MsgContext mCtx=createMsgContext();
        Msg msg=(Msg)mCtx.getMsg(0);
        msg.reset();

        msg.appendByte( SHM_RESET );
        
        this.invoke( msg, mCtx );
    }

    public void dumpScoreboard(String fname) throws IOException {
        if( apr==null ) return;
        MsgContext mCtx=createMsgContext();
        Msg msg=(Msg)mCtx.getMsg(0);
        C2BConverter c2b=mCtx.getConverter();
        msg.reset();

        msg.appendByte( SHM_DUMP );

        appendString( msg, fname, c2b);
        
        this.invoke( msg, mCtx );
    }

    /** Register a tomcat instance
     *  XXX make it more flexible
     */
    public void registerTomcat(String host, int port, String unixDomain)
        throws IOException
    {
        String instanceId=host+":" + port;

        String slotName="TOMCAT:" + instanceId;
        MsgContext mCtx=createMsgContext();
        Msg msg=(Msg)mCtx.getMsg(0);
        msg.reset();
        C2BConverter c2b=mCtx.getConverter();
        
        msg.appendByte( SHM_WRITE_SLOT );
        appendString( msg, slotName, c2b );

        int channelCnt=1;
        if( unixDomain != null ) channelCnt++;

        // number of groups. 0 means the default lb.
        msg.appendInt( groups.size() );
        for( int i=0; i<groups.size(); i++ ) {
            appendString( msg, (String)groups.elementAt( i ), c2b);
            appendString( msg, instanceId, c2b);
        }
        
        // number of channels for this instance
        msg.appendInt( channelCnt );
        
        // The body:
        appendString(msg, "channel.socket:" + host + ":" + port, c2b );
        msg.appendInt( 1 );
        appendString(msg, "tomcatId", c2b);
        appendString(msg, instanceId, c2b);

        if( unixDomain != null ) {
            appendString(msg, "channel.apr:" + unixDomain, c2b );
            msg.appendInt(1);
            appendString(msg, "tomcatId", c2b);
            appendString(msg, instanceId, c2b);
        }

        if (log.isLoggable(Level.FINEST))
            log.finest("Register " + instanceId );
        this.invoke( msg, mCtx );
    }

    public void unRegisterTomcat(String host, int port)
        throws IOException
    {
        String slotName="TOMCAT:" + host + ":" + port;
        MsgContext mCtx=createMsgContext();
        Msg msg=(Msg)mCtx.getMsg(0);
        msg.reset();
        C2BConverter c2b=mCtx.getConverter();
        
        msg.appendByte( SHM_WRITE_SLOT );
        appendString( msg, slotName, c2b );

        // number of channels for this instance
        msg.appendInt( 0 );
        msg.appendInt( 0 );
        
        if (log.isLoggable(Level.FINEST))
            log.finest("UnRegister " + slotName );
        this.invoke( msg, mCtx );
    }

    public void destroy() throws IOException {
        destroyJkComponent();
    }

    
    public  int invoke(Msg msg, MsgContext ep )
        throws IOException
    {
        if( apr==null ) return 0;
        log.finest("ChannelShm.invoke: "  + ep );
        super.nativeDispatch( msg, ep, JK_HANDLE_SHM_DISPATCH, 0 );
        return 0;
    }    

    private static Logger log = Logger.getLogger( Shm.class.getName() );

    
    //-------------------- Main - use the shm functions from ant or CLI ------

    /** Local initialization - for standalone use
     */
    public void initCli() throws IOException {
        WorkerEnv wEnv=new WorkerEnv();
        AprImpl apr=new AprImpl();
        wEnv.addHandler( "apr", apr );
        wEnv.addHandler( "shm", this );
        apr.init();
        if( ! apr.isLoaded() ) {
            log.severe("No native support. " +
                       "Make sure libapr.so and libjkjni.so are available in LD_LIBRARY_PATH");
            return;
        }
    }
    
    public void execute() {
        try {
            if( help ) return;
            initCli();
            init();

            if( reset ) {
                resetScoreboard();
            } else if( dumpFile!=null ) {
                dumpScoreboard(dumpFile);
            } else if( unregister ) {
                unRegisterTomcat( host, port );
            } else {
                registerTomcat( host, port, unixSocket );
            }
        } catch (Exception ex ) {
            log.log(Level.SEVERE, "Error executing Shm", ex);
        }
    }

    public void setHelp( boolean b ) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Usage: ");
            log.finest("  Shm [OPTIONS]");
            log.finest("");
            log.finest("  -file SHM_FILE");
            log.finest("  -group GROUP ( can be specified multiple times )");
            log.finest("  -host HOST");
            log.finest("  -port PORT");
            log.finest("  -unixSocket UNIX_FILE");
            //        log.finest("  -priority XXX");
            //        log.finest("  -lbFactor XXX");
        }
        help=true;
        return;
    }
    
    public static void main( String args[] ) {
        try {
            Shm shm=new Shm();

            if( args.length == 0 ||
                ( "-?".equals(args[0]) ) ) {
                shm.setHelp( true );
                return;
            }

            IntrospectionUtils.processArgs( shm, args);
            shm.execute();
        } catch( Exception ex ) {
            ex.printStackTrace();
        }
    }
}
