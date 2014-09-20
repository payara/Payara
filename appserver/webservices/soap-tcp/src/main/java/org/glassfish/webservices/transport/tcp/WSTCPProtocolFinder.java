/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.webservices.transport.tcp;

import com.sun.xml.ws.transport.tcp.util.TCPConstants;
import java.io.UnsupportedEncodingException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.portunif.PUContext;
import org.glassfish.grizzly.portunif.ProtocolFinder;

/**
 *
 * @author Alexey Stashok
 */
public class WSTCPProtocolFinder implements ProtocolFinder {

    private static final Logger LOGGER = LogUtils.getLogger();

    private final static byte[] PROTOCOL_SCHEMA_BYTES;

    static {
        byte[] bytes;
        try {
            bytes = TCPConstants.PROTOCOL_SCHEMA.getBytes("US-ASCII");
        } catch (UnsupportedEncodingException e) {
            LOGGER.log(Level.WARNING, LogUtils.CANNOT_CONVERT_PROTOCOL_ID, e);
            bytes = TCPConstants.PROTOCOL_SCHEMA.getBytes();
        }

        PROTOCOL_SCHEMA_BYTES = bytes;
    }

    @Override
    public Result find(final PUContext puContext,
            final FilterChainContext filterChainContext) {

        final Buffer buffer = filterChainContext.getMessage();
        if (buffer.remaining() < PROTOCOL_SCHEMA_BYTES.length) {
            return Result.NEED_MORE_DATA;
        }

        final int pos = buffer.position();
        for (int i = 0; i < PROTOCOL_SCHEMA_BYTES.length; i++) {
            if (buffer.get(pos + i) != PROTOCOL_SCHEMA_BYTES[i]) {
                return Result.NOT_FOUND;
            }
        }

        return Result.FOUND;
    }
}
