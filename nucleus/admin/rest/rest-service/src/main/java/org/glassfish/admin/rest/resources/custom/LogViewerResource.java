/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.admin.rest.resources.custom;

import com.sun.enterprise.server.logging.logviewer.backend.LogFilter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.Writer;
import java.nio.charset.Charset;


import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.*;

import java.io.OutputStreamWriter;
import java.net.URI;
import java.util.zip.GZIPOutputStream;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.glassfish.admin.rest.adapter.LocatorBridge;
import org.glassfish.admin.rest.logviewer.CharSpool;
import org.glassfish.admin.rest.logviewer.LineEndNormalizingWriter;
import org.glassfish.admin.rest.logviewer.WriterOutputStream;
import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.config.Dom;

/**
 * Represents a large text data.
 * <p/>
 * <p/>
 * This class defines methods for handling progressive text update.
 * <p/>
 * <h2>Usage</h2>
 * <p/>
 *
 * @author Kohsuke Kawaguchi
 */
//@Path("view-log/")
public class LogViewerResource {

    @Context
    protected ServiceLocator injector;
    @Context
    protected UriInfo ui;

    @Context
    protected LocatorBridge habitat;

    /**
     * Represents the data source of this text.
     */
    private interface Source {

        Session open() throws IOException;

        long length();

        boolean exists();
    }

    private Source source;
    protected Charset charset;
    private volatile boolean completed;

    public void setEntity(Dom p) {
        // ugly no-op hack. For now.
    }

    @Path("details/")
    public StructuredLogViewerResource getDomainUptimeResource() {
        StructuredLogViewerResource resource = injector.createAndInitialize(StructuredLogViewerResource.class);
        return resource;
    }

    @GET
    @Produces("text/plain;charset=UTF-8")
    public Response get(@QueryParam("start")
                        @DefaultValue("0") long start,
                        @QueryParam("instanceName") @DefaultValue("server") String instanceName,
                        @Context HttpHeaders hh) throws IOException {
        boolean gzipOK = true;
        MultivaluedMap<String, String> headerParams = hh.getRequestHeaders();
        String acceptEncoding = headerParams.getFirst("Accept-Encoding");
        if (acceptEncoding == null || acceptEncoding.indexOf("gzip") == -1) {
            gzipOK = false;
        }


        // getting logFilter object from habitat
        LogFilter logFilter = habitat.getRemoteLocator().getService(LogFilter.class);
        String logLocation = "";

        // getting log file location on DAS for server/local instance/remote instance
        logLocation = logFilter.getLogFileForGivenTarget(instanceName);
        initLargeText(new File(logLocation), false);


        if (!source.exists()) {
            // file doesn't exist yet
            UriBuilder uriBuilder = ui.getAbsolutePathBuilder();
            uriBuilder.queryParam("start", 0);
            uriBuilder.queryParam("instanceName", instanceName);

            return Response.ok(
                    new StreamingOutput() {

                        @Override
                        public void write(OutputStream out) throws IOException, WebApplicationException {
                        }
                    }).
                    header("X-Text-Append-Next", uriBuilder.build()).build();
        }


        if (source.length() < start) {
            start = 0;  // text rolled over
        }
        final CharSpool spool = new CharSpool();
        long size = writeLogTo(start, spool);

        //       response.addHeader("X-Text-Size", String.valueOf(r));
        // if (!completed) {
        //           response.addHeader("X-More-Data", "true");
        // }
        if (size < 10000) {
            gzipOK = false;
        }
        final boolean gz = gzipOK;
        ResponseBuilder rp = Response.ok(
                new StreamingOutput() {

                    @Override
                    public void write(OutputStream out) throws IOException, WebApplicationException {
                        Writer w = getWriter(out, gz);
                        spool.writeTo(new LineEndNormalizingWriter(w));
                        w.flush();
                        w.close();
                    }
                });
        UriBuilder uriBuilder = ui.getAbsolutePathBuilder();
        uriBuilder.queryParam("start", size);
        uriBuilder.queryParam("instanceName", instanceName);
        URI next = uriBuilder.build();
        rp.header("X-Text-Append-Next", next);
        if (gzipOK) {
            rp = rp.header("Content-Encoding", "gzip");
        }
        return rp.build();
        //   return rp.header("X-Text-Size", String.valueOf(r)).header("X-More-Data", "true").build();
    }

    private Writer getWriter(OutputStream out, boolean gzipOK) throws IOException {
        if (gzipOK == false) {
            return new OutputStreamWriter(out);
        } else {
            return new OutputStreamWriter(new GZIPOutputStream(out), "UTF-8");
        }
    }

    public void initLargeText(File file, boolean completed) {
        initLargeText(file, Charset.defaultCharset(), completed);
    }

