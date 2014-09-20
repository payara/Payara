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
 *
 *
 * This file incorporates work covered by the following copyright and
 * permission notice:
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.catalina.fileupload;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


/** Utility class for working with streams.
 */
final class Streams {
    /**
     * Private constructor, to prevent instantiation.
     * This class has only static methods.
     */
    private Streams() {
        // Does nothing
    }

    /**
     * Default buffer size for use in
     * {@link #copy(InputStream, OutputStream, boolean)}.
     */
    private static final int DEFAULT_BUFFER_SIZE = 8192;

    /**
     * Copies the contents of the given {@link InputStream}
     * to the given {@link OutputStream}. Shortcut for
     * <pre>
     *   copy(pInputStream, pOutputStream, new byte[8192]);
     * </pre>
     * @param pInputStream The input stream, which is being read.
     * It is guaranteed, that {@link InputStream#close()} is called
     * on the stream.
     * @param pOutputStream The output stream, to which data should
     * be written. May be null, in which case the input streams
     * contents are simply discarded.
     * @param pClose True guarantees, that {@link OutputStream#close()}
     * is called on the stream. False indicates, that only
     * {@link OutputStream#flush()} should be called finally.
     *
     * @return Number of bytes, which have been copied.
     * @throws IOException An I/O error occurred.
     */
    public static long copy(InputStream pInputStream,
            OutputStream pOutputStream, boolean pClose)
            throws IOException {
        return copy(pInputStream, pOutputStream, pClose,
                new byte[DEFAULT_BUFFER_SIZE]);
    }

    /**
     * Copies the contents of the given {@link InputStream}
     * to the given {@link OutputStream}.
     * @param pIn The input stream, which is being read.
     *   It is guaranteed, that {@link InputStream#close()} is called
     *   on the stream.
     * @param pOut The output stream, to which data should
     *   be written. May be null, in which case the input streams
     *   contents are simply discarded.
     * @param pClose True guarantees, that {@link OutputStream#close()}
     *   is called on the stream. False indicates, that only
     *   {@link OutputStream#flush()} should be called finally.
     * @param pBuffer Temporary buffer, which is to be used for
     *   copying data.
     * @return Number of bytes, which have been copied.
     * @throws IOException An I/O error occurred.
     */
    public static long copy(InputStream pIn,
            OutputStream pOut, boolean pClose,
            byte[] pBuffer)
    throws IOException {
        OutputStream out = pOut;
        InputStream in = pIn;
        try {
            long total = 0;
            for (;;) {
                int res = in.read(pBuffer);
                if (res == -1) {
                    break;
                }
                if (res > 0) {
                    total += res;
                    if (out != null) {
                        out.write(pBuffer, 0, res);
                    }
                }
            }
            if (out != null) {
                if (pClose) {
                    out.close();
                } else {
                    out.flush();
                }
                out = null;
            }
            in.close();
            in = null;
            return total;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Throwable t) {
                    /* Ignore me */
                }
            }
            if (pClose  &&  out != null) {
                try {
                    out.close();
                } catch (Throwable t) {
                    /* Ignore me */
                }
            }
        }
    }

    /**
     * This convenience method allows to read a Stream
     * content into a string. The platform's default character encoding
     * is used for converting bytes into characters.
     * @param pStream The input stream to read.
     * @see #asString(InputStream, String)
     * @return The streams contents, as a string.
     * @throws IOException An I/O error occurred.
     */
    public static String asString(InputStream pStream) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        copy(pStream, baos, true);
        return baos.toString();
    }

    /**
     * This convenience method allows to read a Stream
     * content into a string, using the given character encoding.
     * @param pStream The input stream to read.
     * @param pEncoding The character encoding, typically "UTF-8".
     * @see #asString(InputStream)
     * @return The streams contents, as a string.
     * @throws IOException An I/O error occurred.
     */
    public static String asString(InputStream pStream, String pEncoding)
            throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        copy(pStream, baos, true);
        return baos.toString(pEncoding);
    }
}
