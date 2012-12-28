/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.admin.payload;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

/**
 * Implementation of a Payload.Inbound that works with InputStreams
 * 
 * Usage is like this:
 *
 *      CommandRunner.CommandInvocation ci =
 *              commandRunner.getCommandInvocation("some-command", areport, theSubject);
 *      InputStreamPayload in = new InputStreamInboundPayload();
 *      // get an InputStream is from somewhere
 *      in.addStream("someoption", is)
 *      // get another InputStream for the operand
 *      in.addStream("DEFAULT", is)
 *      ParameterMap map = new ParameterMap();
 *      // populate map with other options
 *      ci.inbound(in).parameters(map).execute();
 *
 * @author Tom Mueller
 */
public class InputStreamInboundPayload extends PayloadImpl.Inbound {
    private Map<String,InputStream> args = new HashMap<String,InputStream>();

    public void addStream(String name, InputStream is) {
        args.put(name, is);
    }

    @Override
    public Iterator<org.glassfish.api.admin.Payload.Part> parts() {
        return new Iterator<org.glassfish.api.admin.Payload.Part>() {

            private Iterator<Map.Entry<String,InputStream>> argiter = args.entrySet().iterator();

            @Override
            public boolean hasNext() {
                return argiter.hasNext();
            }

            @Override
            public org.glassfish.api.admin.Payload.Part next() {
                Map.Entry<String,InputStream> e = argiter.next();
                Properties props = new Properties();
                props.setProperty("data-request-type", "file-xfer");
                props.setProperty("data-request-name", e.getKey());
                return PayloadImpl.Part.newInstance("application/binary", e.getKey(), props, e.getValue());
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
}