    public void initLargeText(final File file, Charset charset, boolean completed) {
        this.charset = charset;
        this.source = new Source() {

            @Override
            public Session open() throws IOException {
                return new FileSession(file);
            }

            @Override
            public long length() {
                return file.length();
            }

            @Override
            public boolean exists() {
                return file.exists();
            }
        };
        this.completed = completed;
    }

    public void markAsComplete() {
        completed = true;
    }

    public boolean isComplete() {
        return completed;
    }

    private long writeLogTo(long start, Writer w) throws IOException {
        return writeLogTo(start, new WriterOutputStream(w, charset));
    }

    /**
     * Writes the tail portion of the file to the {@link OutputStream}.
     *
     * @param start The byte offset in the input file where the write operation starts.
     * @return if the file is still being written, this method writes the file
     *         until the last newline character and returns the offset to start
     *         the next write operation.
     */
    private long writeLogTo(long start, OutputStream os) throws IOException {
        /// CountingOutputStream os = new CountingOutputStream(out);
        long count = 0;
        Session f = source.open();
        f.skip(start);

        if (completed) {
            // write everything till EOF
            byte[] buf = new byte[1024];
            int sz;
            while ((sz = f.read(buf)) >= 0) {
                os.write(buf, 0, sz);
            }
            count += sz;
        } else {
            ByteBuf buf = new ByteBuf(null, f);
            HeadMark head = new HeadMark(buf);
            TailMark tail = new TailMark(buf);

            while (tail.moveToNextLine(f)) {
                count += head.moveTo(tail, os);
            }
            count += head.finish(os);
        }

        f.close();
        os.flush();

        return count + start;
    }

    /**
     * Points to a byte in the buffer.
     */
    private static class Mark {

        protected ByteBuf buf;
        protected int pos;

        public Mark(ByteBuf buf) {
            this.buf = buf;
        }
    }

    /**
     * Points to the start of the region that's not committed
     * to the output yet.
     */
    private static final class HeadMark extends Mark {

        public HeadMark(ByteBuf buf) {
            super(buf);
        }

        /**
         * Moves this mark to 'that' mark, and writes the data
         * to {@link OutputStream} if necessary.
         */
        long moveTo(Mark that, OutputStream os) throws IOException {
            long count = 0;
            while (this.buf != that.buf) {
                os.write(buf.buf, 0, buf.size);
                count += buf.size;
                buf = buf.next;
                pos = 0;
            }

            this.pos = that.pos;
            return count;
        }

        long finish(OutputStream os) throws IOException {
            os.write(buf.buf, 0, pos);
            return pos;
        }
    }

    /**
     * Points to the end of the region.
     */
    private static final class TailMark extends Mark {

        public TailMark(ByteBuf buf) {
            super(buf);
        }

        boolean moveToNextLine(Session f) throws IOException {
            while (true) {
                while (pos == buf.size) {
                    if (!buf.isFull()) {
                        // read until EOF
                        return false;
                    } else {
                        // read into the next buffer
                        buf = new ByteBuf(buf, f);
                        pos = 0;
                    }
                }
                byte b = buf.buf[pos++];
                if (b == '\r' || b == '\n') {
                    return true;
                }
            }
        }
    }

    /**
     * Variable length byte buffer implemented as a linked list of fixed length buffer.
     */
    private static final class ByteBuf {

        private final byte[] buf = new byte[1024];
        private int size = 0;
        private ByteBuf next;

        public ByteBuf(ByteBuf previous, Session f) throws IOException {
            if (previous != null) {
                assert previous.next == null;
                previous.next = this;
            }

            while (!this.isFull()) {
                int chunk = f.read(buf, size, buf.length - size);
                if (chunk == -1) {
                    return;
                }
                size += chunk;
            }
        }

        public boolean isFull() {
            return buf.length == size;
        }
    }

    /**
     * Represents the read session of the {@link Source}.
     * Methods generally follow the contracts of {@link InputStream}.
     */
    private interface Session {

        void close() throws IOException;

        void skip(long start) throws IOException;

        int read(byte[] buf) throws IOException;

        int read(byte[] buf, int offset, int length) throws IOException;
    }

    /**
     * {@link Session} implementation over {@link RandomAccessFile}.
     */
    private static final class FileSession implements Session {

        private final RandomAccessFile file;

        public FileSession(File file) throws IOException {
            this.file = new RandomAccessFile(file, "r");
        }

        @Override
        public void close() throws IOException {
            file.close();
        }

        @Override
        public void skip(long start) throws IOException {
            file.seek(file.getFilePointer() + start);
        }

        @Override
        public int read(byte[] buf) throws IOException {
            return file.read(buf);
        }

        @Override
        public int read(byte[] buf, int offset, int length) throws IOException {
            return file.read(buf, offset, length);
        }
    }
}
