/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.config.portunif;

import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.attributes.Attribute;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.portunif.PUContext;
import org.glassfish.grizzly.portunif.ProtocolFinder;

/**
 * A {@link ProtocolFinder} implementation that parse the available
 * SocketChannel bytes looking for the 'http' bytes. An http request will
 * always has the form of:
 *
 * METHOD URI PROTOCOL/VERSION
 *
 * example: GET / HTTP/1.1
 *
 * The algorithm will try to find the protocol token. 
 *
 * @author Jeanfrancois Arcand
 * @author Alexey Stashok
 */
public class HttpProtocolFinder implements ProtocolFinder {
    private static final char[] METHOD_FIRST_LETTERS = new char[] {'G', 'P', 'O', 'H', 'D', 'T', 'C'};
    private final Attribute<ParsingState> parsingStateAttribute =
            Grizzly.DEFAULT_ATTRIBUTE_BUILDER.createAttribute(
                    HttpProtocolFinder.class + "-" + hashCode()
                            + ".parsingStateAttribute");

    private final int maxRequestLineSize;

    public HttpProtocolFinder() {
        this(2048);
    }

    public HttpProtocolFinder(int maxRequestLineSize) {
        this.maxRequestLineSize = maxRequestLineSize;
    }

    @Override
    public Result find(final PUContext puContext, final FilterChainContext ctx) {
        final Connection connection = ctx.getConnection();
        final Buffer buffer = ctx.getMessage();

        final ParsingState parsingState = parsingStateAttribute.get(connection);

        final int limit = buffer.limit();

        int position;
        int state;

        if (parsingState == null) {
            position = buffer.position();
            state = 0;
        } else {
            position = parsingState.position;
            state = parsingState.state;
        }
        
        byte c = 0;
        byte c2;

        // Rule b - try to determine the context-root
        while (position < limit) {
            c2 = c;
            c = buffer.get(position++);
            // State Machine
            // 0 - Search for the first SPACE ' ' between the method and the
            //     the request URI
            // 1 - Search for the second SPACE ' ' between the request URI
            //     and the method
            _1:
            switch (state) {
                case 0:
                    // Check method name
                    for (int i = 0; i < METHOD_FIRST_LETTERS.length; i++) {
                        if (c == METHOD_FIRST_LETTERS[i]) {
                            state = 1;
                            break _1;
                        }
                    }
                    
                    return Result.NOT_FOUND;
                case 1:
                    // Search for first ' '
                    if (c == 0x20) {
                        state = 2;
                    }
                    break;
                case 2:
                    // Search for next ' '
                    if (c == 0x20) {
                        state = 3;
                    }
                    break;
                case 3:
                    // Check 'H' part of HTTP/
                    if (c == 'H') {
                        state = 4;
                        break;
                    }
                    return Result.NOT_FOUND;

                case 4:
                    // Search for P/ (part of HTTP/)
                    if (c == 0x2f && c2 == 'P') {
                        // find SSL preprocessor
                        if (parsingState != null) {
                            parsingStateAttribute.remove(connection);
                        }
                        
                        return Result.FOUND;
                    }
                    break;
                default:
                    return Result.NOT_FOUND;
            }
        }

        if (position >= maxRequestLineSize) {
            return Result.NOT_FOUND;
        }
        
        if (parsingState == null) {
            parsingStateAttribute.set(connection, new ParsingState(position, state));
        } else {
            parsingState.position = position;
            parsingState.state = state;
        }
        
        return Result.NEED_MORE_DATA;
    }

    private static final class ParsingState {
        int position;
        int state;

        public ParsingState(int position, int state) {
            this.position = position;
            this.state = state;
        }
    }
}
