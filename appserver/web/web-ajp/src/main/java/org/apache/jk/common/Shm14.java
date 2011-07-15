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
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.logging.*;

import org.apache.jk.core.Msg;
import org.apache.jk.core.MsgContext;
import org.glassfish.grizzly.http.server.util.IntrospectionUtils;

/** Shm implementation using JDK1.4 nio.
 *
 *
 * @author Costin Manolache
 */
public class Shm14 extends Shm {
    
    
    // Not ready yet.
    
    private static Logger log=
        Logger.getLogger( Shm14.class.getName() );
    
    MappedByteBuffer bb;

    public void init() {
        try {
            RandomAccessFile f=new RandomAccessFile( file, "rw" );
            FileChannel fc=f.getChannel();
            
            bb=fc.map( FileChannel.MapMode.READ_WRITE, 0, f.length());
        } catch( IOException ex ) {
            ex.printStackTrace();
        }
    }

    public void dumpScoreboard(String file) {
        // We can only sync with our backing store.
        bb.force();
        // XXX we should copy the content to the file
    }

    public void resetScoreboard() throws IOException {
        // XXX Need to write the head
    }


    public  int invoke(Msg msg, MsgContext ep )
        throws IOException
    {
        if (log.isLoggable(Level.FINEST))
            log.finest("ChannelShm14.invoke: "  + ep );

        // 
        
        return 0;
    }    

    public void initCli() {
    }

    public static void main( String args[] ) {
        try {
            Shm14 shm=new Shm14();

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
