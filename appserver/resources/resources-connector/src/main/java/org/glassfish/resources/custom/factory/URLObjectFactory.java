/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.resources.custom.factory;

import com.sun.logging.LogDomains;

import javax.naming.spi.ObjectFactory;
import javax.naming.*;
import java.io.Serializable;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.net.URL;

public class URLObjectFactory implements Serializable, ObjectFactory {
    public Object getObjectInstance(Object obj, Name name, Context nameCtx, Hashtable<?, ?> environment) throws Exception {
        Reference ref = (Reference)obj;

        Enumeration<RefAddr> refAddrs = ref.getAll();

        String protocol = null;
        String host = null;
        int port = -1;
        String file = null;
        String spec = null;

        while(refAddrs.hasMoreElements()){
            RefAddr addr = refAddrs.nextElement();

            String type = addr.getType();
            String content = (String)addr.getContent();
            if(type.equalsIgnoreCase("protocol")){
                protocol = content;
            }else if(type.equalsIgnoreCase("host")){
                host = content;
            }else if(type.equalsIgnoreCase("port")){
                try{
                    port = Integer.parseInt(content);
                }catch(NumberFormatException nfe){
                    Object args[] = new Object[]{content, nfe};
                    Logger.getLogger(LogDomains.RSR_LOGGER).log(Level.WARNING, "invalid.port.number", args);
                    IllegalArgumentException iae = new IllegalArgumentException("Invalid value for port");
                    iae.initCause(nfe);
                    throw iae;
                }
            }else if(type.equalsIgnoreCase("file")){
                file = content;
            }else if(type.equalsIgnoreCase("spec")){
                spec = content;
            }
        }

        if(protocol != null && host != null && port != -1 && file != null){
            return new URL(protocol, host, port, file);
        }else if(protocol != null && host != null && file != null){
            return new URL(protocol, host, file);
        }else if(spec != null){
            return new URL(spec);
        }

        throw new IllegalArgumentException("URLObjectFactory does not have necessary parameters for URL construction");
    }
}
